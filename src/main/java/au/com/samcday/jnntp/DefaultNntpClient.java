package au.com.samcday.jnntp;

import au.com.samcday.jnntp.bandwidth.BandwidthHandler;
import au.com.samcday.jnntp.bandwidth.HandlerRegistration;
import au.com.samcday.jnntp.exceptions.*;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Futures;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultNntpClient implements NntpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNntpClient.class);

    static final String HANDLER_PROCESSOR = "nntpprocessor";

    private static final AtomicInteger channelFactoryRefs = new AtomicInteger(0);
    private EventLoopGroup group = null;

    private String host;
    private int port;
    private boolean ssl;
    private Channel channel;
    private final ConcurrentLinkedQueue<NntpFuture<?>> pipeline;
    private boolean canPost;
    private int connectTimeoutMillis;
    private Map<HandlerRegistration, BandwidthHandler> bandwidthHandlers;
    private TrafficShapingHandler trafficHandler;

    public DefaultNntpClient(String host, int port, boolean ssl, int connectTimeoutMillis) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.pipeline = new ConcurrentLinkedQueue<>();
        this.bandwidthHandlers = new ConcurrentHashMap<>();
        this.trafficHandler = new TrafficShapingHandler();
    }

    @Override
    public void connect() throws NntpClientConnectionError {
        LOGGER.info("Connecting to {} on port {}", this.host, this.port);

        // We'll be waiting for the connection message.
        NntpFuture<GenericResponse> welcomeFuture = new NntpFuture<>(Response.ResponseType.WELCOME);
        this.pipeline.add(welcomeFuture);

        // Connect to the server now.
        ChannelFuture future = this.initializeChannel(new InetSocketAddress(this.host, this.port));
        if(!future.isSuccess()) {
            throw new NntpClientConnectionError(future.cause());
        }
        this.channel = future.channel();

        if(this.ssl) {
            LOGGER.info("Using SSL for new connection");
            Future handshakeFuture = this.channel.pipeline().get(SslHandler.class).handshakeFuture().syncUninterruptibly();
            if(!handshakeFuture.isSuccess()) {
                throw new NntpClientConnectionError(handshakeFuture.cause());
            }
        }

        LOGGER.info("Successfully connected.");

        GenericResponse response = Futures.getUnchecked(welcomeFuture);
        boolean temporarilyUnavailable = false;
        switch(response.getCode()) {
            case 200:
                this.canPost = true;
            case 201:
                return;
            case 400:
                temporarilyUnavailable = true;
            case 502:
                throw new NntpClientConnectionError(new NntpServerUnavailableException(temporarilyUnavailable));
            default:
                // FIXME: typed exception here mebbe?
                throw new NntpClientConnectionError(new RuntimeException("Unexpected status code " + response.getCode() + " returned on initial connection."));
        }
    }

    /**
     * Closes the connection to the remote NNTP server and cleans up any resources held by this client.
     */
    @Override
    public void disconnect() {
        // TODO: cancel all futures in pipeline and clean up the command pipeline.
        this.group.shutdownGracefully().syncUninterruptibly();
    }

    private ChannelFuture initializeChannel(InetSocketAddress addr) throws NntpClientConnectionError {
        final SSLEngine sslEngine;
        if(this.ssl) {
            try {
                sslEngine = this.initializeSsl();
                sslEngine.setUseClientMode(true);
            }
            catch(NoSuchAlgorithmException|KeyManagementException ex) {
                throw new NntpClientConnectionError(ex);
            }
        }
        else {
            sslEngine = null;
        }

        this.group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap()
            .group(this.group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeoutMillis)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("BANDWIDTH", trafficHandler);

                    if (sslEngine != null) {
                        ch.pipeline().addLast("ssl", new SslHandler(sslEngine));
                    }

                    ch.pipeline()
                        .addLast("stringenc", new StringEncoder(Charsets.UTF_8))
                        .addLast("lineframer", new DelimiterBasedFrameDecoder(4096, Delimiters.lineDelimiter()))
                        .addLast("decoder", new ResponseDecoder(new ResponseStateNotifierImpl(DefaultNntpClient.this.pipeline)))
                        .addLast(HANDLER_PROCESSOR, new ResponseProcessor(DefaultNntpClient.this.pipeline));
                }
            });

        return bootstrap.connect(addr).syncUninterruptibly();
    }

    private SSLEngine initializeSsl() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        return ctx.createSSLEngine();
    }

    @Override
    public void authenticate(String username, String password) throws NntpClientAuthenticationException {
        NntpFuture<GenericResponse> future = this.sendCommand(Response.ResponseType.AUTHINFO, "USER", username);
        GenericResponse resp = Futures.getUnchecked(future);
        if(resp.getCode() == 281) {
            // Well ... that was easy.
            return;
        }

        if(resp.getCode() == 381) {
            future = this.sendCommand(Response.ResponseType.AUTHINFO, "PASS", password);
            resp = Futures.getUnchecked(future);
            if(resp.getCode() == 281) {
                return;
            }

            if(resp.getCode() == 481) {
                throw new NntpClientAuthenticationException(new NntpInvalidLoginException());
            }
        }

        throw new NntpClientAuthenticationException(new NntpClientException("Unknown login error."));
    }

    /**
     * Invokes the DATE command on NNTP server and returns response as a java.util.Date
     * @return
     */
    @Override
    public Date date() {
        NntpFuture<DateResponse> future = this.sendCommand(Response.ResponseType.DATE);
        DateResponse response = Futures.getUnchecked(future);
        return response.getDate();
    }

    @Override
    public List<GroupListItem> list() {
        NntpFuture<ListResponse> future = this.sendCommand(Response.ResponseType.LIST);
        ListResponse response = Futures.getUnchecked(future);
        return response.getItems();
    }

    @Override
    public GroupInfo group(String name) {
        NntpFuture<GroupResponse> future = this.sendCommand(Response.ResponseType.GROUP, name);
        return Futures.getUnchecked(future).info;
    }

    @Override
    public OverviewList overview(long start, long end) {
        NntpFuture<OverviewResponse> future = this.sendCommand(Response.ResponseType.XZVER, Long.toString(start) + "-" + Long.toString(end));
        return Futures.getUnchecked(future).list;
    }

    @Override
    public InputStream body(String messageId) {
        NntpFuture<BodyResponse> future = this.sendCommand(Response.ResponseType.BODY, messageId);
        return Futures.getUnchecked(future).stream;
    }

    @Override
    public HandlerRegistration registerBandwidthHandler(BandwidthHandler handler) {
        HandlerRegistration reg = new HandlerRegistration() {
            @Override
            public void remove() {
                bandwidthHandlers.remove(this);
            }
        };

        this.bandwidthHandlers.put(reg, handler);

        return reg;
    }

    private <T extends Response> NntpFuture<T> sendCommand(Response.ResponseType type, String... args) {
        LOGGER.debug("Sending command {}", type);

        NntpFuture future = new NntpFuture(type);
        synchronized (this.pipeline) {
            this.pipeline.add(future);
            this.channel.write(type.name());
            for(int i = 0; i < args.length; i++) {
                this.channel.write(" ");
                this.channel.write(args[i]);
            }
            this.channel.write("\r\n");
            this.channel.flush();
        }

        return future;
    }

    private class TrafficShapingHandler extends ChannelTrafficShapingHandler {
        public TrafficShapingHandler() {
            super(0, 0, 1000);
        }

        @Override
        protected void doAccounting(TrafficCounter counter) {
            long read = counter.lastReadBytes();
            long write = counter.lastWrittenBytes();
            for(BandwidthHandler handler : bandwidthHandlers.values()) {
                handler.update(read, write);
            }
        }
    }
}

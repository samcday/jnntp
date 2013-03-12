package au.com.samcday.jnntp;

import au.com.samcday.jnntp.bandwidth.BandwidthHandler;
import au.com.samcday.jnntp.bandwidth.HandlerRegistration;
import au.com.samcday.jnntp.exceptions.*;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Futures;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.jboss.netty.handler.traffic.TrafficCounter;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.ObjectSizeEstimator;

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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultNntpClient implements NntpClient {
    static final String HANDLER_PROCESSOR = "nntpprocessor";

    private static final AtomicInteger channelFactoryRefs = new AtomicInteger(0);
    private static ChannelFactory channelFactory = null;

    private String host;
    private int port;
    private boolean ssl;
    private Channel channel;
    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;
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
        // We'll be waiting for the connection message.
        NntpFuture<GenericResponse> welcomeFuture = new NntpFuture<>(Response.ResponseType.WELCOME);
        this.pipeline.add(welcomeFuture);

        // Connect to the server now.
        ChannelFuture future = this.initializeChannel(new InetSocketAddress(this.host, this.port));
        if(!future.isSuccess()) {
            throw new NntpClientConnectionError(future.getCause());
        }
        this.channel = future.getChannel();

        if(this.ssl) {
            ChannelFuture handshakeFuture = this.channel.getPipeline().get(SslHandler.class).handshake().awaitUninterruptibly();
            if(!handshakeFuture.isSuccess()) {
                throw new NntpClientConnectionError(handshakeFuture.getCause());
            }
        }

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
        this.channel.close().awaitUninterruptibly();
        unrefChannelFactory();
        this.trafficHandler.releaseExternalResources();
    }

    private ChannelFuture initializeChannel(InetSocketAddress addr) throws NntpClientConnectionError {
        final SSLEngine engine;
        if(this.ssl) {
            try {
                engine = this.initializeSsl();
                engine.setUseClientMode(true);
            }
            catch(NoSuchAlgorithmException|KeyManagementException ex) {
                throw new NntpClientConnectionError(ex);
            }
        }
        else {
            engine = null;
        }

        ClientBootstrap bootstrap = new ClientBootstrap(channelFactory());
        bootstrap.setOption("connectTimeoutMillis", this.connectTimeoutMillis);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        bootstrap.setOption("reuseAddress", true);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();

                pipeline.addLast("BANDWIDTH", trafficHandler);

                if(engine != null) {
                    pipeline.addLast("ssl", new SslHandler(engine));
                }

                pipeline.addLast("stringenc", new StringEncoder(Charsets.UTF_8));
                pipeline.addLast("lineframer", new LineBasedFrameDecoder(4096));
                pipeline.addLast("decoder", new ResponseDecoder(new ResponseStateNotifierImpl(DefaultNntpClient.this.pipeline)));
                pipeline.addLast(HANDLER_PROCESSOR, new ResponseProcessor(DefaultNntpClient.this.pipeline));

                return pipeline;
            }
        });

        return bootstrap.connect(addr).awaitUninterruptibly();
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
        NntpFuture future = new NntpFuture(type);
        synchronized (this.pipeline) {
            this.pipeline.add(future);
            this.channel.write(type.name());
            for(int i = 0; i < args.length; i++) {
                this.channel.write(" ");
                this.channel.write(args[i]);
            }
            this.channel.write("\r\n");
        }

        return future;
    }

    private static final ChannelFactory channelFactory() {
        synchronized (channelFactoryRefs) {
            if(channelFactory == null) {
                channelFactory = new NioClientSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool()
                );
            }
            channelFactoryRefs.incrementAndGet();
            return channelFactory;
        }
    }

    private static final void unrefChannelFactory() {
        synchronized (channelFactoryRefs) {
            if(channelFactoryRefs.decrementAndGet() == 0) {
                channelFactory.releaseExternalResources();
                channelFactory = null;
            }
        }
    }

    private static final HashedWheelTimer TICKER = new HashedWheelTimer(1, TimeUnit.SECONDS);
    private class TrafficShapingHandler extends ChannelTrafficShapingHandler {
        public TrafficShapingHandler() {
            super(new ObjectSizeEstimator() {
                @Override
                public int estimateSize(Object o) {
                    return o instanceof ChannelBuffer ? ChannelBuffer.class.cast(o).readableBytes() : 0;
                }
            }, TICKER, 1000);
        }

        @Override
        protected void doAccounting(TrafficCounter counter) {
            long read = counter.getLastReadBytes();
            long write = counter.getLastWrittenBytes();
            for(BandwidthHandler handler : bandwidthHandlers.values()) {
                handler.update(read, write);
            }
        }
    }
}

package au.com.samcday.jnntp;

import au.com.samcday.jnntp.exceptions.*;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Futures;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

public class NntpClient {
    static final String HANDLER_PROCESSOR = "nntpprocessor";

    private String host;
    private int port;
    private boolean ssl;
    private Channel channel;
    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;
    private ClientBootstrap bootstrap;
    private boolean canPost;

    public NntpClient(String host, int port, boolean ssl) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;

        this.pipeline = new ConcurrentLinkedQueue<>();
    }

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
    public void disconnect() {
        // TODO: cancel all futures in pipeline and clean up the command pipeline.
        this.channel.close().awaitUninterruptibly();
        this.bootstrap.releaseExternalResources();
    }

    private ChannelFuture initializeChannel(InetSocketAddress addr) throws NntpClientConnectionError {
        ChannelFactory factory = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()
        );

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

        this.bootstrap = new ClientBootstrap(factory);
        this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                if(engine != null) {
                    pipeline.addLast("ssl", new SslHandler(engine));
                }

                pipeline.addLast("stringenc", new StringEncoder(Charsets.UTF_8));
                pipeline.addLast("lineframer", new LineBasedFrameDecoder(4096));
                pipeline.addLast("decoder", new ResponseDecoder(new ResponseStateNotifierImpl(NntpClient.this.pipeline)));
                pipeline.addLast(HANDLER_PROCESSOR, new ResponseProcessor(NntpClient.this.pipeline));

                return pipeline;
            }
        });

        return this.bootstrap.connect(addr).awaitUninterruptibly();
    }

    private SSLEngine initializeSsl() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        return ctx.createSSLEngine();
    }

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
    public Date date() {
        NntpFuture<DateResponse> future = this.sendCommand(Response.ResponseType.DATE);
        DateResponse response = Futures.getUnchecked(future);
        return response.getDate();
    }

    public List<GroupListItem> list() {
        NntpFuture<ListResponse> future = this.sendCommand(Response.ResponseType.LIST);
        ListResponse response = Futures.getUnchecked(future);
        return response.getItems();
    }

    public GroupInfo group(String name) {
        NntpFuture<GroupResponse> future = this.sendCommand(Response.ResponseType.GROUP, name);
        return Futures.getUnchecked(future).info;
    }

    public OverviewList overview(int start, int end) {
        NntpFuture<OverviewResponse> future = this.sendCommand(Response.ResponseType.XZVER, Integer.toString(start) + "-" + Integer.toString(end));
        return Futures.getUnchecked(future).list;
    }

    private <T extends Response> NntpFuture<T> sendCommand(Response.ResponseType type, String... args) {
        NntpFuture future = new NntpFuture(type);
        synchronized (this.channel) {
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
}

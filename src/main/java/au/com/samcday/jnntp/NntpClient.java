package au.com.samcday.jnntp;

import au.com.samcday.ResponseStateNotifierImpl;
import au.com.samcday.jnntp.exceptions.*;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Futures;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

public class NntpClient {
    private String host;
    private int port;
    private Channel channel;
    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;
    private boolean canPost;

    public NntpClient(String host, int port) {
        this.host = host;
        this.port = port;

        this.pipeline = new ConcurrentLinkedQueue<>();
    }

    public void connect() throws NntpClientConnectionError {
        // We'll be waiting for the connection message.
        NntpFuture<NntpGenericResponse> welcomeFuture = new NntpFuture<>(NntpResponse.ResponseType.WELCOME);
        this.pipeline.add(welcomeFuture);

        // Connect to the server now.
        ChannelFuture future = this.initializeChannel(new InetSocketAddress(this.host, this.port));
        if(!future.isSuccess()) {
            throw new NntpClientConnectionError(future.getCause());
        }
        this.channel = future.getChannel();

        NntpGenericResponse response = Futures.getUnchecked(welcomeFuture);
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

    private ChannelFuture initializeChannel(InetSocketAddress addr) {
        ChannelFactory factory = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()
        );
        ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setPipeline(Channels.pipeline(
            new StringEncoder(Charsets.UTF_8),
            new LineBasedFrameDecoder(4096),
            new ResponseDecoder(new ResponseStateNotifierImpl(this.pipeline)),
            new ResponseProcessor(this.pipeline)
        ));

        return bootstrap.connect(addr).awaitUninterruptibly();
    }

    public void authenticate(String username, String password) throws NntpClientAuthenticationException {
        NntpFuture<NntpGenericResponse> future = this.sendCommand(NntpResponse.ResponseType.AUTHINFO, "USER", username);
        NntpGenericResponse resp = Futures.getUnchecked(future);
        if(resp.getCode() == 281) {
            // Well ... that was easy.
            return;
        }

        if(resp.getCode() == 381) {
            future = this.sendCommand(NntpResponse.ResponseType.AUTHINFO, "PASS", password);
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
        NntpFuture<NntpDateResponse> future = this.sendCommand(NntpResponse.ResponseType.DATE);
        NntpDateResponse response = Futures.getUnchecked(future);
        return response.getDate();
    }

    public List<GroupListItem> list() {
        NntpFuture<NntpListResponse> future = this.sendCommand(NntpResponse.ResponseType.LIST);
        NntpListResponse response = Futures.getUnchecked(future);
        return response.getItems();
    }

    public GroupInfo group(String name) {
        NntpFuture<NntpGroupResponse> future = this.sendCommand(NntpResponse.ResponseType.GROUP, name);
        return Futures.getUnchecked(future).info;
    }



    private <T extends NntpResponse> NntpFuture<T> sendCommand(NntpResponse.ResponseType type, String... args) {
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

package au.com.samcday.jnntp;

import au.com.samcday.jnntp.exceptions.NntpClientConnectionError;
import au.com.samcday.jnntp.exceptions.NntpServerUnavailableException;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

public class NntpClient {
    private String host;
    private int port;
    private Channel channel;
    private ConcurrentLinkedQueue<NntpFuture<?>> responseQueue;
    private boolean canPost;

    public NntpClient(String host, int port) {
        this.host = host;
        this.port = port;

        this.responseQueue = new ConcurrentLinkedQueue<>();
    }

    public void connect() throws NntpClientConnectionError {
        // We'll be waiting for the connection message.
        NntpFuture<NntpWelcomeResponse> welcomeFuture = new NntpFuture<>(NntpResponse.ResponseType.WELCOME);
        this.responseQueue.add(welcomeFuture);

        // Connect to the server now.
        ChannelFuture future = this.initializeChannel(new InetSocketAddress(this.host, this.port));
        if(!future.isSuccess()) {
            throw new NntpClientConnectionError(future.getCause());
        }
        this.channel = future.getChannel();

        NntpWelcomeResponse response = Futures.getUnchecked(welcomeFuture);
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
        bootstrap.setPipeline(Channels.pipeline(new StringEncoder(Charsets.UTF_8), new LineBasedFrameDecoder(4096), new NntpResponseDecoder(responseQueue)));

        return bootstrap.connect(addr).awaitUninterruptibly();
    }

    public void authenticate(String username, String password) {

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

    private <T extends NntpResponse> NntpFuture<T> sendCommand(NntpResponse.ResponseType type) {
        NntpFuture future = new NntpFuture(type);
        synchronized (this.channel) {
            this.responseQueue.add(future);
            this.channel.write(type.name() + "\r\n");
        }

        return future;
    }
}

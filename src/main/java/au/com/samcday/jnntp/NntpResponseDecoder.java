package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

import static au.com.samcday.jnntp.Util.pullAsciiNumberFromBuffer;

public class NntpResponseDecoder extends SimpleChannelHandler {
    private ConcurrentLinkedQueue<NntpFuture<?>> responseQueue;

    public NntpResponseDecoder(ConcurrentLinkedQueue<NntpFuture<?>> responseQueue) {
        this.responseQueue = responseQueue;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ChannelBuffer buf = (ChannelBuffer)e.getMessage();

        int code = pullAsciiNumberFromBuffer(buf, 3);
        buf.skipBytes(1);

        NntpFuture future = this.responseQueue.poll();

        NntpResponse response = this.prepareResponse(future.getType(), code, buf);
        future.onResponse(response);
    }

    private NntpResponse prepareResponse(NntpResponse.ResponseType type, int code, ChannelBuffer buf) {
        NntpResponse response = null;

        switch(type) {
            case WELCOME:
                response = new NntpWelcomeResponse(code);
                break;
            case DATE:
                response = new NntpDateResponse(code);
                break;
        }

        if(response != null) response.process(buf);
        return response;
    }
}

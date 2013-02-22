package au.com.samcday.jnntp;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

public class NntpResponseDispatcher extends SimpleChannelHandler {
    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;

    public NntpResponseDispatcher(ConcurrentLinkedQueue<NntpFuture<?>> pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        NntpFuture future = this.pipeline.poll();
        future.onResponse((NntpResponse)e.getMessage());
    }
}

package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ResponseProcessor extends SimpleChannelHandler {
    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;

    private NntpResponse currentResponse;

    public ResponseProcessor(ConcurrentLinkedQueue<NntpFuture<?>> pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if(e.getMessage() instanceof RawResponseMessage) {
            RawResponseMessage rawResponse = (RawResponseMessage)e.getMessage();

            // TODO: error code handling here.

            NntpResponse response = this.constructResponse(this.pipeline.peek().getType());
            response.setCode(rawResponse.code);
            response.process(rawResponse.buffer);

            if(rawResponse.multiline) {
                this.currentResponse = response;
            }
            else {
                NntpFuture future = this.pipeline.poll();
                future.onResponse(response);
            }
        }
        else if(e.getMessage() == MultilineEndMessage.INSTANCE) {
            NntpFuture future = this.pipeline.poll();
            future.onResponse(this.currentResponse);
        }
        else if(this.currentResponse != null) {
            this.currentResponse.processLine((ChannelBuffer)e.getMessage());
        }
    }

    private NntpResponse constructResponse(NntpResponse.ResponseType type) {
        switch(type) {
            case WELCOME:
            case AUTHINFO:
                return new NntpGenericResponse();
            case DATE:
                return new NntpDateResponse();
            case LIST:
                return new NntpListResponse();
            case GROUP:
                return new NntpGroupResponse();
            default:
                return null;
        }
    }
}

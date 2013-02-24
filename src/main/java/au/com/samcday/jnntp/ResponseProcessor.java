package au.com.samcday.jnntp;

import au.com.samcday.yenc.YencDecoder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibWrapper;
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ResponseProcessor extends SimpleChannelHandler {
    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;

    private Response currentResponse;

    public ResponseProcessor(ConcurrentLinkedQueue<NntpFuture<?>> pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if(e.getMessage() instanceof RawResponseMessage) {
            RawResponseMessage rawResponse = (RawResponseMessage)e.getMessage();

            // TODO: error code handling here.

            Response.ResponseType type = this.pipeline.peek().getType();
            Response response = this.constructResponse(type);
            response.setCode(rawResponse.code);
            response.process(rawResponse.buffer);

            if(type == Response.ResponseType.XZVER) {
                ChannelPipeline pipeline = ctx.getPipeline();
                pipeline.addBefore(NntpClient.HANDLER_PROCESSOR, "ydecode", new YencDecoder());
                pipeline.addBefore(NntpClient.HANDLER_PROCESSOR, "zlib", new ZlibDecoder(ZlibWrapper.NONE));
                pipeline.addBefore(NntpClient.HANDLER_PROCESSOR, "lineframeragain", new LineBasedFrameDecoder(4096));
            }

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

            Response.ResponseType type = future.getType();
            if(type == Response.ResponseType.XZVER) {
                ChannelPipeline pipeline = ctx.getPipeline();
                pipeline.remove("ydecode");
                pipeline.remove("zlib");
                pipeline.remove("lineframeragain");
            }
        }
        else if(this.currentResponse != null) {
            this.currentResponse.processLine((ChannelBuffer)e.getMessage());
        }
    }

    private Response constructResponse(Response.ResponseType type) {
        switch(type) {
            case WELCOME:
            case AUTHINFO:
                return new GenericResponse();
            case DATE:
                return new DateResponse();
            case LIST:
                return new ListResponse();
            case GROUP:
                return new GroupResponse();
            case XOVER:
            case XZVER:
            case OVER:
                return new OverviewResponse();

            default:
                return null;
        }
    }
}

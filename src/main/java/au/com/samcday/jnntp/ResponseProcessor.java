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

    private NntpResponse currentResponse;

    public ResponseProcessor(ConcurrentLinkedQueue<NntpFuture<?>> pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if(e.getMessage() instanceof RawResponseMessage) {
            RawResponseMessage rawResponse = (RawResponseMessage)e.getMessage();

            // TODO: error code handling here.

            NntpResponse.ResponseType type = this.pipeline.peek().getType();
            NntpResponse response = this.constructResponse(type);
            response.setCode(rawResponse.code);
            response.process(rawResponse.buffer);

            if(type == NntpResponse.ResponseType.XZVER) {
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

            NntpResponse.ResponseType type = future.getType();
            if(type == NntpResponse.ResponseType.XZVER) {
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
            case XOVER:
            case XZVER:
            case OVER:
                return new NntpOverviewResponse();

            default:
                return null;
        }
    }
}

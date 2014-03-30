package au.com.samcday.jnntp;

import au.com.samcday.yenc.YencDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ResponseProcessor extends SimpleChannelInboundHandler<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseProcessor.class);

    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;

    private Response currentResponse;

    public ResponseProcessor(ConcurrentLinkedQueue<NntpFuture<?>> pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof RawResponseMessage) {
            RawResponseMessage rawResponse = (RawResponseMessage)msg;
            LOGGER.trace("Got raw response message with code {}", rawResponse.code);

            // TODO: error code handling here.

            Command type = this.pipeline.peek().getType();
            Response response = this.constructResponse(type);
            response.setCode(rawResponse.code);
            response.process(rawResponse.buffer);

            ChannelPipeline pipeline = ctx.pipeline();
            if(type == Command.XZVER || type == Command.BODY) {
                pipeline.addBefore(DefaultNntpClient.HANDLER_PROCESSOR, "ydecode", new YencDecoder());
            }

            if(type == Command.XZVER) {
                pipeline.addBefore(DefaultNntpClient.HANDLER_PROCESSOR, "zlib", new JdkZlibDecoder(ZlibWrapper.NONE));
                pipeline.addBefore(DefaultNntpClient.HANDLER_PROCESSOR, "lineframeragain", new LineBasedFrameDecoder(4096));
            }

            NntpFuture future;
            if(rawResponse.multiline) {
                this.currentResponse = response;
                future = this.pipeline.peek();
            }
            else {
                future = this.pipeline.poll();
            }
            future.onResponse(response);
        }
        else if(msg == MultilineEndMessage.INSTANCE) {
            NntpFuture future = this.pipeline.remove();
            this.currentResponse.processLine(null);
            this.currentResponse = null;

            Command type = future.getType();
            ChannelPipeline pipeline = ctx.pipeline();
            if(type == Command.XZVER || type == Command.BODY) {
                pipeline.remove("ydecode");
            }
            if(type == Command.XZVER) {
                pipeline.remove("zlib");
                pipeline.remove("lineframeragain");
            }
        }
        else if(this.currentResponse != null) {
            this.currentResponse.processLine((ByteBuf)msg);
        }
    }

    private Response constructResponse(Command type) {
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
            case BODY:
                return new BodyResponse();
            default:
                return null;
        }
    }
}

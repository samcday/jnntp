package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import static au.com.samcday.jnntp.Util.pullAsciiNumberFromBuffer;

public class NntpResponseDecoder extends FrameDecoder {
    private byte LINE_TERMINATOR = 0x2E;

    private CommandPipelinePeeker pipelinePeeker;
    private NntpResponseFactory responseFactory;

    private NntpResponse currentMultilineResponse;

    public NntpResponseDecoder(CommandPipelinePeeker pipelinePeeker, NntpResponseFactory responseFactory) {
        this.pipelinePeeker = pipelinePeeker;
        this.responseFactory = responseFactory;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        if(this.currentMultilineResponse != null) {
            if(buffer.readByte() == LINE_TERMINATOR) {
                if(buffer.capacity() == 1) {
                    NntpResponse response = this.currentMultilineResponse;
                    this.currentMultilineResponse = null;
                    return response;
                }
            }
            else {
                buffer.resetReaderIndex();
            }
            this.currentMultilineResponse.processLine(buffer);
            buffer.skipBytes(buffer.readableBytes());
            return null;
        }
        else {
            int code = pullAsciiNumberFromBuffer(buffer, 3);
            buffer.skipBytes(1);

            NntpResponse.ResponseType type = this.pipelinePeeker.peekType();
            NntpResponse response = responseFactory.newResponse(type);
            response.setCode(code);
            response.process(buffer);

            // Just in case the response class didn't fully parse the buffer for whatever reason...
            buffer.skipBytes(buffer.readableBytes());

            if(response.isMultiline()) {
                this.currentMultilineResponse = response;
                return null;
            }
            else {
                return response;
            }
        }
    }
}

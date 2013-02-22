package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import static au.com.samcday.jnntp.Util.pullAsciiNumberFromBuffer;

public class NntpResponseDecoder extends FrameDecoder {
    private CommandPipelinePeeker pipelinePeeker;

    public NntpResponseDecoder(CommandPipelinePeeker pipelinePeeker) {
        this.pipelinePeeker = pipelinePeeker;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        int code = pullAsciiNumberFromBuffer(buffer, 3);
        buffer.skipBytes(1);

        NntpResponse.ResponseType type = this.pipelinePeeker.peekType();
        NntpResponse response = type.construct(code);
        response.process(buffer);

        // Just in case the response class didn't fully parse the buffer for whatever reason...
        buffer.skipBytes(buffer.readableBytes());
        return response;
    }
}

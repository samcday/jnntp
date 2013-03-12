package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import static au.com.samcday.jnntp.Util.pullAsciiIntFromBuffer;

/**
 * This decoder handles the raw responses from the server, and relies on an injected {@link ResponseStateNotifier}
 * to determine if it should parse a single line response, or a multiline one. It assumes framing has already been
 * handled by a downstream {@link org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder}.
 */
public class ResponseDecoder extends OneToOneDecoder {
    private static final byte LINE_TERMINATOR = 0x2E;

    private ResponseStateNotifier responseStateNotifier;
    private boolean decodingMultiline;

    public ResponseDecoder(ResponseStateNotifier responseStateNotifier) {
        this.responseStateNotifier = responseStateNotifier;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if(!(msg instanceof ChannelBuffer)) return msg;
        ChannelBuffer buffer = (ChannelBuffer)msg;

        if(this.decodingMultiline) {
            if(buffer.getByte(buffer.readerIndex()) == LINE_TERMINATOR) {
                buffer.skipBytes(1);
                if(!buffer.readable()) {
                    this.decodingMultiline = false;
                    return MultilineEndMessage.INSTANCE;
                }
            }

            return buffer.slice();
        }

        int code = pullAsciiIntFromBuffer(buffer, 3);
        buffer.skipBytes(1);

        this.decodingMultiline = this.responseStateNotifier.isMultiline(code);

        return new RawResponseMessage(code, buffer.slice(), this.decodingMultiline);
    }
}

package au.com.samcday.jnntp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import static au.com.samcday.jnntp.Util.pullAsciiIntFromBuffer;

/**
 * This decoder handles the raw responses from the server, and relies on an injected {@link ResponseStateNotifier}
 * to determine if it should parse a single line response, or a multiline one. It assumes framing has already been
 * handled by a downstream {@link io.netty.handler.codec.LineBasedFrameDecoder}.
 */
public class ResponseDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final byte LINE_TERMINATOR = 0x2E;

    private ResponseStateNotifier responseStateNotifier;
    private boolean decodingMultiline;

    public ResponseDecoder(ResponseStateNotifier responseStateNotifier) {
        this.responseStateNotifier = responseStateNotifier;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        if(this.decodingMultiline) {
            if(buffer.getByte(buffer.readerIndex()) == LINE_TERMINATOR) {
                buffer.skipBytes(1);
                if(!buffer.isReadable()) {
                    this.decodingMultiline = false;
                    out.add(MultilineEndMessage.INSTANCE);
                    return;
                }
            }

            out.add(buffer.retain());
            return;
        }

        int code = pullAsciiIntFromBuffer(buffer, 3);
        buffer.skipBytes(1);

        this.decodingMultiline = this.responseStateNotifier.isMultiline(code);

        out.add(new RawResponseMessage(code, buffer.retain(), this.decodingMultiline));
    }
}

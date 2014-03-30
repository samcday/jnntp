package au.com.samcday.yenc;

import au.com.samcday.jnntp.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.List;
import java.util.zip.CRC32;

import static au.com.samcday.jnntp.Util.pullAsciiHexNumberFromBuffer;
import static au.com.samcday.jnntp.Util.pullAsciiIntFromBuffer;

/**
 * Specialized yEnc decoder that works off ChannelBuffers for better zero-copy efficiency. Code based on lpireyn/pi-yenc
 * It is expected that this decoder will be downstream from a LineBasedFrameDecoder.
 */
public class YencDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final byte BYTE_SPACE = 0x20;
    private static final byte BYTE_ESCAPE = 0x3d;
    private static final byte BYTE_KEYWORD_LINE = 0x79;

    private CRC32 crc32 = new CRC32();
    private boolean decoding;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        // Are we looking at a yEnc header/trailer?
        if(buf.getByte(buf.readerIndex()) == BYTE_ESCAPE && buf.getByte(buf.readerIndex() + 1) == BYTE_KEYWORD_LINE) {
            buf.skipBytes(2);

            byte next = buf.readByte();

            if (next == 'b') {
                if (buf.readByte() == 'e' && buf.readByte() == 'g' && buf.readByte() == 'i' && buf.readByte() == 'n') {
                    if(this.decoding) {
                        // Uhhhh....
                        throw new IOException("New yEnc block begun whilst decoding a previous one.");
                    }

                    // The yEnc header includes some stuff we don't care about due to the awesome abstractions Netty
                    // provides: we don't care about line length advisory, or about size. The only thing we *might* end
                    // up caring about is the filename, as we may wanna emit a start message informing of the name of the
                    // yEnc block.
//                    this.parseHeader(buffer);

                    this.decoding = true;
                    this.crc32.reset();
                    return;
                }
            } else if (next == 'p') {
                if (buf.readByte() == 'a' && buf.readByte() == 'r' && buf.readByte() == 't') {
                    // TODO: should we propagate some kind of message for this?
                    return;
                }
            } else if (next == 'e') {
                if (buf.readByte() == 'n' && buf.readByte() == 'd') {
                    if(!this.decoding) {
                        // Uhhhh....
                        throw new IOException("yEnc block ended when we weren't decoding one.");
                    }

                    // We've already been emitting data as we parse it, so if CRC32 checksum has failed then it's probably
                    // already resulted in downstream parsing errors anyway. But, we'll check it for correctness.
                    long expectedCRC = this.parseTrailer(buf);
                    if(expectedCRC > -1 && this.crc32.getValue() != expectedCRC) {
                        throw new YencChecksumFailureException();
                    }

                    return;
                }
            }

            throw new IOException("Decoded unknown yEnc control line.");
        }

        if(this.decoding) {
            ByteBuf decoded = Unpooled.buffer(buf.readableBytes());

            while(buf.isReadable()) {
                int b = buf.readUnsignedByte();
                if(b == BYTE_ESCAPE) {
                    if(!buf.isReadable()) {
                        throw new IOException("EOL after escape byte.");
                    }
                    b = buf.readUnsignedByte() - 64;
                }
                b = b - 42 & 0xff;
                decoded.writeByte(b);
                this.crc32.update(b);
            }
            out.add(decoded);
        }

        return;
    }

    /**
     * Parse the ybegin line and return the size hint, if it exists.
     * @param buffer
     * @return
     */
//    @SuppressWarnings("unused")
//    private int parseHeader(ChannelBuffer buffer) {
//        int sizeIndex = buffer.bytesBefore(SIZE_INDEX_FINDER);
//        if(sizeIndex == -1) return -1;
//        buffer.skipBytes(sizeIndex + 5); // + 5 for the "size="
//        int endIndex = buffer.bytesBefore(BYTE_SPACE);
//        if(endIndex == -1) endIndex = buffer.readableBytes();
//        return pullAsciiIntFromBuffer(buffer, endIndex);
//    }

    private long parseTrailer(ByteBuf buffer) {
        int start = buffer.forEachByte(new Util.ByteBufStringIndexFinder("crc32="));
        if (start == -1) {
            return -1;
        }

        buffer.skipBytes(start - buffer.readerIndex() + 1);
        buffer.markReaderIndex();

        while(buffer.isReadable() && buffer.readByte() != BYTE_SPACE) {}

        int end = buffer.readerIndex();
        buffer.resetReaderIndex();

        return pullAsciiHexNumberFromBuffer(buffer.slice(buffer.readerIndex(), end - buffer.readerIndex()));

//        if (crc32Index)
//        int crc32Index = buffer.bytesBefore(CRC32_INDEX_FINDER);
//        if(crc32Index == -1) return -1;
//        buffer.skipBytes(crc32Index + 6); // + 6 for the "crc32="
//        int endIndex = buffer.bytesBefore(BYTE_SPACE);
//        if(endIndex == -1) endIndex = buffer.readableBytes();
//        return pullAsciiHexNumberFromBuffer(buffer, endIndex);
    }
}

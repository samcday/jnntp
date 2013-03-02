package au.com.samcday.yenc;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import java.io.IOException;
import java.util.zip.CRC32;

import static au.com.samcday.jnntp.Util.pullAsciiHexNumberFromBuffer;
import static au.com.samcday.jnntp.Util.pullAsciiIntFromBuffer;

/**
 * Specialized yEnc decoder that works off ChannelBuffers for better zero-copy efficiency. Code based on lpireyn/pi-yenc
 * It is expected that this decoder will be downstream from a LineBasedFrameDecoder.
 */
public class YencDecoder extends OneToOneDecoder {
    private static final byte BYTE_SPACE = 0x20;
    private static final byte BYTE_ESCAPE = 0x3d;
    private static final byte BYTE_KEYWORD_LINE = 0x79;
    private static final ChannelBufferIndexFinder BEGIN_INDEX_FINDER = new CharArrayIndexFinder("begin");
    private static final ChannelBufferIndexFinder END_INDEX_FINDER = new CharArrayIndexFinder("end");
    private static final ChannelBufferIndexFinder PART_INDEX_FINDER = new CharArrayIndexFinder("part");
    private static final ChannelBufferIndexFinder SIZE_INDEX_FINDER = new CharArrayIndexFinder("size");
    private static final ChannelBufferIndexFinder CRC32_INDEX_FINDER = new CharArrayIndexFinder("crc32");

    private CRC32 crc32 = new CRC32();
    private boolean decoding;

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if(!(msg instanceof ChannelBuffer)) return msg;
        ChannelBuffer buffer = (ChannelBuffer)msg;

        // Are we looking at a yEnc header/trailer?
        if(buffer.getByte(buffer.readerIndex()) == BYTE_ESCAPE && buffer.getByte(buffer.readerIndex() + 1) == BYTE_KEYWORD_LINE) {
            buffer.skipBytes(2);
            if(buffer.bytesBefore(BEGIN_INDEX_FINDER) == 0) {
                if(this.decoding) {
                    // Uhhhh....
                    throw new IOException("New yEnc block begun whilst decoding a previous one.");
                }

                // The yEnc header includes some stuff we don't care about due to the awesome abstractions Netty
                // provides: we don't care about line length advisory, or about size. The only thing we *might* end
                // up caring about is the filename, as we may wanna emit a start message informing of the name of the
                // yEnc block.
//                this.parseHeader(buffer);

                this.decoding = true;
                this.crc32.reset();

                return null;
            }
            else if(buffer.bytesBefore(END_INDEX_FINDER) == 0) {
                if(!this.decoding) {
                    // Uhhhh....
                    throw new IOException("yEnc block ended when we weren't decoding one.");
                }

                // We've already been emitting data as we parse it, so if CRC32 checksum has failed then it's probably
                // already resulted in downstream parsing errors anyway. But, we'll check it for brevity.
                long expectedCRC = this.parseTrailer(buffer);
                if(expectedCRC > -1 && this.crc32.getValue() != expectedCRC) {
                    throw new YencChecksumFailureException();
                }

                return null;
            }
            else if(buffer.bytesBefore(PART_INDEX_FINDER) == 0) {
                // TODO: should we propagate some kind of message for this?
                return null;
            }
            else {
                throw new IOException("Decoded unknown yEnc control line.");
            }
        }

        if(this.decoding) {
            ChannelBuffer decoded = buffer.factory().getBuffer(buffer.readableBytes());
            while(buffer.readable()) {
                int b = buffer.readUnsignedByte();
                if(b == BYTE_ESCAPE) {
                    if(!buffer.readable()) {
                        throw new IOException("EOL after escape byte.");
                    }
                    b = buffer.readUnsignedByte() - 64;
                }
                b = b - 42 & 0xff;
                decoded.writeByte(b);
                this.crc32.update(b);
            }
            return decoded;
        }

        return null;
    }

    /**
     * Parse the ybegin line and return the size hint, if it exists.
     * @param buffer
     * @return
     */
    @SuppressWarnings("unused")
    private int parseHeader(ChannelBuffer buffer) {
        int sizeIndex = buffer.bytesBefore(SIZE_INDEX_FINDER);
        if(sizeIndex == -1) return -1;
        buffer.skipBytes(sizeIndex + 5); // + 5 for the "size="
        int endIndex = buffer.bytesBefore(BYTE_SPACE);
        if(endIndex == -1) endIndex = buffer.readableBytes();
        return pullAsciiIntFromBuffer(buffer, endIndex);
    }

    private long parseTrailer(ChannelBuffer buffer) {
        int crc32Index = buffer.bytesBefore(CRC32_INDEX_FINDER);
        if(crc32Index == -1) return -1;
        buffer.skipBytes(crc32Index + 6); // + 6 for the "crc32="
        int endIndex = buffer.bytesBefore(BYTE_SPACE);
        if(endIndex == -1) endIndex = buffer.readableBytes();
        return pullAsciiHexNumberFromBuffer(buffer, endIndex);
    }

    private static final class CharArrayIndexFinder implements ChannelBufferIndexFinder {
        private char[] str;
        private int strlen;

        CharArrayIndexFinder(String str) {
            this.str = str.toCharArray();
            this.strlen = this.str.length;
        }

        @Override
        public boolean find(ChannelBuffer buffer, int guessedIndex) {
            if(guessedIndex + this.strlen > buffer.readableBytes()) return false;
            for(int i = 0; i < this.strlen; i++) {
                if(buffer.getByte(guessedIndex + i) != this.str[i]) return false;
            }
            return true;
        }
    }
}

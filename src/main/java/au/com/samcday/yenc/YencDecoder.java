package au.com.samcday.yenc;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.io.IOException;
import java.util.zip.CRC32;

import static au.com.samcday.jnntp.Util.pullAsciiHexNumberFromBuffer;
import static au.com.samcday.jnntp.Util.pullAsciiNumberFromBuffer;

/**
 * Specialized yEnc decoder that works off ChannelBuffers for better zero-copy efficiency. Code based on lpireyn/pi-yenc
 * It is expected that this decoder will be downstream from a LineBasedFrameDecoder.
 */
public class YencDecoder extends FrameDecoder {
    private static final byte BYTE_SPACE = 0x20;
    private static final byte BYTE_ESCAPE = 0x3d;
    private static final byte BYTE_KEYWORD_LINE = 0x79;
    private static final ChannelBufferIndexFinder BEGIN_INDEX_FINDER = new CharArrayIndexFinder("begin");
    private static final ChannelBufferIndexFinder END_INDEX_FINDER = new CharArrayIndexFinder("end");
    private static final ChannelBufferIndexFinder SIZE_INDEX_FINDER = new CharArrayIndexFinder("size");
    private static final ChannelBufferIndexFinder CRC32_INDEX_FINDER = new CharArrayIndexFinder("crc32");

    private ChannelBuffer decodeBuffer = null;
    private CRC32 crc32 = new CRC32();

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        // Is this a yEnc keyword line?
        buffer.markReaderIndex();
        if(buffer.readByte() == BYTE_ESCAPE && buffer.readByte() == BYTE_KEYWORD_LINE) {
            if(buffer.bytesBefore(BEGIN_INDEX_FINDER) == 0) {
                if(this.decodeBuffer != null) {
                    // Uhhhh....
                    throw new IOException("New yEnc block begun whilst decoding a previous one.");
                }

                // Let's go ahead and parse the header now.
                int size = this.parseHeader(buffer);
                if(size > -1) {
                    this.decodeBuffer = ChannelBuffers.dynamicBuffer(size);
                }
                else {
                    // TODO: any other hints we could be picking up to get some kind of decent estimate here?
                    this.decodeBuffer = ChannelBuffers.dynamicBuffer(4096);
                }
                this.crc32.reset();
                buffer.skipBytes(buffer.readableBytes());
                return null;
            }
            else if(buffer.bytesBefore(END_INDEX_FINDER) == 0) {
                if(this.decodeBuffer == null) {
                    // Uhhhh....
                    throw new IOException("yEnc block ended when we weren't decoding one.");
                }

                long expectedCRC = this.parseTrailer(buffer);
                if(expectedCRC > -1 && this.crc32.getValue() != expectedCRC) {
                    throw new YencChecksumFailureException();
                }

                ChannelBuffer ret = this.decodeBuffer;
                this.decodeBuffer = null;
                buffer.skipBytes(buffer.readableBytes());
                return ret;
            }
        }
        else {
            buffer.resetReaderIndex();

            // Are we currently decoding?
            if(this.decodeBuffer != null) {
                while(buffer.readable()) {
                    int b = buffer.readUnsignedByte();
                    if(b == BYTE_ESCAPE) {
                        if(!buffer.readable()) {
                            throw new IOException("EOL after escape byte.");
                        }
                        b = buffer.readUnsignedByte() - 64;
                    }
                    b = b - 42 & 0xff;
                    this.decodeBuffer.writeByte(b);
                    this.crc32.update(b);
                }
                return null;
            }

            // We're not decoding, and this isn't a yEnc keyword line, so we just pass the data thru.
            ChannelBuffer ret = buffer.copy();
            buffer.skipBytes(buffer.readableBytes());
            return ret;
        }

        return null;
    }

    /**
     * Parse the ybegin line and return the size hint, if it exists.
     * @param buffer
     * @return
     */
    private int parseHeader(ChannelBuffer buffer) {
        int sizeIndex = buffer.bytesBefore(SIZE_INDEX_FINDER);
        if(sizeIndex == -1) return -1;
        buffer.skipBytes(sizeIndex + 5); // + 5 for the "size="
        int endIndex = buffer.bytesBefore(BYTE_SPACE);
        if(endIndex == -1) endIndex = buffer.readableBytes();
        return pullAsciiNumberFromBuffer(buffer, endIndex);
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

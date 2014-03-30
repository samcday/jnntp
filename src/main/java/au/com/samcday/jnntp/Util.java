package au.com.samcday.jnntp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;

public class Util {
    /**
     * Reads len bytes from buffer and interprets as an ASCII-encoded number, returning an actual integer number.
     * Bails with -1 if number has invalid characters.
     * @param buffer
     * @param len
     * @return
     */
    public static final int pullAsciiIntFromBuffer(ByteBuf buffer, int len) {
        int b;
        int num = 0;
        for(; len > 0; len--) {
            b = buffer.readByte();
            if(b < 48 || b > 57) return -1;

            num += (b - 48) * Math.pow(10, len - 1);
        }

        return num;
    }

    public static final long pullAsciiLongFromBuffer(ByteBuf buffer, int len) {
        int b;
        long num = 0;
        for(; len > 0; len--) {
            b = buffer.readByte();
            if(b < 48 || b > 57) return -1;

            num += (b - 48) * Math.pow(10, len - 1);
        }

        return num;
    }

    public static final long pullAsciiHexNumberFromBuffer(ByteBuf buffer) {
        return pullAsciiHexNumberFromBuffer(buffer, buffer.readableBytes());
    }

    public static final long pullAsciiHexNumberFromBuffer(ByteBuf buffer, int len) {
        int b;
        long num = 0;
        for(; len > 0; len--) {
            b = buffer.readByte();
            if(b > 96 && b < 103) {
                b = b - 97 + 10; // -97 because lower case a starts there, +10 because 'a' comes after 9 in hex.
            }
            else if(b > 64 && b < 71) {
                b = b - 65 + 10; // same deal as above, but upper case.
            }
            else if(b > 47 && b < 58) {
                b = b - 48;
            }
            else {
                return -1;
            }

            num += b * Math.pow(16, len - 1);
        }

        return num;
    }

    public static class ByteBufStringIndexFinder implements ByteBufProcessor {
        private char[] str;
        private int strLen;
        private int strPos;

        public ByteBufStringIndexFinder(String str) {
            this.str = str.toCharArray();
            this.strLen = this.str.length;
            this.strPos = 0;
        }

        @Override
        public boolean process(byte value) throws Exception {
            if (value == this.str[this.strPos]) {
                this.strPos++;
                return this.strPos < this.strLen;
            }
            this.strPos = 0;
            return true;
        }
    }
}

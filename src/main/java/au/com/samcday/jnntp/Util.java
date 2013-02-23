package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public class Util {
    /**
     * Reads len bytes from buffer and interprets as an ASCII-encoded number, returning an actual integer number.
     * Bails with -1 if number has invalid characters.
     * @param buffer
     * @param len
     * @return
     */
    public static final int pullAsciiNumberFromBuffer(ChannelBuffer buffer, int len) {
        int b;
        int num = 0;
        for(; len > 0; len--) {
            b = buffer.readByte();
            if(b < 48 || b > 57) return -1;

            num += (b - 48) * Math.pow(10, len - 1);
        }

        return num;
    }

    public static final long pullAsciiHexNumberFromBuffer(ChannelBuffer buffer, int len) {
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
}

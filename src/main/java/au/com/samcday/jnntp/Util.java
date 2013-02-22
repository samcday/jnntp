package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public class Util {
    /**
     * Reads len bytes from buffer and interprets as an ASCII-encoded number, returning an actual integer number.
     * @param buffer
     * @param len
     * @return
     */
    public static final int pullAsciiNumberFromBuffer(ChannelBuffer buffer, int len) {
        byte b;
        int num = 0;
        for(; len > 0; len--) {
            num += (buffer.readByte() - 48) * Math.pow(10, len - 1);
        }

        return num;
    }
}

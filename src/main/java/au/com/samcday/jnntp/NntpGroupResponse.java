package au.com.samcday.jnntp;

import au.com.samcday.GroupInfo;
import org.jboss.netty.buffer.ChannelBuffer;

import static au.com.samcday.jnntp.Util.pullAsciiNumberFromBuffer;

public class NntpGroupResponse extends NntpResponse {
    public GroupInfo info;

    @Override
    public void process(ChannelBuffer buffer) {
        this.info = new GroupInfo();

        int countLen = buffer.bytesBefore((byte)0x20);
        this.info.count = pullAsciiNumberFromBuffer(buffer, countLen);
        buffer.skipBytes(1);
        int lowLen = buffer.bytesBefore((byte)0x20);
        this.info.low = pullAsciiNumberFromBuffer(buffer, lowLen);
        buffer.skipBytes(1);
        int highLen = buffer.bytesBefore((byte)0x20);
        this.info.high = pullAsciiNumberFromBuffer(buffer, highLen);
        buffer.skipBytes(1);
    }
}

package au.com.samcday.jnntp;

import io.netty.buffer.ByteBuf;

import static au.com.samcday.jnntp.Util.pullAsciiLongFromBuffer;

public class GroupResponse extends Response {
    public GroupInfo info;

    @Override
    public void process(ByteBuf buffer) {
        this.info = new GroupInfo();

        int countLen = buffer.bytesBefore((byte)0x20);
        this.info.count = pullAsciiLongFromBuffer(buffer, countLen);
        buffer.skipBytes(1);
        int lowLen = buffer.bytesBefore((byte)0x20);
        this.info.low = pullAsciiLongFromBuffer(buffer, lowLen);
        buffer.skipBytes(1);
        int highLen = buffer.bytesBefore((byte)0x20);
        this.info.high = pullAsciiLongFromBuffer(buffer, highLen);
        buffer.skipBytes(1);
    }
}

package au.com.samcday.jnntp;

import io.netty.buffer.ByteBuf;

public class RawResponseMessage {
    public int code;
    public ByteBuf buffer;
    public boolean multiline;

    public RawResponseMessage(int code, ByteBuf buffer, boolean multiline) {
        this.code = code;
        this.buffer = buffer;
        this.multiline = multiline;
    }
}

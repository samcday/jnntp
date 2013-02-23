package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public class RawResponseMessage {
    public int code;
    public ChannelBuffer buffer;
    public boolean multiline;

    public RawResponseMessage(int code, ChannelBuffer buffer, boolean multiline) {
        this.code = code;
        this.buffer = buffer;
        this.multiline = multiline;
    }
}

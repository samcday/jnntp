package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public abstract class NntpResponse {
    protected int code;

    public NntpResponse(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public abstract void process(ChannelBuffer buffer);

    public static enum ResponseType {
        WELCOME,
        DATE
    }
}

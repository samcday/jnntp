package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public abstract class NntpResponse {
    protected int code;

    public NntpResponse() {
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isMultiline() {
        return false;
    }

    public void processLine(ChannelBuffer buffer) {
        // Given empty implementation as it is not mandatory.
    }

    public abstract void process(ChannelBuffer buffer);


    public static enum ResponseType {
        WELCOME,
        DATE;
    }
}

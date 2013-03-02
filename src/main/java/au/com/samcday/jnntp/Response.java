package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public abstract class Response {
    protected int code;

    public Response() {
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void processLine(ChannelBuffer buffer) {
        // Given empty implementation as it is not mandatory.
    }

    public abstract void process(ChannelBuffer buffer);

    public static enum ResponseType {
        WELCOME,
        AUTHINFO,
        DATE,
        LIST,
        GROUP,
        OVER,
        XOVER,
        XZVER,
        BODY;
    }
}

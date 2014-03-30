package au.com.samcday.jnntp;

import io.netty.buffer.ByteBuf;

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

    public void processLine(ByteBuf buffer) {
        // Given empty implementation as it is not mandatory.
    }

    public abstract void process(ByteBuf buffer);

}

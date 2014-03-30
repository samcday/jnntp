package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;

public class GenericResponse extends Response {
    private String message;

    @Override
    public void process(ByteBuf buffer) {
        this.message = buffer.toString(Charsets.UTF_8);
    }

    public String getMessage() {
        return message;
    }
}

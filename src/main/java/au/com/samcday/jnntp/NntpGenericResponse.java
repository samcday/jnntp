package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;

public class NntpGenericResponse extends NntpResponse {
    private String message;

    @Override
    public void process(ChannelBuffer buffer) {
        this.message = buffer.toString(Charsets.UTF_8);
    }

    public String getMessage() {
        return message;
    }
}

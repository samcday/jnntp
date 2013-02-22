package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public class NntpWelcomeResponse extends NntpResponse {
    public static final ResponseType TYPE = ResponseType.WELCOME;

    public NntpWelcomeResponse(int code) {
        super(code);
    }

    @Override
    public void process(ChannelBuffer buffer) {
    }
}

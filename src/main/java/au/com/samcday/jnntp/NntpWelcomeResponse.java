package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public class NntpWelcomeResponse extends NntpResponse {
    public NntpWelcomeResponse(int code) {
        super(code);
    }

    @Override
    public void process(ChannelBuffer buffer) {
    }
}

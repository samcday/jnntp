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
        DATE;

        public NntpResponse construct(int code) {
            switch(this) {
                case WELCOME:
                    return new NntpWelcomeResponse(code);
                case DATE:
                    return new NntpDateResponse(code);
                default:
                    return null;
            }
        }
    }
}

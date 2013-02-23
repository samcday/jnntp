package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

public class NntpOverviewResponse extends NntpResponse {
    @Override
    public void process(ChannelBuffer buffer) {

    }

    @Override
    public boolean isMultiline() {
        return this.code == 224;
    }

    @Override
    public void processLine(ChannelBuffer buffer) {

    }
}

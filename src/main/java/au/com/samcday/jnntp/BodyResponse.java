package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class BodyResponse extends Response {
    public CompositeChannelBufferInputStream stream;

    public BodyResponse() {
        this.stream = new CompositeChannelBufferInputStream();
    }

    @Override
    public void process(ChannelBuffer buffer) {}

    @Override
    public void processLine(ChannelBuffer buffer) {
        if(buffer == null) {
            this.stream.addData(ChannelBuffers.EMPTY_BUFFER);
        }
        else {
            this.stream.addData(buffer);
        }
    }
}

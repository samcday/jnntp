package au.com.samcday.jnntp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class BodyResponse extends Response {
    public CompositeChannelBufferInputStream stream;

    public BodyResponse() {
        this.stream = new CompositeChannelBufferInputStream();
    }

    @Override
    public void process(ByteBuf buffer) {}

    @Override
    public void processLine(ByteBuf buffer) {
        if(buffer == null) {
            this.stream.addData(Unpooled.EMPTY_BUFFER);
        }
        else {
            this.stream.addData(buffer);
        }
    }
}

package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;

import java.nio.charset.Charset;

public class OverviewResponse extends Response {
    public OverviewList list;

    @Override
    public void process(ChannelBuffer buffer) {

    }

    @Override
    public void processLine(ChannelBuffer buffer) {
        System.out.println("oh cool.");
        System.out.println(buffer.toString(Charset.defaultCharset()));
    }
}

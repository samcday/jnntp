package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;

import java.util.ArrayList;
import java.util.List;

import static au.com.samcday.jnntp.Util.pullAsciiNumberFromBuffer;

public class NntpListResponse extends NntpResponse {
    private List<GroupListItem> items;

    public NntpListResponse() {
        this.items = new ArrayList<>();
    }

    @Override
    public boolean isMultiline() {
        return this.code == 215;
    }

    @Override
    public void process(ChannelBuffer buffer) {
    }

    @Override
    public void processLine(ChannelBuffer buffer) {
        int groupLen = buffer.bytesBefore((byte)0x20);
        String group = buffer.toString(0, groupLen, Charsets.UTF_8);
        buffer.skipBytes(groupLen + 1);
        int highLen = buffer.bytesBefore((byte)0x20);
        int high = pullAsciiNumberFromBuffer(buffer, highLen);
        buffer.skipBytes(1);
        int lowLen = buffer.bytesBefore((byte)0x20);
        int low = pullAsciiNumberFromBuffer(buffer, lowLen);
        buffer.skipBytes(1);
        this.items.add(new GroupListItem(group, low, high));
    }

    public List<GroupListItem> getItems() {
        return items;
    }
}

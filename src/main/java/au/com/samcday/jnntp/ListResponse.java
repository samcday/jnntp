package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import static au.com.samcday.jnntp.Util.pullAsciiIntFromBuffer;

public class ListResponse extends Response {
    private List<GroupListItem> items;

    public ListResponse() {
        this.items = new ArrayList<>();
    }

    @Override
    public void process(ByteBuf buffer) {
    }

    @Override
    public void processLine(ByteBuf buffer) {
        int groupLen = buffer.bytesBefore((byte)0x20);
        String group = buffer.toString(0, groupLen, Charsets.UTF_8);
        buffer.skipBytes(groupLen + 1);
        int highLen = buffer.bytesBefore((byte)0x20);
        int high = pullAsciiIntFromBuffer(buffer, highLen);
        buffer.skipBytes(1);
        int lowLen = buffer.bytesBefore((byte)0x20);
        int low = pullAsciiIntFromBuffer(buffer, lowLen);
        buffer.skipBytes(1);
        this.items.add(new GroupListItem(group, low, high));
    }

    public List<GroupListItem> getItems() {
        return items;
    }
}

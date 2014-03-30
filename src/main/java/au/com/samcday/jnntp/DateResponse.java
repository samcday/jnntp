package au.com.samcday.jnntp;

import io.netty.buffer.ByteBuf;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Date;

import static au.com.samcday.jnntp.Util.pullAsciiIntFromBuffer;

public class DateResponse extends Response {
    private Date date;

    public Date getDate() {
        return date;
    }

    @Override
    public void process(ByteBuf buffer) {
        this.date = new DateTime(
            pullAsciiIntFromBuffer(buffer, 4), // yyyy
            pullAsciiIntFromBuffer(buffer, 2), // mm
            pullAsciiIntFromBuffer(buffer, 2), // dd
            pullAsciiIntFromBuffer(buffer, 2), // hh
            pullAsciiIntFromBuffer(buffer, 2), // mm
            pullAsciiIntFromBuffer(buffer, 2), // ss
            DateTimeZone.UTC
        ).toDate();
    }
}

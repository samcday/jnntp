package au.com.samcday.jnntp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Date;

import static au.com.samcday.jnntp.Util.pullAsciiNumberFromBuffer;

public class DateResponse extends Response {
    private Date date;

    public Date getDate() {
        return date;
    }

    @Override
    public void process(ChannelBuffer buffer) {
        this.date = new DateTime(
            pullAsciiNumberFromBuffer(buffer, 4), // yyyy
            pullAsciiNumberFromBuffer(buffer, 2), // mm
            pullAsciiNumberFromBuffer(buffer, 2), // dd
            pullAsciiNumberFromBuffer(buffer, 2), // hh
            pullAsciiNumberFromBuffer(buffer, 2), // mm
            pullAsciiNumberFromBuffer(buffer, 2), // ss
            DateTimeZone.UTC
        ).toDate();
    }
}

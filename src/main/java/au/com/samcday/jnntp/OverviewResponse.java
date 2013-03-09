package au.com.samcday.jnntp;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.jboss.netty.buffer.ChannelBuffer;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class OverviewResponse extends Response {
    private static final Splitter TAB_SPLITTER = Splitter.on("\t");
    private static final List<DateTimeFormatter> DATE_FORMATS = Lists.newArrayList(
        DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z"),
        DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZoneUTC(),
        DateTimeFormat.forPattern("dd MMM yyyy HH:mm:ss 'GMT'").withZoneUTC()
    );

    public OverviewList list;
    private LinkedBlockingQueue<Overview> items;

    public OverviewResponse() {
        this.items = new LinkedBlockingQueue<>();
        this.list = new OverviewList(this.items);
    }

    @Override
    public void process(ChannelBuffer buffer) {
    }

    @Override
    public void processLine(ChannelBuffer buffer) {
        if(buffer == null) {
            this.items.offer(Overview.END);
            return;
        }

        Iterator<String> parts = TAB_SPLITTER.split(new ChannelBufferCharSequence(buffer)).iterator();

        long articleNum = Long.parseLong(parts.next());
        String subject = parts.next();
        String from = parts.next();
        Date date = this.parseDate(parts.next());
        String msgId = parts.next();
        String ref = parts.next();
        int bytes = Integer.parseInt(parts.next());
        int lines = Integer.parseInt(parts.next());

        this.items.offer(new Overview(articleNum, subject, from, date, msgId, ref, bytes, lines));
    }

    private Date parseDate(String str) {
        if(str == null) return null;

        for(DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return new Date(formatter.parseMillis(str));
            }
            catch(IllegalArgumentException iae) {
            }
        }

        throw new RuntimeException("Couldn't parse date " + str);
    }
}

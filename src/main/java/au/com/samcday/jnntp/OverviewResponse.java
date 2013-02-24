package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import org.jboss.netty.buffer.ChannelBuffer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class OverviewResponse extends Response {
    private static final List<DateTimeFormatter> DATE_FORMATS = Lists.newArrayList(
        DateTimeFormat.forPattern("EEE, dd MMM yyyy kk:mm:ss Z"),
        DateTimeFormat.forPattern("dd MMM yyyy kk:mm:ss 'GMT'").withZoneUTC()
    );

    public OverviewList list;
    private Iterator<Overview> iterator;
    private boolean done;
    private LinkedBlockingQueue<Overview> items;

    public OverviewResponse() {
        this.iterator = new OverviewIterator();
        this.list = new OverviewList(this.iterator);
        this.items = new LinkedBlockingQueue<>();
    }

    @Override
    public void process(ChannelBuffer buffer) {
    }

    @Override
    public void processLine(ChannelBuffer buffer) {
        if(buffer == null) {
            this.done = true;
            return;
        }

        Iterator<String> parts = Splitter.on("\t").split(buffer.toString(Charsets.UTF_8)).iterator();

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
        DateTime date = null;

        for(DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                date = formatter.parseDateTime(str);
            }
            catch(IllegalArgumentException iae) {
            }

            if(date != null) return date.toDate();
        }

        return null;
    }

    private class OverviewIterator extends AbstractIterator<Overview> {
        @Override
        protected Overview computeNext() {
            if(done && items.size() == 0) {
                return this.endOfData();
            }

            while(true) {
                try {
                    return items.take();
                }
                catch(InterruptedException ie) {

                }
            }
        }
    }
}

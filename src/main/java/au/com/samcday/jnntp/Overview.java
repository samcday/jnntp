package au.com.samcday.jnntp;

import java.util.Date;

public class Overview {
    private String subject;
    private String from;
    private Date date;
    private String messageId;
    private String references;
    private int bytes;
    private int lines;

    public String getSubject() {
        return subject;
    }

    public String getFrom() {
        return from;
    }

    public Date getDate() {
        return date;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getReferences() {
        return references;
    }

    public int getBytes() {
        return bytes;
    }

    public int getLines() {
        return lines;
    }
}

package au.com.samcday.jnntp;

import java.util.Date;

public class Overview {
    private long article;
    private String subject;
    private String from;
    private Date date;
    private String messageId;
    private String references;
    private int bytes;
    private int lines;

    public Overview(long article, String subject, String from, Date date, String messageId, String references, int bytes, int lines) {
        this.article = article;
        this.subject = subject;
        this.from = from;
        this.date = date;
        this.messageId = messageId;
        this.references = references;
        this.bytes = bytes;
        this.lines = lines;
    }

    public long getArticle() {
        return article;
    }

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

package au.com.samcday.jnntp.exceptions;

public class NntpInvalidLoginException extends Exception {
    public NntpInvalidLoginException() {
        super("Provided authentication details were rejected by server.");
    }
}

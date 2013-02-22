package au.com.samcday.jnntp.exceptions;

public class NntpClientConnectionError extends Exception {
    public NntpClientConnectionError(Throwable cause) {
        super("Could not connect to NNTP server.", cause);
    }
}

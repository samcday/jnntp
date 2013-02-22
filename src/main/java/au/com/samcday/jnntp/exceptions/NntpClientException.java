package au.com.samcday.jnntp.exceptions;

/**
 * Represents a generic exception when interacting with the NNTP server.
 */
public class NntpClientException extends Exception {
    public NntpClientException(String message) {
        super(message);
    }
}

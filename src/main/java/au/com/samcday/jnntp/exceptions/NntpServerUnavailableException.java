package au.com.samcday.jnntp.exceptions;

public class NntpServerUnavailableException extends Exception {
    private boolean temporary;

    public NntpServerUnavailableException(boolean temporary) {
        super("Server is " + (temporary ? "temporarily" : "permanently") + " unavailable.");
        this.temporary = temporary;
    }

    public boolean isTemporary() {
        return temporary;
    }
}

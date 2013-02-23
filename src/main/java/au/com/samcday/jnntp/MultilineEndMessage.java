package au.com.samcday.jnntp;

/**
 * Marker message sent upstream from {@link ResponseDecoder} when a multiline message has finished transmitting.
 */
public class MultilineEndMessage {
    public static final MultilineEndMessage INSTANCE = new MultilineEndMessage();

    private MultilineEndMessage() { }
}

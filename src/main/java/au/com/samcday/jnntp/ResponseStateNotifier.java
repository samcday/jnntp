package au.com.samcday.jnntp;

/**
 * Implementation of this interface will be used to determine if an incoming response is multiline.
 */
public interface ResponseStateNotifier {
    public boolean isMultiline(int code);
}

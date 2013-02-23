package au.com.samcday.jnntp;

/**
 * When decoding responses from the server, the {@link ResponseDecoder} needs to know the type of the command in
 * some cases in order to determine if the response should be parsed as a multi line or not. It relies on an
 * implementation of this interface to peek at the head of command pipeline and return the type of command instigated
 * the response.
 */
public interface CommandPipelinePeeker {
    public NntpResponse.ResponseType peekType();
}

package au.com.samcday.jnntp;

public interface NntpResponseFactory {
    public NntpResponse newResponse(NntpResponse.ResponseType type);
}

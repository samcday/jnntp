package au.com.samcday.jnntp;

public class NntpResponseFactoryImpl implements NntpResponseFactory {
    @Override
    public NntpResponse newResponse(NntpResponse.ResponseType type) {
        NntpResponse response;
        switch(type) {
            case WELCOME:
                response = new NntpWelcomeResponse();
                break;
            case DATE:
                response = new NntpDateResponse();
                break;
            default:
                return null;
        }

        return response;
    }
}

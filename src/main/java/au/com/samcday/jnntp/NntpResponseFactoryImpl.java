package au.com.samcday.jnntp;

public class NntpResponseFactoryImpl implements NntpResponseFactory {
    @Override
    public NntpResponse newResponse(NntpResponse.ResponseType type) {
        NntpResponse response;
        switch(type) {
            case WELCOME:
            case AUTHINFO:
                response = new NntpGenericResponse();
                break;
            case DATE:
                response = new NntpDateResponse();
                break;
            case LIST:
                response = new NntpListResponse();
                break;
            default:
                return null;
        }

        return response;
    }
}

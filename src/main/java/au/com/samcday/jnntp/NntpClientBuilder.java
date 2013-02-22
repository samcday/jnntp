package au.com.samcday.jnntp;

import au.com.samcday.jnntp.exceptions.NntpClientAuthenticationException;
import au.com.samcday.jnntp.exceptions.NntpClientConnectionError;

public class NntpClientBuilder {
    private String host;
    private int port = 119;
    private String username;
    private String password;

    public static NntpClientBuilder nntpClient(String host) {
        return new NntpClientBuilder(host);
    }

    protected NntpClientBuilder(String host) {
        this.host = host;
    }

    public NntpClientBuilder port(int port) {
        this.port = port;
        return this;
    }

    public NntpClientBuilder auth(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public NntpClient build() throws NntpClientConnectionError, NntpClientAuthenticationException {
        NntpClient client = new NntpClient(this.host, this.port);

        client.connect();

        if(this.username != null) {
            client.authenticate(this.username, this.password);
        }

        return client;
    }
}

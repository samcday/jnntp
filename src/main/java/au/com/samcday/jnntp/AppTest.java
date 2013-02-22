package au.com.samcday.jnntp;

import static au.com.samcday.jnntp.NntpClientBuilder.nntpClient;

public class AppTest {
    public static final void main(String... args) throws Exception {
        NntpClient client = nntpClient("news.astraweb.com")
            .build();

        System.out.println(client.date());
    }
}

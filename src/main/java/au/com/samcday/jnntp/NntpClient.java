package au.com.samcday.jnntp;

import au.com.samcday.jnntp.bandwidth.BandwidthHandler;
import au.com.samcday.jnntp.bandwidth.HandlerRegistration;
import au.com.samcday.jnntp.exceptions.NntpClientAuthenticationException;
import au.com.samcday.jnntp.exceptions.NntpClientConnectionError;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

public interface NntpClient {
    void connect() throws NntpClientConnectionError;

    void disconnect();

    void authenticate(String username, String password) throws NntpClientAuthenticationException;

    Date date();

    List<GroupListItem> list();

    GroupInfo group(String name);

    OverviewList overview(long start, long end);

    InputStream body(String messageId);

    HandlerRegistration registerBandwidthHandler(BandwidthHandler handler);
}

package au.com.samcday.jnntp.bandwidth;

public interface BandwidthHandler {
    public void update(long read, long written);
}

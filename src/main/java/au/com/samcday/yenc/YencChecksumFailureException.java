package au.com.samcday.yenc;

import java.io.IOException;

public class YencChecksumFailureException extends IOException {
    public YencChecksumFailureException() {
        super("CRC checksum error in yEnc block.");
    }
}

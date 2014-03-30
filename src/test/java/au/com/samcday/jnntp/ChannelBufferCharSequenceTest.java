package au.com.samcday.jnntp;

import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.fest.assertions.api.Assertions.assertThat;

public class ChannelBufferCharSequenceTest {
    @Test
    public void testBasic() {
        CharSequence seq = new ByteBufCharSequence(Unpooled.copiedBuffer("Hello, World!", Charset.defaultCharset()));
        assertThat(seq.length()).isEqualTo(13);
        assertThat(seq.charAt(0)).isEqualTo('H');
        assertThat(seq.charAt(12)).isEqualTo('!');
    }

    @Test
    public void testSubSequence() {
        CharSequence seq = new ByteBufCharSequence(Unpooled.copiedBuffer("Hello, World!", Charset.defaultCharset()));
        CharSequence sub = seq.subSequence(7, 12);
        assertThat(sub.length()).isEqualTo(5);
        assertThat(sub.charAt(0)).isEqualTo('W');
        assertThat(sub.charAt(4)).isEqualTo('d');
    }
}

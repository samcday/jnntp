package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class ChannelBufferMatchers {
    public static final ExactChannelBufferMatcher exactStringChannelBuffer(String expected) {
        return exactChannelBuffer(ChannelBuffers.copiedBuffer(expected, Charsets.UTF_8));
    }

    public static final ExactChannelBufferMatcher exactChannelBuffer(ChannelBuffer expected) {
        return new ExactChannelBufferMatcher(expected);
    }

    private static class ExactChannelBufferMatcher extends TypeSafeMatcher<ChannelBuffer> {
        private ChannelBuffer expected;

        private ExactChannelBufferMatcher(ChannelBuffer expected) {
            this.expected = expected;
        }

        @Override
        protected boolean matchesSafely(ChannelBuffer channelBuffer) {
            if(channelBuffer.capacity() != this.expected.capacity()) return false;

            for(int i = 0; i < channelBuffer.capacity(); i++) {
                if(channelBuffer.readByte() != this.expected.readByte()) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a ChannelBuffer with specific contents");
        }
    }
}

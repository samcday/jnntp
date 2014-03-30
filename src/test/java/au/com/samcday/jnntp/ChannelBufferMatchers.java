package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ChannelBufferMatchers {
    public static final ExactChannelBufferMatcher exactStringChannelBuffer(String expected) {
        return exactChannelBuffer(Unpooled.copiedBuffer(expected, Charsets.UTF_8));
    }

    public static final ExactChannelBufferMatcher exactChannelBuffer(ByteBuf expected) {
        return new ExactChannelBufferMatcher(expected);
    }

    private static class ExactChannelBufferMatcher extends TypeSafeMatcher<ByteBuf> {
        private ByteBuf expected;

        private ExactChannelBufferMatcher(ByteBuf expected) {
            this.expected = expected;
        }

        @Override
        protected boolean matchesSafely(ByteBuf channelBuffer) {
            if(channelBuffer.readableBytes() != this.expected.readableBytes()) return false;

            while(channelBuffer.isReadable()) {
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

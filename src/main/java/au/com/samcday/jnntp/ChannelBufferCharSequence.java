package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * A wrapper to view a ChannelBuffer as a CharSequence. Will only work if underlying ChannelBuffer is UTF8-encoded.
 * Respects readerIndex.
 */
public class ChannelBufferCharSequence implements CharSequence {
    private final ChannelBuffer underlying;

    public ChannelBufferCharSequence(ChannelBuffer underlying) {
        this.underlying = underlying;
    }

    @Override
    public int length() {
        return this.underlying.readableBytes();
    }

    @Override
    public char charAt(int index) {
        return (char)this.underlying.getByte(this.underlying.readerIndex() + index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new ChannelBufferCharSequence(this.underlying.slice(this.underlying.readerIndex() + start,
            this.underlying.readerIndex() + (end - start)));
    }

    @Override
    public String toString() {
        return this.underlying.toString(Charsets.UTF_8);
    }
}

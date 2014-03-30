package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;

/**
 * A wrapper to view a ByteBuf as a CharSequence. Will only work if underlying ByteBuf is UTF8-encoded.
 * Respects readerIndex.
 */
public class ByteBufCharSequence implements CharSequence {
    private final ByteBuf underlying;

    public ByteBufCharSequence(ByteBuf underlying) {
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
        return new ByteBufCharSequence(this.underlying.slice(this.underlying.readerIndex() + start,
            this.underlying.readerIndex() + (end - start)));
    }

    @Override
    public String toString() {
        return this.underlying.toString(Charsets.UTF_8);
    }
}

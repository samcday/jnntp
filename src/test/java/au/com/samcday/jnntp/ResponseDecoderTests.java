package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.junit.Before;
import org.junit.Test;

import static au.com.samcday.jnntp.ChannelBufferMatchers.exactStringChannelBuffer;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResponseDecoderTests {
    private DecoderEmbedder<Object> decoderEmbedder;
    private ResponseStateNotifier mockResponseStateNotifier;

    @Before
    public void setup() {
        this.mockResponseStateNotifier = mock(ResponseStateNotifier.class);
        this.decoderEmbedder = new DecoderEmbedder<>(
            new ResponseDecoder(this.mockResponseStateNotifier));
    }

    @Test
    public void testBasicDecode() {
        assertTrue(this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("200 This is a simple response", Charsets.UTF_8)));
        assertTrue(this.decoderEmbedder.finish());
        assertThat(this.decoderEmbedder.peek(), instanceOf(RawResponseMessage.class));
        RawResponseMessage raw = (RawResponseMessage)this.decoderEmbedder.poll();
        assertEquals(200, raw.code);
        assertThat(raw.buffer, exactStringChannelBuffer("This is a simple response"));
    }

    @Test
    public void testMultilineDecode() {
        when(this.mockResponseStateNotifier.isMultiline(200)).thenReturn(true);

        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("200 Multiline kgo", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("Line 1", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("Line 2", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer(".", Charsets.UTF_8));
        this.decoderEmbedder.finish();

        // First thing out should be a RawResponseMessage.
        assertThat(this.decoderEmbedder.peek(), instanceOf(RawResponseMessage.class));
        RawResponseMessage raw = (RawResponseMessage)this.decoderEmbedder.poll();
        assertThat(raw.code, is(200));
        assertTrue(raw.multiline);
        assertThat(raw.buffer, exactStringChannelBuffer("Multiline kgo"));

        assertThat(this.decoderEmbedder.peek(), instanceOf(ChannelBuffer.class));
        assertThat((ChannelBuffer)this.decoderEmbedder.poll(), exactStringChannelBuffer("Line 1"));

        assertThat(this.decoderEmbedder.peek(), instanceOf(ChannelBuffer.class));
        assertThat((ChannelBuffer)this.decoderEmbedder.poll(), exactStringChannelBuffer("Line 2"));

        assertThat(this.decoderEmbedder.poll(), is((Object) MultilineEndMessage.INSTANCE));
    }

    @Test
    public void testDotStuffedMultilineDecode() {
        when(this.mockResponseStateNotifier.isMultiline(200)).thenReturn(true);

        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("200 Multiline kgo", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("....", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer(".", Charsets.UTF_8));
        this.decoderEmbedder.finish();

        this.decoderEmbedder.poll();

        assertThat((ChannelBuffer)this.decoderEmbedder.poll(), exactStringChannelBuffer("..."));
    }
}

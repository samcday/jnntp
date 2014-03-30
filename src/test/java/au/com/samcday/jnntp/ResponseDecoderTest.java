package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static au.com.samcday.jnntp.ChannelBufferMatchers.exactStringChannelBuffer;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResponseDecoderTest {
    private EmbeddedChannel embeddedChannel;
    private ResponseStateNotifier mockResponseStateNotifier;

    @Before
    public void setup() {
        this.mockResponseStateNotifier = mock(ResponseStateNotifier.class);
        this.embeddedChannel = new EmbeddedChannel(
            new ResponseDecoder(this.mockResponseStateNotifier));
    }

    @Test
    public void testBasicDecode() {
        assertTrue(this.embeddedChannel.writeInbound(Unpooled.copiedBuffer("200 This is a simple response", Charsets.UTF_8)));
        assertTrue(this.embeddedChannel.finish());
        Object outbound = this.embeddedChannel.readInbound();
        assertThat(outbound, instanceOf(RawResponseMessage.class));
        RawResponseMessage raw = (RawResponseMessage)outbound;
        assertEquals(200, raw.code);
        assertThat(raw.buffer, exactStringChannelBuffer("This is a simple response"));
    }

    @Test
    public void testMultilineDecode() {
        when(this.mockResponseStateNotifier.isMultiline(200)).thenReturn(true);

        this.embeddedChannel.writeInbound(Unpooled.copiedBuffer("200 Multiline kgo", Charsets.UTF_8));
        this.embeddedChannel.writeInbound(Unpooled.copiedBuffer("Line 1", Charsets.UTF_8));
        this.embeddedChannel.writeInbound(Unpooled.copiedBuffer("Line 2", Charsets.UTF_8));
        this.embeddedChannel.writeInbound(Unpooled.copiedBuffer(".", Charsets.UTF_8));
        this.embeddedChannel.finish();

        // First thing out should be a RawResponseMessage.
        Object outbound = this.embeddedChannel.readInbound();
        assertThat(outbound, instanceOf(RawResponseMessage.class));
        RawResponseMessage raw = (RawResponseMessage)outbound;
        assertThat(raw.code, is(200));
        assertTrue(raw.multiline);
        assertThat(raw.buffer, exactStringChannelBuffer("Multiline kgo"));

        outbound = this.embeddedChannel.readInbound();
        assertThat(outbound, instanceOf(ByteBuf.class));
        assertThat((ByteBuf)outbound, exactStringChannelBuffer("Line 1"));

        outbound = this.embeddedChannel.readInbound();
        assertThat(outbound, instanceOf(ByteBuf.class));
        assertThat((ByteBuf)outbound, exactStringChannelBuffer("Line 2"));

        assertThat(this.embeddedChannel.readInbound(), is((Object) MultilineEndMessage.INSTANCE));
    }

    @Test
    public void testDotStuffedMultilineDecode() {
        when(this.mockResponseStateNotifier.isMultiline(200)).thenReturn(true);

        this.embeddedChannel.writeInbound(Unpooled.copiedBuffer("200 Multiline kgo", Charsets.UTF_8));
        this.embeddedChannel.writeInbound(Unpooled.copiedBuffer("....", Charsets.UTF_8));
        this.embeddedChannel.writeInbound(Unpooled.copiedBuffer(".", Charsets.UTF_8));
        this.embeddedChannel.finish();

        this.embeddedChannel.readInbound();

        assertThat((ByteBuf) this.embeddedChannel.readInbound(), exactStringChannelBuffer("..."));
    }
}

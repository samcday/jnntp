package au.com.samcday.jnntp;

import au.com.samcday.yenc.YencChecksumFailureException;
import au.com.samcday.yenc.YencDecoder;
import com.google.common.io.Resources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LineBasedFrameDecoder;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static au.com.samcday.jnntp.ChannelBufferMatchers.exactChannelBuffer;
import static org.junit.Assert.assertThat;

public class YencDecoderTests {
    @Test
    public void testDecode() throws IOException {
        EmbeddedChannel decoderEmbedder = new EmbeddedChannel(new LineBasedFrameDecoder(4096), new YencDecoder());

        ByteBuf encoded = Unpooled.buffer();
        IOUtils.copy(Resources.getResource("lorem-ipsum.ync").openStream(), new ByteBufOutputStream(encoded));

        ByteBuf original = Unpooled.buffer();
        IOUtils.copy(Resources.getResource("lorem-ipsum").openStream(), new ByteBufOutputStream(original));

        decoderEmbedder.writeInbound(encoded);
        decoderEmbedder.finish();

        Object[] result = decoderEmbedder.inboundMessages().toArray();
        ByteBuf[] buffers = Arrays.copyOf(result, result.length, ByteBuf[].class);
        assertThat(Unpooled.copiedBuffer(buffers), exactChannelBuffer(original));
    }

    @Test(expected = YencChecksumFailureException.class)
    public void testChecksumFailure() throws Throwable {
        EmbeddedChannel decoderEmbedder = new EmbeddedChannel(new LineBasedFrameDecoder(4096), new YencDecoder());

        ByteBuf encoded = Unpooled.buffer();
        IOUtils.copy(Resources.getResource("lorem-ipsum-invalid-checksum.ync").openStream(), new ByteBufOutputStream(encoded));

        ByteBuf original = Unpooled.buffer();
        IOUtils.copy(Resources.getResource("lorem-ipsum").openStream(), new ByteBufOutputStream(original));

        try {
            decoderEmbedder.writeInbound(encoded);
            decoderEmbedder.finish();
        }
        catch(DecoderException cee) {
            throw cee.getCause();
        }
    }
}

package au.com.samcday.jnntp;

import au.com.samcday.yenc.YencChecksumFailureException;
import au.com.samcday.yenc.YencDecoder;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.CodecEmbedderException;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;
import org.junit.Test;

import java.io.IOException;

import static au.com.samcday.jnntp.ChannelBufferMatchers.exactChannelBuffer;
import static org.junit.Assert.assertThat;

public class YencDecoderTests {
    @Test
    public void testDecode() throws IOException {
        DecoderEmbedder<ChannelBufferMatchers> decoderEmbedder = new DecoderEmbedder<>(new LineBasedFrameDecoder(4096), new YencDecoder());

        ChannelBuffer encoded = ChannelBuffers.dynamicBuffer();
        IOUtils.copy(Resources.getResource("lorem-ipsum.ync").openStream(), new ChannelBufferOutputStream(encoded));

        ChannelBuffer original = ChannelBuffers.dynamicBuffer();
        IOUtils.copy(Resources.getResource("lorem-ipsum").openStream(), new ChannelBufferOutputStream(original));

        decoderEmbedder.offer(encoded);
        decoderEmbedder.finish();

        assertThat((ChannelBuffer)decoderEmbedder.poll(), exactChannelBuffer(original));
    }

    @Test(expected = YencChecksumFailureException.class)
    public void testChecksumFailure() throws Throwable {
        DecoderEmbedder<ChannelBufferMatchers> decoderEmbedder = new DecoderEmbedder<>(new LineBasedFrameDecoder(4096), new YencDecoder());

        ChannelBuffer encoded = ChannelBuffers.dynamicBuffer();
        IOUtils.copy(Resources.getResource("lorem-ipsum-invalid-checksum.ync").openStream(), new ChannelBufferOutputStream(encoded));

        ChannelBuffer original = ChannelBuffers.dynamicBuffer();
        IOUtils.copy(Resources.getResource("lorem-ipsum").openStream(), new ChannelBufferOutputStream(original));

        try {
            decoderEmbedder.offer(encoded);
            decoderEmbedder.finish();
        }
        catch(CodecEmbedderException cee) {
            throw cee.getCause();
        }

        assertThat((ChannelBuffer)decoderEmbedder.poll(), exactChannelBuffer(original));
    }
}

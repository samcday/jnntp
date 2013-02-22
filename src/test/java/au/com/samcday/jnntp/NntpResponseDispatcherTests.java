package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NntpResponseDispatcherTests {
    private DecoderEmbedder<NntpResponse> decoderEmbedder;
    private CommandPipelinePeeker mockPipelinePeeker;
    private NntpResponseFactory mockResponseFactory;

    @Before
    public void setup() {
        this.mockResponseFactory = mock(NntpResponseFactory.class);
        this.mockPipelinePeeker = mock(CommandPipelinePeeker.class);
        this.decoderEmbedder = new DecoderEmbedder<>(
            new NntpResponseDecoder(this.mockPipelinePeeker, this.mockResponseFactory));
    }

    @Test
    public void testBasicDecode() {
        when(this.mockPipelinePeeker.peekType()).thenReturn(NntpResponse.ResponseType.WELCOME);
        when(this.mockResponseFactory.newResponse(NntpResponse.ResponseType.WELCOME)).thenReturn(new NntpWelcomeResponse());

        assertTrue(this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("200 This is a simple response", Charsets.UTF_8)));
        assertTrue(this.decoderEmbedder.finish());
        assertEquals(200, this.decoderEmbedder.poll().code);
    }
}

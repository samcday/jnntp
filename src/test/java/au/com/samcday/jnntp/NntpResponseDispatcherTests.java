package au.com.samcday.jnntp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

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
        when(this.mockResponseFactory.newResponse(NntpResponse.ResponseType.WELCOME)).thenReturn(new NntpGenericResponse());

        assertTrue(this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("200 This is a simple response", Charsets.UTF_8)));
        assertTrue(this.decoderEmbedder.finish());
        assertEquals(200, this.decoderEmbedder.poll().code);
    }

    @Test
    public void testMultilineDecode() {
        NntpResponse mockResponse = mock(NntpResponse.class);
        when(mockResponse.isMultiline()).thenReturn(true);

        when(this.mockPipelinePeeker.peekType()).thenReturn(NntpResponse.ResponseType.WELCOME);
        when(this.mockResponseFactory.newResponse(NntpResponse.ResponseType.WELCOME)).thenReturn(mockResponse);

        final List<String> lines = new ArrayList<>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                lines.add(((ChannelBuffer)invocationOnMock.getArguments()[0]) .toString(Charsets.UTF_8));
                return null;
            }
        }).when(mockResponse).processLine(any(ChannelBuffer.class));

        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("200 Multiline kgo", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("Line 1", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("Line 2", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer(".", Charsets.UTF_8));
        this.decoderEmbedder.finish();

        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
    }

    @Test
    public void testDotStuffedMultilineDecode() {
        NntpResponse mockResponse = mock(NntpResponse.class);
        when(mockResponse.isMultiline()).thenReturn(true);

        when(this.mockPipelinePeeker.peekType()).thenReturn(NntpResponse.ResponseType.WELCOME);
        when(this.mockResponseFactory.newResponse(NntpResponse.ResponseType.WELCOME)).thenReturn(mockResponse);

        final List<String> lines = new ArrayList<>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                lines.add(((ChannelBuffer) invocationOnMock.getArguments()[0]).toString(Charsets.UTF_8));
                return null;
            }
        }).when(mockResponse).processLine(any(ChannelBuffer.class));

        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("200 Multiline kgo", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("Line 1", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer("....", Charsets.UTF_8));
        this.decoderEmbedder.offer(ChannelBuffers.copiedBuffer(".", Charsets.UTF_8));
        this.decoderEmbedder.finish();

        assertEquals("Line 1", lines.get(0));
        assertEquals("...", lines.get(1));
    }
}

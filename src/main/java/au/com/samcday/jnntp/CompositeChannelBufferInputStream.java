package au.com.samcday.jnntp;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

public class CompositeChannelBufferInputStream extends InputStream {
    private LinkedBlockingQueue<ByteBuf> buffers;
    private ByteBuf current;
    private boolean eof = false;

    public CompositeChannelBufferInputStream() {
        this.buffers = new LinkedBlockingQueue<>();
    }

    @Override
    public int read() throws IOException {
        if(this.eof) return -1;

        // If current buffer is finished, throw it away and we'll get another.
        if(this.current != null && !this.current.isReadable()) {
            this.current = null;
        }

        while(this.current == null) {
            try {
                this.current = this.buffers.take();
            }
            catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        // Since we already checked for empty buffers earlier, getting one here means that async thread handed us an
        // empty buffer, which means we're done.
        if(!this.current.isReadable()) {
            this.eof = true;
            return -1;
        }

        return this.current.readUnsignedByte();
    }

    public void addData(ByteBuf buffer) {
        this.buffers.offer(buffer);
    }
}

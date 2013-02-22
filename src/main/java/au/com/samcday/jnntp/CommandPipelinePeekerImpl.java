package au.com.samcday.jnntp;

import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandPipelinePeekerImpl implements CommandPipelinePeeker {
    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;

    public CommandPipelinePeekerImpl(ConcurrentLinkedQueue<NntpFuture<?>> pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public NntpResponse.ResponseType peekType() {
        return this.pipeline.peek().getType();
    }
}

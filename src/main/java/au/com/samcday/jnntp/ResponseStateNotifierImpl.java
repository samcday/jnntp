package au.com.samcday.jnntp;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ResponseStateNotifierImpl implements ResponseStateNotifier {
    private static Set<Integer> MULTILINE_CODES = Sets.newHashSet(100, 101, 215, 220, 221, 222, 224, 225, 230, 231);
    private ConcurrentLinkedQueue<NntpFuture<?>> pipeline;

    public ResponseStateNotifierImpl(ConcurrentLinkedQueue<NntpFuture<?>> pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public boolean isMultiline(int code) {
        return MULTILINE_CODES.contains(code) && (code != 211 || pipeline.peek().getType() != NntpResponse.ResponseType.GROUP);
    }
}

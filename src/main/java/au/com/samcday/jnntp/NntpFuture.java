package au.com.samcday.jnntp;

import java.util.concurrent.*;

public class NntpFuture<T extends NntpResponse> implements Future<T> {
    private CountDownLatch latch;
    private T response;
    private NntpResponse.ResponseType type;

    public NntpFuture(NntpResponse.ResponseType type) {
        this.type = type;
        this.latch = new CountDownLatch(1);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new IllegalAccessError();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return this.latch.getCount() == 0;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        this.latch.await();
        return this.response;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        this.latch.await(timeout, unit);
        return this.response;
    }

    public NntpResponse.ResponseType getType() {
        return type;
    }

    public void onResponse(T response) {
        this.response = response;
        this.latch.countDown();
    }
}

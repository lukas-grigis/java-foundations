package dev.lukasgrigis.foundations.virtualthreadspinning.proof;

import dev.lukasgrigis.foundations.virtualthreadspinning.example.LockingSessionClient;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question: the control. The same 64 threads each renew their own session, but through
 * {@link LockingSessionClient#renew()}, which blocks inside a {@code ReentrantLock}. Does the JDK make
 * a difference here?
 */
public final class BlockingInsideReentrantLock {

    private static final int THREADS = 64;

    private BlockingInsideReentrantLock() {
    }

    public static void main(String[] args) {
        final var inside = new AtomicInteger();
        final var maxInside = new AtomicInteger();

        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < THREADS; i++) {
                final var client = new LockingSessionClient(); // its own session, so its own lock
                executor.execute(() -> {
                    int n = inside.incrementAndGet(); // one lock each, so this also counts lock holders
                    maxInside.accumulateAndGet(n, Math::max);
                    client.renew();
                    inside.decrementAndGet();
                });
            }
        } // close() returns once every thread is done
        long elapsedMs = millisSince(start);

        System.out.println("ReentrantLock    maxInside = " + maxInside.get() + "    elapsed = " + elapsedMs + " ms");
    }

    private static long millisSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

}

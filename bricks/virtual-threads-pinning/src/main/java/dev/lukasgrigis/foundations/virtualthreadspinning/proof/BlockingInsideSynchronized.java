package dev.lukasgrigis.foundations.virtualthreadspinning.proof;

import dev.lukasgrigis.foundations.virtualthreadspinning.example.SynchronizedSessionClient;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question: 64 virtual threads each renew their own session through
 * {@link SynchronizedSessionClient#renew()}, which blocks inside a synchronized method. How many of
 * them are inside that method at the same time, on JDK 21 and on JDK 26?
 */
public final class BlockingInsideSynchronized {

    private static final int THREADS = 64;

    private BlockingInsideSynchronized() {
    }

    public static void main(String[] args) {
        final var inside = new AtomicInteger();
        final var maxInside = new AtomicInteger();

        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < THREADS; i++) {
                final var client = new SynchronizedSessionClient(); // its own session, so its own lock
                executor.execute(() -> {
                    int n = inside.incrementAndGet(); // one lock each, so this also counts lock holders
                    maxInside.accumulateAndGet(n, Math::max);
                    client.renew();
                    inside.decrementAndGet();
                });
            }
        } // close() returns once every thread is done
        long elapsedMs = millisSince(start);

        System.out.println("JDK " + Runtime.version().feature() + "   synchronized     maxInside = " + maxInside.get() + "    elapsed = " + elapsedMs + " ms");
    }

    private static long millisSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

}

package dev.lukasgrigis.foundations.virtualthreadspinning.proof;

import dev.lukasgrigis.foundations.virtualthreadspinning.example.SynchronizedSessionClient;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question: 64 virtual threads all renew the same session, so they share one monitor. Does JEP 491
 * let more than one of them inside at a time?
 */
public final class SharedLock {

    private static final int THREADS = 64;

    private SharedLock() {
    }

    public static void main(String[] args) {
        final var client = new SynchronizedSessionClient(); // one session, shared by every thread
        final var inside = new AtomicInteger();
        final var maxInside = new AtomicInteger();

        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < THREADS; i++) {
                executor.execute(() -> {
                    // renew() locks on the client as well; the monitor is reentrant, so counting in here
                    // counts the holder only, not the threads still queued for the monitor
                    synchronized (client) {
                        int n = inside.incrementAndGet();
                        maxInside.accumulateAndGet(n, Math::max);
                        client.renew();
                        inside.decrementAndGet();
                    }
                });
            }
        } // close() returns once every thread is done
        long elapsedMs = millisSince(start);

        System.out.println("JDK " + Runtime.version().feature() + "   shared lock      maxInside = " + maxInside.get() + "    elapsed = " + elapsedMs + " ms");
    }

    private static long millisSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

}

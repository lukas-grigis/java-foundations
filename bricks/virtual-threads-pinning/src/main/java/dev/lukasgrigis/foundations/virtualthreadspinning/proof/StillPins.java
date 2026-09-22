package dev.lukasgrigis.foundations.virtualthreadspinning.proof;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question: JEP 491 keeps one case pinned on purpose, waiting for another thread's static
 * initializer. A platform thread runs a slow initializer and 64 virtual threads only wait for it.
 * Does that waiting still pin the carrier, on JDK 21 and on JDK 26?
 */
public final class StillPins {

    private static final int THREADS = 64;

    private StillPins() {
    }

    public static void main(String[] args) throws InterruptedException {
        final var initializer = new Thread(SlowInit::touch);
        initializer.start();
        Thread.sleep(20); // let <clinit> get well under way before the first waiter arrives

        final var inside = new AtomicInteger();
        final var maxInside = new AtomicInteger();

        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < THREADS; i++) {
                executor.execute(() -> {
                    int n = inside.incrementAndGet();
                    maxInside.accumulateAndGet(n, Math::max);
                    SlowInit.touch(); // a pure waiter: <clinit> is already running on the platform thread
                    inside.decrementAndGet();
                });
            }
        } // close() returns once every thread is done
        initializer.join();
        long elapsedMs = millisSince(start);

        System.out.println("JDK " + Runtime.version().feature() + "   waiting-on-init  maxInside = " + maxInside.get() + "    elapsed = " + elapsedMs + " ms");
    }

    private static long millisSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    // a class nobody has touched yet: its static initializer runs exactly once
    private static final class SlowInit {

        static {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        static void touch() {
        }

    }

}

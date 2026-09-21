package dev.lukasgrigis.foundations.structuredconcurrency.proof;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question: a task that would run for 5 s gets a 300 ms deadline. How long does the caller wait,
 * and is the task still running 500 ms after the deadline?
 */
public final class Timeout {

    private static final Duration DEADLINE = Duration.ofMillis(300);

    private Timeout() {
    }

    static void main() throws InterruptedException {
        runWithExecutorService();
        runWithStructuredTaskScope();
    }

    private static void runWithExecutorService() throws InterruptedException {
        final var running = new AtomicInteger();
        // Not closed on purpose: close() would wait for the abandoned task this proof counts.
        final var pool = Executors.newVirtualThreadPerTaskExecutor();

        long start = System.nanoTime();
        final var future = pool.submit(() -> slow(running));
        try {
            future.get(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException expected) {
            // The deadline has passed. Typical code gives up here and never calls future.cancel(true).
        }
        long elapsedMs = millisSince(start);

        Thread.sleep(500);
        System.out.println("ExecutorService  " + elapsedMs + " ms, " + running.get() + " subtask still running");
    }

    private static void runWithStructuredTaskScope() throws InterruptedException {
        final var running = new AtomicInteger();

        long start = System.nanoTime();
        try (var scope = StructuredTaskScope.open(config -> config.withTimeout(DEADLINE))) {
            scope.fork(() -> slow(running));
            scope.join();
        } catch (ExecutionException expected) {
            // Caused by CancelledByTimeoutException. close() has cancelled the task and waited for it.
        }
        long elapsedMs = millisSince(start);

        Thread.sleep(500);
        System.out.println("StructuredTaskScope  " + elapsedMs + " ms, " + running.get() + " subtask still running");
    }

    private static Void slow(AtomicInteger running) throws InterruptedException {
        running.incrementAndGet();
        try {
            Thread.sleep(5_000);
            return null;
        } finally {
            running.decrementAndGet();
        }
    }

    private static long millisSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

}

package dev.lukasgrigis.foundations.structuredconcurrency.proof;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question: three mirrors race to answer the same request, and the fastest wins. How long does the
 * caller wait, and how many losers are still running 150 ms after the winner answered?
 */
public final class FirstWins {

    private FirstWins() {
    }

    static void main() throws ExecutionException, InterruptedException {
        runWithCompletableFutures();
        runWithStructuredTaskScope();
    }

    private static void runWithCompletableFutures() throws InterruptedException {
        final var running = new AtomicInteger();
        // Not closed on purpose: close() would wait for the losers this proof counts.
        final var pool = Executors.newVirtualThreadPerTaskExecutor();

        long start = System.nanoTime();
        final var firstAnswer = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> mirror(running, 100), pool),
                CompletableFuture.supplyAsync(() -> mirror(running, 300), pool),
                CompletableFuture.supplyAsync(() -> mirror(running, 500), pool)
        );
        firstAnswer.join(); // the other two keep running
        long elapsedMs = millisSince(start);

        Thread.sleep(150);
        System.out.println("ExecutorService  " + elapsedMs + " ms, " + running.get() + " losers still running");
    }

    private static void runWithStructuredTaskScope() throws ExecutionException, InterruptedException {
        final var running = new AtomicInteger();

        long start = System.nanoTime();
        try (var scope = StructuredTaskScope.open(Joiner.<String>anySuccessfulOrThrow())) {
            scope.fork(() -> mirror(running, 100));
            scope.fork(() -> mirror(running, 300));
            scope.fork(() -> mirror(running, 500));
            scope.join(); // returns the first answer; throws only if all three fail
        }
        long elapsedMs = millisSince(start);

        Thread.sleep(150);
        System.out.println("StructuredTaskScope  " + elapsedMs + " ms, " + running.get() + " losers still running");
    }

    private static String mirror(AtomicInteger running, long millis) {
        running.incrementAndGet();
        try {
            Thread.sleep(millis);
            return "mirror-" + millis;
        } catch (InterruptedException e) {
            // a loser the scope cancelled; a Supplier cannot throw the checked exception
            throw new RuntimeException(e);
        } finally {
            running.decrementAndGet();
        }
    }

    private static long millisSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

}

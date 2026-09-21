package dev.lukasgrigis.foundations.structuredconcurrency.proof;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question: the caller gives up waiting. How many of its subtasks are still running one second
 * later?
 */
public final class LeakedThreads {

    private LeakedThreads() {
    }

    static void main() throws InterruptedException {
        runWithExecutorService();
        runWithStructuredTaskScope();
    }

    private static void runWithExecutorService() throws InterruptedException {
        final var running = new AtomicInteger();
        final var pool = Executors.newVirtualThreadPerTaskExecutor(); // never closed, that is the point

        for (int i = 0; i < 3; i++) {
            pool.submit(() -> work(running));
        }
        Thread.sleep(150); // the caller gives up here

        Thread.sleep(1_000);
        System.out.println("ExecutorService  " + running.get() + " subtasks still running");
    }

    private static void runWithStructuredTaskScope() throws InterruptedException {
        final var running = new AtomicInteger();
        final var caller = Thread.ofPlatform().start(() -> {
            try (var scope = StructuredTaskScope.open()) {
                for (int i = 0; i < 3; i++) {
                    scope.fork(() -> work(running));
                }
                scope.join();
            } catch (ExecutionException | InterruptedException expected) {
                // Interrupted in join(). By now close() has cancelled the subtasks and waited for them.
            }
        });
        Thread.sleep(150);
        caller.interrupt(); // the caller gives up here
        caller.join();

        Thread.sleep(1_000);
        System.out.println("StructuredTaskScope  " + running.get() + " subtasks still running");
    }

    private static Void work(AtomicInteger running) throws InterruptedException {
        running.incrementAndGet();
        try {
            Thread.sleep(5_000);
            return null;
        } finally {
            running.decrementAndGet();
        }
    }

}

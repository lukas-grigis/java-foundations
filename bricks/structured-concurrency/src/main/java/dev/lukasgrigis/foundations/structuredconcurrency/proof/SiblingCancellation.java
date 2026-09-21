package dev.lukasgrigis.foundations.structuredconcurrency.proof;

import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.OrderService;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.RecommendationService;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.Recommendations;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.UserProfile;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.UserService;
import dev.lukasgrigis.foundations.structuredconcurrency.example.DashboardLoaderExecutor;
import dev.lukasgrigis.foundations.structuredconcurrency.example.DashboardLoaderScope;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Question: the order service fails after 100 ms while the user service takes 5 s. How long does
 * the dashboard loader wait before it has the exception?
 */
public final class SiblingCancellation {

    private static final UserService SLOW_USERS = _ -> {
        pause(5_000);
        return new UserProfile("Ada", "gold");
    };

    private static final OrderService FAILING_ORDERS = _ -> {
        pause(100);
        throw new IllegalStateException("order service unavailable");
    };

    private static final RecommendationService RECOMMENDATIONS = _ -> new Recommendations(List.of("keyboard"));

    private SiblingCancellation() {
    }

    static void main() throws InterruptedException {
        runWithExecutorService();
        runWithStructuredTaskScope();
    }

    private static void runWithExecutorService() throws InterruptedException {
        long start = System.nanoTime();
        long elapsedMs = -1; // stays -1 if the order service unexpectedly does not fail

        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            // reads the user first, so the order failure waits for the slow user service (see README)
            DashboardLoaderExecutor.load(
                    pool,
                    "user-1",
                    SLOW_USERS,
                    FAILING_ORDERS,
                    RECOMMENDATIONS
            );
        } catch (ExecutionException expected) {
            elapsedMs = millisSince(start);
        }

        System.out.println("ExecutorService  " + elapsedMs + " ms");
    }

    private static void runWithStructuredTaskScope() throws InterruptedException {
        long start = System.nanoTime();
        long elapsedMs = -1; // stays -1 if the order service unexpectedly does not fail

        try {
            DashboardLoaderScope.load(
                    "user-1",
                    SLOW_USERS,
                    FAILING_ORDERS,
                    RECOMMENDATIONS
            );
        } catch (ExecutionException expected) {
            elapsedMs = millisSince(start);
        }

        System.out.println("StructuredTaskScope  " + elapsedMs + " ms");
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // cancelled by the scope: a blocking client call gives up the same way
            Thread.currentThread().interrupt();
            throw new IllegalStateException("call cancelled", e);
        }
    }

    private static long millisSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

}

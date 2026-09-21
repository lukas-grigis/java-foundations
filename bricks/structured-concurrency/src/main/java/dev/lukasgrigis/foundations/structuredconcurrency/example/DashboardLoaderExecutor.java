package dev.lukasgrigis.foundations.structuredconcurrency.example;

import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.OrderService;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.Page;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.RecommendationService;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.UserService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * The dashboard loader most codebases have today: three {@code submit()} calls on an
 * {@link ExecutorService}, then three {@link Future#get()} calls in a row.
 *
 * <p>It reads fine, and it hides the bug most reviewers miss. When one {@code get()} throws, the
 * futures not read yet are never cancelled, so their calls keep running for a request that has
 * already failed. And because the futures are read in a fixed order, a failing order service only
 * surfaces after the user service has answered, however long that takes.
 */
public final class DashboardLoaderExecutor {

    private DashboardLoaderExecutor() {
    }

    public static Page load(
            ExecutorService pool,
            String userId,
            UserService userService,
            OrderService orderService,
            RecommendationService recommendationService
    ) throws ExecutionException, InterruptedException {

        final var userFuture = pool.submit(() -> userService.fetch(userId));
        final var orderFuture = pool.submit(() -> orderService.fetch(userId));
        final var recommendationsFuture = pool.submit(() -> recommendationService.fetch(userId));

        // If any get() below throws, the futures not read yet are never cancelled:
        // nothing in this method calls Future.cancel(). That is the bug.
        final var user = userFuture.get();
        final var orders = orderFuture.get();
        final var recommendations = recommendationsFuture.get();

        return new Page(user, orders, recommendations);
    }

}

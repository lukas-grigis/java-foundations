package dev.lukasgrigis.foundations.structuredconcurrency.example;

import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.OrderService;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.Page;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.RecommendationService;
import dev.lukasgrigis.foundations.structuredconcurrency.example.Collaborators.UserService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

/**
 * The same loader with a {@link StructuredTaskScope}: one scope, three forks, one join.
 *
 * <p>{@link StructuredTaskScope#open()} without a joiner waits until every subtask has succeeded or
 * the first one has failed. When one fails, the scope cancels the others and
 * {@link StructuredTaskScope#join() join()} throws an {@link ExecutionException} with that failure
 * as its cause. There is no {@code Future.cancel()} to forget, and a
 * {@link StructuredTaskScope.Subtask Subtask} hands out its value only after {@code join()} has
 * returned normally.
 */
public final class DashboardLoaderScope {

    private DashboardLoaderScope() {
    }

    public static Page load(
            String userId,
            UserService userService,
            OrderService orderService,
            RecommendationService recommendationService
    ) throws ExecutionException, InterruptedException {

        try (var scope = StructuredTaskScope.open()) {
            final var userTask = scope.fork(() -> userService.fetch(userId));
            final var orderTask = scope.fork(() -> orderService.fetch(userId));
            final var recommendationsTask = scope.fork(() -> recommendationService.fetch(userId));

            // Throws as soon as one subtask fails, after cancelling the other two.
            // close() then waits for them to finish before the exception leaves this method.
            scope.join();

            return new Page(userTask.get(), orderTask.get(), recommendationsTask.get());
        }
    }

}

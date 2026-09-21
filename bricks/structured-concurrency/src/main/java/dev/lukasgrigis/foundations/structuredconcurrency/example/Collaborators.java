package dev.lukasgrigis.foundations.structuredconcurrency.example;

import java.math.BigDecimal;
import java.util.List;

/**
 * The three services a dashboard page fans out to, and the page they assemble.
 *
 * <p>Plain interfaces and records: no HTTP, no Spring, no implementations. They stand in for the
 * user, order and recommendation services a backend-for-frontend calls on every page load.
 * {@link DashboardLoaderExecutor} and {@link DashboardLoaderScope} load the same {@link Page}
 * from them, once by hand and once with a scope.
 */
public final class Collaborators {

    private Collaborators() {
    }

    public interface UserService {

        UserProfile fetch(String userId);

    }

    public interface OrderService {

        OrderSummary fetch(String userId);

    }

    public interface RecommendationService {

        Recommendations fetch(String userId);

    }

    public record UserProfile(
            String name,
            String tier
    ) {

    }

    public record OrderSummary(
            int openOrders,
            BigDecimal totalDue
    ) {

    }

    public record Recommendations(List<String> items) {

    }

    public record Page(
            UserProfile user,
            OrderSummary orders,
            Recommendations recommendations
    ) {

    }

}

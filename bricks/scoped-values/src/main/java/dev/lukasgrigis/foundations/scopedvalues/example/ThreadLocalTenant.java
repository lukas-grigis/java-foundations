package dev.lukasgrigis.foundations.scopedvalues.example;

/**
 * The tenant of the current request, carried down the call stack in a {@link ThreadLocal}: the context job
 * {@code ThreadLocal} has done for years.
 *
 * <p>{@link #runAs} is the careful way in. But {@link #set} is public too, the way MDC.put and
 * SecurityContextHolder.setContext are, because a request filter sets the tenant and something else clears it.
 * Any code that can read the tenant can also change it. {@link ScopedTenant} is the same holder without that
 * door.
 */
public final class ThreadLocalTenant {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private ThreadLocalTenant() {
    }

    public static void runAs(String tenant, Runnable operation) {
        final var previous = TENANT.get();
        TENANT.set(tenant);
        try {
            operation.run();
        } finally {
            if (previous == null) {
                TENANT.remove();
            } else {
                TENANT.set(previous);
            }
        }
    }

    public static void set(String tenant) {
        TENANT.set(tenant);
    }

    // null when no request has set a tenant on this thread
    public static String current() {
        return TENANT.get();
    }

}

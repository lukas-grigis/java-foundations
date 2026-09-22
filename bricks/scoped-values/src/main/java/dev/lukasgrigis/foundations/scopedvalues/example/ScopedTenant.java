package dev.lukasgrigis.foundations.scopedvalues.example;

/**
 * The same tenant holder as {@link ThreadLocalTenant}, built on a {@link ScopedValue} (JEP 506, final since
 * JDK 25).
 *
 * <p>A tenant is bound for the length of one {@link #runAs} call and unbound the moment it returns. There is
 * no setter: code further down can only run its own callees with a different tenant, in a nested scope, and
 * the caller's tenant is back once that scope ends.
 */
public final class ScopedTenant {

    private static final ScopedValue<String> TENANT = ScopedValue.newInstance();

    private ScopedTenant() {
    }

    public static void runAs(String tenant, Runnable operation) {
        ScopedValue.where(TENANT, tenant).run(operation);
    }

    // null outside any runAs, the same answer ThreadLocalTenant gives
    public static String current() {
        return TENANT.isBound() ? TENANT.get() : null;
    }

}

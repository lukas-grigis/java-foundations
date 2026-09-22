package dev.lukasgrigis.foundations.scopedvalues.proof;

import dev.lukasgrigis.foundations.scopedvalues.example.ScopedTenant;
import dev.lukasgrigis.foundations.scopedvalues.example.ThreadLocalTenant;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Question: every other request sets its tenant and forgets to clear it, and the requests in between are
 * anonymous. How many anonymous requests run as the tenant of the request before them?
 */
public final class ContextLeak {

    private static final int REQUESTS = 1_000;

    private ContextLeak() {
    }

    static void main() {
        runWithThreadLocalOnPlatformPool();
        runWithThreadLocalOnVirtualThreads();
        runWithScopedValueOnPlatformPool();
    }

    private static void runWithThreadLocalOnPlatformPool() {
        final var stale = new AtomicInteger();
        try (var pool = Executors.newSingleThreadExecutor()) {
            serveWithThreadLocal(pool, stale);
        }

        report("platform pool of 1", "ThreadLocal", stale.get());
    }

    private static void runWithThreadLocalOnVirtualThreads() {
        final var stale = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            serveWithThreadLocal(executor, stale);
        }

        report("virtual threads", "ThreadLocal", stale.get());
    }

    private static void runWithScopedValueOnPlatformPool() {
        final var stale = new AtomicInteger();
        try (var pool = Executors.newSingleThreadExecutor()) {
            for (int i = 0; i < REQUESTS; i++) {
                final var tenant = "tenant-" + i;
                if (i % 2 == 0) {
                    // there is nothing to forget: the binding ends when runAs returns
                    pool.execute(() -> ScopedTenant.runAs(tenant, () -> {
                    }));
                } else {
                    pool.execute(() -> countStale(ScopedTenant.current(), stale));
                }
            }
        }

        report("platform pool of 1", "ScopedValue", stale.get());
    }

    private static void serveWithThreadLocal(ExecutorService executor, AtomicInteger stale) {
        for (int i = 0; i < REQUESTS; i++) {
            final var tenant = "tenant-" + i;
            if (i % 2 == 0) {
                executor.execute(() -> ThreadLocalTenant.set(tenant)); // the handler that forgets to clear
            } else {
                executor.execute(() -> countStale(ThreadLocalTenant.current(), stale));
            }
        }
    }

    private static void report(String threads, String holder, int stale) {
        System.out.printf(
                "%-19s %-12s %d of %d anonymous requests ran as a stale tenant%n",
                threads,
                holder,
                stale,
                REQUESTS / 2
        );
    }

    private static void countStale(String tenant, AtomicInteger stale) {
        if (tenant != null) {
            stale.incrementAndGet();
        }
    }

}

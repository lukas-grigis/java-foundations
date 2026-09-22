package dev.lukasgrigis.foundations.scopedvalues.proof;

import dev.lukasgrigis.foundations.scopedvalues.example.SharedDateFormat;
import dev.lukasgrigis.foundations.scopedvalues.example.ThreadLocalDateFormat;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Question: 10,000 tasks each format one timestamp through the thread-local formatter cache. How many
 * formatters get built on a pool of 8 platform threads, how many on virtual threads, and how many with one
 * shared {@code DateTimeFormatter}?
 */
public final class FormatterInstances {

    private static final int TASKS = 10_000;

    private static final int POOL_SIZE = 8;

    private FormatterInstances() {
    }

    static void main() {
        runWithPlatformPool();
        runWithVirtualThreads();
        runWithSharedFormatter();
    }

    private static void runWithPlatformPool() {
        final var formatters = identitySet();
        try (var pool = Executors.newFixedThreadPool(POOL_SIZE)) {
            formatWithThreadLocal(pool, formatters);
        }

        report("platform pool of 8", "ThreadLocal<SimpleDateFormat>", formatters.size());
    }

    private static void runWithVirtualThreads() {
        final var formatters = identitySet();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            formatWithThreadLocal(executor, formatters);
        }

        report("virtual threads", "ThreadLocal<SimpleDateFormat>", formatters.size());
    }

    private static void runWithSharedFormatter() {
        final var formatters = identitySet();
        final var now = ZonedDateTime.now();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TASKS; i++) {
                executor.execute(() -> {
                    final var formatter = SharedDateFormat.formatter();
                    formatter.format(now);
                    formatters.add(formatter);
                });
            }
        }

        report("virtual threads", "shared DateTimeFormatter", formatters.size());
    }

    private static void formatWithThreadLocal(ExecutorService executor, Set<Object> formatters) {
        final var now = new Date();
        for (int i = 0; i < TASKS; i++) {
            executor.execute(() -> {
                final var formatter = ThreadLocalDateFormat.formatter();
                formatter.format(now);
                formatters.add(formatter);
            });
        }
    }

    private static void report(String threads, String cache, int formatters) {
        System.out.printf(
                "%-19s %-30s %d formatter%s for %d tasks%n",
                threads,
                cache,
                formatters,
                formatters == 1 ? "" : "s",
                TASKS
        );
    }

    // by identity: SimpleDateFormat.equals() calls two formatters with the same pattern equal
    private static Set<Object> identitySet() {
        return Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    }

}

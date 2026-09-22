package dev.lukasgrigis.foundations.scopedvalues.example;

import java.text.SimpleDateFormat;

/**
 * The caching idiom from before virtual threads: one {@link SimpleDateFormat} per thread, built on first use
 * and reused by every task that thread runs afterwards.
 *
 * <p>{@code SimpleDateFormat} is not thread-safe and not cheap to build, so on a thread pool this made sense:
 * a pool of 8 threads builds 8 formatters and keeps them for the rest of its life. The pool is what made the
 * cache work. {@link SharedDateFormat} is the replacement that does not depend on it.
 */
public final class ThreadLocalDateFormat {

    private static final ThreadLocal<SimpleDateFormat> FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));

    private ThreadLocalDateFormat() {
    }

    public static SimpleDateFormat formatter() {
        return FORMAT.get();
    }

}

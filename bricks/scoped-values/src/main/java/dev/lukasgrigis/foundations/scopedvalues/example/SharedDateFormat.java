package dev.lukasgrigis.foundations.scopedvalues.example;

import java.time.format.DateTimeFormatter;

/**
 * The replacement for {@link ThreadLocalDateFormat}: {@link DateTimeFormatter} is immutable and thread-safe,
 * so one instance serves every thread, pooled or virtual. The expensive object never belonged to a thread.
 */
public final class SharedDateFormat {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private SharedDateFormat() {
    }

    public static DateTimeFormatter formatter() {
        return FORMAT;
    }

}

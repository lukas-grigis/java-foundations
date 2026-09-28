package dev.lukasgrigis.foundations.memorymodel.example;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Same shape as {@link HitCounter}, backed by an {@link AtomicInteger}: {@code incrementAndGet()}
 * makes the read, the add and the store happen as one atomic step, without a lock.
 */
public final class AtomicHitCounter {

    private final AtomicInteger hits = new AtomicInteger();

    public void hit() {
        hits.incrementAndGet();
    }

    public int hits() {
        return hits.get();
    }

}

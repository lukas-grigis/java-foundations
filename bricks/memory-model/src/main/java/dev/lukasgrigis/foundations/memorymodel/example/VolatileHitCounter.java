package dev.lukasgrigis.foundations.memorymodel.example;

/**
 * Same shape as {@link HitCounter}, with the field made {@code volatile}, and still not thread-safe.
 *
 * <p>{@code ++} is a read, an add and a store. {@code volatile} only guarantees that each of those
 * three sees the latest value, not that the three happen as one.
 */
public final class VolatileHitCounter {

    private volatile int hits;

    public void hit() {
        hits++;
    }

    public int hits() {
        return hits;
    }

}

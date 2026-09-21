package dev.lukasgrigis.foundations.memorymodel.example;

/**
 * Counts hits to an endpoint. Looks fine in code review: one {@code int} field, one increment.
 *
 * <p>{@link VolatileHitCounter} is the fix an engineer reaches for first, and it is not a fix.
 * {@link SynchronizedHitCounter} is the one that is.
 */
public final class HitCounter {

    private int hits;

    public void hit() {
        hits++;
    }

    public int hits() {
        return hits;
    }

}

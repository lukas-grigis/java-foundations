package dev.lukasgrigis.foundations.memorymodel.example;

/**
 * Same shape as {@link HitCounter}, guarded by a lock: the fix.
 */
public final class SynchronizedHitCounter {

    private final Object lock = new Object();

    private int hits;

    public void hit() {
        synchronized (lock) {
            hits++;
        }
    }

    public int hits() {
        synchronized (lock) {
            return hits;
        }
    }

}

package dev.lukasgrigis.foundations.virtualthreadspinning.example;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The rewrite of {@link SynchronizedSessionClient} that was safe before JDK 24: the same heartbeat
 * and the same mutual exclusion, guarded by a {@link ReentrantLock} instead of {@code synchronized}.
 *
 * <p>A {@code ReentrantLock} never pinned a virtual thread, on any JDK. Teams migrated to it before
 * JEP 491 existed, so that one slow renewal could not tie up a carrier thread.
 */
public final class LockingSessionClient {

    private static final int HEARTBEAT_MS = 100;

    private final ReentrantLock lock = new ReentrantLock();

    // stands in for the network round trip to the IdP; a real client would use an HTTP client here
    private static void heartbeat() {
        try {
            Thread.sleep(HEARTBEAT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("heartbeat interrupted", e);
        }
    }

    public void renew() {
        lock.lock();
        try {
            heartbeat();
        } finally {
            lock.unlock();
        }
    }

}

package dev.lukasgrigis.foundations.virtualthreadspinning.example;

/**
 * A client that keeps one session alive with a third-party identity provider.
 *
 * <p>{@link #renew()} sends a heartbeat over the network so the IdP does not expire the session, and
 * it is {@code synchronized} so two concurrent renewals of the same session never send duplicate
 * heartbeats. This is the shape teams wrote before JDK 24: the blocking network call sits inside the
 * synchronized method. {@link LockingSessionClient} is the rewrite the pre-24 advice recommended, and
 * this brick's proofs show whether that rewrite still buys anything on JDK 26.
 */
public final class SynchronizedSessionClient {

    private static final int HEARTBEAT_MS = 100;

    // stands in for the network round trip to the IdP; a real client would use an HTTP client here
    private static void heartbeat() {
        try {
            Thread.sleep(HEARTBEAT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("heartbeat interrupted", e);
        }
    }

    public synchronized void renew() {
        heartbeat();
    }

}

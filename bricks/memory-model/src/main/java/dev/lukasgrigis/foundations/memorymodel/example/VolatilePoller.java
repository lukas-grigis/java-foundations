package dev.lukasgrigis.foundations.memorymodel.example;

/**
 * Same shape as {@link Poller}, with the field made {@code volatile}: the fix.
 */
public final class VolatilePoller {

    private volatile boolean stopped;

    public void run() {
        while (!stopped) {
            // spins on the flag, see Poller
        }
    }

    public void shutdown() {
        stopped = true;
    }

}

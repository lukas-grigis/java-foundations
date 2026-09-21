package dev.lukasgrigis.foundations.memorymodel.example;

/**
 * Polls a queue until told to stop. Looks fine in review: one {@code boolean} flag, checked at the top
 * of the loop.
 *
 * <p>{@link VolatilePoller} is the same class with that one field made {@code volatile}. The loop
 * body is empty on purpose: this class isolates the flag check under test, and a real
 * {@code queue.poll(1, SECONDS)} call would block and hide the visibility gap the proof runs.
 */
public final class Poller {

    private boolean stopped;

    public void run() {
        while (!stopped) {
            // spins on the flag, see the class comment
        }
    }

    public void shutdown() {
        stopped = true;
    }

}

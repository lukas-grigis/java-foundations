package dev.lukasgrigis.foundations.memorymodel.proof;

import dev.lukasgrigis.foundations.memorymodel.example.Poller;
import dev.lukasgrigis.foundations.memorymodel.example.VolatilePoller;

/**
 * Question: a worker loops on a stop flag, and main sets the flag after 200 ms. How long until the
 * worker notices?
 */
public final class StaleRead {

    private static final long CAP_MS = 3000; // still running at the cap means it never noticed

    private StaleRead() {
    }

    static void main() throws InterruptedException {
        runWithPlainBoolean();
        runWithVolatileBoolean();
    }

    private static void runWithPlainBoolean() throws InterruptedException {
        final var poller = new Poller();
        System.out.println("plain boolean     " + timeToStop(poller::run, poller::shutdown));
    }

    private static void runWithVolatileBoolean() throws InterruptedException {
        final var poller = new VolatilePoller();
        System.out.println("volatile boolean  " + timeToStop(poller::run, poller::shutdown));
    }

    // Times how long after the stop the worker actually exits, from a nanoTime stamp the worker writes
    // itself: join() would add OS wake-up latency on top of the visibility gap.
    private static String timeToStop(Runnable body, Runnable stop) throws InterruptedException {
        final var exitNanos = new long[] {-1};
        final var worker = new Thread(() -> {
            body.run();
            exitNanos[0] = System.nanoTime();
        });
        worker.setDaemon(true); // a loop that may never end must not keep the JVM alive
        worker.start();
        Thread.sleep(200);

        long start = System.nanoTime();
        stop.run();
        worker.join(CAP_MS);

        if (worker.isAlive()) {
            return CAP_MS + " ms (cap — still running)";
        }
        return String.format("%.6f ms", (exitNanos[0] - start) / 1_000_000.0);
    }

}

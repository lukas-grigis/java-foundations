package dev.lukasgrigis.foundations.memorymodel.proof;

import dev.lukasgrigis.foundations.memorymodel.example.HitCounter;
import dev.lukasgrigis.foundations.memorymodel.example.SynchronizedHitCounter;
import dev.lukasgrigis.foundations.memorymodel.example.VolatileHitCounter;

import java.util.concurrent.CountDownLatch;

/**
 * Question: two threads call {@code hit()} on the same counter N times each. Does it end at 2N?
 */
public final class LostUpdates {

    private static final int N = 20_000_000;

    private static volatile boolean go;

    private LostUpdates() {
    }

    static void main() throws InterruptedException {
        runWithPlainInt();
        runWithVolatileInt();
        runWithSynchronized();
    }

    private static void runWithPlainInt() throws InterruptedException {
        final var counter = new HitCounter();
        race(counter::hit);
        System.out.println("plain int     " + counter.hits() + " hits");
    }

    private static void runWithVolatileInt() throws InterruptedException {
        final var counter = new VolatileHitCounter();
        race(counter::hit);
        System.out.println("volatile int  " + counter.hits() + " hits");
    }

    private static void runWithSynchronized() throws InterruptedException {
        final var counter = new SynchronizedHitCounter();
        race(counter::hit);
        System.out.println("synchronized  " + counter.hits() + " hits");
    }

    // Both threads leave a start gate together: with two plain start() calls, thread one runs ahead
    // alone, and the plain row comes back exactly right, hiding the race.
    private static void race(Runnable hit) throws InterruptedException {
        final var ready = new CountDownLatch(2);
        go = false;

        final Runnable atTheGate = () -> {
            ready.countDown();
            while (!go) {
                // waits at the gate
            }
            for (int i = 0; i < N; i++) {
                hit.run();
            }
        };

        final var one = new Thread(atTheGate);
        final var two = new Thread(atTheGate);
        one.start();
        two.start();

        ready.await();
        go = true;

        one.join();
        two.join();
    }

}

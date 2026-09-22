package dev.lukasgrigis.foundations.memorymodel.proof;

import dev.lukasgrigis.foundations.memorymodel.example.ImmutableSettings;
import dev.lukasgrigis.foundations.memorymodel.example.Settings;

/**
 * Question: a writer publishes settings through a plain array slot, with no lock. How often does a
 * racing reader see the field still at its default 0, with a plain field and with a final one?
 */
public final class UnsafePublication {

    private static final int OBJECTS = 2_000_000;

    // the writer cycles through 64 slots, so the reader keeps finding fresh objects
    private static final int SLOTS = 64;

    private UnsafePublication() {
    }

    static void main() {
        runWithPlainField();
        runWithFinalField();
    }

    private static void runWithPlainField() {
        final var slots = new Settings[SLOTS];
        final var writer = new Thread(() -> {
            for (int i = 0; i < OBJECTS; i++) {
                slots[i % SLOTS] = new Settings(42);
            }
        });
        writer.start();

        long zeroReads = 0;
        long reads = 0;
        while (writer.isAlive()) { // main is the reader, for as long as the writer runs
            for (final var settings : slots) {
                if (settings != null) {
                    reads++;
                    if (settings.timeoutMs() == 0) {
                        zeroReads++;
                    }
                }
            }
        }

        System.out.println("plain field   " + zeroReads + " zero-reads / " + reads + " reads");
    }

    // Written out a second time on purpose: an abstraction over the two types would put a cast inside
    // the loop this measures, extra work a shared helper would hide.
    private static void runWithFinalField() {
        final var slots = new ImmutableSettings[SLOTS];
        final var writer = new Thread(() -> {
            for (int i = 0; i < OBJECTS; i++) {
                slots[i % SLOTS] = new ImmutableSettings(42);
            }
        });
        writer.start();

        long zeroReads = 0;
        long reads = 0;
        while (writer.isAlive()) {
            for (final var settings : slots) {
                if (settings != null) {
                    reads++;
                    if (settings.timeoutMs() == 0) {
                        zeroReads++;
                    }
                }
            }
        }

        System.out.println("final field   " + zeroReads + " zero-reads / " + reads + " reads");
    }

}

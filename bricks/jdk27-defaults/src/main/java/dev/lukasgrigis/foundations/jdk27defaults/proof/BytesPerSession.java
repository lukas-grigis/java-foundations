package dev.lukasgrigis.foundations.jdk27defaults.proof;

import com.sun.management.HotSpotDiagnosticMXBean;
import dev.lukasgrigis.foundations.jdk27defaults.example.SessionRecord;
import dev.lukasgrigis.foundations.jdk27defaults.example.SessionStore;

import java.lang.management.ManagementFactory;

/**
 * Question: how many fewer bytes does one stored session cost on the heap when the JVM's object
 * header shrinks from 96 to 64 bits (JEP 534)?
 */
public class BytesPerSession {

    private static final int N = 2_000_000;

    static void main() {
        final var store = new SessionStore();
        System.gc();

        long before = usedHeap();

        for (long i = 0; i < N; i++) {
            store.put(new SessionRecord(i, i % 50_000, i + 3_600_000L));
        }

        System.gc();
        long after = usedHeap();

        // keeps `store` reachable across the second System.gc(), so it cannot be collected away
        if (store.size() != N) {
            throw new IllegalStateException("lost sessions: " + store.size());
        }

        double bytesPerSession = (after - before) / (double) N;

        final var jdk = System.getProperty("java.version");
        final var compact = vmFlag("UseCompactObjectHeaders");

        System.out.printf("java-%s compact=%s  %.1f bytes-per-session%n", jdk, compact, bytesPerSession);
    }

    private static long usedHeap() {
        final var runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static String vmFlag(String name) {
        final var bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        return bean.getVMOption(name).getValue();
    }

}

package dev.lukasgrigis.foundations.jdk27defaults.proof;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;

/**
 * Question: does an idle JVM cost more resident memory under G1 (the new everywhere-default,
 * JEP 523) than under Serial (the collector it replaced in a constrained container)?
 */
public class IdleFootprint {

    static void main() throws IOException {
        System.gc();
        long pid = ProcessHandle.current().pid();

        // `ps -o rss=` prints the resident set size in KiB, on macOS and on Linux
        final long residentKib;

        // Process is AutoCloseable since JDK 26: close() closes the streams and waits for `ps` to exit
        try (var process = new ProcessBuilder("ps", "-o", "rss=", "-p", String.valueOf(pid)).start()) {
            residentKib = Long.parseLong(
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII).trim());
        }

        final var names = ManagementFactory.getGarbageCollectorMXBeans()
                .stream()
                .map(GarbageCollectorMXBean::getName)
                .toList();

        boolean g1 = names.stream().anyMatch(n -> n.startsWith("G1"));
        double residentMb = residentKib / 1024.0;

        System.out.printf("%s  %.1f MB%n", g1 ? "G1" : "Serial", residentMb);
    }

}

package dev.lukasgrigis.foundations.jdk27defaults.proof;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * Question: which collector does the JVM choose when nobody tells it (JEP 523)?
 */
public class WhichCollector {

    static void main() {
        final var names = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(GarbageCollectorMXBean::getName)
                .toList();

        boolean g1 = names.stream().anyMatch(n -> n.startsWith("G1"));
        final var jdk = System.getProperty("java.version");

        System.out.printf("java-%s %s  %d g1-selected%n", jdk, names, g1 ? 1 : 0);
    }

}

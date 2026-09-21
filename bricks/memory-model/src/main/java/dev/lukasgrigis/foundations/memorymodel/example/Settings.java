package dev.lukasgrigis.foundations.memorymodel.example;

/**
 * Config built once and handed to worker threads through a plain field. Looks fine in review:
 * {@code timeoutMs} is set in the constructor and never touched again.
 *
 * <p>{@link ImmutableSettings} makes the field {@code final}, which is the only thing that actually
 * protects a reader that takes no lock.
 */
public final class Settings {

    private int timeoutMs;

    public Settings(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

}

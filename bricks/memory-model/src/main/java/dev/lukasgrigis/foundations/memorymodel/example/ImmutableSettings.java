package dev.lukasgrigis.foundations.memorymodel.example;

/**
 * Same shape as {@link Settings}, with the field made {@code final}: the fix.
 */
public final class ImmutableSettings {

    private final int timeoutMs;

    public ImmutableSettings(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

}

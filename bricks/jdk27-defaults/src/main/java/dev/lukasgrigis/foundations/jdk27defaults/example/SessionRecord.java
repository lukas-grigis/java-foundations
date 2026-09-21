package dev.lukasgrigis.foundations.jdk27defaults.example;

/**
 * One entry in an in-memory session store, the way an auth service would hold it: a session id,
 * the user it belongs to, and when it expires.
 */
public record SessionRecord(
        long sessionId,
        long userId,
        long expiresAtEpochMilli
) {

}

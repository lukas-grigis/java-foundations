package dev.lukasgrigis.foundations.jdk27defaults.example;

import java.util.HashMap;
import java.util.Map;

/**
 * A minimal in-memory session store: the kind of structure an auth service keeps in the heap
 * between requests, one {@link SessionRecord} per logged-in user.
 */
public final class SessionStore {

    private final Map<Long, SessionRecord> sessions = new HashMap<>();

    public void put(SessionRecord session) {
        sessions.put(session.sessionId(), session);
    }

    public int size() {
        return sessions.size();
    }

}

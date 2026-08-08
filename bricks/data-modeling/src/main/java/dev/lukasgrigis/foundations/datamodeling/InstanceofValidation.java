package dev.lukasgrigis.foundations.datamodeling;

import dev.lukasgrigis.foundations.datamodeling.TokenValidation.Expired;
import dev.lukasgrigis.foundations.datamodeling.TokenValidation.Malformed;
import dev.lukasgrigis.foundations.datamodeling.TokenValidation.WrongAudience;
import dev.lukasgrigis.foundations.datamodeling.TokenGate.Problem;

import java.time.Instant;
import java.util.Optional;

/**
 * The counter-example: the same gate as an {@code instanceof} ladder — the shape this code
 * takes without a sealed set — kept here so the difference is runnable, not hypothetical.
 *
 * <p>This compiles, runs, and today produces the same denials as {@link TokenGate#deny}.
 * The problem is the shape itself: the ladder enumerates the failures it knows and treats
 * everything else as valid. That final fall-through is a <b>fail-open</b> default. Add a
 * fifth outcome to the domain — say {@code Revoked} — and this method still compiles
 * cleanly: a revoked token falls past every check and the request is allowed. No warning,
 * no test failure until someone writes the test, no diff on this file at all. The sealed
 * switch in {@link TokenGate} turns that same change into a compile error — the compiler
 * becomes the reviewer who never skims.
 *
 * <p>(Notice {@code Valid} isn't even mentioned here; acceptance is whatever fails to match.
 * In {@link TokenGate} the accept path is a named, visible case.)
 */
public final class InstanceofValidation {

    private InstanceofValidation() {
    }

    /**
     * Outcome → HTTP denial, the unchecked way: match the failures you know, let the rest through.
     */
    public static Optional<Problem> deny(TokenValidation outcome) {
        if (outcome instanceof Expired(Instant expiredAt)) {
            return Optional.of(new Problem(401, "invalid_token: token expired at " + expiredAt));
        }
        if (outcome instanceof WrongAudience(String expected, String actual)) {
            return Optional.of(new Problem(401, "invalid_token: audience is \"" + actual
                    + "\", this resource expects \"" + expected + "\""));
        }
        if (outcome instanceof Malformed(String reason)) {
            return Optional.of(new Problem(401, "invalid_token: " + reason));
        }
        // "Everything else must be valid" — the fail-open branch that admits future outcomes.
        return Optional.empty();
    }

}

package dev.lukasgrigis.foundations.datamodeling;

import java.time.Instant;
import java.util.List;

/**
 * The whole domain in one file, on purpose.
 *
 * <p>This is the thesis of the brick: {@code sealed} + records + pattern matching are one modeling
 * primitive, an algebraic data type, rather than three unrelated features.
 *
 * <ul>
 *   <li>The sealed interface is the <b>closed set of alternatives</b> ("validating a token ends
 *       in exactly one of these four outcomes").</li>
 *   <li>Each record is <b>one alternative's data</b>, nothing more: no behavior smuggled in,
 *       no mutable state, no builder ceremony.</li>
 *   <li>Pattern matching (see {@link TokenGate}) is the <b>only consumer</b>, and because the
 *       set is closed, the compiler proves every consumer handles every alternative.</li>
 * </ul>
 *
 * <p>The parts are not interchangeable, and it is worth being precise about which one does the
 * proving. Swap these records for plain final classes and the switch in {@link TokenGate} still
 * refuses to compile when a case is missing. Take {@code sealed} off this interface and the switch
 * cannot be written without a {@code default} at all. The check comes from the closed set and the
 * consumer the compiler holds against it. What the records add is that every outcome arrives
 * carrying its own data instead of a marker class the caller has to cast and interrogate.
 *
 * <p>In this domain the guarantee is a security property. An outcome a consumer forgets to
 * handle is not a cosmetic gap: whatever the fall-through does becomes the policy for every
 * future outcome.
 */
public sealed interface TokenValidation {

    /**
     * The token checked out. Subject and scopes are what the resource server acts on;
     * {@code expiresAt} matters downstream (see the audit guard in {@link TokenGate}).
     */
    record Valid(String subject, List<String> scopes, Instant expiresAt) implements TokenValidation {

        /**
         * Defensive copy so a Valid outcome cannot grow or lose scopes after the fact.
         */
        public Valid {
            scopes = List.copyOf(scopes);
        }

    }

    /**
     * The token was genuine but its lifetime is over. Knowing WHEN it expired is the one
     * useful thing to tell the client.
     */
    record Expired(Instant expiredAt) implements TokenValidation {

    }

    /**
     * A genuine token, presented to the wrong resource. Classic confused-deputy material:
     * the token is not "bad", it just was never meant for us.
     */
    record WrongAudience(String expected, String actual) implements TokenValidation {

    }

    /**
     * Not a parseable token at all. The reason is all there is to know.
     */
    record Malformed(String reason) implements TokenValidation {

    }

}

package dev.lukasgrigis.foundations.datamodeling;

import dev.lukasgrigis.foundations.datamodeling.TokenValidation.Expired;
import dev.lukasgrigis.foundations.datamodeling.TokenValidation.Malformed;
import dev.lukasgrigis.foundations.datamodeling.TokenValidation.Valid;
import dev.lukasgrigis.foundations.datamodeling.TokenValidation.WrongAudience;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The consumer side of the primitive: exhaustive {@code switch} over the sealed hierarchy.
 *
 * <p>Note what is NOT here: no {@code default} branch, no visitor interface, no
 * {@code instanceof} ladder. The compiler knows the four outcomes and refuses to compile
 * any switch that misses one. Add a fifth outcome to {@link TokenValidation} and every
 * switch in this file becomes a compile error — which is exactly what you want at a
 * security boundary: the compiler hands you the complete to-do list of gates to update.
 */
public final class TokenGate {

    /**
     * A still-valid token this close to expiry gets an audit warning instead of a plain accept.
     */
    private static final Duration EXPIRY_WARNING = Duration.ofSeconds(60);

    private TokenGate() {
    }

    /**
     * Outcome → HTTP denial. Empty means the request proceeds — and {@code Valid} is visibly
     * the ONLY case that produces it. Record patterns deconstruct each alternative in place;
     * the sealed set makes the switch exhaustive without a default. Components a case does
     * not need are unnamed ({@code _}), so every visible binding is a used one. The denial
     * shapes follow RFC 6750: a bad bearer token is a 401 with an {@code invalid_token} hint.
     */
    public static Optional<Problem> deny(TokenValidation outcome) {
        return switch (outcome) {
            case Valid _ -> Optional.empty(); // the only way through — visibly a decision
            case Expired(Instant expiredAt) ->
                    Optional.of(new Problem(401, "invalid_token: token expired at " + expiredAt));
            case WrongAudience(String expected, String actual) ->
                    Optional.of(new Problem(401, "invalid_token: audience is \"" + actual
                            + "\", this resource expects \"" + expected + "\""));
            case Malformed(String reason) ->
                    Optional.of(new Problem(401, "invalid_token: " + reason));
        };
    }

    /**
     * Same data, second consumer: one audit line per outcome.
     * Guards ({@code when}) refine a case without losing exhaustiveness — the guarded
     * {@code Valid} case warns about imminent expiry, and the unguarded {@code Valid}
     * case below it still covers the rest.
     */
    public static String auditLine(TokenValidation outcome, Instant now) {
        return switch (outcome) {
            case Valid(String subject, _, Instant expiresAt)
                    when Duration.between(now, expiresAt).compareTo(EXPIRY_WARNING) <= 0 ->
                    "WARN  accepted " + subject + " — token expires in "
                            + Duration.between(now, expiresAt).toSeconds() + "s, client should refresh";
            case Valid(String subject, List<String> scopes, _) ->
                    "INFO  accepted " + subject + " with scopes " + scopes;
            case Expired(Instant expiredAt) ->
                    "INFO  rejected — expired at " + expiredAt;
            case WrongAudience(String expected, String actual) ->
                    "WARN  rejected — token for \"" + actual + "\" presented to \"" + expected + "\"";
            case Malformed(String reason) ->
                    "WARN  rejected — not a parseable token (" + reason + ")";
        };
    }

    /**
     * A denial the HTTP layer can serialize: status code plus a human-readable detail.
     */
    public record Problem(int status, String detail) {

    }

}

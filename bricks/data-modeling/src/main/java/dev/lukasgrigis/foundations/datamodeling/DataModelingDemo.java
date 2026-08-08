package dev.lukasgrigis.foundations.datamodeling;

import dev.lukasgrigis.foundations.datamodeling.TokenValidation.Expired;
import dev.lukasgrigis.foundations.datamodeling.TokenValidation.Malformed;
import dev.lukasgrigis.foundations.datamodeling.TokenValidation.Valid;
import dev.lukasgrigis.foundations.datamodeling.TokenValidation.WrongAudience;

import java.time.Instant;
import java.util.List;

/**
 * Run a handful of validation outcomes through both consumers.
 * The interesting part is not the output — it is what the compiler guaranteed on the way here.
 */
public final class DataModelingDemo {

    static void main() {
        var now = Instant.parse("2026-08-08T10:00:00Z");
        List<TokenValidation> outcomes = List.of(
                new Valid("alice@example.com", List.of("orders:read", "orders:write"), now.plusSeconds(3600)),
                new Valid("build-agent-07", List.of("artifacts:read"), now.plusSeconds(42)),
                new Expired(now.minusSeconds(180)),
                new WrongAudience("orders-api", "billing-api"),
                new Malformed("header is not valid base64url")
        );

        System.out.println("== Gate decisions (exhaustive switch, no default) ==");
        for (var outcome : outcomes) {
            var verdict = TokenGate.deny(outcome)
                    .map(problem -> "deny " + problem.status() + " — " + problem.detail())
                    .orElse("allow");
            System.out.printf("  %-13s -> %s%n", outcome.getClass().getSimpleName(), verdict);
        }

        System.out.println();
        System.out.println("== Audit lines (a when-guard refines, exhaustiveness stays) ==");
        outcomes.forEach(outcome -> System.out.println("  " + TokenGate.auditLine(outcome, now)));

        System.out.println();
        System.out.println("== Sanity: instanceof ladder agrees with the switch — today ==");
        boolean agree = outcomes.stream().allMatch(outcome ->
                TokenGate.deny(outcome).equals(InstanceofValidation.deny(outcome)));
        System.out.println("  ladder == switch for all " + outcomes.size() + " outcomes: " + agree);
        System.out.println("  (Add a fifth outcome, e.g. Revoked: both switches stop compiling;");
        System.out.println("   the ladder keeps compiling and waves the revoked token through — fail-open.)");
    }

}

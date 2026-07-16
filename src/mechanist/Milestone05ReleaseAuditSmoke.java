package mechanist;

import java.util.HashSet;
import java.util.List;

/** Focused Phase 19 smoke for release claims and explicit milestone blockers. */
final class Milestone05ReleaseAuditSmoke {
    public static void main(String[] args) {
        Milestone05ReleaseAuditAuthority.Audit audit =
                Milestone05ReleaseAuditAuthority.inspect();
        List<String> lines = audit.lines();
        HashSet<String> ids = new HashSet<>();

        require(audit.checks() != null && !audit.checks().isEmpty(),
                "release audit should expose named checks");
        require(audit.passed() > 0,
                "release audit should recognize implemented milestone slices");
        require(audit.releaseBlockers() > 0 && !audit.releaseClaimReady(),
                "release claim must remain blocked while required verification and parity gaps remain");
        require(audit.failed() == 0,
                "current static release audit should report explicit conditional/deferred blockers rather than hidden structural failures: "
                        + lines);

        for (Milestone05ReleaseAuditAuthority.Check check : audit.checks()) {
            require(check != null, "release audit check must not be null");
            require(check.id() != null && !check.id().isBlank()
                            && ids.add(check.id()),
                    "release audit checks require stable unique IDs: " + check);
            require(check.label() != null && !check.label().isBlank(),
                    check.id() + " requires a readable label");
            require(check.evidence() != null && !check.evidence().isBlank(),
                    check.id() + " requires explicit evidence");
            require(check.status() != null,
                    check.id() + " requires an explicit status");
            require(!PlayerFacingText.containsLikelyLeak(check.line()),
                    check.id() + " leaked implementation text: " + check.line());
        }

        requireStatus(audit, "catalog-parity-coverage",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "stable-blueprint-identity",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "acquisition-paths",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "vendor-access-legality",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "ownership-permission-resources",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "contract-blueprint-loop",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "infopedia-acquisition-bridge",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "faction-construction-capability",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "physical-strategic-assets",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "operational-vendor-restrictions",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "intentional-parity-exceptions",
                Milestone05ReleaseAuditAuthority.Status.PASS);
        requireStatus(audit, "room-stamp-declarations",
                Milestone05ReleaseAuditAuthority.Status.CONDITIONAL);
        requireStatus(audit, "vehicle-parity",
                Milestone05ReleaseAuditAuthority.Status.DEFERRED);
        requireStatus(audit, "manual-playability",
                Milestone05ReleaseAuditAuthority.Status.CONDITIONAL);
        requireStatus(audit, "java17-package-gate",
                Milestone05ReleaseAuditAuthority.Status.CONDITIONAL);

        requireContains(lines, "Release claim state: BLOCKED",
                "release audit final state");
        requireContains(lines, "vehicle", "vehicle parity blocker");
        requireContains(lines, "room stamp", "room-stamp blocker");
        requireContains(lines, "Java 17", "Java 17 gate blocker");
        requireContains(lines, "keyboard and mouse", "manual playability blocker");
        requireContains(lines, "reveal remains distinct from ownership",
                "blueprint ownership boundary evidence");
        requireContains(lines, "live physical faction support",
                "faction construction evidence");

        System.out.println("Milestone 05 release audit smoke passed.");
    }

    private static void requireStatus(Milestone05ReleaseAuditAuthority.Audit audit,
                                      String id,
                                      Milestone05ReleaseAuditAuthority.Status expected) {
        for (Milestone05ReleaseAuditAuthority.Check check : audit.checks()) {
            if (check != null && id.equals(check.id())) {
                require(check.status() == expected,
                        id + " expected " + expected + " but was "
                                + check.status() + ": " + check.evidence());
                return;
            }
        }
        throw new AssertionError("Release audit is missing required check: " + id);
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.toLowerCase().contains(
                    expected.toLowerCase())) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05ReleaseAuditSmoke() { }
}

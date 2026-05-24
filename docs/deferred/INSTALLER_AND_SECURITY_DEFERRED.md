# Deferred Work — Installer Packaging and Network/Security Bundle

Installer packaging and network/security hardening are deliberately deferred from the trimmed handoff package.

Reason:
The current priority is preserving a clean runnable/development core after the art rebase. Pulling installer and security scaffolding into this package would reintroduce tool sprawl and make the handoff harder to validate.

Resume order:
1. Validate trimmed core launch on Windows and Linux.
2. Validate bundled `low_32` art and optional external art payload mounting.
3. Rebuild installers around the trimmed package.
4. Reintroduce network/security hardening as a separate phase with explicit tests.

Packaging rule:
Installers should ship the core game with bundled `low_32`. Higher art tiers should remain optional external downloads.

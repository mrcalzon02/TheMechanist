# The Mechanist — Diagnostic Collection

Use the thin launcher **Diagnostics** button as the primary collection path. It produces a local Markdown report under the launcher cache and includes bounded, redacted excerpts instead of copying entire log directories automatically.

## Included identity

A current report records:

- Candidate version.
- Exact source commit when the canonical manifest is available.
- Platform package.
- Java release target.
- Release-hardening state.
- Distribution model.
- Canonical or compatibility manifest path.
- Package verification result and support-library count.
- Anonymous local client hash.
- Graphics and audio selections.
- Modified-content detection.
- Redacted install, save, settings, log, cache, and package-seed paths.
- Recent bounded launcher and local log excerpts.

A report without version, exact commit, platform, Java release, hardening state, and package-verification status is incomplete evidence. Do not infer those values from an archive name or screenshot when the candidate can report them directly.

## Canonical limited-alpha report fields

Every defect report should provide the same minimum evidence whether it is filed through the structured GitHub form or a private test channel:

1. Severity: blocker, major, moderate, minor, or cosmetic.
2. Candidate version and exact 40-character source commit.
3. Platform package and operating-system version.
4. Package verification result.
5. Modified-content status and whether the defect reproduces after restoring the verified package.
6. Test lane: launcher, installer, single-player host, persistence, independent host, remote lobby, gameplay, presentation, performance, or diagnostics.
7. A concise defect summary.
8. Reproduction steps beginning from installation or launch.
9. Expected behavior.
10. Actual behavior.
11. Clean-restart, new-save, fresh-profile, and prior-candidate persistence results where applicable.
12. The redacted diagnostic identity and only the bounded excerpts relevant to the failure.

Do not submit a workflow artifact, development checkout, or locally modified package as though it were an approved candidate. Record that distinction explicitly.

## Before sharing

Open the generated report and review it. Automatic redaction covers common token, password, secret, authorization, cookie, session-key, and API-key forms, plus the user home directory. It cannot recognize every private value.

Remove or replace:

- Personal names not needed for reproduction.
- Private server addresses.
- Authentication material.
- Unrelated file paths.
- Chat content or documents accidentally copied into logs.
- Save data not approved for the test channel.

Never attach, paste, rename for upload, or copy into an ordinary report an independent-host resume-token custody file. Never include a plaintext resume token, password, authorization header, API key, cookie, or session secret. The reusable client credential is not diagnostic evidence.

## Modified packages

When modified content is detected, the launcher writes the report locally but does not automatically open a base-game issue draft. Preserve the report for comparison, restore the verified package, and reproduce the problem again before filing it as a base-game defect.

A modified-content report must state whether the failure also occurs after package restoration. Until that comparison exists, classify it separately from verified base-game triage.

## Manual collection when the launcher cannot start

Collect the following without changing the installation:

1. Candidate archive or installer filename.
2. `SHA256SUMS.txt` result.
3. Platform and operating-system version.
4. Screenshot or exact text of the failure.
5. Launcher logs from the user-state log directory.
6. The machine-readable verification report supplied with the candidate.
7. Whether the installation path contains spaces or non-ASCII characters.
8. Whether the application directory is read-only or protected by the operating system.
9. Whether antivirus or application-control software quarantined a file.

Do not send the entire development repository, protected source assets, raw browser profiles, or unrelated system logs.

## Crash reproduction

Record:

- The last successful action.
- The exact action that triggered the crash.
- Whether the crash occurs in a new world.
- Whether the crash occurs after package verification.
- Whether the crash occurs with a fresh user profile.
- Whether the process exits or remains running.
- Whether the single-player internal host shuts down or leaves an orphan process/thread symptom.
- Whether the same candidate reproduces after a clean machine restart.

## Save and persistence defects

Keep an untouched backup before further testing. Report:

- Slot number.
- Candidate version and commit that created the save.
- Candidate version and commit attempting to load it.
- Whether the failure occurs before or after world display.
- Character identity, world identity, turn, and world time before save.
- Whether a new save works.
- Whether the original candidate can still load the backup.
- Whether rollback, repair, upgrade, or uninstall changed mutable user data.

Share saves only through the approved private test channel and only when requested. Do not make a destructive migration attempt against the only copy of a world.

## Independent host defects

Distinguish transport, authenticated lobby authority, and remote gameplay authority.

Transport evidence includes:

- Requested bind address and port.
- Actual reported bind address and port.
- Whether the host listened on an unintended interface.
- Connection count.
- Sequence or frame rejection messages.
- Close and restart behavior.

Authenticated lobby evidence includes:

- Stable profile identity without exposing the reusable credential.
- Connection generation before and after reconnect.
- Connected-only roster membership and ordering.
- Readiness, presence, and typing-state reset behavior.
- Lifetime relay and hosted-command accounting across a clean host restart.

Remote authoritative gameplay is not yet an open alpha capability. Do not describe relay connectivity, authentication, roster delivery, readiness changes, wait-command acceptance, or reconnect continuity as a successful game session. A report must state whether the observed behavior belongs to transport, lobby authority, or actual server-owned world authority.

## Attachment boundary

Appropriate attachments are limited to reviewed screenshots, bounded logs, generated diagnostic reports, supplied verification reports, and save material explicitly approved for the private test channel.

Before submission, confirm all of the following:

- The candidate identity is exact.
- The package verification and modified-content state are stated.
- Every attachment has been reviewed for personal or unrelated information.
- No reusable credential or resume-token custody material is present.
- A backup exists before destructive persistence or installer testing.
- Relay or lobby evidence is not mislabeled as authoritative remote gameplay.

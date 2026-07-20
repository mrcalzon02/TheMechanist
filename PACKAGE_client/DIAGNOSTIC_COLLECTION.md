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

## Before sharing

Open the generated report and review it. Automatic redaction covers common token, password, secret, authorization, cookie, session-key, and API-key forms, plus the user home directory. It cannot recognize every private value.

Remove or replace:

- Personal names not needed for reproduction.
- Private server addresses.
- Authentication material.
- Unrelated file paths.
- Chat content or documents accidentally copied into logs.
- Save data not approved for the test channel.

## Modified packages

When modified content is detected, the launcher writes the report locally but does not automatically open a base-game issue draft. Preserve the report for comparison, restore the verified package, and reproduce the problem again before filing it as a base-game defect.

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

## Save and persistence defects

Keep an untouched backup before further testing. Report:

- Slot number.
- Candidate version and commit that created the save.
- Candidate version and commit attempting to load it.
- Whether the failure occurs before or after world display.
- Character identity, world identity, turn, and world time before save.
- Whether a new save works.
- Whether the original candidate can still load the backup.

Share saves only through the approved private test channel and only when requested.

## Independent host defects

Distinguish transport from gameplay authority.

Transport evidence includes:

- Requested bind address and port.
- Actual reported bind address and port.
- Whether the host listened on an unintended interface.
- Connection count.
- Sequence or frame rejection messages.
- Close and restart behavior.

Remote authoritative gameplay is not yet an open alpha capability. Do not describe relay connectivity as a successful game session.

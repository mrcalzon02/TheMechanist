# The Mechanist — Limited Alpha Client Package

The Mechanist is a Java 17 desktop simulation project currently distributed only to specifically authorized evaluators. The verified candidate is designed as one complete launcher → client → server package rather than a loose collection of JAR files.

## What this package contains

A complete portable or native candidate includes:

- A thin launcher that verifies the bundled package and starts the client.
- The standalone desktop client.
- A separately runnable headless server.
- Platform-specific support and native libraries.
- A bundled Java 17 runtime.
- Canonical SHA-256 manifests and release identity.
- Limited-alpha operating documents.
- Platform launch scripts.

Do not extract or redistribute only the client JAR. The manifest, runtime, support libraries, launcher, client, server, and documentation are one certified set.

## Supported limited-alpha platforms

The current release gates target:

- Windows x64.
- Linux x64.

macOS, ARM, Android, and other platforms are not part of the current candidate line unless a later release announcement explicitly opens them.

## Current operating modes

### Launcher and single-player

The ordinary launcher verifies the bundled candidate and starts the desktop client. Single-player mounts a supervised in-process internal host with one authoritative world-mutation lane, local session identity, immutable snapshots, save/resume checks, and supervised shutdown.

### Independent-host lobby

`mechanist.RemoteClientMain` opens the player-facing independent-host lobby. The lobby provides editable host, port, server-key, and profile fields; authenticated connect/disconnect; readiness, presence, and typing controls; a connected-only authoritative roster; bounded relay messages; reconnect identity; and interrogatable status.

The remote lobby does **not** mount `GamePanel`, the single-player internal host, or remote world authority. It has no movement, combat, inventory, map, character, world-snapshot, or gameplay-command interface.

### Headless server

The separately packaged server initializes its own mutable storage namespace and can bind an exact requested address and allowed game port. The current native fallback is an authenticated, sequenced, bounded relay with a persistent server-owned session ledger and a narrow pre-world lobby authority.

Remote world gameplay remains unavailable until server-owned world snapshots and gameplay-command processing are separately implemented and certified.

## Mutable data

Installation files are immutable candidate content. Saves, profiles, settings, logs, diagnostics, server ledgers, and remote-client resume-token custody belong in user-writable storage outside Program Files, `/opt`, app-image directories, and extracted package roots.

The server persists only SHA-256 resume-token hashes. The client must retain the reusable plaintext token in protected mutable profile storage to reconnect. Token values are excluded from normal status and diagnostic text.

## Start here

Read these documents before testing:

1. `EULA.md`
2. `RUN_INSTRUCTIONS.md`
3. `LIMITED_ALPHA_PLAYTEST_GUIDE.md`
4. `KNOWN_ALPHA_LIMITATIONS.md`
5. `DIAGNOSTIC_COLLECTION.md`
6. `SERVER_README.md` when operating a host

## Verification and support

A build is not approved because it compiles or exists as a workflow artifact. Distribution requires exact Windows and Linux verification, Java 17 classfile checks, package and archive integrity, packaged synthetic tests, single-player lifecycle tests, remote-client and host tests, native app-image verification, asset/redistribution clearance, and release authorization.

Use the launcher diagnostics flow when possible. Never attach a resume-token custody file to an ordinary defect report. Reports should include the exact candidate version, source commit, platform, reproduction steps, severity, and whether the problem occurs in a fresh save or remote profile.

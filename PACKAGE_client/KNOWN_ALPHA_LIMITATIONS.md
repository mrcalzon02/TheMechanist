# The Mechanist — Known Limited-Alpha Constraints

This document describes current intentional limits. It is not a promise that every other feature is complete or defect-free.

## Distribution

- The initial target is Windows x64 and Linux x64.
- macOS, ARM, Android, and other platforms are outside the current release gate.
- A build is not approved merely because it compiles or appears in a workflow artifact.
- Native EXE, MSI, DEB, and RPM availability depends on successful target-native packaging and certification.
- Code signing and operating-system reputation warnings may remain unresolved for private alpha packages.

## Launcher and updates

- The launcher exposes only the bundled limited-alpha package source.
- Authenticated remote acquisition and update channels are not active.
- `main`, `testing`, and `dev` are not player-selectable update services.
- Repair can verify an installed package and use a verified local rollback or package seed when present. Reinstallation may be required when neither exists.
- The launcher must never clone or require the development repository.

## Single-player authority

- Single-player uses a supervised in-process internal host rather than a separate network server process.
- World mutation is intended to pass through one authoritative single-writer lane with local session identity and immutable snapshots.
- Packaged lifecycle certification covers new world creation, an authoritative turn, save, shutdown, fresh client startup, load, session rebind, and continuity checks.
- Broad gameplay still contains legacy compatibility surfaces. A passing lifecycle smoke does not prove every action path is already isolated from legacy direct mutation.
- Save compatibility may change between alpha candidates.

## Independent host

- The headless executable can initialize separate server storage and bind an exact requested interface.
- The native fallback is currently an authenticated bounded relay transport.
- Before relay access, the supervised client must complete identity submission, manifest delivery, acquisition confirmation, restart completion, and a server-issued integrity challenge.
- Successful authentication grants only `RELAY_ONLY` access.
- The supervised client owns handshake progression, resume-token custody, hosted-command and relay sequencing, canonical roster parsing, asynchronous control-frame dispatch, reconnect, and orderly shutdown.
- The client exposes no movement, combat, inventory, world-snapshot, or gameplay-command API.
- The host assigns a stable player ID and one server-issued resume token to each authenticated profile session.
- A disconnected client can recover the same session only with the correct resume token. Invalid tokens and simultaneous duplicate attachment are rejected.
- Immutable session snapshots report online state, connection generation, lifetime accepted relay-frame count, and the current connection sequence.
- The host owns a deliberately narrow pre-world lobby authority. Authenticated clients may submit ordered readiness, presence, and chat-state commands and may request an immutable, deterministically ordered hosted-session roster.
- Public hosted rosters contain only currently connected lobby members and are capped at 64 visible players. Offline identities retained for resume continuity remain private to the server.
- The client roster authority rejects malformed, incomplete, backward-versioned, same-version-divergent, duplicate, out-of-order, offline-member, oversized, or world-authority-claiming roster groups.
- Authenticated peers receive authoritative hosted-roster broadcasts when another client joins, changes readiness, presence, or chat-state, disconnects, or resumes.
- Hosted-roster broadcasts are asynchronous `MECH` control frames. They are separate from `SEQ` relay payloads, do not consume relay sequence IDs, and do not grant gameplay command authority.
- Hosted-session commands use a separate per-connection monotonic command sequence. Lifetime accepted-command accounting survives reconnect and a clean server-process restart.
- Readiness, presence, and typing state describe only the living connection. They reset to not ready, offline, and idle when a client disconnects or when a ledger is restored after host restart.
- Server-side remote session ledgers are written atomically under the dedicated server save namespace. Reusable plaintext resume tokens are not stored there; only SHA-256 token hashes are persisted.
- The client must retain the reusable plaintext token to reconnect. It stores that credential only in the user's mutable profile namespace, using required atomic replacement and owner-only permissions where supported by the operating system.
- Resume tokens are excluded from client status and diagnostic text. Testers should not copy, edit, publish, or attach the token-custody file to an ordinary defect report.
- A missing, corrupted, wrong-profile, or wrong-server client token record fails closed. It does not silently create a replacement session for the same retained profile identity.
- A clean server-process restart restores every known session offline. The original client token can then resume the same stable player identity, advance its connection generation, and preserve lifetime relay and hosted-command accounting.
- Corrupted, unsupported-schema, or world-mismatched server session ledgers fail closed and prevent that host world from binding until the ledger is repaired or deliberately removed.
- Transport certification covers exact binding, denial of pre-authentication data, bad-challenge rejection, supervised client handshake ownership, protected client token custody, token-redacted diagnostics, two-client authenticated connection, ordered frame relay, replay rejection, token-gated reconnect continuity, stale-attachment isolation, hosted-session command ordering, canonical connected-only roster delivery, authenticated peer roster broadcasts, separation of asynchronous control frames from relay data, clean host-restart continuity, hash-only server persistence, corrupt client-token rejection, corrupt server-ledger rejection, close, restart, and refusal to widen a failed explicit bind.
- The delivered base manifest currently represents the packaged base runtime rather than a complete remote mod-distribution service.
- The independent host still does not initialize or own authoritative remote world state.
- Movement, combat, inventory, position, world simulation, and hosted-world persistence commands are not accepted. Unsupported world verbs are rejected at the hosted-session boundary.
- Reconnecting restores the authenticated session and lobby accounting, not a character inside a living authoritative game world.
- Public multiplayer, matchmaking, server browsing, and production account authentication are not open.
- Netty and Steam integrations remain optional seams unless a candidate report explicitly proves a real adapter was initialized.

## Mods and external content

- External mod downloading, source acquisition, hot loading, and public Workshop publication are not open.
- Modified packages may fail verification and are separated from ordinary base-game triage.
- The presence of a mod API class or editor concept does not mean third-party code is safe or supported in the alpha.

## Assets and redistribution

- The protected source-asset vault is not a runtime package.
- Repository inventory, payload ownership, and redistribution clearance must be reviewed before an approved candidate is distributed.
- An exact source-vault/runtime duplicate requires explicit transformation and redistribution review.
- Optional graphics and audio tiers may fall back to lower tiers or silence when the selected payload is not present.

## Experimental presentation

- `doom` mode is experimental, disabled by default, and remains behind a warning gate.
- Java2D remains the safe rendering backend. The presence of LWJGL libraries does not prove an OpenGL backend is active.
- Controller, scaling, audio, and accessibility behavior may vary by operating system and hardware.

## Performance and stability

- Large worlds and long-running saves may expose performance or persistence defects not covered by startup smokes.
- The application remains an alpha simulation with active subsystem migration.
- Clean shutdown, backup, and reproduction discipline are required during testing.
- Testers should expect defects, incomplete balance, missing art, and compatibility changes.

## Support boundary

The most useful reports include the exact generated diagnostic identity, reproduction steps, severity, and whether the problem occurs in a new save. Reports from modified packages may be retained locally but are not automatically treated as base-game defects. Never attach or paste an independent-host resume-token custody file into an ordinary support report.

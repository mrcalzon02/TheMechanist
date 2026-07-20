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
- Before relay access, the client must complete identity submission, manifest delivery, acquisition confirmation, restart completion, and a server-issued integrity challenge.
- Successful authentication grants only `RELAY_ONLY` access.
- Transport certification covers exact binding, denial of pre-authentication data, bad-challenge rejection, two-client authenticated connection, ordered frame relay, replay rejection, close, restart, and refusal to widen a failed explicit bind.
- The delivered base manifest currently represents the packaged base runtime rather than a complete remote mod-distribution service.
- It does not yet initialize or own authoritative remote world state.
- It does not yet process remote gameplay commands, player inventory, world persistence, disconnect continuity, or reconnect into a living hosted world.
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

The most useful reports include the exact generated diagnostic identity, reproduction steps, severity, and whether the problem occurs in a new save. Reports from modified packages may be retained locally but are not automatically treated as base-game defects.

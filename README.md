# The Mechanist

The Mechanist is a Java 17 desktop survival-simulation prototype about life inside a decaying civic-industrial civilization. The project focuses on infrastructural survival, continuity doctrine, archival bureaucracy, population pressure, industrial logistics, public order, factional habitation, and the long-term persistence of people and institutions inside failing built environments.

The current development line is a public-safe semantic rebase. Older prototype language and inherited placeholder identity are being replaced by an original setting architecture based on civic continuity, spireholds, low habitations, ash economies, engine orders, public-order directorates, archival records, and registry-driven identity. The gameplay and simulation architecture remain valuable; the public-facing identity layer is what is being stabilized.

## Current focus

The active repository focus is:

- public-safe terminology and metadata cleanup,
- Historicus / Concord Archive integration,
- semantic asset registry stabilization,
- UI, input, rendering, and presentation containment,
- launcher/client/server separation,
- Java 17 packaging and runtime reliability,
- durable documentation containment.

The project is not being restarted. Existing systems for world generation, tile descriptors, semantic assets, profiles, saves, local server authority, rendering, options, packaging, modding seams, and diagnostics are being preserved while public-facing terminology and setting identity are brought under original project control.

## Requirements

Use Java 17 or newer.

The packaged desktop build targets ordinary desktop machines and keeps Java2D/Swing compatibility as the primary runtime surface. Optional native/runtime integrations may exist as adapter seams, but the project should not claim a native backend, online service, workshop path, or multiplayer authority is active unless the packaged runtime actually initializes and verifies it.

## How to run

On Linux, extract the full package, then use one of the launch helpers:

```bash
chmod +x run_linux.sh PLAY_THE_MECHANIST_LINUX.sh "The Mechanist.desktop" install_linux_launcher.sh
./run_linux.sh
```

Or run the client jar directly:

```bash
java -jar TheMechanist.jar
```

On Windows, extract the full package before running the launcher. The direct testing path is:

```bat
RUN_THE_MECHANIST_WINDOWS.bat
```

The Windows launcher performs a visible Java 17 preflight and should leave diagnostic output available instead of silently closing when Java, the jar, or startup initialization fails.

The headless server entry point can be checked with:

```bash
java -jar TheMechanistServer.jar --status
```

## Package contents

A normal development package may include:

- `TheMechanist.jar` — desktop client runtime.
- `TheMechanistServer.jar` — headless server/status runtime.
- `EULA.md` — user consent, local logging, third-party rights, and user-content notice.
- `SERVER_README.md` — server startup and save-location notes.
- `PACKAGING_PIPELINE.md` — Java 17 Maven, jlink, jpackage, and CI packaging guide.
- `RUN_INSTRUCTIONS.md` and platform launch helpers.
- `src/` — Java source.
- `assets/art/` and `assets/sound/` — bundled runtime assets.
- `assets/artpacks/` and `assets/audiopacks/` — optional drop-in pack locations.
- `assets/indexes/` — machine-readable semantic asset indexes and migration ledgers.
- `docs/` — durable development governance documents and explicitly approved transition ledgers.
- `modding/` — public modding API references and example templates where present.
- `scripts/package/` — native packaging scripts.
- `scripts/security/` and `config/proguard/` — obfuscation and release-security support where present.

## Save and storage model

Mutable player data must live outside installer-controlled application directories. Runtime storage is routed through the user-storage authority for saves, profiles, exported mods, live mods, archived mods, logs, and generated world data.

Single-player saves are exact save-time snapshots. Server or multiplayer-oriented records separate player/character attachments from living world authority files so world state, faction continuity, and player-founded organizations can persist independently of a specific character slot.

## Optional packs

The core package should stay lightweight. Higher-resolution graphics, larger art packs, and audio/music packs should remain optional drop-in content rather than being silently folded into the core runtime archive.

## Development documentation

Durable project direction is governed by:

- `docs/MASTER_DEVELOPMENT_PLAN.md`
- `docs/STANDARDS_AND_PRACTICES.md`
- `docs/DEVELOPMENT_HISTORY.md`
- `docs/MASTER_GOVERNANCE_REVISION_II.md`

Specialized public-safe and Historicus transition ledgers may exist while the rebase is active, but they must not become uncontrolled planning sprawl.

## Legal and content status

The Mechanist is intended to ship as an original project using owned, original, licensed, or otherwise cleared public-facing content. Third-party marks and works remain the property of their respective owners. User-created mods, imported assets, and redistributed packages are the responsibility of the person creating or distributing them.

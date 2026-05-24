# The Mechanist

**Current package:** 0.9.10is — Options, portraits, multiplayer containment, and Doom-mode repair.

Current build note 0.9.10is: graphics include render-only visual lighting, Display/Graphics menu separation, F3 performance diagnostics, JVM runtime profiles, and Accessibility compatibility options. JVM profile settings live in `settings/jvm_runtime.properties`; heap, garbage collector, headless mode, and Java2D pipeline selections require restart. The JVM tab provides launcher/client/thin-client/single-player-combined/server profile selection and an Accept + Restart control. Accessibility options include color-vision correction, high-contrast text containers, instant text support, screen-shake intensity, and screen-reader narration hooks. The main-menu Profile button opens a local fallback Profile Management window when no wrapper/store profile is detected; loot routes through the 0.9.10ij authority for death drops, corpse script, and zone container injection; Windows packaging now emits portable app-image, EXE installer, and MSI installer outputs with icon-backed shortcuts and install-location selection.


Current core build: **0.9.10is**.

The Mechanist is a lightweight Java/Swing desktop prototype for a turn-based underhive simulation. It targets older Linux/Xfce-class machines, keeps low-cost rendering fallbacks, and ships with bundled low-resolution assets so the core game can run without optional packs.

## What it currently includes

The current build includes the native Swing Simulation Editor Suite, mod ZIP exporter, and public modding API/template suite, plus world selection, character creation, tile-icon/ASCII rendering, cached text layout, cached map/UI raster scaling, a default single-player internal-server authoritative world runtime, server-authoritative sector tiering, sector entity pooling, sector-isolated replication stubs, immutable server-published world snapshots, explicit console command requests, server-side long-action gating/progress with player-icon countdown overlays, a separate headless server executable/save namespace, save-slot/world-definition cataloging with slot-to-world references and itemized persistence namespace review, portable light and darkness visibility, environmental hazard warning overlays, trap metadata foundations, noise/hearing fields, economic-topology metadata surfaces, preview-only topology consumers, map/intel summaries, Java2D render-scaling/CRT presentation foundations, display-density authority for native 1:1 Java2D scaling and crisp tiny text, keyboard/gamepad input registry foundations, corrected Linux/XFCE launch helpers, persisted graphics downscale controls, borderless-windowed default launch, centered/clipped transition text, recent-action filtering, Look-stack tile/object summaries, distinct wall/floor/road/corridor rendering, transparent semantic tile underlays, compiled tile descriptors, narrated intro crawl playback, and core footstep/ambient audio cues.

Missing art or audio falls back to bundled low-resolution assets and ASCII/vector rendering.

## How to run

Use Java 17 or newer.

On Linux, extract the full zip, then double-click `PLAY_THE_MECHANIST_LINUX.sh` or `The Mechanist.desktop`. If your file manager blocks launchers from downloaded archives, right-click the file, open Properties → Permissions, and enable execution, or run:

```bash
chmod +x run_linux.sh PLAY_THE_MECHANIST_LINUX.sh "The Mechanist.desktop" install_linux_launcher.sh
./run_linux.sh
```

Or run the jar directly:

```bash
java -jar TheMechanist.jar
```













## 0.9.10is Options / portraits / Doom-mode repair

This build addresses the Windows/Linux visual testing pass covering options-menu verticality, profile portrait display, character portrait fallback, multiplayer button containment, restored comprehensive graphics resolution choices, and first-person Doom-mode rendering faults. Options BACK is moved out of the header, the options frame is taller and wider, QoL controls use a four-column fit, and display/window mode dropdowns remain available. Profile management now shows actual portrait art instead of a numbered color block, character creation always falls back to real portrait images when possible, and the main-menu profile button carries a portrait thumbnail. Doom mode remains disabled unless explicitly enabled, locks/hides/recenters the mouse while active, stops old 2D mouse-preview artifacts from drawing into the 3D viewport, renders textured floor/ceiling sampling from the tile cache, and uses portrait/tile billboard imagery instead of plain entity letters wherever assets are available.

## 0.9.10iq Sector Audit density and road separation repair

This repair removes redundant copy from the Sector Audit command rail, tightens audit buttons and report text, moves BACK into the lower command rail, and uses smaller audit-specific map cells to pull the audit view back. Road/sidewalk handling now forbids sidewalk-to-road promotion in generation, descriptors, and rendering. Roads, sidewalks, road-adjacent structures, and frontage fixtures now refuse room envelopes. Void tiles render as black void, and Neutral Civilian Floor lighting density is significantly higher.

## 0.9.10ip Windows/Linux UI containment repair

This package incorporates the first unified Windows/Linux visual test repairs: EULA legal text wraps by measured pixel width, the main menu route frame reserves a separate EXIT footer so Multiplayer is not overwritten, the Editor / Mod Packaging buttons no longer sit on top of explanatory text, and the Room Editor now includes a visual room-stamp preview plus palette tabs for items, entity spawners, floor tiles, wall tiles, and objects.

Testing note: the archive is kept versioned and current locally, but Windows Java Pro retesting is expected to happen only when the tester explicitly moves to a newer package.

## 0.9.10in Windows Java version probe repair

The Windows direct launcher now uses a PowerShell-based Java discovery path instead of blindly accepting the first `java.exe` on PATH. This specifically fixes machines where Oracle Java 8 is registered first under `Common Files\Oracle\Java\java8path`. The launcher now probes bundled runtime, `JAVA_HOME`, common Java 17 vendor install folders, and PATH candidates; it rejects Java versions below 17 before running any game class. If no Java 17+ runtime is found, the console remains open and the log states that Java 8 cannot run this build.

## 0.9.10il Windows direct-launch repair

The fastest Windows testing path is no longer the native installer. Extract the zip fully, then double-click `RUN_THE_MECHANIST_WINDOWS.bat`. The launcher now performs a Java 17 preflight, writes `%LOCALAPPDATA%\TheMechanist\logs\launch-client.log`, and pauses with visible diagnostics if Java is missing, the jar is missing, or the game throws during startup. See `WINDOWS_QUICK_START.md` for the short Windows testing path.

## 0.9.10ik Windows installer repair and testing path

Windows packaging now uses `scripts/package/build-windows-installers.ps1` as the primary path. It produces a portable app-image ZIP containing a directly runnable `The Mechanist.exe`, plus wizard-style EXE/MSI installers when WiX Toolset 3.x is available. The Windows installer path requests a Start Menu entry, a desktop shortcut, a shortcut confirmation prompt, install-directory selection, and the bundled `the-mechanist.ico` icon. `scripts/package/build-windows-installers.cmd` is provided as a double-clickable wrapper for local Windows testing.

## 0.9.10ij loot/drop authority

This build adds a reusable loot authority for entity death, corpse search, and zone container bonus injection. NPC deaths now resolve carried armor/equipment/ammo/medical items through faction- and zone-tier-scaled drop math; corpses may carry Imperial Script that is collected once on search; hostile low-tier factions such as mutants and cultists carry no script but have much higher raw item drop pressure. A secure 2% death-injection path may add a random catalog item, and room/container caches now have a separate secure 2% zone-purpose bonus table for manufacturing, Guard/PDF, noble, trash/scavenge, cult/mutant, and civilian spaces.

## 0.9.10ih runtime audit / frame pacing / stress diagnostics

This build adds a targeted runtime audit report at `tools/audit/runtime_performance_audit_0_9_10ih.md`, a persisted frame-limit toggle, rolling 60-frame telemetry, and a bounded render stress mode. The Graphics menu exposes `FRAME LIMIT ON/OFF`; the QoL menu exposes `RENDER STRESS`, which floods particles, collision checks, lightmap work, and synthetic ray intersections while reporting FPS, frame time, variance, and active element counts.

The experimental `doom` viewport now treats light texture objects placed on blocked wall tiles as wall-mounted overlays/tints on the rendered wall column. Light objects placed on walkable tiles still render as normal billboarded objects.

## 0.9.10if continuous grid movement / doom collision

The experimental `doom` mode now uses a reusable continuous grid-movement authority rather than one-tile key taps. The player has double-precision X/Y position, velocity, WASD movement relative to mouse-look yaw, a collision radius, and rigid AABB-vs-grid collision against walls and full-tile entity blockers. Movement attempts slide along the free axis when one side is blocked and stop cleanly when both axes are blocked. The HUD/status line reports the current logical tile from the player center point.

A standalone `ContinuousMovementGamePanel` diagnostic panel and `ContinuousGridWorld`/`ContinuousGridPlayer` classes were added so the movement logic can be smoke-tested without opening the full game UI.


## 0.9.10ie visual juice / HUD / 2D effects bridge

The experimental `doom` mode now includes a local camera-juice layer with velocity-driven view bobbing, weapon recoil kick/decay, and a screen-space glitch/static post-process for damage/EMP-style interference. A shared bottom HUD overlay now draws over both normal 2D play and the first-person viewport, with centered portrait frame, left/right weapon boxes, and segmented endurance/health pips.

The normal 2D renderer gained reusable high-quality Graphics2D hints, a primitive-array particle emitter for las/plasma/shotgun/impact effects, a lightmap darkness overlay with radial punch-through lights, and a lightweight AABB projectile collision bridge that can spawn impact particles without allocating per active projectile in the collision loop.

## 0.9.10id experimental doom mode and default LWJGL packaging

The Quality of Life menu now includes a disabled-by-default toggle labeled exactly `doom` mode. Enabling it requires a warning confirmation stating that the mode is highly experimental and may cause instability. When enabled, the active game screen can render an experimental first-person Java2D raycast viewport over the normal simulation view, including mouse-look, FOV controls, billboarded entities/objects, door ray-interaction, forward-ray right-click targeting, basic light attenuation, particle impact splashes, and projected ghost movement path hints.

`pom.xml` now declares LWJGL core, GLFW, OpenGL, and Linux/Windows native runtime classifiers by default so installer builds already carry the native rendering library path. The current in-game backend remains the dependency-free Java2D software renderer unless a future pass explicitly activates the LWJGL/OpenGL backend.

## 0.9.10ib ProGuard obfuscation and secure mapping pipeline

The native packaging line now includes a post-shade ProGuard phase. Maven still emits the dependency-shaded jars, then produces `target/TheMechanist-obfuscated.jar` and `target/TheMechanistServer-obfuscated.jar` before jpackage consumes the client jar. The public `mechanist.modapi` namespace and exported data records remain stable for modders, while internal networking, security, packet sequencing, transaction, and anti-macro implementation classes are eligible for aggressive renaming.

Packaging scripts regenerate the sensitive string table, run the Maven package lifecycle, encrypt ProGuard mapping files into `dist/secure-maps/`, and use the obfuscated client jar for DEB/RPM/MSI output. Raw mapping files are deleted after encryption and must not be distributed with player installers.

## 0.9.10hz native packaging and user-storage routing

This build adds a production packaging pipeline for Java 17 native installers. `pom.xml` now declares Netty and emits dependency-shaded desktop/server fat jars through Maven. Packaging scripts under `scripts/package/` build jlink runtime images and feed them into jpackage for Linux DEB/RPM and Windows MSI installer production. `.github/workflows/package.yml` runs Linux and Windows packaging on separate native runners and assembles a unified installer ZIP artifact. See `PACKAGING_PIPELINE.md` for the full release guide.

Mutable player data is now routed outside the application install folder through `GameStorageManager`. On Linux, data defaults to `$XDG_DATA_HOME/TheMechanist/` or `$HOME/.local/share/TheMechanist/`; on Windows, it defaults to `Documents\TheMechanist\`. The enforced layout is `saves/`, `saves/data/profiles/`, `export/`, `mods/`, and `modsarchived/`.


## 0.9.10ia handshake hot-restart lifecycle

The secure multiplayer join flow now includes an explicit `CLIENT_HOT_RESTART` phase between asset acquisition and the integrity challenge. During this phase the client purges stale local runtime registries, validates and remounts the active user `mods/` directory through a fresh `SecureModClassLoader`, and signals completion without closing the underlying direct TCP/Steam socket abstraction. The server gate still refuses all world snapshot bytes until acquisition, layout verification, hot restart completion, and the salted integrity challenge have all passed for that session.

## Headless local server executable

The package also includes a separate server entry point for the launcher/client/server line:

```bash
java -jar TheMechanistServer.jar --status
```

Server state and server-world files live under `saves/server/`. Desktop single-player saves and world definitions live under `saves/singleplayer/`, so the two runtimes do not share save slots. Single-player `.mechsave` files are bundled save-time snapshots; server/multiplayer character slots stay separate from their `.mechworld` authority files. Host console persistence review commands include `/save_catalog` for the compact count summary and `/save_itemized` for a namespace-by-namespace list of what is currently being saved.

## Package contents

- `TheMechanist.jar` — runnable Java desktop build.
- `TheMechanistServer.jar` — separate headless server runtime initializer/status executable.
- `SERVER_README.md` — basic server usage and save-location notes.
- `PACKAGING_PIPELINE.md` — Java 17 Maven, jlink, jpackage, and CI packaging guide.
- `EULA.md` — root copy of the mandatory EULA and fan-project intellectual-property disclaimer shown by the launch gate.
- `RUN_MECHANIST_SERVER_LINUX.sh` / `RUN_MECHANIST_SERVER_WINDOWS.bat` — server launch helpers.
- `run_linux.sh` — terminal-safe Linux launch helper.
- `PLAY_THE_MECHANIST_LINUX.sh` — double-click-oriented Linux/XFCE launch helper.
- `The Mechanist.desktop` — Linux desktop launcher entry.
- `install_linux_launcher.sh` — refreshes executable bits and can install the desktop launcher.
- `RUN_INSTRUCTIONS.md` — short launch notes.
- `src/` — Java source.
- `assets/art/` — bundled core low-resolution art and fallback material.
- `assets/sound/` — bundled core sound effects and lightweight ambient cues.
- `assets/artpacks/` — optional drop-in art-pack folder.
- `assets/audiopacks/` — optional drop-in audio/music-pack folder.
- `assets/indexes/` — machine-readable asset index data used by tooling/runtime.
- `docs/` — four durable developer source-of-truth documents.
- `modding/` — public modding API reference and six complete example template mods with manifests and Java integration classes.
- `scripts/package/` — Linux and Windows native installer packaging scripts.
- `scripts/security/` and `config/proguard/` — ProGuard, encrypted mapping, and sensitive-string generation pipeline inputs.
- `.github/workflows/package.yml` — CI workflow for platform installer builds and unified release ZIP assembly.




## 0.9.10hy mod sandbox and transaction hardening

The server/security line now includes a stronger Java 17 mod pre-loader scanner, a stricter sandbox class loader, sealed data-only mod payload mapping, instrumented runtime intrusion detection, bounded exception/fuzzing detection, idempotent server-authoritative purchase validation, and monotonic gameplay packet sequence checks. These systems are implemented with native Java and optional adapter seams; Netty-specific handlers remain adapter work until Netty is actually packaged.


## 0.9.10hw secure server architecture foundation

The headless server now includes a zero-trust networking/security foundation for the multiplayer line. Native Java 17 authorities cover load-aware direct acquisition throttling, strict pre-live handshake phase separation, AES-GCM packet metadata validation and replay rejection, bounded native relay frames with read timeouts, dual-direction packet policing, background-I/O degradation gates, local path traversal protection, HMAC-signed local save/config validation, masterlist heartbeat signing utilities, secure mod-classloader bytecode scanning, server-authoritative character profile storage with atomic saves, offline player session retention, safe login spawn resolution, and 60-second login protection with early action cancellation. Netty and Steam hooks remain optional seams and are not faked when those runtime adapters are absent.


## 0.9.10hv modding API template suite

The package now includes a public Java 17 modder-facing API seam under `src/mechanist/modapi/` and a complete template suite under `modding/examples/`. The six examples cover Sector, Room, Faction, Item, Knowledge, and Infopedia editor subsystems. Each example includes a valid `manifest.json` and a full Java integration class that implements `ModIntegrationHook`; compile-checked mirrored examples are bundled under `src/mechanist/modapi/examples/`. The engineering guide is `modding/API_REFERENCE.md` and begins with the required warning block about API drift during active engine development.


## 0.9.10hu simulation editor and mod packaging suite

The Mods / Tools route now opens a native Java 17 Swing Simulation Editor Suite. It contains seven editor tabs: Sector, Room, Faction, Item, Knowledge, Infopedia, and Mod Packaging. The workspace uses an event-bus/MVC-style separation, shared undo/redo commands, JTable property editing, combo-box link editors, bounded JSpinner numeric editors, and a SwingWorker deployment path so export work does not freeze the UI.

The Mod Packaging Editor binds selected sectors, rooms, factions, items, knowledge nodes, and infopedia entries into a project scope. Fallback deployment writes a standard ZIP archive containing `manifest.json`, exported data JSON, documentation, version tags, dependencies, and SHA-256 structural checksums for staged content files. When Steam and a compatible steamworks4j runtime are actually present, the deployment manager detects that route and attempts the reflected SteamUGC publication path; otherwise it transparently uses ZIP export instead of pretending Workshop upload succeeded.

## 0.9.10ht program icon integration

The desktop build now ships the new transparent Linux/Java application icon pack under `assets/app/icons/`. The Swing client loads the same icon sizes for the window/taskbar icon, the Linux `.desktop` launcher now resolves `Icon=the-mechanist`, and `install_linux_launcher.sh` installs the PNGs into the user hicolor icon theme.

## 0.9.10hs multiplayer host-binding foundation

The launcher now includes a Multiplayer surface with direct IP entry, bracketed IPv6 endpoint support, recent servers, editable favorites, Steam-environment detection, and host-from-current-world preparation. The headless server accepts `--host` and `--host-once` arguments and bridges world seed/name/setup/max-player settings into a server configuration before binding. The direct host path uses the configured `25500-25599` custom game port range, avoids system and Steam query ports, tries Steam detection first, then IPv6, then IPv4. When Steam/Netty adapters are not packaged, the build does not fake them; it falls back to a real Java NIO blind relay for encrypted packets.

## 0.9.10hr native Java E2EE chat packet foundation

The chat runtime now prepares native Java 17 end-to-end encrypted relay packets for multiplayer chat transport. `HybridEncryptionManager` uses RSA-3072 identity keys, RSA-OAEP-SHA256 temporary AES-key encapsulation, and AES-256-GCM payload sealing with 12-byte IVs and 128-bit authentication tags. `SecureChatPacket` carries only sender/recipient routing metadata plus Base64 ciphertext, IV, and encrypted AES-key envelope so a future central server can relay packets without reading message bodies. Public network hosting remains closed; this pass establishes the cryptographic packet seam and validates it with a smoke test.

## 0.9.10hq EULA/IP disclaimer update

The desktop runtime EULA now includes the unofficial fan-made project acknowledgment, explicit Games Workshop non-ownership/trademark disclaimer, zero-commerce statement, and removal-on-official-objection notice. The launch gate remains fail-closed until `settings/legal.properties` contains `eula_consented=true`; declining through **EXIT** or **I DO NOT AGREE** closes the program immediately. A root `EULA.md` copy is included beside this README for base-package visibility.

## Optional packs

The core archive intentionally stays lightweight. Higher-resolution graphics and larger audio/music packs should be installed as optional packs instead of being bundled into the core runtime package.

### Save/world persistence model

Current persistence uses two models. Single-player slots under `saves/singleplayer/` are bundled character-plus-world snapshots for exact reload at save time. Server/multiplayer character attachments remain separate from world authority files; world files store generated hive-world definition plus mutable active-world state such as NPCs, objects, room factions, population ledgers, hazards, player-faction continuity, and player-faction autonomy records.

## Save model

Single-player save slots are full bundled snapshots of the active character and the world at save time. Server/multiplayer worlds keep their world state in the server world file, while player save records remain separate character attachments.

## 0.9.10gr persistence update

Single-player saves remain bundled snapshots containing character state plus the embedded world definition and mutable world state. Server/multiplayer character slots now strip world-owned runtime namespaces such as base, faction, NPC-site, news, bank, crime, scavenge, machine, and logistics state; those records are copied to the referenced `.mechworld` authority so the world can continue without a specific player slot loaded.

Persistence note: player-created factions are stored as world state. A returning character resumes command through their stable player ID; another character loading the same world sees the faction as an existing world organization rather than owning it by default.

## 0.9.10gu persistence update

Player-founded faction autonomy now writes a compact world-owned autonomous tick ledger. The ledger records elapsed turn windows, bounded production/trade/defense/morale deltas, stock estimate, risk state, reserved player IDs, and a public continuity note. Server/multiplayer character slots still strip this world-owned state, while single-player bundled saves include it as part of the full save-time world snapshot.

Admin diagnostics include `/faction_tick [turns]` for previewing the faction autonomy tick outcome from current world state.

## 0.9.10gt persistence update

Player-created factions now record a first autonomous continuity ledger in world state. The world file tracks the faction's reserved player slots, separated player/NPC command structures, production intent, trade intent, defense posture, and public/news continuity so the organization can persist without requiring a specific player save to be loaded.


## 0.9.10gv faction/personnel parity update

Player-founded factions now record a shared command-tier scale for separate player and NPC command structures. The founder remains a unique tier-0 slot, while recruited players use tiers 1-5; a rank-N player has equivalent rank-N NPC command authority without merging the player roster into the NPC roster. Developer summaries `/rank_parity`, `/personnel_parity`, `/ui_framework`, and `/management_ui` expose the current parity and management-window descriptor state.


## 0.9.10gw display-density update

The desktop client now applies display-density policy before Swing startup: Java2D UI scale is forced to native 1:1, JVM text anti-aliasing is requested globally, standard Swing fonts are scaled through the UI defaults table, and custom Java2D panels apply LCD text antialiasing plus fractional metrics for high-density text.

## 0.9.10gy visual lighting pass

This build adds a render-only visual lighting authority for deterministic flicker and low-resolution bloom. Gameplay light values remain turn-stable for vision/combat math; the visual layer only decorates the map pane during rendering. Graphics options now include Visual Lighting FX: Off, Static Lightmap, or Flicker + Bloom. Reduced Motion suppresses live flicker animation while preserving stable lighting density.

## 0.9.10gx display and graphics menu update

Display and Graphics options have been reorganized. Display now handles window mode, runtime-detected/safe resolutions, font/text scale, GUI scale, hover help, screensaver, and apply-display behavior. Graphics now handles render downscale, target FPS, render quality, art quality, tile size, tile-art rendering, portraits, reduced motion, render profile, palette, and brightness controls.

## 0.9.10hd project re-evaluation and replanning pass

This build adds a structured project re-evaluation authority for host/developer review. Console summaries `/project_evaluation`, `/gap_analysis`, `/replanned_goals`, `/order_of_operations`, and `/hidden_dependency` expose the current gap analysis, rewritten goals, ordered next tasks, and the largest hidden dependency without creating extra standalone planning documents.

## 0.9.10hj gameplay-defaults quality-of-life pass

The Options screen now includes a QOL tab for default behavior that prevents common survival/base-builder friction: subtitles, repeat-splash skipping, auto-loot preference, omni-directional ghost build, hold-to-repeat construction, smart storage filters, proxy crafting from linked storage, production blocker warnings, market alerts, favored-item protection, and item safety profile strictness. These are persistent preferences and integration contracts for current and future inventory, logistics, construction, production, market, and alert systems.

## 0.9.10hm Progressive Construction State

Construction now supports staged under-construction sites. A confirmed build places a reserved construction site that can accept materials and labor over time, draws a per-tile progress bar, and fades from pale blue ghost coloration toward the final built color as progress increases. Interacting with an unfinished site contributes available components and tool-adjusted labor; deconstruction/clearance time estimates now respect equipped construction-capable tools or tool-like weapons.


### Multiplayer / Headless Server Foundation

This build adds the first host-binding and multiplayer menu foundation. The server jar can be launched with `--host` or `--host-once` and accepts world seed/name/setup, max player count, and configurable direct TCP ports in the `25500-25599` range. The desktop launcher now has a Multiplayer surface with direct address entry, recent servers, favorites, Steam-environment detection, and host-from-current-world preparation. Steam and Netty paths are detected but not faked when their adapters are absent; the packaged fallback is a real Java NIO blind relay for encrypted packets.


### 0.9.10hy network optimization / local host conversion pass

This build adds the first staged implementation of low-bandwidth replication and local-host conversion infrastructure: server-side snapshot delta compression, client prediction/reconciliation, remote entity interpolation buffers, NAT/STUN discovery helpers, Linux systemd deployment assets, disaster-recovery crash dumps, explicit single-player-to-local-multiplayer warning/lock flow, host-key/password admission gates, local host dashboard panels, transactional host-profile/world-data splitting, and multiplayer respawn fallback placement. Netty and Steam remain optional seams; this build does not fake those runtimes when their dependencies are absent.


## 0.9.10ic Crash Log De-Obfuscator

The admin tooling now includes a Crash Log De-Obfuscator panel for release builds. Developers can load a plain ProGuard `mapping.txt` or the encrypted `MECHANIST-MAPPING-V1` mapping payload created by the packaging scripts, paste an obfuscated player crash trace, and reconstruct original class/method names with line information where the release map contains it. The tool runs parsing and reconstruction on background workers so the Swing admin panel stays responsive. Raw mapping files and mapping keys remain developer-only materials and are not part of player-facing installer bundles.


## Doom rendering note

The experimental QoL-gated `doom` mode now supports selectable linear Z-depth fog and radial distance fog. Linear mode keeps edge columns from fogging oddly under wide FOV by using perpendicular depth; radial mode gives the player a circular visibility bubble. Client runtime profiles now request Java2D hardware acceleration before Swing startup, and the 2D lightmap path uses a VolatileImage surface when the host graphics environment supports it, falling back safely in headless mode.

### 0.9.10it UI Typography / Text Density Repair
- Default text density is now compact instead of legacy 100% enlarged defaults.
- Text scale and GUI scale remain adjustable upward from Options > Graphics/Display.
- Swing font scaling now uses captured baseline fonts instead of multiplying already-scaled defaults.
- Text crispness is controlled by the render-quality/crispness setting separately from text size.


## 0.9.10jl Zone Audit selector and multiplayer menu repair

The developer generation inspection surface is now user-facing as Zone Audit. Its zone-type button opens a bounded pop-up selector containing every available zone type, and selecting one regenerates that audit target. The multiplayer surface has been enlarged and split into a left content/status area plus a two-column right command area.

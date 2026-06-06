# The Mechanist — Master Governance Revision II

This document contains long-term doctrine. It is not a pass ledger and it is not a changelog.

## Core doctrine

The project is now in a platform-architecture era rather than a loose prototype era. The goal is not to add every interesting mechanic immediately. The goal is to reduce the future cost of complexity by building shared authorities, stable data boundaries, inspectable tooling, and durable documentation discipline.

## Source-of-truth doctrine

The durable development-document root is `ROOT_docs/`.

Standing development documents, governance, standards, milestone plans, generated ledgers, history, and architecture records belong under `ROOT_docs/`. Historical references to a root `docs/` directory mean `ROOT_docs/` unless explicitly describing an archived legacy path.

The durable tool root is `ROOT_tools/`. Durable scripts, auditors, scanners, indexers, generator tools, packaging helpers, classfile scanners, and maintenance commands belong under `ROOT_tools/`.

The durable build-operations root is `ROOT_build/`. Checked-in build orchestration, build definitions, and durable build helper entry points belong there unless they are package-owned under `PACKAGE_*` or general maintenance tools under `ROOT_tools/`.

The current primary durable documents include:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md` — phase roadmap and current target sequence.
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md` — durable rules that prevent regressions.
3. `ROOT_docs/DOCUMENTATION_STANDARDS.md` — repository storage, documentation, tooling, build, generated-ledger, and purgation rules.
4. `ROOT_docs/DEVELOPMENT_HISTORY.md` — completed work and build notes.
5. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md` — doctrine and architectural boundaries.

No other standing development documents belong at repository root or in ad-hoc top-level folders unless explicitly requested by the user. Notes, audit files, architecture addenda, and pass summaries must be folded into the appropriate durable document, moved to `ROOT_docs/archive/`, regenerated from tools, or deleted.

## Runtime separation doctrine

The long-term main menu should become a launcher/orchestrator for the game client and an internal headless server. That future must be planned before mods, multiplayer, source acquisition, or authoritative server ticking are opened.

Current permitted work is Phase 4 UI, input, rendering, and presentation containment. The local launcher/tools boundary and Sector Editor Audit tooling are no longer the active target; they return to the Phase 15 editor/modding line unless explicitly reopened. The project must not claim live external mod loading, classpath mutation, multiplayer, hot restart, or server authority before those systems have explicit architecture, safety, package, and test gates.

## Distribution path doctrine

The intended user execution path is installer → thin launcher → client → server. The installer installs the smallest durable launcher/orchestrator layer and any files required for that launcher to start, present diagnostics, verify manifests, and manage updates. The installer must not require the end user to download or unpack the full development repository.

The thin launcher owns acquisition, verification, installation, update, rollback, and launch of the client package, headless/internal server package, Java runtime image when bundled, and runtime support libraries. Support libraries include graphical/native dependencies such as LWJGL, controller/input bridges such as Jamepad or its replacement, networking/runtime libraries such as Netty when used by a launched package, and any future explicitly required support library. These libraries are acquired and verified as launcher-managed package artifacts before the client starts; the game client must not opportunistically download libraries during game launch.

The client is a launched runtime package, not the updater. It may verify that required support libraries are present and fail loudly with a visible log if the thin launcher or manifest acquisition failed, but it must not mutate its own classpath by downloading dependencies at startup. The client may start or connect to the local/internal server lane after its own package and support-library set has passed manifest and integrity verification.

The server is a separate launched authority package with its own manifest identity, runtime profile, storage namespace, and verification path. The server must not depend on graphical client libraries unless an explicitly tested shared package requires them. Client/server package identity, checksums, versions, and compatibility constraints must be visible in launcher manifests rather than inferred from loose repository layout.

## Shared authority doctrine

Every durable shared framework reduces the cost of future content.

New systems should prefer named authorities and APIs over local switch clusters or central-class growth. Shared operations, UI, construction, logistics, production, item identity, portrait identity, tile descriptors, and audit surfaces should be reused rather than reinvented.

## Tile/data identity doctrine

The project has outgrown raw ASCII glyphs as a complete source of truth. Glyphs may remain compact input tokens, but the renderer, inspector, audit tools, and future editor should consume compiled descriptor identities that separate terrain, fixtures, overlays, roads, sidewalks, walls, floors, factions, and semantic markers.

This is a control and readability doctrine, not a license to open full simulation everywhere.

## UI and operational clarity doctrine

A complex simulation is only useful if the player can understand what it is doing. UI containment, readable targeting, clear failure messages, visible item icons, correct portrait categories, and inspectable tile descriptors are not polish luxuries. They are core infrastructure.

No major subsystem should invent its own UI behavior when a shared UniversalWindow or runtime support pattern exists.

## Simulation tier doctrine

Simulation depth must scale by relevance.

- Tier 1: immediate reality layer near the player; detailed machinery, hazards, AI, combat, queues, and operations.
- Tier 2: operational ledger layer for local districts; staffing summaries, logistics summaries, inventory aggregates, and trade abstraction.
- Tier 3: strategic world layer; macroeconomics, distant industry, shortages, banking, faction stability, and strategic logistics.

Large-scale economy and faction systems must consume schemas and ledgers rather than hidden full-tile background simulation.

## Asset lifecycle doctrine

An art file is not implementation. An indexed atlas cell is not implementation. A placeholder handle is not implementation.

Promotion requires a semantic runtime name, a consuming authority, inspection/use meaning, placement or generation path, and fallback behavior. Source art and high-resolution packs do not belong hidden inside the core runtime archive when they are not required by the runtime.

## Editor and modding doctrine

The eventual editor should make zone stamps, features, entities, factions, machinery, recipes, items, tiles, and localization durable and interoperable. The local Sector Editor Audit surface is a parked early tooling bridge toward that goal, not the finished modding system and not the current Phase 4 workstream.

External mod sources, hot-loading, source acquisition, and multiplayer-compatible mod orchestration remain closed until the launcher/client/server boundary is intentionally built.

## Closing directive

When development begins, consult the master plan, standards, governance, and history in that order. Do not mistake a note, asset index, screenshot, README entry, or planned feature for implemented code. Do not create a new document when an existing durable file is the correct place for the information. Do not create new ad-hoc root folders for docs, tools, build outputs, diagnostics, or scratch material when `ROOT_docs/`, `ROOT_tools/`, `ROOT_build/`, `ROOT_SRC_assets/`, or an owning `PACKAGE_*` tree is the correct home.

Phase 4 governance note: input corrections must be authority-based. When rendered map geometry and mouse targeting disagree, the repair belongs in a shared map viewport transform authority, not in hardcoded per-resolution offsets. Command-surface containment must also be authority-based: a button belongs to its owning command surface because of the active screen/panel, not because a stale coordinate happened to intersect a frame. Ordinary player-facing surfaces should not explain development phase numbers; those belong in the durable documents and developer-only tooling.

Phase 4 efficiency note: repeated presentation work should be cached through named authorities with bounded memory, visible audit metrics, and no change to gameplay authority. Per-frame optimizations must not invent alternate tile, portrait, item, or input meanings.

## Server/runtime separation doctrine — 0.9.10gm

The launcher/client/server line must preserve physical save separation as well as authority separation: server and single-player save files must not share path authority. Client-facing countdown indicators for gated action time are presentation of server state, not an independent client timer.

## Persistence authority doctrine — 0.9.10gn

Generated world-definition ledgers are world-file authority, not save-slot filler. Save slots resume a run and current mutable slice; `.mechworld` files own generated world identity/provenance/history. New persistence work must measure and catalog payload contents before claiming efficiency, and must prefer references over duplicated ledgers where a durable owning file already exists.

## Persistence governance — world continuity and character authority

A world must be able to persist as a living state independent of the currently loaded character. Character saves attach a player identity to that world; they do not own the world. Player-created factions are persistent world organizations with player membership records keyed by stable player IDs. A returning character resumes the command rights recorded for that player ID. A different character loading the same world sees the faction as an existing organization rather than becoming its owner by implication. Player rank assignments and NPC command structures are equivalent in authority level but stored as separate chains so multiplayer membership can coexist with autonomous NPC management.

### Persistence authority doctrine — 0.9.10gr

Single-player slots are allowed to be large because they are exact save-time world snapshots. Server/multiplayer slots are not world saves; they are player/character attachments to a living world authority. A player-created faction is a world entity with separate player and NPC command structures, reserved player membership by stable player ID, and autonomous continuity when its founder is absent.

### Player-faction autonomy doctrine — 0.9.10gt

A player-founded faction must be capable of continuing as a world organization while the founding player is absent. Its autonomous production, trade, defense, news, NPC command continuity, and reserved player command slots belong to world state. Character saves may identify the returning player and their resume rights, but they must not be the only place where the faction's operating plan exists. Player assignment surfaces must remain distinct from NPC staffing surfaces even when rank names overlap.

### Autonomous player-faction tick doctrine — 0.9.10gu

A player-founded faction does not cease to exist when its founding player is absent. Its autonomous progress belongs to the world, not to the character slot. Production, trade, defense, morale, stock, risk, and public continuity outcomes must remain tied to the faction/world ledger so the server world can continue without requiring a specific player save to be loaded.

## Player/NPC Command Parity Doctrine

Player-founded factions use two command structures: one for players and one for NPC personnel. These structures remain separate for identity, persistence, and UI purposes, but they share the same command-tier scale. A rank-N player can command the same NPC tier that a rank-N NPC can command. The founder is a unique tier-0 authority and is not a recruited-player rank. This prevents multiplayer recruits from becoming equivalent to the faction founder while still giving them clear operational control over NPC personnel at their assigned tier.

### Accessibility compatibility doctrine

The project treats accessibility and compatibility as engine-level presentation infrastructure, not as optional cosmetic afterthoughts. The custom Java2D canvas must expose readable text density, color-vision accommodations, reduced motion, and narration hooks where feasible, while preserving deterministic gameplay authority on the server/world side.

## Profile identity and migration governance

The profile layer is not the character layer and not the world layer. Local profiles may carry operator configuration, settings, accessibility, JVM/runtime preferences, portrait selection, and migration metadata. They must not silently absorb world state, character inventory, faction ownership, or server state.

## Project re-evaluation doctrine

Project replanning is an engineering control surface, not a writing exercise. A valid replan must identify the implemented authority boundary, name the missing authority or migration seam, and assign a verification path before it can become active development work. The durable order remains: protect server/world authority, protect save compatibility, prevent UI fragmentation, and avoid opening multiplayer/networked authority before local command/snapshot/session boundaries are stable.

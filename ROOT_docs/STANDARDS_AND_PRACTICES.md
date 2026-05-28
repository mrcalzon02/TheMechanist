# The Mechanist — Standards and Practices

This document contains durable rules. It is not a changelog. When a pass completes, record what happened in `DEVELOPMENT_HISTORY.md`. When a roadmap changes, update `MASTER_DEVELOPMENT_PLAN.md`. When doctrine changes, update `MASTER_GOVERNANCE_REVISION_II.md`.

## Documentation containment standard

- `ROOT_docs/` is the durable development-document root. The active authority set is:
  - `MASTER_DEVELOPMENT_PLAN.md`
  - `STANDARDS_AND_PRACTICES.md`
  - `DEVELOPMENT_HISTORY.md`
  - `MASTER_GOVERNANCE_REVISION_II.md`
  - `MILESTONE_INDEX.md`
  - ordered milestone files `MILESTONE_00` through `MILESTONE_10`
  - `LEGACY_MILESTONE_SOURCE_MAP.md`
- Archived, deferred, packaging, launcher, UI, and handoff documents may remain under named `ROOT_docs/` subfolders, but they are context material unless an authority document explicitly promotes them for the current pass.
- The user explicitly ordered one specialized durable exception: `STAGED_ASSET_INTEGRATION_PLAN.md`. It exists only to preserve the long semantic-asset migration path and must not become a changelog, file manifest, or general planning dump. When the migration is complete, either merge its remaining durable rules into this file/master plan or archive it deliberately.
- The pre-milestone development ledger is archived at `ROOT_docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`. It is read-only historical context; new completed work belongs in the active `ROOT_docs/DEVELOPMENT_HISTORY.md`.
- Do not add other pass-specific Markdown files, audit notes, architecture notes, addenda, index summaries, generated file inventories, or temporary planning documents unless the user explicitly orders a separate artifact.
- If a pass introduces a rule, put the durable rule here.
- If a pass changes the roadmap, put the planning change in the master plan.
- If a pass implements or repairs something, put the completion note in development history.
- If a pass changes long-term doctrine, put the doctrine in governance.
- README is user-facing. It may describe how to run the game and what the current build generally does. It must not become a changelog, roadmap, standards file, or asset-index commentary dump.
- `NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md` is a short resumption brief, not a second master plan.
- Generated indexes and manifests belong under the consuming package asset tree, such as `PACKAGE_client/assets/indexes/`, or under a tool/module path only when consumed by code, tools, or packaging. They must not be placed in `ROOT_docs/` as project-structure authority.
- User-ordered repository discovery exception: `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` lists every non-`.git` file, including binary assets, so images, audio, jars, docs, tools, and package files remain findable even when their content is not text-searchable. Regenerate it with `ROOT_tools/update-repository-file-manifest.ps1` whenever any file is added, removed, renamed, or moved. This index is for discovery only and must not be used as a runtime composition layer.

## Repository layout and asset placement standard

- The implemented top-level workspace names are authoritative: `ROOT_docs/`, `ROOT_SRC_assets/`, `ROOT_tools/`, `PACKAGE_client/`, `PACKAGE_launcher/`, `PACKAGE_installer/`, `scripts/`, and `src/`.
- Do not write new instructions that point to nonexistent legacy roots such as `docs/`, `client/`, `launcher/`, `installer/`, `assets/`, or `tools/` unless the instruction is explicitly discussing an old archived layout.
- `ROOT_SRC_assets/` is a protected source vault. Do not edit files there in place.
- When an asset is modified, renamed, compressed, converted, resized, cleared, or otherwise compiled for runtime use, place the output into the folder where the consuming architecture actually loads it.
- Runtime client assets belong under `PACKAGE_client/assets/` unless a specific client package subfolder owns them.
- Launcher runtime assets belong under `PACKAGE_launcher/java/src/main/resources/assets/` unless a launcher packaging script requires another owned launcher resource folder.
- Documentation may explain asset flow, but it must not create pointer-only manifests, placeholder README ladders, or false composition maps in place of moving the actual runtime files.
- Package manifests are allowed for acquisition, verification, update, rollback, and integrity checks. They are not allowed to stand in for physical asset placement.



## Semantic asset registry standard

- All new graphical asset references must route toward the Semantic Asset Registry using an exact 8-character asset ID. New gameplay/UI code must not directly reference graphical file paths except inside approved low-level registry, loader, migration, or tooling classes.
- Every player-facing semantic asset row must have an ID, type, path/URI, name, and semantic description. Empty descriptions, UNKNOWN types, duplicate IDs, and missing paths are release-gate failures for the active registry.
- The same physical graphical asset path should not receive multiple semantic IDs unless a future explicit alias system records the duplication and explains why it is unavoidable. Stage 3 establishes path uniqueness as the normal rule.
- The InfoPedia `ASSETS` tab is the audit surface for semantic assets. If an asset appears in-game, it should eventually be discoverable by ID, type, image preview, and description through the InfoPedia asset index unless explicitly marked hidden/internal.
- High-error categories — water barrels versus shelves, cots/beds, knives versus bolters, armor/clothing, roads, sidewalks, corridors, walls, corpse/decay markers, and machines — must use the reconciliation crosswalk during migration so old wrong-art fallbacks do not get reintroduced.
- Item and UI previews that have a semantic asset resolver must ask the registry first. Legacy icon aliases are allowed only as transitional fallback bridges and must not silently override a valid semantic asset ID.
- Tile descriptors must expose semantic asset IDs for their primary, underlay, and overlay art whenever a registry mapping exists. Zone Audit and Tile Infopedia must report those IDs so tile/floor/wall/corridor/road/transition mismatches can be diagnosed from the in-game audit surface. Java2D tile rendering may retain legacy alias/glyph fallback during migration, but registry ID lookup must be tried first once a tile family enters Stage 5.
- Construction recipes, built base objects, map fixtures, interactables, traps, lights, and editor palette entries that have entered Stage 6 must expose or resolve a semantic asset ID before falling back to older tile-art aliases. Construction/editor UI should show asset IDs and registry previews so wrong-object art is caught during normal inspection instead of later gameplay surprise.
- Portrait, faction, creature, servitor, child, pet, and name-locked profile art that has entered Stage 8 must be partitioned by semantic metadata. Ordinary player character creation may only draw from explicitly allowed human/profile partitions; name-locked portraits, creature/pet/beast portraits, servitors/automata, mutants, children, and hostile faction portrait pools must not leak into ordinary random player generation.
- Migrated semantic asset families that have entered Stage 9 must fail visibly with typed missing-art fallbacks rather than silently falling through to semantically wrong legacy art. Missing item, wall, floor, road, sidewalk, corridor, object, fixture, machine, portrait, UI, armor, weapon, and corpse/decay art must be distinguishable during normal play/audit.
- Remaining legacy graphical loader boundaries must be listed in `PACKAGE_client/assets/indexes/semantic_asset_deferred_legacy.tsv` with an owner, reason, and next action. This ledger is not permission for new direct path usage; it is a shrinking containment list for unavoidable pre-Stage-10 bridges.

## Application icon and launcher asset standard

- The program icon is a first-class runtime asset, not only a desktop-launcher decoration.
- Keep the Java/Swing window icon, taskbar icon attempt, Linux `.desktop` `Icon=` identifier, and installer-copied hicolor PNG assets aligned to the packaged `PACKAGE_client/assets/app/icons/the-mechanist-*` and launcher resource icon sets.
- Do not replace the icon by editing only the `.desktop` file; update the bundled icon assets and the runtime icon authority together so the extracted zip, jar resource path, and installed launcher remain consistent.


## Packaging and user-storage standard

- Native installers must be produced on their own target operating systems; do not claim jpackage cross-compilation for MSI, DEB, or RPM artifacts.
- Maven/shade packaging may introduce external runtime libraries such as Netty, but the packaged build must distinguish between dependency declaration, compiled adapter seams, and actually exercised runtime behavior.
- jlink runtime images contain JDK modules only; application and dependency bytecode belongs in the shaded application jars consumed by jpackage.
- Installer-controlled application directories such as `/opt/the-mechanist` or `Program Files` must not be used for mutable saves, profiles, active mods, exported mods, or archived mods.
- Route mutable data through `GameStorageManager`, with saves under `TheMechanist/saves/`, authoritative player profiles under `TheMechanist/saves/data/profiles/`, exports under `TheMechanist/export/`, live mods under `TheMechanist/mods/`, and archived mods under `TheMechanist/modsarchived/`.
- Any file name or path segment entering storage helpers must be normalized and verified to remain inside the approved user-storage root before reads, writes, moves, or exports occur.
- Windows installer packaging must provide a directly runnable app-image/portable output in addition to EXE/MSI installers so local testing can verify the bundled runtime before testing the installer wizard.
- Windows installers must request Start Menu integration, a desktop shortcut, shortcut prompt, directory chooser, and the bundled `.ico` application icon. Do not treat Linux `.desktop` icon wiring as sufficient Windows icon support.
- Windows packaging scripts must print explicit diagnostics for missing JAVA_HOME, Maven, jpackage, or WiX instead of failing silently or relying on a double-clicked PowerShell window that closes immediately.
- Shipped archive paths must remain Windows Explorer-safe: runtime art assets use short canonical roots and cell filenames, while semantic meaning is preserved by directory architecture and loader aliases rather than repeated long names.

## Obfuscation and release-security standard

- Release installer packaging must consume the obfuscated client jar, not the raw shaded development jar.
- Public modding API classes under `mechanist.modapi.**`, mod payload records, and launch entrypoints must remain stable through ProGuard keep rules.
- Internal networking, security, transaction, packet sequencing, anti-macro, and intrusion-detection classes should remain eligible for aggressive renaming unless a tested runtime/reflection path proves that a specific keep rule is required.
- Java 17 `Record`, `PermittedSubclasses`, `Signature`, `InnerClasses`, annotation, exception, source, and line-number metadata required for sealed/record correctness and de-obfuscation must be explicitly handled in ProGuard configuration.
- Raw ProGuard mapping files must be encrypted immediately after obfuscation and must not be distributed with player-facing installers. Developer de-obfuscation keys belong outside the shipped application archive.
- Crash log de-obfuscation tools may load raw or encrypted release mappings only from developer/admin-selected paths. They must not auto-discover or ship private mapping keys inside the player runtime, and missing mappings must be reported inline rather than crashing the admin tool.
- Sensitive security/disconnect strings may be routed through `ObfuscatedStringTable` to remove easy constant-pool search anchors, but this is an obfuscation measure only. Never store real secrets in client/server bytecode.


## Experimental first-person rendering standard

- The hidden first-person 3D viewport is exposed only through the Quality of Life toggle labeled exactly `doom` mode.
- `doom` mode must default to disabled and must show a confirmation warning before it can be enabled. Cancelling the warning must leave the renderer locked.

- Shared combat/status HUD rendering must remain renderer-agnostic. The bottom HUD overlay is allowed to draw over both normal 2D mode and experimental `doom` mode, but it must not move authoritative gameplay state.
- `doom` mode camera bob, recoil, tracer, glitch, and particle effects are visual-only client feedback layers. Movement and interaction authority must remain tied to the existing 2D grid/server-authoritative logic.
- High-frequency visual feedback systems should prefer primitive arrays, reusable buffers, cached fonts/colors, and bounded object pools to avoid garbage-collection spikes in the 30 FPS render loop.
- The mode is experimental; keep the Java2D software backend functional even when LWJGL is declared in Maven, and do not claim OpenGL/LWJGL rendering is active until an actual LWJGL backend is initialized and tested.
- Graphical native/runtime dependencies such as LWJGL must be treated as real shipped runtime assets, not only Maven declarations. Direct Windows/Linux launchers must include `lib/**/*.jar` on the classpath, startup preflight must report whether LWJGL is present, and any future OpenGL backend must fail loudly or fall back honestly rather than pretending a missing backend is active.
- Mouse-look, raycast targeting, door interaction, and first-person movement remain client/UI affordances over server-authoritative grid movement and interaction checks.
- Continuous first-person movement must keep double-precision local coordinates but derive authoritative logical tile occupancy from the player center point. Collision must test the player radius/AABB against blocked grid tiles and full-tile entity blockers before committing movement.
- Field-of-view values must remain bounded to 60-110 degrees and must be persisted through `GameOptions`.

## Loot and drop authority standard

- Entity death loot must route through `LootDropSystemAuthority` rather than ad-hoc corpse lists when a new death path is touched.
- Carried NPC armor, equipment, ammunition, medical items, and misc goods resolve through faction/role/zone-tier drop rules before entering corpse or ground-loot containers.
- Imperial Script from corpse search is a one-time corpse marker value, not a repeatable item stack. Mutants, cultists, heretics, and rogue machines must not generate script by default.
- Secure random 2% catalog injections are allowed on death and zone containers, but they must run outside render/tick hot loops and must not allocate per-frame state.
- Zone container bonus injection tables should remain purpose-distinct: manufacturing favors intermediates/base items, Guard/PDF favors finished military goods and rations, noble zones favor high-end foods/paperwork/finished goods, and trash/cult/mutant zones favor salvage or corrupted low-end finds.
## Zone Audit naming and containment standard

- The user-facing generation inspection surface is named **Zone Audit**, not Sector Audit.
- Zone Audit may keep legacy internal class names until a safe refactor, but menu labels, route labels, reports, hover text, and current-facing debug surfaces should use Zone Audit wording.
- Zone Audit zone selection belongs in a bounded pop-up opened from a single zone-type command button, not as a duplicated text dropdown and not as a long embedded command-rail list.



## In-game UI ownership standard

- Player-facing game menus, progression screens, editors, audit surfaces, and purchasing flows must be owned by the main game UI surface. Do not expose them as detached `JFrame` or `JDialog` windows unless the user explicitly orders a separate developer utility.
- A Java/Swing implementation is not automatically acceptable if it bypasses the game's own frame, render scaling, input routing, button authority, and UI visual language.
- Character progression and knowledge purchases must be rendered, clicked, and confirmed through in-game screens/frames. Infopedia may describe knowledge, but it must not own purchases, and no external window may be required for normal play.
- Game-owned editor screens must follow the same rule: construction/build editors, room editors, item editors, faction editors, knowledge editors, Infopedia editors, and packaging/export tools must enter through a main-client screen unless the user explicitly asks for a detached developer-only utility. Legacy `JFrame`/`JDialog` editor launch paths must not be exposed from player-facing menus.
- Editor palettes and nested selection lists must be bounded inside their owning frame or pop-up, scroll when long, expose all known entries, and visibly mark locked or unavailable entries without letting those disabled entries execute.
- Room/grid editors must mutate the editor data model first, then repaint the view. Left-click placement, right-click erasure, drag painting/erasing, universal NEW entry creation, undo/redo, and helper-banner toggles belong to the in-game editor interaction contract.
- Screen-space lighting, bloom, particles, and post-process effects must be clipped to the world viewport or explicitly disabled around UI chrome. They must never wash over menu frames, command panels, or modal text areas.
- The packaged default display policy is highest detectable supported resolution on launch unless the user has explicitly selected and saved a different resolution. Do not reintroduce a player-facing CRT/render-profile cycle button or hotkey unless the user explicitly reopens that feature.

## Build and delivery standard

- Use Java 17 compatibility. This means Java 17 runtime compatibility, not merely writing code that looks like Java 17 source.
- Before a code/package pass, read this Standards and Practices file, the Master Governance file, and the Master Development Plan as active constraints. Treat this file as hard release law, governance as design ethos, and the master plan as movement/order of operations.
- Compile from source before delivery. Do not patch jars with stale classfiles and do not rely on previously compiled build directories as proof of correctness.
- Any direct `javac` invocation used for project source, smoke tests, launch probes, or hotfix classes must include `--release 17`. If Maven is available, the Maven compiler release target must remain 17. If Maven is unavailable and direct `javac` is used, explicitly state that direct compilation was used and that `--release 17` was applied.
- Rebuild the runnable client and server jars before packaging. Both jars must be produced from the same corrected source tree and target the same release gate.
- Before packaging, run a classfile-version scan against every shipped jar and any loose shipped class directory. Java 17 classfiles have major version 61; no packaged classfile may exceed major 61.
- A launcher smoke running under a newer local JDK is not proof of Java 17 compatibility. The classfile scan is mandatory and must be reported in the verification summary.
- Rebuild the zip from the corrected project tree only after compile, jar rebuild, launcher/preflight smoke, and Java 17 classfile scan have passed.
- Run zip integrity checks.
- For code changes, run targeted smoke tests tied to the touched subsystem, compiled with `--release 17` when direct `javac` is used.
- State honestly what was not manually tested.





## Mod sandbox and transaction-security standard

- Downloaded or third-party mod jars must be scanned before any ClassLoader receives them. Scanning must reject dangerous constant-pool references, binary strings, path-traversal zip entries, file/process/reflection/network/native-access calls, and oversized entries.
- Mods may resolve only the public `mechanist.modapi` seam plus a small explicitly harmless Java allow-list. They must not resolve internal `mechanist.*` classes, file APIs, process APIs, reflection APIs, sockets, native/internal JDK packages, cryptography packages, or runtime/system exit hooks.
- Mod content should prefer sealed, immutable, data-only payload records that the engine interprets. Do not grant downloaded mods unchecked structural loop authority when a data payload can express the same room/item/faction/sector/knowledge/lore change.
- Runtime intrusion monitoring must be honest about Java limits: `StackWalker` is used at instrumented execution boundaries for the current thread, not as a fake omniscient arbitrary-thread stack inspector. Background security loops may verify hashes and exception frequency without blocking simulation ticks.
- Purchase/item mutation requests must be server-authoritative and idempotent. Duplicate client transaction tokens may return the cached first result, but must never grant duplicate items or currency changes.
- Sequenced gameplay packets must be monotonic per authenticated session. Replayed or far-future packet IDs are disconnect conditions; optional native and future Netty adapters should share the same validator rather than duplicating sequence policy.

## Headless server security and authority standard

- Multiplayer clients are zero-trust inputs. They may request actions or upload opaque encrypted packets, but they must not author live world state, character state, inventory state, faction values, or server-side file paths.
- Asset acquisition and mod synchronization are separate from live world initialization. Do not serialize entities, chunks, voxel/tile state, or live simulation snapshots to a client until acquisition, layout verification, and integrity challenge phases have completed for that specific session.
- The handshake lifecycle order is identity verification, manifest delivery, acquisition/sync, client hot restart, integrity challenge, live world initialization, then access granted. The client hot restart phase must run off the I/O thread, must not close the active socket/session handle, and must complete before the integrity challenge can be issued.
- Direct acquisition traffic must remain load-aware and degradable. Active gameplay traffic has priority over background file/mod transfer, and degraded tick conditions should pause or throttle background I/O before harming live game tick responsiveness.
- Server-side file access must resolve through canonical root guards. Any client-provided path segment that escapes the approved root is an immediate denial condition.
- Character profiles for multiplayer/server play are server-authoritative records under the host runtime, written atomically through temporary files and final rename. Client-side local file edits are not trusted as multiplayer truth.
- Netty and Steam integrations may be optional seams, but code must not claim Netty, Steam Datagram Relay, SteamUGC, ByteBuf leak detection, or Steam identity validation are active unless the actual runtime adapter is present and initialized.

## Simulation editor and mod export standard

- Native editor tooling must keep Swing views decoupled from simulation data through the editor repository, event bus, and command runner rather than directly mutating live world runtime objects.
- Editor controls that can change state must route through undoable commands when practical; table edits, form metadata, and mod-scope selection should not bypass shared state tracking.
- Mod export and Workshop publication attempts must run outside the Swing Event Dispatch Thread through `SwingWorker` or equivalent background execution.
- Fallback mod export must generate a readable `manifest.json` with version tags, dependencies, selected entity scope, and structural checksums before writing the archive.
- Steam Workshop paths must detect a real wrapper/runtime and may use reflection seams while the dependency is optional; they must not claim upload success when Steam or steamworks4j is absent.

## Source-code hygiene standard

- Source code may contain compact comments explaining current behavior and invariants.
- Source code must not contain phase roadmaps, future-work lists, development-history prose, duplicate master-plan sections, or pass-numbered planning arrays.
- New work should extend named authorities instead of creating new unowned helper clusters in the main class.
- If a touched subsystem exposes a safe extraction seam, prefer a semantic authority/API over more central-class growth.

## Player-facing text standard

- Player-facing text describes current behavior only.
- Player-facing text must be diegetic or immediately useful character-facing information.
- Do not show placeholder, temporary, coming soon, not implemented, debug excuse, phase-label, random-generation explanation, or filler descriptor language to the player.
- Numeric/generated character facts should be presented as facts, not explained as products of the generator.
- Populated faction/resident rooms should have visible functional support where possible: sleep points, food/ration support, logistics storage, or work/machinery surfaces matching the room's purpose.
- Development/audit wording may appear only in developer tooling surfaces, not ordinary player-facing UI.

## Phase transition and deferred tooling standard

- Phase 3 is closed for durable scheduling as of 0.9.10fz. Do not continue treating Phase 3.6 as the active target.
- Mods / Tools and Sector Editor Audit are deferred to the editor/modding line in Phase 15 unless the user explicitly reopens them.
- Existing local audit code may remain in the tree, but new active work must not expand that tooling during Phase 4.
- World-generation fixes during Phase 4 must be narrow repairs required by rendering, input, or presentation correctness; broad worldgen growth belongs to later durable world/facility phases.
- Development/audit wording may appear only in developer tooling surfaces; normal player-facing UI must not expose development phase labels.
- Phase bucket lists belong in `MASTER_DEVELOPMENT_PLAN.md`; they must not become standalone pass documents.

## Tile descriptor and rendering standard

- Raw single-character map glyphs are compact generation/input tokens, not full visual identity.
- Active rendering and inspection must resolve tile meaning through compiled descriptor data wherever possible.
- A compiled tile descriptor should carry base layer, family, road/corridor shape, deterministic variant, primary art key, underlay art key, overlay art key, and semantic tag where applicable.
- Transparent semantic markers and fixture glyphs must draw over a valid inferred underlay; they may not render directly onto a black map pane.
- New tile meanings should be descriptor fields, semantic tags, fixture records, or art aliases before consuming additional glyph meanings.
- Descriptor fallback is a diagnostic condition and should be visible in developer inspection surfaces when those surfaces are active.

- Infopedia tile audit entries must expose the compiled tile descriptor, glyph, renderer alias, loaded icon state, and allowed variant set for every active tile family that can be assigned by world generation or room/faction context.

## Road, sidewalk, and street standard

- Road connectivity and road art shape resolution count only true road-lane tiles as road neighbors.
- Sidewalks are street-family tiles, not road-neighbor connectors.
- A sidewalk must not promote into a road lane for ordinary road-shoulder generation, rendering, or compiled-tile descriptor inference. The only allowed exception is a crossing-gap tile directly trapped between opposed true road lanes, which may compile/normalize as road continuity so intersections do not leave sidewalk gaps through the carriageway.
- Road art must distinguish north/south, east/west, direction-specific corner/round/end, intersection, and sidewalk families. Direction-specific atlas cells are not random visual variants.
- A road tile should become an intersection only when true road-lane continuity exists on both axes.
- Zone lights and road-frontage objects may use sidewalks and legal floors but must not overwrite true road-lane tiles.
- A generated zone should use one cohesive road visual variant set across the map; road descriptors may vary by direction/shape, but not by arbitrary per-tile road material hashes.
- Sector-scale roads are core infrastructure and must be seeded before ordinary rooms when using the road-first generator. Later rooms/corridors must fit into the remaining wall/carve space and connect back to legal street frontage rather than allowing roads to overwrite rooms after the fact.

## Sector generation audit standard

- Zone Audit may expose developer tooling to replay generation snapshots when the user is actively diagnosing world-generation order.
- Generation trace replay must be observational: it may snapshot reset, room, corridor, road, frontage, boundary, interwall, population, and tile-compile phases, but must not become authoritative gameplay generation logic.
- Rejected placement markers in audit surfaces are developer-facing diagnostics and must not leak into ordinary player-facing UI.
- Zone Audit trace order should expose infrastructure, transition, room, corridor, asset, interwall, population, and tile-compile phases clearly enough to diagnose generation-order failures without turning those diagnostics into player-facing text.

## Exterior boundary standard

- The exterior envelope is not a generic cube wall.
- The intended boundary sequence is: playable interior, inner exterior-maintenance bulkhead, exterior maintenance corridor floor, outer exterior-maintenance bulkhead, then void beyond the exterior.
- Exterior maintenance bulkheads may reuse wall glyphs internally, but compiled descriptors must identify them as exterior maintenance bulkheads.
- Do not place room-bearing pockets beyond the outer bulkhead unless a later phase explicitly reopens them inside a new boundary model.


## Zone generation diagnostics and trace standard

- Zone generation ordering must remain auditable through Zone Audit when generation logic changes. Plaza, road, room, corridor, frontage, boundary, interwall, population, and tile-compile phases should each leave trace records sufficient to identify what was attempted and when.
- Rejected placement attempts may be visualized as temporary ghost rectangles in Zone Audit trace replay, but a failed ghost must not mutate authoritative terrain and must disappear on the next trace step.
- Sidewalk frontage is directional generation metadata. Room placement should use the wall-facing side of a sidewalk/corridor frontage and must not treat true road lanes as ordinary room anchors.
- Room generation must preserve one authoritative room shell: room boundary cells are walls or doors/transitions, while access/corridor fabric outside that shell must be non-room terrain rather than a second room wall layer.
- Tile-art variation may use deterministic seed-owned noise for visual variation, but noise must not silently move gameplay-critical room, road, door, trap, or transition authority.

## World-generation weight standard

- The 500-1000 value is a clamped world-generation weight / variance band, not literal width or height.
- Zone Size settings may map into this band to control layout pressure, room quotas, spacing, and variation.
- Runtime slice dimensions must remain practical gameplay dimensions derived from that profile.
- Do not reintroduce a 500+ tile minimum edge clamp without a dedicated performance and audit gate.


## Experimental doom renderer fog and Java2D acceleration standard

- `doom` mode remains disabled by default and must stay behind the QoL confirmation gate.
- First-person fog must be selectable between linear perpendicular Z-depth and radial Euclidean distance; wall columns should use perpendicular depth for edge-stable fog when linear mode is selected.
- Fog blending must run through the shared depth-fog authority instead of ad-hoc per-renderer darkening.
- Runtime Java2D pipeline properties must be applied before Swing window creation. Client profiles may request OS acceleration; headless server profiles must not force desktop rendering pipelines.
- VolatileImage-backed render/lightmap surfaces must validate before drawing, handle lost contents, and retain a safe BufferedImage fallback for headless or incompatible environments.

## Display density and Java2D scaling standard

- Desktop Swing startup must configure Java2D/Swing display properties before any GUI window is initialized.
- The client should force Java2D UI scale to native 1:1 (`sun.java2d.uiScale=1.0`) so the operating system does not stretch a low-resolution Swing surface into blurred text.
- Text density and custom world/gui density are separate controls. Standard Swing component fonts scale through the Look-and-Feel defaults table with `FontUIResource`; custom game panels use their own Java2D transform/viewport logic.
- Tiny text and dense custom-panel rendering should use the shared display-density authority hints: LCD HRGB text antialiasing, fractional metrics on, and shape antialiasing on.
- Do not shrink arbitrary components by hand to fit more information. Route density changes through the display-density, UI containment, viewport-transform, and text-layout authorities.

## UI containment standard

- Every UI frame owns its text and controls.
- Captions are compact labels in the upper-left or frame title region; they are not the body of the window.
- Body copy must wrap and clip inside a child content rectangle.
- Buttons must be sized from the actual owning command/control frame and must not spill below or outside it.
- Command-surface button ownership is semantic, not geometric: if a panel owns command buttons, those buttons must be corralled into the command frame even when old placement coordinates start outside it.
- Dropdowns are popup/menu frames drawn above the parent menu; dropdown options must not be reflowed into the parent as ordinary bottom buttons.
- Transition, first-impression, dossier, survival, build, interaction, tactical, recent-action, and auspex/look text must wrap to the frame that contains it.
- High-churn UI text surfaces should use the shared text layout authority for pixel-width wrapping and bounded caching instead of recomputing rough character-count wraps every frame.
- High-frequency scaled map images and repeated UI chrome rasters should use the shared render image scale cache rather than repeatedly scaling the same tile, feature, object, NPC, player portrait, frame rail, frame corner, or panel-center raster every paint.
- Contextual management UI should migrate toward a universal window wrapper rather than separate isolated menu scripts. The wrapper owns chrome, title/header, close/back behavior, context target, shared input handling, and stable pane anchors.
- Interfaces that move items must preserve a stable player-inventory anchor and shared transfer semantics across containers, cargo routing, barter, and machine logistics.
- Dialogue/standings, container/routing, machine/recipe, and faction/personnel management panels should be module descriptors under the shared management-window authority before becoming duplicated Swing panels.
- Faction/personnel management UI must show player command slots and NPC command rosters as separate panes/modules while consuming the shared player/NPC command-tier parity model.
- Escape or right-clicking empty background should close or step back one layer in contextual windows once that window is migrated to the universal wrapper.


## Input and targeting standard

- Mouse-to-world targeting must use the same virtual render-layout/map rectangle used by drawing.
- Gameplay map rendering and gameplay mouse targeting must share one viewport transform authority for frame rectangle, cell size, camera origin, draw origin, and visible tile counts.
- Never correct mouse drift with fixed tile offsets.
- Zoom, borderless windowing, font metrics, tile-icon size, GUI scale, downscale, and letterboxing must be included in coordinate transforms.
- Look, Combat, Interact, movement preview, and future gamepad reticles should consume the same targeting model.
- Keyboard/gamepad reticle movement must not be swallowed by generic panel scrolling.

## Inventory and equipment standard

- Equipping an item must move a real item instance from carried inventory into a slot-specific equipment container.
- Replaced gear moves back into carried inventory.
- Equipment display strings are legacy/UI bridges only; item containers and stat calculations are authority.
- Defensive, offensive, and utility stats must recalculate from equipped item state.

## Asset and portrait authority standard

- All new graphical asset references should route toward the Semantic Asset Registry using an exact 8-character asset ID such as `TILE-A01`, `OBJ-WB01`, `WEAP-K01`, or `MACH-A01`. New gameplay/UI code must not add scattered direct graphical file-path calls except inside approved low-level registry, loader, importer, migration, or tooling classes. The current approved backend classes are `mechanist.assets.AssetMetadata`, `AssetType`, `AssetRegistry`, and `AssetManager`; the approved in-game browser bridge is `SemanticAssetInfopediaAuthority`. Gameplay migration should consume these rather than inventing parallel loaders.
- The Infopedia Semantic Asset Browser is an internal game-owned audit surface, not an external utility. Asset preview, type filtering, and text search must stay inside the existing game UI renderer unless the user explicitly orders a separate developer-only tool.

- Every player-facing semantic asset entry must have an ID, type/purpose, path/URI, asset name, and non-empty semantic description. The semantic description should explain intended use and should call out important forbidden substitutions when an asset is easily confused with another family.
- The Infopedia is the audit surface for semantic assets. If an asset appears in-game and is not explicitly hidden/internal, it should eventually be discoverable by ID, type, image preview, and description through an in-game Infopedia asset index.
- Follow `ROOT_docs/STAGED_ASSET_INTEGRATION_PLAN.md` for the asset migration order. Do not jump directly to full render-path replacement before the registry foundation, Infopedia browser, high-error category indexing, and preview migration stages have been completed and tested.
- Stage 7 direct graphical path enforcement is active. New gameplay/UI code must not introduce raw graphical file paths, direct `ImageIO.read(...)`, direct `new ImageIcon(...)`, or resource image loads outside approved low-level loader/import/migration surfaces. `PACKAGE_client/assets/indexes/semantic_asset_direct_path_allowlist.tsv` records explicit exceptions and `PACKAGE_client/assets/indexes/semantic_asset_direct_path_baseline.tsv` records temporary legacy debt; additions outside those channels are regressions until deliberately accepted.
- Folder names and semantic categories are authoritative.
- Player character/profile portraits may use only explicit player-human/profile folders; if the art pack lacks such a folder, the ordinary administratum human bucket is the only allowed standard fallback.
- Faction NPC portraits may use only exact matching faction/category buckets or a safe neutral human fallback where appropriate.
- Noble, Arbites/enforcer, PDF/military, mechanicus, medicae, ecclesiarch, mutant, cultist, genestealer cult, heretic, ganger, servant, child, pet, beast, servitor, and special entity portraits must remain partitioned by their named folders.
- No portrait selector may flatten all portrait folders into a universal random pool.
- Creature, pet, mutant, servitor, beast, cultist, noble, and special entity portraits must not leak into player creation or unrelated factions.
- Name-locked/celebrity portraits are a separate partition and must not appear unless explicit name-lock rules unlock them.
- Item thumbnails must resolve through named item-icon semantic buckets where available.
- Scrap/unknown icons are fallbacks only, not the default for every item.

## Audio and intro presentation standard

- Narrated intro playback must treat measured narration duration as a lower bound and include tail safety before automatic handoff.
- Manual skip/continue stops the narration immediately.
- Dynamic music should not compete with the narrated crawl.
- Text crawl timing may be average-duration driven rather than line-synced when that is the simpler and more robust implementation.

## Simulation-tier standard

- Immediate active zones may use detailed simulation.
- Distant districts should use operational ledgers and summaries.
- Strategic economy and faction systems must not become hidden full-tile simulations at distance.
- Server-authoritative sector simulation must be tiered by player presence: sectors with one or more players run full authoritative ticks; empty sectors immediately downgrade to lightweight ledger ticks or pause.
- Sector simulation workers must not overlap ticks for the same sector. Use Java 17 concurrency primitives, atomics, concurrent collections, or locks where state crosses threads.
- Heavy population/NPC-style runtime entities may be pooled when a sector empties, but pooled entities must be scrubbed before reuse.
- Network replication must be sector-isolated: players receive updates only for the sector they currently inhabit. No sector may broadcast internal state to non-inhabitants.
- The desktop single-player runtime must route player turns and committed world-mutation bodies through the internal-server single-writer authoritative world lane by default. Swing submits requests and renders published snapshots; the sector manager owns presence, tier state, tick accounting, non-overlap guarding, and sector-isolated local turn packets. Player actions that are converted during this server-authority line must enter as named command request objects rather than anonymous UI-side mutation bodies. True per-sector parallel world mutation remains deferred until shared world state is split or made safe for independent sector writers.
- Long-duration player actions must be gated by the server-side `PlayerActionRegistry`. Progress may be shown to the client, but completion must resolve through a named `WorldCommandRequest` or other owned authority record; do not complete long actions by storing arbitrary world-mutating `Runnable` payloads in the registry.
- Authoritative world commits should publish immutable `WorldSnapshot` records for client/render/network-facing consumption. The Swing renderer may continue to use legacy draw helpers during migration, but any server-authority surface added during this line should prefer published snapshot data over direct live `World` reads.
- Console/admin input must enter as an explicit request carrying player/session context. State-mutating admin operations must route through named command records or owned server authorities, not loose UI-side edits.
- Rejected commands should leave diagnostic evidence for gated players, invalid targets, no admin rights, foreign/stale sessions, and other authority-boundary failures.

- Desktop single-player saves and world-definition files must remain physically separate from headless/server-world files. Single-player manual/autosave files live under `saves/singleplayer/`; server state, server slots, and server/multiplayer world definitions live under `saves/server/`.
- Long-action countdown indicators shown to the player must be derived from server-side `PlayerActionRegistry` state. They may decorate the player icon, but they must not become a client-side action timer or grant input authority independently.
- A separate headless server executable may initialize and report the server runtime namespace, but public networking, remote clients, and remote console authority remain closed until explicitly opened by a later server phase.


## Persistence efficiency standard

- Desktop single-player and headless/server persistence intentionally use different save models.
- Single-player `.mechsave` files are bundled save-time snapshots. They should carry the active character/player state plus embedded `worlddef.*` and `world.*` data so loading that one single-player slot restores the world exactly as it existed when the slot was written.
- Single-player `.mechworld` files may still be written as an external continuity/reference copy, but the single-player slot's embedded snapshot is authoritative for loading that slot when present.
- Server/multiplayer player slots are character/player attachments, not full world saves. They should store character identity, stable player ID, inventory/equipment/account state, knowledge/history that follows that character, current attachment/position, and a reference to the owning server `.mechworld` file.
- Server/multiplayer `.mechworld` files are the authority for generated world data and mutable world state. The server must be able to run from its world file without needing any specific player slot to exist or be loaded.
- `.mechworld` files own generated hive-world definition data: world identity, seed/setup, sector names, zone names, zone history, faction epochs, facility/provenance/production/stock/loss/materialization/labor ledgers, and world-generation progress.
- Mutable world state belongs to the world authority. This includes current active-world state, NPC/object/light/noise/hazard/trap/population records, room factions, persistent faction membership, and player-faction continuity records. In single-player bundled slots this world authority is embedded as the slot's save-time world snapshot; in server/multiplayer it remains in the `.mechworld` file.
- Loading must prefer an embedded single-player snapshot when the slot declares that one is present. Loading a server/multiplayer character slot should merge mutable `world.*` state from the referenced `.mechworld` file when present, while retaining backward compatibility for older slots that carried mutable world state directly.
- Player-created factions are world entities. Player membership, reserved player slots, and player command ranks are world-state records keyed by stable player IDs; NPC command structures remain separate from player command structures.
- Persistence passes must catalog key counts, approximate payload size, namespace counts, embedded world-definition keys, embedded mutable-world keys, and an itemized namespace list before claiming a save-efficiency improvement.
- Shorthand key dictionaries may be introduced only as versioned schemas with migration tests. Do not silently replace readable keys with opaque aliases until load/save compatibility is verified.

- Server/multiplayer character slots must not become backdoor world saves. World-owned runtime namespaces such as `base.*`, `factions.*`, `npc.*`, `inn.*`, `bank.*`, `crime.*`, `scavenge.*`, `machine.*`, and `logistics.*` belong to the referenced `.mechworld` authority for server-style persistence. Single-player bundled slots may still contain these records as part of their save-time world snapshot.


## Gameplay quality-of-life default standard

- Gameplay comfort defaults should prevent common hostile startup and simulation friction: subtitles should be available by default, repeat splash waits should be skippable where supported, and audio volumes must remain separated by channel.
- Single-entity base-builder tools should prefer reasonable-distance ghost placement, hold-to-repeat construction, trap-prevention warnings, smart storage filters, and proxy crafting/counting from linked storage where the underlying system supports it.
- Production, logistics, and economy surfaces should expose blocker reasons, global scarcity warnings, market disruption alerts, local/global price hints, recipe pinning, and output-routing preferences rather than forcing blind chest hunts or unexplained stalls.
- Item-quality systems must not rely on identical icons alone. Favored-item protection, low-quality pickup warnings, no mixed-quality stacking, durability/quality badge preferences, and safe sorting rules should be owned by gameplay/QoL settings and consumed by inventory/logistics surfaces as they mature.
- Faction automation must respect survival and threat context: under-attack supply locks, safe worker priorities, and named death/incident alerts should be preferred over silent suicidal hauls or nameless failure notices.

## Anti-drift standard

If a change cannot name the owning phase, authority, durable rule, and verification path, stop and refocus before adding more code or documents.

- Player-created faction persistence must be owned by a world-side player faction ledger keyed by stable player ID. Character saves may remember the last known faction ID and resume rule, but leadership, reserved player slots, NPC command hierarchy, production continuity, trade continuity, and defense continuity belong to the world state.
- Player-command roles and NPC-command roles must remain separate tracks even when their rank names are equivalent. Player assignment UI and NPC management UI may share concepts, but they must not collapse into one ambiguous roster. Player and NPC command ranks must share a durable numeric command-tier scale: a rank-N player has rank-N NPC command authority. The founder remains a unique tier-0 player slot and must not be assignable to recruited players.
- Player-founded faction autonomy records are world state. Production continuity, trade continuity, defense posture, public/news continuity, NPC command continuity, and reserved player command slots must be persisted under the world/faction ledger and stripped from server/multiplayer character slots. Single-player bundled saves may carry those records only as part of their full save-time world snapshot.

- Player-founded faction autonomy ticks are world-owned state. Elapsed production, trade, defense, morale, stock, risk, public continuity notes, and reserved player IDs must be written to the world/faction ledger and stripped from server/multiplayer character slots. Single-player bundled saves may contain them only as part of the full save-time world snapshot.


## Display and graphics options ownership standard

- Display settings own monitor-facing choices: window mode, detected/safe resolution, text/font scale, GUI/chrome scale, hover help visibility, screensaver behavior, and applying window changes.
- Graphics settings own rendering choices: internal render downscale, target frame pacing, Java2D render-quality emphasis, art quality, map tile size, tile-art rendering, portrait rendering, reduced motion, render profile, color palette, color target, and brightness adjustment.
- Resolution dropdowns must be populated from runtime display-mode detection where possible, with safe windowed fallbacks for platforms that do not expose every OS mode through Java.
- Target FPS is best-effort Swing frame pacing only. Server-authoritative simulation, turns, long actions, and sector ticks must not derive game truth from render FPS.
- Refresh rates may be displayed when Java exposes them, but the client should prefer safe/current modes and avoid forcing unusual refresh behavior without an explicit selected mode.
- Visual lighting effects are graphics/render features only. Gameplay light for vision, combat, stealth, and targeting must remain turn-stable and must not be derived from frame flicker, bloom, or render-only animation.
- Deterministic flicker must be seeded per light source and synchronized with the render loop; do not use asynchronous random pixel animation for gameplay-relevant tile lighting.

## Diagnostics and JVM runtime profile standard

- F3/performance overlays are developer diagnostics. They may report FPS, paint time, memory, GC, render profile, lighting status, and server-lane status, but they must not become simulation authority.
- Render diagnostics must be drawn after the final Java2D surface composition so they remain readable regardless of internal downscale or CRT/render profile.
- JVM heap, garbage collector, string deduplication, and manual JVM override settings belong to a runtime profile authority and config file. Applying those flags to the active process requires restart; do not pretend a running JVM can change heap or collector mode in-place.
- Server/headless JVM profiles may prefer throughput and larger heap allocation, while client graphics profiles may prefer smooth frame pacing, but both must remain explicit saved profiles rather than hidden launcher magic.

## JVM options and restart-required UI standard

- JVM heap size, garbage collector selection, headless mode, and Java2D hardware-pipeline flags are restart-required settings. UI must display that requirement clearly before accepting changes.
- The JVM options surface must save selected profiles explicitly and offer a direct accept-and-restart path; it must not imply that the running JVM can change heap or collector mode in-place.
- Launcher/main-menu, graphical client, thin network client, and headless server are separate runtime profile targets of the same program. Server/headless profiles must not request graphical Java2D pipelines.
- OS-specific Java2D pipeline flags must be compiled conservatively by runtime profile and host OS. Windows graphical profiles may request Direct3D-related flags; Linux graphical profiles may request OpenGL; software-safe/server modes suppress those graphical acceleration requests.

## Accessibility and compatibility standard

- Accessibility compatibility options are render and input-presentation aids, not gameplay authority. Color-vision correction, high-contrast text, instant text, narration hooks, and screen-shake reduction must not alter server-authoritative simulation, combat math, turn logic, faction state, or gameplay lighting.
- Color-vision correction should be applied to the composited Java2D backbuffer when enabled so custom canvas rendering receives the same treatment as tile art, lighting, and UI chrome.
- Critical state must not rely on color alone. Danger, faction status, damage, locked choices, and target validity should eventually combine color with icon, border, text, number, shape, or texture indicators.
- Custom Swing/Java2D canvas surfaces should publish useful AccessibleContext names/descriptions when practical because ordinary screen readers cannot inspect text drawn directly through Graphics2D.
- Single-player combined JVM profiles must remain distinct from thin-client, client-graphics, launcher, and headless-server profiles. Combined profiles assume the local client and local authoritative runtime share one JVM process and may need larger heap allocation.

## Fallback profile management standard

- When no external wrapper/store profile is detected, the launcher profile route should expose a local fallback profile manager rather than relying on external profile links.
- Fallback profiles own local operator metadata and configuration migration only: profile ID/name, hardware signature, portrait index, display/accessibility anchors, options, JVM profile, and profile seed.
- Profile migration packages must not move character saves or world saves. Character and world persistence remain under the save/world authorities.
- Profile import must use staging, entry allow-listing, size limits, and atomic replacement where the host file system supports it.

## Project evaluation and replanning standard

- Structured project evaluation should compare durable design goals against implemented authorities, identify current gaps, rewrite goals by category, and produce an ordered engineering sequence with prerequisites and definitions of done.
- Replanning passes must update the four durable docs and may expose host/developer console summaries, but must not create standalone audit or planning documents unless explicitly requested.
- Gap-analysis output is developer-facing. It may be blunt and technical, but it must not leak into ordinary player-facing UI.
- Each replanning recommendation should name the owning authority or the authority that must be created before implementation begins.

## Name-locked profile portrait standard

- Name-locked special portraits are partitioned assets. They must not be added to the ordinary random player portrait pool or generic NPC portrait pool.
- A special profile portrait may be shown for character creation or the player only when both the active operator profile name and the in-game character name resolve to the same registered profile entry.
- Wrapper-detected profile names and fallback local profile names may seed the first character-creation candidate, but they must not silently unlock unrelated names.
- Special profile matches may grant profile-dossier flavor items or knowledge records, but they must be explicit, bounded, and saved as character-owned data rather than world authority.

## Name-locked profile portrait and noble-seeding standard

- Name-locked profile portraits are a separate portrait partition and must not enter ordinary/random character generation.
- A name-locked profile portrait unlock requires both the active launcher/local/wrapper profile name and the finalized character name to resolve to the same registered profile key.
- Matching profile unlocks must be visible to the player through a notification and a durable Infopedia/knowledge entry, not only through hidden inventory strings or logs.
- Rare world-seeded profile NPCs may appear in noble/governor contexts only when their registered key does not match the active operator profile key. The player should not encounter a duplicate noble version of the active profile they are currently playing.

## Blueprint construction and room-editor standard

- Player-built rooms and captured room templates must be represented as logical room objects: bounding box, metadata, relative tile array, anchor points, and object matrix. Blueprints must not store world-global coordinates as their internal layout identity.
- Room stamping must run preflight validation before placement. Obstructions, unmined wall/rock, deep liquid, non-buildable descriptors, and critical entities must produce explicit invalid-placement reasons instead of silently overwriting the world.
- Stamped construction previews must create collisionless ghost/hologram tasks first. Ghost plans may reserve intent and construction metadata, but they must not trap players or workers before real build tasks complete.
- Hollow-box, anchor snapping, resource estimate tooltips, material substitution prompts, and sandbox blueprint design are editor/construction authority features and should be consumed by UI/tooling rather than duplicated in unrelated menus.

## Progressive construction standard

- Construction blueprints and placed base structures should support staged construction where possible: a construction site may hold required materials, inserted materials, labor required, labor completed, and final build identity separately.
- Under-construction tiles should be visibly distinct from completed structures. The visual state should begin as pale blue ghost construction and fade toward the final built status color as material and labor progress approaches completion.
- Under-construction tiles should expose per-tile progress bars or equivalent compact progress indicators when visible on the map.
- Player or worker contribution to a construction site should be resumable over time. Missing components should block completion, not necessarily block initial placement of a valid construction site.
- Deconstruction, clearance, and terrain preparation for walls, doors, and obstructing interim structures must have a time cost. Equipped tools or tool-like weapons may reduce that time through the construction authority rather than bypassing it.
## Profile certification standard

- Fallback local profile creation/sync must require an explicit legal adult certification checkbox when no wrapper environment is providing profile authority.
- The certification state is stored as `profile.isLegalAdult` in the fallback profile properties file and exported/imported with profile migration packages.
- Profile validation must fail closed when the certification is absent or false; wrapper-provided profiles may seed certification only when the wrapper environment is actually detected.


## Chat and console input security standard

- Chat and console input must be hard-capped before processing, sanitized for control characters, line breaks, and tag-like markup, and logged without allowing user text to forge additional log lines.
- Chat display lines should show clean player-facing identity only; wrapper or fallback installation identifiers belong in local/server audit logs, not ordinary chat presentation.
- Client chat surfaces must rate-limit sends, suppress rapid identical resend spam, cap visible log buffers, and rotate local chat logs before unbounded growth.

## Secure chat E2EE standard

- Multiplayer chat payloads must be prepared as end-to-end encrypted packets before crossing a server relay boundary. The relay server may route and persist opaque packet metadata, but it must not receive plaintext, AES session keys, or private identity keys.
- Java 17 standard-library cryptography is the baseline: RSA identity keys are 3072-bit, AES payload keys are 256-bit, RSA wrapping uses OAEP with SHA-256/MGF1-SHA-256 parameters, and message payloads use `AES/GCM/NoPadding` with 12-byte IVs and 128-bit tags.
- AES-GCM IVs must be freshly generated per encrypted packet for a given AES key. Do not reuse an IV/key pair.
- Relay-visible metadata that affects sender, recipient, key identity, encrypted AES-key envelope, IV, or timestamp must be bound as AES-GCM additional authenticated data so tampering fails closed during decryption.
- Do not add external cryptography dependencies for this chat layer unless the user explicitly reopens the dependency policy.
- Local developer logs should record relay-safe packet summaries, not plaintext chat bodies, whenever the network-bound E2EE packet path is used.

## Legal gate and console command standard

- The fallback desktop runtime must fail closed behind the EULA consent gate until `settings/legal.properties` contains `eula_consented=true`.
- Refusing the EULA exits the application immediately; accepting it persists consent locally before normal boot/menu/game access continues.
- The client package must include `PACKAGE_client/EULA.md` beside its client-facing README material; any runtime EULA text update must be mirrored there, while legal notes stay out of unrelated gameplay UI.
- Fan-project intellectual-property disclaimers shown in the EULA gate must remain player-facing legal notice text, not scattered through unrelated gameplay UI.
- Gameplay/debug/server console commands must be registered through a named command authority with rank requirements, sanitized input, history bounds, and fail-closed handling for destructive or remote multiplayer operations that are not yet wired to a live server transport.
- Commands listed for future multiplayer enforcement may be registered and audited before networking opens, but they must not pretend to enforce remote bans, kicks, identity blocks, or database changes until the backing server authority exists.

## Multiplayer networking and host-binding standard

- Multiplayer host configuration must bridge through immutable `ServerConfig` records carrying world seed, world name/id, encoded world setup, difficulty summary, max player count, selected port, bind address, and selected protocol state.
- The project custom game port range is `25500-25599`. Do not bind well-known system ports `0-1023`, and do not use Steam query/service-adjacent ports `27015-27050` for the custom direct TCP host path.
- Host binding order is Steam wrapper/relay detection first, IPv6 direct binding second, and IPv4 direct binding last. Higher-layer transports may fail closed, but they must not be faked.
- Any local relay fallback must treat chat/session payloads as opaque encrypted packets and must not inspect or decrypt E2EE chat content.
- Headless server host sessions, socket channels, worker executors, and any future Netty EventLoopGroups must be closeable and registered with shutdown handling so Linux server processes do not leak runtime threads.

## Modding API and template standard

- Public modder-facing API seams belong under `src/mechanist/modapi/` and must remain narrower and more stable than editor internals.
- Example mod content belongs under `PACKAGE_client/modding/examples/` with a valid `manifest.json` and a complete Java entrypoint implementing `ModIntegrationHook`.
- Modding documentation belongs under `PACKAGE_client/modding/` rather than `ROOT_docs/` unless it is durable internal planning.
- Example manifests must keep `id`, Java `entrypoint`, and Java `modId()` aligned.
- Example mod hooks must validate callback targets and return without mutation when invoked for unrelated sectors, rooms, factions, items, knowledge nodes, or lore queries.
- Long-running mod export, validation, upload, and file scanning must remain outside the Swing Event Dispatch Thread.

## Network optimization and local host conversion standard

- Server world replication should prefer per-client delta packets over full snapshots. Each client needs its own acknowledged-sequence baseline because two clients may be missing different historical state.
- High-frequency replicated positions should be quantized before transmission. Floating-point state may remain authoritative inside the simulation, but the wire representation should use bounded fixed-point integers wherever practical.
- Client-side prediction may move the local player immediately for responsiveness, but authoritative server corrections must win. When the correction exceeds the configured threshold, the client must restore the server state and resimulate only still-unacknowledged inputs.
- Remote entities should render slightly in the past through interpolation buffers. Do not render remote packet arrivals directly when jitter smoothing is available.
- Opening a single-player world to local multiplayer is an irreversible runtime lock for that session. The player must acknowledge the warning before any server code starts, and host character data must be split from world data transactionally before continuous multiplayer ticks begin.
- Local multiplayer hosting must support a pre-world-data authentication gate. Password or generated host-key checks must occur before manifest, mod acquisition, or live world state disclosure.
- Java-only NAT/STUN helpers may discover and advertise WAN/LAN profiles, but they must not claim universal NAT traversal. If Steam relay or a real relay dependency is absent, direct hosting must honestly report fallback limitations.
- Emergency crash handling must prioritize authoritative character saves, network pause hooks, and crash dumps. It should avoid heavy work on Netty/native I/O threads and must use atomic file publication for emergency records where possible.
- systemd deployment assets must run under a non-privileged user, apply service sandboxing, and be explicit about Java standard-library limitations around inherited socket activation file descriptors.

## Render pacing and performance-audit standard

- The default client frame limiter should remain enabled unless a diagnostic/stress run explicitly disables it.
- Uncapped frame pacing is a profiling mode, not the normal player-facing default.
- Stress tests must be bounded and toggleable; they may flood render paths but must not mutate authoritative world state.
- New high-frequency render features should expose rolling telemetry through the shared frame metrics overlay before they are treated as performance-safe.
- Light/object rendering in experimental `doom` mode must distinguish wall-mounted light objects from floor-standing objects: lights on blocked wall tiles tint the wall column; lights on walkable tiles render as normal billboard objects.

## Windows direct-launch standard

Windows zip builds must include a diagnostic launcher that does not fail silently. The launcher must check that the jar exists beside it, search for a Java 17+ runtime through bundled runtime/JAVA_HOME/PATH routes, write a launch log under the user's local application data directory when available, and pause on startup failure so the user can see the error. Native installer work is secondary to first proving that the direct extracted-zip launch path opens the game.

### Windows Direct Launcher Java Version Rule

Windows direct-launch scripts must not blindly run the first `java.exe` found on PATH. Oracle Java 8 commonly registers itself under `Common Files\Oracle\Java\java8path`, which cannot run Java 17 class files. Launchers must verify Java major version before loading any game class, reject candidates below Java 17, and keep the console visible with a log path when no Java 17+ runtime is found.


## Windows Java probe rule

Windows launcher scripts must detect Java versions with explicit process stdout/stderr capture, such as `System.Diagnostics.ProcessStartInfo`, and must not rely on PowerShell `2>&1` under strict error mode for `java -version`. Java writes version text to stderr, so strict PowerShell redirection can incorrectly classify valid Java 17+ runtimes as unreadable. Launcher logs must be written as UTF-8 for readable user support uploads.

### Native graphical dependency bootstrap

Desktop graphical dependencies that are treated as required by client rendering paths must be either bundled in `lib/` or bootstrapped into `lib/` before startup preflight. A Maven/POM declaration alone is not runtime availability. If a required native graphics library cannot be found or installed, the client must fail loudly with a visible log instead of silently continuing in a misleading partial state. Headless server launch paths must not require desktop graphics libraries.



## Character knowledge/progression authority standard

- Knowledge purchases belong to the character-owned Knowledge Tree / progression surface, not the Infopedia reference ledger.
- The Infopedia may describe doctrine, recipes, construction permissions, and lore, but it must not be the primary spend-points workflow once the Knowledge Tree menu is available.
- Knowledge credits and unlocked knowledge IDs remain character-owned persisted state. UI tree branches must read from and commit back to that authority rather than inventing parallel progression storage.
- Debug/admin commands that add or set knowledge credits, unlock knowledge, or lock knowledge must mutate the same character-owned knowledge-credit and unlocked-knowledge state used by the Knowledge Tree, then refresh any open Knowledge Tree menu. They must not route through the Infopedia reference ledger.
- Knowledge nodes may carry later payload hooks for recipe grants, construction permissions, character perks, services, and dialogue gates, but the base node/prerequisite/point transaction must compile and test independently of those payloads.
- Registered active knowledge definitions must be placed in at least one active in-game branch or explicitly marked hidden/deprecated by the branch-composition authority. Knowledge branch composition changes must run an audit that checks placement, prerequisite existence, branch emptiness, and branch-local cycles. Cross-listed nodes are allowed only when they intentionally share the same character-owned unlock ID across multiple branches.

## Specialized tile-set authority standard

- Tile folder architecture is semantic authority. Sewer corridors, exterior maintenance void corridors, noble corridors, noble floors, and noble walls must resolve through their own imported folders/cells rather than generic floor/corridor/wall fallback aliases.
- Room-owned walls are determined by the room/faction context on the playable side of the wall. Interwall/hidden hive mesh may keep local random variation, but it must not override noble, sewer, or other explicit room-wall families.
- Sewer corridor and sewer intersection glyphs must use sewer-specific corridor art. Exterior maintenance corridors must use the imported exterior/void maintenance corridor column family.

### Java 17 release-classfile gate

Any packaged client or server jar that claims Java 17 compatibility must be compiled with `javac --release 17` or an equivalent toolchain target. Before shipping an archive, scan the jar classfiles and verify no class has a major version above 61. A launcher smoke passing under a newer local JDK is not sufficient proof that the package will run on a user's Java 17 installation.

The repository includes Java 17 classfile release-gate helpers under `ROOT_tools/build/` when that tooling is present, with older equivalents possibly still under `scripts/` during migration. These helpers are not optional decoration: when packaging through a manual/non-Maven path, run the available verifier against `TheMechanist.jar`, `TheMechanistServer.jar`, and any loose class output that will ship. If the helper reports any classfile above major 61, stop the release and rebuild with the correct target before producing the zip.

# The Mechanist — Standards and Practices

TOP-LINE MERMAID POSITION COMMAND: Every code module, generated code error, compile error cluster, and subsystem remap must submit a position in `ROOT_docs/functionmap/Mermaid_Code_Map_Master.md` / `CODE_MERMAID_POSITION_LEDGER.tsv` before it is considered mapped, repaired, or complete. Unpositioned modules and errors are architecture debt.

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

## Mermaid code-position mapping standard

- The active codewide Mermaid map is `ROOT_docs/functionmap/Mermaid_Code_Map_Master.md`.
- Regenerate the code-position ledgers with `py -3 scripts\BUILD_MERMAID_CODE_MAP.py --apply` whenever Java modules are added, deleted, renamed, moved, or substantially remapped.
- Every new Java module must either match an existing Mermaid zone or add a deliberate new zone to the builder. Silent unpositioned modules are not allowed.
- Every compile error cluster must be reported against a Mermaid node or zone before repair is considered complete.
- Every subsystem remap must name the old position, the new target position, and whether the old position is retired, bridged, or still active.
- `CODE_MERMAID_EVALUATION.tsv` is the gate surface for unpositioned modules and oversized mapped modules. `ERROR` rows require action before a remap is considered complete; `WARN` rows require an ownership decision before structural splitting.

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

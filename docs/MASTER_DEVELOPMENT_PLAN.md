# The Mechanist — Master Development Plan

This document is the durable roadmap. It is not a changelog and it is not a dumping ground for the most recent pass. The first thing a future development session should see is the durable phase structure, not a stack of version notes.

Development must follow this containment rule: planning belongs here, completed work belongs in `DEVELOPMENT_HISTORY.md`, durable implementation rules belong in `STANDARDS_AND_PRACTICES.md`, and high-level doctrine belongs in `MASTER_GOVERNANCE_REVISION_II.md`. No pass may add another standalone Markdown file to `docs/` unless the user explicitly orders it.

## Durable phase roadmap

### Phase 0 — Governance, documentation, and build hygiene

Goal: keep the project navigable and prevent disconnected pseudo-progress.

Required outcomes:

- `docs/` contains exactly four durable development documents: this plan, standards and practices, development history, and master governance revision.
- README remains user-facing and does not accumulate changelogs, roadmaps, standards, or asset-index commentary.
- Every pass updates only the durable file that actually owns the information.
- Source files use semantic authority names and compact behavior comments, not phase-numbered roadmap prose.
- Player-facing strings describe current behavior only and avoid placeholder / temporary / coming-soon language.
- Asset indexes remain machine-readable data under `assets/indexes/` or a tool module only when consumed by code, tools, or packaging.
- Java 17 compile, jar rebuild, and zip packaging remain required before delivery.

Exit criteria: docs remain consolidated, source stays free of planning sprawl, the package builds, and the next target is identifiable from this plan.

### Phase 1 — Framework stabilization and code dissection

Goal: reduce the oversized central Java surface into durable authority classes without changing gameplay behavior by accident.

Status: the main extraction gates are complete enough to proceed. `TheMechanist.java` still contains launcher, panel wiring, selected-context bridges, and legacy glue, but new work must name the owning authority it extends.

Required outcomes:

- Continue extracting touched subsystems into focused authorities and APIs.
- Preserve save/load behavior while extracting.
- Route repeated behavior through shared authorities rather than new local helpers.
- Maintain old-hardware performance.
- Prevent the central class from regrowing into a storage site for unrelated domain code.

Primary authority targets remain construction, UniversalWindow/UI, machine operations, staffing, logistics, faction/room ledgers, combat/health, hazards, persistence, editor, and localization.

### Phase 2 — Placeholder and asset integration discipline

Goal: convert indexed art from inert asset mass into intentionally promoted gameplay families.

Active sub-plan: follow `docs/STAGED_ASSET_INTEGRATION_PLAN.md`. Stage 2, the in-game Infopedia Semantic Asset Browser, is complete. The immediate next target is Stage 3, high-error asset category indexing and semantic reconciliation. Do not skip ahead to wholesale render-path replacement; registry, Infopedia browsing, high-error indexing, and preview migration must come first.

Required outcomes:

- Preserve low_32 as the core runtime art tier.
- Keep standard/intermediate/high art as optional packs.
- Promote assets only through narrow semantic buckets.
- Treat placeholder art handles as candidates, not gameplay implementation.
- Use semantic runtime handles for promoted objects, fixtures, items, portraits, roads, walls, floors, and entities.
- Maintain folder/category authority for portraits, faction art, creatures, pets, and item icons.
- Keep generated machine indexes under `assets/indexes/` when consumed by code/tools, while using the staged asset integration plan as the human migration checklist.
- Route new graphical references toward exact 8-character semantic asset IDs and expose player-facing assets through the in-game Infopedia asset audit surface as stages mature.

Exit criteria: art becomes usable through semantic runtime authority instead of broad filename scans or scrap-icon fallbacks, high-error assets have semantic IDs, and direct path fallback is audited rather than silently hiding wrong art.

### Phase 3 — Room, road, frontage, plaza, alley, parking, and spatial integration — COMPLETE

Goal: make generated spaces physically connect, face one another, and produce believable civic, industrial, commercial, residential, sewer, and exterior-boundary layouts.

Completion state: closed as of 0.9.10fz for durable-phase scheduling. Phase 3 established the required spatial authority foundations: roaded and roadless zone families, descriptor-backed tile identity, road/sidewalk distinction, exterior maintenance boundary layers, world-generation weight semantics, and inspection surfaces sufficient to expose spatial defects. Remaining deep world-generation expansion is not reopened here; it belongs to later durable world/facility phases, especially Phase 8 and Phase 13, unless a Phase 4 presentation/input defect requires a narrow repair.

Completed outcomes:

- Maintained explicit roaded vs roadless zone families.
- Preserved road/sidewalk distinction: sidewalks are street-family tiles, not road-lane connectivity.
- Separated roads, walls, floors, corridors, fixtures, overlays, and semantic markers through compiled tile descriptors.
- Protected edge safety bands, transition anchors, exterior maintenance corridors, bulkheads, and void layers.
- Established enough local inspection and descriptor reporting to identify spatial failures without continuing to patch blind.
- Closed the Phase 3.6 tooling detour as an active target; editor/mod tooling now returns to Phase 15 unless explicitly reopened.

Exit criteria: met for phase scheduling. Generated zones are now descriptor-backed, road/sidewalk-aware, exterior-boundary-aware, and inspectable enough to move into client presentation and input containment.

### Phase 4 — UI, input, rendering, and presentation containment — ACTIVE

Goal: make the client playable and legible on older machines and scaled desktop environments.

Required outcomes:

- Keep Zone Audit generation-trace visibility active while repairing worldgen presentation defects: current generation step, rejected placement ghosts, and phase ordering must remain inspectable during Phase 4 UI/presentation work.
- Keep all menu text, buttons, captions, dropdowns, and panels bounded to their owning frames.
- Treat captions as compact labels, not body text.
- Use frame wrapping and clipping for body copy.
- Use the virtual render layout for mouse-to-world targeting and never hardcode tile offsets.
- Keep keyboard, mouse, and gamepad reticles under the same targeting model.
- Preserve borderless-windowed default launch and safe runtime window-mode switching.
- Maintain narrated intro timing with tail safety.

Exit criteria: the UI does not spill outside frames, target coordinates match what is rendered, and presentation features do not mask gameplay information.

### Phase 5 — Operations, logistics, hauling, and reservations

Goal: make manual and eventual automated work flow through shared operational structures.

Required outcomes:

- Use shared operation records for queued, active, paused, blocked, failed, completed, and abandoned work.
- Keep manual hauling, source reservation, route readiness, delivery intent, and contract lifecycle inside named authorities.
- Avoid hidden route reservations or autonomous logistics until the manual workflow is stable.
- Preserve clear inspection/preflight feedback for why a task can or cannot proceed.

Exit criteria: player-directed work can be explained, reserved, executed, interrupted, and audited.

### Phase 6 — Production, machines, recipes, staffing, and operation queues

Goal: make machines and recipes operational instead of decorative.

Required outcomes:

- Machines expose required inputs, tools, staffing, duration, power/utility needs, outputs, status, and interruption rules.
- If no assigned worker is available, eligible machines may fall back to manual player operation with explicit time cost.
- Production status must be inspectable and must not depend on hidden local timers.
- Recipe/build requirements should use meaningful industrial components rather than universal supplies.

Exit criteria: production can be planned, staffed, operated, interrupted, and explained.

### Phase 7 — Containers, permissions, access checks, and ownership

Goal: make inventory, storage, machines, and faction spaces respect ownership and access.

Required outcomes:

- Add actor access checks for containers and production.
- Distinguish player-owned, faction-owned, public, restricted, stolen, locked, and forbidden access.
- Route access denial through inspectable messages and faction consequences.
- Preserve item provenance and equipment transfer authority.

Exit criteria: actors cannot freely use every container or machine without a governing access model.

### Phase 8 — Provenance, item origin, facilities, and supply ecology

Goal: explain why goods exist and where they came from.

Required outcomes:

- Food, water, fuel, medicine, weapons, ammunition, construction materials, and trade goods have plausible sources.
- Facility families include food production, hydroponics, water recyclers, waste treatment, generators, forges, medicae clinics, labs, precincts, markets, temples, depots, sewer utilities, and noble service layers.
- Generation gains progress/audit surfaces for staged facility and provenance creation.

Exit criteria: the world can explain item origin and supply pressure.

### Phase 9 — Construction, bases, rooms, defenses, and ownership

Goal: make player and faction spaces physically meaningful.

Required outcomes:

- Continue construction validation, no-self-entombment, placement feedback, and validation inspection.
- Expand base ownership and room metadata.
- Promote passive defenses into physical map objects before active targeting.
- Integrate doors, gates, barricades, alarms, cameras, traps, walls, defensive furniture, and checkpoints.

Exit criteria: buildings and rooms govern access, cover, raids, ownership, and later combat.

### Phase 10 — Hazards, lighting, darkness, and traps

Goal: make dangerous spaces readable and mechanically consistent.

Required outcomes:

- Support gas, fumes, sludge, shorted wires, jagged passages, overheating factories, freezer spaces, traps, sewer hazards, and industrial kill zones.
- Add readable warning overlays where appropriate.
- Integrate hazards with lighting, visibility, sound, pathing, health, equipment, and generation.

Exit criteria: a player can see danger, understand danger, and trace the system that produced it.

### Phase 11 — Combat, health, unconsciousness, death, saves, and feedback

Goal: make violence and failure robust enough for a survival sim.

Required outcomes:

- Review combat, combat feedback, health, unconsciousness, armor, cover, weapons, ammunition, and death.
- Unconscious players move to their base if possible, otherwise the nearest medical facility.
- Death opens a You Lost screen with cause, weapon, killer, survival length, score, kills, crafted items, knowledge, sectors, zones, NPCs, wealth, bases, recruits, and weighted values.
- Load Save opens the save/load menu.
- Save UI uses four manual slots plus a separate autosave column.
- Autosaves occur on zone transition or every in-game hour.

Exit criteria: injury, defeat, recovery, save/load, and final loss are coherent and testable.

### Phase 12 — Character creation, names, ranks, rosters, and social identity

Goal: stop treating characters and factions as thin placeholders.

Required outcomes:

- Implement deterministic stat distribution and editable character-name entry.
- Expand Imperial-style first, last, and singular random name pools.
- Give every faction ranks, leaders, deputies, supervisors, workers, guards, priests, representatives, and specialists.
- Tie ranks to facilities, rooms, production authority, quests, and command chains.
- Player portraits use baseline/base human pools only; faction and creature portraits use exact folder authority.

Exit criteria: factions can produce named people with roles, ranks, continuity, and command meaning.

### Phase 13 — World generation, districts, sewers, temples, estates, and strategic simulation

Goal: make the world a layered industrial city instead of a collection of local rooms.

Required outcomes:

- Expand district stamps and zone families: hab blocks, apartments, markets, industrial sectors, noble estates, sewers, precincts, medicae zones, temples, military areas, utilities, waste plants, cargo yards, and faction compounds.
- Integrate Adeptus Ministorum / Ecclesiarchy temples as neutral civilian Imperial anchors near plazas.
- Temples include church rooms, pillars, relics, prayer nooks, candle racks, saint alcoves, donation boxes, supplicant kitchens, priests, pilgrims, Sisters of Battle guards, and immortal/non-targetable head clerics.
- The head cleric offers a 24-hour Imperial forgiveness prayer service costing hunger/sleep and restoring limited standing with civil Imperial factions.
- Expand distant-zone simulation through operational ledgers, not full tile simulation.

Exit criteria: local zones, offscreen districts, factions, and continuity anchors form one scalable world model.

### Phase 14 — Economy, quests, contracts, and faction reputation

Goal: convert faction presence into work, risk, trade, and standing.

Required outcomes:

- Faction representatives offer bounded fetch-item and bounty contracts.
- Contracts identify item, target, source, destination, reward, expiry, reputation effect, and failure state.
- Economy layers use abstraction at distance and detail only where relevant.

Exit criteria: quests and economy produce work tied to faction facilities and item provenance.

### Phase 15 — Editor, localization, modding, and content pipeline

Goal: make content creation durable instead of hand-editing fragile code.

Required outcomes:

- Develop an in-game editor for zone stamps, features, entities, factions, machinery, recipes, items, and tiles.
- Use the Mods / Tools route as the local tooling entry point before claiming live external mod loading.
- Move player-facing text to keyed localization files.
- Keep maximum interoperability and customizability as the editor goal.
- Future stamps and content should consume the same definitions used by runtime systems.

Exit criteria: content can be added through data/editor surfaces instead of central-code sprawl.

### Phase 16 — Polish, packaging, QA, and endgame readiness

Goal: turn the simulation into a coherent playable loop.

Required outcomes:

- Package the core archive leanly with low_32 assets and optional art/audio packs.
- Maintain Java 17 compatibility and old-machine viability.
- Add regression checklists for save/load, worldgen, pathing, construction, combat, logistics, staffing, UI, and asset fallback.
- Preserve a root new-conversation briefing document for continuity.

Exit criteria: development can proceed phase by phase without losing context, duplicating authorities, or inventing fake implementation.

## Current active implementation gate — Phase 4 with temporary server-authority override now bridged into single-player

The current active target remains Phase 4: UI, input, rendering, and presentation containment. The user explicitly extended the launcher/client/server override through 0.9.10gp: 0.9.10gf established the server-authoritative sector simulation boundary, 0.9.10gg migrated the desktop single-player turn loop onto that sector manager by default, 0.9.10gj moved the main committed single-player world-mutation lane behind an internal-server single-writer `AuthoritativeWorldRuntime`, 0.9.10gk hardened explicit turn modes plus server-side long-action gating/progress, and 0.9.10gl returned to phases C-F by publishing immutable `WorldSnapshot` records and routing console/admin mutations through explicit request/command records. The local player is sector-bound, turn/sleep/deferred-maintenance/paced-movement commits route through the authoritative bridge, converted actions submit named command requests, Wait can advance a gated long action, and Swing records rendered snapshot versions instead of owning truth. Phase 3 and the Phase 3.6 tooling detour are closed for scheduling. The Mods / Tools and Sector Editor Audit line is preserved as a future editor/modding thread under Phase 15, not the active development target.

Phase 4 currently allows:

- Project-wide frame containment repair for menus, panels, captions, body text, dropdowns, command strips, and transition copy.
- Input correctness work: mouse-to-world transforms, zoom/borderless layout transforms, reticles, keyboard/gamepad targeting, and panel-scroll conflicts.
- Rendering and presentation containment: text clipping/wrapping, map frame ownership, tile descriptor display correctness, intro timing safety, and borderless-windowed behavior.
- UI authority extraction when a touched surface exposes a clean seam.
- Documentation cleanup only inside the four durable documents.

Phase 4 does not allow as an active target:

- Expansion of Mods / Tools, Sector Editor Audit, or in-game editor behavior.
- Live external mod loading, source acquisition, classpath mutation, hot restart, or public multiplayer networking. Authoritative sector ticking and the single-player authoritative world lane may continue only as the bounded server/client seam explicitly extended through 0.9.10gp unless the user extends that server track again.
- Broad new world-generation content beyond a narrow repair needed to stop a Phase 4 rendering/input defect.
- New standalone Markdown notes.

The near-term Phase 4 sequence is:

1. Keep the four-document rule intact.
2. Audit the main menu, options, gameplay HUD, tactical slate, recent actions, dossier, inventory, build, look/auspex, transition, and modal surfaces for frame containment.
3. Unify mouse, keyboard, and future gamepad reticles through the shared targeting model.
4. Keep render-layout transforms authoritative for every map-targeting action; gameplay map draw and gameplay mouse hit-testing now begin from the same viewport transform authority.
5. Continue code dissection only when touching the owning UI/input/rendering subsystem.

Phase 4 bucket list, in priority order:

1. **Frame ownership and command containment.** Every button and label must belong to a real frame. Main-game, interaction, combat, build, workbench, trade, and scavenge command buttons must be corralled by semantic ownership, not by whichever old rectangle they happened to intersect.
2. **Text surfaces and wrapping.** Recent Actions, Tactical Slate, Look/Auspex, Interact, Combat, Build, Character Dossier, Survival, Inventory, Map/Intel, Options, first-impression, transition, capture, and loss surfaces must wrap and clip inside their child rectangles. Captions remain compact labels. 0.9.10gc begins this bucket by moving high-churn panels to cached pixel-width text layout. 0.9.10gd extends the efficiency rule to high-frequency map/entity image scaling through a bounded render image scale cache. 0.9.10ge extends that cache to repeated UI chrome frame rasters and adds cache hit/miss accounting.
3. **Reticle and targeting convergence.** Movement preview, Look, Interact, Combat, Build placement, mouse targeting, keyboard reticle movement, and future gamepad targeting must share the same viewport/reticle model.
4. **Presentation and z-order.** Dropdowns, hover help, timed alerts, popup frames, command panels, map overlays, text capsules, tile icons, NPCs, and cursor rectangles must draw in intentional layers without hiding the information they are meant to expose.
5. **Input focus and scroll conflict repair.** Keyboard/gamepad reticle movement must not be swallowed by generic panel scrolling; scroll regions must identify ownership and remain bounded.
6. **Verification bucket.** Each Phase 4 pass should include at least one targeted UI/input/render smoke test for the surfaces it touched, plus Java 17 compile, jar rebuild, and zip integrity.

## Current do-next sequence

1. Maintain Phase 0 governance and build hygiene on every pass.
2. Treat the 0.9.10gf/0.9.10gg/0.9.10gj/0.9.10gk/0.9.10gm/0.9.10gp server-authority/persistence override as bridged: the sector manager exists, single-player turns route through it by default, the main committed world-mutation body passes through the internal-server single-writer lane, the first player-action surfaces enter as `WorldCommandRequest` objects, immutable `WorldSnapshot` records are published after commits, console input enters as `ConsoleCommandRequest`, long actions gate input with server-side progress/completion command handling, clock/countdown overlays derive from the action registry, server/single-player save namespaces are physically separated, and new save slots reference `.mechworld` files instead of embedding duplicated `worlddef.*` or mutable `world.*` state. Return to Phase 4 unless the user explicitly extends the server track again.
3. Continue Phase 4 through the bucket list: frame ownership, text wrapping, reticle convergence, presentation z-order, input focus, and verification.
4. Do not expand Mods / Tools or Sector Editor Audit until Phase 15 or an explicit user override.
5. Preserve 0.9.04b character creation / Imperial name generation as the later identity-system pivot when Phase 12 is reached or explicitly directed.
6. Preserve 0.9.05 actor access / permission checks for containers and production as the later access-control target when Phase 7 is reached or explicitly directed.
7. Move toward combat, health, unconsciousness, death, and save-flow only when Phase 11 is reached or explicitly directed.

## Anti-drift rule

When uncertain, do not invent a new phase, new document, or new subsystem. Consult this plan, the standards, governance, and history. Then make the smallest bounded implementation that advances the current phase and compiles.

### Server-authority continuation through 0.9.10gp

The user explicitly reopened the launcher/client/server override and directed the work back to phases C, D, E, and F of the prior architecture plan. Current state: the desktop client submits named player/admin requests, the internal server lane owns committed mutation, the authoritative lane publishes immutable world snapshots for client/render/network-facing use, strict and slow-continuous turn modes are explicit, long actions gate input through the server action registry, and the admin console now submits explicit console requests before dispatching named mutation commands. The next safe server-authority targets are phase G/H/I work: sector threading preparation without parallel mutation of shared legacy `World`, lifecycle/death/respawn migration behind server authority, and broader command rejection/diagnostic coverage. True per-sector parallel world mutation remains deferred until shared state is split or isolated. 0.9.10gm adds the next G/H/I-adjacent server-authority foothold: server-derived player countdown overlays, server-world lifecycle respawn resolution, a separate headless server executable/status initializer, and separated `saves/singleplayer/` versus `saves/server/` namespaces. 0.9.10gn begins the persistence-efficiency pass by cataloging slot/world-definition payloads and removing duplicated generated-world ledgers from new `.mechsave` writes. 0.9.10go adds the itemized namespace list needed before deeper pruning: slot namespaces, world-definition namespace, key counts, approximate payload weights, example keys, and ownership/review notes. 0.9.10gp then splits mutable `world.*` state out of the character slot and into the `.mechworld` runtime world file, with load-time merge support and initial player-faction membership continuity records keyed by stable player IDs.


### Persistence architecture line — character slots and world-state files

The active persistence direction is now character-slot plus world-state separation. `.mechsave` files should become lightweight character attachments: identity, player ID, inventory/equipment/account state, knowledge/history, and the pointer into a world file. `.mechworld` files should own generated world ledgers plus mutable runtime world state. Later passes should continue extracting long-term faction/property/production state out of the character slot and into world/faction ledgers, then consider versioned shorthand key dictionaries after compatibility tests exist.

### Persistence continuation after 0.9.10gr

The save architecture now has three active tracks: bundled single-player snapshots, server/multiplayer character attachments, and authoritative `.mechworld` runtime state. Next persistence work should split item/container ownership so player-owned inventory instances can remain with the character while placed/world-owned item containers remain in the world file. Only after that split should shorthand key dictionaries or compressed aliases be introduced.

0.9.10gs extends the persistence continuation line with a formal player-faction world ledger. Player-created factions are now treated as world entities with stable reserved player membership records and separate player/NPC command tracks. The next valid persistence targets are: expanding the player assignment menu surface, making NPC autonomous faction plans consume the world ledger when the founding player is absent, and only then considering versioned shorthand key dictionaries for large text-heavy namespaces.

### Persistence continuation after 0.9.10gu

The player-faction ledger now has a first autonomous world-state layer. Player-founded factions can persist production/trade/defense intent in the world file even when the founding player is absent, and player assignment inspection is separate from NPC management. Next valid persistence targets are: converting more base production/storage/item-container ownership into world-ledger records, adding a real player-faction assignment UI over the existing console/developer inspection path, and then introducing versioned shorthand/compressed persistence schemas only after migration tests cover readable-key compatibility.

- 0.9.10gu added compact world-owned autonomous tick output for player-founded factions. Next persistence work should decide how stored autonomous tick ledgers are merged from disk into live world runtime during server load, then expose a player-facing faction assignment/role menu separate from NPC management.


### Current faction/personnel parity line

- Maintain separate player-command and NPC-command rosters while using a shared command-tier scale.
- Founder authority remains a unique player tier above recruited-player ranks.
- Recruited players may receive tiers 1-5; each tier carries equivalent NPC command authority for the same tier and lower.
- Future personnel-management UI should expose player assignments and NPC assignments as separate modules inside the same contextual management framework, not as one ambiguous roster.


### 0.9.10gw display-density containment

The UI/input/rendering line now includes a display-density authority for the pure Java 17 Swing client. The client configures native Java2D scale and text antialiasing before Swing startup, scales standard Swing fonts via Look-and-Feel defaults, and applies shared tiny-text rendering hints in custom panels. Further UI density work should extend this authority instead of manually shrinking isolated panels.

### 0.9.10gx display/graphics continuation

Display and Graphics options are now split by responsibility. Display owns monitor and density decisions; Graphics owns render cost, frame pacing, art quality, motion, and palette behavior. Future display work should refine runtime display-mode selection, borderless/exclusive fullscreen stability, and per-monitor behavior. Future graphics work should tune the render-quality profiles and reduced-motion behavior before attempting deeper Java2D/BufferStrategy rendering changes.


### 0.9.10gy visual lighting continuation

The render-only lighting line now separates gameplay light values from visual light treatment. Next lighting work should keep that separation: tile safety, vision, combat, and stealth read turn-stable gameplay light; flicker, bloom, color wash, and environmental mood belong to render snapshots and graphics options.


Phase 4 graphics/runtime follow-up now includes the 0.9.10gz diagnostics/runtime profile line: keep F3 diagnostics as a final-surface developer overlay, keep JVM profile settings explicit and restart-bound, and keep all render-loop metrics separate from server-authoritative simulation truth.

Phase 4 options/presentation follow-up now includes the 0.9.10hj gameplay-quality defaults line: retain a dedicated QOL options surface for hostile-default prevention, and make future inventory, logistics, construction, production, faction, market, and alert systems consume those preferences rather than adding duplicated hard-coded toggles.

### 0.9.10ha JVM/runtime options continuation

The Options surface now separates JVM/runtime profile controls from Display and Graphics. JVM options are restart-bound and must continue to present that fact clearly. Future runtime work should keep launcher/main-menu, graphical client, thin network client, and headless server profiles as related but separate targets. Do not imply the multiplayer server GUI exists yet; the server remains headless/status-initializer until the network/server layer is explicitly reopened and examined.

### 0.9.10hb compatibility/runtime continuation

The JVM/runtime line now includes single-player combined and single-player combined heavy presets for the embedded local-host architecture. Future JVM UI work should keep profile purposes explicit and avoid implying that headless server networking is complete. The accessibility line now has first-pass render-side color-vision correction and custom-canvas narration hooks; future UI work should replace color-only state cues with dual-channel indicators and continue routing accessibility controls through named authorities rather than isolated menu code.


### 0.9.10hc fallback profile continuation

Fallback profile management is now the local recovery surface when no wrapper/store profile exists. Later profile work should connect this surface to the universal management-window architecture, profile-specific keybind sets, and profile-scoped audio/accessibility presets without mixing it with character/world save migration.


### 0.9.10hd project re-evaluation line

A structured re-evaluation pass is now part of the developer authority surface. The current replanned sequence is: continue command-request conversion before per-sector parallelism; build tier-aware faction/personnel assignment authority; migrate one real management screen into the UniversalWindow wrapper; perform a second persistence ownership audit before shorthand schemas; validate fallback/JVM/profile migration; add non-color render/accessibility indicators for critical states; and keep networking closed until command, snapshot, session, and save boundaries are smokeable. The main hidden dependency remains legacy UI/helper code that still assumes direct live `World` availability.

### 0.9.10jn editor/UI cleanup continuation gate

The user explicitly reopened the in-game editor/UI ownership line after rejecting detached Swing editor/progression windows. The immediate editor direction is now to reincorporate editor surfaces into the main game client surface, beginning with a game-owned Room Editor surface that supports contained palettes, universal NEW entry creation, model-first grid placement/erasure, undo/redo, and helper visibility controls. External `JFrame`/`JDialog` editor launch paths must remain closed from player-facing menus.

Next scheduled review after this archive: perform a dedicated source audit for legacy detached-window paths, stale editor launchers, external-dialog stubs, render-profile/CRT player controls, and any remaining player-facing code that bypasses the main game UI surface. The audit should either remove those roots outright or mark them as development-only until they are converted to owned in-game screens. Do not expand new editor features broadly until that cleanup audit is complete.

### 0.9.10jr knowledge composition continuation gate

The Knowledge Tree is now structurally composed into eight major in-game branches with broad quality gates, cross-branch prerequisites, and a build/smoke audit that requires every registered active knowledge to be placed and reachable by definition. The next knowledge pass should not rework the branch map unless testing shows a specific readability or pacing defect. Instead, future work should attach gameplay payloads to nodes in small slices: recipe unlocks, construction unlocks, equipment permissions, faction services, character perks, dialogue gates, and passive modifiers. The point economy should be reviewed against actual XP/contract pacing after several play sessions rather than only from total full-tree cost.


### Current asset migration stage

The staged semantic asset migration is currently complete through Stage 3. Stage 4 is next: migrate player-visible UI previews and item/equipment icons to resolve by semantic asset ID first, using `assets/indexes/high_error_asset_reconciliation.tsv` for the known bad-assignment families. Do not migrate high-frequency world rendering until Stage 5.


### Asset Migration Program Update — 0.9.10jw

Stage 4 is complete: item and UI preview resolution now has a registry-first semantic bridge for high-error carried items. The next asset migration stage should begin tile descriptor, road, wall, corridor, transition, and Zone Audit preview migration through semantic asset IDs while keeping generation logic separate from art identity.
### Asset Migration Stage 5 completion — 0.9.10jx

Tile descriptors, Zone Audit reporting, and Tile Infopedia details now carry Semantic Asset Registry IDs for mapped tile art aliases. The renderer is registry-first for compiled tile descriptor art but still retains legacy alias/glyph fallback during migration. Next asset stage is Stage 6: object, machine, fixture, and construction/editor palette migration.


- Asset migration next step: Stage 7 — direct path reference audit and enforcement. Stage 6 is complete as 0.9.10jy: construction recipes, base objects, map fixtures, lights, traps, and in-game editor palettes now resolve semantic asset IDs through `ObjectSemanticAssetAuthority` before legacy art fallback.


### Asset migration continuation — 0.9.10jz

Stage 7 direct path-reference audit/enforcement is complete. `SemanticAssetPathAudit` plus `SemanticAssetDirectPathAuditSmoke` now guard the project against new unbaselined direct graphical path calls outside approved low-level loader/import/migration surfaces. The next asset migration stage is Stage 8: portrait, faction, creature, and entity-art partitioning.


### Asset migration status update — 0.9.10ka

Stage 8 portrait/entity partitioning has been completed. The next asset stage is Stage 9: retire legacy fallback aliases for migrated families and harden the registry so covered categories cannot silently fall back to semantically wrong art.
### Asset migration status update — 0.9.10kb

- Stage 9 is complete: migrated graphical families now have typed missing-art fallback behavior and a hardening smoke gate. The remaining legacy graphical paths are explicitly deferred in `assets/indexes/semantic_asset_deferred_legacy.tsv` with owner/reason/next action.
- Next asset-program target: Stage 10 — mod and art-pack semantic registry extension. Define external registry declaration format, namespace/conflict rules, override/replacement policy, and acquisition-time validation before world/session initialization.

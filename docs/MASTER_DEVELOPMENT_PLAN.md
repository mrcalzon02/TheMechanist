# The Mechanist - Master Development Plan

This document is the durable roadmap and checkpoint timeline. It is not a changelog and it is not a dumping ground for continuation notes. Future development sessions should first see the phase structure, the active checkpoint, the current publish-safe gates, and the next required runtime pass.

Development must follow this containment rule: planning belongs here, completed work belongs in `DEVELOPMENT_HISTORY.md`, durable implementation rules belong in `STANDARDS_AND_PRACTICES.md`, and high-level doctrine belongs in `MASTER_GOVERNANCE_REVISION_II.md`. No pass may add another standalone Markdown file to `docs/` unless the user explicitly orders it.

## Active checkpoint

Current checkpoint: **Phase 4 publish-safe client containment**, with the prior server-authority and persistence overrides treated as bridged unless the user explicitly reopens them.

The active target is to make the Swing client playable, readable, and safe to publish while preserving the authoritative world lane already introduced through the 0.9.10gf-0.9.10gp work. The sector manager exists, single-player turns route through it by default, committed world mutation passes through the internal-server single-writer lane, immutable `WorldSnapshot` records publish after commits, server/single-player save namespaces are separated, and new save slots point at `.mechworld` runtime files.

### Checkpoint scope

Allowed now:

- Phase 0 governance and build hygiene on every pass.
- Phase 4 UI, input, rendering, accessibility, display, graphics, options, and presentation containment.
- Narrow authority extraction when touching the owning UI/input/render subsystem.
- Small persistence/server-boundary repairs only when needed to keep the bridged single-player runtime publish-safe.
- Documentation cleanup inside the four durable documents.
- Publish-safe terminology cleanup on package-facing, player-facing, manifest, launcher, installer, and documentation surfaces.

Not active now:

- Broad Mods / Tools, Sector Editor Audit, or in-game editor expansion.
- Live external mod loading, source acquisition, classpath mutation, hot restart, or public multiplayer networking.
- True per-sector parallel world mutation while shared legacy `World` state remains unsplit.
- Broad new world-generation content unless it narrowly repairs a Phase 4 presentation/input defect.
- New standalone Markdown notes.

## Near-term publish-safe gates

Each gate should leave the game buildable and the working tree easy to review. Do not start a later gate by expanding scope; finish the smallest publish-safe version of the current gate first.

### Gate 1 - Repo and document hygiene

Required runtime passes:

- Gate 1.1 - Four-document rule verification. Confirm that planning, standards, history, and doctrine each live in their owning durable file.
- Gate 1.2 - User-facing README containment. Keep README useful to a player or reviewer rather than letting it accumulate changelog, roadmap, standards, or asset-index commentary.
- Gate 1.3 - Repository root cleanliness. Keep root-level files limited to repository control and workspace-map duties.
- Gate 1.4 - History migration preparation. Move stale version-log material to `DEVELOPMENT_HISTORY.md` during a controlled history pass instead of expanding this plan.

Done when: the repo shows only intentional project changes, this plan names the current checkpoint, and no new planning documents have been created.

### Gate 2 - Frame ownership

Required runtime passes:

- Gate 2.1 - Main menu and options frame ownership.
- Gate 2.2 - Gameplay HUD, tactical slate, and recent-actions frame ownership.
- Gate 2.3 - Dossier, inventory, build, look/scan, transition, modal, interaction, combat, workbench, trade, and scavenge frame ownership.
- Gate 2.4 - Button, label, command strip, dropdown, and caption ownership audit.
- Gate 2.5 - Compact caption enforcement.

Done when: high-traffic UI surfaces no longer spill controls outside their owning frames.

### Gate 3 - Text wrapping and clipping

Required runtime passes:

- Gate 3.1 - Shared wrapped-text authority pass.
- Gate 3.2 - Recent Actions and Tactical Slate text containment.
- Gate 3.3 - Look/Scan, Interact, Combat, Build, Character Dossier, Survival, Inventory, Map/Intel, and Options text containment.
- Gate 3.4 - First-impression, transition, capture, and loss surface text containment.
- Gate 3.5 - Cached pixel-width layout extension through shared authorities only.

Done when: player-facing text remains inside its panel at expected desktop scales.

### Gate 4 - Targeting convergence

Required runtime passes:

- Gate 4.1 - Gameplay map draw and mouse hit-test transform unification.
- Gate 4.2 - Keyboard targeting model convergence.
- Gate 4.3 - Gamepad reticle model convergence.
- Gate 4.4 - Panel scroll and input focus conflict repair.

Done when: what the player points at matches what the game targets across mouse, keyboard, and future gamepad paths.

### Gate 5 - Presentation layering

Required runtime passes:

- Gate 5.1 - Dropdown, hover help, timed alert, and popup z-order pass.
- Gate 5.2 - Command panel, map overlay, text capsule, tile icon, NPC, and cursor rectangle z-order pass.
- Gate 5.3 - Developer diagnostics final-surface overlay pass.
- Gate 5.4 - Render-loop metric versus server-authoritative truth separation pass.

Done when: overlays expose useful information without masking the gameplay state they describe.

### Gate 6 - Display, graphics, runtime, and accessibility polish

Required runtime passes:

- Gate 6.1 - Display authority: monitor, density, scaling, borderless/window mode, and per-monitor behavior.
- Gate 6.2 - Graphics authority: render cost, frame pacing, art quality, reduced motion, palette behavior, and render-only lighting treatment.
- Gate 6.3 - JVM/runtime profile authority: restart-bound, clearly labeled runtime choices.
- Gate 6.4 - Dual-channel critical cue pass for touched surfaces.

Done when: default settings are safe for older machines and scaled desktop environments without implying unsupported server GUI or networking features.

### Gate 7 - Asset-preview migration follow-through

Required runtime passes:

- Gate 7.1 - Semantic asset program continuity check.
- Gate 7.2 - Stage 10 mod and art-pack semantic registry extension.
- Gate 7.3 - External registry declaration format pass.
- Gate 7.4 - Namespace/conflict rule pass.
- Gate 7.5 - Override/replacement policy pass.
- Gate 7.6 - Acquisition-time validation pass before world/session initialization.

Done when: external registry declaration format, namespace/conflict rules, override/replacement policy, and acquisition-time validation are defined before world/session initialization.

### Gate 8 - Build, package, and smoke

Required runtime passes:

- Gate 8.1 - Java 17 compile pass.
- Gate 8.2 - Client and server jar rebuild pass.
- Gate 8.3 - Zip/package integrity pass.
- Gate 8.4 - Installer/thin-launcher independence pass.
- Gate 8.5 - Bootstrap dependency rejection pass: installer and thin launcher must not require main client jar, server jar, core asset tree, optional asset packs, or subsidiary main-project files before bootstrap.
- Gate 8.6 - Targeted UI/input/render smoke pass for each touched surface.

Done when: the build artifacts are reproducible, the touched publish-safe path has been checked, and the bootstrap chain can be reasoned about as installer -> thin launcher -> manifest lookup -> main client/server download.

## Durable phase roadmap

Roadmap rule: each former required outcome is now a required subphase/runtime pass. A subphase is not complete because a note exists; it is complete only when the owning authority exists, the old behavior still works or is intentionally migrated, and the verification path is named.

### Phase 0 - Governance, documentation, and build hygiene

Goal: keep the project navigable and prevent disconnected pseudo-progress.

Required subphases:

- Phase 0.1 - Durable document boundary pass. `docs/` contains exactly four durable development documents: this plan, standards and practices, development history, and master governance revision.
- Phase 0.2 - README public-facing pass. README remains user-facing and does not accumulate changelogs, roadmaps, standards, or asset-index commentary.
- Phase 0.3 - Ownership-correct documentation pass. Every pass updates only the durable file that actually owns the information.
- Phase 0.4 - Source-comment containment pass. Source files use semantic authority names and compact behavior comments, not phase-numbered roadmap prose.
- Phase 0.5 - Player-facing string truth pass. Player-facing strings describe current behavior only and avoid placeholder, temporary, or coming-soon language.
- Phase 0.6 - Asset-index location pass. Asset indexes remain machine-readable data under `client/assets/indexes/` or a tool module only when consumed by code, tools, or packaging.
- Phase 0.7 - Installer/thin-launcher bootstrap independence pass. Bootstrap components may not require the main client/server jars, main asset tree, optional packs, or project-local runtime files in order to start and fetch manifests.
- Phase 0.8 - Build-delivery hygiene pass. Java 17 compile, jar rebuild, and zip/package verification remain required before delivery.

Exit criteria: docs remain consolidated, source stays free of planning sprawl, the package builds, and the next target is identifiable from this plan.

### Phase 1 - Framework stabilization and code dissection

Goal: reduce the oversized central Java surface into durable authority classes without changing gameplay behavior by accident.

Status: the main extraction gates are complete enough to proceed. `TheMechanist.java` still contains launcher, panel wiring, selected-context bridges, and legacy glue, but new work must name the owning authority it extends.

Required subphases:

- Phase 1.1 - Touched-subsystem authority extraction pass. Continue extracting each touched subsystem into focused authorities and APIs before expanding its behavior.
- Phase 1.2 - Save/load preservation pass. Preserve save/load behavior while extracting; any migration must name old payloads, new payloads, and fallback behavior.
- Phase 1.3 - Shared-authority routing pass. Route repeated behavior through shared authorities rather than new local helpers.
- Phase 1.4 - Old-hardware performance pass. Maintain old-hardware performance through bounded caches, low-allocation paths, and visible metrics when a hot surface is touched.
- Phase 1.5 - Central-class anti-regrowth pass. Prevent the central class from regrowing into a storage site for unrelated domain code.
- Phase 1.6 - Construction authority pass.
- Phase 1.7 - UniversalWindow/UI authority pass.
- Phase 1.8 - Machine operation authority pass.
- Phase 1.9 - Staffing and labor authority pass.
- Phase 1.10 - Logistics and reservation authority pass.
- Phase 1.11 - Faction/room ledger authority pass.
- Phase 1.12 - Combat/health authority pass.
- Phase 1.13 - Hazard authority pass.
- Phase 1.14 - Persistence authority pass.
- Phase 1.15 - Editor authority pass.
- Phase 1.16 - Localization/text-key authority pass.

Exit criteria: each touched subsystem has a named owning authority, old behavior is preserved or intentionally migrated, and the central class no longer absorbs unrelated storage or workflow logic.

### Phase 2 - Placeholder and asset integration discipline

Goal: convert indexed art from inert asset mass into intentionally promoted gameplay families.

Current state: the staged semantic asset migration has completed through Stage 9. Stage 10 is next: mod and art-pack semantic registry extension.

Required subphases:

- Phase 2.1 - Core runtime art tier pass. Preserve low_32 as the core runtime art tier.
- Phase 2.2 - Optional art-pack pass. Keep standard/intermediate/high art as optional packs.
- Phase 2.3 - Narrow semantic bucket promotion pass. Promote assets only through narrow semantic buckets.
- Phase 2.4 - Placeholder candidate pass. Treat placeholder art handles as candidates, not gameplay implementation.
- Phase 2.5 - Semantic runtime handle pass. Use semantic runtime handles for promoted objects, fixtures, items, portraits, roads, walls, floors, and entities.
- Phase 2.6 - Portrait and faction-art authority pass. Maintain folder/category authority for portraits, faction art, creatures, pets, and item icons.
- Phase 2.7 - Machine index consumption pass. Keep generated machine indexes under `client/assets/indexes/` when consumed by code/tools.
- Phase 2.8 - Eight-character semantic asset ID pass. Route new graphical references toward exact 8-character semantic asset IDs.
- Phase 2.9 - Infopedia asset-audit exposure pass. Expose player-facing assets through the in-game Infopedia asset audit surface as stages mature.
- Phase 2.10 - Direct path fallback audit pass. Direct path fallback must be audited rather than silently hiding wrong art.

Exit criteria: art becomes usable through semantic runtime authority instead of broad filename scans or scrap-icon fallbacks, high-error assets have semantic IDs, and direct path fallback is visible when it occurs.

### Phase 3 - Room, road, frontage, plaza, alley, parking, and spatial integration - COMPLETE

Goal: make generated spaces physically connect, face one another, and produce believable civic, industrial, commercial, residential, sewer, and exterior-boundary layouts.

Completion state: closed as of 0.9.10fz for durable-phase scheduling. Phase 3 established roaded and roadless zone families, descriptor-backed tile identity, road/sidewalk distinction, exterior maintenance boundary layers, world-generation weight semantics, and inspection surfaces sufficient to expose spatial defects. Remaining deep world-generation expansion belongs to later durable world/facility phases, especially Phase 8 and Phase 13, unless a Phase 4 presentation/input defect requires a narrow repair.

Completed subphases:

- Phase 3.1 - Roaded versus roadless zone family pass.
- Phase 3.2 - Road/sidewalk distinction pass.
- Phase 3.3 - Tile descriptor separation pass for roads, walls, floors, corridors, fixtures, overlays, and semantic markers.
- Phase 3.4 - Edge safety band and transition anchor pass.
- Phase 3.5 - Exterior maintenance corridor, bulkhead, and void-layer pass.
- Phase 3.6 - Local inspection and descriptor-reporting pass.
- Phase 3.7 - Tooling detour closure pass.

Exit criteria: met for phase scheduling. Generated zones are descriptor-backed, road/sidewalk-aware, exterior-boundary-aware, and inspectable enough to move into client presentation and input containment.

### Phase 4 - UI, input, rendering, and presentation containment - ACTIVE

Goal: make the client playable and legible on older machines and scaled desktop environments.

Required subphases:

- Phase 4.1 - Zone Audit generation-trace visibility pass.
- Phase 4.2 - Menu text, button, caption, dropdown, and panel containment pass.
- Phase 4.3 - Compact caption pass.
- Phase 4.4 - Body copy wrapping and clipping pass.
- Phase 4.5 - Virtual render layout targeting pass.
- Phase 4.6 - Keyboard targeting convergence pass.
- Phase 4.7 - Mouse targeting convergence pass.
- Phase 4.8 - Gamepad reticle convergence pass.
- Phase 4.9 - Borderless-windowed default and runtime switching pass.
- Phase 4.10 - Narrated intro timing and tail-safety pass.
- Phase 4.11 - Display-density authority pass.
- Phase 4.12 - Graphics/runtime profile authority pass.
- Phase 4.13 - Render-only lighting authority pass.
- Phase 4.14 - Diagnostics/QOL/defaults authority pass.
- Phase 4.15 - Accessibility authority pass.

Exit criteria: the UI does not spill outside frames, target coordinates match what is rendered, and presentation features do not mask gameplay information.

### Phase 5 - Operations, logistics, hauling, and reservations

Goal: make manual and eventual automated work flow through shared operational structures.

Required subphases:

- Phase 5.1 - Shared operation-record lifecycle pass. Use shared operation records for queued, active, paused, blocked, failed, completed, and abandoned work.
- Phase 5.2 - Manual hauling authority pass.
- Phase 5.3 - Source reservation authority pass.
- Phase 5.4 - Route readiness authority pass.
- Phase 5.5 - Delivery intent authority pass.
- Phase 5.6 - Contract lifecycle authority pass.
- Phase 5.7 - Hidden reservation ban pass. Avoid hidden route reservations or autonomous logistics until the manual workflow is stable.
- Phase 5.8 - Inspection/preflight feedback pass. Preserve clear feedback for why a task can or cannot proceed.

Exit criteria: player-directed work can be explained, reserved, executed, interrupted, and audited.

### Phase 6 - Production, machines, recipes, staffing, and operation queues

Goal: make machines and recipes operational instead of decorative.

Required subphases:

- Phase 6.1 - Machine input/tool/staffing definition pass.
- Phase 6.2 - Machine duration, utility, output, status, and interruption pass.
- Phase 6.3 - Manual fallback operation pass. If no assigned worker is available, eligible machines may fall back to manual player operation with explicit time cost.
- Phase 6.4 - Production status inspection pass.
- Phase 6.5 - Hidden local timer removal pass.
- Phase 6.6 - Industrial component requirement pass. Recipe/build requirements should use meaningful industrial components rather than universal supplies.

Exit criteria: production can be planned, staffed, operated, interrupted, and explained.

### Phase 7 - Containers, permissions, access checks, and ownership

Goal: make inventory, storage, machines, and faction spaces respect ownership and access.

Required subphases:

- Phase 7.1 - Actor access check pass for containers.
- Phase 7.2 - Actor access check pass for production.
- Phase 7.3 - Ownership category pass for player-owned, faction-owned, public, restricted, stolen, locked, and forbidden access.
- Phase 7.4 - Access denial message pass.
- Phase 7.5 - Faction consequence pass for unauthorized access.
- Phase 7.6 - Item provenance authority pass.
- Phase 7.7 - Equipment transfer authority pass.

Exit criteria: actors cannot freely use every container or machine without a governing access model.

### Phase 8 - Provenance, item origin, facilities, and supply ecology

Goal: explain why goods exist and where they came from.

Required subphases:

- Phase 8.1 - Food and water provenance pass.
- Phase 8.2 - Fuel and medicine provenance pass.
- Phase 8.3 - Weapons and ammunition provenance pass.
- Phase 8.4 - Construction material and trade-good provenance pass.
- Phase 8.5 - Food production and hydroponics facility pass.
- Phase 8.6 - Water recycler and waste-treatment facility pass.
- Phase 8.7 - Generator, forge, clinic, lab, precinct, market, temple, depot, sewer utility, and estate-service facility pass.
- Phase 8.8 - Facility generation progress/audit surface pass.
- Phase 8.9 - Supply-pressure explanation pass.

Exit criteria: the world can explain item origin and supply pressure.

### Phase 9 - Construction, bases, rooms, defenses, and ownership

Goal: make player and faction spaces physically meaningful.

Required subphases:

- Phase 9.1 - Construction validation pass.
- Phase 9.2 - No-self-entombment pass.
- Phase 9.3 - Placement feedback pass.
- Phase 9.4 - Validation inspection pass.
- Phase 9.5 - Base ownership metadata pass.
- Phase 9.6 - Room metadata pass.
- Phase 9.7 - Passive defense physicalization pass.
- Phase 9.8 - Doors, gates, and barricades integration pass.
- Phase 9.9 - Alarms, cameras, traps, defensive furniture, and checkpoint integration pass.

Exit criteria: buildings and rooms govern access, cover, raids, ownership, and later combat.

### Phase 10 - Hazards, lighting, darkness, and traps

Goal: make dangerous spaces readable and mechanically consistent.

Required subphases:

- Phase 10.1 - Gas, fumes, sludge, and sewer hazard pass.
- Phase 10.2 - Shorted wire, jagged passage, overheating factory, freezer space, and industrial kill-zone pass.
- Phase 10.3 - Trap system pass.
- Phase 10.4 - Warning overlay pass.
- Phase 10.5 - Lighting and visibility integration pass.
- Phase 10.6 - Sound, pathing, health, equipment, and generation integration pass.

Exit criteria: a player can see danger, understand danger, and trace the system that produced it.

### Phase 11 - Combat, health, unconsciousness, death, saves, and feedback

Goal: make violence and failure robust enough for a survival sim.

Required subphases:

- Phase 11.1 - Combat and combat-feedback review pass.
- Phase 11.2 - Health, unconsciousness, armor, cover, weapons, ammunition, and death review pass.
- Phase 11.3 - Unconscious player relocation pass.
- Phase 11.4 - You Lost screen cause/killer/weapon/survival pass.
- Phase 11.5 - You Lost score categories pass: kills, crafted items, knowledge, sectors, zones, NPCs, wealth, bases, recruits, and weighted values.
- Phase 11.6 - Load Save menu handoff pass.
- Phase 11.7 - Save UI manual slot pass.
- Phase 11.8 - Autosave column and autosave trigger pass.

Exit criteria: injury, defeat, recovery, save/load, and final loss are coherent and testable.

### Phase 12 - Character creation, names, ranks, rosters, and social identity

Goal: stop treating characters and factions as thin placeholders.

Required subphases:

- Phase 12.1 - Deterministic stat distribution pass.
- Phase 12.2 - Editable character-name entry pass.
- Phase 12.3 - Original-setting first-name pool pass.
- Phase 12.4 - Original-setting last-name pool pass.
- Phase 12.5 - Original-setting singular-name pool pass.
- Phase 12.6 - Faction rank taxonomy pass.
- Phase 12.7 - Leaders, deputies, supervisors, workers, guards, clergy, representatives, and specialists roster pass.
- Phase 12.8 - Rank-to-facility and rank-to-room authority pass.
- Phase 12.9 - Rank-to-production, quest, and command-chain authority pass.
- Phase 12.10 - Player portrait baseline/base human pool pass.
- Phase 12.11 - Faction and creature portrait exact-folder authority pass.

Exit criteria: factions can produce named people with roles, ranks, continuity, and command meaning.

### Phase 13 - World generation, districts, sewers, temples, estates, and strategic simulation

Goal: make the world a layered industrial city instead of a collection of local rooms.

Required subphases:

- Phase 13.1 - Hab block and apartment district-stamp pass.
- Phase 13.2 - Market and commercial district-stamp pass.
- Phase 13.3 - Industrial sector district-stamp pass.
- Phase 13.4 - Estate and elite-service district-stamp pass.
- Phase 13.5 - Sewer, precinct, clinic, temple, military, utility, waste, cargo, and faction-compound district-stamp pass.
- Phase 13.6 - Neutral civic-faith institution pass near plazas.
- Phase 13.7 - Temple room layout pass: nave halls, pillars, relic alcoves, prayer nooks, candle racks, donation boxes, and supplicant kitchens.
- Phase 13.8 - Temple staffing pass: clergy, pilgrims, guards, and non-hostile head officiants.
- Phase 13.9 - Civic absolution service pass. The head officiant may offer a costly standing-repair service tied to hunger/sleep/time costs and limited civil-faction standing restoration.
- Phase 13.10 - Distant-zone operational ledger simulation pass.

Exit criteria: local zones, offscreen districts, factions, and continuity anchors form one scalable world model.

### Phase 14 - Economy, quests, contracts, and faction reputation

Goal: convert faction presence into work, risk, trade, and standing.

Required subphases:

- Phase 14.1 - Faction representative contract surface pass.
- Phase 14.2 - Fetch-item contract pass.
- Phase 14.3 - Bounty contract pass.
- Phase 14.4 - Contract identity pass: item, target, source, destination, reward, expiry, reputation effect, and failure state.
- Phase 14.5 - Distance-abstraction economy pass.
- Phase 14.6 - Local-detail economy pass where relevant.

Exit criteria: quests and economy produce work tied to faction facilities and item provenance.

### Phase 15 - Editor, localization, modding, and content pipeline

Goal: make content creation durable instead of hand-editing fragile code.

Required subphases:

- Phase 15.1 - Zone-stamp editor pass.
- Phase 15.2 - Feature/entity/faction editor pass.
- Phase 15.3 - Machinery/recipe/item/tile editor pass.
- Phase 15.4 - Mods / Tools local tooling entry pass.
- Phase 15.5 - External mod loading claim ban pass until architecture is verified.
- Phase 15.6 - Keyed localization file pass.
- Phase 15.7 - Editor interoperability and customizability pass.
- Phase 15.8 - Runtime-definition consumption pass for future stamps and content.
- Phase 15.9 - In-game editor surface containment pass.
- Phase 15.10 - Detached external editor window closure pass.

Exit criteria: content can be added through data/editor surfaces instead of central-code sprawl.

### Phase 16 - Polish, packaging, QA, and endgame readiness

Goal: turn the simulation into a coherent playable loop.

Required subphases:

- Phase 16.1 - Lean core archive pass with low_32 assets.
- Phase 16.2 - Optional art/audio pack pass.
- Phase 16.3 - Installer bootstrap independence pass.
- Phase 16.4 - Thin launcher bootstrap independence pass.
- Phase 16.5 - Main client/server payload download and manifest pass.
- Phase 16.6 - Bootstrap artifact dependency rejection pass.
- Phase 16.7 - Java 17 compatibility pass.
- Phase 16.8 - Old-machine viability pass.
- Phase 16.9 - Save/load regression checklist pass.
- Phase 16.10 - Worldgen/pathing/construction regression checklist pass.
- Phase 16.11 - Combat/logistics/staffing regression checklist pass.
- Phase 16.12 - UI/asset fallback regression checklist pass.
- Phase 16.13 - Root new-conversation briefing continuity pass.

Exit criteria: development can proceed phase by phase without losing context, duplicating authorities, or inventing fake implementation.

## Deferred checkpoint lines

These lines are preserved as planning constraints, but they are not the active checkpoint unless the user explicitly reopens them.

### Server authority

The launcher/client/server override established an internal authoritative sector lane for single-player. Future server-authority targets are sector threading preparation without parallel mutation of shared legacy `World`, lifecycle/death/respawn migration behind server authority, and broader command rejection/diagnostic coverage. True per-sector parallel world mutation remains deferred until shared state is split or isolated.

### Persistence architecture

The active persistence direction is character-slot plus world-state separation. `.mechsave` files should become lightweight character attachments for identity, player ID, inventory/equipment/account state, knowledge/history, and a world-file pointer. `.mechworld` files should own generated world ledgers plus mutable runtime world state.

Next persistence targets, when reopened:

- Split item/container ownership so player-owned inventory instances remain with the character while placed/world-owned containers remain in the world file.
- Expand player-faction assignment surfaces.
- Make NPC autonomous faction plans consume world ledgers when the founding player is absent.
- Add migration tests before shorthand key dictionaries or compressed aliases.

### Faction and personnel parity

- Maintain separate player-command and NPC-command rosters while using a shared command-tier scale.
- Founder authority remains a unique player tier above recruited-player ranks.
- Recruited players may receive tiers 1-5; each tier carries equivalent NPC command authority for the same tier and lower.
- Future personnel-management UI should expose player assignments and NPC assignments as separate modules inside one contextual management framework.

### Knowledge composition

The Knowledge Tree is structurally composed into eight major in-game branches with quality gates, cross-branch prerequisites, and a build/smoke audit requiring every registered active knowledge to be placed and reachable by definition. Future work should attach gameplay payloads in small slices: recipe unlocks, construction unlocks, equipment permissions, faction services, character perks, dialogue gates, and passive modifiers. Review point economy against actual XP/contract pacing after play sessions.

### Editor/UI cleanup

The next editor review should audit legacy detached-window paths, stale editor launchers, external-dialog stubs, render-profile/CRT player controls, and any remaining player-facing code that bypasses the main game UI surface. Remove those roots outright or mark them development-only until converted to owned in-game screens. Do not expand editor features broadly until that cleanup audit is complete.

## Candidate for DEVELOPMENT_HISTORY migration

The following version-log style facts are retained here only to avoid losing continuity during this rewrite. They should be moved to `DEVELOPMENT_HISTORY.md` during a dedicated history cleanup pass.

- 0.9.10gc began high-churn panel cached pixel-width text layout.
- 0.9.10gd extended caching to high-frequency map/entity image scaling.
- 0.9.10ge extended caching to repeated UI chrome frame rasters and cache accounting.
- 0.9.10gf established the server-authoritative sector simulation boundary.
- 0.9.10gg migrated the desktop single-player turn loop onto the sector manager by default.
- 0.9.10gj moved the main committed single-player world-mutation lane behind internal-server single-writer `AuthoritativeWorldRuntime`.
- 0.9.10gk hardened explicit turn modes plus server-side long-action gating/progress.
- 0.9.10gl published immutable `WorldSnapshot` records and routed console/admin mutations through explicit request/command records.
- 0.9.10gm added server-derived player countdown overlays, server-world lifecycle respawn resolution, a separate headless server executable/status initializer, and separated `saves/singleplayer/` versus `saves/server/` namespaces.
- 0.9.10gn cataloged slot/world-definition payloads and removed duplicated generated-world ledgers from new `.mechsave` writes.
- 0.9.10go added itemized namespace listing for persistence review.
- 0.9.10gp split mutable `world.*` state out of character slots and into `.mechworld` runtime world files.
- 0.9.10gs added a formal player-faction world ledger.
- 0.9.10gu added compact world-owned autonomous tick output for player-founded factions.
- 0.9.10gw added display-density authority for the Java 17 Swing client.
- 0.9.10gx split Display and Graphics options by responsibility.
- 0.9.10gy separated gameplay light values from visual light treatment.
- 0.9.10gz established diagnostics/runtime profile boundaries.
- 0.9.10ha separated JVM/runtime profile controls from Display and Graphics.
- 0.9.10hb added single-player combined runtime presets and first-pass render-side accessibility controls.
- 0.9.10hc added fallback profile management as the local recovery surface.
- 0.9.10hd added a structured project re-evaluation line.
- 0.9.10hj added gameplay-quality defaults and the QOL options surface.
- 0.9.10jn reopened the in-game editor/UI ownership line after detached Swing editor/progression windows were rejected.
- 0.9.10jr composed the Knowledge Tree into eight major in-game branches.
- Asset migration Stage 4 completed item and UI preview registry-first semantic resolution for high-error carried items.
- Asset migration Stage 5 completed tile descriptor, Zone Audit, and Tile Infopedia semantic asset IDs.
- Asset migration Stage 6 completed object, machine, fixture, construction, and editor-palette semantic asset resolution.
- Asset migration Stage 7 completed direct path-reference audit/enforcement.
- Asset migration Stage 8 completed portrait/entity partitioning.
- Asset migration Stage 9 completed typed missing-art fallback behavior and hardening smoke coverage.

## Anti-drift rule

When uncertain, do not invent a new phase, new document, or new subsystem. Consult this plan, the standards, governance, and history. Then make the smallest bounded implementation that advances the current checkpoint and compiles.

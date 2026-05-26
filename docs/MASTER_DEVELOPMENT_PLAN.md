# The Mechanist - Master Development Plan

This document is the durable roadmap and checkpoint timeline. It is not a changelog and it is not a dumping ground for continuation notes. Future development sessions should first see the phase structure, the active checkpoint, and the next publish-safe gates.

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

Not active now:

- Broad Mods / Tools, Sector Editor Audit, or in-game editor expansion.
- Live external mod loading, source acquisition, classpath mutation, hot restart, or public multiplayer networking.
- True per-sector parallel world mutation while shared legacy `World` state remains unsplit.
- Broad new world-generation content unless it narrowly repairs a Phase 4 presentation/input defect.
- New standalone Markdown notes.

## Near-term publish-safe gates

Each gate should leave the game buildable and the working tree easy to review. Do not start a later gate by expanding scope; finish the smallest publish-safe version of the current gate first.

### Gate 1 - Repo and document hygiene

- Keep the four-document rule intact.
- Keep README user-facing.
- Keep `.gitignore` and packaging hygiene small and intentional.
- Move stale version-log material to `DEVELOPMENT_HISTORY.md` later instead of appending it here.

Done when: the repo shows only intentional project changes, this plan names the current checkpoint, and no new planning documents have been created.

### Gate 2 - Frame ownership

- Audit main menu, options, gameplay HUD, tactical slate, recent actions, dossier, inventory, build, look/auspex, transition, modal, interaction, combat, workbench, trade, and scavenge surfaces.
- Ensure every button, label, command strip, dropdown, and caption belongs to a real frame.
- Keep captions compact and bounded.

Done when: high-traffic UI surfaces no longer spill controls outside their owning frames.

### Gate 3 - Text wrapping and clipping

- Route body copy through wrapped, clipped frame text.
- Keep Recent Actions, Tactical Slate, Look/Auspex, Interact, Combat, Build, Character Dossier, Survival, Inventory, Map/Intel, Options, first-impression, transition, capture, and loss surfaces bounded.
- Extend cached pixel-width layout only through shared text/layout authorities.

Done when: player-facing text remains inside its panel at expected desktop scales.

### Gate 4 - Targeting convergence

- Keep gameplay map draw and gameplay mouse hit-testing on the same viewport transform authority.
- Route mouse, keyboard, and future gamepad reticles through the same targeting model.
- Repair panel-scroll and input-focus conflicts where they hide movement or targeting intent.

Done when: what the player points at matches what the game targets across mouse and keyboard paths.

### Gate 5 - Presentation layering

- Keep dropdowns, hover help, timed alerts, popup frames, command panels, map overlays, text capsules, tile icons, NPCs, cursor rectangles, and diagnostics in intentional z-order.
- Keep F3 diagnostics as a final-surface developer overlay.
- Preserve the separation between render-loop metrics and server-authoritative simulation truth.

Done when: overlays expose useful information without masking the gameplay state they describe.

### Gate 6 - Display, graphics, runtime, and accessibility polish

- Keep Display responsible for monitor, density, scaling, borderless/window mode, and per-monitor behavior.
- Keep Graphics responsible for render cost, frame pacing, art quality, reduced motion, palette behavior, and render-only lighting treatment.
- Keep JVM/runtime profiles restart-bound and clearly labeled.
- Replace color-only critical cues with dual-channel indicators as surfaces are touched.

Done when: default settings are safe for older machines and scaled desktop environments without implying unsupported server GUI or networking features.

### Gate 7 - Asset-preview migration follow-through

- Continue the semantic asset program from its current state.
- Next asset target: Stage 10, mod and art-pack semantic registry extension.
- Do not reopen high-frequency world rendering migration unless it is part of the staged semantic asset path.

Done when: external registry declaration format, namespace/conflict rules, override/replacement policy, and acquisition-time validation are defined before world/session initialization.

### Gate 8 - Build, package, and smoke

- Run Java 17 compile.
- Rebuild jars.
- Verify zip/package integrity.
- Verify installer/thin-launcher independence: installer downloads must contain only the information needed to create/run the thin launcher, and the thin launcher must use its own minimal assets plus repo manifests to obtain the current main client/server payloads.
- Reject any installer or thin-launcher dependency that requires the main client jar, server jar, core asset tree, optional asset packs, or subsidiary main-project files before bootstrap.
- Include at least one targeted UI/input/render smoke for each touched surface.

Done when: the build artifacts are reproducible, the touched publish-safe path has been checked, and the bootstrap chain can be reasoned about as installer -> thin launcher -> manifest lookup -> main client/server download.

## Durable phase roadmap

### Phase 0 - Governance, documentation, and build hygiene

Goal: keep the project navigable and prevent disconnected pseudo-progress.

Required outcomes:

- `docs/` contains exactly four durable development documents: this plan, standards and practices, development history, and master governance revision.
- README remains user-facing and does not accumulate changelogs, roadmaps, standards, or asset-index commentary.
- Every pass updates only the durable file that actually owns the information.
- Source files use semantic authority names and compact behavior comments, not phase-numbered roadmap prose.
- Player-facing strings describe current behavior only and avoid placeholder / temporary / coming-soon language.
- Asset indexes remain machine-readable data under `assets/indexes/` or a tool module only when consumed by code, tools, or packaging.
- Installer and thin-launcher packaging remains asset-independent from the main project: bootstrap components may not require the main client/server jars, main asset tree, optional packs, or project-local runtime files in order to start and fetch manifests.
- Java 17 compile, jar rebuild, and zip packaging remain required before delivery.

Exit criteria: docs remain consolidated, source stays free of planning sprawl, the package builds, and the next target is identifiable from this plan.

### Phase 1 - Framework stabilization and code dissection

Goal: reduce the oversized central Java surface into durable authority classes without changing gameplay behavior by accident.

Status: the main extraction gates are complete enough to proceed. `TheMechanist.java` still contains launcher, panel wiring, selected-context bridges, and legacy glue, but new work must name the owning authority it extends.

Required outcomes:

- Continue extracting touched subsystems into focused authorities and APIs.
- Preserve save/load behavior while extracting.
- Route repeated behavior through shared authorities rather than new local helpers.
- Maintain old-hardware performance.
- Prevent the central class from regrowing into a storage site for unrelated domain code.

Primary authority targets remain construction, UniversalWindow/UI, machine operations, staffing, logistics, faction/room ledgers, combat/health, hazards, persistence, editor, and localization.

### Phase 2 - Placeholder and asset integration discipline

Goal: convert indexed art from inert asset mass into intentionally promoted gameplay families.

Current state: the staged semantic asset migration has completed through Stage 9. Stage 10 is next: mod and art-pack semantic registry extension.

Required outcomes:

- Preserve low_32 as the core runtime art tier.
- Keep standard/intermediate/high art as optional packs.
- Promote assets only through narrow semantic buckets.
- Treat placeholder art handles as candidates, not gameplay implementation.
- Use semantic runtime handles for promoted objects, fixtures, items, portraits, roads, walls, floors, and entities.
- Maintain folder/category authority for portraits, faction art, creatures, pets, and item icons.
- Keep generated machine indexes under `assets/indexes/` when consumed by code/tools.
- Route new graphical references toward exact 8-character semantic asset IDs.
- Expose player-facing assets through the in-game Infopedia asset audit surface as stages mature.

Exit criteria: art becomes usable through semantic runtime authority instead of broad filename scans or scrap-icon fallbacks, high-error assets have semantic IDs, and direct path fallback is audited rather than silently hiding wrong art.

### Phase 3 - Room, road, frontage, plaza, alley, parking, and spatial integration - COMPLETE

Goal: make generated spaces physically connect, face one another, and produce believable civic, industrial, commercial, residential, sewer, and exterior-boundary layouts.

Completion state: closed as of 0.9.10fz for durable-phase scheduling. Phase 3 established roaded and roadless zone families, descriptor-backed tile identity, road/sidewalk distinction, exterior maintenance boundary layers, world-generation weight semantics, and inspection surfaces sufficient to expose spatial defects. Remaining deep world-generation expansion belongs to later durable world/facility phases, especially Phase 8 and Phase 13, unless a Phase 4 presentation/input defect requires a narrow repair.

Completed outcomes:

- Maintained explicit roaded vs roadless zone families.
- Preserved road/sidewalk distinction: sidewalks are street-family tiles, not road-lane connectivity.
- Separated roads, walls, floors, corridors, fixtures, overlays, and semantic markers through compiled tile descriptors.
- Protected edge safety bands, transition anchors, exterior maintenance corridors, bulkheads, and void layers.
- Established enough local inspection and descriptor reporting to identify spatial failures without continuing to patch blind.
- Closed the Phase 3.6 tooling detour as an active target; editor/mod tooling now returns to Phase 15 unless explicitly reopened.

Exit criteria: met for phase scheduling. Generated zones are descriptor-backed, road/sidewalk-aware, exterior-boundary-aware, and inspectable enough to move into client presentation and input containment.

### Phase 4 - UI, input, rendering, and presentation containment - ACTIVE

Goal: make the client playable and legible on older machines and scaled desktop environments.

Required outcomes:

- Keep Zone Audit generation-trace visibility active while repairing worldgen presentation defects.
- Keep all menu text, buttons, captions, dropdowns, and panels bounded to their owning frames.
- Treat captions as compact labels, not body text.
- Use frame wrapping and clipping for body copy.
- Use the virtual render layout for mouse-to-world targeting and never hardcode tile offsets.
- Keep keyboard, mouse, and gamepad reticles under the same targeting model.
- Preserve borderless-windowed default launch and safe runtime window-mode switching.
- Maintain narrated intro timing with tail safety.
- Preserve display-density, graphics/runtime profile, render-only lighting, diagnostics, QOL defaults, and accessibility authorities as the owning paths for related changes.

Exit criteria: the UI does not spill outside frames, target coordinates match what is rendered, and presentation features do not mask gameplay information.

### Phase 5 - Operations, logistics, hauling, and reservations

Goal: make manual and eventual automated work flow through shared operational structures.

Required outcomes:

- Use shared operation records for queued, active, paused, blocked, failed, completed, and abandoned work.
- Keep manual hauling, source reservation, route readiness, delivery intent, and contract lifecycle inside named authorities.
- Avoid hidden route reservations or autonomous logistics until the manual workflow is stable.
- Preserve clear inspection/preflight feedback for why a task can or cannot proceed.

Exit criteria: player-directed work can be explained, reserved, executed, interrupted, and audited.

### Phase 6 - Production, machines, recipes, staffing, and operation queues

Goal: make machines and recipes operational instead of decorative.

Required outcomes:

- Machines expose required inputs, tools, staffing, duration, power/utility needs, outputs, status, and interruption rules.
- If no assigned worker is available, eligible machines may fall back to manual player operation with explicit time cost.
- Production status must be inspectable and must not depend on hidden local timers.
- Recipe/build requirements should use meaningful industrial components rather than universal supplies.

Exit criteria: production can be planned, staffed, operated, interrupted, and explained.

### Phase 7 - Containers, permissions, access checks, and ownership

Goal: make inventory, storage, machines, and faction spaces respect ownership and access.

Required outcomes:

- Add actor access checks for containers and production.
- Distinguish player-owned, faction-owned, public, restricted, stolen, locked, and forbidden access.
- Route access denial through inspectable messages and faction consequences.
- Preserve item provenance and equipment transfer authority.

Exit criteria: actors cannot freely use every container or machine without a governing access model.

### Phase 8 - Provenance, item origin, facilities, and supply ecology

Goal: explain why goods exist and where they came from.

Required outcomes:

- Food, water, fuel, medicine, weapons, ammunition, construction materials, and trade goods have plausible sources.
- Facility families include food production, hydroponics, water recyclers, waste treatment, generators, forges, medicae clinics, labs, precincts, markets, temples, depots, sewer utilities, and noble service layers.
- Generation gains progress/audit surfaces for staged facility and provenance creation.

Exit criteria: the world can explain item origin and supply pressure.

### Phase 9 - Construction, bases, rooms, defenses, and ownership

Goal: make player and faction spaces physically meaningful.

Required outcomes:

- Continue construction validation, no-self-entombment, placement feedback, and validation inspection.
- Expand base ownership and room metadata.
- Promote passive defenses into physical map objects before active targeting.
- Integrate doors, gates, barricades, alarms, cameras, traps, walls, defensive furniture, and checkpoints.

Exit criteria: buildings and rooms govern access, cover, raids, ownership, and later combat.

### Phase 10 - Hazards, lighting, darkness, and traps

Goal: make dangerous spaces readable and mechanically consistent.

Required outcomes:

- Support gas, fumes, sludge, shorted wires, jagged passages, overheating factories, freezer spaces, traps, sewer hazards, and industrial kill zones.
- Add readable warning overlays where appropriate.
- Integrate hazards with lighting, visibility, sound, pathing, health, equipment, and generation.

Exit criteria: a player can see danger, understand danger, and trace the system that produced it.

### Phase 11 - Combat, health, unconsciousness, death, saves, and feedback

Goal: make violence and failure robust enough for a survival sim.

Required outcomes:

- Review combat, combat feedback, health, unconsciousness, armor, cover, weapons, ammunition, and death.
- Unconscious players move to their base if possible, otherwise the nearest medical facility.
- Death opens a You Lost screen with cause, weapon, killer, survival length, score, kills, crafted items, knowledge, sectors, zones, NPCs, wealth, bases, recruits, and weighted values.
- Load Save opens the save/load menu.
- Save UI uses four manual slots plus a separate autosave column.
- Autosaves occur on zone transition or every in-game hour.

Exit criteria: injury, defeat, recovery, save/load, and final loss are coherent and testable.

### Phase 12 - Character creation, names, ranks, rosters, and social identity

Goal: stop treating characters and factions as thin placeholders.

Required outcomes:

- Implement deterministic stat distribution and editable character-name entry.
- Expand Imperial-style first, last, and singular random name pools.
- Give every faction ranks, leaders, deputies, supervisors, workers, guards, priests, representatives, and specialists.
- Tie ranks to facilities, rooms, production authority, quests, and command chains.
- Player portraits use baseline/base human pools only; faction and creature portraits use exact folder authority.

Exit criteria: factions can produce named people with roles, ranks, continuity, and command meaning.

### Phase 13 - World generation, districts, sewers, temples, estates, and strategic simulation

Goal: make the world a layered industrial city instead of a collection of local rooms.

Required outcomes:

- Expand district stamps and zone families: hab blocks, apartments, markets, industrial sectors, noble estates, sewers, precincts, medicae zones, temples, military areas, utilities, waste plants, cargo yards, and faction compounds.
- Integrate Adeptus Ministorum / Ecclesiarchy temples as neutral civilian Imperial anchors near plazas.
- Temples include church rooms, pillars, relics, prayer nooks, candle racks, saint alcoves, donation boxes, supplicant kitchens, priests, pilgrims, Sisters of Battle guards, and immortal/non-targetable head clerics.
- The head cleric offers a 24-hour Imperial forgiveness prayer service costing hunger/sleep and restoring limited standing with civil Imperial factions.
- Expand distant-zone simulation through operational ledgers, not full tile simulation.

Exit criteria: local zones, offscreen districts, factions, and continuity anchors form one scalable world model.

### Phase 14 - Economy, quests, contracts, and faction reputation

Goal: convert faction presence into work, risk, trade, and standing.

Required outcomes:

- Faction representatives offer bounded fetch-item and bounty contracts.
- Contracts identify item, target, source, destination, reward, expiry, reputation effect, and failure state.
- Economy layers use abstraction at distance and detail only where relevant.

Exit criteria: quests and economy produce work tied to faction facilities and item provenance.

### Phase 15 - Editor, localization, modding, and content pipeline

Goal: make content creation durable instead of hand-editing fragile code.

Required outcomes:

- Develop an in-game editor for zone stamps, features, entities, factions, machinery, recipes, items, and tiles.
- Use the Mods / Tools route as the local tooling entry point before claiming live external mod loading.
- Move player-facing text to keyed localization files.
- Keep maximum interoperability and customizability as the editor goal.
- Future stamps and content should consume the same definitions used by runtime systems.
- Keep player-facing editor surfaces inside the main game client surface; external `JFrame`/`JDialog` editor launch paths remain closed from player-facing menus until converted.

Exit criteria: content can be added through data/editor surfaces instead of central-code sprawl.

### Phase 16 - Polish, packaging, QA, and endgame readiness

Goal: turn the simulation into a coherent playable loop.

Required outcomes:

- Package the core archive leanly with low_32 assets and optional art/audio packs.
- Keep installer and thin launcher as independent bootstrap artifacts. The installer creates or obtains the thin launcher; the thin launcher runs with its own minimal assets, reads latest-version manifests from the repo/release source, then downloads the main client and server payloads. If either bootstrap artifact depends on bundled main-client files or main asset packs to launch, it fails the packaging requirement.
- Maintain Java 17 compatibility and old-machine viability.
- Add regression checklists for save/load, worldgen, pathing, construction, combat, logistics, staffing, UI, and asset fallback.
- Preserve a root new-conversation briefing document for continuity.

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

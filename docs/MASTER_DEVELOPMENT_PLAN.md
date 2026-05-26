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
- Clarity and overview expansion of broad roadmap phases/subphases when that expansion prevents vague implementation work.
- Opening and closing QOL/sensibility evaluation for every major phase or reopened phase segment.

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

Dependency rule: asset promotion precedes broad content placement; comparative damage/durability balance precedes vehicle, machine, wall, and heavy-weapon integration; population provenance precedes item provenance; item provenance precedes vehicle provenance; vehicle provenance precedes Ages of Control zone-history generation; leadership schemes must expose player-facing missions through the quest system when player participation is plausible; quest windows must provide enough time and guidance for the player to reach and identify the objective. The game should know what visual assets are usable in world space before placing them broadly, should know the relative scale between handheld weapons, mounted weapons, vehicles, machines, and walls before assigning damage values, should know who exists and who works where before explaining who produced an item, should know which component and factory chains can produce vehicles before factions deploy them, should know vehicle capacity/loss/ownership before using historical faction plans to generate aged control, expansion, assaults, defenses, production, and decay inside zones, and should let the player discover, join, trigger, influence, or fail scheme-related missions through readable timing windows, objective arrows, and clear target highlighting.

### Clarity and overview revision path

Purpose: prevent broad phase names and generic pass labels from becoming vague implementation permission. Any roadmap entry that is too broad, too general, or insufficiently specified must receive a secondary clarification expansion before it can be treated as ready for implementation.

This path applies recursively to gates, phases, subphases, deferred checkpoint lines, and any newly introduced runtime pass. A phase may remain high-level for orientation, but the next actionable slice beneath it must be specific enough that a developer can identify the owning authority, the affected data, the affected UI/runtime surfaces, the intended efficiency or functionality gain, the non-goals, and the verification path.

Trigger terms requiring clarification include but are not limited to: `authority pass`, `integration pass`, `audit pass`, `review pass`, `polish pass`, `containment pass`, `migration pass`, `viability pass`, `hygiene pass`, `system pass`, `support pass`, `cleanup pass`, `expansion pass`, `stabilization pass`, and any phase whose description could be satisfied by a note rather than code, data, tests, tooling, or a player-visible/system-visible result.

Required clarification outputs for a broad phase/subphase:

- Overview: state the practical runtime problem being solved in concrete terms.
- Owning authority: name the class, package, data file, tool module, manifest family, or planned authority that owns the work.
- Current behavior: describe how the system behaves now, including known defects, duplicated logic, missing data, player confusion, performance cost, or packaging risk.
- Desired behavior: describe the functional or systemic improvement that should exist after completion.
- Efficiency path: explain how the pass reduces repeated code, prevents central-class growth, improves runtime performance, reduces packaging risk, clarifies data ownership, improves player comprehension, or makes later work cheaper.
- Functional expansion path: explain what new meaningful gameplay, tooling, packaging, UI, audit, or simulation capability becomes possible because of the pass.
- Extents and limits: name what is in scope, what is explicitly out of scope, and what must not be claimed as implemented.
- Progression steps: break the pass into ordered child steps small enough to review, compile, and smoke individually.
- Data and save impact: state whether saves, manifests, asset indexes, settings, localization, package layout, or world files are touched and how compatibility is preserved.
- UI/player-facing impact: state whether player-facing strings, panels, commands, diagnostics, overlays, or infopedia surfaces are touched.
- Performance and old-hardware impact: state whether the pass can affect hot loops, allocations, asset loading, rendering, startup, save/load, or package size.
- Publish-safe impact: state whether names, assets, likenesses, lore terms, package metadata, or public docs are exposed and whether clearance/quarantine is required.
- Verification path: name compile checks, smoke checks, asset/package validation, UI checks, save/load checks, or manual inspection steps required for completion.

Specificity floor: a clarified pass must not merely say `improve`, `audit`, `integrate`, `support`, or `clean up`. It must name the system being improved, the runtime surface that changes, the mechanism of change, and the condition that proves the change worked. If those facts are unknown, the required next step is a clarification pass, not implementation.

Clarification progression order:

- First clarify the active checkpoint and near-term gates.
- Then clarify any phase being touched by current implementation work.
- Then clarify phases that are prerequisites for packaging, publish safety, save compatibility, or player-facing stability.
- Deferred phases may remain summarized until reopened, but once reopened they must receive the same overview and specificity expansion before implementation.

A valid clarification expansion may add child steps such as `Phase 6.1a`, `Phase 6.1b`, and `Phase 6.1c` under an existing subphase. It must not create a new standalone document. If the expansion becomes too large for a small edit, it still belongs in this master plan unless it is a durable implementation rule for `STANDARDS_AND_PRACTICES.md` or completed work for `DEVELOPMENT_HISTORY.md`.

Immediate clarification backlog:

- Phase 1.6 through Phase 1.16 need secondary expansion because names such as construction authority, machine operation authority, staffing authority, logistics authority, and localization authority identify the intended owners but not the child runtime seams, extraction order, or verification criteria.
- Phase 2.1 through Phase 2.15 need secondary expansion because master asset integration must identify real graphical assets, promote unused assets into usable world-space definitions, expose usage readiness, and assign comparative damage/durability tiers without silently bloating runtime packages.
- Phase 4.11 through Phase 4.17 need secondary expansion because display density, graphics/runtime profiles, diagnostics/QOL/defaults, accessibility, large-entity targeting, and quest guidance overlays are active publish-safe systems with performance, UI, and settings implications.
- Phase 5.1 through Phase 5.8 need secondary expansion because operations, hauling, reservations, route readiness, delivery intent, contracts, and preflight feedback will define much of the later automation model.
- Phase 6.1 through Phase 6.6 need secondary expansion because production and machine operation can easily become hidden timers unless the data model, UI inspection, staffing fallback, and interruption semantics are specified.
- Phase 8.1 through Phase 8.12 need secondary expansion before population simulation work begins because demographic origin, age promotion, faction membership, workplace assignment, room occupancy, and reinforcement requests become foundational inputs for item provenance and faction continuity.
- Phase 10.1 through Phase 10.20 need secondary expansion before vehicle systems begin because vehicles require promoted vehicle assets, component factories, vehicle factories, ownership ledgers, body schemas, part integrity, repairs, faction control, transit behavior, comparative durability, road-only movement, and sector-level military balance.
- Phase 11.1 through Phase 11.20 need secondary expansion before historical zone generation work begins because Ages of Control, leadership schemes, faction expansion, concessions, assaults, defense, population drift, production plans, vehicle deployment/loss, room control, player-visible scheme events, scheme-quest timing windows, and active gameplay scheme activation can otherwise become unbounded worldgen prose rather than inspectable generation ledgers.
- Phase 17.1 through Phase 17.15 need secondary expansion before scheme quests are implemented because player-facing mission offers need planning/execution/cooldown timing, variable availability, non-synchronized activation times, minimum active duration, countdown messaging, active participation rules, neutral missed-quest standing behavior, clear objective highlighting, pointer arrows, fail conditions, and compact NPC dialogue surfaces.
- Phase 16 and Phase 19 need secondary expansion before broad world-content or release-candidate work resumes because district simulation, asset packs, old-machine viability, and regression checklists require measurable acceptance criteria.

Example clarification pattern:

- Broad entry: `Phase 1.6 - Construction authority pass.`
- Clarified child pass: `Phase 1.6a - Construction command intake`. Define the command record, source UI surfaces, validation inputs, and rejection messages.
- Clarified child pass: `Phase 1.6b - Construction placement validation`. Define collision checks, access checks, no-self-entombment rules, cost checks, and preview feedback.
- Clarified child pass: `Phase 1.6c - Construction execution authority`. Define work operation records, material reservation, interruption handling, save/load behavior, and inspection output.
- Completion proof: compile, place/cancel/build smoke, save/load during pending construction, and player-facing rejection message inspection.

### Phase quality-of-life and sensibility evaluator

Purpose: every major phase must be evaluated as a player-facing product slice, not merely as an internal engineering exercise. A system can compile and still be useless, opaque, cluttered, misleading, or nonsensical to the player. This evaluator is mandatory at the beginning and end of every major phase segment and every reopened phase segment.

Opening evaluator: before implementation begins on a major phase or reopened segment, answer these questions in the pass plan or implementation notes:

- Player usefulness: what does this phase make easier, clearer, more interesting, safer, faster, or more meaningful for the player?
- Player visibility: is the created or changed system visible to the player, indirectly visible through diagnostics/infopedia/status panels, or entirely internal?
- Information value: what information is being displayed, and why does the player need it at that moment?
- Clutter rejection: what information should not be shown because it is noise, debug residue, redundant state, or internal implementation detail?
- Identifier hygiene: are long package IDs, item IDs, asset IDs, Java class names, manifest keys, registry names, or internal handles hidden from ordinary player-facing UI unless a developer/audit surface explicitly needs them?
- Placeholder audit: does any visible text, icon, title, faction name, item name, tooltip, warning, button, or panel still look like placeholder material that needs revision?
- End-to-end logic: does the proposed system make coherent, cogent sense from player action to system response to feedback to save/load persistence?
- Compact transparency: is the information presented compactly and plainly, with enough explanation to be understood but not enough to drown the player?
- Sensible defaults: does the system choose defaults that a normal player would understand without reading development notes?
- Failure readability: when the system refuses an action, breaks, blocks, or waits, does it tell the player the real reason in useful language?

Closing evaluator: before a major phase or reopened segment may be marked complete, answer these questions against the implemented result:

- Did the implementation produce a player-useful improvement, or only an internal rearrangement?
- If the result is internal-only, is there a justified reason it should remain invisible, and is any required diagnostic/audit surface available to developers?
- Is every player-facing string final enough for the current publish-safe tier, or does it need localization/keying/rewrite before release?
- Are internal package IDs, registry IDs, asset handles, UUIDs, debug keys, stack-trace labels, or class names hidden from normal UI?
- Are technical identifiers still visible only in explicit developer/audit/diagnostic surfaces?
- Is every visible panel, tooltip, alert, button, title, list row, and status line compact, transparent, and useful?
- Does any display surface show duplicated information, stale information, hidden timers, misleading state, or clutter that should be collapsed or removed?
- Does the system make sense end to end when used by a player who has not read the source code?
- Does the implementation preserve old-hardware readability and performance by avoiding unnecessary redraws, allocations, oversized text blocks, asset bloat, and hot-loop string churn?
- Does the result remain publish-safe by avoiding uncleared names, likenesses, protected terms, placeholder lore, or unlicensed asset references?

Evaluator enforcement: no major phase may be considered complete unless its closing evaluator has a positive answer or a named follow-up subphase for every negative answer. If the negative answer affects player comprehension, publish safety, save/load safety, or package integrity, the follow-up must be scheduled before any release-candidate claim.

Evaluator placement: this section is considered prepended and appended to every `### Phase N` segment. During later clarification expansion, phase-specific QOL child checks may be written directly under the affected phase, but the generic evaluator remains binding even when not repeated verbatim under each heading.

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

### Phase 2 - Master asset integration and world-usable asset promotion

Goal: promote the graphical assets already in the project from inert files or preview-only candidates into world-usable, indexed, semantically meaningful game assets.

This phase exists because the project now has a large asset base, but a large asset base is not the same thing as a usable game asset system. The game needs to know which graphical assets exist, which are unused, which are placeholders, which are publish-safe, which belong in the world, which belong only in UI/editor/diagnostics, and which require future content passes before promotion. Unused assets should not remain invisible indefinitely; they should be discovered, classified, indexed, and promoted where they can meaningfully improve rooms, items, fixtures, characters, facilities, hazards, production, world generation, or UI clarity.

Required subphases:

- Phase 2.1 - Master asset inventory pass. Identify every graphical asset family available under the client asset tree and optional art-pack inputs, including objects, fixtures, machines, items, portraits, faction art, creatures, roads, floors, walls, overlays, UI icons, editor icons, hazards, facility details, and decorative room features.
- Phase 2.2 - Unused asset discovery pass. Generate or maintain an audit that distinguishes assets referenced by code/data from assets present but unused, assets only used by tools, assets only used by editor previews, and assets that are currently dead inventory.
- Phase 2.3 - Publish-safe asset clearance pass. Mark each asset family as approved public, internal development-only, quarantined, needs replacement, needs license/source note, or unknown. No unclear asset should be promoted into a public package without a clearance status.
- Phase 2.4 - World-usable asset promotion pass. Convert appropriate unused or underused graphical assets into usable world-space definitions with stable IDs, categories, dimensions, render layer, collision assumptions, interaction class, and placement constraints.
- Phase 2.5 - Semantic ID and registry pass. Assign exact semantic asset IDs and registry entries so promoted assets can be referenced by worldgen, room stamps, facilities, items, machines, hazards, UI, and editor surfaces without direct fragile path references.
- Phase 2.6 - Usage information pass. For each promoted asset, record intended use, valid contexts, invalid contexts, owning subsystem, player-facing label, audit notes, and future integration opportunities.
- Phase 2.7 - Placeholder-to-candidate distinction pass. Treat placeholder art handles as candidates, not gameplay implementation; a placeholder may appear in diagnostics or editor previews but must not silently masquerade as finished game content.
- Phase 2.8 - Runtime art-tier pass. Preserve low_32 as the lean runtime art tier while standard/intermediate/high art remain optional packs or higher-quality overrides.
- Phase 2.9 - Optional art-pack registry pass. Define how optional art packs declare overrides, replacements, semantic IDs, quality tier, source, package ownership, and conflict behavior.
- Phase 2.10 - Direct path fallback audit pass. Direct path fallback must be audited rather than silently hiding wrong art; fallback should identify the missing ID, requested category, and attempted context on developer/audit surfaces.
- Phase 2.11 - Infopedia asset-audit exposure pass. Expose player-safe asset information through the Infopedia or audit panels as stages mature while hiding internal package IDs from ordinary player views.
- Phase 2.12 - Editor palette promotion pass. Make promoted world assets available to the in-game editor and future stamp editor through semantic categories rather than raw filenames.
- Phase 2.13 - Worldgen integration readiness pass. Mark which promoted assets are ready for room stamps, facility stamps, district generation, road/corridor placement, sewer placement, industrial placement, hazard placement, or faction compound placement.
- Phase 2.14 - Asset bloat and old-hardware pass. Prevent broad promotion from bloating startup, memory use, packaging size, or hot-loop rendering; use indexed lookup, lazy loading, quality tiers, and explicit package ownership.
- Phase 2.15 - Comparative damage/durability tier asset pass. Assign world-scale balance categories to weapons, walls, machines, doors, vehicles, mounted weapons, and structural assets so later systems cannot accidentally treat a handheld dagger, rifle, APC, tank, block wall, or industrial machine as comparable durability/damage peers.

Exit criteria: graphical assets are no longer a passive pile of files. The project can list what exists, what is unused, what is unsafe, what is world-usable, what has been promoted, where it may appear, what broad balance scale it belongs to, and what future phase should integrate it. Direct path fallback is visible, high-error assets have semantic IDs, and world/editor systems can consume promoted assets through stable registry entries.

### Phase 3 - Room, road, frontage, plaza, alley, parking, and spatial integration - COMPLETE

Goal: make generated spaces physically connect, face one another, and produce believable civic, industrial, commercial, residential, sewer, and exterior-boundary layouts.

Completion state: closed as of 0.9.10fz for durable-phase scheduling. Phase 3 established roaded and roadless zone families, descriptor-backed tile identity, road/sidewalk distinction, exterior maintenance boundary layers, world-generation weight semantics, and inspection surfaces sufficient to expose spatial defects. Remaining deep world-generation expansion belongs to later durable world/facility phases, especially Phase 9, Phase 10, Phase 11, and Phase 16, unless a Phase 4 presentation/input defect requires a narrow repair.

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
- Phase 4.16 - Large-entity targeting and interaction pass. Ensure vehicles, walls, machines, and other large durable targets use appropriate hit areas, selection feedback, access/ownership messaging, road/parking constraints, and repair/damage inspection without making them feel like ordinary handheld-target entities.
- Phase 4.17 - Quest objective guidance overlay pass. Quest UI must provide clear player-facing objective guidance with a pointer arrow toward the current objective or the nearest zone transition that leads toward it, plus slowly pulsing highlights on the involved container, entity, room, vehicle, machine, or interaction target when the target is in the visible/current context.

Exit criteria: the UI does not spill outside frames, target coordinates match what is rendered, presentation features do not mask gameplay information, and active quests clearly guide the player toward the relevant objective without deciding how the player must complete it.

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
- Phase 7.8 - Vehicle access and ownership bridge pass. Prepare container/access ownership categories to handle vehicle storage, vehicle cargo, faction motor pools, crew access, seized vehicles, abandoned vehicles, restricted military assets, and repair permissions.

Exit criteria: actors cannot freely use every container, machine, vehicle, vehicle cargo hold, or faction asset without a governing access model.

### Phase 8 - Population provenance, workforce context, and demographic continuity

Goal: explain who lives in a zone, where they came from, how they got there, what faction or household they belong to, what rooms they occupy, what work they do, and how they become part of future faction continuity.

This phase must come before item provenance because items are not meaningfully produced by abstract buildings alone. Once the simulation tracks people, rooms, work assignments, machine operation, faction membership, age cohorts, and reinforcement flows, later item provenance can identify who made or handled an item, what machine produced it, which facility owned the production, and which population group supplied the labor.

Required subphases:

- Phase 8.1 - Abstract population target intake pass. Consume existing world-generation and zone-creation population targets per area, district, room type, faction territory, facility type, and settlement density without pretending those numbers are already people.
- Phase 8.2 - Population origin pass. Assign contextual origins such as locally born, recently arrived, faction-transferred, recruited, displaced, contracted labor, household dependent, facility staff, prisoner, patient, pilgrim, merchant, guard, or transient visitor.
- Phase 8.3 - Arrival and movement provenance pass. Track how population groups reached a zone: local birth, road migration, faction reinforcement, labor assignment, evacuation, contract convoy, market flow, district transfer, or worldgen seed placement.
- Phase 8.4 - Household and room occupancy pass. Associate people and population groups with rooms, bunks, apartments, barracks, clinics, temples, work dormitories, cells, markets, workshops, and service spaces.
- Phase 8.5 - Faction membership and social role pass. Distinguish general residents, faction members, recruits, workers, guards, specialists, clergy, traders, technicians, supervisors, deputies, leaders, dependents, and outsiders.
- Phase 8.6 - Age cohort and promotion pass. Track age bands and promotion eligibility so created population targets at the correct age can be promoted into faction membership, specialist roles, and leadership structures instead of remaining anonymous general population forever.
- Phase 8.7 - Workforce assignment pass. Track who works where, which machines they operate, which buildings they maintain, which objects they build, which rooms they occupy during work, and what shifts or duty cycles they follow.
- Phase 8.8 - Reinforcement request integration pass. Tie faction reinforcement requests to population provenance so reinforcements have origin, route, faction identity, role, age band, equipment expectation, and arrival context.
- Phase 8.9 - Population promotion and succession pass. Define how residents become recruits, recruits become workers or guards, workers become specialists, specialists become supervisors, and supervisors can enter leadership or command structures.
- Phase 8.10 - Population ledger persistence pass. Store population provenance in world ledgers without bloating save files with unnecessary per-person detail until a person becomes individually relevant.
- Phase 8.11 - Simulation granularity pass. Define when population remains aggregated and when it splits into individual tracked actors because the player met them, hired them, fought them, assigned them, followed them, or saw them operate an important machine.
- Phase 8.12 - Player-facing population audit pass. Provide compact, useful population explanations through zone audit, faction panels, room inspection, facility inspection, or infopedia surfaces without exposing internal IDs or overwhelming the player.

Exit criteria: zones no longer contain only abstract population numbers. The world can say who the population represents, where they came from, how they arrived, what rooms they occupy, what factions they belong to, what work they perform, which machines or facilities they operate, and when they can become individually tracked faction members, specialists, or leaders. This phase must produce the human/labor context required for item provenance, facility production, faction reinforcement, and long-term settlement continuity.

### Phase 9 - Item provenance, facility origin, and supply ecology

Goal: explain why goods exist, who made them, where they came from, which facility or machine produced them, and which population or faction supplied the labor.

Required subphases:

- Phase 9.1 - Food and water provenance pass.
- Phase 9.2 - Fuel and medicine provenance pass.
- Phase 9.3 - Weapons and ammunition provenance pass.
- Phase 9.4 - Construction material and trade-good provenance pass.
- Phase 9.5 - Producer and operator linkage pass. Use Phase 8 population provenance to connect item creation to workers, machine operators, facility staff, contractors, scavengers, traders, or faction supply chains.
- Phase 9.6 - Machine and facility production ledger pass. Record which machine, facility, room, or district produced or handled goods when the simulation has enough information to do so.
- Phase 9.7 - Vehicle component provenance pass. Treat engines, wheels, tracks, armor plates, weapons mounts, batteries, fuel cells, sensors, transmissions, hull frames, repair parts, and other vehicle components as provenance-tracked goods.
- Phase 9.8 - Food production and hydroponics facility pass.
- Phase 9.9 - Water recycler and waste-treatment facility pass.
- Phase 9.10 - Generator, forge, clinic, lab, precinct, market, temple, depot, sewer utility, and estate-service facility pass.
- Phase 9.11 - Facility generation progress/audit surface pass.
- Phase 9.12 - Supply-pressure explanation pass.

Exit criteria: the world can explain item origin and supply pressure through facilities, machines, factions, trade paths, salvage paths, and the populations or workers responsible for production and handling, including the component chains needed by later vehicle factories.

### Phase 10 - Vehicle provenance, factories, component schemas, and faction vehicle control

Goal: make vehicles into provenance-tracked, inspectable, buildable, damageable, repairable, ownable, transport-capable, and strategically meaningful faction assets rather than decorative sprites or isolated item icons.

This phase consumes promoted Phase 2 vehicle and component assets, Phase 8 population/workforce provenance, Phase 9 component/facility provenance, and Phase 7 ownership/access rules. The game has vehicle assets for cars, trucks, bikes, APCs, tanks, and component families; this phase turns those assets into usable vehicle definitions, production chains, factories, body schemas, faction motor pools, transit systems, repair workflows, and sector-level military/economic power.

Required subphases:

- Phase 10.1 - Vehicle asset inventory and promotion pass. Identify car, truck, bike, APC, tank, wreck, chassis, turret, wheel, track, armor, engine, cargo, weapon, repair, and vehicle-component assets; promote usable ones into semantic vehicle and component registry entries.
- Phase 10.2 - Vehicle class and variant taxonomy pass. Define civilian, commercial, industrial, emergency, security, military, heavy military, cargo, scouting, patrol, armored personnel, and armored fighting vehicle classes with faction-appropriate variants.
- Phase 10.3 - Corporate/manufacturer entity pass. Create corporate, guild, workshop, foundry, motorworks, military contractor, salvage yard, and faction-affiliated producer entities that manufacture or refurbish vehicle models and variants.
- Phase 10.4 - Vehicle component factory pass. Define production-chain factories for engines, transmissions, wheels, tracks, hull frames, armor plate, suspension, weapons mounts, sensors, power systems, cargo beds, crew compartments, repair parts, and replacement assemblies.
- Phase 10.5 - Vehicle final-assembly factory pass. Define vehicle factories that consume component provenance, workforce assignments, machine/facility provenance, and manufacturer identity to produce complete vehicles or refurbished vehicles.
- Phase 10.6 - Vehicle provenance ledger pass. Track manufacturer, model, variant, production batch, facility of origin, component sources, current owner, former owners, faction assignment, damage history, repair history, and capture/salvage history.
- Phase 10.7 - Vehicle ownership and access pass. Integrate vehicle ownership with faction property, motor pools, restricted assets, stolen/seized vehicles, player-owned vehicles, crew permissions, repair permissions, cargo access, and command permissions.
- Phase 10.8 - Vehicle transit and routing pass. Use vehicles as transportation entities between locations inside a zone and for intra-zone/inter-zone transit where roads, gates, garages, depots, checkpoints, and security conditions allow it.
- Phase 10.9 - Vehicle body schema pass. Define inspectable body layouts for vehicles analogous to creature body parts: chassis, cab, engine, transmission, wheels/tracks, armor, turret, weapons, fuel/power system, cargo, crew compartment, sensors, suspension, and external fittings.
- Phase 10.10 - Vehicle component integrity and endurance pass. Each major component should have integrity, endurance, damage thresholds, disabled states, reduced-performance states, catastrophic failure states, and repair/replacement requirements.
- Phase 10.11 - Vehicle damage model pass. Support damage from combat, traps, crashes, hazards, maintenance failure, sabotage, degradation, and age; damage should affect movement, armor, weapons, cargo, crew safety, noise, fuel use, and tactical value.
- Phase 10.12 - Vehicle repair and maintenance pass. Define repair workflows using items, tools, skilled labor, workshops, spare parts, replacement components, salvage, and time costs.
- Phase 10.13 - Vehicle inspection UI/pass. Provide compact player-facing inspection for ownership, condition, damaged parts, cargo, crew, assigned faction, manufacturer, model, and repair needs while hiding raw IDs from ordinary UI.
- Phase 10.14 - Faction vehicle doctrine pass. Define how factions value vehicles for transport, patrol, cargo, assault, defense, intimidation, evacuation, logistics, production support, and strategic projection.
- Phase 10.15 - Military asset balance pass. Track APC and tank counts as sector-level balance-of-power factors that affect assaults, defenses, negotiations, deterrence, route control, and faction confidence.
- Phase 10.16 - Leadership vehicle scheme pass. Allow faction leaders to plan vehicle construction, acquisition, deployment, destruction, capture, repair, concealment, escort, convoy use, and production-capacity expansion.
- Phase 10.17 - Vehicle loss and salvage pass. Destroyed, abandoned, captured, or disabled vehicles should become salvage, wrecks, repair projects, trophies, hazards, or contested assets depending on context.
- Phase 10.18 - Comparative damage and durability scale pass. Handheld/entity weapons, mounted weapons, vehicles, walls, machines, and structural objects must not share the same damage tier. Mounted vehicle weapons should be roughly an order of magnitude above the deadliest player/entity-wielded weapons, and vehicle durability should be roughly an order of magnitude above ordinary player/entity endurance and health. Walls and industrial machines should sit on comparable structural scales. A dagger should require effectively absurd repeated effort, on the order of a thousand concentrated turns, to do meaningful damage to a vehicle, wall, or machine; a powerful anti-armor/plasma-class weapon might do meaningful damage after a much smaller number of attacks, such as roughly a dozen, depending on target and component.
- Phase 10.19 - Road, parking, and sidewalk transit constraint pass. Vehicles are true endgame entities and must be transit-limited. Ordinary vehicles may travel on roads, alleys, lanes, garages, depots, ramps, vehicle yards, and parking lots only. They cannot be driven freely anywhere on the map. At most, owned vehicles may be allowed to park on sidewalks or designated curb/sidewalk-adjacent parking spots when the player exits, with the vehicle automatically resolving to a legal nearby parking position.
- Phase 10.20 - Save/load and old-hardware pass. Persist vehicle ledgers and component states without bloating saves; use aggregate records for distant vehicles and full part schemas only for player-visible or strategically relevant vehicles.

Exit criteria: vehicles have production origin, component origin, ownership, access rules, inspectable body schemas, damage/repair states, factory support chains, faction usage doctrine, relative durability/damage scale, road/parking-constrained movement, and strategic meaning. A faction’s cars, trucks, bikes, APCs, and tanks are not merely placed sprites; they are assets that can move people and goods, consume production capacity, alter balance of power, be damaged, be repaired, be captured, be destroyed, and influence zone and sector history.

### Phase 11 - Ages of Control, faction leadership schemes, and zone generation history

Goal: make zone generation a historical simulation of control, expansion, loss, recovery, production, population movement, political schemes, assaults, defense, decay, vehicle deployment, and faction leadership decisions instead of a one-shot placement of rooms.

This phase consumes Phase 8 population provenance, Phase 9 item/facility provenance, and Phase 10 vehicle provenance. Once the simulation knows who lives in a zone, who works there, what is produced there, which facilities exist, which vehicles are owned or deployed, and what factions have access to people, goods, and transport/military assets, world generation can model each zone as the result of multiple historical ages. Each age records who controlled the zone, what their leadership wanted, how much they expanded, what they built, what vehicles or military assets they deployed or lost, who moved in or out, what production succeeded or failed, what assaults or defenses occurred, and how later factions inherited, contested, repaired, repurposed, or degraded the rooms and assets left behind.

Required subphases:

- Phase 11.1 - Zone-generation mechanics review pass. Audit current zone generation, room minimums, room type guarantees, room ownership assignment, faction control assignment, population target injection, facility placement, production placement, vehicle placement, transit layout, and historical metadata gaps.
- Phase 11.2 - Age-of-control data model pass. Define an ordered zone-history record containing age name/index, controlling faction, dominant faction advantage, rival factions, leadership scheme, population trend, production trend, vehicle trend, room changes, facility changes, conflict events, political events, decay events, and inheritance into the next age.
- Phase 11.3 - Initial viability age pass. The first playable viability age for a zone should create at least 20 rooms and include at least one room of every required type for minimum zone viability before later ages expand, repurpose, damage, or transfer control.
- Phase 11.4 - Dominant-faction advantage pass. The dominant faction in a zone should receive real advantages in keeping or expanding control, but not immunity from decline, assault, intrigue, population loss, production failure, vehicle loss, or concessions.
- Phase 11.5 - Leadership scheme definition pass. Define leadership schemes such as expansion, consolidation, fortification, production surge, vehicle buildup, housing boom, political capture, intrigue, sabotage, assault preparation, recovery, retreat, austerity, decadence, and managed decay.
- Phase 11.6 - Leadership scheme timing pass. Active gameplay schemes should use a baseline three-day planning phase, three-day execution phase, and three-day cooldown phase, with variability based on faction leader experience, planning competence, available staff, faction pressure, scheme complexity, and time since the last plan attempt so schemes do not always appear on rigid identical timers. Exact activation and attempt times must vary within the day so assaults, assassinations, sabotage, negotiations, and other operations do not all begin at midnight, 12:01, or the first tick of a new day.
- Phase 11.7 - Expansion and room-growth pass. During expansion ages, faction schemes may add rooms, enlarge districts, improve production capacity, increase housing capacity, add defenses, add service rooms, add garages/depots, or create new facility clusters using promoted Phase 2 assets.
- Phase 11.8 - Loss, concession, and room-control transfer pass. During decline, conflict, or negotiated concession ages, room ownership and facility control can transfer from one faction to another, become neutral, become contested, become abandoned, or become degraded.
- Phase 11.9 - Population drift and demographic shock pass. Each age can produce population boom, population loss, migration, reinforcement arrival, evacuation, displacement, labor import, prison intake, disease loss, recruitment, or faction membership promotion using Phase 8 provenance.
- Phase 11.10 - Production plan success/failure pass. Faction production schemes should consume Phase 9 item/facility provenance to decide whether a zone gained workshops, lost machines, expanded supply chains, suffered shortages, produced surplus, or degraded into scavenging.
- Phase 11.11 - Vehicle deployment and loss-history pass. Vehicle provenance should inform historical convoys, patrol routes, motor pools, armored assaults, tank losses, APC captures, truck shortages, garage expansion, road-control changes, and vehicle wreck/scar placement.
- Phase 11.12 - Assault, defense, and conflict-history pass. Historical assaults and defenses should leave traces such as damaged rooms, reinforced checkpoints, abandoned rooms, contested borders, new barricades, population losses, weapon stockpiles, vehicle wrecks, or faction-control scars.
- Phase 11.13 - Political plan and intrigue pass. Faction leadership may pursue influence, bribery, infiltration, administrative capture, religious/civic legitimacy, blackmail, legal control, or covert displacement that changes room control without direct combat.
- Phase 11.14 - Degradation and maintenance-failure pass. Ages can decay infrastructure, degrade rooms, break machines, damage vehicles, reduce housing quality, create hazards, block corridors, damage utilities, or create abandoned pockets when maintenance or political will fails.
- Phase 11.15 - Cross-zone sector scheme pass. Leadership schemes must not be only local. A faction controlling or contesting several zones in a sector may plan expansion corridors, production chains, vehicle routes, assaults, defenses, migration routes, reinforcement paths, trade plans, and political capture across multiple zones.
- Phase 11.16 - Ongoing gameplay scheme activation pass. The same leadership-scheme framework used during world generation should later support active gameplay planning, so factions can continue to expand, assault, reinforce, build, deploy vehicles, lose vehicles, recover, scheme, and change room control after game start.
- Phase 11.17 - Player-visible active scheme event pass. Active faction schemes in the player’s current zone should have a chance to become visible world events rather than hidden ledger math. If a faction attempts to seize a room, annex a facility, reinforce a checkpoint, storm a room, sabotage a machine, defend territory, or counterattack another faction, the player should be able to witness appropriate actors, movement, combat, warnings, noise, aftermath, and control-state changes when they are nearby or when zone visibility rules allow it.
- Phase 11.18 - Scheme mission state handoff pass. Scheme records must expose their current lifecycle state to the quest system: planning, execution, cooldown, completed, failed, or cancelled. Planning-phase schemes may offer preparation, scouting, sabotage, support, delivery, diplomacy, or betrayal missions. Execution-phase schemes may allow the player to join, trigger, escort, fight, defend, sabotage, or intervene in the active operation. Active player missions tied to a scheme must automatically fail or resolve to an appropriate missed-window outcome if the scheme reaches cooldown before the player completes the mission.
- Phase 11.19 - Zone history inspection pass. Provide compact audit surfaces that explain the current zone layout as a sequence of ages: who controlled it, what they built, what they lost, who moved, what production changed, what vehicles mattered, what active schemes are known or rumored, and why the zone looks the way it does.
- Phase 11.20 - Deterministic generation and save compatibility pass. Age histories must be deterministic from seed/world settings where appropriate, persist enough history for inspection and ongoing gameplay, and avoid save bloat by aggregating historical events until player interaction requires detail.

Exit criteria: a zone is generated as the visible result of historical control rather than a static room scatter. Minimum viable zones begin with at least 20 rooms and at least one room of every required type, then subsequent ages can expand, lose, regain, repurpose, damage, or transfer rooms according to faction leadership schemes, population provenance, item/facility provenance, vehicle provenance, and sector-wide faction plans. The same leadership scheme model can later activate during ongoing gameplay, active schemes in the player’s zone can surface as visible assaults, defenses, reinforcements, sabotage, control transfers, or aftermath, and player quests can attach to planning/execution windows with clear availability, countdown, objective guidance, and failure behavior.

### Phase 12 - Construction, bases, rooms, defenses, and ownership

Goal: make player and faction spaces physically meaningful.

Required subphases:

- Phase 12.1 - Construction validation pass.
- Phase 12.2 - No-self-entombment pass.
- Phase 12.3 - Placement feedback pass.
- Phase 12.4 - Validation inspection pass.
- Phase 12.5 - Base ownership metadata pass.
- Phase 12.6 - Room metadata pass.
- Phase 12.7 - Passive defense physicalization pass.
- Phase 12.8 - Doors, gates, and barricades integration pass.
- Phase 12.9 - Alarms, cameras, traps, defensive furniture, and checkpoint integration pass.

Exit criteria: buildings and rooms govern access, cover, raids, ownership, and later combat.

### Phase 13 - Hazards, lighting, darkness, and traps

Goal: make dangerous spaces readable and mechanically consistent.

Required subphases:

- Phase 13.1 - Gas, fumes, sludge, and sewer hazard pass.
- Phase 13.2 - Shorted wire, jagged passage, overheating factory, freezer space, and industrial kill-zone pass.
- Phase 13.3 - Trap system pass.
- Phase 13.4 - Warning overlay pass.
- Phase 13.5 - Lighting and visibility integration pass.
- Phase 13.6 - Sound, pathing, health, equipment, and generation integration pass.
- Phase 13.7 - Vehicle hazard integration pass. Allow wrecks, fuel leaks, blocked roads, damaged engines, sabotaged vehicles, armored hulks, and transport choke points to participate in hazard and visibility systems.

Exit criteria: a player can see danger, understand danger, and trace the system that produced it.

### Phase 14 - Combat, health, unconsciousness, death, saves, and feedback

Goal: make violence and failure robust enough for a survival sim.

Required subphases:

- Phase 14.1 - Combat and combat-feedback review pass.
- Phase 14.2 - Health, unconsciousness, armor, cover, weapons, ammunition, and death review pass.
- Phase 14.3 - Unconscious player relocation pass.
- Phase 14.4 - You Lost screen cause/killer/weapon/survival pass.
- Phase 14.5 - You Lost score categories pass: kills, crafted items, knowledge, sectors, zones, NPCs, wealth, bases, recruits, vehicles owned, vehicles destroyed, vehicles captured, and weighted values.
- Phase 14.6 - Vehicle combat and damage bridge pass. Ensure vehicle damage, crew harm, component failure, weapon hits, armor protection, ramming/crash damage, and vehicle destruction can later connect to the combat system.
- Phase 14.7 - Structural and vehicle weapon-scale balance pass. Ensure melee weapons, handheld firearms, heavy handheld weapons, anti-armor weapons, mounted weapons, walls, machines, and vehicles use distinct armor/durability/damage scales so a pocket knife does not threaten a tank, but rare endgame anti-armor weapons can meaningfully damage structural or vehicle targets over a plausible number of attacks.
- Phase 14.8 - Load Save menu handoff pass.
- Phase 14.9 - Save UI manual slot pass.
- Phase 14.10 - Autosave column and autosave trigger pass.

Exit criteria: injury, defeat, recovery, save/load, vehicle loss, structural damage, and final loss are coherent and testable.

### Phase 15 - Character creation, names, ranks, rosters, and social identity

Goal: stop treating characters and factions as thin placeholders.

Required subphases:

- Phase 15.1 - Deterministic stat distribution pass.
- Phase 15.2 - Editable character-name entry pass.
- Phase 15.3 - Original-setting first-name pool pass.
- Phase 15.4 - Original-setting last-name pool pass.
- Phase 15.5 - Original-setting singular-name pool pass.
- Phase 15.6 - Faction rank taxonomy pass.
- Phase 15.7 - Leaders, deputies, supervisors, workers, guards, clergy, representatives, and specialists roster pass.
- Phase 15.8 - Rank-to-facility and rank-to-room authority pass.
- Phase 15.9 - Rank-to-production, quest, and command-chain authority pass.
- Phase 15.10 - Rank-to-vehicle authority pass. Define which ranks can request, crew, command, repair, assign, seize, build, or deploy vehicle assets.
- Phase 15.11 - Player portrait baseline/base human pool pass.
- Phase 15.12 - Faction and creature portrait exact-folder authority pass.

Exit criteria: factions can produce named people with roles, ranks, continuity, command meaning, and vehicle authority.

### Phase 16 - World generation, districts, sewers, temples, estates, and strategic simulation

Goal: make the world a layered industrial city whose zones inherit visible history from assets, population provenance, item/facility provenance, vehicle provenance, and Ages of Control rather than existing as isolated local rooms.

Required subphases:

- Phase 16.1 - Hab block and apartment district-stamp pass.
- Phase 16.2 - Market and commercial district-stamp pass.
- Phase 16.3 - Industrial sector district-stamp pass.
- Phase 16.4 - Estate and elite-service district-stamp pass.
- Phase 16.5 - Sewer, precinct, clinic, temple, military, utility, waste, cargo, vehicle, and faction-compound district-stamp pass.
- Phase 16.6 - Neutral civic-faith institution pass near plazas.
- Phase 16.7 - Temple room layout pass: nave halls, pillars, relic alcoves, prayer nooks, candle racks, donation boxes, and supplicant kitchens.
- Phase 16.8 - Temple staffing pass: clergy, pilgrims, guards, and non-hostile head officiants.
- Phase 16.9 - Civic absolution service pass. The head officiant may offer a costly standing-repair service tied to hunger/sleep/time costs and limited civil-faction standing restoration.
- Phase 16.10 - Vehicle facility district pass. Generate garages, depots, motor pools, repair shops, vehicle yards, military vehicle bays, convoy staging areas, salvage yards, and vehicle component factories where faction history and production capacity justify them.
- Phase 16.11 - Distant-zone operational ledger simulation pass.
- Phase 16.12 - Population-provenance worldgen integration pass. Ensure district generation assigns plausible population origins, occupancy, workforce, faction affiliation, and age cohorts rather than raw population numbers only.
- Phase 16.13 - Asset-promotion worldgen integration pass. Ensure promoted Phase 2 assets are usable by room stamps, district stamps, facility stamps, and audit surfaces through semantic IDs.
- Phase 16.14 - Vehicle-provenance worldgen integration pass. Ensure vehicles, vehicle factories, garages, depots, wrecks, roads, blocked routes, motor pools, and vehicle strategic effects consume Phase 10 provenance.
- Phase 16.15 - Ages-of-control worldgen integration pass. Ensure zone generation consumes Phase 11 histories so district stamps and room layouts reflect expansion, loss, concessions, defense, production success/failure, vehicle buildup/loss, degradation, population movement, and faction leadership schemes.

Exit criteria: local zones, offscreen districts, factions, population continuity anchors, asset placement, facility provenance, vehicle provenance, room control history, and leadership schemes form one scalable world model.

### Phase 17 - Economy, quests, contracts, and faction reputation

Goal: convert faction presence, leadership schemes, supply needs, and strategic plans into player-facing work, risk, trade, reputation, and visible participation opportunities.

Required subphases:

- Phase 17.1 - Faction representative contract surface pass.
- Phase 17.2 - Fetch-item contract pass.
- Phase 17.3 - Bounty contract pass.
- Phase 17.4 - Contract identity pass: item, target, source, destination, reward, expiry, reputation effect, and failure state.
- Phase 17.5 - Distance-abstraction economy pass.
- Phase 17.6 - Local-detail economy pass where relevant.
- Phase 17.7 - Population-aware contract source pass. Allow contracts to reference workforce, residents, specialists, leaders, reinforcements, and facility staff when those people are relevant to the work.
- Phase 17.8 - Vehicle-aware contract pass. Allow contracts for vehicle delivery, vehicle repair, vehicle theft/recovery, convoy escort, motor-pool supply, component acquisition, vehicle sabotage, vehicle salvage, and faction military buildup.
- Phase 17.9 - Leadership-scheme contract pass. Allow faction leaders and representatives to produce contracts that reflect active schemes such as expansion, defense, production surge, sabotage, political capture, recovery, convoy planning, or vehicle deployment.
- Phase 17.10 - Scheme quest lifecycle timing pass. Scheme-related quests use the scheme lifecycle: planning, execution, cooldown, completed, failed, or cancelled. The baseline cadence is three days of planning, three days of execution, and three days of cooldown, but actual offer timing and cooldown timing should vary based on faction leader experience, planning skill, available staff, pressure, scheme complexity, and time since the last plan attempt. Exact activation times should vary within the day so operations do not synchronize to midnight or the first tick of the day. Any active quest window should remain active for at least two days unless the player directly causes an early resolution, so normal player transit and participation time are possible.
- Phase 17.11 - Planning-phase mission offer pass. During the planning phase, NPCs may offer preparation missions such as scouting, supply delivery, recruiting, bribery, sabotage setup, route clearing, vehicle preparation, room mapping, defensive preparation, or betrayal opportunities.
- Phase 17.12 - Execution-phase participation pass. During the execution phase, the player may join, trigger, escort, defend, sabotage, fight, negotiate, steal, rescue, or otherwise participate in the active scheme. These missions should be able to spawn or attach to visible in-zone events when the player is nearby.
- Phase 17.13 - Cooldown and missed-window rule pass. If a scheme reaches cooldown while a player’s related mission is still active, that mission should automatically fail, resolve to a missed-window outcome, or convert into aftermath cleanup only when that makes coherent sense. Failed, missed, expired, or unaccepted quests must not reduce standing by themselves. Faction standing should be reduced by actions taken against a faction, betrayal, theft, violence, sabotage, exposure, or other concrete hostile behavior, not by merely failing to arrive in time. NPCs who will offer the same or related quest again soon must show a compact countdown or availability note instead of giving a vague refusal.
- Phase 17.14 - Quest objective pointer and highlighting pass. Player quests must expose clear, identifiable target guidance. The quest system should draw an arrow toward the current objective, or toward the nearest zone transition that takes the player closer to that objective. In the target zone or visible context, slowly pulsing objective highlighting should make the relevant entity, container, room, machine, vehicle, corpse, document, door, terminal, or interaction point unmistakable. The player should know exactly who must be killed, who must be contacted, what container must be looted, what item must be recovered, or what location must be reached; how the player completes that objective remains their choice.
- Phase 17.15 - Quest guidance QOL and clutter pass. Objective arrows and highlights must be useful rather than noisy. They should avoid raw IDs, avoid permanent screen clutter, allow sensible toggles or reduced guidance where appropriate, and distinguish between exact known targets, approximate search areas, rumored targets, and hostile/hidden objectives without misleading the player.

Exit criteria: quests and economy produce work tied to faction facilities, population roles, item provenance, vehicle provenance, actual supply needs, active leadership plans, and scheme lifecycle timing. The player can see when scheme-related missions are being planned, when they can join an active operation, why an opportunity is unavailable, when a repeat offer is expected to return, where to go next, and which object or actor matters once they arrive.

### Phase 18 - Editor, localization, modding, and content pipeline

Goal: make content creation durable instead of hand-editing fragile code.

Required subphases:

- Phase 18.1 - Zone-stamp editor pass.
- Phase 18.2 - Feature/entity/faction editor pass.
- Phase 18.3 - Machinery/recipe/item/tile editor pass.
- Phase 18.4 - Vehicle/component/factory editor pass. Let future data/editor surfaces define vehicle classes, body schemas, components, factories, manufacturers, repair recipes, cargo rules, and faction vehicle doctrine.
- Phase 18.5 - Mods / Tools local tooling entry pass.
- Phase 18.6 - External mod loading claim ban pass until architecture is verified.
- Phase 18.7 - Keyed localization file pass.
- Phase 18.8 - Editor interoperability and customizability pass.
- Phase 18.9 - Runtime-definition consumption pass for future stamps and content.
- Phase 18.10 - In-game editor surface containment pass.
- Phase 18.11 - Detached external editor window closure pass.
- Phase 18.12 - Asset promotion editor pass. Let promoted Phase 2 world assets appear in editor palettes through semantic categories and publish-safe labels.
- Phase 18.13 - Population provenance editor/audit pass. Let future editor/audit surfaces inspect population origins, role distributions, room occupancy, and workforce assignments without raw internal IDs.
- Phase 18.14 - Vehicle provenance editor/audit pass. Let future editor/audit surfaces inspect vehicle origin, owner, manufacturer, component schema, part damage, repair needs, cargo, crew, motor-pool assignment, and strategic faction value without raw internal IDs.
- Phase 18.15 - Ages-of-control editor/audit pass. Let future editor/audit surfaces inspect zone ages, control changes, leadership schemes, room transfers, production outcomes, vehicle outcomes, visible scheme events, and historical scars without requiring raw internal IDs.
- Phase 18.16 - Scheme quest editor/audit pass. Let future data/editor surfaces inspect scheme quest windows, planning/execution/cooldown timing, leader variability, exact time-of-day activation variance, minimum active windows, offer countdowns, missed-window standing neutrality, failure rules, target guidance rules, objective highlights, and visible event hooks without exposing raw internal IDs in normal player UI.

Exit criteria: content can be added through data/editor surfaces instead of central-code sprawl, and asset promotion, population provenance, item provenance, vehicle provenance, ages-of-control histories, and scheme quests can be inspected or extended through owned data surfaces.

### Phase 19 - Polish, packaging, QA, and endgame readiness

Goal: turn the simulation into a coherent playable loop.

Required subphases:

- Phase 19.1 - Lean core archive pass with low_32 assets.
- Phase 19.2 - Optional art/audio pack pass.
- Phase 19.3 - Installer bootstrap independence pass.
- Phase 19.4 - Thin launcher bootstrap independence pass.
- Phase 19.5 - Main client/server payload download and manifest pass.
- Phase 19.6 - Bootstrap artifact dependency rejection pass.
- Phase 19.7 - Java 17 compatibility pass.
- Phase 19.8 - Old-machine viability pass.
- Phase 19.9 - Save/load regression checklist pass.
- Phase 19.10 - Worldgen/pathing/construction regression checklist pass.
- Phase 19.11 - Combat/logistics/staffing regression checklist pass.
- Phase 19.12 - UI/asset fallback regression checklist pass.
- Phase 19.13 - Root new-conversation briefing continuity pass.
- Phase 19.14 - Master asset integration release audit. Verify promoted assets, unused assets, quarantined assets, package ownership, optional packs, semantic IDs, and comparative damage/durability tier labels before release-candidate packaging.
- Phase 19.15 - Population provenance release audit. Verify population ledgers, workforce assignments, reinforcement requests, age promotion, and item-provenance handoff before release-candidate claims.
- Phase 19.16 - Vehicle provenance release audit. Verify vehicle assets, component assets, manufacturers, component factories, vehicle factories, ownership ledgers, body schemas, damage/repair loops, road/parking transit limits, faction control, and strategic balance tracking before release-candidate claims.
- Phase 19.17 - Ages-of-control release audit. Verify zone-history ledgers, minimum viable room generation, historical room control changes, faction scheme outcomes, vehicle outcomes, player-visible active scheme events, active gameplay scheme handoff, and player-facing inspection surfaces before release-candidate claims.
- Phase 19.18 - Scheme quest lifecycle release audit. Verify planning/execution/cooldown quest windows, leader/timing variability, non-synchronized time-of-day activation, minimum two-day active quest windows, NPC countdowns for repeat offers, neutral missed-quest standing behavior, execution-phase participation, automatic missed-window failure, visible event hooks, objective arrows, pulsing target highlights, and compact player-facing mission text before release-candidate claims.

Exit criteria: development can proceed phase by phase without losing context, duplicating authorities, inventing fake implementation, shipping unclear assets, or claiming provenance/history/scheme systems that do not yet have population, workforce, production, vehicle, room-control, active-scheme visibility, leadership-scheme context, player-facing mission lifecycle behavior, and clear objective guidance.

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

### Population, workforce, and provenance continuity

Population provenance is the prerequisite bridge between world generation and item provenance. Future persistence and world-ledger work should preserve the distinction between aggregated population groups, individually tracked actors, room occupancy, faction membership, age cohorts, promotion eligibility, workforce assignment, and machine/facility operation. Do not require every background resident to become a full actor, but do preserve enough ledger context to answer who lives here, who works here, who operates this, who made that, who requested reinforcements, and who can rise into faction structure.

### Vehicle provenance and faction vehicle power

Vehicles are faction assets, transport entities, production-chain outputs, repair projects, and strategic power markers. Future world-ledger work should preserve the distinction between vehicle model, variant, manufacturer, component provenance, current owner, former owners, faction assignment, crew access, cargo access, body schema, component integrity, repair history, deployment history, and military/economic value. Civilian vehicles, commercial vehicles, industrial vehicles, APCs, and tanks should not be interchangeable; heavy military assets must affect sector-level balance of power and leadership scheme planning. Vehicles must be transit-limited to roads, alleys, vehicle yards, depots, garages, parking lots, and designated curb/sidewalk parking behavior; they must not become free-roaming map traversal tools.

### Comparative durability and damage scale

Handheld weapons, mounted weapons, vehicles, machines, and structural walls must exist on intentionally different scale tiers. Ordinary handheld tools and light weapons should not be meaningful anti-vehicle, anti-machine, or anti-wall options. Mounted weapons and dedicated anti-armor or structural weapons should be exceptional, expensive, loud, risky, and balance-shaping. Structural targets and vehicles should expose damage through component/state degradation rather than behaving like ordinary character health bars.

### Ages of Control and faction leadership schemes

Ages of Control are the historical bridge between item/population/vehicle provenance and final zone layout. Future worldgen and active gameplay planning should use shared leadership-scheme records so faction leaders can pursue expansion, consolidation, production, vehicle buildup, defense, assault, political capture, intrigue, recovery, retreat, or degradation both during initial world generation and after game start. Zone layout should preserve enough history to explain why rooms exist, who built them, who lost them, who controls them now, what vehicles or military assets mattered, and what scars or successes remain. Active schemes should be able to become player-visible events in the current zone when appropriate, including room assaults, counterattacks, defensive musters, sabotage, reinforcement arrivals, and post-conflict aftermath.

### Scheme quest lifecycle and availability

Leadership schemes should feed the player quest system rather than remaining invisible faction logic. The default scheme lifecycle is a three-day planning phase, a three-day execution phase, and a three-day cooldown phase, with timing variability based on faction leader experience, planning skill, available staff, pressure, scheme complexity, and time since the last plan. Exact activation times and attempt times should vary within the day so operations are not synchronized to midnight, 12:01, or the first tick of the day. Planning-phase quests should let the player prepare, scout, supply, undermine, or influence the operation before it begins. Execution-phase quests should let the player join, trigger, defend, sabotage, escort, fight, negotiate, or otherwise participate while the operation is live. Any active quest window should last at least two days unless the player directly resolves it early. If an active scheme quest reaches cooldown before completion, it should fail, convert to a missed-window outcome, or become aftermath work only when that is logically coherent. Failed, missed, expired, or unaccepted quests should not reduce faction standing by themselves; only concrete actions against a faction should reduce standing. NPCs who will offer a quest again soon should show a compact countdown rather than a vague refusal.

### Quest objective guidance and target clarity

Player quests should clearly identify the target without forcing a single solution path. The quest system should provide an arrow toward the objective or toward the nearest zone transition that moves the player closer to it. Once the player is in the relevant context, slowly pulsing objective highlights should make the involved container, entity, corpse, machine, vehicle, room, terminal, document, door, or interaction point unmistakable. The player should know who to kill, who to contact, what to loot, what item to recover, or where to go; how they achieve that objective remains up to them. Objective guidance must avoid raw internal IDs, avoid excessive clutter, and distinguish exact known targets from approximate search areas or rumored targets.

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
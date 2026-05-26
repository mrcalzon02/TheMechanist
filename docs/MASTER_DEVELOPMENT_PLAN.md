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

Dependency rule: asset promotion precedes broad content placement; population provenance precedes item provenance. The game should know what visual assets are usable in world space before it tries to place them broadly, and it should know who exists, where they live, where they work, what faction they belong to, and what machines or rooms they operate before it tries to explain who produced an item.

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
- Phase 2.1 through Phase 2.14 need secondary expansion because master asset integration must identify real graphical assets, promote unused assets into usable world-space definitions, and expose usage readiness without silently bloating runtime packages.
- Phase 4.11 through Phase 4.15 need secondary expansion because display density, graphics/runtime profiles, diagnostics/QOL/defaults, and accessibility are active publish-safe systems with performance, UI, and settings implications.
- Phase 5.1 through Phase 5.8 need secondary expansion because operations, hauling, reservations, route readiness, delivery intent, contracts, and preflight feedback will define much of the later automation model.
- Phase 6.1 through Phase 6.6 need secondary expansion because production and machine operation can easily become hidden timers unless the data model, UI inspection, staffing fallback, and interruption semantics are specified.
- Phase 8.1 through Phase 8.12 need secondary expansion before population simulation work begins because demographic origin, age promotion, faction membership, workplace assignment, room occupancy, and reinforcement requests become foundational inputs for item provenance and faction continuity.
- Phase 9 and Phase 14 need secondary expansion before broad world-content work resumes because item/facility provenance and district simulation can otherwise sprawl into unbounded content generation.
- Phase 17.1 through Phase 17.13 need secondary expansion before any release-candidate claim because packaging, asset packs, compatibility, old-machine viability, and regression checklists require measurable acceptance criteria.

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

Exit criteria: graphical assets are no longer a passive pile of files. The project can list what exists, what is unused, what is unsafe, what is world-usable, what has been promoted, where it may appear, and what future phase should integrate it. Direct path fallback is visible, high-error assets have semantic IDs, and world/editor systems can consume promoted assets through stable registry entries.

### Phase 3 - Room, road, frontage, plaza, alley, parking, and spatial integration - COMPLETE

Goal: make generated spaces physically connect, face one another, and produce believable civic, industrial, commercial, residential, sewer, and exterior-boundary layouts.

Completion state: closed as of 0.9.10fz for durable-phase scheduling. Phase 3 established roaded and roadless zone families, descriptor-backed tile identity, road/sidewalk distinction, exterior maintenance boundary layers, world-generation weight semantics, and inspection surfaces sufficient to expose spatial defects. Remaining deep world-generation expansion belongs to later durable world/facility phases, especially Phase 9 and Phase 14, unless a Phase 4 presentation/input defect requires a narrow repair.

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
- Phase 9.7 - Food production and hydroponics facility pass.
- Phase 9.8 - Water recycler and waste-treatment facility pass.
- Phase 9.9 - Generator, forge, clinic, lab, precinct, market, temple, depot, sewer utility, and estate-service facility pass.
- Phase 9.10 - Facility generation progress/audit surface pass.
- Phase 9.11 - Supply-pressure explanation pass.

Exit criteria: the world can explain item origin and supply pressure through facilities, machines, factions, trade paths, salvage paths, and the populations or workers responsible for production and handling.

### Phase 10 - Construction, bases, rooms, defenses, and ownership

Goal: make player and faction spaces physically meaningful.

Required subphases:

- Phase 10.1 - Construction validation pass.
- Phase 10.2 - No-self-entombment pass.
- Phase 10.3 - Placement feedback pass.
- Phase 10.4 - Validation inspection pass.
- Phase 10.5 - Base ownership metadata pass.
- Phase 10.6 - Room metadata pass.
- Phase 10.7 - Passive defense physicalization pass.
- Phase 10.8 - Doors, gates, and barricades integration pass.
- Phase 10.9 - Alarms, cameras, traps, defensive furniture, and checkpoint integration pass.

Exit criteria: buildings and rooms govern access, cover, raids, ownership, and later combat.

### Phase 11 - Hazards, lighting, darkness, and traps

Goal: make dangerous spaces readable and mechanically consistent.

Required subphases:

- Phase 11.1 - Gas, fumes, sludge, and sewer hazard pass.
- Phase 11.2 - Shorted wire, jagged passage, overheating factory, freezer space, and industrial kill-zone pass.
- Phase 11.3 - Trap system pass.
- Phase 11.4 - Warning overlay pass.
- Phase 11.5 - Lighting and visibility integration pass.
- Phase 11.6 - Sound, pathing, health, equipment, and generation integration pass.

Exit criteria: a player can see danger, understand danger, and trace the system that produced it.

### Phase 12 - Combat, health, unconsciousness, death, saves, and feedback

Goal: make violence and failure robust enough for a survival sim.

Required subphases:

- Phase 12.1 - Combat and combat-feedback review pass.
- Phase 12.2 - Health, unconsciousness, armor, cover, weapons, ammunition, and death review pass.
- Phase 12.3 - Unconscious player relocation pass.
- Phase 12.4 - You Lost screen cause/killer/weapon/survival pass.
- Phase 12.5 - You Lost score categories pass: kills, crafted items, knowledge, sectors, zones, NPCs, wealth, bases, recruits, and weighted values.
- Phase 12.6 - Load Save menu handoff pass.
- Phase 12.7 - Save UI manual slot pass.
- Phase 12.8 - Autosave column and autosave trigger pass.

Exit criteria: injury, defeat, recovery, save/load, and final loss are coherent and testable.

### Phase 13 - Character creation, names, ranks, rosters, and social identity

Goal: stop treating characters and factions as thin placeholders.

Required subphases:

- Phase 13.1 - Deterministic stat distribution pass.
- Phase 13.2 - Editable character-name entry pass.
- Phase 13.3 - Original-setting first-name pool pass.
- Phase 13.4 - Original-setting last-name pool pass.
- Phase 13.5 - Original-setting singular-name pool pass.
- Phase 13.6 - Faction rank taxonomy pass.
- Phase 13.7 - Leaders, deputies, supervisors, workers, guards, clergy, representatives, and specialists roster pass.
- Phase 13.8 - Rank-to-facility and rank-to-room authority pass.
- Phase 13.9 - Rank-to-production, quest, and command-chain authority pass.
- Phase 13.10 - Player portrait baseline/base human pool pass.
- Phase 13.11 - Faction and creature portrait exact-folder authority pass.

Exit criteria: factions can produce named people with roles, ranks, continuity, and command meaning.

### Phase 14 - World generation, districts, sewers, temples, estates, and strategic simulation

Goal: make the world a layered industrial city instead of a collection of local rooms.

Required subphases:

- Phase 14.1 - Hab block and apartment district-stamp pass.
- Phase 14.2 - Market and commercial district-stamp pass.
- Phase 14.3 - Industrial sector district-stamp pass.
- Phase 14.4 - Estate and elite-service district-stamp pass.
- Phase 14.5 - Sewer, precinct, clinic, temple, military, utility, waste, cargo, and faction-compound district-stamp pass.
- Phase 14.6 - Neutral civic-faith institution pass near plazas.
- Phase 14.7 - Temple room layout pass: nave halls, pillars, relic alcoves, prayer nooks, candle racks, donation boxes, and supplicant kitchens.
- Phase 14.8 - Temple staffing pass: clergy, pilgrims, guards, and non-hostile head officiants.
- Phase 14.9 - Civic absolution service pass. The head officiant may offer a costly standing-repair service tied to hunger/sleep/time costs and limited civil-faction standing restoration.
- Phase 14.10 - Distant-zone operational ledger simulation pass.
- Phase 14.11 - Population-provenance worldgen integration pass. Ensure district generation assigns plausible population origins, occupancy, workforce, faction affiliation, and age cohorts rather than raw population numbers only.
- Phase 14.12 - Asset-promotion worldgen integration pass. Ensure promoted Phase 2 assets are usable by room stamps, district stamps, facility stamps, and audit surfaces through semantic IDs.

Exit criteria: local zones, offscreen districts, factions, population continuity anchors, asset placement, and facility provenance form one scalable world model.

### Phase 15 - Economy, quests, contracts, and faction reputation

Goal: convert faction presence into work, risk, trade, and standing.

Required subphases:

- Phase 15.1 - Faction representative contract surface pass.
- Phase 15.2 - Fetch-item contract pass.
- Phase 15.3 - Bounty contract pass.
- Phase 15.4 - Contract identity pass: item, target, source, destination, reward, expiry, reputation effect, and failure state.
- Phase 15.5 - Distance-abstraction economy pass.
- Phase 15.6 - Local-detail economy pass where relevant.
- Phase 15.7 - Population-aware contract source pass. Allow contracts to reference workforce, residents, specialists, leaders, reinforcements, and facility staff when those people are relevant to the work.

Exit criteria: quests and economy produce work tied to faction facilities, population roles, item provenance, and actual supply needs.

### Phase 16 - Editor, localization, modding, and content pipeline

Goal: make content creation durable instead of hand-editing fragile code.

Required subphases:

- Phase 16.1 - Zone-stamp editor pass.
- Phase 16.2 - Feature/entity/faction editor pass.
- Phase 16.3 - Machinery/recipe/item/tile editor pass.
- Phase 16.4 - Mods / Tools local tooling entry pass.
- Phase 16.5 - External mod loading claim ban pass until architecture is verified.
- Phase 16.6 - Keyed localization file pass.
- Phase 16.7 - Editor interoperability and customizability pass.
- Phase 16.8 - Runtime-definition consumption pass for future stamps and content.
- Phase 16.9 - In-game editor surface containment pass.
- Phase 16.10 - Detached external editor window closure pass.
- Phase 16.11 - Asset promotion editor pass. Let promoted Phase 2 world assets appear in editor palettes through semantic categories and publish-safe labels.
- Phase 16.12 - Population provenance editor/audit pass. Let future editor/audit surfaces inspect population origins, role distributions, room occupancy, and workforce assignments without raw internal IDs.

Exit criteria: content can be added through data/editor surfaces instead of central-code sprawl, and both asset promotion and population provenance can be inspected or extended through owned data surfaces.

### Phase 17 - Polish, packaging, QA, and endgame readiness

Goal: turn the simulation into a coherent playable loop.

Required subphases:

- Phase 17.1 - Lean core archive pass with low_32 assets.
- Phase 17.2 - Optional art/audio pack pass.
- Phase 17.3 - Installer bootstrap independence pass.
- Phase 17.4 - Thin launcher bootstrap independence pass.
- Phase 17.5 - Main client/server payload download and manifest pass.
- Phase 17.6 - Bootstrap artifact dependency rejection pass.
- Phase 17.7 - Java 17 compatibility pass.
- Phase 17.8 - Old-machine viability pass.
- Phase 17.9 - Save/load regression checklist pass.
- Phase 17.10 - Worldgen/pathing/construction regression checklist pass.
- Phase 17.11 - Combat/logistics/staffing regression checklist pass.
- Phase 17.12 - UI/asset fallback regression checklist pass.
- Phase 17.13 - Root new-conversation briefing continuity pass.
- Phase 17.14 - Master asset integration release audit. Verify promoted assets, unused assets, quarantined assets, package ownership, optional packs, and semantic IDs before release-candidate packaging.
- Phase 17.15 - Population provenance release audit. Verify population ledgers, workforce assignments, reinforcement requests, age promotion, and item-provenance handoff before release-candidate claims.

Exit criteria: development can proceed phase by phase without losing context, duplicating authorities, inventing fake implementation, shipping unclear assets, or claiming provenance systems that do not yet have population/workforce context.

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
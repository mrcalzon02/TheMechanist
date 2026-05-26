# The Mechanist - Master Development Plan

This document is the authoritative roadmap, checkpoint, and handoff control surface for The Mechanist. It is not a changelog, implementation transcript, asset index, or full design bible. Detailed phase-group expansions now live in the ordered milestone sequence indexed by `docs/MILESTONE_INDEX.md`.

This revision intentionally keeps the master plan concise and redirects implementation-depth planning to the ordered milestone documents. The master plan remains the place to determine the active checkpoint, the current gates, the dependency order, and the correct next handoff target.

## Authority documents

Before any development pass, read these documents in order:

1. `docs/MASTER_DEVELOPMENT_PLAN.md` - current checkpoint, phase order, and movement authority.
2. `docs/STANDARDS_AND_PRACTICES.md` - durable implementation, build, packaging, asset, UI, Java 17, security, and release-safety rules.
3. `docs/MASTER_GOVERNANCE_REVISION_II.md` - long-term architecture doctrine and boundary discipline.
4. `docs/DEVELOPMENT_HISTORY.md` - completed work and historical implementation facts.
5. `docs/MILESTONE_INDEX.md` - ordered milestone navigation for detailed phase-group targets.
6. `docs/LEGACY_MILESTONE_SOURCE_MAP.md` - source/archive map for older topical milestone files.

The Standards file remains hard release law. The Governance file remains design ethos and architectural boundary law. This master plan controls movement and handoff order.

## Documentation containment

Planning belongs in this master plan or in the ordered milestone sequence indexed by `docs/MILESTONE_INDEX.md`. Completed work belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

The ordered milestone files are a user-ordered durable exception created to prevent the master plan from becoming an unsafe monolith. Do not create additional milestone, supplement, audit, addendum, or planning files unless the user explicitly orders a new phase-aligned document and the material cannot fit into an existing ordered milestone.

Legacy topical milestone files are source/archive material only. Their canonical ordered homes are mapped in `LEGACY_MILESTONE_SOURCE_MAP.md`. They are not preferred Codex implementation entry points.

## Active checkpoint

Current checkpoint: **Phase 4 publish-safe client containment and package/handoff hygiene**.

The active target remains to make the Swing client playable, readable, and safe to publish while preserving the server-authoritative world lane already introduced through prior 0.9.10 server/persistence work. The sector manager, internal-server single-writer lane, immutable `WorldSnapshot` publishing, and separate single-player/server save namespaces are treated as bridged unless explicitly reopened.

### Allowed now

- Phase 0 governance and build hygiene.
- Phase 4 UI, input, rendering, accessibility, display, graphics, options, and presentation containment.
- Documentation cleanup that improves handoff clarity without inventing new document sprawl.
- Packaging and publish-safe hygiene for the installer -> thin launcher -> client/server execution path.
- Narrow authority extraction when touching a UI/input/render/package subsystem.
- Small persistence/server-boundary repairs required to keep the bridged single-player runtime safe.
- Milestone-index, legacy-source-map, and Codex-handoff preparation.

### Not active now

- Broad worldgen expansion, except as documentation planning already captured in ordered milestones.
- Full implementation of the ordered milestone simulation systems.
- Live external mod loading, source acquisition, classpath mutation, hot restart, public multiplayer networking, or real remote server authority.
- True per-sector parallel world mutation while shared legacy `World` state remains unsplit.
- New standalone Markdown notes or random pass documents.

## Current publish-safe gates

Each gate should leave the repository buildable and reviewable. Do not start a later gate by expanding scope; finish the smallest publish-safe version of the current gate first.

### Gate 1 - Documentation and repository hygiene

Required outcomes:

- `MASTER_DEVELOPMENT_PLAN.md` remains concise and points to `MILESTONE_INDEX.md` for detailed phase-group planning.
- `MILESTONE_INDEX.md` lists the ordered `00` through `10` milestone sequence.
- `LEGACY_MILESTONE_SOURCE_MAP.md` maps older topical files to ordered milestone homes.
- Standards and governance remain recognized constraints for all future code/package work.
- Legacy topical milestone files are preserved as source/archive material until cleanup verification approves movement or deletion.
- README remains user-facing and does not become roadmap/changelog/standards content.
- Root-level command files and scripts remain partitioned into client, launcher, installer, server, tooling, or docs locations as appropriate.

Done when: a new developer or Codex session can identify the active checkpoint, ordered milestones, standards, governance, source-map boundary, and next pass without reading the old topical sprawl.

### Gate 2 - Bootstrap and package identity hygiene

Required outcomes:

- The intended execution path remains installer -> thin launcher -> client/server payloads.
- The installer installs the thin launcher and only the launcher support set required to start, diagnose, acquire manifests, verify payloads, and manage updates.
- The thin launcher acquires and verifies client, server, Java runtime image when bundled, LWJGL/native graphics libraries when required, controller/input libraries, Netty/runtime libraries when used, and other support libraries before launching the client.
- The client must not opportunistically download support libraries at game launch.
- Thin launcher and installer must not require the full repository, main client jar, server jar, main asset tree, optional packs, or subsidiary main-project files before bootstrap.
- Client/server package identity, checksums, versions, and compatibility constraints belong in launcher manifests rather than loose repo layout inference.

Done when: the bootstrap chain can be reasoned about without downloading the entire development repo just to start the thin launcher.

### Gate 3 - UI containment and targeting readiness

Required outcomes:

- High-traffic UI surfaces keep controls inside owning frames.
- Body copy wraps and clips inside owning panels.
- Mouse, keyboard, and future gamepad targeting consume shared viewport/targeting transforms.
- Look, interaction, combat, inventory, build, transition, dossier, and options surfaces avoid raw IDs and placeholder strings.
- Presentation layers do not mask the gameplay state they describe.

Done when: Phase 4 presentation work remains player-readable, old-machine viable, and authority-based.

### Gate 4 - Build, Java 17, and smoke

Required outcomes:

- Compile from source before delivery.
- Rebuild runnable client and server jars before packaging.
- Verify Java 17 classfile compatibility; no packaged classfile may exceed major version 61.
- Run targeted smoke tests tied to touched subsystem.
- Rebuild zip only after compile, jar rebuild, launcher/preflight smoke, and classfile scan have passed.
- State honestly what was not manually tested.

Done when: a delivered artifact is reproducible, Java 17-compatible, and verified beyond a local newer-JDK smoke.

## Ordered milestone sequence

Detailed phase-group expansions are maintained in the ordered milestone sequence indexed by `docs/MILESTONE_INDEX.md`. The master plan intentionally does not duplicate those documents.

Current ordered milestones:

- `MILESTONE_00_GOVERNANCE_PACKAGE_AND_INDEX.md`
- `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md`
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md`
- `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md`
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md`
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md`
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md`
- `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md`
- `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md`
- `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md`
- `MILESTONE_10_PERSISTENCE_SAVE_SCHEMA_MIGRATION_AND_BACKUPS.md`

Use `MILESTONE_INDEX.md` to choose the correct detailed milestone for any future pass. Use `LEGACY_MILESTONE_SOURCE_MAP.md` only when tracing old topical source material.

## Global dependency order

The ordered milestone sequence preserves these major dependency rules:

- Asset readiness precedes broad content placement.
- UI readability and Infopedia reference support should precede or accompany systems the player must understand.
- Knowledge-quality fabrication, machine knowledge, skills, and item quality precede deep production claims.
- Population provenance precedes item provenance.
- Item provenance precedes robust economy and faction markets.
- Deferred out-of-sector simulation and top-down world events consume provenance, faction strength, influence, routes, economy, and persistence rules.
- Vehicle/component provenance precedes vehicle factories, vehicle deployment, and strategic vehicle power.
- Comparative damage/durability scale precedes vehicle, machine, wall, mounted-weapon, and structural combat integration.
- Construction blueprints, access rules, ownership, and parity precede broad player/faction construction claims.
- Ages of Control and leadership schemes consume population, item/facility, vehicle, and room-control provenance.
- Scheme quests require timing windows, target guidance, reliable evidence, missed-window standing rules, and Quest Editor validation.
- Medical, cybernetic, prosthetic, narcotic, and body systems require readable body-state, treatment, item-quality, facility, and Infopedia support.
- Worldgen/district/room-stamp expansion consumes Asset Tools output, semantic assets, provenance systems, faction identity, Ages of Control, top-down world events, and room/facility audit surfaces.
- Persistence/save ownership applies to every milestone before publish-safe claims: world truth to world save, player-character truth to player save, settings to profile/settings, launcher state to launcher/cache manifests, and editor-authored definitions to versioned content definitions.

## Quality-of-life and sensibility evaluator

Every major implementation pass must answer these questions before and after the pass:

- What does this make easier, clearer, safer, more interesting, or more meaningful for the player?
- Is the result visible to the player, visible only through diagnostics/audit, or entirely internal?
- What information is displayed, and why does the player need it at that moment?
- What internal IDs, debug residue, duplicate state, or noise should be hidden?
- Does the action make sense from player command to system response to feedback to save/load persistence?
- Are failure reasons honest and useful?
- Are placeholder labels, raw IDs, Java class names, registry handles, manifest keys, and package names hidden from ordinary UI?
- Does the pass preserve old-machine performance and avoid hot-loop string churn, asset bloat, and needless redraws?
- Does the result remain publish-safe?

No major pass may claim completion if player comprehension, publish safety, save/load safety, or package integrity has an unresolved negative answer.

## Codex handoff protocol

Codex handoffs must be narrow, testable, and tied to one ordered milestone or one publish-safe gate at a time. Do not hand Codex the entire roadmap as a single task.

A valid handoff includes:

- Active checkpoint.
- Target ordered milestone or gate.
- Exact files/packages to inspect first.
- Expected runtime behavior.
- Non-goals.
- Verification steps.
- Compile/build expectations.
- Documentation update targets.
- Honest limitation notes.

### Recommended first Codex handoff

The safest first handoff after this cleanup is documentation/package alignment rather than simulation expansion:

1. Read `MASTER_DEVELOPMENT_PLAN.md`, `STANDARDS_AND_PRACTICES.md`, `MASTER_GOVERNANCE_REVISION_II.md`, `DEVELOPMENT_HISTORY.md`, `MILESTONE_INDEX.md`, and `LEGACY_MILESTONE_SOURCE_MAP.md`.
2. Verify `MILESTONE_00` through `MILESTONE_10` exist.
3. Verify legacy topical milestone files are not treated as primary entry points.
4. Verify bootstrap/package docs and scripts still reflect installer -> thin launcher -> client/server.
5. Verify thin launcher and installer do not depend on the full client/server payload or full project tree before manifest acquisition.
6. Report exact files touched, exact verification run, and anything not tested.

After that, the first runtime-support slice should be one of:

- Milestone 01: asset/semantic audit support needed by later worldgen.
- Milestone 02: UI/readability support for player-facing inspection, controls, movement, Look/Examine groundwork, and Infopedia reference hygiene.
- Milestone 10: persistence ownership and save/load boundary support required before large systems write durable state.

Do not begin Milestone 09 worldgen expansion as the first Codex runtime task. It depends on assets, readability, provenance, parity, vehicle, scheme, medical/body, world-event, and persistence foundations.

## Reviewed constraints from Standards and Governance

This cleanup was prepared against these standing constraints:

- Build and delivery remain Java 17, compile-from-source, jar rebuild, classfile scan, smoke, and zip integrity gated.
- Runtime separation remains installer -> thin launcher -> client -> server.
- The thin launcher owns acquisition, verification, update, rollback, and launch; the client does not mutate its own classpath by downloading support libraries at startup.
- LWJGL and other graphical/native dependencies must be treated as shipped runtime assets, not just Maven declarations.
- UI/player-facing surfaces must avoid raw IDs, placeholder text, phase labels, and debug excuses.
- New graphical references route toward the Semantic Asset Registry and Asset Tools outputs rather than scattered direct file paths.
- Sector/server authority remains closed to public networking and live external mod loading until explicit architecture, safety, package, and test gates exist.
- Simulation depth must scale by relevance: immediate reality near the player, operational ledgers for local districts, and strategic/deferred summaries for distant systems.
- Asset files, atlas cells, and placeholders are not implementation until semantic runtime identity, consuming authority, inspection/use meaning, placement/generation path, and fallback behavior exist.
- Every persistent system must define world/player/profile/launcher-cache/editor-content ownership before publish-safe save/load claims.

## Current handoff status

Milestone consolidation is complete enough for Codex handoff preparation.

Completed handoff-prep state:

- `MILESTONE_00` through `MILESTONE_10` exist.
- `MILESTONE_INDEX.md` points to the ordered sequence.
- `LEGACY_MILESTONE_SOURCE_MAP.md` maps stale topical files to canonical ordered homes.
- This master plan points to `MILESTONE_INDEX.md` instead of duplicating large milestone detail.
- Obsolete candidate/topical roadmap duplication has been removed from this master plan.
- Legacy topical milestone files are preserved as source/archive material pending final cleanup approval.

Remaining before first implementation handoff:

- Optionally add a short entry to `DEVELOPMENT_HISTORY.md` noting milestone consolidation, source-map cleanup, and master-plan refresh.
- Optionally move legacy topical files into an archive folder after user approval and verification that no unique requirements remain only in legacy files.
- Select the first narrow Codex task.

## Anti-drift directive

When uncertain, do not create another document and do not expand scope. Read the master plan, standards, governance, history, milestone index, and legacy source map. Pick one gate or one ordered milestone slice. Make the smallest compile-safe, smoke-testable implementation that advances the active checkpoint.
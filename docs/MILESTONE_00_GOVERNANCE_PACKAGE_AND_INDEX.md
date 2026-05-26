# Milestone 00 - Governance, Package, and Milestone Index

This ordered milestone document is the capstone and navigation rule for the ordered milestone set. It explains how the project should read, maintain, and hand off the roadmap now that the former topical milestone sprawl has been reorganized into phase-aligned implementation targets.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This document explains how to use the milestone sequence without turning it into a second ungoverned planning pile.

This document is not a changelog. Completed implementation belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Purpose

The milestone system exists because `MASTER_DEVELOPMENT_PLAN.md` became too large for safe detailed editing. The ordered milestones provide implementation-depth planning while preserving the master plan as the index and authority map.

The goal is not to create more documents for their own sake. The goal is to make each large implementation lane easier to hand to Codex, easier to audit, easier to sequence, and safer to update without replacing a giant monolithic roadmap file.

## Document authority order

When planning or implementing work, read documents in this order:

1. `docs/MASTER_DEVELOPMENT_PLAN.md` - authoritative phase map, active checkpoint, dependencies, and roadmap boundaries.
2. `docs/STANDARDS_AND_PRACTICES.md` - durable implementation, build, packaging, and coding rules.
3. `docs/MASTER_GOVERNANCE_REVISION_II.md` - high-level governance and doctrine.
4. `docs/DEVELOPMENT_HISTORY.md` - completed work and historical implementation facts.
5. Ordered milestone documents - detailed phase-aligned implementation targets.
6. Topical legacy milestone/supplement files - source material only until absorbed, not preferred implementation entry points.

If documents conflict, do not guess. Prefer the master plan for phase/dependency authority, standards for implementation rules, governance for doctrine, and the newest ordered milestone for detailed implementation targeting unless the user explicitly overrides.

## Ordered milestone sequence

The current ordered milestone sequence is:

- `MILESTONE_00_GOVERNANCE_PACKAGE_AND_INDEX.md` - governance, package, milestone reading order, anti-sprawl, and Codex handoff rules.
- `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md` - semantic asset IDs, asset promotion, Asset Tools integration, publish-safe clearance, optional packs, and content readiness.
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` - Look/Examine, UI readability, Infopedia depth, quest guidance, pet feedback, and player-facing clarity.
- `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md` - knowledge-quality fabrication, machine/facility knowledge, item quality, factional mutations, and skill tree.
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` - population provenance, item provenance, economy, vendors, illicit markets, noble luxury, and draught goods.
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` - construction parity, blueprints, access, ownership, permits, licenses, and player expansion heat.
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md` - vehicles, body schemas, factories, structural damage scale, road/parking limits, and strategic vehicle power.
- `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md` - Ages of Control, active faction schemes, quest lifecycles, leadership journals, and intelligence sale.
- `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md` - body states, medicine, narcotic effects, surgery, prosthetics, cybernetics, and treatment systems.
- `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md` - worldgen, districts, room stamps, facility stamps, faction theming, and Asset Tools-driven spatial integration.

## Active checkpoint discipline

The active checkpoint from the master plan remains binding until explicitly changed.

Current checkpoint context:

- Publish-safe client containment remains the near-term priority.
- Work must keep the Swing client playable, readable, and safe to package.
- The launcher/client/server packaging path remains installer -> thin launcher -> client/server payloads.
- The thin launcher and installer must remain independent of the full main project tree.
- Broad implementation from future milestones should not begin until the current publish-safe and packaging gates are ready or the user explicitly reopens that lane.

Ordered milestones are implementation targets, not permission to ignore the active checkpoint.

## Anti-sprawl rule

Do not create new milestone files unless the user explicitly orders a new phase-aligned document or a genuinely new implementation lane cannot fit into the existing ordered sequence.

New ideas should normally be added to the correct ordered milestone:

- Assets, semantic IDs, optional packs, publish-safety -> Milestone 01.
- UI, inspection, Infopedia, player-facing readability -> Milestone 02.
- Production, knowledge, skills, quality, factional production mutations -> Milestone 03.
- Population, provenance, vendors, markets, luxury, narcotics economy, draught trade -> Milestone 04.
- Construction, blueprints, ownership, access, parity, heat -> Milestone 05.
- Vehicles, structural scale, vehicle factories, road/parking, strategic assets -> Milestone 06.
- Ages of Control, schemes, quests, leadership journals, intelligence sale -> Milestone 07.
- Medicine, body states, cybernetics, prosthetics, narcotic effects -> Milestone 08.
- Worldgen, districts, room/facility stamps, faction theming -> Milestone 09.

If a new note does not clearly belong anywhere, add a short planning note to `MASTER_DEVELOPMENT_PLAN.md` only if it affects phase authority, or ask for user direction before creating another document.

## Topical legacy file handling

The first wave of topical milestone files and supplements remains valuable source material, but the ordered milestone files are now the preferred implementation entry points.

Topical files should not be deleted casually. Treat them as legacy source files until:

1. Their content has been absorbed into ordered milestones.
2. The ordered milestone names the source file it absorbed.
3. `MILESTONE_INDEX.md` or the master plan references the ordered structure.
4. The user explicitly approves archival, deprecation, or cleanup.

Potential future cleanup states:

- Keep as source archive.
- Mark as superseded by ordered milestone.
- Move to an archive folder.
- Delete after verification.

Do not perform cleanup without explicit user approval.

## Packaging and publish-safe gates

All milestone implementation must preserve package safety.

Required gates:

- Java 17 compile must remain clean.
- Client and server jars must remain reproducible.
- Installer/thin-launcher bootstrap independence must remain intact.
- Thin launcher must not require the full project tree.
- Main client/server payloads should be acquired through manifest/update flow rather than bundled into the launcher.
- Low_32 lean assets remain the baseline runtime art tier.
- Optional higher-quality art packs must declare semantic overrides rather than raw path hacks.
- Public package assets, names, likenesses, lore terms, and labels must be publish-safe or quarantined.
- Player-facing strings must avoid raw IDs and placeholder labels.

## Asset and UI safety rule

Assets and UI are recurring release risks.

Before any milestone claims completion, verify:

- Player-facing labels are clear.
- Raw internal IDs are hidden from ordinary UI.
- Placeholder text is not accidentally shipped as final.
- Asset IDs are semantic where needed.
- Direct path fallback is audited.
- Publish-safety status is known for shipped assets.
- Infopedia or inspection surfaces explain exposed mechanics.

## Codex handoff rule

A Codex handoff should be a bounded implementation request, not a dump of every milestone.

A useful handoff should include:

- Active checkpoint.
- Ordered milestone document relevant to the current implementation slice.
- Exact files or packages to inspect first.
- Expected runtime behavior.
- Non-goals.
- Verification steps.
- Compile/build command expectations.
- Documentation update targets.

Codex should not be asked to implement the entire ordered milestone sequence at once. Each pass should target one small runtime slice with compile/smoke expectations.

## Recommended first Codex handoff after milestone consolidation

The safest first handoff should not be a giant worldgen implementation. It should be a documentation and packaging alignment pass:

1. Update `MILESTONE_INDEX.md` to point to the ordered milestone sequence.
2. Add a small reference in `MASTER_DEVELOPMENT_PLAN.md` pointing to `MILESTONE_INDEX.md` rather than listing every milestone file.
3. Add supersession notes to topical milestone files only if explicitly approved.
4. Verify no new root-level or random `docs/` sprawl was introduced beyond the ordered milestone set.
5. Confirm the repo still treats the master plan, standards, governance, and development history as durable authority documents.

After that, a safe runtime handoff would likely begin with Milestone 02 or Milestone 01 because UI/readability and asset audit work support later systems without prematurely expanding simulation complexity.

## Verification checklist

Before calling the ordered milestone consolidation complete, verify:

- Milestone 00 through 09 exist.
- `MILESTONE_INDEX.md` lists the ordered sequence.
- `MASTER_DEVELOPMENT_PLAN.md` references `MILESTONE_INDEX.md` as the milestone navigation surface.
- Topical source files are not treated as primary implementation entry points.
- The active checkpoint remains visible.
- Package/publish-safe gates remain visible.
- The next Codex handoff is narrow and testable.

## Non-goals

This document does not implement any runtime feature.

This document does not replace the master plan.

This document does not authorize deleting topical milestone files.

This document does not change the active checkpoint by itself.

This document does not permit uncontrolled documentation sprawl.

## Deferred checkpoint summary

The milestone system should now function as a phase-aligned implementation map. The master plan remains the authority. The ordered milestones provide depth. The index provides navigation. Topical files become source material until explicitly cleaned up. Codex handoffs should be narrow, compile-safe, and tied to one ordered milestone at a time.
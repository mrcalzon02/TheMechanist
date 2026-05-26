# Milestone Index and Ordered Phase-Aligned Map

This document is the navigation surface for the ordered milestone system. It replaces the earlier candidate/topical milestone map with the current phase-aligned `00` through `09` sequence.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is the milestone navigation index. Individual ordered milestone files carry detailed implementation targets for large phase groups.

This document is not a changelog. Completed implementation belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Document authority rule

Read planning and implementation documents in this order:

1. `MASTER_DEVELOPMENT_PLAN.md` - roadmap authority, active checkpoint, phase boundaries, and dependency map.
2. `STANDARDS_AND_PRACTICES.md` - durable implementation, build, packaging, coding, and release-safety rules.
3. `MASTER_GOVERNANCE_REVISION_II.md` - high-level governance and doctrine.
4. `DEVELOPMENT_HISTORY.md` - completed implementation history.
5. Ordered milestone documents - detailed phase-aligned implementation targets.
6. Legacy topical milestone and supplement files - source/archive material until fully absorbed and explicitly cleaned up.

If documents conflict, do not guess. Prefer the master plan for roadmap authority, standards for implementation rules, governance for doctrine, and the newest ordered milestone for detailed implementation targeting unless the user explicitly overrides.

## Ordered milestone sequence

### `MILESTONE_00_GOVERNANCE_PACKAGE_AND_INDEX.md`

Focus:

- Milestone reading order.
- Anti-sprawl rules.
- Package and publish-safe gates.
- Active checkpoint discipline.
- Codex handoff rules.
- Legacy milestone cleanup policy.

Primary function:

This is the capstone and rulebook for using the ordered milestone set without creating a second unmanaged planning pile.

### `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md`

Focus:

- Semantic asset IDs.
- Asset Tools integration.
- Unused asset discovery.
- Publish-safe asset clearance.
- World-usable asset promotion.
- Optional art packs.
- Low_32 lean runtime tier.
- Agriculture, animal, pet, noble, draught, vehicle, quality, and blueprint-ready asset definitions.

Primary phases:

- Phase 2.
- Phase 18 asset/editor portions.
- Phase 19 asset release-audit portions.

### `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md`

Focus:

- `Look` versus `Examine`.
- Intelligence-based entity examination.
- Refreshed intent and state checks.
- Character-sheet-like examination screens.
- Body-plan and injury approximation.
- Quest objective arrows and pulsing highlights.
- Pet interaction feedback.
- Access, ownership, construction, and market denial messages.
- Infopedia hot linking and deep entries.

Primary phases:

- Phase 4.
- Phase 14 readability portions.
- Phase 15 biography/social identity portions.
- Phase 17 quest/pet/economy readability portions.
- Phase 18 Infopedia/editor portions.
- Phase 19 UI/readability audits.

### `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md`

Focus:

- Knowledge-quality fabrication.
- Machine and facility knowledge.
- Blueprint and schematic quality.
- Skill tree separate from Knowledge Tree.
- XP-based skill progression.
- Item quality bands.
- Production inspection and forecasting.
- Quality provenance.
- Factional production mutations.
- Batch defects, counterfeit, and contamination.

Primary phases:

- Phase 6.
- Phase 9 quality provenance portions.
- Phase 15 skill tree portions.
- Phase 17 trainer/specialist portions.
- Phase 18 editor/Infopedia portions.
- Phase 19 production-quality audits.

### `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md`

Focus:

- Population provenance.
- Workforce identity.
- Item provenance.
- Supply ecology.
- Faction vendors.
- Critical suppliers.
- Food, water, weapons, ammunition, medical, construction, animal, and pet markets.
- Illicit narcotics faction production and sale.
- Noble luxury, narcotics, and draught trade.
- Rare off-world draught items as extremely valuable vault goods.

Primary phases:

- Phase 8.
- Phase 9.
- Phase 16 vendor/facility placement portions.
- Phase 17 economy/vendor portions.
- Phase 18 editor/Infopedia portions.
- Phase 19 economy/provenance audits.

### `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md`

Focus:

- Player/faction construction parity.
- Room and asset blueprints.
- Blueprint legality and access.
- Permits, licenses, restricted plans, stolen plans, black-market plans.
- Ownership categories.
- Construction validation and blueprint execution.
- Player expansion heat and suspicion.
- Blueprint vendors and representatives.
- Symmetric faction acquisition.

Primary phases:

- Phase 7.
- Phase 12.
- Phase 16 construction/vendor placement portions.
- Phase 17 blueprint economy and expansion heat portions.
- Phase 18 editor/Infopedia portions.
- Phase 19 parity/construction audits.

### `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md`

Focus:

- Vehicle classes and variants.
- Vehicle component schemas.
- Vehicle integrity and endurance.
- Manufacturers, models, and variants.
- Vehicle factories and component factories.
- Vehicle provenance ledgers.
- Vehicle ownership and access.
- Road, parking, garage, depot, and sidewalk movement constraints.
- Mounted weapon scale.
- Structural target scale.
- APCs and tanks as sector-level balance-of-power assets.

Primary phases:

- Phase 10.
- Phase 14 vehicle/combat/structural portions.
- Phase 16 vehicle facility portions.
- Phase 17 vehicle doctrine/contracts portions.
- Phase 18 vehicle editor/Infopedia portions.
- Phase 19 vehicle/structural audits.

### `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md`

Focus:

- Ages of Control.
- Historical zone generation.
- Leadership schemes.
- Room control changes.
- Visible active scheme events.
- Planning, execution, and cooldown quest lifecycles.
- Minimum two-day active quest windows.
- Non-synchronized activation times.
- Zero-weight quest-critical items.
- Proof-of-death evidence.
- Leadership journals and sellable intelligence.

Primary phases:

- Phase 11.
- Phase 16 Ages-of-Control/worldgen integration.
- Phase 17 scheme quests/intelligence sale portions.
- Phase 18 editor/Infopedia portions.
- Phase 19 scheme/quest/intelligence audits.

### `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md`

Focus:

- Body-state simulation.
- Wounds, pain, bleeding, infection, shock.
- Field treatment.
- Clinic treatment.
- Surgery.
- Recovery, treatment risk, and failure.
- Prosthetics.
- Cybernetics.
- Prosthetic/cybernetic quality and malfunction.
- Medical skills.
- Narcotics and drug body effects.
- Addiction/dependency framework if implemented later.

Primary phases:

- Phase 9 medical item/provenance portions.
- Phase 14 body/health/medical/cybernetic portions.
- Phase 15 body identity and skill portions.
- Phase 17 clinic/vendor/trainer portions.
- Phase 18 editor/Infopedia portions.
- Phase 19 medical/body audits.

### `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md`

Focus:

- Asset Tools-driven worldgen asset discovery.
- Semantic asset palettes.
- Zone families.
- District stamps.
- Room stamps.
- Facility stamps.
- Road, alley, parking, frontage, sewer, and maintenance corridors.
- Residential, market, industrial, medical, security, civic, temple, noble, illicit, agriculture, animal, pet, vehicle, hazard, and utility spaces.
- Faction theme integration.
- Vendor, NPC, workforce, container, loot, journal, and evidence anchors.
- Ages of Control room mutation.
- Active scheme room events.

Primary phases:

- Phase 2 asset readiness portions.
- Phase 8/9/10/11 provenance dependencies.
- Phase 12 construction/ownership context.
- Phase 16 worldgen/stamp implementation.
- Phase 17 economy/vendor/pet/quest context.
- Phase 18 editor/Infopedia portions.
- Phase 19 worldgen/stamp audits.

## Legacy topical files and supplements

The following topical files remain source material until explicitly archived, deprecated, or deleted by user approval:

- `MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md`
- `MILESTONE_EXAMINATION_AND_INFOPEDIA_SYSTEMS.md`
- `MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md`
- `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md`
- `MILESTONE_SUPPLEMENT_ILLICIT_NARCOTICS_FACTION_PRODUCTION.md`
- `MILESTONE_SUPPLEMENT_NOBLE_LUXURY_NARCOTICS_AND_DRAUGHT_TRADE.md`
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`

Current status:

- Their key planning content has been aligned into the ordered milestone sequence.
- They should not be treated as the primary implementation entry point.
- They should not be deleted without explicit user approval.
- A later cleanup pass may mark them as superseded, move them to an archive folder, or delete them after verification.

## Master-plan patch target

The next safe master-plan edit should add a compact reference to this index rather than listing every milestone file.

Suggested master-plan reference:

```text
Detailed phase-group expansions are maintained in the ordered milestone sequence indexed by `docs/MILESTONE_INDEX.md`. The master plan remains the authoritative roadmap and phase map; ordered milestones provide implementation-depth targets for Codex and later development passes.
```

This keeps `MASTER_DEVELOPMENT_PLAN.md` from becoming a giant list of every milestone document.

## Anti-sprawl rule

Do not create new milestone files unless the user explicitly orders a new phase-aligned document or a genuinely new implementation lane cannot fit into the ordered sequence.

Add future notes to the correct ordered milestone whenever possible:

- Assets and publish-safety -> Milestone 01.
- UI, inspection, Infopedia, readability -> Milestone 02.
- Production, knowledge, skills, item quality -> Milestone 03.
- Population, provenance, markets, vendors, luxury, draught, economy -> Milestone 04.
- Construction, blueprints, ownership, access, parity, heat -> Milestone 05.
- Vehicles, structural scale, motor pools, road/parking, strategic assets -> Milestone 06.
- Ages, schemes, quests, journals, intelligence sale -> Milestone 07.
- Medicine, body states, cybernetics, prosthetics, drug effects -> Milestone 08.
- Worldgen, districts, room/facility stamps, faction theming -> Milestone 09.

## Codex handoff rule

Codex handoffs should be narrow, testable, and tied to one ordered milestone at a time.

A handoff should include:

- Active checkpoint.
- Target ordered milestone.
- Exact files/packages to inspect first.
- Expected runtime behavior.
- Non-goals.
- Verification steps.
- Compile/build expectations.
- Documentation update targets.

Do not hand Codex the entire milestone sequence as one implementation request. That creates a giant untestable instruction cloud instead of a safe development pass.

## Current recommended next pass

The next recommended documentation pass is:

1. Add the compact `MILESTONE_INDEX.md` reference to `MASTER_DEVELOPMENT_PLAN.md`.
2. Prepare a Codex handoff brief that points Codex to the master plan, standards/practices, governance, development history, and this ordered milestone index.
3. Keep the first Codex task narrow, preferably documentation/package alignment or a small Milestone 01/02 runtime-support slice.

## Verification checklist

Before declaring milestone consolidation complete, verify:

- `MILESTONE_00_GOVERNANCE_PACKAGE_AND_INDEX.md` exists.
- `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md` exists.
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` exists.
- `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md` exists.
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` exists.
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` exists.
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md` exists.
- `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md` exists.
- `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md` exists.
- `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md` exists.
- `MASTER_DEVELOPMENT_PLAN.md` references this index.
- Topical files are preserved until cleanup approval.
- The next Codex handoff is narrow and testable.

## Deferred checkpoint summary

The milestone index is now the ordered navigation surface for detailed planning. The master development plan remains the authority. Ordered milestones provide implementation depth. Legacy topical files remain source material until cleanup is explicitly approved. Future work should patch the correct ordered milestone rather than creating new scattered planning files.
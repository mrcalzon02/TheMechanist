# Milestone - Roadmap Reorganization and Phase Alignment

This milestone document defines the planned rework of the current milestone-document system. It exists because the milestone split solved the immediate master-plan monolith problem, but the first wave of milestone files is now beginning to create a second-order navigation problem: too many files that must be checked individually.

The goal is not to delete detail. The goal is to reorganize detail into ordered, phase-aligned milestone documentation targets so future development can follow one coherent path without searching across scattered supplements.

## Problem statement

`MASTER_DEVELOPMENT_PLAN.md` became too large to safely edit directly for every new detailed system. Milestone documents were introduced to prevent unsafe full-document replacement. That was the correct containment move.

However, the current milestone set is still functionally grouped by topic rather than ordered by development sequence. As more supplements are added, a developer must remember to check the master plan, milestone index, several milestone files, and multiple supplements. That creates the same kind of drift risk the split was meant to solve.

The next documentation pass should reorganize the milestone system into phase-aligned increments.

## Target structure

The long-term target is a small set of ordered milestone documents that follow implementation sequence.

Candidate ordered milestone documents:

### `MILESTONE_00_GOVERNANCE_PACKAGE_AND_INDEX.md`

Purpose:

- Master-plan index rules.
- Milestone document rules.
- Package/readme/root hygiene.
- Active checkpoint references.
- Release-safe documentation ownership.

Absorbs or references:

- Current `MILESTONE_INDEX.md`.
- Anti-sprawl and milestone partition rules.
- Master-plan patch target notes.

### `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md`

Purpose:

- Semantic asset IDs.
- Asset promotion.
- Agriculture/animal/pet assets.
- Luxury/draught assets.
- Blueprint-ready asset metadata.
- Optional packs and publish-safe clearance.

Phase focus:

- Phase 2.
- Phase 18 asset/editor portions.
- Phase 19 asset audit portions.

### `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md`

Purpose:

- Look and Examine.
- Intelligence-based entity analysis.
- Quest guidance overlays.
- Pet interaction feedback.
- Infopedia hot linking.
- Player-facing compact clarity.

Phase focus:

- Phase 4.
- Phase 14 condition readability portions.
- Phase 15 biography portions.
- Phase 17 quest/pet/ownership readability portions.
- Phase 18 Infopedia portions.
- Phase 19 UI/readability audits.

### `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md`

Purpose:

- Knowledge-quality fabrication.
- Machine knowledge.
- Blueprint quality.
- Skill tree.
- Factional production mutation.
- Item quality provenance.

Phase focus:

- Phase 6.
- Phase 9 quality provenance portions.
- Phase 15 skill tree.
- Phase 17 trainers/specialists.
- Phase 18 editor/audit.
- Phase 19 release audit.

### `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md`

Purpose:

- Population provenance.
- Workforce identity.
- Item provenance.
- Faction vendors.
- Critical vendors.
- Noble luxury/narcotics/draught economy.
- Illicit narcotics economy.
- Faction market stock provenance.

Phase focus:

- Phase 8.
- Phase 9.
- Phase 16 vendor/facility placement portions.
- Phase 17 economy/vendor portions.
- Phase 18 editor/audit.
- Phase 19 release audit.

### `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md`

Purpose:

- Player/faction parity.
- Room blueprints.
- Construction parity.
- Access, legality, licenses, permits.
- Player expansion heat.
- Ownership and faction reaction.

Phase focus:

- Phase 7.
- Phase 12.
- Phase 16 construction/vendor facility portions.
- Phase 17 blueprint economy and expansion heat.
- Phase 18 editor/audit.
- Phase 19 release audit.

### `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md`

Purpose:

- Vehicle provenance.
- Vehicle factories.
- Vehicle ownership.
- Body schemas.
- Damage/repair.
- Road/parking constraints.
- Mounted weapon and structural scale.
- Sector-level power.

Phase focus:

- Phase 10.
- Phase 14 vehicle/structural combat portions.
- Phase 16 vehicle facility portions.
- Phase 17 vehicle contract portions.
- Phase 18 editor/audit.
- Phase 19 release audit.

### `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md`

Purpose:

- Ages of Control.
- Zone-history generation.
- Leadership schemes.
- Visible active schemes.
- Scheme quest lifecycle.
- Timing windows.
- Objective evidence.

Phase focus:

- Phase 11.
- Phase 16 Ages-of-Control integration.
- Phase 17 scheme quests and evidence.
- Phase 18 editor/audit.
- Phase 19 release audit.

### `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md`

Purpose:

- Injury and treatment.
- Medical items.
- Cybernetics.
- Prosthetics.
- Narcotic/medicine effects.
- Addiction/dependency risk where appropriate.
- Medical vendors and facilities.

Phase focus:

- Phase 9 medical item provenance.
- Phase 14 body/health/medicine/cybernetics.
- Phase 17 medical economy/trainers/vendors.
- Phase 18 editor/audit and Infopedia.
- Phase 19 release audit.

### `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md`

Purpose:

- District stamps.
- Farms/gardens/cloning/animal/pet rooms.
- Noble estates/vaults.
- Markets/clinics/armories/vendors.
- Temples/civic facilities.
- Faction compounds.
- Facility placement from provenance and Ages of Control.

Phase focus:

- Phase 16.
- Phase 2 asset prerequisites.
- Phase 8/9/10/11 provenance prerequisites.
- Phase 19 worldgen release audit.

## Reorganization method

The reorganization must be incremental and safe. Do not perform a giant delete-and-rewrite pass.

Recommended process:

1. Freeze creation of new topical milestone files unless the user explicitly orders a new one.
2. Create the ordered target milestone files one at a time.
3. Move or summarize existing topical milestone content into the correct ordered document.
4. Preserve original topical files until their content has been absorbed and cited in the ordered replacement.
5. Add a status note to absorbed topical files stating which ordered milestone supersedes them.
6. Update `MILESTONE_INDEX.md` to point at the ordered sequence.
7. Add a small master-plan index patch pointing to `MILESTONE_INDEX.md` only, rather than listing every milestone file in the master plan.
8. After the ordered sequence is stable, optionally archive or deprecate the older topical files only if the user explicitly approves.

## Current topical files to align

Current topical files and likely destination:

- `MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` -> mostly `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md`, `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md`, `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md`, and `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md`.
- `MILESTONE_EXAMINATION_AND_INFOPEDIA_SYSTEMS.md` -> `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md`.
- `MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md` -> `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` plus vendor portions in `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md`.
- `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md` -> `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md` and `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md`.
- `MILESTONE_SUPPLEMENT_ILLICIT_NARCOTICS_FACTION_PRODUCTION.md` -> `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` and `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md`.
- `MILESTONE_SUPPLEMENT_NOBLE_LUXURY_NARCOTICS_AND_DRAUGHT_TRADE.md` -> `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md`, `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md`, and `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md` where item effects apply.

## Rules for ordered milestone documents

Each ordered milestone document should include:

- Phase range and phase mapping.
- Dependency prerequisites.
- Player-facing purpose.
- Data model targets.
- UI/Infopedia targets where relevant.
- Editor/audit targets.
- Release audit targets.
- Deferred checkpoint summary.
- Source topical files absorbed.
- Non-goals.

Each ordered milestone document should avoid:

- Duplicating full text from every source file unless needed.
- Making implementation claims.
- Adding new phase numbers unless mapping back to the master plan.
- Creating new lore or mechanics unrelated to its phase alignment.
- Hiding decisions in prose without audit hooks.

## Immediate next target

The next recommended documentation pass is:

```text
Create MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md
```

Reason:

The newest additions—illicit narcotics, noble luxury/narcotics trade, draught items, faction vendors, critical supplies, and provenance-aware markets—belong to the same economic/provenance/faction-market alignment. Consolidating that first will reduce the most recent sprawl while preserving the newest user intent.

## Deferred checkpoint line - milestone reorganization

The milestone system should be reorganized from topical sprawl into ordered, phase-aligned milestone documents. The master development plan remains the authoritative roadmap and should point to the milestone index, not every individual milestone. The milestone index should become the navigation surface. Existing topical milestone files should be absorbed into ordered milestone documents incrementally, with source/destination notes preserved until the user approves cleanup or archival.

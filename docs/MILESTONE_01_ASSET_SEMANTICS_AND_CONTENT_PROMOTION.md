# Milestone 01 - Asset Semantics and Content Promotion

This ordered milestone document consolidates planning for semantic asset IDs, unused asset discovery, publish-safe asset clearance, world-usable asset promotion, optional art-pack handling, low_32 lean runtime packaging, blueprint-ready asset metadata, agriculture/animal/pet assets, luxury/draught assets, faction-market goods, editor palette promotion, and asset release audits.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the asset semantics/content-promotion slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- `MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md`
- `MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md` where blueprint-ready room and asset definitions are required
- `MILESTONE_SUPPLEMENT_NOBLE_LUXURY_NARCOTICS_AND_DRAUGHT_TRADE.md` where luxury, narcotic, vault, and draught assets require semantic promotion
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` where semantic names, player-facing labels, and Infopedia links depend on stable entries
- `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md` where quality bands, produced item variants, and factional mutations require stable asset categories
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where vendor goods, market fixtures, legal states, and provenance states require stable asset definitions
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` where rooms and assets need blueprint-ready definitions
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 2 - Master asset integration and world-usable asset promotion.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 0 - Governance, documentation, and build/package hygiene.
- Phase 4 - UI/readability surfaces that require player-facing asset labels and no raw ID leakage.
- Phase 6 - Production systems that consume item, machine, tool, recipe, and quality asset definitions.
- Phase 9 - Item provenance and facility origin systems that depend on stable goods, fixtures, and source categories.
- Phase 10 - Vehicle and component assets.
- Phase 12 - Construction and blueprint systems.
- Phase 16 - World generation, room stamps, facilities, estates, farms, animal rooms, and district placement.
- Phase 17 - Economy, vendors, contracts, faction markets, and player acquisition paths.

## Core doctrine

Graphical files are not automatically game content. The project may contain thousands of images, icons, tiles, portraits, overlays, room details, and object graphics, but those files only become durable game assets when they receive semantic identity, ownership, usage context, publish-safe status, and runtime/package rules.

The asset pipeline must distinguish:

- File present.
- File referenced.
- File unused.
- File placeholder.
- File quarantined.
- File publish-safe.
- File tool/editor-only.
- File UI-only.
- File world-usable.
- File optional-pack override.
- File blueprint-ready.
- File eligible for world generation.
- File eligible for Infopedia/reference display.

The game should never depend on fragile direct file paths when a stable semantic ID is required.

## Dependency rule

Asset promotion precedes broad content placement. Agriculture, animal, and pet asset promotion precedes farm/garden/animal/pet room-stamp generation and pet ownership systems. Noble luxury and draught asset promotion precedes noble estate vault placement and draught item systems. Blueprint-ready asset definitions precede construction parity and faction-room blueprint sales. Vehicle component asset promotion precedes vehicle body schemas and factories. Provenance-ready item definitions precede economy/vendor claims.

If an asset cannot be classified, cleared, or given a stable semantic identity, it should remain a candidate or placeholder rather than silently becoming shipped game content.

## Phase 2 - Master asset integration and promotion

### Phase 2.1 - Master asset inventory pass

Identify every graphical asset family available under the client asset tree and optional art-pack inputs.

Inventory categories should include:

- Tiles.
- Floors.
- Walls.
- Roads.
- Sidewalks.
- Corridors.
- Sewer corridors.
- Exterior maintenance corridors.
- Objects.
- Fixtures.
- Machines.
- Items.
- Weapons.
- Ammunition.
- Tools.
- Medical goods.
- Narcotics.
- Luxury goods.
- Draught items.
- Vehicles.
- Vehicle components.
- Portraits.
- Entity sprites.
- Creature sprites.
- Pet sprites.
- Faction art.
- Room-stamp details.
- UI icons.
- Editor icons.
- Overlays.
- Hazards.
- Lighting/visibility cues.
- Objective/highlight cues.
- Sound hook declarations where asset-backed.

Exit criteria:

The project can list what major asset families exist and where they are expected to be consumed.

### Phase 2.2 - Unused asset discovery pass

Generate or maintain an audit that distinguishes assets referenced by code/data from assets present but unused.

Unused assets should be classified as:

- Candidate for promotion.
- Optional-pack candidate.
- Tool/editor-only.
- Preview-only.
- Placeholder.
- Duplicate.
- Superseded by better resolution.
- Quarantined.
- Needs source/license review.
- Dead inventory.

Exit criteria:

Unused assets are no longer invisible; they have a review state and potential destination.

### Phase 2.3 - Publish-safe asset clearance pass

Each asset family must receive a publish-safety status before public packaging.

Clearance states include:

- Approved public.
- Internal development-only.
- Quarantined.
- Needs replacement.
- Needs license/source note.
- Needs original-setting rename.
- Needs likeness review.
- Needs protected-term review.
- Unknown.

No unclear asset should be promoted into a public package without a clearance status.

Exit criteria:

Release packaging can separate approved assets from unsafe, unknown, or development-only material.

### Phase 2.4 - Semantic ID and registry pass

Assign stable semantic IDs and registry entries so game systems reference intent rather than raw paths.

Semantic entries should include:

- Stable semantic ID.
- Player-facing label.
- Asset family.
- Runtime category.
- Source path.
- Quality tier.
- Optional-pack ownership.
- Publish-safe status.
- Valid contexts.
- Invalid contexts.
- Fallback behavior.
- Editor category.
- Infopedia category where relevant.

Exit criteria:

Worldgen, UI, editor, Infopedia, room stamps, item definitions, and package tooling can consume assets through stable IDs.

### Phase 2.5 - World-usable asset promotion pass

Convert appropriate unused or underused graphical assets into usable world-space definitions.

World-usable definitions should record:

- Stable ID.
- Category.
- Dimensions.
- Footprint.
- Render layer.
- Collision assumptions.
- Interaction class.
- Access/ownership expectations.
- Placement constraints.
- Room/facility contexts.
- Damage/durability class where applicable.
- Whether it can appear through worldgen, construction, vendor display, inventory, decoration, or editor placement.

Exit criteria:

Assets become usable by rooms, facilities, items, machines, hazards, UI, and editor surfaces without one-off path hacks.

### Phase 2.6 - Placeholder-to-candidate distinction pass

Placeholder art handles should be treated as candidates, not finished implementation.

A placeholder may appear in diagnostics, editor preview, or internal staging, but must not silently masquerade as release-ready content.

Exit criteria:

The project can distinguish `we have a placeholder` from `this content is implemented and publish-safe`.

## Phase 2 - Specialized asset promotion lanes

### Phase 2.7 - Agriculture, animal, and pet asset promotion pass

Identify and promote farm, garden, crop, hydroponic, mushroom bed, cloning, incubator, animal pen, kennel, cattery, rodent cage, pet zoo, pet vendor, feed bowl, water bowl, leash, toy, bedding, enclosure, animal-sound, pet-sound, and pet-entity assets into semantic registry entries before room stamps or pet systems consume them.

Each promoted pet/animal asset should track:

- Species or category.
- Player-facing label.
- Pettible eligibility.
- Interaction verb family.
- Sound/feedback hook.
- Hostile/non-hostile state compatibility.
- Room/facility context.
- Ownership/care context.

Exit criteria:

Farm, garden, animal, and pet systems can consume promoted assets rather than raw files or decorative labels.

### Phase 2.8 - Noble luxury, narcotics, and draught asset promotion pass

Promote assets for noble luxury, narcotics, rare off-world draught items, sealed containers, estate vault goods, locked cabinets, private medicine stores, ornate bottles, reliquary boxes, imported crates, and house-certified goods.

Draught asset definitions must make clear that draught items are:

- Extremely valuable.
- Rare off-world substances.
- Normally not for sale.
- Valid for locked vaults, sealed cabinets, estate strongrooms, or secured noble estate stamps.
- Potential quest, diplomacy, blackmail, theft, inheritance, and prestige objects.

Exit criteria:

Luxury and draught systems have assets that support value, storage, provenance, and noble estate placement without generic treasure-room treatment.

### Phase 2.9 - Blueprint-ready room and asset definition pass

Promoted room and asset definitions should expose enough metadata to support blueprints, construction parity, and acquisition paths.

Blueprint-ready entries should record:

- Stable semantic ID.
- Player-facing name.
- Room or asset category.
- Footprint.
- Placement constraints.
- Required tiles, walls, doors, fixtures, machines, containers, utilities, and access rules.
- Faction style variants.
- Required materials, tools, skills, knowledge, workforce, and time.
- Whether the player can buy, earn, steal, reverse engineer, salvage, research, or capture the blueprint.
- Whether factions can generate, build, stock, repair, expand, defend, or lose the same asset.

Exit criteria:

Faction room stamps and player construction options can share asset definitions rather than diverging.

### Phase 2.10 - Vehicle and component asset promotion pass

Vehicle and component assets should be promoted before vehicle body schemas, factories, and damage systems depend on them.

Asset categories include:

- Cars.
- Trucks.
- Bikes.
- APCs.
- Tanks.
- Wrecks.
- Chassis.
- Turrets.
- Wheels.
- Tracks.
- Armor plates.
- Engines.
- Transmissions.
- Cargo beds.
- Weapons mounts.
- Sensors.
- Fuel systems.
- Repair parts.
- Component crates.

Exit criteria:

Vehicle systems can consume assets through semantic vehicle and component definitions rather than isolated sprites.

### Phase 2.11 - Quality, variant, and provenance visual pass

Assets should be able to support quality/variant/provenance differences where visually meaningful.

Potential visual distinctions:

- Crude.
- Standard.
- Refined.
- Industrial.
- Military.
- Noble-certified.
- Black-market.
- Counterfeit.
- Damaged.
- Pristine.
- Faction-specific.
- Contaminated or unstable where appropriate.

Not every item requires variant art. The registry should declare whether visual variation exists, is planned, or is intentionally abstracted.

Exit criteria:

Quality and provenance systems know whether an item has visual support or only data/inspection support.

## Runtime tiers, optional packs, and fallback

### Phase 2.12 - Runtime art-tier pass

Preserve low_32 as the lean runtime art tier while standard, intermediate, high, and native-quality art remain optional packs or higher-quality overrides.

Runtime tier definitions should include:

- Core lean assets.
- Optional standard assets.
- Optional intermediate assets.
- Optional high/native assets.
- Override rules.
- Package ownership.
- Memory/startup implications.
- Missing-asset behavior.

Exit criteria:

The default package remains old-hardware viable while optional packs can improve fidelity without changing semantic asset identity.

### Phase 2.13 - Optional art-pack registry pass

Optional art packs should declare overrides, replacements, semantic IDs, quality tier, source, package ownership, and conflict behavior.

The registry should prevent optional packs from silently replacing unrelated assets or mismatching semantic IDs.

Exit criteria:

Optional art packs can be validated before session/world initialization.

### Phase 2.14 - Direct path fallback audit pass

Direct path fallback must be audited rather than silently hiding wrong art.

Fallback should identify:

- Missing semantic ID.
- Requested category.
- Attempted context.
- Requested tier.
- Available fallback.
- Whether the fallback is safe, placeholder, or error-state.

Exit criteria:

Missing or wrong art becomes visible to developer/audit surfaces without polluting ordinary player UI.

### Phase 2.15 - Asset bloat and old-hardware pass

Broad asset promotion must not bloat startup, memory use, packaging size, or hot-loop rendering.

Required safeguards:

- Indexed lookup.
- Lazy loading where appropriate.
- Quality tiers.
- Explicit package ownership.
- Cache limits.
- No repeated hot-loop path scanning.
- No unnecessary high-resolution loading in lean mode.
- Audit output for asset counts and missing references.

Exit criteria:

Asset expansion does not undermine the old-machine viability target.

## Phase 18 - Editor, audit, and Infopedia support

### Phase 18.1 - Editor palette promotion pass

Promoted world assets should appear in editor palettes through semantic categories rather than raw filenames.

Editor palettes should support:

- Room/facility categories.
- Tile categories.
- Object categories.
- Machine categories.
- Item categories.
- Pet/animal categories.
- Vehicle/component categories.
- Noble/luxury/vault categories.
- Hazard/overlay categories.
- Publish-safe filters.
- Placeholder/internal filters.
- Optional-pack filters.

Exit criteria:

The editor can place or inspect assets through meaningful categories rather than fragile file browsing.

### Phase 18.2 - Asset audit surface pass

Editor/audit surfaces should inspect:

- Semantic IDs.
- Source paths.
- Usage contexts.
- Unused status.
- Publish-safe status.
- Optional-pack ownership.
- Quality tier.
- Fallback behavior.
- Blueprint readiness.
- Worldgen readiness.
- Infopedia readiness.
- Placeholder/quarantine status.
- Direct path fallback incidents.

Exit criteria:

Asset state can be audited through owned surfaces rather than manual source inspection.

### Phase 18.3 - Infopedia asset reference pass

Infopedia entries for items, rooms, machines, vehicles, pets, factions, goods, and mechanics should consume semantic asset labels and links.

Infopedia should avoid raw asset IDs unless developer/audit mode is explicitly active.

Exit criteria:

Player-facing reference entries can show asset-backed content using clean labels and hot links.

### Phase 18.4 - Localization/text-key asset label pass

Player-facing asset labels, descriptions, tooltips, and Infopedia strings should eventually move to keyed localization files.

The asset registry should be compatible with localization keys rather than hardcoded display text only.

Exit criteria:

Asset display text can be made publish-safe and localization-ready.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Major asset families are inventoried.
- Unused assets have review states.
- Publish-safe status exists for shipped assets.
- Quarantined/internal/unknown assets are not accidentally included in public packages.
- Semantic IDs exist for high-use and high-error assets.
- Direct path fallback is audited.
- low_32 remains the lean runtime tier.
- Optional art packs declare semantic overrides.
- Agriculture/animal/pet assets have valid semantic definitions before room/pet systems consume them.
- Noble luxury/draught assets have value, storage, and sale-restriction metadata before estate/vault systems consume them.
- Blueprint-ready assets expose construction/acquisition metadata before parity systems consume them.
- Vehicle/component assets expose semantic categories before vehicle systems consume them.
- Asset expansion does not break startup, package size, memory, or old-hardware viability targets.
- Editor/audit surfaces can inspect asset definitions.
- Infopedia/player-facing labels avoid raw IDs and placeholder labels.

Exit criteria:

The game may claim asset promotion readiness only when graphical files have stable semantic identity, publish-safe status, usage context, runtime tier ownership, fallback behavior, and audit visibility.

## Non-goals for this milestone

This milestone does not require every asset to be final art. It requires clear classification and safe promotion rules.

This milestone does not require every unused file to become game content. It requires unused files to be visible to audit and assigned a review state.

This milestone does not replace optional art-pack implementation. It defines the semantic and package rules those packs must obey.

This milestone does not make placeholder assets publish-safe. It requires placeholders to remain identifiable until replaced or explicitly approved for a temporary tier.

## Deferred checkpoint summary

The project must promote assets deliberately. Assets should be inventoried, classified, cleared, assigned semantic IDs, and given valid usage contexts before they are consumed by world generation, construction, vendors, Infopedia, pets, vehicles, noble estates, draught vaults, or production systems. The lean runtime should preserve low_32 assets for old-machine viability, while optional higher-quality packs provide overrides through validated semantic registries rather than fragile path replacement.

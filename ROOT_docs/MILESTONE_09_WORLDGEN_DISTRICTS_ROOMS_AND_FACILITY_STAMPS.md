# Milestone 09 - Worldgen, Districts, Rooms, and Facility Stamps

This ordered milestone document consolidates planning for world generation, district stamps, room stamps, facility stamps, faction-themed spaces, civic and industrial layouts, sewers, exterior maintenance corridors, noble estates, markets, clinics, armories, temples, farms, gardens, cloning rooms, animal pens, pet vendors, vehicle facilities, illicit laboratories, noble vaults, and asset-rich theme placement using the available Asset Tools integration.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the worldgen/district/room-stamp/facility-stamp slice of that roadmap.

This document is intentionally large and detailed. World generation is where the promoted asset inventory, faction identity, population provenance, item provenance, vehicle provenance, Ages of Control, construction parity, markets, pets, medicine, schemes, and player readability all become visible space. This milestone should therefore be treated as an expensive, asset-heavy integration pass rather than a light room-name pass.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- `MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` for agriculture, animal rooms, pet rooms, pet vendors, pet zoos, and care infrastructure.
- `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md` for semantic assets, asset promotion, asset readiness, optional art packs, and Asset Tools integration expectations.
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` for room/facility inspection, player-facing labels, Infopedia references, and no raw ID leakage.
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` for population demand, markets, vendors, critical supplies, illicit economies, noble luxury, and draught storage.
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` for faction rooms, player acquisition paths, blueprint-ready stamps, ownership, and construction parity.
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md` for garages, depots, motor pools, roads, parking, vehicle yards, salvage yards, and route control.
- `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md` for historical zone generation, Ages of Control, faction schemes, active events, leadership journals, and room-control history.
- `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md` for clinics, surgery rooms, private medicine rooms, medical stores, prosthetic/cybernetic labs, and narcotic body-effect support spaces.
- `MILESTONE_SUPPLEMENT_NOBLE_LUXURY_NARCOTICS_AND_DRAUGHT_TRADE.md` for noble luxury rooms, draught vaults, private medicine, and secured estate storage.
- `MILESTONE_SUPPLEMENT_ILLICIT_NARCOTICS_FACTION_PRODUCTION.md` for hidden labs, stash rooms, black-market distribution, and illicit production spaces.
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`.

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 2 - Asset promotion and worldgen-ready semantic assets.
- Phase 3 - Room, road, frontage, plaza, alley, parking, and spatial integration foundations already completed enough for scheduling.
- Phase 8 - Population provenance and demographic continuity.
- Phase 9 - Item provenance, facility origin, and supply ecology.
- Phase 10 - Vehicle provenance and facility support.
- Phase 11 - Ages of Control and historical zone generation.
- Phase 12 - Construction, bases, rooms, defenses, and ownership.
- Phase 16 - World generation, districts, sewers, temples, estates, and strategic simulation.
- Phase 17 - Economy, vendors, quests, contracts, pets, ownership, and faction reputation as generated facilities.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 4 - Presentation, targeting, examination, Infopedia, and inspection readability for generated spaces.
- Phase 5 - Operations, hauling, reservations, route readiness, and facility work.
- Phase 6 - Production, machines, recipes, staffing, and operation queues.
- Phase 7 - Ownership, access checks, container rules, restricted areas, and faction consequences.
- Phase 13 - Hazards, lighting, darkness, traps, gas, sludge, fires, wires, and environmental danger.
- Phase 14 - Combat, health, medical rooms, field hospitals, injury feedback, and structural damage.
- Phase 15 - Faction ranks, leaders, specialists, representatives, workers, guards, clergy, handlers, and vendors.

## Core doctrine

World generation must make the project’s content real in space. A room stamp is not a name. A district stamp is not a rectangle. A facility stamp is not decoration. Each generated space should have a reason to exist, an owning or occupying faction, a function, access rules, asset-backed visual identity, population or workforce context where relevant, supply or service meaning where relevant, and inspection text that tells the player what they are seeing without exposing internal IDs.

This milestone must be asset-rich. Where the Asset Tools integration exposes usable room, object, fixture, tile, corridor, wall, floor, overlay, faction, entity, vehicle, furniture, storage, market, medical, animal, pet, noble, illicit, industrial, civic, sewer, or utility asset families, worldgen should increasingly consume them through semantic asset IDs rather than raw filenames.

Generated spaces should make visible use of the available assets for different room types, faction identities, quality levels, and themes. If assets exist for a room category but the generator never places them, the asset audit should flag that as unused or under-integrated. If worldgen needs a room category but no safe asset exists, the asset audit should flag the missing asset requirement instead of silently placing empty placeholder rooms.

## Dependency rule

Asset readiness precedes stamp placement. A stamp may be planned before final art exists, but broad generation should prefer assets that have stable semantic IDs, publish-safe status, valid categories, footprint rules, render-layer expectations, and inspection labels.

Population provenance, item provenance, vehicle provenance, and Ages of Control should feed generation. A district should not place rooms only because a random table rolled them. It should place rooms because the zone’s faction history, economy, population, production needs, civic needs, vehicle needs, noble politics, illicit economy, market pressure, animal/pet economy, medical demand, or player-relevant scheme context justifies them.

Minimum viable zone generation should preserve useful room diversity. The first playable viability age should target at least 20 rooms and at least one room of every required type for the zone family before later Ages of Control expand, lose, repurpose, degrade, or transfer control.

## Asset Tools integration rule

The Asset Tools integration should be treated as the worldgen asset discovery and readiness layer.

Worldgen work should use Asset Tools outputs to:

- Identify available asset families.
- Identify unused but relevant assets.
- Identify room/facility categories with assets already available.
- Identify room/facility categories with missing or placeholder assets.
- Map assets to semantic IDs.
- Separate floor, wall, object, fixture, overlay, UI, editor, portrait, entity, and item categories.
- Preserve tile orientation and atlas indexing.
- Distinguish sewer corridor tiles from external maintenance corridor/void corridor tiles.
- Distinguish noble corridors, industrial corridors, civic corridors, market corridors, and faction-themed variants where assets exist.
- Promote worldgen-ready assets into stamp palettes.
- Flag raw path references that should become semantic references.
- Preserve low_32 lean runtime behavior while allowing optional higher-quality packs to override visual quality.

Worldgen should never knowingly flatten all assets into one generic pool. Asset category, theme, faction context, room purpose, tile orientation, and runtime tier matter.

## Phase 2 dependency - Asset readiness for world generation

### Phase 2.1 - Worldgen asset availability audit

Before expanding room generation, audit available assets using the Asset Tools integration.

Required categories:

- Floors.
- Walls.
- Roads.
- Sidewalks.
- Alleys.
- Parking surfaces.
- Sewer corridors.
- Sewer intersections.
- Exterior maintenance corridor / void corridor tiles.
- Noble corridors.
- Industrial floors.
- Market floors.
- Civic floors.
- Residential floors.
- Utility floors.
- Medical floors.
- Temple/civic-faith floors.
- Farm/garden/hydroponic assets.
- Animal/pet room assets.
- Vehicle facility assets.
- Noble estate assets.
- Vault/locked storage assets.
- Illicit lab/stash assets.
- Medical/cybernetic/prosthetic assets.
- Vendor/market assets.
- Containers and crates.
- Furniture.
- Lighting/overlay assets.
- Hazard assets.
- Faction markers and themed decorations.
- Entity and NPC role sprites where relevant.

Exit criteria:

Worldgen has a known asset palette per room/facility category instead of guessing from the file tree.

### Phase 2.2 - Asset-to-stamp palette pass

Create stamp palettes that assign semantic assets to room and district categories.

Palette entries should identify:

- Room category.
- District category.
- Faction theme.
- Floor asset options.
- Wall asset options.
- Door/gate options.
- Fixture options.
- Furniture options.
- Containers.
- Machines.
- Vendors/NPC anchors.
- Hazard options.
- Overlay options.
- Optional high-resolution overrides.
- Placeholder/fallback status.
- Publish-safe status.

Exit criteria:

Each major room and facility stamp can ask for an appropriate palette rather than manually assembling raw file paths.

### Phase 2.3 - Theme and faction visual identity pass

Generated rooms should reflect faction and theme identity where assets permit.

Theme axes include:

- Civic.
- Industrial.
- Residential.
- Market/commercial.
- Medical.
- Military/security.
- Noble estate.
- Illicit/gang.
- Agricultural.
- Animal/pet.
- Sewer/utility.
- Exterior maintenance/void corridor.
- Temple/civic-faith.
- Vehicle/garage/depot.
- Abandoned/ruined.
- Degraded/dirty.
- High-status/refined.
- Crude/improvised.

Faction room stamps should be able to request faction-specific colors, banners, signage, furniture, guard positions, containers, restricted doors, and recognizable asset families where available.

Exit criteria:

A player can often infer broad faction/theme from the room’s visual language, not just its tooltip.

## Phase 16 - District and zone families

### Phase 16.1 - Zone family definition pass

Zone families should define broad spatial and economic purpose.

Zone families may include:

- Hab block.
- Apartment district.
- Market/commercial district.
- Industrial sector.
- Estate/noble district.
- Sewer/utility district.
- Precinct/security district.
- Clinic/medical district.
- Temple/civic-faith district.
- Military district.
- Cargo/logistics district.
- Vehicle/garage district.
- Agricultural/hydroponic district.
- Animal/pet trade district.
- Illicit/gang district.
- Mixed contested district.
- Abandoned/degraded district.
- Faction compound.

Each zone family should define required rooms, optional rooms, dominant assets, route structure, typical faction owners, population support, vendors, hazards, and minimum viability requirements.

Exit criteria:

Zone generation starts from meaningful family definitions rather than undifferentiated room scatter.

### Phase 16.2 - District stamp pass

District stamps should group room stamps into coherent larger spaces.

District stamps should define:

- Anchor rooms.
- Support rooms.
- Circulation.
- Frontage.
- Road/sidewalk relationship where relevant.
- Restricted/public/private areas.
- Storage areas.
- Staff areas.
- Service corridors.
- Hazard zones.
- Faction control zones.
- Vendor/representative anchors.
- Population/workforce assumptions.
- Asset palettes.

Exit criteria:

A market district feels like a market district, an estate feels like an estate, and an industrial district feels like industrial infrastructure.

### Phase 16.3 - Road, alley, parking, and frontage integration pass

Roaded zones should use road-first generation where vehicles, commerce, patrols, cargo, and public access matter.

Required spatial elements:

- Roads.
- Sidewalks.
- Alleys.
- Frontages.
- Service entrances.
- Parking lots.
- Curb parking.
- Loading bays.
- Vehicle gates.
- Checkpoints.
- Transit anchors.
- Road-to-building transitions.
- Parking-to-room transitions.

Exit criteria:

Rooms connect to public infrastructure believably, and vehicles have legal movement/storage surfaces.

### Phase 16.4 - Sewer, utility, and external maintenance corridor pass

Sewer and maintenance spaces need their own tile identities and generation rules.

Required handling:

- Use sewer corridor tiles for sewer corridors and intersections.
- Use exterior maintenance corridor / void corridor tiles for external boundary maintenance spaces.
- Preserve tile orientation and atlas indexing.
- Avoid mixing wall and floor assets incorrectly.
- Avoid rendering semantic markers over black transparency without a base tile.
- Add utility hazards where appropriate.
- Add inspection labels for sewer, utility, and maintenance spaces.

Exit criteria:

Sewers and external maintenance spaces are visually and mechanically distinct rather than generic dark corridors.

## Phase 16 - Core room stamp families

### Phase 16.5 - Residential and hab room-stamp pass

Residential spaces should support normal life, population provenance, ownership, and inspection.

Room types:

- Apartment.
- Dormitory.
- Barracks bunkroom.
- Worker quarters.
- Servant quarters.
- Private room.
- Family room.
- Slum dwelling.
- Rented room.
- Player-owned room.
- Faction-owned housing.
- Noble servant housing.

Exit criteria:

Residents have plausible places to live, and room ownership can support access, theft, journals, pets, and player acquisition.

### Phase 16.6 - Market and commercial room-stamp pass

Commercial spaces should support vendors, storage, goods, contracts, and faction markets.

Room types:

- General goods stall.
- Food vendor.
- Water/supply vendor.
- Weapons vendor.
- Ammunition vendor.
- Armor vendor.
- Medical vendor.
- Tool/construction vendor.
- Blueprint vendor.
- Pet vendor.
- Luxury broker.
- Black-market dealer.
- Storage backroom.
- Counting office.
- Loading area.
- Market security post.

Exit criteria:

Vendors have physical rooms, stock storage, access rules, and faction/economy context.

### Phase 16.7 - Industrial and production room-stamp pass

Industrial spaces should support machines, production chains, workforce, hazards, and item provenance.

Room types:

- Workshop.
- Machine shop.
- Forge/fabrication room.
- Assembly room.
- Component factory.
- Tool room.
- Material storage.
- Maintenance room.
- Power room.
- Boiler/utility room.
- Waste room.
- Quality inspection room.
- Worker staging room.
- Loading bay.

Exit criteria:

Production facilities create plausible goods, supply pressure, jobs, hazards, and faction value.

### Phase 16.8 - Medical, cybernetic, and clinic room-stamp pass

Medical spaces should support treatment, medicine, surgery, prosthetics, cybernetics, and body-state systems.

Room types:

- Clinic.
- Field clinic.
- Surgery room.
- Recovery ward.
- Medicine storage.
- Private physician office.
- Noble medical room.
- Black-market doctor room.
- Prosthetic fitting room.
- Cybernetic installation room.
- Quarantine room.
- Treatment waiting area.
- Medical vendor.

Exit criteria:

Medical systems have physical places to happen and assets to represent them.

### Phase 16.9 - Security, precinct, military, and defensive room-stamp pass

Security and military spaces should support guards, weapons, evidence, arrests, restricted access, and faction power.

Room types:

- Guard post.
- Checkpoint.
- Armory.
- Ammunition store.
- Barracks.
- Training room.
- Holding cell.
- Interrogation room.
- Evidence room.
- Security office.
- Patrol staging room.
- Defensive bunker.
- Gatehouse.
- Vehicle checkpoint.

Exit criteria:

Security and military factions have physical infrastructure that supports control, violence, restricted goods, and schemes.

### Phase 16.10 - Administrative, civic, and faction representative room-stamp pass

Civic and administrative spaces should support permits, licenses, property rights, blueprints, records, and public authority.

Room types:

- Faction representative office.
- Permit office.
- License office.
- Property records office.
- Tax office.
- Contract desk.
- Public records room.
- Meeting room.
- Clerk office.
- Leadership office.
- Archive.
- Locked records room.

Exit criteria:

Administrative systems have physical spaces for faction legitimacy, contracts, records, and player/faction parity.

## Phase 16 - Specialized room/facility stamp families

### Phase 16.11 - Temple and civic-faith room-stamp pass

Every suitable zone should be able to host civic-faith spaces near plazas where setting rules support it.

Temple elements:

- Nave or main hall.
- Pillars.
- Relic alcoves.
- Prayer nooks.
- Candle racks.
- Donation box.
- Supplicant kitchen.
- Clergy room.
- Pilgrim space.
- Guard position.
- Non-hostile head officiant interaction point.

Service support:

- Costly absolution/standing-repair service tied to hunger/sleep/time costs and limited civil-faction standing restoration where implemented.

Exit criteria:

Civic-faith institutions are physical, staffed, inspectable, and connected to player services rather than decorative icons.

### Phase 16.12 - Noble estate and Ashbourne house room-stamp pass

Noble estates should be complex, high-status, secured, and faction-specific.

Room types:

- Estate foyer.
- Reception room.
- Noble apartment.
- Private study.
- Servant quarters.
- Guard room.
- Luxury cellar.
- Pleasure salon.
- House physician room.
- Private medicine cabinet.
- Imported-goods store.
- Broker receiving room.
- Guarded storehouse.
- Noble vault.
- Sealed off-world substance vault.
- Draught vault.
- Reliquary display.
- Locked records room.
- Leadership journal storage.
- Private dining room.
- Garden court.

Draught items should appear only in secured contexts and should be explicitly rare, off-world, extremely valuable, and normally not for sale.

Exit criteria:

Noble estates become spatial expressions of wealth, secrecy, luxury, restricted goods, politics, and theft/quest opportunity.

### Phase 16.13 - Illicit, gang, smuggling, and black-market room-stamp pass

Illicit organizations should have physical production, sale, storage, and secrecy infrastructure.

Room types:

- Hidden drug lab.
- Back-room chemical workshop.
- Corrupt clinic.
- Front pharmacy.
- Greenhouse or mushroom drug room.
- Stash room.
- Smuggling depot.
- Distribution room.
- Guarded dealer room.
- Black-market stall.
- Counterfeit workshop.
- Debt collection room.
- Gang boss room.
- Hidden planning room.
- Burned-out lab.
- Contaminated lab.

Exit criteria:

Illicit economies leave visible, inspectable, and scheme-relevant room/facility traces.

### Phase 16.14 - Agriculture, cloning, animal, and pet room-stamp pass

Agriculture and animal spaces should support food supply, civilian life, trade, research, pets, and care systems.

Room types:

- Farm.
- Garden.
- Hydroponic plot.
- Mushroom bed.
- Seed storage.
- Crop storage.
- Cloning room.
- Incubator room.
- Livestock room.
- Animal pen.
- Kennel.
- Cattery.
- Rodent cage room.
- Pet zoo.
- Pet vendor.
- Feed storage.
- Water station.
- Cleaning station.
- Animal-care room.
- Handler office.
- Veterinary treatment area where supported.

Pet/animal spaces must support future ownership, care, pettible interactions, feeding, watering, shelter, and inspection. A kennel should imply dog-like pets or working animals; a cattery should imply cat-like pets; a rodent cage should imply mice, rats, or equivalent small animals.

Exit criteria:

Agriculture, animal, and pet rooms are functional spaces, not decorative labels.

### Phase 16.15 - Vehicle facility, garage, and motor-pool room-stamp pass

Vehicle systems require physical infrastructure.

Room types:

- Garage.
- Depot.
- Motor pool.
- Repair shop.
- Vehicle yard.
- Military vehicle bay.
- Convoy staging area.
- Salvage yard.
- Component factory.
- Fuel/power depot.
- Loading bay.
- Parking lot.
- Checkpoint.
- Vehicle gate.
- Chop shop.
- Noble vehicle house.

Exit criteria:

Vehicles have places to be stored, repaired, deployed, stolen, salvaged, and contested.

### Phase 16.16 - Hazard, ruin, and degradation room-stamp pass

Ages of Control and maintenance failure should produce hazardous and degraded spaces.

Room/feature types:

- Burned room.
- Collapsed room.
- Flooded room.
- Gas-filled room.
- Toxic spill.
- Shorted wire room.
- Sludge area.
- Overheated factory.
- Freezer space.
- Jagged passage.
- Broken machine room.
- Abandoned clinic.
- Contaminated lab.
- Vehicle wreck chokepoint.
- Looted store.
- Ruined apartment.
- Barricaded checkpoint.

Exit criteria:

Dangerous spaces are generated with readable hazards, asset support, and historical cause.

## Phase 16 - Faction themed stamp integration

### Phase 16.17 - Faction room ownership and theme pass

Faction-owned rooms should reflect the controlling faction through assets, layout, guards, furniture, signage, containers, restricted doors, and inspection labels.

Faction theme integration should include:

- Noble house variants.
- Gang groups.
- Illicit organizations.
- Civic authorities.
- Security/military factions.
- Market factions.
- Industrial factions.
- Medical factions.
- Agricultural factions.
- Religious/civic-faith institutions.
- Player-founded or player-controlled rooms where supported.

Exit criteria:

Faction control is visible in room composition, not only map metadata.

### Phase 16.18 - Room ownership, access, and restricted-area pass

Generated spaces should define access expectations.

Access categories include:

- Public.
- Semi-public.
- Private.
- Faction-only.
- Staff-only.
- Noble-only.
- Restricted.
- Military.
- Illicit-hidden.
- Locked.
- Vaulted.
- Abandoned.
- Player-owned.
- Contested.

Exit criteria:

Generated rooms can participate in access checks, theft, ownership, blueprint acquisition, faction consequences, and player expansion heat.

### Phase 16.19 - Vendor, NPC, and workforce anchor pass

Room stamps should declare where vendors, guards, workers, handlers, clergy, representatives, doctors, brokers, mechanics, smugglers, and other role actors can stand, work, sleep, patrol, or interact.

Anchor types:

- Vendor counter.
- Workbench.
- Machine station.
- Guard post.
- Patrol route.
- Clerk desk.
- Treatment bed.
- Surgery table.
- Pet enclosure.
- Vehicle bay.
- Boss desk.
- Journal storage.
- Vault guard point.
- Prayer/officiant point.

Exit criteria:

NPC placement and behavior are supported by room layout rather than random actor scatter.

### Phase 16.20 - Container, loot, journal, and evidence placement pass

Room stamps should support controlled placement of containers and evidence.

Placement targets:

- Lockers.
- Crates.
- Cabinets.
- Safes.
- Vaults.
- Desks.
- Personal containers.
- Stashes.
- Medicine cabinets.
- Food stores.
- Armories.
- Ammunition stores.
- Pet supplies.
- Blueprint records.
- Leadership journals.
- Sealed orders.
- Draught inventories.
- Narcotics ledgers.
- Proof-of-death evidence containers where applicable.

Exit criteria:

Important objects are placed in plausible storage contexts that support theft, quests, inspection, and consequences.

## Phase 11 and Phase 16 - Ages of Control integration

### Phase 16.21 - Historical room mutation pass

Ages of Control should mutate district and room stamps.

Historical changes may include:

- Room expansion.
- Room abandonment.
- Ownership transfer.
- Room repurposing.
- Room fortification.
- Room looting.
- Room damage.
- Room cleanup.
- Room conversion to illicit use.
- Room conversion to emergency use.
- Room conversion to market use.
- Room conversion to barracks or storage.
- Vault sealing.
- Journal removal or loss.
- Vehicle wreck placement.
- Garden death or recovery.

Exit criteria:

Generated zones visibly show historical sequence rather than static first-pass layout.

### Phase 16.22 - Active scheme room event pass

Active faction schemes should use room stamps and anchors for visible runtime events.

Events include:

- Room assault.
- Counterattack.
- Reinforcement arrival.
- Sabotage.
- Smuggling pickup.
- Journal recovery search.
- Vehicle repair.
- Guard muster.
- Market seizure.
- Vault theft.
- Pet/animal rescue.
- Clinic emergency.
- Narcotics lab raid.
- Noble scandal cleanup.

Exit criteria:

Active schemes can manifest in physical room contexts the player can see and interact with.

## Phase 18 - Editor, stamp tooling, audit, and Infopedia support

### Phase 18.1 - Room stamp editor pass

The editor should support defining and inspecting room stamps through semantic assets and categories.

Editor fields:

- Stamp ID.
- Player-facing room name.
- Room category.
- Faction/theme variants.
- Required assets.
- Optional assets.
- Floors/walls/doors/fixtures.
- Furniture.
- Containers.
- NPC anchors.
- Vendor anchors.
- Workforce anchors.
- Access categories.
- Ownership expectations.
- Hazard options.
- Loot/evidence placement.
- Blueprint/acquisition metadata.
- Infopedia entry link.
- Publish-safe status.

Exit criteria:

Room stamps can be created and audited without central-code sprawl.

### Phase 18.2 - District stamp editor pass

The editor should support district stamps as compositions of room stamps and circulation.

Editor fields:

- District category.
- Zone family.
- Anchor rooms.
- Required rooms.
- Optional rooms.
- Circulation rules.
- Road/sidewalk relation.
- Faction ownership weights.
- Population assumptions.
- Facility assumptions.
- Vendor assumptions.
- Vehicle support.
- Hazard/degradation variants.
- Ages-of-Control mutation hooks.

Exit criteria:

Districts can be reviewed and adjusted as coherent facility groups rather than hand-coded patterns.

### Phase 18.3 - Asset Tools stamp-audit pass

Asset Tools integration should expose worldgen readiness and stamp usage.

Audit outputs should include:

- Assets used by each room stamp.
- Assets unused by any stamp.
- Stamps missing required assets.
- Assets with raw path references.
- Assets with missing semantic IDs.
- Assets with unknown publish-safe status.
- Placeholder assets used by worldgen.
- Optional-pack overrides used by stamps.
- Atlas/tile-orientation problems.
- Wall/floor category mismatches.
- Transparent markers lacking base tiles.
- Missing player-facing labels.
- Missing Infopedia links.

Exit criteria:

The project can tell which assets power which generated spaces and where the asset pipeline is failing.

### Phase 18.4 - Worldgen audit and inspection pass

Audit surfaces should inspect generated zone structure.

Audit fields:

- Zone family.
- District stamps used.
- Room stamps used.
- Required room coverage.
- Faction ownership.
- Access categories.
- Population targets.
- Vendor/facility support.
- Vehicle support.
- Hazard support.
- Ages-of-Control history.
- Asset palettes used.
- Missing asset fallbacks.
- Room-control transfers.
- Scheme/event hooks.

Exit criteria:

Generated zones can be reviewed for correctness rather than judged by sight alone.

### Phase 18.5 - Infopedia room/facility entry pass

Infopedia entries should exist for major room and facility categories.

Entries should explain:

- What the room is.
- What it does.
- Which factions use it.
- Whether the player can build/acquire it.
- What blueprint unlocks it where applicable.
- What vendors, workers, machines, animals, vehicles, or services it supports.
- What goods it produces or stores.
- What access restrictions apply.
- What hazards may exist.
- What related rooms or facilities exist.

Exit criteria:

The player can look up generated spaces and understand their function.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Asset Tools integration can identify room/facility asset families relevant to worldgen.
- Major room and facility categories use semantic asset palettes rather than raw paths.
- Floor, wall, road, sewer, external maintenance, and corridor categories remain separated.
- Sewer corridors and external maintenance/void corridors use correct tile families.
- Transparent markers are drawn over valid base tiles rather than black backgrounds.
- Zone families define required and optional rooms.
- Minimum viable zones can generate enough room diversity for gameplay.
- District stamps compose rooms coherently.
- Faction-owned rooms show faction/theme identity where assets exist.
- Residential, market, industrial, medical, security, civic, temple, noble, illicit, agriculture, animal, pet, vehicle, hazard, and utility spaces have stamp targets where supported.
- Noble draught vaults and luxury rooms appear only in secured appropriate estate contexts where implemented.
- Illicit narcotics rooms appear only where faction identity/history supports them where implemented.
- Pet/animal rooms support future ownership/care/petting logic rather than decoration only where implemented.
- Vehicle facilities support road/parking/garage/depot constraints where implemented.
- Rooms define access/ownership categories.
- Vendors and workforce have anchors where room purpose requires them.
- Important containers, leadership journals, vault goods, evidence, and blueprints have plausible storage contexts where implemented.
- Ages of Control can mutate rooms and ownership where implemented.
- Active schemes can use room anchors for visible events where implemented.
- Editor/audit surfaces can inspect room stamps, district stamps, asset usage, and worldgen output.
- Infopedia entries explain major exposed room/facility types.
- Player-facing room names, labels, and inspection text avoid raw IDs and placeholder labels.
- Worldgen does not bloat startup, memory, or old-hardware rendering beyond defined runtime tiers.

Exit criteria:

The game may claim expanded world generation, district stamps, room stamps, facility stamps, and asset-rich faction/themed spaces only when those spaces are asset-backed, inspectable, faction-aware, player-readable, access-aware, provenance-aware where relevant, and integrated with the broader milestone systems.

## Non-goals for this milestone

This milestone does not require every room type to be fully implemented immediately. It defines the expensive integration target and audit requirements so worldgen can grow safely.

This milestone does not replace asset promotion. It consumes asset promotion results and identifies missing asset needs.

This milestone does not require every background room to run full detailed simulation. Rooms may be abstract until player proximity, faction schemes, quests, vendors, hazards, ownership, or inspection require detail.

This milestone does not allow random markdown sprawl for every room type. New room families should be added through this milestone, stamp data, editor/audit definitions, or the master plan as appropriate.

## Deferred checkpoint summary

World generation should be an asset-rich, faction-aware, historically informed spatial system. It should consume Asset Tools integration outputs, semantic asset palettes, population provenance, item provenance, vehicle provenance, Ages of Control, construction parity, economy, medical systems, pet systems, noble/draught systems, illicit systems, and active-scheme requirements. Generated districts and rooms should use available assets for their type, faction, and theme, expose player-readable inspection, define access/ownership, support vendors/workforce/containers where needed, and provide physical spaces for the simulation rather than decorative names on empty rectangles.

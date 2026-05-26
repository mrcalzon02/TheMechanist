# Milestone 05 - Construction, Blueprints, Ownership, and Player/Faction Parity

This ordered milestone document consolidates planning for construction parity, room and asset blueprints, faction-accessible construction, player-accessible faction assets, ownership, legality, access checks, permits, licenses, stolen assets, restricted assets, player expansion heat, and the emergence of the player as a faction-scale actor.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the construction/ownership/parity slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- `MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md`
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where vendors, markets, permits, licenses, and faction representatives expose blueprints
- Master-plan Phase 7, Phase 12, Phase 16, Phase 17, Phase 18, and Phase 19 parity and construction notes

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 7 - Containers, permissions, access checks, and ownership.
- Phase 12 - Construction, bases, rooms, defenses, and ownership.
- Phase 16 - World generation, district/facility placement, and faction construction context.
- Phase 17 - Economy, contracts, blueprint sales, faction representatives, reputation, and market access.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Asset promotion and blueprint-ready room/asset definitions.
- Phase 4 - UI, construction feedback, targeting, inspection, and player-facing readability.
- Phase 5 - Operations, reservations, hauling, and task lifecycle.
- Phase 6 - Machines, production, staffing, and construction material requirements.
- Phase 8 - Population/workforce context for who builds, owns, staffs, and operates assets.
- Phase 9 - Item provenance and supply ecology for construction materials and manufactured assets.
- Phase 10 - Vehicles as constructed, owned, stored, repaired, and restricted assets.
- Phase 11 - Faction schemes that build, seize, expand, repair, or sabotage rooms/assets.

## Core parity doctrine

Whatever a faction can have, the player should have a plausible path to obtain. Whatever the player can have, factions should have a plausible path to obtain.

This does not mean all access is immediate, cheap, legal, safe, or reputation-neutral. It means the world model should not reserve entire functional categories exclusively for NPC factions unless there is a deliberate, documented exception.

Valid player acquisition paths may include:

- Buying a blueprint.
- Buying an item, machine, room kit, or vehicle kit.
- Purchasing access through reputation.
- Receiving a blueprint as a quest reward.
- Earning a faction license or permit.
- Salvaging or reverse engineering.
- Theft, smuggling, or black-market acquisition.
- Trading with a faction vendor.
- Hiring specialists who know the construction pattern.
- Capturing an existing room, machine, vehicle, or facility.
- Learning through research, examination, or Infopedia-supported discovery.

Valid faction acquisition paths may include:

- Using faction-owned blueprints.
- Buying plans, permits, or licenses from superior institutions.
- Producing rooms/assets through workforce and materials.
- Stocking vendors from provenance-aware supply.
- Salvaging captured rooms, machinery, or vehicles.
- Seizing property through schemes, law, war, debt, or intimidation.
- Hiring specialists.
- Repairing, expanding, or replacing damaged infrastructure.
- Abstract distant-zone procurement where performance requires aggregation.

## Dependency rule

Player/faction parity depends on stable room, asset, vendor, blueprint, ownership, access, reputation, material, workforce, and provenance definitions.

Blueprint availability must not be merely a list of item names. Each blueprint should know what room or asset it unlocks, what faction or vendor can sell it, what reputation/license gates apply, what materials or machines it requires, what facility category it belongs to, whether it is legal/restricted/illicit/stolen/counterfeit, and whether factions can also build or deploy it.

Construction parity must not bypass ownership, law, faction standing, construction validation, no-self-entombment, material requirements, staffing requirements, utility requirements, heat/suspicion, or access systems.

## Phase 2 dependency - Blueprint-ready semantic definitions

Rooms and world assets must become blueprint-ready before construction parity can be audited.

Blueprint-ready entries should record:

- Stable semantic ID.
- Player-facing name.
- Room or asset category.
- Footprint and placement constraints.
- Required tiles, walls, doors, fixtures, machines, containers, utilities, and access rules.
- Owning faction styles or variants.
- Required materials, tools, skills, knowledge, workforce, utility, and time.
- Whether it is public, private, restricted, military, industrial, civic, residential, commercial, agricultural, animal, vehicle, religious, medical, research, illicit, or black-market.
- Whether the player can buy, earn, steal, reverse engineer, salvage, research, or capture the blueprint.
- Whether factions can generate, build, stock, repair, expand, defend, or lose the same room.

Exit criteria:

Rooms and assets that appear in faction spaces can be tied to blueprint definitions instead of remaining worldgen-only decoration.

## Phase 7 - Access, legality, ownership, and restricted use

### Phase 7.1 - Ownership categories

Construction and blueprint systems should respect clear ownership categories.

Ownership/access categories include:

- Player-owned.
- Faction-owned.
- Public.
- Private.
- Restricted.
- Military.
- Civic/permit-bound.
- Noble/house-owned.
- Stolen.
- Seized.
- Abandoned.
- Locked.
- Forbidden.
- Black-market.
- Counterfeit.
- Salvaged.
- Quest-granted.

Exit criteria:

The game can explain who owns a room, asset, vehicle, blueprint, machine, or container and why the player can or cannot use it.

### Phase 7.2 - Blueprint access and legality

Blueprint ownership, access, and usage must respect faction permission systems.

Access types include:

- Public civilian blueprint.
- Faction-approved blueprint.
- Reputation-gated blueprint.
- License-gated blueprint.
- Permit-gated blueprint.
- Restricted industrial blueprint.
- Restricted military blueprint.
- Noble-house blueprint.
- Illegal or black-market blueprint.
- Stolen blueprint.
- Salvaged blueprint.
- Reverse-engineered blueprint.
- Quest-awarded blueprint.
- Counterfeit or unsafe blueprint.

The system should distinguish owning a plan from having the right, resources, space, and authority to build it.

Exit criteria:

Blueprint access does not bypass ownership, law, faction standing, construction validation, or restricted-area systems.

### Phase 7.3 - Access denial and consequence messaging

Denied construction, denied blueprint usage, and denied asset interaction must produce useful player-facing explanations.

Examples:

- You know the plan, but lack the license.
- You own the blueprint, but this faction controls the space.
- The blueprint is military-restricted.
- This room requires a civic permit.
- The machine requires a trained operator.
- You lack the materials.
- You lack utility access.
- You are not authorized to modify this faction room.
- This plan appears stolen and using it may raise heat.

Exit criteria:

The player understands why an action is blocked and what broad path could make it possible.

## Phase 12 - Construction parity and player-owned infrastructure

### Phase 12.1 - Player/faction construction parity pass

Every faction room type that can be generated, built, controlled, or expanded by factions should eventually have a player-accessible construction pathway unless explicitly marked non-acquirable with a documented reason. Every player-constructible room type should be available to factions through faction construction, facility expansion, worldgen history, or leadership schemes where appropriate.

This includes:

- Housing rooms.
- Workshops.
- Storage rooms.
- Markets.
- Clinics.
- Kitchens and food rooms.
- Security rooms.
- Armories.
- Ammunition stores.
- Vehicle garages.
- Motor pools.
- Farms and gardens.
- Animal pens.
- Pet vendors.
- Laboratories.
- Temples or civic-faith facilities where setting rules allow.
- Utility rooms.
- Defensive rooms.
- Faction offices and representative rooms.
- Noble estate service rooms where access rules allow.
- Black-market rooms where illicit acquisition paths exist.

Exit criteria:

Faction rooms are not permanently locked behind invisible NPC-only rules, and player construction does not exist in a separate toy system that factions cannot use.

### Phase 12.2 - Blueprint-to-construction execution pass

Blueprints should connect to actual construction operations.

A blueprint should be able to answer:

- What can be built.
- Where it can be built.
- What it costs.
- What time it requires.
- What workers or manual effort are required.
- What utilities are required.
- What tools and machines are required.
- What faction/reputation/license/permit requirements apply.
- What heat/suspicion may be generated.
- What inspection output should show before construction begins.

Exit criteria:

Blueprints are not collectible text objects only; they enable construction workflows when conditions are met.

### Phase 12.3 - Player asset heat and suspicion pass

Player construction, ownership, and accumulation of rooms, machines, vehicles, defenses, and production capacity should raise heat and suspicion by noticeable but variable amounts.

Sliding-scale examples:

- One living room or shelter: visible, but not a strategic threat.
- One small workshop: visible to local interests, modest heat.
- One purchased car: noticeable, but not alarming.
- Several rooms: increased attention from landlords, civic authorities, gangs, and neighboring factions.
- Production machines: stronger commercial/faction interest.
- Armed defenses: security concern and possible hostile attention.
- Three cars or a small vehicle fleet: significant faction notice.
- A dozen rooms: player looks like a real local power.
- Armory, clinic, lab, motor pool, guarded storage, or production chain: faction-scale heat.

Possible faction reactions:

- Surveillance.
- Warnings.
- Offers.
- Taxes or fees.
- Protection rackets.
- Extortion.
- Recruitment attempts.
- Diplomacy.
- Sabotage.
- Raids.
- Legal pressure.
- Scheme targeting.
- Rival construction or market competition.

Exit criteria:

The player can build a modest life without instantly becoming a warlord, but sustained expansion into faction-like infrastructure makes factions notice and react.

### Phase 12.4 - Room ownership and capture bridge

The player should be able to own, capture, lease, rent, buy, claim, be granted, or lose rooms through coherent world systems.

Ownership paths include:

- Purchase.
- Lease.
- Faction grant.
- Quest reward.
- Conquest.
- Abandonment claim.
- Salvage claim.
- Legal permit.
- Illegal occupation.
- Noble patronage.
- Criminal protection agreement.

Exit criteria:

Room control is a world-state concept, not merely a construction menu flag.

## Phase 16 - Worldgen and faction construction context

### Phase 16.1 - Faction-room construction source pass

Generated faction rooms should be traceable to faction capability, historical control, leadership schemes, facility need, or room-stamp logic.

A faction room should be able to answer:

- Who built it.
- Who controls it now.
- Whether it is public, private, restricted, or hostile.
- Whether the player can acquire a blueprint for it.
- Which faction representative or vendor might sell the plan.
- Whether the room is impossible, forbidden, or non-acquirable and why.

Exit criteria:

Worldgen rooms can feed construction parity and blueprint discovery.

### Phase 16.2 - Blueprint vendor and representative placement

Faction territories should place representatives or vendors who can sell, grant, or explain relevant room blueprints.

Examples:

- Civic offices for permits, licenses, and legal room plans.
- Industrial offices for workshop, machine, and production plans.
- Market vendors for stalls, storage, food, and general business rooms.
- Military/security representatives for restricted armory, barracks, checkpoint, and defensive plans.
- Medical administrators for clinic, surgery, quarantine, and care rooms.
- Agricultural representatives for farms, gardens, hydroponics, animal pens, and food rooms.
- Pet vendors or handlers for kennels, catteries, rodent cages, and animal-care rooms.
- Noble brokers for estate services, luxury rooms, private medicine, and house-restricted assets.
- Illicit contacts for black-market rooms, hidden labs, stash rooms, and smuggling facilities.

Exit criteria:

The player can seek out the right kind of faction location or representative for the kind of room they want.

## Phase 17 - Blueprint economy, representatives, and parity markets

### Phase 17.1 - Blueprint economy pass

Faction representatives and appropriate vendors should sell or grant blueprints for faction rooms and assets.

Blueprint offers should define:

- Vendor or representative type.
- Faction source.
- Room or asset unlocked.
- Price.
- Reputation requirement.
- License or permit requirement.
- Knowledge requirement.
- Quest prerequisite where relevant.
- Stock or availability behavior.
- Legal/restricted/stolen/counterfeit/military/black-market/civic status.
- Heat, suspicion, or faction-attention impact.

Exit criteria:

The player can intentionally pursue a desired room or asset blueprint instead of hoping random construction options appear.

### Phase 17.2 - Symmetric faction acquisition pass

Factions should use comparable acquisition paths to the player when expanding or upgrading.

Factions can:

- Buy or receive blueprints and permits.
- Use known faction plans.
- Construct rooms from materials and workforce.
- Open vendors based on facilities and staff.
- Stock vendors from provenance-aware supply.
- Repair or replace machines and vehicles.
- Salvage captured rooms or vehicles.
- Seize property through schemes.
- Deploy specialists to operate facilities.
- Close or restrict vendor access due to conflict, scarcity, or reputation.

Exit criteria:

Faction growth does not feel like pure script magic when the same kinds of assets are player-accessible.

### Phase 17.3 - Blueprint contracts and rewards pass

Contracts and quests should be able to grant, unlock, reveal, steal, recover, or counterfeit blueprints.

Examples:

- Earn a clinic-room blueprint by helping a medical faction.
- Steal a hidden lab blueprint from an illicit group.
- Recover an industrial plan from a ruined workshop.
- Buy a legal shopfront permit from a civic office.
- Receive a defensive-room plan from a security faction.
- Trade faction reputation for a restricted armory plan.
- Find a counterfeit blueprint that creates safety or legality risk.

Exit criteria:

Blueprints become part of the quest/economy loop, not only vendor stock.

### Phase 17.4 - Infopedia acquisition bridge

The player should be able to use the Infopedia to learn acquisition pathways for rooms and assets when those pathways are known.

Infopedia entries should answer:

- Can the player build this?
- Can factions build this?
- Which blueprint unlocks it?
- Who sells or grants the blueprint?
- Which faction type commonly provides it?
- What reputation, license, or permit is required?
- What materials, machines, rooms, utilities, or workforce are needed?
- What heat, suspicion, legality, or access issues may apply?

Exit criteria:

The player can investigate how to obtain a room or asset without external notes.

## Phase 18 - Editor and audit support

### Phase 18.1 - Parity audit pass

Editor/audit surfaces should inspect parity gaps.

Audit questions:

- Does every faction room have a declared player acquisition status?
- Does every player room have a declared faction-use status?
- Does every blueprint point to a valid room or asset?
- Does every blueprint have a player-facing name?
- Does every restricted blueprint have a legality/access explanation?
- Does every faction vendor stock blueprint categories matching faction identity?
- Are non-acquirable rooms explicitly marked and justified?
- Are player-only or faction-only exceptions documented?

Exit criteria:

Parity gaps can be found through data surfaces rather than manual source inspection.

### Phase 18.2 - Blueprint editor/audit pass

Editor/audit surfaces should define and inspect:

- Blueprint definitions.
- Room-to-blueprint mappings.
- Asset-to-blueprint mappings.
- Faction vendor types.
- Vendor stock categories.
- Reputation/license/permit gates.
- Legal, restricted, stolen, black-market, counterfeit, noble, civic, and military labels.
- Faction construction capability.
- Player construction capability.
- Facility provenance behind blueprint stock.
- Blueprint acquisition paths.
- Heat and suspicion impacts.

Exit criteria:

Blueprints, construction parity, and acquisition paths are data-owned and inspectable.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Every faction room stamp has a declared player acquisition status.
- Every player-constructible room has a declared faction-use status.
- Room and asset blueprints have stable semantic IDs and player-facing names.
- Blueprint vendors or representatives exist for major faction room categories where implemented.
- Blueprint restrictions are visible and understandable.
- Restricted, illegal, military, black-market, noble, civic, stolen, or counterfeit blueprints are labeled appropriately.
- Owning a blueprint is distinguished from having permission and resources to build it.
- Construction validation still checks placement, materials, access, utilities, and no-self-entombment.
- Player expansion heat scales with room, machine, vehicle, defense, and production footprint.
- Faction reactions to player expansion are available or explicitly deferred.
- Infopedia entries explain blueprint acquisition where applicable.
- Parity gaps are fixed or documented as intentional exceptions.
- Player-facing text avoids raw IDs and placeholder labels.

Exit criteria:

The game may claim player/faction construction parity only when faction rooms, player rooms, blueprint paths, vendor access, access restrictions, ownership rules, heat consequences, and faction acquisition logic can be audited coherently.

## Non-goals for this milestone

This milestone does not require every room, machine, vehicle, or facility to be fully implemented immediately. It requires the planning and audit structure that prevents faction-only and player-only systems from drifting apart.

This milestone does not make restricted or military assets easy to acquire. It requires that acquisition paths, restrictions, and exceptions be explicit.

This milestone does not remove faction advantage. Factions may have resources, labor, licenses, protection, supply chains, and political power the player lacks. The player should still have possible paths into comparable capability through time, risk, reputation, theft, salvage, research, or conquest.

## Deferred checkpoint summary

Player and faction item/construction parity must be preserved. Faction rooms should have acquirable or discoverable blueprint paths unless explicitly excepted. Factions should have vendors and representatives appropriate to their identity. Blueprint acquisition should connect to reputation, permits, licenses, stores, black markets, quests, salvage, reverse engineering, and Infopedia references. Player construction and asset accumulation should raise heat and suspicion on a sliding scale as the player grows from individual survivor into faction-scale actor.

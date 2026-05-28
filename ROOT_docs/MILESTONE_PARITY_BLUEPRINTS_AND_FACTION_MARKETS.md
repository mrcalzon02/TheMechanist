# Milestone - Player/Faction Parity, Blueprints, and Faction Markets

This milestone document is a controlled phase-group expansion for the master development plan. It exists because player/faction construction parity, faction-room blueprints, faction vendors, and critical supply markets cross multiple roadmap phases and are central enough to require a dedicated planning surface.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This milestone describes how the game should preserve parity between player-accessible and faction-accessible rooms, items, machines, vehicles, defenses, services, and construction capabilities.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable coding or packaging rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Core parity doctrine

Whatever a faction can have, the player should have a plausible path to obtain. Whatever the player can build, own, stock, operate, or deploy, factions should have a plausible path to build, own, stock, operate, or deploy.

This does not mean all access is immediate, cheap, legal, safe, or reputation-neutral. It means the world model should not reserve entire categories of functional capability exclusively for NPC factions unless there is a deliberate, documented exception.

Valid acquisition paths may include:

- Buying a blueprint.
- Buying an item, machine, vehicle, or room kit.
- Purchasing access through reputation.
- Receiving a blueprint as a quest reward.
- Earning a faction license or permit.
- Salvaging or reverse engineering.
- Theft or black-market acquisition.
- Trading with a faction vendor.
- Hiring specialists who know the construction pattern.
- Capturing an existing room, machine, vehicle, or facility.
- Learning through research or examination.

The same parity runs in reverse: factions should not be unable to use major gameplay categories just because they were first created for the player. If the player can own rooms, vehicles, production machines, pets, defenses, stores, or specialized facilities, factions must eventually be able to own and operate comparable assets through their own economic and leadership systems.

## Dependency rule

Faction room, machine, vehicle, and facility blueprints depend on stable semantic room/facility definitions. Faction vendors depend on faction identity, room/facility ownership, stock rules, supply provenance, access checks, and reputation rules. Critical supply vendors for food, weapons, and ammunition should exist before faction economies claim to support basic survival, security, and conflict loops.

Blueprint availability must not be merely a list of item names. Each blueprint should know what room or asset it unlocks, what faction or vendor can sell it, what reputation/license gates apply, what materials or machines it requires, what facility category it belongs to, and whether factions can also build or deploy it.

## Phase 2 addition - Blueprint and room-definition asset readiness pass

Add a Phase 2 child pass for blueprint-linked room and asset definitions.

Required outcome:

Promoted world assets and room stamps should expose enough semantic metadata to be sold, unlocked, inspected, and constructed as blueprints.

Blueprint-ready entries should record:

- Stable semantic ID.
- Player-facing name.
- Room or asset category.
- Footprint and placement constraints.
- Required tiles, walls, doors, fixtures, machines, containers, utilities, and access rules.
- Owning faction styles or variants.
- Whether the room is public, private, restricted, military, industrial, civic, residential, commercial, agricultural, animal, vehicle, religious, medical, research, or black-market.
- Whether a player may buy, earn, steal, reverse engineer, salvage, or research the blueprint.
- Whether factions may generate, build, stock, repair, expand, or defend the same room.

Exit criteria:

Rooms and assets that appear in faction spaces can be tied to blueprint definitions instead of remaining worldgen-only decoration.

## Phase 7 addition - Blueprint access, ownership, and legality pass

Add a Phase 7 child pass for blueprint access and legality.

Required outcome:

Blueprint ownership, access, and usage must respect faction permission systems. A player may possess a blueprint but still lack permission, reputation, license, materials, space, utilities, or staff to construct it legally or safely.

Access categories should include:

- Public civilian blueprint.
- Faction-approved blueprint.
- Reputation-gated blueprint.
- License-gated blueprint.
- Restricted industrial blueprint.
- Restricted military blueprint.
- Illegal or black-market blueprint.
- Stolen blueprint.
- Salvaged blueprint.
- Reverse-engineered blueprint.
- Quest-awarded blueprint.

The system should distinguish owning a plan from having the right, resources, space, and authority to build it.

Exit criteria:

Blueprint access does not bypass ownership, law, faction standing, construction validation, or restricted-area systems.

## Phase 12 addition - Player/faction construction parity pass

Add a Phase 12 child pass for construction parity.

Required outcome:

Every faction room type that can be generated, built, controlled, or expanded by factions should eventually have a player-accessible construction pathway unless explicitly marked as non-acquirable with a documented reason. Every player-constructible room type should be available to factions through faction construction, facility expansion, worldgen history, or leadership schemes where appropriate.

This includes, but is not limited to:

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

Construction parity should be checked through data, not assumption. If a faction room stamp exists, the project should be able to say whether the player can acquire it, where, how, and under what restrictions.

Exit criteria:

Faction rooms are not permanently locked behind invisible NPC-only rules, and player construction does not exist in a separate toy system that factions cannot use.

## Phase 16 addition - Faction vendor and critical facility generation pass

Add a Phase 16 child pass for faction vendor and critical facility generation.

Required outcome:

Factions should generate vendors and supply facilities appropriate to their identity, territory, economy, and role in the zone. Critical vendors and facilities must exist for survival and conflict loops where the faction has enough infrastructure to plausibly support them.

Critical vendor/facility categories include:

- Food and basic supplies.
- Water or drinkable supply equivalents where appropriate.
- Weapons.
- Ammunition.
- Armor or protective gear.
- Medical supplies.
- Tools and construction supplies.
- Room blueprints.
- Machine blueprints.
- Vehicle or garage-related supplies where appropriate.
- Pet or animal vendors where appropriate.
- Black-market or restricted goods where appropriate.

Faction-specific examples:

- A market-oriented faction should have traders, food supply vendors, general goods, and blueprint vendors.
- A security or military faction should have weapons, ammunition, armor, controlled access, and restricted blueprint vendors.
- An industrial faction should have machine, tool, material, room, and production blueprint vendors.
- A civic or administrative faction should have permits, licenses, property access, and official room blueprints.
- A medical faction should have clinics, medical vendors, medicine, and care-related blueprints.
- An agricultural faction should have food, seeds, farm/garden blueprints, animal rooms, and care infrastructure.
- A pet or animal-focused vendor should have animals, pet supplies, care infrastructure, and pet-room blueprints.

Exit criteria:

Faction territories can produce meaningful vendor access that matches who the faction is and what facilities they control.

## Phase 17 addition - Blueprint economy and faction representative sales pass

Add a Phase 17 child pass for blueprint sales and faction representatives.

Required outcome:

Faction representatives and appropriate vendors should sell or grant blueprints for faction rooms and assets. The player should be able to pursue a desired room by finding the right faction representative, store vendor, black-market contact, reward chain, or salvage/research path.

Blueprint offers should define:

- Vendor or representative type.
- Faction source.
- Room or asset unlocked.
- Price.
- Reputation requirement.
- License or permit requirement.
- Knowledge requirement.
- Quest prerequisite where relevant.
- Stock/availability behavior.
- Whether it is legal, restricted, stolen, counterfeit, military, black-market, or civic.
- Whether the purchase raises heat, suspicion, or faction attention.

Blueprints should include player-facing explanations: what the room is, what it does, why the faction sells it, what it requires to build, and what restrictions apply.

Exit criteria:

The player can intentionally seek out blueprints for faction room types instead of hoping random construction options appear.

## Phase 17 addition - Critical supply vendor pass

Add a Phase 17 child pass for critical supply vendors.

Required outcome:

Factions should have vendors appropriate to their function and survival economy. Food, supply, weapons, and ammunition vendors are not optional flavor once the game expects the player to engage with survival, combat, faction territory, and contracts.

Critical vendor stock should be provenance-aware where possible:

- Food supply should connect to farms, kitchens, markets, trade, hydroponics, scavenging, or faction stores.
- Weapons and ammunition should connect to armories, military depots, workshops, factories, trade routes, black markets, or faction stockpiles.
- Medical goods should connect to clinics, labs, trade, scavenging, or faction stores.
- Construction goods should connect to workshops, depots, industrial suppliers, salvage, or merchants.
- Blueprint stock should connect to faction identity, facilities, knowledge, licensing, and reputation.

Exit criteria:

The player can locate expected basic suppliers in factions that plausibly provide them, and faction supply loops do not feel like arbitrary loot tables detached from facilities.

## Phase 17 addition - Symmetric faction acquisition pass

Add a Phase 17 child pass for faction acquisition symmetry.

Required outcome:

Factions should use comparable acquisition paths to the player when expanding or upgrading. They can buy, build, salvage, contract, produce, confiscate, trade, or scheme for assets. This keeps faction growth from being pure script magic.

Faction acquisition should support:

- Buying blueprints or licenses from superior institutions.
- Using known faction blueprints.
- Constructing rooms from materials and workforce.
- Stocking vendors from provenance-aware supply.
- Repairing or replacing machines and vehicles.
- Salvaging captured rooms or vehicles.
- Deploying specialists to operate facilities.
- Opening or closing vendor access based on control, war, scarcity, or reputation.

Exit criteria:

Factions and players operate under visibly related economic and construction logic, even if factions use abstraction at distance for performance.

## Phase 18 addition - Blueprint, vendor, and parity editor/audit pass

Add a Phase 18 child pass for editor and audit support.

Required outcome:

Future editor/audit surfaces should define and inspect:

- Blueprint definitions.
- Room-to-blueprint mappings.
- Asset-to-blueprint mappings.
- Faction vendor types.
- Vendor stock categories.
- Critical vendor requirements.
- Reputation/license/permit gates.
- Legal, restricted, stolen, black-market, counterfeit, and military access labels.
- Faction construction capability.
- Player construction capability.
- Parity gaps where a faction can have something the player cannot acquire, or the player can build something factions cannot use.
- Facility provenance behind vendor stock.
- Blueprint acquisition paths.

Editor/audit surfaces may expose semantic IDs where explicitly needed for development, but ordinary player UI must hide raw IDs.

Exit criteria:

Blueprints, vendors, and parity can be audited through data surfaces rather than discovered by manually reading scattered source code.

## Phase 18 addition - Infopedia blueprint and vendor reference pass

Add a Phase 18 child pass connecting this milestone to Infopedia depth.

Required outcome:

Infopedia entries for rooms, machines, vehicles, facilities, and major assets should include blueprint and vendor information when known.

Entries should answer:

- Can the player build this?
- Can factions build this?
- Which blueprint unlocks it?
- Who sells or grants the blueprint?
- Which faction type commonly provides it?
- What reputation, license, or permit is required?
- What materials, machines, rooms, or workforce are needed?
- What the room or asset is used for.
- What heat, suspicion, legality, or access issues may apply.

Exit criteria:

The player can use the Infopedia to learn how to acquire or build a room or asset rather than relying on external notes.

## Phase 19 addition - Parity, blueprint, and faction market release audit

Add a Phase 19 child pass for release readiness.

Required audit checks:

- Every faction room stamp has a declared player acquisition status.
- Every player-constructible room has a declared faction-use status.
- Room blueprints have stable semantic IDs and player-facing names.
- Blueprint vendors or representatives exist for major faction room categories.
- Critical vendors exist where faction identity and facilities justify them.
- Food, weapons, ammunition, medical, construction, and blueprint vendors are not placeholder-only.
- Vendor stock is connected to faction identity and facility provenance where possible.
- Blueprint restrictions are visible and understandable.
- Restricted, illegal, military, black-market, stolen, or counterfeit blueprints are labeled appropriately.
- Infopedia entries explain blueprint acquisition where applicable.
- Parity gaps are either fixed or documented as intentional exceptions.

Exit criteria:

The game may claim player/faction parity only when faction rooms, player rooms, blueprint paths, vendor access, critical supplies, and faction acquisition logic can be audited coherently.

## Deferred checkpoint line - Player/faction parity, blueprints, and faction markets

Whatever a faction can have, the player should have a plausible path to obtain; whatever the player can have, factions should have a plausible path to obtain. Faction rooms should not remain permanently NPC-only worldgen decorations. The player should be able to acquire room blueprints through faction representatives, appropriate stores, black markets, quests, salvage, reverse engineering, licensing, permits, or reputation access. Factions should have vendors appropriate to their identity and facilities, including critical vendors for food supplies, weapons, ammunition, medicine, construction goods, and blueprints where the faction's infrastructure supports them.

## Master-plan index patch target

During the next safe `MASTER_DEVELOPMENT_PLAN.md` patch, add a brief milestone reference near `## Durable phase roadmap`:

```text
Detailed phase-group expansions may live in controlled milestone documents when the master plan becomes unsafe to edit monolithically. See `docs/MILESTONE_INDEX.md` for the active milestone map, `docs/MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` for agriculture and pet systems, `docs/MILESTONE_EXAMINATION_AND_INFOPEDIA_SYSTEMS.md` for examination and Infopedia systems, and `docs/MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md` for player/faction parity, blueprint acquisition, and faction vendor systems.
```

Also update the master plan dependency rule to include:

```text
player/faction parity depends on stable room, asset, vendor, blueprint, ownership, access, reputation, and provenance definitions
```

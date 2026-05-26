# Milestone 06 - Vehicles, Structural Scale, and Strategic Power

This ordered milestone document consolidates planning for vehicles, vehicle components, vehicle factories, vehicle ownership, vehicle body schemas, damage and repair, road/parking movement constraints, mounted weapons, structural durability scale, machines/walls/vehicles as heavy targets, vehicle provenance, motor pools, faction doctrine, and sector-level strategic power.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the vehicle/structural-scale/strategic-power slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- Master-plan Phase 10 vehicle provenance, factories, component schemas, and faction vehicle control.
- Master-plan Phase 14 vehicle combat, structural damage, and weapon-scale balance notes.
- Master-plan Phase 16 vehicle facility district and vehicle-provenance worldgen notes.
- Master-plan Phase 17 vehicle-aware contracts and faction vehicle economy notes.
- Master-plan Phase 18 vehicle/component/factory editor notes.
- Master-plan Phase 19 vehicle release-audit notes.
- `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md` where vehicle and component assets are promoted.
- `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md` where produced vehicle/component quality depends on knowledge, skill, machines, materials, and factional mutation.
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where vehicle goods, parts, military assets, and motor-pool supply connect to markets.
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` where vehicle garages, motor pools, restricted blueprints, ownership, and player/faction parity connect to construction.
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`.

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 10 - Vehicle provenance, factories, component schemas, and faction vehicle control.
- Phase 14 - Combat, health, vehicle damage, structural targets, and feedback.
- Phase 16 - World generation, vehicle facilities, roads, depots, garages, and strategic simulation.
- Phase 17 - Economy, contracts, vehicle ownership, faction reputation, and vehicle-related work.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Vehicle and component asset promotion.
- Phase 5 - Operations, logistics, hauling, reservations, route readiness, and delivery intent.
- Phase 6 - Production, factories, machines, recipes, staffing, and quality output.
- Phase 7 - Vehicle access, cargo access, ownership, seized property, and restricted assets.
- Phase 8 - Workforce, crews, drivers, mechanics, guards, and faction motor-pool staff.
- Phase 9 - Vehicle component provenance and supply ecology.
- Phase 11 - Leadership schemes for vehicle construction, deployment, capture, sabotage, loss, and repair.
- Phase 12 - Garages, motor pools, defensive infrastructure, and player-owned vehicles contributing to heat.
- Phase 13 - Vehicle hazards, wrecks, fuel leaks, blocked roads, and damaged hulks.
- Phase 15 - Ranks, roles, and authority to command, repair, crew, or deploy vehicles.

## Core doctrine

Vehicles are not decorative sprites. Vehicles are faction assets, player assets, transport entities, production-chain outputs, repair projects, body-schema targets, restricted property, tactical objects, hazards, and strategic power markers.

A car, truck, bike, APC, or tank should have meaning beyond occupying a tile. It should know what it is, who owns it, who made it, what components it contains, what condition it is in, who may use it, where it can move, what it can carry, how it can fail, how it can be repaired, what faction doctrine values it, and how it changes balance of power.

## Structural scale doctrine

Handheld weapons, mounted weapons, vehicles, walls, machines, and structural objects must not share the same damage scale.

A pocket knife, dagger, or light melee weapon should not be a meaningful anti-vehicle, anti-machine, or anti-wall tool. A player with a dagger should require effectively absurd repeated effort, on the order of a thousand concentrated turns, to do meaningful damage to a vehicle, heavy machine, or block wall. A powerful anti-armor or plasma-class weapon may do meaningful damage after a far smaller number of attacks, such as roughly a dozen, depending on target, armor, component, and weapon class.

Mounted vehicle weapons should be roughly an order of magnitude above the deadliest player/entity-wielded weapons. Vehicle durability should be roughly an order of magnitude above ordinary player/entity endurance and health. Industrial machines and structural walls should sit on comparable structural scales and expose damage through condition states, component damage, disabled behavior, and repair needs rather than ordinary character health bars.

## Dependency rule

Vehicle systems depend on promoted vehicle and component assets, component recipes, manufacturer/faction definitions, ownership/access categories, body schemas, road/parking spatial rules, structural durability tiers, vehicle-capable UI inspection, and save/load-safe ledgers.

Vehicle strategic power depends on faction doctrine, motor pools, crews, mechanics, fuel or power supply, road control, garage/depot facilities, leadership schemes, and sector-level tracking of military assets such as APCs and tanks.

## Phase 10 - Vehicle definitions, provenance, factories, and ownership

### Phase 10.1 - Vehicle asset and class taxonomy pass

Vehicle assets should become semantic vehicle definitions before gameplay systems consume them.

Vehicle classes include:

- Bicycle or primitive light transit where setting supports it.
- Bike or motorcycle.
- Civilian car.
- Utility vehicle.
- Truck.
- Cargo truck.
- Industrial hauler.
- Emergency vehicle.
- Security patrol vehicle.
- Armored car.
- APC.
- Tank.
- Heavy military vehicle.
- Wreck.
- Salvage hulk.
- Immobile vehicle feature.

Each class should define expected footprint, movement constraints, cargo role, crew needs, durability tier, legal/restricted status, faction-use expectations, and whether it can be player-owned.

Exit criteria:

Vehicle classes are stable enough for assets, factories, ownership, worldgen, combat, and inspection systems to reference.

### Phase 10.2 - Vehicle component schema pass

Vehicles should use inspectable body/component schemas rather than flat health values.

Component categories include:

- Chassis or frame.
- Cab or rider station.
- Engine or powerplant.
- Transmission.
- Wheels or tracks.
- Suspension.
- Armor.
- Hull.
- Turret.
- Weapon mounts.
- Sensors.
- Fuel or power system.
- Cargo area.
- Crew compartment.
- Doors or hatches.
- External fittings.
- Repairable subassemblies.

Exit criteria:

Vehicles can be inspected, damaged, repaired, disabled, salvaged, and upgraded through major component areas.

### Phase 10.3 - Vehicle component integrity and endurance pass

Each major component should have integrity, endurance, damage thresholds, disabled states, reduced-performance states, catastrophic failure states, and repair/replacement requirements.

Damage should be able to affect:

- Movement.
- Steering.
- Speed.
- Fuel or power use.
- Cargo capacity.
- Crew safety.
- Weapon use.
- Noise.
- Visibility.
- Armor protection.
- Repair cost.
- Salvage value.
- Tactical value.

Exit criteria:

Vehicles can fail in meaningful ways without every hit simply subtracting from one generic health pool.

### Phase 10.4 - Manufacturer, model, and variant pass

Vehicles should track manufacturer, model, variant, production batch, and faction-style modifications where relevant.

Possible producer entities:

- Corporate motorworks.
- Industrial guilds.
- Faction workshops.
- Military contractors.
- Noble carriage/vehicle houses.
- Salvage yards.
- Black-market refitters.
- Improvised garages.
- State or civic depots.

Variants may affect durability, repairability, compatibility, cargo capacity, speed, noise, fuel use, armor, weapon mounts, legality, value, and faction preference.

Exit criteria:

A vehicle can be identified as a produced object with model, variant, origin, and faction/manufacturer traits rather than a generic car or tank sprite.

### Phase 10.5 - Vehicle component factory pass

Vehicle components should have factory and production chains where appropriate.

Component factory outputs may include:

- Engines.
- Transmissions.
- Wheels.
- Tracks.
- Hull frames.
- Armor plates.
- Suspension.
- Weapon mounts.
- Sensors.
- Power systems.
- Cargo beds.
- Crew compartments.
- Repair parts.
- Replacement assemblies.

Exit criteria:

Vehicle production depends on actual component supply rather than free vehicle spawning.

### Phase 10.6 - Vehicle final assembly factory pass

Vehicle final-assembly factories should consume component provenance, workforce, machines, facility quality, manufacturer identity, and faction doctrine to produce complete or refurbished vehicles.

Assembly output should support:

- New vehicle.
- Refurbished vehicle.
- Improvised vehicle.
- Military conversion.
- Armored conversion.
- Damaged but functional vehicle.
- Counterfeit or misdeclared vehicle.
- Faction-custom variant.

Exit criteria:

Vehicles can emerge from factories and workshops with provenance, quality, and variation.

### Phase 10.7 - Vehicle provenance ledger pass

Vehicle ledgers should record:

- Manufacturer.
- Model.
- Variant.
- Production batch.
- Facility of origin.
- Component sources.
- Current owner.
- Former owners.
- Faction assignment.
- Crew assignment.
- Cargo ownership.
- Damage history.
- Repair history.
- Capture history.
- Salvage history.
- Deployment history.
- Legal/restricted/stolen/seized status.

Exit criteria:

Vehicles can participate in quests, market value, faction schemes, inspection, theft, repair, and strategic ledgers as persistent assets.

### Phase 10.8 - Vehicle ownership and access pass

Vehicle ownership must integrate with property, access, cargo, and faction systems.

Access categories include:

- Player-owned.
- Faction-owned.
- Public service.
- Private civilian.
- Commercial.
- Military restricted.
- Noble-owned.
- Stolen.
- Seized.
- Abandoned.
- Salvage claim.
- Motor-pool assigned.
- Quest-bound.
- Black-market.

Vehicle access should distinguish:

- Driving/operation permission.
- Passenger permission.
- Cargo access.
- Repair permission.
- Refueling permission.
- Command/deployment permission.
- Seizure/confiscation rights.

Exit criteria:

Actors cannot freely use every vehicle, cargo hold, motor-pool asset, or restricted military vehicle without access checks.

## Phase 10 - Vehicle movement, use, loss, and salvage

### Phase 10.9 - Vehicle transit and routing pass

Vehicles should be transport entities between locations inside zones and across zones where infrastructure allows.

Transit should consider:

- Roads.
- Alleys.
- Vehicle lanes.
- Garages.
- Depots.
- Ramps.
- Gates.
- Checkpoints.
- Parking lots.
- Vehicle yards.
- Blocked routes.
- Road control.
- Fuel or power.
- Driver availability.
- Security conditions.
- Vehicle size.

Exit criteria:

Vehicles move through infrastructure and logistics rules rather than free-form teleportation or unrestricted map traversal.

### Phase 10.10 - Road, parking, and sidewalk constraint pass

Vehicles are true endgame and infrastructure-limited assets. Ordinary vehicles may travel on roads, alleys, lanes, garages, depots, ramps, vehicle yards, and parking lots only. They cannot be driven freely anywhere on the map.

At most, owned vehicles may be allowed to park on sidewalks or designated curb/sidewalk-adjacent parking spots when the player exits, with the vehicle automatically resolving to a legal nearby parking position.

Exit criteria:

Vehicles do not become free-roaming traversal tools that ignore the spatial structure of the city.

### Phase 10.11 - Vehicle repair and maintenance pass

Vehicle repair should use items, tools, skilled labor, workshops, spare parts, replacement components, salvage, and time costs.

Repair types include:

- Field patch.
- Garage repair.
- Component replacement.
- Armor patching.
- Wheel/track repair.
- Engine repair.
- Weapon mount repair.
- Sensor repair.
- Fuel/power system repair.
- Full refurbishment.
- Salvage conversion.

Exit criteria:

Damaged vehicles create repair projects, economic demand, and strategic decisions.

### Phase 10.12 - Vehicle loss and salvage pass

Destroyed, abandoned, captured, or disabled vehicles should become salvage, wrecks, repair projects, trophies, hazards, or contested assets depending on context.

Vehicle loss outcomes include:

- Burned-out wreck.
- Disabled but repairable vehicle.
- Salvage hulk.
- Captured motor-pool asset.
- Looted vehicle.
- Blocked road obstacle.
- Fuel leak or hazard.
- Faction trophy.
- Quest objective.
- Strategic asset loss.

Exit criteria:

Vehicle destruction and capture matter beyond deleting an entity.

## Phase 14 - Vehicle combat and structural damage scale

### Phase 14.1 - Vehicle combat bridge pass

Vehicle systems must connect to combat without collapsing into ordinary actor combat.

Vehicle combat should support:

- Hits to armor.
- Hits to components.
- Crew harm where applicable.
- Passenger harm where applicable.
- Mobility damage.
- Weapon disablement.
- Fuel/power damage.
- Fire or explosion risk where appropriate.
- Ramming or crash damage.
- Cover interaction.
- Structural target interaction.

Exit criteria:

Vehicle combat can damage vehicles, crew, and structural targets through appropriate scale and component logic.

### Phase 14.2 - Mounted weapon scale pass

Mounted weapons should sit above handheld/entity-wielded weapons in force, reach, recoil, ammunition demand, noise, risk, and structural effect.

Mounted weapons may include:

- Heavy stubber or equivalent.
- Autocannon or equivalent.
- Heavy bolter or equivalent setting-safe equivalent.
- Heavy flamer or equivalent.
- Anti-armor mount.
- Tank cannon or heavy structural weapon.
- Machine-mounted industrial cutters or hazards.

Exit criteria:

Mounted weapons are balance-shaping assets, not just rifles with bigger icons.

### Phase 14.3 - Structural target scale pass

Walls, machines, vehicles, doors, gates, barricades, heavy crates, and industrial structures should use structural durability tiers.

Damage should consider:

- Target material.
- Structural tier.
- Weapon class.
- Armor penetration.
- Repetition.
- Component exposure.
- Repairability.
- Secondary hazards.
- Whether the action is plausible or absurd.

Exit criteria:

The player cannot casually destroy tanks, machines, or walls with ordinary handheld attacks, while true anti-armor/structural tools have distinct value.

### Phase 14.4 - Vehicle inspection and combat feedback pass

Player-facing feedback should explain vehicle and structural damage in readable terms.

Examples:

- `Your shots scar the armor but do not penetrate.`
- `The track assembly is damaged; movement is impaired.`
- `The engine compartment is smoking.`
- `The weapon mount is disabled.`
- `This tool is not meaningful against that armor.`
- `The wall is chipped, but not structurally weakened.`
- `Anti-armor impact: hull integrity compromised.`

Exit criteria:

Vehicle and structural combat feedback is readable without raw numeric debug leakage.

## Phase 16 - Worldgen, facilities, roads, and strategic placement

### Phase 16.1 - Vehicle facility district pass

World generation should place vehicle-supporting facilities where faction history and production capacity justify them.

Possible facilities:

- Garages.
- Depots.
- Motor pools.
- Repair shops.
- Vehicle yards.
- Military vehicle bays.
- Convoy staging areas.
- Salvage yards.
- Vehicle component factories.
- Fuel/power depots.
- Checkpoints.
- Road control posts.
- Noble vehicle houses.
- Black-market chop shops.

Exit criteria:

Vehicle systems have room/facility support in world generation.

### Phase 16.2 - Vehicle-provenance worldgen integration pass

World generation should consume vehicle provenance where possible.

Historical traces include:

- Old motor pools.
- Wrecks.
- Abandoned vehicles.
- Captured vehicles.
- Garage expansion.
- Vehicle shortages.
- Road-control changes.
- Tank/APC losses.
- Convoy routes.
- Salvage piles.
- Damaged depots.

Exit criteria:

Vehicles help explain why zones and roads look the way they do.

### Phase 16.3 - Road and parking worldgen support pass

Roaded zones must reserve enough vehicle-appropriate space for movement and storage.

Support elements include:

- Roads.
- Sidewalk boundaries.
- Alleys.
- Parking lots.
- Curb parking.
- Garages.
- Loading bays.
- Vehicle doors/gates.
- Turning and staging spaces where possible.
- Blockages and chokepoints.

Exit criteria:

Vehicle placement does not contradict the zone's physical layout.

## Phase 17 - Vehicle economy, contracts, and faction doctrine

### Phase 17.1 - Faction vehicle doctrine pass

Factions should value vehicles based on identity and strategy.

Doctrine categories include:

- Transport.
- Patrol.
- Cargo.
- Assault.
- Defense.
- Intimidation.
- Evacuation.
- Logistics.
- Production support.
- Convoy use.
- Route control.
- Strategic projection.
- Noble prestige.
- Black-market smuggling.

Exit criteria:

Factions do not treat all vehicles as interchangeable generic assets.

### Phase 17.2 - Military asset balance pass

APCs, tanks, and heavy military vehicles should affect sector-level balance of power.

Tracked effects may include:

- Assault confidence.
- Defensive strength.
- Deterrence.
- Negotiation leverage.
- Route control.
- Raid capability.
- Faction fear/respect.
- Player heat if owned.
- Strategic scheme options.

Exit criteria:

Heavy vehicles are not merely local combat pieces; they affect faction power.

### Phase 17.3 - Leadership vehicle scheme pass

Faction leaders should plan around vehicle assets.

Schemes include:

- Build vehicle.
- Acquire vehicle.
- Repair vehicle.
- Capture vehicle.
- Destroy rival vehicle.
- Protect motor pool.
- Expand garage.
- Escort convoy.
- Sabotage route.
- Hide restricted vehicle.
- Smuggle with vehicle.
- Deploy APCs or tanks to shift territorial control.

Exit criteria:

Vehicle assets participate in faction planning and active world change.

### Phase 17.4 - Vehicle-aware contract pass

Contracts should support:

- Vehicle delivery.
- Vehicle repair.
- Vehicle theft or recovery.
- Convoy escort.
- Motor-pool supply.
- Component acquisition.
- Vehicle sabotage.
- Vehicle salvage.
- Fuel/power delivery.
- Route clearing.
- Military buildup.
- Black-market chop-shop work.

Exit criteria:

Vehicle systems produce player-facing work and risk.

## Phase 18 - Editor, audit, and Infopedia support

### Phase 18.1 - Vehicle/component/factory editor pass

Future data/editor surfaces should define:

- Vehicle classes.
- Vehicle variants.
- Manufacturers.
- Body schemas.
- Components.
- Component factories.
- Final assembly factories.
- Repair recipes.
- Cargo rules.
- Crew rules.
- Access categories.
- Movement constraints.
- Durability tiers.
- Mounted weapon mounts.
- Faction doctrine.
- Strategic value.

Exit criteria:

Vehicles can be inspected and extended through owned data surfaces rather than central-code sprawl.

### Phase 18.2 - Vehicle audit pass

Audit surfaces should inspect:

- Vehicle provenance.
- Ownership.
- Access rules.
- Component state.
- Damage state.
- Repair needs.
- Cargo.
- Crew.
- Motor-pool assignment.
- Strategic faction value.
- Road/parking legality.
- Save/load footprint.

Exit criteria:

Vehicle systems can be debugged and validated without raw player-facing leakage.

### Phase 18.3 - Vehicle Infopedia pass

Infopedia entries should explain:

- Vehicle class.
- What it does.
- Movement restrictions.
- Ownership restrictions.
- Cargo or crew capacity.
- Body components.
- Repair needs.
- Required facilities.
- Manufacturer or faction variants.
- Whether it is civilian, commercial, industrial, restricted, military, noble, stolen, or black-market.
- What components are used to build or repair it.
- What factories, garages, depots, or vendors support it.
- Why heavy vehicles matter strategically.

Exit criteria:

The player can look up vehicle and structural-scale rules without external notes.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Vehicle assets and component assets have semantic definitions.
- Vehicle classes, variants, and manufacturers are represented where implemented.
- Vehicle provenance ledgers exist where vehicles are persistent assets.
- Vehicle ownership and access rules are distinct from ordinary item ownership.
- Vehicle cargo and repair permissions are governed.
- Vehicles are constrained to roads, alleys, depots, garages, vehicle yards, parking lots, and legal curb/sidewalk behavior where applicable.
- Vehicle body schemas and component integrity are represented where implemented.
- Vehicle damage, repair, loss, capture, and salvage produce meaningful states.
- Mounted weapons and structural targets use distinct scale logic.
- Ordinary handheld weapons do not casually damage vehicles, machines, or structural walls.
- APCs, tanks, and heavy military vehicles affect faction power where implemented.
- Vehicle facilities appear in worldgen only where supported by assets and rules.
- Vehicle contracts and schemes can reference vehicle state where implemented.
- Infopedia entries explain vehicle and structural-scale rules.
- Player-facing text avoids raw IDs and placeholder labels.

Exit criteria:

The game may claim vehicle systems, structural damage scale, and strategic vehicle power only when vehicles are inspectable, ownable, damageable, repairable, spatially constrained, provenance-tracked, faction-relevant, and balanced against handheld weapons, machines, and walls.

## Non-goals for this milestone

This milestone does not require every vehicle type to be implemented at once. It requires the system boundaries and audit rules that prevent vehicles from becoming decorative sprites or unbalanced traversal tools.

This milestone does not make tanks common, cheap, or easy to own. It makes their exceptional power and restrictions explicit.

This milestone does not require all distant vehicles to be full component simulations. Distant or background vehicles may remain aggregated until player visibility, strategic relevance, damage, ownership, or quest interaction requires detail.

This milestone does not replace combat, construction, economy, or worldgen milestones. It defines how vehicles and structural scale connect to them.

## Deferred checkpoint summary

Vehicles should be provenance-tracked, inspectable, buildable, damageable, repairable, ownable, transport-capable, and strategically meaningful faction assets. Civilian vehicles, commercial vehicles, industrial vehicles, APCs, and tanks are not interchangeable. Heavy military assets should affect sector-level power. Vehicles must remain transit-limited to roads, alleys, garages, depots, vehicle yards, parking lots, and legal curb/sidewalk parking behavior. Handheld weapons, mounted weapons, vehicles, machines, and walls must occupy intentionally different damage and durability scales so ordinary tools do not threaten tanks, while true anti-armor and structural weapons have distinct value.

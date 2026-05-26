# Milestone 06 - Vehicles, Structural Scale, and Strategic Power

This ordered milestone document consolidates planning for vehicles, vehicle components, vehicle factories, vehicle ownership, vehicle body schemas, damage and repair, road/parking movement constraints, mounted weapons, structural durability scale, machines/walls/vehicles as heavy targets, vehicle provenance, motor pools, faction doctrine, sector-level strategic power, vehicle operation feedback, vehicle ambient sound, headlights/light cones, and planned vehicle movement controls.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the vehicle/structural-scale/strategic-power/vehicle-presentation slice of that roadmap.

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
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` where vehicle inspection, player-facing feedback, input grammar, and menu/readability rules apply.
- `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md` where produced vehicle/component quality depends on knowledge, skill, machines, materials, and factional mutation.
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where vehicle goods, parts, military assets, and motor-pool supply connect to markets.
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` where vehicle garages, motor pools, restricted blueprints, ownership, and player/faction parity connect to construction.
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`.

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 10 - Vehicle provenance, factories, component schemas, operation feedback, movement controls, and faction vehicle control.
- Phase 14 - Combat, health, vehicle damage, structural targets, and feedback.
- Phase 16 - World generation, vehicle facilities, roads, depots, garages, and strategic simulation.
- Phase 17 - Economy, contracts, vehicle ownership, faction reputation, and vehicle-related work.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Vehicle, component, headlight, sound, overlay, and operation-indicator asset promotion.
- Phase 4 - UI, input, rendering, planned movement, driving feedback, sound feedback, and player-facing readability.
- Phase 5 - Operations, logistics, hauling, reservations, route readiness, and delivery intent.
- Phase 6 - Production, factories, machines, recipes, staffing, and quality output.
- Phase 7 - Vehicle access, cargo access, ownership, seized property, and restricted assets.
- Phase 8 - Workforce, crews, drivers, mechanics, guards, and faction motor-pool staff.
- Phase 9 - Vehicle component provenance and supply ecology.
- Phase 11 - Leadership schemes for vehicle construction, deployment, capture, sabotage, loss, and repair.
- Phase 12 - Garages, motor pools, defensive infrastructure, and player-owned vehicles contributing to heat.
- Phase 13 - Vehicle hazards, wrecks, fuel leaks, blocked roads, damaged hulks, and visibility/lighting effects.
- Phase 15 - Ranks, roles, and authority to command, repair, crew, or deploy vehicles.

## Core doctrine

Vehicles are not decorative sprites. Vehicles are faction assets, player assets, transport entities, production-chain outputs, repair projects, body-schema targets, restricted property, tactical objects, hazards, and strategic power markers.

A car, truck, bike, APC, or tank should have meaning beyond occupying a tile. It should know what it is, who owns it, who made it, what components it contains, what condition it is in, who may use it, where it can move, what it can carry, how it can fail, how it can be repaired, what faction doctrine values it, and how it changes balance of power.

## Vehicle presentation doctrine

Operating vehicles must communicate that they are operating. A vehicle that is running, idling, reversing, accelerating, damaged, immobilized, or parked should produce player-facing feedback through motion cues, icons, light cones, sound hooks, inspection text, and control behavior where applicable.

At minimum, an actively operated vehicle should have a slight pulsing operation indicator: a compact icon or marker that expands and contracts subtly so the player can identify that the vehicle is active without confusing it for a selection marker, combat target marker, quest marker, or debug overlay. The pulse must be readable but not obnoxious.

A vehicle facing a direction of travel should be able to project a forward-facing light cone when headlights, lamps, or equivalent illumination are active. This light cone should communicate facing, intended movement direction, visibility, and night/darkness interaction without becoming a blinding overlay or obscuring map readability.

Vehicles should have ambient running sound hooks where sound support exists. An idling vehicle, moving vehicle, damaged engine, heavy truck, bike, APC, and tank should not all feel silent and identical. Sound must be distance-aware, loop-safe, and stoppable so it does not become audio spam.

Driving should not be a blind directional keypress when a better movement-planning system exists. When the player is operating a vehicle, they should be able to use the planned movement system to preview, fine-tune, and confirm a planned movement direction or route segment where feasible, especially because vehicles are road-limited, larger than actors, more dangerous, and more expensive to repair.

## Structural scale doctrine

Handheld weapons, mounted weapons, vehicles, walls, machines, and structural objects must not share the same damage scale.

A pocket knife, dagger, or light melee weapon should not be a meaningful anti-vehicle, anti-machine, or anti-wall tool. A player with a dagger should require effectively absurd repeated effort, on the order of a thousand concentrated turns, to do meaningful damage to a vehicle, heavy machine, or block wall. A powerful anti-armor or plasma-class weapon may do meaningful damage after a far smaller number of attacks, such as roughly a dozen, depending on target, armor, component, and weapon class.

Mounted vehicle weapons should be roughly an order of magnitude above the deadliest player/entity-wielded weapons. Vehicle durability should be roughly an order of magnitude above ordinary player/entity endurance and health. Industrial machines and structural walls should sit on comparable structural scales and expose damage through condition states, component damage, disabled behavior, and repair needs rather than ordinary character health bars.

## Dependency rule

Vehicle systems depend on promoted vehicle and component assets, component recipes, manufacturer/faction definitions, ownership/access categories, body schemas, road/parking spatial rules, structural durability tiers, vehicle-capable UI inspection, operation-feedback overlays, light-cone rendering, sound hooks, planned-movement input support, and save/load-safe ledgers.

Vehicle strategic power depends on faction doctrine, motor pools, crews, mechanics, fuel or power supply, road control, garage/depot facilities, leadership schemes, and sector-level tracking of military assets such as APCs and tanks.

Vehicle operation feedback depends on a shared rendering/input/sound grammar. The same operation state that drives movement and fuel/power use should also drive the subtle pulsing active icon, headlight/light cone, ambient running sound, and planned movement preview. Feedback should not be a separate decorative state that lies about the real vehicle state.

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

Each class should define expected footprint, movement constraints, cargo role, crew needs, durability tier, legal/restricted status, faction-use expectations, whether it can be player-owned, whether it supports headlights/light cones, whether it supports ambient running sound, and whether it uses planned movement controls.

Exit criteria:

Vehicle classes are stable enough for assets, factories, ownership, worldgen, combat, inspection, visual feedback, sound feedback, and driving controls to reference.

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
- Headlights, lamps, optics, or equivalent forward illumination.
- Horn, siren, engine, exhaust, track, tire, or mechanical sound source where relevant.
- Cargo area.
- Crew compartment.
- Doors or hatches.
- External fittings.
- Repairable subassemblies.

Exit criteria:

Vehicles can be inspected, damaged, repaired, disabled, salvaged, and upgraded through major component areas, including visibility and sound-related components where implemented.

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
- Headlights or forward illumination.
- Engine, track, tire, horn, siren, or running sound state.
- Noise.
- Visibility.
- Armor protection.
- Repair cost.
- Salvage value.
- Tactical value.

Exit criteria:

Vehicles can fail in meaningful ways without every hit simply subtracting from one generic health pool, and damaged visibility/sound components can change player-facing feedback.

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

Variants may affect durability, repairability, compatibility, cargo capacity, speed, noise, fuel use, armor, weapon mounts, legality, value, headlight strength, engine sound profile, operation indicator style, and faction preference.

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
- Headlight/lamp units.
- Horns/sirens/sound-producing systems where represented as components.
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

## Phase 10 - Vehicle movement, use, loss, feedback, and salvage

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

### Phase 10.11 - Vehicle operation visual feedback pass

Operating vehicles should have a readable active-state indicator.

Required behavior:

- A running or actively operated vehicle should display a subtle pulsing icon or operation marker.
- The pulse should expand and contract slightly to show active operation.
- The indicator should be visually distinct from selection cursors, quest markers, combat targeting, debug overlays, and ownership icons.
- The pulse should reduce or stop when the vehicle is parked, disabled, abandoned, unpowered, or no longer operated.
- Damaged or malfunctioning vehicles may use a degraded, warning, stuttering, or irregular operation indicator where supported.
- The indicator should respect zoom, old-machine performance, and overlay clutter limits.

Exit criteria:

The player can tell at a glance which nearby vehicle is operating without opening inspection or reading a log.

### Phase 10.12 - Headlight and facing light-cone pass

Operating vehicles should be able to show forward-facing illumination when headlights, lamps, or equivalent optics are active.

Required behavior:

- A vehicle should project a forward light cone in its facing or planned movement direction when headlights are active.
- The cone should help communicate direction of travel and visibility without obscuring terrain, actors, hazards, or UI markers.
- Light cone range, width, brightness, and color behavior should be data-driven where possible.
- Broken headlights should reduce, flicker, skew, or remove the cone where implemented.
- Headlights should interact with darkness, fog, smoke, underground zones, sewers, and maintenance spaces where the lighting system supports it.
- Heavy vehicles, bikes, civilian cars, trucks, APCs, tanks, and improvised vehicles may have different light profiles.

Exit criteria:

Vehicle facing and night/dark movement are communicated visually through forward illumination instead of hidden state.

### Phase 10.13 - Ambient running sound and vehicle audio feedback pass

Vehicles should expose ambient running sound hooks where sound support exists.

Sound states may include:

- Engine start.
- Engine stop.
- Idle loop.
- Movement loop.
- Reverse or backing cue where appropriate.
- Heavy engine idle.
- Track clatter.
- Tire movement.
- Damaged engine sputter.
- Horn or siren where implemented.
- Crash impact.
- Brake or skid where implemented.
- Disabled engine failure.

Required constraints:

- Loops must start and stop reliably.
- Sound should be distance-aware and not spam repeated start calls.
- Sound should respect mute/options settings.
- Sound should degrade or change when the engine, wheels, tracks, or power systems are damaged where supported.
- Sound should not continue after save/load, zone transition, parking, destruction, or ownership changes unless the vehicle is truly still operating.

Exit criteria:

Vehicles provide ambient operational feedback without creating audio spam or stale looping sounds.

### Phase 10.14 - Planned vehicle movement and fine-tuned driving control pass

Driving should use the planned movement system where feasible rather than forcing blind directional keypresses.

Required behavior:

- When operating a vehicle, the player should be able to preview a planned movement direction or short route segment before committing.
- Planned movement should account for roads, lanes, alleys, garages, depots, parking spaces, vehicle footprint, turning limits where implemented, blocked cells, and forbidden surfaces.
- The preview should show the intended destination or path and explain invalid movement before the player commits.
- Fine-tuned movement should allow careful alignment for parking, turning, entering garages, leaving depots, approaching loading bays, and avoiding collisions.
- Simple directional input may remain as a quick control, but should feed the same validation/path preview rules where practical rather than bypassing them.
- Cancel/confirm behavior should be clear and consistent with player movement planning.

Exit criteria:

Driving a vehicle gives the player enough preview and control to avoid blind directional mistakes with expensive, road-limited, high-consequence assets.

### Phase 10.15 - Vehicle repair and maintenance pass

Vehicle repair should use items, tools, skilled labor, workshops, spare parts, replacement components, salvage, and time costs.

Repair types include:

- Field patch.
- Garage repair.
- Component replacement.
- Armor patching.
- Wheel/track repair.
- Engine repair.
- Headlight/lamp repair.
- Horn/siren/sound system repair where represented.
- Weapon mount repair.
- Sensor repair.
- Fuel/power system repair.
- Full refurbishment.
- Salvage conversion.

Exit criteria:

Damaged vehicles create repair projects, economic demand, strategic decisions, and visible/sound feedback changes when relevant components fail.

### Phase 10.16 - Vehicle loss and salvage pass

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
- Headlight or sensor damage.
- Engine/noise-state damage.
- Fire or explosion risk where appropriate.
- Ramming or crash damage.
- Cover interaction.
- Structural target interaction.

Exit criteria:

Vehicle combat can damage vehicles, crew, structural targets, visibility components, and sound/operation state through appropriate scale and component logic.

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
- `The headlights are shattered; forward visibility is reduced.`
- `The engine sputters unevenly.`
- `This tool is not meaningful against that armor.`
- `The wall is chipped, but not structurally weakened.`
- `Anti-armor impact: hull integrity compromised.`

Exit criteria:

Vehicle and structural combat feedback is readable without raw numeric debug leakage.

### Phase 14.5 - Driving feedback and collision warning pass

Driving feedback should explain invalid movement, likely collisions, blocked routes, and dangerous vehicle actions before or immediately after the player commits.

Feedback examples:

- `The vehicle cannot leave the road here.`
- `The turn is blocked by the parked truck.`
- `The planned route clips the wall.`
- `The vehicle is too large for that alley.`
- `Parking position found at the curb.`
- `No legal parking position nearby.`
- `Damaged steering makes that maneuver unsafe.`
- `Headlights are off; visibility ahead is poor.`

Exit criteria:

Vehicle operation failure feels like readable vehicle logic rather than silent refusal or arbitrary bumping.

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

### Phase 16.4 - Vehicle lighting and nighttime route support pass

Roaded and vehicle-supporting zones should support vehicle headlights and route lighting where darkness or visibility matters.

Support elements include:

- Road darkness state.
- Tunnel/sewer/maintenance darkness interaction.
- Headlight visibility cones.
- Streetlight or facility-light interaction where implemented.
- Fog, smoke, dust, or darkness modifiers where implemented.
- Wrecked or disabled vehicles with broken lamps where appropriate.

Exit criteria:

Vehicle lighting has worldgen contexts where it matters rather than being a decorative cone in fully lit rooms only.

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
- Headlight/lamp or sensor repair.
- Road lighting restoration where implemented.
- Military buildup.
- Black-market chop-shop work.

Exit criteria:

Vehicle systems produce player-facing work and risk.

## Phase 18 - Editor, audit, Infopedia, and vehicle feedback support

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

### Phase 18.2 - Vehicle operation feedback editor pass

Editor/data surfaces should define vehicle operation feedback.

Definitions should include:

- Operation indicator asset or icon.
- Pulse size.
- Pulse speed.
- Visibility rules.
- Running/idling/moving/disabled indicator states.
- Headlight/light-cone asset or procedural shape.
- Light-cone range.
- Light-cone width.
- Light-cone brightness/opacity.
- Facing/source offset.
- Broken/flicker/degraded light state.
- Ambient sound profile.
- Start/stop sound hooks.
- Idle/move/damaged sound loops.
- Sound range.
- Mute/options compliance.
- Planned movement preview style.
- Invalid movement warning text.

Exit criteria:

Vehicle feedback is data-owned and auditable instead of scattered one-off rendering/audio code.

### Phase 18.3 - Vehicle audit pass

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
- Operation indicator state.
- Headlight/light-cone state.
- Ambient sound state.
- Planned movement validation state.
- Save/load footprint.

Exit criteria:

Vehicle systems can be debugged and validated without raw player-facing leakage.

### Phase 18.4 - Vehicle Infopedia pass

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
- What headlights/light cones indicate.
- What the pulsing operation icon means.
- How planned vehicle movement differs from ordinary walking.
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
- Operating vehicles display a subtle pulsing active-operation indicator where implemented.
- Vehicle operation indicators are distinct from quest, targeting, selection, ownership, and debug markers.
- Vehicles with active headlights or equivalent illumination show a forward-facing light cone where implemented.
- Headlight cones communicate facing/travel direction without obscuring gameplay.
- Vehicle ambient running sounds start, loop, attenuate, mute, stop, and survive save/load correctly where implemented.
- Vehicle sound state does not continue after parking, destruction, zone transition, or operation stop unless the vehicle truly remains running.
- Vehicle driving supports planned/fine-tuned movement preview where implemented.
- Planned vehicle movement validates road/parking/footprint/blockage rules before commitment where feasible.
- Invalid driving movement has readable feedback.
- Mounted weapons and structural targets use distinct scale logic.
- Ordinary handheld weapons do not casually damage vehicles, machines, or structural walls.
- APCs, tanks, and heavy military vehicles affect faction power where implemented.
- Vehicle facilities appear in worldgen only where supported by assets and rules.
- Vehicle contracts and schemes can reference vehicle state where implemented.
- Infopedia entries explain vehicle operation feedback, movement constraints, and structural-scale rules.
- Player-facing text avoids raw IDs and placeholder labels.

Exit criteria:

The game may claim vehicle systems, structural damage scale, strategic vehicle power, and readable vehicle operation only when vehicles are inspectable, ownable, damageable, repairable, spatially constrained, provenance-tracked, faction-relevant, visually communicative, sound-aware, planned-movement compatible, and balanced against handheld weapons, machines, and walls.

## Non-goals for this milestone

This milestone does not require every vehicle type to be implemented at once. It requires the system boundaries and audit rules that prevent vehicles from becoming decorative sprites or unbalanced traversal tools.

This milestone does not make tanks common, cheap, or easy to own. It makes their exceptional power and restrictions explicit.

This milestone does not require all distant vehicles to be full component simulations. Distant or background vehicles may remain aggregated until player visibility, strategic relevance, damage, ownership, or quest interaction requires detail.

This milestone does not require vehicle audio, headlights, and planned movement to be implemented before basic vehicle ownership and movement exist. It records them as required quality-of-life targets before vehicle systems can claim polish/readiness.

This milestone does not replace combat, construction, economy, UI, input, sound, or worldgen milestones. It defines how vehicles and structural scale connect to them.

## Deferred checkpoint summary

Vehicles should be provenance-tracked, inspectable, buildable, damageable, repairable, ownable, transport-capable, and strategically meaningful faction assets. Civilian vehicles, commercial vehicles, industrial vehicles, APCs, and tanks are not interchangeable. Heavy military assets should affect sector-level power. Vehicles must remain transit-limited to roads, alleys, garages, depots, vehicle yards, parking lots, and legal curb/sidewalk parking behavior.

Operating vehicles should provide readable user-facing feedback. A running or operated vehicle should have a subtle pulsing icon that expands and contracts. Vehicles with headlights or equivalent illumination should project a forward-facing light cone that indicates facing/travel direction. Vehicles should have ambient running sound hooks such as idle, movement, damaged engine, heavy track, horn, siren, start, and stop states where sound support exists. Driving should support planned/fine-tuned movement preview so vehicle control is not merely blind directional keypress input, especially for expensive, road-limited, high-consequence assets.

Handheld weapons, mounted weapons, vehicles, machines, and walls must occupy intentionally different damage and durability scales so ordinary tools do not threaten tanks, while true anti-armor and structural weapons have distinct value.

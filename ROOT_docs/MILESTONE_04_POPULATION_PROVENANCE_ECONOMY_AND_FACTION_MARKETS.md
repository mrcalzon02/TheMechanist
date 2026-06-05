# Milestone 04 - Population Provenance, Economy, Faction Markets, Deferred Sector Simulation, and World Events

This ordered milestone document consolidates the population, provenance, economy, faction vendor, illicit-market, noble-luxury, narcotics, draught, faction-market, raw-material-source, out-of-sector shipment, reinforcement-import, deferred sector-simulation, and top-down world-event planning that was previously distributed across topical milestone files and supplements.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the economic/provenance/faction-market/deferred-simulation/world-event slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- `MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md`
- `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md`
- `MILESTONE_SUPPLEMENT_ILLICIT_NARCOTICS_FACTION_PRODUCTION.md`
- `MILESTONE_SUPPLEMENT_NOBLE_LUXURY_NARCOTICS_AND_DRAUGHT_TRADE.md`
- `MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` where animal, agriculture, pet vendor, and civilian-market content touches economy or provenance.
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` where news, rumors, market warnings, objective guidance, and player-facing explanation expose events to the player.
- `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md` where faction schemes must be distinguished from non-faction world events.
- `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md` where disasters, accidents, edicts, blockades, room destruction, room creation, train/import nodes, markets, vendors, storage rooms, raw material depots, and reinforcement arrival contexts become generated or mutated spaces.
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 8 - Population provenance, workforce context, demographic continuity, migration, and reinforcement imports.
- Phase 9 - Item provenance, raw-material provenance, facility origin, shipment origin, and supply ecology.
- Phase 16 - World generation, districts, facilities, vendors, train/import nodes, out-of-sector abstraction, top-down world events, and economy-supporting room stamps.
- Phase 17 - Economy, quests, contracts, faction reputation, vendors, markets, faction reinforcement demand, market pressure, news/rumor exposure, and civic/event consequences.
- Phase 18 - Editor, localization, modding, economy/faction/population/source/world-event editor support, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Asset promotion for vendor goods, narcotics, luxury items, draught items, market fixtures, vault containers, food goods, medical goods, trade items, shipment crates, raw material piles, event markers, news/rumor assets, and train/import-node assets.
- Phase 4 - News, rumors, market notices, event logs, event warnings, player-facing feedback, and readable UI exposure.
- Phase 6 - Production chains for goods that are manufactured rather than spawned.
- Phase 7 - Ownership, access, legality, stolen goods, restricted goods, forbidden access, customs/seizure rules, import permissions, emergency closures, and edict enforcement.
- Phase 11 - Leadership schemes that consume economic/provenance data and influence/strength/probability calculations while remaining separate from top-down non-faction world events.
- Phase 12 - Player-owned construction and expansion heat feeding economic/faction response.
- Phase 13 - Disasters, hazards, spire quakes, infrastructure failures, fires, collapse, blocked routes, and environmental event consequences.
- Phase 14 - Medical/narcotic item effects where economy intersects health.
- Phase 15 - Faction ranks, leaders, representatives, reinforcement staff, train arrivals, migrant workers, imported faction members, rumor carriers, officials, and event reporters.

## Core goal

The world should be able to explain who lives in a zone, who works there, who produces goods, which faction controls which facilities, why a vendor has its stock, where critical supplies come from, why a faction trades in certain goods, how raw materials entered the sector, why a faction is able or unable to replenish lost members, which external events are disrupting trade, and how markets change when faction control, scarcity, corruption, luxury demand, illicit production, shipments, reinforcements, disasters, edicts, blockades, or player activity changes.

Economy should not be a disconnected loot table. Faction markets should emerge from population, facilities, provenance, supply routes, faction identity, legality, storage, risk, reputation, leadership schemes, outside-sector supply, reinforcement routes, deferred probability-based simulation, and top-down world events.

## Deferred simulation doctrine

The game should not fully simulate every person, item, shipment, machine, faction decision, and mine outside the player's immediate world context. Out-of-sector and distant-sector behavior should defer into probability, strength, influence, supply, demand, source-provenance, event modifiers, and cooldown ledgers.

Distant systems should answer useful questions without pretending to run full local simulation:

- Did the faction successfully import reinforcements?
- Did a shipment arrive?
- Did a raw material shortage worsen?
- Did an outside supplier fail?
- Did a faction's strength or influence increase?
- Did a rival intercept supplies?
- Did the train/import node have capacity?
- Did a top-down event close the train route?
- Did a tithing decree drain faction reserves?
- Did an export ban prevent off-map sales?
- Did available rooms allow new people to enter and remain?
- Did an expired reinforcement window waste the opportunity?

The result should be deterministic enough to save/load and audit, but abstract enough to preserve performance. Immediate reality near the player can run high detail. Local districts can run operational ledgers. Out-of-sector systems can run probabilistic summaries.

## Top-down world event doctrine

Top-down world events are not ordinary faction schemes. They are external or systemic conditions that happen to the sector, the spire, the rail network, the civic order, the market environment, or surrounding sectors. They may be caused by accidents, infrastructure failures, distant wars, sector edicts, spire quakes, taxation, tithing decrees, epidemics, weather or atmospheric failures, route closures, religious/civic decrees, blockades, disasters, or other higher-order pressures.

A faction may exploit, suffer from, respond to, or be blamed for a top-down event, but the event itself should not be authored as that faction's plan unless the cause is explicitly factional. This distinction matters. A gang raid is a scheme. A spire quake is a world event. A faction blockade is a scheme. An external-sector export ban or imperial tax decree is a top-down world event. A noble plot to close a market is a scheme. A civic tithing decree that affects all markets is a top-down world event.

Top-down world events should be generated, curated, and validated for suitability. They should not simply be random punishment. Each event should define scale, duration, eligible zones/sectors, economy effects, population effects, reinforcement effects, room/worldgen effects, news/rumor exposure, internal-vendor exceptions, off-map trade restrictions, and cleanup/aftermath rules.

World events should be visible through news, rumors, vendor explanations, faction chatter, market notices, civic announcements, train-station messages, and Infopedia/event-log references where appropriate. The player should be able to hear that trains are out, exports are banned, a tithe has been imposed, a quake has damaged a district, or reinforcement shipments are delayed before the effects feel arbitrary.

## Dependency rule

Population provenance precedes item provenance. Item provenance precedes robust economy. Robust economy precedes believable faction vendors and market stock. Vendor stock should be connected to facility origin, faction identity, supply pressure, legality, access rules, raw material source, shipment route, world-event modifier, or outside-sector fallback wherever possible.

Illicit narcotics economies depend on production rooms, recipes, provenance, market access, and faction preference. Noble luxury and draught economies depend on noble estate rooms, vault custody, off-world provenance, house ownership, scarcity, and sale restrictions. Critical vendors depend on facilities, stock rules, ownership/access checks, player-facing explanations, and source-provenance fallbacks when local production cannot account for supply.

Out-of-sector simulation depends on probability ledgers, faction influence, faction strength, route access, source provenance, import nodes, storage capacity, housing capacity, room availability, local demand, event modifiers, and cooldown timers. If a distant raw material, person, or shipment appears inside the playable sector, the game should still be able to say whether it came from local mining, local salvage, local production, an outside-sector shipment, a faction import, a train arrival, a temporary event exception, or an unresolved fallback source.

## Phase 8 - Population provenance and economic identity

### Phase 8.1 - Abstract population target intake

Population targets produced during world generation should be treated as inputs to economic identity, not merely numbers. A zone with 400 residents, an industrial faction, and active market rooms should not produce the same economy as a low-population sewer district, noble estate, military compound, illicit gang territory, or disaster-damaged district.

Population targets should be connected to:

- Zone type.
- District type.
- Faction territory.
- Room type.
- Facility type.
- Worker demand.
- Resident demand.
- Market demand.
- Supply consumption.
- Faction recruitment and reinforcement.
- Vendor viability.
- World-event disruption.

Exit criteria:

Population counts can influence economic demand, labor availability, vendor viability, supply pressure, and event vulnerability.

### Phase 8.2 - Population origin and demand profile

Population origin should affect what goods are needed and what markets make sense.

Examples:

- Locally born residents need food, water, clothing, shelter, tools, medicine, and ordinary services.
- Contract labor may need dormitories, wage goods, tools, work food, and transport.
- Prisoners may create guard, food, medicine, restraint, and illicit-trade pressure.
- Pilgrims may create temple, food, lodging, trinket, and donation economies.
- Guards and soldiers create weapons, ammunition, armor, food, medical, and vice demand.
- Noble households create luxury, servants, security, private medicine, controlled narcotics, vault, and prestige-good demand.
- Gang dependents create food, protection, narcotics, black-market, stolen goods, and coercive economies.
- Imported faction members arriving from outside the sector create temporary lodging, onboarding, equipment, food, rank-assignment, and faction-integration demand.
- Disaster refugees, stranded travelers, and people displaced by quakes or route closures create temporary shelter, ration, medical, and employment pressure.

Exit criteria:

The world can distinguish not only how many people exist, but what their presence does to demand, labor, faction replenishment, crisis pressure, and faction markets.

### Phase 8.3 - Workforce assignment and economic production

Economic production should depend on who works where. Food vendors, narcotics labs, noble vaults, armories, clinics, pet vendors, workshops, and black markets should all be able to point to operators, owners, staff, handlers, brokers, guards, or abstract workforce groups.

Workforce records should support:

- Facility operators.
- Market vendors.
- Skilled workers.
- Guards.
- Smugglers.
- Brokers.
- Noble servants.
- House physicians.
- Lab technicians.
- Farmers and gardeners.
- Animal handlers.
- Pet vendors.
- Black-market dealers.
- Haulers and couriers.
- Imported replacement members.
- Reinforcement intake staff.
- Train/import-node workers.
- Disaster repair crews.
- Civic/tithe/tax collectors where represented.

Exit criteria:

Facilities and vendors can explain who operates them at the appropriate level of simulation detail.

### Phase 8.4 - Aggregation versus individual economic actors

Most background population can remain aggregated. Individuals should split out when they become player-relevant.

Split triggers include:

- The player talks to the vendor.
- The player takes a contract from the representative.
- The player attacks, hires, rescues, arrests, examines, tracks, or trades with the actor.
- The actor owns a pet, vehicle, room, vault, store, or quest-critical item relevant to the player.
- The actor is a faction leader, representative, specialist, noble, broker, doctor, smuggler, handler, imported reinforcement officer, disaster official, event witness, rumor carrier, or named criminal.

Exit criteria:

The economy can remain performant at distance while still creating individuals when player interaction requires detail.

### Phase 8.5 - Faction replacement and bulk reinforcement import pass

Factions that lose members should be able to request or generate replacement population through local recruitment, internal promotion, outside-sector imports, or bulk reinforcement arrivals.

Reinforcement logic should track:

- Faction member losses.
- Desired minimum staffing.
- Desired military/security staffing.
- Specialist shortages.
- Housing capacity.
- Barracks capacity.
- Faction room capacity.
- Food/water/support capacity.
- Import route availability.
- Train/import-node access.
- World-event restrictions.
- Reinforcement request cooldown.
- Semi-random reinforcement availability timer.
- Bulk group size.
- Rank/role composition.
- Equipment availability.
- Whether the opportunity expires if the faction cannot receive them.

A default early implementation target may use a train/import arrival point around level 5, zone coordinate 2,2 as a designated importation node, provided the value remains data-driven and can be relocated by worldgen, settings, or future map definitions.

Exit criteria:

Factions can replenish lost people through readable, capacity-limited, timer-based systems instead of spawning replacements without provenance.

### Phase 8.6 - Reinforcement cooldown, capacity, and expiration pass

Bulk reinforcement availability should not be constant or guaranteed. A faction that requests replacements should receive a countdown/cooldown window before reinforcements become available, with semi-random variation based on faction influence, strength, route access, leadership competence, wealth, outside support, danger, world-event restrictions, and recent losses.

Rules should include:

- Reinforcement availability timer.
- Arrival window.
- Capacity check at arrival.
- Housing/barracks/room-capacity check.
- Food/supply support check where implemented.
- Import node availability check.
- World-event closure or delay check.
- Expiration if the faction cannot receive the group.
- Delayed arrival if route or train access is blocked where implemented.
- Partial arrival if capacity is limited where implemented.
- Higher heat or visibility when large groups arrive.

Exit criteria:

Reinforcements feel like shipments of people through infrastructure, not invisible instant replacement spawns.

## Phase 9 - Item provenance and supply ecology

### Phase 9.1 - General item provenance

Items should be able to carry provenance when provenance matters.

Provenance may include:

- Producing faction.
- Producing facility.
- Producing machine.
- Operator or workforce group.
- Recipe or blueprint source.
- Input material quality.
- Production quality.
- Batch identity.
- Legal status.
- Current owner.
- Prior owner when meaningful.
- Trade path.
- Shipment route.
- Outside-sector source.
- Local raw-material source.
- World-event exception or disruption.
- Theft, salvage, counterfeit, contamination, or black-market status.

Exit criteria:

Items can be explained as goods with origins, not just free-floating inventory objects.

### Phase 9.2 - Food, water, and survival supply provenance

Food and water supply should connect to farms, gardens, hydroponics, mushroom rooms, kitchens, markets, trade routes, storage rooms, water recyclers, purification systems, scavenging, faction stores, ration systems, outside shipments, emergency imports, or emergency event fallbacks.

Vendor stock should distinguish:

- Fresh food.
- Preserved food.
- Rations.
- Luxury food.
- Spoiled or contaminated food.
- Stolen food.
- Faction ration stock.
- Black-market food.
- Noble imported foods.
- Outside-sector shipments.
- Disaster relief stock.
- Tithe-taxed or rationed stock.

Exit criteria:

Food vendors and supply shortages can be explained by facilities, trade routes, shipment routes, world events, and faction control.

### Phase 9.3 - Weapons, ammunition, and security-supply provenance

Weapons and ammunition should connect to armories, workshops, military depots, factories, black markets, smugglers, faction stores, stolen shipments, salvage, battlefield recovery, or outside-sector military/commercial shipments.

Vendor stock should distinguish:

- Civilian weapons.
- Security weapons.
- Military weapons.
- Ammunition.
- Restricted ammunition.
- Stolen stock.
- Counterfeit or defective stock.
- Faction surplus.
- Black-market arms.
- Outside-sector arms shipments.
- Blockade-restricted stock.
- Emergency confiscated stock where applicable.

Exit criteria:

Ammunition and weapon vendors are tied to faction identity, legality, supply routes, production/storage facilities, and event restrictions.

### Phase 9.4 - Medicine, narcotics, and pharmaceutical provenance

Medical goods, narcotics, stimulants, sedatives, painkillers, anesthetics, antibiotics, anti-toxins, medical kits, and black-market pharmaceuticals should have provenance where relevant.

Provenance should support:

- Clinic source.
- Lab source.
- Faction source.
- Gang or illicit producer.
- Noble physician supply.
- Off-world import.
- Outside-sector shipment.
- Relief shipment.
- Counterfeit batch.
- Contaminated batch.
- Restricted medicine.
- Legal medicine.
- Black-market narcotic.
- Recreational narcotic.
- Performance drug.
- Addiction/dependency risk classification where mechanics exist.

Exit criteria:

Medical and narcotic goods can be distinguished by source, legality, quality, risk, event disruption, and faction economy.

### Phase 9.5 - Noble luxury and draught provenance

Noble luxury goods and draught items require special provenance.

Draught items are rare off-world substances that noble houses can sometimes possess, but normally never sell. They are explicitly extremely valuable and should be distinguished from ordinary narcotics, medicines, and luxury goods.

Draught provenance should support:

- Noble house owner.
- Ashbourne house or sub-house owner where applicable.
- Off-world origin.
- Import route.
- Broker, smuggler, physician, or merchant source.
- Vault custody history.
- Whether it is genuine, diluted, counterfeit, contaminated, stolen, misdeclared, or house-certified.
- Whether it is affected by blockade, tax, seizure, or tithing edict.
- Whether it is for household use, prestige, gifting, bargaining, blackmail, medical privilege, private indulgence, inheritance, or hoarding.

Exit criteria:

Draught items are traceable, protected, event-sensitive, and exceptional rather than generic valuables.

### Phase 9.6 - Pet, animal, and agricultural goods provenance

Pet vendors, animal handlers, farms, gardens, and animal rooms should produce or stock goods with provenance where relevant.

Examples:

- Pet source.
- Animal breeder.
- Animal pen owner.
- Feed source.
- Water station source.
- Veterinary or handler care source.
- Seeds, crops, fungi, cloning samples, animal products, and pet supplies.
- Event-driven animal disease, feed shortage, or import restriction where implemented.

Exit criteria:

Animal/pet/agriculture economy connects to rooms, handlers, vendors, ownership, and event pressure rather than appearing as isolated flavor.

### Phase 9.7 - Raw material source provenance and fallback pass

Raw materials should never appear as unexplained infinite resources when provenance matters.

Raw material source categories include:

- Local mining.
- Local quarrying.
- Local salvage.
- Local recycling.
- Local scavenging.
- Facility stockpiles.
- Faction reserves.
- Outside-sector shipment.
- Off-world shipment.
- Train import.
- Merchant import.
- Noble import.
- Military import.
- Black-market import.
- Emergency fallback source.
- World-event relief or seizure source.
- Unknown/unresolved source flagged for audit.

When a local production chain requires raw materials that have not been fully simulated, the system should use provenance fallbacks rather than silent spawning. A fallback should record what was assumed, why it was allowed, which faction/source supplied it, whether it was local or external, whether an event changed the source, and whether the assumption should be audited later.

Exit criteria:

Mined, quarried, salvaged, shipped, imported, event-supplied, or fallback raw materials have explainable source provenance.

### Phase 9.8 - Shipment and import provenance pass

Incoming shipments should carry source and route context.

Shipment records should include:

- Source faction or supplier.
- Source sector/off-world/local facility.
- Destination faction or facility.
- Route or arrival node.
- Cargo manifest.
- Value.
- Legal/restricted/illicit status.
- Quality/contamination/counterfeit risk.
- Event modifier.
- Interception risk.
- Delay risk.
- Arrival time/window.
- Whether the shipment is abstracted, operational, or player-visible.

Exit criteria:

External supply supports the economy without requiring every distant factory or mine to run at full local detail.

## Phase 16 - Facility, vendor, deferred simulation, and world-event generation

### Phase 16.1 - Critical vendor and facility placement

Factions should generate vendors and facilities appropriate to their identity and infrastructure.

Critical vendor categories include:

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
- Luxury brokers where appropriate.
- Black-market or restricted goods where appropriate.

Exit criteria:

Faction territories can produce meaningful vendor access that matches who the faction is and what facilities they control.

### Phase 16.2 - Illicit narcotics facility generation

Gang groups, illicit organizations, smuggling rings, corrupt clinics, and black-market groups should be able to generate narcotics production and distribution facilities where appropriate.

Possible facilities:

- Hidden drug labs.
- Back-room chemical workshops.
- Corrupt clinics.
- Front pharmacies.
- Greenhouses or mushroom rooms for controlled substances.
- Storage rooms and stash rooms.
- Smuggling depots.
- Distribution rooms.
- Guarded dealer rooms.
- Contaminated labs.
- Burned-out labs as historical scars.

Exit criteria:

Illicit factions with narcotics economies leave visible room/facility traces in the world.

### Phase 16.3 - Noble estate luxury and draught vault generation

Noble estates, especially Ashbourne house estates, should support luxury and draught storage contexts.

Possible estate rooms:

- Locked noble vault.
- Sealed off-world substance vault.
- Luxury cellar.
- Private medicine cabinet.
- House physician room.
- Pleasure salon.
- Imported-goods store.
- Broker receiving room.
- Guarded estate storehouse.
- Reliquary display or sealed cabinet.
- Hidden noble stash.

Draught items should normally be locked behind access, ownership, stealth, diplomacy, quest, inheritance, faction permission, or violence.

Exit criteria:

Noble estate stamps can contain luxury and draught storage as meaningful protected assets, not generic treasure rooms.

### Phase 16.4 - Agricultural, animal, and pet market facilities

Agricultural factions, civilian markets, noble estates, animal handlers, and pet-focused vendors should be able to generate economy-supporting facilities.

Possible rooms:

- Farms.
- Gardens.
- Hydroponics.
- Mushroom beds.
- Animal pens.
- Kennels.
- Catteries.
- Rodent cages.
- Pet vendors.
- Pet zoos.
- Feed storage.
- Water stations.
- Cleaning stations.
- Animal-care rooms.

Exit criteria:

Animal, pet, and agricultural vendors have room/facility support rather than isolated NPC shop lists.

### Phase 16.5 - Train, shipment, and import-node generation pass

The economy and population systems need physical or abstract arrival points for goods and people entering the sector.

Import nodes may include:

- Train platform.
- Cargo station.
- Freight elevator.
- Service lift.
- Checkpoint.
- Customs room.
- Loading bay.
- Off-map road gate.
- Air/void cargo dock where setting supports it.
- Noble private import room.
- Black-market smuggling entry.

A default early implementation target may route imported faction members and selected shipments through a train/import node around level 5, zone coordinate 2,2, as long as this remains data-driven and not hardcoded forever.

Exit criteria:

External population and goods enter through identifiable import logic rather than unexplained appearance.

### Phase 16.6 - Deferred out-of-sector simulation pass

Out-of-sector and distant-sector activity should be simulated through abstract ledgers rather than full actor/item ticking.

Deferred ledgers should track:

- Faction strength.
- Faction influence.
- Faction wealth.
- Faction population pressure.
- Faction personnel losses.
- Faction reinforcement demand.
- Supplier reliability.
- Route safety.
- Shipment pressure.
- Warehouse and stockpile vulnerability.
- Raw material availability.
- Machine and production-line reliability.
- Product quality drift.
- Production inefficiency or waste.
- Import/export capacity.
- Rival interference.
- Leadership competence.
- Scheme pressure.
- Top-down event pressure.
- Player-caused heat or disruption.

Exit criteria:

Distant systems can produce believable outcomes without simulating every distant room, actor, and item.

### Phase 16.7 - Probability, influence, and strength calculation pass

Deferred simulation should use probability math informed by influence and strength factors.

Calculation inputs may include:

- Faction strength.
- Faction influence.
- Faction wealth.
- Faction control of routes.
- Local room/facility capacity.
- Leader competence.
- Rival opposition.
- Market scarcity.
- Available raw materials.
- Available train/import capacity.
- World-event modifiers.
- Previous successes/failures.
- Player interference.
- Heat/suspicion.
- Weather/hazard/road conditions where implemented.
- Security state.
- Journal/intelligence leaks.

Outputs may include:

- Shipment arrived.
- Shipment delayed.
- Shipment lost/intercepted.
- Arrival supply convoy raided or diverted.
- Warehouse stock stolen, spoiled, burned, seized, or hidden.
- Machinery sabotaged, damaged, repaired, or temporarily degraded.
- Raw material stores contaminated, wasted, adulterated, or substituted.
- Reinforcements available.
- Reinforcements delayed.
- Reinforcement opportunity expired.
- Raw material supply improved.
- Shortage worsened.
- Product quality improved or dropped.
- Production efficiency improved or degraded.
- Price moved.
- Faction strength changed.
- Faction influence changed.
- Scheme advanced or faltered.
- Event conditions improved or worsened.

Exit criteria:

Abstract simulation outcomes are explainable by faction factors and world-event modifiers instead of arbitrary random rolls.

### Phase 16.8 - Top-down world event generation pass

World events should be generated and curated separately from faction schemes.

Event families include:

- Spire quake.
- Structural collapse.
- Fire or industrial accident.
- Gas leak or toxic spill.
- Disease outbreak or quarantine.
- Train outage.
- Import node closure.
- Export ban.
- Off-map sales blockade.
- Sector trade blockade.
- Tithing decree.
- Tax edict.
- Confiscation order.
- Civic inspection sweep.
- Rationing order.
- Religious/civic observance affecting commerce.
- Weather, atmospheric, or pressure-system failure where setting supports it.
- Distant war or security alert.
- External sector unrest.
- Supply shock.
- Relief shipment.
- Reconstruction grant.
- Infrastructure repair period.

Each event should define:

- Event type.
- Start date/time.
- Duration.
- Scope: room, zone, level, sector, outside-sector, faction-neutral, or specific market category.
- Severity.
- Probability/eligibility rules.
- News/rumor exposure.
- Economy modifiers.
- Population modifiers.
- Shipment/import/export modifiers.
- Reinforcement modifiers.
- Room creation/destruction/damage hooks.
- Vendor restrictions and exceptions.
- Aftermath cleanup or recovery.

Exit criteria:

Top-down events become structured world-state records rather than untracked flavor text.

### Phase 16.9 - World event room and facility mutation pass

Top-down events may create, destroy, damage, close, repair, or repurpose rooms and facilities.

Examples:

- A spire quake damages rooms, blocks corridors, creates rubble, breaks utilities, closes train routes, and generates repair work.
- A reconstruction grant creates repair sites, temporary storage, contractor rooms, or public works offices.
- A quarantine closes clinics, markets, or housing blocks and creates checkpoints.
- A tax/tithe edict creates collection offices, inspection points, seizure storage, and temporary market restrictions.
- A train outage closes import nodes, strands travelers, blocks reinforcements, and delays shipments.
- A relief shipment creates temporary aid distribution rooms or market surpluses.

Exit criteria:

World events can affect physical space and facility availability, not only prices.

## Phase 17 - Faction markets, contracts, reinforcement, world events, and trade behavior

### Phase 17.1 - Faction vendor identity pass

Faction vendors should reflect faction identity.

Examples:

- Market factions provide food, general goods, traders, and blueprint vendors.
- Security or military factions provide weapons, ammunition, armor, controlled access, and restricted goods.
- Industrial factions provide tools, materials, machines, and production blueprints.
- Civic or administrative factions provide permits, licenses, property access, records, and official blueprints.
- Medical factions provide clinic goods, treatments, medicine, and care blueprints.
- Agricultural factions provide food, seeds, farm/garden blueprints, animal rooms, and care infrastructure.
- Pet or animal-focused vendors provide animals, pet supplies, care infrastructure, and pet-room blueprints.
- Noble factions provide luxury brokers, elite goods, private medicine access, and guarded prestige goods.
- Gang or illicit factions provide black-market goods, narcotics, stolen goods, restricted access, smuggling, and protection-based trade.

Exit criteria:

Vendors feel like faction infrastructure rather than generic shops.

### Phase 17.2 - Critical supply vendor pass

Food, supplies, weapons, ammunition, medicine, tools, and construction goods are critical economic categories once survival, combat, construction, contracts, and faction territory matter.

Critical stock should be provenance-aware where possible:

- Food from farms, kitchens, markets, trade, hydroponics, scavenging, faction stores, or relief shipments.
- Weapons and ammunition from armories, depots, workshops, factories, trade routes, black markets, stockpiles, or embargoed/imported sources.
- Medical goods from clinics, labs, trade, scavenging, relief stock, or faction stores.
- Construction goods from workshops, depots, industrial suppliers, salvage, merchants, local mines, or outside-sector raw-material shipments.
- Blueprint stock from faction identity, facilities, knowledge, licensing, and reputation.

Exit criteria:

The player can locate expected basic suppliers in factions that plausibly provide them.

### Phase 17.3 - Illicit narcotics market preference pass

Gang groups and illicit organizations should prefer to use, manufacture, stockpile, and sell narcotics when it matches their faction identity.

They may:

- Produce narcotics in bulk.
- Stockpile narcotics for internal use.
- Issue narcotics to faction members where doctrine supports it.
- Sell narcotics through black-market vendors.
- Sell to other factions or neutral buyers.
- Trade narcotics for weapons, food, medicine, protection, favors, information, or territory.
- Use narcotics as leverage, bribery, addiction pressure, recruitment bait, or social control.
- Protect narcotics production rooms as high-value facilities.
- Hide production from law enforcement, inspectors, hostile factions, rivals, or civic sweeps.

Exit criteria:

Illicit factions have a visible economic reason to manufacture narcotics: use, income, leverage, trade, and territorial influence.

### Phase 17.4 - Noble luxury and restricted narcotics trade pass

Noble factions, especially the Ashbourne houses, should participate in luxury-goods and narcotics trade.

Behavior should include:

- Noble brokers or representatives may buy and sell luxury goods.
- Selected narcotics or high-status medicines may be traded through official, private, or black-market channels.
- Nobles may use luxury items, rare medicines, and controlled substances as gifts, bribes, favors, blackmail tools, or hospitality.
- Nobles may pay extremely well for draught items but refuse to sell their own.
- Acquiring, stealing, exposing, adulterating, taxing, confiscating, or returning draught items should affect relations, heat, reputation, and schemes.

Draught items should be visible as known desires, rumors, vault contents, quest targets, or political goods rather than normal shop stock.

Exit criteria:

Noble trade supports luxury goods and narcotics while preserving draught items as exceptional locked-vault valuables.

### Phase 17.5 - Faction market legality and access pass

Markets should distinguish legal, restricted, illicit, stolen, counterfeit, military, noble-only, faction-only, license-gated, reputation-gated, event-restricted, rationed, taxed, embargoed, and black-market goods.

Access can be affected by:

- Faction standing.
- Permits or licenses.
- Rank.
- Bribes.
- Quest outcomes.
- Heat and suspicion.
- Current conflict state.
- Scarcity.
- Law enforcement pressure.
- Noble invitation or patronage.
- Criminal reputation.
- Top-down world events.

Exit criteria:

A vendor not selling something has a readable reason beyond invisible arbitrary lockout.

### Phase 17.6 - Contracts and faction-market work pass

Faction markets should generate contracts that reflect their economy.

Examples:

- Deliver food, medicine, ammunition, narcotics, luxury goods, or draught-related evidence.
- Protect shipments.
- Raid a stash.
- Recover stolen draught.
- Escort a broker.
- Sabotage a rival lab.
- Find counterfeit medicine.
- Investigate contaminated batches.
- Steal blueprints.
- Guard a market.
- Supply a pet vendor or animal handler.
- Break a blockade.
- Smuggle goods through rival territory.
- Escort reinforcement arrivals.
- Clear the train/import node.
- Recover lost raw material shipments.
- Repair quake-damaged facilities.
- Move goods before an export ban closes.
- Distribute relief supplies.
- Evacuate stranded travelers.

Exit criteria:

Contracts reflect real faction supply pressure, illicit economy, noble privilege, reinforcement needs, world events, and market needs.

### Phase 17.7 - Reinforcement import and faction recovery pass

Faction markets and population systems should support reinforcement import as a strategic recovery path.

Faction recovery behavior should include:

- Detect personnel losses.
- Decide whether local recruitment is sufficient.
- Request outside reinforcements where faction identity supports it.
- Start a semi-random availability cooldown.
- Reserve room/barracks/housing capacity where possible.
- Import members in bulk through the import node when the timer matures.
- Expire the opportunity if no room or support exists.
- Delay or cancel arrivals when a world event closes train/import routes.
- Assign arrivals to faction roles, guards, workers, specialists, or reserves.
- Attach provenance to imported people.

Exit criteria:

Faction member replenishment becomes a timed, capacity-limited, infrastructure-aware process.

### Phase 17.8 - News, rumor, and market-notice exposure pass

Top-down world events should be surfaced through the rumor/news system before or while they affect the player.

Exposure channels include:

- News bulletins.
- Rumors.
- Vendor chatter.
- Train station notices.
- Civic proclamations.
- Temple/civic announcements.
- Market boards.
- Courier talk.
- Faction representative dialogue.
- Event log entries.
- Infopedia/world-state references where appropriate.

Event rumors should support uncertainty. Early rumors may be incomplete, wrong, exaggerated, delayed, propaganda-shaped, or faction-biased. Confirmed notices should be clearer and more actionable.

Exit criteria:

The player can discover why markets, shipments, reinforcements, rooms, or routes have changed.

### Phase 17.9 - Off-map trade restriction and internal-vendor exception pass

World events may restrict off-map sales, imports, exports, or settlement with outside buyers without shutting down every local/internal vendor.

Rules should distinguish:

- Internal faction vendors still buying/selling from internal stock.
- Local market vendors still trading locally available goods.
- Off-map export buyers refusing goods during export bans.
- Outside suppliers failing or delaying imports.
- Smugglers ignoring or bypassing official restrictions at higher risk/cost.
- Civic/military/noble edicts restricting specific goods.
- Tax/tithe decrees reducing profit or increasing cost.
- Blockades preventing sale settlement until lifted.

Exit criteria:

A blockade or export ban can close off-map sales while local/internal commerce remains coherent.

## Phase 18 - Editor, audit, and Infopedia support

### Phase 18.1 - Economy/provenance editor and audit pass

Editor/audit surfaces should inspect:

- Population demand profiles.
- Workforce assignments.
- Facility operators.
- Item provenance fields.
- Raw material source provenance.
- Shipment/source provenance.
- Vendor stock rules.
- Critical vendor requirements.
- Faction market preferences.
- Legal/restricted/illicit status.
- Noble luxury definitions.
- Draught item definitions.
- Illicit narcotics production preference flags.
- Bulk production thresholds.
- Internal-use rules.
- External-sale rules.
- Vault-only storage flags.
- Faction vendor types.
- Contract hooks.
- Market scarcity and supply pressure.
- Reinforcement request state.
- Train/import-node state.
- Deferred simulation ledgers.
- Top-down world-event definitions.
- Event-to-economy modifiers.
- Event-to-room/facility mutation hooks.
- Event news/rumor exposure.

Exit criteria:

Economy, provenance, reinforcements, and world events can be audited through data surfaces rather than hidden in scattered source logic.

### Phase 18.2 - Faction, population, deferred simulation, and world-event editor pass

The editor suite should include faction/population/deferred-simulation/world-event authoring and audit surfaces.

Editor targets should include:

- Faction definitions.
- Faction influence.
- Faction strength.
- Faction wealth.
- Faction staffing targets.
- Faction reinforcement rules.
- Bulk import group definitions.
- Train/import-node definitions.
- Reinforcement cooldown ranges.
- Reinforcement expiration rules.
- Population origin profiles.
- Workforce profiles.
- Migration and arrival rules.
- Out-of-sector source definitions.
- Raw material source fallback rules.
- Shipment source tables.
- Probability inputs and weights.
- Probability output explanations.
- Route/interception risks.
- Top-down world-event families.
- Event suitability/eligibility rules.
- Event duration/severity tables.
- Event economy modifiers.
- Event room/facility mutation hooks.
- Event news/rumor templates.
- Event expiration and recovery rules.

Exit criteria:

Faction recovery, deferred simulation, population provenance, and top-down world events can be authored and audited instead of hidden in opaque random code.

### Phase 18.3 - Economy, event, and provenance Infopedia pass

Infopedia entries should explain faction markets, item provenance, world events, and event consequences in player-facing terms.

Entries should answer:

- What this item is.
- Who commonly makes it.
- Which factions commonly sell it.
- Whether it is legal, restricted, illicit, military, noble-only, counterfeit, stolen, black-market, event-restricted, embargoed, rationed, or taxed.
- What facilities produce or store it.
- What it is made from.
- Whether it came from local mining, local salvage, local production, outside-sector shipment, train import, off-world import, event relief, confiscation, or an unknown/fallback source.
- What it can be used to make.
- What quality/provenance states matter.
- Why a faction may want it.
- Why it may be unavailable.
- What risks, heat, suspicion, reputation, tax, tithe, blockade, or event effects may apply.

Exit criteria:

The player can use the Infopedia to understand markets, provenance, supply, legality, value, shipments, world events, and source assumptions without external notes.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Population provenance affects economic demand and labor availability.
- Vendors are tied to faction identity and facilities where possible.
- Critical vendors exist where the faction and facility context justifies them.
- Food, water, weapons, ammunition, medical, construction, pet, luxury, and black-market goods have coherent stock logic where implemented.
- Illicit factions can prefer narcotics production and sale where implemented.
- Noble factions and Ashbourne houses can participate in luxury/narcotics trade where implemented.
- Draught items are explicitly extremely valuable rare off-world substances.
- Draught items are normally not for sale and appear in secured noble estate contexts.
- Item provenance distinguishes legal, restricted, illicit, stolen, counterfeit, contaminated, noble-owned, local-source, outside-sector, train-imported, event-restricted, and off-world goods where relevant.
- Raw material provenance fallbacks identify local mining, local salvage, local stockpiles, outside-sector shipments, train imports, off-world imports, event relief/seizure, or unresolved audit states.
- Faction reinforcement imports use timers/cooldowns, capacity checks, import-node logic, event restrictions, and provenance where implemented.
- Reinforcement opportunities can expire when the faction lacks room/barracks/housing/support capacity where implemented.
- Deferred out-of-sector simulation uses probability, influence, strength, route, event, and supply ledgers rather than full distant ticking where implemented.
- Top-down world events are distinct from faction schemes.
- Top-down world events define duration, severity, scope, economy effects, population effects, shipment/import/export effects, reinforcement effects, room/facility mutation hooks, and news/rumor exposure where implemented.
- Train outages can block or delay reinforcements and shipments where implemented.
- Export bans or blockades can stop off-map sales while internal/local vendors continue coherently where implemented.
- Tax edicts, tithing decrees, rationing orders, disasters, and infrastructure failures affect markets in readable ways where implemented.
- Contracts can reference supply pressure, faction markets, illicit goods, luxury goods, draught items, raw material shipments, train/import nodes, reinforcement arrivals, world events, and vendor needs.
- Player-facing text avoids raw IDs and placeholder labels.
- Editor/audit surfaces can inspect the relevant data definitions.
- Infopedia and rumor/news systems explain major exposed market/provenance/reinforcement/deferred-simulation/world-event mechanics.

Exit criteria:

The game may claim population-backed economy, provenance-aware markets, illicit narcotics economies, noble luxury trade, draught goods, faction vendors, raw-material provenance, reinforcement imports, deferred out-of-sector simulation, and top-down world events only when these systems are inspectable, player-readable, and tied to facilities, factions, population, probability ledgers, shipments, item origins, news/rumor exposure, and event consequences.

## Non-goals for this milestone

This milestone does not complete all item effects, medical effects, addiction mechanics, combat mechanics, construction implementation, vehicle implementation, or full faction AI. It defines how economy and provenance should consume those systems when they exist.

This milestone does not require every background item to carry full provenance. It requires provenance when the item is important enough for trade, legality, quality, faction identity, quests, inspection, production, source fallback, event consequence, or player decision-making.

This milestone does not require every out-of-sector actor or item to be individually simulated. Deferred simulation is explicitly allowed and preferred for distant systems.

This milestone does not make draught items ordinary vendor goods. Draught items are exceptional valuables and should remain locked-vault/prestige/quest/political objects unless a future explicit exception is added.

This milestone does not make every top-down world event a disaster or punishment. Events may be negative, positive, mixed, localized, sector-wide, temporary, chronic, announced, rumored, hidden, or delayed depending on suitability and design context.

## Deferred checkpoint summary

Population provenance should feed economic demand, workforce availability, facility operation, item provenance, vendor stock, contracts, faction recovery, and faction market behavior. Illicit factions should prefer narcotics manufacturing and sale when their identity supports it. Noble factions, especially the Ashbourne houses, should participate in luxury goods and narcotics trade while hoarding rare draught items as extremely valuable off-world substances kept in locked vaults or secured noble estate spaces rather than ordinary vendor stock.

Distant and out-of-sector systems should defer to probability-ledger simulation rather than full ticking. Raw materials should retain source-provenance fallbacks for local mining, local salvage, local stockpiles, outside-sector shipments, train imports, off-world imports, event relief/seizure sources, or unresolved audit states. Factions that lose people should be able to request bulk replacements through a timed, semi-random reinforcement availability system, with capacity checks and expiration if they cannot house or support the arrivals. A train/import node around level 5, zone 2,2 may serve as an early data-driven importation target until the map/import-node system is generalized.

Top-down world events should be generated and curated as a separate system from faction schemes. Spire quakes, train outages, export bans, blockades, tax edicts, tithing decrees, rationing orders, relief shipments, infrastructure failures, and other systemic events should appear through news and rumors, affect markets and trade, alter shipments/imports/exports/reinforcements, sometimes damage or create rooms/facilities, and preserve internal/local vendor behavior where broader off-map trade is restricted.

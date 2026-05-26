# Milestone 07 - Ages of Control, Schemes, and Quest Lifecycles

This ordered milestone document consolidates planning for Ages of Control, historical zone generation, faction leadership schemes, room control changes, population drift, production plans, vehicle deployment, visible active schemes, scheme-related quests, quest timing windows, objective guidance, proof-of-death evidence, faction leadership journals, sellable intelligence, and missed-window rules.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the historical worldgen/faction-scheme/quest-lifecycle slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- Master-plan Phase 11 Ages of Control, faction leadership schemes, and zone generation history.
- Master-plan Phase 16 Ages-of-Control worldgen integration notes.
- Master-plan Phase 17 scheme quest lifecycle, objective guidance, quest evidence, and missed-window rules.
- Master-plan Phase 18 scheme quest editor/audit notes.
- Master-plan Phase 19 scheme quest and Ages-of-Control release-audit notes.
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` where quest guidance, objective highlights, examination, and Infopedia support player-facing scheme clarity.
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where population, economy, provenance, illicit markets, noble luxury, and draught goods feed faction schemes.
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` where construction parity, room ownership, player expansion heat, and faction attention feed active schemes.
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md` where vehicle deployment, APC/tank losses, route control, and military assets affect schemes.
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`.

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 11 - Ages of Control, faction leadership schemes, and zone generation history.
- Phase 16 - World generation, district simulation, facility placement, and strategic worldgen integration.
- Phase 17 - Economy, quests, contracts, faction reputation, scheme participation, intelligence trade, and mission lifecycle.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Asset promotion and worldgen-ready room/facility/journal/document assets.
- Phase 4 - UI, objective guidance, overlays, examination, and player-facing readability.
- Phase 5 - Operation records, routing, delivery, and interruption handling.
- Phase 6 - Production plans, machines, recipes, staffing, and operation queues.
- Phase 7 - Ownership, access checks, locked homes, private apartments, barracks, theft, stolen goods, and faction consequences.
- Phase 8 - Population provenance, workforce, demographic drift, and reinforcement requests.
- Phase 9 - Item provenance, supply ecology, facility output, luxury/draught/narcotics provenance, and trade pressure.
- Phase 10 - Vehicles, motor pools, route control, APCs, tanks, and vehicle loss history.
- Phase 12 - Construction, room ownership, player expansion heat, defenses, and bases.
- Phase 13 - Hazards, decay, infrastructure damage, and environmental scars.
- Phase 14 - Combat outcomes, death, structural damage, and vehicle combat outcomes.
- Phase 15 - Leaders, ranks, representatives, specialists, homes, barracks, private apartments, and command-chain authority.

## Core doctrine

Zones should be generated as visible results of history, not as one-shot scatters of rooms. A zone should know which factions controlled it, what those factions wanted, what they built, what they lost, who moved in or out, what production succeeded or failed, what vehicles mattered, what rooms changed hands, what decay occurred, and which schemes shaped the current layout.

The same leadership-scheme framework used during world generation should be able to continue after game start. Factions should not become frozen background murals. They should be able to plan, build, reinforce, raid, sabotage, negotiate, defend, recover, expand, retreat, smuggle, hoard, distribute, and contest control in ways that the player can discover, witness, join, oppose, or exploit.

Faction plans should have discoverable physical and social traces. In particular, important faction leaders should maintain leadership journals, private ledgers, planning notebooks, sealed orders, or equivalent documents in homes, barracks, private apartments, offices, command rooms, or secured personal spaces. Those documents can reveal current active plans and become valuable intelligence that the player may steal, sell, expose, deliver, copy, forge, or use.

Quest systems should expose plausible participation in faction schemes without reducing the world to a quest board. Planning-phase schemes can offer preparation work. Execution-phase schemes can offer active participation. Cooldown/aftermath states can offer cleanup, recovery, consequence, or missed-window outcomes when coherent.

## Dependency rule

Ages of Control depend on population provenance, item/facility provenance, vehicle provenance, promoted room/facility assets, faction leadership records, and deterministic generation settings. Scheme quests depend on scheme lifecycle states, player-facing objective guidance, timing windows, and clear failure behavior. Player-visible scheme events depend on the player being in or near a context where actors, rooms, routes, objectives, and consequences can be represented.

Leadership journals depend on named or role-tracked faction leaders, homes/barracks/private apartments/offices or other leader storage contexts, active scheme records, item/document provenance, theft/access rules, intelligence buyers, and player-facing valuation/reputation logic.

Minimum viable zones should begin with enough room diversity to support later history. As a rule target, the first playable viability age of a zone should create at least 20 rooms and at least one room of every required type for minimum zone viability before later ages expand, repurpose, damage, transfer, or abandon rooms.

## Phase 11 - Ages of Control and historical zone generation

### Phase 11.1 - Zone-generation mechanics review pass

Audit current zone generation before adding deeper history.

Review targets:

- Room minimums.
- Required room type guarantees.
- Room ownership assignment.
- Faction control assignment.
- Population target injection.
- Facility placement.
- Production placement.
- Vehicle placement.
- Road and transit layout.
- Noble estate placement.
- Illicit facility placement.
- Vendor placement.
- Leader home, barracks, office, and private-apartment placement.
- Journal/document storage contexts.
- Historical metadata gaps.
- Save/load needs.

Exit criteria:

The project can identify where current generation supports history, leadership-document placement, and active faction planning, and where it still behaves like static room scatter.

### Phase 11.2 - Age-of-control data model pass

Define an ordered zone-history record.

Each age record should support:

- Age index or name.
- Start and end conditions.
- Controlling faction.
- Dominant faction advantage.
- Rival factions.
- Leadership scheme or dominant strategy.
- Population trend.
- Production trend.
- Vehicle trend.
- Market trend.
- Room changes.
- Facility changes.
- Ownership changes.
- Conflict events.
- Political events.
- Intrigue events.
- Decay events.
- Vehicle losses or acquisitions.
- Luxury, draught, illicit, or strategic goods where relevant.
- Leader records and known planning documents where relevant.
- Inheritance into the next age.

Exit criteria:

Zone history has a structured record that can drive generation, inspection, journals/intelligence, and future active-scheme continuity.

### Phase 11.3 - Initial viability age pass

The first playable viability age for a zone should create a workable base layout.

Targets:

- At least 20 rooms.
- At least one room of every required type for minimum zone viability.
- Basic circulation.
- Required access routes.
- Civic or faction anchor where appropriate.
- Economic support rooms where appropriate.
- Population support rooms where appropriate.
- Basic storage/supply rooms.
- Leader housing, barracks, office, or private planning space where faction leadership exists.
- Enough ownership metadata for later control changes.

Exit criteria:

A zone begins with enough structure that later ages can expand, lose, contest, repurpose, document, and scheme over meaningful space.

### Phase 11.4 - Dominant-faction advantage pass

The dominant faction in a zone should have real advantages in maintaining control, but not immunity from decline or attack.

Advantages may include:

- More rooms.
- Better access routes.
- Better guards.
- Better vendor control.
- Stronger production capacity.
- More influence over civic offices.
- Better motor pools or vehicle access.
- More storage.
- Better intelligence.
- More secure leadership offices and journal storage.
- More legal authority.
- Stronger defensive positions.

Threats may include:

- Decline.
- Assault.
- Sabotage.
- Intrigue.
- Journal theft.
- Plan exposure.
- Population loss.
- Production failure.
- Vehicle loss.
- Smuggling.
- Blackmail.
- Draught theft.
- Narcotics destabilization.
- Concessions.

Exit criteria:

Dominant factions are advantaged but still participate in historical change, intelligence leakage, and plan exposure.

### Phase 11.5 - Leadership scheme definition pass

Define leadership schemes as explicit records rather than hidden prose.

Scheme families include:

- Expansion.
- Consolidation.
- Fortification.
- Production surge.
- Vehicle buildup.
- Housing boom.
- Market expansion.
- Political capture.
- Intrigue.
- Sabotage.
- Assault preparation.
- Defense.
- Recovery.
- Retreat.
- Austerity.
- Decadence.
- Managed decay.
- Smuggling.
- Narcotics production.
- Luxury acquisition.
- Draught hoarding or recovery.
- Blueprint acquisition.
- Player-pressure response.
- Counter-intelligence.
- Journal recovery.
- Plan-leak retaliation.

Exit criteria:

Faction plans can be named, timed, audited, written into journals, and connected to worldgen and active gameplay.

### Phase 11.6 - Historical expansion and room-growth pass

During expansion ages, faction schemes may add rooms or improve existing districts.

Expansion may include:

- New housing.
- Workshops.
- Markets.
- Clinics.
- Storage rooms.
- Armories.
- Garages.
- Motor pools.
- Farms or gardens.
- Animal rooms.
- Pet vendors.
- Laboratories.
- Noble estate rooms.
- Vaults.
- Illicit labs.
- Stash rooms.
- Leader apartments.
- Barracks offices.
- Command rooms.
- Document archives.
- Defenses.
- Service rooms.
- Utility rooms.

Exit criteria:

Faction expansion leaves concrete spatial evidence in zone layout, including places where leadership planning documents plausibly exist.

### Phase 11.7 - Loss, concession, and room-control transfer pass

During decline, conflict, or negotiation, rooms and facilities can change control.

Outcomes include:

- Transferred to rival faction.
- Returned to original faction.
- Contested.
- Neutral.
- Abandoned.
- Degraded.
- Seized by civic authority.
- Captured by gang.
- Claimed by player-founded faction in later active gameplay.
- Locked down.
- Converted to black-market use.
- Converted to emergency use.
- Looted, including possible journal or plan theft.
- Searched or purged after plan exposure.

Exit criteria:

Room control is historical, mutable, inspectable, and able to affect leadership document security.

### Phase 11.8 - Population drift and demographic shock pass

Each age should be able to change population.

Population events include:

- Population boom.
- Population loss.
- Migration.
- Reinforcement arrival.
- Evacuation.
- Displacement.
- Labor import.
- Prison intake.
- Disease loss.
- Recruitment.
- Pilgrim arrival.
- Noble household expansion.
- Gang dependency expansion.
- Faction membership promotion.
- Leadership replacement or succession.

Exit criteria:

Population provenance and demographic change help explain current room use, leadership continuity, and economic demand.

### Phase 11.9 - Production plan success/failure pass

Faction production schemes should consume item/facility provenance.

Outcomes include:

- Gained workshop.
- Lost machines.
- Expanded supply chain.
- Suffered shortages.
- Produced surplus.
- Degraded into scavenging.
- Established narcotics production.
- Built luxury stores.
- Acquired off-world goods.
- Lost or contaminated batches.
- Expanded ammunition or weapon supply.
- Failed to maintain machines.
- Wrote production targets into leadership journal or ledger.

Exit criteria:

Production history affects facilities, markets, item provenance, visible scars, and intelligence value.

### Phase 11.10 - Vehicle deployment and loss-history pass

Vehicle provenance should inform historical zone and sector outcomes.

Historical traces include:

- Convoy routes.
- Patrol routes.
- Motor pools.
- Armored assaults.
- Tank losses.
- APC captures.
- Truck shortages.
- Garage expansion.
- Road-control changes.
- Wreck placement.
- Salvage yards.
- Chokepoints.
- Abandoned vehicles.
- Vehicle deployment plans in journals.

Exit criteria:

Vehicles help explain faction power, physical zone history, and the intelligence value of stolen planning documents.

### Phase 11.11 - Assault, defense, and conflict-history pass

Historical assaults and defenses should leave traces.

Possible traces:

- Damaged rooms.
- Reinforced checkpoints.
- Abandoned rooms.
- Contested borders.
- Barricades.
- Population losses.
- Weapon stockpiles.
- Vehicle wrecks.
- Burned sections.
- Looted stores.
- Fortified doors.
- Hazardous ruins.
- Faction-control scars.
- Destroyed or stolen command records.

Exit criteria:

Conflict history affects current geometry, ownership, hazards, faction relationships, and document security.

### Phase 11.12 - Political plan and intrigue pass

Factions may change control without direct combat.

Political plans include:

- Influence.
- Bribery.
- Infiltration.
- Administrative capture.
- Religious or civic legitimacy.
- Blackmail.
- Legal control.
- Covert displacement.
- Debt capture.
- Marriage/alliance politics.
- Luxury gift diplomacy.
- Draught blackmail.
- Narcotics leverage.
- Journal theft.
- Plan sale to rivals.
- Plan sale to the Imperial News Network or other in-setting public press network after publish-safe naming review.

Exit criteria:

The history system can explain quiet power shifts, document leaks, scandals, and intelligence sales as well as open violence.

### Phase 11.13 - Degradation and maintenance-failure pass

Ages can decay infrastructure when maintenance, funding, or political will fails.

Degradation may include:

- Damaged rooms.
- Broken machines.
- Damaged vehicles.
- Reduced housing quality.
- Hazards.
- Blocked corridors.
- Utility failures.
- Abandoned pockets.
- Contaminated labs.
- Burned markets.
- Dead gardens.
- Failed animal pens.
- Cracked vaults.
- Flooded service spaces.
- Abandoned planning offices.
- Lost, outdated, or stale journals.

Exit criteria:

Decay becomes part of history, not random decoration, and can affect the freshness of discovered intelligence.

### Phase 11.14 - Cross-zone sector scheme pass

Faction schemes must not be only local. Factions controlling or contesting several zones may plan at sector scale.

Cross-zone schemes include:

- Expansion corridors.
- Production chains.
- Vehicle routes.
- Assaults.
- Defense networks.
- Migration routes.
- Reinforcement paths.
- Trade plans.
- Smuggling routes.
- Political capture.
- Luxury import paths.
- Draught recovery routes.
- Narcotics distribution routes.
- Player containment or diplomacy.
- Intelligence suppression.
- Recovery of stolen journals.

Exit criteria:

Sector-level faction behavior can affect local zone generation, active gameplay, and intelligence markets.

## Phase 11 - Ongoing active schemes after game start

### Phase 11.15 - Ongoing gameplay scheme activation pass

The same scheme framework used during world generation should support active gameplay.

After game start, factions may:

- Expand.
- Build.
- Reinforce.
- Assault.
- Defend.
- Repair.
- Deploy vehicles.
- Lose vehicles.
- Sabotage.
- Smuggle.
- Recover stolen goods.
- Open or close vendors.
- Change room control.
- Respond to player heat.
- Negotiate or threaten.
- Recover from losses.
- Update leadership journals.
- React to stolen journals or leaked plans.

Exit criteria:

Factions continue to plan and change after world generation, and their active plans can leave discoverable intelligence traces.

### Phase 11.16 - Player-visible active scheme event pass

Active faction schemes in the player's current zone should have a chance to become visible world events.

Visible events include:

- Faction soldiers storming a room.
- Defenders counterattacking.
- Reinforcements moving through streets.
- Guards mustering at checkpoints.
- Saboteurs approaching a machine.
- Smugglers moving contraband.
- Noble agents protecting or retrieving draught.
- Gang members defending a narcotics lab.
- Workers expanding a facility.
- Mechanics repairing a vehicle.
- Factions seizing a market or storage room.
- Guards searching for a stolen journal.
- Public scandal after plans are sold to a news network.
- Rival faction exploiting purchased intelligence.
- Aftermath cleanup.

Exit criteria:

Faction schemes are not only hidden ledger math when the player is nearby.

### Phase 11.17 - Scheme timing and lifecycle pass

Active gameplay schemes should use lifecycle states.

States include:

- Planned.
- Planning.
- Execution.
- Cooldown.
- Completed.
- Failed.
- Cancelled.
- Leaked.
- Compromised.
- Converted to aftermath.

The baseline cadence should be three days of planning, three days of execution, and three days of cooldown, with variability based on:

- Faction leader experience.
- Planning competence.
- Available staff.
- Faction pressure.
- Scheme complexity.
- Time since last plan attempt.
- Urgency.
- Resource scarcity.
- Player interference.
- Whether the faction's journal or plan has been stolen, copied, leaked, or sold.

Exact activation and attempt times must vary within the day so assaults, assassinations, sabotage, negotiations, smuggling, or other operations do not all begin at midnight, 12:01, or the first tick of a new day.

Exit criteria:

Faction schemes have readable, variable lifecycles instead of synchronized mechanical timers.

### Phase 11.18 - Scheme state handoff to quest system pass

Scheme records must expose current lifecycle state to the quest system.

Planning-phase schemes may offer:

- Scouting.
- Supply delivery.
- Recruiting.
- Bribery.
- Sabotage setup.
- Route clearing.
- Vehicle preparation.
- Room mapping.
- Defensive preparation.
- Intelligence gathering.
- Journal theft.
- Plan copying.
- Betrayal opportunities.

Execution-phase schemes may allow the player to:

- Join.
- Trigger.
- Escort.
- Defend.
- Sabotage.
- Fight.
- Negotiate.
- Steal.
- Rescue.
- Disrupt.
- Reveal.
- Smuggle.
- Seize.
- Leak plans.
- Sell intelligence.

Cooldown/aftermath may offer:

- Cleanup.
- Recovery.
- Retaliation.
- Salvage.
- Medical aid.
- Evidence recovery.
- Consequence negotiation.
- Repair work.
- Journal recovery.
- Scandal management.

Exit criteria:

Quest opportunities can be generated from scheme state rather than disconnected job-board logic.

## Phase 17 - Quest lifecycle, guidance, evidence, intelligence, and standing

### Phase 17.1 - Scheme quest lifecycle timing pass

Scheme-related quests should follow scheme lifecycle timing.

Rules:

- Planning quests occur during planning state.
- Execution quests occur during execution state.
- Active quest windows should last at least two days unless the player directly causes early resolution.
- Cooldown can fail, convert, or resolve unfinished scheme quests depending on context.
- Exact activation times vary within the day.
- NPCs who will offer a repeat or related quest soon show a compact countdown or availability note.

Exit criteria:

The player has enough time and guidance to participate without every scheme becoming a midnight trap.

### Phase 17.2 - Missed-window and standing-neutrality pass

Failed, missed, expired, or unaccepted quests should not reduce faction standing by themselves.

Standing changes should come from concrete hostile behavior, such as:

- Betrayal.
- Theft.
- Violence.
- Sabotage.
- Exposure.
- Lying where systems support it.
- Selling out allies.
- Aiding enemies.
- Destroying assets.
- Killing faction members.
- Stealing draught, vehicles, blueprints, journals, or restricted goods.
- Selling leadership plans to rivals or public press networks.

Merely failing to arrive in time should not be treated as hostility unless the quest explicitly defined an accepted obligation with consequences.

Exit criteria:

Missed opportunities do not unfairly punish the player as if they committed hostile acts, while actual betrayal and intelligence sale can matter.

### Phase 17.3 - Quest objective pointer and highlighting pass

Player quests must expose clear, identifiable target guidance.

Guidance should include:

- Arrow toward the current objective when exact location is known.
- Arrow toward nearest zone transition when the objective is elsewhere.
- Slowly pulsing objective highlight on visible targets.
- Distinction between exact targets, approximate search areas, rumored targets, hidden targets, and unknown targets.

Targets include:

- Entity.
- Container.
- Room.
- Machine.
- Vehicle.
- Corpse.
- Document.
- Leadership journal.
- Sealed orders.
- Plan ledger.
- Door.
- Terminal.
- Stash.
- Vault.
- Evidence item.
- Interaction point.

Exit criteria:

The player knows what matters once they arrive, while still choosing how to solve the problem.

### Phase 17.4 - Quest-critical item and proof-of-death evidence pass

Quest-critical pickup items required for quest performance should have zero weight.

Kill-target quests should not require the player to personally land the killing blow unless explicitly stated. When a required target dies, the target's corpse or death container should produce an identifiable proof-of-death item for pickup.

The system should validate:

- Target died.
- Evidence exists or was recovered.
- Evidence is the correct proof item.
- Quest delivery or confirmation conditions are met.

The system should not obsess over who or what killed the target unless the mission explicitly requires a specific killer or method.

Exit criteria:

Quest completion evidence is reliable, recoverable, and not blocked by inventory burden.

### Phase 17.5 - Leadership journal and plan-document pass

Faction leaders should have physical or data-backed planning documents that can expose current active plans.

Document types may include:

- Leadership journal.
- Private ledger.
- Sealed orders.
- Strategy notebook.
- Command docket.
- Patrol schedule.
- Production target list.
- Smuggling route list.
- Noble correspondence.
- Draught vault inventory.
- Narcotics distribution ledger.
- Vehicle deployment order.
- Assault plan.
- Bribery ledger.
- Blackmail note.

Storage contexts include:

- Leader home.
- Barracks.
- Private apartment.
- Faction office.
- Command room.
- Noble estate study.
- Secured desk.
- Locked cabinet.
- Safe.
- Vault.
- Personal container.
- Hidden stash.

A journal should not be omniscient. It should contain the plans that leader plausibly knows, controls, records, or is coordinating. It may be current, stale, partial, coded, encrypted, misleading, forged, redacted, or intentionally planted.

Exit criteria:

Faction plans can become physical or discoverable intelligence objects rather than only hidden scheduler state.

### Phase 17.6 - Intelligence sale and reputation pass

Stolen or copied leadership journals and planning documents should be sellable or deliverable to interested buyers.

Potential buyers include:

- Rival factions.
- The Imperial News Network or an equivalent in-setting public press network after publish-safe naming review.
- Black-market brokers.
- Civic investigators.
- Noble rivals.
- Gang rivals.
- Security forces.
- Internal dissidents.
- The targeted faction, as ransom or return.

Rewards may include:

- Money.
- Reputation with buyer.
- Reputation loss with exposed faction if discovered.
- Heat increase.
- Scandal.
- Scheme disruption.
- New quest chains.
- Protection offers.
- Retaliation.
- Blackmail leverage.
- Market movement.

The sale value should depend on:

- Freshness.
- Specificity.
- Scheme severity.
- Target faction importance.
- Buyer interest.
- Proof authenticity.
- Whether the information is original, copied, forged, outdated, encrypted, or already leaked.
- Whether the plan concerns assault, vaults, draught, narcotics, vehicles, leadership, production, or territorial control.

Exit criteria:

The player can turn stolen leadership intelligence into money, reputation, political disruption, and faction consequences.

### Phase 17.7 - Plan leak and scheme disruption pass

When a leadership journal or plan is stolen, sold, copied, or exposed, the owning faction's scheme may react.

Possible reactions:

- Scheme continues unchanged because the faction does not know it leaked.
- Scheme becomes compromised.
- Scheme is cancelled.
- Scheme changes target.
- Scheme accelerates.
- Scheme delays.
- Faction increases guards.
- Faction searches for the thief.
- Faction retaliates.
- Rival faction prepares defense.
- Rival faction counterattacks.
- Press scandal reduces reputation.
- Public exposure changes civic pressure.
- Noble house suppresses scandal.
- Gang punishes suspected informants.

Exit criteria:

Plan documents are not static loot; stealing or selling them can affect scheme state and faction behavior.

### Phase 17.8 - Player participation and alternate resolution pass

Scheme quests should allow player choice where coherent.

Possible resolutions:

- Complete as requested.
- Betray the offering faction.
- Warn the target.
- Sabotage both sides.
- Steal the objective.
- Sell the plan to a rival.
- Sell the plan to the press.
- Return the stolen journal for a reward.
- Negotiate a compromise.
- Arrive late and find aftermath.
- Rescue an unintended survivor.
- Capture rather than kill.
- Recover evidence after someone else completes the kill.
- Fail by leaving or letting time pass.

Exit criteria:

Scheme-related quests support player agency, betrayal, journalism/intelligence markets, and aftermath rather than only one linear completion path.

## Phase 16 - Worldgen integration

### Phase 16.1 - Ages-of-Control worldgen integration pass

Zone generation should consume age histories so room layouts reflect:

- Expansion.
- Loss.
- Concessions.
- Defense.
- Production success/failure.
- Vehicle buildup/loss.
- Degradation.
- Population movement.
- Faction leadership schemes.
- Noble luxury/draught politics.
- Illicit narcotics economies.
- Leadership document security.

Exit criteria:

District stamps and room layouts reflect the zone's historical ledger.

### Phase 16.2 - Leader home, barracks, and private planning-space generation pass

If a faction has active leaders, command staff, or meaningful scheme authority, world generation should provide plausible spaces where those actors live, work, store records, or coordinate plans.

Possible spaces:

- Private apartment.
- Barracks office.
- Command room.
- Faction representative office.
- Noble estate study.
- Guard captain's room.
- Gang boss room.
- Broker office.
- House physician office.
- Hidden planning room.
- Secure archive.
- Locked desk or cabinet.

Exit criteria:

Leadership journals and plan documents have plausible storage locations in the world.

### Phase 16.3 - Active-scheme visibility in current zone pass

When active schemes occur in the player's current zone, worldgen/runtime should surface visible actors, objectives, and aftermath where appropriate.

Visible elements include:

- Moving actors.
- Room attacks.
- Guard posts.
- Warning noise.
- Courier movement.
- Smuggler routes.
- Journal recovery squads.
- Locked-down offices.
- Reinforced vaults.
- Search parties.
- Post-conflict debris.
- Changed room control.

Exit criteria:

Active schemes become observable world events when the player is near the action.

## Phase 18 - Editor, audit, and Infopedia support

### Phase 18.1 - Ages, scheme, and quest editor/audit pass

Editor/audit surfaces should inspect:

- Zone ages.
- Faction control history.
- Room transfers.
- Leadership schemes.
- Scheme lifecycle state.
- Scheme timing.
- Scheme target.
- Scheme owner.
- Player-visible event hooks.
- Quest handoff state.
- Objective guidance state.
- Missed-window behavior.
- Proof-of-death evidence definitions.
- Quest-critical zero-weight item flags.
- Leadership journal definitions.
- Journal storage locations.
- Journal freshness.
- Journal contents.
- Intelligence buyers.
- Intelligence sale consequences.
- Plan leak reactions.

Exit criteria:

Ages, schemes, quests, and leadership intelligence can be audited through owned data surfaces rather than hidden in scattered source logic.

### Phase 18.2 - Scheme and intelligence Infopedia pass

Infopedia entries should explain exposed scheme and intelligence mechanics.

Entries should cover:

- Ages of Control.
- Faction schemes.
- Planning/execution/cooldown.
- Quest windows.
- Objective guidance.
- Proof-of-death evidence.
- Quest-critical item weight rules.
- Leadership journals.
- Stolen plans.
- Intelligence buyers.
- Public exposure.
- Rival-faction sale.
- Heat and reputation consequences.
- Why not every plan is perfectly known or current.

Exit criteria:

The player can understand scheme and intelligence mechanics without seeing raw scheduler state.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Zone histories are structured and deterministic where required.
- Initial viable zones create enough room diversity for history to matter.
- Faction leadership schemes exist as inspectable records where implemented.
- Scheme states include planning, execution, cooldown, completed, failed, cancelled, compromised, or leaked where implemented.
- Exact activation times vary within the day rather than synchronizing to midnight or first tick.
- Active quest windows last at least two days unless resolved early by player action.
- Missed/unaccepted/expired quests do not reduce standing by themselves.
- Concrete hostile actions, betrayal, theft, sabotage, exposure, and intelligence sale can affect standing and heat.
- Quest objective arrows and target highlights are useful and not clutter.
- Quest-critical pickup items required for quest performance have zero weight.
- Kill-target evidence appears on death containers/corpses regardless of killer unless a quest explicitly requires otherwise.
- Leadership journals or planning documents are stored in plausible homes, barracks, private apartments, offices, command spaces, safes, vaults, or personal containers where implemented.
- Journals expose plausible active plans rather than omniscient hidden state.
- Stolen journals or copied plans can be sold, returned, leaked, or delivered to interested buyers where implemented.
- Imperial News Network / public press naming receives publish-safe review before public release.
- Rival-faction intelligence sale can grant money/reputation and produce consequences.
- Plan leaks can compromise, delay, cancel, redirect, or intensify schemes where implemented.
- Editor/audit surfaces can inspect scheme and journal definitions.
- Infopedia entries explain major exposed scheme/intelligence mechanics.
- Player-facing text avoids raw IDs and placeholder labels.

Exit criteria:

The game may claim Ages of Control, active faction schemes, scheme quest lifecycles, leadership journals, and sellable intelligence only when these systems are structured, inspectable, player-readable, and tied to room control, faction plans, evidence, timing, reputation, and world consequences.

## Non-goals for this milestone

This milestone does not require all faction plans to be perfectly knowable. It requires that discoverable plans have plausible physical, social, or intelligence traces.

This milestone does not require every minor faction member to keep a detailed journal. It targets leaders, officers, representatives, planners, nobles, gang bosses, command staff, or other actors whose written plans would matter.

This milestone does not make every journal accurate. Journals may be stale, partial, coded, encrypted, forged, planted, misleading, or incomplete.

This milestone does not force the player into journalism, espionage, or betrayal. It makes those paths available when plans, buyers, and consequences exist.

## Deferred checkpoint summary

Ages of Control should turn zone generation into visible historical simulation. Faction leadership schemes should drive expansion, defense, assaults, production, vehicles, markets, illicit activity, noble politics, room control, and active gameplay. Scheme quests should use readable planning/execution/cooldown windows, non-synchronized activation times, minimum active duration, objective guidance, zero-weight quest items, proof-of-death evidence, and neutral missed-window standing behavior.

Faction leaders should keep journals, ledgers, sealed orders, or equivalent planning documents in homes, barracks, private apartments, offices, command rooms, or secured personal spaces. These documents should expose plausible current active plans and become valuable intelligence. The player should be able to steal, copy, return, sell, leak, or deliver such plans to rival factions, public press networks such as the Imperial News Network after publish-safe naming review, civic investigators, brokers, or the targeted faction itself, earning money, reputation, heat, retaliation, scandal, or scheme disruption depending on context.

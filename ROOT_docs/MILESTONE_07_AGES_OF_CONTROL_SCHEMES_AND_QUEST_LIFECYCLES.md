# Milestone 07 - Ages of Control, Schemes, Quest Lifecycles, and Quest Editor

This ordered milestone document consolidates planning for Ages of Control, historical zone generation, faction leadership schemes, room control changes, population drift, production plans, vehicle deployment, visible active schemes, scheme-related quests, quest timing windows, objective guidance, proof-of-death evidence, faction leadership journals, sellable intelligence, missed-window rules, and the dedicated Quest Editor required to author, validate, test, and audit those quest flows.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the historical worldgen/faction-scheme/quest-lifecycle/quest-editor slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- Master-plan Phase 11 Ages of Control, faction leadership schemes, and zone generation history.
- Master-plan Phase 16 Ages-of-Control worldgen integration notes.
- Master-plan Phase 17 scheme quest lifecycle, objective guidance, quest evidence, intelligence sale, and missed-window rules.
- Master-plan Phase 18 scheme quest editor/audit notes.
- Master-plan Phase 19 scheme quest and Ages-of-Control release-audit notes.
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md` where quest guidance, objective highlights, examination, movement-to-objective readability, and Infopedia support player-facing scheme clarity.
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where population, economy, provenance, illicit markets, noble luxury, and draught goods feed faction schemes.
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` where construction parity, room ownership, player expansion heat, and faction attention feed active schemes.
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md` where vehicle deployment, APC/tank losses, route control, and military assets affect schemes.
- `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md` where room/facility anchors, storage contexts, journal placement, and active-scheme room events are surfaced in world space.
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`.

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 11 - Ages of Control, faction leadership schemes, and zone generation history.
- Phase 16 - World generation, district simulation, facility placement, and strategic worldgen integration.
- Phase 17 - Economy, quests, contracts, faction reputation, scheme participation, intelligence trade, and mission lifecycle.
- Phase 18 - Editor, localization, modding, quest editor, scheme editor, audit tooling, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Asset promotion and worldgen-ready room/facility/journal/document/objective assets.
- Phase 4 - UI, objective guidance, overlays, examination, movement-to-objective guidance, and player-facing readability.
- Phase 5 - Operation records, routing, delivery, interruption handling, and task state.
- Phase 6 - Production plans, machines, recipes, staffing, operation queues, and production objectives.
- Phase 7 - Ownership, access checks, locked homes, private apartments, barracks, theft, stolen goods, quest containers, and faction consequences.
- Phase 8 - Population provenance, workforce, demographic drift, quest actors, and reinforcement requests.
- Phase 9 - Item provenance, supply ecology, facility output, quest-critical items, evidence, luxury/draught/narcotics provenance, and trade pressure.
- Phase 10 - Vehicles, motor pools, route control, APCs, tanks, vehicle loss history, and vehicle objectives.
- Phase 12 - Construction, room ownership, player expansion heat, defenses, bases, and construction objectives.
- Phase 13 - Hazards, decay, infrastructure damage, route danger, and environmental scars.
- Phase 14 - Combat outcomes, death, proof-of-death evidence, structural damage, and vehicle combat outcomes.
- Phase 15 - Leaders, ranks, representatives, specialists, homes, barracks, private apartments, command-chain authority, and quest-giver identity.

## Core doctrine

Zones should be generated as visible results of history, not as one-shot scatters of rooms. A zone should know which factions controlled it, what those factions wanted, what they built, what they lost, who moved in or out, what production succeeded or failed, what vehicles mattered, what rooms changed hands, what decay occurred, and which schemes shaped the current layout.

The same leadership-scheme framework used during world generation should be able to continue after game start. Factions should not become frozen background murals. They should be able to plan, build, reinforce, raid, sabotage, negotiate, defend, recover, expand, retreat, smuggle, hoard, distribute, and contest control in ways that the player can discover, witness, join, oppose, exploit, expose, or sell.

Faction plans should have discoverable physical and social traces. Important faction leaders should maintain leadership journals, private ledgers, planning notebooks, sealed orders, or equivalent documents in homes, barracks, private apartments, offices, command rooms, or secured personal spaces. Those documents can reveal current active plans and become valuable intelligence that the player may steal, sell, expose, deliver, copy, forge, return, or use.

Quest systems should expose plausible participation in faction schemes without reducing the world to a quest board. Planning-phase schemes can offer preparation work. Execution-phase schemes can offer active participation. Cooldown/aftermath states can offer cleanup, recovery, consequence, intelligence sale, retaliation, or missed-window outcomes when coherent.

## Quest Editor doctrine

A quest system this interconnected cannot be authored only through scattered code, hidden constants, or one-off scripts. The project needs a dedicated Quest Editor or editor-grade quest authoring surface that can define, inspect, validate, and test quests as data-backed content.

The Quest Editor should support authored quests, scheme-generated quests, contract-style quests, intelligence-sale quests, tutorial/help quests, faction service quests, construction/material quests, vehicle quests, kill/evidence quests, delivery quests, recovery quests, investigation quests, pet/animal quests, medical quests, and worldgen/age-derived quests where those systems exist.

The Quest Editor must make quest lifecycle rules explicit. It should author and audit planning, execution, cooldown, active, completed, failed, cancelled, leaked, compromised, expired, aftermath, and repeatable states. It should also validate that objectives, rewards, evidence, timers, actors, containers, locations, highlights, arrows, reputation effects, heat effects, journal hooks, and failure rules are coherent.

Quest editing must be player-readability-aware. If a quest has an objective, the editor should know whether the objective can be pointed to, highlighted, searched for, inferred, kept hidden, or intentionally vague. If a quest requires an item, the editor should know whether it is zero-weight, quest-protected, tradable, droppable, stolen, forged, copied, or evidence. If a quest requires a death, the editor should know what proof-of-death evidence appears and where.

## Dependency rule

Ages of Control depend on population provenance, item/facility provenance, vehicle provenance, promoted room/facility assets, faction leadership records, and deterministic generation settings. Scheme quests depend on scheme lifecycle states, player-facing objective guidance, timing windows, and clear failure behavior. Player-visible scheme events depend on the player being in or near a context where actors, rooms, routes, objectives, and consequences can be represented.

Leadership journals depend on named or role-tracked faction leaders, homes/barracks/private apartments/offices or other leader storage contexts, active scheme records, item/document provenance, theft/access rules, intelligence buyers, and player-facing valuation/reputation logic.

Quest Editor functionality depends on stable quest IDs, player-facing localization keys, objective types, reward/effect definitions, actor/entity references, room/location references, item/evidence references, scheme references, validation rules, preview/smoke tooling, and ordinary UI hiding raw debug identifiers from players.

Minimum viable zones should begin with enough room diversity to support later history. As a rule target, the first playable viability age of a zone should create at least 20 rooms and at least one room of every required type for minimum zone viability before later ages expand, repurpose, damage, transfer, or abandon rooms.

## Phase 11 - Ages of Control and historical zone generation

### Phase 11.1 - Zone-generation mechanics review pass

Audit current zone generation before adding deeper history. Review room minimums, required room types, ownership, faction control, population injection, facility placement, production placement, vehicle placement, road/transit layout, noble estates, illicit facilities, vendors, leader homes, barracks, offices, private apartments, journal/document storage contexts, historical metadata, and save/load needs.

Exit criteria:

The project can identify where current generation supports history, leadership-document placement, active faction planning, quest generation, and where it still behaves like static room scatter.

### Phase 11.2 - Age-of-control data model pass

Define an ordered zone-history record with age index/name, start/end conditions, controlling faction, dominant faction advantage, rival factions, leadership strategy, population trend, production trend, vehicle trend, market trend, room/facility/ownership changes, conflict events, political events, intrigue events, decay events, vehicle loss/acquisition, luxury/draught/illicit/strategic goods, leader records, known planning documents, quest seeds, and inheritance into the next age.

Exit criteria:

Zone history has a structured record that can drive generation, inspection, journals/intelligence, quests, and future active-scheme continuity.

### Phase 11.3 - Initial viability age pass

The first playable viability age for a zone should create a workable base layout with at least 20 rooms, at least one room of every required type for minimum zone viability, basic circulation, required access routes, civic/faction anchors, economic support rooms, population support rooms, storage/supply rooms, leader housing/barracks/offices/private planning spaces where faction leadership exists, and enough ownership metadata for later control changes.

Exit criteria:

A zone begins with enough structure that later ages can expand, lose, contest, repurpose, document, scheme, and quest over meaningful space.

### Phase 11.4 - Dominant-faction advantage pass

The dominant faction in a zone should have real advantages in maintaining control, but not immunity from decline or attack. Advantages may include more rooms, better access, guards, vendor control, production capacity, civic influence, motor pools, storage, intelligence, secure leadership offices/journal storage, legal authority, and defensive positions. Threats include decline, assault, sabotage, intrigue, journal theft, plan exposure, population loss, production failure, vehicle loss, smuggling, blackmail, draught theft, narcotics destabilization, and concessions.

Exit criteria:

Dominant factions are advantaged but still participate in historical change, intelligence leakage, plan exposure, and quest opportunity.

### Phase 11.5 - Leadership scheme definition pass

Define leadership schemes as explicit records rather than hidden prose. Scheme families include expansion, consolidation, fortification, production surge, vehicle buildup, housing boom, market expansion, political capture, intrigue, sabotage, assault preparation, defense, recovery, retreat, austerity, decadence, managed decay, smuggling, narcotics production, luxury acquisition, draught hoarding/recovery, blueprint acquisition, player-pressure response, counter-intelligence, journal recovery, and plan-leak retaliation.

Exit criteria:

Faction plans can be named, timed, audited, written into journals, connected to worldgen, and converted into active gameplay or quest opportunities.

### Phase 11.6 - Historical expansion and room-growth pass

During expansion ages, faction schemes may add or improve housing, workshops, markets, clinics, storage rooms, armories, garages, motor pools, farms/gardens, animal rooms, pet vendors, laboratories, noble estate rooms, vaults, illicit labs, stash rooms, leader apartments, barracks offices, command rooms, document archives, defenses, service rooms, and utility rooms.

Exit criteria:

Faction expansion leaves concrete spatial evidence in zone layout, including places where leadership planning documents and quest objectives plausibly exist.

### Phase 11.7 - Loss, concession, and room-control transfer pass

During decline, conflict, or negotiation, rooms and facilities can transfer to a rival faction, return to an original faction, become contested, neutral, abandoned, degraded, seized, captured by gangs, claimed by a player-founded faction, locked down, converted to black-market use, converted to emergency use, looted, searched, or purged after plan exposure.

Exit criteria:

Room control is historical, mutable, inspectable, and able to affect leadership document security, quest targets, and active scheme state.

### Phase 11.8 - Population drift and demographic shock pass

Each age should be able to change population through boom, loss, migration, reinforcement arrival, evacuation, displacement, labor import, prison intake, disease loss, recruitment, pilgrim arrival, noble household expansion, gang dependency expansion, faction membership promotion, leadership replacement, or succession.

Exit criteria:

Population provenance and demographic change help explain current room use, leadership continuity, economic demand, quest actors, and faction workforce.

### Phase 11.9 - Production plan success/failure pass

Faction production schemes should consume item/facility provenance. Outcomes include gained workshops, lost machines, expanded supply chains, shortages, surplus, scavenging decline, narcotics production, luxury stores, off-world goods, lost/contaminated batches, ammunition/weapon supply, failed machine maintenance, and production targets written into journals/ledgers.

Exit criteria:

Production history affects facilities, markets, item provenance, visible scars, intelligence value, and quest generation.

### Phase 11.10 - Vehicle deployment and loss-history pass

Vehicle provenance should inform historical zone and sector outcomes through convoy routes, patrol routes, motor pools, armored assaults, tank losses, APC captures, truck shortages, garage expansion, road-control changes, wreck placement, salvage yards, chokepoints, abandoned vehicles, and vehicle deployment plans in journals.

Exit criteria:

Vehicles help explain faction power, physical zone history, quest targets, and the intelligence value of stolen planning documents.

### Phase 11.11 - Assault, defense, and conflict-history pass

Historical assaults and defenses should leave traces such as damaged rooms, reinforced checkpoints, abandoned rooms, contested borders, barricades, population losses, weapon stockpiles, vehicle wrecks, burned sections, looted stores, fortified doors, hazardous ruins, faction-control scars, destroyed records, and stolen command records.

Exit criteria:

Conflict history affects current geometry, ownership, hazards, faction relationships, quest context, and document security.

### Phase 11.12 - Political plan and intrigue pass

Factions may change control without direct combat through influence, bribery, infiltration, administrative capture, civic/religious legitimacy, blackmail, legal control, covert displacement, debt capture, alliance politics, luxury gift diplomacy, draught blackmail, narcotics leverage, journal theft, plan sale to rivals, and plan sale to the Imperial News Network or other in-setting public press network after publish-safe naming review.

Exit criteria:

The history system can explain quiet power shifts, document leaks, scandals, intelligence sales, and quest lines as well as open violence.

### Phase 11.13 - Degradation and maintenance-failure pass

Ages can decay infrastructure when maintenance, funding, or political will fails. Degradation may include damaged rooms, broken machines, damaged vehicles, reduced housing quality, hazards, blocked corridors, utility failures, abandoned pockets, contaminated labs, burned markets, dead gardens, failed animal pens, cracked vaults, flooded service spaces, abandoned planning offices, and lost/outdated/stale journals.

Exit criteria:

Decay becomes part of history, not random decoration, and can affect discovered intelligence, quest reliability, and objective freshness.

### Phase 11.14 - Cross-zone sector scheme pass

Faction schemes must not be only local. Cross-zone schemes include expansion corridors, production chains, vehicle routes, assaults, defense networks, migration routes, reinforcement paths, trade plans, smuggling routes, political capture, luxury import paths, draught recovery routes, narcotics distribution routes, player containment/diplomacy, intelligence suppression, and recovery of stolen journals.

Exit criteria:

Sector-level faction behavior can affect local zone generation, active gameplay, intelligence markets, and quest targets.

## Phase 11 - Ongoing active schemes after game start

### Phase 11.15 - Ongoing gameplay scheme activation pass

The same scheme framework used during world generation should support active gameplay. After game start, factions may expand, build, reinforce, assault, defend, repair, deploy vehicles, lose vehicles, sabotage, smuggle, recover stolen goods, open/close vendors, change room control, respond to player heat, negotiate/threaten, recover from losses, update leadership journals, and react to stolen journals or leaked plans.

Exit criteria:

Factions continue to plan and change after world generation, and their active plans can leave discoverable intelligence and quest traces.

### Phase 11.16 - Player-visible active scheme event pass

Active faction schemes in the player's current zone should have a chance to become visible world events: soldiers storming rooms, defenders counterattacking, reinforcements moving through streets, guards mustering, saboteurs approaching machines, smugglers moving contraband, noble agents protecting/retrieving draught, gangs defending narcotics labs, workers expanding facilities, mechanics repairing vehicles, factions seizing markets/storage rooms, guards searching for journals, press scandals, rival exploitation of purchased intelligence, and aftermath cleanup.

Exit criteria:

Faction schemes are not only hidden ledger math when the player is nearby.

### Phase 11.17 - Scheme timing and lifecycle pass

Active gameplay schemes should use lifecycle states: planned, planning, execution, cooldown, completed, failed, cancelled, leaked, compromised, and converted to aftermath. The baseline cadence should be three days of planning, three days of execution, and three days of cooldown, with variability based on leader experience, planning competence, staff, pressure, complexity, time since last attempt, urgency, scarcity, player interference, and whether the plan has been stolen/copied/leaked/sold.

Exact activation and attempt times must vary within the day so assaults, assassinations, sabotage, negotiations, smuggling, or other operations do not all begin at midnight, 12:01, or the first tick of a new day.

Exit criteria:

Faction schemes have readable, variable lifecycles instead of synchronized mechanical timers.

### Phase 11.18 - Scheme state handoff to quest system pass

Scheme records must expose lifecycle state to the quest system. Planning-phase schemes may offer scouting, supply delivery, recruiting, bribery, setup sabotage, route clearing, vehicle preparation, room mapping, defense preparation, intelligence gathering, journal theft, plan copying, and betrayal opportunities. Execution-phase schemes may allow joining, triggering, escorting, defending, sabotaging, fighting, negotiating, stealing, rescuing, disrupting, revealing, smuggling, seizing, leaking plans, and selling intelligence. Cooldown/aftermath may offer cleanup, recovery, retaliation, salvage, medical aid, evidence recovery, consequence negotiation, repair work, journal recovery, and scandal management.

Exit criteria:

Quest opportunities can be generated from scheme state rather than disconnected job-board logic.

## Phase 17 - Quest lifecycle, guidance, evidence, intelligence, and standing

### Phase 17.1 - Scheme quest lifecycle timing pass

Scheme-related quests should follow scheme lifecycle timing. Planning quests occur during planning state, execution quests occur during execution state, active quest windows should last at least two days unless resolved early by player action, cooldown can fail/convert/resolve unfinished quests depending on context, exact activation times vary within the day, and NPCs who will offer a repeat or related quest soon show compact countdown/availability notes.

Exit criteria:

The player has enough time and guidance to participate without every scheme becoming a midnight trap.

### Phase 17.2 - Missed-window and standing-neutrality pass

Failed, missed, expired, or unaccepted quests should not reduce faction standing by themselves. Standing changes should come from concrete hostile behavior such as betrayal, theft, violence, sabotage, exposure, lying where supported, selling out allies, aiding enemies, destroying assets, killing faction members, stealing draught/vehicles/blueprints/journals/restricted goods, or selling leadership plans to rivals/public press networks.

Merely failing to arrive in time should not be treated as hostility unless the quest explicitly defined an accepted obligation with consequences.

Exit criteria:

Missed opportunities do not unfairly punish the player as if they committed hostile acts, while actual betrayal and intelligence sale can matter.

### Phase 17.3 - Quest objective pointer and highlighting pass

Player quests must expose clear, identifiable target guidance: arrows toward known objectives, arrows toward nearest zone transitions when elsewhere, slowly pulsing objective highlights on visible targets, and distinct feedback for exact targets, approximate search areas, rumored targets, hidden targets, and unknown targets.

Targets include entities, containers, rooms, machines, vehicles, corpses, documents, leadership journals, sealed orders, plan ledgers, doors, terminals, stashes, vaults, evidence items, and interaction points.

Exit criteria:

The player knows what matters once they arrive, while still choosing how to solve the problem.

### Phase 17.4 - Quest-critical item and proof-of-death evidence pass

Quest-critical pickup items required for quest performance should have zero weight. Kill-target quests should not require the player to personally land the killing blow unless explicitly stated. When a required target dies, the target's corpse or death container should produce identifiable proof-of-death evidence for pickup.

The system should validate target death, evidence existence/recovery, correct proof item, and quest delivery/confirmation conditions. It should not care who or what killed the target unless the mission explicitly requires a specific killer or method.

Exit criteria:

Quest completion evidence is reliable, recoverable, and not blocked by inventory burden.

### Phase 17.5 - Leadership journal and plan-document pass

Faction leaders should have physical or data-backed planning documents that can expose plausible current active plans.

Document types include leadership journals, private ledgers, sealed orders, strategy notebooks, command dockets, patrol schedules, production target lists, smuggling route lists, noble correspondence, draught vault inventories, narcotics distribution ledgers, vehicle deployment orders, assault plans, bribery ledgers, and blackmail notes.

Storage contexts include leader homes, barracks, private apartments, faction offices, command rooms, noble estate studies, secured desks, locked cabinets, safes, vaults, personal containers, and hidden stashes. A journal should not be omniscient; it may be current, stale, partial, coded, encrypted, misleading, forged, redacted, or intentionally planted.

Exit criteria:

Faction plans can become physical or discoverable intelligence objects rather than only hidden scheduler state.

### Phase 17.6 - Intelligence sale and reputation pass

Stolen or copied leadership journals and planning documents should be sellable or deliverable to interested buyers: rival factions, the Imperial News Network or equivalent in-setting public press network after publish-safe naming review, black-market brokers, civic investigators, noble rivals, gang rivals, security forces, internal dissidents, or the targeted faction as ransom/return.

Rewards and consequences may include money, reputation with buyer, reputation loss with exposed faction if discovered, heat, scandal, scheme disruption, new quest chains, protection offers, retaliation, blackmail leverage, and market movement. Sale value depends on freshness, specificity, severity, target importance, buyer interest, authenticity, whether information is original/copied/forged/outdated/encrypted/already leaked, and whether the plan concerns assault, vaults, draught, narcotics, vehicles, leadership, production, or territorial control.

Exit criteria:

The player can turn stolen leadership intelligence into money, reputation, political disruption, and faction consequences.

### Phase 17.7 - Plan leak and scheme disruption pass

When a leadership journal or plan is stolen, sold, copied, or exposed, the owning faction's scheme may continue unchanged, become compromised, cancel, change target, accelerate, delay, increase guards, search for the thief, retaliate, trigger rival defense/counterattack, cause press scandal, change civic pressure, trigger noble suppression, or cause gang punishment of suspected informants.

Exit criteria:

Plan documents are not static loot; stealing or selling them can affect scheme state and faction behavior.

### Phase 17.8 - Player participation and alternate resolution pass

Scheme quests should allow player choice where coherent: complete as requested, betray the offering faction, warn the target, sabotage both sides, steal the objective, sell the plan to a rival, sell the plan to the press, return the stolen journal for a reward, negotiate compromise, arrive late and find aftermath, rescue an unintended survivor, capture rather than kill, recover evidence after someone else completes the kill, or fail by leaving/letting time pass.

Exit criteria:

Scheme-related quests support player agency, betrayal, journalism/intelligence markets, and aftermath rather than only one linear completion path.

## Phase 16 - Worldgen integration

### Phase 16.1 - Ages-of-Control worldgen integration pass

Zone generation should consume age histories so room layouts reflect expansion, loss, concessions, defense, production success/failure, vehicle buildup/loss, degradation, population movement, faction leadership schemes, noble luxury/draught politics, illicit narcotics economies, and leadership document security.

Exit criteria:

District stamps and room layouts reflect the zone's historical ledger.

### Phase 16.2 - Leader home, barracks, and private planning-space generation pass

If a faction has active leaders, command staff, or meaningful scheme authority, world generation should provide plausible spaces where those actors live, work, store records, or coordinate plans: private apartments, barracks offices, command rooms, representative offices, noble estate studies, guard captain rooms, gang boss rooms, broker offices, house physician offices, hidden planning rooms, secure archives, locked desks, and locked cabinets.

Exit criteria:

Leadership journals and plan documents have plausible storage locations in the world.

### Phase 16.3 - Active-scheme visibility in current zone pass

When active schemes occur in the player's current zone, worldgen/runtime should surface visible actors, objectives, and aftermath such as moving actors, room attacks, guard posts, warning noise, courier movement, smuggler routes, journal recovery squads, locked-down offices, reinforced vaults, search parties, post-conflict debris, and changed room control.

Exit criteria:

Active schemes become observable world events when the player is near the action.

## Phase 18 - Editor, audit, Quest Editor, and Infopedia support

### Phase 18.1 - Ages, scheme, and quest editor/audit pass

Editor/audit surfaces should inspect zone ages, faction control history, room transfers, leadership schemes, lifecycle state, timing, targets, owner, player-visible event hooks, quest handoff state, objective guidance state, missed-window behavior, proof-of-death evidence definitions, quest-critical zero-weight item flags, leadership journal definitions, journal storage locations, journal freshness, journal contents, intelligence buyers, intelligence sale consequences, and plan leak reactions.

Exit criteria:

Ages, schemes, quests, and leadership intelligence can be audited through owned data surfaces rather than hidden in scattered source logic.

### Phase 18.2 - Dedicated Quest Editor authoring pass

Create or plan a dedicated Quest Editor surface for authoring and revising quest definitions.

The Quest Editor should define:

- Quest ID and player-facing title.
- Localization keys for title, summary, objective text, failure text, completion text, and journal/log entries.
- Quest family/type: scheme quest, contract, delivery, kill/evidence, investigation, theft, escort, construction, medical, vehicle, pet/animal, tutorial, intelligence sale, recovery, sabotage, defense, assault, or aftermath.
- Quest giver or source: NPC, faction, room, scheme, terminal/document, journal, vendor, event, worldgen seed, or hidden trigger.
- Owning faction, benefiting faction, opposed faction, and neutral/civic observers.
- Prerequisites.
- Start conditions.
- Expiration conditions.
- Repeatability and cooldown.
- Lifecycle state mapping.
- Planning/execution/cooldown/aftermath compatibility.
- Objective graph.
- Branching and alternate resolution paths.
- Rewards and consequences.
- Standing/heat changes.
- Failure behavior.
- Infopedia links.
- Debug/audit notes hidden from ordinary UI.

Exit criteria:

Quests can be authored and inspected as structured content instead of scattered hardcoded behavior.

### Phase 18.3 - Quest objective editor pass

The Quest Editor should support objective authoring for:

- Talk to actor.
- Reach room/location.
- Search area.
- Pick up item.
- Deliver item.
- Use object/machine/terminal.
- Open or access container.
- Kill target.
- Recover proof-of-death evidence.
- Capture target.
- Escort actor.
- Defend room/actor/object.
- Attack room/actor/object.
- Sabotage machine/facility/vehicle.
- Repair machine/facility/vehicle.
- Build room/object/machine.
- Acquire blueprint/permit/license.
- Steal journal/document/item.
- Copy plan/intelligence.
- Sell intelligence.
- Leak information.
- Return stolen journal.
- Rescue pet/animal/NPC.
- Treat injured actor.
- Clear route/hazard.
- Observe/witness event.

Objective definitions should include target selectors, exact/approximate/hidden target modes, guidance behavior, arrow behavior, highlight behavior, search-radius behavior, zone-transition guidance, failure-on-timer rules, completion validation, and save/load persistence.

Exit criteria:

Quest objectives can be composed, validated, guided, and completed without one-off logic for every mission.

### Phase 18.4 - Quest reward, consequence, and faction-effect editor pass

The Quest Editor should define rewards and consequences as structured outcomes.

Supported outcomes should include:

- Money.
- Items.
- Blueprint access.
- Permit/license access.
- Service unlock.
- Vendor access.
- Faction standing change.
- Heat/suspicion change.
- Scheme disruption.
- Scheme acceleration/delay/cancellation.
- Room control change.
- Intelligence exposure.
- Public scandal.
- Retaliation trigger.
- Protection offer.
- Contract chain unlock.
- Companion/recruit relationship change where supported.
- Market movement.
- Journal/document state change.
- Evidence consumed/returned/copied.

The editor should distinguish accepted-obligation failure from missed/unaccepted opportunity so missed-window neutrality remains intact.

Exit criteria:

Rewards and consequences are readable, auditable, and consistent with faction/standing/heat rules.

### Phase 18.5 - Quest evidence, item, and document editor pass

The Quest Editor should define quest-critical items and evidence rules.

Fields should include:

- Quest item flag.
- Zero-weight requirement.
- Droppable or protected.
- Tradable/sellable/returnable/copyable.
- Stolen/restricted/legal state.
- Proof-of-death generation rule.
- Corpse/death-container placement.
- Evidence authenticity.
- Forged/coded/encrypted/stale state.
- Intelligence freshness.
- Valid buyers.
- Delivery targets.
- Whether evidence is consumed, retained, copied, or returned.

Exit criteria:

Quest evidence, journals, and critical items are not fragile ad hoc loot entries.

### Phase 18.6 - Quest validation, preview, and smoke-test pass

The Quest Editor should validate quests before they are released.

Validation should check:

- Missing localization keys.
- Missing quest giver/source.
- Missing objective target.
- Invalid target selectors.
- Impossible location references.
- Missing item/evidence definitions.
- Non-zero-weight required pickup items.
- Kill quests lacking proof-of-death evidence where required.
- Missing reward/consequence definitions.
- Contradictory faction effects.
- Dangerous standing loss on missed/unaccepted quests.
- Expiration timers shorter than allowed windows unless explicitly exempt.
- Missing objective guidance for known exact targets.
- Raw IDs or placeholder text in player-facing strings.
- Save/load persistence flags.
- Broken scheme lifecycle mapping.
- Broken journal/intelligence sale hooks.

The editor should support preview/smoke modes that simulate start, objective completion, failure, expiration, save/load, and alternate resolution paths without requiring a full manual playthrough.

Exit criteria:

Quest content can be validated and smoke-tested before it reaches ordinary gameplay.

### Phase 18.7 - Quest editor UI and usability pass

The Quest Editor itself should follow the same UI rules as other editor tools.

The editor should provide:

- Quest list/search/filter.
- Quest detail pane.
- Objective graph or ordered objective list.
- Lifecycle preview.
- Faction-effect preview.
- Reward/consequence preview.
- Evidence/item preview.
- Objective guidance preview.
- Validation panel.
- Warnings and errors separated by severity.
- Raw IDs visible only in editor/developer context.
- Clear save/revert behavior.
- Export/package validation where relevant.

Exit criteria:

Designers can author and audit quests without manually reading scattered source files.

### Phase 18.8 - Scheme and intelligence Infopedia pass

Infopedia entries should explain exposed scheme and intelligence mechanics: Ages of Control, faction schemes, planning/execution/cooldown, quest windows, objective guidance, proof-of-death evidence, quest-critical item weight rules, leadership journals, stolen plans, intelligence buyers, public exposure, rival-faction sale, heat/reputation consequences, and why not every plan is perfectly known or current.

Exit criteria:

The player can understand scheme and intelligence mechanics without seeing raw scheduler state.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- Zone histories are structured and deterministic where required.
- Initial viable zones create enough room diversity for history to matter.
- Faction leadership schemes exist as inspectable records where implemented.
- Scheme states include planning, execution, cooldown, completed, failed, cancelled, compromised, or leaked where implemented.
- Exact activation times vary within the day rather than synchronizing to midnight or first tick.
- Active quest windows last at least two days unless resolved early by player action or explicitly exempted.
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
- A dedicated Quest Editor or editor-grade authoring surface exists/planned before claiming deep quest content readiness.
- Quest Editor definitions cover quest IDs, localization, objectives, lifecycle, rewards, consequences, evidence, objective guidance, and validation where implemented.
- Quest validation catches missing targets, missing localization, impossible objectives, bad timers, non-zero-weight critical items, raw player-facing IDs, and unsafe missed-window standing losses where implemented.
- Infopedia entries explain major exposed scheme/intelligence/quest mechanics.
- Player-facing text avoids raw IDs and placeholder labels.

Exit criteria:

The game may claim Ages of Control, active faction schemes, scheme quest lifecycles, leadership journals, sellable intelligence, and quest-editor readiness only when these systems are structured, inspectable, player-readable, authorable, validated, and tied to room control, faction plans, evidence, timing, reputation, and world consequences.

## Non-goals for this milestone

This milestone does not require all faction plans to be perfectly knowable. It requires that discoverable plans have plausible physical, social, or intelligence traces.

This milestone does not require every minor faction member to keep a detailed journal. It targets leaders, officers, representatives, planners, nobles, gang bosses, command staff, or other actors whose written plans would matter.

This milestone does not make every journal accurate. Journals may be stale, partial, coded, encrypted, forged, planted, misleading, or incomplete.

This milestone does not force the player into journalism, espionage, or betrayal. It makes those paths available when plans, buyers, and consequences exist.

This milestone does not require the first Quest Editor implementation to be beautiful or final. It requires quest authoring and validation to move away from scattered one-off code and toward owned editor/audit surfaces.

## Deferred checkpoint summary

Ages of Control should turn zone generation into visible historical simulation. Faction leadership schemes should drive expansion, defense, assaults, production, vehicles, markets, illicit activity, noble politics, room control, and active gameplay. Scheme quests should use readable planning/execution/cooldown windows, non-synchronized activation times, minimum active duration, objective guidance, zero-weight quest items, proof-of-death evidence, and neutral missed-window standing behavior.

Faction leaders should keep journals, ledgers, sealed orders, or equivalent planning documents in homes, barracks, private apartments, offices, command rooms, or secured personal spaces. These documents should expose plausible current active plans and become valuable intelligence. The player should be able to steal, copy, return, sell, leak, or deliver such plans to rival factions, public press networks such as the Imperial News Network after publish-safe naming review, civic investigators, brokers, or the targeted faction itself, earning money, reputation, heat, retaliation, scandal, or scheme disruption depending on context.

The project should include a dedicated Quest Editor or editor-grade quest authoring surface. It should author quest identity, localization, objectives, lifecycle states, scheme hooks, evidence, proof-of-death rules, zero-weight quest items, objective arrows/highlights, rewards, reputation, heat, alternate resolutions, expiration, validation, and smoke-test previews. Quest content should become structured, inspectable, and testable rather than hidden inside scattered source logic.

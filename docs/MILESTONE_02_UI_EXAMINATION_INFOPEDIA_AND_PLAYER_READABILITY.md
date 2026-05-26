# Milestone 02 - UI, Examination, Infopedia, and Player Readability

This ordered milestone document consolidates planning for player-facing readability, the `Look` and `Examine` interaction split, intelligence-based entity analysis, refreshed intent checks, entity examination screens, quest objective guidance, pet interaction feedback, Infopedia depth, Infopedia hot linking, and the general rule that exposed mechanics must be explained in compact useful language.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the UI/readability/examination/Infopedia slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- `MILESTONE_EXAMINATION_AND_INFOPEDIA_SYSTEMS.md`
- `MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` where pet interaction feedback touches UI/readability
- `MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md` where Infopedia acquisition references and blueprint restrictions need player-facing explanation
- `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md` where Infopedia entries explain knowledge, skills, medicine, cybernetics, narcotics, and quality fabrication
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where economy/provenance/legality needs Infopedia and player-facing explanation
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` where construction, access, heat, and blueprint systems need compact UI explanations
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 4 - UI, input, rendering, accessibility, display, graphics, options, and presentation containment.
- Phase 14 - Combat, health, body condition, unconsciousness, death, saves, and feedback.
- Phase 15 - Character creation, names, ranks, rosters, and social identity.
- Phase 17 - Economy, quests, contracts, faction reputation, pets, ownership, and player-facing mission clarity.
- Phase 18 - Editor, localization, modding, Infopedia, and content pipeline.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Semantic asset IDs and player-facing labels.
- Phase 6 - Production status, machine operation, and recipe explanation.
- Phase 7 - Access/ownership denial messaging.
- Phase 8 - Population/social identity exposed through examination.
- Phase 9 - Item provenance exposed through Infopedia and inspection.
- Phase 10 - Vehicle inspection and large-entity readability.
- Phase 11 - Active schemes and quest lifecycle readability.
- Phase 12 - Construction feedback, blueprint restrictions, and player expansion heat explanation.
- Phase 13 - Hazard warnings and visibility feedback.

## Core readability doctrine

If a system is player-visible, the player should be able to understand what it is, why it matters, and what broad action paths are available without reading source code or seeing raw internal identifiers.

Player-facing UI should prefer compact truth over verbose noise. It should hide raw package IDs, Java class names, UUIDs, registry handles, implementation flags, manifest keys, and developer-only identifiers unless the player has explicitly opened a developer/audit surface.

Readable does not mean omniscient. The game may hide secrets, uncertain identities, vague rumors, unknown mechanisms, locked details, and failed checks. But uncertainty should itself be represented in player-facing terms such as `unknown`, `suspected`, `rumored`, `appears to be`, `too dark to tell`, `requires closer examination`, or `needs better tools` rather than leaking debug state.

## Dependency rule

The basic `Look` surface must precede the deeper `Examine` command. `Look` provides immediate visible observation. `Examine` builds on that observation and uses intelligence, relevant skill, visibility, distance, time spent, prior knowledge, and tools to refine estimates.

Infopedia depth depends on stable semantic entries for items, entities, mechanics, rooms, machines, recipes, factions, statuses, blueprints, qualities, injuries, pets, vehicles, ownership states, and provenance states.

Quest guidance depends on target identity and zone/path knowledge. If a target is exact and known, guidance may be precise. If a target is approximate, rumored, hidden, disguised, or unknown, guidance should communicate uncertainty instead of lying.

## Phase 4 - Look, Examine, UI feedback, and objective readability

### Phase 4.1 - Look command readability pass

`Look` should answer what the player can immediately observe.

The look output may include:

- Basic entity or object label.
- Visible faction or uniform cues.
- Approximate posture or behavior.
- Immediate threat impression.
- Surface intent read.
- Obvious injuries, equipment, or hostility.
- Whether the target appears owned, restricted, public, hostile, friendly, unconscious, damaged, locked, accessible, or unknown.
- Whether deeper `Examine` is possible.

`Look` should be fast, compact, and readable. It should not require a full-screen dossier for ordinary observations.

Exit criteria:

Looking at an entity, object, container, room feature, pet, machine, or vehicle produces useful immediate information without raw IDs or hidden-state leakage.

### Phase 4.2 - Examine command and entity examination screen pass

When the player looks at a valid entity, the UI should expose an additional `Examine` command. `Examine` opens a screen similar in structure to the character/dossier screen, but tuned for uncertainty and approximation rather than perfect internal truth.

The examination screen should include, where available and appropriate:

- Entity name or known alias.
- Entity type.
- Faction or suspected faction.
- Role, rank, job, or visible social function.
- Biography or known background information.
- Current visible state.
- Immediate intent estimate.
- Threat estimate.
- Health or injury approximation.
- Body-plan layout or body-part overview.
- Equipment estimate.
- Armor and weapon danger estimate.
- Mood, morale, fear, anger, hostility, or confidence estimate where observable.
- Movement state.
- Current target or activity if readable.
- Known quest relevance.
- Known ownership or access relevance.
- Known pet/animal temperament where relevant.
- Hot links into Infopedia entries for entity type, faction, equipment, body-plan concepts, statuses, injuries, and mechanics.

Exit criteria:

A player can look at an entity, choose `Examine`, and see a coherent character-sheet-like analysis screen that provides useful information without pretending the player has perfect omniscience.

### Phase 4.3 - Examination refresh and time-investment pass

Intent and state checks should refresh every few turns while the player continues examining or repeatedly re-examines the entity. Spending time examining should be able to improve confidence, reveal additional details, correct earlier uncertainty, or update stale intent information.

Refresh behavior should consider:

- Character intelligence.
- Relevant perception, medicine, combat, animal-handling, social, technical, or investigative skills if present.
- Distance to target.
- Lighting and visibility.
- Obstructions.
- Whether the target is moving, fighting, hiding, wounded, disguised, unconscious, hostile, or cooperating.
- Whether the examiner has prior knowledge of the species, faction, role, equipment, machine, vehicle, status, or condition.
- Whether tools, scanners, optics, medical gear, faction records, or local rumors are available.

The system should avoid excessive turn spam. Examination refresh should update the screen and recent-action feedback compactly.

Exit criteria:

Spending extra time examining an entity can produce more reliable or more complete information, and stale intent estimates do not remain frozen forever.

### Phase 4.4 - Quest objective guidance overlay pass

Quest UI must provide clear player-facing objective guidance.

Guidance should include:

- A pointer arrow toward the current objective when an objective location is known.
- A pointer arrow toward the nearest zone transition that moves the player closer when the objective is in another zone.
- A slowly pulsing highlight on the involved container, entity, room, vehicle, machine, corpse, document, terminal, door, or interaction point when the target is visible/current.
- Distinct feedback for exact targets, approximate search areas, rumored targets, hidden targets, and hostile/unsafe targets.

The player should know who must be contacted, who must be killed, what container must be looted, what item must be recovered, what room must be reached, or what machine must be used. How the player completes that objective remains up to them.

Exit criteria:

Active quests guide the player toward relevant objectives without deciding the solution path or flooding the screen with clutter.

### Phase 4.5 - Pet interaction feedback pass

Non-hostile pet entities must expose clear interaction feedback when the player can pet them.

Required species interactions:

- Dog-like pets support head pats and bark, wag, huff, lean in, or otherwise respond appropriately.
- Cat-like pets support scritches and meow, purr, blink, stretch, rub, or otherwise respond appropriately.
- Mouse and rat pets support nose boops and squeak, chitter, sniff, twitch whiskers, or otherwise respond appropriately.

Blocked pet interactions must explain the block in player-facing terms. Valid reasons include hostile, frightened, inaccessible, out of reach, behind a locked enclosure, asleep, injured, working, or owned by someone who forbids interaction.

Exit criteria:

If the player sees a non-hostile pet, the game acknowledges the obvious interaction path unless a real in-world state blocks it.

### Phase 4.6 - Access, construction, and ownership feedback pass

Access-denied, construction-denied, blueprint-denied, and interaction-denied states must explain themselves in useful language.

Examples:

- You know the plan, but lack the license.
- This room is faction-owned.
- This container is locked.
- This blueprint is military-restricted.
- You lack the materials.
- The room has no utility access.
- You are not authorized to modify this area.
- This pet belongs to someone who does not permit handling.
- This item appears stolen and using it may raise heat.

Exit criteria:

The player understands what blocked an action and what kind of path might resolve it.

## Phase 14 - Body-plan, injury, combat-readiness, and condition readability

### Phase 14.1 - Body-plan and condition approximation pass

Examination should show body-plan approximations and current condition estimates when appropriate. For humanoids, animals, creatures, machines, and vehicles, the system should provide a readable overview of major parts or condition areas without requiring perfect exact numbers.

For living entities, this may include:

- Head, torso, limbs, or species-appropriate body regions.
- Obvious wounds.
- Limping, bleeding, stunned, unconscious, exhausted, frightened, poisoned, burned, or infected state.
- Approximate health band.
- Combat readiness estimate.
- Weapon and armor danger estimate.

For machines or vehicles, this may include:

- Chassis or frame.
- Power or fuel state.
- Mobility systems.
- Weapons or mounts if visible.
- Armor or structural condition.
- Obvious leaks, fire, damage, missing parts, disabled state, or dangerous instability.

Exit criteria:

The player can make a reasonable decision about whether an examined target looks healthy, injured, dangerous, disabled, weak, heavily armored, or likely beyond their current ability.

### Phase 14.2 - Medical and narcotic readability pass

Medical states, treatments, drugs, narcotics, sedatives, stimulants, painkillers, prosthetics, and cybernetics should be explained through inspection and Infopedia entries where player-facing.

The UI should distinguish:

- Injury.
- Pain.
- Bleeding.
- Infection.
- Poisoning or toxin exposure.
- Sedation.
- Stimulation.
- Addiction/dependency risk where implemented.
- Treatment available.
- Treatment missing.
- Prosthetic or cybernetic condition.
- Counterfeit, contaminated, or restricted medicine where known.

Exit criteria:

Medical and drug systems are not hidden status-effect soup; they can be inspected and explained.

## Phase 15 - Biography, identity, and social context readability

### Phase 15.1 - Entity biography and identity pass

The examination screen should consume character and social identity systems when information is known or discoverable.

Biography may include:

- Known name.
- Alias or uncertain identity.
- Faction.
- Rank or role.
- Workplace or home room when known.
- Relationship to local rooms, factions, contracts, pets, vehicles, schemes, noble houses, vendors, or markets.
- Known reputation or rumors.
- Known quest relevance.
- Known ownership or access relevance.
- Whether the information is confirmed, inferred, rumored, or unknown.

Exit criteria:

The examination screen does not treat all entities as blank stat blocks. It can show social and biographical context when the world has that information.

### Phase 15.2 - Rank, faction, and role label hygiene pass

Rank, faction, role, and profession labels should be compact and player-facing.

The game should avoid showing raw faction keys, role enum names, package strings, and debug labels to ordinary players. It should show readable labels such as `House Guard`, `Market Broker`, `Clinic Surgeon`, `Pet Vendor`, `Gang Courier`, `Noble House Servant`, `Faction Representative`, or `Unknown Armed Figure` as appropriate.

Exit criteria:

Social identity is readable and diegetic enough to support decisions without debug leakage.

## Phase 17 - Quest, ownership, pet, faction, and economy readability

### Phase 17.1 - Quest target and evidence readability pass

Examination and quest UI should help identify quest targets and proof-of-death evidence where appropriate.

Player-facing facts may include:

- This appears to be the quest target.
- This corpse contains required evidence.
- This item is required for a quest and has no carry weight burden.
- This target may produce proof-of-death evidence if killed.
- This container likely holds the requested item.
- This mission window is expiring soon.
- This scheme has moved from planning to execution.

The system must avoid revealing hidden quest outcomes or secret identities without a successful check, known record, or deliberate investigation path.

Exit criteria:

Quest guidance supports player action without collapsing mystery or hidden systems into free omniscience.

### Phase 17.2 - Economy, legality, and market readability pass

Vendors, markets, and trade goods should explain major access restrictions and provenance states in player-facing language.

Readable market states include:

- Legal.
- Restricted.
- Illicit.
- Stolen.
- Counterfeit.
- Contaminated.
- Noble-owned.
- Military-controlled.
- Black-market.
- Vault-only.
- Not for sale.
- Requires license.
- Requires reputation.
- Requires invitation.
- Scarce.
- Blockaded.

Exit criteria:

The player can understand why a market has, lacks, restricts, or refuses goods.

### Phase 17.3 - Pet ownership and care readability pass

Pet entities and pet-related UI should explain ownership, temperament, care, and interaction state.

Readable pet states include:

- Owned by player.
- Owned by faction.
- Owned by civilian/NPC.
- Available for sale.
- Available for adoption.
- Not for sale.
- Hungry.
- Thirsty.
- Sheltered.
- Neglected.
- Frightened.
- Hostile.
- Friendly.
- Inaccessible.
- Can be petted.
- Cannot be petted because of a clear reason.

Exit criteria:

Pet systems feel like visible living systems rather than hidden timers or inert sprites.

### Phase 17.4 - Player expansion heat readability pass

As the player grows from individual survivor to faction-scale actor, UI and Infopedia systems should explain heat and suspicion in compact terms.

Readable heat drivers include:

- Owning multiple rooms.
- Owning vehicles.
- Building production machines.
- Building defenses.
- Stockpiling restricted goods.
- Running shops or markets.
- Building clinics, labs, armories, motor pools, or faction-like infrastructure.
- Using stolen, illegal, military, or restricted blueprints.

Exit criteria:

Faction attention does not feel arbitrary when player construction and ownership start raising heat.

## Phase 18 - Infopedia, hot links, editor, and audit surfaces

### Phase 18.1 - Infopedia hot-link and deep reference pass

The Infopedia should support clickable or otherwise navigable references. If an entry mentions an item, entity, faction, mechanic, room, machine, vehicle, quest rule, pet rule, status, body part, recipe, material, blueprint, market, provenance state, or production process with a known entry, the player should be able to jump to that entry.

Infopedia entries should include, where applicable:

- What it is.
- What it does.
- Player-facing description.
- Important values.
- Statistics.
- Stat ranges.
- Quality-level ranges.
- Durability, damage, armor, nutrition, weight, volume, heat, suspicion, care, fuel, power, speed, capacity, or other relevant values.
- Where it is made.
- What facilities, machines, rooms, factions, or manufacturers produce it.
- What it is made from.
- What it can be used to make.
- What quests, contracts, schemes, or mechanics may reference it.
- Known warnings, access rules, or ownership issues.
- Related Infopedia entries.

Exit criteria:

The Infopedia becomes a true player reference system rather than a shallow glossary.

### Phase 18.2 - Mechanics and options Infopedia coverage pass

Every significant exposed mechanic, interaction, and option added through roadmap work should eventually have an Infopedia entry.

Coverage targets include:

- Look and Examine.
- Intent checks.
- Quest objective arrows and highlights.
- Proof-of-death evidence.
- Zero-weight quest-critical items.
- Pets, pet ownership, care, and petting interactions.
- Blueprints, permits, licenses, and construction restrictions.
- Player expansion heat and faction attention.
- Population provenance.
- Item provenance.
- Faction markets.
- Illicit narcotics economies.
- Noble luxury, draught items, and vault-only goods.
- Knowledge quality.
- Skill trees.
- Medical treatment.
- Prosthetics and cybernetics.
- Vehicles and structural scale.
- Ages of Control and leadership schemes.

Exit criteria:

A player can look up the systems the UI exposes instead of guessing from scattered tooltips.

### Phase 18.3 - Examination and Infopedia editor/audit pass

Future editor/audit surfaces should define and inspect:

- Examine screen sections.
- Entity biography fields.
- Body-plan templates.
- Stat approximation bands.
- Intent check rules.
- Refresh cadence.
- Intelligence and skill modifiers.
- Visibility and distance modifiers.
- Known, inferred, rumored, and unknown information states.
- Infopedia entry definitions.
- Infopedia links and backlinks.
- Infopedia categories.
- Item stat/value ranges.
- Recipe origin and downstream-use references.
- Mechanic explanation entries.

Editor/audit surfaces may expose semantic IDs where explicitly needed for development, but ordinary player UI must hide raw IDs.

Exit criteria:

Examine and Infopedia behavior can be audited or extended through owned data/editor surfaces instead of hardcoded central-class sprawl.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- `Look` and `Examine` are distinct but connected.
- `Examine` is available from valid looked-at entities.
- Examination screen hides raw IDs from ordinary player-facing UI.
- Intelligence and relevant checks affect information accuracy.
- Intent/state estimates refresh after several turns or repeated examination.
- Spending time examining can improve useful information.
- Body-plan and condition estimates are readable.
- Biography/social context appears when known or inferred.
- Quest, pet, ownership, economy, and faction relevance appear only when known, inferred, or successfully discovered.
- Quest objective arrows and pulsing highlights are useful, not clutter.
- Pet interaction feedback exists for pettible non-hostile pets where implemented.
- Access and construction denial messages explain the actual reason.
- Infopedia entries exist for major exposed mechanics, interactions, and options added by recent roadmap work.
- Infopedia hot links work between entries where implemented.
- Item entries include what the item is, what it does, values/statistics, quality ranges, where it is made, what it is made from, and what it can make where known.
- Developer/audit surfaces may show semantic IDs, but ordinary player UI does not.
- Player-facing text is compact, clear, and free of placeholder labels.

Exit criteria:

The game may claim in-depth examination, deep Infopedia reference, quest guidance, pet interaction feedback, and player-readable mechanics only after these systems are inspectable, useful, compact, and free of ordinary debug leakage.

## Non-goals for this milestone

This milestone does not require every hidden system to be revealed to the player. It requires that visible information, uncertainty, denial reasons, and reference material be useful and honest.

This milestone does not complete all UI implementation. It defines the readability, examination, and Infopedia targets that later UI implementation must satisfy.

This milestone does not require every item/entity/mechanic to have a perfect final entry immediately. It requires that exposed systems move toward Infopedia coverage and hot-linked player reference instead of remaining undocumented.

## Deferred checkpoint summary

The player should have a dedicated `Examine` option beyond ordinary `Look`. `Look` gives immediate impressions and a surface intent read. `Examine` opens a deeper character-screen-like view that uses intelligence, relevant skill, visibility, distance, prior knowledge, tools, and time spent observing to estimate biography, state, body plan, stats, injury, intent, equipment, faction, role, pet status, ownership, quest relevance, and danger. Examination should refresh every few turns or through continued attention so stale intent reads can change and careful observation can reveal more.

The Infopedia should become the long-term reference layer for the game's mechanics and interactions. Every significant exposed system should eventually have an Infopedia entry. Entries should be hot-linked so clicking a referenced item, entity, mechanic, room, machine, faction, status, blueprint, or recipe jumps to its entry. Item entries should explain what the item is, what it does, its values and statistics, quality-level ranges, where it is made, what it is made from, and what it can be used to make.

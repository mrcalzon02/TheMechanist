# Milestone - Examination, Intelligence-Based Entity Analysis, and Infopedia Depth Systems

This milestone document is a controlled phase-group expansion for the master development plan. It exists because the entity examination and Infopedia systems cross multiple roadmap phases and are detailed enough that they should not be buried as a few loose lines inside `MASTER_DEVELOPMENT_PLAN.md`.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file carries detailed implementation goals for an entity `Examine` command, intelligence-based information discovery, refreshed intent/state estimates, character-sheet-like entity inspection, and a much deeper Infopedia with hot links and full recipe/value/stat context.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable coding or packaging rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Scope

This milestone covers:

- A new look-adjacent command called `Examine`.
- Intelligence-based entity analysis.
- Intent checks that refresh over time.
- A deeper entity screen similar to the player character screen.
- Biography, faction, state, body-plan, and stat approximation display.
- Accuracy and uncertainty rules based on character intelligence, skill, distance, visibility, time spent examining, and prior knowledge.
- Infopedia entries for the mechanics, interactions, and options introduced across recent roadmap work.
- Infopedia hot linking from one entry to another.
- Full item/entity/mechanic entries describing what something is, what it does, statistics, quality ranges, production origin, ingredients, and downstream uses.

This milestone maps back to master-plan phases:

- Phase 4 - UI, input, rendering, and presentation containment.
- Phase 14 - Combat, health, unconsciousness, death, saves, and feedback.
- Phase 15 - Character creation, names, ranks, rosters, and social identity.
- Phase 17 - Economy, quests, contracts, faction reputation, pets, and ownership.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and endgame readiness.

## Dependency rule

The basic `Look` command may provide immediate visible impressions and a limited intent read. The deeper `Examine` command must build from that look/intent result rather than replacing it. Examination should improve or refresh knowledge over time, but it must not instantly reveal perfect hidden information without character capability, visibility, proximity, time investment, prior knowledge, or tools.

The Infopedia must become the player-facing reference layer for mechanics as they are added. If a mechanic, interaction, entity type, item family, quest rule, pet rule, vehicle rule, construction rule, faction-scheme rule, or provenance rule is exposed to the player, the Infopedia should eventually provide an entry that explains it in plain player-facing terms.

## Look versus Examine

`Look` and `Examine` should be separate but connected actions.

`Look` should answer: what can the player immediately observe?

Examples:

- Basic entity label.
- Visible faction or uniform cues.
- Approximate posture or behavior.
- Immediate threat impression.
- Surface intent read.
- Obvious injuries, equipment, or hostility.
- Whether a deeper examination is possible.

`Examine` should answer: what can the character infer after focusing attention?

Examples:

- More precise intent estimate.
- Approximate attributes and combat danger.
- Body-plan overview.
- Injury and condition estimates.
- Equipment and carried-item impressions.
- Biography or known identity information.
- Faction, rank, role, and social context if known or inferable.
- Relevant Infopedia links.

`Look` should be fast. `Examine` may take turns or require continued attention.

## Phase 4 addition - Examine command and entity examination screen pass

Add a Phase 4 child pass for the `Examine` command and entity examination screen.

Required outcome:

When the player looks at an entity, the UI should expose an additional `Examine` command when the target is valid. `Examine` opens a screen similar in structure to the character/dossier screen, but tuned for uncertainty and approximation rather than perfect internal truth.

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
- Armor/weapon danger estimate.
- Mood, morale, fear, anger, hostility, or confidence estimate where observable.
- Movement state.
- Current target or activity if readable.
- Known quest relevance.
- Known ownership/access relevance.
- Known pet/animal temperament where relevant.
- Hot links into Infopedia entries for entity type, faction, equipment, body-plan concepts, statuses, injuries, and mechanics.

The screen must hide raw internal identifiers, package IDs, object UUIDs, Java class names, registry names, and developer-only handles from normal player-facing display.

Exit criteria:

A player can look at an entity, choose `Examine`, and see a coherent character-sheet-like analysis screen that provides useful information without pretending the player has perfect omniscience.

## Phase 4 addition - Examination refresh and time-investment pass

Add a Phase 4 child pass for examination refresh behavior.

Required outcome:

Intent and state checks should refresh every few turns while the player continues examining or repeatedly re-examines the entity. Spending time examining should be able to improve confidence, reveal additional details, correct earlier uncertainty, or update stale intent information.

Refresh behavior should consider:

- Character intelligence.
- Relevant perception, medicine, combat, animal-handling, social, technical, or investigative skills if present.
- Distance to target.
- Lighting and visibility.
- Obstructions.
- Whether the target is moving, fighting, hiding, wounded, disguised, unconscious, hostile, or cooperating.
- Whether the examiner has prior knowledge of the species, faction, role, equipment, machine, vehicle, status, or condition.
- Whether tools, scanners, optics, medical gear, or faction records are available.

The system should avoid excessive turn spam. Examination refresh should update the screen and recent-action feedback compactly.

Exit criteria:

Spending extra time examining an entity can produce more reliable or more complete information, and stale intent estimates do not remain frozen forever.

## Phase 15 addition - Entity biography, identity, and social context pass

Add a Phase 15 child pass for entity biography and identity information.

Required outcome:

The examination screen should consume the character/social identity systems when information is known or discoverable. Named characters, faction members, leaders, workers, guards, clergy, specialists, pets, animals, vendors, quest targets, and other meaningful actors should expose appropriate biographical context.

Biography may include:

- Known name.
- Alias or uncertain identity.
- Faction.
- Rank or role.
- Workplace or home room when known.
- Relationship to local rooms, factions, contracts, pets, vehicles, or schemes.
- Known reputation or rumors.
- Known quest relevance.
- Known ownership or access relevance.
- Whether the information is confirmed, inferred, rumored, or unknown.

Exit criteria:

The examination screen does not treat all entities as blank stat blocks. It can show social and biographical context when the world has that information.

## Phase 14 addition - Body-plan, injury, combat-readiness, and condition approximation pass

Add a Phase 14 child pass for body-plan and condition estimates.

Required outcome:

Examination should show body-plan approximations and current condition estimates when appropriate. For humanoids, animals, creatures, machines, and vehicles, the system should provide a readable overview of major parts or condition areas without requiring perfect exact numbers.

For living entities, this may include:

- Head/torso/limbs or species-appropriate body regions.
- Obvious wounds.
- Limping, bleeding, stunned, unconscious, exhausted, frightened, or poisoned state.
- Approximate health band.
- Combat readiness estimate.
- Weapon and armor danger estimate.

For machines or vehicles, this may include:

- Chassis or frame.
- Power/fuel state.
- Mobility systems.
- Weapons/mounts if visible.
- Armor/structural condition.
- Obvious leaks, fire, damage, missing parts, or disabled state.

Exit criteria:

The player can make a reasonable decision about whether an examined target looks healthy, injured, dangerous, disabled, weak, heavily armored, or likely beyond their current ability.

## Phase 17 addition - Examination support for quest targets, pets, ownership, and faction standing

Add a Phase 17 child pass connecting examination to quest/economy/ownership systems.

Required outcome:

Examine should help identify quest targets, proof-of-death targets, pet ownership status, vendor animals, owned containers, restricted assets, faction representatives, hostile actors, and scheme participants where appropriate.

Examination should expose player-useful facts such as:

- This appears to be the quest target.
- This entity may carry proof-of-death evidence if killed.
- This pet appears owned by someone else.
- This animal is non-hostile and can be petted.
- This vendor appears to sell pets or animals.
- This guard appears to belong to a faction that currently dislikes you.
- This entity is involved in an active or rumored faction scheme.
- This object or room is restricted, owned, public, stolen, or forbidden.

The system must avoid revealing hidden quest outcomes or secret identities without a successful check, known record, or deliberate investigation path.

Exit criteria:

Examination supports play decisions around quests, ownership, pets, factions, and active schemes without turning every hidden system into free information.

## Phase 18 addition - Examination and Infopedia editor/audit pass

Add a Phase 18 child pass for data/editor/audit support.

Required outcome:

Future editor/audit surfaces should define and inspect:

- Examine screen sections.
- Entity biography fields.
- Body-plan templates.
- Stat approximation bands.
- Intent check rules.
- Refresh cadence.
- Intelligence and skill modifiers.
- Visibility/distance modifiers.
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

## Phase 18 addition - Infopedia hot-link and deep reference pass

Add a Phase 18 child pass for Infopedia hot linking and in-depth entries.

Required outcome:

The Infopedia should support clickable/hot-linked references. If an entry mentions an item, entity, faction, mechanic, room, machine, vehicle, quest rule, pet rule, status, body part, recipe, material, or production process with a known entry, the player should be able to jump to that entry.

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

Quality-aware entries should present ranges instead of hiding meaningful variation. For example, crude, standard, refined, high-grade, damaged, pristine, improvised, industrial, military, or faction-specific variants should be represented where the game supports them.

Exit criteria:

The Infopedia becomes a true player reference system rather than a shallow glossary. Players can follow links from mechanics to items, items to recipes, recipes to facilities, facilities to factions, and entities to body plans or statuses.

## Phase 19 addition - Examination and Infopedia release audit

Add a Phase 19 child pass for release readiness.

Required audit checks:

- `Look` and `Examine` are distinct but connected.
- `Examine` is available from valid looked-at entities.
- Examination screen hides raw IDs from normal player-facing UI.
- Intelligence and relevant checks affect information accuracy.
- Intent/state estimates refresh after several turns or repeated examination.
- Spending time examining can improve useful information.
- Body-plan and condition estimates are readable.
- Biography/social context appears when known or inferred.
- Quest, pet, ownership, and faction relevance appear only when known, inferred, or successfully discovered.
- Infopedia entries exist for major exposed mechanics, interactions, and options added by recent roadmap work.
- Infopedia hot links work between entries.
- Item entries include what the item is, what it does, values/statistics, quality ranges, where it is made, what it is made from, and what it can make.
- Player-facing text is compact, clear, and free of placeholder labels.
- Developer/audit surfaces may show semantic IDs, but ordinary player UI does not.

Exit criteria:

The game may claim an in-depth examination system and a deep Infopedia only after the player can examine entities meaningfully, gain better information over time, and look up exposed mechanics/items/entities through hot-linked entries.

## Deferred checkpoint line - Examination and Infopedia depth

The player should have a dedicated `Examine` option beyond ordinary `Look`. `Look` gives immediate impressions and a surface intent read. `Examine` opens a deeper character-screen-like view that uses intelligence, relevant skill, visibility, distance, prior knowledge, and time spent observing to estimate biography, state, body plan, stats, injury, intent, equipment, faction, role, pet status, ownership, quest relevance, and danger. Examination should refresh every few turns or through continued attention so stale intent reads can change and careful observation can reveal more.

The Infopedia should become the long-term reference layer for the game's mechanics and interactions. Every significant exposed system should eventually have an Infopedia entry. Entries should be hot-linked so clicking a referenced item, entity, mechanic, room, machine, faction, status, or recipe jumps to its entry. Item entries should explain what the item is, what it does, its values and statistics, quality-level ranges, where it is made, what it is made from, and what it can be used to make.

## Master-plan index patch target

During the next safe `MASTER_DEVELOPMENT_PLAN.md` patch, add a brief milestone reference near `## Durable phase roadmap`:

```text
Detailed phase-group expansions may live in controlled milestone documents when the master plan becomes unsafe to edit monolithically. See `docs/MILESTONE_INDEX.md` for the active milestone map, `docs/MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` for agriculture and pet systems, and `docs/MILESTONE_EXAMINATION_AND_INFOPEDIA_SYSTEMS.md` for intelligence-based examination and Infopedia depth systems.
```

Also update the master plan dependency rule to include:

```text
entity examination depends on the basic look/intent surface, and Infopedia hot-link depth depends on stable semantic entries for items, entities, mechanics, rooms, machines, recipes, factions, and statuses
```

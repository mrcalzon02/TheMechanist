# Milestone - World Content, Agriculture, Animals, Pets, and Room-Stamp Systems

This milestone document is a controlled phase-group expansion for the master development plan. It exists because the master plan has grown large enough that adding detailed content systems directly to the central roadmap risks unsafe full-document replacement. The master development plan remains the authoritative index and phase map; this file carries the detailed implementation goals for a distributed milestone group until the master plan can safely reference it through a small index patch.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable coding or packaging rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Milestone partition rule

The roadmap may be distributed into milestone documents when a phase cluster becomes too large for safe editing inside `MASTER_DEVELOPMENT_PLAN.md`. Each milestone document must remain explicitly tied to master-plan phase numbers, must not invent unrelated side-roadmaps, and must name how its contents map back into the master plan.

Allowed milestone documents:

- Must group related phase work by functional milestone, not by random notes.
- Must preserve master-plan phase numbering and exit criteria.
- Must describe dependencies, implementation intent, player-facing effects, editor/audit needs, release-audit needs, and non-goals.
- Must not replace `DEVELOPMENT_HISTORY.md`, `STANDARDS_AND_PRACTICES.md`, or `MASTER_GOVERNANCE_REVISION_II.md`.
- Must be referenced from `MASTER_DEVELOPMENT_PLAN.md` during the next safe index update.

## Scope

This milestone covers agriculture, food-production spaces, cloning facilities, animal spaces, pet vendors, pet zoos, pet ownership, pet care, pet interaction, pet sound/feedback hooks, and room-generation stamps that require these assets.

This milestone intentionally connects multiple master phases:

- Phase 2 - Master asset integration and world-usable asset promotion.
- Phase 4 - UI, input, rendering, and presentation containment.
- Phase 16 - World generation, districts, sewers, temples, estates, and strategic simulation.
- Phase 17 - Economy, quests, contracts, faction reputation, pets, and ownership.
- Phase 18 - Editor, localization, modding, and content pipeline.
- Phase 19 - Polish, packaging, QA, and endgame readiness.

## Dependency rule

Agricultural, animal, and pet asset promotion must precede farm, garden, cloning, animal, and pet room-stamp generation. Pet-entity definitions must precede pet ownership, pet buying, pet care, and pet interaction logic. Room generation must not place a `pet zoo`, `pet vendor`, `kennel`, `cattery`, `animal pen`, or `rodent cage` as a decorative name only; it must either use real semantic assets and inspectable purpose or remain unavailable until the supporting definitions exist.

If the game displays a non-hostile pet to the player, the interaction model must acknowledge the player-facing expectation that the pet can be petted unless a real state blocks the interaction.

## Phase 2 addition - Agriculture, animal, and pet asset promotion pass

Add a Phase 2 child pass for agriculture, animals, and pet assets.

Required outcome:

Identify and promote farm, garden, crop, hydroponic, mushroom bed, cloning, incubator, animal pen, kennel, cattery, rodent cage, pet zoo, pet vendor, feed bowl, water bowl, leash, toy, bedding, enclosure, animal-sound, pet-sound, and pet-entity assets into semantic registry entries before room stamps or pet systems consume them.

The asset promotion must record:

- Semantic ID.
- Player-facing label.
- Asset family.
- Intended room/facility contexts.
- Whether it is world-usable, UI-only, editor-only, audit-only, placeholder, quarantined, or publish-safe.
- Species or animal category where relevant.
- Interaction category where relevant.
- Sound hook category where relevant.
- Whether the asset is suitable for room generation, entity placement, inventory, vendor display, care infrastructure, or inspection UI.

Exit criteria:

The project can tell which agriculture, animal, and pet assets exist, which are unused, which are safe, which are placeholders, and which room-generation or pet systems may consume them.

## Phase 4 addition - Pet interaction feedback pass

Add a Phase 4 child pass for pet interaction feedback.

Required outcome:

Non-hostile pet entities must expose clear interaction feedback when the player can pet them. Dog-like pets should support `head pats` and bark or otherwise vocalize appropriately. Cat-like pets should support `scritches` and meow, purr, or otherwise respond appropriately. Mouse and rat pets should support `nose boops` and squeak, chitter, or otherwise respond appropriately.

The interaction should provide compact feedback through one or more of:

- Status text.
- Floating text.
- Small animation cue.
- Sound hook.
- Mood/affinity change.
- Pet inspection update.

Blocked interactions must explain the block in useful language. The player should not see raw internal state such as `pettable=false`, `entity.pet_owner_id`, or a registry key. Examples of valid explanations include: the animal is hostile, frightened, asleep, out of reach, behind a locked enclosure, owned by someone who forbids interaction, injured, or currently working.

Exit criteria:

A visible non-hostile pet has an obvious interaction path, and the player receives a small but meaningful acknowledgement when they pet it.

## Phase 16 addition - Agriculture, cloning, animal, and pet room-stamp pass

Add a Phase 16 child pass for agriculture, cloning, animal, and pet room generation.

Required outcome:

Generate farms, gardens, hydroponic plots, mushroom beds, cloning rooms, incubators, animal pens, livestock rooms, kennels, catteries, rodent cages, pet zoos, pet vendors, feed storage, water stations, cleaning stations, and animal-care rooms where faction history, economy, food supply, research, trade, or civilian life justifies them.

Room stamps must include:

- Purpose and ownership.
- Valid factions or settlement contexts.
- Required assets and fallback behavior.
- Animal/pet entity spawn rules where applicable.
- Care infrastructure such as feed, water, bedding, cleaning, and enclosures where applicable.
- Inspection labels that explain what the space does.
- Connection to economy, population, food supply, research, trade, or civilian life.

These rooms must not be decorative room names only. A kennel should imply dogs or dog-like pets, care infrastructure, handlers or owners, and interaction possibilities. A cattery should imply cats or cat-like pets. A rodent cage should imply mice, rats, or similar small animals. A pet vendor should imply an NPC, stock, ownership transfer, price, restrictions, and care expectations.

Exit criteria:

World generation can place agriculture, cloning, animal, and pet spaces as meaningful rooms with semantic assets, inspectable purpose, and future pet/economy hooks.

## Phase 17 addition - Pet purchase, adoption, and ownership pass

Add a Phase 17 child pass for pet purchase, adoption, and ownership.

Required outcome:

Pet vendors, pet zoos, animal pens, faction handlers, and civilian owners should support buying, adopting, assigning, naming, and keeping pets where the setting and ownership rules allow it.

Pet records should track:

- Species or animal category.
- Name, if named.
- Current owner.
- Source, such as vendor, adoption, gift, rescue, faction handler, or found animal.
- Temperament.
- Hostile, neutral, friendly, frightened, tame, feral, working, injured, or inaccessible state.
- Home room or enclosure.
- Whether the pet is available for normal friendly interaction.
- Whether the player is allowed to interact with it.

Ownership should respect faction and property rules. Buying or adopting a pet should not silently bypass ownership, theft, access, faction standing, or restricted-area rules.

Exit criteria:

The player can acquire and keep pets through coherent ownership pathways, and pet entities have enough record structure to persist and be inspected.

## Phase 17 addition - Pet feeding, care, and upkeep pass

Add a Phase 17 child pass for pet feeding, care, and upkeep.

Required outcome:

Pets should have care requirements appropriate to their species and abstraction tier. At minimum, the system should support feeding, watering, shelter or enclosure, cleanliness, and owner or handler responsibility.

Care should be visible through compact inspection rather than hidden timer math. The player should be able to understand whether a pet is fed, thirsty, neglected, sheltered, frightened, hostile, injured, or content without opening a developer surface.

Care interactions may use:

- Food items.
- Water bowls.
- Feed bowls.
- Enclosures.
- Bedding.
- Cleaning stations.
- NPC handlers.
- Room infrastructure.
- Scheduled care tasks.

Exit criteria:

Pets are not merely collectible sprites. They can be cared for, inspected, and supported through rooms, items, and NPC or player responsibility.

## Phase 17 addition - Pettible interaction and species response pass

Add a Phase 17 child pass for pettible interactions.

Required outcome:

All non-hostile pet entries should be marked as pettible. If the player can see a non-hostile pet and can reach it, the game should provide an interaction unless a real state blocks it.

Required species interaction verbs:

- Dog-like pets: `head pats`.
- Cat-like pets: `scritches`.
- Mouse and rat pets: `nose boops`.

Required feedback examples:

- Dog-like pets bark, wag, whine, huff, lean in, or otherwise respond appropriately.
- Cat-like pets meow, purr, blink, stretch, rub against the player, or otherwise respond appropriately.
- Mouse and rat pets squeak, chitter, sniff, twitch whiskers, climb, or otherwise respond appropriately.

The interaction must be player-facing and compact. It may be delightful; it must not be hidden debug state.

Blocked interactions:

If the animal is hostile, frightened, inaccessible, forbidden by an owner, asleep, working, injured, or otherwise unsafe, the game should explain why the player cannot pet it right now.

Exit criteria:

If the player sees the dog, they can pet the dog unless there is a real in-world reason they cannot. The same standard applies to cats, mice, rats, and other non-hostile pet entries with species-appropriate verbs.

## Phase 18 addition - Agriculture, animal, pet room-stamp, and pet-entity editor/audit pass

Add a Phase 18 child pass for editor and audit support.

Required outcome:

Future data/editor surfaces should define and inspect:

- Farm stamps.
- Garden stamps.
- Hydroponic stamps.
- Cloning rooms.
- Incubators.
- Animal pens.
- Pet zoos.
- Pet vendors.
- Kennels.
- Catteries.
- Rodent cages.
- Feed and water infrastructure.
- Pet species definitions.
- Pet ownership records.
- Feeding requirements.
- Pettible flags.
- Species-specific petting verbs.
- Sound hooks.
- Hostile-state interaction blocks.
- Compact player-facing labels.

Editor/audit surfaces may expose semantic IDs where explicitly needed for development, but ordinary player UI must hide raw IDs.

Exit criteria:

Agriculture, animal, pet rooms, and pet entities can be inspected or extended through owned data/editor surfaces rather than hardcoded central-class sprawl.

## Phase 19 addition - Agriculture, animal, and pet release audit

Add a Phase 19 child pass for agriculture, animal, and pet release readiness.

Required audit checks:

- Farm, garden, cloning, animal, and pet room stamps exist only where supported by semantic assets and generation rules.
- Promoted agriculture and pet assets have publish-safe status.
- Pet vendor behavior is clear and not placeholder-only.
- Pet ownership persists and respects access/ownership rules.
- Feeding and care inspection is visible and compact.
- Pettible flags are present for non-hostile pets.
- Dog-like pets support head pats.
- Cat-like pets support scritches.
- Mouse and rat pets support nose boops.
- Bark, meow, purr, squeak, chitter, or equivalent sound/feedback hooks exist where available.
- Hostile-state, inaccessible-state, and owner-forbidden blocks are explained clearly.
- Player-facing text avoids raw IDs and placeholder labels.

Exit criteria:

The game may claim agriculture, animal rooms, pet vendors, pet ownership, pet care, and pettible interactions only after these checks pass.

## Deferred checkpoint line - Agriculture, animals, and pet entities

Room generation should include farms, gardens, hydroponic growing spaces, cloning rooms, animal pens, pet zoos, pet vendors, kennels, catteries, rodent cages, and animal-care spaces where the zone economy, faction history, food supply, science, trade, or civilian life justifies them.

Pet entities should support buying, adopting, keeping, feeding, watering, and compact care inspection. Every non-hostile pet entry should be pettible: dog-like pets allow head pats and should bark or respond appropriately; cat-like pets allow scritches and should meow, purr, or respond appropriately; mouse and rat pets allow nose boops and should squeak, chitter, or respond appropriately. If the animal is hostile, unsafe, inaccessible, or otherwise not available for friendly interaction, the game should explain why the player cannot pet it right now.

## Master-plan index patch target

During the next safe small edit to `MASTER_DEVELOPMENT_PLAN.md`, add a brief milestone reference near the durable phase roadmap:

```text
Detailed phase-group expansions may live in controlled milestone documents when the master plan becomes unsafe to edit monolithically. See `docs/MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` for agriculture, animal-room, pet-room, pet-entity, pet-care, and pettible-interaction roadmap details.
```

Also update the master plan dependency rule to include agriculture/animal/pet asset promotion before farm/garden/animal/pet room-stamp generation and pet ownership systems.

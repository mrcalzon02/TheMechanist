# Milestone Index and Phase Partition Map

This document is a controlled index for distributed milestone documents. It exists because `MASTER_DEVELOPMENT_PLAN.md` has grown large enough that some detailed phase clusters should be maintained in separate milestone documents rather than expanded through repeated full-file replacement.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is a secondary navigation aid for milestone-sized phase groups and must be referenced from the master plan during the next safe index patch.

## Document ownership rule

Milestone documents are allowed only when a phase cluster becomes too large or too detailed to safely maintain inside the master plan.

Milestone documents must:

- Map back to existing master-plan phase numbers.
- Group related work by functional milestone.
- Preserve the master plan's dependency logic.
- Include player-facing goals, data/editor implications, release-audit implications, and non-goals.
- Avoid becoming changelogs, scratchpads, or disconnected design dumps.
- Avoid raw implementation claims unless code/data has actually been implemented.

Milestone documents must not replace:

- `MASTER_DEVELOPMENT_PLAN.md` for roadmap authority.
- `DEVELOPMENT_HISTORY.md` for completed work.
- `STANDARDS_AND_PRACTICES.md` for durable implementation rules.
- `MASTER_GOVERNANCE_REVISION_II.md` for high-level doctrine.

## Current milestone documents

### `MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md`

Phase groups:

- Phase 2 - Asset promotion.
- Phase 4 - UI/input/render feedback.
- Phase 16 - World generation and room stamps.
- Phase 17 - Economy, ownership, pets, and care.
- Phase 18 - Editor/audit content pipeline.
- Phase 19 - Release audit.

Scope:

- Farms.
- Gardens.
- Hydroponics.
- Mushroom beds.
- Cloning rooms.
- Incubators.
- Animal pens.
- Livestock rooms.
- Pet zoos.
- Pet vendors.
- Kennels.
- Catteries.
- Rodent cages.
- Pet buying/adoption.
- Pet ownership and naming.
- Pet feeding, watering, and care.
- Mandatory pettible flags for non-hostile pets.
- Dog head pats.
- Cat scritches.
- Mouse and rat nose boops.
- Bark, meow, purr, squeak, chitter, and equivalent feedback hooks.
- Hostile, inaccessible, owner-forbidden, frightened, or unsafe interaction blocks.

Status:

- Created as a controlled milestone document.
- Needs a small `MASTER_DEVELOPMENT_PLAN.md` index reference during a safe master-plan patch.
- Issue #38 remains the recovery/migration tracker until the master plan has a direct pointer to the milestone.

## Candidate future milestone partitions

These are not created yet. They are candidate groups to prevent the master plan from becoming unreadable or unsafe to edit.

### Candidate - Asset Promotion and Package Readiness

Likely phase groups:

- Phase 2.
- Phase 18 asset/editor portions.
- Phase 19 asset release-audit portions.

Purpose:

Track semantic asset IDs, unused asset discovery, publish-safe asset clearance, optional art packs, low_32 lean runtime packaging, external registry declaration rules, editor palette integration, and release audit requirements.

### Candidate - Population, Provenance, and Economic Identity

Likely phase groups:

- Phase 8.
- Phase 9.
- Phase 17 economy/contract portions.
- Phase 19 provenance audit portions.

Purpose:

Track population provenance, workforce identity, item provenance, facility production, contract sources, role-aware labor, reinforcement origins, and the bridge between who exists and who made or handled a thing.

### Candidate - Vehicles, Structural Scale, and Strategic Power

Likely phase groups:

- Phase 10.
- Phase 14 vehicle/combat portions.
- Phase 16 vehicle district portions.
- Phase 19 vehicle release-audit portions.

Purpose:

Track vehicle assets, component factories, manufacturers, body schemas, damage models, repairs, ownership, road-only movement, motor pools, APCs, tanks, strategic faction power, and comparative damage/durability scale.

### Candidate - Ages of Control and Faction Schemes

Likely phase groups:

- Phase 11.
- Phase 16 Ages-of-Control worldgen integration.
- Phase 17 scheme quest lifecycle portions.
- Phase 19 scheme release-audit portions.

Purpose:

Track historical zone generation, room control, faction expansion, concessions, assaults, sabotage, political schemes, cross-zone plans, active gameplay scheme activation, visible events, and scheme-quest windows.

### Candidate - Quest Lifecycle, Guidance, Evidence, and Standing

Likely phase groups:

- Phase 4 quest overlay portions.
- Phase 17 quest/contract portions.
- Phase 18 scheme quest editor/audit portions.
- Phase 19 quest release-audit portions.

Purpose:

Track quest lifecycle timing, non-synchronized activation times, minimum two-day active windows, planning/execution/cooldown states, NPC countdowns, neutral missed-quest standing behavior, objective arrows, pulsing highlights, zero-weight quest items, proof-of-death evidence, and compact player-facing mission text.

### Candidate - Construction, Ownership, Heat, and Player-Faction Emergence

Likely phase groups:

- Phase 7.
- Phase 12.
- Phase 17 player expansion heat portions.
- Phase 19 expansion heat release-audit portions.

Purpose:

Track access checks, ownership categories, player-owned rooms, machines, vehicles, bases, defenses, player expansion heat, suspicion, faction attention thresholds, and the transition from individual survival to faction-scale visibility.

## Master-plan patch target

During the next safe `MASTER_DEVELOPMENT_PLAN.md` patch, add a short reference near `## Durable phase roadmap`:

```text
Detailed phase-group expansions may live in controlled milestone documents when the master plan becomes unsafe to edit monolithically. See `docs/MILESTONE_INDEX.md` for the active milestone map and `docs/MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` for agriculture, animal-room, pet-room, pet-entity, pet-care, and pettible-interaction roadmap details.
```

Also update the master plan dependency rule to include:

```text
agricultural, animal, and pet asset promotion precedes farm/garden/animal/pet room-stamp generation and pet ownership systems
```

## Anti-sprawl rule

Do not create a milestone document merely because an idea is large. Create one only when a functional phase group has enough detailed requirements that keeping the full expansion inside `MASTER_DEVELOPMENT_PLAN.md` would make the master plan harder to review, harder to patch, or more dangerous to update.

When uncertain, keep the master plan as the index, use this file as the milestone map, and create at most one milestone document for a tightly related phase cluster.

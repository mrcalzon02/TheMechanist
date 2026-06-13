# Milestone 02 Next Step - Movement Execution Unification

## Why this slice exists

The project now contains:

- actor-layer push/squeeze resolution
- movement planning readability
- standable-tile recovery search
- runtime recovery bridge

The remaining risk is divergence between:

1. movement preview
2. movement planning
3. final movement execution

A destination that previews as reachable must not later be rejected by a different ruleset.

## Required next implementation slices

Current checkpoint: Slices 1 through 4 are implemented and smoke-covered. Movement execution unification is ready for milestone review.

### Slice 1

Status: complete.

Locate every runtime player-movement commit path.

Create an audit documenting:

- keyboard movement path
- mouse movement path
- queued movement path
- controller movement path
- auto movement path

### Slice 2

Status: complete.

Route occupied-tile movement attempts through the actor-layer resolver.

Legacy direct occupancy checks should become a thin bridge into:

`ZoneTileMovementResolutionAuthority`

### Slice 3

Status: complete.

Pause menu recovery button.

Add a visible player-facing Unstuck action.

The button should:

- call MovementPlanningAuthority.applyNearestStandableRecovery(...)
- report success or failure
- never silently teleport
- leave an audit trail in the event log

### Slice 4

Status: complete.

Movement debug overlay.

Expose:

- destination tile
- occupancy result
- push/squeeze result
- recovery result

for smoke testing and validation.

## Success criteria

A movement destination evaluated by preview, planning, and execution produces identical acceptance or rejection decisions and shares the same underlying authority chain.

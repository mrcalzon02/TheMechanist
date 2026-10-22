# Multi-floor buildings, open volumes, and apartment stacks

Status: **deterministic blueprints and initial physical per-floor stamping integrated**. Remaining: atomic cross-slice site acceptance, vertical travel, persisted building modifications, roof/slab rendering, and finished 3D atrium presentation.

This design extends the existing 3x3-sector / 3x3-zone / ten-floor atlas. It does **not** reinterpret one atlas floor as one building floor or claim that existing globally aligned elevator glyphs already form individual multi-storey buildings.

## Architectural rule

A **Building** is one named, persistent property and structural footprint. It may span multiple atlas floor slices. It contains many **FloorPlates**, and each plate contains multiple separately usable **Rooms** and non-room **Spaces**. Individual rooms keep their own functions, faction leases, supplies, populations, and ownership state, but share one immutable `buildingId` for grouping and building-level decisions. A building spanning three floors with twelve apartments counts as **one building and twelve rooms**, not twelve separate buildings.

Every instance is addressed using:

`(atlasSeed, sectorX, sectorY, zoneX, zoneY, stableSiteSlot) -> buildingId`.

Every room within that instance is addressed using:

`(buildingId, atlasFloor, stableRoomKey) -> roomId`.

**Do not use mutable `world.rooms` array indexes as persistent identities.** The current room-generation code may reject, remove, and renumber room records. Resolve building membership into room indexes only after accepted placement and store the stable room association in a separate structure registry. Preserve explicit per-room and building ownership.

## One plan shared by all floors

The atlas currently derives a different procedural *slice seed* for each floor and generates slices lazily. A building plan must be keyed from the **shared atlas seed**, zone coordinates, and stable site slot; it must **not** depend on the current slice seed, generation visit order, global RNG consumption, or floor number.

The building record owns:

- A fixed 2D footprint in zone-local tile coordinates, across the full floor span.
- A contiguous floor span, within main floors 1–10; sewer B layers are separate and do not count as normal upper floors.
- A floor plate for every floor, with apartments, storage, lobbies, businesses, production rooms, halls, balconies, stair landings, lift stops, and open volumes.
- One stair/elevator shaft identity across its connected levels, with a valid aligned landing on both ends of each vertical link.
- Individual room references and their economic/ownership state, plus a distinct building-level title and services.
- The roof, support/slab geometry, entrances and exposure to street/inner pedestrian corridors.

A building can change tenant, room function, or faction ownership without regenerating its footprint. Permanent construction/demolition must be stored as authoritative changes to the building, not lost when a distant floor is lazily loaded.

## Initial executable geometry

Implementation: `src/mechanist/VerticalBuildingBlueprintAuthority.java`.

The first deterministic local blueprints establish exact geometry, **not yet world placement coordinates**:

| Blueprint | Floor span | Footprint | Usable room count | Connections |
|---|---|---|---|---|
| Apartment Stack | 2–4 | 32 × 26 tiles | 12 rooms grouped as one building | Aligned stairs and lift |
| Apartment Court | 2–4 | 32 × 26 tiles | 12 rooms grouped as one building | Aligned stairs and lift |
| Upper Atrium Hall | 6–8 | 38 × 32 tiles | 13 rooms grouped as one building | Aligned stairs and lift |

Apartment plates contain four individually counted rooms plus a corridor, landing, and lift. Their bottom plate replaces one apartment with an entrance lobby; the top plate replaces another with a shared service room. These are not rooms randomly stamped on top of unrelated roads: all plates inherit the same shell.

The atrium has a walkable gathering floor on level 6. The matching 20 × 15 local rectangle on levels 7 and 8 is `OPEN_TO_BELOW`, which **does not create a walkable floor**. Rooms and railed balconies surround this opening at the higher levels.

Conceptually:

```text
Floor 8     gallery rooms | balcony | OPEN AIR | balcony | stairs/lift
Floor 7     gallery rooms | balcony | OPEN AIR | balcony | stairs/lift
Floor 6     gallery rooms |       WALKABLE ATRIUM HALL     | stairs/lift
                       one persistent building footprint
```

The model validates intra-level collision, building boundary containment, complete ordered floors, unique room/space IDs, aligned consecutive stair/lift links, and the aligned floor-or-void volume below every upper-level opening.

## Placement integration: one transaction spanning all floors

The present `World.worldgenPhaseRoomsAndFactionClaims` chooses rooms by per-floor road frontage and treats each candidate as independent. Stacked buildings cannot be added safely by simply repeating `tryPlaceRoadFirstRoom` at the same x/y; the separate floors have different slice seeds and may have different zone themes.

Introduce a **MultiFloorBuildingPlacementAuthority** as the consumer of the blueprint, before ordinary single-floor room stamping:

1. Choose a parcel with one street/service approach or an internally connected access lane. Prefer varied depths behind the roadfront rather than requiring every apartment door to face a road.
2. For **each affected floor**, inspect proposed street networks, central plaza, global transition reservations, pre-existing saved structures, exterior wall envelope, and protected interstitial infrastructure. All projected building slabs, shafts, external entrances, and open volumes must pass.
3. Reserve the entire stack atomically in a persistent building registry. **Any collision on any floor rejects or re-sites the whole building**; do not accept a building on floor 2 and silently omit its floor 3.
4. Generate shell, lobby/entrance, shared corridors and slab/stair/lift shafts from the single plan. Carve each room's own interior and door on its specified floor. Mark the corresponding `roomId -> buildingId` membership.
5. On upper floors, stamp **no floor tiles** where `OPEN_TO_BELOW` is declared. Attach balustrades, collision and fall rules on the balcony edges and a roof or overhead volume above the final level.
6. Join the ground-level lobby to the reachable road/corridor graph. Join upper-level rooms to their own floor plate's stairs and lift. Validate a full 3D path from every room to that lobby using horizontal **and** vertical edges.
7. Commit and compile semantic tile descriptors for each touched floor. On failure, roll back all affected floor footprints and reservations.

Importantly, a world floor may have an overarching faction/zone archetype different from the building's owner. A residential block extending between floors 2–4 must retain its plan even if a particular floor has a different underhive zone theme. A global road or floor-specific feature must not overwrite a confirmed building.

## Spaces, collisions, rendering, and NPCs

The existing `ZoneTileState` supports room, corridor and vertical-transition slots, but it does not yet encode a genuine cross-floor open volume. Add an explicit `OPEN_TO_BELOW` semantic surface/space type instead of recycling the exterior/interwall void glyph. The upper opening is non-walkable and blocks floor spawning, furnishing and item placement unless the object is explicitly suspended; it does **not** count as a room.

The renderer must draw the atrium's lower level through this aperture with correct depth, occlusion, balconies and railings, using graphical tile/sprite assets. The map and inspection UI should show one expandable building entry with floors and rooms, clear stairs/lifts, and the lower-floor void outline. No ASCII fallback is an acceptable substitute for a missing required art binding.

For movement, adjacent floor destinations require matching shaft position and building ID, not just a generic `E` glyph on two unrelated levels. Lift service availability, lock state, ownership, and stair pathability are independently simulated. NPC homes/duties can be keyed to `buildingId + roomId`, so replacements, persistent memories, guards, merchants and tenants return to the correct location even when another level is not loaded.

## Save compatibility and scalability

Canonical design is deterministic, but **building state is not merely regenerated from a seed**. Save the accepted building site and original blueprint version, assigned owners, unit occupants, doors, built/destroyed state, altered room partitions, and lift operation. Once committed, do not re-site buildings on upgrades or reinterpret player modifications. Old single-floor saves load as legacy standalone rooms with no fabricated building membership.

A completed building registry should be authoritative for the full sector/zone across lazily loaded floors. Distant floors may simulate occupants and supply at an abstract resolution, but all floors consume the same immutable geometry and persistent modifications.

## Development acceptance gates

**Completed in the initial blueprint pass:** deterministic stable IDs, multiple buildings per zone, coherent floor plates, distinct room/building counts, non-walkable upper voids, aligned stair/lift edges, and strict constructor invariants. `VerticalBuildingBlueprintSmoke` is included in Gate 3.

**Current physical pass:** `MultiFloorBuildingPlacementAuthority` reserves two apartment footprints and one upper atrium footprint using the shared atlas seed, materializes rooms into their corresponding main-floor slices, maintains room-to-building references, and rejects a blocked site or road approach before stamping its plate. Its geometric `MultiFloorBuildingPlacementSmoke` has passed on floors 2, 4, and 7. An explicit structured `OPEN_VOLUME` tile now keeps atrium voids distinguishable from generic exterior voids.

**Next: atomic cross-slice placement and real world acceptance.** Create one real placed residential building in a generated roaded zone. Verify lower, middle and upper floors agree on x/y footprint, confirm no road/plaza overwrite, physically connect a lobby, verify stairs/lift transitions, save, unload, reload in a different order and compare the structure/room IDs and tile geometry.

**Then:** atrium stamp/renderer with looking down across levels; two-sided fall/rail validation; apartment tenancy, occupancy and faction ownership; editor and player construction flows; 3D reachability, persistence round trips and deterministic snapshots at all six configured map sizes. Run independently of the large general gameplay suite as well as through Gate 3.

Until the reservation/stamping and real movement passes are verified, **multi-floor building generation is not claimed playable**.

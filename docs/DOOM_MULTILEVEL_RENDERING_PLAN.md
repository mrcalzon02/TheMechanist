# Doom viewpoint: multi-level rendering and vertical depth plan

Status: proposed implementation plan; no runtime changes included.

## Agreed generated-world vertical layout

Main atlas levels are large stacked volumes, not adjacent building storeys. From bottom to top, the repeating arrangement is:

1. The occupied main level's ground plane.
2. Approximately ten building-storeys of headspace, containing local building floors, balconies, and other structures.
3. The underside of the sewer structure belonging to the next main level above.
4. That next main level's sewer layer and enclosing structure.
5. The next main level's floor plate and ground surface, followed by its own headspace.

Building storeys within a main-level volume and main atlas levels are distinct concepts. The renderer must represent both explicitly; it must not turn each atlas floor into a single building-storey interval. The overhead surface normally visible from below is the upper level's sewer underside. Seeing into the sewer or through to the next main level requires an actual opening through the intervening structure.

Use a purpose-built chamber to validate this arrangement before integrating generated terrain: a lower ground plane, a ten-storey headspace with intermediate balconies, an overhead sewer structure, and an upper main floor plate. Include closed structural sections and deliberate inspection openings so both occlusion and vertical connectivity can be verified. Exact storey height, sewer clearance, and slab thickness remain implementation parameters; the agreed ordering does not depend on those dimensions.

## Intended result

From a balcony, the player can look down through an atrium and see the actual lower floor, its objects, and visible actors. From below, they can look up at balconies, upper walls, and ceilings. Solid floor slabs block sight and targeting. Several levels can appear in the same frame. Looking does not change the player's occupied level; stairs and elevators retain authoritative travel rules.

## Current implementation and gaps

- `src/mechanist/FirstPerson3DFramework.java`: `rasterizeWorld` casts one horizontal ray per screen column into `panel.world`, then draws one fixed-height wall stripe. This cannot represent stacked surfaces or openings above and below a wall.
- `rasterizeFloorAndCeiling` paints a floor and ceiling across the viewport using fixed local heights. It does not omit slabs for open volumes or sample another floor.
- `FirstPersonCamera` already receives mouse pitch, but the world renderer approximates pitch by shifting the horizon. Sprite projection has a separate transform. All geometry needs one consistent camera basis and projection.
- Camera coordinates currently use Y for vertical height and Z for the map's second horizontal axis. Gameplay Z levels must be mapped explicitly to renderer height to avoid axis confusion.
- `ViewportRaycaster` and `RaycastHit` describe horizontal tile hits; targeting does not intersect floor slabs or actor height bounds.
- Billboards draw with Java2D after the world image; they do not share a per-pixel depth buffer with walls and floors.
- `WorldAtlas` already holds floor/sewer slices, loaded lazily. `MultiFloorBuildingPlacementAuthority`, `VerticalBuildingBlueprintAuthority`, and `StructuredZoneGridRuntime` already describe aligned buildings and open volumes. These should supply geometry rather than creating a separate procedural building system.
- `MultiFloorBuildingTransitAuthority` already validates matching building landings. Visual connectivity must not bypass those checks.

## Architecture decision

Keep the Java2D presentation path, but introduce a scene adapter and a software triangle rasterizer with a per-pixel depth buffer. Generate only exposed tile faces and floor/ceiling slabs. This directly supports stacked rooms, balcony undersides, holes, and true pitch; extending the current single-hit column renderer would require substantial special cases.

Keep scene geometry, camera transforms, materials, and 3D intersection queries independent of the backend. A later OpenGL backend can consume the same scene. Benchmark the software path early before committing to its final draw distance or default internal resolution.

## Ordered milestones

### 1. Define coordinates, heights, and a deterministic fixture

- Define gameplay coordinates as `(tileX, tileY, levelId)` and render coordinates as `(x, height, z)` with explicit conversion helpers.
- Give every main floor/sewer level a physical elevation and thickness using the agreed arrangement above. Main-level ground planes are separated by ten building-storeys of headspace plus the intervening sewer structure and slabs. Do not treat the atlas storage index as physical elevation: floor and sewer ordering needs an explicit mapping consistent with existing travel semantics.
- Distinguish main atlas level identity from local building-storey identity in geometry, scene queries, and target ownership. Audit existing multi-floor blueprints and transit semantics before deciding how they map into these volumes; their current floor indices must not silently define the new physical spacing.
- Use stable slice identity containing sector, zone, floor, and sewer state. Add level identity, impact height, and 3D normals to the new hit contract.
- Define floor, ceiling, slab, opening, and solid-wall semantics from structured tiles and stamped building geometry. Audit how open-to-below areas connect to the underside of the next plate; do not cut ceilings globally.
- Create the purpose-built chamber described above with a balcony, atrium, closed room, aligned shaft, and actors at overlapping horizontal coordinates on different building storeys. Include the overhead sewer and next main floor plate rather than testing only three tightly stacked slabs.

Acceptance: floor elevations, opening alignment, and slice transforms are deterministic; solid versus open geometry is explicit.

### 2. Provide a bounded multi-level scene

- Add a proposed `DoomSceneProvider` that reads the current slice and reachable vertical neighbors without changing `atlas.floor`, `atlas.sewer`, or `panel.world`.
- Traverse visibility openings within a configurable level and distance budget. Start with current level plus one above and below; support deeper connected shafts within a strict cap.
- Validate that actual stamped building sites and opening bounds match across slices, including differing slice dimensions. Blueprint intent alone must not make an unbuilt floor visible.
- Request missing slices outside the render loop using the existing world's ownership/threading rules. Publish immutable render snapshots; avoid concurrent mutation of live `World` collections and generation globals.
- Use bounded caches keyed by slice identity and geometry/material revisions. Invalidate for doors, construction, destruction, and slice replacement.
- Until a requested slice is available, show an explicit occluding loading boundary rather than exposing unknown space. Do not advance simulation or exploration merely by preparing a render snapshot.

Acceptance: looking into an atrium loads only the bounded scene and never changes the active gameplay slice or stalls painting with generation work.

### 3. Implement true 3D projection and depth

- Share one yaw/pitch camera basis, FOV convention, aspect ratio, near plane, and projection between geometry, sprites, effects, and screen-to-world rays.
- Set eye height from occupied-level elevation plus stance and visual offsets. Remove hard-coded floor/ceiling eye-height assumptions.
- Extract exposed wall faces, floor tops, slab undersides, ceilings, balcony edges, and door geometry. Omit surfaces only where an explicit opening exists.
- Clip triangles at the near plane, interpolate textures correctly, and depth-test all opaque geometry into reusable color/depth buffers.
- Allow sustained upward/downward looking. Make the existing automatic pitch return optional, with explicit recenter input. Choose and test a near-vertical pitch limit without singularities.
- Keep HUD drawing in screen space after the scene.

Acceptance: the fixture shows multiple levels simultaneously; slabs and balcony edges occlude correctly while looking steeply up/down, crouching, and changing FOV.

### 4. Integrate actors, effects, lighting, and visibility

- Place actors and objects at their owning level's elevation; give them physical height/width bounds.
- Render billboards and projectiles with the shared camera and depth buffer. Transparent texels must not occlude the scene; handle translucent effects in a separate ordered pass.
- Read light data from each surface's owning slice. Extend radial fog to full 3D distance and linear fog to camera-space depth consistently across surfaces and sprites.
- Add cross-level visibility queries through actual openings. Keep geometric occlusion, gameplay visibility, and remembered exploration separate; current-slice 2D visibility is insufficient for another floor.
- Do not reveal hidden actors from unloaded or occluded rooms. Define whether cross-level looking updates exploration before enabling that behavior.

Acceptance: an actor below a balcony is partly hidden by its edge, disappears behind a slab, and receives the correct level's lighting and fog.

### 5. Align targeting and travel with the view

- Cast the reticle ray in 3D against the same surfaces and actor/object bounds used for rendering; return the nearest valid intersection and owning level.
- Carry level identity through look/use/weapon dispatch. Existing tile-only actions must never silently resolve to the current floor when the visible target belongs to another one.
- Support cross-level inspection first. Preserve existing interaction distance, line-of-sight, weapon, and authority checks; enable cross-level use/fire only when those systems explicitly support it. Otherwise show a clear unavailable action.
- Preserve stair/elevator landing validation and synchronize camera elevation and continuous movement state after a successful transition, including transitions at identical X/Y coordinates.
- Show the occupied level and target's relative level when useful. Looking down does not move the player or enable free flight.

Acceptance: a slab blocks selection and shots; overlapping actors on different floors resolve correctly; stair/elevator travel keeps occupancy, camera, and active slice synchronized.

### 6. Validate performance and release incrementally

- Run geometry, projection, intersection, and scene-provider tests on the deterministic fixture, including missing neighbors, world boundaries, negative coordinates, and near-plane crossings.
- Capture fixed-camera reference frames at multiple FOVs, aspect ratios, and pitches; manually review texture alignment, openings, slab undersides, sprite clipping, and target alignment.
- Re-run existing Doom camera/HUD/facing and multi-floor placement/transit smoke checks. Confirm ordinary 2D rendering and save/load still work.
- Measure frame-time distributions, generation stalls, allocations, cache memory, and EDT responsiveness in both the fixture and dense generated buildings. Establish a baseline and an agreed target on supported hardware before setting defaults.
- Optimize exposed-face merging, spatial culling, cached geometry, bounded loading, and optional internal-resolution scaling based on measurements.
- Ship behind an experimental option with the current renderer available for comparison. Broaden visible depth only after correctness and performance gates pass.

Acceptance: the complete atrium scenario passes visual, targeting, travel, regression, and measured performance checks.

## First deliverable and scope boundaries

Build milestones 1-3 first as a reviewable vertical slice: the purpose-built chamber with ten-storey headspace, intermediate building storeys, the upper sewer underside and enclosure, and the next main floor plate; correct up/down projection, real openings, and reliable depth occlusion. That proves the central rendering requirement before adding live cross-level actors and actions.

The initial feature does not require free flight, jumping/falling physics, continuous stair simulation, cross-level NPC pathfinding, a save-format rewrite, or an immediate OpenGL migration. Any cross-level combat or exploration rule changes need explicit implementation and validation in their owning systems.

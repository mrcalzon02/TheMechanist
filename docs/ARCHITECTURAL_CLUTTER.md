# Vertical and underside architectural clutter

Implemented 2026-11-02 in the existing The Mechanist World, tile system, planner and first-person viewport. This pass adds physical surface relief to long walls and the undersides of slabs, using original procedural geometry and the shipped wall/closed-door textures. No replacement renderer, generated raster asset pack or separate demonstration world is involved.

## Architectural language

The governing idea is accumulated infrastructure: older structures survive beneath later repairs and new service routes. Continuous runs establish useful infrastructure first; localized patches, old thresholds, clamps and differing finishes suggest subsequent interventions. The four material-generation tags are visual history cues, not a simulation of thousands of years of individual construction events.

| Feature | Current geometry and purpose |
| --- | --- |
| RISER | Round vertical service pipe, periodically clamped to the wall |
| SERVICE_PIPE | Round horizontal service bands, joined across adjacent wall tiles |
| CABLE | Thin round runs above later pipe bands |
| SUPPORT | Wall clamps, vent ribs and transverse underside stiffeners |
| SEALED_DOOR | Closed-door artwork recessed beneath a repair strap on an already solid wall |
| PATCH | Differing metal generations over former openings and upper wall bands |
| VENT | Raised service panel with physical grille ribs |
| LAMP | Emissive retrofit strip in a raised protective housing |
| DUCT | Suspended rectangular trunk under an opaque slab |
| HANGER | Supporting yokes and vertical drops linking ducts to the slab |
| SWAG | Thin round cable segments with a deterministic sag profile |

Risers share a six-tile rhythm along a wall line, with lower service runs at 0.24 local height and later bands around 1.25, 3.45 and 6.65. Wall height limits clip the recipe to the actual structure. Sealed openings, vents and patches occupy deterministic segments, with additional repairs on upper bands. Old brown iron, oxidized green metal, later blue-grey work and yellowed repair hardware vary the treatment of native textures. Cylindrical pipes and wires have curved silhouettes and view-facing shading; boxes remain appropriate for plates, ducts and brackets.

## Placement and clearance

Geometry occupies the existing spatial tile cells beside a solid wall or below an existing opaque slab. It does not replace the terrain glyph, create an actor/furniture item, or change room ownership. Wall detail protrudes at most 0.19 tiles into the aisle; the existing continuous player's wall clearance is larger. Hanging services preserve at least 1.12 local height above the floor, clearing the existing 1.1-high character billboards. Deep hanging assemblies flatten against low roofs; roofs at or below 1.12 do not receive them.

Glass and grating roofs do not acquire opaque underside assemblies. Open spaces may receive services attached to the next main level's underside at height 10; this does not mark the road or park as locally roofed. VOID cells, stair-art cells, doors, wall cells and other non-walkable fixture cells do not host the recipe. The current recipe can terminate at those cells; routing ducts around fixtures or through corners is follow-up work.

Sealed-door relief attaches to an already solid wall and never creates a new passage or usable door. Camera target labels identify the hit feature. The geometry participates in the shared visual/physical ray path and depth buffer; pipes and supports can occlude a line of fire. Ground movement still uses the existing tile model, which this clutter pass does not alter. There is no new salvage, use, repair, demolition or construction-cost interaction for these derived decorations yet.

## Generation, saved data and cache

`ArchitecturalClutterAuthority` owns recipe version 1. New normal zone generation enables it after room roofs and open-road defaults are authored. `CityArchitectureCaptureProbe` uses the same enable path after its ordinary structural-generation stages. Explicit developer test worlds remain opt-in so other battle/material tests retain their original fixtures.

`World.architecturalClutterVersion` is saved as `world.architecturalClutter.version`. Zero disables the recipe; one selects this implementation. An absent property retains the world's generated setting. Unsupported or malformed versions disable clutter and log a warning. The seed and existing terrain/structure metadata reproduce placement; no frame-dependent random state or wall-clock input is used. The recipe has no dependency on the order cells are viewed.

`Piece` records contain feature, world-space bounding extents, tint, native art glyph and visual generation. They are derived lazily per cell, cached on the World, and invalidated by the existing vision revision when geometry changes. Direct editing code must mark that revision dirty. The archive exports the actual chamber pieces as TSV, including absolute elevations. The renderer uses the cache in its existing DDA traversal; it does not raycast every piece in the entire world.

Lights use the existing `ZoneLightSourceRecord` system, with deterministic identifiers beginning `CLUTTER-LIGHT-`, radius 3 and intensity 48. Synchronization and reset remove duplicates and obsolete generated records. A maximum of 96 diffuse clutter lights per world bounds lighting cost; further lamp meshes remain emissive. This is a visual service-light recipe, without a new power-network simulation. Host changes invalidate meshes immediately; the diffuse-light inventory is refreshed on enable/load and can be explicitly refreshed with `synchronizeLights` after live host edits.

The normal 2D planner remains the occupied-level view, with small overhead-service and lamp indicators. The #001a33 minimum scene exposure, existing fog settings, native artwork and full mouse look remain active. Renderer/network snapshots do not serialize individual decorative meshes; a future remote client must share the recipe/version and host geometry to reconstruct them.

## Developer inspection and evidence

Use `/devroom battlemap clutter` in the existing developer facility, or the **layered wall and ceiling services** control. It retains the three-height combat map and gives the north aisle a 1.6-clearance service canopy, beside open space from which the upper wall can be inspected. Existing `battlemap multi`, `materials` and `flat` reset the clutter recipe and its derived lights before building their fixtures.

The [archival gallery](../diagnostics/architecture-clutter-gallery-20261102/README.md) contains seven facility frames, including a matching before/after aisle pose, plus nine generated-city frames. The archive includes camera poses, seed/version, physical piece inventory, image hashes, a generation audit and validation notes. Elevated camera poses are labeled inspection views; they do not prove traversable local stacked storeys. Captures are headless GamePanel renders rather than native mouse-input playtesting.

The city uses seed 466713, main floor 4, 500×500 tiles, with 930 rooms and three planned building sites. Its audit still records 33,997 open road/sidewalk/parking tiles and zero accidentally covered road tiles. Population and economy simulation are omitted from architectural captures. Existing world-generation validation warnings about weak doors, spacing and squat corridors remain; this pass does not claim to fix them.

Full Java 17 compilation passed. Validation covers all eleven feature families, all four visual generations, exact seeded replay, connected pipe joins, geometric depth, unchanged ground passability, glass aperture preservation, low-roof head clearance, host-removal invalidation, persistence, bounded/deduplicated lights and developer reset. Existing material, roofing, combat, movement and fog checks also passed. Concurrent open-door art edits were excluded from the committed renderer hunks and isolated compilation used the committed renderer versions for the archive.

## Next architectural layers

- Former floor and roof lines: truncated lintels, bricked windows, cut beam ends, and stair landings that now face a wall. Tie these to actual future local-storey geometry.
- Abandoned services: capped tees, disconnected valves, ceramic insulators, obsolete meter cupboards, pressure gauges and redundant cable trays beside newer bypasses.
- Structural survival: sistered beams, bolted gussets, angle braces, patched expansion joints and visible load-transfer collars where later buildings bear on older fabric.
- Human occupation: faded address bands, ghost lettering beneath newer signs, old inspection marks, memorial plates, improvised shrines and reused utility niches. These warrant small original art assets and readable distance-aware treatment.
- Water and maintenance history: mineral streaks below leaking joints, layered soot, differing corrosion near drains, and newer clean patches around frequently serviced valves.
- Service networks: route turns, junctions and fixtures coherently; add district/building-specific palettes and density budgets, then profile normal gameplay before increasing mesh density.

These follow-ups should add evidence of use and reconstruction while preserving silhouettes, sight openings, useful navigation and the established native art.

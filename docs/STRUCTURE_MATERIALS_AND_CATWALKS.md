# Structure materials and catwalks

Implemented 2026-11-02 on the existing The Mechanist World, GamePanel, tile art, planner and viewport. The latest `origin/main` was merged into `viewport/roofed-city-20261029` before this work. No separate rendering environment was added.

## Authoritative tile data

`TileStructureAuthority.Structure` keeps these independent fields:

| Field | Values / meaning |
| --- | --- |
| roof | OPEN, COVERED, GLASS, GRATING |
| roofClearance | Positive local distance from the tile's ground surface to the underside |
| wallTopElevation | Absolute top of the wall; generic walls retain the 10-storey default |
| explicitRoof | Whether authored coverage overrides generated defaults |
| wall | SOLID, GLASS, GRATING, PILLAR, PIPE |
| finish | PLAIN, STAINED, PANELS, LIT |
| floor | SOLID, GRATING, VOID |
| railings | Edge mask: north=1, east=2, south=4, west=8; combinations 0..15 |

Roof edits preserve wall, finish, floor and railing data. `withWall` and `withFloor` author the other independent properties. Structured grid synchronization, world save/load and immutable tile snapshots carry the material and railing state. Structure edits invalidate vision and lighting caches.

The `world.tileStructures.v1` property contains semicolon-separated records:

```text
x,y,roof,roofClearance,wallTopElevation,explicitRoof,wall,finish,floor,railings;
```

Existing five- and six-field records remain readable, with SOLID/PLAIN/SOLID/0 material defaults. A missing property retains generated metadata; an empty property clears saved overrides. Invalid or truncated records are refused. Existing three- and four-argument Structure constructors and older TileSnapshot constructors retain their defaults. The property is an extended format: older game executables do not understand the new material semantics.

## Visibility and rendering

`StructureSurfaceAuthority` supplies the existing viewport and physical firing-line geometry. Glass surfaces are tinted translucent layers. The renderer composites them after native character billboards using depth, so a character in front of glass is not tinted by glass behind them. Up to eight glass layers are composited per sampled ray; additional glass layers are skipped optically rather than becoming opaque. Internal tile boundaries in continuous glass canopies do not create extra glass faces. The distant next-level underside remains opaque concrete even above a glass roof.

Grating uses actual openings in ray geometry, with eight divisions per tile and bar coverage of 18% in each direction. Apertures reveal geometry behind or below; bars occlude it. Pillars use a centered square column 0.46 tiles wide; pipes use a centered round column 0.22 tiles across. Both retain full-cell movement blocking in the existing tile-based actor model. Railings use narrow posts and two horizontal bars, reaching 0.88 local storeys high. Views pass between the bars; the bars themselves remain visible geometry.

Finishes modify the game's shipped wall textures: deterministic discoloration, panel seams and emissive light strips. LIT tiles register a real blue `ZoneLightSourceRecord` with radius 4 and intensity 65. Editing/removing lit finishes and resetting the material bay remove their derived lights. Glass and grating roofs admit diffuse ambient baselines of 22 and 18 respectively, compared with 28 for open space. The #001a33 scene darkness floor and softer 12..96 distance fog remain in place.

The normal 2D planner shows material indicators and railing edges over the native tile artwork. Its existing cell-based sight model treats glass, grating, pillars and pipes as admitting sight; first-person rays resolve their exact silhouettes. Visual transparency does not make intact glass passable or grant a clear firing line through it. No glass shattering, penetration or ballistic material simulation is claimed.

## Movement and fall prevention

Grating floor supports actors even when the ray passes through a visible hole. VOID floor provides no support and cannot be entered. A railing belongs to an edge, rather than making its entire tile impassable. Crossing that edge fails from either direction, including diagonal corner attempts. Open edges at bridge ends remain usable.

Checks apply to normal player movement, movement planning, continuous viewport movement, NPC steps, and the structured push/squeeze resolver. Crowd displacement cannot push an actor across a railing or into an unsupported tile. Existing terrace height/quarter-storey stair limits remain authoritative. This iteration prevents accidental unsupported steps; falling, jumping, deliberate vaulting and fall damage remain future mechanics.

## Paid roof construction

All three recipes use normal placement, staged components and labor in **Shelter and Storage**. Each needs a supporting wall within three cardinal tiles. Duplicate roofs and unsupported placements fail before payment. Coverage applies after construction completes; the ground and furniture slot remain available.

| Recipe | Supplies | Machine parts | Labor turns | Named components per tile |
| --- | --- | --- | --- | --- |
| Roof Panel | 3 | 1 | 6 | Scrap plate x2; Reinforced fastener set x1; Sealing gasket set x1 |
| Glass Roof Panel | 4 | 1 | 7 | Structural glazing pane x1; Reinforced fastener set x1; Sealing gasket set x2 |
| Grating Roof Panel | 3 | 1 | 6 | Metal grating panel x1; Reinforced fastener set x2 |

Structural glazing pane and Metal grating panel are registered construction components. Their catalog values are 16 and 10. Listed source descriptions are catalog metadata, not a claim that new dedicated manufacturing chains or guaranteed merchant stock were added.

## Existing developer chamber

Enter the existing developer facility, then use `/devroom battlemap materials` or its material-bay control. This builds the existing three-height battle map and adds glass/grating canopies on the west terrace, wall forms/finishes to the north, and a guarded grating catwalk over unsupported tiles to the east. `/devroom battlemap multi` restores the ordinary terrace layout; `/devroom battlemap flat` restores the original tiles/structures and removes sample wall lights. Normal mode remains the 2D planner; the existing viewport retains full mouse look.

Seven native GamePanel snapshots and inspection poses are archived in [the material gallery](../diagnostics/structure-materials-gallery-20261102/README.md). These are labeled inspection cameras, including elevated viewpoints. They are not evidence of traversable room-over-room storeys or live mouse-input playtesting.

## Validation and next work

Full Java 17 compilation and the checks listed in the gallery validation record passed. `StructureMaterialsSmoke` verifies glass layering, opaque upper undersides, grating apertures/bars, pillar/pipe silhouettes, rail crossings in both directions, diagonal rejection, unsupported floors, crowd safety, persistence, snapshots, actual paid roof variants and player/continuous movement integration. Existing movement, combat, roofing, fog and multilevel battle tests passed as well.

Materials and catwalks are authored capabilities plus a developer fixture in this iteration. Broad procedural city placement, height-dependent facade bands, separate local stacked-storey occupancy, normal paid catwalk/railing construction tools, material destruction and fall mechanics remain follow-up work. New wall art atlases and material-specific texture mapping can replace these deterministic treatments later while retaining the authoritative data and collision model.

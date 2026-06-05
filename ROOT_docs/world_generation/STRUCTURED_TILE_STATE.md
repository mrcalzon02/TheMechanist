# Structured Tile State Foundation

The Mechanist world generator must no longer treat maps as only `char[][]` text glyphs.

Legacy glyph maps remain useful as a compatibility/export layer for current rendering and debugging, but generation needs a structured tile model so later systems can reason about room ownership, roads, corridors, transitions, lighting, placed objects, containers, vehicles, pets, and entity occupancy.

## Implemented foundation

### `ZoneTileState`

`src/mechanist/ZoneTileState.java`

Represents one tile in a generated zone.

It currently tracks:

- base tile type
- space type
- legacy glyph
- room ID
- corridor ID
- road network ID
- transition ID
- vertical transition ID
- owning faction
- reservation label
- occupant entity ID
- pet entity ID
- vehicle ID
- tile flags
- placed objects
- lights

Important enums:

```java
ZoneTileState.BaseTileType
ZoneTileState.SpaceType
ZoneTileState.TileFlag
ZoneTileState.LightKind
```

Important nested records/classes:

```java
ZoneTileState.PlacedObjectRef
ZoneTileState.LightState
```

### `ZoneTileGrid`

`src/mechanist/ZoneTileGrid.java`

Represents a complete structured tile grid for a zone.

It provides:

- grid creation by width/height
- import from legacy `char[][]`
- export back to legacy `char[][]`
- room marking
- corridor marking
- road marking
- central plaza marking
- transition marking
- reservation marking
- object placement
- light placement
- occupant/pet/vehicle tracking
- placement-blocking checks
- conversion to `ZonePlacementValidator`

## Compatibility rule

The structured grid does not replace legacy rendering immediately.

The intended migration path is:

```text
legacy char[][] generation
  -> ZoneTileGrid.fromLegacyTiles(...)
  -> structured generator passes modify ZoneTileState
  -> ZoneTileGrid.toLegacyGlyphs() for current render compatibility
```

Later, the richer renderer/simulation can read directly from `ZoneTileGrid`.

## Placement safety

A tile can block room placement through:

- `TileFlag.BLOCKS_ROOM_PLACEMENT`
- `TileFlag.RESERVED`
- `SpaceType.ROAD_NETWORK`
- `SpaceType.CENTRAL_PLAZA`
- `SpaceType.TRANSITION_ROOM`
- `SpaceType.SEWER_NETWORK`

This lets room generation reject candidates that overlap roads, corridors, plazas, transition rooms, sewer corridors, parking, or other reserved infrastructure.

## Required future wiring

The next generator pass should attach this foundation to actual zone generation:

1. Create `ZoneTileGrid` immediately after the legacy or scaffold tile map is created.
2. Mark central plaza and road corridors in the grid.
3. Mark sewer corridors as `SpaceType.SEWER_NETWORK` instead of road infrastructure.
4. Mark transition rooms and vertical transition anchors.
5. Use `ZoneTileGrid.blocksRoomPlacement(...)` before placing rooms.
6. Use `ZoneTileGrid.toPlacementValidator()` to feed legacy placement code.
7. Place faction/wealth housing objects and containers through `addObject(...)`.
8. Place street lights and room lights through `addLight(...)`.
9. Export to legacy glyphs only after structured passes finish.

## Design notes

The grid is deliberately package-private and lightweight so it can be introduced without changing the public API or rendering architecture.

This pass is a foundation only. It does not yet rewrite room generation, road generation, sewer generation, or rendering.

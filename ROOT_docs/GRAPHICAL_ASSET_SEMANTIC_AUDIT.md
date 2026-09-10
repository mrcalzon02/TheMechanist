# Graphical Asset Semantic Audit

## Scope and authority

This audit covers the compiled, simulation-selectable graphical corpus governed by
`ROOT_tools/atlas_asset_pipeline`. The corpus contains 2,898 unique atlas cells from
120 source atlases, compiled at five resolutions. The checked-in 32px and 256px
semantic indexes are generated outputs; `configs/deep_asset_descriptors.json` is the
human-authored semantic authority.

The full-screen presentation art and application icon families are intentionally
outside the world-asset registry:

- `Background/Backdrop.png`, `Background/CLOUDS1slow.png`, and
  `Background/Clouds2fast.png` are presentation layers loaded by
  `MediaRuntimeFramework`.
- `Title/TITLE.png` and `Title/Subtitle.png` are main-menu presentation art loaded by
  `MediaRuntimeFramework`.
- The packaged application icon sizes and ICO are governed by `AppIconAuthority`, the
  launcher icon authority, and packaging rules.

These assets remain indexed by the repository and source-art synchronization
manifests, but they must not be offered to world generation, construction, loot,
population, or simulation render selection.

## Visual inspection

Contact-sheet inspection covered every compiled cell across the 17 graphical groups:
BULKHEAD, Corridors, Defenses, Doors, Emergency machines, Faction, floors, Implants,
ITEMS, Knowledges, Objects, Protraits, Roads, SYSTEM, Vehicles, VOID tiles, and Walls.

The broad visual families agree with their group taxonomy. The following mixed or
exceptional sheets required narrower rules:

- `Bulkhead_walls+doors` and `Bulkhead_walls+doors-open`: fourteen cells are explicit
  closed/open door fixtures; the other thirty-six remain structural wall tiles.
- `Doors-C` and `Doors-O`: the whole sheets are explicit closed/open access fixtures.
- `Road_doors`: fourteen cells are vehicle-navigable road surfaces and eleven are
  gates, shutters, platforms, barriers, vents, or other access fixtures.
- `Stairs_stairs_elevator_platforms_hatches_and_ladders`: all cells are vertical
  pathfinding-transition fixtures.
- `Faction_2`: eleven cells are active faction emblems and fourteen visually blank
  cells are reserved `internal` placeholders that must not enter runtime selection.
- Weapons and mixed weapon/armor sheets retain cell-level descriptions where visual
  meaning differs inside a single atlas.

## Semantic coverage after regeneration

- 2,898 / 2,898 cells have an explicit nonblank `content_type`.
- 2,898 / 2,898 cells have a nonblank semantic description.
- 2,898 / 2,898 cells are sourced from a group, atlas, row, or cell rule; none depend
  on the old derived-only descriptor fallback.
- Asset IDs are unique; every packaged 32px runtime path and every authoritative
  pipeline 256px review path exists.
- Twenty-seven authoring content types collapse into fourteen supported runtime
  `AssetType` classes.

The authoring taxonomy now distinguishes walls, floors, corridors, roads, fixtures,
machines, vehicles, portraits, interface icons, items, implants, documents, wearables,
weapons, trade goods, industrial components, data devices, corpse/decay art, props,
and protected internal placeholders. Family tags also record intended systems such as
world generation, construction, pathfinding, strategic transit, inventory, economy,
population, faction identity, and UI-only use.

## Simulation integration boundary

`AssetRegistry` now treats explicit index types as authoritative before folder-name
fallbacks. This is essential for mixed atlases such as `Road_doors`, whose road cells
must resolve as `ROAD_TILE` while neighboring access hardware resolves as `FIXTURE`.
Descriptor tags are retained in runtime semantic text so render-intent matching can
use the generated metadata instead of losing it during TSV loading.

`SemanticRenderAssetResolver` keeps strict type boundaries:

- only explicit machine art can satisfy machine intents;
- only explicit fixtures can satisfy infrastructure-fixture intents;
- only explicit weapon or armor types can satisfy their inventory intents;
- data-device compatibility accepts correctly described legacy UI assets while
  excluding system controls;
- trade goods may be inventory icons or world cargo objects, while junk/debris is
  excluded;
- market surfaces require a true `market` or `bazaar` semantic rather than a partial
  word or generic commercial label.

The end-to-end `DeepSemanticAssetCoverageSmoke` loads the checked-in 32px package
index, audits all 2,898 paths and runtime types, verifies exceptional atlas counts,
and requires representative door, equipment, trade, data-device, shrine, and market
render intents to resolve successfully.

## Safe expansion rule

New simulation features should request an `AssetType` plus semantic intent through
the registry/resolver boundary. They must not select by raw filename, folder, or
substring alone. When an atlas contains multiple simulation roles, add exact cell or
row rules to `deep_asset_descriptors.json`, regenerate both governed package indexes,
and extend the coverage smoke with the new invariant.

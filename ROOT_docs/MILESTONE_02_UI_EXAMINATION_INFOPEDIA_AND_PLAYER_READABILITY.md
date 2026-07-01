# Milestone 02 - UI, Examination, Infopedia, Player Movement, Controls, and Player Readability

[Existing content retained]

## Phase 4.16 - Semantic Rendering Migration and Asset Binding Authority

This phase is now an active Milestone 02 ownership lane.

The project already possesses semantic asset registries, asset metadata records, compiled asset indexes, semantic asset IDs, atlas pipelines, and asset categorization systems. The current problem is not asset availability. The current problem is that some renderer paths still bypass semantic identity and instead use convenience fallbacks, generic tile selection, placeholder icons, debug presentation assets, or first-match category lookups.

The renderer must progressively migrate from path-based or category-only rendering toward semantic rendering.

Current checkpoint: live tile rendering prefers semantic asset IDs, tile aliases resolve only to entries in the active runtime registry, and representative generic, industrial, sewer, noble, road, door, and streetlight families are guarded by aggregate smoke coverage. Dedicated open and closed door fixture atlases retain their state through compiled-index loading and are selected ahead of mixed wall sheets. Road-infrastructure streetlight cells retain explicit fixture metadata and cannot resolve through system inventory or item/UI icons.

District and room context now enters live floor compilation through the existing `World.zoneType`, room-faction, sewer-layer, and floor-height state. `TileDataCompilationAuthority` distinguishes habitation, market, security, industrial, administrative, transit, religious, noble, rough, sewer, and generic floor families before the renderer receives a `CompiledTileDescriptor`. Context-specific floor aliases resolve through the active tile registry first and then through the strict semantic resolver; recognized missing floor families carry `MISSING-SEMANTIC-TILE` rather than silently degrading to an unrelated generic floor. `Milestone02DistrictRoomTileContextSmoke` guards both context classification and the live descriptor boundary.

The live inventory and world-object render bridges preserve valid authored asset identities first, then classify unresolved or generic labels through strict semantic render families. `ItemSemanticAssetAuthority` covers weapon, armor, tool, medical, drug, food, industrial-component, trade-good, religious-object, and data-device families. `ObjectSemanticAssetAuthority` covers doors, typed containers, purpose-specific furniture, streetlights, traffic lights, generators, transformers, junction boxes, ventilation units, water pipes, sewer pipes, security cameras, and refrigerated storage for build recipes, base objects, map objects, lights, and editor previews.

Recognized item and object families now fail closed. When no compatible indexed family asset exists, the live bridge carries an explicit unknown semantic asset ID into `AssetManager`, which produces typed missing art instead of allowing a later broad lookup to choose an unrelated crate, UI icon, wall, fixture, or item. `Milestone02SemanticRuntimeIntentBridgeSmoke` guards classification, positive family resolution, cross-theme rejection, typed-missing fallback identifiers, and the rule that unknown labels do not invent a family.

Remaining Phase 4.16 work is narrowed to deeper room-purpose semantics beyond existing zone/faction context, wall and fixture selection that consumes the same context, additional genuinely distinct infrastructure families discovered in the asset registry, and unclassified world-object paths. It is no longer accurate to describe district floors, infrastructure, containers, furniture, or item previews as wholly disconnected from the live semantic registry.

The renderer should never ask for:

- floor tile
- wall tile
- door
- light
- container
- item icon

Instead it should ask for semantic intent.

Examples:

- sewer floor tile
- sewer wall tile
- habitation floor tile
- industrial floor tile
- market floor tile
- closed door tile
- open door tile
- streetlight fixture
- medical cabinet
- weapons locker
- cargo crate

Asset resolution should occur through semantic registry lookups and approved semantic render authorities.

### Phase 4.16.1 - Tile Theme Authority

All tile families become semantic rendering groups.

Examples:

- Sewer
- Utility Tunnel
- Maintenance Corridor
- Habitation
- Slum
- Market
- Industrial
- Manufactorum
- Noble District
- Medical
- Security
- Administrative
- Religious
- Transit
- Warehouse
- Exterior Street
- Exterior Alley
- Exterior Plaza
- Exterior Ruins

Rules:

- Sewer tiles remain sewer tiles.
- Generic tiles remain generic tiles.
- Specialized district tile families are not interchangeable.
- Renderer fallbacks may fail to missing-art states but may not silently substitute unrelated themes.

Current bridge status:

- Existing zone and faction state selects habitation, market, security, industrial, administrative, transit, religious, noble, rough, sewer, or generic floor families.
- Every recognized contextual floor family maps to a strict `SemanticRenderAssetResolver` floor intent.
- Missing contextual floor art publishes typed missing-tile identity rather than unrelated floor art.

Exit criteria:

The renderer can no longer place sewer floors inside ordinary habitation rooms because a generic floor request happened to resolve first.

### Phase 4.16.2 - Room Theme Authority

Rooms become semantic rendering owners.

Examples:

- Apartment
- Barracks
- Hospital Ward
- Operating Theater
- Shrine
- Workshop
- Generator Room
- Storage Room
- Market Stall
- Tavern
- Office
- Security Checkpoint
- Prison Cell
- Morgue

Rooms should influence floor, wall, fixture, furniture, and decoration selection.

Current bridge status:

- Room faction and surrounding zone context now influence compiled floor families.
- Specialized faction identity overrides a generic civilian-zone fallback.
- Explicit sewer, noble, rough, market, security, industrial, administrative, and transit district contexts retain priority over generic habitation.
- Detailed authored room-purpose records remain future work for walls, fixtures, furniture variation, and decorations.

Exit criteria:

Room rendering visibly reflects room function rather than generic fallback tiles.

### Phase 4.16.3 - Infrastructure Authority

Infrastructure objects must resolve through infrastructure art.

Examples:

- Streetlight
- Traffic Light
- Generator
- Transformer
- Junction Box
- Ventilation Unit
- Water Pipe
- Sewer Pipe
- Security Camera

Current bridge status:

- Every listed infrastructure family has a strict render intent and registry-backed resolver path.
- UI controls and system inventory icons are forbidden as infrastructure matches.
- Fresh-water pipes reject sewer, waste, and sludge semantics.
- Missing recognized infrastructure remains typed missing art instead of degrading into a generic object.

Exit criteria:

Infrastructure objects consistently use infrastructure-themed visual assets.

### Phase 4.16.4 - Door Authority

Doors must render through door semantic identities.

Required support:

- Closed door variants.
- Open door variants.
- Future locked, reinforced, blast, security, industrial, and faction-specific variants.

Exit criteria:

Door state directly determines rendered door art.

### Phase 4.16.5 - Furniture and Fixture Authority

Furniture becomes semantic.

Examples:

- Workshop Table
- Dining Table
- Medical Table
- Shrine Altar
- Market Counter
- Administrative Desk
- Interrogation Desk

Current bridge status:

- These purpose-specific furniture families have strict runtime intent classification.
- Valid authored identities remain higher priority than family fallback.
- Recognized furniture with missing family art displays typed missing art.
- Generic or unknown furniture labels remain unresolved rather than being silently forced into an unrelated purpose.

Exit criteria:

Furniture art reflects purpose and room context.

### Phase 4.16.6 - Item Semantic Authority

Items must resolve through semantic item identities rather than generic icon buckets.

Examples:

- Weapon
- Armor
- Tool
- Medical Item
- Drug
- Food
- Industrial Component
- Trade Good
- Religious Object
- Data Device

Current bridge status:

- All listed item families have strict runtime intent classification.
- Existing authored and structured atlas identities remain first priority.
- Recognized families use strict resolver matching and carry a typed missing-item identifier when no compatible art exists.
- Broad semantic matching remains available only for text that does not claim one of the recognized families.

Future quality, provenance, condition, faction, and manufacturer visual variation should consume these semantic categories.

Exit criteria:

Item rendering is driven by semantic identity and provenance rather than arbitrary icon selection.

### Phase 4.16.7 - Container Authority

Containers become semantic rendering entities.

Examples:

- Toolbox
- Medical Cabinet
- Weapons Locker
- Wardrobe
- Cargo Container
- Filing Cabinet
- Refrigerated Storage

Current bridge status:

- Every listed container family has strict runtime intent classification.
- Valid authored identities remain first priority.
- Missing recognized container art produces a typed missing object rather than a generic crate or cabinet.

Exit criteria:

Containers are visually identifiable before interaction.

### Phase 4.16.8 - Zone and District Rendering Authority

Every rendered space should know:

- District Theme
- Room Theme
- Infrastructure Theme

Zone identity becomes a rendering input.

Examples:

- Market District Tavern
- Industrial Machine Shop
- Habitation Apartment Block
- Sewer Utility Tunnel

Current bridge status:

- Zone type is now a live input to floor-family compilation.
- Habitation, market, security, industrial, administrative, transit, noble, rough, and sewer districts compile different floor-family and art-key identities from the same ordinary floor glyph.
- Room faction supplements zone context for noble, security, industrial, administrative, religious, and habitation ownership.
- District wall, fixture, decoration, and detailed room-purpose context remain incomplete.

Exit criteria:

Districts become visually recognizable without requiring labels.

### Phase 4.16.9 - Semantic Render Resolver Migration

Renderer paths should progressively migrate to `SemanticRenderAssetResolver`, `SemanticRenderIntentAuthority`, and AssetRegistry-backed lookups.

Migration state:

1. Doors - live semantic state path established.
2. Streetlights and principal infrastructure families - live strict-family path established.
3. Sewer and generic tiles - live semantic tile path established.
4. District floor families - live zone/faction-aware compilation established; walls, fixtures, and decoration context remain.
5. Room floor families - live zone/faction-aware compilation established; detailed authored room-purpose context remains.
6. Containers - live strict-family bridge established, including refrigerated storage.
7. Furniture - live strict-family bridge established for named purpose families.
8. Items - live strict-family bridge established for principal player-facing families.
9. Remaining world objects - continue by meaningful runtime family, not audit-only layers.

Every migration should gain smoke coverage and semantic validation.

The Tools / Zone Audit surface now includes a curated Semantic Asset Audit Dev Room. Its cursor can manually cycle the selected tile through compatible indexed assets with `[` / `]`, Enter, or the Previous/Next Tile Asset buttons. These overrides are transient audit state: they immediately affect the rendered tile for visual verification without rewriting the semantic registry or production world-generation rules.

### Phase 4.16.10 - Generic Fallback Elimination

The long-term objective is removal of silent visual substitution.

Current bridge status:

- Recognized contextual floor, item, door, furniture, container, and infrastructure families fail closed to typed missing art.
- Unknown text may still use bounded legacy matching because it makes no stronger semantic claim.
- Cross-theme negative smokes reject sewer/generic tile swaps, UI-icon infrastructure, wall-as-door, unrelated item families, and sewer-contaminated water pipes.

Allowed:

- Explicit missing-art placeholders.
- Diagnostic overlays.
- Audit warnings.

Forbidden:

- Sewer assets replacing habitation assets.
- Generic icons replacing infrastructure assets.
- Wall assets replacing door assets.
- Menu actions silently redirecting to unrelated systems.

Exit criteria:

Visible world objects are rendered because of what they are and where they belong, not because they happened to be the first matching asset in a category.

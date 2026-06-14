# Milestone 02 - UI, Examination, Infopedia, Player Movement, Controls, and Player Readability

[Existing content retained]

## Phase 4.16 - Semantic Rendering Migration and Asset Binding Authority

This phase is now an active Milestone 02 ownership lane.

The project already possesses semantic asset registries, asset metadata records, compiled asset indexes, semantic asset IDs, atlas pipelines, and asset categorization systems. The current problem is not asset availability. The current problem is that some renderer paths still bypass semantic identity and instead use convenience fallbacks, generic tile selection, placeholder icons, debug presentation assets, or first-match category lookups.

The renderer must progressively migrate from path-based or category-only rendering toward semantic rendering.

Current checkpoint: live tile rendering prefers semantic asset IDs, tile aliases resolve only to entries in the active runtime registry, and representative generic, industrial, sewer, noble, road, door, and streetlight families are guarded by aggregate smoke coverage. Dedicated open and closed door fixture atlases retain their state through compiled-index loading and are selected ahead of mixed wall sheets. Road-infrastructure streetlight cells retain explicit fixture metadata and cannot resolve through system inventory or item/UI icons. Remaining infrastructure, district, room, fixture, furniture, container, item, and world-object paths continue through the ordered migration below.

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

Streetlights specifically must never render through system inventory icons.

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

Exit criteria:

Districts become visually recognizable without requiring labels.

### Phase 4.16.9 - Semantic Render Resolver Migration

Renderer paths should progressively migrate to SemanticRenderAssetResolver and AssetRegistry-backed lookups.

Migration order:

1. Doors.
2. Streetlights and infrastructure.
3. Sewer and generic tiles.
4. District tile families.
5. Room tile families.
6. Containers.
7. Furniture.
8. Items.
9. Remaining world objects.

Every migration should gain smoke coverage and semantic validation.

The Tools / Zone Audit surface now includes a curated Semantic Asset Audit Dev Room. Its cursor can manually cycle the selected tile through compatible indexed assets with `[` / `]`, Enter, or the Previous/Next Tile Asset buttons. These overrides are transient audit state: they immediately affect the rendered tile for visual verification without rewriting the semantic registry or production world-generation rules.

### Phase 4.16.10 - Generic Fallback Elimination

The long-term objective is removal of silent visual substitution.

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

> [!WARNING]
> This developer API documentation, including structural class models, payload schemas, and lifecycle callback signatures, is provided as-is. Without warning, this information may become invalid, deprecated, or outdated due to continuous engine updates and patch deployments.

# The Mechanist Modding API Reference — Java 17 Template Suite

This document describes the modder-facing API seam introduced for the Simulation Editor Suite. It is intentionally separate from live editor internals so that example mods can compile against a narrow, explicit package: `mechanist.modapi`.

The included production examples are stored under `modding/examples/` and mirrored as compile-checked Java classes under `src/mechanist/modapi/examples/`:

| Editor subsystem | Example folder | Java entrypoint |
|---|---|---|
| Sector Editor | `modding/examples/anomalous-cosmic-sector/` | `mechanist.modapi.examples.AnomalousCosmicSectorMod` |
| Room Editor | `modding/examples/reinforced-hydroponics-lab/` | `mechanist.modapi.examples.ReinforcedHydroponicsLabMod` |
| Faction Editor | `modding/examples/cybernetic-collector-faction/` | `mechanist.modapi.examples.CyberneticCollectorFactionMod` |
| Item Editor | `modding/examples/localized-gravity-anchor/` | `mechanist.modapi.examples.LocalizedGravityAnchorItemMod` |
| Knowledge Editor | `modding/examples/ancient-xenobiology-knowledge/` | `mechanist.modapi.examples.AncientXenobiologyKnowledgeMod` |
| Infopedia Editor | `modding/examples/precursor-infopedia/` | `mechanist.modapi.examples.PrecursorInfopediaMod` |

## 1. Package Structure

Mod integrations should import the public API package:

```java
import mechanist.modapi.ModIntegrationHook;
import mechanist.modapi.SimulationContext;
```

A Java integration class implements `ModIntegrationHook`. The engine, editor preview harness, headless server, or test harness owns a `SimulationModRuntime`, registers each hook, and dispatches lifecycle callbacks into the shared `SimulationContext`.

```java
SimulationContext context = new SimulationContext();
SimulationModRuntime runtime = new SimulationModRuntime(context);
runtime.register(new YourModHook());
runtime.tickSector("your-sector-id");
```

The API is intentionally narrow. Mods should manipulate `SectorInstance`, `RoomNode`, `FactionProfile`, `ItemTemplate`, `ResearchTree`, and `LoreDatabase` through their public methods instead of reaching into the older Swing editor repository.

## 2. Core Structural Model Relationships

The current modding seam models the editor subsystems like this:

```text
SimulationModRuntime
  └── SimulationContext
      ├── Map<String, SectorInstance>
      ├── Map<String, RoomNode>
      ├── Map<String, FactionProfile>
      ├── Map<String, ItemTemplate>
      ├── ResearchTree
      │   └── Map<String, ResearchNode>
      └── LoreDatabase
          └── Map<String, LoreEntry>
```

`SimulationModRuntime` is the dispatcher. It advances ticks and calls hooks.

`SimulationContext` is the safe mutable registry. Mods register sectors, rooms, factions, items, research nodes, and lore entries here. It also emits typed `SimulationEvent` records for audit, preview, and testing.

`SectorInstance` corresponds to the Sector Editor. It owns interstellar coordinates, environmental hazards, faction-control percentages, arbitrary typed attributes, and the current navigation vector.

`RoomNode` corresponds to the Room Editor. It owns dimensions, oxygen seal state, security terminal count, oxygen percentage, placement nodes, and arbitrary typed attributes.

`FactionProfile` corresponds to the Faction Editor. It owns political alignment vectors, economic resource ledgers, aggression matrices, cultural traits, and arbitrary typed attributes.

`ItemTemplate` corresponds to the Item Editor. It owns tech tier, mass, fuel charges, durability, components, and arbitrary typed attributes.

`ResearchTree` corresponds to the Knowledge Editor. It owns `ResearchNode` records, prerequisite relationships, and blueprint unlock payloads.

`LoreDatabase` corresponds to the Infopedia Editor. It owns `LoreEntry` records, taxonomy paths, search tags, and cross-links.

## 3. Lifecycle Callback Hooks

`ModIntegrationHook` is the contract every Java mod integration class implements.

```java
public interface ModIntegrationHook {
    String modId();

    default void onRegister(SimulationContext context) { }
    default void onSectorEnter(SimulationContext context, SectorInstance sector) { }
    default void onSectorTick(SimulationContext context, SectorInstance sector) { }
    default void onRoomTick(SimulationContext context, RoomNode room) { }
    default void onItemConsumed(SimulationContext context, ItemTemplate item) { }
    default void onFactionDiplomacyChange(SimulationContext context, FactionProfile faction, DiplomacyChange change) { }
    default void onResearchNodeUnlocked(SimulationContext context, ResearchTree researchTree, ResearchNode node) { }
    default List<LoreEntry> onLoreQuery(SimulationContext context, LoreDatabase loreDatabase, LoreQuery query) { return loreDatabase.search(query.queryText()); }
}
```

The implemented API file includes null checks in default methods. Mod overrides should still validate identifiers and return immediately when the callback is for a different entity.

Recommended callback uses:

| Hook | Purpose |
|---|---|
| `onRegister()` | Inject static records into `SimulationContext`: sectors, room archetypes, factions, item templates, research nodes, and lore entries. |
| `onSectorEnter()` | Apply one-time navigation, hazard, or faction-control changes when a player/fleet enters a sector. |
| `onSectorTick()` | Apply recurring sector effects such as storms, gravity drift, radiation, patrol sweeps, or anomaly pulses. |
| `onRoomTick()` | Update oxygen, containment, security, growth nodes, power state, contamination, or atmosphere behavior. |
| `onItemConsumed()` | Consume charges, decay durability, apply effects, and record failure states for tools or consumables. |
| `onFactionDiplomacyChange()` | Mutate aggression matrices, cultural reactions, economy state, or political relationship values. |
| `onResearchNodeUnlocked()` | Emit blueprint payloads, unlock downstream records, or record narrative/science events. |
| `onLoreQuery()` | Add search ranking, taxonomy filtering, cross-link expansion, or custom wiki lookup behavior. |

## 4. Manifest Schema Used by the Examples

Each example mod contains a valid `manifest.json` with these fields:

```json
{
  "id": "mechanist.example.example_id",
  "version": "1.0.0",
  "schemaVersion": 1,
  "name": "Readable Mod Name",
  "description": "What the mod adds.",
  "authors": ["The Mechanist API Team"],
  "license": "Example content for modder education",
  "entrypoint": "mechanist.modapi.examples.ExampleHookClass",
  "supportedGameVersion": "0.9.10hv",
  "dependencies": [
    {
      "id": "mechanist.core",
      "version": ">=0.9.10hv",
      "required": true
    }
  ],
  "components": {
    "sectors": [],
    "rooms": [],
    "factions": [],
    "items": [],
    "knowledge": [],
    "infopedia": []
  },
  "tags": ["example"]
}
```

The manifest `entrypoint` must name a Java class implementing `ModIntegrationHook`. The `components` object is an index for editors, packagers, and validation tooling. It should list IDs registered by the hook.

## 5. Sector Editor API

### Structural class

`SectorInstance` is the Sector Editor runtime payload. It carries coordinates, hazards, faction-control shares, a typed attribute map, and a `NavigationVector`.

```java
SectorInstance sector = new SectorInstance(
        "sector.null-tide-recurrence",
        "Null-Tide Recurrence",
        new SectorCoordinates(41, -9, 7));
sector.addEnvironmentalHazard("fluctuating-gravity-field");
sector.setFactionControl("neutral-cartographer-guild", 45);
sector.setNavigationVector(new NavigationVector(1.0, 0.0, 0.15));
context.registerSector(sector);
```

### Integration pattern

Use `onRegister()` for the static sector record. Use `onSectorEnter()` and `onSectorTick()` for dynamic navigation effects.

```java
@Override
public void onSectorTick(SimulationContext context, SectorInstance sector) {
    if (!"sector.null-tide-recurrence".equals(sector.id())) return;
    if (context.tick() % 12L != 0L) return;
    NavigationVector oldVector = sector.navigationVector();
    NavigationVector shifted = oldVector.add(new NavigationVector(0.1, -0.05, 0.02)).clampMagnitude(2.25);
    sector.setNavigationVector(shifted);
    context.emit(new SimulationEvent.NavigationVectorChanged(sector.id(), oldVector, shifted, "spatial anomaly pulse"));
}
```

The included complete example is `AnomalousCosmicSectorMod`.

## 6. Room Editor API

### Structural class

`RoomNode` is the Room Editor runtime payload. It models dimensions, oxygen sealing, security terminals, oxygen percentage, placement nodes, and arbitrary typed attributes.

```java
RoomNode room = new RoomNode(
        "room.reinforced-hydroponics-lab",
        "Reinforced Hydroponics Laboratory",
        new RoomDimensions(14, 10),
        true,
        2);
room.addPlacementNode(new PlacementNode("growth-cell-a", 3, 3, "growth-node-containment-cell"));
room.setAttribute("scrubberResponseRate", 1.75d);
context.registerRoom(room);
```

### Integration pattern

Use `onRoomTick()` to apply atmosphere, containment, and room-equipment logic.

```java
@Override
public void onRoomTick(SimulationContext context, RoomNode room) {
    if (!"room.reinforced-hydroponics-lab".equals(room.id())) return;
    double oldOxygen = room.oxygenPercent();
    double nextOxygen = Math.min(21.0d, oldOxygen + 1.75d);
    room.setOxygenPercent(nextOxygen);
    context.emit(new SimulationEvent.RoomAtmosphereChanged(room.id(), oldOxygen, nextOxygen, "scrubber correction"));
}
```

The included complete example is `ReinforcedHydroponicsLabMod`.

## 7. Faction Editor API

### Structural class

`FactionProfile` is the Faction Editor runtime payload. It models alignment, synthetic or ordinary resource ledgers, aggression matrices, cultural traits, and attributes.

```java
FactionProfile faction = new FactionProfile(
        "faction.cogwork-collectors",
        "Cogwork Collectors",
        new AlignmentVector(72, -48, 86, -66, 94));
faction.setEconomicResource("synthetic-memory-filaments", 4800);
faction.addCulturalTrait("isolationist");
faction.setAttribute("economyModel", "synthetic-collector-ledger");
context.registerFaction(faction);
```

### Integration pattern

Use `onFactionDiplomacyChange()` to react to diplomatic events. Java 17 switch expressions are appropriate for enumerated signals.

```java
@Override
public void onFactionDiplomacyChange(SimulationContext context, FactionProfile faction, DiplomacyChange change) {
    if (!"faction.cogwork-collectors".equals(faction.id())) return;
    int delta = switch (change.signal()) {
        case TRADE_REQUEST -> faction.culturalTraits().contains("isolationist") ? 3 : -2;
        case ESPIONAGE_DISCOVERED -> 18;
        case RESOURCE_CLAIM_CONFLICT -> 12;
        case ALLIANCE_OFFER, CEASEFIRE_REQUEST -> -6;
        case BORDER_INCIDENT, UNKNOWN -> 4;
    };
    faction.setAggressionToward(change.targetFactionId(), faction.aggressionToward(change.targetFactionId()) + delta);
}
```

The included complete example is `CyberneticCollectorFactionMod`.

## 8. Item Editor API

### Structural class

`ItemTemplate` is the Item Editor runtime payload. It models tech tier, mass, plasma or other fuel charges, durability, components, and attributes.

```java
ItemTemplate anchor = new ItemTemplate(
        "item.localized-gravity-anchor",
        "Localized Gravity Anchor",
        7,
        18.5d,
        6,
        100);
anchor.addComponent("plasma-fuel-cell", 2);
anchor.setAttribute("massAdjustmentKg", 1250.0d);
context.registerItem(anchor);
```

### Integration pattern

Use `onItemConsumed()` for tools, charges, durability decay, and failure notices.

```java
@Override
public void onItemConsumed(SimulationContext context, ItemTemplate item) {
    if (!"item.localized-gravity-anchor".equals(item.id())) return;
    int oldCharges = item.fuelCharges();
    int oldDurability = item.durability();
    if (!item.consumeCharge()) {
        context.audit(modId(), "gravity anchor had no plasma fuel charges");
        return;
    }
    item.damage(9);
    context.emit(new SimulationEvent.ItemStateChanged(item.id(), oldCharges, item.fuelCharges(), oldDurability, item.durability(), "anchor activation"));
}
```

The included complete example is `LocalizedGravityAnchorItemMod`.

## 9. Knowledge Editor API

### Structural classes

`ResearchTree` owns `ResearchNode` records. Each node has an ID, display name, prerequisite IDs, blueprint unlock IDs, and an unlocked flag.

```java
ResearchTree tree = context.researchTree();
tree.addNode(new ResearchNode(
        "knowledge.ancient-xenobiology.root",
        "Ancient Xenobiology Survey",
        List.of(),
        List.of("blueprint.xeno-sample-vault"),
        false));
tree.addNode(new ResearchNode(
        "knowledge.ancient-xenobiology.synthesis",
        "Xenobiological Containment Synthesis",
        List.of("knowledge.ancient-xenobiology.spores", "knowledge.ancient-xenobiology.chitin"),
        List.of("blueprint.ancient-growth-containment-cell"),
        false));
```

### Integration pattern

Use `onRegister()` to add graph nodes. Use `onResearchNodeUnlocked()` to emit unlock effects or auxiliary records.

```java
@Override
public void onResearchNodeUnlocked(SimulationContext context, ResearchTree researchTree, ResearchNode node) {
    if (!node.id().startsWith("knowledge.ancient-xenobiology.")) return;
    for (String blueprint : node.unlockedBlueprints()) {
        context.emit(new SimulationEvent.ResearchUnlocked(node.id(), blueprint, "xenobiology blueprint unlocked"));
    }
}
```

The included complete example is `AncientXenobiologyKnowledgeMod`.

## 10. Infopedia Editor API

### Structural classes

`LoreDatabase` owns `LoreEntry` records. Each entry has an ID, title, taxonomy path, body, search tags, and cross-links.

```java
LoreEntry entry = new LoreEntry(
        "lore.precursor.aurelian-concordance",
        "The Aurelian Concordance",
        "precursor-civilizations/aurelian-concordance/overview",
        "A vanished polity remembered through gravity-locked archives.",
        List.of("precursor", "aurelian", "taxonomy"),
        List.of("lore.precursor.castes"));
context.loreDatabase().addEntry(entry);
```

### Integration pattern

Use `onRegister()` to add entries. Use `onLoreQuery()` to provide ranking, taxonomy expansion, or cross-link routing.

```java
@Override
public List<LoreEntry> onLoreQuery(SimulationContext context, LoreDatabase loreDatabase, LoreQuery query) {
    List<LoreEntry> results = loreDatabase.search(query.queryText());
    if (results.size() <= query.maxResults()) return results;
    return List.copyOf(results.subList(0, query.maxResults()));
}
```

The included complete example is `PrecursorInfopediaMod`.

## 11. Thread Safety and UI Safety

Mod hooks shown here are synchronous simulation hooks. They should not run slow I/O directly. Exporting ZIPs, uploading to Steam Workshop, or scanning large file trees must stay outside Swing's Event Dispatch Thread through `SwingWorker`, `CompletableFuture`, or a server worker pool.

Safe pattern:

```java
SwingWorker<Path, Integer> worker = new SwingWorker<>() {
    @Override
    protected Path doInBackground() throws Exception {
        return exportZipArchiveOutsideTheEdt();
    }
};
worker.execute();
```

Unsafe pattern:

```java
// Do not block a button callback with filesystem or network work.
button.addActionListener(event -> exportLargeModArchive());
```

The included `ModDeploymentManager` already performs long-running deployment through `SwingWorker` and publishes progress events back to the editor UI.

## 12. Validation Checklist for Mod Authors

Before packaging a mod:

1. Confirm the manifest `id` matches the `modId()` returned by the Java hook.
2. Confirm the manifest `entrypoint` names the fully qualified Java class.
3. Confirm every component ID listed in `components` is registered by `onRegister()`.
4. Confirm dynamic hooks return immediately when the callback does not match the mod's entity ID.
5. Confirm item charge and durability changes clamp values instead of going negative.
6. Confirm research nodes do not contain impossible prerequisite cycles.
7. Confirm lore tags include both human-readable terms and taxonomy terms.
8. Run `ModdingApiTemplateSmoke` or an equivalent harness before distributing.

## 13. Minimal Standalone Harness

```java
SimulationContext context = new SimulationContext();
SimulationModRuntime runtime = new SimulationModRuntime(context);
runtime.register(new mechanist.modapi.examples.AnomalousCosmicSectorMod());
runtime.enterSector("sector.null-tide-recurrence");
for (int i = 0; i < 12; i++) {
    runtime.tickSector("sector.null-tide-recurrence");
}
System.out.println(context.events());
```

This pattern is how a headless test, editor preview, or future server authority can verify mod behavior without opening a Swing window.

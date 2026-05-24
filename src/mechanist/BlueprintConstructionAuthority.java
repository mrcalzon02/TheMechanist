package mechanist;

import java.util.*;

/**
 * Named authority for early room-blueprint/editor construction contracts.
 *
 * The authority is intentionally data-first: it can be exercised by tests,
 * future editor UI, and construction placement previews without requiring a
 * live generated world.  Live integration should feed actual tile descriptors,
 * entity blockers, and faction storage into the same validation request.
 */
final class BlueprintConstructionAuthority {
    private BlueprintConstructionAuthority() {}

    enum CellKind { EMPTY, FLOOR, WALL, DOOR, MACHINE, FURNITURE, LOGIC_IO }
    enum AnchorKind { CONNECTION, DOORWAY, INPUT, OUTPUT }
    enum ValidationSeverity { INFO, WARNING, BLOCKED }

    record BlueprintCell(int x, int y, CellKind kind, String abstractId) {
        BlueprintCell {
            if (x < 0 || y < 0) throw new IllegalArgumentException("blueprint cell offsets must be non-negative");
            kind = kind == null ? CellKind.EMPTY : kind;
            abstractId = abstractId == null ? kind.name().toLowerCase(Locale.ROOT) : abstractId.trim();
        }
    }

    record AnchorPoint(int x, int y, AnchorKind kind, String direction) {
        AnchorPoint {
            if (x < 0 || y < 0) throw new IllegalArgumentException("anchor offsets must be non-negative");
            kind = kind == null ? AnchorKind.CONNECTION : kind;
            direction = direction == null || direction.isBlank() ? "ANY" : direction.trim().toUpperCase(Locale.ROOT);
        }
    }

    record BlueprintObject(int x, int y, String objectId, String role) {
        BlueprintObject {
            if (x < 0 || y < 0) throw new IllegalArgumentException("object offsets must be non-negative");
            objectId = objectId == null || objectId.isBlank() ? "generic-object" : objectId.trim();
            role = role == null || role.isBlank() ? "decor" : role.trim();
        }
    }

    record RoomBlueprint(
            String id,
            String name,
            String role,
            String themeId,
            int width,
            int height,
            List<BlueprintCell> cells,
            List<AnchorPoint> anchors,
            List<BlueprintObject> objects,
            List<String> tags
    ) {
        RoomBlueprint {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("blueprint dimensions must be positive");
            id = safeId(id, "room-blueprint");
            name = name == null || name.isBlank() ? "Unnamed Room Blueprint" : name.trim();
            role = role == null || role.isBlank() ? "generic" : role.trim();
            themeId = themeId == null || themeId.isBlank() ? "default" : themeId.trim();
            cells = List.copyOf(cells == null ? List.of() : cells);
            anchors = List.copyOf(anchors == null ? List.of() : anchors);
            objects = List.copyOf(objects == null ? List.of() : objects);
            tags = List.copyOf(tags == null ? List.of() : tags);
        }

        int materialCellCount() {
            int count = 0;
            for (BlueprintCell cell : cells) if (cell.kind() != CellKind.EMPTY) count++;
            return count;
        }
    }

    record TargetTile(int x, int y, boolean occupied, boolean solidMountain, boolean deepLiquid, boolean criticalEntity, boolean buildable, String descriptor) {}

    record ValidationIssue(ValidationSeverity severity, int x, int y, String reason) {
        boolean blocked() { return severity == ValidationSeverity.BLOCKED; }
    }

    record ValidationResult(boolean canPlace, List<ValidationIssue> issues, int ghostTileCount, String summary) {}

    record GhostTile(int worldX, int worldY, CellKind plannedKind, String abstractId, boolean collisionless) {}
    record GhostPlan(RoomBlueprint blueprint, int originX, int originY, List<GhostTile> ghosts, ValidationResult validation) {}
    record BuildComponent(String itemName, int count, String purpose) {
        BuildComponent {
            itemName = itemName == null || itemName.isBlank() ? "Construction supplies" : itemName.trim();
            count = Math.max(0, count);
            purpose = purpose == null || purpose.isBlank() ? "construction input" : purpose.trim();
        }
    }
    record TileBuildRecipe(String abstractId, CellKind kind, String label, int laborTurns, boolean requiresClearance, List<BuildComponent> components) {
        TileBuildRecipe {
            abstractId = abstractId == null || abstractId.isBlank() ? kind.name().toLowerCase(Locale.ROOT) : abstractId.trim();
            kind = kind == null ? CellKind.EMPTY : kind;
            label = label == null || label.isBlank() ? abstractId : label.trim();
            laborTurns = Math.max(1, laborTurns);
            components = List.copyOf(components == null ? List.of() : components);
        }
    }

    /** Hollow box tool: perimeter walls, interior floors, optional doorway anchor on the north wall. */
    static RoomBlueprint hollowBox(String id, String name, int width, int height, boolean northDoor) {
        List<BlueprintCell> cells = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean edge = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                CellKind kind = edge ? CellKind.WALL : CellKind.FLOOR;
                String abstractId = edge ? "wall_tier_1" : "floor_tier_1";
                cells.add(new BlueprintCell(x, y, kind, abstractId));
            }
        }
        List<AnchorPoint> anchors = new ArrayList<>();
        if (northDoor && width >= 3) {
            int doorX = width / 2;
            replaceCell(cells, new BlueprintCell(doorX, 0, CellKind.DOOR, "door_basic"));
            anchors.add(new AnchorPoint(doorX, 0, AnchorKind.DOORWAY, "N"));
        }
        return new RoomBlueprint(id, name, "room", "generic-structure", width, height, cells, anchors, List.of(), List.of("#Room", "#HollowBox"));
    }

    static ValidationResult preflight(RoomBlueprint blueprint, int originX, int originY, Collection<TargetTile> targetTiles, Map<String, Integer> availableResources) {
        if (blueprint == null) {
            return new ValidationResult(false, List.of(new ValidationIssue(ValidationSeverity.BLOCKED, originX, originY, "No blueprint selected.")), 0, "Placement blocked: no blueprint selected.");
        }
        Map<String, TargetTile> targets = new HashMap<>();
        if (targetTiles != null) {
            for (TargetTile t : targetTiles) targets.put(key(t.x(), t.y()), t);
        }
        List<ValidationIssue> issues = new ArrayList<>();
        int ghosts = 0;
        for (BlueprintCell cell : blueprint.cells()) {
            if (cell.kind() == CellKind.EMPTY) continue;
            ghosts++;
            int wx = originX + cell.x();
            int wy = originY + cell.y();
            TargetTile target = targets.get(key(wx, wy));
            if (target == null) {
                issues.add(new ValidationIssue(ValidationSeverity.WARNING, wx, wy, "Target tile has no descriptor; live placement should inspect the real world grid before committing."));
                continue;
            }
            if (!target.buildable()) issues.add(new ValidationIssue(ValidationSeverity.BLOCKED, wx, wy, "Target tile is not buildable: " + target.descriptor()));
            if (target.occupied()) issues.add(new ValidationIssue(ValidationSeverity.BLOCKED, wx, wy, "Placement obstructed by existing object or construction."));
            if (target.solidMountain()) issues.add(new ValidationIssue(ValidationSeverity.BLOCKED, wx, wy, "Placement intersects unmined wall/rock and must become a dig task first."));
            if (target.deepLiquid()) issues.add(new ValidationIssue(ValidationSeverity.BLOCKED, wx, wy, "Placement intersects deep liquid."));
            if (target.criticalEntity()) issues.add(new ValidationIssue(ValidationSeverity.BLOCKED, wx, wy, "Placement intersects critical faction entity."));
        }
        if (blueprint.anchors().isEmpty()) {
            issues.add(new ValidationIssue(ValidationSeverity.WARNING, originX, originY, "Blueprint has no connection anchor or doorway."));
        }
        Map<String, Integer> cost = estimateCost(blueprint);
        Map<String, Integer> have = availableResources == null ? Map.of() : availableResources;
        for (Map.Entry<String, Integer> e : cost.entrySet()) {
            int available = have.getOrDefault(e.getKey(), 0);
            if (available < e.getValue()) {
                issues.add(new ValidationIssue(ValidationSeverity.WARNING, originX, originY, "Resource shortfall: " + e.getKey() + " needs " + e.getValue() + ", available " + available + "."));
            }
        }
        boolean blocked = issues.stream().anyMatch(ValidationIssue::blocked);
        String summary = (blocked ? "Placement blocked" : "Placement valid") + ": " + blueprint.name() + " ghostTiles=" + ghosts + " issues=" + issues.size();
        return new ValidationResult(!blocked, List.copyOf(issues), ghosts, summary);
    }

    static GhostPlan createGhostPlan(RoomBlueprint blueprint, int originX, int originY, Collection<TargetTile> targetTiles, Map<String, Integer> availableResources) {
        ValidationResult validation = preflight(blueprint, originX, originY, targetTiles, availableResources);
        List<GhostTile> ghosts = new ArrayList<>();
        if (blueprint != null && validation.canPlace()) {
            for (BlueprintCell cell : blueprint.cells()) {
                if (cell.kind() != CellKind.EMPTY) ghosts.add(new GhostTile(originX + cell.x(), originY + cell.y(), cell.kind(), cell.abstractId(), true));
            }
        }
        return new GhostPlan(blueprint, originX, originY, List.copyOf(ghosts), validation);
    }

    static Map<String, Integer> estimateCost(RoomBlueprint blueprint) {
        return estimateItemCost(blueprint);
    }

    static Map<String, Integer> estimateItemCost(RoomBlueprint blueprint) {
        Map<String, Integer> cost = new TreeMap<>();
        if (blueprint == null) return cost;
        for (BlueprintCell cell : blueprint.cells()) {
            if (cell.kind() == CellKind.EMPTY) continue;
            TileBuildRecipe recipe = recipeFor(cell);
            for (BuildComponent component : recipe.components()) {
                if (component.count() > 0) cost.merge(component.itemName(), component.count(), Integer::sum);
            }
        }
        for (BlueprintObject object : blueprint.objects()) cost.merge(object.role(), 1, Integer::sum);
        return cost;
    }

    static Map<String, Integer> estimateLaborTurns(RoomBlueprint blueprint) {
        Map<String, Integer> turns = new TreeMap<>();
        if (blueprint == null) return turns;
        for (BlueprintCell cell : blueprint.cells()) {
            if (cell.kind() == CellKind.EMPTY) continue;
            TileBuildRecipe recipe = recipeFor(cell);
            turns.merge(recipe.label(), recipe.laborTurns(), Integer::sum);
        }
        return turns;
    }

    static List<String> hollowBoxRecipeEvaluation(RoomBlueprint blueprint) {
        List<String> lines = new ArrayList<>();
        if (blueprint == null) {
            lines.add("No Hollow Box blueprint selected.");
            return lines;
        }
        int floors = 0, walls = 0, doors = 0, other = 0;
        for (BlueprintCell cell : blueprint.cells()) {
            switch (cell.kind()) {
                case FLOOR -> floors++;
                case WALL -> walls++;
                case DOOR -> doors++;
                case EMPTY -> { }
                default -> other++;
            }
        }
        lines.add("Hollow Box recipe evaluation: " + blueprint.name() + " " + blueprint.width() + "x" + blueprint.height()
                + " cells=" + blueprint.materialCellCount() + " floors=" + floors + " walls=" + walls + " doors=" + doors + " other=" + other + ".");
        lines.add("Itemized build cost: " + estimateItemCost(blueprint));
        lines.add("Labor estimate: " + estimateLaborTurns(blueprint));
        lines.add("Recipe rule: ghost placement remains collisionless; physical collision/functionality begins only when tile construction tasks complete.");
        lines.add("Clearance rule: wall and door recipes require target clearance or a generated dig/clear task when placed into unmined structure.");
        return lines;
    }

    static TileBuildRecipe recipeFor(BlueprintCell cell) {
        String id = cell == null ? "" : String.valueOf(cell.abstractId()).toLowerCase(Locale.ROOT);
        CellKind kind = cell == null ? CellKind.EMPTY : cell.kind();
        if (id.contains("floor_tier_1") || kind == CellKind.FLOOR) {
            return new TileBuildRecipe("floor_tier_1", CellKind.FLOOR, "Tier-1 floor plate", 1, false,
                    List.of(new BuildComponent("Construction supplies", 1, "floor plate, mortar, fasteners, and finish work")));
        }
        if (id.contains("door_basic") || kind == CellKind.DOOR) {
            return new TileBuildRecipe("door_basic", CellKind.DOOR, "Basic door kit", 3, true,
                    List.of(new BuildComponent("Construction supplies", 1, "door frame and fitting work"),
                            new BuildComponent("Scrap plate", 1, "door slab reinforcement"),
                            new BuildComponent("Rivet set", 1, "frame fastening"),
                            new BuildComponent("Bearing set", 1, "hinge and latch motion")));
        }
        if (id.contains("wall_tier_1") || kind == CellKind.WALL) {
            return new TileBuildRecipe("wall_tier_1", CellKind.WALL, "Tier-1 wall panel", 2, true,
                    List.of(new BuildComponent("Construction supplies", 1, "wall panel bulk material"),
                            new BuildComponent("Scrap plate", 1, "crude wall reinforcement"),
                            new BuildComponent("Rivet set", 1, "panel fastening")));
        }
        if (kind == CellKind.LOGIC_IO) {
            return new TileBuildRecipe("logic_io", CellKind.LOGIC_IO, "Logic or utility interface", 2, false,
                    List.of(new BuildComponent("Wire bundle", 1, "control line"), new BuildComponent("Pipe coupling set", 1, "utility pass-through")));
        }
        if (kind == CellKind.MACHINE) {
            return new TileBuildRecipe("machine", CellKind.MACHINE, "Machine mount", 4, true,
                    List.of(new BuildComponent("Machine part", 1, "installed machine frame"), new BuildComponent("Tool bundle", 1, "installation tools")));
        }
        if (kind == CellKind.FURNITURE) {
            return new TileBuildRecipe("furniture", CellKind.FURNITURE, "Furnishing", 2, false,
                    List.of(new BuildComponent("Construction supplies", 1, "basic fixture material")));
        }
        return new TileBuildRecipe("generic_material", kind, "Generic construction", 1, false,
                List.of(new BuildComponent("Construction supplies", 1, "fallback construction input")));
    }

    static String auditSummary() {
        RoomBlueprint sample = hollowBox("sample-hollow-box", "Hollow Box Test Room", 5, 4, true);
        return "blueprintAuthority=room-object-schema relativeOffsets=true hollowBox=true anchors=" + sample.anchors().size()
                + " validation=obstruction/resource/preflight ghostCollision=false cost=" + estimateCost(sample);
    }

    static List<String> optionLines(GameOptions options) {
        List<String> lines = new ArrayList<>();
        lines.add("Construction/editor foundations use logical room objects: bounding box, metadata, relative tile array, anchors, and object matrix.");
        lines.add("Invalid placement diagnostics: " + onOff(options == null || options.invalidPlacementDiagnostics) + "; ghost room stamps: " + onOff(options == null || options.ghostRoomStamps) + "; preflight checklist: " + onOff(options == null || options.blueprintPreflightChecklist) + ".");
        lines.add("Layout tools: hollow-box room drawing " + onOff(options == null || options.hollowBoxTool) + "; anchor snapping " + onOff(options == null || options.anchorPointSnapping) + "; resource estimate tooltips " + onOff(options == null || options.resourceEstimateTooltips) + ".");
        lines.add("Catalog/testing: sandbox room canvas " + onOff(options == null || options.standaloneBlueprintSandbox) + "; design capture " + onOff(options == null || options.blueprintCaptureTool) + "; material substitution prompts " + onOff(options == null || options.materialSubstitutionPrompts) + ".");
        lines.add("Ghost construction rule: previews and stamped tasks are collisionless until built, so blueprints cannot trap the player or workers while still planning rooms.");
        lines.addAll(hollowBoxRecipeEvaluation(hollowBox("sample-hollow-box", "Hollow Box Test Room", 5, 4, true)));
        return lines;
    }

    private static void replaceCell(List<BlueprintCell> cells, BlueprintCell replacement) {
        for (int i = 0; i < cells.size(); i++) {
            BlueprintCell c = cells.get(i);
            if (c.x() == replacement.x() && c.y() == replacement.y()) {
                cells.set(i, replacement);
                return;
            }
        }
        cells.add(replacement);
    }

    private static String key(int x, int y) { return x + "," + y; }
    private static String onOff(boolean v) { return v ? "ON" : "OFF"; }
    private static String safeId(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
    }
}

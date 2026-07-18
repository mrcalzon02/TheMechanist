package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared large-target combat boundary for vehicles, machines, durable fixtures,
 * doors, walls, and other structural targets.
 *
 * Actor combat remains owned by the existing NPC targeting lane. Structural
 * truth stays on the owning vehicle, BaseObject, MapObjectState, or the existing
 * terrainIntegrity ledger instead of creating a parallel damage registry.
 */
final class StructuralScaleCombatAuthority {
    enum WeaponTier {
        UNARMED("unarmed strike"),
        LIGHT_MELEE("light hand weapon"),
        HEAVY_MELEE("heavy hand weapon"),
        HANDGUN("handgun"),
        RIFLE("rifle"),
        HEAVY_HANDHELD("heavy handheld weapon"),
        ANTI_ARMOR("anti-armor weapon"),
        MOUNTED("mounted structural weapon");

        final String label;
        WeaponTier(String label) { this.label = label; }
    }

    enum TargetKind {
        ACTOR,
        VEHICLE,
        MACHINE,
        STRUCTURE,
        DOOR,
        WALL,
        DURABLE_OBJECT,
        NONE
    }

    record WeaponProfile(String name, WeaponTier tier, int force,
                         int penetration, int range, boolean firearm,
                         int ammunitionPerAttack) { }

    record TargetProfile(TargetKind kind, String label, int armor,
                         int currentIntegrity, int maximumIntegrity,
                         MapObjectState mapObject, BaseObject baseObject,
                         VehicleRuntimeAuthority.Component vehicleComponent,
                         int x, int y) {
        boolean durable() {
            return kind != TargetKind.NONE && kind != TargetKind.ACTOR;
        }
    }

    record Preview(boolean available, boolean delegatedActor,
                   boolean wouldDamage, int projectedDamage,
                   int ammunitionRequired, WeaponProfile weapon,
                   TargetProfile target, String summary) { }

    record Result(boolean handled, boolean success, boolean changed,
                  int damage, int ammunitionSpent, String message,
                  Preview preview) { }

    private StructuralScaleCombatAuthority() { }

    static Preview preview(GamePanel game) {
        if (game == null) return unavailable("Combat targeting is unavailable.");
        return preview(game, game.combatX, game.combatY,
                game.activeWeaponName(), game.fireModeLabel());
    }

    static Preview preview(GamePanel game, int x, int y,
                           String weaponName, String fireMode) {
        if (game == null || game.world == null) {
            return unavailable("Combat targeting requires a loaded world.");
        }
        if (!game.world.inBounds(x, y)) {
            return unavailable("The selected combat target is outside the current world slice.");
        }
        WeaponProfile weapon = weaponProfile(weaponName, fireMode);
        TargetProfile target = targetAt(game, x, y);
        if (target.kind() == TargetKind.ACTOR) {
            return new Preview(true, true, false, 0,
                    ammunitionRequired(weapon, fireMode), weapon, target,
                    "Combat target: " + target.label()
                            + ". Actor damage remains on the existing combat lane; confirm to take aim.");
        }
        if (!target.durable()) {
            return new Preview(false, false, false, 0, 0, weapon, target,
                    "No actor or durable structural target is present at "
                            + x + "," + y + ".");
        }
        int distance = distance(game.playerX, game.playerY, x, y);
        if (distance > weapon.range()) {
            return new Preview(false, false, false, 0, 0, weapon, target,
                    target.label() + " is outside the " + weapon.tier().label
                            + " range of " + weapon.range() + " tile(s).");
        }
        if (weapon.range() > 1 && !clearLine(game.world,
                game.playerX, game.playerY, x, y)) {
            return new Preview(false, false, false, 0, 0, weapon, target,
                    "The line of fire to " + target.label() + " is blocked.");
        }
        int required = ammunitionRequired(weapon, fireMode);
        if (weapon.firearm()) {
            int loaded = Math.max(0, game.loadedWeaponShots.getOrDefault(
                    weapon.name(), 0));
            if (loaded < required) {
                return new Preview(false, false, false, 0, required,
                        weapon, target, weapon.name() + " needs " + required
                        + " loaded shot(s), but only " + loaded + " remain.");
            }
        }
        int damage = projectedDamage(weapon, target, fireMode);
        String summary = damage <= 0
                ? ineffectiveLine(weapon, target)
                : "Attack preview: " + weapon.name() + " can inflict about "
                + damage + " structural damage on " + target.label()
                + " (integrity " + target.currentIntegrity() + "/"
                + target.maximumIntegrity() + ").";
        return new Preview(true, false, damage > 0, damage, required,
                weapon, target, summary);
    }

    static Result confirm(GamePanel game) {
        Preview preview = preview(game);
        if (game == null) {
            return new Result(true, false, false, 0, 0,
                    preview.summary(), preview);
        }
        game.lastTargetingReport = preview.summary();
        if (!preview.available()) {
            game.logEvent(preview.summary());
            return new Result(true, false, false, 0, 0,
                    preview.summary(), preview);
        }
        if (preview.delegatedActor()) {
            game.confirmCombatTarget();
            return new Result(true, true, false, 0, 0,
                    game.lastTargetingReport, preview);
        }

        int ammunition = consumeAmmunition(game, preview.weapon(),
                preview.ammunitionRequired());
        int damage = preview.projectedDamage();
        boolean changed = false;
        String impact;
        if (damage <= 0) {
            impact = ineffectiveLine(preview.weapon(), preview.target());
        } else {
            changed = applyDamage(game, preview.target(), damage,
                    preview.weapon().name());
            int remaining = remainingIntegrity(game, preview.target());
            impact = impactLine(preview.weapon(), preview.target(),
                    remaining, damage, changed);
        }
        game.lastTargetingReport = impact;
        game.advanceTurn(impact);
        if (changed) {
            game.markLocalDirtyRegion("structural combat impact",
                    preview.target().x(), preview.target().y(), 3,
                    true, false, true, false);
        }
        return new Result(true, true, changed, changed ? damage : 0,
                ammunition, impact, preview);
    }

    static void refreshTargetingReport(GamePanel game) {
        if (game == null) return;
        game.lastTargetingReport = preview(game).summary();
    }

    static WeaponProfile weaponProfile(String weaponName, String fireMode) {
        String name = clean(weaponName, "Bare hands");
        String text = name.toLowerCase(Locale.ROOT);
        WeaponTier tier;
        int force;
        int penetration;
        int range;
        boolean firearm;

        if (contains(text, "mounted", "autocannon", "tank cannon",
                "heavy bolter", "siege gun")) {
            tier = WeaponTier.MOUNTED;
            force = 24;
            penetration = 20;
            range = 16;
            firearm = true;
        } else if (contains(text, "anti-armor", "anti armour", "plasma",
                "melta", "arc rifle", "rocket", "launcher")) {
            tier = WeaponTier.ANTI_ARMOR;
            force = 16;
            penetration = 17;
            range = 12;
            firearm = true;
        } else if (contains(text, "heavy rifle", "long rifle", "heavy stubber",
                "bolter", "machine gun")) {
            tier = WeaponTier.HEAVY_HANDHELD;
            force = 14;
            penetration = 11;
            range = 13;
            firearm = true;
        } else if (contains(text, "rifle", "carbine", "lasgun")) {
            tier = WeaponTier.RIFLE;
            force = 11;
            penetration = 8;
            range = 12;
            firearm = true;
        } else if (contains(text, "pistol", "revolver", "stub gun",
                "webber", "shotgun")) {
            tier = WeaponTier.HANDGUN;
            force = text.contains("shotgun") ? 10 : 8;
            penetration = 5;
            range = text.contains("shotgun") ? 6 : 8;
            firearm = true;
        } else if (contains(text, "hammer", "maul", "axe", "pick",
                "crowbar", "cleaver", "chain", "saw")) {
            tier = WeaponTier.HEAVY_MELEE;
            force = 7;
            penetration = 2;
            range = 1;
            firearm = false;
        } else if (contains(text, "knife", "dagger", "bayonet", "spear",
                "blade", "sword")) {
            tier = WeaponTier.LIGHT_MELEE;
            force = 4;
            penetration = 1;
            range = 1;
            firearm = false;
        } else if (text.equals("bare hands") || text.contains("unarmed")) {
            tier = WeaponTier.UNARMED;
            force = 2;
            penetration = 0;
            range = 1;
            firearm = false;
        } else if (ItemCatalog.isFirearmLike(name)) {
            tier = WeaponTier.HANDGUN;
            force = 8;
            penetration = 5;
            range = 8;
            firearm = true;
        } else {
            tier = WeaponTier.LIGHT_MELEE;
            force = 4;
            penetration = 1;
            range = 1;
            firearm = false;
        }

        String mode = clean(fireMode, "SNAP").toUpperCase(Locale.ROOT);
        if (mode.equals("AIMED")) penetration += 3;
        if (mode.equals("BURST") && firearm) force += 3;
        int ammunition = firearm && mode.equals("BURST") ? 3 : firearm ? 1 : 0;
        return new WeaponProfile(name, tier, force, penetration, range,
                firearm, ammunition);
    }

    static TargetProfile targetAt(GamePanel game, int x, int y) {
        if (game == null || game.world == null || !game.world.inBounds(x, y)) {
            return none(x, y);
        }
        NpcEntity npc = game.world.npcAt(x, y);
        if (npc != null) {
            return new TargetProfile(TargetKind.ACTOR,
                    clean(npc.name, "local actor"), 0,
                    Math.max(0, npc.hp), Math.max(1, npc.hp),
                    null, null, null, x, y);
        }
        MapObjectState object = game.world.mapObjectAt(x, y);
        if (VehicleRuntimeAuthority.isVehicle(object)) {
            VehicleRuntimeAuthority.ensureInitialized(game.world, object);
            VehicleRuntimeAuthority.Snapshot snapshot =
                    VehicleRuntimeAuthority.inspect(game.world, object);
            VehicleRuntimeAuthority.Component component = vehicleComponent(snapshot);
            int current = snapshot.components().getOrDefault(component, 100);
            return new TargetProfile(TargetKind.VEHICLE,
                    vehicleLabel(snapshot), vehicleArmor(snapshot.vehicleClass()),
                    current, 100, object, null, component, x, y);
        }
        BaseObject base = baseObjectAt(game, x, y);
        if (base != null && !base.underConstruction) {
            return new TargetProfile(TargetKind.MACHINE,
                    clean(base.name, "constructed machine"),
                    baseArmor(base), Math.max(0, base.integrity),
                    baseMaximum(base), null, base, null, x, y);
        }
        if (object != null && durableObject(object)) {
            int maximum = intValue(MapObjectState.stockValue(
                    object.stockState, "structuralMaxIntegrity"),
                    objectMaximum(object));
            int current = intValue(MapObjectState.stockValue(
                    object.stockState, "structuralIntegrity"), maximum);
            TargetKind kind = machineObject(object)
                    ? TargetKind.MACHINE : TargetKind.DURABLE_OBJECT;
            return new TargetProfile(kind,
                    clean(object.label, clean(object.type, "durable fixture")),
                    objectArmor(object), current, maximum,
                    object, null, null, x, y);
        }
        char tile = game.world.tiles[x][y];
        if (tile != '/' && game.isDoorTile(tile)) {
            return tileTarget(game, TargetKind.DOOR, doorLabel(tile),
                    tileArmor(tile), tileMaximum(tile), x, y);
        }
        if (wallTile(tile)) {
            return tileTarget(game, TargetKind.WALL, wallLabel(tile),
                    tileArmor(tile), tileMaximum(tile), x, y);
        }
        if (structuralTile(tile)) {
            return tileTarget(game, TargetKind.STRUCTURE,
                    "structural obstacle", tileArmor(tile), tileMaximum(tile),
                    x, y);
        }
        return none(x, y);
    }

    private static TargetProfile tileTarget(GamePanel game, TargetKind kind,
                                            String label, int armor,
                                            int maximum, int x, int y) {
        int current = game.terrainIntegrity.getOrDefault(
                terrainKey(game, x, y), maximum);
        return new TargetProfile(kind, label, armor, current, maximum,
                null, null, null, x, y);
    }

    private static boolean applyDamage(GamePanel game, TargetProfile target,
                                       int damage, String source) {
        if (target.kind() == TargetKind.VEHICLE) {
            VehicleRuntimeAuthority.Result result =
                    VehicleRuntimeAuthority.applyDamage(target.mapObject(),
                            target.vehicleComponent(), damage, game.turn,
                            source + " structural combat");
            return result.success() && result.changed();
        }
        if (target.baseObject() != null) {
            int before = Math.max(0, target.baseObject().integrity);
            int after = Math.max(0, before - damage);
            target.baseObject().integrity = after;
            return before != after;
        }
        if (target.mapObject() != null) {
            int before = Math.max(0, target.currentIntegrity());
            int after = Math.max(0, before - damage);
            MapObjectState object = target.mapObject();
            object.stockState = MapObjectState.setStockFlag(object.stockState,
                    "structuralMaxIntegrity",
                    Integer.toString(target.maximumIntegrity()));
            object.stockState = MapObjectState.setStockFlag(object.stockState,
                    "structuralIntegrity", Integer.toString(after));
            object.stockState = MapObjectState.setStockFlag(object.stockState,
                    "structuralCondition", condition(after,
                            target.maximumIntegrity()));
            appendHistory(object, source + " caused " + damage
                    + " structural damage at turn " + game.turn);
            return before != after;
        }
        String key = terrainKey(game, target.x(), target.y());
        int before = game.terrainIntegrity.getOrDefault(key,
                target.maximumIntegrity());
        int after = Math.max(0, before - damage);
        game.terrainIntegrity.put(key, after);
        if (after <= 0) breachTile(game, target.x(), target.y(),
                game.world.tiles[target.x()][target.y()]);
        return before != after;
    }

    private static int remainingIntegrity(GamePanel game,
                                          TargetProfile target) {
        if (target.kind() == TargetKind.VEHICLE) {
            VehicleRuntimeAuthority.Snapshot snapshot =
                    VehicleRuntimeAuthority.inspect(game.world,
                            target.mapObject());
            return snapshot == null ? 0 : snapshot.components().getOrDefault(
                    target.vehicleComponent(), 0);
        }
        if (target.baseObject() != null) {
            return Math.max(0, target.baseObject().integrity);
        }
        if (target.mapObject() != null) {
            return intValue(MapObjectState.stockValue(
                    target.mapObject().stockState, "structuralIntegrity"), 0);
        }
        return Math.max(0, game.terrainIntegrity.getOrDefault(
                terrainKey(game, target.x(), target.y()), 0));
    }

    private static void breachTile(GamePanel game, int x, int y, char tile) {
        if (game.isDoorTile(tile)) game.world.tiles[x][y] = '/';
        else if (wallTile(tile) || structuralTile(tile)) game.world.tiles[x][y] = '.';
    }

    private static int projectedDamage(WeaponProfile weapon,
                                       TargetProfile target,
                                       String fireMode) {
        if (!target.durable()) return 0;
        if (weapon.penetration() < target.armor()) return 0;
        int damage = weapon.force()
                + Math.max(0, weapon.penetration() - target.armor())
                - target.armor() / 2;
        if (clean(fireMode, "SNAP").equalsIgnoreCase("AIMED")) damage += 1;
        return Math.max(1, Math.min(30, damage));
    }

    private static int consumeAmmunition(GamePanel game,
                                         WeaponProfile weapon, int required) {
        if (!weapon.firearm() || required <= 0) return 0;
        int loaded = Math.max(0, game.loadedWeaponShots.getOrDefault(
                weapon.name(), 0));
        int spent = Math.min(required, loaded);
        game.loadedWeaponShots.put(weapon.name(), loaded - spent);
        return spent;
    }

    private static String ineffectiveLine(WeaponProfile weapon,
                                          TargetProfile target) {
        return "The " + weapon.tier().label + " cannot meaningfully penetrate "
                + target.label() + ". The impact leaves no structural damage.";
    }

    private static String impactLine(WeaponProfile weapon,
                                     TargetProfile target,
                                     int remaining, int damage,
                                     boolean changed) {
        if (!changed) return ineffectiveLine(weapon, target);
        if (remaining <= 0) {
            return weapon.name() + " breaches " + target.label()
                    + "; structural integrity is exhausted.";
        }
        return weapon.name() + " damages " + target.label() + " for "
                + damage + " structural integrity; " + remaining + "/"
                + target.maximumIntegrity() + " remains.";
    }

    private static VehicleRuntimeAuthority.Component vehicleComponent(
            VehicleRuntimeAuthority.Snapshot snapshot) {
        if (snapshot == null) return VehicleRuntimeAuthority.Component.FRAME;
        int armor = snapshot.components().getOrDefault(
                VehicleRuntimeAuthority.Component.ARMOR, 100);
        return armor > 0 ? VehicleRuntimeAuthority.Component.ARMOR
                : VehicleRuntimeAuthority.Component.FRAME;
    }

    private static int vehicleArmor(
            VehicleRuntimeAuthority.VehicleClass vehicleClass) {
        return switch (vehicleClass) {
            case UTILITY_BIKE -> 5;
            case CIVILIAN_CAR -> 8;
            case CARGO_TRUCK -> 10;
            case ARMORED_CAR -> 14;
            case TANK -> 18;
        };
    }

    private static String vehicleLabel(
            VehicleRuntimeAuthority.Snapshot snapshot) {
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static BaseObject baseObjectAt(GamePanel game, int x, int y) {
        if (game == null || game.baseObjects == null) return null;
        for (BaseObject object : game.baseObjects) {
            if (object != null && object.x == x && object.y == y) return object;
        }
        return null;
    }

    private static int baseArmor(BaseObject base) {
        String text = baseText(base);
        if (contains(text, "reactor", "forge", "press", "industrial",
                "armored", "heavy")) return 12;
        if (contains(text, "machine", "generator", "workbench", "fabricator")) return 10;
        return 8;
    }

    private static int baseMaximum(BaseObject base) {
        String text = baseText(base);
        if (contains(text, "reactor", "forge", "press", "industrial",
                "armored", "heavy")) return 220;
        if (contains(text, "machine", "generator", "workbench", "fabricator")) return 180;
        return 140;
    }

    private static String baseText(BaseObject base) {
        return (clean(base == null ? null : base.name, "") + " "
                + clean(base == null ? null : base.description, ""))
                .toLowerCase(Locale.ROOT);
    }

    private static boolean durableObject(MapObjectState object) {
        if (object == null) return false;
        String text = objectText(object);
        return machineObject(object) || contains(text, "barricade", "crate",
                "gate", "vault", "bulkhead", "generator", "reactor",
                "press", "forge", "turret", "wreck", "pillar");
    }

    private static boolean machineObject(MapObjectState object) {
        String text = objectText(object);
        return contains(text, "machine", "generator", "reactor", "press",
                "forge", "fabricator", "workbench", "pump", "motor");
    }

    private static String objectText(MapObjectState object) {
        return (clean(object == null ? null : object.type, "") + " "
                + clean(object == null ? null : object.label, ""))
                .toLowerCase(Locale.ROOT);
    }

    private static int objectArmor(MapObjectState object) {
        String text = objectText(object);
        if (contains(text, "vault", "bulkhead", "armored", "reactor")) return 13;
        if (machineObject(object)) return 10;
        if (text.contains("barricade")) return 7;
        return 8;
    }

    private static int objectMaximum(MapObjectState object) {
        String text = objectText(object);
        if (contains(text, "vault", "bulkhead", "reactor")) return 220;
        if (machineObject(object)) return 180;
        if (text.contains("barricade")) return 120;
        return 140;
    }

    private static boolean wallTile(char tile) {
        return tile == '#' || tile == 'W' || tile == 'H';
    }

    private static boolean structuralTile(char tile) {
        return tile == 'Y' || tile == '+' || tile == '\\';
    }

    private static int tileArmor(char tile) {
        return switch (tile) {
            case 'X', 'D' -> 13;
            case '#', 'W', 'H' -> 14;
            case 'L', '|', 'V' -> 9;
            case 'Y' -> 8;
            default -> 7;
        };
    }

    private static int tileMaximum(char tile) {
        return switch (tile) {
            case 'X', 'D' -> 220;
            case '#', 'W', 'H' -> 240;
            case 'L', '|', 'V' -> 140;
            case 'Y' -> 120;
            default -> 100;
        };
    }

    private static String doorLabel(char tile) {
        return switch (tile) {
            case 'X' -> "security door";
            case 'D' -> "maintenance bulkhead";
            case 'L' -> "locked door";
            case 'V' -> "vent panel";
            case '|' -> "hinged door";
            default -> "door";
        };
    }

    private static String wallLabel(char tile) {
        return switch (tile) {
            case 'W' -> "structural wall";
            case 'H' -> "heavy partition";
            default -> "block wall";
        };
    }

    private static String terrainKey(GamePanel game, int x, int y) {
        return clean(game == null ? null : game.currentWorldKey(), "world")
                + ":" + x + "," + y;
    }

    private static boolean clearLine(World world, int x0, int y0,
                                     int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (!(x == x1 && y == y1)) {
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
            if (x == x1 && y == y1) return true;
            if (!world.inBounds(x, y) || blocksProjectile(world.tiles[x][y])) {
                return false;
            }
        }
        return true;
    }

    private static boolean blocksProjectile(char tile) {
        return wallTile(tile) || tile == 'X' || tile == 'D' || tile == 'L'
                || tile == '|' || tile == 'V';
    }

    private static void appendHistory(MapObjectState object, String entry) {
        String existing = MapObjectState.stockValue(object.stockState,
                "structuralDamageHistory");
        ArrayList<String> lines = new ArrayList<>();
        if (existing != null && !existing.isBlank()) {
            for (String part : existing.split("~")) {
                if (part != null && !part.isBlank()) lines.add(part.trim());
            }
        }
        lines.add(clean(entry, "structural damage"));
        while (lines.size() > 8) lines.remove(0);
        object.stockState = MapObjectState.setStockFlag(object.stockState,
                "structuralDamageHistory", String.join("~", lines));
    }

    private static String condition(int current, int maximum) {
        if (current <= 0) return "destroyed";
        int percent = current * 100 / Math.max(1, maximum);
        if (percent < 30) return "critical";
        if (percent < 60) return "damaged";
        if (percent < 85) return "worn";
        return "serviceable";
    }

    private static int ammunitionRequired(WeaponProfile weapon,
                                           String fireMode) {
        if (weapon == null || !weapon.firearm()) return 0;
        return clean(fireMode, "SNAP").equalsIgnoreCase("BURST") ? 3 : 1;
    }

    private static int distance(int x0, int y0, int x1, int y1) {
        return Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
    }

    private static TargetProfile none(int x, int y) {
        return new TargetProfile(TargetKind.NONE, "empty ground", 0, 0, 0,
                null, null, null, x, y);
    }

    private static Preview unavailable(String message) {
        WeaponProfile weapon = weaponProfile("Bare hands", "SNAP");
        return new Preview(false, false, false, 0, 0, weapon,
                none(0, 0), clean(message, "Combat targeting is unavailable."));
    }

    private static int intValue(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean contains(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? (fallback == null ? "" : fallback) : text;
    }
}

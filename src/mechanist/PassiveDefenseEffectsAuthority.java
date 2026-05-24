package mechanist;

import java.util.*;

/**
 * 0.9.09c passive defense effects bridge.
 *
 * This authority gives physical defensive entities conservative, inspectable effects
 * before any autonomous turret/fire-control logic is promoted.  It is deliberately
 * table-driven and pure/static so it adds no per-frame scanning and no background
 * simulation overhead.
 */
final class PassiveDefenseEffectsAuthority {
    static final String VERSION = "0.9.09c";

    private static final Map<Character, PassiveDefenseProfile> PROFILES = new LinkedHashMap<>();

    static {
        register(new PassiveDefenseProfile('d', "Scrap Barricade", "obstruction", 2, 2, 0,
                "cheap physical obstruction; slows raiders and grants modest cover"));
        register(new PassiveDefenseProfile('S', "Sandbag Line", "cover", 5, 4, 0,
                "strong passive ballistic cover; useful as a visible roadblock or firing position"));
        register(new PassiveDefenseProfile('R', "Razor Wire Coil", "area denial", 2, 1, 0,
                "visible area denial; injury/movement hooks remain dormant until hazard authority owns them"));
        register(new PassiveDefenseProfile('W', "Reinforced Wall Panel", "hard barrier", 8, 7, 0,
                "hard physical perimeter; strongest passive raid-resistance object in this pass"));
        register(new PassiveDefenseProfile('D', "Reinforced Internal Door", "access control", 5, 4, 0,
                "interior choke-point protection and storage-room raid delay"));
        register(new PassiveDefenseProfile('Y', "Arbites Reinforced Door", "access control", 7, 5, 0,
                "precinct-grade choke-point protection; access-control hooks remain dormant"));
        register(new PassiveDefenseProfile('N', "Security Sensor Mast", "sensor", 3, 1, 2,
                "visible detection hardware; improves warning/readiness without autonomous targeting"));
        register(new PassiveDefenseProfile('P', "Precinct Defense Fixture", "precinct fixture", 2, 1, 1,
                "Arbites checkpoint furnishing and reserved service/security anchor"));
        register(new PassiveDefenseProfile('J', "Arbites Suppression Turret", "dormant turret", 4, 3, 1,
                "inspectable turret mount; no live fire until ownership, ammo, power, staffing, and hostility rules are ready"));
        register(new PassiveDefenseProfile('U', "Heavy Stub Turret", "dormant turret", 4, 3, 1,
                "weapon mount contributes passive deterrence only; autonomous targeting remains disabled"));
        register(new PassiveDefenseProfile('Z', "Noble Sentry Turret", "dormant turret", 3, 2, 1,
                "visible noble deterrent; expensive posture before live targeting exists"));
        register(new PassiveDefenseProfile('T', "Powered Defense Turret", "legacy powered turret", 6, 4, 2,
                "legacy base-defense turret; still does not use the new autonomous turret authority"));
        register(new PassiveDefenseProfile('H', "Shield Relay", "shield support", 6, 5, 1,
                "passive defensive support and existing shield mitigation support"));
        register(new PassiveDefenseProfile('a', "Alarm Trap", "alarm", 2, 0, 2,
                "warning device; strongest when armed"));
        register(new PassiveDefenseProfile('p', "Watch Post", "watch", 3, 1, 2,
                "guard platform and warning position"));
        register(new PassiveDefenseProfile('g', "Guard Barracks", "staffing", 4, 1, 1,
                "security staffing anchor and response point"));
        register(new PassiveDefenseProfile('x', "Security Cogitator Node", "coordination", 4, 1, 2,
                "security coordination node and reserved defense network anchor"));
    }

    private PassiveDefenseEffectsAuthority() {}

    private static void register(PassiveDefenseProfile p) { PROFILES.put(p.symbol, p); }

    static PassiveDefenseProfile profile(char symbol) { return PROFILES.get(symbol); }
    static boolean isPassiveDefense(char symbol) { return PROFILES.containsKey(symbol); }

    static int raidResistance(BaseObject obj) {
        if (obj == null) return 0;
        PassiveDefenseProfile p = profile(obj.symbol);
        if (p == null) return 0;
        int q = ItemQuality.tierIndex((obj.qualityName == null ? "Common" : obj.qualityName) + " defense");
        int armedBonus = obj.armed ? Math.max(0, p.sensorBonus) : 0;
        int chargeBonus = Math.max(0, Math.min(3, obj.charges));
        return Math.max(0, p.raidResistance + Math.max(0, obj.cover) + q / 2 + armedBonus + chargeBonus);
    }

    static int coverPenalty(BaseObject obj) {
        if (obj == null) return 0;
        PassiveDefenseProfile p = profile(obj.symbol);
        if (p == null) return Math.max(0, obj.cover);
        int integrityBand = obj.integrity >= 40 ? 4 : obj.integrity >= 20 ? 2 : obj.integrity > 0 ? 1 : 0;
        return Math.max(0, p.coverPenalty + Math.max(0, obj.cover) + integrityBand);
    }

    static String inspectLine(BaseObject obj) {
        if (obj == null) return "No passive defense selected.";
        PassiveDefenseProfile p = profile(obj.symbol);
        if (p == null) return "No passive defense profile registered for " + obj.name + ".";
        return p.label + " [" + p.family + "] passive cover +" + coverPenalty(obj)
                + ", raid resistance +" + raidResistance(obj)
                + ", integrity " + obj.integrity
                + ", armed=" + obj.armed
                + ". " + p.doctrine;
    }

    static void annotate(BaseObject obj) {
        if (obj == null || !isPassiveDefense(obj.symbol)) return;
        String line = inspectLine(obj);
        if (obj.description == null || obj.description.isBlank()) obj.description = line;
        else if (!obj.description.contains("Passive defense:")) obj.description += " Passive defense: " + line;
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Passive Defense Effects Authority " + VERSION + ": physical defenses now expose cover/resistance/inspection semantics without autonomous turret targeting.");
        for (PassiveDefenseProfile p : PROFILES.values()) {
            out.add(p.label + " -> family=" + p.family + ", coverBase=" + p.coverPenalty + ", raidBase=" + p.raidResistance + ", sensor=" + p.sensorBonus + ". " + p.doctrine);
        }
        return out;
    }

    static String auditSummary() {
        int cover = 0, turretDormant = 0, sensors = 0;
        for (PassiveDefenseProfile p : PROFILES.values()) {
            if (p.coverPenalty > 0) cover++;
            if (p.family.contains("turret")) turretDormant++;
            if (p.sensorBonus > 0) sensors++;
        }
        return "passiveDefenseEffects version=" + VERSION + " profiles=" + PROFILES.size()
                + " coverProfiles=" + cover + " sensorProfiles=" + sensors + " dormantTurretProfiles=" + turretDormant;
    }
}

final class PassiveDefenseProfile {
    final char symbol;
    final String label;
    final String family;
    final int raidResistance;
    final int coverPenalty;
    final int sensorBonus;
    final String doctrine;

    PassiveDefenseProfile(char symbol, String label, String family, int raidResistance, int coverPenalty, int sensorBonus, String doctrine) {
        this.symbol = symbol;
        this.label = label;
        this.family = family;
        this.raidResistance = raidResistance;
        this.coverPenalty = coverPenalty;
        this.sensorBonus = sensorBonus;
        this.doctrine = doctrine;
    }
}

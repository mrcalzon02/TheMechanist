package mechanist;

import java.util.*;

/**
 * Defensive semantic integration authority.
 *
 * Connects defensive art families to faction identity, construction recipes,
 * decomposition classes, infopedia topics, and combat statistics without
 * enabling autonomous turret targeting. Runtime combat hooks consume these
 * profiles instead of hardcoding turret/wall/barricade numbers in several places.
 */
final class DefenseSemanticIntegration {
    static final String VERSION = "0.9.10ai";

    enum DefenseKind { BARRICADE, WALL, WIRE, SENSOR, DOOR, TURRET, PRECINCT_FIXTURE }
    enum ActivationState { PASSIVE_NOW, INSPECTABLE_NOW, COMBAT_HELD, ADMIN_HELD }

    static final class DefenseProfile {
        final String id, label, assetFamily, constructionRecipe, decompositionClass, infopediaTopic, constructionBlueprint, combatRole, notes;
        final DefenseKind kind;
        final Faction faction;
        final ActivationState activation;
        final int maxIntegrity, armor, coverBonus, range, accuracy, damage, powerDraw, ammoUse, staffingNeed, heatAttention;

        DefenseProfile(String id, String label, DefenseKind kind, Faction faction, String assetFamily,
                       String constructionRecipe, String decompositionClass, String infopediaTopic,
                       String constructionBlueprint, String combatRole, ActivationState activation,
                       int maxIntegrity, int armor, int coverBonus, int range, int accuracy, int damage,
                       int powerDraw, int ammoUse, int staffingNeed, int heatAttention, String notes) {
            this.id = clean(id, "defense");
            this.label = clean(label, this.id);
            this.kind = kind == null ? DefenseKind.BARRICADE : kind;
            this.faction = faction == null ? Faction.NONE : faction;
            this.assetFamily = clean(assetFamily, "DefenseArtBase5/unassigned");
            this.constructionRecipe = clean(constructionRecipe, this.label);
            this.decompositionClass = clean(decompositionClass, "defense/general");
            this.infopediaTopic = clean(infopediaTopic, "Defenses / " + this.label);
            this.constructionBlueprint = clean(constructionBlueprint, "defense_obstruction");
            this.combatRole = clean(combatRole, "passive obstruction");
            this.activation = activation == null ? ActivationState.PASSIVE_NOW : activation;
            this.maxIntegrity = Math.max(1, maxIntegrity);
            this.armor = Math.max(0, armor);
            this.coverBonus = Math.max(0, coverBonus);
            this.range = Math.max(0, range);
            this.accuracy = Math.max(0, accuracy);
            this.damage = Math.max(0, damage);
            this.powerDraw = Math.max(0, powerDraw);
            this.ammoUse = Math.max(0, ammoUse);
            this.staffingNeed = Math.max(0, staffingNeed);
            this.heatAttention = Math.max(0, heatAttention);
            this.notes = clean(notes, "no notes");
        }

        boolean isAutonomousTurret() { return kind == DefenseKind.TURRET && range > 0 && damage > 0; }
        String statLine() {
            return id + " | " + label + " | kind=" + kind + " faction=" + faction.label
                    + " asset=" + assetFamily + " recipe=" + constructionRecipe
                    + " decomposition=" + decompositionClass + " blueprint=" + constructionBlueprint
                    + " stats integrity=" + maxIntegrity + " armor=" + armor + " cover=" + coverBonus
                    + " range=" + range + " accuracy=" + accuracy + " damage=" + damage
                    + " power=" + powerDraw + " ammo=" + ammoUse + " staff=" + staffingNeed
                    + " activation=" + activation + " role=" + combatRole;
        }
    }

    private static final ArrayList<DefenseProfile> PROFILES = makeProfiles();

    private static ArrayList<DefenseProfile> makeProfiles() {
        ArrayList<DefenseProfile> p = new ArrayList<>();
        p.add(new DefenseProfile("scrap_barricade", "Scrap Barricade", DefenseKind.BARRICADE, Faction.NONE,
                "DefenseArtBase5/defenses/barricades", "Barricade", "defense/obstruction/scrap",
                "Defenses / Barricades", "defense_obstruction", "passive cover and movement obstruction",
                ActivationState.PASSIVE_NOW, 35, 1, 18, 0, 0, 0, 0, 0, 0, 2,
                "Existing barricade gameplay remains passive; new art supplies better semantic variants."));
        p.add(new DefenseProfile("sandbag_line", "Sandbag Line", DefenseKind.BARRICADE, Faction.IMPERIAL_GUARD,
                "DefenseArtBase5/defenses/sandbags", "Sandbag Line", "defense/obstruction/earthworks",
                "Defenses / Sandbags", "defense_obstruction", "cheap ballistic cover",
                ActivationState.PASSIVE_NOW, 28, 0, 24, 0, 0, 0, 0, 0, 0, 1,
                "Low-tech defense for Guard/PDF/civil militia layouts; cheap, visible, and disposable."));
        p.add(new DefenseProfile("razor_wire_coil", "Razor Wire Coil", DefenseKind.WIRE, Faction.ARBITES,
                "DefenseArtBase5/defenses/barbed_wire", "Razor Wire Coil", "defense/area-denial/wire",
                "Defenses / Barbed Wire", "defense_area_denial", "movement penalty and injury hazard hook",
                ActivationState.INSPECTABLE_NOW, 18, 0, 4, 0, 0, 0, 0, 0, 0, 2,
                "Inspectable and buildable; hazard effects route through the hazard authority."));
        p.add(new DefenseProfile("reinforced_wall_panel", "Reinforced Wall Panel", DefenseKind.WALL, Faction.NONE,
                "DefenseArtBase5/defenses/defensive_walls", "Reinforced Wall Panel", "defense/structure/reinforced_wall",
                "Defenses / Reinforced Walls", "defense_wall", "hard obstruction and room perimeter reinforcement",
                ActivationState.PASSIVE_NOW, 70, 4, 30, 0, 0, 0, 0, 0, 0, 3,
                "Foundation for faction wall kits, gates, precinct perimeters, and noble estate hardening."));
        p.add(new DefenseProfile("arbites_reinforced_door", "Arbites Reinforced Door", DefenseKind.DOOR, Faction.ARBITES,
                "DefenseArtBase5/arbites/precinct_doors", "Arbites Reinforced Door", "defense/access/arbites_door",
                "Adeptus Arbites / Precinct Defenses", "defense_access_control", "access choke point",
                ActivationState.PASSIVE_NOW, 65, 3, 12, 0, 0, 0, 0, 0, 0, 2,
                "Uses door/access semantics; no bespoke door logic should be created for it."));
        p.add(new DefenseProfile("security_sensor_mast", "Security Sensor Mast", DefenseKind.SENSOR, Faction.ARBITES,
                "DefenseArtBase5/defenses/sensors", "Security Sensor Mast", "defense/sensor/security",
                "Defenses / Sensors", "defense_sensor", "detection and alarm routing",
                ActivationState.INSPECTABLE_NOW, 24, 0, 0, 7, 0, 0, 1, 0, 0, 3,
                "Provides faction/security binding for alarm and line-of-sight systems."));
        p.add(new DefenseProfile("light_stub_turret", "Light Stub Turret", DefenseKind.TURRET, Faction.NONE,
                "DefenseArtBase5/defenses/turrets/light", "Light Stub Turret", "defense/turret/light_ballistic",
                "Defenses / Turrets / Light", "defense_turret", "short-range ballistic turret",
                ActivationState.COMBAT_HELD, 45, 2, 8, 6, 48, 8, 1, 1, 0, 5,
                "Semantic combat stats only; autonomous target acquisition remains disabled until ownership/ammo/power rules are ready."));
        p.add(new DefenseProfile("heavy_stub_turret", "Heavy Stub Turret", DefenseKind.TURRET, Faction.IMPERIAL_GUARD,
                "DefenseArtBase5/defenses/turrets/heavy", "Heavy Stub Turret", "defense/turret/heavy_ballistic",
                "Defenses / Turrets / Heavy", "defense_turret", "sustained ballistic fire lane",
                ActivationState.COMBAT_HELD, 70, 3, 10, 8, 42, 13, 2, 2, 1, 7,
                "Requires staffing/ammo doctrine; suited to PDF/Guard checkpoints."));
        p.add(new DefenseProfile("arbites_suppression_turret", "Arbites Suppression Turret", DefenseKind.TURRET, Faction.ARBITES,
                "DefenseArtBase5/arbites/precinct_turrets", "Arbites Suppression Turret", "defense/turret/suppression",
                "Adeptus Arbites / Suppression Turrets", "defense_turret", "lawful suppression and precinct perimeter control",
                ActivationState.COMBAT_HELD, 78, 4, 10, 7, 55, 10, 2, 1, 1, 8,
                "Faction-bound and legally unpleasant; live fire must respect ownership and hostility rules."));
        p.add(new DefenseProfile("noble_gilded_sentry", "Gilded Sentry Turret", DefenseKind.TURRET, Faction.NOBLE,
                "DefenseArtBase5/noble/defenses", "Gilded Sentry Turret", "defense/turret/noble_luxury",
                "Noble Houses / Gilded Defenses", "defense_turret", "expensive private security automation",
                ActivationState.COMBAT_HELD, 62, 2, 8, 7, 50, 11, 3, 1, 0, 9,
                "Noble estate defenses should be physical in-world assets, not invisible background modifiers."));
        p.add(new DefenseProfile("precinct_asset_set", "Precinct Defensive Fixture Set", DefenseKind.PRECINCT_FIXTURE, Faction.ARBITES,
                "DefenseArtBase5/arbites/precinct_assets", "Precinct Defensive Fixture Set", "defense/fixture/precinct",
                "Adeptus Arbites / Precinct Assets", "defense_precinct_fixture", "room fixture and checkpoint support",
                ActivationState.INSPECTABLE_NOW, 30, 1, 6, 0, 0, 0, 0, 0, 0, 2,
                "Sergeant desks, benches, holding fixtures, coffee stations, and checkpoint props bind to precinct rooms first."));
        p.add(new DefenseProfile("pdf_wall_panel", "PDF Wall Panel", DefenseKind.WALL, Faction.IMPERIAL_GUARD,
                "DefenseArtBase5/pdf_defenses/build_pdf_wall_panel", "Reinforced Wall Panel", "defense/structure/pdf_wall",
                "Astra Militarum / PDF Defenses", "defense_wall", "field-issue perimeter reinforcement",
                ActivationState.PASSIVE_NOW, 68, 4, 28, 0, 0, 0, 0, 0, 0, 3,
                "Maps PDF wall art to the existing reinforced-wall lane without a parallel defense economy."));
        p.add(new DefenseProfile("pdf_gate", "PDF Checkpoint Gate", DefenseKind.DOOR, Faction.IMPERIAL_GUARD,
                "DefenseArtBase5/pdf_defenses/build_pdf_gate", "Reinforced Door", "defense/access/pdf_gate",
                "Astra Militarum / PDF Defenses", "defense_access_control", "field checkpoint access control",
                ActivationState.PASSIVE_NOW, 62, 3, 12, 0, 0, 0, 0, 0, 0, 2,
                "Maps PDF gate art to existing reinforced-door semantics; no bespoke door system."));
        p.add(new DefenseProfile("pdf_sandbag_barricade", "PDF Sandbag Barricade", DefenseKind.BARRICADE, Faction.IMPERIAL_GUARD,
                "DefenseArtBase5/pdf_defenses/build_sandbag_barricade", "Sandbag Line", "defense/obstruction/earthworks",
                "Astra Militarum / PDF Defenses", "defense_obstruction", "cheap ballistic cover",
                ActivationState.PASSIVE_NOW, 30, 0, 25, 0, 0, 0, 0, 0, 0, 1,
                "Maps the field sandbag art to the existing sandbag-line build lane."));
        p.add(new DefenseProfile("pdf_turret_mk1", "PDF Turret Mk I", DefenseKind.TURRET, Faction.IMPERIAL_GUARD,
                "DefenseArtBase5/pdf_defenses/build_pdf_turret_mk1", "Heavy Stub Turret", "defense/turret/pdf_field",
                "Astra Militarum / PDF Defenses", "defense_turret", "dormant field turret mount",
                ActivationState.COMBAT_HELD, 65, 3, 9, 7, 40, 11, 2, 2, 1, 6,
                "Readable turret mount only; ownership, ammo, power, staffing, and hostility rules still gate live fire."));
        return p;
    }

    static List<DefenseProfile> profiles() { return Collections.unmodifiableList(PROFILES); }
    static DefenseProfile byRecipe(String recipeName) {
        String r = clean(recipeName, "").toLowerCase(Locale.ROOT);
        for (DefenseProfile p : PROFILES) if (p.constructionRecipe.toLowerCase(Locale.ROOT).equals(r)) return p;
        return null;
    }
    static boolean isDefenseRecipe(String recipeName) { return byRecipe(recipeName) != null; }
    static String blueprintForRecipe(String recipeName) {
        DefenseProfile p = byRecipe(recipeName);
        return p == null ? null : p.constructionBlueprint;
    }
    static ArrayList<String> constructionRecipeNames() {
        ArrayList<String> out = new ArrayList<>();
        for (DefenseProfile p : PROFILES) if (!out.contains(p.constructionRecipe)) out.add(p.constructionRecipe);
        return out;
    }

    static DefenseProfile byLabel(String label) {
        String key = clean(label, "").toLowerCase(Locale.ROOT);
        for (DefenseProfile p : PROFILES) if (p.label.toLowerCase(Locale.ROOT).equals(key) || p.id.toLowerCase(Locale.ROOT).equals(key)) return p;
        return null;
    }
    static ArrayList<String> labels() {
        ArrayList<String> out = new ArrayList<>();
        for (DefenseProfile p : PROFILES) out.add(p.label);
        return out;
    }
    static ArrayList<String> detailLines(String label) {
        ArrayList<String> lines = new ArrayList<>();
        DefenseProfile p = byLabel(label);
        if (p == null) {
            lines.add("Defense profile not found: " + clean(label, "unknown"));
            lines.addAll(infopediaLines());
            return lines;
        }
        lines.add(p.label);
        lines.add("Semantic ID: " + p.id);
        lines.add("Kind: " + p.kind + " | Faction binding: " + p.faction.label + " | Activation: " + p.activation);
        lines.add("Asset family: " + p.assetFamily);
        lines.add("Construction recipe: " + p.constructionRecipe + " | Blueprint: " + p.constructionBlueprint);
        lines.add("Decomposition class: " + p.decompositionClass);
        lines.add("Combat role: " + p.combatRole);
        lines.add("Stats: integrity " + p.maxIntegrity + ", armor " + p.armor + ", cover " + p.coverBonus + ", range " + p.range + ", accuracy " + p.accuracy + ", damage " + p.damage + ".");
        lines.add("Operation costs/hooks: power " + p.powerDraw + ", ammo " + p.ammoUse + ", staffing " + p.staffingNeed + ", heat/attention " + p.heatAttention + ".");
        lines.add("Implementation boundary: passive/buildable/inspectable data is allowed now; autonomous live combat waits for ownership, ammo, power, staffing, hostility, save/load, and multiplayer-safe authority.");
        lines.add("Notes: " + p.notes);
        return lines;
    }
    static ArrayList<String> infopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Defense semantic integration " + VERSION + ": art -> entity profile -> construction recipe -> decomposition class -> faction binding -> infopedia/stat line.");
        lines.add("Live turret combat remains inactive unless ownership, ammo, power, staffing, hostility, and save/load authority explicitly allow it.");
        for (DefenseProfile p : PROFILES) lines.add(p.statLine() + " :: " + p.notes);
        return lines;
    }
    static String auditSummary() {
        int turrets = 0, passive = 0, combatLater = 0;
        TreeSet<String> factions = new TreeSet<>();
        for (DefenseProfile p : PROFILES) {
            if (p.kind == DefenseKind.TURRET) turrets++;
            if (p.activation == ActivationState.PASSIVE_NOW || p.activation == ActivationState.INSPECTABLE_NOW) passive++;
            if (p.activation == ActivationState.COMBAT_HELD) combatLater++;
            factions.add(p.faction.label);
        }
        return "defenseSemantic version=" + VERSION + " profiles=" + PROFILES.size()
                + " recipes=" + constructionRecipeNames().size() + " turrets=" + turrets
                + " passiveOrInspectable=" + passive + " combatLater=" + combatLater
                + " factions=" + factions;
    }
    private static String clean(String s, String fallback) {
        if (s == null) return fallback;
        String v = s.trim();
        return v.isEmpty() ? fallback : v;
    }
}

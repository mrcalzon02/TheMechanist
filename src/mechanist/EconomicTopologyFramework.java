package mechanist;

import java.util.*;

final class EconomicTopologyFramework {
    static final String VERSION = "0.9.10em";

    enum PressureType {
        INDUSTRIAL("industrial"),
        LOGISTICS("logistics"),
        LABOR("labor"),
        SECURITY("security"),
        POLLUTION("pollution"),
        RELIGIOUS("religious"),
        DECAY("decay"),
        BLACK_MARKET("black market");
        final String label;
        PressureType(String label) { this.label = label; }
    }

    enum CirculationClass {
        PUBLIC_SERVICE_SPINE("public service spine"),
        FREIGHT_ARTERY("freight artery"),
        CARGO_CORRIDOR("cargo corridor"),
        INDUSTRIAL_SERVICE_LOOP("industrial service loop"),
        MAINTENANCE_TUNNEL("maintenance tunnel"),
        SEWER_TRUNK("sewer trunk"),
        NOBLE_BOULEVARD("noble service boulevard"),
        SHRINE_PILGRIMAGE_ROUTE("shrine pilgrimage route"),
        BARRACKS_ACCESS_GRID("barracks access grid"),
        HIDDEN_BYPASS("hidden bypass"),
        HAB_BRANCH_CORRIDOR("hab branch corridor"),
        ADMINISTRATIVE_QUEUE_LINE("administrative queue line");
        final String label;
        CirculationClass(String label) { this.label = label; }
    }

    enum InfrastructureAgeBand {
        FOUNDATIONAL("foundational civic layer"),
        INHERITED("inherited industrial layer"),
        MAINTAINED("maintained active layer"),
        PATCHED("patched survival layer"),
        DEGRADED("degraded legacy layer"),
        FORBIDDEN("forbidden hidden layer");
        final String label;
        InfrastructureAgeBand(String label) { this.label = label; }
    }

    enum ZonePurpose {
        CIVILIAN_MIXED_INDUSTRIAL("civilian mixed-industrial district"),
        SALVAGE_DISTRICT("salvage and reclamation district"),
        HAB_LABOR_RESERVOIR("hab labor reservoir"),
        SUMP_MARKET_EXCHANGE("sump market exchange district"),
        CRIMINAL_PRODUCTION_TURF("criminal production and smuggling turf"),
        LAW_SECURITY_NODE("law and security node"),
        MECHANICUS_RELIC_SERVICE("Mechanicus relic-service district"),
        BIOLOGICAL_EXILE_CAMP("biological exile survival camp"),
        CULT_RITUAL_HIDEOUT("cult ritual logistics hideout"),
        UTILITY_SEWER_SPINE("utility sewer spine"),
        FREIGHT_AND_TRANSIT_DISTRICT("freight and transit district"),
        ADMINISTRATIVE_CONTROL_ARCHIVE("administrative control archive"),
        PDF_LOGISTICS_BILLET("PDF logistics and billet district"),
        FORGE_CLOISTER("forge cloister production district"),
        NOBLE_COMMAND_ESTATE("noble command estate"),
        NOBLE_SERVICE_BACKBONE("noble service backbone"),
        SANCTIONED_MEDIA_BUREAU("sanctioned media and distribution bureau"),
        RAIL_DEPOT_INTERCHANGE("rail depot interchange");
        final String label;
        ZonePurpose(String label) { this.label = label; }
    }

    static final class PressureProfile {
        final EnumMap<PressureType, Integer> values;
        PressureProfile(int industrial, int logistics, int labor, int security, int pollution, int religious, int decay, int blackMarket) {
            values = new EnumMap<>(PressureType.class);
            values.put(PressureType.INDUSTRIAL, clamp(industrial));
            values.put(PressureType.LOGISTICS, clamp(logistics));
            values.put(PressureType.LABOR, clamp(labor));
            values.put(PressureType.SECURITY, clamp(security));
            values.put(PressureType.POLLUTION, clamp(pollution));
            values.put(PressureType.RELIGIOUS, clamp(religious));
            values.put(PressureType.DECAY, clamp(decay));
            values.put(PressureType.BLACK_MARKET, clamp(blackMarket));
        }
        int get(PressureType t) { return values.getOrDefault(t, 0); }
        String compactLine() {
            ArrayList<String> parts = new ArrayList<>();
            for (PressureType t : PressureType.values()) parts.add(t.label + " " + get(t));
            return String.join(", ", parts);
        }
        ArrayList<String> dominantLines(int limit) {
            ArrayList<PressureType> ordered = new ArrayList<>(Arrays.asList(PressureType.values()));
            ordered.sort((a, b) -> Integer.compare(get(b), get(a)));
            ArrayList<String> out = new ArrayList<>();
            for (int i = 0; i < Math.min(Math.max(1, limit), ordered.size()); i++) {
                PressureType t = ordered.get(i);
                out.add(t.label + " pressure: " + get(t) + "/10");
            }
            return out;
        }
    }

    static final class FactionDoctrine {
        final Faction faction;
        final String doctrineName;
        final String industrialRole;
        final ArrayList<String> imports;
        final ArrayList<String> exports;
        final ArrayList<String> preferredRooms;
        final ArrayList<String> infrastructure;
        final ArrayList<CirculationClass> circulation;
        final PressureProfile pressure;
        FactionDoctrine(Faction faction, String doctrineName, String industrialRole, String[] imports, String[] exports, String[] preferredRooms, String[] infrastructure, CirculationClass[] circulation, PressureProfile pressure) {
            this.faction = faction;
            this.doctrineName = doctrineName;
            this.industrialRole = industrialRole;
            this.imports = list(imports);
            this.exports = list(exports);
            this.preferredRooms = list(preferredRooms);
            this.infrastructure = list(infrastructure);
            this.circulation = new ArrayList<>(Arrays.asList(circulation));
            this.pressure = pressure;
        }
        String circulationLine() {
            ArrayList<String> parts = new ArrayList<>();
            for (CirculationClass c : circulation) parts.add(c.label);
            return String.join(", ", parts);
        }
    }

    static final class ZonePurposeProfile {
        final ZoneType zone;
        final ZonePurpose purpose;
        final Faction controllingBias;
        final InfrastructureAgeBand ageBand;
        final ArrayList<CirculationClass> circulation;
        final PressureProfile pressure;
        final String identity;
        final String nextDependency;
        ZonePurposeProfile(ZoneType zone, ZonePurpose purpose, Faction controllingBias, InfrastructureAgeBand ageBand, CirculationClass[] circulation, PressureProfile pressure, String identity, String nextDependency) {
            this.zone = zone;
            this.purpose = purpose;
            this.controllingBias = controllingBias;
            this.ageBand = ageBand;
            this.circulation = new ArrayList<>(Arrays.asList(circulation));
            this.pressure = pressure;
            this.identity = identity;
            this.nextDependency = nextDependency;
        }
        String circulationLine() {
            ArrayList<String> parts = new ArrayList<>();
            for (CirculationClass c : circulation) parts.add(c.label);
            return String.join(", ", parts);
        }
    }

    private static final EnumMap<ZoneType, ZonePurposeProfile> ZONE_PURPOSES = buildZonePurposeProfiles();
    private static final ArrayList<FactionDoctrine> DOCTRINES = buildDoctrineProfiles();

    static ZonePurposeProfile profileFor(ZoneType zone) {
        ZonePurposeProfile p = ZONE_PURPOSES.get(zone);
        if (p != null) return p;
        return new ZonePurposeProfile(zone, ZonePurpose.CIVILIAN_MIXED_INDUSTRIAL, Faction.HIVER, InfrastructureAgeBand.PATCHED,
                new CirculationClass[]{CirculationClass.HAB_BRANCH_CORRIDOR, CirculationClass.PUBLIC_SERVICE_SPINE},
                new PressureProfile(3, 3, 5, 2, 2, 2, 4, 2),
                "Unclassified local district with mixed labor, small services, and opportunistic salvage.",
                "Needs explicit zone-purpose tuning before conversion, demand, or route-shaping systems read it.");
    }

    static FactionDoctrine doctrineFor(Faction faction) {
        if (faction == null) return doctrineByFaction(Faction.NONE);
        FactionDoctrine exact = doctrineByFaction(faction);
        if (exact != null) return exact;
        String n = faction.name();
        if (n.contains("MECHANICUS")) return doctrineByFaction(Faction.MECHANICUS);
        if (n.contains("GANGER") || faction == Faction.BANDIT) return doctrineByFaction(Faction.BANDIT);
        if (n.contains("NOBLE") || faction == Faction.NOBLE) return doctrineByFaction(Faction.NOBLE);
        if (n.contains("HIVER") || faction == Faction.HIVER) return doctrineByFaction(Faction.HIVER);
        return doctrineByFaction(Faction.NONE);
    }

    private static FactionDoctrine doctrineByFaction(Faction faction) {
        for (FactionDoctrine d : DOCTRINES) if (d.faction == faction) return d;
        return null;
    }

    static ArrayList<String> zonePurposeLines(ZoneType zone) {
        ArrayList<String> lines = new ArrayList<>();
        ZonePurposeProfile p = profileFor(zone);
        lines.add("");
        lines.add("Economic topology profile");
        lines.add("Purpose identity: " + p.purpose.label + ".");
        lines.add("Controlling bias: " + p.controllingBias.label + " | Infrastructure age: " + p.ageBand.label + ".");
        lines.add("Circulation class: " + p.circulationLine() + ".");
        lines.add("Pressure field: " + p.pressure.compactLine() + ".");
        lines.add("Dominant pressures: " + String.join("; ", p.pressure.dominantLines(3)) + ".");
        lines.add("Purpose reading: " + p.identity);
        lines.add("Operational boundary: " + p.nextDependency);
        return lines;
    }

    static ArrayList<String> factionDoctrineLines(Faction faction) {
        ArrayList<String> lines = new ArrayList<>();
        FactionDoctrine d = doctrineFor(faction);
        if (d == null) return lines;
        lines.add("");
        lines.add("Industrial doctrine profile");
        lines.add("Doctrine: " + d.doctrineName + ".");
        lines.add("Industrial role: " + d.industrialRole);
        lines.add("Strategic imports: " + String.join(", ", d.imports) + ".");
        lines.add("Strategic exports: " + String.join(", ", d.exports) + ".");
        lines.add("Preferred rooms: " + String.join(", ", d.preferredRooms) + ".");
        lines.add("Preferred infrastructure: " + String.join(", ", d.infrastructure) + ".");
        lines.add("Circulation preference: " + d.circulationLine() + ".");
        lines.add("Doctrine pressure bias: " + d.pressure.compactLine() + ".");
        return lines;
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Economic Topology Framework " + VERSION);
        lines.add("Purpose: assigns faction doctrine, pressure-field, circulation-class, infrastructure-age, and zone-purpose metadata without running a live economy.");
        lines.add("Use: generation, maps, inspections, and ledger systems can query the same authority instead of inventing separate PDF, forge, noble, sewer, market, and ganger interpretations.");
        lines.add("Runtime rule: these profiles classify zones and factions only; they do not mutate rooms, spawn labor, reserve goods, or run district conversion.");
        lines.add("");
        lines.add("Schema counts: doctrines " + DOCTRINES.size() + ", zone profiles " + ZONE_PURPOSES.size() + ", pressure types " + PressureType.values().length + ", circulation classes " + CirculationClass.values().length + ", age bands " + InfrastructureAgeBand.values().length + ".");
        lines.add("");
        lines.add("Zone-purpose samples:");
        for (ZoneType z : ZoneType.values()) {
            ZonePurposeProfile p = profileFor(z);
            lines.add("- " + z.label + ": " + p.purpose.label + "; " + p.circulationLine() + "; pressures " + p.pressure.compactLine() + ".");
        }
        return lines;
    }

    static ArrayList<String> auditLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(auditSummary());
        lines.add("Authority: EconomicTopologyFramework owns Phase 3.5 doctrine and topology-classification vocabulary.");
        lines.add("No live simulation hooks: room conversion, route widening, faction labor, market stock changes, district decay, and demand propagation remain separate consumers of this data.");
        lines.add("Coverage check: each current ZoneType has a purpose profile; major faction families route to a doctrine profile through exact or family matching.");
        lines.add("Pressure vocabulary: " + pressureVocabularyLine() + ".");
        lines.add("Circulation vocabulary: " + circulationVocabularyLine() + ".");
        return lines;
    }

    static String auditSummary() {
        int mappedZones = 0;
        for (ZoneType z : ZoneType.values()) if (ZONE_PURPOSES.containsKey(z)) mappedZones++;
        TreeSet<String> purposes = new TreeSet<>();
        TreeSet<String> circulation = new TreeSet<>();
        for (ZonePurposeProfile p : ZONE_PURPOSES.values()) {
            purposes.add(p.purpose.label);
            for (CirculationClass c : p.circulation) circulation.add(c.label);
        }
        return "economicTopology version=" + VERSION + " doctrines=" + DOCTRINES.size()
                + " zoneProfiles=" + mappedZones + "/" + ZoneType.values().length
                + " purposes=" + purposes.size() + " circulationClassesUsed=" + circulation.size()
                + " liveEconomy=false";
    }

    private static String pressureVocabularyLine() {
        ArrayList<String> out = new ArrayList<>();
        for (PressureType t : PressureType.values()) out.add(t.label);
        return String.join(", ", out);
    }

    private static String circulationVocabularyLine() {
        ArrayList<String> out = new ArrayList<>();
        for (CirculationClass c : CirculationClass.values()) out.add(c.label);
        return String.join(", ", out);
    }

    private static EnumMap<ZoneType, ZonePurposeProfile> buildZonePurposeProfiles() {
        EnumMap<ZoneType, ZonePurposeProfile> m = new EnumMap<>(ZoneType.class);
        put(m, ZoneType.NEUTRAL_CIVILIAN_FLOOR, ZonePurpose.CIVILIAN_MIXED_INDUSTRIAL, Faction.HIVER, InfrastructureAgeBand.PATCHED,
                new CirculationClass[]{CirculationClass.PUBLIC_SERVICE_SPINE, CirculationClass.HAB_BRANCH_CORRIDOR},
                new PressureProfile(3, 4, 6, 2, 2, 3, 4, 2),
                "A local population floor where labor supply, small services, ration movement, and civic clutter have to coexist.",
                "This profile exposes labor and service pressure without requiring every civilian room to be simulated.");
        put(m, ZoneType.TRASH_WARREN, ZonePurpose.SALVAGE_DISTRICT, Faction.SCAVENGER, InfrastructureAgeBand.DEGRADED,
                new CirculationClass[]{CirculationClass.MAINTENANCE_TUNNEL, CirculationClass.HIDDEN_BYPASS},
                new PressureProfile(4, 2, 4, 2, 7, 1, 8, 5),
                "A reclamation ecology where collapse, scrap, refuse, informal labor, and unlicensed movement become the industrial base.",
                "This profile exposes salvage as a resource source with high decay and black-market drag.");
        put(m, ZoneType.HAB_STACK, ZonePurpose.HAB_LABOR_RESERVOIR, Faction.HIVER, InfrastructureAgeBand.PATCHED,
                new CirculationClass[]{CirculationClass.HAB_BRANCH_CORRIDOR, CirculationClass.PUBLIC_SERVICE_SPINE},
                new PressureProfile(2, 3, 8, 2, 2, 3, 5, 2),
                "Dense habitation that feeds bodies into nearby factories, markets, stations, barracks, and service corridors.",
                "This profile exposes labor pressure without requiring bedroom-by-bedroom scans.");
        put(m, ZoneType.SUMP_MARKET, ZonePurpose.SUMP_MARKET_EXCHANGE, Faction.HIVER, InfrastructureAgeBand.PATCHED,
                new CirculationClass[]{CirculationClass.PUBLIC_SERVICE_SPINE, CirculationClass.CARGO_CORRIDOR, CirculationClass.HIDDEN_BYPASS},
                new PressureProfile(3, 7, 5, 3, 3, 2, 4, 6),
                "A market exchange zone where legal services, barter, off-ledger goods, and crowd movement all fight over the same corridors.",
                "This profile exposes a local exchange node with both lawful and unlawful routing hooks.");
        put(m, ZoneType.GANGER_TURF, ZonePurpose.CRIMINAL_PRODUCTION_TURF, Faction.BANDIT, InfrastructureAgeBand.FORBIDDEN,
                new CirculationClass[]{CirculationClass.HIDDEN_BYPASS, CirculationClass.MAINTENANCE_TUNNEL},
                new PressureProfile(4, 5, 3, 7, 4, 1, 5, 9),
                "A territorial criminal industry: hidden rooms, coercive trade, stolen stock, contraband craft, and violent access control.",
                "This profile exposes black-market and security pressure without changing district geometry.");
        put(m, ZoneType.ARBITES_PRECINCT_EDGE, ZonePurpose.LAW_SECURITY_NODE, Faction.ARBITES, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.ADMINISTRATIVE_QUEUE_LINE, CirculationClass.PUBLIC_SERVICE_SPINE},
                new PressureProfile(1, 4, 4, 9, 1, 2, 2, 1),
                "A lawful choke node where queues, cells, evidence, surveillance, and controlled counters shape civilian movement.",
                "Security and access systems can use this profile without creating a separate precinct economy.");
        put(m, ZoneType.MECHANICUS_RELIC_DUCT, ZonePurpose.MECHANICUS_RELIC_SERVICE, Faction.MECHANICUS, InfrastructureAgeBand.INHERITED,
                new CirculationClass[]{CirculationClass.MAINTENANCE_TUNNEL, CirculationClass.INDUSTRIAL_SERVICE_LOOP},
                new PressureProfile(7, 5, 3, 4, 5, 6, 6, 1),
                "A machine-service layer where old hardware, inherited ducts, restricted relic equipment, and maintenance doctrine define the terrain.",
                "This profile distinguishes inherited machine corridors from maintained production halls.");
        put(m, ZoneType.MUTANT_WARRENS, ZonePurpose.BIOLOGICAL_EXILE_CAMP, Faction.MUTANT, InfrastructureAgeBand.DEGRADED,
                new CirculationClass[]{CirculationClass.HIDDEN_BYPASS, CirculationClass.MAINTENANCE_TUNNEL},
                new PressureProfile(1, 2, 3, 6, 6, 1, 8, 4),
                "A collapsed survival ecology with biological risk, improvised dens, territorial defense, and little formal logistics.",
                "Hazard and hostile ecology systems can read this profile before any population simulation treats it as a settlement.");
        put(m, ZoneType.MUTANT_SEWER_CAMP, ZonePurpose.BIOLOGICAL_EXILE_CAMP, Faction.MUTANT, InfrastructureAgeBand.DEGRADED,
                new CirculationClass[]{CirculationClass.SEWER_TRUNK, CirculationClass.HIDDEN_BYPASS},
                new PressureProfile(1, 2, 3, 7, 8, 1, 8, 4),
                "A sewer-bound exile camp where contamination, ambush routes, and survival adaptation dominate formal district purpose.",
                "Sewer hazard, disease, and hostile-control systems can query this identity without creating full sewer demographics.");
        put(m, ZoneType.CULTIST_SEWER_CAMP, ZonePurpose.CULT_RITUAL_HIDEOUT, Faction.CULTIST, InfrastructureAgeBand.FORBIDDEN,
                new CirculationClass[]{CirculationClass.SEWER_TRUNK, CirculationClass.HIDDEN_BYPASS, CirculationClass.SHRINE_PILGRIMAGE_ROUTE},
                new PressureProfile(1, 3, 2, 6, 7, 9, 7, 6),
                "A hidden ritual logistics site where sewer access, illegal congregation, sacrificial clutter, and social danger overlap.",
                "Religious pressure here means forbidden ritual gravity, not lawful Ecclesiarchy service.");
        put(m, ZoneType.SEWER_CONDUIT, ZonePurpose.UTILITY_SEWER_SPINE, Faction.NONE, InfrastructureAgeBand.FOUNDATIONAL,
                new CirculationClass[]{CirculationClass.SEWER_TRUNK, CirculationClass.MAINTENANCE_TUNNEL},
                new PressureProfile(2, 6, 1, 2, 8, 0, 7, 5),
                "A utility circulation spine whose industrial purpose is movement of water, waste, maintenance crews, and unwanted traffic.",
                "This profile distinguishes sewer trunks from ordinary corridors before route calculation becomes expensive.");
        put(m, ZoneType.TRAIN_SERVICE_YARD, ZonePurpose.FREIGHT_AND_TRANSIT_DISTRICT, Faction.NONE, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.FREIGHT_ARTERY, CirculationClass.CARGO_CORRIDOR},
                new PressureProfile(4, 9, 4, 4, 4, 1, 3, 3),
                "A cargo handling zone where platforms, freight cages, workrooms, and track adjacency make logistics the primary identity.",
                "This profile marks the zone as a throughput amplifier and congestion risk.");
        put(m, ZoneType.ADMINISTRATUM_ARCHIVE, ZonePurpose.ADMINISTRATIVE_CONTROL_ARCHIVE, Faction.ADMINISTRATUM, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.ADMINISTRATIVE_QUEUE_LINE, CirculationClass.PUBLIC_SERVICE_SPINE},
                new PressureProfile(1, 5, 6, 5, 1, 2, 3, 1),
                "A paperwork and records machine that converts labor and access into permits, queues, delays, and control.",
                "This profile binds the zone to access, permission, and reputation pressure without simulating every clerk.");
        put(m, ZoneType.IMPERIAL_GUARD_BILLET, ZonePurpose.PDF_LOGISTICS_BILLET, Faction.IMPERIAL_GUARD, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.BARRACKS_ACCESS_GRID, CirculationClass.CARGO_CORRIDOR},
                new PressureProfile(4, 7, 6, 8, 3, 2, 2, 1),
                "A military support base where bunks, rations, stores, armory control, medical support, and vehicle-side logistics justify the district.",
                "This is the schema hook for turning military zones into industrial consumers rather than static barracks maps.");
        put(m, ZoneType.MECHANICUS_FORGE_CLOISTER, ZonePurpose.FORGE_CLOISTER, Faction.MECHANICUS, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.INDUSTRIAL_SERVICE_LOOP, CirculationClass.FREIGHT_ARTERY, CirculationClass.MAINTENANCE_TUNNEL},
                new PressureProfile(9, 7, 5, 5, 7, 7, 3, 1),
                "A production cloister where metallurgy, machine maintenance, sanctified work cells, and cargo movement are the reason the rooms exist.",
                "This profile marks the zone as a production amplifier without activating district automation.");
        put(m, ZoneType.SECTOR_GOVERNORS_MANSION, ZonePurpose.NOBLE_COMMAND_ESTATE, Faction.NOBLE, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.NOBLE_BOULEVARD, CirculationClass.ADMINISTRATIVE_QUEUE_LINE},
                new PressureProfile(1, 5, 7, 9, 1, 4, 1, 3),
                "A command estate where access, servants, security, luxury consumption, and political gravity shape topology.",
                "This profile marks the estate as a demand sink and permission fortress, not an ordinary residence.");
        put(m, ZoneType.NOBLE_SERVICE_SPINE, ZonePurpose.NOBLE_SERVICE_BACKBONE, Faction.NOBLE, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.NOBLE_BOULEVARD, CirculationClass.CARGO_CORRIDOR, CirculationClass.HIDDEN_BYPASS},
                new PressureProfile(2, 6, 8, 7, 2, 3, 2, 4),
                "A servant and supply backbone feeding elite households while hiding labor, waste, deliveries, and household logistics from noble sight.",
                "This profile separates servant traffic pressure from public and noble circulation.");
        put(m, ZoneType.IMPERIAL_NEWS_NETWORK, ZonePurpose.SANCTIONED_MEDIA_BUREAU, Faction.INN, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.PUBLIC_SERVICE_SPINE, CirculationClass.ADMINISTRATIVE_QUEUE_LINE},
                new PressureProfile(2, 5, 5, 5, 1, 4, 2, 2),
                "A media and distribution bureau where message control, paper stock, public counters, and sanctioned narratives define the facility.",
                "Public-service systems can use this as a civic information node before propaganda or faction morale loops activate.");
        put(m, ZoneType.NEUTRAL_RAIL_DEPOT, ZonePurpose.RAIL_DEPOT_INTERCHANGE, Faction.NONE, InfrastructureAgeBand.MAINTAINED,
                new CirculationClass[]{CirculationClass.FREIGHT_ARTERY, CirculationClass.PUBLIC_SERVICE_SPINE, CirculationClass.CARGO_CORRIDOR},
                new PressureProfile(3, 9, 5, 5, 3, 1, 3, 4),
                "A public transit and freight interchange where cargo, passengers, tickets, trade counters, and watchers converge.",
                "This profile marks the depot as a junction between selected-context travel, freight abstraction, and market routing.");
        return m;
    }

    private static void put(EnumMap<ZoneType, ZonePurposeProfile> m, ZoneType zone, ZonePurpose purpose, Faction faction, InfrastructureAgeBand ageBand, CirculationClass[] circulation, PressureProfile pressure, String identity, String nextDependency) {
        m.put(zone, new ZonePurposeProfile(zone, purpose, faction, ageBand, circulation, pressure, identity, nextDependency));
    }

    private static ArrayList<FactionDoctrine> buildDoctrineProfiles() {
        ArrayList<FactionDoctrine> out = new ArrayList<>();
        out.add(new FactionDoctrine(Faction.NONE, "Unowned civic background", "uses local infrastructure without strategic industrial doctrine",
                new String[]{"ambient labor", "local salvage"}, new String[]{"open access", "generic traffic"},
                new String[]{"public corridors", "mixed rooms"}, new String[]{"service spines", "shared corridors"},
                new CirculationClass[]{CirculationClass.PUBLIC_SERVICE_SPINE, CirculationClass.HAB_BRANCH_CORRIDOR},
                new PressureProfile(2, 3, 3, 1, 2, 1, 3, 1)));
        out.add(new FactionDoctrine(Faction.MECHANICUS, "Forge-cloister industrial doctrine", "dominates metallurgy, machine parts, maintenance rites, refining, and technical infrastructure",
                new String[]{"ore", "fuel", "scrap", "chemicals", "skilled labor"}, new String[]{"machine parts", "tools", "processed materials", "maintenance service"},
                new String[]{"forge cells", "machine chapels", "relay rooms", "refineries"}, new String[]{"industrial loops", "freight arteries", "maintenance tunnels"},
                new CirculationClass[]{CirculationClass.INDUSTRIAL_SERVICE_LOOP, CirculationClass.FREIGHT_ARTERY, CirculationClass.MAINTENANCE_TUNNEL},
                new PressureProfile(9, 7, 5, 5, 7, 8, 3, 1)));
        out.add(new FactionDoctrine(Faction.IMPERIAL_GUARD, "PDF logistics doctrine", "consumes mass supplies to maintain billets, armories, rations, vehicle repair, and military readiness",
                new String[]{"rations", "water", "munitions", "armor plates", "medical stock"}, new String[]{"security", "controlled access", "surplus field goods"},
                new String[]{"barracks", "armories", "ration stores", "vehicle bays"}, new String[]{"barracks grids", "cargo corridors", "checkpoint lanes"},
                new CirculationClass[]{CirculationClass.BARRACKS_ACCESS_GRID, CirculationClass.CARGO_CORRIDOR, CirculationClass.FREIGHT_ARTERY},
                new PressureProfile(4, 8, 6, 9, 3, 2, 2, 1)));
        out.add(new FactionDoctrine(Faction.ARBITES, "Precinct control doctrine", "turns corridors, counters, cells, and records into lawful choke points",
                new String[]{"paperwork", "evidence", "restraints", "armor", "rations"}, new String[]{"permission pressure", "detention", "lawful coercion"},
                new String[]{"holding cells", "evidence rooms", "public counters", "watch posts"}, new String[]{"queue lines", "controlled doors", "security desks"},
                new CirculationClass[]{CirculationClass.ADMINISTRATIVE_QUEUE_LINE, CirculationClass.PUBLIC_SERVICE_SPINE},
                new PressureProfile(1, 4, 5, 10, 1, 2, 2, 1)));
        out.add(new FactionDoctrine(Faction.NOBLE, "Estate consumption doctrine", "imports labor and goods to produce status, access control, luxury service, and political command",
                new String[]{"servants", "clean water", "luxury food", "permits", "security"}, new String[]{"patronage", "restricted access", "elite contracts", "luxury waste"},
                new String[]{"service halls", "salons", "vaults", "guard rooms"}, new String[]{"noble boulevards", "servant spines", "private cargo corridors"},
                new CirculationClass[]{CirculationClass.NOBLE_BOULEVARD, CirculationClass.CARGO_CORRIDOR, CirculationClass.HIDDEN_BYPASS},
                new PressureProfile(2, 6, 8, 9, 1, 4, 1, 4)));
        out.add(new FactionDoctrine(Faction.HIVER, "Civilian survival doctrine", "turns crowded habitation and small services into labor supply, repair culture, and everyday trade",
                new String[]{"water", "food", "cheap tools", "permits", "work access"}, new String[]{"labor", "local goods", "repair work", "gossip"},
                new String[]{"hab rooms", "kitchens", "repair nooks", "markets"}, new String[]{"public spines", "hab branch corridors", "shared utility rooms"},
                new CirculationClass[]{CirculationClass.PUBLIC_SERVICE_SPINE, CirculationClass.HAB_BRANCH_CORRIDOR},
                new PressureProfile(3, 4, 8, 2, 2, 3, 4, 2)));
        out.add(new FactionDoctrine(Faction.BANDIT, "Black-market turf doctrine", "uses hidden access, coercion, contraband craft, stolen stock, and violence to create an illegal industrial layer",
                new String[]{"stolen goods", "weapons", "chemicals", "desperate labor"}, new String[]{"contraband", "protection", "black-market routing", "heat"},
                new String[]{"stash rooms", "back shops", "ambush corridors", "drug kitchens"}, new String[]{"hidden bypasses", "maintenance tunnels", "false fronts"},
                new CirculationClass[]{CirculationClass.HIDDEN_BYPASS, CirculationClass.MAINTENANCE_TUNNEL},
                new PressureProfile(4, 5, 3, 7, 4, 1, 5, 10)));
        out.add(new FactionDoctrine(Faction.ADMINISTRATUM, "Bureaucratic control doctrine", "converts queues, files, forms, stamped permissions, and clerical labor into access control",
                new String[]{"paper", "clerks", "ink", "security", "petitioners"}, new String[]{"permits", "records", "delays", "legal gates"},
                new String[]{"archives", "counter halls", "file cages", "permit offices"}, new String[]{"queue lines", "public counters", "locked records stacks"},
                new CirculationClass[]{CirculationClass.ADMINISTRATIVE_QUEUE_LINE, CirculationClass.PUBLIC_SERVICE_SPINE},
                new PressureProfile(1, 5, 7, 5, 1, 2, 3, 1)));
        out.add(new FactionDoctrine(Faction.MINISTORUM, "Cult Imperialis service doctrine", "anchors lawful ritual, charity, supplication, temple traffic, and civilian legitimacy",
                new String[]{"candles", "food", "water", "pilgrims", "security"}, new String[]{"forgiveness service", "charity", "social legitimacy", "religious gravity"},
                new String[]{"temples", "kitchens", "chapels", "relic alcoves"}, new String[]{"pilgrimage routes", "public service spines", "guarded nave access"},
                new CirculationClass[]{CirculationClass.SHRINE_PILGRIMAGE_ROUTE, CirculationClass.PUBLIC_SERVICE_SPINE},
                new PressureProfile(1, 4, 5, 4, 1, 10, 2, 1)));
        out.add(new FactionDoctrine(Faction.SCAVENGER, "Reclamation doctrine", "extracts value from decay, refuse, abandoned rooms, and marginal routes",
                new String[]{"trash", "scrap", "dirty water", "broken tools"}, new String[]{"salvage", "patched tools", "rumors", "cheap materials"},
                new String[]{"sorting rooms", "refuse dens", "repair corners", "barter nests"}, new String[]{"maintenance tunnels", "hidden bypasses", "collapsed service routes"},
                new CirculationClass[]{CirculationClass.MAINTENANCE_TUNNEL, CirculationClass.HIDDEN_BYPASS},
                new PressureProfile(4, 2, 5, 2, 7, 1, 9, 5)));
        out.add(new FactionDoctrine(Faction.CULTIST, "Forbidden ritual logistics doctrine", "hides worship, contraband, bodies, symbols, and radicalization in neglected infrastructure",
                new String[]{"secrecy", "victims", "illegal relics", "black-market supplies"}, new String[]{"ritual pressure", "corruption", "ambush risk", "forbidden knowledge"},
                new String[]{"hidden shrines", "sewer chapels", "sacrificial rooms", "false stores"}, new String[]{"sewer trunks", "hidden bypasses", "pilgrimage-like ritual routes"},
                new CirculationClass[]{CirculationClass.SEWER_TRUNK, CirculationClass.HIDDEN_BYPASS, CirculationClass.SHRINE_PILGRIMAGE_ROUTE},
                new PressureProfile(1, 3, 2, 6, 6, 9, 7, 7)));
        out.add(new FactionDoctrine(Faction.MUTANT, "Exile survival doctrine", "survives through contaminated shelter, ambush geography, scavenged food, and hostile territorial control",
                new String[]{"scrap", "filth water", "shelter", "stolen food"}, new String[]{"biological danger", "territorial pressure", "salvage"},
                new String[]{"dens", "bone rooms", "fungal stores", "collapsed camps"}, new String[]{"sewer trunks", "hidden bypasses", "broken utility corridors"},
                new CirculationClass[]{CirculationClass.SEWER_TRUNK, CirculationClass.HIDDEN_BYPASS, CirculationClass.MAINTENANCE_TUNNEL},
                new PressureProfile(1, 2, 3, 7, 8, 1, 8, 4)));
        return out;
    }

    private static ArrayList<String> list(String[] values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(10, v));
    }

    private EconomicTopologyFramework() {}
}

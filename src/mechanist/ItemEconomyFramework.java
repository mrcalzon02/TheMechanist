package mechanist;

import java.util.*;

class ItemActionResult {
    boolean success;
    String itemName = "";
    String itemInstanceId = "";
    String containerId = "";
    String playerText = "";
    String auditText = "";
    ItemProvenanceRecord provenance = null;

    static ItemActionResult ok(String itemName, String itemInstanceId, String containerId, ItemProvenanceRecord provenance, String playerText, String auditText) {
        ItemActionResult r = new ItemActionResult();
        r.success = true;
        r.itemName = itemName == null ? "" : itemName;
        r.itemInstanceId = itemInstanceId == null ? "" : itemInstanceId;
        r.containerId = containerId == null ? "" : containerId;
        r.provenance = provenance;
        r.playerText = playerText == null ? "" : playerText;
        r.auditText = auditText == null ? "" : auditText;
        return r;
    }

    static ItemActionResult fail(String playerText, String auditText) {
        ItemActionResult r = new ItemActionResult();
        r.success = false;
        r.playerText = playerText == null ? "" : playerText;
        r.auditText = auditText == null ? "" : auditText;
        return r;
    }
}

class ItemInstance {
    String id = "I0";
    String displayName = "Unknown item";
    String containerId = "unknown.container";
    String provenanceUnitId = "untracked-unit";
    ItemProvenanceRecord provenance = null;

    ItemInstance() {}
    ItemInstance(String id, String displayName, String containerId, String provenanceUnitId, ItemProvenanceRecord provenance) {
        this.id = id == null || id.isBlank() ? "I0" : id;
        this.displayName = displayName == null || displayName.isBlank() ? "Unknown item" : displayName;
        this.containerId = containerId == null || containerId.isBlank() ? "unknown.container" : containerId;
        this.provenance = provenance;
        this.provenanceUnitId = provenanceUnitId == null || provenanceUnitId.isBlank() ? (provenance == null ? "untracked-unit" : provenance.unitId) : provenanceUnitId;
    }
    String encode() {
        return enc(id)+"~"+enc(displayName)+"~"+enc(containerId)+"~"+enc(provenanceUnitId)+"~"+enc(provenance == null ? "" : provenance.encode());
    }
    static ItemInstance decode(String line) {
        try {
            String[] a=line.split("~",5); if (a.length < 4) return null;
            ItemInstance inst = new ItemInstance();
            inst.id=dec(a[0]); inst.displayName=dec(a[1]); inst.containerId=dec(a[2]); inst.provenanceUnitId=dec(a[3]);
            if (a.length >= 5 && !dec(a[4]).isBlank()) inst.provenance = ItemProvenanceRecord.decode(dec(a[4]));
            return inst;
        } catch(Exception ex) { return null; }
    }
    static ArrayList<String> encodeRegistry(LinkedHashMap<String,ItemInstance> registry) {
        ArrayList<String> out=new ArrayList<>(); if (registry == null) return out;
        for (ItemInstance inst : registry.values()) if (inst != null) out.add(inst.encode());
        return out;
    }
    static void decodeRegistry(String encoded, LinkedHashMap<String,ItemInstance> registry) {
        if (registry == null) return;
        for (String line : Persistence.decList(encoded)) { ItemInstance inst=decode(line); if (inst != null) registry.put(inst.id, inst); }
    }
    static String enc(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String s) { return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8); }
}

class ContainerRecord {
    String id = "unknown.container";
    String label = "Unknown container";
    final ArrayList<String> itemInstanceIds = new ArrayList<>();

    ContainerRecord() {}
    ContainerRecord(String id, String label) { this.id=id; this.label=label; }
    String encode() { return enc(id)+"~"+enc(label)+"~"+enc(String.join(";", itemInstanceIds)); }
    static ContainerRecord decode(String line) {
        try {
            String[] a=line.split("~",3); if (a.length < 2) return null;
            ContainerRecord c=new ContainerRecord(dec(a[0]), dec(a[1]));
            if (a.length >= 3) for (String id : dec(a[2]).split(";")) if (!id.isBlank()) c.itemInstanceIds.add(id);
            return c;
        } catch(Exception ex) { return null; }
    }
    static ArrayList<String> encodeRegistry(LinkedHashMap<String,ContainerRecord> registry) {
        ArrayList<String> out=new ArrayList<>(); if (registry == null) return out;
        for (ContainerRecord c : registry.values()) if (c != null) out.add(c.encode());
        return out;
    }
    static void decodeRegistry(String encoded, LinkedHashMap<String,ContainerRecord> registry) {
        if (registry == null) return;
        for (String line : Persistence.decList(encoded)) { ContainerRecord c=decode(line); if (c != null) registry.put(c.id, c); }
    }
    static String enc(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String s) { return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8); }
}

class ItemProvenanceRecord {
    String itemName = "Unknown item";
    String makerFaction = "none";
    String maker = "unknown";
    String place = "unknown location";
    String inputs = "unrecorded inputs";
    String route = "direct acquisition";
    String chain = "";
    String unitId = "untracked-unit";
    String outputQuality = "";
    String knowledgeSource = "";
    String knowledgeProvider = "";
    String batchId = "";
    String defectState = "";
    String machineQuality = "";
    String machineCondition = "";
    String operatorSkill = "";
    String operatorSkillBand = "";
    String materialQuality = "";
    String qualityLimiter = "";
    int turnMade = 0;

    static ItemProvenanceRecord of(String item, Faction faction, String maker, World w, int turn, String inputs, String route) {
        ItemProvenanceRecord r = new ItemProvenanceRecord();
        r.itemName = item == null ? "Unknown item" : item;
        r.makerFaction = faction == null ? "NONE" : faction.name();
        r.maker = maker == null || maker.isBlank() ? "unknown" : maker;
        r.place = w == null ? "unknown location" : w.zoneCoordText() + " / " + w.zoneType.label;
        r.turnMade = Math.max(0, turn);
        r.inputs = inputs == null || inputs.isBlank() ? "unrecorded inputs" : inputs;
        r.route = route == null || route.isBlank() ? "direct acquisition" : route;
        r.chain = r.maker + " -> " + r.route;
        r.unitId = "ITM-" + Math.abs(Objects.hash(r.itemName, r.makerFaction, r.maker, r.place, r.inputs, r.route, r.turnMade, System.nanoTime()));
        return r;
    }
    static ItemProvenanceRecord produced(ProductionRecipe pr, BaseObject machine, World w, int turn, String worker) {
        return produced(pr, machine, w, turn, worker, null);
    }
    static ItemProvenanceRecord produced(ProductionRecipe pr, BaseObject machine, World w, int turn, String worker,
                                         ProductionQualityTraceAuthority.QualityTrace qualityTrace) {
        return produced(pr, machine, w, turn, worker, qualityTrace, null);
    }
    static ItemProvenanceRecord produced(ProductionRecipe pr, BaseObject machine, World w, int turn, String worker,
                                         ProductionQualityTraceAuthority.QualityTrace qualityTrace,
                                         ProductionOperatorSkillAuthority.OperatorSkill operatorSkill) {
        return produced(pr, machine, w, turn, worker, qualityTrace, operatorSkill, null);
    }
    static ItemProvenanceRecord produced(ProductionRecipe pr, BaseObject machine, World w, int turn, String worker,
                                         ProductionQualityTraceAuthority.QualityTrace qualityTrace,
                                         ProductionOperatorSkillAuthority.OperatorSkill operatorSkill,
                                         ProductionKnowledgeSourceAuthority.KnowledgeSource knowledge) {
        return produced(pr, machine, w, turn, worker, qualityTrace, operatorSkill, knowledge, null);
    }
    static ItemProvenanceRecord produced(ProductionRecipe pr, BaseObject machine, World w, int turn, String worker,
                                         ProductionQualityTraceAuthority.QualityTrace qualityTrace,
                                         ProductionOperatorSkillAuthority.OperatorSkill operatorSkill,
                                         ProductionKnowledgeSourceAuthority.KnowledgeSource knowledge,
                                         ProductionBatchAuthority.BatchDisposition batch) {
        String item = pr == null ? "Unknown product" : pr.outputItemName();
        Faction f = pr == null ? Faction.NONE : pr.faction;
        String m = (machine == null ? "manual workbench" : machine.name) + " operated by " + (worker == null ? "unknown worker" : worker);
        String inputs = pr == null ? "unknown recipe inputs" : "recipe knowledge=" + pr.knowledgeName + "; machine=" + pr.machineName + "; faction pattern=" + pr.profile.label;
        ItemProvenanceRecord r = of(item, f, m, w, turn, inputs, "produced into player/faction stock");
        if (pr != null) {
            r.outputQuality = pr.qualityName == null ? "" : pr.qualityName;
            r.knowledgeSource = pr.knowledgeName == null ? "" : pr.knowledgeName;
            r.machineQuality = machine == null || machine.qualityName == null ? "Common" : machine.qualityName;
            r.machineCondition = MachineConditionProductionAuthority.evaluate(machine).label();
        }
        if (qualityTrace != null) {
            r.outputQuality = qualityTrace.outputQuality();
            r.qualityLimiter = qualityTrace.limiterLabel();
            r.materialQuality = qualityTrace.materialTier() < 0 ? "open" : QualityAuthorityApi.qualityName(qualityTrace.materialTier());
        }
        if (operatorSkill != null) {
            r.operatorSkill = operatorSkill.recipeSkill() + " via " + operatorSkill.coreStat() + " " + operatorSkill.value();
            r.operatorSkillBand = operatorSkill.band();
        }
        if (knowledge != null) r.knowledgeProvider = knowledge.sourceLabel();
        if (batch != null) {
            r.batchId = batch.batchId();
            r.defectState = batch.defectState();
        }
        return r;
    }
    static ItemProvenanceRecord producedByFixture(String item, Faction faction, String fixture, World w, int turn, String inputs, String route) { return of(item, faction, fixture, w, turn, inputs, route); }
    static ItemProvenanceRecord transferred(ItemProvenanceRecord prior, String item, World w, int turn, String route) {
        if (prior == null) return of(item, Faction.NONE, "untraced transfer", w, turn, "prior ledger unavailable", route);
        ItemProvenanceRecord r = of(item == null ? prior.itemName : item, Faction.NONE, prior.maker, w, turn, prior.inputs, route);
        r.makerFaction = prior.makerFaction;
        r.place = prior.place;
        r.turnMade = prior.turnMade;
        r.unitId = prior.unitId == null || prior.unitId.isBlank() ? r.unitId : prior.unitId;
        r.outputQuality = prior.outputQuality;
        r.knowledgeSource = prior.knowledgeSource;
        r.knowledgeProvider = prior.knowledgeProvider;
        r.batchId = prior.batchId;
        r.defectState = prior.defectState;
        r.machineQuality = prior.machineQuality;
        r.machineCondition = prior.machineCondition;
        r.operatorSkill = prior.operatorSkill;
        r.operatorSkillBand = prior.operatorSkillBand;
        r.materialQuality = prior.materialQuality;
        r.qualityLimiter = prior.qualityLimiter;
        String priorChain = prior.chain == null || prior.chain.isBlank() ? (prior.maker + " -> " + prior.route) : prior.chain;
        r.chain = priorChain + " -> " + r.route;
        return r;
    }
    static ItemProvenanceRecord trade(String item, Faction faction, String trader, World w, int turn, String route) { return of(item, faction, trader, w, turn, "trader/vendor stock ledger", route); }
    static ItemProvenanceRecord found(String item, World w, int turn, String site) { return of(item, Faction.NONE, site, w, turn, "world container/cache", "found by player"); }
    static ItemProvenanceRecord startingKit(String item, JobProfile job, World w, int turn) { return of(item, job == null ? Faction.NONE : job.faction, job == null ? "starting kit" : job.name + " starting kit", w, turn, "occupation-issued starting equipment", "carried at character insertion"); }
    static ItemProvenanceRecord unknown(String item, GamePanel g) { return of(item, Faction.NONE, "legacy stack", g == null ? null : g.world, g == null ? 0 : g.turn, "legacy/untraced", "created before provenance tracking"); }
    String shortChain() {
        String c = chain == null || chain.isBlank() ? maker + " -> " + route : chain;
        return unitId + " " + c;
    }
    ArrayList<String> qualityContextLines() {
        ArrayList<String> lines = new ArrayList<>();
        if (!outputQuality.isBlank()) lines.add("Production quality: " + outputQuality + ".");
        if (!knowledgeSource.isBlank()) lines.add("Knowledge source: " + knowledgeSource + ".");
        if (!knowledgeProvider.isBlank()) lines.add("Knowledge provider: " + knowledgeProvider + ".");
        if (!batchId.isBlank()) lines.add("Production batch: " + batchId + ".");
        if (!defectState.isBlank()) lines.add("Batch inspection: " + defectState + ".");
        if (!machineQuality.isBlank()) lines.add("Producing machine quality: " + machineQuality + ".");
        if (!machineCondition.isBlank()) lines.add("Producing machine condition: " + machineCondition + ".");
        if (!operatorSkill.isBlank()) lines.add("Producing operator skill: " + operatorSkill + ".");
        if (!operatorSkillBand.isBlank()) lines.add("Producing operator band: " + operatorSkillBand + ".");
        if (!materialQuality.isBlank()) lines.add("Consumed material quality cap: " + materialQuality + ".");
        if (!qualityLimiter.isBlank()) lines.add("Recorded quality limiter: " + qualityLimiter + ".");
        return lines;
    }
    String summary() { return "Origin: " + itemName + " | unit=" + unitId + " | maker=" + maker + " | faction=" + makerFaction + " | place=" + place + " | inputs=" + inputs + " | route=" + route + " | chain=" + shortChain() + " | turn=" + turnMade + "."; }
    String encode() { return enc(itemName)+"~"+enc(makerFaction)+"~"+enc(maker)+"~"+enc(place)+"~"+enc(inputs)+"~"+enc(route)+"~"+turnMade+"~"+enc(chain)+"~"+enc(unitId)+"~"+enc(outputQuality)+"~"+enc(knowledgeSource)+"~"+enc(machineQuality)+"~"+enc(qualityLimiter)+"~"+enc(machineCondition)+"~"+enc(operatorSkill)+"~"+enc(operatorSkillBand)+"~"+enc(materialQuality)+"~"+enc(knowledgeProvider)+"~"+enc(batchId)+"~"+enc(defectState); }
    static ItemProvenanceRecord decode(String line) {
        try {
            String[] a = line.split("~",20); if (a.length < 7) return null;
            ItemProvenanceRecord r = new ItemProvenanceRecord(); r.itemName=dec(a[0]); r.makerFaction=dec(a[1]); r.maker=dec(a[2]); r.place=dec(a[3]); r.inputs=dec(a[4]); r.route=dec(a[5]); r.turnMade=Integer.parseInt(a[6]);
            if (a.length >= 8) r.chain=dec(a[7]); else r.chain=r.maker + " -> " + r.route;
            if (a.length >= 9) r.unitId=dec(a[8]); else r.unitId="LEGACY-" + Math.abs(Objects.hash(r.itemName, r.maker, r.place, r.turnMade));
            if (a.length >= 10) r.outputQuality=dec(a[9]);
            if (a.length >= 11) r.knowledgeSource=dec(a[10]);
            if (a.length >= 12) r.machineQuality=dec(a[11]);
            if (a.length >= 13) r.qualityLimiter=dec(a[12]);
            if (a.length >= 14) r.machineCondition=dec(a[13]);
            if (a.length >= 15) r.operatorSkill=dec(a[14]);
            if (a.length >= 16) r.operatorSkillBand=dec(a[15]);
            if (a.length >= 17) r.materialQuality=dec(a[16]);
            if (a.length >= 18) r.knowledgeProvider=dec(a[17]);
            if (a.length >= 19) r.batchId=dec(a[18]);
            if (a.length >= 20) r.defectState=dec(a[19]);
            return r;
        } catch(Exception ex) { return null; }
    }
    static ArrayList<String> encodeLedger(LinkedHashMap<String,ArrayDeque<ItemProvenanceRecord>> ledger) {
        ArrayList<String> out = new ArrayList<>();
        if (ledger == null) return out;
        for (Map.Entry<String,ArrayDeque<ItemProvenanceRecord>> e : ledger.entrySet()) for (ItemProvenanceRecord r : e.getValue()) out.add(r.encode());
        return out;
    }
    static void decodeLedger(String encoded, LinkedHashMap<String,ArrayDeque<ItemProvenanceRecord>> ledger) {
        if (ledger == null) return;
        for (String line : Persistence.decList(encoded)) {
            ItemProvenanceRecord r = decode(line); if (r == null) continue;
            ledger.computeIfAbsent(r.itemName, k -> new ArrayDeque<>()).addLast(r);
        }
    }
    static String enc(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String s) { return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8); }
}

class ItemQuality {
    static final String[] NAMES = {"Junk", "Shoddy", "Common", "Serviceable", "Fine", "Masterwork", "Noble", "Archeotech"};
    static final double[] VALUE = {0.20, 0.45, 1.00, 1.35, 1.90, 2.80, 4.25, 7.00};
    static final double[] CHARGES = {0.35, 0.60, 1.00, 1.20, 1.55, 2.10, 3.00, 4.50};
    static boolean isQualityPrefix(String name) {
        if (name == null) return false;
        for (String q : NAMES) if (name.equalsIgnoreCase(q)) return true;
        return false;
    }
    static int tierIndex(String itemName) {
        if (itemName == null) return 2;
        String s = itemName.trim().toLowerCase(Locale.ROOT);
        for (int i=0;i<NAMES.length;i++) if (s.startsWith(NAMES[i].toLowerCase(Locale.ROOT) + " ")) return i;
        return 2;
    }
    static String stripQuality(String itemName) {
        if (itemName == null) return "Unknown item";
        String s = itemName.trim();
        for (String q : NAMES) if (s.toLowerCase(Locale.ROOT).startsWith(q.toLowerCase(Locale.ROOT) + " ")) return s.substring(q.length()).trim();
        return s;
    }
    static String stripManufacturingIdentity(String itemName) {
        String s = stripQuality(itemName);
        String[] prefixes = {"Rite-Forged", "Sanctioned", "Field-Issue", "Gilded", "Street-Cut", "Profaned", "Adapted", "Improvised", "Civic"};
        for (String pre : prefixes) if (s.toLowerCase(Locale.ROOT).startsWith(pre.toLowerCase(Locale.ROOT) + " ")) return s.substring(pre.length()).trim();
        return s;
    }

    static boolean namesMatch(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b) || a.equalsIgnoreCase(b)) return true;
        String aa = stripManufacturingIdentity(a);
        String bb = stripManufacturingIdentity(b);
        return aa.equalsIgnoreCase(bb) || stripQuality(aa).equalsIgnoreCase(stripQuality(bb));
    }

    static int priced(int base, String itemName) { return Math.max(1, (int)Math.round(Math.max(1, base) * VALUE[tierIndex(itemName)])); }
    static ArrayList<String> detailLines() {
        return QualityAuthorityApi.detailLines();
    }
}


class ContainerIdentityApi {
    static String safeToken(String raw) {
        if (raw == null || raw.isBlank()) return "unknown";
        StringBuilder b = new StringBuilder();
        for (char ch : raw.toLowerCase(Locale.ROOT).toCharArray()) {
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) b.append(ch);
            else if (ch == '-' || ch == '_' || ch == '.') b.append(ch);
            else b.append('_');
        }
        String out = b.toString().replaceAll("_+", "_");
        return out.isBlank() ? "unknown" : out;
    }
}

class ItemDef {
    String name, category, source, description, use; int basePrice; boolean weapon;
    ItemDef(String name, String category, int basePrice, String source, String description, String use, boolean weapon) {
        this.name=name; this.category=category; this.basePrice=basePrice; this.source=source; this.description=description; this.use=use; this.weapon=weapon;
    }
    ArrayList<String> detailLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Item registry entry");
        lines.add("Category: " + category + (weapon ? " / weapon" : ""));
        lines.add("Base price: " + basePrice + " script at Common quality; quality modifies sale/buy values.");
        lines.add("Sources: " + source);
        lines.add("Description: " + description);
        lines.add("Use: " + use);
        lines.add(ItemSemanticAssetAuthority.semanticSummaryForItemName(name));
        lines.add("Quality variants: " + String.join(" -> ", ItemQuality.NAMES));
        lines.add("Example value range: Junk " + ItemQuality.priced(basePrice, "Junk " + name) + " / Common " + basePrice + " / Archeotech " + ItemQuality.priced(basePrice, "Archeotech " + name));
        return lines;
    }
}

class ItemCatalog {
    static final LinkedHashMap<String,ItemDef> ITEMS = makeItems();
    static ItemDef add(LinkedHashMap<String,ItemDef> m, String n, String cat, int price, String src, String desc, String use, boolean weapon) {
        ItemDef d = new ItemDef(n, cat, price, src, desc, use, weapon); m.put(n, d); return d;
    }
    static ItemDef get(String name) {
        if (name == null) return null;
        ItemDef d = ITEMS.get(ItemQuality.stripQuality(name));
        if (d != null) return d;
        return ITEMS.get(ItemQuality.stripManufacturingIdentity(name));
    }
    static int priceFor(String name) { ItemDef d = get(name); return d == null ? 1 : ItemQuality.priced(d.basePrice, name); }
    static boolean isFirearmLike(String name) {
        ItemDef d = get(name);
        if (d == null || !d.weapon) return false;
        String low = (ItemQuality.stripManufacturingIdentity(name) + " " + d.category + " " + d.use).toLowerCase(Locale.ROOT);
        if (isExplosiveLike(name)) return false;
        return low.contains("pistol") || low.contains("rifle") || low.contains("gun") || low.contains("bolter") || low.contains("flamer") || low.contains("melta") || low.contains("webber") || low.contains("spitter") || low.contains("stubber") || low.contains("autocannon") || low.contains("shotgun") || low.contains("carbine") || low.contains("las") || low.contains("ranged") || low.contains("ammo");
    }
    static boolean isExplosiveLike(String name) {
        ItemDef d = get(name);
        if (d == null || !d.weapon) return false;
        String low = (ItemQuality.stripManufacturingIdentity(name) + " " + d.category + " " + d.use).toLowerCase(Locale.ROOT);
        return low.contains("grenade") || low.contains("bomb") || low.contains("mine") || low.contains("satchel") || low.contains("tripwire") || low.contains("claymore") || low.contains("bouncing betty") || low.contains("explosive");
    }
    static ArrayList<String> names(boolean weapons) {
        ArrayList<String> out = new ArrayList<>();
        for (ItemDef d : ITEMS.values()) if (d.weapon == weapons) out.add(d.name);
        Collections.sort(out);
        return out;
    }
    static LinkedHashMap<String,ItemDef> makeItems() {
        LinkedHashMap<String,ItemDef> m = new LinkedHashMap<>();
        add(m,"Emergency rations","food",4,"civilian traders, sewer traders, vending machines, scavenging, threshold events","compressed survival matter with the texture of punished cardboard","consume to restore food and some water",false);
        add(m,"Plain ration pack","food",3,"Arbites-adjacent traders and lawful ration stocks","sealed ration issued through official or semi-official channels","consume to restore food",false);
        add(m,"Protein ration","food",4,"Depot Loader starting kit, freight and worker-sector loot","dense worker protein in a wrapper that has seen things","consume to restore food",false);
        add(m,"Ration brick","food",3,"Underhive Scavenger starting kit and low-grade scavenging","a rectangular argument against hunger","consume to restore food and a little water",false);
        add(m,"Tin of corpse-starch","food",2,"Tunnel Bruiser starting kit and underhive food caches","cheap starch ration best not investigated","consume to restore food",false);
        add(m,"Cheap lunch tin","food",3,"Hab worker starts, civilian floors, rail yards","small civilian meal tin; edible by local legal standards","consume to restore food",false);
        add(m,"Water bottle","water",3,"civilian traders, vending machines, markets, neutral floors","sealed or resealed drinking water","consume to restore water",false);
        add(m,"Water ration","water",2,"Arbites-adjacent traders and ration kiosks","stamped water ration with bureaucratic aftertaste","consume to restore water",false);
        add(m,"Sealed water ration","water",4,"uncommon zone events, sewer thresholds, survival caches","better sealed survival water for hostile layers","consume to restore water",false);
        add(m,"Water purification tab","water",3,"sewer traders and utility stores","small chemical treatment tablet for questionable water","consume/use to restore water or justify reserved dirty-water purification",false);
        add(m,"Dirty canteen","water/tool",2,"Scrap Mechanist starting kit and trash-warren scavenging","a canteen with more history than hygiene","consume to restore water; quality affects reliability later",false);
        add(m,"Filter canteen","water/tool",5,"Sump Prospector starting kit and sewer traders","canteen with a filter that may actually filter something","consume to restore water; supports sewer survival checks",false);
        add(m,"Construction supplies","supplies",5,"civilian traders, work sites, workbench salvage, vending machines","generic build supplies pooled into the construction system","USE packs carried copy into build-supply pool",false);
        add(m,"Machine part","mechanical",3,"Mechanicus traders, vending machines, machinery rooms, forge cloisters","usable mechanical component","workbench/build/repair material",false);
        add(m,"Mechanical detritus","mechanical junk",2,"broken machines, trash warrens, forge floors","scrap-mouth parts and incomplete components","workbench salvage; low-tier quality tends toward clogging junk",false);
        add(m,"Spare bolts","mechanical",2,"Scrap Mechanist starting kit and maintenance rooms","small hardware worth keeping","workbench/repair material",false);
        add(m,"Wire bundle","component",4,"Hab Electrician starting kit, electrical rooms, trade","bundle of serviceable wire","electrical repair/build material",false);
        add(m,"Sacred wire bundle","component",5,"Mechanicus traders and forge cloisters","wire bundle with ritual handling and better insulation","electrical and emergency-machine construction material",false);
        add(m,"Machine oil vial","mechanical",5,"Junior Tech Priest starting kit, forge cloisters, Mechanicus traders","small vial of blessed or at least oily machine lubricant","reserved machine maintenance and repair support",false);
        add(m,"Tool bundle","tool",7,"civilian traders, work rooms, neutral markets","basic tools for practical problems","carried tool; applies to doors, repair, and fabrication checks",false);
        add(m,"Mechanicus tool roll","tool",8,"Mechanicus traders and forge cloisters","ritualized tool set with actual utility beneath the incense","carried tool; improves machine/workbench/security checks",false);
        add(m,"Insulated pliers","tool",6,"Hab Electrician starting kit and maintenance rooms","pliers with shock protection that may still be aspirational","carried electrical/mechanics tool",false);
        add(m,"Cracked wrench","tool",4,"Scrap Mechanist starting kit and machine rooms","a wrench with history and questionable alignment","carried tool; repair/workbench support",false);
        add(m,"Ritual wrench","tool/weapon",7,"Junior Tech Priest starting kit and forge cloisters","a tool with enough doctrine to hurt someone","carried tool and melee weapon",true);
        add(m,"Cargo hook","tool/weapon",5,"Depot Loader starting kit, freight depots","hook for cargo, doors, and unfortunate arguments","carried tool and melee weapon",true);
        add(m,"Lockpicks","security",6,"ganger traders, caches, criminal rooms","small lock tools for weak mechanical locks","carried security tool",false);
        add(m,"Data spike","security",18,"Mechanicus traders, Arbites-adjacent black trade, rare traders","single-use intrusion spike for vending hacks and electronic locks","context security use; later consumed by stronger hacks",false);
        add(m,"Off-world ticket","victory",5000,"Sector Governor's Mansion, rare seed-bound governor loot or bribe result","a priceless warrant for leaving the sector stack and ending the run","win condition item; reach a departure route with it",false);
        add(m,"Governor signet","paperwork",250,"dead sector governors without tickets","a blood-warm seal of failed authority","reserved bribe, forgery, or faction leverage",false);
        add(m,"Permit form","paperwork",10,"Arbites traders, administratum archives, permit kiosks","a document-shaped shield against official attention","reserved social/inspection mitigation",false);
        add(m,"Noble Commerce Permit","paperwork/commerce",18,"noble factors, lawful commerce offices, high-status traders","a purchased license allowing one player-owned business to operate lawfully","consume from inventory/base storage when opening a licensed business",false);
        add(m,"Trade chit","currency/trade",2,"depot scrap buyers, market rooms, barter counters, scavenging","low-denomination local trade marker used by depot clerks and petty vendors","trade currency for simple depot/barter actions",false);
        add(m,"Secure vault key","security/key",45,"noble, Arbites, and Administratum secure rooms; rare scavenge result","coded key or credential token for sealed vault doors","opens selected sealed vault doors when carried",false);
        add(m,"Blank form packet","paperwork",7,"Permit Clerk Deserter starting kit and Administratum archives","blank forms, useful to anyone dishonest or employed","paperwork/social access support",false);
        add(m,"Expired work permit","paperwork",4,"Permit Clerk Deserter starting kit and worker sectors","a permit that failed to stay useful on schedule","reserved low-grade identity/social checks",false);
        add(m,"Ink stylus","paperwork/tool",3,"Permit Clerk Deserter starting kit and offices","writing tool for forms, lies, and small corrections","reserved administratum/form interaction support",false);
        add(m,"Mildewed map scrap","navigation",3,"Sump Prospector starting kit and sewer scavenging","partial route note grown damp and smug","USE reduces suspicion/heat pressure",false);
        add(m,"Survey hook","navigation/tool",4,"Sump Prospector starting kit and utility rooms","hook and line for probing distance and danger","reserved navigation/scavenge support",false);
        add(m,"Filter mask","survival",5,"sewer traders, utility rooms, maintenance caches","mask that makes breathing below ground slightly less insulting","reserved sewer hazard mitigation",false);
        add(m,"Coffee tin","stimulant",3,"ganger traders, worker spaces, vending","bitter wakefulness in a tin","USE reduces sleep need and adds stimulant strain",false);
        add(m,"Stim vial","stimulant",6,"traders, Chem Runner starts, medical or criminal caches","chemical insistence that the body continue","USE sharply reduces sleep need and adds strain",false);
        add(m,"Stimulant ampoule","stimulant",6,"Chem Runner starting kit and chemical trade","single-dose stimulant ampoule","USE reduces sleep need and adds strain",false);
        add(m,"Bandage roll","medical",4,"medical rooms, vendors, scavenged kits, worker safety boxes","cloth strips clean enough to argue with bleeding","USE reduces bleeding and minor wounds",false);
        add(m,"Field dressings","medical",7,"Rogue Medicae starting kit, medical rooms, caches","basic wound care supplies","USE reduces bleeding, wounds, and fatigue",false);
        add(m,"Antiseptic vial","medical",6,"medical rooms, Medicae stalls, utility first-aid lockers","stinging chemical mercy against infection","USE sharply reduces infection risk",false);
        add(m,"Splint kit","medical",8,"Medicae stalls, freight infirmaries, combat caches","rigid limb support for arms and legs that no longer agree with doctrine","USE stabilizes the worst damaged limb and lowers pain",false);
        add(m,"Medkit","medical",14,"Medicae stalls, clinics, noble security caches, rare vending","proper compact trauma kit","USE treats bleeding, wounds, infection risk, fatigue, pain, and worst body damage",false);
        add(m,"Injector case","medical",9,"Rogue Medicae starting kit and medical stores","case of injectable supplies","USE reduces wounds/fatigue/pain; quality later affects charges",false);
        add(m,"Bent scalpel","medical/tool/weapon",4,"Rogue Medicae starting kit and medicae rooms","small medical blade with questionable straightness","reserved medical tool and emergency weapon",true);
        add(m,"Patch sack","container",2,"Underhive Scavenger starting kit and trash warrens","bag for carrying low-value dignity and salvage","reserved carry/storage modifier",false);
        add(m,"Hidden pouch","container",5,"Chem Runner starting kit and criminal spaces","concealed pouch for contraband","reserved concealment/carry modifier",false);
        add(m,"Work gloves","clothing/tool",3,"Depot Loader starting kit and freight yards","work gloves for cargo and hand survival","reserved hand protection/tool support",false);
        add(m,"Knee wraps","clothing",2,"Tunnel Bruiser starting kit and maintenance tunnels","padded wraps for crawling, bracing, and regretting floors","reserved limb protection",false);
        add(m,"Plain civilian coat","clothing",6,"civilian traders and neutral floors","low-grade defense and civilian cover","USE equips as clothing/disguise",false);
        add(m,"Damaged ganger coat","clothing",5,"ganger traders, corpses, gang rooms","minor protection and questionable gang-adjacent cover","USE equips as clothing/disguise",false);
        add(m,"Scavenger rags","clothing",4,"Underhive Scavenger starting outfit and trash warrens","ragged floor-level clothing","USE equips as clothing/disguise",false);
        add(m,"Mechanicus novice robe","clothing",9,"Junior Tech Priest starting outfit and forge cloisters","low-ranking Mechanicus robe","USE equips as clothing/disguise",false);
        add(m,"Gang colors","clothing",7,"Hive Ganger starting outfit and gang turf","visible gang affiliation","USE equips as clothing/disguise",false);
        add(m,"Arbites patrol coat","clothing/armor",12,"Arbites Probationer starting outfit and law zones","junior law-enforcement coat","USE equips as clothing/disguise",false);
        add(m,"Waterproof scavenger wraps","clothing",6,"Sump Prospector starting outfit and sewer scavengers","water-resistant scavenger wraps","USE equips as clothing/disguise",false);
        add(m,"Hiver workwear","clothing",6,"Hab Electrician starting outfit and worker sectors","plain workwear for ordinary survival","USE equips as clothing/disguise",false);
        add(m,"Stained civilian coat","clothing",6,"Rogue Medicae starting outfit and civilian rooms","civilian coat with medical stains","USE equips as clothing/disguise",false);
        add(m,"Cargo hauler overalls","clothing",6,"Depot Loader starting outfit and freight depots","durable freight-worker clothing","USE equips as clothing/disguise",false);
        add(m,"Frayed administratum coat","clothing",6,"Permit Clerk Deserter starting outfit and archives","administratum coat that has lost a fight with humidity","USE equips as clothing/disguise",false);
        add(m,"Padded tunnel leathers","clothing/armor",7,"Tunnel Bruiser starting outfit and tunnel rooms","padded leathers for rough maintenance spaces","USE equips as clothing/disguise",false);
        add(m,"Runner colors under coat","clothing",6,"Chem Runner starting outfit and criminal route caches","concealed runner markings under civilian cover","USE equips as clothing/disguise",false);
        add(m,"Oil-stained workwear","clothing",6,"Scrap Mechanist starting outfit and repair rooms","workwear soaked in machine biography","USE equips as clothing/disguise",false);
        add(m,"Baton-dented vest","armor",9,"Arbites-adjacent traders and law/security zones","civilian-grade protection with institutional bruising","USE equips as armor-like clothing",false);
        add(m,"Flashlight","portable light",9,"civilian traders, maintenance rooms, security lockers, trash caches","hand torch with a narrow useful beam and a battery that will betray you later","USE activates; DROP/THROW leaves light in world space",false);
        add(m,"Glow stick","portable light",3,"medical kits, security lockers, sewer caches, children and fools","single-use chemical light for low-intensity marking and emergency visibility","USE snaps and consumes; DROP/THROW can mark terrain",false);
        add(m,"Lantern","portable light",8,"hab markets, maintenance rooms, underhive stalls","general lamp with better spread than dignity","USE lights; DROP/THROW leaves a broader source",false);
        add(m,"Stub light","portable light/junk",2,"trash warrens, scavver benches, improvised tool piles","a bulb, battery, and wire wrapped in duct tape; an argument against darkness","USE activates briefly; can be dropped or thrown",false);
        add(m,"Mining helmet","helmet portable light",14,"mining stores, industrial lockers, sump crews","helmet-mounted lamp for people paid to enter holes","USE wears the head lamp without occupying carried hands",false);
        add(m,"Scavenging helmet","helmet portable light",10,"scavenger traders, trash warrens, abandoned rooms","patched helmet with a serviceable forward light","USE wears the head lamp; good for rummaging where the floor lies",false);
        add(m,"Electrician's rig","headband portable light",7,"electrical rooms, maintenance lockers, hab electrician kits","snap light attached to a stretchy band; crude, useful, and humiliating","USE wears as a head light",false);
        add(m,"Phosphor bulb","organic portable light",4,"swamps, abandoned hydroponics, sewer fungal patches","wild organic glow bulb that can be carried or thrown before it fades","USE carries the live glow; DROP/THROW plants temporary light",false);
        add(m,"Swamp lantern","chemical portable light",11,"sump markets, mutant camps, swamp stills","unknown chemicals dumped into a gas lantern and lit on fire with confidence","USE lights a strong but ugly flame source",false);
        add(m,"Scrap knife","weapon",4,"Underhive Scavenger starting kit and trash/gang spaces","rough cutting weapon","weapon; equip/combat wiring still pending",true);
        add(m,"Rusty knife","weapon",4,"sewer traders and low-grade scavenging","a social tool with edges and tetanus ambitions","weapon; equip/combat wiring still pending",true);
        add(m,"Shiv","weapon",3,"Hive Ganger starting kit and gang turf","small improvised blade","weapon; equip/combat wiring still pending",true);
        add(m,"Tiny knife","weapon",2,"Chem Runner starting kit and pockets that should be searched","tiny but rude blade","weapon; equip/combat wiring still pending",true);
        add(m,"Stub pistol with poor sights","weapon",12,"Hive Ganger starting kit and gang stock","cheap firearm with optimistic sights","weapon; equip/reload wiring pending",true);
        add(m,"Stub rounds","ammo",4,"ganger traders, ammo tins, security rooms","ammunition and threats in one small packet","ammunition for stub weapons",false);
        add(m,"Stub round packet","ammo",4,"weapon vending and gang storage","small packet of stub ammunition","ammunition for stub weapons",false);
        add(m,"Shock baton","weapon",10,"Arbites starts, precincts, evidence lockers","lawful stick with unlawful persuasion","weapon; equip/combat wiring pending",true);
        add(m,"Security baton","weapon",8,"security areas, precincts, patrol rooms","basic baton for making rules portable","weapon; equip/combat wiring pending",true);
        add(m,"Heavy spanner","weapon/tool",6,"Tunnel Bruiser starting kit and machine rooms","large spanner with workplace violence potential","tool and weapon",true);
        add(m,"Improvised club","weapon",3,"trash warrens, riots, desperate rooms","a blunt solution to a complicated century","weapon; equip/combat wiring pending",true);
        add(m,"Battered helmet","armor",5,"armor vending, security rooms, corpses","helmet that has already participated in bad news","reserved head protection",false);
        add(m,"Padded coat","clothing/armor",5,"armor vending and civilian rooms","thick coat for cold metal halls and softer impacts","USE equips as clothing/disguise",false);
        add(m,"Contraband charm","trinket",5,"Hive Ganger starting kit and criminal spaces","small charm with illegal sentimental value","reserved social/faction flavor item",false);
        add(m,"Citation slate","paperwork/tool",8,"Arbites Probationer starting kit and precincts","official slate for making problems formal","reserved law/social interaction support",false);
        add(m,"Vended scrap","junk",1,"failed or generic vending output","low-value scrap from a machine with contempt","workbench salvage",false);
        ItemCatalogExpansionApi.registerFactionIdentityItems(m);
        return m;
    }
}

class ItemLedgerAuthority {
    private ItemLedgerAuthority() {}
    static String audit(GamePanel g, String context) {
        if (g == null) return "Item ledger audit skipped: no game panel.";
        int containers = g.itemContainers.size();
        int instances = g.itemInstances.size();
        int dangling = 0;
        int physicalScript = 0;
        HashSet<String> seen = new HashSet<>();
        for (ContainerRecord c : g.itemContainers.values()) {
            if (c == null) continue;
            for (String id : c.itemInstanceIds) {
                ItemInstance inst = g.itemInstances.get(id);
                if (inst == null) { dangling++; continue; }
                if (!seen.add(id)) dangling++;
                if ("Imperial Script".equalsIgnoreCase(inst.displayName)) physicalScript++;
                if (inst.containerId == null || !inst.containerId.equals(c.id)) dangling++;
            }
        }
        for (ItemInstance inst : g.itemInstances.values()) {
            if (inst == null) continue;
            if ("Imperial Script".equalsIgnoreCase(inst.displayName)) physicalScript++;
            ContainerRecord c = g.itemContainers.get(inst.containerId);
            if (c == null || !c.itemInstanceIds.contains(inst.id)) dangling++;
        }
        boolean parity = g.verifyItemOperationalParity(context);
        if (physicalScript > 0) g.purgePhysicalScriptInstances("item ledger audit found physical script");
        return "context=" + context + "; containers=" + containers + "; instances=" + instances + "; dangling=" + dangling + "; physicalScript=" + physicalScript + "; player/base parity=" + parity + "; carriedScript=" + g.carriedScript + "; banked=" + g.totalBankedCash() + "; baseScript=" + g.baseStashedScript + ".";
    }
}

class ItemCatalogExpansionApi {
    static void registerFactionIdentityItems(LinkedHashMap<String,ItemDef> m){
        // 0.8.68 CONTENT API PASS:
        // Item/weapon variety now has its own registration surface instead of continuing
        // to bloat the base ItemCatalog.makeItems() pile. These are catalog entries first:
        // trade, loot, provenance, and infopedia can reference them immediately, while
        // deeper equip/ammo simulation remains a later combat API concern.
        foodAndDailyLife(m);
        waterFoodWasteAgricultureChainItems(m);
        chemNarcoticIntoxicantChainItems(m);
        laboratoryChemicalEquipmentItems(m);
        factionToolsAndProducts(m);
        industrialComponentItems(m);
        clothingArmorTextileComponentItems(m);
        clothingArmorVarietyItems(m);
        factionRecipeVariantIdentityItems(m);
        factionWeaponsAndAmmo(m);
        importedWeaponFamilies(m);
        underhiveImprovisedWeapons(m);
        weaponAmmunitionFamilies(m);
        factionArmorAndClothing(m);
        booksTrainingAndCivicGoods(m);
        hivewallCacheGoods(m);
    }
    private static void add(LinkedHashMap<String,ItemDef> m, String n, String cat, int price, String src, String desc, String use, boolean weapon){ ItemCatalog.add(m,n,cat,price,src,desc,use,weapon); }

    private static void industrialComponentItems(LinkedHashMap<String,ItemDef> m){
        // 0.8.92 COMPONENT CATALOG EXPANSION PASS:
        // Reusable precursor components are catalog entries, not immediately exposed player recipes.
        // RecipeDecompositionApi scans the registry and generates draft bills of material from these names.
        // This catalog widens the industrial vocabulary used by weapon-family decomposition.
        add(m,"Sandbag fill","component/defense/earthworks",2,"PDF stores, construction yards, roadblocks, Guard depots","dense fill and sack material for low-tech firing positions","construction input for sandbag lines and cheap barricades",false);
        add(m,"Razor wire coil","component/defense/wire",5,"Arbites precinct stores, perimeter cages, military checkpoints","coiled wire intended to make movement slow, loud, and regrettable","construction input for wire obstacles and reserved hazard defenses",false);
        add(m,"Sensor lens","component/defense/sensor",8,"security rooms, Arbites cogitator stores, stripped alarm fixtures","small detection lens and mount for alarm/sensor hardware","construction input for sensors and reserved line-of-sight security systems",false);
        add(m,"Turret traverse ring","component/defense/turret",16,"armory hardpoints, precinct workshops, Guard emplacements","bearing ring and servo mount for defensive turret rotation","construction input for turret entities",false);
        add(m,"Turret barrel assembly","component/defense/turret",18,"weapon shops, Guard armories, Arbites stores","barrel, shroud, recoil sleeve, and mounting lugs for fixed defenses","construction input for turret entities",false);
        add(m,"Precast defense slab","component/defense/wall",10,"construction yards, precinct refits, estate contractors","dense prefabricated panel for reinforced walling and hard cover","construction input for reinforced walls and defensive doors",false);
        add(m,"Arbites lock core","component/defense/access",14,"Adeptus Arbites precinct stores and lawful security workshops","serialized lock heart used in precinct doors and access-control defenses","construction input for Arbites access-control objects",false);
        add(m,"Armament components","component/armament",6,"forge bins, armory works, salvage sorting tables","generic hardened weapon parts: plates, brackets, pins, feed lips, mounts, and receiver fragments","industrial input for weapon bodies and tool weapons",false);
        add(m,"Small fastener set","component/fasteners",2,"workshops, hab maintenance bins, ganger benches","mixed screws, pins, clips, washers, and tiny betrayals of inventory control","industrial input for small assemblies",false);
        add(m,"Reinforced fastener set","component/fasteners/heavy",4,"forge stores, heavy shops, security repair benches","larger hardened bolts, locking pins, and reinforced clips","industrial input for heavy or military assemblies",false);
        add(m,"Grip frame","component/weapon/grip",3,"tool benches, armory scrap, civilian repair drawers","basic hand grip frame for pistols, tools, and compact weapons","industrial input for handheld weapons",false);
        add(m,"Pistol body","component/weapon/body",8,"armory benches, ganger workshops, security stores","compact weapon frame sized for pistols and holdout guns","industrial input for pistol-family weapons",false);
        add(m,"Carbine body","component/weapon/body",11,"militia racks, Guard repair benches, gang chop shops","short longarm frame for carbines and compact rifles","industrial input for carbine-family weapons",false);
        add(m,"Rifle body","component/weapon/body",15,"Guard armories, militia workshops, lawful security manufactories","full-length longarm frame with receiver, stock mount, and barrel channel","industrial input for rifle-family weapons",false);
        add(m,"Precision rifle body","component/weapon/body/precision",24,"marksman lockers, noble gun rooms, careful machine shops","stabilized receiver and stock frame for long-range weapons","industrial input for marksman weapons",false);
        add(m,"Heavy weapon body","component/weapon/body/heavy",36,"heavy armory cages, emplacement shops, military hardpoints","braced crew-weapon frame with recoil mounts and carrying lugs","industrial input for heavy weapons",false);
        add(m,"Scrap weapon body","component/weapon/body/improvised",5,"trash warrens, gang benches, maintenance refuse","crude weapon frame made from pipe, brackets, and stubborn optimism","industrial input for improvised weapons",false);
        add(m,"Trigger group","component/weapon/trigger",5,"armory bins, security repair benches, ganger workshops","trigger, sear, spring, selector, and small violence-permission parts","industrial input for firearms and powered tools",false);
        add(m,"Crude trigger group","component/weapon/trigger/improvised",2,"hiver benches, pipe weapon caches, trash warrens","rough trigger parts that work until confidence runs out","industrial input for improvised firearms",false);
        add(m,"Short barrel","component/weapon/barrel",6,"security workshops, pistol repair trays, gang stores","short pressure-rated barrel for compact solid-round weapons","industrial input for pistols and compact firearms",false);
        add(m,"Long barrel","component/weapon/barrel",10,"rifle racks, militia repair benches, noble hunting stores","long pressure-rated barrel for rifles and marksman weapons","industrial input for longarm firearms",false);
        add(m,"Reinforced barrel","component/weapon/barrel/heavy",16,"heavy shops, emplacement repair bays, military cages","thick barrel or sleeve for high pressure, heat, and recoil","industrial input for shotguns and heavy firearms",false);
        add(m,"Pressure-rated pipe barrel","component/weapon/barrel/improvised",4,"utility stores, pipe racks, sump workshops","pipe section chosen because it looks less likely to explode than the others","industrial input for improvised firearms",false);
        add(m,"Receiver block","component/weapon/receiver",9,"machine shops, armory stores, security repair rooms","machined receiver core for solid-projectile weapons","industrial input for auto, stub, and bolt weapons",false);
        add(m,"Magazine well","component/weapon/feed",4,"armory benches, gang repair drawers, security lockers","feed housing for magazines, clips, belts, or cartridges","industrial input for magazine-fed weapons",false);
        add(m,"Weapon stock frame","component/weapon/stock",5,"wood shops, plastics bins, weapon repair racks","stock or brace frame for long weapons","industrial input for carbines, rifles, and shotguns",false);
        add(m,"Optic mount","component/weapon/optic",7,"marksman benches, noble hunting rooms, security overwatch stores","rail, bracket, or scope mount for aiming devices","industrial input for precision weapons",false);
        add(m,"Focusing lens","component/las/lens",11,"Mechanicus optics trays, Guard las repair crates, noble vaults","treated optical lens for coherent energy weapons","industrial input for las and precision-energy weapons",false);
        add(m,"High-grade focusing lens","component/las/lens/precision",22,"Mechanicus optical shrines, sniper stores, rare trade","precision lens cut for long-range las work and expensive disappointment","industrial input for marksman las weapons",false);
        add(m,"Compact las emitter","component/las/emitter",12,"laspistol repair trays, Guard stores, stripped security lockers","short las emission assembly for compact weapons","industrial input for las pistols and crude laslocks",false);
        add(m,"Short las emitter barrel","component/las/emitter",15,"Guard repair benches, militia armories, security stores","carbine-length las barrel and emitter assembly","industrial input for las carbines",false);
        add(m,"Long las emitter barrel","component/las/emitter/long",20,"Guard armories, marksman lockers, Mechanicus custody","long las barrel and emitter channel for rifle-class weapons","industrial input for las rifles",false);
        add(m,"Stabilized las barrel","component/las/emitter/precision",32,"marksman cells, noble hunting galleries, Mechanicus optics labs","precision-stabilized las barrel for longlas and sniper patterns","industrial input for precision las weapons",false);
        add(m,"Las power capacitor","component/las/power",13,"Guard munition stores, Mechanicus repair trays, stripped cells","capacitor stack for las-family weapon discharge","industrial input for las weapons",false);
        add(m,"Charge regulator","component/energy/regulator",14,"Mechanicus diagnostic bays, military stores, electrical rooms","regulator that keeps stored energy from becoming a room event","industrial input for energy and plasma weapons",false);
        add(m,"Charger cell socket","component/energy/socket",5,"las repair bins, electrical shops, armory benches","socket and contacts for removable charge packs","industrial input for las and energy weapons",false);
        add(m,"Heat sink","component/cooling",7,"machine rooms, forge stores, power relay salvage","heat dissipation block for weapons and hard-worked machines","industrial input for heat-producing equipment",false);
        add(m,"Power-field coil","component/powerfield",26,"Mechanicus custody, noble weapon rooms, relic repair benches","field coil for powered melee weapon edges and impact heads","industrial input for power weapons",false);
        add(m,"Sanctified control housing","component/control/religious",18,"Mechanicus stores, relic cabinets, sanctioned armories","control casing with rites, seals, and sensible insulation","industrial input for controlled power systems",false);
        add(m,"Chain drive motor","component/chainweapon",16,"industrial saw repair, ganger chop shops, forge bins","compact drive motor for chain weapons and powered cutters","industrial input for chain weapons",false);
        add(m,"Chain teeth strip","component/chainweapon",9,"saw stores, cutter bins, gang workshops","linked cutting teeth for chain weapons and rock saws","industrial input for chain weapons",false);
        add(m,"Blade blank","component/blade",6,"forge racks, kitchen metal salvage, tool shops","unfinished blade stock waiting for edge, handle, and trouble","industrial input for knives and swords",false);
        add(m,"Heavy blade blank","component/blade/heavy",12,"forge floors, archaic weapon racks, heavy tool shops","large blade blank for axes, greatswords, and oversized statements","industrial input for heavy melee weapons",false);
        add(m,"Hammer head blank","component/blunt",8,"forge shops, rail yards, construction bins","dense striking head for mauls, hammers, and blunt doctrine","industrial input for blunt weapons",false);
        add(m,"Haft core","component/handle",4,"wood stores, tool repair bins, pipe racks","long handle, haft, or support core for tools and melee weapons","industrial input for hafted weapons",false);
        add(m,"Pressure chamber","component/pressure",10,"flamer shops, chem sprayer repair, industrial safety cages","sealed chamber for pressure, fuel, or gas systems","industrial input for sprayers, flamers, and shotguns",false);
        add(m,"Fuel canister mount","component/flame",8,"hazard lockers, flamer cages, industrial heating systems","bracket, valve seat, and safety latch for fuel canisters","industrial input for flame weapons",false);
        add(m,"Igniter assembly","component/flame",7,"kitchens, hazard cages, flamer repair stores","spark, wick, or pilot system for lighting things that should not be lit","industrial input for flame weapons",false);
        add(m,"Nozzle and valve set","component/fluid",9,"sanitation rooms, pressure washers, flamer shops","valves, nozzles, and hand controls for directed fluids","industrial input for sprayers and flame weapons",false);
        add(m,"Containment coil","component/plasma/melta",30,"Mechanicus vaults, high-energy labs, military special weapon cages","coil assembly for keeping high-energy reactions polite","industrial input for melta and plasma weapons",false);
        add(m,"Thermal regulator","component/melta",24,"anti-armor lockers, forge stores, sealed hot-work benches","regulator for managing thermal discharge without becoming included in it","industrial input for melta weapons",false);
        add(m,"Plasma containment flask","component/plasma",34,"Mechanicus sealed stores, plasma cages, rare wargear lockers","containment bottle and coupling for plasma systems","industrial input for plasma weapons",false);
        add(m,"Arc discharge coil","component/arc",20,"Mechanicus diagnostic bays, power relay salvage, cable chapels","coil for directed electrical discharge and workplace incidents","industrial input for arc weapons",false);
        add(m,"Toxin reservoir","component/toxin",13,"medicae black trade, sump chem rooms, assassin stores","sealed micro-reservoir for toxins, drugs, or corrosives","industrial input for needle and toxic weapons",false);
        add(m,"Web projector spool","component/security/web",18,"Arbites restraint lockers, security stores, noble capture teams","compressed webbing spool with controlled-issue markings","industrial input for web weapons",false);
        add(m,"Compressed-gas bottle","component/pressure",6,"construction stores, nailgun lockers, chem sprayer racks","small gas bottle for launching, spraying, or regretting","industrial input for nail rifles and sprayers",false);
        add(m,"Nail feed box","component/feed/improvised",3,"construction sites, ganger benches, tool lockers","box and crude feed lips for nails, rivets, or sharp junk","industrial input for nail rifles",false);
        add(m,"Spring scrap","component/spring/improvised",2,"broken furniture, machine guts, trash sorting tables","springs of uncertain temperament and useful tension","industrial input for improvised triggers",false);
        add(m,"Grip wrap","component/grip/improvised",1,"cloth piles, cable offcuts, trash warrens","cloth, tape, wire, or leather wrapped until a handle forms","industrial input for improvised weapons",false);
        add(m,"Cloth scrap","component/textile/scrap",1,"laundry bins, corpse piles, hab refuse, market sweepings","salvaged cloth suitable for wrapping, sealing, filtering, or making new misery portable","textile salvage input",false);

        add(m,"Tool frame","component/tool/frame",6,"workshop racks, maintenance lockers, stripped tool rooms","basic powered or hand-tool frame","industrial input for tool weapons and machinery",false);
        add(m,"Cutter head","component/tool/cutter",8,"rescue lockers, pipe shops, emergency equipment bays","replaceable cutter head for emergency tools and industrial weapons","industrial input for cutters and breachers",false);
        add(m,"Drill head","component/tool/drill",8,"mining lockers, repair stores, deep-work benches","boring head for drills and rock tools","industrial input for drills and mining tools",false);
        add(m,"Scrap plate","component/scrap",3,"trash warrens, salvage crates, collapsed rooms","flat salvaged plate suitable for crude reinforcement","industrial input for scrap assemblies",false);
        add(m,"Ferric scrap","material/raw/metal",2,"salvage yards, collapsed rooms, trash warrens, stripped rail fittings","mixed iron-bearing scrap waiting to be sorted into something less shameful","raw input for metal stock and scrap plates",false);
        add(m,"Refined metal stock","material/processed/metal",6,"forge stores, machine shops, rail depots, armory works","usable bar, strip, and plate stock for frames, receivers, and tool heads","processed input for industrial components",false);
        add(m,"Hardened metal stock","material/processed/metal/hardened",10,"armory heat-treat benches, military shops, forge cloisters","hardened stock for barrels, blades, springs, and things expected to survive impact","processed input for weapon-critical components",false);
        add(m,"Plasteel shaving bale","material/processed/plasteel",14,"high-grade salvage sorters, military repair cages, noble security workshops","bundled plasteel offcuts too valuable to leave in the sweepings","processed input for advanced casings and heavy components",false);
        add(m,"Industrial polymer sheet","material/processed/polymer",5,"packing works, hab fabrication benches, utility stores","durable plastic or resin sheet for housings, grips, seals, and ration packaging","processed input for casings and packaging",false);
        add(m,"Ceramic insulator blank","component/electrical/ceramic",7,"kilns, electronics benches, Mechanicus stores","unfired or finished ceramic shape used to keep current where doctrine placed it","electrical and heat insulation component",false);
        add(m,"Glass optic blank","component/optic/raw",8,"optics bins, noble repair benches, market glass cutters","clear blank suitable for lenses after grinding, polishing, and ritual squinting","raw optic input",false);
        add(m,"Conductive filament spool","component/electrical/wire",6,"electrical rooms, cable shrines, stripped machines","fine conductive filament for coils, contacts, and small electric arguments","electrical component input",false);
        add(m,"Insulation sleeve","component/electrical/insulation",4,"hab maintenance bins, forge wiring racks, utility stores","heat-resistant sleeve for wires and contact runs","electrical safety input",false);
        add(m,"Contact strip","component/electrical/contact",5,"battery lockers, las repair trays, electrical benches","spring contact strips for cells, sockets, and compact power systems","electrical contact component",false);
        add(m,"Capacitor wafer stack","component/electrical/capacitor",12,"Mechanicus diagnostic bays, las repair cages, electronics benches","stacked dielectric wafers for storing charge until a trigger makes poor choices","capacitor precursor component",false);
        add(m,"Circuit wafer","component/electronics/wafer",10,"data alcoves, cogitator repair benches, security nodes","simple control wafer for regulation, timing, and small machine obedience","electronics component input",false);
        add(m,"Data-link socket","component/electronics/socket",9,"security repair drawers, cogitator desks, Mechanicus bins","socket for data slates, weapon regulators, and machine conversations best kept brief","electronics component input",false);
        add(m,"Sensor crystal","component/electronics/sensor",15,"security sensors, auspex repair trays, noble alarm systems","small tuned crystal for detection and signal handling","sensor and optic component input",false);
        add(m,"Calibration screw set","component/precision/fasteners",5,"precision benches, optics drawers, Mechanicus repair kits","tiny screws and adjusters for machines that punish imprecision","precision assembly input",false);
        add(m,"Rubber gasket sheet","component/seal/raw",4,"sanitation stores, pump rooms, pressure benches","flexible gasket stock for seals, hoses, and pressure assemblies","raw seal input",false);
        add(m,"Sealing gasket set","component/seal",5,"pump rooms, pressure benches, water plants","rings, washers, and flat seals for pressure and fluid systems","seal input for pressure and fluid components",false);
        add(m,"Pipe coupling set","component/fluid/pipe",5,"utility closets, sewer works, rail service rooms","threaded couplings, reducers, and unions for making pipes agree","fluid and improvised barrel input",false);
        add(m,"Pressure hose bundle","component/fluid/hose",6,"sanitation carts, flamer repair, chemical sprayers","reinforced hoses for fuel, water, toxins, and other bad news under pressure","fluid transfer component",false);
        add(m,"Valve spring set","component/fluid/valve",4,"pump shops, flamer cages, pressure benches","springs sized for valves, regulators, and awkward maintenance silence","fluid control component",false);
        add(m,"Bearing set","component/mechanical/bearing",6,"machine rooms, rail depots, forge stores","bearings, bushings, and cups for things that rotate instead of scream","mechanical motion input",false);
        add(m,"Gear train","component/mechanical/gear",8,"machine shops, tool repair benches, servo wrecks","matched gears and shafts for compact transfer of effort and regret","mechanical drive component",false);
        add(m,"Motor coil pack","component/mechanical/motor",10,"powered tool repair, forge benches, electric pump wrecks","coil pack for compact motors and actuator drives","motor and powered-tool input",false);
        add(m,"Micro-actuator","component/mechanical/actuator",11,"security nodes, servo-skull wreckage, Mechanicus repair trays","small actuator for locks, tools, regulators, and tiny betrayals","precision mechanical component",false);
        add(m,"Servo linkage","component/mechanical/servo",13,"servitor wrecks, factory arms, Mechanicus salvage drawers","joint and linkage set for small articulated machinery","servo and machine-control component",false);
        add(m,"Pump impeller","component/mechanical/pump",9,"water plants, sump pumps, chem sprayers","impeller and shaft assembly for moving fluids through places fluids should not be","pump component input",false);
        add(m,"Gearbox casing","component/mechanical/casing",9,"tool shops, chain-weapon benches, machine rooms","compact casing for gears and bearings under load","mechanical housing input",false);
        add(m,"Hardened spring set","component/spring",5,"machine guts, armory trays, rail repair lockers","springs with a chance of behaving the same way twice","spring input for weapons and tools",false);
        add(m,"Recoil spring assembly","component/weapon/recoil",8,"armory benches, gun repair trays, militia lockers","spring and guide assembly for cycling solid-projectile weapons","firearm recoil component",false);
        add(m,"Firing pin set","component/weapon/ignition",6,"gun repair drawers, security stores, ganger benches","firing pins, strikers, and tiny metal decisions","solid-projectile ignition component",false);
        add(m,"Extractor claw set","component/weapon/extractor",6,"armory trays, security repair rooms, gang chop shops","claws, ejectors, and little hooks for removing spent cases before panic","solid-projectile extraction component",false);
        add(m,"Bolt carrier assembly","component/weapon/boltcarrier",12,"military repair benches, heavy weapon cages, militia stores","carrier and locking parts for automatic or heavy firearm actions","advanced firearm component",false);
        add(m,"Feed pawl assembly","component/weapon/feed/heavy",10,"heavy weapon lockers, turret repair, factory belts","feed pawls and belt guides for moving ammunition into consequences","heavy-feed component",false);
        add(m,"Breech seal","component/weapon/breech",8,"shotgun stores, autocannon cages, flamer pressure benches","seal or locking face for chambers that would otherwise vent drama","pressure/breech component",false);
        add(m,"Gas piston assembly","component/weapon/gas",9,"autogun repair trays, military stores, gang workshops","gas piston and return hardware for automatic weapons","automatic firearm component",false);
        add(m,"Pistol firearm action","component/weapon/action/pistol",13,"pistol repair trays, security armories, ganger benches","compact receiver action group for pistols and small stub weapons","pistol action component",false);
        add(m,"Rifle firearm action","component/weapon/action/rifle",18,"rifle repair benches, militia armories, autogun shops","rifle action group with carrier, piston, pin, and extractor","rifle action component",false);
        add(m,"Heavy firearm action","component/weapon/action/heavy",26,"heavy weapon cages, turret workshops, military depots","oversized action group for belt-fed or heavy solid weapons","heavy weapon action component",false);
        add(m,"Heavy feed action group","component/weapon/feed/heavy-group",16,"emplacement stores, heavy stubber benches, munition cages","feed pawls, belt path, and reinforced feed housing","heavy weapon feed component",false);
        add(m,"Bolt weapon action","component/weapon/action/bolt",28,"controlled munition armories, noble hard stores, Arbites repair cages","reinforced action group for bolt weapons and bolt recoil behavior","bolt weapon action component",false);
        add(m,"Shotgun breech action","component/weapon/action/shotgun",14,"shotgun repair trays, Arbites stores, ganger benches","breech and striker group for shotguns and pipe scatter weapons","shotgun action component",false);
        add(m,"Revolver cylinder assembly","component/weapon/action/revolver",12,"old security drawers, ganger reload benches, frontier-style armories","cylinder, pawl, and timing group for revolvers","revolver action component",false);
        add(m,"Heavy weapon recoil cradle","component/weapon/recoil-cradle",22,"heavy armory cages, turret hardpoints, vehicle repair bays","cradle and buffer assembly for heavy portable weapons","heavy recoil component",false);
        add(m,"Military weapon casing","component/weapon/casing/military",12,"Guard armories, sanctioned security shops, quartermaster repair benches","standardized rugged casing for issued weapons","weapon casing component",false);
        add(m,"Civilian weapon casing","component/weapon/casing/civilian",8,"hunting stores, hab security shops, civilian repair counters","simpler legal-ish casing for civilian longarms and security weapons","weapon casing component",false);
        add(m,"Improvised weapon casing","component/weapon/casing/improvised",4,"trash warrens, gang workshops, hivers' benches","bent scrap casing for weapons that apologize in advance","improvised casing component",false);
        add(m,"Compact charge cradle","component/energy/cradle/compact",8,"laspistol trays, electronics bins, security repair lockers","compact cradle for removable charge cells","compact energy component",false);
        add(m,"Plasma discharge assembly","component/plasma/discharge",48,"Mechanicus plasma benches, sealed military cages, forbidden repair trays","plasma throat, injector, and bottle group","plasma weapon component",false);
        add(m,"Melta discharge assembly","component/melta/discharge",46,"anti-armor weapon lockers, Mechanicus custody, hot-work vaults","melta focusing and discharge group","melta weapon component",false);
        add(m,"Heavy energy heat exchanger","component/energy/heat-exchanger/heavy",30,"plasma cages, melta repair benches, coolant stores","large heat exchanger for high-energy weapons","heavy energy component",false);
        add(m,"Pressure weapon hose harness","component/pressure/hose-harness",13,"flamer shops, sanitation stores, chemical sprayer benches","reinforced hose and valve harness for directed fluids and fuel","pressure weapon component",false);
        add(m,"Needle delivery assembly","component/toxin/needle-delivery",18,"assassin stores, medicae black trade, noble security drawers","needle and ampoule delivery group for quiet weapons","toxin weapon component",false);
        add(m,"Web discharge assembly","component/security/web-discharge",22,"Arbites restraint lockers, noble capture stores, security repair bays","web compound and projector discharge group","webber component",false);
        add(m,"Arc discharge head","component/arc/discharge-head",18,"Mechanicus diagnostic stores, shock weapon benches, cable chapels","prong and coil head for directed arc discharge","arc weapon component",false);
        add(m,"Field emitter array","component/powerfield/emitter-array",28,"power weapon benches, noble armories, Mechanicus relic repair","distributed field emitters for powered melee heads and edges","power-field component",false);
        add(m,"Force focus lattice","component/force/lattice",40,"sanctioned psyker stores, sealed reliquaries, forbidden occult caches","focus lattice for force weapons and sanctioned psychic instruments","force weapon component",false);
        add(m,"Mono-edge strip","component/blade/mono-edge",14,"precision forges, noble weapon benches, assassin stores","fine mono-edge strip for high-quality cutting weapons","mono-blade component",false);
        add(m,"Toxin delivery channel","component/blade/toxin-channel",13,"toxin cabinets, assassin repair kits, sump chem benches","blade channeling for toxins and bio-active agents","toxin blade component",false);
        add(m,"Shock head assembly","component/shock/head",14,"Arbites shock stores, electrical rooms, baton repair trays","contact head and prong group for shock melee weapons","shock weapon component",false);
        add(m,"Chain cutter rail assembly","component/chainweapon/cutter-rail",18,"chain weapon benches, industrial saw shops, ganger chop shops","guide rail, tensioner, and chain path assembly","chain weapon component",false);
        add(m,"Heavy cutter carriage","component/tool/cutter/heavy-carriage",18,"rock saw stores, industrial cutter racks, mining repair benches","heavy carriage for saws, cutters, and drill violence","heavy cutter component",false);
        add(m,"Cartridge casing batch","component/ammo/casing",5,"press rooms, armory reload benches, brass salvage tubs","batch of reusable or newly drawn casings for solid ammunition","ammo precursor",false);
        add(m,"Primer cap tray","component/ammo/primer",4,"munition benches, chemical stores, illicit reload shops","tray of primer caps handled with the usual local optimism","ammo precursor",false);
        add(m,"Propellant charge","component/ammo/propellant",7,"munition stores, chem rooms, Guard reload cages","measured propellant charge for cartridges, shells, and risky demonstrations","ammo precursor",false);
        add(m,"Shot pellet pouch","component/ammo/shot",4,"shotgun lockers, scrap presses, ganger benches","pouch of pellets, fragments, or properly malicious small metal","shotgun ammo precursor",false);
        add(m,"Stub slug batch","component/ammo/stub",5,"ganger reload benches, hab security stores, pawn ammo drawers","batch of stub slugs ready for casing and complaint","stub ammo precursor",false);
        add(m,"Autogun round batch","component/ammo/auto",7,"militia reload stores, gang workshops, Guard munition overflow","batch of rifle rounds for auto-family weapons","auto ammo precursor",false);
        add(m,"Bolt penetrator core","component/ammo/bolt",18,"controlled munition cages, noble vaults, Arbites hard stores","dense core for bolt rounds, expensive enough to count as policy","bolt ammo precursor",false);
        add(m,"Gyrojet propellant pellet","component/ammo/bolt",16,"controlled chem stores, military munition benches, Mechanicus custody","miniature propellant pellet for bolt ammunition and loud bookkeeping","bolt ammo precursor",false);
        add(m,"Shell crimp ring","component/ammo/shell",5,"shotgun reload trays, autocannon cages, munition benches","rings, crimps, and collars for holding shells together until impact","shell ammo precursor",false);
        add(m,"Needle lancet bundle","component/toxin/needle",9,"medicae drawers, assassin kits, noble security stores","bundle of fine needles for toxin weapons and quiet malpractice","needle weapon component",false);
        add(m,"Toxin ampoule tray","component/toxin/ampoule",10,"medicae black trade, sump chem labs, cult poison drawers","tray of sealed ampoules holding toxins, sedatives, or lies","toxin delivery component",false);
        add(m,"Web compound tub","component/security/web/compound",12,"Arbites restraint stores, noble capture teams, chemical lockers","tub of adhesive compound for sanctioned restraint and unsanctioned humiliation","web weapon component",false);
        add(m,"Promethium gel canister","component/flame/fuel",14,"fuel cages, Guard stores, industrial heater rooms","gelled fuel canister for flamers and awful problem solving","flame fuel component",false);
        add(m,"Promethium valve cartridge","component/flame/valve",9,"flamer repair benches, fuel cages, hazard lockers","valved cartridge head for controlled promethium feed","flame control component",false);
        add(m,"Ignition wick bundle","component/flame/ignition",4,"kitchens, forge stores, flamer repair drawers","wicks, sparking cords, and pilot bits for ignition systems","ignition precursor",false);
        add(m,"Melta focusing crystal","component/melta/focusing",28,"anti-armor stores, Mechanicus custody, rare crystal drawers","focusing crystal for melta discharge alignment","melta component",false);
        add(m,"Thermal baffle plate","component/melta/heat",18,"hot-work benches, melta stores, forge cages","baffled plate set for keeping heat on the enemy side of the weapon","melta heat component",false);
        add(m,"Magnetic bottle ring","component/plasma/bottle",26,"plasma cages, Mechanicus sealed racks, high-energy labs","ring assembly for magnetic containment fields","plasma containment component",false);
        add(m,"Plasma injector nozzle","component/plasma/injector",24,"plasma weapon cages, Mechanicus labs, forbidden repair trays","injector and throat assembly for plasma discharge","plasma emission component",false);
        add(m,"Plasma coolant cartridge","component/plasma/cooling",18,"plasma lockers, coolant stores, high-energy labs","sealed coolant cartridge for weapons that consider ventilation optional","plasma cooling component",false);
        add(m,"Arc prong set","component/arc/prong",9,"diagnostic probe kits, cable chapels, Mechanicus drawers","forked prongs and contacts for directed arc discharge","arc weapon component",false);
        add(m,"Power coupling socket","component/energy/coupling",10,"las repair benches, power rooms, machine shops","coupling socket between capacitors, regulators, and emitters","energy coupling component",false);
        add(m,"Field emitter stud","component/powerfield/emitter",14,"noble weapon rooms, Mechanicus power-field benches, relic drawers","small emitter stud for powered weapon edges and heads","power-field component",false);
        add(m,"Chain guide rail","component/chainweapon/rail",8,"saw benches, chain-weapon shops, ganger cutters","guide rail for keeping a moving chain near the intended victim","chain weapon component",false);
        add(m,"Chain tensioner","component/chainweapon/tension",7,"tool repair bins, chain-weapon benches, industrial saw lockers","tensioner assembly for saw chain and chain weapons","chain weapon component",false);
        add(m,"Cutting tooth blank","component/chainweapon/tooth",4,"saw shops, forge trays, heavy cutter bins","unfinished cutting teeth before hardening and installation","chain/saw tooth precursor",false);
        add(m,"Saw chain link set","component/chainweapon/link",5,"industrial saw repair, gang workshops, cutter stores","chain links sized for powered cutting tools","chain weapon precursor",false);
        add(m,"Blade edge strip","component/blade/edge",5,"forge racks, sharpening benches, old tool stock","edge strip to weld, pin, or grind onto blade blanks","blade finishing component",false);
        add(m,"Handle core blank","component/handle/core",3,"wood shops, pipe racks, furniture salvage, tool stores","blank for grips, hafts, and handles of varying dignity","handle precursor",false);
        add(m,"Weighted pommel","component/handle/pommel",4,"weapon benches, tool shops, noble dueling stores","counterweight or pommel for balance, impact, and dramatic pointing","melee finishing component",false);
        add(m,"Shock grip sleeve","component/grip/shock",6,"Arbites repair benches, electrical rooms, baton lockers","insulated grip sleeve for shock batons and arc tools","shock weapon component",false);
        add(m,"Drill chuck","component/tool/drill/chuck",6,"tool repair benches, mining lockers, maintenance closets","chuck and collar for holding drill heads under unhappy load","drill tool component",false);
        add(m,"Cutter armature","component/tool/cutter/armature",7,"rescue lockers, pipe cutter stores, rail maintenance bays","armature and bracket for powered cutter heads","cutter tool component",false);
        add(m,"Mining tooth bit","component/tool/mining",8,"mining lockers, rock saw stores, excavation benches","hardened bit or tooth used against stone, ore, and stubborn infrastructure","mining/cutting component",false);
        add(m,"Filter cartridge housing","component/water/filter",6,"water plants, canteen workshops, reclamation benches","housing for layered filter media and membranes","water-processing component",false);
        add(m,"Charcoal filter bed","component/water/filter/media",3,"kitchens, burned scrap, purifier stores","charcoal media for removing some of the water's accusations","water-processing input",false);
        add(m,"Sand filter pack","component/water/filter/media",2,"sump works, utility stores, raw earth sorting","graded sand and grit packed for slow filtration","water-processing input",false);
        add(m,"Ceramic filter candle","component/water/filter/ceramic",7,"water guild stores, kilns, utility closets","porous ceramic filter element for canteens and reclamation racks","water-processing component",false);
        add(m,"Reclamation membrane","component/water/membrane",12,"recycler plants, chem stores, utility vaults","thin membrane for separating clean-ish water from civic reality","water-processing component",false);
        add(m,"Distillation coil","component/water/distillation",9,"still rooms, medicae clean benches, chem processors","coiled condenser line for distilling water or less respectable fluids","distillation component",false);
        add(m,"Sterile vial rack","component/medical/container",6,"medicae rooms, labs, clean stores","rack of sterile vials for medicine, toxins, and careful liquids","medical/chemical packaging component",false);
        add(m,"Nutrient salt packet","component/agriculture/nutrient",4,"hydroponic stores, ration plants, sump farms","packet of salts and minerals plants pretend to appreciate","agricultural nutrient input",false);
        add(m,"Seed culture tray","component/agriculture/seed",6,"hydroponic farms, noble bio-gardens, ration greenhouses","tray of seed cultures kept alive by regulation and artificial hope","agricultural input",false);
        add(m,"Algae starter culture","component/agriculture/algae",5,"algae vats, reclamation tanks, synthetic food works","living starter culture for synthetic nutrient production","vat agriculture input",false);
        add(m,"Fungus starter mat","component/agriculture/fungus",5,"sump farms, sewer niches, ration works","fungal mat ready to colonize trays and weak policy","fungus agriculture input",false);
        add(m,"Hydroponic growth tray","component/agriculture/tray",8,"hydroponic racks, farm stores, noble gardens","growth tray with channels, clips, and roots' last known address","hydroponic facility component",false);
        add(m,"Artificial sun-lamp tube","component/agriculture/light",16,"noble gardens, hydroponic decks, maintenance stores","lamp tube tuned to convince plants the ceiling is a star","controlled agriculture component",false);
        add(m,"Fermentation yeast culture","component/food/fermentation",5,"still rooms, kitchens, orchard stores","yeast culture for turning fruit and grain into commerce with headaches","fermentation input",false);
        add(m,"Mash tun liner","component/food/processing",7,"distilleries, food plants, kitchen machine rooms","lined vessel insert for controlled mash, slurry, and questionable brewing","food-processing component",false);
        add(m,"Food-safe sealant","component/food/packaging",4,"ration plants, kitchens, medicae stores","sealant allegedly safe enough for packages and desperate people","food packaging input",false);
        add(m,"Ration wrapper roll","component/food/packaging",3,"ration plants, printing shops, quartermaster stores","printed wrapper roll for making food look intentional","ration packaging input",false);
        add(m,"Tin can sleeve","component/food/packaging",4,"canneries, ration plants, scrap presses","formed tin sleeve for ration cans, chemical tins, and sealed disappointments","food packaging input",false);
        add(m,"Preservative salt packet","component/food/preservative",3,"ration plants, kitchens, hydroponic stores","salt and preservative blend for extending shelf life and melancholy","food-preservation input",false);
        add(m,"Protein binder paste","component/food/binder",4,"nutrient vats, ration plants, algae works","sticky binder that turns slurry into something with a defined edge","synthetic food input",false);
        add(m,"Starch flour sack","component/food/starch",4,"grain mills, hydroponic stores, ration plants","starch flour for ration bricks, porridge, and excuses called bread","food-processing input",false);
        add(m,"Ploin fruit pulp","component/food/fruit",7,"ploin stores, void provisioners, hydroponic fruit racks","pulp from lopsided ploin fruit, rich in vitamins and logistic smugness","fruit drink input",false);
        add(m,"Amino additive vial","component/food/amino",6,"military kitchens, medicae stores, nutrient vats","concentrated amino additive for high-efficiency body fuel","military food input",false);
        add(m,"Wastewater","water/raw/waste",1,"sanitation sluices, reclamation tanks, sump drains","water with enough contents to require optimism and filters","raw water input for reclamation",false);
        add(m,"Sump sludge","water/raw/sludge",1,"sump pools, sewer rooms, mutant camps","sludge containing water, toxins, minerals, and possible opinions","raw reclamation input",false);
        add(m,"Dirty water","water/raw",1,"canteens, drains, open cisterns, sump traders","water that has not yet earned trust","raw water input",false);
        add(m,"Filtered water","water/processed",2,"filters, condensers, reclamation stills","water with most visible crimes removed","processing input for potable water",false);
        add(m,"Potable water","water/processed",3,"hab taps, treated cisterns, utility plants","drinkable water by local standards and local desperation","consume/use as food and manufacturing input",false);
        add(m,"Distilled water","water/industrial",5,"labs, medicae rooms, fine manufactories","clean processed water useful where minerals and mystery are unwelcome","industrial input for medicine, optics, and electronics",false);
        add(m,"Raw earth","material/raw",2,"excavation sites, planter bins, collapsed rooms, sump farms","mineral soil and grit scraped from the hive body","raw input for fertilizer and ceramics",false);
        add(m,"Waste biomass","organic/waste",1,"kitchens, corpse recovery, algae vats, fungus farms","organic waste stream before someone calls it food again","raw input for fertilizer and vat production",false);
        add(m,"Fertilizer","agriculture/input",3,"reclamation tanks, sump farms, hydroponic racks","processed nutrients for growing things in places things should not grow","input for agriculture and hydroponics",false);
        add(m,"Vat nutrient slurry","food/raw/synthetic",3,"nutrient vats, algae tanks, reclamation works","raw slurry for making synthetic food with administrative confidence","input for ration and porridge production",false);
        add(m,"Fruit mash","food/raw/luxury",10,"orchard stillhouses, noble bio-gardens, market bruised-fruit bins","fermentable fruit mash with actual flavor trying to escape","input for juice and amasec",false);
    }


    private static void clothingArmorTextileComponentItems(LinkedHashMap<String,ItemDef> m){
        // 0.8.92a CLOTHING / ARMOR / TEXTILE COMPONENT EXPANSION:
        // These entries create a reusable garment and armor vocabulary before clothing recipes are promoted.
        // They remain catalog + draft-decomposition material, not a player-facing workbench flood.
        add(m,"Textile fiber bale","component/textile/fiber",2,"laundry vats, waste reclamation, fungus farms, hab fiber spinners","basic fiber stock for cloth, padding, thread, and recycled garments","textile precursor input",false);
        add(m,"Reclaimed textile bundle","component/textile/reclaimed",2,"laundry refuse, corpse piles, charity bins, ganger floors","salvaged fabric sorted into something the needle can tolerate","textile salvage input",false);
        add(m,"Coarse cloth roll","component/textile/cloth",4,"hab looms, workwear stores, civilian manufactories","plain durable cloth for ordinary clothing and uniforms","garment shell input",false);
        add(m,"Treated canvas roll","component/textile/canvas",6,"rail depots, hazard stores, cargo tarpaulin shops","heavy treated cloth for coats, aprons, straps, and rugged workwear","heavy garment input",false);
        add(m,"Synthweave sheet","component/textile/synthweave",14,"noble ateliers, military stores, Mechanicus fiber benches","advanced woven sheet with better strength and social implications","high-grade garment and armor input",false);
        add(m,"Rubberized fabric","component/textile/rubberized",7,"sewer gear stalls, hazard shops, pressure-suit repair benches","cloth bonded with sealant for wet, toxic, or generally offensive places","hazard garment input",false);
        add(m,"Leather substitute sheet","component/textile/leatherette",5,"market tailors, gang workshops, upholstery salvage","synthetic hide substitute used where real leather is too expensive or too traceable","coat, boot, and strap input",false);
        add(m,"Hide strip bundle","component/textile/hide",5,"mutant camps, sump hunters, wall-rat processors","tanned strips of hide suitable for straps, mantles, and bad smell management","primitive garment and armor input",false);
        add(m,"Thread spool","component/textile/thread",1,"looms, laundry rooms, hab sewing desks","thread stock for stitching cloth into social class","garment assembly input",false);
        add(m,"Wire stitching spool","component/textile/wire-stitching",3,"gang benches, Mechanicus repair kits, hazard-suit shops","thread reinforced with wire for repairs that expect violence","rugged garment and armor stitching input",false);
        add(m,"Fastener button card","component/garment/fasteners",2,"tailor counters, workwear bins, hab repair drawers","buttons, snaps, hooks, and tiny dignity anchors","garment closure input",false);
        add(m,"Buckle and clasp set","component/garment/buckles",3,"belt makers, armor shops, cargo harness bins","buckles, clasps, and closure hardware for straps and armor","strap and harness input",false);
        add(m,"Webbing strap roll","component/garment/webbing",4,"Guard stores, cargo depots, riot gear shops","woven strap stock for harnesses, webbing, pouches, and field kit","uniform and harness input",false);
        add(m,"Padding layer","component/garment/padding",3,"laundry salvage, armor repair, workwear manufactories","quilted padding for coats, armor backing, and elbows with enemies","soft protection input",false);
        add(m,"Insulation batting","component/garment/insulation",4,"void workrooms, cold-storage stores, hazard gear racks","thermal and electrical insulation layer for dangerous workwear","environmental protection input",false);
        add(m,"Filter cloth roll","component/garment/filter-cloth",4,"rebreather shops, sanitation stores, medicae bins","filter-grade cloth for masks, respirators, and not dying of air","mask and filter input",false);
        add(m,"Sealant strip roll","component/garment/sealant-strip",4,"pressure-suit repair, pump rooms, hazard lockers","flexible strips for sealed seams and patched suits","sealed garment input",false);
        add(m,"Pressure gasket set","component/garment/pressure-seal",6,"void-suit benches, pump rooms, hazard stores","gaskets for collars, gloves, boots, masks, and pressure seams","sealed suit input",false);
        add(m,"Visor lens","component/garment/visor",8,"Arbites stores, hardhat repair, noble protection ateliers","curved or flat lens for face protection and suspicious authority","helmet and mask input",false);
        add(m,"Rebreather filter","component/garment/rebreather",7,"sanitation stores, medicae shops, sump traders","replaceable filter cartridge for masks, hoods, and sealed work suits","respiratory protection input",false);
        add(m,"Helmet shell","component/armor/helmet-shell",8,"hardhat bins, flak repair cages, riot gear stores","formed shell for helmets, hard hats, and skull negotiations","head protection input",false);
        add(m,"Hood pattern cut","component/garment/hood",3,"tailor benches, cult sewing rooms, Mechanicus robe racks","pre-cut hood panels for robes, cloaks, and identity reduction","hood and robe input",false);
        add(m,"Coat shell","component/garment/coat-shell",6,"workwear shops, noble tailors, gang coat piles","cut panels for coats, jackets, greatcoats, and portable class signals","coat and jacket input",false);
        add(m,"Robe shell","component/garment/robe-shell",6,"Mechanicus robe stores, cult cells, hab tailors","long garment shell for robes, wraps, and ritual fabric overcommitment","robe and wrap input",false);
        add(m,"Workwear panel set","component/garment/workwear-panel",5,"hab manufactories, depot stores, maintenance closets","pre-cut durable panels for overalls, fatigues, and utility trousers","workwear garment input",false);
        add(m,"Boot sole set","component/garment/boot-sole",4,"cargo depots, cobbler stalls, reclamation rubber stores","paired soles for boots, waders, and feet with plans","boot assembly input",false);
        add(m,"Glove palm set","component/garment/glove-palm",3,"workwear bins, medicae stores, hazard lockers","cut palm pieces for gloves and gauntlet liners","glove assembly input",false);
        add(m,"Armor backing","component/armor/backing",7,"flak repair cages, workwear shops, militia stores","padded backing layer that keeps armor from becoming internal architecture","soft armor backing input",false);
        add(m,"Flak weave panel","component/armor/flak-weave",13,"Guard quartermaster stores, militia repair benches, fabric armor shops","layered ballistic weave for fragment and small-round protection","flak armor input",false);
        add(m,"Mesh armor panel","component/armor/mesh",18,"noble security ateliers, black-market armor benches","flexible mesh panel for concealed or high-status protection","concealable armor input",false);
        add(m,"Scrap metal plate","component/armor/scrap-plate",4,"trash warrens, gang armor benches, collapsed works","flat ugly plate used by people who cannot afford stamped armor","scrap armor input",false);
        add(m,"Stamped armor plate","component/armor/stamped-plate",12,"security manufactories, Guard repair cages, Arbites stores","formed metal armor plate with repeatable geometry and institutional confidence","rigid armor input",false);
        add(m,"Ceramic armor tile","component/armor/ceramic-tile",16,"military armor shops, noble security vaults, Mechanicus kilns","ceramic insert for high-grade protection without admitting fear","advanced armor input",false);
        add(m,"Carapace plate","component/armor/carapace",24,"Arbites riot cages, noble armories, military hard stores","heavy rigid armor segment for people authorized to survive first","heavy armor input",false);
        add(m,"Rivet set","component/armor/rivets",2,"armor benches, gang floors, rail repair stores","rivets for attaching plates, patches, and questionable decisions","armor fastening input",false);
        add(m,"Armor harness webbing","component/armor/harness-webbing",5,"Guard stores, cargo depots, gang armor benches","load-bearing webbing cut for armor suspension and pouches","armor harness input",false);
        add(m,"Shock-absorber padding","component/armor/impact-padding",7,"riot gear shops, cargo safety stores, tunnel workrooms","thicker impact padding for blunt trauma and wall introductions","impact armor input",false);
        add(m,"Concealment lining","component/garment/concealment",6,"runner shops, noble tailors, smuggler laundries","pocketed lining for hiding weapons, papers, or inconvenient truths","concealed garment input",false);
        add(m,"Tailored garment liner","component/garment/tailored-liner",10,"noble ateliers, high-end laundries, luxury hab servants","smooth liner that makes clothing expensive before it becomes useful","luxury garment input",false);
        add(m,"Decorative trim set","component/garment/trim",7,"noble trim shops, uniform stores, cult robe rooms","braid, edging, colored cloth, beads, and other wearable hierarchy","decorative garment input",false);
        add(m,"Faction insignia patch","component/faction/patch",5,"uniform stores, gang stencil rooms, Administratum issue desks","patch or badge declaring who gets blamed for the wearer","faction-marking input",false);
        add(m,"Rank tab set","component/faction/rank-tabs",6,"Guard depots, Arbites precincts, noble livery stores","rank markers for uniforms, coats, and social danger","rank-marking input",false);
        add(m,"House livery ribbon set","component/faction/house-livery",9,"noble tailors, household stores, ceremonial wardrobes","ribbon and sash stock for noble household colors","noble faction-marking input",false);
        add(m,"Purity seal backing","component/faction/purity-seal",8,"Mechanicus scriptoria, shrine stalls, parchment cutters","wax-ready backing for machine and robe devotional tags","Mechanicus/cult marking input",false);
        add(m,"Bone charm string","component/faction/bone-charm",4,"mutant camps, cult pits, sewer shrines","small bone-and-cord decoration for armor that has opinions","mutant/cult marking input",false);
        add(m,"Tire-rubber slab","component/armor/rubber-slab",3,"cargo yards, vehicle wreckage, ganger workshops","thick rubber slab cut from transport waste and civic neglect","improvised impact armor input",false);
    }

    private static void clothingArmorVarietyItems(LinkedHashMap<String,ItemDef> m){
        // First broad clothing/armor variety wave: final goods for social identity, protection, trade, and later disguise logic.
        add(m,"Worker fatigues","clothing/workwear",7,"hab laundries, worker stores, manufactory lockers","plain utility fatigues for people expected to be useful and replaceable","USE equips as worker clothing/disguise",false);
        add(m,"Utility trousers","clothing/workwear",4,"workwear bins, dorm lockers, market stalls","durable trousers with pockets sized for wage labor and regret","USE equips as clothing",false);
        add(m,"Reclaimed work shirt","clothing/workwear/reclaimed",3,"laundry salvage, charity bins, corpse recovery","work shirt assembled from fabric with previous employment history","USE equips as low-status clothing",false);
        add(m,"Patchwork coat","clothing/coat/reclaimed",5,"sump markets, scavenger camps, hab refuse","coat stitched from mismatched cloth and stubborn survival","USE equips as underhive clothing/disguise",false);
        add(m,"Industrial apron","clothing/workwear/protective",6,"machine shops, kitchens, forge-adjacent lockers","heavy apron against sparks, grease, and reasonable expectations","USE equips as protective workwear",false);
        add(m,"Hazard gloves","clothing/protective/gloves",5,"hazard lockers, sanitation rooms, medicae stores","gloves for substances that should not meet skin or optimism","USE equips as hand protection",false);
        add(m,"Work boots","clothing/workwear/boots",6,"worker stores, cargo depots, barracks spare racks","sturdy boots for metal floors and workplace shoving","USE equips as foot protection",false);
        add(m,"Sump waders","clothing/protective/waders",8,"sump markets, sanitation lockers, sewer camps","rubberized waders for water that should have been a crime","USE equips as sewer protection",false);
        add(m,"Respirator hood","clothing/protective/hood",9,"sanitation stores, hazard rooms, sump traders","hood and filter rig for bad air and worse neighborhoods","USE equips as respiratory protection",false);
        add(m,"Hard hat","armor/head/work",5,"construction lockers, rail depots, maintenance rooms","formed work helmet for overhead violence and falling policy failures","USE equips as light head protection",false);
        add(m,"Maintenance harness","clothing/harness/work",6,"utility closets, lift shafts, cargo yards","strap harness for tools, pouches, and falling less dramatically","USE equips as work harness",false);

        add(m,"Gang jacket","clothing/gang",8,"gang crash rooms, chop shops, extortion closets","jacket with colors, patches, and enough swagger to get searched","USE equips as gang disguise",false);
        add(m,"Patch armor","armor/scrap/soft",9,"gang benches, riot piles, scavenger markets","padded patch armor made from cloth, plate scraps, and bad patterns","USE equips as improvised armor",false);
        add(m,"Scrap plate harness","armor/scrap/rigid",12,"gang workshops, mutant camps, trash warrens","webbing harness carrying scrap plates over vital disappointment","USE equips as scrap armor",false);
        add(m,"Chain-wrapped bracers","armor/scrap/bracers",7,"gang lockers, fighting pits, chain piles","forearm wraps of chain, cloth, and theatrical threat","USE equips as arm protection",false);
        add(m,"Tire-rubber shoulder guard","armor/scrap/rubber",6,"vehicle wrecks, cargo yards, gang armor benches","shoulder guard cut from tire rubber and underhive initiative","USE equips as improvised shoulder armor",false);
        add(m,"Spiked mask","clothing/gang/mask",7,"gang intimidators, fighting pits, stolen costume trunks","mask built to solve social problems by worsening them","USE equips as intimidation mask/disguise",false);
        add(m,"Rebreather scarf","clothing/protective/scarf",6,"sump traders, runner caches, sanitation bins","filter cloth hidden in scarf form for breathing and lying","USE equips as light respiratory cover",false);
        add(m,"Sump cloak","clothing/cloak/sump",7,"sewer camps, scavenger stalls, fungus markets","heavy cloak with water resistance and olfactory ambitions","USE equips as sump disguise/protection",false);
        add(m,"Scavenger hood","clothing/hood/scavenger",4,"trash warrens, salvage crews, sewer camps","simple hood that hides grime by joining it","USE equips as scavenger disguise",false);
        add(m,"Rag-wrapped boots","clothing/boots/reclaimed",3,"trash warrens, mutant camps, desperate dorms","boots rebuilt with rags where soles and pride failed","USE equips as poor foot protection",false);

        add(m,"Hide mantle","clothing/mutant/hide",7,"mutant warrens, sump hunting camps, bone shrines","hide mantle marked by smoke, teeth, and distance from laundries","USE equips as mutant/sump disguise",false);
        add(m,"Bone-studded harness","armor/mutant/scrap",10,"mutant camps, bone piles, sewer fighting dens","strap harness decorated and reinforced with bone and scrap","USE equips as mutant improvised armor",false);
        add(m,"Cult robe","clothing/contraband/robe",9,"cult cells, hidden chapels, sleeper crypts","robe cut for secrecy, ritual, and immediate suspicion","USE equips as contraband disguise",false);
        add(m,"Ritual hood","clothing/contraband/hood",6,"cult sewing rooms, shrine caches, sleeper cells","hood that helps the wearer become a rumor with hands","USE equips as cult disguise",false);
        add(m,"Blasphemous mask","clothing/contraband/mask",10,"cult pits, forbidden shrines, evidence lockers","mask with symbols that make paperwork turn hostile","USE equips as dangerous disguise",false);
        add(m,"Sacrificial apron","clothing/contraband/apron",8,"ritual kitchens, cult cells, evidence cages","apron with stains that found religion and kept it","USE equips as cult workwear",false);
        add(m,"Scrap idol armor","armor/contraband/scrap",14,"cult shrines, mutant-cult caches, forbidden workshops","scrap armor covered in charms, icons, and prosecution evidence","USE equips as cult armor",false);
        add(m,"Filth-wrapped cloak","clothing/mutant/cloak",4,"mutant warrens, sewer nests, abandoned camps","cloak of rags, hide, and accumulated environmental argument","USE equips as low-status cover",false);

        add(m,"Guard fatigues","clothing/guard/uniform",9,"Guard barracks, quartermaster racks, dead patrol caches","standard military fatigues with enough pockets to inventory blame","USE equips as Guard disguise",false);
        add(m,"Guard flak helmet","armor/guard/flak/head",14,"Guard barracks, munition cages, battlefield caches","standard flak helmet issued with hope priced separately","USE equips as flak head protection",false);
        add(m,"Guard webbing harness","clothing/guard/harness",8,"Guard stores, militia racks, drill halls","webbing for magazines, canteens, grenades, and marching misery","USE equips as military harness",false);
        add(m,"Ammo bandolier","clothing/ammo-harness",7,"barracks stores, gang caches, militia lockers","strap rig for carrying ammunition where people can see the threat","USE equips as ammo harness",false);
        add(m,"Officer greatcoat","clothing/guard/officer",18,"officer lockers, command rooms, noble militia stores","heavy coat that makes orders warmer and consequences colder","USE equips as officer disguise",false);
        add(m,"Field boots","clothing/guard/boots",8,"Guard stores, barracks, quartermaster crates","military boots designed for marching away from comfort","USE equips as durable boots",false);
        add(m,"Gas mask","clothing/protective/mask",10,"Guard stores, hazard lockers, riot cages","sealed mask for gas, smoke, and rooms that should be vents","USE equips as respiratory protection",false);
        add(m,"Combat gloves","clothing/guard/gloves",6,"Guard stores, security lockers, militia racks","reinforced gloves for weapons, rubble, and command decisions","USE equips as hand protection",false);
        add(m,"Regimental coat","clothing/guard/regimental",15,"Guard parade stores, veteran lockers, command billets","coat carrying regiment color, history, and social assumptions","USE equips as Guard identity clothing",false);

        add(m,"Arbites suppression armor","armor/arbites/riot",28,"Arbites precinct stores, riot cages, evidence vaults","heavy riot armor built for citizens who insist on questions","USE equips as suppression armor",false);
        add(m,"Riot helmet","armor/arbites/riot/head",16,"Arbites stores, riot wagons, precinct armories","helmet with visor mounts and an institutional forehead","USE equips as riot head protection",false);
        add(m,"Shock gauntlets","armor/arbites/gloves",18,"Arbites gear cages, security vaults, rare evidence stores","gauntlets insulated and reinforced for close persuasion","USE equips as armored gloves",false);
        add(m,"Armored boots","armor/arbites/boots",14,"Arbites stores, security depots, riot cages","boots designed for doors, stairs, and people under both","USE equips as armored boots",false);
        add(m,"Visor mask","clothing/arbites/mask",12,"precinct gear racks, noble security shops, riot stores","face mask with a visor and very little emotional range","USE equips as identity-concealing protection",false);
        add(m,"Evidence pouch harness","clothing/arbites/harness",10,"precinct stores, evidence rooms, patrol lockers","harness of sealed pouches for evidence, bribes, and denial","USE equips as Arbites work harness",false);
        add(m,"Patrol greatcoat","clothing/arbites/coat",14,"Arbites lockers, security offices, cold patrol rooms","dark patrol coat that makes civic order look heavier","USE equips as Arbites disguise",false);

        add(m,"Mechanicus red work robe","clothing/mechanicus/robe",12,"forge cloisters, cable chapels, Mechanicus lockers","red work robe cut for tools, ash, and supervised humanity","USE equips as Mechanicus disguise",false);
        add(m,"Electro-priest wrappings","clothing/mechanicus/wrappings",16,"Mechanicus shrines, electro-choir rooms, evidence cages","insulated wrappings for bodies treated as wiring diagrams","USE equips as Mechanicus protective clothing",false);
        add(m,"Enginseer apron","clothing/mechanicus/apron",14,"forge workrooms, engine shrines, cogitator bays","heavy apron with tool loops, oil history, and doctrinal stains","USE equips as forge workwear",false);
        add(m,"Augmetic harness","clothing/mechanicus/harness",18,"Mechanicus repair bays, surgery rooms, servo stores","anchoring harness for augmentics, cables, and sanctioned posture","USE equips as augmentic support harness",false);
        add(m,"Rubberized insulation robe","clothing/mechanicus/protective",15,"power rooms, cable chapels, high-voltage shrines","robe insulated against current, sparks, and questioning the current","USE equips as electrical protection",false);
        add(m,"Cogitator maintenance gloves","clothing/mechanicus/gloves",8,"cogitator rooms, data alcoves, tech-priest lockers","gloves for touching machine minds and dusty connectors","USE equips as precision work gloves",false);
        add(m,"Cable mantle","clothing/mechanicus/mantle",13,"wire shrines, forge stores, relic ducts","mantle of bundled cable, tags, and social isolation","USE equips as Mechanicus identity clothing",false);
        add(m,"Mechadendrite anchor harness","armor/mechanicus/harness",24,"augmetic bays, forge sanctums, sealed Mechanicus stores","reinforced harness for mounting tools, arms, and bodily decisions","USE equips as heavy Mechanicus harness",false);

        add(m,"Noble tailored coat","clothing/noble/coat",26,"noble wardrobes, salons, household stores","tailored coat that weaponizes fit, fabric, and inherited insulation","USE equips as noble disguise",false);
        add(m,"Formal bodyglove","clothing/noble/bodyglove",24,"noble tailors, dueling salons, high-status shops","close-fitted formal underlayer for ceremony and concealed armor","USE equips as noble formalwear",false);
        add(m,"Noble dueling jacket","clothing/noble/dueling",34,"dueling salons, noble armories, household wardrobes","reinforced jacket for ritualized violence with better etiquette","USE equips as noble dueling clothing",false);
        add(m,"Silk-lined cloak","clothing/noble/cloak",30,"noble wardrobes, governor stores, luxury laundries","cloak lined with soft fabric and hard class separation","USE equips as luxury clothing",false);
        add(m,"Decorated breastplate","armor/noble/ceremonial",42,"noble armories, ceremonial halls, governor stores","ornamental breastplate that still remembers how to stop a blade","USE equips as ceremonial armor",false);
        add(m,"Void silk gloves","clothing/noble/gloves",18,"noble ateliers, void salons, high-status stores","fine gloves for touching nothing directly, especially consequences","USE equips as noble gloves",false);
        add(m,"House livery sash","clothing/noble/livery",16,"household wardrobes, noble service stores, ceremonial racks","sash in household colors for belonging to someone expensive","USE equips as livery/disguise",false);
        add(m,"Ornamental cuirass","armor/noble/ornamental",50,"noble armories, parade rooms, relic wardrobes","ornate rigid torso armor balanced between protection and vanity","USE equips as noble armor",false);

        add(m,"Void crew coverall","clothing/void/coverall",11,"void crew lockers, rail transfer depots, cargo ports","sealed-looking coverall for shipboard labor and corridor accidents","USE equips as void crew disguise",false);
        add(m,"Pressure underlayer","clothing/void/underlayer",13,"void suit stores, hazard repair, orbital cargo lockers","tight underlayer meant to support seals and thermal survival","USE equips as suit underlayer",false);
        add(m,"Sealed work suit","clothing/void/sealed",20,"cargo ports, hazard bays, pressure-lock stores","sealed suit for bad air, worse fluids, and temporary faith in gaskets","USE equips as sealed protective suit",false);
        add(m,"Emergency void hood","clothing/void/hood",15,"evacuation lockers, void stations, cargo emergency boxes","hood for brief exposure emergencies and long-term anxiety","USE equips as emergency pressure hood",false);
        add(m,"Rebreather mask","clothing/protective/rebreather",12,"sump stalls, Guard stores, sanitation lockers","mask and filter assembly for breathable lies","USE equips as respiratory protection",false);
        add(m,"Radiation apron","clothing/protective/radiation",16,"reactor rooms, medicae imaging stores, Mechanicus lockers","weighted apron for radiation work and bleak occupational math","USE equips as radiation protection",false);
        add(m,"Insulated boots","clothing/protective/boots",10,"cold stores, power rooms, void gear racks","boots insulated against cold floors, live panels, and regret","USE equips as protective boots",false);
        add(m,"Filter pack harness","clothing/protective/filter-pack",14,"hazard stores, sanitation depots, sump trader cages","back harness carrying filter media and breathable postponement","USE equips as filter harness",false);
    }

    private static void foodAndDailyLife(LinkedHashMap<String,ItemDef> m){
        add(m,"Civilian meal voucher","food/paperwork",3,"hab kitchens, civic cafeterias, market counters","paper authority for one disappointing meal","trade or consume as low-grade food entitlement",false);
        add(m,"Hab breakfast tray","food",4,"civilian cafeterias and hab kitchens","stacked tray of starch, protein paste, and civic resignation","consume to restore food",false);
        add(m,"Guard field ration tin","food",5,"Imperial Guard mess halls and quartermaster stores","military ration tin designed to survive both war and chewing","consume to restore food",false);
        add(m,"Noble preserved delicacy","food/luxury",28,"noble pantries, salons, and governor stores","sealed luxury food with more protection than most citizens","valuable food; consume or trade",false);
        add(m,"Mechanicus nutrient ampoule","food/chemical",8,"Mechanicus nutrient galleys and diagnostic bays","gray nutrient dose calibrated for obedience more than pleasure","consume to restore food with faction-effect profile",false);
        add(m,"Sump fungus loaf","food",3,"sump markets, sewer camps, fungus niches","spongy underhive bread that may still be thinking about the sewer","consume to restore food",false);
        add(m,"Cult offering wafer","food/contraband",6,"cultist sewer shrines and hidden ritual kitchens","thin ration wafer stamped with symbols nobody should recognize","consume or trade; corruption/social-effect profile",false);
        add(m,"Child creche snack pack","food",2,"creches, daycares, public learning rooms","small ration pack meant for children and desperate adults","consume to restore a little food",false);
        add(m,"Laundry token bundle","civic/trade",4,"hab laundries, noble service rooms, worker stores","small tokens for communal laundry or tiny bribes","trade/civic utility item",false);
        add(m,"Sanitation permit chit","paperwork/sanitation",5,"administratum counters, hab maintenance rooms","permission to use infrastructure that should not require permission","civic access/social-mitigation profile",false);
        add(m,"Water guild token","water/trade",7,"sump water stalls, rail depots, hab counters","token redeemable for cleaner water if the counter still honors it","trade or water-access profile",false);
        add(m,"Sealed cafeteria cutlery","tool/junk",2,"cafeterias, mess halls, noble dining galleries","cheap utensil pack wrapped like a holy relic","minor tool, trade scrap, improvised utility",false);
        add(m,"Hydroponic protein grain","food/raw",6,"hydroponic farms, hab food decks, rail provisioning farms","thick nutrient grain grown under sour artificial lighting","raw food input; consume or cooking/processing input",false);
        add(m,"Marsh-rice sack","food/raw",5,"civilian hydroponics, sump farms, ration board greenhouses","bag of wet-grown ration grain with stubborn shelf life","raw food input and low-grade ration ingredient",false);
        add(m,"Vorder leaf bundle","food/stimulant",7,"caf greenhouses and civic hydroponic racks","brewable leaves for recaf-style hot drinks","consume/use as mild stimulant or trade beverage input",false);
        add(m,"Ploin juice flask","food/drink",8,"void-crew provisioners, rail depots, hydroponic fruit racks","vitamin-rich fruit drink with a shelf life built for bad travel","consume to restore food/water; illness-mitigation profile",false);
        add(m,"Low-quality amasec bottle","drink/contraband",9,"lower-class still rooms, sump markets, ganger kitchens","rough distilled scrap mash with chemical courage and a cleaning-fluid finish","trade, consume for nerve at risk profile",false);
        add(m,"High-quality amasec bottle","drink/luxury",42,"noble bio-gardens, orchard stillhouses, governor stores","barrel-aged fruit or grain spirit smooth enough to insult poverty by existing","luxury trade good; reserved social/bribe use",false);
        add(m,"Noble orchard fruit crate","food/luxury/raw",34,"noble synthetic orchards and artificial-sun gardens","real fruit grown under private artificial daylight","consume or trade as luxury food",false);
        add(m,"Bio-garden truffle tin","food/luxury",46,"noble bio-gardens and sealed pantry galleries","cultivated truffles from carefully managed synthetic soil","high-value food and social gift",false);
        add(m,"Caba nut packet","food/raw",8,"hydroponic farms, market food stalls, rail provisioning","shelf-stable nuts used in worker snacks and ration blending","consume or use as cooking input",false);
        add(m,"Wall-rat meat strip","food/meat",4,"interstitial hunting, sewer camps, mutant warrens","cured strip from wall-rat fauna native to forgotten hive spaces","consume as risky protein",false);
        add(m,"Soylens viridian algae cake","food/synthetic",5,"algae vats, reclamation tanks, low-grade ration works","green synthetic nutrient block made from algae and whatever passed audit","consume to restore food",false);
        add(m,"Corpse-starch ration slab","food/synthetic",3,"reclamation works, lower-hive ration boards, emergency stores","recycled nutrient slab whose ingredients are better left bureaucratic","consume to restore food",false);
        add(m,"Triglyceride gel tube","food/synthetic/military",12,"military nutrient vats and high-calorie combat stores","dense calorie gel made for brutal exertion and joyless survival","consume to restore food strongly",false);
        add(m,"Amino-porridge ration bowl","food/synthetic/military",10,"military-grade nutrient kitchens and barracks messes","synthetic amino porridge issued for reliable body-fuel without ceremony","consume to restore food",false);
        add(m,"Recaf tin","food/drink/stimulant",7,"cafeterias, guard messes, archives, market stalls","crushed brew leaves for a hot stimulant drink that keeps workers upright","consume/use as mild wakefulness item",false);
        add(m,"Void crew wobble bottle","drink/contraband",13,"rail depots, void provision traders, rough stills","strong ploin-based drink named after its fruit and its victims","trade or consume for nerve at risk profile",false);
        add(m,"Fresh INN newspaper","media/news/paper",2,"Imperial News Network dispensers, newsstands, public counters","today's sanctioned paper: fresh ink, official framing, and enough real information to be dangerous","USE reads current public faction news",false);
        add(m,"Yesterday's INN newspaper","media/news/paper/old",1,"trash bins, benches, discarded bundles, hab corridors","yesterday's sanctioned paper: stale but still useful if you are poor, patient, or investigative","USE reads yesterday's public faction news",false);
        add(m,"Old INN newspaper","media/news/paper/old",1,"trash piles, service corridors, archive floors, hab bins","old newsprint stained by boots, water, and the slow victory of entropy","USE reads an older public news issue if legible",false);
        add(m,"Useless paper mush","junk/paper/decayed",0,"wet trash piles, drains, bins, corridor corners","newsprint that lost a theological argument with moisture","junk; too decayed to read",false);
        add(m,"Used food tin","junk/food/tin",0,"trash piles, bins, hab floors, mess halls","empty food tin with enough residue to prove somebody was hungry here","common trash; recycling input",false);
        add(m,"Ration wrapper","junk/food/wrapper",0,"trash piles, benches, barracks, corridors","creased ration wrapper marked by grease, fingerprints, and the absence of food","common trash; recycling input",false);
        add(m,"Radio set","media/news/radio",9,"INN offices, bars, rail depots, hab common rooms","small sanctioned receiver tuned to bulletins, hymns, warnings, and deliberate gaps","bar/broadcast news receiver",false);
        add(m,"Public pict-screen tube","media/news/pict",12,"broadcast centers, bars, noble lounges, public plazas","display tube for approved pict bulletins and civic reassurance","pict-screen news receiver",false);
        add(m,"Stolen faction leader journal","intel/stolen/journal",5000,"faction leader bedrooms, offices, locked desks","private leadership plans valuable enough to get people murdered twice","sell to INN for bounty; readable intelligence",false);
        add(m,"Bank manager keycard","bank/heist/keycard",180,"bank managers, branch offices, vault desks","keycard granting bank-vault access profile if the alarms and conscience cooperate","reserved bank heist access tool",false);
        add(m,"Sealed bank lockbox","bank/heist/lockbox",900,"bank vaults and noble financial quests","sealed client property whose contents are valuable, illegal to possess, or both","heist/quest-objective profile",false);
        add(m,"Stock certificate bundle","bank/heist/certificate",650,"bank vaults, noble offices, investment ledgers","paper claims on wealth printed for people who dislike carrying it personally","reserved noble/bank quest objective",false);
        add(m,"House gold ingot","bank/heist/gold",1200,"upper-hive vaults and noble reserve rooms","heavy precious-metal reserve stamped by a banking house","reserved bank heist loot",false);
    }


    private static void waterFoodWasteAgricultureChainItems(LinkedHashMap<String,ItemDef> m){
        // 0.8.94 WATER / FOOD / WASTE / AGRICULTURE CHAINS:
        // These are production-chain vocabulary entries, not an immediate player-facing recipe flood.
        // They let the draft decomposition graph distinguish raw waste streams, reclaimed water,
        // hydroponic growth media, vat nutrition, orchard goods, military foods, and underhive survival foods.
        add(m,"Atmospheric condensate","water/raw/condensate",1,"condenser vanes, cold pipes, atmospheric collectors","thin collected moisture before filtration and bureaucratic optimism","raw condenser water input",false);
        add(m,"Greywater","water/raw/grey",1,"laundries, wash rooms, cafeterias, hab drains","used wash water that is less horrifying than sump sludge but not by much","raw reclamation input",false);
        add(m,"Strained sump water","water/processed/strained",1,"sump screens, sewer galleries, mutant camps","sump water with chunks removed, which counts as progress locally","intermediate reclamation input",false);
        add(m,"Reclamation brine","water/processed/brine",2,"filter beds, utility plants, desalination racks","salty concentrated reject stream from reclamation membranes","industrial byproduct and mineral input",false);
        add(m,"Toxin slurry","chemical/waste/slurry",3,"sump separators, chem drains, mutant warrens","concentrated toxic residue after water is bullied out of sludge","chemical waste and poison precursor",false);
        add(m,"Reclaimed mineral cake","material/agriculture/mineral",2,"reclamation beds, sump filters, hydroponic stores","pressed mineral residue useful when plants need food and standards have left","agricultural mineral input",false);
        add(m,"Clean water","water/processed/clean",4,"boilers, utility stills, medicae kettles","cleaner water suitable for consumption and simple procedures","consume/use as clean water input",false);
        add(m,"Sterile water flask","water/industrial/sterile",7,"medicae clean benches, labs, sealed stills","distilled water sealed hard enough to deserve a label","medical and precision manufacturing input",false);
        add(m,"Compost substrate","agriculture/input/compost",2,"waste yards, fungus farms, hab gardens","rotted managed substrate for growing food out of civic embarrassment","agriculture input",false);
        add(m,"Sterilized grow medium","agriculture/input/medium",5,"hydroponic racks, noble gardens, Mechanicus clean trays","treated growth medium with most organisms evicted by force","controlled agriculture input",false);
        add(m,"Hydroponic nutrient solution","agriculture/input/solution",5,"hydroponic pumps, ration farms, agri racks","water, salts, and nutrient mix for root systems under artificial mercy","hydroponic fluid input",false);
        add(m,"Hydroponic crop stock","food/raw/hydroponic",6,"hydroponic racks, civilian farms, Guard provisioning decks","fresh crop mass before milling, drying, or being turned into ration paste","raw food crop input",false);
        add(m,"Algae culture vat","food/raw/algae",5,"algae vats, water recyclers, nutrient works","green production culture that converts light, water, and disgust into calories","synthetic food input",false);
        add(m,"Fungus culture tray","food/raw/fungus",4,"sump farms, sewer niches, mutant food racks","cultivated fungus tray grown where sunshine was not invited","underhive food input",false);
        add(m,"Marsh-rice paddy tray","food/raw/grain",5,"wet hydroponic trays, sump farms, civic agri racks","managed tray of marsh-rice before sacks and ration mills","grain input",false);
        add(m,"Vorder leaf clipping tray","food/raw/stimulant",6,"caf greenhouses, hydroponic rooms, archive kettles","cuttings for stimulant leaves used in recaf and wakefulness workarounds","stimulant crop input",false);
        add(m,"Caba nut culture pot","food/raw/nut",7,"hydroponic rack farms, market agri stalls","nut culture pot that turns clean inputs into hard little calories","raw food crop input",false);
        add(m,"Ploin fruit culture tray","food/raw/fruit",8,"void provision gardens, fruit racks, hydroponic farms","starter tray for the lopsided fruit sailors keep insisting is medicine","fruit crop input",false);
        add(m,"Noble orchard graft stock","agriculture/luxury/orchard",18,"noble bio-gardens, sealed orchard nurseries","licensed graft stock for real fruit trees under private artificial suns","luxury agriculture input",false);
        add(m,"Bio-garden soil bed","agriculture/luxury/soil",16,"noble gardens, governor service spines, sealed green rooms","engineered soil bed so rich it would be illegal in the sump if anyone cared","luxury garden input",false);
        add(m,"Artificial-sun orchard tray","agriculture/luxury/tray",24,"noble synthetic orchards, estate bio-gardens","orchard tray with lighting and nutrient loops tuned for embarrassing abundance","luxury food production fixture input",false);
        add(m,"Nutrient vat base","food/raw/vat",4,"nutrient vat rooms, reclamation kitchens, military food works","base vat load before additives, binding, or moral laundering","synthetic food input",false);
        add(m,"Protein slurry","food/raw/protein",4,"vat tanks, algae works, military kitchens","protein-heavy slurry useful for ration bricks and other crimes against texture","food-processing input",false);
        add(m,"Amino culture broth","food/raw/amino",5,"medicae vats, military kitchens, Mechanicus galleys","amino-rich broth grown or blended for reliable bodily obedience","military and Mechanicus food input",false);
        add(m,"Lipid skim","food/raw/lipid",5,"nutrient vats, grease separators, military kitchens","fat-rich skim for calorie-dense gel and harsh survival food","high-calorie food input",false);
        add(m,"Triglyceride stock","food/raw/lipid/stock",7,"military nutrient vats, ration chemistry benches","stabilized lipid stock before being packed into combat gel","military food precursor",false);
        add(m,"Soylens algae paste","food/intermediate/synthetic",4,"algae vats, ration plants, reclamation kitchens","green paste on the long road to being called a cake","synthetic food intermediate",false);
        add(m,"Corpse-starch paste","food/intermediate/reclamation",3,"reclamation works, corpse recovery, lower-hive ration boards","starch paste from reclaimed biomass and official silence","reclamation food intermediate",false);
        add(m,"Ration paste","food/intermediate/ration",4,"ration plants, cafeterias, field kitchens","processed paste that can become tins, trays, bricks, or punishments","ration intermediate",false);
        add(m,"Emergency ration paste","food/intermediate/ration/emergency",5,"survival packers, vending plants, Guard commissaries","dense ration paste stabilized for being forgotten until needed","emergency ration intermediate",false);
        add(m,"Grain mash","food/intermediate/grain",4,"ration mills, kitchens, agri processors","milled grain mash for porridge, bread, ration paste, and cheap alcohol","food-processing input",false);
        add(m,"Fermentable scrap mash","food/intermediate/fermentation",3,"sump stills, lower kitchens, gang mess rooms","whatever can ferment after the best parts were already eaten","low-grade brewing input",false);
        add(m,"Distilled spirit base","drink/intermediate/spirit",8,"still rooms, orchard houses, sump boilers","clear spirit base before class identity is applied by barrel or desperation","amasec intermediate",false);
        add(m,"Low-grade amasec wash","drink/intermediate/amasec/low",5,"sump stills, ganger kitchens, lower hab bars","rough alcoholic wash distilled from scraps with hostile intent","low-grade amasec input",false);
        add(m,"Aged amasec cask","drink/intermediate/amasec/high",30,"noble cellar vaults, orchard stillhouses","aged spirit cask with enough patience to become expensive","high-quality amasec input",false);
        add(m,"Ploin vitamin concentrate","food/intermediate/juice",7,"void provision rooms, fruit processors, medicae-adjacent kitchens","concentrated ploin pulp and vitamins before bottling","drink and illness-mitigation input",false);
        add(m,"Recaf leaf roast","food/intermediate/stimulant",6,"cafeterias, Guard messes, archive kettles","roasted stimulant leaves ready for tins and bitter mornings","recaf input",false);
        add(m,"Wall-rat protein stock","food/raw/meat",3,"sewer hunters, mutant camps, scavenger kitchens","processed wall-rat protein before curing and social denial","meat input",false);
        add(m,"Preserved meat strip batch","food/intermediate/meat",5,"sump smoke racks, mutant kitchens, field messes","salted and preserved meat strips batch for travel or threat display","meat ration intermediate",false);
    }

    private static void chemNarcoticIntoxicantChainItems(LinkedHashMap<String,ItemDef> m){
        // 0.8.95a CHEM / NARCOTIC / INTOXICANT PRODUCTION CHAIN PASS:
        // Catalog entries for underhive drugs, labor-control chems, medicae compounds, noble vice goods,
        // cult-corruption substances, mutant sump tonics, void/frontier intoxicants, and their reusable precursors.
        // These remain draft/audit-facing production entries until Infopedia/ledger presentation is hardened.
        add(m,"Chemical reagent bottle","component/chem/reagent",4,"chem lockers, clinics, sump processors","standard bottle of reactive solvent and stabilizers","chemical precursor",false);
        add(m,"Medicae stabilizer compound","component/chem/stabilizer",8,"clinics, aid stations, illicit chop-shops","stabilizing compound used to stop drugs from becoming immediate funerals","medical precursor",false);
        add(m,"Alkaloid extract","component/chem/alkaloid",5,"recaf roasters, leaf dryers, illegal stills","bitter plant-derived stimulant fraction","stimulant precursor",false);
        add(m,"Stimulant salt batch","component/chem/stimulant",6,"labor chem rooms, gang kitchens, medicae stores","crystalline stimulant salts for wakefulness, aggression, and poor scheduling","stimulant precursor",false);
        add(m,"Sedative tincture base","component/chem/sedative",6,"clinics, pleasure houses, hospice shrines","sleep and compliance base dissolved into alcohol or syrup","sedative precursor",false);
        add(m,"Analgesic compound","component/chem/analgesic",8,"medicae stores, labor dispensaries, illicit clinics","pain-dulling compound used medically or cruelly","medical precursor",false);
        add(m,"Combat stimm compound","component/chem/combat-stimm",10,"battlefield aid stations, gang labs, penal stores","adrenal stimulant base for drugs that make consequences negotiable","combat drug precursor",false);
        add(m,"Aggression catalyst","component/chem/aggression",7,"gang kitchens, penal stores, cult caches","compound that turns fear, pain, or shame into forward motion","combat/riot precursor",false);
        add(m,"Focus nootropic base","component/chem/nootropic",9,"data-serf pharmacies, investigator desks, cogitator rooms","pattern-recognition and attention compound","focus drug precursor",false);
        add(m,"Hallucinogen resin","component/chem/hallucinogen",10,"fungus grow-pits, noble salons, cult dens","psychoactive resin that convinces the brain it has been promoted","hallucinogen precursor",false);
        add(m,"Euphoric syrup base","component/chem/euphoric",8,"pleasure dens, noble stillhouses, vice kitchens","sweetened mood-lifter base with expensive mistakes dissolved in it","euphoric precursor",false);
        add(m,"Fungal spore culture","component/chem/fungal",5,"sump gardens, sewer grow rooms, mold shrines","cultured fungal stock for spores, teas, and regrettable laughter","fungal precursor",false);
        add(m,"Psychoactive mold cake","component/chem/mold",7,"corpse stores, damp archives, shrine refuse","mold culture grown where sanitation lost a theological argument","hallucinogen precursor",false);
        add(m,"Sump fermentation mash","component/chem/sump-mash",4,"sump stills, mutant cisterns, trash kitchens","fermenting underhive mash made from scrap organics and bad water","fermentation precursor",false);
        add(m,"Voidship rotgut wash","component/chem/void-wash",5,"underdeck stills, coolant ducts, void crew lockers","fermented recyclables before they become tradition or blindness","void intoxicant precursor",false);
        add(m,"Mineral tonic slurry","component/chem/mineral-tonic",5,"ash-waste camps, sump pipes, corrosion stills","mineral-heavy slurry for frontier tonics and unsafe sweets","mineral intoxicant precursor",false);
        add(m,"Radiant mineral dust","component/chem/radiant-dust",9,"contaminated zones, rad-mutant dens, ash spoil heaps","glowing mineral dust no sensible person would taste twice","rad chem precursor",false);
        add(m,"Lho leaf bale","component/chem/lho-leaf",4,"hydroponic leaf racks, pilgrim markets, worker kiosks","dried smokeable leaf or fungal substitute for lho products","smoke precursor",false);
        add(m,"Smokeable lho roll","component/chem/lho-roll",2,"hab kiosks, trench packs, manufactorum ration desks","rolled smoke substrate awaiting additives and packaging","smoke precursor",false);
        add(m,"Incense resin pellet","component/chem/incense",5,"shrines, pilgrim stalls, cult rooms","burnable resin pellet for devotional and manipulative smoke","incense precursor",false);
        add(m,"Perfume carrier oil","component/chem/perfume-oil",8,"noble salons, chem perfumers, shrine markets","aromatic carrier oil for elite narcotic sprays and oils","luxury precursor",false);
        add(m,"Spire crystal vial","component/chem/noble-vial",12,"spire glassworks, noble pharmacies, velvet dens","decorative clean vial for drugs that can afford manners","luxury packaging",false);
        add(m,"Underhive paper twist","component/chem/paper-twist",1,"corner dealers, worker bars, gang benches","cheap paper fold for powder, resin, and bad choices","cheap packaging",false);
        add(m,"Injector ampoule set","component/chem/injector-ampoule",8,"medicae lockers, gang medics, clinic stores","sterile or allegedly sterile ampoules and injector fittings","injection packaging",false);
        add(m,"Snuff capsule tin","component/chem/snuff-tin",4,"club stalls, youth dens, messenger markets","small tin or capsule case for powders and snuffs","powder packaging",false);
        add(m,"Lozenge binder base","component/chem/lozenge",3,"medicae lozenge presses, shuttle lockers, vice kitchens","binder for tablets, lozenges, and chewable chems","tablet precursor",false);
        add(m,"Confection binder base","component/chem/confection",4,"pleasure dens, cheap candy stalls, noble dessert rooms","sweet binder for narcotic candies and dessert chems","confection precursor",false);
        add(m,"Aerosol propellant bulb","component/chem/aerosol",6,"pressure shops, maintenance lockers, club sprayers","pressurized bulb for inhalants and misted doses","inhalant precursor",false);
        add(m,"Warp-tainted mirror shard","component/chem/warp-shard",40,"heretek caches, forbidden shrines, black glass markets","reflective shard tagged as corruption-bearing contraband","forbidden precursor",false);
        add(m,"Psychic catalyst powder","component/chem/psychic-catalyst",35,"wyrd markets, handler caches, cult stores","dangerous powder used to provoke perception or psychic leakage","forbidden psychic precursor",false);
        add(m,"Forbidden focus wafer","component/chem/focus-wafer",30,"black-market scholars, interrogator caches, cult archives","transparent wafer used in mental clarity, memory, and perception chems","forbidden nootropic precursor",false);
        add(m,"Profane binding ash","component/chem/profane-ash",16,"hidden chapels, burnt shrines, cult braziers","ritual ash used to make crowds feel chosen by the wrong thing","cult precursor",false);
        add(m,"Bone ash reagent","component/chem/bone-ash",8,"corpse reclamation, mutant camps, cult altars","alkaline bone ash used in awful cheap chemistry","reagent precursor",false);
        add(m,"Clotting foam reagent","component/chem/clotting",11,"battlefield kits, mine clinics, gang medics","foaming clotting reagent for trauma drugs","medical precursor",false);
        add(m,"Nerve dampener solution","component/chem/nerve-dampener",14,"surgical theaters, augmetic dens, illicit clinics","compound that makes nerves stop arguing with knives","medical precursor",false);
        add(m,"Revival shock compound","component/chem/revival",12,"aid stations, gang interrogation kits, medicae drawers","ugly stimulant for dragging the body back into service","medical precursor",false);
        add(m,"Interrogation dosing kit","component/chem/interrogation",12,"black sites, Arbites stores, bounty cages","restricted dosing hardware for coercive pharmaceuticals","security precursor",false);
        add(m,"Compliance sedative base","component/chem/compliance",10,"transport cages, confession cells, noble hospice cabinets","sedative base tuned for passivity and memory gaps","security/medical precursor",false);
        add(m,"Labor dosing ticket strip","component/chem/labor-ticket",2,"manufactorum dispensaries, overseer desks, ration lines","tear-off administration strip for legal exploitation in dose form","labor packaging",false);
        add(m,"Devotional wrapper strip","component/chem/devotional-wrapper",3,"pilgrim stalls, shrine kitchens, confession cells","printed wrapper for holy-looking substances with administrative ambiguity","devotional packaging",false);
        add(m,"Anti-nausea lozenge base","component/chem/anti-nausea",5,"shuttle crews, medicae drawers, void lockers","lozenge base for grav-sickness and recreationally poor judgment","void/medical precursor",false);
        add(m,"Vitamin concentrate","component/chem/vitamin",6,"void provisioners, ploin presses, clinic stores","vitamin concentrate for underfed crews and plausible beverages","provisioning precursor",false);
        add(m,"Lho-Sticks","chem/everyday-smoke",3,"hab kiosks, trench lines, worker bars","everyday smoke product with habit, trade, and underhive wage implications","everyday smoke/intoxicant; trade and habit good",false);
        add(m,"Black Lho","chem/everyday-smoke",3,"hab kiosks, trench lines, worker bars","everyday smoke product with habit, trade, and underhive wage implications","everyday smoke/intoxicant; trade and habit good",false);
        add(m,"Ash Lho","chem/everyday-smoke",3,"hab kiosks, trench lines, worker bars","everyday smoke product with habit, trade, and underhive wage implications","everyday smoke/intoxicant; trade and habit good",false);
        add(m,"Saint’s Lho","chem/everyday-smoke",3,"hab kiosks, trench lines, worker bars","everyday smoke product with habit, trade, and underhive wage implications","everyday smoke/intoxicant; trade and habit good",false);
        add(m,"Recaf","chem/everyday-stimulant",4,"vending machines, work canteens, overseer desks","everyday stimulant product used to survive work, paperwork, and sleeplessness","everyday stimulant; fatigue-control profile",false);
        add(m,"Burn Recaf","chem/everyday-stimulant",4,"vending machines, work canteens, overseer desks","everyday stimulant product used to survive work, paperwork, and sleeplessness","everyday stimulant; fatigue-control profile",false);
        add(m,"Grin Powder","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Dreamsap","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Grey Drops","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Glow-Snuff","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Sumpweed","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Green Haze","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Mirth-Spores","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Night Milk","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Static Chew","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Copper Kiss","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Lantern Dust","chem/everyday",6,"hab markets, worker bars, petty traders","everyday underhive intoxicant with habit, trade, and morale implications","everyday underhive intoxicant; reserved habit/morale effects",false);
        add(m,"Obscura","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Pearl Obscura","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Mourning Obscura","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Amasec","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"High Amasec","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Low Amasec","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Gildwine","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Vermilion Cordial","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Sable Nectar","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Angel’s Vitreous","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Spire Lotus","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Memory Lacquer","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Velvet Sleep","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Saint’s Tears","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Red Chapel Drops","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Glass Vein","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Halo Mist","chem/noble-luxury",34,"noble salons, spire dens, black-market vintners","high-society intoxicant with luxury packaging and social leverage value","luxury narcotic/intoxicant; social/dependency-effect profile",false);
        add(m,"Stimm","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Street Stimm","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Redline","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Slaught","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Frenzon","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Knuckle Joy","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Iron Grin","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Bolt Rush","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Spinefire","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Hate Salt","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Chainbite","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Ripper Tabs","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Mud Courage","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Smokeghost","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Wirewake","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Bad Saint","chem/ganger-combat",14,"gang chem kitchens, fighting pits, mercenary lockers","street chem used by gangers, pit fighters, mercenaries, or knife crews","combat drug; aggression/pain/crash-effect profile",false);
        add(m,"Flects","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Spook","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Witchsalt","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Choir Ash","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Black Benediction","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Saintsbane Incense","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Gutter Prophet","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Vox-Dust","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Red Revelation","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Pale Communion","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Mirror Milk","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Carrion Scripture","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Eclipse Resin","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Thirteenth Perfume","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Woundlight","chem/cult-warp",28,"hidden chapels, cult recruiters, heretek caches","forbidden cult or warp-tainted substance with corruption and contraband implications","forbidden ritual chem; contraband and corruption-tagged",false);
        add(m,"Sumpkalm","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Brine Joy","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Glandwake","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Bone Softener","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Thickblood","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Rad-Sweet","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Glowgut Mash","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Scale Oil","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Mire-Dream","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Rustmilk","chem/mutant-sump",9,"mutant warrens, sump stills, sewer gardens","sump or mutant-adapted chemical tolerated by desperate lower-hive bodies","mutant/sump chem; tolerance/contamination-effect profile",false);
        add(m,"Shiftwake","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Line-Keeper","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Grey Mercy","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Soot Calm","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Quota Joy","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Hammerwake","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Stillhand","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Dustlung Draught","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Cogitator Blue","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Clerk’s Mercy","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Boiler Black","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Red Ticket","chem/labor-control",8,"manufactorum dispensaries, overseer lockers, labor stores","manufactorum drug used to manage fatigue, pain, focus, or obedience","labor-control chem; fatigue/obedience-effect profile",false);
        add(m,"Medi-Stimm","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"White Mercy","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Nerve Lace","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Red Waker","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Stitchdream","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Clotfoam Ampoule","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Pale Nurse","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Saint’s Anesthetic","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Wakewire Solution","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Mercy Spoil","medical/chem",18,"clinics, medicae lockers, illicit chop-shops","medicae or illicit clinic compound with treatment and abuse potential","medical or illicit clinic drug; treatment/abuse-effect profile",false);
        add(m,"Warpglimmer","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"Third-Eye Tea","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"Static Communion","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"Dream-Index","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"Thoughtglass","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"False Astronomican","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"Echo Seed","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"Mind-Soot","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"Blue Silence","chem/psyker-forbidden",42,"black-market diviners, witch-cults, handler caches","forbidden perception drug with psychic, investigative, or handler relevance","forbidden perception drug; psychic-risk tagged",false);
        add(m,"Luckdust","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Soft Gold","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Jester’s Mercy","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Pink Static","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Velvet Teeth","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Diceblood","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Sweet Ruin","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Chorus Wine","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Hushpetal","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Silver Giggle","chem/vice-market",16,"pleasure dens, gambling houses, slum clubs","pleasure-den, gambling-house, or vice-market intoxicant with social-risk value","vice-market intoxicant; morale/social-risk-effect profile",false);
        add(m,"Voidwake","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Blackwater Rum","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Ploin Juice","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Ploin Spike","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Ashbite","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Dune Milk","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Hullshine","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Starless Sleep","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Grav-Sick Green","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Red Hull Paste","chem/void-frontier",11,"voidship underdecks, ash-waste camps, shuttle crews","voidship, ash-waste, or frontier chemical for fatigue, sickness, or endurance","frontier or voidship intoxicant; fatigue/survival-effect profile",false);
        add(m,"Pilgrim’s Ease","chem/devotional",15,"pilgrim markets, fringe shrines, confession cells","pilgrim-market or Ecclesiarchy-adjacent substance used for pain, devotion, or crowd control","devotional or crowd-control substance; faith/social-effect profile",false);
        add(m,"Censer-Dream","chem/devotional",15,"pilgrim markets, fringe shrines, confession cells","pilgrim-market or Ecclesiarchy-adjacent substance used for pain, devotion, or crowd control","devotional or crowd-control substance; faith/social-effect profile",false);
        add(m,"Martyr’s Wine","chem/devotional",15,"pilgrim markets, fringe shrines, confession cells","pilgrim-market or Ecclesiarchy-adjacent substance used for pain, devotion, or crowd control","devotional or crowd-control substance; faith/social-effect profile",false);
        add(m,"Ashen Host","chem/devotional",15,"pilgrim markets, fringe shrines, confession cells","pilgrim-market or Ecclesiarchy-adjacent substance used for pain, devotion, or crowd control","devotional or crowd-control substance; faith/social-effect profile",false);
        add(m,"Reliquary Drops","chem/devotional",15,"pilgrim markets, fringe shrines, confession cells","pilgrim-market or Ecclesiarchy-adjacent substance used for pain, devotion, or crowd control","devotional or crowd-control substance; faith/social-effect profile",false);
        add(m,"Flagellant’s Fire","chem/devotional",15,"pilgrim markets, fringe shrines, confession cells","pilgrim-market or Ecclesiarchy-adjacent substance used for pain, devotion, or crowd control","devotional or crowd-control substance; faith/social-effect profile",false);
        add(m,"Golden Quiet","chem/devotional",15,"pilgrim markets, fringe shrines, confession cells","pilgrim-market or Ecclesiarchy-adjacent substance used for pain, devotion, or crowd control","devotional or crowd-control substance; faith/social-effect profile",false);
        add(m,"Choir Honey","chem/devotional",15,"pilgrim markets, fringe shrines, confession cells","pilgrim-market or Ecclesiarchy-adjacent substance used for pain, devotion, or crowd control","devotional or crowd-control substance; faith/social-effect profile",false);
        add(m,"Truth Ache","chem/security-interrogation",24,"Arbites evidence lockers, black sites, bounty cages","restricted security or interrogation chemical with strong legal consequences","security/interrogation chem; restricted/contraband",false);
        add(m,"Pale Compliance","chem/security-interrogation",24,"Arbites evidence lockers, black sites, bounty cages","restricted security or interrogation chemical with strong legal consequences","security/interrogation chem; restricted/contraband",false);
        add(m,"Blue Lantern","chem/security-interrogation",24,"Arbites evidence lockers, black sites, bounty cages","restricted security or interrogation chemical with strong legal consequences","security/interrogation chem; restricted/contraband",false);
        add(m,"Fearhook","chem/security-interrogation",24,"Arbites evidence lockers, black sites, bounty cages","restricted security or interrogation chemical with strong legal consequences","security/interrogation chem; restricted/contraband",false);
        add(m,"Grav-Lock","chem/security-interrogation",24,"Arbites evidence lockers, black sites, bounty cages","restricted security or interrogation chemical with strong legal consequences","security/interrogation chem; restricted/contraband",false);
        add(m,"Confessor’s Needle","chem/security-interrogation",24,"Arbites evidence lockers, black sites, bounty cages","restricted security or interrogation chemical with strong legal consequences","security/interrogation chem; restricted/contraband",false);
        add(m,"Cold Candle","chem/security-interrogation",24,"Arbites evidence lockers, black sites, bounty cages","restricted security or interrogation chemical with strong legal consequences","security/interrogation chem; restricted/contraband",false);
        add(m,"Black Badge","chem/security-interrogation",24,"Arbites evidence lockers, black sites, bounty cages","restricted security or interrogation chemical with strong legal consequences","security/interrogation chem; restricted/contraband",false);
        add(m,"Pipe Bloom","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Drain Sugar","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Battery Kiss","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Corpse Gin","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Yellow Mercy","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Sewer Bells","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Rat King","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Mold Saint","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Blue Scab","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Rust Angel","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Glass Belly","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Cradle Rot","chem/lower-hive-trash",3,"pipe residue sellers, lower-hive stalls, desperate dens","cheap hazardous lower-hive intoxicant made from waste, residue, or desperation","hazardous cheap intoxicant; severe contamination risk",false);
        add(m,"Emperor’s Mercy","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Saint Drusus’ Breath","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Lucid Null","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Ghost Orchid","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Ebon Lotus","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Angel Engine","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Machine Rapture","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Black Sun Draught","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Sorrowglass","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Kingmaker Serum","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Widow’s Crown","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"Red Choir","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
        add(m,"The Beautiful Error","chem/rare-campaign",85,"campaign relic caches, noble vaults, inquisitorial rumors","rare campaign-significant chemical stock; valuable, dangerous, and politically explosive","rare campaign-significant chem; high risk/high value",false);
    }

    private static void laboratoryChemicalEquipmentItems(LinkedHashMap<String,ItemDef> m){
        // 0.8.95b LABORATORY / CHEMICAL EQUIPMENT REQUIREMENTS:
        // These are registered as durable catalog equipment so chemical recipes can require
        // installed apparatus without pretending the apparatus is consumed like an ingredient.
        add(m,"Crude chem bench","equipment/lab/crude",32,"gang chem kitchens, sump still rooms, illicit hab workshops","scarred reagent table for unsafe but common underhive chemistry","installed lab apparatus; crude compounding, paper twists, ganger batches",false);
        add(m,"Reagent preparation bench","equipment/lab/reagent",44,"clinics, manufactorum labs, chem guild rooms","general bench for compounding abstract fictional reagents","installed lab apparatus; generic chemical preparation",false);
        add(m,"Chemical reagent rack","equipment/lab/storage",28,"chem stores, medicae cabinets, manufactorum supply cages","rack for labeled bottles and suspiciously relabeled bottles","installed lab storage; reagent handling",false);
        add(m,"Filtration rack","equipment/lab/filtration",38,"water refineries, medicae rooms, sump labs","filter frame for water, slurry, tinctures, and bad ideas","installed lab apparatus; filtration and clarification",false);
        add(m,"Distillation column","equipment/lab/distillation",62,"breweries, noble cellars, medicae still rooms","column for fictional spirit and solvent refinement","installed lab apparatus; distillation",false);
        add(m,"Crude still","equipment/lab/distillation/crude",26,"sump bars, voidship underdecks, gang dens","ugly still that technically separates liquids and consequences","installed lab apparatus; low-quality distillation",false);
        add(m,"Clean fermentation vat","equipment/lab/fermentation",48,"licensed breweries, noble estates, food guild rooms","sealed vat for controlled fermentation and cultured stocks","installed lab apparatus; fermentation",false);
        add(m,"Sump fermentation tub","equipment/lab/fermentation/crude",22,"sump kitchens, mutant warrens, sewer gardens","stained tub for brewing things that should have remained dead","installed lab apparatus; dirty fermentation",false);
        add(m,"Fungal grow tray bank","equipment/lab/fungal",35,"sewer gardens, mutant enclaves, underhive vat rooms","stacked trays for controlled fungal and mold culture","installed lab apparatus; fungal cultivation",false);
        add(m,"Narcotic drying rack","equipment/lab/drying",24,"gang dens, lho rooms, illegal clinics","screen rack for drying leaf, fungus, powders, and questionable hope","installed lab apparatus; drying and smoke preparation",false);
        add(m,"Pellet press","equipment/lab/press",36,"noble vice rooms, chem kitchens, manufactorum dosing stations","small press for tablets, pearls, tabs, and lozenges","installed lab apparatus; pressing and dosing",false);
        add(m,"Injector filling station","equipment/lab/injector",58,"clinics, military aid stations, gang chop-shops","ampoule rack and dosing jig for injectable fictional compounds","installed lab apparatus; injector filling",false);
        add(m,"Sterile medicae clean bench","equipment/lab/sterile",85,"medicae clinics, battlefield hospitals, licensed labs","clean preparation bench for legitimate medicine and expensive lies","installed lab apparatus; sterile medical compounding",false);
        add(m,"Cold storage locker","equipment/lab/cold-storage",46,"clinics, noble laboratories, voidship med-bays","sealed locker for unstable medical and luxury compounds","installed lab apparatus; storage requirement",false);
        add(m,"Aerosol bulb filler","equipment/lab/aerosol",50,"riot stores, manufactorum safety stations, black-market labs","pressure filler for inhalants, mists, and aerosolized bad judgment","installed lab apparatus; aerosolization",false);
        add(m,"Pressure-rated chem vessel","equipment/lab/pressure",64,"industrial labs, flamer shops, toxin rooms","reinforced vessel for pressurized fictional chemical processing","installed lab apparatus; pressure synthesis",false);
        add(m,"Fume hood","equipment/lab/safety",72,"licensed labs, security chem rooms, noble toxin cabinets","vented hood that admits the air would rather not be involved","installed lab apparatus; toxic handling",false);
        add(m,"Toxin lockbox","equipment/lab/toxin",54,"Arbites evidence stores, assassin rooms, security labs","sealed cabinet for poisonous reagents and evidentiary panic","installed lab apparatus; toxin/security storage",false);
        add(m,"Interrogation dosing cradle","equipment/lab/security",70,"Arbites black rooms, bounty cages, illegal clinics","restrained dosing rig for security chems and moral collapse","installed lab apparatus; interrogation/security preparation",false);
        add(m,"Labor dosing dispenser","equipment/lab/labor",34,"manufactorum clinics, shift offices, overseer stations","metered dispenser for productivity chemistry and workplace horror","installed lab apparatus; labor-control dosing",false);
        add(m,"Nootropic assay desk","equipment/lab/nootropic",55,"scribe offices, cogitator rooms, tech-ganger labs","quiet desk for focus compounds and cognitive endurance mixtures","installed lab apparatus; nootropic formulation",false);
        add(m,"Calibrated assay shrine","equipment/lab/mechanicus",92,"Mechanicus labs, forge shrines, medicae cogitator rooms","red-sealed assay station that measures, blesses, and judges reagents","installed lab apparatus; high-efficiency Mechanicus chemical control",false);
        add(m,"Spire perfumery glassware","equipment/lab/noble",96,"noble salons, private still rooms, luxury chem ateliers","delicate glassware for fashionable intoxicants and expensive poisons","installed lab apparatus; noble luxury infusion",false);
        add(m,"Luxury vialing station","equipment/lab/noble-packaging",74,"noble vice rooms, spire apothecaries, estate cellars","packaging bench for crystal vials, seals, and prestige dosage","installed lab apparatus; luxury vialing and presentation",false);
        add(m,"Ritual censer kiln","equipment/lab/ritual",45,"cult shrines, fringe chapels, illegal devotional markets","kiln for incense, ash, and liturgical contraband","installed lab apparatus; ritual incense preparation",false);
        add(m,"Forbidden preparation chamber","equipment/lab/forbidden",110,"hidden cult laboratories, witch dens, sealed vault rooms","warded chamber for perception drugs and other prosecutable mistakes","installed lab apparatus; forbidden/psyker preparation",false);
        add(m,"Warp-containment mirror box","equipment/lab/warp",130,"cult vaults, inquisitorial evidence lockers, heretek rooms","sealed mirror housing for tainted reflective materials","installed lab apparatus; warp-tainted material handling",false);
        add(m,"Mutant adaptation rack","equipment/lab/mutant",30,"mutant warrens, sump clinics, illegal flesh-shops","adjustable rack for body-adapted dosing and hide-bound repairs","installed lab apparatus; mutant/sump adaptation",false);
        add(m,"Voidship galley still","equipment/lab/void",34,"voidship underdecks, frontier bars, ash-waste rigs","compact still designed for ships, deserts, and poor supervision","installed lab apparatus; void/frontier brewing",false);
        add(m,"Ash-waste mineral leacher","equipment/lab/mineral",42,"ash-waste camps, reclamation yards, mutant stills","leaching drum for mineral tonics and contaminated dust","installed lab apparatus; mineral extraction",false);
        add(m,"Chemical waste trap","equipment/lab/waste",31,"sump labs, water refineries, illegal kitchens","trap for slurry, precipitate, and things nobody wants named","installed lab apparatus; waste capture",false);
    }

    private static void factionToolsAndProducts(LinkedHashMap<String,ItemDef> m){
        add(m,"Guard entrenching tool","tool/weapon",9,"Guard drill halls, munition stores, field kit","folding shovel that doubles as military doctrine applied to skulls","tool and melee weapon",true);
        add(m,"Quartermaster ledger slate","paperwork/tool",14,"Guard quartermaster counters and depot offices","rugged slate tracking rations, ammunition, and blame","reserved requisition/faction logistics support",false);
        add(m,"Mechanicus calibration probe","tool",16,"Mechanicus diagnostic bays, cable chapels, relic duct shrines","precision probe for asking machines why they hate you","mechanics and diagnostic tool",false);
        add(m,"Servo-skull maintenance kit","tool/component",22,"Mechanicus servo-skull rookeries and forge stores","tiny blessed parts for tiny blessed hovering problems","reserved drone/repair component",false);
        add(m,"Administratum stamp matrix","paperwork/tool",12,"archive counters, clerk training rooms, dead-file vaults","official stamp block useful for bureaucracy or forgery","reserved paperwork/social bypass",false);
        add(m,"Arbites restraint kit","security/tool",15,"Arbites holding cells and checkpoint stores","restraint bundle with tags, cuffs, seals, and threat value","security/social control tool",false);
        add(m,"Noble signet wax kit","paperwork/luxury",26,"noble offices, salons, audience antechambers","wax, seals, and little tools for pretending authority is clean","reserved high-status paperwork leverage",false);
        add(m,"Market scale set","commerce/tool",10,"sump storefronts and barter rows","portable scales calibrated by optimism and knife pressure","commerce/barter utility",false);
        add(m,"Rail cargo stencil kit","commerce/tool",8,"rail warehouses and train service yards","stencils, chalk, seals, and cargo tags for making goods become official","reserved cargo/provenance utility",false);
        add(m,"Scrap recycler gasket set","mechanical/component",11,"trash warrens, maintenance rooms, recycler sluices","seals and gaskets pulled from machines that still resent it","repair/build component",false);
        add(m,"Prayer candle bundle","devotional/ministorum",5,"Cult Imperialis temples, pilgrim markets, shrine stalls","cheap candles for legal prayer, debt grief, and performative repentance","devotional good; temple scavenge and trade",false);
        add(m,"Saint token","devotional/ministorum",7,"Cult Imperialis temples, Ecclesiarchy donation boxes, pilgrim stalls","pressed icon token of a local saint with institutional soot in the grooves","devotional good and social token",false);
        add(m,"Donation chit","paperwork/ministorum",4,"temple donation boxes, supplicant counters, charity kitchens","small stamped proof that the temple accepted something from someone","civil-religious paperwork",false);
        add(m,"Devotional pamphlet","paperwork/ministorum",3,"temple racks, pilgrim kitchens, Administratum-approved printers","thin printed sermon with more warnings than comfort","faith/knowledge reading good",false);
        add(m,"Thin soup ration","food/ministorum/charity",6,"supplicant kitchens, shrine pantries, pilgrim queues","watery temple soup designed to keep bodies upright through sermons","food; charity kitchen ration",false);
        add(m,"Pilgrim ration","food/ministorum/pilgrim",8,"pilgrim markets, temple kitchens, Ecclesiarchy supply lockers","portable ration stamped with a saint and tasting faintly of waxed paper","food; pilgrim travel ration",false);
    }

    private static void factionWeaponsAndAmmo(LinkedHashMap<String,ItemDef> m){
        add(m,"Guard lascarbine","weapon",34,"Imperial Guard armories, billet security rooms, rare military traders","rugged short-pattern las weapon with institutional ownership marks","weapon; deeper ranged/ammo API pending",true);
        add(m,"Las charge pack","ammo/energy",9,"Guard munition warehouses and quartermaster counters","standard charge pack with serial numbers filed down only sometimes","ammunition for reserved las weapons",false);
        add(m,"Guard trench knife","weapon",12,"Guard barracks, drill halls, field kit","broad military knife built for utility and close bad news","melee weapon",true);
        add(m,"Arbites shock maul","weapon/security",30,"Arbites armories, holding cell rows, precinct stores","authority on a stick with capacitors and paperwork behind it","weapon; stun-control API pending",true);
        add(m,"Arbites suppression shells","ammo/security",11,"Arbites contraband/evidence stores and checkpoint armories","less-lethal by legal definition rather than victim experience","ammunition for reserved suppression weapons",false);
        add(m,"Ganger chain cleaver","weapon/contraband",18,"gang stash rooms, chop shops, black-market stalls","chain-toothed intimidation device with maintenance problems","melee weapon",true);
        add(m,"Sawed-off stub shotgun","weapon/contraband",24,"ganger turf, stolen goods warehouses, sump black trade","short brutal firearm with a short brutal philosophy","ranged weapon; reloads through matching ammunition",true);
        add(m,"Shot shell handful","ammo",8,"gang stock, Arbites evidence, rail contraband crates","loose shells with too many fingerprints","ammunition for shotgun-pattern weapons",false);
        add(m,"Cult ritual blade","weapon/contraband",16,"cult chapels, offering stores, hidden sewer shrines","knife shaped for symbolism first and murder second, which is not better","melee weapon; reserved corruption/social heat",true);
        add(m,"Mutant bone maul","weapon",10,"mutant warrens, sewer camps, bone shrines","heavy bone club reinforced with scrap bands","melee weapon",true);
        add(m,"Mechanicus arc prod","weapon/tool",32,"Mechanicus diagnostic bays, inspection shrines, rare forge trade","electrical prod pretending to be a maintenance implement","weapon and machine-control tool",true);
        add(m,"Monoblade sliver","weapon/rare",45,"noble trophy galleries, Mechanicus relic duct caches","thin high-grade blade fragment too expensive to trust","rare melee weapon",true);
        add(m,"Rail spike hammer","weapon/tool",11,"train service yards, rail maintenance bays","work hammer for track hardware and poor decisions","tool and melee weapon",true);
        add(m,"Trash-hook spear","weapon",7,"trash warrens, sump camps, scavenger nests","long hook-spear assembled from refuse and spite","reach-like melee weapon profile",true);
        add(m,"Noble dueling pistol","weapon/luxury",62,"noble security galleries, trophy rooms, elite contraband","ornate pistol whose legal status depends on who asks","weapon; high-status heat risk",true);
        add(m,"Dueling pistol cartridge box","ammo/luxury",18,"noble pantries, security galleries, rare weapon vendors","expensive cartridges in a little box that smells like privilege","ammunition for reserved dueling pistols",false);
    }

    private static void importedWeaponFamilies(LinkedHashMap<String,ItemDef> m){
        // 0.8.90 WEAPON IMPORT PASS:
        // Seeded from the user-provided base weapon list and normalized into catalog families.
        // Named relics such as "Foehammer" are deliberately not registered as base items; later relic layers
        // should wrap these base families through provenance, quality, and special history instead.
        add(m,"Chainaxe","weapon/melee/chain",38,"gang chop shops, Guard contraband lockers, cult stores, rare armories","axe head with powered chain teeth and all the subtlety of a factory accident","melee weapon; chain family",true);
        add(m,"Chainsword","weapon/melee/chain",36,"Guard armories, noble trophy racks, ganger black trade","sword-length chain weapon requiring maintenance and poor judgment","melee weapon; chain family",true);
        add(m,"Force Sword","weapon/melee/psyker/rare",110,"sanctioned psyker vaults, noble relic rooms, forbidden caches","psy-reactive blade whose safe ownership requires forms no underhiver has seen","rare melee weapon; reserved psyker gating",true);
        add(m,"Power Axe","weapon/melee/power/rare",70,"military relic lockers, noble security galleries, Mechanicus custody stores","powered axe that turns armor into a negotiation it loses","melee weapon; power-field family",true);
        add(m,"Power Maul","weapon/melee/power/security",66,"Arbites precinct armories, noble guards, sanctioned security lockers","powered maul for official violence with good paperwork","melee weapon; power-field security family",true);
        add(m,"Power Pick","weapon/melee/power/industrial",58,"mining security stores, Mechanicus work vaults, underhive deep works","energized industrial pick adapted from labor tool into controlled brutality","tool and melee weapon",true);
        add(m,"Power Sword","weapon/melee/power/rare",75,"noble dueling halls, Guard officer lockers, relic caches","elegant powered blade that makes rank look like physics","melee weapon; power-field family",true);
        add(m,"Axe","weapon/melee/common",8,"hab workrooms, trash warrens, forager camps, maintenance lockers","ordinary cutting axe pressed into extraordinary unpleasantness","melee weapon and rough tool",true);
        add(m,"Knife","weapon/melee/common",5,"hab kitchens, worker kits, gangers, corpses, civilian drawers","simple blade; cheap, common, and therefore everywhere","melee weapon and utility blade",true);
        add(m,"Mono-Knife","weapon/melee/rare",32,"noble trophy drawers, Mechanicus microforge accidents, elite black trade","high-grade monomolecular knife too fine for most hands holding it","rare melee weapon",true);
        add(m,"Toxic Knife","weapon/melee/contraband",28,"cult stores, sump gangs, mutant toxin caches, black-market surgeons","knife carrying chemical assurance that the wound will keep negotiating","melee weapon; reserved toxin hook",true);
        add(m,"Bio-Dagger","weapon/melee/biological/rare",34,"mutant warrens, heretek tissue shops, forbidden medicae stores","organic or bio-treated dagger with living edge implications","melee weapon; reserved biological/corruption hook",true);
        add(m,"Munitorum Shovel","weapon/tool/military",9,"Guard field kit, quartermaster cages, abandoned trenches","folding military shovel with approved digging and skull-management applications","tool and melee weapon",true);
        add(m,"Sword","weapon/melee/common",11,"noble service rooms, old security closets, cult displays, civilian heirlooms","plain sword with more history than ammunition requirements","melee weapon",true);
        add(m,"Duelling Sword","weapon/melee/luxury",30,"noble salons, trophy security galleries, formal schools of expensive murder","slender status blade meant to make violence socially legible","melee weapon; high-status heat risk",true);
        add(m,"Industrial Hammer","weapon/tool/industrial",10,"machine rooms, train yards, forge floors, construction lockers","heavy work hammer that does not know when the shift ended","tool and melee weapon",true);
        add(m,"Thunder Hammer","weapon/melee/power/relic",105,"relic armories, noble vaults, Mechanicus sealed custody","massive powered hammer with institutional recoil and theological weight","rare melee weapon; power impact family",true);
        add(m,"Omnissian Axe","weapon/tool/mechanicus/relic",95,"Mechanicus shrines, senior tech-adept stores, relic ducts","Mechanicus authority axe that is equal parts tool, symbol, and threat","tool and melee weapon; Mechanicus rare",true);
        add(m,"Power Claymore","weapon/melee/power/rare",82,"noble armories, officer relic lockers, sealed family vaults","large powered sword for people with rank, reach, and a floor plan","melee weapon; power-field family",true);
        add(m,"Power Blade","weapon/melee/power/rare",60,"elite security stores, assassin caches, trophy drawers","short powered blade made for close work and expensive silence","melee weapon; power-field family",true);
        add(m,"Greatsword","weapon/melee/heavy",18,"noble halls, cult displays, archaic weapon cages","oversized blade more ceremonial than practical until it lands","heavy melee weapon",true);
        add(m,"Heavy Rock Saw","weapon/tool/industrial",42,"mining works, Mechanicus maintenance bays, deep construction sites","industrial saw meant for stone and now regrettably people","tool and melee weapon; chain/industrial family",true);
        add(m,"Emergency Cutter","weapon/tool/industrial",22,"transit safety lockers, maintenance walls, shipboard emergency cabinets","powered rescue cutter easily recontextualized by panic","tool and melee weapon",true);
        add(m,"Emergency Drill","weapon/tool/industrial",24,"maintenance lockers, mining rooms, emergency repair stations","rugged drill built for bulkheads, rockcrete, and bad improvisation","tool and melee weapon",true);
        add(m,"Psychic Staff","weapon/melee/psyker/rare",90,"sanctioned psyker cells, forbidden chapels, noble occult stores","focus staff for dangerous minds and people standing too close to them","rare melee weapon; reserved psyker gating",true);
        add(m,"Bolt Pistol","weapon/ranged/bolt",58,"officer lockers, noble guards, Arbites evidence cages, rare contraband","compact bolt weapon with ammunition worth more than most rooms","ranged weapon; bolt family",true);
        add(m,"Needle Pistol","weapon/ranged/toxin/rare",52,"noble assassins, medicae black trade, hidden trophy cabinets","quiet toxin pistol built for manners and murder","ranged weapon; reserved toxin hook",true);
        add(m,"Hand Flamer","weapon/ranged/flame",60,"Arbites riot stores, Guard lockers, cult caches, industrial hazard cages","pistol-scale flame weapon for solving corners badly","ranged weapon; flame family",true);
        add(m,"Laspistol","weapon/ranged/las",26,"Guard sidearm racks, Arbites lockers, civilian security dealers","sidearm las weapon with modest recoil and serious paperwork","ranged weapon; uses las charge packs",true);
        add(m,"Inferno Pistol","weapon/ranged/melta/rare",95,"noble relic vaults, elite security stores, forbidden wargear caches","short-range melta pistol for people who think doors are suggestions","rare ranged weapon; melta family",true);
        add(m,"Plasma Pistol","weapon/ranged/plasma/rare",88,"officer stores, Mechanicus custody cages, noble heirloom vaults","compact plasma weapon with status, heat, and terrible failure modes","rare ranged weapon; plasma family",true);
        add(m,"Autopistol","weapon/ranged/auto",22,"gang lockers, cheap security stalls, civilian contraband drawers","automatic pistol with cheap ammunition and cheaper decisions","ranged weapon; auto family",true);
        add(m,"Stub pistol","weapon/ranged/stub",15,"hab drawers, gangers, nervous vendors, corpses, pawn counters","basic solid-projectile pistol; the hive's argument punctuation","ranged weapon; stub family",true);
        add(m,"Stub Revolver","weapon/ranged/stub",18,"sump markets, old security closets, ganger sidearms","reliable revolver with simple parts and complicated ownership","ranged weapon; stub family",true);
        add(m,"Bolter","weapon/ranged/bolt",85,"Astartes-adjacent relic traffic excluded; human-scale officer, noble, and Arbites-controlled stock","human-scale bolt weapon; controlled, loud, and ruinously expensive","ranged weapon; bolt family",true);
        add(m,"Storm Bolter","weapon/ranged/bolt/heavy",130,"relic weapon vaults, turret stores, noble trophy security rooms","double-barrel bolt weapon whose logistics invoice could kill a hab block","heavy ranged weapon; bolt family",true);
        add(m,"Arc Rifle","weapon/ranged/mechanicus",78,"Mechanicus forge custody, diagnostic-armory stores, rare tech trade","electrical rifle with sanctioned maintenance rites and unsanctioned screaming","ranged weapon; Mechanicus arc family",true);
        add(m,"Needle Rifle","weapon/ranged/toxin/rare",72,"noble assassin stores, medicae contraband, hidden security nests","long toxin weapon built for quiet removals and expensive deniability","ranged weapon; toxin family",true);
        add(m,"Webber","weapon/ranged/security",64,"Arbites lockers, restraint stores, noble capture teams","web-projector for turning fugitives into paperwork with legs","ranged weapon; reserved restraint hook",true);
        add(m,"Acid Spitter","weapon/ranged/chemical/contraband",55,"mutant warrens, heretek chem shops, cult toxin stores","chemical projector that makes cover file a complaint","ranged weapon; chemical family",true);
        add(m,"Flamer","weapon/ranged/flame",80,"Guard armories, Arbites riot stores, cult theft, industrial hazard rooms","promethium weapon with simple doctrine and severe consequences","ranged weapon; flame family",true);
        add(m,"Lasgun","weapon/ranged/las",36,"Guard armories, militia stores, regulated surplus lockers","standard long las weapon; rugged and institutionally beloved","ranged weapon; uses las charge packs",true);
        add(m,"Longlas","weapon/ranged/las/marksman",48,"Guard marksman lockers, noble hunting galleries, covert overwatch nests","long precision las weapon for patient violence","ranged weapon; marksman las family",true);
        add(m,"Meltagun","weapon/ranged/melta",110,"Guard anti-armor lockers, Mechanicus custody, sealed wargear crates","short-range anti-armor weapon built to make matter reconsider","ranged weapon; melta family",true);
        add(m,"Plasma Gun","weapon/ranged/plasma",118,"Mechanicus custody, Guard special weapon cages, noble relic traffic","plasma weapon with catastrophic potential in both directions","ranged weapon; plasma family",true);
        add(m,"Heavy Plasma Gun","weapon/ranged/plasma/heavy",140,"heavy weapon stores, Mechanicus sealed vaults, controlled military stock","overbuilt plasma weapon requiring crew, cooling, or optimism","heavy ranged weapon; plasma family",true);
        add(m,"Autogun","weapon/ranged/auto",28,"gang stores, militia racks, civilian security rooms, freight contraband","automatic rifle using cheap solid ammunition","ranged weapon; auto family",true);
        add(m,"Shotgun","weapon/ranged/shotgun",30,"Arbites precincts, ganger lockers, worksite security cabinets","close-range firearm with broad social utility and poor subtlety","ranged weapon; shotgun family",true);
        add(m,"Sniper Rifle","weapon/ranged/marksman",60,"Guard marksman cells, noble hunting rooms, covert security nests","precision rifle for long patience and short conversations","ranged weapon; marksman family",true);
        add(m,"Marksman Rifle","weapon/ranged/marksman",42,"security overwatch stores, militia racks, hunting clubs","accurized rifle built for controlled distance","ranged weapon; marksman family",true);
        add(m,"Hunting Rifle","weapon/ranged/marksman",26,"sump hunters, noble game stores, frontier-style civilian cabinets","simple hunting rifle with local repairs and old blood in the stock","ranged weapon; civilian marksman family",true);
        add(m,"Stubcarbine","weapon/ranged/stub",20,"ganger lockers, hab militias, underhive pawn stalls","short solid-shot carbine with crude reliability","ranged weapon; stub family",true);
        add(m,"Heavy Bolter","weapon/ranged/bolt/heavy",150,"turret magazines, military heavy weapon cages, noble security emplacements","heavy bolt weapon better understood as portable architecture damage","heavy ranged weapon; bolt family",true);
        add(m,"Heavy Flamer","weapon/ranged/flame/heavy",125,"Guard assault stores, Arbites purge lockers, industrial hazard vaults","large flame weapon for rooms nobody plans to reuse soon","heavy ranged weapon; flame family",true);
        add(m,"Multi-Melta","weapon/ranged/melta/heavy",160,"anti-armor wargear vaults, Mechanicus sealed custody, rare military stock","heavy melta weapon whose logistics footprint is part of the threat","heavy ranged weapon; melta family",true);
        add(m,"Heavy Stubber","weapon/ranged/stub/heavy",80,"emplacement nests, gang fortress stores, militia checkpoints","belt-fed solid-shot support weapon with democratic recoil","heavy ranged weapon; stub family",true);
        add(m,"Autocannon","weapon/ranged/auto/heavy",175,"military hardpoints, turret stores, armored convoy caches","heavy automatic cannon intended for targets that do not fit through doors","heavy ranged weapon; auto-cannon family",true);
    }

    private static void underhiveImprovisedWeapons(LinkedHashMap<String,ItemDef> m){
        add(m,"Zip pistol","weapon/ranged/improvised",9,"hiver benches, ganger cubbies, trash warrens, pawn crates","single-shot or barely-repeating scrap pistol made from pipe, spring, and poor boundaries","ranged weapon; improvised firearm",true);
        add(m,"Pipe shotgun","weapon/ranged/improvised",14,"ganger chop shops, sump workshops, hidden hab benches","pipe-built scatter weapon that treats reloads as a prayer request","ranged weapon; improvised shotgun",true);
        add(m,"Scrap autogun","weapon/ranged/improvised",18,"gang workshops, stolen stock rooms, militia repair benches","autogun assembled from mismatched receivers, barrel sleeves, and spite","ranged weapon; improvised auto family",true);
        add(m,"Hiver nail rifle","weapon/ranged/improvised",12,"construction stores, worker riots, gang debt shops","compressed nail-launcher modified until safety resigned","ranged weapon; improvised industrial family",true);
        add(m,"Jury-rigged laslock","weapon/ranged/improvised/las",16,"hab electricians, militia closets, stripped las-cell lockers","crude las weapon coaxed from broken emitters and borrowed charge packs","ranged weapon; improvised las family",true);
        add(m,"Chem sprayer","weapon/ranged/chemical/improvised",13,"sump farms, sanitation rooms, ganger kitchens, mutant warrens","pressurized sprayer for cleaning, pest control, and regrettable crowds","ranged weapon; chemical improvised family",true);
        add(m,"Rebar maul","weapon/melee/improvised",6,"construction debris, collapsed rooms, mutant camps, trash warrens","length of rebar wrapped until it agrees to be a weapon","melee weapon; improvised blunt family",true);
        add(m,"Sump hook blade","weapon/melee/improvised",7,"sump markets, sewer camps, fishery wrecks, trash rooms","hooked blade made from tool steel and hungry practical thinking","melee weapon; improvised blade family",true);
        add(m,"Chain-wrapped club","weapon/melee/improvised",5,"gang barracks, trash warrens, riot piles","club improved with chain, nails, wire, and bad civic planning","melee weapon; improvised blunt family",true);
        add(m,"Pressure cutter spear","weapon/tool/improvised",11,"maintenance closets, rail service rooms, scavenger camps","long tool haft carrying a cutter head meant for pipes and now distance","tool and melee weapon",true);
        add(m,"Ganger buzz-cleaver","weapon/melee/chain/improvised",20,"ganger chop shops, fighting pits, stolen tool rooms","motorized cleaver that turns intimidation into maintenance debt","melee weapon; chain improvised family",true);
        add(m,"Hiver emergency breacher","weapon/tool/improvised",17,"evacuation lockers, blocked hab corridors, collapse caches","crowbar-cutter-drill hybrid made to open doors that disagree","tool and melee weapon",true);
        add(m,"Mutant scrap axe","weapon/melee/mutant/improvised",9,"mutant warrens, sump camps, scrap hoards","oversized axe of plate scrap, straps, and body-specific leverage","melee weapon; mutant improvised family",true);
        add(m,"Mutant tusk club","weapon/melee/mutant",8,"mutant warrens, bone shrines, sewer camps","bone-and-scrap club shaped around whoever can actually lift it","melee weapon; mutant family",true);
        add(m,"Heretic nail flail","weapon/melee/contraband/improvised",12,"cult sleeper crypts, hidden knife chapels, punishment rooms","chain flail of nails, tags, and devotional errors","melee weapon; cult improvised family",true);
        add(m,"Cult martyr pistol","weapon/ranged/contraband/improvised",15,"cult caches, sewer chapels, martyr cells","unreliable pistol built for one violent sermon and no aftercare","ranged weapon; cult improvised firearm",true);
        add(m,"Broken servo-arm club","weapon/tool/mechanicus/improvised",16,"forge scrap bins, rogue servitor wrecks, relic duct debris","dead servo-arm section carried like a holy tire iron","tool and melee weapon; Mechanicus salvage family",true);
    }

    private static void weaponAmmunitionFamilies(LinkedHashMap<String,ItemDef> m){
        add(m,"Frag grenade","weapon/explosive/grenade",22,"Guard lockers, gang caches, Arbites evidence cages, munition rooms","fragmentation grenade for clearing rooms, corridors, and friendships","thrown explosive weapon; fuse and blast behavior",true);
        add(m,"Krak grenade","weapon/explosive/grenade/anti-armor",34,"Guard anti-armor stores, Mechanicus custody, hardpoint lockers","focused explosive charge for armor, machines, and doors that asked for it","thrown anti-armor explosive weapon; focused blast behavior",true);
        add(m,"Smoke grenade","weapon/explosive/grenade/smoke",16,"Arbites riot stores, Guard issue crates, security desks","smoke canister for breaking vision and making everyone lie later","thrown smoke explosive; smoke/vision effect profile",true);
        add(m,"Melta grenade","weapon/explosive/grenade/melta",52,"anti-armor lockers, Mechanicus sealed issue, rare wargear rooms","compact melta charge for objects too rude to remain solid","thrown melta explosive; high-heat breach profile",true);
        add(m,"Plasma bomb","weapon/explosive/bomb/plasma",70,"Mechanicus sealed vaults, desperate Guard stores, noble black security","unstable plasma charge with a blast radius and a personnel problem","placed/thrown plasma explosive; unstable blast profile",true);
        add(m,"Satchel charge","weapon/explosive/charge",42,"demolition stores, rebel caches, construction theft, Guard engineers","bagged demolition charge for walls, machines, and bad plans","placed explosive charge; breach profile",true);
        add(m,"Tripwire mine","weapon/explosive/mine/tripwire",28,"gang ambush caches, militia stores, bad corridors","wire-triggered mine for hallway arguments","placed trap explosive; trap trigger profile",true);
        add(m,"Motion claymore","weapon/explosive/mine/directional",48,"security hardpoints, noble kill corridors, Guard stores","directional motion-triggered mine with strong opinions about frontage","placed directional explosive; cone blast profile",true);
        add(m,"Bouncing Betty","weapon/explosive/mine/bounding",44,"militia caches, old military stores, gang hardpoints","bounding fragmentation mine that rises before becoming everyone's problem","placed bounding explosive; trap trigger profile",true);
        add(m,"Bolt round magazine","ammo/bolt",24,"officer lockers, Arbites evidence stores, noble security vaults","small magazine of bolt rounds with a provenance trail worth hiding","ammunition for bolt-family weapons",false);
        add(m,"Autogun magazine","ammo/auto",7,"ganger caches, militia racks, pawn counters, security rooms","cheap solid-round magazine for autogun and autopistol families","ammunition for auto-family weapons",false);
        add(m,"Stub cartridge box","ammo/stub",5,"hab drawers, pawn stalls, gang lockers, old security closets","box of solid-shot cartridges for stub weapons","ammunition for stub-family weapons",false);
        add(m,"Needle toxin vial","ammo/toxin",18,"noble assassin stores, medicae black trade, chem shrines","small toxin charge for needle weapons and bad medical ethics","ammunition/reagent for needle-family weapons",false);
        add(m,"Promethium canister","ammo/flame",20,"Guard stores, industrial hazard cages, cult theft, Arbites lockers","sealed fuel canister for flame weapons and other terrible plans","ammunition/fuel for flame-family weapons",false);
        add(m,"Melta charge cell","ammo/melta",32,"anti-armor lockers, Mechanicus custody, rare wargear stores","charge cell for melta weapons; expensive enough to have enemies","ammunition for melta-family weapons",true);
        add(m,"Plasma flask","ammo/plasma",34,"Mechanicus sealed stores, Guard special weapon cages, noble vaults","plasma charge flask with cooling marks and nervous handling tags","ammunition for plasma-family weapons",false);
        add(m,"Web cartridge","ammo/security",16,"Arbites restraint lockers, noble capture teams, evidence cages","web-projector cartridge tagged for controlled issue","ammunition for webber/restraint weapons",false);
        add(m,"Arc capacitor pack","ammo/mechanicus",22,"Mechanicus diagnostic armories and cable shrines","charged capacitor pack for arc weapons and unsafe diagnostics","ammunition for arc-family weapons",false);
        add(m,"Heavy stubber belt","ammo/stub/heavy",14,"emplacement nests, gang strongrooms, militia checkpoints","belt of heavy stub rounds that clacks like bad news","ammunition for heavy stubber weapons",false);
        add(m,"Autocannon shell belt","ammo/auto/heavy",30,"military hardpoints, armored stores, turret magazines","heavy shell belt for autocannon weapons and structural arguments","ammunition for autocannon-family weapons",false);
    }

    private static void factionRecipeVariantIdentityItems(LinkedHashMap<String,ItemDef> m){
        // 0.8.95 FACTION MANUFACTURING IDENTITY VARIANTS:
        // Small marker/finish/inspection components used by generated faction recipes.
        // These are not final goods by themselves; they let provenance describe why a Guard,
        // Noble, Mechanicus, ganger, cult, mutant, scavver, or civic product differs materially.
        add(m,"Quartermaster stamp pad","component/faction/guard-stamp",4,"Guard depots, field stores, quartermaster desks","stamp pad and issue tag kit used to mark field-issue goods before they vanish into a ledger","faction identity input for Guard production variants",false);
        add(m,"Serialized casing tag","component/faction/serial-tag",5,"Arbites evidence rooms, Guard armories, lawful manufactories","serial plate or tag that makes ownership obvious and theft more interesting","faction identity input for lawful/security variants",false);
        add(m,"Mechanicus calibration seal","component/faction/mechanicus-seal",8,"forge cloisters, diagnostic benches, machine shrines","calibration seal and wafer scrap marking a product as ritually checked by somebody with red robes","faction identity input for Mechanicus variants",false);
        add(m,"Shrine-etched control tag","component/faction/mechanicus-tag",9,"Mechanicus stores, cable shrines, sanctified benches","small etched tag for controlled power assemblies and doctrinal maintenance claims","faction identity input for high-control Mechanicus variants",false);
        add(m,"House hallmark plate","component/faction/noble-hallmark",10,"noble workshops, estate stores, factor ledgers","noble house hallmark plate declaring expense before function has the chance","faction identity input for noble variants",false);
        add(m,"Gilding foil","component/faction/gilding",7,"noble service closets, ornament shops, stolen luxury stores","thin decorative foil for prestige finish, visible theft risk, and terrible priorities","faction identity input for noble status variants",false);
        add(m,"Gang color scrap","component/faction/gang-colors",3,"gang dens, chop shops, underhive tailors","painted scrap, cloth strip, or color badge marking local gang manufacture","faction identity input for ganger variants",false);
        add(m,"Intimidation spike set","component/faction/gang-spikes",4,"gang workshops, scrap shrines, unpleasant drawers","small spikes, hooks, and aggressive decoration for making cheap goods legally worse","faction identity input for ganger variants",false);
        add(m,"Contraband cipher tag","component/faction/contraband-tag",5,"black-market desks, hidden chapels, criminal caches","hidden mark or cipher proving illicit ownership to exactly the wrong people","faction identity input for illegal variants",false);
        add(m,"Profane mark stencil","component/faction/profane-mark",4,"cult cells, hidden shrines, burned paperwork drawers","ritual stencil for marks that make an object harder to explain during an inspection","faction identity input for cult variants",false);
        add(m,"Mutant fitment strap","component/faction/mutant-fitment",3,"mutant hollows, hide-working piles, sump repair mats","irregular strap and spacer kit for body-adapted equipment and crude repairs","faction identity input for mutant variants",false);
        add(m,"Salvage repair tag","component/faction/salvage-tag",2,"scavver benches, trash markets, repair stalls","tag, wire tie, or scratched note proving the item was repaired with confidence rather than standards","faction identity input for scavver variants",false);
        add(m,"Reclaimed repair bracket","component/faction/repair-bracket",3,"trash markets, machine junk bins, scavver carts","bent bracket used to keep improvised products together until tomorrow","faction identity input for improvised and repairable variants",false);
        add(m,"Civic inspection chit","component/faction/civic-chit",3,"hab offices, public counters, civic stores","small legitimacy token for ordinary shop-counter production","faction identity input for civilian variants",false);
    }

    private static void factionArmorAndClothing(LinkedHashMap<String,ItemDef> m){
        add(m,"Guard flak vest","armor/clothing",24,"Imperial Guard billet stores and field racks","military vest designed for fragments, not miracles","USE equips as armor-like clothing",false);
        add(m,"Arbites riot visor","armor/clothing",18,"Arbites precinct stores and evidence cages","face shield with scratches at citizen height","reserved head/face protection",false);
        add(m,"Mechanicus rubberized apron","clothing/armor",14,"forge cloisters and machine shops","heavy apron against sparks, fluids, and casual humanity","USE equips as protective workwear",false);
        add(m,"Noble fur-lined coat","clothing/luxury",40,"noble dormitories, salons, service closets","lavish coat that converts warmth into social offense","USE equips as clothing/disguise",false);
        add(m,"Market vendor sash","clothing/commerce",9,"sump storefronts and barter rows","bright sash marking someone as economically vulnerable but tolerated","USE equips as market disguise",false);
        add(m,"Cult hooded wrap","clothing/contraband",12,"cult sleeper crypts and hidden chapels","hooded cloth with too many intentional stains","USE equips as risky disguise",false);
        add(m,"Rail worker hazard coat","clothing/armor",13,"rail depots and train service yards","reflective heavy coat for freight work and industrial shoving","USE equips as workwear/disguise",false);
        add(m,"Child minder apron","clothing/civic",6,"creches, daycares, civilian kitchens","apron with pockets for snacks, rags, and despair","USE equips as civilian work disguise",false);
    }

    private static void booksTrainingAndCivicGoods(LinkedHashMap<String,ItemDef> m){
        add(m,"Primer slate","knowledge/tool",9,"public learning closets, Administratum instruction rooms","basic learning slate full of approved facts and missing context","reserved knowledge training support",false);
        add(m,"Guard drill manual","knowledge/military",12,"Guard drill halls and barracks lockers","manual explaining how to march, shoot, obey, and fill forms afterward","reserved training/Firearms support",false);
        add(m,"Mechanicus catechism strip","knowledge/mechanicus",18,"cable chapels, nutrient galleys, forge shrines","thin strip of machine-prayers and maintenance order","reserved Mechanicus knowledge support",false);
        add(m,"Arbites casebook excerpt","knowledge/law",15,"precinct training rooms and complaint counters","legal examples selected to make authority look inevitable","reserved law/social knowledge support",false);
        add(m,"Noble etiquette card","knowledge/social",20,"noble service spines, salons, dining galleries","laminated etiquette card for surviving rooms with better carpets than morals","reserved noble social support",false);
        add(m,"Creche lesson toy","toy/knowledge",5,"daycares, creches, public learning rooms","toy cog, number blocks, or safety puppet from childhood logistics","trade or reserved morale/social hook",false);
        add(m,"Warehouse inventory tag bundle","commerce/paperwork",6,"warehouses, rail depots, product stores","bundle of tags that make piles of stuff easier to steal or count","reserved provenance/logistics utility",false);
        add(m,"Kitchen grease tin","chemical/junk",3,"cafeterias, kitchens, food courts","grease tin useful for cooking, slipping, burning, or machines in a bad mood","reserved crafting/maintenance material",false);
    }
    private static void hivewallCacheGoods(LinkedHashMap<String,ItemDef> m){
        add(m,"Sealed maintenance tool chest","tool/cache",26,"hivewall maintenance rooms and abandoned interwall caches","sealed tool chest left by exterior-wall crews before the corridor was forgotten","open/use as reserved maintenance-tool cache; trade as valuable tools",false);
        add(m,"Interwall ration reserve crate","food/cache",18,"hivewall maintenance rooms, sealed refuge pockets, abandoned interwall danger rooms","old ration crate packed for workers who never came back","consume or break down into food supplies",false);
        add(m,"Voidside water condenser flask","water/tool",14,"hivewall maintenance rooms and exterior service corridors","rugged flask with condenser fittings for miserable maintenance shifts near open abyss","consume/use for water; reserved maintenance support",false);
        add(m,"Forgotten flak bundle","armor/cache",32,"abandoned interwall armories and sealed gang/military caches","wrapped bundle of armor plates, straps, and old bloodless impact scars","USE equips as crude armor bundle once equipment API deepens",false);
        add(m,"Abandoned las-locker contents","weapon/cache",48,"sealed Guard or Arbites interwall caches","locked weapons bundle from an abandoned security locker","weapon cache; reserved unpack action",true);
        add(m,"Rogue automata service core","mechanical/rare",60,"abandoned Mechanicus service works and rogue automata remains","heavy service core with enough machine-spirit spite left to matter","rare component for reserved automata/Mechanicus systems",false);
        add(m,"Sealed tech relic case","technology/rare",85,"Mechanicus relic ducts, interwall sealed rooms, forgotten maintenance vaults","sealed case of unknown device fragments and doctrinal warning labels","rare technology cache; reserved research/provenance unlock",false);
        add(m,"Wanted criminal stash roll","contraband/cache",30,"abandoned interwall hideouts and bandit refuge rooms","rolled cloth bundle of script, blades, keys, and heat you can inherit by touching it","contraband cache; trade or reserved crime/social heat hook",false);
        add(m,"Cult-sealed reliquary packet","contraband/rare",42,"cult interwall chambers and sealed devotional caches","packet of prohibited relics wrapped in oaths and bad decisions","contraband/religious cache; reserved corruption and faction leverage",false);
        add(m,"Collapsed hive salvage crate","supplies/cache",20,"collapsed interstitial pockets and abandoned exterior wall rooms","crate of tools, wire, brackets, and repair supplies shaken loose by old hive collapse","build/repair supplies and salvage",false);
    }

}


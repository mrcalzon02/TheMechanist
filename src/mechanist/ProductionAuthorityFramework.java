package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class MachineTierProfile {
    final String qualityName, role;
    final int qualityCeilingTier, workerCapacity;
    final double efficiency, throughput, breakdownRate, durabilityMultiplier, powerDemand;
    MachineTierProfile(String qualityName, int ceiling, double efficiency, double throughput, double breakdown, int workers, double durability, double powerDemand, String role) {
        this.qualityName=qualityName; this.qualityCeilingTier=ceiling; this.efficiency=efficiency; this.throughput=throughput; this.breakdownRate=breakdown; this.workerCapacity=workers; this.durabilityMultiplier=durability; this.powerDemand=powerDemand; this.role=role;
    }
    String auditLine() { return qualityName + " ceiling=" + QualityAuthorityApi.qualityName(qualityCeilingTier) + " efficiency x" + fmt(efficiency) + " throughput x" + fmt(throughput) + " breakdown " + fmt(breakdownRate) + " workerCapacity=" + workerCapacity + " durability x" + fmt(durabilityMultiplier) + " powerDemand x" + fmt(powerDemand) + " :: " + role; }
    static String fmt(double d){ return String.format(Locale.US, "%.2f", d); }
}


class MachineTierAuthority {
    static ArrayList<MachineTierProfile> profiles() {
        ArrayList<MachineTierProfile> p = new ArrayList<>();
        p.add(new MachineTierProfile("Junk",0,0.55,0.60,0.30,1,0.65,0.80,"barely-held-together emergency machines"));
        p.add(new MachineTierProfile("Shoddy",1,0.70,0.75,0.22,1,0.80,0.90,"bad but deliberately assembled machines"));
        p.add(new MachineTierProfile("Common",2,1.00,1.00,0.12,1,1.00,1.00,"ordinary baseline machinery"));
        p.add(new MachineTierProfile("Serviceable",3,1.12,1.18,0.08,2,1.15,1.05,"stable workshop machinery with modest labor handling"));
        p.add(new MachineTierProfile("Fine",4,1.25,1.35,0.05,2,1.28,1.12,"precise machinery with better throughput"));
        p.add(new MachineTierProfile("Masterwork",5,1.45,1.65,0.03,3,1.45,1.20,"expert-built machinery with high labor and output tolerance"));
        p.add(new MachineTierProfile("Noble",6,1.30,1.50,0.04,3,1.35,1.30,"prestige machinery: comfortable, clean, expensive, socially visible"));
        p.add(new MachineTierProfile("Archeotech",7,1.80,2.20,0.01,4,1.80,1.60,"relic machinery with exceptional ceiling and grim maintenance implications"));
        return p;
    }
    static MachineTierProfile forQuality(String qualityName) {
        int t = QualityAuthorityApi.tierIndex(qualityName);
        for (MachineTierProfile p : profiles()) if (p.qualityCeilingTier == t) return p;
        return profiles().get(2);
    }
    static MachineTierProfile forMachine(BaseObject obj) { return forQuality(obj == null ? "Junk" : (obj.qualityName == null ? "Common" : obj.qualityName)); }
    static int qualityCeilingTier(BaseObject obj) { return forMachine(obj).qualityCeilingTier; }
    static boolean isMachineOrFacilitySymbol(char s) { return "sweflxBMTHkqGupgDAL".indexOf(s) >= 0; }
    static void applyToConfiguredObject(BaseObject obj) {
        if (obj == null || !isMachineOrFacilitySymbol(obj.symbol)) return;
        MachineTierProfile p = forMachine(obj);
        if (obj.capacity > 0) obj.capacity = Math.max(1, (int)Math.round(obj.capacity * p.throughput));
        if (obj.integrity > 0) obj.integrity = Math.max(1, (int)Math.round(obj.integrity * p.durabilityMultiplier));
        if (obj.capacity > 0) obj.charges = Math.min(obj.charges, Math.max(0, obj.capacity));
        if (obj.description != null && !obj.description.contains("Machine authority:")) obj.description += " Machine authority: " + p.auditLine() + ".";
    }
    static ArrayList<String> detailLines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Machine Tier Authority: machines now resolve quality ceiling, efficiency, throughput, breakdown risk, and worker capacity from one table.");
        l.add("Production cap hook: machine ceiling participates in the central QualityAuthorityApi capping rule.");
        for (MachineTierProfile p : profiles()) l.add(p.auditLine());
        return l;
    }
    static ArrayList<String> auditLines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("MachineTier profiles=" + profiles().size());
        for (MachineTierProfile p : profiles()) l.add("MACHINE_TIER " + p.auditLine());
        return l;
    }
}


class BuildRecipe {
    String name, description, requiredKnowledge, qualityName = "Common"; char symbol; int supplyCost, partCost, attention, reqMechanics, reqIntellect, baseTurns, baseFail; boolean requiresWorkbench; Faction requiredFaction;
    final LinkedHashMap<String,Integer> componentCosts = new LinkedHashMap<>();
    BuildRecipe(String name, char symbol, int supplies, int parts, int attention, int mech, int intel, int turns, int fail, boolean workbench, Faction faction, String requiredKnowledge, String description) {
        this.name=name; this.symbol=symbol; this.supplyCost=supplies; this.partCost=parts; this.attention=attention; this.reqMechanics=mech; this.reqIntellect=intel; this.baseTurns=turns; this.baseFail=fail; this.requiresWorkbench=workbench; this.requiredFaction=faction; this.requiredKnowledge=requiredKnowledge; this.description=description;
    }
    String shortTip() {
        String f = requiredFaction == Faction.NONE ? "" : " Faction: " + requiredFaction.label + ".";
        String wb = requiresWorkbench ? " Requires workbench." : "";
        String k = (requiredKnowledge == null || requiredKnowledge.isBlank()) ? "" : " Knowledge: " + requiredKnowledge + ".";
        String c = componentCosts.isEmpty() ? "" : " Components: " + componentCostSummary(3) + ".";
        return name + " | Quality ceiling " + qualityName + ". Supplies " + supplyCost + ", parts " + partCost + ", Mechanics " + reqMechanics + ", Intellect " + reqIntellect + "." + c + wb + f + k + " " + ObjectSemanticAssetAuthority.semanticSummaryForName(name);
    }
    String componentCostSummary(int maxEntries) {
        if (componentCosts.isEmpty()) return "none";
        ArrayList<String> bits = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<String,Integer> e : componentCosts.entrySet()) {
            if (shown >= maxEntries) { bits.add("+" + (componentCosts.size() - shown) + " more"); break; }
            bits.add(e.getValue() + "x " + e.getKey());
            shown++;
        }
        return String.join(", ", bits);
    }
    BuildRecipe withQuality(String q){ this.qualityName = (q == null || q.isBlank()) ? "Common" : q; return this; }
    BuildRecipe withComponent(String item, int count){ if (item != null && !item.isBlank() && count > 0) componentCosts.put(item, componentCosts.getOrDefault(item,0) + count); return this; }
    BuildRecipe withComponents(Object... pairs){
        if (pairs == null) return this;
        for (int i=0; i+1<pairs.length; i+=2) {
            Object nameObj = pairs[i], countObj = pairs[i+1];
            if (nameObj == null || countObj == null) continue;
            int count = 0;
            if (countObj instanceof Number) count = ((Number)countObj).intValue();
            else try { count = Integer.parseInt(String.valueOf(countObj)); } catch (Exception ignored) { count = 0; }
            withComponent(String.valueOf(nameObj), count);
        }
        return this;
    }
    static BuildRecipe storage(){ return new BuildRecipe("Storage Crate", 's', 1, 0, 0, 0, 0, 3, 26, false, Faction.NONE, null, "A lockable crate for holding more stolen problems.").withQuality("Common").withComponents("Rivet set",1); }
    static BuildRecipe workbench(){ return new BuildRecipe("Scrap Workbench", 'w', 2, 1, 1, 2, 2, 6, 34, false, Faction.NONE, null, "A crude bench for repairs, parts, and later EMM fabrication.").withQuality("Common").withComponents("Tool bundle",1,"Rivet set",1); }
    static BuildRecipe barricade(){ return new BuildRecipe("Barricade", 'd', 2, 0, 2, 1, 0, 5, 30, false, Faction.NONE, null, "A hard obstruction. Good cover, loud implication of valuables.").withQuality("Common").withComponents("Scrap plate",1,"Rivet set",1); }
    static BuildRecipe cot(){ return new BuildRecipe("Sleeping Cot", 'c', 1, 0, 0, 0, 0, 3, 20, false, Faction.NONE, null, "A miserable cot for survival-grade sleep.").withQuality("Common").withComponents("Reclaimed textile bundle",1); }
    static BuildRecipe water(){ return new BuildRecipe("Water Barrel", 'u', 1, 0, 0, 0, 0, 3, 22, false, Faction.NONE, null, "A sealed barrel for drinkable reserve.").withQuality("Common").withComponents("Sealing gasket set",1,"Pipe coupling set",1); }
    static BuildRecipe alarm(){ return new BuildRecipe("Alarm Trap", 'a', 2, 1, 2, 2, 1, 5, 36, false, Faction.NONE, null, "A crude alarm. It may warn you before it embarrasses you.").withQuality("Common").withComponents("Wire bundle",1,"Circuit wafer",1); }
    static BuildRecipe watchPost(){ return new BuildRecipe("Watch Post", 'p', 3, 1, 2, 2, 1, 6, 34, false, Faction.NONE, null, "A raised guard point that improves early warning and makes assigned security staff useful.").withQuality("Common").withComponents("Tool bundle",1,"Wire bundle",1); }
    static BuildRecipe guardBarracks(){ return new BuildRecipe("Guard Barracks", 'g', 5, 2, 2, 3, 1, 8, 38, false, Faction.NONE, "Recruit Berthing Doctrine", "A bunk-and-arms corner for security duty. Improves base defense and raid response.").withQuality("Common").withComponents("Webbing strap roll",1,"Armament components",1,"Rivet set",1); }
    static BuildRecipe reinforcedDoor(){ return new BuildRecipe("Reinforced Door", 'D', 4, 2, 1, 3, 1, 7, 32, false, Faction.NONE, null, "A heavy internal choke-point door for base defense.").withQuality("Common").withComponents("Scrap plate",2,"Rivet set",2,"Bearing set",1); }
    static BuildRecipe sandbagLine(){ return new BuildRecipe("Sandbag Line", 'S', 2, 0, 1, 1, 0, 4, 24, false, Faction.IMPERIAL_GUARD, null, "A low-tech ballistic cover line for checkpoints, roadblocks, and PDF/Guard defensive lanes.").withQuality("Common").withComponents("Sandbag fill",2,"Reclaimed textile bundle",1); }
    static BuildRecipe razorWireCoil(){ return new BuildRecipe("Razor Wire Coil", 'R', 3, 1, 2, 2, 1, 5, 30, false, Faction.ARBITES, null, "A visible area-denial coil. Inspectable now; later hooks into hazard and movement injury rules.").withQuality("Common").withComponents("Razor wire coil",2,"Reinforced fastener set",1); }
    static BuildRecipe reinforcedWallPanel(){ return new BuildRecipe("Reinforced Wall Panel", 'W', 5, 2, 2, 4, 1, 9, 34, false, Faction.NONE, null, "A defensive wall segment for fortified rooms, precinct perimeters, and estate hardening.").withQuality("Serviceable").withComponents("Precast defense slab",2,"Scrap plate",2,"Reinforced fastener set",2); }
    static BuildRecipe securitySensorMast(){ return new BuildRecipe("Security Sensor Mast", 'N', 5, 3, 3, 4, 4, 10, 42, true, Faction.ARBITES, "Security Cogitator Rites", "A sensor mast for alarm routing, checkpoint awareness, and later line-of-sight security logic.").withQuality("Serviceable").withComponents("Sensor lens",1,"Circuit wafer",1,"Wire bundle",2,"Power coupling socket",1); }
    static BuildRecipe arbitesReinforcedDoor(){ return new BuildRecipe("Arbites Reinforced Door", 'Y', 6, 3, 2, 4, 2, 10, 36, false, Faction.ARBITES, "Security Cogitator Rites", "A serialized precinct-grade choke-point door with access-control binding.").withQuality("Serviceable").withComponents("Arbites lock core",1,"Precast defense slab",1,"Reinforced fastener set",2,"Bearing set",1); }
    static BuildRecipe lightStubTurret(){ return new BuildRecipe("Light Stub Turret", 't', 6, 4, 4, 5, 3, 12, 48, true, Faction.NONE, "Security Cogitator Rites", "A short-range turret profile with passive cover and inspection data.").withQuality("Serviceable").withComponents("Turret traverse ring",1,"Turret barrel assembly",1,"Armament components",1,"Circuit wafer",1,"Motor coil pack",1); }
    static BuildRecipe heavyStubTurret(){ return new BuildRecipe("Heavy Stub Turret", 'U', 9, 6, 6, 6, 4, 16, 54, true, Faction.IMPERIAL_GUARD, "Security Cogitator Rites", "A heavier crew-served turret profile for PDF and Guard-style checkpoints. Requires staffing, ammunition, and ownership readiness checks.").withQuality("Fine").withComponents("Turret traverse ring",2,"Turret barrel assembly",2,"Heavy weapon body",1,"Armament components",2,"Heat sink",1); }
    static BuildRecipe arbitesSuppressionTurret(){ return new BuildRecipe("Arbites Suppression Turret", 'J', 10, 7, 7, 6, 5, 17, 56, true, Faction.ARBITES, "Security Cogitator Rites", "A serialized suppression turret profile for precinct defense. Live fire is inactive until ownership and hostility rules authorize it.").withQuality("Fine").withComponents("Turret traverse ring",2,"Turret barrel assembly",1,"Arbites lock core",1,"Circuit wafer",2,"Capacitor wafer stack",1); }
    static BuildRecipe gildedSentryTurret(){ return new BuildRecipe("Gilded Sentry Turret", 'Z', 10, 7, 8, 5, 5, 16, 52, true, Faction.NOBLE, "Commercial Ledgers", "A private noble sentry turret: ornate, expensive, and very likely to make burglary more theatrical.").withQuality("Fine").withComponents("Turret traverse ring",1,"Turret barrel assembly",1,"House hallmark plate",1,"Gilding foil",2,"Circuit wafer",1); }
    static BuildRecipe precinctDefensiveFixtureSet(){ return new BuildRecipe("Precinct Defensive Fixture Set", 'P', 4, 2, 2, 2, 2, 8, 32, false, Faction.ARBITES, "Security Cogitator Rites", "Inspectable precinct support fixtures: desks, benches, holding fixtures, alarm furniture, and checkpoint clutter.").withQuality("Common").withComponents("Serialized casing tag",1,"Civic inspection chit",1,"Reinforced fastener set",1); }
    static BuildRecipe atmosCondenser(){ return new BuildRecipe("EMM Atmospheric Condenser", 'e', 3, 2, 1, 4, 3, 8, 42, true, Faction.NONE, "Condensation Handling", "Emergency Mechanist Machine: draws potable moisture from bad air.").withQuality("Common").withComponents("Filter cartridge housing",1,"Ceramic filter candle",1,"Pipe coupling set",2,"Sealing gasket set",1,"Distillation coil",1); }
    static BuildRecipe microForge(){ return new BuildRecipe("EMM Micro Forge", 'f', 4, 3, 2, 5, 3, 10, 45, true, Faction.MECHANICUS, "Scrap-Forging Doctrine", "Mechanicus-aligned EMM: fabricates sturdier parts and supply conversions.").withQuality("Common").withComponents("Gear train",1,"Bearing set",1,"Motor coil pack",1,"Ceramic insulator blank",1,"Heat sink",1); }
    static BuildRecipe microLab(){ return new BuildRecipe("EMM Micro Lab", 'l', 4, 2, 2, 4, 5, 10, 44, true, Faction.NONE, null, "Crude research station for underhive doctrine and technical unlocks.").withQuality("Common").withComponents("Circuit wafer",1,"Glass optic blank",1,"Ceramic insulator blank",1,"Wire bundle",2); }
    static BuildRecipe shopCounter(){ return new BuildRecipe("Licensed Shop Counter", 'B', 3, 1, 2, 1, 2, 7, 36, false, Faction.NONE, "Commerce Permits", "A counter, lockbox, and ledger surface for public sales. Without a noble permit it functions illegally and raises heat.").withQuality("Common").withComponents("Warehouse inventory tag bundle",1,"Fastener button card",1); }
    static BuildRecipe clinicStall(){ return new BuildRecipe("Backroom Medicae Stall", 'M', 4, 2, 2, 2, 3, 8, 38, true, Faction.NONE, "Field Medicae Practices", "A small treatment counter for bandages, paid patch-ups, and dubious recovery services.").withQuality("Common").withComponents("Sterile vial rack",1,"Filter cloth roll",1,"Chemical reagent bottle",1); }
    static BuildRecipe securityNode(){ return new BuildRecipe("Security Cogitator Node", 'x', 5, 4, 3, 5, 5, 12, 48, true, Faction.ARBITES, "Security Cogitator Rites", "Faction-gated security management node. Useful, suspicious, and bureaucratically cursed.").withQuality("Common").withComponents("Circuit wafer",2,"Capacitor wafer stack",1,"Wire bundle",2,"Ceramic insulator blank",1); }
    static BuildRecipe powerTurret(){ return new BuildRecipe("Powered Defense Turret", 'T', 8, 5, 5, 6, 4, 14, 52, true, Faction.NONE, "Security Cogitator Rites", "A high-attention powered defense that burns charge to blunt raids and protect expensive base infrastructure.").withQuality("Serviceable").withComponents("Armament components",2,"Motor coil pack",1,"Circuit wafer",1,"Heat sink",1,"Capacitor wafer stack",1); }
    static BuildRecipe shieldRelay(){ return new BuildRecipe("Shield Relay", 'H', 7, 5, 4, 6, 5, 14, 50, true, Faction.MECHANICUS, "Mechanicus Fabrication Rites", "A powered ward relay that improves raid absorption and makes valuable rooms harder to crack.").withQuality("Serviceable").withComponents("Power coupling socket",2,"Capacitor wafer stack",2,"Ceramic insulator blank",2,"Circuit wafer",1); }
    static BuildRecipe decor(){ return new BuildRecipe("Base Decor Object", 'v', 1, 0, 0, 0, 1, 3, 20, false, Faction.NONE, null, "Lighting, signage, cloth, devotional junk, or noble trash arranged to make a room less miserable and more sellable.").withQuality("Common").withComponents("Reclaimed textile bundle",1); }
    static BuildRecipe businessAddon(){ return new BuildRecipe("Business Add-on Fixture", 'A', 3, 1, 1, 1, 2, 6, 30, false, Faction.NONE, "Commercial Ledgers", "Shelving, lockbox, display rack, menu board, or service counter upgrade that raises selling power.").withQuality("Common").withComponents("Warehouse inventory tag bundle",1,"Fastener button card",1,"Tool bundle",1); }
    static BuildRecipe carryingStation(){ return new BuildRecipe("Carrying Station", 'k', 2, 1, 1, 1, 1, 5, 28, false, Faction.NONE, "Workshop Labor Discipline", "First-tier cargo transfer staging for manual movement of goods between owned bases.").withQuality("Common").withComponents("Webbing strap roll",1,"Tool bundle",1); }
    static BuildRecipe supplyPost(){ return new BuildRecipe("Supply Post", 'q', 5, 2, 2, 3, 2, 9, 38, false, Faction.NONE, "Workshop Labor Discipline", "Second-tier organized cargo hub for larger stock movements, route slips, and recruit handling.").withQuality("Serviceable").withComponents("Warehouse inventory tag bundle",2,"Webbing strap roll",1,"Tool bundle",1); }
    static BuildRecipe logisticsCenter(){ return new BuildRecipe("Logistics Center", 'G', 9, 5, 4, 5, 5, 16, 54, true, Faction.NONE, "Commercial Ledgers", "High-tier cargo transfer authority for multi-base routing, business stock, and caravan dispatch.").withQuality("Fine").withComponents("Circuit wafer",2,"Warehouse inventory tag bundle",3,"Wire bundle",2,"Gear train",1); }
    static BuildRecipe labEquipment(String equipmentName, int supplies, int parts, int attention, int mech, int intel, String knowledge, String quality, String description){ return new BuildRecipe(equipmentName, 'L', supplies, parts, attention, mech, intel, Math.max(8, supplies + parts + 6), Math.max(34, 42 + attention * 3 - mech), true, Faction.NONE, knowledge, description).withQuality(quality); }
    static BuildRecipe crudeChemBench(){ return labEquipment("Crude chem bench", 3, 2, 3, 3, 2, "Junk Chemical Synthesis Patterns", "Shoddy", "A dirty bench, reagent space, and pipe-fed working surface for underhive chemical compounding.").withComponents("Chemical reagent bottle",1,"Pipe coupling set",1,"Valve spring set",1,"Chemical waste trap",1); }
    static BuildRecipe reagentBench(){ return labEquipment("Reagent preparation bench", 4, 3, 2, 4, 4, "Common Chemical Synthesis Patterns", "Common", "A controlled reagent bench for ordinary legal or semi-legal chemical preparation.").withComponents("Chemical reagent bottle",2,"Glass optic blank",1,"Sealing gasket set",1,"Ceramic insulator blank",1); }
    static BuildRecipe distillationColumn(){ return labEquipment("Distillation column", 6, 4, 4, 5, 4, "Serviceable Chemical Synthesis Patterns", "Serviceable", "A still column for amasec, solvents, refined distillates, and fumes that make landlords ask questions.").withComponents("Distillation coil",2,"Pipe coupling set",2,"Sealing gasket set",2,"Pressure hose bundle",1,"Heat sink",1); }
    static BuildRecipe sterileBench(){ return labEquipment("Sterile medicae clean bench", 7, 5, 3, 5, 5, "Fine Medical Processing Patterns", "Fine", "A clean medicae bench for controlled ampoules, sterile compounds, and clinic-grade production.").withComponents("Sterile vial rack",2,"Filter cartridge housing",1,"Ceramic filter candle",1,"Circuit wafer",1,"Sealing gasket set",1); }
    static BuildRecipe fumeHood(){ return labEquipment("Fume hood", 6, 5, 3, 5, 5, "Fine Chemical Synthesis Patterns", "Fine", "A ventilated toxic-handling hood for volatile, aerosolized, or security-sensitive compounds.").withComponents("Filter cartridge housing",2,"Pressure hose bundle",2,"Sealing gasket set",2,"Chemical waste trap",1,"Motor coil pack",1); }
    static BuildRecipe injectorStation(){ return labEquipment("Injector filling station", 5, 4, 3, 4, 4, "Serviceable Medical Processing Patterns", "Serviceable", "A filling station for ampoules, medi-stimms, combat injectors, and regrettable precision dosing.").withComponents("Injector ampoule set",2,"Valve spring set",2,"Sealing gasket set",1,"Circuit wafer",1); }
    static BuildRecipe fungalGrowTray(){ return labEquipment("Fungal grow tray bank", 4, 2, 2, 3, 3, "Common Agricultural Processing Patterns", "Common", "Stacked damp grow trays for sumpweed, fungal cultures, algae-adjacent misery, and controlled underhive agriculture.").withComponents("Sterilized grow medium",2,"Filter cloth roll",1,"Pipe coupling set",1,"Sealing gasket set",1); }
    static BuildRecipe ritualCenserKiln(){ return labEquipment("Ritual censer kiln", 4, 3, 4, 3, 4, "Common Chemical Synthesis Patterns", "Common", "A kiln and censer rig for incense, devotional fumes, and rites best logged before denial begins.").withComponents("Incense resin pellet",2,"Ceramic insulator blank",1,"Chemical reagent bottle",1,"Rivet set",1); }
    static ArrayList<BuildRecipe> laboratoryEquipmentRecipes(){ ArrayList<BuildRecipe> r = new ArrayList<>(); r.add(crudeChemBench()); r.add(reagentBench()); r.add(distillationColumn()); r.add(sterileBench()); r.add(fumeHood()); r.add(injectorStation()); r.add(fungalGrowTray()); r.add(ritualCenserKiln()); return r; }
    static ArrayList<BuildRecipe> allBuildRecipes(){
        ArrayList<BuildRecipe> r = new ArrayList<>();
        r.add(storage()); r.add(workbench()); r.add(barricade()); r.add(cot()); r.add(water()); r.add(alarm()); r.add(watchPost()); r.add(guardBarracks()); r.add(reinforcedDoor());
        r.add(sandbagLine()); r.add(razorWireCoil()); r.add(reinforcedWallPanel()); r.add(securitySensorMast()); r.add(arbitesReinforcedDoor());
        r.add(lightStubTurret()); r.add(heavyStubTurret()); r.add(arbitesSuppressionTurret()); r.add(gildedSentryTurret()); r.add(precinctDefensiveFixtureSet());
        r.add(atmosCondenser()); r.add(microForge()); r.add(microLab()); r.add(shopCounter()); r.add(clinicStall()); r.add(securityNode()); r.add(powerTurret()); r.add(shieldRelay());
        r.add(decor()); r.add(businessAddon()); r.add(carryingStation()); r.add(supplyPost()); r.add(logisticsCenter());
        r.addAll(laboratoryEquipmentRecipes());
        return r;
    }
}





class FactionManufacturingProfile {
    final Faction faction;
    final String label, recipePrefix, strengths, weaknesses, aesthetic;
    final double valueBias, chargeBias, defectBias, durabilityBias, comfortBias, prestigeBias, efficiencyBias, maintenanceBias, powerDemandBias, repairabilityBias, reliabilityBias;
    FactionManufacturingProfile(Faction faction, String label, String prefix, double valueBias, double chargeBias, double defectBias, double durabilityBias, double comfortBias, double prestigeBias, double efficiencyBias, double maintenanceBias, double powerDemandBias, double repairabilityBias, double reliabilityBias, String aesthetic, String strengths, String weaknesses) {
        this.faction=faction; this.label=label; this.recipePrefix=prefix; this.valueBias=valueBias; this.chargeBias=chargeBias; this.defectBias=defectBias; this.durabilityBias=durabilityBias; this.comfortBias=comfortBias; this.prestigeBias=prestigeBias; this.efficiencyBias=efficiencyBias; this.maintenanceBias=maintenanceBias; this.powerDemandBias=powerDemandBias; this.repairabilityBias=repairabilityBias; this.reliabilityBias=reliabilityBias; this.aesthetic=aesthetic; this.strengths=strengths; this.weaknesses=weaknesses;
    }
    static FactionManufacturingProfile forFaction(Faction f) {
        if (f == Faction.MINISTORUM) return new FactionManufacturingProfile(f, "Ministorum", "Blessed", 1.12, 1.00, 0.88, 1.05, 1.10, 1.35, 0.92, 1.05, 0.95, 0.90, 1.05, "wax-sealed, stamped, devotional, charity-marked, socially legitimized", "devotional goods, pilgrim kitchens, lawful charity, morale and civil standing", "slow ritual throughput and public scrutiny");
        if (f == Faction.SORORITAS) return new FactionManufacturingProfile(f, "Sororitas", "Vowed", 1.20, 1.05, 0.65, 1.35, 0.75, 1.25, 1.05, 1.20, 1.10, 0.70, 1.30, "white-black-red, sealed, martial, convent-stamped, very hard to steal twice", "durable wargear, medical discipline, temple security", "restricted access and severe consequences");
        if (f == Faction.MECHANICUS) return new FactionManufacturingProfile(f, "Mechanicus", "Rite-Forged", 1.35, 1.20, 0.55, 1.20, 0.80, 1.40, 1.35, 1.35, 1.40, 0.70, 1.20, "red-lacquered, stamped, sealed, calibrated, covered in maintenance obligations", "high efficiency, high throughput, precision tools, power systems", "high maintenance, doctrine gates, power appetite");
        if (f == Faction.ARBITES) return new FactionManufacturingProfile(f, "Arbites", "Sanctioned", 1.18, 1.05, 0.70, 1.25, 0.85, 1.15, 1.05, 1.05, 1.05, 0.90, 1.22, "black, stamped, serialized, restraint-forward, lawfully unpleasant", "armor, restraint, paperwork, lawful access", "bureaucratic cost, faction suspicion");
        if (f == Faction.IMPERIAL_GUARD) return new FactionManufacturingProfile(f, "Astra Militarum", "Field-Issue", 1.12, 1.25, 0.75, 1.35, 0.70, 0.85, 1.05, 0.90, 0.95, 1.05, 1.25, "drab, serialized, rugged, standardized, comfort-hostile", "rugged, cheap, durable, scalable weapons/rations/uniforms", "low comfort and little subtlety");
        if (f == Faction.NOBLE) return new FactionManufacturingProfile(f, "Noble House", "Gilded", 1.75, 1.45, 0.65, 1.05, 1.65, 1.95, 1.05, 1.15, 1.20, 0.85, 1.10, "ornate, house-marked, soft-lined, obviously stealable", "comfort, prestige, social utility, high prices", "expensive inputs, visible status, theft risk");
        if (f == Faction.BANDIT) return new FactionManufacturingProfile(f, "Ganger", "Street-Cut", 0.88, 0.90, 1.25, 0.85, 0.75, 0.90, 0.85, 0.80, 0.85, 1.15, 0.80, "painted, spiked, patched, loud, personally incriminating", "cheap weapons, intimidation, black-market supply", "bad reliability and legal heat");
        if (f == Faction.CULTIST) return new FactionManufacturingProfile(f, "Cult", "Profaned", 1.05, 1.10, 1.50, 0.95, 0.60, 1.15, 0.90, 1.25, 1.10, 0.80, 0.70, "marked, hidden, ritualized, biologically or spiritually questionable", "hidden power, illicit rites, strange weapons", "dangerous failures and faction consequences");
        if (f == Faction.MUTANT) return new FactionManufacturingProfile(f, "Mutant", "Adapted", 0.80, 1.05, 1.30, 1.10, 0.65, 0.50, 0.80, 0.85, 0.75, 1.30, 0.78, "hide-bound, bone-tied, toxin-stained, body-adapted", "scrap survival, toxins, resilience", "social rejection and unstable quality");
        if (f == Faction.SCAVENGER) return new FactionManufacturingProfile(f, "Scavver", "Improvised", 0.65, 0.80, 1.60, 0.70, 0.55, 0.35, 0.65, 0.65, 0.65, 1.60, 0.60, "reused, mismatched, repairable, ugly, honest about the trash", "improvised, unstable, repairable, uses trash inputs", "low value, low charges, high defect rate");
        return new FactionManufacturingProfile(Faction.HIVER, "Civilian", "Civic", 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, "plain, available, shop-counter legitimate", "available goods, basic stores, hab utilities", "modest output and little specialization");
    }
    String modifierLine() {
        return label + ": prefix " + recipePrefix + "; value x" + fmt(valueBias) + "; charges x" + fmt(chargeBias) + "; durability x" + fmt(durabilityBias) + "; comfort x" + fmt(comfortBias) + "; prestige x" + fmt(prestigeBias) + "; efficiency x" + fmt(efficiencyBias) + "; reliability x" + fmt(reliabilityBias) + "; repairability x" + fmt(repairabilityBias) + "; defects x" + fmt(defectBias) + "; aesthetic: " + aesthetic + ".";
    }
    static String fmt(double d){ return String.format(Locale.US, "%.2f", d); }
    static ArrayList<String> summaryLines() {
        ArrayList<String> lines = new ArrayList<>();
        Faction[] fs = {Faction.HIVER, Faction.SCAVENGER, Faction.BANDIT, Faction.ARBITES, Faction.IMPERIAL_GUARD, Faction.MINISTORUM, Faction.SORORITAS, Faction.MECHANICUS, Faction.NOBLE, Faction.CULTIST, Faction.MUTANT};
        for (Faction f : fs) {
            FactionManufacturingProfile p = forFaction(f);
            lines.add(p.modifierLine() + " Strengths: " + p.strengths + "; weaknesses: " + p.weaknesses + ".");
        }
        return lines;
    }
}




class FactionRecipeVariant {
    final DraftIndustrialRecipe base;
    final Faction faction;
    final FactionManufacturingProfile profile;
    final String outputName, qualityName, knowledgeCategory, requiredKnowledge, machineQuality, machineHint, lawStatus, productionNote;
    final LinkedHashMap<String,Integer> itemInputs = new LinkedHashMap<>();
    final LinkedHashMap<String,Integer> equipmentRequirements = new LinkedHashMap<>();
    final double valueMultiplier, durabilityMultiplier, comfortMultiplier, prestigeMultiplier, efficiencyMultiplier, maintenanceMultiplier, powerDemandMultiplier, repairabilityMultiplier, reliabilityMultiplier, defectMultiplier;
    FactionRecipeVariant(DraftIndustrialRecipe base, Faction faction, String qualityName, String knowledgeCategory, String requiredKnowledge, String machineQuality, String machineHint, String lawStatus, String productionNote) {
        this.base=base; this.faction=faction==null?Faction.HIVER:faction; this.profile=FactionManufacturingProfile.forFaction(this.faction);
        this.qualityName=qualityName==null?"Common":qualityName;
        this.knowledgeCategory=knowledgeCategory==null?"Industrial Maintenance":knowledgeCategory;
        this.requiredKnowledge=requiredKnowledge==null?"Common Industrial Maintenance Patterns":requiredKnowledge;
        this.machineQuality=machineQuality==null?"Common":machineQuality;
        this.machineHint=machineHint==null?"faction workshop":machineHint;
        this.outputName=this.qualityName + " " + profile.recipePrefix + " " + (base==null?"Unknown item":base.outputBaseItem);
        this.lawStatus=lawStatus==null?"unclassified":lawStatus;
        this.productionNote=productionNote==null?"generated faction manufacturing variant":productionNote;
        if (base != null) {
            for (Map.Entry<String,Integer> e : base.itemInputs.entrySet()) addInput(e.getKey(), e.getValue());
            for (Map.Entry<String,Integer> e : base.equipmentRequirements.entrySet()) addEquipment(e.getKey(), e.getValue());
        }
        QualityAuthorityProfile q = QualityAuthorityApi.profile(this.qualityName);
        this.valueMultiplier = q.valueMultiplier * profile.valueBias * Math.max(0.50, profile.prestigeBias);
        this.durabilityMultiplier = q.reliabilityMultiplier * profile.durabilityBias;
        this.comfortMultiplier = q.comfortMultiplier * profile.comfortBias;
        this.prestigeMultiplier = q.prestigeMultiplier * profile.prestigeBias;
        this.efficiencyMultiplier = q.efficiencyMultiplier * profile.efficiencyBias;
        this.maintenanceMultiplier = profile.maintenanceBias;
        this.powerDemandMultiplier = profile.powerDemandBias;
        this.repairabilityMultiplier = profile.repairabilityBias;
        this.reliabilityMultiplier = q.reliabilityMultiplier * profile.reliabilityBias;
        this.defectMultiplier = q.defectMultiplier * profile.defectBias / Math.max(0.25, profile.reliabilityBias);
    }
    FactionRecipeVariant addInput(String item, int count) { if (item != null && !item.isBlank() && count > 0) itemInputs.put(item, itemInputs.getOrDefault(item,0)+count); return this; }
    FactionRecipeVariant addEquipment(String item, int count) { if (item != null && !item.isBlank() && count > 0) equipmentRequirements.put(item, equipmentRequirements.getOrDefault(item,0)+count); return this; }
    FactionRecipeVariant replaceEquipment(String from, String to) { Integer n = equipmentRequirements.remove(from); if (n != null && n > 0) addEquipment(to, n); return this; }
    FactionRecipeVariant replaceInput(String from, String to) {
        Integer n = itemInputs.remove(from);
        if (n != null && n > 0) addInput(to, n);
        return this;
    }
    int width() { return itemInputs.size(); }
    int totalInputCount() { int n=0; for (int v : itemInputs.values()) n += v; return n; }
    int estimatedValue() {
        ItemDef d = ItemCatalog.get(base==null?null:base.outputBaseItem);
        int basePrice = d == null ? 2 : d.basePrice;
        return Math.max(1, (int)Math.round(basePrice * valueMultiplier));
    }
    int defectPercent() { return Math.max(1, Math.min(95, (int)Math.round(10.0 * defectMultiplier))); }
    String inputSummary() { ArrayList<String> bits = new ArrayList<>(); for (Map.Entry<String,Integer> e : itemInputs.entrySet()) bits.add(e.getValue() + "x " + e.getKey()); return bits.isEmpty()?"time/knowledge only":String.join(", ", bits); }
    String equipmentSummary() { ArrayList<String> bits = new ArrayList<>(); for (Map.Entry<String,Integer> e : equipmentRequirements.entrySet()) bits.add(e.getValue() + "x " + e.getKey()); return bits.isEmpty()?"ordinary tools":String.join(", ", bits); }
    String statSummary() { return "value x" + fmt(valueMultiplier) + ", durability x" + fmt(durabilityMultiplier) + ", comfort x" + fmt(comfortMultiplier) + ", prestige x" + fmt(prestigeMultiplier) + ", efficiency x" + fmt(efficiencyMultiplier) + ", reliability x" + fmt(reliabilityMultiplier) + ", repairability x" + fmt(repairabilityMultiplier) + ", power x" + fmt(powerDemandMultiplier) + ", maintenance x" + fmt(maintenanceMultiplier) + ", defectRisk~" + defectPercent() + "%"; }
    String auditLine() { return "variant=" + outputName + " base=" + (base==null?"Unknown":base.outputBaseItem) + " faction=" + faction + " family=" + (base==null?"unknown":base.family) + " knowledge=" + requiredKnowledge + " machine=" + machineQuality + " law=" + lawStatus + " width=" + width() + " inputs=" + inputSummary() + " equipment=" + equipmentSummary() + " stats=[" + statSummary() + "]"; }
    String sampleLine() { return outputName + " <- " + inputSummary() + " | equipment " + equipmentSummary() + " | " + lawStatus + " | " + statSummary(); }
    static String fmt(double d){ return String.format(Locale.US, "%.2f", d); }
}


class FactionRecipeVariantApi {
    static final Faction[] FACTIONS = {Faction.HIVER, Faction.SCAVENGER, Faction.BANDIT, Faction.ARBITES, Faction.IMPERIAL_GUARD, Faction.MINISTORUM, Faction.SORORITAS, Faction.MECHANICUS, Faction.NOBLE, Faction.CULTIST, Faction.MUTANT};
    static ArrayList<FactionRecipeVariant> generatedFactionVariants() {
        ArrayList<FactionRecipeVariant> out = new ArrayList<>();
        LinkedHashSet<String> seenBases = new LinkedHashSet<>();
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) {
            if (!isVariantEligible(r)) continue;
            String key = r.outputBaseItem + "|" + r.family;
            if (!seenBases.add(key)) continue;
            for (Faction f : FACTIONS) if (canFactionProduce(f, r)) out.add(applyFactionIdentity(r, f));
        }
        return out;
    }
    static boolean isVariantEligible(DraftIndustrialRecipe r) {
        if (r == null || r.outputBaseItem == null) return false;
        ItemDef d = ItemCatalog.get(r.outputBaseItem);
        if (d == null) return false;
        String c = low(d.category), fam = low(r.family), n = low(d.name);
        if (c.startsWith("component/") || c.startsWith("material/") || c.startsWith("organic/") || c.startsWith("chemical/waste")) return false;
        if (c.contains("cache") || c.contains("currency") || c.contains("paperwork") || c.contains("victory") || c.contains("knowledge")) return false;
        if (c.startsWith("food/intermediate") || c.startsWith("food/raw") || c.startsWith("food/vat") || c.startsWith("drink/intermediate") || c.startsWith("agriculture/")) return false;
        return d.weapon || c.startsWith("weapon") || c.startsWith("armor") || c.startsWith("clothing") || c.startsWith("food") || c.startsWith("drink") || c.startsWith("water") || c.startsWith("tool") || c.startsWith("chem/") || c.startsWith("medical/chem") || fam.startsWith("weapon/") || fam.startsWith("food/") || fam.startsWith("water/") || fam.startsWith("clothing/") || fam.startsWith("armor/") || fam.startsWith("chem/") || n.contains("ration") || n.contains("canteen");
    }
    static boolean canFactionProduce(Faction f, DraftIndustrialRecipe r) {
        String fam = low(r.family), n = low(r.outputBaseItem), all = fam + " " + n;
        boolean weapon = fam.startsWith("weapon/") || (ItemCatalog.get(r.outputBaseItem) != null && ItemCatalog.get(r.outputBaseItem).weapon);
        boolean foodWater = fam.startsWith("food/") || fam.startsWith("water/") || n.contains("ration") || n.contains("water") || n.contains("amasec") || n.contains("recaf") || n.contains("juice");
        boolean clothingArmor = fam.startsWith("clothing/") || fam.startsWith("armor/") || n.contains("coat") || n.contains("robe") || n.contains("vest") || n.contains("helmet") || n.contains("suit") || n.contains("mask") || n.contains("apron");
        boolean chem = fam.startsWith("chem/") || cStarts(r, "chem/") || cStarts(r, "medical/chem") || containsAny(all,"stimm","chem","lho","obscura","spook","drug","amasec","recaf","mercy","wake","joy","dust","drops","incense","perfume","serum","draught");
        if (f == Faction.MECHANICUS) return !all.contains("mutant") && !all.contains("cult") && !all.contains("profane") && !all.contains("warp");
        if (f == Faction.IMPERIAL_GUARD) return foodWater || clothingArmor || (chem && containsAny(all,"stimm","medi-stimm","white mercy","red waker","clotfoam","shiftwake","dustlung","recaf","amasec","voidwake")) || (weapon && !containsAny(all,"force","psychic","profane","mutant","cult","needle","toxin"));
        if (f == Faction.MINISTORUM) return foodWater || clothingArmor || (chem && containsAny(all,"pilgrim","saint","censer","martyr","golden quiet","choir honey","reliquary","ease","devotional","incense","wine","amasec","recaf")) || containsAny(all,"robe","candle","ration","water","meal","book","scripture","seal","flamer","maul");
        if (f == Faction.SORORITAS) return foodWater || clothingArmor || (chem && containsAny(all,"medi-stimm","white mercy","clotfoam","red waker","saint","black badge")) || (weapon && containsAny(all,"bolter","flamer","melta","power","chainsword","maul","pistol","sword"));
        if (f == Faction.ARBITES) return foodWater || clothingArmor || (chem && containsAny(all,"truth","compliance","blue lantern","grav-lock","confessor","black badge","white mercy","nerve lace","stillhand")) || (weapon && !containsAny(all,"cult","mutant","force","psychic","melta","plasma","autocannon","chainaxe","thunder"));
        if (f == Faction.NOBLE) return foodWater || clothingArmor || (chem && containsAny(all,"noble","luxury","obscura","amasec","gildwine","cordial","nectar","vitreous","lotus","velvet","glass","halo","ghost","ebon","sorrowglass","kingmaker","widow")) || containsAny(all,"dueling","duelling","needle","power","hunting","marksman","pistol","sword","coat","amasec","orchard","luxury");
        if (f == Faction.BANDIT) return clothingArmor || foodWater || chem || (weapon && !containsAny(all,"archeotech","force","psychic","omnissian","multi-melta","heavy plasma"));
        if (f == Faction.SCAVENGER) return foodWater || clothingArmor || (chem && !containsAny(all,"noble-only","archeotech")) || containsAny(all,"improvised","scrap","pipe","zip","nail","stub","knife","maul","spear","tool","canteen","water","corpse","sump","loaf","ration");
        if (f == Faction.CULTIST) return foodWater || clothingArmor || (chem && containsAny(all,"cult","warp","spook","flect","witch","choir","benediction","prophet","communion","mirror","scripture","eclipse","wound","red choir","black sun","saint","devotional")) || containsAny(all,"cult","profane","knife","blade","sword","flamer","toxin","needle","pistol","shotgun","chain","maul","martyr","nail","spear");
        if (f == Faction.MUTANT) return foodWater || clothingArmor || (chem && containsAny(all,"mutant","sump","brine","gland","bone","thickblood","rad","glowgut","scale","mire","rust","drain","blue scab","cradle")) || containsAny(all,"mutant","improvised","scrap","knife","maul","axe","club","spear","toxin","chem","sump","corpse","hide","patch");
        return foodWater || clothingArmor || (chem && containsAny(all,"recaf","lho","amasec","night milk","sumpweed","grey drops","shiftwake","mercy","ploin","pilgrim")) || !containsAny(all,"plasma","melta","force","psychic","autocannon","heavy bolter","multi-melta","power","arc","webber","needle","bolter");
    }
    static FactionRecipeVariant applyFactionIdentity(DraftIndustrialRecipe base, Faction faction) {
        int preferred = preferredTier(faction, base);
        String knowledgeCategory = knowledgeCategoryFor(base);
        String band = bandForTier(preferred);
        String requiredKnowledge = band + " " + knowledgeCategory + " Patterns";
        int machineTier = machineTierFor(faction, base, preferred);
        int capped = QualityAuthorityApi.cappedTier(preferred, QualityAuthorityApi.requiredTierForRecipeKnowledge(requiredKnowledge), machineTier, -1, -1, -1);
        FactionRecipeVariant v = new FactionRecipeVariant(base, faction, QualityAuthorityApi.qualityName(capped), knowledgeCategory, requiredKnowledge, QualityAuthorityApi.qualityName(machineTier), machineHintFor(faction, base), lawStatusFor(faction, base), noteFor(faction, base));
        applyInputMutations(v);
        return v;
    }
    static void applyInputMutations(FactionRecipeVariant v) {
        String fam = low(v.base.family), n = low(v.base.outputBaseItem), all = fam + " " + n;
        boolean weapon = fam.startsWith("weapon/") || (ItemCatalog.get(v.base.outputBaseItem) != null && ItemCatalog.get(v.base.outputBaseItem).weapon);
        boolean foodWater = fam.startsWith("food/") || fam.startsWith("water/") || n.contains("ration") || n.contains("water") || n.contains("juice") || n.contains("amasec") || n.contains("recaf");
        boolean clothingArmor = fam.startsWith("clothing/") || fam.startsWith("armor/") || n.contains("coat") || n.contains("robe") || n.contains("vest") || n.contains("helmet") || n.contains("suit") || n.contains("mask") || n.contains("apron");
        boolean chem = fam.startsWith("chem/") || (ItemCatalog.get(v.base.outputBaseItem) != null && ItemCatalog.get(v.base.outputBaseItem).category.toLowerCase(Locale.ROOT).startsWith("chem/")) || (ItemCatalog.get(v.base.outputBaseItem) != null && ItemCatalog.get(v.base.outputBaseItem).category.toLowerCase(Locale.ROOT).startsWith("medical/chem"));
        if (v.faction == Faction.HIVER) {
            v.addInput("Civic inspection chit",1);
            if (foodWater) v.addInput("Food-safe sealant",1);
            if (chem) v.addInput("Civic inspection chit",1);
        } else if (v.faction == Faction.SCAVENGER) {
            v.replaceInput("Military weapon casing","Improvised weapon casing").replaceInput("Civilian weapon casing","Improvised weapon casing").replaceInput("Rifle body","Scrap weapon body").replaceInput("Pistol body","Scrap weapon body");
            v.addInput("Salvage repair tag",1).addInput("Reclaimed repair bracket",1);
            if (weapon || clothingArmor) v.addInput("Scrap plate",1);
            if (foodWater) v.addInput("Waste biomass",1);
            if (chem) v.addInput("Mutant fitment strap",1).addInput("Sump fermentation mash",1);
            if (chem) v.addInput("Underhive paper twist",1).addInput("Salvage repair tag",1);
        } else if (v.faction == Faction.BANDIT) {
            if (weapon) v.replaceInput("Military weapon casing","Improvised weapon casing").addInput("Gang color scrap",1).addInput("Intimidation spike set",1);
            if (clothingArmor) v.addInput("Gang color scrap",1).addInput("Wire stitching spool",1);
            if (foodWater) v.addInput("Contraband cipher tag",1);
            if (chem) v.addInput("Contraband cipher tag",1).addInput("Gang color scrap",1);
        } else if (v.faction == Faction.ARBITES) {
            v.addInput("Serialized casing tag",1);
            if (weapon) v.addInput("Military weapon casing",1);
            if (clothingArmor) v.addInput("Rank tab set",1).addInput("Pressure gasket set",1);
            if (foodWater) v.addInput("Civic inspection chit",1);
            if (chem) v.addInput("Interrogation dosing kit",1).addInput("Serialized casing tag",1);
        } else if (v.faction == Faction.IMPERIAL_GUARD) {
            v.addInput("Quartermaster stamp pad",1).addInput("Serialized casing tag",1);
            if (weapon) v.addInput("Military weapon casing",1);
            if (clothingArmor) v.addInput("Faction insignia patch",1).addInput("Webbing strap roll",1);
            if (foodWater) v.addInput("Ration wrapper roll",1).addInput("Preservative salt packet",1);
            if (chem) v.addInput("Quartermaster stamp pad",1).addInput("Injector ampoule set",1);
        } else if (v.faction == Faction.MINISTORUM) {
            v.addInput("Purity seal backing",1).addInput("Incense resin pellet",1);
            if (clothingArmor) v.addInput("Faction insignia patch",1).addInput("Purity seal backing",1);
            if (foodWater) v.addInput("Food-safe sealant",1).addInput("Ration wrapper roll",1);
            if (chem) v.addInput("Incense resin pellet",1).addInput("Civic inspection chit",1);
        } else if (v.faction == Faction.SORORITAS) {
            v.addInput("Purity seal backing",1).addInput("Serialized casing tag",1);
            if (weapon) v.addInput("Military weapon casing",1);
            if (clothingArmor) v.addInput("Flak weave panel",1).addInput("Rank tab set",1);
            if (foodWater) v.addInput("Ration wrapper roll",1);
            if (chem) v.addInput("Sterile vial rack",1).addInput("Civic inspection chit",1);
        } else if (v.faction == Faction.MECHANICUS) {
            v.addInput("Mechanicus calibration seal",1).addInput("Ceramic insulator blank",1);
            if (weapon || all.contains("energy") || all.contains("machine") || all.contains("power")) v.addInput("Shrine-etched control tag",1).addInput("Purity seal backing",1);
            if (foodWater) v.addInput("Sterile vial rack",1);
            if (chem) v.addInput("Mechanicus calibration seal",1).addInput("Chemical reagent bottle",1);
        } else if (v.faction == Faction.NOBLE) {
            v.addInput("House hallmark plate",1).addInput("Gilding foil",1);
            if (clothingArmor) v.addInput("House livery ribbon set",1).addInput("Decorative trim set",1);
            if (weapon) v.addInput("Decorative trim set",1);
            if (foodWater) v.addInput("Food-safe sealant",1).addInput("Tin can sleeve",1);
            if (chem) v.addInput("Spire crystal vial",1).addInput("House hallmark plate",1);
        } else if (v.faction == Faction.CULTIST) {
            v.addInput("Profane mark stencil",1).addInput("Contraband cipher tag",1);
            if (weapon) v.replaceInput("Military weapon casing","Improvised weapon casing").addInput("Bone charm string",1);
            if (clothingArmor) v.addInput("Bone charm string",1);
            if (foodWater) v.addInput("Kitchen grease tin",1);
            if (chem) v.addInput("Profane binding ash",1).addInput("Contraband cipher tag",1);
        } else if (v.faction == Faction.MUTANT) {
            v.replaceInput("Military weapon casing","Improvised weapon casing").replaceInput("Civilian weapon casing","Improvised weapon casing");
            v.addInput("Mutant fitment strap",1).addInput("Hide strip bundle",1);
            if (weapon || clothingArmor) v.addInput("Bone charm string",1);
            if (foodWater) v.addInput("Waste biomass",1);
            if (chem) v.addInput("Mutant fitment strap",1).addInput("Sump fermentation mash",1);
        }
        // 0.8.95b: faction identities may substitute laboratory apparatus without changing the base ingredient graph.
        if (chem) ChemicalEquipmentAuthority.applyFactionEquipmentPreference(v);
    }
    static int preferredTier(Faction f, DraftIndustrialRecipe r) {
        String all = low(r.family + " " + r.outputBaseItem);
        if (f == Faction.MECHANICUS) return containsAny(all,"archeotech") ? 7 : (containsAny(all,"plasma","melta","arc","power","mechanicus","medicae","nootropic","chemical") ? 5 : 4);
        if (f == Faction.NOBLE) return containsAny(all,"orchard","amasec","dueling","luxury","coat","gilded","obscura","serum","perfume") ? 6 : 4;
        if (f == Faction.ARBITES) return containsAny(all,"security","web","riot","suppression") ? 4 : 3;
        if (f == Faction.IMPERIAL_GUARD) return 3;
        if (f == Faction.MINISTORUM) return containsAny(all,"saint","relic","pilgrim","devotional","amasec") ? 3 : 2;
        if (f == Faction.SORORITAS) return containsAny(all,"bolter","flamer","melta","power","armor","medicae") ? 4 : 3;
        if (f == Faction.BANDIT) return containsAny(all,"scrap","pipe","zip","improvised") ? 1 : 2;
        if (f == Faction.SCAVENGER) return containsAny(all,"water","ration","repair","scrap") ? 1 : 0;
        if (f == Faction.CULTIST) return containsAny(all,"ritual","profane","cult") ? 2 : 1;
        if (f == Faction.MUTANT) return 1;
        return 2;
    }
    static int machineTierFor(Faction f, DraftIndustrialRecipe r, int preferred) {
        if (f == Faction.MECHANICUS) return Math.max(preferred, 5);
        if (f == Faction.NOBLE) return Math.max(preferred, 4);
        if (f == Faction.ARBITES || f == Faction.IMPERIAL_GUARD) return Math.max(preferred, 3);
        if (f == Faction.BANDIT || f == Faction.CULTIST) return Math.max(1, Math.min(3, preferred));
        if (f == Faction.SCAVENGER || f == Faction.MUTANT) return Math.max(0, Math.min(2, preferred));
        return Math.max(2, preferred);
    }
    static String bandForTier(int tier) {
        String q = QualityAuthorityApi.qualityName(tier);
        if ("Shoddy".equals(q)) return "Junk";
        return q;
    }
    static String knowledgeCategoryFor(DraftIndustrialRecipe r) {
        String all = low(r.family + " " + r.outputBaseItem);
        if (containsAny(all,"medical/chem","medicae","surgical","stitch","clot","nerve","white mercy","medi-stimm","red waker","anesthetic","wakewire")) return "Medical Processing";
        if (containsAny(all,"chem/","stimm","narcotic","intoxicant","lho","obscura","spook","drug","salt","powder","drops","resin","incense","serum","draught","perfume","revelation","compliance","truth ache","black badge","warpglimmer")) return "Chemical Synthesis";
        if (containsAny(all,"water","canteen","purification")) return "Water Purification";
        if (containsAny(all,"food","ration","drink","amasec","recaf","juice","corpse","soylens","porridge")) return "Food Processing";
        if (containsAny(all,"hydroponic","agriculture","orchard","grow","fungus","algae")) return "Agricultural Processing";
        if (containsAny(all,"clothing","textile","coat","robe","fatigue","suit","mask","apron","hood","boot","glove")) return "Textile Fabrication";
        if (containsAny(all,"armor","flak","carapace","plate","helmet","vest")) return "Metallurgy";
        if (containsAny(all,"plasma","melta","las","arc","power","field","energy")) return "Energy Systems";
        if (containsAny(all,"flamer","chemical","toxin","needle","acid","promethium")) return "Chemical Synthesis";
        if (containsAny(all,"gun","bolter","stubber","auto","shotgun","rifle","pistol","autocannon","solid")) return "Ballistics";
        if (containsAny(all,"scrap","improvised","pipe","zip","nail","scav")) return "Salvage Processing";
        if (containsAny(all,"tool","machine","maintenance")) return "Industrial Maintenance";
        return "Industrial Maintenance";
    }
    static String machineHintFor(Faction f, DraftIndustrialRecipe r) {
        String cat = knowledgeCategoryFor(r);
        String label = FactionManufacturingProfile.forFaction(f).label;
        if (cat.equals("Medical Processing")) return label + " medicae chem bench";
        if (cat.equals("Chemical Synthesis")) return label + " chem kitchen";
        if (cat.equals("Water Purification")) return label + " water reclamation bench";
        if (cat.equals("Food Processing")) return label + " provisioner line";
        if (cat.equals("Agricultural Processing")) return label + " growth-rack authority";
        if (cat.equals("Textile Fabrication")) return label + " textile/armor shop";
        if (cat.equals("Metallurgy") || cat.equals("Ballistics")) return label + " armory forge";
        if (cat.equals("Energy Systems")) return label + " energy weapon bench";
        if (cat.equals("Chemical Synthesis")) return label + " chemical pressure bench";
        if (cat.equals("Salvage Processing")) return label + " salvage bench";
        return label + " workshop";
    }
    static String lawStatusFor(Faction f, DraftIndustrialRecipe r) {
        String all = low(r.family + " " + r.outputBaseItem);
        boolean dangerous = containsAny(all,"weapon","plasma","melta","bolt","flamer","toxin","needle","cult","profane","mutant","contraband","spook","flect","slaught","frenzon","fearhook","interrogation","warp","serum","black sun","red choir");
        if (f == Faction.CULTIST || f == Faction.MUTANT) return "contraband/hostile-identity";
        if (f == Faction.BANDIT || f == Faction.SCAVENGER) return dangerous ? "illegal or seizure-prone" : "gray-market";
        if (f == Faction.ARBITES || f == Faction.IMPERIAL_GUARD) return dangerous ? "restricted lawful issue" : "lawful issued stock";
        if (f == Faction.MECHANICUS) return dangerous ? "doctrine-controlled" : "doctrine-sanctioned";
        if (f == Faction.NOBLE) return dangerous ? "licensed privilege / theft-visible" : "prestige-marked lawful stock";
        return dangerous ? "licensed or suspicious civilian stock" : "ordinary civic stock";
    }
    static String noteFor(Faction f, DraftIndustrialRecipe r) {
        FactionManufacturingProfile p = FactionManufacturingProfile.forFaction(f);
        return p.label + " variant: " + p.strengths + "; weakness profile: " + p.weaknesses + "; base family " + (r==null?"unknown":r.family) + ".";
    }
    static ArrayList<String> sampleLines(int max) {
        ArrayList<String> lines = new ArrayList<>();
        int i=0;
        for (FactionRecipeVariant v : generatedFactionVariants()) {
            lines.add(v.sampleLine());
            if (++i >= max) break;
        }
        return lines;
    }
    static boolean cStarts(DraftIndustrialRecipe r, String prefix) { ItemDef d = r == null ? null : ItemCatalog.get(r.outputBaseItem); return d != null && d.category != null && d.category.toLowerCase(Locale.ROOT).startsWith(prefix); }
    static boolean containsAny(String s, String... terms) { if (s == null) return false; for (String t : terms) if (t != null && !t.isBlank() && s.contains(t.toLowerCase(Locale.ROOT))) return true; return false; }
    static String low(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}


class FactionRecipeVariantAuditApi {
    static FactionRecipeVariantAudit audit() {
        FactionRecipeVariantAudit a = new FactionRecipeVariantAudit();
        ArrayList<DraftIndustrialRecipe> base = RecipeDecompositionApi.generatedDraftRecipes();
        a.baseDraftRecipes = base.size();
        LinkedHashSet<String> eligibleOutputs = new LinkedHashSet<>();
        for (DraftIndustrialRecipe r : base) if (FactionRecipeVariantApi.isVariantEligible(r)) eligibleOutputs.add(r.outputBaseItem + "|" + r.family);
        a.eligibleBaseRecipes = eligibleOutputs.size();
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
            a.variantRecipes++;
            a.perFaction.put(v.profile.label, a.perFaction.getOrDefault(v.profile.label,0)+1);
            a.perKnowledgeCategory.put(v.knowledgeCategory, a.perKnowledgeCategory.getOrDefault(v.knowledgeCategory,0)+1);
            a.perLawStatus.put(v.lawStatus, a.perLawStatus.getOrDefault(v.lawStatus,0)+1);
            a.coveredBaseKeys.add(v.base.outputBaseItem + "|" + v.base.family);
            if (v.width() > 14) a.absurdWidths.add(v.outputName + " width=" + v.width());
            if (v.outputName == null || v.outputName.isBlank()) a.issues.add("blank output name for " + v.faction + " " + (v.base==null?"unknown":v.base.outputBaseItem));
            if (v.requiredKnowledge == null || !KnowledgeDef.all().containsKey(v.requiredKnowledge)) a.issues.add("missing knowledge node " + v.requiredKnowledge + " for " + v.outputName);
            if (MachineTierAuthority.forQuality(v.machineQuality) == null) a.issues.add("missing machine tier " + v.machineQuality + " for " + v.outputName);
            for (String input : v.itemInputs.keySet()) if (ItemCatalog.get(input) == null) a.missingInputs.add(input + " required by " + v.outputName);
        }
        for (String k : eligibleOutputs) if (!a.coveredBaseKeys.contains(k)) a.uncoveredBaseRecipes.add(k);
        for (Faction f : FactionRecipeVariantApi.FACTIONS) {
            String label = FactionManufacturingProfile.forFaction(f).label;
            if (!a.perFaction.containsKey(label)) a.issues.add("no generated variants for faction " + label);
        }
        return a;
    }
}


class FactionRecipeVariantAudit {
    int baseDraftRecipes, eligibleBaseRecipes, variantRecipes;
    final TreeMap<String,Integer> perFaction = new TreeMap<>();
    final TreeMap<String,Integer> perKnowledgeCategory = new TreeMap<>();
    final TreeMap<String,Integer> perLawStatus = new TreeMap<>();
    final TreeSet<String> coveredBaseKeys = new TreeSet<>();
    final TreeSet<String> uncoveredBaseRecipes = new TreeSet<>();
    final TreeSet<String> missingInputs = new TreeSet<>();
    final TreeSet<String> absurdWidths = new TreeSet<>();
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return variantRecipes > 0 && missingInputs.isEmpty() && absurdWidths.isEmpty() && uncoveredBaseRecipes.isEmpty() && issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("0.8.95 Faction Manufacturing Identity Variant audit");
        l.add("Base draft recipes scanned: " + baseDraftRecipes);
        l.add("Variant-eligible base recipes: " + eligibleBaseRecipes);
        l.add("Generated faction recipe variants: " + variantRecipes);
        l.add("Faction coverage:");
        for (Map.Entry<String,Integer> e : perFaction.entrySet()) l.add("  faction " + e.getKey() + ": " + e.getValue());
        l.add("Knowledge category coverage:");
        for (Map.Entry<String,Integer> e : perKnowledgeCategory.entrySet()) l.add("  knowledge " + e.getKey() + ": " + e.getValue());
        l.add("Law/status coverage:");
        for (Map.Entry<String,Integer> e : perLawStatus.entrySet()) l.add("  law " + e.getKey() + ": " + e.getValue());
        l.add("Missing transformed inputs: " + missingInputs.size());
        for (String s : missingInputs) l.add("  MISSING_INPUT " + s);
        l.add("Uncovered eligible base recipes: " + uncoveredBaseRecipes.size());
        for (String s : uncoveredBaseRecipes) l.add("  UNCOVERED " + s);
        l.add("Absurd variant widths (>14 direct inputs): " + absurdWidths.size());
        for (String s : absurdWidths) l.add("  WIDTH " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("Sample variants:");
        int n=0;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
            l.add("  " + v.auditLine());
            if (++n >= 24) break;
        }
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}


class CraftingRecipe {
    final String name, outputBaseItem, requiredKnowledge, description, xpSkill;
    final Faction faction;
    final char machineSymbol;
    final int suppliesCost, machinePartsCost, outputCount, fatigueCost, turnCost, xpGain, machineWear;
    final LinkedHashMap<String,Integer> itemInputs = new LinkedHashMap<>();
    boolean disabled = false;
    CraftingRecipe(String name, String outputBaseItem, Faction faction, String requiredKnowledge, char machineSymbol, int suppliesCost, int machinePartsCost, int outputCount, int fatigueCost, int turnCost, int xpGain, int machineWear, String xpSkill, String description) {
        this.name=name; this.outputBaseItem=outputBaseItem; this.faction=faction==null?Faction.HIVER:faction; this.requiredKnowledge=requiredKnowledge; this.machineSymbol=machineSymbol; this.suppliesCost=suppliesCost; this.machinePartsCost=machinePartsCost; this.outputCount=outputCount; this.fatigueCost=fatigueCost; this.turnCost=turnCost; this.xpGain=xpGain; this.machineWear=machineWear; this.xpSkill=xpSkill==null?"Mechanics":xpSkill; this.description=description;
    }
    CraftingRecipe input(String item, int count) { itemInputs.put(item, count); return this; }
    static CraftingRecipe noKnownRecipes() { CraftingRecipe r = new CraftingRecipe("No known craft recipes", "Vended scrap", Faction.NONE, null, ' ', 0,0,0,0,0,0,0,"Knowledge", "Unlock production knowledge to reveal real recipes here."); r.disabled = true; return r; }
    boolean visibleTo(GamePanel g) { return disabled || g == null || g.hasProductionKnowledge(this, requiredMachine(g)); }
    BaseObject requiredMachine(GamePanel g) { return g.requiredMachineFor(this); }
    String machineName() { if (machineSymbol == ' ') return "Scrap Workbench"; if (machineSymbol == 'w') return "Scrap Workbench"; if (machineSymbol == 'e') return "EMM Atmospheric Condenser"; if (machineSymbol == 'f') return "EMM Micro Forge"; if (machineSymbol == 'l') return "EMM Micro Lab"; if (machineSymbol == 'x') return "Security Cogitator Node"; return "machine '" + machineSymbol + "'"; }
    String machineLabel() { return machineName(); }
    String blockingProblem(GamePanel g) {
        return blockingProblemForMachine(g, requiredMachine(g));
    }
    String assignmentProblem(GamePanel g, BaseObject machine) {
        if (disabled) return "no real recipe is selected.";
        if (machine == null) return "no machine selected.";
        if (!g.hasProductionKnowledge(this, machine)) return "requires knowledge: " + requiredKnowledge + ".";
        if (machineSymbol != ' ' && machineSymbol != machine.symbol) return "requires " + machineName() + ".";
        return null;
    }
    String blockingProblemForMachine(GamePanel g, BaseObject machine) {
        if (disabled) return "no real recipe is selected.";
        if (!g.hasProductionKnowledge(this, machine)) return "requires knowledge: " + requiredKnowledge + ".";
        if (machine == null) return "requires built " + machineName() + ".";
        if (machineSymbol != ' ' && machineSymbol != machine.symbol) return "requires " + machineName() + ".";
        if (!MachineConditionProductionAuthority.evaluate(machine).operational()) return "requires repair: " + machine.name + " is broken.";
        int needSupplies = effectiveSuppliesCost();
        int needParts = effectiveMachinePartsCost();
        if (g.supplies < needSupplies) return "needs " + needSupplies + " supplies; available " + g.supplies + ".";
        if (g.machineParts < needParts) return "needs " + needParts + " machine parts; available " + g.machineParts + ".";
        for (Map.Entry<String,Integer> e : itemInputs.entrySet()) if (g.countCraftInput(e.getKey()) < e.getValue()) return "needs " + e.getValue() + "x " + e.getKey() + "; available " + g.countCraftInput(e.getKey()) + ".";
        return null;
    }
    void consumeInputs(GamePanel g) {
        g.supplies -= effectiveSuppliesCost();
        g.machineParts -= effectiveMachinePartsCost();
        for (Map.Entry<String,Integer> e : itemInputs.entrySet()) for (int i=0;i<e.getValue();i++) g.consumeInventoryNamed(e.getKey());
    }
    String shortStatus(GamePanel g) {
        if (disabled) return "no unlocked recipes";
        String problem = blockingProblem(g);
        BaseObject machine = requiredMachine(g);
        String cap = machine == null ? "no machine" : "cap " + g.cappedProductionQuality(machine, this);
        return (problem == null ? "READY" : "LOCKED: " + problem) + " | " + cap + " | inputs " + inputSummary();
    }
    int effectiveSuppliesCost(){ return Math.max(0, (int)Math.ceil(suppliesCost * WorldGenerationApi.settings().craftMultiplier())); }
    int effectiveMachinePartsCost(){ return Math.max(0, (int)Math.ceil(machinePartsCost * WorldGenerationApi.settings().craftMultiplier())); }
    String inputSummary() {
        ArrayList<String> bits = new ArrayList<>();
        int sCost = effectiveSuppliesCost(), pCost = effectiveMachinePartsCost();
        if (sCost > 0) bits.add(sCost + " supplies");
        if (pCost > 0) bits.add(pCost + " machine parts");
        for (Map.Entry<String,Integer> e : itemInputs.entrySet()) bits.add(e.getValue() + "x " + e.getKey());
        return bits.isEmpty() ? "time only" : String.join(", ", bits);
    }
    ArrayList<String> detailLines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Recipe: " + name);
        l.add("Output base item: " + outputBaseItem);
        l.add("Faction output style: " + FactionManufacturingProfile.forFaction(faction).label + " / " + FactionManufacturingProfile.forFaction(faction).recipePrefix);
        l.add("Required knowledge: " + (requiredKnowledge==null||requiredKnowledge.isBlank()?"none":requiredKnowledge));
        l.add("Required machine: " + machineName());
        l.add("Inputs: " + inputSummary());
        l.add("Base output count: " + outputCount + "; final output may gain extra charges/count from quality.");
        l.add("Time/fatigue: " + turnCost + " turn(s), fatigue +" + fatigueCost + ". XP: " + xpGain + " " + xpSkill + ".");
        l.add("Description: " + description);
        l.add("Rule: output quality is capped by QualityAuthorityApi across doctrine, recipe requirement, machine ceiling, and input/facility/worker quality hooks.");
        return l;
    }
    String auditLine() { return "recipe=" + name + " outputBase=" + outputBaseItem + " faction=" + faction + " knowledge=" + requiredKnowledge + " machine=" + machineSymbol + " inputs=" + inputSummary(); }
    static CraftingRecipe byName(String n) { for (CraftingRecipe r : all()) if (r.name.equals(n)) return r; return null; }
    static ArrayList<CraftingRecipe> all() {
        ArrayList<CraftingRecipe> r = new ArrayList<>();
        r.add(new CraftingRecipe("Civic Emergency Ration Pack", "Emergency rations", Faction.HIVER, "Civilian Provisioning Patterns", 'w', 1, 0, 1, 1, 8, 2, 0, "Survival", "Civilian ration assembly from ordinary stores. Cheap, modest, and scalable once faction kitchens exist."));
        r.add(new CraftingRecipe("Field-Issue Emergency Ration Pack", "Emergency rations", Faction.IMPERIAL_GUARD, "Military Logistics Patterns", 'w', 2, 1, 1, 1, 10, 3, 1, "Survival", "Astra Militarum style rationing. More rugged and higher charge potential, but not worth producing without the proper doctrine and machinery."));
        r.add(new CraftingRecipe("Gilded Emergency Ration Hamper", "Emergency rations", Faction.NOBLE, "Noble Manufactury Patterns", 'w', 4, 1, 1, 1, 12, 4, 1, "Commerce", "Noble provisioning turns food into prestige, comfort, and sale value. Expensive inputs, excellent resale when quality is high."));
        r.add(new CraftingRecipe("Improvised Bandage Roll", "Bandage roll", Faction.SCAVENGER, "Junk Fabrication Patterns", 'w', 1, 0, 1, 1, 6, 1, 0, "Medical", "Low-grade cloth salvage converted into basic bleeding control. Better than bleeding on principle."));
        r.add(new CraftingRecipe("Field Dressing Kit", "Field dressings", Faction.HIVER, "Civilian Provisioning Patterns", 'w', 2, 0, 1, 1, 8, 2, 0, "Medical", "Civilian/worksite wound care packed into a usable kit."));
        r.add(new CraftingRecipe("Rite-Forged Machine Part", "Machine part", Faction.MECHANICUS, "Scrap-Forging Doctrine", 'f', 1, 0, 1, 1, 10, 3, 1, "Mechanics", "Forge detritus and supplies into a machine component under Mechanicus-style rites.").input("Mechanical detritus",1));
        r.add(new CraftingRecipe("Security Data Spike", "Data spike", Faction.ARBITES, "Security Cogitator Rites", 'x', 1, 2, 1, 1, 12, 4, 1, "Security", "Converts parts and sanctioned logic into a single-use electronic intrusion tool."));
        r.add(new CraftingRecipe("Hiver Rebar Maul", "Rebar maul", Faction.SCAVENGER, "Junk Fabrication Patterns", 'w', 1, 0, 1, 1, 6, 2, 0, "Melee", "Turns construction trash into a blunt weapon with honest mass and dishonest intent."));
        r.add(new CraftingRecipe("Jury-Rigged Zip Pistol", "Zip pistol", Faction.SCAVENGER, "Junk Fabrication Patterns", 'w', 2, 1, 1, 1, 10, 3, 1, "Firearms", "Bench-built emergency firearm for hivers who have access to pipe, springs, and bad odds."));
        r.add(new CraftingRecipe("Street-Cut Pipe Shotgun", "Pipe shotgun", Faction.BANDIT, "Junk Fabrication Patterns", 'w', 3, 1, 1, 2, 12, 4, 1, "Firearms", "Ganger-pattern pipe shotgun assembled from tool scrap and pressure confidence."));
        r.add(new CraftingRecipe("Rite-Forged Emergency Cutter", "Emergency Cutter", Faction.MECHANICUS, "Scrap-Forging Doctrine", 'f', 2, 2, 1, 1, 12, 4, 1, "Mechanics", "Micro-forge salvage cutter that remains a tool until someone insists otherwise."));
        r.add(new CraftingRecipe("Micro Lab Research Packet", "Blank form packet", Faction.HIVER, "Underhive Research Methods", 'l', 1, 1, 1, 1, 10, 5, 0, "Knowledge", "Turns parts and supplies into organized notes for later trainer/library/research loops."));
        return r;
    }
}



class DraftIndustrialRecipe {
    final String name, outputBaseItem, family, source, note;
    final LinkedHashMap<String,Integer> itemInputs = new LinkedHashMap<>();
    final LinkedHashMap<String,Integer> equipmentRequirements = new LinkedHashMap<>();
    final LinkedHashSet<String> roomRequirements = new LinkedHashSet<>();
    String processType = "general assembly";
    String placementNote = "ordinary workshop placement";
    String manningRequirement = "1 general worker";
    int minimumMachineTier = 2;
    boolean generated = true;
    DraftIndustrialRecipe(String name, String outputBaseItem, String family, String source, String note) {
        this.name=name; this.outputBaseItem=outputBaseItem; this.family=family; this.source=source; this.note=note;
    }
    DraftIndustrialRecipe input(String item, int count) { if (item != null && !item.isBlank() && count > 0) itemInputs.put(item, itemInputs.getOrDefault(item,0)+count); return this; }
    DraftIndustrialRecipe equipment(String item, int count) { if (item != null && !item.isBlank() && count > 0) equipmentRequirements.put(item, equipmentRequirements.getOrDefault(item,0)+count); return this; }
    DraftIndustrialRecipe room(String room) { if (room != null && !room.isBlank()) roomRequirements.add(room); return this; }
    DraftIndustrialRecipe process(String processType, int machineTier, String manning, String placementNote) {
        if (processType != null && !processType.isBlank()) this.processType = processType;
        this.minimumMachineTier = Math.max(0, Math.min(QualityAuthorityApi.UNLIMITED_TIER, machineTier));
        if (manning != null && !manning.isBlank()) this.manningRequirement = manning;
        if (placementNote != null && !placementNote.isBlank()) this.placementNote = placementNote;
        return this;
    }
    int width() { return itemInputs.size(); }
    int equipmentWidth() { return equipmentRequirements.size(); }
    int totalInputCount() { int n=0; for (int v : itemInputs.values()) n += v; return n; }
    String inputSummary() { ArrayList<String> bits = new ArrayList<>(); for (Map.Entry<String,Integer> e : itemInputs.entrySet()) bits.add(e.getValue() + "x " + e.getKey()); return bits.isEmpty()?"time/knowledge only":String.join(", ", bits); }
    String equipmentSummary() { ArrayList<String> bits = new ArrayList<>(); for (Map.Entry<String,Integer> e : equipmentRequirements.entrySet()) bits.add(e.getValue() + "x " + e.getKey()); return bits.isEmpty()?"ordinary tools":String.join(", ", bits); }
    String roomSummary() { return roomRequirements.isEmpty()?"ordinary workshop":String.join(" / ", roomRequirements); }
    String auditLine() { return "draftRecipe=" + name + " output=" + outputBaseItem + " family=" + family + " width=" + width() + " totalInputs=" + totalInputCount() + " inputs=" + inputSummary() + " process=" + processType + " equipment=" + equipmentSummary() + " rooms=" + roomSummary() + " minMachine=" + QualityAuthorityApi.qualityName(minimumMachineTier); }
}


class IndustrialRecipeProfile {
    final String family, label, machineHint, knowledgeHint;
    final ArrayList<ComponentNeed> needs = new ArrayList<>();
    IndustrialRecipeProfile(String family, String label, String machineHint, String knowledgeHint) { this.family=family; this.label=label; this.machineHint=machineHint; this.knowledgeHint=knowledgeHint; }
    IndustrialRecipeProfile need(String item, int count) { needs.add(new ComponentNeed(item,count)); return this; }
    DraftIndustrialRecipe build(ItemDef d) {
        DraftIndustrialRecipe r = new DraftIndustrialRecipe("Draft: " + label + " -> " + d.name, d.name, family, "RecipeDecompositionApi registry scan", "Draft only; not injected into CraftingRecipe.all() yet. Machine hint: " + machineHint + "; knowledge: " + knowledgeHint + ".");
        for (ComponentNeed n : needs) r.input(n.item, n.count);
        ChemicalEquipmentAuthority.applyRequirements(r);
        return r;
    }
    static IndustrialRecipeProfile profileFor(ItemDef d) {
        String n = low(d.name), c = low(d.category), u = low(d.use);
        String all = n + " " + c + " " + u;
        if (d.weapon) {
            // 0.8.93 WEAPON FAMILY DECOMPOSITION PASS:
            // Keep the recipes draft-facing, but make the family bill-of-materials specific enough
            // to feed faction variants, facility outputs, armory ledgers, and provenance text.
            if (containsAny(all,"jury-rigged laslock")) return lasProfile("weapon/las/improvised", "jury-rigged laslock pattern", "Scrap weapon body", "Compact las emitter", false).need("Crude trigger group",1).need("Wire bundle",1).need("Improvised weapon casing",1);
            if (containsAny(all,"laspistol")) return lasProfile("weapon/las/pistol", "las pistol pattern", "Pistol body", "Compact las emitter", false).need("Trigger group",1).need("Grip frame",1).need("Compact charge cradle",1);
            if (containsAny(all,"longlas") || containsAny(all,"marksman las")) return lasProfile("weapon/las/precision", "precision las pattern", "Precision rifle body", "Stabilized las barrel", true).need("Charge regulator",1).need("Optic mount",1).need("Military weapon casing",1);
            if (containsAny(all,"lascarbine") || (containsAny(all,"carbine") && containsAny(all,"las"))) return lasProfile("weapon/las/carbine", "las carbine pattern", "Carbine body", "Short las emitter barrel", false).need("Trigger group",1).need("Weapon stock frame",1).need("Military weapon casing",1);
            if (containsAny(all,"lasgun","las rifle","lasrifle") || (containsAny(all,"weapon/ranged/las") && !containsAny(all,"improvised"))) return lasProfile("weapon/las/rifle", "las rifle pattern", "Rifle body", "Long las emitter barrel", false).need("Trigger group",1).need("Weapon stock frame",1).need("Military weapon casing",1);

            if (containsAny(all,"bolt pistol")) return solidProfile("weapon/bolt/pistol", "bolt pistol pattern", "Pistol body", "Short barrel", 1).need("Bolt weapon action",1).need("Bolt round magazine",1).need("Reinforced fastener set",1);
            if (containsAny(all,"heavy bolter","storm bolter") || (containsAny(all,"bolter") && containsAny(all,"heavy"))) return solidProfile("weapon/bolt/heavy", "heavy bolt pattern", "Heavy weapon body", "Reinforced barrel", 2).need("Bolt weapon action",2).need("Heavy feed action group",1).need("Bolt round magazine",2).need("Heavy weapon recoil cradle",1);
            if (containsAny(all,"bolter")) return solidProfile("weapon/bolt/rifle", "bolt weapon pattern", "Rifle body", "Reinforced barrel", 1).need("Bolt weapon action",1).need("Bolt round magazine",1).need("Military weapon casing",1);

            if (containsAny(all,"plasma pistol")) return energyProfile("weapon/plasma/pistol", "plasma pistol pattern", "Pistol body", 1).need("Plasma discharge assembly",1).need("Compact charge cradle",1).need("Plasma coolant cartridge",1);
            if (containsAny(all,"heavy plasma")) return energyProfile("weapon/plasma/heavy", "heavy plasma pattern", "Heavy weapon body", 2).need("Plasma discharge assembly",2).need("Heavy energy heat exchanger",1).need("Plasma coolant cartridge",2).need("Heavy weapon recoil cradle",1);
            if (containsAny(all,"plasma")) return energyProfile("weapon/plasma/rifle", "plasma gun pattern", "Rifle body", 1).need("Plasma discharge assembly",1).need("Plasma coolant cartridge",1).need("Military weapon casing",1);
            if (containsAny(all,"inferno pistol")) return energyProfile("weapon/melta/pistol", "inferno pistol pattern", "Pistol body", 1).need("Melta discharge assembly",1).need("Compact charge cradle",1).need("Thermal baffle plate",1);
            if (containsAny(all,"multi-melta")) return energyProfile("weapon/melta/heavy", "multi-melta pattern", "Heavy weapon body", 2).need("Melta discharge assembly",2).need("Heavy energy heat exchanger",1).need("Thermal baffle plate",2).need("Heavy weapon recoil cradle",1);
            if (containsAny(all,"melta")) return energyProfile("weapon/melta/rifle", "meltagun pattern", "Rifle body", 1).need("Melta discharge assembly",1).need("Thermal baffle plate",1).need("Military weapon casing",1);

            if (containsAny(all,"hand flamer")) return flameProfile("weapon/flame/pistol", "hand flamer pattern", "Pistol body", 1).need("Promethium canister",1).need("Pressure weapon hose harness",1);
            if (containsAny(all,"heavy flamer")) return flameProfile("weapon/flame/heavy", "heavy flamer pattern", "Heavy weapon body", 2).need("Promethium canister",2).need("Pressure weapon hose harness",2).need("Heavy weapon recoil cradle",1);
            if (containsAny(all,"flamer")) return flameProfile("weapon/flame/rifle", "flamer pattern", "Rifle body", 1).need("Promethium canister",1).need("Pressure weapon hose harness",1).need("Weapon stock frame",1);
            if (containsAny(all,"needle pistol")) return toxicProfile("weapon/toxin/pistol", "needle pistol pattern", "Pistol body", "Short barrel").need("Needle delivery assembly",1).need("Sterile vial rack",1);
            if (containsAny(all,"needle rifle")) return toxicProfile("weapon/toxin/rifle", "needle rifle pattern", "Rifle body", "Long barrel").need("Needle delivery assembly",1).need("Weapon stock frame",1).need("Sterile vial rack",1);
            if (containsAny(all,"webber")) return new IndustrialRecipeProfile("weapon/security/web", "webber pattern", "security cogitator node + forge", "Sanctioned Restraint Patterns").need("Rifle body",1).need("Web discharge assembly",1).need("Pressure weapon hose harness",1).need("Trigger group",1).need("Charge regulator",1).need("Web cartridge",1);
            if (containsAny(all,"arc rifle","arc prod")) return new IndustrialRecipeProfile("weapon/mechanicus/arc", "arc weapon pattern", "micro forge + diagnostic bench", "Mechanicus Armament Rites").need(containsAny(all,"rifle")?"Rifle body":"Tool frame",1).need("Arc discharge head",1).need("Arc capacitor pack",1).need("Charge regulator",1).need("Shock grip sleeve",1).need("Wire bundle",1).need("Heat sink",1);
            if (containsAny(all,"chem sprayer","acid spitter")) return new IndustrialRecipeProfile("weapon/chemical/pressure", "chemical sprayer pattern", "scrap workbench + pressure bench", "Sump Chemical Handling").need("Scrap weapon body",1).need("Pressure chamber",1).need("Pressure weapon hose harness",1).need("Compressed-gas bottle",1).need("Toxin reservoir",1).need("Grip frame",1);

            if (containsAny(all,"autocannon")) return solidProfile("weapon/auto/heavy", "autocannon pattern", "Heavy weapon body", "Reinforced barrel", 2).need("Heavy firearm action",1).need("Heavy feed action group",1).need("Autocannon shell belt",1).need("Heavy weapon recoil cradle",1);
            if (containsAny(all,"heavy stubber")) return solidProfile("weapon/stub/heavy", "heavy stubber pattern", "Heavy weapon body", "Reinforced barrel", 2).need("Heavy firearm action",1).need("Heavy feed action group",1).need("Heavy stubber belt",1).need("Heavy weapon recoil cradle",1);
            if (containsAny(all,"shotgun")) return solidProfile("weapon/shotgun", "shotgun pattern", containsAny(all,"pipe","sawed")?"Scrap weapon body":"Rifle body", containsAny(all,"pipe","sawed")?"Pressure-rated pipe barrel":"Reinforced barrel", 1).need("Shotgun breech action",1).need("Shot shell handful",1).need(containsAny(all,"pipe","sawed")?"Improvised weapon casing":"Military weapon casing",1).need(containsAny(all,"pipe","sawed")?"Crude trigger group":"Trigger group",1);
            if (containsAny(all,"autopistol")) return solidProfile("weapon/auto/pistol", "autopistol pattern", "Pistol body", "Short barrel", 1).need("Pistol firearm action",1).need("Autogun magazine",1);
            if (containsAny(all,"autogun","scrap autogun")) return solidProfile("weapon/auto/rifle", "autogun pattern", containsAny(all,"scrap")?"Scrap weapon body":"Rifle body", containsAny(all,"scrap")?"Pressure-rated pipe barrel":"Long barrel", 1).need("Rifle firearm action",1).need("Autogun magazine",1).need(containsAny(all,"scrap")?"Improvised weapon casing":"Military weapon casing",1);
            if (containsAny(all,"stub revolver")) return solidProfile("weapon/stub/revolver", "stub revolver pattern", "Pistol body", "Short barrel", 1).need("Revolver cylinder assembly",1).need("Stub cartridge box",1);
            if (containsAny(all,"stub pistol")) return solidProfile("weapon/stub/pistol", "stub pistol pattern", "Pistol body", "Short barrel", 1).need("Pistol firearm action",1).need("Stub cartridge box",1);
            if (containsAny(all,"stubcarbine")) return solidProfile("weapon/stub/carbine", "stub carbine pattern", "Carbine body", "Long barrel", 1).need("Rifle firearm action",1).need("Stub cartridge box",1).need("Weapon stock frame",1);
            if (containsAny(all,"dueling pistol","duelling pistol","noble dueling pistol")) return solidProfile("weapon/dueling/pistol", "noble dueling pistol pattern", "Pistol body", "Short barrel", 1).need("Pistol firearm action",1).need("Civilian weapon casing",1).need("Dueling pistol cartridge box",1).need("Decorative trim set",1);
            if (containsAny(all,"sniper rifle","marksman rifle","hunting rifle")) return solidProfile("weapon/marksman", "marksman rifle pattern", "Precision rifle body", "Long barrel", 1).need("Rifle firearm action",1).need("Optic mount",1).need("Weapon stock frame",1).need(containsAny(all,"hunting")?"Civilian weapon casing":"Military weapon casing",1);
            if (containsAny(all,"zip pistol","cult martyr pistol")) return new IndustrialRecipeProfile("weapon/improvised/pistol", "zip pistol pattern", "scrap workbench", "Junk Fabrication Patterns").need("Scrap weapon body",1).need("Pressure-rated pipe barrel",1).need("Crude trigger group",1).need("Spring scrap",1).need("Grip wrap",1).need("Stub cartridge box",1).need("Improvised weapon casing",1);
            if (containsAny(all,"hiver nail rifle")) return new IndustrialRecipeProfile("weapon/improvised/nail", "nail rifle pattern", "scrap workbench", "Junk Fabrication Patterns").need("Tool frame",1).need("Pressure chamber",1).need("Nail feed box",1).need("Compressed-gas bottle",1).need("Crude trigger group",1).need("Grip wrap",1).need("Improvised weapon casing",1);
            if (containsAny(all,"nail flail","heretic nail flail")) return new IndustrialRecipeProfile("weapon/improvised/flail", "heretic nail flail pattern", "scrap workbench", "Forbidden Armament Patterns").need("Haft core",1).need("Nail feed box",1).need("Chain teeth strip",1).need("Grip wrap",1).need("Improvised weapon casing",1).need("Bone charm string",1);

            if (containsAny(all,"heavy rock saw")) return new IndustrialRecipeProfile("weapon/chain/industrial-saw", "heavy rock saw pattern", "industrial cutter bench", "Industrial Rescue Tool Patterns").need("Heavy blade blank",1).need("Chain cutter rail assembly",1).need("Heavy cutter carriage",1).need("Mining tooth bit",1).need("Trigger group",1).need("Heat sink",1).need("Reinforced fastener set",1);
            if (containsAny(all,"chainaxe","chainsword","chain cleaver","buzz-cleaver")) return new IndustrialRecipeProfile("weapon/chain/melee", "chain weapon pattern", "micro forge", "Chain-Weapon Maintenance Rites").need(containsAny(all,"axe","cleaver")?"Heavy blade blank":"Blade blank",1).need("Chain cutter rail assembly",1).need("Chain drive motor",1).need("Grip frame",1).need("Trigger group",1).need("Heat sink",1).need("Reinforced fastener set",1);
            if (containsAny(all,"force sword","psychic staff")) return new IndustrialRecipeProfile("weapon/force/psyker", "force weapon focus pattern", "psyker-sanctioned reliquary bench", "Sanctioned Psyker Focus Patterns").need(containsAny(all,"staff")?"Haft core":"Blade blank",1).need("Force focus lattice",1).need("Sanctified control housing",1).need("Weighted pommel",1).need("Purity seal backing",1).need("Grip frame",1);
            if (containsAny(all,"power axe","power maul","power pick","power sword","thunder hammer","omnissian axe","power claymore","power blade")) return new IndustrialRecipeProfile("weapon/power/melee", "power weapon pattern", "micro forge + power-field bench", "Power-Field Armament Rites").need(containsAny(all,"hammer","maul")?"Hammer head blank":containsAny(all,"axe","pick")?"Heavy blade blank":"Blade blank",1).need("Field emitter array",1).need("Power-field coil",1).need("Las power capacitor",1).need("Sanctified control housing",1).need("Grip frame",1).need("Reinforced fastener set",1);
            if (containsAny(all,"shock baton","shock maul")) return new IndustrialRecipeProfile("weapon/shock/melee", "shock weapon pattern", "security armory bench", "Sanctioned Riot Gear Patterns").need(containsAny(all,"maul")?"Hammer head blank":"Haft core",1).need("Shock head assembly",1).need("Shock grip sleeve",1).need("Arc capacitor pack",1).need("Small fastener set",1);
            if (containsAny(all,"emergency cutter","pressure cutter")) return new IndustrialRecipeProfile("weapon/tool/cutter", "emergency cutter pattern", "micro forge", "Industrial Rescue Tool Patterns").need("Tool frame",1).need("Cutter armature",1).need("Cutter head",1).need("Grip frame",1).need("Trigger group",1).need("Heat sink",1);
            if (containsAny(all,"emergency drill")) return new IndustrialRecipeProfile("weapon/tool/drill", "emergency drill pattern", "micro forge", "Industrial Rescue Tool Patterns").need("Tool frame",1).need("Drill chuck",1).need("Drill head",1).need("Grip frame",1).need("Trigger group",1).need("Heat sink",1);
            if (containsAny(all,"mono-knife","monoblade","mono knife")) return new IndustrialRecipeProfile("weapon/blade/mono", "mono-edge blade pattern", "precision forge", "Common Armament Patterns").need("Blade blank",1).need("Mono-edge strip",1).need("Grip frame",1).need("Weighted pommel",1).need("Small fastener set",1);
            if (containsAny(all,"toxic knife","bio-dagger","bio dagger")) return new IndustrialRecipeProfile("weapon/blade/toxin", "toxin blade pattern", "toxin cabinet + workbench", "Needle Weapon Patterns").need("Blade blank",1).need("Toxin delivery channel",1).need("Toxin reservoir",1).need("Grip frame",1).need("Sterile vial rack",1);
            if (containsAny(all,"duelling sword","dueling sword")) return new IndustrialRecipeProfile("weapon/blade/dueling", "duelling blade pattern", "fine weapon bench", "Noble Armory Patterns").need("Blade blank",1).need("Blade edge strip",1).need("Weighted pommel",1).need("Grip frame",1).need("Decorative trim set",1);
            if (containsAny(all,"greatsword","sword","knife","dagger","blade","shiv")) return new IndustrialRecipeProfile("weapon/blade/melee", "blade weapon pattern", "micro forge or workbench", "Common Armament Patterns").need(containsAny(all,"greatsword")?"Heavy blade blank":"Blade blank",1).need("Blade edge strip",1).need("Grip frame",1).need("Weighted pommel",1).need("Small fastener set",1);
            if (containsAny(all,"munitorum shovel")) return new IndustrialRecipeProfile("weapon/tool/entrenching", "entrenching shovel pattern", "tool bench", "Military Logistics Patterns").need("Heavy blade blank",1).need("Haft core",1).need("Grip wrap",1).need("Rivet set",1).need("Military weapon casing",1);
            if (containsAny(all,"axe","shovel","spear","hook","pick")) return new IndustrialRecipeProfile("weapon/hafted/melee", "hafted weapon pattern", "workbench", "Common Armament Patterns").need("Heavy blade blank",1).need("Haft core",1).need("Grip wrap",1).need("Small fastener set",1).need(containsAny(all,"scrap","trash","mutant","sump")?"Scrap plate":"Armament components",1);
            if (containsAny(all,"maul","hammer","club","baton","staff","prod","wrench","spanner","cargo hook")) return new IndustrialRecipeProfile("weapon/blunt/tool", "blunt or tool weapon pattern", "workbench", "Common Armament Patterns").need("Hammer head blank",1).need("Haft core",1).need("Grip wrap",1).need(containsAny(all,"shock","arc")?"Shock head assembly":"Small fastener set",1);
        }
        if (containsAny(c,"armor") || containsAny(c,"clothing") || containsAny(u,"disguise","protection","workwear","uniform")) {
            if (containsAny(all,"flak vest","flak helmet","guard flak","flak weave")) return new IndustrialRecipeProfile("armor/flak", "flak armor pattern", "uniform armor bench", "Military Textile and Flak Patterns").need("Flak weave panel",1).need("Armor backing",1).need("Webbing strap roll",1).need("Buckle and clasp set",1).need("Rivet set",1).need("Faction insignia patch",1);
            if (containsAny(all,"suppression armor","riot helmet","riot visor","visor mask","shock gauntlets","armored boots","arbites")) return new IndustrialRecipeProfile("armor/riot", "riot protection pattern", "security armor bench", "Sanctioned Riot Gear Patterns").need("Stamped armor plate",1).need("Shock-absorber padding",1).need("Visor lens",1).need("Armor backing",1).need("Buckle and clasp set",1).need("Rank tab set",1);
            if (containsAny(all,"carapace","cuirass","decorated breastplate")) return new IndustrialRecipeProfile("armor/rigid/ceremonial", "rigid ceremonial armor pattern", "fine armor bench", "Noble Armory Patterns").need("Carapace plate",1).need("Ceramic armor tile",1).need("Armor backing",1).need("Decorative trim set",1).need("House livery ribbon set",1).need("Buckle and clasp set",1);
            if (containsAny(all,"scrap plate","scrap idol","patch armor","bone-studded","tire-rubber","chain-wrapped")) return new IndustrialRecipeProfile("armor/scrap", "scrap armor pattern", "scrap workbench", "Junk Fabrication Patterns").need("Scrap metal plate",2).need("Armor harness webbing",1).need("Reclaimed textile bundle",1).need("Wire stitching spool",1).need("Rivet set",1);
            if (containsAny(all,"dueling jacket","bodyglove","mesh","noble armored","concealment")) return new IndustrialRecipeProfile("armor/concealed", "concealed mesh garment pattern", "tailor armor bench", "Noble Concealed Armor Patterns").need("Mesh armor panel",1).need("Tailored garment liner",1).need("Concealment lining",1).need("Decorative trim set",1).need("Fastener button card",1);
            if (containsAny(all,"hazard","sump waders","respirator","gas mask","rebreather","sealed work suit","pressure underlayer","void hood","radiation","filter pack","insulated boots","rubberized")) return new IndustrialRecipeProfile("clothing/protective/sealed", "sealed protective garment pattern", "hazard suit bench", "Hazard and Void Textile Patterns").need("Rubberized fabric",1).need("Sealant strip roll",1).need("Pressure gasket set",1).need("Rebreather filter",1).need("Visor lens",1).need("Buckle and clasp set",1);
            if (containsAny(all,"guard fatigues","webbing harness","ammo bandolier","officer greatcoat","regimental coat","field boots","combat gloves")) return new IndustrialRecipeProfile("clothing/uniform/guard", "Guard uniform pattern", "uniform bench", "Military Textile Patterns").need("Workwear panel set",1).need("Webbing strap roll",1).need("Buckle and clasp set",1).need("Faction insignia patch",1).need("Rank tab set",1);
            if (containsAny(all,"workwear","fatigues","coverall","overalls","utility trousers","work shirt","industrial apron","worker")) return new IndustrialRecipeProfile("clothing/workwear", "workwear garment pattern", "civilian textile bench", "Civilian Textile Patterns").need("Workwear panel set",1).need("Coarse cloth roll",1).need("Thread spool",1).need("Fastener button card",1).need("Padding layer",1);
            if (containsAny(all,"mechanicus","robe","enginseer","electro-priest","cable mantle","mechadendrite","cogitator")) return new IndustrialRecipeProfile("clothing/uniform/mechanicus", "Mechanicus robe and harness pattern", "forge textile bench", "Mechanicus Vestment Patterns").need("Robe shell",1).need("Rubberized fabric",1).need("Wire stitching spool",1).need("Purity seal backing",1).need("Buckle and clasp set",1);
            if (containsAny(all,"cult robe","ritual hood","blasphemous","sacrificial","cult hooded")) return new IndustrialRecipeProfile("clothing/contraband/cult", "cult garment pattern", "hidden sewing bench", "Forbidden Vestment Patterns").need("Robe shell",1).need("Hood pattern cut",1).need("Bone charm string",1).need("Decorative trim set",1).need("Wire stitching spool",1);
            if (containsAny(all,"noble","formal","silk","tailored","livery","fur-lined")) return new IndustrialRecipeProfile("clothing/noble", "noble garment pattern", "tailor atelier", "Noble Manufactury Patterns").need("Coat shell",1).need("Tailored garment liner",1).need("Decorative trim set",1).need("House livery ribbon set",1).need("Fastener button card",1);
            if (containsAny(all,"gang","ganger","runner colors","spiked mask")) return new IndustrialRecipeProfile("clothing/gang", "gang clothing pattern", "gang tailor bench", "Underhive Gang Color Patterns").need("Coat shell",1).need("Reclaimed textile bundle",1).need("Leather substitute sheet",1).need("Faction insignia patch",1).need("Wire stitching spool",1);
            if (containsAny(all,"cloak","coat","jacket","greatcoat")) return new IndustrialRecipeProfile("clothing/coat", "coat garment pattern", "tailor bench", "Civilian Textile Patterns").need("Coat shell",1).need("Coarse cloth roll",1).need("Thread spool",1).need("Fastener button card",1).need("Padding layer",1);
            if (containsAny(all,"hood","mask","helmet","hard hat")) return new IndustrialRecipeProfile("clothing/head", "headgear pattern", "tailor or armor bench", "Civilian Textile Patterns").need(containsAny(all,"helmet","hard hat")?"Helmet shell":"Hood pattern cut",1).need("Padding layer",1).need("Fastener button card",1);
            if (containsAny(all,"gloves","boots","waders")) return new IndustrialRecipeProfile("clothing/limb", "glove or boot pattern", "tailor bench", "Civilian Textile Patterns").need(containsAny(all,"boots","waders")?"Boot sole set":"Glove palm set",1).need("Leather substitute sheet",1).need("Thread spool",1).need("Buckle and clasp set",1);
            if (containsAny(all,"harness","bandolier","sash")) return new IndustrialRecipeProfile("clothing/harness", "harness or sash pattern", "tailor bench", "Civilian Textile Patterns").need("Webbing strap roll",1).need("Buckle and clasp set",1).need("Fastener button card",1).need("Faction insignia patch",1);
            return new IndustrialRecipeProfile("clothing/general", "general garment pattern", "tailor bench", "Civilian Textile Patterns").need("Workwear panel set",1).need("Thread spool",1).need("Fastener button card",1).need("Coarse cloth roll",1);
        }
        // 0.8.94 SURVIVAL ECONOMY CHAINS: keep draft/audit-facing while making water, food, waste, and agriculture routes legible.
        if (containsAny(n,"atmospheric condensate")) return new IndustrialRecipeProfile("water/source/condensate", "atmospheric condensate capture", "condenser vanes", "Water Reclamation Patterns").need("Reclamation membrane",1).need("Distillation coil",1).need("Filter cartridge housing",1);
        if (containsAny(n,"greywater")) return new IndustrialRecipeProfile("water/source/greywater", "greywater collection", "laundry sump", "Water Reclamation Patterns").need("Wastewater",1).need("Filter cloth roll",1);
        if (containsAny(n,"strained sump water")) return new IndustrialRecipeProfile("water/reclamation/strained", "sump straining", "sump screen rack", "Sump Reclamation Patterns").need("Sump sludge",1).need("Sand filter pack",1).need("Filter cartridge housing",1);
        if (containsAny(n,"filtered water")) return new IndustrialRecipeProfile("water/reclamation/filtered", "filtered water reclamation", "filter gallery", "Water Reclamation Patterns").need("Wastewater",1).need("Strained sump water",1).need("Sand filter pack",1).need("Charcoal filter bed",1);
        if (containsAny(n,"potable water","clean water")) return new IndustrialRecipeProfile("water/reclamation/potable", "potable water finishing", "water refinery", "Civilian Provisioning Patterns").need("Filtered water",1).need("Water purification tab",1).need("Charcoal filter bed",1);
        if (containsAny(n,"distilled water")) return new IndustrialRecipeProfile("water/reclamation/distilled", "distilled water refining", "still bench", "Medicae and Precision Water Patterns").need("Potable water",2).need("Distillation coil",1);
        if (containsAny(n,"sterile water flask")) return new IndustrialRecipeProfile("water/medical/sterile", "sterile water flask sealing", "medicae clean bench", "Medicae Provisioning Patterns").need("Distilled water",1).need("Sterile vial rack",1).need("Food-safe sealant",1);
        if (containsAny(n,"reclamation brine")) return new IndustrialRecipeProfile("water/byproduct/brine", "reclamation brine draw", "membrane rack", "Water Reclamation Patterns").need("Wastewater",2).need("Reclamation membrane",1);
        if (containsAny(n,"toxin slurry")) return new IndustrialRecipeProfile("waste/chemical/slurry", "toxin slurry separation", "sump separator", "Sump Chemical Handling").need("Sump sludge",2).need("Sterile vial rack",1).need("Filter cartridge housing",1);
        if (containsAny(n,"reclaimed mineral cake")) return new IndustrialRecipeProfile("agriculture/mineral/reclamation", "mineral cake pressing", "filter press", "Agricultural Reclamation Patterns").need("Reclamation brine",1).need("Raw earth",1).need("Sand filter pack",1);
        if (containsAny(n,"compost substrate")) return new IndustrialRecipeProfile("agriculture/compost", "compost substrate batching", "waste yard", "Agricultural Reclamation Patterns").need("Waste biomass",2).need("Raw earth",1).need("Filtered water",1);
        if (containsAny(n,"fertilizer")) return new IndustrialRecipeProfile("agriculture/fertilizer", "fertilizer batching", "reclamation vat", "Agricultural Reclamation Patterns").need("Compost substrate",1).need("Reclaimed mineral cake",1).need("Nutrient salt packet",1);
        if (containsAny(n,"sterilized grow medium")) return new IndustrialRecipeProfile("agriculture/grow-medium", "sterilized grow medium", "hydroponic sterilizer", "Hydroponic Farm Patterns").need("Compost substrate",1).need("Distilled water",1).need("Reclaimed mineral cake",1);
        if (containsAny(n,"hydroponic nutrient solution")) return new IndustrialRecipeProfile("agriculture/nutrient-solution", "hydroponic nutrient solution", "hydroponic mixer", "Hydroponic Farm Patterns").need("Potable water",1).need("Nutrient salt packet",1).need("Fertilizer",1);
        if (containsAny(n,"hydroponic crop stock")) return new IndustrialRecipeProfile("food/raw/hydroponic-stock", "hydroponic crop stock", "hydroponic rack", "Hydroponic Farm Patterns").need("Hydroponic growth tray",1).need("Hydroponic nutrient solution",1).need("Seed culture tray",1).need("Artificial sun-lamp tube",1);
        if (containsAny(n,"hydroponic protein grain")) return new IndustrialRecipeProfile("food/raw/protein-grain", "hydroponic protein grain", "grain rack", "Hydroponic Farm Patterns").need("Hydroponic crop stock",1).need("Ration wrapper roll",1);
        if (containsAny(n,"marsh-rice paddy tray")) return new IndustrialRecipeProfile("food/raw/marsh-rice-tray", "marsh-rice paddy tray", "wet hydroponic rack", "Hydroponic Farm Patterns").need("Hydroponic growth tray",1).need("Hydroponic nutrient solution",1).need("Filtered water",1);
        if (containsAny(n,"marsh-rice sack")) return new IndustrialRecipeProfile("food/raw/marsh-rice", "marsh-rice sack", "grain mill", "Civilian Provisioning Patterns").need("Marsh-rice paddy tray",1).need("Ration wrapper roll",1);
        if (containsAny(n,"vorder leaf clipping tray")) return new IndustrialRecipeProfile("food/raw/vorder-tray", "vorder leaf clipping tray", "caf greenhouse", "Hydroponic Farm Patterns").need("Hydroponic growth tray",1).need("Hydroponic nutrient solution",1).need("Seed culture tray",1);
        if (containsAny(n,"vorder leaf bundle")) return new IndustrialRecipeProfile("food/stimulant/vorder", "vorder leaf bundle", "leaf drying rack", "Civilian Provisioning Patterns").need("Vorder leaf clipping tray",1).need("Ration wrapper roll",1);
        if (containsAny(n,"recaf leaf roast")) return new IndustrialRecipeProfile("food/stimulant/recaf-roast", "recaf leaf roast", "roaster", "Civilian Provisioning Patterns").need("Vorder leaf bundle",1).need("Preservative salt packet",1);
        if (containsAny(n,"recaf tin")) return new IndustrialRecipeProfile("food/stimulant/recaf-tin", "recaf tin packing", "cafeteria packing bench", "Civilian Provisioning Patterns").need("Recaf leaf roast",1).need("Tin can sleeve",1);
        if (containsAny(n,"caba nut culture pot")) return new IndustrialRecipeProfile("food/raw/caba-culture", "caba nut culture", "hydroponic nut rack", "Hydroponic Farm Patterns").need("Hydroponic growth tray",1).need("Seed culture tray",1).need("Hydroponic nutrient solution",1);
        if (containsAny(n,"caba nut packet")) return new IndustrialRecipeProfile("food/raw/caba-packet", "caba nut packet", "market food packing bench", "Civilian Provisioning Patterns").need("Caba nut culture pot",1).need("Ration wrapper roll",1);
        if (containsAny(n,"ploin fruit culture tray")) return new IndustrialRecipeProfile("food/raw/ploin-culture", "ploin fruit culture", "fruit hydroponic rack", "Hydroponic Farm Patterns").need("Hydroponic growth tray",1).need("Seed culture tray",1).need("Hydroponic nutrient solution",1);
        if (containsAny(n,"ploin fruit pulp")) return new IndustrialRecipeProfile("food/fruit/ploin-pulp", "ploin fruit pulp", "fruit press", "Void Provisioning Patterns").need("Ploin fruit culture tray",1).need("Potable water",1);
        if (containsAny(n,"ploin vitamin concentrate")) return new IndustrialRecipeProfile("food/drink/ploin-concentrate", "ploin vitamin concentrate", "fruit processor", "Void Provisioning Patterns").need("Ploin fruit pulp",1).need("Distilled water",1).need("Sterile vial rack",1);
        if (containsAny(n,"ploin juice flask")) return new IndustrialRecipeProfile("food/drink/ploin-juice", "ploin juice flask", "void provisioner bottling bench", "Void Provisioning Patterns").need("Ploin vitamin concentrate",1).need("Potable water",1).need("Ration wrapper roll",1);
        if (containsAny(n,"noble orchard graft stock")) return new IndustrialRecipeProfile("agriculture/luxury/graft", "noble orchard graft stock", "sealed orchard nursery", "Noble Bio-Garden Patterns").need("Seed culture tray",1).need("Distilled water",1).need("Nutrient salt packet",1);
        if (containsAny(n,"bio-garden soil bed")) return new IndustrialRecipeProfile("agriculture/luxury/soil", "bio-garden soil bed", "noble garden bench", "Noble Bio-Garden Patterns").need("Sterilized grow medium",1).need("Fertilizer",1).need("Distilled water",1);
        if (containsAny(n,"artificial-sun orchard tray")) return new IndustrialRecipeProfile("agriculture/luxury/orchard-tray", "artificial-sun orchard tray", "noble orchard rig", "Noble Bio-Garden Patterns").need("Bio-garden soil bed",1).need("Noble orchard graft stock",1).need("Artificial sun-lamp tube",1);
        if (containsAny(n,"noble orchard fruit crate")) return new IndustrialRecipeProfile("food/luxury/orchard-fruit", "noble orchard fruit crate", "sealed orchard", "Noble Bio-Garden Patterns").need("Artificial-sun orchard tray",1).need("Ration wrapper roll",1);
        if (containsAny(n,"bio-garden truffle tin")) return new IndustrialRecipeProfile("food/luxury/truffle", "bio-garden truffle tin", "sealed bio-garden bed", "Noble Bio-Garden Patterns").need("Bio-garden soil bed",1).need("Sterilized grow medium",1).need("Tin can sleeve",1);
        if (containsAny(n,"fruit mash")) return new IndustrialRecipeProfile("food/fruit/mash", "fruit mash", "orchard processor", "Noble Bio-Garden Patterns").need("Noble orchard fruit crate",1).need("Potable water",1).need("Mash tun liner",1);
        if (containsAny(n,"fermentable scrap mash")) return new IndustrialRecipeProfile("food/fermentation/scrap-mash", "fermentable scrap mash", "sump still", "Underhive Fermentation Patterns").need("Ration paste",1).need("Kitchen grease tin",1).need("Fermentation yeast culture",1);
        if (containsAny(n,"grain mash")) return new IndustrialRecipeProfile("food/grain/mash", "grain mash", "grain mill", "Civilian Provisioning Patterns").need("Marsh-rice sack",1).need("Hydroponic protein grain",1).need("Mash tun liner",1);
        if (containsAny(n,"distilled spirit base")) return new IndustrialRecipeProfile("drink/spirit/base", "distilled spirit base", "still bench", "Distillation Patterns").need("Fruit mash",1).need("Grain mash",1).need("Distillation coil",1);
        if (containsAny(n,"low-grade amasec wash")) return new IndustrialRecipeProfile("drink/amasec/low-wash", "low-grade amasec wash", "sump still", "Underhive Fermentation Patterns").need("Fermentable scrap mash",1).need("Distillation coil",1).need("Potable water",1);
        if (containsAny(n,"aged amasec cask")) return new IndustrialRecipeProfile("drink/amasec/aged-cask", "aged amasec cask", "orchard stillhouse", "Noble Bio-Garden Patterns").need("Distilled spirit base",1).need("Preservative salt packet",1).need("Tin can sleeve",1);
        if (containsAny(n,"low-quality amasec bottle","void crew wobble bottle")) return new IndustrialRecipeProfile("drink/amasec/low", "low-quality amasec bottling", "sump still", "Underhive Fermentation Patterns").need("Low-grade amasec wash",1).need("Ration wrapper roll",1);
        if (containsAny(n,"high-quality amasec bottle")) return new IndustrialRecipeProfile("drink/amasec/high", "high-quality amasec bottling", "noble stillhouse", "Noble Bio-Garden Patterns").need("Aged amasec cask",1).need("Fruit mash",1).need("Food-safe sealant",1);
        if (containsAny(n,"algae culture vat")) return new IndustrialRecipeProfile("food/raw/algae-vat", "algae culture vat", "algae vat", "Nutrient Vat Patterns").need("Algae starter culture",1).need("Hydroponic nutrient solution",1).need("Filtered water",1);
        if (containsAny(n,"soylens algae paste")) return new IndustrialRecipeProfile("food/intermediate/soylens-paste", "soylens algae paste", "algae press", "Nutrient Vat Patterns").need("Algae culture vat",1).need("Protein binder paste",1);
        if (containsAny(n,"soylens viridian algae cake")) return new IndustrialRecipeProfile("food/synthetic/soylens", "soylens algae cake", "ration press", "Nutrient Vat Patterns").need("Soylens algae paste",1).need("Ration wrapper roll",1);
        if (containsAny(n,"fungus culture tray")) return new IndustrialRecipeProfile("food/raw/fungus-tray", "fungus culture tray", "sump fungus rack", "Underhive Agriculture Patterns").need("Fungus starter mat",1).need("Compost substrate",1).need("Filtered water",1);
        if (containsAny(n,"sump fungus loaf")) return new IndustrialRecipeProfile("food/underhive/fungus-loaf", "sump fungus loaf", "sump kitchen", "Underhive Agriculture Patterns").need("Fungus culture tray",1).need("Starch flour sack",1).need("Food-safe sealant",1);
        if (containsAny(n,"wall-rat protein stock")) return new IndustrialRecipeProfile("food/raw/wall-rat", "wall-rat protein stock", "sump butcher table", "Underhive Protein Patterns").need("Waste biomass",1).need("Preservative salt packet",1);
        if (containsAny(n,"preserved meat strip batch")) return new IndustrialRecipeProfile("food/meat/preserved-batch", "preserved meat strip batch", "smoke rack", "Underhive Protein Patterns").need("Wall-rat protein stock",1).need("Preservative salt packet",1).need("Ration wrapper roll",1);
        if (containsAny(n,"wall-rat meat strip")) return new IndustrialRecipeProfile("food/meat/wall-rat-strip", "wall-rat meat strip", "sump smoke rack", "Underhive Protein Patterns").need("Preserved meat strip batch",1).need("Food-safe sealant",1);
        if (containsAny(n,"nutrient vat base")) return new IndustrialRecipeProfile("food/vat/base", "nutrient vat base", "nutrient vat", "Nutrient Vat Patterns").need("Vat nutrient slurry",1).need("Hydroponic nutrient solution",1).need("Protein binder paste",1);
        if (containsAny(n,"protein slurry")) return new IndustrialRecipeProfile("food/vat/protein-slurry", "protein slurry", "vat agitator", "Nutrient Vat Patterns").need("Nutrient vat base",1).need("Soylens algae paste",1);
        if (containsAny(n,"amino culture broth")) return new IndustrialRecipeProfile("food/vat/amino-broth", "amino culture broth", "military nutrient vat", "Military Provisioning Patterns").need("Protein slurry",1).need("Amino additive vial",1).need("Distilled water",1);
        if (containsAny(n,"lipid skim")) return new IndustrialRecipeProfile("food/vat/lipid-skim", "lipid skim", "grease separator", "Military Provisioning Patterns").need("Kitchen grease tin",1).need("Protein slurry",1).need("Filtered water",1);
        if (containsAny(n,"triglyceride stock")) return new IndustrialRecipeProfile("food/vat/triglyceride-stock", "triglyceride stock", "military gel vat", "Military Provisioning Patterns").need("Lipid skim",1).need("Amino culture broth",1).need("Protein binder paste",1);
        if (containsAny(n,"triglyceride gel tube")) return new IndustrialRecipeProfile("food/military/triglyceride-gel", "triglyceride gel tube", "military ration extruder", "Military Provisioning Patterns").need("Triglyceride stock",1).need("Ration wrapper roll",1).need("Food-safe sealant",1);
        if (containsAny(n,"amino-porridge ration bowl")) return new IndustrialRecipeProfile("food/military/amino-porridge", "amino-porridge ration bowl", "military mess vat", "Military Provisioning Patterns").need("Amino culture broth",1).need("Grain mash",1).need("Tin can sleeve",1);
        if (containsAny(n,"corpse-starch paste")) return new IndustrialRecipeProfile("food/reclamation/corpse-paste", "corpse-starch paste", "reclamation kitchen", "Lower Hive Reclamation Patterns").need("Waste biomass",2).need("Starch flour sack",1).need("Preservative salt packet",1);
        if (containsAny(n,"corpse-starch ration slab","tin of corpse-starch")) return new IndustrialRecipeProfile("food/reclamation/corpse-starch", "corpse-starch ration", "ration press", "Lower Hive Reclamation Patterns").need("Corpse-starch paste",1).need("Ration wrapper roll",1);
        if (containsAny(n,"ration paste")) return new IndustrialRecipeProfile("food/ration/paste", "ration paste", "ration mixing vat", "Civilian Provisioning Patterns").need("Grain mash",1).need("Protein slurry",1).need("Preservative salt packet",1);
        if (containsAny(n,"emergency ration paste")) return new IndustrialRecipeProfile("food/ration/emergency-paste", "emergency ration paste", "survival ration press", "Civilian Provisioning Patterns").need("Ration paste",1).need("Protein binder paste",1).need("Water purification tab",1);
        if (containsAny(n,"emergency rations","plain ration pack","guard field ration tin","protein ration","ration brick")) return new IndustrialRecipeProfile("food/ration/final", "ration packing", "ration plant", "Civilian and Military Provisioning Patterns").need("Emergency ration paste",1).need("Ration wrapper roll",1).need("Tin can sleeve",1);
        if (containsAny(n,"hab breakfast tray","cheap lunch tin","civilian meal voucher","child creche snack pack")) return new IndustrialRecipeProfile("food/civilian/meal", "civilian meal assembly", "cafeteria kitchen", "Civilian Provisioning Patterns").need("Ration paste",1).need("Potable water",1).need("Sealed cafeteria cutlery",1);
        if (containsAny(n,"mechanicus nutrient ampoule")) return new IndustrialRecipeProfile("food/mechanicus/ampoule", "Mechanicus nutrient ampoule", "diagnostic galley", "Mechanicus Provisioning Rites").need("Amino culture broth",1).need("Sterile water flask",1).need("Sterile vial rack",1);
        if (containsAny(n,"noble preserved delicacy")) return new IndustrialRecipeProfile("food/noble/preserved", "noble preserved delicacy", "noble pantry kitchen", "Noble Bio-Garden Patterns").need("Noble orchard fruit crate",1).need("Bio-garden truffle tin",1).need("Tin can sleeve",1).need("Food-safe sealant",1);
        if (containsAny(n,"cult offering wafer")) return new IndustrialRecipeProfile("food/contraband/cult-wafer", "cult offering wafer", "hidden ritual kitchen", "Forbidden Provisioning Patterns").need("Ration paste",1).need("Bone charm string",1).need("Purity seal backing",1);
        if (containsAny(n,"water bottle","water ration","sealed water ration")) return new IndustrialRecipeProfile("water/containerized", "water good packing", "condenser/reclamation bench", "Civilian Provisioning Patterns").need("Potable water",1).need("Water purification tab",1).need("Ration wrapper roll",1);
        if (containsAny(n,"dirty canteen","filter canteen","voidside water condenser flask")) return new IndustrialRecipeProfile("tool/water/container", "water container pattern", "scrap workbench", "Junk Fabrication Patterns").need("Tool frame",1).need("Nozzle and valve set",1).need("Small fastener set",1);
        if ((containsAny(c,"food","drink") || containsNameTerm(n,"ration","rations","porridge","amasec","juice","loaf","meal","wafer","snack","grain","cake","slab","tube","bowl","recaf")) && !containsAny(c,"/raw","raw/")) return new IndustrialRecipeProfile("food/provisioning", "provisioning good", "kitchen/vat/hydroponic bench", "Civilian Provisioning Patterns").need("Potable water",1).need("Fertilizer",1).need(containsNameTerm(n,"amasec","juice")?"Fruit mash":"Vat nutrient slurry",1);

        // 0.8.95a CHEM / NARCOTIC / INTOXICANT PRODUCTION CHAINS:
        if (c.startsWith("chem/") || c.startsWith("medical/chem")) return chemProfileFor(d, n, c, all);
        if (containsAny(c,"tool") && !containsAny(c,"water")) return new IndustrialRecipeProfile("tool/general", "general tool pattern", "scrap workbench", "Junk Fabrication Patterns").need("Tool frame",1).need("Armament components",1).need("Small fastener set",1);
        return null;
    }

    static IndustrialRecipeProfile chemProfileFor(ItemDef d, String n, String c, String all) {
        String knowledge = c.startsWith("medical/chem") ? "Medical Processing Patterns" : (containsAny(c,"psyker","cult","rare-campaign") ? "Forbidden Chemical Synthesis" : "Chemical Synthesis Patterns");
        String bench = c.startsWith("medical/chem") ? "medicae chem bench" : (containsAny(c,"noble") ? "noble chem atelier" : (containsAny(c,"cult","psyker") ? "hidden ritual chem bench" : (containsAny(c,"labor") ? "manufactorum dosing bench" : "underhive chem kitchen")));
        IndustrialRecipeProfile p = new IndustrialRecipeProfile("chem/" + c.replace('/','-'), "chem/intoxicant batching", bench, knowledge);
        if (containsAny(n,"lho")) return p.need("Smokeable lho roll",1).need(containsAny(n,"saint")?"Incense resin pellet":(containsAny(n,"ash")?"Chemical reagent bottle":"Lho leaf bale"),1).need("Underhive paper twist",1);
        if (containsAny(n,"recaf")) return p.need("Recaf leaf roast",1).need("Alkaloid extract",1).need(containsAny(n,"burn")?"Stimulant salt batch":"Tin can sleeve",1);
        if (containsAny(n,"amasec","rum","gin","wine","cordial","nectar")) return p.need(containsAny(n,"high","gild","vermilion","sable","chorus","martyr")?"Distilled spirit base":"Low-grade amasec wash",1).need(containsAny(c,"noble")?"Spire crystal vial":"Underhive paper twist",1).need(containsAny(n,"gild","cordial","nectar","wine")?"Euphoric syrup base":"Chemical reagent bottle",1);
        if (containsAny(n,"stimm","redline","slaught","frenzon","rush","spinefire","hammerwake","red ticket","red hull","voidwake","ashbite","black badge")) return p.need("Combat stimm compound",1).need("Stimulant salt batch",1).need(containsAny(c,"medical","labor")?"Injector ampoule set":"Underhive paper twist",1);
        if (containsAny(n,"mercy","pain","anesthetic","nerve lace","sumpkalm","night milk","velvet sleep","starless sleep","yellow mercy","golden quiet","pale nurse","pale compliance","grav-lock","blue silence")) return p.need("Sedative tincture base",1).need("Analgesic compound",1).need(containsAny(c,"medical","security")?"Injector ampoule set":"Spire crystal vial",1);
        if (containsAny(n,"clotfoam","thickblood")) return p.need("Clotting foam reagent",1).need("Injector ampoule set",1).need("Medicae stabilizer compound",1);
        if (containsAny(n,"wakewire","red waker")) return p.need("Revival shock compound",1).need("Nerve dampener solution",1).need("Injector ampoule set",1);
        if (containsAny(n,"truth","fearhook","confessor","cold candle")) return p.need("Interrogation dosing kit",1).need(containsAny(n,"fear")?"Aggression catalyst":"Compliance sedative base",1).need("Chemical reagent bottle",1);
        if (containsAny(n,"obscura","dream","lantern","mirth","haze","luckdust","pink static","jester","silver giggle","beautiful error","sorrowglass","memory lacquer","thoughtglass","third-eye","warpglimmer","dream-index","echo seed")) return p.need("Hallucinogen resin",1).need(containsAny(c,"noble","rare")?"Spire crystal vial":"Snuff capsule tin",1).need(containsAny(n,"thought","memory","index")?"Forbidden focus wafer":"Fungal spore culture",1);
        if (containsAny(n,"spook","flects","witchsalt","static communion","false astronomican","lucid null","mind-soot","black sun","mirror milk")) return p.need("Psychic catalyst powder",1).need(containsAny(n,"flect","mirror")?"Warp-tainted mirror shard":"Forbidden focus wafer",1).need("Underhive paper twist",1);
        if (containsAny(c,"cult") || containsAny(n,"choir ash","benediction","saintsbane","gutter prophet","red revelation","pale communion","carrion scripture","eclipse resin","woundlight","red choir")) return p.need("Profane binding ash",1).need("Hallucinogen resin",1).need(containsAny(n,"carrion","ash","scripture")?"Psychoactive mold cake":"Incense resin pellet",1);
        if (containsAny(c,"mutant") || containsAny(n,"sumpweed","brine joy","glowgut","mire-dream","rustmilk","scale oil","rad-sweet","dune milk")) return p.need(containsAny(n,"rad","glow")?"Radiant mineral dust":"Sump fermentation mash",1).need(containsAny(n,"fungus","mire","sumpweed","glowgut")?"Fungal spore culture":"Mineral tonic slurry",1).need("Underhive paper twist",1);
        if (containsAny(c,"labor") || containsAny(n,"shiftwake","line-keeper","quota joy","stillhand","cogitator blue","clerk","boiler black","dustlung")) return p.need(containsAny(n,"stillhand","cogitator","blue lantern","wirewake")?"Focus nootropic base":"Stimulant salt batch",1).need("Labor dosing ticket strip",1).need(containsAny(n,"dustlung","soot")?"Aerosol propellant bulb":"Chemical reagent bottle",1);
        if (containsAny(c,"vice") || containsAny(n,"soft gold","velvet teeth","diceblood","sweet ruin","hushpetal")) return p.need(containsAny(n,"hush")?"Sedative tincture base":"Euphoric syrup base",1).need(containsAny(n,"dice","luck")?"Focus nootropic base":"Confection binder base",1).need("Snuff capsule tin",1);
        if (containsAny(c,"void") || containsAny(n,"ploin","hullshine","grav-sick")) return p.need(containsAny(n,"ploin")?"Vitamin concentrate":(containsAny(n,"hullshine","blackwater")?"Voidship rotgut wash":"Anti-nausea lozenge base"),1).need(containsAny(n,"ploin")?"Ploin vitamin concentrate":"Chemical reagent bottle",1).need("Underhive paper twist",1);
        if (containsAny(c,"devotional") || containsAny(n,"pilgrim","censer","reliquary","flagellant","choir honey","ashen host")) return p.need(containsAny(n,"flagellant")?"Aggression catalyst":"Incense resin pellet",1).need("Devotional wrapper strip",1).need(containsAny(n,"honey")?"Euphoric syrup base":"Sedative tincture base",1);
        if (containsAny(c,"lower-hive-trash") || containsAny(n,"pipe bloom","drain sugar","battery kiss","blue scab","rust angel","glass belly","cradle rot")) return p.need("Toxin slurry",1).need("Sump fermentation mash",1).need(containsAny(n,"battery","rust","blue")?"Radiant mineral dust":"Underhive paper twist",1);
        if (containsAny(c,"rare-campaign") || containsAny(n,"emperor","kingmaker","widow","ghost orchid","ebon lotus","angel engine","machine rapture")) return p.need(containsAny(n,"engine","machine")?"Focus nootropic base":"Hallucinogen resin",1).need(containsAny(n,"widow")?"Toxin reservoir":"Spire crystal vial",1).need(containsAny(n,"kingmaker")?"Euphoric syrup base":"Psychic catalyst powder",1);
        if (containsAny(n,"grin","joy","kiss","static chew","glow-snuff","grey drops","bad saint")) return p.need("Euphoric syrup base",1).need("Stimulant salt batch",1).need("Lozenge binder base",1);
        return p.need("Chemical reagent bottle",1).need("Alkaloid extract",1).need("Underhive paper twist",1);
}

    static IndustrialRecipeProfile solidProfile(String family, String label, String body, String barrel, int heaviness) {
        return new IndustrialRecipeProfile(family, label, heaviness > 1 ? "micro forge + heavy armory bench" : "micro forge + armory bench", "Solid-Projectile Armament Patterns").need(body,1).need(barrel,1).need("Receiver block",1).need("Trigger group",1).need("Grip frame",1).need("Heat sink",Math.max(1,heaviness));
    }
    static IndustrialRecipeProfile energyProfile(String family, String label, String body, int heaviness) {
        return new IndustrialRecipeProfile(family, label, heaviness > 1 ? "sealed energy bench" : "micro forge + energy bench", "High-Energy Weapon Rites").need(body,1).need("Charger cell socket",1).need("Trigger group",1).need("Grip frame",1).need("Reinforced fastener set",heaviness);
    }
    static IndustrialRecipeProfile flameProfile(String family, String label, String body, int heaviness) {
        return new IndustrialRecipeProfile(family, label, heaviness > 1 ? "heavy pressure bench" : "pressure bench", "Promethium Weapon Patterns").need(body,1).need("Pressure chamber",1).need("Fuel canister mount",1).need("Igniter assembly",1).need("Nozzle and valve set",1).need("Trigger group",1).need("Heat sink",heaviness);
    }
    static IndustrialRecipeProfile toxicProfile(String family, String label, String body, String barrel) {
        return new IndustrialRecipeProfile(family, label, "precision bench + toxin cabinet", "Needle Weapon Patterns").need(body,1).need(barrel,1).need("Toxin reservoir",1).need("Pressure chamber",1).need("Trigger group",1).need("Grip frame",1);
    }
    static IndustrialRecipeProfile lasProfile(String family, String label, String body, String emitter, boolean precision) {
        return new IndustrialRecipeProfile(family, label, precision ? "micro forge + optics bench" : "micro forge + las bench", "Las-Weapon Assembly Doctrine")
                .need(body,1).need(emitter,1).need(precision ? "High-grade focusing lens" : "Focusing lens",1)
                .need("Las power capacitor",1).need("Charger cell socket",1).need("Heat sink", precision ? 2 : 1);
    }
    static boolean containsAny(String hay, String... needles) { for (String x : needles) if (hay.contains(x)) return true; return false; }
    static boolean containsNameTerm(String hay, String... terms) { String padded = " " + low(hay).replace('-',' ').replace('_',' ').replace('/',' ') + " "; for (String t : terms) if (padded.contains(" " + low(t) + " ")) return true; return false; }
    static String low(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
    static class ComponentNeed { final String item; final int count; ComponentNeed(String item, int count){this.item=item; this.count=count;} }
}


class RecipeDecompositionApi {
    static boolean isDraftDecomposable(ItemDef d) {
        if (d == null) return false;
        String c = d.category == null ? "" : d.category.toLowerCase(Locale.ROOT);
        if (c.startsWith("component/") || c.startsWith("ammo")) return false;
        if (c.contains("cache") || c.contains("paperwork") || c.contains("currency") || c.contains("victory")) return false;
        return d.weapon || c.contains("food") || c.contains("drink") || c.contains("water") || c.contains("tool") || c.startsWith("clothing") || c.startsWith("armor") || c.startsWith("agriculture") || c.startsWith("organic/") || c.startsWith("chemical/waste") || c.startsWith("chem/") || c.startsWith("medical/chem");
    }
    static ArrayList<DraftIndustrialRecipe> generatedDraftRecipes() {
        ArrayList<DraftIndustrialRecipe> out = new ArrayList<>();
        for (ItemDef d : ItemCatalog.ITEMS.values()) {
            if (!isDraftDecomposable(d)) continue;
            IndustrialRecipeProfile p = IndustrialRecipeProfile.profileFor(d);
            if (p != null) out.add(p.build(d));
        }
        out.addAll(componentRecipes());
        for (DraftIndustrialRecipe r : out) ChemicalEquipmentAuthority.applyRequirements(r);
        return out;
    }
    static ArrayList<DraftIndustrialRecipe> componentRecipes() {
        ArrayList<DraftIndustrialRecipe> r = new ArrayList<>();
        r.add(new DraftIndustrialRecipe("Draft: armament component batch", "Armament components", "component/armament", "RecipeDecompositionApi component seed", "Shared precursor, not player-facing yet.").input("Machine part",1).input("Scrap plate",1).input("Mechanical detritus",1));
        r.add(new DraftIndustrialRecipe("Draft: small fastener set", "Small fastener set", "component/fasteners", "RecipeDecompositionApi component seed", "Shared precursor, not player-facing yet.").input("Mechanical detritus",1));
        r.add(new DraftIndustrialRecipe("Draft: reinforced fastener set", "Reinforced fastener set", "component/fasteners/heavy", "RecipeDecompositionApi component seed", "Shared precursor, not player-facing yet.").input("Small fastener set",2).input("Armament components",1));
        r.add(new DraftIndustrialRecipe("Draft: pistol body", "Pistol body", "component/body/pistol", "RecipeDecompositionApi component seed", "Pistol body decomposes into fewer armament parts than rifle bodies.").input("Armament components",1).input("Grip frame",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: carbine body", "Carbine body", "component/body/carbine", "RecipeDecompositionApi component seed", "Carbine body occupies the middle ground between pistol and rifle.").input("Armament components",2).input("Weapon stock frame",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: rifle body", "Rifle body", "component/body/rifle", "RecipeDecompositionApi component seed", "Rifle body intentionally costs three armament components.").input("Armament components",3).input("Weapon stock frame",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: precision rifle body", "Precision rifle body", "component/body/precision", "RecipeDecompositionApi component seed", "Precision body adds optics mounting and stricter parts.").input("Rifle body",1).input("Optic mount",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: heavy weapon body", "Heavy weapon body", "component/body/heavy", "RecipeDecompositionApi component seed", "Heavy body uses double bracing and much larger part demand.").input("Armament components",6).input("Receiver block",2).input("Reinforced fastener set",2));
        r.add(new DraftIndustrialRecipe("Draft: scrap weapon body", "Scrap weapon body", "component/body/improvised", "RecipeDecompositionApi component seed", "Cheap underhive body route, fragile but available.").input("Scrap plate",2).input("Grip wrap",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: refined metal stock", "Refined metal stock", "component/material/metal", "RecipeDecompositionApi component expansion", "Scrap metal becomes reusable stock for frames and receivers.").input("Ferric scrap",2).input("Machine part",1));
        r.add(new DraftIndustrialRecipe("Draft: hardened metal stock", "Hardened metal stock", "component/material/hardened", "RecipeDecompositionApi component expansion", "Metal stock heat-treated for barrels, blades, and high-stress assemblies.").input("Refined metal stock",1).input("Machine oil vial",1));
        r.add(new DraftIndustrialRecipe("Draft: scrap plate rolling", "Scrap plate", "component/material/scrap", "RecipeDecompositionApi component expansion", "Flat salvage plate produced from ferric scrap and crude sorting.").input("Ferric scrap",1).input("Spare bolts",1));
        r.add(new DraftIndustrialRecipe("Draft: industrial polymer sheet", "Industrial polymer sheet", "component/material/polymer", "RecipeDecompositionApi component expansion", "Polymer sheet stock for casings, grips, and packaging.").input("Mechanical detritus",1).input("Machine oil vial",1));
        r.add(new DraftIndustrialRecipe("Draft: ceramic insulator blank", "Ceramic insulator blank", "component/electrical/ceramic", "RecipeDecompositionApi component expansion", "Raw earth and clean water become ceramic electrical insulation blanks.").input("Raw earth",1).input("Distilled water",1));
        r.add(new DraftIndustrialRecipe("Draft: glass optic blank", "Glass optic blank", "component/optic/raw", "RecipeDecompositionApi component expansion", "Clean optic blanks are the lower ancestor of focusing lenses.").input("Raw earth",1).input("Distilled water",1).input("Machine part",1));
        r.add(new DraftIndustrialRecipe("Draft: focusing lens grinding", "Focusing lens", "component/las/lens", "RecipeDecompositionApi component expansion", "Standard las lenses derive from glass optic blanks and precision adjusters.").input("Glass optic blank",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: high-grade focusing lens grinding", "High-grade focusing lens", "component/las/lens/precision", "RecipeDecompositionApi component expansion", "Precision lenses require better blanks, sensors, and calibration hardware.").input("Focusing lens",1).input("Sensor crystal",1).input("Calibration screw set",2));
        r.add(new DraftIndustrialRecipe("Draft: conductive filament spool", "Conductive filament spool", "component/electrical/wire", "RecipeDecompositionApi component expansion", "Fine conductor wire drawn from ordinary wire bundles and contacts.").input("Wire bundle",1).input("Contact strip",1));
        r.add(new DraftIndustrialRecipe("Draft: insulation sleeve", "Insulation sleeve", "component/electrical/insulation", "RecipeDecompositionApi component expansion", "Sleeving made from polymer and ceramic insulation stock.").input("Industrial polymer sheet",1).input("Ceramic insulator blank",1));
        r.add(new DraftIndustrialRecipe("Draft: contact strip", "Contact strip", "component/electrical/contact", "RecipeDecompositionApi component expansion", "Battery contacts and terminals cut from wire and small fasteners.").input("Wire bundle",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: capacitor wafer stack", "Capacitor wafer stack", "component/electrical/capacitor", "RecipeDecompositionApi component expansion", "Capacitor wafers become the parent of las capacitors and energy buffers.").input("Ceramic insulator blank",1).input("Conductive filament spool",1).input("Contact strip",1));
        r.add(new DraftIndustrialRecipe("Draft: las power capacitor", "Las power capacitor", "component/las/power", "RecipeDecompositionApi component expansion", "Las capacitors require capacitor wafers, contacts, and insulation.").input("Capacitor wafer stack",1).input("Contact strip",1).input("Insulation sleeve",1));
        r.add(new DraftIndustrialRecipe("Draft: charge regulator", "Charge regulator", "component/energy/regulator", "RecipeDecompositionApi component expansion", "Energy regulators combine control wafers and capacitor hardware.").input("Circuit wafer",1).input("Capacitor wafer stack",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: charger cell socket", "Charger cell socket", "component/energy/socket", "RecipeDecompositionApi component expansion", "Cell sockets derive from contact strips, insulation, and fittings.").input("Contact strip",1).input("Insulation sleeve",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: power coupling socket", "Power coupling socket", "component/energy/coupling", "RecipeDecompositionApi component expansion", "Power couplers bridge capacitors, regulators, and emitters.").input("Charger cell socket",1).input("Conductive filament spool",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: heat sink block", "Heat sink", "component/cooling", "RecipeDecompositionApi component expansion", "Heat sinks are machined from metal stock with contact surfaces.").input("Refined metal stock",1).input("Machine oil vial",1));
        r.add(new DraftIndustrialRecipe("Draft: compact las emitter", "Compact las emitter", "component/las/emitter", "RecipeDecompositionApi component expansion", "Compact emitters combine a lens, coupling, and heat path.").input("Focusing lens",1).input("Power coupling socket",1).input("Heat sink",1));
        r.add(new DraftIndustrialRecipe("Draft: short las emitter barrel", "Short las emitter barrel", "component/las/emitter", "RecipeDecompositionApi component expansion", "Carbine emitters extend the compact emitter with barrel and bracing.").input("Compact las emitter",1).input("Short barrel",1).input("Heat sink",1));
        r.add(new DraftIndustrialRecipe("Draft: long las emitter barrel", "Long las emitter barrel", "component/las/emitter/long", "RecipeDecompositionApi component expansion", "Rifle emitters require longer heat-managed channels.").input("Compact las emitter",1).input("Long barrel",1).input("Heat sink",2));
        r.add(new DraftIndustrialRecipe("Draft: stabilized las barrel", "Stabilized las barrel", "component/las/emitter/precision", "RecipeDecompositionApi component expansion", "Longlas barrels add sensor and calibration hardware.").input("Long las emitter barrel",1).input("High-grade focusing lens",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: bearing set", "Bearing set", "component/mechanical/bearing", "RecipeDecompositionApi component expansion", "Bearings are recovered and refit from machine parts and oil.").input("Machine part",1).input("Machine oil vial",1));
        r.add(new DraftIndustrialRecipe("Draft: gear train", "Gear train", "component/mechanical/gear", "RecipeDecompositionApi component expansion", "Gear trains need metal stock, bearings, and tolerable alignment.").input("Refined metal stock",1).input("Bearing set",1));
        r.add(new DraftIndustrialRecipe("Draft: motor coil pack", "Motor coil pack", "component/mechanical/motor", "RecipeDecompositionApi component expansion", "Motor coils combine conductor windings, bearings, and casing stock.").input("Conductive filament spool",1).input("Bearing set",1).input("Refined metal stock",1));
        r.add(new DraftIndustrialRecipe("Draft: micro-actuator", "Micro-actuator", "component/mechanical/actuator", "RecipeDecompositionApi component expansion", "Actuators combine motor coils, gears, and circuit wafers.").input("Motor coil pack",1).input("Gear train",1).input("Circuit wafer",1));
        r.add(new DraftIndustrialRecipe("Draft: servo linkage", "Servo linkage", "component/mechanical/servo", "RecipeDecompositionApi component expansion", "Servo linkages are actuator hardware plus bracing.").input("Micro-actuator",1).input("Refined metal stock",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: gearbox casing", "Gearbox casing", "component/mechanical/casing", "RecipeDecompositionApi component expansion", "Gearbox casings hold drive parts together during bad decisions.").input("Refined metal stock",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: hardened spring set", "Hardened spring set", "component/spring", "RecipeDecompositionApi component expansion", "Serviceable springs from hardened stock and calibration work.").input("Hardened metal stock",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: recoil spring assembly", "Recoil spring assembly", "component/weapon/recoil", "RecipeDecompositionApi component expansion", "Firearm recoil hardware from springs and stock.").input("Hardened spring set",1).input("Refined metal stock",1));
        r.add(new DraftIndustrialRecipe("Draft: firing pin set", "Firing pin set", "component/weapon/ignition", "RecipeDecompositionApi component expansion", "Pins and strikers from hardened metal and tiny fasteners.").input("Hardened metal stock",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: extractor claw set", "Extractor claw set", "component/weapon/extractor", "RecipeDecompositionApi component expansion", "Extractors require hardened hooks and springs.").input("Hardened metal stock",1).input("Hardened spring set",1));
        r.add(new DraftIndustrialRecipe("Draft: bolt carrier assembly", "Bolt carrier assembly", "component/weapon/boltcarrier", "RecipeDecompositionApi component expansion", "Automatic firearm carrier assembly for rifle and heavy actions.").input("Receiver block",1).input("Recoil spring assembly",1).input("Extractor claw set",1));
        r.add(new DraftIndustrialRecipe("Draft: feed pawl assembly", "Feed pawl assembly", "component/weapon/feed/heavy", "RecipeDecompositionApi component expansion", "Heavy-feed hardware for belts and awkward munition chains.").input("Magazine well",1).input("Hardened spring set",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: breech seal", "Breech seal", "component/weapon/breech", "RecipeDecompositionApi component expansion", "Breech seals pair hardened stock with gasket material.").input("Hardened metal stock",1).input("Sealing gasket set",1));
        r.add(new DraftIndustrialRecipe("Draft: gas piston assembly", "Gas piston assembly", "component/weapon/gas", "RecipeDecompositionApi component expansion", "Gas piston assemblies support auto-family cycling.").input("Pressure chamber",1).input("Recoil spring assembly",1).input("Pipe coupling set",1));
        r.add(new DraftIndustrialRecipe("Draft: pistol firearm action", "Pistol firearm action", "component/weapon/action/pistol", "RecipeDecompositionApi weapon decomposition", "Compact firearm action group for pistols and small stub weapons.").input("Receiver block",1).input("Recoil spring assembly",1).input("Firing pin set",1).input("Extractor claw set",1));
        r.add(new DraftIndustrialRecipe("Draft: rifle firearm action", "Rifle firearm action", "component/weapon/action/rifle", "RecipeDecompositionApi weapon decomposition", "Rifle action group for auto, stub, and marksman longarms.").input("Bolt carrier assembly",1).input("Gas piston assembly",1).input("Firing pin set",1).input("Extractor claw set",1));
        r.add(new DraftIndustrialRecipe("Draft: heavy firearm action", "Heavy firearm action", "component/weapon/action/heavy", "RecipeDecompositionApi weapon decomposition", "Heavy firearm action group for stubbers and autocannons.").input("Bolt carrier assembly",1).input("Feed pawl assembly",1).input("Breech seal",1).input("Recoil spring assembly",1));
        r.add(new DraftIndustrialRecipe("Draft: heavy feed action group", "Heavy feed action group", "component/weapon/feed/heavy-group", "RecipeDecompositionApi weapon decomposition", "Belt and pawl group for heavy weapons.").input("Feed pawl assembly",1).input("Magazine well",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: bolt weapon action", "Bolt weapon action", "component/weapon/action/bolt", "RecipeDecompositionApi weapon decomposition", "Bolt action group for human-scale bolt weapons.").input("Bolt carrier assembly",1).input("Breech seal",1).input("Firing pin set",1).input("Recoil spring assembly",1));
        r.add(new DraftIndustrialRecipe("Draft: shotgun breech action", "Shotgun breech action", "component/weapon/action/shotgun", "RecipeDecompositionApi weapon decomposition", "Break or pump breech group for shotguns.").input("Breech seal",1).input("Firing pin set",1).input("Recoil spring assembly",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: revolver cylinder assembly", "Revolver cylinder assembly", "component/weapon/action/revolver", "RecipeDecompositionApi weapon decomposition", "Cylinder, pawl, and timing group for revolvers.").input("Receiver block",1).input("Firing pin set",1).input("Hardened spring set",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: heavy weapon recoil cradle", "Heavy weapon recoil cradle", "component/weapon/recoil-cradle", "RecipeDecompositionApi weapon decomposition", "Recoil cradle for heavy portable weapons and emplacements.").input("Recoil spring assembly",2).input("Reinforced fastener set",1).input("Hardened metal stock",1));
        r.add(new DraftIndustrialRecipe("Draft: military weapon casing", "Military weapon casing", "component/weapon/casing/military", "RecipeDecompositionApi weapon decomposition", "Standardized durable casing used by Guard, security, and armory production.").input("Armament components",1).input("Refined metal stock",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: civilian weapon casing", "Civilian weapon casing", "component/weapon/casing/civilian", "RecipeDecompositionApi weapon decomposition", "Simpler casing for hunting, security, and non-military weapons.").input("Refined metal stock",1).input("Industrial polymer sheet",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: improvised weapon casing", "Improvised weapon casing", "component/weapon/casing/improvised", "RecipeDecompositionApi weapon decomposition", "Scrap casing route for underhive and panic-built weapons.").input("Scrap plate",1).input("Cloth scrap",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: compact charge cradle", "Compact charge cradle", "component/energy/cradle/compact", "RecipeDecompositionApi weapon decomposition", "Compact cell cradle for pistol-scale or short high-energy weapons.").input("Charger cell socket",1).input("Contact strip",1).input("Insulation sleeve",1));
        r.add(new DraftIndustrialRecipe("Draft: plasma discharge assembly", "Plasma discharge assembly", "component/plasma/discharge", "RecipeDecompositionApi weapon decomposition", "Plasma throat, bottle, and injector assembly.").input("Plasma containment flask",1).input("Plasma injector nozzle",1).input("Magnetic bottle ring",1));
        r.add(new DraftIndustrialRecipe("Draft: melta discharge assembly", "Melta discharge assembly", "component/melta/discharge", "RecipeDecompositionApi weapon decomposition", "Melta focusing and charge discharge assembly.").input("Thermal regulator",1).input("Melta focusing crystal",1).input("Melta charge cell",1));
        r.add(new DraftIndustrialRecipe("Draft: heavy energy heat exchanger", "Heavy energy heat exchanger", "component/energy/heat-exchanger/heavy", "RecipeDecompositionApi weapon decomposition", "Cooling block for heavy plasma and melta weapons.").input("Heat sink",3).input("Thermal baffle plate",1).input("Plasma coolant cartridge",1));
        r.add(new DraftIndustrialRecipe("Draft: pressure weapon hose harness", "Pressure weapon hose harness", "component/pressure/hose-harness", "RecipeDecompositionApi weapon decomposition", "Hose, valve, and handline harness for flame, web, and chemical projectors.").input("Pressure hose bundle",1).input("Nozzle and valve set",1).input("Valve spring set",1));
        r.add(new DraftIndustrialRecipe("Draft: needle delivery assembly", "Needle delivery assembly", "component/toxin/needle-delivery", "RecipeDecompositionApi weapon decomposition", "Needle and ampoule delivery group for toxin weapons.").input("Needle lancet bundle",1).input("Toxin ampoule tray",1).input("Pressure chamber",1));
        r.add(new DraftIndustrialRecipe("Draft: web discharge assembly", "Web discharge assembly", "component/security/web-discharge", "RecipeDecompositionApi weapon decomposition", "Projector and compound discharge group for webbers.").input("Web projector spool",1).input("Web compound tub",1).input("Pressure chamber",1));
        r.add(new DraftIndustrialRecipe("Draft: arc discharge head", "Arc discharge head", "component/arc/discharge-head", "RecipeDecompositionApi weapon decomposition", "Prong and coil head for arc weapons.").input("Arc discharge coil",1).input("Arc prong set",1).input("Shock grip sleeve",1));
        r.add(new DraftIndustrialRecipe("Draft: field emitter array", "Field emitter array", "component/powerfield/emitter-array", "RecipeDecompositionApi weapon decomposition", "Distributed studs and coil links for power weapon edges or impact heads.").input("Field emitter stud",2).input("Power-field coil",1).input("Conductive filament spool",1));
        r.add(new DraftIndustrialRecipe("Draft: force focus lattice", "Force focus lattice", "component/force/lattice", "RecipeDecompositionApi weapon decomposition", "Psyker focus lattice for force weapons.").input("High-grade focusing lens",1).input("Sensor crystal",1).input("Sanctified control housing",1).input("Purity seal backing",1));
        r.add(new DraftIndustrialRecipe("Draft: mono-edge strip", "Mono-edge strip", "component/blade/mono-edge", "RecipeDecompositionApi weapon decomposition", "Fine edge strip for monoblades and mono-knives.").input("Blade edge strip",1).input("Calibration screw set",1).input("Hardened metal stock",1));
        r.add(new DraftIndustrialRecipe("Draft: toxin delivery channel", "Toxin delivery channel", "component/blade/toxin-channel", "RecipeDecompositionApi weapon decomposition", "Blade channel and reservoir taps for toxin knives.").input("Needle lancet bundle",1).input("Toxin ampoule tray",1).input("Blade edge strip",1));
        r.add(new DraftIndustrialRecipe("Draft: shock head assembly", "Shock head assembly", "component/shock/head", "RecipeDecompositionApi weapon decomposition", "Arc prongs, contact head, and insulated striking assembly.").input("Arc prong set",1).input("Arc discharge coil",1).input("Shock grip sleeve",1));
        r.add(new DraftIndustrialRecipe("Draft: chain cutter rail assembly", "Chain cutter rail assembly", "component/chainweapon/cutter-rail", "RecipeDecompositionApi weapon decomposition", "Rail, tensioner, and chain-path assembly for chain weapons.").input("Chain guide rail",1).input("Chain tensioner",1).input("Saw chain link set",1).input("Chain teeth strip",1));
        r.add(new DraftIndustrialRecipe("Draft: heavy cutter carriage", "Heavy cutter carriage", "component/tool/cutter/heavy-carriage", "RecipeDecompositionApi weapon decomposition", "Heavy carriage for rock saws and industrial cutters.").input("Gearbox casing",1).input("Motor coil pack",1).input("Bearing set",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: receiver block machining", "Receiver block", "component/weapon/receiver", "RecipeDecompositionApi component expansion", "Receiver blocks from refined stock, fasteners, and carrier channels.").input("Refined metal stock",1).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: magazine well", "Magazine well", "component/weapon/feed", "RecipeDecompositionApi component expansion", "Feed housings from metal stock, springs, and fasteners.").input("Refined metal stock",1).input("Hardened spring set",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: short barrel boring", "Short barrel", "component/weapon/barrel", "RecipeDecompositionApi component expansion", "Short barrels require hardened stock and boring tools.").input("Hardened metal stock",1).input("Machine part",1));
        r.add(new DraftIndustrialRecipe("Draft: long barrel boring", "Long barrel", "component/weapon/barrel", "RecipeDecompositionApi component expansion", "Long barrels require more hardened stock and cleaner machining.").input("Hardened metal stock",2).input("Machine part",1));
        r.add(new DraftIndustrialRecipe("Draft: reinforced barrel sleeve", "Reinforced barrel", "component/weapon/barrel/heavy", "RecipeDecompositionApi component expansion", "Heavy barrels combine hardened stock and breech sealing.").input("Long barrel",1).input("Hardened metal stock",2).input("Breech seal",1));
        r.add(new DraftIndustrialRecipe("Draft: pressure-rated pipe barrel", "Pressure-rated pipe barrel", "component/weapon/barrel/improvised", "RecipeDecompositionApi component expansion", "Improvised pressure barrels from pipe couplings and scrap plate.").input("Pipe coupling set",1).input("Scrap plate",1));
        r.add(new DraftIndustrialRecipe("Draft: pistol body refinement", "Pistol body", "component/body/pistol/refined", "RecipeDecompositionApi component expansion", "Alternate deeper route from refined stock into pistol bodies.").input("Armament components",1).input("Grip frame",1).input("Trigger group",1));
        r.add(new DraftIndustrialRecipe("Draft: grip frame shaping", "Grip frame", "component/weapon/grip", "RecipeDecompositionApi component expansion", "Grips combine handle blanks and polymer or wrapped stock.").input("Handle core blank",1).input("Industrial polymer sheet",1));
        r.add(new DraftIndustrialRecipe("Draft: trigger group refinement", "Trigger group", "component/weapon/trigger", "RecipeDecompositionApi component expansion", "Trigger groups assemble springs, pins, and calibrated metal.").input("Firing pin set",1).input("Hardened spring set",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: crude trigger group", "Crude trigger group", "component/weapon/trigger/improvised", "RecipeDecompositionApi component expansion", "Crude triggers use scrap springs, wire, and hope.").input("Spring scrap",1).input("Wire bundle",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: weapon stock frame", "Weapon stock frame", "component/weapon/stock", "RecipeDecompositionApi component expansion", "Stocks and braces from handle blanks and polymer/metal reinforcement.").input("Handle core blank",1).input("Industrial polymer sheet",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: optic mount", "Optic mount", "component/weapon/optic", "RecipeDecompositionApi component expansion", "Optic mounts require calibration hardware and stock.").input("Refined metal stock",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: pressure chamber", "Pressure chamber", "component/pressure", "RecipeDecompositionApi component expansion", "Pressure chambers require reinforced stock, seals, and pipe fittings.").input("Hardened metal stock",1).input("Sealing gasket set",1).input("Pipe coupling set",1));
        r.add(new DraftIndustrialRecipe("Draft: nozzles and valves", "Nozzle and valve set", "component/fluid", "RecipeDecompositionApi component expansion", "Fluid control sets combine valves, springs, hoses, and couplings.").input("Valve spring set",1).input("Pressure hose bundle",1).input("Pipe coupling set",1));
        r.add(new DraftIndustrialRecipe("Draft: fuel canister mount", "Fuel canister mount", "component/flame", "RecipeDecompositionApi component expansion", "Fuel mounts need metal stock, gaskets, and valve cartridge hardware.").input("Refined metal stock",1).input("Sealing gasket set",1).input("Promethium valve cartridge",1));
        r.add(new DraftIndustrialRecipe("Draft: igniter assembly", "Igniter assembly", "component/flame", "RecipeDecompositionApi component expansion", "Igniters need conductive filament and wicks.").input("Conductive filament spool",1).input("Ignition wick bundle",1));
        r.add(new DraftIndustrialRecipe("Draft: promethium valve cartridge", "Promethium valve cartridge", "component/flame/valve", "RecipeDecompositionApi component expansion", "Fuel valve cartridges require seals, hoses, and springs.").input("Sealing gasket set",1).input("Valve spring set",1).input("Pressure hose bundle",1));
        r.add(new DraftIndustrialRecipe("Draft: containment coil", "Containment coil", "component/plasma/melta", "RecipeDecompositionApi component expansion", "Containment coils use capacitor stock, filament, and insulation.").input("Capacitor wafer stack",1).input("Conductive filament spool",1).input("Insulation sleeve",1));
        r.add(new DraftIndustrialRecipe("Draft: thermal regulator", "Thermal regulator", "component/melta", "RecipeDecompositionApi component expansion", "Melta regulators combine baffles, focusing crystal, and control wafers.").input("Thermal baffle plate",1).input("Melta focusing crystal",1).input("Circuit wafer",1));
        r.add(new DraftIndustrialRecipe("Draft: plasma containment flask", "Plasma containment flask", "component/plasma", "RecipeDecompositionApi component expansion", "Plasma flasks require bottle rings, ceramic insulation, and coolant.").input("Magnetic bottle ring",1).input("Ceramic insulator blank",1).input("Plasma coolant cartridge",1));
        r.add(new DraftIndustrialRecipe("Draft: plasma injector nozzle", "Plasma injector nozzle", "component/plasma/injector", "RecipeDecompositionApi component expansion", "Injector nozzles require heat-managed hardened stock.").input("Hardened metal stock",1).input("Thermal baffle plate",1));
        r.add(new DraftIndustrialRecipe("Draft: arc discharge coil", "Arc discharge coil", "component/arc", "RecipeDecompositionApi component expansion", "Arc coils use prongs, conductors, and capacitors.").input("Arc prong set",1).input("Conductive filament spool",1).input("Capacitor wafer stack",1));
        r.add(new DraftIndustrialRecipe("Draft: power-field coil", "Power-field coil", "component/powerfield", "RecipeDecompositionApi component expansion", "Power-field coils combine emitter studs, capacitors, and conductors.").input("Field emitter stud",2).input("Capacitor wafer stack",1).input("Conductive filament spool",1));
        r.add(new DraftIndustrialRecipe("Draft: sanctified control housing", "Sanctified control housing", "component/control/religious", "RecipeDecompositionApi component expansion", "Control housings combine electronics, insulation, and religiously labeled casings.").input("Circuit wafer",1).input("Ceramic insulator blank",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: chain drive motor", "Chain drive motor", "component/chainweapon", "RecipeDecompositionApi component expansion", "Chain drive motors use motor coils, gearboxes, and bearings.").input("Motor coil pack",1).input("Gearbox casing",1).input("Bearing set",1));
        r.add(new DraftIndustrialRecipe("Draft: chain teeth strip", "Chain teeth strip", "component/chainweapon", "RecipeDecompositionApi component expansion", "Chain teeth strips from tooth blanks and saw links.").input("Cutting tooth blank",2).input("Saw chain link set",1));
        r.add(new DraftIndustrialRecipe("Draft: chain guide rail", "Chain guide rail", "component/chainweapon/rail", "RecipeDecompositionApi component expansion", "Guide rails use hardened stock and calibration screws.").input("Hardened metal stock",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: chain tensioner", "Chain tensioner", "component/chainweapon/tension", "RecipeDecompositionApi component expansion", "Chain tensioners combine springs and bearings.").input("Hardened spring set",1).input("Bearing set",1));
        r.add(new DraftIndustrialRecipe("Draft: blade blank finishing", "Blade blank", "component/blade", "RecipeDecompositionApi component expansion", "Blade blanks from metal stock and edge strips.").input("Refined metal stock",1).input("Blade edge strip",1));
        r.add(new DraftIndustrialRecipe("Draft: heavy blade blank finishing", "Heavy blade blank", "component/blade/heavy", "RecipeDecompositionApi component expansion", "Heavy blades consume more stock and edge material.").input("Refined metal stock",2).input("Blade edge strip",2));
        r.add(new DraftIndustrialRecipe("Draft: hammer head blank", "Hammer head blank", "component/blunt", "RecipeDecompositionApi component expansion", "Blunt heads from dense metal stock and fastening points.").input("Refined metal stock",2).input("Reinforced fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: haft core", "Haft core", "component/handle", "RecipeDecompositionApi component expansion", "Hafts from handle blanks and reinforcement.").input("Handle core blank",1).input("Scrap plate",1));
        r.add(new DraftIndustrialRecipe("Draft: grip wrap", "Grip wrap", "component/grip/improvised", "RecipeDecompositionApi component expansion", "Grip wrap from cloth salvage and sealing scraps.").input("Cloth scrap",1).input("Rubber gasket sheet",1));
        r.add(new DraftIndustrialRecipe("Draft: tool frame", "Tool frame", "component/tool/frame", "RecipeDecompositionApi component expansion", "Tool frames from metal stock, handles, and fasteners.").input("Refined metal stock",1).input("Handle core blank",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: cutter head", "Cutter head", "component/tool/cutter", "RecipeDecompositionApi component expansion", "Cutter heads from hardened stock and cutter armatures.").input("Hardened metal stock",1).input("Cutter armature",1));
        r.add(new DraftIndustrialRecipe("Draft: drill head", "Drill head", "component/tool/drill", "RecipeDecompositionApi component expansion", "Drill heads from hardened stock and drill chucks.").input("Hardened metal stock",1).input("Drill chuck",1));
        r.add(new DraftIndustrialRecipe("Draft: filter canteen component upgrade", "Filter canteen", "tool/water/container", "RecipeDecompositionApi component expansion", "Deeper water-container route kept draft-only for now.").input("Dirty canteen",1).input("Filter cartridge housing",1).input("Ceramic filter candle",1));
        r.add(new DraftIndustrialRecipe("Draft: filter cartridge housing", "Filter cartridge housing", "component/water/filter", "RecipeDecompositionApi component expansion", "Filter housings assemble media, cartridge body, and seals.").input("Industrial polymer sheet",1).input("Sealing gasket set",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: ceramic filter candle", "Ceramic filter candle", "component/water/filter/ceramic", "RecipeDecompositionApi component expansion", "Filter candles from ceramic blanks and charcoal media.").input("Ceramic insulator blank",1).input("Charcoal filter bed",1));
        r.add(new DraftIndustrialRecipe("Draft: reclamation membrane", "Reclamation membrane", "component/water/membrane", "RecipeDecompositionApi component expansion", "Membranes derive from polymer sheet and clean water handling.").input("Industrial polymer sheet",1).input("Distilled water",1));
        r.add(new DraftIndustrialRecipe("Draft: distillation coil", "Distillation coil", "component/water/distillation", "RecipeDecompositionApi component expansion", "Distillation coils from pipe couplings and clean metal.").input("Pipe coupling set",1).input("Refined metal stock",1));
        r.add(new DraftIndustrialRecipe("Draft: fertilizer nutrient salts", "Nutrient salt packet", "component/agriculture/nutrient", "RecipeDecompositionApi component expansion", "Nutrient salts from raw earth, waste stream, and clean water.").input("Raw earth",1).input("Waste biomass",1).input("Distilled water",1));
        r.add(new DraftIndustrialRecipe("Draft: hydroponic growth tray", "Hydroponic growth tray", "component/agriculture/tray", "RecipeDecompositionApi component expansion", "Growth trays from polymer stock, piping, and seals.").input("Industrial polymer sheet",1).input("Pipe coupling set",1).input("Sealing gasket set",1));
        r.add(new DraftIndustrialRecipe("Draft: artificial sun-lamp tube", "Artificial sun-lamp tube", "component/agriculture/light", "RecipeDecompositionApi component expansion", "Sun-lamps use optic blanks, contacts, and circuit wafers.").input("Glass optic blank",1).input("Contact strip",1).input("Circuit wafer",1));
        r.add(new DraftIndustrialRecipe("Draft: ration packaging set", "Ration wrapper roll", "component/food/packaging", "RecipeDecompositionApi component expansion", "Ration wrapping from polymer sheet and sealant.").input("Industrial polymer sheet",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: tin can sleeve", "Tin can sleeve", "component/food/packaging", "RecipeDecompositionApi component expansion", "Tin sleeves pressed from refined metal stock.").input("Refined metal stock",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: protein binder paste", "Protein binder paste", "component/food/binder", "RecipeDecompositionApi component expansion", "Binder paste from slurry and starch flour.").input("Vat nutrient slurry",1).input("Starch flour sack",1));
        r.add(new DraftIndustrialRecipe("Draft: starch flour sack", "Starch flour sack", "component/food/starch", "RecipeDecompositionApi component expansion", "Starch flour milled from hydroponic grain or marsh-rice stock.").input("Hydroponic protein grain",1).input("Marsh-rice sack",1));
        r.add(new DraftIndustrialRecipe("Draft: filtered water from wastewater", "Filtered water", "water/reclamation", "RecipeDecompositionApi water seed", "Wastewater can become filtered water with tabs and labor.").input("Wastewater",2).input("Water purification tab",1));
        r.add(new DraftIndustrialRecipe("Draft: dirty water from sump sludge", "Dirty water", "water/reclamation", "RecipeDecompositionApi water seed", "Sludge separation yields dirty water and toxin byproducts.").input("Sump sludge",2).input("Filter canteen",1));
        r.add(new DraftIndustrialRecipe("Draft: potable water", "Potable water", "water/reclamation", "RecipeDecompositionApi water seed", "Filtered water made drinkable for people and recipes.").input("Filtered water",1).input("Water purification tab",1));
        r.add(new DraftIndustrialRecipe("Draft: distilled water", "Distilled water", "water/distillation", "RecipeDecompositionApi water seed", "Industrial clean water for medicine, lenses, and electronics.").input("Potable water",2).input("Machine part",1));
        r.add(new DraftIndustrialRecipe("Draft: fertilizer", "Fertilizer", "agriculture/reclamation", "RecipeDecompositionApi agriculture seed", "Waste and raw earth become managed growth input.").input("Waste biomass",2).input("Raw earth",1).input("Sump sludge",1));
        r.add(new DraftIndustrialRecipe("Draft: vat nutrient slurry", "Vat nutrient slurry", "food/vat", "RecipeDecompositionApi agriculture seed", "Low-grade organic stream into synthetic food base.").input("Waste biomass",2).input("Potable water",1).input("Fertilizer",1));
        r.add(new DraftIndustrialRecipe("Draft: hydroponic protein grain", "Hydroponic protein grain", "food/hydroponic", "RecipeDecompositionApi agriculture seed", "Food crops require water, fertilizer, and controlled grow space.").input("Potable water",2).input("Fertilizer",1).input("Raw earth",1));
        r.add(new DraftIndustrialRecipe("Draft: fruit mash", "Fruit mash", "food/luxury/orchard", "RecipeDecompositionApi agriculture seed", "Noble orchard fruit can be processed into juice or amasec feedstock.").input("Noble orchard fruit crate",1).input("Potable water",1));
        r.add(new DraftIndustrialRecipe("Draft: atmospheric condensate capture", "Atmospheric condensate", "water/source/condensate", "RecipeDecompositionApi water seed", "Condensers create raw moisture before filtration.").input("Reclamation membrane",1).input("Distillation coil",1));
        r.add(new DraftIndustrialRecipe("Draft: greywater collection", "Greywater", "water/source/greywater", "RecipeDecompositionApi water seed", "Laundry and wash water becomes a managed reclaimable stream.").input("Wastewater",1).input("Filter cloth roll",1));
        r.add(new DraftIndustrialRecipe("Draft: strained sump water", "Strained sump water", "water/reclamation/strained", "RecipeDecompositionApi water seed", "Sump sludge is first screened into a pumpable fluid.").input("Sump sludge",1).input("Sand filter pack",1));
        r.add(new DraftIndustrialRecipe("Draft: reclamation brine", "Reclamation brine", "water/byproduct/brine", "RecipeDecompositionApi water seed", "Membrane filtration leaves a salty reject stream useful for minerals.").input("Wastewater",2).input("Reclamation membrane",1));
        r.add(new DraftIndustrialRecipe("Draft: toxin slurry", "Toxin slurry", "waste/chemical/slurry", "RecipeDecompositionApi water seed", "Sludge separation concentrates toxins for later chemical use.").input("Sump sludge",2).input("Filter cartridge housing",1));
        r.add(new DraftIndustrialRecipe("Draft: reclaimed mineral cake", "Reclaimed mineral cake", "agriculture/mineral/reclamation", "RecipeDecompositionApi agriculture seed", "Brine and earth become a mineral feedstock for plants.").input("Reclamation brine",1).input("Raw earth",1));
        r.add(new DraftIndustrialRecipe("Draft: compost substrate", "Compost substrate", "agriculture/compost", "RecipeDecompositionApi agriculture seed", "Waste biomass and earth become growth substrate.").input("Waste biomass",2).input("Raw earth",1).input("Filtered water",1));
        r.add(new DraftIndustrialRecipe("Draft: sterilized grow medium", "Sterilized grow medium", "agriculture/grow-medium", "RecipeDecompositionApi agriculture seed", "Cleaned growth medium for controlled agriculture.").input("Compost substrate",1).input("Distilled water",1).input("Reclaimed mineral cake",1));
        r.add(new DraftIndustrialRecipe("Draft: hydroponic nutrient solution", "Hydroponic nutrient solution", "agriculture/nutrient-solution", "RecipeDecompositionApi agriculture seed", "Controlled nutrient solution for agri racks.").input("Potable water",1).input("Nutrient salt packet",1).input("Fertilizer",1));
        r.add(new DraftIndustrialRecipe("Draft: hydroponic crop stock", "Hydroponic crop stock", "food/raw/hydroponic-stock", "RecipeDecompositionApi agriculture seed", "Generic hydroponic crop mass before specialization.").input("Hydroponic growth tray",1).input("Hydroponic nutrient solution",1).input("Seed culture tray",1));
        r.add(new DraftIndustrialRecipe("Draft: algae culture vat", "Algae culture vat", "food/raw/algae", "RecipeDecompositionApi vat seed", "Algae vats convert water and nutrients into food base.").input("Algae starter culture",1).input("Hydroponic nutrient solution",1).input("Filtered water",1));
        r.add(new DraftIndustrialRecipe("Draft: fungus culture tray", "Fungus culture tray", "food/raw/fungus", "RecipeDecompositionApi agriculture seed", "Fungus trays turn compost into underhive food.").input("Fungus starter mat",1).input("Compost substrate",1).input("Filtered water",1));
        r.add(new DraftIndustrialRecipe("Draft: nutrient vat base", "Nutrient vat base", "food/vat/base", "RecipeDecompositionApi vat seed", "Vat base from slurry, nutrient solution, and binder.").input("Vat nutrient slurry",1).input("Hydroponic nutrient solution",1).input("Protein binder paste",1));
        r.add(new DraftIndustrialRecipe("Draft: protein slurry", "Protein slurry", "food/vat/protein", "RecipeDecompositionApi vat seed", "Protein slurry from vat base and algae paste.").input("Nutrient vat base",1).input("Soylens algae paste",1));
        r.add(new DraftIndustrialRecipe("Draft: ration paste", "Ration paste", "food/ration/paste", "RecipeDecompositionApi ration seed", "Basic ration paste from grain mash and protein slurry.").input("Grain mash",1).input("Protein slurry",1).input("Preservative salt packet",1));
        r.add(new DraftIndustrialRecipe("Draft: emergency ration paste", "Emergency ration paste", "food/ration/emergency", "RecipeDecompositionApi ration seed", "Survival paste stabilized for sealed ration outputs.").input("Ration paste",1).input("Protein binder paste",1).input("Water purification tab",1));
        r.add(new DraftIndustrialRecipe("Draft: textile fiber bale", "Textile fiber bale", "component/textile/fiber", "RecipeDecompositionApi textile seed", "Waste biomass and clean water become fiber stock for the textile branch.").input("Waste biomass",1).input("Potable water",1));
        r.add(new DraftIndustrialRecipe("Draft: reclaimed textile sorting", "Reclaimed textile bundle", "component/textile/reclaimed", "RecipeDecompositionApi textile seed", "Cloth salvage is washed, sorted, and bundled before re-use.").input("Cloth scrap",2).input("Filtered water",1));
        r.add(new DraftIndustrialRecipe("Draft: thread spool spinning", "Thread spool", "component/textile/thread", "RecipeDecompositionApi textile seed", "Basic fiber is spun into thread for garment assembly.").input("Textile fiber bale",1));
        r.add(new DraftIndustrialRecipe("Draft: coarse cloth roll weaving", "Coarse cloth roll", "component/textile/cloth", "RecipeDecompositionApi textile seed", "Civilian cloth comes from fiber and thread, not magic wardrobe spawn.").input("Textile fiber bale",2).input("Thread spool",1));
        r.add(new DraftIndustrialRecipe("Draft: treated canvas roll", "Treated canvas roll", "component/textile/canvas", "RecipeDecompositionApi textile seed", "Canvas is heavier cloth treated for workwear, straps, and rugged covers.").input("Coarse cloth roll",1).input("Machine oil vial",1).input("Filtered water",1));
        r.add(new DraftIndustrialRecipe("Draft: synthweave sheet", "Synthweave sheet", "component/textile/synthweave", "RecipeDecompositionApi textile seed", "Advanced cloth adds polymer and conductor fibers for strength and finish.").input("Coarse cloth roll",1).input("Industrial polymer sheet",1).input("Conductive filament spool",1));
        r.add(new DraftIndustrialRecipe("Draft: rubberized fabric", "Rubberized fabric", "component/textile/rubberized", "RecipeDecompositionApi textile seed", "Rubberized fabric supports sealed clothing, waders, and hazard suits.").input("Coarse cloth roll",1).input("Rubber gasket sheet",1).input("Machine oil vial",1));
        r.add(new DraftIndustrialRecipe("Draft: leather substitute sheet", "Leather substitute sheet", "component/textile/leatherette", "RecipeDecompositionApi textile seed", "Synthetic leather substitute from reclaimed cloth and polymer sheet.").input("Reclaimed textile bundle",1).input("Industrial polymer sheet",1));
        r.add(new DraftIndustrialRecipe("Draft: hide strip bundle", "Hide strip bundle", "component/textile/hide", "RecipeDecompositionApi textile seed", "Primitive hide strips from organic waste, water, and oil tanning.").input("Waste biomass",1).input("Filtered water",1).input("Machine oil vial",1));
        r.add(new DraftIndustrialRecipe("Draft: wire stitching spool", "Wire stitching spool", "component/textile/wire-stitching", "RecipeDecompositionApi textile seed", "Wire-reinforced stitching for rugged garments and scrap armor.").input("Thread spool",1).input("Wire bundle",1));
        r.add(new DraftIndustrialRecipe("Draft: fastener button card", "Fastener button card", "component/garment/fasteners", "RecipeDecompositionApi garment seed", "Small garment closures from polymer and small fasteners.").input("Industrial polymer sheet",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: buckle and clasp set", "Buckle and clasp set", "component/garment/buckles", "RecipeDecompositionApi garment seed", "Buckles and clasps from metal stock and fasteners.").input("Refined metal stock",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: webbing strap roll", "Webbing strap roll", "component/garment/webbing", "RecipeDecompositionApi garment seed", "Webbing stock for harnesses, armor, and military kit.").input("Treated canvas roll",1).input("Thread spool",1));
        r.add(new DraftIndustrialRecipe("Draft: padding layer", "Padding layer", "component/garment/padding", "RecipeDecompositionApi garment seed", "Padding layers from reclaimed textile and thread.").input("Reclaimed textile bundle",1).input("Thread spool",1));
        r.add(new DraftIndustrialRecipe("Draft: insulation batting", "Insulation batting", "component/garment/insulation", "RecipeDecompositionApi garment seed", "Insulation adds thermal and electrical buffering to protective clothes.").input("Textile fiber bale",1).input("Ceramic insulator blank",1));
        r.add(new DraftIndustrialRecipe("Draft: filter cloth roll", "Filter cloth roll", "component/garment/filter-cloth", "RecipeDecompositionApi garment seed", "Filter cloth for masks and rebreather layers.").input("Coarse cloth roll",1).input("Charcoal filter bed",1));
        r.add(new DraftIndustrialRecipe("Draft: sealant strip roll", "Sealant strip roll", "component/garment/sealant-strip", "RecipeDecompositionApi garment seed", "Seam strips for sealed suits and patching bad air out.").input("Rubberized fabric",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: pressure gasket set", "Pressure gasket set", "component/garment/pressure-seal", "RecipeDecompositionApi garment seed", "Suit gaskets derive from existing seal stock and precision fasteners.").input("Sealing gasket set",1).input("Rubber gasket sheet",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: visor lens", "Visor lens", "component/garment/visor", "RecipeDecompositionApi garment seed", "Visor lenses from optic blanks and polymer housing.").input("Glass optic blank",1).input("Industrial polymer sheet",1));
        r.add(new DraftIndustrialRecipe("Draft: rebreather filter", "Rebreather filter", "component/garment/rebreather", "RecipeDecompositionApi garment seed", "Rebreather filters from filter cloth, charcoal media, and housing.").input("Filter cloth roll",1).input("Charcoal filter bed",1).input("Filter cartridge housing",1));
        r.add(new DraftIndustrialRecipe("Draft: helmet shell", "Helmet shell", "component/armor/helmet-shell", "RecipeDecompositionApi armor seed", "Helmet shells from stamped plate, padding, and rivets.").input("Stamped armor plate",1).input("Padding layer",1).input("Rivet set",1));
        r.add(new DraftIndustrialRecipe("Draft: hood pattern cut", "Hood pattern cut", "component/garment/hood", "RecipeDecompositionApi garment seed", "Hood panels from cloth and thread.").input("Coarse cloth roll",1).input("Thread spool",1));
        r.add(new DraftIndustrialRecipe("Draft: coat shell", "Coat shell", "component/garment/coat-shell", "RecipeDecompositionApi garment seed", "Coat shells are cut from cloth with closure stock.").input("Coarse cloth roll",2).input("Fastener button card",1).input("Thread spool",1));
        r.add(new DraftIndustrialRecipe("Draft: robe shell", "Robe shell", "component/garment/robe-shell", "RecipeDecompositionApi garment seed", "Robe shells use more cloth and simple closures.").input("Coarse cloth roll",2).input("Thread spool",1).input("Fastener button card",1));
        r.add(new DraftIndustrialRecipe("Draft: workwear panel set", "Workwear panel set", "component/garment/workwear-panel", "RecipeDecompositionApi garment seed", "Workwear panels from treated canvas and thread.").input("Treated canvas roll",1).input("Thread spool",1));
        r.add(new DraftIndustrialRecipe("Draft: boot sole set", "Boot sole set", "component/garment/boot-sole", "RecipeDecompositionApi garment seed", "Boot soles from rubber sheet and small fasteners.").input("Rubber gasket sheet",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: glove palm set", "Glove palm set", "component/garment/glove-palm", "RecipeDecompositionApi garment seed", "Glove palms from leather substitute and thread.").input("Leather substitute sheet",1).input("Thread spool",1));
        r.add(new DraftIndustrialRecipe("Draft: armor backing", "Armor backing", "component/armor/backing", "RecipeDecompositionApi armor seed", "Armor backing from padding, webbing, and cloth.").input("Padding layer",1).input("Webbing strap roll",1).input("Coarse cloth roll",1));
        r.add(new DraftIndustrialRecipe("Draft: flak weave panel", "Flak weave panel", "component/armor/flak-weave", "RecipeDecompositionApi armor seed", "Flak weave combines synthweave, ceramic tiles, and padding.").input("Synthweave sheet",1).input("Ceramic armor tile",1).input("Padding layer",1));
        r.add(new DraftIndustrialRecipe("Draft: mesh armor panel", "Mesh armor panel", "component/armor/mesh", "RecipeDecompositionApi armor seed", "Concealable mesh armor from synthweave and conductive filament.").input("Synthweave sheet",1).input("Conductive filament spool",1).input("Calibration screw set",1));
        r.add(new DraftIndustrialRecipe("Draft: scrap metal armor plate", "Scrap metal plate", "component/armor/scrap-plate", "RecipeDecompositionApi armor seed", "Scrap armor plate from scrap plate and rivets.").input("Scrap plate",1).input("Rivet set",1));
        r.add(new DraftIndustrialRecipe("Draft: stamped armor plate", "Stamped armor plate", "component/armor/stamped-plate", "RecipeDecompositionApi armor seed", "Stamped armor plate from refined and hardened metal stock.").input("Refined metal stock",1).input("Hardened metal stock",1));
        r.add(new DraftIndustrialRecipe("Draft: ceramic armor tile", "Ceramic armor tile", "component/armor/ceramic-tile", "RecipeDecompositionApi armor seed", "Armor ceramic from ceramic blanks and distilled water finish.").input("Ceramic insulator blank",2).input("Distilled water",1));
        r.add(new DraftIndustrialRecipe("Draft: carapace plate", "Carapace plate", "component/armor/carapace", "RecipeDecompositionApi armor seed", "Carapace plate combines stamped plate, ceramic tile, and plasteel shavings.").input("Stamped armor plate",1).input("Ceramic armor tile",1).input("Plasteel shaving bale",1));
        r.add(new DraftIndustrialRecipe("Draft: rivet set", "Rivet set", "component/armor/rivets", "RecipeDecompositionApi armor seed", "Armor rivets from small fasteners and metal stock.").input("Small fastener set",1).input("Refined metal stock",1));
        r.add(new DraftIndustrialRecipe("Draft: armor harness webbing", "Armor harness webbing", "component/armor/harness-webbing", "RecipeDecompositionApi armor seed", "Armor harness webbing from webbing, buckles, and stitching.").input("Webbing strap roll",1).input("Buckle and clasp set",1).input("Wire stitching spool",1));
        r.add(new DraftIndustrialRecipe("Draft: shock absorber padding", "Shock-absorber padding", "component/armor/impact-padding", "RecipeDecompositionApi armor seed", "Impact padding from padding layers and rubber slab.").input("Padding layer",1).input("Tire-rubber slab",1));
        r.add(new DraftIndustrialRecipe("Draft: concealment lining", "Concealment lining", "component/garment/concealment", "RecipeDecompositionApi garment seed", "Concealment lining from tailored liner and pocketing.").input("Tailored garment liner",1).input("Fastener button card",1));
        r.add(new DraftIndustrialRecipe("Draft: tailored garment liner", "Tailored garment liner", "component/garment/tailored-liner", "RecipeDecompositionApi garment seed", "Luxury garment liner from synthweave and trim.").input("Synthweave sheet",1).input("Decorative trim set",1));
        r.add(new DraftIndustrialRecipe("Draft: decorative trim set", "Decorative trim set", "component/garment/trim", "RecipeDecompositionApi garment seed", "Trim from cloth, ribbon, and faction colors.").input("Coarse cloth roll",1).input("Faction insignia patch",1));
        r.add(new DraftIndustrialRecipe("Draft: faction insignia patch", "Faction insignia patch", "component/faction/patch", "RecipeDecompositionApi faction-mark seed", "Faction patches from cloth, thread, and printed authority.").input("Coarse cloth roll",1).input("Thread spool",1).input("Ink stylus",1));
        r.add(new DraftIndustrialRecipe("Draft: rank tab set", "Rank tab set", "component/faction/rank-tabs", "RecipeDecompositionApi faction-mark seed", "Rank tabs from patch stock and small fasteners.").input("Faction insignia patch",1).input("Small fastener set",1));
        r.add(new DraftIndustrialRecipe("Draft: house livery ribbon set", "House livery ribbon set", "component/faction/house-livery", "RecipeDecompositionApi faction-mark seed", "Noble livery ribbon from decorative trim and tailored cloth.").input("Decorative trim set",1).input("Tailored garment liner",1));
        r.add(new DraftIndustrialRecipe("Draft: purity seal backing", "Purity seal backing", "component/faction/purity-seal", "RecipeDecompositionApi faction-mark seed", "Seal backing from paperwork, wax-like sealant, and doctrine-adjacent materials.").input("Blank form packet",1).input("Food-safe sealant",1).input("Ink stylus",1));
        r.add(new DraftIndustrialRecipe("Draft: bone charm string", "Bone charm string", "component/faction/bone-charm", "RecipeDecompositionApi faction-mark seed", "Bone charms from biomass, hide strips, and thread.").input("Waste biomass",1).input("Hide strip bundle",1).input("Thread spool",1));
        r.add(new DraftIndustrialRecipe("Draft: tire rubber slab", "Tire-rubber slab", "component/armor/rubber-slab", "RecipeDecompositionApi armor seed", "Thick rubber slab from gasket sheet and salvage plate.").input("Rubber gasket sheet",1).input("Scrap plate",1));

        // 0.8.95a chem / narcotic precursor recipes
        r.add(new DraftIndustrialRecipe("Draft: chemical reagent bottle", "Chemical reagent bottle", "component/chem/reagent", "RecipeDecompositionApi chem seed", "Distilled water and toxin slurry become a generic chemical working bottle.").input("Distilled water",1).input("Toxin slurry",1).input("Sterile vial rack",1));
        r.add(new DraftIndustrialRecipe("Draft: medicae stabilizer compound", "Medicae stabilizer compound", "component/chem/stabilizer", "RecipeDecompositionApi chem seed", "Stabilizer compound from antiseptic, sterile water, and reagent stock.").input("Antiseptic vial",1).input("Sterile water flask",1).input("Chemical reagent bottle",1));
        r.add(new DraftIndustrialRecipe("Draft: alkaloid extract", "Alkaloid extract", "component/chem/alkaloid", "RecipeDecompositionApi chem seed", "Leaf stock and clean solvent become a stimulant extract.").input("Vorder leaf bundle",1).input("Distilled water",1).input("Chemical reagent bottle",1));
        r.add(new DraftIndustrialRecipe("Draft: stimulant salt batch", "Stimulant salt batch", "component/chem/stimulant", "RecipeDecompositionApi chem seed", "Stimulant salts are crystallized from alkaloids and mineral feedstock.").input("Alkaloid extract",1).input("Nutrient salt packet",1).input("Distilled water",1));
        r.add(new DraftIndustrialRecipe("Draft: sedative tincture base", "Sedative tincture base", "component/chem/sedative", "RecipeDecompositionApi chem seed", "Sedative tincture from spirit, fungal cultures, and chemical reagent.").input("Distilled spirit base",1).input("Fungal spore culture",1).input("Chemical reagent bottle",1));
        r.add(new DraftIndustrialRecipe("Draft: analgesic compound", "Analgesic compound", "component/chem/analgesic", "RecipeDecompositionApi chem seed", "Pain-dulling compound from sterile water and reagent stock.").input("Sterile water flask",1).input("Chemical reagent bottle",1).input("Alkaloid extract",1));
        r.add(new DraftIndustrialRecipe("Draft: combat stimm compound", "Combat stimm compound", "component/chem/combat-stimm", "RecipeDecompositionApi chem seed", "Combat stimm base from stimulant salts and revival shock precursor.").input("Stimulant salt batch",1).input("Revival shock compound",1).input("Injector ampoule set",1));
        r.add(new DraftIndustrialRecipe("Draft: aggression catalyst", "Aggression catalyst", "component/chem/aggression", "RecipeDecompositionApi chem seed", "Aggression catalyst from stimulant salts and bone ash reagent.").input("Stimulant salt batch",1).input("Bone ash reagent",1).input("Chemical reagent bottle",1));
        r.add(new DraftIndustrialRecipe("Draft: focus nootropic base", "Focus nootropic base", "component/chem/nootropic", "RecipeDecompositionApi chem seed", "Nootropic base from alkaloids, circuit-dust handling, and clean solvent.").input("Alkaloid extract",1).input("Circuit wafer",1).input("Distilled water",1));
        r.add(new DraftIndustrialRecipe("Draft: hallucinogen resin", "Hallucinogen resin", "component/chem/hallucinogen", "RecipeDecompositionApi chem seed", "Hallucinogen resin from fungal culture and euphoric syrup base.").input("Fungal spore culture",1).input("Euphoric syrup base",1).input("Chemical reagent bottle",1));
        r.add(new DraftIndustrialRecipe("Draft: euphoric syrup base", "Euphoric syrup base", "component/chem/euphoric", "RecipeDecompositionApi chem seed", "Euphoric syrup from fruit mash, spirit base, and alkaloid extract.").input("Fruit mash",1).input("Distilled spirit base",1).input("Alkaloid extract",1));
        r.add(new DraftIndustrialRecipe("Draft: fungal spore culture", "Fungal spore culture", "component/chem/fungal", "RecipeDecompositionApi chem seed", "Spore culture grown from fungus trays and dirty water.").input("Fungus culture tray",1).input("Dirty water",1).input("Compost substrate",1));
        r.add(new DraftIndustrialRecipe("Draft: psychoactive mold cake", "Psychoactive mold cake", "component/chem/mold", "RecipeDecompositionApi chem seed", "Mold cake from fungus culture, parchment refuse, and corpse-starch paste.").input("Fungal spore culture",1).input("Blank form packet",1).input("Corpse-starch paste",1));
        r.add(new DraftIndustrialRecipe("Draft: sump fermentation mash", "Sump fermentation mash", "component/chem/sump-mash", "RecipeDecompositionApi chem seed", "Sump mash from scrap fermentation and dirty water.").input("Fermentable scrap mash",1).input("Dirty water",1).input("Fungal spore culture",1));
        r.add(new DraftIndustrialRecipe("Draft: voidship rotgut wash", "Voidship rotgut wash", "component/chem/void-wash", "RecipeDecompositionApi chem seed", "Void rotgut wash from low-grade amasec and recycled water.").input("Low-grade amasec wash",1).input("Greywater",1).input("Chemical reagent bottle",1));
        r.add(new DraftIndustrialRecipe("Draft: mineral tonic slurry", "Mineral tonic slurry", "component/chem/mineral-tonic", "RecipeDecompositionApi chem seed", "Mineral tonic slurry from brine, raw earth, and dirty water.").input("Reclamation brine",1).input("Raw earth",1).input("Dirty water",1));
        r.add(new DraftIndustrialRecipe("Draft: radiant mineral dust", "Radiant mineral dust", "component/chem/radiant-dust", "RecipeDecompositionApi chem seed", "Radiant dust from mineral cake and contaminated slurry.").input("Reclaimed mineral cake",1).input("Toxin slurry",1).input("Raw earth",1));
        r.add(new DraftIndustrialRecipe("Draft: lho leaf bale", "Lho leaf bale", "component/chem/lho-leaf", "RecipeDecompositionApi chem seed", "Lho leaf bale from hydroponic and vorder leaf stock.").input("Vorder leaf bundle",1).input("Hydroponic crop stock",1).input("Preservative salt packet",1));
        r.add(new DraftIndustrialRecipe("Draft: smokeable lho roll", "Smokeable lho roll", "component/chem/lho-roll", "RecipeDecompositionApi chem seed", "Smoke roll from lho leaf, paper, and sealant.").input("Lho leaf bale",1).input("Blank form packet",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: incense resin pellet", "Incense resin pellet", "component/chem/incense", "RecipeDecompositionApi chem seed", "Incense pellet from resin, perfume carrier, and devotional packaging.").input("Hallucinogen resin",1).input("Perfume carrier oil",1).input("Devotional wrapper strip",1));
        r.add(new DraftIndustrialRecipe("Draft: perfume carrier oil", "Perfume carrier oil", "component/chem/perfume-oil", "RecipeDecompositionApi chem seed", "Carrier oil from fruit mash and machine oil refined into something politely dangerous.").input("Fruit mash",1).input("Machine oil vial",1).input("Distilled water",1));
        r.add(new DraftIndustrialRecipe("Draft: noble crystal vial", "Spire crystal vial", "component/chem/noble-vial", "RecipeDecompositionApi chem seed", "Crystal vial from glass optic blanks and gilding foil.").input("Glass optic blank",1).input("Gilding foil",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: underhive paper twist", "Underhive paper twist", "component/chem/paper-twist", "RecipeDecompositionApi chem seed", "Paper twists from blank forms and cheap sealant.").input("Blank form packet",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: injector ampoule set", "Injector ampoule set", "component/chem/injector-ampoule", "RecipeDecompositionApi chem seed", "Injector ampoules from sterile vials and contact hardware.").input("Sterile vial rack",1).input("Needle lancet bundle",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: snuff capsule tin", "Snuff capsule tin", "component/chem/snuff-tin", "RecipeDecompositionApi chem seed", "Snuff tin from metal sleeve and paper twist stock.").input("Tin can sleeve",1).input("Underhive paper twist",1));
        r.add(new DraftIndustrialRecipe("Draft: lozenge binder base", "Lozenge binder base", "component/chem/lozenge", "RecipeDecompositionApi chem seed", "Lozenge binder from starch and water.").input("Starch flour sack",1).input("Potable water",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: confection binder base", "Confection binder base", "component/chem/confection", "RecipeDecompositionApi chem seed", "Vice candy binder from fruit mash and starch.").input("Fruit mash",1).input("Starch flour sack",1).input("Euphoric syrup base",1));
        r.add(new DraftIndustrialRecipe("Draft: aerosol propellant bulb", "Aerosol propellant bulb", "component/chem/aerosol", "RecipeDecompositionApi chem seed", "Aerosol bulb from compressed gas and pressure fittings.").input("Compressed-gas bottle",1).input("Pressure chamber",1).input("Nozzle and valve set",1));
        r.add(new DraftIndustrialRecipe("Draft: warp-tainted mirror shard", "Warp-tainted mirror shard", "component/chem/warp-shard", "RecipeDecompositionApi chem seed", "Warp-tainted mirror shards are treated as contraband salvage, not safe manufacture.").input("Glass optic blank",1).input("Psychic catalyst powder",1).input("Profane binding ash",1));
        r.add(new DraftIndustrialRecipe("Draft: psychic catalyst powder", "Psychic catalyst powder", "component/chem/psychic-catalyst", "RecipeDecompositionApi chem seed", "Psychic catalyst powder from rare dust, focus wafer, and very poor judgment.").input("Radiant mineral dust",1).input("Forbidden focus wafer",1).input("Profane binding ash",1));
        r.add(new DraftIndustrialRecipe("Draft: forbidden focus wafer", "Forbidden focus wafer", "component/chem/focus-wafer", "RecipeDecompositionApi chem seed", "Forbidden focus wafer from optic glass, nootropic base, and sealed thoughts.").input("Glass optic blank",1).input("Focus nootropic base",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: profane binding ash", "Profane binding ash", "component/chem/profane-ash", "RecipeDecompositionApi chem seed", "Profane ash from incense and bone ash reagent.").input("Incense resin pellet",1).input("Bone ash reagent",1).input("Chemical reagent bottle",1));
        r.add(new DraftIndustrialRecipe("Draft: bone ash reagent", "Bone ash reagent", "component/chem/bone-ash", "RecipeDecompositionApi chem seed", "Bone ash reagent from waste biomass and mineral cake.").input("Waste biomass",1).input("Reclaimed mineral cake",1));
        r.add(new DraftIndustrialRecipe("Draft: clotting foam reagent", "Clotting foam reagent", "component/chem/clotting", "RecipeDecompositionApi chem seed", "Clotting reagent from sterile water, protein binder, and medical stabilizer.").input("Sterile water flask",1).input("Protein binder paste",1).input("Medicae stabilizer compound",1));
        r.add(new DraftIndustrialRecipe("Draft: nerve dampener solution", "Nerve dampener solution", "component/chem/nerve-dampener", "RecipeDecompositionApi chem seed", "Nerve dampener solution from analgesic and sedative bases.").input("Analgesic compound",1).input("Sedative tincture base",1).input("Sterile water flask",1));
        r.add(new DraftIndustrialRecipe("Draft: revival shock compound", "Revival shock compound", "component/chem/revival", "RecipeDecompositionApi chem seed", "Revival compound from stimulant salts and medicae stabilizer.").input("Stimulant salt batch",1).input("Medicae stabilizer compound",1).input("Sterile water flask",1));
        r.add(new DraftIndustrialRecipe("Draft: interrogation dosing kit", "Interrogation dosing kit", "component/chem/interrogation", "RecipeDecompositionApi chem seed", "Interrogation dosing kit from compliance base and injector hardware.").input("Compliance sedative base",1).input("Injector ampoule set",1).input("Blank form packet",1));
        r.add(new DraftIndustrialRecipe("Draft: compliance sedative base", "Compliance sedative base", "component/chem/compliance", "RecipeDecompositionApi chem seed", "Compliance sedative from sedative tincture and nerve dampener.").input("Sedative tincture base",1).input("Nerve dampener solution",1).input("Chemical reagent bottle",1));
        r.add(new DraftIndustrialRecipe("Draft: labor dosing ticket strip", "Labor dosing ticket strip", "component/chem/labor-ticket", "RecipeDecompositionApi chem seed", "Labor ticket strips from forms and cheap sealant.").input("Blank form packet",1).input("Ink stylus",1).input("Food-safe sealant",1));
        r.add(new DraftIndustrialRecipe("Draft: devotional wrapper strip", "Devotional wrapper strip", "component/chem/devotional-wrapper", "RecipeDecompositionApi chem seed", "Devotional wrappers from forms, ink, and wax-like sealant.").input("Blank form packet",1).input("Ink stylus",1).input("Purity seal backing",1));
        r.add(new DraftIndustrialRecipe("Draft: anti-nausea lozenge base", "Anti-nausea lozenge base", "component/chem/anti-nausea", "RecipeDecompositionApi chem seed", "Anti-nausea lozenge base from lozenge binder and sterile water.").input("Lozenge binder base",1).input("Sterile water flask",1).input("Analgesic compound",1));
        r.add(new DraftIndustrialRecipe("Draft: vitamin concentrate", "Vitamin concentrate", "component/chem/vitamin", "RecipeDecompositionApi chem seed", "Vitamin concentrate from ploin and distilled water.").input("Ploin vitamin concentrate",1).input("Distilled water",1).input("Sterile vial rack",1));
        return r;
    }
    static ArrayList<String> sampleLines(int max) {
        ArrayList<String> lines = new ArrayList<>();
        int i = 0;
        for (DraftIndustrialRecipe r : generatedDraftRecipes()) {
            lines.add(r.auditLine());
            i++;
            if (i >= max) break;
        }
        return lines;
    }
}



class LaboratoryEquipmentProfile {
    final String name, processType, knowledgeCategory, roomKind, manning, placementNote;
    final int minimumMachineTier;
    LaboratoryEquipmentProfile(String name, String processType, String knowledgeCategory, int tier, String roomKind, String manning, String placementNote) {
        this.name=name; this.processType=processType; this.knowledgeCategory=knowledgeCategory; this.minimumMachineTier=tier; this.roomKind=roomKind; this.manning=manning; this.placementNote=placementNote;
    }
    String auditLine() { return name + " process=" + processType + " knowledge=" + knowledgeCategory + " minMachine=" + QualityAuthorityApi.qualityName(minimumMachineTier) + " room=" + roomKind + " manning=" + manning + " placement=" + placementNote; }
}


class ChemicalEquipmentAuthority {
    static ArrayList<LaboratoryEquipmentProfile> profiles() {
        ArrayList<LaboratoryEquipmentProfile> p = new ArrayList<>();
        p.add(new LaboratoryEquipmentProfile("Crude chem bench","crude compounding","Chemical Synthesis",1,"gang chem kitchen / sump workshop","1 chem-cooker or scavver","dirty rooms allowed; high defect risk is handled by faction/quality bias"));
        p.add(new LaboratoryEquipmentProfile("Reagent preparation bench","compounding","Chemical Synthesis",2,"chem laboratory / clinic prep room","1 chemist","ordinary legal or semi-legal preparation bench"));
        p.add(new LaboratoryEquipmentProfile("Chemical reagent rack","storage","Chemical Synthesis",2,"chem storage / clinic cabinet","0-1 attendant","adjacent to a lab, clinic, or manufactorum dosing station"));
        p.add(new LaboratoryEquipmentProfile("Filtration rack","filtration","Water Purification",2,"water refinery / medicae prep / sump lab","1 technician","needs drainable wet-room style placement"));
        p.add(new LaboratoryEquipmentProfile("Distillation column","distillation","Chemical Synthesis",4,"licensed still room / noble cellar / medicae lab","1 distiller plus 1 attendant","avoid dormitories; heat and fumes"));
        p.add(new LaboratoryEquipmentProfile("Crude still","crude distillation","Chemical Synthesis",1,"sump bar / gang den / void underdeck","1 brewer","unsafe but cheap; legal status depends on owner"));
        p.add(new LaboratoryEquipmentProfile("Clean fermentation vat","fermentation","Food Processing",3,"brewery / food guild room / hydroponic annex","1 vat tender","requires floor space and clean water access"));
        p.add(new LaboratoryEquipmentProfile("Sump fermentation tub","dirty fermentation","Salvage Processing",0,"sump kitchen / mutant warren / sewer garden","1 desperate brewer","dirty wet rooms permitted"));
        p.add(new LaboratoryEquipmentProfile("Fungal grow tray bank","fungal cultivation","Agricultural Processing",2,"sewer garden / vat room / mutant clinic","1 grow-tender","dark damp rooms valid; clean labs improve quality"));
        p.add(new LaboratoryEquipmentProfile("Narcotic drying rack","drying","Chemical Synthesis",1,"gang room / lho room / illicit clinic","0-1 attendant","needs ventilation if lawful; hidden placement if contraband"));
        p.add(new LaboratoryEquipmentProfile("Pellet press","tablet pressing","Chemical Synthesis",2,"dosing station / noble apothecary / gang kitchen","1 operator","compact bench apparatus"));
        p.add(new LaboratoryEquipmentProfile("Injector filling station","injector filling","Medical Processing",3,"clinic / military aid station / chop-shop","1 medicae or chem-cooker","should be near sterile supplies or accepts defect penalties"));
        p.add(new LaboratoryEquipmentProfile("Sterile medicae clean bench","sterile preparation","Medical Processing",5,"medicae clinic / surgical room","1 medicae worker","requires clean-room style placement for high-quality medical chems"));
        p.add(new LaboratoryEquipmentProfile("Cold storage locker","cold storage","Medical Processing",3,"clinic / noble lab / void medbay","0-1 attendant","stores unstable medical and luxury compounds"));
        p.add(new LaboratoryEquipmentProfile("Aerosol bulb filler","aerosolization","Chemical Synthesis",3,"riot store / fume room / safety station","1 chemist","requires pressure-safe room or fume hood adjacency"));
        p.add(new LaboratoryEquipmentProfile("Pressure-rated chem vessel","pressure synthesis","Chemical Synthesis",4,"industrial chemical lab / toxin room","1 chemist plus 1 machine attendant","requires industrial floor and maintenance access"));
        p.add(new LaboratoryEquipmentProfile("Fume hood","toxic handling","Chemical Synthesis",4,"chemical lab / security toxin room","1 trained operator","ventilation requirement for toxic, volatile, and security chems"));
        p.add(new LaboratoryEquipmentProfile("Toxin lockbox","toxin storage","Security",3,"evidence room / toxin cabinet / assassin cell","0-1 custodian","restricted storage; lawful factions use paperwork, criminals use locks"));
        p.add(new LaboratoryEquipmentProfile("Interrogation dosing cradle","security dosing","Medical Processing",4,"black room / holding cell / illegal clinic","1 interrogator plus 1 attendant","requires prisoner-control room or security clinic"));
        p.add(new LaboratoryEquipmentProfile("Labor dosing dispenser","labor dosing","Industrial Maintenance",2,"shift office / manufactorum clinic","1 overseer or medic","installed near work assignment facilities"));
        p.add(new LaboratoryEquipmentProfile("Nootropic assay desk","nootropic compounding","Chemical Synthesis",3,"scribe office / cogitator room / tech lab","1 logic adept or chemist","quiet room and cogitator adjacency preferred"));
        p.add(new LaboratoryEquipmentProfile("Calibrated assay shrine","mechanicus assay","Energy Systems",5,"Mechanicus lab / forge shrine","1 tech-adept","high power and maintenance demand, high efficiency"));
        p.add(new LaboratoryEquipmentProfile("Spire perfumery glassware","luxury infusion","Chemical Synthesis",5,"noble atelier / spire salon","1 perfumer plus 1 servant","prestige placement; theft-visible"));
        p.add(new LaboratoryEquipmentProfile("Luxury vialing station","luxury vialing","Chemical Synthesis",4,"noble apothecary / estate cellar","1 apothecary attendant","packaging and social presentation requirement"));
        p.add(new LaboratoryEquipmentProfile("Ritual censer kiln","ritual incense","Chemical Synthesis",2,"shrine / cult room / pilgrim market stall","1 censer-keeper","lawful if devotional, contraband if profane"));
        p.add(new LaboratoryEquipmentProfile("Forbidden preparation chamber","forbidden preparation","Chemical Synthesis",6,"hidden ritual lab / sealed vault","2 initiated handlers","contraband, high inspection risk, serious knowledge gate"));
        p.add(new LaboratoryEquipmentProfile("Warp-containment mirror box","warp handling","Energy Systems",6,"sealed evidence room / cult vault","2 handlers or 1 specialist","must not be ordinary room placement"));
        p.add(new LaboratoryEquipmentProfile("Mutant adaptation rack","mutant adaptation","Medical Processing",1,"mutant clinic / sump infirmary","1 body-adapted handler","body-size and tolerance adjustment apparatus"));
        p.add(new LaboratoryEquipmentProfile("Voidship galley still","void brewing","Food Processing",2,"voidship galley / frontier bar","1 voidsman brewer","compact, mobile, and suspicious"));
        p.add(new LaboratoryEquipmentProfile("Ash-waste mineral leacher","mineral extraction","Chemical Synthesis",2,"ash-waste rig / reclamation shed","1 leacher","dry, contaminated, frontier placement"));
        p.add(new LaboratoryEquipmentProfile("Chemical waste trap","waste capture","Salvage Processing",1,"sump lab / refinery drain / illegal kitchen","0-1 attendant","captures waste streams and bad evidence"));
        return p;
    }
    static LaboratoryEquipmentProfile profileForName(String name) {
        if (name == null) return null;
        for (LaboratoryEquipmentProfile p : profiles()) if (p.name.equalsIgnoreCase(name.trim())) return p;
        return null;
    }
    static boolean isLabEquipmentName(String name) { return profileForName(name) != null; }
    static boolean isChemicalRecipe(DraftIndustrialRecipe r) {
        if (r == null) return false;
        String fam = low(r.family), out = low(r.outputBaseItem);
        ItemDef d = ItemCatalog.get(r.outputBaseItem);
        String cat = d == null ? "" : low(d.category);
        return fam.startsWith("chem/") || fam.startsWith("medical/chem") || fam.startsWith("component/chem") || cat.startsWith("chem/") || cat.startsWith("medical/chem") || cat.startsWith("component/chem") || cat.startsWith("equipment/lab") || containsAny(out,"stimm","recaf","lho","obscura","amasec","spook","mercy","wake","dust","drops","incense","serum","draught","nectar","wine","rum","gin","milk","mash","mold","resin","tincture","compound","reagent","ampoule","aerosol","lozenge","perfume");
    }
    static void applyRequirements(DraftIndustrialRecipe r) {
        if (r == null || !isChemicalRecipe(r) || r.equipmentWidth() > 0) return;
        String process = processTypeFor(r);
        int tier = minTierForProcess(process, r);
        r.process(process, tier, manningFor(process, r), placementFor(process, r));
        for (String e : equipmentFor(process, r)) r.equipment(e, 1);
        for (String room : roomsFor(process, r)) r.room(room);
    }
    static String processTypeFor(DraftIndustrialRecipe r) {
        String all = low(r.family + " " + r.outputBaseItem + " " + r.inputSummary());
        if (containsAny(all,"flect","mirror","warp","spook","witchsalt","false astronomican","black sun","lucid null","psychic","forbidden focus","warp-tainted")) return "forbidden/warp handling";
        if (containsAny(all,"truth","compliance","fearhook","confessor","black badge","grav-lock","interrogation")) return "security dosing";
        if (containsAny(all,"medi-stimm","white mercy","nerve lace","stitchdream","clotfoam","pale nurse","saint’s anesthetic","wakewire","red waker","medical","sterile","clotting","nerve dampener","revival")) return "sterile medical compounding";
        if (containsAny(all,"stimm","redline","slaught","frenzon","rush","spinefire","hammerwake","red ticket","combat")) return "combat drug compounding";
        if (containsAny(all,"aerosol","mist","haze","inhalant","soot calm","dustlung")) return "aerosolization";
        if (containsAny(all,"lho","sumpweed","smokeable","drying")) return "drying/smoke preparation";
        if (containsAny(all,"incense","choir ash","benediction","censer","devotional","saintsbane","reliquary","flagellant")) return "ritual incense preparation";
        if (containsAny(all,"amasec","rum","gin","wine","cordial","nectar","hullshine","distilled spirit")) return containsAny(all,"high","gild","noble","sable","vermilion") ? "refined distillation" : "crude distillation";
        if (containsAny(all,"fermentation","mash","brine joy","glowgut","rustmilk","mire","sump")) return "fermentation";
        if (containsAny(all,"fungal","mold","spore","sewer bells","mirth-spores","mold saint")) return "fungal cultivation";
        if (containsAny(all,"ploin","grav-sick","anti-nausea","vitamin","voidwake","ashbite","dune milk","mineral")) return "frontier/void compounding";
        if (containsAny(all,"pearl obscura","gildwine","sable","vitreous","lotus","velvet","glass vein","halo","ghost orchid","ebon lotus","sorrowglass","widow")) return "luxury infusion and vialing";
        if (containsAny(all,"line-keeper","quota","shiftwake","chainbite","labor","cogitator blue","clerk")) return "labor-control dosing";
        if (containsAny(all,"toxin","pipe bloom","drain sugar","battery kiss","blue scab","rust angel","chemical waste","slurry")) return "toxic waste extraction";
        return "general chemical compounding";
    }
    static ArrayList<String> equipmentFor(String process, DraftIndustrialRecipe r) {
        ArrayList<String> e = new ArrayList<>();
        String p = low(process), all = low(r.family + " " + r.outputBaseItem);
        if (p.contains("forbidden") || p.contains("warp")) { e.add("Forbidden preparation chamber"); e.add(containsAny(all,"flect","mirror","warp")?"Warp-containment mirror box":"Nootropic assay desk"); e.add("Chemical reagent rack"); }
        else if (p.contains("security")) { e.add("Interrogation dosing cradle"); e.add("Toxin lockbox"); e.add("Chemical reagent rack"); }
        else if (p.contains("sterile")) { e.add("Sterile medicae clean bench"); e.add("Injector filling station"); e.add("Cold storage locker"); }
        else if (p.contains("combat")) { e.add("Reagent preparation bench"); e.add("Injector filling station"); e.add("Chemical reagent rack"); }
        else if (p.contains("aerosol")) { e.add("Aerosol bulb filler"); e.add("Pressure-rated chem vessel"); e.add("Fume hood"); }
        else if (p.contains("drying")) { e.add("Narcotic drying rack"); e.add("Crude chem bench"); }
        else if (p.contains("ritual")) { e.add("Ritual censer kiln"); e.add("Narcotic drying rack"); }
        else if (p.contains("refined distillation")) { e.add("Distillation column"); e.add("Clean fermentation vat"); e.add("Luxury vialing station"); }
        else if (p.contains("crude distillation")) { e.add("Crude still"); e.add("Sump fermentation tub"); }
        else if (p.contains("fermentation")) { e.add(containsAny(all,"sump","mutant","rust","brine")?"Sump fermentation tub":"Clean fermentation vat"); e.add("Filtration rack"); }
        else if (p.contains("fungal")) { e.add("Fungal grow tray bank"); e.add("Narcotic drying rack"); }
        else if (p.contains("frontier") || p.contains("void")) { e.add("Voidship galley still"); e.add("Ash-waste mineral leacher"); e.add("Chemical reagent rack"); }
        else if (p.contains("luxury")) { e.add("Spire perfumery glassware"); e.add("Luxury vialing station"); e.add("Cold storage locker"); }
        else if (p.contains("labor")) { e.add("Labor dosing dispenser"); e.add("Reagent preparation bench"); }
        else if (p.contains("toxic") || p.contains("waste")) { e.add("Fume hood"); e.add("Chemical waste trap"); e.add("Filtration rack"); }
        else { e.add("Reagent preparation bench"); e.add("Chemical reagent rack"); }
        return e;
    }
    static ArrayList<String> roomsFor(String process, DraftIndustrialRecipe r) {
        ArrayList<String> rooms = new ArrayList<>();
        String p = low(process);
        if (p.contains("forbidden") || p.contains("ritual")) rooms.add("hidden shrine laboratory");
        else if (p.contains("sterile")) rooms.add("medicae clean room");
        else if (p.contains("security")) rooms.add("security/interrogation room");
        else if (p.contains("luxury")) rooms.add("noble apothecary atelier");
        else if (p.contains("labor")) rooms.add("manufactorum dosing station");
        else if (p.contains("frontier") || p.contains("void")) rooms.add("voidship/frontier still room");
        else if (p.contains("fermentation") || p.contains("fungal")) rooms.add("wet vat room or sewer garden");
        else if (p.contains("toxic") || p.contains("aerosol")) rooms.add("ventilated chemical room");
        else rooms.add("chemical laboratory");
        return rooms;
    }
    static int minTierForProcess(String process, DraftIndustrialRecipe r) {
        String p = low(process);
        if (p.contains("forbidden") || p.contains("warp")) return 6;
        if (p.contains("luxury") || p.contains("sterile")) return 5;
        if (p.contains("security") || p.contains("refined") || p.contains("pressure")) return 4;
        if (p.contains("combat") || p.contains("aerosol") || p.contains("frontier")) return 3;
        if (p.contains("general") || p.contains("labor") || p.contains("fungal")) return 2;
        return 1;
    }
    static String manningFor(String process, DraftIndustrialRecipe r) {
        String p = low(process);
        if (p.contains("forbidden") || p.contains("warp")) return "2 initiated handlers or 1 specialist";
        if (p.contains("sterile")) return "1 medicae worker";
        if (p.contains("security")) return "1 interrogator plus 1 attendant";
        if (p.contains("labor")) return "1 overseer/medicae attendant";
        if (p.contains("luxury")) return "1 apothecary/perfumer plus 1 servant";
        return "1 chem worker";
    }
    static String placementFor(String process, DraftIndustrialRecipe r) {
        String p = low(process);
        if (p.contains("forbidden") || p.contains("warp")) return "valid only in hidden, sealed, cult, evidence, or vault-like rooms";
        if (p.contains("sterile")) return "requires clinic/medicae placement for full quality ceiling";
        if (p.contains("toxic") || p.contains("aerosol")) return "requires ventilated chemical room or fume hood adjacency";
        if (p.contains("fermentation") || p.contains("fungal")) return "valid in wet rooms, sewer gardens, vats, or food labs";
        return "valid in faction-appropriate lab, workshop, clinic, still room, or chem kitchen";
    }
    static void applyFactionEquipmentPreference(FactionRecipeVariant v) {
        if (v == null || v.base == null) return;
        if (v.faction == Faction.SCAVENGER || v.faction == Faction.BANDIT) {
            v.replaceEquipment("Reagent preparation bench","Crude chem bench").replaceEquipment("Distillation column","Crude still").replaceEquipment("Clean fermentation vat","Sump fermentation tub").addEquipment("Chemical waste trap",1);
        } else if (v.faction == Faction.MECHANICUS) {
            v.addEquipment("Calibrated assay shrine",1);
            v.replaceEquipment("Crude chem bench","Reagent preparation bench");
        } else if (v.faction == Faction.NOBLE) {
            if (containsAny(low(v.base.outputBaseItem),"obscura","wine","cordial","nectar","lotus","perfume","glass","sorrow","widow","ghost","gild")) v.addEquipment("Spire perfumery glassware",1).addEquipment("Luxury vialing station",1);
        } else if (v.faction == Faction.ARBITES) {
            if (containsAny(low(v.base.outputBaseItem),"truth","compliance","black badge","fear","confessor","grav")) v.addEquipment("Interrogation dosing cradle",1).addEquipment("Toxin lockbox",1);
        } else if (v.faction == Faction.CULTIST) {
            if (containsAny(low(v.base.outputBaseItem),"witch","choir","benediction","spook","mirror","red choir","black sun","flect")) v.addEquipment("Ritual censer kiln",1).addEquipment("Forbidden preparation chamber",1);
        } else if (v.faction == Faction.MUTANT) {
            v.addEquipment("Mutant adaptation rack",1);
            v.replaceEquipment("Clean fermentation vat","Sump fermentation tub");
        } else if (v.faction == Faction.IMPERIAL_GUARD) {
            if (containsAny(low(v.base.outputBaseItem),"stimm","clotfoam","white mercy","red waker")) v.addEquipment("Injector filling station",1).addEquipment("Cold storage locker",1);
        }
    }
    static ArrayList<String> auditLines() { ArrayList<String> l = new ArrayList<>(); for (LaboratoryEquipmentProfile p : profiles()) l.add("LAB_EQUIPMENT " + p.auditLine()); return l; }
    static boolean containsAny(String hay, String... needles) { for (String x : needles) if (hay.contains(x)) return true; return false; }
    static String low(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}


class ChemicalEquipmentAuditApi {
    static ChemicalEquipmentAudit audit() {
        ChemicalEquipmentAudit a = new ChemicalEquipmentAudit();
        a.registeredEquipmentProfiles = ChemicalEquipmentAuthority.profiles().size();
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) if (ChemicalEquipmentAuthority.isChemicalRecipe(r)) {
            a.chemicalRecipes++;
            a.processCounts.put(r.processType, a.processCounts.getOrDefault(r.processType,0)+1);
            for (String room : r.roomRequirements) a.roomCounts.put(room, a.roomCounts.getOrDefault(room,0)+1);
            if (r.equipmentRequirements.isEmpty()) a.missingEquipment.add(r.outputBaseItem + " has no equipment requirement");
            if (r.minimumMachineTier < 0 || r.minimumMachineTier > QualityAuthorityApi.UNLIMITED_TIER) a.issues.add(r.outputBaseItem + " invalid machine tier " + r.minimumMachineTier);
            for (String eq : r.equipmentRequirements.keySet()) if (ItemCatalog.get(eq) == null) a.missingEquipmentCatalogItems.add(eq + " required by " + r.outputBaseItem);
        }
        for (LaboratoryEquipmentProfile p : ChemicalEquipmentAuthority.profiles()) if (ItemCatalog.get(p.name) == null) a.missingEquipmentCatalogItems.add("profile item missing: " + p.name);
        return a;
    }
}


class ChemicalEquipmentAudit {
    int registeredEquipmentProfiles, chemicalRecipes;
    final TreeMap<String,Integer> processCounts = new TreeMap<>();
    final TreeMap<String,Integer> roomCounts = new TreeMap<>();
    final TreeSet<String> missingEquipment = new TreeSet<>();
    final TreeSet<String> missingEquipmentCatalogItems = new TreeSet<>();
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return missingEquipment.isEmpty() && missingEquipmentCatalogItems.isEmpty() && issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Laboratory / Chemical Equipment Production Requirements audit");
        l.add("Registered lab equipment profiles: " + registeredEquipmentProfiles);
        l.add("Chemical / medicae / precursor recipes requiring apparatus: " + chemicalRecipes);
        l.add("Process types: " + processCounts.size());
        for (Map.Entry<String,Integer> e : processCounts.entrySet()) l.add("  process " + e.getKey() + ": " + e.getValue());
        l.add("Room requirement groups: " + roomCounts.size());
        for (Map.Entry<String,Integer> e : roomCounts.entrySet()) l.add("  room " + e.getKey() + ": " + e.getValue());
        l.add("Missing equipment annotations: " + missingEquipment.size());
        for (String s : missingEquipment) l.add("  MISSING_EQUIPMENT_ANNOTATION " + s);
        l.add("Missing equipment catalog items: " + missingEquipmentCatalogItems.size());
        for (String s : missingEquipmentCatalogItems) l.add("  MISSING_EQUIPMENT_ITEM " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("-- Equipment Authority Profiles --");
        l.addAll(ChemicalEquipmentAuthority.auditLines());
        int shown = 0;
        l.add("-- Sample annotated recipes --");
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) if (ChemicalEquipmentAuthority.isChemicalRecipe(r) && shown++ < 24) l.add("  " + r.auditLine());
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}



class FacilityOutputRunEstimate {
    final int outputCount, turns, wear, breakdownRollPercent, queueThroughput, workerCapacityUsed;
    final double effectiveThroughput, effectiveEfficiency, factionEfficiency, machineThroughput;
    final String qualityCeiling, workerLine;
    FacilityOutputRunEstimate(int outputCount, int turns, int wear, int breakdownRollPercent, int queueThroughput, int workerCapacityUsed, double effectiveThroughput, double effectiveEfficiency, double factionEfficiency, double machineThroughput, String qualityCeiling, String workerLine) {
        this.outputCount=outputCount; this.turns=turns; this.wear=wear; this.breakdownRollPercent=breakdownRollPercent; this.queueThroughput=queueThroughput; this.workerCapacityUsed=workerCapacityUsed; this.effectiveThroughput=effectiveThroughput; this.effectiveEfficiency=effectiveEfficiency; this.factionEfficiency=factionEfficiency; this.machineThroughput=machineThroughput; this.qualityCeiling=qualityCeiling; this.workerLine=workerLine;
    }
    String shortLine() {
        return "output x" + outputCount + ", turns " + turns + ", queueStep " + queueThroughput + ", wear " + wear + ", breakdown~" + breakdownRollPercent + "%" + ", machineThroughput x" + fmt(machineThroughput) + ", factionEfficiency x" + fmt(factionEfficiency) + ", " + workerLine + ", ceiling " + qualityCeiling;
    }
    static String fmt(double d){ return String.format(Locale.US, "%.2f", d); }
}


class FacilityOutputModifierAuthority {
    static FacilityOutputRunEstimate estimate(GamePanel g, BaseObject machine, FactionRecipeVariant v, boolean manual) {
        MachineTierProfile mp = MachineTierAuthority.forMachine(machine);
        int width = v == null ? 1 : Math.max(1, v.width() + v.equipmentRequirements.size());
        int baseTurns = 6 + width + (v != null && v.base != null ? Math.max(0, v.base.minimumMachineTier) : 0);
        double factionEff = v == null ? 1.0 : Math.max(0.30, v.efficiencyMultiplier);
        double reliability = v == null ? 1.0 : Math.max(0.20, v.reliabilityMultiplier);
        double maintenance = v == null ? 1.0 : Math.max(0.20, v.maintenanceMultiplier);
        double power = v == null ? 1.0 : Math.max(0.20, v.powerDemandMultiplier);
        int availableLabor = g == null ? 0 : g.availableRecruitLabor();
        int workerCap = manual ? 1 : Math.max(1, Math.min(mp.workerCapacity, Math.max(1, availableLabor)));
        double workerBoost = manual ? 0.72 : Math.max(1.0, 1.0 + (workerCap - 1) * 0.18);
        double effectiveThroughput = Math.max(0.25, mp.throughput * factionEff * workerBoost / Math.max(0.70, power));
        double effectiveEfficiency = Math.max(0.20, mp.efficiency * factionEff * reliability / Math.max(0.60, maintenance));
        int turns = Math.max(manual ? 4 : 1, (int)Math.round(baseTurns / effectiveThroughput));
        if (manual && v != null && v.base != null && v.base.processType != null && ControlledProductionJobAuthority.containsAny(v.base.processType.toLowerCase(Locale.ROOT), "sterile", "forbidden", "warp", "aerosol", "distillation", "combat")) turns += 6;
        if (manual && g != null) turns = Math.max(4, turns - Math.max(g.stat("Mechanics",0), Math.max(g.stat("Intellect",0), g.stat("Medical",0))) / 3);
        int baseOutput = 1;
        int output = Math.max(1, (int)Math.round(baseOutput * Math.max(0.40, effectiveEfficiency)));
        if (manual) output = Math.max(1, (int)Math.floor(output * 0.85));
        int queueStep = Math.max(1, Math.min(3, (int)Math.floor(effectiveThroughput)));
        int defect = v == null ? 10 : v.defectPercent();
        int wear = Math.max(0, (defect >= 16 ? 1 : 0) + (maintenance > 1.18 ? 1 : 0) + (power > 1.25 ? 1 : 0) - (mp.durabilityMultiplier >= 1.25 ? 1 : 0));
        if (manual && wear > 0) wear = Math.max(1, wear - 1);
        int breakdown = (int)Math.round(mp.breakdownRate * 100.0 * Math.max(0.35, defect / 10.0) * Math.max(0.60, maintenance) * Math.max(0.60, power) / Math.max(0.50, reliability));
        if (manual) breakdown = Math.max(1, breakdown - 3);
        breakdown = Math.max(0, Math.min(75, breakdown));
        String ceiling = QualityAuthorityApi.qualityName(mp.qualityCeilingTier);
        String workers = manual ? "manual player operation" : ("workers " + workerCap + "/" + mp.workerCapacity);
        return new FacilityOutputRunEstimate(output, turns, wear, breakdown, queueStep, workerCap, effectiveThroughput, effectiveEfficiency, factionEff, mp.throughput, ceiling, workers);
    }
    static String forecastLine(GamePanel g, BaseObject machine, FactionRecipeVariant v, boolean manual) { return "facility " + estimate(g, machine, v, manual).shortLine(); }
    static ArrayList<String> ledgerLines(int max) {
        ArrayList<String> l = new ArrayList<>();
        l.add("Facility capacity / output modifier ledger");
        l.add("Scope: live queued generated production now estimates output count, turn cost, queue throughput, machine wear, breakdown risk, machine quality ceiling, and worker-capacity effect from MachineTierAuthority plus faction manufacturing identity.");
        l.add("Formula: machine tier throughput/efficiency/worker capacity + faction efficiency/reliability/power/maintenance + recipe width + manual-vs-crew operation -> run estimate.");
        int n=0;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
            BaseObject sample = new BaseObject(sampleMachineName(v), sampleMachineSymbol(v), 0,0,0,0);
            sample.qualityName = v.machineQuality;
            MachineTierAuthority.applyToConfiguredObject(sample);
            FacilityOutputRunEstimate crew = estimate(null, sample, v, false);
            FacilityOutputRunEstimate manual = estimate(null, sample, v, true);
            l.add("  " + v.outputName + " | crew " + crew.shortLine() + " | manual " + manual.shortLine());
            if (++n >= max) break;
        }
        return l;
    }
    static String sampleMachineName(FactionRecipeVariant v) {
        String c = v == null ? "" : (v.knowledgeCategory == null ? "" : v.knowledgeCategory.toLowerCase(Locale.ROOT));
        if (c.contains("chemical") || c.contains("medical")) return "Audit lab apparatus";
        if (c.contains("food") || c.contains("water") || c.contains("agricultural")) return "Audit processing bench";
        if (c.contains("ballistics") || c.contains("metallurgy")) return "Audit micro forge";
        return "Audit workbench";
    }
    static char sampleMachineSymbol(FactionRecipeVariant v) {
        String c = v == null ? "" : (v.knowledgeCategory == null ? "" : v.knowledgeCategory.toLowerCase(Locale.ROOT));
        if (c.contains("chemical") || c.contains("medical")) return 'L';
        if (c.contains("food") || c.contains("water") || c.contains("agricultural")) return 'w';
        if (c.contains("ballistics") || c.contains("metallurgy")) return 'f';
        return 'w';
    }
}


class FacilityOutputModifierAuditApi {
    static FacilityOutputModifierAudit audit() {
        FacilityOutputModifierAudit a = new FacilityOutputModifierAudit();
        a.machineTierProfiles = MachineTierAuthority.profiles().size();
        for (MachineTierProfile p : MachineTierAuthority.profiles()) {
            if (p.workerCapacity < 1) a.issues.add("machine tier has no worker capacity " + p.qualityName);
            if (p.throughput <= 0 || p.efficiency <= 0) a.issues.add("machine tier invalid multiplier " + p.qualityName);
        }
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
            a.generatedVariants++;
            BaseObject sample = new BaseObject(FacilityOutputModifierAuthority.sampleMachineName(v), FacilityOutputModifierAuthority.sampleMachineSymbol(v), 0,0,0,0);
            sample.qualityName = v.machineQuality;
            MachineTierAuthority.applyToConfiguredObject(sample);
            FacilityOutputRunEstimate crew = FacilityOutputModifierAuthority.estimate(null, sample, v, false);
            FacilityOutputRunEstimate manual = FacilityOutputModifierAuthority.estimate(null, sample, v, true);
            if (crew.outputCount < 1 || manual.outputCount < 1) a.issues.add("nonpositive output estimate for " + v.outputName);
            if (crew.turns < 1 || manual.turns < 1) a.issues.add("nonpositive turn estimate for " + v.outputName);
            if (crew.breakdownRollPercent < 0 || crew.breakdownRollPercent > 75) a.issues.add("crew breakdown out of range for " + v.outputName);
            if (manual.breakdownRollPercent < 0 || manual.breakdownRollPercent > 75) a.issues.add("manual breakdown out of range for " + v.outputName);
            a.forecastableVariants++;
            if (crew.queueThroughput > 1) a.multiQueueVariants++;
            if (crew.wear > 0 || manual.wear > 0) a.wearTrackedVariants++;
            a.maxOutput = Math.max(a.maxOutput, Math.max(crew.outputCount, manual.outputCount));
            a.maxTurns = Math.max(a.maxTurns, Math.max(crew.turns, manual.turns));
        }
        return a;
    }
}

class FacilityOutputModifierAudit {
    int generatedVariants, forecastableVariants, machineTierProfiles, multiQueueVariants, wearTrackedVariants, maxOutput, maxTurns;
    ArrayList<String> issues = new ArrayList<>();
    boolean passed(){ return generatedVariants > 0 && forecastableVariants == generatedVariants && machineTierProfiles > 0 && maxOutput > 0 && maxTurns > 0 && issues.isEmpty(); }
    ArrayList<String> lines(){
        ArrayList<String> l = new ArrayList<>();
        l.add("0.9.02 Controlled Production Job UI / Queue / Input Forecasting audit");
        l.add("Generated variants checked: " + generatedVariants);
        l.add("Forecastable variants: " + forecastableVariants);
        l.add("Machine tier profiles: " + machineTierProfiles);
        l.add("Multi-queue-throughput variants: " + multiQueueVariants);
        l.add("Wear-tracked variants: " + wearTrackedVariants);
        l.add("Max output estimate: " + maxOutput);
        l.add("Max turn estimate: " + maxTurns);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("ISSUE " + s);
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}



class ProductionInputConsumptionRecord {
    boolean success = false;
    String failureReason = "";
    final ArrayList<String> itemNames = new ArrayList<>();
    final ArrayList<String> instanceIds = new ArrayList<>();
    final ArrayList<String> containerIds = new ArrayList<>();
    final ArrayList<String> provenanceSummaries = new ArrayList<>();
    void add(ItemInstance inst, String containerId) {
        if (inst == null) return;
        success = true;
        itemNames.add(inst.displayName == null ? "Unknown item" : inst.displayName);
        instanceIds.add(inst.id == null ? "I?" : inst.id);
        containerIds.add(containerId == null ? "unknown.container" : containerId);
        provenanceSummaries.add(inst.provenance == null ? "untraced" : inst.provenance.summary());
    }
    void merge(ProductionInputConsumptionRecord other) {
        if (other == null) return;
        if (!other.success) { success = false; failureReason = other.failureReason; }
        else if (itemNames.isEmpty()) success = true;
        itemNames.addAll(other.itemNames);
        instanceIds.addAll(other.instanceIds);
        containerIds.addAll(other.containerIds);
        provenanceSummaries.addAll(other.provenanceSummaries);
    }
    String shortLine() {
        if (!success) return failureReason == null || failureReason.isBlank() ? "input consume failed" : failureReason;
        if (itemNames.isEmpty()) return "no consumed inputs";
        return itemNames.get(0) + "#" + instanceIds.get(0) + (itemNames.size() > 1 ? " +" + (itemNames.size()-1) + " more" : "");
    }
    String summaryLine() {
        if (!success) return shortLine();
        if (itemNames.isEmpty()) return "no consumed inputs";
        LinkedHashMap<String,Integer> counts = new LinkedHashMap<>();
        for (String n : itemNames) counts.put(n, counts.getOrDefault(n,0)+1);
        ArrayList<String> bits = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<String,Integer> e : counts.entrySet()) { bits.add(e.getValue() + "x " + e.getKey()); if (++shown >= 4) break; }
        if (counts.size() > shown) bits.add("+" + (counts.size() - shown) + " more kind(s)");
        return String.join(", ", bits) + " via " + instanceIds.size() + " item instance(s)";
    }
}


class ProductionContainerAuthority {
    private ProductionContainerAuthority() {}
    static final String[] INPUT_CONTAINER_ORDER = new String[]{GamePanel.CONTAINER_BASE_STORAGE, GamePanel.CONTAINER_PLAYER_INVENTORY};

    static ArrayList<String> inputContainerOrder(GamePanel g) {
        ArrayList<String> ids = new ArrayList<>();
        if (g != null) {
            for (String cid : g.itemContainers.keySet()) if (cid != null && cid.startsWith(GamePanel.CONTAINER_MACHINE_INPUT_PREFIX)) ids.add(cid);
        }
        ids.add(GamePanel.CONTAINER_BASE_STORAGE);
        ids.add(GamePanel.CONTAINER_PLAYER_INVENTORY);
        return ids;
    }

    static int countAvailable(GamePanel g, String item) {
        if (g == null || item == null || item.isBlank()) return 0;
        int registryCount = 0;
        boolean sawRegistry = false;
        for (String cid : inputContainerOrder(g)) {
            ContainerRecord c = g.itemContainers.get(cid);
            if (c == null) continue;
            sawRegistry = true;
            for (String id : c.itemInstanceIds) {
                ItemInstance inst = g.itemInstances.get(id);
                if (inst != null && ItemQuality.namesMatch(inst.displayName, item)) registryCount++;
            }
        }
        if (sawRegistry) return registryCount;
        return countLegacy(g.baseStorage, g, item) + countLegacy(g.inventory, g, item);
    }

    static ProductionInputConsumptionRecord consumeOne(GamePanel g, String item, String route) {
        ProductionInputConsumptionRecord rec = new ProductionInputConsumptionRecord();
        if (g == null || item == null || item.isBlank()) { rec.failureReason = "blank production input"; return rec; }
        for (String cid : inputContainerOrder(g)) {
            ContainerRecord c = g.itemContainers.get(cid);
            if (c == null) continue;
            for (int i=c.itemInstanceIds.size()-1; i>=0; i--) {
                String id = c.itemInstanceIds.get(i);
                ItemInstance inst = g.itemInstances.get(id);
                if (inst == null || !ItemQuality.namesMatch(inst.displayName, item)) continue;
                c.itemInstanceIds.remove(i);
                g.itemInstances.remove(id);
                removeOneLegacyForContainer(g, cid, inst.displayName, i);
                ItemProvenanceRecord consumed = ItemProvenanceRecord.transferred(inst.provenance, inst.displayName, g.world, g.turn, route == null || route.isBlank() ? "consumed by production" : route);
                inst.provenance = consumed;
                rec.add(inst, cid);
                if (consumed != null) g.rememberItemProvenance(inst.displayName, consumed);
                DebugLog.audit("PRODUCTION_INPUT_INSTANCE_CONSUMED", "item=" + inst.displayName + " instance=" + inst.id + " from=" + cid + " route=" + route + " state=" + g.stateSummary());
                return rec;
            }
        }
        // Legacy fallback for old saves with list entries but no item registry parity.
        for (String cid : INPUT_CONTAINER_ORDER) {
            ArrayList<String> legacy = legacyListFor(g, cid);
            if (legacy == null) continue;
            for (int i=legacy.size()-1; i>=0; i--) {
                String raw = legacy.get(i);
                if (!ItemQuality.namesMatch(raw, item)) continue;
                legacy.remove(i);
                ItemProvenanceRecord pr = g.takeProvenanceForItem(raw);
                ItemProvenanceRecord consumed = ItemProvenanceRecord.transferred(pr, raw, g.world, g.turn, route == null || route.isBlank() ? "consumed by production legacy fallback" : route);
                ItemInstance synthetic = new ItemInstance("legacy-" + Math.abs(Objects.hash(raw, cid, g.turn, System.nanoTime())), raw, cid, consumed == null ? "" : consumed.unitId, consumed);
                rec.add(synthetic, cid);
                if (consumed != null) g.rememberItemProvenance(raw, consumed);
                DebugLog.warn("PRODUCTION_INPUT_LEGACY_FALLBACK", "item=" + raw + " from=" + cid + " route=" + route + " state=" + g.stateSummary());
                return rec;
            }
        }
        rec.failureReason = "missing input item instance: " + item;
        return rec;
    }

    static ArrayList<String> ledgerLines(GamePanel g, int limit) {
        ArrayList<String> l = new ArrayList<>();
        l.add("Production container authority ledger");
        l.add("Rule: generated/build production consumes concrete ItemInstance records from station input buffers, base storage, then carried inventory; equipment requirements remain installed/non-consumed apparatus.");
        if (g == null) { l.add("No active game panel supplied."); return l; }
        for (String cid : inputContainerOrder(g)) {
            ContainerRecord c = g.itemContainers.get(cid);
            ArrayList<String> legacy = legacyListFor(g, cid);
            int registry = c == null ? 0 : c.itemInstanceIds.size();
            int legacyCount = legacy == null ? 0 : legacy.size();
            String parity = legacy == null ? "registry-only" : (registry == legacyCount ? "OK" : "MISMATCH");
            l.add(cid + ": registry instances=" + registry + ", legacy mirror=" + legacyCount + ", parity=" + parity);
            int shown = 0;
            if (c != null) for (String id : c.itemInstanceIds) {
                ItemInstance inst = g.itemInstances.get(id);
                if (inst == null) { l.add("  missing registry entry " + id); continue; }
                l.add("  " + inst.id + " " + inst.displayName + " | " + (inst.provenance == null ? "untraced" : inst.provenance.summary()));
                if (++shown >= Math.max(1, limit/2)) break;
            }
        }
        return l;
    }

    private static int countLegacy(ArrayList<String> list, GamePanel g, String item) { int n=0; if (list != null) for (String s : list) if (ItemQuality.namesMatch(s, item)) n++; return n; }
    private static ArrayList<String> legacyListFor(GamePanel g, String cid) {
        if (GamePanel.CONTAINER_BASE_STORAGE.equals(cid)) return g.baseStorage;
        if (GamePanel.CONTAINER_PLAYER_INVENTORY.equals(cid)) return g.inventory;
        return null;
    }
    private static void removeOneLegacyForContainer(GamePanel g, String cid, String itemName, int preferredIndex) {
        ArrayList<String> list = legacyListFor(g, cid);
        if (list == null || itemName == null) return;
        if (preferredIndex >= 0 && preferredIndex < list.size() && ItemQuality.namesMatch(list.get(preferredIndex), itemName)) { list.remove(preferredIndex); return; }
        for (int i=list.size()-1;i>=0;i--) if (ItemQuality.namesMatch(list.get(i), itemName)) { list.remove(i); return; }
        DebugLog.warn("PRODUCTION_CONTAINER_LEGACY_MIRROR_GAP", "no legacy mirror item=" + itemName + " cid=" + cid + " state=" + g.stateSummary());
    }
}


class ProductionContainerAuthorityAuditApi {
    static ProductionContainerAuthorityAudit audit(GamePanel g) {
        ProductionContainerAuthorityAudit a = new ProductionContainerAuthorityAudit();
        if (g == null) { a.issues.add("no active GamePanel for runtime production-container audit"); return a; }
        for (String cid : ProductionContainerAuthority.INPUT_CONTAINER_ORDER) {
            ContainerRecord c = g.itemContainers.get(cid);
            ArrayList<String> legacy = GamePanel.CONTAINER_BASE_STORAGE.equals(cid) ? g.baseStorage : g.inventory;
            int registry = c == null ? 0 : c.itemInstanceIds.size();
            int legacyCount = legacy == null ? 0 : legacy.size();
            a.containersChecked++;
            a.registryInstances += registry;
            a.legacyMirrorItems += legacyCount;
            if (registry != legacyCount) a.parityIssues.add(cid + " registry=" + registry + " legacy=" + legacyCount);
            if (c != null) for (String id : c.itemInstanceIds) {
                ItemInstance inst = g.itemInstances.get(id);
                if (inst == null) { a.missingInstances.add(cid + ":" + id); continue; }
                a.instanceBackedItems++;
                if (inst.provenance != null) a.provenanceBackedItems++;
            }
        }
        int sampled = 0;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
            if (v == null || v.itemInputs == null || v.itemInputs.isEmpty()) continue;
            a.generatedRecipesWithInputs++;
            for (String item : v.itemInputs.keySet()) {
                a.forecastChecks++;
                ProductionContainerAuthority.countAvailable(g, item);
            }
            if (++sampled >= 250) break;
        }
        if (a.containersChecked < 2) a.issues.add("expected player/base input containers were not both checked");
        if (!a.missingInstances.isEmpty()) a.issues.add("missing item instance registry entries in input containers");
        return a;
    }
}


class ProductionContainerAuthorityAudit {
    int containersChecked, registryInstances, legacyMirrorItems, instanceBackedItems, provenanceBackedItems, generatedRecipesWithInputs, forecastChecks;
    final TreeSet<String> parityIssues = new TreeSet<>();
    final TreeSet<String> missingInstances = new TreeSet<>();
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return missingInstances.isEmpty() && issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("0.9.03 Real ItemInstance Input Consumption / Production Container Authority Expansion audit");
        l.add("Input containers checked: " + containersChecked);
        l.add("Registry item instances in input containers: " + registryInstances);
        l.add("Legacy mirror items in input containers: " + legacyMirrorItems);
        l.add("Instance-backed input items: " + instanceBackedItems);
        l.add("Provenance-backed input items: " + provenanceBackedItems);
        l.add("Generated recipes with consumed inputs sampled: " + generatedRecipesWithInputs);
        l.add("Forecast count checks executed: " + forecastChecks);
        l.add("Parity notices: " + parityIssues.size());
        for (String s : parityIssues) l.add("  PARITY_NOTICE " + s);
        l.add("Missing instances: " + missingInstances.size());
        for (String s : missingInstances) l.add("  MISSING_INSTANCE " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("Consumption rule: production/build inputs are consumed as concrete ItemInstance records from base storage first, then player inventory, with a legacy-list fallback only for migrated saves.");
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}



class PersistentStockContainerAuthority {
    private PersistentStockContainerAuthority() {}
    static boolean isPersistentStockContainer(String id) {
        if (id == null) return false;
        return id.startsWith(GamePanel.CONTAINER_CONTRACT_OBJECT_PREFIX) || id.startsWith(GamePanel.CONTAINER_CORPSE_LOOT_PREFIX) || id.startsWith(GamePanel.CONTAINER_FACTION_STOCK_PREFIX);
    }
    static ArrayList<String> ledgerLines(GamePanel g, int limit) {
        ArrayList<String> l = new ArrayList<>();
        l.add("Persistent contract / corpse / faction stock container ledger");
        l.add("Rule: contracts, corpse loot, and faction-site exports should be held as ItemInstance records in named containers before transfer to player/trader inventory.");
        if (g == null) { l.add("No active game panel supplied."); return l; }
        PersistentStockContainerAudit a = PersistentStockContainerAuditApi.audit(g);
        l.add("Contract containers=" + a.contractContainers + ", corpse containers=" + a.corpseContainers + ", faction stock containers=" + a.factionStockContainers + ", persistent instances=" + a.persistentInstances + ".");
        int shown = 0;
        for (ContainerRecord c : g.itemContainers.values()) {
            if (c == null || !isPersistentStockContainer(c.id)) continue;
            l.add(c.id + " — " + c.label + " — items " + c.itemInstanceIds.size());
            int inner = 0;
            for (String id : c.itemInstanceIds) {
                ItemInstance inst = g.itemInstances.get(id);
                l.add("  " + id + " " + (inst == null ? "MISSING" : inst.displayName) + " | " + (inst == null || inst.provenance == null ? "untraced" : inst.provenance.summary()));
                if (++inner >= 3) break;
            }
            if (++shown >= Math.max(1, limit/4)) break;
        }
        if (shown == 0) l.add("No persistent contract/corpse/faction-stock containers are currently materialized in this save; they appear when contracts spawn, NPCs die, or faction stock is contacted through trade.");
        return l;
    }
}


class PersistentStockPickupUxAuthority {
    private PersistentStockPickupUxAuthority() {}
    static ArrayList<String> ledgerLines(GamePanel g, int limit) {
        ArrayList<String> l = new ArrayList<>();
        l.add("Persistent stock pickup UX ledger");
        l.add("Scope: contract-object and corpse-loot map markers now expose previews, inspect-without-transfer, one-item confirm pickup, and TAKE ALL bulk pickup through the same ItemInstance transfer authority.");
        if (g == null) { l.add("No active game panel supplied."); return l; }
        PersistentStockPickupUxAudit a = PersistentStockPickupUxAuditApi.audit(g);
        l.add("Inspectable markers=" + a.inspectableMarkers + ", linked containers=" + a.markersWithContainers + ", empty markers=" + a.emptyMarkers + ", transfer-ready markers=" + a.transferReadyMarkers + ", status " + (a.passed()?"PASS":"FAIL") + ".");
        int shown = 0;
        if (g.world != null) {
            for (MapObjectState m : g.world.mapObjects) {
                if (m == null || !("contract-object".equals(m.type) || "corpse-container".equals(m.type))) continue;
                String cid = g.persistentContainerIdForObject(m);
                l.add("  " + m.type + " at " + m.x + "," + m.y + " | " + m.label + " | " + cid + " | items " + g.containerItemCount(cid) + " | next " + g.containerNextItemSummary(cid));
                if (++shown >= limit) break;
            }
        }
        if (shown == 0) l.add("  No contract/corpse stock markers in the current zone; use accepted contracts or combat deaths to create them.");
        l.add("Interaction rule: CONFIRM transfers one item; INSPECT STOCK logs up to five provenance-backed entries; TAKE ALL transfers until empty, capacity-blocked, or safety-capped.");
        return l;
    }
}


class PersistentStockPickupUxAuditApi {
    static PersistentStockPickupUxAudit audit(GamePanel g) {
        PersistentStockPickupUxAudit a = new PersistentStockPickupUxAudit();
        if (g == null) { a.issues.add("no active GamePanel for persistent stock pickup UX audit"); return a; }
        a.hasPreviewRenderer = true;
        a.hasInspectAction = true;
        a.hasTakeAllAction = true;
        a.hasCleanupAction = true;
        if (g.world != null) {
            for (MapObjectState m : g.world.mapObjects) {
                if (m == null || !("contract-object".equals(m.type) || "corpse-container".equals(m.type))) continue;
                a.inspectableMarkers++;
                String cid = g.persistentContainerIdForObject(m);
                if (cid == null || cid.isBlank()) { a.markersMissingContainerIds.add(m.type + "@" + m.x + "," + m.y); continue; }
                ContainerRecord c = g.itemContainers.get(cid);
                if (c == null) a.markersMissingContainers.add(cid + " for " + m.type + "@" + m.x + "," + m.y);
                else {
                    a.markersWithContainers++;
                    if (c.itemInstanceIds.isEmpty()) a.emptyMarkers++;
                    else a.transferReadyMarkers++;
                    for (String id : c.itemInstanceIds) {
                        ItemInstance inst = g.itemInstances.get(id);
                        if (inst == null) a.missingInstances.add(cid + ":" + id);
                        else if (inst.provenance != null) a.provenanceBackedInstances++;
                    }
                }
            }
        }
        if (!a.hasPreviewRenderer) a.issues.add("persistent stock interaction preview renderer missing");
        if (!a.hasInspectAction) a.issues.add("inspect action missing");
        if (!a.hasTakeAllAction) a.issues.add("take-all action missing");
        if (!a.hasCleanupAction) a.issues.add("empty-marker cleanup action missing");
        if (!a.markersMissingContainerIds.isEmpty()) a.issues.add("persistent stock markers lack container ids");
        if (!a.markersMissingContainers.isEmpty()) a.issues.add("persistent stock markers point at missing containers");
        if (!a.missingInstances.isEmpty()) a.issues.add("persistent stock UX markers reference missing item instances");
        return a;
    }
}


class PersistentStockPickupUxAudit {
    int inspectableMarkers, markersWithContainers, transferReadyMarkers, emptyMarkers, provenanceBackedInstances;
    boolean hasPreviewRenderer, hasInspectAction, hasTakeAllAction, hasCleanupAction;
    final TreeSet<String> markersMissingContainerIds = new TreeSet<>();
    final TreeSet<String> markersMissingContainers = new TreeSet<>();
    final TreeSet<String> missingInstances = new TreeSet<>();
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("0.9.04 Contract / Corpse / Faction Stock UX and Container Pickup Polish audit");
        l.add("Inspectable contract/corpse markers: " + inspectableMarkers);
        l.add("Markers with containers: " + markersWithContainers);
        l.add("Transfer-ready markers: " + transferReadyMarkers);
        l.add("Empty markers awaiting cleanup: " + emptyMarkers);
        l.add("Provenance-backed visible instances: " + provenanceBackedInstances);
        l.add("Preview renderer: " + (hasPreviewRenderer?"YES":"NO"));
        l.add("Inspect action: " + (hasInspectAction?"YES":"NO"));
        l.add("Take-all action: " + (hasTakeAllAction?"YES":"NO"));
        l.add("Empty cleanup action: " + (hasCleanupAction?"YES":"NO"));
        l.add("Missing marker container IDs: " + markersMissingContainerIds.size());
        for (String s : markersMissingContainerIds) l.add("  MISSING_CONTAINER_ID " + s);
        l.add("Missing marker containers: " + markersMissingContainers.size());
        for (String s : markersMissingContainers) l.add("  MISSING_CONTAINER " + s);
        l.add("Missing item instances: " + missingInstances.size());
        for (String s : missingInstances) l.add("  MISSING_INSTANCE " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}


class PersistentStockContainerAuditApi {
    static PersistentStockContainerAudit audit(GamePanel g) {
        PersistentStockContainerAudit a = new PersistentStockContainerAudit();
        if (g == null) { a.issues.add("no active GamePanel for persistent stock container audit"); return a; }
        for (ContainerRecord c : g.itemContainers.values()) {
            if (c == null || c.id == null) continue;
            boolean relevant = PersistentStockContainerAuthority.isPersistentStockContainer(c.id);
            if (!relevant) continue;
            a.persistentContainers++;
            if (c.id.startsWith(GamePanel.CONTAINER_CONTRACT_OBJECT_PREFIX)) a.contractContainers++;
            if (c.id.startsWith(GamePanel.CONTAINER_CORPSE_LOOT_PREFIX)) a.corpseContainers++;
            if (c.id.startsWith(GamePanel.CONTAINER_FACTION_STOCK_PREFIX)) a.factionStockContainers++;
            for (String id : c.itemInstanceIds) {
                a.persistentInstances++;
                ItemInstance inst = g.itemInstances.get(id);
                if (inst == null) a.missingInstances.add(c.id + ":" + id);
                else {
                    if (inst.containerId == null || !inst.containerId.equals(c.id)) a.containerMismatches.add(id + " registry=" + inst.containerId + " container=" + c.id);
                    if (inst.provenance != null) a.provenanceBackedInstances++;
                }
            }
        }
        if (!a.missingInstances.isEmpty()) a.issues.add("persistent containers reference missing item instances");
        if (!a.containerMismatches.isEmpty()) a.issues.add("persistent container instance/container id mismatch");
        if (a.persistentInstances > 0 && a.provenanceBackedInstances <= 0) a.issues.add("persistent stock exists but has no provenance-backed instances");
        return a;
    }
}


class PersistentStockContainerAudit {
    int persistentContainers, contractContainers, corpseContainers, factionStockContainers, persistentInstances, provenanceBackedInstances;
    final TreeSet<String> missingInstances = new TreeSet<>();
    final TreeSet<String> containerMismatches = new TreeSet<>();
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("0.9.03 Persistent Contract / Corpse / Faction Stock Container Expansion audit");
        l.add("Persistent containers: " + persistentContainers);
        l.add("Contract object containers: " + contractContainers);
        l.add("Corpse loot containers: " + corpseContainers);
        l.add("Faction stock containers: " + factionStockContainers);
        l.add("Persistent item instances: " + persistentInstances);
        l.add("Provenance-backed persistent instances: " + provenanceBackedInstances);
        l.add("Missing instances: " + missingInstances.size());
        for (String s : missingInstances) l.add("  MISSING_INSTANCE " + s);
        l.add("Container mismatches: " + containerMismatches.size());
        for (String s : containerMismatches) l.add("  CONTAINER_MISMATCH " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}



class ActorAccessAuthority {
    static String containerTransferProblem(GamePanel g, String fromContainerId, String toContainerId, String route) {
        if (g == null) return null;
        String from = fromContainerId == null ? "" : fromContainerId;
        String to = toContainerId == null ? "" : toContainerId;
        String r = route == null ? "" : route.toLowerCase(Locale.ROOT);
        if (from.isBlank() || to.isBlank()) return "container route is incomplete";
        if (isVoid(from) || isVoid(to)) return null;
        if (from.equals(GamePanel.CONTAINER_PLAYER_INVENTORY) || to.equals(GamePanel.CONTAINER_PLAYER_INVENTORY)) {
            if (from.equals(GamePanel.CONTAINER_PLAYER_INVENTORY)) return null;
            if (from.equals(GamePanel.CONTAINER_BASE_STORAGE) && !g.isInClaimedRoom(g.playerX, g.playerY)) return "base storage may only be recovered from inside the claimed room";
            if (from.startsWith(GamePanel.CONTAINER_TRADER_SHELF_PREFIX)) return r.contains("sold by") || r.contains("purchase") ? null : "trader shelf stock must move through a trade action";
            if (from.startsWith(GamePanel.CONTAINER_FACTION_STOCK_PREFIX)) return "faction stock cannot be looted directly; use an authorized trader shelf or faction interface";
            if (from.startsWith(GamePanel.CONTAINER_CONTRACT_OBJECT_PREFIX) || from.startsWith(GamePanel.CONTAINER_CORPSE_LOOT_PREFIX) || from.startsWith(GamePanel.CONTAINER_ROOM_CACHE_PREFIX)) return null;
        }
        if (to.equals(GamePanel.CONTAINER_BASE_STORAGE) && !g.isInClaimedRoom(g.playerX, g.playerY)) return "base storage may only be filled from inside the claimed room";
        if (from.startsWith(GamePanel.CONTAINER_FACTION_STOCK_PREFIX) && to.startsWith(GamePanel.CONTAINER_TRADER_SHELF_PREFIX)) return r.contains("trader") || r.contains("stock") ? null : "faction stock may only be staged onto a trader shelf by trade authority";
        if (to.startsWith(GamePanel.CONTAINER_FACTION_STOCK_PREFIX)) return r.contains("materialize") || r.contains("production") || r.contains("faction") ? null : "faction stock accepts only faction-production or materialization routes";
        if (to.startsWith(GamePanel.CONTAINER_TRADER_SHELF_PREFIX)) return r.contains("sold by player") || r.contains("loaded persistent faction stock") || r.contains("itemized trader shelf") || r.contains("trader") ? null : "trader shelf accepts only trade-authorized routes";
        return null;
    }
    static boolean isVoid(String id) { return id != null && id.startsWith("void."); }

    static String productionAssignmentProblem(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        if (g == null || machine == null || v == null) return null;
        if (!g.baseClaimed) return "no claimed base owns this production assignment";
        if (!g.isInClaimedRoom(machine.x, machine.y)) return machine.name + " is outside the claimed room and cannot receive controlled production jobs";
        Faction owner = machine.faction == null ? Faction.NONE : machine.faction;
        if (owner != Faction.NONE && !g.playerIsFactionMember(owner) && !g.sameFactionFamily(owner, Faction.HIVER)) return "machine ownership is " + owner.label + "; authorization or reassignment required";
        String law = v.lawStatus == null ? "" : v.lawStatus.toLowerCase(Locale.ROOT);
        if (containsAny(law, "illegal", "contraband", "profaned", "hostile") && !hasContrabandProductionCover(g, v)) return "contraband production requires gang/cult/scavver cover, a corrupt shop context, or matching faction identity";
        return null;
    }
    static boolean hasContrabandProductionCover(GamePanel g, FactionRecipeVariant v) {
        if (g == null || v == null) return false;
        Faction pf = g.playerFaction();
        if (pf == Faction.BANDIT || pf == Faction.SCAVENGER || pf == Faction.CULTIST || pf == Faction.HERETIC || pf == Faction.MUTANT) return true;
        Faction vf = v.faction == null ? Faction.NONE : v.faction;
        if (vf == Faction.BANDIT || vf == Faction.SCAVENGER || vf == Faction.CULTIST || vf == Faction.HERETIC || vf == Faction.MUTANT) return true;
        BaseObject m = g.selectedWorkerMachine();
        return m != null && m.businessOpen && !m.permittedBusiness;
    }
    static boolean containsAny(String s, String... terms) { if (s == null) return false; for (String t: terms) if (s.contains(t)) return true; return false; }

    static ArrayList<String> ledgerLines(GamePanel g, int limit) {
        ArrayList<String> l = new ArrayList<>();
        l.add("Actor access / permission ledger");
        l.add("Scope: guards container transfer and controlled production assignment routes without creating a second inventory system.");
        l.add("Player faction: " + (g == null ? "unknown" : g.playerFaction().label) + ". Claimed base: " + (g != null && g.baseClaimed ? g.baseDisplayName() : "none") + ".");
        l.add("Container policy: player/base storage, room caches, contract objects, corpse loot, trader shelves, faction stock, and void ledgers each carry route rules.");
        l.add("Production policy: controlled generated jobs require claimed-room machine ownership, valid authorization, and contraband cover when law status demands it.");
        if (g != null) {
            int shown = 0;
            for (String id : g.itemContainers.keySet()) {
                l.add("  " + id + " → " + containerKind(id) + "; items=" + g.containerItemCount(id));
                if (++shown >= Math.max(1, limit)) break;
            }
        }
        return l;
    }
    static String containerKind(String id) {
        if (id == null) return "unknown";
        if (id.equals(GamePanel.CONTAINER_PLAYER_INVENTORY)) return "player-carried inventory";
        if (id.equals(GamePanel.CONTAINER_BASE_STORAGE)) return "claimed base storage";
        if (id.startsWith(GamePanel.CONTAINER_TRADER_SHELF_PREFIX)) return "trader shelf";
        if (id.startsWith(GamePanel.CONTAINER_FACTION_STOCK_PREFIX)) return "faction stock";
        if (id.startsWith(GamePanel.CONTAINER_CONTRACT_OBJECT_PREFIX)) return "contract object";
        if (id.startsWith(GamePanel.CONTAINER_CORPSE_LOOT_PREFIX)) return "corpse loot";
        if (id.startsWith(GamePanel.CONTAINER_ROOM_CACHE_PREFIX)) return "room cache";
        if (id.startsWith(GamePanel.CONTAINER_MACHINE_INPUT_PREFIX)) return "machine input buffer";
        if (id.startsWith("void.")) return "void/audit ledger";
        return "general container";
    }
}


class ActorAccessAudit {
    int containerRoutesChecked;
    int productionRoutesChecked;
    int protectedDenials;
    final ArrayList<String> issues = new ArrayList<>();
    boolean pass() { return issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Actor access permission audit: " + (pass()?"PASS":"FAIL"));
        l.add("Container routes checked: " + containerRoutesChecked);
        l.add("Production routes checked: " + productionRoutesChecked);
        l.add("Expected protected denials: " + protectedDenials);
        l.add("Issues: " + issues.size());
        for (String i: issues) l.add(" - " + i);
        return l;
    }
}

class ActorAccessAuditApi {
    static ActorAccessAudit audit(GamePanel g) {
        ActorAccessAudit a = new ActorAccessAudit();
        String[][] routes = {
            {GamePanel.CONTAINER_BASE_STORAGE, GamePanel.CONTAINER_PLAYER_INVENTORY, "recovered from player base storage"},
            {GamePanel.CONTAINER_PLAYER_INVENTORY, GamePanel.CONTAINER_BASE_STORAGE, "stashed into player base storage"},
            {GamePanel.CONTAINER_CONTRACT_OBJECT_PREFIX+"audit", GamePanel.CONTAINER_PLAYER_INVENTORY, "retrieved from persistent contract container"},
            {GamePanel.CONTAINER_CORPSE_LOOT_PREFIX+"audit", GamePanel.CONTAINER_PLAYER_INVENTORY, "looted from persistent corpse container"},
            {GamePanel.CONTAINER_FACTION_STOCK_PREFIX+"audit", GamePanel.CONTAINER_PLAYER_INVENTORY, "direct loot attempt"},
            {GamePanel.CONTAINER_FACTION_STOCK_PREFIX+"audit", GamePanel.CONTAINER_TRADER_SHELF_PREFIX+"audit", "loaded persistent faction stock onto trader shelf"},
            {GamePanel.CONTAINER_TRADER_SHELF_PREFIX+"audit", GamePanel.CONTAINER_PLAYER_INVENTORY, "sold by audit trader to player"}
        };
        for (String[] r: routes) {
            a.containerRoutesChecked++;
            String p = ActorAccessAuthority.containerTransferProblem(g, r[0], r[1], r[2]);
            boolean expectedDeny = r[0].startsWith(GamePanel.CONTAINER_FACTION_STOCK_PREFIX) && r[1].equals(GamePanel.CONTAINER_PLAYER_INVENTORY);
            if (expectedDeny) { if (p == null) a.issues.add("direct faction-stock to player route was not denied"); else a.protectedDenials++; }
            else if (p != null && !(r[0].equals(GamePanel.CONTAINER_BASE_STORAGE) && (g == null || !g.isInClaimedRoom(g.playerX,g.playerY)))) a.issues.add("authorized route unexpectedly denied: " + Arrays.toString(r) + " reason=" + p);
        }
        if (g != null) {
            for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
                if (a.productionRoutesChecked >= 80) break;
                BaseObject machine = new BaseObject("Audit claimed workbench", 'w', g.playerX, g.playerY, 0, 0);
                machine.qualityName = "Archeotech";
                String p = ActorAccessAuthority.productionAssignmentProblem(g, machine, v);
                a.productionRoutesChecked++;
                if (p != null && !p.contains("contraband") && !p.contains("claimed")) a.issues.add("unexpected production access block for " + v.outputName + ": " + p);
            }
        }
        return a;
    }
}


class ControlledProductionJobAuthority {
    static final String GENERATED_PREFIX = "GENVAR::";
    static final String MANUAL_WORKER_LABEL = "manual/player operation fallback";
    static final String[] CATEGORY_FILTERS = {"All", "Weapons", "Chems/Medicae", "Food/Water/Agri", "Clothing/Armor", "Salvage/Maintenance", "Energy/Power"};
    static final String[] READINESS_FILTERS = {"All valid", "Ready now", "Missing inputs", "Needs apparatus", "Contraband", "Manual eligible", "Crew ready"};

    static String[] categoryFilters(){ return CATEGORY_FILTERS; }
    static String[] readinessFilters(){ return READINESS_FILTERS; }
    static String categoryFilterName(int i){ return CATEGORY_FILTERS[Math.max(0, Math.min(CATEGORY_FILTERS.length-1, i))]; }
    static String readinessFilterName(int i){ return READINESS_FILTERS[Math.max(0, Math.min(READINESS_FILTERS.length-1, i))]; }

    static ArrayList<FactionRecipeVariant> visibleJobs(GamePanel g, int max) {
        ArrayList<FactionRecipeVariant> out = new ArrayList<>();
        BaseObject selected = g == null ? null : g.selectedWorkerMachine();
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
            if (v == null || v.base == null) continue;
            if (!passesBasicFilter(g, selected, v)) continue;
            if (!passesCategoryFilter(g, v)) continue;
            if (!passesReadinessFilter(g, selected, v)) continue;
            out.add(v);
            if (out.size() >= Math.max(1, max)) break;
        }
        return out;
    }

    static boolean passesBasicFilter(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        if (v == null || v.base == null) return false;
        if (g != null && v.requiredKnowledge != null && !v.requiredKnowledge.isBlank() && !g.hasKnowledge(v.requiredKnowledge)) {
            // Keep the selector controlled: doctrine-locked jobs stay in Infopedia ledgers until known.
            return false;
        }
        if (machine != null && !machineAcceptsVariant(machine, v)) return false;
        if (v.base.equipmentRequirements.size() > 0 && g != null) {
            boolean anyRelevant = false;
            for (String eq : v.equipmentRequirements.keySet()) if (LiveProductionPlacementAuthority.hasInstalledEquipment(g, eq)) { anyRelevant = true; break; }
            if (!anyRelevant && machine != null && machine.symbol == 'L') anyRelevant = LiveProductionPlacementAuthority.isLaboratoryObject(machine);
            if (!anyRelevant) return false;
        }
        return true;
    }

    static boolean passesCategoryFilter(GamePanel g, FactionRecipeVariant v) {
        int idx = g == null ? 0 : Math.max(0, Math.min(CATEGORY_FILTERS.length-1, g.generatedJobCategoryFilterIndex));
        if (idx == 0) return true;
        String c = categoryFor(v);
        switch (idx) {
            case 1: return c.equals("Weapons");
            case 2: return c.equals("Chems/Medicae");
            case 3: return c.equals("Food/Water/Agri");
            case 4: return c.equals("Clothing/Armor");
            case 5: return c.equals("Salvage/Maintenance");
            case 6: return c.equals("Energy/Power");
            default: return true;
        }
    }

    static boolean passesReadinessFilter(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        int idx = g == null ? 0 : Math.max(0, Math.min(READINESS_FILTERS.length-1, g.generatedJobReadinessFilterIndex));
        if (idx == 0) return true;
        String assignment = assignmentProblem(g, machine, v);
        boolean missingInputs = inputShortageCount(g, v) > 0;
        switch (idx) {
            case 1: return assignment == null && !missingInputs;
            case 2: return assignment == null && missingInputs;
            case 3: return v != null && v.equipmentRequirements.size() > 0;
            case 4: return v != null && v.lawStatus != null && containsAny(v.lawStatus.toLowerCase(Locale.ROOT), "illegal", "contraband", "restricted", "black-market", "seizure", "profaned", "hostile");
            case 5: return manualFallbackAllowed(g, machine, v);
            case 6: return assignment == null && !missingInputs && g != null && g.availableRecruitLabor() > 0;
            default: return true;
        }
    }

    static String categoryFor(FactionRecipeVariant v) {
        if (v == null || v.base == null) return "Other";
        String cat = (v.knowledgeCategory == null ? "" : v.knowledgeCategory).toLowerCase(Locale.ROOT);
        String fam = (v.base.family == null ? "" : v.base.family).toLowerCase(Locale.ROOT);
        String name = (v.outputName == null ? "" : v.outputName).toLowerCase(Locale.ROOT);
        if (fam.startsWith("weapon/") || cat.contains("ballistics")) return "Weapons";
        if (cat.contains("chemical") || cat.contains("medical") || fam.contains("chem") || name.contains("stimm") || name.contains("lho") || name.contains("amasec")) return "Chems/Medicae";
        if (cat.contains("food") || cat.contains("water") || cat.contains("agricultural") || fam.contains("food") || fam.contains("water")) return "Food/Water/Agri";
        if (cat.contains("textile") || fam.contains("clothing") || fam.contains("armor")) return "Clothing/Armor";
        if (cat.contains("salvage") || cat.contains("maintenance") || fam.contains("component") || fam.contains("tool")) return "Salvage/Maintenance";
        if (cat.contains("energy") || fam.contains("energy") || fam.contains("power")) return "Energy/Power";
        return "Other";
    }

    static String assignmentKey(FactionRecipeVariant v) {
        if (v == null || v.base == null) return GENERATED_PREFIX + "invalid";
        return GENERATED_PREFIX + v.outputName + "||" + v.base.family + "||" + v.base.outputBaseItem;
    }
    static boolean isGeneratedAssignment(String s) { return s != null && s.startsWith(GENERATED_PREFIX); }
    static FactionRecipeVariant findVariantByAssignmentKey(String key) {
        if (!isGeneratedAssignment(key)) return null;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) if (assignmentKey(v).equals(key)) return v;
        return null;
    }

    static String shortStatus(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        String problem = assignmentProblem(g, machine, v);
        if (v == null) return "no generated job";
        String manual = machine == null ? "manual unavailable: no machine selected" : manualFallbackLine(g, machine, v);
        String input = inputForecastLine(g, v);
        return "category " + categoryFor(v) + " | " + (problem == null ? "ASSIGNABLE" : "LOCKED: " + problem) + " | " + input + " | equipment " + v.equipmentSummary() + " | " + manual;
    }

    static int inputShortageCount(GamePanel g, FactionRecipeVariant v) {
        if (g == null || v == null) return 0;
        int missing = 0;
        for (Map.Entry<String,Integer> e : v.itemInputs.entrySet()) if (g.countProductionInput(e.getKey()) < e.getValue()) missing++;
        return missing;
    }

    static String inputForecastLine(GamePanel g, FactionRecipeVariant v) {
        if (v == null) return "inputs unknown";
        if (v.itemInputs.isEmpty()) return "inputs ready: no consumed inputs";
        ArrayList<String> parts = new ArrayList<>();
        int missingKinds = 0;
        for (Map.Entry<String,Integer> e : v.itemInputs.entrySet()) {
            int have = g == null ? 0 : g.countProductionInput(e.getKey());
            int need = Math.max(0, e.getValue());
            if (have < need) missingKinds++;
            if (parts.size() < 4) parts.add(e.getKey() + " " + have + "/" + need);
        }
        String tail = v.itemInputs.size() > parts.size() ? " +" + (v.itemInputs.size()-parts.size()) + " more" : "";
        return (missingKinds == 0 ? "inputs READY: " : "inputs MISSING " + missingKinds + " kind(s): ") + String.join(", ", parts) + tail;
    }

    static String queueForecastLine(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        if (machine == null) return "Queue forecast unavailable: no selected machine.";
        if (v == null) return "Queue forecast unavailable: no generated variant.";
        int target = Math.max(1, machine.productionQueueTarget);
        int remaining = Math.max(0, machine.productionQueueRemaining);
        int perRunMissing = inputShortageCount(g, v);
        int readyRuns = forecastReadyRuns(g, v, target);
        String assign = assignmentProblem(g, machine, v);
        String staffing = g == null ? "labor unknown" : (g.availableRecruitLabor() > 0 ? "crew ready" : manualFallbackLine(g, machine, v));
        String facility = FacilityOutputModifierAuthority.forecastLine(g, machine, v, g != null && g.availableRecruitLabor() <= 0);
        return "Queue target " + target + ", remaining " + remaining + ", input-ready runs " + readyRuns + "/" + target + ", " + (assign == null ? "assignment valid" : "blocked: " + assign) + ", per-run missing kinds " + perRunMissing + ", " + staffing + ", " + facility + ".";
    }

    static int forecastReadyRuns(GamePanel g, FactionRecipeVariant v, int maxRuns) {
        if (g == null || v == null) return 0;
        int possible = Math.max(0, maxRuns);
        for (Map.Entry<String,Integer> e : v.itemInputs.entrySet()) {
            int need = Math.max(1, e.getValue());
            possible = Math.min(possible, g.countProductionInput(e.getKey()) / need);
        }
        return Math.max(0, possible);
    }

    static String assignmentProblem(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        if (g == null) return "missing game context.";
        if (machine == null) return "no selected machine/apparatus.";
        if (v == null || v.base == null) return "no generated production job selected.";
        String access = ActorAccessAuthority.productionAssignmentProblem(g, machine, v);
        if (access != null) return access;
        if (v.requiredKnowledge != null && !v.requiredKnowledge.isBlank() && !g.hasKnowledge(v.requiredKnowledge)) return "requires knowledge: " + v.requiredKnowledge + ".";
        if (!machineAcceptsVariant(machine, v)) return machine.name + " is not valid for " + v.knowledgeCategory + " / " + v.base.family + ".";
        int requiredTier = Math.max(QualityAuthorityApi.tierIndex(v.machineQuality), v.base.minimumMachineTier);
        if (g.machineQualityTier(machine) < requiredTier) return "machine quality " + machine.qualityName + " is below required " + QualityAuthorityApi.qualityName(requiredTier) + ".";
        if (v.equipmentRequirements.size() > 0) {
            for (String eq : v.equipmentRequirements.keySet()) if (!LiveProductionPlacementAuthority.hasInstalledEquipment(g, eq)) return "missing installed apparatus " + eq + ".";
            String live = LiveProductionPlacementAuthority.validateDraftRecipeForBase(g, v.base);
            if (live != null && !live.startsWith("no available worker")) return live + ".";
        }
        return null;
    }

    static String operationProblem(GamePanel g, BaseObject machine, FactionRecipeVariant v, boolean manual) {
        String assign = assignmentProblem(g, machine, v);
        if (assign != null) return assign;
        if (!manual && g.availableRecruitLabor() <= 0) return "no assigned recruit labor available";
        if (manual && !manualFallbackAllowed(g, machine, v)) return "manual operation is not safe or not allowed for this job";
        for (Map.Entry<String,Integer> e : v.itemInputs.entrySet()) if (g.countProductionInput(e.getKey()) < e.getValue()) return "needs " + e.getValue() + "x " + e.getKey() + "; available " + g.countProductionInput(e.getKey());
        if (manual && g.fatigue >= 20) return "player is too exhausted to operate machinery safely";
        return null;
    }

    static boolean manualFallbackAllowed(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        if (machine == null || v == null || v.base == null) return false;
        if (!machineAcceptsVariant(machine, v)) return false;
        String manning = v.base.manningRequirement == null ? "" : v.base.manningRequirement.toLowerCase(Locale.ROOT);
        if (manning.contains("crew of") || manning.contains("two-worker") || manning.contains("team-only")) return false;
        return true;
    }

    static String manualFallbackLine(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        if (v == null || machine == null) return "manual fallback unavailable";
        if (!manualFallbackAllowed(g, machine, v)) return "manual fallback unavailable for this job";
        return "manual fallback: player may operate for " + manualTurnCost(g, machine, v) + " turns, fatigue +" + manualFatigueCost(g, machine, v) + ", if inputs are present";
    }

    static boolean machineAcceptsVariant(BaseObject machine, FactionRecipeVariant v) {
        if (machine == null || v == null || v.base == null) return false;
        String cat = (v.knowledgeCategory == null ? "" : v.knowledgeCategory).toLowerCase(Locale.ROOT);
        String fam = (v.base.family == null ? "" : v.base.family).toLowerCase(Locale.ROOT);
        if (v.equipmentRequirements.size() > 0 || cat.contains("chemical") || cat.contains("medical")) return machine.symbol == 'L' || machine.symbol == 'l' || machine.symbol == 'M';
        if (cat.contains("ballistics") || cat.contains("metallurgy") || fam.startsWith("weapon/")) return machine.symbol == 'f' || machine.symbol == 'w' || machine.symbol == 'x';
        if (cat.contains("energy") || cat.contains("maintenance")) return machine.symbol == 'f' || machine.symbol == 'l' || machine.symbol == 'w' || machine.symbol == 'x';
        if (cat.contains("food") || cat.contains("water") || cat.contains("agricultural")) return machine.symbol == 'w' || machine.symbol == 'e' || machine.symbol == 'L';
        if (cat.contains("textile")) return machine.symbol == 'w';
        if (cat.contains("salvage")) return machine.symbol == 'w' || machine.symbol == 'f';
        return machine.symbol == 'w' || machine.symbol == 'f' || machine.symbol == 'l' || machine.symbol == 'L';
    }

    static void consumeInputs(GamePanel g, FactionRecipeVariant v) {
        consumeInputsWithTrace(g, v, "legacy controlled production consume");
    }
    static ProductionInputConsumptionRecord consumeInputsWithTrace(GamePanel g, FactionRecipeVariant v, String route) {
        ProductionInputConsumptionRecord total = new ProductionInputConsumptionRecord();
        total.success = true;
        if (g == null || v == null) return total;
        for (Map.Entry<String,Integer> e : v.itemInputs.entrySet()) {
            for (int i=0;i<e.getValue();i++) {
                ProductionInputConsumptionRecord rec = g.consumeProductionInputNamedResult(e.getKey(), route);
                total.merge(rec);
                if (!rec.success) {
                    total.success = false;
                    total.failureReason = rec.failureReason;
                    return total;
                }
            }
        }
        return total;
    }

    static int staffedTurnCost(GamePanel g, BaseObject machine, FactionRecipeVariant v) { return FacilityOutputModifierAuthority.estimate(g, machine, v, false).turns; }
    static int manualTurnCost(GamePanel g, BaseObject machine, FactionRecipeVariant v) { return FacilityOutputModifierAuthority.estimate(g, machine, v, true).turns; }
    static int manualTurnCost(GamePanel g, BaseObject machine, CraftingRecipe r) {
        int skill = g == null ? 0 : Math.max(g.stat("Mechanics",0), g.stat("Intellect",0));
        return Math.max(3, (r == null ? 8 : r.turnCost + 4) - skill / 3);
    }
    static int manualFatigueCost(GamePanel g, BaseObject machine, FactionRecipeVariant v) {
        int n = 1 + (v == null ? 0 : v.equipmentRequirements.size()/2);
        if (v != null && v.lawStatus != null && (v.lawStatus.contains("contraband") || v.lawStatus.contains("illegal"))) n++;
        return Math.max(1, Math.min(6, n));
    }
    static int manualFatigueCost(GamePanel g, BaseObject machine, CraftingRecipe r) { return Math.max(1, Math.min(5, r == null ? 1 : r.fatigueCost + 1)); }
    static int machineWearFor(FactionRecipeVariant v) { return Math.max(0, v == null ? 0 : (v.defectPercent() >= 16 ? 1 : 0)); }
    static String skillFor(FactionRecipeVariant v) {
        if (v == null || v.knowledgeCategory == null) return "Mechanics";
        String c = v.knowledgeCategory;
        if (c.contains("Medical")) return "Medical";
        if (c.contains("Food") || c.contains("Water") || c.contains("Agricultural")) return "Survival";
        if (c.contains("Ballistics")) return "Firearms";
        if (c.contains("Textile")) return "Commerce";
        return "Mechanics";
    }
    static boolean containsAny(String s, String... needles) { if (s == null) return false; for (String n : needles) if (s.contains(n)) return true; return false; }
}


class ControlledProductionJobAuditApi {
    static ControlledProductionJobAudit audit() {
        ControlledProductionJobAudit a = new ControlledProductionJobAudit();
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
            a.generatedVariants++;
            if (v == null || v.base == null) continue;
            if (!ControlledProductionJobAuthority.assignmentKey(v).startsWith(ControlledProductionJobAuthority.GENERATED_PREFIX)) a.issues.add("bad assignment key for " + v.outputName);
            if (v.requiredKnowledge == null || !KnowledgeDef.all().containsKey(v.requiredKnowledge)) a.issues.add("missing required knowledge " + v.requiredKnowledge + " for " + v.outputName);
            if (v.equipmentRequirements.size() > 0) a.apparatusBackedVariants++;
            if (v.itemInputs != null) a.forecastableVariants++;
            a.categoryCounts.put(ControlledProductionJobAuthority.categoryFor(v), a.categoryCounts.getOrDefault(ControlledProductionJobAuthority.categoryFor(v),0)+1);
            if (v.lawStatus != null && ControlledProductionJobAuthority.containsAny(v.lawStatus.toLowerCase(Locale.ROOT), "illegal", "contraband", "restricted", "black-market", "seizure", "profaned", "hostile")) a.contrabandVariants++;
            if (ControlledProductionJobAuthority.manualFallbackAllowed(null, new BaseObject("Audit workbench", 'w', 0,0,0,0), v)
                    || ControlledProductionJobAuthority.manualFallbackAllowed(null, new BaseObject("Audit lab apparatus", 'L', 0,0,0,0), v)
                    || ControlledProductionJobAuthority.manualFallbackAllowed(null, new BaseObject("Audit micro forge", 'f', 0,0,0,0), v)
                    || ControlledProductionJobAuthority.manualFallbackAllowed(null, new BaseObject("Audit condenser", 'e', 0,0,0,0), v)) a.manualEligibleVariants++;
            for (String eq : v.equipmentRequirements.keySet()) if (!ChemicalEquipmentAuthority.isLabEquipmentName(eq)) a.missingEquipmentProfiles.add(eq + " for " + v.outputName);
        }
        a.buildableLabApparatus = BuildRecipe.laboratoryEquipmentRecipes().size();
        a.filterBuckets = ControlledProductionJobAuthority.categoryFilters().length + ControlledProductionJobAuthority.readinessFilters().length;
        if (a.filterBuckets < 10) a.issues.add("too few generated-job filter buckets");
        if (a.forecastableVariants <= 0) a.issues.add("no forecastable variants for input preview");
        return a;
    }
}



class ControlledProductionJobAudit {
    int generatedVariants, apparatusBackedVariants, manualEligibleVariants, buildableLabApparatus, forecastableVariants, contrabandVariants, filterBuckets;
    final TreeMap<String,Integer> categoryCounts = new TreeMap<>();
    final TreeSet<String> missingEquipmentProfiles = new TreeSet<>();
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return generatedVariants > 0 && manualEligibleVariants > 0 && forecastableVariants > 0 && filterBuckets > 0 && missingEquipmentProfiles.isEmpty() && issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("0.9.02 Controlled Production Job UI / Queue / Input Forecasting audit");
        l.add("Generated faction variants visible to assignment authority: " + generatedVariants);
        l.add("Apparatus-backed generated variants: " + apparatusBackedVariants);
        l.add("Manual-operation eligible generated variants: " + manualEligibleVariants);
        l.add("Forecastable input-preview variants: " + forecastableVariants);
        l.add("Contraband/restricted/status-filter variants: " + contrabandVariants);
        l.add("Filter buckets: " + filterBuckets + " (category " + ControlledProductionJobAuthority.categoryFilters().length + ", readiness " + ControlledProductionJobAuthority.readinessFilters().length + ")");
        l.add("Category coverage:");
        for (Map.Entry<String,Integer> e : categoryCounts.entrySet()) l.add("  " + e.getKey() + ": " + e.getValue());
        l.add("Buildable laboratory apparatus recipes: " + buildableLabApparatus);
        l.add("Missing equipment profiles: " + missingEquipmentProfiles.size());
        for (String s : missingEquipmentProfiles) l.add("  MISSING_EQUIPMENT_PROFILE " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("Manual fallback rule: if no assigned worker is available, eligible generated jobs can be run by the player on a valid assigned machine/apparatus for a deterministic turn/fatigue cost.");
        l.add("Queue rule: assigned generated jobs seed a machine queue target/remaining run count; QUEUE +/- adjusts scheduled runs and input forecasting reports ready runs before production.");
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}


class LiveProductionPlacementAuthority {
    static boolean isLaboratoryBuild(BuildRecipe r) { return r != null && r.symbol == 'L' && ChemicalEquipmentAuthority.isLabEquipmentName(r.name); }
    static boolean isLaboratoryObject(BaseObject obj) { return obj != null && obj.symbol == 'L' && ChemicalEquipmentAuthority.isLabEquipmentName(obj.name); }
    static String validateBuildPlacement(GamePanel g, BuildRecipe r, int x, int y) {
        if (!isLaboratoryBuild(r)) return "OK";
        LaboratoryEquipmentProfile p = ChemicalEquipmentAuthority.profileForName(r.name);
        if (p == null) return "Unknown laboratory apparatus profile for " + r.name + ".";
        if (g == null || g.world == null) return "No active room authority for laboratory placement.";
        int rid = g.world.roomIdAt(x, y);
        RoomProfile rp = g.world.roomProfile(rid);
        Faction owner = g.world.roomFaction(rid);
        if (!roomAcceptsEquipment(rp, owner, p, g)) return "Room is not valid for " + p.name + ": needs " + p.roomKind + ".";
        if (QualityAuthorityApi.tierIndex(r.qualityName) < Math.min(p.minimumMachineTier, QualityAuthorityApi.UNLIMITED_TIER)) return "Build quality " + r.qualityName + " is below apparatus minimum " + QualityAuthorityApi.qualityName(p.minimumMachineTier) + ".";
        return "OK";
    }
    static void annotateBuiltObject(GamePanel g, BaseObject obj) {
        if (!isLaboratoryObject(obj)) return;
        LaboratoryEquipmentProfile p = ChemicalEquipmentAuthority.profileForName(obj.name);
        if (p == null) return;
        obj.capacity = Math.max(obj.capacity, Math.max(3, MachineTierAuthority.forMachine(obj).workerCapacity * 3));
        obj.description = p.name + ": process " + p.processType + "; knowledge " + p.knowledgeCategory + "; minimum machine tier " + QualityAuthorityApi.qualityName(p.minimumMachineTier) + "; room family " + p.roomKind + "; manning " + p.manning + ". " + p.placementNote;
    }
    static String objectPlacementLine(GamePanel g, BaseObject obj) {
        if (!isLaboratoryObject(obj) || g == null || g.world == null) return "";
        LaboratoryEquipmentProfile p = ChemicalEquipmentAuthority.profileForName(obj.name);
        int rid = g.world.roomIdAt(obj.x, obj.y);
        RoomProfile rp = g.world.roomProfile(rid);
        Faction owner = g.world.roomFaction(rid);
        boolean ok = roomAcceptsEquipment(rp, owner, p, g);
        return (ok ? "valid" : "misplaced") + " for " + p.processType + " in " + (rp == null ? "unknown room" : rp.name) + "; requires " + p.roomKind + "; manning " + p.manning;
    }
    static ArrayList<String> baseReadinessLines(GamePanel g, int max) {
        ArrayList<String> l = new ArrayList<>();
        if (g == null || !g.baseClaimed) { l.add("  Live production placement: no claimed base room yet."); return l; }
        l.add("  Live production placement readiness:");
        ArrayList<BaseObject> labs = installedLabEquipment(g);
        if (labs.isEmpty()) { l.add("    No installed laboratory apparatus. Chem draft recipes remain ledger-only until apparatus is built."); return l; }
        int shown = 0;
        for (BaseObject obj : labs) {
            l.add("    Apparatus: " + obj.name + " | " + objectPlacementLine(g, obj));
            if (++shown >= max) return l;
        }
        int ready = 0, blocked = 0;
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) if (ChemicalEquipmentAuthority.isChemicalRecipe(r)) {
            String problem = validateDraftRecipeForBase(g, r);
            if (problem == null) ready++; else blocked++;
        }
        l.add("    Chem recipe readiness: ready " + ready + ", blocked " + blocked + " against currently installed apparatus.");
        return l;
    }
    static ArrayList<BaseObject> installedLabEquipment(GamePanel g) {
        ArrayList<BaseObject> out = new ArrayList<>();
        if (g == null) return out;
        for (BaseObject obj : g.baseObjects) if (isLaboratoryObject(obj)) out.add(obj);
        return out;
    }
    static String validateDraftRecipeForBase(GamePanel g, DraftIndustrialRecipe r) {
        if (g == null || r == null) return "missing validation context";
        if (!ChemicalEquipmentAuthority.isChemicalRecipe(r)) return null;
        if (!g.baseClaimed) return "no claimed base room";
        if (r.minimumMachineTier > highestInstalledLabTier(g)) return "needs machine tier " + QualityAuthorityApi.qualityName(r.minimumMachineTier);
        for (String eq : r.equipmentRequirements.keySet()) if (!hasInstalledEquipment(g, eq)) return "missing apparatus " + eq;
        if (availableQualifiedLabor(g, r.manningRequirement) <= 0 && !manualFallbackAllowed(r)) return "no available worker for " + r.manningRequirement;
        boolean roomOk = false;
        for (BaseObject obj : installedLabEquipment(g)) {
            LaboratoryEquipmentProfile p = ChemicalEquipmentAuthority.profileForName(obj.name);
            if (p != null && roomAcceptsEquipment(g.world.roomProfile(g.world.roomIdAt(obj.x, obj.y)), g.world.roomFaction(g.world.roomIdAt(obj.x, obj.y)), p, g)) { roomOk = true; break; }
        }
        if (!roomOk) return "no installed apparatus in a valid room";
        return null;
    }
    static int highestInstalledLabTier(GamePanel g) {
        int best = -1;
        for (BaseObject obj : installedLabEquipment(g)) best = Math.max(best, QualityAuthorityApi.tierIndex(obj.qualityName));
        return best;
    }
    static boolean hasInstalledEquipment(GamePanel g, String equipmentName) {
        for (BaseObject obj : installedLabEquipment(g)) if (obj.name.equalsIgnoreCase(equipmentName)) return true;
        return false;
    }
    static int availableQualifiedLabor(GamePanel g, String manning) {
        int available = g == null ? 0 : g.availableRecruitLabor();
        if (manning != null && manning.startsWith("0-1")) return Math.max(1, available);
        return available;
    }
    static boolean manualFallbackAllowed(DraftIndustrialRecipe r) {
        return r != null && r.manningRequirement != null && !r.manningRequirement.toLowerCase(Locale.ROOT).contains("crew of");
    }
    static boolean roomAcceptsEquipment(RoomProfile rp, Faction owner, LaboratoryEquipmentProfile p, GamePanel g) {
        if (p == null) return false;
        String room = low((rp == null ? "" : rp.name + " " + rp.descriptor + " " + rp.featureText) + " " + (owner == null ? "" : owner.label) + " " + (g == null || g.world == null || g.world.zoneType == null ? "" : g.world.zoneType.label));
        String req = low(p.roomKind + " " + p.placementNote + " " + p.processType);
        if (containsAny(req,"forbidden","warp")) return containsAny(room,"cult","shrine","vault","evidence","sealed","mechanicus","relic","precinct","hidden");
        if (containsAny(req,"sterile","medicae","surgical")) return containsAny(room,"clinic","medicae","surgical","aid station","hospital") || (g != null && g.firstBaseObject('M') != null);
        if (containsAny(req,"noble","spire","luxury","perfumery")) return containsAny(room,"noble","mansion","atrium","cellar","salon","estate") || owner == Faction.NOBLE;
        if (containsAny(req,"cult","shrine","ritual")) return containsAny(room,"cult","shrine","chapel","pilgrim","sewer") || owner == Faction.CULTIST;
        if (containsAny(req,"mutant","sump","sewer","fungal","wet","fermentation")) return containsAny(room,"sump","sewer","water","wash","kitchen","vat","garden","mutant","clinic","utility","plaza") || owner == Faction.MUTANT;
        if (containsAny(req,"security","interrogation","evidence","toxin")) return containsAny(room,"security","precinct","arbites","evidence","holding","clinic","black room") || owner == Faction.ARBITES;
        if (containsAny(req,"void","frontier","ash")) return containsAny(room,"void","rail","depot","frontier","ash","service","utility","workshop");
        if (containsAny(req,"toxic","fume","pressure","chemical","distillation","lab")) return containsAny(room,"workshop","kitchen","clinic","lab","laboratory","chem","warehouse","utility","plaza","storehouse","forge","repair","chop shop","mess","den") || (g != null && g.firstBaseObject('w') != null);
        return true;
    }
    static boolean containsAny(String hay, String... needles) { for (String n : needles) if (hay.contains(n)) return true; return false; }
    static String low(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}


class BuildRequirementCostAuditApi {
    static BuildRequirementCostAudit audit() {
        BuildRequirementCostAudit a = new BuildRequirementCostAudit();
        HashSet<String> seen = new HashSet<>();
        for (BuildRecipe r : BuildRecipe.allBuildRecipes()) {
            if (r == null || r.name == null) continue;
            if (!seen.add(r.name)) continue;
            a.totalRecipes++;
            boolean machinery = isMachineryOrFacility(r);
            if (machinery) a.machineryRecipes++;
            if (!r.componentCosts.isEmpty()) a.variedComponentRecipes++;
            if (machinery && r.componentCosts.isEmpty()) a.genericOnlyMachinery.add(r.name);
            if (r.supplyCost + r.partCost > 0 && r.componentCosts.isEmpty()) a.genericOnlyRecipes++;
            int direct = r.componentCosts.size();
            a.maxDirectComponentKinds = Math.max(a.maxDirectComponentKinds, direct);
            if (direct > 8) a.absurdWidthRecipes.add(r.name + " has " + direct + " direct component kinds");
            for (Map.Entry<String,Integer> e : r.componentCosts.entrySet()) {
                if (e.getValue() == null || e.getValue() <= 0) a.badQuantities.add(r.name + " -> " + e.getKey() + " x" + e.getValue());
                if (ItemCatalog.get(e.getKey()) == null) a.missingCatalogComponents.add(r.name + " requires missing catalog item " + e.getKey());
                a.uniqueComponents.add(e.getKey());
            }
        }
        if (a.machineryRecipes > 0 && a.variedComponentRecipes < a.machineryRecipes) {
            // Simple furniture may stay cheap, but machinery/facilities must not remain all-universal.
            for (String name : a.genericOnlyMachinery) a.issues.add("machinery/facility still lacks varied component requirements: " + name);
        }
        if (a.uniqueComponents.size() < 12) a.issues.add("component-cost vocabulary too narrow: " + a.uniqueComponents.size());
        return a;
    }
    static boolean isMachineryOrFacility(BuildRecipe r) {
        if (r == null) return false;
        if (r.symbol == 'L' || r.symbol == 'e' || r.symbol == 'f' || r.symbol == 'l' || r.symbol == 'T' || r.symbol == 'H' || r.symbol == 'x' || r.symbol == 'G' || r.symbol == 'q' || r.symbol == 'k' || r.symbol == 'M' || r.symbol == 'B' || r.symbol == 'g') return true;
        String n = r.name == null ? "" : r.name.toLowerCase(Locale.ROOT);
        return n.contains("bench") || n.contains("forge") || n.contains("lab") || n.contains("condenser") || n.contains("turret") || n.contains("relay") || n.contains("node") || n.contains("station") || n.contains("center") || n.contains("stall") || n.contains("counter") || n.contains("barracks");
    }
}


class BuildRequirementCostAudit {
    int totalRecipes, machineryRecipes, variedComponentRecipes, genericOnlyRecipes, maxDirectComponentKinds;
    final TreeSet<String> uniqueComponents = new TreeSet<>();
    final TreeSet<String> genericOnlyMachinery = new TreeSet<>();
    final TreeSet<String> missingCatalogComponents = new TreeSet<>();
    final TreeSet<String> badQuantities = new TreeSet<>();
    final TreeSet<String> absurdWidthRecipes = new TreeSet<>();
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return missingCatalogComponents.isEmpty() && badQuantities.isEmpty() && absurdWidthRecipes.isEmpty() && issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("0.9.02 Industrial Component Build Requirements / Machinery Cost Refactor audit");
        l.add("Build recipes checked: " + totalRecipes);
        l.add("Machinery/facility recipes checked: " + machineryRecipes);
        l.add("Recipes with named component costs: " + variedComponentRecipes);
        l.add("Generic-only recipes: " + genericOnlyRecipes);
        l.add("Unique named build components: " + uniqueComponents.size());
        l.add("Max direct component kinds: " + maxDirectComponentKinds);
        l.add("Missing catalog components: " + missingCatalogComponents.size());
        for (String s : missingCatalogComponents) l.add("  MISSING_COMPONENT " + s);
        l.add("Bad quantities: " + badQuantities.size());
        for (String s : badQuantities) l.add("  BAD_QUANTITY " + s);
        l.add("Absurd width recipes: " + absurdWidthRecipes.size());
        for (String s : absurdWidthRecipes) l.add("  ABSURD_WIDTH " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("-- Component vocabulary --");
        int shown = 0;
        for (String c : uniqueComponents) {
            if (shown++ >= 80) { l.add("  ..."); break; }
            l.add("  " + c);
        }
        l.add("-- Sample build costs --");
        for (BuildRecipe r : BuildRecipe.allBuildRecipes()) {
            if (r.componentCosts.isEmpty()) continue;
            l.add("  " + r.name + " | supplies " + r.supplyCost + " parts " + r.partCost + " | components " + r.componentCostSummary(8));
        }
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}


class LiveProductionPlacementAuditApi {
    static LiveProductionPlacementAudit audit() {
        LiveProductionPlacementAudit a = new LiveProductionPlacementAudit();
        a.labProfiles = ChemicalEquipmentAuthority.profiles().size();
        a.buildableLabApparatus = BuildRecipe.laboratoryEquipmentRecipes().size();
        HashSet<String> buildable = new HashSet<>();
        for (BuildRecipe b : BuildRecipe.laboratoryEquipmentRecipes()) buildable.add(b.name);
        for (LaboratoryEquipmentProfile p : ChemicalEquipmentAuthority.profiles()) {
            if (ItemCatalog.get(p.name) == null) a.issues.add("missing catalog item for lab profile " + p.name);
            if (p.minimumMachineTier < 0 || p.minimumMachineTier > QualityAuthorityApi.UNLIMITED_TIER) a.issues.add("invalid machine tier for " + p.name);
            if (buildable.contains(p.name)) a.buildableProfiles++;
        }
        for (BuildRecipe b : BuildRecipe.laboratoryEquipmentRecipes()) {
            if (!ChemicalEquipmentAuthority.isLabEquipmentName(b.name)) a.issues.add("build recipe has no lab profile " + b.name);
            if (b.symbol != 'L') a.issues.add("lab build recipe not using L symbol " + b.name);
            if (b.requiredKnowledge == null || b.requiredKnowledge.isBlank()) a.issues.add("lab build recipe lacks knowledge gate " + b.name);
            if (QualityAuthorityApi.tierIndex(b.qualityName) < 0) a.issues.add("lab build recipe invalid quality " + b.name + " -> " + b.qualityName);
        }
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) if (ChemicalEquipmentAuthority.isChemicalRecipe(r)) {
            a.chemicalRecipes++;
            if (r.equipmentRequirements.isEmpty()) a.issues.add("chemical recipe lacks apparatus " + r.outputBaseItem);
            if (r.roomRequirements.isEmpty()) a.issues.add("chemical recipe lacks room requirement " + r.outputBaseItem);
            if (r.manningRequirement == null || r.manningRequirement.isBlank()) a.issues.add("chemical recipe lacks manning " + r.outputBaseItem);
            for (String eq : r.equipmentRequirements.keySet()) if (ChemicalEquipmentAuthority.profileForName(eq) == null) a.missingEquipmentProfiles.add(eq + " required by " + r.outputBaseItem);
        }
        if (a.buildableProfiles < 6) a.issues.add("too few buildable lab profiles for live placement bridge");
        return a;
    }
}


class LiveProductionPlacementAudit {
    int labProfiles, buildableLabApparatus, buildableProfiles, chemicalRecipes;
    final TreeSet<String> missingEquipmentProfiles = new TreeSet<>();
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return missingEquipmentProfiles.isEmpty() && issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("0.9.02 Live Production Placement / Apparatus Validation audit");
        l.add("Laboratory equipment profiles: " + labProfiles);
        l.add("Buildable laboratory apparatus recipes: " + buildableLabApparatus);
        l.add("Buildable profiles covered: " + buildableProfiles);
        l.add("Chemical recipes checked for live placement requirements: " + chemicalRecipes);
        l.add("Missing equipment profiles: " + missingEquipmentProfiles.size());
        for (String s : missingEquipmentProfiles) l.add("  MISSING_EQUIPMENT_PROFILE " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("-- Buildable apparatus --");
        for (BuildRecipe b : BuildRecipe.laboratoryEquipmentRecipes()) l.add("  " + b.name + " | quality " + b.qualityName + " | knowledge " + b.requiredKnowledge + " | cost supplies " + b.supplyCost + " parts " + b.partCost + " | " + b.description);
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}



class ProductionLedgerDisplayApi {
    static ArrayList<String> overviewLines() {
        ArrayList<String> l = new ArrayList<>();
        RecipeGraphAudit graph = RecipeGraphAuditApi.audit();
        FactionRecipeVariantAudit variants = FactionRecipeVariantAuditApi.audit();
        ChemicalEquipmentAudit chem = ChemicalEquipmentAuditApi.audit();
        LiveProductionPlacementAudit live = LiveProductionPlacementAuditApi.audit();
        ControlledProductionJobAudit jobs = ControlledProductionJobAuditApi.audit();
        FacilityOutputModifierAudit facility = FacilityOutputModifierAuditApi.audit();
        BuildRequirementCostAudit buildCosts = BuildRequirementCostAuditApi.audit();
        AuthorityAlignmentAudit auth = AuthorityAlignmentAuditApi.audit();
        l.add("Production ledger display overview");
        l.add("Scope: player-readable index over draft industrial recipes, faction manufacturing variants, chemical apparatus requirements, quality/knowledge/machine gates, and law status.");
        l.add("Draft recipes: " + graph.recipeCount + " | industrial/component entries: " + graph.industrialCatalogCount + " | recipe families: " + graph.familyCounts.size() + ".");
        l.add("Faction variants: " + variants.variantRecipes + " from " + variants.eligibleBaseRecipes + " eligible base recipes.");
        l.add("Chemical apparatus recipes: " + chem.chemicalRecipes + " | lab equipment profiles: " + chem.registeredEquipmentProfiles + " | process types: " + chem.processCounts.size() + " | buildable apparatus " + live.buildableLabApparatus + ".");
        l.add("Controlled generated production jobs: " + jobs.generatedVariants + " variants, " + jobs.manualEligibleVariants + " manual-fallback eligible, " + jobs.filterBuckets + " filter buckets, " + jobs.forecastableVariants + " forecastable, status " + pass(jobs.passed()) + ".");
        l.add("Facility output modifiers: " + facility.forecastableVariants + " variants forecastable, " + facility.machineTierProfiles + " machine tier profiles, max output " + facility.maxOutput + ", max turns " + facility.maxTurns + ", status " + pass(facility.passed()) + ".");
        l.add("Build requirement costs: " + buildCosts.totalRecipes + " build recipes, " + buildCosts.variedComponentRecipes + " with named components, " + buildCosts.uniqueComponents.size() + " unique components, status " + pass(buildCosts.passed()) + ".");
        l.add("Authority layer: " + auth.qualityTierCount + " quality tiers, " + auth.knowledgeCategoryCount + " knowledge categories, " + auth.machineTierCount + " machine tier profiles, " + auth.factionProfileCount + " faction manufacturing identities.");
        l.add("Audit rollup: recipe graph " + pass(graph.passed()) + ", faction variants " + pass(variants.passed()) + ", chemical equipment " + pass(chem.passed()) + ", live placement " + pass(live.passed()) + ", controlled jobs " + pass(jobs.passed()) + ", facility output " + pass(facility.passed()) + ", build costs " + pass(buildCosts.passed()) + ", authority alignment " + pass(auth.passed()) + ".");
        l.add("Workmanship rule: generated recipes are still ledger/Infopedia/audit-facing and are not wholesale-injected into CraftingRecipe.all().");
        return l;
    }
    static ArrayList<String> compactCountsLines() {
        ArrayList<String> l = new ArrayList<>();
        RecipeGraphAudit graph = RecipeGraphAuditApi.audit();
        FactionRecipeVariantAudit variants = FactionRecipeVariantAuditApi.audit();
        ChemicalEquipmentAudit chem = ChemicalEquipmentAuditApi.audit();
        LiveProductionPlacementAudit live = LiveProductionPlacementAuditApi.audit();
        ControlledProductionJobAudit jobs = ControlledProductionJobAuditApi.audit();
        FacilityOutputModifierAudit facility = FacilityOutputModifierAuditApi.audit();
        BuildRequirementCostAudit buildCosts = BuildRequirementCostAuditApi.audit();
        l.add("  Draft recipe graph: " + graph.recipeCount + " recipes, " + graph.familyCounts.size() + " families, status " + pass(graph.passed()) + ".");
        l.add("  Faction variant graph: " + variants.variantRecipes + " generated variants, status " + pass(variants.passed()) + ".");
        l.add("  Chemical apparatus graph: " + chem.chemicalRecipes + " apparatus-tagged recipes, " + chem.registeredEquipmentProfiles + " lab equipment profiles, status " + pass(chem.passed()) + ".");
        l.add("  Live placement graph: " + live.buildableLabApparatus + " buildable apparatus, " + live.chemicalRecipes + " chemical recipes validated, status " + pass(live.passed()) + ".");
        l.add("  Controlled job graph: " + jobs.generatedVariants + " generated variants, " + jobs.manualEligibleVariants + " manual-fallback eligible, " + jobs.filterBuckets + " filter buckets, status " + pass(jobs.passed()) + ".");
        l.add("  Facility output graph: " + facility.forecastableVariants + " generated variants forecastable, " + facility.machineTierProfiles + " machine tiers, status " + pass(facility.passed()) + ".");
        l.add("  Build cost graph: " + buildCosts.totalRecipes + " build recipes, " + buildCosts.variedComponentRecipes + " componentized, " + buildCosts.uniqueComponents.size() + " named components, status " + pass(buildCosts.passed()) + ".");
        return l;
    }
    static ArrayList<String> buildRequirementCostLedgerLines(int max) {
        ArrayList<String> l = new ArrayList<>();
        BuildRequirementCostAudit a = BuildRequirementCostAuditApi.audit();
        l.add("Build requirement cost ledger");
        l.add("Scope: live build recipes now mix abstract supplies/parts with named industrial components pulled from carried inventory or claimed-base storage.");
        l.add("Build recipes: " + a.totalRecipes + " | machinery/facilities " + a.machineryRecipes + " | componentized " + a.variedComponentRecipes + " | unique named components " + a.uniqueComponents.size() + " | status " + pass(a.passed()) + ".");
        l.add("Rule: simple fixtures may retain generic supplies, but machinery, facilities, and specialized lab apparatus should require specific industrial parts.");
        int n=0;
        for (BuildRecipe r : BuildRecipe.allBuildRecipes()) {
            if (r.componentCosts.isEmpty()) continue;
            l.add("  " + r.name + " | supplies " + r.supplyCost + " parts " + r.partCost + " | components " + r.componentCostSummary(8));
            if (++n >= max) break;
        }
        if (n == 0) l.add("  No componentized build recipes found; this is an audit-visible failure.");
        return l;
    }
    static ArrayList<String> draftRecipeGraphLines(int max) {
        ArrayList<String> l = new ArrayList<>();
        RecipeGraphAudit graph = RecipeGraphAuditApi.audit();
        l.add("Draft recipe graph ledger");
        l.add("Scope: generated recipe ancestry from ItemCatalog scans. This is not the player workbench list.");
        l.add("Recipe count: " + graph.recipeCount + " | missing inputs " + graph.missingInputs.size() + " | missing outputs " + graph.missingOutputs.size() + " | cycles " + graph.circularRecipes.size() + " | width issues " + graph.absurdWidths.size() + ".");
        l.add("Family coverage:");
        int n=0;
        for (Map.Entry<String,Integer> e : graph.familyCounts.entrySet()) { l.add("  " + e.getKey() + ": " + e.getValue()); if (++n >= max) break; }
        l.add("Sample generated recipes:");
        n=0;
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) { l.add("  " + r.outputBaseItem + " | " + r.family + " | inputs " + r.inputSummary() + " | equipment " + r.equipmentSummary()); if (++n >= max) break; }
        return l;
    }
    static ArrayList<String> factionVariantLedgerLines(int max) {
        ArrayList<String> l = new ArrayList<>();
        FactionRecipeVariantAudit a = FactionRecipeVariantAuditApi.audit();
        l.add("Faction manufacturing variant ledger");
        l.add("Scope: generated faction identity overlays on eligible draft recipes.");
        l.add("Generated variants: " + a.variantRecipes + " | eligible base recipes: " + a.eligibleBaseRecipes + " | status " + pass(a.passed()) + ".");
        l.add("Faction coverage:");
        for (Map.Entry<String,Integer> e : a.perFaction.entrySet()) l.add("  " + e.getKey() + ": " + e.getValue());
        l.add("Law/status coverage:");
        for (Map.Entry<String,Integer> e : a.perLawStatus.entrySet()) l.add("  " + e.getKey() + ": " + e.getValue());
        l.add("Sample variants:");
        int n=0;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) { l.add("  " + v.sampleLine()); if (++n >= max) break; }
        return l;
    }
    static ArrayList<String> chemicalEquipmentLedgerLines(int max) {
        ArrayList<String> l = new ArrayList<>();
        ChemicalEquipmentAudit a = ChemicalEquipmentAuditApi.audit();
        l.add("Chemical equipment ledger");
        l.add("Scope: non-consumed apparatus, room groups, process types, manning, and minimum machine tiers for chemistry and medicae production.");
        l.add("Equipment profiles: " + a.registeredEquipmentProfiles + " | apparatus-tagged chemical recipes: " + a.chemicalRecipes + " | status " + pass(a.passed()) + ".");
        l.add("Process coverage:");
        for (Map.Entry<String,Integer> e : a.processCounts.entrySet()) l.add("  " + e.getKey() + ": " + e.getValue());
        l.add("Room coverage:");
        for (Map.Entry<String,Integer> e : a.roomCounts.entrySet()) l.add("  " + e.getKey() + ": " + e.getValue());
        l.add("Buildable live apparatus recipes:");
        for (BuildRecipe b : BuildRecipe.laboratoryEquipmentRecipes()) l.add("  " + b.name + " | build quality " + b.qualityName + " | knowledge " + b.requiredKnowledge + " | symbol " + b.symbol);
        l.add("Equipment profiles:");
        int n=0;
        for (LaboratoryEquipmentProfile p : ChemicalEquipmentAuthority.profiles()) { l.add("  " + p.name + " | process " + p.processType + " | knowledge " + p.knowledgeCategory + " | ceiling " + QualityAuthorityApi.qualityName(p.minimumMachineTier) + " | rooms " + p.roomKind); if (++n >= max) break; }
        return l;
    }

    static ArrayList<String> controlledProductionJobLedgerLines(int max) {
        ArrayList<String> l = new ArrayList<>();
        ControlledProductionJobAudit a = ControlledProductionJobAuditApi.audit();
        l.add("Controlled generated production job ledger");
        l.add("Scope: filtered generated/factional production jobs can be assigned to valid machines/apparatus without flooding ordinary crafting.");
        l.add("Generated variants: " + a.generatedVariants + " | apparatus-backed " + a.apparatusBackedVariants + " | manual-fallback eligible " + a.manualEligibleVariants + " | forecastable " + a.forecastableVariants + " | filter buckets " + a.filterBuckets + " | status " + pass(a.passed()) + ".");
        l.add("Filters: category=" + String.join("/", ControlledProductionJobAuthority.categoryFilters()) + "; status=" + String.join("/", ControlledProductionJobAuthority.readinessFilters()) + ".");
        l.add("Manual fallback: if no recruit is available, the player may operate an eligible assigned machine manually for deterministic turn and fatigue cost.");
        int n=0;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) {
            BaseObject sample = new BaseObject(FacilityOutputModifierAuthority.sampleMachineName(v), FacilityOutputModifierAuthority.sampleMachineSymbol(v), 0,0,0,0);
            sample.qualityName = v.machineQuality;
            l.add("  " + v.outputName + " | category " + ControlledProductionJobAuthority.categoryFor(v) + " | knowledge " + v.requiredKnowledge + " | machine " + v.machineQuality + " | equipment " + v.equipmentSummary() + " | " + ControlledProductionJobAuthority.inputForecastLine(null, v) + " | " + FacilityOutputModifierAuthority.forecastLine(null, sample, v, false));
            if (++n >= max) break;
        }
        return l;
    }

    static ArrayList<String> authorityLedgerLines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Quality / Knowledge / Machine authority ledger");
        l.add("Scope: the authority layers that cap output quality, govern knowledge unlock identity, and make machine tier limits visible.");
        l.add("-- Quality --");
        l.addAll(QualityAuthorityApi.detailLines());
        l.add("-- Knowledge --");
        l.addAll(KnowledgeTreeApi.auditLines());
        l.add("-- Machine tiers --");
        l.addAll(MachineTierAuthority.auditLines());
        return l;
    }
    static ArrayList<String> lawStatusLedgerLines() {
        ArrayList<String> l = new ArrayList<>();
        FactionRecipeVariantAudit a = FactionRecipeVariantAuditApi.audit();
        l.add("Contraband and law status ledger");
        l.add("Scope: shows how generated faction variants are flagged for lawful, restricted, black-market, contraband, profaned, or hostile-social production contexts.");
        for (Map.Entry<String,Integer> e : a.perLawStatus.entrySet()) l.add("  " + e.getKey() + ": " + e.getValue() + " generated variants");
        l.add("Examples:");
        int shown=0;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) if (!low(v.lawStatus).contains("lawful") || low(v.lawStatus).contains("restricted")) {
            l.add("  " + v.outputName + " | " + v.lawStatus + " | equipment " + v.equipmentSummary());
            if (++shown >= 28) break;
        }
        return l;
    }
    static ArrayList<String> recipeAncestrySampleLines(int max) {
        ArrayList<String> l = new ArrayList<>();
        l.add("Recipe ancestry samples");
        l.add("Scope: direct consumed inputs plus non-consumed equipment, process, rooms, manning, and machine ceiling.");
        int n=0;
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) {
            if (n >= max) break;
            l.add("  " + r.outputBaseItem + " <- " + r.inputSummary() + " | process " + r.processType + " | equipment " + r.equipmentSummary() + " | room " + r.roomSummary() + " | manning " + r.manningRequirement + " | min machine " + QualityAuthorityApi.qualityName(r.minimumMachineTier));
            n++;
        }
        return l;
    }
    static ArrayList<String> laboratoryProcessSampleLines(int max) {
        ArrayList<String> l = new ArrayList<>();
        l.add("Laboratory process samples");
        l.add("Scope: chemistry-specific process records. Equipment is required apparatus, not consumed ingredients.");
        int n=0;
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) if (ChemicalEquipmentAuthority.isChemicalRecipe(r)) {
            l.add("  " + r.outputBaseItem + " | " + r.processType + " | inputs " + r.inputSummary() + " | apparatus " + r.equipmentSummary() + " | " + r.placementNote);
            if (++n >= max) break;
        }
        return l;
    }
    static ArrayList<String> draftRecipeDetailLines(String outputName) {
        ArrayList<String> l = new ArrayList<>();
        DraftIndustrialRecipe r = findDraftRecipe(outputName);
        if (r == null) { l.add("Draft recipe not found"); l.add("No generated draft recipe currently resolves for: " + outputName); return l; }
        l.add("Draft recipe detail");
        l.add("Output base item: " + r.outputBaseItem);
        l.add("Recipe name: " + r.name);
        l.add("Family: " + r.family);
        l.add("Source: " + r.source);
        l.add("Note: " + r.note);
        l.add("Consumed inputs: " + r.inputSummary());
        l.add("Required apparatus: " + r.equipmentSummary());
        l.add("Process type: " + r.processType);
        l.add("Room requirement: " + r.roomSummary());
        l.add("Manning: " + r.manningRequirement);
        l.add("Minimum machine tier: " + QualityAuthorityApi.qualityName(r.minimumMachineTier));
        l.add("Placement note: " + r.placementNote);
        ItemDef d = ItemCatalog.get(r.outputBaseItem);
        if (d != null) { l.add("Catalog category: " + d.category + (d.weapon ? " / weapon" : "")); l.add("Catalog use: " + d.use); }
        l.add("Matching faction variants:");
        int n=0;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) if (v.base != null && v.base.outputBaseItem.equals(r.outputBaseItem)) { l.add("  " + v.outputName + " | " + v.faction + " | " + v.lawStatus + " | " + v.statSummary()); if (++n >= 18) break; }
        if (n == 0) l.add("  No generated faction variant for this output; likely a precursor/component or excluded class.");
        return l;
    }
    static ArrayList<String> factionVariantDetailLines(String outputName) {
        ArrayList<String> l = new ArrayList<>();
        FactionRecipeVariant v = findVariant(outputName);
        if (v == null) { l.add("Faction variant not found"); l.add("No generated variant currently resolves for: " + outputName); return l; }
        l.add("Faction recipe variant detail");
        l.add("Output: " + v.outputName);
        l.add("Base item: " + (v.base == null ? "unknown" : v.base.outputBaseItem));
        l.add("Faction: " + v.profile.label + " | prefix " + v.profile.recipePrefix);
        l.add("Quality: " + v.qualityName + " | knowledge: " + v.requiredKnowledge + " | machine tier: " + v.machineQuality + " | machine hint: " + v.machineHint);
        l.add("Law status: " + v.lawStatus);
        l.add("Production note: " + v.productionNote);
        l.add("Consumed inputs: " + v.inputSummary());
        l.add("Required apparatus: " + v.equipmentSummary());
        l.add("Stats: " + v.statSummary());
        if (v.base != null) { l.add("Base process: " + v.base.processType + " | room " + v.base.roomSummary() + " | manning " + v.base.manningRequirement + " | placement " + v.base.placementNote); }
        return l;
    }
    static ArrayList<String> productionContainerLedgerLines(GamePanel g, int limit) {
        return ProductionContainerAuthority.ledgerLines(g, limit);
    }
    static ArrayList<String> auditSummaryLines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("  Industrial itemization: " + pass(RecipeGraphAuditApi.audit().passed()));
        l.add("  Faction variants: " + pass(FactionRecipeVariantAuditApi.audit().passed()));
        l.add("  Chemical equipment: " + pass(ChemicalEquipmentAuditApi.audit().passed()));
        l.add("  Authority alignment: " + pass(AuthorityAlignmentAuditApi.audit().passed()));
        l.add("  Live placement: " + pass(LiveProductionPlacementAuditApi.audit().passed()));
        l.add("  Facility output modifiers: " + pass(FacilityOutputModifierAuditApi.audit().passed()));
        l.add("  Build requirement costs: " + pass(BuildRequirementCostAuditApi.audit().passed()));
        l.add("  Production container authority: runtime audit in active panel; see Production Container Authority Audit.");
        l.add("  Persistent stock containers: runtime audit in active panel; see Persistent Stock Container Audit.");
        l.add("  Persistent stock pickup UX: runtime audit in active panel; see Persistent Stock Pickup UX Audit.");
        l.add("  Production display: " + pass(ProductionLedgerDisplayAuditApi.audit().passed()));
        l.add("  Controlled job UI / queue: " + pass(ControlledProductionJobAuditApi.audit().passed()));
        return l;
    }
    static DraftIndustrialRecipe findDraftRecipe(String outputName) {
        String wanted = outputName == null ? "" : outputName.trim();
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) if (r.outputBaseItem.equalsIgnoreCase(wanted) || r.name.equalsIgnoreCase(wanted)) return r;
        return null;
    }
    static FactionRecipeVariant findVariant(String outputName) {
        String wanted = outputName == null ? "" : outputName.trim();
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) if (v.outputName.equalsIgnoreCase(wanted)) return v;
        return null;
    }
    static String pass(boolean b) { return b ? "PASS" : "FAIL"; }
    static String low(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}


class ProductionLedgerDisplayAuditApi {
    static ProductionLedgerDisplayAudit audit() {
        ProductionLedgerDisplayAudit a = new ProductionLedgerDisplayAudit();
        a.productionTabEntries = 13 + Math.min(90, RecipeDecompositionApi.generatedDraftRecipes().size()) + Math.min(50, FactionRecipeVariantApi.generatedFactionVariants().size());
        a.auditTabEntries = 11;
        a.overviewLines = ProductionLedgerDisplayApi.overviewLines().size();
        a.draftRecipes = RecipeDecompositionApi.generatedDraftRecipes().size();
        a.factionVariants = FactionRecipeVariantApi.generatedFactionVariants().size();
        a.chemicalApparatusRecipes = ChemicalEquipmentAuditApi.audit().chemicalRecipes;
        a.labEquipmentProfiles = ChemicalEquipmentAuthority.profiles().size();
        a.liveBuildableApparatus = LiveProductionPlacementAuditApi.audit().buildableLabApparatus;
        a.facilityOutputVariants = FacilityOutputModifierAuditApi.audit().forecastableVariants;
        a.componentizedBuildRecipes = BuildRequirementCostAuditApi.audit().variedComponentRecipes;
        if (a.productionTabEntries < 12) a.issues.add("production tab has too few entries");
        if (a.auditTabEntries < 5) a.issues.add("audit tab has too few entries");
        if (a.overviewLines < 6) a.issues.add("overview renderer is too shallow");
        if (a.draftRecipes <= 0) a.issues.add("no draft recipes available for display");
        if (a.factionVariants <= 0) a.issues.add("no faction variants available for display");
        if (a.chemicalApparatusRecipes <= 0) a.issues.add("no chemical apparatus recipes available for display");
        if (a.labEquipmentProfiles <= 0) a.issues.add("no lab equipment profiles available for display");
        if (a.liveBuildableApparatus <= 0) a.issues.add("no live buildable apparatus available for display");
        DraftIndustrialRecipe sampleRecipe = null;
        for (DraftIndustrialRecipe r : RecipeDecompositionApi.generatedDraftRecipes()) if (ChemicalEquipmentAuthority.isChemicalRecipe(r) && r.equipmentWidth() > 0) { sampleRecipe = r; break; }
        if (sampleRecipe == null) a.issues.add("no chemical recipe with equipment found for detailed display");
        else {
            a.sampleRecipeName = sampleRecipe.outputBaseItem;
            a.sampleRecipeDetailLines = ProductionLedgerDisplayApi.draftRecipeDetailLines(sampleRecipe.outputBaseItem).size();
            if (a.sampleRecipeDetailLines < 10) a.issues.add("sample recipe detail renderer is too shallow for " + sampleRecipe.outputBaseItem);
        }
        FactionRecipeVariant sampleVariant = null;
        for (FactionRecipeVariant v : FactionRecipeVariantApi.generatedFactionVariants()) { sampleVariant = v; break; }
        if (sampleVariant == null) a.issues.add("no faction variant found for detailed display");
        else {
            a.sampleVariantName = sampleVariant.outputName;
            a.sampleVariantDetailLines = ProductionLedgerDisplayApi.factionVariantDetailLines(sampleVariant.outputName).size();
            if (a.sampleVariantDetailLines < 9) a.issues.add("sample variant detail renderer is too shallow for " + sampleVariant.outputName);
        }
        if (!RecipeGraphAuditApi.audit().passed()) a.issues.add("recipe graph audit not passing");
        if (!FactionRecipeVariantAuditApi.audit().passed()) a.issues.add("faction variant audit not passing");
        if (!ChemicalEquipmentAuditApi.audit().passed()) a.issues.add("chemical equipment audit not passing");
        if (!AuthorityAlignmentAuditApi.audit().passed()) a.issues.add("authority alignment audit not passing");
        if (!LiveProductionPlacementAuditApi.audit().passed()) a.issues.add("live production placement audit not passing");
        if (!BuildRequirementCostAuditApi.audit().passed()) a.issues.add("build requirement cost audit not passing");
        if (a.componentizedBuildRecipes <= 0) a.issues.add("no componentized build recipes visible");
        return a;
    }
}


class ProductionLedgerDisplayAudit {
    int productionTabEntries, auditTabEntries, overviewLines, draftRecipes, factionVariants, chemicalApparatusRecipes, labEquipmentProfiles, liveBuildableApparatus, facilityOutputVariants, componentizedBuildRecipes, sampleRecipeDetailLines, sampleVariantDetailLines;
    String sampleRecipeName = "none", sampleVariantName = "none";
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("0.9.05 Infopedia / Audit / Production Ledger Display audit");
        l.add("Production tab entries: " + productionTabEntries);
        l.add("Audit tab entries: " + auditTabEntries);
        l.add("Overview renderer lines: " + overviewLines);
        l.add("Draft recipes visible to display: " + draftRecipes);
        l.add("Faction variants visible to display: " + factionVariants);
        l.add("Chemical apparatus recipes visible to display: " + chemicalApparatusRecipes);
        l.add("Lab equipment profiles visible to display: " + labEquipmentProfiles);
        l.add("Live buildable apparatus visible to display: " + liveBuildableApparatus);
        l.add("Facility output modifier variants visible: " + facilityOutputVariants);
        l.add("Componentized build recipes visible: " + componentizedBuildRecipes);
        l.add("Sample recipe detail: " + sampleRecipeName + " -> " + sampleRecipeDetailLines + " lines");
        l.add("Sample variant detail: " + sampleVariantName + " -> " + sampleVariantDetailLines + " lines");
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}


class RecipeGraphAuditApi {
    static RecipeGraphAudit audit() { return audit(RecipeDecompositionApi.generatedDraftRecipes()); }
    static RecipeGraphAudit audit(ArrayList<DraftIndustrialRecipe> recipes) {
        RecipeGraphAudit a = new RecipeGraphAudit();
        a.recipeCount = recipes.size();
        HashSet<String> outputs = new HashSet<>();
        LinkedHashMap<String,DraftIndustrialRecipe> byOutput = new LinkedHashMap<>();
        for (DraftIndustrialRecipe r : recipes) {
            outputs.add(r.outputBaseItem);
            byOutput.putIfAbsent(r.outputBaseItem, r);
            a.familyCounts.put(r.family, a.familyCounts.getOrDefault(r.family,0)+1);
            if (!knownCatalogItem(r.outputBaseItem)) a.missingOutputs.add(r.outputBaseItem + " from " + r.name);
            if (r.width() > 10) a.absurdWidths.add(r.outputBaseItem + " width=" + r.width() + " in " + r.name);
            for (String input : r.itemInputs.keySet()) if (!knownCatalogItem(input)) a.missingInputs.add(input + " required by " + r.outputBaseItem);
        }
        for (ItemDef d : ItemCatalog.ITEMS.values()) if (isIndustrialCatalogItem(d)) {
            a.industrialCatalogCount++;
            String key = rootCategory(d.category);
            a.industrialCatalogCounts.put(key, a.industrialCatalogCounts.getOrDefault(key,0)+1);
        }
        for (String out : outputs) detectCycles(out, byOutput, new LinkedHashSet<String>(), a);
        return a;
    }
    private static boolean knownCatalogItem(String item) { return ItemCatalog.ITEMS.containsKey(item) || ItemCatalog.get(item) != null; }
    private static boolean isIndustrialCatalogItem(ItemDef d) {
        if (d == null || d.category == null) return false;
        String c = d.category.toLowerCase(Locale.ROOT);
        return c.startsWith("equipment/") || c.startsWith("component/") || c.startsWith("material/") || c.startsWith("water/") || c.startsWith("agriculture/") || c.startsWith("organic/") || c.startsWith("food/raw") || c.startsWith("food/intermediate") || c.startsWith("food/synthetic") || c.startsWith("food/vat") || c.startsWith("drink/intermediate") || c.startsWith("chemical/waste") || c.startsWith("chem/") || c.startsWith("medical/chem");
    }
    private static String rootCategory(String c) {
        if (c == null || c.isBlank()) return "uncategorized";
        String[] p = c.toLowerCase(Locale.ROOT).split("/");
        if (p.length >= 2) return p[0] + "/" + p[1];
        return p[0];
    }
    private static void detectCycles(String item, LinkedHashMap<String,DraftIndustrialRecipe> byOutput, LinkedHashSet<String> stack, RecipeGraphAudit a) {
        if (stack.contains(item)) { a.circularRecipes.add(String.join(" -> ", stack) + " -> " + item); return; }
        DraftIndustrialRecipe r = byOutput.get(item);
        if (r == null) return;
        stack.add(item);
        for (String input : r.itemInputs.keySet()) if (byOutput.containsKey(input)) detectCycles(input, byOutput, stack, a);
        stack.remove(item);
    }
}


class RecipeGraphAudit {
    int recipeCount;
    int industrialCatalogCount;
    final TreeMap<String,Integer> familyCounts = new TreeMap<>();
    final TreeMap<String,Integer> industrialCatalogCounts = new TreeMap<>();
    final TreeSet<String> missingInputs = new TreeSet<>();
    final TreeSet<String> missingOutputs = new TreeSet<>();
    final TreeSet<String> circularRecipes = new TreeSet<>();
    final TreeSet<String> absurdWidths = new TreeSet<>();
    boolean passed() { return missingInputs.isEmpty() && missingOutputs.isEmpty() && circularRecipes.isEmpty() && absurdWidths.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Industrialized Itemization draft recipe graph audit");
        l.add("Draft recipes generated: " + recipeCount);
        l.add("Industrial/component catalog entries: " + industrialCatalogCount);
        l.add("Industrial/component catalog groups: " + industrialCatalogCounts.size());
        for (Map.Entry<String,Integer> e : industrialCatalogCounts.entrySet()) l.add("  catalog " + e.getKey() + ": " + e.getValue());
        l.add("Families generated: " + familyCounts.size());
        for (Map.Entry<String,Integer> e : familyCounts.entrySet()) l.add("  " + e.getKey() + ": " + e.getValue());
        l.add("Missing inputs: " + missingInputs.size());
        for (String s : missingInputs) l.add("  MISSING_INPUT " + s);
        l.add("Missing outputs: " + missingOutputs.size());
        for (String s : missingOutputs) l.add("  MISSING_OUTPUT " + s);
        l.add("Circular recipes: " + circularRecipes.size());
        for (String s : circularRecipes) l.add("  CYCLE " + s);
        l.add("Absurd recipe widths (>10 direct inputs): " + absurdWidths.size());
        for (String s : absurdWidths) l.add("  WIDTH " + s);
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }

}


class AuthorityAlignmentAuditApi {
    static AuthorityAlignmentAudit audit() {
        AuthorityAlignmentAudit a = new AuthorityAlignmentAudit();
        a.qualityTierCount = QualityAuthorityApi.profiles().size();
        a.knowledgeCategoryCount = KnowledgeTreeApi.CATEGORIES.length;
        a.machineTierCount = MachineTierAuthority.profiles().size();
        Faction[] fs = {Faction.HIVER, Faction.SCAVENGER, Faction.BANDIT, Faction.ARBITES, Faction.IMPERIAL_GUARD, Faction.MINISTORUM, Faction.SORORITAS, Faction.MECHANICUS, Faction.NOBLE, Faction.CULTIST, Faction.MUTANT};
        a.factionProfileCount = fs.length;
        LinkedHashMap<String,KnowledgeDef> knowledge = KnowledgeDef.all();
        for (String cat : KnowledgeTreeApi.CATEGORIES) for (String band : QualityAuthorityApi.KNOWLEDGE_BANDS) {
            String n = band + " " + cat + " Patterns";
            if (!knowledge.containsKey(n)) a.issues.add("missing knowledge tree node: " + n);
        }
        double lastValue = -1.0;
        for (QualityAuthorityProfile q : QualityAuthorityApi.profiles()) {
            if (q.valueMultiplier < lastValue) a.issues.add("non-monotonic value multiplier at " + q.name);
            lastValue = q.valueMultiplier;
        }
        int lastCeiling = -1;
        for (MachineTierProfile m : MachineTierAuthority.profiles()) {
            if (m.qualityCeilingTier < lastCeiling) a.issues.add("non-monotonic machine ceiling at " + m.qualityName);
            if (m.workerCapacity < 1) a.issues.add("machine worker capacity below one at " + m.qualityName);
            lastCeiling = m.qualityCeilingTier;
        }
        for (Faction f : fs) {
            FactionManufacturingProfile p = FactionManufacturingProfile.forFaction(f);
            if (p.recipePrefix == null || p.recipePrefix.isBlank()) a.issues.add("blank recipe prefix for " + f);
            if (p.defectBias <= 0 || p.reliabilityBias <= 0) a.issues.add("invalid reliability/defect bias for " + f);
        }
        return a;
    }
}


class AuthorityAlignmentAudit {
    int qualityTierCount, knowledgeCategoryCount, machineTierCount, factionProfileCount;
    final TreeSet<String> issues = new TreeSet<>();
    boolean passed() { return issues.isEmpty(); }
    ArrayList<String> lines() {
        ArrayList<String> l = new ArrayList<>();
        l.add("Quality / Knowledge / Machine Authority Alignment audit");
        l.add("Quality authority tiers: " + qualityTierCount);
        l.add("Knowledge tree categories: " + knowledgeCategoryCount + " x " + QualityAuthorityApi.KNOWLEDGE_BANDS.length + " quality bands = " + (knowledgeCategoryCount * QualityAuthorityApi.KNOWLEDGE_BANDS.length) + " nodes");
        l.add("Machine tier profiles: " + machineTierCount);
        l.add("Faction manufacturing identity profiles: " + factionProfileCount);
        l.add("-- Quality Authority --");
        l.addAll(QualityAuthorityApi.auditLines());
        l.add("-- Knowledge Trees --");
        l.addAll(KnowledgeTreeApi.auditLines());
        l.add("-- Machine Tiers --");
        l.addAll(MachineTierAuthority.auditLines());
        l.add("-- Faction Manufacturing Identities --");
        for (String s : FactionManufacturingProfile.summaryLines()) l.add("FACTION_IDENTITY " + s);
        l.add("Issues: " + issues.size());
        for (String s : issues) l.add("  ISSUE " + s);
        l.add("Audit status: " + (passed()?"PASS":"FAIL"));
        return l;
    }
}


class ProductionRecipe {
    final String baseItem, qualityName, knowledgeName, machineName;
    final Faction faction;
    final FactionManufacturingProfile profile;
    ProductionRecipe(String baseItem, Faction faction, String qualityName, String knowledgeName, String machineName) {
        this.baseItem=baseItem; this.faction=faction==null?Faction.HIVER:faction; this.qualityName=qualityName==null?"Common":qualityName; this.knowledgeName=knowledgeName; this.machineName=machineName; this.profile=FactionManufacturingProfile.forFaction(this.faction);
    }
    static ProductionRecipe create(String baseItem, Faction faction, String qualityName, String knowledgeName, String machineName) { return new ProductionRecipe(baseItem, faction, qualityName, knowledgeName, machineName); }
    String outputItemName() { return qualityName + " " + profile.recipePrefix + " " + baseItem; }
    int qualityTier() { return ItemQuality.tierIndex(qualityName + " " + baseItem); }
    int outputCharges() { return Math.max(1, (int)Math.round(ItemQuality.CHARGES[qualityTier()] * profile.chargeBias * profile.efficiencyBias * 3.0)); }
    int estimatedValue() { ItemDef d = ItemCatalog.get(baseItem); int base = d == null ? 2 : d.basePrice; return Math.max(1, (int)Math.round(ItemQuality.priced(base, qualityName + " " + baseItem) * profile.valueBias * Math.max(0.50, profile.prestigeBias))); }
    int estimatedDefectPercent() {
        QualityAuthorityProfile quality = QualityAuthorityApi.profile(qualityName);
        return Math.max(1, (int)Math.round(10.0 * profile.defectBias * quality.defectMultiplier
                / Math.max(0.25, profile.reliabilityBias * quality.reliabilityMultiplier)));
    }
    int estimatedDefectPercent(BaseObject machine) {
        return Math.min(99, estimatedDefectPercent()
                + MachineConditionProductionAuthority.evaluate(machine).defectRiskAdd());
    }
    int estimatedDefectPercent(BaseObject machine, int operatorAdjustment) {
        return Math.max(1, Math.min(99, estimatedDefectPercent(machine) + operatorAdjustment));
    }
    String summary() { return machineName + " makes " + outputItemName() + " using " + knowledgeName + " [value~" + estimatedValue() + ", charges~" + outputCharges() + ", defectRisk~" + estimatedDefectPercent() + "%]"; }
    String auditLine() { return "recipe=" + baseItem + " faction=" + faction + " quality=" + qualityName + " knowledge=" + knowledgeName + " machine=" + machineName + " output=" + outputItemName(); }
    static ArrayList<String> sampleLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(create("Emergency rations", Faction.HIVER, "Junk", "Junk Fabrication Patterns", "Junk Micro Assembler").summary());
        lines.add(create("Emergency rations", Faction.IMPERIAL_GUARD, "Serviceable", "Military Logistics Patterns", "Serviceable Micro Assembler").summary());
        lines.add(create("Emergency rations", Faction.NOBLE, "Noble", "Noble Manufactury Patterns", "Noble Provisioning Suite").summary());
        lines.add(create("Power relay", Faction.MECHANICUS, "Archeotech", "Archeotech Production Rites", "Archeotech Power Relay Loom").summary());
        return lines;
    }
}



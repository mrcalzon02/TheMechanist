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

class Persistence {
    static final String VERSION = "0.9.10gs";

    static void writeCore(GamePanel g, Properties p) {
        CharacterEquipmentAndMedicalAuthority.writeState(p, g.equippedWearableSlots, g.installedBodyModifications);
        put(p, "save.version", VERSION);
        put(p, "save.created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        SaveEfficiencyAuthority.writeWorldReference(g, p);
        put(p, "run.seed", Long.toString(g.seed));
        put(p, "run.worldSetup", g.worldSetup == null ? WorldSetupSettings.standard().encode() : g.worldSetup.encode());
        put(p, "run.turn", Integer.toString(g.turn));
        put(p, "run.worldTurn", Long.toString(g.worldTurn));
        put(p, "run.playerX", Integer.toString(g.playerX));
        put(p, "run.playerY", Integer.toString(g.playerY));
        put(p, "run.suspicion", Integer.toString(g.suspicion));
        put(p, "run.gangHeat", Integer.toString(g.gangHeat));
        put(p, "run.supplies", Integer.toString(g.supplies));
        put(p, "run.food", Integer.toString(g.food));
        put(p, "run.water", Integer.toString(g.water));
        put(p, "run.fatigue", Integer.toString(g.fatigue));
        put(p, "run.wounds", Integer.toString(g.wounds));
        put(p, "run.sleepNeed", Integer.toString(g.sleepNeed));
        put(p, "run.stimulantStrain", Integer.toString(g.stimulantStrain));
        put(p, "run.bleeding", Integer.toString(g.bleeding));
        put(p, "run.infectionRisk", Integer.toString(g.infectionRisk));
        put(p, "run.pain", Integer.toString(g.pain));
        put(p, "run.machineParts", Integer.toString(g.machineParts));
        put(p, "run.xp", Integer.toString(g.xp));
        put(p, "run.knowledgeCredits", Integer.toString(g.knowledgeCredits));
        put(p, "run.kills", Integer.toString(g.runKills));
        put(p, "run.crafted", Integer.toString(g.runCrafted));
        put(p, "run.npcTalked", Integer.toString(g.runNpcTalkedTo));
        put(p, "run.unconscious", Integer.toString(g.runUnconsciousEvents));
        put(p, "run.lastDefeatCause", g.lastDefeatCause);
        put(p, "run.lastDefeatWeapon", g.lastDefeatWeapon);
        put(p, "run.lastDefeatAttacker", g.lastDefeatAttacker);
        put(p, "run.lastDefeatLocation", g.lastDefeatLocation);
        put(p, "run.knowledges", encList(new ArrayList<String>(g.unlockedKnowledges)));
        put(p, "run.skillNodes", encList(new ArrayList<String>(g.unlockedSkillNodes)));
        if (g.atlas != null) {
            put(p, "atlas.seed", Long.toString(g.atlas.seed));
            put(p, "atlas.sx", Integer.toString(g.atlas.sectorX));
            put(p, "atlas.sy", Integer.toString(g.atlas.sectorY));
            put(p, "atlas.zx", Integer.toString(g.atlas.zoneX));
            put(p, "atlas.zy", Integer.toString(g.atlas.zoneY));
            put(p, "atlas.floor", Integer.toString(g.atlas.floor));
            put(p, "atlas.sewer", Boolean.toString(g.atlas.sewer));
            SaveEfficiencyAuthority.writeWorldReference(g, p);
        }
        if (g.active != null) writeCandidate(g.active, p);
        if (g.equippedClothing != null) {
            put(p, "clothing.name", g.equippedClothing.name);
            put(p, "clothing.faction", g.equippedClothing.alignedFaction.name());
            put(p, "clothing.disguise", Integer.toString(g.equippedClothing.disguiseBase));
            put(p, "clothing.defense", Integer.toString(g.equippedClothing.defense));
        }
        put(p, "inventory", encList(g.inventory));
        put(p, "base.storage", encList(g.baseStorage));
        put(p, "script.carried", Integer.toString(Math.max(0, g.carriedScript)));
        put(p, "script.baseStashed", Integer.toString(Math.max(0, g.baseStashedScript)));
        put(p, "light.activeItem", g.activePortableLightItem);
        put(p, "light.activeExpires", Integer.toString(g.activePortableLightExpiresTurn));
        put(p, "light.activeWorn", Boolean.toString(g.activePortableLightWorn));
        put(p, "light.lastReport", g.lastPortableLightReport);
        ArrayList<String> lights = new ArrayList<>(); for (PortableLightInstance l : g.portableLights) if(l != null && l.expiresTurn > g.turn) lights.add(l.encode()); put(p, "light.instances", encList(lights));
        ArrayList<String> loaded = new ArrayList<>(); for (Map.Entry<String,Integer> e : g.loadedWeaponShots.entrySet()) loaded.add(e.getKey()+":"+e.getValue()); put(p, "combat.loadedWeapons", encList(loaded));
        ArrayList<String> terrain = new ArrayList<>(); for (Map.Entry<String,Integer> e : g.terrainIntegrity.entrySet()) terrain.add(e.getKey()+":"+e.getValue()); put(p, "combat.terrainIntegrity", encList(terrain));
        put(p, "item.provenance", encList(ItemProvenanceRecord.encodeLedger(g.itemProvenance)));
        put(p, "item.nextInstanceSeq", Integer.toString(g.nextItemInstanceSeq));
        put(p, "item.instances", encList(ItemInstance.encodeRegistry(g.itemInstances)));
        put(p, "item.containers", encList(ContainerRecord.encodeRegistry(g.itemContainers)));
        put(p, "visited.zoneInstances", encList(new ArrayList<String>(g.visitedZoneInstances)));
        ArrayList<String> zoneTypes = new ArrayList<>(); for (ZoneType z : g.visitedZoneTypes) zoneTypes.add(z.name()); put(p, "visited.zoneTypes", encList(zoneTypes));
        if (g.machineOperationQueue != null) put(p, "machine.queue.history", encList(g.machineOperationQueue.encodeRecentHistory()));
        put(p, "logistics.intent.nextSeq", Integer.toString(Math.max(1, g.nextLogisticsIntentSeq)));
        put(p, "logistics.intent.history", encList(LogisticsDeliveryIntentAuthority.encodeHistory(g)));
        put(p, "logistics.intent.lastReport", g.lastLogisticsDeliveryIntentReport);
        put(p, "logistics.source.nextSeq", Integer.toString(Math.max(1, g.nextLogisticsSourceReservationSeq)));
        put(p, "logistics.source.history", encList(LogisticsSourceReservationAuthority.encodeHistory(g)));
        put(p, "logistics.source.lastReport", g.lastLogisticsSourceReservationReport);
        put(p, "logistics.route.nextSeq", Integer.toString(Math.max(1, g.nextLogisticsRouteIntentSeq)));
        put(p, "logistics.route.history", encList(LogisticsRouteIntentAuthority.encodeHistory(g)));
        put(p, "logistics.route.lastReport", g.lastLogisticsRouteIntentReport);
        put(p, "logistics.routePreview.nextSeq", Integer.toString(Math.max(1, g.nextLogisticsRoutePreviewSeq)));
        put(p, "logistics.routePreview.history", encList(LogisticsRouteReadinessPreviewAuthority.encodeHistory(g)));
        put(p, "logistics.routePreview.lastReport", g.lastLogisticsRoutePreviewReport);
        put(p, "logistics.haulContract.nextSeq", Integer.toString(Math.max(1, g.nextLogisticsHaulContractSeq)));
        put(p, "logistics.haulContract.history", encList(LogisticsManualHaulContractAuthority.encodeHistory(g)));
        put(p, "logistics.haulContract.lastReport", g.lastLogisticsHaulContractReport);
        put(p, "logistics.haulPreflight.nextSeq", Integer.toString(Math.max(1, g.nextLogisticsHaulPreflightSeq)));
        put(p, "logistics.haulPreflight.history", encList(LogisticsHaulFulfillmentPreflightAuthority.encodeHistory(g)));
        put(p, "logistics.haulPreflight.lastReport", g.lastLogisticsHaulPreflightReport);
        put(p, "logistics.contractLifecycle.nextSeq", Integer.toString(Math.max(1, g.nextLogisticsContractLifecycleSeq)));
        put(p, "logistics.contractLifecycle.history", encList(LogisticsContractLifecycleAuthority.encodeHistory(g)));
        put(p, "logistics.contractLifecycle.lastReport", g.lastLogisticsContractLifecycleReport);
        put(p, "logistics.haulExecution.nextSeq", Integer.toString(Math.max(1, g.nextLogisticsHaulExecutionSeq)));
        put(p, "logistics.haulExecution.history", encList(LogisticsManualHaulExecutionAuthority.encodeHistory(g)));
        put(p, "logistics.haulExecution.lastReport", g.lastLogisticsHaulExecutionReport);
        ArrayList<String> standing = new ArrayList<>(); for (Map.Entry<Faction,Integer> e : g.factionStanding.entrySet()) standing.add(e.getKey().name()+":"+e.getValue()); put(p,"factions.standing",encList(standing));
        ArrayList<String> hostile = new ArrayList<>(); for (Map.Entry<Faction,Integer> e : g.temporaryHostileTurns.entrySet()) hostile.add(e.getKey().name()+":"+e.getValue()); put(p,"factions.hostile",encList(hostile));
        ArrayList<String> marketPressure = new ArrayList<>(); for (Map.Entry<Faction,Integer> e : g.factionMarketPressure.entrySet()) marketPressure.add(e.getKey().name()+":"+e.getValue()); put(p,"factions.marketPressure",encList(marketPressure));
        ArrayList<String> contracts = new ArrayList<>(); for (FactionContract c : g.factionContracts) contracts.add(c.saveLine()); put(p,"factions.contracts",encList(contracts));
        ArrayList<String> factionSites = new ArrayList<>(); for (NpcFactionSite site : g.npcFactionSites) factionSites.add(site.saveLine()); put(p,"npc.faction.sites",encList(factionSites));
        ArrayList<String> factionPlans = new ArrayList<>(); for (FactionStrategicPlan plan : g.factionStrategicPlans) factionPlans.add(plan.saveLine()); put(p,"factions.strategicPlans",encList(factionPlans));
        put(p,"factions.lastSimulationDay",Integer.toString(g.lastFactionSimulationDay));
        put(p,"factions.lastSimulationReport",g.lastFactionSimulationReport);
        put(p,"factions.lastPublicNewsBulletin",g.lastPublicNewsBulletin);
        ArrayList<String> innIssues = new ArrayList<>(); for (Map.Entry<Integer,String> e : g.innDailyIssues.entrySet()) innIssues.add(e.getKey()+"|"+e.getValue()); put(p,"inn.dailyIssues",encList(innIssues));
        put(p,"inn.lastIssueDay",Integer.toString(g.innLastIssueDay));
        put(p,"inn.lastIssue",g.lastInnNewsIssue);
        put(p,"inn.lastBroadcastReport",g.lastBroadcastReport);
        ArrayList<String> playerNews = new ArrayList<>(); for (PlayerNewsEvent ev : g.playerNewsEvents) if (ev != null) playerNews.add(ev.saveLine()); put(p,"inn.playerNewsEvents",encList(playerNews));
        put(p,"inn.lastPlayerNewsReport",g.lastPlayerNewsReport);
        put(p,"inn.authorityFacilityInspectionCooldown",Integer.toString(g.authorityFacilityInspectionCooldownUntilTurn));
        put(p,"inn.lastAuthorityFacilityInspection",g.lastAuthorityFacilityInspectionReport);
        put(p,"bank.accounts",encList(new ArrayList<String>(g.openBankAccounts)));
        ArrayList<String> bankLines = new ArrayList<>(); for (Map.Entry<String,Integer> e : g.bankBalances.entrySet()) bankLines.add(e.getKey()+":"+e.getValue()); put(p,"bank.balances",encList(bankLines));
        put(p,"bank.lastReport",g.lastBankReport);
        put(p,"bank.lastHeistReport",g.lastBankHeistReport);
        put(p,"bank.lastAlarmReport",g.lastBankAlarmReport);
        put(p,"bank.lastLockboxContractReport",g.lastBankLockboxContractReport);
        put(p,"bank.heistAlarmCooldown",Integer.toString(g.bankHeistAlarmCooldownUntilTurn));
        put(p,"bank.lootedVaultIds",encList(new ArrayList<String>(g.lootedBankVaultIds)));
        put(p,"bank.disabledAlarmPanelIds",encList(new ArrayList<String>(g.disabledBankAlarmPanelIds)));
        put(p,"crime.lastPunishment",g.lastCrimePunishmentReport);
        put(p,"crime.lastCustodyDetail",g.lastCustodyReportDetailed);
        put(p,"crime.arbitesCaptureCooldown",Integer.toString(g.arbitesCaptureCooldownUntilTurn));
        put(p,"crime.arbitesInspectionCooldown",Integer.toString(g.arbitesInspectionCooldownUntilTurn));
        put(p,"crime.lastArbitesPatrol",g.lastArbitesPatrolReport);
        put(p,"item.lastLedgerAudit",g.lastItemLedgerAuditReport);
        ArrayList<String> sc = new ArrayList<>(); for (Map.Entry<Integer,Integer> e : g.scavengeCooldownUntilTurn.entrySet()) sc.add(e.getKey()+":"+e.getValue()); put(p,"scavenge.cooldowns",encList(sc));
        ArrayList<String> bo = new ArrayList<>(); for (BaseObject b : g.baseObjects) bo.add(b.saveLine()); put(p,"base.objects",encList(bo));
        ArrayList<String> rw = new ArrayList<>(); for (RecruitWorker r : g.factionRecruits) rw.add(r.saveLine()); put(p,"base.recruits",encList(rw));
        put(p,"base.claimed",Boolean.toString(g.baseClaimed)); put(p,"base.x",Integer.toString(g.baseX)); put(p,"base.y",Integer.toString(g.baseY)); put(p,"base.room",Integer.toString(g.claimedRoomId));
        writeWorldState(g.world, p);
        put(p, "settings.snapshot", g.options.summary());
        g.auditItemLedgers("save/write");
        DebugLog.audit("SAVE_WRITE_CORE", "keys=" + p.size() + " active=" + (g.active==null?"none":g.active.name) + " world=" + (g.world==null?"none":g.world.zoneCoordText()));
    }

    static void readCore(GamePanel g, Properties p) {
        CharacterEquipmentAndMedicalAuthority.readState(p, g.equippedWearableSlots, g.installedBodyModifications);
        String version = p.getProperty("save.version", "unknown");
        if (!VERSION.equals(version)) DebugLog.warn("SAVE_VERSION", "Loading save version " + version + " with runtime " + VERSION + ". Migration uses compatibility defaults.");
        g.seed = getLong(p,"run.seed",System.currentTimeMillis());
        g.worldSetup = WorldSetupSettings.decode(p.getProperty("run.worldSetup", WorldSetupSettings.standard().encode()));
        long atlasSeed = getLong(p,"atlas.seed",g.seed);
        g.atlas = WorldAtlas.loadSavedRun(atlasSeed, g.worldSetup);
        g.atlas.sectorX = clamp(getInt(p,"atlas.sx",1),1,3); g.atlas.sectorY = clamp(getInt(p,"atlas.sy",1),1,3);
        g.atlas.zoneX = clamp(getInt(p,"atlas.zx",2),1,3); g.atlas.zoneY = clamp(getInt(p,"atlas.zy",2),1,3);
        g.atlas.floor = clamp(getInt(p,"atlas.floor",4),1,10); g.atlas.sewer = Boolean.parseBoolean(p.getProperty("atlas.sewer","false"));
        g.atlas.generateScaffold();
        g.world = g.atlas.currentWorld();
        g.visibleTiles = new boolean[g.world.w][g.world.h];
        g.rememberedTiles = new boolean[g.world.w][g.world.h];
        g.turn = getInt(p,"run.turn",0); g.worldTurn = getLong(p,"run.worldTurn", g.turn); g.lastStaffedProductionBackgroundTurn = g.turn; g.playerX = clamp(getInt(p,"run.playerX", g.world.startPoint().x),0,g.world.w-1); g.playerY = clamp(getInt(p,"run.playerY", g.world.startPoint().y),0,g.world.h-1);
        g.lookX = g.playerX; g.lookY = g.playerY; g.lookCursorActive=false; g.interactCursorActive=false; g.panelMode=GamePanel.PanelMode.NONE;
        g.suspicion=getInt(p,"run.suspicion",0); g.gangHeat=getInt(p,"run.gangHeat",0); g.supplies=getInt(p,"run.supplies",0); g.food=getInt(p,"run.food",GamePanel.MAX_FOOD_WATER); g.water=getInt(p,"run.water",GamePanel.MAX_FOOD_WATER); g.fatigue=getInt(p,"run.fatigue",0); g.wounds=getInt(p,"run.wounds",0); g.sleepNeed=getInt(p,"run.sleepNeed",0); g.stimulantStrain=getInt(p,"run.stimulantStrain",0); g.bleeding=getInt(p,"run.bleeding",0); g.infectionRisk=getInt(p,"run.infectionRisk",0); g.pain=getInt(p,"run.pain",0); g.machineParts=getInt(p,"run.machineParts",0); g.xp=getInt(p,"run.xp",0); g.knowledgeCredits=getInt(p,"run.knowledgeCredits",0); g.unlockedKnowledges.clear(); g.unlockedKnowledges.addAll(decList(p.getProperty("run.knowledges","Underhive Basics"))); g.unlockedSkillNodes.clear(); g.unlockedSkillNodes.addAll(decList(p.getProperty("run.skillNodes","")));
        g.active = readCandidate(p, g.seed);
        try { Faction cf=Faction.valueOf(p.getProperty("clothing.faction","SCAVENGER")); g.equippedClothing = new Clothing(p.getProperty("clothing.name","Scavenger rags"), cf, getInt(p,"clothing.disguise",35), getInt(p,"clothing.defense",1), false); } catch(Exception e){ g.equippedClothing=Clothing.scavengerRags(); }
        g.inventory.clear(); g.inventory.addAll(decList(p.getProperty("inventory","")));
        g.baseStorage.clear(); g.baseStorage.addAll(decList(p.getProperty("base.storage","")));
        g.carriedScript = Math.max(0, getInt(p,"script.carried",0));
        g.baseStashedScript = Math.max(0, getInt(p,"script.baseStashed",0));
        g.activePortableLightItem = p.getProperty("light.activeItem", "");
        g.activePortableLightExpiresTurn = getInt(p,"light.activeExpires",0);
        g.activePortableLightWorn = Boolean.parseBoolean(p.getProperty("light.activeWorn","false"));
        g.lastPortableLightReport = p.getProperty("light.lastReport", g.lastPortableLightReport);
        g.portableLights.clear(); for(String ls : decList(p.getProperty("light.instances", ""))) { PortableLightInstance li = PortableLightInstance.parse(ls); if(li != null) g.portableLights.add(li); }
        g.migrateLegacyPhysicalScript("save/read legacy lists");
        g.loadedWeaponShots.clear(); for(String ls : decList(p.getProperty("combat.loadedWeapons",""))) { int cut=ls.lastIndexOf(':'); if(cut>0) try{ g.loadedWeaponShots.put(ls.substring(0,cut), Integer.parseInt(ls.substring(cut+1))); }catch(Exception ignored){} }
        g.terrainIntegrity.clear(); for(String ts : decList(p.getProperty("combat.terrainIntegrity",""))) { int cut=ts.lastIndexOf(':'); if(cut>0) try{ g.terrainIntegrity.put(ts.substring(0,cut), Integer.parseInt(ts.substring(cut+1))); }catch(Exception ignored){} }
        g.itemProvenance.clear(); ItemProvenanceRecord.decodeLedger(p.getProperty("item.provenance",""), g.itemProvenance);
        g.nextItemInstanceSeq = Math.max(1, getInt(p,"item.nextInstanceSeq",1));
        g.itemInstances.clear(); ItemInstance.decodeRegistry(p.getProperty("item.instances",""), g.itemInstances);
        g.itemContainers.clear(); ContainerRecord.decodeRegistry(p.getProperty("item.containers",""), g.itemContainers);
        g.purgePhysicalScriptInstances("save/read tracked currency migration");
        if (g.itemInstances.isEmpty() || g.itemContainers.isEmpty()) g.rebuildItemContainersFromLegacyLists();
        else g.repairLegacyListsFromContainersIfNeeded();
        g.visitedZoneInstances.clear(); g.visitedZoneInstances.addAll(decList(p.getProperty("visited.zoneInstances","")));
        g.visitedZoneTypes.clear(); for(String z: decList(p.getProperty("visited.zoneTypes",""))) try{ g.visitedZoneTypes.add(ZoneType.valueOf(z)); }catch(Exception ignored){}
        g.initFactionState(); readFactionMap(p.getProperty("factions.standing",""), g.factionStanding); readFactionMap(p.getProperty("factions.hostile",""), g.temporaryHostileTurns); readFactionMap(p.getProperty("factions.marketPressure",""), g.factionMarketPressure);
        g.factionContracts.clear(); for(String cs : decList(p.getProperty("factions.contracts",""))) { FactionContract fc = FactionContract.parse(cs); if(fc != null) g.factionContracts.add(fc); }
        g.npcFactionSites.clear(); for(String ss : decList(p.getProperty("npc.faction.sites",""))) { NpcFactionSite site = NpcFactionSite.parse(ss); if(site != null) g.npcFactionSites.add(site); }
        g.factionStrategicPlans.clear(); for(String ps : decList(p.getProperty("factions.strategicPlans",""))) { FactionStrategicPlan plan = FactionStrategicPlan.parse(ps); if(plan != null) g.factionStrategicPlans.add(plan); }
        g.lastFactionSimulationDay = getInt(p,"factions.lastSimulationDay",-1);
        g.lastFactionSimulationReport = p.getProperty("factions.lastSimulationReport", g.lastFactionSimulationReport);
        g.lastPublicNewsBulletin = p.getProperty("factions.lastPublicNewsBulletin", g.lastPublicNewsBulletin);
        g.innDailyIssues.clear();
        for(String is : decList(p.getProperty("inn.dailyIssues", ""))) {
            String[] a = is.split("\\|", 2);
            if(a.length == 2) try { g.innDailyIssues.put(Integer.parseInt(a[0]), a[1]); } catch(Exception ignored) {}
        }
        g.innLastIssueDay = getInt(p, "inn.lastIssueDay", -1);
        g.lastInnNewsIssue = p.getProperty("inn.lastIssue", g.lastInnNewsIssue);
        g.lastBroadcastReport = p.getProperty("inn.lastBroadcastReport", g.lastBroadcastReport);
        g.playerNewsEvents.clear(); for(String ns : decList(p.getProperty("inn.playerNewsEvents", ""))) { PlayerNewsEvent ev = PlayerNewsEvent.parse(ns); if (ev != null) g.playerNewsEvents.add(ev); }
        g.lastPlayerNewsReport = p.getProperty("inn.lastPlayerNewsReport", g.lastPlayerNewsReport);
        g.authorityFacilityInspectionCooldownUntilTurn = getInt(p,"inn.authorityFacilityInspectionCooldown",0);
        g.lastAuthorityFacilityInspectionReport = p.getProperty("inn.lastAuthorityFacilityInspection", g.lastAuthorityFacilityInspectionReport);
        g.openBankAccounts.clear(); g.openBankAccounts.addAll(decList(p.getProperty("bank.accounts", "")));
        g.bankBalances.clear(); for(String bs : decList(p.getProperty("bank.balances", ""))) { String[] a=bs.split(":",2); if(a.length==2) try{g.bankBalances.put(a[0], Integer.parseInt(a[1]));}catch(Exception ignored){} }
        g.lastBankReport = p.getProperty("bank.lastReport", g.lastBankReport);
        g.lastBankHeistReport = p.getProperty("bank.lastHeistReport", g.lastBankHeistReport);
        g.lastBankAlarmReport = p.getProperty("bank.lastAlarmReport", g.lastBankAlarmReport);
        g.lastBankLockboxContractReport = p.getProperty("bank.lastLockboxContractReport", g.lastBankLockboxContractReport);
        g.bankHeistAlarmCooldownUntilTurn = getInt(p,"bank.heistAlarmCooldown",0);
        g.lootedBankVaultIds.clear(); g.lootedBankVaultIds.addAll(decList(p.getProperty("bank.lootedVaultIds", "")));
        g.disabledBankAlarmPanelIds.clear(); g.disabledBankAlarmPanelIds.addAll(decList(p.getProperty("bank.disabledAlarmPanelIds", "")));
        g.lastCrimePunishmentReport = p.getProperty("crime.lastPunishment", g.lastCrimePunishmentReport);
        g.lastCustodyReportDetailed = p.getProperty("crime.lastCustodyDetail", g.lastCustodyReportDetailed);
        g.arbitesCaptureCooldownUntilTurn = getInt(p,"crime.arbitesCaptureCooldown",0);
        g.arbitesInspectionCooldownUntilTurn = getInt(p,"crime.arbitesInspectionCooldown",0);
        g.lastArbitesPatrolReport = p.getProperty("crime.lastArbitesPatrol", g.lastArbitesPatrolReport);
        g.lastItemLedgerAuditReport = p.getProperty("item.lastLedgerAudit", g.lastItemLedgerAuditReport);
        g.seedNpcFactionProductionSites();
        g.scavengeCooldownUntilTurn.clear(); for(String s: decList(p.getProperty("scavenge.cooldowns",""))) { String[] a=s.split(":",2); if(a.length==2) try{g.scavengeCooldownUntilTurn.put(Integer.parseInt(a[0]),Integer.parseInt(a[1]));}catch(Exception ignored){} }
        g.baseClaimed=Boolean.parseBoolean(p.getProperty("base.claimed","false")); g.baseX=getInt(p,"base.x",-1); g.baseY=getInt(p,"base.y",-1); g.claimedRoomId=getInt(p,"base.room",-1);
        g.baseObjects.clear(); for(String s: decList(p.getProperty("base.objects",""))) { String[] a=s.split("\\|",31); if(a.length>=5) try{ BaseObject b=new BaseObject(a[0], a[1].isEmpty()?'?':a[1].charAt(0), Integer.parseInt(a[2]), Integer.parseInt(a[3]), 0, 0); b.capacity=Integer.parseInt(a[4]); if(a.length>=6) b.qualityName=a[5]; if(a.length>=7) try{ b.faction=Faction.valueOf(a[6]); }catch(Exception ignored){} if(a.length>=8) b.charges=Integer.parseInt(a[7]); if(a.length>=9) b.integrity=Integer.parseInt(a[8]); if(a.length>=10) b.assignedRecipe=BaseObject.decodeDelimitedField(a[9]); if(a.length>=11) b.assignedWorker=a[10]; if(a.length>=12) b.businessOpen=Boolean.parseBoolean(a[11]); if(a.length>=13) b.permittedBusiness=Boolean.parseBoolean(a[12]); if(a.length>=14) try{ b.businessHeat=Integer.parseInt(a[13]); }catch(Exception ignored){} if(a.length>=15) try{ b.productionQueueTarget=Integer.parseInt(a[14]); }catch(Exception ignored){} if(a.length>=16) try{ b.productionQueueRemaining=Integer.parseInt(a[15]); }catch(Exception ignored){} if(a.length>=17) b.underConstruction=Boolean.parseBoolean(a[16]); if(a.length>=18 && !a[17].isBlank()) b.finalSymbol=a[17].charAt(0); if(a.length>=19) b.constructionRequiredItems=a[18]; if(a.length>=20) b.constructionInsertedItems=a[19]; if(a.length>=21) try{ b.constructionLaborRequired=Integer.parseInt(a[20]); }catch(Exception ignored){} if(a.length>=22) try{ b.constructionLaborDone=Integer.parseInt(a[21]); }catch(Exception ignored){} if(a.length>=23) try{ b.constructionVisualProgress=Integer.parseInt(a[22]); }catch(Exception ignored){} if(a.length>=24) b.machineKnowledge=a[23]; if(a.length>=25) b.machineRepairHistory=a[24]; if(a.length>=26 && !a[25].isBlank()) b.constructionOriginalTile=a[25].charAt(0); if(a.length>=27) try{ b.productionProgressTurns=Math.max(0,Integer.parseInt(a[26])); }catch(Exception ignored){} if(a.length>=28) b.productionMaterialPolicy=a[27]; if(a.length>=29) b.productionOutputPolicy=a[28]; if(a.length>=30) b.productionNoRoomPolicy=a[29]; if(a.length>=31) b.productionLastBlocker=BaseObject.decodeDelimitedField(a[30]); if(!b.underConstruction) g.configureBaseObject(b); g.baseObjects.add(b); if(g.world.inBounds(b.x,b.y)) g.world.tiles[b.x][b.y]=b.symbol; }catch(Exception ignored){} }
        g.factionRecruits.clear(); for(String rs: decList(p.getProperty("base.recruits",""))) { RecruitWorker r = RecruitWorker.parse(rs); if(r != null) g.factionRecruits.add(r); }
        if (g.machineOperationQueue != null) g.machineOperationQueue.restoreRecentHistory(decList(p.getProperty("machine.queue.history", "")));
        g.nextLogisticsIntentSeq = Math.max(1, getInt(p, "logistics.intent.nextSeq", g.nextLogisticsIntentSeq));
        LogisticsDeliveryIntentAuthority.restoreHistory(g, decList(p.getProperty("logistics.intent.history", "")));
        g.lastLogisticsDeliveryIntentReport = p.getProperty("logistics.intent.lastReport", g.lastLogisticsDeliveryIntentReport);
        g.nextLogisticsSourceReservationSeq = Math.max(1, getInt(p, "logistics.source.nextSeq", g.nextLogisticsSourceReservationSeq));
        LogisticsSourceReservationAuthority.restoreHistory(g, decList(p.getProperty("logistics.source.history", "")));
        g.lastLogisticsSourceReservationReport = p.getProperty("logistics.source.lastReport", g.lastLogisticsSourceReservationReport);
        g.nextLogisticsRouteIntentSeq = Math.max(1, getInt(p, "logistics.route.nextSeq", g.nextLogisticsRouteIntentSeq));
        LogisticsRouteIntentAuthority.restoreHistory(g, decList(p.getProperty("logistics.route.history", "")));
        g.lastLogisticsRouteIntentReport = p.getProperty("logistics.route.lastReport", g.lastLogisticsRouteIntentReport);
        g.nextLogisticsRoutePreviewSeq = Math.max(1, getInt(p, "logistics.routePreview.nextSeq", g.nextLogisticsRoutePreviewSeq));
        LogisticsRouteReadinessPreviewAuthority.restoreHistory(g, decList(p.getProperty("logistics.routePreview.history", "")));
        g.lastLogisticsRoutePreviewReport = p.getProperty("logistics.routePreview.lastReport", g.lastLogisticsRoutePreviewReport);
        g.nextLogisticsHaulContractSeq = Math.max(1, getInt(p, "logistics.haulContract.nextSeq", g.nextLogisticsHaulContractSeq));
        LogisticsManualHaulContractAuthority.restoreHistory(g, decList(p.getProperty("logistics.haulContract.history", "")));
        g.lastLogisticsHaulContractReport = p.getProperty("logistics.haulContract.lastReport", g.lastLogisticsHaulContractReport);
        g.nextLogisticsHaulPreflightSeq = Math.max(1, getInt(p, "logistics.haulPreflight.nextSeq", g.nextLogisticsHaulPreflightSeq));
        LogisticsHaulFulfillmentPreflightAuthority.restoreHistory(g, decList(p.getProperty("logistics.haulPreflight.history", "")));
        g.lastLogisticsHaulPreflightReport = p.getProperty("logistics.haulPreflight.lastReport", g.lastLogisticsHaulPreflightReport);
        g.nextLogisticsContractLifecycleSeq = Math.max(1, getInt(p, "logistics.contractLifecycle.nextSeq", g.nextLogisticsContractLifecycleSeq));
        LogisticsContractLifecycleAuthority.restoreHistory(g, decList(p.getProperty("logistics.contractLifecycle.history", "")));
        g.lastLogisticsContractLifecycleReport = p.getProperty("logistics.contractLifecycle.lastReport", g.lastLogisticsContractLifecycleReport);
        g.nextLogisticsHaulExecutionSeq = Math.max(1, getInt(p, "logistics.haulExecution.nextSeq", g.nextLogisticsHaulExecutionSeq));
        LogisticsManualHaulExecutionAuthority.restoreHistory(g, decList(p.getProperty("logistics.haulExecution.history", "")));
        g.lastLogisticsHaulExecutionReport = p.getProperty("logistics.haulExecution.lastReport", g.lastLogisticsHaulExecutionReport);
        readWorldState(g.world, p);
        g.markZoneVisitedAndCheckFirstType();
        g.migrateLegacyPhysicalScript("save/read");
        g.auditItemLedgers("save/read");
        DebugLog.audit("SAVE_READ_CORE", "version=" + version + " state=" + g.stateSummary());
    }

    static void writeCandidate(Candidate c, Properties p) {
        put(p,"char.name",c.name); put(p,"char.job",c.job); put(p,"char.ageYears",Integer.toString(c.ageYears)); put(p,"char.birthWorldTurn",Long.toString(c.birthWorldTurn)); put(p,"char.ageBand",c.ageBand); put(p,"char.portraitSheet",Integer.toString(c.portraitSheet)); put(p,"char.portraitIndex",Integer.toString(c.portraitIndex)); put(p,"char.nameLockedProfileKey",c.nameLockedProfileKey==null?"":c.nameLockedProfileKey);
        ArrayList<String> stats=new ArrayList<>(); for(Map.Entry<String,Integer> e:c.stats.entrySet()) stats.add(e.getKey()+":"+e.getValue()); put(p,"char.stats",encList(stats));
        put(p,"char.visitedTypes",encList(new ArrayList<String>(c.visitedZoneTypes)));
    }
    static Candidate readCandidate(Properties p, long seed) {
        Candidate c = Candidate.random(new Random(seed ^ 0xC0FFEE));
        c.name=p.getProperty("char.name",c.name); c.job=p.getProperty("char.job",c.job); c.ageYears=getInt(p,"char.ageYears",c.ageYears); c.birthWorldTurn=getLong(p,"char.birthWorldTurn",c.birthWorldTurn); c.ageBand=p.getProperty("char.ageBand",c.ageBand); c.portraitSheet=getInt(p,"char.portraitSheet",c.portraitSheet); c.portraitIndex=getInt(p,"char.portraitIndex",c.portraitIndex); c.nameLockedProfileKey=p.getProperty("char.nameLockedProfileKey","");
        for(String s: decList(p.getProperty("char.stats",""))) { String[] a=s.split(":",2); if(a.length==2) try{ c.stats.put(a[0],Integer.parseInt(a[1])); }catch(Exception ignored){} }
        c.visitedZoneTypes.clear(); c.visitedZoneTypes.addAll(decList(p.getProperty("char.visitedTypes","")));
        return c;
    }
    static void writeWorldState(World w, Properties p) {
        if (w == null) return;
        put(p,"world.seed",Long.toString(w.seed)); put(p,"world.w",Integer.toString(w.w)); put(p,"world.h",Integer.toString(w.h)); put(p,"world.zoneType",w.zoneType.name()); put(p,"world.coord",w.sectorX+","+w.sectorY+","+w.zoneX+","+w.zoneY+","+w.floor+","+w.sewerLayer); put(p,"world.hiveName",w.hiveName); put(p,"world.sectorName",w.sectorName); put(p,"world.zoneName",w.zoneName); put(p,"world.zoneHistory",w.zoneHistory); put(p,"world.zoneFacilityHistory",w.zoneFacilityHistory); put(p,"world.zoneProductionHistory",w.zoneProductionHistory); put(p,"world.zoneStockMovementHistory",w.zoneStockMovementHistory); put(p,"world.zoneConflictLossHistory",w.zoneConflictLossHistory); put(p,"world.zoneMaterializedItemHistory",w.zoneMaterializedItemHistory); put(p,"world.zoneLaborAssignmentHistory",w.zoneLaborAssignmentHistory);
        ArrayList<String> objs=new ArrayList<>(); for(MapObjectState m:w.mapObjects) objs.add(m.id+"|"+m.type+"|"+m.label+"|"+m.x+"|"+m.y+"|"+m.cooldownUntilTurn+"|"+m.vendCount+"|"+m.glyph+"|"+m.stockState); put(p,"world.mapObjects",encList(objs));
        ArrayList<String> lsrc=new ArrayList<>(); for(ZoneLightSourceRecord l:w.lightSources) if(l!=null) lsrc.add(l.encode()); put(p,"world.lightSources",encList(lsrc));
        ArrayList<String> nsrc=new ArrayList<>(); for(ZoneNoiseSourceRecord n:w.noiseSources) if(n!=null) nsrc.add(n.encode()); put(p,"world.noiseSources",encList(nsrc));
        ArrayList<String> hsrc=new ArrayList<>(); for(EnvironmentalHazardRecord h:w.hazardWarnings) if(h!=null) hsrc.add(h.encode()); put(p,"world.hazardWarnings",encList(hsrc));
        ArrayList<String> tsrc=new ArrayList<>(); for(TrapRecord t:w.trapRecords) if(t!=null) tsrc.add(t.encode()); put(p,"world.trapRecords",encList(tsrc));
        put(p,"world.hazardVisibilitySummary",w.hazardVisibilitySummary);
        put(p,"world.trapInteractionSummary",w.trapInteractionSummary);
        put(p,"world.lightNoiseSummary",w.lightNoiseSummary==null?"":w.lightNoiseSummary);
        ArrayList<String> npcs=new ArrayList<>(); for(NpcEntity n:w.npcs) npcs.add(n.saveLine()); put(p,"world.npcs",encList(npcs));
        ArrayList<String> repl=new ArrayList<>(); for(PersonnelReplacementRequest rr:w.replacementQueue) repl.add(rr.saveLine()); put(p,"world.personnelReplacements",encList(repl));
        ArrayList<String> popLedgers=new ArrayList<>(); for(RoomPopulationLedger pl:w.roomPopulationLedgers) popLedgers.add(pl.saveLine()); put(p,"world.populationLedgers",encList(popLedgers));
        ArrayList<String> crecheCohorts=new ArrayList<>(); for(CrecheCohortRecord cc:w.crecheCohorts) if(cc!=null) crecheCohorts.add(cc.saveLine()); put(p,"world.crecheCohorts",encList(crecheCohorts));
        ArrayList<String> essentialReserves=new ArrayList<>(); for(EssentialSupplyReserveRecord er:w.essentialSupplyReserves) if(er!=null) essentialReserves.add(er.saveLine()); put(p,"world.essentialSupplyReserves",encList(essentialReserves));
        ArrayList<String> verticalReserves=new ArrayList<>(); for(VerticalTradeReserveRecord vr:w.verticalTradeReserves) if(vr!=null) verticalReserves.add(vr.saveLine()); put(p,"world.verticalTradeReserves",encList(verticalReserves));
        ArrayList<String> factionHappiness=new ArrayList<>(); for(Map.Entry<Faction,Integer> e:w.factionHappinessBoost.entrySet()) factionHappiness.add(e.getKey().name()+":"+Math.max(0,e.getValue())); put(p,"world.factionHappinessBoost",encList(factionHappiness));
        ArrayList<String> roomFaction=new ArrayList<>(); for(Faction f:w.roomFactions) roomFaction.add(f.name()); put(p,"world.roomFactions",encList(roomFaction));
    }
    static void readWorldState(World w, Properties p) {
        if (w == null) return;
        w.hiveName = p.getProperty("world.hiveName", w.hiveName); w.sectorName = p.getProperty("world.sectorName", w.sectorName); w.zoneName = p.getProperty("world.zoneName", w.zoneName); w.zoneHistory = p.getProperty("world.zoneHistory", w.zoneHistory); w.zoneFacilityHistory = p.getProperty("world.zoneFacilityHistory", w.zoneFacilityHistory); w.zoneProductionHistory = p.getProperty("world.zoneProductionHistory", w.zoneProductionHistory); w.zoneStockMovementHistory = p.getProperty("world.zoneStockMovementHistory", w.zoneStockMovementHistory); w.zoneConflictLossHistory = p.getProperty("world.zoneConflictLossHistory", w.zoneConflictLossHistory); w.zoneMaterializedItemHistory = p.getProperty("world.zoneMaterializedItemHistory", w.zoneMaterializedItemHistory); w.zoneLaborAssignmentHistory = p.getProperty("world.zoneLaborAssignmentHistory", w.zoneLaborAssignmentHistory);
        java.util.List<String> roomFaction=decList(p.getProperty("world.roomFactions","")); for(int i=0;i<roomFaction.size()&&i<w.roomFactions.size();i++) try{w.roomFactions.set(i,Faction.valueOf(roomFaction.get(i)));}catch(Exception ignored){}
        java.util.List<String> objs=decList(p.getProperty("world.mapObjects","")); if(!objs.isEmpty()){ w.mapObjects.clear(); for(String s:objs){ String[] a=s.split("\\|",9); if(a.length>=9) try{ MapObjectState m=new MapObjectState(); m.id=a[0];m.type=a[1];m.label=a[2];m.x=Integer.parseInt(a[3]);m.y=Integer.parseInt(a[4]);m.cooldownUntilTurn=Integer.parseInt(a[5]);m.vendCount=Integer.parseInt(a[6]);m.glyph=a[7].isEmpty()?'?':a[7].charAt(0);m.stockState=a[8]; w.mapObjects.add(m); if(w.inBounds(m.x,m.y)) w.tiles[m.x][m.y]=m.glyph; }catch(Exception ignored){} } }
        w.lightSources.clear(); for(String ls: decList(p.getProperty("world.lightSources",""))){ ZoneLightSourceRecord l=ZoneLightSourceRecord.parse(ls); if(l!=null) w.lightSources.add(l); }
        w.noiseSources.clear(); for(String nz: decList(p.getProperty("world.noiseSources",""))){ ZoneNoiseSourceRecord n=ZoneNoiseSourceRecord.parse(nz); if(n!=null) w.noiseSources.add(n); }
        w.hazardWarnings.clear(); for(String hz: decList(p.getProperty("world.hazardWarnings",""))){ EnvironmentalHazardRecord h=EnvironmentalHazardRecord.parse(hz); if(h!=null) w.hazardWarnings.add(h); }
        w.trapRecords.clear(); for(String tr: decList(p.getProperty("world.trapRecords",""))){ TrapRecord t=TrapRecord.parse(tr); if(t!=null) w.trapRecords.add(t); }
        w.trapInteractionSummary = p.getProperty("world.trapInteractionSummary", w.trapInteractionSummary);
        w.lightNoiseSummary = p.getProperty("world.lightNoiseSummary", w.lightNoiseSummary);
        w.hazardVisibilitySummary = p.getProperty("world.hazardVisibilitySummary", w.hazardVisibilitySummary);
        java.util.List<String> ns=decList(p.getProperty("world.npcs","")); if(!ns.isEmpty()){ w.npcs.clear(); for(String s:ns){ try{ NpcEntity n=NpcEntity.parseLine(s, w); if(n!=null) w.npcs.add(n); }catch(Exception ignored){} } }
        java.util.List<String> repl=decList(p.getProperty("world.personnelReplacements","")); w.replacementQueue.clear(); for(String s:repl){ PersonnelReplacementRequest rr=PersonnelReplacementRequest.parse(s); if(rr!=null) w.replacementQueue.add(rr); }
        java.util.List<String> popLedgers=decList(p.getProperty("world.populationLedgers","")); w.roomPopulationLedgers.clear(); for(String s:popLedgers){ RoomPopulationLedger pl=RoomPopulationLedger.parse(s); if(pl!=null) w.roomPopulationLedgers.add(pl); } if(w.roomPopulationLedgers.isEmpty()) PersonnelPopulationApi.ensureLedgers(w, new Random(w.seed ^ 0x5070A11L));
        java.util.List<String> crecheCohorts=decList(p.getProperty("world.crecheCohorts","")); w.crecheCohorts.clear(); for(String s:crecheCohorts){ CrecheCohortRecord cc=CrecheCohortRecord.parse(s); if(cc!=null) w.crecheCohorts.add(cc); }
        java.util.List<String> essentialReserves=decList(p.getProperty("world.essentialSupplyReserves","")); w.essentialSupplyReserves.clear(); for(String s:essentialReserves){ EssentialSupplyReserveRecord er=EssentialSupplyReserveRecord.parse(s); if(er!=null) w.essentialSupplyReserves.add(er); }
        java.util.List<String> verticalReserves=decList(p.getProperty("world.verticalTradeReserves","")); w.verticalTradeReserves.clear(); for(String s:verticalReserves){ VerticalTradeReserveRecord vr=VerticalTradeReserveRecord.parse(s); if(vr!=null) w.verticalTradeReserves.add(vr); }
        w.factionHappinessBoost.clear(); readFactionMap(p.getProperty("world.factionHappinessBoost",""), w.factionHappinessBoost);
    }
    static void readFactionMap(String text, EnumMap<Faction,Integer> map) { for(String s: decList(text)) { String[] a=s.split(":",2); if(a.length==2) try{ map.put(Faction.valueOf(a[0]),Integer.parseInt(a[1])); }catch(Exception ignored){} } }
    static void put(Properties p,String k,String v){ if(v!=null) p.setProperty(k,v); }
    static int getInt(Properties p,String k,int d){ try{return Integer.parseInt(p.getProperty(k,""+d));}catch(Exception e){return d;} }
    static long getLong(Properties p,String k,long d){ try{return Long.parseLong(p.getProperty(k,""+d));}catch(Exception e){return d;} }
    static int clamp(int v,int lo,int hi){ return Math.max(lo,Math.min(hi,v)); }
    static String enc(String s){ return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String s){ try{return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return "";} }
    static String encList(java.util.List<String> list){ ArrayList<String> out=new ArrayList<>(); for(String s:list) out.add(enc(s)); return String.join(",",out); }
    static java.util.List<String> decList(String text){ ArrayList<String> out=new ArrayList<>(); if(text==null||text.isEmpty()) return out; for(String s:text.split(",")) if(!s.isEmpty()) out.add(dec(s)); return out; }
}

class SaveSlotSurfaceApi {
    static Path savePathForSlot(int slot) {
        return ServerRuntimePaths.slotPath(ServerRuntimePaths.SaveDomain.SINGLE_PLAYER, slot);
    }

    static String slotLabel(int slot) {
        if (slot == GamePanel.AUTOSAVE_HOURLY_SLOT) return "Hourly Autosave";
        if (slot == GamePanel.AUTOSAVE_ZONE_SLOT) return "Zone Autosave";
        return "Slot " + slot;
    }

    static String saveSlotSummary(int slot) {
        Path file = savePathForSlot(slot);
        if (!Files.exists(file)) return slotLabel(slot) + ": empty";
        Properties pr = new Properties();
        try (InputStream in = Files.newInputStream(file)) { pr.load(in); }
        catch (IOException ex) { return slotLabel(slot) + ": unreadable at " + file.toString(); }
        String character = pr.getProperty("char.name", "unknown character");
        String worldId = pr.getProperty("save.worldId", pr.getProperty("worlddef.worldId", "unknown-world"));
        String worldFile = pr.getProperty("save.worldFile", ServerRuntimePaths.singlePlayerWorldReference(worldId));
        String seedText = pr.getProperty("run.seed", pr.getProperty("atlas.seed", "unknown seed"));
        int t = Persistence.getInt(pr, "run.turn", 0);
        int hour = t / Math.max(1, GamePanel.TURNS_PER_HOUR);
        int day = hour / Math.max(1, GamePanel.HOURS_PER_DAY);
        int remHour = hour % Math.max(1, GamePanel.HOURS_PER_DAY);
        String sx = pr.getProperty("atlas.sx", "?");
        String sy = pr.getProperty("atlas.sy", "?");
        String zx = pr.getProperty("atlas.zx", "?");
        String zy = pr.getProperty("atlas.zy", "?");
        String floor = pr.getProperty("atlas.floor", "?");
        String sewer = Boolean.parseBoolean(pr.getProperty("atlas.sewer", "false")) ? "B" : "";
        String zoneName = pr.getProperty("world.zoneName", "unknown zone");
        String created = pr.getProperty("save.created", "unknown date");
        return slotLabel(slot) + ": " + character + " | " + worldFile + " | seed " + seedText + " | turn " + t + " day " + day + " hour " + remHour + " | sector " + sx + "," + sy + " zone " + zx + "," + zy + " floor " + floor + sewer + " | " + zoneName + " | saved " + created;
    }

    static String auditSummary() {
        return "saveSlotSurface slots=" + GamePanel.SAVE_SLOT_COUNT + " autosaves=hourly+zone singlePlayerPaths=saves/singleplayer/*.mechsave serverPaths=saves/server/slots/*.mechsave";
    }
}


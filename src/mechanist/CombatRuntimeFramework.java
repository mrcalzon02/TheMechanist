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

class ExplosiveProfile {
    final String name; final int radius, damage, fuseTurns, throwRange, complexity, disarmTurns; final boolean plantable, smoke;
    ExplosiveProfile(String name, int radius, int damage, int fuseTurns, int throwRange, int complexity, int disarmTurns, boolean plantable, boolean smoke){ this.name=name; this.radius=radius; this.damage=damage; this.fuseTurns=fuseTurns; this.throwRange=throwRange; this.complexity=complexity; this.disarmTurns=disarmTurns; this.plantable=plantable; this.smoke=smoke; }
    static boolean isExplosive(String item){ return ItemCatalog.isExplosiveLike(item); }
    static ExplosiveProfile forItem(String raw){
        String item = MapObjectState.itemNameFromStock(raw);
        String low = item == null ? "" : ItemQuality.stripManufacturingIdentity(item).toLowerCase(Locale.ROOT);
        if (low.contains("smoke")) return new ExplosiveProfile(item.isBlank()?"Smoke grenade":item, 4, 2, 2, 7, 2, 1, false, true);
        if (low.contains("krak")) return new ExplosiveProfile(item.isBlank()?"Krak grenade":item, 1, 22, 2, 6, 3, 2, false, false);
        if (low.contains("melta")) return new ExplosiveProfile(item.isBlank()?"Melta grenade":item, 2, 30, 2, 5, 4, 3, false, false);
        if (low.contains("plasma")) return new ExplosiveProfile(item.isBlank()?"Plasma bomb":item, 3, 26, 3, 5, 5, 4, true, false);
        if (low.contains("satchel")) return new ExplosiveProfile(item.isBlank()?"Satchel charge":item, 3, 28, 4, 4, 4, 3, true, false);
        if (low.contains("tripwire")) return new ExplosiveProfile(item.isBlank()?"Tripwire mine":item, 2, 18, 1, 2, 3, 2, true, false);
        if (low.contains("claymore")) return new ExplosiveProfile(item.isBlank()?"Motion claymore":item, 3, 22, 1, 2, 4, 3, true, false);
        if (low.contains("bouncing")) return new ExplosiveProfile(item.isBlank()?"Bouncing Betty":item, 3, 24, 1, 2, 5, 4, true, false);
        return new ExplosiveProfile(item==null||item.isBlank()?"Frag grenade":item, 2, 16, 2, 7, 2, 1, false, false);
    }
}



class ExplosionAnimation {
    final int x0, y0, x1, y1, radius; final String name; final boolean grenadeArc; final long startMillis; final int durationMillis;
    ExplosionAnimation(int x0,int y0,int x1,int y1,int radius,String name,boolean grenadeArc,long startMillis,int durationMillis){ this.x0=x0; this.y0=y0; this.x1=x1; this.y1=y1; this.radius=radius; this.name=name==null?"explosion":name; this.grenadeArc=grenadeArc; this.startMillis=startMillis; this.durationMillis=durationMillis; }
    static ExplosionAnimation grenade(int x0,int y0,int x1,int y1,String name,long start){ return new ExplosionAnimation(x0,y0,x1,y1,0,name,true,start,420); }
    static ExplosionAnimation blast(int x,int y,int radius,String name,long start){ return new ExplosionAnimation(x,y,x,y,Math.max(1,radius),name,false,start,520); }
}



class CombatAnimation {
    final String weapon;
    final int x0, y0, x1, y1;
    final boolean hit;
    final long startMillis;
    final int durationMillis;
    CombatAnimation(String weapon, int x0, int y0, int x1, int y1, boolean hit, long startMillis) {
        this.weapon = weapon == null ? "bare hands" : weapon;
        this.x0=x0; this.y0=y0; this.x1=x1; this.y1=y1; this.hit=hit; this.startMillis=startMillis;
        this.durationMillis = 360;
    }
}



class SensePing {
    int x, y, ttl; String label;
    SensePing(int x, int y, String label, int ttl){ this.x=x; this.y=y; this.label=label; this.ttl=ttl; }
}



class TargetingSolution {
    int targetX, targetY;
    int distance = 0;
    int range = 1;
    int hitPercent = 0;
    int coverPenalty = 0;
    int darknessPenalty = 0;
    int shieldReduction = 0;
    int loadedShots = 0;
    int magazineCapacity = 0;
    int ammoCost = 0;
    String ammoItem = "";
    boolean needsReload = false;
    boolean canFire = false;
    boolean ranged = false;
    boolean coverHardBlocked = false;
    String weaponName = "Bare hands";
    String fireMode = "SNAP";
    String coverSummary = "clear firing lane";
    String summary = "No target selected.";
    NpcEntity targetNpc = null;
    String summaryLine(){ return summary == null || summary.isBlank() ? "No target selected." : summary; }
}



class IntentReadResult {
    boolean available = false;
    int degree = 0;
    int margin = 0;
    int visionRange = 0;
    int hearingRange = 0;
    int projectedX = -1, projectedY = -1;
    int facingDx = 1, facingDy = 0;
    boolean attackLine = false;
    String intent = "Unknown";
    String summary = "No intent read.";
}





class PlayerDefeatAndScoreAuthority {
    static int score(GamePanel g) {
        if (g == null) return 0;
        int turns = Math.max(0, g.turn);
        int days = turns / Math.max(1, GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY);
        int wealth = Math.max(0, g.supplies + g.machineParts * 2 + g.food + g.water + g.inventory.size() * 3 + g.baseStorage.size() * 2 + g.carriedScript / 10 + g.totalBankedCash() / 20 + g.baseStashedScript / 15);
        int roomsOwned = g.baseClaimed ? Math.max(1, g.claimedRoomId >= 0 ? 1 : 0) : 0;
        int recruits = g.factionRecruits == null ? 0 : g.factionRecruits.size();
        int sectors = g.visitedZoneTypes == null ? 0 : g.visitedZoneTypes.size();
        int zones = g.visitedZoneInstances == null ? 0 : g.visitedZoneInstances.size();
        int knowledge = g.unlockedKnowledges == null ? 0 : g.unlockedKnowledges.size();
        return turns / 25 + days * 25 + g.runKills * 40 + g.runCrafted * 6 + knowledge * 35 + sectors * 30 + zones * 12 + g.runNpcTalkedTo * 5 + wealth * 2 + roomsOwned * 75 + recruits * 60 + Math.max(0, g.xp / 2) - g.runUnconsciousEvents * 15;
    }

    static ArrayList<String> lossScoreLines(GamePanel g) {
        ArrayList<String> l = new ArrayList<>();
        if (g == null) { l.add("No run data available."); return l; }
        int turns = Math.max(0, g.turn);
        int hours = turns / Math.max(1, GamePanel.TURNS_PER_HOUR);
        int days = hours / GamePanel.HOURS_PER_DAY;
        int remHours = hours % GamePanel.HOURS_PER_DAY;
        int knowledge = g.unlockedKnowledges == null ? 0 : g.unlockedKnowledges.size();
        int sectors = g.visitedZoneTypes == null ? 0 : g.visitedZoneTypes.size();
        int zones = g.visitedZoneInstances == null ? 0 : g.visitedZoneInstances.size();
        int wealth = Math.max(0, g.supplies + g.machineParts * 2 + g.food + g.water + g.inventory.size() * 3 + g.baseStorage.size() * 2 + g.carriedScript / 10 + g.totalBankedCash() / 20 + g.baseStashedScript / 15);
        int roomsOwned = g.baseClaimed ? 1 : 0;
        int recruits = g.factionRecruits == null ? 0 : g.factionRecruits.size();
        int finalScore = score(g);
        l.add("FINAL RUN SUMMARY");
        l.add("Character: " + (g.active == null ? "unknown" : g.active.name + " / " + g.active.job));
        l.add("Killed by: " + g.lastDefeatAttacker);
        l.add("Weapon / method: " + g.lastDefeatWeapon);
        l.add("Fatal cause: " + g.lastDefeatCause + " at " + g.lastDefeatLocation);
        l.add("Survived: turn " + turns + " — about " + days + " day(s), " + remHours + " hour(s). Local time " + g.timeText() + ".");
        l.add("");
        l.add("SCORE BREAKDOWN — total " + finalScore);
        add(l, "Kills", g.runKills, 40, g.runKills * 40);
        add(l, "Things crafted / built / produced", g.runCrafted, 6, g.runCrafted * 6);
        add(l, "Knowledge doctrines learned", knowledge, 35, knowledge * 35);
        add(l, "Sector/zone types seen", sectors, 30, sectors * 30);
        add(l, "Zone instances visited", zones, 12, zones * 12);
        add(l, "NPCs talked to", g.runNpcTalkedTo, 5, g.runNpcTalkedTo * 5);
        add(l, "Estimated wealth", wealth, 2, wealth * 2);
        add(l, "Rooms owned as bases", roomsOwned, 75, roomsOwned * 75);
        add(l, "Recruits retained", recruits, 60, recruits * 60);
        l.add("  XP carryover score: " + Math.max(0, g.xp / 2) + " from " + g.xp + " XP.");
        if (g.runUnconsciousEvents > 0) l.add("  Unconsciousness penalty: -" + (g.runUnconsciousEvents * 15) + " from " + g.runUnconsciousEvents + " collapse event(s).");
        l.add("");
        l.add("End-state resources: supplies " + g.supplies + ", machine parts " + g.machineParts + ", food " + g.food + ", water " + g.water + ", carried script " + g.carriedScript + ", banked script " + g.totalBankedCash() + ", base script " + g.baseStashedScript + ", inventory " + g.inventory.size() + ", base storage " + g.baseStorage.size() + ".");
        l.add("Health at loss: wounds " + g.wounds + ", bleeding " + g.bleeding + ", infection risk " + g.infectionRisk + ", pain " + g.pain + ", fatigue " + g.fatigue + ", sleep debt " + g.sleepNeed + ".");
        l.add("Use LOAD SAVE below to open the save/load menu, then choose a manual slot, hourly autosave, or zone-transition autosave. Quit returns to the main menu.");
        return l;
    }

    static void add(ArrayList<String> l, String label, int count, int weight, int subtotal) {
        l.add("  " + label + ": " + count + " x " + weight + " = " + subtotal);
    }
}



class BodyPart {
    String name;
    String slot;
    int endurance;
    int agility;
    double health;
    BodyPart(String name, String slot, int endurance, int agility) {
        this.name = name;
        this.slot = slot;
        this.endurance = endurance;
        this.agility = agility;
        this.health = endurance * 100.0;
    }
    double maxHealth() { return Math.max(1.0, endurance * 100.0); }
    double currentHealth() { return Math.max(0.0, Math.min(health, maxHealth())); }
    boolean destroyed() { return currentHealth() <= 0.0; }
    String displayLine() {
        return String.format(Locale.US, "%-12s END %d  HP %.0f/%.0f  AGI %d  [%s]", name, endurance, currentHealth(), maxHealth(), agility, slot == null || slot.isEmpty() ? "empty" : slot);
    }
    BodyPart copy() { BodyPart b = new BodyPart(name, slot, endurance, agility); b.health = health; return b; }
}






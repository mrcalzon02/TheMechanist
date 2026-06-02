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

class TileMotion {
    final String actorId;
    final int fromX, fromY, toX, toY;
    final long startMillis;
    final int durationMillis;
    TileMotion(String actorId, int fromX, int fromY, int toX, int toY, long startMillis, int durationMillis) {
        this.actorId = actorId == null ? "actor" : actorId;
        this.fromX = fromX; this.fromY = fromY; this.toX = toX; this.toY = toY;
        this.startMillis = startMillis;
        this.durationMillis = Math.max(25, durationMillis);
    }
    double progress(long now) { return Math.max(0.0, Math.min(1.0, (now - startMillis) / (double)durationMillis)); }
    boolean done(long now) { return now - startMillis >= durationMillis; }
}


class PortableLightProfile {
    final String name;
    final int radius, durationTurns, throwRange;
    final boolean wearable, consumedOnUse;
    final String note;
    PortableLightProfile(String name, int radius, int hours, int throwRange, boolean wearable, boolean consumedOnUse, String note) {
        this.name = name; this.radius = radius; this.durationTurns = Math.max(1, hours) * GamePanel.TURNS_PER_HOUR; this.throwRange = throwRange; this.wearable = wearable; this.consumedOnUse = consumedOnUse; this.note = note;
    }
    int remainingDuration(int turn, String activeItem, int activeExpires) {
        if (name.equals(activeItem) && activeExpires > turn) return Math.max(1, activeExpires - turn);
        return durationTurns;
    }
    static final LinkedHashMap<String,PortableLightProfile> PROFILES = make();
    static LinkedHashMap<String,PortableLightProfile> make() {
        LinkedHashMap<String,PortableLightProfile> m = new LinkedHashMap<>();
        add(m, new PortableLightProfile("Flashlight", 5, 18, 4, false, false, "narrow practical beam; batteries are abstracted for the first lighting pass."));
        add(m, new PortableLightProfile("Glow stick", 3, 6, 5, false, true, "single-use chemical glow; good marker, poor dignity."));
        add(m, new PortableLightProfile("Lantern", 6, 14, 3, false, false, "broader pool of light, more obvious to anyone nearby."));
        add(m, new PortableLightProfile("Stub light", 2, 3, 4, false, false, "junk bulb/battery/wire bundle; cheap, short-lived, and better than blackness."));
        add(m, new PortableLightProfile("Mining helmet", 5, 20, 1, true, false, "hands-free forward lamp; useful in tunnels and maintenance holes."));
        add(m, new PortableLightProfile("Scavenging helmet", 4, 14, 1, true, false, "patched hands-free lamp; lower radius but common in trash work."));
        add(m, new PortableLightProfile("Electrician's rig", 3, 10, 1, true, false, "snap-light headband for work inside junction boxes."));
        add(m, new PortableLightProfile("Phosphor bulb", 4, 8, 5, false, true, "organic light grown in damp and forgotten places; throwable and perishable."));
        add(m, new PortableLightProfile("Swamp lantern", 7, 9, 3, false, false, "volatile chemical flame with a large dirty glow."));
        return m;
    }
    static void add(LinkedHashMap<String,PortableLightProfile> m, PortableLightProfile p){ m.put(p.name.toLowerCase(Locale.ROOT), p); }
    static PortableLightProfile profile(String item) {
        if (item == null) return null;
        String key = ItemQuality.stripManufacturingIdentity(ItemQuality.stripQuality(item)).trim().toLowerCase(Locale.ROOT);
        PortableLightProfile p = PROFILES.get(key);
        if (p != null) return p;
        for (Map.Entry<String,PortableLightProfile> e : PROFILES.entrySet()) if (key.contains(e.getKey())) return e.getValue();
        return null;
    }
    static boolean isLightItem(String item) { return profile(item) != null; }
}


class PortableLightInstance {
    String itemName, worldKey, placement;
    int x, y, radius, expiresTurn;
    PortableLightInstance(String itemName, int x, int y, int radius, int expiresTurn, String worldKey, String placement) {
        this.itemName=itemName; this.x=x; this.y=y; this.radius=radius; this.expiresTurn=expiresTurn; this.worldKey=worldKey; this.placement=placement;
    }
    String encode(){ return esc(itemName)+"|"+x+"|"+y+"|"+radius+"|"+expiresTurn+"|"+esc(worldKey)+"|"+esc(placement); }
    static PortableLightInstance parse(String s){
        try{
            String[] a=s.split("\\|",7); if(a.length<7) return null;
            return new PortableLightInstance(unesc(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]), unesc(a[5]), unesc(a[6]));
        }catch(Exception e){ return null; }
    }
    static String esc(String s){ return s==null?"":s.replace("%","%25").replace("|","%7C"); }
    static String unesc(String s){ return s==null?"":s.replace("%7C","|").replace("%25","%"); }
}




class TrapRecord {
    String id, type, label, effect, owner, state; int x,y,roomId,severity,detectionDifficulty,disarmDifficulty,damage,noiseRadius,triggeredTurn; boolean hidden, reusable, linkedHazard;
    TrapRecord(String id,String type,String label,String effect,String owner,String state,int x,int y,int roomId,int severity,int detectionDifficulty,int disarmDifficulty,int damage,int noiseRadius,int triggeredTurn,boolean hidden,boolean reusable,boolean linkedHazard){this.id=id;this.type=type;this.label=label;this.effect=effect;this.owner=owner;this.state=state;this.x=x;this.y=y;this.roomId=roomId;this.severity=severity;this.detectionDifficulty=detectionDifficulty;this.disarmDifficulty=disarmDifficulty;this.damage=damage;this.noiseRadius=noiseRadius;this.triggeredTurn=triggeredTurn;this.hidden=hidden;this.reusable=reusable;this.linkedHazard=linkedHazard;}
    boolean canTrigger(){ return !"triggered".equals(state) && !"disarmed".equals(state); }
    String triggerText(){ return "TRAP TRIGGERED: " + label + " at " + x + "," + y + " releases " + effect + "."; }
    String encode(){ return esc(id)+"|"+esc(type)+"|"+esc(label)+"|"+esc(effect)+"|"+esc(owner)+"|"+esc(state)+"|"+x+"|"+y+"|"+roomId+"|"+severity+"|"+detectionDifficulty+"|"+disarmDifficulty+"|"+damage+"|"+noiseRadius+"|"+triggeredTurn+"|"+hidden+"|"+reusable+"|"+linkedHazard; }
    static TrapRecord parse(String s){ try{ String[] a=s.split("\\|",18); if(a.length<18)return null; return new TrapRecord(unesc(a[0]),unesc(a[1]),unesc(a[2]),unesc(a[3]),unesc(a[4]),unesc(a[5]),Integer.parseInt(a[6]),Integer.parseInt(a[7]),Integer.parseInt(a[8]),Integer.parseInt(a[9]),Integer.parseInt(a[10]),Integer.parseInt(a[11]),Integer.parseInt(a[12]),Integer.parseInt(a[13]),Integer.parseInt(a[14]),Boolean.parseBoolean(a[15]),Boolean.parseBoolean(a[16]),Boolean.parseBoolean(a[17])); }catch(Exception e){return null;} }
    static String esc(String s){return s==null?"":s.replace("%","%25").replace("|","%7C");} static String unesc(String s){return s==null?"":s.replace("%7C","|").replace("%25","%");}
}


class TrapInteractionApi {
    private TrapInteractionApi() {}
    static class Result { int pressure, wire, gas, shock, noble, total, skipped; String summary(){ return "traps="+total+" pressure="+pressure+" wire="+wire+" gas="+gas+" shock="+shock+" nobleLinked="+noble+" skipped="+skipped; } }
    static Result seed(World w, Random r){
        Result res = new Result(); if(w==null)return res; w.trapRecords.clear();
        int base = (w.zoneType==ZoneType.SECTOR_GOVERNORS_MANSION || w.zoneType==ZoneType.NOBLE_SERVICE_SPINE || w.floor>=7) ? 10 : (w.sewerLayer ? 4 : 3);
        res.pressure=scatter(w,r,"pressure","Pressure Plate","mechanical impact",base, false);
        res.wire=scatter(w,r,"wire","Tripwire Alarm","alarm clatter",Math.max(1,base/2), false);
        res.gas=scatter(w,r,"gas","Gas Release Trap","noxious gas",w.sewerLayer?3:1, true);
        res.shock=scatter(w,r,"shock","Electrified Floor Trap","electrical discharge",w.floor>=4?2:1, true);
        for(TrapRecord t:w.trapRecords) if("NOBLE".equals(t.owner)) res.noble++;
        res.total=w.trapRecords.size(); w.trapInteractionSummary=res.summary(); return res;
    }
    static int scatter(World w, Random r, String type, String label, String effect, int target, boolean linkedHazard){ int made=0, tries=0; while(made<target && tries<target*90+60){ tries++; int x=1+r.nextInt(Math.max(1,w.w-2)), y=1+r.nextInt(Math.max(1,w.h-2)); if(!w.walkable(x,y) || w.npcAt(x,y)!=null || hasTrap(w,x,y)) continue; int room=w.roomIdAt(x,y); boolean noble=w.zoneType==ZoneType.SECTOR_GOVERNORS_MANSION || w.zoneType==ZoneType.NOBLE_SERVICE_SPINE || w.floor>=7 || (room>=0 && room<w.roomFactions.size() && w.roomFactions.get(room)==Faction.NOBLE); int sev=(noble?3:1)+r.nextInt(noble?5:3); String id="TR-"+Math.abs(Objects.hash(w.seed,type,x,y,made)); w.trapRecords.add(new TrapRecord(id,type,label,effect,noble?"NOBLE":"LOCAL","hidden",x,y,room,sev,22+sev*7,24+sev*8,Math.max(0,sev*2),type.equals("wire")?8:Math.max(2,sev+2),-1,true,false,linkedHazard)); made++; } return made; }
    static boolean hasTrap(World w,int x,int y){ for(TrapRecord t:w.trapRecords) if(t.x==x&&t.y==y) return true; return false; }
}


class EnvironmentalHazardRecord {
    String id, family, label, warning;
    int x, y, roomId, severity, phase;
    EnvironmentalHazardRecord(String id, String family, String label, String warning, int x, int y, int roomId, int severity, int phase){ this.id=id;this.family=family;this.label=label;this.warning=warning;this.x=x;this.y=y;this.roomId=roomId;this.severity=severity;this.phase=phase; }
    Color color(){ String f=family==null?"":family.toLowerCase(Locale.ROOT); if(f.contains("wire")||f.contains("electric")) return new Color(255,225,95); if(f.contains("sludge")||f.contains("runoff")||f.contains("caustic")) return new Color(120,235,90); if(f.contains("cold")||f.contains("freezer")) return new Color(150,215,255); if(f.contains("heat")||f.contains("factory")) return new Color(255,120,55); return new Color(175,245,130); }
    String severityText(){ if(severity>=75) return "extreme"; if(severity>=50) return "severe"; if(severity>=30) return "dangerous"; return "suspect"; }
    String warningText(){ return warning==null||warning.isBlank()?"red hazard border and corner warning rune visible":warning; }
    String encode(){ return esc(id)+"|"+esc(family)+"|"+esc(label)+"|"+esc(warning)+"|"+x+"|"+y+"|"+roomId+"|"+severity+"|"+phase; }
    static EnvironmentalHazardRecord parse(String s){ try{ String[] a=s.split("\\|",9); if(a.length<9)return null; return new EnvironmentalHazardRecord(unesc(a[0]),unesc(a[1]),unesc(a[2]),unesc(a[3]),Integer.parseInt(a[4]),Integer.parseInt(a[5]),Integer.parseInt(a[6]),Integer.parseInt(a[7]),Integer.parseInt(a[8])); }catch(Exception e){return null;} }
    static String esc(String s){ return s==null?"":s.replace("%","%25").replace("|","%7C"); }
    static String unesc(String s){ return s==null?"":s.replace("%7C","|").replace("%25","%"); }
}


class EnvironmentalHazardVisibilityApi {
    private EnvironmentalHazardVisibilityApi() {}
    static class Result { int gas, sludge, wires, thermal, total, skipped; String summary(){ return "hazardOverlays=" + total + " gas=" + gas + " sludge=" + sludge + " wires=" + wires + " thermal=" + thermal + " skipped=" + skipped; } }
    static Result seed(World w, Random r){
        Result res = new Result(); if(w==null) return res; if(r==null) r = new Random(w.seed ^ 0x9071L);
        w.hazardWarnings.clear();
        ArrayList<Point> used = new ArrayList<>();
        boolean sewer = w.sewerLayer || w.zoneType==ZoneType.SEWER_CONDUIT || w.zoneType==ZoneType.MUTANT_SEWER_CAMP || w.zoneType==ZoneType.CULTIST_SEWER_CAMP;
        int gasTarget = sewer ? 8 : (w.zoneType==ZoneType.TRASH_WARREN || w.zoneType==ZoneType.MUTANT_WARRENS ? 5 : 3);
        int sludgeTarget = sewer ? 9 : (w.zoneType==ZoneType.TRASH_WARREN ? 4 : 1);
        int wireTarget = (w.zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || w.zoneType==ZoneType.MECHANICUS_RELIC_DUCT || w.zoneType==ZoneType.IMPERIAL_GUARD_BILLET) ? 7 : 3;
        int thermalTarget = (w.zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || w.zoneType==ZoneType.NOBLE_SERVICE_SPINE) ? 4 : 1;
        res.gas = scatter(w,r,used,gasTarget,"noxious gas","Coiling gas","visible haze curls across the tile; filtration or retreat recommended",36,68);
        res.sludge = scatter(w,r,used,sludgeTarget,"caustic sludge","Surging sludge","slick sump runoff surges underfoot; boots and time discipline advised",34,72);
        res.wires = scatter(w,r,used,wireTarget,"shorted wires","Crackling wires","exposed conductors crackle; crossing later should test agility, tools, and insulation",30,66);
        res.thermal = scatter(w,r,used,thermalTarget,"thermal hazard", sewer?"Freezer/steam pocket":"Oppressive heat pocket", sewer?"cold fog or steam wash marks a temperature hazard":"shimmering air marks an unsafe factory heat pocket",28,60);
        res.total = w.hazardWarnings.size();
        w.hazardVisibilitySummary = res.summary();
        return res;
    }
    static int scatter(World w, Random r, ArrayList<Point> used, int target, String family, String label, String warning, int sevMin, int sevMax){
        int made=0, tries=0;
        while(made<target && tries<target*90+80){ tries++; int x=1+r.nextInt(Math.max(1,w.w-2)); int y=1+r.nextInt(Math.max(1,w.h-2)); if(!validHazardTile(w,x,y,used)) continue; int room=w.roomIdAt(x,y); int severity=sevMin+r.nextInt(Math.max(1,sevMax-sevMin+1)); String id="HZ-"+Math.abs(Objects.hash(w.seed,family,x,y,made)); w.hazardWarnings.add(new EnvironmentalHazardRecord(id,family,label,warning,x,y,room,severity,r.nextInt(3))); used.add(new Point(x,y)); made++; }
        return made;
    }
    static boolean validHazardTile(World w, int x, int y, ArrayList<Point> used){
        if(w==null || !w.inBounds(x,y) || !w.walkable(x,y)) return false;
        char ch=w.tiles[x][y]; if(ch=='+' || ch=='a' || ch=='o' || ch=='<' || ch=='>') return false;
        for(Point p: used) if(Math.abs(x-p.x)+Math.abs(y-p.y)<3) return false;
        if(w.mapObjectAt(x,y)!=null) return false;
        return true;
    }
}


class ZoneLightSourceRecord {
    String id, profile, groupId, colorName;
    int x, y, roomId, radius, intensity, phase, flickerPeriod;
    boolean on, powered, flicker, switchControlled;
    ZoneLightSourceRecord(String id, String profile, int x, int y, int roomId, int radius, int intensity, String colorName, boolean on, boolean powered, boolean flicker, int flickerPeriod, int phase, boolean switchControlled, String groupId){
        this.id=id; this.profile=profile; this.x=x; this.y=y; this.roomId=roomId; this.radius=radius; this.intensity=intensity; this.colorName=colorName; this.on=on; this.powered=powered; this.flicker=flicker; this.flickerPeriod=flickerPeriod; this.phase=phase; this.switchControlled=switchControlled; this.groupId=groupId;
    }
    Color color(){
        String c = colorName==null?"":colorName.toLowerCase(Locale.ROOT);
        if(c.contains("green")) return new Color(145,245,155);
        if(c.contains("blue")) return new Color(145,190,255);
        if(c.contains("red")) return new Color(255,120,100);
        if(c.contains("sick") || c.contains("sump")) return new Color(175,235,115);
        if(c.contains("amber")) return new Color(255,205,115);
        return new Color(245,230,170);
    }
    String stockState(){ return "profile="+profile+";group="+groupId+";room="+roomId+";radius="+radius+";intensity="+intensity+";color="+colorName+";on="+on+";powered="+powered+";flicker="+flicker+";period="+flickerPeriod+";phase="+phase+";switch="+switchControlled; }
    String encode(){ return esc(id)+"|"+esc(profile)+"|"+x+"|"+y+"|"+roomId+"|"+radius+"|"+intensity+"|"+esc(colorName)+"|"+on+"|"+powered+"|"+flicker+"|"+flickerPeriod+"|"+phase+"|"+switchControlled+"|"+esc(groupId); }
    static ZoneLightSourceRecord parse(String s){ try{ String[] a=s.split("\\|",15); if(a.length<15)return null; return new ZoneLightSourceRecord(unesc(a[0]),unesc(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3]),Integer.parseInt(a[4]),Integer.parseInt(a[5]),Integer.parseInt(a[6]),unesc(a[7]),Boolean.parseBoolean(a[8]),Boolean.parseBoolean(a[9]),Boolean.parseBoolean(a[10]),Integer.parseInt(a[11]),Integer.parseInt(a[12]),Boolean.parseBoolean(a[13]),unesc(a[14])); }catch(Exception e){return null;} }
    static String groupFromStock(String stock){ return MapObjectState.stockValue(stock,"group"); }
    static String esc(String s){ return s==null?"":s.replace("%","%25").replace("|","%7C"); }
    static String unesc(String s){ return s==null?"":s.replace("%7C","|").replace("%25","%"); }
}


class ZoneNoiseSourceRecord {
    String id, label, kind, sourceId;
    int x, y, radius, intensity, phase, period;
    boolean active, intermittent;
    ZoneNoiseSourceRecord(String id, String label, String kind, int x, int y, int radius, int intensity, boolean active, boolean intermittent, int phase, int period, String sourceId){ this.id=id;this.label=label;this.kind=kind;this.x=x;this.y=y;this.radius=radius;this.intensity=intensity;this.active=active;this.intermittent=intermittent;this.phase=phase;this.period=period;this.sourceId=sourceId; }
    String summary(){ return label+"/"+kind+" at "+x+","+y+" r"+radius+" i"+intensity; }
    String encode(){ return esc(id)+"|"+esc(label)+"|"+esc(kind)+"|"+x+"|"+y+"|"+radius+"|"+intensity+"|"+active+"|"+intermittent+"|"+phase+"|"+period+"|"+esc(sourceId); }
    static ZoneNoiseSourceRecord parse(String s){ try{ String[] a=s.split("\\|",12); if(a.length<12)return null; return new ZoneNoiseSourceRecord(unesc(a[0]),unesc(a[1]),unesc(a[2]),Integer.parseInt(a[3]),Integer.parseInt(a[4]),Integer.parseInt(a[5]),Integer.parseInt(a[6]),Boolean.parseBoolean(a[7]),Boolean.parseBoolean(a[8]),Integer.parseInt(a[9]),Integer.parseInt(a[10]),unesc(a[11])); }catch(Exception e){return null;} }
    static String esc(String s){ return s==null?"":s.replace("%","%25").replace("|","%7C"); }
    static String unesc(String s){ return s==null?"":s.replace("%7C","|").replace("%25","%"); }
}


class LightingNoiseMetadataApi {
    private LightingNoiseMetadataApi() {}
    static class ZoneLightingProfile {
        final int roomChance, corridorChance, radiusMin, radiusMax, intensityMin, intensityMax, spacing, switchChance, flickerChance, powerChance;
        final String color, label;
        ZoneLightingProfile(String label, int roomChance, int corridorChance, int radiusMin, int radiusMax, int intensityMin, int intensityMax, int spacing, int switchChance, int flickerChance, int powerChance, String color){ this.label=label;this.roomChance=roomChance;this.corridorChance=corridorChance;this.radiusMin=radiusMin;this.radiusMax=radiusMax;this.intensityMin=intensityMin;this.intensityMax=intensityMax;this.spacing=spacing;this.switchChance=switchChance;this.flickerChance=flickerChance;this.powerChance=powerChance;this.color=color; }
    }
    static class Result { int lights, switches, noises; String profile="unknown"; String summary(){ return "lights="+lights+" switches="+switches+" noises="+noises+" profile="+profile; } }
    static Result seed(World w, Random r){
        Result res = new Result(); if(w==null) return res; if(r==null) r = new Random(w.seed ^ 0x5107E);
        w.lightSources.clear(); w.noiseSources.clear();
        ZoneLightingProfile p = profileFor(w.zoneType, w.sewerLayer); res.profile = p.label;
        ArrayList<Point> placed = new ArrayList<>();
        for(int i=0;i<w.rooms.size();i++){
            Rectangle rr = w.rooms.get(i); if(rr == null) continue;
            int chance = i==0 ? Math.max(p.roomChance, 50) : p.roomChance;
            if(r.nextInt(100) >= chance) continue;
            Point pt = candidateInRoom(w, rr, r, placed, p.spacing);
            if(pt == null) continue;
            ZoneLightSourceRecord l = makeLight(w,p,pt.x,pt.y,i,r,"room");
            addLight(w,l,placed); res.lights++;
            if(r.nextInt(100) < p.switchChance) { Point sw = nearbySwitchPoint(w, rr, pt, r); if(sw != null){ char under=w.tiles[sw.x][sw.y]; w.tiles[sw.x][sw.y]='a'; w.mapObjects.add(MapObjectState.lightSwitch(sw.x,sw.y,l.groupId,w.zoneType,under)); l.switchControlled=true; res.switches++; } }
        }
        int corridorTrials = Math.max(8, (w.w*w.h)/80);
        for(int t=0;t<corridorTrials;t++){
            int x=1+r.nextInt(Math.max(1,w.w-2)), y=1+r.nextInt(Math.max(1,w.h-2));
            if(!w.inBounds(x,y) || w.roomIds[x][y] >= 0) continue;
            char ch = w.tiles[x][y]; if("+=,:;-~_".indexOf(ch) < 0) continue;
            if(ch == RoadGridIntegrationAuthority.ROAD_LANE) continue;
            if(r.nextInt(100) >= p.corridorChance) continue;
            if(!farEnough(x,y,placed,p.spacing)) continue;
            ZoneLightSourceRecord l = makeLight(w,p,x,y,-1,r,"corridor"); addLight(w,l,placed); res.lights++;
        }
        seedNoise(w,r,res);
        w.lightNoiseSummary = res.summary();
        return res;
    }
    static ZoneLightingProfile profileFor(ZoneType z, boolean sewer){
        if(sewer || z==ZoneType.SEWER_CONDUIT || z==ZoneType.MUTANT_SEWER_CAMP || z==ZoneType.CULTIST_SEWER_CAMP) return new ZoneLightingProfile("sump/sewer sparse",18,10,2,4,16,34,7,18,45,58,"sick green");
        if(z==ZoneType.NEUTRAL_CIVILIAN_FLOOR) return new ZoneLightingProfile("neutral civilian maintained",86,74,5,8,58,92,3,72,5,96,"warm white");
        if(z==ZoneType.SECTOR_GOVERNORS_MANSION || z==ZoneType.NOBLE_SERVICE_SPINE) return new ZoneLightingProfile("upper noble maintained",78,62,4,7,48,78,5,72,8,96,"warm amber");
        if(z==ZoneType.ADMINISTRATUM_ARCHIVE || z==ZoneType.IMPERIAL_NEWS_NETWORK || z==ZoneType.ARBITES_PRECINCT_EDGE) return new ZoneLightingProfile("civic sanctioned",62,48,3,6,38,65,5,58,15,90,"white");
        if(z==ZoneType.IMPERIAL_GUARD_BILLET || z==ZoneType.MECHANICUS_FORGE_CLOISTER || z==ZoneType.MECHANICUS_RELIC_DUCT) return new ZoneLightingProfile("industrial duty",54,42,3,6,36,70,5,42,24,84,"blue white");
        if(z==ZoneType.GANGER_TURF || z==ZoneType.TRASH_WARREN || z==ZoneType.MUTANT_WARRENS) return new ZoneLightingProfile("lower arcology scavenged",30,20,2,5,20,44,7,20,42,66,"amber");
        return new ZoneLightingProfile("civil hab functional",48,36,3,5,30,56,6,38,24,78,"white");
    }
    static ZoneLightSourceRecord makeLight(World w, ZoneLightingProfile p, int x, int y, int roomId, Random r, String kind){
        int radius = p.radiusMin + r.nextInt(Math.max(1,p.radiusMax-p.radiusMin+1));
        int intensity = p.intensityMin + r.nextInt(Math.max(1,p.intensityMax-p.intensityMin+1));
        boolean powered = r.nextInt(100) < p.powerChance;
        boolean flicker = r.nextInt(100) < p.flickerChance;
        int period = flicker ? (3+r.nextInt(5)) : 99;
        String group = "L"+Math.abs(Objects.hash(w.seed,x,y,kind,roomId));
        String id = "ZL-"+Math.abs(Objects.hash(w.seed,x,y,roomId,kind,p.label));
        return new ZoneLightSourceRecord(id, kind+" "+p.label, x,y,roomId,radius,intensity,p.color,true,powered,flicker,period,r.nextInt(Math.max(2,period)),false,group);
    }
    static void addLight(World w, ZoneLightSourceRecord l, ArrayList<Point> placed){
        if(w==null || l==null || !w.inBounds(l.x,l.y)) return;
        Point landing = lightFixtureLandingSpot(w, l.x, l.y, placed);
        if(landing == null) return;
        l.x = landing.x; l.y = landing.y;
        char under = w.tiles[l.x][l.y];
        w.lightSources.add(l); placed.add(new Point(l.x,l.y));
        if(under != 'o' && under != 'a' && canPlaceLightFixtureOn(w,l.x,l.y)) { w.tiles[l.x][l.y]='o'; w.mapObjects.add(MapObjectState.lightFixture(l,w.zoneType,under)); }
    }

    static Point lightFixtureLandingSpot(World w, int sx, int sy, ArrayList<Point> placed) {
        if (canPlaceLightFixtureOn(w, sx, sy) && farEnough(sx, sy, placed, 1)) return new Point(sx, sy);
        Point best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int r = 1; r <= 4; r++) {
            for (int x = Math.max(1, sx-r); x <= Math.min(w.w-2, sx+r); x++) {
                for (int y = Math.max(1, sy-r); y <= Math.min(w.h-2, sy+r); y++) {
                    if (!canPlaceLightFixtureOn(w, x, y) || !farEnough(x,y,placed,1)) continue;
                    char ch = w.tiles[x][y];
                    int score = Math.abs(x-sx) + Math.abs(y-sy) + (ch == RoadGridIntegrationAuthority.SIDEWALK ? 0 : 3);
                    if (score < bestScore) { bestScore = score; best = new Point(x,y); }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    static boolean canPlaceLightFixtureOn(World w, int x, int y) {
        if(w==null || !w.inBounds(x,y) || !w.walkable(x,y) || w.mapObjectAt(x,y)!=null) return false;
        char ch = w.tiles[x][y];
        if(ch == RoadGridIntegrationAuthority.ROAD_LANE) return false;
        return ch != 'o' && ch != 'a';
    }
    static Point candidateInRoom(World w, Rectangle rr, Random r, ArrayList<Point> placed, int spacing){
        for(int tries=0; tries<60; tries++){ int x=rr.x+1+r.nextInt(Math.max(1,rr.width-2)); int y=rr.y+1+r.nextInt(Math.max(1,rr.height-2)); if(w.inBounds(x,y) && w.walkable(x,y) && w.tiles[x][y] != RoadGridIntegrationAuthority.ROAD_LANE && farEnough(x,y,placed,spacing)) return new Point(x,y); }
        Point c=w.center(rr); if(w.inBounds(c.x,c.y) && w.walkable(c.x,c.y) && w.tiles[c.x][c.y] != RoadGridIntegrationAuthority.ROAD_LANE && farEnough(c.x,c.y,placed,spacing)) return c; return null;
    }
    static boolean farEnough(int x,int y, ArrayList<Point> placed, int spacing){ for(Point p: placed) if(Math.abs(x-p.x)+Math.abs(y-p.y) < spacing) return false; return true; }
    static Point nearbySwitchPoint(World w, Rectangle rr, Point src, Random r){
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1},{2,0},{-2,0},{0,2},{0,-2}};
        for(int tries=0; tries<dirs.length*2; tries++){ int[] d=dirs[r.nextInt(dirs.length)]; int x=src.x+d[0], y=src.y+d[1]; if(w.inBounds(x,y) && w.walkable(x,y) && w.tiles[x][y] != RoadGridIntegrationAuthority.ROAD_LANE && w.mapObjectAt(x,y)==null) return new Point(x,y); }
        return null;
    }
    static void seedNoise(World w, Random r, Result res){
        for(MapObjectState m: w.mapObjects){ if(m==null) continue; String type=m.type==null?"":m.type; String label=null; int radius=0,intensity=0; boolean intermittent=true;
            if(type.contains("broadcast")){ label="vox/pict-screen chatter"; radius=6; intensity=18; }
            else if(type.contains("bank-alarm")){ label="alarm relay hum"; radius=5; intensity=16; intermittent=false; }
            else if(type.contains("martian-emergency")){ label="machine cycling"; radius=7; intensity=24; }
            else if(type.contains("vending")){ label="coin slot rattle"; radius=3; intensity=9; }
            else if(type.contains("shop")){ label="trade counter voices"; radius=5; intensity=14; }
            else if(type.contains("light-fixture") && r.nextInt(100)<18){ label="ballast buzz"; radius=3; intensity=8; }
            if(label != null){ w.noiseSources.add(new ZoneNoiseSourceRecord("ZN-"+Math.abs(Objects.hash(w.seed,m.id,m.x,m.y)),label,type,m.x,m.y,radius,intensity,true,intermittent,r.nextInt(7),3+r.nextInt(8),m.id)); res.noises++; }
        }
        int machineScans=0;
        for(int x=0;x<w.w;x++) for(int y=0;y<w.h;y++){ char ch=w.tiles[x][y]; if((ch=='N'||ch=='R'||"YJBKOZPFU".indexOf(ch)>=0) && machineScans++<80){ w.noiseSources.add(new ZoneNoiseSourceRecord("ZN-TILE-"+Math.abs(Objects.hash(w.seed,x,y,ch)), ch=='R'?"rogue machine thrum":"machinery drone", "tile-"+ch, x,y, ch=='R'?8:6, ch=='R'?28:20, true, true, r.nextInt(7),4+r.nextInt(6),"tile")); res.noises++; } }
        if(w.hazardWarnings != null) for(EnvironmentalHazardRecord hz : w.hazardWarnings){
            if(hz == null || !w.inBounds(hz.x,hz.y)) continue;
            String fam = hz.family==null?"":hz.family.toLowerCase(Locale.ROOT);
            String label = fam.contains("wire") || fam.contains("electric") ? "crackling hazard" : (fam.contains("sludge") || fam.contains("runoff") || fam.contains("caustic") ? "surging sludge" : (fam.contains("heat") || fam.contains("thermal") || fam.contains("cold") ? "thermal machinery wash" : "coiling gas hiss"));
            int radius = 3 + Math.max(0, hz.severity / 18);
            int intensity = 8 + Math.max(0, hz.severity / 3);
            w.noiseSources.add(new ZoneNoiseSourceRecord("ZN-HZ-"+Math.abs(Objects.hash(w.seed,hz.id,hz.x,hz.y)), label, "hazard-"+fam, hz.x,hz.y, Math.min(9,radius), Math.min(42,intensity), true, true, hz.phase, 3+(Math.abs(hz.severity)%5), hz.id));
            res.noises++;
        }
        if(w.trapRecords != null) for(TrapRecord t : w.trapRecords){
            if(t == null || !w.inBounds(t.x,t.y) || "disarmed".equals(t.state)) continue;
            if("hidden".equals(t.state) && !"wire".equals(t.type)) continue;
            String label = "wire".equals(t.type) ? "taut wire tick" : "armed trap mechanism";
            int radius = Math.max(2, Math.min(8, t.noiseRadius));
            int intensity = "triggered".equals(t.state) ? Math.max(24, t.severity*10) : Math.max(5, t.severity*4);
            w.noiseSources.add(new ZoneNoiseSourceRecord("ZN-TR-"+Math.abs(Objects.hash(w.seed,t.id,t.x,t.y)), label, "trap-"+t.type, t.x,t.y, radius, Math.min(36,intensity), true, true, t.severity, 4+Math.max(1,t.severity%4), t.id));
            res.noises++;
        }
        w.noiseFieldTurn = -1;
        w.hearingFieldSummary = "Noise/hearing field dirty after source seed; awaiting first sensory rebuild.";
    }
}


class NoiseHearingFieldApi {
    private NoiseHearingFieldApi() {}
    static class Result {
        int sources, hazardSources, trapSources, tileSources, peak, average, muffledTiles;
        boolean changedSummary;
        String summary(){ return "cachedNoiseField sources="+sources+" hazards="+hazardSources+" traps="+trapSources+" tileDrones="+tileSources+" peak="+peak+" avg="+average+" muffledTiles="+muffledTiles; }
    }
    static Result rebuild(World w, int turn){
        Result res = new Result();
        if(w == null) return res;
        if(w.noiseField == null || w.noiseField.length != w.w || w.noiseField[0].length != w.h) w.noiseField = new int[w.w][w.h];
        for(int x=0;x<w.w;x++) Arrays.fill(w.noiseField[x], 0);
        if(w.noiseSources != null){
            for(ZoneNoiseSourceRecord ns : w.noiseSources){
                if(ns == null || !ns.active || !w.inBounds(ns.x,ns.y)) continue;
                if(ns.intermittent && Math.floorMod(turn + ns.phase, Math.max(2, ns.period)) == 0) continue;
                res.sources++;
                String kind = ns.kind==null?"":ns.kind.toLowerCase(Locale.ROOT);
                if(kind.startsWith("hazard")) res.hazardSources++;
                else if(kind.startsWith("trap")) res.trapSources++;
                else if(kind.startsWith("tile")) res.tileSources++;
                applySource(w, ns.x, ns.y, Math.max(1, ns.radius), Math.max(1, ns.intensity), res);
            }
        }
        int total=0, count=0, peak=0;
        for(int x=0;x<w.w;x++) for(int y=0;y<w.h;y++){
            int v = Math.max(0, Math.min(70, w.noiseField[x][y]));
            if(mufflesNoise(w.tiles[x][y])) { v = Math.max(0, v - 8); res.muffledTiles++; }
            w.noiseField[x][y] = v; total += v; count++; if(v > peak) peak = v;
        }
        res.peak = peak; res.average = count<=0?0:total/count; w.noiseFieldTurn = turn;
        String old = w.hearingFieldSummary;
        w.hearingFieldSummary = res.summary();
        res.changedSummary = old == null || !old.equals(w.hearingFieldSummary);
        return res;
    }
    static void applySource(World w, int sx, int sy, int radius, int intensity, Result res){
        for(int x=Math.max(0,sx-radius); x<=Math.min(w.w-1,sx+radius); x++) for(int y=Math.max(0,sy-radius); y<=Math.min(w.h-1,sy+radius); y++){
            int d = Math.abs(x-sx) + Math.abs(y-sy); if(d > radius) continue;
            int v = intensity - d * 3 - lineMufflePenalty(w, sx, sy, x, y); if(v <= 0) continue;
            w.noiseField[x][y] = Math.min(70, w.noiseField[x][y] + Math.max(1, v));
        }
    }
    static int lineMufflePenalty(World w, int x0, int y0, int x1, int y1){
        int penalty = 0;
        int dx = Math.abs(x1-x0), dy = Math.abs(y1-y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx-dy;
        int x=x0,y=y0;
        while(true){
            if(!(x==x0 && y==y0) && mufflesNoise(w.tiles[x][y])) penalty += 7;
            if(x==x1 && y==y1) break;
            int e2 = 2*err; if(e2 > -dy){ err -= dy; x += sx; } if(e2 < dx){ err += dx; y += sy; }
            if(!w.inBounds(x,y)) break;
        }
        return Math.min(30, penalty);
    }
    static boolean mufflesNoise(char ch){ return ch=='#' || ch=='D' || ch=='H' || ch=='W' || ch=='X' || InterstitialInfrastructureApi.isInterstitialSolid(ch); }
}




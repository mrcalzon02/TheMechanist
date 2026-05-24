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

class MapObjectState {
    String id, type, label, stockState; int x, y, cooldownUntilTurn, vendCount; char glyph;
    static MapObjectState vending(int x, int y, char glyph, ZoneType z){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph=glyph;m.type="vending";m.label="Vending machine "+glyph+" / "+z.label;m.stockState="seeded-stock";m.cooldownUntilTurn=0;m.vendCount=0;m.id="VM-"+Math.abs(Objects.hash(x,y,glyph,z.label)); return m; }
    static MapObjectState shrine(int x, int y, char glyph, ZoneType z){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph=glyph;m.type=(glyph=='H'?"heretical-shrine":"imperial-shrine");m.label=(glyph=='H'?"Heretical shrine":"Imperial shrine")+" / "+z.label;m.stockState="passive";m.id="SH-"+Math.abs(Objects.hash(x,y,glyph,z.label)); return m; }
    static MapObjectState shop(int x, int y, ZoneType z){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='T';m.type="shop";m.label="Trader stock point / "+z.label;m.stockState="zone-appropriate-inventory";m.id="SHOP-"+Math.abs(Objects.hash(x,y,z.label)); return m; }
    static MapObjectState governor(int x, int y, ZoneType z, long seed){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='Q';m.type="sector-governor";m.label="Sector Governor audience dais / "+z.label;m.stockState=(Math.floorMod(seed ^ (x*31L) ^ (y*17L),100)<35?"seed-ticket-present":"seed-ticket-absent");m.id="GOV-"+Math.abs(Objects.hash(x,y,z.label,seed)); return m; }
    static MapObjectState emergencyMachine(int x, int y, char glyph, ZoneType z){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph=glyph;m.type="martian-emergency-machine";m.label="EMM "+glyph+" / "+z.label;m.stockState="locked-state-seeded; powered-state-persistent";m.id="EMM-"+Math.abs(Objects.hash(x,y,glyph,z.label)); return m; }
    static MapObjectState contractObject(int x, int y, String id, String item, Faction f, ZoneType z){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='o';m.type="contract-object";m.label="Sealed contract object / "+(f==null?"Unknown":f.label)+" / "+z.label;m.stockState=item;m.cooldownUntilTurn=0;m.vendCount=0;m.id=id; return m; }
    static MapObjectState corpseObject(int x, int y, String id, String label, String containerId, Faction f, ZoneType z){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='r';m.type="corpse-container";m.label=(label==null||label.isBlank()?"Corpse":label)+" / "+(f==null?"Unknown":f.label)+" / "+z.label;m.stockState=containerId==null?"":containerId;m.cooldownUntilTurn=0;m.vendCount=0;m.id=id; return m; }
    static MapObjectState thrownExplosive(int x, int y, String item, ZoneType z, int dueTurn, char underlying){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='*';m.type="thrown-explosive";m.label="Thrown " + item + " / " + (z==null?"Unknown zone":z.label);m.stockState=(item==null?"Frag grenade":item)+";under="+(int)underlying;m.cooldownUntilTurn=dueTurn;m.vendCount=0;m.id="THROWN-EXP-"+Math.abs(Objects.hash(x,y,item,dueTurn)); return m; }
    static MapObjectState plantedExplosive(int x, int y, String item, ZoneType z, int armedTurn, char underlying, String owner){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='!';m.type="planted-explosive";m.label="Planted " + item + " / " + (owner==null?"unknown":owner) + " / " + (z==null?"Unknown zone":z.label);m.stockState=(item==null?"Tripwire mine":item)+";under="+(int)underlying;m.cooldownUntilTurn=armedTurn;m.vendCount=1;m.id="PLANTED-EXP-"+Math.abs(Objects.hash(x,y,item,armedTurn,owner)); return m; }
    static MapObjectState newsVending(int x, int y, ZoneType z, char underlying){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='1';m.type=AssetIntegrationDisciplineAuthority.INN_NEWSPAPER_DISPENSER;m.label="Imperial News Network newspaper dispenser / "+(z==null?"Unknown zone":z.label);m.stockState="daily-paper;under="+(int)underlying;m.cooldownUntilTurn=0;m.vendCount=0;m.id="INN-VEND-"+Math.abs(Objects.hash(x,y,z==null?"zone":z.label)); return m; }
    static MapObjectState oldNewspaper(int x, int y, int issueDay, ZoneType z, char underlying){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='o';m.type=AssetIntegrationDisciplineAuthority.DISCARDED_NEWSPRINT_SOURCE;m.label="Discarded INN newspaper / "+(z==null?"Unknown zone":z.label);m.stockState=Math.max(0,issueDay)+";under="+(int)underlying;m.cooldownUntilTurn=0;m.vendCount=0;m.id="INN-OLD-PAPER-"+Math.abs(Objects.hash(x,y,issueDay,z==null?"zone":z.label)); return m; }
    static MapObjectState broadcastDevice(int x, int y, String kind, ZoneType z, char underlying){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='N';m.type="broadcast-device";String k=(kind==null||kind.isBlank()?"radio":kind);m.label="INN "+k+" receiver / "+(z==null?"Unknown zone":z.label);m.stockState=k+";under="+(int)underlying;m.cooldownUntilTurn=0;m.vendCount=0;m.id="INN-BCAST-"+Math.abs(Objects.hash(x,y,k,z==null?"zone":z.label)); return m; }
    static MapObjectState bankTerminal(int x, int y, BankProfile b, boolean branch, ZoneType z, char underlying){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='$';m.type="bank-terminal";m.label=(branch?"Bank branch office":"Credit terminal kiosk")+" / "+(b==null?"Unknown Bank":b.label)+" / "+(z==null?"Unknown zone":z.label);m.stockState=(b==null?"sump-ledger-mutual":b.id)+";kind="+(branch?"branch":"atm")+";under="+(int)underlying;m.cooldownUntilTurn=0;m.vendCount=0;m.id="BANK-"+Math.abs(Objects.hash(x,y,b==null?"bank":b.id,branch,z==null?"zone":z.label)); return m; }
    static MapObjectState bankVault(int x, int y, BankProfile b, ZoneType z, char underlying){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='X';m.type="bank-vault";m.label="Bank vault / "+(b==null?"Unknown Bank":b.label)+" / "+(z==null?"Unknown zone":z.label);m.stockState=(b==null?"sump-ledger-mutual":b.id)+";locked=true;looted=false;open=false;under="+(int)underlying;m.cooldownUntilTurn=0;m.vendCount=0;m.id="BANK-VAULT-"+Math.abs(Objects.hash(x,y,b==null?"bank":b.id,z==null?"zone":z.label)); return m; }
    static MapObjectState bankAlarmPanel(int x, int y, BankProfile b, ZoneType z, char underlying){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='a';m.type="bank-alarm-panel";m.label="Bank alarm panel / "+(b==null?"Unknown Bank":b.label)+" / "+(z==null?"Unknown zone":z.label);m.stockState=(b==null?"sump-ledger-mutual":b.id)+";armed=true;disabled=false;alarmed=false;under="+(int)underlying;m.cooldownUntilTurn=0;m.vendCount=0;m.id="BANK-ALARM-"+Math.abs(Objects.hash(x,y,b==null?"bank":b.id,z==null?"zone":z.label)); return m; }
    static MapObjectState lightFixture(ZoneLightSourceRecord l, ZoneType z, char underlying){ MapObjectState m=new MapObjectState(); m.x=l==null?0:l.x;m.y=l==null?0:l.y;m.glyph='o';m.type="light-fixture";m.label="Light fixture / "+(l==null?"unknown":l.profile)+" / "+(z==null?"Unknown zone":z.label);m.stockState=(l==null?"":l.stockState())+";under="+(int)underlying;m.cooldownUntilTurn=0;m.vendCount=0;m.id=l==null?"LIGHT-UNKNOWN":l.id; return m; }
    static MapObjectState lightSwitch(int x, int y, String group, ZoneType z, char underlying){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='a';m.type="light-switch";m.label="Local light switch / "+(z==null?"Unknown zone":z.label);m.stockState="group="+(group==null?"":group)+";on=true;under="+(int)underlying;m.cooldownUntilTurn=0;m.vendCount=0;m.id="LIGHT-SWITCH-"+Math.abs(Objects.hash(x,y,group,z==null?"zone":z.label)); return m; }
    static String stockValue(String stock, String key){ if(stock==null||key==null)return""; for(String part: stock.split(";")){ int i=part.indexOf('='); if(i>0 && part.substring(0,i).equals(key)) return part.substring(i+1); } return ""; }
    static String setStockFlag(String stock, String key, String value){ LinkedHashMap<String,String> m=new LinkedHashMap<>(); if(stock!=null&&!stock.isBlank()) for(String part: stock.split(";")){ if(part.isBlank()) continue; int i=part.indexOf('='); if(i>0)m.put(part.substring(0,i),part.substring(i+1)); else m.put(part,""); } m.put(key,value==null?"":value); ArrayList<String> out=new ArrayList<>(); for(Map.Entry<String,String> e:m.entrySet()) out.add(e.getValue().isEmpty()?e.getKey():e.getKey()+"="+e.getValue()); return String.join(";",out); }
    static char underlyingTileFromStock(String stock){ if(stock==null) return 0; int i=stock.indexOf(";under="); if(i<0) i=stock.indexOf("under="); if(i<0) return 0; int start=i+(stock.startsWith(";under=", i)?7:6); int end=start; while(end<stock.length() && Character.isDigit(stock.charAt(end))) end++; try{return (char)Integer.parseInt(stock.substring(start,end));}catch(Exception e){return 0;} }
    static String itemNameFromStock(String stock){ if(stock==null||stock.isBlank()) return ""; int i=stock.indexOf(';'); return i<0?stock:stock.substring(0,i); }
    static MapObjectState factionJournal(int x, int y, FactionStrategicPlan plan, ZoneType z){ MapObjectState m=new MapObjectState(); m.x=x;m.y=y;m.glyph='o';m.type="faction-journal";m.label=(plan==null?"Faction journal":plan.faction.label+" leader journal")+" / "+(z==null?"Unknown zone":z.label);m.stockState=plan==null?"":plan.id;m.cooldownUntilTurn=0;m.vendCount=0;m.id="JOURNAL-"+Math.abs(Objects.hash(x,y,m.stockState,z==null?"zone":z.label)); return m; }
    String summary(){ return id+" "+type+" "+label+" at "+x+","+y+" cooldown="+cooldownUntilTurn+" vendCount="+vendCount+" stock="+stockState; }
}


class TradeOffer {
    String name, category, description; int basePrice;
    String itemInstanceId = "";
    ItemProvenanceRecord provenance = null;
    TradeOffer(String name, String category, int basePrice, String description){ this(name, category, basePrice, description, null); }
    TradeOffer(String name, String category, int basePrice, String description, ItemProvenanceRecord provenance){ this.name=name; this.category=category; this.basePrice=basePrice; this.description=description; this.provenance=provenance; }
    String baseName() { return ItemQuality.stripQuality(name); }
    ItemDef catalogDef() { return ItemCatalog.get(name); }
    String catalogDescription() { ItemDef d = catalogDef(); return d == null ? description : d.description; }
    String catalogUse() { ItemDef d = catalogDef(); return d == null ? "uncataloged trade good" : d.use; }
    int catalogBasePrice() { ItemDef d = catalogDef(); return d == null ? Math.max(1, basePrice) : d.basePrice; }
    String displayLine(int price) {
        ItemDef d = catalogDef();
        String cat = d == null ? category : d.category;
        String src = d == null ? "local trader stock" : d.source;
        String trace = provenance == null ? "" : " | Trace: " + provenance.shortChain();
        return name + " — " + price + " script — " + cat + " — " + catalogDescription() + " | Use: " + catalogUse() + " | Source: " + src + trace;
    }
}




class RoomContainerApi {
    private RoomContainerApi() {}

    static String containerId(int locationKey, int roomId) {
        return "room.cache." + locationKey + "." + Math.max(0, roomId);
    }

    static String containerLabel(int roomId, RoomProfile rp, ZoneType zt) {
        String rn = rp == null || rp.name == null ? "Unknown room" : rp.name;
        String zn = zt == null ? "Unknown zone" : zt.label;
        return "Room cache " + roomId + " / " + rn + " / " + zn;
    }

    static ArrayList<String> seedCacheItems(RoomProfile rp, ZoneType zt, Random rng) {
        if (rng == null) rng = new Random(0);
        ArrayList<String> out = new ArrayList<>();
        String text = ((rp == null || rp.name == null) ? "" : rp.name) + " " + ((rp == null || rp.descriptor == null) ? "" : rp.descriptor);
        String low = text.toLowerCase(Locale.ROOT);
        if (HivewallRoomCacheApi.isHivewallRoom(rp)) return HivewallRoomCacheApi.seedCacheItems(rp, zt, rng);
        int count = 3 + rng.nextInt(4);
        if (has(low, "warehouse", "storehouse", "armory", "munition", "component")) count += 2;
        if (has(low, "library", "archive", "learning", "data-chapel")) count += 1;
        if (has(low, "cafeteria", "kitchen", "mess", "galley", "pantry")) count += 1;
        String[] targets = new String[]{"General supplies", "Food", "Water", "Machine parts"};
        for (int i=0; i<count; i++) {
            String target = targets[Math.floorMod(i + rng.nextInt(targets.length), targets.length)];
            String picked = RoomLootApi.pickScavengeLoot(target, rp, zt, rng, null);
            if (picked != null && !picked.isBlank() && ItemCatalog.get(picked) != null) out.add(picked);
        }
        LootDropSystemAuthority.ZoneContainerInjection extra = LootDropSystemAuthority.maybeInjectZoneContainerBonus(zt, zoneTierForCache(zt), text);
        if (extra.present()) out.add(extra.qualifiedItemName());
        if (out.isEmpty()) out.add("Vended scrap");
        return out;
    }

    private static double zoneTierForCache(ZoneType zt) {
        if (zt == null) return 0.35d;
        double richness = Math.max(-3, Math.min(3, zt.richness)) / 6.0d + 0.5d;
        double floorBias = switch (zt) {
            case SECTOR_GOVERNORS_MANSION, NOBLE_SERVICE_SPINE -> 0.92d;
            case IMPERIAL_GUARD_BILLET, ARBITES_PRECINCT_EDGE, ADMINISTRATUM_ARCHIVE, IMPERIAL_NEWS_NETWORK -> 0.70d;
            case MECHANICUS_FORGE_CLOISTER, MECHANICUS_RELIC_DUCT, TRAIN_SERVICE_YARD, NEUTRAL_RAIL_DEPOT -> 0.62d;
            case TRASH_WARREN, MUTANT_WARRENS, MUTANT_SEWER_CAMP, CULTIST_SEWER_CAMP, SEWER_CONDUIT -> 0.15d;
            case GANGER_TURF, SUMP_MARKET, HAB_STACK -> 0.30d;
            default -> 0.45d;
        };
        return Math.max(0.0d, Math.min(1.0d, floorBias * 0.75d + richness * 0.25d));
    }

    static ArrayList<String> cacheLedgerLines(String id, ContainerRecord c, LinkedHashMap<String,ItemInstance> registry, int limit) {
        ArrayList<String> lines = new ArrayList<>();
        String safeId = id == null || id.isBlank() ? "unknown room cache" : id;
        int total = c == null ? 0 : c.itemInstanceIds.size();
        lines.add(safeId + " contains " + total + " item instance" + (total == 1 ? "" : "s"));
        if (c == null || registry == null || c.itemInstanceIds.isEmpty()) {
            lines.add("empty cache");
            return lines;
        }
        int max = Math.max(1, Math.min(limit, c.itemInstanceIds.size()));
        for (int i=0; i<max; i++) {
            ItemInstance inst = registry.get(c.itemInstanceIds.get(i));
            if (inst == null) {
                lines.add("[missing instance " + c.itemInstanceIds.get(i) + "]");
                continue;
            }
            ItemDef d = ItemCatalog.get(inst.displayName);
            String cat = d == null || d.category == null ? "uncataloged" : d.category;
            String trace = inst.provenance == null ? "untraced" : inst.provenance.summary();
            lines.add(inst.displayName + " [" + cat + "] " + trace);
        }
        if (c.itemInstanceIds.size() > max) lines.add("... " + (c.itemInstanceIds.size() - max) + " more cached item(s)");
        return lines;
    }

    static String chooseInstanceForTarget(String target, ContainerRecord c, LinkedHashMap<String,ItemInstance> registry) {
        if (c == null || registry == null || c.itemInstanceIds.isEmpty()) return "";
        for (String id : c.itemInstanceIds) {
            ItemInstance inst = registry.get(id);
            if (inst != null && matchesTarget(target, inst.displayName)) return id;
        }
        return c.itemInstanceIds.get(0);
    }

    static boolean matchesTarget(String target, String item) {
        if (item == null) return false;
        String t = target == null ? "" : target.toLowerCase(Locale.ROOT);
        String i = item.toLowerCase(Locale.ROOT);
        ItemDef d = ItemCatalog.get(item);
        String cat = d == null || d.category == null ? "" : d.category.toLowerCase(Locale.ROOT);
        String use = d == null || d.use == null ? "" : d.use.toLowerCase(Locale.ROOT);
        String all = i + " " + cat + " " + use;
        if (t.contains("food")) return has(all, "food", "ration", "meal", "fungus", "nutrient", "wafer", "snack");
        if (t.contains("water")) return has(all, "water", "canteen", "filter", "purification");
        if (t.contains("machine") || t.contains("part")) return has(all, "machine", "part", "wire", "tool", "probe", "servo", "gasket", "bolt", "scrap");
        return true;
    }

    private static boolean has(String s, String... needles) {
        if (s == null) return false;
        for (String n : needles) if (n != null && !n.isBlank() && s.contains(n)) return true;
        return false;
    }
}



class FacilityLinkedRoomCacheApi {
    private FacilityLinkedRoomCacheApi() {}

    static ZoneFacilityLedgerEntry matchFacilityForRoom(World w, RoomProfile rp, ZoneType zt) {
        if (w == null) return null;
        java.util.List<ZoneFacilityLedgerEntry> entries = ZoneFacilityHistoryApi.parseFacilityLedger(w.zoneFacilityHistory);
        if (entries == null || entries.isEmpty()) return null;
        String hay = (((rp == null || rp.name == null) ? "" : rp.name) + " " +
                ((rp == null || rp.descriptor == null) ? "" : rp.descriptor) + " " +
                ((rp == null || rp.featureText == null) ? "" : rp.featureText) + " " +
                (zt == null ? "" : zt.label)).toLowerCase(Locale.ROOT);
        ZoneFacilityLedgerEntry best = null;
        int bestScore = Integer.MIN_VALUE;
        for (ZoneFacilityLedgerEntry e : entries) {
            int score = 0;
            score += ZoneFacilityHistoryApi.wordOverlapScore(hay, e.roomType);
            score += ZoneFacilityHistoryApi.wordOverlapScore(hay, e.purpose);
            score += ZoneFacilityHistoryApi.wordOverlapScore(hay, e.productFocus);
            String all = safe(e.roomType) + " " + safe(e.purpose) + " " + safe(e.productFocus) + " " + safe(e.populationSource);
            String low = all.toLowerCase(Locale.ROOT);
            if (has(hay,"cafeteria","kitchen","mess","galley","canteen","pantry") && has(low,"food","ration","mess","galley","kitchen","canteen","pantry","nutrient")) score += 12;
            if (has(hay,"warehouse","storehouse","stores","armory","munition","component","cargo","evidence") && has(low,"warehouse","store","storage","goods","munition","cargo","evidence","component")) score += 11;
            if (has(hay,"clinic","aid","medicae","medical","diagnostic") && has(low,"clinic","aid","diagnostic","medical","maintenance")) score += 9;
            if (has(hay,"library","learning","archive","data-chapel","chapel","training","drill") && has(low,"learning","archive","chapel","training","drill","doctrine")) score += 9;
            if (has(hay,"barracks","dormitory","apartment","rest cell","household") && has(low,"housing","habitation","barracks","dormitory","household","rest")) score += 8;
            if (has(hay,"storefront","counter","salon","barter","ticket","complaint") && has(low,"commerce","public","counter","salon","barter","ticket","complaint")) score += 7;
            if (score > bestScore) { bestScore = score; best = e; }
        }
        return bestScore <= 0 ? entries.get(0) : best;
    }

    static ArrayList<String> seedCacheItems(RoomProfile rp, ZoneType zt, Random rng, ZoneFacilityLedgerEntry facility, World w) {
        if (rng == null) rng = new Random(0);
        ArrayList<String> out = RoomContainerApi.seedCacheItems(rp, zt, rng);
        String text = (((rp == null || rp.name == null) ? "" : rp.name) + " " +
                ((rp == null || rp.descriptor == null) ? "" : rp.descriptor) + " " +
                (facility == null ? "" : facility.summary())).toLowerCase(Locale.ROOT);
        ArrayList<String> extras = new ArrayList<>();
        if (has(text,"guard","munition","barracks","drill","field mess")) add(extras,"Guard field ration tin","Guard drill manual","Guard flak vest","Guard entrenching tool","Guard lascarbine");
        if (has(text,"mechanicus","forge","diagnostic","galley","chapel","cable")) add(extras,"Mechanicus calibration probe","Mechanicus nutrient ampoule","Mechanicus catechism strip","Sacred wire bundle","Machine oil vial","Mechanicus tool roll");
        if (has(text,"arbites","evidence","holding","complaint","precinct")) add(extras,"Arbites restraint kit","Arbites casebook excerpt","Arbites riot visor","Arbites shock maul","Arbites suppression shells");
        if (has(text,"noble","house","salon","dining","pantry","luxury")) add(extras,"Noble preserved delicacy","Noble etiquette card","Noble signet wax kit","Noble fur-lined coat","Noble dueling pistol");
        if (has(text,"rail","cargo","freight","depot","platform")) add(extras,"Rail cargo stencil kit","Warehouse inventory tag bundle","Rail worker hazard coat","Rail spike hammer","Cargo hook");
        if (has(text,"market","barter","storefront","pawn","sump")) add(extras,"Trade chit","Market scale set","Water bottle","Market vendor sash","Warehouse inventory tag bundle");
        if (has(text,"creche","daycare","learning","library","archive","instruction")) add(extras,"Primer slate","Creche lesson toy","Blank form packet","Warehouse inventory tag bundle");
        if (has(text,"clinic","aid","medicae","medical")) add(extras,"Bandage roll","Field dressings","Antiseptic vial","Splint kit","Medkit");
        if (has(text,"food","ration","kitchen","cafeteria","mess","galley","pantry","canteen")) add(extras,"Emergency rations","Plain ration pack","Water ration","Sealed water ration","Kitchen grease tin");
        if (has(text,"warehouse","storehouse","storage","goods","component")) add(extras,"Construction supplies","Machine part","Tool bundle","Wire bundle","Warehouse inventory tag bundle");
        int extraCount = Math.min(3, extras.size());
        for (int i=0; i<extraCount; i++) {
            String picked = extras.get(Math.floorMod(rng.nextInt() + i, extras.size()));
            if (picked != null && ItemCatalog.get(picked) != null && !out.contains(picked)) out.add(picked);
        }
        HistoricalConflictLossApi.addConflictSamplesToCache(out, w, facility, rp, rng);
        LootDropSystemAuthority.ZoneContainerInjection extra = LootDropSystemAuthority.maybeInjectZoneContainerBonus(zt, zoneTierForCache(zt), text);
        if (extra.present()) out.add(extra.qualifiedItemName());
        if (out.isEmpty()) out.add("Vended scrap");
        return out;
    }

    private static double zoneTierForCache(ZoneType zt) {
        if (zt == null) return 0.35d;
        double richness = Math.max(-3, Math.min(3, zt.richness)) / 6.0d + 0.5d;
        double floorBias = switch (zt) {
            case SECTOR_GOVERNORS_MANSION, NOBLE_SERVICE_SPINE -> 0.92d;
            case IMPERIAL_GUARD_BILLET, ARBITES_PRECINCT_EDGE, ADMINISTRATUM_ARCHIVE, IMPERIAL_NEWS_NETWORK -> 0.70d;
            case MECHANICUS_FORGE_CLOISTER, MECHANICUS_RELIC_DUCT, TRAIN_SERVICE_YARD, NEUTRAL_RAIL_DEPOT -> 0.62d;
            case TRASH_WARREN, MUTANT_WARRENS, MUTANT_SEWER_CAMP, CULTIST_SEWER_CAMP, SEWER_CONDUIT -> 0.15d;
            case GANGER_TURF, SUMP_MARKET, HAB_STACK -> 0.30d;
            default -> 0.45d;
        };
        return Math.max(0.0d, Math.min(1.0d, floorBias * 0.75d + richness * 0.25d));
    }

    static ItemProvenanceRecord cacheProvenance(String item, World w, int roomId, RoomProfile rp, ZoneType zt, ZoneFacilityLedgerEntry facility, int turn) {
        Faction f = Faction.NONE;
        if (w != null && roomId >= 0 && roomId < w.roomFactions.size()) f = w.roomFactions.get(roomId);
        String roomName = rp == null || rp.name == null ? "unknown room" : rp.name;
        String facilityText = facility == null ? "no matched facility record" : facility.summary();
        String history = compact(w == null ? null : w.zoneEpochHistory);
        String zoneFacility = compact(w == null ? null : w.zoneFacilityHistory);
        String maker = (f == null || f == Faction.NONE ? "local facility cache" : f.label + " facility cache") + " / " + roomName;
        String production = compact(w == null ? null : w.zoneProductionHistory);
        String movement = compact(w == null ? null : w.zoneStockMovementHistory);
        String matchedMovement = ProductionDistributionApi.movementSummary(w, facility, rp);
        String conflict = compact(w == null ? null : w.zoneConflictLossHistory);
        String matchedConflict = HistoricalConflictLossApi.conflictSummary(w, facility, rp);
        String materialized = compact(w == null ? null : w.zoneMaterializedItemHistory);
        String matchedMaterialized = HistoricalItemMaterializationApi.materializationSummary(w, facility, rp, item);
        String labor = compact(w == null ? null : w.zoneLaborAssignmentHistory);
        String matchedLabor = PopulationWorkAssignmentApi.laborSummary(w, facility);
        String inputs = "facility=" + facilityText + "; faction epochs=" + history + "; zone facility ledger=" + zoneFacility + "; production output ledger=" + production + "; stock movement ledger=" + movement + "; matched movement=" + matchedMovement + "; conflict/loss ledger=" + conflict + "; matched conflict=" + matchedConflict + "; historical item materialization ledger=" + materialized + "; matched materialized item=" + matchedMaterialized + "; labor assignment ledger=" + labor + "; matched labor=" + matchedLabor;
        String route = "stored by ordinary room cache under facility-history authority -> recoverable by scavenging";
        ItemProvenanceRecord pr = ItemProvenanceRecord.of(item, f, maker, w, turn, inputs, route);
        pr.place = (w == null ? "unknown hive" : w.hiveName + " / " + w.sectorName + " / " + w.zoneName + " / " + (zt == null ? w.zoneType.label : zt.label)) + " / room #" + Math.max(0, roomId);
        String facilityId = facility == null || facility.id == null || facility.id.isBlank() ? "facility.unmatched" : facility.id;
        String outputRef = ProductionFacilityOutputSimulationApi.outputChainFor(w, facility);
        String movementRef = ProductionDistributionApi.movementChainFor(w, facility, rp);
        String conflictRef = HistoricalConflictLossApi.conflictChainFor(w, facility, rp);
        String materializedRef = HistoricalItemMaterializationApi.materializationChainFor(w, facility, rp, item);
        String laborRef = PopulationWorkAssignmentApi.laborChainFor(w, facility);
        pr.chain = maker + " -> " + facilityId + laborRef + outputRef + movementRef + conflictRef + materializedRef + " -> persistent room cache " + RoomContainerApi.containerId(w == null ? 0 : w.locationKey(), roomId);
        return pr;
    }

    static void add(ArrayList<String> out, String... vals) {
        for (String v : vals) if (v != null && ItemCatalog.get(v) != null) out.add(v);
    }
    static boolean has(String s, String... vals) {
        if (s == null) return false;
        for (String v : vals) if (v != null && !v.isBlank() && s.contains(v.toLowerCase(Locale.ROOT))) return true;
        return false;
    }
    static String safe(String s){ return s == null ? "" : s; }
    static String compact(String s){
        if (s == null || s.isBlank()) return "unsynthesized";
        String t = s.replace(";;", " | ").trim();
        return t.length() > 160 ? t.substring(0,157) + "..." : t;
    }
}


class RoomLootApi {
    interface DataSpikeChance { double chance(RoomProfile rp, ZoneType zt); }
    private RoomLootApi() {}

    static String pickScavengeLoot(String target, RoomProfile rp, ZoneType zt, Random rng, DataSpikeChance dataSpikeChance) {
        if (rng == null) rng = new Random(0);
        if (dataSpikeChance != null && rng.nextDouble() < dataSpikeChance.chance(rp, zt)) return "Data spike";
        if ((zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION || zt == ZoneType.ARBITES_PRECINCT_EDGE || zt == ZoneType.ADMINISTRATUM_ARCHIVE) && rng.nextDouble() < 0.006) return "Secure vault key";
        ArrayList<String> pool = poolFor(target, rp, zt);
        if (!pool.isEmpty()) return pool.get(rng.nextInt(pool.size()));
        String fallback = rp == null ? "Vended scrap" : normalizeLegacyLoot(rp.randomLoot(rng), rp, zt, rng);
        return catalogOrFallback(fallback, "Vended scrap");
    }

    private static ArrayList<String> poolFor(String target, RoomProfile rp, ZoneType zt) {
        ArrayList<String> out = new ArrayList<>();
        String t = target == null ? "" : target.toLowerCase(Locale.ROOT);
        String rn = rp == null || rp.name == null ? "" : rp.name.toLowerCase(Locale.ROOT);
        String rd = rp == null || rp.descriptor == null ? "" : rp.descriptor.toLowerCase(Locale.ROOT);
        String text = rn + " " + rd;

        if (t.contains("food")) {
            addFood(out, zt, text);
        } else if (t.contains("water")) {
            addWater(out, zt, text);
        } else if (t.contains("machine") || t.contains("part")) {
            addParts(out, zt, text);
        } else {
            addByRoomPurpose(out, zt, text);
            if (rp != null && rp.loot != null) for (String legacy : rp.loot) out.add(normalizeLegacyLoot(legacy, rp, zt, new Random(legacy == null ? 0 : legacy.hashCode())));
        }
        out.removeIf(x -> x == null || x.isBlank() || ItemCatalog.get(x) == null);
        if (out.isEmpty()) {
            if (t.contains("food")) { out.add("Emergency rations"); out.add("Cheap lunch tin"); }
            else if (t.contains("water")) { out.add("Water bottle"); out.add("Sealed water ration"); }
            else if (t.contains("machine") || t.contains("part")) { out.add("Machine part"); out.add("Wire bundle"); }
        }
        return out;
    }

    private static void addFood(ArrayList<String> out, ZoneType zt, String text) {
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) { add(out,"Guard field ration tin","Plain ration pack","Sealed cafeteria cutlery"); return; }
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) { add(out,"Noble preserved delicacy","Cheap lunch tin","Sealed cafeteria cutlery"); return; }
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) { add(out,"Mechanicus nutrient ampoule","Water purification tab","Sealed cafeteria cutlery"); return; }
        if (zt == ZoneType.SUMP_MARKET || zt == ZoneType.SEWER_CONDUIT || zt == ZoneType.MUTANT_SEWER_CAMP || zt == ZoneType.MUTANT_WARRENS) { add(out,"Sump fungus loaf","Tin of corpse-starch","Water purification tab"); return; }
        if (zt == ZoneType.CULTIST_SEWER_CAMP) { add(out,"Cult offering wafer","Tin of corpse-starch","Dirty canteen"); return; }
        add(out,"Cheap lunch tin","Emergency rations","Child creche snack pack");
    }

    private static void addWater(ArrayList<String> out, ZoneType zt, String text) {
        if (zt == ZoneType.SUMP_MARKET || zt == ZoneType.SEWER_CONDUIT) add(out,"Water guild token","Filter canteen","Water purification tab","Sealed water ration");
        else if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) add(out,"Sealed water ration","Water bottle","Noble preserved delicacy");
        else if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) add(out,"Water purification tab","Filter canteen","Mechanicus nutrient ampoule");
        else add(out,"Water bottle","Water ration","Sealed water ration","Dirty canteen");
    }

    private static void addParts(ArrayList<String> out, ZoneType zt, String text) {
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) add(out,"Mechanicus calibration probe","Servo-skull maintenance kit","Machine part","Sacred wire bundle","Machine oil vial");
        else if (zt == ZoneType.TRAIN_SERVICE_YARD || zt == ZoneType.NEUTRAL_RAIL_DEPOT) add(out,"Rail cargo stencil kit","Cargo hook","Machine part","Wire bundle","Work gloves");
        else if (zt == ZoneType.TRASH_WARREN || zt == ZoneType.SUMP_MARKET || zt == ZoneType.GANGER_TURF) add(out,"Scrap recycler gasket set","Machine part","Wire bundle","Lockpicks","Market scale set");
        else add(out,"Machine part","Wire bundle","Tool bundle","Spare bolts");
    }

    private static void addByRoomPurpose(ArrayList<String> out, ZoneType zt, String text) {
        if (has(text,"barrack","dorm","apartment","rest cell","sleeper","sleep")) add(out, clothingFor(zt), "Laundry token bundle", "Padded coat");
        if (has(text,"trash","refuse","bin","waste","garbage","discarded")) add(out, "Used food tin", "Ration wrapper", "Yesterday's INN newspaper", "Old INN newspaper", "Useless paper mush", "Stub light", "Glow stick");
        if (has(text,"cafeteria","mess","kitchen","galley","pantry","food","canteen")) { addFood(out, zt, text); add(out, "Used food tin", "Ration wrapper"); }
        if (has(text,"storehouse","ration store","food store")) addFood(out, zt, text);
        if (has(text,"warehouse","freight","cargo","product")) add(out, warehouseGoodFor(zt), "Warehouse inventory tag bundle", "Construction supplies");
        if (has(text,"storefront","counter","market","ticket","quartermaster")) add(out, commerceGoodFor(zt), "Trade chit", "Water guild token");
        if (has(text,"clinic","aid","medicae","wound","diagnostic")) add(out, medicalGoodFor(zt), "Bandage roll", "Antiseptic vial", "Medi-Stimm", "White Mercy");
        if (has(text,"chem","drug","narcotic","bar","vice","pleasure","smoke","stimm","labor dosing")) add(out, chemGoodFor(zt), labEquipmentFor(zt, text), "Chemical reagent rack", "Lho-Sticks", "Recaf", "Low Amasec");
        if (has(text,"library","learning","instruction","data-chapel","archive","file","clerk","news","print","broadcast","editor")) add(out, knowledgeGoodFor(zt), "Primer slate", "Data spike", "Fresh INN newspaper", "Old INN newspaper");
        if (has(text,"daycare","creche","child")) add(out, "Creche lesson toy", "Child creche snack pack", "Child minder apron");
        if (has(text,"training","drill","armory","munition","security","holding","cell","interrogation")) add(out, weaponOrSecurityGoodFor(zt), ammoFor(zt), explosiveFor(zt), "Battered helmet");
        if (has(text,"workshop","maintenance","machine","boiler","relay","smelter","recycler")) { addParts(out, zt, text); add(out, "Flashlight", "Stub light", "Electrician's rig"); }
        if (has(text,"garden","hydroponic","fungus","swamp","abandoned","sewer")) add(out, "Phosphor bulb", "Swamp lantern", "Glow stick");
        if (zt == ZoneType.SEWER_CONDUIT || zt == ZoneType.SUMP_MARKET || zt == ZoneType.MUTANT_SEWER_CAMP || zt == ZoneType.MUTANT_WARRENS) add(out, "Phosphor bulb", "Swamp lantern", "Glow stick", "Stub light");
        if (out.isEmpty()) add(out, zoneGeneralGood(zt), "Emergency rations", "Water bottle", "Stub light");
    }

    private static String normalizeLegacyLoot(String legacy, RoomProfile rp, ZoneType zt, Random rng) {
        if (legacy == null) return "Vended scrap";
        String x = legacy.toLowerCase(Locale.ROOT).trim();
        if (ItemCatalog.get(legacy) != null) return legacy;
        if (x.contains("guard ration") || x.contains("ration tin")) return "Guard field ration tin";
        if (x.contains("fine meal") || x.contains("spice") || x.contains("silver ration")) return "Noble preserved delicacy";
        if (x.contains("nutrient") || x.contains("paste")) return "Mechanicus nutrient ampoule";
        if (x.contains("fungus")) return "Sump fungus loaf";
        if (x.contains("cult") || x.contains("offering")) return "Cult offering wafer";
        if (x.contains("toy")) return "Creche lesson toy";
        if (x.contains("meal") || x.contains("lunch") || x.contains("food") || x.contains("ration")) return "Emergency rations";
        if (x.contains("water")) return "Sealed water ration";
        if (x.contains("canteen")) return "Dirty canteen";
        if (x.contains("wire")) return "Wire bundle";
        if (x.contains("machine") || x.contains("scrap") || x.contains("rust")) return "Machine part";
        if (x.contains("cloth") || x.contains("fabric") || x.contains("fatigue")) return clothingFor(zt);
        if (x.contains("newspaper") || x.contains("newsprint") || x.contains("paper")) return "Old INN newspaper";
        if (x.contains("tin")) return "Used food tin";
        if (x.contains("wrapper")) return "Ration wrapper";
        if (x.contains("book") || x.contains("manual") || x.contains("slate") || x.contains("data")) return knowledgeGoodFor(zt);
        if (x.contains("permit") || x.contains("form") || x.contains("ink") || x.contains("ledger")) return commerceGoodFor(zt);
        if (x.contains("key") || x.contains("citation") || x.contains("lock")) return securityGoodFor(zt);
        if (x.contains("fuel") || x.contains("grease")) return "Kitchen grease tin";
        if (x.contains("filter")) return "Water purification tab";
        if (x.contains("sample") || x.contains("vial")) return "Antiseptic vial";
        if (x.contains("tool") || x.contains("hook")) return zt == ZoneType.TRAIN_SERVICE_YARD ? "Rail spike hammer" : "Tool bundle";
        if (x.contains("trade") || x.contains("token") || x.contains("chit")) return commerceGoodFor(zt);
        return zoneGeneralGood(zt);
    }

    private static String catalogOrFallback(String name, String fallback) { return ItemCatalog.get(name) == null ? fallback : name; }
    private static boolean has(String text, String... keys){ for(String k: keys) if(text.contains(k)) return true; return false; }
    private static void add(ArrayList<String> out, String... names){ for(String n:names) if(n!=null && ItemCatalog.get(n)!=null) out.add(n); }

    private static String clothingFor(ZoneType zt){
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) return "Guard flak vest";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE) return "Arbites riot visor";
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Mechanicus rubberized apron";
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Noble fur-lined coat";
        if (zt == ZoneType.SUMP_MARKET) return "Market vendor sash";
        if (zt == ZoneType.CULTIST_SEWER_CAMP) return "Cult hooded wrap";
        if (zt == ZoneType.IMPERIAL_NEWS_NETWORK) return "Plain civilian coat";
        if (zt == ZoneType.TRAIN_SERVICE_YARD || zt == ZoneType.NEUTRAL_RAIL_DEPOT) return "Rail worker hazard coat";
        return "Plain civilian coat";
    }
    private static String warehouseGoodFor(ZoneType zt){
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) return "Quartermaster ledger slate";
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Servo-skull maintenance kit";
        if (zt == ZoneType.IMPERIAL_NEWS_NETWORK) return "Fresh INN newspaper";
        if (zt == ZoneType.IMPERIAL_NEWS_NETWORK) return "Fresh INN newspaper";
        if (zt == ZoneType.TRAIN_SERVICE_YARD || zt == ZoneType.NEUTRAL_RAIL_DEPOT) return "Rail cargo stencil kit";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE) return "Arbites restraint kit";
        return "Warehouse inventory tag bundle";
    }
    private static String commerceGoodFor(ZoneType zt){
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Noble signet wax kit";
        if (zt == ZoneType.ADMINISTRATUM_ARCHIVE) return "Administratum stamp matrix";
        if (zt == ZoneType.SUMP_MARKET) return "Market scale set";
        if (zt == ZoneType.TRAIN_SERVICE_YARD || zt == ZoneType.NEUTRAL_RAIL_DEPOT) return "Rail cargo stencil kit";
        return "Permit form";
    }
    private static String medicalGoodFor(ZoneType zt){ return zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT ? "Mechanicus calibration probe" : (zt == ZoneType.IMPERIAL_GUARD_BILLET ? "Field dressings" : "Medkit"); }
    private static String labEquipmentFor(ZoneType zt, String text){
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Calibrated assay shrine";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE || has(t,"interrogation","holding","cell","evidence")) return "Interrogation dosing cradle";
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Spire perfumery glassware";
        if (zt == ZoneType.CULTIST_SEWER_CAMP || has(t,"ritual","shrine","chapel")) return "Ritual censer kiln";
        if (zt == ZoneType.MUTANT_WARRENS || zt == ZoneType.MUTANT_SEWER_CAMP) return "Mutant adaptation rack";
        if (zt == ZoneType.GANGER_TURF || zt == ZoneType.TRASH_WARREN) return "Crude chem bench";
        if (zt == ZoneType.SUMP_MARKET || zt == ZoneType.SEWER_CONDUIT) return "Sump fermentation tub";
        if (has(t,"clinic","medicae","aid")) return "Sterile medicae clean bench";
        if (has(t,"bar","galley","still")) return "Crude still";
        return "Reagent preparation bench";
    }
    private static String chemGoodFor(ZoneType zt){
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) return "Medi-Stimm";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE) return "Black Badge";
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Cogitator Blue";
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Pearl Obscura";
        if (zt == ZoneType.GANGER_TURF) return "Street Stimm";
        if (zt == ZoneType.CULTIST_SEWER_CAMP) return "Witchsalt";
        if (zt == ZoneType.MUTANT_WARRENS || zt == ZoneType.MUTANT_SEWER_CAMP || zt == ZoneType.SEWER_CONDUIT) return "Sumpkalm";
        if (zt == ZoneType.SUMP_MARKET || zt == ZoneType.TRASH_WARREN) return "Pipe Bloom";
        return "Recaf";
    }
    private static String knowledgeGoodFor(ZoneType zt){
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) return "Guard drill manual";
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Mechanicus catechism strip";
        if (zt == ZoneType.IMPERIAL_NEWS_NETWORK) return "Fresh INN newspaper";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE) return "Arbites casebook excerpt";
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Noble etiquette card";
        return "Primer slate";
    }
    private static String weaponOrSecurityGoodFor(ZoneType zt){
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) return "Lasgun";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE) return "Webber";
        if (zt == ZoneType.GANGER_TURF) return "Pipe shotgun";
        if (zt == ZoneType.CULTIST_SEWER_CAMP) return "Heretic nail flail";
        if (zt == ZoneType.MUTANT_WARRENS || zt == ZoneType.MUTANT_SEWER_CAMP) return "Mutant scrap axe";
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Arc Rifle";
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Needle Pistol";
        if (zt == ZoneType.TRAIN_SERVICE_YARD || zt == ZoneType.NEUTRAL_RAIL_DEPOT) return "Hiver emergency breacher";
        return "Knife";
    }
    private static String explosiveFor(ZoneType zt) {
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) return "Frag grenade";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE) return "Smoke grenade";
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Krak grenade";
        if (zt == ZoneType.GANGER_TURF) return "Tripwire mine";
        if (zt == ZoneType.CULTIST_SEWER_CAMP) return "Satchel charge";
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Motion claymore";
        return "Frag grenade";
    }

    private static String ammoFor(ZoneType zt){
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) return "Las charge pack";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE) return "Web cartridge";
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Dueling pistol cartridge box";
        if (zt == ZoneType.GANGER_TURF) return "Shot shell handful";
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Arc capacitor pack";
        return "Stub cartridge box";
    }
    private static String securityGoodFor(ZoneType zt){ return zt == ZoneType.ARBITES_PRECINCT_EDGE ? "Arbites restraint kit" : "Lockpicks"; }
    private static String zoneGeneralGood(ZoneType zt){
        if (zt == ZoneType.IMPERIAL_GUARD_BILLET) return "Guard field ration tin";
        if (zt == ZoneType.ARBITES_PRECINCT_EDGE) return "Citation slate";
        if (zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT) return "Mechanicus calibration probe";
        if (zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.SECTOR_GOVERNORS_MANSION) return "Noble preserved delicacy";
        if (zt == ZoneType.SUMP_MARKET) return "Market scale set";
        if (zt == ZoneType.TRAIN_SERVICE_YARD || zt == ZoneType.NEUTRAL_RAIL_DEPOT) return "Rail cargo stencil kit";
        if (zt == ZoneType.GANGER_TURF) return "Shot shell handful";
        if (zt == ZoneType.CULTIST_SEWER_CAMP) return "Cult offering wafer";
        if (zt == ZoneType.MUTANT_WARRENS || zt == ZoneType.MUTANT_SEWER_CAMP) return "Sump fungus loaf";
        if (zt == ZoneType.TRASH_WARREN) return "Scrap recycler gasket set";
        return "Cheap lunch tin";
    }
}


class TraderStockExpansionApi {
    static void applyFactionIdentityStock(TraderSession t, ZoneType z, NpcEntity npc, Random r){
        if(t == null || z == null || r == null) return;
        if(z == ZoneType.IMPERIAL_GUARD_BILLET){
            add(t,"Guard field ration tin","food",5,"military ration tin with serial-stamped provenance.");
            add(t,"Guard entrenching tool","tool/weapon",9,"military tool with weapon usefulness.");
            if(r.nextBoolean()) add(t,"Medi-Stimm","medical/chem",18,"controlled battlefield stimulant stock.");
            if(r.nextInt(100)<15) add(t,"Injector filling station","equipment/lab/injector",58,"regulated medical dosing apparatus from quartermaster channels.");
            if(r.nextBoolean()) add(t,"Guard flak vest","armor",24,"surplus or semi-surplus flak protection.");
            if(r.nextInt(100)<35) add(t,"Las charge pack","ammo",9,"regulated energy ammunition stock.");
            if(r.nextInt(100)<18) add(t,"Frag grenade","weapon/explosive",22,"controlled munition with unpleasant room-clearing utility.");
            if(r.nextInt(100)<20) add(t,"Lasgun","weapon/ranged",36,"regulated long-las family stock with quartermaster history.");
        } else if(z == ZoneType.ARBITES_PRECINCT_EDGE){
            add(t,"Arbites restraint kit","security/tool",15,"restraints, seals, tags, and bad civic news.");
            if(r.nextBoolean()) add(t,"Black Badge","chem/security-interrogation",24,"restricted alertness chem from evidence-adjacent channels.");
            if(r.nextInt(100)<18) add(t,"Interrogation dosing cradle","equipment/lab/security",70,"security dosing apparatus with paperwork or worse.");
            add(t,"Arbites riot visor","armor",18,"protective visor with institutional scratches.");
            if(r.nextInt(100)<30) add(t,"Arbites suppression shells","ammo/security",11,"controlled suppression ammunition.");
            if(r.nextInt(100)<18) add(t,"Webber","weapon/security",64,"capture weapon with controlled-issue provenance.");
        } else if(z == ZoneType.MECHANICUS_FORGE_CLOISTER || z == ZoneType.MECHANICUS_RELIC_DUCT){
            add(t,"Mechanicus calibration probe","tool",16,"diagnostic probe for machine and lock work.");
            add(t,"Mechanicus nutrient ampoule","food/chemical",8,"nutrient dose from machine-cult stores.");
            if(r.nextBoolean()) add(t,"Cogitator Blue","chem/labor-control",12,"logic-adept focus compound with machine-cult paperwork.");
            if(r.nextInt(100)<22) add(t,"Calibrated assay shrine","equipment/lab/mechanicus",92,"Mechanicus chemical assay apparatus with power and maintenance demands.");
            if(r.nextInt(100)<28) add(t,"Mechanicus arc prod","weapon/tool",32,"electrical tool with combat implications.");
            if(r.nextInt(100)<14) add(t,"Arc Rifle","weapon/ranged",78,"rite-tagged arc weapon from forge custody.");
        } else if(z == ZoneType.NOBLE_SERVICE_SPINE || z == ZoneType.SECTOR_GOVERNORS_MANSION){
            add(t,"Noble preserved delicacy","food/luxury",28,"sealed luxury food for high-status trade.");
            add(t,"Noble signet wax kit","paperwork/luxury",26,"social and paperwork leverage kit.");
            if(r.nextBoolean()) add(t,"Pearl Obscura","chem/noble-luxury",42,"noble-grade dream narcotic in expensive packaging.");
            if(r.nextInt(100)<18) add(t,"Spire perfumery glassware","equipment/lab/noble",96,"luxury infusion apparatus for spire-grade intoxicants.");
            if(r.nextInt(100)<22) add(t,"Noble fur-lined coat","clothing/luxury",40,"lavish disguise and valuable clothing.");
            if(r.nextInt(100)<15) add(t,"Needle Pistol","weapon/luxury",52,"quiet noble-sidearm stock with assassination rumors.");
        } else if(z == ZoneType.SUMP_MARKET || z == ZoneType.NEUTRAL_RAIL_DEPOT || z == ZoneType.TRAIN_SERVICE_YARD){
            add(t,"Market scale set","commerce/tool",10,"trade tool for barter and commerce rooms.");
            add(t,"Rail cargo stencil kit","commerce/tool",8,"cargo marking kit for freight provenance.");
            if(r.nextBoolean()) add(t,"Water guild token","water/trade",7,"redeemable water-market token.");
            if(r.nextInt(100)<30) add(t,"Zip pistol","weapon/improvised",9,"cheap bench-built firearm with plausible deniability.");
            if(r.nextBoolean()) add(t,"Lho-Sticks","chem/everyday-smoke",3,"common smoke stock for worker and market trade.");
            if(r.nextInt(100)<18) add(t,"Crude still","equipment/lab/distillation/crude",26,"market-accessible stillware with questionable inspection history.");
        } else if(z == ZoneType.GANGER_TURF){
            add(t,"Ganger chain cleaver","weapon/contraband",18,"chain-toothed street weapon.");
            if(r.nextBoolean()) add(t,"Shot shell handful","ammo",8,"loose ammunition with ugly provenance.");
            if(r.nextInt(100)<22) add(t,"Tripwire mine","weapon/explosive",28,"corridor trap stock that should make any buyer nervous.");
            add(t,"Pipe shotgun","weapon/improvised",14,"street-cut pipe shotgun from gang workshops.");
            if(r.nextBoolean()) add(t,"Street Stimm","chem/ganger-combat",14,"bootleg combat stimulant from gang kitchens.");
            if(r.nextInt(100)<25) add(t,"Crude chem bench","equipment/lab/crude",32,"gang chem-kitchen apparatus for unsafe batches.");
            add(t,"Market vendor sash","clothing/commerce",9,"trade disguise useful in market-adjacent criminal spaces.");
        } else if(z == ZoneType.CULTIST_SEWER_CAMP){
            add(t,"Cult ritual blade","weapon/contraband",16,"ritual blade with legal and spiritual consequences.");
            add(t,"Heretic nail flail","weapon/contraband",12,"devotional scrap weapon with no sane paperwork.");
            if(r.nextInt(100)<18) add(t,"Satchel charge","weapon/explosive",42,"cult demolition stock wrapped in prayers and obvious crimes.");
            add(t,"Cult hooded wrap","clothing/contraband",12,"risky cult disguise item.");
            if(r.nextBoolean()) add(t,"Witchsalt","chem/cult-warp",28,"ritual trance powder with obvious contraband consequences.");
            if(r.nextInt(100)<20) add(t,"Ritual censer kiln","equipment/lab/ritual",45,"incense and ash apparatus with problematic prayers.");
        } else if(z == ZoneType.MUTANT_WARRENS || z == ZoneType.MUTANT_SEWER_CAMP || z == ZoneType.TRASH_WARREN || z == ZoneType.SEWER_CONDUIT){
            add(t,"Sump fungus loaf","food",3,"low-grade food from dangerous lower spaces.");
            add(t,"Trash-hook spear","weapon",7,"reach-like scavenger weapon.");
            add(t,"Mutant scrap axe","weapon/mutant",9,"oversized scrap axe adapted for mutant strength.");
            if(r.nextBoolean()) add(t,"Kitchen grease tin","chemical/junk",3,"useful grime from kitchens and machines.");
            if(r.nextBoolean()) add(t,"Sumpkalm","chem/mutant-sump",9,"sump settlement sedative and pain-management chem.");
            if(r.nextInt(100)<20) add(t,"Mutant adaptation rack","equipment/lab/mutant",30,"body-adapted dosing rack from sump clinics.");
        } else {
            add(t,"Civilian meal voucher","food/paperwork",3,"civic cafeteria entitlement.");
            add(t,"Laundry token bundle","civic/trade",4,"ordinary daily-life trade tokens.");
            if(r.nextInt(100)<25) add(t,"Primer slate","knowledge/tool",9,"basic public learning slate.");
        }
    }
    private static void add(TraderSession t, String n, String c, int p, String d){
        for(TradeOffer o: t.offers) if(o.name.equalsIgnoreCase(n)) return;
        t.offers.add(new TradeOffer(n,c,p,d));
    }
}



class TraderSupplyChainApi {
    private TraderSupplyChainApi() {}

    static void applySupplyChainStock(TraderSession t, World w, int turn, Random r) {
        if (t == null || w == null) return;
        if (r == null) r = new Random(0);
        java.util.List<ZoneStockMovementRecord> movements = ProductionDistributionApi.parseStockMovementLedger(w.zoneStockMovementHistory);
        java.util.List<ZoneProductionOutputRecord> outputs = ProductionFacilityOutputSimulationApi.parseProductionLedger(w.zoneProductionHistory);
        if (movements.isEmpty() && outputs.isEmpty()) return;
        int added = 0;
        int traced = 0;
        for (ZoneStockMovementRecord m : movements) {
            if (m == null) continue;
            if (!isTraderRelevant(t, m)) continue;
            ArrayList<String> samples = ProductionFacilityOutputSimulationApi.sampleList(m.itemSamples);
            if (samples.isEmpty()) samples.add(fallbackItemForMovement(m, w.zoneType));
            int cap = Math.min(2, samples.size());
            for (int i = 0; i < cap; i++) {
                String item = samples.get(Math.floorMod(r.nextInt() + i, samples.size()));
                if (item == null || ItemCatalog.get(item) == null) continue;
                ItemProvenanceRecord pr = provenanceFromMovement(item, t, w, turn, m);
                if (mergeOrAddOffer(t, item, categoryFor(item), ItemCatalog.priceFor(item), "supply-chain stock routed through " + m.destination + ".", pr)) added++;
                traced++;
            }
        }
        if (added < 2 && !outputs.isEmpty()) {
            for (ZoneProductionOutputRecord out : outputs) {
                ArrayList<String> samples = ProductionFacilityOutputSimulationApi.sampleList(out.sampleItems);
                if (samples.isEmpty()) continue;
                String item = samples.get(Math.floorMod(r.nextInt(), samples.size()));
                if (item == null || ItemCatalog.get(item) == null) continue;
                ItemProvenanceRecord pr = provenanceFromOutput(item, t, w, turn, out);
                if (mergeOrAddOffer(t, item, categoryFor(item), ItemCatalog.priceFor(item), "facility-output stock offered through local trade.", pr)) added++;
                traced++;
                if (added >= 3) break;
            }
        }
        if (traced > 0) {
            t.supplyChainSummary = added + " new traced offer(s), " + traced + " stock-ledger sample(s); " + compactLedger(w.zoneStockMovementHistory);
            DebugLog.audit("TRADER_SUPPLY_CHAIN", "trader=" + t.name + " zone=" + w.zoneType.label + " added=" + added + " traced=" + traced + " summary=" + t.supplyChainSummary);
        }
    }

    static boolean isTraderRelevant(TraderSession t, ZoneStockMovementRecord m) {
        String text = (safe(t.zoneLabel) + " " + safe(t.archetype) + " " + safe(m.destination) + " " + safe(m.movementKind) + " " + safe(m.historyNote)).toLowerCase(Locale.ROOT);
        if (text.contains("trader") || text.contains("storefront") || text.contains("market") || text.contains("counter") || text.contains("barter") || text.contains("shelf")) return true;
        if (t.sourceSite != null && m.sourceFacilityId != null && m.sourceFacilityId.toLowerCase(Locale.ROOT).contains(t.sourceSite.faction.name().toLowerCase(Locale.ROOT))) return true;
        return text.contains("ration") || text.contains("warehouse") || text.contains("cargo") || text.contains("issue") || text.contains("storehouse");
    }

    static boolean mergeOrAddOffer(TraderSession t, String item, String category, int price, String desc, ItemProvenanceRecord pr) {
        for (TradeOffer o : t.offers) {
            if (o.name.equalsIgnoreCase(item)) {
                if (o.provenance == null) o.provenance = pr;
                if (o.description == null || !o.description.toLowerCase(Locale.ROOT).contains("supply")) o.description = o.description + " Supply-chain trace available.";
                return false;
            }
        }
        t.offers.add(new TradeOffer(item, category, Math.max(1, price), desc, pr));
        return true;
    }

    static ItemProvenanceRecord provenanceFromMovement(String item, TraderSession t, World w, int turn, ZoneStockMovementRecord m) {
        String maker = "stock movement " + safe(m.id) + " from " + safe(m.sourceFacilityId);
        String inputs = "production-distribution ledger; controller=" + safe(m.controller) + "; samples=" + safe(m.itemSamples);
        String route = "moved by " + safe(m.movementKind) + " to " + safe(m.destination) + " -> loaded onto trader shelf for " + safe(t.name);
        ItemProvenanceRecord pr = ItemProvenanceRecord.of(item, Faction.NONE, maker, w, turn, inputs, route);
        if (m.controller != null && !m.controller.isBlank()) pr.makerFaction = m.controller;
        pr.chain = maker + " -> " + safe(m.destination) + " -> " + t.name + " shelf";
        return pr;
    }

    static ItemProvenanceRecord provenanceFromOutput(String item, TraderSession t, World w, int turn, ZoneProductionOutputRecord out) {
        String maker = "facility output " + safe(out.id) + " from " + safe(out.facilityId);
        String inputs = "facility output ledger; focus=" + safe(out.outputFocus) + "; cadence=" + safe(out.cadence) + "; batches=" + out.batches;
        String route = "facility output diverted to faction store/trader shelf for " + safe(t.name);
        ItemProvenanceRecord pr = ItemProvenanceRecord.of(item, Faction.NONE, maker, w, turn, inputs, route);
        if (out.controller != null && !out.controller.isBlank()) pr.makerFaction = out.controller;
        pr.chain = maker + " -> local faction store -> " + t.name + " shelf";
        return pr;
    }

    static String fallbackItemForMovement(ZoneStockMovementRecord m, ZoneType zt) {
        String text = (safe(m.destination) + " " + safe(m.movementKind) + " " + safe(m.historyNote) + " " + (zt == null ? "" : zt.label)).toLowerCase(Locale.ROOT);
        if (text.contains("ration") || text.contains("food") || text.contains("mess")) return "Emergency rations";
        if (text.contains("water")) return "Water ration";
        if (text.contains("armory") || text.contains("weapon")) return "Stub rounds";
        if (text.contains("clinic") || text.contains("medical")) return "Bandage roll";
        if (text.contains("warehouse") || text.contains("cargo")) return "Warehouse inventory tag bundle";
        if (text.contains("mechanicus")) return "Mechanicus calibration probe";
        return "Trade chit";
    }

    static String categoryFor(String item) { ItemDef d = ItemCatalog.get(item); return d == null ? "supply-chain stock" : d.category; }
    static String compactLedger(String ledger) { if (ledger == null || ledger.isBlank()) return "no movement ledger"; String s = ledger.replace(";;", " | "); return s.length() > 180 ? s.substring(0, 180) + "..." : s; }
    static String safe(String s) { return s == null ? "" : s; }
}


class TraderSession {
    String name, archetype, zoneLabel; int discountPct=0, markupPct=0, haggleAttempts=0;
    String supplyChainSummary = "";
    NpcFactionSite sourceSite = null;
    ArrayList<TradeOffer> offers = new ArrayList<>();
    static TraderSession forNpc(NpcEntity npc, ZoneType zone, Random r) {
        TraderSession t = new TraderSession();
        t.name = npc == null ? "Nameless Counter" : npc.name;
        t.archetype = npc == null ? "Trader" : npc.role;
        t.zoneLabel = zone == null ? "Unknown Zone" : zone.label;
        ZoneType z = zone == null ? ZoneType.NEUTRAL_CIVILIAN_FLOOR : zone;
        if (z == ZoneType.MECHANICUS_FORGE_CLOISTER || (npc != null && npc.faction.name().startsWith("MECHANICUS"))) {
            t.offers.add(new TradeOffer("Machine part", "mechanical", 3, "usable in workbench fabrication and machine repair."));
            t.offers.add(new TradeOffer("Mechanical detritus", "mechanical", 2, "scrap-mouth parts and incomplete components."));
            t.offers.add(new TradeOffer("Mechanicus tool roll", "tool", 8, "improves door, machine, and workbench checks."));
            t.offers.add(new TradeOffer("Sacred wire bundle", "component", 5, "useful in electrical and EMM construction later."));
            t.offers.add(new TradeOffer("Data spike", "security", 18, "single-use electronic intrusion spike for vending hacks and electronic locks."));
        } else if (z == ZoneType.GANGER_TURF || (npc != null && (npc.faction == Faction.BANDIT || npc.faction.name().startsWith("GANGER")))) {
            t.offers.add(new TradeOffer("Stub cartridge box", "ammo", 5, "ammunition and threats in one small packet."));
            t.offers.add(new TradeOffer("Damaged ganger coat", "clothing", 5, "minor protection and questionable social value."));
            t.offers.add(new TradeOffer("Lockpicks", "tool", 6, "opens weaker doors if the dice and your hands agree."));
            t.offers.add(new TradeOffer("Coffee tin", "stimulant", 3, "reduces sleep need at a cumulative endurance cost."));
        } else if (z == ZoneType.ARBITES_PRECINCT_EDGE) {
            t.offers.add(new TradeOffer("Permit form", "paperwork", 10, "a document-shaped shield against official questions."));
            t.offers.add(new TradeOffer("Data spike", "security", 22, "single-use electronic intrusion spike; illegal enough to have a smell."));
            t.offers.add(new TradeOffer("Plain ration pack", "food", 3, "lawful calories, allegedly."));
            t.offers.add(new TradeOffer("Water ration", "water", 2, "sealed and stamped."));
            t.offers.add(new TradeOffer("Baton-dented vest", "armor", 9, "civilian-grade protection with institutional bruising."));
        } else if (z == ZoneType.SEWER_CONDUIT || z == ZoneType.MUTANT_SEWER_CAMP || z == ZoneType.CULTIST_SEWER_CAMP) {
            t.offers.add(new TradeOffer("Filter mask", "survival", 5, "helps justify breathing in the sewers."));
            t.offers.add(new TradeOffer("Water purification tab", "water", 3, "makes suspicious water slightly less prosecutable by biology."));
            t.offers.add(new TradeOffer("Emergency rations", "food", 4, "compressed survival matter."));
            t.offers.add(new TradeOffer("Rusty knife", "weapon", 4, "a social tool with edges."));
            t.offers.add(new TradeOffer("Antiseptic vial", "medical", 6, "stops sewer injuries from becoming sewer opinions."));
        } else {
            t.offers.add(new TradeOffer("Emergency rations", "food", 4, "restores food when consumed."));
            t.offers.add(new TradeOffer("Water bottle", "water", 3, "restores water when consumed."));
            t.offers.add(new TradeOffer("Construction supplies", "supplies", 5, "useful for base construction."));
            t.offers.add(new TradeOffer("Tool bundle", "tool", 7, "basic tools for doors, repair, and fabrication."));
            t.offers.add(new TradeOffer("Plain civilian coat", "clothing", 6, "low-grade defense and civilian cover."));
            t.offers.add(new TradeOffer("Bandage roll", "medical", 4, "cheap immediate bleeding control."));
        }
        TraderStockExpansionApi.applyFactionIdentityStock(t, z, npc, r);
        if (r.nextDouble() < 0.35) t.offers.add(new TradeOffer("Stim vial", "stimulant", 6, "pushes back sleep need while accumulating strain."));
        if (r.nextDouble() < 0.28) t.offers.add(new TradeOffer(r.nextBoolean()?"Field dressings":"Medkit", "medical", r.nextBoolean()?7:14, "medical treatment for bleeding, wounds, and body damage."));
        if (r.nextDouble() < 0.12) t.offers.add(new TradeOffer("Data spike", "security", 24, "rare single-use electronic intrusion spike."));
        t.applyStockQuality(z, r);
        return t;
    }

    void applyNpcSiteStock(NpcFactionSite site, Random r, World world, int turn) {
        if (site == null) return;
        sourceSite = site;
        for (String item : site.exportSample(r)) {
            ItemProvenanceRecord made = ItemProvenanceRecord.of(item, site.faction, site.name, world, Math.max(0, site.lastProductionTurn), site.recipeSummaryFor(item), "produced into faction stock ledger");
            ItemProvenanceRecord shelf = ItemProvenanceRecord.transferred(made, item, world, turn, "loaded from " + site.name + " onto trader shelf for " + name);
            boolean exists = false;
            for (TradeOffer o : offers) if (o.name.equalsIgnoreCase(item)) {
                exists = true;
                if (o.provenance == null) o.provenance = shelf;
                break;
            }
            if (!exists) offers.add(new TradeOffer(item, "site stock", Math.max(2, ItemCatalog.priceFor(item)), "traceable stock from " + site.name + ".", shelf));
        }
    }

    void applySupplyChainStock(World world, int turn, Random r) {
        TraderSupplyChainApi.applySupplyChainStock(this, world, turn, r);
    }

    void applyStockQuality(ZoneType z, Random r) {
        for (TradeOffer o : offers) {
            if (ItemCatalog.get(o.name) == null || ItemQuality.tierIndex(o.name) != 2 || o.name.startsWith("Common ")) continue;
            int tier = qualityTierForStock(z, r, o);
            if (tier != 2) o.name = ItemQuality.NAMES[tier] + " " + o.name;
        }
    }
    int qualityTierForStock(ZoneType z, Random r, TradeOffer o) {
        int roll = r.nextInt(100);
        int bonus = 0;
        if (z == ZoneType.MECHANICUS_FORGE_CLOISTER && (o.category.contains("tool") || o.category.contains("component") || o.category.contains("security"))) bonus += 16;
        if (z == ZoneType.NEUTRAL_RAIL_DEPOT && (o.category.contains("supplies") || o.category.contains("tool"))) bonus += 8;
        if (z == ZoneType.GANGER_TURF || z == ZoneType.SEWER_CONDUIT || z == ZoneType.MUTANT_SEWER_CAMP || z == ZoneType.CULTIST_SEWER_CAMP) bonus -= 10;
        roll += bonus;
        if (roll < 8) return 0;
        if (roll < 25) return 1;
        if (roll < 67) return 2;
        if (roll < 83) return 3;
        if (roll < 93) return 4;
        if (roll < 98) return 5;
        if (roll < 100) return 6;
        return 7;
    }
    int buyPrice(TradeOffer o) { int p = ItemCatalog.priceFor(o.name); if (p <= 1 && ItemCatalog.get(o.name) == null) p = o.basePrice; p = (int)Math.ceil(p * (100 + markupPct - discountPct) / 100.0 * WorldGenerationApi.settings().priceMultiplier()); return Math.max(1, p); }
    int sellPrice(String item) {
        int v = ItemCatalog.priceFor(item);
        return Math.max(1, (int)Math.floor(v * (100 - markupPct/2) / 100.0 / Math.max(0.75, WorldGenerationApi.settings().priceMultiplier())));
    }
    String rumor(ZoneType z, Random r) {
        String[] generic = {"Doors remember who forces them.", "A quiet corridor is often waiting for witnesses.", "The lower sewers have started praying back.", "If a room has guards, it has ledgers or loot."};
        if (z == ZoneType.GANGER_TURF) return "Gang colors shift by hallway. Do not mistake one red rag for another.";
        if (z == ZoneType.MECHANICUS_FORGE_CLOISTER) return "Forge cloisters sell parts, but never without measuring your ignorance.";
        if (z == ZoneType.NEUTRAL_RAIL_DEPOT) return "The station connects sectors, but every train carries somebody else's jurisdiction.";
        if (z == ZoneType.MUTANT_SEWER_CAMP || z == ZoneType.CULTIST_SEWER_CAMP) return "Some sewer ladders go down farther than the map admits.";
        return generic[r.nextInt(generic.length)];
    }
}







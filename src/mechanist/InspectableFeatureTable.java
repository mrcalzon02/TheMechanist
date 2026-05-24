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

class InspectableFeatureTable {
    static String sampleLine(){ return "mold, prayer marks, personal debris, refuse piles, benches, beds, lockers, loose wiring, pipe leaks, shrine scratches, machine housings, ration wrappers, empty bottles"; }
    static final LinkedHashMap<ZoneType,String[]> TABLE = new LinkedHashMap<>();
    static {
        TABLE.put(ZoneType.HAB_STACK, new String[]{"owner-none bunk plaque", "black mold seam", "scratched prayer marks", "personal debris heap", "disgusting cot", "broken privacy curtain", "trash bin", "cracked basin", "collapsed sleeping shelf", "rusted footlocker", "child-sized blanket nest", "condensation-stained wall shrine"});
        TABLE.put(ZoneType.NEUTRAL_CIVILIAN_FLOOR, new String[]{"ration queue bench", "civilian notice board", "water token dispenser", "broken ticket bell", "public cot", "cheap eating counter", "permit kiosk", "missing-person scrawl", "sealed medicae cabinet", "laundry bundle", "worker prayer tile", "public trash barrel"});
        TABLE.put(ZoneType.SUMP_MARKET, new String[]{"merchant awning", "locked trade crate", "cheap scale", "water exchange barrel", "counterfeit permit stack", "barter chalkboard", "spice-stained table", "coin slot shrine", "patched canvas stall", "debt marker board", "scrap sort bin", "watchman stool"});
        TABLE.put(ZoneType.GANGER_TURF, new String[]{"territory tag wall", "bullet-pocked table", "stash brick", "lookout stool", "blood-dark drain", "cheap shrine to violence", "drug foil pile", "ammo tin", "rival warning mark", "knife game table", "smuggled radio", "burned gang banner"});
        TABLE.put(ZoneType.TRASH_WARREN, new String[]{"compacted refuse wall", "useful rag knot", "crawling mold layer", "metal teeth scrap", "bone heap", "half-buried water bottle", "rat tunnel", "collapsed bin stack", "recycler grate", "old glove pile", "fungal stain", "smoldering trash pocket"});
        TABLE.put(ZoneType.MUTANT_WARRENS, new String[]{"bone nest", "claw-marked wall", "fungus sleeping mat", "mutant warning rune", "chewed armor piece", "blood-warm bedding", "strange hair clump", "boiled leather strap", "dripping sump shrine", "gnawed ration tin", "unpleasant family icon", "glowing mold vein"});
        TABLE.put(ZoneType.SEWER_CONDUIT, new String[]{"rusted ladder bracket", "slime-coated pipe", "filter grate", "maintenance tally mark", "sump fungus shelf", "drain current ripple", "pressure valve", "floating refuse snag", "broken inspection lamp", "black-water eddy", "old boot print", "pipe access wheel"});
        TABLE.put(ZoneType.MUTANT_SEWER_CAMP, new String[]{"bone-and-pipe barricade", "fungus rack", "mutant sleeping hollow", "slick claw gouge", "sewer idol", "boiled water pot", "teeth necklace", "sump-hide curtain", "blood-washed drain", "scrap spear bundle", "warm nesting trash", "warning scrape"});
        TABLE.put(ZoneType.CULTIST_SEWER_CAMP, new String[]{"drain candle circle", "painted heresy mark", "knife-scratched prayer", "blood gutter", "hidden offering bowl", "torn robe bundle", "wax-coated grate", "whispering pipe seam", "ritual ash pile", "unlicensed corpse niche", "tainted water jar", "chain-bound hatch"});
        TABLE.put(ZoneType.ADMINISTRATUM_ARCHIVE, new String[]{"wet paper stack", "broken filing altar", "stamp drawer", "red tape snarl", "permit cage", "clerk stool", "index cogitator", "identity slate pile", "queue rail", "locked record cabinet", "ink reservoir", "petition rejection bin"});
        TABLE.put(ZoneType.ARBITES_PRECINCT_EDGE, new String[]{"barred service window", "confiscation crate", "interrogation bench", "wanted board", "shock baton rack", "citation terminal", "blood-wiped floor drain", "evidence locker", "permit checkpoint bell", "armored counter", "holding cell grille", "law plaque"});
        TABLE.put(ZoneType.IMPERIAL_GUARD_BILLET, new String[]{"stacked bunk", "ration tin locker", "las-cell crate", "muddy boot row", "orders board", "prayer strip", "helmet rack", "field cot", "munition tally", "guard-issue water can", "sandbag wall", "fatigue-stained blanket"});
        TABLE.put(ZoneType.MECHANICUS_FORGE_CLOISTER, new String[]{"tool shrine", "oil-stained cogitator", "cable loom", "incense burner", "machine parts tray", "diagnostic lectern", "servo skull perch", "maintenance rite plate", "sealed component locker", "warning lumen", "heat-scarred floor", "chanting speaker grill"});
        TABLE.put(ZoneType.MECHANICUS_RELIC_DUCT, new String[]{"ancient conduit idol", "relic access hatch", "frosted data panel", "red prayer seal", "broken servo arm", "sacred grease mark", "watchful lens cluster", "coded pipe tags", "sealed relic vent", "machine tooth rack", "dormant actuator", "oil-caked purity ribbon"});
        TABLE.put(ZoneType.NOBLE_SERVICE_SPINE, new String[]{"servant pantry shelf", "polished waste chute", "forbidden velvet scrap", "private lift plaque", "silver ration tin", "gilded warning sign", "house cipher mark", "perfumed refuse sack", "clean water spigot", "locked wardrobe", "service bell", "gene-sealed cabinet"});
        TABLE.put(ZoneType.TRAIN_SERVICE_YARD, new String[]{"freight ledger", "cargo chain", "signal lever", "rail map plate", "ticket punch", "locked crate", "maintenance bench", "brake valve", "coal dust pile", "train bell cord", "platform warning strip", "worker thermos"});
        TABLE.put(ZoneType.NEUTRAL_RAIL_DEPOT, new String[]{"neutral trade counter", "rail schedule board", "sealed parcel shelf", "vendor cage", "platform bench", "cargo claim box", "water ration kiosk", "map placard", "public vox tube", "station shrine", "security mirror", "merchant stool"});
    }
    static String[] featuresFor(ZoneType z, Random r){
        String[] f = TABLE.get(z);
        if(f != null) return f;
        return new String[]{"loose trash", "old wall scratch", "rust patch", "broken bench", "discarded blanket", "small locked box"};
    }
    static String combinedFor(ZoneType z, Random r, String seedDescriptor){
        String[] f = featuresFor(z, r);
        ArrayList<String> pick = new ArrayList<>();
        for(int i=0;i<Math.min(4, f.length);i++) pick.add(f[(r.nextInt(f.length)+i)%f.length]);
        return seedDescriptor + "; inspectable features: " + String.join(", ", pick);
    }
}

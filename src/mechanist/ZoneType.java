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

enum ZoneType {
    NEUTRAL_CIVILIAN_FLOOR("Neutral Civilian Floor", "hab queues, depot counters, legal-ish civilians, and people trying not to be noticed", 5, 1),
    TRASH_WARREN("Trash Warren", "refuse heaps, ratways, broken lockers", 8, 0),
    HAB_STACK("Hab Stack", "sleeping cells, kitchens, family shrines", 4, 0),
    SUMP_MARKET("Sump Market", "neutral stalls, barter counters, guarded depots", 6, 1),
    GANGER_TURF("Ganger Turf", "tagged dens, stash rooms, ambush corridors", 7, -1),
    ARBITES_PRECINCT_EDGE("Arbites Precinct Edge", "public counter rooms, holding pens, checkpoint halls", 2, -2),
    MECHANICUS_RELIC_DUCT("Mechanicus Relic Duct", "machine alcoves, cable vaults, old EMM pads", 5, 2),
    MUTANT_WARRENS("Mutant Warrens", "collapsed dens, bone alcoves, fungal stores", 5, 1),
    MUTANT_SEWER_CAMP("Mutant Sewer Encampment", "sewer camps fouled by territorial mutant packs that attack strangers on sight", 7, 1),
    CULTIST_SEWER_CAMP("Cultist Sewer Encampment", "sump shrines, painted drains, mad hymns, and heretics in the runoff", 6, 1),
    SEWER_CONDUIT("Sewer Conduit", "drain tunnels, sump gates, ladder shafts", 9, 2),
    TRAIN_SERVICE_YARD("Train Service Yard", "freight lockers, railside offices, cargo cages", 6, 2),
    ADMINISTRATUM_ARCHIVE("Administratum Archive", "paper catacombs, queue halls, permit cages, stamped misery", 3, -1),
    IMPERIAL_GUARD_BILLET("Imperial Guard Billet", "barracks overflow, armory cages, rations, bored soldiers", 3, -2),
    MECHANICUS_FORGE_CLOISTER("Mechanicus Forge Cloister", "red-lit shrines, machine cells, manufactorum ducts", 5, 2),
    SECTOR_GOVERNORS_MANSION("Sector Governor\'s Mansion", "lavish upper-sector residence, bribe chamber, ticket gamble, elite guard response", 4, -3),
    NOBLE_SERVICE_SPINE("Noble Service Spine", "servant passages, locked pantries, security doors", 3, -1),
    IMPERIAL_NEWS_NETWORK("Imperial News Network Bureau", "newsprint rooms, editorial desks, vox studios, pict-screen booths, public distributors, and sanctioned omissions", 4, 1),
    NEUTRAL_RAIL_DEPOT("Neutral Rail Depot", "sector train station, legal-ish trade, watchers in every corner", 8, 2);
    final String label, descriptor; final int scavengeBonus; final int richness;
    ZoneType(String label, String descriptor, int scavengeBonus, int richness){this.label=label;this.descriptor=descriptor;this.scavengeBonus=scavengeBonus;this.richness=richness;}
    static ZoneType random(Random r, boolean sewer){
        if(sewer) return r.nextInt(100)<25 ? (r.nextBoolean()?MUTANT_SEWER_CAMP:CULTIST_SEWER_CAMP) : SEWER_CONDUIT;
        ZoneType[] vals={NEUTRAL_CIVILIAN_FLOOR,TRASH_WARREN,HAB_STACK,SUMP_MARKET,GANGER_TURF,ARBITES_PRECINCT_EDGE,MECHANICUS_RELIC_DUCT,MECHANICUS_FORGE_CLOISTER,MUTANT_WARRENS,TRAIN_SERVICE_YARD,ADMINISTRATUM_ARCHIVE,IMPERIAL_GUARD_BILLET,SECTOR_GOVERNORS_MANSION,NOBLE_SERVICE_SPINE};
        return vals[r.nextInt(vals.length)];
    }
    int[] roomSize(Random r){
        int w=2+r.nextInt(7), h=3+r.nextInt(6);
        if(this==HAB_STACK || this==NEUTRAL_CIVILIAN_FLOOR){ w=3+r.nextInt(5); h=3+r.nextInt(5); }
        if(this==SEWER_CONDUIT || this==MUTANT_SEWER_CAMP || this==CULTIST_SEWER_CAMP){ w=2+r.nextInt(4); h=3+r.nextInt(8); }
        if(this==TRAIN_SERVICE_YARD || this==NEUTRAL_RAIL_DEPOT){ w=5+r.nextInt(5); h=4+r.nextInt(5); }
        if(this==NOBLE_SERVICE_SPINE){ w=4+r.nextInt(5); h=3+r.nextInt(4); }
        if(this==SECTOR_GOVERNORS_MANSION){ w=6+r.nextInt(5); h=4+r.nextInt(4); }
        if(this==IMPERIAL_NEWS_NETWORK){ w=5+r.nextInt(5); h=4+r.nextInt(5); }
        return new int[]{Math.min(9,w), Math.min(9,h)};
    }
    char floorGlyph(Random r){
        if(this==SEWER_CONDUIT || this==MUTANT_SEWER_CAMP || this==CULTIST_SEWER_CAMP) return ';';
        if(this==GANGER_TURF) return ',';
        if(this==MECHANICUS_FORGE_CLOISTER || this==MECHANICUS_RELIC_DUCT) return ':';
        if(this==SECTOR_GOVERNORS_MANSION || this==NOBLE_SERVICE_SPINE || this==ADMINISTRATUM_ARCHIVE || this==IMPERIAL_NEWS_NETWORK) return '.';
        if(this==MUTANT_WARRENS || this==TRASH_WARREN) return '`';
        return '.';
    }
    char corridorGlyph(Random r){
        if(this==SEWER_CONDUIT || this==MUTANT_SEWER_CAMP || this==CULTIST_SEWER_CAMP) return '~';
        if(this==MECHANICUS_FORGE_CLOISTER || this==MECHANICUS_RELIC_DUCT) return '=';
        if(this==GANGER_TURF || this==TRASH_WARREN) return ',';
        if(this==SECTOR_GOVERNORS_MANSION || this==NOBLE_SERVICE_SPINE || this==ADMINISTRATUM_ARCHIVE || this==ARBITES_PRECINCT_EDGE || this==IMPERIAL_NEWS_NETWORK) return ':';
        return '+';
    }
}

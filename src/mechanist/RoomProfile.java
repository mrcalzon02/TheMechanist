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

class RoomProfile {
    String name, descriptor, featureText; int scavengeChance; Faction faction; String[] loot; char[] contents;
    RoomProfile(String name,String descriptor,int chance,Faction faction,String[] loot,char[] contents){this.name=name;this.descriptor=descriptor;this.featureText=defaultFeatures(name, descriptor);this.scavengeChance=chance;this.faction=faction;this.loot=loot;this.contents=contents;}
    static String defaultFeatures(String name, String descriptor){ return descriptor; }
    RoomProfile withFeatures(String f){ this.featureText=f; return this; }
    RoomProfile asSpecial(ZoneType z, Random r){
        String[] specialNames={"Trader alcove","Hidden supply cache","Faction-locked chamber","Machinery room","Random encounter room","Shrine-side store","Clinic spillover","Sealed service cell","Watch post"};
        String sn=specialNames[r.nextInt(specialNames.length)];
        RoomProfile rp=new RoomProfile(sn + " / " + name, descriptor + "; special feature: " + sn.toLowerCase(Locale.ROOT), Math.min(90, scavengeChance+12+r.nextInt(12)), faction, loot, contents);
        rp.featureText = featureText + "; SPECIAL: " + sn + "; inspectable hooks: trader, cache, lock, machinery, or encounter hook.";
        return rp;
    }
    RoomProfile asMachineRoom(String machineName, String machineDescription, char glyph){
        RoomProfile rp = new RoomProfile(machineName, machineDescription, Math.max(45, scavengeChance), faction, loot, contents);
        rp.featureText = machineDescription + " Inspectable Martian emergency machine glyph '" + glyph + "'. Look displays the imported EMM machine profile; Interact handles locks, power, ownership, and machine-specific work.";
        return rp;
    }

    static RoomProfile closetStub(ZoneType z, Random r){
        String[] names={"Dead-end utility closet","Blind crawlspace pocket","False maintenance niche","Collapsed side cupboard","One-person storage stub","Sealed service cubby"};
        String[] features=InspectableFeatureTable.featuresFor(z, r);
        String f=features.length==0?"one lonely inspectable thing that probably disappointed someone":features[r.nextInt(features.length)];
        RoomProfile rp = new RoomProfile(names[r.nextInt(names.length)], "single-door stub created when a corridor projection was unsafe", 18+r.nextInt(14), Faction.NONE, new String[]{"bent nail tin","cloth scraps","machine scrap","sealed water ration"}, new char[]{'o','p','b','N','u'});
        rp.featureText = "Closet fallback: " + f + "; generated because the door's intended corridor would have overlapped a room, wall, or existing corridor.";
        return rp;
    }

    static RoomProfile dormitoryCell(ZoneType z, Random r){
        RoomProfile rp = new RoomProfile("Hab Dormitory Cell", "a cramped worker dormitory cell stamped off a long residence corridor; cot, sink, dresser, and cabinet occupy more space than dignity does", 34, Faction.HIVER, new String[]{"sealed water ration","cloth scraps","cheap trinket","emergency ration"}, new char[]{'c','u','s'});
        rp.featureText = "Inspectable dormitory features: narrow cot with patched thermal blanket; stained sink with ration-water residue; small dresser of dented plasteel; cabinet with scratched personal marks.";
        return rp;
    }

    static RoomProfile apartmentRoom(String label, ZoneType z, Random r){
        RoomProfile rp = new RoomProfile(label, "one chamber of a predictable hab apartment block module: compact, repetitive, and designed by someone paid by the square meter saved", 38, Faction.HIVER, new String[]{"sealed water ration","cloth scraps","cheap meal tin","household scrap"}, new char[]{'q','b'});
        if(label.contains("Bedroom")) { rp.contents = new char[]{'c','s'}; rp.featureText = "Inspectable apartment features: low cot, storage locker, folded work clothes, and small devotional scratch marks."; }
        else if(label.contains("Washroom")) { rp.contents = new char[]{'u','N'}; rp.featureText = "Inspectable apartment features: water-stained sink, recycler pipe access, cracked privacy partition, and bad drainage."; }
        else if(label.contains("Dining")) { rp.contents = new char[]{'T','b'}; rp.featureText = "Inspectable apartment features: tiny food-prep counter, battered stool, ration hooks, and stained family table."; }
        else { rp.contents = new char[]{'q','b'}; rp.featureText = "Inspectable apartment features: worn seats, prayer marks, personal debris, and a room that has witnessed too many rent disputes."; }
        return rp;
    }



    static RoomProfile factionRepresentativeBar(ZoneType z, Faction f, Random r){
        Faction fac = f == null ? Faction.HIVER : f;
        String label = fac.label + " Faction Representative Bar";
        RoomProfile rp = new RoomProfile(label,
            "a distinctive faction-aligned continuity bar near a transition edge: long counter, stools, refrigerators, service kegs, cheap radio, pict viewer, patrons, and a protected representative desk",
            42, fac,
            new String[]{"Low Amasec","Recaf tin","Used food tin","Trade chit","Lho-Sticks","Water bottle"},
            new char[]{'T','b','u','N','q','1'});
        rp.featureText = "Continuity bar: recognizable fallback contact point for " + fac.label + ". The representative is invulnerable and untargetable; if the faction controls no other rooms, the rep functions as provisional faction leader. If the faction controls another room, the rank-one leader should reside at that room as the local base of operations.";
        return rp;
    }

    static RoomProfile centralPlaza(ZoneType z, boolean sewer, Random r){
        String name;
        String desc;
        Faction fac = Faction.NONE;
        String[] loot = new String[]{"trade chit","sealed water ration","cloth scraps","machine scrap"};
        char[] contents = new char[]{'T','q','b','h','n'};
        if(sewer || z==ZoneType.SEWER_CONDUIT || z==ZoneType.MUTANT_SEWER_CAMP || z==ZoneType.CULTIST_SEWER_CAMP){
            name = "Sewer Utility Plaza";
            desc = "a centered broad 15x15 utility nexus of generators, waste treatment tanks, water recyclers, power junction boxes, trash machinery, homeless encampments, and scavenger bases";
            contents = new char[]{'N','Z','Y','F','v','m','p'};
            loot = new String[]{"filter cartridge","dirty water","machine parts","wire bundle","sump fungus"};
            fac = (z==ZoneType.MUTANT_SEWER_CAMP?Faction.MUTANT:(z==ZoneType.CULTIST_SEWER_CAMP?Faction.CULTIST:Faction.NONE));
        } else if(z==ZoneType.GANGER_TURF){ name="Gang Camp Plaza"; desc="a central turf camp of tents, weapon tables, lookouts, painted claims, and henchmen watching every approach"; fac=Faction.BANDIT; contents=new char[]{'g','q','b','p','N'}; }
        else if(z==ZoneType.ARBITES_PRECINCT_EDGE){ name="Arbites Precinct Lobby"; desc="a hard-lit precinct lobby with public counters, barred access, queue rails, and armed personnel behind every meaningful door"; fac=Faction.ARBITES; contents=new char[]{'A','q','b','n','N'}; }
        else if(z==ZoneType.MECHANICUS_FORGE_CLOISTER || z==ZoneType.MECHANICUS_RELIC_DUCT){ name="Mechanicus Machine Plaza"; desc="a red-lit machinery nave of emergency machines, relay shrines, maintenance lecterns, and humming industrial organs"; fac=Faction.MECHANICUS; contents=new char[]{'N','R','Z','Y','J','P','F'}; }
        else if(z==ZoneType.ADMINISTRATUM_ARCHIVE){ name="Administratum Queue Plaza"; desc="a central bureaucratic hall of counters, line rails, stamped notices, clerks, and citizens ageing in public"; fac=Faction.ADMINISTRATUM; contents=new char[]{'q','h','b','T','n'}; }
        else if(z==ZoneType.IMPERIAL_NEWS_NETWORK){ name="Imperial News Network Atrium"; desc="a sanctioned media atrium of newspaper dispensers, route boards, vox speakers, pict-screen slogans, editor desks, and reporters pretending neutrality is a physical object"; fac=Faction.INN; contents=new char[]{'1','q','T','b','N'}; loot=new String[]{"Fresh INN newspaper","Yesterday's INN newspaper","Recaf tin","Used food tin"}; }
        else if(z==ZoneType.IMPERIAL_GUARD_BILLET){ name="Imperial Guard Muster Plaza"; desc="a muster hall of cots, ration points, weapon lockers, inspection lines, and exhausted guardsmen"; fac=Faction.IMPERIAL_GUARD; contents=new char[]{'M','q','b','T','N'}; }
        else if(z==ZoneType.SECTOR_GOVERNORS_MANSION){ name="Governor's Audience Hall"; desc="a monstrous upper-sector mansion hall of velvet crime, weaponized etiquette, concealed guard hatches, and the possible off-world ticket"; fac=Faction.NOBLE; contents=new char[]{'Q','n','A','q','b'}; loot=new String[]{"governor signet","permit form","fine ration","trade chit"}; }
        else if(z==ZoneType.NOBLE_SERVICE_SPINE){ name="Noble Service Atrium"; desc="a polished service atrium where servants, locked pantries, watchers, and invisible ownership structure every step"; fac=Faction.NOBLE; contents=new char[]{'n','q','b','A','T'}; }
        else { name="Central Market Plaza"; desc="a centered broad 15x15 market nexus of stalls, roaming traffic, neutral trade plants, little fountains, park scraps, lost children's toys, and hard-eyed witnesses"; contents=new char[]{'T','q','b','h','n'}; }
        RoomProfile rp = new RoomProfile(name, desc, 65, fac, loot, contents);
        rp.featureText = desc + "; all branch corridors originate from or connect back to this plaza so the zone has a guaranteed accessible nexus.";
        return rp;
    }
    static RoomProfile generic(){ return new RoomProfile("Generic underhive room","unremarkable misery with structural opinions",30,Faction.NONE,new String[]{"scrap bundle","bent nail tin","emergency ration"},new char[]{'p','b','N'}); }

    static RoomProfile neutralContestRoom(ZoneType z, Random r){
        String[] names = {"Neutral Empty Room", "Vacant Claim Room", "Unassigned Side Chamber", "Empty Service Lease", "Contestable Utility Room"};
        String name = names[(r == null ? 0 : r.nextInt(names.length))];
        RoomProfile rp = new RoomProfile(name, "an intentionally unoccupied neutral room left between faction claims; bare floor, stripped fixtures, old paper, used tins, and just enough empty space for possible occupation, ambush, squatters, or room-control schemes", 16, Faction.NONE, new String[]{"Old INN newspaper","Used food tin","cloth scraps","paper mush","Vended scrap"}, new char[]{'o','b','q'});
        rp.featureText = "Contestable neutral room: currently unclaimed, lightly littered, and suitable for faction takeover, player occupation, hidden meetings, bank-heist staging, or evidence drops.";
        return rp;
    }

    static RoomProfile themedRoom(String name, String descriptor, int chance, Faction faction, String[] loot, char[] contents){
        return new RoomProfile(name, descriptor, chance, faction, loot, contents);
    }

    static void addInfrastructureRooms(ArrayList<RoomProfile> list, ZoneType z){
        // 0.8.61 FACTION INFRASTRUCTURE LIBRARY:
        // Each zone now receives its own habitation, kitchen/mess, food storehouse, product
        // warehouse, storefront/counter, clinic, workshop, security, sanitation, utility,
        // and shrine/social pattern. Similar civic functions must not share identical text
        // across factions: a noble dormitory, a civilian dormitory, a Guard barracks, and a
        // Mechanicus rest-cell are different rooms with different bodies and values.
        switch(z){
            case NEUTRAL_CIVILIAN_FLOOR:
            case HAB_STACK:
                addCivilianRooms(list, z); break;
            case SUMP_MARKET:
                addMarketRooms(list); break;
            case GANGER_TURF:
                addGangerRooms(list); break;
            case ARBITES_PRECINCT_EDGE:
                addArbitesRooms(list); break;
            case ADMINISTRATUM_ARCHIVE:
                addAdministratumRooms(list); break;
            case IMPERIAL_NEWS_NETWORK:
                addInnRooms(list); break;
            case IMPERIAL_GUARD_BILLET:
                addGuardRooms(list); break;
            case MECHANICUS_FORGE_CLOISTER:
            case MECHANICUS_RELIC_DUCT:
                addMechanicusRooms(list, z); break;
            case SECTOR_GOVERNORS_MANSION:
            case NOBLE_SERVICE_SPINE:
                addNobleRooms(list, z); break;
            case NEUTRAL_RAIL_DEPOT:
            case TRAIN_SERVICE_YARD:
                addRailRooms(list, z); break;
            case SEWER_CONDUIT:
                addSewerRooms(list); break;
            case MUTANT_WARRENS:
            case MUTANT_SEWER_CAMP:
                addMutantRooms(list, z); break;
            case CULTIST_SEWER_CAMP:
                addCultistRooms(list); break;
            case TRASH_WARREN:
                addTrashRooms(list); break;
            default:
                list.add(generic());
        }
    }

    static void addCivilianRooms(ArrayList<RoomProfile> list, ZoneType z){
        Faction f = Faction.HIVER;
        list.add(themedRoom("Civilian Dormitory Bay", "stacked cot racks, shared wash basins, cracked privacy curtains, and family bundles wedged into state-approved human shelving", 38, f, new String[]{"sealed water ration","cloth scraps","cheap trinket","emergency ration"}, new char[]{'h','c','u','s','b'}));
        list.add(themedRoom("Communal Kitchen Block", "ration burners, grease-black counters, water recycler taps, and queue marks cut into the floor by hungry feet", 44, f, new String[]{"cheap meal tin","sealed water ration","fuel scrap","ration ticket"}, new char[]{'T','Y','b','q'}));
        list.add(themedRoom("Food Storehouse", "locked racks of ration tins, dry starch sacks, water tokens, and a clerk desk for denying the obvious", 52, f, new String[]{"emergency ration","sealed water ration","ration ticket","cheap meal tin"}, new char[]{'b','T','q','n'}));
        list.add(themedRoom("Civic Product Warehouse", "crated work clothes, household parts, broken furniture, and tagged bundles waiting for a distribution schedule that died three shifts ago", 50, f, new String[]{"household scrap","cloth scraps","machine scrap","trade chit"}, new char[]{'b','p','N','q'}));
        list.add(themedRoom("Permit Storefront", "a narrow counter selling legal patience: form packets, water claims, ration stamps, and small humiliations", 36, Faction.ADMINISTRATUM, new String[]{"permit stub","ration ticket","trade chit","ink cartridge"}, new char[]{'q','h','b'}));
        list.add(themedRoom("Block Clinic Room", "sealed medicae cabinet, stained cot, boiled instruments, and a queue of people negotiating with pain", 30, f, new String[]{"bandage roll","sealed water ration","sample vial","cloth scraps"}, new char[]{'u','c','b','N'}));
        list.add(themedRoom("Laundry and Wash Room", "steam pipes, hanging work clothes, grey suds, drainage stink, and gossip carried faster than disease", 42, f, new String[]{"laundered cloth","cloth scraps","dirty water","soap stub"}, new char[]{'u','Y','p','b'}));
        list.add(themedRoom("Maintenance Workshop", "patched tools, wire reels, spare fasteners, and a civic mechanic's shrine to making bad hardware continue", 52, f, new String[]{"machine scrap","wire bundle","rusted tool","spare bolts"}, new char[]{'N','R','q','b'}));
        list.add(themedRoom("Family Shrine Alcove", "cheap candles, ancestor marks, ration wrappers folded into offerings, and exhausted faith doing local work", 26, f, new String[]{"wax scrap","cheap trinket","cloth scraps"}, new char[]{'c','n','q'}));
    }

    static void addMarketRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("Sump Market Food Storehouse", "guarded tins, fungus sacks, reclaimed water barrels, and prices adjusted by hunger rather than law", 56, Faction.HIVER, new String[]{"cheap meal tin","sealed water ration","sump fungus","trade chit"}, new char[]{'T','b','Y','q'}));
        list.add(themedRoom("Sump Product Warehouse", "mixed crates of parts, cloth, filters, tools, and cargo tags scraped off previous owners", 58, Faction.HIVER, new String[]{"machine scrap","wire bundle","supply crate scrap","trade chit"}, new char[]{'b','p','N','T'}));
        list.add(themedRoom("Barter Storefront Row", "canvas counters, hanging lamps, chalk prices, and vendors smiling like they know tomorrow's shortages", 44, Faction.HIVER, new String[]{"trade chit","cheap trinket","ration ticket","cloth scraps"}, new char[]{'T','q','b','n'}));
        list.add(themedRoom("Water Merchant Stall", "sealed taps, filter cages, wet ledgers, and guards watching every cup like a jewel", 42, Faction.HIVER, new String[]{"sealed water ration","filter cartridge","dirty water","trade chit"}, new char[]{'Y','q','b'}));
        list.add(themedRoom("Pawn Cage", "barred shelves of watches, tools, boots, knives, heirlooms, and desperation priced by weight", 40, Faction.HIVER, new String[]{"cheap trinket","rusted tool","tiny knife","trade chit"}, new char[]{'b','q','n'}));
        list.add(themedRoom("Market Kitchen Smokehole", "cheap burners, vat grease, suspicious protein, and a crowd pretending the smell is normal", 45, Faction.HIVER, new String[]{"cheap meal tin","spoiled ration","fuel scrap","sump fungus"}, new char[]{'T','B','q'}));
        list.add(themedRoom("Debt Office Back Room", "ledger hooks, intimidation chairs, pawn tags, and an exit placed for the collector rather than the debtor", 28, Faction.BANDIT, new String[]{"trade chit","debt marker","permit scrap"}, new char[]{'q','g','b'}));
        list.add(themedRoom("Repair Booth", "open tool rolls, salvaged components, counterfeit seals, and machines halfway between repair and fraud", 50, Faction.HIVER, new String[]{"machine parts","wire bundle","rusted tool","filter cartridge"}, new char[]{'N','R','q'}));
    }

    static void addGangerRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("Gang Crash Barracks", "mattresses, stolen blankets, weapon hooks, and bodies sleeping in shifts beside loaded grudges", 34, Faction.BANDIT, new String[]{"damaged bandit colors","cloth scraps","ammo scrap","cheap meal tin"}, new char[]{'g','c','b','p'}));
        list.add(themedRoom("Gang Mess and Chem Kitchen", "hot plates, protein tins, stimulant stains, and drug foil mixed with dinner scraps", 42, Faction.BANDIT, new String[]{"stimulant ampoule","spoiled ration","cheap meal tin","chem vial"}, new char[]{'T','g','b','q'}));
        list.add(themedRoom("Protection Storehouse", "food and water taken as payment for not having worse problems, stacked under painted threats", 50, Faction.BANDIT, new String[]{"emergency ration","sealed water ration","trade chit","ammo scrap"}, new char[]{'b','g','q'}));
        list.add(themedRoom("Stolen Goods Warehouse", "crates with scraped serials, cut locks, cargo cloth, and enough lies to open a shop", 58, Faction.BANDIT, new String[]{"supply crate scrap","machine scrap","wire bundle","cheap trinket"}, new char[]{'b','p','g','N'}));
        list.add(themedRoom("Fence Storefront", "a counter where everything is second-hand except the threat behind it", 36, Faction.BANDIT, new String[]{"trade chit","cheap trinket","rusted tool","tiny knife"}, new char[]{'T','g','q'}));
        list.add(themedRoom("Fighting Pit Side Room", "blooded floor rings, betting marks, chain hooks, and a bucket that knows too much", 26, Faction.BANDIT, new String[]{"trade chit","bone charm","cloth scraps"}, new char[]{'g','p','b'}));
        list.add(themedRoom("Chop Shop Workshop", "stripped parts, wire nests, stolen panels, and tools used with criminal enthusiasm", 55, Faction.BANDIT, new String[]{"machine parts","wire bundle","rusted tool","ammo scrap"}, new char[]{'N','R','g','b'}));
        list.add(themedRoom("Lookout Security Nest", "peepholes, alarm strings, slits, and a stool polished by nervous violence", 30, Faction.BANDIT, new String[]{"ammo scrap","tiny knife","trade chit"}, new char[]{'g','A','q'}));
    }

    static void addArbitesRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("Arbites Duty Barracks", "brutalist bunks, boot cages, weapon discipline charts, and sleep treated as a punishable necessity", 26, Faction.ARBITES, new String[]{"sealed water ration","ammo scrap","permit scrap"}, new char[]{'A','c','b','q'}));
        list.add(themedRoom("Precinct Mess Slot", "ration trays, steel benches, recaff stains, and silence sharpened by rank", 30, Faction.ARBITES, new String[]{"guard ration","sealed water ration","cheap meal tin"}, new char[]{'T','A','q'}));
        list.add(themedRoom("Evidence Storehouse", "tagged food, confiscated water, property bags, and labels pretending custody equals justice", 42, Faction.ARBITES, new String[]{"confiscated trinket","permit scrap","sealed water ration","trade chit"}, new char[]{'b','A','q'}));
        list.add(themedRoom("Contraband Warehouse", "barred product cages, sealed weapons crates, chem lockers, and a clerk with keys like teeth", 48, Faction.ARBITES, new String[]{"ammo scrap","machine scrap","chem vial","rusted blade"}, new char[]{'b','A','N'}));
        list.add(themedRoom("Public Complaint Counter", "armored glass, forms, queue rails, and a window designed to make rage speak politely", 24, Faction.ARBITES, new String[]{"permit scrap","citation slip","ink cartridge"}, new char[]{'q','A','h'}));
        list.add(themedRoom("Holding Cell Row", "steel bunks, floor drains, numbered doors, and wall scratches filed under confession", 20, Faction.ARBITES, new String[]{"cloth scraps","bone charm","sealed water ration"}, new char[]{'A','c','u'}));
        list.add(themedRoom("Interrogation Room", "one table, two chairs, bright lamp, drain, and the architectural belief that truth fears discomfort", 18, Faction.ARBITES, new String[]{"permit scrap","cloth scraps"}, new char[]{'A','T','q'}));
        list.add(themedRoom("Checkpoint Armory", "baton racks, ammo cabinets, riot shields, and a red line civilians are expected to understand", 34, Faction.ARBITES, new String[]{"ammo scrap","shock cell","machine parts"}, new char[]{'A','b','N'}));
    }

    static void addInnRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("INN Editorial Bullpen", "ranks of desks, copy spikes, censor marks, hot recaf, and reporters converting bloodshed into printable civic posture", 38, Faction.INN, new String[]{"Fresh INN newspaper","Primer slate","Recaf tin","Blank form packet"}, new char[]{'q','b','T','N'}));
        list.add(themedRoom("News Printing Room", "press beds, ink drums, paper rolls, cutter tables, and machine noise hammering the daily shape of public truth", 52, Faction.INN, new String[]{"Fresh INN newspaper","Old INN newspaper","Ink cartridge","Warehouse inventory tag bundle"}, new char[]{'N','b','q','1'}));
        list.add(themedRoom("Imperial Broadcast Center", "vox masts, pict-screen booths, script lecterns, censor relays, and broadcasters smiling into sanctioned catastrophe", 44, Faction.INN, new String[]{"Radio set","Public pict-screen tube","Fresh INN newspaper","Data spike"}, new char[]{'N','q','T','1'}));
        list.add(themedRoom("Circulation Dispatch Office", "route ledgers, delivery satchels, vending-machine keys, and bundled newspapers sorted by who is allowed to know what", 48, Faction.INN, new String[]{"Fresh INN newspaper","Yesterday's INN newspaper","Trade chit","Water guild token"}, new char[]{'b','q','T','1'}));
        list.add(themedRoom("Reporter Locker Room", "press coats, broken pict slates, old shoes, meal tins, and the smell of people paid to walk toward explosions", 34, Faction.INN, new String[]{"Plain civilian coat","Used food tin","Fresh INN newspaper","Recaf tin"}, new char[]{'c','s','q'}));
        list.add(themedRoom("Censor Review Office", "redacted copy, sealed drawers, keyed waste bins, and a private desk where stories go to become safer lies", 32, Faction.INN, new String[]{"Blank form packet","Data spike","Fresh INN newspaper","Administratum stamp matrix"}, new char[]{'q','b','X'}));
        list.add(themedRoom("Archive Morgue of Old Papers", "bound editions, failed headlines, seven-day trash bundles, and paper decay cataloged more carefully than most citizens", 56, Faction.INN, new String[]{"Old INN newspaper","Yesterday's INN newspaper","Useless paper mush","Primer slate"}, new char[]{'b','q','o'}));
        list.add(themedRoom("Public News Dispenser Alcove", "newspaper vending cabinets, radio handsets, pict-screen adverts, and an approved price for knowing slightly more", 40, Faction.INN, new String[]{"Fresh INN newspaper","Radio set","Used food tin","Trade chit"}, new char[]{'1','T','q','b'}));
    }

    static void addAdministratumRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("Clerk Dormitory Ledger Cell", "narrow bunks folded between filing cages, grey blankets, and dreams stamped invalid", 30, Faction.ADMINISTRATUM, new String[]{"permit scrap","cloth scraps","sealed water ration"}, new char[]{'h','c','q'}));
        list.add(themedRoom("Stamp-Clerk Canteen", "thin soup, recaff urns, form talk, and benches worn by generations of minor authority", 36, Faction.ADMINISTRATUM, new String[]{"cheap meal tin","sealed water ration","ink cartridge"}, new char[]{'T','q','h'}));
        list.add(themedRoom("Ration File Storehouse", "food claims, water tokens, emergency allotments, and paperwork proving hunger was scheduled", 46, Faction.ADMINISTRATUM, new String[]{"ration ticket","emergency ration","sealed water ration","permit stub"}, new char[]{'b','h','q'}));
        list.add(themedRoom("Form Product Warehouse", "blank forms, ink barrels, seal ribbons, spare queue rails, and crates no one may open without Form 19", 50, Faction.ADMINISTRATUM, new String[]{"blank form packet","ink cartridge","permit scrap","trade chit"}, new char[]{'b','h','N'}));
        list.add(themedRoom("Permit Appeal Counter", "a small counter where hope is made to wait until it becomes paper dust", 28, Faction.ADMINISTRATUM, new String[]{"permit stub","petition scrap","ink cartridge"}, new char[]{'q','h','b'}));
        list.add(themedRoom("Dead-File Vault", "old names, failed claims, sealed shelves, and the soft rot of lives completed administratively", 42, Faction.ADMINISTRATUM, new String[]{"permit scrap","identity slate","cloth scraps"}, new char[]{'h','b','q'}));
        list.add(themedRoom("Queue Pen", "rail mazes, tired benches, number lamps, and a social machine for turning time into submission", 22, Faction.ADMINISTRATUM, new String[]{"ration ticket","permit stub","cheap trinket"}, new char[]{'h','q','n'}));
        list.add(themedRoom("Archive Maintenance Niche", "rolling ladders, dust filters, cable trays, and a machine that indexes misery", 48, Faction.ADMINISTRATUM, new String[]{"filter cartridge","wire bundle","cogitator shard"}, new char[]{'N','h','q'}));
    }

    static void addGuardRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("Brutalist Guard Barracks", "rows of identical bunks, steel lockers, weapon racks, and walls designed to survive morale", 32, Faction.IMPERIAL_GUARD, new String[]{"guard ration","sealed water ration","ammo scrap","boot knife"}, new char[]{'M','c','b','q'}));
        list.add(themedRoom("Field Mess Hall", "folding tables, ration drums, recaff steam, and boots lined under benches like a warning", 42, Faction.IMPERIAL_GUARD, new String[]{"guard ration","cheap meal tin","sealed water ration"}, new char[]{'T','M','b'}));
        list.add(themedRoom("Ration Storehouse", "stacked guard rations, water cans, inventory slates, and quartermaster suspicion", 54, Faction.IMPERIAL_GUARD, new String[]{"guard ration","sealed water ration","fuel scrap","trade chit"}, new char[]{'b','M','q'}));
        list.add(themedRoom("Munition Warehouse", "ammo crates, las-cell racks, warning stripes, and a smell of oil disciplined into rows", 46, Faction.IMPERIAL_GUARD, new String[]{"ammo scrap","las-cell scrap","machine parts"}, new char[]{'b','M','N'}));
        list.add(themedRoom("Quartermaster Counter", "issue window, requisition ledgers, returned kit bins, and a professional hatred of missing socks", 36, Faction.IMPERIAL_GUARD, new String[]{"supply docket","trade chit","guard ration","ammo scrap"}, new char[]{'q','M','b'}));
        list.add(themedRoom("Aid Station", "field cot, red-marked crates, blood cloth, and pain triaged by operational importance", 28, Faction.IMPERIAL_GUARD, new String[]{"bandage roll","sealed water ration","stimulant ampoule"}, new char[]{'c','u','M'}));
        list.add(themedRoom("Drill Hall", "painted lines, impact-scored walls, shouted orders, and the geometry of obedience", 20, Faction.IMPERIAL_GUARD, new String[]{"ammo scrap","cloth scraps","guard token"}, new char[]{'M','q','A'}));
        list.add(themedRoom("Sandbag Security Post", "overlapping sight lines, stacked sandbags, helmet hooks, and a kill zone with paperwork", 30, Faction.IMPERIAL_GUARD, new String[]{"ammo scrap","sealed water ration","wire bundle"}, new char[]{'M','A','b'}));
    }

    static void addMechanicusRooms(ArrayList<RoomProfile> list, ZoneType z){
        list.add(themedRoom("Augmetic Rest Cell", "a human-unfriendly recess of angled metal, standing restraint rails, cable ports, and a cot added only as a concession to biology", 34, Faction.MECHANICUS, new String[]{"wire bundle","cloth scraps","sacred oil dreg"}, new char[]{'R','c','N'}));
        list.add(themedRoom("Nutrient Reclamation Galley", "tube-fed ration pumps, bitter electrolyte tanks, and a dining arrangement optimized for maintenance servitors", 42, Faction.MECHANICUS, new String[]{"nutrient paste","sealed water ration","filter cartridge"}, new char[]{'Y','N','T'}));
        list.add(themedRoom("Blessed Lubricant Storehouse", "oil phials, coolant cans, feedstock barrels, and purity tags hanging from every valve", 56, Faction.MECHANICUS, new String[]{"sacred oil dreg","hot oil phial","filter cartridge","fuel scrap"}, new char[]{'b','R','N'}));
        list.add(themedRoom("Component Warehouse", "sorted machine limbs, cable bundles, cogitator plates, and jagged shelving hostile to soft hands", 60, Faction.MECHANICUS, new String[]{"machine parts","wire bundle","cogitator shard","construction supplies"}, new char[]{'b','N','R','J'}));
        list.add(themedRoom("Inspection Counter Shrine", "a service counter disguised as an altar, where devices are judged more gently than people", 46, Faction.MECHANICUS, new String[]{"machine scrap","trade chit","sacred oil dreg"}, new char[]{'q','R','N'}));
        list.add(themedRoom("Diagnostic Bay", "auspex arms, warning lumens, suspended cables, and floor plates cut for machines instead of feet", 52, Faction.MECHANICUS, new String[]{"cogitator shard","wire bundle","sample vial"}, new char[]{'R','N','Z'}));
        list.add(themedRoom("Cable Chapel", "hanging conduit, red lamps, incense vents, and devotional sockets humming under the walls", 38, Faction.MECHANICUS, new String[]{"wire bundle","sacred oil dreg","wax scrap"}, new char[]{'c','R','N'}));
        list.add(themedRoom("Servo-Skull Rookery", "charging perches, tiny tool nests, data droppings, and the anxious flutter of loyal dead hardware", 44, Faction.MECHANICUS, new String[]{"cogitator shard","machine parts","wire bundle"}, new char[]{'R','q','N'}));
        if(z==ZoneType.MECHANICUS_RELIC_DUCT) list.add(themedRoom("Relic Duct Access Shrine", "ancient actuator teeth, sealed access wheels, data frost, and warnings written for people already gone", 50, Faction.MECHANICUS, new String[]{"cogitator shard","machine parts","relic tag"}, new char[]{'R','M','N'}));
    }

    static void addNobleRooms(ArrayList<RoomProfile> list, ZoneType z){
        list.add(themedRoom("Lavish Servitor-Tended Dormitory", "fur carpeting, curtained sleep alcoves, soft lamps, polished brass, and servants whose comfort is not considered part of the design", 32, Faction.NOBLE, new String[]{"fine ration","laundered cloth","cheap trinket","trade chit"}, new char[]{'n','c','Q','b'}));
        list.add(themedRoom("Noble Kitchen Gallery", "copper heat tables, perfumed vents, imported spices, and knives cleaner than most citizens", 48, Faction.NOBLE, new String[]{"fine ration","sealed water ration","spice pinch","trade chit"}, new char[]{'T','Q','n','b'}));
        list.add(themedRoom("Fine Food Storehouse", "gene-locked pantries, silver tins, chilled water, and enough preserved luxury to start a riot", 60, Faction.NOBLE, new String[]{"fine ration","sealed water ration","silver ration tin","trade chit"}, new char[]{'b','n','Q'}));
        list.add(themedRoom("House Product Warehouse", "velvet-wrapped crates, spare uniforms, decorative weapons, and cargo marked with family cipher seals", 56, Faction.NOBLE, new String[]{"laundered cloth","cheap trinket","trade chit","permit form"}, new char[]{'b','n','A'}));
        list.add(themedRoom("Private Storefront Salon", "a discreet purchasing room where servants bargain for luxuries behind legal curtains", 34, Faction.NOBLE, new String[]{"trade chit","fine ration","permit form"}, new char[]{'T','Q','n'}));
        list.add(themedRoom("House Medicae Suite", "white tile, soft restraints, sealed instruments, and medicine stored behind pedigree", 26, Faction.NOBLE, new String[]{"bandage roll","stimulant ampoule","sample vial","sealed water ration"}, new char[]{'u','c','Q'}));
        list.add(themedRoom("Trophy Security Gallery", "display weapons, hidden shutters, portrait eyes, and a corridor that watches back", 24, Faction.NOBLE, new String[]{"cheap trinket","permit form","ammo scrap"}, new char[]{'A','n','Q'}));
        list.add(themedRoom("Servant Laundry Room", "pressed uniforms, perfume water, hidden bruises, and linens cleaner than the air outside", 42, Faction.NOBLE, new String[]{"laundered cloth","sealed water ration","soap stub"}, new char[]{'u','Y','n'}));
        if(z==ZoneType.SECTOR_GOVERNORS_MANSION) list.add(themedRoom("Audience Preparation Chamber", "mirrors, house banners, concealed guard slits, and etiquette sharpened into a weapon", 22, Faction.NOBLE, new String[]{"permit form","governor signet","fine ration"}, new char[]{'Q','A','n'}));
    }

    static void addRailRooms(ArrayList<RoomProfile> list, ZoneType z){
        list.add(themedRoom("Rail Worker Dormitory", "short bunks, alarm bells, soot blankets, and workers sleeping near freight they cannot afford", 36, Faction.HIVER, new String[]{"worker thermos","cloth scraps","sealed water ration"}, new char[]{'c','b','T'}));
        list.add(themedRoom("Platform Canteen", "hot tins, ticket talk, steam drafts, and bad coffee sold as civic infrastructure", 44, Faction.HIVER, new String[]{"cheap meal tin","sealed water ration","trade chit"}, new char[]{'T','q','b'}));
        list.add(themedRoom("Freight Food Storehouse", "ration crates, water barrels, cargo seals, and forklift scars near the floor", 54, Faction.HIVER, new String[]{"emergency ration","sealed water ration","supply docket"}, new char[]{'b','T','Y'}));
        list.add(themedRoom("Cargo Warehouse", "stacked product crates, cargo chains, inspection chalk, and goods moving faster than accountability", 60, Faction.HIVER, new String[]{"supply crate scrap","machine parts","wire bundle","trade chit"}, new char[]{'b','N','T'}));
        list.add(themedRoom("Ticket Storefront", "grimy windows, route boards, punch tools, and passengers calculating what they can leave behind", 36, Faction.HIVER, new String[]{"ticket stub","trade chit","permit scrap"}, new char[]{'q','T','n'}));
        list.add(themedRoom("Cargo Inspection Cage", "barred tables, seal knives, ledgers, and inspectors who can smell undeclared hope", 38, Faction.ARBITES, new String[]{"permit scrap","supply docket","confiscated trinket"}, new char[]{'A','b','q'}));
        list.add(themedRoom("Rail Maintenance Bay", "brake valves, signal lamps, oily tools, and machine noise dragged in from the line", 52, Faction.HIVER, new String[]{"machine parts","wire bundle","rusted tool","fuel scrap"}, new char[]{'N','R','T'}));
        list.add(themedRoom("Baggage Claim Cage", "unclaimed bags, parcel shelves, rope lanes, and small tragedies with luggage tags", 46, Faction.HIVER, new String[]{"cheap trinket","cloth scraps","trade chit","sealed water ration"}, new char[]{'b','q','T'}));
    }

    static void addSewerRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("Pump Service Chamber", "thick pipes, pressure wheels, warning paint, and water moving with institutional spite", 50, Faction.NONE, new String[]{"filter cartridge","machine parts","dirty water","wire bundle"}, new char[]{'N','v','Y'}));
        list.add(themedRoom("Filtration Gallery", "grate beds, sludge screens, dripping tanks, and a smell that has achieved seniority", 48, Faction.NONE, new String[]{"filter cartridge","dirty water","sump fungus"}, new char[]{'Y','v','m'}));
        list.add(themedRoom("Cistern Walkway", "black water below, narrow rails above, echoing drops, and too many places to fall", 32, Faction.NONE, new String[]{"dirty water","sealed water ration","rust scrap"}, new char[]{'v','m','N'}));
        list.add(themedRoom("Pipe Junction Storehouse", "spare valves, pipe clamps, emergency filters, and a maintenance inventory nobody trusts", 54, Faction.NONE, new String[]{"filter cartridge","machine parts","rusted tool","wire bundle"}, new char[]{'b','N','v'}));
        list.add(themedRoom("Fungus Food Niche", "edible mold racks, dripping heat, scavenger cuts, and spores with opinions", 44, Faction.SCAVENGER, new String[]{"sump fungus","dubious water","cloth scraps"}, new char[]{'m','b','Y'}));
        list.add(themedRoom("Drowned Maintenance Office", "desk legs in water, ruined logs, corroded keys, and old orders floating face-down", 40, Faction.NONE, new String[]{"permit scrap","rusted tool","filter cartridge"}, new char[]{'q','v','N'}));
        list.add(themedRoom("Sewer Camp Kitchen", "boiling pots, reclaimed tins, gutter smoke, and survival cooking with no witnesses", 38, Faction.SCAVENGER, new String[]{"dubious water","sump fungus","spoiled ration"}, new char[]{'T','m','B'}));
        list.add(themedRoom("Utility Storefront Niche", "a scavenger counter wedged beside pipes, selling filters, water, and lies about both", 36, Faction.SCAVENGER, new String[]{"filter cartridge","sealed water ration","trade chit"}, new char[]{'T','q','v'}));
    }

    static void addMutantRooms(ArrayList<RoomProfile> list, ZoneType z){
        list.add(themedRoom("Mutant Sleeping Hollow", "warm bedding, fungus mats, bone charms, and family marks scratched where outsiders should not look", 30, Faction.MUTANT, new String[]{"bone charm","fungal ration","cloth scraps"}, new char[]{'m','c','o'}));
        list.add(themedRoom("Boiling Pot Kitchen", "sump pots, fungus heaps, cracked bone tools, and something almost like hospitality", 38, Faction.MUTANT, new String[]{"fungal ration","dubious water","sump fungus"}, new char[]{'T','m','B'}));
        list.add(themedRoom("Fungus Food Store", "mold bundles, hide sacks, damp shelves, and guarded food that fights back softly", 48, Faction.MUTANT, new String[]{"fungal ration","sump fungus","dubious water"}, new char[]{'b','m','Y'}));
        list.add(themedRoom("Scrap Product Hoard", "bent metal, chewed straps, stolen fittings, and objects kept because they once mattered", 52, Faction.MUTANT, new String[]{"scrap bundle","machine scrap","bone tool"}, new char[]{'b','p','m'}));
        list.add(themedRoom("Mutant Trade Mat", "a hide mat of barter goods, warnings, teeth, and exchange customs that punish haste", 30, Faction.MUTANT, new String[]{"bone charm","scrap bundle","dubious water"}, new char[]{'T','m','o'}));
        list.add(themedRoom("Wound Licking Alcove", "boiled rags, fungus poultices, old blood, and medicine discovered under pressure", 26, Faction.MUTANT, new String[]{"bandage roll","fungal ration","cloth scraps"}, new char[]{'u','m','c'}));
        list.add(themedRoom("Bone Shrine", "old skulls, pipe idols, family signs, and a theology of being hated first", 22, Faction.MUTANT, new String[]{"bone charm","wax scrap","cloth scraps"}, new char[]{'c','o','m'}));
        if(z==ZoneType.MUTANT_SEWER_CAMP) list.add(themedRoom("Sump Barricade Room", "bone-and-pipe walls, watched drains, spear bundles, and territorial silence", 34, Faction.MUTANT, new String[]{"scrap spear bundle","bone tool","rust scrap"}, new char[]{'m','A','b'}));
    }

    static void addCultistRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("Cult Sleeper Crypt", "bedrolls around painted drains, candle smoke, hidden knives, and sleep guarded by heresy", 26, Faction.CULTIST, new String[]{"wax scrap","cloth scraps","tainted token"}, new char[]{'c','o','b'}));
        list.add(themedRoom("Ritual Kitchen", "boiling pots, blood gutters, burned spices, and a meal nobody describes plainly", 32, Faction.CULTIST, new String[]{"spoiled ration","wax scrap","tainted water jar"}, new char[]{'T','c','B'}));
        list.add(themedRoom("Offering Storehouse", "food tins, water jars, robes, knives, and gifts taken from people who stopped needing them", 44, Faction.CULTIST, new String[]{"spoiled ration","tainted token","rusted blade","cloth scraps"}, new char[]{'b','c','o'}));
        list.add(themedRoom("Contraband Shrine Warehouse", "sealed crates, wax marks, forbidden texts, and product inventory arranged as devotion", 46, Faction.CULTIST, new String[]{"tainted token","wax scrap","rusted blade","permit scrap"}, new char[]{'b','c','N'}));
        list.add(themedRoom("Whisper Counter", "a half-storefront for rumors, charms, illegal rites, and customers pretending not to know the price", 24, Faction.CULTIST, new String[]{"tainted token","trade chit","wax scrap"}, new char[]{'q','c','o'}));
        list.add(themedRoom("Blood Wash Room", "drains, rags, water jars, and cleanup treated as sacrament", 30, Faction.CULTIST, new String[]{"cloth scraps","tainted water jar","wax scrap"}, new char[]{'u','c','v'}));
        list.add(themedRoom("Hidden Knife Chapel", "knife scratches, gutter candles, whispering pipes, and faith doing its worst work indoors", 20, Faction.CULTIST, new String[]{"rusted blade","wax scrap","tainted token"}, new char[]{'c','o','b'}));
    }

    static void addTrashRooms(ArrayList<RoomProfile> list){
        list.add(themedRoom("Salvager Sleep Nest", "rags, glove piles, warm trash, and a sleeping hollow nobody admits is home", 34, Faction.SCAVENGER, new String[]{"cloth scraps","spoiled ration","bottle of dubious water"}, new char[]{'p','c','m'}));
        list.add(themedRoom("Trash Fire Kitchen", "burn barrels, scavenged tins, fungus scraps, and smoke doing seasoning work", 42, Faction.SCAVENGER, new String[]{"spoiled ration","sump fungus","fuel scrap"}, new char[]{'T','B','p'}));
        list.add(themedRoom("Scrap Food Storehouse", "half-buried ration crates, water bottles, mold sacks, and ownership enforced by proximity", 50, Faction.SCAVENGER, new String[]{"spoiled ration","sealed water ration","sump fungus"}, new char[]{'b','p','m'}));
        list.add(themedRoom("Salvage Warehouse", "sorted refuse, metal teeth, recycler grates, and useful junk promoted to inventory", 60, Faction.SCAVENGER, new String[]{"scrap bundle","machine scrap","wire bundle","cloth scraps"}, new char[]{'b','p','N'}));
        list.add(themedRoom("Junk Storefront", "a tarp counter of recovered goods, barter marks, and cheerful fraud by lantern light", 40, Faction.SCAVENGER, new String[]{"cheap trinket","scrap bundle","trade chit"}, new char[]{'T','p','q'}));
        list.add(themedRoom("Recycler Sluice", "grinding teeth, waste chutes, filter screens, and a machine that eats mistakes", 52, Faction.SCAVENGER, new String[]{"machine parts","filter cartridge","rust scrap"}, new char[]{'N','Y','p'}));
        list.add(themedRoom("Patchwork Clinic", "rags, boiled water, broken splints, and practical medicine without permission", 28, Faction.SCAVENGER, new String[]{"bandage roll","cloth scraps","dirty water"}, new char[]{'u','c','p'}));
    }

    static RoomProfile forZone(ZoneType z, Random r){
        ArrayList<RoomProfile> list=new ArrayList<>();
        addInfrastructureRooms(list, z);
        // Closets remain universal, but their feature table is zone-colored after selection.
        list.add(new RoomProfile("Maintenance closet", "zone-local tools, pipes, wiring, and work debris shaped by the surrounding institution", 44, Faction.NONE, new String[]{"machine scrap","wire bundle","rusted tool","filter cartridge"}, new char[]{'N','p','R'}));
        list.add(new RoomProfile("Dead-end storage closet", "a short local storage pocket holding whatever this faction forgets, hides, or hoards", 48, Faction.NONE, new String[]{"supply crate scrap","trade chit","machine parts","sealed water ration"}, new char[]{'b','p','N'}));
        if(z==ZoneType.MECHANICUS_FORGE_CLOISTER || z==ZoneType.MECHANICUS_RELIC_DUCT){
            list.add(new RoomProfile("Atmospheric Condenser Cell","Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware",44,Faction.MECHANICUS,new String[]{"water bottle","filter cartridge","machine parts"},new char[]{'Y','N','q'}));
            list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
            list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
            list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
            list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
            list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
            list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
            list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
        }
        EconomicGenerationBiasAuthority.weightRoomSelection(z, list);
        RoomProfile selected = list.get(r.nextInt(list.size()));
        selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
        return selected;
    }
    String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
    char contentSymbol(Random r){ return contents[r.nextInt(contents.length)]; }
}

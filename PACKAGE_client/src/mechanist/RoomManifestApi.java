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

class RoomManifestApi {
    private RoomManifestApi() {}

    static java.util.List<StampedModuleSpec> structuredModulesFor(ZoneType z){
        ArrayList<StampedModuleSpec> out = new ArrayList<>();
        switch(z){
            case IMPERIAL_GUARD_BILLET:
                out.add(module("Guard Barracks And Drill Spine","DRILL",18,3,5,5,1,-7,
                    spec("BARRACKS","Stamped Brutalist Guard Barracks","architectural barracks stamp: ordered hard bunks, lockers, weapons discipline, square corners, and brutalist military utility",48,Faction.IMPERIAL_GUARD,new String[]{"guard ration tin","fatigue cloth","las-cell crate"},new char[]{'c','s','!'}),
                    spec("TRAINING","Stamped Guard Drill Room","training stamp with lanes, benches, target marks, and enough open floor for shouted correction",34,Faction.IMPERIAL_GUARD,new String[]{"training manual","fatigue cloth","dummy round"},new char[]{'!','b'})));
                out.add(module("Guard Field Cafeteria To Kitchen","CAFETERIA",20,3,6,5,3,7,
                    spec("CAFETERIA","Stamped Guard Field Cafeteria","long thin cafeteria spine: ordered tables and benches in ranks, trash bins, ration counter, and military throughput over comfort",54,Faction.IMPERIAL_GUARD,new String[]{"guard ration tin","sealed water ration","meal tray"},new char[]{'T','b','N'}),
                    spec("KITCHEN","Stamped Guard Ration Kitchen","rear ration kitchen with bulk tins, heat plates, wash basins, and a service counter feeding the mess line",46,Faction.IMPERIAL_GUARD,new String[]{"fuel scrap","guard ration tin","dirty water"},new char[]{'q','N','F'})));
                break;
            case NOBLE_SERVICE_SPINE: case SECTOR_GOVERNORS_MANSION:
                out.add(module("Noble Apartment And Service Suite","SALON",18,3,6,6,1,-7,
                    spec("APARTMENT","Stamped Noble Apartment Suite","lavish apartment stamp with fur carpeting, soft furniture, servant bells, locked wardrobes, and obscene survival odds",64,Faction.NOBLE,new String[]{"fine meal tin","perfumed cloth","house token"},new char[]{'q','c','s'}),
                    spec("SANITATION","Stamped Noble Bath And Laundry","private bath/laundry room with polished basins, warm tiles, clean water, and servants made invisible by design",48,Faction.NOBLE,new String[]{"perfumed cloth","fine water ration","soap tablet"},new char[]{'u','N','q'})));
                out.add(module("Noble Dining Gallery And Kitchen","CAFETERIA",20,3,6,5,3,7,
                    spec("CAFETERIA","Stamped Noble Dining Gallery","controlled dining gallery with aligned servant benches, private side tables, porcelain bins, and discreet service lanes",58,Faction.NOBLE,new String[]{"fine meal tin","silver ration chit","sealed water ration"},new char[]{'T','b','q'}),
                    spec("KITCHEN","Stamped Noble Kitchen Gallery","bright kitchen gallery of brass heat plates, locked pantries, servant counters, and supply discipline disguised as elegance",52,Faction.NOBLE,new String[]{"fine meal tin","spice packet","clean water flask"},new char[]{'q','u','N'})));
                break;
            case MECHANICUS_FORGE_CLOISTER: case MECHANICUS_RELIC_DUCT:
                out.add(module("Mechanist Collegia Nutrient And Diagnostic Spine","DATA_STACK",18,3,5,5,1,-7,
                    spec("CAFETERIA","Stamped Mechanist Collegia Nutrient Galley","anti-human nutrient galley: jagged dispensers, standing brackets, feed tubes, cable gutters, and no concession to comfort",50,Faction.MECHANIST_COLLEGIA,new String[]{"nutrient paste tube","sealed water ration","filter cartridge"},new char[]{'T','N','q'}),
                    spec("CLINIC","Stamped Mechanist Collegia Diagnostic Bay","diagnostic bay of hard tables, red lenses, hanging tools, and repair doctrine that treats flesh as warranty debt",48,Faction.MECHANIST_COLLEGIA,new String[]{"machine parts","filter cartridge","logic Engine shard"},new char[]{'u','R','q'})));
                out.add(module("Mechanist Collegia Data Chapel And Rest Cells","DATA_STACK",20,3,5,5,3,7,
                    spec("LIBRARY","Stamped Data-Chapel Library","learning chamber of data stacks, prayer terminals, cable lecterns, and knowledge stored as obedience",48,Faction.MECHANIST_COLLEGIA,new String[]{"data slate","logic Engine shard","ritual scrap"},new char[]{'l','R','q'}),
                    spec("DORMITORY","Stamped Augmetic Rest Cells","rest-cell row where bodies dock in narrow recesses beside tool arms, cable loops, and devotional hazard signage",42,Faction.MECHANIST_COLLEGIA,new String[]{"machine oil smear","cloth scraps","wire bundle"},new char[]{'c','s','R'})));
                break;
            case ARBITES_PRECINCT_EDGE:
                out.add(module("Civic Wardens Holding Cell Row","CELL_ROW",20,3,4,4,1,-7,
                    spec("SECURITY","Stamped Holding Cell Row","cell-row stamp: bars, benches, floor drains, camera angles, and architecture designed to make procedure feel inevitable",42,Faction.CIVIC_WARDENS,new String[]{"key chit","cloth scraps","citation token"},new char[]{'X','b'}),
                    spec("CLINIC","Stamped Precinct Wound Room","small processing aid room for people injured by law before paperwork has decided why",34,Faction.CIVIC_WARDENS,new String[]{"bandage roll","sealed water ration","citation token"},new char[]{'u','c'}),
                    spec("STOREFRONT","Stamped Complaint Counter","barred public counter for accusations, notices, fines, and the administrative conversion of distress",36,Faction.CIVIC_WARDENS,new String[]{"citation token","permit slip","data slate"},new char[]{'T','X'})));
                break;
            case ADMINISTRATUM_ARCHIVE:
                out.add(module("Civic Ledger Office Dead-File Library Spine","DATA_STACK",20,3,5,5,1,-7,
                    spec("LIBRARY","Stamped Dead-File Library","shelved archive stamp with index cages, dead files, damp ledgers, and knowledge arranged to deny use",46,Faction.CIVIC_LEDGER_OFFICE,new String[]{"old book","data slate","form packet"},new char[]{'l','X'}),
                    spec("TRAINING","Stamped Clerk Instruction Room","learning room for stamps, ledgers, denial scripts, and the sacred art of non-responsibility",30,Faction.CIVIC_LEDGER_OFFICE,new String[]{"training manual","ink vial","permit slip"},new char[]{'l','q'})));
                break;
            case NEUTRAL_CIVILIAN_FLOOR: case HAB_STACK:
                out.add(module("Civilian Apartment Creche Cluster","HAB",18,3,5,5,1,-7,
                    spec("APARTMENT","Stamped Civilian Apartment Cell","compact apartment block chamber with living corner, sleeping shelf, wash point, ration cupboard, and no spare air",42,Faction.HIVER,new String[]{"sealed water ration","cheap meal tin","cloth scraps"},new char[]{'q','c','u'}),
                    spec("DAYCARE","Stamped Hab Creche","daycare corner with tiny blankets, lesson scratches, ration bins, and adults pretending this is adequate",28,Faction.HIVER,new String[]{"toy cog","cheap meal tin","cloth scraps"},new char[]{'d','c','T'}),
                    spec("LEARNING","Stamped Block Learning Room","community learning room with slates, benches, warning posters, and badly copied arithmetic",30,Faction.HIVER,new String[]{"data slate","training manual","pencil stub"},new char[]{'l','q'})));
                break;
            case SUMP_MARKET:
                out.add(module("Sump Storefront Row","MARKET_ROW",22,3,5,5,1,-7,
                    spec("STOREFRONT","Stamped Sump Storefront Row","market row stamp of counters, awnings, scales, debt marks, water prices, and knives under tables",52,Faction.NONE,new String[]{"trade chit","water token","cheap trinket"},new char[]{'T','$','b'}),
                    spec("WAREHOUSE","Stamped Sump Product Warehouse","back-room product warehouse of cargo, repair goods, salvage, stolen products, and renamed merchandise",50,Faction.NONE,new String[]{"machine parts","supply crate scrap","trade chit"},new char[]{'b','N'}),
                    spec("DAYCARE","Stamped Market Creche Corner","child corner under vendor canvas where children learn counting, dodging, and debt",24,Faction.NONE,new String[]{"toy cog","cheap meal tin","cloth scraps"},new char[]{'d','T'})));
                break;
            default:
                break;
        }
        return out;
    }

    static java.util.List<StampedRoomSpec> requiredRoomManifest(ZoneType z){
        ArrayList<StampedRoomSpec> out = new ArrayList<>();
        switch(z){
            case IMPERIAL_GUARD_BILLET:
                add(out,"BARRACKS","Brutalist Guard Barracks","long hard-edged sleeping room: ordered cot ranks, footlockers, weapon checks, concrete-grey utility, and no softness not issued by command",45,Faction.IMPERIAL_GUARD,new String[]{"guard ration tin","las-cell crate","fatigue cloth"},new char[]{'c','s','!'});
                add(out,"CAFETERIA","Concord Guard Field Cafeteria","long thin mess corridor with bench lines, fixed tables, trash bins, a ration counter, and a rear kitchen hatch meant to move soldiers through quickly",50,Faction.IMPERIAL_GUARD,new String[]{"guard ration tin","sealed water ration","meal tray"},new char[]{'T','b','N','q'});
                add(out,"KITCHEN","Guard Ration Kitchen","brutalist ration preparation cell: heat plates, bulk tins, wash basins, and a service counter feeding the mess hall",44,Faction.IMPERIAL_GUARD,new String[]{"fuel scrap","guard ration tin","dirty water"},new char[]{'q','N','F'});
                add(out,"FOOD_STORE","Guard Ration Storehouse","stacked ration crates, water cans, expiry tags, and quartermaster seals",48,Faction.IMPERIAL_GUARD,new String[]{"guard ration tin","sealed water ration","field biscuit"},new char[]{'u','b','q'});
                add(out,"WAREHOUSE","Munition Product Warehouse","caged product warehouse for cells, parts, uniforms, sandbags, and weapon maintenance stock",36,Faction.IMPERIAL_GUARD,new String[]{"las-cell crate","machine parts","cloth scraps"},new char[]{'b','X'});
                add(out,"STOREFRONT","Quartermaster Issue Counter","barred issue counter where soldiers queue for kit and civilians are told to leave",40,Faction.IMPERIAL_GUARD,new String[]{"trade chit","repair token","guard requisition slip"},new char[]{'T','X'});
                add(out,"CLINIC","Guard Aid Station","field medicae corner with cots, splints, painkillers, and too many boots near the sterile line",35,Faction.IMPERIAL_GUARD,new String[]{"bandage roll","stimulant ampoule","sealed water ration"},new char[]{'u','c','q'});
                add(out,"TRAINING","Drill Training Room","rectangular training bay with marked lanes, weapons racks, ordered benches, and shouted doctrine baked into the walls",30,Faction.IMPERIAL_GUARD,new String[]{"training baton","fatigue cloth","las-cell crate"},new char[]{'!','b','T'});
                add(out,"LEARNING","Tactica Learning Closet","small instruction room of slates, target diagrams, casualty examples, and training manuals nobody admits to reading",26,Faction.IMPERIAL_GUARD,new String[]{"training manual","data slate","pencil stub"},new char[]{'l','q'});
                break;
            case NOBLE_SERVICE_SPINE: case SECTOR_GOVERNORS_MANSION:
                add(out,"APARTMENT","Lavish Noble Apartment Suite","fur-carpeted private suite with perfumed air, soft furniture, servant bells, and locked closets pretending not to be vaults",62,Faction.NOBLE,new String[]{"fine meal tin","perfumed cloth","trade chit"},new char[]{'q','c','s','u'});
                add(out,"DORMITORY","Bound Labor Automaton-Tended Retainer Dormitory","lavish by lower-sector standards: curtained servant bunks, polished trunks, house colors, and enough comfort to keep loyalty cheap",45,Faction.NOBLE,new String[]{"cloth scraps","fine water ration","house token"},new char[]{'c','s','q'});
                add(out,"CAFETERIA","Noble Service Dining Gallery","long controlled dining gallery with aligned benches for servants, private side tables, porcelain bins, and a hidden service counter to the kitchen",55,Faction.NOBLE,new String[]{"fine meal tin","sealed water ration","silver ration chit"},new char[]{'T','b','q','N'});
                add(out,"KITCHEN","Noble Kitchen Gallery","bright service kitchen with polished counters, staff lanes, heat cabinets, and controlled access to luxury food stores",52,Faction.NOBLE,new String[]{"fine meal tin","spice packet","clean water"},new char[]{'q','N','F'});
                add(out,"FOOD_STORE","Fine Food Storehouse","cold pantries, locked wine-less luxury ration cabinets, perfumed seals, and clean shelves that insult the rest of the arcology",56,Faction.NOBLE,new String[]{"fine meal tin","clean water","spice packet"},new char[]{'u','X','q'});
                add(out,"WAREHOUSE","House Product Warehouse","gene-marked product storage for garments, tools, imported luxuries, furniture parts, and emergency bribery",50,Faction.NOBLE,new String[]{"trade chit","luxury scrap","machine parts"},new char[]{'b','X'});
                add(out,"LIBRARY","Private Library and Tutor Room","soft-lit learning room with shelves, instruction slates, tutor desks, and history edited for comfort",42,Faction.NOBLE,new String[]{"data slate","old book","house lesson card"},new char[]{'l','q'});
                add(out,"DAYCARE","Noble Nursery and Manners Room","guarded child-care suite with soft flooring, etiquette diagrams, toy relics, and servants trained to be furniture",36,Faction.NOBLE,new String[]{"toy cog","cloth scraps","fine water ration"},new char[]{'d','c','T'});
                add(out,"SECURITY","Trophy Security Gallery","ornate security room where display weapons, cameras, and house guards turn vanity into surveillance",38,Faction.NOBLE,new String[]{"house token","weapon part","trade chit"},new char[]{'X','!'});
                break;
            case MECHANICUS_FORGE_CLOISTER: case MECHANICUS_RELIC_DUCT:
                add(out,"DORMITORY","Augmetic Rest Cell","anti-human rest slit with metal ribs, recharge hooks, prayer ports, and a cot designed by someone with a grudge against vertebrae",40,Faction.MECHANIST_COLLEGIA,new String[]{"wire bundle","cloth scraps","machine oil smear"},new char[]{'c','s','N'});
                add(out,"CAFETERIA","Nutrient Reclamation Galley","narrow feeding gallery of measured nutrient taps, waste bins, harsh counters, and devotional efficiency",45,Faction.MECHANIST_COLLEGIA,new String[]{"nutrient paste","sealed water ration","filter cartridge"},new char[]{'T','q','N'});
                add(out,"KITCHEN","Nutrient Processing Cell","machine-heavy food processor room where organic need is converted into metered slurry and quiet shame",42,Faction.MECHANIST_COLLEGIA,new String[]{"filter cartridge","machine parts","nutrient paste"},new char[]{'q','N','F'});
                add(out,"FOOD_STORE","Nutrient and Lubricant Storehouse","separated lockers for human nutrient paste, sacred lubricants, and fluids nobody should confuse",45,Faction.MECHANIST_COLLEGIA,new String[]{"sacred machine oil","filter cartridge","sealed water ration"},new char[]{'u','X','q'});
                add(out,"WAREHOUSE","Component Warehouse","jagged metal storage grid of bins, cable reels, component crates, and labels written for machines first",55,Faction.MECHANIST_COLLEGIA,new String[]{"machine parts","wire bundle","logic Engine shard"},new char[]{'b','N','q'});
                add(out,"STOREFRONT","Inspection Counter Shrine","counter-shrine where tools are issued, blessed, counted, and accused before use",46,Faction.MECHANIST_COLLEGIA,new String[]{"repair token","machine parts","ritual salt"},new char[]{'T','q'});
                add(out,"LIBRARY","Data-Chapel Learning Bay","learning chamber of red lenses, data-slates, catechism shelves, and machine-readable doctrine",44,Faction.MECHANIST_COLLEGIA,new String[]{"data slate","logic Engine shard","wire bundle"},new char[]{'l','q','N'});
                add(out,"TRAINING","Servo-Calibration Training Bay","hazardous training bay for augmetic calibration, tool rites, target servitors, and unacceptable softness correction",36,Faction.MECHANIST_COLLEGIA,new String[]{"machine parts","wire bundle","training cog"},new char[]{'!','N'});
                add(out,"SPECIALTY","Cable Chapel","hostile devotional utility chamber where cables descend like vines and every surface prefers steel to skin",48,Faction.MECHANIST_COLLEGIA,new String[]{"wire bundle","sacred machine oil","machine parts"},new char[]{'N','q','l'});
                break;
            case HAB_STACK: case NEUTRAL_CIVILIAN_FLOOR:
                add(out,"DORMITORY","Civilian Dormitory Bay","compact hab dormitory with worker cots, sinks, dressers, cabinets, laundry bundles, and the privacy of a rumor",38,Faction.HIVER,new String[]{"sealed water ration","cloth scraps","cheap trinket"},new char[]{'c','u','s'});
                add(out,"APARTMENT","Family Apartment Cluster","modest apartment cells: living corner, sleep nook, wash space, eating table, and enough personal debris to prove somebody lives here",42,Faction.HIVER,new String[]{"cheap meal tin","cloth scraps","household scrap"},new char[]{'q','c','u','s'});
                add(out,"CAFETERIA","Communal Cafeteria","long shared eating room with cheap benches, table rows, public bins, water queue marks, and a ration counter leading to a kitchen",44,Faction.HIVER,new String[]{"cheap meal tin","sealed water ration","ration wrapper"},new char[]{'T','b','N'});
                add(out,"KITCHEN","Communal Kitchen Block","shared cooking block with heat plates, stained basins, ration pots, and signs telling the poor to be orderly",40,Faction.HIVER,new String[]{"cheap meal tin","dirty water","fuel scrap"},new char[]{'q','N','F'});
                add(out,"FOOD_STORE","Civic Food Storehouse","local ration and water storage cage with token slits, stamped crates, and nervous locks",46,Faction.HIVER,new String[]{"sealed water ration","emergency ration","cheap meal tin"},new char[]{'u','X','q'});
                add(out,"WAREHOUSE","Civic Product Warehouse","household goods, civic supplies, repair parts, and products awaiting distribution or theft",44,Faction.HIVER,new String[]{"machine parts","cloth scraps","supply crate scrap"},new char[]{'b','N'});
                add(out,"STOREFRONT","Permit Storefront Row","counter row for permits, food chits, replacement tokens, and little civic humiliations",38,Faction.HIVER,new String[]{"trade chit","permit slip","cheap trinket"},new char[]{'T','$'});
                add(out,"LIBRARY","Public Learning Closet","thin public education room of battered slates, worker safety diagrams, and children learning scarcity early",25,Faction.HIVER,new String[]{"data slate","pencil stub","old book"},new char[]{'l','q'});
                add(out,"DAYCARE","Creche Daycare Room","crowded child-care room with cheap mats, low tables, food stains, and too few adults for too many futures",28,Faction.HIVER,new String[]{"toy cog","cloth scraps","cheap meal tin"},new char[]{'d','c','T'});
                break;
            case ARBITES_PRECINCT_EDGE:
                add(out,"BARRACKS","Civic Wardens Duty Barracks","severe duty dormitory with lockered bunks, armor stands, discipline plaques, and lighting chosen for interrogation practice",40,Faction.CIVIC_WARDENS,new String[]{"citation token","shock baton part","cloth scraps"},new char[]{'c','s','X'});
                add(out,"CAFETERIA","Precinct Mess Slot","narrow mess with fixed benches, monitored tables, trash bins, and a service slit that discourages lingering",38,Faction.CIVIC_WARDENS,new String[]{"ration tin","sealed water ration","citation token"},new char[]{'T','b','N'});
                add(out,"FOOD_STORE","Evidence Ration Storehouse","food and water cage separated from confiscated goods by labels, bars, and institutional suspicion",36,Faction.CIVIC_WARDENS,new String[]{"ration tin","sealed water ration","key chit"},new char[]{'u','X','q'});
                add(out,"WAREHOUSE","Contraband Product Warehouse","sealed warehouse of confiscated products, illegal tools, weapons, paperwork, and unanswered ownership questions",48,Faction.CIVIC_WARDENS,new String[]{"contraband packet","machine parts","trade chit"},new char[]{'b','X'});
                add(out,"STOREFRONT","Public Complaint Counter","barred public counter for accusations, fines, notices, and the conversion of suffering into forms",34,Faction.CIVIC_WARDENS,new String[]{"citation token","permit slip","data slate"},new char[]{'T','X'});
                add(out,"SECURITY","Holding Cell Row","security room of bars, benches, drain grates, and the smell of procedure becoming violence",35,Faction.CIVIC_WARDENS,new String[]{"key chit","cloth scraps","dirty water"},new char[]{'X','b'});
                add(out,"TRAINING","Compliance Training Room","training room with baton lanes, restraint hooks, case diagrams, and target silhouettes shaped like citizens",30,Faction.CIVIC_WARDENS,new String[]{"training baton","citation token","shock baton part"},new char[]{'!','X'});
                break;
            case ADMINISTRATUM_ARCHIVE:
                add(out,"DORMITORY","Clerk Dormitory Ledger Cell","clerk sleep cell with bunk slots, ink stains, personal forms, and fatigue stacked like paperwork",34,Faction.CIVIC_LEDGER_OFFICE,new String[]{"permit slip","cloth scraps","ink vial"},new char[]{'c','s','l'});
                add(out,"CAFETERIA","Stamp-Clerk Canteen","thin canteen with table lines, queue rails, food bins, and a counter that probably needs three signatures",36,Faction.CIVIC_LEDGER_OFFICE,new String[]{"cheap meal tin","sealed water ration","permit slip"},new char[]{'T','b','N'});
                add(out,"FOOD_STORE","Ration File Storehouse","food storehouse where ration crates and paperwork are both shelved as if edible",42,Faction.CIVIC_LEDGER_OFFICE,new String[]{"sealed water ration","cheap meal tin","form packet"},new char[]{'u','l','q'});
                add(out,"WAREHOUSE","Form Product Warehouse","warehouse for paper, stamps, slates, cabinets, rejected lives, and other civic Ledger Office products",50,Faction.CIVIC_LEDGER_OFFICE,new String[]{"form packet","data slate","ink vial"},new char[]{'b','l'});
                add(out,"STOREFRONT","Permit Appeal Counter","service counter with queue rails and a tactical architecture of delay",35,Faction.CIVIC_LEDGER_OFFICE,new String[]{"permit slip","trade chit","stamp token"},new char[]{'T','X'});
                add(out,"LIBRARY","Dead-File Library Vault","record library of shelves, dead files, index plates, and knowledge arranged to prevent use",42,Faction.CIVIC_LEDGER_OFFICE,new String[]{"old book","data slate","form packet"},new char[]{'l','X'});
                add(out,"TRAINING","Clerk Instruction Room","learning room for stamps, ledgers, denial scripts, and the sacred art of not being responsible",26,Faction.CIVIC_LEDGER_OFFICE,new String[]{"training manual","ink vial","permit slip"},new char[]{'l','q'});
                break;
            case SUMP_MARKET:
                add(out,"DORMITORY","Market Stall Sleeper Row","sleeping stalls behind curtains, crates, and merchandise; home and storefront share the same bad air",34,Faction.NONE,new String[]{"cloth scraps","cheap meal tin","trade chit"},new char[]{'c','b','T'});
                add(out,"CAFETERIA","Sump Food Court Corridor","long eating lane with vendor benches, bins, counters, steam, debt marks, and arguments priced by the bowl",42,Faction.NONE,new String[]{"cheap meal tin","dirty water","trade chit"},new char[]{'T','b','N'});
                add(out,"FOOD_STORE","Sump Market Food Storehouse","guarded edible stock: fungus crates, water barrels, ration tins, and the faint economics of poisoning",45,Faction.NONE,new String[]{"cheap meal tin","sealed water ration","fungus food"},new char[]{'u','b','q'});
                add(out,"WAREHOUSE","Sump Product Warehouse","mixed cargo warehouse of repair goods, salvage, stolen products, and merchandise that changed names twice",50,Faction.NONE,new String[]{"machine parts","supply crate scrap","trade chit"},new char[]{'b','N'});
                add(out,"STOREFRONT","Barter Storefront Row","front-facing vendor counters, awnings, crates, scales, water prices, and knives hidden under tables",48,Faction.NONE,new String[]{"trade chit","cheap trinket","water token"},new char[]{'T','$','b'});
                add(out,"DAYCARE","Market Creche Corner","improvised child corner under awnings where children learn counting, dodging, and debt",22,Faction.NONE,new String[]{"toy cog","cheap meal tin","cloth scraps"},new char[]{'d','T'});
                break;
            default:
                addDefault(out, z);
                break;
        }
        return out;
    }

    private static StampedModuleSpec module(String name, String dress, int len, int cw, int rw, int rh, int preferredSide, int laneOffset, StampedRoomSpec... rooms){
        return new StampedModuleSpec(name, dress, len, cw, rw, rh, preferredSide, laneOffset, rooms);
    }
    private static StampedRoomSpec spec(String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){
        return new StampedRoomSpec(kind, name, desc, chance, faction, loot, contents);
    }

    private static void addDefault(ArrayList<StampedRoomSpec> out, ZoneType z){
        Faction f = factionFor(z);
        add(out,"DORMITORY",z.label+" Dormitory Variant","zone-specific habitation room shaped by "+z.descriptor+"; this is not a generic dormitory but the local answer to sleep, crowding, and control",34,f,new String[]{"cloth scraps","sealed water ration","cheap trinket"},new char[]{'c','s'});
        add(out,"CAFETERIA",z.label+" Eating Hall Variant","zone-specific cafeteria or mess: long eating surfaces, benches, refuse bins, and a service counter adapted to the faction occupying this space",36,f,new String[]{"cheap meal tin","sealed water ration","dirty water"},new char[]{'T','b','N'});
        add(out,"FOOD_STORE",z.label+" Food Storehouse","food, water, and ration storage distinct from product warehousing; shelves and crates reflect the local faction economy",40,f,new String[]{"sealed water ration","emergency ration","cheap meal tin"},new char[]{'u','b','q'});
        add(out,"WAREHOUSE",z.label+" Product Warehouse","non-food product storage: salvage, tools, cargo, components, contraband, or manufactured stock depending on the zone",42,f,new String[]{"machine parts","supply crate scrap","trade chit"},new char[]{'b','N'});
        add(out,"STOREFRONT",z.label+" Storefront or Issue Counter","local exchange surface where goods, permissions, demands, or threats pass across a counter",36,f,new String[]{"trade chit","permit slip","cheap trinket"},new char[]{'T','$'});
        add(out,"CLINIC",z.label+" Care Room Variant","local care space: clinic, wound alcove, aid post, medicae suite, or equivalent survival room",30,f,new String[]{"bandage roll","sealed water ration","cloth scraps"},new char[]{'u','c'});
        add(out,"MACHINERY",z.label+" Work Machinery Room","functional machine room with local production tools, repair benches, power leads, and enough moving parts to justify assigned personnel",36,f,new String[]{"machine parts","wire bundle","filter cartridge"},new char[]{'R','N','q'});
        add(out,"LOGISTICS",z.label+" Logistics Stock Room","room for the faction's actual stock movement: crates, food issue, water cans, paper slips, and product awaiting use or theft",38,f,new String[]{"supply crate scrap","sealed water ration","trade chit"},new char[]{'b','u','N'});
        add(out,"SHRINE",z.label+" Devotional or Morale Room","local shrine, morale corner, chapel niche, gang icon room, or equivalent ideological anchor for the people housed nearby",28,f,new String[]{"candle stub","cloth scraps","cheap trinket"},new char[]{'h','n','T'});
        add(out,"TRAINING",z.label+" Training or Social Room","space for doctrine, lessons, intimidation, ritual, play, or practice depending on who owns the zone",25,f,new String[]{"data slate","training manual","cheap trinket"},new char[]{'!','l'});
    }

    private static Faction factionFor(ZoneType z){
        switch(z){
            case GANGER_TURF: return Faction.BANDIT;
            case ARBITES_PRECINCT_EDGE: return Faction.CIVIC_WARDENS;
            case MECHANICUS_RELIC_DUCT: case MECHANICUS_FORGE_CLOISTER: return Faction.MECHANIST_COLLEGIA;
            case MUTANT_WARRENS: case MUTANT_SEWER_CAMP: return Faction.MUTANT;
            case CULTIST_SEWER_CAMP: return Faction.CULTIST;
            case ADMINISTRATUM_ARCHIVE: return Faction.CIVIC_LEDGER_OFFICE;
            case IMPERIAL_GUARD_BILLET: return Faction.IMPERIAL_GUARD;
            case NOBLE_SERVICE_SPINE: case SECTOR_GOVERNORS_MANSION: return Faction.NOBLE;
            default: return Faction.NONE;
        }
    }
    private static void add(ArrayList<StampedRoomSpec> out, String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){ out.add(new StampedRoomSpec(kind,name,desc,chance,faction,loot,contents)); }
}

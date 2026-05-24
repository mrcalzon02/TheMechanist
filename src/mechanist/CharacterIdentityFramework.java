package mechanist;

import java.util.*;

class CharacterCreationAudit {
    final int candidateCount;
    final int firstNameCount;
    final int lastNameCount;
    final int singularNameCount;
    final int minStat;
    final int maxStat;
    final int highBands;
    final int lowBands;
    final int invalidCandidates;
    CharacterCreationAudit(int candidateCount, int firstNameCount, int lastNameCount, int singularNameCount, int minStat, int maxStat, int highBands, int lowBands, int invalidCandidates) {
        this.candidateCount=candidateCount; this.firstNameCount=firstNameCount; this.lastNameCount=lastNameCount; this.singularNameCount=singularNameCount; this.minStat=minStat; this.maxStat=maxStat; this.highBands=highBands; this.lowBands=lowBands; this.invalidCandidates=invalidCandidates;
    }
    String summaryLine(){ return "candidates=" + candidateCount + " names=" + firstNameCount + "/" + lastNameCount + "/" + singularNameCount + " statRange=" + minStat + "-" + maxStat + " invalid=" + invalidCandidates; }
    String toLogBlock(){ return summaryLine() + " highSlots=" + highBands + " lowSlots=" + lowBands + " rule=bounded deterministic spread not independent 4-11 spam-rolls"; }
}

class CharacterCreationAuditApi {
    static CharacterCreationAudit audit(java.util.List<Candidate> candidates) {
        int min=99, max=-99, invalid=0, high=0, low=0, count = candidates == null ? 0 : candidates.size();
        if (candidates != null) for (Candidate c : candidates) {
            if (c == null || c.stats == null || c.stats.isEmpty() || c.name == null || c.name.isBlank()) { invalid++; continue; }
            int localHigh = 0, localLow = 0;
            for (int v : c.stats.values()) { min=Math.min(min,v); max=Math.max(max,v); if(v>=9) localHigh++; if(v<=5) localLow++; }
            if (localHigh < 2 || localLow < 2) invalid++;
            high += localHigh; low += localLow;
        }
        if (min == 99) min = 0; if (max == -99) max = 0;
        return new CharacterCreationAudit(count, CharacterCreationAuthority.IMPERIAL_FIRST_NAMES.length, CharacterCreationAuthority.IMPERIAL_LAST_NAMES.length, CharacterCreationAuthority.IMPERIAL_SINGULAR_NAMES.length, min, max, high, low, invalid);
    }
}

class CharacterCreationAuthority {
    static final int MAX_PLAYER_NAME_LENGTH = 32;

    static final String[] IMPERIAL_MALE_NAMES = {
        "Kessel", "Brann", "Toll", "Rusk", "Demer", "Joric", "Orro", "Mathiel",
        "Severin", "Lucien", "Marcell", "Hadrian", "Varro", "Drusus", "Quint", "Cyran",
        "Gavrel", "Narcus", "Dorn", "Krail", "Venn", "Ferrum", "Lox", "Rho",
        "Thale", "Calder", "Vossian", "Iosef", "Bastian", "Malkor", "Erem", "Silas",
        "Acastus", "Aderic", "Alaric", "Amon", "Andren", "Anselm", "Anton", "Arctus",
        "Aurel", "Aurelian", "Balef", "Bardane", "Baruch", "Benedikt", "Bors", "Brennus",
        "Caius", "Caldor", "Cassian", "Castor", "Cavian", "Cedric", "Clemens", "Corvin",
        "Cyrillus", "Dacien", "Danton", "Decimus", "Dieder", "Domin", "Dorian", "Elias",
        "Ephram", "Erasmus", "Erdan", "Eudox", "Fabian", "Faustus", "Ferrian", "Fisk",
        "Gallus", "Garran", "Gervase", "Gideon", "Goran", "Graccus", "Gregor", "Hadrak",
        "Harkon", "Helbrecht", "Iago", "Ignace", "Ilarion", "Isidor", "Janus", "Jareth",
        "Jonan", "Jorvan", "Kaeso", "Karn", "Kastus", "Kordian", "Lazlo", "Leontes",
        "Lethold", "Lorcan", "Lothar", "Lucan", "Magnus", "Malchior", "Marius", "Markov",
        "Matthias", "Mordek", "Nestor", "Niklas", "Octavian", "Orlan", "Osric", "Otho",
        "Pavel", "Petron", "Phocas", "Quillon", "Quintus", "Rafen", "Ralk", "Remus",
        "Roderic", "Rufus", "Sabastian", "Sargon", "Sava", "Sebek", "Simeon", "Solomon",
        "Stellan", "Tavian", "Tiber", "Tolliver", "Trask", "Ulric", "Ursan", "Valen",
        "Valerian", "Vander", "Varric", "Vaylen", "Vesper", "Viktor", "Vortan", "Wulfric",
        "Xavian", "Zadok", "Zerek", "Zephon"
    };
    static final String[] IMPERIAL_FEMALE_NAMES = {
        "Morda", "Isca", "Lume", "Sable", "Mara", "Nera", "Sama", "Verena",
        "Miriya", "Octavia", "Kasta", "Cassia", "Tavia", "Alecto", "Sibyl", "Sabine",
        "Ilyra", "Ephrael", "Helika", "Solenne", "Juno", "Renata", "Adelpha", "Adira",
        "Aeliana", "Agatha", "Albina", "Alia", "Amalia", "Anika", "Annora", "Arabella",
        "Ariadne", "Astra", "Aurelia", "Avia", "Beata", "Berenice", "Brigitta", "Calista",
        "Camilla", "Carina", "Celestine", "Claudia", "Cordelia", "Corisande", "Daciana", "Dalia",
        "Delphine", "Dominica", "Drusilla", "Elara", "Elenia", "Elspeth", "Emilia", "Euphemia",
        "Evadne", "Fausta", "Felicita", "Flavia", "Genevra", "Gratia", "Helena", "Hestia",
        "Ianthe", "Idalia", "Imelda", "Iona", "Isolde", "Jocasta", "Jovia", "Junia",
        "Kallista", "Katryn", "Livia", "Liviana", "Lucilla", "Magdala", "Mariana", "Marta",
        "Meliora", "Meridia", "Mina", "Mirelle", "Morwen", "Nadia", "Natalia", "Nerissa",
        "Novia", "Odelia", "Ophelia", "Orphea", "Pallas", "Petra", "Philomena", "Prisca",
        "Quilla", "Rafaela", "Rosalind", "Sabella", "Salome", "Seraphina", "Severa", "Sidonia",
        "Sola", "Soraya", "Talia", "Tatiana", "Thalia", "Theodora", "Tiberia", "Valeria",
        "Vandora", "Veda", "Velora", "Verity", "Vespera", "Vionna", "Viridia", "Ysabel",
        "Zara", "Zelena", "Zora", "Agnella", "Arcadia", "Brielle", "Cymbeline", "Damaris",
        "Eirene", "Finella", "Galatea", "Honoria", "Justina", "Kerensa", "Laelia", "Maelia",
        "Nicasia", "Oriana", "Pelagia", "Quintilla", "Ravia", "Septima", "Tristana", "Ursula"
    };
    static final String[] IMPERIAL_NEUTRAL_GIVEN_NAMES = {
        "Vey", "Nix", "Harrow", "Eli", "Tollen", "Mordane", "Kessel-N", "Thrace",
        "Vale", "Klyne", "Strake", "Merrick", "Stern", "Prax", "Vhal", "Tyber",
        "Morne", "Cawlborn", "Ashen", "Aster", "Aurel-N", "Bale", "Basil", "Briar",
        "Cairn", "Canto", "Cinder", "Clem", "Corl", "Crowe", "Damar", "Dax",
        "Docket", "Dove", "Drail", "Drift", "Ebon", "Echo", "Eidolon", "Ember",
        "Fane", "Fen", "Fennic", "Flint", "Gable", "Galen", "Garnet", "Graves",
        "Grey", "Halix", "Hale", "Hallow", "Harper", "Hearth", "Hex", "Hollis",
        "Icon", "Iven", "Jade", "Joss", "Judex", "Kael", "Kane", "Kestrel",
        "Kip", "Kyr", "Lark", "Ledger", "Lent", "Lio", "Locke", "Lumen",
        "Mallow", "Mercy", "Mica", "Moss", "Nave", "Nero", "Nettle", "Noble",
        "Nova", "Omen", "Onyx", "Orison", "Pale", "Pax", "Pike", "Pillar",
        "Quarry", "Quill", "Raven", "Reed", "Reliq", "Riven", "Rune", "Sable-N",
        "Saffron", "Salt", "Scrip", "Seam", "Seneschal", "Sever", "Shade", "Shard",
        "Slate", "Solace", "Spar", "Spires", "Sterling", "Still", "Sump", "Tallow",
        "Tarn", "Teal", "Teeth", "Thorn", "Tithe", "Toll-N", "Trace", "Vane",
        "Vault", "Vellum", "Vesper-N", "Vigil", "Voss", "Warden", "Wick", "Winter",
        "Writ", "Yarrow", "Yven", "Zeal", "Zed", "Zinc", "Anchor", "Axiom",
        "Bastion", "Bracken", "Brass", "Candle", "Charnel", "Cobalt", "Corpus", "Credo",
        "Crux", "Datta", "Egress", "Ferric", "Gleam", "Gutter", "Helix", "Hush",
        "Ion", "Iron", "Junction", "Kite", "Lantern", "Machine"
    };
    static final String[] IMPERIAL_FAMILY_NAMES = {
        "Grime", "Bolt", "Ash", "Cog", "Wyrm", "Hale", "Sump", "Rivet",
        "Knell", "Drake", "Scab", "Voss", "Gant", "Coil", "Krail", "Moth",
        "Dorn", "Mathiel", "Orro", "Kasta", "Varn", "Sevrin", "Thrace", "Kord",
        "Hax", "Mordane", "Bast", "Lucarne", "Wolfe", "Grendel", "Stern", "Klyne",
        "Rath", "Beller", "Cawlborn", "Strake", "Danton", "Merrick", "Vale", "Kesselring",
        "Novak", "Praxis", "Heth", "Sorn", "Malvo", "Tarsk", "Baruch", "Quill",
        "Draik", "Serrat", "Calix", "Rhyne", "Gethsem", "Voln", "Rosk", "Tyber",
        "Morne", "Vhal", "Sarro", "Prax", "Aurelian", "Baro", "Bastion", "Blackwell",
        "Brasslock", "Candlewick", "Carmine", "Cathor", "Chervan", "Clast", "Cogsworn", "Crassus",
        "Cyr", "Dacron", "Damar", "Decant", "Draeg", "Ebonholt", "Eckhart", "Ferris",
        "Fulgor", "Gallowmere", "Galvan", "Gravus", "Greywake", "Grimhold", "Halbrecht", "Harth",
        "Hexley", "Irongate", "Jast", "Judicant", "Karnak", "Kastor", "Kordane", "Lask",
        "Lenthor", "Lucerne", "Magrail", "Malkov", "Marrow", "Mercator", "Morvain", "Nacell",
        "Neroch", "Ossuary", "Pall", "Parvan", "Praxor", "Quell", "Quire", "Rathbone",
        "Ravel", "Redmark", "Reliq", "Rend", "Rictus", "Rook", "Salvane", "Sanctor",
        "Sarrox", "Sept", "Serrek", "Shard", "Silt", "Skoria", "Straxis", "Sumpter",
        "Tarn", "Tarsus", "Thorn", "Tolliver", "Trigant", "Vandra", "Vask", "Vayne",
        "Vell", "Vhalis", "Vire", "Vorren", "Wolfram", "Writ", "Yorvan", "Zenth",
        "Zorren", "Ammadon", "Bellerose", "Cinderfall", "Cradle", "Dross", "Emberlain", "Fallow",
        "Grail", "Hox", "Ironmoth", "Kettle", "Lumenhold", "Misericord", "Nail", "Obrecht"
    };
    static final String[] IMPERIAL_SINGULAR_NAMES = {
        "Bitewire", "Lamprey", "Wet-Eye", "Moldback", "Three-Hand", "Splitjaw", "Candle Saint", "Whisper-Red",
        "The Listening Man", "Ash Grin", "Cutter Nox", "Red Skulk", "Iron Psalm", "Gutter Votive", "Saintless", "Black-Thrum",
        "Old Mercy", "Rag Prophet", "Grief-Token", "Brass Moth", "Scab-Orison", "Knife Psalm", "Sump Benediction", "Bonewake",
        "Grey Pilgrim", "Soot Martyr", "Hullrat", "Rust Choir", "Noonless", "Vox-Mutter", "Chainwake", "Drain Saint",
        "Rivet Mercy", "Tallow Eye", "Cinder Vane", "Glass Hunger", "Null Candle", "Pipe Bloom", "Gallows Kindly", "Wound Psalm",
        "Pale Dividend", "Cradle Ash", "Docket Nine", "Votive Knife", "Latch-Click", "Red Amen", "Sewer Bell", "Black Receipt",
        "Mirthless", "Salt-Kin", "Ash-Psalm", "Bale Saint", "Black Candle", "Bone Ledger", "Brass Wretch", "Broken Laurels",
        "Cage-Mercy", "Cinder Choir", "Cold Tithe", "Copper Smile", "Cracked Icon", "Drowned Bell", "Dust Apostle", "Empty Cup",
        "Ferric Grace", "Flesh Receipt", "Gallows Moth", "Glass Benediction", "Grave Cog", "Gutter Angel", "Half-Vox", "Hollow Oath",
        "Iron Mercy", "Lantern Rat", "Ledger Ghost", "Maggot Crown", "Mercy Nail", "Mirth Tax", "Mold Saint", "Mournful Brass",
        "Null Brother", "Null Sister", "Oath-Eater", "Old Red", "Pale Cog", "Paper Prophet", "Poor Benediction", "Prayer Burn",
        "Rag-Crown", "Red Vesper", "Reliquary Teeth", "Rust Oracle", "Salt Mother", "Scab Laureate", "Seven Knuckles", "Shiver Clerk",
        "Soot Bride", "Soot Groom", "Sump Apostle", "Tallow King", "Throne-Scrap", "Three Warrants", "Tin Confessor", "Tithe-Wound",
        "Votive Wretch", "Wet Hymn", "Wire Rat", "Writ-Biter", "Yellow Psalm", "Zeal Grin", "Ashen Cousin", "Bell-Liar",
        "Black Ration", "Burnt Velvet", "Cage Saint", "Candle Grub", "Clot-Crown", "Cold Ticket", "Corpse Etiquette", "Cradle Thief",
        "Docket Saint", "Drain Bride", "Dry Miracle"
    };

    // Backwards-compatible pools retained for older character creation, audits, and save migration paths.
    static final String[] IMPERIAL_FIRST_NAMES = concat(IMPERIAL_MALE_NAMES, IMPERIAL_FEMALE_NAMES, IMPERIAL_NEUTRAL_GIVEN_NAMES);
    static final String[] IMPERIAL_LAST_NAMES = IMPERIAL_FAMILY_NAMES;

    static String randomPlayerName(Random r) { if (r == null) r = new Random(); return twoPart(r); }
    static String randomNpcName(Faction f, Random r) {
        if (r == null) r = new Random();
        return FactionRosterAuthority.rankedName(f, r);
    }
    static String twoPart(Random r) { return pick(IMPERIAL_FIRST_NAMES, r) + " " + pick(IMPERIAL_LAST_NAMES, r); }
    static String formalName(Random r) { return pick(IMPERIAL_FIRST_NAMES, r) + " " + pick(IMPERIAL_FAMILY_NAMES, r); }
    static String singularName(Random r) { return pick(IMPERIAL_SINGULAR_NAMES, r); }
    static String pick(String[] arr, Random r) { return arr[Math.floorMod(r.nextInt(), arr.length)]; }
    static String[] concat(String[]... arrays) {
        int n = 0; for (String[] a : arrays) n += a.length;
        String[] out = new String[n]; int p = 0;
        for (String[] a : arrays) { System.arraycopy(a, 0, out, p, a.length); p += a.length; }
        return out;
    }
    static int[] deterministicStatSpread(Random r, int count) {
        int[] base = {11,10,9,8,8,7,7,6,6,5,5,4};
        int[] out = new int[count];
        for (int i=0;i<count;i++) out[i] = base[i % base.length];
        for (int i=out.length-1;i>0;i--) { int j = r.nextInt(i+1); int t = out[i]; out[i]=out[j]; out[j]=t; }
        return out;
    }
    static boolean acceptNameChar(char ch) {
        return Character.isLetter(ch) || ch == ' ' || ch == '-' || ch == '\'' || ch == '.';
    }
    static String normalizeSpacing(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").replaceAll("^[ .'-]+", "");
    }
    static String sanitizePlayerName(String s, Random r) {
        String cleaned = normalizeSpacing(s).trim();
        cleaned = cleaned.replaceAll("[ .'-]+$", "");
        if (cleaned.isBlank()) cleaned = randomPlayerName(r == null ? new Random() : r);
        if (cleaned.length() > MAX_PLAYER_NAME_LENGTH) cleaned = cleaned.substring(0, MAX_PLAYER_NAME_LENGTH).trim();
        return cleaned;
    }
}

class FactionRankEntry {
    final int rank;
    final int commandSlots;
    final String title;
    final String scope;
    FactionRankEntry(int rank, int commandSlots, String title, String scope) { this.rank=rank; this.commandSlots=commandSlots; this.title=title; this.scope=scope; }
    String label() { return "Rank " + rank + " / " + commandSlots + " slots / " + title + " / " + scope; }
}

class FactionRosterProfile {
    final Faction faction;
    final String rosterFamily;
    final FactionRankEntry[] ranks;
    FactionRosterProfile(Faction faction, String rosterFamily, FactionRankEntry[] ranks) { this.faction=faction; this.rosterFamily=rosterFamily; this.ranks=ranks; }
    FactionRankEntry rankFor(Random r) {
        // Most encountered NPCs are room/post/desk/shrine/apparatus hands; rare rolls expose command layers.
        int roll = r.nextInt(1000);
        int idx = roll < 520 ? 7 : roll < 730 ? 6 : roll < 850 ? 5 : roll < 925 ? 4 : roll < 970 ? 3 : roll < 992 ? 2 : roll < 998 ? 1 : 0;
        return ranks[Math.min(idx, ranks.length-1)];
    }
}

class FactionRosterAuthority {
    static final int[] COMMAND_SLOTS = {1,2,4,8,16,32,64,128};
    static final String[] SCOPE = {
        "controls all faction-held zones",
        "controls a major deputy chain or half-command of faction operations",
        "controls facility clusters and regional commands",
        "controls room groups, watches, blocks, or command subsections",
        "controls fifth-rank supervisors and local work crews",
        "controls sub-supervisors, small crews, and local rosters",
        "controls local hands, shifts, and practical work details",
        "controls a single room, post, door, desk, shrine, apparatus, or local work point"
    };
    static final String[] RANKS_ADMIN = {
        "Lord Archivist", "Chancellor Senioris", "Master Comptroller", "Archive Prefect", "Ledger Captain", "Seal Warden", "Form Superior", "Queue Steward"
    };
    static final String[] RANKS_ARBITES = {
        "Chief Marshal", "Sub-Marshal", "Senior Proctor", "Watch Commander", "Badge Lead", "Lex-Warden", "Sub-Juror", "Probationary Badge"
    };
    static final String[] RANKS_GUARD = {
        "Lord Castellan", "Colonel Commandant", "Major of Line", "Captain of Watch", "Lieutenant", "Sergeant", "Corporal", "Trooper Lead"
    };
    static final String[] RANKS_MINISTORUM = {
        "Arch-Confessor", "Cardinal-Delegate", "Prelate Warden", "Senior Confessor", "Deacon Superior", "Lay Priest", "Candle Keeper", "Pilgrim Usher"
    };
    static final String[] RANKS_SORORITAS = {
        "Canoness Guard", "Palatine Guard", "Celestian Superior", "Sister Superior", "Dominion Lead", "Retributor Lead", "Battle Sister", "Novitiate Watch"
    };
    static final String[] RANKS_MECHANICUS = {
        "Magos Dominus", "Magos Adjunct", "Enginseer Prime", "Adept Overseer", "Tech-Acolyte Lead", "Data-Scrivener", "Clade Servitor", "Lubricant Novice"
    };
    static final String[] RANKS_HIVER = {
        "Block Speaker", "Hab Factor", "Corridor Steward", "Stack Captain", "Ration Line Lead", "Dorm Warden", "Cell Steward", "Door Monitor"
    };
    static final String[] RANKS_GANG = {
        "Boss", "Knife Second", "Pack Lieutenant", "Corner Captain", "Crew Lead", "Lookout Chief", "Racket Hand", "Door Knife"
    };
    static final String[] RANKS_NOBLE = {
        "House Patriarch", "House Seneschal", "Estate Marshal", "Service Master", "Salon Captain", "Chamberlain", "Butler-Prime", "Door Servant"
    };
    static final String[] RANKS_SCAVENGER = {
        "Scrap King", "Heap Second", "Market Jack", "Route Captain", "Cache Lead", "Sort Warden", "Picker Boss", "Bin Hand"
    };
    static final String[] RANKS_MUTANT = {
        "Brood Patriarch", "Brood Second", "Nest Speaker", "Tunnel Father", "Claw Lead", "Mire Warden", "Fungus Hand", "Lair Guard"
    };
    static final String[] RANKS_CULTIST = {
        "Hidden Prophet", "Knife Apostle", "Rite Speaker", "Cell Master", "Chant Lead", "Candle Knife", "Whisperer", "Mask Bearer"
    };
    static final String[] RANKS_ROGUE = {
        "Core Sovereign", "Logic Regent", "Process Marshal", "Directive Node", "Subroutine Lead", "Relay Warden", "Servo Captain", "Door Process"
    };
    static final String[] RANKS_HERETIC = {
        "Exile Hierarch", "Apostate Second", "Blaspheme Captain", "Ruin Speaker", "Profane Lead", "Oath-Breaker", "Rag Apostle", "Desecrant"
    };
    static final String[] RANKS_GENERIC = {
        "Faction Master", "Senior Deputy", "Third Officer", "Fourth Officer", "Fifth Supervisor", "Sixth Sub-Supervisor", "Seventh Local Hand", "Eighth Post Lead"
    };

    static final LinkedHashMap<Faction,FactionRosterProfile> PROFILES = makeProfiles();

    static LinkedHashMap<Faction,FactionRosterProfile> makeProfiles() {
        LinkedHashMap<Faction,FactionRosterProfile> m = new LinkedHashMap<>();
        for (Faction f : Faction.visibleFactions()) m.put(f, profileForVisible(f));
        return m;
    }
    static FactionRosterProfile profileForVisible(Faction f) {
        if (f == Faction.SCAVENGER) return profile(f, "Scavenger", RANKS_SCAVENGER);
        if (f == Faction.ADMINISTRATUM) return profile(f, "Administratum", RANKS_ADMIN);
        if (f == Faction.ARBITES) return profile(f, "Adeptus Arbites", RANKS_ARBITES);
        if (f == Faction.IMPERIAL_GUARD) return profile(f, "Imperial Guard", RANKS_GUARD);
        if (f == Faction.MINISTORUM) return profile(f, "Adeptus Ministorum", RANKS_MINISTORUM);
        if (f == Faction.SORORITAS) return profile(f, "Adepta Sororitas", RANKS_SORORITAS);
        if (f == Faction.MECHANICUS || (f != null && f.name().startsWith("MECHANICUS"))) return profile(f, "Mechanicus", RANKS_MECHANICUS);
        if (f == Faction.HIVER || (f != null && f.name().startsWith("HIVER"))) return profile(f, "Hiver", RANKS_HIVER);
        if (f == Faction.BANDIT || (f != null && f.name().startsWith("GANGER"))) return profile(f, "Ganger", RANKS_GANG);
        if (f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE"))) return profile(f, "Noble House", RANKS_NOBLE);
        if (f == Faction.MUTANT) return profile(f, "Mutant Pack", RANKS_MUTANT);
        if (f == Faction.CULTIST) return profile(f, "Cult Cell", RANKS_CULTIST);
        if (f == Faction.ROGUE_MACHINE) return profile(f, "Rogue Machine", RANKS_ROGUE);
        if (f == Faction.HERETIC) return profile(f, "Heretic", RANKS_HERETIC);
        return profile(f, "Generic Faction", RANKS_GENERIC);
    }
    static FactionRosterProfile profile(Faction f, String family, String[] titles) {
        FactionRankEntry[] ranks = new FactionRankEntry[8];
        for (int i=0;i<8;i++) ranks[i] = new FactionRankEntry(i+1, COMMAND_SLOTS[i], titles[i], SCOPE[i]);
        return new FactionRosterProfile(f, family, ranks);
    }
    static FactionRosterProfile get(Faction f) {
        if (f == null || f == Faction.NONE) return profile(Faction.NONE, "Unaligned", RANKS_GENERIC);
        FactionRosterProfile p = PROFILES.get(f);
        return p == null ? profileForVisible(f) : p;
    }
    static String rankedName(Faction f, Random r) {
        if (r == null) r = new Random();
        FactionRosterProfile p = get(f);
        FactionRankEntry rank = p.rankFor(r);
        boolean aliasHeavy = f == Faction.MUTANT || f == Faction.CULTIST || f == Faction.HERETIC || f == Faction.BANDIT || (f != null && f.name().startsWith("GANGER"));
        String personal = aliasHeavy && r.nextInt(100) < 42 ? CharacterCreationAuthority.singularName(r) : CharacterCreationAuthority.formalName(r);
        String suffix = "";
        if (f == Faction.MECHANICUS || (f != null && f.name().startsWith("MECHANICUS"))) suffix = r.nextInt(100)<35 ? "-" + (1+r.nextInt(99)) : "";
        return rank.title + " " + personal + suffix;
    }
    static FactionRankEntry rankEntryFor(Faction f, int rank) {
        FactionRosterProfile p = get(f);
        int idx = Math.max(0, Math.min(7, rank - 1));
        return p.ranks[idx];
    }
    static String titleForRank(Faction f, int rank) { return rankEntryFor(f, rank).title; }
    static String scopeForRank(int rank) { return SCOPE[Math.max(0, Math.min(7, rank - 1))]; }
    static FactionRankEntry inferRankForName(Faction f, String name) {
        if (name == null) return null;
        FactionRosterProfile p = get(f);
        String low = name.toLowerCase(Locale.ROOT);
        for (FactionRankEntry e : p.ranks) if (low.startsWith(e.title.toLowerCase(Locale.ROOT) + " ") || low.contains(" " + e.title.toLowerCase(Locale.ROOT) + " ")) return e;
        return null;
    }
    static String auditLine() {
        return "factionRosterProfiles=" + PROFILES.size() + " visibleFactions=" + Faction.visibleFactions().length + " rankSlots=1/2/4/8/16/32/64/128";
    }
}

class Candidate {
    String name, job; LinkedHashMap<String,Integer> stats = new LinkedHashMap<>(); ArrayList<String> jobs = new ArrayList<>(); String[] portrait; int portraitSheet; int portraitIndex; String nameLockedProfileKey = ""; int ageYears = 0; long birthWorldTurn = 0L; String ageBand = "adult";
    final HashSet<String> visitedZoneTypes = new HashSet<>();
    final HashMap<String,Integer> zoneVisitCounts = new HashMap<>();
    LinkedHashMap<String,BodyPart> body = new LinkedHashMap<>();
    // Player and NPC names now route through CharacterCreationAuthority so creation and population synthesis share the same Imperial name pool.
    static String[] first=CharacterCreationAuthority.IMPERIAL_FIRST_NAMES;
    static String[] last=CharacterCreationAuthority.IMPERIAL_LAST_NAMES;
    static String[] allJobs={"Underhive Scavenger","Junior Tech Priest","Hive Ganger","Arbites Probationer","Sump Prospector","Hab Block Electrician","Rogue Medicae","Depot Loader","Permit Clerk Deserter","Tunnel Bruiser","Chem Runner","Scrap Mechanist"};
    static String[] statKeys(){ return new String[]{"Strength","Agility","Endurance","Intellect","Mechanics","Firearms","Melee","Nerve","Charm","Faith","Vision","Hearing"}; }
    static int statMin(String k){ return 4; }
    static int statMax(String k){ return 11; }
    static String statRangeText(String k){ return statMin(k)+"-"+statMax(k); }
    static Candidate random(Random r){
        Candidate c=new Candidate(); c.name=CharacterCreationAuthority.randomPlayerName(r);
        int[] spread = CharacterCreationAuthority.deterministicStatSpread(r, statKeys().length);
        int si = 0;
        for(String k:statKeys()) c.stats.put(k, spread[si++]);
        c.initBody(r);
        // Body minimum rule: every character starts with at least 1 Endurance and 1 Agility in every tracked part, with random natural maximum of 2 each.
        // Broad attributes are currently still generated as 4-11 for playability, then later derived downward from body condition and augmentation.
        // All jobs are visible during character creation. Eligibility is determined per rolled character by JobProfile requirements.
        c.jobs.addAll(Arrays.asList(allJobs)); c.job=c.jobs.get(0);
        // PLAYER PORTRAIT AUTHORITY: character creation may ONLY use the baseline
        // normal-human portrait pool. The augmented/magos/tech-priest sheets are
        // reserved for NPCs until their slicing metadata is verified.
        c.portraitSheet = ImageCache.PLAYER_BASELINE_HUMAN_POOL;
        c.portraitIndex = r.nextInt(10000);
        String eye = r.nextBoolean()?"o":"*"; c.portrait=new String[]{"  ____  "," /|"+eye+" " + eye + "|\\"," ||_-_|| "," /|###|\\","  /| |\\ ","  || ||  "};
        return c;
    }
    void initBody(Random r) {
        addPart(r, "Head", "headgear / cranial augments");
        addPart(r, "Torso", "coat / armor / organs");
        addPart(r, "Hips", "belt / pelvis");
        addPart(r, "L Upper Arm", "sleeve / armor / aug");
        addPart(r, "R Upper Arm", "sleeve / armor / aug");
        addPart(r, "L Lower Arm", "bracer / aug");
        addPart(r, "R Lower Arm", "bracer / aug");
        addPart(r, "L Hand", "glove / tool / aug");
        addPart(r, "R Hand", "glove / weapon / aug");
        addPart(r, "Fingers", "rings / digits / fine tools");
        addPart(r, "L Upper Leg", "leg armor / aug");
        addPart(r, "R Upper Leg", "leg armor / aug");
        addPart(r, "L Lower Leg", "shin / aug");
        addPart(r, "R Lower Leg", "shin / aug");
        addPart(r, "L Foot", "boot / aug");
        addPart(r, "R Foot", "boot / aug");
    }
    void addPart(Random r, String name, String slot) { body.put(name, new BodyPart(name, slot, 1 + r.nextInt(2), 1 + r.nextInt(2))); }
    Candidate copy(){ Candidate c=new Candidate(); c.name=name; c.job=job; c.ageYears=ageYears; c.birthWorldTurn=birthWorldTurn; c.ageBand=ageBand; c.stats.putAll(stats); c.jobs.addAll(jobs); c.portrait=portrait.clone(); c.portraitSheet=portraitSheet; c.portraitIndex=portraitIndex; c.nameLockedProfileKey=nameLockedProfileKey; c.visitedZoneTypes.addAll(visitedZoneTypes); c.zoneVisitCounts.putAll(zoneVisitCounts); for(Map.Entry<String,BodyPart> e: body.entrySet()) c.body.put(e.getKey(), e.getValue().copy()); return c; }
}



class JobProfile {
    String name, description, clothingName;
    Faction faction;
    int disguiseBase, clothingDefense;
    LinkedHashMap<String,Integer> statBonuses = new LinkedHashMap<>();
    LinkedHashMap<String,Integer> statPenalties = new LinkedHashMap<>();
    ArrayList<String> items = new ArrayList<>();
    LinkedHashMap<String,Integer> requirements = new LinkedHashMap<>();

    JobProfile(String name, Faction faction, String clothingName, int disguiseBase, int clothingDefense, String description) {
        this.name = name; this.faction = faction; this.clothingName = clothingName; this.disguiseBase = disguiseBase; this.clothingDefense = clothingDefense; this.description = description;
    }
    JobProfile bonus(String stat, int value){ statBonuses.put(stat, value); return this; }
    JobProfile penalty(String stat, int value){ statPenalties.put(stat, -Math.abs(value)); return this; }
    JobProfile item(String item){ items.add(item); return this; }
    JobProfile req(String stat, int value){ requirements.put(stat, value); return this; }
    String bonusText(){ ArrayList<String> b=new ArrayList<>(); for(Map.Entry<String,Integer> e:statBonuses.entrySet()) b.add(e.getKey()+String.format(" %+d", e.getValue())); return b.isEmpty()?"none":String.join(", ", b); }
    String penaltyText(){ ArrayList<String> b=new ArrayList<>(); for(Map.Entry<String,Integer> e:statPenalties.entrySet()) b.add(e.getKey()+String.format(" %+d", e.getValue())); return b.isEmpty()?"none":String.join(", ", b); }
    String requirementText(){ if(requirements.isEmpty()) return "none"; ArrayList<String> b=new ArrayList<>(); for(Map.Entry<String,Integer> e:requirements.entrySet()) b.add(e.getKey()+" "+e.getValue()+"+"); return String.join(", ", b); }
    boolean meets(Candidate c){ for(Map.Entry<String,Integer> e: requirements.entrySet()) if(c.stats.getOrDefault(e.getKey(),0) < e.getValue()) return false; return true; }
    String missingText(Candidate c){ ArrayList<String> b=new ArrayList<>(); for(Map.Entry<String,Integer> e:requirements.entrySet()){ int have=c.stats.getOrDefault(e.getKey(),0); if(have < e.getValue()) b.add(e.getKey()+" "+have+"/"+e.getValue()); } return b.isEmpty()?"none":String.join(", ", b); }
    String shortIdentity(){ return faction.label + " / " + clothingName; }
    ArrayList<String> startingItems(){ ArrayList<String> out=new ArrayList<>(items); if(!out.contains(clothingName)) out.add(clothingName); return out; }
    Clothing clothing(){ return new Clothing(clothingName, faction == Faction.NONE ? Faction.HIVER : faction, disguiseBase, clothingDefense, false); }

    static final LinkedHashMap<String,JobProfile> PROFILES = makeProfiles();
    static JobProfile get(String name){ JobProfile p=PROFILES.get(name); return p == null ? PROFILES.get("Underhive Scavenger") : p; }
    static LinkedHashMap<String,JobProfile> makeProfiles(){
        LinkedHashMap<String,JobProfile> m=new LinkedHashMap<>();
        m.put("Underhive Scavenger", new JobProfile("Underhive Scavenger", Faction.SCAVENGER, "Scavenger rags", 35, 1, "A floor-level survivor with a sack, a blade, and the good sense to look hungry instead of interesting. Neutral to most, questioned by law, prey to bandits.").bonus("Vision",1).bonus("Hearing",1).bonus("Agility",1).bonus("Melee",1).penalty("Intellect",1).penalty("Charm",1).item("Scrap knife").item("Patch sack").item("Ration brick"));
        m.put("Junior Tech Priest", new JobProfile("Junior Tech Priest", Faction.MECHANICUS, "Mechanicus novice robe", 48, 2, "A low-ranking machine cult functionary with tool rites, awkward authority, and the social warmth of a locked fuse box. Machines make more sense than people.").req("Mechanics",8).req("Intellect",8).bonus("Mechanics",2).bonus("Intellect",1).bonus("Faith",1).penalty("Charm",1).penalty("Melee",1).item("Cracked auspex").item("Ritual wrench").item("Machine oil vial"));
        m.put("Hive Ganger", new JobProfile("Hive Ganger", Faction.BANDIT, "Gang colors", 54, 2, "A crew-tied brawler with visible colors, local enemies, and an informal education in making people stop disagreeing. Arbites and rivals are immediate problems.").req("Nerve",7).req("Endurance",7).bonus("Firearms",1).bonus("Melee",1).bonus("Nerve",1).bonus("Agility",1).penalty("Charm",1).penalty("Intellect",1).item("Stub pistol with poor sights").item("Shiv").item("Contraband charm"));
        m.put("Arbites Probationer", new JobProfile("Arbites Probationer", Faction.ARBITES, "Arbites patrol coat", 45, 4, "A junior law enforcer cut loose below the nice floors. Better armor, worse neighbors. Every ganger sees a badge-shaped reason to start shooting.").req("Faith",8).req("Firearms",8).req("Endurance",8).req("Nerve",6).bonus("Firearms",1).bonus("Nerve",2).bonus("Endurance",1).penalty("Charm",1).penalty("Melee",1).item("Shock baton").item("Citation slate").item("Emergency ration"));
        m.put("Sump Prospector", new JobProfile("Sump Prospector", Faction.SCAVENGER, "Waterproof scavenger wraps", 42, 1, "A damp, patient resource hunter trained to hear dripping water and lies in the same echo. Useful in ruins, disgusting at dinner.").bonus("Hearing",2).bonus("Endurance",1).bonus("Vision",1).penalty("Charm",1).penalty("Firearms",1).item("Filter canteen").item("Survey hook").item("Mildewed map scrap"));
        m.put("Hab Block Electrician", new JobProfile("Hab Block Electrician", Faction.NONE, "Hiver workwear", 50, 1, "A civilian tradesworker with practical hands and no official protection worth mentioning. Better skills, no banner, no noble nonsense.").req("Intellect",8).req("Mechanics",8).bonus("Mechanics",2).bonus("Intellect",1).bonus("Vision",1).penalty("Melee",1).penalty("Nerve",1).item("Insulated pliers").item("Wire bundle").item("Cheap lunch tin"));
        m.put("Rogue Medicae", new JobProfile("Rogue Medicae", Faction.NONE, "Stained civilian coat", 44, 1, "A back-room healer with steady hands, questionable licensing, and a worrying familiarity with screaming. Useful until the authorities ask for paperwork.").req("Charm",7).req("Intellect",8).req("Nerve",7).bonus("Intellect",1).bonus("Charm",1).bonus("Nerve",1).bonus("Vision",1).penalty("Strength",1).penalty("Firearms",1).item("Field dressings").item("Injector case").item("Bent scalpel"));
        m.put("Depot Loader", new JobProfile("Depot Loader", Faction.NONE, "Cargo hauler overalls", 46, 2, "A civilian freight brute from the endless loading bays. Strong back, dull paperwork, and a working knowledge of where people hide valuable things.").bonus("Strength",2).bonus("Endurance",1).bonus("Melee",1).penalty("Intellect",1).penalty("Charm",1).item("Cargo hook").item("Work gloves").item("Protein ration"));
        m.put("Permit Clerk Deserter", new JobProfile("Permit Clerk Deserter", Faction.NONE, "Frayed administratum coat", 38, 1, "A civilian paper-rat who fled the stamp desk with enough procedural knowledge to make law enforcement briefly regret conversation.").req("Intellect",8).bonus("Charm",2).bonus("Intellect",1).bonus("Faith",1).penalty("Strength",1).penalty("Melee",1).item("Blank form packet").item("Ink stylus").item("Expired work permit"));
        m.put("Tunnel Bruiser", new JobProfile("Tunnel Bruiser", Faction.NONE, "Padded tunnel leathers", 35, 3, "A civilian muscle-for-hire from maintenance tunnels and debt collection corners. No faction owns you yet, which is either freedom or unemployment.").req("Strength",9).bonus("Strength",1).bonus("Melee",2).bonus("Endurance",1).penalty("Intellect",1).penalty("Charm",1).item("Heavy spanner").item("Knee wraps").item("Tin of corpse-starch"));
        m.put("Chem Runner", new JobProfile("Chem Runner", Faction.BANDIT, "Runner colors under coat", 43, 1, "A fast courier for substances polite society pretends not to manufacture. Good feet, bad friends, worse customers.").bonus("Agility",2).bonus("Nerve",1).bonus("Hearing",1).penalty("Faith",1).penalty("Mechanics",1).item("Stimulant ampoule").item("Hidden pouch").item("Tiny knife"));
        m.put("Scrap Mechanist", new JobProfile("Scrap Mechanist", Faction.NONE, "Oil-stained workwear", 42, 1, "A civilian repair scavenger who can coax one more breath from machines that should have died three owners ago.").bonus("Mechanics",2).bonus("Vision",1).bonus("Intellect",1).penalty("Charm",1).penalty("Faith",1).item("Cracked wrench").item("Spare bolts").item("Dirty canteen"));
        return m;
    }
}




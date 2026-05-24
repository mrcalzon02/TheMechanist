package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

/**
 * Owns the partitioned, name-locked profile portrait set. These portraits are
 * not part of random character generation; they become selectable only when the
 * external/local profile name and the in-game character name resolve to the same
 * registered profile entry.
 */
final class NameLockedProfilePortraitAuthority {
    static final String VERSION = "0.9.10hg";
    static final String ASSET_ROOT = "assets/art/portraits/name_locked/";

    static final class Entry {
        final String key;
        final String displayName;
        final String title;
        final String assetPath;
        final List<String> aliases;
        final List<String> unlockFlags;

        Entry(String key, String displayName, String title, String assetFile, List<String> aliases, List<String> unlockFlags) {
            this.key = key;
            this.displayName = displayName;
            this.title = title;
            this.assetPath = ASSET_ROOT + assetFile;
            this.aliases = List.copyOf(aliases);
            this.unlockFlags = List.copyOf(unlockFlags);
        }

        boolean matches(String text) {
            String n = normalize(text);
            if (n.isEmpty()) return false;
            if (n.equals(normalize(key)) || n.equals(normalize(displayName))) return true;
            for (String a : aliases) if (n.equals(normalize(a))) return true;
            return false;
        }

        String compactLine() { return displayName + " // " + title + " // " + key; }
    }

    private static final ArrayList<Entry> ENTRIES = new ArrayList<>();
    private static final LinkedHashMap<String, Entry> BY_KEY = new LinkedHashMap<>();
    private static volatile String activeOperatorProfileKey = "";

    static {
        add("markiplier", "Markiplier", "The Iron Host", "markiplier.png", list("Mark Fischbach"), list("PROFILE_PORTRAIT_MARKIPLIER", "TITLE_IRON_HOST"));
        add("jacksepticeye", "Jacksepticeye", "The Emerald Marine", "jacksepticeye.png", list("Jacksepticeye", "Jack", "Sean McLoughlin", "Seán McLoughlin"), list("PROFILE_PORTRAIT_JACKSEPTICEYE", "COLOR_EMERALD"));
        add("tom_yogscast", "Tom (Yogscast)", "Hooded Strategist", "tom_yogscast.png", list("Tom", "Angor", "Angory Tom", "Tom Clark"), list("PROFILE_PORTRAIT_TOM_YOGSCAST", "TACTICAL_HOOD"));
        add("ben_yogscast", "Ben (Yogscast)", "The Lore Keeper", "ben_yogscast.png", list("Ben", "Bedgar", "Bedgarsan", "Ben Edgar"), list("PROFILE_PORTRAIT_BEN_YOGSCAST", "LORE_KEEPER"));
        add("quill18", "Quill18", "Grand Strategist", "quill18.png", list("Quill", "Martin Gladu"), list("PROFILE_PORTRAIT_QUILL18", "STRATEGY_AIDE"));
        add("the_spiffing_brit", "The Spiffing Brit", "Tea-Borne Exploit Auditor", "the_spiffing_brit.png", list("SpiffingBrit", "Spiff", "The Spiffing Brit"), list("PROFILE_PORTRAIT_SPIFFING_BRIT", "TEA_RATION"));
        add("splitsie", "Splitsie", "Chief Engineer", "splitsie.png", list("Dr. Michael Pomroy", "Michael Pomroy"), list("PROFILE_PORTRAIT_SPLITSIE", "ENGINEER_CERT"));
        add("wastedspace", "Wastedspace", "Void Architect", "wastedspace.png", list("WastedSpace", "WastedSpaceGames", "Ken"), list("PROFILE_PORTRAIT_WASTEDSPACE", "VOID_ARCHITECT"));
        add("truepen", "Truepen", "Factory Overseer", "truepen.png", list("TruePen"), list("PROFILE_PORTRAIT_TRUEPEN", "AUTOMATION_AIDE"));
        add("dash_dashington", "Dash Dashington", "Logistics Director", "dash_dashington.png", list("DashDashington", "Dash"), list("PROFILE_PORTRAIT_DASH_DASHINGTON", "LOGISTICS_SEAL"));
        add("luetin09", "Luetin09", "High Scholar of Terra", "luetin09.png", list("Luetin", "Luke"), list("PROFILE_PORTRAIT_LUETIN09", "SCHOLAR_RELIQUARY"));
        add("bricky", "Bricky", "Adeptus Comedicus", "bricky.png", list("BrickyOrchid8"), list("PROFILE_PORTRAIT_BRICKY", "MORALE_CHARM"));
        add("dkdiamantes", "DKDiamantes", "The Cult Initiate", "dkdiamantes.png", list("DK", "Adeptus Ridiculous"), list("PROFILE_PORTRAIT_DKDIAMANTES", "RITUAL_WARNING_TAG"));
        add("auspex_tactics", "Auspex Tactics", "The Living Datasheet", "auspex_tactics.png", list("Auspex", "AuspexTactics"), list("PROFILE_PORTRAIT_AUSPEX", "TACTICAL_SCANNER"));
        add("weshammer", "Weshammer", "Cinematic Chronicler", "weshammer.png", list("Wes"), list("PROFILE_PORTRAIT_WESHAMMER", "CHRONICLE_TOKEN"));
        add("chapter_master_valrak", "Chapter Master Valrak", "Son of Dorn", "chapter_master_valrak.png", list("Valrak", "CMValrak"), list("PROFILE_PORTRAIT_VALRAK", "FORTIFICATION_SEAL"));
        add("majorkill", "Majorkill", "Outback Marauder", "majorkill.png", list("MajorKill", "Major Kill"), list("PROFILE_PORTRAIT_MAJORKILL", "BRAWLER_TOKEN"));
        add("oculus_imperia", "Oculus Imperia", "Imperial Historian", "oculus_imperia.png", list("OculusImperia", "ScribeOculus", "Oculus"), list("PROFILE_PORTRAIT_OCULUS", "HISTORIAN_QUILL"));
        add("baldermorts_guide", "Baldermort's Guide", "The Resonant Orator", "baldermorts_guide.png", list("Baldermort", "Baldermorts Guide", "Baldermort's Guide"), list("PROFILE_PORTRAIT_BALDERMORT", "ORATOR_TOKEN"));
        add("arbitor_ian", "Arbitor Ian", "Chronology Archivist", "arbitor_ian.png", list("ArbitorIan", "Ian"), list("PROFILE_PORTRAIT_ARBITOR_IAN", "ARCHIVE_STAMP"));
        add("wolf_lord_rho", "Wolf Lord Rho", "Saga Narrator", "wolf_lord_rho.png", list("WolfLordRho", "Rho"), list("PROFILE_PORTRAIT_WOLF_LORD_RHO", "SAGA_MARK"));
        add("pancreasnowork", "PancreasNoWork", "Trope Analyst", "pancreasnowork.png", list("Pancreas No Work", "Pancreas"), list("PROFILE_PORTRAIT_PANCREAS", "CRITIQUE_SHEET"));
        add("a_vox_in_the_void", "A Vox in the Void", "The Grimdark Echo", "a_vox_in_the_void.png", list("AVoxInTheVoid", "Vox in the Void", "Vox"), list("PROFILE_PORTRAIT_VOX", "VOID_ECHO"));
        add("tabletop_titan", "Tabletop Titan", "Cinematic General", "tabletop_titan.png", list("Tabletop Titans"), list("PROFILE_PORTRAIT_TABLETOP_TITAN", "COMMAND_SLATE"));
        add("miniac", "Miniac", "Master Artisan", "miniac.png", list("Miniac", "Scott Walter"), list("PROFILE_PORTRAIT_MINIAC", "ARTISAN_BRUSH"));
    }

    private static void add(String key, String displayName, String title, String assetFile, List<String> aliases, List<String> unlockFlags) {
        Entry e = new Entry(key, displayName, title, assetFile, aliases, unlockFlags);
        ENTRIES.add(e);
        BY_KEY.put(key, e);
    }

    private static List<String> list(String... values) { return Arrays.asList(values); }

    static Optional<Entry> match(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        for (Entry e : ENTRIES) if (e.matches(name)) return Optional.of(e);
        return Optional.empty();
    }

    static Entry byKey(String key) { return key == null ? null : BY_KEY.get(key); }

    static void setActiveOperatorProfileName(String profileName) {
        activeOperatorProfileKey = match(profileName).map(e -> e.key).orElse("");
    }

    static String activeOperatorProfileKey() { return activeOperatorProfileKey == null ? "" : activeOperatorProfileKey; }

    static boolean isActiveOperatorKey(String key) {
        return key != null && !key.isBlank() && key.equals(activeOperatorProfileKey());
    }

    static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    static boolean isUnlockedForProfile(String operatorProfileName, String characterName) {
        Optional<Entry> profile = match(operatorProfileName);
        Optional<Entry> character = match(characterName);
        return profile.isPresent() && character.isPresent() && profile.get().key.equals(character.get().key);
    }

    static Entry unlockedEntry(String operatorProfileName, String characterName) {
        Optional<Entry> profile = match(operatorProfileName);
        Optional<Entry> character = match(characterName);
        if (profile.isPresent() && character.isPresent() && profile.get().key.equals(character.get().key)) return profile.get();
        return null;
    }

    static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT).replace("'", "").replaceAll("[^a-z0-9]+", "").trim();
    }


    static List<Entry> nobleSeedCandidates() {
        ArrayList<Entry> out = new ArrayList<>();
        String active = activeOperatorProfileKey();
        for (Entry e : ENTRIES) if (!e.key.equals(active)) out.add(e);
        return out;
    }

    static boolean isNobleSeedZone(ZoneType z) {
        return z == ZoneType.SECTOR_GOVERNORS_MANSION || z == ZoneType.NOBLE_SERVICE_SPINE;
    }

    static NpcEntity maybeCreateNobleSeed(World world, int roomId, Random r) {
        if (world == null || r == null || !isNobleSeedZone(world.zoneType)) return null;
        List<Entry> pool = nobleSeedCandidates();
        if (pool.isEmpty()) return null;
        // Rare and deterministic inside the normal world-generation RNG stream.
        if (r.nextDouble() >= 0.018) return null;
        Rectangle room = roomId >= 0 && roomId < world.rooms.size() ? world.rooms.get(roomId) : null;
        Point p = room == null ? world.randomOpenPoint(r) : world.randomOpenPointInRoom(room);
        if (p == null || world.npcAt(p.x, p.y) != null) return null;
        Entry e = pool.get(Math.floorMod(r.nextInt(), pool.size()));
        Faction f = world.npcFactionForRoom(roomId);
        if (f == null || f == Faction.NONE || !f.name().startsWith("NOBLE")) f = Faction.NOBLE;
        NpcEntity n = NpcEntity.create(f, world.zoneType, p.x, p.y, r);
        n.name = e.displayName;
        n.role = "Visiting Noble Profile Dossier";
        n.state = "Noble Entourage";
        n.symbol = 'N';
        n.factionRank = 3;
        n.factionRankTitle = "Visiting Noble";
        n.factionRankScope = "rare world-seeded profile noble; excluded when this profile is the active operator";
        n.nameLockedProfileKey = e.key;
        n.portraitIndex = Math.abs(Objects.hash(e.key, world.seed, roomId));
        n.hp = Math.max(n.hp, 18);
        n.id = "NOBLE-PROFILE-" + e.key + "-" + Math.abs(Objects.hash(world.seed, roomId, p.x, p.y));
        return n;
    }

    static List<String> infopediaLines(Entry e, boolean activeProfileUnlocked) {
        ArrayList<String> lines = new ArrayList<>();
        if (e == null) {
            lines.add("Profile dossier not found.");
            return lines;
        }
        lines.add("Profile Dossier: " + e.displayName);
        lines.add("Title: " + e.title);
        lines.add("Profile key: " + e.key);
        lines.add("Portrait partition: name-locked profile portraits; excluded from random character generation.");
        lines.add("Unlock rule: active profile name and finalized character name must resolve to this same registered profile.");
        lines.add("Current status: " + (activeProfileUnlocked ? "UNLOCKED FOR THIS CHARACTER" : "LOCKED UNLESS PROFILE AND CHARACTER NAME MATCH"));
        lines.add("");
        lines.add("Granted profile items/perks:");
        for (String flag : e.unlockFlags) lines.add("- " + readableUnlockFlag(flag));
        lines.add("- " + e.displayName + " profile sigil");
        lines.add("- " + e.title + " dossier token");
        lines.add("");
        lines.add("World-seeding rule: this figure may rarely appear as a noble-zone NPC in other profiles' worlds, but is excluded while its matching profile is the active operator.");
        return lines;
    }

    static String readableUnlockFlag(String flag) {
        if (flag == null || flag.isBlank()) return "profile recognition";
        return flag.replace("PROFILE_PORTRAIT_", "").replace('_', ' ').toLowerCase(Locale.ROOT).trim();
    }

    static String auditSummary() {
        return "authority=" + VERSION + " partition=name-locked-profile-portraits entries=" + ENTRIES.size() + " assetRoot=" + ASSET_ROOT;
    }
}

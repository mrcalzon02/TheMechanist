package mechanist;

import java.util.*;

/**
 * Road-frontage fixture interaction authority.
 *
 * Handles current public-road fixtures: refuse search, benches, broadcast
 * screens, radios, kiosks, service counters, clinic signs, and bar frontage
 * anchors.
 */
final class FrontageFixtureInteractionAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] INTERACTIVE_TYPES = {
            AssetIntegrationDisciplineAuthority.PUBLIC_TRASH_BIN,
            AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE,
            AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER,
            AssetIntegrationDisciplineAuthority.PUBLIC_RATION_BENCH,
            AssetIntegrationDisciplineAuthority.PUBLIC_PICT_SCREEN,
            AssetIntegrationDisciplineAuthority.CHEAP_RADIO,
            AssetIntegrationDisciplineAuthority.PUBLIC_INFO_KIOSK,
            AssetIntegrationDisciplineAuthority.PUBLIC_SERVICE_COUNTER,
            AssetIntegrationDisciplineAuthority.MEDICAE_FRONTAGE,
            AssetIntegrationDisciplineAuthority.BAR_FRONTAGE
    };

    private FrontageFixtureInteractionAuthority() {}

    static boolean tryInteract(GamePanel g, int tx, int ty) {
        if (g == null || g.world == null) return false;
        MapObjectState m = g.world.mapObjectAt(tx, ty);
        if (m == null || m.type == null) return false;
        switch (AssetIntegrationDisciplineAuthority.canonicalType(m.type)) {
            case AssetIntegrationDisciplineAuthority.PUBLIC_TRASH_BIN: interactScavengeContainer(g, m); return true;
            case AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE: interactScavengeContainer(g, m); return true;
            case AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER: interactScavengeContainer(g, m); return true;
            case AssetIntegrationDisciplineAuthority.PUBLIC_RATION_BENCH: interactBench(g, m); return true;
            case AssetIntegrationDisciplineAuthority.PUBLIC_PICT_SCREEN: interactPictScreen(g, m); return true;
            case AssetIntegrationDisciplineAuthority.CHEAP_RADIO: interactRadio(g, m); return true;
            case AssetIntegrationDisciplineAuthority.PUBLIC_INFO_KIOSK: interactInfoKiosk(g, m); return true;
            case AssetIntegrationDisciplineAuthority.PUBLIC_SERVICE_COUNTER: interactServiceCounter(g, m); return true;
            case AssetIntegrationDisciplineAuthority.MEDICAE_FRONTAGE: interactMedicaeFrontage(g, m); return true;
            case AssetIntegrationDisciplineAuthority.BAR_FRONTAGE: interactBarFrontage(g, m); return true;
            default: return false;
        }
    }

    static void interactScavengeContainer(GamePanel g, MapObjectState m) {
        if (m.cooldownUntilTurn > g.turn) {
            g.logEvent("This refuse fixture has already been picked over recently. Give the civic waste stream a moment to become disappointing again.");
            g.advanceTurn("checks an already-searched refuse fixture.");
            return;
        }
        Random r = localRandom(g, m, 11);
        String item = WasteNewsprintScavengeAuthority.chooseLoot(g, m, r);
        int chance = WasteNewsprintScavengeAuthority.searchChance(g, m);
        if (r.nextInt(100) < chance) {
            if (g.inventoryWeight() + 1 <= g.carryCapacity()) {
                g.addInventoryItem(item, ItemProvenanceRecord.of(item, Faction.NONE, WasteNewsprintScavengeAuthority.shortLabel(m), g.world, g.turn,
                        "promoted waste/newsprint/scavenge fixture search; source=" + safe(m.label), "player recovered item from public refuse/newsprint fixture"));
                g.logEvent("SCAVENGE CONTAINER: recovered " + item + " from " + WasteNewsprintScavengeAuthority.shortLabel(m) + ".");
                g.gainXp("Scavenging", 1, "public refuse/newsprint fixture search");
            } else {
                g.logEvent("SCAVENGE CONTAINER: found " + item + ", but your carrying load is full.");
            }
        } else {
            g.logEvent("SCAVENGE CONTAINER: wrappers, damp print, and refuse with no remaining tactical value. Nothing useful this time.");
        }
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 16 + r.nextInt(20));
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_pipe"), 2, g.options);
        g.advanceTurn(WasteNewsprintScavengeAuthority.searchVerb(m.type));
        g.repaint();
    }

    static void interactBench(GamePanel g, MapObjectState m) {
        String note = PublicServiceMediaAuthority.benchRumor(g, localRandom(g, m, 22));
        g.logEvent("PUBLIC BENCH: you sit for a moment. " + note);
        if (g.fatigue > 0) g.fatigue = Math.max(0, g.fatigue - 1);
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 8);
        g.sounds.playDistantCue("ambient_machine", 6, g.options);
        g.advanceTurn("rests briefly on a public bench.");
        g.repaint();
    }

    static void interactPictScreen(GamePanel g, MapObjectState m) {
        String report = ImperialNewsNetworkApi.broadcastBulletin(g, "public-pict-screen/frontage", localRandom(g, m, 33));
        g.lastBroadcastReport = report;
        g.lastInnNewsIssue = report;
        g.logEvent("PUBLIC PICT SCREEN: " + report);
        g.gainXp("Investigation", 1, "watched public pict screen");
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, Math.max(12, GamePanel.TURNS_PER_HOUR / 2));
        g.sounds.playDistantCue("ambient_tv", 3, g.options);
        g.advanceTurn("watches a public pict/news screen.");
        g.repaint();
    }

    static void interactRadio(GamePanel g, MapObjectState m) {
        String report = ImperialNewsNetworkApi.broadcastBulletin(g, "cheap-roadside-radio/frontage", localRandom(g, m, 44));
        g.lastBroadcastReport = report;
        g.lastInnNewsIssue = report;
        g.logEvent("CHEAP RADIO: " + report);
        g.gainXp("Investigation", 1, "listened to cheap public radio");
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, Math.max(8, GamePanel.TURNS_PER_HOUR / 3));
        g.sounds.playDistantCue("ambient_radio", 2, g.options);
        g.advanceTurn("listens to a cheap public radio.");
        g.repaint();
    }

    static void interactInfoKiosk(GamePanel g, MapObjectState m) {
        String target = PublicServiceMediaAuthority.kioskLine(g, localRandom(g, m, 55));
        g.logEvent("INFO KIOSK: " + target);
        g.gainXp("Knowledge", 1, "read public information kiosk");
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 24);
        g.sounds.playDistantCue("ambient_chime", 4, g.options);
        g.advanceTurn("reads a public information kiosk.");
        g.repaint();
    }

    static void interactServiceCounter(GamePanel g, MapObjectState m) {
        if (g.countMoney() >= 1 && localRandom(g,m,66).nextInt(100) < 35) {
            g.spendImperialScript(1);
            g.logEvent("PUBLIC SERVICE COUNTER: paid 1 Imperial Script for a stamped civic chit, a queue number, and no guarantee of relevance.");
            String item = PublicServiceMediaAuthority.serviceCounterIssuedItem();
            if (ItemCatalog.get(item) != null && g.inventoryWeight() + 1 <= g.carryCapacity()) {
                g.addInventoryItem(item, ItemProvenanceRecord.of(item, Faction.HIVER, "public service counter", g.world, g.turn,
                        "frontage counter civic-service issue", "issued to player after minor civic fee"));
            }
            g.gainXp("Commerce", 1, "used public service counter");
        } else {
            g.logEvent("PUBLIC SERVICE COUNTER: " + PublicServiceMediaAuthority.serviceCounterInspectionLine(localRandom(g, m, 67)));
            g.gainXp("Knowledge", 1, "inspected public service counter");
        }
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, GamePanel.TURNS_PER_HOUR);
        g.sounds.playDistantCue("ambient_door_servo", 5, g.options);
        g.advanceTurn("uses a public service counter.");
        g.repaint();
    }

    static void interactMedicaeFrontage(GamePanel g, MapObjectState m) {
        g.logEvent(MedicaeFixtureAuthority.frontageInspectionLine());
        g.gainXp("Medicine", 1, "identified roadside medicae frontage");
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + 20;
        g.sounds.playDistantCue("ambient_chime", 5, g.options);
        g.advanceTurn("inspects medicae frontage.");
        g.repaint();
    }

    static void interactBarFrontage(GamePanel g, MapObjectState m) {
        Faction f = g.world == null ? Faction.NONE : g.world.dominantContinuityFactionForZone();
        g.logEvent(BarMarketSocialFixtureAuthority.frontageInspectionLine(f));
        g.gainXp("Social", 1, "identified faction bar frontage");
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, 20);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, "ambient_radio"), 4, g.options);
        g.advanceTurn("inspects faction bar frontage.");
        g.repaint();
    }

    static Random localRandom(GamePanel g, MapObjectState m, int salt) {
        long seed = (g == null ? 0 : g.seed) ^ (g == null ? 0 : g.turn * 65537L) ^ Objects.hash(m == null ? "" : m.id, m == null ? 0 : m.x, m == null ? 0 : m.y, salt);
        return new Random(seed);
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "frontage fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "frontage fixture" : s;
    }

    static String safe(String s) { return s == null ? "" : s.replace(';', ','); }}

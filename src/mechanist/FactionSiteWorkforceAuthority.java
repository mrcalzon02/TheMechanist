package mechanist;

import java.util.Locale;

/** Connects local population assignments to faction-site production and shelf viability. */
final class FactionSiteWorkforceAuthority {
    record Status(int matchedLedgers, int assignedWorkers, int availableReserve,
                  int effectiveWorkers, boolean locallyBacked, String reason) {
        String playerLine() {
            if (!locallyBacked) return "Site workforce: " + effectiveWorkers
                    + " abstract worker(s); no matching local population ledger is loaded.";
            if (effectiveWorkers <= 0) return "Site workforce unavailable: matching local rosters have no assigned workers; production and site exports are paused.";
            return "Site workforce: " + effectiveWorkers + " assigned worker(s) from " + matchedLedgers
                    + " matching local roster(s), with reserve capacity " + availableReserve + ".";
        }
    }

    private FactionSiteWorkforceAuthority() { }

    static Status sync(NpcFactionSite site, World world) {
        Status status = evaluate(site, world);
        if (site != null && status.locallyBacked()) site.workers = Math.max(0, Math.min(24, status.effectiveWorkers()));
        return status;
    }

    static Status evaluate(NpcFactionSite site, World world) {
        if (site == null) return new Status(0, 0, 0, 0, false, "no faction production site");
        if (world == null || !sameLocation(site, world) || world.roomPopulationLedgers == null) {
            return new Status(0, 0, 0, Math.max(0, site.workers), false,
                    "site remains under distant abstract workforce accounting");
        }
        int matched = 0;
        int assigned = 0;
        int reserve = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || !matches(site, ledger)) continue;
            matched++;
            assigned += Math.max(0, ledger.assigned);
            reserve += Math.max(0, ledger.available);
        }
        if (matched <= 0) {
            return new Status(0, 0, 0, Math.max(0, site.workers), false,
                    "no matching local population roster");
        }
        return new Status(matched, assigned, reserve, assigned, true,
                assigned <= 0 ? "matching rosters are unstaffed" : "matching assigned workforce is active");
    }

    private static boolean matches(NpcFactionSite site, RoomPopulationLedger ledger) {
        Faction siteFaction = site.faction == null ? Faction.NONE : site.faction;
        Faction ledgerFaction = ledger.faction == null ? Faction.NONE : ledger.faction;
        if (GamePanel.sameFactionFamilyStatic(siteFaction, ledgerFaction)) return true;
        String siteText = (safe(site.name) + " " + safe(site.facilityType)).toLowerCase(Locale.ROOT);
        String ledgerText = (safe(ledger.facilityId) + " " + safe(ledger.facilityPurpose) + " "
                + safe(ledger.facilityProductFocus) + " " + safe(ledger.roomName)).toLowerCase(Locale.ROOT);
        for (String token : siteText.split("[^a-z0-9]+")) {
            if (token.length() >= 5 && ledgerText.contains(token)) return true;
        }
        return false;
    }

    private static boolean sameLocation(NpcFactionSite site, World world) {
        return site.sectorX == world.sectorX && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX && site.zoneY == world.zoneY && site.floor == world.floor;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}

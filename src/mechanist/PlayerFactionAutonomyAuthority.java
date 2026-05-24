package mechanist;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

/** World-side continuity planner for player-founded factions when players are absent. */
final class PlayerFactionAutonomyAuthority {
    static final String VERSION = "player-faction-autonomy-authority-0.9.10gv";
    private static final String PREFIX = "world.playerFaction.autonomy.";

    private PlayerFactionAutonomyAuthority() {}

    static void writeAutonomyLedger(GamePanel game, Properties world) {
        if (game == null || world == null) return;
        String playerId = SaveEfficiencyAuthority.playerIdFor(game);
        String factionId = SaveEfficiencyAuthority.playerFactionIdFor(game, playerId);
        boolean founded = game.baseClaimed && !"none".equals(factionId);
        int workers = safeSize(game.factionRecruits);
        int assets = safeSize(game.baseObjects);
        int storage = safeSize(game.baseStorage);
        int security = securityCount(game);
        int production = productionAssetCount(game);
        int trade = tradeAssetCount(game);
        int labor = Math.max(0, workers - security);

        world.setProperty(PREFIX + "schema", "player-faction-autonomy-v1");
        world.setProperty(PREFIX + "authority", VERSION);
        world.setProperty(PREFIX + "factionId", factionId);
        world.setProperty(PREFIX + "active", Boolean.toString(founded));
        world.setProperty(PREFIX + "playerPresence", "current-character=" + playerId);
        world.setProperty(PREFIX + "mode", founded ? "world-owned-continuity-while-player-absent" : "inactive-no-player-founded-faction");
        world.setProperty(PREFIX + "lastEvaluatedTurn", Integer.toString(game.turn));
        world.setProperty(PREFIX + "baseRoom", Integer.toString(game.claimedRoomId));
        world.setProperty(PREFIX + "basePosition", game.baseX + "," + game.baseY);
        world.setProperty(PREFIX + "workerCount", Integer.toString(workers));
        world.setProperty(PREFIX + "laborCount", Integer.toString(labor));
        world.setProperty(PREFIX + "securityCount", Integer.toString(security));
        world.setProperty(PREFIX + "assetCount", Integer.toString(assets));
        world.setProperty(PREFIX + "storageCount", Integer.toString(storage));
        world.setProperty(PREFIX + "productionAssetCount", Integer.toString(production));
        world.setProperty(PREFIX + "tradeAssetCount", Integer.toString(trade));
        world.setProperty(PREFIX + "productionPlan", productionPlan(founded, production, labor, storage));
        world.setProperty(PREFIX + "tradePlan", tradePlan(founded, trade, storage));
        world.setProperty(PREFIX + "defensePlan", defensePlan(founded, security, assets));
        world.setProperty(PREFIX + "newsPlan", newsPlan(founded, workers, assets, game.claimedRoomId));
        world.setProperty(PREFIX + "npcCommandContinuity", founded ? npcContinuity(game) : "none");
        world.setProperty(PREFIX + "playerCommandContinuity", founded ? playerContinuity(world) : "none");
        world.setProperty(PREFIX + "commandParity", founded ? PlayerNpcCommandParityAuthority.summaryFromWorld(world) : "none");
        world.setProperty(PREFIX + "summary", summaryFromWorld(world));
    }

    static String summary(GamePanel game) {
        Properties world = SaveEfficiencyAuthority.worldRuntimeProperties(game);
        return summaryFromWorld(world);
    }

    static String summaryFromWorld(Properties world) {
        if (world == null) return "Player faction autonomy: unavailable.";
        String active = world.getProperty(PREFIX + "active", "false");
        String faction = world.getProperty(PREFIX + "factionId", "none");
        String workers = world.getProperty(PREFIX + "workerCount", "0");
        String assets = world.getProperty(PREFIX + "assetCount", "0");
        String production = world.getProperty(PREFIX + "productionPlan", "none");
        String trade = world.getProperty(PREFIX + "tradePlan", "none");
        String defense = world.getProperty(PREFIX + "defensePlan", "none");
        return "Player faction autonomy: active=" + active + " faction=" + faction + " workers=" + workers
                + " assets=" + assets + " production=" + production + " trade=" + trade + " defense=" + defense
                + "; " + PlayerFactionAutonomousTickAuthority.summaryFromWorld(world);
    }

    static String assignmentSummary(GamePanel game) {
        Properties world = SaveEfficiencyAuthority.worldRuntimeProperties(game);
        ArrayList<String> ids = new ArrayList<>(PlayerFactionWorldAuthority.reservedPlayerIds(world));
        String idSummary = ids.isEmpty() ? "none" : String.join(",", ids);
        return summaryFromWorld(world) + " Reserved player IDs=" + idSummary
                + "; player roles and NPC roles remain separate command tracks; equivalent numeric rank tiers grant equivalent NPC command authority. "
                + PlayerNpcCommandParityAuthority.assignmentParitySummary(game);
    }

    private static String productionPlan(boolean founded, int production, int labor, int storage) {
        if (!founded) return "none";
        if (production > 0 && labor > 0) return "continue-assigned-production-queues";
        if (production > 0) return "hold-production-awaiting-labor";
        if (storage > 0) return "maintain-storage-and-salvage-ledger";
        return "maintain-claimed-room";
    }

    private static String tradePlan(boolean founded, int trade, int storage) {
        if (!founded) return "none";
        if (trade > 0 && storage > 0) return "continue-local-trade-from-stock";
        if (trade > 0) return "open-services-with-limited-stock";
        return "no-formal-trade-assets";
    }

    private static String defensePlan(boolean founded, int security, int assets) {
        if (!founded) return "none";
        if (security > 0) return "staffed-watch-and-room-defense";
        if (assets > 0) return "asset-lockdown-and-low-visibility";
        return "hide-and-preserve-claim";
    }

    private static String newsPlan(boolean founded, int workers, int assets, int roomId) {
        if (!founded) return "none";
        return "publish-autonomous-freehold-note-room-" + roomId + "-workers-" + workers + "-assets-" + assets;
    }

    private static String npcContinuity(GamePanel game) {
        int security = securityCount(game);
        int labor = Math.max(0, safeSize(game.factionRecruits) - security);
        return "npc-track security=" + security + " labor=" + labor + " management=separate-from-player-roles";
    }

    private static String playerContinuity(Properties world) {
        String ids = world == null ? "" : world.getProperty("world.playerFaction.memberIds", "");
        return ids == null || ids.isBlank() ? "no-reserved-player-slots" : "reserved-player-slots=" + ids;
    }

    private static int securityCount(GamePanel game) {
        if (game == null || game.factionRecruits == null) return 0;
        int n = 0;
        for (RecruitWorker r : game.factionRecruits) {
            if (r != null && "security".equalsIgnoreCase(r.duty)) n++;
        }
        return n;
    }

    private static int productionAssetCount(GamePanel game) {
        if (game == null || game.baseObjects == null) return 0;
        int n = 0;
        for (BaseObject obj : game.baseObjects) {
            if (obj == null) continue;
            char s = obj.symbol;
            if (s == 'w' || s == 'e' || s == 'f' || s == 'l' || s == 'M') n++;
        }
        return n;
    }

    private static int tradeAssetCount(GamePanel game) {
        if (game == null || game.baseObjects == null) return 0;
        int n = 0;
        for (BaseObject obj : game.baseObjects) {
            if (obj != null && obj.isBusinessAsset()) n++;
        }
        return n;
    }

    private static int safeSize(java.util.Collection<?> c) { return c == null ? 0 : c.size(); }

    static boolean isValidAssignmentRole(String role) {
        if (role == null) return false;
        String r = role.toLowerCase(Locale.ROOT);
        return PlayerFactionWorldAuthority.isValidPlayerRole(r);
    }
}

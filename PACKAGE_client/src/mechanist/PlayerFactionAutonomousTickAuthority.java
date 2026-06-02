package mechanist;

import java.util.Locale;
import java.util.Properties;

/** Converts player-founded faction autonomy plans into compact world-owned tick ledgers. */
final class PlayerFactionAutonomousTickAuthority {
    static final String VERSION = "player-faction-autonomous-tick-authority-0.9.10gu";
    private static final String PREFIX = "world.playerFaction.autonomy.tick.";

    private PlayerFactionAutonomousTickAuthority() {}

    static void writeTickLedger(GamePanel game, Properties world) {
        if (game == null || world == null) return;
        int currentTurn = Math.max(0, game.turn);
        String factionId = world.getProperty("world.playerFaction.id", SaveEfficiencyAuthority.playerFactionIdFor(game, SaveEfficiencyAuthority.playerIdFor(game)));
        boolean active = Boolean.parseBoolean(world.getProperty("world.playerFaction.exists", Boolean.toString(game.baseClaimed)))
                && factionId != null && !factionId.isBlank() && !"none".equals(factionId);
        TickInputs inputs = readInputs(game, world, active, currentTurn);
        TickOutcome outcome = resolve(inputs);
        writeOutcome(world, inputs, outcome, "runtime-save");
    }

    static String previewTick(GamePanel game, int requestedTurns) {
        Properties world = SaveEfficiencyAuthority.worldRuntimeProperties(game);
        int currentTurn = game == null ? 0 : Math.max(0, game.turn);
        int safeTurns = Math.max(1, Math.min(10_000, requestedTurns));
        TickInputs inputs = readInputs(game, world, Boolean.parseBoolean(world.getProperty("world.playerFaction.exists", "false")), currentTurn + safeTurns);
        TickOutcome outcome = resolve(inputs);
        writeOutcome(world, inputs, outcome, "admin-preview");
        return summaryFromWorld(world);
    }

    static String summary(GamePanel game) {
        return summaryFromWorld(SaveEfficiencyAuthority.worldRuntimeProperties(game));
    }

    static String summaryFromWorld(Properties world) {
        if (world == null) return "Player faction autonomous tick: unavailable.";
        return "Player faction autonomous tick: faction=" + world.getProperty(PREFIX + "factionId", "none")
                + " active=" + world.getProperty(PREFIX + "active", "false")
                + " targetTurn=" + world.getProperty(PREFIX + "targetTurn", "0")
                + " elapsed=" + world.getProperty(PREFIX + "elapsedTurns", "0")
                + " production=" + world.getProperty(PREFIX + "productionDelta", "0")
                + " trade=" + world.getProperty(PREFIX + "tradeDelta", "0")
                + " defense=" + world.getProperty(PREFIX + "defenseDelta", "0")
                + " morale=" + world.getProperty(PREFIX + "moraleDelta", "0")
                + " stock=" + world.getProperty(PREFIX + "stockEstimate", "0")
                + " risk=" + world.getProperty(PREFIX + "risk", "none")
                + ".";
    }

    private static TickInputs readInputs(GamePanel game, Properties world, boolean active, int targetTurn) {
        int previousTurn = readInt(world, PREFIX + "targetTurn", readInt(world, "world.playerFaction.autonomy.lastEvaluatedTurn", game == null ? 0 : game.turn));
        int elapsed = Math.max(0, targetTurn - previousTurn);
        int workers = readInt(world, "world.playerFaction.autonomy.workerCount", game == null || game.factionRecruits == null ? 0 : game.factionRecruits.size());
        int labor = readInt(world, "world.playerFaction.autonomy.laborCount", Math.max(0, workers));
        int security = readInt(world, "world.playerFaction.autonomy.securityCount", 0);
        int productionAssets = readInt(world, "world.playerFaction.autonomy.productionAssetCount", 0);
        int tradeAssets = readInt(world, "world.playerFaction.autonomy.tradeAssetCount", 0);
        int storage = readInt(world, "world.playerFaction.autonomy.storageCount", 0);
        int assets = readInt(world, "world.playerFaction.autonomy.assetCount", 0);
        String factionId = world == null ? "none" : world.getProperty("world.playerFaction.id", "none");
        String playerIds = world == null ? "" : world.getProperty("world.playerFaction.memberIds", "");
        return new TickInputs(active, factionId, targetTurn, previousTurn, elapsed, workers, labor, security, productionAssets, tradeAssets, storage, assets, playerIds);
    }

    private static TickOutcome resolve(TickInputs in) {
        if (!in.active) return new TickOutcome(0, 0, 0, 0, 0, "inactive", "no-player-founded-faction");
        int effectiveTurns = Math.min(240, Math.max(0, in.elapsedTurns));
        int production = effectiveTurns * Math.max(0, Math.min(in.labor, in.productionAssets));
        int trade = effectiveTurns * Math.max(0, Math.min(in.tradeAssets, Math.max(1, in.storage)));
        int defense = effectiveTurns * Math.max(0, in.security);
        int morale = effectiveTurns * Math.max(0, Math.min(in.workers, 8));
        int stock = Math.max(0, in.storage * 10 + production - Math.max(0, trade / 2));
        String risk = risk(in, defense, stock);
        String news = news(in, production, trade, defense, risk);
        return new TickOutcome(production, trade, defense, morale, stock, risk, news);
    }

    private static String risk(TickInputs in, int defense, int stock) {
        if (in.security <= 0 && in.assets > 0) return "unguarded-assets";
        if (stock <= 0 && in.productionAssets > 0) return "stock-starved-production";
        if (defense >= Math.max(1, in.elapsedTurns * 2)) return "guarded";
        return "ordinary";
    }

    private static String news(TickInputs in, int production, int trade, int defense, String risk) {
        String base = sanitize(in.factionId);
        if (!in.active) return "none";
        if (production > trade && production > 0) return base + " reports continued workshop output while command is absent";
        if (trade > 0) return base + " continues local exchange under standing orders";
        if (defense > 0) return base + " maintains watches and preserves its claim";
        return base + " holds position under autonomous standing orders risk=" + risk;
    }

    private static void writeOutcome(Properties world, TickInputs in, TickOutcome out, String source) {
        if (world == null) return;
        world.setProperty(PREFIX + "schema", "player-faction-autonomous-tick-v1");
        world.setProperty(PREFIX + "authority", VERSION);
        world.setProperty(PREFIX + "source", source);
        world.setProperty(PREFIX + "active", Boolean.toString(in.active));
        world.setProperty(PREFIX + "factionId", in.factionId);
        world.setProperty(PREFIX + "previousTurn", Integer.toString(in.previousTurn));
        world.setProperty(PREFIX + "targetTurn", Integer.toString(in.targetTurn));
        world.setProperty(PREFIX + "elapsedTurns", Integer.toString(in.elapsedTurns));
        world.setProperty(PREFIX + "boundedTurns", Integer.toString(Math.min(240, Math.max(0, in.elapsedTurns))));
        world.setProperty(PREFIX + "workerCount", Integer.toString(in.workers));
        world.setProperty(PREFIX + "laborCount", Integer.toString(in.labor));
        world.setProperty(PREFIX + "securityCount", Integer.toString(in.security));
        world.setProperty(PREFIX + "productionAssetCount", Integer.toString(in.productionAssets));
        world.setProperty(PREFIX + "tradeAssetCount", Integer.toString(in.tradeAssets));
        world.setProperty(PREFIX + "reservedPlayerIds", in.playerIds == null ? "" : in.playerIds);
        world.setProperty(PREFIX + "productionDelta", Integer.toString(out.productionDelta));
        world.setProperty(PREFIX + "tradeDelta", Integer.toString(out.tradeDelta));
        world.setProperty(PREFIX + "defenseDelta", Integer.toString(out.defenseDelta));
        world.setProperty(PREFIX + "moraleDelta", Integer.toString(out.moraleDelta));
        world.setProperty(PREFIX + "stockEstimate", Integer.toString(out.stockEstimate));
        world.setProperty(PREFIX + "risk", out.risk);
        world.setProperty(PREFIX + "publicNote", out.publicNote);
        world.setProperty(PREFIX + "summary", summaryFromWorld(world));
    }

    private static int readInt(Properties p, String key, int fallback) {
        if (p == null || key == null) return fallback;
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()); }
        catch (Exception ignored) { return fallback; }
    }

    private static String sanitize(String text) {
        if (text == null || text.isBlank()) return "unaffiliated";
        return text.toLowerCase(Locale.ROOT).replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private record TickInputs(boolean active, String factionId, int targetTurn, int previousTurn, int elapsedTurns,
                              int workers, int labor, int security, int productionAssets, int tradeAssets,
                              int storage, int assets, String playerIds) { }
    private record TickOutcome(int productionDelta, int tradeDelta, int defenseDelta, int moraleDelta,
                               int stockEstimate, String risk, String publicNote) { }
}

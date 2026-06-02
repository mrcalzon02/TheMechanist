package mechanist;

import java.awt.*;
import java.util.*;

/**
 * Named authority for staged, per-tile construction progress.
 *
 * Construction sites are intentionally represented as normal BaseObject records
 * with under-construction metadata so existing placement, persistence, and map
 * inspection paths can see them without adding a second live-world object list.
 */
final class ProgressiveConstructionAuthority {
    static final String VERSION = "0.9.10hm";
    private static final Color GHOST_BLUE = new Color(175, 220, 255);

    private ProgressiveConstructionAuthority() {}

    static BaseObject createSite(BuildRecipe recipe, int x, int y, int requiredLabor) {
        BaseObject site = new BaseObject("Under construction: " + clean(recipe == null ? null : recipe.name, "unknown structure"), '?', x, y,
                recipe == null ? 0 : recipe.supplyCost, recipe == null ? 0 : recipe.attention);
        site.underConstruction = true;
        site.finalSymbol = recipe == null ? '?' : recipe.symbol;
        site.constructionRequiredItems = encodeRequirements(recipe);
        site.constructionInsertedItems = "";
        site.constructionLaborRequired = Math.max(1, requiredLabor);
        site.constructionLaborDone = 0;
        site.constructionVisualProgress = 0;
        site.description = "A staged construction site. Components and labor can be contributed over time; the ghost remains collisionless in doctrine, but this placed site reserves the tile until complete or dismantled.";
        site.qualityName = recipe == null ? "Common" : recipe.qualityName;
        site.faction = recipe == null ? Faction.NONE : recipe.requiredFaction;
        site.assignedRecipe = recipe == null ? "" : recipe.name;
        return site;
    }

    static int contribute(GamePanel g, BaseObject site, int laborTurns, boolean allowMaterialPull) {
        if (g == null || site == null || !site.underConstruction) return 0;
        int inserted = allowMaterialPull ? insertAvailableMaterials(g, site, 32) : 0;
        int labor = Math.max(0, laborTurns) * toolLaborMultiplier(g);
        if (materialsComplete(site)) site.constructionLaborDone = Math.min(site.constructionLaborRequired, site.constructionLaborDone + labor);
        site.constructionVisualProgress = progressPercent(site);
        if (complete(site)) finalizeSite(g, site);
        return inserted;
    }

    static int toolLaborMultiplier(GamePanel g) {
        String hands = ((g == null ? "" : g.equippedLeftHandItem) + " " + (g == null ? "" : g.equippedRightHandItem)).toLowerCase(Locale.ROOT);
        if (hands.contains("multi-tool") || hands.contains("power tool") || hands.contains("servo") || hands.contains("welder")) return 3;
        if (hands.contains("hammer") || hands.contains("axe") || hands.contains("pick") || hands.contains("spade") || hands.contains("shovel") || hands.contains("crowbar") || hands.contains("knife") || hands.contains("bayonet") || hands.contains("chain") || hands.contains("saw")) return 2;
        return 1;
    }

    static int deconstructionTurnsForTile(char tile, GamePanel g) {
        int base = switch (tile) {
            case '#', 'W', 'D', 'Y' -> 5;
            case '+', '/', '\\' -> 3;
            case '~', '≈' -> 4;
            default -> 2;
        };
        return Math.max(1, (int)Math.ceil(base / (double)toolLaborMultiplier(g)));
    }

    static Color constructionOverlayColor(BaseObject site, Color builtColor) {
        int p = site == null ? 0 : Math.max(0, Math.min(100, site.constructionVisualProgress));
        float t = p / 100.0f;
        Color target = builtColor == null ? new Color(170, 230, 155) : builtColor;
        int r = (int)(GHOST_BLUE.getRed() * (1f - t) + target.getRed() * t);
        int g = (int)(GHOST_BLUE.getGreen() * (1f - t) + target.getGreen() * t);
        int b = (int)(GHOST_BLUE.getBlue() * (1f - t) + target.getBlue() * t);
        return new Color(clamp(r), clamp(g), clamp(b), 120);
    }

    static String progressLine(BaseObject site) {
        if (site == null || !site.underConstruction) return "No staged construction site selected.";
        return site.name + " progress=" + progressPercent(site) + "% materials=" + materialProgress(site)
                + " labor=" + site.constructionLaborDone + "/" + site.constructionLaborRequired
                + " missing=" + missingMaterials(site);
    }

    static String auditSummary(GamePanel g) {
        int active = 0;
        ArrayList<String> lines = new ArrayList<>();
        if (g != null && g.baseObjects != null) {
            for (BaseObject obj : g.baseObjects) if (obj != null && obj.underConstruction) {
                active++;
                if (lines.size() < 3) lines.add(progressLine(obj));
            }
        }
        return "progressiveConstruction version=" + VERSION + " activeSites=" + active + " toolMultiplier=" + toolLaborMultiplier(g)
                + " stagedMaterials=true perTileProgressBars=true ghostBlueToBuiltColor=true deconstructionTurnsWithTools=true"
                + (lines.isEmpty() ? "" : " | " + String.join(" | ", lines));
    }

    private static int insertAvailableMaterials(GamePanel g, BaseObject site, int maxUnits) {
        Map<String,Integer> req = decode(site.constructionRequiredItems);
        Map<String,Integer> ins = decode(site.constructionInsertedItems);
        int moved = 0;
        for (Map.Entry<String,Integer> e : req.entrySet()) {
            String item = e.getKey();
            int need = Math.max(0, e.getValue() - ins.getOrDefault(item, 0));
            while (need > 0 && moved < maxUnits) {
                if (!consumeOne(g, item)) break;
                ins.merge(item, 1, Integer::sum);
                need--; moved++;
            }
            if (moved >= maxUnits) break;
        }
        site.constructionInsertedItems = encode(ins);
        return moved;
    }

    private static boolean consumeOne(GamePanel g, String item) {
        if (g == null || item == null) return false;
        if (item.equalsIgnoreCase("Construction supplies")) {
            if (g.supplies <= 0) return false;
            g.supplies--;
            return true;
        }
        if (item.equalsIgnoreCase("Machine part") || item.equalsIgnoreCase("Machine parts")) {
            if (g.machineParts <= 0) return false;
            g.machineParts--;
            return true;
        }
        ProductionInputConsumptionRecord rec = g.consumeProductionInputNamedResult(item, "staged construction insertion");
        return rec != null && rec.success;
    }

    private static boolean materialsComplete(BaseObject site) {
        Map<String,Integer> req = decode(site.constructionRequiredItems);
        Map<String,Integer> ins = decode(site.constructionInsertedItems);
        for (Map.Entry<String,Integer> e : req.entrySet()) if (ins.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        return true;
    }

    private static boolean complete(BaseObject site) {
        return site != null && site.underConstruction && materialsComplete(site) && site.constructionLaborDone >= site.constructionLaborRequired;
    }

    private static void finalizeSite(GamePanel g, BaseObject site) {
        if (g == null || site == null || !site.underConstruction) return;
        site.name = site.name.replaceFirst("^Under construction: ", "");
        site.symbol = site.finalSymbol == 0 ? '?' : site.finalSymbol;
        site.underConstruction = false;
        site.constructionVisualProgress = 100;
        g.configureBaseObject(site);
        LiveProductionPlacementAuthority.annotateBuiltObject(g, site);
        g.runCrafted++;
        g.gainXp("Mechanics", 4, "completed staged construction");
        g.recordPlayerNewsEvent("facility-built", site.name, site.faction, "staged facility completed in claimed space at " + site.x + "," + site.y, 2 + Math.max(0, site.attention));
        g.logEvent("Construction complete: " + site.name + " at " + site.x + "," + site.y + ".");
        DebugLog.audit("STAGED_BUILD_COMPLETE", "site=" + site.name + " at=" + site.x + "," + site.y + " " + auditSummary(g));
    }

    private static String encodeRequirements(BuildRecipe recipe) {
        TreeMap<String,Integer> req = new TreeMap<>();
        if (recipe == null) return "";
        if (recipe.supplyCost > 0) req.put("Construction supplies", recipe.supplyCost);
        if (recipe.partCost > 0) req.put("Machine part", recipe.partCost);
        if (recipe.componentCosts != null) for (Map.Entry<String,Integer> e : recipe.componentCosts.entrySet()) if (e.getValue() != null && e.getValue() > 0) req.merge(e.getKey(), e.getValue(), Integer::sum);
        return encode(req);
    }

    private static String encode(Map<String,Integer> map) {
        if (map == null || map.isEmpty()) return "";
        ArrayList<String> parts = new ArrayList<>();
        for (Map.Entry<String,Integer> e : new TreeMap<>(map).entrySet()) parts.add(esc(e.getKey()) + "=" + Math.max(0, e.getValue()));
        return String.join(";", parts);
    }

    private static Map<String,Integer> decode(String encoded) {
        TreeMap<String,Integer> out = new TreeMap<>();
        if (encoded == null || encoded.isBlank()) return out;
        for (String part : encoded.split(";")) {
            if (part.isBlank()) continue;
            String[] a = part.split("=", 2);
            if (a.length != 2) continue;
            try { out.put(unesc(a[0]), Math.max(0, Integer.parseInt(a[1]))); } catch (Exception ignored) {}
        }
        return out;
    }

    private static int progressPercent(BaseObject site) {
        if (site == null) return 0;
        Map<String,Integer> req = decode(site.constructionRequiredItems);
        Map<String,Integer> ins = decode(site.constructionInsertedItems);
        int required = 0, inserted = 0;
        for (Map.Entry<String,Integer> e : req.entrySet()) {
            required += Math.max(0, e.getValue());
            inserted += Math.min(Math.max(0, e.getValue()), Math.max(0, ins.getOrDefault(e.getKey(), 0)));
        }
        int materialPct = required <= 0 ? 100 : (inserted * 100) / Math.max(1, required);
        int laborPct = site.constructionLaborRequired <= 0 ? 100 : (Math.max(0, site.constructionLaborDone) * 100) / Math.max(1, site.constructionLaborRequired);
        return Math.max(0, Math.min(100, (materialPct * 65 + laborPct * 35) / 100));
    }

    private static String materialProgress(BaseObject site) {
        Map<String,Integer> req = decode(site.constructionRequiredItems);
        Map<String,Integer> ins = decode(site.constructionInsertedItems);
        if (req.isEmpty()) return "none";
        ArrayList<String> lines = new ArrayList<>();
        for (Map.Entry<String,Integer> e : req.entrySet()) lines.add(e.getKey() + " " + ins.getOrDefault(e.getKey(), 0) + "/" + e.getValue());
        return String.join(", ", lines);
    }

    private static String missingMaterials(BaseObject site) {
        Map<String,Integer> req = decode(site.constructionRequiredItems);
        Map<String,Integer> ins = decode(site.constructionInsertedItems);
        ArrayList<String> miss = new ArrayList<>();
        for (Map.Entry<String,Integer> e : req.entrySet()) {
            int n = e.getValue() - ins.getOrDefault(e.getKey(), 0);
            if (n > 0) miss.add(e.getKey() + " x" + n);
        }
        return miss.isEmpty() ? "none" : String.join(", ", miss);
    }

    private static String esc(String s) { return (s == null ? "" : s).replace("\\", "\\\\").replace(";", "\\s").replace("=", "\\e").replace("|", "\\p"); }
    private static String unesc(String s) { return (s == null ? "" : s).replace("\\p", "|").replace("\\e", "=").replace("\\s", ";").replace("\\\\", "\\"); }
    private static String clean(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.trim(); }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}

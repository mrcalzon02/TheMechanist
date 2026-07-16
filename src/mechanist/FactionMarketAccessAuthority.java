package mechanist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shared legality and access decision for visible faction-market offers. */
final class FactionMarketAccessAuthority {
    record AccessContext(Faction playerFaction, int standing, int gangHeat, int suspicion,
                         Set<String> skillNodes, Set<String> knowledges, Collection<String> carriedItems,
                         World world, long worldTurn) {
        AccessContext {
            playerFaction = playerFaction == null ? Faction.NONE : playerFaction;
            skillNodes = skillNodes == null ? Set.of() : skillNodes;
            knowledges = knowledges == null ? Set.of() : knowledges;
            carriedItems = carriedItems == null ? List.of() : carriedItems;
        }
    }

    record Decision(String legalClass, boolean allowed, String requirement, String consequence,
                    String eventNotice) {
        String purchaseBlock() { return allowed ? "" : requirement; }
        String rowTag() {
            if (!allowed) return "[LOCKED] ";
            String low = legalClass.toLowerCase(Locale.ROOT);
            return contains(low, "illicit", "forbidden", "stolen", "counterfeit", "black-market") ? "[RISK] " : "";
        }
        List<String> lines() {
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Market class: " + legalClass + ".");
            lines.add((allowed ? "Access available: " : "Access blocked: ") + requirement + ".");
            lines.add("Trade consequence: " + consequence + ".");
            if (eventNotice != null && !eventNotice.isBlank()) lines.add("Event rule: " + eventNotice + ".");
            return lines;
        }
    }

    private FactionMarketAccessAuthority() {}

    static Decision evaluate(TraderSession trader, TradeOffer offer, AccessContext context) {
        AccessContext c = context == null
                ? new AccessContext(Faction.NONE, 0, 0, 0, Set.of(), Set.of(), List.of(), null, 0L)
                : context;
        if (offer == null) return new Decision("unknown stock", false, "select an offer", "nothing changes", "");

        Faction vendor = trader == null || trader.marketFaction == null ? Faction.NONE : trader.marketFaction;
        String channel = safe(trader == null ? "" : trader.marketCategory).toLowerCase(Locale.ROOT);
        String text = offerText(offer);
        String legalText = offerLegalText(offer);
        boolean member = sameFactionFamily(vendor, c.playerFaction());
        boolean credential = hasCredential(c);
        boolean patronage = credential || member || c.standing() >= 10;
        boolean trusted = credential || member || c.standing() >= 25;
        boolean blackMarket = contains(channel, "black-market", "illicit")
                || contains(safe(trader == null ? "" : trader.archetype).toLowerCase(Locale.ROOT), "black-market", "fence", "smuggler");
        boolean hostile = vendor != Faction.NONE && !member && c.standing() <= -25;
        boolean narcotic = contains(text, "narcotic", "street stimm", "grin powder", "night milk", "pearl obscura",
                "witchsalt", "black badge", "combat stimulant", "dream", "chem/ganger", "chem/cult", "chem/noble");
        boolean draught = !safe(offer.draughtCustodyId).isBlank() || text.contains("chem/rare-campaign") || text.contains("draught");
        boolean forbidden = contains(legalText, "chem/cult", "cultist", "cult-made", "cult-produced",
                "cult contraband", "cult ritual", "cult faction", "heretic", "warp", "blasphem", "forbidden");
        boolean contraband = forbidden || contains(legalText, "contraband", "illegal", "black-market", "smuggl");
        boolean disputed = contains(legalText, "stolen", "counterfeit", "misdeclared", "diluted", "contaminated");
        boolean explicitBlueprint = !safe(offer.constructionBlueprintId).isBlank()
                || contains(legalText,
                "blueprint", "licensed plan", "construction folio");
        boolean essential = !explicitBlueprint && !narcotic && essentialOffer(offer);
        boolean military = !essential && (channel.contains("armory") || !safe(offer.securitySupplyReserveId).isBlank()
                || contains(text, "military", "regulated", "munition", "ammo", "explosive", "guard flak", "suppression shell"));
        boolean noble = !essential && (channel.contains("luxury")
                || contains(text, "noble", "house-certified", "estate luxury", "private medicine"));
        boolean blueprint = explicitBlueprint || (!essential && channel.contains("blueprint"));
        boolean controlledMedical = !essential && (narcotic
                || contains(text, "controlled medicine", "restricted medicine", "interrogation dosing"));
        String eventNotice = eventNotice(c.world(), c.worldTurn(), offer, blackMarket);

        Decision operational = operationalRestriction(trader, vendor, c, member,
                military, noble, blueprint, controlledMedical, essential, eventNotice);
        if (operational != null) return operational;
        if (hostile) {
            return blocked("faction-closed market", "the vendor faction is hostile at standing " + c.standing(),
                    "improve standing or end active hostility before trading", eventNotice);
        }
        if (quarantineBlocks(c.world(), c.worldTurn(), controlledMedical, blackMarket)) {
            return blocked("event-restricted medical resale", "the active quarantine suspends unrestricted narcotics and controlled-medicine resale",
                    "clinic treatment and relief medicine remain available; black-market channels may ignore the order", eventNotice);
        }
        if (draught) {
            if (blackMarket) return allowed("exceptional black-market draught custody",
                    "the custody holder explicitly released this item through an illicit channel",
                    "purchase carries severe theft, authenticity, and faction-reprisal risk", eventNotice);
            if (trusted) return allowed("noble-only exceptional transfer",
                    "trusted standing, faction membership, invitation, or senior access papers satisfy the release gate",
                    "the custody record and political ownership remain attached after purchase", eventNotice);
            return blocked("noble-only exceptional transfer",
                    "requires trusted standing 25, noble patronage, faction membership, or senior access papers",
                    "the draught remains visible as protected custody rather than ordinary shelf stock", eventNotice);
        }
        if (contraband) {
            if (!blackMarket) return blocked(forbidden ? "forbidden contraband" : "illicit contraband",
                    "this legal vendor channel cannot transfer contraband; locate a black-market counter",
                    "possession can create severe legal and faction consequences", eventNotice);
            boolean underworldAccess = c.standing() >= -5 || c.gangHeat() >= 10 || hasUnderworldCredential(c);
            if (!underworldAccess) return blocked("black-market restricted goods",
                    "requires non-strained dealer standing, underworld credibility, or Streetwise Appraisal",
                    "a successful purchase remains illegal and may attract enforcement", eventNotice);
            return allowed(forbidden ? "forbidden black-market goods" : "illicit black-market goods",
                    "the dealer accepts your standing or underworld credibility",
                    "possession remains illegal; provenance can expose the buyer and supplier", eventNotice);
        }
        if (controlledMedical) {
            if (blackMarket) return allowed("restricted black-market narcotic",
                    "the illicit vendor accepts local script without a medical permit",
                    "the sale supports the faction chem economy and carries addiction, enforcement, and provenance risk", eventNotice);
            if (patronage || c.standing() >= 5) return allowed(noble ? "noble private medicine" : "license-gated controlled medicine",
                    "favorable standing, faction membership, patronage, or permit access satisfies the controlled-sale gate",
                    "the controlled batch remains traceable after purchase", eventNotice);
            return blocked(noble ? "noble private medicine" : "license-gated controlled medicine",
                    "requires favorable standing 5, faction membership, patronage, or medical/commerce access papers",
                    "ordinary treatment stock remains available", eventNotice);
        }
        if (military) {
            if (c.suspicion() >= 60 && !member) return blocked("military and security restricted goods",
                    "current suspicion " + c.suspicion() + " triggers an inspection hold on controlled issue",
                    "reduce suspicion or use a channel that does not enforce faction issue policy", eventNotice);
            if (patronage) return allowed("military and security restricted goods",
                    "favorable standing, faction membership, or permit access authorizes controlled issue",
                    "weapons and ammunition remain regulated and provenance-traceable", eventNotice);
            return blocked("military and security restricted goods",
                    "requires favorable standing 10, faction membership, or faction/permit access",
                    "basic provisions and ordinary tools remain available", eventNotice);
        }
        if (noble) {
            if (c.standing() >= 0 || patronage) return allowed("noble broker luxury",
                    "non-hostile standing, faction membership, invitation, patronage, or licensed commerce grants access",
                    "luxury provenance and house ownership remain visible", eventNotice);
            return blocked("reputation-gated noble broker luxury",
                    "requires neutral standing 0, noble invitation, patronage, or licensed commerce",
                    "the broker keeps the item visible as a known house good", eventNotice);
        }
        if (blueprint) {
            if (member || credential || c.standing() >= 5) return allowed("faction-only licensed blueprint",
                    "faction membership, favorable standing, or permit-based service access satisfies the license gate",
                    "the purchased blueprint remains tied to its issuing faction and facility", eventNotice);
            return blocked("faction-only licensed blueprint",
                    "requires favorable standing 5, faction membership, or permit-based service access",
                    "ordinary tools and construction materials remain available", eventNotice);
        }
        if (disputed) return allowed("stolen, counterfeit, or disputed-provenance goods",
                "the vendor offers the item openly but does not erase its recorded provenance",
                "appraisal, enforcement, and later buyers may react to the recorded risk", eventNotice);
        if (isReliefRation(c.world(), c.worldTurn(), offer)) return allowed("event-rationed relief stock",
                "local residents and faction dependants may buy the visible ration while stock lasts",
                "finite relief reserve and ration provenance are consumed on purchase", eventNotice);
        return allowed("legal open market stock", "no special standing, rank, permit, or invitation is required",
                "the ordinary purchase transfers one item for the displayed price", eventNotice);
    }

    static String marketNotice(World world, long worldTurn) {
        if (world == null) return "";
        ArrayList<String> notices = new ArrayList<>();
        for (TopDownWorldEventRecord event : world.topDownWorldEvents) {
            if (!active(event, worldTurn)) continue;
            notices.add(event.title + ": " + event.vendorRestriction + " " + event.vendorException);
        }
        return String.join(" ", notices);
    }

    static String offMapSaleBlock(World world, long worldTurn) {
        if (world == null) return "";
        for (TopDownWorldEventRecord event : world.topDownWorldEvents) {
            if (!active(event, worldTurn) || (!event.offMapSalesClosed && !event.exportClosed)) continue;
            return event.title + " closes off-map sale settlement through world turn " + event.endWorldTurn
                    + ": " + event.vendorRestriction;
        }
        return "";
    }

    private static Decision operationalRestriction(TraderSession trader, Faction vendor,
                                                   AccessContext context, boolean member,
                                                   boolean military, boolean noble,
                                                   boolean blueprint,
                                                   boolean controlledMedical,
                                                   boolean essential,
                                                   String eventNotice) {
        NpcFactionSite site = trader == null ? null : trader.sourceSite;
        if (site != null && site.workers <= 0) {
            return blocked("unstaffed faction facility",
                    site.name + " has no effective workers available to operate this counter",
                    "the vendor remains physically present but all sales are closed until staff return",
                    eventNotice);
        }
        if (site != null && site.stock <= 0 && !essential) {
            return blocked("depleted faction-site stock",
                    site.name + " has exhausted distributable stock and is reserving its remaining essentials",
                    "food, water, basic dressings, and explicit relief stock remain the only eligible sales",
                    eventNotice);
        }
        if (site != null && site.stock <= 2 && !essential
                && (military || noble || blueprint || controlledMedical)) {
            return blocked("scarcity-restricted strategic stock",
                    site.name + " is at critical stock " + site.stock
                            + " and has suspended controlled, luxury, blueprint, and military transfers",
                    "restore faction-site supply before strategic stock returns to public issue",
                    eventNotice);
        }
        if (!essential && !member && recentFactionConflict(context.world(), context.worldTurn(), vendor)
                && (military || noble || blueprint || controlledMedical)) {
            return blocked("conflict-restricted faction market",
                    "recent seizure or salvage conflict involving " + vendor.label
                            + " suspends strategic transfers to non-members",
                    "ordinary essentials remain open; faction membership or the end of the conflict window restores controlled access",
                    eventNotice);
        }
        return null;
    }

    private static boolean recentFactionConflict(World world, long worldTurn,
                                                 Faction vendor) {
        if (world == null || vendor == null || vendor == Faction.NONE
                || world.zoneConflictLossHistory == null
                || world.zoneConflictLossHistory.isBlank()) return false;
        String faction = vendor.label.toLowerCase(Locale.ROOT);
        String normalized = vendor.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] entries = world.zoneConflictLossHistory.split(";;");
        for (int i = entries.length - 1; i >= 0; i--) {
            String entry = safe(entries[i]).toLowerCase(Locale.ROOT);
            if (!contains(entry, "live-seizure", "live-salvage")) continue;
            if (!entry.contains(faction) && !entry.contains(normalized)) continue;
            long eventTurn = tokenLong(entry, "turn=", -1L);
            if (eventTurn < 0L) return true;
            long age = Math.max(0L, worldTurn - eventTurn);
            return age <= 12L * Math.max(1, GamePanel.TURNS_PER_HOUR);
        }
        return false;
    }

    private static long tokenLong(String text, String token, long fallback) {
        if (text == null || token == null) return fallback;
        int start = text.indexOf(token);
        if (start < 0) return fallback;
        start += token.length();
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        if (end <= start) return fallback;
        try { return Long.parseLong(text.substring(start, end)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static boolean essentialOffer(TradeOffer offer) {
        if (offer == null) return false;
        if (!safe(offer.essentialSupplyReserveId).isBlank()) return true;
        if (!safe(offer.constructionBlueprintId).isBlank()
                || !safe(offer.securitySupplyReserveId).isBlank()
                || !safe(offer.nobleLuxuryReserveId).isBlank()
                || !safe(offer.draughtCustodyId).isBlank()) return false;
        String category = safe(offer.category).toLowerCase(Locale.ROOT);
        if (containsTerm(category,
                "luxury", "blueprint", "narcotic", "controlled", "contraband")) return false;
        String name = safe(offer.name).toLowerCase(Locale.ROOT);
        if ("food".equals(category) || category.startsWith("food/")
                || "water".equals(category) || category.startsWith("water/")) return true;
        return containsTerm(name,
                "emergency ration", "emergency rations", "ration", "rations",
                "food", "water", "clean water", "bandage", "bandages",
                "field dressing", "field dressings", "antiseptic",
                "basic medicine", "relief", "animal feed");
    }

    private static boolean containsTerm(String text, String... terms) {
        if (text == null || text.isBlank() || terms == null) return false;
        for (String term : terms) {
            if (term == null || term.isBlank()) continue;
            int from = 0;
            while (from < text.length()) {
                int index = text.indexOf(term, from);
                if (index < 0) break;
                int end = index + term.length();
                boolean leftBoundary = index == 0
                        || !Character.isLetterOrDigit(text.charAt(index - 1));
                boolean rightBoundary = end >= text.length()
                        || !Character.isLetterOrDigit(text.charAt(end));
                if (leftBoundary && rightBoundary) return true;
                from = index + 1;
            }
        }
        return false;
    }

    private static String eventNotice(World world, long worldTurn, TradeOffer offer, boolean blackMarket) {
        if (world == null) return "";
        ArrayList<String> notices = new ArrayList<>();
        for (TopDownWorldEventRecord event : world.topDownWorldEvents) {
            if (!active(event, worldTurn)) continue;
            if ("EXPORT_BAN".equals(event.eventType) || event.exportClosed || event.offMapSalesClosed) {
                notices.add("off-map settlement is closed, but this local/internal vendor sale remains exempt");
            } else if ("TITHING_DECREE".equals(event.eventType)) {
                notices.add("the market is under a temporary civic tithe; direct relief and internal issue remain exempt");
            } else if ("QUARANTINE".equals(event.eventType) && blackMarket) {
                notices.add("this illicit channel ignores the quarantine resale order at increased legal risk");
            } else if (isReliefText(offerText(offer)) && "RELIEF_SHIPMENT".equals(event.eventType)) {
                notices.add("relief distribution is rationed while local/internal commerce remains open");
            }
        }
        return String.join(" ", notices);
    }

    private static boolean quarantineBlocks(World world, long worldTurn, boolean controlledMedical, boolean blackMarket) {
        if (!controlledMedical || blackMarket || world == null) return false;
        for (TopDownWorldEventRecord event : world.topDownWorldEvents)
            if (active(event, worldTurn) && "QUARANTINE".equals(event.eventType)) return true;
        return false;
    }

    private static boolean isReliefRation(World world, long worldTurn, TradeOffer offer) {
        if (world == null || !isReliefText(offerText(offer))) return false;
        for (TopDownWorldEventRecord event : world.topDownWorldEvents)
            if (active(event, worldTurn) && "RELIEF_SHIPMENT".equals(event.eventType)) return true;
        return false;
    }

    private static boolean active(TopDownWorldEventRecord event, long turn) {
        return event != null && "ACTIVE".equals(event.status) && turn >= event.startWorldTurn && turn < event.endWorldTurn;
    }

    private static boolean hasCredential(AccessContext c) {
        return containsCollection(c.knowledges(), "licensed commerce", "commerce permit", "permit based access",
                "faction access", "faction service access", "district permit", "noble invitation", "patronage")
                || containsCollection(c.carriedItems(), "permit form", "commerce permit", "noble invitation", "service charter");
    }

    private static boolean hasUnderworldCredential(AccessContext c) {
        return containsCollection(c.skillNodes(), "streetwise", "underworld", "black-market")
                || containsCollection(c.knowledges(), "underhive", "streetwise", "smuggling");
    }

    private static boolean containsCollection(Collection<String> values, String... terms) {
        if (values == null) return false;
        for (String value : values) {
            String low = safe(value).toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
            if (contains(low, terms)) return true;
        }
        return false;
    }

    private static String offerText(TradeOffer offer) {
        StringBuilder text = new StringBuilder();
        text.append(offerLegalText(offer));
        ItemDef def = ItemCatalog.get(offer.name);
        if (def != null) text.append(' ').append(safe(def.category)).append(' ').append(safe(def.source)).append(' ').append(safe(def.use));
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private static String offerLegalText(TradeOffer offer) {
        StringBuilder text = new StringBuilder();
        text.append(safe(offer.name)).append(' ').append(safe(offer.category)).append(' ').append(safe(offer.description));
        if (offer.provenance != null) text.append(' ').append(safe(offer.provenance.batchIssueTags))
                .append(' ').append(safe(offer.provenance.productionLegalStatus)).append(' ').append(safe(offer.provenance.chain));
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean sameFactionFamily(Faction a, Faction b) {
        if (a == null || b == null || a == Faction.NONE || b == Faction.NONE) return false;
        return FactionInventoryStockAuthority.normalizeFaction(a) == FactionInventoryStockAuthority.normalizeFaction(b);
    }

    private static boolean isReliefText(String text) { return contains(text, "relief", "emergency allocation", "ration reserve"); }
    private static Decision allowed(String type, String requirement, String consequence, String event) {
        return new Decision(type, true, requirement, consequence, event);
    }
    private static Decision blocked(String type, String requirement, String consequence, String event) {
        return new Decision(type, false, requirement, consequence, event);
    }
    private static boolean contains(String text, String... terms) {
        for (String term : terms) if (term != null && !term.isBlank() && text.contains(term)) return true;
        return false;
    }
    private static String safe(String value) { return value == null ? "" : value; }
}

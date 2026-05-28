package mechanist.modapi.examples;

import mechanist.modapi.AlignmentVector;
import mechanist.modapi.DiplomacyChange;
import mechanist.modapi.DiplomaticSignal;
import mechanist.modapi.FactionProfile;
import mechanist.modapi.ModIntegrationHook;
import mechanist.modapi.SimulationContext;
import mechanist.modapi.SimulationEvent;

/** Faction Editor example: cybernetic collectors with synthetic economy and hostile diplomacy vectors. */
public final class CyberneticCollectorFactionMod implements ModIntegrationHook {
    public static final String MOD_ID = "mechanist.example.cybernetic_collector_faction";
    public static final String FACTION_ID = "faction.cogwork-collectors";

    @Override public String modId() { return MOD_ID; }

    @Override public void onRegister(SimulationContext context) {
        FactionProfile faction = new FactionProfile(FACTION_ID, "Cogwork Collectors", new AlignmentVector(72, -48, 86, -66, 94));
        faction.setEconomicResource("synthetic-memory-filaments", 4800);
        faction.setEconomicResource("salvaged-cybernetic-components", 1250);
        faction.setEconomicResource("biological-sample-debt", 340);
        faction.addCulturalTrait("isolationist");
        faction.addCulturalTrait("cybernetic-accretion");
        faction.addCulturalTrait("collector-logic-diplomacy");
        faction.setAttribute("economyModel", "synthetic-collector-ledger");
        faction.setAttribute("prefersTradeWithOrganicFactions", false);
        context.registerFaction(faction);
        context.audit(MOD_ID, "registered cybernetic collector faction profile");
    }

    @Override public void onFactionDiplomacyChange(SimulationContext context, FactionProfile faction, DiplomacyChange change) {
        if (!FACTION_ID.equals(faction.id())) return;
        int oldAggression = faction.aggressionToward(change.targetFactionId());
        int delta = switch (change.signal()) {
            case ALLIANCE_OFFER -> -4;
            case TRADE_REQUEST -> faction.culturalTraits().contains("isolationist") ? 3 : -2;
            case BORDER_INCIDENT -> 8;
            case ESPIONAGE_DISCOVERED -> 18;
            case RESOURCE_CLAIM_CONFLICT -> 12;
            case CEASEFIRE_REQUEST -> -8;
            case UNKNOWN -> 2;
        };
        int nextAggression = Math.max(0, Math.min(100, oldAggression + delta + (change.severity() / 10)));
        faction.setAggressionToward(change.targetFactionId(), nextAggression);
        context.emit(new SimulationEvent.FactionDiplomacyMutated(faction.id(), change.targetFactionId(), change.signal(), oldAggression, nextAggression));
    }
}

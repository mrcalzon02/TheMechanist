package mechanist.modapi.examples;

import mechanist.modapi.LoreDatabase;
import mechanist.modapi.LoreEntry;
import mechanist.modapi.LoreQuery;
import mechanist.modapi.ModIntegrationHook;
import mechanist.modapi.SimulationContext;
import mechanist.modapi.SimulationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Infopedia Editor example: nested taxonomy and cross-linked wiki entries for a precursor civilization. */
public final class PrecursorInfopediaMod implements ModIntegrationHook {
    public static final String MOD_ID = "mechanist.example.precursor_infopedia";

    @Override public String modId() { return MOD_ID; }

    @Override public void onRegister(SimulationContext context) {
        register(context, new LoreEntry(
                "lore.precursor.aurelian-concordance",
                "The Aurelian Concordance",
                "precursor-civilizations/aurelian-concordance/overview",
                "A vanished polity remembered through gravity-locked archives, corrosion-proof reliquaries, and biological taxonomy engines that treated culture as an ecosystem.",
                List.of("precursor", "aurelian", "taxonomy", "civilization", "archive"),
                List.of("lore.precursor.castes", "lore.precursor.reliquaries")));
        register(context, new LoreEntry(
                "lore.precursor.castes",
                "Caste Taxonomy of the Concordance",
                "precursor-civilizations/aurelian-concordance/social-taxonomy/castes",
                "A nested index describing custodians, cartographers, wet-engineers, void singers, and hereditary adjudicators.",
                List.of("precursor", "caste", "taxonomy", "social-order"),
                List.of("lore.precursor.aurelian-concordance")));
        register(context, new LoreEntry(
                "lore.precursor.reliquaries",
                "Reliquary Memory Vaults",
                "precursor-civilizations/aurelian-concordance/material-culture/reliquaries",
                "Gravity-suspended memory vaults carrying civic law, migration maps, and extinct biosphere recovery patterns.",
                List.of("precursor", "reliquary", "memory-vault", "gravity-archive"),
                List.of("lore.precursor.aurelian-concordance", "lore.precursor.castes")));
        context.audit(MOD_ID, "registered precursor infopedia taxonomy entries");
    }

    private static void register(SimulationContext context, LoreEntry entry) {
        context.loreDatabase().addEntry(entry);
        context.emit(new SimulationEvent.LoreIndexed(entry.id(), entry.taxonomyPath(), String.join(",", entry.searchTags())));
    }

    @Override public List<LoreEntry> onLoreQuery(SimulationContext context, LoreDatabase loreDatabase, LoreQuery query) {
        String clean = query.queryText().toLowerCase(Locale.ROOT);
        ArrayList<LoreEntry> base = new ArrayList<>(loreDatabase.search(query.queryText()));
        if (clean.contains("precursor") || clean.contains("aurelian") || clean.contains("taxonomy")) {
            base.sort((a, b) -> a.taxonomyPath().compareToIgnoreCase(b.taxonomyPath()));
        }
        if (base.size() <= query.maxResults()) return List.copyOf(base);
        return List.copyOf(base.subList(0, query.maxResults()));
    }
}

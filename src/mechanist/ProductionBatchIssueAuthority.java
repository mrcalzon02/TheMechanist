package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

/** Classifies evidence-backed batch issue tags without applying hidden item effects. */
final class ProductionBatchIssueAuthority {
    private ProductionBatchIssueAuthority() { }

    static String tagsFor(ProductionRecipe recipe, ProductionBatchAuthority.BatchDisposition batch) {
        LinkedHashSet<String> tags = baseTags(batch);
        ItemDef def = recipe == null ? null : ItemCatalog.get(recipe.baseItem);
        String text = low((recipe == null ? "" : recipe.baseItem + " " + recipe.outputItemName()
                + " " + recipe.faction + " " + recipe.profile.label + " " + recipe.profile.weaknesses)
                + " " + (def == null ? "" : def.category + " " + def.source + " " + def.description + " " + def.use));
        addTextTags(tags, text);
        return join(tags);
    }

    static String tagsFor(FactionRecipeVariant variant, ProductionBatchAuthority.BatchDisposition batch) {
        LinkedHashSet<String> tags = baseTags(batch);
        String text = low((variant == null ? "" : variant.outputName + " " + variant.lawStatus + " "
                + variant.productionNote + " " + (variant.base == null ? "" : variant.base.family + " "
                + variant.base.outputBaseItem + " " + variant.base.note + " " + variant.base.source)));
        addTextTags(tags, text);
        if (variant != null && (variant.faction == Faction.ARBITES || variant.faction == Faction.IMPERIAL_GUARD
                || variant.faction == Faction.MECHANICUS || variant.faction == Faction.NOBLE)) {
            tags.add("faction-certified batch");
        }
        return join(tags);
    }

    static ArrayList<String> lines(String tags) {
        ArrayList<String> lines = new ArrayList<>();
        if (tags == null || tags.isBlank()) return lines;
        lines.add("Batch issue tags: " + tags + ".");
        lines.add("Batch issue boundary: tags preserve inspection and source risk; only the existing defect appraisal changes ordinary resale value.");
        return lines;
    }

    static ArrayList<String> definitionAuditLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Batch issue definition audit: owner=ProductionBatchIssueAuthority, batchOwner=ProductionBatchAuthority, provenanceOwner=ItemProvenanceRecord.");
        lines.add("Batch inspection tag audit: supported disposition tags are good batch and defective batch.");
        lines.add("Batch source-risk tag audit: supported evidence tags are contaminated batch, unstable batch, counterfeit batch, stolen-risk batch, restricted batch, and faction-certified batch.");
        lines.add("Batch recall audit: recallFlag=reserved future owner; current batch issue tags do not enforce recall, seizure, law, reputation, contamination damage, or counterfeit penalties.");
        lines.add("Batch consequence boundary: only existing defect appraisal changes ordinary resale value; item statistics and ordinary use effects remain unchanged by issue tags.");
        lines.add("Batch evidence boundary: tags require inspection, recipe, law, faction, or source metadata evidence and do not invent hidden effects.");
        lines.add("Guard: Milestone03BatchIssueDefinitionAuditSmoke checks issue owners, supported tags, recall reservation, effect boundaries, evidence boundaries, and raw-ID hiding.");
        return lines;
    }

    private static LinkedHashSet<String> baseTags(ProductionBatchAuthority.BatchDisposition batch) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (batch == null || batch.defectState() == null || batch.defectState().isBlank()) return tags;
        if ("defect flagged".equalsIgnoreCase(batch.defectState())) tags.add("defective batch");
        else if ("passed inspection".equalsIgnoreCase(batch.defectState())) tags.add("good batch");
        return tags;
    }

    private static void addTextTags(LinkedHashSet<String> tags, String text) {
        if (containsAny(text, "contaminat", "tainted", "toxin", "dirty water", "filth", "corpse", "warp", "profane")) {
            tags.add("contaminated batch");
        }
        if (containsAny(text, "unstable", "improvised", "salvage", "scavver", "mutant")) tags.add("unstable batch");
        if (containsAny(text, "counterfeit", "fake", "forged seal", "fraud")) tags.add("counterfeit batch");
        if (containsAny(text, "stolen", "theft-visible", "seizure-prone")) tags.add("stolen-risk batch");
        if (containsAny(text, "restricted", "contraband", "illegal", "black-market", "hostile-identity", "doctrine-controlled")) {
            tags.add("restricted batch");
        }
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (needle != null && !needle.isBlank() && text.contains(needle)) return true;
        return false;
    }

    private static String join(LinkedHashSet<String> tags) {
        return tags == null || tags.isEmpty() ? "" : String.join(", ", tags);
    }

    private static String low(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}

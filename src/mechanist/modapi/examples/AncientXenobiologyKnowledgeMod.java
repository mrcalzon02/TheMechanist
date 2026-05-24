package mechanist.modapi.examples;

import mechanist.modapi.ModIntegrationHook;
import mechanist.modapi.ResearchNode;
import mechanist.modapi.ResearchTree;
import mechanist.modapi.SimulationContext;
import mechanist.modapi.SimulationEvent;

import java.util.List;

/** Knowledge Editor example: non-linear ancient xenobiology branch with blueprint unlocks. */
public final class AncientXenobiologyKnowledgeMod implements ModIntegrationHook {
    public static final String MOD_ID = "mechanist.example.ancient_xenobiology_knowledge";
    public static final String ROOT_NODE = "knowledge.ancient-xenobiology.root";
    public static final String SPORE_NODE = "knowledge.ancient-xenobiology.spores";
    public static final String CHITIN_NODE = "knowledge.ancient-xenobiology.chitin";
    public static final String SYNTHESIS_NODE = "knowledge.ancient-xenobiology.synthesis";

    @Override public String modId() { return MOD_ID; }

    @Override public void onRegister(SimulationContext context) {
        ResearchTree tree = context.researchTree();
        tree.addNode(new ResearchNode(ROOT_NODE, "Ancient Xenobiology Survey", List.of(), List.of("blueprint.xeno-sample-vault"), false));
        tree.addNode(new ResearchNode(SPORE_NODE, "Dormant Spore Lineage Mapping", List.of(ROOT_NODE), List.of("blueprint.sporic-atmosphere-filter"), false));
        tree.addNode(new ResearchNode(CHITIN_NODE, "Precursor Chitin Lattice Study", List.of(ROOT_NODE), List.of("blueprint.chitin-lattice-armor"), false));
        tree.addNode(new ResearchNode(SYNTHESIS_NODE, "Xenobiological Containment Synthesis", List.of(SPORE_NODE, CHITIN_NODE), List.of("blueprint.ancient-growth-containment-cell", "blueprint.xeno-medicae-serum"), false));
        context.audit(MOD_ID, "registered ancient xenobiology research graph");
    }

    @Override public void onResearchNodeUnlocked(SimulationContext context, ResearchTree researchTree, ResearchNode node) {
        if (!node.id().startsWith("knowledge.ancient-xenobiology.")) return;
        String blueprint = node.unlockedBlueprints().isEmpty() ? "none" : String.join(",", node.unlockedBlueprints());
        context.emit(new SimulationEvent.ResearchUnlocked(node.id(), blueprint, "ancient xenobiology branch unlocked blueprint payloads"));
    }
}

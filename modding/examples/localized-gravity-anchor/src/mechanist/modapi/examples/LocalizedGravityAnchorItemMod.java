package mechanist.modapi.examples;

import mechanist.modapi.ItemTemplate;
import mechanist.modapi.ModIntegrationHook;
import mechanist.modapi.SimulationContext;
import mechanist.modapi.SimulationEvent;

/** Item Editor example: localized gravity anchor tool with plasma fuel charges and durability decay. */
public final class LocalizedGravityAnchorItemMod implements ModIntegrationHook {
    public static final String MOD_ID = "mechanist.example.localized_gravity_anchor";
    public static final String ITEM_ID = "item.localized-gravity-anchor";

    @Override public String modId() { return MOD_ID; }

    @Override public void onRegister(SimulationContext context) {
        ItemTemplate item = new ItemTemplate(ITEM_ID, "Localized Gravity Anchor", 7, 18.5d, 6, 100);
        item.addComponent("plasma-fuel-cell", 2);
        item.addComponent("gravitic-lens", 1);
        item.addComponent("reinforced-tool-frame", 1);
        item.setAttribute("massAdjustmentKg", 1250.0d);
        item.setAttribute("anchorDurationTicks", 30L);
        item.setAttribute("decayPerUse", 9);
        context.registerItem(item);
        context.audit(MOD_ID, "registered localized gravity anchor item template");
    }

    @Override public void onItemConsumed(SimulationContext context, ItemTemplate item) {
        if (!ITEM_ID.equals(item.id())) return;
        int oldCharges = item.fuelCharges();
        int oldDurability = item.durability();
        if (!item.consumeCharge()) {
            context.audit(MOD_ID, "gravity anchor activation failed: no plasma fuel charges remain");
            return;
        }
        int decay = 9;
        Object configured = item.attributes().get("decayPerUse");
        if (configured instanceof Number number) decay = Math.max(1, Math.min(50, number.intValue()));
        item.damage(decay);
        item.setAttribute("lastAnchorTick", context.tick());
        context.emit(new SimulationEvent.ItemStateChanged(item.id(), oldCharges, item.fuelCharges(), oldDurability, item.durability(), "plasma charge consumed to create localized gravity anchor field"));
    }
}

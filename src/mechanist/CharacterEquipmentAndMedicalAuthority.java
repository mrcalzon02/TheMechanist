package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Character loadout and body-modification presentation/state contract.
 *
 * Existing hand and clothing fields remain authoritative for their legacy slots.
 * New wearable slots and future medical layers are kept in explicit maps so the
 * later mutation/cybernetic systems can attach mechanics without redesigning
 * the character screen again.
 */
final class CharacterEquipmentAndMedicalAuthority {
    static final String VERSION = "character-equipment-medical-0.1";

    enum CharacterTab {
        OVERVIEW("Overview"),
        EQUIPMENT("Equipment"),
        MEDICAL("Medical");

        private final String label;

        CharacterTab(String label) { this.label = label; }
        String label() { return label; }

        static CharacterTab at(int index) {
            CharacterTab[] values = values();
            return values[Math.max(0, Math.min(index, values.length - 1))];
        }
    }

    enum EquipmentSlot {
        HEADGEAR("Headgear", "Head"),
        UNDERCLOTHES("Underclothes", "Torso"),
        CLOTHES("Clothes / Body", "Torso"),
        GLOVES("Gloves", "Hands"),
        BOOTS("Boots", "Feet"),
        BACKPACK("Backpack", "Back"),
        LEFT_RING("Left Ring", "Left Hand"),
        RIGHT_RING("Right Ring", "Right Hand"),
        ACCESSORY_ONE("Accessory 1", "General"),
        ACCESSORY_TWO("Accessory 2", "General"),
        LEFT_HAND("Left Hand", "Left Hand"),
        RIGHT_HAND("Right Hand", "Right Hand");

        private final String label;
        private final String bodyRegion;

        EquipmentSlot(String label, String bodyRegion) {
            this.label = label;
            this.bodyRegion = bodyRegion;
        }

        String label() { return label; }
        String bodyRegion() { return bodyRegion; }

        static EquipmentSlot at(int index) {
            EquipmentSlot[] values = values();
            return values[Math.max(0, Math.min(index, values.length - 1))];
        }
    }

    enum MedicalLayer {
        MUTATION("Mutation"),
        MODIFICATION("Modification"),
        CYBERNETIC("Cybernetic");

        private final String label;
        MedicalLayer(String label) { this.label = label; }
        String label() { return label; }

        static MedicalLayer at(int index) {
            MedicalLayer[] values = values();
            return values[Math.max(0, Math.min(index, values.length - 1))];
        }
    }

    record EquipmentView(EquipmentSlot slot, String itemName, boolean empty) {
        String label(boolean selected) {
            return (selected ? "> " : "  ") + slot.label() + ": " + itemName;
        }
    }

    record MedicalSlotKey(String bodyPartName, MedicalLayer layer) {
        MedicalSlotKey {
            bodyPartName = bodyPartName == null || bodyPartName.isBlank() ? "Unknown Region" : bodyPartName;
            layer = layer == null ? MedicalLayer.MODIFICATION : layer;
        }

        String storageKey() { return bodyPartName + "|" + layer.name(); }
        String label() { return bodyPartName + " — " + layer.label(); }
    }

    record MedicalView(MedicalSlotKey key, String installedName, boolean empty) {
        String label(boolean selected) {
            return (selected ? "> " : "  ") + key.layer().label() + ": " + installedName;
        }
    }

    private CharacterEquipmentAndMedicalAuthority() {}

    static List<EquipmentView> equipmentViews(Map<EquipmentSlot, String> extraSlots,
                                               String leftHand,
                                               String rightHand,
                                               Clothing clothing) {
        EnumMap<EquipmentSlot, String> values = new EnumMap<>(EquipmentSlot.class);
        if (extraSlots != null) values.putAll(extraSlots);
        values.put(EquipmentSlot.LEFT_HAND, normalizeLegacyHand(leftHand));
        values.put(EquipmentSlot.RIGHT_HAND, normalizeLegacyHand(rightHand));
        values.put(EquipmentSlot.CLOTHES, clothing == null ? "Empty" : safe(clothing.name));

        ArrayList<EquipmentView> views = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String item = safe(values.get(slot));
            boolean empty = item.equals("Empty");
            views.add(new EquipmentView(slot, item, empty));
        }
        return List.copyOf(views);
    }

    static EquipmentView selectedEquipment(int selectedIndex,
                                           Map<EquipmentSlot, String> extraSlots,
                                           String leftHand,
                                           String rightHand,
                                           Clothing clothing) {
        EquipmentSlot slot = EquipmentSlot.at(selectedIndex);
        return equipmentViews(extraSlots, leftHand, rightHand, clothing).stream()
                .filter(view -> view.slot() == slot)
                .findFirst()
                .orElse(new EquipmentView(slot, "Empty", true));
    }

    static boolean canEquip(String itemName, EquipmentSlot slot) {
        if (itemName == null || itemName.isBlank() || slot == null) return false;
        String item = itemName.toLowerCase(Locale.ROOT);
        return switch (slot) {
            case LEFT_HAND, RIGHT_HAND -> true;
            case HEADGEAR -> contains(item, "hat", "helmet", "hood", "cap", "mask", "headgear", "goggles");
            case UNDERCLOTHES -> contains(item, "underclothes", "undersuit", "shirt", "tunic", "vest", "underwear");
            case CLOTHES -> contains(item, "coat", "clothes", "clothing", "armor", "armour", "robes", "uniform", "workwear", "rags", "colors");
            case GLOVES -> contains(item, "glove", "gauntlet", "handwrap", "mitt");
            case BOOTS -> contains(item, "boot", "shoe", "greave", "sandal");
            case BACKPACK -> contains(item, "backpack", "rucksack", "pack", "satchel", "haversack");
            case LEFT_RING, RIGHT_RING -> contains(item, "ring", "band", "signet");
            case ACCESSORY_ONE, ACCESSORY_TWO -> contains(item, "accessory", "amulet", "necklace", "badge", "charm", "trinket", "medallion", "brooch", "belt", "scarf");
        };
    }

    static List<String> compatibilityHints(EquipmentSlot slot) {
        if (slot == null) return List.of();
        return switch (slot) {
            case HEADGEAR -> List.of("helmets", "hats", "hoods", "masks", "goggles");
            case UNDERCLOTHES -> List.of("undersuits", "shirts", "tunics", "vests");
            case CLOTHES -> List.of("clothing", "uniforms", "coats", "robes", "armor");
            case GLOVES -> List.of("gloves", "gauntlets", "hand wraps");
            case BOOTS -> List.of("boots", "shoes", "greaves");
            case BACKPACK -> List.of("backpacks", "rucksacks", "satchels");
            case LEFT_RING, RIGHT_RING -> List.of("rings", "signets", "bands");
            case ACCESSORY_ONE, ACCESSORY_TWO -> List.of("amulets", "badges", "charms", "belts", "trinkets");
            case LEFT_HAND, RIGHT_HAND -> List.of("weapons", "tools", "carried hand items");
        };
    }

    static List<String> bodyPartNames(Candidate candidate) {
        if (candidate == null || candidate.body == null || candidate.body.isEmpty()) return List.of();
        return candidate.body.values().stream()
                .filter(part -> part != null && part.name != null && !part.name.isBlank())
                .map(part -> part.name)
                .toList();
    }

    static List<MedicalView> medicalViews(Candidate candidate,
                                          String selectedBodyPart,
                                          Map<String, String> installed) {
        String bodyPart = selectedBodyPart;
        List<String> parts = bodyPartNames(candidate);
        if ((bodyPart == null || bodyPart.isBlank()) && !parts.isEmpty()) bodyPart = parts.get(0);
        if (bodyPart == null || bodyPart.isBlank()) bodyPart = "Unknown Region";

        ArrayList<MedicalView> result = new ArrayList<>();
        for (MedicalLayer layer : MedicalLayer.values()) {
            MedicalSlotKey key = new MedicalSlotKey(bodyPart, layer);
            String value = installed == null ? null : installed.get(key.storageKey());
            String safeValue = safe(value);
            result.add(new MedicalView(key, safeValue, safeValue.equals("Empty")));
        }
        return List.copyOf(result);
    }

    static List<String> medicalReadinessLines(Candidate candidate,
                                              String selectedBodyPart,
                                              Map<String, String> installed) {
        ArrayList<String> lines = new ArrayList<>();
        String part = selectedBodyPart == null || selectedBodyPart.isBlank() ? "No region selected" : selectedBodyPart;
        lines.add("Selected body region: " + part + ".");
        lines.add("Mutation, modification, and cybernetic layers are reserved independently.");
        lines.add("No surgery, compatibility, rejection, power, or maintenance mechanics are active yet.");
        lines.add("Future systems can bind directly through MedicalSlotKey.storageKey().");
        int installedCount = 0;
        if (installed != null) for (String value : installed.values()) if (!safe(value).equals("Empty")) installedCount++;
        lines.add("Installed placeholder records: " + installedCount + ".");
        if (candidate == null || candidate.body == null || candidate.body.isEmpty()) lines.add("Character body map unavailable.");
        return List.copyOf(lines);
    }

    static String bodyPartAt(Candidate candidate, Rectangle bounds, int x, int y) {
        for (CharacterPaperDollAuthority.RegionView region : CharacterPaperDollAuthority.regions(candidate, bounds)) {
            if (region.bounds().contains(x, y)) return region.bodyPartName();
        }
        return null;
    }

    static void clearLegacyBackedSlot(Map<EquipmentSlot, String> extraSlots, EquipmentSlot slot) {
        if (extraSlots == null || slot == null) return;
        if (slot != EquipmentSlot.LEFT_HAND && slot != EquipmentSlot.RIGHT_HAND && slot != EquipmentSlot.CLOTHES) {
            extraSlots.remove(slot);
        }
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static String normalizeLegacyHand(String item) {
        if (item == null || item.isBlank()) return "Empty";
        String upper = item.toUpperCase(Locale.ROOT);
        return upper.contains("EMPTY") ? "Empty" : item;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Empty" : value;
    }
}

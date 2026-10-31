package mechanist;

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Milestone02CharacterEquipmentMedicalSmoke {
    public static void main(String[] args) {
        Candidate candidate = Candidate.random(new Random(9221L));
        EnumMap<CharacterEquipmentAndMedicalAuthority.EquipmentSlot, String> extra =
                new EnumMap<>(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.class);
        extra.put(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR, "Scrap helmet");
        extra.put(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BACKPACK, "Survey backpack");

        List<CharacterEquipmentAndMedicalAuthority.EquipmentView> equipment =
                CharacterEquipmentAndMedicalAuthority.equipmentViews(extra, "Stub pistol", "RIGHT EMPTY",
                        Clothing.scavengerRags());
        if (equipment.size() != CharacterEquipmentAndMedicalAuthority.EquipmentSlot.values().length) {
            throw new AssertionError("equipment slot count mismatch");
        }
        if (!equipment.stream().anyMatch(view -> view.slot() == CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR
                && view.itemName().equals("Scrap helmet"))) {
            throw new AssertionError("headgear slot did not retain its equipped item");
        }
        if (!equipment.stream().anyMatch(view -> view.slot() == CharacterEquipmentAndMedicalAuthority.EquipmentSlot.RIGHT_HAND
                && view.empty())) {
            throw new AssertionError("legacy empty hand was not normalized");
        }
        if (!CharacterEquipmentAndMedicalAuthority.canEquip("Leather gloves",
                CharacterEquipmentAndMedicalAuthority.EquipmentSlot.GLOVES)) {
            throw new AssertionError("glove compatibility was not recognized");
        }
        if (CharacterEquipmentAndMedicalAuthority.canEquip("Leather gloves",
                CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BOOTS)) {
            throw new AssertionError("gloves incorrectly matched boots");
        }

        List<String> bodyParts = CharacterEquipmentAndMedicalAuthority.bodyPartNames(candidate);
        if (bodyParts.isEmpty()) throw new AssertionError("medical tab body-part mapping was empty");
        String selectedPart = bodyParts.get(0);
        LinkedHashMap<String, String> installed = new LinkedHashMap<>();
        CharacterEquipmentAndMedicalAuthority.MedicalSlotKey cybernetic =
                new CharacterEquipmentAndMedicalAuthority.MedicalSlotKey(selectedPart,
                        CharacterEquipmentAndMedicalAuthority.MedicalLayer.CYBERNETIC);
        installed.put(cybernetic.storageKey(), "Test interface socket");
        List<CharacterEquipmentAndMedicalAuthority.MedicalView> medical =
                CharacterEquipmentAndMedicalAuthority.medicalViews(candidate, selectedPart, installed);
        if (medical.size() != CharacterEquipmentAndMedicalAuthority.MedicalLayer.values().length) {
            throw new AssertionError("medical layer count mismatch");
        }
        if (!medical.stream().anyMatch(view -> view.key().layer()
                == CharacterEquipmentAndMedicalAuthority.MedicalLayer.CYBERNETIC
                && view.installedName().equals("Test interface socket"))) {
            throw new AssertionError("cybernetic placeholder binding was not retained");
        }
        // Click the center of the actual rendered hit target. The old fixed
        // (140, 30) point falls in the panel heading, above the head region.
        Rectangle panelBounds = new Rectangle(0, 0, 280, 440);
        Rectangle interactive = CharacterPaperDollAuthority.interactiveBounds(panelBounds);
        CharacterPaperDollAuthority.RegionView head = CharacterPaperDollAuthority.regions(candidate, interactive)
                .stream().filter(region -> "Head".equals(region.bodyPartName()))
                .findFirst().orElseThrow(() -> new AssertionError("live character has no rendered head region"));
        int clickX = head.bounds().x + head.bounds().width / 2;
        int clickY = head.bounds().y + head.bounds().height / 2;
        String clicked = CharacterEquipmentAndMedicalAuthority.bodyPartAt(
                candidate, panelBounds, clickX, clickY);
        if (!head.bodyPartName().equals(clicked)) {
            throw new AssertionError("rendered head click did not select its live body region: " + clicked);
        }
        if (CharacterEquipmentAndMedicalAuthority.bodyPartAt(candidate, panelBounds, 140, 30) != null
                || CharacterEquipmentAndMedicalAuthority.bodyPartAt(candidate, panelBounds, -1, -1) != null) {
            throw new AssertionError("paper-doll header/outside click selected a body region");
        }
        if (CharacterEquipmentAndMedicalAuthority.medicalViews(candidate, clicked, installed).stream()
                .anyMatch(view -> !clicked.equals(view.key().bodyPartName()))) {
            throw new AssertionError("medical tab did not preserve the clicked body-region identity");
        }
        if (!CharacterPaperDollAuthority.selectedBodyPartMatchesAny(head,
                CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSlot(
                        CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR))) {
            throw new AssertionError("equipment headgear focus does not match the clicked head region");
        }

        System.out.println("Milestone02CharacterEquipmentMedicalSmoke PASS "
                + CharacterEquipmentAndMedicalAuthority.VERSION);
    }

    private Milestone02CharacterEquipmentMedicalSmoke() {}
}

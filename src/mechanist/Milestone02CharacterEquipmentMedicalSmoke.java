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
        String clicked = CharacterEquipmentAndMedicalAuthority.bodyPartAt(candidate,
                new Rectangle(0, 0, 280, 440), 140, 30);
        if (clicked == null || clicked.isBlank()) {
            throw new AssertionError("paper-doll body region selection contract returned no region");
        }

        System.out.println("Milestone02CharacterEquipmentMedicalSmoke PASS "
                + CharacterEquipmentAndMedicalAuthority.VERSION);
    }

    private Milestone02CharacterEquipmentMedicalSmoke() {}
}

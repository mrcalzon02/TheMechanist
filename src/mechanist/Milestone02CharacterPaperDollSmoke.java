package mechanist;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

public final class Milestone02CharacterPaperDollSmoke {
    public static void main(String[] args) {
        Candidate candidate = Candidate.random(new Random(419L));
        BodyPart injured = candidate.body.get("L Lower Arm");
        if (injured == null) throw new AssertionError("candidate body did not expose expected left lower arm");
        injured.health = injured.maxHealth() * 0.40;

        List<CharacterPaperDollAuthority.RegionView> regions = CharacterPaperDollAuthority.regions(
                candidate, new Rectangle(0, 0, 260, 420));
        if (regions.isEmpty()) throw new AssertionError("paper doll produced no body regions");
        CharacterPaperDollAuthority.RegionView arm = regions.stream()
                .filter(region -> region.bodyPartName().equals("L Lower Arm"))
                .findFirst().orElseThrow(() -> new AssertionError("left lower arm missing from paper doll"));
        if (!arm.status().toLowerCase().contains("injured")) {
            throw new AssertionError("injured limb did not expose injury status: " + arm.status());
        }
        if (arm.currentHealth() <= 0 || arm.maximumHealth() <= arm.currentHealth()) {
            throw new AssertionError("limb hit-point readout was not preserved: " + arm.readout());
        }
        if (!"L Lower Arm".equals(CharacterPaperDollAuthority.regionIdentityLabel(arm))) {
            throw new AssertionError("paper-doll region identity label was not preserved: "
                    + CharacterPaperDollAuthority.regionIdentityLabel(arm));
        }
        if (!"LLA".equals(CharacterPaperDollAuthority.compactRegionIdentityLabel(arm))) {
            throw new AssertionError("narrow paper-doll region lost compact semantic identity: "
                    + CharacterPaperDollAuthority.compactRegionIdentityLabel(arm));
        }
        if (!"LLA".equals(CharacterPaperDollAuthority.narrowOverflowFallback("LLA"))) {
            throw new AssertionError("narrow semantic label degraded during overflow fallback");
        }
        if (!"7".equals(CharacterPaperDollAuthority.narrowOverflowFallback("7/10"))) {
            throw new AssertionError("narrow health fallback did not preserve current hit points");
        }
        if (!CharacterPaperDollAuthority.selectedBodyPartMatches(arm, "left-lower_arm")) {
            throw new AssertionError("selected medical body part did not map back to the paper-doll region");
        }
        if (CharacterPaperDollAuthority.selectedBodyPartMatches(arm, "R Lower Arm")) {
            throw new AssertionError("unselected body part was incorrectly treated as selected");
        }
        if (CharacterPaperDollAuthority.selectedBodyPartMatches(arm, "")) {
            throw new AssertionError("blank medical selection must not highlight a body region");
        }

        Rectangle renderedBounds = new Rectangle(8, 8, 284, 444);
        Rectangle interactive = CharacterPaperDollAuthority.interactiveBounds(renderedBounds);
        CharacterPaperDollAuthority.RegionView renderedArm = CharacterPaperDollAuthority.regions(candidate, interactive).stream()
                .filter(region -> region.bodyPartName().equals("L Lower Arm"))
                .findFirst().orElseThrow(() -> new AssertionError("rendered left lower arm missing from paper doll"));
        int centerX = renderedArm.bounds().x + renderedArm.bounds().width / 2;
        int centerY = renderedArm.bounds().y + renderedArm.bounds().height / 2;
        String hit = CharacterEquipmentAndMedicalAuthority.bodyPartAt(candidate, renderedBounds, centerX, centerY);
        if (!"L Lower Arm".equals(hit)) {
            throw new AssertionError("paper-doll click geometry diverged from rendered region: " + hit);
        }

        BufferedImage unselected = render(candidate, null);
        BufferedImage selected = render(candidate, "L Lower Arm");
        int changedPixels = changedPixels(unselected, selected);
        if (changedPixels < 20) {
            throw new AssertionError("selected body region produced no meaningful paper-doll visual delineation: " + changedPixels);
        }

        List<String> clothesRegions = CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSlot(
                CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES);
        for (String expectedRegion : clothesRegions) {
            CharacterPaperDollAuthority.RegionView region = CharacterPaperDollAuthority.regions(candidate, interactive).stream()
                    .filter(view -> view.bodyPartName().equals(expectedRegion))
                    .findFirst().orElseThrow(() -> new AssertionError("equipment-associated body region missing: " + expectedRegion));
            if (!CharacterPaperDollAuthority.selectedBodyPartMatchesAny(region, clothesRegions)) {
                throw new AssertionError("multi-region selection did not include equipment-associated body region: " + expectedRegion);
            }
        }
        BufferedImage clothesSelected = renderRegions(candidate, clothesRegions);
        int clothesChangedPixels = changedPixels(unselected, clothesSelected);
        if (clothesChangedPixels <= changedPixels) {
            throw new AssertionError("multi-region equipment selection did not delineate more than one body region: "
                    + clothesChangedPixels + " versus single-region " + changedPixels);
        }

        List<CharacterPaperDollAuthority.EquipmentView> equipment = CharacterPaperDollAuthority.equipment(
                "Stub pistol", "Knife", Clothing.scavengerRags());
        if (equipment.size() != 3) throw new AssertionError("expected left hand, right hand, and body slots");
        if (!equipment.get(0).itemName().contains("Stub pistol")) throw new AssertionError("left-hand equipment missing");
        if (!equipment.get(1).itemName().contains("Knife")) throw new AssertionError("right-hand equipment missing");
        if (equipment.get(2).empty()) throw new AssertionError("body protection slot incorrectly marked empty");
        if (CharacterPaperDollAuthority.selectedEquipment(99, "L", "R", null).slot()
                != CharacterPaperDollAuthority.EquipmentSlot.BODY) {
            throw new AssertionError("equipment selection did not clamp safely");
        }

        assertEquipmentRegion("Head", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR);
        assertEquipmentRegion("left hand", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.LEFT_HAND);
        assertEquipmentRegion("R Hand", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.RIGHT_HAND);
        assertEquipmentRegion("Chest", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES);
        assertEquipmentRegion("L Foot", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BOOTS);
        if (CharacterEquipmentAndMedicalAuthority.equipmentSlotForBodyPart("L Lower Arm") != null) {
            throw new AssertionError("ambiguous arm region must not silently select an unrelated equipment slot");
        }

        assertPreferredEquipmentRegion("L Hand", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.GLOVES);
        assertPreferredEquipmentRegion("R Hand", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.GLOVES);
        assertPreferredEquipmentRegion("L Hand", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.LEFT_RING);
        assertPreferredEquipmentRegion("Chest", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES);
        assertPreferredEquipmentRegion("R Foot", CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BOOTS);
        if (MouseLateUiController.equipmentSlotForBodyPart("L Hand", 999)
                != CharacterEquipmentAndMedicalAuthority.EquipmentSlot.LEFT_HAND) {
            throw new AssertionError("stale preferred equipment selection did not fall back to canonical body-region mapping");
        }

        assertEquipmentSlotRegions(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR, "Head");
        assertEquipmentSlotRegions(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES,
                "Chest", "Abdomen", "Pelvis");
        assertEquipmentSlotRegions(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.GLOVES,
                "L Hand", "R Hand");
        assertEquipmentSlotRegions(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BOOTS,
                "L Foot", "R Foot");
        assertEquipmentSlotRegions(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.LEFT_HAND, "L Hand");
        assertEquipmentSlotRegions(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.RIGHT_HAND, "R Hand");
        if (!CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSlot(
                CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BACKPACK).isEmpty()) {
            throw new AssertionError("backpack must not invent a visible body-region association");
        }
        if (!CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSelection(-1).isEmpty()) {
            throw new AssertionError("negative equipment selection must not highlight a paper-doll region");
        }
        if (!CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSelection(
                CharacterEquipmentAndMedicalAuthority.EquipmentSlot.values().length).isEmpty()) {
            throw new AssertionError("stale equipment selection must not highlight a paper-doll region");
        }
        if (!CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSelection(
                CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES.ordinal()).equals(clothesRegions)) {
            throw new AssertionError("valid equipment selection no longer resolves through the slot-region authority");
        }
        assertEquipmentRegionLabel(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES,
                "Chest / Abdomen / Pelvis");
        assertEquipmentRegionLabel(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.GLOVES,
                "L Hand / R Hand");
        assertEquipmentRegionLabel(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BOOTS,
                "L Foot / R Foot");
        assertEquipmentRegionLabel(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BACKPACK,
                "No direct paper-doll region");
        assertEquipmentRegionLabel(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.ACCESSORY_ONE,
                "No direct paper-doll region");

        Rectangle equipmentBounds = MouseLateUiController.characterEquipmentPaperDollBounds(1600, 900);
        Rectangle medicalBounds = MouseLateUiController.characterMedicalPaperDollBounds(1600, 900);
        if (equipmentBounds.width >= medicalBounds.width) {
            throw new AssertionError("equipment and medical paper-doll geometry no longer matches their render contracts");
        }
        if (equipmentBounds.x != medicalBounds.x || equipmentBounds.y != medicalBounds.y
                || equipmentBounds.height != medicalBounds.height) {
            throw new AssertionError("character tab paper-doll origins diverged across equipment and medical surfaces");
        }

        System.out.println("Milestone02CharacterPaperDollSmoke PASS " + CharacterPaperDollAuthority.VERSION
                + " equipment=" + CharacterEquipmentAndMedicalAuthority.VERSION
                + " selectedPixels=" + changedPixels + " clothesPixels=" + clothesChangedPixels + " hit=" + hit);
    }

    private static void assertEquipmentRegion(String bodyPart,
                                              CharacterEquipmentAndMedicalAuthority.EquipmentSlot expected) {
        CharacterEquipmentAndMedicalAuthority.EquipmentSlot actual =
                MouseLateUiController.equipmentSlotForBodyPart(bodyPart);
        if (actual != expected) {
            throw new AssertionError("equipment body-region association failed for " + bodyPart
                    + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertPreferredEquipmentRegion(String bodyPart,
                                                       CharacterEquipmentAndMedicalAuthority.EquipmentSlot preferred) {
        CharacterEquipmentAndMedicalAuthority.EquipmentSlot actual =
                MouseLateUiController.equipmentSlotForBodyPart(bodyPart, preferred.ordinal());
        if (actual != preferred) {
            throw new AssertionError("paper-doll click lost preferred equipment association for " + bodyPart
                    + ": expected " + preferred + " but got " + actual);
        }
    }

    private static void assertEquipmentSlotRegions(CharacterEquipmentAndMedicalAuthority.EquipmentSlot slot,
                                                   String... expected) {
        List<String> actual = CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSlot(slot);
        if (!actual.equals(List.of(expected))) {
            throw new AssertionError("equipment slot region association failed for " + slot
                    + ": expected " + List.of(expected) + " but got " + actual);
        }
    }

    private static void assertEquipmentRegionLabel(CharacterEquipmentAndMedicalAuthority.EquipmentSlot slot,
                                                   String expected) {
        if (!expected.equals(slot.bodyRegion())) {
            throw new AssertionError("equipment slot visible region label diverged for " + slot
                    + ": expected " + expected + " but got " + slot.bodyRegion());
        }
    }

    private static BufferedImage render(Candidate candidate, String selectedBodyPart) {
        BufferedImage image = new BufferedImage(300, 460, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            CharacterPaperDollAuthority.paint(g, new Rectangle(8, 8, 284, 444), candidate, null, selectedBodyPart);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static BufferedImage renderRegions(Candidate candidate, List<String> selectedBodyParts) {
        BufferedImage image = new BufferedImage(300, 460, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            CharacterPaperDollAuthority.paintSelectedRegions(
                    g, new Rectangle(8, 8, 284, 444), candidate, null, selectedBodyParts);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int changedPixels(BufferedImage first, BufferedImage second) {
        int changed = 0;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) changed++;
            }
        }
        return changed;
    }

    private Milestone02CharacterPaperDollSmoke() {}
}

package mechanist;

import java.lang.reflect.Method;
import java.util.Random;

public final class Milestone02CharacterEquipmentScreenIntegrationSmoke {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        GamePanel panel = new GamePanel();
        try {
  panel.active = Candidate.random(new Random(8181L));
  panel.inventory.clear();
  panel.inventory.add("Scrap helmet");
  panel.selectedInventoryIndex = 0;
  panel.selectedCharacterEquipmentSlot = CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR.ordinal();

  Method equip = GamePanel.class.getDeclaredMethod("equipSelectedInventoryToCharacterSlot");
  equip.setAccessible(true);
  equip.invoke(panel);
  if (!"Scrap helmet".equals(panel.equippedWearableSlots.get(
          CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR))) {
      throw new AssertionError("headgear did not move into the live loadout map");
  }
  if (!panel.inventory.isEmpty()) throw new AssertionError("equipped headgear remained in carried inventory");

  Method unequip = GamePanel.class.getDeclaredMethod("unequipSelectedCharacterEquipment");
  unequip.setAccessible(true);
  unequip.invoke(panel);
  if (panel.equippedWearableSlots.containsKey(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR)) {
      throw new AssertionError("headgear slot did not clear");
  }
  if (!panel.inventory.contains("Scrap helmet")) throw new AssertionError("unequipped headgear was not returned");

  panel.inventory.clear();
  panel.inventory.add("Survey backpack");
  panel.selectedInventoryIndex = 0;
  panel.selectedCharacterEquipmentSlot = CharacterEquipmentAndMedicalAuthority.EquipmentSlot.BACKPACK.ordinal();
  int before = panel.carryCapacity();
  equip.invoke(panel);
  int after = panel.carryCapacity();
  if (after <= before) throw new AssertionError("backpack did not increase carry capacity");

  String region = CharacterEquipmentAndMedicalAuthority.bodyPartNames(panel.active).get(0);
  panel.installCharacterMedicalRecord(region,
          CharacterEquipmentAndMedicalAuthority.MedicalLayer.CYBERNETIC,
          "Isolated test interface");
  if (!"Isolated test interface".equals(panel.characterMedicalRecord(region,
          CharacterEquipmentAndMedicalAuthority.MedicalLayer.CYBERNETIC))) {
      throw new AssertionError("medical slot hook did not retain the installed record");
  }

  assertPaperDollClickToRenderedFocus(panel);

  System.out.println("Milestone02CharacterEquipmentScreenIntegrationSmoke PASS "
          + CharacterEquipmentAndMedicalAuthority.VERSION);
        } finally {
  panel.shutdownRuntime();
        }
    }


    /**
     * Follow the real character-screen mouse dispatch from an on-screen body
     * hit target into the selected state and the shared live renderer. This
     * intentionally uses the same bounds and selection authorities as the
     * equipment/medical tabs rather than testing only a synthetic highlight.
     */
    private static void assertPaperDollClickToRenderedFocus(GamePanel panel) {
        panel.setSize(1280, 800);
        panel.screen = GamePanel.Screen.CHARACTER;
        panel.newGameSetupActive = false;

        panel.characterTab = CharacterEquipmentAndMedicalAuthority.CharacterTab.MEDICAL.ordinal();
        java.awt.Rectangle medicalBounds = MouseLateUiController.characterMedicalPaperDollBounds(
                panel.getWidth(), panel.getHeight());
        java.awt.Rectangle medicalHit = bodyRegion(panel.active, medicalBounds, "L Lower Arm");
        java.awt.Rectangle otherMedical = bodyRegion(panel.active, medicalBounds, "R Lower Arm");
        int mx = medicalHit.x + medicalHit.width / 2;
        int my = medicalHit.y + medicalHit.height / 2;
        if (!"L Lower Arm".equals(CharacterEquipmentAndMedicalAuthority.bodyPartAt(
                panel.active, medicalBounds, mx, my))) {
            throw new AssertionError("medical rendered region does not match the mouse hit target");
        }

        java.awt.image.BufferedImage medicalBefore = renderSelection(
                panel, medicalBounds, java.util.List.of());
        if (!MouseLateUiController.handleLateUiClick(panel, mx, my)) {
            throw new AssertionError("medical character-screen click was not consumed");
        }
        if (!"L Lower Arm".equals(panel.selectedCharacterMedicalBodyPart)) {
            throw new AssertionError("medical click did not update the actual selected body region: "
                    + panel.selectedCharacterMedicalBodyPart);
        }
        java.awt.image.BufferedImage medicalAfter = renderSelection(
                panel, medicalBounds, java.util.List.of(panel.selectedCharacterMedicalBodyPart));
        assertFocusPixels("medical", medicalBefore, medicalAfter, mx, my,
                otherMedical.x + otherMedical.width / 2,
                otherMedical.y + otherMedical.height / 2);
        if (MouseLateUiController.handleCharacterMedicalPaperDollClick(
                panel, medicalBounds.x - 1, medicalBounds.y - 1)
                || !"L Lower Arm".equals(panel.selectedCharacterMedicalBodyPart)) {
            throw new AssertionError("outside medical click changed selection");
        }

        panel.characterTab = CharacterEquipmentAndMedicalAuthority.CharacterTab.EQUIPMENT.ordinal();
        panel.selectedCharacterEquipmentSlot =
                CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR.ordinal();
        java.awt.Rectangle equipmentBounds = MouseLateUiController.characterEquipmentPaperDollBounds(
                panel.getWidth(), panel.getHeight());
        java.awt.Rectangle equipmentHit = bodyRegion(panel.active, equipmentBounds, "L Hand");
        java.awt.Rectangle otherEquipment = bodyRegion(panel.active, equipmentBounds, "R Hand");
        int ex = equipmentHit.x + equipmentHit.width / 2;
        int ey = equipmentHit.y + equipmentHit.height / 2;
        if (!"L Hand".equals(CharacterEquipmentAndMedicalAuthority.bodyPartAt(
                panel.active, equipmentBounds, ex, ey))) {
            throw new AssertionError("equipment rendered region does not match the mouse hit target");
        }

        java.awt.image.BufferedImage equipmentBefore = renderSelection(
                panel, equipmentBounds, java.util.List.of());
        if (!MouseLateUiController.handleLateUiClick(panel, ex, ey)) {
            throw new AssertionError("equipment character-screen click was not consumed");
        }
        if (panel.selectedCharacterEquipmentSlot
                != CharacterEquipmentAndMedicalAuthority.EquipmentSlot.LEFT_HAND.ordinal()) {
            throw new AssertionError("equipment click selected the wrong live slot: "
                    + panel.selectedCharacterEquipmentSlot);
        }
        java.awt.image.BufferedImage equipmentAfter = renderSelection(panel, equipmentBounds,
                CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSelection(
                        panel.selectedCharacterEquipmentSlot));
        assertFocusPixels("equipment", equipmentBefore, equipmentAfter, ex, ey,
                otherEquipment.x + otherEquipment.width / 2,
                otherEquipment.y + otherEquipment.height / 2);
        if (MouseLateUiController.handleCharacterEquipmentPaperDollClick(
                panel, equipmentBounds.x - 1, equipmentBounds.y - 1)
                || panel.selectedCharacterEquipmentSlot
                != CharacterEquipmentAndMedicalAuthority.EquipmentSlot.LEFT_HAND.ordinal()) {
            throw new AssertionError("outside equipment click changed selection");
        }
    }

    private static java.awt.Rectangle bodyRegion(Candidate candidate,
                                                   java.awt.Rectangle bounds, String name) {
        return CharacterPaperDollAuthority.regions(
                candidate, CharacterPaperDollAuthority.interactiveBounds(bounds)).stream()
                .filter(region -> name.equals(region.bodyPartName()))
                .findFirst().orElseThrow(() -> new AssertionError("missing rendered body region " + name))
                .bounds();
    }

    private static java.awt.image.BufferedImage renderSelection(GamePanel panel,
                                                                  java.awt.Rectangle bounds,
                                                                  java.util.List<String> selected) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                panel.getWidth(), panel.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            CharacterPaperDollAuthority.paintSelectedRegions(g, bounds, panel.active, null, selected);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void assertFocusPixels(String tab, java.awt.image.BufferedImage before,
                                          java.awt.image.BufferedImage after,
                                          int selectedX, int selectedY, int otherX, int otherY) {
        if (before.getRGB(selectedX, selectedY) == after.getRGB(selectedX, selectedY)) {
            throw new AssertionError(tab + " selected hit target has no visible in-region focus");
        }
        if (before.getRGB(otherX, otherY) != after.getRGB(otherX, otherY)) {
            throw new AssertionError(tab + " selection changed a nonselected limb");
        }
    }

    private Milestone02CharacterEquipmentScreenIntegrationSmoke() {}
}

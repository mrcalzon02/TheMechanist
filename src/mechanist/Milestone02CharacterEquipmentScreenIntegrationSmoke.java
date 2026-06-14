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

  System.out.println("Milestone02CharacterEquipmentScreenIntegrationSmoke PASS "
          + CharacterEquipmentAndMedicalAuthority.VERSION);
        } finally {
  panel.shutdownRuntime();
        }
    }

    private Milestone02CharacterEquipmentScreenIntegrationSmoke() {}
}

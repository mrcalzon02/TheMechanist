package mechanist;

import java.awt.Rectangle;
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

        System.out.println("Milestone02CharacterPaperDollSmoke PASS " + CharacterPaperDollAuthority.VERSION);
    }

    private Milestone02CharacterPaperDollSmoke() {}
}

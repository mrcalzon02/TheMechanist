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
        if (!CharacterPaperDollAuthority.selectedBodyPartMatches(arm, "left-lower_arm")) {
            throw new AssertionError("selected medical body part did not map back to the paper-doll region");
        }
        if (CharacterPaperDollAuthority.selectedBodyPartMatches(arm, "R Lower Arm")) {
            throw new AssertionError("unselected body part was incorrectly treated as selected");
        }
        if (CharacterPaperDollAuthority.selectedBodyPartMatches(arm, "")) {
            throw new AssertionError("blank medical selection must not highlight a body region");
        }

        BufferedImage unselected = render(candidate, null);
        BufferedImage selected = render(candidate, "L Lower Arm");
        int changedPixels = changedPixels(unselected, selected);
        if (changedPixels < 20) {
            throw new AssertionError("selected body region produced no meaningful paper-doll visual delineation: " + changedPixels);
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

        System.out.println("Milestone02CharacterPaperDollSmoke PASS " + CharacterPaperDollAuthority.VERSION
                + " selectedPixels=" + changedPixels);
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

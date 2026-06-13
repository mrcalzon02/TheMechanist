package mechanist;

/** Smoke for species-aware pet actions and honest blocked reasons. */
final class Milestone02PetInteractionFeedbackSmoke {
    public static void main(String[] args) {
        NpcEntity dog = pet("Brass", "kennel dog", "Alert");
        PetInteractionFeedbackAuthority.PetInteractionReadout dogReadout = PetInteractionFeedbackAuthority.describe(dog);
        require(dogReadout.allowed(), "non-hostile dog should allow affection");
        require("Head Pat".equals(dogReadout.actionLabel()), "dog action should be Head Pat");
        requireContains(dogReadout.feedback(), "head pat", "dog feedback");

        NpcEntity cat = pet("Mote", "ship cat", "Idle");
        require("Scritch".equals(PetInteractionFeedbackAuthority.describe(cat).actionLabel()), "cat action should be Scritch");

        NpcEntity rat = pet("Nib", "pet rat", "Curious");
        require("Nose Boop".equals(PetInteractionFeedbackAuthority.describe(rat).actionLabel()), "rat action should be Nose Boop");

        dog.state = "Hostile";
        PetInteractionFeedbackAuthority.PetInteractionReadout hostile = PetInteractionFeedbackAuthority.describe(dog);
        require(!hostile.allowed(), "hostile pet should block affection");
        requireContains(hostile.feedback(), "hostile", "hostile denial");

        NpcEntity wild = pet("Sump rat", "wild rat", "Wary");
        wild.creatureKind = "wild-animal";
        requireContains(PetInteractionFeedbackAuthority.describe(wild).feedback(), "not a companion animal", "wild animal denial");
        requireContains(PetInteractionFeedbackAuthority.auditSummary(), "headPat+scritch+noseBoop", "pet interaction audit");
    }

    private static NpcEntity pet(String name, String profile, String state) {
        NpcEntity npc = new NpcEntity();
        npc.name = name;
        npc.role = profile;
        npc.animalProfileId = profile;
        npc.creatureKind = "pet";
        npc.state = state;
        npc.hp = 12;
        return npc;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.toLowerCase(java.util.Locale.ROOT).contains(expected.toLowerCase(java.util.Locale.ROOT))) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02PetInteractionFeedbackSmoke() { }
}

package mechanist;

import java.util.Locale;

/** Species-aware player feedback for non-hostile companion-animal interaction. */
final class PetInteractionFeedbackAuthority {
    static final String VERSION = "0.9.10kl";

    record PetInteractionReadout(boolean allowed, String actionLabel, String feedback, String resultingState) { }

    private PetInteractionFeedbackAuthority() { }

    static PetInteractionReadout describe(NpcEntity npc) {
        if (npc == null) return denied("Interact", "No pet is selected.");
        if (!npc.isAnimalActor()) return denied("Interact", "This entity is not an animal.");
        if (!npc.isPetActor()) return denied("Give space", safeName(npc) + " is not a companion animal and does not welcome handling.");

        String state = npc.state == null ? "" : npc.state.toLowerCase(Locale.ROOT);
        if (state.contains("hostile") || state.contains("attack") || state.contains("fighting")) {
            return denied(actionLabel(npc), safeName(npc) + " is hostile and will not accept contact.");
        }
        if (state.contains("injured") || state.contains("wounded") || npc.hp <= 3) {
            return denied(actionLabel(npc), safeName(npc) + " is hurt and needs careful treatment before affection.");
        }
        if (state.contains("sleep")) return denied(actionLabel(npc), safeName(npc) + " is sleeping. Let them rest.");
        if (state.contains("restrained") || state.contains("caged")) {
            return denied(actionLabel(npc), safeName(npc) + " cannot comfortably reach you while restrained.");
        }

        String action = actionLabel(npc);
        String feedback = switch (species(npc)) {
            case DOG -> "You give " + safeName(npc) + " a careful head pat.";
            case CAT -> "You give " + safeName(npc) + " a gentle scritch.";
            case RODENT -> "You give " + safeName(npc) + " a tiny nose boop.";
            case OTHER -> "You offer " + safeName(npc) + " calm, gentle affection.";
        };
        return new PetInteractionReadout(true, action, feedback, "Content");
    }

    static PetInteractionReadout apply(NpcEntity npc) {
        PetInteractionReadout readout = describe(npc);
        if (readout.allowed() && npc != null) npc.state = readout.resultingState();
        return readout;
    }

    static String auditSummary() {
        return "petInteractionFeedbackAuthority version=" + VERSION
                + " actions=headPat+scritch+noseBoop+gentleAffection"
                + " denials=hostile+injured+sleeping+restrained+nonPet";
    }

    private static PetInteractionReadout denied(String label, String reason) {
        return new PetInteractionReadout(false, label, PlayerFacingText.sanitize(reason), "Unchanged");
    }

    private static String actionLabel(NpcEntity npc) {
        return switch (species(npc)) {
            case DOG -> "Head Pat";
            case CAT -> "Scritch";
            case RODENT -> "Nose Boop";
            case OTHER -> "Pet";
        };
    }

    private static Species species(NpcEntity npc) {
        String text = ((npc == null ? "" : npc.animalProfileId) + " "
                + (npc == null ? "" : npc.name) + " " + (npc == null ? "" : npc.role)).toLowerCase(Locale.ROOT);
        if (text.contains("dog") || text.contains("hound") || text.contains("canid")) return Species.DOG;
        if (text.contains("cat") || text.contains("feline")) return Species.CAT;
        if (text.contains("mouse") || text.contains("rat") || text.contains("rodent")) return Species.RODENT;
        return Species.OTHER;
    }

    private static String safeName(NpcEntity npc) {
        return PlayerFacingText.sanitize(npc == null || npc.name == null || npc.name.isBlank() ? "The animal" : npc.name);
    }

    private enum Species { DOG, CAT, RODENT, OTHER }
}

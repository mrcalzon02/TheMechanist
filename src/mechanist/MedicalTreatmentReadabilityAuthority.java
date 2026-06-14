package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MedicalTreatmentReadabilityAuthority {
    private MedicalTreatmentReadabilityAuthority() {}

    static List<String> summary(List<String> carriedItems, int wounds, int bleeding, int infectionRisk, int pain) {
        ArrayList<String> usable = new ArrayList<>();
        if (carriedItems != null) for (String item : carriedItems) {
            if (isRuntimeTreatment(item) && usable.size() < 3) usable.add(item);
        }

        ArrayList<String> lines = new ArrayList<>();
        boolean needsTreatment = wounds > 0 || bleeding > 0 || infectionRisk > 0 || pain > 0;
        if (!needsTreatment) {
            lines.add("Treatment readiness: no current wound-care need detected.");
        } else if (usable.isEmpty()) {
            lines.add("Treatment readiness: no usable wound-care item is carried; seek a bandage, splint, antiseptic, medkit, or clinic.");
        } else {
            lines.add("Treatment readiness: carried " + String.join(", ", usable) + ".");
            lines.add("Use effect: one carried treatment reduces wound burden, bleeding, infection risk, and pain by the current field-treatment amounts.");
        }
        lines.add("Medicine boundary: named drugs and stimulants are cataloged, but their specialized effects are not active through the ordinary Use action yet.");
        return lines;
    }

    static boolean isRuntimeTreatment(String item) {
        String low = item == null ? "" : item.toLowerCase(Locale.ROOT);
        return low.contains("medkit") || low.contains("bandage") || low.contains("splint") || low.contains("antiseptic");
    }
}

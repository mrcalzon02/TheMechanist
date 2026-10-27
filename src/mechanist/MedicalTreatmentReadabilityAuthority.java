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
            lines.add("Use effect: each treatment has its own recovery profile for wounds, bleeding, infection, pain and injured body parts; inspect the item before using it.");
        }
        lines.add("Combat drugs and stims with a field-use profile provide timed effects and a fatigue crash. Doses from the same family do not stack.");
        return lines;
    }

    static boolean isRuntimeTreatment(String item) {
        var profile=ActorConsumableAuthority.profile(item);
        ItemDef definition=ItemCatalog.get(item);
        return profile!=null && definition!=null && definition.category.startsWith("medical") && (profile.heal()>0 || profile.wounds()>0 || profile.bleeding()>0 || profile.infection()>0 || profile.pain()>0);
    }
}

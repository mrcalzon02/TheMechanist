package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Audit-only permission readiness layer for construction blueprints. */
final class BlueprintPermissionReadinessAuthority {
    record PermissionReadiness(String blueprintName, String permissionClass, boolean blueprintOwned,
                               boolean permissionReady, List<String> blockers, String readinessLine) { }

    private BlueprintPermissionReadinessAuthority() { }

    static List<PermissionReadiness> catalogReadiness() {
        ArrayList<PermissionReadiness> rows = new ArrayList<>();
        for (BlueprintAcquisitionPathAuthority.AcquisitionPath path : BlueprintAcquisitionPathAuthority.paths()) {
            rows.add(evaluate(path, true, true, true, true, true));
        }
        return List.copyOf(rows);
    }

    static PermissionReadiness evaluate(BlueprintAcquisitionPathAuthority.AcquisitionPath path,
                                        boolean blueprintOwned,
                                        boolean reputationReady,
                                        boolean licenseReady,
                                        boolean permitReady,
                                        boolean legalAccessReady) {
        String name = path == null ? "Unknown Blueprint" : safe(path.blueprintName(), "Unknown Blueprint");
        String permissionClass = permissionClassFor(path);
        ArrayList<String> blockers = new ArrayList<>();
        if (!blueprintOwned) blockers.add("blueprint not owned");
        if (requiresReputation(permissionClass) && !reputationReady) blockers.add("reputation or faction standing not ready");
        if (requiresLicense(permissionClass) && !licenseReady) blockers.add("license not ready");
        if (requiresPermit(permissionClass) && !permitReady) blockers.add("permit not ready");
        if (requiresLegalAccess(permissionClass) && !legalAccessReady) blockers.add("legal access not ready");
        boolean ready = blockers.isEmpty();
        String line = name + " permission " + (ready ? "ready" : "blocked")
                + ": class=" + permissionClass
                + ", owned=" + yesNo(blueprintOwned)
                + ", blockers=" + (ready ? "none" : String.join("; ", blockers)) + ".";
        return new PermissionReadiness(name, permissionClass, blueprintOwned, ready, List.copyOf(blockers), line);
    }

    static List<String> definitionAuditLines() {
        List<PermissionReadiness> rows = catalogReadiness();
        int publicReady = 0;
        int permitOrLicense = 0;
        int factionStanding = 0;
        int restricted = 0;
        int illicit = 0;
        for (PermissionReadiness row : rows) {
            switch (row.permissionClass()) {
                case "public-ready" -> publicReady++;
                case "permit-or-license" -> permitOrLicense++;
                case "faction-standing" -> factionStanding++;
                case "restricted-legal-access" -> restricted++;
                case "illicit-or-stolen-risk" -> illicit++;
                default -> { }
            }
        }
        PermissionReadiness unownedShop = evaluate(pathNamed("Licensed Shop Counter"), false, true, false, false, true);
        PermissionReadiness standingForge = evaluate(pathNamed("EMM Micro Forge"), true, false, false, true, true);
        PermissionReadiness restrictedSensor = evaluate(pathNamed("Security Sensor Mast"), true, true, true, true, false);
        return List.of(
                "Blueprint permission readiness audit: owner=BlueprintPermissionReadinessAuthority, acquisitionOwner=BlueprintAcquisitionPathAuthority, catalogOwner=BuildRecipe, readiness=forecast-only, ordinaryUiRawIds=false.",
                "Blueprint permission catalog audit: blueprintReadinessRows=" + rows.size()
                        + ", publicReady=" + publicReady
                        + ", permitOrLicense=" + permitOrLicense
                        + ", factionStanding=" + factionStanding
                        + ", restrictedLegalAccess=" + restricted
                        + ", illicitOrStolenRisk=" + illicit + ".",
                "Blueprint permission class audit: supported classes are public-ready, permit-or-license, faction-standing, restricted-legal-access, and illicit-or-stolen-risk.",
                "Blueprint permission sample audit: " + unownedShop.readinessLine()
                        + " | " + standingForge.readinessLine()
                        + " | " + restrictedSensor.readinessLine(),
                "Blueprint permission rule: owning a blueprint is necessary but not sufficient; reputation, faction standing, license, permit, legal access, materials, workbench, knowledge, placement access, utilities, and construction labor remain separately checked by their owners.",
                "Blueprint permission boundary: this audit does not add live vendor offers, spend reputation, buy permits, grant licenses, resolve theft, mutate heat or suspicion, bypass placement validation, or execute faction construction.",
                "Guard: Milestone03BlueprintPermissionReadinessAuditSmoke checks permission classes, sample blockers, ownership separation, future-owner boundaries, and raw-ID hiding."
        );
    }

    private static BlueprintAcquisitionPathAuthority.AcquisitionPath pathNamed(String name) {
        for (BlueprintAcquisitionPathAuthority.AcquisitionPath path : BlueprintAcquisitionPathAuthority.paths()) {
            if (path != null && path.blueprintName().equalsIgnoreCase(name)) return path;
        }
        return null;
    }

    private static String permissionClassFor(BlueprintAcquisitionPathAuthority.AcquisitionPath path) {
        String text = "";
        if (path != null) {
            text = (safe(path.legalLabel(), "") + " " + safe(path.accessLabel(), "") + " "
                    + safe(path.acquisitionPath(), "") + " " + safe(path.explanation(), "")).toLowerCase(Locale.ROOT);
        }
        if (text.contains("black-market") || text.contains("stolen")) return "illicit-or-stolen-risk";
        if (text.contains("military restricted") || text.contains("restricted civic") || text.contains("controlled")) return "restricted-legal-access";
        if (text.contains("faction-approved") || text.contains("earn standing") || text.contains("noble-house")) return "faction-standing";
        if (text.contains("permit") || text.contains("license")) return "permit-or-license";
        return "public-ready";
    }

    private static boolean requiresReputation(String permissionClass) {
        return "faction-standing".equals(permissionClass);
    }

    private static boolean requiresLicense(String permissionClass) {
        return "permit-or-license".equals(permissionClass) || "faction-standing".equals(permissionClass);
    }

    private static boolean requiresPermit(String permissionClass) {
        return "permit-or-license".equals(permissionClass);
    }

    private static boolean requiresLegalAccess(String permissionClass) {
        return "restricted-legal-access".equals(permissionClass) || "illicit-or-stolen-risk".equals(permissionClass);
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

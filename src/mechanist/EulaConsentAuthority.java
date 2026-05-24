package mechanist;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Properties;

/** Persistent, fail-closed EULA consent gate for the desktop fallback runtime. */
final class EulaConsentAuthority {
    static final String VERSION = "eula-consent-authority-0.9.10ip";
    static final String CONSENT_KEY = "eula_consented";
    private static final Path PATH = Paths.get("settings", "legal.properties");

    static boolean hasConsent() {
        try {
            if (!Files.exists(PATH)) return false;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(PATH)) { p.load(in); }
            return Boolean.parseBoolean(p.getProperty(CONSENT_KEY, "false"));
        } catch (Throwable t) {
            DebugLog.warn("EULA_LOAD", "Consent settings could not be read; legal gate will remain closed.");
            return false;
        }
    }

    static void accept(UserProfileAuthority.Profile profile) {
        try {
            Files.createDirectories(PATH.getParent());
            Properties p = new Properties();
            if (Files.exists(PATH)) try (InputStream in = Files.newInputStream(PATH)) { p.load(in); }
            p.setProperty(CONSENT_KEY, "true");
            p.setProperty("eula.acceptedAt", Instant.now().toString());
            p.setProperty("eula.version", VERSION);
            p.setProperty("profile.provider", profile == null ? "fallback" : safe(profile.provider));
            p.setProperty("profile.id", profile == null ? "unknown" : safe(profile.shortId()));
            try (OutputStream out = Files.newOutputStream(PATH)) { p.store(out, "The Mechanist EULA consent state"); }
            DebugLog.audit("EULA_ACCEPTED", "profile=" + (profile == null ? "unknown" : profile.provider + "/" + profile.shortId()));
        } catch (Throwable t) {
            DebugLog.error("EULA_SAVE", "Could not persist EULA consent.", t);
            throw new IllegalStateException("EULA consent could not be persisted.", t);
        }
    }

    static String[] eulaText() {
        return new String[]{
            "THE MECHANIST END USER LICENSE AND CONSENT NOTICE",
            "You must accept this notice before accessing the application, menus, gameplay, server tools, profile creation, chat, or local content.",
            "This development build stores local settings, profile identifiers, diagnostics, chat logs, and save data on this machine for operation, debugging, and continuity.",
            "Network and server features may record profile, wrapper, installation, session, moderation, and administrative identifiers when those systems are active.",
            "Do not use the application if you are not legally permitted to do so in your region or if you do not agree to local/profile logging required for the runtime.",
            "INTELLECTUAL PROPERTY DISCLAIMER & FAN PROJECT ACKNOWLEDGMENT",
            "IMPORTANT NOTICE: UNOFFICIAL FAN-MADE CONTENT",
            "1. ABSOLUTE ADMISSION OF NON-OWNERSHIP: This project is an entirely unofficial, non-commercial, fan-made application. The developer of this program explicitly admits, acknowledges, and declares that they do not own, do not hold a license to, and do not claim any proprietary rights over any intellectual property owned by Games Workshop Limited.",
            "2. EXPLICIT RECOGNITION OF GAMES WORKSHOP TRADEMARKS: All names, phrases, titles, characters, factions, logistics, lore, settings, vehicles, weapons, locations, and associated imagery used within this program are the exclusive, copyrighted property and registered trademarks of Games Workshop Limited, its subsidiaries, and its licensors. This includes, but is strictly not limited to, all content relating to: Warhammer, Warhammer 40,000, Warhammer Age of Sigmar, Space Marine, Chaos, Citadel, Forge World, and all related logos, insignia, devices, and nomenclatures.",
            "3. ZERO COMMERCE & NON-PROFIT COMPLIANCE: This project is created and distributed as a pure hobbyist endeavor. No Commercial Value: This program is 100% free of charge. No Revenue Generation: The developer receives no direct financial compensation, ad revenue, subscription fees, or other monetization. No Infringement Intended: This project is designed purely for the personal enjoyment of the fan community and is not intended to compete with, replace, or infringe upon official Games Workshop products, software, or licensed media.",
            "4. SEVERABILITY AND ABSOLUTION OF LIABILITY: By executing this application, the user acknowledges that this software is completely unaffiliated with, unauthorized by, and unendorsed by Games Workshop Limited. The developer stands completely absolved of any implication of official corporate association. If Games Workshop Limited objects to the existence of this fan project, the program will be permanently removed and terminated immediately upon official request.",
            "Selecting I AGREE writes eula_consented=true to the local settings file. Selecting EXIT or I DO NOT AGREE closes the program immediately."
        };
    }

    static String auditSummary() { return "authority=" + VERSION + " path=" + PATH + " consent=" + hasConsent() + " key=" + CONSENT_KEY; }
    private static String safe(String s) { return ChatRuntimeAuthority.ChatSecurity.sanitizeIdentifier(s == null ? "" : s); }
    private EulaConsentAuthority() {}
}

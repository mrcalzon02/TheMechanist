package mechanist;

import java.util.Properties;

/** Proves build identity resolution and release-facing runtime wording remain coherent. */
final class ReleaseBuildIdentitySmoke {
    public static void main(String[] args) {
        Properties maven = new Properties();
        maven.setProperty("version", "3.4.5m");

        BuildIdentityAuthority.Resolution explicit =
                BuildIdentityAuthority.resolve(" 1.2.3a ", "2.0.0", maven);
        require("1.2.3a".equals(explicit.version()), "explicit build version should win");
        require("system-property".equals(explicit.source()), "explicit source should be reported");

        BuildIdentityAuthority.Resolution implementation =
                BuildIdentityAuthority.resolve("", " 2.0.0b ", maven);
        require("2.0.0b".equals(implementation.version()), "manifest version should be second");
        require("jar-manifest".equals(implementation.source()), "manifest source should be reported");

        BuildIdentityAuthority.Resolution pom =
                BuildIdentityAuthority.resolve(null, null, maven);
        require("3.4.5m".equals(pom.version()), "Maven properties should be third");

        BuildIdentityAuthority.Resolution fallback =
                BuildIdentityAuthority.resolve(null, null, new Properties());
        require("development".equals(fallback.version()), "development fallback should be honest");

        require(!BuildIdentityAuthority.version().isBlank(), "runtime build version must not be blank");
        require(BuildIdentityAuthority.clientWindowTitle().contains(BuildIdentityAuthority.version()),
                "window title must use the shared build version");
        require(BuildIdentityAuthority.componentVersion("server").contains(BuildIdentityAuthority.version()),
                "server identity must use the shared build version");

        String separation = RuntimeSeparationAuthority.auditSummary(RuntimeProfile.defaultProfile());
        require(separation.contains("headlessServer=implemented"),
                "runtime separation must acknowledge the packaged headless server");
        require(separation.contains("singlePlayerInternalServer=not-yet-supervised"),
                "runtime separation must not overclaim single-player server supervision");

        System.out.println("ReleaseBuildIdentitySmoke PASS " + BuildIdentityAuthority.auditSummary());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private ReleaseBuildIdentitySmoke() { }
}

package mechanist.launcher;

/** Offline contract tests for release discovery and asset validation. */
public final class GitHubReleaseUpdateServiceSmoke {
    private static final String SHA = "a".repeat(64);

    public static void main(String[] args) throws Exception {
        String older = release(15, "111111111111", "mechanist-15.msi", true, false, "github.com", SHA);
        String latest = release(21, "222222222222", "The Mechanist 21.msi", true, false, "github.com", SHA);
        String draft = release(29, "333333333333", "mechanist-29.msi", true, true, "github.com", SHA);
        String forged = release(31, "444444444444", "mechanist-31.msi", true, false, "evil.example", SHA);
        String malformed = release(33, "555555555555", "mechanist-33.msi", true, false, "github.com", "");
        String all = "[" + older + "," + latest + "," + draft + "," + forged + "," + malformed + "]";

        GitHubReleaseUpdateService.Candidate candidate =
                GitHubReleaseUpdateService.selectLatest(all, "windows-x64", "111111111111ffffffff");
        require(candidate != null && "auto-21-222222222222".equals(candidate.tag()),
                "latest fully verified published installer must win");
        require("The Mechanist 21.msi".equals(candidate.assetName()),
                "spaces in asset names must be supported");
        require(candidate.sha256().equals(SHA), "GitHub asset SHA-256 must be used");
        require(GitHubReleaseUpdateService.selectLatest(all, "windows-x64",
                "222222222222") == null, "same commit must never offer an update");
        require(GitHubReleaseUpdateService.selectLatest(all, "macos-arm64", "unknown") == null,
                "unsupported platform must not choose a Windows/Linux installer");

        String linux = release(40, "666666666666", "mechanist_0.9.40_amd64.deb",
                true, false, "github.com", SHA);
        require(GitHubReleaseUpdateService.selectLatest("[" + linux + "]", "linux-x64",
                "unknown") != null, "Linux installer should be recognized");
        try {
            GitHubReleaseUpdateService.selectLatest("not-json", "windows-x64", "unknown");
            throw new AssertionError("Malformed JSON must fail closed");
        } catch (java.io.IOException expected) {
            // Expected.
        }
        System.out.println("GitHubReleaseUpdateServiceSmoke PASS");
    }

    private static String release(int run, String commit, String assetName,
                                  boolean prerelease, boolean draft, String host, String sha) {
        String tag = "auto-" + run + "-" + commit;
        String encodedName = assetName.replace(" ", "%20");
        return """
                {
                  "tag_name": "%s",
                  "draft": %s,
                  "prerelease": %s,
                  "assets": [{
                      "name": "%s",
                      "size": 16000000,
                      "digest": "sha256:%s",
                      "browser_download_url": "https://%s/mrcalzon02/TheMechanist/releases/download/%s/%s"
                  }]
                }
                """.formatted(tag, draft, prerelease, assetName, sha, host, tag, encodedName);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

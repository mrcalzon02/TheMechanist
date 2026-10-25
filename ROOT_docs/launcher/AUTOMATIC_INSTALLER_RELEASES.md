# Automatic installers and GitHub Releases updates

## Current distribution contract

Every push to `main` schedules **Automatic Verified Installer Release**.
The workflow calls the existing Windows/Linux `alpha-installer-deployment.yml`
lifecycle on the exact triggering SHA, using a monotonically increasing native
version `0.9.<workflow run number>`. Both platform builds must pass:

1. Java 17 updater smoke test and hardened client/server/launcher compilation.
2. Canonical distribution and SHA-256 manifest verification.
3. Windows MSI and Linux DEB creation from that exact canonical distribution.
4. Real native installation, installed launcher/game smoke, uninstallation, and
   persistence checks for user data outside the installed application.

Only after both jobs succeed does the publish job read their run artifacts,
select exactly one Windows MSI and one Linux DEB, verify their size, calculate
SHA256SUMS.txt, and publish a **prerelease** as
`auto-<run number>-<12-character commit SHA>`. Publication starts as a draft;
the release becomes visible only after uploading every asset succeeds. A
failed job or missing native installer **must not** produce a published release.

Release artifacts: https://github.com/mrcalzon02/TheMechanist/releases
Workflow: https://github.com/mrcalzon02/TheMechanist/actions/workflows/automatic-verified-installer-release.yml

Native installer signing is not required for this alpha pipeline. Unsigned MSI
files may trigger Windows SmartScreen or publisher warnings; verification with
GitHub's SHA-256 digest is an integrity check, not code signing.

## Launcher behaviour

The Java 17 launcher performs a nonblocking update check at startup and offers
a manual **Check Updates / Install Update** control. It queries the published
GitHub `/releases?per_page=30` collection instead of
`/releases/latest`, because GitHub's latest endpoint excludes prereleases.
Only published `auto-...` tags with compatible installer extensions and
GitHub-provided `sha256:` asset digests qualify.

The updater compares the installed canonical manifest's commit against the
latest release commit prefix. When an update is available, the player can opt
to download it. Downloads are staged in the per-user cache, verified using
both expected byte length and SHA-256, and passed to the OS installer:

- Windows: `msiexec.exe /i <downloaded .msi>`
- Linux desktop: `xdg-open <downloaded .deb>` (package-manager confirmation)

The launcher then exits so the native installer can replace its files. It
never replaces a running JAR or overwrites saves, profiles, settings, mods,
logs, or cache inside the native installer. Network/verification errors do not
modify the installed game. Offline use of the installed client continues.

The older "Verify Packages" and "Verify / Rollback" commands still validate
the **local manifest-backed package set**. The package source combo remains
locked to bundled limited alpha: remote branch cloning and unimplemented
main/testing/dev package channels are *not* enabled by this release updater.

## Operational verification

- A GitHub commit and file read-back prove only that workflow/source exists.
- An actual **successful workflow run** proves the installer lifecycle gate.
- A published GitHub Release with MSI, DEB, SHA256SUMS.txt, and
  release-metadata.json proves the distribution was delivered.
- An end-user test must still launch the installed native launcher, verify
  discovery, download, checksum checking, native installation and successful
  game restart on a real target OS. Earlier installed launcher builds that
  lacked the network updater require one manual installation of a new
  updater-equipped native package as their bootstrap step.

To verify CI, open the workflow URL above and inspect the newest completed
run. A pending, failed, or absent run is **not** a released installer.

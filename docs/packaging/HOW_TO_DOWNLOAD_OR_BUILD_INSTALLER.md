# How to Download or Build The Mechanist Launcher / Installer

## Current truth

The repository stores source files and packaging scripts. It does **not** store generated `.exe`, app-image, or installer outputs directly in normal source folders.

Generated native outputs are produced by the packaging pipeline and then surfaced as downloadable workflow artifacts or GitHub Release assets.

This is intentional: generated binary packages should not be committed back into the repo as source files.

## Fast path once workflow artifacts exist

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Select **Windows Native Packaging**.
4. Run the workflow manually with **Run workflow**.
5. When the run finishes, open the completed run.
6. Download the artifact named something like:

```text
TheMechanist-Windows-Native-<run number>
```

7. Extract it locally.
8. Expected downloadable ZIPs inside:

```text
TheMechanist_LauncherAppImage_<version>.zip
TheMechanist_GameAppImage_<version>.zip
TheMechanist_ServerAppImage_<version>.zip
TheMechanist_LauncherInstaller_<version>.zip   # only if installer build was requested and successful
release-manifest.json
```

## Local Windows build path

From a local clone of the repo, run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/packaging/windows/build_all_native_windows.ps1
```

This creates app-image outputs under:

```text
dist/native/windows/app-image/
dist/native/windows/game-app-image/
dist/native/windows/server-app-image/
dist/native/windows/release-manifest.json
```

Then package them into downloadable ZIPs:

```powershell
powershell -ExecutionPolicy Bypass -File tools/packaging/windows/package_native_outputs.ps1 -Version dev-local
```

Expected ZIP output:

```text
dist/native/windows/downloads/
```

## Installer EXE build

Only run the installer build after app-image smoke testing:

```powershell
powershell -ExecutionPolicy Bypass -File tools/packaging/windows/build_all_native_windows.ps1 -BuildInstaller
```

Installer generation requires the WiX Toolset to be available on PATH. If WiX is not installed, the script should fail clearly rather than silently producing nothing.

## What to test first

Start with the launcher app-image ZIP. Extract it and run the launcher executable inside it. The launcher should then be able to pull/update the game payload through the configured GitHub path.

Then test:

1. Launcher starts.
2. Runtime Info opens.
3. Diagnostics opens or writes a local report.
4. Install/Update path pulls the game.
5. Launch Game finds the game payload.
6. Server app-image starts or responds to admin/update commands.

## Why there is not a simple file in the repo root

GitHub repositories are source trees. Native executables/installers are generated build artifacts. The correct publication path is:

```text
Source repo -> packaging workflow -> downloadable artifact -> later GitHub Release asset
```

The next improvement after workflow artifacts is a release workflow that attaches the generated ZIPs directly to a GitHub Release, so testers can download them from the repository Releases page instead of Actions.

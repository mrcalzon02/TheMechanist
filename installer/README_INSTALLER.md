# The Mechanist Installer Layer

This folder contains the first lightweight installer wrapper for the GitHub-backed launcher.

## Purpose

The installer does not package the whole game into another archive. Instead, it installs a small launcher/updater locally. That launcher maintains a Git-backed game install and pulls updates from GitHub.

## Windows

Run:

```text
installer\windows\INSTALL_THE_MECHANIST_LAUNCHER.bat
```

The installer copies the Windows launcher into:

```text
%LOCALAPPDATA%\TheMechanist\launcher
```

Then it creates:

- a local launcher batch file
- a desktop shortcut unless disabled
- a Start Menu shortcut unless disabled
- an optional first update/clone of the game repository

## Linux

Run:

```bash
chmod +x installer/linux/install-mechanist-launcher.sh
./installer/linux/install-mechanist-launcher.sh
```

The installer copies the Linux launcher into:

```text
$XDG_DATA_HOME/TheMechanist/launcher
```

or:

```text
$HOME/.local/share/TheMechanist/launcher
```

Then it creates:

- a `the-mechanist` command wrapper under `$HOME/.local/bin`
- a desktop entry under `$HOME/.local/share/applications`
- an optional first update/clone of the game repository

## Current limitations

This is still a lightweight installer wrapper, not a signed production installer.

It still requires:

- Git installed locally
- GitHub authentication configured locally because the repository is private
- Java 17+ for the game runtime

## Future installer work

- signed Windows installer
- Linux AppImage/deb/rpm packaging
- release channel selector UI
- self-repair for broken local clones
- save/settings migration outside tracked Git folders
- hash/signature validation for release builds
- security/network hardening bundle

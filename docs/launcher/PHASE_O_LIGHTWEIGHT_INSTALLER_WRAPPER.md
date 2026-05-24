# Phase O — Lightweight Installer Wrapper

## Purpose

Phase O adds lightweight installer wrappers around the GitHub launcher created in Phase N.

The goal is not to create a final signed installer yet. The goal is to give Windows and Linux users a simple first-run setup path that installs the launcher outside the checked-out game repository, creates shortcuts, performs an initial update/clone, and then lets the launcher maintain the local game install from GitHub.

## Windows files

- `installer/windows/InstallMechanistLauncher.ps1`
- `installer/windows/INSTALL_THE_MECHANIST_LAUNCHER.bat`

Default install root:

```text
%LOCALAPPDATA%\TheMechanist
```

Installed launcher location:

```text
%LOCALAPPDATA%\TheMechanist\launcher
```

Game repository location:

```text
%LOCALAPPDATA%\TheMechanist\repo
```

The installer creates a desktop shortcut and Start Menu shortcut unless disabled with flags.

## Linux files

- `installer/linux/install-mechanist-launcher.sh`

Default install root:

```text
$HOME/.local/share/TheMechanist
```

Installed launcher location:

```text
$HOME/.local/share/TheMechanist/launcher
```

Game repository location:

```text
$HOME/.local/share/TheMechanist/repo
```

The installer creates:

```text
$HOME/.local/bin/the-mechanist
$HOME/.local/share/applications/the-mechanist.desktop
```

## Current limitations

- Git is still required.
- The repo is private, so GitHub authentication must already be configured.
- The Windows installer uses shortcut creation through WScript.Shell.
- The Linux desktop entry runs in a terminal for now.
- This does not yet include signing, release channels, or a recovery UI.

## Next pass

The next pass should add:

- channel selector: stable/testing/dev
- self-repair mode for broken local clones
- explicit save/settings directories outside the repo
- local validation of required runtime files
- user-facing logs for update failures
- eventual signed installer packaging

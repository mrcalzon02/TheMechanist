# The Mechanist Launcher / Updater

This folder contains the first GitHub-backed launcher and updater layer.

## Goal

The project should no longer require a fresh full archive download after every development step. The canonical project state is now this GitHub repository. A local install can update by pulling changed files from GitHub.

## Current launcher model

The existing root launch scripts remain valid:

- `RUN_THE_MECHANIST_WINDOWS.bat`
- `RUN_THE_MECHANIST_WINDOWS.ps1`
- `PLAY_THE_MECHANIST_LINUX.sh`

The launcher/updater scripts in this folder sit one level above that. They maintain a local clone/install directory, update it from GitHub, and then call the normal run script.

## Windows quick start

Run:

```powershell
.\launcher\windows\MechanistLauncher.ps1
```

or double-click:

```text
launcher\windows\RUN_MECHANIST_LAUNCHER.bat
```

Default install location:

```text
%LOCALAPPDATA%\TheMechanist\repo
```

## Linux quick start

Run:

```bash
chmod +x launcher/linux/mechanist-launcher.sh
./launcher/linux/mechanist-launcher.sh
```

Default install location:

```text
$HOME/.local/share/TheMechanist/repo
```

## Private repository note

This repository is private. Git authentication must already be available on the machine. The simplest path is to sign in with GitHub Desktop or Git Credential Manager before running the launcher/updater.

## Art tiers

The repository now contains the generated art tiers directly:

- `low_32`
- `standard_64`
- `intermediate_128`
- `high_native`

The game still supports generated payload roots, but the launcher path now assumes the integrated repository tree is authoritative.

## Safety policy

The updater preserves local runtime state outside the repository where possible. Saves and logs should not be stored inside tracked repo folders long-term.

This is the first assembled launcher layer. Installer packaging, desktop shortcuts, release channels, signature validation, and network/security hardening remain future passes.

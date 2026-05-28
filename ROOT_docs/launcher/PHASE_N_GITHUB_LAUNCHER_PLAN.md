# Phase N — GitHub Launcher / Updater Assembly

## Purpose

The project has moved from chat-delivered archive handoffs into a GitHub-backed workflow. The launcher layer should let a local install update from GitHub and then run the normal game launch script.

## What this phase adds

- Windows GitHub launcher script
- Windows double-click batch wrapper
- Linux GitHub launcher script
- Launcher documentation

## Design

The GitHub launcher maintains a local Git clone of the repository:

- Windows default: `%LOCALAPPDATA%\\TheMechanist\\repo`
- Linux default: `$HOME/.local/share/TheMechanist/repo`

On each run it:

1. verifies Git is installed
2. clones the repository if missing
3. fetches the selected branch
4. fast-forwards the local install
5. runs the normal platform launcher

## Why fast-forward only

The launcher uses fast-forward updates to avoid overwriting user-modified files silently. If local files are changed inside the repository, the user should either move personal data outside the tracked tree or resolve the Git state manually.

## Private repo requirement

The repository is private. A user must authenticate Git once on the machine through Git Credential Manager, GitHub Desktop, or another Git-compatible credential flow.

## Future work

- Installer wrapper that places the launcher outside the repo
- Desktop shortcuts
- branch/channel selector: stable, testing, dev
- update status UI
- local save/settings migration outside tracked repo tree
- optional self-repair mode for broken local clones
- signature/hash verification for release builds
- network/security hardening bundle

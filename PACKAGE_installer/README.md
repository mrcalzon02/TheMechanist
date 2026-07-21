# The Mechanist — Installer Workspace

This folder owns installer and native packaging work.

The intended user path is:

```text
installer → thin launcher → client → server
```

The installer should install the smallest durable launcher/orchestrator layer required to bootstrap the rest of the application. It should not require the user to download the whole development repository, and it should not treat the repository root as the runtime layout.

## Installer responsibilities

The installer owns:

- native installer generation and platform packaging,
- installation location selection and shortcut/menu integration,
- bundled launcher runtime image where applicable,
- installer-visible application icon and metadata,
- initial package manifest placement,
- initial package cache or bootstrap seed when included,
- clear diagnostics for missing packaging tools during development builds.

## Package layout target

Installer outputs should stage or support this launcher-managed layout:

```text
manifests/
packages/client/
packages/server/
packages/support/lib/
```

The package manifest must describe artifact identity, version, platform, paths, hashes, sizes, and compatibility requirements. The launcher owns package acquisition and verification after installation.

## Local Codex alpha builds

When GitHub Actions cannot be used, follow the authoritative operator protocol in:

```text
ROOT_docs/LOCAL_RELEASE_SEQUENCE.md
```

That protocol owns clean-candidate preparation, the governed repository-manifest review stop, the complete local release gate sequence, Windows and Linux installer commands, checksum handling, manual app-image and installer-lifecycle testing, artifact custody, Codex reporting, and the publication boundary.

Do not invoke the platform packaging scripts against an arbitrary Maven output or stale distribution. They must consume the exact release-hardened canonical distribution produced and verified by the local release sequence for the current clean `main` commit.

## Non-responsibilities

The installer should not own gameplay state, saves, mutable user settings, active mods, world files, server authority, or live package self-modification after installation. Mutable state belongs to the user-storage authority.

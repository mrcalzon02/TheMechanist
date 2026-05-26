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

## Non-responsibilities

The installer should not own gameplay state, saves, mutable user settings, active mods, world files, server authority, or live package self-modification after installation. Mutable state belongs to the user-storage authority.

# The Mechanist — Repository Workspace

The root of this repository is a development workspace, not the final client package layout.

The intended user delivery path is:

```text
installer → thin launcher → client → server
```

## Top-level workspace map

- `client/` — client/runtime distribution boundary. Client-facing readme, runtime package notes, shipped client/server package contents, and client distribution rules belong here.
- `launcher/` — thin launcher and package-orchestration workspace. Manifest verification, acquisition/update/rollback, diagnostics, and launch handoff belong here.
- `installer/` — installer/native packaging workspace. Installer metadata, packaging materials, and installer-facing docs belong here.
- `docs/` — durable governance, standards, development history, and master plan only.
- `src/` — Java source tree while the build remains Maven-rooted.
- `assets/` — source/runtime asset inputs while the asset and package pipelines remain Maven-rooted.
- `scripts/` — development/build automation. Scripts should migrate under their owning workspace as the build system is refactored.
- `modding/` — public modding API references and examples where present.
- `tools/` — developer tooling and verification helpers.

## Current development focus

The current line is public-safe rebase and delivery-path cleanup. Older prototype identity is being replaced by original civic-industrial setting architecture while preserving the simulation/client/server systems that remain useful.

## Durable governance

Read these before code/package work:

- `docs/MASTER_DEVELOPMENT_PLAN.md`
- `docs/STANDARDS_AND_PRACTICES.md`
- `docs/DEVELOPMENT_HISTORY.md`
- `docs/MASTER_GOVERNANCE_REVISION_II.md`

## Distribution notes

A user should not need to clone or download the whole repository to run the launcher. The installer should install the thin launcher; the launcher should acquire and verify client, server, and support-library packages through manifests. The game client may verify package completeness, but it must not download dependencies during game launch.

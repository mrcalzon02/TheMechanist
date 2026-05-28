# The Mechanist - Repository Workspace

This repository is the development workspace for The Mechanist. It is not the final installed client layout.

The intended user delivery path is:

```text
PACKAGE_installer -> PACKAGE_launcher -> PACKAGE_client -> packaged server payload
```

## Top-Level Workspace Map

- `PACKAGE_client/` - shipped client/runtime package boundary. Client launch files, runtime assets, config, bundled libraries, server payload material, modding examples, and client-facing notes belong here.
- `PACKAGE_launcher/` - thin launcher and package-orchestration workspace. Launcher code, launcher resources, package acquisition, verification, diagnostics, update, rollback, and launch handoff belong here.
- `PACKAGE_installer/` - installer/native packaging workspace. Installer scripts, wrapper material, packaging notes, and installer-facing docs belong here.
- `ROOT_docs/` - durable governance, standards, development history, milestone planning, handoff, and archived planning material.
- `ROOT_SRC_assets/` - protected source asset vault. These files are upstream source material and must not be edited in place.
- `ROOT_tools/` - developer tooling and verification helpers that are not part of the shipped runtime package.
- `scripts/` - current build/package automation that has not yet been moved under a tighter owning workspace.
- `src/` - Java source tree while the build remains Maven-rooted.

## Asset Rule

Do not use documentation manifests as a substitute for the real file architecture.

`ROOT_SRC_assets/` is the preserved source vault. When an asset is transformed, renamed, compressed, resized, cleared for runtime use, or otherwise made game-ready, place the resulting file in the folder where the consuming runtime actually loads it, such as `PACKAGE_client/assets/` or `PACKAGE_launcher/java/src/main/resources/assets/`. Documentation may describe the rule, but it must not create a pointer-only layer that pretends assets have moved.

Generated indexes may exist only when code, tooling, or packaging consumes them. They are not the authority for where files belong.

## Current Development Focus

The current line is public-safe rebase and delivery-path cleanup. Older prototype identity is being replaced by original civic-industrial setting architecture while preserving useful simulation, client, launcher, and server systems.

## Durable Governance

Read these before code, package, or asset work:

- `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
- `ROOT_docs/STANDARDS_AND_PRACTICES.md`
- `ROOT_docs/DEVELOPMENT_HISTORY.md`
- `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`

## Distribution Notes

A user should not need the whole development repository to run the launcher. The installer should install the thin launcher; the launcher should acquire and verify client, server, and support-library packages. Package manifests are allowed for acquisition and integrity checks, but they must not replace the physical runtime asset layout. The game client may verify package completeness, but it must not download dependencies during game launch.

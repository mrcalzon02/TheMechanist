# New Conversation Handoff - The Mechanist

## Current Checkpoint

The active checkpoint is Phase 4 publish-safe client containment and package/handoff hygiene, with an immediate documentation repair focus: make the written project map match the implemented `ROOT_*` and `PACKAGE_*` structure.

Before any code, package, or asset pass, read:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md`
3. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`
4. `ROOT_docs/DEVELOPMENT_HISTORY.md`
5. `ROOT_docs/MILESTONE_INDEX.md`
6. `ROOT_docs/LEGACY_MILESTONE_SOURCE_MAP.md`

## Active Workspace Boundaries

- Keep durable planning in `ROOT_docs/`, preferably in the master plan, standards, development history, governance, or ordered milestone sequence.
- Treat `ROOT_SRC_assets/` as protected source material. Do not modify those files in place.
- Put transformed, renamed, compressed, cleared, or runtime-ready assets into the consuming package tree, usually `PACKAGE_client/assets/` or `PACKAGE_launcher/java/src/main/resources/assets/`.
- Keep client launch material under `PACKAGE_client/`, launcher bootstrap work under `PACKAGE_launcher/`, installer work under `PACKAGE_installer/`, server launch/package material under `PACKAGE_client/server/`, and developer tooling under `ROOT_tools/` or `scripts/`.
- Do not create pointer-only docs, manifest-only maps, or placeholder README layers to stand in for moving files into the correct architecture.

## Gate Status

Gate 1 documentation and repository hygiene is reopened for structure alignment. The older docs still described pre-reorganization paths such as `docs/`, `client/`, `launcher/`, `installer/`, `assets/`, and `tools/`. Those names are no longer the repository map. The active map is `ROOT_docs/`, `ROOT_SRC_assets/`, `ROOT_tools/`, `PACKAGE_client/`, `PACKAGE_launcher/`, and `PACKAGE_installer/`.

Gate 2 remains unfinished. The execution path is still installer -> thin launcher -> client/server payloads. Package manifests are acceptable for acquisition, verification, update, rollback, and integrity checks, but not as a fake composition layer for runtime assets. Runtime assets must physically live under the package tree that consumes them.

Gate 3 has begun with player-facing readability slices. Continue keeping raw IDs, debug residue, registry plumbing, manifest keys, and package implementation details out of ordinary UI unless the surface is explicitly an audit/developer surface.

## Next Pass

Continue the Gate 1/Gate 2 bridge by making docs and package guidance reflect the actual structure. Do not broaden into worldgen, live external mod loading, public multiplayer networking, or unrelated simulation systems during this repair line.

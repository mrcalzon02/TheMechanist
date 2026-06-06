# New Conversation Handoff - The Mechanist

## Current checkpoint

Active checkpoint: **Phase 4 publish-safe client containment and package/handoff hygiene**.

This briefing is intentionally short. It is not a second master plan, changelog, roadmap, standards file, or substitute for the durable authority documents.

## Required reading order

Before any code, package, asset, or documentation pass, read:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md`
3. `ROOT_docs/DOCUMENTATION_STANDARDS.md`
4. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`
5. `ROOT_docs/DEVELOPMENT_HISTORY.md`
6. `ROOT_docs/MILESTONE_INDEX.md`
7. `ROOT_docs/LEGACY_MILESTONE_SOURCE_MAP.md`
8. The one ordered milestone file that owns the current narrow task.

Additional required reading by work type:

- Asset-loader, image, tile, portrait, item-icon, machine-art, fixture-art, or Infopedia asset work: read `ROOT_docs/STAGED_ASSET_INTEGRATION_PLAN.md`.
- Package, launcher, installer, runtime-manifest, or support-library work: read `PACKAGE_installer/PACKAGING_PIPELINE.md` and `PACKAGE_launcher/README_LAUNCHER.md`.
- Java module mapping, generated error repair, or subsystem remap work: read `ROOT_docs/functionmap/Mermaid_Code_Map_Master.md` and use `ROOT_tools/functionmap/BUILD_MERMAID_CODE_MAP.py --apply` for regeneration.
- Repository inventory, added/removed/moved file cleanup, or storage-location cleanup: read `ROOT_docs/DOCUMENTATION_STANDARDS.md` and run `ROOT_tools/update-repository-file-manifest.ps1` after the file operations.

## Active workspace boundaries

- `ROOT_docs/` is the durable development-document root. Do not create a root-level `docs/` tree for project documentation.
- `ROOT_tools/` is the durable development-tool root. Durable scripts, auditors, scanners, indexers, packaging helpers, and generator tools belong here.
- `ROOT_build/` is the durable build-operations root for checked-in build definitions, build docs, and build orchestration helpers. Generated build outputs still belong in ignored runtime build output folders unless explicitly packaged.
- `ROOT_SRC_assets/` is protected source material. Do not modify source assets in place.
- Runtime-ready assets belong in the consuming package tree, usually `PACKAGE_client/assets/` or `PACKAGE_launcher/java/src/main/resources/assets/`.
- Current delivery path remains `PACKAGE_installer -> PACKAGE_launcher -> PACKAGE_client -> packaged server payload`.
- The launcher owns acquisition, verification, update, rollback, diagnostics, and launch handoff.
- The client must not opportunistically download support libraries at game launch.
- `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` is a discovery index only, not a runtime composition layer or architecture authority.
- Do not create pointer-only docs, manifest-only maps, temporary audit notes, duplicate roadmap files, root-level scratch files, or new top-level storage roots unless the user explicitly orders a separate artifact.

## Current state summary

- Gate 1 documentation and repository-layout hygiene use the implemented `ROOT_*` and `PACKAGE_*` structure.
- Gate 2 launcher/package identity has local manifest verification, local package seed acquisition, support-library hash checks, compatibility checks, and rollback repair. Remote artifact authentication, signing/trust metadata, and full native package verification remain open.
- Gate 3 / Milestone 02 remains the strongest recent implementation lane. Continue it only through exact, testable migrations tied to player-facing readability, input, movement, Examine, Infopedia, or menu containment.
- Gate 4 local package seed staging is owned by `ROOT_tools/packaging/stage_local_package_seed.ps1`; full release packaging still requires Java 17 compile, smokes, jar rebuild, classfile scan, platform-native package verification, and integrity checks.
- `GamePanel.java` is intentionally absent. The active transitional compatibility surface is `GamePanel` inside `src/mechanist/LegacyPanelContext.java`, extending `LegacyPanelBridgeBase`. Do not re-inflate it; retarget touched dependencies to narrower authorities when safe.

## Required update discipline

After every completed code, package, asset, or documentation pass:

- Update `ROOT_docs/DEVELOPMENT_HISTORY.md` with what changed, why it matters, what verification ran, and what was not tested.
- If any file is added, removed, renamed, moved, or replaced, regenerate `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` with `ROOT_tools/update-repository-file-manifest.ps1`.
- If a Java module is added, moved, renamed, repaired after generated errors, or remapped, update the Mermaid map and position ledger before claiming the pass complete.
- If a durable tool is added or moved, keep it under `ROOT_tools/` and update any command references that still point to old `scripts/`, `tools/`, or root-level scratch paths.
- If the active checkpoint, gate, or milestone order changes, update `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`.
- If a durable implementation rule changes, update `ROOT_docs/STANDARDS_AND_PRACTICES.md`.
- If a documentation/storage rule changes, update `ROOT_docs/DOCUMENTATION_STANDARDS.md`.
- If a long-term doctrine or architecture boundary changes, update `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`.
- State verification honestly. A connector-only documentation pass is not a compile, not a smoke test, not a jar rebuild, not a classfile scan, and not a package integrity check.

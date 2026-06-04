# New Conversation Handoff - The Mechanist

## Current checkpoint

Active checkpoint: **Phase 4 publish-safe client containment and package/handoff hygiene**.

Primary orientation snapshot for the next conversation:

- `ROOT_docs/PROJECT_STATE_ONBOARDING_ASSESSMENT.md`

This briefing is intentionally short. It is not a second master plan, a changelog, or a substitute for the durable authority documents.

## Required reading order

Before any code, package, asset, or documentation pass, read:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md`
3. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`
4. `ROOT_docs/DEVELOPMENT_HISTORY.md`
5. `ROOT_docs/MILESTONE_INDEX.md`
6. `ROOT_docs/LEGACY_MILESTONE_SOURCE_MAP.md`
7. `ROOT_docs/PROJECT_STATE_ONBOARDING_ASSESSMENT.md`

Additional required reading by work type:

- Asset-loader, image, tile, portrait, item-icon, machine-art, fixture-art, or Infopedia asset work: read `ROOT_docs/STAGED_ASSET_INTEGRATION_PLAN.md`.
- Package, launcher, installer, runtime-manifest, or support-library work: read `PACKAGE_installer/PACKAGING_PIPELINE.md` and `PACKAGE_launcher/README_LAUNCHER.md`.
- Java module mapping, code architecture, generated error repair, or subsystem remap work: read `ROOT_docs/functionmap/Mermaid_Code_Map_Master.md` and verify or regenerate the generated Mermaid ledgers required by standards.

## Active workspace boundaries

- `ROOT_docs/` is the durable development-document root.
- `ROOT_SRC_assets/` is protected source material. Do not modify source assets in place.
- Runtime-ready assets belong in the consuming package tree, usually `PACKAGE_client/assets/` or `PACKAGE_launcher/java/src/main/resources/assets/`.
- Current delivery path remains `PACKAGE_installer -> PACKAGE_launcher -> PACKAGE_client -> packaged server payload`.
- The launcher owns acquisition, verification, update, rollback, diagnostics, and launch handoff.
- The client must not opportunistically download support libraries at game launch.
- `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` is a discovery index only, not a runtime composition layer or architecture authority.
- Do not create pointer-only docs, manifest-only maps, temporary audit notes, or duplicate roadmap files unless the user explicitly orders a separate artifact.

## Current state summary

- Gate 1 documentation and repository-layout hygiene repaired the active map to the implemented `ROOT_*` and `PACKAGE_*` structure.
- Gate 2 launcher/package identity has local manifest verification, local package seed acquisition, support-library hash checks, compatibility checks, and rollback repair. Remote artifact authentication, signing/trust metadata, and full native package verification remain open.
- Gate 3 / Milestone 02 is the strongest recent implementation lane: player-facing text containment, movement planning, Examine, Infopedia, prompt lines, transfer guidance, pet care, market legality, base storage, faction roster, medical status, and quest evidence readability are recorded in development history.
- Gate 4 has a local package seed builder path, but full release packaging still requires Java 17 compile, smokes, jar rebuild, classfile scan, platform-native package verification, and integrity checks.
- `GamePanel.java` is intentionally absent. The active transitional compatibility surface is `GamePanel` inside `src/mechanist/LegacyPanelContext.java`, extending `LegacyPanelBridgeBase`. Do not re-inflate it; retarget touched dependencies to narrower authorities when safe.
- The Mermaid master map reports 391 mapped Java modules, 0 unpositioned modules, and 11 oversized mapped modules. The generated ledger paths named by the map should be verified or regenerated before claiming future module-map completion.
- The semantic asset migration is complete through Stage 9. The next asset-system target is Stage 10: controlled mod/art-pack semantic registry extension.

## Required update discipline

After every completed code, package, asset, or documentation pass:

- Update `ROOT_docs/DEVELOPMENT_HISTORY.md` with what changed, why it matters, what verification ran, and what was not tested.
- If any file is added, removed, renamed, moved, or replaced, regenerate `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` with `ROOT_tools/update-repository-file-manifest.ps1`.
- If a Java module is added, moved, renamed, repaired after generated errors, or remapped, update the Mermaid map and position ledger before claiming the pass complete.
- If the active checkpoint, gate, or milestone order changes, update `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`.
- If a durable implementation rule changes, update `ROOT_docs/STANDARDS_AND_PRACTICES.md`.
- If a long-term doctrine or architecture boundary changes, update `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`.
- State verification honestly. A connector-only documentation pass is not a compile, not a smoke test, not a jar rebuild, not a classfile scan, and not a package integrity check.

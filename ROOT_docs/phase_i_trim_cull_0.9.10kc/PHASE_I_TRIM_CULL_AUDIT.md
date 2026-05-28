# Phase I — Zealous Trim / Culling Package Audit

Base package audited:
`Mech_0.9.10kc_expunged_with_phase_h_tiered_art_packages.zip`

Trimmed package produced:
`Mech_0.9.10kc_core_trimmed_handoff.zip`

## Size result
- Before trim, uncompressed payload: 220.08 MiB
- After trim, uncompressed payload: 41.69 MiB
- Removed by culling: 178.40 MiB
- Files removed: 3168

## Major culls
- Removed old Phase A-G detailed documentation/output payloads from the runnable project.
- Removed local `saves/` and `logs/`.
- Removed `build/` compiled classes and smoke artifacts.
- Removed duplicate generated-art manifests outside `assets/indexes/`.
- Removed legacy registry previews/import-era indexes not used by runtime.
- Removed stale art import tools and old source-intake skeleton paths.
- Removed installer/deployment/security/obfuscation scaffolding that is explicitly deferred.
- Removed accidental `.class` files from `src/`.

## Runtime-critical content retained
- Root runnable jars: `TheMechanist.jar`, `TheMechanistServer.jar`
- Windows launch scripts
- Linux launch scripts
- Java source tree under `src/`
- Active settings/config
- Active low-tier generated art under `assets/graphics/generated/low_32/`
- Active runtime indexes under `assets/indexes/`
- Core sound assets
- Concise handoff/deferred-work documentation

## External art rule retained
The core package contains only bundled `low_32` art. Higher art tiers remain external downloads.

## Known caveat
The previously surfaced H2 project zip was not used as the base because the local copy failed central-directory ZIP validation. The H2 high/native sharding notes were copied from the intact H2 report folder into this trimmed handoff instead. The code/runtime base is the valid Phase H project zip, which already contains the Phase F/G runtime work required for external payload mounting.

## Audit files
- `removed_files_manifest.csv`
- `remaining_files_manifest.csv`
- `package_size_by_top_level.csv`
- `NEW_CONVERSATION_HANDOFF_0.9.10kc.md`
- `FINAL_DOWNLOAD_PATH_INDEX.md`
- `INSTALLER_AND_SECURITY_DEFERRED.md`

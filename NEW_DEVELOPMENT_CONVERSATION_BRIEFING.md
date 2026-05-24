# New Conversation Handoff — The Mechanist 0.9.10kc Core Trim

## Current state
The project has completed the graphical source rebase and external art payload split. The core game now carries the bundled `low_32` generated assets required by the active semantic registry. Higher tiers are external packages and must remain out of the core game zip.

The active core package for the next conversation is:

`Mech_0.9.10kc_core_trimmed_handoff.zip`

The active external art package model is:

- core game: bundled `low_32`
- optional external package: `standard_64`
- optional external package: `intermediate_128`
- optional external package: `high_native` split into four shards

## Runtime graphics configuration already implemented
The options/runtime bridge supports:

- `generatedAssetPayloadRoot=` in `settings/options.properties`
- `mechanist.generatedAssetRoot`
- `mechanist.assetPayloadRoot`
- `mechanist.assetTier`
- `mechanist.graphicsTier`

The generated-art runtime supports external payload layouts:

- `assets/graphics/generated/<tier>/<sheet>/<file>.png`
- `exports/<tier>/<sheet>/<file>.png`

The UI exposes:

- `PAYLOAD`
- `CLEAR PAYLOAD`
- `ART QUALITY`

## What was trimmed before handoff
The package was aggressively culled to remove old phase-output documentation, generated QA payloads, build smoke artifacts, logs, saves, duplicated manifests, legacy registry previews, old import tools, installer/deployment scaffolding, obfuscation/security scaffolding, and accidental `.class` files inside `src/`.

The active retained runtime indexes are:

- `assets/indexes/semantic_asset_registry.tsv`
- `assets/indexes/semantic_portrait_entity_partitions.tsv`
- `assets/indexes/runtime_asset_manifest.json`
- `assets/indexes/tier_path_manifest.json`

The active retained generated art payload inside the core is:

- `assets/graphics/generated/low_32/...`

## Installer/security status
Installer packaging and network/security hardening were intentionally deferred. They should not be forced into this conversation's final package. The next conversation should resume them as separate phases after validating that the trimmed core launches correctly.

## Recommended next phases

### Phase J — Trimmed Core Launch Validation
Validate that the trimmed package launches on Windows and Linux using the root launch scripts. Confirm bundled `low_32` art loads without an external payload. Confirm that choosing an external `standard_64` payload changes resolved generated asset paths.

### Phase K — Semantic QA Promotion
Continue replacing unresolved or legacy semantic registry rows and attach richer tags/use-cases to generated assets so room/faction/object selection becomes semantically meaningful rather than merely path-resolvable.

### Phase L — Installer Packaging
Only after the trimmed runtime is verified, rebuild Windows/Linux installers around the trimmed package. The installer should not include external high-tier art by default. It should include UI/path support for selecting optional art packages.

### Phase M — Network/Security Hardening
Revisit the network security bundle after core runtime and installers are stable. Treat security hardening as a focused implementation phase with smoke tests, not as incidental packaging sprawl.

## Do not regress these rules
- Do not re-import old art trees into the core zip.
- Do not bundle `standard_64`, `intermediate_128`, or `high_native` into the core zip.
- Do not keep generated QA contact sheets or old phase payloads in the runnable project.
- Do not resurrect the unified TAR as the player-facing art distribution method.
- Keep `low_32` bundled as the safe fallback.


---

# The Mechanist 0.9.10kc — Final Download Path Index

## Core game / development handoff package
- `Mech_0.9.10kc_core_trimmed_handoff.zip`

This is the lean current core package. It contains the Java source, root runnable jars, Windows/Linux launch scripts, active settings/config, active runtime indexes, bundled `low_32` generated art required by the active semantic registry, and concise handoff documentation.

## External art packages
Keep `low_32` bundled in the core game. Do not create a separate low-tier package.

Download/install external tiers separately:

- `mechanist_generated_art_payload_0.9.10kc_standard_64.zip`
- `mechanist_generated_art_payload_0.9.10kc_intermediate_128.zip`
- `mechanist_generated_art_payload_0.9.10kc_high_native_part01_of_04.zip`
- `mechanist_generated_art_payload_0.9.10kc_high_native_part02_of_04.zip`
- `mechanist_generated_art_payload_0.9.10kc_high_native_part03_of_04.zip`
- `mechanist_generated_art_payload_0.9.10kc_high_native_part04_of_04.zip`

## External art install rule
Extract the desired tier package to a folder and select that folder in:

`Options > Performance/Graphics > PAYLOAD`

Then choose the matching:

`Options > Performance/Graphics > ART QUALITY`

For high/native, extract all four shard zips into the same payload folder and let the directories merge. The expected merged layout is:

`assets/graphics/generated/high_native/<sheet>/<file>.png`

## Important packaging note
The earlier unified high/native TAR and single high/native ZIP are superseded. The distribution path is now tier-specific zip packages, with high/native split into four shards.

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

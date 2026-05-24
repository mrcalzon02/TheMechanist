# Phase H — Tiered External Art Package Split

The unified TAR is superseded for distribution. Phase H splits generated art into one ZIP per external graphical level.

## Distribution policy

- `low_32` remains inside the core game and is not exported as a separate external art package.
- `standard_64`, `intermediate_128`, and `high_native` are separate external payload downloads.
- Each ZIP extracts to a mountable payload root using `assets/graphics/generated/<tier>/...`.
- The Options/Launcher PAYLOAD field should point at the extracted package root.
- ART QUALITY should match the package tier.

## Packages

- `mechanist_generated_art_payload_0.9.10kc_standard_64.zip` — tier `standard_64`, PNGs 2,780, size 45.94 MB, sha256 `5418beb2dc1d1a8ffe1972a9f43628b1f416fe44dd89bdbd7c143aa4fe52345b`
- `mechanist_generated_art_payload_0.9.10kc_intermediate_128.zip` — tier `intermediate_128`, PNGs 2,780, size 167.81 MB, sha256 `7738f8e0f707cc5e5eeea7cf17894d9a13513923113d3786968fdb24d71fc585`
- `mechanist_generated_art_payload_0.9.10kc_high_native.zip` — tier `high_native`, PNGs 2,780, size 611.81 MB, sha256 `81946719115c7acdcc6410d87cbb9b2c64c1a4f9f169ff61c6df9572154fd75e`

## Installation

Extract exactly one or more tier packages wherever you want the external art payloads to live. In the game, use:

`Options > Performance/Graphics > PAYLOAD`

and select the extracted folder root for the tier you want. Then set ART QUALITY to the matching tier.

The bundled `low_32` fallback stays available even if no external payload is mounted.
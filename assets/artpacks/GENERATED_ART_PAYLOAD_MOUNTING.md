# Generated Art Payload Mounting

The lean project zip carries the active `low_32` generated art subset. Higher tiers can be mounted externally without copying hundreds of megabytes into the project.

Use JVM flags like:

```text
-Dmechanist.generatedAssetRoot=/path/to/mech_phase_b2_next_wave_and_tier_exports_0.9.10kc;/path/to/mech_phase_b3_remaining_uniform_approval_0.9.10kc
-Dmechanist.assetTier=standard_64
```

Supported payload layouts:

```text
assets/graphics/generated/<tier>/<sheet>/<file>.png
exports/<tier>/<sheet>/<file>.png
```

The second layout allows the Phase B2/B3 export folders to be mounted directly.

The fallback rule is intentional: when the requested tier is not present, runtime falls back to the bundled `low_32` assets rather than failing or showing missing-art icons.

To copy a mounted payload into the project instead of mounting it externally:

```bash
python tools/runtime/install_generated_art_payload.py --project-root . --source /path/to/payload --mode copy --tiers standard_64,intermediate_128,high_native
```

To validate a payload without copying:

```bash
python tools/runtime/install_generated_art_payload.py --project-root . --source /path/to/payload --mode validate --tiers standard_64
```

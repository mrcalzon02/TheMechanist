# Phase H2 — High/Native Art Payload Sharding

The single `high_native` art package was approximately **612 MiB**, which appears to be above the reliable artifact surfacing/download threshold. Phase H2 replaces that single package with four smaller shard zips.

## Install rule

Extract **all four** high_native part zips into the same external payload folder. Folder merging is expected. Then set:

```text
PAYLOAD = extracted shared folder
ART QUALITY = high_native
```

The expected merged layout is:

```text
assets/graphics/generated/high_native/<sheet>/<file>.png
```

## Shards

| File | Size | Sheets | PNGs |
|---|---:|---:|---:|
| `mechanist_generated_art_payload_0.9.10kc_high_native_part01_of_04.zip` | 153.2 MiB | 27 | 686 |
| `mechanist_generated_art_payload_0.9.10kc_high_native_part02_of_04.zip` | 153.22 MiB | 27 | 714 |
| `mechanist_generated_art_payload_0.9.10kc_high_native_part03_of_04.zip` | 153.16 MiB | 27 | 675 |
| `mechanist_generated_art_payload_0.9.10kc_high_native_part04_of_04.zip` | 152.08 MiB | 29 | 705 |

Low_32 remains inside the core game; `standard_64` and `intermediate_128` remain single external packages because they surfaced successfully.

Drop The Mechanist art-pack ZIP files here.
On launch, the game unpacks them into cache/artpacks. The bundled LOW 32 tier remains available inside core.
Optional overlay packs may contain only assets/a/r/tiles/quality/standard_64, intermediate_128, or high_native.
Full legacy packs containing source/Title are also accepted. Missing selected tiers fall back to bundled LOW 32.

Milestone 01 Stage 10 registry rule:
An unpacked art pack may include semantic_asset_registry.tsv at its package root. The TSV uses the same columns as the core semantic asset registry: id, type, path, name, description. Paths must stay inside the art-pack folder, duplicate IDs are rejected, and registry loading happens before world/session initialization.

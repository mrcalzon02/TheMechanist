# Shared PSX source assets — Mechanist compatibility gate

Reviewed 2026-10-18 against the [Beckenburg living master asset index](https://docs.google.com/document/d/18G144Qd0-L5rL7NdqxMykqkXaBHtu8-ytr_yYPol6rI/edit). This is a candidate assignment for a future **specific missing tile**, not a switch to a 3D renderer or authorization to overwrite the established tiles.

The Mechanist presently stores authored art in `PACKAGE_client/assets/a/r/tiles/quality/low_32/cells/`, including `Objects/Machinery.png`, `Objects/LabsChem.png`, `Objects/Service_items.png`, `Objects/Shops_Counters.png`, `Objects/Vending.png`, `Roads/`, `Vehicles/` and `Emergency_machines,1x1/`. These existing atlas classes are the primary assets. Importing a PSX mesh directly into a tile-based Java game would not produce useful gameplay art.

**P1 conditional**: consider selected industrial or electrical machinery from the purchased PSX Mega Pack's `Electronics & Misc` / `Large Props` only where a named sprite is absent from the current machinery/chemistry atlases. Examine the original GLB, bake an overhead/isometric raster aligned with existing 32px cell orientation, maintain a corresponding selection/hitbox, and verify all quality variants and category registrations. Do not use synthetic substitute thumbnails.

**P2 conditional**: Kenney urban benches, dumpsters, traffic barriers and damaged road meshes from [Converted Runtime Assets](https://drive.google.com/drive/folders/1IibnT8YL3guVbFzGFnoMhkLpxuIpgsX-) may provide settlement tiles *only after* checking for equivalents in `Objects/parks-things.png`, `Roads/` and the existing city/vehicle art. Their upstream `kenney_retro-urban-kit.zip` is an independent source with its own provenance and license record; the purchased Mega Pack license does not apply to it.

**No assignment**: Beckenburg ghost-hunting meters, modern handheld thermal cameras, game-specific chess assets, candy-space models and direct OpenGL PSX shader packs. They do not address an observed current Mechanist art gap.

Acceptance before marking integrated: evidence of a real gap; asset path, upstream package, license and SHA-256; explicit named object/tileset mapping; actual committed raster atlas change; unchanged game rules, collision and canonical visuals; Java validation; packaged client preview and remote commit readback. Preserve all original archives in Drive and only ship transformed pieces actually used by the game. No GitHub Actions introduced by this note.

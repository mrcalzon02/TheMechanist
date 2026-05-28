# Repository Manifest Audit Report

Generated UTC: `2026-05-28T23:49:19+00:00`

## Summary

- Manifest rows: `15563`
- Approximate indexed bytes: `1238747778`
- Images: `14650`
- Audio files: `261`
- Source files: `392`
- Audit issues: `328`
  - Errors: `0`
  - Warnings: `14`
  - Info: `314`
- Full issue ledger: `docs/repository_manifest_audit_issues.tsv`

## File Families

| Value | Count |
|---|---:|
| `image` | 14650 |
| `source_code` | 392 |
| `audio` | 261 |
| `document` | 185 |
| `config_or_data` | 43 |
| `spreadsheet_or_table` | 20 |
| `unknown` | 10 |
| `binary` | 2 |

## Asset Categories

| Value | Count |
|---|---:|
| `developer_tooling` | 14555 |
| `runtime_source` | 320 |
| `documentation_or_reference` | 178 |
| `sound_effect_asset` | 127 |
| `tile_or_terrain_asset` | 117 |
| `audio_asset` | 75 |
| `music_asset` | 59 |
| `repository_file` | 48 |
| `icon_asset` | 23 |
| `data_file` | 22 |
| `manifest_or_index` | 20 |
| `documentation` | 14 |
| `image_file` | 5 |

## Root Areas

| Value | Count |
|---|---:|
| `ROOT_tools` | 14547 |
| `ROOT_SRC_assets` | 340 |
| `src` | 320 |
| `PACKAGE_client` | 222 |
| `ROOT_docs` | 56 |
| `PACKAGE_launcher` | 34 |
| `docs` | 14 |
| `scripts` | 8 |
| `(repo-root)` | 7 |
| `PACKAGE_installer` | 7 |
| `settings` | 3 |
| `assets` | 2 |
| `.github` | 1 |
| `logs` | 1 |
| `profiles` | 1 |

## Largest Files

| Size Bytes | Family | Path |
|---:|---|---|
| 43451080 | `audio` | `PACKAGE_client/assets/music/wav/Rust_CrownP-1.wav` |
| 35422286 | `audio` | `PACKAGE_client/assets/Lore intro crawl New/Intro crawl voice320.wav` |
| 35422286 | `audio` | `PACKAGE_client/assets/sound/voice/new_world_intro_crawl_narration.wav` |
| 7395029 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Iron PsalmMECH2.mp3` |
| 6884669 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Velvet RopesNob3.mp3` |
| 6807510 | `audio` | `PACKAGE_client/assets/music/wav/Engine_Psalm2.wav` |
| 6791233 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Palace of GoldP-3battle.mp3` |
| 6740809 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Lowerstack PulseMainmenu.mp3` |
| 6736508 | `spreadsheet_or_table` | `docs/repository_file_manifest.tsv` |
| 6708726 | `audio` | `PACKAGE_client/assets/music/wav/Ashline_PulseMainmenu.wav` |
| 6691966 | `audio` | `PACKAGE_client/assets/music/wav/Velvet_RopesEstate3.wav` |
| 6250094 | `audio` | `PACKAGE_client/assets/music/wav/Palace_of_GoldP-3battle.wav` |
| 5376254 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/LowerstackPulseE.mp3` |
| 5036131 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Sump Dark ChoirSMut1.mp3` |
| 5018975 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Mechanical MoonHer2.mp3` |
| 4930672 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Grey Tile TriageMed1.mp3` |
| 4911997 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Lowerstack PulseCharaterSelect.mp3` |
| 4885632 | `audio` | `PACKAGE_client/assets/music/wav/LowHabitationPulseE.wav` |
| 4612434 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Chainblade ShrineGang1.mp3` |
| 4604278 | `audio` | `PACKAGE_client/assets/music/wav/Sump_Dark_ChoirOutcast1.wav` |
| 4564586 | `audio` | `PACKAGE_client/assets/music/wav/Mechanical_MoonCell2.wav` |
| 4546095 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Ration LinePDF2.mp3` |
| 4425453 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Mechanical MoonHer1.mp3` |
| 4411998 | `audio` | `PACKAGE_client/assets/music/wav/Ashline_PulseCharacterSelect.wav` |
| 4370717 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Velvet Over HiveNob1.mp3` |
| 4169450 | `audio` | `PACKAGE_client/assets/music/wav/Mechanical_MoonCell1.wav` |
| 4137152 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/Lowerstack Pulsecombat-1.mp3` |
| 4120056 | `audio` | `PACKAGE_client/assets/music/wav/Ration_LineCharter2.wav` |
| 4114531 | `audio` | `ROOT_SRC_assets/Mechanist_Music_SRC_do_not_modify/False Seal RunTrain1.mp3` |
| 4109478 | `audio` | `PACKAGE_client/assets/music/wav/Grey_Tile_Triage1.wav` |

## Highest Priority Issues

| Severity | Type | Path | Detail |
|---|---|---|---|
| `warning` | `packaged_powershell_script` | `PACKAGE_client/MAIN launchers/RUN_THE_MECHANIST_WINDOWS.ps1` | A PowerShell script exists inside a package tree. |
| `warning` | `packaged_powershell_script` | `PACKAGE_client/server/launchers/RUN_MECHANIST_SERVER_WINDOWS.ps1` | A PowerShell script exists inside a package tree. |
| `warning` | `packaged_powershell_script` | `PACKAGE_installer/windows/InstallMechanistLauncher.ps1` | A PowerShell script exists inside a package tree. |
| `warning` | `packaged_powershell_script` | `PACKAGE_launcher/windows/MechanistLauncher.ps1` | A PowerShell script exists inside a package tree. |
| `warning` | `unknown_file_family` | `.gitignore` | Extension '(none)' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `PACKAGE_client/MAIN launchers/The Mechanist.desktop` | Extension '.desktop' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `PACKAGE_client/logs/current.log` | Extension '.log' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `PACKAGE_client/settings/profile.seed` | Extension '.seed' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `ROOT_tools/atlas_asset_pipeline/LICENSE` | Extension '(none)' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `ROOT_tools/atlas_asset_pipeline/compiled_assets/.~lock.asset_content_index_256px.tsv#` | Extension '.tsv#' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `logs/current.log` | Extension '.log' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `manifest.mf` | Extension '.mf' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `manifest_server.mf` | Extension '.mf' is not classified by the scanner. |
| `warning` | `unknown_file_family` | `settings/profile.seed` | Extension '.seed' is not classified by the scanner. |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/Lore intro crawl New/Intro crawl voice320.mp3` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/Lore intro crawl New/Intro crawl voice320.mp3; ROOT_SRC_assets/ROOT_Sounds/voice/new_world_intro_crawl_narration.mp3 |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/Lore intro crawl New/Intro crawl voice320.wav` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/Lore intro crawl New/Intro crawl voice320.wav; PACKAGE_client/assets/sound/voice/new_world_intro_crawl_narration.wav |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/a/r/source/new game Intro crawl text/Text crawl.txt` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/a/r/source/new game Intro crawl text/Text crawl.txt; PACKAGE_client/assets/Lore intro crawl New/LOREINTROCRAWL.txt |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/app/icons/the-mechanist-128.png` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/app/icons/the-mechanist-128.png; PACKAGE_launcher/java/src/main/resources/assets/app/icons/the-mechanist-128.png |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/app/icons/the-mechanist-16.png` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/app/icons/the-mechanist-16.png; PACKAGE_launcher/java/src/main/resources/assets/app/icons/the-mechanist-16.png |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/app/icons/the-mechanist-256.png` | Same SHA-256 appears in 4 files: PACKAGE_client/assets/app/icons/the-mechanist-256.png; PACKAGE_client/assets/app/icons/the-mechanist.png; PACKAGE_launcher/java/src/main/resources/assets/app/icons/the-mechanist-256.png; PACKAGE_launcher/java/src/main/resources/assets/app/icons/the-mechanist.png |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/app/icons/the-mechanist-32.png` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/app/icons/the-mechanist-32.png; PACKAGE_launcher/java/src/main/resources/assets/app/icons/the-mechanist-32.png |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/app/icons/the-mechanist-48.png` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/app/icons/the-mechanist-48.png; PACKAGE_launcher/java/src/main/resources/assets/app/icons/the-mechanist-48.png |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/app/icons/the-mechanist-64.png` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/app/icons/the-mechanist-64.png; PACKAGE_launcher/java/src/main/resources/assets/app/icons/the-mechanist-64.png |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/core/ambient_alarm_far_01.wav` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/core/ambient_alarm_far_01.wav; PACKAGE_launcher/java/src/main/resources/assets/sound/core/ambient_alarm_far_01.wav |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/core/ambient_chime_01.wav` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/core/ambient_chime_01.wav; PACKAGE_launcher/java/src/main/resources/assets/sound/core/ambient_chime_01.wav |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/core/ambient_press_01.wav` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/core/ambient_press_01.wav; PACKAGE_launcher/java/src/main/resources/assets/sound/core/ambient_press_01.wav |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_01.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_01.ogg; PACKAGE_client/assets/sound/voice/blahblah_01.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_02.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_02.ogg; PACKAGE_client/assets/sound/voice/blahblah_02.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_03.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_03.ogg; PACKAGE_client/assets/sound/voice/blahblah_03.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_04.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_04.ogg; PACKAGE_client/assets/sound/voice/blahblah_04.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_05.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_05.ogg; PACKAGE_client/assets/sound/voice/blahblah_05.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_06.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_06.ogg; PACKAGE_client/assets/sound/voice/blahblah_06.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_07.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_07.ogg; PACKAGE_client/assets/sound/voice/blahblah_07.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_08.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_08.ogg; PACKAGE_client/assets/sound/voice/blahblah_08.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_09.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_09.ogg; PACKAGE_client/assets/sound/voice/blahblah_09.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_10.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_10.ogg; PACKAGE_client/assets/sound/voice/blahblah_10.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_11.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_11.ogg; PACKAGE_client/assets/sound/voice/blahblah_11.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/assets/sound/effects/Voice/blahblah_12.ogg` | Same SHA-256 appears in 2 files: PACKAGE_client/assets/sound/effects/Voice/blahblah_12.ogg; PACKAGE_client/assets/sound/voice/blahblah_12.ogg |
| `info` | `duplicate_content_hash` | `PACKAGE_client/modding/examples/ancient-xenobiology-knowledge/src/mechanist/modapi/examples/AncientXenobiologyKnowledgeMod.java` | Same SHA-256 appears in 2 files: PACKAGE_client/modding/examples/ancient-xenobiology-knowledge/src/mechanist/modapi/examples/AncientXenobiologyKnowledgeMod.java; src/mechanist/modapi/examples/AncientXenobiologyKnowledgeMod.java |
| `info` | `duplicate_content_hash` | `PACKAGE_client/modding/examples/anomalous-cosmic-sector/src/mechanist/modapi/examples/AnomalousCosmicSectorMod.java` | Same SHA-256 appears in 2 files: PACKAGE_client/modding/examples/anomalous-cosmic-sector/src/mechanist/modapi/examples/AnomalousCosmicSectorMod.java; src/mechanist/modapi/examples/AnomalousCosmicSectorMod.java |
| `info` | `duplicate_content_hash` | `PACKAGE_client/modding/examples/cybernetic-collector-faction/src/mechanist/modapi/examples/CyberneticCollectorFactionMod.java` | Same SHA-256 appears in 2 files: PACKAGE_client/modding/examples/cybernetic-collector-faction/src/mechanist/modapi/examples/CyberneticCollectorFactionMod.java; src/mechanist/modapi/examples/CyberneticCollectorFactionMod.java |
| `info` | `duplicate_content_hash` | `PACKAGE_client/modding/examples/localized-gravity-anchor/src/mechanist/modapi/examples/LocalizedGravityAnchorItemMod.java` | Same SHA-256 appears in 2 files: PACKAGE_client/modding/examples/localized-gravity-anchor/src/mechanist/modapi/examples/LocalizedGravityAnchorItemMod.java; src/mechanist/modapi/examples/LocalizedGravityAnchorItemMod.java |
| `info` | `duplicate_content_hash` | `PACKAGE_client/modding/examples/precursor-infopedia/src/mechanist/modapi/examples/PrecursorInfopediaMod.java` | Same SHA-256 appears in 2 files: PACKAGE_client/modding/examples/precursor-infopedia/src/mechanist/modapi/examples/PrecursorInfopediaMod.java; src/mechanist/modapi/examples/PrecursorInfopediaMod.java |
| `info` | `duplicate_content_hash` | `PACKAGE_client/modding/examples/reinforced-hydroponics-lab/src/mechanist/modapi/examples/ReinforcedHydroponicsLabMod.java` | Same SHA-256 appears in 2 files: PACKAGE_client/modding/examples/reinforced-hydroponics-lab/src/mechanist/modapi/examples/ReinforcedHydroponicsLabMod.java; src/mechanist/modapi/examples/ReinforcedHydroponicsLabMod.java |
| `info` | `duplicate_content_hash` | `PACKAGE_client/profiles/active_profile.properties` | Same SHA-256 appears in 2 files: PACKAGE_client/profiles/active_profile.properties; PACKAGE_launcher/profiles/active_profile.properties |
| `info` | `duplicate_content_hash` | `ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r01c01_128px.png` | Same SHA-256 appears in 2 files: ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r01c01_128px.png; ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors_r01c01_128px.png |
| `info` | `duplicate_content_hash` | `ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r01c03_128px.png` | Same SHA-256 appears in 2 files: ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r01c03_128px.png; ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors_r01c03_128px.png |
| `info` | `duplicate_content_hash` | `ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r01c04_128px.png` | Same SHA-256 appears in 2 files: ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r01c04_128px.png; ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors_r01c04_128px.png |
| `info` | `duplicate_content_hash` | `ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r02c01_128px.png` | Same SHA-256 appears in 2 files: ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r02c01_128px.png; ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors_r02c01_128px.png |
| `info` | `duplicate_content_hash` | `ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r02c02_128px.png` | Same SHA-256 appears in 2 files: ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors-open_r02c02_128px.png; ROOT_tools/atlas_asset_pipeline/compiled_assets/128px/BULKHEAD/Bulkhead_walls+doors_r02c02_128px.png |
| ... | ... | ... | 278 more issues in `docs/repository_manifest_audit_issues.tsv` |

# Initial Function Map Audit

Status: active audit layer for the first function-mapping pass.

Source reports: function-map reports generated `2026-06-01 13:33:06` and provided directly for review.

## Raw Inventory Summary

The current mapper inventory reports:

- Files inventoried: `2,149`
- Java classes/interfaces/enums/records: `1,131`
- Java functions/methods parsed: `7,130`
- Duplicate function-name groups: `490`
- Documents inventoried: `90`
- Document-retirement candidate rows: `46`
- Oversized owner rows: `41`

Interpretation: these reports are an index, not deletion authority. The mapper over-classifies some records, especially the `ui.render.surface` bucket. Every code movement still needs a human ownership decision and a smoke pass.

## First Critical Finding: GamePanel Remains the Dominant Owner

`src/mechanist/GamePanel.java` remains the largest active Java owner and must be treated as a bridge shell rather than a deletion target.

| File | Functions | Total parsed method lines | Longest method |
|---|---:|---:|---:|
| `src/mechanist/GamePanel.java` | `1,397` | `19,045` | `719` |
| `src/mechanist/WorldRuntimeGenerationFramework.java` | `449` | `6,635` | `109` |
| `src/mechanist/ProductionAuthorityFramework.java` | `314` | `2,738` | `236` |
| `src/mechanist/WorldSimulationFramework.java` | `205` | `1,534` | `63` |
| `src/mechanist/PopulationPersonnelFramework.java` | `122` | `1,030` | `41` |
| `src/mechanist/FactionServicesFramework.java` | `105` | `779` | `48` |
| `src/mechanist/ContainerTradeFramework.java` | `92` | `706` | `67` |
| `src/mechanist/FirstPerson3DFramework.java` | `78` | `877` | `48` |
| `src/mechanist/GameOptionsFramework.java` | `73` | `534` | `97` |
| `src/mechanist/ItemEconomyFramework.java` | `71` | `1,238` | `215` |
| `src/mechanist/EnvironmentSensesFramework.java` | `68` | `307` | `33` |
| `src/mechanist/SimulationEditorSuite.java` | `60` | `454` | `31` |

## Oversized Java Owners

These are active behavior concentrators. Mark them `do_not_touch` until each has a specific split plan.

| File | Bytes | Lines | Initial status |
|---|---:|---:|---|
| `src/mechanist/GamePanel.java` | `1,388,996` | `21,165` | `bridge_owner` + `move_candidate` source |
| `src/mechanist/WorldRuntimeGenerationFramework.java` | `389,004` | `8,140` | `do_not_touch` pending generation split |
| `src/mechanist/ProductionAuthorityFramework.java` | `344,937` | `3,105` | `do_not_touch` pending production split |
| `src/mechanist/ItemEconomyFramework.java` | `225,851` | `1,327` | `do_not_touch` pending catalog/economy split |
| `src/mechanist/WorldSimulationFramework.java` | `134,755` | `1,800` | `do_not_touch` pending simulation split |
| `src/mechanist/PopulationPersonnelFramework.java` | `82,504` | `1,262` | `do_not_touch` pending population split |
| `src/mechanist/ContainerTradeFramework.java` | `70,095` | `802` | `do_not_touch` pending trade split |
| `src/mechanist/FactionServicesFramework.java` | `69,597` | `939` | `do_not_touch` pending services split |
| `src/mechanist/InfrastructurePromotionRegistry.java` | `62,143` | `1,001` | `do_not_touch` pending registry split |
| `src/mechanist/FirstPerson3DFramework.java` | `54,913` | `1,042` | `do_not_touch` pending optional 3D split |
| `src/mechanist/SimulationEditorSuite.java` | `45,909` | `824` | `do_not_touch` pending editor split |

## Longest Parsed Methods

The first concrete remap batch should target UI shell methods before gameplay mutation systems.

| File | Function | Lines | Initial target |
|---|---|---:|---|
| `src/mechanist/GamePanel.java` | `rebuildButtons` | `719` | `PanelButtonCommandRouter` / panel command authorities |
| `src/mechanist/GamePanel.java` | `panelLines` | `251` | `PanelTextLineAuthority` / mode-specific line providers |
| `src/mechanist/GamePanel.java` | `handleKeyPressed` | `240` | `GamePanelKeyController` / input navigation subsystem |
| `src/mechanist/GamePanel.java` | `drawCharacter` | `169` | character/dossier surface painter |
| `src/mechanist/GamePanel.java` | `drawGame` | `137` | game surface painter / renderer bridge |
| `src/mechanist/GamePanel.java` | `drawOptions` | `130` | `OptionsScreenPainter` |
| `src/mechanist/GamePanel.java` | `infopediaDetailLines` | `140` | Infopedia authority line providers |
| `src/mechanist/GamePanel.java` | `productionInfopediaLines` | `141` | production Infopedia authority |
| `src/mechanist/GamePanel.java` | `auditInfopediaLines` | `130` | audit Infopedia authority |
| `src/mechanist/GamePanel.java` | `interactAt` | `89` | interaction dispatcher, later pass |

## Duplicate Function-Name Group Rules

Duplicate names are not automatically duplicate behavior.

| Function-name group | Initial classification | Notes |
|---|---|---|
| `main` | `expected_multi_entrypoints` | Launchers, smoke tools, demos, and command-line utilities. Do not consolidate by name alone. |
| `toString`, `equals`, `hashCode` | `expected_java_object_pattern` | Java object/record-style methods. Do not consolidate by name. |
| `encode`, `decode`, `parse`, `esc`, `unesc` | `codec_pattern_review` | Save/persistence-adjacent. High compatibility risk. Review before any utility extraction. |
| `auditSummary`, `summary`, `audit`, `auditLines` | `duplicate_candidate` | Requires source comparison. Many are legitimate local audit surfaces. |
| `label`, `statusLines`, `lines`, `compact` | `display_text_pattern_review` | Possible interface candidates, not deletion targets. |
| `remember`, `passed` | `parser_noise_review` | Treat as mapper noise until manually inspected. |

## Generated Shard Documents

Generated subsystem documents are evidence/reference material during reattachment. They should not be mixed into active long-term governance forever, but they remain useful until their corresponding Java owners are verified.

Initial decisions:

- `ROOT_DOCS/shardmining/generated_subsystems/*.txt` = `generated_reference`
- `ROOT_DOCS/shardmining/generated_subsystems/MANIFEST.md` = `active_generated_manifest`
- `_retired_shards/...` = `generated_reference_source_of_truth`
- `_backups/...` = `retire_candidate_after_hash_verification`

Do not delete generated subsystem documents during active function remapping.

## Unknown Document Review Queue

The first mapper pass left some documents as `unknown_review`. Initial treatment:

- `GAMEPANEL_STRIP_MINING_REBUILD_PLAN.md`, `GAMEPANEL_PROGRESS_MARKERS.md`, `gamepanel_catalog.md`, and `gamepanel_extraction_targets.md` are historical inputs to the current function map. Summarize into Development History before retirement.
- Packaging, installer, launcher, and native executable docs should remain parked unless they are part of the current Java 17/Swing packaging track.
- `docs/CONCORD_IP_NEUTRALIZATION_LEDGER.md` is active parked-governance, not a retirement target.

## First Remap Batch

Start with UI shell functions because they can usually be bridged without changing simulation state.

1. `GamePanel.rebuildButtons` -> target owner: `PanelButtonCommandRouter` or existing panel command authorities.
2. `GamePanel.handleKeyPressed` -> target owner: `GamePanelKeyController` / input navigation subsystem.
3. `GamePanel.drawOptions` -> target owner: `OptionsScreenPainter`.
4. `GamePanel.drawGame` and `GamePanel.drawCharacter` -> target owner: dedicated surface painters.
5. Infopedia line builders -> target owner: specific Infopedia authority classes.

Do not start with inventory, world generation, production, or simulation mutation until the UI routing shell is cleaner.

## Ledger Actions To Promote

Promote these into `Function_Ownership_Ledger.md`:

- `GamePanel.java` = `bridge_owner` and `move_candidate` source.
- Generated subsystem docs = `generated_reference`.
- `_backups` copies = `retire_candidate_after_hash_verification`.
- `main` duplicate group = expected entrypoint/smoke pattern.
- `encode/decode/parse` duplicate groups = codec-pattern review, high compatibility risk.
- `rebuildButtons` = first explicit function remap target.

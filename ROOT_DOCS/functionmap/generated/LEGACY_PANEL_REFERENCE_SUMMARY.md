# Legacy Panel Reference Summary

Generated: `2026-06-01 23:25:19`

Status: active retirement ledger for the temporary `LegacyPanelContext.java` bridge.

- Total references: `2327`
- Files with references: `106`
- Unique member references: `247`

## Top Files

- `src/mechanist/GamePanelKeyController.java`: `254`
- `src/mechanist/FirstPerson3DFramework.java`: `136`
- `src/mechanist/OptionsScreenPainter.java`: `105`
- `src/mechanist/ProductionAuthorityFramework.java`: `98`
- `src/mechanist/GameplayConsoleCommandAuthority.java`: `75`
- `src/mechanist/LayerD.java`: `73`
- `src/mechanist/AccessibilityRuntimeOptionsSubsystem.java`: `65`
- `src/mechanist/DisplayScaleOptionsSubsystem.java`: `57`
- `src/mechanist/LayerF.java`: `55`
- `src/mechanist/WorldSnapshot.java`: `50`
- `src/mechanist/MapViewportOptionsSubsystem.java`: `49`
- `src/mechanist/KeyEarlyScreenController.java`: `47`
- `src/mechanist/MouseEarlyScreenController.java`: `45`
- `src/mechanist/MouseGamePanelController.java`: `43`
- `src/mechanist/SinglePlayerSectorRuntimeBridge.java`: `43`
- `src/mechanist/JvmRuntimeOptionsSubsystem.java`: `42`
- `src/mechanist/PlayerLifecycleService.java`: `42`
- `src/mechanist/MultiplayerSurfacePainter.java`: `41`
- `src/mechanist/GeneratedArtPayloadOptionsSubsystem.java`: `40`
- `src/mechanist/SaveEfficiencyAuthority.java`: `40`
- `src/mechanist/LegacyPanelContextAdapters.java`: `39`
- `src/mechanist/AdminCommandDispatcher.java`: `37`
- `src/mechanist/MouseLateUiController.java`: `36`
- `src/mechanist/WorldCommandRuntimeContext.java`: `34`
- `src/mechanist/FactionServicesFramework.java`: `30`
- `src/mechanist/LayerC.java`: `27`
- `src/mechanist/LayerI.java`: `27`
- `src/mechanist/PanelTargetingController.java`: `27`
- `src/mechanist/UiModalButtonController.java`: `27`
- `src/mechanist/LayerG.java`: `26`

## Top Members

- `options`: `262`
- `repaint`: `108`
- `world`: `101`
- `logEvent`: `69`
- `screen`: `59`
- `jvmRuntimeProfile`: `36`
- `panelMode`: `35`
- `active`: `29`
- `atlas`: `28`
- `add`: `28`
- `graphicsDropdown`: `27`
- `playerX`: `24`
- `playerY`: `24`
- `buttons`: `24`
- `smallFont`: `23`
- `requestFocusInWindow`: `20`
- `sounds`: `20`
- `optionColor`: `19`
- `userProfile`: `13`
- `turn`: `13`
- `getHeight`: `13`
- `renderScaling`: `13`
- `multiplayerMenu`: `13`
- `optionsTab`: `13`
- `images`: `12`
- `selectedButton`: `12`
- `baseX`: `11`
- `baseY`: `11`
- `getWidth`: `10`
- `lookX`: `10`
- `lookY`: `10`
- `knowledgeCredits`: `10`
- `unlockedKnowledges`: `10`
- `baseClaimed`: `10`
- `singlePlayerSectorBridge`: `9`
- `titleFont`: `9`
- `candidates`: `9`
- `performanceDiagnostics`: `8`
- `stampUiFrameId`: `8`
- `characterNameEditActive`: `7`

## Retirement Rule

Do not add new gameplay behavior to `LegacyPanelContext.java`. Use it only to preserve compile continuity while each listed reference is retargeted to a smaller context or manager.

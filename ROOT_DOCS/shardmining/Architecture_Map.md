# Shard Mining Architecture Map

Status: canonical shard-mining system map. Always check this file before continuing GamePanel or shard-shell extraction work.

Last verified clean smoke baseline: `diagnostics/shard8_smoke_20260601_084116/compile_errors.tsv` contains only the header and no compiler errors.

## Current Refactor Goal

The current goal is to hollow out the oversized `GamePanel.java`/Shard shell surface by extracting coherent functionality into focused Java 17 Swing support classes, then deleting the mined code from the shard once the extracted authority compiles and is wired. The shard file remains source-of-truth for what has not yet been mined. Shrinking shard size is the progress metric.

## Hard Constraints

- Java 17 only.
- Swing client remains the UI shell.
- Do not casually touch `GamePanel.java`; prefer bridge/support classes unless the compiler leaves no alternative.
- Every extraction pass must preserve compileability through the smoke harness.
- Public registry/API access is required. Do not reach into private registry internals.
- Update this architecture map and the progress log after each mining pass.

## Zones and Current Files

### 1. Shell / Monolith Boundary Zone

Purpose: oversized live shell and compatibility boundary; source of truth for unmined code.

Current files:

- `src/mechanist/GamePanel.java` — oversized shard/monolith surface; avoid direct edits unless absolutely necessary.
- `src/mechanist/GamePanelKeyController.java` — extracted key controller shell.
- `src/mechanist/ScreenPainter.java` — stateless immediate-mode rendering contract.
- `src/mechanist/UiRuntimeSupportFramework.java` — shared UI support classes including `TextSurfaceApi`, `TextLayoutAuthority`, `GuiLayoutApi`, `ButtonBox`, `ScrollRegion`, and related helpers.

Mining rule: if behavior is currently in `GamePanel.java`, extract it into a named authority/subsystem, wire the smallest call-site bridge, smoke test, then remove the mined body from the shard when safe.

### 2. Rendering / Screen Surface Zone

Purpose: draw isolated screens and UI surfaces without swelling `GamePanel.java`.

Current files:

- `src/mechanist/BootSurfacePainter.java` — boot screen painter.
- `src/mechanist/IntroCrawlSurfacePainter.java` — conservative intro crawl bridge added after clean compile pass.
- `src/mechanist/OptionsScreenPainter.java` — extracted options screen body/shell painter.
- `src/mechanist/RenderScalingCrtAuthority.java` — render scaling / CRT-adjacent runtime authority.
- `src/mechanist/FramePacingAndStressFramework.java` — frame pacing and stress profile support.

Current status: `IntroCrawlSurfacePainter` is a conservative bridge, not the final rich crawl implementation. It exists to preserve compile/run continuity without touching `GamePanel.java`.

### 3. Runtime Options / Display / JVM Zone

Purpose: options state, option rendering, display scaling, runtime profile switching, and Java2D/JVM settings.

Current files:

- `src/mechanist/GameOptionsFramework.java` — owns `GameOptions` and persistent runtime option values.
- `src/mechanist/LayerG.java` — option action bridge for display/art/map zoom operations.
- `src/mechanist/GeneratedArtPayloadOptionsSubsystem.java` — generated asset payload root controls and art-quality cycling.
- `src/mechanist/DisplayScaleOptionsSubsystem.java`
- `src/mechanist/AccessibilityRuntimeOptionsSubsystem.java`
- `src/mechanist/JvmRuntimeOptionsSubsystem.java`
- `src/mechanist/JvmRuntimeProfileAuthority.java`
- `src/mechanist/OptionsScreenPainter.java`
- `src/mechanist/OptionsBoundaryAuthority.java` if present in source tree.

Recent repair: `LayerG`, `GeneratedArtPayloadOptionsSubsystem`, and `OptionsScreenPainter` now import `mechanist.assets.AssetManager` directly where needed.

### 4. Asset / Registry / Tile Art Zone

Purpose: semantic asset registry, loaded image registry, tile/art lookup, generated asset runtime, and Infopedia/audit surfaces.

Current files:

- `src/mechanist/TileArtSystem.java` — tile art orchestration and semantic bridge helpers.
- `src/mechanist/TileImageRegistry.java` — actual runtime image registry owner.
- `src/mechanist/TileInfopediaAuthority.java` — tile reference/Infopedia ledger.
- `src/mechanist/TileSemanticAssetAuthority.java`
- `src/mechanist/SemanticAssetInfopediaAuthority.java`
- `src/mechanist/AssetIntegrationDisciplineAuthority.java`
- `src/mechanist/GlyphBinder.java`
- `src/mechanist/RuntimePathResolver.java` — package/client root and asset path resolver.
- `src/mechanist/assets/AssetManager.java`
- `src/mechanist/assets/AssetMetadata.java`
- `src/mechanist/assets/AssetRegistry.java`
- `src/mechanist/assets/GeneratedAssetRuntime.java`
- `src/mechanist/assets/SemanticAssetResolver.java`

Important rule: registry interaction must go through public APIs such as `TileArtSystem.getRegistry()`, `TileImageRegistry.getAlias(...)`, `TileImageRegistry.aliasView()`, `AssetManager.metadata(...)`, and generated-runtime public methods. Do not add new private-field reach-through.

Current state: `TileInfopediaAuthority` now uses `art.getRegistry().getAlias(a)` for loaded-alias checks, and the temporary `TileArtSystem.byAlias` compatibility view has been removed. `TileArtSystem.semanticKeyForMapObject(...)` now uses typed `MapObjectState.label`, `type`, and `stockState` access instead of reflection, matching the existing typed semantic path in `ObjectSemanticAssetAuthority`.

### 5. Input / Controls Zone

Purpose: keyboard/controller command naming, input mapping, rebinding, and prompt text.

Current files:

- `src/mechanist/ControlReferenceTextSubsystem.java`
- `src/mechanist/InputAction.java`
- `src/mechanist/InputRegistry.java`
- `src/mechanist/InputSource.java`
- `src/mechanist/KeyboardInputBridge.java`
- `src/mechanist/GamepadInputEngine.java`
- `src/mechanist/GamepadControllerSnapshot.java`
- `src/mechanist/input/*`

Recent repair: `OptionsScreenPainter.controlsLines(...)` now calls the real `ControlReferenceTextSubsystem.controlsReferenceLines(panel)` API, not a nonexistent integer overload.

### 6. World Generation / Room / Road / Fixture Zone

Purpose: world generation, rooms, roads, fixture placement, room interactions, frontage/transit systems, and faction-aware generation hooks.

Current files:

- `src/mechanist/WorldRuntimeGenerationFramework.java`
- `src/mechanist/RoomFixtureInteractionAuthority.java`
- `src/mechanist/RoomProfile.java`
- `src/mechanist/RoadAdjacencyIntegrationAuthority.java`
- `src/mechanist/RoadFrontageFixtureAuthority.java`
- `src/mechanist/RoadGridIntegrationAuthority.java`
- `src/mechanist/RoadTransitFixtureAuthority.java`
- `src/mechanist/RoadTransitFixtureInteractionAuthority.java`
- `src/mechanist/FrontageFixtureInteractionAuthority.java`
- `src/mechanist/ArbitesPrecinctFixtureAuthority.java` — legacy class name retained for compile/save continuity; Concord-facing labels should be Civic Wardens.
- `src/mechanist/GuardPdfDefenseFixtureAuthority.java`
- `src/mechanist/NobleEstateSecurityFixtureAuthority.java`
- `src/mechanist/IndustrialForgeFixtureAuthority.java`
- `src/mechanist/FoodBioProductionFixtureAuthority.java`
- `src/mechanist/DomesticHabFixtureAuthority.java`
- `src/mechanist/BarMarketSocialFixtureAuthority.java`
- `src/mechanist/LabChemicalFixtureAuthority.java`
- `src/mechanist/MedicaeFixtureAuthority.java` if present.

Recent repairs: the illegal identifier phrase `civic Wardens` was normalized to `civicWardens` in both `RoomFixtureInteractionAuthority` and `WorldRuntimeGenerationFramework`.

### 7. Faction / Concord IP Neutralization Zone

Purpose: preserve save compatibility while migrating visible terms away from Warhammer/40K-derived language.

Current files:

- `src/mechanist/Faction.java`
- `docs/CONCORD_IP_NEUTRALIZATION_LEDGER.md`
- `scripts/AUDIT_CONCORD_IP_TERMS_WINDOWS.ps1`
- `diagnostics/concord_ip_audit_*.tsv`

Current status: IP sweep is parked as backlog. Compile bridge aliases exist in `Faction.java`:

- `CIVIC_LEDGER_OFFICE`
- `CIVIC_WARDENS`
- `MECHANIST_COLLEGIA`

Legacy constants remain for save/profile compatibility but visible labels are Concord-neutralized.

### 8. Authoritative Runtime / Server / Simulation Zone

Purpose: authoritative world runtime, internal server bridge, multiplayer preparation, and simulation management.

Current files:

- `src/mechanist/AuthoritativeWorldRuntime.java`
- `src/mechanist/AuthoritativeWorldGrid.java`
- `src/mechanist/InternalServerSessionAuthority.java`
- `src/mechanist/SinglePlayerSectorRuntimeBridge.java`
- `src/mechanist/SectorManager.java`
- `src/mechanist/LauncherClientServerRuntimeAuthority.java`
- `src/mechanist/IntegratedLocalMultiplayerHost.java`
- `src/mechanist/server/admin/*`
- `src/mechanist/TheMechanistServer.java`

Current status: not the immediate shard-mining target unless compile errors route there.

### 9. Persistence / Profile / Save Zone

Purpose: character/world save management, profile fallback, identity, and safe persistence.

Current files:

- `src/mechanist/GameStorageManager.java`
- `src/mechanist/CharacterSaveManager.java`
- `src/mechanist/CharacterStateRecord.java`
- `src/mechanist/FallbackProfileManagementAuthority.java`
- `src/mechanist/PlayerIdentity.java`
- `src/mechanist/PlayerSessionManager.java`
- `src/mechanist/PlayerWorldStateRecord.java`
- `src/mechanist/SecureLocalSaveValidationManager.java`

Current status: do not rename persisted IDs casually; use aliases/migrations where needed.

### 10. Diagnostics / Smoke / Audit Zone

Purpose: compile smoke, generated diagnostics, IP audits, and handoff verification.

Current files:

- `scripts/SMOKE_SHARD8_DIAGNOSTIC_WINDOWS.ps1`
- `scripts/EXTRACT_SMOKE_COMPILE_ERRORS_WINDOWS.ps1`
- `scripts/AUDIT_CONCORD_IP_TERMS_WINDOWS.ps1`
- `diagnostics/shard8_smoke_*/compile_errors.tsv`
- `diagnostics/shard8_smoke_*/SUMMARY.txt`

Current baseline: `diagnostics/shard8_smoke_20260601_084116/compile_errors.tsv` has zero compiler errors.

## Current Shard Mining Assignment

- Active focus: Shard 8 / UI-runtime-support and GamePanel hollowing path.
- Shards 1–7: compatibility errors cleared earlier.
- Shard 8 smoke: clean compile at `20260601_084116`.
- Next work: resume extraction passes from the shard source-of-truth, update maps/progress after each pass, run smoke, then delete mined material from the shard when the new subsystem owns it.

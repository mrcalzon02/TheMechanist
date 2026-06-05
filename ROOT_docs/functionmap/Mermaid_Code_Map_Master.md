# Mermaid Code Map Master Record

Status: active master code-position map.

Generated/evaluated: `2026-06-05 13:01:00`

## Top-Line Rule

Every code module, generated code error, compile error cluster, or subsystem remap must submit a Mermaid position before it is considered mapped. Unpositioned modules are architecture debt, not invisible implementation detail.

## Counts

- Java modules mapped: `436`
- Unpositioned modules: `0`
- Oversized mapped modules: `13`

## Zone Counts

- `ASSET_REGISTRY`: `25` modules
- `COMBAT_SIM`: `24` modules
- `DIAGNOSTIC_DOC`: `25` modules
- `FIXTURE_MACHINE`: `18` modules
- `INVENTORY_PERSIST`: `16` modules
- `LOCALIZATION_TEXT`: `66` modules
- `RUNTIME_OPTIONS`: `34` modules
- `SERVER_AUTH`: `39` modules
- `UI_INPUT`: `53` modules
- `UI_RENDER`: `95` modules
- `WORLD_GEN`: `41` modules

## Status Counts

- `positioned`: `436` modules

## Explicit Override Rule

Explicit `MODULE_OVERRIDES` entries in `scripts/BUILD_MERMAID_CODE_MAP.py` beat broad keyword heuristics. Add an override when a known owner is misclassified by generic words such as render, panel, audit, semantic, faction, or zone.

## Master Mermaid Map

```mermaid
flowchart TD
    ROOT["The Mechanist Codebase<br/>Mermaid position master"]
    ERRORS["Code errors / unmapped modules<br/>must submit map position"]
    ROOT --> ERRORS
    ROOT --> Z_LOCALIZATION_TEXT["Localization Text<br/>66 modules"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_MediaRuntimeFramework_java["src/mechanist/MediaRuntimeFramework.java<br/>56 funcs / 779 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_ModDeploymentManager_java["src/mechanist/ModDeploymentManager.java<br/>42 funcs / 551 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_AudioRuntimeFramework_java["src/mechanist/AudioRuntimeFramework.java<br/>47 funcs / 493 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_SaveEfficiencyAuthority_java["src/mechanist/SaveEfficiencyAuthority.java<br/>38 funcs / 482 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_HousingVariantRules_java["src/mechanist/HousingVariantRules.java<br/>21 funcs / 448 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_EnvironmentSensesFramework_java["src/mechanist/EnvironmentSensesFramework.java<br/>70 funcs / 432 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_ArtPackManager_java["src/mechanist/ArtPackManager.java<br/>19 funcs / 360 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_PersistenceFramework_java["src/mechanist/PersistenceFramework.java<br/>19 funcs / 358 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_LootDropSystemAuthority_java["src/mechanist/LootDropSystemAuthority.java<br/>36 funcs / 344 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_KnowledgeBranchDefinitions_java["src/mechanist/KnowledgeBranchDefinitions.java<br/>29 funcs / 338 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_VisualLightingAuthority_java["src/mechanist/VisualLightingAuthority.java<br/>21 funcs / 332 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_ChatRuntimeAuthority_java["src/mechanist/ChatRuntimeAuthority.java<br/>25 funcs / 322 lines"]
    Z_LOCALIZATION_TEXT --> Z_LOCALIZATION_TEXT_OTHER["+ 54 smaller modules"]
    ROOT --> Z_UI_RENDER["UI Render Surfaces<br/>95 modules"]
    Z_UI_RENDER --> M_src_mechanist_LegacyPanelContext_java["src/mechanist/LegacyPanelContext.java<br/>494 funcs / 5671 lines"]
    Z_UI_RENDER --> M_src_mechanist_SimulationEditorSuite_java["src/mechanist/SimulationEditorSuite.java<br/>111 funcs / 1687 lines"]
    Z_UI_RENDER --> M_src_mechanist_WorldStartFlowAuthority_java["src/mechanist/WorldStartFlowAuthority.java<br/>115 funcs / 1093 lines"]
    Z_UI_RENDER --> M_src_mechanist_FirstPerson3DFramework_java["src/mechanist/FirstPerson3DFramework.java<br/>86 funcs / 1057 lines"]
    Z_UI_RENDER --> M_src_mechanist_VisualJuiceFramework_java["src/mechanist/VisualJuiceFramework.java<br/>56 funcs / 632 lines"]
    Z_UI_RENDER --> M_src_mechanist_EconomicTopologyFramework_java["src/mechanist/EconomicTopologyFramework.java<br/>28 funcs / 452 lines"]
    Z_UI_RENDER --> M_src_mechanist_UiRuntimeSupportFramework_java["src/mechanist/UiRuntimeSupportFramework.java<br/>50 funcs / 439 lines"]
    Z_UI_RENDER --> M_src_mechanist_CharacterIdentityFramework_java["src/mechanist/CharacterIdentityFramework.java<br/>52 funcs / 430 lines"]
    Z_UI_RENDER --> M_src_mechanist_KnowledgeQualityFramework_java["src/mechanist/KnowledgeQualityFramework.java<br/>26 funcs / 413 lines"]
    Z_UI_RENDER --> M_src_mechanist_GamePanelKeyController_java["src/mechanist/GamePanelKeyController.java<br/>20 funcs / 404 lines"]
    Z_UI_RENDER --> M_src_mechanist_LogisticsManualHaulExecutionAuthority_java["src/mechanist/LogisticsManualHaulExecutionAuthority.java<br/>24 funcs / 349 lines"]
    Z_UI_RENDER --> M_src_mechanist_ui_KeybindingRemappingPanel_java["src/mechanist/ui/KeybindingRemappingPanel.java<br/>24 funcs / 344 lines"]
    Z_UI_RENDER --> Z_UI_RENDER_OTHER["+ 83 smaller modules"]
    ROOT --> Z_UI_INPUT["UI Input Navigation<br/>53 modules"]
    Z_UI_INPUT --> M_src_mechanist_SectorManager_java["src/mechanist/SectorManager.java<br/>59 funcs / 609 lines"]
    Z_UI_INPUT --> M_src_mechanist_input_KeyBindingManager_java["src/mechanist/input/KeyBindingManager.java<br/>34 funcs / 381 lines"]
    Z_UI_INPUT --> M_src_mechanist_AdminCommandDispatcher_java["src/mechanist/AdminCommandDispatcher.java<br/>34 funcs / 342 lines"]
    Z_UI_INPUT --> M_src_mechanist_HybridEncryptionManager_java["src/mechanist/HybridEncryptionManager.java<br/>16 funcs / 322 lines"]
    Z_UI_INPUT --> M_src_mechanist_SnapshotDeltaCompressor_java["src/mechanist/SnapshotDeltaCompressor.java<br/>39 funcs / 294 lines"]
    Z_UI_INPUT --> M_src_mechanist_WorldTopologyTransitGraph_java["src/mechanist/WorldTopologyTransitGraph.java<br/>21 funcs / 256 lines"]
    Z_UI_INPUT --> M_src_mechanist_GameplayConsoleCommandAuthority_java["src/mechanist/GameplayConsoleCommandAuthority.java<br/>36 funcs / 206 lines"]
    Z_UI_INPUT --> M_src_mechanist_WorldCommandRequest_java["src/mechanist/WorldCommandRequest.java<br/>47 funcs / 171 lines"]
    Z_UI_INPUT --> M_src_mechanist_WorldEconomyInitializationAuthority_java["src/mechanist/WorldEconomyInitializationAuthority.java<br/>10 funcs / 158 lines"]
    Z_UI_INPUT --> M_src_mechanist_CrashDeobfuscatorEngine_java["src/mechanist/CrashDeobfuscatorEngine.java<br/>14 funcs / 155 lines"]
    Z_UI_INPUT --> M_src_mechanist_PlayerBehaviorMonitor_java["src/mechanist/PlayerBehaviorMonitor.java<br/>13 funcs / 149 lines"]
    Z_UI_INPUT --> M_src_mechanist_EconomyRuntimeState_java["src/mechanist/EconomyRuntimeState.java<br/>11 funcs / 140 lines"]
    Z_UI_INPUT --> Z_UI_INPUT_OTHER["+ 41 smaller modules"]
    ROOT --> Z_RUNTIME_OPTIONS["Runtime Options<br/>34 modules"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_GameOptionsFramework_java["src/mechanist/GameOptionsFramework.java<br/>85 funcs / 788 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_OptionsScreenPainter_java["src/mechanist/OptionsScreenPainter.java<br/>40 funcs / 572 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_WorldTopologyContract_java["src/mechanist/WorldTopologyContract.java<br/>25 funcs / 286 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_NatTraversalManager_java["src/mechanist/NatTraversalManager.java<br/>15 funcs / 208 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_WorldTopologySettingsBridge_java["src/mechanist/WorldTopologySettingsBridge.java<br/>16 funcs / 162 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_SecureModClassLoader_java["src/mechanist/SecureModClassLoader.java<br/>6 funcs / 142 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_RuntimeProfile_java["src/mechanist/RuntimeProfile.java<br/>5 funcs / 115 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_JvmRuntimeOptionsSubsystem_java["src/mechanist/JvmRuntimeOptionsSubsystem.java<br/>11 funcs / 88 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_DisplayScaleOptionsSubsystem_java["src/mechanist/DisplayScaleOptionsSubsystem.java<br/>9 funcs / 80 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_PerlinNoiseAuthority_java["src/mechanist/PerlinNoiseAuthority.java<br/>9 funcs / 79 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_GraphicsDropdownOptionsRuntime_java["src/mechanist/GraphicsDropdownOptionsRuntime.java<br/>17 funcs / 78 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_AccessibilityRuntimeOptionsSubsystem_java["src/mechanist/AccessibilityRuntimeOptionsSubsystem.java<br/>10 funcs / 76 lines"]
    Z_RUNTIME_OPTIONS --> Z_RUNTIME_OPTIONS_OTHER["+ 22 smaller modules"]
    ROOT --> Z_WORLD_GEN["World Generation Transition<br/>41 modules"]
    Z_WORLD_GEN --> M_src_mechanist_WorldRuntimeGenerationFramework_java["src/mechanist/WorldRuntimeGenerationFramework.java<br/>372 funcs / 8279 lines"]
    Z_WORLD_GEN --> M_src_mechanist_ZoneGenerationContext_java["src/mechanist/ZoneGenerationContext.java<br/>18 funcs / 396 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoadGridIntegrationAuthority_java["src/mechanist/RoadGridIntegrationAuthority.java<br/>26 funcs / 359 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoomProfile_java["src/mechanist/RoomProfile.java<br/>31 funcs / 335 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoomFixtureInteractionAuthority_java["src/mechanist/RoomFixtureInteractionAuthority.java<br/>21 funcs / 314 lines"]
    Z_WORLD_GEN --> M_src_mechanist_FactionRoomLayoutAuthority_java["src/mechanist/FactionRoomLayoutAuthority.java<br/>22 funcs / 291 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoadAdjacencyIntegrationAuthority_java["src/mechanist/RoadAdjacencyIntegrationAuthority.java<br/>19 funcs / 250 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoadFrontageFixtureAuthority_java["src/mechanist/RoadFrontageFixtureAuthority.java<br/>15 funcs / 231 lines"]
    Z_WORLD_GEN --> M_src_mechanist_WorldTopologyPreplacementPlan_java["src/mechanist/WorldTopologyPreplacementPlan.java<br/>16 funcs / 192 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoomManifestApi_java["src/mechanist/RoomManifestApi.java<br/>8 funcs / 189 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoadTransitFixtureAuthority_java["src/mechanist/RoadTransitFixtureAuthority.java<br/>18 funcs / 184 lines"]
    Z_WORLD_GEN --> M_src_mechanist_ZoneTileGrid_java["src/mechanist/ZoneTileGrid.java<br/>25 funcs / 180 lines"]
    Z_WORLD_GEN --> Z_WORLD_GEN_OTHER["+ 29 smaller modules"]
    ROOT --> Z_INVENTORY_PERSIST["Inventory Items Persistence<br/>16 modules"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_ItemEconomyFramework_java["src/mechanist/ItemEconomyFramework.java<br/>71 funcs / 1327 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_ContainerTradeFramework_java["src/mechanist/ContainerTradeFramework.java<br/>92 funcs / 802 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_FallbackProfileManagementAuthority_java["src/mechanist/FallbackProfileManagementAuthority.java<br/>49 funcs / 684 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_GameStorageManager_java["src/mechanist/GameStorageManager.java<br/>34 funcs / 320 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_InventoryTransactionGuard_java["src/mechanist/InventoryTransactionGuard.java<br/>21 funcs / 190 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_FactionInventoryStockAuthority_java["src/mechanist/FactionInventoryStockAuthority.java<br/>13 funcs / 179 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_ItemSemanticAssetAuthority_java["src/mechanist/ItemSemanticAssetAuthority.java<br/>7 funcs / 146 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_ZoneFactionStockTracker_java["src/mechanist/ZoneFactionStockTracker.java<br/>14 funcs / 115 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_FactionWideStockTracker_java["src/mechanist/FactionWideStockTracker.java<br/>13 funcs / 113 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_TraderTradeActionAuthority_java["src/mechanist/TraderTradeActionAuthority.java<br/>7 funcs / 84 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_CharacterSaveManager_java["src/mechanist/CharacterSaveManager.java<br/>6 funcs / 79 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_FactionInventoryRoutingAuthority_java["src/mechanist/FactionInventoryRoutingAuthority.java<br/>8 funcs / 51 lines"]
    Z_INVENTORY_PERSIST --> Z_INVENTORY_PERSIST_OTHER["+ 4 smaller modules"]
    ROOT --> Z_FIXTURE_MACHINE["Fixtures Machines<br/>18 modules"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_ProductionAuthorityFramework_java["src/mechanist/ProductionAuthorityFramework.java<br/>314 funcs / 3105 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_MachineOperationQueue_java["src/mechanist/MachineOperationQueue.java<br/>32 funcs / 339 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_SecureHandshakeStateMachine_java["src/mechanist/SecureHandshakeStateMachine.java<br/>62 funcs / 292 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_DomesticHabFixtureAuthority_java["src/mechanist/DomesticHabFixtureAuthority.java<br/>17 funcs / 227 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_GuardPdfDefenseFixtureAuthority_java["src/mechanist/GuardPdfDefenseFixtureAuthority.java<br/>18 funcs / 211 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_BarMarketSocialFixtureAuthority_java["src/mechanist/BarMarketSocialFixtureAuthority.java<br/>21 funcs / 202 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_ArbitesPrecinctFixtureAuthority_java["src/mechanist/ArbitesPrecinctFixtureAuthority.java<br/>18 funcs / 202 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_FoodBioProductionFixtureAuthority_java["src/mechanist/FoodBioProductionFixtureAuthority.java<br/>17 funcs / 193 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_FixtureInteractionRegistry_java["src/mechanist/FixtureInteractionRegistry.java<br/>7 funcs / 188 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_LabChemicalFixtureAuthority_java["src/mechanist/LabChemicalFixtureAuthority.java<br/>17 funcs / 184 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_NobleEstateSecurityFixtureAuthority_java["src/mechanist/NobleEstateSecurityFixtureAuthority.java<br/>16 funcs / 179 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_FrontageFixtureInteractionAuthority_java["src/mechanist/FrontageFixtureInteractionAuthority.java<br/>13 funcs / 179 lines"]
    Z_FIXTURE_MACHINE --> Z_FIXTURE_MACHINE_OTHER["+ 6 smaller modules"]
    ROOT --> Z_COMBAT_SIM["Combat Entity Simulation<br/>24 modules"]
    Z_COMBAT_SIM --> M_src_mechanist_WorldSimulationFramework_java["src/mechanist/WorldSimulationFramework.java<br/>204 funcs / 1802 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_PopulationPersonnelFramework_java["src/mechanist/PopulationPersonnelFramework.java<br/>122 funcs / 1264 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_FactionServicesFramework_java["src/mechanist/FactionServicesFramework.java<br/>106 funcs / 943 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_ContinuousGridMovementFramework_java["src/mechanist/ContinuousGridMovementFramework.java<br/>38 funcs / 387 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_assets_SemanticAssetPathAudit_java["src/mechanist/assets/SemanticAssetPathAudit.java<br/>21 funcs / 302 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_NpcNeedsDutyRuntimeAuthority_java["src/mechanist/NpcNeedsDutyRuntimeAuthority.java<br/>13 funcs / 213 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_MovementPlanningAuthority_java["src/mechanist/MovementPlanningAuthority.java<br/>13 funcs / 212 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_CombatRuntimeFramework_java["src/mechanist/CombatRuntimeFramework.java<br/>18 funcs / 196 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_PlayerNpcCommandParityAuthority_java["src/mechanist/PlayerNpcCommandParityAuthority.java<br/>16 funcs / 174 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_EntityInterpolationBuffer_java["src/mechanist/EntityInterpolationBuffer.java<br/>22 funcs / 131 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_NpcTurnBudgetScheduler_java["src/mechanist/NpcTurnBudgetScheduler.java<br/>5 funcs / 123 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_RuntimePathResolver_java["src/mechanist/RuntimePathResolver.java<br/>12 funcs / 122 lines"]
    Z_COMBAT_SIM --> Z_COMBAT_SIM_OTHER["+ 12 smaller modules"]
    ROOT --> Z_ASSET_REGISTRY["Asset Registry Art<br/>25 modules"]
    Z_ASSET_REGISTRY --> M_src_mechanist_InfrastructurePromotionRegistry_java["src/mechanist/InfrastructurePromotionRegistry.java<br/>11 funcs / 1001 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_TileDataCompilationAuthority_java["src/mechanist/TileDataCompilationAuthority.java<br/>49 funcs / 626 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_AssetIntegrationDisciplineAuthority_java["src/mechanist/AssetIntegrationDisciplineAuthority.java<br/>8 funcs / 518 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_assets_AssetRegistry_java["src/mechanist/assets/AssetRegistry.java<br/>33 funcs / 478 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_GlyphBinder_java["src/mechanist/GlyphBinder.java<br/>14 funcs / 430 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_assets_AssetManager_java["src/mechanist/assets/AssetManager.java<br/>31 funcs / 320 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_SemanticAssetInfopediaAuthority_java["src/mechanist/SemanticAssetInfopediaAuthority.java<br/>22 funcs / 307 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_assets_GeneratedAssetRuntime_java["src/mechanist/assets/GeneratedAssetRuntime.java<br/>21 funcs / 276 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_TileSemanticAssetAuthority_java["src/mechanist/TileSemanticAssetAuthority.java<br/>17 funcs / 260 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_ObjectSemanticAssetAuthority_java["src/mechanist/ObjectSemanticAssetAuthority.java<br/>18 funcs / 258 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_TileInfopediaAuthority_java["src/mechanist/TileInfopediaAuthority.java<br/>11 funcs / 252 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_PortraitSemanticAssetAuthority_java["src/mechanist/PortraitSemanticAssetAuthority.java<br/>17 funcs / 231 lines"]
    Z_ASSET_REGISTRY --> Z_ASSET_REGISTRY_OTHER["+ 13 smaller modules"]
    ROOT --> Z_SERVER_AUTH["Server Authority Launcher<br/>39 modules"]
    Z_SERVER_AUTH --> M_src_mechanist_server_admin_ServerAdminConsole_java["src/mechanist/server/admin/ServerAdminConsole.java<br/>14 funcs / 270 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_NativeTcpRelayServer_java["src/mechanist/NativeTcpRelayServer.java<br/>14 funcs / 253 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_MechanistServerMain_java["src/mechanist/MechanistServerMain.java<br/>17 funcs / 207 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_ClientLauncherContext_java["src/mechanist/ClientLauncherContext.java<br/>17 funcs / 203 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_ServerDisasterRecoveryEngine_java["src/mechanist/ServerDisasterRecoveryEngine.java<br/>11 funcs / 178 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_EngineHotLauncher_java["src/mechanist/EngineHotLauncher.java<br/>8 funcs / 167 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_NetworkThrottlingManager_java["src/mechanist/NetworkThrottlingManager.java<br/>22 funcs / 166 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_server_admin_ServerMaintenanceService_java["src/mechanist/server/admin/ServerMaintenanceService.java<br/>14 funcs / 150 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_LauncherShellStateAuthority_java["src/mechanist/LauncherShellStateAuthority.java<br/>12 funcs / 144 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_SecureChatPacket_java["src/mechanist/SecureChatPacket.java<br/>11 funcs / 132 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_launcher_LauncherFallbackProfileAuthority_java["src/mechanist/launcher/LauncherFallbackProfileAuthority.java<br/>5 funcs / 131 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_launcher_LauncherWrapperDetector_java["src/mechanist/launcher/LauncherWrapperDetector.java<br/>9 funcs / 129 lines"]
    Z_SERVER_AUTH --> Z_SERVER_AUTH_OTHER["+ 27 smaller modules"]
    ROOT --> Z_DIAGNOSTIC_DOC["Diagnostics Smoke Audit<br/>25 modules"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_RetargetReadinessAuditAuthority_java["src/mechanist/RetargetReadinessAuditAuthority.java<br/>17 funcs / 212 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_SectorAuditRuntimeAuthority_java["src/mechanist/SectorAuditRuntimeAuthority.java<br/>11 funcs / 195 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_PerformanceDiagnosticsOverlayAuthority_java["src/mechanist/PerformanceDiagnosticsOverlayAuthority.java<br/>10 funcs / 125 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_DebugLog_java["src/mechanist/DebugLog.java<br/>19 funcs / 115 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_SimulationToolSuiteRecoverySmoke_java["src/mechanist/SimulationToolSuiteRecoverySmoke.java<br/>4 funcs / 94 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone02LookExamineReadabilitySmoke_java["src/mechanist/Milestone02LookExamineReadabilitySmoke.java<br/>5 funcs / 73 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone02InfopediaMechanicsReadabilitySmoke_java["src/mechanist/Milestone02InfopediaMechanicsReadabilitySmoke.java<br/>6 funcs / 62 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone02MenuUniformityReadabilitySmoke_java["src/mechanist/Milestone02MenuUniformityReadabilitySmoke.java<br/>6 funcs / 62 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone02ContextPromptReadabilitySmoke_java["src/mechanist/Milestone02ContextPromptReadabilitySmoke.java<br/>5 funcs / 61 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone02CurrentBindingPromptSmoke_java["src/mechanist/Milestone02CurrentBindingPromptSmoke.java<br/>4 funcs / 50 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_PlayerFacingUiTextSmoke_java["src/mechanist/PlayerFacingUiTextSmoke.java<br/>1 funcs / 47 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_PlayerFacingDenialTextSmoke_java["src/mechanist/PlayerFacingDenialTextSmoke.java<br/>1 funcs / 38 lines"]
    Z_DIAGNOSTIC_DOC --> Z_DIAGNOSTIC_DOC_OTHER["+ 13 smaller modules"]
```

## Generated Ledgers

- `ROOT_docs/functionmap/generated/CODE_MERMAID_POSITION_LEDGER.tsv`
- `ROOT_docs/functionmap/generated/CODE_MERMAID_EVALUATION.tsv`
- `ROOT_docs/functionmap/generated/MERMAID_CODE_MAP.md`

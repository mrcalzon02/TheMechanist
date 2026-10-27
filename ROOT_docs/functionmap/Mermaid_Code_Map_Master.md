# Mermaid Code Map Master Record

Status: active master code-position map.

Generated/evaluated: `2026-10-27 06:06:02`

## Top-Line Rule

Every code module, generated code error, compile error cluster, or subsystem remap must submit a Mermaid position before it is considered mapped. Unpositioned modules are architecture debt, not invisible implementation detail.

## Counts

- Java modules mapped: `955`
- Unpositioned modules: `0`
- Oversized mapped modules: `23`

## Zone Counts

- `ASSET_REGISTRY`: `50` modules
- `COMBAT_SIM`: `72` modules
- `DIAGNOSTIC_DOC`: `241` modules
- `FIXTURE_MACHINE`: `32` modules
- `INVENTORY_PERSIST`: `34` modules
- `LOCALIZATION_TEXT`: `119` modules
- `RUNTIME_OPTIONS`: `37` modules
- `SERVER_AUTH`: `49` modules
- `UI_INPUT`: `66` modules
- `UI_RENDER`: `152` modules
- `WORLD_GEN`: `103` modules

## Status Counts

- `positioned`: `955` modules

## Explicit Override Rule

Explicit `MODULE_OVERRIDES` entries in `scripts/BUILD_MERMAID_CODE_MAP.py` beat broad keyword heuristics. Add an override when a known owner is misclassified by generic words such as render, panel, audit, semantic, faction, or zone.

## Master Mermaid Map

```mermaid
flowchart TD
    ROOT["The Mechanist Codebase<br/>Mermaid position master"]
    ERRORS["Code errors / unmapped modules<br/>must submit map position"]
    ROOT --> ERRORS
    ROOT --> Z_LOCALIZATION_TEXT["Localization Text<br/>119 modules"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_MediaRuntimeFramework_java["src/mechanist/MediaRuntimeFramework.java<br/>58 funcs / 882 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_ModDeploymentManager_java["src/mechanist/ModDeploymentManager.java<br/>42 funcs / 551 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_FactionReinforcementAuthority_java["src/mechanist/FactionReinforcementAuthority.java<br/>30 funcs / 499 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_SaveEfficiencyAuthority_java["src/mechanist/SaveEfficiencyAuthority.java<br/>38 funcs / 482 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_FactionCriticalVendorPlacementAuthority_java["src/mechanist/FactionCriticalVendorPlacementAuthority.java<br/>32 funcs / 480 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_FactionMarketContractAuthority_java["src/mechanist/FactionMarketContractAuthority.java<br/>26 funcs / 478 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_PersistenceFramework_java["src/mechanist/PersistenceFramework.java<br/>19 funcs / 463 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_AnimalAgricultureSupplyProvenanceAuthority_java["src/mechanist/AnimalAgricultureSupplyProvenanceAuthority.java<br/>34 funcs / 462 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_DeveloperFacilityAuthority_java["src/mechanist/DeveloperFacilityAuthority.java<br/>29 funcs / 457 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_HousingVariantRules_java["src/mechanist/HousingVariantRules.java<br/>21 funcs / 448 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_SecuritySupplyProvenanceAuthority_java["src/mechanist/SecuritySupplyProvenanceAuthority.java<br/>32 funcs / 436 lines"]
    Z_LOCALIZATION_TEXT --> M_src_mechanist_SimulationRuntimeEditorBridgeAuthority_java["src/mechanist/SimulationRuntimeEditorBridgeAuthority.java<br/>14 funcs / 436 lines"]
    Z_LOCALIZATION_TEXT --> Z_LOCALIZATION_TEXT_OTHER["+ 107 smaller modules"]
    ROOT --> Z_UI_RENDER["UI Render Surfaces<br/>152 modules"]
    Z_UI_RENDER --> M_src_mechanist_LegacyPanelContext_java["src/mechanist/LegacyPanelContext.java<br/>609 funcs / 8073 lines"]
    Z_UI_RENDER --> M_src_mechanist_SimulationEditorSuite_java["src/mechanist/SimulationEditorSuite.java<br/>117 funcs / 1699 lines"]
    Z_UI_RENDER --> M_src_mechanist_WorldStartFlowAuthority_java["src/mechanist/WorldStartFlowAuthority.java<br/>118 funcs / 1269 lines"]
    Z_UI_RENDER --> M_src_mechanist_FirstPerson3DFramework_java["src/mechanist/FirstPerson3DFramework.java<br/>111 funcs / 1257 lines"]
    Z_UI_RENDER --> M_src_mechanist_ProgressiveConstructionAuthority_java["src/mechanist/ProgressiveConstructionAuthority.java<br/>66 funcs / 784 lines"]
    Z_UI_RENDER --> M_src_mechanist_IndependentHostLobbyWindow_java["src/mechanist/IndependentHostLobbyWindow.java<br/>25 funcs / 757 lines"]
    Z_UI_RENDER --> M_src_mechanist_VisualJuiceFramework_java["src/mechanist/VisualJuiceFramework.java<br/>57 funcs / 635 lines"]
    Z_UI_RENDER --> M_src_mechanist_AudioRuntimeFramework_java["src/mechanist/AudioRuntimeFramework.java<br/>53 funcs / 605 lines"]
    Z_UI_RENDER --> M_src_mechanist_AuthoritativeWorldRuntime_java["src/mechanist/AuthoritativeWorldRuntime.java<br/>23 funcs / 583 lines"]
    Z_UI_RENDER --> M_src_mechanist_SkillTreeProgressionAuthority_java["src/mechanist/SkillTreeProgressionAuthority.java<br/>42 funcs / 573 lines"]
    Z_UI_RENDER --> M_src_mechanist_VehicleOperationFeedbackAuthority_java["src/mechanist/VehicleOperationFeedbackAuthority.java<br/>19 funcs / 546 lines"]
    Z_UI_RENDER --> M_src_mechanist_FactionPhysicalConstructionAuthority_java["src/mechanist/FactionPhysicalConstructionAuthority.java<br/>30 funcs / 513 lines"]
    Z_UI_RENDER --> Z_UI_RENDER_OTHER["+ 140 smaller modules"]
    ROOT --> Z_UI_INPUT["UI Input Navigation<br/>66 modules"]
    Z_UI_INPUT --> M_src_mechanist_SectorManager_java["src/mechanist/SectorManager.java<br/>60 funcs / 615 lines"]
    Z_UI_INPUT --> M_src_mechanist_input_KeyBindingManager_java["src/mechanist/input/KeyBindingManager.java<br/>46 funcs / 518 lines"]
    Z_UI_INPUT --> M_src_mechanist_FactionCrecheAuthority_java["src/mechanist/FactionCrecheAuthority.java<br/>22 funcs / 415 lines"]
    Z_UI_INPUT --> M_src_mechanist_CharacterStateRecord_java["src/mechanist/CharacterStateRecord.java<br/>22 funcs / 382 lines"]
    Z_UI_INPUT --> M_src_mechanist_GameplayConsoleCommandAuthority_java["src/mechanist/GameplayConsoleCommandAuthority.java<br/>54 funcs / 355 lines"]
    Z_UI_INPUT --> M_src_mechanist_AdminCommandDispatcher_java["src/mechanist/AdminCommandDispatcher.java<br/>35 funcs / 353 lines"]
    Z_UI_INPUT --> M_src_mechanist_HybridEncryptionManager_java["src/mechanist/HybridEncryptionManager.java<br/>16 funcs / 322 lines"]
    Z_UI_INPUT --> M_src_mechanist_SnapshotDeltaCompressor_java["src/mechanist/SnapshotDeltaCompressor.java<br/>39 funcs / 294 lines"]
    Z_UI_INPUT --> M_src_mechanist_WorldTopologyContract_java["src/mechanist/WorldTopologyContract.java<br/>25 funcs / 282 lines"]
    Z_UI_INPUT --> M_src_mechanist_Milestone05ConstructionGameplayCommandSmoke_java["src/mechanist/Milestone05ConstructionGameplayCommandSmoke.java<br/>4 funcs / 276 lines"]
    Z_UI_INPUT --> M_src_mechanist_WorldTopologyTransitGraph_java["src/mechanist/WorldTopologyTransitGraph.java<br/>21 funcs / 256 lines"]
    Z_UI_INPUT --> M_src_mechanist_ContractObjectiveReadabilityAuthority_java["src/mechanist/ContractObjectiveReadabilityAuthority.java<br/>12 funcs / 213 lines"]
    Z_UI_INPUT --> Z_UI_INPUT_OTHER["+ 54 smaller modules"]
    ROOT --> Z_RUNTIME_OPTIONS["Runtime Options<br/>37 modules"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_GameOptionsFramework_java["src/mechanist/GameOptionsFramework.java<br/>85 funcs / 788 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_OptionsScreenPainter_java["src/mechanist/OptionsScreenPainter.java<br/>43 funcs / 636 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_NatTraversalManager_java["src/mechanist/NatTraversalManager.java<br/>15 funcs / 208 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_WorldTopologySettingsBridge_java["src/mechanist/WorldTopologySettingsBridge.java<br/>16 funcs / 163 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_SecureModClassLoader_java["src/mechanist/SecureModClassLoader.java<br/>6 funcs / 142 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_MultiplayerHostBindingService_java["src/mechanist/MultiplayerHostBindingService.java<br/>5 funcs / 119 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_GraphicsDropdownOptionsRuntime_java["src/mechanist/GraphicsDropdownOptionsRuntime.java<br/>20 funcs / 110 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_JvmRuntimeOptionsSubsystem_java["src/mechanist/JvmRuntimeOptionsSubsystem.java<br/>11 funcs / 88 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_DisplayScaleOptionsSubsystem_java["src/mechanist/DisplayScaleOptionsSubsystem.java<br/>9 funcs / 80 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_PerlinNoiseAuthority_java["src/mechanist/PerlinNoiseAuthority.java<br/>9 funcs / 79 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_BuildIdentityAuthority_java["src/mechanist/BuildIdentityAuthority.java<br/>10 funcs / 78 lines"]
    Z_RUNTIME_OPTIONS --> M_src_mechanist_AccessibilityRuntimeOptionsSubsystem_java["src/mechanist/AccessibilityRuntimeOptionsSubsystem.java<br/>10 funcs / 76 lines"]
    Z_RUNTIME_OPTIONS --> Z_RUNTIME_OPTIONS_OTHER["+ 25 smaller modules"]
    ROOT --> Z_WORLD_GEN["World Generation Transition<br/>103 modules"]
    Z_WORLD_GEN --> M_src_mechanist_WorldRuntimeGenerationFramework_java["src/mechanist/WorldRuntimeGenerationFramework.java<br/>378 funcs / 8352 lines"]
    Z_WORLD_GEN --> M_src_mechanist_FactionRoomShellConstructionAuthority_java["src/mechanist/FactionRoomShellConstructionAuthority.java<br/>52 funcs / 1256 lines"]
    Z_WORLD_GEN --> M_src_mechanist_ExplicitRoomTypeRequirementAuthority_java["src/mechanist/ExplicitRoomTypeRequirementAuthority.java<br/>61 funcs / 1106 lines"]
    Z_WORLD_GEN --> M_src_mechanist_Milestone05FactionRoomShellConstructionSmoke_java["src/mechanist/Milestone05FactionRoomShellConstructionSmoke.java<br/>30 funcs / 806 lines"]
    Z_WORLD_GEN --> M_src_mechanist_VehicleStrategicTransitAuthority_java["src/mechanist/VehicleStrategicTransitAuthority.java<br/>8 funcs / 529 lines"]
    Z_WORLD_GEN --> M_src_mechanist_Milestone05RoomConstructionParityEditorSmoke_java["src/mechanist/Milestone05RoomConstructionParityEditorSmoke.java<br/>12 funcs / 502 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoomOwnershipAuthority_java["src/mechanist/RoomOwnershipAuthority.java<br/>39 funcs / 467 lines"]
    Z_WORLD_GEN --> M_src_mechanist_ZoneGenerationContext_java["src/mechanist/ZoneGenerationContext.java<br/>18 funcs / 396 lines"]
    Z_WORLD_GEN --> M_src_mechanist_VehicleStrategicTransitCommitAuthority_java["src/mechanist/VehicleStrategicTransitCommitAuthority.java<br/>10 funcs / 364 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoadGridIntegrationAuthority_java["src/mechanist/RoadGridIntegrationAuthority.java<br/>26 funcs / 359 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoomProfile_java["src/mechanist/RoomProfile.java<br/>31 funcs / 340 lines"]
    Z_WORLD_GEN --> M_src_mechanist_RoomFixtureInteractionAuthority_java["src/mechanist/RoomFixtureInteractionAuthority.java<br/>21 funcs / 314 lines"]
    Z_WORLD_GEN --> Z_WORLD_GEN_OTHER["+ 91 smaller modules"]
    ROOT --> Z_INVENTORY_PERSIST["Inventory Items Persistence<br/>34 modules"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_ItemEconomyFramework_java["src/mechanist/ItemEconomyFramework.java<br/>73 funcs / 1535 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_ContainerTradeFramework_java["src/mechanist/ContainerTradeFramework.java<br/>93 funcs / 828 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_FallbackProfileManagementAuthority_java["src/mechanist/FallbackProfileManagementAuthority.java<br/>49 funcs / 684 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_VehicleMotorPoolAuthority_java["src/mechanist/VehicleMotorPoolAuthority.java<br/>10 funcs / 482 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_VehicleManifestAuthority_java["src/mechanist/VehicleManifestAuthority.java<br/>14 funcs / 461 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_VehicleMaintenanceAuthority_java["src/mechanist/VehicleMaintenanceAuthority.java<br/>14 funcs / 398 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_GameStorageManager_java["src/mechanist/GameStorageManager.java<br/>34 funcs / 336 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_IndependentHostResumeTokenStore_java["src/mechanist/IndependentHostResumeTokenStore.java<br/>11 funcs / 308 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_VehicleAccessAuthority_java["src/mechanist/VehicleAccessAuthority.java<br/>7 funcs / 296 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_VehicleFuelAuthority_java["src/mechanist/VehicleFuelAuthority.java<br/>12 funcs / 276 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_ItemSemanticAssetAuthority_java["src/mechanist/ItemSemanticAssetAuthority.java<br/>10 funcs / 257 lines"]
    Z_INVENTORY_PERSIST --> M_src_mechanist_VerticalFloorTradeAuthority_java["src/mechanist/VerticalFloorTradeAuthority.java<br/>25 funcs / 246 lines"]
    Z_INVENTORY_PERSIST --> Z_INVENTORY_PERSIST_OTHER["+ 22 smaller modules"]
    ROOT --> Z_FIXTURE_MACHINE["Fixtures Machines<br/>32 modules"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_ProductionAuthorityFramework_java["src/mechanist/ProductionAuthorityFramework.java<br/>319 funcs / 3141 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_MachineRepairAuthority_java["src/mechanist/MachineRepairAuthority.java<br/>20 funcs / 357 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_MachineOperationQueue_java["src/mechanist/MachineOperationQueue.java<br/>34 funcs / 347 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_SecureHandshakeStateMachine_java["src/mechanist/SecureHandshakeStateMachine.java<br/>62 funcs / 292 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_MachineOperationStatusBridge_java["src/mechanist/MachineOperationStatusBridge.java<br/>19 funcs / 242 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_DomesticHabFixtureAuthority_java["src/mechanist/DomesticHabFixtureAuthority.java<br/>17 funcs / 227 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_GuardPdfDefenseFixtureAuthority_java["src/mechanist/GuardPdfDefenseFixtureAuthority.java<br/>18 funcs / 211 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_CombatFixtureExecutionSmoke_java["src/mechanist/CombatFixtureExecutionSmoke.java<br/>9 funcs / 204 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_BarMarketSocialFixtureAuthority_java["src/mechanist/BarMarketSocialFixtureAuthority.java<br/>21 funcs / 202 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_ArbitesPrecinctFixtureAuthority_java["src/mechanist/ArbitesPrecinctFixtureAuthority.java<br/>18 funcs / 202 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_FoodBioProductionFixtureAuthority_java["src/mechanist/FoodBioProductionFixtureAuthority.java<br/>17 funcs / 193 lines"]
    Z_FIXTURE_MACHINE --> M_src_mechanist_FixtureInteractionRegistry_java["src/mechanist/FixtureInteractionRegistry.java<br/>7 funcs / 193 lines"]
    Z_FIXTURE_MACHINE --> Z_FIXTURE_MACHINE_OTHER["+ 20 smaller modules"]
    ROOT --> Z_COMBAT_SIM["Combat Entity Simulation<br/>72 modules"]
    Z_COMBAT_SIM --> M_src_mechanist_WorldSimulationFramework_java["src/mechanist/WorldSimulationFramework.java<br/>207 funcs / 1810 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_PopulationPersonnelFramework_java["src/mechanist/PopulationPersonnelFramework.java<br/>125 funcs / 1429 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_IndependentHostTurnAuthority_java["src/mechanist/IndependentHostTurnAuthority.java<br/>48 funcs / 1138 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_FactionServicesFramework_java["src/mechanist/FactionServicesFramework.java<br/>124 funcs / 1105 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_FactionVehicleRouteControlAuthority_java["src/mechanist/FactionVehicleRouteControlAuthority.java<br/>16 funcs / 849 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_VehicleRuntimeAuthority_java["src/mechanist/VehicleRuntimeAuthority.java<br/>44 funcs / 821 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_FactionVehicleRouteStrategicAuthority_java["src/mechanist/FactionVehicleRouteStrategicAuthority.java<br/>24 funcs / 695 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_StructuralScaleCombatAuthority_java["src/mechanist/StructuralScaleCombatAuthority.java<br/>34 funcs / 678 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_FactionVehicleDoctrineAuthority_java["src/mechanist/FactionVehicleDoctrineAuthority.java<br/>10 funcs / 596 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_VehicleLossAuthority_java["src/mechanist/VehicleLossAuthority.java<br/>16 funcs / 537 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_VehicleTransitAuthority_java["src/mechanist/VehicleTransitAuthority.java<br/>15 funcs / 433 lines"]
    Z_COMBAT_SIM --> M_src_mechanist_MovementPlanningAuthority_java["src/mechanist/MovementPlanningAuthority.java<br/>30 funcs / 425 lines"]
    Z_COMBAT_SIM --> Z_COMBAT_SIM_OTHER["+ 60 smaller modules"]
    ROOT --> Z_ASSET_REGISTRY["Asset Registry Art<br/>50 modules"]
    Z_ASSET_REGISTRY --> M_src_mechanist_InfrastructurePromotionRegistry_java["src/mechanist/InfrastructurePromotionRegistry.java<br/>11 funcs / 1001 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_SemanticAssetInfopediaAuthority_java["src/mechanist/SemanticAssetInfopediaAuthority.java<br/>38 funcs / 744 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_TileDataCompilationAuthority_java["src/mechanist/TileDataCompilationAuthority.java<br/>58 funcs / 713 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_assets_AssetRegistry_java["src/mechanist/assets/AssetRegistry.java<br/>36 funcs / 605 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_ObjectSemanticAssetAuthority_java["src/mechanist/ObjectSemanticAssetAuthority.java<br/>37 funcs / 561 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_SemanticRenderAssetResolver_java["src/mechanist/SemanticRenderAssetResolver.java<br/>39 funcs / 537 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_AssetIntegrationDisciplineAuthority_java["src/mechanist/AssetIntegrationDisciplineAuthority.java<br/>8 funcs / 533 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_TileSemanticAssetAuthority_java["src/mechanist/TileSemanticAssetAuthority.java<br/>38 funcs / 525 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_PortraitSemanticAssetAuthority_java["src/mechanist/PortraitSemanticAssetAuthority.java<br/>37 funcs / 522 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_FactionStrategicAssetAuthority_java["src/mechanist/FactionStrategicAssetAuthority.java<br/>21 funcs / 488 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_Milestone05FactionStrategicAssetSmoke_java["src/mechanist/Milestone05FactionStrategicAssetSmoke.java<br/>14 funcs / 447 lines"]
    Z_ASSET_REGISTRY --> M_src_mechanist_GlyphBinder_java["src/mechanist/GlyphBinder.java<br/>14 funcs / 430 lines"]
    Z_ASSET_REGISTRY --> Z_ASSET_REGISTRY_OTHER["+ 38 smaller modules"]
    ROOT --> Z_SERVER_AUTH["Server Authority Launcher<br/>49 modules"]
    Z_SERVER_AUTH --> M_src_mechanist_IndependentHostClientSupervisor_java["src/mechanist/IndependentHostClientSupervisor.java<br/>41 funcs / 1068 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_RemoteSessionLedgerAuthority_java["src/mechanist/RemoteSessionLedgerAuthority.java<br/>33 funcs / 901 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_IndependentHostWireProtocol_java["src/mechanist/IndependentHostWireProtocol.java<br/>41 funcs / 658 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_NativeTcpRelayServer_java["src/mechanist/NativeTcpRelayServer.java<br/>32 funcs / 604 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_MechanistServerMain_java["src/mechanist/MechanistServerMain.java<br/>14 funcs / 380 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_HostedRosterClientAuthority_java["src/mechanist/HostedRosterClientAuthority.java<br/>19 funcs / 359 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_launcher_LauncherProfileSelectionDialog_java["src/mechanist/launcher/LauncherProfileSelectionDialog.java<br/>17 funcs / 311 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_server_admin_ServerAdminConsole_java["src/mechanist/server/admin/ServerAdminConsole.java<br/>14 funcs / 270 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_launcher_LauncherProfileSelectionDialogSmoke_java["src/mechanist/launcher/LauncherProfileSelectionDialogSmoke.java<br/>4 funcs / 225 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_launcher_LauncherFallbackProfileAuthority_java["src/mechanist/launcher/LauncherFallbackProfileAuthority.java<br/>10 funcs / 212 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_ClientLauncherContext_java["src/mechanist/ClientLauncherContext.java<br/>17 funcs / 203 lines"]
    Z_SERVER_AUTH --> M_src_mechanist_ServerDisasterRecoveryEngine_java["src/mechanist/ServerDisasterRecoveryEngine.java<br/>11 funcs / 178 lines"]
    Z_SERVER_AUTH --> Z_SERVER_AUTH_OTHER["+ 37 smaller modules"]
    ROOT --> Z_DIAGNOSTIC_DOC["Diagnostics Smoke Audit<br/>241 modules"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone06FactionVehicleBalanceSmoke_java["src/mechanist/Milestone06FactionVehicleBalanceSmoke.java<br/>8 funcs / 868 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_IndependentHostTransportSessionSmoke_java["src/mechanist/IndependentHostTransportSessionSmoke.java<br/>19 funcs / 769 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone05FactionPhysicalConstructionSmoke_java["src/mechanist/Milestone05FactionPhysicalConstructionSmoke.java<br/>20 funcs / 725 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone09ExplicitRoomPurposeCrecheSmoke_java["src/mechanist/Milestone09ExplicitRoomPurposeCrecheSmoke.java<br/>23 funcs / 563 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_AuthoritativeWorldRuntimeOverflowSmoke_java["src/mechanist/AuthoritativeWorldRuntimeOverflowSmoke.java<br/>10 funcs / 547 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone09ExplicitRoomPurposeBarracksSmoke_java["src/mechanist/Milestone09ExplicitRoomPurposeBarracksSmoke.java<br/>23 funcs / 538 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone05FactionBlueprintSiteUpgradeSmoke_java["src/mechanist/Milestone05FactionBlueprintSiteUpgradeSmoke.java<br/>14 funcs / 512 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_IndependentHostPersistentSessionRestartSmoke_java["src/mechanist/IndependentHostPersistentSessionRestartSmoke.java<br/>17 funcs / 510 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone05ConstructionParityEditorSmoke_java["src/mechanist/Milestone05ConstructionParityEditorSmoke.java<br/>14 funcs / 484 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone06VehicleStrategicTransitCommitSmoke_java["src/mechanist/Milestone06VehicleStrategicTransitCommitSmoke.java<br/>4 funcs / 472 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_IndependentHostClientSupervisorSmoke_java["src/mechanist/IndependentHostClientSupervisorSmoke.java<br/>7 funcs / 467 lines"]
    Z_DIAGNOSTIC_DOC --> M_src_mechanist_Milestone06VehicleRuntimeFoundationSmoke_java["src/mechanist/Milestone06VehicleRuntimeFoundationSmoke.java<br/>12 funcs / 461 lines"]
    Z_DIAGNOSTIC_DOC --> Z_DIAGNOSTIC_DOC_OTHER["+ 229 smaller modules"]
```

## Generated Ledgers

- `ROOT_docs/functionmap/generated/CODE_MERMAID_POSITION_LEDGER.tsv`
- `ROOT_docs/functionmap/generated/CODE_MERMAID_EVALUATION.tsv`
- `ROOT_docs/functionmap/generated/MERMAID_CODE_MAP.md`

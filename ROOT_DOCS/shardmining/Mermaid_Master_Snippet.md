# Mermaid Master Snippet

Status: unified high-level module connection map for the current shard-mining state.

Use this together with `Architecture_Map.md`, `Shard_Mining_Method_and_Progress.md`, and `Compatibility_Ledger.md` before continuing extraction work.

```mermaid
flowchart TD
    GP["GamePanel.java<br/>Oversized shell / unmined source of truth"]
    GPK["GamePanelKeyController<br/>Key controller bridge"]
    SP["ScreenPainter<br/>paint(Graphics2D, GamePanel)"]
    UIS["UiRuntimeSupportFramework<br/>GuiLayoutApi / TextSurfaceApi / TextLayoutAuthority / ButtonBox"]

    subgraph Rendering["Rendering / Screen Surface Zone"]
        BSP["BootSurfacePainter"]
        ICP["IntroCrawlSurfacePainter<br/>conservative bridge"]
        OSP["OptionsScreenPainter"]
        RSC["RenderScalingCrtAuthority"]
        FPS["FramePacingAndStressFramework"]
    end

    subgraph Options["Runtime Options / Display / JVM Zone"]
        GOF["GameOptionsFramework<br/>GameOptions"]
        LG["LayerG<br/>options action bridge"]
        GAP["GeneratedArtPayloadOptionsSubsystem"]
        DSO["DisplayScaleOptionsSubsystem"]
        ARO["AccessibilityRuntimeOptionsSubsystem"]
        JRO["JvmRuntimeOptionsSubsystem"]
        JRP["JvmRuntimeProfileAuthority"]
    end

    subgraph Assets["Asset / Registry / Tile Art Zone"]
        TAS["TileArtSystem<br/>loader + semantic helpers"]
        TIR["TileImageRegistry<br/>runtime image owner"]
        TIA["TileInfopediaAuthority"]
        TSA["TileSemanticAssetAuthority"]
        SAI["SemanticAssetInfopediaAuthority"]
        AIDA["AssetIntegrationDisciplineAuthority"]
        GB["GlyphBinder"]
        RPR["RuntimePathResolver"]
        AM["assets.AssetManager"]
        AR["assets.AssetRegistry"]
        GAR["assets.GeneratedAssetRuntime"]
        SAM["assets.AssetMetadata"]
    end

    subgraph Input["Input / Controls Zone"]
        CRT["ControlReferenceTextSubsystem"]
        IA["InputAction"]
        IR["InputRegistry"]
        KIB["KeyboardInputBridge"]
        GIE["GamepadInputEngine"]
        GIS["GamepadControllerSnapshot"]
        INP["mechanist.input.*"]
    end

    subgraph World["World Generation / Room / Road / Fixture Zone"]
        WRG["WorldRuntimeGenerationFramework"]
        RFI["RoomFixtureInteractionAuthority"]
        RP["RoomProfile"]
        RAF["RoadAdjacencyIntegrationAuthority"]
        RFF["RoadFrontageFixtureAuthority"]
        RGF["RoadGridIntegrationAuthority"]
        RTF["RoadTransitFixtureAuthority"]
        RTI["RoadTransitFixtureInteractionAuthority"]
        FFI["FrontageFixtureInteractionAuthority"]
        APF["ArbitesPrecinctFixtureAuthority<br/>legacy name / Civic Wardens label"]
        GPF["GuardPdfDefenseFixtureAuthority<br/>legacy name"]
        NSF["NobleEstateSecurityFixtureAuthority"]
        IFF["IndustrialForgeFixtureAuthority"]
        FBF["FoodBioProductionFixtureAuthority"]
        DHF["DomesticHabFixtureAuthority"]
        BMF["BarMarketSocialFixtureAuthority"]
        LCF["LabChemicalFixtureAuthority"]
    end

    subgraph FactionZone["Faction / Concord IP Neutralization Zone"]
        FAC["Faction.java<br/>legacy constants + neutral aliases"]
        IPLED["docs/CONCORD_IP_NEUTRALIZATION_LEDGER.md"]
        IPAUD["scripts/AUDIT_CONCORD_IP_TERMS_WINDOWS.ps1"]
    end

    subgraph Runtime["Authoritative Runtime / Server / Simulation Zone"]
        AWR["AuthoritativeWorldRuntime"]
        AWG["AuthoritativeWorldGrid"]
        ISS["InternalServerSessionAuthority"]
        SPS["SinglePlayerSectorRuntimeBridge"]
        SM["SectorManager"]
        LCSR["LauncherClientServerRuntimeAuthority"]
        ILH["IntegratedLocalMultiplayerHost"]
        SRV["server/admin/* + TheMechanistServer"]
    end

    subgraph Save["Persistence / Profile / Save Zone"]
        GSM["GameStorageManager"]
        CSM["CharacterSaveManager"]
        CSR["CharacterStateRecord"]
        FPM["FallbackProfileManagementAuthority"]
        PID["PlayerIdentity"]
        PSM["PlayerSessionManager"]
        PWR["PlayerWorldStateRecord"]
        SLS["SecureLocalSaveValidationManager"]
    end

    subgraph Diagnostics["Diagnostics / Smoke / Audit Zone"]
        SMOKE["SMOKE_SHARD8_DIAGNOSTIC_WINDOWS.ps1"]
        EXTRACT["EXTRACT_SMOKE_COMPILE_ERRORS_WINDOWS.ps1"]
        ERR["diagnostics/shard8_smoke_*/compile_errors.tsv"]
        CLEAN["Clean baseline:<br/>shard8_smoke_20260601_084116"]
    end

    subgraph Docs["Shard-Mining Documentation Zone"]
        ARCH["ROOT_DOCS/shardmining/Architecture_Map.md"]
        METHOD["Shard_Mining_Method_and_Progress.md"]
        COMPAT["Compatibility_Ledger.md"]
        MERM["Mermaid_Master_Snippet.md"]
    end

    %% Core shell links
    GP --> GPK
    GP --> SP
    SP --> BSP
    SP --> ICP
    SP --> OSP
    GP --> UIS
    OSP --> UIS

    %% Options links
    GP --> GOF
    GP --> LG
    LG --> GAP
    LG --> DSO
    LG --> JRO
    GOF --> GAP
    OSP --> GOF
    OSP --> CRT
    OSP --> AM
    JRO --> JRP

    %% Asset links
    GAP --> AM
    LG --> AM
    AM --> GAR
    AM --> AR
    AM --> SAM
    TAS --> TIR
    TAS --> GB
    TAS --> RPR
    TIA --> AM
    TIA -. temporary alias view .-> TAS
    TAS -. owns public registry .-> TIR
    TSA --> AM
    SAI --> AM

    %% Input links
    GPK --> IR
    KIB --> IR
    IR --> IA
    GIE --> GIS
    CRT --> IA
    CRT --> GIE
    INP --> IR

    %% World links
    WRG --> FAC
    WRG --> RP
    WRG --> RFI
    WRG --> RAF
    WRG --> RGF
    WRG --> RTF
    RFI --> APF
    RFI --> GPF
    RFI --> NSF
    RFI --> IFF
    RFI --> FBF
    RFI --> DHF
    RFI --> BMF
    RFI --> LCF
    FFI --> RFF
    RTI --> RTF
    APF --> FAC
    GPF --> FAC

    %% Runtime/save links
    GP --> AWR
    GP --> SPS
    AWR --> AWG
    SPS --> ISS
    SM --> AWR
    LCSR --> ILH
    LCSR --> SRV
    GP --> GSM
    GSM --> CSM
    CSM --> CSR
    GSM --> FPM
    PSM --> PID
    PSM --> PWR
    SLS --> GSM

    %% Diagnostics/docs links
    SMOKE --> EXTRACT
    SMOKE --> ERR
    ERR --> CLEAN
    CLEAN --> ARCH
    ARCH --> METHOD
    METHOD --> COMPAT
    COMPAT --> MERM
    IPLED --> ARCH
    IPAUD --> IPLED

    %% Rules / debt markers
    TIA -. retire art.byAlias by public registry API .-> COMPAT
    ICP -. conservative bridge, re-mine richer crawl later .-> COMPAT
    FAC -. legacy enum constants retained for save compatibility .-> COMPAT
    GP -. shrink by deleting mined shard bodies .-> METHOD
```

## Reading Guide

- Solid arrows indicate active dependency or call ownership.
- Dotted arrows indicate compatibility debt, deferred cleanup, or documentation relationship.
- `GamePanel.java` remains the shell and source of truth for unmined behavior.
- `ROOT_DOCS/shardmining/Architecture_Map.md` remains the canonical textual map.

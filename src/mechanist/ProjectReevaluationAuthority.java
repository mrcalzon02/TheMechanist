package mechanist;

import java.util.List;

/**
 * Owns the structured project re-evaluation sequence so goal replanning is repeatable,
 * inspectable from the host console, and recorded in durable docs instead of scattered notes.
 */
public final class ProjectReevaluationAuthority {
    static final String VERSION = "project-reevaluation-authority-0.9.10hd";

    private ProjectReevaluationAuthority() {}

    public static String alignmentSummary() {
        return String.join("\n",
                "Project re-evaluation alignment — " + VERSION,
                "Core purpose: The Mechanist is a pure Java 17 Swing underhive simulation with a local launcher/client/server trajectory, persistent worlds, player/faction continuity, and dense management UI.",
                "Evaluation method: compare durable master goals against implemented authorities, identify deltas, rewrite near-term goals, and order work by prerequisites plus definition of done.",
                "Operating rule: do not create pass-specific documents; update the four durable docs and expose concise host-console summaries.");
    }

    public static String gapAnalysis() {
        List<String> rows = List.of(
                row("Server-authoritative client boundary", "Partial", "Single-writer lane, command requests, snapshots, turn modes, long actions, and admin console exist; many UI helper actions still resolve through legacy helpers after entering the authority lane.", "High"),
                row("World/character persistence separation", "Strong foundation", "Single-player bundled snapshots and server character/world separation exist; more world-owned namespaces need ownership audits and migration tests before shorthand schemas.", "Medium"),
                row("Player/NPC/faction management parity", "Foundation", "Player/NPC command tiers and faction autonomy ledgers exist; full assignment UI, permissions editing, and live personnel management are not yet unified.", "High"),
                row("Universal contextual UI", "Descriptor foundation", "UniversalManagementWindowAuthority describes modules, but most Swing surfaces are still independent panels/buttons instead of wrapper-hosted modules.", "High"),
                row("Display/graphics/runtime configuration", "Good foundation", "Display density, resolution detection, graphics split, diagnostics, JVM profiles, accessibility, and profile fallback exist; restart/profile flow still needs broader end-to-end launcher/client/server validation.", "Medium"),
                row("Rendering and accessibility compatibility", "Foundation", "Visual lighting, F3 overlay, Daltonization, narration hooks, and motion controls exist; critical gameplay state still needs dual-channel indicators across all target/standing/danger UI.", "Medium"),
                row("Dedicated server/multiplayer readiness", "Closed/initializer only", "Headless server packaging and save namespace exist; public networking, remote clients, remote console, and server GUI are intentionally not opened.", "High"),
                row("Performance/data efficiency", "Audited first layer", "Save catalogs and runtime profiles exist; high-churn rendering loops and large persistence namespaces still need measured primitive/cache-oriented optimization passes.", "Medium"));
        return "Strict gap analysis\n" + String.join("\n", rows);
    }

    public static String rewrittenGoals() {
        return String.join("\n",
                "Rewritten goals by category",
                "Architecture: Continue converting player-facing actions into explicit server command requests before parallel sector mutation. Why: the server-authoritative boundary is present but legacy helpers still execute inside it.",
                "Persistence: Keep single-player bundled snapshots and server world-authority files as separate models; only introduce shorthand keys behind versioned schema/migration tests. Why: separation exists, but silent key compression would risk corrupting old saves.",
                "Faction/personnel: Build one assignment/permission authority consumed by both player and NPC management. Why: rank parity exists, but actual management flows are not yet unified.",
                "UX/UI: Migrate high-value screens into UniversalWindow modules one at a time, starting with faction/personnel and inventory/container transfer. Why: descriptors exist while legacy panels remain fragmented.",
                "Performance: Optimize only measured hot paths: final blit/render overlays, tile/image scale caches, text layout caches, and save payload namespaces. Why: JVM/render options exist, but unmeasured rewrites invite instability.",
                "Server: Keep headless server as a status/runtime namespace until command/snapshot and save authority are robust enough for networking. Why: opening remote clients before authority hardening would create security and sync debt.");
    }

    public static String orderOfOperations() {
        return String.join("\n",
                "Next order of operations",
                "1. Command-conversion continuation — prerequisite: current AuthoritativeWorldRuntime and WorldCommandRequest records; DoD: one additional legacy action family routes through a typed command with rejection diagnostics and smoke coverage.",
                "2. Faction/personnel assignment authority — prerequisite: PlayerNpcCommandParityAuthority and player-faction world ledger; DoD: player and NPC assignments share one tier-aware permission resolver while remaining separate rosters.",
                "3. UniversalWindow first migration — prerequisite: UniversalManagementWindowAuthority descriptors; DoD: one real management surface opens through the wrapper with Escape/right-click back behavior and stable pane anchors.",
                "4. Persistence ownership audit v2 — prerequisite: bundled single-player and server world split; DoD: itemized catalog marks each remaining large namespace as character-owned, world-owned, cache, audit, or migration-only.",
                "5. Runtime/profile validation pass — prerequisite: JVM profiles, accessibility, display/graphics menus, fallback profiles; DoD: profile export/import and restart-required JVM settings preserve selected launcher/client/server target without touching saves.",
                "6. Render/accessibility indicator pass — prerequisite: Daltonization and F3 overlay; DoD: danger/target/standing surfaces include at least one non-color indicator pathway and diagnostics confirm no gameplay-light mutation from render effects.",
                "7. Server networking planning gate — prerequisite: command conversion, snapshot filtering, save authority, session permission model; DoD: documented API boundary and no public networking opened until smokeable local loopback harness exists.");
    }

    public static String hiddenDependency() {
        return "Hidden dependency: the largest bottleneck is not networking or graphics; it is remaining legacy UI/helper code that still assumes the live World object is locally available. Until those paths submit typed commands and consume snapshots/modules, multiplayer, faction autonomy, and universal UI will keep dragging old assumptions forward.";
    }

    public static String fullReport() {
        return alignmentSummary() + "\n\n" + gapAnalysis() + "\n\n" + rewrittenGoals() + "\n\n" + orderOfOperations() + "\n\n" + hiddenDependency();
    }

    public static String auditSummary() {
        return "authority=" + VERSION + " commands=/project_evaluation,/gap_analysis,/replanned_goals,/order_of_operations,/hidden_dependency scope=durable-doc-replanning no-standalone-docs";
    }

    private static String row(String goal, String status, String delta, String risk) {
        return "- goal=" + goal + " | status=" + status + " | gap=" + delta + " | risk=" + risk;
    }
}

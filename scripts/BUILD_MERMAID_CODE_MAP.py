#!/usr/bin/env python3
"""
BUILD_MERMAID_CODE_MAP.py

Builds and evaluates the codewide Mermaid.js ownership map for The Mechanist.

The map is governance evidence. It does not compile, move, delete, or rewrite Java.
It inventories Java modules, assigns each module to a Mermaid position, writes a
master Mermaid record, and logs unpositioned/oversized/error candidates.

Run from repository root:

    py -3 scripts\BUILD_MERMAID_CODE_MAP.py --apply

Outputs:

    ROOT_docs/functionmap/Mermaid_Code_Map_Master.md
    ROOT_docs/functionmap/generated/MERMAID_CODE_MAP.md
    ROOT_docs/functionmap/generated/CODE_MERMAID_POSITION_LEDGER.tsv
    ROOT_docs/functionmap/generated/CODE_MERMAID_EVALUATION.tsv
"""

from __future__ import annotations

import argparse
import dataclasses
import datetime as dt
import hashlib
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

ROOT = Path.cwd()
SRC_ROOT = ROOT / "src"
OUT_ROOT = ROOT / "ROOT_docs" / "functionmap"
GENERATED_ROOT = OUT_ROOT / "generated"
MASTER_RECORD = OUT_ROOT / "Mermaid_Code_Map_Master.md"

ZONE_RULES: List[Tuple[str, str, Sequence[str], str]] = [
    ("LOCALIZATION_TEXT", "Localization Text", [r"locali[sz]e|locale|language|translation|semantic text|text manager|properties"], "Semantic player-facing text keys, locale files, translation selection, and language override surfaces."),
    ("UI_RENDER", "UI Render Surfaces", [r"paint|draw|render|surface|painter|graphics|hud|overlay|splash|menu|panel|frame|color|font|infopedia"], "Screen painters, immediate-mode drawing, HUD, visual panels, and display line providers."),
    ("UI_INPUT", "UI Input Navigation", [r"key|mouse|wheel|scroll|input|controller|gamepad|keyboard|route|navigation|button"], "Keyboard, mouse, controller, scrollbars, button routing, and screen route changes."),
    ("RUNTIME_OPTIONS", "Runtime Options", [r"option|display|graphics|sound|volume|jvm|runtime|accessibility|scale|density|doom"], "Display/audio/JVM/accessibility/options runtime controls."),
    ("WORLD_GEN", "World Generation Transition", [r"world|zone|sector|atlas|generation|transition|audit|room|road|plaza|frontage|terrain"], "World setup, atlas, zones, rooms, roads, generation audit, and transitions."),
    ("INVENTORY_PERSIST", "Inventory Items Persistence", [r"inventory|item|container|loot|equip|save|load|autosave|profile|persistence|storage|economy|trade|stock"], "Inventory, item catalog/economy, equipment, containers, save/load/profile, trade persistence, and stock ledgers."),
    ("FIXTURE_MACHINE", "Fixtures Machines", [r"fixture|machine|vending|interact|use|hack|smelter|assembler|relay|turret|defense|production"], "Fixtures, machines, vending, powered devices, production, and construction defenses."),
    ("COMBAT_SIM", "Combat Entity Simulation", [r"combat|attack|damage|npc|entity|turn|advance|move|movement|motion|path|faction|heat|suspicion|simulation|population|personnel"], "Entity state, turn advancement, combat, movement, factions, heat/suspicion, simulation, and population systems."),
    ("ASSET_REGISTRY", "Asset Registry Art", [r"asset|registry|tile|art|image|glyph|semantic|texture|atlas|portrait|icon"], "Asset registry, tile art, glyphs, semantic art, portraits, icons, and texture indexes."),
    ("SERVER_AUTH", "Server Authority Launcher", [r"server|authoritative|launcher|multiplayer|session|client|bridge|host|packet|network"], "Authoritative runtime, launcher, client/server boundary, networking, multiplayer host/session."),
    ("DIAGNOSTIC_DOC", "Diagnostics Smoke Audit", [r"debug|diagnostic|smoke|audit|test|validation|report|ledger|history|manifest"], "Smoke scripts, debug reports, validation, generated ledgers, diagnostics, and audit tooling."),
]

ZONE_LABELS: Dict[str, str] = {zone_id: label for zone_id, label, _patterns, _desc in ZONE_RULES}
ZONE_LABELS["UNPOSITIONED"] = "Unpositioned / Needs Map Assignment"

MODULE_OVERRIDES: List[Tuple[str, str, str]] = [
    ("SemanticTextManager.java", "LOCALIZATION_TEXT", "Semantic key text manager and locale loader."),
    ("MainMenuLanguageSelectorAuthority.java", "LOCALIZATION_TEXT", "Main-menu language selector and locale override surface."),
    ("Locale", "LOCALIZATION_TEXT", "Locale and translation family."),
    ("Language", "LOCALIZATION_TEXT", "Language selector family."),
    ("Localization", "LOCALIZATION_TEXT", "Localization family."),
    ("TextManager", "LOCALIZATION_TEXT", "Semantic text manager family."),
    ("FactionWideStockTracker.java", "INVENTORY_PERSIST", "Faction-wide stock ledger owner."),
    ("ZoneFactionStockTracker.java", "INVENTORY_PERSIST", "Zone/faction stock ledger owner."),
    ("FactionInventoryStockAuthority.java", "INVENTORY_PERSIST", "Faction stock access authority owner."),
    ("TraderTradeActionAuthority.java", "INVENTORY_PERSIST", "Trader action/offer authority owner."),
    ("LimitedVendingStockAuthority.java", "FIXTURE_MACHINE", "Limited vending stock authority owner."),
    ("FactionPopulationTracker.java", "COMBAT_SIM", "Faction-wide population ledger owner."),
    ("ZonePopulationTracker.java", "COMBAT_SIM", "Zone/faction population ledger owner."),
    ("StockTracker", "INVENTORY_PERSIST", "Stock tracking family."),
    ("StockAuthority", "INVENTORY_PERSIST", "Stock authority family."),
    ("PopulationTracker", "COMBAT_SIM", "Population tracking family."),
    ("MediaLayerAlpha.java", "UI_RENDER", "Media layer rendering/visual composition owner."),
    ("ControllerGlyphPromptAuthority.java", "UI_INPUT", "Controller prompt mode and glyph/text fallback owner."),
    ("ControllerConnectionStateTracker.java", "UI_INPUT", "Controller connection notice and fallback state owner."),
    ("ControllerTapHoldTracker.java", "UI_INPUT", "Controller tap/hold timing interpretation owner."),
    ("GamepadInputEngine.java", "UI_INPUT", "Gamepad polling and connection notice bridge owner."),
    ("GenericControllerSchema.java", "UI_INPUT", "Generic controller schema and runtime action translation owner."),
    ("MovementPlanningAuthority.java", "COMBAT_SIM", "Player movement planning, occupied-tile routing, and recovery search owner."),
    ("ZoneTileMovementResolutionAuthority.java", "COMBAT_SIM", "Actor-layer push/squeeze movement resolution owner."),
    ("ZoneTileLayerMappingAuditAuthority.java", "WORLD_GEN", "Zone tile semantic layer audit owner."),
    ("ZoneTileState.java", "WORLD_GEN", "Structured zone tile state and slot bridge owner."),
    ("WorldRuntimeGenerationFramework.java", "WORLD_GEN", "World generation framework owner."),
    ("Road", "WORLD_GEN", "Road/frontage/grid generation family."),
    ("Room", "WORLD_GEN", "Room/profile/generation family."),
    ("Spawn", "WORLD_GEN", "Spawn placement/generation family."),
    ("Zone", "WORLD_GEN", "Zone generation/audit/transition family."),
    ("Atlas", "WORLD_GEN", "Atlas/world map family."),
    ("ProductionAuthorityFramework.java", "FIXTURE_MACHINE", "Production and powered-machine authority owner."),
    ("IndustrialForgeFixtureAuthority.java", "FIXTURE_MACHINE", "Fixture machine owner."),
    ("FoodBioProductionFixtureAuthority.java", "FIXTURE_MACHINE", "Production fixture owner."),
    ("Fixture", "FIXTURE_MACHINE", "Fixture interaction family."),
    ("Machine", "FIXTURE_MACHINE", "Machine interaction family."),
    ("Vending", "FIXTURE_MACHINE", "Vending interaction family."),
    ("ItemEconomyFramework.java", "INVENTORY_PERSIST", "Item economy/catalog authority owner."),
    ("ContainerTradeFramework.java", "INVENTORY_PERSIST", "Container trade/inventory owner."),
    ("Inventory", "INVENTORY_PERSIST", "Inventory family."),
    ("Item", "INVENTORY_PERSIST", "Item family."),
    ("Trade", "INVENTORY_PERSIST", "Trade family."),
    ("GameStorageManager.java", "INVENTORY_PERSIST", "Save/storage owner."),
    ("CharacterSaveManager.java", "INVENTORY_PERSIST", "Character save owner."),
    ("FallbackProfileManagementAuthority.java", "INVENTORY_PERSIST", "Profile persistence owner."),
    ("WorldSimulationFramework.java", "COMBAT_SIM", "World simulation owner."),
    ("PopulationPersonnelFramework.java", "COMBAT_SIM", "Population/personnel simulation owner."),
    ("FactionServicesFramework.java", "COMBAT_SIM", "Faction service/simulation owner."),
    ("Faction.java", "COMBAT_SIM", "Faction model owner."),
    ("Combat", "COMBAT_SIM", "Combat family."),
    ("Npc", "COMBAT_SIM", "NPC/entity family."),
    ("Entity", "COMBAT_SIM", "Entity family."),
    ("Movement", "COMBAT_SIM", "Movement/pathing family."),
    ("Path", "COMBAT_SIM", "Pathing family."),
    ("Turn", "COMBAT_SIM", "Turn simulation family."),
    ("InfrastructurePromotionRegistry.java", "ASSET_REGISTRY", "Infrastructure registry/catalog owner."),
    ("Asset", "ASSET_REGISTRY", "Asset registry family."),
    ("Tile", "ASSET_REGISTRY", "Tile/art registry family."),
    ("Glyph", "ASSET_REGISTRY", "Glyph/art registry family."),
    ("Portrait", "ASSET_REGISTRY", "Portrait semantic asset family."),
    ("SemanticAsset", "ASSET_REGISTRY", "Semantic asset registry family."),
    ("GameOptionsFramework.java", "RUNTIME_OPTIONS", "Game options owner."),
    ("Options", "RUNTIME_OPTIONS", "Options/runtime controls family."),
    ("RuntimePathResolver.java", "RUNTIME_OPTIONS", "Runtime path/options support."),
    ("FirstPerson3DFramework.java", "UI_RENDER", "Experimental first-person renderer owner."),
    ("SimulationEditorSuite.java", "UI_RENDER", "In-game editor UI surface owner."),
    ("Painter", "UI_RENDER", "Screen painter family."),
    ("Surface", "UI_RENDER", "Screen surface family."),
    ("Screen", "UI_RENDER", "Screen rendering family."),
    ("Panel", "UI_RENDER", "Panel/rendering family."),
    ("Key", "UI_INPUT", "Keyboard/input family."),
    ("Input", "UI_INPUT", "Input family."),
    ("Mouse", "UI_INPUT", "Mouse input family."),
    ("Scroll", "UI_INPUT", "Scroll/navigation family."),
    ("Command", "UI_INPUT", "Command/action routing family."),
    ("Server", "SERVER_AUTH", "Server authority family."),
    ("Client", "SERVER_AUTH", "Client/server boundary family."),
    ("Launcher", "SERVER_AUTH", "Launcher boundary family."),
    ("Packet", "SERVER_AUTH", "Network packet family."),
    ("Network", "SERVER_AUTH", "Network family."),
    ("Debug", "DIAGNOSTIC_DOC", "Debug/diagnostic family."),
    ("Diagnostic", "DIAGNOSTIC_DOC", "Diagnostic family."),
    ("Smoke", "DIAGNOSTIC_DOC", "Smoke testing family."),
    ("Audit", "DIAGNOSTIC_DOC", "Audit family."),
]

CLASS_RE = re.compile(r"\b(?:public\s+|private\s+|protected\s+|abstract\s+|final\s+|static\s+)*(class|interface|enum|record)\s+([A-Za-z_$][\w$]*)")
METHOD_RE = re.compile(
    r"^\s*(?:(?:public|private|protected|static|final|synchronized|abstract|native|strictfp)\s+)*"
    r"(?:(?:[A-Za-z_$][\w$<>\[\], ?.&]*|void|int|long|double|float|boolean|char|byte|short)\s+)?"
    r"(?P<name>[A-Za-z_$][\w$]*)\s*\([^;{}]*\)\s*(?:throws\s+[^{}]+)?\{"
)
CONTROL_WORDS = {"if", "for", "while", "switch", "catch", "try", "else", "do", "return", "new", "case", "default"}

@dataclasses.dataclass(frozen=True)
class ModuleRecord:
    path: str
    class_count: int
    function_count: int
    line_count: int
    byte_count: int
    zone_id: str
    zone_label: str
    node_id: str
    status: str
    sha256: str


def rel(path: Path) -> str:
    return path.resolve().relative_to(ROOT.resolve()).as_posix()


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def digest(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def neutralize(line: str, in_block: bool = False) -> Tuple[str, bool]:
    out: List[str] = []
    quote: Optional[str] = None
    esc = False
    i = 0
    while i < len(line):
        ch = line[i]
        nxt = line[i + 1] if i + 1 < len(line) else ""
        if in_block:
            if ch == "*" and nxt == "/":
                in_block = False
                out.append("  ")
                i += 2
            else:
                out.append(" ")
                i += 1
            continue
        if quote:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == quote:
                quote = None
            out.append(" ")
            i += 1
            continue
        if ch == "/" and nxt == "/":
            out.append(" " * (len(line) - i))
            break
        if ch == "/" and nxt == "*":
            in_block = True
            out.append("  ")
            i += 2
            continue
        if ch in ("'", '"'):
            quote = ch
            out.append(" ")
            i += 1
            continue
        out.append(ch)
        i += 1
    return "".join(out), in_block


def parse_function_count(lines: Sequence[str]) -> int:
    count = 0
    for line in lines:
        compact = " ".join(line.strip().split())
        if not compact or compact.startswith(("@", "//", "*", "/*")):
            continue
        if "{" not in compact or "(" not in compact or ")" not in compact:
            continue
        safe, _ = neutralize(compact)
        if ";" in safe.split("{")[0]:
            continue
        m = METHOD_RE.search(safe)
        if m and m.group("name") not in CONTROL_WORDS:
            count += 1
    return count


def override_zone(path: str) -> Tuple[Optional[str], Optional[str]]:
    hay = path.replace("\\", "/")
    filename = hay.rsplit("/", 1)[-1]
    for needle, zone_id, _reason in MODULE_OVERRIDES:
        if needle == filename or needle in hay:
            return zone_id, ZONE_LABELS.get(zone_id, zone_id)
    return None, None


def classify(path: str, text: str) -> Tuple[str, str]:
    zone_id, label = override_zone(path)
    if zone_id and label:
        return zone_id, label
    hay = f"{path}\n{text[:6000]}".lower()
    for zone_id, label, patterns, _desc in ZONE_RULES:
        if any(re.search(pattern, hay, re.IGNORECASE) for pattern in patterns):
            return zone_id, label
    return "UNPOSITIONED", "Unpositioned / Needs Map Assignment"


def node_id_for(path: str) -> str:
    base = re.sub(r"[^A-Za-z0-9]+", "_", path)
    base = re.sub(r"_+", "_", base).strip("_")
    if not base:
        base = "module"
    return "M_" + base[:90]


def gather_java() -> List[Path]:
    if not SRC_ROOT.exists():
        return []
    return sorted(SRC_ROOT.rglob("*.java"), key=lambda p: rel(p))


def module_record(path: Path) -> ModuleRecord:
    text = read_text(path)
    lines = text.replace("\r\n", "\n").replace("\r", "\n").splitlines()
    r = rel(path)
    classes = len(CLASS_RE.findall(text))
    functions = parse_function_count(lines)
    zone_id, zone_label = classify(r, text)
    status = "positioned" if zone_id != "UNPOSITIONED" else "unpositioned_error"
    if path.name == "GamePanel.java" and not text.strip():
        status = "retired_empty_shell"
        zone_id = "DIAGNOSTIC_DOC"
        zone_label = "Diagnostics Smoke Audit"
    return ModuleRecord(r, classes, functions, len(lines), len(text.encode("utf-8", errors="replace")), zone_id, zone_label, node_id_for(r), status, digest(text))


def mermaid(records: Sequence[ModuleRecord]) -> str:
    by_zone: Dict[str, List[ModuleRecord]] = defaultdict(list)
    for record in records:
        by_zone[record.zone_id].append(record)
    lines: List[str] = ["flowchart TD"]
    lines.append('    ROOT["The Mechanist Codebase<br/>Mermaid position master"]')
    lines.append('    ERRORS["Code errors / unmapped modules<br/>must submit map position"]')
    lines.append('    ROOT --> ERRORS')
    for zone_id, label, _patterns, _desc in ZONE_RULES + [("UNPOSITIONED", "Unpositioned / Needs Map Assignment", [], "")]:
        entries = by_zone.get(zone_id, [])
        if not entries:
            continue
        znode = f"Z_{zone_id}"
        lines.append(f'    ROOT --> {znode}["{label}<br/>{len(entries)} modules"]')
        biggest = sorted(entries, key=lambda r: (r.line_count, r.function_count), reverse=True)[:12]
        for rec in biggest:
            label_text = rec.path.replace('"', "'")
            lines.append(f'    {znode} --> {rec.node_id}["{label_text}<br/>{rec.function_count} funcs / {rec.line_count} lines"]')
        if len(entries) > len(biggest):
            other_node = f"{znode}_OTHER"
            lines.append(f'    {znode} --> {other_node}["+ {len(entries) - len(biggest)} smaller modules"]')
    return "```mermaid\n" + "\n".join(lines) + "\n```\n"


def write_tsv(path: Path, header: Sequence[str], rows: Iterable[Sequence[object]]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("\t".join(header) + "\n")
        for row in rows:
            f.write("\t".join(str(x).replace("\t", " ").replace("\n", " ") for x in row) + "\n")


def write_reports(records: Sequence[ModuleRecord]) -> None:
    GENERATED_ROOT.mkdir(parents=True, exist_ok=True)
    OUT_ROOT.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    zone_counts = Counter(r.zone_id for r in records)
    status_counts = Counter(r.status for r in records)
    unpositioned = [r for r in records if r.status == "unpositioned_error"]
    oversized = [r for r in records if r.line_count >= 800 or r.byte_count >= 120_000 or r.function_count >= 75]
    write_tsv(GENERATED_ROOT / "CODE_MERMAID_POSITION_LEDGER.tsv", ["path", "node_id", "zone_id", "zone_label", "status", "classes", "functions", "lines", "bytes", "sha256"], [(r.path, r.node_id, r.zone_id, r.zone_label, r.status, r.class_count, r.function_count, r.line_count, r.byte_count, r.sha256) for r in records])
    write_tsv(GENERATED_ROOT / "CODE_MERMAID_EVALUATION.tsv", ["severity", "path", "node_id", "zone_id", "status", "message"], [("ERROR", r.path, r.node_id, r.zone_id, r.status, "Module did not receive a Mermaid position; add rule or explicit ownership.") for r in unpositioned] + [("WARN", r.path, r.node_id, r.zone_id, r.status, f"Oversized mapped module: {r.function_count} funcs / {r.line_count} lines / {r.byte_count} bytes") for r in oversized])
    map_block = mermaid(records)
    (GENERATED_ROOT / "MERMAID_CODE_MAP.md").write_text("# Generated Mermaid Code Map\n\nGenerated: `" + stamp + "`\n\n" + map_block, encoding="utf-8")
    with MASTER_RECORD.open("w", encoding="utf-8", newline="\n") as f:
        f.write("# Mermaid Code Map Master Record\n\n")
        f.write("Status: active master code-position map.\n\n")
        f.write(f"Generated/evaluated: `{stamp}`\n\n")
        f.write("## Top-Line Rule\n\n")
        f.write("Every code module, generated code error, compile error cluster, or subsystem remap must submit a Mermaid position before it is considered mapped. Unpositioned modules are architecture debt, not invisible implementation detail.\n\n")
        f.write("## Counts\n\n")
        f.write(f"- Java modules mapped: `{len(records)}`\n")
        f.write(f"- Unpositioned modules: `{len(unpositioned)}`\n")
        f.write(f"- Oversized mapped modules: `{len(oversized)}`\n\n")
        f.write("## Zone Counts\n\n")
        for zone_id, count in sorted(zone_counts.items()):
            f.write(f"- `{zone_id}`: `{count}` modules\n")
        f.write("\n## Status Counts\n\n")
        for status, count in sorted(status_counts.items()):
            f.write(f"- `{status}`: `{count}` modules\n")
        f.write("\n## Explicit Override Rule\n\n")
        f.write("Explicit `MODULE_OVERRIDES` entries in `scripts/BUILD_MERMAID_CODE_MAP.py` beat broad keyword heuristics. Add an override when a known owner is misclassified by generic words such as render, panel, audit, semantic, faction, or zone.\n")
        f.write("\n## Master Mermaid Map\n\n")
        f.write(map_block)
        f.write("\n## Generated Ledgers\n\n")
        f.write("- `ROOT_docs/functionmap/generated/CODE_MERMAID_POSITION_LEDGER.tsv`\n")
        f.write("- `ROOT_docs/functionmap/generated/CODE_MERMAID_EVALUATION.tsv`\n")
        f.write("- `ROOT_docs/functionmap/generated/MERMAID_CODE_MAP.md`\n")


def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="Build/evaluate Mermaid.js code ownership map.")
    ap.add_argument("--apply", action="store_true", help="Write master record and generated ledgers.")
    args = ap.parse_args(argv)
    records = [module_record(path) for path in gather_java()]
    print(f"Java modules found: {len(records)}")
    print(f"Unpositioned modules: {sum(1 for r in records if r.status == 'unpositioned_error')}")
    if not args.apply:
        print("Dry run only. Re-run with --apply to write Mermaid master records.")
        return 0
    write_reports(records)
    print(f"Wrote {MASTER_RECORD}")
    print(f"Wrote generated ledgers under {GENERATED_ROOT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

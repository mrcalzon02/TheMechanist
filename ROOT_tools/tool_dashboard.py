#!/usr/bin/env python3
"""
Local-only tool dashboard for The Mechanist.

Starts a localhost web dashboard with:
  - whitelisted project command buttons
  - milestone tracker view from docs/PROJECT_MILESTONES.json
  - document index view from docs/document_index.json
  - development history/document filtered view
  - ROOT_RELEASE scaffold controls

Run from repository root:

    python ROOT_tools/tool_dashboard.py

Then open:

    http://127.0.0.1:8765
"""

from __future__ import annotations

import argparse
import html
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import subprocess
import sys
import time
import traceback
import urllib.parse
import webbrowser
from dataclasses import dataclass, asdict
from typing import Any

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
PYTHON = sys.executable
DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 8765

MILESTONES_PATH = REPO_ROOT / "docs" / "PROJECT_MILESTONES.json"
DOCUMENT_INDEX_PATH = REPO_ROOT / "docs" / "document_index.json"
RELEASE_ROOT = REPO_ROOT / "ROOT_RELEASE"


@dataclass(frozen=True)
class ToolCommand:
    id: str
    label: str
    group: str
    description: str
    command: list[str]
    danger: str = "safe"  # safe | writes | destructive
    cwd: str = str(REPO_ROOT)

    def display_command(self) -> str:
        parts: list[str] = []
        for item in self.command:
            parts.append(f'"{item}"' if any(ch.isspace() for ch in item) else item)
        return " ".join(parts)


def repo_path(*parts: str) -> str:
    return str(REPO_ROOT.joinpath(*parts))


def py_script(*parts: str) -> list[str]:
    return [PYTHON, repo_path(*parts)]


def build_registry() -> dict[str, ToolCommand]:
    commands = [
        ToolCommand(
            id="doc_index",
            label="Refresh Document Index",
            group="Documentation",
            description="Indexes docs and top-level process/history documents for dashboard browsing.",
            command=py_script("ROOT_tools", "document_indexer.py"),
            danger="writes",
        ),
        ToolCommand(
            id="repo_scan",
            label="Refresh Repository File Manifest",
            group="Repository Audit",
            description="Indexes every repository file and rewrites docs/repository_file_manifest.tsv.",
            command=py_script("ROOT_tools", "repository_scan_indexer.py"),
            danger="writes",
        ),
        ToolCommand(
            id="repo_audit",
            label="Build Repository Manifest Audit",
            group="Repository Audit",
            description="Reads the repository manifest and writes compact audit reports under docs/.",
            command=py_script("ROOT_tools", "repository_manifest_auditor.py"),
            danger="writes",
        ),
        ToolCommand(
            id="clear_previews_dry",
            label="Dry Run: Clear Atlas Previews",
            group="Atlas Pipeline",
            description="Lists stale preview/manual-audit files that would be removed.",
            command=py_script("ROOT_tools", "atlas_asset_pipeline", "clear_atlas_previews.py") + ["--dry-run"],
        ),
        ToolCommand(
            id="clear_previews",
            label="Clear Atlas Previews",
            group="Atlas Pipeline",
            description="Deletes stale atlas browser preview/manual-slice audit artifacts.",
            command=py_script("ROOT_tools", "atlas_asset_pipeline", "clear_atlas_previews.py"),
            danger="destructive",
        ),
        ToolCommand(
            id="semantic_index_256",
            label="Regenerate 256px Semantic Asset Index",
            group="Atlas Pipeline",
            description="Indexes compiled 256px atlas tiles and rewrites JSON/TSV content indexes.",
            command=py_script("ROOT_tools", "atlas_asset_pipeline", "semantic_asset_indexer.py") + ["--size", "256"],
            danger="writes",
        ),
        ToolCommand(
            id="packager_dry",
            label="Dry Run: Compiled Asset Packager",
            group="Package Promotion",
            description="Reports how compiled atlas assets would be promoted into package runtime assets.",
            command=py_script("ROOT_tools", "Compiled_asset_packager.py") + ["--dry-run"],
        ),
        ToolCommand(
            id="packager_run",
            label="Run Compiled Asset Packager",
            group="Package Promotion",
            description="Copies compiled atlas assets into PACKAGE_client and updates runtime_asset_manifest.json.",
            command=py_script("ROOT_tools", "Compiled_asset_packager.py"),
            danger="writes",
        ),
        ToolCommand(
            id="packager_automotive_dry",
            label="Dry Run: Package Automotive Assets Only",
            group="Package Promotion",
            description="Focused promotion check for automotive/vehicle asset slices.",
            command=py_script("ROOT_tools", "Compiled_asset_packager.py") + ["--dry-run", "--include", "*automotive*", "--include", "*Automotive*", "--include", "*vehicle*", "--include", "*Vehicle*"],
        ),
        ToolCommand(
            id="packager_automotive_run",
            label="Package Automotive Assets Only",
            group="Package Promotion",
            description="Focused promotion for automotive/vehicle asset slices.",
            command=py_script("ROOT_tools", "Compiled_asset_packager.py") + ["--include", "*automotive*", "--include", "*Automotive*", "--include", "*vehicle*", "--include", "*Vehicle*"],
            danger="writes",
        ),
        ToolCommand(
            id="release_scaffold_dry",
            label="Dry Run: Master Release Scaffold",
            group="Release",
            description="Shows the ROOT_RELEASE scaffold plan without writing files.",
            command=py_script("ROOT_tools", "master_release_builder.py") + ["--dry-run"],
        ),
        ToolCommand(
            id="release_scaffold_run",
            label="Create/Update ROOT_RELEASE Scaffold",
            group="Release",
            description="Creates ROOT_RELEASE folders and release_scaffold_manifest.json. This is a placeholder for future full release compilation.",
            command=py_script("ROOT_tools", "master_release_builder.py"),
            danger="writes",
        ),
    ]
    return {command.id: command for command in commands}


REGISTRY = build_registry()
RUN_HISTORY: list[dict[str, Any]] = []


def load_json_file(path: Path, default: Any) -> Any:
    try:
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception as exc:
        return {"error": str(exc), "path": str(path)}
    return default


def milestone_summary() -> dict[str, Any]:
    data = load_json_file(MILESTONES_PATH, {"milestones": []})
    milestones = data.get("milestones", []) if isinstance(data, dict) else []
    counts: dict[str, int] = {}
    for item in milestones:
        status = str(item.get("status", "unknown"))
        counts[status] = counts.get(status, 0) + 1
    return {"path": str(MILESTONES_PATH), "counts": counts, "milestones": milestones, "legend": data.get("status_legend", {}) if isinstance(data, dict) else {}}


def document_index() -> dict[str, Any]:
    data = load_json_file(DOCUMENT_INDEX_PATH, {"documents": [], "document_count": 0})
    docs = data.get("documents", []) if isinstance(data, dict) else []
    categories: dict[str, int] = {}
    for doc in docs:
        cat = str(doc.get("category", "unknown"))
        categories[cat] = categories.get(cat, 0) + 1
    return {"path": str(DOCUMENT_INDEX_PATH), "categories": categories, "documents": docs, "document_count": len(docs)}


def release_status() -> dict[str, Any]:
    manifest = RELEASE_ROOT / "release_scaffold_manifest.json"
    return {
        "root": str(RELEASE_ROOT),
        "exists": RELEASE_ROOT.exists(),
        "manifest_exists": manifest.exists(),
        "manifest": load_json_file(manifest, {}) if manifest.exists() else {},
    }


def command_groups_html() -> str:
    groups: dict[str, list[ToolCommand]] = {}
    for command in REGISTRY.values():
        groups.setdefault(command.group, []).append(command)
    chunks: list[str] = []
    for group_name, commands in groups.items():
        chunks.append(f"<section><h2>{html.escape(group_name)}</h2><div class='cards'>")
        for command in commands:
            chunks.append(f"""
            <article class="card {html.escape(command.danger)}">
              <h3>{html.escape(command.label)}</h3>
              <p>{html.escape(command.description)}</p>
              <pre>{html.escape(command.display_command())}</pre>
              <button data-command="{html.escape(command.id)}" data-danger="{html.escape(command.danger)}">Run</button>
            </article>
            """)
        chunks.append("</div></section>")
    return "".join(chunks)


def dashboard_html() -> str:
    tool_json = json.dumps({key: asdict(value) | {"display_command": value.display_command()} for key, value in REGISTRY.items()}, indent=2)
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>The Mechanist Project Dashboard</title>
<style>
:root{{color-scheme:dark;--bg:#151515;--panel:#222;--line:#444;--text:#eee;--muted:#aaa;--ok:#5fc477;--warn:#e2bc54;--bad:#ff725c;--blue:#7eb7ff;--review:#d6a84f}}
body{{margin:0;background:var(--bg);color:var(--text);font-family:Segoe UI,Arial,sans-serif}}
header{{position:sticky;top:0;background:#101010;border-bottom:1px solid var(--line);padding:14px 24px;z-index:5}}
h1{{margin:0 0 6px;font-size:24px}} header p{{margin:0;color:var(--muted)}}
main{{padding:20px 24px 36px;display:grid;gap:20px}}
section{{background:#1b1b1b;border:1px solid #333;padding:16px}} h2{{margin:0 0 12px;font-size:18px}}
.tabs{{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}} .tab-button{{background:#252525}} .tab-button[data-active="true"]{{background:#324f72;border-color:var(--blue)}}
.tab{{display:none}} .tab[data-active="true"]{{display:grid;gap:20px}}
.cards{{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:12px}}
.card{{background:var(--panel);border:1px solid var(--line);padding:14px;display:flex;flex-direction:column;gap:8px}}
.card h3{{margin:0;font-size:16px}} .card p{{margin:0;color:#ccc;line-height:1.35}}
.card pre{{white-space:pre-wrap;word-break:break-word;background:#111;border:1px solid #333;padding:8px;color:#ddd;font-size:12px;min-height:44px}}
.safe{{border-left:4px solid var(--ok)}} .writes{{border-left:4px solid var(--warn)}} .destructive{{border-left:4px solid var(--bad)}}
button{{background:#333;border:1px solid #777;color:#fff;padding:9px 12px;cursor:pointer;font-weight:600}} button:hover{{background:#444}} button.running{{background:#203c5f;border-color:var(--blue)}}
#output{{background:#070707;border:1px solid #444;padding:12px;min-height:260px;max-height:60vh;overflow:auto;white-space:pre-wrap;font-family:Consolas,monospace;font-size:13px}}
.toolbar{{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}} .status{{color:var(--muted)}} .error{{color:var(--bad)}} .success{{color:var(--ok)}} .small{{font-size:12px;color:var(--muted)}}
.details,.metric-grid{{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:10px}} code{{background:#111;border:1px solid #333;padding:1px 4px}}
.metric{{background:#222;border:1px solid #444;padding:12px}} .metric strong{{font-size:24px;display:block}}
table{{width:100%;border-collapse:collapse;font-size:13px}} th,td{{border-bottom:1px solid #333;padding:7px;text-align:left;vertical-align:top}} th{{color:#ddd;background:#202020;position:sticky;top:108px}}
.badge{{display:inline-block;border:1px solid #555;padding:2px 6px;border-radius:10px;background:#222}} .completed{{color:var(--ok)}} .in_progress{{color:var(--blue)}} .review{{color:var(--review)}} .planned{{color:#aaa}} .blocked{{color:var(--bad)}}
input[type="search"]{{width:100%;box-sizing:border-box;background:#111;color:#eee;border:1px solid #555;padding:9px;margin:8px 0 12px}}
</style>
</head>
<body>
<header>
  <h1>The Mechanist Project Dashboard</h1>
  <p>Local-only command center bound to <code>127.0.0.1</code>. Whitelisted commands only.</p>
  <div class="toolbar"><button id="refresh-status">Refresh Data</button><button id="clear-output">Clear Output</button><span class="status" id="status">Ready.</span></div>
  <nav class="tabs">
    <button class="tab-button" data-tab="overview" data-active="true">Overview</button>
    <button class="tab-button" data-tab="commands">Commands</button>
    <button class="tab-button" data-tab="milestones">Milestones</button>
    <button class="tab-button" data-tab="docs">Documents</button>
    <button class="tab-button" data-tab="history">Development History</button>
    <button class="tab-button" data-tab="release">Release</button>
    <button class="tab-button" data-tab="output">Output</button>
  </nav>
</header>
<main>
  <div class="tab" data-panel="overview" data-active="true">
    <section><h2>Milestone Tracker</h2><div class="metric-grid" id="milestone-metrics"></div></section>
    <section><h2>Current Repository</h2><div class="details"><p>Repo root:<br><code>{html.escape(str(REPO_ROOT))}</code></p><p>Python:<br><code>{html.escape(PYTHON)}</code></p><p>ROOT_RELEASE:<br><code>{html.escape(str(RELEASE_ROOT))}</code></p></div><p class="small">Run dry-run commands first after large asset changes. The dashboard does not bypass Git review or commits.</p></section>
  </div>
  <div class="tab" data-panel="commands">{command_groups_html()}<section><h2>Registered Command JSON</h2><pre>{html.escape(tool_json)}</pre></section></div>
  <div class="tab" data-panel="milestones"><section><h2>Tracked Milestones</h2><div id="milestone-table"></div></section></div>
  <div class="tab" data-panel="docs"><section><h2>Document Index</h2><input type="search" id="doc-search" placeholder="Filter documents..."><div id="doc-table"></div></section></div>
  <div class="tab" data-panel="history"><section><h2>Development History / Briefing Documents</h2><div id="history-table"></div></section></div>
  <div class="tab" data-panel="release"><section><h2>Master Release Staging</h2><p>This is currently a scaffold. It creates <code>ROOT_RELEASE</code> and records the future command plan, but does not pretend the final installer/client/server release builder is complete.</p><div class="cards"><article class="card safe"><h3>Dry Run Release Scaffold</h3><p>Show the planned ROOT_RELEASE structure.</p><button data-command="release_scaffold_dry" data-danger="safe">Run</button></article><article class="card writes"><h3>Create ROOT_RELEASE Scaffold</h3><p>Create release directories and scaffold manifest.</p><button data-command="release_scaffold_run" data-danger="writes">Run</button></article></div><h3>Status</h3><pre id="release-status"></pre></section></div>
  <div class="tab" data-panel="output"><section><h2>Command Output</h2><pre id="output">No command has been run yet.</pre></section></div>
</main>
<script>
const statusEl=document.getElementById('status'); const outputEl=document.getElementById('output'); let dashboardData={{}};
function esc(s){{return String(s??'').replace(/[&<>"]/g,c=>({{'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}}[c]));}}
function setTab(name){{document.querySelectorAll('.tab-button').forEach(b=>b.dataset.active=String(b.dataset.tab===name));document.querySelectorAll('.tab').forEach(p=>p.dataset.active=String(p.dataset.panel===name));}}
document.querySelectorAll('.tab-button').forEach(b=>b.addEventListener('click',()=>setTab(b.dataset.tab)));
async function loadStatus(){{const r=await fetch('/api/status'); dashboardData=await r.json(); renderData(); statusEl.textContent='Data refreshed.'; statusEl.className='status success';}}
function renderData(){{renderMilestones();renderDocs();renderHistory();document.getElementById('release-status').textContent=JSON.stringify(dashboardData.release,null,2);}}
function renderMilestones(){{const m=dashboardData.milestones||{{counts:{{}},milestones:[]}}; const counts=m.counts||{{}}; document.getElementById('milestone-metrics').innerHTML=['completed','in_progress','review','planned','blocked'].map(k=>`<div class="metric"><strong class="${{k}}">${{counts[k]||0}}</strong>${{k.replace('_',' ')}}</div>`).join(''); let rows=(m.milestones||[]).map(x=>`<tr><td><span class="badge ${{esc(x.status)}}">${{esc(x.status)}}</span></td><td><strong>${{esc(x.title)}}</strong><br><span class="small">${{esc(x.id)}} / ${{esc(x.area)}}</span></td><td>${{esc(x.summary)}}</td><td>${{esc(x.review_notes)}}</td></tr>`).join(''); document.getElementById('milestone-table').innerHTML=`<table><thead><tr><th>Status</th><th>Milestone</th><th>Summary</th><th>Review Notes</th></tr></thead><tbody>${{rows}}</tbody></table>`;}}
function docRows(filter=''){{const docs=(dashboardData.documents&&dashboardData.documents.documents)||[]; const f=filter.toLowerCase(); return docs.filter(d=>!f||JSON.stringify(d).toLowerCase().includes(f));}}
function renderDocs(){{const input=document.getElementById('doc-search'); const rows=docRows(input.value).map(d=>`<tr><td><code>${{esc(d.path)}}</code></td><td>${{esc(d.title)}}</td><td><span class="badge">${{esc(d.category)}}</span></td><td>${{esc(d.preview)}}</td></tr>`).join(''); document.getElementById('doc-table').innerHTML=`<table><thead><tr><th>Path</th><th>Title</th><th>Category</th><th>Preview</th></tr></thead><tbody>${{rows}}</tbody></table>`; input.oninput=()=>renderDocs();}}
function renderHistory(){{const docs=(dashboardData.documents&&dashboardData.documents.documents)||[]; const rows=docs.filter(d=>['development_history','milestone_or_roadmap'].includes(d.category)||String(d.path).toLowerCase().includes('history')||String(d.path).toLowerCase().includes('briefing')).map(d=>`<tr><td><code>${{esc(d.path)}}</code></td><td>${{esc(d.title)}}</td><td>${{esc(d.preview)}}</td></tr>`).join(''); document.getElementById('history-table').innerHTML=`<table><thead><tr><th>Path</th><th>Title</th><th>Preview</th></tr></thead><tbody>${{rows}}</tbody></table>`;}}
async function runCommand(id,button){{const danger=button.dataset.danger;if(danger==='writes'&&!confirm('This command writes files. Run it now?'))return;if(danger==='destructive'&&!confirm('This command deletes generated preview/audit files. Run it now?'))return;setTab('output');button.classList.add('running');button.disabled=true;statusEl.textContent='Running '+id+'...';outputEl.textContent='Running '+id+'...\n';try{{const r=await fetch('/api/run',{{method:'POST',headers:{{'Content-Type':'application/json'}},body:JSON.stringify({{id}})}});const data=await r.json();outputEl.textContent=data.output||'(no output)';statusEl.textContent=(data.ok?'Completed ':'Failed ')+id+' with exit code '+data.returncode+'.';statusEl.className='status '+(data.ok?'success':'error');await loadStatus();}}catch(e){{outputEl.textContent=String(e&&e.stack?e.stack:e);statusEl.textContent='Dashboard request failed.';statusEl.className='status error';}}finally{{button.classList.remove('running');button.disabled=false;}}}}
document.addEventListener('click',e=>{{const b=e.target.closest('button[data-command]'); if(b) runCommand(b.dataset.command,b);}});
document.getElementById('clear-output').addEventListener('click',()=>{{outputEl.textContent='';}});document.getElementById('refresh-status').addEventListener('click',loadStatus);
loadStatus();
</script>
</body>
</html>"""


class DashboardHandler(BaseHTTPRequestHandler):
    server_version = "MechanistToolDashboard/2.0"

    def log_message(self, fmt: str, *args: Any) -> None:
        print(f"[{self.log_date_time_string()}] {self.address_string()} {fmt % args}")

    def send_json(self, value: dict[str, Any], status: int = 200) -> None:
        payload = json.dumps(value, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self) -> None:  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path in {"/", "/index.html"}:
            payload = dashboard_html().encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(payload)
            return
        if parsed.path == "/api/status":
            self.send_json({
                "repo_root": str(REPO_ROOT),
                "root_tools": str(ROOT_TOOLS),
                "python": PYTHON,
                "commands": {key: asdict(value) | {"display_command": value.display_command()} for key, value in REGISTRY.items()},
                "history": RUN_HISTORY[-20:],
                "milestones": milestone_summary(),
                "documents": document_index(),
                "release": release_status(),
            })
            return
        self.send_json({"ok": False, "error": "not found"}, status=404)

    def do_POST(self) -> None:  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path != "/api/run":
            self.send_json({"ok": False, "error": "not found"}, status=404)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            body = self.rfile.read(length).decode("utf-8") if length else "{}"
            request = json.loads(body)
            command_id = str(request.get("id", ""))
            command = REGISTRY.get(command_id)
            if command is None:
                self.send_json({"ok": False, "error": f"unknown command id: {command_id}"}, status=400)
                return
            started = time.time()
            completed = subprocess.run(command.command, cwd=command.cwd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, shell=False, timeout=60 * 60)
            duration = round(time.time() - started, 3)
            record = {"id": command.id, "label": command.label, "returncode": completed.returncode, "duration_seconds": duration, "ok": completed.returncode == 0, "command": command.display_command()}
            RUN_HISTORY.append(record)
            self.send_json({**record, "output": completed.stdout or ""})
        except Exception:
            self.send_json({"ok": False, "error": traceback.format_exc(), "returncode": -1, "output": traceback.format_exc()}, status=500)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Start the local The Mechanist project dashboard.")
    parser.add_argument("--host", default=DEFAULT_HOST, help="Bind address. Default is 127.0.0.1. Do not expose this dashboard to a network.")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help="Bind port. Default: 8765")
    parser.add_argument("--no-browser", action="store_true", help="Do not open the browser automatically.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.host not in {"127.0.0.1", "localhost"}:
        print("Refusing to bind to a non-localhost address. This dashboard can run local commands.", file=sys.stderr)
        return 2
    server = ThreadingHTTPServer((args.host, args.port), DashboardHandler)
    url = f"http://{args.host}:{args.port}/"
    print(f"The Mechanist Project Dashboard running at {url}")
    print("Press Ctrl+C to stop.")
    if not args.no_browser:
        try:
            webbrowser.open(url)
        except Exception:
            pass
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping dashboard.")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""
Local-only tool dashboard for The Mechanist.

Starts a small localhost web dashboard with buttons for common ROOT_tools and
atlas pipeline commands. This is intentionally not a static web page: browsers
cannot safely execute local commands by themselves, so this script runs a
127.0.0.1-only HTTP server and exposes only a fixed whitelist of commands.

Run from the repository root:

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
        parts = []
        for item in self.command:
            if any(ch.isspace() for ch in item):
                parts.append(f'"{item}"')
            else:
                parts.append(item)
        return " ".join(parts)


def repo_path(*parts: str) -> str:
    return str(REPO_ROOT.joinpath(*parts))


def py_script(*parts: str) -> list[str]:
    return [PYTHON, repo_path(*parts)]


def build_registry() -> dict[str, ToolCommand]:
    commands = [
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
            danger="safe",
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
            danger="safe",
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
            danger="safe",
        ),
        ToolCommand(
            id="packager_automotive_run",
            label="Package Automotive Assets Only",
            group="Package Promotion",
            description="Focused promotion for automotive/vehicle asset slices.",
            command=py_script("ROOT_tools", "Compiled_asset_packager.py") + ["--include", "*automotive*", "--include", "*Automotive*", "--include", "*vehicle*", "--include", "*Vehicle*"],
            danger="writes",
        ),
    ]
    return {command.id: command for command in commands}


REGISTRY = build_registry()
RUN_HISTORY: list[dict[str, Any]] = []


def dashboard_html() -> str:
    groups: dict[str, list[ToolCommand]] = {}
    for command in REGISTRY.values():
        groups.setdefault(command.group, []).append(command)

    command_cards: list[str] = []
    for group_name, commands in groups.items():
        command_cards.append(f"<section><h2>{html.escape(group_name)}</h2><div class='cards'>")
        for command in commands:
            danger_class = html.escape(command.danger)
            command_cards.append(
                f"""
                <article class="card {danger_class}">
                  <h3>{html.escape(command.label)}</h3>
                  <p>{html.escape(command.description)}</p>
                  <pre>{html.escape(command.display_command())}</pre>
                  <button data-command="{html.escape(command.id)}" data-danger="{html.escape(command.danger)}">Run</button>
                </article>
                """
            )
        command_cards.append("</div></section>")

    tool_json = json.dumps({key: asdict(value) | {"display_command": value.display_command()} for key, value in REGISTRY.items()}, indent=2)

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>The Mechanist Tool Dashboard</title>
<style>
:root{{color-scheme:dark;--bg:#151515;--panel:#222;--line:#444;--text:#eee;--muted:#aaa;--ok:#5fc477;--warn:#e2bc54;--bad:#ff725c;--blue:#7eb7ff}}
body{{margin:0;background:var(--bg);color:var(--text);font-family:Segoe UI,Arial,sans-serif}}
header{{position:sticky;top:0;background:#101010;border-bottom:1px solid var(--line);padding:16px 24px;z-index:5}}
h1{{margin:0 0 6px;font-size:24px}}
header p{{margin:0;color:var(--muted)}}
main{{padding:20px 24px 36px;display:grid;gap:20px}}
section{{background:#1b1b1b;border:1px solid #333;padding:16px}}
h2{{margin:0 0 12px;font-size:18px}}
.cards{{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:12px}}
.card{{background:var(--panel);border:1px solid var(--line);padding:14px;display:flex;flex-direction:column;gap:8px}}
.card h3{{margin:0;font-size:16px}}
.card p{{margin:0;color:#ccc;line-height:1.35}}
.card pre{{white-space:pre-wrap;word-break:break-word;background:#111;border:1px solid #333;padding:8px;color:#ddd;font-size:12px;min-height:44px}}
.card.safe{{border-left:4px solid var(--ok)}}
.card.writes{{border-left:4px solid var(--warn)}}
.card.destructive{{border-left:4px solid var(--bad)}}
button{{background:#333;border:1px solid #777;color:#fff;padding:9px 12px;cursor:pointer;font-weight:600}}
button:hover{{background:#444}}
button.running{{background:#203c5f;border-color:var(--blue)}}
#output{{background:#070707;border:1px solid #444;padding:12px;min-height:280px;max-height:60vh;overflow:auto;white-space:pre-wrap;font-family:Consolas,monospace;font-size:13px}}
.toolbar{{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}}
.status{{color:var(--muted)}}
.warning{{color:var(--warn)}}
.error{{color:var(--bad)}}
.success{{color:var(--ok)}}
.small{{font-size:12px;color:var(--muted)}}
.details{{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:10px}}
code{{background:#111;border:1px solid #333;padding:1px 4px}}
</style>
</head>
<body>
<header>
  <h1>The Mechanist Tool Dashboard</h1>
  <p>Local-only command runner bound to <code>127.0.0.1</code>. Whitelisted commands only. No arbitrary shell execution.</p>
  <div class="toolbar">
    <button id="refresh-status">Refresh Status</button>
    <button id="clear-output">Clear Output</button>
    <span class="status" id="status">Ready.</span>
  </div>
</header>
<main>
  <section>
    <h2>Current Repository</h2>
    <div class="details">
      <p>Repo root:<br><code>{html.escape(str(REPO_ROOT))}</code></p>
      <p>Python:<br><code>{html.escape(PYTHON)}</code></p>
      <p>Dashboard script:<br><code>{html.escape(str(Path(__file__).resolve()))}</code></p>
    </div>
    <p class="small">Run dry-run commands first after large asset changes. The dashboard does not bypass version control; you still inspect and commit results manually.</p>
  </section>
  {''.join(command_cards)}
  <section>
    <h2>Command Output</h2>
    <pre id="output">No command has been run yet.</pre>
  </section>
  <section>
    <h2>Registered Command JSON</h2>
    <pre>{html.escape(tool_json)}</pre>
  </section>
</main>
<script>
const statusEl = document.getElementById('status');
const outputEl = document.getElementById('output');

async function runCommand(id, button) {{
  const danger = button.dataset.danger;
  if (danger === 'writes' && !confirm('This command writes files. Run it now?')) return;
  if (danger === 'destructive' && !confirm('This command deletes generated preview/audit files. Run it now?')) return;

  button.classList.add('running');
  button.disabled = true;
  statusEl.textContent = 'Running ' + id + '...';
  outputEl.textContent = 'Running ' + id + '...\n';

  try {{
    const response = await fetch('/api/run', {{
      method: 'POST',
      headers: {{'Content-Type': 'application/json'}},
      body: JSON.stringify({{id}})
    }});
    const data = await response.json();
    outputEl.textContent = data.output || '(no output)';
    if (data.ok) {{
      statusEl.textContent = 'Completed ' + id + ' with exit code ' + data.returncode + '.';
      statusEl.className = 'status success';
    }} else {{
      statusEl.textContent = 'Failed ' + id + ' with exit code ' + data.returncode + '.';
      statusEl.className = 'status error';
    }}
  }} catch (error) {{
    outputEl.textContent = String(error && error.stack ? error.stack : error);
    statusEl.textContent = 'Dashboard request failed.';
    statusEl.className = 'status error';
  }} finally {{
    button.classList.remove('running');
    button.disabled = false;
  }}
}}

document.querySelectorAll('button[data-command]').forEach(button => {{
  button.addEventListener('click', () => runCommand(button.dataset.command, button));
}});

document.getElementById('clear-output').addEventListener('click', () => {{ outputEl.textContent = ''; }});
document.getElementById('refresh-status').addEventListener('click', async () => {{
  const response = await fetch('/api/status');
  const data = await response.json();
  outputEl.textContent = JSON.stringify(data, null, 2);
  statusEl.textContent = 'Status refreshed.';
  statusEl.className = 'status';
}});
</script>
</body>
</html>
"""


class DashboardHandler(BaseHTTPRequestHandler):
    server_version = "MechanistToolDashboard/1.0"

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
            completed = subprocess.run(
                command.command,
                cwd=command.cwd,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                shell=False,
                timeout=60 * 60,
            )
            duration = round(time.time() - started, 3)
            output = completed.stdout or ""
            record = {
                "id": command.id,
                "label": command.label,
                "returncode": completed.returncode,
                "duration_seconds": duration,
                "ok": completed.returncode == 0,
                "command": command.display_command(),
            }
            RUN_HISTORY.append(record)
            self.send_json({
                **record,
                "output": output,
            })
        except Exception as exc:
            self.send_json({
                "ok": False,
                "error": str(exc),
                "traceback": traceback.format_exc(),
                "returncode": -1,
                "output": traceback.format_exc(),
            }, status=500)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Start the local The Mechanist tool dashboard.")
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
    print(f"The Mechanist Tool Dashboard running at {url}")
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

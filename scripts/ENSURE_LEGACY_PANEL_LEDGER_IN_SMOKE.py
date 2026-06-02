#!/usr/bin/env python3
"""Injects the legacy panel reference ledger builder into operations smoke."""
from __future__ import annotations
import argparse
from pathlib import Path
from typing import Optional, Sequence

ROOT = Path.cwd()
TARGET = ROOT / "scripts" / "SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1"
ANCHOR = "Write-Section 'Compile smoke'"
BLOCK = r'''
Write-Section 'Legacy panel reference ledger'
$legacyPanelExit = 999
if ($hasPython) {
    $legacyPanelExit = Run-ProcessCaptured 'Legacy panel reference ledger builder' 'py' @('-3', (Join-Path $root 'scripts\BUILD_LEGACY_PANEL_REFERENCE_LEDGER.py'), '--apply') (Join-Path $runRoot 'legacy_panel_reference_ledger.log') $CommandTimeoutSeconds
    if ($legacyPanelExit -eq 0) { Add-Gate 'INFO' 'legacy_panel_reference_ledger' 'pass' 'Legacy panel reference ledger regenerated.' } else { Add-Gate 'ERROR' 'legacy_panel_reference_ledger' 'fail' "Exit code $legacyPanelExit" }
} else {
    Add-Gate 'ERROR' 'legacy_panel_reference_ledger' 'skipped' 'py launcher missing; legacy panel reference ledger was not regenerated.'
}

'''

def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true')
    args = ap.parse_args(argv)
    text = TARGET.read_text(encoding='utf-8', errors='replace')
    if 'legacy_panel_reference_ledger' in text:
        print('Legacy panel reference ledger block already present.')
        return 0
    idx = text.find(ANCHOR)
    if idx < 0:
        raise SystemExit('Could not find compile smoke anchor.')
    updated = text[:idx] + BLOCK + text[idx:]
    print('Will insert legacy panel reference ledger block before compile smoke.')
    if not args.apply:
        print('Dry run only. Re-run with --apply to update the smoke script.')
        return 0
    TARGET.write_text(updated, encoding='utf-8', newline='\n')
    print('Updated', TARGET)
    return 0

if __name__ == '__main__':
    raise SystemExit(main())

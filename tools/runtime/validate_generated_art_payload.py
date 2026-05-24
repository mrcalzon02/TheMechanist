#!/usr/bin/env python3
"""Validate a generated-art payload root against the project runtime manifest."""
from __future__ import annotations
import argparse, csv, json
from pathlib import Path
TIERS=("high_native","intermediate_128","standard_64","low_32")

def candidate_paths(payload_root: Path, canonical_relative: str):
    rel=Path(canonical_relative)
    parts=rel.parts
    # canonical assets/graphics/generated/<tier>/<sheet>/<file>
    yield payload_root/rel
    if len(parts)>=5 and parts[0]=='assets' and parts[1]=='graphics' and parts[2]=='generated':
        tier=parts[3]
        after=Path(*parts[4:])
        yield payload_root/'exports'/tier/after
        yield payload_root/tier/after

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--project-root', required=True, type=Path)
    ap.add_argument('--payload-root', required=True, type=Path)
    ap.add_argument('--tiers', default=','.join(TIERS))
    ap.add_argument('--report', type=Path, default=None)
    args=ap.parse_args()
    tiers={t.strip() for t in args.tiers.split(',') if t.strip()}
    manifest=args.project_root/'assets/indexes/runtime_asset_manifest.json'
    data=json.loads(manifest.read_text(encoding='utf-8'))
    entries=data['entries']
    if isinstance(entries, dict): entries=entries.values()
    rows=[]; missing=0; present=0
    for e in entries:
        for tier in tiers:
            rel=e['tiers'][tier]
            found=next((p for p in candidate_paths(args.payload_root, rel) if p.is_file()), None)
            ok=found is not None
            if ok: present += 1
            else: missing += 1
            rows.append({'asset_id':e.get('asset_id',''),'tier':tier,'canonical_relative':rel,'present':str(ok).lower(),'resolved_path':str(found or '')})
    report=args.report or args.project_root/'logs/generated_art_payload_validation.csv'
    report.parent.mkdir(parents=True, exist_ok=True)
    with report.open('w', newline='', encoding='utf-8') as f:
        w=csv.DictWriter(f, fieldnames=['asset_id','tier','canonical_relative','present','resolved_path']); w.writeheader(); w.writerows(rows)
    print(f'payload={args.payload_root} present={present} missing={missing} report={report}')
    if missing: raise SystemExit(2)
if __name__=='__main__': main()

#!/usr/bin/env python3
"""Merge one or more generated-art payload/export roots into one canonical payload folder."""
from __future__ import annotations
import argparse, csv, os, shutil
from pathlib import Path
TIERS=("high_native","intermediate_128","standard_64","low_32")

def discover(source: Path, tiers: set[str]):
    roots=[]
    if (source/"assets/graphics/generated").is_dir(): roots.append(source/"assets/graphics/generated")
    if (source/"exports").is_dir(): roots.append(source/"exports")
    for root in roots:
        for tier in TIERS:
            if tier not in tiers: continue
            tier_root=root/tier
            if not tier_root.is_dir(): continue
            for png in sorted(tier_root.rglob("*.png")):
                rel_after_tier=png.relative_to(tier_root)
                if len(rel_after_tier.parts)<2: continue
                yield tier, png, Path("assets/graphics/generated")/tier/rel_after_tier

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--source', action='append', required=True, help='Payload/export source root. May be repeated.')
    ap.add_argument('--out', required=True, type=Path, help='Destination unified payload root.')
    ap.add_argument('--tiers', default=','.join(TIERS))
    ap.add_argument('--copy-mode', choices=('copy','link'), default='copy')
    args=ap.parse_args()
    tiers={t.strip() for t in args.tiers.split(',') if t.strip()}
    bad=tiers.difference(TIERS)
    if bad: raise SystemExit(f'Unknown tier(s): {sorted(bad)}')
    rows=[]; seen=set()
    for raw in args.source:
        source=Path(raw).resolve()
        for tier, src, rel in discover(source, tiers):
            dest=args.out/rel
            key=str(rel).replace('\\','/')
            if key in seen: continue
            seen.add(key)
            dest.parent.mkdir(parents=True, exist_ok=True)
            if args.copy_mode=='link':
                try: os.link(src,dest)
                except OSError: shutil.copy2(src,dest)
            else:
                shutil.copy2(src,dest)
            rows.append({'tier':tier,'source':str(src),'canonical_relative':key,'destination':str(dest)})
    report=args.out/'manifests/merge_report.csv'
    report.parent.mkdir(parents=True, exist_ok=True)
    with report.open('w', newline='', encoding='utf-8') as f:
        w=csv.DictWriter(f, fieldnames=['tier','source','canonical_relative','destination']); w.writeheader(); w.writerows(rows)
    print(f'merged files={len(rows)} out={args.out} report={report}')
if __name__=='__main__': main()

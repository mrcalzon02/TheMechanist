#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile
if len(sys.argv) < 2:
    print('Usage: validate_art_tier_zip.py <zip>')
    raise SystemExit(2)
p=Path(sys.argv[1])
with zipfile.ZipFile(p) as z:
    bad=z.testzip()
    pngs=[n for n in z.namelist() if n.lower().endswith('.png')]
    tiers=sorted({n.split('/')[3] for n in pngs if n.startswith('assets/graphics/generated/') and len(n.split('/'))>4})
if bad:
    print('FAIL', bad)
    raise SystemExit(1)
print(f'PASS {p.name} pngs={len(pngs)} tiers={tiers}')

import csv, hashlib, html, json, struct
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root=Path(__file__).resolve().parent
old=root.parent/'render-baseline-20261025/after'
rows=list(csv.DictReader((root/'captures.tsv').open(),delimiter='\t'))
previous={r['file']:r for r in csv.DictReader((old/'captures.tsv').open(),delimiter='\t')}
assert len(rows)==34
for row in rows:
    for key in ('seed','zone','floor','sewer','point','x','y','fov','yaw','pitch'):
        assert row[key]==previous[row['file']][key]
    with Image.open(root/row['file']) as im: assert im.size==(1280,800)
targets=list(csv.DictReader((root/'fixture-targets.tsv').open(),delimiter='\t'))
assert [r['target'] for r in targets]==['object Inspection workbench','object Floor salvage']
classes=list((root/'classes').rglob('*.class'))
majors=sorted({struct.unpack('>H',p.read_bytes()[6:8])[0] for p in classes})
assert max(majors)<=61
report={'status':'passed-focused-regressions-and-offscreen-captures','captureCount':36,
        'classfileMajors':majors,'targets':targets,
        'changes':['Shared +X-forward yaw convention for wall rays and sprite projection.',
                   'Floor-anchored upright sprites for map objects, NPCs, built objects and unrepresented tile fixtures.',
                   'Existing semantic art; simple primitive panels when art is missing; no new bitmap assets.',
                   'Screen-column wall-depth clipping; existing tile visibility retained before artwork lookup.',
                   'Examination uses the same billboard footprint and height; built and tile-only objects route through existing Look and interaction authorities.'],
        'tests':['Milestone02DoomHudFacingSmoke','FirstPersonHudLayoutAuthoritySmoke',
                 'LightingFalloffStreetLightSmoke','Milestone02RenderAssetBindingDoctrineSmoke','BillboardFixtureCapture'],
        'limits':['Full source/package build still blocked by the previously documented vehicle merge-marker and duplicate-local errors.',
                  'Uses the prior isolated diagnostic build with vehicle-only compile repairs and immutable semantic lookup caches; source changes compiled with --release 17.',
                  'Offscreen Java2D captures; no desktop automation. No runnable JAR, launcher or user save replaced.',
                  'Billboards are upright approximations using existing artwork, including top-down art; not volumetric models. Wall occlusion is per column, not a multi-level per-pixel depth buffer.',
                  'The existing world floor/ceiling projection and linear horizon pitch remain separate from billboard projection; steep-pitch world geometry and cross-level targeting are not repaired here.'],
        'captures':[{**r,'sha256':hashlib.sha256((root/r['file']).read_bytes()).hexdigest()} for r in rows]}
(root/'report.json').write_text(json.dumps(report,indent=2)+'\n')
font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',17)
cells=[('Previous sprites',old/'466713-insertion-spawn-doom.png'),
       ('Upright props and fixtures',root/'466713-insertion-spawn-doom.png'),
       ('Built object: L LOOK',root/'fixture-workbench.png'),
       ('Floor salvage: look down + L',root/'fixture-floor-salvage.png')]
sheet=Image.new('RGB',(1280,856),(12,16,14));d=ImageDraw.Draw(sheet)
for i,(label,path) in enumerate(cells):
    x=(i%2)*640;y=(i//2)*428
    d.text((x+10,y+6),label,font=font,fill=(224,195,120))
    with Image.open(path) as tile:sheet.paste(tile.resize((640,400),Image.Resampling.LANCZOS),(x,y+28))
sheet.save(root/'comparison.png')
esc=html.escape
parts=['''<!doctype html><meta charset="utf-8"><title>The Mechanist: floor billboards</title><style>
body{background:#0c100e;color:#d3d7cd;font:16px system-ui;max-width:1400px;margin:32px auto;padding:0 24px}h1,h2,summary{color:#dec278}p,li{line-height:1.6}a{color:#a3c9d1}img{width:100%}.pair{display:grid;grid-template-columns:1fr 1fr;gap:12px}details{border:1px solid #3b453b;padding:16px;margin:18px 0}figure{margin:0}figcaption{padding:8px 0}.note{background:#292017;padding:16px}@media(max-width:900px){.pair{grid-template-columns:1fr}}</style>
<h1>The Mechanist: floor-object billboards</h1><p>Existing game renderer and artwork · 34 matched seeded-world frames + 2 controlled fixture frames · offscreen Java 17 diagnostics.</p>
<p><a href="report.json">Structured evidence and limits</a> · <a href="fixture-targets.tsv">Verified reticle targets</a> · <a href="../render-baseline-20261025/index.html">Original baseline</a></p>
<img src="comparison.png" alt="Before and after billboards with workbench and floor-salvage examination">
<h2>Implemented</h2><ul>''']
parts += [f'<li>{esc(s)}</li>' for s in report['changes']]
parts += ['</ul><h2>Verification</h2><ul>']
parts += [f'<li>PASS: {esc(s)}</li>' for s in report['tests']]
parts += ['</ul><h2>Evidence limits</h2><div class="note"><ul>']
parts += [f'<li>{esc(s)}</li>' for s in report['limits']]
parts += ['</ul></div><h2>Matched seeded worlds</h2>']
for row in rows:
    if row['view']!='doom':continue
    name=row['file']
    title=f"{row['seed']} / {row['zone']} / floor {row['floor']}{'B' if row['sewer']=='true' else ''} / {row['point']} ({row['x']},{row['y']}) / FOV {row['fov']} / pitch {row['pitch']}"
    parts += [f'<details><summary>{esc(title)}</summary><div class="pair">']
    for label,prefix in [('Previous','../render-baseline-20261025/after/'),('Current','')]:
        parts += [f'<figure><a href="{prefix+name}"><img loading="lazy" src="{prefix+name}" alt="{label} {esc(title)}"></a><figcaption>{label}</figcaption></figure>']
    parts += ['</div></details>']
(root/'index.html').write_text(''.join(parts),encoding='utf-8')
old_index=root.parent/'render-baseline-20261025/index.html'
source=old_index.read_text(encoding='utf-8')
banner='<p><a href="../render-billboards-20261025/index.html">Latest: floor-object billboards and examination captures</a></p>'
if banner not in source:old_index.write_text(source.replace('<h1>',banner+'<h1>',1),encoding='utf-8')
print('PASS: 34 matched frames, 2 fixture targets, Java 17 classfiles; gallery written')

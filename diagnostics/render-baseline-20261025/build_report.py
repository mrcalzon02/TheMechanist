import csv, hashlib, html, json, statistics, struct, subprocess
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parent
repo = root.parent.parent
before = list(csv.DictReader((root/'captures.tsv').open(), delimiter='\t'))
after = list(csv.DictReader((root/'after/captures.tsv').open(), delimiter='\t'))
assert len(before) == len(after) == 34
assert {r['file'] for r in before} == {r['file'] for r in after}
for rows, folder in [(before, root), (after, root/'after')]:
    for row in rows:
        with Image.open(folder/row['file']) as im:
            assert im.size == (1280, 800)
classes = list((root/'classes').rglob('*.class')) + list((root/'updated-classes').rglob('*.class'))
majors = sorted({struct.unpack('>H', p.read_bytes()[6:8])[0] for p in classes})
assert max(majors) <= 61

limitations = [
    'The unmodified checkout fails to compile: unresolved merge markers and duplicate local declarations in VehicleStrategicTransitCommitAuthority.java, plus a duplicate destinationKey in Milestone06FactionVehicleRouteTransitCommitSmoke.java.',
    'All captures use current game sources compiled for Java 17 in an isolated diagnostic directory. The two vehicle files were repaired only in diagnostic copies, never in src/.',
    'Diagnostic copies of SemanticRenderAssetResolver and SemanticAssetHintResolver memoize immutable semantic lookup results. No renderer or asset-selection rule was changed by these caches. Timings are diagnostic observations, not untouched baseline or release performance.',
    'The existing fat jar supplied dependency classes and bundled assets on the classpath. Freshly compiled game classes take precedence. These are offscreen GamePanel.paint captures, not desktop screenshots or native GPU tests.',
    'All tested slices use the smallest supported 500 x 500 setup; other setup values remain standard. Spawn is the real startPoint. Street and room points are developer relocations to existing walkable, unoccupied tiles, not alternate player-start features.',
    'No packaged client/server jar, launcher, installer, or user save was replaced. Test storage is isolated. No desktop automation continued after the user requested exclusive desktop use.',
    'The semantic texture-variant cohort regression fails with both the original and diagnostic resolver: a lower-priority generic floor is admitted. This was not repaired in this pass.'
]
findings = [
    ('Existing mode', 'Doom Mode is already implemented in GameOptions, OptionsScreenPainter, DoomRuntimeOptionsSubsystem and FirstPersonRenderViewport. Default off, FOV 80, persisted bounds 60-110, linear/radial fog. No replacement mode was created.'),
    ('Existing game worlds', 'WorldAtlas owns seeded, lazy slices across 3 x 3 sectors, 3 x 3 zones, ten main floors and sewer layers. The default insertion is the neutral civilian floor at sector 1,1 / zone 2,2 / floor 4. Tested seeds: 466713, 20261025, 8675309.'),
    ('Existing rendering and art', 'Swing/Java2D paints authored semantic tile, floor, wall, portrait, fixture and interface artwork. Doom is a software column raycaster over the same World; LWJGL availability does not make this path an OpenGL renderer.'),
    ('Fixed: map visibility guide', 'Removed drawFacingVisionConeOverlay and its visual-only helper from LegacyPanelContext. Actual sensory visibility, illumination, exploration, movement previews, targeting and player indicators remain in place.'),
    ('Fixed: distant sprites', 'NPCs and map objects are rejected by GamePanel.isVisible before any portrait or semantic artwork lookup. Existing directional range, lighting and line-of-sight therefore gate sprites. Camera yaw now updates sensory facing while looking, without changing player coordinates.'),
    ('Observed: generation stalls', 'Generate World calls generateScaffold synchronously on the Swing event thread. An unaccelerated offscreen sample also showed full registry scans during tile descriptor compilation. The user-reported failure was not established as a JVM crash.'),
    ('Observed: render stalls', 'The original sprite pass processed over six thousand objects per slice and repeatedly searched semantic metadata. Matched captures show the distant horizon line disappearing after culling. Diagnostic cached Doom paint times still exceed a playable frame budget.'),
    ('Pending: shared projection', 'Static inspection finds wall rays use +X as forward at yaw zero while billboard projection uses +Z; horizontal projection also differs in aspect scaling. This pass does not redesign projection, target geometry or effects.'),
    ('Pending: multi-level depth', 'Current walls are one horizontal ray hit per screen column with a shifted horizon. Floor/ceiling are fixed planes; sprites lack shared per-pixel depth. Existing docs/DOOM_MULTILEVEL_RENDERING_PLAN.md describes the separate future multi-level work. Tile visibility culling does not provide partial sprite depth clipping.')
]
passes = ['Milestone02DoomHudFacingSmoke (including real wall/range visibility, remembered tiles, NPC/object culling and camera-facing regression)',
          'FirstPersonHudLayoutAuthoritySmoke', 'LightingFalloffStreetLightSmoke',
          'Milestone02RenderAssetBindingDoctrineSmoke', 'MultiFloorBuildingPlacementSmoke',
          'MultiFloorBuildingTransitSmoke', 'WorldGenerationSetupPersistenceSmoke']
report = {'date':'2026-10-25', 'timezone':'America/Anchorage',
          'commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=repo,text=True).strip(),
          'status':'diagnostic-captures-and-focused-regressions-complete; clean-source-build-blocked',
          'beforeCaptureCount':len(before), 'afterCaptureCount':len(after), 'classCountScanned':len(classes),
          'classfileMajors':majors, 'limitations':limitations, 'findings':dict(findings), 'passedChecks':passes,
          'captures':[]}
after_by_name = {r['file']:r for r in after}
for b in before:
    a=after_by_name[b['file']]
    for field in ('seed','zone','floor','sewer','point','x','y','glyph','fov','yaw','pitch','rooms','npcs','objects'):
        assert a[field] == b[field], (b['file'],field,a[field],b[field])
    report['captures'].append({'before':b,'after':a,
        'beforeSha256':hashlib.sha256((root/b['file']).read_bytes()).hexdigest(),
        'afterSha256':hashlib.sha256((root/'after'/a['file']).read_bytes()).hexdigest()})
(root/'report.json').write_text(json.dumps(report,indent=2)+'\n')

font_path=Path('C:/Windows/Fonts/consola.ttf')
font=ImageFont.truetype(str(font_path),17)
def sheet(name, cells, columns=2):
    cw,ch=640,428
    im=Image.new('RGB',(cw*columns,ch*((len(cells)+columns-1)//columns)),(13,15,14))
    d=ImageDraw.Draw(im)
    for index,(label,path) in enumerate(cells):
        x=(index%columns)*cw;y=(index//columns)*ch
        d.text((x+10,y+6),label,font=font,fill=(223,195,121))
        with Image.open(path) as tile:
            tile=tile.resize((640,400),Image.Resampling.LANCZOS)
            im.paste(tile,(x,y+28))
    im.save(root/name)
sheet('comparison.png',[(label,folder/file) for file,view in [
    ('466713-insertion-spawn-2d.png','2D'),('466713-insertion-spawn-doom.png','Doom')]
    for label,folder in [(view+' BEFORE',root),(view+' AFTER',root/'after')]])
sheet('worlds.png',[(f'{seed} / {slice_} / {view}',root/'after'/f'{seed}-{slice_}-spawn-{view}.png')
    for seed,slice_ in [(466713,'insertion'),(466713,'sewer'),(466713,'upper'),(20261025,'insertion'),(8675309,'insertion')]
    for view in ['2d','doom']])

esc=html.escape
parts=['''<!doctype html><html><head><meta charset="utf-8"><title>The Mechanist rendering baseline</title>
<style>body{background:#0c100e;color:#d3d7cd;font:16px system-ui;margin:32px auto;max-width:1400px;padding:0 24px}h1,h2,summary{color:#dec278}p,li{line-height:1.6}a{color:#a3c9d1}select{padding:9px;background:#19211c;color:#dec278;border:1px solid #716447;margin:8px}article,details{border:1px solid #3b453b;padding:16px;margin:18px 0;border-radius:8px}.pair{display:grid;grid-template-columns:1fr 1fr;gap:12px}img{width:100%;display:block}figcaption{padding:7px 0;color:#b2b8a8}figure{margin:0}.meta{font:13px monospace;color:#abbda9}table{border-collapse:collapse;width:100%}td{vertical-align:top;padding:10px;border-bottom:1px solid #303a31}td:first-child{color:#dec278;width:200px}.warning{background:#292017;padding:18px;border-left:4px solid #d7ab59}@media(max-width:900px){.pair{grid-template-columns:1fr}}</style></head><body>
<h1>The Mechanist: generated-world rendering baseline</h1>
<p>October 25, 2026 · 3 world seeds · 5 slices · 15 positions · 34 before + 34 after frames · 1280 × 800 · entirely offscreen.</p>
<p class="warning">The source checkout does not build as-is. These are isolated diagnostic builds with temporary vehicle compile repairs and pure semantic-lookup caches. The actual source changes are visibility culling, camera-facing synchronization, removal of the blue vision guide, and focused regression coverage. See the exact limits below.</p>
<p><a href="report.json">Structured report and image hashes</a> · <a href="comparison.png">Before/after contact sheet</a> · <a href="worlds.png">Five-slice contact sheet</a> · <a href="captures.tsv">Before coordinates</a> · <a href="after/captures.tsv">After coordinates</a></p>
<img src="comparison.png" alt="Matched before and after 2D and Doom views">
<h2>What exists and what changed</h2><table>''']
for title,body in findings:parts.append(f'<tr><td>{esc(title)}</td><td>{esc(body)}</td></tr>')
parts.append('</table><h2>Verification</h2><ul>')
for p in passes:parts.append(f'<li>PASS: {esc(p)}</li>')
parts.append(f'<li>PASS: {len(classes)} compiled classfiles scanned; majors {majors} (Java 17).</li>')
parts.append('<li>FAIL, pre-existing: semantic variant cohort; reproduced with the original resolver.</li></ul><h2>Evidence limits</h2><ul>')
for p in limitations:parts.append(f'<li>{esc(p)}</li>')
parts.append('''</ul><h2>All matched captures</h2><label>Seed <select id="seed"><option value="">All</option><option>466713</option><option>20261025</option><option>8675309</option></select></label><label>View <select id="view"><option value="">All</option><option value="2d">2D</option><option value="doom">Doom</option></select></label>''')
for index,b in enumerate(before):
    a=after_by_name[b['file']]
    title=f"{b['seed']} · {b['zone']} · floor {b['floor']}{'B' if b['sewer']=='true' else ''} · {b['point']} ({b['x']},{b['y']}) · {b['view']} · FOV {b['fov']} · pitch {b['pitch']}"
    parts.append(f'<details data-seed="{b["seed"]}" data-view="{b["view"]}" {"open" if index<2 else ""}><summary>{esc(title)}</summary><p class="meta">{b["rooms"]} rooms / {b["npcs"]} NPCs / {b["objects"]} objects in full slice. Developer point relocation where labeled street/room.</p><div class="pair">')
    for label,prefix,r in [('Before','',b),('After','after/',a)]:
        path=prefix+r['file']
        parts.append(f'<figure><a href="{path}"><img loading="lazy" src="{path}" alt="{esc(label+": "+title)}"></a><figcaption>{label} · diagnostic single paint {r["paintMs"]} ms</figcaption></figure>')
    parts.append('</div></details>')
parts.append('''<script>for(const s of document.querySelectorAll('select'))s.onchange=()=>{for(const d of document.querySelectorAll('details'))d.hidden=!!((seed.value&&d.dataset.seed!==seed.value)||(view.value&&d.dataset.view!==view.value));};</script></body></html>''')
(root/'index.html').write_text(''.join(parts),encoding='utf-8')
print(json.dumps({k:report[k] for k in ['status','beforeCaptureCount','afterCaptureCount','classCountScanned','classfileMajors']},indent=2))

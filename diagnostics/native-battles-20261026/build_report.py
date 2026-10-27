from pathlib import Path
import csv,json,hashlib,subprocess,shutil,zipfile,struct,html
folder=Path(__file__).resolve().parent
repo=folder.parents[1]
rows=list(csv.DictReader((folder/'results.csv').open()))
reference=list(csv.DictReader((folder/'results-reference.csv').open()))
assert [{k:v for k,v in r.items() if k!='coverAffectedAttacks'} for r in rows]==[{k:v for k,v in r.items() if k!='coverAffectedAttacks'} for r in reference], 'Encounter outcomes must repeat'
positions=json.loads((folder/'positions-reference.json').read_text())
checks=json.loads((folder/'regressions.json').read_text(encoding='utf-8-sig'))
assert len(checks)==16 and all(c['exitCode']==0 for c in checks)
assert len(rows)==4 and all('wins' in r['winner'] for r in rows)
ffmpeg=shutil.which('ffmpeg')
assert ffmpeg
videos=[]
for row in rows:
    case=folder/row['case']
    assert hashlib.sha256((case/'positions.tsv').read_bytes()).hexdigest()==positions[row['case']]
    sound=json.loads((case/'audio-summary.json').read_text())
    assert sound['cues']>0 and sound['generatedFallbacks']==0
    for view in ('2d','doom'):
        name=row['case']+'-'+view
        args=[ffmpeg,'-hide_banner','-loglevel','warning','-y','-framerate','6','-i',str(case/view/'%05d.png'),'-i',str(case/'battle.wav'),'-c:v','libx264','-preset','fast','-crf','22','-pix_fmt','yuv420p','-c:a','aac','-b:a','160k','-shortest','-movflags','+faststart',str(folder/(name+'.mp4'))]
        with (folder/(name+'-encode.log')).open('w') as log:subprocess.run(args,stdout=log,stderr=log,check=True)
        frame=sorted((case/view).glob('*.png'))[3]
        shutil.copyfile(frame,folder/(name+'-poster.png'))
        videos.append(dict(id=name,file=name+'.mp4',poster=name+'-poster.png',case=row['case'],view=view))
packages=[]
for name in ('TheMechanist-all.jar','TheMechanistServer-all.jar'):
    path=repo/'target'/name
    with path.open('rb') as f:sha=hashlib.file_digest(f,'sha256').hexdigest()
    with zipfile.ZipFile(path) as z:
        assert z.testzip() is None
        maximum=max(struct.unpack('>H',z.read(n)[6:8])[0] for n in z.namelist() if n.endswith('.class') and not n.startswith('META-INF/versions/'))
        assert maximum<=61
    packages.append(dict(name=name,sha256=sha,bytes=path.stat().st_size,maximumBaseClassVersion=maximum))
report=dict(startDate='2026-10-26',capture='Native GamePanel.paint / normal tile renderer and Doom software renderer; no substitute diagrams',audio='The same packaged cue decoder, positional stereo mixer and room reflections used live. Offline summation has an anti-clipping peak limiter; no desktop or microphone audio was captured.',settings=dict(seed=71000,coverSeed=81000,cyberneticChance=25,observerVisionStat=42,visionRangeCap=14,doomFov=110,worldZoomPercent=100,videoFps=6,turnDurationMs=500),results=rows,videos=videos,checks=checks,packages=packages,repeatability='All four outcomes and all recorded actor positions match the prior capture. Cover diagnostics now retain the exact modifier at attack time rather than inferring it from end-of-turn positions.',limits=['Four fixed-seed scenarios are regression examples, not balance certification.','Turn-based actors move on authoritative turns; the recording does not invent between-turn movement.','Normal directional vision, lighting and wall culling apply; offscreen actors remain culled.','Cover uses existing adjacent-object accuracy penalties; directional cover tactics and deliberate cover-seeking AI are not implemented.','No complete Gate 3, installer/native deployment certification or installed-client replacement claimed.','The known pre-existing paper-doll selection failure remains in the historical combat-execution logs.'])
names=['WeaponAudioAuthority.java','WeaponAudioSmoke.java','ActorCombatAuthority.java','ActorCombatFeedbackAuthority.java','ActorCombatPresentationSmoke.java','NativeBattleCaptureProbe.java','DeveloperCoverLayoutAuthority.java','ObjectSemanticAssetAuthority.java','WorldPositionalSoundAuthority.java','AudioRuntimeFramework.java','FirstPerson3DFramework.java']
report['lastValidatedDate']='2026-10-27'
report['weaponAudioAudit']={'result':(folder/'weapon-audio/results.txt').read_text().strip(),'bindings':'weapon-audio/weapon-bindings.tsv','pcm':'weapon-audio/pcm-audit.tsv','listeningPage':'weapon-audio/index.html'}
report['sourceSha256']={n:hashlib.sha256((repo/'src/mechanist'/n).read_bytes()).hexdigest() for n in names}
(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n')
table=''.join('<tr>'+''.join('<td>'+html.escape(r[k])+'</td>' for k in ['case','cover','turns','winner','movementSteps','movingActors','coverAffectedAttacks','audioCues'])+'</tr>' for r in rows)
links=''.join('<li><a href="'+c['log']+'">'+c['test']+'</a>: passed</li>' for c in checks)
data=''.join('<li>'+r['case']+': <a href="'+r['case']+'/positions.tsv">positions</a> / <a href="'+r['case']+'/events.tsv">attack events</a> / <a href="'+r['case']+'/cover.tsv">seeded cover and asset bindings</a> / <a href="'+r['case']+'/audio-events.tsv">audio cues and source WAVs</a></li>' for r in rows)
doc='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>The Mechanist - Native battle recordings</title>
<style>:root{color-scheme:dark}body{max-width:1400px;margin:25px auto;padding:0 22px;background:#10171b;color:#e7e9df;font:16px system-ui}h1{font-size:32px}p{line-height:1.5;color:#c4cfca}a{color:#a4d5ef}select,button{font:inherit;padding:9px;background:#263a40;color:#f1efe1;border:1px solid #718a8d;border-radius:4px}.bar{display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin:18px 0}video{width:100%;display:block;background:#070a0b;border:1px solid #455653}.notice{padding:12px 16px;border-left:4px solid #afbd7a;background:#213029}table{border-collapse:collapse;width:100%;font-size:14px}td,th{padding:8px;border-bottom:1px solid #3b4b4e;text-align:left}li{margin:8px 0}</style>
<h1>The Mechanist - Native battle recordings</h1><p>The game's rendered views, artwork, effects and positional sound. Four seeded fights, each captured in normal tile view and Doom view.</p>
<div class="notice">These videos come directly from GamePanel rendering. No diagram substitutes. Sound uses the packaged WAV assets through the same stereo mixer and room reflections as live gameplay.</div>
<div class="bar"><label>Recording <select id="recording"></select></label><button id="tiles">Normal tile view</button><button id="doom">Doom view</button><a id="download">Open video</a></div>
<video id="movie" controls playsinline preload="metadata"></video>
<p>Press Play and use the player's volume control for sound. Battles advance one real game turn every half-second in the recording. Normal lighting, directional visibility and occlusion remain active. The isolated observer has Vision 42, using the existing capped range of 14; normal tile zoom is 100%.</p>
<p><a href="weapon-audio/index.html">Listen to the existing weapon sound library and inspect the corrected family bindings</a></p><h2>What changed</h2><p>NPC attacks now trigger actual weapon cues and shared particle effects. Blocked firing lanes route around occupants rather than waiting indefinitely. Fresh Doom viewports respect the observer's facing. Sandbag and barricade bindings now point to visually checked defense artwork, replacing the unrelated hardware selected by a legacy asset ID.</p>
<h2>Recorded outcomes</h2><div style="overflow:auto"><table><thead><tr><th>Scenario</th><th>Cover objects</th><th>Turns</th><th>Outcome</th><th>Tile steps</th><th>Moving actors</th><th>Cover-affected attacks</th><th>Audio cues</th></tr></thead><tbody>TABLE</tbody></table></div>
<p>Cover consists of seeded existing Sandbag Line and Scrap Barricade buildables. Current protection is an adjacent-object accuracy penalty; directional cover tactics and deliberate cover-seeking AI remain future work. These examples do not establish balance.</p>
<h2>Reproducible evidence</h2><p><a href="report.json">Build hashes, source hashes and validation limits</a> / <a href="results.csv">Results</a> / <a href="capture.ps1">Native capture command</a> / <a href="run-tests.ps1">Focused checks</a> / <a href="../test-batches-20261026.json">Batch ledger</a></p><ul>DATA</ul>
<h2>Packaged Java 17 checks</h2><ul>CHECKS</ul><p>All 16 focused entry points passed. The complete Gate 3 and installer certification were not run; the installed distribution was not replaced. Historical paper-doll selection failure remains recorded.</p>
<p>Live arena: <code>/devroom</code>, <code>/devroom cover 16 81000</code>, <code>/devroom match 20 mixed 71000</code>, then <code>/devroom watch</code>. The cover and watch controls are also on the combat console.</p>
<script>const videos=VIDEOS;const s=document.getElementById('recording'),v=document.getElementById('movie'),d=document.getElementById('download');for(const x of videos){let o=document.createElement('option');o.value=x.id;o.textContent=x.case+' / '+x.view;s.append(o)}function choose(id){const x=videos.find(x=>x.id===id)||videos[0];s.value=x.id;v.src=x.file;v.poster=x.poster;d.href=x.file;v.load()}s.onchange=()=>choose(s.value);for(const mode of ['tiles','doom'])document.getElementById(mode).onclick=()=>{const x=videos.find(x=>x.id===s.value);choose(x.case+'-'+(mode==='tiles'?'2d':'doom'))};choose('20v20-rifles-cover-doom');</script></html>'''
doc=doc.replace('TABLE',table).replace('DATA',data).replace('CHECKS',links).replace('VIDEOS',json.dumps(videos))
(folder/'index.html').write_text(doc,encoding='utf-8')
print(json.dumps(dict(cases=len(rows),videos=len(videos),checks=len(checks),packages=packages),indent=2))

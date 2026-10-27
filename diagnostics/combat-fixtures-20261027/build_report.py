"""Publish reproducible fixture test evidence and native GamePanel captures."""
import csv, hashlib, html, json, struct, zipfile
from pathlib import Path
root=Path(__file__).resolve().parents[2]
out=Path(__file__).resolve().parent
rows=list(csv.DictReader((out/'attacks.tsv').open(),delimiter='\t'))
regressions=json.loads((out/'regressions.json').read_text(encoding='utf-8-sig'))
assert len(rows)==486 and all(r['exitCode']==0 for r in regressions)
packages={}
for name in ['TheMechanist-all.jar','TheMechanistServer-all.jar']:
    p=root/'target'/name
    with zipfile.ZipFile(p) as z:
        assert z.testzip() is None
        versions=[struct.unpack('>H',z.read(n)[6:8])[0] for n in z.namelist() if n.startswith('mechanist/') and n.endswith('.class')]
        assert max(versions)<=61
    packages[name]={'sha256':hashlib.file_digest(p.open('rb'),'sha256').hexdigest(),'maximumGameClassVersion':max(versions)}
report={'status':'passed','attacks':len(rows),'replayedAttacks':len(rows),'seeds':[91000,91001,91002],
 'checks':(out/'results.txt').read_text().strip(),'nativeViews':['tiles','doom'],
 'damagingAttacks':sum(int(r['reportedDamage'])>0 for r in rows),
 'noPenetrationAttacks':sum(int(r['reportedDamage'])==0 for r in rows),
 'regressions':regressions,'packages':packages,
 'limits':['Turrets remain passive; no autonomous power/ammo/crew/targeting execution.',
 'Vehicle-mounted firing and NPC-to-structure attacks are absent.',
 'Structural attacks select armor then frame; other component tests use explicit fault injection.',
 'Source clock and Doom movement-budget work remains separate; these tests do not certify hosted pacing.']}
report['sourceSha256']={p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((root/'src').rglob('*.java'))}
(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
labels={'suppression':'Arbites Suppression Turret','stub':'Heavy Stub Turret','sentry':'Noble Sentry Turret','powered':'Powered Defense Turret','bike':'Utility bike','car':'Civilian car','truck':'Cargo truck','armored-car':'Armored car','tank':'Armored crawler'}
cards=''.join(f'<article><h2>{label}</h2><a href="{key}-tiles.png"><img loading="lazy" src="{key}-tiles.png" alt="{label}, native tile view after firing"></a><a href="{key}-doom.png"><img loading="lazy" src="{key}-doom.png" alt="{label}, native Doom view after firing"></a></article>' for key,label in labels.items())
tr=''.join(f'<tr><td>{html.escape(r["test"])}</td><td>PASS</td><td><a href="{r["log"]}">log</a></td></tr>' for r in regressions)
page='''<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>The Mechanist — Combat fixtures</title><style>body{margin:2em auto;max-width:1200px;background:#11120f;color:#e4e0cf;font:16px/1.5 system-ui;padding:0 1em}h1,h2{color:#ddc483}a{color:#83cedd}article{margin:2em 0}img{width:49%;border:1px solid #594e35}code{color:#b7df99}table{border-collapse:collapse}td{padding:.3em 1em;border-bottom:1px solid #383a2e}.gap{border-left:4px solid #d89a60;padding:1em;background:#252016}</style><h1>The Mechanist: combat fixtures</h1><p>Four existing turret mounts and all five vehicle classes, executed through the game's structural combat authority. 486 attacks at three seeds, replayed identically: six weapons, three fire modes, nine targets. Native captures below use the actual GamePanel, tiles and Doom renderer, 50 ms after an executed Arc Rifle attack.</p><p>In the developer facility: <code>/devroom fixtures all 91000</code>. The combat console also offers turret-only, vehicle-only and clear controls. New actor matches retain the selection; clearing the arena removes test targets.</p><p>Verified finite ammunition, one-turn action costs, armor penetration, dry and blocked shot refusals, armor-to-chassis progression, wreck state, eight component states per vehicle, persisted component readback, seeded placement and isolated cleanup. Fixed final-hit damage overreporting and destroyed defenses retaining passive bonuses.</p><div class="gap"><b>Capability gaps:</b> turret mounts currently provide passive defenses, not live autonomous fire. Vehicle-mounted firing and NPC attacks on structures are absent. Other component-disable checks are explicit fault injection through the real vehicle authority, not aimed gunfire. These are gaps, not passing battle simulations.</div><p><a href="attacks.tsv">Attack records</a> · <a href="components.tsv">Component records</a> · <a href="capabilities.tsv">Capabilities and gaps</a> · <a href="results.txt">Assertions</a> · <a href="report.json">Package hashes and results</a> · <a href="run-tests.ps1">Rerun</a></p>'''+cards+'<p><a href="../native-battles-20261026/weapon-audio/variation/index.html">Weapon pitch, speed and start-time variation audition</a></p><h2>Packaged Java 17 regressions</h2><table>'+tr+'</table>'
(out/'index.html').write_text(page,encoding='utf-8')
print(json.dumps({'attacks':report['attacks'],'damaging':report['damagingAttacks'],'regressions':len(regressions),'packages':packages},indent=2))


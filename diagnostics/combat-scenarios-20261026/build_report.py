"""Validate recorded observations and embed a file://-compatible replay viewer."""
from pathlib import Path
import csv, json, hashlib
from collections import defaultdict

folder=Path(__file__).resolve().parent
rows=list(csv.DictReader((folder/'results.csv').open()))
first=list(csv.DictReader((folder/'first-run-results.csv').open()))
assert len(rows)==480 and len(first)==480
assert all({k:v for k,v in a.items() if k!='elapsedMs'}=={k:v for k,v in b.items() if k!='elapsedMs'} for a,b in zip(first,rows)), 'repeatability mismatch'
assert all(int(r['missingTicks'])==0 for r in rows), 'participant missed an update'
assert all(int(r['survivorsA'])==int(r['size'])==int(r['survivorsB']) for r in rows)
traces=[json.loads(p.read_text()) for p in sorted(folder.glob('*.json')) if p.name not in ('summary.json','player-confirm.json','report.json')]
assert len(traces)==24
assert all(len(t['frames'])==25 and len(t['frames'][0]['actors'])==t['size']*2 for t in traces)
summary=json.loads((folder/'summary.json').read_text())
player=json.loads((folder/'player-confirm.json').read_text())
group=defaultdict(list)
for row in rows:group[int(row['size'])].append(row)
table=''.join(f'<tr><td>{n} vs {n}</td><td>{len(rs)}</td><td>0</td><td>0</td><td>0</td><td>{sum(int(r["actorTicks"]) for r in rs):,}</td></tr>' for n,rs in sorted(group.items()))
report=dict(summary,playerConfirmation=player,repeatability='480/480 outcomes and update/movement counts identical on second run; elapsed time excluded',
            missingActorTicks=sum(int(r['missingTicks']) for r in rows),
            execution='New probe compiled directly with javac --release 17, ahead of prior packaged client on classpath. No combat runtime changes or new package build.',
            status='COMBAT_CAPABILITY_GAP: no damaging or resolved actor encounters',
            limitations=['120-turn windows are baseline observations, not win-rate estimates.',
                         'A/B are experiment labels; runtime has no team-targeting input. Factions are Imperial Guard and Cultist, with equal HP30 and quality tier2.',
                         'NPC combat skill does not exist; intellect2/12 is recorded separately, not called combat skill.',
                         'Scenario walls and placement are test setup. Movement, needs, target confirmation and rendering use actual game paths.',
                         'Replay is an omniscient visualization of sampled positions; game screenshots preserve actual visibility.',
                         'Headless observation, no manual desktop fight or balance certification.'])
repo=folder.parents[1]
report['sourceSha256']=hashlib.sha256((repo/'src/mechanist/CombatScenarioBaselineProbe.java').read_bytes()).hexdigest()
report['probeClassMajor']=int.from_bytes((folder/'classes/mechanist/CombatScenarioBaselineProbe.class').read_bytes()[6:8],'big')
assert report['probeClassMajor']==61
with (repo/'target/TheMechanist-all.jar').open('rb') as jar:report['clientJarSha256']=hashlib.file_digest(jar,'sha256').hexdigest()
(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n')
cards=''.join(f'<figure><a href="equal-knives-contact-{n}-turn-{turn}.png"><img loading="lazy" src="equal-knives-contact-{n}-turn-{turn}.png"></a><figcaption>{n}v{n}, actual game render, turn {turn}</figcaption></figure>' for n in (1,5,10,20) for turn in (1,120))
document='''<!doctype html><meta charset="utf-8"><title>The Mechanist — Combat baseline replays</title>
<style>body{margin:28px auto;max-width:1200px;padding:0 22px;background:#10171c;color:#e2e9e9;font:17px system-ui}a{color:#93d3ff}h1{font-size:32px}p{line-height:1.5}.notice{border-left:5px solid #ffb259;padding:12px 18px;background:#2d251d}button,select{font:inherit;background:#263844;color:white;padding:8px;border:1px solid #66808a;border-radius:4px}input{width:55%}canvas{width:100%;background:#19242c;border:1px solid #425660}table{border-collapse:collapse;width:100%;margin:20px 0}td,th{text-align:left;border-bottom:1px solid #44525b;padding:10px}section{display:grid;grid-template-columns:1fr 1fr;gap:20px}figure{margin:0}img{width:100%}figcaption{padding:8px;background:#263844}.blue{color:#7ccaff}.orange{color:#ffbb7c}code{overflow-wrap:anywhere}#state{min-height:48px}</style>
<h1>Combat baseline: 1v1 → 20v20</h1><p>2026-10-26 · The Mechanist · Recorded through the developer facility's actual <code>advanceTurnBody → NpcNeedsDutyRuntimeAuthority → NpcEntity.tick</code> path.</p>
<div class="notice"><strong>Observed result: no fight resolves.</strong> All 480 scenarios ended with every participant alive at full HP and no ammunition spent. The NPCs actively move, but hostile behavior pursues the player rather than selecting and attacking another NPC. This is a missing combat capability, not evidence that the weapons are balanced.</div>
<h2>Watch recorded movement</h2><p>This omniscient replay plots actual sampled actor positions, HP and loaded shots every five turns. It does not simulate damage or bypass game visibility in the game itself. <span class="blue">A = Imperial Guard</span> · <span class="orange">B = Cultist</span>. White diamond = player observer. Hover a participant for its readings.</p>
<p><select id="scenario"></select> <button id="play">Play</button> <button id="reset">Reset</button></p><p><input id="scrub" type="range" min="0" max="24" value="0"> <span id="turn"></span></p><canvas id="map" width="1150" height="610"></canvas><p id="state"></p><p id="kit"></p>
<h2>Matrix and outcomes</h2><p>Six scenarios × four team sizes × ten seeds (71000–71009) × two starting-side arrangements = 480 runs, each 120 turns. The entire matrix was repeated: all 480 outcome, update and movement records match, excluding elapsed time. Every participant was updated every turn: no scheduler starvation.</p>
<table><thead><tr><th>Teams</th><th>Runs</th><th>Resolved</th><th>HP lost</th><th>Ammo spent</th><th>Actor updates</th></tr></thead><tbody>'''+table+'''</tbody></table>
<ul><li>Equal knives, adjacent formation.</li><li>Equal Light Rifles, separated formation.</li><li>Light Rifles versus knives.</li><li>Stub pistols versus Heavy stubbers/Chainswords.</li><li>Identical rifles/armor with intellect 2 versus 12. NPC combat skill is not implemented; intellect is not a substitute skill.</li><li>Identical rifles separated by a wall.</li></ul>
<p>Both sides start at 30 HP, equipment tier 2, the same faction pair and equivalent formations. Seed and mirrored placement vary. Rifle sides use Guard flak vests; knife/pistol sides use civilian coats. Firearms start loaded with four reserve reloads. The probe supplies no attack rolls, damage, team AI, forced opponent movement or invented skills.</p>
<h2>Player confirmation check</h2><p>Ten real combat confirmations advanced '''+str(player['turnsAdvanced'])+''' turns. Target HP lost: '''+str(player['targetHpLost'])+'''. Loaded rifle rounds before/after: '''+str(player['loadedBefore'])+'/'+str(player['loadedAfter'])+'''. This agrees with the current handler: targeting text, event log and a turn advance, without actor damage.</p>
<h2>What must exist before win-rate tests are meaningful</h2><p>The runtime needs NPC enemy selection and engagement, an authoritative actor attack/damage/death path, weapon/ammunition handling for those attacks, and explicit combat skill inputs. Structural damage already has a separate authority; its weapon-force values were not repurposed into invented actor damage. Until these paths are connected, longer tournaments cannot establish weapon or skill balance.</p>
<p>Source evidence: <code>PopulationPersonnelFramework.java: NpcEntity.tick/tickAnimal</code>, <code>NpcNeedsDutyRuntimeAuthority.tick</code>, <code>LegacyPanelContext.java: confirmCombatTargetBody/advanceTurnBody</code>. This probe is compiled with Java 17 against the prior packaged client; no game combat behavior or distribution JAR was changed.</p>
<p><a href="results.csv">480 result rows</a> · <a href="report.json">Report</a> · <a href="probe.log">Execution log</a> · <a href="first-run-results.csv">First-run comparison</a> · <a href="../developer-facility-20261025/index.html">Developer facility</a></p>
<h2>Actual game captures</h2><p>Normal in-game rendering retains directional visibility. The replay above provides the full actor roster for measurement.</p><section>'''+cards+'''</section>
<script>const traces=TRACE_DATA;
const kits={'equal-knives-contact':'Both: Knife, civilian coat, intellect 5.','equal-rifles-range':'Both: Light Rifle, Knife, flak vest, intellect 5.','rifles-vs-knives':'A: Light Rifle/Knife/flak vest. B: Knife/civilian coat. Both intellect 5.','pistols-vs-heavy':'A: Stub pistol/Knife/civilian coat. B: Heavy stubber/Chainsword/flak vest. Both intellect 5.','intellect-2-vs-12':'Both: Light Rifle/Knife/flak vest. A intellect 2; B intellect 12. No combat skill field exists.','rifles-wall-separated':'Both: Light Rifle/Knife/flak vest; wall separates formations.'};
const sel=document.querySelector('#scenario'),scrub=document.querySelector('#scrub'),canvas=document.querySelector('#map'),ctx=canvas.getContext('2d');let timer=null,points=[];
traces.forEach((t,i)=>sel.add(new Option(t.scenario+' · '+t.size+' vs '+t.size,i)));
function draw(){let t=traces[+sel.value],f=t.frames[+scrub.value],scale=23,x0=18,y0=29;points=[];ctx.clearRect(0,0,1150,610);ctx.font='12px system-ui';
for(let x=18;x<=45;x++)for(let y=29;y<=53;y++){let px=(x-x0)*scale+30,py=(y-y0)*scale+20;ctx.strokeStyle='#2b3c46';ctx.strokeRect(px,py,scale,scale);if(t.wall&&x===27&&y>=34){ctx.fillStyle='#67717a';ctx.fillRect(px,py,scale,scale);}}
ctx.fillStyle='white';let ox=(27-x0)*scale+41.5,oy=(32-y0)*scale+31.5;ctx.beginPath();ctx.moveTo(ox,oy-8);ctx.lineTo(ox+8,oy);ctx.lineTo(ox,oy+8);ctx.lineTo(ox-8,oy);ctx.closePath();ctx.fill();
let hp=[0,0],ammo=[0,0];f.actors.forEach((a,i)=>{let side=i<t.size?0:1,x=(a[0]-x0)*scale+41.5,y=(a[1]-y0)*scale+31.5;hp[side]+=a[2];ammo[side]+=a[3];ctx.fillStyle=side?'#ffb06d':'#70c6ff';ctx.beginPath();ctx.arc(x,y,9,0,Math.PI*2);ctx.fill();ctx.fillStyle='#10171c';ctx.textAlign='center';ctx.fillText((i%t.size)+1,x,y+4);points.push({x,y,label:(side?'B':'A')+(i%t.size+1)+': HP '+a[2]+'; loaded '+a[3]+'; position '+a[0]+','+a[1]+'; last NPC tick '+a[4]});});
ctx.textAlign='left';ctx.fillStyle='#e2e9e9';ctx.font='20px system-ui';ctx.fillText('Turn '+f.turn+' / 120',730,55);ctx.fillStyle='#70c6ff';ctx.fillText('A: '+t.size+' alive · HP '+hp[0],730,100);ctx.fillStyle='#ffb06d';ctx.fillText('B: '+t.size+' alive · HP '+hp[1],730,140);ctx.fillStyle='#e2e9e9';ctx.fillText('Loaded shots A/B: '+ammo.join(' / '),730,185);ctx.fillText('Winner: none',730,230);ctx.font='16px system-ui';ctx.fillText('Runtime positions; no invented attacks',730,275);document.querySelector('#turn').textContent='Turn '+f.turn;document.querySelector('#state').textContent='Seed 71000 · original sides · '+t.size*2+' participants · 30 starting HP each';document.querySelector('#kit').textContent=kits[t.scenario];}
function stop(){clearInterval(timer);timer=null;document.querySelector('#play').textContent='Play';}
document.querySelector('#play').onclick=()=>{if(timer){stop();return;}if(+scrub.value===24)scrub.value=0;timer=setInterval(()=>{scrub.value=+scrub.value+1;draw();if(+scrub.value===24)stop();},240);document.querySelector('#play').textContent='Pause';};document.querySelector('#reset').onclick=()=>{stop();scrub.value=0;draw();};sel.onchange=()=>{stop();scrub.value=0;draw();};scrub.oninput=draw;canvas.onmousemove=e=>{let r=canvas.getBoundingClientRect(),x=(e.clientX-r.left)*canvas.width/r.width,y=(e.clientY-r.top)*canvas.height/r.height,p=points.find(p=>Math.hypot(p.x-x,p.y-y)<12);canvas.title=p?p.label:'';};draw();</script>'''
document=document.replace('TRACE_DATA',json.dumps(traces,separators=(',',':')))
(folder/'index.html').write_text(document,encoding='utf-8')
print('Gallery ready: 24 replays, 480 rows, repeatability verified; missing actor ticks = 0.')

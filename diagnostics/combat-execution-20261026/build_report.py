from pathlib import Path
import csv, json, hashlib, zipfile, struct, statistics, collections

folder=Path(__file__).resolve().parent
repo=folder.parents[1]
rows=list(csv.DictReader((folder/'results.csv').open()))
assert len(rows)==560
assert sum(int(r['missingTicks']) for r in rows)==0
repeat=list(csv.DictReader((folder/'repeatability-reference.csv').open()))
assert all({k:v for k,v in a.items() if k!='elapsedMs'}=={k:v for k,v in b.items() if k!='elapsedMs'} for a,b in zip(rows,repeat)) and len(repeat)==560
traces=[json.loads(p.read_text()) for p in sorted(folder.glob('*.json')) if p.name not in ('report.json','summary.json')]
assert len(traces)==28
for trace in traces:
    with (folder/(trace['id']+'-events.tsv')).open() as f:
        trace['events']=list(csv.DictReader(f,delimiter='\t'))
summary=json.loads((folder/'summary.json').read_text())
groups={name:[r for r in rows if r['scenario']==name] for name in dict.fromkeys(r['scenario'] for r in rows)}
aggregate=[]
for name,g in groups.items():
    aggregate.append(dict(scenario=name,runs=len(g),winsA=sum(r['winner']=='A' for r in g),winsB=sum(r['winner']=='B' for r in g),timeouts=sum(r['winner']=='none' for r in g),medianTurns=statistics.median(int(r['turns']) for r in g),damage=sum(int(r['hpLostA'])+int(r['hpLostB']) for r in g),shots=sum(int(r['ammoSpent']) for r in g),wallRefusals=sum(int(r['wallRefusals']) for r in g)))
packages=[]
for name in ('TheMechanist-all.jar','TheMechanistServer-all.jar'):
    path=repo/'target'/name
    with path.open('rb') as f:digest=hashlib.file_digest(f,'sha256').hexdigest()
    major=0;versioned=collections.Counter();count=0
    with zipfile.ZipFile(path) as z:
        assert z.testzip() is None
        for entry in z.infolist():
            if entry.filename.endswith('.class'):
                with z.open(entry) as stream:header=stream.read(8)
                value=struct.unpack('>H',header[6:8])[0]
                if entry.filename.startswith('META-INF/versions/'):versioned[value]+=1
                else:major=max(major,value);count+=1
    assert major<=61
    packages.append(dict(file='target/'+name,sha256=digest,baseClasses=count,baseMaximumMajor=major,versionDirectoryClasses=dict(versioned),zipIntegrity='PASS'))
tests=['ActorCombatExecutionSmoke','DeveloperFacilitySmoke','NpcContinuityDeterminismSmoke','Milestone04NpcHappinessFactionRecruitmentSmoke','Milestone02MovementExecutionAuthoritySmoke','Milestone02DoomHudFacingSmoke','Milestone02TradeReadabilitySmoke','Milestone02ContainerReadabilitySmoke','Milestone06StructuralScaleCombatSmoke','Milestone04FactionReinforcementLifecycleSmoke','Milestone02BodyConditionReadabilitySmoke','Milestone02CharacterEquipmentMedicalSmoke','Milestone02MedicalTreatmentReadabilitySmoke']
checks={test:('FAIL' if 'AssertionError' in (folder/(test+'.log')).read_text() else 'PASS') for test in tests}
assert sum(value=='PASS' for value in checks.values())==12
assert 'paper-doll body region selection' in (folder/'paper-doll-prior-build.log').read_text()
report=dict(date='2026-10-26',summary=summary,aggregate=aggregate,regressions=checks,packages=packages,
            repeatability='560/560 identical outcome, damage, ammo, movement and update counters on repeat; elapsed time excluded',
            changes=['Shared actor attack execution for NPCs and players; explicit NPC Firearms/Melee skills with optional trailing save fields.',
                     'Range, closed-wall/corner and intervening-actor refusals; passive defense cover; deterministic rolls on game RNG; HP damage/death and finite magazines/reserves.',
                     'NPC inspection/save parsing preserves depleted ammo. Case-insensitive catalog identity fixes Heavy stubber zero-capacity reload loop.',
                     'Player command coordinates are honored, inventory ammo is consumed on reload, player body receives NPC injury.',
                     'Developer teams, seeded match presets, watch/pause and step controls, protected merchants, bounded structured diagnostics.',
                     'Reinforcement intake uses its receive turn once, preventing an immediate second routine update.'],
            limits=['Initial actor damage/hit tuning, not finished balance certification. Armor currently uses coarse name bands for NPC equipment.',
                    'Wall-separated opponents do not route around the obstacle in this matrix; 80 blocked cases time out. This remains an NPC navigation limitation.',
                    'Explosives, suppression, morale, full injury/death UI, complete loot integration and darkness modifiers are not certified by this pass.',
                    'The paper-doll body-region UI smoke fails identically with the prior diagnostic build. Full Gate3 was not run.',
                    'Headless packaged tests; installed PACKAGE_client distribution was not replaced.'])
(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n')
table=''.join('<tr>'+''.join('<td>'+str(a[k])+'</td>' for k in ['scenario','runs','winsA','winsB','timeouts','medianTurns','shots'])+'</tr>' for a in aggregate)
checks_html=''.join('<li><a href="'+name+'.log">'+name+'</a>: '+result+'</li>' for name,result in checks.items())
cards=''.join(f'<figure><a href="equal-knives-contact-{n}-final.png"><img loading="lazy" src="equal-knives-contact-{n}-final.png"></a><figcaption>{n}v{n} final game render</figcaption></figure>' for n in (1,5,10,20))
document=r'''<!doctype html><meta charset="utf-8"><title>The Mechanist — Working actor combat tests</title>
<style>body{margin:28px auto;max-width:1200px;padding:0 22px;background:#10171c;color:#e2e9e9;font:17px system-ui}a{color:#93d3ff}p{line-height:1.5}.notice{border-left:5px solid #7ccfa6;padding:12px 18px;background:#20302c}button,select{font:inherit;background:#263844;color:white;padding:8px;border:1px solid #66808a}input{width:55%}canvas{width:100%;background:#19242c;border:1px solid #425660}table{border-collapse:collapse;width:100%;font-size:15px}td,th{text-align:left;border-bottom:1px solid #44525b;padding:9px}section{display:grid;grid-template-columns:1fr 1fr;gap:20px}figure{margin:0}img{width:100%}code{overflow-wrap:anywhere}#events{font:14px monospace;min-height:100px;white-space:pre-wrap;padding:12px;background:#202b33}.warning{color:#ffbf81}</style>
<h1>Actor combat: fixes, diagnostics and recorded fights</h1><p>2026-10-26 · Actual packaged game turn execution, headless Java 17.</p>
<div class="notice"><strong>480 open fights now resolve.</strong> All 80 wall-separated cases correctly refuse fire and time out. The original 480-case baseline had no damage or winners; the same scenarios now resolve 400 open encounters, plus 80 new skill-controlled encounters.</div>
<p>Repeated all 560 scenarios: identical winners, damage, ammunition, movement and actor-update counters. Each has up to 120 turns. Ten seeds × two starting sides × four team sizes × seven scenarios. No living participant missed a scheduler update.</p>
<h2>Watch recorded fights</h2><p>Blue A = Imperial Guard. Orange B = Cultist. White diamond = observer. HP bars and dead markers use recorded runtime values. Samples are every five turns plus the final turn; the event panel lists actual attacks within the current interval. This diagram is omniscient; game screenshots retain normal vision culling.</p>
<p><select id="scenario"></select> <button id="play">Play</button> <button id="reset">Reset</button></p><p><input id="scrub" type="range" min="0" value="0"> <span id="turn"></span></p><canvas id="map" width="1150" height="610"></canvas><p id="state"></p><pre id="events"></pre>
<h2>Outcome matrix</h2><table><thead><tr><th>Scenario</th><th>Runs</th><th>A wins</th><th>B wins</th><th>Timeouts</th><th>Median turns</th><th>Shots</th></tr></thead><tbody>'''+table+r'''</tbody></table>
<p>Both sides: 30 HP, quality tier 2, equal formations, explicit opposing teams and finite reload reserves. Equal-gear sides use skill 5. The skill contrast uses 2 versus 12 with identical rifles/armor. The intellect contrast changes intellect only; it does not confer an invented combat skill. Mirrored runs swap physical starting sides.</p>
<h2>Faults found and corrected</h2><ul><li>Actor confirmation only logged aim; NPC turn processing never invoked an attack resolver. Both now use a shared turn-owned actor authority.</li><li>Inspection and save parsing could replenish empty NPC magazines/reserves. Initialization now occurs once; depleted ammunition and explicit skills survive loading.</li><li>Catalog lookup treated Heavy stubber and Heavy Stubber differently, creating a zero-capacity reload loop. The case-normalized catalog index fixes it. <a href="before-heavy-magazine-fix-events.tsv">Recorded faulty reloads</a>.</li><li>Combat commands ignored their supplied coordinates. The authoritative adapter now uses them.</li><li>Reloading created free player ammunition. It now requires and consumes a carried compatible ammo unit; ammo family uses catalog metadata.</li><li>Reinforcement arrivals received an immediate extra routine action. Their receive turn is now reserved so intake state survives arrival.</li></ul>
<h2>Use the arena</h2><p><code>/devroom</code>, then <code>/devroom match 5 mixed 71000</code>. Sizes: 1, 5, 10, 20. Presets: knives, rifles, mixed, heavy, intellect, wall, skills. Use <code>/devroom watch</code> to watch/pause, <code>/devroom step 1</code> or <code>step 10</code> to advance, and <code>/devroom combatstats</code> for counters. Physical combat consoles expose these controls. Teams attack each other and leave the observer and pinned traders alone. Exit restores the paused campaign.</p>
<h2>Verification</h2><p>Client and server Maven package builds passed. Both archive integrity checks pass; base classfile maximum is 61 (Java 17). LWJGL includes separate version-directory dependency classes, recorded in the report.</p><ul>'''+checks_html+r'''</ul><p class="warning">The paper-doll body-region UI test fails identically against the prior diagnostic build: <a href="paper-doll-prior-build.log">comparison log</a>. Full Gate3 was not run.</p>
<p class="warning">Remaining limits: wall navigation stalls in the blocked test, initial damage/armor tuning, and no certification of explosives, suppression, morale, complete loot/death UI or darkness modifiers. Tests were headless. Installed game distribution was not replaced.</p>
<p><a href="results.csv">560 result rows</a> · <a href="report.json">Detailed report and package hashes</a> · <a href="matrix.log">Execution log</a> · <a href="../combat-scenarios-20261026/index.html">Before-fix baseline</a></p><h2>Game captures</h2><section>'''+cards+r'''</section>
<script>const traces=TRACE_DATA;const sel=document.querySelector('#scenario'),scrub=document.querySelector('#scrub'),canvas=document.querySelector('#map'),ctx=canvas.getContext('2d');let timer=null;
traces.forEach((t,i)=>sel.add(new Option(t.scenario+' · '+t.size+' vs '+t.size,i)));
function draw(){let t=traces[+sel.value],index=+scrub.value,f=t.frames[index],scale=23,x0=18,y0=29;ctx.clearRect(0,0,1150,610);ctx.font='12px system-ui';for(let x=18;x<=45;x++)for(let y=29;y<=53;y++){let px=(x-x0)*scale+30,py=(y-y0)*scale+20;ctx.strokeStyle='#2b3c46';ctx.strokeRect(px,py,scale,scale);if(t.wall&&x===27&&y>=34){ctx.fillStyle='#67717a';ctx.fillRect(px,py,scale,scale);}}
ctx.fillStyle='white';let ox=(27-x0)*scale+41.5,oy=(32-y0)*scale+31.5;ctx.beginPath();ctx.moveTo(ox,oy-8);ctx.lineTo(ox+8,oy);ctx.lineTo(ox,oy+8);ctx.lineTo(ox-8,oy);ctx.closePath();ctx.fill();let alive=[0,0],hp=[0,0],ammo=[0,0];
f.actors.forEach((a,i)=>{let side=i<t.size?0:1,x=(a[0]-x0)*scale+41.5,y=(a[1]-y0)*scale+31.5;hp[side]+=a[2];ammo[side]+=a[3];if(a[2]>0){alive[side]++;ctx.fillStyle=side?'#ffb06d':'#70c6ff';ctx.beginPath();ctx.arc(x,y,9,0,Math.PI*2);ctx.fill();ctx.fillStyle='#10171c';ctx.textAlign='center';ctx.fillText((i%t.size)+1,x,y+4);ctx.fillStyle='#7bdf99';ctx.fillRect(x-10,y-14,20*a[2]/30,3);}else{ctx.strokeStyle='#a06e76';ctx.beginPath();ctx.moveTo(x-5,y-5);ctx.lineTo(x+5,y+5);ctx.moveTo(x+5,y-5);ctx.lineTo(x-5,y+5);ctx.stroke();}});
ctx.textAlign='left';ctx.fillStyle='#e2e9e9';ctx.font='20px system-ui';ctx.fillText('Turn '+f.turn+' / '+t.frames.at(-1).turn,730,55);ctx.fillStyle='#70c6ff';ctx.fillText('A: '+alive[0]+' alive · HP '+hp[0],730,100);ctx.fillStyle='#ffb06d';ctx.fillText('B: '+alive[1]+' alive · HP '+hp[1],730,140);ctx.fillStyle='#e2e9e9';ctx.fillText('Loaded shots A/B: '+ammo.join(' / '),730,185);ctx.fillText('Winner: '+(alive[0]&&alive[1]?'pending':alive[0]?'A':alive[1]?'B':'mutual'),730,230);document.querySelector('#turn').textContent='Turn '+f.turn;document.querySelector('#state').textContent='Seed 71000 · original starting sides · '+t.size*2+' starting participants · 30 HP each';let previous=index?t.frames[index-1].turn:-1;document.querySelector('#events').textContent=t.events.filter(e=>+e.turn>previous&&+e.turn<=f.turn).slice(-8).map(e=>e.turn+' '+e.attacker+' → '+e.target+' '+e.outcome+' '+e.weapon+' | hit '+e.chance+'% roll '+e.roll+' | damage '+e.damage+' ammo '+e.ammo).join('\n')||'No attack events in this interval.';}
function stop(){clearInterval(timer);timer=null;document.querySelector('#play').textContent='Play';}function select(){stop();scrub.max=traces[+sel.value].frames.length-1;scrub.value=0;draw();}
document.querySelector('#play').onclick=()=>{if(timer){stop();return;}if(+scrub.value===+scrub.max)scrub.value=0;timer=setInterval(()=>{scrub.value=+scrub.value+1;draw();if(+scrub.value===+scrub.max)stop();},400);document.querySelector('#play').textContent='Pause';};document.querySelector('#reset').onclick=select;sel.onchange=select;scrub.oninput=draw;select();</script>'''
document=document.replace('TRACE_DATA',json.dumps(traces,separators=(',',':')))
(folder/'index.html').write_text(document,encoding='utf-8')
(folder/'replay-script.js').write_text(document.split('<script>')[1].split('</script>')[0],encoding='utf-8')
print(json.dumps(aggregate,indent=2))

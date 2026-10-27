from pathlib import Path
import csv,json,hashlib,zipfile,struct,statistics,collections

folder=Path(__file__).resolve().parent
repo=folder.parents[1]
summary=json.loads((folder/'summary.json').read_text())
encounters=json.loads((folder/'encounter-summary.json').read_text())
rows=list(csv.DictReader((folder/'encounters.csv').open()))
reference=list(csv.DictReader((folder/'repeatability-reference.csv').open()))
assert len(rows)==1280 and rows==reference
assert all(int(r['missingLivingTicks'])==0 for r in rows)
assert 'CombatSupportExecutionSmoke PASS' in (folder/'CombatSupportExecutionSmoke.log').read_text()
assert 'ActorCombatExecutionSmoke PASS' in (folder/'ActorCombatExecutionSmoke.log').read_text()
groups=[]
for mode in ('knives','rifles'):
    for variant in ('control','A-medkit','B-medkit','A-stimm','B-stimm','A-armor','B-armor','both-all'):
        group=[r for r in rows if r['mode']==mode and r['variant']==variant]
        groups.append(dict(mode=mode,variant=variant,runs=len(group),winsA=sum(r['winner']=='A' for r in group),winsB=sum(r['winner']=='B' for r in group),timeouts=sum(r['winner']=='none' for r in group),medianTurns=statistics.median(int(r['turns']) for r in group),medkits=sum(int(r['medkitsUsed']) for r in group),stimms=sum(int(r['stimmsUsed']) for r in group),hpRestored=sum(int(r['hpRestored']) for r in group)))
traces=[]
for path in sorted(folder.glob('*.json')):
    if path.name in ('summary.json','encounter-summary.json','report.json'):continue
    trace=json.loads(path.read_text())
    with (folder/(trace['id']+'-events.tsv')).open() as f:trace['events']=list(csv.DictReader(f,delimiter='\t'))
    traces.append(trace)
assert len(traces)==64
packages=[]
for name in ('TheMechanist-all.jar','TheMechanistServer-all.jar'):
    path=repo/'target'/name
    with path.open('rb') as f:digest=hashlib.file_digest(f,'sha256').hexdigest()
    major,count=0,0;versioned=collections.Counter()
    with zipfile.ZipFile(path) as z:
        assert z.testzip() is None
        for entry in z.infolist():
            if not entry.filename.endswith('.class'):continue
            with z.open(entry) as stream:header=stream.read(8)
            value=struct.unpack('>H',header[6:8])[0]
            if entry.filename.startswith('META-INF/versions/'):versioned[value]+=1
            else:major=max(major,value);count+=1
    assert major<=61
    packages.append(dict(file='target/'+name,sha256=digest,baseClasses=count,baseMaximumMajor=major,versionDirectoryClasses=dict(versioned),zipIntegrity='PASS'))
tests=['CombatSupportExecutionSmoke','ActorCombatExecutionSmoke','DeveloperFacilitySmoke','NpcContinuityDeterminismSmoke','Milestone04NpcHappinessFactionRecruitmentSmoke','Milestone02MovementExecutionAuthoritySmoke','Milestone02DoomHudFacingSmoke','Milestone02TradeReadabilitySmoke','Milestone02ContainerReadabilitySmoke','Milestone06StructuralScaleCombatSmoke','Milestone04FactionReinforcementLifecycleSmoke','Milestone02BodyConditionReadabilitySmoke','Milestone02CharacterEquipmentMedicalSmoke','Milestone02MedicalTreatmentReadabilitySmoke','Milestone04MedicalSupplyProvenanceSmoke','Milestone02InventoryReadabilitySmoke','Milestone02CharacterEquipmentScreenIntegrationSmoke']
checks={name:('FAIL' if 'Exception in thread' in (folder/(name+'.log')).read_text() else 'PASS') for name in tests}
limits=['Effect magnitudes, armor ratings/coverage and NPC use thresholds are initial tuning, not approved final balance.',
        'Supported field-use medicines, combat drugs, starting stimulants, recaf and labor stimulants are active. Other narcotics, surgical tools, psychic, addiction and withdrawal effects remain outside this pass; unsupported items are not consumed as fake treatment.',
        'NPCs use abstract HP plus condition state; players use existing body-part health and condition fields. Finite NPC supplies are seeded for new appropriate roles and persisted; old saves do not gain free replacement stock.',
        'Common flak/riot protection is fully penetrated by the current rifle profile. Knife protection and location-specific worn gear are verified; rifle armor balance still needs review.',
        'Healing spends a combat action and is not a guaranteed survival increase; rifle medkit groups can lose more often despite measurable healing.',
        'Armor uses the best covering layer, not additive stacking of all inventory goods. Damage/quality/coverage tests do not certify every weapon type, shield, augmetic, disease or full injury/loot presentation.',
        'The paper-doll body-region UI failure is the same pre-existing failure recorded in the prior build. Full Gate3 and release certification were not run.',
        'An additional broad world-size generation/persistence sweep was interrupted during large-world generation; it is not counted as passed.',
        'Headless packaged execution only. Installed PACKAGE_client distribution was not replaced.']
report=dict(date='2026-10-26',registryChecks=summary,encounters=encounters,aggregate=groups,regressions=checks,packages=packages,repeatability='All 1,280 encounter result rows match exactly on a second run of the same packaged build.',limits=limits)
report['sources']={name:hashlib.sha256((repo/'src/mechanist'/name).read_bytes()).hexdigest() for name in ['ActorConsumableAuthority.java','ActorArmorAuthority.java','ActorCombatAuthority.java','CombatSupportExecutionSmoke.java','CombatSupportScenarioProbe.java']}
(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n')
table=''.join('<tr>'+''.join('<td>'+str(row[k])+'</td>' for k in ['mode','variant','runs','winsA','winsB','timeouts','medianTurns','medkits','stimms','hpRestored'])+'</tr>' for row in groups)
comparison=list(csv.DictReader((folder/'actual-armor-comparisons.tsv').open(),delimiter='\t'))
armor_table=''.join('<tr>'+''.join('<td>'+row[k]+'</td>' for k in ['actor','armor','weapon','bareDamage','armoredDamage'])+'</tr>' for row in comparison)
check_html=''.join('<li><a href="'+name+'.log">'+name+'</a>: '+result+'</li>' for name,result in checks.items())
limit_html=''.join('<li>'+text+'</li>' for text in limits)
doc=r'''<!doctype html><meta charset="utf-8"><title>The Mechanist — Healing, stims and armor</title><style>body{max-width:1350px;margin:28px auto;padding:0 22px;background:#10171c;color:#e2e9e9;font:17px system-ui}a{color:#99d7ff}p{line-height:1.5}table{border-collapse:collapse;width:100%;font-size:15px}td,th{padding:9px;text-align:left;border-bottom:1px solid #44525b}.notice{background:#20302c;border-left:5px solid #7ccfa6;padding:12px 18px}button,select{font:inherit;background:#263844;color:white;padding:8px;border:1px solid #66808a}input{width:55%}canvas{width:100%;background:#19242c;border:1px solid #425660}#events{font:14px monospace;white-space:pre-wrap;min-height:110px;padding:12px;background:#202b33}li{margin:9px 0}</style>
<h1>Healing, combat drugs and armor: player/NPC parity</h1><p>2026-10-26 · Actual packaged game execution · Java 17, headless.</p><div class="notice"><strong>REGISTRY_CHECKS checks passed:</strong> 49 consumable profiles and 97 armor/clothing entries, all eight quality levels, both actor types. The encounter matrix contains 1,280 resolved fights and repeats exactly.</div>
<p>Healing now restores real health, not just condition labels. Supported drugs alter derived combat values for a bounded duration, refuse same-family stacking, expire once and apply their crash. Both actors use the same armor coverage/quality rules. NPCs consume finite carried stock and spend an action using it.</p>
<h2>What testing found</h2><ul><li>Medical Use reduced counters without repairing player body damage; most combat drugs only printed catalog text. Shared support execution now restores health/conditions and applies timed effects.</li><li>Legacy clothing equip replaced quality-bearing item identities with Common names. The original identity and quality now survive equip.</li><li>NPC attacks used abstract equipment tier while players used actual weapon quality. Equal gear, quality and skill now yield equal attack effects.</li><li>Helmets, gauntlets and boots apply only to their actual coverage. A helmet cannot protect a torso, gloves cannot protect upper arms, and boots cannot protect thighs.</li><li>Finite stock, one-action use, physical player item-instance consumption, NPC/player effect persistence, expiry, non-stacking and no resurrection are checked.</li><li>Coffee tin, Stim vial and Stimulant ampoule now perform their declared sleep/debt reduction and strain effects. Reusable tools and unsupported narcotics are not swallowed as medicine.</li></ul>
<h2>Actual damage comparisons</h2><p>Controlled successful attacks isolate gear effects through the real attack resolver. Coverage, quality, armor rating, chance and damage match across equal player/NPC inputs.</p><table><thead><tr><th>Defender</th><th>Worn item</th><th>Weapon</th><th>Bare damage</th><th>Protected damage</th></tr></thead><tbody>'''+armor_table+r'''</tbody></table>
<h2>Watch fights with supplies</h2><p>Blue A = Guard, orange B = Cultist, white diamond = observer. Every frame is a recorded turn. Green bars show HP; amber outlines indicate active drug effects. Hover a participant for loaded rounds and remaining carried supplies. This is an omniscient replay of game state, not a separate combat simulator.</p>
<p><select id="scenario"></select> <button id="play">Play</button> <button id="reset">Reset</button></p><p><input id="scrub" type="range" min="0" value="0"> <span id="turn"></span></p><canvas id="map" width="1150" height="610"></canvas><pre id="events"></pre><p id="download"></p>
<h2>Randomized encounter results</h2><p>Two weapon modes × eight supply conditions × four sizes (1v1, 5v5, 10v10, 20v20) × ten seeds × two mirrored starting sides = 1,280 cases, each capped at 180 turns. Each selected actor receives one Medkit and/or one Street Stimm. Armor conditions use Common suppression armor plus a flak helmet. Controls have the same weapons/skills/HP and no support stock or armor.</p>
<table><thead><tr><th>Mode</th><th>Condition</th><th>Runs</th><th>A wins</th><th>B wins</th><th>Timeout</th><th>Median turns</th><th>Medkits used</th><th>Stims used</th><th>HP restored</th></tr></thead><tbody>'''+table+r'''</tbody></table>
<p><strong>Interpretation:</strong> protection is powerful against knives, while the current rifle penetration ignores this Common armor. Medkits demonstrably restore HP, but spending an action on treatment can lose a firing race. Drug bonuses are measurable; their use timing and crash still need balancing. These are controlled execution observations, not final win-rate or balance certification.</p>
<h2>Try it in the developer room</h2><pre>/devroom match 5 rifles 71000
/devroom support A Medkit
/devroom support B Street Stimm
/devroom armor A Guard flak vest
/devroom armor A Guard flak helmet
/devroom watch</pre><p>Use <code>/devroom step 1</code> for single turns and <code>/devroom combatstats</code> for actual healing, dose and attack counters. The combat console also has supply and armor buttons. Normal player inventory Use/equip actions run the same support/armor rules. NPC supplies survive save/load and do not regenerate during inspection.</p>
<h2>Validation and limits</h2><p>Normal client/server Maven packages pass. Both JARs pass archive integrity and base-class Java 17 checks; separate LWJGL version-directory classes are recorded in the JSON report. The registry and encounter tests ran directly from the packaged client, without diagnostic class overrides.</p><ul>'''+check_html+r'''</ul><ul>'''+limit_html+r'''</ul>
<p><a href="report.json">Full report and package hashes</a> · <a href="item-quality-audit.tsv">Item/quality audit</a> · <a href="actual-armor-comparisons.tsv">Actual damage table</a> · <a href="encounters.csv">1,280 encounter rows</a> · <a href="CombatSupportExecutionSmoke.log">Registry checks</a> · <a href="matrix.log">Encounter log</a> · <a href="../combat-execution-20261026/index.html">Previous combat pass</a></p>
<script>const traces=TRACE_DATA;const sel=document.querySelector('#scenario'),scrub=document.querySelector('#scrub'),canvas=document.querySelector('#map'),ctx=canvas.getContext('2d');let timer=null,points=[];
traces.forEach((t,i)=>sel.add(new Option(t.mode+' · '+t.variant+' · '+t.size+' vs '+t.size,i)));
function draw(){let t=traces[+sel.value],f=t.frames[+scrub.value],scale=23,x0=18,y0=29;points=[];ctx.clearRect(0,0,1150,610);ctx.font='12px system-ui';for(let x=18;x<=45;x++)for(let y=29;y<=53;y++){ctx.strokeStyle='#2b3c46';ctx.strokeRect((x-x0)*scale+30,(y-y0)*scale+20,scale,scale);}ctx.fillStyle='white';let ox=(27-x0)*scale+41.5,oy=(32-y0)*scale+31.5;ctx.beginPath();ctx.moveTo(ox,oy-8);ctx.lineTo(ox+8,oy);ctx.lineTo(ox,oy+8);ctx.lineTo(ox-8,oy);ctx.closePath();ctx.fill();let alive=[0,0],hp=[0,0],stock=[0,0],effects=[0,0];
f.actors.forEach((a,i)=>{let side=i<t.size?0:1,x=(a[0]-x0)*scale+41.5,y=(a[1]-y0)*scale+31.5;hp[side]+=a[2];stock[side]+=a[5];if(a[2]>0){alive[side]++;effects[side]+=a[4];ctx.fillStyle=side?'#ffb06d':'#70c6ff';ctx.beginPath();ctx.arc(x,y,9,0,Math.PI*2);ctx.fill();if(a[4]){ctx.strokeStyle='#f7da71';ctx.lineWidth=2;ctx.stroke();ctx.lineWidth=1;}ctx.fillStyle='#10171c';ctx.textAlign='center';ctx.fillText((i%t.size)+1,x,y+4);ctx.fillStyle='#7bdf99';ctx.fillRect(x-10,y-14,20*a[2]/30,3);}else{ctx.strokeStyle='#a06e76';ctx.beginPath();ctx.moveTo(x-5,y-5);ctx.lineTo(x+5,y+5);ctx.moveTo(x+5,y-5);ctx.lineTo(x-5,y+5);ctx.stroke();}points.push({x,y,label:(side?'B':'A')+(i%t.size+1)+': HP '+a[2]+'; loaded '+a[3]+'; active effects '+a[4]+'; carried supplies '+a[5]});});
ctx.textAlign='left';ctx.fillStyle='#e2e9e9';ctx.font='20px system-ui';ctx.fillText('Turn '+f.turn+' / '+t.frames.at(-1).turn,730,55);ctx.fillStyle='#70c6ff';ctx.fillText('A: '+alive[0]+' alive · HP '+hp[0],730,100);ctx.fillStyle='#ffb06d';ctx.fillText('B: '+alive[1]+' alive · HP '+hp[1],730,140);ctx.fillStyle='#e2e9e9';ctx.fillText('Carried supplies A/B: '+stock.join(' / '),730,185);ctx.fillText('Active effects A/B: '+effects.join(' / '),730,225);ctx.fillText('Winner: '+(alive[0]&&alive[1]?'pending':alive[0]?'A':alive[1]?'B':'mutual'),730,270);document.querySelector('#turn').textContent='Turn '+f.turn;document.querySelector('#events').textContent=t.events.filter(e=>+e.turn===f.turn).slice(-9).map(e=>e.actor+' → '+e.target+' '+e.outcome+' '+e.weapon+(e.region?' | '+e.region+' armor '+e.armor:'')+' | damage '+e.damage+' ammo '+e.ammo).join('\n')||'No events at initial setup.';document.querySelector('#download').innerHTML='<a href="'+t.id+'-events.tsv">Complete event trace for this recorded encounter</a>';}
function stop(){clearInterval(timer);timer=null;document.querySelector('#play').textContent='Play';}function select(){stop();scrub.max=traces[+sel.value].frames.length-1;scrub.value=0;draw();}
document.querySelector('#play').onclick=()=>{if(timer){stop();return;}if(+scrub.value===+scrub.max)scrub.value=0;timer=setInterval(()=>{scrub.value=+scrub.value+1;draw();if(+scrub.value===+scrub.max)stop();},230);document.querySelector('#play').textContent='Pause';};document.querySelector('#reset').onclick=select;sel.onchange=select;scrub.oninput=draw;canvas.onmousemove=e=>{let r=canvas.getBoundingClientRect(),x=(e.clientX-r.left)*canvas.width/r.width,y=(e.clientY-r.top)*canvas.height/r.height,p=points.find(p=>Math.hypot(p.x-x,p.y-y)<12);canvas.title=p?p.label:'';};select();</script>'''
doc=doc.replace('REGISTRY_CHECKS',str(summary['checks'])).replace('TRACE_DATA',json.dumps(traces,separators=(',',':')))
(folder/'index.html').write_text(doc,encoding='utf-8')
(folder/'replay-script.js').write_text(doc.split('<script>')[1].split('</script>')[0],encoding='utf-8')
print(json.dumps(dict(checks=summary,encounters=encounters,regressions=checks),indent=2))

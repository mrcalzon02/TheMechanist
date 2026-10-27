from pathlib import Path
import csv,json,hashlib,zipfile,struct,html
folder=Path(__file__).resolve().parent
repo=folder.parents[1]
rows=list(csv.DictReader((folder/'battles.csv').open()))
traces=[json.loads((folder/(r['id']+'.json')).read_text()) for r in rows]
checks=json.loads((folder/'regressions.json').read_text(encoding='utf-8-sig'))
assert all(r['exitCode']==0 for r in checks)
assert len(rows)==24 and all(int(r['missingLivingTicks'])==0 for r in rows)
packages=[]
for name in ('TheMechanist-all.jar','TheMechanistServer-all.jar'):
    path=repo/'target'/name
    with path.open('rb') as f: sha=hashlib.file_digest(f,'sha256').hexdigest()
    with zipfile.ZipFile(path) as z:
        assert z.testzip() is None
        base=[n for n in z.namelist() if n.endswith('.class') and not n.startswith('META-INF/versions/')]
        maximum=max(struct.unpack('>H',z.read(n)[6:8])[0] for n in base)
        assert maximum<=61
    packages.append(dict(name=name,sha256=sha,baseClassMajor=maximum,bytes=path.stat().st_size))
report=dict(date='2026-10-26',runtime='Java 17',scope='Limb prosthetics and surgery; not the entire Milestone 08 roadmap',checks=checks,packages=packages,battles=len(rows),resolved=sum(r['winner']!='timeout' for r in rows),candidatePool={'chance':25,'fitted':499,'candidates':2000},limits=['One seed per battle configuration; functional regression evidence, not a balance conclusion.','Replays are omniscient diagrams of recorded state, not native game renderer captures.','Paper-doll selection has a separately recorded pre-existing failure.','Organic transplantation, neural/sensory implants, autonomous specialist surgery and full faction/social implant policy remain future work.','No full Gate 3, installer certification or installed-distribution replacement claimed.'])
report['sourceSha256']={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (repo/'src/mechanist').glob('Cybernetic*.java')}
(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n')
table=''.join('<tr>'+''.join('<td>'+html.escape(r[k])+'</td>' for k in ['id','fittedA','fittedB','turns','winner','damage','shots','healing','hardwareEvents'])+'</tr>' for r in rows)
logs=''.join('<li><a href="'+r['log']+'">'+r['test']+'</a>: exit '+str(r['exitCode'])+'</li>' for r in checks)
doc=r'''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>The Mechanist - Mass battle recordings</title>
<style>:root{color-scheme:dark}body{background:#10161c;color:#e4e9e9;font:16px system-ui;margin:24px auto;max-width:1400px;padding:0 22px}h1{font-size:34px;margin-bottom:8px}h2{font-size:22px}p{line-height:1.5;color:#c7d1d7}a{color:#9ed8f5}button,select{font:inherit;color:#e4e9e9;background:#263644;border:1px solid #627689;padding:8px;border-radius:5px}button{cursor:pointer}.controls{display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin:14px 0}.badge{background:#25392e;color:#a5e6b4;padding:9px 14px;border-left:3px solid #99d3ad}canvas{width:100%;background:#182129;border:1px solid #526574;border-radius:5px}.panels{display:grid;grid-template-columns:2fr 1fr;gap:16px}.panel{padding:14px;background:#19232d;border:1px solid #344756;border-radius:5px}#events,#inspect{white-space:pre-wrap;font:13px ui-monospace,monospace;line-height:1.55;max-height:230px;overflow:auto}#scrub{flex:1;min-width:190px}table{width:100%;border-collapse:collapse;font-size:13px}td,th{text-align:left;padding:7px;border-bottom:1px solid #354351}li{margin:7px 0}@media(max-width:800px){.panels{grid-template-columns:1fr}}.keyA{color:#8ac9ff}.keyB{color:#ff9a7a}</style>
<h1>The Mechanist - Mass battle recordings</h1><p>Actual packaged game turns - 26 October 2026 - 1v1, 5v5, 10v10 and 20v20 - Seed 71000</p>
<div class="badge">24 recorded encounters. All 24 resolved. No missed living participant updates. Cybernetic lifecycle: 7,429 assertions passed.</div>
<p>Playback visualizes recorded positions and combat events. The game executed attacks, ammunition use, healing, stims, armor and hardware damage. This is an omniscient diagram. Both teams carry finite support and augmetic service supplies.</p>
<div class="controls"><label>Battle <select id="battle"></select></label><button id="play">Play</button><button id="back">Previous</button><button id="step">Next turn</button><button id="restart">Restart</button><label>Speed <select id="speed"><option value="800">Slow</option><option value="350" selected>Normal</option><option value="120">Fast</option></select></label></div>
<div class="controls"><span id="turn"></span><input id="scrub" type="range" min="0" value="0" aria-label="Recorded turn"><span id="score"></span></div>
<canvas id="field" width="1320" height="590" aria-label="Recorded battle positions"></canvas>
<p><span class="keyA">Blue: A / Imperial Guard</span> - <span class="keyB">Red: B / Cultist</span> - Gold ring: cybernetic limb - Purple ring: active drug - Grey cross: dead. Click an actor to inspect it.</p>
<div class="panels"><section class="panel"><h2>Events this turn</h2><div id="events"></div></section><section class="panel"><h2>Selected actor</h2><div id="inspect">Click an actor in the arena.</div></section></div>
<h2>Results</h2><p>Fitted counts are random selections, not team quotas. One seed per configuration supports functional testing; it does not establish balance.</p><div style="overflow:auto"><table><thead><tr><th>Battle</th><th>Fitted A</th><th>Fitted B</th><th>Turns</th><th>Winner</th><th>Damage</th><th>Shots</th><th>Healing</th><th>Hardware hits</th></tr></thead><tbody>RESULT_TABLE</tbody></table></div>
<p><a href="battles.csv">Outcomes</a> - <a href="candidate-distribution.csv">2,000 candidate audit</a> - <a href="report.json">Validation and package hashes</a> - <a href="cybernetic-results.txt">Lifecycle checks</a> - <a href="run-tests.ps1">Rerun script</a> - <a href="medical-controls.png">Medical controls capture</a></p>
<h2>Focused packaged checks</h2><ul>TEST_LOGS</ul>
<p>Organic transplants, neural/sensory implants, autonomous specialist surgeons and full faction/social implant policy remain future work. Full Gate 3 and installer certification were not run.</p>
<p>Earlier evidence: <a href="../combat-scenarios-20261026/index.html">Original baseline</a> - <a href="../combat-execution-20261026/index.html">560 combat cases</a> - <a href="../combat-support-20261026/index.html">1,280 support encounters</a>. Earlier reports describe historical builds; later medical boundaries supersede their healing semantics.</p>
<script>
const traces=TRACE_DATA;
const $=id=>document.getElementById(id),canvas=$('field'),ctx=canvas.getContext('2d');let which=0,index=0,playing=false,last=0,selected='',points=[];
for(let i=0;i<traces.length;i++){let o=document.createElement('option');o.value=i;o.textContent=traces[i].id;$('battle').append(o)}
which=Math.max(0,traces.findIndex(t=>t.id==='20v20-rifles-cyber25'));$('battle').value=which;
function select(){index=0;playing=false;$('play').textContent='Play';$('scrub').max=traces[which].frames.length-1;draw()}
function draw(){let t=traces[which],f=t.frames[index];$('scrub').value=index;$('turn').textContent='Turn '+f.turn+' / '+t.frames.at(-1).turn;let a=f.actors.filter(n=>n.team==='A'&&n.hp>0).length,b=f.actors.filter(n=>n.team==='B'&&n.hp>0).length;$('score').textContent='A '+a+' / B '+b+(index===t.frames.length-1?' / '+t.winner+' wins':'');
ctx.clearRect(0,0,canvas.width,canvas.height);let all=t.frames.flatMap(f=>f.actors),xs=all.map(n=>n.x),ys=all.map(n=>n.y),minX=Math.min(...xs)-2,maxX=Math.max(...xs)+2,minY=Math.min(...ys)-2,maxY=Math.max(...ys)+2;let scale=Math.min(65,(canvas.width-90)/(maxX-minX+1),(canvas.height-65)/(maxY-minY+1));let ox=(canvas.width-(maxX-minX)*scale)/2,oy=(canvas.height-(maxY-minY)*scale)/2;const pos=n=>({x:ox+(n.x-minX)*scale,y:oy+(n.y-minY)*scale});
ctx.strokeStyle='#27343e';ctx.lineWidth=1;for(let x=minX;x<=maxX;x++)for(let y=minY;y<=maxY;y++){let p=pos({x,y});ctx.strokeRect(p.x-scale/2,p.y-scale/2,scale,scale)}ctx.font='14px system-ui';ctx.fillStyle='#9eabb7';ctx.fillText(t.id+' / recorded state / cybernetic chance '+t.chance+'%',18,25);
let byId=Object.fromEntries(f.actors.map(n=>[n.id,n]));for(let e of f.events){let from=byId[e.attacker],to=byId[e.target];if(!from||!to||from===to||!['HIT','KILL','MISS'].includes(e.outcome))continue;let p=pos(from),q=pos(to);ctx.strokeStyle=e.outcome==='MISS'?'#89949b70':e.outcome==='KILL'?'#ff615d':'#f2d279';ctx.lineWidth=2;ctx.setLineDash(e.outcome==='MISS'?[4,5]:[]);ctx.beginPath();ctx.moveTo(p.x,p.y);ctx.lineTo(q.x,q.y);ctx.stroke();ctx.setLineDash([])}
points=[];for(let n of f.actors){let p=pos(n),r=Math.min(18,scale*.31);points.push({...p,n,r});ctx.save();ctx.translate(p.x,p.y);if(n.hp<=0){ctx.strokeStyle='#778087';ctx.lineWidth=3;ctx.beginPath();ctx.moveTo(-7,-7);ctx.lineTo(7,7);ctx.moveTo(7,-7);ctx.lineTo(-7,7);ctx.stroke();ctx.restore();continue}if(n.drug){ctx.strokeStyle='#cb99ff';ctx.lineWidth=2;ctx.beginPath();ctx.arc(0,0,r+7,0,Math.PI*2);ctx.stroke()}if(n.limbs.length){ctx.strokeStyle=n.limbs.every(l=>l.online)?'#e1bc64':'#f85151';ctx.lineWidth=3;ctx.beginPath();ctx.arc(0,0,r+3,0,Math.PI*2);ctx.stroke()}ctx.fillStyle=n.team==='A'?'#72b6ed':'#e78e70';ctx.beginPath();ctx.arc(0,0,r,0,Math.PI*2);ctx.fill();ctx.fillStyle='#12202b';ctx.textAlign='center';ctx.font='bold 12px system-ui';ctx.fillText(n.name,0,4);ctx.fillStyle='#263238';ctx.fillRect(-r,r+11,r*2,4);ctx.fillStyle='#a6dfa9';ctx.fillRect(-r,r+11,r*2*n.hp/30,4);ctx.restore()}
$('events').textContent=f.events.map(e=>e.attacker+' -> '+e.target+' / '+e.outcome+' / '+e.weapon+(e.damage?' / '+e.damage+' damage':'')+(e.region?' / '+e.region:'')+(['HIT','KILL','MISS'].includes(e.outcome)?' / hit '+e.chance+'%, roll '+e.roll:'')).join('\n')||'No combat events in this recorded turn.';
let n=byId[selected];if(n)$('inspect').textContent=n.name+' / Team '+n.team+'\nHP '+n.hp+'/30 / loaded shots '+n.ammo+'\n'+n.weapon+'\nActive drug: '+n.drug+'\n'+(n.limbs.length?n.limbs.map(l=>l.item+' / '+l.root+'\nquality '+l.quality+' / '+(l.online?'online':'OFFLINE')+'\nintegrity '+l.integrity+'% / power '+l.power+'% / calibration '+l.calibration+'%').join('\n'):'No fitted cybernetic limb.');}
$('battle').onchange=()=>{which=+$('battle').value;select()};$('play').onclick=()=>{if(index===traces[which].frames.length-1)index=0;playing=!playing;$('play').textContent=playing?'Pause':'Play'};$('step').onclick=()=>{index=Math.min(index+1,traces[which].frames.length-1);draw()};$('back').onclick=()=>{index=Math.max(0,index-1);draw()};$('restart').onclick=select;$('scrub').oninput=()=>{index=+$('scrub').value;draw()};canvas.onclick=e=>{let r=canvas.getBoundingClientRect(),x=(e.clientX-r.left)*canvas.width/r.width,y=(e.clientY-r.top)*canvas.height/r.height;let n=points.find(p=>Math.hypot(x-p.x,y-p.y)<p.r+6);if(n){selected=n.n.id;draw()}};function loop(now){if(playing&&now-last>+$('speed').value){last=now;if(index<traces[which].frames.length-1){index++;draw()}else{playing=false;$('play').textContent='Play'}}requestAnimationFrame(loop)}select();requestAnimationFrame(loop);
</script></html>'''
doc=doc.replace('RESULT_TABLE',table).replace('TEST_LOGS',logs).replace('TRACE_DATA',json.dumps(traces,separators=(',',':')))
(folder/'index.html').write_text(doc,encoding='utf-8')
(folder/'replay-script.js').write_text(doc.split('<script>')[1].split('</script>')[0],encoding='utf-8')
print(json.dumps({'battles':len(rows),'resolved':report['resolved'],'focusedChecks':len(checks),'packages':packages},indent=2))

"""Generate the facility's review gallery and verify both packaged JARs."""
from pathlib import Path
import hashlib, html, json, struct, zipfile
from PIL import Image, ImageDraw

folder = Path(__file__).resolve().parent
repo = folder.parents[1]
packages = []
for name in ('TheMechanist-all.jar', 'TheMechanistServer-all.jar'):
    path = repo/'target'/name
    digest = hashlib.file_digest(path.open('rb'), 'sha256').hexdigest()
    maximum, count, versioned = 0, 0, []
    with zipfile.ZipFile(path) as jar:
        assert jar.testzip() is None, name
        for entry in jar.infolist():
            if entry.filename.endswith('.class'):
                with jar.open(entry) as stream:
                    header = stream.read(8)
                assert header[:4] == b'\xca\xfe\xba\xbe', entry.filename
                major = struct.unpack('>H', header[6:8])[0]
                if entry.filename.startswith('META-INF/versions/'):
                    versioned.append(dict(file=entry.filename, major=major))
                    continue
                maximum = max(maximum, major)
                count += 1
    assert maximum <= 61, (name, maximum)
    packages.append(dict(file='target/'+name, bytes=path.stat().st_size, sha256=digest,
                         classes=count, maximumClassMajor=maximum, versionedDependencyClasses=versioned, zipIntegrity='PASS'))
counts = json.loads((folder/'facility-counts.json').read_text())
report = dict(date='2026-10-26', counts=counts, packages=packages,
    build='Normal offline Maven package from current repository source: PASS',
    validation='DeveloperFacilitySmoke and eight focused regressions passed from packaged client, headless Java 17.',
    limitations=['No manual desktop gameplay check.', 'Existing combat confirmation records aim and advances time; weapon damage is not implemented by that handler.',
                 'Some existing hardware interfaces provide inspection/service previews.',
                 'Installed PACKAGE_client distribution was not replaced. Full Gate3 suite and release certification were not run.'])
(folder/'report.json').write_text(json.dumps(report, indent=2)+'\n')
names = ['hub-2d','hub-doom','combat-2d','combat-doom','systems-2d','systems-doom','trade-2d','trade-doom',
         'hub-controls','combat-controls','systems-controls','trade-controls','trade-trusted-fine','trade-hostile-gates','catalog-supply']
sheet = Image.new('RGB',(960,5*224),'#151819')
draw = ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    picture = Image.open(folder/(name+'.png')).convert('RGB')
    picture.thumbnail((316,198))
    x,y = (i%3)*320,(i//3)*224
    sheet.paste(picture,(x,y+22))
    draw.text((x+5,y+5),name,fill='#eedb9b')
sheet.save(folder/'overview.jpg',quality=90)
cards = ''.join('<figure><a href="'+n+'.png"><img loading="lazy" src="'+n+'.png" alt="'+n+'"></a><figcaption>'+n.replace('-',' ')+'</figcaption></figure>' for n in names)
package_html = ''.join('<li>'+html.escape(p['file'])+': '+str(p['classes'])+' classes, maximum major '+str(p['maximumClassMajor'])+', ZIP integrity PASS. SHA-256: <code>'+p['sha256']+'</code></li>' for p in packages)
document = '''<!doctype html><meta charset="utf-8"><title>The Mechanist — Developer facility</title>
<style>body{background:#111718;color:#e5e6df;font:17px system-ui;margin:32px auto;max-width:1400px;padding:0 20px}a{color:#b0dcff}code{overflow-wrap:anywhere}section{display:grid;grid-template-columns:repeat(auto-fit,minmax(420px,1fr));gap:20px}figure{margin:0}img{width:100%}figcaption{padding:8px;background:#263030}li{margin:10px 0}</style>
<h1>The Mechanist: developer test facility</h1><p>2026-10-26 · Actual game panels, rendered offscreen at 1280×800 from the freshly packaged client.</p>
<p>Enter <code>/devroom</code> in the local game's admin console. Use the player-only teleport doors or <code>/devroom combat</code>, <code>/devroom systems</code>, <code>/devroom trade</code>, and <code>/devroom supplies</code>. Return with <code>/devroom exit</code>. The campaign remains paused and isolated; test saves are blocked.</p>
<ul><li>Four sealed chambers; hostile arena with humanoid, machine and animal spawning, batch/tier controls and reset.</li><li>40 buildable exhibits and 103 registered hardware types, using existing production and interaction interfaces.</li><li>2,016 pinned merchants: 36 factions × seven categories × eight qualities. Existing trade gates/prices apply. Test standing can be set or purchased for 20 Script per point.</li><li>909 catalog item types × eight qualities × five units = 36,360 stocked units. Withdraw and refill through normal container handling.</li></ul>
<h2>Verification</h2><p>Normal Maven client/server package: PASS. Packaged DeveloperFacilitySmoke: PASS, including authority rejection, portal access, pinned AI, quality stock, real purchase, hostile gate, atomic reputation costs, supply withdrawals/refill, production prerequisites, persistence guards and exact campaign restoration.</p>
<p>Eight packaged regressions passed: vehicle strategic transit, faction vehicle transit, Doom HUD/billboards, NPC continuity, faction recruitment, movement execution, trade readability and container readability. The two vehicle source files' merge/duplicate-local errors were repaired to restore the normal build.</p><ul>'''+package_html+'''</ul>
<p><strong>Limits:</strong> headless validation only. The existing combat confirmation handler records aim and advances time; it does not apply weapon damage. Some hardware retains its existing service previews. The installed distribution was not replaced; the full Gate3 suite and release certification were not run.</p>
<p><a href="report.json">Machine-readable report</a> · <a href="smoke-packaged.log">Facility smoke log</a> · <a href="package.log">Maven build log</a> · <a href="facility-layout.tsv">Facility coordinates</a> · <a href="../render-billboards-20261025/index.html">Billboard baseline</a></p><h2>Captured views and controls</h2><section>'''+cards+'</section>'
(folder/'index.html').write_text(document,encoding='utf-8')

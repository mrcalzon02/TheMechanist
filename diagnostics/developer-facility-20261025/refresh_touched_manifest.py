"""Refresh this pass's evidence and changed files using the repository manifest writer.

Preserve unrelated existing rows; a full-tree refresh is outside this rendering pass.
"""
import runpy, subprocess
from pathlib import Path

repo = Path(__file__).resolve().parents[2]
api = runpy.run_path(str(repo/'ROOT_tools/update_repository_file_manifest_incremental.py'))
target = repo/'ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv'
rows = api['load_existing'](target)
paths = [repo/p for p in subprocess.check_output(['git','diff','--name-only'],cwd=repo,text=True).splitlines()]
paths += [p for p in Path(__file__).parent.rglob('*') if p.is_file()]
paths += list((repo/'src/mechanist').glob('DeveloperFacility*.java'))
paths += [repo/'src/mechanist/FirstPersonBillboardAuthority.java']
paths += list((repo/'target/classes').rglob('*.class'))
paths += list((repo/'target').glob('*.jar'))
changed = 0
for path in paths:
    if path.resolve() == target.resolve() or not path.is_file():
        continue
    relative = path.relative_to(repo).as_posix()
    previous = rows.get(relative, {})
    digest = api['sha256_file'](path)
    modified = previous.get('modified_utc') if previous.get('sha256') == digest else api['modified_utc'](path)
    rows[relative] = {
        'relative_path':relative, 'file_kind':api['file_kind'](relative,path.suffix.lower()),
        'text_or_binary':'binary' if path.suffix.lower() in api['BINARY_EXTENSIONS'] else 'text_or_unknown',
        'extension':path.suffix.lower().lstrip('.'), 'bytes':str(path.stat().st_size),
        'modified_utc':modified, 'sha256':digest}
    changed += 1
api['write_rows'](target,[rows[key] for key in sorted(rows,key=str.lower)])
print(f'Scoped manifest refresh: {changed} touched/evidence paths; {len(rows)} total rows retained.')


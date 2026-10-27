"""Refresh authored code/evidence and normal build outputs without rescanning the asset vault."""
from pathlib import Path
import runpy,subprocess

repo=Path(__file__).resolve().parents[2]
FOLDERS=['render-billboards-20261025','developer-facility-20261025','combat-scenarios-20261026','combat-execution-20261026','combat-support-20261026','medical-treatment-boundaries-20261026','cybernetics-20261026','native-battles-20261026']
EXTENSIONS={'.png','.tsv','.json','.csv','.html','.py','.ps1','.js','.cjs','.java','.txt','.log','.mp4'}
def publication_files():
    paths=[repo/p for p in subprocess.check_output(['git','diff','HEAD','--name-only'],cwd=repo,text=True,stderr=subprocess.DEVNULL).splitlines()]
    paths += [repo/p for p in subprocess.check_output(['git','ls-files','--others','--exclude-standard','src'],cwd=repo,text=True).splitlines() if p.endswith('.java')]
    for name in FOLDERS:
        folder=repo/'diagnostics'/name
        paths += [p for p in folder.iterdir() if p.is_file() and (p.suffix in EXTENSIONS or p.name=='.gitignore') and p.name!='publication-files.txt']
    for folder in (repo/'diagnostics/native-battles-20261026').iterdir():
        if folder.is_dir() and (folder/'positions.tsv').exists():
            paths += [p for p in folder.iterdir() if p.is_file() and p.suffix in {'.tsv','.json'}]
    audio=repo/'diagnostics/native-battles-20261026/weapon-audio'
    if audio.exists():paths += [p for p in audio.iterdir() if p.is_file() and p.suffix in {'.wav','.png','.html','.tsv','.json','.txt','.log'}]
    paths += [repo/'diagnostics/test-batches-20261026.json']
    return sorted(set(p for p in paths if p.is_file()))

if __name__=='__main__':
    api=runpy.run_path(str(repo/'ROOT_tools/update_repository_file_manifest_incremental.py'))
    target=repo/'ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv'
    rows=api['load_existing'](target)
    paths=publication_files()+list((repo/'target/classes').rglob('*.class'))+list((repo/'target').glob('*.jar'))
    changed=0
    for path in paths:
        if path.resolve()==target.resolve():continue
        relative=path.relative_to(repo).as_posix();previous=rows.get(relative,{})
        digest=api['sha256_file'](path)
        modified=previous.get('modified_utc') if previous.get('sha256')==digest else api['modified_utc'](path)
        rows[relative]={'relative_path':relative,'file_kind':api['file_kind'](relative,path.suffix.lower()),'text_or_binary':'binary' if path.suffix.lower() in api['BINARY_EXTENSIONS'] else 'text_or_unknown','extension':path.suffix.lower().lstrip('.'),'bytes':str(path.stat().st_size),'modified_utc':modified,'sha256':digest}
        changed+=1
    api['write_rows'](target,[rows[key] for key in sorted(rows,key=str.lower)])
    print(f'Scoped manifest: {changed} authored/evidence/build paths; {len(rows)} total rows preserved.')

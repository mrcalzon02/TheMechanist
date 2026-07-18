#!/usr/bin/env python3
from __future__ import annotations
import argparse, json, os, pathlib, shutil, stat, subprocess, sys, tempfile


def run(cmd, cwd=None, env=None, expect=0, timeout=180):
    print('+', ' '.join(str(x) for x in cmd), flush=True)
    p = subprocess.run([str(x) for x in cmd], cwd=cwd, env=env, timeout=timeout)
    if p.returncode != expect:
        raise RuntimeError(f'command returned {p.returncode}, expected {expect}: {cmd}')


def java_bin(root: pathlib.Path) -> pathlib.Path:
    return root / 'runtime' / 'bin' / ('java.exe' if os.name == 'nt' else 'java')


def classpath(root: pathlib.Path, role: str) -> str:
    jar = root / 'packages' / role / ('TheMechanist.jar' if role == 'client' else 'TheMechanistServer.jar')
    sep = ';' if os.name == 'nt' else ':'
    return f'{jar}{sep}{root / "packages" / "support" / "lib" / "*"}'


def profile_env(profile: pathlib.Path) -> dict[str, str]:
    env = os.environ.copy()
    env['HOME'] = str(profile)
    env['USERPROFILE'] = str(profile)
    env['APPDATA'] = str(profile / 'AppData' / 'Roaming')
    env['LOCALAPPDATA'] = str(profile / 'AppData' / 'Local')
    env['XDG_DATA_HOME'] = str(profile / '.local' / 'share')
    env['XDG_CONFIG_HOME'] = str(profile / '.config')
    return env


def make_read_only(root: pathlib.Path) -> None:
    for path in root.rglob('*'):
        try:
            mode = path.stat().st_mode
            path.chmod(mode & ~stat.S_IWUSR & ~stat.S_IWGRP & ~stat.S_IWOTH)
        except OSError:
            pass


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('distribution', type=pathlib.Path)
    ap.add_argument('--verifier', type=pathlib.Path, required=True)
    ap.add_argument('--report', type=pathlib.Path)
    args = ap.parse_args()
    source = args.distribution.resolve()
    if not source.is_dir():
        raise SystemExit(f'missing distribution: {source}')
    root = pathlib.Path(tempfile.mkdtemp(prefix='Mechanist Synthetic Path With Spaces '))
    install = root / 'Read Only Program Files' / source.name
    shutil.copytree(source, install)
    profile = root / 'Synthetic User Profile With Spaces'
    profile.mkdir(parents=True)
    env = profile_env(profile)
    java = java_bin(install)

    run([sys.executable, args.verifier.resolve(), install], env=env)
    run([java, '-Djava.awt.headless=true', '-cp', classpath(install, 'client'),
         'mechanist.Gate3PlayerFacingTextSmokeSuite'], cwd=root, env=env, timeout=600)
    run([java, '-cp', classpath(install, 'server'), 'mechanist.MechanistServerMain', '--help'],
        cwd=root, env=env)
    before = sorted(str(p.relative_to(profile)) for p in profile.rglob('*'))
    run([java, '-cp', classpath(install, 'server'), 'mechanist.MechanistServerMain', '--help'],
        cwd=root, env=env)
    after = sorted(str(p.relative_to(profile)) for p in profile.rglob('*'))
    if not after:
        raise RuntimeError('synthetic profile remained empty after server initialization')

    make_read_only(install)
    run([java, '-cp', classpath(install, 'server'), 'mechanist.MechanistServerMain', '--help'],
        cwd=root, env=env)

    tampered = root / 'Tampered Distribution'
    shutil.copytree(source, tampered)
    manifest = tampered / 'manifests' / 'runtime-manifest.json'
    data = json.loads(manifest.read_text(encoding='utf-8'))
    data['commit'] = 'tampered'
    manifest.write_text(json.dumps(data, indent=2), encoding='utf-8')
    run([sys.executable, args.verifier.resolve(), tampered], expect=1)

    missing = root / 'Missing Support Library'
    shutil.copytree(source, missing)
    support = sorted((missing / 'packages' / 'support' / 'lib').glob('*.jar'))
    if not support:
        raise RuntimeError('no support library available for missing-library rejection test')
    support[0].unlink()
    run([sys.executable, args.verifier.resolve(), missing], expect=1)

    summary = {
        'status': 'passed', 'distribution': source.name,
        'pathWithSpaces': True, 'isolatedProfile': True,
        'returningProfile': len(after) >= len(before), 'readOnlyInstall': True,
        'packagedGate3': True, 'serverOperation': True,
        'tamperedManifestRejected': True, 'missingSupportRejected': True,
    }
    rendered = json.dumps(summary, indent=2, sort_keys=True)
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + '\n', encoding='utf-8')
    shutil.rmtree(root, ignore_errors=True)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

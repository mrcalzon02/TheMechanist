#!/usr/bin/env python3
"""Required immutable portrait/audio payload for a bootable Mechanist installation.

The full development art tree is deliberately not part of the thin-launcher
installer. These are the concrete, authored client assets needed by profile
selection and the documented standard-graphics/core-audio defaults.
"""
from __future__ import annotations

import hashlib
import json
import pathlib
import shutil
import struct

HUMAN_ROOT = "profile-packages/human-8x8"
CLIENT_PORTRAITS = "packages/client/assets/graphics/packages/default_32/Protraits"
CLIENT_MUSIC = "packages/client/assets/music"
CLIENT_CORE_AUDIO = "packages/client/assets/sound/core"
SOURCE_HUMAN = "PACKAGE_launcher/profile-packages/human-8x8"
SOURCE_PORTRAITS = "PACKAGE_client/assets/graphics/packages/default_32/Protraits"
SOURCE_MUSIC = "PACKAGE_client/assets/music"
SOURCE_CORE_AUDIO = "PACKAGE_client/assets/sound/core"


def human_portrait_names() -> tuple[str, ...]:
    return tuple(
        f"Humans8x8_r{row:02d}c{column:02d}_32px.png"
        for row in range(1, 9) for column in range(1, 9)
    )


def _sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _required_file(path: pathlib.Path) -> None:
    if path.is_symlink() or not path.is_file() or path.stat().st_size <= 0:
        raise RuntimeError(f"required startup asset is missing, empty, or a link: {path}")


def _valid_png_32(path: pathlib.Path) -> None:
    _required_file(path)
    with path.open("rb") as stream:
        header = stream.read(24)
    if (len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n"
            or header[12:16] != b"IHDR"
            or struct.unpack(">II", header[16:24]) != (32, 32)):
        raise RuntimeError(f"startup portrait must be a 32x32 PNG: {path}")


def _copy(source: pathlib.Path, destination: pathlib.Path) -> None:
    _required_file(source)
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)
    if _sha256(source) != _sha256(destination):
        raise RuntimeError(f"copied startup asset hash mismatch: {destination}")


def _music_tracks(manifest: pathlib.Path) -> tuple[str, ...]:
    _required_file(manifest)
    tracks: list[str] = []
    for line_number, text in enumerate(manifest.read_text(encoding="utf-8").splitlines(), 1):
        if not text.strip() or text.lstrip().startswith("#"):
            continue
        parts = text.split("\t")
        if len(parts) < 2 or not parts[0].strip():
            raise RuntimeError(f"invalid music manifest row {line_number}: {manifest}")
        name = parts[1].strip()
        if (not name.lower().endswith(".wav") or name.startswith(".")
                or pathlib.PurePosixPath(name).name != name or "\\" in name):
            raise RuntimeError(f"unsafe/invalid music file in row {line_number}: {name}")
        tracks.append(name)
    if not tracks:
        raise RuntimeError(f"startup music manifest contains no tracks: {manifest}")
    return tuple(dict.fromkeys(tracks))


def stage_startup_assets(repo: pathlib.Path, distribution: pathlib.Path) -> dict[str, int]:
    """Stage only authored shipping portrait/music/core-audio assets; fail closed."""
    human_source = repo / SOURCE_HUMAN
    human_destination = distribution / HUMAN_ROOT
    package_file = human_source / "package.json"
    _required_file(package_file)
    spec = json.loads(package_file.read_text(encoding="utf-8"))
    if spec.get("asset_payload", {}).get("included_png_files") != 64:
        raise RuntimeError("launcher human portrait package must declare exactly 64 images")
    expected = set(human_portrait_names())
    actual = {p.name for p in (human_source / "assets").glob("*.png")}
    if actual != expected:
        raise RuntimeError(f"launcher portrait source catalog incomplete: missing={sorted(expected-actual)} extra={sorted(actual-expected)}")
    _copy(package_file, human_destination / "package.json")
    for filename in human_portrait_names():
        source = human_source / "assets" / filename
        _valid_png_32(source)
        _copy(source, human_destination / "assets" / filename)

    portraits_source = repo / SOURCE_PORTRAITS
    client_portraits = sorted(portraits_source.glob("*.png"))
    if not expected.issubset({p.name for p in client_portraits}):
        raise RuntimeError("client catalog omits one or more standard human portraits")
    if len(client_portraits) < 64:
        raise RuntimeError("client startup portrait catalog has fewer than 64 images")
    for portrait in client_portraits:
        _valid_png_32(portrait)
        _copy(portrait, distribution / CLIENT_PORTRAITS / portrait.name)

    music_source = repo / SOURCE_MUSIC
    music_manifest = music_source / "music_manifest.tsv"
    tracks = _music_tracks(music_manifest)
    _copy(music_manifest, distribution / CLIENT_MUSIC / "music_manifest.tsv")
    for filename in tracks:
        _copy(music_source / "wav" / filename, distribution / CLIENT_MUSIC / "wav" / filename)

    core_source = repo / SOURCE_CORE_AUDIO
    core_audio = sorted(p for p in core_source.iterdir() if p.is_file() and p.suffix.lower() == ".wav")
    if not core_audio:
        raise RuntimeError("client core-audio source catalog is empty")
    for wav in core_audio:
        _copy(wav, distribution / CLIENT_CORE_AUDIO / wav.name)
    return {"launcherHumanPortraits": 64, "clientPortraits": len(client_portraits),
            "musicTracks": len(tracks), "coreAudioClips": len(core_audio)}


def _verified_asset(root: pathlib.Path, entries: dict[str, dict[str, object]], relative: str,
                    *, portrait: bool = False) -> None:
    entry = entries.get(relative)
    if not isinstance(entry, dict):
        raise RuntimeError(f"canonical runtime manifest does not declare startup asset: {relative}")
    path = root.joinpath(*pathlib.PurePosixPath(relative).parts)
    _required_file(path)
    if path.stat().st_size != entry.get("size") or _sha256(path) != entry.get("sha256"):
        raise RuntimeError(f"startup asset differs from canonical runtime manifest: {relative}")
    if portrait:
        _valid_png_32(path)


def verify_startup_assets(root: pathlib.Path,
                          entries: dict[str, dict[str, object]]) -> dict[str, int]:
    """Validate the *actual* staged/installed files against canonical SHA-256 records."""
    _verified_asset(root, entries, f"{HUMAN_ROOT}/package.json")
    spec = json.loads((root / HUMAN_ROOT / "package.json").read_text(encoding="utf-8"))
    if spec.get("asset_payload", {}).get("included_png_files") != 64:
        raise RuntimeError("installed launcher human portrait package declares an invalid count")
    human_names = human_portrait_names()
    for filename in human_names:
        _verified_asset(root, entries, f"{HUMAN_ROOT}/assets/{filename}", portrait=True)

    client_portraits = sorted(key for key in entries
                              if key.startswith(CLIENT_PORTRAITS + "/") and key.endswith(".png"))
    if len(client_portraits) < 64:
        raise RuntimeError("installed client portrait catalog has fewer than 64 declared PNGs")
    if not {f"{CLIENT_PORTRAITS}/{filename}" for filename in human_names}.issubset(client_portraits):
        raise RuntimeError("installed client portrait catalog omits standard human portraits")
    for relative in client_portraits:
        _verified_asset(root, entries, relative, portrait=True)

    music_manifest = f"{CLIENT_MUSIC}/music_manifest.tsv"
    _verified_asset(root, entries, music_manifest)
    tracks = _music_tracks(root / music_manifest)
    for filename in tracks:
        _verified_asset(root, entries, f"{CLIENT_MUSIC}/wav/{filename}")
    clips = sorted(key for key in entries
                   if key.startswith(CLIENT_CORE_AUDIO + "/") and key.lower().endswith(".wav"))
    if not clips:
        raise RuntimeError("installed core-audio catalog has no declared clips")
    for relative in clips:
        _verified_asset(root, entries, relative)
    return {"launcherHumanPortraits": len(human_names),
            "clientPortraits": len(client_portraits),
            "musicTracks": len(tracks), "coreAudioClips": len(clips)}

#!/usr/bin/env python3
"""Fast, hermetic regressions for the previously unshipped launcher portraits."""
from __future__ import annotations

import hashlib
import os
import subprocess
import pathlib
import struct
import tempfile
import unittest

from build_runnable_distribution import write_launchers
from startup_assets import (
    CLIENT_CORE_AUDIO,
    CLIENT_MUSIC,
    CLIENT_PORTRAITS,
    HUMAN_ROOT,
    SOURCE_CORE_AUDIO,
    SOURCE_HUMAN,
    SOURCE_MUSIC,
    SOURCE_PORTRAITS,
    human_portrait_names,
    stage_startup_assets,
    verify_startup_assets,
)


# The verifier needs a valid PNG header, not generated art or a Pillow dependency.
def png(width: int = 32, height: int = 32) -> bytes:
    return b"\x89PNG\r\n\x1a\n" + b"\x00\x00\x00\rIHDR" + struct.pack(">II", width, height) + b"\x08\x06\x00\x00\x00"


class StartupAssetsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="mechanist-startup-assets-")
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)
        self.repo = self.root / "source"
        self.release = self.root / "release"
        self._write(SOURCE_HUMAN + "/package.json", b'{"asset_payload":{"included_png_files":64}}')
        for filename in human_portrait_names():
            self._write(SOURCE_HUMAN + "/assets/" + filename, png())
            self._write(SOURCE_PORTRAITS + "/" + filename, png())
        self._write(SOURCE_MUSIC + "/music_manifest.tsv", b"MAIN_MENU\tmain.wav\tMain menu\n")
        self._write(SOURCE_MUSIC + "/wav/main.wav", b"RIFF" + b"\0" * 40)
        self._write(SOURCE_CORE_AUDIO + "/footstep.wav", b"RIFF" + b"\0" * 40)
        self.assertEqual(stage_startup_assets(self.repo, self.release)["launcherHumanPortraits"], 64)
        self.entries = {
            path.relative_to(self.release).as_posix(): {
                "size": path.stat().st_size,
                "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            }
            for path in self.release.rglob("*") if path.is_file()
        }

    def _write(self, relative: str, data: bytes):
        path = self.repo / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    def test_complete_bundled_set_passes(self):
        self.assertEqual(verify_startup_assets(self.release, self.entries), {
            "launcherHumanPortraits": 64, "clientPortraits": 64,
            "musicTracks": 1, "coreAudioClips": 1,
        })

    def test_missing_launcher_portrait_blocks_release(self):
        (self.release / HUMAN_ROOT / "assets" / human_portrait_names()[0]).unlink()
        with self.assertRaisesRegex(RuntimeError, "startup asset"):
            verify_startup_assets(self.release, self.entries)

    def test_bad_png_dimensions_blocks_release(self):
        path = self.release / HUMAN_ROOT / "assets" / human_portrait_names()[0]
        path.write_bytes(png(64, 64))
        self.entries[path.relative_to(self.release).as_posix()] = {
            "size": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
        }
        with self.assertRaisesRegex(RuntimeError, "32x32 PNG"):
            verify_startup_assets(self.release, self.entries)

    def test_missing_client_catalog_image_blocks_release(self):
        (self.release / CLIENT_PORTRAITS / human_portrait_names()[-1]).unlink()
        with self.assertRaisesRegex(RuntimeError, "startup asset"):
            verify_startup_assets(self.release, self.entries)

    def test_missing_music_blocks_release(self):
        (self.release / CLIENT_MUSIC / "wav/main.wav").unlink()
        with self.assertRaisesRegex(RuntimeError, "startup asset"):
            verify_startup_assets(self.release, self.entries)

    def test_missing_core_audio_blocks_release(self):
        (self.release / CLIENT_CORE_AUDIO / "footstep.wav").unlink()
        with self.assertRaisesRegex(RuntimeError, "startup asset"):
            verify_startup_assets(self.release, self.entries)

    def test_direct_client_launchers_enter_client_package(self):
        install = self.root / "Install With Spaces"
        install.mkdir()
        write_launchers(install, "windows-x64")
        win = (install / "Run-Client-Direct.cmd").read_text(encoding="utf-8")
        self.assertIn('cd /d "%ROOT%packages\\client" || exit /b 1', win)
        self.assertLess(win.index("cd /d"), win.index("java.exe"))
        write_launchers(install, "linux-x64")
        shell = install / "run-client-direct.sh"
        text = shell.read_text(encoding="utf-8")
        self.assertIn('cd "$ROOT/packages/client"', text)
        self.assertLess(text.index('cd "$ROOT/packages/client"'), text.index('exec "$ROOT/runtime/bin/java"'))
        if os.name == "nt":
            return
        client = install / "packages/client"
        client.mkdir(parents=True)
        java = install / "runtime/bin/java"
        java.parent.mkdir(parents=True)
        java.write_text('#!/bin/sh\nprintf "%s\\n" "$PWD" "$@"\nexit 23\n', encoding="utf-8")
        java.chmod(0o755)
        result = subprocess.run([str(shell), "arg with spaces"], cwd=self.root,
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(result.returncode, 23)
        self.assertEqual(result.stdout.splitlines(), [
            str(client.resolve()), "-cp",
            f"{install}/packages/client/TheMechanist.jar:{install}/packages/support/lib/*",
            "mechanist.TheMechanist", "arg with spaces",
        ])

    def test_source_catalog_missing_portrait_rejected(self):
        (self.repo / SOURCE_HUMAN / "assets" / human_portrait_names()[-1]).unlink()
        with self.assertRaisesRegex(RuntimeError, "catalog incomplete"):
            stage_startup_assets(self.repo, self.root / "broken")


if __name__ == "__main__":
    unittest.main()

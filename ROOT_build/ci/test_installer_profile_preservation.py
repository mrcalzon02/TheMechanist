#!/usr/bin/env python3
"""Host-only negative and positive tests for installer mutable-profile evidence."""
import pathlib
import tempfile
import unittest
from unittest.mock import patch

from verify_installer_profile_preservation import (SENTINELS, capture, check,
    capture_deb_upgrade, check_deb_upgrade)


class ProfilePreservationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = pathlib.Path(self.temp.name)
        self.image = root / "Installed Mechanist"
        self.image.mkdir()
        self.profile = root / "User Profile With Spaces"
        self.baseline = root / "evidence" / "baseline.json"
        self.commit = "candidate-sha"

    def capture(self):
        return capture(self.profile, self.image, self.commit, self.baseline)

    def test_capture_and_readback(self):
        self.capture()
        record = check(self.profile, self.image, self.commit, self.baseline)
        self.assertEqual(set(record["files"]), set(SENTINELS))

    def test_changed_save_is_rejected(self):
        self.capture()
        (self.profile / SENTINELS[0]).write_bytes(b"overwritten")
        with self.assertRaisesRegex(RuntimeError, "changed"):
            check(self.profile, self.image, self.commit, self.baseline)

    def test_missing_profile_is_rejected(self):
        self.capture()
        (self.profile / SENTINELS[1]).unlink()
        with self.assertRaisesRegex(RuntimeError, "removed"):
            check(self.profile, self.image, self.commit, self.baseline)

    def test_wrong_candidate_is_rejected(self):
        self.capture()
        with self.assertRaisesRegex(RuntimeError, "identity"):
            check(self.profile, self.image, "different-sha", self.baseline)

    def test_refuses_to_replace_existing_data(self):
        self.capture()
        with self.assertRaisesRegex(RuntimeError, "baseline already exists"):
            self.capture()

    def test_rejects_profile_inside_installer(self):
        with self.assertRaisesRegex(RuntimeError, "inside installed"):
            capture(self.image / "profile", self.image, self.commit, self.baseline)



class DebUpgradeEvidenceTests(unittest.TestCase):
    """Pure verifier-contract tests; these do not pretend to install real DEBs."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = pathlib.Path(self.temp.name)
        self.image = root / "Installed Game"
        self.image.mkdir()
        self.profile = root / "Mutable Profile With Spaces"
        self.baseline = root / "evidence.json"
        self.old_deb = root / "old.deb"
        self.new_deb = root / "new.deb"
        self.old_report = root / "old-smoke.json"
        self.new_report = root / "new-smoke.json"
        self.old_report.write_text("old deployment evidence", encoding="utf-8")
        self.old = {"package": "the-mechanist", "version": "1.0.0-1", "sha256": "old-hash"}
        self.new = {"package": "the-mechanist", "version": "1.1.0-1", "sha256": "new-hash"}

    def capture_old(self):
        with (patch("verify_installer_profile_preservation.deb_identity", return_value=self.old),
              patch("verify_installer_profile_preservation.installed_deb_version", return_value="1.0.0-1"),
              patch("verify_installer_profile_preservation.verified_deployment", return_value={"version": "1.0.0"})):
            capture_deb_upgrade(self.profile, self.image, "old-commit", self.baseline,
                                self.old_deb, self.old_report)

    def verify_new(self, new=None, installed="1.1.0-1", commit="new-commit",
                   deployment=None, upgrade=True):
        if new is None:
            new = self.new
        if deployment is None:
            deployment = {"version": "1.1.0"}
        with (patch("verify_installer_profile_preservation.deb_identity",
                    side_effect=lambda path: self.old if path == self.old_deb else new),
              patch("verify_installer_profile_preservation.installed_deb_version", return_value=installed),
              patch("verify_installer_profile_preservation.verified_deployment", return_value=deployment),
              patch("verify_installer_profile_preservation.is_deb_upgrade", return_value=upgrade)):
            return check_deb_upgrade(self.profile, self.image, "old-commit", commit,
                                     self.baseline, self.old_deb, self.new_deb, self.new_report)

    def test_distinct_version_commit_and_profile_pass(self):
        self.capture_old()
        result = self.verify_new()
        self.assertTrue(result["installedGameVerified"])
        self.assertEqual(set(result["preservedFiles"]), set(SENTINELS))

    def test_same_version_is_not_upgrade(self):
        self.capture_old()
        with self.assertRaisesRegex(RuntimeError, "different version"):
            self.verify_new(new=self.old, installed="1.0.0-1")

    def test_same_commit_is_not_upgrade(self):
        self.capture_old()
        with self.assertRaisesRegex(RuntimeError, "distinct"):
            self.verify_new(commit="old-commit")

    def test_wrong_installed_version_is_rejected(self):
        self.capture_old()
        with self.assertRaisesRegex(RuntimeError, "installed DEB version"):
            self.verify_new(installed="1.0.0-1")

    def test_downgrade_is_rejected(self):
        self.capture_old()
        with self.assertRaisesRegex(RuntimeError, "not greater"):
            self.verify_new(upgrade=False)

    def test_changed_save_is_rejected(self):
        self.capture_old()
        (self.profile / SENTINELS[0]).write_bytes(b"mutated save")
        with self.assertRaisesRegex(RuntimeError, "changed"):
            self.verify_new()

    def test_wrong_package_is_rejected(self):
        self.capture_old()
        with self.assertRaisesRegex(RuntimeError, "same package"):
            self.verify_new(new={"package": "unrelated", "version": "1.1.0-1",
                                 "sha256": "new-hash"})

    def test_missing_old_capture_is_rejected(self):
        capture(self.profile, self.image, "old-commit", self.baseline)
        with self.assertRaisesRegex(RuntimeError, "capture evidence"):
            self.verify_new()

    def test_mismatched_new_game_version_is_rejected(self):
        self.capture_old()
        with self.assertRaisesRegex(RuntimeError, "installed game version"):
            self.verify_new(deployment={"version": "9.9.9"})


if __name__ == "__main__":
    unittest.main()

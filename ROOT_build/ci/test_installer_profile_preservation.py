#!/usr/bin/env python3
"""Host-only negative and positive tests for installer mutable-profile evidence."""
import pathlib
import tempfile
import unittest

from verify_installer_profile_preservation import SENTINELS, capture, check


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


if __name__ == "__main__":
    unittest.main()

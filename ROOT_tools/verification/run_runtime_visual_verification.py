#!/usr/bin/env python3
"""Build and exercise a development client on a real X11 virtual desktop."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile


def main():
    repo = Path(__file__).resolve().parents[2]
    output = repo / "dist/runtime-visual-verification"
    output.mkdir(parents=True, exist_ok=True)
    report = {"status": "failed", "releaseCertification": False,
              "commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=repo, text=True).strip()}

    def run(command, cwd, logfile, timeout=1200):
        with (output / logfile).open("w", encoding="utf-8") as log:
            subprocess.run([str(item) for item in command], cwd=cwd, stdout=log,
                           stderr=subprocess.STDOUT, check=True, timeout=timeout)

    try:
        run(["mvn", "-B", "-DskipTests", "clean", "package"], repo, "build.log")
        jar = repo / "target/TheMechanist-all.jar"
        with zipfile.ZipFile(jar) as archive:
            classes = [name for name in archive.namelist() if name.startswith("mechanist/") and name.endswith(".class")]
            if not classes:
                raise RuntimeError("Packaged client contains no game classes")
            for name in classes:
                if int.from_bytes(archive.read(name)[6:8], "big") > 61:
                    raise RuntimeError("Non-Java-17 class in package: " + name)
        report["clientSha256"] = hashlib.sha256(jar.read_bytes()).hexdigest()
        report["gameClassCount"] = len(classes)
        # The runtime and harness are deliberately outside the repository. The
        # source vault, loose classes and checkout cannot satisfy missing assets.
        with tempfile.TemporaryDirectory(prefix="mechanist-visual-") as scratch:
            root = Path(scratch)
            runtime = root / "runtime"
            runtime.mkdir()
            shutil.copy2(jar, runtime / "TheMechanist.jar")
            shutil.copytree(repo / "PACKAGE_client/assets", runtime / "assets")
            harness = root / "driver"
            harness.mkdir()
            run(["javac", "--release", "17", "-encoding", "UTF-8", "-cp", runtime / "TheMechanist.jar",
                 "-d", harness, repo / "ROOT_tools/verification/RuntimeVisualVerification.java"],
                root, "driver-compile.log")
            user = root / "user"
            user.mkdir()
            run(["java", "-Xmx4g", "-Djava.awt.headless=false", "-Dsun.java2d.opengl=false",
                 "-Duser.home=" + str(user), "-Dmechanist.client.storage.root=" + str(user / "data"),
                 "-cp", os.pathsep.join([str(harness), str(runtime / "TheMechanist.jar")]),
                 "mechanist.RuntimeVisualVerification", output], runtime, "client.log", timeout=900)
        report["status"] = "passed-automated-checks"
        report["manualScreenshotReviewRequired"] = True
        return 0
    except Exception as failure:
        report["error"] = str(failure)
        print(str(failure), file=sys.stderr)
        return 1
    finally:
        report["screenshots"] = {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                                 for path in sorted(output.glob("*.png"))}
        (output / "report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(report, indent=2))


if __name__ == "__main__":
    sys.exit(main())

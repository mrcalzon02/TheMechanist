#!/usr/bin/env python3
"""Fail closed when The Mechanist release-obfuscation policies drift."""

from __future__ import annotations

import argparse
import json
import pathlib
import re
import sys

POLICIES = {
    "client": pathlib.PurePosixPath("config/proguard/proguard-client.conf"),
    "server": pathlib.PurePosixPath("config/proguard/proguard-server.conf"),
}
REQUIRED_MAIN_CLASSES = (
    "mechanist.TheMechanist",
    "mechanist.RemoteClientMain",
    "mechanist.MechanistServerMain",
)
FORBIDDEN_OPTIONS = (
    "-dontpreverify",
    "-keepresourcefiles",
    "-target",
    "-allowaccessmodification",
    "-repackageclasses",
    "-flattenpackagehierarchy",
)
REQUIRED_ATTRIBUTES = (
    "Exceptions",
    "InnerClasses",
    "EnclosingMethod",
    "Signature",
    "*Annotation*",
    "Record",
    "PermittedSubclasses",
    "MethodParameters",
)
OPTIONAL_WARNING_NAMESPACES = {
    "com.studiohartman.jamepad.**",
    "org.lwjgl.**",
    "io.netty.**",
}


def fail(message: str) -> None:
    raise RuntimeError(message)


def active_lines(text: str) -> list[str]:
    lines: list[str] = []
    for raw in text.splitlines():
        line = raw.split("#", 1)[0].strip()
        if line:
            lines.append(line)
    return lines


def option_present(lines: list[str], option: str) -> bool:
    return any(line == option or line.startswith(option + " ") for line in lines)


def verify_policy(repo: pathlib.Path, role: str, relative: pathlib.PurePosixPath) -> dict[str, object]:
    path = repo.joinpath(*relative.parts)
    if not path.is_file():
        fail(f"missing {role} ProGuard policy: {relative.as_posix()}")
    text = path.read_text(encoding="utf-8")
    lines = active_lines(text)

    for forbidden in FORBIDDEN_OPTIONS:
        if option_present(lines, forbidden):
            fail(f"{relative}: forbidden or Java-17-unsafe option {forbidden}")

    for required in ("-basedirectory", "-dontshrink", "-dontoptimize", "-useuniqueclassmembernames"):
        if not option_present(lines, required):
            fail(f"{relative}: missing required option {required}")

    basedirectory = next(
        (line.split(None, 1)[1].strip() for line in lines if line.startswith("-basedirectory ")),
        "",
    )
    if basedirectory != "../../":
        fail(f"{relative}: basedirectory must resolve config/proguard back to repository root")

    attribute_line = next(
        (line for line in lines if line.startswith("-keepattributes ")),
        "",
    )
    for attribute in REQUIRED_ATTRIBUTES:
        if attribute not in attribute_line:
            fail(f"{relative}: keepattributes is missing {attribute}")

    expected_report_root = f"target/proguard/{role}/"
    report_options = {
        "-printmapping": "mapping.txt",
        "-printseeds": "seeds.txt",
        "-printusage": "usage.txt",
    }
    reports: dict[str, str] = {}
    for option, filename in report_options.items():
        values = [
            line.split(None, 1)[1].strip()
            for line in lines
            if line.startswith(option + " ")
        ]
        if values != [expected_report_root + filename]:
            fail(
                f"{relative}: {option} must point exactly to "
                f"{expected_report_root + filename}"
            )
        reports[option.removeprefix("-")] = values[0]

    if "-keepclasseswithmembers class * {" not in text:
        fail(f"{relative}: executable main-class preservation rule is missing")
    if "public static void main(java.lang.String[]);" not in text:
        fail(f"{relative}: public static main signature preservation is missing")

    for class_name in REQUIRED_MAIN_CLASSES:
        pattern = re.compile(
            rf"-keep\s+class\s+{re.escape(class_name)}\s*\{{[^}}]*"
            rf"public\s+static\s+void\s+main\(java\.lang\.String\[\]\);",
            re.DOTALL,
        )
        if not pattern.search(text):
            fail(f"{relative}: stable keep rule missing for {class_name}")

    warning_lines = {
        line.split(None, 1)[1].strip()
        for line in lines
        if line.startswith("-dontwarn ")
    }
    if warning_lines != OPTIONAL_WARNING_NAMESPACES:
        fail(
            f"{relative}: warning suppressions must remain narrow; "
            f"found {sorted(warning_lines)}"
        )

    for namespace in ("io.netty.**", "org.lwjgl.**", "com.studiohartman.jamepad.**"):
        if f"-keep class {namespace} {{ *; }}" not in text:
            fail(f"{relative}: reflected/native class preservation missing for {namespace}")
        if f"-keep interface {namespace} {{ *; }}" not in text:
            fail(f"{relative}: reflected/native interface preservation missing for {namespace}")

    return {
        "role": role,
        "path": relative.as_posix(),
        "stabilityMode": "obfuscate-only-no-shrink-no-optimize",
        "java17UnsafeOptionsAbsent": True,
        "stableMainClasses": list(REQUIRED_MAIN_CLASSES),
        "reports": reports,
        "narrowWarningSuppressions": sorted(warning_lines),
        "mappingPackaged": False,
    }


def verify(repo: pathlib.Path) -> dict[str, object]:
    repo = repo.resolve()
    pom = repo / "pom.xml"
    if not pom.is_file():
        fail(f"missing Maven project: {pom}")
    pom_text = pom.read_text(encoding="utf-8")
    policies: list[dict[str, object]] = []
    for role, relative in POLICIES.items():
        expected_reference = "${project.basedir}/" + relative.as_posix()
        if expected_reference not in pom_text:
            fail(f"pom.xml does not reference governed {role} policy {expected_reference}")
        policies.append(verify_policy(repo, role, relative))

    forbidden_distribution_fragments = (
        "mapping.txt",
        "seeds.txt",
        "usage.txt",
        "secure-maps",
    )
    builder = repo / "ROOT_build" / "ci" / "build_runnable_distribution.py"
    if not builder.is_file():
        fail("portable distribution builder is missing")
    builder_text = builder.read_text(encoding="utf-8")
    for fragment in forbidden_distribution_fragments:
        if re.search(
            rf"copy_file\([^\n]*{re.escape(fragment)}|PLAYTEST_DOCS[^\n]*{re.escape(fragment)}",
            builder_text,
            re.IGNORECASE,
        ):
            fail(f"portable builder appears to package protected ProGuard artifact {fragment}")

    return {
        "status": "verified",
        "javaRelease": 17,
        "policyCount": len(policies),
        "policies": policies,
        "releaseObfuscation": True,
        "shrinking": False,
        "optimization": False,
        "mappingArtifactsOutsideDistribution": True,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=pathlib.Path, default=pathlib.Path.cwd())
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()
    try:
        summary = verify(args.repo)
    except Exception as exc:  # noqa: BLE001 - one release-gate failure surface
        print(f"PROGUARD POLICY VERIFICATION FAILED: {exc}", file=sys.stderr)
        return 1
    rendered = json.dumps(summary, indent=2, sort_keys=True)
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Verify the real Jekyll output and APKs before a GitHub Pages publication.

Run after preparing both downloadable APK names and building the site:
    python mon-petit-leopard/tools/validate_pages_output.py \
        --site-dir _site \
        --compiled-apk mon-petit-leopard/app/build/outputs/apk/debug/app-debug.apk \
        --version 0.8.11

The report is written outside the published directory, including on failure.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SITE_LIMIT_BYTES = 1_000_000_000
APK_LIMIT_BYTES = 100 * 1024 * 1024
BLOCK_BYTES = 1024 * 1024
DEFAULT_REPORT = REPOSITORY_ROOT / "mon-petit-leopard/build/pages-verification.json"


def repository_path(value: str) -> Path:
    path = Path(value)
    return path.resolve() if path.is_absolute() else (REPOSITORY_ROOT / path).resolve()


def fingerprint(path: Path) -> dict[str, Any]:
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(BLOCK_BYTES), b""):
            size += len(block)
            digest.update(block)
    return {"bytes": size, "sha256": digest.hexdigest()}


def compare_files(source: Path, published: Path) -> dict[str, Any]:
    """Compare actual bytes and retain hashes for the publication report."""
    source_digest = hashlib.sha256()
    published_digest = hashlib.sha256()
    source_size = published_size = 0
    identical = True
    with source.open("rb") as first, published.open("rb") as second:
        while True:
            source_block = first.read(BLOCK_BYTES)
            published_block = second.read(BLOCK_BYTES)
            if not source_block and not published_block:
                break
            identical = identical and source_block == published_block
            source_size += len(source_block)
            published_size += len(published_block)
            source_digest.update(source_block)
            published_digest.update(published_block)
    return {
        "identical": identical,
        "source": {"bytes": source_size, "sha256": source_digest.hexdigest()},
        "published": {"bytes": published_size, "sha256": published_digest.hexdigest()},
    }


def collect_files(directory: Path, errors: list[str]) -> dict[str, Path]:
    files: dict[str, Path] = {}
    if not directory.is_dir():
        errors.append(f"Missing directory: {directory}")
        return files
    for path in sorted(directory.rglob("*")):
        relative = path.relative_to(directory).as_posix()
        if path.is_symlink():
            errors.append(f"Symlink is not a verifiable published file: {path}")
        elif path.is_file():
            files[relative] = path
        elif not path.is_dir():
            errors.append(f"Unsupported filesystem entry: {path}")
    return files


def validate(site_dir: Path, compiled_apk: Path, version: str) -> dict[str, Any]:
    errors: list[str] = []
    report: dict[str, Any] = {
        "verified_at_utc": datetime.now(timezone.utc).isoformat(),
        "version": version,
        "repository": str(REPOSITORY_ROOT),
        "site_directory": str(site_dir),
        "compiled_apk_path": str(compiled_apk),
        "limits": {
            "site_bytes_exclusive": SITE_LIMIT_BYTES,
            "apk_bytes_inclusive": APK_LIMIT_BYTES,
        },
        "public_files": {},
        "downloads": {},
        "current_apks": {},
        "errors": errors,
    }

    site_files = collect_files(site_dir, errors)
    site_bytes = sum(path.stat().st_size for path in site_files.values())
    report["site"] = {
        "file_count": len(site_files),
        "bytes": site_bytes,
        "files": sorted(site_files),
    }
    if site_bytes >= SITE_LIMIT_BYTES:
        errors.append(f"Published site must be smaller than {SITE_LIMIT_BYTES} bytes; got {site_bytes}")

    for name in ("app", "source-assets", "qa", "tools", "archive", "build"):
        if (site_dir / "mon-petit-leopard" / name).exists():
            errors.append(f"Android project directory was published: mon-petit-leopard/{name}")
    for relative in site_files:
        if relative.startswith("mon-petit-leopard/") and relative != "mon-petit-leopard/index.html":
            errors.append(f"Android project file was published: {relative}")
        if relative.startswith(".github/") or relative in {"_config.yml", ".nojekyll"}:
            errors.append(f"Publication configuration was published: {relative}")
        if Path(relative).suffix.lower() == ".apk" and not relative.startswith("downloads/"):
            errors.append(f"Unexpected APK outside downloads/: {relative}")

    for relative in ("mon-petit-leopard/index.html", ".well-known/assetlinks.json"):
        source = REPOSITORY_ROOT / relative
        published = site_files.get(relative)
        if not source.is_file() or source.is_symlink():
            errors.append(f"Missing regular source file: {relative}")
        elif published is None:
            errors.append(f"Missing public file: {relative}")
        else:
            comparison = compare_files(source, published)
            report["public_files"][relative] = comparison
            if not comparison["identical"]:
                errors.append(f"Public file bytes changed: {relative}")

    source_downloads = collect_files(REPOSITORY_ROOT / "downloads", errors)
    expected_apks = {
        relative: path for relative, path in source_downloads.items()
        if path.suffix.lower() == ".apk"
    }
    published_apks = {
        relative.removeprefix("downloads/"): path
        for relative, path in site_files.items()
        if relative.startswith("downloads/") and path.suffix.lower() == ".apk"
    }
    missing = sorted(expected_apks.keys() - published_apks.keys())
    unexpected = sorted(published_apks.keys() - expected_apks.keys())
    report["download_inventory"] = {
        "source_apk_count": len(expected_apks),
        "published_apk_count": len(published_apks),
        "missing": missing,
        "unexpected": unexpected,
    }
    if missing:
        errors.append(f"Missing published APKs: {', '.join(missing)}")
    if unexpected:
        errors.append(f"Unexpected published APKs: {', '.join(unexpected)}")
    for relative in sorted(expected_apks.keys() & published_apks.keys()):
        comparison = compare_files(expected_apks[relative], published_apks[relative])
        report["downloads"][relative] = comparison
        if not comparison["identical"]:
            errors.append(f"Published APK bytes changed: {relative}")

    if not compiled_apk.is_file() or compiled_apk.is_symlink():
        errors.append(f"Missing regular compiled APK: {compiled_apk}")
    else:
        compiled = fingerprint(compiled_apk)
        report["compiled_apk"] = compiled
        if compiled["bytes"] == 0:
            errors.append("Compiled APK is empty")
        if compiled["bytes"] > APK_LIMIT_BYTES:
            errors.append(f"Compiled APK exceeds {APK_LIMIT_BYTES} bytes: {compiled['bytes']}")
        for name in ("mon-petit-leopard.apk", f"mon-petit-leopard-v{version}.apk"):
            if name not in expected_apks:
                errors.append(f"Current APK was not prepared in downloads/: {name}")
            if name not in published_apks:
                errors.append(f"Missing current public APK: {name}")
            else:
                comparison = compare_files(compiled_apk, published_apks[name])
                report["current_apks"][name] = comparison
                if not comparison["identical"]:
                    errors.append(f"Current public APK differs from the compiled APK: {name}")

    report["passed"] = not errors
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--site-dir", required=True, help="Actual Jekyll destination, relative to the repository or absolute")
    parser.add_argument("--compiled-apk", required=True, help="APK just compiled by the Android build")
    parser.add_argument("--version", required=True, help="Version represented by both current download filenames")
    parser.add_argument("--report", default=str(DEFAULT_REPORT), help="JSON report path, outside the published site")
    args = parser.parse_args()
    if not re.fullmatch(r"\d+\.\d+\.\d+", args.version):
        parser.error("--version must have the form 0.8.11")
    site_dir = repository_path(args.site_dir)
    compiled_apk = repository_path(args.compiled_apk)
    report_path = repository_path(args.report)
    if report_path.is_relative_to(site_dir):
        parser.error("--report must be outside the published site directory")
    try:
        report = validate(site_dir, compiled_apk, args.version)
    except OSError as error:
        report = {
            "passed": False,
            "version": args.version,
            "errors": [f"Could not inspect the real publication files: {error}"],
        }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if report["passed"]:
        print(f"Pages verification passed: {report['download_inventory']['published_apk_count']} APKs, {report['site']['bytes']} site bytes")
    else:
        for error in report["errors"]:
            print(f"ERROR: {error}")
    print(f"Report: {report_path}")
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())

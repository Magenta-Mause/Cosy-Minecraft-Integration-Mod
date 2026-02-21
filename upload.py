#!/usr/bin/env python3
import os
import subprocess
import shlex
import pathlib
import re
import sys

name = os.environ["RELEASE_NAME"]
tag = os.environ["RELEASE_TAG"]
notes = os.environ["RELEASE_NOTES"]
token = os.environ["MODRINTH_TOKEN"]
project_id = os.environ["MODRINTH_PROJECT_ID"]

libs = pathlib.Path("build/libs")
jars = [p for p in libs.iterdir() if p.suffix == '.jar' and not p.name.endswith('-sources.jar')]
print(f"Found {len(jars)} non-source JARs")

# Extract MC from filename: cosyintegrationmod-mc1.21.2-1.0.jar → 1.21.2
mc_regex = re.compile(r'-mc([0-9.]+?)(?=\.\d+\.jar|$)')

published = 0
for jarpath in jars:
    match = mc_regex.search(jarpath.name)
    if match:
        mc = match.group(1)
        args = [
            "npx", "modrinth", "publish",
            "--token", token,
            "--project-id", project_id,
            "--name", f"{name} (MC {mc})",
            "--version-number", f"{tag}+mc{mc}",
            "--changelog", notes,
            "--loaders", "fabric",
            "--game-versions", mc,
            str(jarpath)
        ]
        cmd = ["bash", "-lc", " ".join(shlex.quote(a) for a in args)]
        print(f"Publishing {jarpath.name} for MC {mc}...")
        subprocess.check_call(cmd)
        published += 1
    else:
        print(f"Skipping {jarpath.name}: no MC version")

print(f"Published {published}/{len(jars)} JARs.")
sys.exit(0)


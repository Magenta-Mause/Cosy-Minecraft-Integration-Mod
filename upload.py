#!/usr/bin/env python3
import json
import os
import subprocess
import shlex
import pathlib
import re
import sys

# Read env vars (set by workflow)
mc_versions = json.loads(os.environ["MC_VERSIONS"])
name = os.environ["RELEASE_NAME"]
tag = os.environ["RELEASE_TAG"]
notes = os.environ["RELEASE_NOTES"]
token = os.environ["MODRINTH_TOKEN"]
project_id = os.environ["MODRINTH_PROJECT_ID"]

libs = pathlib.Path("build/libs")
jars = [p for p in libs.iterdir() if p.suffix == '.jar' and not p.name.endswith('-sources.jar')]
print(f"Found non-source JARs: {[p.name for p in jars]}")

# Regex: capture MC after -mc until version (e.g., 1.21.2)
mc_regex = re.compile(r'-mc([0-9.]+?)(?=\.\d+\.jar|-sources\.jar|$)')

published = 0
for jarpath in jars:
    match = mc_regex.search(jarpath.name)
    if match:
        mc = match.group(1)
        if mc in mc_versions:
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
            print(f"Skipping {jarpath.name}: MC {mc} not in versions")
    else:
        print(f"Skipping {jarpath.name}: no MC matched")

print(f"Published {published} versions.")
sys.exit(0 if published > 0 else 1)

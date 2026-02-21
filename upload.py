#!/usr/bin/env python3
import os
import subprocess
import shlex
import pathlib
import sys

name = os.environ["RELEASE_NAME"]
tag = os.environ["RELEASE_TAG"]
notes = os.environ["RELEASE_NOTES"]
token = os.environ["MODRINTH_TOKEN"]
project_id = os.environ["MODRINTH_PROJECT_ID"]

print("Installing modrinth-cli...")
subprocess.check_call(["npm", "install", "-g", "@modrinth/cli"])  # Official CLI

libs = pathlib.Path("build/libs")
jars = [p for p in libs.iterdir() if p.suffix == '.jar' and not p.name.endswith('-sources.jar')]
print(f"Found {len(jars)} non-source JARs")

mc_regex = re.compile(r'-mc([0-9.]+?)(?=\.\d+\.jar|$)')

published = 0
for jarpath in jars:
    match = mc_regex.search(jarpath.name)
    if match:
        mc = match.group(1)
        args = [
            "modrinth", "publish",  # Now direct binary
            "--token", token,
            "--project-id", project_id,
            "--name", f"{name} (MC {mc})",
            "--version-number", f"{tag}+mc{mc}",
            "--changelog", notes,
            "--loaders", "fabric",
            "--game-versions", mc,
            str(jarpath)
        ]
        print(f"Publishing {jarpath.name} for MC {mc}...")
        subprocess.check_call(args)  # Direct args, no bash
        published += 1
    else:
        print(f"Skipping {jarpath.name}")

print(f"Published {published}/{len(jars)} JARs.")

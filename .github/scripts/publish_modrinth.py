"""Upload the per-Minecraft-version JARs in build/libs to Modrinth.

Only JARs Modrinth does not have yet are uploaded: a JAR counts as published when
an existing Modrinth version already carries a file with the same name. Every push
to main therefore publishes just the newly added Minecraft versions, and bumping
mod_version publishes all of them again.

Environment: MODRINTH_TOKEN, MODRINTH_PROJECT_ID. Pass --dry-run to only print the
plan (needs no token).
"""

import json
import os
import pathlib
import sys
import urllib.request
import uuid

API = "https://api.modrinth.com/v2"
USER_AGENT = "Magenta-Mause/Cosy-Minecraft-Integration-Mod (github-actions)"
FABRIC_API_PROJECT_ID = "P7dR8mSH"


def read_properties(path):
    props = {}
    for line in pathlib.Path(path).read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def request(method, url, token=None, body=None, content_type=None):
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("User-Agent", USER_AGENT)
    if token:
        req.add_header("Authorization", token)
    if content_type:
        req.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(req) as res:
            return json.load(res)
    except urllib.error.HTTPError as e:
        sys.exit(f"{method} {url} failed: {e.code} {e.read().decode(errors='replace')}")


def multipart(data, jar):
    boundary = uuid.uuid4().hex
    parts = [
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="data"\r\n'
        "Content-Type: application/json\r\n\r\n".encode()
        + json.dumps(data).encode()
        + b"\r\n",
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{jar.name}"\r\n'
        "Content-Type: application/java-archive\r\n\r\n".encode()
        + jar.read_bytes()
        + b"\r\n",
        f"--{boundary}--\r\n".encode(),
    ]
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
    dry_run = "--dry-run" in sys.argv
    props = read_properties("gradle.properties")
    base_name = props["archives_base_name"]
    mod_version = props["mod_version"]
    mc_versions = [v.strip() for v in props["minecraft_versions"].split(",") if v.strip()]

    token = os.environ.get("MODRINTH_TOKEN")
    project_id = os.environ.get("MODRINTH_PROJECT_ID") or "cosy-integration-mod"
    if not dry_run and not token:
        sys.exit("MODRINTH_TOKEN is not set")

    existing = request("GET", f"{API}/project/{project_id}/version", token)
    published = {f["filename"] for v in existing for f in v["files"]}

    uploaded = 0
    for mc in mc_versions:
        jar = pathlib.Path("build/libs") / f"{base_name}-mc{mc}-{mod_version}.jar"
        if jar.name in published:
            print(f"skip   MC {mc}: {jar.name} is already on Modrinth")
            continue
        if not jar.is_file():
            sys.exit(f"missing build output {jar}")
        if dry_run:
            print(f"upload MC {mc}: {jar.name} (dry run)")
            continue

        data = {
            "project_id": project_id,
            "name": f"Cosy Integration Mod {mod_version} (MC {mc})",
            "version_number": f"{mod_version}+mc{mc}",
            "changelog": "",
            "dependencies": [
                {"project_id": FABRIC_API_PROJECT_ID, "dependency_type": "required"}
            ],
            "game_versions": [mc],
            "version_type": "release",
            "loaders": ["fabric"],
            "featured": False,
            "file_parts": ["file"],
            "primary_file": "file",
        }
        body, content_type = multipart(data, jar)
        created = request("POST", f"{API}/version", token, body, content_type)
        print(f"upload MC {mc}: {jar.name} -> version {created['id']}")
        uploaded += 1

    print(f"{uploaded} version(s) uploaded")


if __name__ == "__main__":
    main()

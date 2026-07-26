# Cosy Minecraft Integration Mod

> A server-side Fabric mod that periodically collects basic Minecraft server metrics and pushes them to the [Cosy](https://github.com/Magenta-Mause/Cosy) API over HTTP.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Magenta-Mause/Cosy-Minecraft-Integration-Mod)](../../releases)
[![Build, Release & Publish (main)](https://github.com/Magenta-Mause/Cosy-Minecraft-Integration-Mod/actions/workflows/build-and-push.yaml/badge.svg)](https://github.com/Magenta-Mause/Cosy-Minecraft-Integration-Mod/actions/workflows/build-and-push.yaml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.18%20%E2%80%93%2026.2-brightgreen.svg)](https://modmuss50.me/fabric.html)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-dbd0b4.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021-orange.svg)](https://adoptium.net/)

---

## Overview

**Cosy** ("Cost Optimized Server Yard") is a self-hostable platform for hosting and managing game
servers. This mod is the Minecraft side of that integration: it runs inside a Fabric **dedicated
server** and streams live server metrics (player count, world state, performance) back to the Cosy
backend so they can be displayed and monitored in the Cosy dashboard.

### What problem does it solve?

Cosy needs real-time visibility into the game servers it hosts. Rather than scraping logs or querying
the server externally, this mod runs *inside* the Minecraft server process, reads metrics directly from
the running `MinecraftServer`, and pushes them to the Cosy API on a fixed interval. The mod is
**server-only** (declared as `"environment": "server"` in `fabric.mod.json`) and fails gracefully — if
it is misconfigured or the Cosy API is unreachable, the Minecraft server keeps running normally.

### Key features

- Collects metrics directly from the running server (no external polling).
- Pushes metrics to the Cosy API on a configurable interval over HTTP(S) — the scheme comes from
  `COSY_BASE_URL`; the platform default is plain HTTP inside the Docker network.
- Runs a one-time connection test at startup and logs the result.
- Fully configured via environment variables — a natural fit for containerized hosting.
- Non-blocking: HTTP calls are asynchronous and errors are suppressed to avoid log spam.
- Multi-version build support (Minecraft 1.18 through 26.2) driven by `gradle.properties`.

### Related repositories

| Repository | Description |
|---|---|
| [Magenta-Mause/Cosy](https://github.com/Magenta-Mause/Cosy) | Main Cosy project (download / meta repo) |
| [Magenta-Mause/Cosy-Backend](https://github.com/Magenta-Mause/Cosy-Backend) | Cosy backend — exposes the API this mod talks to and injects this mod's environment variables |
| [Magenta-Mause/Cosy-Frontend](https://github.com/Magenta-Mause/Cosy-Frontend) | Web dashboard where the metrics this mod pushes are displayed |
| [Magenta-Mause/Cosy-Docs](https://github.com/Magenta-Mause/Cosy-Docs) | Official Cosy documentation ([cosy-hosting.net](https://cosy-hosting.net)) |
| [Magenta-Mause/.github](https://github.com/Magenta-Mause/.github) | Org-wide community health files |

---

## What metrics are collected

Metrics are assembled in `MetricsCollector`, represented as a `MetricsDto`, and serialized to JSON.
Nullable fields are omitted when unavailable.

| Field | Description |
|---|---|
| `playerCount` | Current number of online players |
| `currentDayTime` | Overworld time of day (`timeOfDay % 24000`) |
| `fullTime` | Overworld time of day, un-modulo'd (`timeOfDay`, the same source as `currentDayTime`) |
| `currentWeather` | `Clear`, `Raining`, or `Thundering` |
| `mspt` | Average milliseconds per tick (rolling window of 100 ticks) |
| `tps` | Ticks per second, derived from MSPT and capped at 20 |
| `msSinceEpoch` | System time in milliseconds when the sample was taken |

Overworld fields are included only when the overworld is loaded. `mspt`/`tps` are **always** included:
they are the mean over a fixed 100-tick ring buffer, so during the first ~100 ticks after start the mean
is diluted by still-empty slots — `mspt` reads artificially low and `tps` sits pinned at the 20 cap.

---

## Prerequisites

**To build the mod:**

- A JDK (Temurin/OpenJDK). The mod compiles to **Java 17** bytecode (`targetJavaVersion = 17`), but
  Minecraft 26.x is itself compiled for Java 25, so building the full version matrix needs **JDK 25**
  — which is what the CI pipeline uses.
- The bundled Gradle wrapper (`./gradlew`) — no separate Gradle install needed. Uses the **Fabric Loom**
  plugin.
- Network access on first build: the Gradle build resolves Loader/Fabric API versions from the
  [Fabric meta service](https://meta.fabricmc.net) and the mappings situation from Mojang's version
  manifest at configuration time.

**To run the mod:**

- A **Fabric dedicated server** for a supported Minecraft version (1.18 – 26.2; the default target is
  **26.2**).
- A recent **Fabric Loader** and the **Fabric API** mod. The exact minimum is baked in at build time
  (resolved from the Fabric meta service), so it moves with the build rather than being pinned here —
  JARs built today require Fabric Loader **>= 0.19.3**. Check `fabric.mod.json` inside your JAR for the
  authoritative requirement. Any recent **Fabric API** build for your Minecraft version works; the mod
  declares `"fabric-api": "*"`.
- **Java 17** runtime for Minecraft 1.18 – 1.20.4, **Java 21** for 1.20.5 – 1.21.11, **Java 25** for
  26.x. The mod itself targets Java 17 bytecode, so it runs on all of them — the floor comes from
  Minecraft, not from this mod.
- Reachable **Cosy backend** (see [Configuration](#configuration)).

---

## Building

Build the default Minecraft version (`minecraft_version` in `gradle.properties`):

```bash
./gradlew clean build
```

The resulting mod JAR is written to `build/libs/`. Use the file **without** a `-sources` (or `-dev`)
suffix — its name includes the Minecraft version, e.g. `cosyintegrationmod-mc26.2-1.0.jar`.

To build for **every** Minecraft version listed in `minecraft_versions`:

```bash
./gradlew buildAll
```

To build a single specific version, either override the property or use the generated per-version task:

```bash
./gradlew build -Pminecraft_version=1.20.1
# or
./gradlew buildMc1_20_1
```

---

## Installation

1. Set up a **Fabric dedicated server** for your Minecraft version and install the **Fabric API** mod.
2. [Build](#building) the mod (or grab a JAR from the
   [Releases](https://github.com/Magenta-Mause/Cosy-Minecraft-Integration-Mod/releases)).
3. Copy the built JAR (the one **without** `-sources`) into your server's `mods/` folder.
4. Provide the required [configuration](#configuration) as environment variables.
5. Start the server. On startup you should see a log line from `CosyIntegrationMod` reporting whether the
   connection test to Cosy succeeded.

---

## Configuration

The mod is configured entirely through **process environment variables** — there is no config file. When
run through the Cosy platform, Cosy injects these into the container automatically. For manual/local runs
you must export them yourself before starting the server.

See [`.env.example`](.env.example) for a copy-paste template.

### Required

| Variable | Description |
|---|---|
| `COSY_GAME_SERVER_UUID` | Identifier of the game server; used in the Cosy API path. |
| `COSY_CONTAINER_SECRET` | Secret token sent as the `Authorization` header on every request. |

### Optional

| Variable | Default | Description |
|---|---|---|
| `COSY_BASE_URL` | `http://host.docker.internal:8080` | Base URL of the Cosy backend. |
| `COSY_METRICS_PERIOD_SECONDS` | `2` | Interval between metric pushes. Non-positive values are forced to `5`. |

### Example (Linux / bash)

```bash
export COSY_GAME_SERVER_UUID="<your-server-uuid>"
export COSY_CONTAINER_SECRET="<your-container-secret>"
export COSY_BASE_URL="http://host.docker.internal:8080"
export COSY_METRICS_PERIOD_SECONDS="5"
```

> **Do not commit real secrets.** `COSY_CONTAINER_SECRET` is a credential — keep it out of source
> control and CI logs.

### How it talks to the Cosy API

Requests carry `Authorization: <COSY_CONTAINER_SECRET>` plus the appropriate `Content-Type` /
`Accept: application/json` headers. From the configured base URL the mod calls:

- **PUT** `{baseUrl}/api/internal/game-server/custom-metric/{gameServerUuid}` — sends the JSON metrics
  payload on each interval.
- **GET** `{baseUrl}/api/internal/game-server/test-connection/{gameServerUuid}` — one-time startup check;
  expects a JSON response with a boolean `data` field.

If the test request fails, the mod logs a hint about firewall rules (e.g. for UFW:
`sudo ufw allow in on docker0 to any port 8080 proto tcp`).

---

## Usage / Quick Start

```bash
# 1. Build
./gradlew clean build

# 2. Install the jar
cp build/libs/cosyintegrationmod-mc<version>-*.jar /path/to/server/mods/

# 3. Configure (see Configuration)
export COSY_GAME_SERVER_UUID="<your-server-uuid>"
export COSY_CONTAINER_SECRET="<your-container-secret>"

# 4. Start your Fabric server as usual
```

**Expected behavior:** at server start the mod logs
`Cosy metrics publisher started (period=Ns, uuid=...)` and either
`Successfully connected to Cosy (...)` or a connection-test warning. From then on it pushes a metrics
payload to the Cosy backend every `COSY_METRICS_PERIOD_SECONDS` seconds until the server stops.

---

## Project structure

```
Cosy-Minecraft-Integration-Mod/
├── build.gradle                    # Fabric Loom build; multi-version resolution logic
├── gradle.properties               # Minecraft / Fabric / mod versions
├── settings.gradle                 # Fabric maven plugin repositories
├── LICENSE                         # MIT
├── .github/workflows/              # CI: build+release; issue redirect
└── src/
    ├── main/
    │   ├── java/com/magentamause/cosyintegrationmod/
    │   │   ├── Cosyintegrationmod.java   # Mod entrypoint; wires server lifecycle events
    │   │   ├── CosyConfig.java           # Reads env vars; builds API URIs
    │   │   ├── Env.java                  # Environment-variable helpers
    │   │   ├── CosyClient.java           # Async HTTP client for the Cosy API
    │   │   ├── MetricsCollector.java     # Reads metrics from MinecraftServer
    │   │   ├── MetricsDto.java           # Metrics payload → JSON
    │   │   ├── MetricsPublisher.java     # Scheduled push loop
    │   │   └── TickTimeTracker.java      # Rolling MSPT/TPS measurement
    │   └── resources/
    │       ├── fabric.mod.json
    │       └── *.mixins.json
    ├── compat/                         # Per-era variants of LevelTime; one is added to
    │   ├── pre26/                      # the main source set depending on the target
    │   └── mc26/                       # Minecraft version
    └── client/resources/               # Client-side mixin config (unused at runtime; server-only mod)
```

---

## Available commands

Run these with the Gradle wrapper (`./gradlew <task>`):

| Command | Description |
|---|---|
| `./gradlew build` | Build the mod for the default Minecraft version. |
| `./gradlew clean` | Delete build outputs. |
| `./gradlew buildAll` | Build for every version in `minecraft_versions`. |
| `./gradlew buildMc<version_key>` | Build a single version (dots → underscores, e.g. `buildMc1_20_1`). |
| `./gradlew runServer` | Run a dev Fabric server with the mod (provided by Fabric Loom). |
| `./gradlew genSources` | Generate decompiled Minecraft sources for navigation (Loom). |

Use `./gradlew tasks` to list all available tasks.

---

## Development workflow

1. Import the project into an IDE with Gradle support (IntelliJ IDEA recommended for Fabric/Loom).
2. Make your changes under `src/main/java/...`.
3. Test locally against a dev server with `./gradlew runServer`, or build and drop the JAR into a real
   Fabric server's `mods/` folder.
4. Set the required environment variables (see [Configuration](#configuration)) before starting the
   server so the mod can reach Cosy.
5. Verify the startup log lines and confirm metrics are being received by the Cosy backend.

### Dependencies

- **Fabric Loader + Fabric API** — mod loader and the API surface used for server lifecycle and tick
  events.
- **Fabric Loom** (`fabric-loom` Gradle plugin) — builds/remaps the mod and manages Minecraft
  dependencies.
- **Gson** (provided transitively by Minecraft) — JSON serialization of the metrics payload.
- **Lombok** (`compileOnly` + `annotationProcessor`) — the `@Builder` on `MetricsDto`.
- Toolchain versions (Fabric Loader, Fabric API) are resolved automatically per Minecraft version from
  the Fabric meta service. Overrides go in `gradle.properties` under the keys `loader`/`fabric`,
  optionally suffixed with the Minecraft version (dots as underscores) — e.g. `loader_1_21_11`,
  `fabric_26_2`. Note that the existing `loader_version`/`fabric_version` keys are **not** consulted by
  the build and have no effect (tracked separately).
- Mappings depend on the era. Minecraft up to 1.21.11 is obfuscated and is built against **Mojang's
  official mappings** via the remapping Loom plugin; Minecraft 26.x ships unobfuscated, needs no
  mappings at all, and is built with the plain Loom plugin. The build picks the plugin, the mod
  dependency configuration and the matching `src/compat/...` variant of `LevelTime` automatically from
  Mojang's version manifest.

---

## Documentation

Full Cosy documentation lives at **[cosy-hosting.net](https://cosy-hosting.net)** and in the
[Cosy-Docs](https://github.com/Magenta-Mause/Cosy-Docs) repository.

---

## Contributing

Contributions are welcome! Organization-wide community health files live in the
[Magenta-Mause/.github](https://github.com/Magenta-Mause/.github) repository — a `CONTRIBUTING.md`
has not landed there yet, so until it does, [Development workflow](#development-workflow) below is the
authoritative guide.

**Reporting bugs & requesting features:** issues for this repository are tracked centrally in the main
Cosy repository. Please open new issues at
**[Magenta-Mause/Cosy → New issue](https://github.com/Magenta-Mause/Cosy/issues/new/choose)**. (Issues
opened directly here are automatically redirected and closed by a workflow.)

For local development setup, see [Development workflow](#development-workflow) above.

---

## Releases

Pushes to `main` trigger a CI pipeline that builds all Minecraft versions and publishes a GitHub
Release with the JARs attached (see `.github/workflows/build-and-push.yaml`). Released artifacts are
available under
[Releases](https://github.com/Magenta-Mause/Cosy-Minecraft-Integration-Mod/releases).

---

## License

Released under the **MIT License**. See [`LICENSE`](LICENSE) for the full text.

---

## Contact / Support

- **Documentation:** [cosy-hosting.net](https://cosy-hosting.net)
- **Community & support:** [Cosy Discord](https://discord.gg/nNtZJnSpSk)
- **Issues:** [Magenta-Mause/Cosy](https://github.com/Magenta-Mause/Cosy/issues/new/choose)

---

## Acknowledgments

- Built on [Fabric](https://fabricmc.net/) and [Fabric Loom](https://github.com/FabricMC/fabric-loom).
- Part of the [Cosy](https://github.com/Magenta-Mause/Cosy) project by Magenta-Mäuse.

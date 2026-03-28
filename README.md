# Cosy Minecraft Integration Mod

A **Fabric server-side mod** that periodically collects basic Minecraft server metrics and **pushes them to the [Cosy](https://github.com/Magenta-Mause/Cosy) API** over HTTP.

## What this project does

When the Minecraft **dedicated server starts**, the mod:

1. Loads configuration from environment variables (see [Environment setup](#environment-setup)).
2. Runs a **one-time connection test** against the Cosy API.
3. Starts a scheduled background task that:
    - collects metrics from the running `MinecraftServer`
    - sends them to the Cosy API every *N* seconds

When the server is stopping, it shuts down the scheduler cleanly.

This mod is marked as **server-only** via `fabric.mod.json` (`"environment": "server"`).

---

## What metrics are collected

The metrics payload is built in `MetricsCollector` and represented as a `MetricsDto`, then serialized to JSON.

Currently collected fields:

- `playerCount`
- Overworld info (only if the overworld is available):
    - `currentDayTime` (timeOfDay mod 24000)
    - `fullTime` (raw world time)
    - `currentWeather` (`Clear`, `Raining`, `Thundering`)
- Performance stats (only if tick times are available):
    - `mspt` (average milliseconds per tick)
    - `tps` (derived from MSPT, capped at 20)
- `msSinceEpoch` (current system time in millis)

---

## How it interacts with the Cosy API

### Authentication

Requests include:

- `Authorization: <COSY_CONTAINER_SECRET>`
- `Content-Type: application/json` (for metrics PUT)
- `Accept: application/json` (for test GET)

### Endpoints used

The base URL comes from `COSY_BASE_URL` and defaults to:

- `http://host.docker.internal:8080`

From that, the mod calls:

- **PUT** `{{baseUrl}}/api/internal/game-server/custom-metric/{{gameServerUuid}}`  
  Sends the JSON metrics payload periodically.

- **GET** `{{baseUrl}}/api/internal/game-server/test-connection/{{gameServerUuid}}`  
  One-time startup check. The code expects JSON with a boolean at `data`.

### Scheduling / timing

`MetricsPublisher` runs a single-threaded scheduled executor (`cosy-metrics-publisher`) and sends metrics at a fixed
rate:

- initial delay: `1` second
- period: `COSY_METRICS_PERIOD_SECONDS` (with a safety fallback if invalid)

Errors during publishing are intentionally suppressed to avoid log spam.

---

## Environment setup

This mod is designed to run in a **Docker container** hosted through [Cosy](https://github.com/magenta-Mause/Cosy).
Cosy injects the required environment variables into the container automatically.

The server uses these environment variables:

### Required

- `COSY_GAME_SERVER_UUID`  
  UUID/identifier of the game server used in the API path.

- `COSY_CONTAINER_SECRET`  
  Secret token sent as the `Authorization` header.

### Optional

- `COSY_BASE_URL`  
  Base URL of the Cosy service.  
  Default: `http://host.docker.internal:8080`

- `COSY_METRICS_PERIOD_SECONDS`  
  How often to publish metrics. Default is `2` (and invalid/<=0 values get forced to a safe value).

### Example (Linux / bash)

```bash
export COSY_GAME_SERVER_UUID="<your-server-uuid>" 
export COSY_CONTAINER_SECRET="<your-container-secret>"
export COSY_BASE_URL="http://host.docker.internal:8080"
export COSY_METRICS_PERIOD_SECONDS="5"
```

---

## Local testing (unofficial)

While this mod is designed for the Cosy platform, you can test it with a local Fabric server.

1.  Build the mod:
    ```bash
    ./gradlew clean build
    ```
2.  The JAR file will be in `build/libs/`. Look for the file **without** `-dev` or `-sources`.
3.  Copy this JAR into your local Minecraft server's `mods` folder.
4.  Set the environment variables as described above before starting your server.

---

## Build / tech notes

- Fabric Loom project (`fabric-loom`)
- Targets **Java 21**
- Minecraft / Fabric versions are configured in `gradle.properties`
- Lombok is present as `compileOnly` + `annotationProcessor`

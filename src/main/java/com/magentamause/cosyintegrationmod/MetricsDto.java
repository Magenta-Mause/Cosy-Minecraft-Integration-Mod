package com.magentamause.cosyintegrationmod;

import com.google.gson.JsonObject;
import lombok.Builder;

/**
 * DTO for custom metrics payload.
 * Nullable fields are omitted from JSON serialization via {@link #toJsonObject()}.
 */
@Builder
public final class MetricsDto {
    private final int playerCount;
    private final Long currentDayTime;
    private final Long fullTime;
    private final String currentWeather;

    // Milliseconds per tick https://spark.lucko.me/docs/guides/TPS-and-MSPT
    private final Double mspt;
    // Ticks per second https://spark.lucko.me/docs/guides/TPS-and-MSPT
    private final Double tps;

    private final long msSinceEpoch;

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();

        json.addProperty("playerCount", playerCount);

        if (currentDayTime != null) json.addProperty("currentDayTime", currentDayTime);
        if (fullTime != null) json.addProperty("fullTime", fullTime);
        if (currentWeather != null) json.addProperty("currentWeather", currentWeather);

        if (mspt != null) json.addProperty("mspt", mspt);
        if (tps != null) json.addProperty("tps", tps);

        json.addProperty("msSinceEpoch", msSinceEpoch);

        return json;
    }
}

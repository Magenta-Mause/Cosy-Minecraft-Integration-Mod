package com.magentamause.cosyintegrationmod;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.Arrays;

public final class MetricsCollector {

    public JsonObject collect(MinecraftServer server) {
        JsonObject json = new JsonObject();

        json.addProperty("playerCount", server.getPlayerManager().getPlayerList().size());

        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            long timeOfDay = overworld.getTimeOfDay();
            json.addProperty("currentDayTime", timeOfDay % 24000L);
            json.addProperty("fullTime", timeOfDay);
            json.addProperty("currentWeather", overworld.isThundering() ? "Thundering" : overworld.isRaining() ? "Raining" : "Clear");
        }

        // TPS/MSPT
        // Tick times are usually stored as nanoseconds for the last N ticks.
        long[] tickTimesNs = server.getTickTimes();
        if (tickTimesNs != null && tickTimesNs.length > 0) {
            double avgTickNs = Arrays.stream(tickTimesNs).average().orElse(0.0);
            double mspt = avgTickNs / 1_000_000.0;

            // Avoid division-by-zero; also cap at 20 TPS.
            double tps = (mspt > 0.000001) ? Math.min(20.0, 1000.0 / mspt) : 20.0;

            json.addProperty("mspt", mspt);
            json.addProperty("tps", tps);
        }

        json.addProperty("msSinceEpoch", System.currentTimeMillis());
        return json;
    }
}

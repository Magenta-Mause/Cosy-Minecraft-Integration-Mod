package com.magentamause.cosyintegrationmod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

public final class MetricsCollector {

    public MetricsDto collect(MinecraftServer server) {
        MetricsDto.MetricsDtoBuilder builder = MetricsDto.builder();

        builder.playerCount(server.getPlayerCount());

        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            long timeOfDay = LevelTime.dayTime(overworld);
            builder.currentDayTime(timeOfDay % 24000L);
            builder.fullTime(timeOfDay);
            builder.currentWeather(overworld.isThundering()
                    ? "Thundering" : overworld.isRaining() ? "Raining" : "Clear");
        }

        long[] tickTimesNs = TickTimeTracker.snapshotTickTimesNs();
        if (tickTimesNs != null && tickTimesNs.length > 0) {
            double avgTickNs = Arrays.stream(tickTimesNs).average().orElse(0.0);
            double mspt = avgTickNs / 1_000_000.0;
            builder.mspt(avgTickNs / 1_000_000.0);
            // Avoid division-by-zero; also cap at 20 TPS.
            builder.tps((mspt > 0.000001) ? Math.min(20.0, 1000.0 / mspt) : 20.0);
        }

        builder.msSinceEpoch(System.currentTimeMillis());

        return builder.build();
    }
}

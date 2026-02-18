package com.magentamause.cosyintegrationmod;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MetricsPublisher {

    private final CosyClient client;
    private final MetricsCollector collector = new MetricsCollector();

    private ScheduledExecutorService scheduler;

    public MetricsPublisher(CosyClient client) {
        this.client = client;
    }

    public void start(MinecraftServer server) {
        int periodSeconds = client.config().periodSeconds();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cosy-metrics-publisher");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                MetricsDto body = collector.collect(server);
                client.putCustomMetricsAsync(body.toJsonObject());
            } catch (Exception ignored) {
                // Intentionally quiet to avoid repeated log spam.
            }
        }, 1, periodSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}

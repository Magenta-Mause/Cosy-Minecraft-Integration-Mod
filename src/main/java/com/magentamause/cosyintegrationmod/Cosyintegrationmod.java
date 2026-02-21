package com.magentamause.cosyintegrationmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cosyintegrationmod implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("CosyIntegrationMod");

    private MetricsPublisher publisher;

    @Override
    public void onInitialize() {
        TickTimeTracker.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                CosyConfig config = CosyConfig.fromEnv();
                CosyClient client = new CosyClient(config);

                // Kick off a one-time connection test (async).
                client.testConnectionAsync(ok -> {
                    if (ok) {
                        LOGGER.info("Successfully connected to Cosy (uuid={})", config.gameServerUuid());
                    } else {
                        LOGGER.warn("Cosy connection test failed (uuid={}, requestSend={})", config.gameServerUuid(), config.testConnectionUri());
                    }
                });

                publisher = new MetricsPublisher(client);
                publisher.start(server);

                LOGGER.info("Cosy metrics publisher started (period={}s, uuid={})",
                        config.periodSeconds(), config.gameServerUuid());

            } catch (Exception e) {
                // If env vars are missing, we fail gracefully (server should still run).
                LOGGER.error("Cosy metrics publisher not started: {}", e.getMessage());
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (publisher != null) {
                publisher.stop();
                publisher = null;
                LOGGER.info("Cosy metrics publisher stopped");
            }
        });
    }
}

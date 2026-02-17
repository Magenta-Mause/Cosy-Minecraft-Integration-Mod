package com.magentamause.cosyintegrationmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

public final class CosyClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("CosyIntegrationMod");

    private final CosyConfig config;
    private final HttpClient httpClient;

    public CosyClient(CosyConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public CosyConfig config() {
        return config;
    }

    public void putCustomMetricsAsync(JsonObject metricsBody) {
        HttpRequest request = HttpRequest.newBuilder(config.customMetricsUri())
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(metricsBody.toString()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(resp -> {
                    // You can add logging here if you want non-2xx visibility.
                    // Intentionally kept quiet to avoid log spam every 5 seconds.
                })
                .exceptionally(ex -> null);
    }

    public void testConnectionAsync(Consumer<Boolean> callback) {
        HttpRequest request = HttpRequest.newBuilder(config.testConnectionUri())
                .timeout(Duration.ofSeconds(3))
                .header("Accept", "application/json")
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) return false;

                    String body = resp.body();
                    if (body == null || body.isBlank()) return false;

                    JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                    if (!obj.has("data") || obj.get("data").isJsonNull()) return false;

                    return obj.get("data").getAsBoolean();
                })
                .exceptionally(ex -> {
                    LOGGER.warn("Cosy test request failed: {}", ex.getMessage());
                    LOGGER.error("Cosy test request failed, this issue could relate to missing firewall rules. \n" +
                            " If you are using UFW you can use this command: \"sudo ufw allow in on docker0 to any port 8080 proto tcp\" to set the required firewall rules");
                    return false;
                })
                .thenAccept(callback);
    }
}

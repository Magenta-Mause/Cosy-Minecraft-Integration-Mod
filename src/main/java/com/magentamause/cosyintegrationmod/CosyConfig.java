package com.magentamause.cosyintegrationmod;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record CosyConfig(
        String baseUrl,
        String gameServerUuid,
        String containerSecret,
        int periodSeconds
) {
    public static CosyConfig fromEnv() {
        String uuid = Env.getRequired("COSY_GAME_SERVER_UUID");
        String secret = Env.getRequired("COSY_CONTAINER_SECRET");
        String baseUrl = Env.getOptional("COSY_BASE_URL").orElse("http://host.docker.internal:8080");
        int period = Env.getInt("COSY_METRICS_PERIOD_SECONDS", 2);
        if (period <= 0) period = 5;

        return new CosyConfig(baseUrl, uuid, secret, period);
    }

    public URI customMetricsUri() {
        return URI.create(baseUrl + "/api/internal/game-server/custom-metric/"
                + encodePathSegment(gameServerUuid) + "/"
                + encodePathSegment(containerSecret));
    }

    public URI testConnectionUri() {
        return URI.create(baseUrl + "/api/internal/game-server/test-connection/"
                + encodePathSegment(gameServerUuid) + "/"
                + encodePathSegment(containerSecret));
    }

    private static String encodePathSegment(String s) {
        // URLEncoder is for query params, but works fine for path segments if we fix spaces.
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

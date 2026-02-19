package com.magentamause.cosyintegrationmod;

import java.util.Optional;

public final class Env {
    public static String getRequired(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return v;
    }

    public static int getInt(String name, int defaultValue) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) return defaultValue;
        return Integer.parseInt(v);
    }

    public static Optional<String> getOptional(String name) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? Optional.empty() : Optional.of(v);
    }
}

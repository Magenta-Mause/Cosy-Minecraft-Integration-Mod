package com.magentamause.cosyintegrationmod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class TickTimeTracker {

    private static final int WINDOW = 100;
    private static final Object LOCK = new Object();
    private static final long[] TICK_TIMES_NS = new long[WINDOW];
    private static int tickIndex = 0;
    private static long tickStartNs = 0L;
    private static boolean initialized = false;

    private TickTimeTracker() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.START_SERVER_TICK.register(server -> tickStartNs = System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long end = System.nanoTime();
            long duration = tickStartNs == 0L ? 0L : end - tickStartNs;
            synchronized (LOCK) {
                TICK_TIMES_NS[tickIndex] = duration;
                tickIndex = (tickIndex + 1) % WINDOW;
            }
        });
    }

    public static long[] snapshotTickTimesNs() {
        synchronized (LOCK) {
            return TICK_TIMES_NS.clone();
        }
    }
}

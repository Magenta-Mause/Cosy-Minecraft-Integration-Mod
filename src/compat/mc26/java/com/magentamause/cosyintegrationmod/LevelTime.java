package com.magentamause.cosyintegrationmod;

import net.minecraft.server.level.ServerLevel;

/**
 * Day-time accessor for Minecraft 26.1 and later, where the day time lives in the
 * world-clock system instead of on the level. See the variant of this class in
 * {@code src/compat/pre26} for the older versions.
 */
final class LevelTime {

    private LevelTime() {
    }

    static long dayTime(ServerLevel level) {
        return level.getOverworldClockTime();
    }
}

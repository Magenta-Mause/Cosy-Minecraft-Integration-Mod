package com.magentamause.cosyintegrationmod;

import net.minecraft.server.level.ServerLevel;

/**
 * Day-time accessor for Minecraft up to 1.21.11, where the day time is stored on the
 * level itself. Minecraft 26.1 moved it into the world-clock system — see the variant
 * of this class in {@code src/compat/mc26}.
 */
final class LevelTime {

    private LevelTime() {
    }

    static long dayTime(ServerLevel level) {
        return level.getDayTime();
    }
}

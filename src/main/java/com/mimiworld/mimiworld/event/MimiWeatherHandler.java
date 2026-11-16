package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.ModDimensions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 米米维度天气规则：只有晴天和雨天，永不雷暴。
 *
 * <p>昼夜与晴雨周期由 dimension_type 和原版天气循环驱动，这里只压制雷暴：
 * 原版 {@code ServerLevel.advanceWeatherCycle} 在 thunderTime 倒数到 0 时翻转 isThundering，
 * 因此在每 tick 末尾把米米维度的雷暴剩余时间钳在正数，翻转永远不会发生；
 * 若其他来源已置为雷暴（如 /weather thunder）则立即清除并重置计时。雨天（米汤雨）保留。</p>
 */
public final class MimiWeatherHandler {

    /** 雷暴计时低于该值就续上，保证 advanceWeatherCycle 永远走不到翻转分支。 */
    private static final int THUNDER_TIME_FLOOR = 100;
    private static final int THUNDER_TIME_RESET = 12000;

    private MimiWeatherHandler() {
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().equals(ModDimensions.MIMI_DIMENSION)) {
            return;
        }
        // 26.3 把天气状态移到 SavedData（WeatherData）
        WeatherData weather = serverLevel.getWeatherData();
        if (weather.isThundering()) {
            weather.setThundering(false);
            weather.setThunderTime(THUNDER_TIME_RESET);
        } else if (weather.getThunderTime() <= THUNDER_TIME_FLOOR) {
            weather.setThunderTime(THUNDER_TIME_RESET);
        }
    }
}

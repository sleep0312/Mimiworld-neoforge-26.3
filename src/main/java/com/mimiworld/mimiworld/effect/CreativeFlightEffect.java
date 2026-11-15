package com.mimiworld.mimiworld.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 创造飞行（creative_flight）—— 正面药水效果。
 *
 * <p>该效果本身不直接做任何事：玩家的 mayfly 能力由
 * {@link com.mimiworld.mimiworld.event.CreativeFlightEvents} 在玩家 tick 中逐 tick 授予，
 * 效果结束后撤销。食用特定料理即可获得该效果。</p>
 */
public class CreativeFlightEffect extends MobEffect {

    public CreativeFlightEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

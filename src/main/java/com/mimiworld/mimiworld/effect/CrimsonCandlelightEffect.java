package com.mimiworld.mimiworld.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 腥红烛照（crimson_candlelight）—— 正面药水效果。
 *
 * <p>效果本身不做任何事，伤害倍率逻辑由
 * {@link com.mimiworld.mimiworld.event.DamagePipeline} 处理。</p>
 */
public class CrimsonCandlelightEffect extends MobEffect {

    public CrimsonCandlelightEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

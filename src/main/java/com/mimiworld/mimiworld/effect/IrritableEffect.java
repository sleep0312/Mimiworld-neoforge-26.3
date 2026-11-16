package com.mimiworld.mimiworld.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 易怒（irritable）—— 正面药水效果。
 *
 * <p>效果本身不做任何事，增伤逻辑由
 * {@link com.mimiworld.mimiworld.event.DamagePipeline} 处理。</p>
 */
public class IrritableEffect extends MobEffect {

    public IrritableEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

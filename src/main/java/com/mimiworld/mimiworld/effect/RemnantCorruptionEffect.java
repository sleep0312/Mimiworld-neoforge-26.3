package com.mimiworld.mimiworld.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 残堕化（remnant_corruption）—— 正面药水效果。
 *
 * <p>效果本身不做任何事，实际减伤由
 * {@link com.mimiworld.mimiworld.event.DamagePipeline} 处理：
 * 每级减少 10% 受到的伤害。</p>
 */
public class RemnantCorruptionEffect extends MobEffect {

    public RemnantCorruptionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

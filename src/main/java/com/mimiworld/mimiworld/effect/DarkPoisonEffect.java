package com.mimiworld.mimiworld.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 暗毒（dark_poison）—— 负面药水效果。
 *
 * <p>效果本身不做任何事，实际逻辑由
 * {@link com.mimiworld.mimiworld.event.ModEffectEvents} 在治疗事件中处理：
 * 携带者的一切生命恢复只保留 25%。</p>
 */
public class DarkPoisonEffect extends MobEffect {

    public DarkPoisonEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}

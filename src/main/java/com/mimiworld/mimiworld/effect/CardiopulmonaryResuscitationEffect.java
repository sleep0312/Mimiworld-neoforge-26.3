package com.mimiworld.mimiworld.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 心肺复苏（cardiopulmonary_resuscitation）—— 正面药水效果，仅 1 级。
 *
 * <p>每秒恢复 100 点生命值（受最大生命值上限约束；
 * 走 {@code LivingEntity#heal} 流程，因此会被「暗毒」等降低治疗量的效果影响）。</p>
 */
public class CardiopulmonaryResuscitationEffect extends MobEffect {

    private static final float HEAL_PER_SECOND = 100.0F;

    public CardiopulmonaryResuscitationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0 && duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.heal(HEAL_PER_SECOND);
        return true;
    }
}

package com.mimiworld.mimiworld.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 蟒蛇缠绕（python_constriction）—— 复晶天青蟒施加的负面效果。
 *
 * <p>每秒造成 {@code 10 × (amplifier + 1)} 点魔法伤害（无视护甲，可致死）；
 * 攻击方对复晶天青蟒的 150% 易伤由 DamagePipeline 处理。</p>
 */
public class PythonConstrictionEffect extends MobEffect {

    /** 每级每秒造成的基础伤害（amplifier 0 = I 级 → 10 点/秒）。 */
    private static final float DAMAGE_PER_LEVEL = 10.0F;

    public PythonConstrictionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /** 每 20 tick（1 秒）结算一次，与等级无关。 */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0 && duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.hurtServer(level, entity.damageSources().magic(), DAMAGE_PER_LEVEL * (amplifier + 1));
        return true;
    }
}

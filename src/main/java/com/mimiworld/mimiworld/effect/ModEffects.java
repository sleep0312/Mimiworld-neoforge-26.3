package com.mimiworld.mimiworld.effect;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 米米世界药水效果注册类。
 *
 * <p>注册：创造飞行、腥红烛照、蟒蛇缠绕、残堕化、易怒、暗毒、心肺复苏。</p>
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MimiWorld.MODID);

    /**
     * 创造飞行 —— 正面效果。食用特定料理获得，效果本身不做任何事，
     * 玩家的 mayfly 能力由 {@link com.mimiworld.mimiworld.event.CreativeFlightEvents}
     * 在玩家 tick 中逐 tick 授予，效果结束后撤销。颜色 #79E0F5 天蓝。
     */
    public static final DeferredHolder<MobEffect, MobEffect> CREATIVE_FLIGHT = MOB_EFFECTS.register(
            "creative_flight",
            () -> new CreativeFlightEffect(MobEffectCategory.BENEFICIAL, 0xFF79E0F5));

    /** 腥红烛照 —— 正面增益，伤害倍率由 DamagePipeline 处理。颜色 #E63946 暗红。 */
    public static final DeferredHolder<MobEffect, MobEffect> CRIMSON_CANDLELIGHT = MOB_EFFECTS.register(
            "crimson_candlelight",
            () -> new CrimsonCandlelightEffect(MobEffectCategory.BENEFICIAL, 0xFFE63946));

    /** 蟒蛇缠绕 —— 负面效果，每秒 10×等级魔法伤害。颜色 #2D6A4F 深绿。 */
    public static final DeferredHolder<MobEffect, MobEffect> PYTHON_CONSTRICTION = MOB_EFFECTS.register(
            "python_constriction",
            () -> new PythonConstrictionEffect(MobEffectCategory.HARMFUL, 0xFF2D6A4F));

    /** 残堕化 —— 正面效果，每级减伤 10%。颜色 #A02A2A 残堕暗红。 */
    public static final DeferredHolder<MobEffect, MobEffect> REMNANT_CORRUPTION = MOB_EFFECTS.register(
            "remnant_corruption",
            () -> new RemnantCorruptionEffect(MobEffectCategory.BENEFICIAL, 0xFFA02A2A));

    /** 易怒 —— 正面效果，受伤后 1 秒内攻击 25% 概率增伤。颜色 #F4511E 橙红。 */
    public static final DeferredHolder<MobEffect, MobEffect> IRRITABLE = MOB_EFFECTS.register(
            "irritable",
            () -> new IrritableEffect(MobEffectCategory.BENEFICIAL, 0xFFF4511E));

    /** 暗毒 —— 负面效果，生命恢复量 -75%。颜色 #4B2A5E 暗紫。 */
    public static final DeferredHolder<MobEffect, MobEffect> DARK_POISON = MOB_EFFECTS.register(
            "dark_poison",
            () -> new DarkPoisonEffect(MobEffectCategory.HARMFUL, 0xFF4B2A5E));

    /** 心肺复苏 —— 正面效果，每秒恢复 100 点生命值。颜色 #E0393B 鲜红。 */
    public static final DeferredHolder<MobEffect, MobEffect> CARDIOPULMONARY_RESUSCITATION = MOB_EFFECTS.register(
            "cardiopulmonary_resuscitation",
            () -> new CardiopulmonaryResuscitationEffect(MobEffectCategory.BENEFICIAL, 0xFFE0393B));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}

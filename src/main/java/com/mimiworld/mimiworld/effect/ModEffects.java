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
 * <p>本批次仅移植食物所需的「创造飞行」效果；其余战斗/Boss 效果会在对应系统移植时补入。</p>
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

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}

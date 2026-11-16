package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.effect.ModEffects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 创造飞行效果的玩家 tick 处理。
 *
 * <p>拥有 {@code creative_flight} 效果时授予 mayfly；效果结束后撤销。
 * 用 persistentData 记录「mayfly 是否由本效果授予」，避免误撤创造模式的飞行能力。</p>
 */
public final class CreativeFlightEvents {
    private static final String FOOD_FLIGHT_FLAG = "mimiworld.food_creative_flight";

    private CreativeFlightEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isSpectator()) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        if (player.hasEffect(ModEffects.CREATIVE_FLIGHT)) {
            ArmorEffectEvents.setMayFly(player, true);
            data.putBoolean(FOOD_FLIGHT_FLAG, true);
        } else if (data.getBoolean(FOOD_FLIGHT_FLAG).orElse(false)) {
            if (!player.getAbilities().instabuild && !ArmorEffectEvents.wearsFlightArmor(player)) {
                ArmorEffectEvents.setMayFly(player, false);
            }
            data.putBoolean(FOOD_FLIGHT_FLAG, false);
        }
    }
}

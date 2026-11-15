package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.effect.ModEffects;
import com.mimiworld.mimiworld.item.MimiArmorItem;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 米米世界防具的「穿戴持续药水效果 / 创造飞行」处理器。
 *
 * <p>服务端玩家 tick 中逐 tick 检查穿戴的防具单件：</p>
 * <ul>
 *   <li><b>持续生效</b>：把仍在穿戴的效果刷新到 8 秒；仅在剩余 ≤3 秒时重置，即每 5 秒刷新一次。</li>
 *   <li><b>脱下清理</b>：只清理 ambient（由防具灌注）且剩余时长不超过刷新阈值的残留效果，
 *       不影响玩家自饮药水或饰品提供的效果。</li>
 *   <li><b>创造飞行</b>：穿戴飞行防具时维持 mayfly，脱下且无其它飞行来源时撤销。</li>
 * </ul>
 * 最大生命值不在此处理，由各防具物品的属性修饰符直接增删。
 */
public final class ArmorEffectEvents {

    private static final EquipmentSlot[] ARMOR_SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** 穿戴期间药水效果的刷新时长（8 秒）与刷新阈值（剩余 ≤3 秒）。 */
    private static final int EFFECT_DURATION = 160;
    private static final int EFFECT_REFRESH_REMAINING = 60;

    /** 记录玩家当前的 mayfly 是否由「飞行防具」授予，用于脱下后精确撤销。 */
    private static final String ARMOR_FLIGHT_FLAG = "mimiworld.armor_creative_flight";

    /** 防具可能提供的全部药水效果类型，用于脱下后清理残留效果。 */
    private static final Set<Holder<MobEffect>> ARMOR_EFFECT_TYPES = Set.of(
            MobEffects.REGENERATION,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.SPEED,
            MobEffects.NIGHT_VISION,
            MobEffects.STRENGTH,
            MobEffects.WATER_BREATHING,
            MobEffects.HASTE,
            MobEffects.JUMP_BOOST);

    private ArmorEffectEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }

        Map<Holder<MobEffect>, Integer> desired = new HashMap<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            Item item = stack.getItem();
            if (item instanceof MimiArmorItem armor) {
                armor.getWornEffects().forEach((effect, amp) -> desired.merge(effect, amp, Math::max));
            }
        }

        desired.forEach((effect, amp) -> {
            MobEffectInstance current = player.getEffect(effect);
            if (current == null || current.getAmplifier() < amp
                    || (current.getAmplifier() == amp && current.getDuration() <= EFFECT_REFRESH_REMAINING)) {
                player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amp, true, true, true));
            }
        });

        for (Holder<MobEffect> effect : ARMOR_EFFECT_TYPES) {
            if (desired.containsKey(effect)) {
                continue;
            }
            MobEffectInstance instance = player.getEffect(effect);
            if (instance != null && instance.isAmbient() && instance.getDuration() <= EFFECT_DURATION) {
                player.removeEffect(effect);
            }
        }

        CompoundTag data = player.getPersistentData();
        if (wearsFlightArmor(player)) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            data.putBoolean(ARMOR_FLIGHT_FLAG, true);
        } else if (data.getBoolean(ARMOR_FLIGHT_FLAG).orElse(false)) {
            if (!player.getAbilities().instabuild && !player.hasEffect(ModEffects.CREATIVE_FLIGHT)) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            data.putBoolean(ARMOR_FLIGHT_FLAG, false);
        }
    }

    /** 玩家是否穿着任一提供创造飞行的防具部件。 */
    public static boolean wearsFlightArmor(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof MimiArmorItem armor && armor.grantsFlight()) {
                return true;
            }
        }
        return false;
    }
}

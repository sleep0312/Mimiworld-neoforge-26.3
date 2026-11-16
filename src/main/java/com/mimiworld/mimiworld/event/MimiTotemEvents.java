package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.ModItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 米米图腾的免死结算。
 *
 * <p>原版不死图腾的判定硬编码在 {@code LivingEntity#checkTotemDeathProtection} 中，
 * 这里改用 NeoForge 的伤害前事件复刻同样的行为：当即将受到的伤害足以致死，
 * 且主手/副手持有米米图腾时，取消本次伤害并消耗一个图腾，回 1 点血、清空效果，
 * 附加再生 II 45s / 伤害吸收 II 5s / 抗火 I 40s，并播放图腾音效与粒子。</p>
 */
public final class MimiTotemEvents {

    private MimiTotemEvents() {
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (event.getAmount() < entity.getHealth()) {
            return;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = entity.getItemInHand(hand);
            if (!stack.is(ModItems.MIMI_TOTEM.get())) {
                continue;
            }
            event.setCanceled(true);
            stack.shrink(1);
            entity.setHealth(1.0F);
            entity.removeAllEffects();
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            entity.level().broadcastEntityEvent(entity, (byte) 35);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }
    }
}

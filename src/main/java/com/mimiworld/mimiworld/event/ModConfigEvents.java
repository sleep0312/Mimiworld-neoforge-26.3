package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.ModConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * 数值配置的运行时接线。
 *
 * <ul>
 *   <li>实体加入世界时，把 {@code [mobs.*]} 的数值写入其基础属性；</li>
 *   <li>{@code [combat]} 的攻防倍率由 {@link DamagePipeline} 在阶段 1 结算，只作用于本模组生物；</li>
 *   <li>{@code [drops]} 的掉落倍率在 LivingDropsEvent 中按物品堆叠数量结算。</li>
 * </ul>
 */
public final class ModConfigEvents {

    private ModConfigEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob) {
            ModConfig.applyMobStats(mob);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isModMob(entity)) {
            return;
        }
        double multiplier = ModConfig.lootMultiplier();
        if (multiplier == 1.0D) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            int count = (int) Math.round(stack.getCount() * multiplier);
            if (count <= 0) {
                drop.discard();
            } else {
                stack.setCount(Math.min(count, stack.getMaxStackSize()));
            }
        }
    }

    /** 实体是否为本模组生物（掉落倍率与 {@link DamagePipeline} 的攻防倍率共用）。 */
    public static boolean isModMob(Entity entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && MimiWorld.MODID.equals(key.getNamespace());
    }
}

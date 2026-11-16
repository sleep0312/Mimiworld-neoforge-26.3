package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.ModItems;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/**
 * 领域展开：以佩戴者为中心 10×10×10 范围内的敌对生物护甲值 -50%。
 */
public final class DomainExpansionEvents {

    private DomainExpansionEvents() {
    }

    /** 10×10×10 立方体即每轴 ±5 格。 */
    private static final double RANGE = 5.0D;

    private static final Identifier ARMOR_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "domain_expansion.armor");
    private static final AttributeModifier ARMOR_CUT = new AttributeModifier(
            ARMOR_MODIFIER_ID, -0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private static final List<Player> ACTIVE_PLAYERS = new ArrayList<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ACTIVE_PLAYERS.clear();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (isEquipped(player)) {
                ACTIVE_PLAYERS.add(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        boolean active = !ACTIVE_PLAYERS.isEmpty() && isHostile(entity) && isInsideDomain(entity);
        boolean applied = armor.getModifier(ARMOR_MODIFIER_ID) != null;
        if (active) {
            if (!applied) {
                armor.addTransientModifier(ARMOR_CUT);
            }
        } else if (applied) {
            armor.removeModifier(ARMOR_MODIFIER_ID);
        }
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity.isAlive()
                && (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER);
    }

    private static boolean isInsideDomain(LivingEntity entity) {
        for (Player player : ACTIVE_PLAYERS) {
            if (player.level() == entity.level()
                    && Math.abs(entity.getX() - player.getX()) <= RANGE
                    && Math.abs(entity.getY() - player.getY()) <= RANGE
                    && Math.abs(entity.getZ() - player.getZ()) <= RANGE) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEquipped(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(ModItems.DOMAIN_EXPANSION.get()))
                .orElse(false);
    }
}

package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.ModConfig;
import com.mimiworld.mimiworld.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 猩红竖眼事件：21×21×21 范围内怪物发光，以及客户端按键请求触发的「传送到发光怪物身后」。
 */
public final class CrimsonEyeEvents {

    private CrimsonEyeEvents() {
    }

    /** 范围半径：21×21×21 立方体即每轴 ±10.5 格。 */
    private static final double RANGE = 10.5D;
    /** 每 1 秒刷新一次发光，效果持续 2 秒。 */
    private static final int GLOW_REFRESH_TICKS = 20;
    private static final int GLOW_DURATION_TICKS = 40;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!isEquipped(player) || player.tickCount % GLOW_REFRESH_TICKS != 0) {
            return;
        }
        for (LivingEntity monster : findMonsters(player.level(), glowArea(player))) {
            monster.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    GLOW_DURATION_TICKS, 0, false, false, false));
        }
    }

    private static boolean isMonster(LivingEntity entity) {
        return entity.isAlive()
                && (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER);
    }

    private static List<LivingEntity> findMonsters(Level level, AABB area) {
        return level.getEntitiesOfClass(LivingEntity.class, area, CrimsonEyeEvents::isMonster);
    }

    private static boolean isEquipped(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(ModItems.CRIMSON_VERTICAL_EYE.get()))
                .orElse(false);
    }

    private static AABB glowArea(Player player) {
        return new AABB(
                player.getX() - RANGE, player.getY() - RANGE, player.getZ() - RANGE,
                player.getX() + RANGE, player.getY() + RANGE, player.getZ() + RANGE);
    }

    /** 按键请求入口：挑选最近的发光怪物，逐只尝试传送到其身后。 */
    public static void teleportToGlowingMonster(ServerPlayer player) {
        if (player.isDeadOrDying() || player.isSpectator() || player.isPassenger()) {
            return;
        }
        if (!isEquipped(player)) {
            sendHint(player, "message.mimiworld.crimson_vertical_eye.not_equipped", ChatFormatting.RED);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, glowArea(player),
                monster -> isMonster(monster) && monster.hasEffect(MobEffects.GLOWING));
        if (candidates.isEmpty()) {
            sendHint(player, "message.mimiworld.crimson_vertical_eye.no_target", ChatFormatting.RED);
            return;
        }
        candidates.sort(Comparator.comparingDouble(monster -> monster.distanceToSqr(player)));
        for (LivingEntity monster : candidates) {
            if (teleportBehind(player, level, monster)) {
                sendHint(player, "message.mimiworld.crimson_vertical_eye.success", ChatFormatting.GREEN);
                return;
            }
        }
        sendHint(player, "message.mimiworld.crimson_vertical_eye.blocked", ChatFormatting.RED);
    }

    private static void sendHint(ServerPlayer player, String key, ChatFormatting color) {
        if (!ModConfig.crimsonEyeTeleportHint()) {
            return;
        }
        player.sendSystemMessage(Component.translatable(key).withStyle(color), true);
    }

    private static boolean teleportBehind(ServerPlayer player, ServerLevel level, LivingEntity monster) {
        double offset = 1.0D + monster.getBbWidth() * 0.5D;
        Vec3 dest = monster.position().subtract(monster.getLookAngle().scale(offset));
        Vec3 destEye = dest.add(0.0D, player.getEyeHeight(), 0.0D);

        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), destEye,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            return false;
        }
        AABB destBox = player.getDimensions(player.getPose()).makeBoundingBox(dest.x, dest.y, dest.z);
        if (!level.noCollision(player, destBox)) {
            return false;
        }

        Vec3 facing = monster.getEyePosition().subtract(destEye);
        if (facing.lengthSqr() < 1.0E-4D) {
            return false;
        }
        float yaw = (float) (Mth.atan2(facing.z, facing.x) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(facing.y,
                Math.sqrt(facing.x * facing.x + facing.z * facing.z)) * (180.0D / Math.PI));
        player.teleportTo(level, dest.x, dest.y, dest.z, Set.<Relative>of(), yaw, pitch, false);
        player.setDeltaMovement(Vec3.ZERO);
        return true;
    }
}

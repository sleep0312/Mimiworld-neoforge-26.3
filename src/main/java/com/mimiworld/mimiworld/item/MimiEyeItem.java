package com.mimiworld.mimiworld.item;

import com.mimiworld.mimiworld.ModDimensions;
import com.mimiworld.mimiworld.world.MimiPortalTeleporter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * 米米之眼（Eye of Mimi）：激活米米传送器用的插眼道具，同时是米米维度内的返程钥匙。
 *
 * <p>在米米维度手持右键：传送回主世界中进入米米维度时踩过的那座米米传送门旁；
 * 入口记录缺失时回退到主世界出生点地表并给出提示。</p>
 */
public class MimiEyeItem extends Item {

    public MimiEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.dimension().equals(ModDimensions.MIMI_DIMENSION)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        ServerLevel overworld = serverPlayer.level().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return InteractionResult.FAIL;
        }
        Vec3 landing = MimiPortalTeleporter.findReturnLanding(serverPlayer, overworld);
        boolean atPortal = landing != null;
        if (!atPortal) {
            landing = MimiPortalTeleporter.spawnLanding(overworld);
        }
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        overworld.playSound(null, landing.x, landing.y, landing.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.teleport(new TeleportTransition(overworld, landing, Vec3.ZERO,
                serverPlayer.getYRot(), serverPlayer.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
        if (!atPortal) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.mimiworld.mimi_eye.no_portal"), true);
        }
        return InteractionResult.SUCCESS;
    }
}

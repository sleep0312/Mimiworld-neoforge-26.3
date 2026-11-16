package com.mimiworld.mimiworld.world;

import com.mimiworld.mimiworld.ModDimensions;
import com.mimiworld.mimiworld.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * 残堕界传送门的维度传送器。
 *
 * <p>去程：进入残堕界时确保「降临平台」存在，并把实体放在平台角落。</p>
 * <p>返程：点对点回到进入时那座传送门旁边；记录缺失时回退到世界出生点地表。</p>
 */
public final class RemnantFallenTeleporter {

    private RemnantFallenTeleporter() {
    }

    private static final BlockPos PLATFORM_CORNER = new BlockPos(0, 64, 0);
    private static final int PLATFORM_CLEAR_HEIGHT = 4;
    private static final String TAG_ENTRY_POS = "mimiworld:remnant_fallen_entry_pos";
    private static final String TAG_ENTRY_DIM = "mimiworld:remnant_fallen_entry_dim";
    private static final int LANDING_SEARCH_RADIUS = 3;

    public static void rememberEntry(Entity entity, Level from, BlockPos portalPos) {
        CompoundTag data = entity.getPersistentData();
        data.putLong(TAG_ENTRY_POS, portalPos.asLong());
        data.putString(TAG_ENTRY_DIM, from.dimension().identifier().toString());
    }

    public static void teleport(Entity entity, ServerLevel from,
                                net.minecraft.resources.ResourceKey<Level> target, BlockPos entryPos) {
        ServerLevel dest = from.getServer().getLevel(target);
        if (dest == null) {
            return;
        }
        if (!from.dimension().equals(ModDimensions.REMNANT_FALLEN_REALM)) {
            rememberEntry(entity, from, entryPos);
        }
        Vec3 landing;
        if (target.equals(ModDimensions.REMNANT_FALLEN_REALM)) {
            BlockPos arrival = ensureArrivalPlatform(dest);
            landing = new Vec3(arrival.getX() + 0.5D, arrival.getY() + 1.0D, arrival.getZ() + 0.5D);
        } else {
            landing = returnLanding(entity, dest);
        }
        entity.teleport(new TeleportTransition(dest, landing, Vec3.ZERO,
                entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
    }

    private static Vec3 returnLanding(Entity entity, ServerLevel destWorld) {
        BlockPos entry = recallEntry(entity, destWorld);
        if (entry != null) {
            BlockPos landing = findSafeLanding(destWorld, entry);
            if (landing != null) {
                return new Vec3(landing.getX() + 0.5D, landing.getY() + 1.0D, landing.getZ() + 0.5D);
            }
        }
        BlockPos spawn = destWorld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                destWorld.getRespawnData().pos());
        return new Vec3(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
    }

    @Nullable
    private static BlockPos recallEntry(Entity entity, ServerLevel destWorld) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG_ENTRY_POS) || !data.contains(TAG_ENTRY_DIM)) {
            return null;
        }
        if (!data.getStringOr(TAG_ENTRY_DIM, "").equals(destWorld.dimension().identifier().toString())) {
            return null;
        }
        long pos = data.getLongOr(TAG_ENTRY_POS, Long.MIN_VALUE);
        return pos == Long.MIN_VALUE ? null : BlockPos.of(pos);
    }

    @Nullable
    private static BlockPos findSafeLanding(ServerLevel level, BlockPos entry) {
        if (!level.getBlockState(entry).is(ModBlocks.REMNANT_FALLEN_PORTAL.get())) {
            return null;
        }
        for (int radius = 1; radius <= LANDING_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    BlockPos candidate = entry.offset(dx, 0, dz);
                    if (isSafeStand(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeStand(ServerLevel level, BlockPos standOn) {
        BlockState ground = level.getBlockState(standOn);
        if (ground.is(ModBlocks.REMNANT_FALLEN_PORTAL.get())
                || !ground.isFaceSturdy(level, standOn, Direction.UP)) {
            return false;
        }
        return isClear(level, standOn.above()) && isClear(level, standOn.above(2));
    }

    private static boolean isClear(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.is(ModBlocks.REMNANT_FALLEN_PORTAL.get())
                && state.getCollisionShape(level, pos).isEmpty();
    }

    private static BlockPos ensureArrivalPlatform(ServerLevel level) {
        BlockState rock = ModBlocks.REMNANT_FALLEN_ABYSS_ROCK.get().defaultBlockState();
        BlockState portal = ModBlocks.REMNANT_FALLEN_PORTAL.get().defaultBlockState();
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j <= 3; j++) {
                BlockPos pos = PLATFORM_CORNER.offset(i, 0, j);
                boolean center = (i == 1 || i == 2) && (j == 1 || j == 2);
                BlockState target = center ? portal : rock;
                if (!level.getBlockState(pos).equals(target)) {
                    level.setBlock(pos, target, 3);
                }
                for (int dy = 1; dy <= PLATFORM_CLEAR_HEIGHT; dy++) {
                    BlockPos above = pos.offset(0, dy, 0);
                    if (!level.getBlockState(above).isAir()) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        return PLATFORM_CORNER;
    }
}

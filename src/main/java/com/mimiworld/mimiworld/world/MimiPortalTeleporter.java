package com.mimiworld.mimiworld.world;

import com.mimiworld.mimiworld.ModDimensions;
import com.mimiworld.mimiworld.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * 米米传送门的维度传送器（26.3 使用 {@link TeleportTransition}）。
 *
 * <p>去程：进入米米维度时落在维度出生点地表，并把入口传送门的维度与坐标记在实体持久化数据里。</p>
 * <p>返程：点对点回到进入时那座传送门旁边；记录缺失/传送门被拆除时回退到世界出生点地表。</p>
 */
public final class MimiPortalTeleporter {

    private MimiPortalTeleporter() {
    }

    private static final String TAG_ENTRY_POS = "mimiworld:mimi_entry_pos";
    private static final String TAG_ENTRY_DIM = "mimiworld:mimi_entry_dim";
    private static final int LANDING_SEARCH_RADIUS = 3;

    public static void rememberEntry(Entity entity, Level from, BlockPos portalPos) {
        CompoundTag data = entity.getPersistentData();
        data.putLong(TAG_ENTRY_POS, portalPos.asLong());
        data.putString(TAG_ENTRY_DIM, from.dimension().identifier().toString());
    }

    /** 把实体传送到米米维度或主世界。 */
    public static void teleport(Entity entity, ServerLevel from, net.minecraft.resources.ResourceKey<Level> target,
                                BlockPos entryPos) {
        ServerLevel dest = from.getServer().getLevel(target);
        if (dest == null) {
            return;
        }
        if (!from.dimension().equals(ModDimensions.MIMI_DIMENSION)) {
            rememberEntry(entity, from, entryPos);
        }
        Vec3 landing = target.equals(ModDimensions.MIMI_DIMENSION)
                ? surfaceSpawn(dest)
                : returnLanding(entity, dest);
        entity.teleport(new TeleportTransition(dest, landing, Vec3.ZERO,
                entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
    }

    private static Vec3 returnLanding(Entity entity, ServerLevel destWorld) {
        Vec3 landing = findReturnLanding(entity, destWorld);
        return landing != null ? landing : surfaceSpawn(destWorld);
    }

    /** 返程落点：入口传送门旁的安全落脚位置；入口记录缺失或传送门已拆除时返回 null。 */
    @Nullable
    public static Vec3 findReturnLanding(Entity entity, ServerLevel destWorld) {
        BlockPos entry = recallEntry(entity, destWorld);
        if (entry == null) {
            return null;
        }
        BlockPos landing = findSafeLanding(destWorld, entry);
        return landing == null ? null
                : new Vec3(landing.getX() + 0.5D, landing.getY() + 1.0D, landing.getZ() + 0.5D);
    }

    public static Vec3 spawnLanding(ServerLevel level) {
        return surfaceSpawn(level);
    }

    private static Vec3 surfaceSpawn(ServerLevel level) {
        BlockPos spawn = level.getRespawnData().pos();
        int x = spawn.getX();
        int z = spawn.getZ();
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY() + 1) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, level.getMaxY() - 1, z);
            while (cursor.getY() > level.getMinY()
                    && level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                cursor.move(Direction.DOWN);
            }
            y = cursor.getY() + 1;
        }
        return new Vec3(x + 0.5D, y, z + 0.5D);
    }

    @Nullable
    private static BlockPos recallEntry(Entity entity, ServerLevel destWorld) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG_ENTRY_POS) || !data.contains(TAG_ENTRY_DIM)) {
            return null;
        }
        String dim = data.getStringOr(TAG_ENTRY_DIM, "");
        if (!dim.equals(destWorld.dimension().identifier().toString())) {
            return null;
        }
        long pos = data.getLongOr(TAG_ENTRY_POS, Long.MIN_VALUE);
        return pos == Long.MIN_VALUE ? null : BlockPos.of(pos);
    }

    @Nullable
    private static BlockPos findSafeLanding(ServerLevel level, BlockPos entry) {
        if (!level.getBlockState(entry).is(ModBlocks.MIMI_PORTAL.get())) {
            return null;
        }
        BlockPos base = entry;
        int guard = 0;
        while (level.getBlockState(base).is(ModBlocks.MIMI_PORTAL.get()) && guard++ < 16) {
            base = base.below();
        }
        for (int radius = 1; radius <= LANDING_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    BlockPos candidate = base.offset(dx, 0, dz);
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
        if (ground.is(ModBlocks.MIMI_PORTAL.get()) || !ground.isFaceSturdy(level, standOn, Direction.UP)) {
            return false;
        }
        if (!isClear(level, standOn.above()) || !isClear(level, standOn.above(2))) {
            return false;
        }
        BlockPos feet = standOn.above();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getBlockState(feet.offset(dx, 0, dz)).is(ModBlocks.MIMI_PORTAL.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isClear(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.is(ModBlocks.MIMI_PORTAL.get()) && state.getCollisionShape(level, pos).isEmpty();
    }
}

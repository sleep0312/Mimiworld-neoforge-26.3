package com.mimiworld.mimiworld.block;

import com.mimiworld.mimiworld.ModDimensions;
import com.mimiworld.mimiworld.world.MimiPortalTeleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 米米传送门方块（mimi_portal）：仪式激活后生成在框架中心，实体踩入即双向传送。
 */
public class MimiPortalBlock extends Block {

    public MimiPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(level instanceof ServerLevel serverLevel)
                || entity.isRemoved()
                || entity.level() != level) {
            return;
        }
        ResourceKey<Level> target = serverLevel.dimension().equals(ModDimensions.MIMI_DIMENSION)
                ? Level.OVERWORLD
                : ModDimensions.MIMI_DIMENSION;
        ServerLevel dest = serverLevel.getServer().getLevel(target);
        if (dest == null || !entity.canTeleport(serverLevel, dest)) {
            return;
        }
        MimiPortalTeleporter.teleport(entity, serverLevel, target, pos);
    }

    /** 地狱门式级联：传送门方块被移除时相邻传送门方块一起碎掉。 */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.is(com.mimiworld.mimiworld.block.ModBlocks.MIMI_PORTAL.get())) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = pos.relative(direction);
                if (level.getBlockState(neighbour).is(com.mimiworld.mimiworld.block.ModBlocks.MIMI_PORTAL.get())) {
                    level.removeBlock(neighbour, false);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation") // 26.3 暂未提供该钩子的替代方法
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }
}

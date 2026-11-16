package com.mimiworld.mimiworld.block;

import com.mimiworld.mimiworld.ModDimensions;
import com.mimiworld.mimiworld.world.RemnantFallenTeleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 残堕界传送门方块（remnant_fallen_portal）：贴地薄层的紫色星空平面，实体踩入即双向传送。
 */
public class RemnantFallenPortalBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);

    public RemnantFallenPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(level instanceof ServerLevel serverLevel)
                || entity.isRemoved()
                || entity.level() != level) {
            return;
        }
        boolean overlapping = Shapes.joinIsNotEmpty(
                Shapes.create(entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ())),
                state.getShape(level, pos), BooleanOp.AND);
        if (!overlapping) {
            return;
        }
        ResourceKey<Level> target = serverLevel.dimension().equals(ModDimensions.REMNANT_FALLEN_REALM)
                ? Level.OVERWORLD
                : ModDimensions.REMNANT_FALLEN_REALM;
        ServerLevel dest = serverLevel.getServer().getLevel(target);
        if (dest == null || !entity.canTeleport(serverLevel, dest)) {
            return;
        }
        RemnantFallenTeleporter.teleport(entity, serverLevel, target, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + 0.3D;
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0D, 0.02D, 0.0D);
    }

    @Override
    @SuppressWarnings("deprecation") // 26.3 暂未提供该钩子的替代方法
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }
}

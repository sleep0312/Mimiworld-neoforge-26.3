package com.mimiworld.mimiworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mimiworld.mimiworld.block.entity.GeoDecorationBlockEntity;

import java.util.function.Supplier;

/**
 * Boss 头颅摆件方块的通用实现（7 种头颅共用）。
 *
 * <p>世界中的外观完全由 GeckoLib BlockEntityRenderer 渲染，方块自身模型不渲染
 * （{@link RenderShape#INVISIBLE}），仅提供破坏粒子纹理；物品栏图标走 models/item 的 2D/3D 图标。</p>
 *
 * <p>每种头颅的碰撞箱与渲染缩放由注册处（{@link ModBlocks}）与客户端渲染器分别指定。</p>
 */
public class BossHeadBlock extends Block implements EntityBlock {

    private final VoxelShape shape;
    private final Supplier<BlockEntityType<?>> entityType;

    public BossHeadBlock(VoxelShape shape, Supplier<BlockEntityType<?>> entityType, Properties properties) {
        super(properties);
        this.shape = shape;
        this.entityType = entityType;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(entityType.get(), pos, state);
    }
}

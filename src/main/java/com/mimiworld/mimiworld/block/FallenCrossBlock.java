package com.mimiworld.mimiworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mimiworld.mimiworld.block.entity.GeoDecorationBlockEntity;

import java.util.function.Supplier;

/**
 * 堕落十字架（fallen_cross）：带基座的华丽十字架装饰方块。
 *
 * <p>外观由 GeckoLib BlockEntityRenderer 渲染，方块自身模型不渲染
 * （{@link RenderShape#INVISIBLE}），仅提供破坏粒子纹理。</p>
 *
 * <p>暗下界合金质地、硬度 3.0、抗爆 6.0，需铁镐以上才掉落，破坏掉落本身。
 * 残堕界传送门激活逻辑在维度批次中接入。</p>
 */
public class FallenCrossBlock extends Block implements EntityBlock {

    /** 模型缩放后十字柱整体包围盒（x[5/18,13/18]、z[7/18,11/18]、高满格）。 */
    private static final VoxelShape SHAPE = Shapes.box(5.0 / 18.0, 0.0, 7.0 / 18.0,
            13.0 / 18.0, 1.0, 11.0 / 18.0);

    private final Supplier<BlockEntityType<?>> entityType;

    public FallenCrossBlock(Supplier<BlockEntityType<?>> entityType, Properties properties) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(entityType.get(), pos, state);
    }
}

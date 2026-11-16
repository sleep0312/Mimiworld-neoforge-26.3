package com.mimiworld.mimiworld.block;

import com.mimiworld.mimiworld.block.entity.GridCraftingTableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * NxN 网格工作台式机器方块基类（凝锭台 5x5 等）。
 * <p>
 * 外观为普通全立方块，右键打开 NxN 合成网格 + 1 结果槽的即时合成 GUI
 * （同原版工作台：摆好即预览、取走即消耗）。服务端每 tick 刷新结果预览，
 * 破坏时掉落网格内物品（结果预览自动丢弃）。
 */
public abstract class GridCraftingTableBlock extends BaseEntityBlock {

    protected GridCraftingTableBlock(Properties properties) {
        super(properties);
    }

    /** 子类提供：创建对应的方块实体。 */
    protected abstract BlockEntity createMachineBlockEntity(BlockPos pos, BlockState state);

    /** 子类提供：对应的方块实体类型。 */
    protected abstract BlockEntityType<? extends GridCraftingTableBlockEntity> blockEntityType();

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof GridCraftingTableBlockEntity table) {
                GridCraftingTableBlockEntity.serverTick(tickLevel, pos, tickState, table);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GridCraftingTableBlockEntity blockEntity) {
            serverPlayer.openMenu(blockEntity);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof GridCraftingTableBlockEntity blockEntity) {
            blockEntity.dropContents(level, pos);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

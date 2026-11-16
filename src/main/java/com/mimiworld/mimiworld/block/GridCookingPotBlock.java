package com.mimiworld.mimiworld.block;

import com.mimiworld.mimiworld.block.entity.GridCookingPotBlockEntity;

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
 * NxN 网格煮锅机器方块基类（大煮锅 3x3 / 巨无霸 5x5 / 铁锅 2x2 / 烤箱 3x3 共用）。
 * <p>
 * 右键打开熔炉式加工 GUI：左上 NxN 输入网格、网格下方燃料槽（带火焰）、
 * 右上单独输入槽、右下输出槽；燃料烧炼、定时加工，节奏同原版熔炉（默认 200 tick）。
 * 破坏时掉落全部槽内物品。
 */
public abstract class GridCookingPotBlock extends BaseEntityBlock {

    protected GridCookingPotBlock(Properties properties) {
        super(properties);
    }

    /** 子类提供：创建对应的方块实体。 */
    protected abstract BlockEntity createMachineBlockEntity(BlockPos pos, BlockState state);

    /** 子类提供：对应的方块实体类型。 */
    protected abstract BlockEntityType<? extends GridCookingPotBlockEntity> blockEntityType();

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
            if (blockEntity instanceof GridCookingPotBlockEntity pot) {
                GridCookingPotBlockEntity.serverTick(tickLevel, pos, tickState, pot);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GridCookingPotBlockEntity blockEntity) {
            serverPlayer.openMenu(blockEntity);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof GridCookingPotBlockEntity blockEntity) {
            blockEntity.dropContents(level, pos);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

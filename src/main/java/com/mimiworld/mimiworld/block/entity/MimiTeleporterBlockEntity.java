package com.mimiworld.mimiworld.block.entity;

import com.mimiworld.mimiworld.block.MimiTeleporterBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

/**
 * 米米传送器方块实体：承载激活序列进度（seqStart / seqCenter），存档并同步客户端。
 */
public class MimiTeleporterBlockEntity extends BlockEntity {

    public static final long SEQ_NONE = Long.MIN_VALUE;

    public long seqStart = SEQ_NONE;

    @Nullable
    public BlockPos seqCenter;

    private int tickCounter;

    public MimiTeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIMI_TELEPORTER.get(), pos, state);
    }

    public boolean isSequenceRunning() {
        return seqStart != SEQ_NONE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MimiTeleporterBlockEntity be) {
        if (!state.getValue(MimiTeleporterBlock.EYE)) {
            return;
        }
        if (be.isSequenceRunning()) {
            MimiTeleporterBlock.tryActivate(level, pos);
        } else if (++be.tickCounter >= 20) {
            be.tickCounter = 0;
            MimiTeleporterBlock.tryActivate(level, pos);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (seqStart != SEQ_NONE) {
            output.putLong("mimi_seq_start", seqStart);
            if (seqCenter != null) {
                output.putLong("mimi_seq_center", seqCenter.asLong());
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        seqStart = input.getLongOr("mimi_seq_start", SEQ_NONE);
        seqCenter = input.getLong("mimi_seq_center").map(BlockPos::of).orElse(null);
        // 客户端据此外推场景级激光渲染的活动序列登记（仅客户端执行）
        if (this.level != null && this.level.isClientSide()) {
            com.mimiworld.mimiworld.client.renderer.MimiLaserRenderer.update(
                    this.worldPosition, this.seqStart, this.seqCenter);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel) {
            setChanged();
        } else {
            // 区块初载时 loadAdditional 早于 setLevel（此时 level 仍为 null），
            // 这里补一次登记，确保中途加入世界也能看到进行中的激光序列。
            com.mimiworld.mimiworld.client.renderer.MimiLaserRenderer.update(
                    this.worldPosition, this.seqStart, this.seqCenter);
        }
    }
}

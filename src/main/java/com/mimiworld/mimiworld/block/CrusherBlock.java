package com.mimiworld.mimiworld.block;

import com.mimiworld.mimiworld.block.entity.CrusherBlockEntity;
import com.mimiworld.mimiworld.block.entity.ModBlockEntities;
import com.mimiworld.mimiworld.block.entity.MultiInputProcessorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 搅碎器（crusher）：绑定 CrusherBlockEntity，
 * 右键打开三输入熔炉式加工 GUI（左上/左下/右上输入，右下输出）。
 */
public class CrusherBlock extends MultiInputProcessorBlock {

    public CrusherBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createMachineBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends MultiInputProcessorBlockEntity> blockEntityType() {
        return ModBlockEntities.CRUSHER.get();
    }
}

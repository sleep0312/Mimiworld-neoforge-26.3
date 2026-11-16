package com.mimiworld.mimiworld.mixin;

import com.mimiworld.mimiworld.ModDimensions;
import com.mimiworld.mimiworld.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 米米维度水锅在雨中续接为米汤锅，保证米米维度的雨永远不会让锅增加水。
 */
@Mixin(LayeredCauldronBlock.class)
public abstract class LayeredCauldronBlockMixin {

    @Inject(method = "handlePrecipitation", at = @At("HEAD"), cancellable = true)
    private void mimiworld$fillRiceSoup(BlockState state, Level level, BlockPos pos,
                                        Biome.Precipitation precipitation, CallbackInfo ci) {
        if (precipitation != Biome.Precipitation.RAIN
                || level.isClientSide()
                || !level.dimension().equals(ModDimensions.MIMI_DIMENSION)) {
            return;
        }
        if (!state.is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON)) {
            return;
        }
        int level3 = Math.min(state.getValue(LayeredCauldronBlock.LEVEL) + 1, 3);
        level.setBlockAndUpdate(pos, ModFluids.RICE_SOUP_CAULDRON.get().defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, level3));
        ci.cancel();
    }
}

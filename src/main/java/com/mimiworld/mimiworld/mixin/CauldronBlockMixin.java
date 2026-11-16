package com.mimiworld.mimiworld.mixin;

import com.mimiworld.mimiworld.ModDimensions;
import com.mimiworld.mimiworld.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 米米维度「雨即米汤」的炼药锅接雨逻辑。
 *
 * <p>空锅在米米维度接到雨时变成米汤锅（液位 1），水锅续雨时改接米汤。</p>
 */
@Mixin(CauldronBlock.class)
public abstract class CauldronBlockMixin {

    @Inject(method = "handlePrecipitation", at = @At("HEAD"), cancellable = true)
    private void mimiworld$fillRiceSoup(BlockState state, Level level, BlockPos pos,
                                        Biome.Precipitation precipitation, CallbackInfo ci) {
        if (precipitation != Biome.Precipitation.RAIN
                || level.isClientSide()
                || !level.dimension().equals(ModDimensions.MIMI_DIMENSION)) {
            return;
        }
        level.setBlockAndUpdate(pos, ModFluids.RICE_SOUP_CAULDRON.get().defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 1));
        ci.cancel();
    }
}

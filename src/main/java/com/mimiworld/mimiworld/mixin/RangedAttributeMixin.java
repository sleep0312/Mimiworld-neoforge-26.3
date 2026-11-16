package com.mimiworld.mimiworld.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 解除原版最大生命值 1024 的属性上限，供 5000 血 Boss 使用。
 *
 * <p>仅对 MAX_HEALTH 放宽上限到 10,000,000，其余属性保持原版范围。</p>
 */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {

    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void mimiworld$raiseMaxHealthLimit(double value, CallbackInfoReturnable<Double> cir) {
        RangedAttribute self = (RangedAttribute) (Object) this;
        if (self == Attributes.MAX_HEALTH.value() && value > 1024.0D) {
            cir.setReturnValue(Mth.clamp(value, 1.0D, 10_000_000.0D));
        }
    }
}

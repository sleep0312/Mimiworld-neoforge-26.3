package com.mimiworld.mimiworld.mixin;

import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 药水效果名称的等级显示：等级高于 10 时改用阿拉伯数字。
 *
 * <p>原版只在 amplifier 为 1~9（即 II~X 级）时追加
 * {@code enchantment.level.N} 的罗马数字，更高等级则完全不显示等级。
 * 这里在返回值处补上 {@code " " + (amplifier + 1)}，例如 amplifier 254 的
 * 生命恢复会显示为「生命恢复 255」。</p>
 */
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {

    @Inject(method = "getEffectName", at = @At("RETURN"), cancellable = true)
    private void mimiworld$numericHighLevel(MobEffectInstance effect, CallbackInfoReturnable<Component> cir) {
        if (effect.getAmplifier() < 10) {
            return;
        }
        cir.setReturnValue(effect.getEffect().value().getDisplayName().copy()
                .append(" ")
                .append(Component.literal(String.valueOf(effect.getAmplifier() + 1))));
    }
}

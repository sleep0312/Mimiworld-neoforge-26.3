package com.mimiworld.mimiworld.mixin.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import snownee.jade.addon.vanilla.StatusEffectsProvider;

/**
 * Jade 实体效果条目的等级显示：等级高于 10 时改用阿拉伯数字。
 *
 * <p>Jade 的 {@code StatusEffectsProvider.Client#getEffectName} 与原版一致，
 * 只对 amplifier 1~9 追加 {@code enchantment.level.N} 的罗马数字，更高等级不显示等级。
 * 这里在返回值处补上 {@code " " + (amplifier + 1)}，指向 Boss 时即可看到
 * 例如「残堕化 12」这样的数字等级。</p>
 *
 * <p>该 mixin 通过独立的可选混入配置 {@code mimiworld.jade.mixins.json} 加载：
 * 未安装 Jade 时不会生效，也不会影响本模组启动。</p>
 */
@Mixin(StatusEffectsProvider.Client.class)
public abstract class JadeStatusEffectsProviderMixin {

    @Inject(method = "getEffectName", at = @At("RETURN"), cancellable = true, remap = false)
    private static void mimiworld$numericHighLevel(MobEffectInstance effect,
                                                   CallbackInfoReturnable<MutableComponent> cir) {
        if (effect.getAmplifier() < 10) {
            return;
        }
        cir.setReturnValue(effect.getEffect().value().getDisplayName().copy()
                .append(" ")
                .append(Component.literal(String.valueOf(effect.getAmplifier() + 1))));
    }
}

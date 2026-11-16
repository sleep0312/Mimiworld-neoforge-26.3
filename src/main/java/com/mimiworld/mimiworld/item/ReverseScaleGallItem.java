package com.mimiworld.mimiworld.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mimiworld.mimiworld.MimiWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 逆鳞龙胆（Reverse Scale Gall）：圣遗物槽位饰品。
 *
 * <p>佩戴期间：持续获得抗性提升 IV、护甲值 +100%；
 * 单次受伤不超过最大生命值 20% 的判定由事件系统处理。</p>
 */
public class ReverseScaleGallItem extends Item implements ICurioItem {

    /** 抗性提升 IV 对应 amplifier 3；刷新时长 8 秒，剩余 ≤3 秒时重置（即每 5 秒一次）。 */
    private static final int RESISTANCE_AMPLIFIER = 3;
    private static final int EFFECT_DURATION_TICKS = 160;
    private static final int EFFECT_REFRESH_REMAINING = 60;

    public ReverseScaleGallItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("removal") // Curios 17 仍需要槽位唯一 ID，故沿用带 uuid 的重载
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, Identifier uuid, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        Identifier id = Identifier.fromNamespaceAndPath(MimiWorld.MODID,
                (uuid == null ? "" : uuid.getPath() + "_") + "reverse_scale_gall_armor");
        modifiers.put(Attributes.ARMOR,
                new AttributeModifier(id, 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return modifiers;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        // 非 ambient 施加，避免被盔甲效果的脱下清理误删；剩余 ≤3 秒时重置回 8 秒
        MobEffectInstance current = entity.getEffect(MobEffects.RESISTANCE);
        if (current == null || current.getAmplifier() < RESISTANCE_AMPLIFIER
                || (current.getAmplifier() == RESISTANCE_AMPLIFIER
                    && current.getDuration() <= EFFECT_REFRESH_REMAINING)) {
            entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                    EFFECT_DURATION_TICKS, RESISTANCE_AMPLIFIER, false, false, true));
        }
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        tooltips.clear();
        tooltips.add(Component.empty());
        tooltips.add(Component.translatable("curios.modifiers.sacred_relic").withStyle(ChatFormatting.GOLD));
        tooltips.add(Component.translatable("tooltip.mimiworld.reverse_scale_gall.resistance")
                .withStyle(ChatFormatting.BLUE));
        tooltips.add(Component.translatable("tooltip.mimiworld.reverse_scale_gall.armor")
                .withStyle(ChatFormatting.BLUE));
        tooltips.add(Component.translatable("tooltip.mimiworld.reverse_scale_gall.damage_cap")
                .withStyle(ChatFormatting.BLUE));
        return tooltips;
    }
}

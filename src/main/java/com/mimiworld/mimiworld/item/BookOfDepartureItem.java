package com.mimiworld.mimiworld.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mimiworld.mimiworld.MimiWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Set;

/**
 * 启航之书（Book of Departure）：神遗之书槽位饰品。
 *
 * <p>佩戴期间：持续获得抗性提升 II、跳跃提升 I、生命恢复 I（8 秒时长，每 5 秒刷新一次，
 * 脱下清除残留）；时运 +3、抢夺 +3；最大生命值 +25%、攻击力 +20%。</p>
 */
public class BookOfDepartureItem extends Item implements ICurioItem {

    /** 刷新时长（tick，8 秒）与刷新阈值（剩余 ≤3 秒时重置，即每 5 秒一次）。 */
    private static final int EFFECT_DURATION = 160;
    private static final int EFFECT_REFRESH_REMAINING = 60;

    /** 本饰品提供的全部药水效果类型，用于脱下后清理残留。 */
    private static final Set<Holder<MobEffect>> GRANTED_EFFECTS = Set.of(
            MobEffects.RESISTANCE,
            MobEffects.JUMP_BOOST,
            MobEffects.REGENERATION);

    public BookOfDepartureItem(Properties properties) {
        super(properties);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        refreshEffect(entity, MobEffects.RESISTANCE, 1);
        refreshEffect(entity, MobEffects.JUMP_BOOST, 0);
        refreshEffect(entity, MobEffects.REGENERATION, 0);
    }

    /** 缺失或等级不足时立即施加；等级相同则仅在剩余 ≤3 秒时重置回 8 秒（即每 5 秒一次）。 */
    private static void refreshEffect(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = entity.getEffect(effect);
        if (current == null || current.getAmplifier() < amplifier
                || (current.getAmplifier() == amplifier && current.getDuration() <= EFFECT_REFRESH_REMAINING)) {
            entity.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, false, true, true));
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        // 仅清除本饰品灌注的短时残留，避免误删玩家自饮的长时药水
        for (Holder<MobEffect> effect : GRANTED_EFFECTS) {
            MobEffectInstance instance = entity.getEffect(effect);
            if (instance != null && instance.getDuration() <= EFFECT_DURATION) {
                entity.removeEffect(effect);
            }
        }
    }

    @Override
    @SuppressWarnings("removal") // Curios 17 仍需要槽位唯一 ID，故沿用带 uuid 的重载
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, Identifier uuid, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        String prefix = uuid == null ? "" : uuid.getPath() + "_";
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                Identifier.fromNamespaceAndPath(MimiWorld.MODID, prefix + "book_of_departure_max_health"),
                0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                Identifier.fromNamespaceAndPath(MimiWorld.MODID, prefix + "book_of_departure_attack_damage"),
                0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return modifiers;
    }

    @Override
    public int getFortuneLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
        return 3;
    }

    @Override
    public int getLootingLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
        return 3;
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        tooltips.clear();
        tooltips.add(Component.empty());
        tooltips.add(Component.translatable("curios.modifiers.shenyi_book").withStyle(ChatFormatting.GOLD));
        tooltips.add(Component.translatable("tooltip.mimiworld.book_of_departure.effects")
                .withStyle(ChatFormatting.BLUE));
        tooltips.add(Component.translatable("tooltip.mimiworld.book_of_departure.fortune")
                .withStyle(ChatFormatting.BLUE));
        tooltips.add(Component.translatable("tooltip.mimiworld.book_of_departure.looting")
                .withStyle(ChatFormatting.BLUE));
        return tooltips;
    }
}

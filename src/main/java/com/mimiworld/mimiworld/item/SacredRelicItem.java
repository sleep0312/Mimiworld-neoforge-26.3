package com.mimiworld.mimiworld.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mimiworld.mimiworld.MimiWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 圣遗物/神遗之书通用饰品物品。
 *
 * <p>用配置代替逐类重复实现：佩戴期间由 Curios 属性系统增删 {@link Modifier} 列表中的
 * 修饰符，属性提示区块用金色槽位标题 + 蓝色说明行展示。</p>
 *
 * <p>动态战斗效果（伤害拆分、概率增伤等）不在此类，统一由事件系统处理。</p>
 */
public class SacredRelicItem extends Item implements ICurioItem {

    /**
     * 单条属性修饰符配置。
     *
     * @param attribute 目标属性
     * @param amount    数值
     * @param operation 运算方式
     * @param suffix    修饰符 ID 后缀（用于区分同一物品的多条修饰符）
     */
    public record Modifier(Holder<Attribute> attribute, double amount,
                           AttributeModifier.Operation operation, String suffix) {
    }

    /** 所属 Curios 槽位 ID（sacred_relic / shenyi_book）。 */
    private final String slot;
    private final List<Modifier> modifiers;
    /** 蓝色说明行的语言键（含 {@code tooltip.mimiworld.} 前缀）。 */
    private final List<String> tooltipKeys;

    public SacredRelicItem(Properties properties, String slot, List<Modifier> modifiers, List<String> tooltipKeys) {
        super(properties);
        this.slot = slot;
        this.modifiers = modifiers;
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    @SuppressWarnings("removal") // Curios 17 仍需要槽位唯一 ID，故沿用带 uuid 的重载
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, Identifier uuid, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> result = HashMultimap.create();
        for (Modifier modifier : modifiers) {
            Identifier id = modifierId(uuid, modifier.suffix());
            result.put(modifier.attribute(),
                    new AttributeModifier(id, modifier.amount(), modifier.operation()));
        }
        return result;
    }

    /** 用 Curios 提供的槽位唯一 ID 拼接修饰符 ID，保证同一饰品装在多个槽位时不冲突。 */
    private static Identifier modifierId(Identifier uuid, String suffix) {
        if (uuid == null) {
            return Identifier.fromNamespaceAndPath(MimiWorld.MODID, suffix);
        }
        return Identifier.fromNamespaceAndPath(uuid.getNamespace(), uuid.getPath() + "_" + suffix);
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        tooltips.clear();
        tooltips.add(Component.empty());
        tooltips.add(Component.translatable("curios.modifiers." + slot).withStyle(ChatFormatting.GOLD));
        for (String key : tooltipKeys) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.BLUE));
        }
        return tooltips;
    }
}

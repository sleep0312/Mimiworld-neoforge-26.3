package com.mimiworld.mimiworld.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 上古毛皮（Ancient Pelt）：圣遗物槽位饰品。
 *
 * <p>佩戴期间：抢夺 +2（由 Curios 接入战利品结算）；
 * 「易怒」效果的维持与升降级由事件系统处理。</p>
 */
public class AncientPeltItem extends Item implements ICurioItem {

    private static final int LOOTING_BONUS = 2;

    public AncientPeltItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getLootingLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
        return LOOTING_BONUS;
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        tooltips.clear();
        tooltips.add(Component.empty());
        tooltips.add(Component.translatable("curios.modifiers.sacred_relic").withStyle(ChatFormatting.GOLD));
        tooltips.add(Component.translatable("tooltip.mimiworld.ancient_pelt.looting")
                .withStyle(ChatFormatting.BLUE));
        tooltips.add(Component.translatable("tooltip.mimiworld.ancient_pelt.irritable")
                .withStyle(ChatFormatting.BLUE));
        tooltips.add(Component.translatable("tooltip.mimiworld.ancient_pelt.escalate")
                .withStyle(ChatFormatting.BLUE));
        tooltips.add(Component.translatable("tooltip.mimiworld.ancient_pelt.decay")
                .withStyle(ChatFormatting.BLUE));
        return tooltips;
    }
}

package com.mimiworld.mimiworld.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 动态彩虹渐变名称的普通物品（剑谱/剑诀类）。
 */
public class RainbowNameItem extends Item {

    public RainbowNameItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return RainbowNameSwordItem.rainbow(Component.translatable(getDescriptionId()).getString());
    }
}

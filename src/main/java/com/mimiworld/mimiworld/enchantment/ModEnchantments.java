package com.mimiworld.mimiworld.enchantment;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * 米米世界自定义附魔（1.21+ 数据驱动）。
 *
 * <p>附魔定义位于 data/mimiworld/enchantment/*.json：</p>
 * <ul>
 *   <li>weeping_blood（泣血）：剑类武器专属，造成伤害后按伤害百分比回复生命。</li>
 *   <li>flawless（完美无缺）：任意有耐久的物品，不再消耗耐久（数据驱动 item_damage）。</li>
 *   <li>swift_strike（迅击）：近战武器专属，最终伤害按等级与移动速度放大。</li>
 * </ul>
 */
public final class ModEnchantments {

    private ModEnchantments() {
    }

    public static final ResourceKey<Enchantment> WEEPING_BLOOD = key("weeping_blood");
    public static final ResourceKey<Enchantment> FLAWLESS = key("flawless");
    public static final ResourceKey<Enchantment> SWIFT_STRIKE = key("swift_strike");

    private static ResourceKey<Enchantment> key(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath(MimiWorld.MODID, id));
    }

    /** 从注册表提供者取得附魔 Holder（缺失时直接抛异常，便于早期发现数据包问题）。 */
    public static Holder<Enchantment> holder(HolderLookup.Provider provider, ResourceKey<Enchantment> key) {
        return provider.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** 读取物品上某个自定义附魔的等级（不区分槽位）。 */
    public static int level(ItemStack stack, HolderLookup.Provider provider, ResourceKey<Enchantment> key) {
        return EnchantmentHelper.getTagEnchantmentLevel(holder(provider, key), stack);
    }
}

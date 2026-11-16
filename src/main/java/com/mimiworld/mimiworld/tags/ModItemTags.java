package com.mimiworld.mimiworld.tags;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 米米世界自定义物品标签。
 */
public final class ModItemTags {

    private ModItemTags() {
    }

    /**
     * 任意肉类（生/熟、陆地/鱼类、腐肉均算），供血瓶等配方使用。
     *
     * <p>内容由 {@code datagen.ModItemTagsProvider} 生成到
     * data/mimiworld/tags/item/meats.json。</p>
     */
    public static final TagKey<Item> MEATS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MimiWorld.MODID, "meats"));
}

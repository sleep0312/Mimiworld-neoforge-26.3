package com.mimiworld.mimiworld.datagen;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.ModItems;
import com.mimiworld.mimiworld.tags.ModItemTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

/**
 * 米米世界物品标签数据生成器。
 * runData 后生成 src/generated/resources/data/mimiworld/tags/item/*.json。
 */
public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, MimiWorld.MODID);
    }

    @Override
    @SuppressWarnings("unchecked") // 标签 API 的 ResourceKey<Item> varargs 会触发泛型数组创建警告
    protected void addTags(HolderLookup.Provider provider) {
        // 任意肉类（生/熟、陆地/鱼类、腐肉均算）：
        // 原版生肉
        tag(ModItemTags.MEATS).add(
                key(Items.BEEF), key(Items.PORKCHOP), key(Items.CHICKEN), key(Items.MUTTON), key(Items.RABBIT),
                key(Items.COD), key(Items.SALMON), key(Items.TROPICAL_FISH), key(Items.PUFFERFISH),
                // 原版熟肉
                key(Items.COOKED_BEEF), key(Items.COOKED_PORKCHOP), key(Items.COOKED_CHICKEN), key(Items.COOKED_MUTTON),
                key(Items.COOKED_RABBIT), key(Items.COOKED_COD), key(Items.COOKED_SALMON),
                // 原版腐肉
                key(Items.ROTTEN_FLESH),
                // 本模生肉 5 种、熟肉 5 种与腐米肉（顺序见下）
                key(ModItems.RAW_MIMOO_BEEF.get()), key(ModItems.RAW_MIMI_MUTTON.get()), key(ModItems.RAW_MIHENG_PORK.get()),
                key(ModItems.RAW_TANMI_FISH.get()), key(ModItems.ZHUOMI_CHICKEN.get()),
                key(ModItems.COOKED_ZHUOMI_CHICKEN.get()), key(ModItems.COOKED_MIMOO_BEEF.get()), key(ModItems.COOKED_MIMI_MUTTON.get()),
                key(ModItems.COOKED_MIHENG_PORK.get()), key(ModItems.COOKED_TANMI_FISH.get()),
                key(ModItems.ROTTEN_RICE_FLESH.get()));
    }

    private static ResourceKey<Item> key(Item item) {
        return BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
    }
}

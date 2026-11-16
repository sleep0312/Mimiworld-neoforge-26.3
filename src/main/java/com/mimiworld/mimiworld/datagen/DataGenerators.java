package com.mimiworld.mimiworld.datagen;

import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

/**
 * 数据生成器入口：仅在运行 runData（datagen）时触发，不影响正常游戏加载。
 *
 * <p>26.3 起配方/战利品表属于数据包注册表，改由 {@link GatherDataRegistryEntriesEvent}
 * 通过 {@code DatapackRegistryGatherer} 注册；物品标签等普通 DataProvider 仍在
 * {@link GatherDataEvent} 中注册。生成物输出到 src/generated/resources
 * （由 build.gradle 的 data run 参数指定，并已加入主资源源集）。</p>
 */
public final class DataGenerators {

    private DataGenerators() {
    }

    /** 配方与战利品表（数据包注册表条目）。 */
    public static void gatherRegistryEntries(GatherDataRegistryEntriesEvent event) {
        event.recipe(ModRecipeProvider::new);
        event.add(Registries.LOOT_TABLE, new ModLootTableProvider());
    }

    /** 物品标签（普通 DataProvider）。 */
    public static void gatherData(GatherDataEvent event) {
        event.addProvider(new ModItemTagsProvider(
                event.getDefaultPackGenerator().getPackOutput(),
                event.getReloadableLookupProvider()));
    }
}

package com.mimiworld.mimiworld;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Comparator;
import java.util.List;

/**
 * 米米世界自定义创造模式标签页（mimiworld:mimi）。
 *
 * <p>自动收录命名空间为 mimiworld 的全部已注册物品，并按注册 ID 字母序稳定排序。
 * 后续批次加入工具、防具、方块等物品后无需修改本类。</p>
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MimiWorld.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MIMI_TAB =
            CREATIVE_MODE_TABS.register("mimi", () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.REALM_HEAVEN_BRAIN_FLOWER.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.mimiworld"))
                    .displayItems((parameters, output) -> {
                        List<Item> items = BuiltInRegistries.ITEM.stream()
                                .filter(item -> {
                                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                                    return id != null && id.getNamespace().equals(MimiWorld.MODID);
                                })
                                .sorted(Comparator.comparing(item -> {
                                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                                    return id == null ? "" : id.getPath();
                                }))
                                .toList();
                        items.forEach(output::accept);
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}

package com.mimiworld.mimiworld.block.entity;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.block.ModBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 米米世界方块实体类型注册（机器批次：搅碎器、搅拌器）。
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MimiWorld.MODID);

    /** 搅碎器（方块 crusher）：3 入 1 出定时加工。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register("crusher",
                    () -> new BlockEntityType<>(CrusherBlockEntity::new, ModBlocks.CRUSHER.get()));

    /** 搅拌器（方块 mixer）：9 输入 + 1 输出工作台式无序即时合成。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerBlockEntity>> MIXER =
            BLOCK_ENTITIES.register("mixer",
                    () -> new BlockEntityType<>(MixerBlockEntity::new, ModBlocks.MIXER.get()));

    /** 造米台（方块 rice_maker）：原料 + 水桶 → 米。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RiceMakerBlockEntity>> RICE_MAKER =
            BLOCK_ENTITIES.register("rice_maker",
                    () -> new BlockEntityType<>(RiceMakerBlockEntity::new, ModBlocks.RICE_MAKER.get()));

    /** 锤米台（方块 rice_pounding_table）：无序双输入即时合成。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RicePoundingTableBlockEntity>> RICE_POUNDING_TABLE =
            BLOCK_ENTITIES.register("rice_pounding_table",
                    () -> new BlockEntityType<>(RicePoundingTableBlockEntity::new,
                            ModBlocks.RICE_POUNDING_TABLE.get()));

    /** 沉淀台（方块 precipitation_table）：双输入定时加工。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrecipitationTableBlockEntity>> PRECIPITATION_TABLE =
            BLOCK_ENTITIES.register("precipitation_table",
                    () -> new BlockEntityType<>(PrecipitationTableBlockEntity::new,
                            ModBlocks.PRECIPITATION_TABLE.get()));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}

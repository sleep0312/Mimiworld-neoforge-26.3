package com.mimiworld.mimiworld.block.entity;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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

    /** 凝锭台（方块 ingot_condensation_table）：5x5 网格即时合成。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IngotCondensationTableBlockEntity>> INGOT_CONDENSATION_TABLE =
            BLOCK_ENTITIES.register("ingot_condensation_table",
                    () -> new BlockEntityType<>(IngotCondensationTableBlockEntity::new,
                            ModBlocks.INGOT_CONDENSATION_TABLE.get()));

    /** 大煮锅（方块 large_cooking_pot）：3x3 网格熔炉式烹煮。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeCookingPotBlockEntity>> LARGE_COOKING_POT =
            BLOCK_ENTITIES.register("large_cooking_pot",
                    () -> new BlockEntityType<>(LargeCookingPotBlockEntity::new,
                            ModBlocks.LARGE_COOKING_POT.get()));

    /** 巨无霸大大大煮锅（方块 jumbo_mega_cooking_pot）：5x5 网格熔炉式烹煮。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JumboMegaCookingPotBlockEntity>> JUMBO_MEGA_COOKING_POT =
            BLOCK_ENTITIES.register("jumbo_mega_cooking_pot",
                    () -> new BlockEntityType<>(JumboMegaCookingPotBlockEntity::new,
                            ModBlocks.JUMBO_MEGA_COOKING_POT.get()));

    /** 铁锅（方块 iron_wok）：菱形四输入熔炉式烹煮。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronWokBlockEntity>> IRON_WOK =
            BLOCK_ENTITIES.register("iron_wok",
                    () -> new BlockEntityType<>(IronWokBlockEntity::new,
                            ModBlocks.IRON_WOK.get()));

    /** 烤箱（方块 oven）：3x3 网格熔炉式烹煮。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OvenBlockEntity>> OVEN =
            BLOCK_ENTITIES.register("oven",
                    () -> new BlockEntityType<>(OvenBlockEntity::new,
                            ModBlocks.OVEN.get()));

    // ============================ Boss 头颅摆件与堕落十字架 ============================

    private static GeoDecorationBlockEntity monkeyHead(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(MIMI_MONKEY_HEAD.get(), pos, state);
    }

    private static GeoDecorationBlockEntity tigerKingHead(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(RICE_CRYSTAL_TIGER_KING_HEAD.get(), pos, state);
    }

    private static GeoDecorationBlockEntity oxHead(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(SUPER_CRYSTAL_OX_HEAD.get(), pos, state);
    }

    private static GeoDecorationBlockEntity rabbitKingHead(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(HEAVY_CRYSTAL_RABBIT_KING_HEAD.get(), pos, state);
    }

    private static GeoDecorationBlockEntity azurePythonHead(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(COMPOUND_CRYSTAL_AZURE_PYTHON_HEAD.get(), pos, state);
    }

    private static GeoDecorationBlockEntity longevityKuiHead(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(PURIFIED_LONGEVITY_KUI_HEAD.get(), pos, state);
    }

    private static GeoDecorationBlockEntity endDragonHead(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(LAZIEST_END_DRAGON_HEAD.get(), pos, state);
    }

    private static GeoDecorationBlockEntity dibazongzuiHead(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(DIBAZONGZUI_HEAD.get(), pos, state);
    }

    private static GeoDecorationBlockEntity fallenCross(BlockPos pos, BlockState state) {
        return new GeoDecorationBlockEntity(FALLEN_CROSS.get(), pos, state);
    }

    /** 米米猴头（方块 mimi_monkey_head）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> MIMI_MONKEY_HEAD =
            BLOCK_ENTITIES.register("mimi_monkey_head",
                    () -> new BlockEntityType<>(ModBlockEntities::monkeyHead, ModBlocks.MIMI_MONKEY_HEAD.get()));

    /** 饭晶虎王头（方块 rice_crystal_tiger_king_head）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> RICE_CRYSTAL_TIGER_KING_HEAD =
            BLOCK_ENTITIES.register("rice_crystal_tiger_king_head",
                    () -> new BlockEntityType<>(ModBlockEntities::tigerKingHead,
                            ModBlocks.RICE_CRYSTAL_TIGER_KING_HEAD.get()));

    /** 超晶牛头（方块 super_crystal_ox_head）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> SUPER_CRYSTAL_OX_HEAD =
            BLOCK_ENTITIES.register("super_crystal_ox_head",
                    () -> new BlockEntityType<>(ModBlockEntities::oxHead,
                            ModBlocks.SUPER_CRYSTAL_OX_HEAD.get()));

    /** 重晶兔王头（方块 heavy_crystal_rabbit_king_head）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> HEAVY_CRYSTAL_RABBIT_KING_HEAD =
            BLOCK_ENTITIES.register("heavy_crystal_rabbit_king_head",
                    () -> new BlockEntityType<>(ModBlockEntities::rabbitKingHead,
                            ModBlocks.HEAVY_CRYSTAL_RABBIT_KING_HEAD.get()));

    /** 复晶天青蟒头（方块 compound_crystal_azure_python_head）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> COMPOUND_CRYSTAL_AZURE_PYTHON_HEAD =
            BLOCK_ENTITIES.register("compound_crystal_azure_python_head",
                    () -> new BlockEntityType<>(ModBlockEntities::azurePythonHead,
                            ModBlocks.COMPOUND_CRYSTAL_AZURE_PYTHON_HEAD.get()));

    /** 清洁长命夔头（方块 purified_longevity_kui_head）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> PURIFIED_LONGEVITY_KUI_HEAD =
            BLOCK_ENTITIES.register("purified_longevity_kui_head",
                    () -> new BlockEntityType<>(ModBlockEntities::longevityKuiHead,
                            ModBlocks.PURIFIED_LONGEVITY_KUI_HEAD.get()));

    /** 最懒终末龙头（方块 laziest_end_dragon_head）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> LAZIEST_END_DRAGON_HEAD =
            BLOCK_ENTITIES.register("laziest_end_dragon_head",
                    () -> new BlockEntityType<>(ModBlockEntities::endDragonHead,
                            ModBlocks.LAZIEST_END_DRAGON_HEAD.get()));

    /** 第八宗罪龙头（方块 dibazongzui_head）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> DIBAZONGZUI_HEAD =
            BLOCK_ENTITIES.register("dibazongzui_head",
                    () -> new BlockEntityType<>(ModBlockEntities::dibazongzuiHead,
                            ModBlocks.DIBAZONGZUI_HEAD.get()));

    /** 堕落十字架（方块 fallen_cross）。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeoDecorationBlockEntity>> FALLEN_CROSS =
            BLOCK_ENTITIES.register("fallen_cross",
                    () -> new BlockEntityType<>(ModBlockEntities::fallenCross,
                            ModBlocks.FALLEN_CROSS.get()));

    /** 米米传送器（方块 mimi_teleporter）：插眼/激活序列状态。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MimiTeleporterBlockEntity>> MIMI_TELEPORTER =
            BLOCK_ENTITIES.register("mimi_teleporter",
                    () -> new BlockEntityType<>(MimiTeleporterBlockEntity::new,
                            ModBlocks.MIMI_TELEPORTER.get()));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}

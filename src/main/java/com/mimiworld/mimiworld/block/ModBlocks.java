package com.mimiworld.mimiworld.block;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.world.tree.RiceTreeGrower;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 米米世界方块注册类（基础方块批次）。
 *
 * <p>本批次只移植不依赖 BlockEntity / 流体 / GeckoLib 渲染的方块：
 * 米树系列、石质与矿石系列、植物与装饰方块。机器方块、传送门、Boss 头颅摆件
 * 需要方块实体或 GeckoLib 渲染，后续批次单独移植。</p>
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MimiWorld.MODID);

    // ============================ 米树系列 ============================

    /** 米原木：轴向原木方块，硬度/音效参照原版橡木原木。 */
    public static final DeferredBlock<RotatedPillarBlock> RICE_LOG =
            BLOCKS.registerBlock("rice_log", RotatedPillarBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .sound(SoundType.WOOD));

    /** 米木板：普通全立方块，硬度/音效参照原版橡木木板。 */
    public static final DeferredBlock<Block> RICE_PLANKS =
            BLOCKS.registerBlock("rice_planks", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .sound(SoundType.WOOD));

    /** 米楼梯：木制楼梯，基础状态取自米木板。 */
    public static final DeferredBlock<StairBlock> RICE_STAIRS =
            BLOCKS.registerBlock("rice_stairs",
                    properties -> new StairBlock(RICE_PLANKS.get().defaultBlockState(), properties),
                    () -> BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .sound(SoundType.WOOD));

    /** 米台阶：木制半砖。 */
    public static final DeferredBlock<SlabBlock> RICE_SLAB =
            BLOCKS.registerBlock("rice_slab", SlabBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .sound(SoundType.WOOD));

    /** 米栅栏：木制栅栏。 */
    public static final DeferredBlock<FenceBlock> RICE_FENCE =
            BLOCKS.registerBlock("rice_fence", FenceBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(2.0F)
                            .sound(SoundType.WOOD));

    /** 米树叶：带 distance/persistent 状态，远离原木会随机刻腐烂。 */
    public static final DeferredBlock<LeavesBlock> RICE_LEAVES =
            BLOCKS.registerBlock("rice_leaves",
                    properties -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties),
                    () -> BlockBehaviour.Properties.of()
                            .strength(0.2F)
                            .randomTicks()
                            .sound(SoundType.GRASS)
                            .noOcclusion()
                            .isValidSpawn((state, level, pos, type) ->
                                    type == EntityTypes.OCELOT || type == EntityTypes.PARROT)
                            .isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos, aabb) -> false)
                            .ignitedByLava()
                            .pushReaction(PushReaction.POPPED));

    /** 米树苗：随机刻/骨粉长成米树（白桦形态，米原木 + 米树叶）。 */
    public static final DeferredBlock<SaplingBlock> RICE_SAPLING =
            BLOCKS.registerBlock("rice_sapling",
                    properties -> new SaplingBlock(RiceTreeGrower.INSTANCE, properties),
                    () -> BlockBehaviour.Properties.of()
                            .noCollision()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .pushReaction(PushReaction.POPPED));

    // ============================ 石质与矿石 ============================

    /** 硬化米：石头类全立方块，属性照抄原版石头。 */
    public static final DeferredBlock<Block> HARDENED_RICE =
            BLOCKS.registerBlock("hardened_rice", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 深层硬化米：深板岩质地。 */
    public static final DeferredBlock<Block> DEEPSLATE_HARDENED_RICE =
            BLOCKS.registerBlock("deepslate_hardened_rice", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE));

    /** 基米：基岩类方块，生存不可破坏、爆炸无法摧毁。 */
    public static final DeferredBlock<Block> RICE_BEDROCK =
            BLOCKS.registerBlock("rice_bedrock", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(-1.0F, 3600000.0F)
                            .sound(SoundType.STONE)
                            .isValidSpawn((state, level, pos, type) -> false));

    /** 结晶米矿石：非精准采集掉落结晶米并额外掉落 3~7 经验。 */
    public static final DeferredBlock<DropExperienceBlock> CRYSTALLIZED_RICE_ORE =
            BLOCKS.registerBlock("crystallized_rice_ore",
                    properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 重结晶米矿石：深板岩质地矿石。 */
    public static final DeferredBlock<DropExperienceBlock> RECRYSTALLIZED_RICE_ORE =
            BLOCKS.registerBlock("recrystallized_rice_ore",
                    properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
                    () -> BlockBehaviour.Properties.of()
                            .strength(4.5F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE));

    /** 可燃米矿石。 */
    public static final DeferredBlock<DropExperienceBlock> COMBUSTIBLE_RICE_ORE =
            BLOCKS.registerBlock("combustible_rice_ore",
                    properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 米矿石：非精准采集掉落米石并额外掉落 3~7 经验。 */
    public static final DeferredBlock<DropExperienceBlock> RICE_ORE =
            BLOCKS.registerBlock("rice_ore",
                    properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 超懒精华矿：类似铁矿石，非精准采集掉落超懒精华并额外掉落 3~7 经验。 */
    public static final DeferredBlock<DropExperienceBlock> SUPER_LAZY_ESSENCE_ORE =
            BLOCKS.registerBlock("super_lazy_essence_ore",
                    properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 泥米：泥巴质地方块，属性照抄原版泥巴。 */
    public static final DeferredBlock<Block> MUD_RICE =
            BLOCKS.registerBlock("mud_rice", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(0.5F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.MUD));

    /** 粗米块。 */
    public static final DeferredBlock<Block> COARSE_RICE_BLOCK =
            BLOCKS.registerBlock("coarse_rice_block", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 米块。 */
    public static final DeferredBlock<Block> RICE_BLOCK =
            BLOCKS.registerBlock("rice_block", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 米石块：米石的压缩存储块。 */
    public static final DeferredBlock<Block> RICE_STONE_BLOCK =
            BLOCKS.registerBlock("rice_stone_block", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 堕落台：装饰性全立方块，无 GUI/交互逻辑。 */
    public static final DeferredBlock<Block> FALLEN_ALTAR =
            BLOCKS.registerBlock("fallen_altar", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.NETHERITE_BLOCK));

    /** 残堕深渊岩。 */
    public static final DeferredBlock<Block> REMNANT_FALLEN_ABYSS_ROCK =
            BLOCKS.registerBlock("remnant_fallen_abyss_rock", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE));

    /** 残堕基岩：比深渊岩更坚固。 */
    public static final DeferredBlock<Block> REMNANT_FALLEN_BEDROCK =
            BLOCKS.registerBlock("remnant_fallen_bedrock", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(4.0F, 9.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE));

    /** 磨制残堕深渊岩。 */
    public static final DeferredBlock<Block> POLISHED_REMNANT_FALLEN_ABYSS_ROCK =
            BLOCKS.registerBlock("polished_remnant_fallen_abyss_rock", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE));

    // ============================ 植物与装饰 ============================

    /** 米草：小型十字植物方块，空手破坏不掉落，仅剪刀采集掉落米草物品。 */
    public static final DeferredBlock<BushBlock> RICE_GRASS =
            BLOCKS.registerBlock("rice_grass", BushBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .replaceable()
                            .noCollision()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .offsetType(BlockBehaviour.OffsetType.XZ)
                            .ignitedByLava()
                            .pushReaction(PushReaction.POPPED));

    /** 米羊羊毛：羊毛类全立方块，空手可挖。 */
    public static final DeferredBlock<Block> MIMI_WOOL =
            BLOCKS.registerBlock("mimi_wool", Block::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(0.8F)
                            .sound(SoundType.WOOL));

    // ============================ 机器方块 ============================

    /** 搅碎器：3 入 1 出熔炉式定时加工，右键打开 GUI。 */
    public static final DeferredBlock<CrusherBlock> CRUSHER =
            BLOCKS.registerBlock("crusher", CrusherBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 造米台：原料 + 水桶 → 米，右键打开 GUI。 */
    public static final DeferredBlock<RiceMakerBlock> RICE_MAKER =
            BLOCKS.registerBlock("rice_maker", RiceMakerBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 锤米台：无序双输入工作台式即时合成，右键打开 GUI。 */
    public static final DeferredBlock<RicePoundingTableBlock> RICE_POUNDING_TABLE =
            BLOCKS.registerBlock("rice_pounding_table", RicePoundingTableBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    /** 沉淀台：双输入定时加工（无燃料），右键打开 GUI。 */
    public static final DeferredBlock<PrecipitationTableBlock> PRECIPITATION_TABLE =
            BLOCKS.registerBlock("precipitation_table", PrecipitationTableBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 凝锭台：5x5 网格工作台式即时合成，右键打开 GUI。 */
    public static final DeferredBlock<IngotCondensationTableBlock> INGOT_CONDENSATION_TABLE =
            BLOCKS.registerBlock("ingot_condensation_table", IngotCondensationTableBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 大煮锅：3x3 网格 + 右上输入 + 燃料 + 输出，右键打开 GUI。 */
    public static final DeferredBlock<LargeCookingPotBlock> LARGE_COOKING_POT =
            BLOCKS.registerBlock("large_cooking_pot", LargeCookingPotBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 巨无霸大大大煮锅：5x5 网格 + 右上输入 + 燃料 + 输出，右键打开 GUI。 */
    public static final DeferredBlock<JumboMegaCookingPotBlock> JUMBO_MEGA_COOKING_POT =
            BLOCKS.registerBlock("jumbo_mega_cooking_pot", JumboMegaCookingPotBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 铁锅：菱形 4 输入 + 右上输入 + 燃料 + 输出，右键打开 GUI。 */
    public static final DeferredBlock<IronWokBlock> IRON_WOK =
            BLOCKS.registerBlock("iron_wok", IronWokBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 烤箱：3x3 网格 + 右上输入 + 燃料 + 输出，正面朝向放置玩家，右键打开 GUI。 */
    public static final DeferredBlock<OvenBlock> OVEN =
            BLOCKS.registerBlock("oven", OvenBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 搅拌器：9 输入 + 1 输出工作台式即时合成，右键打开 GUI。 */
    public static final DeferredBlock<MixerBlock> MIXER =
            BLOCKS.registerBlock("mixer", MixerBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 炉灶：煮锅加热源，正面朝向放置玩家，无 GUI。 */
    public static final DeferredBlock<StoveBlock> STOVE =
            BLOCKS.registerBlock("stove", StoveBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));

    // ============================ Boss 头颅摆件与堕落十字架 ============================

    /** 头颅通用属性：木质、硬度 1.0、空手可采、不遮挡。 */
    private static BlockBehaviour.Properties headProperties() {
        return BlockBehaviour.Properties.of()
                .strength(1.0F)
                .sound(SoundType.WOOD)
                .noOcclusion();
    }

    /** 米米猴头：米米猴王掉落的头颅摆件。 */
    public static final DeferredBlock<BossHeadBlock> MIMI_MONKEY_HEAD =
            BLOCKS.registerBlock("mimi_monkey_head",
                    properties -> new BossHeadBlock(Shapes.box(0.0, 0.0, 0.3125, 1.0, 0.625, 0.6875),
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.MIMI_MONKEY_HEAD.get(),
                            properties),
                    ModBlocks::headProperties);

    /** 饭晶虎王头。 */
    public static final DeferredBlock<BossHeadBlock> RICE_CRYSTAL_TIGER_KING_HEAD =
            BLOCKS.registerBlock("rice_crystal_tiger_king_head",
                    properties -> new BossHeadBlock(Shapes.box(0.25, 0.0, 0.22, 0.75, 0.7, 0.78),
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.RICE_CRYSTAL_TIGER_KING_HEAD.get(),
                            properties),
                    ModBlocks::headProperties);

    /** 超晶牛头。 */
    public static final DeferredBlock<BossHeadBlock> SUPER_CRYSTAL_OX_HEAD =
            BLOCKS.registerBlock("super_crystal_ox_head",
                    properties -> new BossHeadBlock(Shapes.box(0.01, 0.0, 0.14, 0.99, 0.72, 0.86),
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.SUPER_CRYSTAL_OX_HEAD.get(),
                            properties),
                    ModBlocks::headProperties);

    /** 重晶兔王头。 */
    public static final DeferredBlock<BossHeadBlock> HEAVY_CRYSTAL_RABBIT_KING_HEAD =
            BLOCKS.registerBlock("heavy_crystal_rabbit_king_head",
                    properties -> new BossHeadBlock(Shapes.box(0.33, 0.0, 0.34, 0.67, 0.7, 0.66),
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.HEAVY_CRYSTAL_RABBIT_KING_HEAD.get(),
                            properties),
                    ModBlocks::headProperties);

    /** 复晶天青蟒头。 */
    public static final DeferredBlock<BossHeadBlock> COMPOUND_CRYSTAL_AZURE_PYTHON_HEAD =
            BLOCKS.registerBlock("compound_crystal_azure_python_head",
                    properties -> new BossHeadBlock(Shapes.box(0.30, 0.0, 0.20, 0.70, 0.69, 0.80),
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.COMPOUND_CRYSTAL_AZURE_PYTHON_HEAD.get(),
                            properties),
                    ModBlocks::headProperties);

    /** 清洁长命夔头。 */
    public static final DeferredBlock<BossHeadBlock> PURIFIED_LONGEVITY_KUI_HEAD =
            BLOCKS.registerBlock("purified_longevity_kui_head",
                    properties -> new BossHeadBlock(Shapes.box(0.18, 0.0, 0.0, 0.82, 0.62, 1.0),
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.PURIFIED_LONGEVITY_KUI_HEAD.get(),
                            properties),
                    ModBlocks::headProperties);

    /** 最懒终末龙头。 */
    public static final DeferredBlock<BossHeadBlock> LAZIEST_END_DRAGON_HEAD =
            BLOCKS.registerBlock("laziest_end_dragon_head",
                    properties -> new BossHeadBlock(Shapes.box(0.36, 0.0, 0.0, 0.64, 0.54, 1.0),
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.LAZIEST_END_DRAGON_HEAD.get(),
                            properties),
                    ModBlocks::headProperties);

    /** 第八宗罪龙头。 */
    public static final DeferredBlock<BossHeadBlock> DIBAZONGZUI_HEAD =
            BLOCKS.registerBlock("dibazongzui_head",
                    properties -> new BossHeadBlock(Shapes.box(0.26, 0.0, 0.0, 0.74, 0.65, 1.0),
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.DIBAZONGZUI_HEAD.get(),
                            properties),
                    ModBlocks::headProperties);

    /** 堕落十字架：暗下界合金质地，需铁镐以上才掉落。 */
    public static final DeferredBlock<FallenCrossBlock> FALLEN_CROSS =
            BLOCKS.registerBlock("fallen_cross",
                    properties -> new FallenCrossBlock(
                            () -> com.mimiworld.mimiworld.block.entity.ModBlockEntities.FALLEN_CROSS.get(),
                            properties),
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.NETHERITE_BLOCK));

    /** 残堕界传送门：贴地紫色星空薄层，实体踩入即与残堕界双向传送。 */
    public static final DeferredBlock<RemnantFallenPortalBlock> REMNANT_FALLEN_PORTAL =
            BLOCKS.registerBlock("remnant_fallen_portal", RemnantFallenPortalBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .noCollision()
                            .noOcclusion()
                            .lightLevel(state -> 11)
                            .strength(-1.0F, 3600000.0F)
                            .sound(SoundType.GLASS));

    /** 米米传送器：传送门框架方块，12 个插眼后激活。 */
    public static final DeferredBlock<MimiTeleporterBlock> MIMI_TELEPORTER =
            BLOCKS.registerBlock("mimi_teleporter", MimiTeleporterBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL));

    /** 米米传送门：仪式激活后生成在框架中心，实体踩入即与米米维度双向传送。 */
    public static final DeferredBlock<MimiPortalBlock> MIMI_PORTAL =
            BLOCKS.registerBlock("mimi_portal", MimiPortalBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .noCollision()
                            .noOcclusion()
                            .lightLevel(state -> 11)
                            .strength(-1.0F, 3600000.0F)
                            .sound(SoundType.GLASS));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}

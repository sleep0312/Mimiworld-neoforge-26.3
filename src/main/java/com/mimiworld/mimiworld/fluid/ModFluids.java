package com.mimiworld.mimiworld.fluid;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.ModItems;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 米米世界流体注册：米汤（rice_soup，米桶）与饭汤（rice_broth，饭桶）。
 *
 * <p>26.3 的流体三件套：FluidType（属性）、源/流动流体（BaseFlowingFluid）、
 * 世界中的流体方块（LiquidBlock，无 BlockItem）。米汤与饭汤各注册一个专属炼药锅；
 * 两种桶行为均同原版水桶。</p>
 */
public final class ModFluids {

    private ModFluids() {
    }

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MimiWorld.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, MimiWorld.MODID);
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MimiWorld.MODID);

    // ===== FluidType =====
    public static final DeferredHolder<FluidType, FluidType> RICE_SOUP_TYPE =
            FLUID_TYPES.register("rice_soup", () -> new FluidType(
                    FluidType.Properties.create()
                            .density(2000)
                            .viscosity(6000)
                            .lightLevel(0)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<FluidType, FluidType> RICE_BROTH_TYPE =
            FLUID_TYPES.register("rice_broth", () -> new FluidType(
                    FluidType.Properties.create()
                            .density(2000)
                            .viscosity(7000)
                            .lightLevel(0)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    // ===== 源流体 / 流动流体 =====
    public static final DeferredHolder<Fluid, FlowingFluid> RICE_SOUP =
            FLUIDS.register("rice_soup", () -> new RiceSoupFluid.Source(makeProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_RICE_SOUP =
            FLUIDS.register("flowing_rice_soup", () -> new RiceSoupFluid.Flowing(makeProperties()));

    public static final DeferredHolder<Fluid, FlowingFluid> RICE_BROTH =
            FLUIDS.register("rice_broth", () -> new RiceBrothFluid.Source(makeBrothProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_RICE_BROTH =
            FLUIDS.register("flowing_rice_broth", () -> new RiceBrothFluid.Flowing(makeBrothProperties()));

    // ===== 流体方块 =====
    public static final DeferredBlock<LiquidBlock> RICE_SOUP_BLOCK =
            BLOCKS.registerBlock("rice_soup",
                    properties -> new LiquidBlock(RICE_SOUP.get(), properties),
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SNOW)
                            .replaceable()
                            .noCollision()
                            .strength(100.0F)
                            .pushReaction(PushReaction.IGNORE_ENTITY)
                            .noLootTable()
                            .liquid()
                            .sound(SoundType.EMPTY));

    public static final DeferredBlock<LiquidBlock> RICE_BROTH_BLOCK =
            BLOCKS.registerBlock("rice_broth",
                    properties -> new LiquidBlock(RICE_BROTH.get(), properties),
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SAND)
                            .replaceable()
                            .noCollision()
                            .strength(100.0F)
                            .pushReaction(PushReaction.IGNORE_ENTITY)
                            .noLootTable()
                            .liquid()
                            .sound(SoundType.EMPTY));

    // ===== 炼药锅交互表 =====
    private static CauldronInteraction.Dispatcher soupCauldronInteractions() {
        CauldronInteraction.Dispatcher dispatcher = new CauldronInteraction.Dispatcher();
        dispatcher.put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
            if (state.getValue(LayeredCauldronBlock.LEVEL) != 3) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                        new net.minecraft.world.item.ItemStack(ModItems.RICE_BUCKET.get())));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(Items.BUCKET));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        });
        return dispatcher;
    }

    private static CauldronInteraction.Dispatcher brothCauldronInteractions() {
        CauldronInteraction.Dispatcher dispatcher = new CauldronInteraction.Dispatcher();
        dispatcher.put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
            if (state.getValue(LayeredCauldronBlock.LEVEL) != 3) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                        new net.minecraft.world.item.ItemStack(ModItems.RICE_PAIL.get())));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(Items.BUCKET));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        });
        return dispatcher;
    }

    public static final DeferredBlock<LayeredCauldronBlock> RICE_SOUP_CAULDRON =
            BLOCKS.registerBlock("rice_soup_cauldron",
                    properties -> new LayeredCauldronBlock(
                            Biome.Precipitation.RAIN, soupCauldronInteractions(), properties),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON));

    public static final DeferredBlock<LayeredCauldronBlock> RICE_BROTH_CAULDRON =
            BLOCKS.registerBlock("rice_broth_cauldron",
                    properties -> new LayeredCauldronBlock(
                            Biome.Precipitation.RAIN, brothCauldronInteractions(), properties),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON));

    private static BaseFlowingFluid.Properties makeProperties() {
        return new BaseFlowingFluid.Properties(RICE_SOUP_TYPE, RICE_SOUP, FLOWING_RICE_SOUP)
                .block(RICE_SOUP_BLOCK)
                .bucket(ModItems.RICE_BUCKET);
    }

    private static BaseFlowingFluid.Properties makeBrothProperties() {
        return new BaseFlowingFluid.Properties(RICE_BROTH_TYPE, RICE_BROTH, FLOWING_RICE_BROTH)
                .block(RICE_BROTH_BLOCK)
                .bucket(ModItems.RICE_PAIL);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
    }

    /** 炼药锅流体内容注册（能力互操作）。 */
    public static void registerCauldronFluidContent(RegisterCauldronFluidContentEvent event) {
        event.register(RICE_SOUP_CAULDRON.get(), RICE_SOUP.get(), FluidType.BUCKET_VOLUME,
                LayeredCauldronBlock.LEVEL);
        event.register(RICE_BROTH_CAULDRON.get(), RICE_BROTH.get(), FluidType.BUCKET_VOLUME,
                LayeredCauldronBlock.LEVEL);
    }

    /**
     * 把米桶/饭桶注册进原版「空炼药锅」交互表（commonSetup 主线程阶段调用）。
     * 桶对空锅使用后：空锅变成装满的对应汤锅、桶变空铁桶、播放水桶倒出音效。
     */
    public static void registerCauldronInteractions() {
        CauldronInteractions.EMPTY.put(ModItems.RICE_BUCKET.get(),
                (state, level, pos, player, hand, stack) -> {
                    if (!level.isClientSide()) {
                        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                                new net.minecraft.world.item.ItemStack(Items.BUCKET)));
                        player.awardStat(Stats.FILL_CAULDRON);
                        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                        level.setBlockAndUpdate(pos, RICE_SOUP_CAULDRON.get().defaultBlockState()
                                .setValue(LayeredCauldronBlock.LEVEL, 3));
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                    }
                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
                });
        CauldronInteractions.EMPTY.put(ModItems.RICE_PAIL.get(),
                (state, level, pos, player, hand, stack) -> {
                    if (!level.isClientSide()) {
                        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                                new net.minecraft.world.item.ItemStack(Items.BUCKET)));
                        player.awardStat(Stats.FILL_CAULDRON);
                        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                        level.setBlockAndUpdate(pos, RICE_BROTH_CAULDRON.get().defaultBlockState()
                                .setValue(LayeredCauldronBlock.LEVEL, 3));
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                    }
                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
                });
    }
}


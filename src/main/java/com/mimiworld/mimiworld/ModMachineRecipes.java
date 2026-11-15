package com.mimiworld.mimiworld;

import com.mimiworld.mimiworld.block.entity.DualInputProcessorBlockEntity;
import com.mimiworld.mimiworld.block.entity.PrecipitationTableBlockEntity;
import com.mimiworld.mimiworld.block.entity.RiceMakerBlockEntity;
import com.mimiworld.mimiworld.block.entity.RicePoundingTableBlockEntity;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

/**
 * 工作方块（非原版工作台/熔炉）配方统一注册入口。
 *
 * <p>在 {@code FMLCommonSetupEvent} 中调用（物品注册表已冻结，可安全读取注册项）。
 * 本批次覆盖：造米台、锤米台、沉淀台。</p>
 *
 * <p>搅碎器 / 搅拌器 / 煮锅类 / 凝锭台的配方依赖尚未移植的食物与材料物品，
 * 待对应批次补齐后再加入本类。</p>
 */
public final class ModMachineRecipes {

    private ModMachineRecipes() {
    }

    public static void register() {
        // ---------- 造米台：1 小麦种子 + 100 mB 水 → 1 米（2 秒） ----------
        RiceMakerBlockEntity.registerRecipe(new RiceMakerBlockEntity.RiceRecipe(
                Items.WHEAT_SEEDS,
                new ItemStackTemplate(ModItems.RICE.get()),
                RiceMakerBlockEntity.RICE_WATER_COST,
                RiceMakerBlockEntity.RICE_PROCESS_TICKS));

        // ---------- 锤米台：无序双输入，即时合成 ----------
        // 燧石 + 米原木 → 米木板
        RicePoundingTableBlockEntity.registerRecipe(
                Items.FLINT,
                ModItems.RICE_LOG.get(),
                new ItemStackTemplate(ModItems.RICE_PLANKS.get()));

        // 结晶米 + 铁锭 → 召唤核心
        RicePoundingTableBlockEntity.registerRecipe(
                ModItems.CRYSTALLIZED_RICE.get(),
                Items.IRON_INGOT,
                new ItemStackTemplate(ModItems.SUMMON_CORE.get()));

        // 重结晶米 + 钻石 → 高级召唤核心
        RicePoundingTableBlockEntity.registerRecipe(
                ModItems.RECRYSTALLIZED_RICE.get(),
                Items.DIAMOND,
                new ItemStackTemplate(ModItems.ADVANCED_SUMMON_CORE.get()));

        // 熔融混合结晶 + 下界合金锭 → 最懒召唤核心
        RicePoundingTableBlockEntity.registerRecipe(
                ModItems.MOLTEN_MIXED_CRYSTAL.get(),
                Items.NETHERITE_INGOT,
                new ItemStackTemplate(ModItems.LAZIEST_SUMMON_CORE.get()));

        // 超重结晶 + 硬化米 → 超重结晶杆
        RicePoundingTableBlockEntity.registerRecipe(
                ModItems.SUPER_HEAVY_CRYSTAL.get(),
                ModItems.HARDENED_RICE.get(),
                new ItemStackTemplate(ModItems.SUPER_HEAVY_CRYSTAL_ROD.get()));

        // 复合米饭锭 + 结晶米 → 复合米饭杆
        RicePoundingTableBlockEntity.registerRecipe(
                ModItems.COMPOUND_RICE_MEAL_INGOT.get(),
                ModItems.CRYSTALLIZED_RICE.get(),
                new ItemStackTemplate(ModItems.COMPOUND_RICE_MEAL_ROD.get()));

        // 清洁复合锭 + 重结晶米 → 清洁复合杆
        RicePoundingTableBlockEntity.registerRecipe(
                ModItems.CLEANSING_COMPOUND_INGOT.get(),
                ModItems.RECRYSTALLIZED_RICE.get(),
                new ItemStackTemplate(ModItems.CLEANSING_COMPOUND_ROD.get()));

        // 超懒合金锭 + 熔融混合结晶 → 超懒合金杆
        RicePoundingTableBlockEntity.registerRecipe(
                ModItems.SUPER_LAZY_ALLOY_INGOT.get(),
                ModItems.MOLTEN_MIXED_CRYSTAL.get(),
                new ItemStackTemplate(ModItems.SUPER_LAZY_ALLOY_ROD.get()));

        // ---------- 沉淀台：双输入定时加工（无燃料） ----------
        // 粗制超懒合金锭 + 清洁米沉离液 → 超懒合金锭（2 秒）
        PrecipitationTableBlockEntity.registerRecipe(
                ModItems.COARSE_SUPER_LAZY_ALLOY_INGOT.get(),
                ModItems.CLEANSING_RICE_SEPARATION_SOLUTION.get(),
                new ItemStackTemplate(ModItems.SUPER_LAZY_ALLOY_INGOT.get()),
                40);

        // 米果渣 + 3 玻璃瓶 → 3 米果汁（每周期消耗 1 米果渣 + 3 玻璃瓶）
        PrecipitationTableBlockEntity.registerRecipe(
                ModItems.RICE_POMACE.get(), 1,
                Items.GLASS_BOTTLE, 3,
                new ItemStackTemplate(ModItems.RICE_FRUIT_JUICE.get(), 3),
                DualInputProcessorBlockEntity.DEFAULT_PROCESS_TICKS);

        // 米果汁 + 米 → 米果清洁液（2 秒）
        PrecipitationTableBlockEntity.registerRecipe(
                ModItems.RICE_FRUIT_JUICE.get(),
                ModItems.RICE.get(),
                new ItemStackTemplate(ModItems.RICE_FRUIT_CLEANSING_SOLUTION.get()),
                40);
    }
}

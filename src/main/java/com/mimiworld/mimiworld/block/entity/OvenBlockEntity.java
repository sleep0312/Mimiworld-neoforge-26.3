package com.mimiworld.mimiworld.block.entity;

import com.mimiworld.mimiworld.menu.OvenMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 烤箱方块实体（oven）：GUI/槽位与大煮锅相同，
 * 3x3 网格（9 入）+ 右上单独输入 + 燃料 + 输出。配方表独立（与大煮锅不通用）。
 */
public class OvenBlockEntity extends GridCookingPotBlockEntity {

    private static final List<PotRecipe> RECIPES = new ArrayList<>();

    /** 注册一条默认时长（200t）配方。 */
    public static void registerRecipe(ItemStackTemplate result, Item extra, Item... grid) {
        registerRecipe(result, extra, DEFAULT_COOK_TICKS, grid);
    }

    /** 注册一条自定义时长配方。 */
    public static void registerRecipe(ItemStackTemplate result, Item extra, int cookTicks, Item... grid) {
        Item[] copy = new Item[9];
        System.arraycopy(grid, 0, copy, 0, Math.min(grid.length, 9));
        RECIPES.add(new PotRecipe(copy, extra, result, cookTicks));
    }

    /** JEI 只读快照。 */
    public static List<PotRecipe> jeiRecipes() {
        return List.copyOf(RECIPES);
    }

    public OvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OVEN.get(), pos, state, 3);
    }

    @Override
    protected PotRecipe findRecipe(ItemStack[] grid, ItemStack extra) {
        return matchGrid(RECIPES, grid, extra);
    }

    @Override
    protected AbstractContainerMenu createPotMenu(int containerId, Inventory playerInventory,
                                                  ContainerData data, ContainerLevelAccess access) {
        return new OvenMenu(containerId, playerInventory, this, data, access);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mimiworld.oven");
    }
}

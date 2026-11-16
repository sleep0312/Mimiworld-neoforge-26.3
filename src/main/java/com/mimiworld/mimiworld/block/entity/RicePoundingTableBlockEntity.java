package com.mimiworld.mimiworld.block.entity;

import com.mimiworld.mimiworld.menu.RicePoundingTableMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 锤米台方块实体（rice_pounding_table）。
 * <p>
 * 合成方式类似原版工作台（即时合成、无加工计时）：
 * <ul>
 *   <li>槽 0 / 槽 1：两个输入槽，<b>任意物品</b>都可放入；</li>
 *   <li>槽 2：输出槽，两个输入满足配方时即时显示 1 个合成结果（仅预览），
 *       玩家取走结果时两个输入槽各消耗 1 个；shift+取走可连续批量合成。</li>
 * </ul>
 * 配方为无序双输入（两槽物品顺序无关）。
 */
public class RicePoundingTableBlockEntity extends BlockEntity implements MenuProvider, Container {

    public static final int SLOT_INPUT_A = 0;
    public static final int SLOT_INPUT_B = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int TOTAL_SLOTS = 3;

    // -------------------- 配方表 --------------------

    /** 一条「物品 A + 物品 B → 产物」配方，A/B 顺序无关。 */
    public record PoundingRecipe(Item inputA, Item inputB, ItemStackTemplate result) {
    }

    /** 键：两个输入物品的注册名按字典序排序后拼接，保证无序匹配。 */
    private static final Map<String, PoundingRecipe> RECIPES = new HashMap<>();

    /** 注册一条无序双输入配方。应在模组加载阶段调用。 */
    public static void registerRecipe(Item inputA, Item inputB, ItemStackTemplate result) {
        RECIPES.put(recipeKey(inputA, inputB), new PoundingRecipe(inputA, inputB, result));
    }

    /** JEI 只读快照。 */
    public static List<PoundingRecipe> jeiRecipes() {
        return List.copyOf(RECIPES.values());
    }

    private static String recipeKey(Item a, Item b) {
        Identifier keyA = BuiltInRegistries.ITEM.getKey(a);
        Identifier keyB = BuiltInRegistries.ITEM.getKey(b);
        return keyA.compareTo(keyB) <= 0 ? keyA + "|" + keyB : keyB + "|" + keyA;
    }

    /** 查询两槽物品能否合成：两槽都非空且命中配方时返回结果副本，否则返回空栈。 */
    public static ItemStack getCraftResult(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return ItemStack.EMPTY;
        }
        PoundingRecipe recipe = RECIPES.get(recipeKey(a.getItem(), b.getItem()));
        return recipe == null ? ItemStack.EMPTY : recipe.result().create();
    }

    // -------------------- 状态 --------------------

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public RicePoundingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RICE_POUNDING_TABLE.get(), pos, state);
    }

    // -------------------- 服务端 tick --------------------

    /** 服务端每 tick 刷新输出预览。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, RicePoundingTableBlockEntity be) {
        be.refreshResultPreview();
    }

    /** 依据两个输入槽刷新输出槽的预览结果（不消耗输入）。 */
    public void refreshResultPreview() {
        ItemStack expected = getCraftResult(items.get(SLOT_INPUT_A), items.get(SLOT_INPUT_B));
        ItemStack current = items.get(SLOT_OUTPUT);

        if (expected.isEmpty()) {
            if (!current.isEmpty()) {
                items.set(SLOT_OUTPUT, ItemStack.EMPTY);
                setChanged();
            }
            return;
        }
        boolean same = !current.isEmpty()
                && ItemStack.isSameItemSameComponents(current, expected)
                && current.getCount() == expected.getCount();
        if (!same) {
            items.set(SLOT_OUTPUT, expected);
            setChanged();
        }
    }

    /** 玩家取走 1 次合成结果时调用：两输入各消耗 1 个，然后刷新下一预览。 */
    public void consumeInputsForCraft() {
        items.get(SLOT_INPUT_A).shrink(1);
        items.get(SLOT_INPUT_B).shrink(1);
        refreshResultPreview();
        setChanged();
    }

    /** 关闭界面时清掉尚未取走的预览结果（预览不是真实产物）。 */
    public void clearResultPreview() {
        if (!items.get(SLOT_OUTPUT).isEmpty()) {
            items.set(SLOT_OUTPUT, ItemStack.EMPTY);
            setChanged();
        }
    }

    // -------------------- 存档 --------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        // 载入后立即按当前输入重算预览，纠正存档中可能残留的旧预览
        refreshResultPreview();
    }

    // -------------------- Container 实现 --------------------

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    /** 两个输入槽接受任意物品（工作台网格语义），输出槽不可放入。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT_A || slot == SLOT_INPUT_B;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // -------------------- 菜单 --------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mimiworld.rice_pounding_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(level, worldPosition);
        return new RicePoundingTableMenu(containerId, playerInventory, this, access);
    }

    /** 方块被移除时供 Block 调用：先抹掉预览再掉落真实物品。 */
    public void dropContents(Level level, BlockPos pos) {
        clearResultPreview();
        Containers.dropContents(level, pos, this);
    }
}

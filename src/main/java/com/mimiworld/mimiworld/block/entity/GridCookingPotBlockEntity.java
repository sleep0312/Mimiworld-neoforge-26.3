package com.mimiworld.mimiworld.block.entity;

import com.mimiworld.mimiworld.block.CookingHeaters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * NxN 网格煮锅方块实体基类（大煮锅 3x3 / 巨无霸 5x5 / 铁锅 2x2 / 烤箱 3x3）。
 * <p>
 * 熔炉式燃料定时加工（默认 200 tick）：左上 NxN 网格 + 右上单独输入槽共同构成配料，
 * 左下燃料槽烧炼，右下输出槽存放成品。配方为网格逐槽<b>有序</b>匹配（null=该格必须为空）
 * + 右上槽指定物品，各子类持有独立配方表。
 * <p>
 * 26.3 起燃料燃烧时长改为物品数据组件 {@code COOKING_FUEL}，需通过战利品上下文解析。
 * <ul>
 *   <li>槽 0 ~ N*N-1：NxN 输入网格（行优先）；</li>
 *   <li>槽 N*N：右上单独输入槽；</li>
 *   <li>槽 N*N+1：燃料槽；</li>
 *   <li>槽 N*N+2：输出槽，禁止手动放入。</li>
 * </ul>
 */
public abstract class GridCookingPotBlockEntity extends BlockEntity
        implements MenuProvider, net.minecraft.world.Container {

    public static final int DEFAULT_COOK_TICKS = 200;

    /** 炉灶供能时对外显示的燃料条刻度（litTime=litDuration=此值，火焰恒满）。 */
    public static final int STOVE_FULL_LIT = 200;

    /** 一条煮锅配方：grid[i] 为网格第 i 格要求物品（null=该格必须为空），extra 为右上槽要求（null=必须空）。 */
    public record PotRecipe(Item[] grid, Item extra, ItemStackTemplate result, int cookTicks) {
    }

    protected final int gridSize;
    protected final int extraSlot;
    protected final int fuelSlot;
    protected final int resultSlot;
    protected final NonNullList<ItemStack> items;

    private int litTime = 0;
    private int litDuration = 0;
    private int cookingProgress = 0;
    private int cookingTotalTime = DEFAULT_COOK_TICKS;

    /* DataSlot 布局：0 = 燃料剩余燃烧 tick，1 = 当前燃料总燃烧 tick，2 = 烹煮进度，3 = 烹煮总时长 */
    public static final int DATA_LIT_TIME = 0;
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_MAX_PROGRESS = 3;
    public static final int DATA_COUNT = 4;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_LIT_TIME -> litTime;
                case DATA_LIT_DURATION -> litDuration;
                case DATA_PROGRESS -> cookingProgress;
                case DATA_MAX_PROGRESS -> cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_LIT_TIME -> litTime = value;
                case DATA_LIT_DURATION -> litDuration = value;
                case DATA_PROGRESS -> cookingProgress = value;
                case DATA_MAX_PROGRESS -> cookingTotalTime = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected GridCookingPotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        int gridSize) {
        super(type, pos, state);
        this.gridSize = gridSize;
        this.extraSlot = gridSize * gridSize;
        this.fuelSlot = extraSlot + 1;
        this.resultSlot = fuelSlot + 1;
        this.items = NonNullList.withSize(resultSlot + 1, ItemStack.EMPTY);
    }

    // -------------------- 子类扩展点 --------------------

    public final int getGridSize() {
        return gridSize;
    }

    public final int getExtraSlot() {
        return extraSlot;
    }

    public final int getFuelSlot() {
        return fuelSlot;
    }

    public final int getResultSlot() {
        return resultSlot;
    }

    /** 查本机独立配方表；无命中返回 null。 */
    @Nullable
    protected abstract PotRecipe findRecipe(ItemStack[] grid, ItemStack extra);

    /** 由具体子类创建对应的菜单。 */
    protected abstract AbstractContainerMenu createPotMenu(int containerId, Inventory playerInventory,
                                                           ContainerData data, ContainerLevelAccess access);

    /** 子类配方表有序匹配工具：网格逐槽比对（配方 null 位要求该格为空）+ 右上槽比对。 */
    @Nullable
    protected static PotRecipe matchGrid(List<PotRecipe> recipes,
                                         ItemStack[] grid, ItemStack extra) {
        outer:
        for (PotRecipe recipe : recipes) {
            if (recipe.grid().length != grid.length) {
                continue;
            }
            for (int i = 0; i < grid.length; i++) {
                Item expected = recipe.grid()[i];
                ItemStack got = grid[i];
                if (expected == null) {
                    if (!got.isEmpty()) {
                        continue outer;
                    }
                } else if (got.isEmpty() || got.getItem() != expected) {
                    continue outer;
                }
            }
            Item expectedExtra = recipe.extra();
            if (expectedExtra == null) {
                if (!extra.isEmpty()) {
                    continue outer;
                }
            } else if (extra.isEmpty() || extra.getItem() != expectedExtra) {
                continue outer;
            }
            return recipe;
        }
        return null;
    }

    // -------------------- 服务端 tick（原版熔炉节奏） --------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  GridCookingPotBlockEntity be) {
        be.tick();
    }

    private void tick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean changed = false;
        boolean wasLit = litTime > 0;
        // 炉灶供能：主方块正下方为 #mimiworld:stoves 标签方块
        boolean powered = CookingHeaters.hasStoveBelow(level, worldPosition);

        if (powered) {
            // 不烧燃料：燃料火焰条保持满格
            litTime = STOVE_FULL_LIT;
            litDuration = STOVE_FULL_LIT;
        } else if (litTime > 0) {
            litTime--;
        }

        ItemStack[] grid = collectGrid();
        ItemStack extra = items.get(extraSlot);
        PotRecipe recipe = findRecipe(grid, extra);
        boolean canCook = recipe != null && canOutputAccept(recipe.result());
        if (canCook) {
            cookingTotalTime = recipe.cookTicks();
        }

        // 无炉灶、需要点火且当前火已灭：消耗一个燃料
        if (!powered && litTime == 0 && canCook) {
            int burnTime = burnTime(serverLevel, items.get(fuelSlot));
            if (burnTime > 0) {
                ItemStack fuel = items.get(fuelSlot);
                UseRemainder remainder = fuel.get(DataComponents.USE_REMAINDER);
                if (remainder != null) {
                    items.set(fuelSlot, remainder.convertInto().create());
                } else {
                    fuel.shrink(1);
                }
                litTime = burnTime;
                litDuration = burnTime;
                changed = true;
            }
        }

        // 有热源（炉灶或燃料）且能煮：推进进度，满一个周期产出
        boolean isLit = powered || litTime > 0;
        if (isLit && canCook) {
            cookingProgress++;
            if (cookingProgress >= cookingTotalTime) {
                cookingProgress = 0;
                cookingTotalTime = recipe.cookTicks();
                for (int i = 0; i < extraSlot; i++) {
                    if (recipe.grid()[i] != null) {
                        items.get(i).shrink(1);
                    }
                }
                if (recipe.extra() != null) {
                    items.get(extraSlot).shrink(1);
                }
                ItemStack output = items.get(resultSlot);
                if (output.isEmpty()) {
                    items.set(resultSlot, recipe.result().create());
                } else {
                    output.grow(recipe.result().count());
                }
                changed = true;
            }
        } else if (cookingProgress > 0) {
            // 条件不满足时进度缓慢回退（同原版熔炉）
            cookingProgress = Math.max(0, cookingProgress - 2);
            changed = true;
        }

        if (wasLit != (litTime > 0)) {
            changed = true;
        }

        data.set(DATA_LIT_TIME, litTime);
        data.set(DATA_LIT_DURATION, litDuration);
        data.set(DATA_PROGRESS, cookingProgress);
        data.set(DATA_MAX_PROGRESS, cookingTotalTime);

        if (changed) {
            setChanged();
        }
    }

    private ItemStack[] collectGrid() {
        ItemStack[] grid = new ItemStack[extraSlot];
        for (int i = 0; i < extraSlot; i++) {
            grid[i] = items.get(i);
        }
        return grid;
    }

    /** 输出槽能否再放入这份结果：空槽可放；同种物品堆叠不爆自身上限可放。 */
    private boolean canOutputAccept(ItemStackTemplate result) {
        ItemStack output = items.get(resultSlot);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result.create())) {
            return false;
        }
        return output.getCount() + result.count() <= output.getMaxStackSize();
    }

    /**
     * 解析燃料燃烧时长（tick）：26.3 使用物品数据组件 {@code COOKING_FUEL}，
     * 其值为战利品上下文可解析的数值，需要构建与原版容器方块实体一致的上下文。
     */
    private int burnTime(ServerLevel serverLevel, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        LootContext context = new LootContext.Builder(new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.BLOCK_STATE, getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(getBlockPos()))
                .withParameter(LootContextParams.CONTAINER, this)
                .withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, stack)
                .create(LootContextParamSets.CONTAINER_PROCESS))
                .create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL,
                CookingFuel::burnTime, context, 0);
    }

    // -------------------- 存档 --------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BurnTime", litTime);
        output.putInt("BurnDuration", litDuration);
        output.putInt("CookTime", cookingProgress);
        output.putInt("CookTimeTotal", cookingTotalTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        litTime = input.getIntOr("BurnTime", 0);
        litDuration = input.getIntOr("BurnDuration", 0);
        cookingProgress = input.getIntOr("CookTime", 0);
        cookingTotalTime = input.getIntOr("CookTimeTotal", DEFAULT_COOK_TICKS);
        if (cookingTotalTime <= 0) {
            cookingTotalTime = DEFAULT_COOK_TICKS;
        }
    }

    // -------------------- Container --------------------

    @Override
    public int getContainerSize() {
        return items.size();
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

    /** 网格槽与右上输入槽接受任意物品；燃料槽只收带燃料组件的物品；输出槽不可放入。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) {
            return false;
        }
        if (slot == resultSlot) {
            return false;
        }
        if (slot == fuelSlot) {
            return stack.has(DataComponents.COOKING_FUEL);
        }
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    public void dropContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, this);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ContainerLevelAccess access = level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(level, worldPosition);
        return createPotMenu(containerId, playerInventory, data, access);
    }
}

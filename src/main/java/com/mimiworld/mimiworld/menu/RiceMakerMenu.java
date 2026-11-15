package com.mimiworld.mimiworld.menu;

import com.mimiworld.mimiworld.block.ModBlocks;
import com.mimiworld.mimiworld.block.entity.RiceMakerBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 造米台菜单：左上原料槽、左下注水槽（仅水桶）、右侧输出槽；下方为玩家背包。
 * 槽位坐标与 rice_maker.png 中槽框左上角严格一致。
 */
public class RiceMakerMenu extends AbstractContainerMenu {

    // 机器槽（16x16 凹槽左上角的像素坐标）
    public static final int SLOT_INPUT_X = 30;
    public static final int SLOT_INPUT_Y = 26;
    public static final int SLOT_WATER_BUCKET_X = 30;
    public static final int SLOT_WATER_BUCKET_Y = 62;
    public static final int SLOT_OUTPUT_X = 112;
    public static final int SLOT_OUTPUT_Y = 43;

    // 玩家背包槽
    private static final int PLAYER_INV_START_X = 8;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;

    private static final int MACHINE_SLOTS = 3;
    private static final int INV_SLOT_START = MACHINE_SLOTS;          // 3
    private static final int INV_SLOT_END = INV_SLOT_START + 27;      // 30（不含）
    private static final int HOTBAR_SLOT_END = INV_SLOT_END + 9;      // 39（不含）

    private final Container container;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** 客户端构造。 */
    public RiceMakerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(MACHINE_SLOTS),
                new SimpleContainerData(RiceMakerBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL);
    }

    /** 服务端构造：由 RiceMakerBlockEntity#createMenu 调用。 */
    public RiceMakerMenu(int containerId, Inventory playerInventory, Container container,
                         ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.RICE_MAKER_MENU.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, RiceMakerBlockEntity.DATA_COUNT);
        this.container = container;
        this.access = access;
        this.data = data;

        // 原料槽（仅配方原料，客户端容器不是 BE，故显式重写 mayPlace 与服务端保持一致）
        addSlot(new Slot(container, RiceMakerBlockEntity.SLOT_INPUT, SLOT_INPUT_X, SLOT_INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RiceMakerBlockEntity.findRecipe(stack) != null;
            }
        });
        // 注水槽：仅水桶；投入后由 BlockEntity 立即换成空桶并注水
        addSlot(new Slot(container, RiceMakerBlockEntity.SLOT_WATER_BUCKET,
                SLOT_WATER_BUCKET_X, SLOT_WATER_BUCKET_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET);
            }
        });
        // 输出槽：只能取，不能放
        addSlot(new Slot(container, RiceMakerBlockEntity.SLOT_OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 玩家背包 3x9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INV_START_X + col * 18,
                        PLAYER_INV_START_Y + row * 18));
            }
        }
        // 快捷栏 9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_START_X + col * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    /** 当前加工进度（0~加工总时长）。 */
    public int getProgress() {
        return data.get(RiceMakerBlockEntity.DATA_PROGRESS);
    }

    /** 加工总进度（tick）。 */
    public int getMaxProgress() {
        return Math.max(1, data.get(RiceMakerBlockEntity.DATA_MAX_PROGRESS));
    }

    /** 当前储水量（mB，0~10000）。 */
    public int getWater() {
        return data.get(RiceMakerBlockEntity.DATA_WATER);
    }

    /** 储水上限（mB）。 */
    public int getMaxWater() {
        return Math.max(1, data.get(RiceMakerBlockEntity.DATA_MAX_WATER));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.RICE_MAKER.get());
    }

    /**
     * Shift+点击移动物品：
     * 机器槽 → 玩家背包；玩家背包 → 原料槽/注水槽（各自 mayPlace 过滤，输出槽不接收）；
     * 背包↔快捷栏互转。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack clicked = slot.getItem();
            result = clicked.copy();

            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(clicked, INV_SLOT_START, HOTBAR_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(clicked, result);
            } else {
                // 玩家背包 → 尝试原料槽 + 注水槽
                if (!moveItemStackTo(clicked, 0, MACHINE_SLOTS - 1, false)) {
                    if (index < INV_SLOT_END) {
                        if (!moveItemStackTo(clicked, INV_SLOT_END, HOTBAR_SLOT_END, true)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!moveItemStackTo(clicked, INV_SLOT_START, INV_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (clicked.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (clicked.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, clicked);
            this.broadcastChanges();
        }
        return result;
    }
}

package com.mimiworld.mimiworld.menu;

import com.mimiworld.mimiworld.block.ModBlocks;
import com.mimiworld.mimiworld.block.entity.DualInputProcessorBlockEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;

/**
 * 沉淀台菜单：槽位布局见 {@link DualInputProcessorMenu}。
 */
public class PrecipitationTableMenu extends DualInputProcessorMenu {

    /** 客户端构造。 */
    public PrecipitationTableMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                new SimpleContainer(DualInputProcessorBlockEntity.TOTAL_SLOTS),
                new SimpleContainerData(DualInputProcessorBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL);
    }

    /** 服务端构造：由 PrecipitationTableBlockEntity#createMenu 调用。 */
    public PrecipitationTableMenu(int containerId, Inventory playerInventory,
                                  Container container, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.PRECIPITATION_TABLE_MENU.get(), containerId, playerInventory,
                container, data, access);
    }

    @Override
    protected Block validBlock() {
        return ModBlocks.PRECIPITATION_TABLE.get();
    }
}

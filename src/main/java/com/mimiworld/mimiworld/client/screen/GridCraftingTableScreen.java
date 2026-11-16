package com.mimiworld.mimiworld.client.screen;

import com.mimiworld.mimiworld.menu.GridCraftingTableMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * NxN 网格工作台式 GUI 基类（凝锭台 5x5 等）。
 * <p>
 * 使用各机器自绘贴图（256x256，左上角 imageWidth x imageHeight 为有效画面），
 * 网格槽、箭头、结果槽全部烘在贴图内（即时合成无进度条），本 Screen 只做整图 blit；
 * 标题与背包名由原版默认实现绘制。
 */
public abstract class GridCraftingTableScreen<M extends GridCraftingTableMenu>
        extends AbstractContainerScreen<M> {

    private static final int IMAGE_WIDTH = 176;
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;
    private static final int INVENTORY_LABEL_X = 8;

    private final Identifier texture;
    private final int imageH;
    private final int invLabelY;

    protected GridCraftingTableScreen(M menu, Inventory inventory, Component title,
                                      Identifier texture, int imageHeight, int inventoryLabelY) {
        super(menu, inventory, title, IMAGE_WIDTH, imageHeight);
        this.texture = texture;
        this.imageH = imageHeight;
        this.invLabelY = inventoryLabelY;
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = INVENTORY_LABEL_X;
        this.inventoryLabelY = inventoryLabelY;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = INVENTORY_LABEL_X;
        this.inventoryLabelY = invLabelY;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos, this.topPos,
                0.0F, 0.0F, IMAGE_WIDTH, imageH, 256, 256);
    }
}

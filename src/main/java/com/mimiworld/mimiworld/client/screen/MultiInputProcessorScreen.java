package com.mimiworld.mimiworld.client.screen;

import com.mimiworld.mimiworld.menu.MultiInputProcessorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 多输入熔炉式加工台 GUI 基类（搅碎器等共用）。
 * <p>
 * 贴图固定 256x256，左上角 176 x {@code imageHeight} 为有效画面（标准机器 166）；
 * 槽框/装饰/灰色进度底图烘在贴图里，白色进度精灵放在贴图留白区。
 * 标题与背包标签由原版默认实现绘制；进度动画由子类 {@link #renderProgress} 绘制。
 */
public abstract class MultiInputProcessorScreen<M extends MultiInputProcessorMenu>
        extends AbstractContainerScreen<M> {

    protected static final int IMAGE_WIDTH = 176;
    protected static final int TITLE_X = 8;
    protected static final int TITLE_Y = 6;

    protected final Identifier texture;
    protected final int imageH;

    protected MultiInputProcessorScreen(M menu, Inventory inventory, Component title,
                                        Identifier texture, int imageHeight, int inventoryLabelY) {
        super(menu, inventory, title, IMAGE_WIDTH, imageHeight);
        this.texture = texture;
        this.imageH = imageHeight;
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = inventoryLabelY;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = 8;
    }

    /** 子类绘制进度动画（机器区装饰/灰底已烘在贴图内）。 */
    protected abstract void renderProgress(GuiGraphicsExtractor graphics, int x, int y);

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos, this.topPos,
                0.0F, 0.0F, IMAGE_WIDTH, imageH, 256, 256);
        renderProgress(graphics, this.leftPos, this.topPos);
    }
}

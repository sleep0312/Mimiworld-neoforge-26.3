package com.mimiworld.mimiworld.client.screen;

import com.mimiworld.mimiworld.menu.GridCookingPotMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * NxN 网格煮锅 GUI 基类（大煮锅 / 巨无霸 / 烤箱 / 铁锅共用）。
 * <p>
 * 贴图内已烘灰色火焰底（网格下方 14x14）与三条灰色横箭头（网格与右上输入槽之间）；
 * 点亮的橙色火焰精灵在留白区 UV(176,0)，白色三箭头进度精灵在 UV(176,14)，
 * 后者宽度按烹煮进度从左向右裁切增长。
 */
public abstract class GridCookingPotScreen extends AbstractContainerScreen<GridCookingPotMenu> {

    private static final int IMAGE_WIDTH = 176;
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;
    private static final int FLAME_SIZE = 14;
    private static final int ARROW_REGION_HEIGHT = 26;

    // 留白区精灵 UV
    private static final int SPRITE_FLAME_U = 176;
    private static final int SPRITE_FLAME_V = 0;
    private static final int SPRITE_ARROW_U = 176;
    private static final int SPRITE_ARROW_V = 14;

    private final Identifier texture;
    private final int imageHeight;
    private final int flameX;
    private final int flameY;
    private final int arrowX;
    private final int arrowTop;
    private final int arrowWidth;

    protected GridCookingPotScreen(GridCookingPotMenu menu, Inventory inventory, Component title,
                                   Identifier texture, int imageHeight, int inventoryLabelY,
                                   int flameX, int flameY,
                                   int arrowX, int arrowTop, int arrowWidth) {
        super(menu, inventory, title, IMAGE_WIDTH, imageHeight);
        this.texture = texture;
        this.imageHeight = imageHeight;
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = TITLE_X;
        this.inventoryLabelY = inventoryLabelY;
        this.flameX = flameX;
        this.flameY = flameY;
        this.arrowX = arrowX;
        this.arrowTop = arrowTop;
        this.arrowWidth = arrowWidth;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = TITLE_X;
        // 屏幕高度不足（topPos 为负）时贴顶显示，避免标题被上边缘裁掉
        if (this.topPos < 0) {
            this.topPos = 0;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
                IMAGE_WIDTH, imageHeight, 256, 256);

        // 点亮火焰（从底部向上增长）
        int litDuration = this.menu.getLitDuration();
        if (this.menu.getLitTime() > 0 && litDuration > 0) {
            int litHeight = FLAME_SIZE * this.menu.getLitTime() / litDuration;
            if (litHeight > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                        x + flameX, y + flameY + FLAME_SIZE - litHeight,
                        SPRITE_FLAME_U, SPRITE_FLAME_V + FLAME_SIZE - litHeight,
                        FLAME_SIZE, litHeight, 256, 256);
            }
        }

        // 烹煮进度：三条横箭头整体从左向右增长
        int maxProgress = this.menu.getMaxProgress();
        int progress = this.menu.getProgress();
        if (progress > 0) {
            int filled = arrowWidth * progress / maxProgress;
            if (filled > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                        x + arrowX, y + arrowTop,
                        SPRITE_ARROW_U, SPRITE_ARROW_V,
                        filled, ARROW_REGION_HEIGHT, 256, 256);
            }
        }
    }
}

package com.mimiworld.mimiworld.client.screen;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.menu.IronWokMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 铁锅 GUI（iron_wok.png 176x178）：菱形四输入 + 右上输入 + 燃料 + 输出。
 * 贴图烘灰色火焰底与三条灰色横箭头；点亮橙火精灵 UV(176,0) 14x14，
 * 白色三箭头进度精灵 UV(176,14) 26x26，按宽度从左向右矩形裁切。
 */
public class IronWokScreen extends AbstractContainerScreen<IronWokMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "textures/gui/iron_wok.png");

    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 178;
    private static final int FLAME_SIZE = 14;
    private static final int ARROW_REGION_HEIGHT = 26;

    private static final int FLAME_X = 37;
    private static final int FLAME_Y = 56;
    private static final int ARROW_X = 86;
    private static final int ARROW_TOP = 22;
    private static final int ARROW_WIDTH = 24;

    public IronWokScreen(IronWokMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 84;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F,
                IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);

        // 点亮火焰（从底部向上增长）
        int litDuration = this.menu.getLitDuration();
        if (this.menu.getLitTime() > 0 && litDuration > 0) {
            int litHeight = FLAME_SIZE * this.menu.getLitTime() / litDuration;
            if (litHeight > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                        x + FLAME_X, y + FLAME_Y + FLAME_SIZE - litHeight,
                        176, FLAME_SIZE - litHeight,
                        FLAME_SIZE, litHeight, 256, 256);
            }
        }

        // 烹煮进度：三条横箭头整体从左向右增长
        int maxProgress = this.menu.getMaxProgress();
        int progress = this.menu.getProgress();
        if (progress > 0) {
            int filled = ARROW_WIDTH * progress / maxProgress;
            if (filled > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                        x + ARROW_X, y + ARROW_TOP,
                        176, 14,
                        filled, ARROW_REGION_HEIGHT, 256, 256);
            }
        }
    }
}

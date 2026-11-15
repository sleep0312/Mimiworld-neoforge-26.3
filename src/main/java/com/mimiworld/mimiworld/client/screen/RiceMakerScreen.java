package com.mimiworld.mimiworld.client.screen;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.menu.RiceMakerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 造米台 GUI：贴图 256x256，左上角 176x166 为有效画面；
 * 槽框/箭头底图均在贴图内，进度箭头填充与水位填充由代码渲染。
 */
public class RiceMakerScreen extends AbstractContainerScreen<RiceMakerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "textures/gui/rice_maker.png");

    // 与贴图一致的画面尺寸（严格 176x166）
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 166;

    // 箭头在 GUI 内的位置/尺寸，与贴图中的底图严格对齐
    private static final int ARROW_X = 62;
    private static final int ARROW_Y = 44;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;
    // 完整亮色箭头精灵在 256x256 贴图中的 UV（位于 GUI 画面之外的留白区）
    private static final int ARROW_FILL_U = 0;
    private static final int ARROW_FILL_V = 200;

    // 水位刻度表：贴图 (143,9) 为 18x64 外框，内膛 16x62 从 (144,10) 起
    private static final int GAUGE_FRAME_X = 143;
    private static final int GAUGE_FRAME_Y = 9;
    private static final int GAUGE_FRAME_W = 18;
    private static final int GAUGE_FRAME_H = 64;
    private static final int GAUGE_INNER_X = 144;
    private static final int GAUGE_INNER_Y = 10;
    private static final int GAUGE_INNER_W = 16;
    private static final int GAUGE_INNER_H = 62;
    // 原版水体蓝 + 水面 1px 浅蓝高光
    private static final int WATER_COLOR = 0xFF3F76E4;
    private static final int WATER_SURFACE_COLOR = 0xFF5B94F5;

    // 标题位置（贴图上无任何文字，全部代码渲染）
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;
    private static final int INVENTORY_LABEL_X = 8;
    private static final int INVENTORY_LABEL_Y = 72;

    public RiceMakerScreen(RiceMakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = INVENTORY_LABEL_X;
        this.inventoryLabelY = INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
        this.inventoryLabelX = INVENTORY_LABEL_X;
        this.inventoryLabelY = INVENTORY_LABEL_Y;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        // GUI 底图（仅左上角 176x166 有画面，其余透明）
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F,
                IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);

        // 进度箭头：按加工进度横向裁切亮色箭头精灵
        int fillWidth = (int) Math.ceil(this.menu.getProgress() * (double) ARROW_WIDTH / this.menu.getMaxProgress());
        if (fillWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    x + ARROW_X, y + ARROW_Y,
                    ARROW_FILL_U, ARROW_FILL_V,
                    fillWidth, ARROW_HEIGHT,
                    256, 256);
        }

        // 水位：从刻度表内膛底部向上填充（高度向下取整，避免水量不足时溢出一格）
        int water = this.menu.getWater();
        int maxWater = this.menu.getMaxWater();
        int fillHeight = Math.max(0, Math.min(GAUGE_INNER_H,
                (int) ((long) water * GAUGE_INNER_H / maxWater)));
        if (fillHeight > 0) {
            int fillTop = y + GAUGE_INNER_Y + GAUGE_INNER_H - fillHeight;
            int fillBottom = y + GAUGE_INNER_Y + GAUGE_INNER_H;
            graphics.fill(x + GAUGE_INNER_X, fillTop,
                    x + GAUGE_INNER_X + GAUGE_INNER_W, fillBottom, WATER_COLOR);
            // 水面 1px 高光
            graphics.fill(x + GAUGE_INNER_X, fillTop,
                    x + GAUGE_INNER_X + GAUGE_INNER_W, fillTop + 1, WATER_SURFACE_COLOR);
        }
    }

    /** 悬停水位表时只显示剩余水量一行（mB，1 桶 = 1000 mB）。 */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (isHovering(GAUGE_FRAME_X, GAUGE_FRAME_Y, GAUGE_FRAME_W, GAUGE_FRAME_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font,
                    Component.literal("水: " + this.menu.getWater() + " mB"),
                    mouseX, mouseY);
        }
    }
}

package com.mimiworld.mimiworld.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * mimiworld 全部工作方块共用的 JEI 分类实现。
 * <p>
 * 一台机器 = 一个本类实例（独立 {@link IRecipeType} + 机器方块作图标/催化剂）。
 * 背景直接取该机器 GUI 贴图的 176 x panelHeight 区域（panelHeight 为面板中
 * 玩家背包起始行，即只截取机器操作区），槽位坐标全部是 GUI 内的绝对像素坐标，
 * 直接来自各 Menu 的 addSlot —— 因此 JEI 页面与游戏内界面逐像素一致。
 * <p>
 * 槽框、进度箭头、火焰底均已在贴图内烘焙，所以这里不再绘制 JEI 槽位背景，
 * 也不再自绘箭头/火焰，只按坐标摆放配料。
 */
public class ViewCategory extends AbstractRecipeCategory<JeiView> {

    /** GUI 贴图统一宽度 */
    public static final int GUI_WIDTH = 176;

    private final IDrawable background;
    private final int outputX;
    private final int outputY;

    /**
     * @param texture     机器 GUI 贴图（mimiworld:textures/gui/&lt;机器&gt;.png）
     * @param panelHeight 需要展示的面板高度（不含玩家背包，且不包含背包槽框顶边那一行）
     * @param outputX     产物槽在 GUI 中的绝对坐标
     * @param outputY     产物槽在 GUI 中的绝对坐标
     */
    public ViewCategory(IGuiHelper guiHelper, IRecipeType<JeiView> recipeType, ItemStack catalyst,
                        Identifier texture, int panelHeight, int outputX, int outputY) {
        super(recipeType, catalyst.getHoverName(), guiHelper.createDrawableItemStack(catalyst),
                GUI_WIDTH, panelHeight);
        this.outputX = outputX;
        this.outputY = outputY;
        this.background = guiHelper.createDrawable(texture, 0, 0, GUI_WIDTH, panelHeight);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, JeiView recipe, IFocusGroup focuses) {
        for (JeiView.Slot slot : recipe.inputs()) {
            IRecipeSlotBuilder slotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, slot.x(), slot.y());
            if (slot.tag() != null) {
                // 标签配料（如任意肉类）：展开为标签内全部物品轮播
                slotBuilder.add(Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(slot.tag())));
            } else if (slot.fluid() != null) {
                // 流体配料（如造米台 100mB 水）：JEI 默认流体渲染器
                slotBuilder.add(slot.fluid(), slot.fluidAmount());
            } else if (slot.stack() != null && !slot.stack().isEmpty()) {
                slotBuilder.add(slot.stack());
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, outputX, outputY)
                .add(recipe.output());
        if (recipe.shapeless()) {
            builder.setShapeless();
        }
    }

    @Override
    public void draw(JeiView recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
                     double mouseX, double mouseY) {
        // 背景即机器 GUI 操作区贴图（槽框/箭头/火焰已烘焙在贴图中），绘制在配料槽下方
        background.draw(guiGraphics, 0, 0);
    }
}

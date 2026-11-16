package com.mimiworld.mimiworld.client.renderer;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mimiworld.mimiworld.item.MimiBowItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 在弓的「string」骨骼上渲染拉弓时搭在弦上的箭。
 *
 * <p>贴图不写死：取持有者背包里将要射出的那支箭（{@link LivingEntity#getProjectile(ItemStack)}），
 * 因此 8 种箭拉弓时各自显示自己的物品贴图。箭只在「有人正在拉这把弓」时出现，
 * 物品栏图标、掉落物、展示框里的弓不会冒出来。</p>
 *
 * <p>位置由骨骼决定：{@code string} 骨就是弦中点（拉弓动画里它会跟着弦往后退），
 * 所以箭会随拉弓动作自然后移。贴图里的箭身约呈 45° 斜向右上，经过物品模型自身的
 * FIXED 显示变换（绕 Y 翻 180°）后再绕 Z 轴转 45°，指向模型正前方（-X，即箭射出去的方向）。</p>
 */
public class NockedArrowLayer extends GeoRenderLayer<MimiBowItem, GeoItemRenderer.RenderData, GeoRenderState> {

    /** 弦上的搭箭点骨骼名。 */
    private static final String NOCK_BONE = "string";
    /**
     * 贴图里箭身斜向右上约 45°。26.3 的物品模型在 {@link ItemDisplayContext#FIXED} 下会先绕 Y 轴翻 180°
     * （贴图里的 45° 实际已经变成左上 135°），所以这里只需再转 45° 就能让箭指向模型正前方 -X。
     */
    private static final float SPRITE_TILT_DEGREES = 45.0F;
    /** 箭尾到贴图中心的 X 距离（格），用于把箭尾对齐到弦上。 */
    private static final float TAIL_OFFSET_X = -0.53F;

    /** 渲染状态中携带「弦上这支箭」的物品栈。 */
    public static final DataTicket<ItemStack> NOCKED_ARROW =
            DataTickets.create("mimi_nocked_arrow", ItemStack.class);
    /** 渲染状态中携带持有者，用于解析箭的物品模型。 */
    public static final DataTicket<net.minecraft.world.entity.ItemOwner> NOCKED_OWNER =
            DataTickets.create("mimi_nocked_owner", net.minecraft.world.entity.ItemOwner.class);

    private final ItemStackRenderState arrowRenderState = new ItemStackRenderState();

    public NockedArrowLayer(GeoItemRenderer<MimiBowItem> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(MimiBowItem animatable, GeoItemRenderer.RenderData renderData,
                              GeoRenderState renderState, float partialTick) {
        net.minecraft.world.entity.ItemOwner owner = renderData.itemOwner();
        LivingEntity holder = owner == null ? null : owner.asLivingEntity();
        ItemStack arrow = ItemStack.EMPTY;
        if (holder != null) {
            arrow = holder.getProjectile(renderData.itemStack());
        }
        renderState.addGeckolibData(NOCKED_ARROW, arrow);
        renderState.addGeckolibData(NOCKED_OWNER, owner);
    }

    @Override
    public void preRender(RenderPassInfo<GeoRenderState> info, SubmitNodeCollector collector) {
        if (!Boolean.TRUE.equals(info.getGeckolibData(MimiBowRenderer.DRAWING))) {
            return;
        }
        ItemStack arrow = info.getGeckolibData(NOCKED_ARROW);
        net.minecraft.world.entity.ItemOwner owner = info.getGeckolibData(NOCKED_OWNER);
        if (arrow == null || arrow.isEmpty() || owner == null) {
            return;
        }
        info.model().getBone(NOCK_BONE).ifPresent(bone -> info.addPerBoneRender(bone, (passInfo, b, nodes) -> {
            this.arrowRenderState.clear();
            Minecraft.getInstance().getItemModelResolver()
                    .updateForTopItem(this.arrowRenderState, arrow, ItemDisplayContext.FIXED,
                            owner.level(), owner, 0);
            PoseStack poseStack = passInfo.poseStack();
            poseStack.pushPose();

            // 先对齐箭尾、再摆正朝向（PoseStack 后调用的先作用到几何体上）
            poseStack.translate(TAIL_OFFSET_X, 0.0F, 0.0F);
            poseStack.rotateDegrees(Axis.ZP, SPRITE_TILT_DEGREES);

            this.arrowRenderState.submit(poseStack, nodes,
                    passInfo.packedLight(), passInfo.packedOverlay(), 0);
            poseStack.popPose();
        }));
    }
}

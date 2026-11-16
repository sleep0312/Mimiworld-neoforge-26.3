package com.mimiworld.mimiworld.client.renderer;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mimiworld.mimiworld.client.model.ThrownMonkeyStaffModel;
import com.mimiworld.mimiworld.entity.ThrownMonkeyStaffEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;

/**
 * 飞行金箍棒渲染器：geo 中棒体沿 Z 轴躺平，这里只做 Y 轴朝向旋转，
 * 让棒头始终对准飞行方向（yaw 在实体 tick 中由速度向量计算）。
 */
public class ThrownMonkeyStaffRenderer
        extends GeoEntityRenderer<ThrownMonkeyStaffEntity, EntityRenderState> {

    public ThrownMonkeyStaffRenderer(EntityRendererProvider.Context context) {
        super(context, new ThrownMonkeyStaffModel());
        this.shadowRadius = 0.3F;
    }

    @Override
    public void extractRenderState(ThrownMonkeyStaffEntity entity, EntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // 默认的 applyRotations 读取 ENTITY_BODY_YAW 并绕 Y 轴旋转 180 - yaw
        ((GeoRenderState) state).addGeckolibData(DataTickets.ENTITY_BODY_YAW,
                Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void applyRotations(RenderPassInfo passInfo, PoseStack poseStack, float ageInTicks) {
        float yaw = passInfo.renderState() instanceof GeoRenderState state
                ? state.getOrDefaultGeckolibData(DataTickets.ENTITY_BODY_YAW, 0.0F)
                : 0.0F;
        poseStack.rotateDegrees(Axis.YP, 180.0F - yaw);
        // 绕棒体长轴轻微自转，飞行时棒头箍有视觉变化
        poseStack.rotateDegrees(Axis.ZP, ageInTicks * 0.4F);
    }
}

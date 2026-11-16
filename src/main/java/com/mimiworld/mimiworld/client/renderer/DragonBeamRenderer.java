package com.mimiworld.mimiworld.client.renderer;

import com.mimiworld.mimiworld.entity.DragonBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * 龙系光束实体渲染器。
 *
 * <p>26.3 的自定义几何管线为提交式，绘制光束需要自行管理 {@code SubmitNodeCollector}；
 * 本工程改用服务端粒子沿「龙眼 → 目标」铺紫色光带（见
 * {@link DragonBeamEntity}），因此这里只保留一个空渲染器，
 * 既保证实体有对应 renderer、又不产生额外几何。</p>
 */
public class DragonBeamRenderer extends EntityRenderer<DragonBeamEntity, EntityRenderState> {

    public DragonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        // 光束视觉由服务端粒子负责
    }
}

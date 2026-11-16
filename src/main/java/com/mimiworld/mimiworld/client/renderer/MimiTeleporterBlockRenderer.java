package com.mimiworld.mimiworld.client.renderer;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.block.MimiTeleporterBlock;
import com.mimiworld.mimiworld.block.entity.MimiTeleporterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 米米传送器（mimi_teleporter）方块实体渲染器：已插眼时在方块正上方渲染一只
 * 缓慢旋转、轻微上下浮动的米米之眼；激活序列期间转速加快。
 *
 * <p>26.3 渲染管线为提交式（{@code SubmitNodeCollector}），这里用
 * {@code submitCustomGeometry} 直接铺两片十字交叉的贴图四边形，避免依赖物品模型解析。</p>
 */
public class MimiTeleporterBlockRenderer
        implements BlockEntityRenderer<MimiTeleporterBlockEntity, MimiTeleporterBlockRenderer.State> {

    private static final Identifier EYE_TEXTURE =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "textures/item/mimi_eye.png");
    private static final RenderType EYE_RENDER_TYPE = RenderTypes.entityTranslucent(EYE_TEXTURE);
    private static final float BASE_SPIN = 1.8F;
    private static final float BOOST = 10.0F;

    public MimiTeleporterBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MimiTeleporterBlockEntity be, State state, float partialTick,
                                   net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(be, state, overlay);
        Level level = be.getLevel();
        if (level == null) {
            state.hasEye = false;
            return;
        }
        BlockState blockState = level.getBlockState(be.getBlockPos());
        state.hasEye = blockState.hasProperty(MimiTeleporterBlock.EYE)
                && blockState.getValue(MimiTeleporterBlock.EYE);
        if (!state.hasEye) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        boolean inSequence = be.seqStart != MimiTeleporterBlockEntity.SEQ_NONE && be.seqCenter != null;
        float seqT = inSequence ? time - be.seqStart : 0.0F;
        float spinSpeed = BASE_SPIN;
        if (inSequence) {
            if (seqT <= 60.0F) {
                spinSpeed = BASE_SPIN * (1.0F + (BOOST - 1.0F) * (seqT / 60.0F));
            } else if (seqT <= 300.0F) {
                spinSpeed = BASE_SPIN * BOOST;
            } else {
                spinSpeed = BASE_SPIN * (1.0F + (BOOST - 1.0F) * Math.max(0.0F, 1.0F - (seqT - 300.0F) / 60.0F));
            }
        }
        state.spin = (time * spinSpeed) % 360.0F;
        state.bob = Mth.sin(time * 0.12F) * 0.08F;
        state.time = time;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.hasEye) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.05F + state.bob, 0.5F);
        poseStack.rotateDegrees(Axis.YP, state.spin);
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, EYE_RENDER_TYPE, (pose, consumer) -> {
            float half = 0.28F;
            // 正面片
            consumer.addVertex(pose, -half, -half, 0.0F).setColor(255, 255, 255, 230).setUv(0.0F, 1.0F)
                    .setLight(light).setOverlay(0).setNormal(pose, 0.0F, 0.0F, -1.0F);
            consumer.addVertex(pose, half, -half, 0.0F).setColor(255, 255, 255, 230).setUv(1.0F, 1.0F)
                    .setLight(light).setOverlay(0).setNormal(pose, 0.0F, 0.0F, -1.0F);
            consumer.addVertex(pose, half, half, 0.0F).setColor(255, 255, 255, 230).setUv(1.0F, 0.0F)
                    .setLight(light).setOverlay(0).setNormal(pose, 0.0F, 0.0F, -1.0F);
            consumer.addVertex(pose, -half, half, 0.0F).setColor(255, 255, 255, 230).setUv(0.0F, 0.0F)
                    .setLight(light).setOverlay(0).setNormal(pose, 0.0F, 0.0F, -1.0F);
            // 背面片（双面可见）
            consumer.addVertex(pose, half, -half, 0.0F).setColor(255, 255, 255, 230).setUv(0.0F, 1.0F)
                    .setLight(light).setOverlay(0).setNormal(pose, 0.0F, 0.0F, 1.0F);
            consumer.addVertex(pose, -half, -half, 0.0F).setColor(255, 255, 255, 230).setUv(1.0F, 1.0F)
                    .setLight(light).setOverlay(0).setNormal(pose, 0.0F, 0.0F, 1.0F);
            consumer.addVertex(pose, -half, half, 0.0F).setColor(255, 255, 255, 230).setUv(1.0F, 0.0F)
                    .setLight(light).setOverlay(0).setNormal(pose, 0.0F, 0.0F, 1.0F);
            consumer.addVertex(pose, half, half, 0.0F).setColor(255, 255, 255, 230).setUv(0.0F, 0.0F)
                    .setLight(light).setOverlay(0).setNormal(pose, 0.0F, 0.0F, 1.0F);
        });
        poseStack.popPose();
    }

    /** 渲染状态：是否插眼、当前旋转角度、上下浮动量。 */
    public static class State extends BlockEntityRenderState {
        public boolean hasEye;
        public float spin;
        public float bob;
        public float time;
    }
}

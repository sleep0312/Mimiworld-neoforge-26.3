package com.mimiworld.mimiworld.client.renderer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mimiworld.mimiworld.client.model.GeoDecorationModel;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 纯装饰 GeckoLib 方块渲染器（7 种 Boss 头颅 + 堕落十字架共用）。
 *
 * <p>GeckoLib 5 在 {@code preRenderPass} 中先把模型平移到方块中心 (0.5, 0, 0.5)，
 * 再按 {@code withScale} 缩放。旧版 1.20.1 的位移是在缩放之前叠加的，
 * 因此这里把旧渲染器的平移换算成「世界坐标下额外偏移」：</p>
 * <pre>dx = TX + 0.5·s - 0.5，dy = TY，dz = TZ + 0.5·s - 0.5</pre>
 * <p>各类头颅的具体参数在 {@code MimiWorldClient} 注册处给出。</p>
 */
public class GeoDecorationBlockRenderer<T extends BlockEntity & GeoAnimatable>
        extends GeoBlockRenderer<T, BlockEntityRenderState> {

    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    public GeoDecorationBlockRenderer(BlockEntityRendererProvider.Context context,
                                      Identifier assetSubpath, float scale,
                                      float offsetX, float offsetY, float offsetZ) {
        super(context, new GeoDecorationModel<T>(assetSubpath));
        withScale(scale);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void preRenderPass(RenderPassInfo info, SubmitNodeCollector collector) {
        super.preRenderPass(info, collector);
        info.poseStack().translate(offsetX, offsetY, offsetZ);
    }
}

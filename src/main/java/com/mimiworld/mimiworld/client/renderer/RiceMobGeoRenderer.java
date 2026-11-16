package com.mimiworld.mimiworld.client.renderer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * 米米系列生物通用 GeckoLib 渲染器。
 *
 * <p>使用 {@link RiceMobGeoModel} 的约定路径加载模型/贴图/动画；
 * 26.3 的实体渲染为 RenderState 架构，状态类型直接用 {@link EntityRenderState}。</p>
 */
public class RiceMobGeoRenderer<T extends Entity & GeoAnimatable>
        extends GeoEntityRenderer<T, EntityRenderState> {

    public RiceMobGeoRenderer(EntityRendererProvider.Context context, String id, float shadowRadius) {
        super(context, new com.mimiworld.mimiworld.client.model.RiceMobGeoModel<>(id));
        this.shadowRadius = shadowRadius;
    }
}

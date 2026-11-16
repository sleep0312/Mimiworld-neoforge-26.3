package com.mimiworld.mimiworld.client.model;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.mimiworld.mimiworld.MimiWorld;
import net.minecraft.resources.Identifier;

/**
 * 米米系列生物通用 GeckoLib 模型。
 *
 * <p>按实体 ID 拼接固定资源路径，免去每个生物写一个模型类：</p>
 * <ul>
 *   <li>模型：{@code geckolib/models/<id>.json}</li>
 *   <li>贴图：{@code textures/entity/<id>.png}</li>
 *   <li>动画：{@code geckolib/animations/<id>.json}</li>
 * </ul>
 *
 * <p>GeckoLib 5 自行补全 {@code geckolib/models/}、{@code geckolib/animations/} 与 {@code .json}
 * 前后缀，故这里只传裸 id。</p>
 */
public class RiceMobGeoModel<T extends GeoAnimatable> extends GeoModel<T> {

    private final Identifier model;
    private final Identifier texture;
    private final Identifier animation;

    public RiceMobGeoModel(String id) {
        this.model = Identifier.fromNamespaceAndPath(MimiWorld.MODID, id);
        this.texture = Identifier.fromNamespaceAndPath(MimiWorld.MODID, "textures/entity/" + id + ".png");
        this.animation = Identifier.fromNamespaceAndPath(MimiWorld.MODID, id);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return this.model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return this.texture;
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return this.animation;
    }
}

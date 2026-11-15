package com.mimiworld.mimiworld.client.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.mimiworld.mimiworld.item.MimiBowItem;

import net.minecraft.resources.Identifier;

/**
 * 通用 3D 弓模型绑定：按构造参数切换 geo/贴图/动画资源。
 */
public class MimiBowModel extends GeoModel<MimiBowItem> {

    private final Identifier model;
    private final Identifier texture;
    private final Identifier animation;

    public MimiBowModel(Identifier model, Identifier texture, Identifier animation) {
        this.model = model;
        this.texture = texture;
        this.animation = animation;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return texture;
    }

    @Override
    public Identifier getAnimationResource(MimiBowItem animatable) {
        return animation;
    }
}

package com.mimiworld.mimiworld.client.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.mimiworld.mimiworld.item.MimiArmorItem;

import net.minecraft.resources.Identifier;

/**
 * 通用 3D 防具模型绑定：九套防具共用本类，按构造参数切换 geo/贴图/动画资源。
 */
public class MimiArmorModel extends GeoModel<MimiArmorItem> {

    private final Identifier model;
    private final Identifier texture;
    private final Identifier animation;

    public MimiArmorModel(Identifier model, Identifier texture, Identifier animation) {
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
    public Identifier getAnimationResource(MimiArmorItem animatable) {
        return animation;
    }
}

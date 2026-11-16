package com.mimiworld.mimiworld.client.model;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 纯装饰 GeckoLib 方块模型绑定类（7 种 Boss 头颅 + 堕落十字架共用）。
 *
 * <ul>
 *   <li>模型：geckolib/models/&lt;id&gt;.json</li>
 *   <li>贴图：textures/geckolib/&lt;id&gt;.png</li>
 *   <li>动画：geckolib/animations/empty.json（空动画，静态渲染）</li>
 * </ul>
 */
public class GeoDecorationModel<T extends BlockEntity & GeoAnimatable> extends GeoModel<T> {

    private final Identifier model;
    private final Identifier texture;
    private final Identifier animation;

    public GeoDecorationModel(Identifier id) {
        String path = id.getPath();
        this.model = Identifier.fromNamespaceAndPath(id.getNamespace(), path);
        this.texture = Identifier.fromNamespaceAndPath(id.getNamespace(),
                "textures/geckolib/" + path + ".png");
        this.animation = Identifier.fromNamespaceAndPath(id.getNamespace(), "empty");
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
    public Identifier getAnimationResource(T animatable) {
        return animation;
    }
}

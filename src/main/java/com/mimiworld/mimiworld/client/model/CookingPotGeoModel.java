package com.mimiworld.mimiworld.client.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 煮锅系列方块的 GeckoLib 模型绑定类（大煮锅 / 巨无霸 / 铁锅共用一个泛型类）。
 *
 * <p>按注册 id 精确指定三个资源文件的位置：</p>
 * <ul>
 *   <li>geckolib/models/&lt;id&gt;.json（Blockbench GeckoLib 导出）</li>
 *   <li>textures/geckolib/&lt;id&gt;.png（独立目录，不影响物品图标使用的 textures/item 旧贴图）</li>
 *   <li>geckolib/animations/empty.json（空动画，静态渲染）</li>
 * </ul>
 */
public class CookingPotGeoModel<T extends BlockEntity & com.geckolib.animatable.GeoAnimatable>
        extends GeoModel<T> {

    private final Identifier model;
    private final Identifier texture;
    private final Identifier animation;

    public CookingPotGeoModel(Identifier id) {
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

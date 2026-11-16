package com.mimiworld.mimiworld.client.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.entity.ThrownMonkeyStaffEntity;

import net.minecraft.resources.Identifier;

/**
 * 扔出的金箍棒投掷物 GeckoLib 模型绑定。
 *
 * <p>模型 {@code geckolib/models/mimihouwang_staff.json}：一根以原点为中心、沿 Z 轴躺平的金箍棒，
 * 直接复用猴王贴图 {@code textures/entity/mimihouwang.png}；
 * 棒体朝向由渲染器按投掷物 yaw 旋转对齐飞行方向。</p>
 */
public class ThrownMonkeyStaffModel extends GeoModel<ThrownMonkeyStaffEntity> {

    private static final Identifier MODEL =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "mimihouwang_staff");
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "textures/entity/mimihouwang.png");
    private static final Identifier ANIMATION =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "mimihouwang");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ThrownMonkeyStaffEntity animatable) {
        return ANIMATION;
    }
}

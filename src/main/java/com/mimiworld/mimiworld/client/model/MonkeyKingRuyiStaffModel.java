package com.mimiworld.mimiworld.client.model;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.item.MonkeyKingRuyiStaffItem;

import net.minecraft.resources.Identifier;

/**
 * 猴王如意棍的 GeckoLib 物品模型：无动画，仅静态 3D 模型。
 * 各视角（GUI/手持/地面/展示框）的位置缩放由
 * models/item/monkey_king_ruyi_staff.json 的 display 节点控制。
 */
public class MonkeyKingRuyiStaffModel extends GeoModel<MonkeyKingRuyiStaffItem> {

    private static final Identifier MODEL =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "houwangruyigun");
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "textures/item/houwangruyigun.png");
    private static final Identifier ANIMATION =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "empty");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(MonkeyKingRuyiStaffItem animatable) {
        return ANIMATION;
    }
}

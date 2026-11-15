package com.mimiworld.mimiworld.client.renderer;

import com.geckolib.renderer.GeoArmorRenderer;
import com.mimiworld.mimiworld.client.model.MimiArmorModel;
import com.mimiworld.mimiworld.item.MimiArmorItem;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * 通用 3D 防具渲染器。GeckoLib 会按当前装备槽自动隐藏与槽位无关的 armor* 骨。
 */
public class MimiArmorRenderer extends GeoArmorRenderer<MimiArmorItem, HumanoidRenderState> {

    public MimiArmorRenderer(MimiArmorModel model) {
        super(model);
    }
}

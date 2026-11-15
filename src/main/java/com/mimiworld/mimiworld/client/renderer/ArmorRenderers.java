package com.mimiworld.mimiworld.client.renderer;

import com.geckolib.renderer.GeoArmorRenderer;
import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.client.model.MimiArmorModel;

import net.minecraft.resources.Identifier;

/**
 * 九套 3D 防具的资源映射与渲染器工厂。
 *
 * <p>只在客户端（物品渲染时）被调用，服务端不会加载本类。</p>
 */
public final class ArmorRenderers {

    private ArmorRenderers() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MimiWorld.MODID, path);
    }

    public static GeoArmorRenderer<?, ?> create(String modelKey) {
        return switch (modelKey) {
            case "rice_crystal_alloy_armor" ->
                    renderer("rice_crystal_alloy_armor", "empty");
            case "super_heavy_crystal_armor" ->
                    renderer("super_heavy_crystal_armor", "empty");
            case "compound_rice_meal_armor" ->
                    renderer("compound_rice_meal_armor", "empty");
            case "cleansing_compound_armor" ->
                    renderer("cleansing_compound_armor", "empty");
            case "super_lazy_alloy_armor" ->
                    renderer("super_lazy_alloy_armor", "empty");
            case "mimi_divine_wings" ->
                    renderer("mimi_divine_wings", "mimi_divine_wings");
            case "super_lazy_wings_chestplate" ->
                    renderer("super_lazy_wings_chestplate", "super_lazy_wings_chestplate");
            case "laziest_heavenfall_armor" ->
                    renderer("laziest_heavenfall_armor", "laziest_heavenfall_armor");
            case "remnant_fallen_cardinal_sin_armor" ->
                    renderer("remnant_fallen_cardinal_sin_armor", "remnant_fallen_cardinal_sin_armor");
            default -> throw new IllegalArgumentException("Unknown armor model: " + modelKey);
        };
    }

    private static MimiArmorRenderer renderer(String geoName, String animationName) {
        return new MimiArmorRenderer(new MimiArmorModel(
                id(geoName),
                id("textures/armor/" + geoName + ".png"),
                id(animationName)));
    }
}

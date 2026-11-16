package com.mimiworld.mimiworld;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * 米米世界自定义维度的 ResourceKey。
 *
 * <p>维度本身由数据包 JSON 注册：</p>
 * <ul>
 *   <li>data/mimiworld/dimension_type/mimi_dimension.json 与 data/mimiworld/dimension/mimi_dimension.json</li>
 *   <li>data/mimiworld/dimension_type/remnant_fallen_realm.json 与对应 dimension JSON</li>
 * </ul>
 */
public final class ModDimensions {

    private ModDimensions() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MimiWorld.MODID, path);
    }

    public static final ResourceKey<Level> MIMI_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, id("mimi_dimension"));
    public static final ResourceKey<DimensionType> MIMI_DIMENSION_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, id("mimi_dimension"));

    public static final ResourceKey<Level> REMNANT_FALLEN_REALM =
            ResourceKey.create(Registries.DIMENSION, id("remnant_fallen_realm"));
    public static final ResourceKey<DimensionType> REMNANT_FALLEN_REALM_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, id("remnant_fallen_realm"));
}

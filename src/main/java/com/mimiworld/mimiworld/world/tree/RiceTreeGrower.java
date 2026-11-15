package com.mimiworld.mimiworld.world.tree;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 米树生长器。
 *
 * <p>26.3 的树木特征直接注册在 {@link Registries#FEATURE} 注册表（数据包路径
 * {@code data/mimiworld/worldgen/feature/rice_tree.json}），不再区分 configured_feature。
 * 无巨型变种与花树变种，普通生长与骨粉催熟都返回 rice_tree（形态参照白桦树）。</p>
 */
public final class RiceTreeGrower {

    /** 与数据包文件 data/mimiworld/worldgen/feature/rice_tree.json 一一对应。 */
    public static final ResourceKey<Feature> RICE_TREE_KEY =
            ResourceKey.create(Registries.FEATURE,
                    Identifier.fromNamespaceAndPath(MimiWorld.MODID, "rice_tree"));

    /** 供米树苗使用：普通生长固定为 rice_tree，不提供巨型/花树变种。 */
    public static final TreeGrower INSTANCE = new TreeGrower("rice",
            WeightedList.of(RICE_TREE_KEY),
            WeightedList.of(),
            WeightedList.of(),
            RICE_TREE_KEY);

    private RiceTreeGrower() {
    }
}

package com.mimiworld.mimiworld.entity;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 米米世界自定义伤害类型（数据包 {@code data/mimiworld/damage_type/*.json}）。
 *
 * <p>这些伤害类型已加入 {@code minecraft:bypasses_armor} 标签：
 * 龙息/龙撞/光束与兔王砸地均无视护甲。</p>
 */
public final class ModDamageTypes {

    public static final ResourceKey<DamageType> DRAGON_BEAM = key("dragon_beam");
    public static final ResourceKey<DamageType> DRAGON_BREATH = key("dragon_breath");
    public static final ResourceKey<DamageType> DRAGON_CHARGE = key("dragon_charge");
    public static final ResourceKey<DamageType> RABBIT_KING_SLAM = key("rabbit_king_slam");
    public static final ResourceKey<DamageType> TIGER_KING_DISDAIN = key("tiger_king_disdain");

    private ModDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                Identifier.fromNamespaceAndPath(MimiWorld.MODID, name));
    }

    /** 以指定类型构造伤害来源；direct 为直接来源实体，causing 为实际造成者。 */
    public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity direct, Entity causing) {
        return level.damageSources().source(type, direct, causing);
    }
}

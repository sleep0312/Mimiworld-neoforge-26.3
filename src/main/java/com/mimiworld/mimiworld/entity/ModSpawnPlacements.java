package com.mimiworld.mimiworld.entity;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * 实体自然生成规则注册（NeoForge 模组总线）。
 *
 * <p>沿用旧版：陆地敌对生物统一 ON_GROUND + MOTION_BLOCKING_NO_LEAVES +
 * {@link Monster#checkMonsterSpawnRules}。生成分布（生物群系刷怪权重）
 * 属于 worldgen 数据，随第 5 步维度一并补齐。</p>
 */
public final class ModSpawnPlacements {

    private ModSpawnPlacements() {
    }

    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        registerMonster(event, ModEntities.MIMI_SKELETON.get());
        registerMonster(event, ModEntities.CARVED_RICE_SKELETON.get());
        registerMonster(event, ModEntities.HUMI_CORPSE.get());
        registerMonster(event, ModEntities.MIHENG_PIG_CORPSE.get());
        registerMonster(event, ModEntities.PAMI_CRAWLER.get());
        registerMonster(event, ModEntities.STICKY_RICE_SPIDER.get());
        registerMonster(event, ModEntities.RICE_WITCH.get());
        registerMonster(event, ModEntities.MILAI_HORSE.get());
        registerMonster(event, ModEntities.RICE_STEALER.get());
        registerMonster(event, ModEntities.BLIGHTED_THORN_FLOWER.get());
        registerMonster(event, ModEntities.RICE_MIRAGE.get());
        registerMonster(event, ModEntities.LAZY_ELF.get());
        registerMonster(event, ModEntities.RICE_SOUL.get());
        registerMonster(event, ModEntities.EXPLOSIVE_RICE_SPIRIT.get());
        registerMonster(event, ModEntities.RICE_WOLF_VULTURE.get());

        // 化灵米：高空飞行，不限制地面落点
        event.register(ModEntities.HUALING_RICE.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        // 嗥米狼：米米维度陆生友好生物判定（动物可生成方块 + 局部亮度 > 8）
        event.register(ModEntities.HAOMI_WOLF.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON)
                                && level.getMaxLocalRawBrightness(pos, 0) > 8
                                && net.minecraft.world.entity.Mob.checkMobSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        // 陆生友好/中立生物：动物可生成方块 + 局部亮度 > 8
        registerLandAnimal(event, ModEntities.MIMI_SHEEP.get());
        registerLandAnimal(event, ModEntities.ZHUOMI_CHICKEN.get());
        registerLandAnimal(event, ModEntities.MIMOUNIU.get());
        registerLandAnimal(event, ModEntities.MIHENG_PIG.get());
        registerLandAnimal(event, ModEntities.RICE_GOLEM.get());

        // 贪米鱼 / 喷米鱼：水下生成（同原版鳕鱼）
        event.register(ModEntities.TANMI_FISH.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.PENMI_FISH.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends net.minecraft.world.entity.Mob> void registerLandAnimal(
            RegisterSpawnPlacementsEvent event, net.minecraft.world.entity.EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, reason, pos, random) ->
                        level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON)
                                && level.getMaxLocalRawBrightness(pos, 0) > 8
                                && net.minecraft.world.entity.Mob.checkMobSpawnRules(entityType, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends net.minecraft.world.entity.Mob> void registerMonster(
            RegisterSpawnPlacementsEvent event, net.minecraft.world.entity.EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}

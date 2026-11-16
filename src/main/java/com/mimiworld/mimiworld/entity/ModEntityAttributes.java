package com.mimiworld.mimiworld.entity;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * 实体属性注册（触发于模组事件总线）。
 *
 * <p>NeoForge 要求所有 LivingEntity 的属性在 {@link EntityAttributeCreationEvent} 中
 * 按实体类型注册，否则召唤时会因缺少 MAX_HEALTH 等基础属性而崩溃。</p>
 */
public final class ModEntityAttributes {

    private ModEntityAttributes() {
    }

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MIMI_SHEEP.get(), MimiSheepEntity.createAttributes().build());
        event.put(ModEntities.ZHUOMI_CHICKEN.get(), ZhuomiChickenEntity.createAttributes().build());
        event.put(ModEntities.MIMOUNIU.get(), MimouniuEntity.createAttributes().build());
        event.put(ModEntities.MIHENG_PIG.get(), MihengPigEntity.createAttributes().build());
        event.put(ModEntities.TANMI_FISH.get(), TanmiFishEntity.createAttributes().build());
        event.put(ModEntities.PENMI_FISH.get(), PenmiFishEntity.createAttributes().build());
        event.put(ModEntities.RICE_GOLEM.get(), RiceGolemEntity.createAttributes().build());
        event.put(ModEntities.MIMI_SKELETON.get(), MimiSkeletonEntity.createAttributes().build());
        event.put(ModEntities.CARVED_RICE_SKELETON.get(), DiaomikulouEntity.createAttributes().build());
        event.put(ModEntities.HUMI_CORPSE.get(), HumiCorpseEntity.createAttributes().build());
        event.put(ModEntities.MIHENG_PIG_CORPSE.get(), MihengPigCorpseEntity.createAttributes().build());
        event.put(ModEntities.HAOMI_WOLF.get(), HaomiWolfEntity.createAttributes().build());
        event.put(ModEntities.PAMI_CRAWLER.get(), PamiCrawlerEntity.createAttributes().build());
        event.put(ModEntities.STICKY_RICE_SPIDER.get(), StickyRiceSpiderEntity.createAttributes().build());
        event.put(ModEntities.RICE_WITCH.get(), RiceWitchEntity.createAttributes().build());
        event.put(ModEntities.MILAI_HORSE.get(), MilaimaEntity.createAttributes().build());
        event.put(ModEntities.RICE_STEALER.get(), DaomizheEntity.createAttributes().build());
        event.put(ModEntities.BLIGHTED_THORN_FLOWER.get(), WujihuaEntity.createAttributes().build());
        event.put(ModEntities.RICE_MIRAGE.get(), MishenEntity.createAttributes().build());
        event.put(ModEntities.LAZY_ELF.get(), LazyElfEntity.createAttributes().build());
        event.put(ModEntities.RICE_SOUL.get(), RiceSoulEntity.createAttributes().build());
        event.put(ModEntities.HUALING_RICE.get(), HualingmiEntity.createAttributes().build());
        event.put(ModEntities.EXPLOSIVE_RICE_SPIRIT.get(), BaoliemilingEntity.createAttributes().build());
        event.put(ModEntities.RICE_WOLF_VULTURE.get(), RiceWolfVultureEntity.createAttributes().build());
        event.put(ModEntities.MIMI_MONKEY_KING.get(), MimiMonkeyKingEntity.createAttributes().build());
        event.put(ModEntities.RICE_CRYSTAL_TIGER_KING.get(), FanjinghuwangEntity.createAttributes().build());
        event.put(ModEntities.SUPER_CRYSTAL_COW_KING.get(), ChaojingniuwangEntity.createAttributes().build());
        event.put(ModEntities.HEAVY_CRYSTAL_RABBIT_KING.get(), HeavyCrystalRabbitKingEntity.createAttributes().build());
        event.put(ModEntities.COMPOUND_AZURE_PYTHON.get(), CompoundAzurePythonEntity.createAttributes().build());
        event.put(ModEntities.CLEANSING_LONGEVITY_KUI.get(), QingjiechangmingkuiEntity.createAttributes().build());
        event.put(ModEntities.LAZIEST_FINAL_DRAGON.get(), LaziestFinalDragonEntity.createAttributes().build());
        event.put(ModEntities.DIBAZONGZUILONG.get(), DibazongzuilongEntity.createAttributes().build());
        event.put(ModEntities.FALLEN_RICE_PHOENIX.get(), FallenRicePhoenixEntity.createAttributes().build());
        event.put(ModEntities.JIETIAN_RICE.get(), JietianmiEntity.createAttributes().build());
        event.put(ModEntities.CHAOTIC_VOID_TOUCHER.get(), ChaoticVoidToucherEntity.createAttributes().build());
        event.put(ModEntities.MOMI_ENTRANT.get(), MeimiruEntity.createAttributes().build());
        event.put(ModEntities.PALE_MERMAID.get(), QianranjiaoEntity.createAttributes().build());
        event.put(ModEntities.WANGMI_WOUNDED_MERMAID.get(), WangmishangjiaoEntity.createAttributes().build());
    }
}

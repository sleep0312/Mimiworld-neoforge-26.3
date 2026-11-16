package com.mimiworld.mimiworld.entity;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 米米世界实体类型注册。
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MimiWorld.MODID);

    /** 米米箭矢：8 种自定义箭共用的投射物实体。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MimiArrowEntity>> MIMI_ARROW =
            ENTITY_TYPES.register("mimi_arrow", () -> EntityType.Builder
                    .<MimiArrowEntity>of(MimiArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "mimi_arrow"))));

    /** 金箍棒投掷物：米米猴王的掷棍技能载体。 */
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownMonkeyStaffEntity>> THROWN_MONKEY_STAFF =
            ENTITY_TYPES.register("thrown_monkey_staff", () -> EntityType.Builder
                    .<ThrownMonkeyStaffEntity>of(ThrownMonkeyStaffEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "thrown_monkey_staff"))));

    /** 龙系 Boss 光束渲染载体（无碰撞、无伤害，仅同步两端实体 ID）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<DragonBeamEntity>> DRAGON_BEAM =
            ENTITY_TYPES.register("dragon_beam", () -> EntityType.Builder
                    .<DragonBeamEntity>of(DragonBeamEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .noSummon()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "dragon_beam"))));

    /** 米团投掷物：小型可投掷实体，参数参照雪球。 */
    public static final DeferredHolder<EntityType<?>, EntityType<RiceBallProjectile>> RICE_BALL =
            ENTITY_TYPES.register("rice_ball", () -> EntityType.Builder
                    .<RiceBallProjectile>of(RiceBallProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "rice_ball"))));

    /** 熟米团投掷物：小型可投掷实体，参数参照雪球。 */
    public static final DeferredHolder<EntityType<?>, EntityType<CookedRiceBallProjectile>> COOKED_RICE_BALL =
            ENTITY_TYPES.register("cooked_rice_ball", () -> EntityType.Builder
                    .<CookedRiceBallProjectile>of(CookedRiceBallProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "cooked_rice_ball"))));

    /** 虚无触毒液：小型抛物线投掷物，命中造成伤害 + 中毒 II。 */
    public static final DeferredHolder<EntityType<?>, EntityType<VoidPoisonSpitEntity>> VOID_POISON_SPIT =
            ENTITY_TYPES.register("void_poison_spit", () -> EntityType.Builder
                    .<VoidPoisonSpitEntity>of(VoidPoisonSpitEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "void_poison_spit"))));

    // ============================ 友好生物（第 4 步 · 第一小批） ============================

    /** 米羊羊。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MimiSheepEntity>> MIMI_SHEEP =
            mob("mimi_sheep", MimiSheepEntity::new, MobCategory.CREATURE, 0.9F, 1.3F);

    /** 啄米鸡。 */
    public static final DeferredHolder<EntityType<?>, EntityType<ZhuomiChickenEntity>> ZHUOMI_CHICKEN =
            mob("zhuomi_chicken", ZhuomiChickenEntity::new, MobCategory.CREATURE, 0.6F, 0.7F);

    /** 米哞牛。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MimouniuEntity>> MIMOUNIU =
            mob("mimoo_cow", MimouniuEntity::new, MobCategory.CREATURE, 0.9F, 1.4F);

    /** 米哼猪。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MihengPigEntity>> MIHENG_PIG =
            mob("miheng_pig", MihengPigEntity::new, MobCategory.CREATURE, 0.9F, 1.3F);

    /** 贪米鱼（水生）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<TanmiFishEntity>> TANMI_FISH =
            ENTITY_TYPES.register("tanmi_fish", () -> EntityType.Builder
                    .<TanmiFishEntity>of(TanmiFishEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.5F, 0.4F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(entityKey("tanmi_fish")));

    /** 喷米鱼（水生）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<PenmiFishEntity>> PENMI_FISH =
            ENTITY_TYPES.register("penmi_fish", () -> EntityType.Builder
                    .<PenmiFishEntity>of(PenmiFishEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.5F, 0.4F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(entityKey("penmi_fish")));

    /** 米傀儡。 */
    public static final DeferredHolder<EntityType<?>, EntityType<RiceGolemEntity>> RICE_GOLEM =
            mob("rice_golem", RiceGolemEntity::new, MobCategory.CREATURE, 1.4F, 2.7F);

    // ============================ 近战亡灵（第 4 步 · 第二小批） ============================

    /** 米米骷髅：0.6×1.99，24 心，近战 4。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MimiSkeletonEntity>> MIMI_SKELETON =
            mob("mimi_skeleton", MimiSkeletonEntity::new, MobCategory.MONSTER, 0.6F, 1.99F);

    /** 雕米骷髅：0.6×2.2，60 心，近战 5。 */
    public static final DeferredHolder<EntityType<?>, EntityType<DiaomikulouEntity>> CARVED_RICE_SKELETON =
            mob("carved_rice_skeleton", DiaomikulouEntity::new, MobCategory.MONSTER, 0.6F, 2.2F);

    /** 糊米古尸：0.7×1.9，30 心，近战 3。 */
    public static final DeferredHolder<EntityType<?>, EntityType<HumiCorpseEntity>> HUMI_CORPSE =
            mob("humi_corpse", HumiCorpseEntity::new, MobCategory.MONSTER, 0.7F, 1.9F);

    /** 米哼猪尸：0.9×1.6，30 心，近战 5。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MihengPigCorpseEntity>> MIHENG_PIG_CORPSE =
            mob("miheng_pig_corpse", MihengPigCorpseEntity::new, MobCategory.MONSTER, 0.9F, 1.6F);

    // ============================ 特殊行为敌对（第 4 步 · 第三小批） ============================

    /** 嗥米狼：友好生物 0.6×1.7，20 心。 */
    public static final DeferredHolder<EntityType<?>, EntityType<HaomiWolfEntity>> HAOMI_WOLF =
            mob("haomi_wolf", HaomiWolfEntity::new, MobCategory.CREATURE, 0.6F, 1.7F);

    /** 爪巴米者：苦力怕式自爆怪 0.7×0.8，12 心。 */
    public static final DeferredHolder<EntityType<?>, EntityType<PamiCrawlerEntity>> PAMI_CRAWLER =
            mob("pami_crawler", PamiCrawlerEntity::new, MobCategory.MONSTER, 0.7F, 0.8F);

    /** 黏米之蛛：1.4×0.9，24 心，可爬墙。 */
    public static final DeferredHolder<EntityType<?>, EntityType<StickyRiceSpiderEntity>> STICKY_RICE_SPIDER =
            mob("sticky_rice_spider", StickyRiceSpiderEntity::new, MobCategory.MONSTER, 1.4F, 0.9F);

    /** 米巫：0.6×1.95，30 心，远程投药水 + 近战。 */
    public static final DeferredHolder<EntityType<?>, EntityType<RiceWitchEntity>> RICE_WITCH =
            mob("rice_witch", RiceWitchEntity::new, MobCategory.MONSTER, 0.6F, 1.95F);

    /** 米莱马：史莱姆型 1.02×1.02，12 心，接触伤害 4。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MilaimaEntity>> MILAI_HORSE =
            mob("milai_horse", MilaimaEntity::new, MobCategory.MONSTER, 1.02F, 1.02F);

    // ============================ 地面精英（第 4 步 · 第四小批） ============================

    /** 盗米者：0.8×2.1，20 心，偷快捷栏食物后逃走。 */
    public static final DeferredHolder<EntityType<?>, EntityType<DaomizheEntity>> RICE_STEALER =
            mob("rice_stealer", DaomizheEntity::new, MobCategory.MONSTER, 0.8F, 2.1F);

    /** 污棘花：0.9×1.8，500 心，扎根不动的食肉植物。 */
    public static final DeferredHolder<EntityType<?>, EntityType<WujihuaEntity>> BLIGHTED_THORN_FLOWER =
            mob("blighted_thorn_flower", WujihuaEntity::new, MobCategory.MONSTER, 0.9F, 1.8F);

    /** 米蜃：1.0×0.8，750 心，贴地疾行啃咬。 */
    public static final DeferredHolder<EntityType<?>, EntityType<MishenEntity>> RICE_MIRAGE =
            mob("rice_mirage", MishenEntity::new, MobCategory.MONSTER, 1.0F, 0.8F);

    /** 懒精灵：0.6×1.2，400 心，近战 100。 */
    public static final DeferredHolder<EntityType<?>, EntityType<LazyElfEntity>> LAZY_ELF =
            mob("lazy_elf", LazyElfEntity::new, MobCategory.MONSTER, 0.6F, 1.2F);

    // ============================ 飞行精英（第 4 步 · 第五小批） ============================

    /** 米魂：悬浮飞行的近战魂体 1.1×1.6，9 心。 */
    public static final DeferredHolder<EntityType<?>, EntityType<RiceSoulEntity>> RICE_SOUL =
            mob("rice_soul", RiceSoulEntity::new, MobCategory.MONSTER, 1.1F, 1.6F);

    /** 化灵米：不穿墙的高空飞行精英 1.0×2.0，5000 心。 */
    public static final DeferredHolder<EntityType<?>, EntityType<HualingmiEntity>> HUALING_RICE =
            mob("hualing_rice", HualingmiEntity::new, MobCategory.MONSTER, 1.0F, 2.0F);

    /** 爆烈米灵：烈焰人式飞行怪 0.8×1.8，60 心，火焰免疫。 */
    public static final DeferredHolder<EntityType<?>, EntityType<BaoliemilingEntity>> EXPLOSIVE_RICE_SPIRIT =
            ENTITY_TYPES.register("explosive_rice_spirit", () -> EntityType.Builder
                    .of(BaoliemilingEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.8F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .fireImmune()
                    .build(entityKey("explosive_rice_spirit")));

    /** 哮噬米狼鹫：陆空敌对 1.2×1.5，500 心。 */
    public static final DeferredHolder<EntityType<?>, EntityType<RiceWolfVultureEntity>> RICE_WOLF_VULTURE =
            mob("rice_wolf_vulture", RiceWolfVultureEntity::new, MobCategory.MONSTER, 1.2F, 1.5F);

    /** 通用陆地生物注册：CREATURE 类别，客户端追踪 8 格、每 3 tick 同步一次。 */
    private static <T extends net.minecraft.world.entity.Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(
            String name, EntityType.EntityFactory<T> factory, MobCategory category, float width, float height) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(factory, category)
                .sized(width, height)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(entityKey(name)));
    }

    // ============================ Boss 与残堕界精英 ============================

    private static <T extends net.minecraft.world.entity.Mob> DeferredHolder<EntityType<?>, EntityType<T>> boss(
            String name, EntityType.EntityFactory<T> factory, float width, float height, int trackingRange) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .of(factory, MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(trackingRange)
                .fireImmune()
                .build(entityKey(name)));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<MimiMonkeyKingEntity>> MIMI_MONKEY_KING =
            boss("mimi_monkey_king", MimiMonkeyKingEntity::new, 1.0F, 2.3F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<FanjinghuwangEntity>> RICE_CRYSTAL_TIGER_KING =
            boss("rice_crystal_tiger_king", FanjinghuwangEntity::new, 1.35F, 3.0F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<ChaojingniuwangEntity>> SUPER_CRYSTAL_COW_KING =
            boss("super_crystal_cow_king", ChaojingniuwangEntity::new, 1.6F, 2.6F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<HeavyCrystalRabbitKingEntity>> HEAVY_CRYSTAL_RABBIT_KING =
            boss("heavy_crystal_rabbit_king", HeavyCrystalRabbitKingEntity::new, 1.5F, 3.2F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<CompoundAzurePythonEntity>> COMPOUND_AZURE_PYTHON =
            boss("compound_azure_python", CompoundAzurePythonEntity::new, 2.6F, 3.5F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<QingjiechangmingkuiEntity>> CLEANSING_LONGEVITY_KUI =
            boss("cleansing_longevity_kui", QingjiechangmingkuiEntity::new, 2.6F, 3.6F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<LaziestFinalDragonEntity>> LAZIEST_FINAL_DRAGON =
            boss("laziest_final_dragon", LaziestFinalDragonEntity::new, 3.0F, 5.5F, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<DibazongzuilongEntity>> DIBAZONGZUILONG =
            boss("dibazongzuilong", DibazongzuilongEntity::new, 6.0F, 8.5F, 16);

    public static final DeferredHolder<EntityType<?>, EntityType<FallenRicePhoenixEntity>> FALLEN_RICE_PHOENIX =
            boss("fallen_rice_phoenix", FallenRicePhoenixEntity::new, 1.4F, 2.2F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<JietianmiEntity>> JIETIAN_RICE =
            boss("jietian_rice", JietianmiEntity::new, 1.3F, 1.9F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<ChaoticVoidToucherEntity>> CHAOTIC_VOID_TOUCHER =
            boss("chaotic_void_toucher", ChaoticVoidToucherEntity::new, 2.2F, 1.6F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<MeimiruEntity>> MOMI_ENTRANT =
            boss("momi_entrant", MeimiruEntity::new, 0.6F, 2.5F, 8);
    public static final DeferredHolder<EntityType<?>, EntityType<QianranjiaoEntity>> PALE_MERMAID =
            boss("pale_mermaid", QianranjiaoEntity::new, 2.6F, 2.0F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<WangmishangjiaoEntity>> WANGMI_WOUNDED_MERMAID =
            boss("wangmi_wounded_mermaid", WangmishangjiaoEntity::new, 3.0F, 4.2F, 12);

    private static ResourceKey<EntityType<?>> entityKey(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MimiWorld.MODID, name));
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}

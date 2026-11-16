package com.mimiworld.mimiworld;

import com.mimiworld.mimiworld.block.ModBlocks;
import com.mimiworld.mimiworld.effect.ModEffects;
import com.mimiworld.mimiworld.entity.ModEntities;
import com.mimiworld.mimiworld.item.AncientPeltItem;
import com.mimiworld.mimiworld.item.BookOfDepartureItem;
import com.mimiworld.mimiworld.item.CookedRiceBallItem;
import com.mimiworld.mimiworld.item.EffectFoodItem;
import com.mimiworld.mimiworld.item.FieryGoldenEyesItem;
import com.mimiworld.mimiworld.item.MimiArmorItem;
import com.mimiworld.mimiworld.item.MimiArrowItem;
import com.mimiworld.mimiworld.item.MimiBowItem;
import com.mimiworld.mimiworld.item.MimiTotemItem;
import com.mimiworld.mimiworld.item.MonkeyKingRuyiStaffItem;
import com.mimiworld.mimiworld.item.RainbowNameItem;
import com.mimiworld.mimiworld.item.RainbowNameSwordItem;
import com.mimiworld.mimiworld.item.ReverseScaleGallItem;
import com.mimiworld.mimiworld.item.RiceBallItem;
import com.mimiworld.mimiworld.item.RiceGrassItem;
import com.mimiworld.mimiworld.item.SacredRelicItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 米米世界物品注册类。
 *
 * <p>本次移植范围：不依赖方块、实体、流体、GeckoLib、Curios、附魔等系统的纯材料物品。
 * 工具、防具、食物、方块物品、生物蛋等会在后续批次逐步补入。</p>
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MimiWorld.MODID);

    /** 注册一个最大堆叠 64 的普通材料物品。 */
    private static DeferredItem<Item> material(String name) {
        return ITEMS.registerSimpleItem(name, properties -> properties.stacksTo(64));
    }

    // ============================ 食物注册辅助 ============================

    /** 熟食食物属性：仅饱食度/饱和度，无负面效果。 */
    private static FoodProperties cookedFood(int nutrition, float saturation) {
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation)
                .build();
    }

    /** 生食基础属性：仅饱食度/饱和度，负面效果由 Consumable 声明。 */
    private static FoodProperties rawFood(int nutrition, float saturation) {
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation)
                .build();
    }

    // 食用效果持续时间常量（tick）
    private static final int SEC_10 = 200;
    private static final int SEC_30 = 600;
    private static final int MIN_1 = 1200;
    private static final int MIN_2 = 2400;
    private static final int MIN_3 = 3600;
    private static final int MIN_5 = 6000;
    private static final int MIN_15 = 18000;
    private static final int MIN_20 = 24000;
    private static final int MIN_30 = 36000;
    private static final int HOUR_1 = 72000;

    /** 带食用效果的食物属性：只设饱食度，饱和度取与饱食度同值。 */
    private static FoodProperties effectFoodProps(int hunger) {
        return new FoodProperties.Builder().nutrition(hunger).saturationModifier(0.5F).build();
    }

    /** 声明一条食用效果：等级 N → 放大器 N-1（一级 = 0），durationTicks 为持续时间（tick）。 */
    private static EffectFoodItem.AppliedEffect effect(Holder<MobEffect> effect, int level, int durationTicks) {
        return new EffectFoodItem.AppliedEffect(effect, level - 1, durationTicks);
    }

    /** 声明一条按概率触发的原版食用效果。 */
    private static ApplyStatusEffectsConsumeEffect statusEffect(Holder<MobEffect> effect,
                                                                 int durationTicks, int amplifier,
                                                                 float probability) {
        return new ApplyStatusEffectsConsumeEffect(
                new MobEffectInstance(effect, durationTicks, amplifier), probability);
    }

    /** 构建食用组件：食用耗时（秒）+ 若干食用效果。 */
    private static Consumable consumable(float seconds, ConsumeEffect... effects) {
        Consumable.Builder builder = Consumable.builder()
                .consumeSeconds(seconds)
                .animation(ItemUseAnimation.EAT)
                .sound(SoundEvents.GENERIC_EAT)
                .hasConsumeParticles(true);
        for (ConsumeEffect effect : effects) {
            builder.onConsume(effect);
        }
        return builder.build();
    }

    /** 生食食用组件：按概率附加「饥饿 I 30 秒」（600 tick），使用默认熟食耗时 1.6 秒。 */
    private static Consumable rawConsumable(float hungerChance) {
        Consumable.Builder builder = Consumable.builder()
                .consumeSeconds(1.6F)
                .animation(ItemUseAnimation.EAT)
                .sound(SoundEvents.GENERIC_EAT)
                .hasConsumeParticles(true);
        if (hungerChance > 0.0F) {
            builder.onConsume(statusEffect(MobEffects.HUNGER, 600, 0, hungerChance));
        }
        return builder.build();
    }

    /** 注册普通食物（默认 1.6 秒食用时长）。 */
    private static DeferredItem<Item> registerFood(String name, FoodProperties food) {
        return ITEMS.registerSimpleItem(name, properties -> properties.stacksTo(64).food(food));
    }

    /** 注册带自定义食用组件的食物。 */
    private static DeferredItem<Item> registerFood(String name, FoodProperties food, Consumable consumable) {
        return ITEMS.registerSimpleItem(name, properties -> properties.stacksTo(64).food(food, consumable));
    }

    /** 注册「饱食度 + 若干食用效果」的食物。 */
    private static DeferredItem<Item> registerEffectFood(String name, int hunger,
                                                          EffectFoodItem.AppliedEffect... effects) {
        return ITEMS.registerItem(name, properties -> new EffectFoodItem(
                properties.stacksTo(64).food(effectFoodProps(hunger)), List.of(effects)));
    }

    /** 注册剑类武器。 */
    private static DeferredItem<Item> sword(String name, ToolMaterial material,
                                             float attackDamage, float attackSpeed) {
        return ITEMS.registerSimpleItem(name,
                properties -> properties.stacksTo(1).sword(material, attackDamage, attackSpeed));
    }

    /** 注册镐类工具。 */
    private static DeferredItem<Item> pickaxe(String name, ToolMaterial material,
                                               float attackDamage, float attackSpeed) {
        return ITEMS.registerSimpleItem(name,
                properties -> properties.stacksTo(1).pickaxe(material, attackDamage, attackSpeed));
    }

    /** 注册 GeckoLib 3D 弓。 */
    private static DeferredItem<Item> bow(String name, double damageMultiplier, String modelId,
                                          String texturePath, int durability) {
        return ITEMS.registerItem(name, properties -> new MimiBowItem(
                damageMultiplier, modelId, texturePath, properties.durability(durability)));
    }

    /** 注册自定义箭矢。 */
    private static DeferredItem<Item> arrow(String name, double hitDamage, int poisonLevel) {
        return ITEMS.registerItem(name,
                properties -> new MimiArrowItem(hitDamage, poisonLevel, properties.stacksTo(64)));
    }

    /** 由物品 ID 推导所属 3D 防具模型。 */
    private static String armorModelKey(String name) {
        if (name.startsWith("laziest_heavenfall_") || name.equals("heavenfall_wings_chestplate")) {
            return "laziest_heavenfall_armor";
        }
        if (name.equals("mimi_divine_wings")) {
            return "mimi_divine_wings";
        }
        if (name.equals("super_lazy_wings_chestplate")) {
            return "super_lazy_wings_chestplate";
        }
        if (name.startsWith("remnant_fallen_cardinal_sin_")) {
            return "remnant_fallen_cardinal_sin_armor";
        }
        return name.replaceAll("_(helmet|chestplate|leggings|boots)$", "") + "_armor";
    }

    /** 注册防具单件（附加最大生命值与穿戴效果数据）。 */
    private static DeferredItem<Item> armor(String name, ArmorMaterial material, ArmorType type,
                                             float maxHealthBonus, Map<Holder<MobEffect>, Integer> wornEffects,
                                             boolean grantsFlight) {
        return ITEMS.registerItem(name, properties -> {
            ItemAttributeModifiers attributes = material.createAttributes(type);
            if (maxHealthBonus > 0.0F) {
                attributes = attributes.withModifierAdded(
                        Attributes.MAX_HEALTH,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath(MimiWorld.MODID, name + "_max_health"),
                                maxHealthBonus, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.bySlot(type.getSlot()));
            }
            return new MimiArmorItem(
                    properties.stacksTo(1)
                            .humanoidArmor(material, type)
                            .component(DataComponents.ATTRIBUTE_MODIFIERS, attributes),
                    maxHealthBonus, wornEffects, grantsFlight, armorModelKey(name));
        });
    }

    // ============================ 米与米制材料 ============================

    /** 米木棍 */
    public static final DeferredItem<Item> RICE_STICK = ITEMS.registerItem("rice_stick",
            properties -> new Item(properties.stacksTo(64)
                    .component(net.minecraft.core.component.DataComponents.COOKING_FUEL, ModFuels.STICK)));
    /** 结晶米 */
    public static final DeferredItem<Item> CRYSTALLIZED_RICE = material("crystallized_rice");
    /** 重结晶米 */
    public static final DeferredItem<Item> RECRYSTALLIZED_RICE = material("recrystallized_rice");
    /** 可燃米 */
    public static final DeferredItem<Item> COMBUSTIBLE_RICE = ITEMS.registerItem("combustible_rice",
            properties -> new Item(properties.stacksTo(64)
                    .component(net.minecraft.core.component.DataComponents.COOKING_FUEL, ModFuels.COMBUSTIBLE_RICE)));
    /** 强化米棍 */
    public static final DeferredItem<Item> REINFORCED_RICE_STICK = material("reinforced_rice_stick");
    /** 饭晶 */
    public static final DeferredItem<Item> CRYSTALLIZED_RICE_MEAL = material("crystallized_rice_meal");
    /** 饭晶合金锭 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_INGOT = material("rice_crystal_alloy_ingot");
    /** 米麻 */
    public static final DeferredItem<Item> REFINED_RICE_FIBER = material("refined_rice_fiber");
    /** 米网 */
    public static final DeferredItem<Item> REFINED_REINFORCED_RICE_NET = material("refined_reinforced_rice_net");
    /** 超懒精华 */
    public static final DeferredItem<Item> SUPER_LAZY_ESSENCE = material("super_lazy_essence");
    /** 米石 */
    public static final DeferredItem<Item> RICE_STONE = material("rice_stone");
    /** 超重结晶 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL = material("super_heavy_crystal");
    /** 复合米饭锭 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_INGOT = material("compound_rice_meal_ingot");
    /** 米果汁 */
    public static final DeferredItem<Item> RICE_FRUIT_JUICE = material("rice_fruit_juice");
    /** 米书 */
    public static final DeferredItem<Item> RICE_BOOK = material("rice_book");
    /** 米纸 */
    public static final DeferredItem<Item> RICE_PAPER = material("rice_paper");
    /** 米饭混合液 */
    public static final DeferredItem<Item> RICE_MEAL_MIXTURE = material("rice_meal_mixture");
    /** 米果清洁液 */
    public static final DeferredItem<Item> RICE_FRUIT_CLEANSING_SOLUTION = material("rice_fruit_cleansing_solution");
    /** 清洁复合锭 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_INGOT = material("cleansing_compound_ingot");
    /** 粗制超懒合金锭 */
    public static final DeferredItem<Item> COARSE_SUPER_LAZY_ALLOY_INGOT = material("coarse_super_lazy_alloy_ingot");
    /** 超懒合金锭 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_INGOT = material("super_lazy_alloy_ingot");
    /** 天堕锭 */
    public static final DeferredItem<Item> HEAVENFALL_INGOT = material("heavenfall_ingot");
    /** 可燃混合液 */
    public static final DeferredItem<Item> COMBUSTIBLE_MIXTURE = material("combustible_mixture");
    /** 可燃固态物 */
    public static final DeferredItem<Item> COMBUSTIBLE_SOLID = material("combustible_solid");
    /** 清洁米沉离液 */
    public static final DeferredItem<Item> CLEANSING_RICE_SEPARATION_SOLUTION = material("cleansing_rice_separation_solution");

    // ============================ 召唤器与核心 ============================

    /** 小型召唤器 */
    public static final DeferredItem<Item> SMALL_SUMMONER = material("small_summoner");
    /** 中型召唤器 */
    public static final DeferredItem<Item> MEDIUM_SUMMONER = material("medium_summoner");
    /** 大型召唤器 */
    public static final DeferredItem<Item> LARGE_SUMMONER = material("large_summoner");
    /** 最懒召唤器 */
    public static final DeferredItem<Item> LAZIEST_SUMMONER = material("laziest_summoner");
    /** 召唤核心 */
    public static final DeferredItem<Item> SUMMON_CORE = material("summon_core");
    /** 高级召唤核心 */
    public static final DeferredItem<Item> ADVANCED_SUMMON_CORE = material("advanced_summon_core");
    /** 混合结晶 */
    public static final DeferredItem<Item> MIXED_CRYSTAL = material("mixed_crystal");
    /** 熔融混合结晶 */
    public static final DeferredItem<Item> MOLTEN_MIXED_CRYSTAL = material("molten_mixed_crystal");
    /** 最懒召唤核心 */
    public static final DeferredItem<Item> LAZIEST_SUMMON_CORE = material("laziest_summon_core");
    /** 米米幻翼膜 */
    public static final DeferredItem<Item> MIMI_PHANTOM_MEMBRANE = material("mimi_phantom_membrane");

    // ============================ 神剑材料链 ============================

    /** 古剑意 */
    public static final DeferredItem<Item> ANCIENT_SWORD_INTENT = material("ancient_sword_intent");
    /** 血瓶 */
    public static final DeferredItem<Item> BLOOD_VIAL = material("blood_vial");
    /** 淬剑液 */
    public static final DeferredItem<Item> SWORD_QUENCHING_LIQUID = material("sword_quenching_liquid");
    /** 古剑魂 */
    public static final DeferredItem<Item> ANCIENT_SWORD_SOUL = material("ancient_sword_soul");
    /** 原初剑魂 */
    public static final DeferredItem<Item> PRIMORDIAL_SWORD_SOUL = material("primordial_sword_soul");
    /** 神玉 */
    public static final DeferredItem<Item> DIVINE_JADE = material("divine_jade");
    /** 金米玉 */
    public static final DeferredItem<Item> GOLDEN_RICE_JADE = material("golden_rice_jade");
    /** 神剑鉴定玉 */
    public static final DeferredItem<Item> DIVINE_SWORD_APPRAISAL_JADE = material("divine_sword_appraisal_jade");
    /** 禁剑谱 */
    public static final DeferredItem<Item> FORBIDDEN_SWORD_MANUAL = material("forbidden_sword_manual");
    /** 结晶米玉 */
    public static final DeferredItem<Item> CRYSTALLIZED_RICE_JADE = material("crystallized_rice_jade");
    /** 破败之心 */
    public static final DeferredItem<Item> RUINED_HEART = material("ruined_heart");
    /** 堕落之心 */
    public static final DeferredItem<Item> FALLEN_HEART = material("fallen_heart");
    /** 堕落王冠 */
    public static final DeferredItem<Item> FALLEN_CROWN = material("fallen_crown");
    /** 七堕 */
    public static final DeferredItem<Item> SEVEN_DEADLY_FALLS = material("seven_deadly_falls");

    // ============================ 生物掉落材料 ============================

    /** 啄米鸡羽毛 */
    public static final DeferredItem<Item> ZHUOMI_FEATHER = material("zhuomi_feather");
    /** 米哞牛皮 */
    public static final DeferredItem<Item> MIMOO_LEATHER = material("mimoo_leather");
    /** 米衡猪皮 */
    public static final DeferredItem<Item> MIHENG_HIDE = material("miheng_hide");
    /** 喷米鱼鳔 */
    public static final DeferredItem<Item> PENMI_FISH_SAC = material("penmi_fish_sac");
    /** 嗥米狼心 */
    public static final DeferredItem<Item> HAOMI_WOLF_HEART = material("haomi_wolf_heart");
    /** 米骨 */
    public static final DeferredItem<Item> RICE_BONE = material("rice_bone");
    /** 惑米粉 */
    public static final DeferredItem<Item> HUOMI_POWDER = material("huomi_powder");
    /** 厌米珠 */
    public static final DeferredItem<Item> YANMI_PEARL = material("yanmi_pearl");
    /** 爆裂米杆 */
    public static final DeferredItem<Item> BAOLIEMI_ROD = material("baoliemi_rod");
    /** 雕纹米骨 */
    public static final DeferredItem<Item> CARVED_RICE_BONE = material("carved_rice_bone");
    /** 米莱马粘球 */
    public static final DeferredItem<Item> MILAIMA_BALL = material("milaima_ball");
    /** 念米丝线 */
    public static final DeferredItem<Item> NIANMI_SILK_THREAD = material("nianmi_silk_thread");
    /** 米米乱炖 */
    public static final DeferredItem<Item> MIMI_MEDLEY = material("mimi_medley");
    /** 骨粉米 */
    public static final DeferredItem<Item> RICE_BONE_MEAL = material("rice_bone_meal");
    /** 米精 */
    public static final DeferredItem<Item> RICE_ESSENCE = material("rice_essence");
    /** 粗米 */
    public static final DeferredItem<Item> COARSE_RICE = material("coarse_rice");
    /** 米毛笔 */
    public static final DeferredItem<Item> RICE_BRUSH_PEN = material("rice_brush_pen");
    /** 米笔书 */
    public static final DeferredItem<Item> BOOK_RICE_PEN = material("book_rice_pen");
    /** 米果黑混合液 */
    public static final DeferredItem<Item> RICE_FRUIT_BLACK_MIXTURE = material("rice_fruit_black_mixture");
    /** 米果粘稠混合液 */
    public static final DeferredItem<Item> RICE_FRUIT_VISCOUS_MIXTURE = material("rice_fruit_viscous_mixture");
    /** 爆米花露 */
    public static final DeferredItem<Item> POPRICE_BLOOM_LIQUID = material("poprice_bloom_liquid");
    /** 厌米心露 */
    public static final DeferredItem<Item> YANMI_HEART_SIP_LIQUID = material("yanmi_heart_sip_liquid");
    /** 米米乱炖液 */
    public static final DeferredItem<Item> MIMI_MEDLEY_LIQUID = material("mimi_medley_liquid");

    // ============================ Boss 材料 ============================

    /** 猴王毫毛 */
    public static final DeferredItem<Item> MONKEY_KING_HAIR = material("monkey_king_hair");
    /** 虎王剃爪 */
    public static final DeferredItem<Item> TIGER_KING_RAZOR_CLAW = material("tiger_king_razor_claw");
    /** 牛王晶角 */
    public static final DeferredItem<Item> OX_KING_CRYSTAL_HORN = material("ox_king_crystal_horn");
    /** 兔王血红瞳 */
    public static final DeferredItem<Item> RABBIT_KING_BLOODY_RED_EYE = material("rabbit_king_bloody_red_eye");
    /** 毒蟒獠牙 */
    public static final DeferredItem<Item> VENOM_AZURE_PYTHON_FANG = material("venom_azure_python_fang");
    /** 邪夔长寿角 */
    public static final DeferredItem<Item> EVIL_KUI_LONGEVITY_HORN = material("evil_kui_longevity_horn");
    /** 终末龙逆鳞 */
    public static final DeferredItem<Item> END_DRAGON_REVERSE_SCALE = material("end_dragon_reverse_scale");
    /** 爆米心露 */
    public static final DeferredItem<Item> POPRICE_HEART_SIP_LIQUID = material("poprice_heart_sip_liquid");
    /** 超重结晶镐头 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_PICKAXE_HEAD = material("super_heavy_crystal_pickaxe_head");
    /** 复合米饭镐头 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_PICKAXE_HEAD = material("compound_rice_meal_pickaxe_head");
    /** 清洁复合镐头 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_PICKAXE_HEAD = material("cleansing_compound_pickaxe_head");
    /** 超懒合金镐头 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_PICKAXE_HEAD = material("super_lazy_alloy_pickaxe_head");
    /** 超重结晶杆 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_ROD = material("super_heavy_crystal_rod");
    /** 复合米饭杆 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_ROD = material("compound_rice_meal_rod");
    /** 清洁复合杆 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_ROD = material("cleansing_compound_rod");
    /** 超懒合金杆 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_ROD = material("super_lazy_alloy_rod");
    /** 窃米者之心 */
    public static final DeferredItem<Item> RICE_STEALER_HEART = material("rice_stealer_heart");
    /** 米魂之泪 */
    public static final DeferredItem<Item> RICE_SOUL_TEAR = material("rice_soul_tear");

    // ============================ 残堕界材料 ============================

    /** 残天之翼 */
    public static final DeferredItem<Item> REMNANT_HEAVEN_WING = material("remnant_heaven_wing");
    /** 虚空之触 */
    public static final DeferredItem<Item> VOID_TOUCH = material("void_touch");
    /** 凋花枝 */
    public static final DeferredItem<Item> FADED_FLOWER_BRANCH = material("faded_flower_branch");
    /** 界天凤喙 */
    public static final DeferredItem<Item> CELESTIAL_PHOENIX_BEAK = material("celestial_phoenix_beak");
    /** 残堕米精 */
    public static final DeferredItem<Item> REMNANT_FALLEN_RICE_ESSENCE = material("remnant_fallen_rice_essence");
    /** 残堕锭 */
    public static final DeferredItem<Item> REMNANT_FALLEN_INGOT = material("remnant_fallen_ingot");
    /** 鲛人泪 */
    public static final DeferredItem<Item> MERMAID_TEAR = material("mermaid_tear");
    /** 蛟龙爪 */
    public static final DeferredItem<Item> JIAOLONG_TALON = material("jiaolong_talon");
    /** 蜃骨 */
    public static final DeferredItem<Item> MIRAGE_BONE = material("mirage_bone");
    /** 精灵帽 */
    public static final DeferredItem<Item> ELF_HAT = material("elf_hat");
    /** 噬月狼爪 */
    public static final DeferredItem<Item> MOON_DEVOURER_WOLF_CLAW = material("moon_devourer_wolf_claw");
    /** 染液 */
    public static final DeferredItem<Item> DYEING_SOLUTION = material("dyeing_solution");
    /** 深染虚空之触 */
    public static final DeferredItem<Item> DEEP_DYED_VOID_TOUCH = material("deep_dyed_void_touch");
    /** 深染凋花枝 */
    public static final DeferredItem<Item> DEEP_DYED_FADED_FLOWER_BRANCH = material("deep_dyed_faded_flower_branch");
    /** 深染界天凤喙 */
    public static final DeferredItem<Item> DEEP_DYED_CELESTIAL_PHOENIX_BEAK = material("deep_dyed_celestial_phoenix_beak");
    /** 深染残堕米精 */
    public static final DeferredItem<Item> DEEP_DYED_REMNANT_FALLEN_RICE_ESSENCE = material("deep_dyed_remnant_fallen_rice_essence");
    /** 深染鲛人泪 */
    public static final DeferredItem<Item> DEEP_DYED_MERMAID_TEAR = material("deep_dyed_mermaid_tear");
    /** 深染蛟龙爪 */
    public static final DeferredItem<Item> DEEP_DYED_JIAOLONG_TALON = material("deep_dyed_jiaolong_talon");
    /** 深染蜃骨 */
    public static final DeferredItem<Item> DEEP_DYED_MIRAGE_BONE = material("deep_dyed_mirage_bone");
    /** 深染精灵帽 */
    public static final DeferredItem<Item> DEEP_DYED_ELF_HAT = material("deep_dyed_elf_hat");
    /** 深染噬月狼爪 */
    public static final DeferredItem<Item> DEEP_DYED_MOON_DEVOURER_WOLF_CLAW = material("deep_dyed_moon_devourer_wolf_claw");
    /** 磨制残堕渊岩精 */
    public static final DeferredItem<Item> POLISHED_REMNANT_FALLEN_WORLD_ESSENCE = material("polished_remnant_fallen_world_essence");
    /** 磨制残堕精华液 */
    public static final DeferredItem<Item> POLISHED_REMNANT_FALLEN_ESSENCE_SOLUTION = material("polished_remnant_fallen_essence_solution");
    /** 残堕界精华锭 */
    public static final DeferredItem<Item> REMNANT_FALLEN_WORLD_ESSENCE_INGOT = material("remnant_fallen_world_essence_ingot");
    /** 世界之泪 */
    public static final DeferredItem<Item> WORLD_TEAR = material("world_tear");
    /** 界天脑花 */
    public static final DeferredItem<Item> REALM_HEAVEN_BRAIN_FLOWER = material("realm_heaven_brain_flower");

    // ============================ 厨具与调味料 ============================

    /** 香囊 */
    public static final DeferredItem<Item> FRAGRANT_POUCH = material("fragrant_pouch");
    /** 香粉 */
    public static final DeferredItem<Item> FRAGRANT_POWDER = material("fragrant_powder");
    /** 大碗 */
    public static final DeferredItem<Item> BIG_BOWL = material("big_bowl");
    /** 盘子 */
    public static final DeferredItem<Item> PLATE = material("plate");
    /** 米蜃油脂 */
    public static final DeferredItem<Item> RICE_MIRAGE_GREASE = material("rice_mirage_grease");
    /** 染鲛油脂 */
    public static final DeferredItem<Item> DYED_MERMAID_GREASE = material("dyed_mermaid_grease");
    /** 油瓶 */
    public static final DeferredItem<Item> OIL_BOTTLE = material("oil_bottle");
    /** 米纸盘 */
    public static final DeferredItem<Item> RICE_PAPER_PLATE = material("rice_paper_plate");
    /** 腌料 */
    public static final DeferredItem<Item> MARINADE = material("marinade");
    /** 珍贵调料 */
    public static final DeferredItem<Item> PRECIOUS_SEASONING = material("precious_seasoning");
    /** 烧烤粉 */
    public static final DeferredItem<Item> BBQ_POWDER = material("bbq_powder");
    /** 花香油 */
    public static final DeferredItem<Item> FLORAL_FRAGRANCE_OIL = material("floral_fragrance_oil");
    /** 复合油 */
    public static final DeferredItem<Item> COMPOUND_OIL = material("compound_oil");
    /** 海带片 */
    public static final DeferredItem<Item> KELP_SLICE = material("kelp_slice");
    /** 锅盖 */
    public static final DeferredItem<Item> POT_LID = material("pot_lid");
    /** 勺子 */
    public static final DeferredItem<Item> SPOON = material("spoon");

    // ============================ 锭粒材料 ============================

    /** 饭晶合金粒 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_NUGGET = material("rice_crystal_alloy_nugget");
    /** 超重结晶粒 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_NUGGET = material("super_heavy_crystal_nugget");
    /** 复合米饭粒 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_NUGGET = material("compound_rice_meal_nugget");
    /** 清洁复合粒 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_NUGGET = material("cleansing_compound_nugget");
    /** 超懒合金粒 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_NUGGET = material("super_lazy_alloy_nugget");

    // ============================ 米与基础食物 ============================

    /** 米：饱食度 1，饱和系数 0.1；反胃 III 10s、中毒 I 5s、缓慢 I 10s、饥饿 I 30s（均 100%）。 */
    public static final FoodProperties RICE_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1F)
            .build();

    /** 米 */
    public static final DeferredItem<Item> RICE = registerFood("rice", RICE_FOOD,
            consumable(1.6F,
                    statusEffect(MobEffects.NAUSEA, 200, 2, 1.0F),
                    statusEffect(MobEffects.POISON, 100, 0, 1.0F),
                    statusEffect(MobEffects.SLOWNESS, 200, 0, 1.0F),
                    statusEffect(MobEffects.HUNGER, 600, 0, 1.0F)));

    /** 熟米：饱食度 5，饱和系数 0.3；反胃 I 5s；食用耗时 1 秒。 */
    public static final FoodProperties COOKED_RICE_FOOD = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.3F)
            .build();

    /** 熟米 */
    public static final DeferredItem<Item> COOKED_RICE = registerFood("cooked_rice", COOKED_RICE_FOOD,
            consumable(1.0F, statusEffect(MobEffects.NAUSEA, 100, 0, 1.0F)));

    /** 米果渣：无配方，只能靠吃米果产出。 */
    public static final DeferredItem<Item> RICE_POMACE = ITEMS.registerItem("rice_pomace",
            properties -> new Item(properties.stacksTo(64)) {
                @Override
                @SuppressWarnings("deprecation") // 26.3 暂未提供该提示钩子的替代方法
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                            Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("tooltip.mimiworld.rice_pomace.source")
                            .withStyle(ChatFormatting.GRAY));
                }
            });

    /** 米果：饱食度 5，饱和系数 2.0；漂浮 I 30s、反胃 I 5s、虚弱 I 30s；食用耗时 2 秒，吃完返还米果渣。 */
    public static final FoodProperties RICE_FRUIT_FOOD = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(2.0F)
            .build();

    /** 米果 */
    public static final DeferredItem<Item> RICE_FRUIT = ITEMS.registerItem("rice_fruit", properties -> new Item(
            properties.stacksTo(64).food(RICE_FRUIT_FOOD, consumable(2.0F,
                    statusEffect(MobEffects.LEVITATION, 600, 0, 1.0F),
                    statusEffect(MobEffects.NAUSEA, 100, 0, 1.0F),
                    statusEffect(MobEffects.WEAKNESS, 600, 0, 1.0F)))) {
        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack result = super.finishUsingItem(stack, level, entity);
            if (!level.isClientSide() && entity instanceof ServerPlayer player) {
                player.getInventory().placeItemBackInInventory(
                        new ItemStack(RICE_POMACE.get()), Prediction.SERVER_ONLY);
            }
            return result;
        }
    });

    // ============================ 米米系列肉鱼食物 ============================

    /** 熟啄米鸡肉：饱食度 5，饱和系数 0.3；1% 中毒 10s + 50% 反胃 5s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> COOKED_ZHUOMI_CHICKEN = registerFood("cooked_zhuomi_chicken",
            cookedFood(5, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.POISON, 200, 0, 0.01F),
                    statusEffect(MobEffects.NAUSEA, 100, 0, 0.5F)));

    /** 米吽牛肉：饱食度 2，饱和系数 0.3；反胃 20s、饥饿 30s、5% 中毒 25s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> RAW_MIMOO_BEEF = registerFood("raw_mimoo_beef",
            rawFood(2, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.NAUSEA, 400, 0, 1.0F),
                    statusEffect(MobEffects.HUNGER, 600, 0, 1.0F),
                    statusEffect(MobEffects.POISON, 500, 0, 0.05F)));

    /** 熟米吽牛肉：饱食度 8，饱和系数 0.3；缓慢 2s、25% 反胃 10s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> COOKED_MIMOO_BEEF = registerFood("cooked_mimoo_beef",
            cookedFood(8, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.SLOWNESS, 40, 0, 1.0F),
                    statusEffect(MobEffects.NAUSEA, 200, 0, 0.25F)));

    /** 米羊羊肉：饱食度 2，饱和系数 0.3；50% 虚弱 10s、70% 饥饿 20s、反胃 10s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> RAW_MIMI_MUTTON = registerFood("raw_mimi_mutton",
            rawFood(2, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.WEAKNESS, 200, 0, 0.5F),
                    statusEffect(MobEffects.HUNGER, 400, 0, 0.7F),
                    statusEffect(MobEffects.NAUSEA, 200, 0, 1.0F)));

    /** 熟米羊羊肉：饱食度 6，饱和系数 0.3；10% 反胃 5s、1% 虚弱 2s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> COOKED_MIMI_MUTTON = registerFood("cooked_mimi_mutton",
            cookedFood(6, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.NAUSEA, 100, 0, 0.1F),
                    statusEffect(MobEffects.WEAKNESS, 40, 0, 0.01F)));

    /** 米哼猪肉：饱食度 4，饱和系数 0.3；失明/虚弱/缓慢/中毒/反胃各 10s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> RAW_MIHENG_PORK = registerFood("raw_miheng_pork",
            rawFood(4, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.BLINDNESS, 200, 0, 1.0F),
                    statusEffect(MobEffects.WEAKNESS, 200, 0, 1.0F),
                    statusEffect(MobEffects.SLOWNESS, 200, 0, 1.0F),
                    statusEffect(MobEffects.POISON, 200, 0, 1.0F),
                    statusEffect(MobEffects.NAUSEA, 200, 0, 1.0F)));

    /** 熟米哼猪肉：饱食度 12，饱和系数 0.3；失明/虚弱 10s、50% 缓慢 10s、50% 反胃 10s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> COOKED_MIHENG_PORK = registerFood("cooked_miheng_pork",
            cookedFood(12, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.BLINDNESS, 200, 0, 1.0F),
                    statusEffect(MobEffects.WEAKNESS, 200, 0, 1.0F),
                    statusEffect(MobEffects.SLOWNESS, 200, 0, 0.5F),
                    statusEffect(MobEffects.NAUSEA, 200, 0, 0.5F)));

    /** 贪米鱼肉：饱食度 1，饱和系数 0.3；缓慢 10s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> RAW_TANMI_FISH = registerFood("raw_tanmi_fish",
            rawFood(1, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.SLOWNESS, 200, 0, 1.0F)));

    /** 熟贪米鱼肉：饱食度 6，饱和系数 0.3；缓慢 5s、虚弱 10s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> COOKED_TANMI_FISH = registerFood("cooked_tanmi_fish",
            cookedFood(6, 0.3F), consumable(1.5F,
                    statusEffect(MobEffects.SLOWNESS, 100, 0, 1.0F),
                    statusEffect(MobEffects.WEAKNESS, 200, 0, 1.0F)));

    /** 腐米肉：饱食度 2，饱和系数 0.1；中毒 5s、饥饿 30s、缓慢 10s、1% 中毒 60s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> ROTTEN_RICE_FLESH = registerFood("rotten_rice_flesh",
            cookedFood(2, 0.1F), consumable(1.5F,
                    statusEffect(MobEffects.POISON, 100, 0, 1.0F),
                    statusEffect(MobEffects.HUNGER, 600, 0, 1.0F),
                    statusEffect(MobEffects.SLOWNESS, 200, 0, 1.0F),
                    statusEffect(MobEffects.POISON, 1200, 0, 0.01F)));

    /** 啄米鸡肉：饱食度 1，饱和系数 0.1；10% 中毒 20s、90% 饥饿 30s、反胃 15s；食用耗时 1.5 秒。 */
    public static final DeferredItem<Item> ZHUOMI_CHICKEN = registerFood("zhuomi_chicken",
            rawFood(1, 0.1F), consumable(1.5F,
                    statusEffect(MobEffects.POISON, 400, 0, 0.1F),
                    statusEffect(MobEffects.HUNGER, 600, 0, 0.9F),
                    statusEffect(MobEffects.NAUSEA, 300, 0, 1.0F)));

    // ============================ 残堕界食材 ============================

    /** 熟界天胎花：饱食度 2 */
    public static final DeferredItem<Item> COOKED_REALM_HEAVEN_EMBRYO_FLOWER =
            registerFood("cooked_realm_heaven_embryo_flower", cookedFood(4, 0.4F));

    /** 虚无触足：饱食度 1，30% 饥饿 I 30s */
    public static final DeferredItem<Item> VOID_TOUCH_TENTACLE = registerFood("void_touch_tentacle",
            rawFood(2, 0.3F), rawConsumable(0.3F));

    /** 熟虚无触足：饱食度 2.5 */
    public static final DeferredItem<Item> COOKED_VOID_TOUCH_TENTACLE =
            registerFood("cooked_void_touch_tentacle", cookedFood(5, 0.6F));

    /** 棘花瓣：饱食度 1，30% 饥饿 I 30s */
    public static final DeferredItem<Item> THORN_PETAL = registerFood("thorn_petal",
            rawFood(2, 0.2F), rawConsumable(0.3F));

    /** 熟棘花瓣：饱食度 2 */
    public static final DeferredItem<Item> COOKED_THORN_PETAL =
            registerFood("cooked_thorn_petal", cookedFood(4, 0.4F));

    /** 米凤肉块：饱食度 1.5，30% 饥饿 I 30s */
    public static final DeferredItem<Item> RICE_PHOENIX_MEAT_CHUNK = registerFood("rice_phoenix_meat_chunk",
            rawFood(3, 0.3F), rawConsumable(0.3F));

    /** 熟米凤肉块：饱食度 3.5 */
    public static final DeferredItem<Item> COOKED_RICE_PHOENIX_MEAT_CHUNK =
            registerFood("cooked_rice_phoenix_meat_chunk", cookedFood(7, 0.8F));

    /** 特色堕落米：饱食度 1.5，30% 饥饿 I 30s */
    public static final DeferredItem<Item> SPECIALTY_FALLEN_RICE = registerFood("specialty_fallen_rice",
            rawFood(3, 0.3F), rawConsumable(0.3F));

    /** 熟特色堕落米：饱食度 3.5 */
    public static final DeferredItem<Item> COOKED_SPECIALTY_FALLEN_RICE =
            registerFood("cooked_specialty_fallen_rice", cookedFood(7, 0.7F));

    /** 浅鲛尾：饱食度 1，30% 饥饿 I 30s */
    public static final DeferredItem<Item> PALE_MERMAID_TAIL = registerFood("pale_mermaid_tail",
            rawFood(2, 0.2F), rawConsumable(0.3F));

    /** 熟浅鲛尾：饱食度 2.5 */
    public static final DeferredItem<Item> COOKED_PALE_MERMAID_TAIL =
            registerFood("cooked_pale_mermaid_tail", cookedFood(5, 0.6F));

    /** 望米伤鲛足：饱食度 1，40% 饥饿 I 30s */
    public static final DeferredItem<Item> WANGMI_WOUNDED_MERMAID_LEG =
            registerFood("wangmi_wounded_mermaid_leg", rawFood(2, 0.3F), rawConsumable(0.4F));

    /** 熟望米伤鲛足：饱食度 2.5 */
    public static final DeferredItem<Item> COOKED_WANGMI_WOUNDED_MERMAID_LEG =
            registerFood("cooked_wangmi_wounded_mermaid_leg", cookedFood(5, 0.6F));

    /** 半蜃肉片：饱食度 1，30% 饥饿 I 30s */
    public static final DeferredItem<Item> HALF_MIRAGE_MEAT_SLICE = registerFood("half_mirage_meat_slice",
            rawFood(2, 0.3F), rawConsumable(0.3F));

    /** 熟半蜃肉片：饱食度 2.5 */
    public static final DeferredItem<Item> COOKED_HALF_MIRAGE_MEAT_SLICE =
            registerFood("cooked_half_mirage_meat_slice", cookedFood(5, 0.6F));

    /** 噬鹫翅：饱食度 1，30% 饥饿 I 30s */
    public static final DeferredItem<Item> DEVOURER_VULTURE_WING = registerFood("devourer_vulture_wing",
            rawFood(2, 0.3F), rawConsumable(0.3F));

    /** 熟噬鹫翅：饱食度 3 */
    public static final DeferredItem<Item> COOKED_DEVOURER_VULTURE_WING =
            registerFood("cooked_devourer_vulture_wing", cookedFood(6, 0.7F));

    // ============================ 料理与饮品 ============================

    /** 天虚棘脑汤：饱食度 10；力量 20/1min、生命恢复 100/5min、迅捷 10/2min */
    public static final DeferredItem<Item> CELESTIAL_VOID_THORN_BRAIN_SOUP =
            registerEffectFood("celestial_void_thorn_brain_soup", 10,
                    effect(MobEffects.STRENGTH, 20, MIN_1),
                    effect(MobEffects.REGENERATION, 100, MIN_5),
                    effect(MobEffects.SPEED, 10, MIN_2));

    /** 香混肉片：饱食度 10；虚弱 1/1min、生命恢复 255/1min */
    public static final DeferredItem<Item> FRAGRANT_MIXED_MEAT_SLICE =
            registerEffectFood("fragrant_mixed_meat_slice", 10,
                    effect(MobEffects.WEAKNESS, 1, MIN_1),
                    effect(MobEffects.REGENERATION, 255, MIN_1));

    /** 酱香双鲛尾：饱食度 10；迅捷 20/2min、跳跃提升 5/5min */
    public static final DeferredItem<Item> SAUCE_GLAZED_TWIN_MERMAID_TAIL =
            registerEffectFood("sauce_glazed_twin_mermaid_tail", 10,
                    effect(MobEffects.SPEED, 20, MIN_2),
                    effect(MobEffects.JUMP_BOOST, 5, MIN_5));

    /** 天脑汁：饱食度 2 */
    public static final DeferredItem<Item> CELESTIAL_BRAIN_JUICE =
            registerFood("celestial_brain_juice", cookedFood(4, 0.4F));

    /** 酱脑：饱食度 2.5 */
    public static final DeferredItem<Item> SAUCED_BRAIN = registerFood("sauced_brain", cookedFood(5, 0.6F));

    /** 米堕汁：饱食度 2 */
    public static final DeferredItem<Item> FALLEN_RICE_JUICE =
            registerFood("fallen_rice_juice", cookedFood(4, 0.3F));

    /** 凤肉泥：饱食度 1，30% 饥饿 I 30s */
    public static final DeferredItem<Item> PHOENIX_MEAT_PASTE = registerFood("phoenix_meat_paste",
            rawFood(2, 0.3F), rawConsumable(0.3F));

    /** 凤球：饱食度 2.5 */
    public static final DeferredItem<Item> PHOENIX_BALL = registerFood("phoenix_ball", cookedFood(5, 0.6F));

    /** 酱烤鹫翅：饱食度 10；创造飞行/1min、生命恢复 50/3min */
    public static final DeferredItem<Item> SAUCE_ROASTED_VULTURE_WING =
            registerEffectFood("sauce_roasted_vulture_wing", 10,
                    effect(ModEffects.CREATIVE_FLIGHT, 1, MIN_1),
                    effect(MobEffects.REGENERATION, 50, MIN_3));

    /** 腌鹫翅：饱食度 1.5，20% 饥饿 I 30s */
    public static final DeferredItem<Item> MARINATED_VULTURE_WING = registerFood("marinated_vulture_wing",
            rawFood(3, 0.3F), rawConsumable(0.2F));

    /** 堕米肉丸汤：饱食度 10；缓慢 2/3min、生命恢复 255/1min、力量 50/1min */
    public static final DeferredItem<Item> FALLEN_RICE_MEATBALL_SOUP =
            registerEffectFood("fallen_rice_meatball_soup", 10,
                    effect(MobEffects.SLOWNESS, 2, MIN_3),
                    effect(MobEffects.REGENERATION, 255, MIN_1),
                    effect(MobEffects.STRENGTH, 50, MIN_1));

    /** 蛟鲛肉泥：饱食度 1，30% 饥饿 I 30s */
    public static final DeferredItem<Item> JIAO_MERMAID_MEAT_PASTE = registerFood("jiao_mermaid_meat_paste",
            rawFood(2, 0.3F), rawConsumable(0.3F));

    /** 凤蜃肉泥：饱食度 1，30% 饥饿 I 30s */
    public static final DeferredItem<Item> PHOENIX_MIRAGE_MEAT_PASTE = registerFood("phoenix_mirage_meat_paste",
            rawFood(2, 0.3F), rawConsumable(0.3F));

    /** 油爆虚无触：饱食度 10；迅捷 10/2min、力量 30/3min */
    public static final DeferredItem<Item> OIL_SEARED_VOID_TENTACLE =
            registerEffectFood("oil_seared_void_tentacle", 10,
                    effect(MobEffects.SPEED, 10, MIN_2),
                    effect(MobEffects.STRENGTH, 30, MIN_3));

    /** 蛟足炒饭：饱食度 10；迅捷 20/1min、力量 100/1min */
    public static final DeferredItem<Item> MERMAID_LEG_FRIED_RICE =
            registerEffectFood("mermaid_leg_fried_rice", 10,
                    effect(MobEffects.SPEED, 20, MIN_1),
                    effect(MobEffects.STRENGTH, 100, MIN_1));

    /** 龙凤花汤：饱食度 10；生命恢复 255/1min、生命提升 50/3min、虚弱 5/2min */
    public static final DeferredItem<Item> DRAGON_PHOENIX_FLOWER_SOUP =
            registerEffectFood("dragon_phoenix_flower_soup", 10,
                    effect(MobEffects.REGENERATION, 255, MIN_1),
                    effect(MobEffects.HEALTH_BOOST, 50, MIN_3),
                    effect(MobEffects.WEAKNESS, 5, MIN_2));

    /** 蜃楼幻想：饱食度 10；反胃 255/30s、瞬间恢复 255 */
    public static final DeferredItem<Item> MIRAGE_ILLUSION = registerEffectFood("mirage_illusion", 10,
            effect(MobEffects.NAUSEA, 255, SEC_30),
            effect(MobEffects.INSTANT_HEALTH, 255, MIN_5));

    /** 花香蛟尾：饱食度 10；生命恢复 255/1min、生命提升 50/1min */
    public static final DeferredItem<Item> FLORAL_MERMAID_TAIL = registerEffectFood("floral_mermaid_tail", 10,
            effect(MobEffects.REGENERATION, 255, MIN_1),
            effect(MobEffects.HEALTH_BOOST, 50, MIN_1));

    /** 海兽相交饭：饱食度 10；反胃 255/1min、虚弱 255/30s、生命恢复 255/1min、瞬间治疗 255、生命提升 255/1min */
    public static final DeferredItem<Item> SEA_BEAST_JIAO_RICE = registerEffectFood("sea_beast_jiao_rice", 10,
            effect(MobEffects.NAUSEA, 255, MIN_1),
            effect(MobEffects.WEAKNESS, 255, SEC_30),
            effect(MobEffects.REGENERATION, 255, MIN_1),
            effect(MobEffects.INSTANT_HEALTH, 255, MIN_1),
            effect(MobEffects.HEALTH_BOOST, 255, MIN_1));

    /** 佛幻想：饱食度 10；反胃 255/1min、失明 1/1min、迅捷 50/1min、创造飞行/1min、生命恢复 255/10s */
    public static final DeferredItem<Item> BUDDHA_ILLUSION = registerEffectFood("buddha_illusion", 10,
            effect(MobEffects.NAUSEA, 255, MIN_1),
            effect(MobEffects.BLINDNESS, 1, MIN_1),
            effect(MobEffects.SPEED, 50, MIN_1),
            effect(ModEffects.CREATIVE_FLIGHT, 1, MIN_1),
            effect(MobEffects.REGENERATION, 255, SEC_10));

    /** 米雪茶：饱食度 10；生命恢复 255/30s、瞬间治疗 255、生命提升 255/1min */
    public static final DeferredItem<Item> RICE_SNOW_TEA = registerEffectFood("rice_snow_tea", 10,
            effect(MobEffects.REGENERATION, 255, SEC_30),
            effect(MobEffects.INSTANT_HEALTH, 255, SEC_30),
            effect(MobEffects.HEALTH_BOOST, 255, MIN_1));

    /** 煎龙凤饼：饱食度 10；生命恢复 255/3min、中毒 100/3min */
    public static final DeferredItem<Item> PAN_FRIED_DRAGON_PHOENIX_CAKE =
            registerEffectFood("pan_fried_dragon_phoenix_cake", 10,
                    effect(MobEffects.REGENERATION, 255, MIN_3),
                    effect(MobEffects.POISON, 100, MIN_3));

    /** 天堕茶：饱食度 10；创造飞行/1min、缓慢 2/2min */
    public static final DeferredItem<Item> CELESTIAL_FALLEN_TEA = registerEffectFood("celestial_fallen_tea", 10,
            effect(ModEffects.CREATIVE_FLIGHT, 1, MIN_1),
            effect(MobEffects.SLOWNESS, 2, MIN_2));

    /** 凤包翅：饱食度 10；创造飞行/1min、力量 10/1min */
    public static final DeferredItem<Item> PHOENIX_WRAPPED_WING = registerEffectFood("phoenix_wrapped_wing", 10,
            effect(ModEffects.CREATIVE_FLIGHT, 1, MIN_1),
            effect(MobEffects.STRENGTH, 10, MIN_1));

    /** 腌翅包：饱食度 1.5，20% 饥饿 I 30s */
    public static final DeferredItem<Item> MARINATED_WING_WRAP = registerFood("marinated_wing_wrap",
            rawFood(3, 0.3F), rawConsumable(0.2F));

    /** 腌肉片：饱食度 1，20% 饥饿 I 30s */
    public static final DeferredItem<Item> MARINATED_MEAT_SLICE = registerFood("marinated_meat_slice",
            rawFood(2, 0.3F), rawConsumable(0.2F));

    /** 肉片粥：饱食度 10；力量 20/3min、迅捷 20/5min */
    public static final DeferredItem<Item> MEAT_SLICE_CONGEE = registerEffectFood("meat_slice_congee", 10,
            effect(MobEffects.STRENGTH, 20, MIN_3),
            effect(MobEffects.SPEED, 20, MIN_5));

    /** 肢体烤串：饱食度 3 */
    public static final DeferredItem<Item> LIMB_SKEWER = registerFood("limb_skewer", cookedFood(6, 0.7F));

    /** 饭团：饱食度 666；生命恢复 255/2min、缓慢 5/1min */
    public static final DeferredItem<Item> ONIGIRI = registerEffectFood("onigiri", 666,
            effect(MobEffects.REGENERATION, 255, MIN_2),
            effect(MobEffects.SLOWNESS, 5, MIN_1));

    /** 天脑饭团：饱食度 10；反胃 255/1min、创造飞行/2min、迅捷 10/3min */
    public static final DeferredItem<Item> CELESTIAL_BRAIN_RICE_BALL =
            registerEffectFood("celestial_brain_rice_ball", 10,
                    effect(MobEffects.NAUSEA, 255, MIN_1),
                    effect(ModEffects.CREATIVE_FLIGHT, 1, MIN_2),
                    effect(MobEffects.SPEED, 10, MIN_3));

    /** 浅鲛饭团：饱食度 10；虚弱 5/1min、迅捷 30/2min、力量 50/30s */
    public static final DeferredItem<Item> PALE_MERMAID_RICE_BALL =
            registerEffectFood("pale_mermaid_rice_ball", 10,
                    effect(MobEffects.WEAKNESS, 5, MIN_1),
                    effect(MobEffects.SPEED, 30, MIN_2),
                    effect(MobEffects.STRENGTH, 50, SEC_30));

    /** 蛟足饭团：饱食度 10；力量 20/2min、生命恢复 50/1min */
    public static final DeferredItem<Item> MERMAID_LEG_RICE_BALL =
            registerEffectFood("mermaid_leg_rice_ball", 10,
                    effect(MobEffects.STRENGTH, 20, MIN_2),
                    effect(MobEffects.REGENERATION, 50, MIN_1));

    /** 鹫翅饭团：饱食度 10；创造飞行/30s、力量 20/1min、迅捷 10/1min */
    public static final DeferredItem<Item> VULTURE_WING_RICE_BALL =
            registerEffectFood("vulture_wing_rice_ball", 10,
                    effect(ModEffects.CREATIVE_FLIGHT, 1, SEC_30),
                    effect(MobEffects.STRENGTH, 20, MIN_1),
                    effect(MobEffects.SPEED, 10, MIN_1));

    /** 超级大乱炖：饱食度 666；创造飞行 15min、力量 255/30min、急迫 255/15min、迅捷 50/15min、
     *  生命提升 255/30min、瞬间恢复 255、防火/1h、隐身/20min、夜视/1h */
    public static final DeferredItem<Item> SUPER_GRAND_STEW = registerEffectFood("super_grand_stew", 666,
            effect(ModEffects.CREATIVE_FLIGHT, 1, MIN_15),
            effect(MobEffects.STRENGTH, 255, MIN_30),
            effect(MobEffects.HASTE, 255, MIN_15),
            effect(MobEffects.SPEED, 50, MIN_15),
            effect(MobEffects.HEALTH_BOOST, 255, MIN_30),
            effect(MobEffects.INSTANT_HEALTH, 255, MIN_30),
            effect(MobEffects.FIRE_RESISTANCE, 1, HOUR_1),
            effect(MobEffects.INVISIBILITY, 1, MIN_20),
            effect(MobEffects.NIGHT_VISION, 1, HOUR_1));

    // ============================ 剑类武器 ============================

    /** 饭晶剑：攻击 +3 / 攻速 -2.4。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_SWORD =
            sword("rice_crystal_sword", ModToolMaterials.RICE_CRYSTAL, 3, -2.4F);

    /** 饭晶合金剑：攻击 +3 / 攻速 -2.4。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_SWORD =
            sword("rice_crystal_alloy_sword", ModToolMaterials.RICE_CRYSTAL_ALLOY, 3, -2.4F);

    /** 超重结晶剑：攻击 +5 / 攻速 -2.8（慢速重剑）。 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_SWORD =
            sword("super_heavy_crystal_sword", ModToolMaterials.SUPER_HEAVY_CRYSTAL, 5, -2.8F);

    /** 复合米饭剑：攻击 +4 / 攻速 -2.4。 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_SWORD =
            sword("compound_rice_meal_sword", ModToolMaterials.COMPOUND_RICE_MEAL, 4, -2.4F);

    /** 清洁复合剑：攻击 +4 / 攻速 -2.4。 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_SWORD =
            sword("cleansing_compound_sword", ModToolMaterials.CLEANSING_COMPOUND, 4, -2.4F);

    /** 超懒合金剑：攻击 +5 / 攻速 -2.4。 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_SWORD =
            sword("super_lazy_alloy_sword", ModToolMaterials.SUPER_LAZY_ALLOY, 5, -2.4F);

    /** 超懒神剑：攻击 +8 / 攻速 -2.4，动态彩虹渐变名称。 */
    public static final DeferredItem<Item> SUPER_LAZY_DIVINE_SWORD =
            ITEMS.registerItem("super_lazy_divine_sword",
                    properties -> new RainbowNameSwordItem(properties.stacksTo(1)
                            .sword(ModToolMaterials.SUPER_LAZY_ALLOY, 8, -2.4F)));

    /** 最懒天堕剑：攻击 +6 / 攻速 -2.4，动态彩虹渐变名称。 */
    public static final DeferredItem<Item> LAZIEST_HEAVENFALL_SWORD_COUNTERFEIT =
            ITEMS.registerItem("laziest_heavenfall_sword_counterfeit",
                    properties -> new RainbowNameSwordItem(properties.stacksTo(1)
                            .sword(ModToolMaterials.LAZIEST_HEAVENFALL, 6, -2.4F)));

    /** 残堕逆鳞之剑：攻击 +5 / 攻速 -2.8（慢速重剑）。 */
    public static final DeferredItem<Item> REMNANT_FALLEN_REVERSE_SCALE_SWORD =
            sword("remnant_fallen_reverse_scale_sword", ModToolMaterials.REMNANT_FALLEN, 5, -2.8F);

    // ============================ 镐类工具 ============================

    /** 超重结晶镐：攻击 +1 / 攻速 -2.8。 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_PICKAXE =
            pickaxe("super_heavy_crystal_pickaxe", ModToolMaterials.SUPER_HEAVY_CRYSTAL, 1, -2.8F);

    /** 复合米饭镐：攻击 +1 / 攻速 -2.8。 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_PICKAXE =
            pickaxe("compound_rice_meal_pickaxe", ModToolMaterials.COMPOUND_RICE_MEAL, 1, -2.8F);

    /** 清洁复合镐：攻击 +1 / 攻速 -2.8。 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_PICKAXE =
            pickaxe("cleansing_compound_pickaxe", ModToolMaterials.CLEANSING_COMPOUND, 1, -2.8F);

    /** 超懒合金镐：攻击 +1 / 攻速 -2.8。 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_PICKAXE =
            pickaxe("super_lazy_alloy_pickaxe", ModToolMaterials.SUPER_LAZY_ALLOY, 1, -2.8F);

    /** 最懒天堕镐：攻击 +1 / 攻速 -2.8。 */
    public static final DeferredItem<Item> LAZIEST_HEAVENFALL_PICKAXE =
            pickaxe("laziest_heavenfall_pickaxe", ModToolMaterials.LAZIEST_HEAVENFALL, 1, -2.8F);

    /** 残堕崩解之镐：攻击 +1 / 攻速 -2.8。 */
    public static final DeferredItem<Item> REMNANT_FALLEN_DISINTEGRATION_PICKAXE =
            pickaxe("remnant_fallen_disintegration_pickaxe", ModToolMaterials.REMNANT_FALLEN, 1, -2.8F);

    // ============================ 防具：饭晶合金套 ============================

    /** 饭晶合金头盔：+7 最大生命值。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_HELMET = armor(
            "rice_crystal_alloy_helmet", ModArmorMaterials.RICE_CRYSTAL_ALLOY, ArmorType.HELMET,
            7.0F, Map.of(), false);

    /** 饭晶合金胸甲：+15 最大生命值。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_CHESTPLATE = armor(
            "rice_crystal_alloy_chestplate", ModArmorMaterials.RICE_CRYSTAL_ALLOY, ArmorType.CHESTPLATE,
            15.0F, Map.of(), false);

    /** 饭晶合金护腿：+12 最大生命值。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_LEGGINGS = armor(
            "rice_crystal_alloy_leggings", ModArmorMaterials.RICE_CRYSTAL_ALLOY, ArmorType.LEGGINGS,
            12.0F, Map.of(), false);

    /** 饭晶合金靴子：+6 最大生命值。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_BOOTS = armor(
            "rice_crystal_alloy_boots", ModArmorMaterials.RICE_CRYSTAL_ALLOY, ArmorType.BOOTS,
            6.0F, Map.of(), false);

    // ============================ 防具：超重结晶套 ============================

    /** 超重结晶头盔：+20 最大生命值。 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_HELMET = armor(
            "super_heavy_crystal_helmet", ModArmorMaterials.SUPER_HEAVY_CRYSTAL, ArmorType.HELMET,
            20.0F, Map.of(), false);

    /** 超重结晶胸甲：+35 最大生命值。 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_CHESTPLATE = armor(
            "super_heavy_crystal_chestplate", ModArmorMaterials.SUPER_HEAVY_CRYSTAL, ArmorType.CHESTPLATE,
            35.0F, Map.of(), false);

    /** 超重结晶护腿：+30 最大生命值。 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_LEGGINGS = armor(
            "super_heavy_crystal_leggings", ModArmorMaterials.SUPER_HEAVY_CRYSTAL, ArmorType.LEGGINGS,
            30.0F, Map.of(), false);

    /** 超重结晶靴子：+15 最大生命值。 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_BOOTS = armor(
            "super_heavy_crystal_boots", ModArmorMaterials.SUPER_HEAVY_CRYSTAL, ArmorType.BOOTS,
            15.0F, Map.of(), false);

    // ============================ 防具：复合米饭套 ============================

    /** 复合米饭头盔：+50 最大生命值。 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_HELMET = armor(
            "compound_rice_meal_helmet", ModArmorMaterials.COMPOUND_RICE_MEAL, ArmorType.HELMET,
            50.0F, Map.of(), false);

    /** 复合米饭胸甲：+100 最大生命值，防火 I。 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_CHESTPLATE = armor(
            "compound_rice_meal_chestplate", ModArmorMaterials.COMPOUND_RICE_MEAL, ArmorType.CHESTPLATE,
            100.0F, Map.of(MobEffects.FIRE_RESISTANCE, 0), false);

    /** 复合米饭护腿：+80 最大生命值，生命恢复 I。 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_LEGGINGS = armor(
            "compound_rice_meal_leggings", ModArmorMaterials.COMPOUND_RICE_MEAL, ArmorType.LEGGINGS,
            80.0F, Map.of(MobEffects.REGENERATION, 0), false);

    /** 复合米饭靴子：+70 最大生命值，迅捷 I。 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_BOOTS = armor(
            "compound_rice_meal_boots", ModArmorMaterials.COMPOUND_RICE_MEAL, ArmorType.BOOTS,
            70.0F, Map.of(MobEffects.SPEED, 0), false);

    // ============================ 防具：清洁复合套 ============================

    /** 清洁复合头盔：+150 最大生命值，夜视 I、力量 III。 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_HELMET = armor(
            "cleansing_compound_helmet", ModArmorMaterials.CLEANSING_COMPOUND, ArmorType.HELMET,
            150.0F, Map.of(MobEffects.NIGHT_VISION, 0, MobEffects.STRENGTH, 2), false);

    /** 清洁复合胸甲：+200 最大生命值，防火 I、水下呼吸 I、生命恢复 III。 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_CHESTPLATE = armor(
            "cleansing_compound_chestplate", ModArmorMaterials.CLEANSING_COMPOUND, ArmorType.CHESTPLATE,
            200.0F, Map.of(MobEffects.FIRE_RESISTANCE, 0, MobEffects.WATER_BREATHING, 0,
                    MobEffects.REGENERATION, 2), false);

    /** 清洁复合护腿：+175 最大生命值，急迫 II、跳跃提升 I。 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_LEGGINGS = armor(
            "cleansing_compound_leggings", ModArmorMaterials.CLEANSING_COMPOUND, ArmorType.LEGGINGS,
            175.0F, Map.of(MobEffects.HASTE, 1, MobEffects.JUMP_BOOST, 0), false);

    /** 清洁复合靴子：+175 最大生命值，迅捷 II。 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_BOOTS = armor(
            "cleansing_compound_boots", ModArmorMaterials.CLEANSING_COMPOUND, ArmorType.BOOTS,
            175.0F, Map.of(MobEffects.SPEED, 1), false);

    // ============================ 防具：超懒合金套 ============================

    /** 超懒合金头盔：+200 最大生命值，夜视 I、力量 VI。 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_HELMET = armor(
            "super_lazy_alloy_helmet", ModArmorMaterials.SUPER_LAZY_ALLOY, ArmorType.HELMET,
            200.0F, Map.of(MobEffects.NIGHT_VISION, 0, MobEffects.STRENGTH, 5), false);

    /** 超懒合金胸甲：+500 最大生命值，防火 I、水下呼吸 I、生命恢复 XX。 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_CHESTPLATE = armor(
            "super_lazy_alloy_chestplate", ModArmorMaterials.SUPER_LAZY_ALLOY, ArmorType.CHESTPLATE,
            500.0F, Map.of(MobEffects.FIRE_RESISTANCE, 0, MobEffects.WATER_BREATHING, 0,
                    MobEffects.REGENERATION, 19), false);

    /** 超懒合金护腿：+250 最大生命值，急迫 V、跳跃提升 III。 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_LEGGINGS = armor(
            "super_lazy_alloy_leggings", ModArmorMaterials.SUPER_LAZY_ALLOY, ArmorType.LEGGINGS,
            250.0F, Map.of(MobEffects.HASTE, 4, MobEffects.JUMP_BOOST, 2), false);

    /** 超懒合金靴子：+250 最大生命值，迅捷 III。 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_BOOTS = armor(
            "super_lazy_alloy_boots", ModArmorMaterials.SUPER_LAZY_ALLOY, ArmorType.BOOTS,
            250.0F, Map.of(MobEffects.SPEED, 2), false);

    // ============================ 防具：最懒天堕套 ============================

    /** 天堕翼胸甲：+700 最大生命值。 */
    public static final DeferredItem<Item> HEAVENFALL_WINGS_CHESTPLATE = armor(
            "heavenfall_wings_chestplate", ModArmorMaterials.HEAVENFALL, ArmorType.CHESTPLATE,
            700.0F, Map.of(), false);

    /** 最懒天堕头盔：+300 最大生命值。 */
    public static final DeferredItem<Item> LAZIEST_HEAVENFALL_HELMET = armor(
            "laziest_heavenfall_helmet", ModArmorMaterials.HEAVENFALL, ArmorType.HELMET,
            300.0F, Map.of(), false);

    /** 最懒天堕护腿：+400 最大生命值。 */
    public static final DeferredItem<Item> LAZIEST_HEAVENFALL_LEGGINGS = armor(
            "laziest_heavenfall_leggings", ModArmorMaterials.HEAVENFALL, ArmorType.LEGGINGS,
            400.0F, Map.of(), false);

    /** 最懒天堕靴子：+400 最大生命值。 */
    public static final DeferredItem<Item> LAZIEST_HEAVENFALL_BOOTS = armor(
            "laziest_heavenfall_boots", ModArmorMaterials.HEAVENFALL, ArmorType.BOOTS,
            400.0F, Map.of(), false);

    // ============================ 防具：残堕宗罪套 ============================

    /** 残堕宗罪头盔：+400 最大生命值。 */
    public static final DeferredItem<Item> REMNANT_FALLEN_CARDINAL_SIN_HELMET = armor(
            "remnant_fallen_cardinal_sin_helmet", ModArmorMaterials.REMNANT_FALLEN_CARDINAL_SIN,
            ArmorType.HELMET, 400.0F, Map.of(), false);

    /** 残堕宗罪胸甲：+1000 最大生命值。 */
    public static final DeferredItem<Item> REMNANT_FALLEN_CARDINAL_SIN_CHESTPLATE = armor(
            "remnant_fallen_cardinal_sin_chestplate", ModArmorMaterials.REMNANT_FALLEN_CARDINAL_SIN,
            ArmorType.CHESTPLATE, 1000.0F, Map.of(), false);

    /** 残堕宗罪护腿：+700 最大生命值。 */
    public static final DeferredItem<Item> REMNANT_FALLEN_CARDINAL_SIN_LEGGINGS = armor(
            "remnant_fallen_cardinal_sin_leggings", ModArmorMaterials.REMNANT_FALLEN_CARDINAL_SIN,
            ArmorType.LEGGINGS, 700.0F, Map.of(), false);

    /** 残堕宗罪靴子：+700 最大生命值。 */
    public static final DeferredItem<Item> REMNANT_FALLEN_CARDINAL_SIN_BOOTS = armor(
            "remnant_fallen_cardinal_sin_boots", ModArmorMaterials.REMNANT_FALLEN_CARDINAL_SIN,
            ArmorType.BOOTS, 700.0F, Map.of(), false);

    // ============================ 防具：神翼系列 ============================

    /** 米米神翼：胸甲，给予创造飞行能力。 */
    public static final DeferredItem<Item> MIMI_DIVINE_WINGS = armor(
            "mimi_divine_wings", ModArmorMaterials.MIMI_WINGS, ArmorType.CHESTPLATE,
            0.0F, Map.of(), true);

    /** 超懒神翼胸甲：+1300 最大生命值，防火 I、生命恢复 L、力量 X、急迫 V，并给予创造飞行。 */
    public static final DeferredItem<Item> SUPER_LAZY_WINGS_CHESTPLATE = armor(
            "super_lazy_wings_chestplate", ModArmorMaterials.SUPER_LAZY_WINGS, ArmorType.CHESTPLATE,
            1300.0F, Map.of(MobEffects.FIRE_RESISTANCE, 0, MobEffects.REGENERATION, 49,
                    MobEffects.STRENGTH, 9, MobEffects.HASTE, 4), true);

    // ============================ 弓 ============================

    /** 饭晶弓：伤害倍率 ×1.2，耐久 384。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_BOW = bow(
            "rice_crystal_bow", 1.2D, "fanjinggong",
            "mimiworld:textures/item/fanjinggong.png", 384);

    /** 饭晶合金弓：伤害倍率 ×1.7，耐久 896。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_BOW = bow(
            "rice_crystal_alloy_bow", 1.7D, "mubangong",
            "mimiworld:textures/item/fanjinghejingong.png", 896);

    /** 超重结晶弓：伤害倍率 ×2.0，耐久 1024。 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_BOW = bow(
            "super_heavy_crystal_bow", 2.0D, "mubangong",
            "mimiworld:textures/item/chaozhongjiejinggong.png", 1024);

    /** 复合米饭弓：伤害倍率 ×2.3，耐久 1280。 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_BOW = bow(
            "compound_rice_meal_bow", 2.3D, "mubangong",
            "mimiworld:textures/item/fuhemifangong.png", 1280);

    /** 清洁复合弓：伤害倍率 ×2.6，耐久 1536。 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_BOW = bow(
            "cleansing_compound_bow", 2.6D, "mubangong",
            "mimiworld:textures/item/qingjiefuhegong.png", 1536);

    /** 超懒合金弓：伤害倍率 ×3.0，耐久 2048。 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_BOW = bow(
            "super_lazy_alloy_bow", 3.0D, "mubangong",
            "mimiworld:textures/item/chaolanhejingong.png", 2048);

    /** 残堕宗罪之弓：伤害倍率 ×3.5，耐久 2304。 */
    public static final DeferredItem<Item> REMNANT_FALLEN_CARDINAL_SIN_BOW = bow(
            "remnant_fallen_cardinal_sin_bow", 3.5D, "mubangong",
            "mimiworld:textures/item/canduozongzuizhigong.png", 2304);

    // ============================ 箭 ============================

    /** 米箭：3 伤害，无中毒。 */
    public static final DeferredItem<Item> RICE_ARROW = arrow("rice_arrow", 3.0D, 0);
    /** 饭晶箭：4 伤害 + 中毒 I。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ARROW = arrow("rice_crystal_arrow", 4.0D, 1);
    /** 饭晶合金箭：6 伤害 + 中毒 II。 */
    public static final DeferredItem<Item> RICE_CRYSTAL_ALLOY_ARROW =
            arrow("rice_crystal_alloy_arrow", 6.0D, 2);
    /** 超重结晶箭：7 伤害 + 中毒 II。 */
    public static final DeferredItem<Item> SUPER_HEAVY_CRYSTAL_ARROW =
            arrow("super_heavy_crystal_arrow", 7.0D, 2);
    /** 复合米饭箭：8 伤害 + 中毒 II。 */
    public static final DeferredItem<Item> COMPOUND_RICE_MEAL_ARROW =
            arrow("compound_rice_meal_arrow", 8.0D, 2);
    /** 清洁复合箭：9 伤害 + 中毒 II。 */
    public static final DeferredItem<Item> CLEANSING_COMPOUND_ARROW =
            arrow("cleansing_compound_arrow", 9.0D, 2);
    /** 超懒合金箭：10 伤害 + 中毒 II。 */
    public static final DeferredItem<Item> SUPER_LAZY_ALLOY_ARROW =
            arrow("super_lazy_alloy_arrow", 10.0D, 2);

    // ============================ 剑谱系列 ============================

    /** 超懒剑法：普通物品，堆叠 1，动态彩虹渐变名称。 */
    public static final DeferredItem<Item> SUPER_LAZY_SWORD_ART = ITEMS.registerItem(
            "super_lazy_sword_art",
            properties -> new RainbowNameItem(properties.stacksTo(1)));

    /** 终末剑诀：普通物品，堆叠 1，动态彩虹渐变名称。 */
    public static final DeferredItem<Item> FINAL_SWORD_CODEX = ITEMS.registerItem(
            "final_sword_codex",
            properties -> new RainbowNameItem(properties.stacksTo(1)));

    // ============================ 可投掷物品与图腾 ============================

    /** 米团：可投掷，命中 1 伤害 + 反胃 I/缓慢 I 各 10s，冷却 2s。 */
    public static final DeferredItem<Item> RICE_BALL =
            ITEMS.registerItem("rice_ball", properties -> new RiceBallItem(properties.stacksTo(64)));

    /** 熟米团：可投掷，命中 2.5 伤害 + 缓慢 III/失明 II 各 10s，冷却 2s。 */
    public static final DeferredItem<Item> COOKED_RICE_BALL =
            ITEMS.registerItem("cooked_rice_ball", properties -> new CookedRiceBallItem(properties.stacksTo(64)));

    /** 米米图腾：主手/副手持有受致命伤时免死一次，效果同原版不死图腾。 */
    public static final DeferredItem<Item> MIMI_TOTEM =
            ITEMS.registerItem("mimi_totem", properties -> new MimiTotemItem(properties));

    // ============================ Curios 饰品（圣遗物 / 神遗之书） ============================

    /** 圣遗物槽位 ID（槽位数据见 data/mimiworld/curios/slots/sacred_relic.json）。 */
    private static final String SACRED_RELIC = "sacred_relic";

    /** 注册一件「圣遗物」饰品：堆叠 1，属性修饰符与蓝色说明行由配置提供。 */
    private static DeferredItem<Item> relic(String name, List<SacredRelicItem.Modifier> modifiers,
                                            String... tooltipKeys) {
        return ITEMS.registerItem(name, properties -> new SacredRelicItem(
                properties.stacksTo(1), SACRED_RELIC, modifiers, List.of(tooltipKeys)));
    }

    /** 构造一条 Curios 属性修饰符配置。 */
    private static SacredRelicItem.Modifier relicModifier(Holder<Attribute> attribute, double amount,
                                                          AttributeModifier.Operation operation, String suffix) {
        return new SacredRelicItem.Modifier(attribute, amount, operation, suffix);
    }

    /** 启航之书：神遗之书槽位，抗性/跳跃/再生 + 时运 3/抢夺 3 + 生命与攻击百分比。 */
    public static final DeferredItem<Item> BOOK_OF_DEPARTURE = ITEMS.registerItem(
            "book_of_departure", properties -> new BookOfDepartureItem(properties.stacksTo(1)));

    /** 火眼金睛：持续夜视，清除失明/黑暗。 */
    public static final DeferredItem<Item> FIERY_GOLDEN_EYES = ITEMS.registerItem(
            "fiery_golden_eyes", properties -> new FieryGoldenEyesItem(properties.stacksTo(1)));

    /** 如意：攻击伤害 +5、攻击距离 +5。 */
    public static final DeferredItem<Item> RUYI = relic("ruyi", List.of(
            relicModifier(Attributes.ATTACK_DAMAGE, 5.0D, AttributeModifier.Operation.ADD_VALUE, "ruyi_attack_damage"),
            relicModifier(Attributes.ENTITY_INTERACTION_RANGE, 5.0D,
                    AttributeModifier.Operation.ADD_VALUE, "ruyi_attack_reach")),
            "tooltip.mimiworld.ruyi.attack_reach",
            "tooltip.mimiworld.ruyi.attack_damage");

    /** 虎王之蔑：受到伤害拆分结算（事件实现）。 */
    public static final DeferredItem<Item> TIGER_KING_DISDAIN = relic("tiger_king_disdain", List.of(),
            "tooltip.mimiworld.tiger_king_disdain.installments",
            "tooltip.mimiworld.tiger_king_disdain.interval");

    /** 坚韧虎皮：护甲与攻击力动态换算（事件实现）。 */
    public static final DeferredItem<Item> TOUGH_TIGER_HIDE = relic("tough_tiger_hide", List.of(),
            "tooltip.mimiworld.tough_tiger_hide.armor",
            "tooltip.mimiworld.tough_tiger_hide.attack",
            "tooltip.mimiworld.tough_tiger_hide.penalty");

    /** 月有圆缺：攻击力 +10（暴击与增伤由事件实现）。 */
    public static final DeferredItem<Item> MOON_PHASES = relic("moon_phases", List.of(
            relicModifier(Attributes.ATTACK_DAMAGE, 10.0D,
                    AttributeModifier.Operation.ADD_VALUE, "moon_phases_attack_damage")),
            "tooltip.mimiworld.moon_phases.crit",
            "tooltip.mimiworld.moon_phases.attack",
            "tooltip.mimiworld.moon_phases.vulnerability",
            "tooltip.mimiworld.moon_phases.monster");

    /** 猩红竖眼：发光与传送（事件实现）。 */
    public static final DeferredItem<Item> CRIMSON_VERTICAL_EYE = relic("crimson_vertical_eye", List.of(),
            "tooltip.mimiworld.crimson_vertical_eye.glow",
            "tooltip.mimiworld.crimson_vertical_eye.teleport",
            "tooltip.mimiworld.crimson_vertical_eye.wall");

    /** 超越之号角：护甲 +10、最大生命 +10、攻速 +30%。 */
    public static final DeferredItem<Item> TRANSCENDENCE_HORN = relic("transcendence_horn", List.of(
            relicModifier(Attributes.ARMOR, 10.0D, AttributeModifier.Operation.ADD_VALUE,
                    "transcendence_horn_armor"),
            relicModifier(Attributes.MAX_HEALTH, 10.0D, AttributeModifier.Operation.ADD_VALUE,
                    "transcendence_horn_max_health"),
            relicModifier(Attributes.ATTACK_SPEED, 0.30D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                    "transcendence_horn_attack_speed")),
            "tooltip.mimiworld.transcendence_horn.armor",
            "tooltip.mimiworld.transcendence_horn.max_health",
            "tooltip.mimiworld.transcendence_horn.attack_speed");

    /** 上古毛皮：抢夺 +2（易怒由事件实现）。 */
    public static final DeferredItem<Item> ANCIENT_PELT = ITEMS.registerItem(
            "ancient_pelt", properties -> new AncientPeltItem(properties.stacksTo(1)));

    /** 蛇皮驺尾：移动速度 +0.1、护甲 +5。 */
    public static final DeferredItem<Item> SNAKESKIN_TAIL = relic("snakeskin_tail", List.of(
            relicModifier(Attributes.MOVEMENT_SPEED, 0.1D, AttributeModifier.Operation.ADD_VALUE,
                    "snakeskin_tail_movement_speed"),
            relicModifier(Attributes.ARMOR, 5.0D, AttributeModifier.Operation.ADD_VALUE,
                    "snakeskin_tail_armor")),
            "tooltip.mimiworld.snakeskin_tail.movement_speed",
            "tooltip.mimiworld.snakeskin_tail.armor");

    /** 暗灵毒牙：攻击力 +25%（暗毒由事件实现）。 */
    public static final DeferredItem<Item> DARK_SPIRIT_FANG = relic("dark_spirit_fang", List.of(
            relicModifier(Attributes.ATTACK_DAMAGE, 0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                    "dark_spirit_fang_attack_damage")),
            "tooltip.mimiworld.dark_spirit_fang.attack",
            "tooltip.mimiworld.dark_spirit_fang.poison");

    /** 天青轮盘：伤害翻倍/减半（事件实现）。 */
    public static final DeferredItem<Item> AZURE_WHEEL = relic("azure_wheel", List.of(),
            "tooltip.mimiworld.azure_wheel.double",
            "tooltip.mimiworld.azure_wheel.half");

    /** 夔牛之力：攻速 +1、攻击力 +20%、护甲 +50%（无视无敌帧由事件实现）。 */
    public static final DeferredItem<Item> KUI_NIU_POWER = relic("kui_niu_power", List.of(
            relicModifier(Attributes.ATTACK_SPEED, 1.0D, AttributeModifier.Operation.ADD_VALUE,
                    "kui_niu_power_attack_speed"),
            relicModifier(Attributes.ATTACK_DAMAGE, 0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                    "kui_niu_power_attack_damage"),
            relicModifier(Attributes.ARMOR, 0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                    "kui_niu_power_armor")),
            "tooltip.mimiworld.kui_niu_power.no_invulnerability",
            "tooltip.mimiworld.kui_niu_power.attack_speed",
            "tooltip.mimiworld.kui_niu_power.attack_damage",
            "tooltip.mimiworld.kui_niu_power.armor");

    /** 领域展开：范围内敌对生物减甲（事件实现）。 */
    public static final DeferredItem<Item> DOMAIN_EXPANSION = relic("domain_expansion", List.of(),
            "tooltip.mimiworld.domain_expansion.armor");

    /** 山海恩泽：跳跃提升与下落增伤（事件实现）。 */
    public static final DeferredItem<Item> SHANHAI_GRACE = relic("shanhai_grace", List.of(),
            "tooltip.mimiworld.shanhai_grace.jump",
            "tooltip.mimiworld.shanhai_grace.fall");

    /** 逆鳞龙胆：抗性提升 IV、护甲 +100%（伤害上限由事件实现）。 */
    public static final DeferredItem<Item> REVERSE_SCALE_GALL = ITEMS.registerItem(
            "reverse_scale_gall", properties -> new ReverseScaleGallItem(properties.stacksTo(1)));

    /** 米米圣盾：最大生命 +5（额外生命由事件实现）。 */
    public static final DeferredItem<Item> MIMI_SHIELD = relic("mimi_shield", List.of(
            relicModifier(Attributes.MAX_HEALTH, 5.0D, AttributeModifier.Operation.ADD_VALUE,
                    "mimi_shield_max_health")),
            "tooltip.mimiworld.mimi_shield.health",
            "tooltip.mimiworld.mimi_shield.heal",
            "tooltip.mimiworld.mimi_shield.threshold");

    /** 双倍快乐：主副手同物品时伤害 ×2（事件实现）。 */
    public static final DeferredItem<Item> DOUBLE_HAPPINESS = relic("double_happiness", List.of(),
            "tooltip.mimiworld.double_happiness.double");

    /** 多多益善：按周围怪物数量增伤（事件实现）。 */
    public static final DeferredItem<Item> THE_MORE_THE_MERRIER = relic("the_more_the_merrier", List.of(),
            "tooltip.mimiworld.the_more_the_merrier.count",
            "tooltip.mimiworld.the_more_the_merrier.cap");

    // ============================ 生物蛋与鱼桶（第 4 步 · 第一小批） ============================

    /**
     * 生物蛋：26.3 起由数据组件 ENTITY_DATA 指定实体，颜色/外观由物品模型与贴图承担。
     *
     * <p>实体类型必须延迟到物品注册回调里取：物品注册事件早于所有注册表就绪，
     * 类加载阶段直接 {@code ModEntities.X.get()} 会抛「unbound value」。</p>
     */
    private static DeferredItem<Item> spawnEgg(String name, Supplier<EntityType<? extends Mob>> type) {
        return ITEMS.registerItem(name + "_spawn_egg", properties -> new SpawnEggItem(properties
                .component(DataComponents.ENTITY_DATA, TypedEntityData.of(type.get(), new CompoundTag()))));
    }

    public static final DeferredItem<Item> MIMI_SHEEP_SPAWN_EGG =
            spawnEgg("mimi_sheep", ModEntities.MIMI_SHEEP::get);
    public static final DeferredItem<Item> ZHUOMI_CHICKEN_SPAWN_EGG =
            spawnEgg("zhuomi_chicken", ModEntities.ZHUOMI_CHICKEN::get);
    public static final DeferredItem<Item> MIMOUNIU_SPAWN_EGG =
            spawnEgg("mimoo_cow", ModEntities.MIMOUNIU::get);
    public static final DeferredItem<Item> MIHENG_PIG_SPAWN_EGG =
            spawnEgg("miheng_pig", ModEntities.MIHENG_PIG::get);
    public static final DeferredItem<Item> TANMI_FISH_SPAWN_EGG =
            spawnEgg("tanmi_fish", ModEntities.TANMI_FISH::get);
    public static final DeferredItem<Item> PENMI_FISH_SPAWN_EGG =
            spawnEgg("penmi_fish", ModEntities.PENMI_FISH::get);
    public static final DeferredItem<Item> RICE_GOLEM_SPAWN_EGG =
            spawnEgg("rice_golem", ModEntities.RICE_GOLEM::get);

    // —— 近战亡灵（第 4 步 · 第二小批）生物蛋 ——
    public static final DeferredItem<Item> MIMI_SKELETON_SPAWN_EGG =
            spawnEgg("mimi_skeleton", ModEntities.MIMI_SKELETON::get);
    public static final DeferredItem<Item> CARVED_RICE_SKELETON_SPAWN_EGG =
            spawnEgg("carved_rice_skeleton", ModEntities.CARVED_RICE_SKELETON::get);
    public static final DeferredItem<Item> HUMI_CORPSE_SPAWN_EGG =
            spawnEgg("humi_corpse", ModEntities.HUMI_CORPSE::get);
    public static final DeferredItem<Item> MIHENG_PIG_CORPSE_SPAWN_EGG =
            spawnEgg("miheng_pig_corpse", ModEntities.MIHENG_PIG_CORPSE::get);

    // —— 特殊行为敌对（第 4 步 · 第三小批）生物蛋 ——
    public static final DeferredItem<Item> HAOMI_WOLF_SPAWN_EGG =
            spawnEgg("haomi_wolf", ModEntities.HAOMI_WOLF::get);
    public static final DeferredItem<Item> PAMI_CRAWLER_SPAWN_EGG =
            spawnEgg("pami_crawler", ModEntities.PAMI_CRAWLER::get);
    public static final DeferredItem<Item> STICKY_RICE_SPIDER_SPAWN_EGG =
            spawnEgg("sticky_rice_spider", ModEntities.STICKY_RICE_SPIDER::get);
    public static final DeferredItem<Item> RICE_WITCH_SPAWN_EGG =
            spawnEgg("rice_witch", ModEntities.RICE_WITCH::get);
    public static final DeferredItem<Item> MILAI_HORSE_SPAWN_EGG =
            spawnEgg("milai_horse", ModEntities.MILAI_HORSE::get);

    // —— 地面精英（第 4 步 · 第四小批）生物蛋 ——
    public static final DeferredItem<Item> RICE_STEALER_SPAWN_EGG =
            spawnEgg("rice_stealer", ModEntities.RICE_STEALER::get);
    public static final DeferredItem<Item> BLIGHTED_THORN_FLOWER_SPAWN_EGG =
            spawnEgg("blighted_thorn_flower", ModEntities.BLIGHTED_THORN_FLOWER::get);
    public static final DeferredItem<Item> RICE_MIRAGE_SPAWN_EGG =
            spawnEgg("rice_mirage", ModEntities.RICE_MIRAGE::get);
    public static final DeferredItem<Item> LAZY_ELF_SPAWN_EGG =
            spawnEgg("lazy_elf", ModEntities.LAZY_ELF::get);

    // —— 飞行精英（第 4 步 · 第五小批）生物蛋 ——
    public static final DeferredItem<Item> RICE_SOUL_SPAWN_EGG =
            spawnEgg("rice_soul", ModEntities.RICE_SOUL::get);
    public static final DeferredItem<Item> HUALING_RICE_SPAWN_EGG =
            spawnEgg("hualing_rice", ModEntities.HUALING_RICE::get);

    // —— 飞行/陆空怪（第 4 步 · 第六小批）生物蛋 ——
    public static final DeferredItem<Item> EXPLOSIVE_RICE_SPIRIT_SPAWN_EGG =
            spawnEgg("explosive_rice_spirit", ModEntities.EXPLOSIVE_RICE_SPIRIT::get);
    public static final DeferredItem<Item> RICE_WOLF_VULTURE_SPAWN_EGG =
            spawnEgg("rice_wolf_vulture", ModEntities.RICE_WOLF_VULTURE::get);

    /** 贪米鱼桶：用水桶装取/倒出贪米鱼。 */
    public static final DeferredItem<Item> TANMI_FISH_BUCKET = ITEMS.registerItem("tanmi_fish_bucket",
            properties -> new MobBucketItem(ModEntities.TANMI_FISH.get(), Fluids.WATER,
                    SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1)));

    /** 喷米鱼桶：用水桶装取/倒出喷米鱼。 */
    public static final DeferredItem<Item> PENMI_FISH_BUCKET = ITEMS.registerItem("penmi_fish_bucket",
            properties -> new MobBucketItem(ModEntities.PENMI_FISH.get(), Fluids.WATER,
                    SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1)));

    /** 米桶：绑定米汤流体，行为同原版水桶。 */
    public static final DeferredItem<Item> RICE_BUCKET = ITEMS.registerItem("rice_bucket",
            properties -> new BucketItem(com.mimiworld.mimiworld.fluid.ModFluids.RICE_SOUP.get(),
                    properties.stacksTo(1)));

    /** 饭桶：绑定饭汤流体，行为同原版水桶。 */
    public static final DeferredItem<Item> RICE_PAIL = ITEMS.registerItem("rice_pail",
            properties -> new BucketItem(com.mimiworld.mimiworld.fluid.ModFluids.RICE_BROTH.get(),
                    properties.stacksTo(1)));

    /** 米米之眼：激活米米传送器，也是米米维度内的返程钥匙。 */
    public static final DeferredItem<Item> MIMI_EYE = ITEMS.registerItem("mimi_eye",
            properties -> new com.mimiworld.mimiworld.item.MimiEyeItem(properties.stacksTo(64)));

    /** 米米传送器方块物品。 */
    public static final DeferredItem<BlockItem> MIMI_TELEPORTER = blockItem(ModBlocks.MIMI_TELEPORTER);

    /** 猴王金箍棒（Boss 武器）：攻击 +15 / 攻速 -3.2 / 触及 +3，命中附加虚弱 I。 */
    public static final DeferredItem<Item> MONKEY_KING_RUYI_STAFF = ITEMS.registerItem("monkey_king_ruyi_staff",
            properties -> new MonkeyKingRuyiStaffItem(MonkeyKingRuyiStaffItem.createProperties(properties)));

    /** 深染残堕天翼（鞘翅类物品）。 */
    public static final DeferredItem<Item> DEEP_DYED_REMNANT_HEAVEN_WING =
            ITEMS.registerItem("deep_dyed_remnant_heaven_wing",
                    properties -> new Item(properties.stacksTo(1).durability(432)));

    // ---- Boss / 残堕界精英生物蛋 ----
    public static final DeferredItem<Item> MIMI_MONKEY_KING_SPAWN_EGG =
            spawnEgg("mimi_monkey_king", ModEntities.MIMI_MONKEY_KING::get);
    public static final DeferredItem<Item> RICE_CRYSTAL_TIGER_KING_SPAWN_EGG =
            spawnEgg("rice_crystal_tiger_king", ModEntities.RICE_CRYSTAL_TIGER_KING::get);
    public static final DeferredItem<Item> SUPER_CRYSTAL_COW_KING_SPAWN_EGG =
            spawnEgg("super_crystal_cow_king", ModEntities.SUPER_CRYSTAL_COW_KING::get);
    public static final DeferredItem<Item> HEAVY_CRYSTAL_RABBIT_KING_SPAWN_EGG =
            spawnEgg("heavy_crystal_rabbit_king", ModEntities.HEAVY_CRYSTAL_RABBIT_KING::get);
    public static final DeferredItem<Item> COMPOUND_AZURE_PYTHON_SPAWN_EGG =
            spawnEgg("compound_azure_python", ModEntities.COMPOUND_AZURE_PYTHON::get);
    public static final DeferredItem<Item> CLEANSING_LONGEVITY_KUI_SPAWN_EGG =
            spawnEgg("cleansing_longevity_kui", ModEntities.CLEANSING_LONGEVITY_KUI::get);
    public static final DeferredItem<Item> LAZIEST_FINAL_DRAGON_SPAWN_EGG =
            spawnEgg("laziest_final_dragon", ModEntities.LAZIEST_FINAL_DRAGON::get);
    public static final DeferredItem<Item> DIBAZONGZUILONG_SPAWN_EGG =
            spawnEgg("dibazongzuilong", ModEntities.DIBAZONGZUILONG::get);
    public static final DeferredItem<Item> FALLEN_RICE_PHOENIX_SPAWN_EGG =
            spawnEgg("fallen_rice_phoenix", ModEntities.FALLEN_RICE_PHOENIX::get);
    public static final DeferredItem<Item> JIETIAN_RICE_SPAWN_EGG =
            spawnEgg("jietian_rice", ModEntities.JIETIAN_RICE::get);
    public static final DeferredItem<Item> CHAOTIC_VOID_TOUCHER_SPAWN_EGG =
            spawnEgg("chaotic_void_toucher", ModEntities.CHAOTIC_VOID_TOUCHER::get);
    public static final DeferredItem<Item> MOMI_ENTRANT_SPAWN_EGG =
            spawnEgg("momi_entrant", ModEntities.MOMI_ENTRANT::get);
    public static final DeferredItem<Item> PALE_MERMAID_SPAWN_EGG =
            spawnEgg("pale_mermaid", ModEntities.PALE_MERMAID::get);
    public static final DeferredItem<Item> WANGMI_WOUNDED_MERMAID_SPAWN_EGG =
            spawnEgg("wangmi_wounded_mermaid", ModEntities.WANGMI_WOUNDED_MERMAID::get);

    // ============================ 方块物品 ============================

    /** 注册普通方块物品（放置后的方块见 ModBlocks）。 */
    private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> block) {
        return ITEMS.registerSimpleBlockItem(block);
    }

    /** 米原木（作燃料可烧 3 个物品） */
    public static final DeferredItem<BlockItem> RICE_LOG = ITEMS.registerItem("rice_log",
            properties -> new BlockItem(ModBlocks.RICE_LOG.get(),
                    properties.component(DataComponents.COOKING_FUEL, ModFuels.WOOD)));
    /** 米木板（作燃料可烧 3 个物品） */
    public static final DeferredItem<BlockItem> RICE_PLANKS = ITEMS.registerItem("rice_planks",
            properties -> new BlockItem(ModBlocks.RICE_PLANKS.get(),
                    properties.component(DataComponents.COOKING_FUEL, ModFuels.WOOD)));
    /** 米楼梯 */
    public static final DeferredItem<BlockItem> RICE_STAIRS = blockItem(ModBlocks.RICE_STAIRS);
    /** 米台阶 */
    public static final DeferredItem<BlockItem> RICE_SLAB = blockItem(ModBlocks.RICE_SLAB);
    /** 米栅栏 */
    public static final DeferredItem<BlockItem> RICE_FENCE = blockItem(ModBlocks.RICE_FENCE);
    /** 米树叶（作燃料可烧 0.5 个物品） */
    public static final DeferredItem<BlockItem> RICE_LEAVES = ITEMS.registerItem("rice_leaves",
            properties -> new BlockItem(ModBlocks.RICE_LEAVES.get(),
                    properties.component(DataComponents.COOKING_FUEL, ModFuels.LEAVES)));
    /** 米树苗 */
    public static final DeferredItem<BlockItem> RICE_SAPLING = blockItem(ModBlocks.RICE_SAPLING);
    /** 硬化米 */
    public static final DeferredItem<BlockItem> HARDENED_RICE = blockItem(ModBlocks.HARDENED_RICE);
    /** 深层硬化米 */
    public static final DeferredItem<BlockItem> DEEPSLATE_HARDENED_RICE =
            blockItem(ModBlocks.DEEPSLATE_HARDENED_RICE);
    /** 基米 */
    public static final DeferredItem<BlockItem> RICE_BEDROCK = blockItem(ModBlocks.RICE_BEDROCK);
    /** 结晶米矿石 */
    public static final DeferredItem<BlockItem> CRYSTALLIZED_RICE_ORE =
            blockItem(ModBlocks.CRYSTALLIZED_RICE_ORE);
    /** 重结晶米矿石 */
    public static final DeferredItem<BlockItem> RECRYSTALLIZED_RICE_ORE =
            blockItem(ModBlocks.RECRYSTALLIZED_RICE_ORE);
    /** 可燃米矿石 */
    public static final DeferredItem<BlockItem> COMBUSTIBLE_RICE_ORE =
            blockItem(ModBlocks.COMBUSTIBLE_RICE_ORE);
    /** 米矿石 */
    public static final DeferredItem<BlockItem> RICE_ORE = blockItem(ModBlocks.RICE_ORE);
    /** 超懒精华矿 */
    public static final DeferredItem<BlockItem> SUPER_LAZY_ESSENCE_ORE =
            blockItem(ModBlocks.SUPER_LAZY_ESSENCE_ORE);
    /** 泥米 */
    public static final DeferredItem<BlockItem> MUD_RICE = blockItem(ModBlocks.MUD_RICE);
    /** 粗米块 */
    public static final DeferredItem<BlockItem> COARSE_RICE_BLOCK = blockItem(ModBlocks.COARSE_RICE_BLOCK);
    /** 米块 */
    public static final DeferredItem<BlockItem> RICE_BLOCK = blockItem(ModBlocks.RICE_BLOCK);
    /** 米石块 */
    public static final DeferredItem<BlockItem> RICE_STONE_BLOCK = blockItem(ModBlocks.RICE_STONE_BLOCK);
    /** 米羊羊毛 */
    public static final DeferredItem<BlockItem> MIMI_WOOL = blockItem(ModBlocks.MIMI_WOOL);
    /** 搅碎器 */
    public static final DeferredItem<BlockItem> CRUSHER = blockItem(ModBlocks.CRUSHER);
    /** 造米台 */
    public static final DeferredItem<BlockItem> RICE_MAKER = blockItem(ModBlocks.RICE_MAKER);
    /** 锤米台 */
    public static final DeferredItem<BlockItem> RICE_POUNDING_TABLE = blockItem(ModBlocks.RICE_POUNDING_TABLE);
    /** 沉淀台 */
    public static final DeferredItem<BlockItem> PRECIPITATION_TABLE = blockItem(ModBlocks.PRECIPITATION_TABLE);
    /** 凝锭台 */
    public static final DeferredItem<BlockItem> INGOT_CONDENSATION_TABLE =
            blockItem(ModBlocks.INGOT_CONDENSATION_TABLE);
    /** 大煮锅 */
    public static final DeferredItem<BlockItem> LARGE_COOKING_POT = blockItem(ModBlocks.LARGE_COOKING_POT);
    /** 巨无霸大大大煮锅 */
    public static final DeferredItem<BlockItem> JUMBO_MEGA_COOKING_POT = blockItem(ModBlocks.JUMBO_MEGA_COOKING_POT);
    /** 铁锅 */
    public static final DeferredItem<BlockItem> IRON_WOK = blockItem(ModBlocks.IRON_WOK);
    /** 烤箱 */
    public static final DeferredItem<BlockItem> OVEN = blockItem(ModBlocks.OVEN);
    /** 搅拌器 */
    public static final DeferredItem<BlockItem> MIXER = blockItem(ModBlocks.MIXER);
    /** 炉灶 */
    public static final DeferredItem<BlockItem> STOVE = blockItem(ModBlocks.STOVE);
    /** 堕落台 */
    public static final DeferredItem<BlockItem> FALLEN_ALTAR = blockItem(ModBlocks.FALLEN_ALTAR);
    /** 残堕深渊岩 */
    public static final DeferredItem<BlockItem> REMNANT_FALLEN_ABYSS_ROCK =
            blockItem(ModBlocks.REMNANT_FALLEN_ABYSS_ROCK);
    /** 残堕基岩 */
    public static final DeferredItem<BlockItem> REMNANT_FALLEN_BEDROCK =
            blockItem(ModBlocks.REMNANT_FALLEN_BEDROCK);
    /** 磨制残堕深渊岩 */
    public static final DeferredItem<BlockItem> POLISHED_REMNANT_FALLEN_ABYSS_ROCK =
            blockItem(ModBlocks.POLISHED_REMNANT_FALLEN_ABYSS_ROCK);

    /** 米草：可放置的草类植物，同时可食用（饱食度 1，反胃 I 5s + 缓慢 II 5s，食用耗时 0.5 秒）。 */
    public static final DeferredItem<BlockItem> RICE_GRASS =
            ITEMS.registerItem("rice_grass", properties -> new RiceGrassItem(
                    ModBlocks.RICE_GRASS.get(),
                    properties.stacksTo(64)
                            .food(effectFoodProps(1), consumable(0.5F,
                                    statusEffect(MobEffects.NAUSEA, 100, 0, 1.0F),
                                    statusEffect(MobEffects.SLOWNESS, 100, 1, 1.0F)))));

    // ============================ Boss 头颅摆件与堕落十字架 ============================

    /** 米米猴头 */
    public static final DeferredItem<BlockItem> MIMI_MONKEY_HEAD = blockItem(ModBlocks.MIMI_MONKEY_HEAD);
    /** 饭晶虎王头 */
    public static final DeferredItem<BlockItem> RICE_CRYSTAL_TIGER_KING_HEAD =
            blockItem(ModBlocks.RICE_CRYSTAL_TIGER_KING_HEAD);
    /** 超晶牛头 */
    public static final DeferredItem<BlockItem> SUPER_CRYSTAL_OX_HEAD = blockItem(ModBlocks.SUPER_CRYSTAL_OX_HEAD);
    /** 重晶兔王头 */
    public static final DeferredItem<BlockItem> HEAVY_CRYSTAL_RABBIT_KING_HEAD =
            blockItem(ModBlocks.HEAVY_CRYSTAL_RABBIT_KING_HEAD);
    /** 复晶天青蟒头 */
    public static final DeferredItem<BlockItem> COMPOUND_CRYSTAL_AZURE_PYTHON_HEAD =
            blockItem(ModBlocks.COMPOUND_CRYSTAL_AZURE_PYTHON_HEAD);
    /** 清洁长命夔头 */
    public static final DeferredItem<BlockItem> PURIFIED_LONGEVITY_KUI_HEAD =
            blockItem(ModBlocks.PURIFIED_LONGEVITY_KUI_HEAD);
    /** 最懒终末龙头 */
    public static final DeferredItem<BlockItem> LAZIEST_END_DRAGON_HEAD =
            blockItem(ModBlocks.LAZIEST_END_DRAGON_HEAD);
    /** 第八宗罪龙头 */
    public static final DeferredItem<BlockItem> DIBAZONGZUI_HEAD = blockItem(ModBlocks.DIBAZONGZUI_HEAD);
    /** 堕落十字架 */
    public static final DeferredItem<BlockItem> FALLEN_CROSS = blockItem(ModBlocks.FALLEN_CROSS);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

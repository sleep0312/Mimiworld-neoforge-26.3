package com.mimiworld.mimiworld;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.List;
import java.util.Map;

/**
 * 米米世界自定义防具材料。
 *
 * <p>26.3 的 {@link ArmorMaterial} 为 record：防御按 {@link ArmorType} 映射，
 * 修复材料用物品标签，贴图通过 {@code equipment} 资源键引用。
 * 装备层贴图位于 {@code textures/entity/equipment/humanoid(_leggings)/<name>.png}。</p>
 *
 * <p>防御/耐久/韧性/击退抗性/附魔能力在物品注册时从 {@code [armor.*]} 配置读取
 * （record 的值会被烘焙进物品组件，改动后需重启游戏）。</p>
 */
public final class ModArmorMaterials {

    private ModArmorMaterials() {
    }

    private static TagKey<Item> repairTag(String name) {
        return ItemTags.create(Identifier.fromNamespaceAndPath(MimiWorld.MODID, name));
    }

    /** 装备资源键：使用 mimiworld 命名空间，对应 assets/mimiworld/equipment/&lt;name&gt;.json。 */
    private static ResourceKey<EquipmentAsset> equipmentId(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID,
                Identifier.fromNamespaceAndPath(MimiWorld.MODID, name));
    }

    /** 防御映射：参数顺序为 靴子/护腿/胸甲/头盔。 */
    private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        return Map.of(
                ArmorType.BOOTS, boots,
                ArmorType.LEGGINGS, leggings,
                ArmorType.CHESTPLATE, chestplate,
                ArmorType.HELMET, helmet);
    }

    /**
     * 按配置构造材料；配置项缺失时回退到 {@link Defaults} 的原始数值。
     *
     * <p>配置使用 STARTUP 类型，在注册配置时即已读盘，因此物品注册阶段能拿到用户数值。</p>
     */
    private static ArmorMaterial create(String id, Holder<SoundEvent> equipSound, TagKey<Item> repairIngredient,
                                        ResourceKey<EquipmentAsset> assetId) {
        Defaults.Entry raw = Defaults.get(id);
        int durability = raw.durability();
        int enchantmentValue = raw.enchantmentValue();
        float toughness = raw.toughness();
        float knockbackResistance = raw.knockbackResistance();
        int[] defenseForSlot = raw.defense().clone();
        ModConfig.ArmorStats stats = ModConfig.armor(id);
        if (stats != null) {
            durability = ModConfig.read(stats.durabilityMultiplier());
            enchantmentValue = ModConfig.read(stats.enchantmentValue());
            toughness = (float) (double) ModConfig.read(stats.toughness());
            knockbackResistance = (float) (double) ModConfig.read(stats.knockbackResistance());
            for (int slot = 0; slot < defenseForSlot.length; slot++) {
                defenseForSlot[slot] = ModConfig.read(stats.defense().get(slot));
            }
        }
        return new ArmorMaterial(durability,
                defense(defenseForSlot[0], defenseForSlot[1], defenseForSlot[2], defenseForSlot[3]),
                enchantmentValue, equipSound, toughness, knockbackResistance, repairIngredient, assetId);
    }

    /** 饭晶合金套：下界合金同级防御、韧性 2、击退抗性 0.05（修复饭晶合金锭）。 */
    public static final ArmorMaterial RICE_CRYSTAL_ALLOY = create("rice_crystal_alloy",
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            repairTag("repairs_rice_crystal_alloy_armor"),
            equipmentId("rice_crystal_alloy"));

    /** 超重结晶套：24 点总防御、韧性 3.5、击退抗性 0.08（修复超重结晶）。 */
    public static final ArmorMaterial SUPER_HEAVY_CRYSTAL = create("super_heavy_crystal",
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            repairTag("repairs_super_heavy_crystal_armor"),
            equipmentId("super_heavy_crystal"));

    /** 复合米饭套：22 点总防御、韧性 3、击退抗性 0.05、高附魔（修复复合米饭锭）。 */
    public static final ArmorMaterial COMPOUND_RICE_MEAL = create("compound_rice_meal",
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            repairTag("repairs_compound_rice_meal_armor"),
            equipmentId("compound_rice_meal"));

    /** 清洁复合套：24 点总防御、韧性 4、击退抗性 0.1（修复清洁复合锭）。 */
    public static final ArmorMaterial CLEANSING_COMPOUND = create("cleansing_compound",
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            repairTag("repairs_cleansing_compound_armor"),
            equipmentId("cleansing_compound"));

    /** 超懒合金套：26 点总防御、韧性 5、击退抗性 0.15（修复超懒合金锭）。 */
    public static final ArmorMaterial SUPER_LAZY_ALLOY = create("super_lazy_alloy",
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            repairTag("repairs_super_lazy_alloy_armor"),
            equipmentId("super_lazy_alloy"));

    /** 米米神翼（仅胸甲）：胸甲防御 8、韧性 3（修复米米幻翼膜）。 */
    public static final ArmorMaterial MIMI_WINGS = create("mimi_wings",
            SoundEvents.ARMOR_EQUIP_ELYTRA,
            repairTag("repairs_mimi_wings_armor"),
            equipmentId("mimi_wings"));

    /** 超懒神翼（仅胸甲）：10 点胸甲防御、韧性 5、击退抗性 0.15（修复超懒合金锭）。 */
    public static final ArmorMaterial SUPER_LAZY_WINGS = create("super_lazy_wings",
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            repairTag("repairs_super_lazy_alloy_armor"),
            equipmentId("super_lazy_wings"));

    /** 最懒天堕套：24 点总防御、韧性 4.5、击退抗性 0.12（修复堕落之心）。 */
    public static final ArmorMaterial HEAVENFALL = create("heavenfall",
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            repairTag("repairs_heavenfall_armor"),
            equipmentId("heavenfall"));

    /** 残堕宗罪套：29 点总防御、韧性 5.5、击退抗性 0.18（修复终末龙逆鳞）。 */
    public static final ArmorMaterial REMNANT_FALLEN_CARDINAL_SIN = create("remnant_fallen_cardinal_sin",
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            repairTag("repairs_remnant_fallen_armor"),
            equipmentId("remnant_fallen_cardinal_sin"));

    /**
     * 各套防具的原始数值（配置默认值来源）。
     *
     * <p>单独放在嵌套类里，使 {@link ModConfig} 构建 {@code [armor.*]} 默认值时不会初始化外层类；
     * 否则外层材料会在配置读盘之前就被构造，从而拿不到用户配置。</p>
     */
    static final class Defaults {

        private Defaults() {
        }

        /** 防御数组顺序固定为 靴/腿/胸/头。 */
        record Entry(String id, int durability, int[] defense, int enchantmentValue,
                     float toughness, float knockbackResistance) {
        }

        static final List<Entry> ALL = List.of(
                new Entry("rice_crystal_alloy", 37, new int[]{3, 6, 8, 3}, 14, 2.0F, 0.05F),
                new Entry("super_heavy_crystal", 40, new int[]{4, 7, 9, 4}, 12, 3.5F, 0.08F),
                new Entry("compound_rice_meal", 40, new int[]{4, 6, 8, 4}, 18, 3.0F, 0.05F),
                new Entry("cleansing_compound", 45, new int[]{4, 7, 9, 4}, 20, 4.0F, 0.1F),
                new Entry("super_lazy_alloy", 55, new int[]{4, 8, 10, 4}, 22, 5.0F, 0.15F),
                new Entry("mimi_wings", 37, new int[]{0, 0, 8, 0}, 18, 3.0F, 0.05F),
                new Entry("super_lazy_wings", 55, new int[]{0, 0, 10, 0}, 22, 5.0F, 0.15F),
                new Entry("heavenfall", 50, new int[]{4, 7, 9, 4}, 20, 4.5F, 0.12F),
                new Entry("remnant_fallen_cardinal_sin", 60, new int[]{5, 8, 11, 5}, 24, 5.5F, 0.18F));

        static Entry get(String id) {
            for (Entry entry : ALL) {
                if (entry.id().equals(id)) {
                    return entry;
                }
            }
            throw new IllegalArgumentException("Unknown armor material: " + id);
        }
    }
}

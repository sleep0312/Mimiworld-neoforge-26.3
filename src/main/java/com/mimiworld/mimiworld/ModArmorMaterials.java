package com.mimiworld.mimiworld;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * 米米世界自定义防具材料。
 *
 * <p>26.3 的 {@link ArmorMaterial} 为 record：防御按 {@link ArmorType} 映射，
 * 修复材料用物品标签，贴图通过 {@code equipment} 资源键引用。
 * 装备层贴图位于 {@code textures/entity/equipment/humanoid(_leggings)/<name>.png}。</p>
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

    /** 饭晶合金套：下界合金同级防御、韧性 2、击退抗性 0.05（修复饭晶合金锭）。 */
    public static final ArmorMaterial RICE_CRYSTAL_ALLOY = new ArmorMaterial(
            37, defense(3, 6, 8, 3), 14, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.05F,
            repairTag("repairs_rice_crystal_alloy_armor"),
            equipmentId("rice_crystal_alloy"));

    /** 超重结晶套：24 点总防御、韧性 3.5、击退抗性 0.08（修复超重结晶）。 */
    public static final ArmorMaterial SUPER_HEAVY_CRYSTAL = new ArmorMaterial(
            40, defense(4, 7, 9, 4), 12, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.08F,
            repairTag("repairs_super_heavy_crystal_armor"),
            equipmentId("super_heavy_crystal"));

    /** 复合米饭套：22 点总防御、韧性 3、击退抗性 0.05、高附魔（修复复合米饭锭）。 */
    public static final ArmorMaterial COMPOUND_RICE_MEAL = new ArmorMaterial(
            40, defense(4, 6, 8, 4), 18, SoundEvents.ARMOR_EQUIP_DIAMOND, 3.0F, 0.05F,
            repairTag("repairs_compound_rice_meal_armor"),
            equipmentId("compound_rice_meal"));

    /** 清洁复合套：24 点总防御、韧性 4、击退抗性 0.1（修复清洁复合锭）。 */
    public static final ArmorMaterial CLEANSING_COMPOUND = new ArmorMaterial(
            45, defense(4, 7, 9, 4), 20, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.1F,
            repairTag("repairs_cleansing_compound_armor"),
            equipmentId("cleansing_compound"));

    /** 超懒合金套：26 点总防御、韧性 5、击退抗性 0.15（修复超懒合金锭）。 */
    public static final ArmorMaterial SUPER_LAZY_ALLOY = new ArmorMaterial(
            55, defense(4, 8, 10, 4), 22, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 0.15F,
            repairTag("repairs_super_lazy_alloy_armor"),
            equipmentId("super_lazy_alloy"));

    /** 米米神翼（仅胸甲）：胸甲防御 8、韧性 3（修复米米幻翼膜）。 */
    public static final ArmorMaterial MIMI_WINGS = new ArmorMaterial(
            37, defense(0, 0, 8, 0), 18, SoundEvents.ARMOR_EQUIP_ELYTRA, 3.0F, 0.05F,
            repairTag("repairs_mimi_wings_armor"),
            equipmentId("mimi_wings"));

    /** 超懒神翼（仅胸甲）：10 点胸甲防御、韧性 5、击退抗性 0.15（修复超懒合金锭）。 */
    public static final ArmorMaterial SUPER_LAZY_WINGS = new ArmorMaterial(
            55, defense(0, 0, 10, 0), 22, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 0.15F,
            repairTag("repairs_super_lazy_alloy_armor"),
            equipmentId("super_lazy_wings"));

    /** 最懒天堕套：24 点总防御、韧性 4.5、击退抗性 0.12（修复堕落之心）。 */
    public static final ArmorMaterial HEAVENFALL = new ArmorMaterial(
            50, defense(4, 7, 9, 4), 20, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.5F, 0.12F,
            repairTag("repairs_heavenfall_armor"),
            equipmentId("heavenfall"));

    /** 残堕宗罪套：29 点总防御、韧性 5.5、击退抗性 0.18（修复终末龙逆鳞）。 */
    public static final ArmorMaterial REMNANT_FALLEN_CARDINAL_SIN = new ArmorMaterial(
            60, defense(5, 8, 11, 5), 24, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.5F, 0.18F,
            repairTag("repairs_remnant_fallen_armor"),
            equipmentId("remnant_fallen_cardinal_sin"));
}

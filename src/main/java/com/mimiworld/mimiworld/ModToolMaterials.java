package com.mimiworld.mimiworld;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * 米米世界工具/武器材料等级。
 *
 * <p>等级链（从低到高）：饭晶 &lt; 饭晶合金锭 &lt; 超重结晶 &lt; 复合米饭锭
 * &lt; 清洁复合锭 &lt; 超懒合金锭 &lt; 最懒天堕 &lt; 残堕。</p>
 *
 * <p>26.3 已移除 {@code Tier}，改用 {@link ToolMaterial}：
 * 挖掘等级由「不可挖掘方块标签」表达，修复材料由物品标签表达。</p>
 */
public final class ModToolMaterials {

    private ModToolMaterials() {
    }

    private static TagKey<Item> repairTag(String name) {
        return ItemTags.create(Identifier.fromNamespaceAndPath(MimiWorld.MODID, name));
    }

    /** 饭晶：钻石级（耐久 1561 / 速度 7 / 伤害 3），修复材料结晶米。 */
    public static final ToolMaterial RICE_CRYSTAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 1561, 7.0F, 3.0F, 14,
            repairTag("repairs_rice_crystal_tool"));

    /** 饭晶合金：下界合金级强化的均衡档（耐久 2200 / 速度 8 / 伤害 4.5），修复材料饭晶合金锭。 */
    public static final ToolMaterial RICE_CRYSTAL_ALLOY = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2200, 8.0F, 4.5F, 15,
            repairTag("repairs_rice_crystal_alloy_tool"));

    /** 超重结晶：慢速重武器路线（耐久 2400 / 速度 8 / 伤害 6），修复材料超重结晶。 */
    public static final ToolMaterial SUPER_HEAVY_CRYSTAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2400, 8.0F, 6.0F, 12,
            repairTag("repairs_super_heavy_crystal_tool"));

    /** 复合米饭：均衡高伤档（耐久 2600 / 速度 9 / 伤害 6.5），修复材料复合米饭锭。 */
    public static final ToolMaterial COMPOUND_RICE_MEAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2600, 9.0F, 6.5F, 18,
            repairTag("repairs_compound_rice_meal_tool"));

    /** 清洁复合：更高一档（耐久 2800 / 速度 9.5 / 伤害 7），修复材料清洁复合锭。 */
    public static final ToolMaterial CLEANSING_COMPOUND = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2800, 9.5F, 7.0F, 20,
            repairTag("repairs_cleansing_compound_tool"));

    /** 超懒合金：终极材料（耐久 3200 / 速度 10 / 伤害 8），修复材料超懒合金锭。 */
    public static final ToolMaterial SUPER_LAZY_ALLOY = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3200, 10.0F, 8.0F, 22,
            repairTag("repairs_super_lazy_alloy_tool"));

    /** 最懒天堕：超懒合金之上的传说级（耐久 3800 / 速度 11 / 伤害 8.5），修复材料堕落之心。 */
    public static final ToolMaterial LAZIEST_HEAVENFALL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3800, 11.0F, 8.5F, 24,
            repairTag("repairs_laziest_heavenfall_tool"));

    /** 残堕：龙系之上的终末级（耐久 4200 / 速度 12 / 伤害 10），修复材料终末龙逆鳞。 */
    public static final ToolMaterial REMNANT_FALLEN = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4200, 12.0F, 10.0F, 26,
            repairTag("repairs_remnant_fallen_tool"));
}

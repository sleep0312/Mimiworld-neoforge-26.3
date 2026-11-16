package com.mimiworld.mimiworld;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 米米世界工具/武器材料等级。
 *
 * <p>等级链（从低到高）：饭晶 &lt; 饭晶合金锭 &lt; 超重结晶 &lt; 复合米饭锭
 * &lt; 清洁复合锭 &lt; 超懒合金锭 &lt; 最懒天堕 &lt; 残堕。</p>
 *
 * <p>26.3 已移除 {@code Tier}，改用 {@link ToolMaterial}：
 * 挖掘等级由「不可挖掘方块标签」表达，修复材料由物品标签表达。</p>
 *
 * <p>耐久/挖掘速度/攻击加成/附魔能力在物品注册时从 {@code [weapons.*]} 配置读取
 * （record 的值会被烘焙进物品组件，改动后需重启游戏）。</p>
 */
public final class ModToolMaterials {

    private ModToolMaterials() {
    }

    private static TagKey<Item> repairTag(String name) {
        return ItemTags.create(Identifier.fromNamespaceAndPath(MimiWorld.MODID, name));
    }

    /** 按配置构造材料；配置项缺失时回退到 {@link Defaults} 的原始数值。 */
    private static ToolMaterial create(String id, TagKey<Block> incorrectBlocksForDrops, TagKey<Item> repairItems) {
        Defaults.Entry raw = Defaults.get(id);
        int durability = raw.durability();
        float speed = raw.speed();
        float attackDamageBonus = raw.attackDamageBonus();
        int enchantmentValue = raw.enchantmentValue();
        ModConfig.WeaponStats stats = ModConfig.weapon(id);
        if (stats != null) {
            durability = ModConfig.read(stats.uses());
            speed = (float) (double) ModConfig.read(stats.miningSpeed());
            attackDamageBonus = (float) (double) ModConfig.read(stats.attackDamageBonus());
            enchantmentValue = ModConfig.read(stats.enchantmentValue());
        }
        return new ToolMaterial(incorrectBlocksForDrops, durability, speed, attackDamageBonus,
                enchantmentValue, repairItems);
    }

    /** 饭晶：钻石级（耐久 1561 / 速度 7 / 伤害 3），修复材料结晶米。 */
    public static final ToolMaterial RICE_CRYSTAL = create("rice_crystal",
            BlockTags.INCORRECT_FOR_IRON_TOOL, repairTag("repairs_rice_crystal_tool"));

    /** 饭晶合金：下界合金级强化的均衡档（耐久 2200 / 速度 8 / 伤害 4.5），修复材料饭晶合金锭。 */
    public static final ToolMaterial RICE_CRYSTAL_ALLOY = create("rice_crystal_alloy",
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, repairTag("repairs_rice_crystal_alloy_tool"));

    /** 超重结晶：慢速重武器路线（耐久 2400 / 速度 8 / 伤害 6），修复材料超重结晶。 */
    public static final ToolMaterial SUPER_HEAVY_CRYSTAL = create("super_heavy_crystal",
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, repairTag("repairs_super_heavy_crystal_tool"));

    /** 复合米饭：均衡高伤档（耐久 2600 / 速度 9 / 伤害 6.5），修复材料复合米饭锭。 */
    public static final ToolMaterial COMPOUND_RICE_MEAL = create("compound_rice_meal",
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, repairTag("repairs_compound_rice_meal_tool"));

    /** 清洁复合：更高一档（耐久 2800 / 速度 9.5 / 伤害 7），修复材料清洁复合锭。 */
    public static final ToolMaterial CLEANSING_COMPOUND = create("cleansing_compound",
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, repairTag("repairs_cleansing_compound_tool"));

    /** 超懒合金：终极材料（耐久 3200 / 速度 10 / 伤害 8），修复材料超懒合金锭。 */
    public static final ToolMaterial SUPER_LAZY_ALLOY = create("super_lazy_alloy",
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, repairTag("repairs_super_lazy_alloy_tool"));

    /** 最懒天堕：超懒合金之上的传说级（耐久 3800 / 速度 11 / 伤害 8.5），修复材料堕落之心。 */
    public static final ToolMaterial LAZIEST_HEAVENFALL = create("laziest_heavenfall",
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, repairTag("repairs_laziest_heavenfall_tool"));

    /** 残堕：龙系之上的终末级（耐久 4200 / 速度 12 / 伤害 10），修复材料终末龙逆鳞。 */
    public static final ToolMaterial REMNANT_FALLEN = create("remnant_fallen",
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, repairTag("repairs_remnant_fallen_tool"));

    /** 猴王如意棍：Boss 专属（耐久 2000 / 速度 9 / 伤害加成 0 / 附魔 22），修复材料米米猴头。 */
    public static final ToolMaterial MONKEY_KING_RUYI_STAFF = create("monkey_king_ruyi_staff",
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, repairTag("repairs_monkey_king_staff"));

    /**
     * 各材料等级的原始数值（配置默认值来源）。
     *
     * <p>单独放在嵌套类里，使 {@link ModConfig} 构建 {@code [weapons.*]} 默认值时不会初始化外层类；
     * 否则外层材料会在配置读盘之前就被构造，从而拿不到用户配置。</p>
     */
    static final class Defaults {

        private Defaults() {
        }

        record Entry(String id, int durability, float speed, float attackDamageBonus, int enchantmentValue) {
        }

        static final List<Entry> ALL = List.of(
                new Entry("rice_crystal", 1561, 7.0F, 3.0F, 14),
                new Entry("rice_crystal_alloy", 2200, 8.0F, 4.5F, 15),
                new Entry("super_heavy_crystal", 2400, 8.0F, 6.0F, 12),
                new Entry("compound_rice_meal", 2600, 9.0F, 6.5F, 18),
                new Entry("cleansing_compound", 2800, 9.5F, 7.0F, 20),
                new Entry("super_lazy_alloy", 3200, 10.0F, 8.0F, 22),
                new Entry("laziest_heavenfall", 3800, 11.0F, 8.5F, 24),
                new Entry("remnant_fallen", 4200, 12.0F, 10.0F, 26),
                new Entry("monkey_king_ruyi_staff", 2000, 9.0F, 0.0F, 22));

        static Entry get(String id) {
            for (Entry entry : ALL) {
                if (entry.id().equals(id)) {
                    return entry;
                }
            }
            throw new IllegalArgumentException("Unknown tool material: " + id);
        }
    }
}

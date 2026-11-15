package com.mimiworld.mimiworld.item;

import com.mimiworld.mimiworld.entity.MimiArrowEntity;
import com.mimiworld.mimiworld.entity.ModEntities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * 米米世界箭矢物品：可被弓／弩发射的 {@link ArrowItem}。
 *
 * <p>发射时生成 {@link MimiArrowEntity}，把本箭的固定伤害与中毒等级写入实体；
 * 命中结算与附加效果见实体类。</p>
 */
public class MimiArrowItem extends ArrowItem {

    private final double hitDamage;
    private final int poisonLevel;

    /**
     * @param hitDamage   命中固定伤害
     * @param poisonLevel 命中附中毒等级，0 表示不附加中毒
     */
    public MimiArrowItem(double hitDamage, int poisonLevel, Properties properties) {
        super(properties);
        this.hitDamage = hitDamage;
        this.poisonLevel = poisonLevel;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter, ItemStack weapon) {
        return new MimiArrowEntity(ModEntities.MIMI_ARROW.get(), shooter, level,
                this.hitDamage, this.poisonLevel, this);
    }

    /** 兼容「无限」附魔：只要弓上有无限附魔即可无限。 */
    @Override
    public boolean isInfinite(ItemStack stack, ItemStack bow, LivingEntity shooter) {
        return shooter.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.INFINITY)
                .map(holder -> bow.getEnchantments().getLevel(holder) > 0)
                .orElse(false);
    }
}

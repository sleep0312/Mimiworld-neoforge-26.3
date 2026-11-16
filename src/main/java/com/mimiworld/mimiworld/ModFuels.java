package com.mimiworld.mimiworld;

import net.minecraft.world.item.component.CookingFuel;

/**
 * 米米世界燃料数值（烹饪燃料）。
 *
 * <p>26.3 已移除 {@code FurnaceFuelBurnTimeEvent}，燃料改为物品数据组件
 * {@code minecraft:cooking_fuel}。米原木/米木板可烧 3 个物品 = 600 tick，
 * 米木棍 300 tick，米树叶 100 tick，可燃米 6400 tick。</p>
 */
public final class ModFuels {

    private ModFuels() {
    }

    /** 600 tick：米原木、米木板。 */
    public static final CookingFuel WOOD = fuel(600);
    /** 300 tick：米木棍。 */
    public static final CookingFuel STICK = fuel(300);
    /** 100 tick：米树叶。 */
    public static final CookingFuel LEAVES = fuel(100);
    /** 6400 tick：可燃米。 */
    public static final CookingFuel COMBUSTIBLE_RICE = fuel(6400);

    private static CookingFuel fuel(int burnTime) {
        return new CookingFuel(
                new net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant(burnTime),
                new net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat.Constant(1.0F));
    }
}

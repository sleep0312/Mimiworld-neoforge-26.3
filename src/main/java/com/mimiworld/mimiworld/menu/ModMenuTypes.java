package com.mimiworld.mimiworld.menu;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 米米世界菜单类型（容器 GUI）注册（机器批次：搅碎器、搅拌器）。
 */
public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MimiWorld.MODID);

    /** 搅碎器菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER_MENU =
            MENUS.register("crusher", () -> IMenuTypeExtension.create(CrusherMenu::new));

    /** 搅拌器菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<MixerMenu>> MIXER_MENU =
            MENUS.register("mixer", () -> IMenuTypeExtension.create(MixerMenu::new));

    /** 造米台菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<RiceMakerMenu>> RICE_MAKER_MENU =
            MENUS.register("rice_maker", () -> IMenuTypeExtension.create(RiceMakerMenu::new));

    /** 锤米台菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<RicePoundingTableMenu>> RICE_POUNDING_TABLE_MENU =
            MENUS.register("rice_pounding_table", () -> IMenuTypeExtension.create(RicePoundingTableMenu::new));

    /** 沉淀台菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<PrecipitationTableMenu>> PRECIPITATION_TABLE_MENU =
            MENUS.register("precipitation_table", () -> IMenuTypeExtension.create(PrecipitationTableMenu::new));

    /** 凝锭台菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<IngotCondensationTableMenu>> INGOT_CONDENSATION_TABLE_MENU =
            MENUS.register("ingot_condensation_table",
                    () -> IMenuTypeExtension.create(IngotCondensationTableMenu::new));

    /** 大煮锅菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<LargeCookingPotMenu>> LARGE_COOKING_POT_MENU =
            MENUS.register("large_cooking_pot", () -> IMenuTypeExtension.create(LargeCookingPotMenu::new));

    /** 巨无霸大大大煮锅菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<JumboMegaCookingPotMenu>> JUMBO_MEGA_COOKING_POT_MENU =
            MENUS.register("jumbo_mega_cooking_pot",
                    () -> IMenuTypeExtension.create(JumboMegaCookingPotMenu::new));

    /** 铁锅菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<IronWokMenu>> IRON_WOK_MENU =
            MENUS.register("iron_wok", () -> IMenuTypeExtension.create(IronWokMenu::new));

    /** 烤箱菜单。 */
    public static final DeferredHolder<MenuType<?>, MenuType<OvenMenu>> OVEN_MENU =
            MENUS.register("oven", () -> IMenuTypeExtension.create(OvenMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}

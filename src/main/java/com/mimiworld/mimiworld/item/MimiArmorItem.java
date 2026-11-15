package com.mimiworld.mimiworld.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.mimiworld.mimiworld.client.renderer.ArmorRenderers;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 米米世界防具单件（GeckoLib 3D 渲染）。
 *
 * <p>26.3 已移除 {@code ArmorItem}，防具本体由 {@code Item.Properties.humanoidArmor} 提供；
 * 本类承载「单件最大生命值、穿戴持续药水效果、是否给予创造飞行」等附加数据，
 * 并通过 {@link GeoItem} 提供立体盔甲渲染与翅膀/尾巴动画。</p>
 */
public class MimiArmorItem extends Item implements GeoItem {

    private static final RawAnimation LAZIEST_WINGS =
            RawAnimation.begin().thenLoop("animation.laziest_heavenfall_armor.wing_flap");
    private static final RawAnimation MIMI_WINGS =
            RawAnimation.begin().thenLoop("animation.mimi_divine_wings.idle");
    private static final RawAnimation SUPER_LAZY_WINGS =
            RawAnimation.begin().thenLoop("animation.super_lazy_wings_chestplate.idle");
    private static final RawAnimation REMNANT_TAIL =
            RawAnimation.begin().thenLoop("animation.walk2");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final float maxHealthBonus;
    private final Map<Holder<MobEffect>, Integer> wornEffects;
    private final boolean grantsFlight;
    private final String modelKey;

    public MimiArmorItem(Properties properties, float maxHealthBonus,
                         Map<Holder<MobEffect>, Integer> wornEffects, boolean grantsFlight,
                         String modelKey) {
        super(properties);
        this.maxHealthBonus = maxHealthBonus;
        this.wornEffects = Map.copyOf(wornEffects);
        this.grantsFlight = grantsFlight;
        this.modelKey = modelKey;
    }

    /** 单件提供的最大生命值加成。 */
    public float getMaxHealthBonus() {
        return maxHealthBonus;
    }

    /** 穿戴期间的持续药水效果（效果 -> 放大器，1 级 = 0）。 */
    public Map<Holder<MobEffect>, Integer> getWornEffects() {
        return wornEffects;
    }

    /** 穿戴时是否给予创造飞行能力。 */
    public boolean grantsFlight() {
        return grantsFlight;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation animation = switch (modelKey) {
            case "laziest_heavenfall_armor" -> LAZIEST_WINGS;
            case "mimi_divine_wings" -> MIMI_WINGS;
            case "super_lazy_wings_chestplate" -> SUPER_LAZY_WINGS;
            case "remnant_fallen_cardinal_sin_armor" -> REMNANT_TAIL;
            default -> null;
        };
        if (animation != null) {
            controllers.add(new AnimationController<MimiArmorItem>("main", 0,
                    state -> state.setAndContinue(animation)));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?, ?> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (renderer == null) {
                    renderer = ArmorRenderers.create(modelKey);
                }
                return renderer;
            }
        });
    }
}

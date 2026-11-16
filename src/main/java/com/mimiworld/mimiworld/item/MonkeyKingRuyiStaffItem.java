package com.mimiworld.mimiworld.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.mimiworld.mimiworld.ModToolMaterials;
import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.client.model.MonkeyKingRuyiStaffModel;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.function.Consumer;

/**
 * 猴王如意棍（金箍棒）：米米猴王掉落的 3D 近战武器。
 *
 * <p>战斗数值：攻击伤害 16（基础 15 + 材料加成 0 + 玩家手部 1）、
 * 攻速 0.8（修饰 -3.2）、实体触及 6 格（在玩家基础 3 格上 +3）；
 * 命中目标附加虚弱 I（2 秒 = 40 tick）。</p>
 *
 * <p>3D 渲染由 GeckoLib 的 special item renderer 提供，各视角变换读取
 * {@code models/item/monkey_king_ruyi_staff.json} 的 display 节点；无动画。</p>
 */
public class MonkeyKingRuyiStaffItem extends Item implements GeoItem {

    /** 基础攻击伤害（材料攻击加成为 0，最终 15 + 0 + 玩家基础 1 = 16）。 */
    private static final float ATTACK_DAMAGE = 15.0F;
    /** 攻速修饰（玩家基础 4 → 显示 0.8）。 */
    private static final float ATTACK_SPEED = -3.2F;
    /** 实体触及加成（3 格 → 6 格）。 */
    private static final double ENTITY_REACH = 3.0D;
    private static final Identifier ENTITY_REACH_ID =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "monkey_king_ruyi_staff_reach");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MonkeyKingRuyiStaffItem(Properties properties) {
        super(properties);
    }

    /**
     * 构造物品属性：先套用剑的标准组件（耐久、附魔、挖掘规则、武器组件），
     * 再用同一套攻击/攻速修饰叠加实体触及。
     */
    public static Properties createProperties(Properties properties) {
        ToolMaterial material = ModToolMaterials.MONKEY_KING_RUYI_STAFF;
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                                ATTACK_DAMAGE + material.attackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                                ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(ENTITY_REACH_ID,
                                ENTITY_REACH, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
        return properties.stacksTo(1)
                .sword(material, ATTACK_DAMAGE, ATTACK_SPEED)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 近战命中：虚弱 I，持续 2 秒（40 tick）
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<?> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(new MonkeyKingRuyiStaffModel());
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 静态模型，无动画控制器
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

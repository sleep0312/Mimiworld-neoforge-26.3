package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.ModItems;
import com.mimiworld.mimiworld.effect.ModEffects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 药水效果与饰品状态相关事件处理器。
 *
 * <ul>
 *   <li>残堕化效果赋予：第一至第八 Boss 生成/载入时永久获得对应等级。</li>
 *   <li>火眼金睛：拦截新的失明/黑暗效果施加。</li>
 *   <li>坚韧虎皮：按总护甲动态维护护甲/攻击力属性修饰符。</li>
 *   <li>暗毒：一切生命恢复削减到 25%。</li>
 *   <li>山海恩泽：佩戴期间持续刷新「跳跃提升 V」。</li>
 * </ul>
 */
public final class ModEffectEvents {

    private ModEffectEvents() {
    }

    /** 坚韧虎皮：基础护甲加成、按总护甲换算攻击/扣减护甲的比例。 */
    private static final double TOUGH_HIDE_ARMOR_BONUS = 12.0D;
    private static final double TOUGH_HIDE_RATIO = 0.1D;
    private static final Identifier TOUGH_HIDE_ARMOR_ID =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "tough_tiger_hide.armor");
    private static final Identifier TOUGH_HIDE_PENALTY_ID =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "tough_tiger_hide.penalty");
    private static final Identifier TOUGH_HIDE_ATTACK_ID =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "tough_tiger_hide.attack");

    private static final Map<UUID, ToughHideState> TOUGH_HIDE_STATES = new HashMap<>();

    private static final class ToughHideState {
        private double baseArmor;
        private double bonus;
    }

    /** 山海恩泽：跳跃提升 V 对应 amplifier 4；刷新时长 8 秒，剩余 ≤3 秒时重置。 */
    private static final int SHANHAI_JUMP_AMPLIFIER = 4;
    private static final int SHANHAI_EFFECT_DURATION = 160;
    private static final int SHANHAI_REFRESH_REMAINING = 60;

    /** 第一至第八 Boss 生成或随存档载入时，永久获得对应等级的残堕化。 */
    @SubscribeEvent
    public static void onBossJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        int tier = bossTier(living);
        if (tier <= 0) {
            return;
        }
        living.addEffect(new MobEffectInstance(ModEffects.REMNANT_CORRUPTION,
                MobEffectInstance.INFINITE_DURATION, tier - 1, false, false, true));
    }

    private static int bossTier(LivingEntity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null || !MimiWorld.MODID.equals(id.getNamespace())) {
            return 0;
        }
        return switch (id.getPath()) {
            case "mimi_monkey_king" -> 1;
            case "rice_crystal_tiger_king" -> 2;
            case "super_crystal_cow_king" -> 3;
            case "heavy_crystal_rabbit_king" -> 4;
            case "compound_azure_python" -> 5;
            case "cleansing_longevity_kui" -> 6;
            case "laziest_final_dragon" -> 7;
            case "dibazongzuilong" -> 8;
            default -> 0;
        };
    }

    /** 火眼金睛：佩戴于圣遗物槽位时拦截失明/黑暗。 */
    @SubscribeEvent
    public static void onBlindnessOrDarkness(MobEffectEvent.Applicable event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) {
            return;
        }
        Holder<MobEffect> effect = instance.getEffect();
        if (effect.value() != MobEffects.BLINDNESS.value() && effect.value() != MobEffects.DARKNESS.value()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        boolean equipped = CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.isEquipped(ModItems.FIERY_GOLDEN_EYES.get()))
                .orElse(false);
        if (equipped) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** 坚韧虎皮：护甲 +12，并以（基础护甲 + 12）为总护甲换算攻击力与护甲罚值。 */
    @SubscribeEvent
    public static void onToughTigerHideTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        boolean equipped = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(ModItems.TOUGH_TIGER_HIDE.get()))
                .orElse(false);
        if (!equipped) {
            if (TOUGH_HIDE_STATES.remove(id) != null) {
                removeToughHideModifiers(player);
            }
            return;
        }
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (armor == null || attack == null) {
            return;
        }
        ToughHideState state = TOUGH_HIDE_STATES.get(id);
        double baseArmor = state == null
                ? armor.getValue()
                : armor.getValue() - (TOUGH_HIDE_ARMOR_BONUS - state.bonus);
        double bonus = Math.max(0.0D, (baseArmor + TOUGH_HIDE_ARMOR_BONUS) * TOUGH_HIDE_RATIO);
        if (state != null
                && Math.abs(baseArmor - state.baseArmor) < 1.0E-4D
                && Math.abs(bonus - state.bonus) < 1.0E-4D) {
            return;
        }
        removeToughHideModifiers(player);
        armor.addTransientModifier(new AttributeModifier(TOUGH_HIDE_ARMOR_ID,
                TOUGH_HIDE_ARMOR_BONUS, AttributeModifier.Operation.ADD_VALUE));
        if (bonus > 0.0D) {
            armor.addTransientModifier(new AttributeModifier(TOUGH_HIDE_PENALTY_ID,
                    -bonus, AttributeModifier.Operation.ADD_VALUE));
            attack.addTransientModifier(new AttributeModifier(TOUGH_HIDE_ATTACK_ID,
                    bonus, AttributeModifier.Operation.ADD_VALUE));
        }
        ToughHideState newState = new ToughHideState();
        newState.baseArmor = baseArmor;
        newState.bonus = bonus;
        TOUGH_HIDE_STATES.put(id, newState);
    }

    @SubscribeEvent
    public static void onToughHideLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TOUGH_HIDE_STATES.remove(event.getEntity().getUUID());
    }

    private static void removeToughHideModifiers(Player player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(TOUGH_HIDE_ARMOR_ID);
            armor.removeModifier(TOUGH_HIDE_PENALTY_ID);
        }
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(TOUGH_HIDE_ATTACK_ID);
        }
    }

    /** 暗毒：一切生命恢复只保留 25%。 */
    @SubscribeEvent
    public static void onDarkPoisonHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity.hasEffect(ModEffects.DARK_POISON)) {
            event.setAmount(event.getAmount() * 0.25F);
        }
    }

    /** 山海恩泽：佩戴期间持续维持「跳跃提升 V」。 */
    @SubscribeEvent
    public static void onShanhaiGraceTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        boolean equipped = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(ModItems.SHANHAI_GRACE.get()))
                .orElse(false);
        if (!equipped) {
            MobEffectInstance jump = player.getEffect(MobEffects.JUMP_BOOST);
            if (jump != null && jump.getAmplifier() == SHANHAI_JUMP_AMPLIFIER
                    && jump.getDuration() <= SHANHAI_EFFECT_DURATION) {
                player.removeEffect(MobEffects.JUMP_BOOST);
            }
            return;
        }
        MobEffectInstance jump = player.getEffect(MobEffects.JUMP_BOOST);
        if (jump == null || jump.getAmplifier() < SHANHAI_JUMP_AMPLIFIER
                || (jump.getAmplifier() == SHANHAI_JUMP_AMPLIFIER
                && jump.getDuration() <= SHANHAI_REFRESH_REMAINING)) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, SHANHAI_EFFECT_DURATION,
                    SHANHAI_JUMP_AMPLIFIER, false, false, true));
        }
    }
}

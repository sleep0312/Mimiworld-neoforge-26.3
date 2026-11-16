package com.mimiworld.mimiworld.event;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.ModConfig;
import com.mimiworld.mimiworld.ModItems;
import com.mimiworld.mimiworld.effect.ModEffects;
import com.mimiworld.mimiworld.enchantment.ModEnchantments;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 伤害结算总览（NeoForge 26.3）。本模组所有「修改伤害数值」的监听器集中在这里。
 *
 * <p><b>事件时机：</b></p>
 * <ul>
 *   <li>{@link LivingIncomingDamageEvent} 在无敌帧检查之后、任何减伤之前触发，
 *       {@code getAmount()} 是护甲前数值（对应旧版 LivingHurtEvent）。</li>
 *   <li>{@link LivingDamageEvent.Pre} 在盔甲/附魔/抗性结算之后、扣血之前触发，
 *       {@code getNewDamage()} 是即将扣除的数值。</li>
 *   <li>{@link LivingDamageEvent.Post} 在扣血与吸收结算之后触发，用于米米圣盾。</li>
 * </ul>
 *
 * <p>完整伤害链条：加法区 × (1 + 乘法区)；虎王之蔑在 LOWEST 优先级拆成 5 次延迟结算；
 * 阶段 2 处理泣血回血、逆鳞龙胆封顶与米米圣盾。</p>
 */
public final class DamagePipeline {

    /** 月有圆缺：暴击率、受伤倍率、附近怪物额外伤害的概率与倍率。 */
    private static final float MOON_CRIT_CHANCE = 0.30F;
    private static final float MOON_DAMAGE_TAKEN_MULTIPLIER = 1.25F;
    private static final double MOON_MONSTER_RADIUS = 5.0D;
    private static final float MOON_MONSTER_CHANCE = 0.20F;
    private static final float MOON_MONSTER_EXTRA_MULTIPLIER = 1.5F;

    /** 易怒：受伤后的窗口时长、触发概率、每级增伤与等级上限。 */
    private static final int IRRITABLE_WINDOW_TICKS = 20;
    private static final float IRRITABLE_CHANCE = 0.25F;
    private static final float IRRITABLE_PER_LEVEL = 0.05F;
    private static final int IRRITABLE_MAX_LEVEL = 5;

    private static final Map<UUID, Long> IRRITABLE_WINDOWS = new HashMap<>();

    /** 暗灵毒牙：命中时附加的暗毒持续时间（3 秒）。 */
    private static final int DARK_POISON_DURATION = 60;

    /** 上古毛皮：易怒最高 5 级、10 秒未触发降级、效果刷新时长 8 秒。 */
    private static final int PELT_MAX_LEVEL = 5;
    private static final long PELT_DECAY_TICKS = 200L;
    private static final int PELT_EFFECT_DURATION = 160;
    private static final int PELT_REFRESH_REMAINING = 60;

    private static final Map<UUID, PeltRageState> PELT_STATES = new HashMap<>();

    private static final class PeltRageState {
        private int level = 1;
        private long lastProcTick;

        private PeltRageState(long now) {
            this.lastProcTick = now;
        }
    }

    /** 多多益善：水平判定半径、每只怪物增伤比例、计入上限。 */
    private static final double MERRIER_RADIUS = 5.0D;
    private static final float MERRIER_PER_MONSTER = 0.1F;
    private static final int MERRIER_MAX_MONSTERS = 20;

    /** 米米圣盾：每次受伤的额外生命值比例与累计上限。 */
    private static final float MIMI_SHIELD_GAIN_RATIO = 0.1F;
    private static final float MIMI_SHIELD_CAP_RATIO = 0.4F;

    /** 虎王之蔑延迟伤害的自定义伤害类型。 */
    private static final Identifier DELAYED_DAMAGE_ID =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "tiger_king_disdain");
    private static final ResourceKey<DamageType> DELAYED_DAMAGE_KEY =
            ResourceKey.create(Registries.DAMAGE_TYPE, DELAYED_DAMAGE_ID);
    private static final int DELAY_INSTALLMENTS = 5;
    private static final int DELAY_INTERVAL_TICKS = 20;
    private static final float DELAY_FRACTION = 0.2F;

    private static final Map<UUID, List<DelayedDamage>> DELAYED_DAMAGES = new HashMap<>();

    private static final class DelayedDamage {
        private final DamageSource source;
        private final float amount;
        private int remaining = DELAY_INSTALLMENTS;
        private int ticksUntilNext = DELAY_INTERVAL_TICKS;

        private DelayedDamage(DamageSource source, float amount) {
            this.source = source;
            this.amount = amount;
        }
    }

    private DamagePipeline() {
    }

    // ------------------------------------------------------------------
    // 阶段 1：护甲前倍率
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        float additive = event.getAmount();
        float percent = 0.0F;
        percent += configBonusPercent(event);
        percent += crimsonCandlelightBonusPercent(event);
        percent += pythonConstrictionBonusPercent(event);
        percent += remnantCorruptionBonusPercent(event);
        percent += moonPhasesBonusPercent(event);
        percent += irritableBonusPercent(event);
        percent += azureWheelBonusPercent(event);
        percent += doubleHappinessBonusPercent(event);
        percent += moreTheMerrierBonusPercent(event);
        percent += shanhaiGraceBonusPercent(event);
        percent += swiftStrikeBonusPercent(event);
        percent = Math.max(percent, -1.0F);
        event.setAmount(additive * (1.0F + percent));
        applyDarkSpiritFang(event);
    }

    /** 公式 1：全局攻防倍率（只影响本模组生物）。 */
    private static float configBonusPercent(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return 0.0F;
        }
        double percent = 0.0D;
        Entity attacker = resolveAttacker(event.getSource());
        if (attacker instanceof LivingEntity livingAttacker && ModConfigEvents.isModMob(livingAttacker)) {
            percent += ModConfig.mobDamageMultiplier() - 1.0D;
        }
        if (ModConfigEvents.isModMob(target)) {
            percent += ModConfig.mobDamageTakenMultiplier() - 1.0D;
        }
        return (float) percent;
    }

    /** 公式 2：腥红烛照 —— 攻击方每级 +100%。 */
    private static float crimsonCandlelightBonusPercent(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || isDelayedDamage(event.getSource())) {
            return 0.0F;
        }
        Entity attacker = resolveAttacker(event.getSource());
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return 0.0F;
        }
        MobEffectInstance instance = livingAttacker.getEffect(ModEffects.CRIMSON_CANDLELIGHT);
        return instance == null ? 0.0F : instance.getAmplifier() + 1.0F;
    }

    /** 公式 3：蟒蛇缠绕 —— 攻击方持有该效果攻击复晶天青蟒时 +0.5。 */
    private static float pythonConstrictionBonusPercent(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || !isEntityType(target, "compound_azure_python")) {
            return 0.0F;
        }
        if (isDelayedDamage(event.getSource())) {
            return 0.0F;
        }
        Entity attacker = resolveAttacker(event.getSource());
        if (attacker instanceof LivingEntity livingAttacker
                && livingAttacker.hasEffect(ModEffects.PYTHON_CONSTRICTION)) {
            return 0.5F;
        }
        return 0.0F;
    }

    /** 公式 4：残堕化 —— 受击方每级 −3%。 */
    private static float remnantCorruptionBonusPercent(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || isDelayedDamage(event.getSource())) {
            return 0.0F;
        }
        MobEffectInstance instance = target.getEffect(ModEffects.REMNANT_CORRUPTION);
        return instance == null ? 0.0F : -0.03F * (instance.getAmplifier() + 1);
    }

    /** 公式 5：月有圆缺（受击）—— +25%，5 格内怪物来源另有 20% 概率再 +50%。 */
    private static float moonPhasesBonusPercent(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return 0.0F;
        }
        if (isDelayedDamage(event.getSource()) || !hasCurio(player, ModItems.MOON_PHASES.get())) {
            return 0.0F;
        }
        float percent = MOON_DAMAGE_TAKEN_MULTIPLIER - 1.0F;
        if (resolveAttacker(event.getSource()) instanceof Monster monster
                && monster.distanceTo(player) <= MOON_MONSTER_RADIUS
                && player.getRandom().nextFloat() < MOON_MONSTER_CHANCE) {
            percent += MOON_MONSTER_EXTRA_MULTIPLIER - 1.0F;
        }
        return percent;
    }

    /** 公式 6：易怒 —— 受击方维护 1 秒窗口；攻击方 25% 概率 +5%×等级。 */
    private static float irritableBonusPercent(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return 0.0F;
        }
        DamageSource source = event.getSource();
        if (isDelayedDamage(source)) {
            return 0.0F;
        }
        long now = target.level().getGameTime();

        if (target.hasEffect(ModEffects.IRRITABLE)) {
            IRRITABLE_WINDOWS.put(target.getUUID(), now + IRRITABLE_WINDOW_TICKS);
        }

        Entity attacker = resolveAttacker(source);
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return 0.0F;
        }
        MobEffectInstance instance = livingAttacker.getEffect(ModEffects.IRRITABLE);
        if (instance == null) {
            return 0.0F;
        }
        Long expiry = IRRITABLE_WINDOWS.get(livingAttacker.getUUID());
        if (expiry == null || now > expiry) {
            IRRITABLE_WINDOWS.remove(livingAttacker.getUUID());
            return 0.0F;
        }
        if (livingAttacker.getRandom().nextFloat() < IRRITABLE_CHANCE) {
            int level = Math.min(instance.getAmplifier() + 1, IRRITABLE_MAX_LEVEL);
            escalateAncientPelt(livingAttacker, now);
            return IRRITABLE_PER_LEVEL * level;
        }
        return 0.0F;
    }

    /** 公式 7：天青轮盘 —— 50% +100%，50% −50%。 */
    private static float azureWheelBonusPercent(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || isDelayedDamage(event.getSource())) {
            return 0.0F;
        }
        Entity attacker = resolveAttacker(event.getSource());
        if (!(attacker instanceof Player player) || player.level().isClientSide()) {
            return 0.0F;
        }
        if (!hasCurio(player, ModItems.AZURE_WHEEL.get())) {
            return 0.0F;
        }
        return player.getRandom().nextBoolean() ? 1.0F : -0.5F;
    }

    /** 公式 8：双倍快乐 —— 主副手同种物品时 +100%。 */
    private static float doubleHappinessBonusPercent(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || isDelayedDamage(event.getSource())) {
            return 0.0F;
        }
        Entity attacker = resolveAttacker(event.getSource());
        if (!(attacker instanceof Player player) || player.level().isClientSide()) {
            return 0.0F;
        }
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.isEmpty() || offHand.isEmpty() || !ItemStack.isSameItem(mainHand, offHand)) {
            return 0.0F;
        }
        return hasCurio(player, ModItems.DOUBLE_HAPPINESS.get()) ? 1.0F : 0.0F;
    }

    /** 公式 9：多多益善 —— +10% × min(10×10 范围内怪物数, 20)。 */
    private static float moreTheMerrierBonusPercent(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || isDelayedDamage(event.getSource())) {
            return 0.0F;
        }
        Entity attacker = resolveAttacker(event.getSource());
        if (!(attacker instanceof Player player) || player.level().isClientSide()) {
            return 0.0F;
        }
        if (!hasCurio(player, ModItems.THE_MORE_THE_MERRIER.get())) {
            return 0.0F;
        }
        AABB area = new AABB(
                player.getX() - MERRIER_RADIUS, player.level().getMinY(), player.getZ() - MERRIER_RADIUS,
                player.getX() + MERRIER_RADIUS, player.level().getMaxY(), player.getZ() + MERRIER_RADIUS);
        int count = player.level().getEntitiesOfClass(Monster.class, area, Entity::isAlive).size();
        if (count <= 0) {
            return 0.0F;
        }
        return MERRIER_PER_MONSTER * Math.min(count, MERRIER_MAX_MONSTERS);
    }

    /** 公式 10：山海恩泽 —— 下落中的近战命中 +下落格数 × 1%。 */
    private static float shanhaiGraceBonusPercent(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player)
                || player.level().isClientSide() || player.fallDistance <= 0.0F) {
            return 0.0F;
        }
        if (!hasCurio(player, ModItems.SHANHAI_GRACE.get())) {
            return 0.0F;
        }
        return (float) (player.fallDistance * 0.01F);
    }

    /** 公式 11：迅击 —— 附魔等级 × 当前移动速度值。 */
    private static float swiftStrikeBonusPercent(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F
                || isDelayedDamage(event.getSource())) {
            return 0.0F;
        }
        LivingEntity attacker = resolveLivingAttacker(event.getSource());
        if (attacker == null) {
            return 0.0F;
        }
        int level = getWeaponEnchantLevel(attacker, ModEnchantments.SWIFT_STRIKE);
        if (level <= 0) {
            return 0.0F;
        }
        double speed = attacker.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (speed <= 0.0D) {
            return 0.0F;
        }
        return (float) (level * speed);
    }

    /** 暗灵毒牙：命中时附加 3 秒暗毒（不改数值）。 */
    private static void applyDarkSpiritFang(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || isDelayedDamage(event.getSource())) {
            return;
        }
        Entity attacker = resolveAttacker(event.getSource());
        if (!(attacker instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!hasCurio(player, ModItems.DARK_SPIRIT_FANG.get())) {
            return;
        }
        target.addEffect(new MobEffectInstance(ModEffects.DARK_POISON,
                DARK_POISON_DURATION, 0, false, true, true));
    }

    // ------------------------------------------------------------------
    // 阶段 1.5：虎王之蔑（LOWEST）
    // ------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTigerKingDisdainHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        if (isDelayedDamage(source) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (event.getAmount() <= 0.0F || player.isDeadOrDying()) {
            return;
        }
        if (!hasCurio(player, ModItems.TIGER_KING_DISDAIN.get())) {
            return;
        }
        event.setCanceled(true);
        DELAYED_DAMAGES.computeIfAbsent(player.getUUID(), key -> new ArrayList<>())
                .add(new DelayedDamage(source, event.getAmount() * DELAY_FRACTION));
    }

    // ------------------------------------------------------------------
    // 阶段 2：护甲后结算
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        applyWeepingBlood(event);
    }

    private static void applyWeepingBlood(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        LivingEntity attacker = resolveLivingAttacker(event.getSource());
        if (attacker == null || event.getNewDamage() <= 0.0F) {
            return;
        }
        int level = getWeaponEnchantLevel(attacker, ModEnchantments.WEEPING_BLOOD);
        if (level <= 0) {
            return;
        }
        float healAmount = event.getNewDamage() * (0.06F + 0.02F * (level - 1));
        attacker.heal(healAmount);
    }

    /** 逆鳞龙胆（LOWEST）：单次最终伤害不超过最大生命值的 20%。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamageReverseScaleGall(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!hasCurio(player, ModItems.REVERSE_SCALE_GALL.get())) {
            return;
        }
        float cap = player.getMaxHealth() * 0.2F;
        if (event.getNewDamage() > cap) {
            event.setNewDamage(cap);
        }
    }

    /** 米米圣盾：扣血与吸收结算后，补充 10% 最大生命的吸收，累计上限 40%。 */
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!hasCurio(player, ModItems.MIMI_SHIELD.get())) {
            return;
        }
        float maxHealth = player.getMaxHealth();
        float cap = maxHealth * MIMI_SHIELD_CAP_RATIO;
        float current = player.getAbsorptionAmount();
        if (current >= cap) {
            return;
        }
        player.setAbsorptionAmount(Math.min(current + maxHealth * MIMI_SHIELD_GAIN_RATIO, cap));
    }

    // ------------------------------------------------------------------
    // 其它伤害相关事件
    // ------------------------------------------------------------------

    /** 月有圆缺：非暴击近战有 30% 概率转为暴击（1.5 倍）。 */
    @SubscribeEvent
    public static void onMoonPhasesCriticalHit(CriticalHitEvent event) {
        if (event.isVanillaCritical()) {
            return;
        }
        Player player = event.getEntity();
        if (!hasCurio(player, ModItems.MOON_PHASES.get())) {
            return;
        }
        if (player.getRandom().nextFloat() < MOON_CRIT_CHANCE) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(1.5F);
        }
    }

    /** 夔牛之力：攻击前清零目标无敌帧。 */
    @SubscribeEvent
    public static void onKuiNiuPowerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (hasCurio(player, ModItems.KUI_NIU_POWER.get())) {
            target.setInvulnerableTime(0);
        }
    }

    // ------------------------------------------------------------------
    // 虎王之蔑延迟结算引擎
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onDelayedDamageTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        List<DelayedDamage> pending = DELAYED_DAMAGES.get(player.getUUID());
        if (pending == null || pending.isEmpty()) {
            return;
        }
        Iterator<DelayedDamage> iterator = pending.iterator();
        while (iterator.hasNext()) {
            DelayedDamage delayed = iterator.next();
            if (--delayed.ticksUntilNext > 0) {
                continue;
            }
            delayed.ticksUntilNext = DELAY_INTERVAL_TICKS;
            player.setInvulnerableTime(0);
            if (player.level() instanceof ServerLevel serverLevel
                    && player.hurtServer(serverLevel, delayedSource(player, delayed.source), delayed.amount)) {
                hurtArmorDurability(player, delayed.source, delayed.amount);
            }
            player.setInvulnerableTime(0);
            if (--delayed.remaining <= 0) {
                iterator.remove();
            }
        }
        if (pending.isEmpty()) {
            DELAYED_DAMAGES.remove(player.getUUID());
        }
    }

    /** 延迟伤害带 bypasses_armor，这里按原版公式补扣护甲耐久。 */
    private static void hurtArmorDurability(Player player, DamageSource source, float amount) {
        if (amount <= 0.0F || !(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        int wear = (int) Math.max(1.0F, amount / 4.0F);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isDamageableItem()) {
                continue;
            }
            net.minecraft.world.item.component.DamageResistant resistant =
                    stack.get(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT);
            if (resistant != null && resistant.isResistantTo(source)) {
                continue;
            }
            stack.hurtAndBreak(wear, serverLevel, player, ignored -> {
            });
        }
    }

    @SubscribeEvent
    public static void onDelayedDamageClear(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            DELAYED_DAMAGES.remove(player.getUUID());
        }
        IRRITABLE_WINDOWS.remove(event.getEntity().getUUID());
        PELT_STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDelayedDamageClearOnLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DELAYED_DAMAGES.remove(event.getEntity().getUUID());
        IRRITABLE_WINDOWS.remove(event.getEntity().getUUID());
        PELT_STATES.remove(event.getEntity().getUUID());
    }

    private static DamageSource delayedSource(Player player, DamageSource original) {
        Holder<DamageType> holder = player.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DELAYED_DAMAGE_KEY);
        return new DamageSource(holder, original.getDirectEntity(), original.getEntity());
    }

    private static boolean isDelayedDamage(DamageSource source) {
        return source.typeHolder().unwrapKey()
                .map(key -> key.identifier().equals(DELAYED_DAMAGE_ID))
                .orElse(false);
    }

    // ------------------------------------------------------------------
    // 易怒 / 上古毛皮的状态维护
    // ------------------------------------------------------------------

    private static boolean hasAncientPelt(Player player) {
        return hasCurio(player, ModItems.ANCIENT_PELT.get());
    }

    private static void escalateAncientPelt(LivingEntity attacker, long now) {
        if (!(attacker instanceof Player player) || player.level().isClientSide()
                || !hasAncientPelt(player)) {
            return;
        }
        PeltRageState state = PELT_STATES.computeIfAbsent(player.getUUID(),
                key -> new PeltRageState(now));
        if (state.level < PELT_MAX_LEVEL) {
            state.level++;
        }
        state.lastProcTick = now;
    }

    @SubscribeEvent
    public static void onAncientPeltTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        if (!hasAncientPelt(player)) {
            if (PELT_STATES.remove(id) != null) {
                MobEffectInstance current = player.getEffect(ModEffects.IRRITABLE);
                if (current != null && current.isAmbient()
                        && current.getDuration() <= PELT_EFFECT_DURATION) {
                    player.removeEffect(ModEffects.IRRITABLE);
                }
            }
            return;
        }
        long now = player.level().getGameTime();
        PeltRageState state = PELT_STATES.computeIfAbsent(id, key -> new PeltRageState(now));
        if (now - state.lastProcTick > PELT_DECAY_TICKS) {
            state.level = 1;
        }
        int amplifier = state.level - 1;
        MobEffectInstance current = player.getEffect(ModEffects.IRRITABLE);
        if (current != null && current.getAmplifier() != amplifier) {
            player.removeEffect(ModEffects.IRRITABLE);
            current = null;
        }
        if (current == null || current.getDuration() <= PELT_REFRESH_REMAINING) {
            player.addEffect(new MobEffectInstance(ModEffects.IRRITABLE,
                    PELT_EFFECT_DURATION, amplifier, true, true, true));
        }
    }

    /** 每 5 秒清理一次过期的易怒窗口。 */
    @SubscribeEvent
    public static void onIrritableWindowCleanup(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 100 != 0) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        IRRITABLE_WINDOWS.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    // ------------------------------------------------------------------
    // 辅助方法
    // ------------------------------------------------------------------

    private static Entity resolveAttacker(DamageSource source) {
        Entity direct = source.getEntity();
        return direct instanceof Projectile projectile ? projectile.getOwner() : direct;
    }

    private static LivingEntity resolveLivingAttacker(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof Projectile projectile) {
            entity = projectile.getOwner();
        }
        return entity instanceof LivingEntity living ? living : null;
    }

    /** 读取攻击方手持武器上的指定附魔等级（优先主手，主手没有时再看副手）。 */
    private static int getWeaponEnchantLevel(LivingEntity attacker, ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = attacker.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        int mainHand = EnchantmentHelper.getTagEnchantmentLevel(holder, attacker.getMainHandItem());
        if (mainHand > 0) {
            return mainHand;
        }
        return EnchantmentHelper.getTagEnchantmentLevel(holder, attacker.getOffhandItem());
    }

    private static boolean hasCurio(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(item))
                .orElse(false);
    }

    private static boolean isEntityType(LivingEntity entity, String path) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.equals(Identifier.fromNamespaceAndPath(MimiWorld.MODID, path));
    }
}

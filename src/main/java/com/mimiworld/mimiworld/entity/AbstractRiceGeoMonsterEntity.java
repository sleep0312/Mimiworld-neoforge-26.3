package com.mimiworld.mimiworld.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 米米世界 Boss / 精英通用基类（陆地近战）。
 *
 * <p>统一实现：漂浮、近战追击、避水游荡、看向玩家与四处张望、受击反击与主动索敌；
 * 命中触发一次 attack 动画；常驻生命恢复与力量增益；每 5 秒一次「重击」技能
 * （对近身目标造成额外伤害并播放咆哮音效）。Boss 的残堕化等级由
 * {@link com.mimiworld.mimiworld.event.ModEffectEvents} 按实体 ID 赋予。</p>
 */
public abstract class AbstractRiceGeoMonsterEntity extends Monster implements GeoEntity {

    protected static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(AbstractRiceGeoMonsterEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String animationPrefix;
    private final SoundEvent ambientSound;
    private final SoundEvent hurtSound;
    private final SoundEvent deathSound;
    private final SoundEvent roarSound;

    private int heavyAttackCooldown = 60;

    protected AbstractRiceGeoMonsterEntity(EntityType<? extends Monster> type, Level level, String animationPrefix,
                                           SoundEvent ambientSound, SoundEvent hurtSound, SoundEvent deathSound,
                                           SoundEvent roarSound) {
        super(type, level);
        this.animationPrefix = animationPrefix;
        this.ambientSound = ambientSound;
        this.hurtSound = hurtSound;
        this.deathSound = deathSound;
        this.roarSound = roarSound;
    }

    /** 通用 Boss 属性构造。 */
    public static AttributeSupplier.Builder bossAttributes(double health, double attack, double speed,
                                                          double armor, double toughness,
                                                          double knockbackResistance, double followRange) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, attack)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ARMOR_TOUGHNESS, toughness)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, followRange);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_ANIM_TICKS, 0);
    }

    protected RawAnimation idleAnimation() {
        return RawAnimation.begin().thenLoop("animation." + animationPrefix + ".idle");
    }

    protected RawAnimation walkAnimation() {
        return RawAnimation.begin().thenLoop("animation." + animationPrefix + ".walk");
    }

    protected RawAnimation attackAnimation() {
        return RawAnimation.begin().thenPlay("animation." + animationPrefix + ".attack");
    }

    protected int attackAnimationTicks() {
        return 20;
    }

    /** 子类可返回正在释放的技能动画；非 null 时优先级高于普攻/行走/待机。 */
    protected RawAnimation skillAnimation() {
        return null;
    }

    /** 子类实现了自己的重击/技能时返回 true，关闭基类的「每 5 秒重击」。 */
    protected boolean suppressGenericHeavyAttack() {
        return false;
    }

    /** 元素动画键前缀（如 {@code fanjinghuwang}）。 */
    protected String animationPrefix() {
        return this.animationPrefix;
    }

    /** 普攻动画剩余 tick（供子类判断是否处于普攻动作中）。 */
    protected int attackAnimTicks() {
        return this.entityData.get(DATA_ATTACK_ANIM_TICKS);
    }

    /** 触发一次普攻动画。 */
    protected void playAttackAnimation(int ticks) {
        this.entityData.set(DATA_ATTACK_ANIM_TICKS, ticks);
    }

    /** 服务端每个 tick 的 Boss 专属逻辑钩子（基类计数器之后调用）。 */
    protected void bossTick() {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AbstractRiceGeoMonsterEntity>("main", 5, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<AbstractRiceGeoMonsterEntity> state) {
        RawAnimation skill = skillAnimation();
        if (skill != null) {
            return state.setAndContinue(skill);
        }
        if (this.entityData.get(DATA_ATTACK_ANIM_TICKS) > 0) {
            return state.setAndContinue(attackAnimation());
        }
        return state.setAndContinue(state.isMoving() ? walkAnimation() : idleAnimation());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            this.entityData.set(DATA_ATTACK_ANIM_TICKS, attackAnimationTicks());
            return true;
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            return;
        }
        int ticks = this.entityData.get(DATA_ATTACK_ANIM_TICKS);
        if (ticks > 0) {
            this.entityData.set(DATA_ATTACK_ANIM_TICKS, ticks - 1);
        }
        // 常驻增益
        if (this.tickCount % 40 == 0) {
            if (!this.hasEffect(MobEffects.REGENERATION)) {
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 0, false, false));
            }
            if (!this.hasEffect(MobEffects.STRENGTH)) {
                this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 1, false, false));
            }
        }
        // 近身重击：每 5 秒一次额外伤害（子类自带技能时关闭）
        if (!suppressGenericHeavyAttack()) {
            if (heavyAttackCooldown > 0) {
                heavyAttackCooldown--;
            } else if (this.getTarget() != null && this.distanceToSqr(this.getTarget()) < 16.0D) {
                heavyAttackCooldown = 100;
                LivingEntityHeavyHit.hit(this, this.getTarget());
            }
        }
        this.bossTick();
    }

    /** 重击辅助：直接造成额外伤害并击退。 */
    private static final class LivingEntityHeavyHit {
        private static void hit(AbstractRiceGeoMonsterEntity boss, net.minecraft.world.entity.LivingEntity target) {
            float damage = (float) boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F;
            ModDamage.hurt(target, boss.damageSources().mobAttack(boss), damage);
            Vec3 push = target.position().subtract(boss.position()).normalize().scale(0.8D).add(0.0D, 0.35D, 0.0D);
            target.push(push.x, push.y, push.z);
            if (boss.roarSound != null) {
                boss.playSound(boss.roarSound, 2.0F, 1.0F);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ambientSound;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return hurtSound;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return deathSound;
    }
}

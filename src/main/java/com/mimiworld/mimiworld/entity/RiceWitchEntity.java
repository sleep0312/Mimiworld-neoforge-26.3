package com.mimiworld.mimiworld.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

/**
 * 米巫（Rice Witch，注册 id = rice_witch）——黑紫丝绒长袍的大米主题敌对女巫。
 *
 * <p>GeckoLib 资源：geckolib/models/rice_witch.geo.json、
 * textures/entity/rice_witch.png、geckolib/animations/rice_witch.animation.json
 * （动画键 animation.idle / walk / attack / throw）。</p>
 *
 * <p>双攻击方式：4 格外随机投掷中毒/缓慢溅射药水，贴身时投掷瞬间伤害；
 * 目标贴脸时用近战魔法推击。30 心、移速 0.25、近战 4，音效沿用原版女巫。
 * 投掷动画 1.2 秒（24 tick），药水瓶在第 12 tick（出手帧）生成；近战动画 0.9 秒（18 tick）。</p>
 */
public class RiceWitchEntity extends Monster implements GeoEntity, RangedAttackMob {

    /** 近战动画剩余 tick。 */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(RiceWitchEntity.class, EntityDataSerializers.INT);
    /** 投药动画剩余 tick。 */
    private static final EntityDataAccessor<Integer> DATA_THROW_ANIM_TICKS =
            SynchedEntityData.defineId(RiceWitchEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("animation.attack");
    private static final RawAnimation ANIM_THROW = RawAnimation.begin().thenPlay("animation.throw");

    /** attack 动画时长 0.9 秒 = 18 tick */
    private static final int ATTACK_ANIMATION_TICKS = 18;
    /** throw 动画时长 1.2 秒 = 24 tick */
    private static final int THROW_ANIMATION_TICKS = 24;
    /** 药水瓶生成时刻：throw 动画第 12 tick（0.6 秒出手帧）。 */
    private static final int THROW_RELEASE_TICK = 12;

    public RiceWitchEntity(EntityType<? extends RiceWitchEntity> type, Level level) {
        super(type, level);
    }

    /** 30 心、移速 0.25、近战攻击力 4。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0D, 50, 8.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RiceWitchEntity>("main", 5, this::handleAnimState));
    }

    /** 动画优先级：投药 > 近战 > 行走 > 待机。 */
    private PlayState handleAnimState(AnimationTest<RiceWitchEntity> state) {
        if (this.entityData.get(DATA_THROW_ANIM_TICKS) > 0) {
            return state.setAndContinue(ANIM_THROW);
        }
        if (this.entityData.get(DATA_ATTACK_ANIM_TICKS) > 0) {
            return state.setAndContinue(ANIM_ATTACK);
        }
        return state.setAndContinue(state.isMoving() ? ANIM_WALK : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            this.entityData.set(DATA_ATTACK_ANIM_TICKS, ATTACK_ANIMATION_TICKS);
            return true;
        }
        return false;
    }

    /** RangedAttackGoal 决定开火时调用（仅服务端）：只启动投药动画并计时。 */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        this.entityData.set(DATA_THROW_ANIM_TICKS, THROW_ANIMATION_TICKS);
    }

    /**
     * 朝目标丢一瓶溅射药水（弹道参数对齐原版女巫）：
     * 4 格外随机中毒/缓慢，4 格内丢瞬间伤害。
     */
    private void launchSplashPotion(LivingEntity target) {
        Holder<Potion> potion = Potions.HARMING;
        if (target.distanceToSqr(this) > 16.0D) {
            potion = this.random.nextBoolean() ? Potions.POISON : Potions.SLOWNESS;
        }
        ItemStack potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, potion);
        ThrownSplashPotion thrownPotion = new ThrownSplashPotion(this.level(), this, potionStack);
        double dx = target.getX() - this.getX();
        double dy = target.getY() + target.getEyeHeight() - 1.1D - thrownPotion.getY();
        double dz = target.getZ() - this.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        thrownPotion.shoot(dx, dy + horizontalDist * 0.1F, dz, 0.75F, 8.0F);
        if (!this.isSilent()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WITCH_THROW, this.getSoundSource(),
                    1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        }
        this.level().addFreshEntity(thrownPotion);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            int attackTicks = this.entityData.get(DATA_ATTACK_ANIM_TICKS);
            if (attackTicks > 0) {
                this.entityData.set(DATA_ATTACK_ANIM_TICKS, attackTicks - 1);
            }
            int throwTicks = this.entityData.get(DATA_THROW_ANIM_TICKS);
            if (throwTicks > 0) {
                // 计数器在 13→12 这一刻出手（动画 0.6 秒前挥帧）
                if (throwTicks == THROW_RELEASE_TICK + 1) {
                    LivingEntity target = this.getTarget();
                    if (target != null && target.isAlive()) {
                        launchSplashPotion(target);
                    }
                }
                this.entityData.set(DATA_THROW_ANIM_TICKS, throwTicks - 1);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_ANIM_TICKS, 0);
        builder.define(DATA_THROW_ANIM_TICKS, 0);
    }

    // —— 音效：沿用原版女巫 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }
}

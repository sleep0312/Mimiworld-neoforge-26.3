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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.EnumSet;

/**
 * 米米世界「常驻飞行敌对生物」通用基类。
 *
 * <p>统一实现：无重力悬浮（{@link FlyingMoveControl} + {@link FlyingPathNavigation}）、
 * 主动索敌玩家与受击反击、近战扑击（命中触发 attack 动画）、
 * 无目标时在空中随机悬停漂浮、飞行生物免摔落伤害。</p>
 *
 * <p>子类只需提供属性、音效、三段动画键与攻击动画时长。</p>
 */
public abstract class AbstractFlyingRiceMobEntity extends Monster implements GeoEntity {

    /** 同步数据：攻击动画剩余 tick。 */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(AbstractFlyingRiceMobEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 位移速度平方阈值：高于它视为飞行移动，否则悬停。 */
    private static final double MOVE_ANIM_SPEED_SQR = 0.0025D;

    protected AbstractFlyingRiceMobEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // 常驻飞行：无重力 + 急停悬停式飞行控制器
        this.setNoGravity(true);
        this.moveControl = new FlyingMoveControl<>(this, 10, true);
        // 26.3 路径代价类型统一为 PathType
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
        this.setPathfindingMalus(PathType.FIRE, 0.0F);
    }

    /** 待机/悬停循环动画。 */
    protected abstract RawAnimation idleAnimation();

    /** 飞行移动循环动画。 */
    protected abstract RawAnimation moveAnimation();

    /** 命中时单次播放的攻击动画。 */
    protected abstract RawAnimation attackAnimation();

    /** 攻击动画时长（tick）。 */
    protected abstract int attackAnimationTicks();

    /** 攻击动画剩余 tick。 */
    protected int attackAnimTicks() {
        return this.entityData.get(DATA_ATTACK_ANIM_TICKS);
    }

    /** 触发一次攻击/技能动画，ticks 内保持该动画并逐 tick 递减。 */
    protected void playAttackAnimation(int ticks) {
        this.entityData.set(DATA_ATTACK_ANIM_TICKS, ticks);
    }

    /** 子类可返回正在播放的技能动画（如飞行循环）；非 null 时优先于移动/待机，低于攻击动画。 */
    protected RawAnimation skillAnimation() {
        return null;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new HoverWanderGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_ANIM_TICKS, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AbstractFlyingRiceMobEntity>("main", 5, this::handleAnimState));
    }

    /** 动画优先级：扑击 > 技能/飞行 > 飞行移动 > 悬停。 */
    private PlayState handleAnimState(AnimationTest<AbstractFlyingRiceMobEntity> state) {
        if (this.entityData.get(DATA_ATTACK_ANIM_TICKS) > 0) {
            return state.setAndContinue(attackAnimation());
        }
        RawAnimation skill = skillAnimation();
        if (skill != null) {
            return state.setAndContinue(skill);
        }
        boolean moving = this.getDeltaMovement().lengthSqr() > MOVE_ANIM_SPEED_SQR;
        return state.setAndContinue(moving ? moveAnimation() : idleAnimation());
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
        if (!this.level().isClientSide()) {
            int ticks = this.entityData.get(DATA_ATTACK_ANIM_TICKS);
            if (ticks > 0) {
                this.entityData.set(DATA_ATTACK_ANIM_TICKS, ticks - 1);
            }
        }
    }

    /** 飞行生物永不承受摔落伤害。 */
    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** 无目标时的空中漂浮：随机选取身体周围 ±12 水平、±8 高度的空中点飘过去。 */
    private class HoverWanderGoal extends Goal {
        HoverWanderGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return AbstractFlyingRiceMobEntity.this.getTarget() == null
                    && !AbstractFlyingRiceMobEntity.this.getMoveControl().hasWanted()
                    && AbstractFlyingRiceMobEntity.this.random.nextInt(40) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
            if (AbstractFlyingRiceMobEntity.this.random.nextInt(20) == 0) {
                AbstractFlyingRiceMobEntity mob = AbstractFlyingRiceMobEntity.this;
                double tx = mob.getX() + (mob.random.nextDouble() * 2.0D - 1.0D) * 12.0D;
                double ty = mob.getY() + (mob.random.nextDouble() * 2.0D - 1.0D) * 8.0D + 2.0D;
                double tz = mob.getZ() + (mob.random.nextDouble() * 2.0D - 1.0D) * 12.0D;
                ty = Math.max(ty, mob.level().getMinY() + 2.0D);
                mob.getMoveControl().setWantedPosition(tx, ty, tz, 0.85D);
            }
        }
    }
}

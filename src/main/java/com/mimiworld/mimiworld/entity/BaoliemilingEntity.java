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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 爆烈米灵（注册 id = explosive_rice_spirit）——烈焰人式敌对飞行生物。
 *
 * <p>GeckoLib 资源：geckolib/models/baoliemiling.geo.json、
 * textures/entity/baoliemiling.png、geckolib/animations/baoliemiling.animation.json
 * （动画键 animation.baoliemiling.idle / walk / attack）。</p>
 *
 * <p>行为对齐原版烈焰人：常驻无重力悬浮、火焰免疫；近距离近战，远距离蓄势连喷三发火米弹；
 * 60 心、近战 6、追击 48；碰撞箱 0.8×1.8。</p>
 */
public class BaoliemilingEntity extends Monster implements GeoEntity {

    /** 同步数据：攻击动画剩余 tick（开火/命中时置 24 并逐 tick 递减）。 */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(BaoliemilingEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.baoliemiling.idle");
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.baoliemiling.walk");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().thenPlay("animation.baoliemiling.attack");

    /** attack 动画时长 1.2 秒 = 24 tick */
    private static final int ATTACK_ANIMATION_TICKS = 24;

    /** 位移速度平方阈值。 */
    private static final double MOVE_ANIM_SPEED_SQR = 0.0025D;

    public BaoliemilingEntity(EntityType<? extends BaoliemilingEntity> type, Level level) {
        super(type, level);
        // 常驻飞行：无重力 + 烈焰人式悬停飞行控制器
        this.setNoGravity(true);
        this.moveControl = new FlyingMoveControl<>(this, 10, true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
        this.setPathfindingMalus(PathType.FIRE, 0.0F);
    }

    /** 60 心、移速 0.23、飞行 1.0、近战 6、追击 48。 */
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FLYING_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
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
        this.goalSelector.addGoal(2, new RiceSpiritAttackGoal());
        this.goalSelector.addGoal(3, new HoverWanderGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BaoliemilingEntity>("main", 5, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<BaoliemilingEntity> state) {
        if (this.entityData.get(DATA_ATTACK_ANIM_TICKS) > 0) {
            return state.setAndContinue(ANIM_ATTACK);
        }
        boolean moving = this.getDeltaMovement().lengthSqr() > MOVE_ANIM_SPEED_SQR;
        return state.setAndContinue(moving ? ANIM_WALK : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            this.triggerAttackAnim();
            return true;
        }
        return false;
    }

    private void triggerAttackAnim() {
        this.entityData.set(DATA_ATTACK_ANIM_TICKS, ATTACK_ANIMATION_TICKS);
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_ANIM_TICKS, 0);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    // —— 音效：沿用原版烈焰人 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    /**
     * 烈焰人式攻击节奏：2 格内每 20 tick 近战一次并向目标贴近；
     * 远距离连续可见 20 tick 后蓄势 60 tick，然后每 6 tick 喷一发小火弹共三发，随后冷却 100 tick。
     */
    private class RiceSpiritAttackGoal extends Goal {
        private int attackStep;
        private int attackTime;
        private int lastSeen;

        RiceSpiritAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = BaoliemilingEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = BaoliemilingEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.attackStep = 0;
        }

        @Override
        public void stop() {
            this.lastSeen = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            --this.attackTime;
            LivingEntity target = BaoliemilingEntity.this.getTarget();
            if (target == null) {
                return;
            }
            boolean hasLineOfSight = BaoliemilingEntity.this.getSensing().hasLineOfSight(target);
            boolean wasVisible = this.lastSeen > 0;
            if (hasLineOfSight != wasVisible) {
                this.lastSeen = 0;
            }
            if (hasLineOfSight) {
                ++this.lastSeen;
            } else {
                --this.lastSeen;
            }

            double distSqr = BaoliemilingEntity.this.distanceToSqr(target);
            double followRange = BaoliemilingEntity.this.getAttributeValue(Attributes.FOLLOW_RANGE);
            if (distSqr < 4.0D) {
                // 近战距离：每 20 tick 扑击一次，身体持续压向目标
                if (this.attackTime <= 0 && hasLineOfSight
                        && BaoliemilingEntity.this.level() instanceof ServerLevel serverLevel) {
                    this.attackTime = 20;
                    BaoliemilingEntity.this.doHurtTarget(serverLevel, target);
                }
                BaoliemilingEntity.this.getMoveControl().setWantedPosition(
                        target.getX(), target.getY(), target.getZ(), 1.0D);
            } else if (distSqr < followRange * followRange && this.lastSeen >= 20) {
                // 远程：蓄势后连发三发火米弹
                if (this.attackTime <= 0) {
                    ++this.attackStep;
                    if (this.attackStep == 1) {
                        this.attackTime = 60;
                    } else if (this.attackStep <= 4) {
                        this.attackTime = 6;
                    } else {
                        this.attackTime = 100;
                        this.attackStep = 0;
                    }
                    if (this.attackStep > 1 && this.attackStep <= 4) {
                        this.shootFireRice(target);
                    }
                }
                BaoliemilingEntity.this.getLookControl().setLookAt(
                        target.getX(), target.getY(0.5D), target.getZ(), 10.0F, 10.0F);
                BaoliemilingEntity.this.getMoveControl().setWantedPosition(
                        target.getX(), target.getY(), target.getZ(), 1.0D);
            } else {
                BaoliemilingEntity.this.getLookControl().setLookAt(
                        target.getX(), target.getY(0.5D), target.getZ(), 10.0F, 10.0F);
            }
            super.tick();
        }

        /** 从身前 4 格沿视线发射一枚小火弹（沿用原版实体）。 */
        private void shootFireRice(LivingEntity target) {
            BaoliemilingEntity spirit = BaoliemilingEntity.this;
            Vec3 view = spirit.getViewVector(1.0F);
            double dx = target.getX() - (spirit.getX() + view.x * 4.0D);
            double dy = target.getY(0.5D) - (0.5D + spirit.getY(0.5D));
            double dz = target.getZ() - (spirit.getZ() + view.z * 4.0D);
            if (!spirit.isSilent()) {
                spirit.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
            }
            // 26.3：SmallFireball 迁至 hurtingprojectile 包，方向参数改为 Vec3
            SmallFireball fireball = new SmallFireball(spirit.level(), spirit, new Vec3(dx, dy, dz));
            fireball.setPos(
                    spirit.getX() + view.x * 4.0D,
                    spirit.getY(0.5D) + 0.5D,
                    spirit.getZ() + view.z * 4.0D);
            spirit.level().addFreshEntity(fireball);
            spirit.triggerAttackAnim();
        }
    }

    /** 无目标时空中漂浮：随机选取周围 ±16 水平、±8 高度的空中点飞过去。 */
    private class HoverWanderGoal extends Goal {
        HoverWanderGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BaoliemilingEntity spirit = BaoliemilingEntity.this;
            return spirit.getTarget() == null
                    && !spirit.getMoveControl().hasWanted()
                    && spirit.random.nextInt(40) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
            if (BaoliemilingEntity.this.random.nextInt(20) == 0) {
                BaoliemilingEntity spirit = BaoliemilingEntity.this;
                double tx = spirit.getX() + (spirit.random.nextDouble() * 2.0D - 1.0D) * 16.0D;
                double ty = spirit.getY() + (spirit.random.nextDouble() * 2.0D - 1.0D) * 8.0D;
                double tz = spirit.getZ() + (spirit.random.nextDouble() * 2.0D - 1.0D) * 16.0D;
                ty = Math.max(ty, spirit.level().getMinY() + 2.0D);
                spirit.getMoveControl().setWantedPosition(tx, ty, tz, 0.85D);
            }
        }
    }
}

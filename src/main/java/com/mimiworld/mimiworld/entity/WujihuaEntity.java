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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 污棘花（注册 id = blighted_thorn_flower）——扎根原地的敌对食肉植物。
 *
 * <p>GeckoLib 资源：geckolib/models/wujihua.geo.json、
 * textures/entity/wujihua.png、geckolib/animations/wujihua.animation.json
 * （动画键 animation.wujihua.idle / attack，无 walk——它不会走）。</p>
 *
 * <p>扎根实现：不注册移动 AI，travel 清零水平速度，免疫击退且不会被实体挤开；
 * 500 心、攻击力 50；咬合音沿用原版史莱姆；碰撞箱 0.9×1.8。</p>
 */
public class WujihuaEntity extends Monster implements GeoEntity {

    /** 同步数据：攻击动画剩余 tick。 */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(WujihuaEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.wujihua.idle");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().thenPlay("animation.wujihua.attack");

    /** attack 动画时长 1.2 秒 = 24 tick */
    private static final int ATTACK_ANIMATION_TICKS = 24;

    /** 咬合触发距离平方：中心距 2 格内。 */
    private static final double CHOMP_RANGE_SQR = 4.0D;

    public WujihuaEntity(EntityType<? extends WujihuaEntity> type, Level level) {
        super(type, level);
    }

    /** 500 心、攻击力 50；移速 0、满击退抗性（扎根）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 50.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // 不注册任何移动类 Goal（不游荡、不追击），只有原地啃咬 + 张望
        this.goalSelector.addGoal(2, new ChompGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 扎根：永久清零水平速度，只让父类结算重力与落地碰撞。 */
    @Override
    public void travel(Vec3 travelVector) {
        Vec3 movement = this.getDeltaMovement();
        if (movement.x != 0.0D || movement.z != 0.0D) {
            this.setDeltaMovement(0.0D, movement.y, 0.0D);
        }
        super.travel(Vec3.ZERO);
    }

    /** 植物不可被其他实体挤动。 */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<WujihuaEntity>("main", 5, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<WujihuaEntity> state) {
        return state.setAndContinue(this.entityData.get(DATA_ATTACK_ANIM_TICKS) > 0 ? ANIM_ATTACK : ANIM_IDLE);
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

    // —— 音效：湿软的植物咬合声 ——

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    /** 原地啃咬 AI：面朝范围内目标，每 30 tick 咬一次，不做任何位移。 */
    private class ChompGoal extends Goal {
        private int cooldown;

        ChompGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = WujihuaEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = WujihuaEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            --this.cooldown;
            LivingEntity target = WujihuaEntity.this.getTarget();
            if (target == null) {
                return;
            }
            WujihuaEntity.this.getLookControl().setLookAt(
                    target.getX(), target.getY(0.5D), target.getZ(), 10.0F, 10.0F);

            if (this.cooldown <= 0
                    && WujihuaEntity.this.distanceToSqr(target) <= CHOMP_RANGE_SQR
                    && WujihuaEntity.this.getSensing().hasLineOfSight(target)
                    && WujihuaEntity.this.level() instanceof ServerLevel serverLevel) {
                if (WujihuaEntity.this.doHurtTarget(serverLevel, target)) {
                    this.cooldown = 30;
                    WujihuaEntity.this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, 0.7F);
                } else {
                    this.cooldown = 10;
                }
            }
        }
    }
}

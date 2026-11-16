package com.mimiworld.mimiworld.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 哮噬米狼鹫（Rice Wolf-Vulture，注册 id = rice_wolf_vulture）——陆空敌对狼鹫。
 *
 * <p>GeckoLib 资源：geckolib/models/rice_wolf_vulture.geo.json（几何名 xiaoshimilangjiu）、
 * textures/entity/rice_wolf_vulture.png、geckolib/animations/rice_wolf_vulture.animation.json
 * （动画键 animation.xiaoshimilangjiu.idle / walk / attack_bite / attack_dive）。碰撞箱 1.2×1.5。</p>
 *
 * <p>500 心、近战 500、移速 0.32、满击退抗性。两种攻击：贴身扑咬（attack_bite），
 * 以及 4~16 格的招牌俯冲撞击（attack_dive，52 tick 时间线，第 38 tick 冲刺、
 * 40~50 tick 贴身结算伤害，冷却 8 秒）。</p>
 */
public class RiceWolfVultureEntity extends Monster implements GeoEntity {

    /** 啃咬动画剩余 tick（attack_bite 长 1.2s = 24 tick）。 */
    private static final EntityDataAccessor<Integer> DATA_BITE_ANIM_TICKS =
            SynchedEntityData.defineId(RiceWolfVultureEntity.class, EntityDataSerializers.INT);
    /** 俯冲动画剩余 tick（attack_dive 长 2.6s = 52 tick）。 */
    private static final EntityDataAccessor<Integer> DATA_DIVE_ANIM_TICKS =
            SynchedEntityData.defineId(RiceWolfVultureEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.xiaoshimilangjiu.idle");
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.xiaoshimilangjiu.walk");
    private static final RawAnimation ANIM_BITE =
            RawAnimation.begin().thenPlay("animation.xiaoshimilangjiu.attack_bite");
    private static final RawAnimation ANIM_DIVE =
            RawAnimation.begin().thenPlay("animation.xiaoshimilangjiu.attack_dive");

    private static final int BITE_ANIMATION_TICKS = 24;
    private static final int DIVE_ANIMATION_TICKS = 52;

    public RiceWolfVultureEntity(EntityType<? extends RiceWolfVultureEntity> type, Level level) {
        super(type, level);
    }

    /** 500 心、近战 500、移速 0.32、追击 40、满击退抗性。 */
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 500.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DiveAttackGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RiceWolfVultureEntity>("main", 5, this::handleAnimState));
    }

    /** 动画优先级：俯冲 > 啃咬 > 行走 > 待机。 */
    private PlayState handleAnimState(AnimationTest<RiceWolfVultureEntity> state) {
        if (this.entityData.get(DATA_DIVE_ANIM_TICKS) > 0) {
            return state.setAndContinue(ANIM_DIVE);
        }
        if (this.entityData.get(DATA_BITE_ANIM_TICKS) > 0) {
            return state.setAndContinue(ANIM_BITE);
        }
        return state.setAndContinue(state.isMoving() ? ANIM_WALK : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** 啃咬命中触发 attack_bite；俯冲期间命中不覆盖俯冲动画。 */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            if (this.entityData.get(DATA_DIVE_ANIM_TICKS) == 0) {
                this.entityData.set(DATA_BITE_ANIM_TICKS, BITE_ANIMATION_TICKS);
            }
            return true;
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            int dive = this.entityData.get(DATA_DIVE_ANIM_TICKS);
            if (dive > 0) {
                this.entityData.set(DATA_DIVE_ANIM_TICKS, dive - 1);
            }
            int bite = this.entityData.get(DATA_BITE_ANIM_TICKS);
            if (bite > 0) {
                this.entityData.set(DATA_BITE_ANIM_TICKS, bite - 1);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BITE_ANIM_TICKS, 0);
        builder.define(DATA_DIVE_ANIM_TICKS, 0);
    }

    // —— 音效：原版狼经典音色 + 幻翼扑翼/俯冲声 ——

    private static final WolfSoundVariant.WolfSoundSet WOLF_SOUNDS_CLASSIC =
            SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.SoundSet.CLASSIC).adultSounds();

    @Override
    protected SoundEvent getAmbientSound() {
        return WOLF_SOUNDS_CLASSIC.growlSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WOLF_SOUNDS_CLASSIC.hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WOLF_SOUNDS_CLASSIC.deathSound().value();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(WOLF_SOUNDS_CLASSIC.stepSound().value(), 0.2F, 0.85F);
    }

    /**
     * 招牌攻击：盘旋升空后俯冲撞击。
     * t=0 播 attack_dive（52 tick）；第 38 tick 给一个朝目标的高速扑冲速度；
     * 第 40~50 tick 贴身时结算一次近战伤害；结束后冷却 8 秒。
     */
    private class DiveAttackGoal extends Goal {
        private int diveTime;
        private int cooldown;
        private boolean hitDone;

        DiveAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = RiceWolfVultureEntity.this.getTarget();
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            if (target == null || !target.isAlive()) {
                return false;
            }
            if (RiceWolfVultureEntity.this.entityData.get(DATA_DIVE_ANIM_TICKS) > 0) {
                return false;
            }
            double distSq = RiceWolfVultureEntity.this.distanceToSqr(target);
            return distSq >= 16.0D && distSq <= 256.0D
                    && RiceWolfVultureEntity.this.onGround()
                    && RiceWolfVultureEntity.this.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return this.diveTime < DIVE_ANIMATION_TICKS;
        }

        @Override
        public void start() {
            this.diveTime = 0;
            this.hitDone = false;
            RiceWolfVultureEntity.this.entityData.set(DATA_DIVE_ANIM_TICKS, DIVE_ANIMATION_TICKS);
            RiceWolfVultureEntity.this.playSound(SoundEvents.PHANTOM_FLAP, 1.2F, 0.7F);
        }

        @Override
        public void tick() {
            this.diveTime++;
            LivingEntity target = RiceWolfVultureEntity.this.getTarget();
            if (target == null) {
                return;
            }
            RiceWolfVultureEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (this.diveTime == 38) {
                Vec3 dir = new Vec3(
                        target.getX() - RiceWolfVultureEntity.this.getX(),
                        Math.max(-1.0D, target.getY() - RiceWolfVultureEntity.this.getY()),
                        target.getZ() - RiceWolfVultureEntity.this.getZ());
                if (dir.lengthSqr() > 1.0E-4D) {
                    dir = dir.normalize();
                    RiceWolfVultureEntity.this.setDeltaMovement(dir.x * 1.7D, 0.22D, dir.z * 1.7D);
                }
                RiceWolfVultureEntity.this.playSound(SoundEvents.PHANTOM_SWOOP, 1.4F, 0.8F);
            }

            if (!this.hitDone && this.diveTime >= 40 && this.diveTime <= 50) {
                double verticalGap = Math.abs(target.getY() - RiceWolfVultureEntity.this.getY());
                if (verticalGap < 2.5D
                        && RiceWolfVultureEntity.this.distanceToSqr(target) < 7.0D
                        && RiceWolfVultureEntity.this.level() instanceof ServerLevel serverLevel) {
                    this.hitDone = RiceWolfVultureEntity.this.doHurtTarget(serverLevel, target);
                }
            }
        }

        @Override
        public void stop() {
            this.diveTime = 0;
            this.cooldown = 160;
        }
    }
}

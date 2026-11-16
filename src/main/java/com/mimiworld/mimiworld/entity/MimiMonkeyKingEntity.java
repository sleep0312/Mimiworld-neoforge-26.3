package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;
import com.mimiworld.mimiworld.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * 米米猴王（Mimi Monkey King，注册 id = mimi_monkey_king）——手持金箍棒的敌对陆地 Boss。
 *
 * <p>三种攻击：普攻（16 tick）、劈棍（前方 3×3，10 伤害，CD 110t）、
 * 掷棍（生成 {@link ThrownMonkeyStaffEntity}，路径 14 伤害，CD 200t）。</p>
 */
public class MimiMonkeyKingEntity extends AbstractRiceGeoMonsterEntity {

    private static final EntityDataAccessor<Integer> DATA_SLAM_ANIM_TICKS =
            SynchedEntityData.defineId(MimiMonkeyKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_THROW_ANIM_TICKS =
            SynchedEntityData.defineId(MimiMonkeyKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STAFF_OUT =
            SynchedEntityData.defineId(MimiMonkeyKingEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation ANIM_SLAM = RawAnimation.begin().thenPlay("animation.mimihouwang.attack_slam");
    private static final RawAnimation ANIM_THROW = RawAnimation.begin().thenPlay("animation.mimihouwang.throw");

    private static final int SLAM_ANIMATION_TICKS = 28;
    private static final int SLAM_HIT_TICK = 18;
    private static final int SLAM_COOLDOWN_TICKS = 110;
    private static final float SLAM_DAMAGE = 10.0F;
    private static final int THROW_ANIMATION_TICKS = 40;
    private static final int THROW_RELEASE_TICK = 10;
    private static final int THROW_COOLDOWN_TICKS = 200;
    private static final int INITIAL_SLAM_DELAY = 100;
    private static final int INITIAL_THROW_DELAY = 50;

    private int slamCooldown = INITIAL_SLAM_DELAY;
    private int throwCooldown = INITIAL_THROW_DELAY;

    public MimiMonkeyKingEntity(EntityType<? extends MimiMonkeyKingEntity> type, Level level) {
        super(type, level, "mimihouwang",
                ModSounds.MIMI_MONKEY_KING_AMBIENT.get(),
                ModSounds.MIMI_MONKEY_KING_HURT.get(),
                ModSounds.MIMI_MONKEY_KING_DEATH.get(),
                ModSounds.MIMI_MONKEY_KING_ROAR.get());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return bossAttributes(150.0D, 7.0D, 0.3D, 15.0D, 0.0D, 0.5D, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLAM_ANIM_TICKS, 0);
        builder.define(DATA_THROW_ANIM_TICKS, 0);
        builder.define(DATA_STAFF_OUT, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ThrowStaffGoal());
        this.goalSelector.addGoal(2, new SlamGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected int attackAnimationTicks() {
        return 16;
    }

    @Override
    protected boolean suppressGenericHeavyAttack() {
        return true;
    }

    @Override
    protected RawAnimation skillAnimation() {
        if (this.entityData.get(DATA_THROW_ANIM_TICKS) > 0) {
            return ANIM_THROW;
        }
        if (this.entityData.get(DATA_SLAM_ANIM_TICKS) > 0) {
            return ANIM_SLAM;
        }
        return null;
    }

    public boolean isStaffOut() {
        return this.entityData.get(DATA_STAFF_OUT);
    }

    private boolean isCasting() {
        return this.entityData.get(DATA_THROW_ANIM_TICKS) > 0
                || this.entityData.get(DATA_SLAM_ANIM_TICKS) > 0;
    }

    @Override
    protected void bossTick() {
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }
        if (this.throwCooldown > 0) {
            this.throwCooldown--;
        }
        int slamTicks = this.entityData.get(DATA_SLAM_ANIM_TICKS);
        if (slamTicks > 0) {
            this.entityData.set(DATA_SLAM_ANIM_TICKS, slamTicks - 1);
        }
        int throwTicks = this.entityData.get(DATA_THROW_ANIM_TICKS);
        if (throwTicks > 0) {
            this.entityData.set(DATA_THROW_ANIM_TICKS, throwTicks - 1);
            if (throwTicks - 1 == 0) {
                this.entityData.set(DATA_STAFF_OUT, false);
            }
        }
    }

    /** 劈棍结算：前方 3×3（左右各 1.5、纵深 0~3），10 点伤害 + 外推。 */
    private void performSlam() {
        LivingEntity lockedTarget = this.getTarget();
        float slamYaw = this.getYRot();
        if (lockedTarget != null) {
            slamYaw = (float) Math.toDegrees(Math.atan2(
                    -(lockedTarget.getX() - this.getX()),
                    lockedTarget.getZ() - this.getZ()));
            this.setYRot(slamYaw);
            this.yBodyRot = slamYaw;
            this.setYHeadRot(slamYaw);
        }

        Vec3 base = this.position();
        Vec3 facing = Vec3.directionFromRotation(0.0F, slamYaw);
        Vec3 right = new Vec3(-facing.z, 0.0D, facing.x);

        final double halfWidth = 1.5D;
        final double depth = 3.0D;
        final double margin = 0.6D;

        AABB searchArea = this.getBoundingBox().inflate(5.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                target -> target != this && target.isAlive() && !target.isAlliedTo(this));
        for (LivingEntity target : targets) {
            Vec3 center = target.getBoundingBox().getCenter();
            double relX = center.x - base.x;
            double relZ = center.z - base.z;
            double forward = relX * facing.x + relZ * facing.z;
            double side = relX * right.x + relZ * right.z;
            boolean horizontalHit = forward >= -margin && forward <= depth + margin
                    && Math.abs(side) <= halfWidth + margin;
            boolean verticalHit = target.getBoundingBox().maxY >= base.y - 0.5D
                    && target.getBoundingBox().minY <= base.y + 3.0D;
            if (horizontalHit && verticalHit) {
                ModDamage.hurt(target, this.damageSources().mobAttack(this), SLAM_DAMAGE);
                double pushX = target.getX() - this.getX();
                double pushZ = target.getZ() - this.getZ();
                double pushLen = Math.sqrt(pushX * pushX + pushZ * pushZ);
                if (pushLen > 1.0E-4D) {
                    target.push(pushX / pushLen * 0.55D, 0.25D, pushZ / pushLen * 0.55D);
                }
            }
        }

        this.playSound(ModSounds.MIMI_MONKEY_KING_ROAR.get(), 1.0F, 1.05F);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.3F);
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int d = 0; d < 3; d++) {
                for (int s = -1; s <= 1; s++) {
                    double dist = 0.7D + d * 1.05D;
                    double cx = base.x + facing.x * dist + right.x * s * 1.05D
                            + (this.random.nextDouble() - 0.5D) * 0.5D;
                    double cz = base.z + facing.z * dist + right.z * s * 1.05D
                            + (this.random.nextDouble() - 0.5D) * 0.5D;
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                            cx, base.y + 0.15D, cz, 5, 0.2D, 0.08D, 0.2D, 0.02D);
                    if (d == 1 && s == 0) {
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                                cx, base.y + 0.2D, cz, 1, 0.1D, 0.05D, 0.1D, 0.0D);
                    }
                }
            }
        }
    }

    /** 第 10 tick 出手：胸前生成金箍棒投掷物，朝目标眼部直线飞出。 */
    private void launchStaffAt(LivingEntity target) {
        ThrownMonkeyStaffEntity staff = new ThrownMonkeyStaffEntity(
                ModEntities.THROWN_MONKEY_STAFF.get(), this, this.level());
        staff.setPos(this.getX(), this.getY() + 1.35D, this.getZ());
        staff.setYRot(this.getYRot());
        staff.launchTowards(target.getEyePosition());
        this.level().addFreshEntity(staff);
        this.entityData.set(DATA_STAFF_OUT, true);
        this.playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.85F + this.random.nextFloat() * 0.3F);
    }

    /** 劈棍 Goal。 */
    private class SlamGoal extends Goal {
        private int runTicks;

        SlamGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = MimiMonkeyKingEntity.this.getTarget();
            return MimiMonkeyKingEntity.this.slamCooldown <= 0
                    && !MimiMonkeyKingEntity.this.isCasting()
                    && target != null && target.isAlive()
                    && MimiMonkeyKingEntity.this.distanceToSqr(target) <= 16.0D
                    && MimiMonkeyKingEntity.this.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = MimiMonkeyKingEntity.this.getTarget();
            return this.runTicks < SLAM_ANIMATION_TICKS && target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.runTicks = 0;
            MimiMonkeyKingEntity.this.getNavigation().stop();
            MimiMonkeyKingEntity.this.entityData.set(DATA_SLAM_ANIM_TICKS, SLAM_ANIMATION_TICKS);
        }

        @Override
        public void tick() {
            this.runTicks++;
            MimiMonkeyKingEntity.this.getNavigation().stop();
            LivingEntity target = MimiMonkeyKingEntity.this.getTarget();
            if (target != null) {
                MimiMonkeyKingEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            if (this.runTicks == SLAM_HIT_TICK) {
                MimiMonkeyKingEntity.this.performSlam();
            }
        }

        @Override
        public void stop() {
            this.runTicks = 0;
            MimiMonkeyKingEntity.this.slamCooldown = SLAM_COOLDOWN_TICKS;
        }
    }

    /** 掷棍 Goal。 */
    private class ThrowStaffGoal extends Goal {
        private int runTicks;

        ThrowStaffGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = MimiMonkeyKingEntity.this.getTarget();
            if (MimiMonkeyKingEntity.this.throwCooldown > 0
                    || MimiMonkeyKingEntity.this.isCasting()
                    || target == null || !target.isAlive()
                    || !MimiMonkeyKingEntity.this.hasLineOfSight(target)) {
                return false;
            }
            double distSqr = MimiMonkeyKingEntity.this.distanceToSqr(target);
            return distSqr >= 9.0D && distSqr <= 256.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = MimiMonkeyKingEntity.this.getTarget();
            return this.runTicks < THROW_ANIMATION_TICKS && target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.runTicks = 0;
            MimiMonkeyKingEntity.this.getNavigation().stop();
            MimiMonkeyKingEntity.this.entityData.set(DATA_THROW_ANIM_TICKS, THROW_ANIMATION_TICKS);
        }

        @Override
        public void tick() {
            this.runTicks++;
            MimiMonkeyKingEntity.this.getNavigation().stop();
            LivingEntity target = MimiMonkeyKingEntity.this.getTarget();
            if (target != null) {
                MimiMonkeyKingEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            if (this.runTicks == THROW_RELEASE_TICK && target != null) {
                MimiMonkeyKingEntity.this.launchStaffAt(target);
            }
        }

        @Override
        public void stop() {
            this.runTicks = 0;
            MimiMonkeyKingEntity.this.throwCooldown = THROW_COOLDOWN_TICKS;
            MimiMonkeyKingEntity.this.entityData.set(DATA_STAFF_OUT, false);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MIMI_MONKEY_KING_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIMI_MONKEY_KING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MIMI_MONKEY_KING_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.PARROT_FLY, 0.06F, 1.4F);
    }
}

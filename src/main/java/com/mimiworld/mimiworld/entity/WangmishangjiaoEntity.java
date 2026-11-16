package com.mimiworld.mimiworld.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 望米伤鲛（Wangmishangjiao，注册 id = wangmi_wounded_mermaid）——水生敌对巨型古鲸精英。
 *
 * <p>深海中缓慢巡游，发现目标后以 1.5 倍速度巨兽冲刺并一口咬下；搁浅时会先尝试寻路回水，
 * 并沉重地朝面朝方向扑腾跳跃。</p>
 *
 * <p>1000 心（2000 血）、啃咬 300 伤害、巡游速度 1.0、追击 48、高击退抗性。</p>
 */
public class WangmishangjiaoEntity extends AbstractRiceGeoMonsterEntity {

    /** attack 动画时长 1.3 秒 = 26 tick */
    private static final int ATTACK_ANIMATION_TICKS = 26;

    public WangmishangjiaoEntity(EntityType<? extends WangmishangjiaoEntity> type, Level level) {
        super(type, level, "wangmishangjiao",
                SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundEvents.ELDER_GUARDIAN_HURT,
                SoundEvents.ELDER_GUARDIAN_DEATH, null);
        // 水生移动：平滑 3D 游泳控制（水下可上下追击），并把水中寻路权重归零
        this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.02F, 0.1F, true);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    /** 属性：1000 心 = 2000 血、啃咬 300、巨兽巡游速度 1.0、追击 48、高击退抗性。 */
    public static AttributeSupplier.Builder createAttributes() {
        return bossAttributes(2000.0D, 300.0D, 1.00D, 0.0D, 0.0D, 0.8D, 48.0D);
    }

    @Override
    protected void registerGoals() {
        // 搁浅时优先寻路回水中（26.3 已移除原版 TryFindWaterGoal，这里用自定义回水 Goal 代替）
        this.goalSelector.addGoal(0, new GoToWaterGoal());
        // 发现目标后巨兽冲刺，1.5 倍巡游速度
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, true));
        // 无目标时缓慢水中巡游
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.9D, 40));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 两栖寻路：水中可全向游（含上下层），搁浅时也能在陆地寻回水中。 */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    /** 水生：水下不会溺水。 */
    @Override
    @SuppressWarnings("deprecation") // 26.3 暂未提供该行为的替代钩子
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** 不会被水流推动。 */
    @Override
    @SuppressWarnings("deprecation") // 26.3 暂未提供该行为的替代钩子
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected int attackAnimationTicks() {
        return ATTACK_ANIMATION_TICKS;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.4F, 0.55F);
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
        // 搁浅扑腾：身躯沉重，在陆地上隔几拍费劲儿朝面朝方向蹦一下（同原版鱼离水逻辑）
        if (!this.isInWater() && this.onGround() && this.getRandom().nextFloat() < 0.08F) {
            Vec3 facing = this.getLookAngle();
            this.setDeltaMovement(this.getDeltaMovement()
                    .add(facing.x * 0.22D, 0.45D, facing.z * 0.22D));
            this.setOnGround(false);
            this.needsSync = true;
        }
    }

    /** 搁浅时在附近随机找一格水并寻路过去（行为对齐已移除的原版 TryFindWaterGoal）。 */
    private class GoToWaterGoal extends Goal {

        private double wantedX;
        private double wantedY;
        private double wantedZ;

        GoToWaterGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (WangmishangjiaoEntity.this.isInWater()) {
                return false;
            }
            Vec3 water = this.getWaterPos();
            if (water == null) {
                return false;
            }
            this.wantedX = water.x;
            this.wantedY = water.y;
            this.wantedZ = water.z;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !WangmishangjiaoEntity.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            WangmishangjiaoEntity.this.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, 1.0D);
        }

        /** 10 次随机采样找附近水面（同原版溺尸回水逻辑）。 */
        private Vec3 getWaterPos() {
            BlockPos origin = WangmishangjiaoEntity.this.blockPosition();
            for (int i = 0; i < 10; i++) {
                BlockPos pos = origin.offset(
                        WangmishangjiaoEntity.this.getRandom().nextInt(20) - 10,
                        2 - WangmishangjiaoEntity.this.getRandom().nextInt(8),
                        WangmishangjiaoEntity.this.getRandom().nextInt(20) - 10);
                if (WangmishangjiaoEntity.this.level().isWaterAt(pos)) {
                    return Vec3.atBottomCenterOf(pos);
                }
            }
            return null;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }
}

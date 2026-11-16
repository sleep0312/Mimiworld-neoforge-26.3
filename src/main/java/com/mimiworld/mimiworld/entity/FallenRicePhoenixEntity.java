package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 堕落米凤（Fallen Rice Phoenix，注册 id = fallen_rice_phoenix）——大米主题的陆空敌对凤凰。
 *
 * <p>模型/贴图/动画资源（Blockbench 导出）：
 * geo/fallen_rice_phoenix.geo.json（geometry.duoluomifeng）、
 * textures/entity/fallen_rice_phoenix.png、
 * animations/fallen_rice_phoenix.animation.json（动画键 animation.idle / .walk / .attack / .flying）。
 * 碰撞箱 1.4×2.2。</p>
 *
 * <p>行为向原版幻翼看齐，但多一套陆地作息：</p>
 * <ul>
 *   <li>飞行阶段（{@link PhoenixFlightGoal}）：蹬地升空 → 像幻翼一样在目标头顶绕圈水平巡航、
 *       周期性收翅俯冲啄击（接触结算近战，播 animation.attack），拉起后重新巡航；
 *       无目标时在空中漫游。飞行期间无重力，速度直接按空中巡航模型驱动；</li>
 *   <li>飞行若干轮俯冲后自行落地：落地休息（animation.idle）、地面走动（animation.walk），
 *       玩家贴身时仍会近战啄击；休息结束再次起飞；</li>
 *   <li>2000 血 / 近战 70、满击退抗性、永不摔落受伤，音效全部使用原版幻翼。</li>
 * </ul>
 */
public class FallenRicePhoenixEntity extends AbstractFlyingRiceMobEntity {

    /** 同步数据：是否处于空中飞行状态（同时驱动无重力与 flying 动画切换）。 */
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(FallenRicePhoenixEntity.class, EntityDataSerializers.BOOLEAN);

    /** animation.attack 长 2 秒 = 40 tick。 */
    private static final int ATTACK_ANIMATION_TICKS = 40;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("animation.attack");
    private static final RawAnimation ANIM_FLYING = RawAnimation.begin().thenLoop("animation.flying");

    /** 落地后的休息计时（仅服务端）：到点后 {@link PhoenixFlightGoal} 重新起飞。 */
    private int restTicks = 120;

    public FallenRicePhoenixEntity(EntityType<? extends FallenRicePhoenixEntity> type, Level level) {
        super(type, level);
        // 陆空两栖：落地面恢复重力与普通地面移动控制器（基类默认常驻悬浮）
        this.setNoGravity(false);
        this.moveControl = new MoveControl<>(this);
    }

    /**
     * 属性：2000 血、近战 70；陆地移速 0.3，FLYING_SPEED 1.5 是空中巡航/俯冲的实际速度基准，
     * 追击 40，满击退抗性。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 1.5D)
                .add(Attributes.ATTACK_DAMAGE, 70.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** 地面作息使用普通地面寻路（飞行阶段的速度由 flyToward 直接驱动）。 */
    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 招牌飞行作息（巡航/俯冲/落地）：持有 MOVE+LOOK，激活期间压制地面近战与漫步
        this.goalSelector.addGoal(1, new PhoenixFlightGoal());
        // 落地休息时贴身玩家的近战啄击
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // -------------------- GeckoLib 动画 --------------------

    @Override
    protected RawAnimation idleAnimation() {
        return ANIM_IDLE;
    }

    @Override
    protected RawAnimation moveAnimation() {
        return ANIM_WALK;
    }

    @Override
    protected RawAnimation attackAnimation() {
        return ANIM_ATTACK;
    }

    @Override
    protected int attackAnimationTicks() {
        return ATTACK_ANIMATION_TICKS;
    }

    /** 空中时强制播 flying 循环（优先级低于俯冲啄击，高于地面 walk/idle）。 */
    @Override
    protected RawAnimation skillAnimation() {
        return this.isFlying() ? ANIM_FLYING : null;
    }

    // -------------------- 飞行状态同步 --------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLYING, false);
    }

    /** 客户端同步到飞行状态时同步无重力，避免客户端预测时往下掉。 */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_FLYING.equals(key)) {
            this.setNoGravity(this.entityData.get(DATA_FLYING));
        }
    }

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    /** 切换飞行状态：服务端同时开关重力；客户端由 {@link #onSyncedDataUpdated} 跟进。 */
    void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
        this.setNoGravity(flying);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && !this.isFlying() && this.restTicks > 0) {
            this.restTicks--;
        }
    }

    // -------------------- 音效：全部使用原版幻翼 --------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    // -------------------- 飞行物理辅助（供 PhoenixFlightGoal 使用） --------------------

    /**
     * 空中巡航移动：把速度朝目标点做平滑插值（无重力环境），近似原版 FlyingMoveControl 的手感。
     *
     * @param factor    相对 FLYING_SPEED 的速度系数（巡航 0.5、俯冲 1.7、降落 0.35）
     * @param levelLook true 时视线保持水平（垂直起降/降落阶段，避免朝上/朝下拧头）
     */
    private void flyToward(Vec3 wanted, double factor, boolean levelLook) {
        Vec3 current = this.getDeltaMovement();
        Vec3 diff = wanted.subtract(this.position());
        double dist = diff.length();
        double speed = this.getAttributeValue(Attributes.FLYING_SPEED) * factor;
        Vec3 goalVelocity = dist < 0.05D
                ? new Vec3(0.0D, current.y * 0.5D, 0.0D)
                : diff.scale(speed / dist);
        this.setDeltaMovement(current.add(goalVelocity.subtract(current).scale(0.14D)));
        if (levelLook) {
            float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
            this.getLookControl().setLookAt(
                    this.getX() - Mth.sin(yawRad) * 3.0D, this.getY(),
                    this.getZ() + Mth.cos(yawRad) * 3.0D, 10.0F, 90.0F);
        } else {
            this.getLookControl().setLookAt(wanted.x, wanted.y, wanted.z, 12.0F, 90.0F);
        }
    }

    /**
     * 从给定高度向下找可降落点：顶面承托的实体方块，且上方两格无碰撞（容得下身体）。
     * 找不到返回 {@link Double#NaN}。
     */
    private double findGroundY(double x, double z, double fromY) {
        Level level = this.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                Mth.floor(x), Mth.ceil(fromY), Mth.floor(z));
        int minY = level.getMinY() + 1;
        while (pos.getY() > minY) {
            BlockState state = level.getBlockState(pos);
            if (state.isFaceSturdy(level, pos, Direction.UP)) {
                BlockPos above1 = pos.above();
                BlockPos above2 = pos.above(2);
                if (level.getBlockState(above1).getCollisionShape(level, above1).isEmpty()
                        && level.getBlockState(above2).getCollisionShape(level, above2).isEmpty()) {
                    return pos.getY() + 1.0D;
                }
            }
            pos.move(Direction.DOWN);
        }
        return Double.NaN;
    }

    // -------------------- 自定义飞行作息 AI --------------------

    /**
     * 幻翼式「起飞 → 巡航/俯冲 → 落地」状态机。
     *
     * <p>TAKEOFF：蹬地垂直拉高；有目标进 CRUISE，无目标进 WANDER。<br>
     * CRUISE：在目标头顶约 10 格、上方 9 格绕圈水平飞行，冷却结束且视线通畅时 SWOOP。<br>
     * SWOOP：收翅高速俯冲突脸，贴身结算一次近战（attack 动画在进入俯冲时起播），随后 RECOVER。<br>
     * RECOVER：斜上拉起，俯冲次数用尽或飞行时间到则 LAND，否则重新 CRUISE/WANDER。<br>
     * WANDER：无目标时高空漫游，飞行时长足够后同样落地。<br>
     * LAND：在当前位置正下方找实体方块缓慢下降，触地即关闭飞行（切回地面 AI 与 idle/walk）。</p>
     */
    private class PhoenixFlightGoal extends Goal {
        private static final int TAKEOFF = 0;
        private static final int CRUISE = 1;
        private static final int SWOOP = 2;
        private static final int RECOVER = 3;
        private static final int WANDER = 4;
        private static final int LAND = 5;

        private static final double CIRCLE_RADIUS = 10.0D;
        private static final double CIRCLE_HEIGHT = 9.0D;
        private static final double HIT_RADIUS_SQR = 6.5D;
        private static final int TAKEOFF_TIME = 22;
        private static final int SWOOP_TIMEOUT = 58;
        private static final int RECOVER_TIME = 28;

        private int phase = TAKEOFF;
        private int phaseTime;
        private int airTime;
        private int maxAirTime;
        private int swoopCount;
        private int maxSwoops;
        private int attackCooldown = 40;
        private int flapSoundTicks;
        private double circleAngle;
        private Vec3 wanderTarget = Vec3.ZERO;
        private double landY = Double.NaN;

        PhoenixFlightGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !FallenRicePhoenixEntity.this.isFlying() && FallenRicePhoenixEntity.this.restTicks <= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return FallenRicePhoenixEntity.this.isFlying();
        }

        @Override
        public void start() {
            FallenRicePhoenixEntity phoenix = FallenRicePhoenixEntity.this;
            phoenix.setFlying(true);
            this.phase = TAKEOFF;
            this.phaseTime = 0;
            this.airTime = 0;
            this.swoopCount = 0;
            this.maxSwoops = 2 + phoenix.random.nextInt(2);
            // 一轮空中活动 18~33 秒，避免无限飞也避免太频繁落地
            this.maxAirTime = 360 + phoenix.random.nextInt(300);
            this.attackCooldown = 40;
            this.flapSoundTicks = 10;
            this.circleAngle = phoenix.random.nextDouble() * Math.PI * 2.0D;
            phoenix.setDeltaMovement(0.0D, 0.55D, 0.0D);
            phoenix.playSound(SoundEvents.PHANTOM_FLAP, 1.0F, 1.1F);
        }

        @Override
        public void stop() {
            this.phase = TAKEOFF;
            this.phaseTime = 0;
            this.landY = Double.NaN;
        }

        @Override
        public void tick() {
            FallenRicePhoenixEntity phoenix = FallenRicePhoenixEntity.this;
            LivingEntity target = phoenix.getTarget();
            this.airTime++;
            this.phaseTime++;

            // 飞行中周期性播放幻翼振翅声
            if (this.phase != LAND) {
                this.flapSoundTicks--;
                if (this.flapSoundTicks <= 0) {
                    phoenix.playSound(SoundEvents.PHANTOM_FLAP, 0.8F, 1.0F + phoenix.random.nextFloat() * 0.25F);
                    this.flapSoundTicks = 34 + phoenix.random.nextInt(22);
                }
            }

            switch (this.phase) {
                case TAKEOFF -> {
                    phoenix.flyToward(new Vec3(phoenix.getX(), phoenix.getY() + 7.0D, phoenix.getZ()), 0.55D, true);
                    if (this.phaseTime >= TAKEOFF_TIME) {
                        this.phase = (target != null) ? CRUISE : WANDER;
                        this.phaseTime = 0;
                        if (this.phase == WANDER) {
                            this.pickWanderTarget(phoenix);
                        }
                    }
                }
                case CRUISE -> this.tickCruise(phoenix, target);
                case SWOOP -> this.tickSwoop(phoenix, target);
                case RECOVER -> this.tickRecover(phoenix, target);
                case WANDER -> this.tickWander(phoenix, target);
                default -> this.tickLand(phoenix);
            }
        }

        /** 巡航：绕目标头顶缓慢画圈，冷却结束即俯冲。 */
        private void tickCruise(FallenRicePhoenixEntity phoenix, LivingEntity target) {
            if (target == null) {
                this.phase = WANDER;
                this.phaseTime = 0;
                this.pickWanderTarget(phoenix);
                return;
            }
            this.circleAngle += 0.03D;
            double cx = target.getX() + Math.cos(this.circleAngle) * CIRCLE_RADIUS;
            double cz = target.getZ() + Math.sin(this.circleAngle) * CIRCLE_RADIUS;
            double cy = Math.max(target.getY() + CIRCLE_HEIGHT, phoenix.level().getMinY() + 5.0D);
            phoenix.flyToward(new Vec3(cx, cy, cz), 0.5D, false);

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            double distSqr = phoenix.distanceToSqr(target);
            if (this.attackCooldown <= 0 && distSqr < 26.0D * 26.0D && phoenix.hasLineOfSight(target)) {
                this.beginSwoop(phoenix);
            } else if (this.airTime >= this.maxAirTime) {
                this.beginLand(phoenix);
            }
        }

        /** 俯冲：全速直取目标，接触结算近战。 */
        private void tickSwoop(FallenRicePhoenixEntity phoenix, LivingEntity target) {
            if (target == null) {
                this.beginRecover();
                return;
            }
            phoenix.flyToward(new Vec3(target.getX(), target.getY(0.4D), target.getZ()), 1.7D, false);

            double verticalGap = Math.abs(target.getY() - phoenix.getY());
            if (verticalGap < 3.0D
                    && phoenix.distanceToSqr(target) <= HIT_RADIUS_SQR
                    && phoenix.hasLineOfSight(target)
                    && phoenix.level() instanceof ServerLevel serverLevel
                    && phoenix.doHurtTarget(serverLevel, target)) {
                this.swoopCount++;
                this.beginRecover();
                return;
            }
            // 超时，或冲到目标下方还没撞到：强制拉起，避免贴地乱窜
            boolean overshot = this.phaseTime > 45 && phoenix.getY() < target.getY() - 0.5D;
            if (this.phaseTime >= SWOOP_TIMEOUT || overshot) {
                this.beginRecover();
            }
        }

        /** 拉起：原地垂直爬高，之后视剩余俯冲次数/飞行时长决定去向。 */
        private void tickRecover(FallenRicePhoenixEntity phoenix, LivingEntity target) {
            phoenix.flyToward(new Vec3(phoenix.getX(), phoenix.getY() + 7.0D, phoenix.getZ()), 0.6D, true);
            if (this.phaseTime >= RECOVER_TIME) {
                if (this.swoopCount >= this.maxSwoops || this.airTime >= this.maxAirTime) {
                    this.beginLand(phoenix);
                } else if (target != null) {
                    this.phase = CRUISE;
                    this.phaseTime = 0;
                    this.attackCooldown = 50 + phoenix.random.nextInt(40);
                } else {
                    this.phase = WANDER;
                    this.phaseTime = 0;
                    this.pickWanderTarget(phoenix);
                }
            }
        }

        /** 无目标空中漫游：朝随机悬空点飞，一个漫游段结束后有概率提前落地。 */
        private void tickWander(FallenRicePhoenixEntity phoenix, LivingEntity target) {
            if (target != null) {
                this.phase = CRUISE;
                this.phaseTime = 0;
                return;
            }
            phoenix.flyToward(this.wanderTarget, 0.45D, false);
            boolean reached = phoenix.position().distanceToSqr(this.wanderTarget) < 4.0D;
            if (reached || this.phaseTime > 220) {
                if (this.airTime >= this.maxAirTime
                        || (this.airTime > 200 && phoenix.random.nextBoolean())) {
                    this.beginLand(phoenix);
                } else {
                    this.pickWanderTarget(phoenix);
                    this.phaseTime = 0;
                }
            }
        }

        /** 降落：垂直压到正下方实体方块表面，触地即结束飞行、进入地面休息。 */
        private void tickLand(FallenRicePhoenixEntity phoenix) {
            if (Double.isNaN(this.landY)) {
                // 脚下找不到落点：回空中重新漫游，避免一直沉到虚空
                this.phase = WANDER;
                this.phaseTime = 0;
                this.pickWanderTarget(phoenix);
                return;
            }
            phoenix.flyToward(new Vec3(phoenix.getX(), this.landY, phoenix.getZ()), 0.35D, true);
            if (phoenix.onGround() || Math.abs(phoenix.getY() - this.landY) < 0.15D) {
                Vec3 velocity = phoenix.getDeltaMovement();
                phoenix.setDeltaMovement(velocity.x * 0.3D, 0.0D, velocity.z * 0.3D);
                phoenix.setFlying(false);
                // 落地后休息 7~16 秒，期间由地面 AI 播 idle/walk
                phoenix.restTicks = 140 + phoenix.random.nextInt(180);
            }
        }

        private void beginSwoop(FallenRicePhoenixEntity phoenix) {
            this.phase = SWOOP;
            this.phaseTime = 0;
            // 俯冲一开始就起播 2s 啄击动画（蓄力→展翅→刺喙的节奏与 SWOOP_TIMEOUT 对齐）
            phoenix.playAttackAnimation(ATTACK_ANIMATION_TICKS);
            phoenix.playSound(SoundEvents.PHANTOM_SWOOP, 1.2F, 0.9F);
        }

        private void beginRecover() {
            this.phase = RECOVER;
            this.phaseTime = 0;
        }

        private void beginLand(FallenRicePhoenixEntity phoenix) {
            this.landY = phoenix.findGroundY(phoenix.getX(), phoenix.getZ(), phoenix.getY() + 2.0D);
            this.phase = LAND;
            this.phaseTime = 0;
        }

        /** 选一个水平 ±14、地面以上 5~10 格的悬空漫游点。 */
        private void pickWanderTarget(FallenRicePhoenixEntity phoenix) {
            Level level = phoenix.level();
            for (int attempt = 0; attempt < 4; attempt++) {
                double tx = phoenix.getX() + (phoenix.random.nextDouble() * 2.0D - 1.0D) * 14.0D;
                double tz = phoenix.getZ() + (phoenix.random.nextDouble() * 2.0D - 1.0D) * 14.0D;
                double groundY = phoenix.findGroundY(tx, tz, phoenix.getY() + 4.0D);
                if (!Double.isNaN(groundY)) {
                    double ty = Mth.clamp(groundY + 5.0D + phoenix.random.nextDouble() * 5.0D,
                            level.getMinY() + 4.0D, level.getMaxY() - 4.0D);
                    this.wanderTarget = new Vec3(tx, ty, tz);
                    return;
                }
            }
            // 周围找不到合适落点：在当前高度原地悬停等待下一轮决策
            this.wanderTarget = phoenix.position();
        }
    }
}

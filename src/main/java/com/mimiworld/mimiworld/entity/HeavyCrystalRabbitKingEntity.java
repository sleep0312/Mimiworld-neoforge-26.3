package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;
import com.mimiworld.mimiworld.effect.ModEffects;
import com.mimiworld.mimiworld.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
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
 * 重晶兔王（Heavy Crystal Rabbit King，注册 id = heavy_crystal_rabbit_king）——晶化巨兔陆地 Boss。
 *
 * <p>动画：animation.heavy_crystal_rabbit_king.idle / walk / attack / skill_slam / skill_enrage。
 * 碰撞箱 1.5×3.2，火焰免疫。</p>
 *
 * <p>三种攻击方式：</p>
 * <ol>
 *   <li>普通近战：MeleeAttackGoal 驱动，attack 动画 26 tick。</li>
 *   <li>【出其不意】（skill_slam）：原地蓄力 20 tick 后跃起 10 格高，于第 42 tick 砸向地面，
 *       落点周围 4 格内所有生物受到 45 点无视护甲伤害（自定义 rabbit_king_slam 伤害类型）
 *       与强击退，伴随爆炸烟尘与巨响；冷却 1200 tick。
 *       空中 22 tick 的位移用对称抛物线脚本驱动（初速 1.7355、重力 0.1653/tick），
 *       第 11 tick 到达 10 格顶点、第 22 tick 落回起跳高度，与动画砸地时刻对齐。</li>
 *   <li>【腥红烛照】（skill_enrage）：咆哮 36 tick，第 11 tick 获得 600 tick
 *       腥红烛照效果（crimson_candlelight，攻击伤害 ×3，含跃砸），冷却 3600 tick。</li>
 * </ol>
 *
 * <p>技能期间通过 Goal 互斥标志（MOVE+LOOK）锁定移动与普攻；动画优先级
 * 跃砸 &gt; 咆哮增益 &gt; 普攻 &gt; 行走 &gt; 待机。</p>
 */
public class HeavyCrystalRabbitKingEntity extends AbstractRiceGeoMonsterEntity {

    // -------------------- 同步动画/增益状态（服务端递减，自动同步客户端） --------------------

    private static final EntityDataAccessor<Integer> DATA_SLAM_ANIM_TICKS =
            SynchedEntityData.defineId(HeavyCrystalRabbitKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ENRAGE_ANIM_TICKS =
            SynchedEntityData.defineId(HeavyCrystalRabbitKingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ENRAGE_BUFF_TICKS =
            SynchedEntityData.defineId(HeavyCrystalRabbitKingEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation ANIM_SLAM =
            RawAnimation.begin().thenPlay("animation.heavy_crystal_rabbit_king.skill_slam");
    private static final RawAnimation ANIM_ENRAGE =
            RawAnimation.begin().thenPlay("animation.heavy_crystal_rabbit_king.skill_enrage");

    /** 普攻 attack 动画 1.3 秒 = 26 tick */
    private static final int ATTACK_ANIMATION_TICKS = 26;

    /** 【出其不意】skill_slam 动画 = 52 tick（蓄力 20 + 滞空 22 + 恢复 10） */
    private static final int SLAM_ANIMATION_TICKS = 52;
    /** 蓄力时长：1 秒 = 20 tick */
    private static final int SLAM_WINDUP_TICKS = 20;
    /** 砸地命中帧：蓄力 20 + 滞空 22 = 第 42 tick（2.1 秒） */
    private static final int SLAM_IMPACT_TICK = 42;
    /** 空中飞行 tick 数：42-20 = 22 tick（1.1 秒） */
    private static final int SLAM_LEAP_TICKS = SLAM_IMPACT_TICK - SLAM_WINDUP_TICKS;
    /** 跳跃顶点高度：10 格 */
    private static final double SLAM_JUMP_HEIGHT = 10.0D;
    /**
     * 对称抛物线脚本参数（22 tick 回到起跳高度、第 11 tick 到顶点 10 格）：
     * 第 t 个空中 tick 的垂直速度 vy = V0 - G*(t-1)。
     */
    private static final double SLAM_LEAP_GRAVITY = SLAM_JUMP_HEIGHT / 60.5D;
    private static final double SLAM_LEAP_V0 = SLAM_LEAP_GRAVITY * 10.5D;
    /** 起跳水平位移上限：5 格 */
    private static final double SLAM_LEAP_HORIZONTAL_LIMIT = 5.0D;
    /** 砸地伤害：45 点无视护甲 */
    private static final float SLAM_DAMAGE = 45.0F;
    /** 砸地 AOE 半径：4 格 */
    private static final double SLAM_RADIUS = 4.0D;
    /** 砸地冷却：60 秒 = 1200 tick */
    private static final int SLAM_COOLDOWN_TICKS = 1200;
    /** 跃砸释放距离：2.5~10 格 */
    private static final double SLAM_MIN_DISTANCE_SQR = 6.25D;
    private static final double SLAM_MAX_DISTANCE_SQR = 100.0D;

    /** 【腥红烛照】skill_enrage 动画 1.8 秒 = 36 tick */
    private static final int ENRAGE_ANIMATION_TICKS = 36;
    /** 增益施加时刻：动画第 11 tick（咆哮顶点） */
    private static final int ENRAGE_APPLY_TICK = 11;
    /** 猩红增益持续：30 秒 = 600 tick */
    private static final int ENRAGE_BUFF_DURATION = 600;
    /** 腥红烛照冷却：180 秒 = 3600 tick */
    private static final int ENRAGE_COOLDOWN_TICKS = 3600;

    /** 首次技能前的准备时间，避免出生瞬间立刻出招 */
    private static final int INITIAL_SLAM_DELAY = 100;
    private static final int INITIAL_ENRAGE_DELAY = 140;

    // 技能冷却（仅服务端计时）
    private int slamCooldown = INITIAL_SLAM_DELAY;
    private int enrageCooldown = INITIAL_ENRAGE_DELAY;

    public HeavyCrystalRabbitKingEntity(EntityType<? extends HeavyCrystalRabbitKingEntity> type, Level level) {
        super(type, level, "heavy_crystal_rabbit_king",
                ModSounds.HEAVY_CRYSTAL_RABBIT_KING_AMBIENT.get(),
                ModSounds.HEAVY_CRYSTAL_RABBIT_KING_HURT.get(),
                ModSounds.HEAVY_CRYSTAL_RABBIT_KING_DEATH.get(),
                ModSounds.HEAVY_CRYSTAL_RABBIT_KING_ROAR.get());
    }

    /**
     * 属性：500 血、移速 0.34、普攻 30、护甲 25、韧性 6、70% 击退抗性、追击 40。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return bossAttributes(500.0D, 30.0D, 0.34D, 25.0D, 6.0D, 0.7D, 40.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLAM_ANIM_TICKS, 0);
        builder.define(DATA_ENRAGE_ANIM_TICKS, 0);
        builder.define(DATA_ENRAGE_BUFF_TICKS, 0);
    }

    /**
     * AI：0 漂浮；1 出其不意（跃砸）；2 腥红烛照（咆哮增益）；3 普攻追击；
     * 5 避水游荡；6 看玩家；7 张望。三个攻击 Goal 共用 MOVE+LOOK 标志，天然互斥。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SlamGoal());
        this.goalSelector.addGoal(2, new EnrageGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected int attackAnimationTicks() {
        return ATTACK_ANIMATION_TICKS;
    }

    @Override
    protected RawAnimation skillAnimation() {
        if (this.entityData.get(DATA_SLAM_ANIM_TICKS) > 0) {
            return ANIM_SLAM;
        }
        if (this.entityData.get(DATA_ENRAGE_ANIM_TICKS) > 0) {
            return ANIM_ENRAGE;
        }
        return null;
    }

    @Override
    protected boolean suppressGenericHeavyAttack() {
        return true;
    }

    /** 是否正在释放技能（跃砸/咆哮增益）。普攻动画不计入。 */
    private boolean isCasting() {
        return this.entityData.get(DATA_SLAM_ANIM_TICKS) > 0
                || this.entityData.get(DATA_ENRAGE_ANIM_TICKS) > 0;
    }

    /** 普攻命中时额外获得短暂的高额生命恢复与加速。 */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 1));
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1));
            return true;
        }
        return false;
    }

    /** 常驻自增益：生命恢复 II + 力量 II（每 40 tick 续接一次）。 */
    private void maintainPassiveBuffs() {
        if (this.tickCount % 40 != 0) {
            return;
        }
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 1));
        this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 1));
    }

    /**
     * 【出其不意】砸地结算：以落点为中心、半径 4 格内的所有活体（排除自身与同盟）
     * 受到 45 点无视护甲伤害与向上/向外的强击退，播放爆炸烟尘与巨响。
     */
    private void performSlamImpact() {
        DamageSource source = ModDamageTypes.source(this.level(), ModDamageTypes.RABBIT_KING_SLAM, this, this);
        AABB searchArea = this.getBoundingBox().inflate(SLAM_RADIUS + 1.0D, 3.0D, SLAM_RADIUS + 1.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                target -> target != this && target.isAlive() && !target.isAlliedTo(this));
        for (LivingEntity target : targets) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double distSqr = dx * dx + dz * dz;
            boolean verticalHit = target.getBoundingBox().maxY >= this.getY() - 1.0D
                    && target.getBoundingBox().minY <= this.getY() + 3.5D;
            if (distSqr <= SLAM_RADIUS * SLAM_RADIUS && verticalHit) {
                ModDamage.hurt(target, source, SLAM_DAMAGE);
                double dist = Math.sqrt(distSqr);
                if (dist > 1.0E-4D) {
                    target.push(dx / dist * 1.35D, 0.55D, dz / dist * 1.35D);
                } else {
                    target.push((this.random.nextDouble() - 0.5D) * 0.6D, 0.55D,
                            (this.random.nextDouble() - 0.5D) * 0.6D);
                }
            }
        }

        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.55F + this.random.nextFloat() * 0.1F);
        this.playSound(SoundEvents.RAVAGER_STEP, 1.2F, 0.6F);

        if (this.level() instanceof ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY() + 0.2D;
            double z = this.getZ();
            // 落点中心一次大爆炸闪光
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            // 地面环形冲击尘
            for (int i = 0; i < 32; i++) {
                double angle = Math.PI * 2.0D * i / 32.0D + this.random.nextDouble() * 0.2D;
                double ring = 1.0D + this.random.nextDouble() * (SLAM_RADIUS - 1.2D);
                double px = x + Math.cos(angle) * ring;
                double pz = z + Math.sin(angle) * ring;
                serverLevel.sendParticles(ParticleTypes.POOF, px, y + 0.1D, pz,
                        1, 0.05D, 0.05D, 0.05D, 0.02D);
            }
            // 上扬黑烟
            for (int i = 0; i < 14; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double ring = this.random.nextDouble() * 2.5D;
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        x + Math.cos(angle) * ring, y + 0.3D + this.random.nextDouble() * 0.8D,
                        z + Math.sin(angle) * ring,
                        1, 0.05D, 0.12D, 0.05D, 0.02D);
            }
        }
    }

    /**
     * 施加【腥红烛照】效果：crimson_candlelight 持续 600 tick（amplifier 与 1.20.1 源实现一致为 0，
     * DamagePipeline 每级 +100% 即 ×2；若需源 javadoc 描述的 ×3 应改用 amplifier 1）。
     */
    private void applyEnrageBuff() {
        this.entityData.set(DATA_ENRAGE_BUFF_TICKS, ENRAGE_BUFF_DURATION);
        this.addEffect(new MobEffectInstance(ModEffects.CRIMSON_CANDLELIGHT, ENRAGE_BUFF_DURATION, 0));
    }

    /** 猩红增益结束：移除药水效果（效果到期时也会自行消失，这里做兜底）。 */
    private void removeEnrageBuff() {
        this.removeEffect(ModEffects.CRIMSON_CANDLELIGHT);
    }

    @Override
    protected void bossTick() {
        this.maintainPassiveBuffs();
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }
        if (this.enrageCooldown > 0) {
            this.enrageCooldown--;
        }
        int slamTicks = this.entityData.get(DATA_SLAM_ANIM_TICKS);
        if (slamTicks > 0) {
            this.entityData.set(DATA_SLAM_ANIM_TICKS, slamTicks - 1);
        }
        int enrageAnimTicks = this.entityData.get(DATA_ENRAGE_ANIM_TICKS);
        if (enrageAnimTicks > 0) {
            this.entityData.set(DATA_ENRAGE_ANIM_TICKS, enrageAnimTicks - 1);
        }
        int enrageBuffTicks = this.entityData.get(DATA_ENRAGE_BUFF_TICKS);
        if (enrageBuffTicks > 0) {
            this.entityData.set(DATA_ENRAGE_BUFF_TICKS, enrageBuffTicks - 1);
            if (enrageBuffTicks - 1 == 0) {
                this.removeEnrageBuff();
            }
        }
    }

    /** 脚本驱动的高空跳跃是受控运动，落地不得受到摔伤。 */
    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    // -------------------- 技能 Goal --------------------

    /**
     * 【出其不意】Goal：目标在 2.5~10 格内且有视线时释放。
     * 前 20 tick 原地蓄力（锁移动、盯紧目标），随后锁定目标方向跃起：
     * 空中 22 tick 按对称抛物线脚本运动（第 11 tick 到 10 格顶点、水平朝目标滑移最多 5 格），
     * 第 42 tick 落回地面并结算 45 点无视护甲 AOE；43~52 tick 为起身恢复段。
     * 一旦起跳不可中断；冷却 1200 tick。
     */
    private class SlamGoal extends Goal {
        private int runTicks;
        private boolean leaped;
        private float leapYaw;
        private double leapVx;
        private double leapVz;
        private double leapStartY;

        SlamGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = HeavyCrystalRabbitKingEntity.this.getTarget();
            return HeavyCrystalRabbitKingEntity.this.slamCooldown <= 0
                    && !HeavyCrystalRabbitKingEntity.this.isCasting()
                    && target != null && target.isAlive()
                    && HeavyCrystalRabbitKingEntity.this.distanceToSqr(target) >= SLAM_MIN_DISTANCE_SQR
                    && HeavyCrystalRabbitKingEntity.this.distanceToSqr(target) <= SLAM_MAX_DISTANCE_SQR
                    && HeavyCrystalRabbitKingEntity.this.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            if (this.runTicks >= SLAM_ANIMATION_TICKS) {
                return false;
            }
            // 蓄力阶段目标丢失/死亡则允许中断；起跳后必须把整套动画演完
            if (!this.leaped) {
                LivingEntity target = HeavyCrystalRabbitKingEntity.this.getTarget();
                return target != null && target.isAlive();
            }
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.runTicks = 0;
            this.leaped = false;
            HeavyCrystalRabbitKingEntity.this.getNavigation().stop();
            HeavyCrystalRabbitKingEntity.this.entityData.set(DATA_SLAM_ANIM_TICKS, SLAM_ANIMATION_TICKS);
        }

        @Override
        public void tick() {
            this.runTicks++;
            HeavyCrystalRabbitKingEntity.this.getNavigation().stop();

            if (this.runTicks <= SLAM_WINDUP_TICKS) {
                // —— 蓄力阶段：原地蹲伏（动画表现），盯紧目标 ——
                LivingEntity target = HeavyCrystalRabbitKingEntity.this.getTarget();
                if (target != null) {
                    HeavyCrystalRabbitKingEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                if (this.runTicks == SLAM_WINDUP_TICKS) {
                    HeavyCrystalRabbitKingEntity.this.playSound(SoundEvents.RAVAGER_AMBIENT, 0.8F,
                            0.7F + HeavyCrystalRabbitKingEntity.this.random.nextFloat() * 0.1F);
                    this.launchLeap();
                }
                return;
            }

            // —— 空中阶段：对称抛物线脚本位移，第 42 tick 落地 ——
            int leapTick = this.runTicks - SLAM_WINDUP_TICKS;
            HeavyCrystalRabbitKingEntity.this.setYRot(this.leapYaw);
            HeavyCrystalRabbitKingEntity.this.yBodyRot = this.leapYaw;
            HeavyCrystalRabbitKingEntity.this.setYHeadRot(this.leapYaw);

            if (leapTick <= SLAM_LEAP_TICKS) {
                double vy = SLAM_LEAP_V0 - SLAM_LEAP_GRAVITY * (leapTick - 1);
                Vec3 moveVec = new Vec3(this.leapVx, vy, this.leapVz);
                HeavyCrystalRabbitKingEntity.this.setDeltaMovement(moveVec);
                HeavyCrystalRabbitKingEntity.this.move(MoverType.SELF, moveVec);

                if (this.leapStartY > 0.0D
                        && HeavyCrystalRabbitKingEntity.this.level() instanceof ServerLevel serverLevel) {
                    // 拖尾晶尘
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            HeavyCrystalRabbitKingEntity.this.getX(),
                            HeavyCrystalRabbitKingEntity.this.getY() + 1.2D,
                            HeavyCrystalRabbitKingEntity.this.getZ(),
                            2, 0.3D, 0.2D, 0.3D, 0.0D);
                }
            }

            if (this.runTicks == SLAM_IMPACT_TICK) {
                // 浮点/碰撞兜底：未正常落回起跳高度则校正，保证砸地发生在地面
                if (!HeavyCrystalRabbitKingEntity.this.onGround()
                        && Math.abs(HeavyCrystalRabbitKingEntity.this.getY() - this.leapStartY) > 0.6D) {
                    HeavyCrystalRabbitKingEntity.this.setPos(
                            HeavyCrystalRabbitKingEntity.this.getX(),
                            this.leapStartY,
                            HeavyCrystalRabbitKingEntity.this.getZ());
                }
                HeavyCrystalRabbitKingEntity.this.setDeltaMovement(Vec3.ZERO);
                HeavyCrystalRabbitKingEntity.this.performSlamImpact();
            }
        }

        /** 蓄力结束瞬间起跳：锁定朝向目标，计算最多 5 格的水平滑移速度。 */
        private void launchLeap() {
            this.leaped = true;
            this.leapStartY = HeavyCrystalRabbitKingEntity.this.getY();
            LivingEntity target = HeavyCrystalRabbitKingEntity.this.getTarget();
            double dirX = 0.0D;
            double dirZ = 1.0D;
            if (target != null) {
                dirX = target.getX() - HeavyCrystalRabbitKingEntity.this.getX();
                dirZ = target.getZ() - HeavyCrystalRabbitKingEntity.this.getZ();
            }
            double horiz = Math.hypot(dirX, dirZ);
            if (horiz > 1.0E-4D) {
                dirX /= horiz;
                dirZ /= horiz;
            } else {
                dirX = 0.0D;
                dirZ = 1.0D;
            }
            double travel = Math.min(horiz, SLAM_LEAP_HORIZONTAL_LIMIT);
            this.leapVx = dirX * travel / SLAM_LEAP_TICKS;
            this.leapVz = dirZ * travel / SLAM_LEAP_TICKS;
            this.leapYaw = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));
            HeavyCrystalRabbitKingEntity.this.setYRot(this.leapYaw);
            HeavyCrystalRabbitKingEntity.this.yBodyRot = this.leapYaw;
            HeavyCrystalRabbitKingEntity.this.setYHeadRot(this.leapYaw);
            HeavyCrystalRabbitKingEntity.this.resetFallDistance();
            HeavyCrystalRabbitKingEntity.this.playSound(
                    ModSounds.HEAVY_CRYSTAL_RABBIT_KING_ROAR.get(), 1.3F, 1.15F);
        }

        @Override
        public void stop() {
            this.runTicks = 0;
            this.leaped = false;
            this.leapStartY = 0.0D;
            HeavyCrystalRabbitKingEntity.this.setDeltaMovement(Vec3.ZERO);
            HeavyCrystalRabbitKingEntity.this.entityData.set(DATA_SLAM_ANIM_TICKS, 0);
            HeavyCrystalRabbitKingEntity.this.slamCooldown = SLAM_COOLDOWN_TICKS;
        }
    }

    /**
     * 【腥红烛照】Goal：目标在 16 格内且有视线时原地咆哮 36 tick，
     * 第 11 tick（动画咆哮顶点）施加 600 tick 腥红增益；冷却 3600 tick。
     */
    private class EnrageGoal extends Goal {
        private int runTicks;

        EnrageGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = HeavyCrystalRabbitKingEntity.this.getTarget();
            return HeavyCrystalRabbitKingEntity.this.enrageCooldown <= 0
                    && !HeavyCrystalRabbitKingEntity.this.isCasting()
                    && target != null && target.isAlive()
                    && HeavyCrystalRabbitKingEntity.this.distanceToSqr(target) <= 256.0D
                    && HeavyCrystalRabbitKingEntity.this.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return this.runTicks < ENRAGE_ANIMATION_TICKS;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.runTicks = 0;
            HeavyCrystalRabbitKingEntity.this.getNavigation().stop();
            HeavyCrystalRabbitKingEntity.this.entityData.set(DATA_ENRAGE_ANIM_TICKS, ENRAGE_ANIMATION_TICKS);
        }

        @Override
        public void tick() {
            this.runTicks++;
            HeavyCrystalRabbitKingEntity.this.getNavigation().stop();
            LivingEntity target = HeavyCrystalRabbitKingEntity.this.getTarget();
            if (target != null) {
                HeavyCrystalRabbitKingEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            if (this.runTicks == ENRAGE_APPLY_TICK) {
                HeavyCrystalRabbitKingEntity.this.applyEnrageBuff();
                HeavyCrystalRabbitKingEntity.this.playSound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.85F);
                if (HeavyCrystalRabbitKingEntity.this.level() instanceof ServerLevel serverLevel) {
                    double x = HeavyCrystalRabbitKingEntity.this.getX();
                    double y = HeavyCrystalRabbitKingEntity.this.getY() + 2.4D;
                    double z = HeavyCrystalRabbitKingEntity.this.getZ();
                    for (int i = 0; i < 18; i++) {
                        double angle = Math.PI * 2.0D * i / 18.0D;
                        serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                                x + Math.cos(angle) * 1.6D, y, z + Math.sin(angle) * 1.6D,
                                1, 0.1D, 0.15D, 0.1D, 0.03D);
                    }
                    for (int i = 0; i < 8; i++) {
                        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                                x + (HeavyCrystalRabbitKingEntity.this.random.nextDouble() - 0.5D) * 1.4D,
                                y + HeavyCrystalRabbitKingEntity.this.random.nextDouble() * 1.2D,
                                z + (HeavyCrystalRabbitKingEntity.this.random.nextDouble() - 0.5D) * 1.4D,
                                1, 0.0D, 0.05D, 0.0D, 0.0D);
                    }
                }
            }
        }

        @Override
        public void stop() {
            this.runTicks = 0;
            HeavyCrystalRabbitKingEntity.this.entityData.set(DATA_ENRAGE_ANIM_TICKS, 0);
            HeavyCrystalRabbitKingEntity.this.enrageCooldown = ENRAGE_COOLDOWN_TICKS;
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.3F, 0.9F);
    }
}

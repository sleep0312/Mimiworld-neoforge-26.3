package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * 龙系超巨型陆空 Boss 通用基类。
 *
 * <p>GeckoLib 动画（两个子类共用同一套动画键）：
 * circling（盘旋循环）/ dive_strike（冲撞）/ dragon_breath（龙息）/
 * laser_beam（光束循环）/ melee_bite（撕咬普攻）/ hurt_roar（受伤嘶吼）。</p>
 *
 * <p>技能：</p>
 * <ol>
 *   <li><b>龙息</b>（CD 30s）：动画第 17 tick 在目标处生成末影龙紫色龙息云，
 *       云内每秒结算一次 {@value #BREATH_DAMAGE} 点无视护甲伤害（{@link ModDamageTypes#DRAGON_BREATH}）。</li>
 *   <li><b>巨龙冲撞</b>（CD 60s）：10 tick 蓄力后脚本化 2.4 格/tick 直线冲撞，
 *       动画第 22 tick 对沿途玩家造成 {@value #CHARGE_DAMAGE} 点伤害与强力击退（{@link ModDamageTypes#DRAGON_CHARGE}）。</li>
 *   <li><b>最懒光束</b>（CD 120s）：飞抵玩家斜上方后悬停照射 60s，
 *       每 0.5s 一次无遮挡射线判定，命中造成 {@value #BEAM_DAMAGE} 点伤害（{@link ModDamageTypes#DRAGON_BEAM}），
 *       目标死亡自动换锁，光束视觉由 {@link DragonBeamEntity} 承担。</li>
 *   <li><b>普通撕咬</b>：近身时播 1.45s 撕咬动画，第 16 tick 咬合造成 {@value #BITE_DAMAGE} 点伤害与多重负面效果。</li>
 * </ol>
 *
 * <p>常驻无重力飞行（{@link FlyingMoveControl} + {@link FlyingPathNavigation}）、
 * 紫色 Boss 血条、末影龙音效、多部件受击碰撞箱（{@link BossBodyPart}）。</p>
 */
public abstract class AbstractDragonBossEntity extends AbstractRiceGeoMonsterEntity {

    // -------------------- 动画状态同步（服务端递减，自动同步客户端） --------------------

    private static final EntityDataAccessor<Integer> DATA_BREATH_ANIM_TICKS =
            SynchedEntityData.defineId(AbstractDragonBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE_ANIM_TICKS =
            SynchedEntityData.defineId(AbstractDragonBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BEAM_ANIM_TICKS =
            SynchedEntityData.defineId(AbstractDragonBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BITE_ANIM_TICKS =
            SynchedEntityData.defineId(AbstractDragonBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HURT_ANIM_TICKS =
            SynchedEntityData.defineId(AbstractDragonBossEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation ANIM_BREATH = RawAnimation.begin().thenPlay("animation.dragon_breath");
    private static final RawAnimation ANIM_CHARGE = RawAnimation.begin().thenPlay("animation.dive_strike");
    private static final RawAnimation ANIM_BEAM = RawAnimation.begin().thenLoop("animation.laser_beam");
    private static final RawAnimation ANIM_BITE = RawAnimation.begin().thenPlay("animation.melee_bite");
    private static final RawAnimation ANIM_HURT = RawAnimation.begin().thenPlay("animation.hurt_roar");

    // -------------------- 技能数值 --------------------

    /** 龙息：动画 2.45s=49tick，第 17tick（0.85s）张口喷射；CD 30s=600tick */
    private static final int BREATH_ANIMATION_TICKS = 49;
    private static final int BREATH_HIT_TICK = 17;
    private static final int BREATH_COOLDOWN_TICKS = 600;
    private static final float BREATH_DAMAGE = 25.0F;
    private static final double BREATH_RANGE_SQR = 32.0D * 32.0D;
    private static final float BREATH_CLOUD_RADIUS = 4.5F;
    private static final int BREATH_CLOUD_DURATION = 120;
    /** 龙息云伤害跳间隔（tick）：20tick=1s，对齐原版末影龙龙息云的结算节奏 */
    private static final int BREATH_CLOUD_HIT_INTERVAL = 20;

    /** 冲撞：动画 2.3s=46tick，10tick 蓄力，第 22tick（1.1s）命中；CD 60s=1200tick */
    private static final int CHARGE_ANIMATION_TICKS = 46;
    private static final int CHARGE_WINDUP_TICKS = 10;
    private static final int CHARGE_HIT_TICK = 22;
    private static final int CHARGE_DASH_END_TICK = 34;
    private static final int CHARGE_COOLDOWN_TICKS = 1200;
    private static final float CHARGE_DAMAGE = 150.0F;
    private static final double CHARGE_HIT_RADIUS = 4.5D;
    /** 冲撞位移：格/tick（2.4×24tick ≈ 58 格冲刺距离，约 48 格/s，为龙最快动作） */
    private static final double CHARGE_SPEED = 2.4D;

    /** 最懒光束：持续 60s=1200tick，每 10tick（0.5s）结算一次；CD 120s=2400tick */
    private static final int BEAM_DURATION_TICKS = 1200;
    private static final int BEAM_COOLDOWN_TICKS = 2400;
    private static final float BEAM_DAMAGE = 500.0F;
    private static final int BEAM_HIT_INTERVAL = 10;
    private static final double BEAM_RANGE = 48.0D;
    /** 悬停照射点与锁定玩家的水平距离（格），垂直高度在 Goal 内取目标上方 9 格 */
    private static final double BEAM_FIRE_DIST = 16.0D;

    /** 普攻撕咬：动画 1.45s=29tick，第 16tick（0.78s）咬合 */
    private static final int BITE_ANIMATION_TICKS = 29;
    private static final int BITE_HIT_TICK = 16;
    private static final int BITE_COOLDOWN_TICKS = 45;
    private static final float BITE_DAMAGE = 20.0F;
    private static final double BITE_RANGE_SQR = 5.0D * 5.0D;

    /** 受伤嘶吼动画 1.8s=36tick */
    private static final int HURT_ANIMATION_TICKS = 36;

    /** 首次技能前的准备时间，避免出生瞬间立刻出招 */
    private static final int INITIAL_BREATH_DELAY = 120;
    private static final int INITIAL_CHARGE_DELAY = 260;
    private static final int INITIAL_BEAM_DELAY = 480;

    /** 26.3：末影龙龙息粒子已改为带参数的 PowerParticleOption，不能直接当 ParticleOptions 传 */
    private static final PowerParticleOption BREATH_PARTICLE =
            PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);

    // 技能冷却（仅服务端计时）
    private int breathCooldown = INITIAL_BREATH_DELAY;
    private int chargeCooldown = INITIAL_CHARGE_DELAY;
    private int beamCooldown = INITIAL_BEAM_DELAY;
    private int biteCooldown;

    /** 当前场上仍在持续造成伤害的龙息云（仅服务端维护，云消失后自动摘除） */
    private final List<ActiveBreathCloud> activeBreathClouds = new ArrayList<>();

    /** 紫色 Boss 血条（服务端维护，自动同步给看到它的玩家） */
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    /**
     * 身体部位碰撞箱（躯干/颈/头/尾/双翼/四肢）。巨型模型无法用单个方块盒贴合，
     * 因此拆成若干部位：每个部位独立 AABB 只做受击判定，不参与物理推挤，
     * 伤害统一转交本体（见 {@link BossBodyPart}）。
     */
    private final BossBodyPart<?>[] bodyParts;

    /** 龙嘴在模型空间相对 root（=实体脚点）的水平前伸量。 */
    private final float mouthForward;
    /** 龙嘴在模型空间相对脚点的高度。 */
    private final float mouthUp;
    /** 咆哮音效（技能起手时播放）。 */
    private final SoundEvent roarSound;

    /**
     * @param animationPrefix 动画键前缀（交给 {@link AbstractRiceGeoMonsterEntity}）
     * @param parts           身体部位几何定义（模型空间，局部 -Z 为头的方向）
     * @param mouthForward    龙嘴水平前伸量（格）
     * @param mouthUp         龙嘴相对脚点高度（格）
     */
    protected AbstractDragonBossEntity(EntityType<? extends AbstractDragonBossEntity> type, Level level,
                                       String animationPrefix,
                                       SoundEvent ambientSound, SoundEvent hurtSound,
                                       SoundEvent deathSound, SoundEvent roarSound,
                                       float mouthForward, float mouthUp, DragonPart... parts) {
        super(type, level, animationPrefix, ambientSound, hurtSound, deathSound, roarSound);
        this.mouthForward = mouthForward;
        this.mouthUp = mouthUp;
        this.roarSound = roarSound;
        // 常驻飞行：无重力 + 飞行移动控制器（转向灵活度 10，同 Ghast）。
        // hoversInPlace 必须为 true：FlyingMoveControl 在没有移动指令的 tick 会调用
        // setNoGravity(false)，传 false 会让龙在悬停/刹停 tick 反复下坠抖动。
        this.setNoGravity(true);
        this.moveControl = new FlyingMoveControl<>(this, 10, true);
        // 飞龙可穿过非固体危险方块，不应被火/岩浆/水卡住或惩罚（26.3 统一为 PathType）
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.bodyParts = new BossBodyPart<?>[parts.length];
        for (int i = 0; i < parts.length; i++) {
            DragonPart part = parts[i];
            this.bodyParts[i] = new BossBodyPart<>(this, part.name(), part.width(), part.height(),
                    part.offsetX(), part.offsetY(), part.offsetZ());
        }
        this.updateBodyParts();
    }

    /** 单个身体部位的几何定义（模型空间偏移，单位格；局部 +X 为本体右方，局部 -Z 为本体前方）。 */
    public record DragonPart(String name, float width, float height,
                             float offsetX, float offsetY, float offsetZ) {
    }

    /** 飞行生物使用空中直线寻路（节点评估器允许悬空点位）。 */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    // -------------------- 多部件碰撞箱 --------------------

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Nullable
    @Override
    public PartEntity<?>[] getParts() {
        return this.bodyParts;
    }

    /** 部位每 tick 跟随本体位置与朝向重新摆放（部位自身不会被 Level tick）。 */
    @Override
    public void tick() {
        super.tick();
        this.updateBodyParts();
    }

    /**
     * 客户端收到生成包后把部位 id 对齐到「本体 id + 序号 + 1」。
     * 26.3 的实体 id 在构造时由 {@code level.getNextEntityId()} 顺序分配，
     * 因此服务端部位 id 天然紧跟本体 id，客户端按同一规则覆盖即可保证两端一致
     * （与 {@code EnderDragon#recreateFromPacket} 同款处理）。
     */
    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        int baseId = packet.getId();
        for (int i = 0; i < this.bodyParts.length; i++) {
            this.bodyParts[i].setId(baseId + i + 1);
        }
    }

    private void updateBodyParts() {
        float yaw = this.yBodyRot * ((float) Math.PI / 180.0F);
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);
        for (BossBodyPart<?> part : this.bodyParts) {
            double prevX = part.getX();
            double prevY = part.getY();
            double prevZ = part.getZ();
            double dx = part.getOffsetX();
            double dz = part.getOffsetZ();
            // Bedrock 模型经 180°-yaw 旋转渲染：局部 +X → 本体右方，局部 +Z → 本体后方
            double worldX = -dx * cos + dz * sin;
            double worldZ = -dx * sin - dz * cos;
            part.setPos(this.getX() + worldX, this.getY() + part.getOffsetY(), this.getZ() + worldZ);
            part.xo = prevX;
            part.yo = prevY;
            part.zo = prevZ;
            part.xOld = prevX;
            part.yOld = prevY;
            part.zOld = prevZ;
        }
    }

    // -------------------- GeckoLib 动画钩子 --------------------

    @Override
    protected boolean suppressGenericHeavyAttack() {
        return true;
    }

    /** 技能动画优先级：光束 > 龙息 > 冲撞 > 撕咬 > 受伤嘶吼 > 盘旋（子类的 idle/walk）。 */
    @Override
    protected RawAnimation skillAnimation() {
        if (this.entityData.get(DATA_BEAM_ANIM_TICKS) > 0) {
            return ANIM_BEAM;
        }
        if (this.entityData.get(DATA_BREATH_ANIM_TICKS) > 0) {
            return ANIM_BREATH;
        }
        if (this.entityData.get(DATA_CHARGE_ANIM_TICKS) > 0) {
            return ANIM_CHARGE;
        }
        if (this.entityData.get(DATA_BITE_ANIM_TICKS) > 0) {
            return ANIM_BITE;
        }
        if (this.entityData.get(DATA_HURT_ANIM_TICKS) > 0) {
            return ANIM_HURT;
        }
        return null;
    }

    // -------------------- AI --------------------

    /**
     * AI 优先级（所有攻击/飞行类 Goal 都占用 MOVE+LOOK，GoalSelector 天然保证互斥）：
     * 1 光束（60s 持续悬停照射）；2 冲撞（俯冲）；3 龙息（悬停吐息）；4 近身撕咬；
     * 5 无技能时的末影龙式高空大圈盘旋（含掠飞与悬停）；6 无目标高空漫游；7/8 注视张望。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new DragonBeamGoal());
        this.goalSelector.addGoal(2, new DragonChargeGoal());
        this.goalSelector.addGoal(3, new DragonBreathGoal());
        this.goalSelector.addGoal(4, new MeleeBiteGoal());
        this.goalSelector.addGoal(5, new DragonOrbitFlightGoal());
        this.goalSelector.addGoal(6, new DragonAirWanderGoal());
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // 全天敌对：受击必反击 + 主动索敌玩家
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // -------------------- 同步数据 / 计时 --------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BREATH_ANIM_TICKS, 0);
        builder.define(DATA_CHARGE_ANIM_TICKS, 0);
        builder.define(DATA_BEAM_ANIM_TICKS, 0);
        builder.define(DATA_BITE_ANIM_TICKS, 0);
        builder.define(DATA_HURT_ANIM_TICKS, 0);
    }

    /** 仅服务端调用（{@link AbstractRiceGeoMonsterEntity#aiStep()} 已过滤客户端）。 */
    @Override
    protected void bossTick() {
        this.maintainPassiveBuffs();
        if (this.breathCooldown > 0) {
            this.breathCooldown--;
        }
        if (this.chargeCooldown > 0) {
            this.chargeCooldown--;
        }
        if (this.beamCooldown > 0) {
            this.beamCooldown--;
        }
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }
        this.decAnim(DATA_BREATH_ANIM_TICKS);
        this.decAnim(DATA_CHARGE_ANIM_TICKS);
        this.decAnim(DATA_BEAM_ANIM_TICKS);
        this.decAnim(DATA_BITE_ANIM_TICKS);
        this.decAnim(DATA_HURT_ANIM_TICKS);
        this.tickBreathClouds();
    }

    /** 常驻自增益：生命恢复 III + 力量 III（效果过期后自动续接，并覆盖基类较弱的一档）。 */
    private void maintainPassiveBuffs() {
        if (this.tickCount % 40 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 2, false, false));
            this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 2, false, false));
        }
    }

    /** 是否正在释放技能（光束/冲撞/龙息/撕咬），技能期间锁住其他攻击 Goal。 */
    private boolean isCasting() {
        return this.entityData.get(DATA_BEAM_ANIM_TICKS) > 0
                || this.entityData.get(DATA_CHARGE_ANIM_TICKS) > 0
                || this.entityData.get(DATA_BREATH_ANIM_TICKS) > 0
                || this.entityData.get(DATA_BITE_ANIM_TICKS) > 0;
    }

    private void decAnim(EntityDataAccessor<Integer> key) {
        int ticks = this.entityData.get(key);
        if (ticks > 0) {
            this.entityData.set(key, ticks - 1);
        }
    }

    /** 受伤且未在出招时触发一次嘶吼动画（技能不被受击打断）。26.3 中 Entity#hurt 为 final，改覆写 hurtServer。 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean flag = super.hurtServer(level, source, amount);
        if (flag && !this.isCasting() && this.entityData.get(DATA_HURT_ANIM_TICKS) <= 0) {
            this.entityData.set(DATA_HURT_ANIM_TICKS, HURT_ANIMATION_TICKS);
        }
        return flag;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        super.customServerAiStep(level);
    }

    // -------------------- 通用工具 --------------------

    /** 构造技能用的无视护甲伤害源（causing entity = 龙，死亡统计/仇恨正确归属）。 */
    private DamageSource skillDamageSource(ResourceKey<DamageType> key) {
        return ModDamageTypes.source(this.level(), key, this, this);
    }

    /** 可被攻击的玩家判定：存活、非旁观、非创造。 */
    private static boolean validTarget(Player player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    /** 平滑转向并直视目标（含俯仰，供光束/龙息瞄准）。 */
    private void faceTarget(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double dy = (target.getY() + target.getBbHeight() * 0.5D)
                - (this.getY() + this.getBbHeight() * 0.5D);
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setYHeadRot(yaw);
        this.setXRot(Mth.clamp(pitch, -60.0F, 60.0F));
    }

    /**
     * 龙嘴世界坐标（含帧间插值，客户端渲染光束用）。模型朝向 -Z，GeckoLib 已做 180° 对齐，
     * 实体朝向即模型头部方向；头颅位于 root 前方约 {@code mouthForward} 格、上方约
     * {@code mouthUp} 格，再沿当前视线（含俯仰）前移 1.2 格，正好落在张开的双颌之间。
     */
    public Vec3 getMouthPosition(float partialTick) {
        Vec3 feet = this.getPosition(partialTick);
        float yaw = this.getViewYRot(partialTick);
        float pitch = this.getViewXRot(partialTick);
        Vec3 horizontalForward = Vec3.directionFromRotation(0.0F, yaw);
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        return feet.add(0.0D, this.mouthUp, 0.0D)
                .add(horizontalForward.scale(this.mouthForward))
                .add(look.scale(1.2D));
    }

    /** 龙嘴世界坐标（服务端技能判定用）。 */
    private Vec3 mouthPosition() {
        return this.getMouthPosition(1.0F);
    }

    /** 刹停空中惯性（蓄力/喷吐定点时用）。 */
    private void brakeInAir() {
        this.getNavigation().stop();
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.55D, 0.8D, 0.55D));
    }

    // -------------------- 音效 / 抗性 --------------------

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected float getSoundVolume() {
        return 2.5F;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    // -------------------- 技能 1：龙息 --------------------

    /**
     * 【龙息】CD 30s：2.45s 动画内，前 0.85s 仰头蓄力，第 17tick 朝锁定玩家当前位置
     * 生成末影龙紫色龙息云（持续 6s，半径 4.5 逐渐收缩到 1）。留在雾内每 1s 受一次
     * 无视护甲伤害（共 6 跳），逼玩家立刻走出紫雾。
     */
    private class DragonBreathGoal extends Goal {
        private int runTicks;

        DragonBreathGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = AbstractDragonBossEntity.this.getTarget();
            return AbstractDragonBossEntity.this.breathCooldown <= 0
                    && !AbstractDragonBossEntity.this.isCasting()
                    && target instanceof Player player && validTarget(player)
                    && AbstractDragonBossEntity.this.distanceToSqr(target) <= BREATH_RANGE_SQR
                    && AbstractDragonBossEntity.this.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = AbstractDragonBossEntity.this.getTarget();
            return this.runTicks < BREATH_ANIMATION_TICKS && target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.runTicks = 0;
            AbstractDragonBossEntity.this.brakeInAir();
            AbstractDragonBossEntity.this.entityData.set(DATA_BREATH_ANIM_TICKS, BREATH_ANIMATION_TICKS);
            AbstractDragonBossEntity.this.playSound(AbstractDragonBossEntity.this.roarSound, 2.0F, 0.8F);
        }

        @Override
        public void tick() {
            this.runTicks++;
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            dragon.brakeInAir();
            LivingEntity target = dragon.getTarget();
            if (target != null) {
                dragon.faceTarget(target);
            }
            if (this.runTicks == BREATH_HIT_TICK) {
                dragon.performDragonBreath(target);
            }
        }

        @Override
        public void stop() {
            this.runTicks = 0;
            AbstractDragonBossEntity.this.breathCooldown = BREATH_COOLDOWN_TICKS;
        }
    }

    /** 生成末影龙龙息云并登记，云的持续多跳伤害由 {@link #tickBreathClouds()} 每秒结算。 */
    private void performDragonBreath(LivingEntity target) {
        this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 2.0F, 1.0F);
        double cx = target.getX();
        double cy = target.getY() + 0.6D;
        double cz = target.getZ();

        // 原版末影龙同款紫色龙息雾（不挂药水效果，伤害由本 Boss 统一结算）
        AreaEffectCloud cloud = new AreaEffectCloud(this.level(), cx, cy, cz);
        cloud.setRadius(BREATH_CLOUD_RADIUS);
        cloud.setRadiusPerTick((1.0F - BREATH_CLOUD_RADIUS) / BREATH_CLOUD_DURATION);
        cloud.setWaitTime(0);
        cloud.setDuration(BREATH_CLOUD_DURATION);
        cloud.setCustomParticle(BREATH_PARTICLE);
        cloud.setOwner(this);
        this.level().addFreshEntity(cloud);
        this.activeBreathClouds.add(new ActiveBreathCloud(cloud));

        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 40; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double r = this.random.nextDouble() * BREATH_CLOUD_RADIUS;
                serverLevel.sendParticles(BREATH_PARTICLE,
                        cx + Math.cos(angle) * r, cy + this.random.nextDouble() * 1.6D,
                        cz + Math.sin(angle) * r, 1, 0.0D, 0.06D, 0.0D, 0.0D);
            }
        }
    }

    /**
     * 龙息云持续伤害：云存活期间每 {@value #BREATH_CLOUD_HIT_INTERVAL}tick（1s）对雾内玩家结算一次，
     * 每跳都是 {@value #BREATH_DAMAGE} 点无视护甲伤害；伤害范围跟随云当前（收缩中的）半径。
     */
    private void tickBreathClouds() {
        if (this.activeBreathClouds.isEmpty()) {
            return;
        }
        Iterator<ActiveBreathCloud> iterator = this.activeBreathClouds.iterator();
        while (iterator.hasNext()) {
            ActiveBreathCloud active = iterator.next();
            AreaEffectCloud cloud = active.cloud;
            active.ticks++;
            if (cloud.isRemoved() || active.ticks > BREATH_CLOUD_DURATION) {
                iterator.remove();
                continue;
            }
            if (active.ticks % BREATH_CLOUD_HIT_INTERVAL != 0) {
                continue;
            }
            float radius = cloud.getRadius();
            AABB hitArea = new AABB(
                    cloud.getX() - radius, cloud.getY() - 2.5D, cloud.getZ() - radius,
                    cloud.getX() + radius, cloud.getY() + 2.5D, cloud.getZ() + radius);
            List<Player> players = this.level().getEntitiesOfClass(Player.class, hitArea,
                    AbstractDragonBossEntity::validTarget);
            for (Player player : players) {
                player.hurtServer((ServerLevel) this.level(),
                        this.skillDamageSource(ModDamageTypes.DRAGON_BREATH), BREATH_DAMAGE);
            }
        }
    }

    /** 活跃龙息云引用：云实体 + 自生成起的 tick 计数（独立计时，不依赖云内部字段）。 */
    private static final class ActiveBreathCloud {
        private final AreaEffectCloud cloud;
        private int ticks;

        private ActiveBreathCloud(AreaEffectCloud cloud) {
            this.cloud = cloud;
        }
    }

    // -------------------- 技能 2：巨龙冲撞 --------------------

    /**
     * 【巨龙冲撞】CD 60s：前 10tick 扬翅蓄力锁定（带目标速度提前量），
     * 11~34tick 沿固定方向以 2.4 格/tick 高速直线冲撞（脚本位移，撞墙提前结束），
     * 第 22tick（动画俯冲最低点）命中帧结算伤害+强击退，35~46tick 拉起恢复。
     */
    private class DragonChargeGoal extends Goal {
        private int runTicks;
        private Vec3 dashDir = Vec3.ZERO;
        private boolean hitDone;

        DragonChargeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = AbstractDragonBossEntity.this.getTarget();
            return AbstractDragonBossEntity.this.chargeCooldown <= 0
                    && !AbstractDragonBossEntity.this.isCasting()
                    && target instanceof Player player && validTarget(player)
                    && AbstractDragonBossEntity.this.distanceToSqr(target) <= 40.0D * 40.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return this.runTicks < CHARGE_ANIMATION_TICKS;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.runTicks = 0;
            this.hitDone = false;
            this.dashDir = Vec3.ZERO;
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            dragon.brakeInAir();
            dragon.entityData.set(DATA_CHARGE_ANIM_TICKS, CHARGE_ANIMATION_TICKS);
            dragon.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.2F, 0.55F);
        }

        @Override
        public void tick() {
            this.runTicks++;
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            LivingEntity target = dragon.getTarget();

            if (this.runTicks <= CHARGE_WINDUP_TICKS) {
                // 蓄力：空中刹停、压低身位、死盯目标
                dragon.brakeInAir();
                if (target != null) {
                    dragon.faceTarget(target);
                }
                if (this.runTicks == CHARGE_WINDUP_TICKS && target != null) {
                    // 锁定冲点：目标当前位置 + 速度提前量（约 12tick）
                    Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                            .add(target.getDeltaMovement().scale(12.0D));
                    Vec3 start = dragon.position().add(0.0D, dragon.getBbHeight() * 0.5D, 0.0D);
                    this.dashDir = aim.subtract(start);
                    if (this.dashDir.lengthSqr() < 1.0E-4D) {
                        this.dashDir = dragon.getLookAngle();
                    }
                    this.dashDir = this.dashDir.normalize();
                    float yaw = (float) (Math.toDegrees(Math.atan2(this.dashDir.z, this.dashDir.x)) - 90.0D);
                    dragon.setYRot(yaw);
                    dragon.yBodyRot = yaw;
                    dragon.setYHeadRot(yaw);
                }
            } else if (this.runTicks <= CHARGE_DASH_END_TICK) {
                // 高速直线冲撞：脚本位移 + 碰撞检测（撞墙立刻进入拉起）
                // 碰撞箱已扩到整条龙翼展，探墙改用龙体核心 3×3 小箱，避免贴地/掠林时整箱误判撞墙
                Vec3 move = this.dashDir.scale(CHARGE_SPEED);
                AABB nextBox = new AABB(
                        dragon.getX() - 1.5D, dragon.getY(), dragon.getZ() - 1.5D,
                        dragon.getX() + 1.5D, dragon.getY() + dragon.getBbHeight(),
                        dragon.getZ() + 1.5D).move(move);
                if (dragon.level().noCollision(dragon, nextBox)) {
                    dragon.setPos(dragon.getX() + move.x, dragon.getY() + move.y, dragon.getZ() + move.z);
                    dragon.setDeltaMovement(move);
                } else {
                    this.runTicks = Math.max(this.runTicks, CHARGE_DASH_END_TICK);
                    dragon.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.6F, 0.6F);
                }

                // 身体拖出的紫色残影
                if (dragon.level() instanceof ServerLevel serverLevel && this.runTicks % 2 == 0) {
                    serverLevel.sendParticles(BREATH_PARTICLE,
                            dragon.getX(), dragon.getY() + dragon.getBbHeight() * 0.5D, dragon.getZ(),
                            6, 2.0D, dragon.getBbHeight() * 0.35D, 2.0D, 0.02D);
                }

                if (!this.hitDone && this.runTicks >= CHARGE_HIT_TICK) {
                    this.hitDone = true;
                    dragon.performChargeHit();
                }
            } else {
                // 拉起恢复：减速、沿冲撞方向上方爬升
                Vec3 recover = this.dashDir.scale(0.45D).add(0.0D, 0.55D, 0.0D);
                dragon.setDeltaMovement(recover);
                dragon.maybeRecoverMove();
            }
        }

        @Override
        public void stop() {
            this.runTicks = 0;
            this.hitDone = false;
            this.dashDir = Vec3.ZERO;
            AbstractDragonBossEntity.this.chargeCooldown = CHARGE_COOLDOWN_TICKS;
        }
    }

    /** 冲撞命中帧：龙体周围 4.5 格内玩家受伤害并沿冲撞方向被轰飞。 */
    private void performChargeHit() {
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.7F);
        // 命中范围按龙体核心固定盒计算：碰撞箱已扩到翼展，直接 inflate 会让判定扩散到三十格外
        AABB hitArea = new AABB(
                this.getX() - CHARGE_HIT_RADIUS, this.getY(), this.getZ() - CHARGE_HIT_RADIUS,
                this.getX() + CHARGE_HIT_RADIUS, this.getY() + this.getBbHeight(),
                this.getZ() + CHARGE_HIT_RADIUS);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, hitArea,
                AbstractDragonBossEntity::validTarget);
        for (Player player : players) {
            player.hurtServer((ServerLevel) this.level(),
                    this.skillDamageSource(ModDamageTypes.DRAGON_CHARGE), CHARGE_DAMAGE);
            Vec3 push = player.position().subtract(this.position());
            if (push.lengthSqr() < 1.0E-4D) {
                push = this.getLookAngle();
            }
            push = push.normalize();
            // push() 内部已标记速度同步（26.3 移除了 hurtMarked 字段）
            player.push(push.x * 1.6D, 0.7D, push.z * 1.6D);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    2, 1.0D, 1.0D, 1.0D, 0.0D);
        }
    }

    /** 冲撞恢复末期交给飞行控制器接管（无目标时自然过渡到盘旋/漫游）。 */
    private void maybeRecoverMove() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getMoveControl().setWantedPosition(
                    target.getX(), target.getY() + 9.0D, target.getZ(), 1.0D);
        }
    }

    // -------------------- 技能 3：最懒光束 --------------------

    /**
     * 【最懒光束】CD 120s：先飞抵锁定玩家斜上方约 {@value #BEAM_FIRE_DIST} 格的照射点
     * （最多 2s 就位），随后悬停、张口持续照射 60s。
     * 每 10tick（0.5s）做一次无遮挡射线判定，命中造成无视护甲伤害；
     * 目标死亡立即换锁最近的下一名玩家（光束实体改写 targetId，视觉无缝转向）。
     */
    private class DragonBeamGoal extends Goal {
        private static final int ALIGN = 0;
        private static final int FIRE = 1;
        /** 就位阶段上限：40tick（2s），超时也强制开火 */
        private static final int ALIGN_MAX = 40;
        private static final double ARRIVE_DIST_SQR = 3.5D * 3.5D;

        private int runTicks;
        private int fireTicks;
        private int alignTicks;
        private int phase;
        private Player beamTarget;
        private DragonBeamEntity beamEntity;

        DragonBeamGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = AbstractDragonBossEntity.this.getTarget();
            return AbstractDragonBossEntity.this.beamCooldown <= 0
                    && !AbstractDragonBossEntity.this.isCasting()
                    && target instanceof Player player && validTarget(player);
        }

        @Override
        public boolean canContinueToUse() {
            return this.runTicks < ALIGN_MAX + BEAM_DURATION_TICKS;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.runTicks = 0;
            this.fireTicks = 0;
            this.alignTicks = 0;
            this.phase = ALIGN;
            this.beamEntity = null;
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            this.beamTarget = dragon.getTarget() instanceof Player player ? player : null;
            dragon.brakeInAir();
            // 动画覆盖就位+照射全程（循环动画，时长无所谓）
            dragon.entityData.set(DATA_BEAM_ANIM_TICKS, ALIGN_MAX + BEAM_DURATION_TICKS);
            dragon.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5F, 0.45F);
        }

        /** 计算照射悬停点：目标斜上方、保持在龙当前所在一侧，避免横穿玩家 */
        private Vec3 firePoint(AbstractDragonBossEntity dragon, Player target) {
            double dx = dragon.getX() - target.getX();
            double dz = dragon.getZ() - target.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-3D) {
                dx = dragon.getLookAngle().x;
                dz = dragon.getLookAngle().z;
            } else {
                dx /= len;
                dz /= len;
            }
            double y = target.getY() + 9.0D;
            y = Mth.clamp(y, dragon.level().getMinY() + 4.0D, dragon.level().getMaxY() - 4.0D);
            return new Vec3(target.getX() + dx * BEAM_FIRE_DIST, y, target.getZ() + dz * BEAM_FIRE_DIST);
        }

        @Override
        public void tick() {
            this.runTicks++;
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;

            // 目标死亡/失效（就位或照射中都可能发生）：立刻自动选择下一位玩家
            if (!validTarget(this.beamTarget)) {
                this.beamTarget = dragon.level().getNearestPlayer(
                        dragon.getX(), dragon.getY(), dragon.getZ(), BEAM_RANGE,
                        entity -> entity instanceof Player player && validTarget(player));
                if (this.beamTarget != null) {
                    dragon.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.5F);
                    if (this.beamEntity != null) {
                        this.beamEntity.setTarget(this.beamTarget);
                    }
                }
            }

            if (this.phase == ALIGN) {
                this.alignTicks++;
                if (this.beamTarget != null) {
                    Vec3 point = this.firePoint(dragon, this.beamTarget);
                    dragon.getMoveControl().setWantedPosition(point.x, point.y, point.z, 1.8D);
                    dragon.faceTarget(this.beamTarget);
                    if (this.alignTicks >= ALIGN_MAX
                            || dragon.position().distanceToSqr(point) <= ARRIVE_DIST_SQR) {
                        this.beginFire(dragon);
                    }
                } else {
                    dragon.brakeInAir();
                    if (this.alignTicks >= ALIGN_MAX) {
                        this.beginFire(dragon);
                    }
                }
            } else {
                this.fireTicks++;
                if (this.beamTarget != null) {
                    Player target = this.beamTarget;
                    dragon.faceTarget(target);
                    // 悬停照射；玩家跑出射程则缓慢跟进到新的照射点，不打断照射
                    if (dragon.distanceToSqr(target) > (BEAM_RANGE - 6.0D) * (BEAM_RANGE - 6.0D)) {
                        Vec3 point = this.firePoint(dragon, target);
                        dragon.getMoveControl().setWantedPosition(point.x, point.y, point.z, 1.3D);
                    } else {
                        dragon.brakeInAir();
                    }
                    dragon.performBeamHit(target, this.fireTicks);
                } else {
                    // 暂时无人可锁：原地悬停空等，计时不停，新目标出现即续射
                    dragon.brakeInAir();
                }
            }
        }

        /** 就位完成：生成光束渲染实体，进入照射阶段。 */
        private void beginFire(AbstractDragonBossEntity dragon) {
            this.phase = FIRE;
            this.fireTicks = 0;
            dragon.brakeInAir();
            if (dragon.level() instanceof ServerLevel serverLevel && this.beamTarget != null) {
                this.beamEntity = DragonBeamEntity.spawn(
                        serverLevel, dragon, this.beamTarget, BEAM_DURATION_TICKS);
                dragon.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 2.2F, 0.7F);
            }
        }

        @Override
        public void stop() {
            this.runTicks = 0;
            this.fireTicks = 0;
            this.phase = ALIGN;
            if (this.beamEntity != null) {
                this.beamEntity.discard();
                this.beamEntity = null;
            }
            this.beamTarget = null;
            AbstractDragonBossEntity.this.beamCooldown = BEAM_COOLDOWN_TICKS;
        }
    }

    /** 光束照射单 tick：嘴部微光 + 每 10tick 一次无遮挡伤害射线判定，光束本体由 {@link DragonBeamEntity} 承担。 */
    private void performBeamHit(Player target, int fireTicks) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 start = this.mouthPosition();

        // 嘴部发射微光（不是光束本体，只强化「光从嘴里出来」的发射点）
        if (fireTicks % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    start.x, start.y, start.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }

        Vec3 aimPoint = target.getBoundingBox().getCenter().add(0.0D, 0.4D, 0.0D);
        if (start.distanceTo(aimPoint) > BEAM_RANGE) {
            return;
        }

        // 方块遮挡：射线被碰撞方块截断则本跳不造成伤害
        BlockHitResult blockHit = this.level().clip(new ClipContext(
                start, aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            return;
        }

        // 每 0.5s 结算一次无视护甲伤害 + 命中点紫光爆裂
        if (fireTicks % BEAM_HIT_INTERVAL == 0) {
            target.hurtServer(serverLevel, this.skillDamageSource(ModDamageTypes.DRAGON_BEAM), BEAM_DAMAGE);
            this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.6F, 1.25F);
            serverLevel.sendParticles(BREATH_PARTICLE,
                    aimPoint.x, aimPoint.y, aimPoint.z, 16, 0.35D, 0.35D, 0.35D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    aimPoint.x, aimPoint.y, aimPoint.z, 5, 0.2D, 0.2D, 0.2D, 0.02D);
        } else if (fireTicks % 40 == 0) {
            this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 0.45F);
        }
    }

    // -------------------- 普通攻击：撕咬 --------------------

    /**
     * 【撕咬普攻】：目标进入 5 格且垂直差不大时，播 1.45s 撕咬动画，
     * 第 16tick（0.78s）咬合帧结算一次近战伤害并挂多重负面效果。
     */
    private class MeleeBiteGoal extends Goal {
        private int runTicks;
        private boolean hitDone;

        MeleeBiteGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            LivingEntity target = dragon.getTarget();
            return dragon.biteCooldown <= 0 && !dragon.isCasting()
                    && target instanceof Player player && validTarget(player)
                    && dragon.distanceToSqr(target) <= BITE_RANGE_SQR
                    && Math.abs(target.getY() - dragon.getY()) < 4.0D
                    && dragon.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = AbstractDragonBossEntity.this.getTarget();
            return this.runTicks < BITE_ANIMATION_TICKS && target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.runTicks = 0;
            this.hitDone = false;
            AbstractDragonBossEntity.this.brakeInAir();
            AbstractDragonBossEntity.this.entityData.set(DATA_BITE_ANIM_TICKS, BITE_ANIMATION_TICKS);
        }

        @Override
        public void tick() {
            this.runTicks++;
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            LivingEntity target = dragon.getTarget();
            if (target != null) {
                dragon.faceTarget(target);
                // 咬合前短促贴近，咬合帧后悬停收势
                if (this.runTicks < BITE_HIT_TICK) {
                    dragon.getMoveControl().setWantedPosition(
                            target.getX(), target.getY() + 1.0D, target.getZ(), 1.8D);
                } else {
                    dragon.brakeInAir();
                }
            }
            if (!this.hitDone && this.runTicks >= BITE_HIT_TICK) {
                this.hitDone = true;
                if (target != null) {
                    dragon.performBite(target);
                }
            }
        }

        @Override
        public void stop() {
            this.runTicks = 0;
            this.hitDone = false;
            AbstractDragonBossEntity.this.biteCooldown = BITE_COOLDOWN_TICKS;
        }
    }

    /** 撕咬结算：固定伤害 + 自身短暂高额生命恢复 + 目标虚弱/失明/反胃/缓慢。 */
    private void performBite(LivingEntity target) {
        boolean hit = target.hurtServer((ServerLevel) this.level(),
                this.damageSources().mobAttack(this), BITE_DAMAGE);
        if (hit) {
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3));
        }
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.2F, 0.85F);
    }

    // -------------------- 常态飞行 AI --------------------

    /**
     * 末影龙式高空盘旋（仿原版末影龙 HoldingPattern 巡航 + Strafe 掠飞节奏）：
     * 以首次交战点为圆心，在约 38 格半径、上方约 16 格的高空沿大圈巡航；巡航中随机穿插：
     * <ul>
     *   <li>悬停（停下来 3~7 秒盯住玩家——此时光束/龙息等技能会自然插入施法）；</li>
     *   <li>掠飞（朝玩家当前位置高速逼近 2~3 秒，给冲撞/撕咬创造距离窗口），随后回到大圈。</li>
     * </ul>
     * 被玩家引离圆心超过 64 格时，圆心转移到玩家处，避免龙被风筝到永远够不着。
     */
    private class DragonOrbitFlightGoal extends Goal {
        private static final int ORBIT = 0;
        private static final int HOVER = 1;
        private static final int STRAFE = 2;

        private static final double ORBIT_RADIUS = 38.0D;
        private static final double ORBIT_HEIGHT = 16.0D;
        private static final double REANCHOR_DISTANCE = 64.0D;

        private int phase = ORBIT;
        private int phaseTime;
        private int phaseLength;
        private double circleAngle;
        private boolean anchored;
        private double centerX;
        private double centerY;
        private double centerZ;

        DragonOrbitFlightGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return AbstractDragonBossEntity.this.getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = AbstractDragonBossEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            // Goal 实例长期复用（被技能抢占后再恢复），圆心只在首次交战时锚定一次
            if (!this.anchored) {
                this.centerX = dragon.getX();
                this.centerY = dragon.getY();
                this.centerZ = dragon.getZ();
                this.circleAngle = dragon.random.nextDouble() * Math.PI * 2.0D;
                this.phase = ORBIT;
                this.phaseTime = 0;
                this.phaseLength = 120 + dragon.random.nextInt(140);
                this.anchored = true;
            }
        }

        @Override
        public void stop() {
            AbstractDragonBossEntity.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            LivingEntity target = dragon.getTarget();
            if (target == null) {
                return;
            }

            double dx = target.getX() - this.centerX;
            double dz = target.getZ() - this.centerZ;
            if (dx * dx + dz * dz > REANCHOR_DISTANCE * REANCHOR_DISTANCE) {
                // 被风筝过远：圆心转移到玩家身上，龙继续在新圆心高空巡航
                this.centerX = target.getX();
                this.centerY = target.getY();
                this.centerZ = target.getZ();
            }

            this.phaseTime++;
            switch (this.phase) {
                case HOVER -> this.tickHover(dragon, target);
                case STRAFE -> this.tickStrafe(dragon, target);
                default -> this.tickOrbit(dragon, target);
            }
        }

        /** 高空大圈巡航：角速度与巡航速度匹配（龙稳态约 1.15 格/tick，贴圈不拖尾）。 */
        private void tickOrbit(AbstractDragonBossEntity dragon, LivingEntity target) {
            this.circleAngle += 0.027D;
            double radius = ORBIT_RADIUS + Math.sin(this.circleAngle * 3.0D) * 4.0D;
            double wx = this.centerX + Math.cos(this.circleAngle) * radius;
            double wz = this.centerZ + Math.sin(this.circleAngle) * radius;
            double wy = Math.max(this.centerY + ORBIT_HEIGHT
                            + Math.sin(this.circleAngle * 1.7D) * 5.0D,
                    dragon.level().getMinY() + 6.0D);
            dragon.getMoveControl().setWantedPosition(wx, wy, wz, 1.15D);
            dragon.getLookControl().setLookAt(target, 12.0F, 12.0F);

            if (this.phaseTime >= this.phaseLength) {
                this.chooseNextPhase(dragon, target);
            }
        }

        /** 悬停：空中刹停、盯住玩家（技能 Goal 在此窗口插入施法）。 */
        private void tickHover(AbstractDragonBossEntity dragon, LivingEntity target) {
            dragon.brakeInAir();
            dragon.getLookControl().setLookAt(target, 20.0F, 20.0F);
            if (this.phaseTime >= this.phaseLength) {
                this.enterPhase(ORBIT, 100 + dragon.random.nextInt(160));
            }
        }

        /** 掠飞：朝玩家头顶高速逼近（不直接造成伤害，伤害由冲撞/撕咬技能结算）。 */
        private void tickStrafe(AbstractDragonBossEntity dragon, LivingEntity target) {
            dragon.getMoveControl().setWantedPosition(
                    target.getX(), target.getY() + 8.0D, target.getZ(), 2.2D);
            dragon.getLookControl().setLookAt(target, 20.0F, 20.0F);
            if (this.phaseTime >= this.phaseLength) {
                this.enterPhase(ORBIT, 120 + dragon.random.nextInt(140));
            }
        }

        /** 巡航到点：30% 悬停；30% 且玩家在 40 格内时掠飞；其余继续巡航。 */
        private void chooseNextPhase(AbstractDragonBossEntity dragon, LivingEntity target) {
            float roll = dragon.random.nextFloat();
            double distSqr = dragon.distanceToSqr(target);
            if (roll < 0.30F) {
                this.enterPhase(HOVER, 60 + dragon.random.nextInt(80));
            } else if (roll < 0.60F && distSqr < 40.0D * 40.0D) {
                this.enterPhase(STRAFE, 36 + dragon.random.nextInt(30));
            } else {
                this.enterPhase(ORBIT, 100 + dragon.random.nextInt(160));
            }
        }

        private void enterPhase(int phase, int length) {
            this.phase = phase;
            this.phaseTime = 0;
            this.phaseLength = length;
        }
    }

    /** 无目标时的高空漫游：在当前位置附近选悬空点飞过去（参考 Ghast 随机漂浮）。 */
    private class DragonAirWanderGoal extends Goal {
        private int timeout;

        DragonAirWanderGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            if (dragon.getTarget() != null || !dragon.getNavigation().isDone()) {
                return false;
            }
            return dragon.random.nextInt(35) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.timeout > 0 && !AbstractDragonBossEntity.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            AbstractDragonBossEntity dragon = AbstractDragonBossEntity.this;
            Level level = dragon.level();
            double tx = dragon.getX() + (dragon.random.nextDouble() * 2.0D - 1.0D) * 18.0D;
            double tz = dragon.getZ() + (dragon.random.nextDouble() * 2.0D - 1.0D) * 18.0D;
            double ty = dragon.getY() + (dragon.random.nextDouble() * 12.0D - 4.0D);
            ty = Mth.clamp(ty, level.getMinY() + 5.0D, level.getMaxY() - 6.0D);
            dragon.getNavigation().moveTo(tx, ty, tz, 1.2D);
            this.timeout = 120;
        }

        @Override
        public void tick() {
            this.timeout--;
        }

        @Override
        public void stop() {
            this.timeout = 0;
        }
    }
}

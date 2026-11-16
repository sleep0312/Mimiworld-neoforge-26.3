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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;

import java.util.EnumSet;

/**
 * 爪巴米者（Pami Crawler，注册 id = pami_crawler）——旧米麻袋成精的苦力怕，四足爬行自爆怪。
 *
 * <p>GeckoLib 资源：geckolib/models/pami_crawler.geo.json、
 * textures/entity/pami_crawler.png、geckolib/animations/pami_crawler.animation.json
 * （动画键 animation.idle / animation.walk / animation.attack）。</p>
 *
 * <p>行为对齐原版苦力怕：不近战，目标进入 3 格点燃引信（30 tick = 1.5 秒），
 * 脱离 7 格后快速熄灭，走满即自爆（半径 3.0，受 mobGriefing 约束）。
 * 12 心、移速 0.25，音效使用原版苦力怕。</p>
 */
public class PamiCrawlerEntity extends Monster implements GeoEntity {

    /** 引信总时长 30 tick = 1.5 秒（对齐原版苦力怕 maxSwell，也等于 attack 动画长度）。 */
    public static final int SWELL_DURATION = 30;
    /** 爆炸半径：与原版苦力怕相同。 */
    public static final float EXPLOSION_RADIUS = 3.0F;
    /** 点燃引信的距离平方（3 格）。 */
    private static final double SWELL_START_DISTANCE_SQR = 9.0D;
    /** 引信熄灭的距离平方（7 格）。 */
    private static final double SWELL_STOP_DISTANCE_SQR = 49.0D;

    /** 同步数据：当前引信进度（0..30），服务端推进，客户端驱动 attack 动画。 */
    private static final EntityDataAccessor<Integer> DATA_SWELL =
            SynchedEntityData.defineId(PamiCrawlerEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("animation.attack");

    public PamiCrawlerEntity(EntityType<? extends PamiCrawlerEntity> type, Level level) {
        super(type, level);
    }

    /** 12 心（24 血）、移速 0.25；无近战攻击力（攻击方式只有自爆）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SwellGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PamiCrawlerEntity>("main", 5, this::handleAnimState));
    }

    /**
     * 动画状态：引信点燃期间播单次 attack（鼓胀引信）；
     * 否则移动播 walk、静止播 idle。
     */
    private PlayState handleAnimState(AnimationTest<PamiCrawlerEntity> state) {
        if (this.isSwelling()) {
            return state.setAndContinue(ANIM_ATTACK);
        }
        return state.setAndContinue(state.isMoving() ? ANIM_WALK : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SWELL, 0);
    }

    /** 当前引信进度（0..30）。 */
    public int getSwell() {
        return this.entityData.get(DATA_SWELL);
    }

    /** 引信是否在燃烧。 */
    public boolean isSwelling() {
        return this.entityData.get(DATA_SWELL) > 0;
    }

    /**
     * 仅服务端的 AI 步进：推进/熄灭引信，走满就自爆。
     * 目标在 3 格内逐 tick 增加，否则每 tick 减 2（灭得比点得快），钳制在 0..30。
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        LivingEntity target = this.getTarget();
        int swell = this.getSwell();
        if (target != null && this.distanceToSqr(target) < SWELL_START_DISTANCE_SQR) {
            if (swell == 0) {
                this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            }
            swell++;
        } else if (swell > 0) {
            swell -= 2;
        }
        swell = Mth.clamp(swell, 0, SWELL_DURATION);
        this.entityData.set(DATA_SWELL, swell);

        if (swell >= SWELL_DURATION && !this.isRemoved()) {
            this.explode(level);
        }
    }

    /** 自爆：调用原版爆炸（ExplosionInteraction.MOB 受 /gamerule mobGriefing 约束），随后消失。 */
    private void explode(ServerLevel level) {
        level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(), EXPLOSION_RADIUS, ExplosionInteraction.MOB);
        this.discard();
    }

    // —— 音效：沿用原版苦力怕 ——

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CREEPER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }

    /**
     * 引信停步 AI：目标进入 3 格后站住不动点燃引信，
     * 目标脱离 7 格或引信熄灭后恢复游荡。
     */
    public static class SwellGoal extends Goal {
        private final PamiCrawlerEntity crawler;

        public SwellGoal(PamiCrawlerEntity crawler) {
            this.crawler = crawler;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.crawler.getTarget();
            return target != null && this.crawler.distanceToSqr(target) < 9.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.crawler.getTarget();
            if (target == null || this.crawler.distanceToSqr(target) > SWELL_STOP_DISTANCE_SQR) {
                return false;
            }
            return this.crawler.isSwelling();
        }

        @Override
        public void start() {
            this.crawler.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.crawler.getNavigation().stop();
        }
    }
}

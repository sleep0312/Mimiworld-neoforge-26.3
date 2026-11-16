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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 黏米之蛛（Sticky Rice Spider，注册 id = sticky_rice_spider）。
 *
 * <p>GeckoLib 资源：geckolib/models/spider.geo.json、
 * textures/entity/spider.png、geckolib/animations/spider.animation.json
 * （动画键 idle / walk / attack）。</p>
 *
 * <p>行为对齐原版蜘蛛：扑击、避水游荡、攀爬垂直墙面、蜘蛛网不减速、
 * 节肢动物（受「节肢杀手」加成，已加入 {@code #minecraft:arthropod} 标签）、原版蜘蛛音效；
 * 24 心 / 0.3 移速 / 2 攻击力，碰撞箱 1.4×0.9。唯一区别是全天敌对（原版蜘蛛白天中立）。</p>
 */
public class StickyRiceSpiderEntity extends Monster implements GeoEntity {

    /** 同步数据：爬墙标志位（与原版蜘蛛相同的字节位 0x1）。 */
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID =
            SynchedEntityData.defineId(StickyRiceSpiderEntity.class, EntityDataSerializers.BYTE);
    /** 同步数据：攻击动画剩余 tick。 */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(StickyRiceSpiderEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("attack");

    /** attack 动画时长 20 tick（动画文件里 attack 为 1 秒单次播放）。 */
    private static final int ATTACK_ANIMATION_TICKS = 20;

    public StickyRiceSpiderEntity(EntityType<? extends StickyRiceSpiderEntity> type, Level level) {
        super(type, level);
    }

    /** 24 心、0.3 移速、2 近战攻击力。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 48.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 全天敌对：不使用原版蜘蛛的白天中立条件
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<StickyRiceSpiderEntity>("main", 5, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<StickyRiceSpiderEntity> state) {
        if (this.entityData.get(DATA_ATTACK_ANIM_TICKS) > 0) {
            return state.setAndContinue(ANIM_ATTACK);
        }
        return state.setAndContinue(state.isMoving() ? ANIM_WALK : ANIM_IDLE);
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
        builder.define(DATA_FLAGS_ID, (byte) 0);
        builder.define(DATA_ATTACK_ANIM_TICKS, 0);
    }

    /** 蜘蛛网不减速（原版蜘蛛特性），其他方块正常处理黏性。 */
    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, motionMultiplier);
        }
    }

    /** 爬墙状态位为 true 时即可贴墙移动。 */
    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public void setClimbing(boolean climbing) {
        byte flag = this.entityData.get(DATA_FLAGS_ID);
        if (climbing) {
            flag = (byte) (flag | 1);
        } else {
            flag = (byte) (flag & -2);
        }
        this.entityData.set(DATA_FLAGS_ID, flag);
    }

    @Override
    public void tick() {
        super.tick();
        // 与原版蜘蛛一致：贴墙水平碰撞时进入爬墙状态（仅服务端驱动并同步给客户端）
        if (!this.level().isClientSide()) {
            this.setClimbing(this.horizontalCollision);
        }
    }

    // —— 音效：全部沿用原版蜘蛛 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 0.9F);
    }
}

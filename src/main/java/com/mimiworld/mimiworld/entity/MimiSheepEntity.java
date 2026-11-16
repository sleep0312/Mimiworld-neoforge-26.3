package com.mimiworld.mimiworld.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 米羊羊（Mimi Sheep）：GeckoLib 动画友好生物。
 *
 * <p>模型/贴图/动画资源：geckolib/models/mimi_sheep.geo.json、
 * textures/entity/mimi_sheep.png、geckolib/animations/mimi_sheep.animation.json
 * （动画键为裸名 idle / walk / scared_run）。</p>
 *
 * <p>生物逻辑对齐原版羊的友好生物部分：水上漂浮、受击惊慌逃跑（加速 1.25）、
 * 避水随机游荡、看向玩家与四处张望；音效使用原版羊。
 * 按需求不注册吃草 AI（EatBlockGoal），因此不会啃食草方块，也没有繁殖/引诱 AI。</p>
 */
public class MimiSheepEntity extends PathfinderMob implements GeoEntity {

    // GeckoLib 要求每个可动画对象持有一个动画实例缓存（按实体实例保存动画进度/状态）
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 待机（循环）。 */
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    /** 行走（循环，四条腿交替摆动）。 */
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    /** 受击逃跑（单次播放，惊慌期间控制器持续重新触发）。 */
    private static final RawAnimation ANIM_SCARED_RUN = RawAnimation.begin().thenPlay("scared_run");

    public MimiSheepEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 实体属性：8 心（16 点血）、基础移速 0.23，与原版羊一致。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // —— 音效：直接使用原版羊 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MimiSheepEntity>("main", 5, this::handleAnimState));
    }

    /** PanicGoal 运行中（被攻击或着火逃跑）视为惊慌状态。 */
    private boolean panicGoalRunning() {
        return this.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.isRunning() && wrapped.getGoal() instanceof PanicGoal);
    }

    /** 惊慌 → scared_run；移动 → walk；站立 → idle。 */
    private PlayState handleAnimState(AnimationTest<MimiSheepEntity> state) {
        if (panicGoalRunning()) {
            return state.setAndContinue(ANIM_SCARED_RUN);
        }
        if (state.isMoving()) {
            return state.setAndContinue(ANIM_WALK);
        }
        return state.setAndContinue(ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

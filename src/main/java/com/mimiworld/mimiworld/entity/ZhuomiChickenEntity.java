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
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariant;
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 啄米鸡（Zhuomi Chicken）：GeckoLib 动画友好生物。
 *
 * <p>模型/贴图/动画资源：geckolib/models/zhuomi_chicken.geo.json、
 * textures/entity/zhuomi_chicken.png、geckolib/animations/zhuomi_chicken.animation.json
 * （动画键为 animation.idle / animation.walk）。</p>
 *
 * <p>生物逻辑对齐原版鸡的友好生物部分：水上漂浮、受击惊慌逃跑（加速 1.5）、
 * 避水随机游荡、看向玩家与四处张望；音效使用原版鸡。
 * 暂不注册繁殖/下蛋/被豹猫追捕相关 AI。</p>
 */
public class ZhuomiChickenEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 待机（循环，含啄米动作）。 */
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.idle");
    /** 行走（循环，双腿交替 + 身体起伏）。 */
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.walk");

    public ZhuomiChickenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 实体属性：4 心（8 点血）、基础移速 0.25（对齐原版鸡）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // —— 音效：使用原版鸡经典音色（26.3 动物音效改为变体表，这里取 CLASSIC 成年音色） ——

    private static final ChickenSoundVariant.ChickenSoundSet CHICKEN_SOUNDS_CLASSIC =
            SoundEvents.CHICKEN_SOUNDS.get(ChickenSoundVariants.SoundSet.CLASSIC).adultSounds();

    @Override
    protected SoundEvent getAmbientSound() {
        return CHICKEN_SOUNDS_CLASSIC.ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return CHICKEN_SOUNDS_CLASSIC.hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CHICKEN_SOUNDS_CLASSIC.deathSound().value();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ZhuomiChickenEntity>("main", 4, this::handleAnimState));
    }

    /** 移动（含惊慌逃跑）→ walk，站立 → idle。 */
    private PlayState handleAnimState(AnimationTest<ZhuomiChickenEntity> state) {
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

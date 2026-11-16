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
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.level.Level;

/**
 * 米哞牛（MiMoo Cow）：GeckoLib 动画友好生物。
 *
 * <p>继承 {@link PlaceholderRiceMobEntity} 的友好生物 AI（水上漂浮、避水随机游荡、
 * 看向玩家与四处张望），音效使用原版牛；暂不注册繁殖/挤奶 AI。</p>
 *
 * <p>模型/贴图/动画资源：geckolib/models/mimouniu.geo.json、
 * textures/entity/mimouniu.png、geckolib/animations/mimouniu.animation.json
 * （动画键为 animation.mimouniu.idle / animation.mimouniu.walk）。</p>
 */
public class MimouniuEntity extends PlaceholderRiceMobEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 待机（4 秒循环：呼吸 + 抬头张望 + 歪头 + 甩尾）。 */
    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.mimouniu.idle");
    /** 行走（2 秒循环：对角步态 + 身体起伏 + 头尾联动）。 */
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.mimouniu.walk");

    public MimouniuEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 实体属性：10 心（20 点血）、基础移速 0.2（对齐原版牛）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PlaceholderRiceMobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    // —— 音效：使用原版牛经典音色（26.3 动物音效改为变体表，这里取 CLASSIC 一套） ——

    private static final CowSoundVariant COW_SOUNDS_CLASSIC =
            SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC);

    @Override
    protected SoundEvent getAmbientSound() {
        return COW_SOUNDS_CLASSIC.ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return COW_SOUNDS_CLASSIC.hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return COW_SOUNDS_CLASSIC.deathSound().value();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MimouniuEntity>("main", 5, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<MimouniuEntity> state) {
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

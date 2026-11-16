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
import net.minecraft.world.entity.animal.pig.PigSoundVariant;
import net.minecraft.world.entity.animal.pig.PigSoundVariants;
import net.minecraft.world.level.Level;

/**
 * 米哼猪（MiHeng Pig）：GeckoLib 动画友好生物。
 *
 * <p>继承 {@link PlaceholderRiceMobEntity} 的友好生物 AI（水上漂浮、避水随机游荡、
 * 看向玩家与四处张望），音效使用原版猪；暂不注册繁殖 AI。</p>
 *
 * <p>模型/贴图/动画资源：geckolib/models/mihengzhu.geo.json、
 * textures/entity/mihengzhu.png、geckolib/animations/mihengzhu.animation.json
 * （动画键为 animation.mihengzhu.idle / animation.mihengzhu.walk）。</p>
 */
public class MihengPigEntity extends PlaceholderRiceMobEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 待机（4 秒循环：呼吸 + 东张西望 + 好奇歪头 + 卷尾抽动）。 */
    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.mihengzhu.idle");
    /** 行走（2 秒循环：对角步态 + 胖猪摇摆 + 点头 + 卷尾快摇）。 */
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.mihengzhu.walk");

    public MihengPigEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 实体属性：10 心（20 点血）、基础移速 0.25（对齐原版猪）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PlaceholderRiceMobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    // —— 音效：使用原版猪经典音色（26.3 动物音效改为变体表，这里取 CLASSIC 成年音色） ——

    private static final PigSoundVariant.PigSoundSet PIG_SOUNDS_CLASSIC =
            SoundEvents.PIG_SOUNDS.get(PigSoundVariants.SoundSet.CLASSIC).adultSounds();

    @Override
    protected SoundEvent getAmbientSound() {
        return PIG_SOUNDS_CLASSIC.ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PIG_SOUNDS_CLASSIC.hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PIG_SOUNDS_CLASSIC.deathSound().value();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MihengPigEntity>("main", 5, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<MihengPigEntity> state) {
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

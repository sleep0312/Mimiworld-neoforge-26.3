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
import net.minecraft.world.level.Level;

/**
 * 米傀儡（Rice Golem）：GeckoLib 动画友好生物。
 *
 * <p>继承 {@link PlaceholderRiceMobEntity} 的友好生物 AI；体型/血量对齐原版铁傀儡
 * （碰撞箱 1.4×2.7、100 心），音效沿用原版铁傀儡；作为友好生物暂不挂载攻击 AI。</p>
 *
 * <p>模型/贴图/动画资源：geckolib/models/rice_golem.geo.json、
 * textures/entity/rice_golem.png、geckolib/animations/rice_golem.animation.json
 * （动画键为 animation.rice_golem.idle / animation.rice_golem.walk）。</p>
 */
public class RiceGolemEntity extends PlaceholderRiceMobEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 待机（4 秒循环：呼吸 + 环顾）。 */
    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.rice_golem.idle");
    /** 行走（2 秒循环：双腿前后摆动、双臂摆动、身体起伏）。 */
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.rice_golem.walk");

    public RiceGolemEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 实体属性：100 心（200 点血）、基础移速 0.25、满击退抗性，对齐原版铁傀儡。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PlaceholderRiceMobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    // 铁傀儡原版无待机叫声，ambient 沿用基类 null

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RiceGolemEntity>("main", 5, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<RiceGolemEntity> state) {
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

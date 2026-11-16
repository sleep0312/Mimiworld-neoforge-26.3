package com.mimiworld.mimiworld.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.mimiworld.mimiworld.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 喷米鱼（Penmi Fish）：水生生物。
 *
 * <p>继承原版 {@link AbstractFish}，自带水下随机游泳、离水扑腾、用水桶装取；
 * 生命 4 心（8 HP），游泳速度与原版鳕鱼一致（1.0）。</p>
 *
 * <p>模型/贴图/动画资源：geckolib/models/penmiyu.geo.json、
 * textures/entity/penmiyu.png、geckolib/animations/penmiyu.animation.json
 * （动画键为 animation.penmiyu.swim / animation.penmiyu.idle）。</p>
 */
public class PenmiFishEntity extends AbstractFish implements GeoEntity {

    private static final RawAnimation ANIM_SWIM = RawAnimation.begin().thenLoop("animation.penmiyu.swim");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.penmiyu.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PenmiFishEntity(EntityType<? extends AbstractFish> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COD_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.PENMI_FISH_BUCKET.get());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PenmiFishEntity>("main", 5, this::handleAnimState));
    }

    /** 水中游动播 swim，静止（悬浮/离水不动）播 idle。 */
    private PlayState handleAnimState(AnimationTest<PenmiFishEntity> state) {
        boolean swimming = state.isMoving() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        return state.setAndContinue(swimming ? ANIM_SWIM : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

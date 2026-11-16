package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 米魂（注册 id = rice_soul）——飘在空中的米之魂，敌对飞行近战生物。
 *
 * <p>GeckoLib 资源：geckolib/models/mihun.geo.json、
 * textures/entity/mihun.png、geckolib/animations/mihun.animation.json
 * （动画键 animation.mihun.idle / walk / attack）。碰撞箱 1.1×1.6。</p>
 *
 * <p>9 心、近战 3、飞行速度 1.0、追击 32；无目标时空中随机漂浮。
 * 原自定义米魂音效尚未移植，暂用原版灵魂逃逸音色。</p>
 */
public class RiceSoulEntity extends AbstractFlyingRiceMobEntity {

    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.mihun.idle");
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.mihun.walk");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().thenPlay("animation.mihun.attack");

    /** attack 动画时长 1 秒 = 20 tick */
    private static final int ATTACK_ANIMATION_TICKS = 20;

    public RiceSoulEntity(EntityType<? extends RiceSoulEntity> type, Level level) {
        super(type, level);
    }

    /** 9 心、移速 0.25、飞行 1.0、近战 3、追击 32。 */
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected RawAnimation idleAnimation() {
        return ANIM_IDLE;
    }

    @Override
    protected RawAnimation moveAnimation() {
        return ANIM_WALK;
    }

    @Override
    protected RawAnimation attackAnimation() {
        return ANIM_ATTACK;
    }

    @Override
    protected int attackAnimationTicks() {
        return ATTACK_ANIMATION_TICKS;
    }

    // —— 音效：暂用原版灵魂逃逸 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }
}

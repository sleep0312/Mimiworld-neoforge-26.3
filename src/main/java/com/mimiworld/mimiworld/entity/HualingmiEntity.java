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
 * 化灵米（Hualing Rice，注册 id = hualing_rice）——大米成精的空中敌对飞灵。
 *
 * <p>GeckoLib 资源：geckolib/models/hualingmi.geo.json、
 * textures/entity/hualingmi.png、geckolib/animations/hualingmi.animation.json
 * （动画键 animation.hualingmi.idle / fly / attack）。碰撞箱 1.0×2.0。</p>
 *
 * <p>恼鬼式常驻悬浮、永不落地但不能穿墙（寻路不穿门）：
 * 5000 心、近战 10、飞行速度 1.2、追击 40；音效沿用原版恼鬼。</p>
 */
public class HualingmiEntity extends AbstractFlyingRiceMobEntity {

    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.hualingmi.idle");
    private static final RawAnimation ANIM_FLY =
            RawAnimation.begin().thenLoop("animation.hualingmi.fly");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().thenPlay("animation.hualingmi.attack");

    /** attack 动画时长约 1.17 秒 = 24 tick */
    private static final int ATTACK_ANIMATION_TICKS = 24;

    public HualingmiEntity(EntityType<? extends HualingmiEntity> type, Level level) {
        super(type, level);
    }

    /** 5000 心、近战 10、移速 0.25、飞行 1.2、追击 40。 */
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 1.2D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected RawAnimation idleAnimation() {
        return ANIM_IDLE;
    }

    @Override
    protected RawAnimation moveAnimation() {
        return ANIM_FLY;
    }

    @Override
    protected RawAnimation attackAnimation() {
        return ANIM_ATTACK;
    }

    @Override
    protected int attackAnimationTicks() {
        return ATTACK_ANIMATION_TICKS;
    }

    // —— 音效：沿用原版恼鬼 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }
}

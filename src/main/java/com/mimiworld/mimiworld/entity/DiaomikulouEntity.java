package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 雕米骷髅（Carved Rice Skeleton，注册 id = carved_rice_skeleton）——大米主题敌对亡灵。
 *
 * <p>GeckoLib 资源：geckolib/models/diaomikulou.geo.json、
 * textures/entity/diaomikulou.png、geckolib/animations/diaomikulou.animation.json
 * （动画键 animation.diaomikulou.idle / walk / attack）。</p>
 *
 * <p>60 心（120 血）、移速 0.25、攻击力 5，音效使用原版骷髅；白天不自燃。</p>
 */
public class DiaomikulouEntity extends AbstractRiceUndeadEntity {

    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.diaomikulou.idle");
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.diaomikulou.walk");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().thenPlay("animation.diaomikulou.attack");

    /** attack 动画时长 0.8 秒 = 16 tick */
    private static final int ATTACK_ANIMATION_TICKS = 16;

    public DiaomikulouEntity(EntityType<? extends DiaomikulouEntity> type, Level level) {
        super(type, level);
    }

    /** 60 心、移速 0.25、攻击力 5。 */
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected RawAnimation idleAnimation() {
        return ANIM_IDLE;
    }

    @Override
    protected RawAnimation walkAnimation() {
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

    // —— 音效：沿用原版骷髅 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
    }
}

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
 * 米哼猪尸（注册 id = miheng_pig_corpse）——腐烂起身的米哼猪尸体，陆地近战敌对亡灵。
 *
 * <p>GeckoLib 资源：geckolib/models/mihengzhushi.geo.json、
 * textures/entity/mihengzhushi.png、geckolib/animations/mihengzhushi.animation.json
 * （动画键 animation.mihengzhushi.idle / walk / attack）。</p>
 *
 * <p>30 心（60 血）、移速 0.23、攻击力 5；音效沿用原版疣猪兽。</p>
 */
public class MihengPigCorpseEntity extends AbstractRiceUndeadEntity {

    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.mihengzhushi.idle");
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.mihengzhushi.walk");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().thenPlay("animation.mihengzhushi.attack");

    /** attack 动画时长 1 秒 = 20 tick */
    private static final int ATTACK_ANIMATION_TICKS = 20;

    public MihengPigCorpseEntity(EntityType<? extends MihengPigCorpseEntity> type, Level level) {
        super(type, level);
    }

    /** 30 心、移速 0.23、攻击力 5。 */
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
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

    // —— 音效：沿用原版疣猪兽 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.HOGLIN_STEP, 0.15F, 0.8F);
    }
}

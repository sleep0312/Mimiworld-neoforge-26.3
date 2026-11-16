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
 * 糊米古尸（Humi Corpse，注册 id = humi_corpse）——中式跳尸（jiangshi）。
 *
 * <p>GeckoLib 资源：geckolib/models/humi_corpse.geo.json、
 * textures/entity/humi_corpse.png、geckolib/animations/humi_corpse.animation.json
 * （动画键 animation.idle / animation.walk / animation.attack，walk 为双腿同步僵尸跳）。</p>
 *
 * <p>30 心（60 血）、移速 0.23、攻击力 3（对齐原版僵尸），音效使用原版僵尸；白天不自燃。</p>
 */
public class HumiCorpseEntity extends AbstractRiceUndeadEntity {

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("animation.attack");

    /** attack 动画时长 0.9 秒 = 18 tick */
    private static final int ATTACK_ANIMATION_TICKS = 18;

    public HumiCorpseEntity(EntityType<? extends HumiCorpseEntity> type, Level level) {
        super(type, level);
    }

    /** 30 心、移速 0.23、攻击力 3（后两者对齐原版僵尸）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
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

    // —— 音效：沿用原版僵尸 ——

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 0.9F);
    }
}

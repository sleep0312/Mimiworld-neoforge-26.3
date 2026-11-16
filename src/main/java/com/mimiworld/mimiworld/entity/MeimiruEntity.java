package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 没米入（Meimiru，注册 id = momi_entrant）——末影人式精英（大米主题）。
 *
 * <p>平时中立游荡，被玩家盯着看（视线锥 + 视线检测）或受击后激怒并近战扑击；
 * 追击时若距离过远会带着末影传送音效与 PORTAL 粒子瞬移到目标附近，空闲时也会随机瞬移。</p>
 *
 * <p>75 心（150 血）、攻击 7、移速 0.3、追击 64，与原版末影人其余属性一致。</p>
 */
public class MeimiruEntity extends AbstractRiceGeoMonsterEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.attack");

    /** attack 动画时长 0.9 秒 = 18 tick */
    private static final int ATTACK_ANIMATION_TICKS = 18;

    /** 被注视索敌的视线锥（同原版末影人）。 */
    private static final double STARE_CONE = 0.025D;

    private int blinkCooldown = 60;

    public MeimiruEntity(EntityType<? extends MeimiruEntity> type, Level level) {
        super(type, level, "meimiru",
                SoundEvents.ENDERMAN_AMBIENT, SoundEvents.ENDERMAN_HURT, SoundEvents.ENDERMAN_DEATH, null);
    }

    @Override
    protected RawAnimation idleAnimation() {
        return IDLE;
    }

    @Override
    protected RawAnimation walkAnimation() {
        return MOVE;
    }

    @Override
    protected RawAnimation attackAnimation() {
        return ATTACK;
    }

    @Override
    protected int attackAnimationTicks() {
        return ATTACK_ANIMATION_TICKS;
    }

    /** 属性：75 心 = 150 血、攻击 7、移速 0.3、追击 64（同原版末影人 + 血量提升）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return bossAttributes(150.0D, 7.0D, 0.30D, 0.0D, 0.0D, 0.0D, 64.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // 末影人式中立：不主动索敌，只在受击或与玩家对视后激怒
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    protected void bossTick() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            // 被玩家盯着看：激怒并吼叫
            if (this.tickCount % 10 == 0) {
                Player staring = this.findStaringPlayer();
                if (staring != null) {
                    this.setTarget(staring);
                    this.playSound(SoundEvents.ENDERMAN_SCREAM, 1.0F, 0.9F);
                    this.blinkCooldown = 20;
                }
            }
            // 空闲时随机瞬移
            if (this.blinkCooldown > 0) {
                this.blinkCooldown--;
            } else if (this.random.nextInt(80) == 0 && this.randomBlink(16.0D)) {
                this.blinkCooldown = 60;
            }
            return;
        }
        // 追击时若离目标太远，瞬移到目标附近（末影人式扑击）
        if (this.blinkCooldown > 0) {
            this.blinkCooldown--;
        } else if (this.distanceToSqr(target) > 16.0D && this.teleportTowards(target)) {
            this.blinkCooldown = 60;
        }
    }

    /** 找到正在与自己对视的玩家（排除创造/旁观）。 */
    private Player findStaringPlayer() {
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(64.0D))) {
            if (player.isAlive() && !player.isCreative() && !player.isSpectator()
                    && this.isLookingAtMe(player, STARE_CONE, true, false, this.getEyeY())) {
                return player;
            }
        }
        return null;
    }

    /** 瞬移到目标附近（同原版末影人的 teleportTowards）。 */
    private boolean teleportTowards(Entity target) {
        Vec3 dir = new Vec3(this.getX() - target.getX(),
                this.getY(0.5D) - target.getEyeY(),
                this.getZ() - target.getZ()).normalize();
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 8.0D - dir.x * 16.0D;
        double y = this.getY() + this.random.nextInt(16) - 8 - dir.y * 16.0D;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 8.0D - dir.z * 16.0D;
        return this.blinkTo(x, y, z);
    }

    /** 在自身附近随机瞬移。 */
    private boolean randomBlink(double radius) {
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 2.0D * radius;
        double y = this.getY() + this.random.nextInt(16) - 8;
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 2.0D * radius;
        return this.blinkTo(x, y, z);
    }

    /** 末影传送：随机落点 + 传送音效 + 两端 PORTAL 粒子。 */
    private boolean blinkTo(double x, double y, double z) {
        if (this.isPassenger()) {
            return false;
        }
        Vec3 from = this.position();
        if (!this.randomTeleport(x, y, z, true, BlockTags.ENDERMAN_DOES_NOT_TELEPORT_TO)) {
            return false;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    from.x, from.y + 1.0D, from.z, 32, 0.5D, 0.5D, 0.5D, 0.5D);
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 32, 0.5D, 0.5D, 0.5D, 0.5D);
        }
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }
}

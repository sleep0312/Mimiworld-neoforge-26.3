package com.mimiworld.mimiworld.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/**
 * 米莱马（Milaima，注册 id = milai_horse）——大米主题的敌对史莱姆，米浆凝胶成精。
 *
 * <p>GeckoLib 资源：geckolib/models/milaima.geo.json、
 * textures/entity/milaima.png、geckolib/animations/milaima.animation.json
 * （动画键 animation.milaima.idle / walk / attack）。</p>
 *
 * <p>直接继承原版史莱姆：随机蹦跳、朝目标扑跳近战、squish 音效；固定 2 号体型
 * （1.02×1.02）、12 心、接触伤害 4。与原版差异：死亡不分裂、不掉原版黏液球
 * （掉落走 mod 战利品表），攻击扑跳时触发一次 attack 动画。</p>
 */
public class MilaimaEntity extends Slime implements GeoEntity {

    /** 固定史莱姆体型：2 号 → 碰撞箱 1.02×1.02。 */
    private static final int SLIME_SIZE = 2;

    /** 同步数据：攻击动画剩余 tick。 */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(MilaimaEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE =
            RawAnimation.begin().thenLoop("animation.milaima.idle");
    private static final RawAnimation ANIM_WALK =
            RawAnimation.begin().thenLoop("animation.milaima.walk");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().thenPlay("animation.milaima.attack");

    /** attack 动画时长 0.8 秒 = 16 tick */
    private static final int ATTACK_ANIMATION_TICKS = 16;

    public MilaimaEntity(EntityType<? extends MilaimaEntity> type, Level level) {
        super(type, level);
        this.setSize(SLIME_SIZE, false);
    }

    /** 12 心、移速 0.28、攻击属性 4（实际接触伤害以 getAttackDamage 为准）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    /** 接触伤害：2 号史莱姆原版只有 2 点，这里提到 4 点。 */
    @Override
    protected float getAttackDamage() {
        return 4.0F;
    }

    /** 死亡不分裂（分裂数量置 0）。 */
    @Override
    protected int getSplitCount() {
        return 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MilaimaEntity>("main", 5, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<MilaimaEntity> state) {
        if (this.entityData.get(DATA_ATTACK_ANIM_TICKS) > 0) {
            return state.setAndContinue(ANIM_ATTACK);
        }
        return state.setAndContinue(state.isMoving() ? ANIM_WALK : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** 蹦跳起跳瞬间：目标在 4 格内视为攻击扑跳，触发一次 attack 动画。 */
    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        if (!this.level().isClientSide()) {
            LivingEntity target = this.getTarget();
            if (target != null && this.entityData.get(DATA_ATTACK_ANIM_TICKS) == 0
                    && this.distanceToSqr(target) < 16.0D) {
                this.entityData.set(DATA_ATTACK_ANIM_TICKS, ATTACK_ANIMATION_TICKS);
            }
        }
    }

    /** 兜底：其他途径结算近战命中时也触发攻击动画。 */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            this.entityData.set(DATA_ATTACK_ANIM_TICKS, ATTACK_ANIMATION_TICKS);
            return true;
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            int ticks = this.entityData.get(DATA_ATTACK_ANIM_TICKS);
            if (ticks > 0) {
                this.entityData.set(DATA_ATTACK_ANIM_TICKS, ticks - 1);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_ANIM_TICKS, 0);
    }

    /** 从存档读取后仍固定回 2 号体型（米莱马体型恒定）。 */
    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSize(SLIME_SIZE, false);
    }

    /** 不掉原版黏液球——掉落由 mod 实体战利品表提供。 */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
    }
}

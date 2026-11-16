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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 米米世界「近战亡灵」通用基类。
 *
 * <p>统一实现：水上漂浮、近战追击、避水游荡、看向玩家与四处张望，
 * 受击反击并主动索敌玩家；命中时通过同步数据触发一次 attack 动画。</p>
 *
 * <p>亡灵药水反转（治疗药水受伤、伤害药水治疗）由 {@link #isInvertedHealAndHarm()} 控制；
 * 「亡灵杀手」附魔加成由 {@code #minecraft:undead} 实体标签决定，
 * 相关实体已在 data/minecraft/tags/entity_type/undead.json 中声明。</p>
 *
 * <p>子类只需提供属性、音效与三段动画键/攻击动画时长。</p>
 */
public abstract class AbstractRiceUndeadEntity extends Monster implements GeoEntity {

    /**
     * 同步数据：攻击动画剩余 tick（服务端命中时置为子类给出的时长并逐 tick 递减，
     * 自动同步到客户端；GeckoLib 动画在客户端播放，必须用 SynchedEntityData）。
     */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM_TICKS =
            SynchedEntityData.defineId(AbstractRiceUndeadEntity.class, EntityDataSerializers.INT);

    // GeckoLib：每个实体实例持有一个动画实例缓存
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractRiceUndeadEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** 待机循环动画。 */
    protected abstract RawAnimation idleAnimation();

    /** 行走循环动画。 */
    protected abstract RawAnimation walkAnimation();

    /** 命中时单次播放的攻击动画。 */
    protected abstract RawAnimation attackAnimation();

    /** 攻击动画时长（tick）。 */
    protected abstract int attackAnimationTicks();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_ANIM_TICKS, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AbstractRiceUndeadEntity>("main", 5, this::handleAnimState));
    }

    /**
     * 动画状态：命中后的若干 tick 内播单次 attack；否则移动播 walk、静止播 idle。
     * setAndContinue 保证同名动画不会被逐帧重启，attack 单次播完自然切回。
     */
    private PlayState handleAnimState(AnimationTest<AbstractRiceUndeadEntity> state) {
        if (this.entityData.get(DATA_ATTACK_ANIM_TICKS) > 0) {
            return state.setAndContinue(attackAnimation());
        }
        return state.setAndContinue(state.isMoving() ? walkAnimation() : idleAnimation());
    }

    /** 命中目标时触发一次攻击动画（服务端伤害结算成功时调用，数据自动同步）。 */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (super.doHurtTarget(level, target)) {
            this.entityData.set(DATA_ATTACK_ANIM_TICKS, attackAnimationTicks());
            return true;
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // 仅服务端倒计时并驱动同步，客户端每 tick 收到递减后的新值
        if (!this.level().isClientSide()) {
            int ticks = this.entityData.get(DATA_ATTACK_ANIM_TICKS);
            if (ticks > 0) {
                this.entityData.set(DATA_ATTACK_ANIM_TICKS, ticks - 1);
            }
        }
    }

    /** 亡灵：治疗药水造成伤害、伤害药水治疗。 */
    @Override
    public boolean isInvertedHealAndHarm() {
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

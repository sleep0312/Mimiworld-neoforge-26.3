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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 嗥米狼（Haomi Wolf，注册 id = haomi_wolf）——大米主题友好狼。
 *
 * <p>GeckoLib 资源：geckolib/models/haomi_wolf.geo.json、
 * textures/entity/haomi_wolf.png、geckolib/animations/haomi_wolf.animation.json
 * （动画键 animation.idle / animation.walk）。</p>
 *
 * <p>友好生物（对齐原版狼的温和部分）：漂浮、受击惊慌逃跑、避水游荡、看玩家与张望；
 * 20 心、移速 0.3，音效使用原版狼；暂不注册驯服/繁殖/攻击 AI。</p>
 */
public class HaomiWolfEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.walk");

    public HaomiWolfEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 20 心（40 血）、移速 0.3（对齐原版狼）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // —— 音效：使用原版狼经典音色（26.3 动物音效改为变体表） ——

    private static final WolfSoundVariant.WolfSoundSet WOLF_SOUNDS_CLASSIC =
            SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.SoundSet.CLASSIC).adultSounds();

    @Override
    protected SoundEvent getAmbientSound() {
        return WOLF_SOUNDS_CLASSIC.ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WOLF_SOUNDS_CLASSIC.hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WOLF_SOUNDS_CLASSIC.deathSound().value();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HaomiWolfEntity>("main", 4, this::handleAnimState));
    }

    private PlayState handleAnimState(AnimationTest<HaomiWolfEntity> state) {
        return state.setAndContinue(state.isMoving() ? ANIM_WALK : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

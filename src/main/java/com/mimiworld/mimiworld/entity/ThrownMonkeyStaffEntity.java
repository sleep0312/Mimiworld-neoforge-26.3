package com.mimiworld.mimiworld.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import com.mimiworld.mimiworld.ModItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 米米猴王扔出的金箍棒投掷物：脱手后无重力直线飞行 20 tick（1 秒），
 * 随后自动追踪猴王飞回手中；撞上方块会立即折返。
 *
 * <p>去程与回程沿途扫过的生物受到 14 点伤害，同一次扔棒中每个实体最多受伤一次，
 * 不会伤到猴王自己及其同盟。服务端权威移动（直接 setPos），客户端只做位置插值。</p>
 */
public class ThrownMonkeyStaffEntity extends ThrowableProjectile implements GeoEntity, ItemSupplier {

    /** 路径伤害 */
    public static final float DAMAGE = 14.0F;
    /** 去程速度（格/tick） */
    private static final double OUT_SPEED = 1.1D;
    /** 回程追踪速度（格/tick） */
    private static final double RETURN_SPEED = 2.4D;
    /** 去程时长：1 秒 = 20 tick */
    private static final int OUT_TICKS = 20;
    /** 兜底存活上限（tick） */
    private static final int MAX_LIFE = 80;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 去程直线方向（单位向量） */
    private Vec3 direction = Vec3.ZERO;
    /** 是否已进入折返阶段 */
    private boolean returning = false;
    private int life;
    /** 本次扔棒已击伤的实体，避免重复伤害 */
    private final Set<UUID> hitEntities = new HashSet<>();

    public ThrownMonkeyStaffEntity(EntityType<? extends ThrownMonkeyStaffEntity> type, Level level) {
        super(type, level);
    }

    public ThrownMonkeyStaffEntity(EntityType<? extends ThrownMonkeyStaffEntity> type,
                                   LivingEntity shooter, Level level) {
        super(type, level);
        this.setOwner(shooter);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    /** 出手：指定目标点，计算无重力直线方向。应在 setPos 设定脱手起点后调用。 */
    public void launchTowards(Vec3 targetPos) {
        Vec3 velocity = targetPos.subtract(this.position());
        if (velocity.lengthSqr() < 1.0E-6D) {
            velocity = this.getLookAngle();
        }
        this.direction = velocity.normalize();
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide()) {
            this.setDeltaMovement(Vec3.ZERO);
            super.tick();
            return;
        }

        this.baseTick();
        Entity owner = this.getOwner();

        Vec3 oldPos = this.position();
        Vec3 newPos;
        if (!this.returning) {
            this.life++;
            newPos = oldPos.add(this.direction.scale(OUT_SPEED));
            BlockHitResult clip = this.level().clip(new ClipContext(
                    oldPos, newPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (clip.getType() == HitResult.Type.BLOCK) {
                newPos = clip.getLocation();
                this.returning = true;
            } else if (this.life >= OUT_TICKS) {
                this.returning = true;
            }
            if (owner == null || !owner.isAlive()) {
                this.discard();
                return;
            }
        } else {
            if (this.life > MAX_LIFE || !(owner instanceof LivingEntity) || !owner.isAlive()) {
                this.discard();
                return;
            }
            Vec3 returnTarget = new Vec3(owner.getX(), owner.getY() + 1.3D, owner.getZ());
            Vec3 delta = returnTarget.subtract(oldPos);
            if (delta.length() <= RETURN_SPEED + 0.4D) {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 1.4F);
                this.discard();
                return;
            }
            newPos = oldPos.add(delta.normalize().scale(RETURN_SPEED));
        }

        hurtEntitiesOnPath(oldPos, newPos, owner);

        double dx = newPos.x - oldPos.x;
        double dz = newPos.z - oldPos.z;
        if (dx * dx + dz * dz > 1.0E-7D) {
            this.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        }
        this.setPos(newPos);
    }

    private void hurtEntitiesOnPath(Vec3 oldPos, Vec3 newPos, Entity owner) {
        AABB path = this.getBoundingBox().expandTowards(newPos.subtract(oldPos)).inflate(0.35D);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, path, target -> {
            if (!target.isAlive() || target == owner || this.hitEntities.contains(target.getUUID())) {
                return false;
            }
            return owner == null || !target.isAlliedTo(owner);
        });
        for (LivingEntity target : entities) {
            this.hitEntities.add(target.getUUID());
            ModDamage.hurt(target, this.damageSources().thrown(this, owner), DAMAGE);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 投掷物无状态动画
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.MONKEY_KING_RUYI_STAFF.get());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

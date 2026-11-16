package com.mimiworld.mimiworld.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * 龙系 Boss 光束的「渲染载体实体」：本身无碰撞、无 AI、不存档、不造成伤害
 * （伤害由龙的技能逻辑结算）。只负责把施放者与锁定玩家的网络 ID 同步到客户端，
 * 由 {@code DragonBeamRenderer} 在两者之间渲染一条紫色光束。
 */
public class DragonBeamEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(DragonBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(DragonBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE_TICKS =
            SynchedEntityData.defineId(DragonBeamEntity.class, EntityDataSerializers.INT);

    public DragonBeamEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setSilent(true);
        this.setInvisible(true);
    }

    /** 服务端生成一条持续 {@code lifeTicks} 的光束。 */
    public static DragonBeamEntity spawn(ServerLevel level, PathfinderMob dragon,
                                         Player target, int lifeTicks) {
        DragonBeamEntity beam = new DragonBeamEntity(ModEntities.DRAGON_BEAM.get(), level);
        beam.setPos(dragon.getX(), dragon.getY(), dragon.getZ());
        beam.setYRot(dragon.getYRot());
        beam.entityData.set(DATA_OWNER_ID, dragon.getId());
        beam.entityData.set(DATA_TARGET_ID, target.getId());
        beam.entityData.set(DATA_LIFE_TICKS, lifeTicks);
        level.addFreshEntity(beam);
        return beam;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_ID, -1);
        builder.define(DATA_TARGET_ID, -1);
        builder.define(DATA_LIFE_TICKS, 0);
    }

    @Override
    public void tick() {
        Entity owner = this.getOwner();
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
        }
        if (!this.level().isClientSide()) {
            int life = this.entityData.get(DATA_LIFE_TICKS);
            if (life <= 1 || !(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) {
                this.discard();
                return;
            }
            this.entityData.set(DATA_LIFE_TICKS, life - 1);
            this.spawnBeamParticles((ServerLevel) this.level(), livingOwner);
        }
    }

    /**
     * 26.3 的自定义几何渲染管线改为提交式（SubmitNodeCollector），这里改用服务端粒子
     * 沿「施放者眼部 → 锁定玩家躯干」铺一条紫色光束，客户端无需额外渲染器。
     */
    private void spawnBeamParticles(ServerLevel level, LivingEntity owner) {
        Player target = this.getTargetPlayer();
        if (target == null || !target.isAlive() || target.isSpectator() || target.isCreative()) {
            return;
        }
        Vec3 start = owner.getEyePosition();
        Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.1D) {
            return;
        }
        int points = (int) Math.min(48, Math.max(6, length * 1.5D));
        for (int i = 0; i <= points; i++) {
            Vec3 p = start.add(delta.scale(i / (double) points));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
            }
        }
    }

    public void setTarget(Player target) {
        this.entityData.set(DATA_TARGET_ID, target.getId());
    }

    public Entity getOwner() {
        int id = this.entityData.get(DATA_OWNER_ID);
        return id == -1 ? null : this.level().getEntity(id);
    }

    public Player getTargetPlayer() {
        int id = this.entityData.get(DATA_TARGET_ID);
        if (id == -1) {
            return null;
        }
        Entity entity = this.level().getEntity(id);
        return entity instanceof Player player ? player : null;
    }

    public int getLifeTicks() {
        return this.entityData.get(DATA_LIFE_TICKS);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 96.0D * 96.0D * 4.0D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    // 不存档：纯瞬时技能特效
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}

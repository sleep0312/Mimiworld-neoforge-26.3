package com.mimiworld.mimiworld.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * 伤害结算辅助。
 *
 * <p>26.3 将 {@code Entity#hurt} 拆分为 {@code hurtServer}/{@code hurtClient}，
 * 旧方法已弃用。模组内的攻击、技能与投射物命中都应以服务端为准，
 * 这里统一转交 {@link Entity#hurtServer}，避免逐个调用点重复判空。</p>
 */
public final class ModDamage {

    private ModDamage() {
    }

    /** 在服务端对目标造成伤害；客户端调用时直接忽略。 */
    public static void hurt(Entity target, DamageSource source, float amount) {
        if (target.level() instanceof ServerLevel serverLevel) {
            target.hurtServer(serverLevel, source, amount);
        }
    }
}

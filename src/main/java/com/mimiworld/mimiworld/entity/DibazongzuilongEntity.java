package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;
import com.mimiworld.mimiworld.sound.ModSounds;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 第八宗罪龙：最终 Boss，龙系超巨型陆空 Boss，体型为最懒终末龙的 1.5 倍，
 * 技能复用 {@link AbstractDragonBossEntity}。
 *
 * <p>常驻无重力飞行、免疫击退、紫色 Boss 血条、末影龙音效。2500 血、100 甲。</p>
 */
public class DibazongzuilongEntity extends AbstractDragonBossEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.circling");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.melee_bite");

    /**
     * 身体部位（偏移取自 dibazongzuilong.geo.json 各骨骼实测包围盒，单位格；
     * 模型局部 -Z 为头的方向）：躯干 5.8×8.5、颈 3.0×4.1、头 4.5×6.1、
     * 尾两段、双翼各三段、四肢各一件，共 15 个部位。
     */
    private static final DragonPart[] PARTS = {
            new DragonPart("body", 5.8F, 8.5F, 0.0F, 4.6F, -0.65F),
            new DragonPart("neck", 3.0F, 4.1F, 0.0F, 8.4F, -11.9F),
            new DragonPart("head", 4.5F, 6.1F, 0.0F, 6.7F, -17.25F),
            new DragonPart("tail_1", 4.4F, 4.0F, 0.0F, 6.55F, 10.5F),
            new DragonPart("tail_2", 2.9F, 3.6F, 0.0F, 7.5F, 17.35F),
            new DragonPart("wing_left_inner", 7.6F, 2.6F, 5.1F, 9.0F, -0.2F),
            new DragonPart("wing_left_mid", 10.5F, 2.7F, 12.5F, 9.95F, -2.2F),
            new DragonPart("wing_left_outer", 12.7F, 1.6F, 22.4F, 10.5F, -3.9F),
            new DragonPart("wing_right_inner", 7.6F, 2.6F, -5.1F, 9.0F, -0.2F),
            new DragonPart("wing_right_mid", 10.5F, 2.7F, -12.5F, 9.95F, -2.2F),
            new DragonPart("wing_right_outer", 12.7F, 1.6F, -22.4F, 10.5F, -3.9F),
            new DragonPart("leg_front_left", 2.6F, 7.1F, 2.05F, 1.7F, -4.6F),
            new DragonPart("leg_front_right", 2.6F, 7.1F, -2.05F, 1.7F, -4.6F),
            new DragonPart("leg_back_left", 2.9F, 7.3F, 2.0F, 1.7F, 5.4F),
            new DragonPart("leg_back_right", 2.9F, 7.3F, -2.0F, 1.7F, 5.4F)
    };

    /** 龙嘴水平前伸 15.9 格（颌 pivot z=-255px/16）、相对脚点高 8.6 格（y=138px/16）。 */
    private static final float MOUTH_FORWARD = 15.9F;
    private static final float MOUTH_UP = 8.6F;

    public DibazongzuilongEntity(EntityType<? extends DibazongzuilongEntity> type, Level level) {
        super(type, level, "dibazongzuilong",
                ModSounds.DIBAZONGZUILONG_AMBIENT.get(),
                ModSounds.DIBAZONGZUILONG_HURT.get(),
                ModSounds.DIBAZONGZUILONG_DEATH.get(),
                ModSounds.DIBAZONGZUILONG_ROAR.get(),
                MOUTH_FORWARD, MOUTH_UP, PARTS);
    }

    @Override
    protected RawAnimation idleAnimation() {
        return IDLE;
    }

    @Override
    protected RawAnimation walkAnimation() {
        return IDLE;
    }

    @Override
    protected RawAnimation attackAnimation() {
        return ATTACK;
    }

    /**
     * 属性：2500 血、普攻 100、飞行加速 4.5、索敌 64、100 甲 6 韧性、免疫击退。
     * 飞行生物空中稳态速度 ≈ 0.222 × MoveControl速度系数 × FLYING_SPEED（格/tick）。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 4.5D)
                .add(Attributes.ATTACK_DAMAGE, 100.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 100.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}

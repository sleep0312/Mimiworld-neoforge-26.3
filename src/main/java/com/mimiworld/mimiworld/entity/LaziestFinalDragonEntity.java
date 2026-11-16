package com.mimiworld.mimiworld.entity;

import com.geckolib.animation.RawAnimation;
import com.mimiworld.mimiworld.sound.ModSounds;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 最懒终末龙：末影龙式超巨型陆空 Boss（技能见 {@link AbstractDragonBossEntity}）。
 *
 * <p>常驻无重力飞行、免疫击退、紫色 Boss 血条、末影龙音效。5000 血、100 甲。</p>
 */
public class LaziestFinalDragonEntity extends AbstractDragonBossEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.circling");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.melee_bite");

    /**
     * 身体部位（偏移取自 laziest_final_dragon.geo.json 各骨骼实测包围盒，单位格；
     * 模型局部 -Z 为头的方向）：躯干 3.0×5.3、颈 1.6×2.6、头 1.8×3.4、
     * 尾两段、双翼各内外两段、四肢各一件，共 13 个部位。
     */
    private static final DragonPart[] PARTS = {
            new DragonPart("body", 3.0F, 5.3F, 0.0F, 3.4F, -0.2F),
            new DragonPart("neck", 1.6F, 2.6F, 0.0F, 5.65F, -7.95F),
            new DragonPart("head", 1.8F, 3.4F, 0.0F, 5.15F, -11.5F),
            new DragonPart("tail_1", 2.0F, 2.6F, 0.0F, 4.4F, 7.0F),
            new DragonPart("tail_2", 1.1F, 2.1F, 0.0F, 4.9F, 11.55F),
            new DragonPart("wing_left_inner", 2.3F, 1.5F, 2.0F, 6.0F, -0.4F),
            new DragonPart("wing_left_outer", 5.5F, 1.2F, 5.4F, 6.6F, -2.6F),
            new DragonPart("wing_right_inner", 2.3F, 1.5F, -2.0F, 6.0F, -0.4F),
            new DragonPart("wing_right_outer", 5.5F, 1.2F, -5.4F, 6.6F, -2.6F),
            new DragonPart("leg_front_left", 1.3F, 4.8F, 1.15F, 1.15F, -3.3F),
            new DragonPart("leg_front_right", 1.3F, 4.8F, -1.15F, 1.15F, -3.3F),
            new DragonPart("leg_back_left", 1.5F, 4.9F, 1.1F, 1.15F, 3.4F),
            new DragonPart("leg_back_right", 1.5F, 4.9F, -1.1F, 1.15F, 3.4F)
    };

    /** 龙嘴水平前伸 10.6 格（颌 pivot z≈-170px/16）、相对脚点高 5.6 格（y≈92px/16）。 */
    private static final float MOUTH_FORWARD = 10.6F;
    private static final float MOUTH_UP = 5.6F;

    public LaziestFinalDragonEntity(EntityType<? extends LaziestFinalDragonEntity> type, Level level) {
        super(type, level, "laziest_final_dragon",
                ModSounds.LAZIEST_FINAL_DRAGON_AMBIENT.get(),
                ModSounds.LAZIEST_FINAL_DRAGON_HURT.get(),
                ModSounds.LAZIEST_FINAL_DRAGON_DEATH.get(),
                ModSounds.LAZIEST_FINAL_DRAGON_ROAR.get(),
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
     * 属性：5000 血、普攻 100、飞行加速 4.5、索敌 64、100 甲 6 韧性、免疫击退。
     * 飞行生物空中稳态速度 ≈ 0.222 × MoveControl速度系数 × FLYING_SPEED（格/tick）。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 4.5D)
                .add(Attributes.ATTACK_DAMAGE, 100.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 100.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}

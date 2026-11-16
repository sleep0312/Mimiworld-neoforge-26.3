package com.mimiworld.mimiworld.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.mimiworld.mimiworld.client.model.MimiBowModel;
import com.mimiworld.mimiworld.client.renderer.MimiBowRenderer;
import com.mimiworld.mimiworld.entity.MimiArrowEntity;
import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;

import java.util.function.Consumer;

/**
 * 米米世界弓物品：在 {@link BowItem} 基础上附带固定伤害倍率与 GeckoLib 3D 渲染。
 *
 * <p>七把弓共用本类：
 * 饭晶弓使用 fanjinggong 模型；其余六把共用 mubangong 模型，
 * 仅贴图与伤害倍率不同。</p>
 */
public class MimiBowItem extends BowItem implements GeoItem {

    /** 射击动画持续 tick 数（0.3 秒 = 6 tick）。 */
    private static final long SHOOT_ANIM_TICKS = 6L;

    private final double damageMultiplier;
    private final String modelId;
    private final Identifier texture;
    private final Identifier animation;
    private final RawAnimation idle;
    private final RawAnimation draw;
    private final RawAnimation shoot;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /**
     * @param damageMultiplier 伤害倍率，最终伤害 = 箭基础伤害 × 该倍率
     * @param modelId          geo/动画名（mubangong 或 fanjinggong）
     * @param texturePath      3D 贴图完整路径（含 .png）
     */
    public MimiBowItem(double damageMultiplier, String modelId, String texturePath, Properties properties) {
        super(properties);
        this.damageMultiplier = damageMultiplier;
        this.modelId = modelId;
        this.texture = Identifier.parse(texturePath);
        this.animation = Identifier.fromNamespaceAndPath(texture.getNamespace(), modelId);
        this.idle = RawAnimation.begin().thenLoop("animation." + modelId + ".idle");
        this.draw = RawAnimation.begin().thenPlay("animation." + modelId + ".draw");
        this.shoot = RawAnimation.begin().thenPlay("animation." + modelId + ".shoot");
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index,
                                   float velocity, float inaccuracy, float angle, LivingEntity target) {
        if (projectile instanceof MimiArrowEntity arrow) {
            arrow.setBowDamageMultiplier(this.damageMultiplier);
        }
        super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, angle, target);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<?> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new MimiBowRenderer(new MimiBowModel(
                            Identifier.fromNamespaceAndPath(MimiWorld.MODID, modelId), texture, animation));
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        DrawState drawState = new DrawState();
        controllers.add(new AnimationController<MimiBowItem>("bow", 2, state -> {
            boolean drawing = state.renderState().getOrDefaultGeckolibData(MimiBowRenderer.DRAWING, false);
            ClientLevel level = Minecraft.getInstance().level;
            long now = level == null ? 0L : level.getGameTime();

            if (drawing) {
                drawState.drawing = true;
                drawState.lastSeenTick = now;
                return state.setAndContinue(draw);
            }
            if (drawState.drawing && now - drawState.lastSeenTick <= 2L) {
                drawState.drawing = false;
                drawState.releaseTick = now;
            }
            if (now - drawState.releaseTick < SHOOT_ANIM_TICKS) {
                return state.setAndContinue(shoot);
            }
            return state.setAndContinue(idle);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** 每个 stack 一份的拉弓/松弓跟踪状态。 */
    private static final class DrawState {
        private boolean drawing;
        private long lastSeenTick;
        private long releaseTick = -1000L;
    }
}

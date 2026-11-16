package com.mimiworld.mimiworld.client.renderer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoBlockRenderer;
import com.mimiworld.mimiworld.client.model.CookingPotGeoModel;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 煮锅系列方块的 GeckoLib 渲染器（大煮锅 / 巨无霸 / 铁锅共用一个泛型类）。
 *
 * <p>GeckoLib 5 在 {@code preRenderPass} 中先把模型平移到方块中心 (0.5, 0, 0.5)，
 * 再在 {@code scaleModelForRender} 中按 {@code withScale} 缩放，
 * 因此模型中心轴需建在 x/z = 0、底面在 y = 0，缩放后自动居中贴地，无需额外补偿。
 * 带 FACING 状态的方块（铁锅）由 GeckoLib 自动按朝向旋转。</p>
 */
public class CookingPotGeoBlockRenderer<T extends BlockEntity & GeoAnimatable>
        extends GeoBlockRenderer<T, BlockEntityRenderState> {

    public CookingPotGeoBlockRenderer(BlockEntityRendererProvider.Context context,
                                      Identifier assetSubpath, float scale) {
        super(context, new CookingPotGeoModel<T>(assetSubpath));
        withScale(scale);
    }
}

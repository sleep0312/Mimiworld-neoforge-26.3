package com.mimiworld.mimiworld.block;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 使用 GeckoLib geo 模型渲染的网格煮锅基类。
 *
 * <p>大煮锅、巨无霸、铁锅继承本类；烤箱继续使用 {@link GridCookingPotBlock}（普通 MODEL 渲染）。</p>
 */
public abstract class GeckoCookingPotBlock extends GridCookingPotBlock {

    protected GeckoCookingPotBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // 世界中的外观完全由 GeckoLib BlockEntityRenderer 渲染，不渲染方块自身的 JSON 模型
        // （26.3 只保留 INVISIBLE / MODEL 两种形状，BER 渲染的方块使用 INVISIBLE）
        return RenderShape.INVISIBLE;
    }
}

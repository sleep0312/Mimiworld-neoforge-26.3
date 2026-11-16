package com.mimiworld.mimiworld.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 使用 GeckoLib geo 模型渲染的网格煮锅方块实体中间基类（大煮锅 / 巨无霸 / 铁锅）。
 *
 * <p>本系列煮锅无动画，geo 模型以静态姿态渲染。</p>
 */
public abstract class GeckoCookingPotBlockEntity extends GridCookingPotBlockEntity
        implements GeoBlockEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    protected GeckoCookingPotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                         int gridSize) {
        super(type, pos, state, gridSize);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 不注册任何动画控制器：geo 模型保持静态
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}

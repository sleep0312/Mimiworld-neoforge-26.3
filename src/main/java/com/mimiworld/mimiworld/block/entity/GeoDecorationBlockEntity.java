package com.mimiworld.mimiworld.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 纯装饰 GeckoLib 方块实体（7 种 Boss 头颅 + 堕落十字架共用）。
 *
 * <p>不注册任何动画控制器，geo 模型以静态姿态渲染，无 GUI、不存数据、不 tick。</p>
 */
public class GeoDecorationBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    public GeoDecorationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 不注册任何动画控制器：模型保持静态
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}

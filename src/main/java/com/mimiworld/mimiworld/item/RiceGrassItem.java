package com.mimiworld.mimiworld.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 米草物品：既是可放置的植物方块物品（BlockItem），又是可食用物品。
 *
 * <p>BlockItem 的 use() 默认只尝试放置方块，即使配置了食物组件也不会进食，
 * 因此这里手动补回进食分支：饥饿时右键 = 吃；否则走 BlockItem 原逻辑 = 放草。
 * 游戏先处理 useOn（点中方块表面）再处理 use，所以对着可种植位置右键仍然优先种草。</p>
 */
public class RiceGrassItem extends BlockItem {
    public RiceGrassItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // canEat(false)：生存模式只有饥饿条不满才能吃，创造模式随时可吃
        if (stack.has(DataComponents.FOOD) && player.canEat(false)) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return super.use(level, player, hand);
    }
}

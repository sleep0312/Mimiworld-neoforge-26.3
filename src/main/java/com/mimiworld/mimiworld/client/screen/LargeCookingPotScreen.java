package com.mimiworld.mimiworld.client.screen;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.menu.GridCookingPotMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 大煮锅 GUI：3x3 网格 + 右上输入 + 燃料 + 输出（176x196），三横箭头进度。
 */
public class LargeCookingPotScreen extends GridCookingPotScreen {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MimiWorld.MODID, "textures/gui/large_cooking_pot.png");

    public LargeCookingPotScreen(GridCookingPotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title,
                TEXTURE, 196, 102,
                28, 74,
                88, 22, 24);
    }
}

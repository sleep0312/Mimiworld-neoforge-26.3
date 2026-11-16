package com.mimiworld.mimiworld.client;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * 猩红竖眼的「自定义」按键：默认 V，可在「选项 → 控制 → 按键绑定 → 米米世界」中修改。
 */
@EventBusSubscriber(modid = MimiWorld.MODID, value = Dist.CLIENT)
public final class CrimsonEyeKeybindings {

    /** 按键分类：米米世界。 */
    @SuppressWarnings("deprecation") // 26.3 仍以此方法注册自定义按键分类，暂无替代
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MimiWorld.MODID, "main"));

    public static final KeyMapping TELEPORT_TO_GLOWING = new KeyMapping(
            "key.mimiworld.crimson_vertical_eye",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_V,
            CATEGORY);

    private CrimsonEyeKeybindings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TELEPORT_TO_GLOWING);
    }
}

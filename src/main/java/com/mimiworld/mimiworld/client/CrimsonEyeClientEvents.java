package com.mimiworld.mimiworld.client;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.network.CrimsonEyeTeleportPacket;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 客户端按键监听：按下「自定义」键后发送空包到服务端，
 * 由服务端重新校验饰品与目标并执行传送。
 */
@EventBusSubscriber(modid = MimiWorld.MODID, value = Dist.CLIENT)
public final class CrimsonEyeClientEvents {

    private CrimsonEyeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (CrimsonEyeKeybindings.TELEPORT_TO_GLOWING.consumeClick()) {
            ClientPacketDistributor.sendToServer(new CrimsonEyeTeleportPacket());
        }
    }
}

package com.mimiworld.mimiworld.network;

import com.mimiworld.mimiworld.event.CrimsonEyeEvents;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 本模组的网络通道。目前仅承载「猩红竖眼」的按键请求：
 * 客户端按键 → 服务端执行传送校验与位移。
 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    /** 注册全部消息，需在模组构造期间调用。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(CrimsonEyeTeleportPacket.TYPE, CrimsonEyeTeleportPacket.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        CrimsonEyeEvents.teleportToGlowingMonster(player);
                    }
                });
    }
}

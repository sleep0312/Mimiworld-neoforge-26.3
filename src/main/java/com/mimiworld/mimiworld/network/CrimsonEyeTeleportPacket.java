package com.mimiworld.mimiworld.network;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 猩红竖眼按键请求（C2S）：无负载，收到后由服务端重新校验饰品与目标再执行传送，
 * 避免客户端伪造坐标。
 */
public record CrimsonEyeTeleportPacket() implements CustomPacketPayload {

    public static final Type<CrimsonEyeTeleportPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MimiWorld.MODID, "crimson_eye_teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrimsonEyeTeleportPacket> STREAM_CODEC =
            StreamCodec.unit(new CrimsonEyeTeleportPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

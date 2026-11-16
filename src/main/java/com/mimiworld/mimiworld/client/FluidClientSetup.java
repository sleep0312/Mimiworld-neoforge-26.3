package com.mimiworld.mimiworld.client;

import com.mimiworld.mimiworld.MimiWorld;
import com.mimiworld.mimiworld.fluid.ModFluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

/**
 * 米汤/饭汤流体渲染：26.3 的流体贴图通过 {@link RegisterFluidModelsEvent} 注册。
 * 贴图位于 textures/block/rice_soup_still.png、rice_soup_flow.png 等。
 */
@EventBusSubscriber(modid = MimiWorld.MODID, value = Dist.CLIENT)
public final class FluidClientSetup {

    private FluidClientSetup() {
    }

    private static final int TINT = 0x80F5F5F5;

    @SubscribeEvent
    static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(
                        new Material(id("block/rice_soup_still")),
                        new Material(id("block/rice_soup_flow")),
                        null,
                        (FluidTintSource) state -> TINT),
                ModFluids.RICE_SOUP, ModFluids.FLOWING_RICE_SOUP);
        event.register(new FluidModel.Unbaked(
                        new Material(id("block/rice_broth_still")),
                        new Material(id("block/rice_broth_flow")),
                        null,
                        (FluidTintSource) state -> TINT),
                ModFluids.RICE_BROTH, ModFluids.FLOWING_RICE_BROTH);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MimiWorld.MODID, path);
    }
}

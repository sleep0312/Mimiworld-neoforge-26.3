package com.mimiworld.mimiworld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.mimiworld.mimiworld.client.screen.CrusherScreen;
import com.mimiworld.mimiworld.client.screen.IngotCondensationTableScreen;
import com.mimiworld.mimiworld.client.screen.IronWokScreen;
import com.mimiworld.mimiworld.client.screen.JumboMegaCookingPotScreen;
import com.mimiworld.mimiworld.client.screen.LargeCookingPotScreen;
import com.mimiworld.mimiworld.client.screen.MixerScreen;
import com.mimiworld.mimiworld.client.screen.OvenScreen;
import com.mimiworld.mimiworld.client.screen.PrecipitationTableScreen;
import com.mimiworld.mimiworld.client.screen.RiceMakerScreen;
import com.mimiworld.mimiworld.client.screen.RicePoundingTableScreen;
import com.mimiworld.mimiworld.block.entity.LargeCookingPotBlockEntity;
import com.mimiworld.mimiworld.block.entity.JumboMegaCookingPotBlockEntity;
import com.mimiworld.mimiworld.block.entity.ModBlockEntities;
import com.mimiworld.mimiworld.client.renderer.CookingPotGeoBlockRenderer;
import com.mimiworld.mimiworld.client.renderer.GeoDecorationBlockRenderer;
import com.mimiworld.mimiworld.client.renderer.RiceMobGeoRenderer;
import com.mimiworld.mimiworld.entity.ModEntities;
import com.mimiworld.mimiworld.menu.ModMenuTypes;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MimiWorld.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = MimiWorld.MODID, value = Dist.CLIENT)
public class MimiWorldClient {
    public MimiWorldClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        MimiWorld.LOGGER.info("HELLO FROM CLIENT SETUP");
        MimiWorld.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MIMI_ARROW.get(), TippableArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.RICE_BALL.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.COOKED_RICE_BALL.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_POISON_SPIT.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_MONKEY_STAFF.get(),
                com.mimiworld.mimiworld.client.renderer.ThrownMonkeyStaffRenderer::new);
        event.registerEntityRenderer(ModEntities.DRAGON_BEAM.get(),
                com.mimiworld.mimiworld.client.renderer.DragonBeamRenderer::new);

        // 友好生物（第 4 步 · 第一小批）：通用 GeckoLib 渲染器，按实体 ID 找资源
        event.registerEntityRenderer(ModEntities.MIMI_SHEEP.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "mimi_sheep", 0.7F));
        event.registerEntityRenderer(ModEntities.ZHUOMI_CHICKEN.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "zhuomi_chicken", 0.3F));
        event.registerEntityRenderer(ModEntities.MIMOUNIU.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "mimouniu", 0.7F));
        event.registerEntityRenderer(ModEntities.MIHENG_PIG.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "mihengzhu", 0.7F));
        event.registerEntityRenderer(ModEntities.TANMI_FISH.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "tanmiyu", 0.3F));
        event.registerEntityRenderer(ModEntities.PENMI_FISH.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "penmiyu", 0.3F));
        event.registerEntityRenderer(ModEntities.RICE_GOLEM.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "rice_golem", 1.0F));

        // 近战亡灵（第 4 步 · 第二小批）：模型资源名与实体 ID 不一定一致，按模型名传参
        event.registerEntityRenderer(ModEntities.MIMI_SKELETON.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "mimi_skeleton", 0.6F));
        event.registerEntityRenderer(ModEntities.CARVED_RICE_SKELETON.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "diaomikulou", 0.7F));
        event.registerEntityRenderer(ModEntities.HUMI_CORPSE.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "humi_corpse", 0.7F));
        event.registerEntityRenderer(ModEntities.MIHENG_PIG_CORPSE.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "mihengzhushi", 0.7F));

        // 特殊行为敌对（第 4 步 · 第三小批）
        event.registerEntityRenderer(ModEntities.HAOMI_WOLF.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "haomi_wolf", 0.5F));
        event.registerEntityRenderer(ModEntities.PAMI_CRAWLER.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "pami_crawler", 0.4F));
        event.registerEntityRenderer(ModEntities.STICKY_RICE_SPIDER.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "spider", 0.9F));
        event.registerEntityRenderer(ModEntities.RICE_WITCH.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "rice_witch", 0.5F));
        event.registerEntityRenderer(ModEntities.MILAI_HORSE.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "milaima", 0.6F));

        // 地面精英（第 4 步 · 第四小批）
        event.registerEntityRenderer(ModEntities.RICE_STEALER.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "daomizhe", 0.6F));
        event.registerEntityRenderer(ModEntities.BLIGHTED_THORN_FLOWER.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "wujihua", 0.6F));
        event.registerEntityRenderer(ModEntities.RICE_MIRAGE.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "mishen", 0.7F));
        event.registerEntityRenderer(ModEntities.LAZY_ELF.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "lanjingling", 0.4F));

        // 飞行精英（第 4 步 · 第五小批）
        event.registerEntityRenderer(ModEntities.RICE_SOUL.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "mihun", 0.6F));
        event.registerEntityRenderer(ModEntities.HUALING_RICE.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "hualingmi", 0.6F));

        // 飞行/陆空怪（第 4 步 · 第六小批）
        event.registerEntityRenderer(ModEntities.EXPLOSIVE_RICE_SPIRIT.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "baoliemiling", 0.5F));
        event.registerEntityRenderer(ModEntities.RICE_WOLF_VULTURE.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "rice_wolf_vulture", 0.8F));

        // ===== Boss 与残堕界精英 =====
        event.registerEntityRenderer(ModEntities.MIMI_MONKEY_KING.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "mimihouwang", 1.0F));
        event.registerEntityRenderer(ModEntities.RICE_CRYSTAL_TIGER_KING.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "fanjinghuwang", 1.3F));
        event.registerEntityRenderer(ModEntities.SUPER_CRYSTAL_COW_KING.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "super_crystal_cow_king", 1.4F));
        event.registerEntityRenderer(ModEntities.HEAVY_CRYSTAL_RABBIT_KING.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "heavy_crystal_rabbit_king", 1.3F));
        event.registerEntityRenderer(ModEntities.COMPOUND_AZURE_PYTHON.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "compound_azure_python", 1.6F));
        event.registerEntityRenderer(ModEntities.CLEANSING_LONGEVITY_KUI.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "qingjiechangmingkui", 1.6F));
        event.registerEntityRenderer(ModEntities.LAZIEST_FINAL_DRAGON.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "laziest_final_dragon", 2.0F));
        event.registerEntityRenderer(ModEntities.DIBAZONGZUILONG.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "dibazongzuilong", 2.4F));
        event.registerEntityRenderer(ModEntities.FALLEN_RICE_PHOENIX.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "fallen_rice_phoenix", 1.0F));
        event.registerEntityRenderer(ModEntities.JIETIAN_RICE.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "jietianmi", 0.8F));
        event.registerEntityRenderer(ModEntities.CHAOTIC_VOID_TOUCHER.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "hundunbukemingzhuangzhimijiexuwuchu", 1.2F));
        event.registerEntityRenderer(ModEntities.MOMI_ENTRANT.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "meimiru", 0.5F));
        event.registerEntityRenderer(ModEntities.PALE_MERMAID.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "qianranjiao", 1.2F));
        event.registerEntityRenderer(ModEntities.WANGMI_WOUNDED_MERMAID.get(),
                ctx -> new RiceMobGeoRenderer<>(ctx, "wangmishangjiao", 1.6F));

        // 煮锅系列：GeckoLib geo 模型（无动画，静态渲染）。
        // 模型按 12.8 / 21.632 / 36.608 px 基准等比缩放，收进一格并贴地居中。
        event.registerBlockEntityRenderer(ModBlockEntities.LARGE_COOKING_POT.get(),
                ctx -> new CookingPotGeoBlockRenderer<LargeCookingPotBlockEntity>(ctx,
                        Identifier.fromNamespaceAndPath(MimiWorld.MODID, "large_cooking_pot"),
                        10.5F / 21.632F));
        event.registerBlockEntityRenderer(ModBlockEntities.JUMBO_MEGA_COOKING_POT.get(),
                ctx -> new CookingPotGeoBlockRenderer<JumboMegaCookingPotBlockEntity>(ctx,
                        Identifier.fromNamespaceAndPath(MimiWorld.MODID, "jumbo_mega_cooking_pot"),
                        16.0F / 36.608F));
        // 铁锅：平底锅 geo 模型（模型本身按 16px 内设计，1:1 渲染）
        event.registerBlockEntityRenderer(ModBlockEntities.IRON_WOK.get(),
                ctx -> new CookingPotGeoBlockRenderer<com.mimiworld.mimiworld.block.entity.IronWokBlockEntity>(ctx,
                        Identifier.fromNamespaceAndPath(MimiWorld.MODID, "iron_wok"),
                        1.0F));
        // 米米传送器：插眼后悬浮旋转的米米之眼
        event.registerBlockEntityRenderer(ModBlockEntities.MIMI_TELEPORTER.get(),
                com.mimiworld.mimiworld.client.renderer.MimiTeleporterBlockRenderer::new);

        // Boss 头颅与堕落十字架：静态 geo 模型，缩放 + 旧渲染器平移换算出的世界偏移
        registerDecorationRenderer(event, ModBlockEntities.MIMI_MONKEY_HEAD.get(), "mimi_monkey_head",
                32.0F / 30.8F, 0.0F, -19.9F / 16.0F, 0.925F / 16.0F);
        registerDecorationRenderer(event, ModBlockEntities.RICE_CRYSTAL_TIGER_KING_HEAD.get(),
                "rice_crystal_tiger_king_head", 1.12F, 0.0F, -27.0F / 16.0F, 3.0F / 16.0F);
        registerDecorationRenderer(event, ModBlockEntities.SUPER_CRYSTAL_OX_HEAD.get(), "super_crystal_ox_head",
                1.44F, 0.0F, -24.0F / 16.0F, 5.0F / 16.0F);
        registerDecorationRenderer(event, ModBlockEntities.HEAVY_CRYSTAL_RABBIT_KING_HEAD.get(),
                "heavy_crystal_rabbit_king_head", 0.90F, 0.0F, -29.0F / 16.0F, 10.75F / 16.0F);
        registerDecorationRenderer(event, ModBlockEntities.COMPOUND_CRYSTAL_AZURE_PYTHON_HEAD.get(),
                "compound_crystal_azure_python_head", 0.92F, 0.0F, -34.0F / 16.0F, 26.5F / 16.0F);
        registerDecorationRenderer(event, ModBlockEntities.PURIFIED_LONGEVITY_KUI_HEAD.get(),
                "purified_longevity_kui_head", 0.94F, 0.0F, -34.0F / 16.0F, 25.0F / 16.0F);
        registerDecorationRenderer(event, ModBlockEntities.LAZIEST_END_DRAGON_HEAD.get(),
                "laziest_end_dragon_head", 0.32F, 0.0F, -82.0F / 16.0F, 184.0F / 16.0F);
        registerDecorationRenderer(event, ModBlockEntities.DIBAZONGZUI_HEAD.get(), "dibazongzui_head",
                32.0F / 150.0F, 0.0F, -106.8F / 16.0F, 276.0F / 16.0F);
        // 堕落十字架：模型为方块坐标系（中心轴 x=z=8），旧渲染器平移 0.5 - s
        registerDecorationRenderer(event, ModBlockEntities.FALLEN_CROSS.get(), "fallen_cross",
                16.0F / 27.0F, -0.5F, 0.0F, -0.5F);
    }

    /**
     * 注册纯装饰 GeckoLib 方块渲染器。
     *
     * <p>参数以「模型基准值」传入，统一乘以缩放换算成世界坐标补偿：
     * dx = (TX + 0.5·s - 0.5) / s（头颅为 0，十字架为 -0.5），dy/dz 为模型像素偏移 / 16。</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerDecorationRenderer(EntityRenderersEvent.RegisterRenderers event,
                                                   net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                                   String id, float scale,
                                                   float dxFactor, float dyPixels, float dzPixels) {
        event.registerBlockEntityRenderer((net.minecraft.world.level.block.entity.BlockEntityType) type,
                ctx -> new GeoDecorationBlockRenderer(
                        ctx, Identifier.fromNamespaceAndPath(MimiWorld.MODID, id),
                        scale, dxFactor * scale, dyPixels * scale, dzPixels * scale));
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CRUSHER_MENU.get(), CrusherScreen::new);
        event.register(ModMenuTypes.MIXER_MENU.get(), MixerScreen::new);
        event.register(ModMenuTypes.RICE_MAKER_MENU.get(), RiceMakerScreen::new);
        event.register(ModMenuTypes.RICE_POUNDING_TABLE_MENU.get(), RicePoundingTableScreen::new);
        event.register(ModMenuTypes.PRECIPITATION_TABLE_MENU.get(), PrecipitationTableScreen::new);
        event.register(ModMenuTypes.INGOT_CONDENSATION_TABLE_MENU.get(), IngotCondensationTableScreen::new);
        event.register(ModMenuTypes.LARGE_COOKING_POT_MENU.get(), LargeCookingPotScreen::new);
        event.register(ModMenuTypes.JUMBO_MEGA_COOKING_POT_MENU.get(), JumboMegaCookingPotScreen::new);
        event.register(ModMenuTypes.IRON_WOK_MENU.get(), IronWokScreen::new);
        event.register(ModMenuTypes.OVEN_MENU.get(), OvenScreen::new);
    }
}

package com.mimiworld.mimiworld;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mimiworld.mimiworld.block.ModBlocks;
import com.mimiworld.mimiworld.block.entity.ModBlockEntities;
import com.mimiworld.mimiworld.datagen.DataGenerators;
import com.mimiworld.mimiworld.effect.ModEffects;
import com.mimiworld.mimiworld.entity.ModEntities;
import com.mimiworld.mimiworld.entity.ModEntityAttributes;
import com.mimiworld.mimiworld.entity.ModSpawnPlacements;
import com.mimiworld.mimiworld.event.ArmorEffectEvents;
import com.mimiworld.mimiworld.event.CreativeFlightEvents;
import com.mimiworld.mimiworld.event.CurioSlotSizeEvents;
import com.mimiworld.mimiworld.event.CrimsonEyeEvents;
import com.mimiworld.mimiworld.event.DamagePipeline;
import com.mimiworld.mimiworld.event.DomainExpansionEvents;
import com.mimiworld.mimiworld.event.MimiTotemEvents;
import com.mimiworld.mimiworld.event.ModConfigEvents;
import com.mimiworld.mimiworld.event.ModEffectEvents;
import com.mimiworld.mimiworld.fluid.ModFluids;
import com.mimiworld.mimiworld.menu.ModMenuTypes;
import com.mimiworld.mimiworld.network.ModNetwork;
import com.mimiworld.mimiworld.sound.ModSounds;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MimiWorld.MODID)
public class MimiWorld {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mimiworld";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MimiWorld(IEventBus modEventBus, ModContainer modContainer) {
        // 注册 commonSetup 方法用于 mod 加载
        modEventBus.addListener(this::commonSetup);

        // 数值配置文件：config/mimiworld-common.toml。
        // MUST 先于物品注册：STARTUP 配置在注册时立即读盘，防具/工具材料构造时会读取其中的数值。
        ModConfig.register(modContainer);

        // 方块（含其方块物品，必须先于物品注册）
        ModBlocks.register(modEventBus);
        // 方块实体类型（依赖方块注册）
        ModBlockEntities.register(modEventBus);
        // 物品与创造模式标签页
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        // 容器菜单类型
        ModMenuTypes.register(modEventBus);
        // 药水效果
        ModEffects.register(modEventBus);
        // 流体（米汤/饭汤）与米汤炼药锅
        ModFluids.register(modEventBus);
        modEventBus.addListener(ModFluids::registerCauldronFluidContent);
        // 自定义音效（8 Boss + 米魂）
        ModSounds.register(modEventBus);
        // 实体类型
        ModEntities.register(modEventBus);
        // 实体属性（NeoForge 模组总线事件）
        modEventBus.addListener(ModEntityAttributes::onEntityAttributeCreation);
        // 实体自然生成规则
        modEventBus.addListener(ModSpawnPlacements::onRegisterSpawnPlacements);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (MimiWorld) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(CreativeFlightEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ArmorEffectEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(MimiTotemEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.mimiworld.mimiworld.event.MimiWeatherHandler::onLevelTick);

        // 战斗伤害管线 / 配置运行时接线 / Curios 栏位调整
        NeoForge.EVENT_BUS.register(DamagePipeline.class);
        NeoForge.EVENT_BUS.register(ModConfigEvents.class);
        NeoForge.EVENT_BUS.register(CurioSlotSizeEvents.class);
        NeoForge.EVENT_BUS.register(ModEffectEvents.class);
        NeoForge.EVENT_BUS.register(DomainExpansionEvents.class);
        NeoForge.EVENT_BUS.register(CrimsonEyeEvents.class);

        // 网络通道（猩红竖眼按键请求）
        modEventBus.addListener(ModNetwork::register);

        // 数据生成（runData 专用；仅 --mod 指定本模时触发）
        modEventBus.addListener(DataGenerators::gatherRegistryEntries);
        modEventBus.addListener(net.neoforged.neoforge.data.event.GatherDataEvent.Client.class,
                DataGenerators::gatherData);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        // 机器方块配方（放入 enqueueWork：ItemStack 需要数据组件绑定完成后才能创建）
        event.enqueueWork(ModMachineRecipes::register);
        // 米桶/饭桶 ↔ 空炼药锅交互（需等物品/方块注册表填充完成）
        event.enqueueWork(ModFluids::registerCauldronInteractions);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}

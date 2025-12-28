package org.xiyu.healthygamer;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.xiyu.healthygamer.init.ItemInit;

// 核心注解：告诉 Forge 这是个模组
@Mod(HealthyGamerMod.MODID)
public class HealthyGamerMod {
    public static final String MODID = "healthy_gamer";
    private static final Logger LOGGER = LogUtils.getLogger();

    public HealthyGamerMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ✅ 新增：注册物品
        ItemInit.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("防沉迷系统核心已启动... 正在连接公安部数据库(伪)...");
    }

    // 服务端事件：当服务器启动时
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("服务器已启动，宵禁逻辑准备就绪。");
    }

    // 客户端专用事件总线 (处理 GUI 和按键拦截)
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("客户端防沉迷模块初始化...");
            // 这里以后注册你的 GUI 覆盖层 (HUD)
        }
        @SubscribeEvent
        public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
            // 注册名为 "time_limit_hud" 的覆盖层
            event.registerAboveAll("time_limit_hud", new org.xiyu.healthygamer.client.gui.TimeLimitHud());
        }
    }
}
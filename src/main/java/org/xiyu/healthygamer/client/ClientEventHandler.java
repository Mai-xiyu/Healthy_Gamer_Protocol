package org.xiyu.healthygamer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xiyu.healthygamer.HealthyGamerMod;
import org.xiyu.healthygamer.client.gui.AuthScreen;
import org.xiyu.healthygamer.client.gui.FaceVerifyScreen;
import org.xiyu.healthygamer.client.gui.TimeLimitHud;
import org.xiyu.healthygamer.client.gui.TimeUpScreen;

import java.time.LocalTime;
import java.util.Random;

@Mod.EventBusSubscriber(modid = HealthyGamerMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    public static final long MAX_PLAY_TIME = 60 * 60 * 1000;

    private static final Random random = new Random();
    private static long lastTickTime = 0;
    private static int saveTicker = 0;

    private static int clickCounter = 0;
    private static float rotationDeltaSum = 0;
    private static float lastYaw = 0;
    private static float lastPitch = 0;
    private static long monitorStartTime = 0;

    public static void generateNextCheckTime() {
        long minDelay = 10 * 60 * 1000;
        long maxDelay = 30 * 60 * 1000;
        long delay = minDelay + random.nextInt((int)(maxDelay - minDelay));

        ClientData.INSTANCE.nextFaceCheckTime = System.currentTimeMillis() + delay;
        ClientData.save();
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("time_limit_hud", new TimeLimitHud());
    }

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientData.load();
        lastTickTime = System.currentTimeMillis();
        clickCounter = 0;
        rotationDeltaSum = 0;
        monitorStartTime = System.currentTimeMillis();
        if (Minecraft.getInstance().player != null) {
            lastYaw = Minecraft.getInstance().player.getYRot();
            lastPitch = Minecraft.getInstance().player.getXRot();
        }

        if (ClientData.INSTANCE.isUsingFakeId) {
            long now = System.currentTimeMillis();
            if (ClientData.INSTANCE.nextFaceCheckTime == 0) {
                generateNextCheckTime();
            }
            else if (now > ClientData.INSTANCE.nextFaceCheckTime) {
                ClientData.INSTANCE.nextFaceCheckTime = now + 10000;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientData.save();
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == 1) {
            clickCounter++;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long now = System.currentTimeMillis();

        if (lastTickTime != 0 && ClientData.INSTANCE.isVerified) {
            ClientData.INSTANCE.dailyPlayedTime += (now - lastTickTime);
        }
        lastTickTime = now;

        saveTicker++;
        if (saveTicker >= 200) {
            ClientData.save();
            saveTicker = 0;
        }

        if (ClientData.INSTANCE.isUsingFakeId && mc.screen == null) {
            float currentYaw = mc.player.getYRot();
            float currentPitch = mc.player.getXRot();
            float delta = Math.abs(currentYaw - lastYaw) + Math.abs(currentPitch - lastPitch);
            rotationDeltaSum += delta;

            lastYaw = currentYaw;
            lastPitch = currentPitch;

            if (now - monitorStartTime > 5000) {
                boolean tooFastClicks = clickCounter > 35;
                boolean tooFastRotation = rotationDeltaSum > 1500;

                if (tooFastClicks || tooFastRotation) {
                    if (ClientData.INSTANCE.nextFaceCheckTime - now > 5000) {
                        mc.player.sendSystemMessage(Component.literal("§c[警告] 系统检测到您的反应速度远超 75 岁用户平均水平！"));
                        mc.player.sendSystemMessage(Component.literal("§e[大数据] 正在重新评估您的身份信息..."));
                        ClientData.INSTANCE.nextFaceCheckTime = now + 3000;
                        ClientData.save();
                    }
                }

                clickCounter = 0;
                rotationDeltaSum = 0;
                monitorStartTime = now;
            }
        }

        if (!ClientData.INSTANCE.isVerified) {
            if (!(mc.screen instanceof AuthScreen)) {
                mc.setScreen(new AuthScreen());
            }
            return;
        }

        if (!ClientData.INSTANCE.isAdult) {
            LocalTime localTime = LocalTime.now();
            boolean isCurfew = localTime.getHour() >= 22 || localTime.getHour() < 8;
            boolean isTimeUp = ClientData.INSTANCE.dailyPlayedTime > MAX_PLAY_TIME;

            if (isCurfew || isTimeUp) {
                if (!(mc.screen instanceof TimeUpScreen)) {
                    mc.setScreen(new TimeUpScreen(isCurfew ? "当前是宵禁时间" : "游戏时间已耗尽"));
                }
                return;
            }
        }

        if (ClientData.INSTANCE.isUsingFakeId) {
            if (mc.screen == null && now > ClientData.INSTANCE.nextFaceCheckTime) {
                // 安全冷却 60 秒
                ClientData.INSTANCE.nextFaceCheckTime = now + 60000;
                mc.setScreen(new FaceVerifyScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientData.INSTANCE.isVerified || Minecraft.getInstance().screen instanceof TimeUpScreen) {
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
        }
    }
}
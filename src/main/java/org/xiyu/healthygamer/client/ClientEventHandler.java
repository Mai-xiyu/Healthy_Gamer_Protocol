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

    // --- 👴 老年人行为监测变量 ---
    private static int clickCounter = 0;          // 记录点击次数
    private static float rotationDeltaSum = 0;    // 记录视角转动幅度累计
    private static float lastYaw = 0;
    private static float lastPitch = 0;
    private static long monitorStartTime = 0;     // 监测周期开始时间

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

        // 重置监测数据
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

    // 🔥 新增：监听鼠标点击 (计算手速)
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        // action 1 = 按下, button 0 = 左键, 1 = 右键
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

        // 1. 时间累加
        if (lastTickTime != 0 && ClientData.INSTANCE.isVerified) {
            ClientData.INSTANCE.dailyPlayedTime += (now - lastTickTime);
        }
        lastTickTime = now;

        // 2. 自动保存
        saveTicker++;
        if (saveTicker >= 200) {
            ClientData.save();
            saveTicker = 0;
        }

        // 3. --- 👴 核心逻辑：操作强度检测 ---
        if (ClientData.INSTANCE.isUsingFakeId && mc.screen == null) {
            // 计算视角的瞬间变化量 (简单防抖)
            float currentYaw = mc.player.getYRot();
            float currentPitch = mc.player.getXRot();
            float delta = Math.abs(currentYaw - lastYaw) + Math.abs(currentPitch - lastPitch);
            rotationDeltaSum += delta;

            lastYaw = currentYaw;
            lastPitch = currentPitch;

            // 每 5 秒结算一次
            if (now - monitorStartTime > 5000) {
                // 判定标准：
                // 1. 5秒内点击超过 35 次 (平均 7 CPS) -> 只有年轻人手速这么快
                // 2. 5秒内视角转动累计超过 1500 度 -> 疯狂甩头/转圈
                boolean tooFastClicks = clickCounter > 35;
                boolean tooFastRotation = rotationDeltaSum > 1500;

                if (tooFastClicks || tooFastRotation) {
                    // 只有当下次检查时间还很远(>5秒)的时候，才触发惩罚
                    if (ClientData.INSTANCE.nextFaceCheckTime - now > 5000) {
                        mc.player.sendSystemMessage(Component.literal("§c[警告] 系统检测到您的反应速度远超 75 岁用户平均水平！"));
                        mc.player.sendSystemMessage(Component.literal("§e[大数据] 正在重新评估您的身份信息..."));

                        // 惩罚：3秒后立刻触发人脸识别
                        ClientData.INSTANCE.nextFaceCheckTime = now + 3000;
                        ClientData.save();
                    }
                }

                // 重置计数器进入下一个周期
                clickCounter = 0;
                rotationDeltaSum = 0;
                monitorStartTime = now;
            }
        }

        // --- 阶段一：实名认证 ---
        if (!ClientData.INSTANCE.isVerified) {
            if (!(mc.screen instanceof AuthScreen)) {
                mc.setScreen(new AuthScreen());
            }
            return;
        }

        // --- 阶段二：防沉迷 (未成年) ---
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

        // --- 阶段三：人脸识别抽查 ---
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
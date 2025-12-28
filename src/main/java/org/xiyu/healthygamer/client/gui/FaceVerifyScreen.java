package org.xiyu.healthygamer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth; // 用于角度计算
import org.joml.Quaternionf;

import java.util.Random;

public class FaceVerifyScreen extends Screen {

    private final long startTime;
    private final long TIME_LIMIT = 5000; // 5秒限时

    // 鼠标控制变量
    private double lastMouseX;
    private double lastMouseY;
    private boolean isFirstFrame = true;

    // 动作检测变量
    private final boolean isNodAction; // true=点头(Pitch), false=摇头(Yaw)
    private float startPitch;
    private float startYaw;

    // 状态标记
    private boolean hasPositiveMove = false; // 抬头 或 向左
    private boolean hasNegativeMove = false; // 低头 或 向右

    public FaceVerifyScreen() {
        super(Component.literal("人脸识别抽查"));
        this.startTime = System.currentTimeMillis();

        // 🔥 随机决定本次动作：50%概率点头，50%概率摇头
        this.isNodAction = new Random().nextBoolean();

        if (Minecraft.getInstance().player != null) {
            this.startPitch = Minecraft.getInstance().player.getXRot();
            this.startYaw = Minecraft.getInstance().player.getYRot();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isFirstFrame) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            isFirstFrame = false;
            return;
        }

        double deltaX = mouseX - this.lastMouseX;
        double deltaY = mouseY - this.lastMouseY;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        // 3. 获取灵敏度
        double sensitivity = 0;
        if (this.minecraft != null) {
            sensitivity = this.minecraft.options.sensitivity().get() * 0.6D + 0.2D;
        }
        double scale = sensitivity * sensitivity * sensitivity * 8.0D;

        // 🔥 修复：移除了之前的 0.15 限制，恢复 1:1 的手感，现在会非常灵敏！
        double finalScale = scale;

        // 4. 手动旋转玩家视角
        if (this.minecraft.player != null) {
            this.minecraft.player.turn(deltaX * finalScale, deltaY * finalScale);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.minecraft == null || this.minecraft.player == null) return;

        // 1. 倒计时检查
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > TIME_LIMIT) {
            if (this.minecraft.getConnection() != null) {
                this.minecraft.getConnection().getConnection().disconnect(
                        Component.literal("§c[人脸识别失败] 检测到非活体操作，已强制下线！")
                );
            }
            return;
        }

        // 2. 动作检测算法
        if (isNodAction) {
            // === 检测点头 (Pitch) ===
            float currentPitch = this.minecraft.player.getXRot();
            float delta = currentPitch - startPitch;

            // 阈值设为 15 度 (因为灵敏度修复了，这个幅度很轻松就能达到)
            if (delta < -15) hasPositiveMove = true;  // 抬头
            if (delta > 15) hasNegativeMove = true;   // 低头

        } else {
            // === 检测摇头 (Yaw) ===
            float currentYaw = this.minecraft.player.getYRot();
            // 处理角度循环 (例如从 180 变到 -180)
            float delta = Mth.wrapDegrees(currentYaw - startYaw);

            if (delta > 15) hasPositiveMove = true;   // 向左转
            if (delta < -15) hasNegativeMove = true;  // 向右转
        }

        // 3. 验证通过
        if (hasPositiveMove && hasNegativeMove) {
            this.onClose();
            this.minecraft.player.sendSystemMessage(Component.literal("§a[系统] 活体检测通过。请继续游戏。"));
            org.xiyu.healthygamer.client.ClientEventHandler.generateNextCheckTime();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (this.minecraft != null && this.minecraft.player != null) {
            try {
                // 计算动画：正弦波
                float time = (System.currentTimeMillis() % 1500) / 1500f;
                float animFactor = (float) Math.sin(time * Math.PI * 2);

                // 备份原始旋转
                float originalBodyRot = this.minecraft.player.yBodyRot;
                float originalYRot = this.minecraft.player.getYRot();
                float originalXRot = this.minecraft.player.getXRot();
                float originalHeadRotO = this.minecraft.player.yHeadRotO;
                float originalHeadRot = this.minecraft.player.yHeadRot;

                try {
                    float lookY = 180.0F; // 身体默认朝向
                    float lookX = 0.0F;

                    // 🔥 3D 模型根据当前要求做动作示范
                    if (isNodAction) {
                        // 示范点头
                        lookX = animFactor * 20.0F;
                    } else {
                        // 示范摇头 (修改 Yaw)
                        lookY = 180.0F + animFactor * 30.0F;
                    }

                    this.minecraft.player.yBodyRot = 180.0F; // 身体不动
                    this.minecraft.player.setYRot(lookY);    // 头左右动
                    this.minecraft.player.setXRot(lookX);    // 头上下动
                    this.minecraft.player.yHeadRot = lookY;
                    this.minecraft.player.yHeadRotO = lookY;

                    Quaternionf pose = new Quaternionf().rotateZ((float)Math.PI);
                    Quaternionf camera = new Quaternionf();

                    InventoryScreen.renderEntityInInventory(
                            graphics, centerX, centerY + 20, 70, pose, camera, this.minecraft.player
                    );

                } finally {
                    // 恢复
                    this.minecraft.player.yBodyRot = originalBodyRot;
                    this.minecraft.player.setYRot(originalYRot);
                    this.minecraft.player.setXRot(originalXRot);
                    this.minecraft.player.yHeadRotO = originalHeadRotO;
                    this.minecraft.player.yHeadRot = originalHeadRot;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // 绘制倒计时
        long remaining = TIME_LIMIT - (System.currentTimeMillis() - startTime);
        String timeStr = String.format("%.1f", remaining / 1000.0f);

        int textY = centerY + 30;
        graphics.drawCenteredString(this.font, "§c§l⚠️ 系统抽查 ⚠️", centerX, textY, 0xFF0000);

        // 🔥 根据动作类型显示不同的提示语
        String actionText = isNodAction ? "请模仿动作：点头 (上下晃动鼠标)" : "请模仿动作：摇头 (左右晃动鼠标)";
        graphics.drawCenteredString(this.font, actionText, centerX, textY + 20, 0xFFFFFF);

        graphics.drawCenteredString(this.font, "剩余时间: " + timeStr + "秒", centerX, textY + 40, 0xFFFF00);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
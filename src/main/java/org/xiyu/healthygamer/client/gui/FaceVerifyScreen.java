package org.xiyu.healthygamer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.util.Random;

public class FaceVerifyScreen extends Screen {

    private final long startTime;
    private final long TIME_LIMIT = 5000;

    private double lastMouseX;
    private double lastMouseY;
    private boolean isFirstFrame = true;

    private final boolean isNodAction;
    private float startPitch;
    private float startYaw;

    private boolean hasPositiveMove = false;
    private boolean hasNegativeMove = false;

    public FaceVerifyScreen() {
        super(Component.literal("人脸识别抽查"));
        this.startTime = System.currentTimeMillis();
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

        double sensitivity = 0;
        if (this.minecraft != null) {
            sensitivity = this.minecraft.options.sensitivity().get() * 0.6D + 0.2D;
        }
        double scale = sensitivity * sensitivity * sensitivity * 8.0D;
        double finalScale = scale;

        if (this.minecraft.player != null) {
            this.minecraft.player.turn(deltaX * finalScale, deltaY * finalScale);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.minecraft == null || this.minecraft.player == null) return;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > TIME_LIMIT) {
            if (this.minecraft.getConnection() != null) {
                this.minecraft.getConnection().getConnection().disconnect(
                        Component.literal("§c[人脸识别失败] 检测到非活体操作，已强制下线！")
                );
            }
            return;
        }

        if (isNodAction) {
            float currentPitch = this.minecraft.player.getXRot();
            float delta = currentPitch - startPitch;

            if (delta < -15) hasPositiveMove = true;
            if (delta > 15) hasNegativeMove = true;

        } else {
            float currentYaw = this.minecraft.player.getYRot();
            float delta = Mth.wrapDegrees(currentYaw - startYaw);

            if (delta > 15) hasPositiveMove = true;
            if (delta < -15) hasNegativeMove = true;
        }

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
                float time = (System.currentTimeMillis() % 1500) / 1500f;
                float animFactor = (float) Math.sin(time * Math.PI * 2);

                float originalBodyRot = this.minecraft.player.yBodyRot;
                float originalYRot = this.minecraft.player.getYRot();
                float originalXRot = this.minecraft.player.getXRot();
                float originalHeadRotO = this.minecraft.player.yHeadRotO;
                float originalHeadRot = this.minecraft.player.yHeadRot;

                try {
                    float lookY = 180.0F;
                    float lookX = 0.0F;

                    if (isNodAction) {
                        lookX = animFactor * 20.0F;
                    } else {
                        lookY = 180.0F + animFactor * 30.0F;
                    }

                    this.minecraft.player.yBodyRot = 180.0F;
                    this.minecraft.player.setYRot(lookY);
                    this.minecraft.player.setXRot(lookX);
                    this.minecraft.player.yHeadRot = lookY;
                    this.minecraft.player.yHeadRotO = lookY;

                    Quaternionf pose = new Quaternionf().rotateZ((float)Math.PI);
                    Quaternionf camera = new Quaternionf();

                    InventoryScreen.renderEntityInInventory(
                            graphics, centerX, centerY + 20, 70, pose, camera, this.minecraft.player
                    );

                } finally {
                    this.minecraft.player.yBodyRot = originalBodyRot;
                    this.minecraft.player.setYRot(originalYRot);
                    this.minecraft.player.setXRot(originalXRot);
                    this.minecraft.player.yHeadRotO = originalHeadRotO;
                    this.minecraft.player.yHeadRot = originalHeadRot;
                }
            } catch (Exception e) {
            }
        }

        long remaining = TIME_LIMIT - (System.currentTimeMillis() - startTime);
        String timeStr = String.format("%.1f", remaining / 1000.0f);

        int textY = centerY + 30;
        graphics.drawCenteredString(this.font, "§c§l⚠️ 系统抽查 ⚠️", centerX, textY, 0xFF0000);

        String actionText = isNodAction ? "请模仿动作：点头 (上下晃动鼠标)" : "请模仿动作：摇头 (左右晃动鼠标)";
        graphics.drawCenteredString(this.font, actionText, centerX, textY + 20, 0xFFFFFF);

        graphics.drawCenteredString(this.font, "剩余时间: " + timeStr + "秒", centerX, textY + 40, 0xFFFF00);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
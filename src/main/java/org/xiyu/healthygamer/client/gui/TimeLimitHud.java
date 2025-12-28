package org.xiyu.healthygamer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.xiyu.healthygamer.client.ClientData;
import org.xiyu.healthygamer.client.ClientEventHandler;

import java.awt.Color;

public class TimeLimitHud implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        // 1. 如果没认证，不显示 (显示 AuthScreen)
        if (!ClientData.INSTANCE.isVerified) return;

        // 🔥 修复：如果是成年人 (包含爷爷的身份证)，也不显示倒计时
        // 成年人应该享受“自由”的感觉 (直到被人脸识别制裁)
        if (ClientData.INSTANCE.isAdult) return;

        // --- 以下仅针对未成年人显示 ---

        long remaining = ClientEventHandler.MAX_PLAY_TIME - ClientData.INSTANCE.dailyPlayedTime;

        if (remaining < 0) remaining = 0;

        // 格式化时间 HH:mm:ss
        long seconds = remaining / 1000;
        String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);

        // 颜色渐变：时间越少越红
        int color = 0x00FF00; // 绿色
        if (seconds < 300) color = 0xFFFF00; // 最后5分钟 黄色
        if (seconds < 60) color = 0xFF0000;  // 最后1分钟 红色

        String text = "§l剩余时间: " + timeStr;

        // 绘制在左上角 (x=10, y=10)
        graphics.drawString(Minecraft.getInstance().font, text, 10, 10, color, true);

        // 显示未成年人标记
        graphics.drawString(Minecraft.getInstance().font, "§7(未成年人防沉迷中)", 10, 20, 0xAAAAAA, true);
    }
}
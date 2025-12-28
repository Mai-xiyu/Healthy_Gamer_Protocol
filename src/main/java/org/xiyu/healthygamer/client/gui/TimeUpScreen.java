package org.xiyu.healthygamer.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TimeUpScreen extends Screen {

    private final String reason;

    public TimeUpScreen(String reason) {
        super(Component.literal("健康系统提示"));
        this.reason = reason;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 绘制大红字
        graphics.drawCenteredString(this.font, "§l§c⚠ 您的游戏时间已结束 ⚠", centerX, centerY - 20, 0xFF0000);
        graphics.drawCenteredString(this.font, reason, centerX, centerY + 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "为了您的身心健康，请按 ALT+F4 强制休息", centerX, centerY + 40, 0xAAAAAA);
    }

    // 🔥 禁止一切操作
    @Override
    public boolean shouldCloseOnEsc() { return false; }
    @Override
    public boolean isPauseScreen() { return true; } // 暂停游戏，防止被打死
}
package org.xiyu.healthygamer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.xiyu.healthygamer.client.ClientData;
import org.xiyu.healthygamer.client.ClientEventHandler;

public class TimeLimitHud implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!ClientData.INSTANCE.isVerified) return;

        if (ClientData.INSTANCE.isAdult) return;

        long remaining = ClientEventHandler.MAX_PLAY_TIME - ClientData.INSTANCE.dailyPlayedTime;

        if (remaining < 0) remaining = 0;

        long seconds = remaining / 1000;
        String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);

        int color = 0x00FF00;
        if (seconds < 300) color = 0xFFFF00;
        if (seconds < 60) color = 0xFF0000;

        String text = "§l剩余时间: " + timeStr;

        graphics.drawString(Minecraft.getInstance().font, text, 10, 10, color, true);

        graphics.drawString(Minecraft.getInstance().font, "§7(未成年人防沉迷中)", 10, 20, 0xAAAAAA, true);
    }
}
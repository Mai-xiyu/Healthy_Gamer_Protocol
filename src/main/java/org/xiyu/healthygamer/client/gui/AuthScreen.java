package org.xiyu.healthygamer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.xiyu.healthygamer.client.ClientData;
import org.xiyu.healthygamer.init.ItemInit;
import org.xiyu.healthygamer.utils.IDCardUtils;
import org.xiyu.healthygamer.utils.IpUtils;

public class AuthScreen extends Screen {

    private EditBox nameInput;
    private EditBox idInput;
    private Button verifyButton;
    private String errorMessage = null;
    private boolean isChecking = false;

    public AuthScreen() {
        super(Component.literal("实名认证系统"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 1. 姓名输入框
        // 调整位置：y - 60
        this.nameInput = new EditBox(this.font, centerX - 100, centerY - 60, 200, 20, Component.literal("姓名"));
        this.nameInput.setMaxLength(10);
        this.nameInput.setHint(Component.literal("请输入真实姓名")); // ✅ 灰色提示文字
        this.addRenderableWidget(this.nameInput);

        // 2. 身份证输入框
        // 调整位置：y - 10
        this.idInput = new EditBox(this.font, centerX - 100, centerY - 10, 200, 20, Component.literal("身份证号"));
        this.idInput.setMaxLength(18);
        this.idInput.setHint(Component.literal("请输入18位身份证号")); // ✅ 灰色提示文字
        this.addRenderableWidget(this.idInput);

        // 3. 认证按钮
        this.verifyButton = Button.builder(Component.literal("立即认证"), (btn) -> {
            startVerification();
        }).bounds(centerX - 50, centerY + 30, 100, 20).build();

        this.addRenderableWidget(this.verifyButton);

        // 4. 检测爷爷的身份证
        boolean hasCard = false;
        if (this.minecraft != null && this.minecraft.player != null) {
            for (ItemStack stack : this.minecraft.player.getInventory().items) {
                if (stack.is(ItemInit.GRANDPA_CARD.get())) {
                    hasCard = true;
                    break;
                }
            }
            if (!hasCard) {
                for (ItemStack stack : this.minecraft.player.getInventory().offhand) {
                    if (stack.is(ItemInit.GRANDPA_CARD.get())) {
                        hasCard = true;
                        break;
                    }
                }
            }
        }

        if (hasCard) {
            Button cheatButton = Button.builder(Component.literal("§6★ 使用爷爷的身份证 ★"), (btn) -> {
                ClientData.INSTANCE.isVerified = true;
                ClientData.INSTANCE.isAdult = true;
                ClientData.INSTANCE.isUsingFakeId = true;
                org.xiyu.healthygamer.client.ClientEventHandler.generateNextCheckTime();
                ClientData.save();
                this.onClose();
            }).bounds(centerX - 80, centerY + 60, 160, 20).build();

            this.addRenderableWidget(cheatButton);
        }
    }

    private void startVerification() {
        String id = idInput.getValue();
        if (!IDCardUtils.validate(id)) {
            this.errorMessage = "§c身份信息格式无效，请重试！";
            return;
        }
        this.isChecking = true;
        this.verifyButton.active = false;
        this.verifyButton.setMessage(Component.literal("正在联网核查..."));
        this.errorMessage = null;

        new Thread(() -> {
            IpUtils.IpResult ipResult = IpUtils.getCurrentIpLocation();
            Minecraft.getInstance().execute(() -> {
                handleIpResult(id, ipResult);
            });
        }).start();
    }

    private void handleIpResult(String id, IpUtils.IpResult ipResult) {
        this.isChecking = false;
        this.verifyButton.active = true;
        this.verifyButton.setMessage(Component.literal("立即认证"));

        if (!ipResult.success) {
            completeVerification(id);
            return;
        }
        if (!"CN".equals(ipResult.countryCode)) {
            completeVerification(id);
            return;
        }
        if (IDCardUtils.checkRegionMatch(id, ipResult.regionName)) {
            completeVerification(id);
        } else {
            this.errorMessage = "§c欺诈警告：IP属地(" + ipResult.regionName + ")与身份证归属地不符！";
        }
    }

    private void completeVerification(String id) {
        ClientData.INSTANCE.isVerified = true;
        ClientData.INSTANCE.isAdult = IDCardUtils.isAdult(id);
        ClientData.save();
        this.onClose();

        assert this.minecraft != null;
        if (ClientData.INSTANCE.isAdult) {
            this.minecraft.player.sendSystemMessage(Component.literal("§a[系统] 实名认证成功！"));
        } else {
            this.minecraft.player.sendSystemMessage(Component.literal("§e[防沉迷] 认证成功 (未成年人)，时间受限。"));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 绘制大标题
        graphics.drawCenteredString(this.font, "§l中国游戏防沉迷实名认证", centerX, centerY - 100, 0xFFFFFF);

        // ✅ 新增：在输入框上方绘制文字标签，让玩家知道填什么
        graphics.drawString(this.font, "真实姓名：", centerX - 100, centerY - 72, 0xA0A0A0, true);
        graphics.drawString(this.font, "身份证号：", centerX - 100, centerY - 22, 0xA0A0A0, true);

        // ✅ 修复：错误信息下移，防止遮挡按钮
        if (errorMessage != null) {
            graphics.drawCenteredString(this.font, errorMessage, centerX, centerY + 95, 0xFF0000);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
    @Override
    public boolean isPauseScreen() { return false; }
}
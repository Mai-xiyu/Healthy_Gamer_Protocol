package org.xiyu.healthygamer.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;

public class ClientData {
    // 存档文件：.minecraft/healthy_gamer_data.json
    private static final File FILE = new File(Minecraft.getInstance().gameDirectory, "healthy_gamer_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // === 存档数据结构 ===
    public static class Data {
        public String lastLoginDate = LocalDate.now().toString(); // 记录日期，用于跨天重置
        public long dailyPlayedTime = 0; // 今日已玩时间 (毫秒)

        public boolean isVerified = false; // 是否已认证
        public boolean isAdult = false;    // 是否成年
        public boolean isUsingFakeId = false; // 是否作弊

        public long nextFaceCheckTime = 0; // 下一次抽查的时间戳
    }

    public static Data INSTANCE = new Data();

    // 加载数据
    public static void load() {
        if (!FILE.exists()) {
            save(); // 不存在则创建默认
            return;
        }
        try (FileReader reader = new FileReader(FILE)) {
            INSTANCE = GSON.fromJson(reader, Data.class);
            if (INSTANCE == null) INSTANCE = new Data();

            // 检查是否跨天 (如果日期变了，重置今日时间)
            String today = LocalDate.now().toString();
            if (!today.equals(INSTANCE.lastLoginDate)) {
                INSTANCE.lastLoginDate = today;
                INSTANCE.dailyPlayedTime = 0; // 新的一天，时间清零
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            INSTANCE = new Data();
        }
    }

    // 保存数据
    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
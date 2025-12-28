package org.xiyu.healthygamer.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;

public class ClientData {
    private static final File FILE = new File(Minecraft.getInstance().gameDirectory, "healthy_gamer_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Data {
        public String lastLoginDate = LocalDate.now().toString();
        public long dailyPlayedTime = 0;

        public boolean isVerified = false;
        public boolean isAdult = false;
        public boolean isUsingFakeId = false;

        public long nextFaceCheckTime = 0;
    }

    public static Data INSTANCE = new Data();

    public static void load() {
        if (!FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(FILE)) {
            INSTANCE = GSON.fromJson(reader, Data.class);
            if (INSTANCE == null) INSTANCE = new Data();

            String today = LocalDate.now().toString();
            if (!today.equals(INSTANCE.lastLoginDate)) {
                INSTANCE.lastLoginDate = today;
                INSTANCE.dailyPlayedTime = 0;
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            INSTANCE = new Data();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
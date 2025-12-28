package org.xiyu.healthygamer.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class IpUtils {

    private static final String API_URL = "http://ip-api.com/json/?lang=zh-CN";

    public static class IpResult {
        public boolean success;
        public String countryCode;
        public String regionName;
        public String message;

        public IpResult(boolean success, String countryCode, String regionName) {
            this.success = success;
            this.countryCode = countryCode;
            this.regionName = regionName;
        }

        public static IpResult error(String msg) {
            IpResult r = new IpResult(false, null, null);
            r.message = msg;
            return r;
        }
    }

    /**
     * 同步请求 API (必须在子线程调用！)
     */
    public static IpResult getCurrentIpLocation() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Minecraft-HealthyGamer-Mod");

            int code = conn.getResponseCode();
            if (code != 200) {
                return IpResult.error("网络连接失败: " + code);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (!"success".equals(json.get("status").getAsString())) {
                return IpResult.error("API查询失败");
            }

            String country = json.get("countryCode").getAsString();
            String region = json.get("regionName").getAsString();

            return new IpResult(true, country, region);

        } catch (Exception e) {
            e.printStackTrace();
            return IpResult.error("网络错误: " + e.getMessage());
        }
    }
}
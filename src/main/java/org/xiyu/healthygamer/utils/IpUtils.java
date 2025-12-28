package org.xiyu.healthygamer.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class IpUtils {

    // 免费 API，指定语言为中文 (lang=zh-CN)
    private static final String API_URL = "http://ip-api.com/json/?lang=zh-CN";

    public static class IpResult {
        public boolean success;
        public String countryCode; // CN, US, JP...
        public String regionName;  // 北京市, 广东省...
        public String message;     // 错误信息

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
            conn.setConnectTimeout(3000); // 3秒超时
            conn.setReadTimeout(3000);
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

            // 解析 JSON
            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (!"success".equals(json.get("status").getAsString())) {
                return IpResult.error("API查询失败");
            }

            String country = json.get("countryCode").getAsString(); // CN
            String region = json.get("regionName").getAsString();   // 广东

            return new IpResult(true, country, region);

        } catch (Exception e) {
            e.printStackTrace();
            return IpResult.error("网络错误: " + e.getMessage());
        }
    }
}
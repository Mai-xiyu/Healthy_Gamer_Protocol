package org.xiyu.healthygamer.utils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class IDCardUtils {

    private static final int[] POWER = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] VERIFY_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
    private static final Map<String, String> PROVINCE_CODES = new HashMap<>();

    static {
        PROVINCE_CODES.put("11", "北京"); PROVINCE_CODES.put("12", "天津");
        PROVINCE_CODES.put("13", "河北"); PROVINCE_CODES.put("14", "山西");
        PROVINCE_CODES.put("15", "内蒙古"); PROVINCE_CODES.put("21", "辽宁");
        PROVINCE_CODES.put("22", "吉林"); PROVINCE_CODES.put("23", "黑龙江");
        PROVINCE_CODES.put("31", "上海"); PROVINCE_CODES.put("32", "江苏");
        PROVINCE_CODES.put("33", "浙江"); PROVINCE_CODES.put("34", "安徽");
        PROVINCE_CODES.put("35", "福建"); PROVINCE_CODES.put("36", "江西");
        PROVINCE_CODES.put("37", "山东"); PROVINCE_CODES.put("41", "河南");
        PROVINCE_CODES.put("42", "湖北"); PROVINCE_CODES.put("43", "湖南");
        PROVINCE_CODES.put("44", "广东"); PROVINCE_CODES.put("45", "广西");
        PROVINCE_CODES.put("46", "海南"); PROVINCE_CODES.put("50", "重庆");
        PROVINCE_CODES.put("51", "四川"); PROVINCE_CODES.put("52", "贵州");
        PROVINCE_CODES.put("53", "云南"); PROVINCE_CODES.put("54", "西藏");
        PROVINCE_CODES.put("61", "陕西"); PROVINCE_CODES.put("62", "甘肃");
        PROVINCE_CODES.put("63", "青海"); PROVINCE_CODES.put("64", "宁夏");
        PROVINCE_CODES.put("65", "新疆"); PROVINCE_CODES.put("71", "台湾");
        PROVINCE_CODES.put("81", "香港"); PROVINCE_CODES.put("82", "澳门");
    }

    /**
     * 严格校验身份证是否合法
     */
    public static boolean validate(String id) {
        if (id == null || id.length() != 18) return false;
        if (!Pattern.matches("^\\d{17}[\\d|X|x]$", id)) return false;
        if (!PROVINCE_CODES.containsKey(id.substring(0, 2))) return false;
        String birthDateStr = id.substring(6, 14);
        if (!isValidDate(birthDateStr)) return false;
        try {
            char[] chars = id.toUpperCase().toCharArray();
            int sum = 0;
            for (int i = 0; i < 17; i++) {
                sum += (chars[i] - '0') * POWER[i];
            }
            char expectedLast = VERIFY_CODE[sum % 11];
            return chars[17] == expectedLast;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 校验身份证归属地与 IP 地址是否匹配
     * @param idCard 身份证号
     * @param ipRegionName IP API 返回的省份名 (如 "广东省", "北京市")
     * @return true=匹配或无法判断, false=明确不匹配
     */
    public static boolean checkRegionMatch(String idCard, String ipRegionName) {
        if (idCard == null || idCard.length() < 2) return true;
        if (ipRegionName == null || ipRegionName.isEmpty()) return true;

        String code = idCard.substring(0, 2);
        String requiredProvince = PROVINCE_CODES.get(code);

        if (requiredProvince == null) return true;
        return ipRegionName.contains(requiredProvince);
    }


    /**
     * 判断是否成年 (精确到天)
     * 使用 Java 8 Time API，不再依赖硬编码年份
     */
    public static boolean isAdult(String id) {
        if (!validate(id)) return false;
        try {
            String birthDateStr = id.substring(6, 14);
            LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            LocalDate now = LocalDate.now();
            return Period.between(birthDate, now).getYears() >= 18;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 辅助方法：校验日期字符串是否有效
     */
    private static boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
package com.zzy.project.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * json工具类
 */
public class JsonValidator {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 判断字符串是否符合json格式
     * @param json
     * @return
     */
    public static boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}

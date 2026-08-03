package io.aik.steins.grimoire.core.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import io.aik.steins.grimoire.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具 -anchor
 *
 * <p>基于 Fastjson2 封装，统一项目 JSON 序列化/反序列化入口</p>
 *
 * @author a I k .
 */
@Slf4j
public final class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }

    /**
     * 对象转格式化的 JSON 字符串
     */
    public static String toJsonPretty(Object obj) {
        return JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
    }

    /**
     * JSON 字符串转对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        try {
            return JSON.parseObject(json, clazz);
        } catch (JSONException e) {
            log.warn("JSON 解析失败：{}", e.getMessage());
            throw new BusinessException("JSON 解析失败");
        }
    }

    /**
     * JSON 字符串转 List
     */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        try {
            return JSON.parseArray(json, clazz);
        } catch (JSONException e) {
            log.warn("JSON 解析失败：{}", e.getMessage());
            throw new BusinessException("JSON 解析失败");
        }
    }

    /**
     * JSON 字符串转 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseMap(String json) {
        try {
            return JSON.parseObject(json, Map.class);
        } catch (JSONException e) {
            log.warn("JSON 解析失败：{}", e.getMessage());
            throw new BusinessException("JSON 解析失败");
        }
    }

    /**
     * JSON 字符串转 JSONObject
     */
    public static JSONObject parseObject(String json) {
        try {
            return JSON.parseObject(json);
        } catch (JSONException e) {
            log.warn("JSON 解析失败：{}", e.getMessage());
            throw new BusinessException("JSON 解析失败");
        }
    }

    /**
     * JSON 字符串转 JSONArray
     */
    public static JSONArray parseArray(String json) {
        try {
            return JSON.parseArray(json);
        } catch (JSONException e) {
            log.warn("JSON 解析失败：{}", e.getMessage());
            throw new BusinessException("JSON 解析失败");
        }
    }

    /**
     * 对象转 JSONObject
     */
    public static JSONObject toJSONObject(Object obj) {
        return JSONObject.from(obj);
    }
}

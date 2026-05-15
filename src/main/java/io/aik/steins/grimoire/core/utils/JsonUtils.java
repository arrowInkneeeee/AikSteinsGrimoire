package io.aik.steins.grimoire.core.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具 -anchor
 *
 * <p>基于 Fastjson 封装，统一项目 JSON 序列化/反序列化入口</p>
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
        return JSON.toJSONString(obj, SerializerFeature.PrettyFormat);
    }

    /**
     * JSON 字符串转对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    /**
     * JSON 字符串转 List
     */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        return JSON.parseArray(json, clazz);
    }

    /**
     * JSON 字符串转 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseMap(String json) {
        return JSON.parseObject(json, Map.class);
    }

    /**
     * JSON 字符串转 JSONObject
     */
    public static JSONObject parseObject(String json) {
        return JSON.parseObject(json);
    }

    /**
     * JSON 字符串转 JSONArray
     */
    public static JSONArray parseArray(String json) {
        return JSON.parseArray(json);
    }

    /**
     * 对象转 JSONObject
     */
    public static JSONObject toJSONObject(Object obj) {
        return (JSONObject) JSON.toJSON(obj);
    }
}

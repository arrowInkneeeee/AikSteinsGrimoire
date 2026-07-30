package io.aik.steins.grimoire.core.utils;

import com.alibaba.fastjson.JSONObject;
import io.aik.steins.grimoire.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JsonUtils 单元测试 -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/7/29
 * -
 **/
@DisplayName("JSON 工具测试")
class JsonUtilsTest {

    private static final String VALID_JSON = "{\"name\":\"魔典\",\"level\":7}";
    private static final String VALID_ARRAY_JSON = "[\"卷轴\",\"药典\"]";
    private static final String INVALID_JSON = "{not-a-json";

    /**
     * 测试数据对象 -anchor
     *
     * @author a I k .
     */
    public static class SamplePo {

        private String name;
        private Integer level;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }
    }

    @Nested
    @DisplayName("序列化")
    class SerializeTest {

        @Test
        @DisplayName("对象转 JSON 字符串成功")
        void toJson_success() {
            // -anchor given
            SamplePo po = new SamplePo();
            po.setName("魔典");
            po.setLevel(7);

            // -anchor when
            String json = JsonUtils.toJson(po);

            // -anchor then
            assertThat(JsonUtils.parseMap(json))
                    .containsEntry("name", "魔典")
                    .containsEntry("level", 7);
        }

        @Test
        @DisplayName("对象转 JSONObject 成功")
        void toJSONObject_success() {
            // -anchor given
            SamplePo po = new SamplePo();
            po.setName("魔典");
            po.setLevel(7);

            // -anchor when
            JSONObject jsonObject = JsonUtils.toJSONObject(po);

            // -anchor then
            assertThat(jsonObject.getString("name")).isEqualTo("魔典");
            assertThat(jsonObject.getInteger("level")).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("反序列化")
    class DeserializeTest {

        @Test
        @DisplayName("JSON 字符串转对象成功")
        void parseObject_success() {
            // -anchor given & when
            SamplePo po = JsonUtils.parseObject(VALID_JSON, SamplePo.class);

            // -anchor then
            assertThat(po).isNotNull();
            assertThat(po.getName()).isEqualTo("魔典");
            assertThat(po.getLevel()).isEqualTo(7);
        }

        @Test
        @DisplayName("非法 JSON 转对象时抛出 BusinessException")
        void parseObject_invalidJson_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> JsonUtils.parseObject(INVALID_JSON, SamplePo.class))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("JSON 解析失败");
        }

        @Test
        @DisplayName("JSON 字符串转 List 成功")
        void parseList_success() {
            // -anchor given & when
            List<String> list = JsonUtils.parseList(VALID_ARRAY_JSON, String.class);

            // -anchor then
            assertThat(list).containsExactly("卷轴", "药典");
        }

        @Test
        @DisplayName("非法 JSON 转 List 时抛出 BusinessException")
        void parseList_invalidJson_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> JsonUtils.parseList(INVALID_JSON, String.class))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("JSON 解析失败");
        }

        @Test
        @DisplayName("JSON 字符串转 Map 成功")
        void parseMap_success() {
            // -anchor given & when
            Map<String, Object> map = JsonUtils.parseMap(VALID_JSON);

            // -anchor then
            assertThat(map).containsEntry("name", "魔典");
            assertThat(map).containsEntry("level", 7);
        }

        @Test
        @DisplayName("非法 JSON 转 Map 时抛出 BusinessException")
        void parseMap_invalidJson_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> JsonUtils.parseMap(INVALID_JSON))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("JSON 解析失败");
        }
    }
}

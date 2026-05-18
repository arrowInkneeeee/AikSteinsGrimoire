package io.aik.steins.grimoire.knowledge.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * -anchor 知识类型枚举
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Getter
@AllArgsConstructor
public enum KnowledgeTypeEnum {

    /**
     * 学习笔记
     */
    NOTE(1, "笔记"),

    /**
     * 可复用组件
     */
    COMPONENT(2, "组件"),

    /**
     * 解决方案
     */
    SOLUTION(3, "解决方案"),

    /**
     * 代码片段
     */
    CODE(4, "代码片段");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String desc;

    public static KnowledgeTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (KnowledgeTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}

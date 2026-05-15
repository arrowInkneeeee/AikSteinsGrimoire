package io.aik.steins.grimoire.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态枚举 -anchor
 *
 * @author a I k .
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    /**
     * 启用
     */
    ENABLE(1, "启用"),

    /**
     * 禁用
     */
    DISABLE(0, "禁用");

    private final int code;
    private final String desc;
}

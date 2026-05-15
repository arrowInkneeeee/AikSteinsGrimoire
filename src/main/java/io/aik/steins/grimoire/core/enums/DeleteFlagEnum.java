package io.aik.steins.grimoire.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 删除标志枚举 -anchor
 *
 * <p>配合 {@link io.aik.steins.grimoire.core.po.BaseLogicEntity} 使用</p>
 *
 * @author a I k .
 */
@Getter
@AllArgsConstructor
public enum DeleteFlagEnum {

    /**
     * 未删除
     */
    NOT_DELETED(0, "未删除"),

    /**
     * 已删除
     */
    DELETED(1, "已删除");

    private final int code;
    private final String desc;
}

package io.aik.steins.grimoire.system.param.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统参数分组枚举 -anchor
 *
 * @author a I k .
 */
@Getter
@AllArgsConstructor
public enum SystemParamGroupEnum {

    //note 系统配置
    SYSTEM("system", "系统配置"),

    //note 文件配置
    FILE("file", "文件配置"),

    //note 分页配置
    PAGE("page", "分页配置");

    private final String code;
    private final String desc;
}

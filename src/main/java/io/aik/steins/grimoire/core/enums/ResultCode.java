package io.aik.steins.grimoire.core.enums;

import io.aik.steins.grimoire.core.enums.IResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用响应码枚举 -anchor
 *
 * @author a I k .
 */
@Getter
@AllArgsConstructor
public enum ResultCode implements IResultCode {

    //note 成功
    SUCCESS(200, "操作成功"),

    //note 失败
    FAILURE(500, "操作失败"),

    //note 参数错误
    PARAM_ERROR(400, "参数错误"),

    //note 未授权
    UNAUTHORIZED(401, "未授权"),

    //note 禁止访问
    FORBIDDEN(403, "禁止访问"),

    //note 资源不存在
    NOT_FOUND(404, "资源不存在"),

    //note 服务器内部错误
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;
}

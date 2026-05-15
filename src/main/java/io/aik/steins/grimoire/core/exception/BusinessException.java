package io.aik.steins.grimoire.core.exception;

import io.aik.steins.grimoire.core.enums.IResultCode;
import io.aik.steins.grimoire.core.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常 -anchor
 *
 * <p>用于抛出预期内的业务错误，如参数校验失败、资源不存在、权限不足等</p>
 *
 * @author a I k .
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String msg;

    public BusinessException(String msg) {
        this(ResultCode.FAILURE.getCode(), msg);
    }

    public BusinessException(IResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    public BusinessException(IResultCode resultCode, String msg) {
        this(resultCode.getCode(), msg);
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }
}

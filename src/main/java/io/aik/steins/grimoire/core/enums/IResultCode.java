package io.aik.steins.grimoire.core.enums;

/**
 * 响应码接口 -anchor
 *
 * @author a I k .
 */
public interface IResultCode {

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    int getCode();

    /**
     * 获取响应消息
     *
     * @return 响应消息
     */
    String getMessage();
}

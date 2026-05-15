package io.aik.steins.grimoire.core.dto;

import io.aik.steins.grimoire.core.enums.IResultCode;
import io.aik.steins.grimoire.core.enums.ResultCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 接口统一返回对象 -anchor
 *
 * @param <T> 响应数据类型
 * @author a I k .
 */
@Data
@Schema(description = "接口统一返回对象")
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    private int code;

    @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private boolean success;

    @Schema(description = "响应消息", requiredMode = Schema.RequiredMode.REQUIRED, example = "操作成功")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    private ApiResponse() {
    }

    private ApiResponse(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
        this.success = ResultCode.SUCCESS.getCode() == code;
    }

    private ApiResponse(IResultCode resultCode, T data, String msg) {
        this(resultCode.getCode(), data, msg);
    }

    // --- success ---

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultCode.SUCCESS, null, ResultCode.SUCCESS.getMessage());
    }

    public static <T> ApiResponse<T> success(String msg) {
        return new ApiResponse<>(ResultCode.SUCCESS, null, msg);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS, data, ResultCode.SUCCESS.getMessage());
    }

    public static <T> ApiResponse<T> success(T data, String msg) {
        return new ApiResponse<>(ResultCode.SUCCESS, data, msg);
    }

    // --- fail ---

    public static <T> ApiResponse<T> fail(String msg) {
        return new ApiResponse<>(ResultCode.FAILURE, null, msg);
    }

    public static <T> ApiResponse<T> fail(int code, String msg) {
        return new ApiResponse<>(code, null, msg);
    }

    public static <T> ApiResponse<T> fail(IResultCode resultCode) {
        return new ApiResponse<>(resultCode, null, resultCode.getMessage());
    }

    public static <T> ApiResponse<T> fail(IResultCode resultCode, String msg) {
        return new ApiResponse<>(resultCode, null, msg);
    }

    // --- status ---

    public static <T> ApiResponse<T> status(boolean flag) {
        return flag ? success() : fail(ResultCode.FAILURE.getMessage());
    }

    public static <T> ApiResponse<T> status(boolean flag, String successMsg, String failMsg) {
        return flag ? success(successMsg) : fail(failMsg);
    }

    // --- judge ---

    public static <T> ApiResponse<T> judge(int rows) {
        return rows > 0 ? success("操作成功") : fail("操作失败");
    }

    // --- check ---

    public static boolean isSuccess(ApiResponse<?> result) {
        return result != null && result.isSuccess();
    }

    public static boolean isNotSuccess(ApiResponse<?> result) {
        return !isSuccess(result);
    }
}

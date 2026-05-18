package io.aik.steins.grimoire.core.exception;

import io.aik.steins.grimoire.core.dto.ApiResponse;
import io.aik.steins.grimoire.core.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 -anchor
 *
 * @author a I k .
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, msg={}", e.getCode(), e.getMsg());
        return ApiResponse.fail(e.getCode(), e.getMsg());
    }

    /**
     * 请求体参数校验失败（@RequestBody + @Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("请求体验证失败：{}", msg);
        return ApiResponse.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 表单参数绑定失败（@ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败：{}", msg);
        return ApiResponse.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 普通参数校验失败（@RequestParam / @PathVariable + @Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败：{}", msg);
        return ApiResponse.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 请求体 JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误：{}", e.getMessage());
        return ApiResponse.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误，请检查 JSON 语法");
    }

    /**
     * 404 接口不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("接口不存在：{} {}", e.getHttpMethod(), e.getRequestURL());
        return ApiResponse.fail(ResultCode.NOT_FOUND.getCode(), "接口不存在");
    }

    /**
     * 兜底：系统异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return ApiResponse.fail(ResultCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}

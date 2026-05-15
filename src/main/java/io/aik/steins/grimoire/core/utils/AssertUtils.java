package io.aik.steins.grimoire.core.utils;

import io.aik.steins.grimoire.core.enums.IResultCode;
import io.aik.steins.grimoire.core.enums.ResultCode;
import io.aik.steins.grimoire.core.exception.BusinessException;

import java.util.Collection;

/**
 * 业务断言工具 -anchor
 *
 * <p>条件不满足时抛 {@link BusinessException}，而非 {@link IllegalArgumentException}</p>
 *
 * @author a I k .
 */
public final class AssertUtils {

    private AssertUtils() {
    }

    // --- 对象非空 ---

    public static void notNull(Object obj, String msg) {
        if (obj == null) {
            throw new BusinessException(msg);
        }
    }

    public static void notNull(Object obj, IResultCode resultCode) {
        if (obj == null) {
            throw new BusinessException(resultCode);
        }
    }

    // --- 字符串非空 ---

    public static void notEmpty(String str, String msg) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(msg);
        }
    }

    public static void notEmpty(String str, IResultCode resultCode) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(resultCode);
        }
    }

    // --- 集合非空 ---

    public static void notEmpty(Collection<?> collection, String msg) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(msg);
        }
    }

    public static void notEmpty(Collection<?> collection, IResultCode resultCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(resultCode);
        }
    }

    // --- 表达式为真 ---

    public static void isTrue(boolean expression, String msg) {
        if (!expression) {
            throw new BusinessException(msg);
        }
    }

    public static void isTrue(boolean expression, IResultCode resultCode) {
        if (!expression) {
            throw new BusinessException(resultCode);
        }
    }

    // --- 表达式为假 ---

    public static void isFalse(boolean expression, String msg) {
        if (expression) {
            throw new BusinessException(msg);
        }
    }

    // --- 大于0 ---

    public static void gtZero(Number number, String msg) {
        if (number == null || number.longValue() <= 0) {
            throw new BusinessException(msg);
        }
    }

    // --- 非 null 且大于0 ---

    public static void notNullGtZero(Number number, String msg) {
        if (number == null || number.longValue() <= 0) {
            throw new BusinessException(msg);
        }
    }
}

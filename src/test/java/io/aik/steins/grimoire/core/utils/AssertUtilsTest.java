package io.aik.steins.grimoire.core.utils;

import io.aik.steins.grimoire.core.enums.ResultCode;
import io.aik.steins.grimoire.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AssertUtils 单元测试 -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/7/29
 * -
 **/
@DisplayName("业务断言工具测试")
class AssertUtilsTest {

    private static final String ERROR_MSG = "校验失败";

    @Nested
    @DisplayName("对象非空断言")
    class NotNullTest {

        @Test
        @DisplayName("对象非空时不抛异常")
        void notNull_nonNull_passes() {
            // -anchor given & when & then
            assertThatCode(() -> AssertUtils.notNull(new Object(), ERROR_MSG))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("对象为 null 时抛出 BusinessException")
        void notNull_null_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.notNull(null, ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
        }

        @Test
        @DisplayName("对象为 null 时按结果码抛出 BusinessException")
        void notNull_nullWithResultCode_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.notNull(null, ResultCode.FAILURE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ResultCode.FAILURE.getMessage());
        }
    }

    @Nested
    @DisplayName("字符串非空断言")
    class NotEmptyStringTest {

        @Test
        @DisplayName("字符串非空白时通过")
        void notEmpty_nonBlank_passes() {
            // -anchor given & when & then
            assertThatCode(() -> AssertUtils.notEmpty("魔典", ERROR_MSG))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("字符串为纯空白时抛出 BusinessException")
        void notEmpty_blank_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.notEmpty("   ", ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
        }

        @Test
        @DisplayName("字符串为 null 时抛出 BusinessException")
        void notEmpty_null_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.notEmpty((String) null, ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
        }
    }

    @Nested
    @DisplayName("集合非空断言")
    class NotEmptyCollectionTest {

        @Test
        @DisplayName("集合非空时通过")
        void notEmpty_nonEmpty_passes() {
            // -anchor given & when & then
            assertThatCode(() -> AssertUtils.notEmpty(Arrays.asList("卷轴"), ERROR_MSG))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("集合为空时抛出 BusinessException")
        void notEmpty_empty_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.notEmpty(Collections.emptyList(), ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
        }
    }

    @Nested
    @DisplayName("表达式断言")
    class ExpressionTest {

        @Test
        @DisplayName("isTrue 表达式为真时通过")
        void isTrue_true_passes() {
            // -anchor given & when & then
            assertThatCode(() -> AssertUtils.isTrue(1 + 1 == 2, ERROR_MSG))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isTrue 表达式为假时抛出 BusinessException")
        void isTrue_false_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.isTrue(false, ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
        }

        @Test
        @DisplayName("isFalse 表达式为假时通过")
        void isFalse_false_passes() {
            // -anchor given & when & then
            assertThatCode(() -> AssertUtils.isFalse(false, ERROR_MSG))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isFalse 表达式为真时抛出 BusinessException")
        void isFalse_true_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.isFalse(true, ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
        }
    }

    @Nested
    @DisplayName("数值大于零断言")
    class GtZeroTest {

        @Test
        @DisplayName("正数时通过")
        void gtZero_positive_passes() {
            // -anchor given & when & then
            assertThatCode(() -> AssertUtils.gtZero(1L, ERROR_MSG))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("零或负数时抛出 BusinessException")
        void gtZero_zeroOrNegative_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.gtZero(0, ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
            assertThatThrownBy(() -> AssertUtils.gtZero(-1, ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
        }

        @Test
        @DisplayName("数值为 null 时抛出 BusinessException")
        void notNullGtZero_null_throwsBusinessException() {
            // -anchor given & when & then
            assertThatThrownBy(() -> AssertUtils.notNullGtZero(null, ERROR_MSG))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ERROR_MSG);
        }
    }
}

package io.aik.steins.grimoire.components.eventbus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * -anchor 异常安全订阅注解
 *
 * <p>标注在监听器的方法上，表示该方法为事件订阅处理器。</p>
 * <p>与 Guava {@code @Subscribe} 不同，本注解配合 {@link io.aik.steins.grimoire.components.eventbus.core.EventBusManager}
 * 使用时，框架会自动捕获并记录方法执行中的异常，防止单个监听器异常影响其他监听器。</p>
 *
 * <p><b>使用约束：</b></p>
 * <ul>
 *     <li>方法必须只有一个参数（事件对象）</li>
 *     <li>方法必须为 public</li>
 *     <li>建议配合 {@link io.aik.steins.grimoire.components.eventbus.listener.AbstractStageListener} 使用</li>
 * </ul>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote 方法签名：public void onXxx(SomeEvent event)
 * @since 2026/09/16
 * -
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SafeSubscribe {
}

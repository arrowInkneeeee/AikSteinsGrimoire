package io.aik.steins.grimoire.components.threadpool.handler;

/**
 * -anchor 任务异常处理接口
 *
 * <p>扩展点：自定义任务执行异常时的处理逻辑</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@FunctionalInterface
public interface TaskExceptionHandler {

    /**
     * 处理任务执行异常
     *
     * @param throwable 异常对象
     * @param context   任务上下文（可为 null）
     */
    void handle(Throwable throwable, Object context);
}

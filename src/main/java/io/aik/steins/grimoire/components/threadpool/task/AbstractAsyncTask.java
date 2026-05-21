package io.aik.steins.grimoire.components.threadpool.task;

import io.aik.steins.grimoire.components.threadpool.handler.TaskExceptionHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * -anchor 异步任务抽象基类
 *
 * <p>模板方法模式：定义任务执行骨架，子类实现具体业务逻辑</p>
 *
 * @param <T> 任务上下文类型
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Slf4j
public abstract class AbstractAsyncTask<T> implements Runnable {

    protected final T context;
    @Setter
    private TaskExceptionHandler exceptionHandler;

    protected AbstractAsyncTask(T context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            execute(context);
        } catch (Exception e) {
            log.error("Async task execution failed, context={}", context, e);
            if (exceptionHandler != null) {
                exceptionHandler.handle(e, context);
            } else {
                handleException(e, context);
            }
        }
    }

    /**
     * 执行业务逻辑（由子类实现）
     *
     * @param context 任务上下文
     */
    protected abstract void execute(T context);

    /**
     * 异常处理（可由子类覆盖）
     *
     * @param throwable 异常对象
     * @param context   任务上下文
     */
    protected void handleException(Throwable throwable, T context) {
        // 默认空实现，子类可覆盖
    }
}

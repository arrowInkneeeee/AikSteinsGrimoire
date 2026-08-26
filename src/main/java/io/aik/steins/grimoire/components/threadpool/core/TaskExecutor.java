package io.aik.steins.grimoire.components.threadpool.core;

import io.aik.steins.grimoire.components.threadpool.task.AbstractAsyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * -anchor 任务提交入口
 *
 * <p>静态工具类，提供简洁的任务提交流口</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Slf4j
@Component("aikTaskExecutor")
@RequiredArgsConstructor
public class TaskExecutor {

    private final ThreadPoolManager threadPoolManager;

    /**
     * 提交 Runnable 任务
     *
     * @param task 任务
     */
    public void execute(Runnable task) {
        threadPoolManager.submit(task);
    }

    /**
     * 提交带上下文的异步任务
     *
     * @param task 异步任务
     * @param <T>  上下文类型
     */
    public <T> void execute(AbstractAsyncTask<T> task) {
        threadPoolManager.submit(task);
    }
}

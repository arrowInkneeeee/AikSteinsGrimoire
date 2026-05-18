package io.aik.steins.grimoire.components.threadpool.core;

import io.aik.steins.grimoire.components.threadpool.config.ThreadPoolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * -anchor 线程池管理器
 *
 * <p>基于 ThreadPoolExecutor 的轻量级封装，提供统一的异步任务执行能力：</p>
 * <ul>
 *     <li>动态线程池大小计算（根据 CPU 核心数自动调整）</li>
 *     <li>命名线程工厂（便于日志追踪）</li>
 *     <li>优雅关闭机制（JVM 停止时自动关闭）</li>
 * </ul>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Slf4j
@Component
public class ThreadPoolManager implements DisposableBean {

    private final ThreadPoolExecutor executor;
    private final ThreadPoolConfig config;

    public ThreadPoolManager(ThreadPoolConfig config) {
        this.config = config;
        int poolSize = calculatePoolSize(config);
        this.executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                config.getKeepAliveMinutes(),
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(config.getNamePrefix()),
                config.getRejectionPolicy().toHandler()
        );
        log.info("ThreadPoolManager initialized, poolSize={}, namePrefix={}", poolSize, config.getNamePrefix());
    }

    /**
     * 提交 Runnable 任务到线程池
     *
     * @param task 任务
     */
    public void submit(Runnable task) {
        executor.submit(task);
    }

    /**
     * 获取线程池当前活跃线程数
     *
     * @return 活跃线程数
     */
    public int getActiveCount() {
        return executor.getActiveCount();
    }

    /**
     * 获取线程池当前队列大小
     *
     * @return 队列大小
     */
    public int getQueueSize() {
        return executor.getQueue().size();
    }

    /**
     * 优雅关闭线程池
     */
    @Override
    public void destroy() {
        log.info("Shutting down thread pool...");
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
        log.info("Thread pool shutdown complete");
    }

    /**
     * 根据配置计算线程池大小
     *
     * @param config 线程池配置
     * @return 线程池大小
     */
    private int calculatePoolSize(ThreadPoolConfig config) {
        int processors = Runtime.getRuntime().availableProcessors();
        int size = (int) Math.floor(processors * config.getCpuRatio());
        return Math.max(size, config.getCoreSize());
    }
}

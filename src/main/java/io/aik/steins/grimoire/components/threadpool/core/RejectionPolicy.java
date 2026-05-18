package io.aik.steins.grimoire.components.threadpool.core;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * -anchor 线程池拒绝策略枚举
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
public enum RejectionPolicy {

    /**
     * 由提交任务的线程自己执行
     */
    CALLER_RUNS {
        @Override
        public RejectedExecutionHandler toHandler() {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        }
    },

    /**
     * 直接抛出 RejectedExecutionException
     */
    ABORT {
        @Override
        public RejectedExecutionHandler toHandler() {
            return new ThreadPoolExecutor.AbortPolicy();
        }
    },

    /**
     * 静默丢弃任务
     */
    DISCARD {
        @Override
        public RejectedExecutionHandler toHandler() {
            return new ThreadPoolExecutor.DiscardPolicy();
        }
    },

    /**
     * 丢弃队列中最老的任务
     */
    DISCARD_OLDEST {
        @Override
        public RejectedExecutionHandler toHandler() {
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        }
    };

    public abstract RejectedExecutionHandler toHandler();
}

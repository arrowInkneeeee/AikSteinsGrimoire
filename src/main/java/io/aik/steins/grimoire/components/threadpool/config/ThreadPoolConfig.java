package io.aik.steins.grimoire.components.threadpool.config;

import io.aik.steins.grimoire.components.threadpool.core.RejectionPolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * -anchor 线程池配置属性
 *
 * <p>支持通过 application.yml 外部化线程池参数</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/18
 * -
 */
@Data
@Component
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolConfig {

    /**
     * 保底线程数
     */
    private Integer coreSize = 2;

    /**
     * CPU 核心数乘数（如 0.3 表示 30%）
     */
    private Double cpuRatio = 0.3;

    /**
     * 线程空闲保活时间（分钟）
     */
    private Long keepAliveMinutes = 60L;

    /**
     * 线程名前缀
     */
    private String namePrefix = "async-task-";

    /**
     * 拒绝策略
     */
    private RejectionPolicy rejectionPolicy = RejectionPolicy.CALLER_RUNS;
}

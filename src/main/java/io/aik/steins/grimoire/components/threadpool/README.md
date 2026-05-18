# 线程池管理组件（Thread Pool Manager）

## 组件概述

基于 Java `ThreadPoolExecutor` 的轻量级线程池封装方案，为 Spring Boot 应用提供统一的异步任务执行能力。

## 核心能力

| 能力 | 说明 |
|------|------|
| 动态线程池大小 | 根据 CPU 核心数自动计算线程池大小（默认 30%，保底 2 个线程） |
| 命名线程工厂 | 线程名称前缀可配置，便于日志追踪 |
| 优雅关闭 | Spring 容器销毁时自动关闭线程池 |
| 任务抽象封装 | `AbstractAsyncTask<T>` 模板方法模式，子类只需实现业务逻辑 |
| 异常处理扩展 | 支持自定义 `TaskExceptionHandler` 处理任务异常 |
| 配置外化 | 全部参数支持 `application.yml` 外部化配置 |

## 文件清单

| 文件 | 职责 |
|------|------|
| `ThreadPoolConfig.java` | 配置属性类（@ConfigurationProperties） |
| `ThreadPoolManager.java` | 线程池管理器（Singleton Spring Bean） |
| `NamedThreadFactory.java` | 命名线程工厂 |
| `AbstractAsyncTask.java` | 异步任务抽象基类（模板方法模式） |
| `TaskExecutor.java` | 任务提交入口（工具类） |
| `TaskExceptionHandler.java` | 异常处理接口（扩展点） |
| `RejectionPolicy.java` | 拒绝策略枚举 |

## 快速开始

### Step 1：复制到目标项目

将本目录下所有文件复制到目标项目的 `com.xxx.common.threadpool` 包下。

### Step 2：添加配置

```yaml
thread-pool:
  core-size: 2
  cpu-ratio: 0.3
  keep-alive-minutes: 60
  name-prefix: "async-task-"
  rejection-policy: CALLER_RUNS
```

### Step 3：定义业务任务

```java
@Component
public class ReportGenerationTask extends AbstractAsyncTask<ReportRequest> {

    private final ReportService reportService;

    public ReportGenerationTask(ReportRequest request, ReportService reportService) {
        super(request);
        this.reportService = reportService;
    }

    @Override
    protected void execute(ReportRequest request) {
        reportService.generate(request);
    }

    @Override
    protected void handleException(Throwable throwable, ReportRequest request) {
        // 自定义异常处理
    }
}
```

### Step 4：提交任务

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final TaskExecutor taskExecutor;

    public void asyncGenerateReport(ReportRequest request) {
        taskExecutor.execute(new ReportGenerationTask(request, reportService));
    }
}
```

## 配置项说明

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `thread-pool.core-size` | Integer | 否 | `2` | 线程池保底大小 |
| `thread-pool.cpu-ratio` | Double | 否 | `0.3` | CPU 核心数乘数 |
| `thread-pool.keep-alive-minutes` | Long | 否 | `60` | 线程空闲保活时间（分钟） |
| `thread-pool.name-prefix` | String | 否 | `"async-task-"` | 线程名前缀 |
| `thread-pool.rejection-policy` | Enum | 否 | `CALLER_RUNS` | 拒绝策略 |

## 已知问题与改进建议

| 问题 | 严重程度 | 建议 |
|------|---------|------|
| 使用无界队列 `LinkedBlockingQueue` | 中 | 高负载下可能导致 OOM，建议改为有界队列并配合自定义拒绝策略 |
| `shutdownNow()` 强制中断任务 | 中 | 若任务不可中断，可能导致线程无法退出；建议改为 `shutdown()` + 等待超时后再 `shutdownNow()` |
| 无任务执行统计/监控 | 低 | 建议集成 Micrometer 暴露线程池活跃线程、队列大小等指标 |

## 依赖

- `spring-boot-starter`（用于 @ConfigurationProperties 和 Spring Bean）
- `lombok`（开发便利）
- 核心线程池仅依赖 JDK

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-05-18 | 基于 Component Manual 完成脱敏复写 |

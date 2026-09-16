# EventBus 事件驱动监听器组件（EventBus Listener）

## 组件概述

基于 Guava EventBus 的进程内发布-订阅模式封装，为 Spring Boot 应用提供事件驱动的级联联动能力。

## 核心能力

| 能力 | 说明 |
|------|------|
| 发布-订阅解耦 | 发布者不知道谁在监听，监听器不知道谁发布的 |
| 三阶段控制 | BEFORE / ON / AFTER 精确控制监听器执行时序 |
| 异常隔离 | `@SafeSubscribe` 防止单个监听器异常影响其他 |
| 四道防线 | 阶段过滤 → 空值防御 → 类型过滤 → 业务逻辑 |
| 开闭原则 | 新增联动只需加 Listener，不改已有代码 |

## 文件清单

| 文件 | 职责 |
|------|------|
| `core/StageEnum.java` | 阶段枚举（BEFORE / ON / AFTER） |
| `core/StageEvent.java` | 事件基类（持有阶段标识 + 泛型数据） |
| `core/EventBusManager.java` | 事件总线管理器（封装 Guava EventBus） |
| `annotation/SafeSubscribe.java` | 异常安全订阅注解 |
| `listener/AbstractStageListener.java` | 阶段监听器抽象基类（四道防线模板） |

## 快速开始

### Step 1：复制到目标项目

将本目录下所有文件复制到目标项目的 `com.xxx.common.eventbus` 包下。

### Step 2：添加依赖

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
</dependency>
```

### Step 3：定义事件

```java
public class TaskCompletedEvent extends StageEvent<List<Long>> {
    public TaskCompletedEvent(StageEnum stage, List<Long> taskIds) {
        super(stage, taskIds);
    }
}
```

### Step 4：编写监听器

```java
@Component
public class TaskCompletedListener extends AbstractStageListener<TaskCompletedEvent, List<Long>> {

    @Autowired
    private EventBusManager eventBusManager;

    @PostConstruct
    public void reg() {
        registerSelf(eventBusManager);
    }

    @Override
    protected StageEnum getTargetStage() {
        return StageEnum.ON;
    }

    @Override
    protected void handleEvent(TaskCompletedEvent event) {
        List<Long> taskIds = event.getPayload();
        // 联动逻辑...
    }

    @SafeSubscribe
    public void onTaskCompleted(TaskCompletedEvent event) {
        processEvent(event);
    }
}
```

### Step 5：发布事件

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final EventBusManager eventBusManager;

    public void completeTask(Long taskId) {
        // 业务操作...
        
        // 发布事件
        eventBusManager.post(new TaskCompletedEvent(StageEnum.ON, Collections.singletonList(taskId)));
    }
}
```

## 设计约束

| 约束 | 说明 |
|------|------|
| 同步执行 | 监听器与发布者在同一线程/事务中 |
| 顺序不可假设 | 同事件多监听器按注册顺序执行，不可依赖 |
| 幂等设计 | 同一事件可能被重复发布 |
| 事务回滚 | 监听器异常会导致整个事务回滚 |
| 判断"全部完成" | 必须用 `allMatch`，不用 `anyMatch` |

## 依赖

- `spring-boot-starter`（@Component、@PostConstruct）
- `guava`（EventBus 核心）
- `lombok`（开发便利）

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-09-16 | 基于 Component Manual 完成脱敏复写 |

# 验证方式

## 基础验证

### 1. 注册验证

启动应用，检查日志输出：

```
INFO  EventBusManager initialized
INFO  Listener registered: OrderCreatedListener
INFO  Listener registered: TaskCompletedListener
```

每个监听器都应出现注册日志。

### 2. 事件发布验证

触发业务操作，检查日志：

```
DEBUG Event posted: OrderCreatedEvent
DEBUG Event payload is null, skip. listener=SomeListener  ← 空值防御生效
```

### 3. 阶段控制验证

发布不同阶段的事件，确认监听器只在目标阶段执行：

```java
// BEFORE 阶段 — 只有 BEFORE 监听器执行
eventBusManager.post(new OrderCreatedEvent(StageEnum.BEFORE, orderId));

// ON 阶段 — 只有 ON 监听器执行
eventBusManager.post(new OrderCreatedEvent(StageEnum.ON, orderId));
```

## 异常隔离验证

### 4. 单监听器异常不影响其他

```java
// 监听器 A 抛异常
@SafeSubscribe
public void onOrderCreated(OrderCreatedEvent event) {
    throw new RuntimeException("模拟异常");
}

// 监听器 B 应正常执行
@SafeSubscribe
public void onOrderCreated(OrderCreatedEvent event) {
    // 正常联动逻辑
}
```

预期：监听器 A 异常被捕获并记录日志，监听器 B 正常执行。

## 业务场景验证

### 5. 级联状态变更

验证多实体级联场景：
- 操作 A → 触发事件 → 监听器 1 变更实体 B 状态 → 发布新事件 → 监听器 2 变更实体 C 状态

### 6. allMatch 语义验证

判断"全部完成"时必须使用 `allMatch`：

```java
// 正确：所有任务完成才标记委托单完成
if (tasks.stream().allMatch(t -> COMPLETED.equals(t.getState()))) {
    markCommissionCompleted();
}

// 错误：一个任务完成就标记（Bug!）
if (tasks.stream().anyMatch(t -> COMPLETED.equals(t.getState()))) {
    markCommissionCompleted();
}
```

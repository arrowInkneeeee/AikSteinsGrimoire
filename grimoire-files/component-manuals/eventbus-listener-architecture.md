# Component Manual v1.0 — EventBus 事件驱动监听器架构

## 类型标记

**SOLUTION** — 解决方案，一套可复用的事件驱动监听架构方案

---

## 第1章：概述

### 1.1 组件名称
EventBus 事件驱动监听器架构（EventBus Listener Architecture）

### 1.2 解决什么问题

在 LIMS 这类多实体关联的业务系统中，一个业务操作（如"复核任务"）往往需要触发一系列级联变更：

- 任务状态 → 样品检测状态 → 委托单状态
- 同步实验数据、生成处置任务、记录操作日志、同步设备结果…

如果用传统的方式在 Service 层硬编码所有联动逻辑，会导致：
- **强耦合**：一个 Service 方法要感知所有下游影响
- **难扩展**：新增联动逻辑必须修改已有 Service
- **难维护**：业务变更时改动面大

### 1.3 方案概述

基于 **Guava EventBus** 实现进程内发布-订阅模式：

- **事件发布者**（外部模块）只负责发布语义明确的事件（如 `ObjectAuditEvent`）
- **事件监听器**（应用层 21 个 Listener）各自订阅关心的事件，独立执行联动逻辑
- 通过 `CommonStageEventTypeEnum`（BEFORE/ON/AFTER）控制执行阶段
- 通过 `@Scope` 注解实现租户级隔离

### 1.4 适用场景
- 多实体级联状态变更（任务→样品→委托单）
- 操作日志记录、预警检测、数据同步等"旁路"逻辑
- 需要在不修改核心业务代码的前提下扩展联动逻辑

### 1.5 核心能力

| 能力 | 说明 |
|------|------|
| 发布-订阅解耦 | 发布者不知道谁在监听，监听器不知道谁发布的 |
| 阶段控制 | BEFORE/ON/AFTER 三阶段精确控制执行时序 |
| 异常隔离 | `@ThrowableSubscribe` 防止单个 Listener 异常影响其他 |
| 租户隔离 | `@Scope` 注解支持多租户数据隔离 |
| 开闭原则 | 新增联动只需加 Listener，不改已有代码 |

---

## 第2章：架构设计

### 2.1 分层架构

```
┌──────────────────────────────────────────────────────────────┐
│  外部模块 (lims-modules-taskmng / lims-modules-base 等)       │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Action 层：执行业务操作 + 发布事件                       │ │
│  │  taskFlowAction.audit() → EventBus.post(ObjectAuditEvent)│ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────┬───────────────────────────────────┘
                           │ EventBus 广播
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  应用层 (hussar-web)                                         │
│                                                              │
│  ┌────────────────────┐  ┌────────────────────────────────┐  │
│  │ ChangeTaskState    │  │ SampleTestStateModfiyListener  │  │
│  │ Listener           │  │                                │  │
│  │ BEFORE: 改任务状态  │  │ ON: 改样品状态 → 改委托单状态   │  │
│  └────────────────────┘  └────────────────────────────────┘  │
│                                                              │
│  ┌────────────────────┐  ┌────────────────────────────────┐  │
│  │ TaskAuditListener  │  │ SampleLogObjAddListener        │  │
│  │ ON: 联动委托单状态  │  │ ON: 记录操作日志               │  │
│  └────────────────────┘  └────────────────────────────────┘  │
│                                                              │
│  ┌────────────────────┐  ┌────────────────────────────────┐  │
│  │ AddDisposeTask     │  │ SyncDevResultAuditListener     │  │
│  │ ON: 生成处置任务    │  │ AFTER: 同步设备审核结果         │  │
│  └────────────────────┘  └────────────────────────────────┘  │
│                                                              │
│  ... 共 21 个 Listener                                       │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 设计原则

| 原则 | 实现方式 |
|------|---------|
| **开闭原则** | 新增联动只需加 Listener，不改已有代码 |
| **单一职责** | 每个 Listener 只关注一种事件的一种阶段 |
| **解耦** | 发布者不知道谁在监听，监听器不知道谁发布的 |
| **阶段可控** | BEFORE/ON/AFTER 三阶段精确控制执行时序 |
| **异常隔离** | `@ThrowableSubscribe` 防止单个 Listener 异常影响其他 |

---

## 第3章：核心组件清单

### 3.1 基础设施（外部模块提供）

| 组件 | 包路径 | 职责 |
|------|--------|------|
| `EventBusSupport` | `com.jxdinfo.lims.common.support.event` | EventBus 封装，提供 register/post |
| `@ThrowableSubscribe` | `com.jxdinfo.lims.common.support.event.guava` | 带异常捕获的 Subscribe 注解 |
| `CommonStageEventTypeEnum` | `com.jxdinfo.lims.common.support.event` | 阶段枚举：BEFORE / ON / AFTER |
| `CommonStageEventImpl` | `com.jxdinfo.lims.common.support.event.impl` | 事件基类，持有 type 字段 |
| `@Scope` | `com.jxdinfo.lims.common.support.scope.aop` | 租户隔离注解 |

### 3.2 事件类型

| 事件类 | 触发时机 | 携带数据 |
|--------|---------|---------|
| `ObjectAddEvent` | 实体新增 | `List<T> objs` |
| `ObjectCommitEvent` | 实体提交（提交审核） | `List<T> objs` |
| `ObjectAuditEvent` | 实体审核（复核完成） | `List<T> objs` |
| `ObjectBackEvent` | 实体退回 | `List<T> objs` |
| `ObjectRevokeEvent` | 实体撤回 | `List<T> objs` |
| `ObjectModifyEvent` | 实体修改 | `List<T> objs` |
| `SampleTestStateModifyEvent` | 样品检测状态变更 | `List<Long> ids` |
| `TaskCompleteEvent` | 任务全部完成 | `Collection<Long> taskIds` |
| `ToVoidEvent` | 任务作废 | `List<Long> taskIds` |
| `TerminationEvent` | 任务终止 | `List<Long> taskIds` |
| `ReStartTaskEvent` | 复核重开 | `List<Long> taskIds` |
| `SwitchPauseEvent` | 启动/停止 | `List<Long> taskIds` |
| `SampleInCompletedEvent` | 收样完成 | `List<T> objs` |
| `ReportArchivedEvent` | 报告归档 | `Long reportId, Long commId` |
| `ModifyResultEvent` | 实验结果修改 | `Collection<ExpeResultEntity>` |

### 3.3 监听器清单

| 监听器 | 订阅事件 | 阶段 | 职责 |
|--------|---------|------|------|
| `ChangeTaskStateListener` | Object*Event (12种) | BEFORE | 任务状态变更 |
| `TaskAuditListener` | ToVoid/Termination/TaskComplete | ON | 联动委托单状态 |
| `SampleTestStateModfiyListener` | ObjectAudit/SwitchPause/Termination/... | ON + AFTER | 样品检测状态 → 委托单状态 |
| `SampleLogObjAddListener` | ObjectCommit/Audit/Modify/Urgent/... | ON | 操作日志记录 |
| `CommissionStateListener` | SampleInCompleted/SampleInRunning | ON | 委托单收样/制样状态 |
| `AddDisposeTaskListener` | ObjectAuditEvent | ON | 生成样品处置任务 |
| `AddReteTaskListener` | SampleInCompleted/ObjectAudit | ON | 生成留样任务 |
| `AddSampleMakeTaskListener` | SampleInCompletedEvent | ON | 生成制样任务 |
| `SampleMakeTaskCompleteListener` | ObjectModifyEvent (SampleMakeTask) | AFTER | 制样完成→展开实验 |
| `SamplingTaskFlowListener` | SamplingTask*Event | AFTER | 采样任务流程流转 |
| `SyncDevResultAuditListener` | ObjectAuditEvent | AFTER | 同步设备审核结果 |
| `GradeListener` | ModifyResultEvent | ON | 等级评定 |
| `SampleWarningListener` | ModifyResultEvent | ON | 样品预警检测 |
| `ReportArchivedListener` | ReportArchivedEvent | ON | 报告归档→委托单完成 |
| `CreateReportListener` | CreateReportEvent | AFTER | 报告创建 |
| `AddDistributeRecordListener` | DistributeRecordEvent | AFTER | 分发记录 |
| `ChangeTaskResultListener` | — | — | 任务结果变更 |
| `TaskMergeListener` | — | — | 任务合并 |
| `SampleRetentionChangeListener` | — | — | 留样变更 |
| `CommissionObjListener` | ObjectAddEvent | ON | 委托单对象新增 |
| `ChangeLog4EntityListener` | EntityEvent | — | 变更日志 |

---

## 第4章：事件体系设计

### 4.1 事件继承结构

```
CommonStageEventImpl (基类)
  ├── getType(): CommonStageEventTypeEnum  ← BEFORE / ON / AFTER
  │
  ├── ObjectAddEvent       ← objs: List<T>
  ├── ObjectCommitEvent    ← objs: List<T>
  ├── ObjectAuditEvent     ← objs: List<T>
  ├── ObjectBackEvent      ← objs: List<T>
  ├── ObjectRevokeEvent    ← objs: List<T>
  ├── ObjectModifyEvent    ← objs: List<T>
  │
  ├── SampleTestStateModifyEvent  ← ids: List<Long>
  ├── TaskCompleteEvent           ← taskIds: Collection<Long>
  ├── ToVoidEvent                 ← taskIds: List<Long>
  ├── TerminationEvent            ← taskIds: List<Long>
  ├── ReStartTaskEvent            ← taskIds: List<Long>
  ├── SwitchPauseEvent            ← taskIds: List<Long>
  │
  ├── SampleInCompletedEvent      ← objs: List<T>
  ├── SampleInRunningEvent        ← objs: List<T>
  ├── ReportArchivedEvent         ← reportId, commId
  └── ModifyResultEvent           ← expeResultEntities: Collection
```

### 4.2 事件命名约定

- `Object*Event`：通用实体生命周期事件（Add/Commit/Audit/Back/Revoke/Modify）
- `*Event`（具体业务名）：业务领域事件（TaskComplete、SampleInCompleted、ReportArchived）
- 事件类名 = 业务动作 + `Event`

### 4.3 事件发布约定

- 由外部模块的 Action 层在业务操作完成后发布
- 同一个业务操作可以发布多个事件（级联）
- 事件携带最小必要数据（taskIds / objs / ids）

---

## 第5章：监听器编写模式

### 5.1 标准模板

```java
@Component
public class XxxListener {

    @Autowired
    private SomeAction someAction;
    @Autowired
    private SomeQuery someQuery;

    @PostConstruct
    public void reg() {
        EventBusSupport.register(this);  // 固定写法：注册到 EventBus
    }

    @ThrowableSubscribe                               // 异常安全订阅
    public void onSomeEvent(SomeEvent event) {
        // 1. 阶段过滤（必须）
        if (!CommonStageEventTypeEnum.ON.equals(event.getType())) {
            return;
        }
        // 2. 空值防御（必须）
        List objs = event.getObjs();
        if (CollUtil.isEmpty(objs)) {
            return;
        }
        // 3. 类型过滤（必须）
        Object first = CollUtil.getFirst(objs);
        if (first == null || !(first instanceof SomeEntity)) {
            return;
        }
        // 4. 业务逻辑
        List<SomeEntity> entities = objs;
        // ... 执行联动操作
    }
}
```

### 5.2 四道防线（必须遵守）

每个事件处理方法必须按顺序包含：

| # | 防线 | 代码 | 目的 |
|---|------|------|------|
| 1 | 阶段过滤 | `if (!CommonStageEventTypeEnum.XX.equals(event.getType())) return;` | 只在目标阶段执行 |
| 2 | 空值防御 | `if (CollUtil.isEmpty(objs)) return;` | 防空指针 |
| 3 | 类型过滤 | `if (!(first instanceof XxxEntity)) return;` | 确保数据类型正确 |
| 4 | 业务逻辑 | 实际处理代码 | 联动操作 |

### 5.3 一个 Listener 订阅多个事件

同一个 Listener 类可以有多个 `@ThrowableSubscribe` 方法，每个订阅不同的事件类型：

```java
@ThrowableSubscribe
public void onTaskAudit(ObjectAuditEvent event) { ... }

@ThrowableSubscribe
public void onTaskTermination(TerminationEvent event) { ... }

@ThrowableSubscribe
public void onTaskPause(SwitchPauseEvent event) { ... }
```

---

## 第6章：阶段控制机制（BEFORE/ON/AFTER）

### 6.1 三阶段语义

```
外部模块 Action 方法执行
    │
    ▼ BEFORE ──── 前置拦截：修改入参、设置初始状态
    │              在业务操作持久化之前执行
    │              典型用途：设置任务状态字段
    │
    ▼ 业务操作执行（持久化）
    │
    ▼ ON ──────── 核心联动：响应业务事件
    │              在业务操作完成后立即执行
    │              典型用途：级联状态变更、生成关联任务
    │
    ▼ AFTER ───── 后置处理：同步、日志、通知
                   在所有 ON 阶段完成后执行
                   典型用途：同步外部系统、记录审计日志
```

### 6.2 实际执行示例（audit 流程）

| 阶段 | 监听器 | 执行内容 |
|------|--------|---------|
| BEFORE | `ChangeTaskStateListener.beforeObjectAuditEvent()` | 任务状态 → REVIEW_COMPLETED("4") |
| ON | `SampleTestStateModfiyListener.onTaskAudit()` | 样品检测状态 → COMPLETED |
| ON | `TaskAuditListener.onTaskCompleteEvent()` | 委托单状态 → COMPLETED |
| ON | `AddDisposeTaskListener.onTaskAudit()` | 生成处置任务 |
| ON | `AddReteTaskListener.onTaskAudit()` | 生成留样任务 |
| ON | `SampleLogObjAddListener.onTaskAudit()` | 记录操作日志 |
| AFTER | `SyncDevResultAuditListener.onTaskAudit()` | 同步设备审核结果 |
| AFTER | `SampleTestStateModfiyListener.afterSampleTestStateModify()` | 委托单状态联动 |

### 6.3 阶段选择决策表

| 你的需求 | 应选阶段 |
|---------|---------|
| 需要在持久化前修改实体字段 | BEFORE |
| 需要响应业务操作做级联变更 | ON |
| 需要在所有联动完成后做收尾工作 | AFTER |
| 需要读取持久化后的数据 | ON 或 AFTER（不能是 BEFORE） |

---

## 第7章：级联事件与链路编排

### 7.1 级联模式

一个事件的处理可能触发新的事件发布，形成链路：

```
ObjectAuditEvent (任务复核)
    │
    ├─→ ChangeTaskStateListener: 任务状态 → REVIEW_COMPLETED
    │
    ├─→ SampleTestStateModfiyListener.onTaskAudit():
    │       检查样品下所有任务是否完成
    │       → sampleEntityAction.modifyTestState()
    │       → 发布 SampleTestStateModifyEvent
    │           │
    │           └─→ SampleTestStateModfiyListener.afterSampleTestStateModify():
    │                   检查委托单下所有样品是否完成
    │                   → commissionAction.updateStates(COMPLETED)
    │
    ├─→ TaskAuditListener.onTaskCompleteEvent():
    │       检查委托单下所有任务是否完成
    │       → commissionAction.updateStates(COMPLETED)
    │
    └─→ AddDisposeTaskListener: 生成处置任务
```

### 7.2 链路编排原则

1. **同一事件的多监听器无顺序保证**：Guava EventBus 按注册顺序分发，不应依赖执行顺序
2. **需要顺序时通过新事件串联**：如 `SampleTestStateModifyEvent` 由 ON 阶段的样品状态变更触发，AFTER 阶段的 `afterSampleTestStateModify` 消费
3. **避免循环**：A 事件触发 B 事件，B 事件不应再触发 A 事件
4. **幂等设计**：同一事件可能被重复发布，监听器逻辑应幂等

---

## 第8章：租户隔离与 @Scope

### 8.1 @Scope 注解

部分监听器方法标注了 `@Scope` 注解：

```java
@Scope
@ThrowableSubscribe
public void onToVoidEvent(ToVoidEvent event) { ... }
```

该注解配合 `ScopeContextHolder` 实现租户级数据隔离，确保监听器在执行时只操作当前租户的数据。

### 8.2 使用场景

- 涉及数据库写操作的监听器建议加 `@Scope`
- 纯查询或日志记录的监听器可不加
- 需要跨租户操作时，通过 `ScopeContextHolder.setTenantId()` 临时切换

---

## 第9章：踩坑记录与设计约束

### 9.1 anyMatch vs allMatch（严重）

**问题**：在判断"委托单是否完成"时，使用 `anyMatch`（任意一个满足即可）而非 `allMatch`（全部满足），导致只完成一个任务就把委托单标记为检测完成。

**修复**：
```java
// 错误：只要一个任务完成就标记委托单完成
if (tasks.stream().anyMatch(t -> REVIEW_COMPLETED.equals(t.getStateCode()))) { ... }

// 正确：所有任务都完成才标记委托单完成
if (tasks.stream().allMatch(t -> REVIEW_COMPLETED.equals(t.getStateCode()))) { ... }
```

**教训**：涉及"全部完成"语义的判断，必须用 `allMatch`。与同方法中"终止"的判断保持对称（终止用的 `allMatch`）。

### 9.2 事件执行顺序不可假设

**问题**：多个 Listener 订阅同一事件时，Guava EventBus 按注册顺序分发，但注册顺序取决于 Spring Bean 初始化顺序，不可依赖。

**约束**：如果 A 必须在 B 之前执行，应通过不同阶段（BEFORE vs ON）或级联事件来保证。

### 9.3 监听器中抛异常的影响

**问题**：`@ThrowableSubscribe` 会捕获异常，但如果是 BEFORE 阶段抛出异常，可能导致业务操作被中断。

**约束**：BEFORE 阶段的监听器应只做字段赋值，不做数据库查询等可能失败的操作。

### 9.4 事务边界

**问题**：监听器中的操作与事件发布者在同一事务中（Guava EventBus 是同步调用）。

**约束**：
- 监听器中的数据库操作会参与事件发布者的事务
- 监听器抛异常会导致整个事务回滚
- 长耗时操作应考虑异步化（当前架构为同步）

---

## 第10章：复用接入指南

### 10.1 新增联动逻辑（最常见）

**步骤**：
1. 确定要监听的事件类型（参考 §3.2 事件类型表）
2. 确定执行阶段（参考 §6.3 阶段选择决策表）
3. 创建 Listener 类，按 §5.1 标准模板编写
4. 加 `@Component` 注解，Spring 自动扫描注册

**示例**：
```java
@Component
public class MyNewListener {
    @Autowired
    private SomeAction someAction;

    @PostConstruct
    public void reg() {
        EventBusSupport.register(this);
    }

    @ThrowableSubscribe
    public void onTaskAudit(ObjectAuditEvent event) {
        if (!CommonStageEventTypeEnum.ON.equals(event.getType())) return;
        // ... 你的联动逻辑
    }
}
```

### 10.2 新增事件类型

**步骤**：
1. 创建事件类，继承 `CommonStageEventImpl`
2. 构造函数中传入 `CommonStageEventTypeEnum`
3. 在 Action 层业务操作完成后通过 `EventBusSupport.post()` 发布
4. 编写对应的 Listener 消费事件

### 10.3 接入检查清单

- [ ] Listener 类加 `@Component`
- [ ] `@PostConstruct reg()` 中调用 `EventBusSupport.register(this)`
- [ ] 每个 `@ThrowableSubscribe` 方法都有四道防线
- [ ] 阶段选择正确（BEFORE/ON/AFTER）
- [ ] 涉及数据库写操作时加 `@Scope`
- [ ] 判断"全部完成"用 `allMatch`，不用 `anyMatch`
- [ ] 不依赖同事件多监听器的执行顺序
- [ ] 监听器逻辑幂等

---

## 附录 A：完整事件流转时序图（audit 场景）

```
前端 POST /modules/taskFlow/audit
  │
  ▼
TaskFlowController.audit()
  │
  ▼
TaskFlowElnActionImpl.audit(flowBusinessId)
  │
  ├─► taskFlowAction.audit()  [外部模块]
  │     │
  │     │  发布 ObjectAuditEvent
  │     │
  │     ├──► [BEFORE] ChangeTaskStateListener
  │     │    └─ 任务状态 → REVIEW_COMPLETED("4")
  │     │
  │     ├──► [ON] SampleTestStateModfiyListener.onTaskAudit()
  │     │    ├─ 检查样品下所有任务是否都完成 (allMatch)
  │     │    ├─ 样品状态 → COMPLETED
  │     │    └─ 发布 SampleTestStateModifyEvent
  │     │         │
  │     │         └──► [AFTER] SampleTestStateModfiyListener
  │     │              .afterSampleTestStateModify()
  │     │              ├─ 检查委托单下所有样品是否都完成 (allMatch)
  │     │              └─ 委托单状态 → COMPLETED("10") ★
  │     │
  │     ├──► [ON] TaskAuditListener.onTaskCompleteEvent()
  │     │    ├─ 检查委托单下所有任务是否都完成 (allMatch)
  │     │    └─ 委托单状态 → COMPLETED("10") ★
  │     │
  │     ├──► [ON] AddDisposeTaskListener
  │     │    └─ 生成样品处置任务
  │     │
  │     ├──► [ON] AddReteTaskListener
  │     │    └─ 生成留样任务
  │     │
  │     ├──► [ON] SampleLogObjAddListener
  │     │    └─ 记录操作日志
  │     │
  │     └──► [AFTER] SyncDevResultAuditListener
  │          └─ 同步设备审核结果
  │
  ├─► 更新审核人/时间
  ├─► ELN 锁定
  └─► 振动专业处理
```

---

## 附录 B：关键设计决策记录

| 决策 | 选择 | 理由 |
|------|------|------|
| 进程内事件 vs MQ | 进程内 (Guava EventBus) | 联动逻辑需要同事务、低延迟 |
| Spring Event vs Guava EventBus | Guava EventBus | `@ThrowableSubscribe` 异常隔离；方法签名灵活，不强制接口 |
| 同步 vs 异步 | 同步 | 保证事务一致性；监听器异常可回滚主事务 |
| 阶段粒度 | 三段式 (BEFORE/ON/AFTER) | 覆盖前置拦截、核心联动、后置处理三个场景 |
| 事件数据粒度 | IDs / Entity List | 最小必要数据，监听器自行查询完整数据 |

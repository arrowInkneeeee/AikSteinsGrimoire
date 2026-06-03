---
name: feasibility-checker
description: 当需要基于当前项目技术栈评估用户故事的技术可行性、复杂度、风险和预估工时，并给出替代方案建议时使用。适用于"评估可行性"、"技术可行性分析"、"能不能做"、"实现难度评估"等场景。
type: Skill
version: 1.0.0
---

# feasibility-checker

## 输入

- **用户故事**: user-story-generator 的输出
- **技术栈信息**: 当前项目使用的技术栈（可选，如未提供则尝试自动检测）

## 输出

可行性评估报告

```json
{
  "assessments": [
    {
      "story_id": "US-001",
      "feasibility": "feasible|partial|not_feasible",
      "complexity": "low|medium|high",
      "risk_level": "low|medium|high",
      "estimated_effort": "粗略估算（人天）",
      "technical_requirements": ["技术需求1", "技术需求2"],
      "dependencies": ["依赖项1", "依赖项2"],
      "risks": [
        {
          "description": "风险描述",
          "mitigation": "缓解措施"
        }
      ],
      "alternatives": ["替代方案1", "替代方案2"],
      "recommendation": "总体建议"
    }
  ],
  "summary": {
    "total_stories": 5,
    "feasible": 4,
    "partial": 1,
    "not_feasible": 0,
    "high_risk": 1
  }
}
```

## 工作流

1. **获取项目技术栈信息**
   - 检查项目配置文件（pom.xml, build.gradle, package.json 等）
   - 识别主要技术框架和库
   - 记录技术约束
   - **针对 Java 技术栈**：检测 Spring Boot 版本、MyBatis-Plus、Lombok 等核心依赖

2. **分析每个用户故事的技术需求**
   - 识别所需技术能力
   - 检查与现有架构的兼容性
   - 评估集成复杂度
   - **针对 Java 技术栈**：评估 Controller/Service/Mapper/DTO/VO 分层实现难度

3. **评估技术实现难度**
   - 参考历史类似功能
   - 评估学习成本
   - 考虑团队技术储备
   - **针对 Java 技术栈**：评估 SQL 复杂度、索引需求、Stream API 适用性

4. **识别技术风险点**
   - 新技术引入风险
   - 第三方依赖风险
   - 性能瓶颈风险（N+1 查询、大数据量处理）
   - 安全合规风险
   - **针对 Java 技术栈**：事务边界、空指针风险、并发安全问题

5. **提供替代方案建议**
   - 简化方案
   - 分阶段实现方案
   - 技术替代选项
   - **针对 Java 技术栈**：MyBatis-Plus API vs 自定义 SQL、同步 vs 异步处理

## 评估维度

| 维度 | 描述 | 评估标准 |
|------|------|---------|
| feasibility | 技术可行性 | 当前技术栈能否直接支持 |
| complexity | 实现复杂度 | 代码量、架构改动范围、SQL 复杂度 |
| risk_level | 风险等级 | 失败概率、回滚难度 |
| estimated_effort | 预估工期 | 粗略人天估算 |
| dependencies | 外部依赖 | 第三方服务、API、库 |

## Java 技术栈专项评估维度

针对 **Java 8 + Spring Boot + MyBatis-Plus + Lombok** 技术栈，增加以下评估项：

| 评估项 | 描述 | 检查点 |
|--------|------|--------|
| **分层适配性** | 是否符合 Controller/Service/Mapper/DTO/VO 分层 | 各层职责是否清晰，DTO/VO 是否需要新增 |
| **MyBatis-Plus 适用性** | 能否使用 LambdaQueryWrapper/LambdaUpdateWrapper 实现 | 简单查询优先使用 MP API，复杂业务评估是否需要自定义 SQL |
| **Stream API 适用性** | 集合处理是否适合使用 Stream + Lambda | 数据量、可读性、性能权衡 |
| **SQL 复杂度** | 数据库查询复杂度评估 | 是否涉及多表 JOIN、子查询、是否需要索引优化 |
| **事务边界** | 事务范围是否合理 | 避免大事务、事务失效场景（同类方法调用、异步等） |
| **空指针风险** | 潜在 NPE 风险点 | Optional 使用建议、入参校验需求 |
| **性能风险** | N+1 查询、大数据量处理 | 分页策略、批量处理、缓存需求 |
| **日志规范** | 是否符合 SLF4J + @Slf4j 规范 | 核心流程 log.info、异常 log.error |

## 复杂度定义

- **low**: 1-3 天，使用现有组件，少量代码
  - **Java 参考**：单表 CRUD，使用 MyBatis-Plus Lambda API，简单 Stream 处理
- **medium**: 3-7 天，需要新组件，中等代码量
  - **Java 参考**：多表关联，需要自定义 SQL，复杂业务逻辑，需新增 DTO/VO
- **high**: 7+ 天，架构改动，大量代码或研究
  - **Java 参考**：引入新中间件，复杂事务场景，性能优化（SQL 重构、缓存设计），高并发处理

## 风险等级定义

- **low**: 技术成熟，团队熟悉，无外部依赖
  - **Java 场景**：标准 CRUD，单表查询，无复杂事务
- **medium**: 有一定不确定性，需要调研或学习
  - **Java 场景**：复杂 SQL（多表 JOIN、子查询），分布式事务，新第三方 SDK 集成
- **high**: 技术新颖，依赖不稳定，或存在已知问题
  - **Java 场景**：大数据量处理（百万级+），高并发（QPS 1000+），核心业务流程变更，潜在 N+1 查询风险

## 调用规则

- 在 user-story-generator 之后调用
- 需要技术栈信息，如未提供应尝试自动检测
- 高风险项需要特别关注并制定缓解措施

## 示例

### 输入

```json
{
  "user_stories": [
    {
      "id": "US-001",
      "role": "顾客",
      "want": "使用微信支付订单",
      "so_that": "方便快捷地完成付款"
    },
    {
      "id": "US-002",
      "role": "顾客",
      "want": "实时查看订单物流状态",
      "so_that": "了解商品配送进度"
    },
    {
      "id": "US-003",
      "role": "顾客",
      "want": "查看我的历史订单列表",
      "so_that": "管理我的购买记录"
    }
  ],
  "tech_stack": {
    "backend": "Java 8 + Spring Boot 2.7",
    "database": "MySQL 8.0",
    "orm": "MyBatis-Plus",
    "tools": "Lombok, SLF4J",
    "payment": "未接入"
  }
}
```

### 输出

```json
{
  "assessments": [
    {
      "story_id": "US-001",
      "feasibility": "feasible",
      "complexity": "medium",
      "risk_level": "low",
      "estimated_effort": "3-5 天",
      "technical_requirements": [
        "接入微信支付 SDK",
        "实现支付回调接口",
        "订单状态管理"
      ],
      "java_specific": {
        "layer_design": "Controller -> OrderService -> OrderMapper + PaymentMapper",
        "dto_vo_needed": ["PaymentRequestDTO", "PaymentResponseVO", "OrderVO"],
        "mybatis_plus_usage": "使用 LambdaUpdateWrapper 更新订单状态",
        "stream_api_usage": "订单状态流转可使用 Stream 处理状态机",
        "sql_complexity": "中等，涉及订单表 + 支付记录表",
        "transaction_boundary": "支付回调需保证幂等性，避免重复支付",
        "npe_risk": "回调参数需校验，使用 Optional 处理空值",
        "index_suggestion": "订单表加 order_no 唯一索引，user_id + status 联合索引"
      },
      "dependencies": [
        "微信支付商户账号",
        "HTTPS 域名"
      ],
      "risks": [
        {
          "description": "微信支付审核可能不通过",
          "mitigation": "提前准备企业资质，申请测试账号先行开发"
        },
        {
          "description": "支付回调幂等性处理不当可能导致重复入账",
          "mitigation": "使用数据库唯一索引或 Redis 分布式锁保证幂等"
        }
      ],
      "alternatives": [
        "使用第三方支付聚合平台（如 Ping++）",
        "先实现支付宝支付"
      ],
      "recommendation": "可行，建议先申请微信支付测试账号进行开发。注意支付回调的幂等性处理。"
    },
    {
      "story_id": "US-002",
      "feasibility": "partial",
      "complexity": "medium",
      "risk_level": "medium",
      "estimated_effort": "5-7 天",
      "technical_requirements": [
        "接入物流查询 API",
        "实现 WebSocket 或轮询机制",
        "物流数据缓存策略"
      ],
      "java_specific": {
        "layer_design": "Controller -> LogisticsService -> LogisticsMapper",
        "dto_vo_needed": ["LogisticsQueryDTO", "LogisticsVO"],
        "mybatis_plus_usage": "使用 LambdaQueryWrapper 查询物流记录",
        "stream_api_usage": "物流轨迹列表处理可使用 Stream API",
        "sql_complexity": "低，单表查询为主",
        "transaction_boundary": "物流查询不涉及事务",
        "npe_risk": "物流单号可能为空，需使用 Optional 处理",
        "performance_risk": "频繁查询物流 API 可能触发限流"
      },
      "dependencies": [
        "物流 API 服务商（如快递100）",
        "实时数据推送方案"
      ],
      "risks": [
        {
          "description": "物流 API 可能收费或有限流",
          "mitigation": "评估多家物流 API 服务商，设计缓存减少调用"
        },
        {
          "description": "实时推送增加服务器负载",
          "mitigation": "采用轮询替代 WebSocket，或限制推送频率"
        }
      ],
      "alternatives": [
        "非实时查询，用户手动刷新",
        "仅显示物流单号，跳转第三方查询"
      ],
      "recommendation": "部分可行，建议先实现基础查询功能，实时推送作为二期优化"
    },
    {
      "story_id": "US-003",
      "feasibility": "feasible",
      "complexity": "low",
      "risk_level": "low",
      "estimated_effort": "1-2 天",
      "technical_requirements": [
        "订单列表查询接口",
        "分页功能实现",
        "订单状态筛选"
      ],
      "java_specific": {
        "layer_design": "Controller -> OrderService -> OrderMapper",
        "dto_vo_needed": ["OrderQueryDTO", "OrderListVO"],
        "mybatis_plus_usage": "使用 LambdaQueryWrapper 构建动态查询条件",
        "stream_api_usage": "订单列表转 VO 可使用 Stream.map()",
        "sql_complexity": "低，单表 + 分页",
        "transaction_boundary": "查询操作，无需事务",
        "npe_risk": "低，入参使用 DTO 接收，JSR-303 校验",
        "index_suggestion": "user_id + create_time 联合索引用于分页查询",
        "performance_risk": "注意大分页性能（深度分页问题），建议限制最大页码或使用游标分页"
      },
      "dependencies": [],
      "risks": [
        {
          "description": "历史订单量大时，深度分页性能下降",
          "mitigation": "限制最大查询页数（如最多 100 页），或使用游标分页（search_after）"
        }
      ],
      "alternatives": [
        "使用 ES 存储订单数据，支持更复杂查询"
      ],
      "recommendation": "可行，标准 CRUD 功能。注意分页性能优化，建议使用 MyBatis-Plus 分页插件。"
    }
  ],
  "summary": {
    "total_stories": 3,
    "feasible": 2,
    "partial": 1,
    "not_feasible": 0,
    "high_risk": 0
  }
}
```

## 文本格式输出

```markdown
# 可行性评估报告

## US-001: 使用微信支付订单

- **可行性**: ✅ 可行
- **复杂度**: 中等
- **风险等级**: 低
- **预估工期**: 3-5 天

### 技术需求
- 接入微信支付 SDK
- 实现支付回调接口
- 订单状态管理

### Java 技术栈专项评估

**分层设计**:
```
Controller -> OrderService -> OrderMapper + PaymentMapper
```

**DTO/VO 需求**:
- `PaymentRequestDTO` - 支付请求参数
- `PaymentResponseVO` - 支付响应数据
- `OrderVO` - 订单展示数据

**MyBatis-Plus 使用建议**:
- 使用 `LambdaUpdateWrapper` 更新订单状态
- 支付记录查询使用 `LambdaQueryWrapper.eq(Payment::getOrderNo, orderNo)`

**Stream API 适用场景**:
- 订单状态流转可使用 Stream 处理状态机转换

**SQL 复杂度**: 中等（订单表 + 支付记录表关联）

**事务边界**:
- ⚠️ 支付回调需保证幂等性，避免重复支付
- 建议：使用数据库唯一索引（order_no + payment_no）

**空指针风险**:
- 回调参数需校验，使用 `Optional.ofNullable()` 处理可能为空字段

**索引建议**:
```sql
-- 订单表
ALTER TABLE t_order ADD UNIQUE INDEX uk_order_no (order_no);
ALTER TABLE t_order ADD INDEX idx_user_status (user_id, status);

-- 支付记录表
ALTER TABLE t_payment ADD UNIQUE INDEX uk_payment_no (payment_no);
ALTER TABLE t_payment ADD INDEX idx_order_no (order_no);
```

### 外部依赖
- 微信支付商户账号
- HTTPS 域名

### 风险与缓解
| 风险 | 缓解措施 |
|------|---------|
| 微信支付审核可能不通过 | 提前准备企业资质，申请测试账号先行开发 |
| 支付回调幂等性处理不当 | 使用数据库唯一索引或 Redis 分布式锁保证幂等 |

### 替代方案
1. 使用第三方支付聚合平台（如 Ping++）
2. 先实现支付宝支付

### 建议
可行，建议先申请微信支付测试账号进行开发。注意支付回调的幂等性处理。

---

## US-002: 实时查看订单物流状态

- **可行性**: ⚠️ 部分可行
- **复杂度**: 中等
- **风险等级**: 中等
- **预估工期**: 5-7 天

### 技术需求
- 接入物流查询 API
- 实现 WebSocket 或轮询机制
- 物流数据缓存策略

### Java 技术栈专项评估

**分层设计**:
```
Controller -> LogisticsService -> LogisticsMapper
```

**DTO/VO 需求**:
- `LogisticsQueryDTO` - 物流查询参数
- `LogisticsVO` - 物流展示数据

**MyBatis-Plus 使用建议**:
- 使用 `LambdaQueryWrapper` 查询物流记录
- 示例：`wrapper.eq(Logistics::getOrderNo, orderNo).orderByDesc(Logistics::getUpdateTime)`

**Stream API 适用场景**:
- 物流轨迹列表处理可使用 `list.stream().map(this::convertToVO).collect(Collectors.toList())`

**SQL 复杂度**: 低（单表查询为主）

**事务边界**: 物流查询不涉及事务

**空指针风险**: 物流单号可能为空，需使用 `Optional.ofNullable()` 处理

**性能风险**: 频繁查询物流 API 可能触发限流，建议本地缓存 30 分钟

### 外部依赖
- 物流 API 服务商（如快递100）
- 实时数据推送方案

### 风险与缓解
| 风险 | 缓解措施 |
|------|---------|
| 物流 API 可能收费或有限流 | 评估多家物流 API 服务商，设计缓存减少调用 |
| 实时推送增加服务器负载 | 采用轮询替代 WebSocket，或限制推送频率 |

### 替代方案
1. 非实时查询，用户手动刷新
2. 仅显示物流单号，跳转第三方查询

### 建议
部分可行，建议先实现基础查询功能，实时推送作为二期优化

---

## US-003: 查看我的历史订单列表

- **可行性**: ✅ 可行
- **复杂度**: 低
- **风险等级**: 低
- **预估工期**: 1-2 天

### 技术需求
- 订单列表查询接口
- 分页功能实现
- 订单状态筛选

### Java 技术栈专项评估

**分层设计**:
```
Controller -> OrderService -> OrderMapper
```

**DTO/VO 需求**:
- `OrderQueryDTO` - 查询参数（分页、状态筛选）
- `OrderListVO` - 列表展示数据

**MyBatis-Plus 使用建议**:
- 使用 `LambdaQueryWrapper` 构建动态查询条件
- 示例代码：
```java
LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Order::getUserId, userId)
       .eq(status != null, Order::getStatus, status)
       .orderByDesc(Order::getCreateTime);
return orderMapper.selectPage(page, wrapper);
```

**Stream API 适用场景**:
- 订单列表转 VO 可使用 `records.stream().map(orderConverter::toVO).collect(Collectors.toList())`

**SQL 复杂度**: 低（单表 + 分页）

**事务边界**: 查询操作，无需事务

**空指针风险**: 低，入参使用 DTO 接收，JSR-303 校验（`@NotNull`、`@Min` 等）

**索引建议**:
```sql
-- 支持用户订单列表分页查询
ALTER TABLE t_order ADD INDEX idx_user_create_time (user_id, create_time);

-- 支持状态筛选
ALTER TABLE t_order ADD INDEX idx_user_status (user_id, status);
```

**性能风险**: ⚠️ 注意大分页性能（深度分页问题）
- 建议限制最大页码（如最多 100 页）
- 或使用游标分页（search_after）

### 风险与缓解
| 风险 | 缓解措施 |
|------|---------|
| 历史订单量大时，深度分页性能下降 | 限制最大查询页数（如最多 100 页），或使用游标分页 |

### 替代方案
- 使用 ES 存储订单数据，支持更复杂查询

### 建议
可行，标准 CRUD 功能。建议使用 MyBatis-Plus 分页插件（`Page<T>`），注意分页性能优化。

---

## 汇总

| 指标 | 数量 |
|------|------|
| 总故事数 | 3 |
| 可行 | 2 |
| 部分可行 | 1 |
| 不可行 | 0 |
| 高风险 | 0 |

### Java 技术栈实施建议

1. **统一分层规范**: 所有功能遵循 Controller/Service/Mapper/DTO/VO 分层
2. **MyBatis-Plus 优先**: 简单查询优先使用 Lambda API，复杂业务再考虑自定义 SQL
3. **Stream API 适度使用**: 集合转换场景使用，避免过度复杂化
4. **日志规范**: 使用 `@Slf4j`，核心流程 `log.info`，异常 `log.error`
5. **索引先行**: 开发前设计好索引，避免上线后性能问题
```

## 注意事项

- 工期估算为粗略估计，供参考而非承诺
- 技术栈信息不完整时，应标注假设条件
- 高风险项应提供详细的缓解措施
- 替代方案应切实可行，而非敷衍

## Java 技术栈评估检查清单

针对 **Java 8 + Spring Boot + MyBatis-Plus + Lombok** 技术栈，评估时必须检查：

### 代码规范
- [ ] 是否可以使用 Java 8 Stream API + Lambda 处理集合
- [ ] 空值处理是否建议使用 Optional
- [ ] 复杂业务是否适合拆分方法保持可读性

### 架构分层
- [ ] Controller/Service/Mapper 职责是否清晰
- [ ] DTO/VO 是否需要新增
- [ ] 是否需要 Converter/Mapper 进行对象转换

### MyBatis-Plus
- [ ] 简单查询是否可使用 LambdaQueryWrapper/LambdaUpdateWrapper
- [ ] 复杂业务是否需要自定义 SQL（Mapper XML）
- [ ] 分页是否使用 MyBatis-Plus Page 插件

### 数据库
- [ ] SQL 复杂度评估（单表/多表 JOIN/子查询）
- [ ] 索引设计建议（列出具体 SQL）
- [ ] 是否存在 N+1 查询风险
- [ ] 大数据量场景是否需要优化（分页、批量）

### 事务与并发
- [ ] 事务边界是否合理（避免大事务）
- [ ] 是否存在事务失效场景（同类方法调用、异步等）
- [ ] 并发场景是否需要锁机制

### 异常与日志
- [ ] 业务异常处理建议（自定义 BusinessException）
- [ ] 日志记录点建议（核心流程 info，异常 error）
- [ ] 是否使用 `@Slf4j` 注解

### 性能
- [ ] 是否存在深度分页问题
- [ ] 是否需要缓存（Redis/Caffeine）
- [ ] 外部 API 调用是否需要限流/熔断

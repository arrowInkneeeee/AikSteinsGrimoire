---
name: desensitization-patterns
description: 脱敏模式与替换策略详细指南，供 component-code-rewriter 按需加载
---

# 脱敏模式与替换策略

## 1. 命名脱敏映射模板

### 1.1 常见领域词汇 → 通用词汇对照

| 源命名（业务） | 通用化命名 | 适用场景 |
|---------------|-----------|---------|
| Order / order | Resource / resource | 通用资源、数据实体 |
| Payment / pay | Transaction / transaction | 通用交易、操作记录 |
| User / user | Principal / subject | 通用主体、实体 |
| Customer | Client | 通用客户 |
| Merchant | Partner | 通用合作方 |
| Product / goods | Item / entity | 通用条目 |
| Audit | Tracker / recorder | 通用追踪 |
| Notification / notify | Message / messenger | 通用消息 |
| File / attachment | Document / doc | 通用文档 |
| Export / import | DataTransfer | 通用数据迁移 |
| Statistics / report | Analytics | 通用分析 |
| Login / logout | Authenticate / auth | 通用认证 |
| Permission / auth | Access / accessCtrl | 通用访问控制 |

### 1.2 命名替换策略

**全量替换**：从头到尾不保留任何业务词汇
```
OrderLockService  →  ResourceLockService
OrderCreateDto    →  ResourceCreateDto
lockOrder()       →  lock()
T_ORDER           →  ${component.table-name}
```

**模式保留**：保留设计模式标识，去除业务部分
```
OrderStrategy     →  ResourceStrategy    // 保留 Strategy
PaymentHandler    →  TransactionHandler  // 保留 Handler
UserObserver      →  SubjectObserver     // 保留 Observer
```

**后缀规范**（遵循 aIk-coding-style）：
```
Po, Dto, Vo, Mapper, Service, ServiceImpl, Controller, Config, Util, Constant
```

---

## 2. 配置外化模板

### 2.1 @ConfigurationProperties 模式

```java
@Data
@ConfigurationProperties(prefix = "component.lock")
public class LockConfig {
    /** 锁前缀 */
    private String prefix = "lock:";
    /** 获取锁超时(秒) */
    private long timeoutSeconds = 30;
    /** 是否启用 */
    private boolean enabled = true;
}
```

对应的 application.yml：
```yaml
component:
  lock:
    prefix: ${LOCK_PREFIX:lock:}
    timeout-seconds: 30
    enabled: true
```

### 2.2 @Value 模式（简单配置项 < 3个）

```java
@Value("${component.lock.prefix:lock:}")
private String prefix;

@Value("${component.lock.timeout-seconds:30}")
private long timeoutSeconds;
```

### 2.3 敏感配置外化

```yaml
# application-component-xxx.yml
component:
  xxx:
    api-key: ${API_KEY}           # 不提供默认值，强制外部注入
    db-url: ${DB_URL}
    secret: ${SECRET}
```

---

## 3. 抽象化策略模板

### 3.1 泛型化实体

```java
// 源：绑定具体类型
public class OrderPo extends BaseEntity {
    private String orderNo;
}

// 复写：泛型化
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ResourcePo extends BaseEntity {
    private String resourceKey;   // 通用命名
    private String resourceType;  // 通用类型字段
}
```

### 3.2 接口抽象依赖

```java
// 源：依赖具体类
@Service
@RequiredArgsConstructor
public class OrderLockServiceImpl {
    private final OrderMapper orderMapper;
    private final OrderValidator orderValidator;
}

// 复写：依赖接口
@Service("component.lock.ResourceLockService")
@RequiredArgsConstructor
public class ResourceLockServiceImpl implements ResourceLockService {
    private final BaseMapper<ResourcePo> mapper;
    private final ResourceValidator validator;
}
```

### 3.3 策略参数化

```java
// 源：硬编码分支
if ("VIP".equals(type)) {
    handler.handleVip(param);
} else {
    handler.handleNormal(param);
}

// 复写：策略模式
private final Map<String, ResourceHandler> handlerMap;

public void handle(String type, Object param) {
    ResourceHandler handler = handlerMap.getOrDefault(type, defaultHandler);
    handler.handle(param);
}
```

---

## 4. 数据库相关脱敏

### 4.1 MyBatis-Plus 实体

```java
// 源
@TableName("T_ORDER")
public class OrderPo extends BaseEntity { }

// 复写 Option A：默认表名策略（推荐）
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ResourcePo extends BaseEntity { }
// 表名自动映射：resource_po（MyBatis-Plus 默认下划线策略）

// 复写 Option B：配置化表名（必须指定时）
@TableName("${component.resource.table-name}")
public class ResourcePo extends BaseEntity { }
```

### 4.2 Mapper 查询

```java
// 源：字符串列名
wrapper.eq("order_status", status);
mapper.selectByOrderNo(orderNo);

// 复写：Lambda 表达式
wrapper.eq(ResourcePo::getStatus, status);
mapper.selectOne(new LambdaQueryWrapper<ResourcePo>()
    .eq(ResourcePo::getResourceKey, resourceKey));
```

---

## 5. Maven 依赖生成

```xml
<!-- 从手册第 5 章提取必要依赖 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.x</version>
    <!-- 注意：版本与父 POM 的 Spring Boot 版本兼容 -->
</dependency>
```

---

## 6. aIk-coding-style 强制检查清单

每个类生成后逐项验证：

### PO 实体
- [ ] 无继承：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
- [ ] 有继承：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)`
- [ ] 不写 `@TableField`，字段自动映射
- [ ] 主键策略、逻辑删除、自动填充通过配置或基类处理

### Service
- [ ] 接口：纯接口，不含实现
- [ ] 实现：`@Service("{module}.{ServiceName}")` 格式
- [ ] 注入：`private final` + `@RequiredArgsConstructor`

### Controller（如有）
- [ ] `@RestController` + `@RequestMapping`
- [ ] 参数校验：`@Valid @RequestBody`
- [ ] 返回：`Result<T>` 统一包装

### 通用
- [ ] 类注释：`-anchor` 格式，`@author a I k .`
- [ ] 行注释：`//note` / `//anchor`，禁止行尾注释
- [ ] 禁止字段注入 `@Autowired`
- [ ] 禁止无大括号的 if 语句
- [ ] 日志：`@Slf4j`，关键节点记录
- [ ] 无残留业务词汇（类名、方法名、变量名、注释文本）

---
name: coupling-patterns
description: 常见业务耦合模式识别规则和脱敏建议，供 component-code-analyzer 按需加载
---

# 业务耦合模式识别规则

## 1. 硬编码常量型耦合

### 1.1 字符串常量
```java
// 模式识别
private static final String PREFIX = "ORDER_";     // 业务前缀
private static final String LOCK_KEY = "ORDER:LOCK:"; // 业务Key
String topic = "order_cancel_topic";               // 业务Topic

// 判定标准：包含业务领域词汇（ORDER、PAYMENT、USER等具体业务名）
```

**脱敏建议**：`@ConfigurationProperties` 注入，`${component.prefix}`

### 1.2 数字常量
```java
// 模式识别
private static final int TIMEOUT = 30000;   // 业务超时
private static final int RETRY = 3;         // 业务重试
private static final long CACHE_TTL = 3600; // 业务缓存

// 判定标准：硬编码数字，缺乏语义配置
```

**脱敏建议**：`@Value` 注入，提供合理默认值

---

## 2. 领域命名型耦合

### 2.1 类名
```java
// ❌ 耦合示例（含业务领域词）
OrderLockService
PaymentValidator
UserExportHandler
CustomerAuditLog

// 判定标准：类名含项目特定的业务实体名（Order、Payment、Customer等）
```

**脱敏建议**：通用化命名
```
OrderLockService → GenericLockService / ResourceLockService
PaymentValidator → BusinessValidator / RuleValidator
UserExportHandler → DataExportHandler / EntityExportHandler
```

### 2.2 方法名/变量名
```java
// ❌ 耦合示例
public void lockOrder(String orderNo) { ... }
String paymentId = ...;
private OrderPo orderPo;

// ✅ 通用化后
public void lock(String resourceKey) { ... }
String resourceId = ...;
private T entity;
```

### 2.3 包名
```java
// ❌ 耦合示例
package com.example.order.lock;
package com.example.payment.util;

// ✅ 通用化后
package com.xxx.component.lock;
package com.xxx.component.payment;
```

---

## 3. 数据绑定型耦合

### 3.1 特定表名
```java
// ❌ 耦合示例
@TableName("T_ORDER")
@TableName("order_table")

// 判定标准：MyBatis-Plus @TableName / JPA @Table / SQL硬编码表名
```

**脱敏建议**：
- MyBatis-Plus：默认表名策略，不显式声明
- 必须声明的：抽象为配置 `${component.table-name}`

### 3.2 特定字段名
```java
// ❌ 耦合示例
queryWrapper.eq("order_status", status);
mapper.selectByOrderNo(orderNo);

// 判定标准：硬编码列名/字段名的查询和操作
```

**脱敏建议**：使用 Lambda 表达式（LambdaQueryWrapper）替代字符串列名

### 3.3 特定数据库/Schema
```java
// ❌ 耦合示例
@TableName("order_db.T_ORDER")
jdbc:mysql://.../order_db

// 判定标准：硬编码数据库名
```

---

## 4. 业务校验型耦合

### 4.1 硬编码枚举值
```java
// ❌ 耦合示例
if ("ORDER".equals(type)) { ... }
switch (status) {
    case "PAID": ...
    case "CANCELLED": ...
}

// 判定标准：直接比较字符串/数字的业务枚举值
```

**脱敏建议**：提取为枚举类或策略模式

### 4.2 领域特化校验
```java
// ❌ 耦合示例
if (amount > orderLimit) { ... }
if (userLevel == VIP) { ... }

// 判定标准：包含业务规则的校验逻辑
```

**脱敏建议**：抽取为可配置的校验规则或策略接口

---

## 5. 外部依赖型耦合

### 5.1 内部API/服务
```java
// ❌ 耦合示例
@FeignClient(name = "order-service")
private OrderClient orderClient;

// 判定标准：调用项目内部其他微服务/模块
```

**脱敏建议**：抽象为接口，具体实现由使用者注入

### 5.2 特定配置中心
```java
// ❌ 耦合示例（Nacos特定）
@NacosValue(value = "${order.timeout}")

// 判定标准：绑定特定配置中心（Nacos/Apollo/Consul等）
```

**脱敏建议**：使用标准 `@Value` 或 `@ConfigurationProperties`

### 5.3 特定消息队列 Topic
```java
// ❌ 耦合示例
@RabbitListener(queues = "order.cancel.queue")
kafkaTemplate.send("order_event_topic", msg);

// 判定标准：硬编码特定业务的 MQ Topic/Queue
```

**脱敏建议**：`${component.mq.topic}` 配置化

---

## 6. 敏感数据识别

### 6.1 凭证类
- 数据库密码：`jdbc:mysql://...?password=xxx` / `spring.datasource.password`
- API Key：`api.key=sk-xxx` / `API_KEY = "xxx"`
- Token/Secret：`secretKey = "xxx"` / `accessToken = "xxx"`

### 6.2 网络类
- 内部URL：`http://internal-xxx/api` / `http://10.x.x.x/xxx`
- IP白名单：`192.168.x.x`
- 内部服务地址

### 6.3 密钥类
- 加密密钥：`AES_KEY_XXXX` / `privateKey = "xxx"`
- 签名密钥：`signKey = "xxx"`
- 证书：`cert.pem` 路径硬编码

### 6.4 个人信息（合规）
- 手机号、身份证号、邮箱如果硬编码在代码中
- 注意：这是检测**代码中硬编码**的数据，不是检测数据处理逻辑

---

## 7. 耦合程度判定表

| 耦合程度 | 特征 | 处理优先级 |
|---------|------|-----------|
| **硬耦合** | 硬编码常量、特定表名、领域命名 | 必须脱敏 |
| **紧耦合** | 依赖内部服务、特定配置中心 | 建议抽象 |
| **松耦合** | 可配置参数、接口已抽象 | 可保留 |
| **无耦合** | 纯技术组件（日期工具、JSON工具等） | 无需处理 |

---

## 8. 识别输出示例

```markdown
### 分析结果
- 总耦合点: 8 个
- 硬耦合(必须处理): 5 个
- 紧耦合(建议处理): 2 个
- 松耦合(可保留): 1 个
- 敏感数据: 2 个
- 扩展点: 3 个
```

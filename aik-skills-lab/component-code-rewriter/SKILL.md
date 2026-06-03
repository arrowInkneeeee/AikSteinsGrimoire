---
name: component-code-rewriter
description: 基于组件知识手册，将源项目中有业务耦合的代码复写为脱敏、去业务化、严格遵循 aIk-coding-style 规范的标准化代码。适用于"将此方案复写为通用组件"、"基于手册生成标准化代码"、"代码脱敏重写"、"组件标准化迁移"等场景。需要 code-generator 先生成骨架。
type: Skill
version: 1.0.0
---

# component-code-rewriter

## 职责

基于 Component Manual 中的分析结果（耦合清单、脱敏指导），将源组件业务逻辑复写为脱敏、去业务耦合、100% 符合 aIk-coding-style 的标准化 Java 代码。

## 输入

- Component Manual（由 spec-component-extractor 产出）
- code-generator 生成的规范化代码骨架（PO/DTO/VO/Mapper/Service/Controller）
- 目标项目上下文（包路径、已有通用组件）

## 输出

标准化、可编译运行的 Java 代码，包含：
- PO/DTO/VO/Mapper/Service/Controller 全部类文件
- 类注释含 `-anchor` 标记，`@author a I k .`
- 全部通过 aIk-coding-style 规范检查

## 六大脱敏策略

策略详细指南参考：`references/desensitization-patterns.md`

### 策略一：命名脱敏

```java
// ❌ 源项目（业务耦合命名）
public class OrderLockService {
    public void lockOrder(String orderNo) { ... }
}

// ✅ 复写后（通用化命名）
public class ResourceLockService {
    public void lock(String resourceKey) { ... }
}
```

**规则**：
- 类名：OrderXxx → ResourceXxx / GenericXxx（参考手册第 8 章命名映射表）
- 方法名：lockOrder → lock（去掉业务前缀或后缀）
- 变量名：orderNo → resourceKey
- 包名：com.xxx.order → com.xxx.component.lock

### 策略二：值脱敏

```java
// ❌ 源项目（硬编码常量）
private static final String PREFIX = "ORDER_";
private static final int TIMEOUT = 30;

// ✅ 复写后（配置化）
@Value("${component.lock.prefix}")
private String prefix;

@Value("${component.lock.timeout-seconds:30}")
private int timeoutSeconds;
```

**规则**：
- 所有魔法值替换为 `@Value` 或 `@ConfigurationProperties`
- 提供合理默认值（`:30`）
- 配置键遵循手册第 8 章的配置外化清单

### 策略三：数据脱敏

```java
// ❌ 源项目（绑定特定 PO 和表）
@TableName("T_ORDER")
public class OrderPo extends BaseEntity { ... }
OrderMapper.selectByOrderNo(orderNo);

// ✅ 复写后（泛型化 + Lambda 查询）
@TableName("${component.table-name}")
public class ResourcePo<T extends BaseEntity> extends BaseEntity { ... }
mapper.selectOne(new LambdaQueryWrapper<ResourcePo>().eq(ResourcePo::getResourceKey, resourceKey));
```

**规则**：
- `@TableName` 配置化或使用默认策略
- SQL 查询使用 Lambda 表达式代替字符串列名
- 实体类泛型化以支持不同表

### 策略四：依赖脱敏

```java
// ❌ 源项目（依赖具体类）
private final OrderValidator orderValidator;
private final OrderMapper orderMapper;

// ✅ 复写后（依赖接口 + DI）
private final ResourceValidator validator;
private final BaseMapper<T> mapper;
```

**规则**：
- 依赖具体类 → 依赖接口或抽象
- 内部模块依赖 → 接口 + `@RequiredArgsConstructor` 注入
- 外部服务依赖 → 接口抽象，由使用方实现

### 策略五：凭证脱敏

```java
// ❌ 源项目（硬编码凭证）
private static final String API_KEY = "sk-xxx";
private static final String DB_URL = "jdbc:mysql://10.0.0.1/db?password=123456";

// ✅ 复写后（外部化 + 占位符）
@Value("${component.api.key}")
private String apiKey;
// application-component-xxx.yml 中：component.api.key=${API_KEY}
```

**规则**：
- 所有凭证外部化到配置文件，代码中使用 `@Value` 注入
- 配置文件中使用环境变量占位符 `${ENV_VAR}`
- 敏感字段标注 `//note 生产环境通过配置中心注入`

### 策略六：风格规范化

所有复写代码必须严格遵循 aIk-coding-style 全部规范：

```java
/**
 * -anchor 资源锁服务
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @since 2026/05/14
 * -
 */
@Service("component.lock.ResourceLockService")
@RequiredArgsConstructor
@Slf4j
public class ResourceLockServiceImpl implements ResourceLockService {

    private final StringRedisTemplate redisTemplate;
    private final ResourceLockConfig lockConfig;

    @Override
    public LockResult lock(String resourceKey) {
        //note 尝试获取锁，超时后返回失败
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockConfig.getPrefix() + resourceKey, "1",
                        lockConfig.getTimeoutSeconds(), TimeUnit.SECONDS);
        //anchor 锁获取失败时记录日志并返回
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("acquire lock failed, key={}", resourceKey);
            return LockResult.fail();
        }
        return LockResult.success();
    }
}
```

**检查清单**：
- [ ] 类注释：`-anchor` 格式，`@author a I k .`，`@implNote JDK 8`
- [ ] Service Bean 命名：`@Service("{module}.{ServiceName}")`
- [ ] 依赖注入：`private final` + `@RequiredArgsConstructor`
- [ ] 行注释：`//note` 和 `//anchor`，禁止行尾注释
- [ ] PO 注解：按有无继承选择 `@Builder` 或 `@SuperBuilder`
- [ ] 空指针：Optional 或 @NonNull 保护
- [ ] 日志：@Slf4j，关键节点 log.info/warn
- [ ] 命名：包名/类名/方法名遵循 aIk-coding-style 命名规范

## 工作流

```
接收 Component Manual + 代码骨架
    │
    ▼
1. 读取手册第 8 章：脱敏与复写指导
   - 命名映射表（源命名 → 目标命名）
   - 抽象化策略
   - 配置外化清单
    │
    ▼
2. 逐类复写
   ├── Config 类：@ConfigurationProperties 绑定配置前缀
   ├── PO 类：泛型化、@TableName 配置化
   ├── DTO/VO 类：通用化字段命名
   ├── Mapper 接口：Lambda 查询代替字符串
   ├── Service 接口：去除业务前缀，通用化方法签名
   └── ServiceImpl：替换硬编码为配置注入，去业务校验
    │
    ▼
3. 生成配置片段 (application-{component}.yml)
   - 从手册第 5 章提取配置属性清单
   - 敏感项使用环境变量占位符
    │
    ▼
4. 生成 Maven 依赖补充说明（如有新增依赖）
    │
    ▼
5. 强制 aIk-coding-style 检查（逐类验证）
```

## 强制规范

- 所有代码 100% 遵循 aIk-coding-style，逐类验证
- 耦合清单中的每个耦合点必须被处理
- 敏感数据清单中的每个敏感点必须被脱敏
- 配置项必须有默认值或占位符
- 复写后代码不得包含源项目的任何业务名称（类名/方法名/变量名/注释）

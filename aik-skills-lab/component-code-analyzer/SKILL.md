---
name: component-code-analyzer
description: 对用户手动指定的代码范围进行深度分析——识别可复用架构模式、业务耦合点、敏感数据和依赖关系，输出结构化分析报告以供组件知识萃取使用。适用于"分析这个类/包有哪些可复用逻辑"、"提取组件核心架构"、"识别业务耦合点"等场景。前置条件：源项目已通过 gitnexus analyze 构建索引（由 spec-component-extractor 自动检查）。
type: Skill
version: 1.0.0
---

# component-code-analyzer

## 职责

对指定代码范围进行 6 维度深度分析，识别可复用架构、业务耦合点、敏感数据和依赖关系，输出结构化分析报告，为 Component Manual 的生成提供数据支撑。

## 输入

- 用户指定的代码范围（文件路径/包名/类名/方法名列表）
- gitnexus 返回的依赖图和调用链（由 `gitnexus query` + `gitnexus context` + `gitnexus cypher` 提供）
- 相关源文件全文（通过 Read 工具读取）

## 输出

结构化代码分析报告，包含以下 6 个维度：

```markdown
## 代码分析报告

### 1. 架构概述
- 设计模式: [Singleton / Strategy / Template Method / Factory / ...]
- 分层位置: Controller / Service / ServiceImpl / Mapper / Utils / Config
- 核心类: [class1, class2, ...]
- 扩展点: [interface1, method1, ...]（哪些地方设计了抽象接口可供扩展）

### 2. 分层映射
| 层 | 类名 | 职责 | 是否核心 |
|----|------|------|---------|
| Config | XxConfig | 配置注入，外部化参数 | 是 |
| Service | XxService | 核心服务接口 | 是 |
| ... | ... | ... | ... |

### 3. 业务耦合清单
| ID | 位置 (类:方法:行号) | 耦合类型 | 当前值 | 影响范围 | 脱敏建议 |
|----|-------------------|---------|--------|---------|---------|
| C01 | XxConfig.java:15 | 硬编码常量 | "ORDER_" | 全局前缀 | 改为 ${component.prefix} |
| C02 | XxServiceImpl.java:42 | 领域命名 | orderNo | 方法参数 | 改为 resourceKey |
| C03 | XxServiceImpl.java:88 | 特定表名 | T_ORDER | 数据查询 | 抽象为接口或泛型 |
| C04 | XxValidator.java:30 | 业务校验 | if ("VIP".equals(type)) | 校验逻辑 | 提取为策略模式参数化 |

### 4. 敏感数据清单
| ID | 位置 | 敏感类型 | 当前值 | 处理方式 |
|----|------|---------|--------|---------|
| S01 | config.java:10 | 数据库密码 | jdbc:mysql://... | 外部化配置 ${DB_PASSWORD} |
| S02 | XxService.java:88 | 内部URL | http://internal/api | 外部化+占位符 |
| S03 | XxUtil.java:42 | 加密密钥 | AES_KEY_XXXX | 配置中心/环境变量 |

### 5. 依赖清单
| 类型 | 名称 | 版本 | 必要性 | 说明 |
|------|------|------|--------|------|
| Maven | redisson | 3.x | 必需 | 分布式锁核心 |
| Maven | poi-ooxml | 5.x | 必需 | Excel读写 |
| 内部 | BaseEntity | - | 可移除 | 继承改为组合 |
| 内部 | RedisConfig | - | 可抽象 | 使用 StringRedisTemplate 接口 |

### 6. 扩展点分析
| 接口/抽象类 | 方法 | 用途 | 是否必须实现 | 扩展建议 |
|------------|------|------|-------------|---------|
| XxHandler | handle(T) | 业务处理器 | 是 - 策略模式 | 保持接口，泛型化 |
| XxCallback | onSuccess() | 成功回调 | 否 - 可选 | 保持，去业务化命名 |
```

## 工作流

```
接收代码范围 + gitnexus 分析数据
    │
    ▼
1. 识别架构模式
   - 扫描类结构：检查是否使用了 Singleton、Strategy、Factory、Template Method 等模式
   - 识别分层归属：Controller/Service/Mapper/Utils/Config
   - 检测抽象层：接口、抽象类、扩展点
    │
    ▼
2. 分析业务耦合
   - 硬编码常量：字符串、数字等魔法值 → 建议配置化
   - 领域命名：类名/方法名/变量名含业务词汇 → 建议通用化
   - 特定表/字段：直接引用具体表名 → 建议抽象化
   - 业务特有校验：硬编码的判断逻辑 → 建议策略参数化
   - 检查类型参考：references/coupling-patterns.md
    │
    ▼
3. 识别敏感数据
   - 凭证类：密码、密钥、Token
   - 网络类：内部URL、IP白名单
   - 环境类：特定环境标识
    │
    ▼
4. 分析依赖关系（基于 gitnexus 调用图）
   - 外部依赖：pom.xml 中的 Maven 依赖（由 tech-solution-selector 辅助）
   - 内部依赖：被哪些类调用 → 调用哪些类
   - 配置依赖：依赖的 application.yml 配置项
    │
    ▼
5. 输出结构化分析报告
   - 每个耦合点标注位置、类型、影响范围、脱敏建议
   - 每个敏感数据标注处理方式
   - 架构和扩展点标注清晰
```

## 耦合类型识别规则

详细规则参考：`references/coupling-patterns.md`

### 硬编码常量
```java
// ❌ 硬编码
String prefix = "ORDER_";
int timeout = 30;

// ✅ 可配置化建议
// @ConfigurationProperties(prefix = "component.lock")
// private String prefix = "ORDER_";
```

### 领域命名
```java
// ❌ 领域命名 → 通用化建议
orderNo → resourceKey
OrderPo → GenericEntity
T_ORDER → ${component.table-name}
```

### 业务校验
```java
// ❌ 硬编码业务逻辑 → 策略参数化建议
if ("VIP".equals(type)) { ... }

// ✅ 参数化
processor.process(type, param);
```

### 特定表/字段
```java
// ❌ 直接引用具体表名
@TableName("T_ORDER")

// ✅ 抽象化建议
// 通过接口/泛型/配置化解耦表名
```

## 与 gitnexus 协作

分析前必须通过 gitnexus 获取完整的代码关系图：
- `gitnexus query`：按业务概念搜索相关流程
- `gitnexus context`：获取核心符号的调用者/被调用者/所属流程
- `gitnexus cypher`：追踪完整调用链，识别间接依赖

## 强制规范

- 所有分析必须基于实际代码，不猜测
- 耦合点标注必须精确到方法级别
- 脱敏建议必须具体可操作
- 敏感数据检测零容忍

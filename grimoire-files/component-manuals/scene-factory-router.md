# Component Manual v1.0 — 场景服务工厂路由组件

## 类型标记

**COMPONENT** — 可复用组件，包含完整工厂+模板方法模式实现

---

## 第1章：组件概述

### 1.1 组件名称
场景服务工厂路由组件（Scene Service Factory Router）

### 1.2 组件用途
基于输入文本的智能场景识别与路由分发组件。通过**工厂模式**自动收集并管理所有场景处理器，根据输入文本匹配对应场景，并将请求路由到具体的场景服务进行解析。适用于需要动态扩展场景类型的语音/文本指令解析系统。

### 1.3 适用场景
- 多场景语音/文本指令解析
- 需要动态扩展场景类型的系统
- 相似度匹配驱动的智能路由
- 多层降级兜底（定向路由→遍历匹配→意图分析）

### 1.4 核心能力
| 能力 | 说明 |
|------|------|
| 自动发现 | Spring 自动注入所有 AbstractSceneService 子类 |
| 多层路由 | 定向路由 → 遍历匹配 → 意图分析兜底 |
| 模板方法 | 统一的解析流程：本地解析 → 完整性检查 → 意图兜底 |
| 相似度匹配 | 综合 Levenshtein + Jaccard 相似度算法 |
| 关键词解析 | 基于配置化的关键词分类进行参数提取 |

---

## 第2章：设计模式与架构

### 2.1 设计模式组合

```
┌─────────────────────────────────────────────────────────────┐
│                    工厂模式 (Factory)                         │
│  SceneServiceFactory ──收集所有 AbstractSceneService          │
│         │                                                    │
│         ▼                                                    │
│  ┌─────────────────┐    ┌─────────────────┐                 │
│  │  matchScene()   │ or │ 定向路由(sceneType)│               │
│  └────────┬────────┘    └────────┬────────┘                 │
│           │                      │                           │
│           ▼                      ▼                           │
│  ┌─────────────────────────────────────────┐                │
│  │         模板方法模式 (Template Method)     │                │
│  │  AbstractSceneService<T>                │                │
│  │  ├── parse() [模板方法]                  │                │
│  │  ├── doParse() [抽象]                   │                │
│  │  ├── isParamsComplete() [抽象]          │                │
│  │  └── createEmptyResult() [抽象]         │                │
│  └─────────────────────────────────────────┘                │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────────────────────────────┐                │
│  │         策略模式 (Strategy)               │                │
│  │  MonitorVideoService                    │                │
│  │  OperationBoardService                  │                │
│  │  PersonTrackService                     │                │
│  │  [新增场景...]                          │                │
│  └─────────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 架构分层

| 层级 | 类/接口 | 职责 |
|------|---------|------|
| 入口层 | SceneToolController | REST API 入口，接收解析请求 |
| 工厂层 | SceneServiceFactory | 路由分发，场景匹配 |
| 抽象层 | AbstractSceneService<T> | 模板方法定义，通用逻辑封装 |
| 实现层 | *SceneService | 具体场景解析逻辑 |
| 数据层 | *Result VO | 场景解析结果定义 |

### 2.3 请求流转

```
请求 → Controller → Factory.parse(text/sceneType)
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         定向路由      遍历匹配      意图分析兜底
              │            │            │
              ▼            ▼            ▼
         指定Service   matchScene()  IntentionApiService
              │            │
              └────────────┘
                     │
                     ▼
            AbstractSceneService.parse()
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
      doParse()  完整性检查   意图分析兜底
                     │
         ┌───────────┴───────────┐
         ▼                       ▼
     参数完整                 参数不完整
         │                       │
         ▼                       ▼
     返回结果              intentionApi.recognize()
                                    │
                              ┌─────┴─────┐
                              ▼           ▼
                          识别成功     识别失败
                              │           │
                              ▼           ▼
                          返回结果     返回空结果
```

---

## 第3章：核心类与接口

### 3.1 SceneServiceFactory（工厂类）

```java
@Service("scene.SceneServiceFactory")
public class SceneServiceFactory {
    @Resource
    private List<AbstractSceneService<?>> sceneServices;  // 自动收集所有实现

    @Resource
    private IntentionApiService intentionApiService;

    // 两层路由入口
    public Object parse(String text)           // 遍历匹配 + 意图兜底
    public Object parse(String text, String sceneType)  // 定向路由 + 降级
}
```

**关键特性**：
- 利用 Spring `List<AbstractSceneService<?>>` 自动收集所有 `@Component` 子类
- 双层解析方法：无 sceneType 时遍历匹配，有 sceneType 时定向路由
- 匹配失败时调用意图分析服务兜底

### 3.2 AbstractSceneService<T>（抽象模板类）

```java
public abstract class AbstractSceneService<T> {
    // ========== 子类必须实现的抽象方法 ==========
    public abstract String getSceneType();           // 场景类型标识
    public abstract String getSceneName();           // 场景名称
    public abstract Class<T> getResultClass();       // 结果类型（用于JSON转换）
    protected abstract String getCommandType();      // 指令类型（用于匹配）
    protected abstract String getKeywordCategory();  // 关键词分类（用于参数解析）
    public abstract boolean matchScene(String text); // 场景匹配判断
    protected abstract T doParse(String text);       // 参数解析逻辑
    protected abstract boolean isParamsComplete(T result);  // 完整性检查
    protected abstract T createEmptyResult();        // 创建空结果

    // ========== 模板方法（不可覆盖） ==========
    public final T parse(String text) {
        T result = doParse(text);                    // 1. 本地解析
        if (isParamsComplete(result)) return result; // 2. 完整性检查
        T intentionResult = intentionApiService.recognize(text, getSceneType(), getResultClass());
        return intentionResult != null ? intentionResult : result;  // 3. 兜底
    }

    // ========== 通用工具方法（可复用） ==========
    protected boolean matchSceneByCommand(String text)     // 指令名称匹配
    protected boolean matchSceneBySynonym(String text)     // 同义词兜底匹配
    protected String parseParamByKeyword(String text)      // 关键词解析参数
    protected String parseParamByCategory(String text, String categoryCode)  // 指定分类解析
    protected double calculateSimilarity(String text1, String text2)  // 综合相似度计算
}
```

**模板方法流程**：
1. `doParse(text)` — 子类实现具体参数解析
2. `isParamsComplete(result)` — 检查参数是否完整
3. 完整 → 直接返回；不完整 → 调用意图分析兜底

### 3.3 具体实现类示例

每个具体服务只需关注：
- 场景元数据（type、name、commandType、keywordCategory）
- 场景匹配逻辑（通常复用 `matchSceneByCommand` + `matchSceneBySynonym`）
- 参数解析逻辑（`doParse`）
- 完整性判断（`isParamsComplete`）
- 兜底解析（私有方法）

---

## 第4章：依赖关系

### 4.1 内部依赖

| 依赖模块 | 用途 | 耦合程度 |
|---------|------|---------|
| command（指令管理） | 指令名称匹配、同义词匹配、关键词管理 | 强耦合 |
| support（通用支撑） | 意图分析API、系统参数、相似度工具 | 中等耦合 |

### 4.2 外部依赖

| 依赖 | 用途 |
|------|------|
| Spring Framework | 依赖注入、@Component/@Service |
| Lombok | @Slf4j、@Data |
| Swagger | API 文档注解 |

### 4.3 依赖图

```
SceneToolController
    └── SceneServiceFactory
            ├── List<AbstractSceneService<?>> (自动注入)
            │       ├── MonitorVideoService
            │       ├── OperationBoardService
            │       └── PersonTrackService
            └── IntentionApiService

AbstractSceneService
    ├── IntentionApiService
    ├── VoiceCommandService (command模块)
    ├── CommandSynonymService (command模块)
    ├── KeywordItemService (command模块)
    ├── KeywordCategoryService (command模块)
    └── SimilarityUtil (support模块)
```

---

## 第5章：使用示例

### 5.1 Controller 调用

```java
@RestController
@RequestMapping("/aih/scene")
public class SceneToolController {
    @Resource
    private SceneServiceFactory sceneServiceFactory;

    @PostMapping("/parse")
    public ApiResponse<Object> parse(@RequestBody @Valid ParseRequestDto dto) {
        Object result = sceneServiceFactory.parse(dto.getText());
        return result != null ? ApiResponse.success(result) : ApiResponse.fail("未匹配到有效场景");
    }
}
```

### 5.2 新增场景步骤

1. **创建结果 VO 类**
```java
@Data
public class NewSceneResult {
    private String sceneKey;
    private String sceneName;
    // 新增场景特有字段...
}
```

2. **创建场景服务类**
```java
@Component
public class NewSceneService extends AbstractSceneService<NewSceneResult> {
    @Override
    public String getSceneType() { return "NEW_SCENE"; }
    
    @Override
    public String getSceneName() { return "新场景"; }
    
    @Override
    public Class<NewSceneResult> getResultClass() { return NewSceneResult.class; }
    
    @Override
    protected String getCommandType() { return CommandConstants.COMMAND_TYPE_SCENE; }
    
    @Override
    protected String getKeywordCategory() { return "NEW_CATEGORY"; }
    
    @Override
    public boolean matchScene(String text) {
        return matchSceneByCommand(text) || matchSceneBySynonym(text);
    }
    
    @Override
    protected NewSceneResult doParse(String text) {
        NewSceneResult result = new NewSceneResult();
        result.setSceneKey(getSceneType());
        result.setSceneName(getSceneName());
        // 参数解析逻辑...
        return result;
    }
    
    @Override
    protected boolean isParamsComplete(NewSceneResult result) {
        // 完整性判断...
        return true;
    }
    
    @Override
    protected NewSceneResult createEmptyResult() {
        NewSceneResult result = new NewSceneResult();
        result.setSceneKey(getSceneType());
        result.setSceneName(getSceneName());
        return result;
    }
}
```

3. **注册常量**（添加到 SceneConstants）
```java
public static final String SCENE_NEW_SCENE = "NEW_SCENE";
```

4. **无需修改工厂类** — Spring 自动注入新服务

---

## 第6章：扩展指南

### 6.1 扩展点

| 扩展点 | 方式 | 说明 |
|--------|------|------|
| 新增场景 | 继承 AbstractSceneService | 零侵入，自动注册 |
| 自定义匹配逻辑 | 覆盖 matchScene() | 默认复用指令+同义词匹配 |
| 自定义参数解析 | 覆盖 doParse() | 可复用 parseParamByKeyword/Category |
| 自定义相似度 | 覆盖 calculateSimilarity() | 默认 Levenshtein(0.6) + Jaccard(0.4) |
| 新增关键词分类 | 配置到 command 模块 | 无需修改场景代码 |

### 6.2 注意事项

- **泛型约束**：每个服务必须指定具体 Result 类型，用于意图分析时的 JSON 反序列化
- **场景类型唯一性**：getSceneType() 返回值必须全局唯一
- **@Component 注解**：必须使用 `@Component` 而非 `@Service`，确保工厂能自动收集
- **线程安全**：工厂和模板类无状态，线程安全

---

## 第7章：配置说明

### 7.1 常量配置

```java
public class SceneConstants {
    public static final String SCENE_MONITOR_VIDEO = "MONITOR_VIDEO";
    public static final String SCENE_PERSON_TRACK = "PERSON_TRACK";
    public static final String SCENE_OPERATION_BOARD = "OPERATION_BOARD";
}
```

### 7.2 相似度阈值（CommandConstants）

```java
public class CommandConstants {
    public static final double SIMILARITY_EXACT = 1.0;   // 精确匹配
    public static final double SIMILARITY_HIGH = 0.85;   // 高相似度
    public static final double SIMILARITY_MEDIUM = 0.70; // 中等相似度（默认阈值）
    public static final double SIMILARITY_MIN = 0.50;    // 最低相似度
}
```

### 7.3 相似度权重（AbstractSceneService）

```java
// 综合评分：编辑距离权重 0.6，Jaccard 权重 0.4
return levenshteinSim * 0.6 + jaccardSim * 0.4;
```

---

## 第8章：性能考量

### 8.1 性能特征

| 操作 | 时间复杂度 | 说明 |
|------|-----------|------|
| 定向路由 | O(1) | 通过 sceneType 直接查找 |
| 遍历匹配 | O(N × M) | N=场景数，M=同义词/关键词数 |
| 相似度计算 | O(L²) | L=文本长度，DP 算法 |
| 关键词匹配 | O(K × L²) | K=关键词数量 |

### 8.2 优化建议

- **缓存同义词和关键词**：避免每次请求查询数据库
- **预过滤**：在相似度计算前进行简单的字符串包含检查
- **并行匹配**：场景数量大时可考虑并行流 `parallelStream()`
- **意图分析异步化**：兜底调用可改为异步，超时返回空结果

---

## 第9章：测试策略

### 9.1 单元测试重点

| 测试对象 | 测试内容 |
|---------|---------|
| SceneServiceFactory | 定向路由正确性、遍历匹配顺序、空输入处理 |
| AbstractSceneService | 模板方法流程（完整/不完整参数）、相似度计算 |
| 具体 Service | matchScene 准确性、doParse 参数提取、兜底逻辑 |

### 9.2 集成测试重点

- Spring 上下文加载时是否正确收集所有 SceneService
- 新增场景后无需修改工厂即可生效
- 与 command 模块的指令/关键词/同义词联动

### 9.3 Mock 建议

- `IntentionApiService` — 外部 API，必须 Mock
- `VoiceCommandService` — 依赖数据库，建议 Mock
- `CommandSynonymService` — 依赖数据库，建议 Mock
- `KeywordItemService` / `KeywordCategoryService` — 依赖数据库，建议 Mock

---

## 第10章：业务耦合点与脱敏说明

### 10.1 业务耦合点清单

| 位置 | 业务耦合内容 | 脱敏方式 |
|------|------------|---------|
| SceneConstants | 具体场景类型标识 | 保留作为示例 |
| MonitorVideoService | 设备名称格式（302-色谱分析室）、楼层提取逻辑 | 替换为通用示例 |
| OperationBoardService | 城市名称（南京、江苏）、专业名称（金属、水分析、油分析） | 替换为占位符 |
| PersonTrackService | 人员姓名（张三、李四、王五） | 替换为占位符 |
| 各 Service 兜底方法 | 硬编码的业务数据 | 替换为通用解析示例 |

### 10.2 敏感数据

- 具体城市名称、人员姓名、设备名称、专业名称等硬编码数据
- 房间号提取规则（特定业务格式）

### 10.3 通用化后的核心模式

脱敏后保留的纯技术模式：
1. **工厂自动收集模式** — Spring List 注入 + stream 匹配
2. **模板方法 + 策略模式** — 抽象类定义流程，子类实现细节
3. **多层降级模式** — 定向路由 → 遍历匹配 → 外部兜底
4. **相似度匹配模式** — 多算法综合评分 + 阈值过滤
5. **关键词参数解析模式** — 分类配置化 + 相似度匹配

---

## 附录：文件清单

| 文件 | 类型 | 说明 |
|------|------|------|
| SceneServiceFactory.java | 工厂类 | 路由分发核心 |
| AbstractSceneService.java | 抽象模板 | 模板方法定义 |
| MonitorVideoService.java | 实现类 | 示例：监控视频场景 |
| OperationBoardService.java | 实现类 | 示例：运行看板场景 |
| PersonTrackService.java | 实现类 | 示例：人员轨迹场景 |
| SceneToolController.java | 控制器 | REST API 入口 |
| SceneConstants.java | 常量 | 场景类型定义 |
| ParseRequestDto.java | DTO | 请求参数 |
| *Result.java | VO | 场景结果（3个） |

---

*手册版本：v1.0*
*生成日期：2026-05-18*
*类型标记：COMPONENT*

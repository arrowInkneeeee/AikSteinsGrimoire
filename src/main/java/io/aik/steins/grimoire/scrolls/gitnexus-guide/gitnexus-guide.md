# GitNexus 学习笔记与使用指南

> 代码知识图谱工具：索引、查询、分析与影响评估

---

## 一、简介

GitNexus 是一套基于**代码知识图谱（Knowledge Graph）**的代码智能工具集，通过构建仓库的图模型（节点 = 代码符号，边 = 关系），实现超越文本搜索的语义级代码理解与分析。
    
**核心能力**：
- 全库语义索引（非纯文本，基于 AST + 向量）
- 自然语言查询代码执行流
- 360° 代码符号上下文
- 变更影响半径分析
- 跨文件协调重命名
- API 路由与消费方映射

---

## 二、核心概念

### 2.1 图谱节点类型（Node Types）

| 节点类型 | 含义 |
|---------|------|
| `File` | 源文件 |
| `Folder` | 目录 |
| `Function` | 函数 |
| `Class` | 类 |
| `Interface` | 接口 |
| `Method` | 方法 |
| `CodeElement` | 通用代码元素 |
| `Community` | 功能社区（算法自动聚类）|
| `Process` | 执行流程（从入口到终点的调用链）|
| `Route` | API 路由 |
| `Tool` | MCP/RPC 工具定义 |

**多语言节点**：`Struct`, `Enum`, `Trait`, `Impl` 等（需用反引号包裹）。

### 2.2 图谱边类型（Edge Types）

所有边存储在统一的 `CodeRelation` 表中，通过 `type` 属性区分：

| 边类型 | 含义 |
|--------|------|
| `CONTAINS` | 包含关系 |
| `DEFINES` | 定义 |
| `CALLS` | 调用 |
| `IMPORTS` | 导入 |
| `EXTENDS` | 继承 |
| `IMPLEMENTS` | 实现 |
| `HAS_METHOD` | 拥有方法 |
| `HAS_PROPERTY` | 拥有属性 |
| `ACCESSES` | 访问字段（含 `reason: read/write`）|
| `METHOD_OVERRIDES` | 方法覆盖 |
| `METHOD_IMPLEMENTS` | 方法实现 |
| `MEMBER_OF` | 属于社区 |
| `STEP_IN_PROCESS` | 流程中的步骤 |
| `HANDLES_ROUTE` | 处理路由 |
| `FETCHES` | 获取数据 |
| `HANDLES_TOOL` | 处理工具 |
| `ENTRY_POINT_OF` | 流程入口 |

### 2.3 Community（功能社区）

- 通过 **Leiden 算法**自动检测的功能区域
- 属性：`heuristicLabel`（人工可读标签）、`cohesion`（内聚度）、`symbolCount`、`keywords`、`description`
- 查询时使用 `heuristicLabel` 而非 `label`

### 2.4 Process（执行流程）

- 从入口点到终点的完整调用链
- 属性：`heuristicLabel`、`processType`、`stepCount`、`communities`、`entryPointId`、`terminalId`
- 可通过 `STEP_IN_PROCESS` 边按 `step` 排序追踪

---

## 三、环境准备

### 3.1 安装

GitNexus 以 `npx` 方式运行，无需全局安装：

```bash
npx gitnexus --version
```

### 3.2 首次索引（必须）

```bash
# 进入仓库根目录
npx gitnexus analyze
```

索引完成后，会在仓库根目录生成 `.gitnexus/` 文件夹（需加入 `.gitignore`）。

### 3.3 常用状态检查

```bash
npx gitnexus status        # 查看当前仓库索引状态
npx gitnexus list          # 列出所有已索引的仓库
npx gitnexus doctor        # 检查运行时平台能力与嵌入模型配置
```

### 3.4 清理索引

```bash
npx gitnexus clean         # 删除当前仓库索引
npx gitnexus remove <target>  # 删除指定仓库索引（按别名/名称/绝对路径）
```

---

## 四、CLI 命令速查表

| 命令 | 用途 | 典型示例 |
|------|------|---------|
| `analyze [path]` | 全量索引仓库 | `npx gitnexus analyze` |
| `status` | 查看索引状态 | `npx gitnexus status` |
| `list` | 列出所有已索引仓库 | `npx gitnexus list` |
| `query <query>` | 自然语言搜索执行流 | `npx gitnexus query "用户登录流程"` |
| `context [name]` | 符号 360° 视图 | `npx gitnexus context KnowledgeController` |
| `cypher <query>` | 原始 Cypher 查询 | `npx gitnexus cypher "MATCH (n) RETURN n LIMIT 10"` |
| `impact <target>` | 变更影响分析 | `npx gitnexus impact "KnowledgeService.add"` |
| `detect-changes` | 分析未提交变更的影响 | `npx gitnexus detect-changes` |
| `rename` | 跨文件协调重命名 | `npx gitnexus rename --dry-run` |
| `serve` | 启动 Web UI | `npx gitnexus serve` |
| `wiki` | 生成仓库 Wiki | `npx gitnexus wiki` |
| `augment <pattern>` | 增强搜索模式 | `npx gitnexus augment "auth"` |
| `group` | 管理仓库组 | `npx gitnexus group` |

---

## 五、MCP 工具详解

GitNexus 同时提供 MCP Server，支持通过 IDE/AI 助手直接调用。

### 5.1 查询类工具

#### `query` — 执行流搜索

- **用途**：用自然语言搜索代码执行流，返回按相关性排序的 Process
- **排名机制**：BM25 关键词 + 语义向量，RRF 融合排序
- **关键参数**：
  - `query`：搜索词（自然语言或关键词）
  - `goal`：想找什么（帮助排序）
  - `task_context`：当前任务上下文（帮助排序）
  - `limit`：最大返回 Process 数（默认 5）
  - `max_symbols`：每个 Process 最大符号数（默认 10）
  - `include_content`：是否包含完整源码
- **使用时机**：理解代码如何协作、找执行流而非文件匹配

#### `context` — 360° 符号视图

- **用途**：查看单个符号的全景信息（调用者、被调用者、引用位置、所属流程）
- **关键参数**：
  - `name`：符号名（如 `validateUser`, `AuthService`）
  - `uid`：直接符号 UID（零歧义查找）
  - `file_path`：文件路径（消歧）
  - `kind`：类型过滤（`Function`, `Class`, `Method`, `Interface`）
  - `include_content`：是否包含源码
- **使用时机**：query 之后深入理解某个具体符号；知道所有调用者和被调用者
- **歧义处理**：同名符号返回排序候选列表，可用 `uid` 或 `file_path` 精确指定

#### `cypher` — 原始图查询

- **用途**：执行 Cypher 查询语言直接操作知识图谱
- **返回**：Markdown 表格 + `row_count`
- **使用时机**：复杂结构查询，query/context 无法回答时
- **关键提示**：
  - 所有关系使用 `CodeRelation` 单表，过滤条件 `{type: 'CALLS'}`
  - 查询 Community/Process 时使用 `heuristicLabel`

### 5.2 分析类工具

#### `impact` — 变更影响半径

- **用途**：分析修改某个符号会波及多大范围
- **输出**：
  - `risk`: LOW / MEDIUM / HIGH / CRITICAL
  - `summary`: 直接调用者、受影响流程、受影响模块
  - `byDepth`: 按遍历深度分组
    - **d=1**: WILL BREAK（直接调用者/导入者）
    - **d=2**: LIKELY AFFECTED（间接影响）
    - **d=3**: MAY NEED TESTING（传递影响）
- **关键参数**：
  - `target`：目标符号名
  - `direction`: `upstream`（谁依赖我）/ `downstream`（我依赖谁）
  - `maxDepth`: 最大深度（默认 3，范围 1-32）
  - `relationTypes`: 边类型过滤（默认不含 `ACCESSES`）
  - `includeTests`: 是否包含测试文件
  - `crossDepth`: 跨仓库跳转深度（monorepo 场景）
- **使用时机**：重构、重命名、修改共享代码前

#### `detect_changes` — 变更影响检测

- **用途**：将 git diff 与知识图谱映射，找出受影响的执行流程
- **关键参数**：
  - `scope`: `unstaged`（默认）/ `staged` / `all` / `compare`
  - `base_ref`: 对比基准分支/提交（`compare` 模式必需）
- **使用时机**：提交前审查、PR 准备

#### `api_impact` — API 变更预检

- **用途**：修改 API 路由前，查看消费方依赖、响应字段访问、中间件保护、触发流程
- **风险等级**：
  - LOW：0-3 个消费者
  - MEDIUM：4-9 个消费者 或 存在字段不匹配
  - HIGH：10+ 消费者 或 与 4+ 消费者存在不匹配
- **关键参数**：
  - `route`: 路由路径（如 `/api/grants`）
  - `file`: Handler 文件路径（route 的替代参数）

#### `shape_check` — API 响应形状校验

- **用途**：检测 API 路由返回的响应字段与消费者实际访问的字段是否一致
- **返回**：MISMATCH 状态当消费者访问了路由不存在的字段
- **使用时机**：发现 shape drift（形状漂移）

#### `route_map` — API 路由映射

- **用途**：查看哪些组件/钩子消费了哪些 API 端点，以及哪些 Handler 文件服务它们
- **返回**：路由节点 + Handler + 中间件包装链（如 `withAuth`, `withRateLimit`）+ 消费者
- **使用时机**：理解 API 消费模式、发现孤儿路由

### 5.3 重构类工具

#### `rename` — 跨文件协调重命名

- **用途**：基于知识图谱 + 文本搜索的跨文件重命名
- **编辑标记**：
  - `graph`：通过图谱关系发现（高置信度，可安全接受）
  - `text_search`：通过正则文本搜索发现（低置信度，需人工审查）
- **关键参数**：
  - `symbol_name` / `symbol_uid`：要重命名的符号
  - `new_name`：新名称
  - `dry_run`: `true`（默认，仅预览不修改）
- **使用时机**：比 find-and-replace 更安全的重命名
- **后续操作**：执行后用 `detect_changes` 验证副作用

### 5.4 管理类工具

#### `list_repos` — 列出已索引仓库

- **用途**：发现可用仓库
- **返回**：名称、路径、索引日期、最后提交、统计信息

#### `group_list` / `group_sync` — 仓库组管理

- **用途**：跨仓库（monorepo 或多仓库）影响分析
- `group_list`：列出所有配置的仓库组
- `group_sync`：重建 Contract Registry（`contracts.json`）

### 5.5 专用工具

#### `tool_map` — MCP/RPC 工具映射

- **用途**：查看项目内定义了哪些 MCP/RPC 工具、在哪些文件中处理、描述是什么

---

## 六、Cypher 查询指南

### 6.1 常用查询模板

```cypher
-- 查找某函数的所有调用者
MATCH (a)-[:CodeRelation {type: 'CALLS'}]->(b:Function {name: "validateUser"})
RETURN a.name, a.filePath

-- 查找社区成员
MATCH (f)-[:CodeRelation {type: 'MEMBER_OF'}]->(c:Community)
WHERE c.heuristicLabel = "Auth"
RETURN f.name

-- 追踪执行流程（按步骤排序）
MATCH (s)-[r:CodeRelation {type: 'STEP_IN_PROCESS'}]->(p:Process)
WHERE p.heuristicLabel = "UserLogin"
RETURN s.name, r.step
ORDER BY r.step

-- 查找类的所有方法
MATCH (c:Class {name: "UserService"})-[r:CodeRelation {type: 'HAS_METHOD'}]->(m:Method)
RETURN m.name, m.parameterCount, m.returnType

-- 查找类的所有属性
MATCH (c:Class {name: "User"})-[r:CodeRelation {type: 'HAS_PROPERTY'}]->(p:Property)
RETURN p.name, p.declaredType

-- 查找字段的所有写入者
MATCH (f:Function)-[r:CodeRelation {type: 'ACCESSES', reason: 'write'}]->(p:Property)
WHERE p.name = "address"
RETURN f.name, f.filePath

-- 查找方法覆盖（MRO 决议）
MATCH (winner:Method)-[r:CodeRelation {type: 'METHOD_OVERRIDES'}]->(loser:Method)
RETURN winner.name, winner.filePath, loser.filePath, r.reason

-- 检测菱形继承
MATCH (d:Class)-[:CodeRelation {type: 'EXTENDS'}]->(b1),
      (d)-[:CodeRelation {type: 'EXTENDS'}]->(b2),
      (b1)-[:CodeRelation {type: 'EXTENDS'}]->(a),
      (b2)-[:CodeRelation {type: 'EXTENDS'}]->(a)
WHERE b1 <> b2
RETURN d.name, b1.name, b2.name, a.name
```

### 6.2 查询技巧

1. **统一关系表**：所有关系都走 `CodeRelation`，务必加 `{type: '...'}` 过滤
2. **标签使用**：Community/Process 使用 `heuristicLabel` 而非 `label`
3. **消歧**：同名符号可通过 `filePath` 或 `uid` 精确指定

---

## 七、典型使用场景

### 场景 1：快速理解陌生代码
```bash
npx gitnexus query "用户认证流程"
npx gitnexus context AuthService
```

### 场景 2：重构前安全评估
```bash
npx gitnexus impact "OrderService.cancel" --direction upstream
npx gitnexus rename --symbol_name "cancel" --new_name "revoke" --dry-run true
```

### 场景 3：提交前自检
```bash
npx gitnexus detect-changes --scope unstaged
```

### 场景 4：API 改造预检
```bash
npx gitnexus api_impact --route "/api/orders"
npx gitnexus shape_check --route "/api/orders"
```

### 场景 5：跨仓库影响分析（Monorepo）
```bash
npx gitnexus impact "sharedUtils.formatDate" --direction upstream --crossDepth 2
```

---

## 八、工作流建议

### 日常开发循环

```
1. 编码前：query/context 理解相关代码
2. 编码中：按需查询符号上下文
3. 提交前：detect_changes 检查影响范围
4. 重构前：impact 评估风险
5. 重命名：rename（dry-run 先预览）
```

### 与 AI 助手协作（MCP 模式）

当通过 MCP Server 调用时，工具调用链推荐：

```
list_repos        → 发现仓库
  → query         → 搜索执行流
    → context     → 深入具体符号
      → impact    → 评估修改风险
        → rename  → 执行重构（dry-run 先）
          → detect_changes → 验证变更
```

---

## 九、注意事项

1. **索引是前提**：任何查询/分析前必须先 `npx gitnexus analyze`
2. **仓库路径**：多仓库场景下，所有工具调用需指定 `repo` 参数
3. **Group 模式**：`repo` 参数以 `@` 开头时进入跨仓库模式
4. **Monorepo**：可用 `service` 参数限定子路径前缀
5. **置信度**：`impact` 中 confidence < 0.8 为模糊匹配，需人工确认
6. **忽略文件**：`.gitnexus/` 目录应加入 `.gitignore`

---

## 十、参考链接

- 源 URL：`http://192.168.16.8:5173/skills/product-69cf97cce4b00f9ef3634918`
- CLI 帮助：`npx gitnexus --help`
- 各子命令帮助：`npx gitnexus help <command>`

---

> 整理时间：2026-05-18
> 基于 GitNexus CLI Help + MCP Tool Schema 整理

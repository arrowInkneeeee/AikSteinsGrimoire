---
name: gitnexus-getting-started
description: Complete getting started guide for GitNexus: installation, real-world scenarios, skill usage, performance tips, and best practices. Use when the user is new to GitNexus or wants comprehensive usage guidance.
---

# GitNexus 完全入门指南

从安装到精通：搭建 → 场景 → 技能使用 → 性能优化 → 最佳实践

## 📋 前置要求

### 系统要求

| 组件 | 最低版本 | 说明 |
|-----|---------|------|
| **Node.js** | v20.17.0+ | ⚠️ 必须，版本过低会失败 |
| **Git** | 任意现代版本 | 需要 git 命令支持 |
| **IDE** | Qoder/Lingma最新版 | 支持 MCP 工具 |
| **项目路径** | **纯英文路径** | ⚠️ **不能包含中文**,会报错 |

### 快速检查

```bash
node --version    # 应该 >= v20.17.0
git --version     # 显示 git 版本
```

### ⚠️ 重要：项目路径要求

**❌ 错误示例 (会导致 IO exception)**:
```
D:\Workspace\部分源码\hussar-base-platform
C:\用户\张三\projects\my-project
E:\开发环境\项目A\code
```

**✅ 正确示例**:
```
D:\workspace\hussar-base-platform
C:\users\zhangsan\projects\my-project
E:\dev-projects\project-a\code
```

**原因**: 
- GitNexus 底层使用 Neo4j 数据库，对中文路径支持不完善
- 路径中的中文会导致 `IO exception: Cannot open file` 错误
- Windows PowerShell 的编码问题会加剧这个情况

**解决方案**:
1. 将项目移动到**全英文路径**
2. 确保所有父级目录都不含中文
3. 如果已有中文路径，先移动项目再重新生成索引

---

## 🚀 第一部分：快速搭建

### 步骤 1: 安装 GitNexus (30 秒)

无需全局安装，通过 `npx` 直接使用:

```bash
# 在项目根目录执行
npx gitnexus analyze
```

首次运行会自动下载 GitNexus。

### 步骤 2: 配置 MCP (1 分钟)

#### Qoder IDE

编辑 Qoder 全局 MCP 配置文件 `C:\Users\%USERNAME%\AppData\Roaming\Qoder\SharedClientCache\mcp.json`:

```json
{
  "mcpServers": {
    "gitnexus": {
      "type": "stdio",
      "command": "npx",
      "args": [
        "-y",
        "gitnexus@latest",
        "mcp"
      ]
    }
  }
}
```

> **注意**: Qoder 使用全局 MCP 配置，不是项目级的 `.qoder/settings.json`

#### Lingma IDE

创建 `.lingma/settings.json`:

```json
{
  "mcpServers": {
    "gitnexus": {
      "command": "npx",
      "args": ["-y", "gitnexus@latest"],
      "cwd": "${workspaceFolder}"
    }
  }
}
```

### 步骤 3: 重启 IDE

**完全关闭并重新打开 IDE**(不只是刷新窗口)。

### 步骤 4: 验证安装

```bash
# 检查 GitNexus 可用
npx gitnexus --version

# 生成项目索引
npx gitnexus analyze

# 验证索引状态
npx gitnexus status
```

看到类似输出表示成功:

```
✓ Index found: hussar-base-platform
Symbols: 12345
Relationships: 56789
Last updated: 2024-01-15 10:30:00
```

---

## 🎯 第二部分：实际使用场景

### 场景 1: 接手新项目，快速理解代码

**问题**: "我刚加入团队，负责认证模块，如何快速上手？"

**步骤**:

1. **读取项目上下文**
   ```
   READ gitnexus://repo/{project-name}/context
   ```
   获得：项目统计、核心模块概览 (~150 tokens)

2. **查询认证相关流程**
   ```
   gitnexus_query({query: "用户认证登录"})
   ```
   获得：所有与认证相关的执行流程

3. **查看关键符号的完整上下文**
   ```
   gitnexus_context({name: "validateUser"})
   ```
   获得:
   - 定义位置
   - 所有调用者 (上游)
   - 所有被调用者 (下游)
   - 所属流程

4. **阅读具体流程的执行轨迹**
   ```
   READ gitnexus://repo/{project-name}/process/LoginFlow
   ```
   获得：逐步执行 trace (~200 tokens)

**预期结果**: 10 分钟内理解整个认证架构

---

### 场景 2: 调试线上 Bug

**问题**: "用户反馈支付失败，报错 'validation timeout',如何定位？"

**步骤**:

1. **搜索相关概念**
   ```
   gitnexus_query({query: "支付验证超时"})
   ```

2. **找到可能的问题函数**
   从 query 结果中定位到 `validatePayment()`

3. **查看完整上下文**
   ```
   gitnexus_context({name: "validatePayment"})
   ```
   重点关注:
   - 调用了哪些外部 API
   - 超时配置在哪里
   - 错误处理逻辑

4. **追踪执行流程**
   ```
   READ gitnexus://repo/{project-name}/process/PaymentFlow
   ```

5. **如果发现是外部 API 问题，检查所有调用点**
   ```
   gitnexus_impact({target: "externalPaymentAPI", direction: "upstream"})
   ```

**预期结果**: 15 分钟定位 root cause

---

### 场景 3: 修改前评估影响范围

**问题**: "我要重构 `calculateOrderTotal()` 函数，会影响哪些地方？"

**步骤**:

1. **运行影响分析**
   ```
   gitnexus_impact({target: "calculateOrderTotal", direction: "upstream"})
   ```

2. **查看结果分层**
   ```
   d=1 (深度 1): WILL BREAK - 直接调用者，必须更新
   d=2 (深度 2): MAY BREAK - 间接调用者，可能需要测试
   d=3+ (深度 3+): LOW RISK - 较远依赖，风险较低
   ```

3. **风险评估**
   - <5 个符号，少量流程 → **LOW**,安全进行
   - 5-15 个符号，2-5 个流程 → **MEDIUM**,充分测试
   - >15 个符号或多个流程 → **HIGH**,谨慎规划
   - 涉及订单/支付核心路径 → **CRITICAL**,全覆盖测试

4. **制定重构计划**
   - 先更新所有 d=1 调用者
   - 为 d=2 编写回归测试
   - 准备回滚方案

**预期结果**: 避免 90% 的重构事故

---

### 场景 4: 安全地重命名函数

**问题**: "要把 `getUserData()` 改名为 `fetchUserProfile()`,如何确保不改坏？"

**步骤**:

1. **预览模式 (不实际修改)**
   ```
   gitnexus_rename({
     symbol_name: "getUserData",
     new_name: "fetchUserProfile",
     dry_run: true
   })
   ```

2. **检查预览结果**
   - graph 编辑：自动更新 (安全)
   - ast_search 匹配：需要人工 review

3. **确认无误后实际应用**
   ```
   gitnexus_rename({
     symbol_name: "getUserData",
     new_name: "fetchUserProfile",
     dry_run: false
   })
   ```

4. **验证变更范围**
   ```
   gitnexus_detect_changes({scope: "staged"})
   ```

5. **运行受影响流程的测试**
   根据 impact 分析结果，运行相关测试用例

**预期结果**: 零风险完成重构

---

### 场景 5: Review PR

**问题**: "同事提了个 PR,如何快速审查质量？"

**步骤**:

1. **查看 PR diff**
   ```bash
   gh pr diff <PR号>
   ```

2. **检测变更影响**
   ```
   gitnexus_detect_changes({
     scope: "compare",
     base_ref: "main",
     head_ref: "feature-branch"
   })
   ```

3. **对每个变更的符号做影响分析**
   ```
   gitnexus_impact({target: "changedFunction", direction: "upstream"})
   ```

4. **检查重点**
   - d=1 调用者是否在 PR 中同步更新
   - 是否破坏了现有流程
   - 是否有足够的测试覆盖

5. **给出 review 意见**
   ```
   ✅ 变更范围清晰，影响可控
   ⚠️ 注意：X 函数的调用者 Y 未更新
   ❌ 高风险：核心支付逻辑缺少测试
   ```

**预期结果**: 10 分钟完成高质量 PR 审查

---

## 🛠️ 第三部分：核心技能详解

### 技能 1: query - 按概念搜索

**用途**: 找到与某个业务概念相关的所有代码流程

**示例**:
```
gitnexus_query({query: "订单取消退款"})
```

**返回**:
- 相关的 Process 列表
- 每个 Process 包含的步骤
- 涉及的函数/类

**最佳实践**:
- ✅ 使用业务术语 ("订单取消")
- ✅ 使用动词 ("处理","验证","计算")
- ❌ 避免函数名 (那是 context 的工作)

---

### 技能 2: context - 符号 360° 视图

**用途**: 全面了解一个函数/类的所有连接

**示例**:
```
gitnexus_context({name: "processRefund"})
```

**返回**:
```
Definition:
  File: src/payment/refund.service.ts:45
  
Callers (Upstream):
  - handleRefundRequest (d=1)
  - batchRefundProcessor (d=1)
  - refundApiController (d=1)
  
Callees (Downstream):
  - validateRefundEligibility
  - executeRefund
  - notifyUser
  
Processes:
  - RefundProcessingFlow
  - BatchRefundFlow
```

**最佳实践**:
- ✅ 修改任何函数前先查 context
- ✅ 关注 d=1 callers(直接依赖)
- ✅ 检查所属 processes(业务场景)

---

### 技能 3: impact - 爆炸半径分析

**用途**: 评估修改的影响范围

**示例**:
```
gitnexus_impact({
  target: "validateUser",
  direction: "upstream"  // 谁依赖它
})

gitnexus_impact({
  target: "axios",
  direction: "downstream"  // 它依赖谁
})
```

**返回结构**:
```
depth=1 (WILL BREAK):
  - login (confidence: 0.95)
  - register (confidence: 0.92)

depth=2 (MAY BREAK):
  - authController (confidence: 0.75)

depth=3+ (LOW RISK):
  - ...
```

**置信度解读**:
- >0.8: 高置信度，可信
- 0.5-0.8: 中等，需要人工确认
- <0.5: 低，可能是误报

---

### 技能 4: detect_changes - Git diff 影响映射

**用途**: 自动检测当前 Git 变更的影响

**模式**:

1. **比较两个分支**
   ```
   gitnexus_detect_changes({
     scope: "compare",
     base_ref: "main",
     head_ref: "feature"
   })
   ```

2. **查看暂存区**
   ```
   gitnexus_detect_changes({scope: "staged"})
   ```

3. **查看工作区**
   ```
   gitnexus_detect_changes({scope: "workspace"})
   ```

**返回**:
- 变更的符号列表
- 每个符号的影响分析
- 受影响的 processes

---

### 技能 5: rename - 自动化重命名

**用途**: 跨文件智能重命名

**示例**:
```
gitnexus_rename({
  symbol_name: "oldFunc",
  new_name: "newFunc",
  dry_run: true  // 先预览
})
```

**优势**:
- ✅ 基于 AST，不是文本替换
- ✅ 理解作用域，不会误杀
- ✅ 自动处理 import/export
- ✅ 区分同名不同函数

---

### 技能 6: cypher - 自定义图查询

**用途**: 灵活查询知识图谱

**示例**:

1. **查找所有调用某函数的代码**
   ```cypher
   MATCH (caller)-[:CodeRelation {type: 'CALLS'}]->(f:Function {name: "myFunc"})
   RETURN caller.name, caller.filePath
   ```

2. **查找循环依赖**
   ```cypher
   MATCH (a:Function)-[:CALLS*2..5]->(a)
   RETURN a.name
   ```

3. **查找未被使用的函数**
   ```cypher
   MATCH (f:Function)
   WHERE NOT ()-[:CALLS]->(f)
   RETURN f.name
   ```

**学习资源**:
- `READ gitnexus://repo/{name}/schema` - 查看图 schema
- [Cypher 查询语言指南](https://neo4j.com/docs/cypher-manual/current/)

---

## 📊 第四部分：性能指标

### 索引大小参考

| 项目规模 | 代码行数 | 索引大小 | 符号数 | 关系数 |
|---------|---------|---------|--------|--------|
| 小型项目 | <10k | ~5MB | <1k | <5k |
| 中型项目 | 10k-50k | ~20MB | 1k-5k | 5k-20k |
| 大型项目 | 50k-200k | ~100MB | 5k-20k | 20k-100k |
| 超大型项目 | >200k | ~500MB | >20k | >100k |

**hussar-base-platform 参考**:
- 代码行数：~80k
- 索引大小：~85MB
- 符号数：~12k
- 关系数：~58k

> 💡 **提示**: 将上述数据替换为你实际项目的统计数据。

### 💾 磁盘空间占用详解

**GitNexus 需要的总空间 = 索引文件 + 缓存 + embeddings(可选)**

#### 1. 必需空间

```
.gitnexus/
├── graph.db           # 知识图谱数据库 (占 80%)
├── registry.json      # 注册表 (<1KB)
├── config.json        # 配置文件 (<1KB)
└── cache/             # 临时缓存 (~10-50MB)
```

**典型占用** (以中型项目为例):
- `graph.db`: ~16MB
- `cache/`: ~20MB
- **合计**: ~36MB

#### 2. 可选空间 (embeddings)

如果启用语义搜索 (`--embeddings`):

```
.gitnexus/embeddings/
├── vectors.bin        # 向量文件 (占大头)
└── metadata.json      # 元数据
```

**典型占用**:
- 小型项目：~15MB
- 中型项目：~60MB
- 大型项目：~300MB
- 超大型项目：~1.5GB

#### 3. 全局配置目录

```
~/.gitnexus/           # 用户主目录下的全局配置
├── registry.json      # 所有索引项目的注册表
├── config.json        # 全局配置
└── logs/              # 日志文件
```

**占用**: ~5-10MB (所有项目共享)

### 📌 实际案例：hussar-base-platform

**不启用 embeddings**:
```
.gitnexus/               85MB
├── graph.db            68MB
├── cache/              15MB
└── 其他文件            2MB

~/.gitnexus/             5MB (与其他项目共享)

总计：~90MB
```

**启用 embeddings**:
```
.gitnexus/              280MB
├── graph.db            68MB
├── embeddings/        195MB
├── cache/              15MB
└── 其他文件            2MB

~/.gitnexus/             5MB

总计：~285MB
```

### ⚠️ 空间优化建议

1. **定期清理缓存**
   ```bash
   # 删除缓存目录 (安全，会自动重建)
   rm -rf .gitnexus/cache/
   ```

2. **不需要 embeddings 就不要启用**
   ```bash
   # 默认不启用 (节省 3-5 倍空间)
   npx gitnexus analyze
   
   # 除非你真的需要语义搜索
   npx gitnexus analyze --embeddings
   ```

3. **多项目共享全局配置**
   - `~/.gitnexus/` 是所有项目共享的
   - 不会因为项目多而线性增长

4. **监控空间使用**
   ```bash
   # Windows PowerShell
   Get-ChildItem .gitnexus -Recurse | Measure-Object -Property Length -Sum
   
   # Mac/Linux
   du -sh .gitnexus/*
   ```

### 🎯 空间需求总结

| 场景 | 小型项目 | 中型项目 | 大型项目 | 超大型项目 |
|-----|---------|---------|---------|-----------|
| **仅索引** | ~10MB | ~36MB | ~120MB | ~600MB |
| **+embeddings** | ~25MB | ~100MB | ~420MB | ~2.1GB |
| **推荐配置** | 预留 50MB | 预留 150MB | 预留 500MB | 预留 2.5GB |

> 💡 **经验法则**: 为每个项目预留的磁盘空间 ≈ 索引大小的 1.5 倍 (包含缓存和余量)

### 响应时间

| 操作 | 平均耗时 | 影响因素 |
|-----|---------|---------|
| `query` | 0.5-2s | 结果数量 |
| `context` | 0.3-1s | 符号复杂度 |
| `impact` | 1-5s | 影响范围 |
| `rename(dry_run)` | 1-3s | 改名范围 |
| `detect_changes` | 2-8s | diff 大小 |

### 优化建议

1. **不使用 embeddings 时更快**
   ```bash
   # 默认不启用 embeddings (快)
   npx gitnexus analyze
   
   # 启用语义搜索 (慢 3-5 倍，但更准确)
   npx gitnexus analyze --embeddings
   ```

2. **增量索引比全量快**
   ```bash
   # 增量 (只扫描变更文件)
   npx gitnexus analyze
   
   # 全量 (--force 强制重新扫描)
   npx gitnexus analyze --force
   ```

3. **SSD vs HDD**
   - SSD: 索引速度快 3-5 倍
   - 推荐将项目放在 SSD 上

---

## 🏆 第五部分：最佳实践 (来自真实项目)

### 1. 每日工作流

**早上开工**:
```bash
# 切换到当前任务分支
git checkout feature/my-task

# 检查索引状态
npx gitnexus status

# 如果过时，更新索引
npx gitnexus analyze
```

**修改代码前**:
```
1. gitnexus_context({name: "要修改的函数"})
2. gitnexus_impact({target: "该函数", direction: "upstream"})
3. 评估风险等级
4. 制定修改计划
```

**提交代码前**:
```bash
# 查看变更影响
gitnexus_detect_changes({scope: "staged"})

# 运行受影响流程的测试
# (根据 detect_changes 结果)
```

### 2. 团队协作规范

**索引共享策略**:
- ❌ **不要**将 `.gitnexus/` 提交到 Git
- ✅ **每个成员**独立生成自己的索引
- ✅ **新成员入职**: 第一件事 `npx gitnexus analyze`

**.gitignore 配置**:
```gitignore
# GitNexus
.gitnexus/
CLAUDE.md
AGENTS.md
```

**团队知识沉淀**:
- ✅ 使用 `gitnexus wiki` 生成文档
- ✅ 将生成的 CLAUDE.md 提交到 Git
- ✅ 新人通过文档快速了解项目

### 3. 多项目管理

**场景**: 同时维护 3 个项目

**策略**:
```bash
# 项目 A
cd project-a
npx gitnexus status  # 检查
npx gitnexus analyze  # 如需更新

# 项目 B
cd project-b
npx gitnexus status

# 项目 C
cd project-c
npx gitnexus status
```

**注意**:
- 每个项目索引独立
- 切换项目后第一次使用前最好 check 一下索引状态
- 不要在 A 项目中问 B 项目的代码 (会混淆)

### 4. 大型重构流程

**阶段 1: 准备 (1-2 天)**
```
1. gitnexus_query({query: "重构目标模块"})
2. 画出完整的依赖图
3. 识别关键路径和风险点
4. 制定分步计划
```

**阶段 2: 分步实施 (按模块拆分)**
```
对于每个模块:
1. gitnexus_impact({target: "模块入口", direction: "upstream"})
2. 只修改当前模块 + d=1 依赖
3. gitnexus_detect_changes 验证
4. 运行测试
5. 提交
```

**阶段 3: 回归验证**
```
1. gitnexus_detect_changes({scope: "compare", base_ref: "main"})
2. 检查所有 affected processes
3. 全量测试套件
```

### 5. 避免常见陷阱

**❌ 错误**: 不检查影响就直接修改
**✅ 正确**: 修改前必做 impact analysis

**❌ 错误**: 忽略 d=1 警告
**✅ 正确**: 优先处理所有 WILL BREAK 项

**❌ 错误**: 重构后不验证
**✅ 正确**: 重构后必跑 detect_changes

**❌ 错误**: 索引过期也不更新
**✅ 正确**: 大变更后立即 re-index

**❌ 错误**: 用 query 查找具体函数
**✅ 正确**: query 用于业务概念，context 用于具体符号

---

## 🔧 第六部分：高级技巧

### 技巧 1: 组合使用威力更大

**场景**: "我要添加一个新 API 端点，如何确保设计合理？"

```
1. gitnexus_query({query: "类似的 API 端点"})
   → 参考现有模式

2. gitnexus_context({name: "现有 Controller"})
   → 理解架构

3. 设计新端点

4. gitnexus_impact({target: "依赖的核心服务", direction: "downstream"})
   → 检查是否影响底层服务

5. gitnexus_detect_changes({scope: "workspace"})
   → 验证实现
```

### 技巧 2: 利用 Process 资源快速学习

**Process 资源** (~200 tokens) 提供执行轨迹:

```
READ gitnexus://repo/{project-name}/process/UserRegistration
```

你会得到:
```
Process: UserRegistration
Steps:
  1. UserController.register() 
     ↓
  2. UserService.createUser()
     ↓
  3. Validator.validateInput()
     ↓
  4. UserRepository.save()
     ↓
  5. EmailService.sendConfirmation()
```

**用途**:
- 理解业务流程
- 定位问题环节
- 设计新功能的参考

### 技巧 3: Cluster 资源看功能分区

```
READ gitnexus://repo/{project-name}/clusters
```

显示项目的功能模块化情况:

```
Cluster: Authentication
- Members: 15 functions
- Cohesion: 0.85 (高内聚)

Cluster: Payment
- Members: 23 functions
- Cohesion: 0.72 (中等)
```

**用途**:
- 理解项目架构
- 发现代码坏味道 (低 cohesion)
- 指导重构方向

### 技巧 4: 自动化 Git hooks

**post-commit 钩子**:

`.git/hooks/post-commit`:
```bash
#!/bin/bash
echo "Updating GitNexus index..."
npx gitnexus analyze > /dev/null 2>&1
echo "✓ Index updated"
```

每次 commit 后自动更新索引，保持新鲜。

**pre-push 钩子**:

`.git/hooks/pre-push`:
```bash
#!/bin/bash
echo "Checking change impact..."
# 可以在这里集成到 CI
```

### 技巧 5: 语义搜索 (可选) - ⚠️ 不推荐启用

**💡 重要建议：不要启用 embeddings**

**原因**:
1. **功能重复** - AI/LLM 本身就是语义搜索，已经足够强大
2. **消耗磁盘** - 增加 3-5 倍索引体积，没有实际价值
3. **性能下降** - 索引速度慢 3-5 倍，查询也变慢
4. **配置复杂** - 需要额外配置 API Key 和模型

**对比**:

| 特性 | 不使用 embeddings | 使用 embeddings |
|-----|------------------|----------------|
| **索引速度** | 快 (~1 分钟) | 慢 (~5 分钟) |
| **索引大小** | 小 (~85MB) | 大 (~285MB) |
| **搜索能力** | ✅ AI 语义理解 | ❌ 向量相似度 |
| **配置成本** | 零配置 | 需要 API Key |
| **性价比** | ⭐⭐⭐⭐⭐ | ⭐ |

**结论**: 
> 🎯 **强烈建议保持默认配置（不启用 embeddings）**，让 AI 用原生的语义理解能力就够了!

---

如果你仍然想尝试 (不推荐),以下是配置方法:

**启用 embeddings**:

```bash
npx gitnexus analyze --embeddings
```

**使用场景** (理论上):
- ✅ 模糊查询 ("那个...处理支付的函数")
- ✅ 跨语言搜索 ("payment" 搜到 "支付")
- ✅ 概念匹配 ("认证" 搜到 "login")

**代价**:
- 索引速度变慢 3-5 倍
- 索引体积增大 2-3 倍
- 需要配置 embedding API

**配置** (`~/.gitnexus/config.json`):
```json
{
  "embeddings": {
    "provider": "openai",
    "apiKey": "sk-...",
    "model": "text-embedding-3-small"
  }
}
```

**再次强调**: 这些功能 AI 本身就能做到，不需要额外的 embeddings!

---

## ⚠️ 第七部分：故障排查

### 问题 1: "Not inside a git repository"

**症状**: 运行任何命令都报这个错

**原因**: 当前目录不是 Git 仓库

**解决**:
```bash
# 初始化 Git
git init

# 或切换到项目目录
cd /path/to/git/project
```

### 问题 2: "Index is stale"

**症状**: 工具响应不准确，或读到旧数据

**原因**: 索引落后于代码

**解决**:
```bash
npx gitnexus analyze --force
```

然后**重启 IDE**。

### 问题 3: 工具不可用

**症状**: AI 无法调用 GitNexus 工具

**排查步骤**:
```
1. 检查 MCP 配置
   cat C:\Users\%USERNAME%\AppData\Roaming\Qoder\SharedClientCache\mcp.json

2. 验证 Node.js 版本
   node --version  # 必须 >= v20.17.0

3. 检查索引状态
   npx gitnexus status

4. 完全重启 IDE (不是刷新窗口)
```

### 问题 4: Windows 权限错误

**症状**: EACCES: permission denied

**解决**:
1. 以管理员身份运行 IDE
2. 或在项目文件夹右键 → 属性 → 安全 → 编辑权限

### 问题 5: Mac npm 权限错误

**症状**: npm install 报错 permission denied

**解决**:
```bash
mkdir ~/.npm-global
npm config set prefix '~/.npm-global'
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.zshrc
source ~/.zshrc
sudo chown -R $(whoami) ~/.npm
```

### 问题 6: 索引太慢

**症状**: analyze 超过 10 分钟

**可能原因**:
- 项目太大 (正常)
- 启用了 embeddings (默认没有)
- HDD 硬盘 (换 SSD)

**优化**:
```bash
# 不使用 embeddings
npx gitnexus analyze

# 排除不必要的目录
# 在 package.json 中添加:
{
  "gitnexus": {
    "exclude": [
      # 通用排除（前后端都适用）
      "node_modules",      # 依赖目录，通常很大且不需要分析
      ".git",              # Git 仓库数据
      
      # 前端项目常见排除
      "dist",              # 构建输出目录
      "build",             # 构建输出目录
      "cdn",               # CDN 静态资源
      "static-public",     # 静态资源目录
      "**/*.min.js",       # 压缩后的 JS 文件
      "**/*.min.css",      # 压缩后的 CSS 文件
      "**/*.map",          # Source map 文件
      
      # 后端项目常见排除
      "target",            # Maven/Gradle 构建输出
      "out",               # 编译输出目录
      "*.jar",             # 打包后的 JAR 文件
      "*.war",             # 打包后的 WAR 文件
      
      # 测试相关（可选）
      "coverage",          # 测试覆盖率报告
      "__tests__",         # 测试目录（如不需要分析测试代码）
      "*.test.js",         # 测试文件
      
      # 文档和配置（可选）
      "docs",              # 文档目录
      ".github",           # GitHub 配置
      ".vscode"            # VSCode 配置
    ]
  }
}
```

**排除规则说明**:
- `**/` 前缀表示递归匹配所有子目录
- `*` 通配符匹配任意字符
- 根据项目实际情况选择排除项，不是越多越好
- 排除后重新运行 `npx gitnexus analyze --force` 生效

---

## 📚 第八部分：学习路径

### 新手入门 (第 1 周)

**Day 1**: 安装和第一个索引
- 跟随本指南第一部分
- 成功运行 `npx gitnexus analyze`

**Day 2-3**: 练习 query 和 context
- 在你的项目中搜索业务概念
- 查看关键函数的 context

**Day 4-5**: 理解 impact analysis
- 对要修改的代码运行 impact
- 理解 d=1/d=2/d=3 的含义

**Weekend**: 实战一个小需求
- 使用 impact 评估
- 使用 detect_changes 验证

### 进阶提升 (第 2-3 周)

**Week 2**: 掌握 refactor
- 练习 rename 操作
- 安全地重构代码

**Week 3**: PR 审查
- 使用 detect_changes 审 PR
- 给出有依据的 review 意见

### 高手之路 (1 个月+)

- 熟练使用 cypher 自定义查询
- 能够设计合理的集群划分
- 指导团队成员使用
- 建立团队的 GitNexus 使用规范

---

## 📝 总结清单

### ✅ 安装检查清单

- [ ] Node.js >= v20.17.0
- [ ] Git 已安装
- [ ] IDE (Qoder/Lingma) 已安装
- [ ] Qoder 全局 MCP 配置 (`mcp.json`) 已配置
- [ ] IDE 已完全重启
- [ ] `npx gitnexus --version` 显示版本
- [ ] `npx gitnexus analyze` 成功执行
- [ ] `npx gitnexus status` 显示索引正常

### ✅ 日常使用检查清单

开始工作前:
- [ ] 切换到正确的分支
- [ ] `npx gitnexus status` 检查索引
- [ ] 如过期则 `npx gitnexus analyze`

修改代码前:
- [ ] `gitnexus_context` 查看上下文
- [ ] `gitnexus_impact` 评估影响
- [ ] 确认风险等级可控

提交代码前:
- [ ] `gitnexus_detect_changes` 验证变更
- [ ] 运行受影响流程的测试

### ✅ 团队协作检查清单

- [ ] `.gitnexus/` 已添加到 .gitignore
- [ ] 新成员知道要自己生成索引
- [ ] CLAUDE.md/AGENTS.md 已提交 (可选)
- [ ] 团队有 GitNexus 使用规范

---

## 🔗 相关资源

### 技能文档

- **[CLI 命令](gitnexus-cli.md)** - 索引管理详解
- **[探索代码](gitnexus-exploring.md)** - 理解代码架构
- **[调试指南](gitnexus-debugging.md)** - 追踪 bug
- **[影响分析](gitnexus-impact-analysis.md)** - 修改前必读
- **[重构指南](gitnexus-refactoring.md)** - 安全重构
- **[PR 审查](gitnexus-pr-review.md)** - Pull Request 审查
- **[工具参考](gitnexus-guide.md)** - 完整工具手册

### 外部资源

- [GitNexus GitHub](https://github.com/gitnexus/gitnexus)
- [GitNexus MCP](https://github.com/gitnexus/gitnexus-mcp)
- [Claude Code 文档](https://docs.anthropic.com/claude-code/)
- [Cypher 查询语言](https://neo4j.com/docs/cypher-manual/current/)

---

**最后提醒**: GitNexus 是你的代码智能副驾驶！但它需要:
1. ✅ **新鲜的索引** - 定期 update
2. ✅ **正确的使用** - 遵循最佳实践
3. ✅ **批判性思维** - 置信度只是参考，人工判断最重要

祝编码愉快！🚀

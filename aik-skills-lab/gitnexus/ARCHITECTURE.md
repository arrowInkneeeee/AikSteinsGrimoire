# GitNexus 架构说明

## 📚 核心概念解析

### Skills 文档 vs GitNexus 工具 vs 代码索引

这是理解 GitNexus 系统的关键！很多人混淆这三者的关系。

## 🎯 三者关系图

```
┌─────────────────────────────────────────────────────────────┐
│                    开发者/AI 用户                             │
└─────────────────────────────────────────────────────────────┘
                            ↓ 使用
┌─────────────────────────────────────────────────────────────┐
│  Skills 文档 (使用说明书)                                      │
│  - .qoder/skills/gitnexus/                                  │
│  - 教 AI 如何调用 GitNexus 工具                                 │
│  - ✅ 完全通用，可迁移到任何项目                              │
│  - ❌ 本身不执行任何操作                                      │
└─────────────────────────────────────────────────────────────┘
                            ↓ 指导调用
┌─────────────────────────────────────────────────────────────┐
│  GitNexus MCP Tools (实际工具)                                │
│  - query, context, impact, rename, detect_changes...        │
│  - ✅ 标准接口，所有项目通用                                  │
│  - ❌ 但需要索引才能工作                                      │
└─────────────────────────────────────────────────────────────┘
                            ↓ 依赖
┌─────────────────────────────────────────────────────────────┐
│  代码索引 (知识图谱)                                          │
│  - .gitnexus/                                               │
│  - ✅ 包含项目的 AST+ 调用关系 + 依赖图                          │
│  - ❌ 每个项目必须单独生成                                    │
└─────────────────────────────────────────────────────────────┘
                            ↓ 基于
┌─────────────────────────────────────────────────────────────┐
│  源代码                                                      │
│  - 你的 Java/TypeScript/Python 代码                           │
│  - ❌ 每个项目都不同                                          │
└─────────────────────────────────────────────────────────────┘
```

## 📊 详细对比表

| 维度 | Skills 文档 | GitNexus 工具 | 代码索引 |
|------|-----------|-------------|---------|
| **位置** | `.qoder/skills/gitnexus/` | Lingma 内置 | `.gitnexus/` |
| **文件格式** | Markdown (.md) | MCP Server | JSON + Graph DB |
| **是否通用** | ✅ 完全通用 | ✅ 标准接口 | ❌ 项目特定 |
| **删除后果** | AI 不会用工具 | 功能消失 | 工具报错 |
| **迁移方式** | 直接复制 | 无需操作 | 重新生成 |
| **更新频率** | 低 (技能改进) | 低 (版本升级) | 高 (代码变更) |
| **大小** | ~50KB | ~10MB | ~100MB-1GB |
| **生成方式** | 手动编写 | 安装获得 | `npx gitnexus analyze` |

## 🔧 工作原理详解

### 1. Skills 文档的工作方式

```markdown
---
name: gitnexusExploring
description: Understand how code works...
---

## Workflow
1. READ gitnexus://repo/{name}/context
2. gitnexus_query({query: "<concept>"})
3. ...
```

**当 AI 收到用户问题:**
1. AI 读取 Skills 文档
2. 匹配用户意图到对应的 Skill
3. 按照 Workflow 调用 GitNexus 工具
4. 工具返回结果 → AI 整合答案

**关键**: Skills 使用**参数化模板**:
- `{name}` → 自动替换为当前项目名
- `<concept>` → 动态填充为用户查询的概念
- **不硬编码任何项目路径**

### 2. GitNexus 工具的工作方式

```
用户： "认证流程是怎么工作的？"

AI (按 Skills 指导):
  gitnexus_query({query: "authentication flow"})

GitNexus MCP Server:
  1. 接收查询
  2. 在 .gitnexus/ 索引中搜索
  3. 找到相关执行流：LoginFlow, TokenRefresh
  4. 返回结构化结果

AI: 
  "认证流程涉及以下组件:
   1. LoginFlow: 用户登录 → validateUser → createToken
   2. TokenRefresh: checkExpiry → refreshToken
   ..."
```

**没有索引时会发生:**
```
GitNexus MCP Server:
  ❌ Error: No index found for repository
  
AI 收到错误后:
  ⚠️ 无法使用 GitNexus 工具
  → 只能退回传统方式 (grep, 手动读代码)
```

### 3. 索引的生成过程

```bash
npx gitnexus analyze
```

**执行步骤:**

1. **扫描源代码**
   - 遍历所有 `.java`, `.ts`, `.py` 等文件
   - 解析 AST (抽象语法树)

2. **提取关系**
   - 函数调用关系：A calls B
   - 类继承关系：Class A extends B
   - 模块依赖：Module A imports Module B

3. **构建图数据库**
   ```
   Nodes: File, Function, Class, Method, Process
   Edges: CALLS, IMPORTS, EXTENDS, IMPLEMENTS
   ```

4. **写入 .gitnexus/**
   - `meta.json`: 元数据
   - `graph.db`: 图数据库
   - `embeddings/`: 语义向量 (可选)

5. **注册到全局**
   - 更新 `~/.gitnexus/registry.json`
   - 记录项目名称和路径

## 🚀 正确的项目迁移流程

### ❌ 错误做法

```bash
# 错误 1: 只复制 Skills，不生成新索引
cp -r old-project/.qoder/skills/gitnexus/ new-project/
# 结果：GitNexus 工具报错 "index not found"

# 错误 2: 复制旧的 .gitnexus/ 目录
cp -r old-project/.gitnexus/ new-project/
# 结果：索引是旧项目的代码关系，查询全部错误

# 错误 3: 修改 Skills 中的项目路径
sed -i 's/old-project/new-project/g' README.md
# 结果：下次迁移又要改，无限循环
```

### ✅ 正确做法

```bash
# 步骤 1: 复制 Skills 文档 (通用，无需修改)
cp -r /path/to/old-project/.qoder/skills/gitnexus/ \
      /path/to/new-project/.qoder/skills/

# 步骤 2: 进入新项目
cd /path/to/new-project

# 步骤 3: 生成新索引 (必须!)
npx gitnexus analyze

# 步骤 4: 验证
npx gitnexus status
# 输出应该显示新项目的信息

# 完成! ✅
```

## 💡 类比理解

### 🚗 汽车导航系统类比

| GitNexus 组件 | 汽车对应物 | 通用性 | 是否需要"地图" |
|-------------|----------|--------|--------------|
| **Skills 文档** | 驾驶手册 + 导航仪说明书 | ✅ 任何车都能用 | ❌ 不需要 |
| **MCP 工具** | 导航仪硬件 | ✅ 标准设备 | ❌ 但需要地图数据 |
| **代码索引** | GPS 地图数据 | ❌ 每座城市不同 | ✅ 本身就是地图 |
| **源代码** | 城市道路 | ❌ 每座城市不同 | N/A |

**场景**: 你搬到新城市

1. **驾驶手册** (Skills) → 带着走，无需修改 ✅
2. **导航仪** (MCP 工具) → 带着走，无需修改 ✅  
3. **地图数据** (索引) → 必须买新城市的地图 ❌
4. **道路** (源代码) → 新城市本来就有 ❌

**你会怎么做？**
```
1. 带上驾驶手册和导航仪 ✅
2. 在新城市购买地图数据 (analyze) ✅
3. 开始使用导航 ✅
```

**不是这样做：**
```
1. 带上驾驶手册和导航仪 ✅
2. 继续用旧城市的地图 ❌ (会导错路!)
3. 或者把驾驶手册里的"北京市"改成"上海市" ❌ (没必要!)
```

## 🎯 回到你的问题

### 原问题回顾

> gitnexus 整理成 skill 是不是错误的方向？

**答案：不是错误的，但需要正确理解层次!**

### ✅ 正确的理解

1. **Skills 文档是通用的** → 对！应该整理 ✅
2. **但需要说明前置条件** → 索引必须每个项目单独生成 ✅
3. **不能硬编码项目路径** → 使用参数化模板 ✅

### 我们已经做的改进

#### Before (错误示范):
```markdown
### 1. 检查索引状态

```bash
cd d:/work/hussar-project/resData/hussar-base-platform  # ❌ 硬编码!
npx gitnexus status
```
```

#### After (正确示范):
```markdown
### ⚠️ 前置要求：先为你的项目生成索引

#### 1. 检查索引状态

```bash
# 在你的项目根目录下执行 (不要 cd 到特定路径!)
npx gitnexus status
```

> 💡 **重要提示**: 
> - 索引是**每个项目独立**的
> - Skills 文档可以通用，但**索引必须为每个项目单独生成**
> - 迁移到新项目时：复制 Skills → 在新项目中运行 `analyze`
```

并新增了 **"项目迁移指南"** 章节，详细说明:
- 如何迁移 Skills
- 为什么要重新生成索引
- 常见问题解答

## 📋 总结 Checklist

✅ **Skills 文档应该整理吗？**
- ✅ 应该！提供清晰的使用指南
- ✅ 但要确保内容通用，不硬编码路径
- ✅ 要说明前置条件 (需要索引)

✅ **索引如何处理？**
- ❌ 不要在 Skills 中包含项目特定路径
- ✅ 使用相对路径和参数化模板
- ✅ 添加迁移指南，说明每个项目需要重新索引

✅ **用户教育**
- ✅ 明确区分三层：Skills / 工具 / 索引
- ✅ 提供迁移指南和 FAQ
- ✅ 使用类比帮助理解

## 🔗 进一步阅读

- [SKILL.md](SKILL.md) - 主索引文件 (英文，快速参考)
- [references/gitnexus-getting-started.md](references/gitnexus-getting-started.md) - 完整入门指南 (中文，978 行)
- [references/gitnexus-cli.md](references/gitnexus-cli.md) - CLI 命令详解

---

**记住**: 
- Skills = 教科书 (通用) ✅
- 索引 = 实验设备 (每个项目自己搭建) ❌
- 带着教科书去新实验室，然后搭建设备！🚀

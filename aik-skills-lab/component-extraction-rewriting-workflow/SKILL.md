---
name: component-extraction-rewriting-workflow
description: 当需要完整执行从源项目萃取组件知识到目标项目复写标准代码的全流程时使用。适用于"把这个组件迁移到新项目"、"标准化复用XX方案"、"从老项目提取组件到积累库"、"完整萃取加复写"等全流程场景。同时支持非代码内容的整理与入库（如将URL资料、学习笔记整理为卷轴存放到scrolls中）。对于单阶段需求，直接使用 spec-component-extractor 或 spec-component-rewriter。
type: Sub-agent
version: 1.1.0
---

# Component Extraction & Rewriting Workflow

## 概述

`component-extraction-rewriting-workflow` 是组件萃取复写工作流的统一入口，将 3 个阶段串联为一条带质量门禁的流水线。

**设计模式**: Pipeline（强制线性分步 + 人工确认卡点，参照 java-sdlc-pipeline）

**支持的内容类型（类型标记）**：

| 类型标记 | 含义 | 对应 KnowledgeType | Phase 1 输入 |
|---------|------|-------------------|-------------|
| **NOTE** | 笔记/文章/学习资料/使用说明 | NOTE(1) | 非代码资料来源（URL/文档/已有资料） |
| **COMPONENT** | 可复用组件 | COMPONENT(2) | 代码范围（文件/包/类/方法）+ 粒度 |
| **SOLUTION** | 解决方案 | SOLUTION(3) | 代码范围（文件/包/类/方法）+ 粒度 |
| **CODE** | 代码片段 | CODE(4) | 代码范围（文件/包/类/方法）+ 粒度 |

**NOTE 类型的特殊性**：
- Phase 1 不需要 gitnexus 索引（非代码资料）
- Phase 2 不需要脱敏和代码审查（非代码产物）
- 产物直接保存为 `src/main/java/io/aik/steins/grimoire/scrolls/{name}/README.md`

---

## 流水线架构

```
用户指定组件范围或资料来源
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ Phase 1: 萃取/整理文档化                                   │
│   输入: 代码范围（COMPONENT/SOLUTION/CODE）                │
│         或资料来源（NOTE：URL/文档/已有资料）               │
│   输出: Component Manual v1.0（10章结构化文档）             │
│         或整理后的笔记/文章（Markdown）                      │
│         含类型标记（NOTE/COMPONENT/SOLUTION/CODE）          │
│   产物保存:                                               │
│     NOTE    → src/main/java/io/aik/steins/grimoire/scrolls/{name}/README.md
│     其他    → doc/ComponentManual-{code}.md               │
│   门禁:                                                   │
│     NOTE    → 内容完整性审核 + 用户确认                     │
│     其他    → 手册覆盖10章 + design-review-checker通过 + 用户确认 │
├─────────────────────────────────────────────────────────┤
│ Phase 2: 脱敏复写/内容落盘                                 │
│   输入: Phase 1 产物（含类型标记）                          │
│   输出: 标准化代码产物 或 卷轴内容                           │
│   规则: 根据类型标记自动决定产物形式                         │
│         NOTE      → src/main/java/io/aik/steins/grimoire/scrolls/{name}/README.md   文件落盘   │
│         COMPONENT → components/{code}/         文件结构   │
│         SOLUTION  → solutions/{code}/          文件结构   │
│         CODE      → content 字段存储，不生成文件            │
│   门禁:                                                   │
│     NOTE    → 内容质量检查 + 用户确认                       │
│     其他    → 三层审查通过 + 脱敏审核 + 用户确认             │
├─────────────────────────────────────────────────────────┤
│ Phase 3: 元数据入库（可选）                                │
│   输入: 已确认的产物                                       │
│   输出: KnowledgePo 元数据记录                             │
│   规则: 询问用户是否入库，type由类型标记自动映射             │
│   门禁: 用户确认 + 数据库表已就绪                           │
└─────────────────────────────────────────────────────────┘
    │
    ▼
完整交付物
```

---

## 执行规则

### 1. 必须按顺序执行
流水线必须从 Phase 1 开始，按顺序执行至 Phase 3。严禁跳过阶段。

### 2. 质量门禁

| 阶段 | 类型 | 门禁条件 | 不通过时的处理 |
|------|------|---------|---------------|
| Phase 1→2 | NOTE | 内容结构完整（标题/章节/结论），类型标记明确 | 返回 Phase 1 补充内容 |
| Phase 1→2 | COMPONENT/SOLUTION/CODE | 手册覆盖 10 章，design-review-checker 通过，含类型标记 | 返回 Phase 1 修复缺陷 |
| Phase 2→3 | NOTE | 内容质量通过（无错字、格式规范、逻辑通顺），产物路径正确 | 返回 Phase 2 修正内容 |
| Phase 2→3 | COMPONENT/SOLUTION/CODE | 三层审查全部通过，无残留业务信息，产物形式与类型匹配 | 返回 Phase 2 修复代码 |
| Phase 3 完成 | 全部 | 数据库表就绪，用户确认入库，元数据写入成功 | 跳过入库，保留产物 |

### 3. 人机协作卡点

每个门禁通过后，必须暂停并**显式询问用户**，获得明确回复后才能决定下一步。严禁在未获得明确回复前执行任何后续操作。

**选项行为定义（必须严格遵守）**：
- **A) 继续** → 仅当用户明确回复包含"A"或"继续"时才进入下一阶段
- **B) 修订/修正** → 仅当用户明确回复包含"B"或"修订/修正"时才返回当前阶段重新执行，不进入下一阶段
- **C) 暂停/暂存** → 仅当用户明确回复包含"C"或"暂停/暂存"时，**必须立即停止整个流水线，不再执行任何后续 Phase，不再加载任何后续技能，输出"流水线已暂停，当前产物已保留"后结束**
- **Y) 确认** → 仅当用户明确回复包含"Y"或"确认/是"时才执行确认操作
- **N) 否定** → 仅当用户明确回复包含"N"或"否/不"时才跳过当前操作

**用户回复解析规则**：
- 如果用户回复是单字母（如 "c"），必须严格按字母匹配对应选项
- 如果用户回复与所有选项均不匹配（如模糊表述、无关内容），必须重新输出询问提示，禁止猜测或假设用户意图
- 禁止将用户的任何回复默认解释为"继续"或"确认"

---

**Phase 1 确认提示（NOTE 类型）**：

```
Phase 1 已完成。产出物：整理后的笔记/文章
类型标记：NOTE
产物已保存至：src/main/java/io/aik/steins/grimoire/scrolls/{name}/README.md
内容结构：[章节数/字数]
资料来源：[URL/文档/用户描述]
质量检查：[通过/未通过]
是否继续进入 Phase 2（内容落盘）？
选项：A) 继续落盘  B) 修订内容  C) 暂存暂停
```

**Phase 1 确认提示（COMPONENT/SOLUTION/CODE 类型）**：

```
Phase 1 已完成。产出物：Component Manual v1.0
产物已保存至：doc/ComponentManual-{code}.md
关键指标：[耦合点数/敏感数据数/类清单/类型标记]
质量检查：[通过/未通过]
是否继续进入 Phase 2？
选项：A) 继续复写  B) 修订手册  C) 暂存手册暂停
```

**Phase 2 确认提示（NOTE 类型）**：

```
Phase 2 已完成。产出物：卷轴内容
类型标记：NOTE
产物路径：src/main/java/io/aik/steins/grimoire/scrolls/{name}/README.md
内容质量：[通过/未通过]
格式规范：[通过/未通过]
是否继续进入 Phase 3（元数据入库）？
选项：A) 确认交付  B) 修正特定内容  C) 补充遗漏
```

**Phase 2 确认提示（COMPONENT/SOLUTION/CODE 类型）**：

```
Phase 2 已完成。产出物：[文件清单/代码片段内容]
类型标记：[COMPONENT/SOLUTION/CODE]
产物路径/存储：[components/ 或 solutions/ 或 content字段]
三层审查：[全部通过/部分未通过]
残留业务信息检查：[清零/有残留]
测试用例数：[N]
是否继续进入 Phase 3（元数据入库）？
选项：A) 确认交付  B) 修正特定文件/片段  C) 补充遗漏
```

**Phase 3 确认提示**：

```
Phase 3 元数据入库确认：

知识条目信息：
- 标题：[条目名称]
- 编码：[code]
- 类型：[NOTE(1)/COMPONENT(2)/SOLUTION(3)/CODE(4)]
- 摘要：[用途描述]
- 资源路径：[文件包路径，CODE类型为空，NOTE类型为src/main/java/io/aik/steins/grimoire/scrolls/{name}/]
- 正文/片段：[content预览，CODE类型显示代码片段]

数据库表状态：[就绪/未创建（需先执行 sql/aik_knowledge_tables.sql）]

是否将该条目元数据存入知识库？
选项：Y) 入库  N) 暂不入库，仅保留产物
```

---

## 三个阶段也可独立使用

如果只需要萃取不需要复写，或之前已萃取现在想复写：

```bash
# 仅萃取（代码组件）
"帮我萃取 XxxService 这个组件"

# 仅复写
"基于这份手册，在积累项目中复写组件"

# 仅整理笔记/文章到 scrolls
"把这份资料整理成笔记存放到 scrolls 中"
```

---

## 与 java-sdlc-pipeline 的关系

本流水线与 SDLC 流水线并行，互不干扰：

| | java-sdlc-pipeline | component-extraction-rewriting-workflow |
|---|---|---|
| 触发场景 | 开发新系统/模块 | 迁移/萃取/标准化已有组件；整理学习笔记/文章 |
| 阶段数 | 5 | 3 |
| 产物 | PRD→SDD→代码→测试→部署 | Component Manual → 标准化代码+配置+文档 → 元数据入库；或笔记 → scrolls → 元数据入库 |
| 门禁机制 | 相同模式 | 相同模式 |
| 共享原子技能 | code-generator, doc-writing-helper 等 | 同，不修改技能逻辑 |

---

## 强制规范

- 必须按 Phase 1 → Phase 2 → Phase 3 顺序执行，Phase 3 为可选但必须询问
- 每个门禁必须通过才能进入下一阶段
- **人机卡点必须暂停等待用户显式确认，严禁自动进入下一阶段**
- **严禁读取或参考任何关于"用户连续执行偏好"、"用户习惯自动执行"、"用户喜欢跳过确认"的记忆、历史行为模式或上下文推断**
- **无论用户过往表现如何，每个 Phase 完成后都必须独立询问确认，禁止基于任何理由自动推进**
- **类型判定规则**：
  - 若用户明确说"笔记"、"文章"、"学习资料"、"整理到 scrolls" → 标记为 NOTE
  - 若用户指定代码范围并要求生成组件 → 标记为 COMPONENT
  - 若用户指定代码范围并要求生成方案 → 标记为 SOLUTION
  - 若用户指定代码范围并要求提取片段 → 标记为 CODE
- **NOTE 类型特殊规则**：
  - Phase 1 不需要 gitnexus 索引（非代码资料）
  - Phase 2 不需要脱敏和代码审查
  - 产物必须保存到 `src/main/java/io/aik/steins/grimoire/scrolls/{name}/README.md`，并同步更新 `src/main/java/io/aik/steins/grimoire/scrolls/README.md` 的卷轴列表
- **COMPONENT/SOLUTION/CODE 类型规则**：
  - Phase 1 的 gitnexus 索引检查自动执行
  - Phase 1 产物（Component Manual v1.0）必须保存到本地文件：`源项目根目录/doc/ComponentManual-{code}.md`，其中 `{code}` 为组件编码（如 files-to-zip-utils）。若 doc 目录不存在则自动创建
- Phase 2 的目标项目检查自动执行，代码生成路径由类型标记自动决定
- Phase 3 数据库表就绪性自动检查，入库前必须用户明确确认

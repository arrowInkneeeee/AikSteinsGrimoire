---
name: component-extract-rewrite
description: '从外部项目、任意工作空间或网站萃取有效知识并沉淀到魔典（AikSteinsGrimoire）时使用：先检测 WSL2/Windows 环境、定位本机魔典，再按 NOTE/COMPONENT/SOLUTION/CODE 分类、脱敏、落盘并生成入库清单；也支持组件 6 维分析、10 章手册与标准化复写。'
type: Skill
version: 2.0.0
trigger:
  - 分析组件
  - 萃取组件
  - 组件复写
  - 代码脱敏
  - 标准化迁移
  - 完整萃取复写
  - 知识摄取
  - 知识入库
  - 网页知识
  - 学习笔记
  - 从网站总结
  - 提取知识点
  - 组件知识萃取
  - component-code-analyzer
  - component-code-rewriter
  - spec-component-extractor
  - spec-component-rewriter
  - component-extraction-rewriting-workflow
---

# component-extract-rewrite

> **落盘确认门禁（DSH workspace-write）**：本技能会创建或修改工作区文件。每次写入前先列出将创建/修改的文件路径与摘要，等用户确认后再写；一次流程结束给出变更清单。

多源知识摄取与组件萃取复写。覆盖两类工作，共用同一条"先定位魔典、再落到魔典"的流水线：

1. **知识摄取**：从外部项目 / 任意工作空间 / 网站 / 用户粘贴文本中萃取有效知识，总结归类后沉淀到魔典。
2. **组件复写**：从外部项目分析组件、产出 10 章知识手册，再脱敏复写为标准代码落到魔典。

**关键事实（不要猜）**：
- 魔典 = 本机物理机上的 `AikSteinsGrimoire` 项目（Windows `D:\...`；WSL2 下 `/mnt/d/...`）。两者是**同一份文件**，不复制、不同步。
- `aIk` 是固定标识：`AIK_GRIMOIRE_HOME`、`aIk-coding-style`、`@author a I k .` 等一律保持原样。
- 外部萃取的知识属于"外部知识"；`grimoire-files/learning-notes/` 只放用户明确指定"给 AI 学习"的整理材料，**不自动写**。
- 网站知识默认"**总结归纳 + 出处**"，不搬运全文。

## 0. 定位魔典（所有落盘类子命令的前置）

完整协议见 [references/grimoire-location-chain.md](references/grimoire-location-chain.md)。要点：

1. **先检测环境**：WSL2 / Windows 原生 / 独立 VM（有共享目录）/ 容器；不同环境路径形态不同。
2. **身份验证**：`grimoire-files/README.md`、`grimoire-scrolls/README.md`、`src/.../knowledge/`、`pom.xml` 含 `AikSteinsGrimoire`、git remote 命中（扫描候选必须全过）。
3. **只读顺序**：cwd 祖先 → machine anchor（书签，`~/.aik/grimoire-anchor.json`）→ `AIK_GRIMOIRE_HOME`（只读）→ 有界扫描 → 问用户。
4. **失败即停**：`NEED_PATH` 时停下问用户；禁止写 `doc/_grimoire_pending/`、禁止自动写环境变量、禁止全盘搜索。
5. **多候选即问**：`AMBIGUOUS` 必须让用户选，不自动挑"最近的"。
6. **写策略门禁**：魔典不在当前会话 workspace 内时，先停，让用户切到"以魔典为 workspace"的会话或显式授权越界写。

```bash
# 只读定位（DSH/WSL）
bash scripts/locate-grimoire.sh --json
```

## 子命令

### locate

只输出环境类型、魔典路径、身份验证结果与候选清单，**不做任何写入**。

- **触发条件**：用户说"魔典在哪"、"定位知识库"、"检查环境"。
- **输入**：无；可选 `--root <dir>` 指定扫描根。
- **输出**：`envType / strategy / path / source / remote / head`；失败输出 `NEED_PATH` + 已尝试路径；多候选输出 `AMBIGUOUS`。
- **步骤**：执行 `scripts/locate-grimoire.sh --json` → 解析结果 → 失败/多候选时询问用户。
- **人机协作**：候选选择；如需写 anchor 书签，必须先取得用户批准（`--remember`）。

---

### ingest

多源知识摄取：**采集 → 分类 → 脱敏 → 落盘 → 清单（可选入库）**。

- **触发条件**：用户说"萃取这个知识"、"整理这个网页"、"沉淀到魔典"、"把 XX 入库"、"从网站总结"。
- **输入**：
  - 源（四选一或多选）：`local-repo` 外部项目路径、`workspace-doc` 文档目录、`web-page` URL、`pasted-text` 粘贴文本；
  - 重点/用途（可选）；期望类型（可选，缺省由判定树决定）。
- **输出**：
  - 落到魔典的 Markdown / 代码文件；
  - `ingest-manifest.json`（来源、目标路径、SHA-256、入库状态）；
  - `ingest-reports/{runId}.md` + 默认 `{runId}.sql`（**默认不执行**）。
- **执行步骤**：
  1. 定位魔典（§0），未通过则停止。
  2. 按源类型采集（[references/source-adapters.md](references/source-adapters.md)），记录来源/抓取时间/许可。
  3. 知识分类（判定树见 [references/knowledge-landing.md](references/knowledge-landing.md) §1）；歧义时问用户。
  4. 规范化 + 脱敏（[references/desensitization-patterns.md](references/desensitization-patterns.md)）+ 去重（路径/`uk_code`/SHA-256）。
  5. **落盘计划确认**：列出将创建/修改的文件、目标类型、摘要，等用户确认。
  6. 写入魔典（只写 `$GRIMOIRE`）；更新索引。
  7. 生成 manifest + SHA-256；生成 SQL 草案。
  8. 若用户明确要求入库执行（`db=apply`），按 [references/knowledge-landing.md](references/knowledge-landing.md) §5.2 执行（探活 → dry-run → 确认 → 事务 → 校验）；执行器为 `scripts/knowledge-import.mjs`（默认只生成 SQL 草案）。
  9. 输出运行报告。
- **人机协作节点**：落盘清单确认、分类歧义确认、入库确认。
- **停止条件**：源不可达/需登录、发现疑似凭据、同 `code` 冲突且内容不同、写策略不允许。

---

### analyze

对指定的外部代码范围做 6 维分析，产出结构化分析报告（不落盘）。

- **触发条件**：用户说"分析这个类"、"分析这个包"、"提取可复用逻辑"。
- **输入**：源项目路径（纯英文路径）、代码范围（类/包/目录）；GitNexus 可选增强。
- **输出**：6 维分析报告（架构模式 / 业务耦合点 / 数据依赖 / 敏感数据 / 通用性 / 依赖关系）。
- **步骤**：加载 [references/coupling-patterns.md](references/coupling-patterns.md) → 有索引用 GitNexus，无索引用 grep/read → 逐维分析 → 输出报告。
- **人机协作**：确认分析范围与结论。

---

### extract

从外部项目萃取组件知识，产出 10 章结构化组件手册。

- **触发条件**：用户说"萃取组件"、"提取组件手册"、"生成组件文档"。
- **输入**：源项目路径、组件范围。
- **输出**：10 章组件手册（Markdown）。
- **步骤**：加载 [references/component-manual-structure.md](references/component-manual-structure.md) → 按 10 章生成 → 落盘计划确认 → 写入 `grimoire-files/component-manuals/{code}.md`（路由见 [references/knowledge-landing.md](references/knowledge-landing.md) §1）。
- **人机协作**：确认组件范围、耦合点完整性、手册内容。

---

### rewrite

基于组件手册，把源代码脱敏复写为符合 `aIk-coding-style` 的标准代码。

- **触发条件**：用户说"复写组件"、"代码脱敏"、"标准化复写"。
- **输入**：组件知识手册、目标项目路径；目标项目已有通用组件（本仓库为 `ApiResponse`/BaseEntity 等）。
- **输出**：脱敏后的标准化代码。
- **步骤**：加载 [references/rewriter-delivery-spec.md](references/rewriter-delivery-spec.md) + [references/desensitization-patterns.md](references/desensitization-patterns.md) → 读取手册 → 复写 → 三层审查（**引用 `code-review` 技能**的 quality/security，不复制规则）→ 落盘计划确认 → 写入目标项目。
- **人机协作**：确认脱敏规则、复写代码、目标项目可编译。

---

### full-pipeline

完整流水线：定位 → 摄取/分析 → 萃取 → 复写 → 落盘 → 校验。

- **触发条件**：用户说"完整萃取复写"、"把这个组件迁移到新项目"、"把这批知识沉淀进魔典"。
- **输入**：源（项目/文档/URL）、范围、目标项目路径（可选）。
- **输出**：手册 + 代码 + 落盘清单 + 运行报告。
- **步骤**：`locate` → `ingest`（或 `analyze`+`extract`）→ `rewrite`（如需要）→ 落盘 → 索引 → manifest → SQL 草案 → 校验。
- **人机协作**：每阶段确认；入库默认否。

## 知识类型与落点

| type | 适用 | 落点 |
|---|---|---|
| NOTE(1) | 外部文章/教程/技巧总结 | `grimoire-scrolls/{slug}/{slug}.md` |
| NOTE(1) | 用户明确指定"给 AI 学习" | `grimoire-files/learning-notes/{slug}.md`（仅用户要求时） |
| COMPONENT(2) | 组件手册 | `grimoire-files/component-manuals/{code}.md` |
| COMPONENT(2) | 可编译组件代码 | `src/main/java/io/aik/steins/grimoire/components/{code}/` |
| SOLUTION(3) | 方案（问题/步骤/验证） | `src/main/java/io/aik/steins/grimoire/solutions/{code}/` |
| CODE(4) | 代码片段 | 不落文件；仅 `aik_knowledge.content` |

详细路由、文件格式、索引更新、manifest、入库 SQL 与校验见 [references/knowledge-landing.md](references/knowledge-landing.md)。

## 源类型

| 源 | 输入 | 采集工具 | 限制 |
|---|---|---|---|
| `local-repo` | 项目路径 + 范围 | 文件工具、bash grep、git（只读）；GitNexus 可选 | 不写源项目；无索引时 grep/read |
| `workspace-doc` | 文档目录/文件 | 文件工具、grep | 只读；二进制按需专用阅读器 |
| `web-page` | URL（一个或多个） | `web_fetch` + `read_page`；图片 modlens；PDF 按需 | 不抓登录/付费墙；默认只做总结+出处 |
| `pasted-text` | 用户粘贴 | 无 | 需声明来源/许可 |

详见 [references/source-adapters.md](references/source-adapters.md)。

## 输入契约

| 输入项 | 路径/来源 | 必须 | 降级策略 |
|---|---|---|---|
| 魔典路径 | 定位协议 | 是（落盘类） | 失败即停，问用户 |
| 源路径/URL/文本 | 用户指定 | 是（ingest/analyze/extract） | 网页失败可改粘贴文本 |
| 组件范围 | 用户指定 | 是（analyze/extract/rewrite） | 无 |
| 目标类型 | 用户指定或分类判定树 | 否 | 歧义时问用户 |
| 目标项目路径 | 用户指定 | 否（rewrite） | 当前项目 |
| GitNexus 索引 | 可选增强 | 否 | 无索引时 grep/read，不阻断 |

## 输出契约

| 输出项 | 路径 | 格式 |
|---|---|---|
| 分析报告 | 内联输出 | Markdown |
| 组件手册 | `grimoire-files/component-manuals/{code}.md` | Markdown（10 章） |
| 知识笔记 | `grimoire-scrolls/{slug}/{slug}.md` | Markdown |
| 标准化代码 | `src/.../components/{code}/` 或目标项目 | Java |
| ingest manifest | `ingest-reports/{runId}.json`（随报告） | JSON（schema 见 assets） |
| SQL 草案 | `grimoire-files/plans/ingest-reports/{runId}.sql` | SQL |
| 运行报告 | `grimoire-files/plans/ingest-reports/{runId}.md` | Markdown |

## 工作流上下文

**上游技能**：无（独立技能，可由用户随时触发）。

**下游技能**：
- `code-review`：对复写代码做质量/安全审查。
- `code-implementation`：在复写代码基础上扩展功能。
- `handoff-bundle`：任务需要跨会话交接时。

## 质量门禁

| 检查点 | 条件 | 动作 |
|---|---|---|
| 定位验证 | `locate` 返回 OK 且身份验证通过 | 继续；否则停止问用户 |
| 来源确认 | 每个源的路径/URL 可见且用户确认 | 继续 |
| 分类确认 | 类型无歧义或用户已选 | 继续 |
| 落盘确认 | 文件清单/摘要已确认、无覆盖冲突 | 继续 |
| 脱敏检查 | 无凭据/隐私残留 | 继续 |
| 入库确认 | 默认生成 SQL 草案；执行需用户明确批准 | 继续/停止 |
| 校验 | manifest 与文件 SHA-256 一致、索引已更新 | 完成 |

## 约束

- 源项目路径必须为纯英文（避免编码问题；WSL 下即 `/mnt` 挂载路径）。
- 所有复写代码必须遵循 `aIk-coding-style` 规范；统一响应类按项目现有（本仓库为 `ApiResponse`）。
- 敏感数据必须脱敏；业务术语替换为通用术语。
- 组件手册使用 10 章标准结构。
- 产出路径遵循 `references/grimoire-location-chain.md` 定位结果 + `references/knowledge-landing.md` 路由。
- 不复制技能库/知识库到其他位置；WSL 与 Windows 是同一份文件，不做"同步"。

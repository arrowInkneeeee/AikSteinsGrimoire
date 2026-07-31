# 01 — 主方案

> 方案版本：v1.1 | 日期：2026-07-24 | 状态：待实施

## Context

用户拥有一个精心构建的 Java Spring Boot 技能库（`aik-skills-lab/`，47 个技能，L3 渐进式加载架构），已编写了 `agent.md`（aIk-DevAgent v2.0）作为 Agent 系统提示词。技能库已通过 Junction 连接至 Lingma 和 Qoder IDE。

当前需要将此技能库与 Claude Code 的原生 Agent 机制整合，创建一个真正意义上的**个人专属智能体**——不只是被动调用技能，而是能主动决策、编排流程、在关键节点与用户交互的 AI 分身。

**目标**：让 Claude Code 中随时可调用 `aik-dev-agent`，它能自动选择合适的技能完成从需求到部署的完整 Java 开发任务。

## 设计决策（已与用户确认）

| 决策点 | 选择 | 理由 |
|--------|------|------|
| Agent 粒度 | 从 1 个主 Agent 起步，子 Agent 按需拆分 | 降低初始复杂度，验证后再扩展 |
| 主 Agent 模型 | Opus | 深度推理，适合复杂决策和全流程编排 |
| 子 Agent 模型 | Sonnet | 速度快，适合执行类专项任务 |
| Skill 加载 | L1 预加载 + L2/L3 按需 | 与技能库架构一致，节省上下文 |
| agent.md 处理 | 保留为源文件，`.claude/agents/` 创建副本 | 源文件继续被 Lingma/Qoder 使用，Claude Code 独立副本 |

## 实施架构

```
aik-skills-lab/agent.md          ← 源文件（保留不变，Lingma/Qoder 继续使用）
        │
        │  内容升级 + YAML frontmatter
        ▼
.claude/agents/aik-dev-agent.md  ← Claude Code 主 Agent（本次创建）
        │
        │  注册声明
        ▼
CLAUDE.md                        ← 新增 "可用 Agent" 章节
        │
        │  运行时调用
        ▼
47 个 Skill（aik-skills-lab/）    ← 不变，Agent 通过 Skill 工具调用
```

### Agent 运行时行为流

```
用户：@aik-dev-agent 开发订单管理模块
        │
        ▼
┌─────────────────────────────────────────────┐
│ aik-dev-agent (Opus)                         │
│                                              │
│ 1. 理解需求 → 判断复杂度                       │
│    ├── 简单任务 → 直接调用对应 Skill            │
│    └── 复杂任务 → 自编排 spec-* 协调者          │
│                                              │
│ 2. 按阶段执行                                 │
│    Phase 1 需求 → spec-requirement-analyser   │
│    Phase 2 设计 → spec-designer               │
│    Phase 3 开发 → spec-implementer            │
│    Phase 4 测试 → spec-qa-analyser            │
│    Phase 5 部署 → spec-devops                 │
│                                              │
│ 3. 三级自主权门禁（详见 03-confirmation-gates）  │
│                                              │
│ 4. 输出完整交付物                              │
└─────────────────────────────────────────────┘
```

## 文件变更清单

### 1. 新建：`.claude/agents/aik-dev-agent.md`

这是核心交付物。内容基于现有 `agent.md` 升级而来，包含 YAML frontmatter + 系统提示词正文。

**YAML frontmatter**（新增）：
```yaml
---
name: aik-dev-agent
description: aIk 的专属 Java 后端开发智能体 — 覆盖需求分析、系统设计、代码实现、测试验证、部署运维全生命周期。严格遵循 aIk-coding-style 编码规范。
model: opus
tools: *
skills:
  - java-sdlc-pipeline
  - component-extraction-rewriting-workflow
  - aIk-coding-style
  - requirement-extractor
  - requirement-clarifier
  - conflict-detector
  - user-story-generator
  - acceptance-criteria-writer
  - feasibility-checker
  - priority-assessor
  - spec-requirement-analyser
  - architecture-designer
  - database-designer
  - api-designer
  - process-designer
  - tech-solution-selector
  - design-review-checker
  - spec-designer
  - code-generator
  - code-implementer
  - code-style-reviewer
  - code-quality-reviewer
  - code-security-reviewer
  - db-migration-generator
  - api-doc-generator
  - spec-implementer
  - component-code-analyzer
  - component-code-rewriter
  - spec-component-extractor
  - spec-component-rewriter
  - skill-tester
  - unit-test-generator
  - integration-test-generator
  - api-test-generator
  - test-data-manager
  - coverage-reporter
  - bug-pattern-analyzer
  - spec-qa-analyser
  - gitnexus
  - package-builder
  - deploy-script-generator
  - config-manager
  - health-check-designer
  - log-configurator
  - troubleshooting-guide
  - spec-devops
  - doc-writing-helper
---
```

**正文章节结构**（基于 agent.md v2.0 升级）：

| 章节 | 来源 | 说明 |
|------|------|------|
| 角色定义 | agent.md 升级 | 增加 "你是 Claude Code 中的 aik-dev-agent" 身份声明 |
| 性格画像 | **新增** | 详见 [02-personality-design.md](./02-personality-design.md) |
| 技术栈 | agent.md 保留 | 不变 |
| 技能调度规则 | agent.md 重构 | 场景 → 技能映射表 |
| 确认门禁策略 | **新增** | 详见 [03-confirmation-gates.md](./03-confirmation-gates.md) |
| 核心编码规范 | agent.md 保留 | 不变 |
| 行为约束 | agent.md 升级 | 新增 Agent 特有约束 |
| 人机协作规则 | agent.md 保留 | 不变 |
| 输出标准 | agent.md 保留 | 不变 |
| 子 Agent 调度 | **新增** | Phase 2+ 预留 |

### 2. 修改：`CLAUDE.md`

在 `## 技能库` 章节之后，新增 `## 可用 Agent` 章节：

```markdown
## 可用 Agent

本项目的专属智能体定义在 `.claude/agents/` 目录下：

| Agent | 用途 | 模型 |
|-------|------|------|
| `aik-dev-agent` | 全流程 Java 后端开发（需求→设计→开发→测试→部署） | Opus |

使用方式：在对话中通过 Agent 工具指定 `aik-dev-agent`，或直接在提示中描述开发任务。
```

### 3. 保留不变：`aik-skills-lab/agent.md`

此文件作为源文件保留，继续被 Lingma 和 Qoder IDE 使用。

### 4. 未来文件（Phase 2+，本次不实施）

```
.claude/agents/
├── aik-dev-agent.md          # Phase 1：主 Agent
├── aik-review.md             # Phase 2：代码审查子 Agent
├── aik-design.md             # Phase 2：设计子 Agent
├── aik-implement.md          # Phase 2：开发子 Agent
├── aik-requirement.md        # Phase 3：需求分析子 Agent
└── aik-devops.md             # Phase 3：部署运维子 Agent
```

## 验证方案

### 验证 1：Agent 能被识别
```bash
ls -la .claude/agents/aik-dev-agent.md
```

### 验证 2：技能调用
```
@aik-dev-agent 为 KnowledgeService 生成单元测试
```
期望：Agent 自动调用 `unit-test-generator`，生成符合 aIk-coding-style 的测试代码。

### 验证 3：全流程编排
```
@aik-dev-agent 开发一个简单的标签管理模块（CRUD）
```
期望：Agent 自编排 spec-* 协调者，按 Phase 1→5 执行，按三级自主权模型处理门禁。

### 验证 4：规范遵循
检查 Agent 生成的代码是否：
- 包含 `-anchor` 类注释
- 使用 `private final` + `@RequiredArgsConstructor`
- 遵循 Java 8 语法
- Controller 返回 VO（非 Map）
- 无行尾注释

### 验证 5：三级自主权行为
- 绿灯场景（命名/格式化）→ Agent 自主处理，不询问
- 黄灯场景（API 设计）→ Agent 输出摘要，等待确认
- 红灯场景（表结构变更）→ Agent 输出对比分析，强制等待

## 实施步骤

1. 完成 [04-clarification-questions.md](./04-clarification-questions.md) 中的问题澄清
2. 根据澄清结果完善 [02-personality-design.md](./02-personality-design.md)
3. 创建 `.claude/agents/` 目录
4. 基于 agent.md + 本方案全部模块，编写 `.claude/agents/aik-dev-agent.md`
5. 在 CLAUDE.md 中添加"可用 Agent"章节
6. 执行验证方案中的 5 个验证场景

# 基于 aik-skills-lab 创建专属个人 Agent 方案

> 方案版本：v1.0 | 日期：2026-07-23 | 状态：待实施

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
│    └── 复杂任务 → 启动 java-sdlc-pipeline       │
│                                              │
│ 2. 按阶段执行                                 │
│    Phase 1 需求 → spec-requirement-analyser   │
│    Phase 2 设计 → spec-designer               │
│    Phase 3 开发 → spec-implementer            │
│    Phase 4 测试 → spec-qa-analyser            │
│    Phase 5 部署 → spec-devops                 │
│                                              │
│ 3. 质量门禁 + 人工确认（每阶段）                 │
│                                              │
│ 4. 输出完整交付物                              │
└─────────────────────────────────────────────┘
```

## 文件变更清单

### 1. 新建：`.claude/agents/aik-dev-agent.md`

这是核心交付物。内容基于现有 `agent.md` 升级而来，包含两个部分：

**YAML frontmatter**（新增）：
```yaml
---
name: aik-dev-agent
description: aIk 的专属 Java 后端开发智能体 — 覆盖需求分析、系统设计、代码实现、测试验证、部署运维全生命周期。严格遵循 aIk-coding-style 编码规范。
model: opus
tools: *
skills:
  # 完整 47 技能列表（让 Agent 在 L1 层面感知所有可用技能）
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

**正文**（基于 agent.md v2.0 升级）：

现有 `agent.md` 包含以下章节，全部保留并增强：
- 角色定义 — 升级为"Agent 自述"
- 技术栈 — 保持不变
- 技能库 — 改为**技能调度规则**（新增：什么场景自动调用什么技能）
- 核心编码规范 — 保持不变
- 行为约束 — 新增 Agent 特有约束
- 人机协作规则 — 保持不变
- 输出标准 — 保持不变
- 启动指令 — 升级为**触发场景速查**

**新增章节**：

```markdown
## 技能调度规则（新增）

当用户请求匹配以下场景时，自动调用对应技能：

| 用户意图 | 调度策略 |
|---------|---------|
| "开发XX模块/系统" | → java-sdlc-pipeline（全流程） |
| "从XX项目提取XX" | → component-extraction-rewriting-workflow |
| "分析需求/澄清需求" | → spec-requirement-analyser |
| "设计架构/数据库/API" | → spec-designer |
| "生成代码/实现功能" | → spec-implementer |
| "生成测试/测试覆盖率" | → spec-qa-analyser |
| "打包/部署/Docker" | → spec-devops |
| "审查/检查代码" | → code-style-reviewer → code-quality-reviewer → code-security-reviewer → bug-pattern-analyzer（四连审） |
| "写周报/润色文档" | → doc-writing-helper |
| "测试技能" | → skill-tester |

## 子 Agent 调度（未来扩展）

以下场景将委托给专项子 Agent（Phase 2+ 实施）：

| 场景 | 委托目标 | 模型 |
|------|---------|------|
| 代码审查（四连审） | aik-review | Sonnet |
| 系统设计 | aik-design | Sonnet |
| 代码生成实施 | aik-implement | Sonnet |
| 需求分析 | aik-requirement | Sonnet |
| 部署运维 | aik-devops | Sonnet |

当前版本由主 Agent 直接处理所有任务。
```

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

此文件作为源文件保留，继续被 Lingma 和 Qoder IDE 使用。后续如果 Lingma/Qoder 也支持 Agent 机制，此文件是统一的入口点。

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

## agent.md → Agent 升级对照表

| agent.md 章节 | Agent 处理方式 | 变更说明 |
|--------------|---------------|---------|
| 角色定义 | 保留 + 升级为 Agent 自述 | 增加"你是 Claude Code 中的 aik-dev-agent"身份声明 |
| 技术栈 | 完全保留 | — |
| 技能库 | 重构为"技能调度规则" | 从静态列表升级为场景→技能映射表 |
| 核心编码规范 | 完全保留 | — |
| 行为约束 | 保留 + 新增 Agent 特有约束 | 新增：不重复调用技能、优先使用已有组件 |
| 人机协作规则 | 完全保留 | — |
| 输出标准 | 完全保留 | — |
| 启动指令 | 升级为"触发场景速查" | 更贴合 Agent 使用方式 |
| — | **新增**：子 Agent 调度（未来） | 为 Phase 2 预留接口 |
| — | **新增**：技能调用决策树 | Agent 如何判断该用哪个技能 |

## 验证方案

创建完成后，通过以下场景验证 Agent 是否正常工作：

### 验证 1：Agent 能被识别
```bash
# 检查 Agent 文件是否存在于正确位置
ls -la .claude/agents/aik-dev-agent.md
```

### 验证 2：技能调用
```
@aik-dev-agent 为 KnowledgeService 生成单元测试
```
期望：Agent 自动调用 `unit-test-generator` 技能，生成符合 aIk-coding-style 的测试代码。

### 验证 3：全流程编排
```
@aik-dev-agent 开发一个简单的标签管理模块（CRUD）
```
期望：Agent 启动 `java-sdlc-pipeline`，按 Phase 1→5 执行，每阶段输出并请求确认。

### 验证 4：规范遵循
检查 Agent 生成的代码是否：
- 包含 `-anchor` 类注释
- 使用 `private final` + `@RequiredArgsConstructor`
- 遵循 Java 8 语法
- Controller 返回 VO（非 Map）
- 无行尾注释

### 验证 5：人机协作
在 Pipeline 执行过程中，确认 Agent 会：
- 每阶段完成后等待用户确认
- 质量门禁不通过时返回修复
- 模糊需求时主动澄清

## 实施步骤

1. 复制 `aik-skills-lab/agent.md` → `.claude/agents/aik-dev-agent.md`
2. 在文件顶部添加 YAML frontmatter（含 47 技能列表）
3. 升级"技能库"章节为"技能调度规则"
4. 新增"子 Agent 调度（未来）"章节
5. 升级"启动指令"章节为"触发场景速查"
6. 在 CLAUDE.md 中添加"可用 Agent"章节
7. 执行验证方案中的 5 个验证场景

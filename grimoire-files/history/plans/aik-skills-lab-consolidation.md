# aik-skills-lab 技能库整合改造方案

## Context

当前 aik-skills-lab 包含 47 个技能，其中 9 个编排器（orchestrator）构成两层调度链，38 个原子技能被编排器调用。随着 AI 能力提升，这种精细化分工带来过高的选择成本和编排复杂度。本方案将 47 个技能整合为 10 个，消灭编排层，采用子命令模式，同时保留所有有价值的模板、检查清单和决策矩阵。

## 整合前后对照

### 整合前：47 个技能

| 类别 | 数量 |
|------|------|
| 编排器（两层） | 9 |
| 需求分析原子 | 7 |
| 系统设计原子 | 6 |
| 代码生成 | 2 |
| 代码审查 | 3 |
| 组件萃取 | 2 |
| 测试 | 6 |
| 部署运维 | 6 |
| 文档/迁移/工具 | 4 |
| 保留不动 | 3 (aIk-coding-style, handoff-bundle, skill-tester) |

### 整合后：10 个技能

| # | 技能名 | 类型 | 来源 | 子命令 |
|---|--------|------|------|--------|
| 1 | aIk-coding-style | 保留 | 原样保留 | - |
| 2 | requirement-engineering | 新建 | 7个需求原子技能 | extract / clarify / conflict-check / user-story / feasibility / prioritize / full |
| 3 | system-design | 新建 | 6个设计原子技能 | architecture / database / api / process / tech-select / review / full |
| 4 | code-implementation | 新建 | code-generator + code-implementer + api-doc-generator + db-migration-generator | skeleton / implement / api-doc / db-migration / full |
| 5 | code-review | 新建 | 3个reviewer + bug-pattern-analyzer | style / quality / security / bug-pattern / full |
| 6 | testing | 新建 | 6个测试技能(去掉bug-pattern-analyzer归入code-review) | unit / integration / api / coverage / data / full |
| 7 | component-extract-rewrite | 新建 | 5个组件相关(2原子+3编排) | analyze / extract / rewrite / full-pipeline |
| 8 | devops | 新建 | 6个部署运维技能 | config / package / deploy / health / log / troubleshoot / full |
| 9 | handoff-bundle | 保留 | 原样保留 | - |
| 10 | skill-tester | 保留 | 原样保留 | - |

### 删除清单（37个目录）

**编排器（9个，直接删除）**：
java-sdlc-pipeline, spec-requirement-analyser, spec-designer, spec-implementer, spec-qa-analyser, spec-devops, component-extraction-rewriting-workflow, spec-component-extractor, spec-component-rewriter

**合并后删除（28个原子技能目录）**：
- 需求7个：requirement-extractor, requirement-clarifier, conflict-detector, user-story-generator, acceptance-criteria-writer, feasibility-checker, priority-assessor
- 设计6个：architecture-designer, database-designer, api-designer, process-designer, tech-solution-selector, design-review-checker
- 代码4个：code-generator, code-implementer, api-doc-generator, db-migration-generator
- 审查4个：code-style-reviewer, code-quality-reviewer, code-security-reviewer, bug-pattern-analyzer
- 测试5个：unit-test-generator, integration-test-generator, api-test-generator, coverage-reporter, test-data-manager
- 组件2个：component-code-analyzer, component-code-rewriter
- 部署6个：config-manager, deploy-script-generator, package-builder, health-check-designer, log-configurator, troubleshooting-guide
- 其他1个：doc-writing-helper

---

## 文件迁移矩阵

### 需要迁移的 assets 文件（16个模板）

| 源文件 | 目标位置 |
|--------|---------|
| spec-requirement-analyser/assets/prd-template.md | requirement-engineering/assets/prd-template.md |
| database-designer/assets/er-diagram-template.md | system-design/assets/er-diagram-template.md |
| api-designer/assets/api-contract-template.md | system-design/assets/api-contract-template.md |
| api-doc-generator/assets/api-doc-template.md | code-implementation/assets/api-doc-template.md |
| db-migration-generator/assets/sql-template.md | code-implementation/assets/sql-template.md |
| code-style-reviewer/assets/review-checklist.md | code-review/assets/style-checklist.md |
| code-quality-reviewer/assets/review-report-template.md | code-review/assets/quality-report-template.md |
| code-security-reviewer/assets/security-report-template.md | code-review/assets/security-report-template.md |
| unit-test-generator/assets/test-class-template.md | testing/assets/test-class-template.md |
| spec-qa-analyser/assets/test-plan-template.md | testing/assets/test-plan-template.md |
| spec-devops/assets/deploy-plan-template.md | devops/assets/deploy-plan-template.md |
| spec-devops/assets/ops-manual-template.md | devops/assets/ops-manual-template.md |

### 需要迁移的 references 文件

| 源文件 | 目标位置 |
|--------|---------|
| component-code-analyzer/references/coupling-patterns.md | component-extract-rewrite/references/coupling-patterns.md |
| component-code-rewriter/references/desensitization-patterns.md | component-extract-rewrite/references/desensitization-patterns.md |
| database-designer/references/README.md | system-design/references/db-design-guide.md |
| api-designer/references/README.md | system-design/references/api-design-guide.md |
| code-generator/references/README.md | code-implementation/references/skeleton-guide.md |
| code-implementer/references/README.md | code-implementation/references/implementation-guide.md |
| api-doc-generator/references/README.md | code-implementation/references/api-doc-guide.md |
| db-migration-generator/references/README.md | code-implementation/references/migration-guide.md |
| code-style-reviewer/references/README.md | code-review/references/style-guide.md |
| code-quality-reviewer/references/README.md | code-review/references/quality-guide.md |
| unit-test-generator/references/README.md | testing/references/unit-test-guide.md |

### 需要从 SKILL.md 中提取为独立 references 的内容

| 来源技能 | 提取内容 | 目标文件 |
|---------|---------|---------|
| conflict-detector | 4种冲突类型定义 + 语义相似度规则 | requirement-engineering/references/conflict-checklist.md |
| feasibility-checker | Java特定评估维度表 | requirement-engineering/references/java-feasibility-dimensions.md |
| priority-assessor | MoSCoW权重矩阵(40/30/20/10) | requirement-engineering/references/moscow-decision-matrix.md |
| design-review-checker | P0-P3审查维度清单 | system-design/references/design-review-checklist.md |
| tech-solution-selector | 选型决策树 + 对比表 | system-design/references/tech-selection-tree.md |
| process-designer | 事务传播行为 + 失败场景表 | system-design/references/transaction-patterns.md |
| coverage-reporter | 渐进阈值 + 排除规则 | testing/references/coverage-thresholds.md |
| test-data-manager | 4种数据管理策略 | testing/references/test-data-strategies.md |
| bug-pattern-analyzer | 8种缺陷模式 + 预防清单 | code-review/references/bug-patterns.md |
| spec-implementer | 编码约束(命名/注释/UTF-8) | 已由 aIk-coding-style 覆盖，不单独提取 |
| spec-qa-analyser | 质量门禁阈值 | testing/references/quality-gates.md |
| spec-devops | 部署方式决策矩阵 | devops/references/deployment-decision-matrix.md |
| component-extraction-rewriting-workflow | AIK_GRIMOIRE_HOME定位链 + 4种内容类型 | component-extract-rewrite/references/grimoire-location-chain.md |
| spec-component-extractor | 组件手册10章结构定义 | component-extract-rewrite/references/component-manual-structure.md |
| spec-component-rewriter | 交付物结构 + 3层审查流程 | component-extract-rewrite/references/rewriter-delivery-spec.md |

### 保留不动的目录

| 技能 | 保留内容 |
|------|---------|
| aIk-coding-style/ | 全部（SKILL.md + 18 references + 2 scripts） |
| handoff-bundle/ | 全部（SKILL.md + 1 asset + 3 references） |
| skill-tester/ | 全部（SKILL.md + 5 references + test-results/） |

---

## 实施步骤

### Step 1: 创建新技能目录结构

创建 7 个新目录及其子目录：

```
aik-skills-lab/
├── requirement-engineering/
│   ├── assets/
│   └── references/
├── system-design/
│   ├── assets/
│   └── references/
├── code-implementation/
│   ├── assets/
│   └── references/
├── code-review/
│   ├── assets/
│   └── references/
├── testing/
│   ├── assets/
│   └── references/
├── component-extract-rewrite/
│   └── references/
├── devops/
│   ├── assets/
│   └── references/
```

### Step 2: 迁移 assets 文件

按迁移矩阵复制 12 个模板文件到新目录。

### Step 3: 迁移 references 文件

按迁移矩阵复制 11 个已有 references 文件。

### Step 4: 从旧 SKILL.md 提取内容写入新 references

从 14 个旧技能中提取关键检查清单/决策矩阵/模式表，写入对应新目录的 references/。

### Step 5: 编写 7 个新 SKILL.md

每个 SKILL.md 包含以下结构：

```markdown
# 技能名称
- type: Skill
- version: 1.0.0
- description: 一句话描述
- trigger: 触发词列表（包含旧技能名作为别名）

## 子命令
### sub-command-1
- 触发条件
- 输入
- 输出
- 执行步骤

### sub-command-2
...

## 完整流程模式（full）
- 当用户说"从头做XX"时，按顺序执行所有子命令
- 每个子命令完成后询问用户确认

## 输入契约
- 主要输入文件路径
- 如果上游文件不存在时的降级策略

## 输出契约
- 产出文件路径和格式要求

## 工作流上下文
- 上游技能及产出
- 下游技能及消费

## 质量门禁
- 从原编排器继承的检查点

## 约束
- 所有代码产出必须遵循 aIk-coding-style
```

### Step 6: 删除旧目录

删除 37 个旧技能目录。

### Step 7: 更新 agent.md

更新 aik-skills-lab/agent.md 中的技能索引（如有）。

### Step 8: 验证

- 确认 10 个技能目录结构完整
- 确认所有 assets/references 文件已正确迁移
- 确认无遗漏的有价值内容

---

## 各新技能 SKILL.md 设计要点

### requirement-engineering

**子命令**：
- `extract`：从非结构化文本提取功能点（原 requirement-extractor）
- `clarify`：识别模糊点并生成澄清问题（原 requirement-clarifier）
- `conflict-check`：检测矛盾/重复/依赖缺失（原 conflict-detector，使用 references/conflict-checklist.md）
- `user-story`：转为标准用户故事格式（原 user-story-generator + acceptance-criteria-writer）
- `feasibility`：技术可行性评估（原 feasibility-checker，使用 references/java-feasibility-dimensions.md）
- `prioritize`：MoSCoW 优先级排序（原 priority-assessor，使用 references/moscow-decision-matrix.md）
- `full`：完整需求分析流程，产出 PRD（原 spec-requirement-analyser 的完整流程）

**从编排器继承的关键内容**：
- PRD 模板（assets/prd-template.md，8节结构）
- 2个人工协作节点（澄清环节、冲突解决）
- 质量门禁：PRD 无高严重度冲突

### system-design

**子命令**：
- `architecture`：分层架构设计（原 architecture-designer）
- `database`：数据库表设计（原 database-designer）
- `api`：API 接口设计（原 api-designer）
- `process`：业务流程设计（原 process-designer，使用 references/transaction-patterns.md）
- `tech-select`：技术方案选型（原 tech-solution-selector，使用 references/tech-selection-tree.md）
- `review`：设计评审（原 design-review-checker，使用 references/design-review-checklist.md）
- `full`：完整系统设计流程，产出 SDD

**从编排器继承的关键内容**：
- SDD 模板（assets/sdd-template.md，8节结构）
- 设计前检查：必须检查项目现有通用组件（Result, BaseEntity, PageDTO）和 pom.xml 依赖
- 质量门禁：设计评审 <=2 警告 / 0 失败

### code-implementation

**子命令**：
- `skeleton`：生成代码骨架（原 code-generator）
- `implement`：填充业务逻辑（原 code-implementer）
- `api-doc`：生成 API 文档（原 api-doc-generator）
- `db-migration`：生成数据库迁移脚本（原 db-migration-generator）
- `full`：从 SDD 到完整可运行代码

**从编排器继承的关键内容**：
- spec-implementer 的 3 个人工确认点（数据库类型、全局异常处理、配置类）
- 编码约束内联摘要（命名规范、注释规范、UTF-8 无 BOM）
- 产出结构：controller/service/dao/entity/dto/vo/common + tests + docs/api

### code-review

**子命令**：
- `style`：命名/注释/格式审查（原 code-style-reviewer）
- `quality`：N+1/空指针/事务审查（原 code-quality-reviewer）
- `security`：SQL注入/越权审查（原 code-security-reviewer）
- `bug-pattern`：缺陷模式分析（原 bug-pattern-analyzer，使用 references/bug-patterns.md）
- `full`：三维全面审查

### testing

**子命令**：
- `unit`：JUnit 5 + Mockito 单元测试（原 unit-test-generator）
- `integration`：@SpringBootTest 集成测试（原 integration-test-generator）
- `api`：API 测试文档生成（原 api-test-generator）
- `coverage`：JaCoCo 覆盖率配置（原 coverage-reporter，使用 references/coverage-thresholds.md）
- `data`：测试数据管理策略（原 test-data-manager，使用 references/test-data-strategies.md）
- `full`：完整测试流程，产出测试计划 + 测试代码 + 覆盖率报告

**从编排器继承的关键内容**：
- 测试计划模板（assets/test-plan-template.md，12节结构）
- 质量门禁：入口(行覆盖>60%, 分支>50%) → 出口(行覆盖>70%, 分支>60%)

### component-extract-rewrite

**子命令**：
- `analyze`：6维代码分析（原 component-code-analyzer）
- `extract`：从源项目萃取组件手册（原 spec-component-extractor 的完整流程）
- `rewrite`：脱敏复写（原 component-code-rewriter）
- `full-pipeline`：萃取 + 复写 + 入库（原 component-extraction-rewriting-workflow）

**从编排器继承的关键内容**：
- AIK_GRIMOIRE_HOME 定位链（references/grimoire-location-chain.md）
- 4种内容类型路由（NOTE/COMPONENT/SOLUTION/CODE）
- 组件手册10章结构（references/component-manual-structure.md）
- 3个人工检查点
- 产出路径解析规则

### devops

**子命令**：
- `config`：多环境配置管理（原 config-manager）
- `package`：构建部署包（原 package-builder）
- `deploy`：生成部署脚本（原 deploy-script-generator）
- `health`：健康检查配置（原 health-check-designer）
- `log`：日志系统配置（原 log-configurator）
- `troubleshoot`：故障排查手册（原 troubleshooting-guide）
- `full`：完整部署方案，产出部署计划 + 运维手册

**从编排器继承的关键内容**：
- 部署计划模板（assets/deploy-plan-template.md，12节结构）
- 运维手册模板（assets/ops-manual-template.md，10节结构）
- 部署方式决策矩阵（references/deployment-decision-matrix.md）
- 部署检查清单（前/中/后）

---

## 验证清单

- [ ] 10 个技能目录存在且结构完整
- [ ] 12 个 assets 模板文件已迁移
- [ ] 11 个已有 references 文件已迁移
- [ ] 14 个新提取的 references 文件已创建
- [ ] 7 个新 SKILL.md 包含子命令定义
- [ ] 7 个新 SKILL.md 包含输入/输出契约
- [ ] 7 个新 SKILL.md 包含工作流上下文
- [ ] 7 个新 SKILL.md 包含从编排器继承的质量门禁
- [ ] 37 个旧目录已删除
- [ ] aIk-coding-style / handoff-bundle / skill-tester 未被修改
- [ ] agent.md 已更新（如需要）

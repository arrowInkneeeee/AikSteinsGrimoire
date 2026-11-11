#!/usr/bin/env node
/**
 * apply-audit-fixes.mjs - one-shot, fail-loud application of the audit's source
 * improvements to `aik-skills-lab/` (user approved "apply all" on 2026-11-10).
 *
 * What it does:
 *   1. Rewrites the catalog `description:` of 10 skills to the audit's
 *      "what + when to load + input -> output" form (DSH routes by description;
 *      `trigger:` keys are ignored by DSH).
 *   2. Fixes the 3 code-generating contradictions: table-name case,
 *      @ApiModel vs @Schema, @Transactional gate; plus the SQL directory and
 *      unified-response-class inconsistencies flagged in the same audit.
 *   3. Adds a "write to disk only after user confirmation" gate to the skills
 *      that create/modify workspace files.
 *   4. Rewrites component-extract-rewrite's Windows/PowerShell location chain
 *      into a read-only, WSL-compatible lookup (no persisted env writes) and
 *      downgrades its GitNexus precondition to optional.
 *   5. Marks skill-tester `disable-model-invocation: true` and refreshes its
 *      stale 10-skill test-target list.
 *
 * Usage:
 *   node dsh-migration/tools/apply-audit-fixes.mjs --check   # validate only
 *   node dsh-migration/tools/apply-audit-fixes.mjs --apply   # write files
 *
 * Every replacement asserts the exact old text occurs exactly once, so a
 * changed/locked file aborts instead of silently corrupting the library.
 */

import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const REPO = resolve(import.meta.dirname, '../..')
const LAB = resolve(REPO, 'aik-skills-lab')
const mode = process.argv[2] === '--apply' ? 'apply' : 'check'

const DESCRIPTION = {
  'aIk-coding-style': '编写或修改 Java 后端代码（Spring Boot 2.7 + MyBatis-Plus + Lombok）时使用：统一模块目录、命名、分层、DTO/VO/PO 注解、注释与日志、Mapper 与建表规范，并提供提交前检查清单。动手写代码或新建模块前加载。',
  'requirement-engineering': '把原始需求整理成 PRD 时使用：提取功能点、澄清模糊点、检测需求冲突、编写用户故事与 Gherkin 验收标准、评估技术可行性、按 MoSCoW 排优先级。用户说"分析需求 / 写 PRD / 排优先级"时加载。',
  'system-design': '从 PRD 产出系统设计（SDD）时使用：分层架构、数据库表与索引、RESTful API 契约、核心流程与状态机、技术选型决策树、P0-P3 设计评审，产出 docs/sdd。用户说"设计架构 / 建表 / 设计接口 / 技术选型"时加载。',
  'code-implementation': '按 SDD 实现 Java 代码时使用：生成 Controller/Service/Mapper/PO/DTO/VO 骨架、填充业务逻辑、生成 API 文档与建表迁移 SQL、补模块 README；产出须符合 aik-coding-style。用户说"实现功能 / 写代码 / 生成骨架"时加载。',
  'code-review': '审查 Java 后端代码时使用：风格（对照 aik-coding-style）、质量（N+1/空指针/事务边界/资源泄漏）、安全（SQL 注入/越权/敏感日志）、缺陷模式四维检查，按 P0-P3 输出带修复建议的报告。',
  'testing': '生成或评审 Java 测试时使用：JUnit 5 + Mockito + AssertJ 单测、@SpringBootTest 集成测试、API 测试文档、JaCoCo 覆盖率阈值、测试数据隔离策略，并按准入/准出门禁判断能否发布。',
  'devops': '准备 Java 应用部署上线时使用：多环境配置、jar/tar 打包、systemd/Docker/Compose 脚本、Actuator 健康检查、Logback 日志轮转、故障排查与回滚方案，产出部署计划与运维手册。',
  'component-extract-rewrite': '把本仓库或其他项目的组件萃取成知识手册并复写到目标项目时使用：6 维耦合分析、10 章组件手册、脱敏与标准化复写、NOTE/COMPONENT/SOLUTION/CODE 入库路由；需先确定 grimoire 基址。',
  'handoff-bundle': '需要把当前任务上下文与散落资产冻结成可核验交接包、交给另一个 Agent 或新会话继续时使用：产出 HANDOFF.md、资产清单、SHA-256 校验、Git 状态与接收方启动提示词。',
  'skill-tester': '维护技能库时使用：用 RED-GREEN-REFACTOR 验证某个技能是否真的约束住模型行为（无技能基线违规、加载后是否拦截、收紧指令回归），输出检测率/误报率与改进项。用户说"测试技能 / 验证技能有效性"时加载。',
}

const GATE =
  '> **落盘确认门禁（DSH workspace-write）**：本技能会创建或修改工作区文件。每次写入前先列出将创建/修改的文件路径与摘要，等用户确认后再写；一次流程结束给出变更清单。'

const WRITE_SKILLS = [
  'requirement-engineering',
  'system-design',
  'code-implementation',
  'code-review',
  'testing',
  'devops',
  'handoff-bundle',
  'component-extract-rewrite',
]

/** file -> [{old, new}] exact-once replacements */
const REPLACEMENTS = {
  'aIk-coding-style/SKILL.md': [
    {
      old: [
        '- [ ] **DTO（独立）**：`@Data + @ApiModel`',
        '- [ ] **DTO（继承 PO）**：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`',
        '- [ ] **VO（默认）**：`@Data + @ApiModel`，包含 `of()` 转换方法',
        '- [ ] **VO（复杂）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`',
        '- [ ] **QueryDto**：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`，继承分页基类',
      ].join('\n'),
      new: [
        '- [ ] **注解方案**：按 `references/01-api-doc-annotations.md` 选择 Swagger（`@ApiModel`）或 SpringDoc（`@Schema`）；本仓库使用 SpringDoc `@Schema`',
        '- [ ] **DTO（独立）**：`@Data` + 上述 DTO 注解',
        '- [ ] **DTO（继承 PO）**：`@Data + @EqualsAndHashCode(callSuper)` + 上述 DTO 注解',
        '- [ ] **VO（默认）**：`@Data` + 上述 VO 注解，包含 `of()` 转换方法',
        '- [ ] **VO（复杂）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor` + 上述 VO 注解',
        '- [ ] **QueryDto**：`@Data + @EqualsAndHashCode(callSuper)` + 上述 DTO 注解，继承分页基类',
      ].join('\n'),
    },
  ],
  'aIk-coding-style/references/07-entity-po.md': [
    {
      old: '- 使用 `IdType.INPUT` 或 `IdType.AUTO`',
      new: '- 统一使用 `IdType.INPUT` + `IdUtil.getSnowflakeNextId()` 显式生成 ID（本仓库约定）',
    },
  ],
  'system-design/SKILL.md': [
    {
      old: '2. 设计表结构（表名大写下划线，字段大写下划线）',
      new: '2. 设计表结构（表名/字段名小写下划线，与 `aIk-coding-style/references/16-naming.md` 一致）',
    },
    {
      old: '- 响应结构定义（VO，统一使用 Result 包装）',
      new: '- 响应结构定义（VO，统一使用项目响应封装类 `ApiResponse<T>` 包装）',
    },
    {
      old: '4. 定义响应结构（统一使用 Result<T> 包装）',
      new: '4. 定义响应结构（统一使用项目响应封装类，本仓库为 `ApiResponse<T>`）',
    },
    {
      old: '- 优先复用项目已有的通用组件（Result, BaseEntity, PageDTO）',
      new: '- 优先复用项目已有的通用组件（统一响应类如 `ApiResponse`、BaseEntity、PageDTO）',
    },
    {
      old: '- 表名大写下划线，字段大写下划线，索引 IDX_/UK_ 前缀',
      new: '- 表名/字段名小写下划线，索引 IDX_/UK_ 前缀',
    },
  ],
  'code-implementation/SKILL.md': [
    {
      old: [
        '7. **DTO/VO注解**：',
        '   - DTO（独立）：`@Data + @ApiModel`',
        '   - DTO（继承PO）：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`',
        '   - VO（默认）：`@Data + @ApiModel`，必须包含 `of()` 方法',
        '   - VO（复杂）：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`',
      ].join('\n'),
      new: [
        '7. **DTO/VO注解**（按 `aIk-coding-style/references/01-api-doc-annotations.md` 选择 Swagger `@ApiModel` 或 SpringDoc `@Schema`；本仓库为 `@Schema`）：',
        '   - DTO（独立）：`@Data` + 上述注解',
        '   - DTO（继承PO）：`@Data + @EqualsAndHashCode(callSuper)` + 上述注解',
        '   - VO（默认）：`@Data` + 上述注解，必须包含 `of()` 方法',
        '   - VO（复杂）：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor` + 上述注解',
      ].join('\n'),
    },
    {
      old: '2. 实现事务管理（@Transactional，标注传播行为）',
      new: '2. 实现事务管理：按 aIk 事务规范默认不加 `@Transactional`；多表/批量写入前先经用户确认，再标注传播行为',
    },
    {
      old: '6. 保存至 `db/migration/` 或 `sql/` 目录',
      new: '6. 保存至项目现有 SQL 目录（默认 `sql/`；已有迁移目录则跟随）',
    },
    {
      old: '| 数据库脚本 | `db/migration/` 或 `sql/` | SQL文件 |',
      new: '| 数据库脚本 | `sql/`（或项目现有 SQL 目录） | SQL文件 |',
    },
  ],
  'devops/SKILL.md': [
    {
      old: '2. 如无，添加依赖',
      new: '2. 如无，建议添加依赖并等待用户确认后再修改 pom.xml',
    },
  ],
  'handoff-bundle/SKILL.md': [
    {
      old: '- **完整模式**：可读写本地文件、复制附件、运行 Git、计算 SHA-256，并能使用任务管理工具。执行完整流程。',
      new: '- **完整模式**：可读写本地文件、复制附件、运行 Git、计算 SHA-256，并能新建会话/子代理。执行完整流程。',
    },
    {
      old: '不得声称已经复制文件、读取 Git 或验证哈希。',
      new: '不得声称已经复制文件、读取 Git 或验证哈希。\n- **DSH 提示**：没有任务通道时走文件模式——生成并校验交接包，把路径与 `NEXT_TASK_PROMPT.md` 交给用户即可。',
    },
  ],
  'code-review/assets/style-checklist.md': [
    {
      old: '> 基于 aIk-coding-style 规范，严格遵循 `-anchor` 类注释、`//note`/`//anchor` 行注释、命名规范等要求。',
      new: '> 本清单是 `aIk-coding-style` 规范的勾选副本，规范以该技能为准（规则更新改规范源，避免双份漂移）；遵循 `-anchor` 类注释、`//note`/`//anchor` 行注释、命名规范等要求。',
    },
  ],
  'component-extract-rewrite/SKILL.md': [
    {
      old: '- 前置条件：源项目已通过 `gitnexus analyze` 构建索引',
      new: '- 前置条件（可选增强）：源项目有 GitNexus 索引时分析更准；没有索引就用文件工具 + grep/read 分析',
    },
    {
      old: '2. 确认源项目已构建 gitnexus 索引',
      new: '2. 索引可选：有 GitNexus 工具/索引则用之，没有则用 grep/read 直接分析',
    },
    {
      old: '| gitnexus索引 | 源项目已执行 `gitnexus analyze` | 是（analyze） | 提示用户先构建索引 |',
      new: '| gitnexus索引 | 可选增强（源项目已执行 `gitnexus analyze`） | 否 | 无索引时用 grep/read 分析 |',
    },
    {
      old: '| 索引检查 | gitnexus 索引已构建 | 继续，否则提示构建 |',
      new: '| 索引检查 | 可选；有索引时用于校验 | 无索引不阻断，按 grep/read 分析结果继续 |',
    },
    {
      old: '- 源项目路径必须为纯英文（避免编码问题）',
      new: '- 源项目路径必须为纯英文（避免编码问题；WSL 下即 `/mnt` 挂载路径）',
    },
  ],
  'skill-tester/SKILL.md': [
    {
      old: '建议优先测试以下 10 个关键技能：',
      new: '建议优先测试以下 11 个关键技能：',
    },
    {
      old: '1. **aIk-coding-style** — 纪律型测试（编码规范基础）',
      new: '1. **aik-coding-style** — 纪律型测试（编码规范基础）',
    },
    {
      old: '10. **skill-tester** — 元测试（测试框架自身）',
      new: '10. **skill-tester** — 元测试（测试框架自身）\n11. **ccgui-plugin-creator** — 资料型测试（CC GUI 插件 SDK；不属 DSH 迁移范围）',
    },
  ],
  'README.md': [
    {
      old: '- **数据库规范**：大写下划线命名（T_ORDER、CREATE_TIME）',
      new: '- **数据库规范**：小写下划线命名（如 `aik_knowledge`、`CREATE_TIME`）',
    },
    {
      old: '包含 **10 个技能**，采用子命令模式',
      new: '包含 **10 个核心技能**（另有 `ccgui-plugin-creator` 由 CC GUI 托管），采用子命令模式',
    },
  ],
  'agent.md': [
    {
      old: [
        '- DTO 独立：`@Data @ApiModel`',
        '- DTO 继承 PO：`@Data @EqualsAndHashCode(callSuper) @ApiModel`',
        '- VO 默认：`@Data @ApiModel`，**必须含 `of()` 静态转换方法**',
        '- VO 复杂：`@Data @Builder @NoArgsConstructor @AllArgsConstructor @ApiModel`',
        '- QueryDto：`@Data @EqualsAndHashCode(callSuper) @ApiModel`，继承分页基类',
      ].join('\r\n'),
      new: [
        '- 注解方案：按 `references/01-api-doc-annotations.md` 选择 Swagger（`@ApiModel`）或 SpringDoc（`@Schema`）；本仓库为 `@Schema`',
        '- DTO 独立：`@Data` + 上述注解',
        '- DTO 继承 PO：`@Data @EqualsAndHashCode(callSuper)` + 上述注解',
        '- VO 默认：`@Data` + 上述注解，**必须含 `of()` 静态转换方法**',
        '- VO 复杂：`@Data @Builder @NoArgsConstructor @AllArgsConstructor` + 上述注解',
        '- QueryDto：`@Data @EqualsAndHashCode(callSuper)` + 上述注解，继承分页基类',
      ].join('\r\n'),
    },
  ],
}

const COMPONENT_CHAIN = 'component-extract-rewrite/references/grimoire-location-chain.md'

const problems = []
const texts = new Map()
const dirty = new Set()

function get(rel) {
  if (!texts.has(rel)) texts.set(rel, readFileSync(resolve(LAB, rel), 'utf8'))
  return texts.get(rel)
}

function put(rel, next) {
  texts.set(rel, next)
  dirty.add(rel)
}

function replaceOnce(rel, old, next) {
  const text = get(rel)
  const first = text.indexOf(old)
  if (first < 0) {
    problems.push(`${rel}: missing expected text: ${JSON.stringify(old.slice(0, 60))}`)
    return
  }
  if (text.indexOf(old, first + old.length) >= 0) {
    problems.push(`${rel}: expected text occurs more than once: ${JSON.stringify(old.slice(0, 60))}`)
    return
  }
  put(rel, text.replace(old, next))
}

function replaceSection(rel, startMarker, endMarker, nextSection) {
  const text = get(rel)
  const start = text.indexOf(startMarker)
  const end = text.indexOf(endMarker)
  if (start < 0 || end < 0 || end <= start) {
    problems.push(`${rel}: cannot locate section ${startMarker} .. ${endMarker}`)
    return
  }
  put(rel, text.slice(0, start) + nextSection + text.slice(end))
}

function replaceDescription(rel, next) {
  const text = get(rel)
  const match = /^description: .*$/m.exec(text)
  if (!match) {
    problems.push(`${rel}: no description line`)
    return
  }
  const line = match[0]
  const cr = line.endsWith('\r') ? '\r' : ''
  put(rel, text.replace(line, `description: '${next}'${cr}`))
}

// 1. descriptions
for (const [skill, description] of Object.entries(DESCRIPTION)) {
  replaceDescription(`${skill}/SKILL.md`, description)
}

// 2. content replacements
for (const [rel, hunks] of Object.entries(REPLACEMENTS)) {
  for (const { old, new: next } of hunks) replaceOnce(rel, old, next)
}

// 3. write gate after the H1 of each writing skill
for (const skill of WRITE_SKILLS) {
  const rel = `${skill}/SKILL.md`
  const text = get(rel)
  if (text.includes('落盘确认门禁（DSH')) continue
  const match = /^# .*$/m.exec(text)
  if (!match) {
    problems.push(`${rel}: no H1 to anchor the write gate`)
    continue
  }
  const at = match.index + match[0].length
  put(rel, `${text.slice(0, at)}\n\n${GATE}${text.slice(at)}`)
}

// 4. component location chain: read-only, WSL-compatible
replaceSection(
  COMPONENT_CHAIN,
  '## 定位链（按优先级，命中即止）',
  '## 4 种内容类型',
  [
    '## 定位链（按优先级，命中即止）',
    '',
    '```text',
    'Step 1 — 当前工作区直查（本仓库优先，零副作用）',
    '  · 若 {workspace} 自身含 grimoire-files/component-manuals，基址 = {workspace}（本仓库命中）',
    '  · 否则若 {workspace} 的父级目录中存在 AikSteinsGrimoire 且含 grimoire-files/component-manuals，基址 = 命中路径',
    '  · 命中 → 结束',
    '',
    'Step 2 — 只读环境变量 AIK_GRIMOIRE_HOME',
    '  · 若非空且 $AIK_GRIMOIRE_HOME/grimoire-files/component-manuals 存在 → 基址 = 该路径，结束',
    '  · 只读探测；不自动写入、不持久化任何环境变量（DSH/WSL 下环境只读、避免破坏性副作用）',
    '',
    'Step 3 — 询问用户',
    '  · 不擅自猜测路径；告知"定位失败，请提供真实目录，或在 shell 中自行 export AIK_GRIMOIRE_HOME=..."',
    '  · 本仓库/DSH 下通常 Step 1 即命中，无需全局搜索',
    '```',
    '',
  ].join('\n'),
)

replaceSection(
  COMPONENT_CHAIN,
  '## 自动写缓存的门禁',
  '## 类型判定规则',
  [
    '## 环境变量说明（只读）',
    '',
    '- 定位链**只读取** `AIK_GRIMOIRE_HOME`，不自动写入、不持久化任何环境变量。',
    '- 需要固定基址时，由用户在自己的 shell 中 `export AIK_GRIMOIRE_HOME=/path/to/AikSteinsGrimoire`（或在 Windows 用户环境变量中自行设置）。',
    '- 每次阶段报告中注明本次使用的基址来源（当前工作区 / 环境变量 / 用户指定）。',
    '',
  ].join('\n'),
)

// 5. skill-tester: keep the maintenance skill out of the model catalog
{
  const rel = 'skill-tester/SKILL.md'
  const text = get(rel)
  const match = /^description: .*$/m.exec(text)
  if (match && !text.includes('disable-model-invocation')) {
    put(rel, text.replace(match[0], `${match[0]}\ndisable-model-invocation: true`))
  }
}

// 6. aIk: document the self-check scripts that were previously unreferenced
{
  const rel = 'aIk-coding-style/SKILL.md'
  const text = get(rel)
  if (!text.includes('scripts/bom-checker.sh')) {
    put(rel, `${text.replace(/\s*$/, '')}\n\n> 可选自检：\`bash scripts/bom-checker.sh\`、\`bash scripts/naming-validator.sh\`（检查 BOM 与命名规范；DSH 下用 bash 工具执行）。\n`)
  }
}

if (problems.length > 0) {
  console.error('REFUSING: the library does not match the audited baseline:')
  for (const problem of problems) console.error(`  - ${problem}`)
  process.exit(1)
}

console.log(`${mode}: ${dirty.size} file(s) to update`)
for (const rel of [...dirty].sort()) console.log(`  - ${rel}`)

if (mode === 'apply') {
  for (const rel of dirty) writeFileSync(resolve(LAB, rel), get(rel))
  console.log(`\napplied ${dirty.size} file(s) under aik-skills-lab/`)
}

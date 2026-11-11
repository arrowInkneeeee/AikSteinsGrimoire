# aik-skills-lab → DSH 技能迁移审计

> 审计对象：`aik-skills-lab/` 下 11 个技能 bundle（README 说 10 个，实际 11 个，`ccgui-plugin-creator` 为第 11 个）
> 审计人：aik-auditor（Agent Team）
> 日期：2026-11-10
> 依据：逐个通读 11 个 `SKILL.md`（共 2384 行 / 88 KB），抽查全部 references/assets/scripts，对照 DSH 加载器源码与本仓库真实代码
> 只读声明：本审计未修改任何技能文件；唯一产出为本报告

---

## 0. 结论速览

**一句话结论**：11 个 bundle 里，10 个可以被 DSH 发现，`aIk-coding-style` 因名称含大写字母会被 DSH **静默丢弃**（这是唯一硬性阻断项，修 1 行即可）。建议迁移 10 个：3 个零改动、7 个需适配；`ccgui-plugin-creator` 不迁。日常高价值子集为 7 个技能，catalog 常驻开销约 824 字符。

| # | 技能 | name 合法 | desc 字符 | 额外键 | 硬依赖 / 主要风险 | 结论 | 优先级 |
|---|------|:---:|---:|------|------|------|:---:|
| 1 | `aIk-coding-style` | ❌（原始库） | 124 | type, version | 名称违反 DSH kebab-case → **不可被发现**；41 处相对链接指向它 | **ADAPTER**（改 1 行 name，审计时已修） | P0 |
| 2 | `requirement-engineering` | ✅ | 144 | type, version, trigger×18 | 无 | **AS-IS** | P1 |
| 3 | `system-design` | ✅ | 123 | type, version, trigger×16 | 依赖 bash 的 find/grep；表名规范与真实项目矛盾 | **ADAPTER**（小） | P1 |
| 4 | `code-implementation` | ✅ | 127 | type, version, trigger×13 | 21 处 `../aIk-coding-style` 链接；`@Transactional`/`@ApiModel` 与规范冲突 | **ADAPTER** | P1 |
| 5 | `code-review` | ✅ | 136 | type, version, trigger×11 | 9 处 `../aIk-coding-style` 链接 | **ADAPTER**（小） | P1 |
| 6 | `testing` | ✅ | 113 | type, version, trigger×15 | 2 处 `../aIk-coding-style` 链接 | **ADAPTER**（小） | P2 |
| 7 | `devops` | ✅ | 128 | type, version, trigger×17 | `mvn`/`docker` 可降级；会擅自往 pom 加依赖 | **AS-IS**（2 处建议小修） | P2 |
| 8 | `component-extract-rewrite` | ✅ | 103 | type, version, trigger×13 | PowerShell 定位链 + `AIK_GRIMOIRE_HOME` + Everything + gitnexus 前置 | **ADAPTER**（重） | P2 |
| 9 | `handoff-bundle` | ✅ | 178 | type, version | 无；DSH 无"新建任务"能力但有降级模式 | **AS-IS**（可选微调） | P1 |
| 10 | `skill-tester` | ✅ | 121 | type, version | 需要"无技能对照"，DSH 单会话内不好切换；测试资产已过期 | **ADAPTER** | P3（维护态） |
| 11 | `ccgui-plugin-creator` | ✅ | 194 | 无（格式已最干净） | 属 CC GUI 产品线，由 CC GUI 工具托管、文档由源码生成 | **SKIP** | — |

> 优先级含义：P0 = 不修则基础规范链完全不可用；P1 = 核心 SDD 流程；P2 = 按需；P3 = 仅技能库维护时需要。

> **审计时点说明**：判定基准是 `aik-skills-lab` 工作树（git HEAD + 审计时的未提交改动）。审计进行中已观测到**并行迁移**动作，报告结论仍以"原始库"为基准，已发生的变化在对应小节标注：
> ① `aIk-coding-style/SKILL.md:2` 已改为 `name: aik-coding-style`（目录名未改，41 处相对链接因此保持有效）——正是 §2 推荐的最小修复；
> ② `.agents/skills/` 下已有 8 个指向 `aik-skills-lab/*` 的符号链接（7 个核心技能 + `devops`）；
> ③ 各技能 description **尚未替换**（DSH catalog 实测仍是旧文案），附录 A 可直接用。

---

## 1. DSH 加载器事实（本次实测基线）

以下行号相对于 DSH 安装目录 `~/.nvm/versions/node/v24.19.0/lib/node_modules/@deepseek-ai/dsh/`（`node_modules/@deepseek-ai/...`）。

### 1.1 发现与解析

- **只扫描两层**：技能根 `<root>/<name>/SKILL.md` 或 `<root>/<name>.md`；`<root>/<name>/references/*` 不会被当成技能（`@deepseek-ai/dsh-skill-filesystem/lib/index.js:562-571`）。
- **技能根**：`<project>/.dsh/skills`、`<project>/.agents/skills`、自定义目录、`~/.dsh/skills`、`~/.agents/skills`（`dsh-skill-filesystem/lib/index.js:160-191`）；project root 由 `findProjectRoot` 向上找，本仓库根即 `/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire`。
- **必填字段**：`name` + `description`，缺一个即"ignored"（`dsh-skill-filesystem/lib/index.js:694-697`）。
- **name 语法**：`/^[a-z0-9]+(?:-[a-z0-9]+)*$/`（`@deepseek-ai/dsh-skill/lib/index.js:23`），严格小写 kebab-case；不合法时**只打一条 warn 日志后丢弃**（`dsh-skill-filesystem/lib/index.js:699-701`）。
- **未知键被忽略**：`parseSkillFile` 只读取 `name`、`description`、`whenToUse`、`metadata`、`disable-model-invocation`、`user-invocable`（`dsh-skill-filesystem/lib/index.js:694-713`、`866-877`）。`type`/`version`/`trigger` 等**可以保留（不报错）但完全不参与任何逻辑**。
- **目录名与技能名无关**：注册的是 frontmatter 的 `name`，`directory` 只作为资源基址（`dsh-skill-filesystem/lib/index.js:596-614`）。→ **改 name 不必改目录名**，这是本次迁移成本最低的路径。
- **`whenToUse`**：会被解析、传递（`dsh-skill/lib/index.js:441-497`），但**不进入模型可见的 catalog，也不注入 `<skill_content>`**，对路由没有作用；路由只看 name+description。

### 1.2 路由、目录与成本

- **catalog 只列 `modelInvocable` 的技能**：`- \`name\`: description` 一行一个（`dsh-tool-skill/lib/index.js:944`、`1028-1030`）。
- **description 上限 500 字符**，先做空白归一化（换行折叠成空格），超长截断为 `...`（`dsh-tool-skill/lib/index.js:768`、`1094-1096`）。本次 11 个技能最大 194 字符，**都不会被截断**。
- **`disable-model-invocation: true`** = 只在用户的 UI 菜单可用、**不进模型 catalog**（零常驻 token），`skill` 工具会拒绝模型调用它（`dsh-tool-skill/lib/index.js:875-878`；UI 侧显示 `userOnly`）。
- **资源不预加载**：调用技能时只注入 `<skill_instructions>` 正文，附一行 `Base directory for this skill: <安装目录>`，提示模型用文件工具按需打开 `references/`（`dsh-skill/lib/index.js:58-81`）。

### 1.3 由此得到的 4 条硬结论

1. **11 个里只有 `aIk-coding-style` 在原始库装不上**（名称含大写 `I`/`K`）；其余 10 个可被发现，desc 均合规。（审计时点该行已并行改为 `aik-coding-style`，DSH catalog 已可见。）
2. 7 个带 `trigger:` 清单的技能（共 **103 个触发词**：requirement 18、devops 17、system-design 16、testing 15、code-implementation 13、component-extract-rewrite 13、code-review 11）在 DSH 里**全部无效**——路由完全依赖 description，迁移时 description 必须重写成"做什么 + 何时加载"。
3. `references/` 里的相对路径会以技能安装目录为基准解析；`references/x.md` 这类**同技能内相对路径天然可用**，但 `../其他技能/...` 只有在同一 root 下同时安装时才成立（§13.4）。
4. L2 正文（每个 6–11 KB）只在技能被调用时进上下文，所以"11 个技能都装"的主要成本是 catalog 常驻的 name+description（当前合计 **1491 字符**），不是正文体积。

---

## 2. `aIk-coding-style` — Java 后端编码规范（P0）

**是什么 / 给谁**：`aIk` 个人/团队的 Spring Boot + MyBatis-Plus + Lombok 编码规范总库：目录结构、命名、注释（`-anchor`/`//note`）、PO/DTO/VO 注解组合、Controller/Service/Mapper 分层、建表 SQL、事务与日志规则。给"在本仓库或同类 Java 8 项目里写/改代码的开发者"用。

**价值**：★★★★★。它是整个技能库的地基——全库共 **103 处**提到它（code-implementation 27、aIk 自身 18、code-review 15、system-design 9、skill-tester 7、component-extract-rewrite 6、testing 5、requirement-engineering 1、README/agent.md/QUICK-START 等文档 15），其中 41 处是直接的相对路径链接；资源量也最大（18 个 reference 共 1825 行 + 2 个 shell 自检脚本）。对 Java/Spring SDD 流程是**最高价值、最高频**的部分。

**DSH 兼容性**：
- ❌ **name 非法（原始库）**：git HEAD 中 `aik-skills-lab/aIk-coding-style/SKILL.md:2` 为 `name: aIk-coding-style`，违反 `/^[a-z0-9]+(?:-[a-z0-9]+)*$/`，DSH 会丢弃整个技能且**用户界面无任何提示**（只有日志 warn）。这是全库唯一的硬阻断。**审计时点已观测到并行修复**：工作树该行已改为 `name: aik-coding-style`（目录名未动），DSH catalog 中已出现 `aik-coding-style`，与本节推荐的最小修复一致。
- ✅ description 124 字符，单行，不会被截断。
- ⚠️ `type: Reference`、`version: 1.0.0`（第 4-5 行）被忽略，建议并入 `metadata:`。
- ✅ 无平台依赖：实测 `grep -E "C:\\|\.lingma|\.qoder|gitnexus|powershell"` 在 `aIk-coding-style/` 下 0 命中；正文不写文件、不跑命令，是 11 个里**最安全**的纯规则技能。
- ⚠️ `scripts/bom-checker.sh`、`scripts/naming-validator.sh` 在 `SKILL.md` 里**零引用**（只在 `README.md:48/166`、`QUICK-START.md:256` 提到），迁移后模型永远不知道它们存在。

**内容质量**：
- **内部矛盾（应修）**：`SKILL.md:174-178` 的检查清单把 DTO/VO 注解硬编码为 `@ApiModel`，而自己的 `references/01-api-doc-annotations.md:3-8` 明确写了"老项目 Swagger / 新项目 SpringDoc 二选一"；本仓库实际是 SpringDoc——`src/` 下 **47 个 Java 文件使用 `@Schema`**（如 `src/main/java/io/aik/steins/grimoire/knowledge/common/po/KnowledgePo.java:32`），`@ApiModel` 在该项目里根本不生效。按现清单生成代码会产出无效注解。
- **小矛盾**：`references/07-entity-po.md:303` 写"使用 `IdType.INPUT` 或 `IdType.AUTO`"，但主流程（`agent.md:104`）与本仓库实际（`KnowledgePo.java:46`）都是 `IdType.INPUT` + `IdUtil.getSnowflakeNextId()`；建议统一为强制 INPUT。
- **正面**：事务规范（`SKILL.md:196-200`）是全库唯一带"必须经用户确认"门禁的规则，质量很高；"不生成空 XML""优先 MyBatis-Plus API"等约定与仓库实践一致（`aik_knowledge` 系列表名小写下划线也与 `references/16-naming.md:13` 一致）。
- **个人化约定**（`@author a I k .`、`-anchor` 标记、Service Bean 名 `{module}.{Name}`）不可泛化，但这就是本技能的价值，不建议通用化，迁移时明确"仅适用于 aIk 的项目"即可。
- **无危险指令**（不写文件、不改配置）。

**结论：MIGRATE WITH ADAPTER。**

必改（1 行，**审计时点已执行**）：
1. `SKILL.md:2` 改为 `name: aik-coding-style`（目录名可不动——DSH 只认 frontmatter name，见 §1.1）。

建议改：
2. 第 4-5 行 `type`/`version` 收进 `metadata: { type: Reference, version: 1.0.0 }`；description 换成下方建议文案。
3. `SKILL.md:174-178` 改为"按 `references/01-api-doc-annotations.md` 选择 Swagger(`@ApiModel`) 或 SpringDoc(`@Schema`)"。
4. `references/07-entity-po.md:303` 改为"统一 `IdType.INPUT` + `IdUtil.getSnowflakeNextId()`"。
5. `SKILL.md` 末尾加一行：生成/审查后可选跑 `scripts/bom-checker.sh`、`scripts/naming-validator.sh` 自检。

**建议 catalog description（130 字符，可直接替换第 3 行）**：

```text
编写或修改 Java 后端代码（Spring Boot 2.7 + MyBatis-Plus + Lombok）时使用：统一模块目录、命名、分层、DTO/VO/PO 注解、注释与日志、Mapper 与建表规范，并提供提交前检查清单。动手写代码或新建模块前加载。
```

**改名影响（若把目录/引用一并改名）**：`SKILL.md:2` 自身；18 个 reference 的"来源：aIk-coding-style"头（`references/01…18:3`）；41 处 `../aIk-coding-style`（分布在 18 个文件：code-implementation 21、code-review 9、skill-tester 5、system-design 4、testing 2）；全库文本提及 103 处（含 `README.md`、`agent.md`、`QUICK-START.md` 等文档 15 处）。
**关键提示**：如果只改 frontmatter name、保留目录名 `aIk-coding-style`，则以上 41 处链接**全部继续有效**，零额外改动。建议先走这条最小路径（**审计时点已按此执行**）；把目录也改成 `aik-coding-style` 是可选的美化项，需同步替换 41 处链接。

---

## 3. `requirement-engineering` — 需求工程 → PRD（P1）

**是什么 / 给谁**：把会议记录/聊天/口述等非结构化需求整理成结构化 PRD。给"拿到需求文本、要出 PRD 再进入设计"的开发者；无需产品经理参与。

**价值**：★★★★☆。覆盖 extract / clarify / conflict-check / user-story / feasibility / prioritize / full 7 个子命令，有明确的 3 个人机协作断点（澄清、high 冲突、阶段确认），4 个 L3 参考（冲突清单、Java 可行性 8 维、MoSCoW 矩阵、PRD 模板 228 行），质量在 11 个里属于上乘。只是频率低于编码类技能。

**DSH 兼容性**：
- ✅ name 合法；description 144 字符，单行。
- ⚠️ `type`/`version`/`trigger`（第 4-24 行）全部被忽略；`trigger` 里 19 个触发词在 DSH 中无效，路由只能靠 description。
- ✅ 无平台依赖、无外部工具；输出是 `docs/prd/prd-{项目名}-v{版本}.md`，属于工作区内文件。
- ⚠️ 正文（`SKILL.md:203/215`）直接说"保存至 `docs/prd/`"，没有"落盘前确认"的门禁；DSH workspace-write 模式下模型可直接写文件（见 §13.5）。
- ✅ 无 `../aIk-coding-style` 链接（只有 1 处纯文本提及 `SKILL.md:257`），改 aIk 名称不影响它。

**内容质量**：
- 无明显错误或过期假设；`references/conflict-checklist.md`（86 行）与 `moscow-decision-matrix.md`（72 行）内容自洽，权重 40/30/20/10 与 SKILL 正文一致。
- 唯一注意点：`full` 子命令与 `system-design` 的"用户直接描述需求也可启动"（`system-design/SKILL.md:243`）存在入口重叠，靠 description 区分即可（见建议文案）。
- 与 `system-design` 的边界清晰（PRD → SDD），无实质重复。

**结论：MIGRATE AS-IS。** 不改也能被 DSH 正常发现和加载。可选：把 `type`/`version`/`trigger` 并入 `metadata`（纯整洁），并替换 description。

**建议 catalog description（107 字符）**：

```text
把原始需求整理成 PRD 时使用：提取功能点、澄清模糊点、检测需求冲突、编写用户故事与 Gherkin 验收标准、评估技术可行性、按 MoSCoW 排优先级。用户说"分析需求 / 写 PRD / 排优先级"时加载。
```

---

## 4. `system-design` — 系统设计 → SDD（P1）

**是什么 / 给谁**：从 PRD 产出结构化 SDD：分层架构、库表/索引、RESTful API 契约、流程/状态机、技术选型决策树、P0-P3 设计评审。给"PRD 已定、要出设计文档"的开发者。

**价值**：★★★★☆。7 个子命令 + 5 个参考（评审清单 132 行、选型树 149 行、事务模式 156 行），把"先查项目已有组件再设计"写成了前置检查，质量好。

**DSH 兼容性**：
- ✅ name 合法；description 123 字符，单行。
- ⚠️ `type`/`version`/`trigger` 被忽略（16 个触发词无效）。
- ⚠️ 前置检查依赖 bash：`find src -type d -name "common" -o -name "util" ...`、`grep -r "class Result"`、`cat pom.xml | grep ...`（`SKILL.md:33-44`）。DSH 有 bash 工具，本机 WSL 可用；但在没有 bash 的宿主上会失败（可降级为文件工具）。附带瑕疵：`find ... -o` 缺括号，`-type d` 只约束第一个分支，输出会混入名为 `util`/`config` 的文件——低危。
- ⚠️ 自身有 4 处 `../aIk-coding-style` 链接（`references/api-design-guide.md:6/8`、`db-design-guide.md:6` 等），依赖 aIk 目录名存在（见 §13.4）。
- 无 Windows/IDE 专属路径。

**内容质量**：
- **硬矛盾（应修）**：`SKILL.md:93` 与 `SKILL.md:276` 要求"表名**大写**下划线"（与 `README.md:154` 的 `T_ORDER` 一致），但 `references/16-naming.md:13` 与本仓库真实 SQL 都是**小写**下划线——`sql/aik_knowledge_tables.sql` 建的是 `aik_knowledge_category`、`aik_knowledge`，`@TableName("aik_knowledge")`（`KnowledgePo.java:33`）。按大写规范生成 DDL 必然与项目现状不一致。
- 小矛盾：`SKILL.md:115/123` 规定响应"统一使用 `Result<T>` 包装"，而 aIk 规范（`references/01`）与本仓库是 `ApiResponse`（`src/main/java/io/aik/steins/grimoire/core/dto/ApiResponse.java`）；建议改成"沿用项目现有响应封装类（本仓库为 `ApiResponse`）"。
- `references/api-design-guide.md`（12 行）、`db-design-guide.md`（6 行）是合并遗留的**薄壳文件**（只有链接列表）、标题还留着旧原子技能名 "API Designer/Database Designer"（见 §13.2）。

**结论：MIGRATE WITH ADAPTER（小）。** 改动：
1. `SKILL.md:93`、`SKILL.md:276`：表名/字段名改为"小写下划线（与 `aIk-coding-style/references/16-naming.md` 一致）"。
2. `SKILL.md:115/123`：`Result<T>` → "项目现有统一响应类（默认 `ApiResponse<T>`，先查项目）"。
3. （可选）合并 `api-design-guide.md`/`db-design-guide.md` 的链接进 SKILL 索引，删薄壳。
4. 无需改 frontmatter 即可被发现；建议替换 description、把额外键并入 `metadata`。

**建议 catalog description（121 字符）**：

```text
从 PRD 产出系统设计（SDD）时使用：分层架构、数据库表与索引、RESTful API 契约、核心流程与状态机、技术选型决策树、P0-P3 设计评审，产出 docs/sdd。用户说"设计架构 / 建表 / 设计接口 / 技术选型"时加载。
```

---

## 5. `code-implementation` — 代码实现（P1）

**是什么 / 给谁**：按 SDD 生成可运行 Java 代码：Controller/Service/Mapper/PO/DTO/VO/常量骨架 → 业务逻辑 → API 文档 → 建表迁移 SQL → 模块 README。给"设计已定、要落代码"的开发者。

**价值**：★★★★★。SDD 工作流的核心执行环节，4 个子命令 + 4 个 guide + 2 个 assets 模板，与 `aIk-coding-style` 的约定高度一致（目录、XML 位置、PO 注解、Service Bean 名）。

**DSH 兼容性**：
- ✅ name 合法；description 127 字符，单行。
- ⚠️ `type`/`version`/`trigger` 被忽略（13 个触发词无效）。
- ⚠️ **21 处 `../aIk-coding-style` 相对链接**（`SKILL.md:26` + `references/skeleton-guide.md`、`implementation-guide.md`、`api-doc-guide.md`、`migration-guide.md` 共 20 处），全部依赖 aIk 目录名与同 root 共存。
- 无平台依赖；会写 `src/main/java/...`、`docs/api/`、`db/migration/`（workspace-write 下可直接落盘，见 §13.5）。

**内容质量**：
- **硬矛盾（应修）**：`SKILL.md:94` 要求"实现事务管理（`@Transactional`，标注传播行为）"，但 `aIk-coding-style/SKILL.md:196-200` 明确规定"**使用 `@Transactional` 前必须经用户确认**、单表单条不加"。两个技能同时生效时会打架，必须改为"按 aIk 事务规范：默认不加，需要时先确认"。
- **硬编码注解与项目现实不符**：`SKILL.md:38-42` 把 DTO/VO 注解固定为 `@ApiModel`，而本仓库实际用 SpringDoc `@Schema`（47 个文件），且 aIk 自己的 `references/01` 允许二选一。
- SQL 落点不一致：`SKILL.md:145` 写 `db/migration/`，`SKILL.md:216` 写 `db/migration/ 或 sql/`，aIk 规范是 `sql/`（`references/08-database.md` 末尾）。建议统一为"优先跟随项目现有 SQL 目录（本仓库 `sql/`）"。
- 无过期资产；template（`assets/sql-template.md` 194 行、`api-doc-template.md` 180 行）质量可用。

**结论：MIGRATE WITH ADAPTER。** 改动：
1. `SKILL.md:94` 改为"事务按 aIk 事务规范：默认不加 `@Transactional`；需多表写入时先向用户确认"。
2. `SKILL.md:38-42` 改为"注解方案按 `aIk-coding-style/references/01-api-doc-annotations.md`：Swagger 老项目 `@ApiModel` / SpringDoc 新项目 `@Schema`（本仓库为 `@Schema`）"。
3. 统一 SQL 目录表述（第 145/216 行）为"项目现有 SQL 目录（默认 `sql/`）"。
4. 改名 aIk 目录时同步替换 21 处链接（见 §13.4）；不推荐改名。
5. （可选）frontmatter 清理 + description 替换。

**建议 catalog description（148 字符）**：

```text
按 SDD 实现 Java 代码时使用：生成 Controller/Service/Mapper/PO/DTO/VO 骨架、填充业务逻辑、生成 API 文档与建表迁移 SQL、补模块 README；产出须符合 aik-coding-style。用户说"实现功能 / 写代码 / 生成骨架"时加载。
```

---

## 6. `code-review` — 代码审查四维（P1）

**是什么 / 给谁**：对 Java 代码做风格 / 质量 / 安全 / 缺陷模式四维审查，按 P0-P3 输出带修复建议的报告，并带质量门禁（质量 P0/P1=0、安全无高危）。给"提交前自审、CR 前置检查"的开发者。

**价值**：★★★★☆。与 `aIk-coding-style` 天然配套（写规则 ↔ 查规则），`references/bug-patterns.md`（187 行，8 种缺陷模式）与安全清单（`assets/security-report-template.md` 269 行）质量高。

**DSH 兼容性**：
- ✅ name 合法；description 136 字符，单行。
- ⚠️ `type`/`version`/`trigger` 被忽略（11 个触发词无效）。
- ⚠️ **9 处 `../aIk-coding-style` 链接**：`references/style-guide.md:6-13`、`quality-guide.md:6`、`assets/style-checklist.md:159`、`quality-report-template.md:227`（以及各自文件头的 `来源` 标记）。
- 无平台依赖；只读代码、输出报告，安全性好。

**内容质量**：
- `assets/style-checklist.md`（159 行）是对 aIk 规则的完整复制——**维护双份**风险：aIk 规则改了这里不会自动同步（当前已出现 `@ApiModel` 与 SpringDoc 的同类问题）。建议迁移时在文件头注明"以 aIk-coding-style 为准，本清单只做勾选"。
- `references/style-guide.md`（14 行）、`quality-guide.md`（9 行）是合并遗留的薄壳（只有链接 + 旧技能名 "Code Style Reviewer"）。
- 与 `component-extract-rewrite` 的 rewrite 子命令有 3 层审查重叠（那边 `references/rewriter-delivery-spec.md:27-45` 又描述了一遍风格/质量/安全三层），属可接受的调用关系，但建议 rewrite 直接引用 code-review，别复制规则。

**结论：MIGRATE WITH ADAPTER（小）。** 改动：
1. 若改 aIk 目录名，替换 9 处链接；不改名则零改动。
2. （可选）`style-checklist.md` 头部加"以 aIk 规范为准"声明；删除两个薄壳 reference 的旧技能名。
3. description 替换（现有描述已可用，但缺少"何时加载"）。

**建议 catalog description（109 字符）**：

```text
审查 Java 后端代码时使用：风格（对照 aik-coding-style）、质量（N+1/空指针/事务边界/资源泄漏）、安全（SQL 注入/越权/敏感日志）、缺陷模式四维检查，按 P0-P3 输出带修复建议的报告。
```

---

## 7. `testing` — 测试全流程（P2）

**是什么 / 给谁**：JUnit 5 + Mockito + AssertJ 单测、@SpringBootTest 集成测试、API 测试文档、JaCoCo 覆盖率、测试数据隔离策略，带准入/准出门禁。给"实现完要补测试、要过 CI 门禁"的开发者。

**价值**：★★★★☆。与仓库技术栈完全一致（JUnit 5 + Mockito + JaCoCo + Spring Boot 2.7），质量门禁（行覆盖 >60%/70%）写得很具体，是 SDD 闭环的必要一环。

**DSH 兼容性**：
- ✅ name 合法；description 113 字符，单行。
- ⚠️ `type`/`version`/`trigger` 被忽略（15 个触发词无效）。
- ⚠️ 2 处 `../aIk-coding-style` 链接：`references/unit-test-guide.md:6`、`assets/test-class-template.md:312`。
- 无平台依赖；会写 `src/test/java/`、`docs/test/`。

**内容质量**：
- `references/unit-test-guide.md`（6 行）是合并遗留薄壳（只有一个链接）。
- 集成测试策略（Testcontainers → 否则 H2）与 `@Transactional` 回滚测试数据是测试域的正确用法，**不**与 aIk 的生产事务规则冲突（一个是测试回滚，一个是生产事务），迁移时可在正文点明避免模型混淆。
- 仓库当前 `src/test/java` 存在但测试案例很少，技能迁移后价值需要真实测试任务来兑现。

**结论：MIGRATE WITH ADAPTER（小）。** 改动：链接（若改名 aIk 目录）；删除薄壳/补内容；替换 description。不涉及内容矛盾。

**建议 catalog description（116 字符）**：

```text
生成或评审 Java 测试时使用：JUnit 5 + Mockito + AssertJ 单测、@SpringBootTest 集成测试、API 测试文档、JaCoCo 覆盖率阈值、测试数据隔离策略，并按准入/准出门禁判断能否发布。
```

---

## 8. `devops` — 部署运维（P2）

**是什么 / 给谁**：多环境配置、jar/tar 打包、systemd/Docker/Compose 脚本、Actuator 健康检查、Logback 日志、故障排查与回滚手册。给"要把服务发到服务器/容器"的开发者。

**价值**：★★★☆☆。内容完整（部署决策矩阵 68 行、部署计划/运维手册模板 285/425 行），但**本仓库（个人知识库应用）目前没有明显的上线部署需求**；对交付型 Java 项目价值会升到 ★★★★。

**DSH 兼容性**：
- ✅ name 合法；description 128 字符，单行。
- ⚠️ `type`/`version`/`trigger` 被忽略（17 个触发词无效）。
- ✅ **零 `../aIk-coding-style` 链接、零 Windows/IDE 路径**，是 6 个"编码/测试类"之外最独立的一个。
- 依赖外部命令 `mvn`、`docker`、`systemctl`（可降级为生成脚本而不执行）。

**内容质量**：
- **需注意的安全点**：`SKILL.md:131-137` 的 health 子命令写"如无，添加依赖"——会直接改 `pom.xml`，没有确认环节；建议改为"建议并等待用户确认后再改 pom"（见 §13.5）。
- 陈旧页脚：`assets/deploy-plan-template.md:285`、`assets/ops-manual-template.md:425` 落款是已删除的旧技能 `spec-devops`；`SKILL.md:23` 的 trigger 里也还有它（反正 trigger 无效）。纯美观问题。
- 与 `testing` 的质量门禁有轻微重叠（都提"测试通过后部署"），无冲突。

**结论：MIGRATE AS-IS。** 不被发现的风险为零、无平台依赖、无链接依赖，可以原样安装；仅建议两处小修（pom 改动加确认、旧页脚改名）。若追求"迁移即正确"，把它算作 ADAPTER（2 行改动）。

**建议 catalog description（110 字符）**：

```text
准备 Java 应用部署上线时使用：多环境配置、jar/tar 打包、systemd/Docker/Compose 脚本、Actuator 健康检查、Logback 日志轮转、故障排查与回滚方案，产出部署计划与运维手册。
```

---

## 9. `component-extract-rewrite` — 组件萃取与复写（P2）

**是什么 / 给谁**：跨项目萃取组件：6 维耦合分析 → 10 章组件知识手册 → 脱敏标准化复写 → 按 NOTE/COMPONENT/SOLUTION/CODE 路由入库（写入本仓库 `grimoire-files/component-manuals/` 等）。给"把旧项目里好用的组件沉淀到知识库、再复写到新项目"的开发者。

**价值**：★★★★☆（对本仓库而言）。**有真实产出佐证**：`grimoire-files/component-manuals/` 下已有 4 份 10 章结构手册（`eventbus-listener-architecture.md`、`scene-factory-router.md`、`threadpool-executor.md`、`word-template-export.md`，均含"第1章：概述"等标准章节）。这是本次审计中除 `handoff-bundle` 外唯一能直接看到历史产出的"生成型"技能。

**DSH 兼容性（问题最多）**：
- ✅ name 合法；description 103 字符，单行。
- ⚠️ `type`/`version`/`trigger` 被忽略（13 个触发词无效）。
- ❌ **Windows/PowerShell 硬依赖**：`references/grimoire-location-chain.md:6-20` 用 `[Environment]::GetEnvironmentVariable('AIK_GRIMOIRE_HOME','User')` 与 `SetEnvironmentVariable` 定位基址，还依赖 `utools.everythingfind`（Everything）全局搜索和 `D:\JeBrainsWorkSpace\` 硬编码目录。当前 DSH 跑在 WSL（工作目录 `/mnt/d/...`），PowerShell/Everything 均不可用，定位链必然走到 Step 4 失败。
- ❌ **持久化副作用**：定位链会在"自动写缓存"步骤**永久写入用户级环境变量**（`grimoire-location-chain.md:16-20`、`73-80`），这是破坏性操作，DSH 迁移版应删除自动写入，改为"只读探测 + 提示用户"。
- ❌ **gitnexus 前置**：`SKILL.md:37/44/176/198` 要求源项目已 `gitnexus analyze` 建索引；DSH 当前会话**没有 gitnexus 工具**（另一个 gitnexus 审计任务也在确认这点）。需要降级为"可选：有索引更准，无索引用 grep/read"。
- ⚠️ 约束"源项目路径必须为纯英文"（`SKILL.md:205`）在 WSL 下也需要注意。

**内容质量**：
- 手册结构（10 章）、脱敏模式（242 行）、耦合模式（228 行）质量高；但"三层审查"（`references/rewriter-delivery-spec.md:27-45`）与 `code-review` 规则重复。
- 路径表（`grimoire-location-chain.md`）把本仓库当"知识库本体"，对**本仓库自己**其实可以硬编码为"当前工作区即 AikSteinsGrimoire（存在 `grimoire-files/component-manuals/`）"，比 PowerShell 全局搜索可靠得多。

**结论：MIGRATE WITH ADAPTER（重）。** 改动：
1. 重写 `references/grimoire-location-chain.md`：删除 PowerShell/Everything/`SetEnvironmentVariable` 自动写缓存；改为"1) 当前工作区若含 `grimoire-files/component-manuals` 则即为基址（本仓库命中）→ 2) 读 `AIK_GRIMOIRE_HOME` 环境变量 → 3) 询问用户"；产物路径沿用。
2. `SKILL.md:37/44/176/198`、`references/coupling-patterns.md` 中把 gitnexus 从"前置条件"降为"可选增强"。
3. `SKILL.md:205` 纯英文路径约束保留但注明"WSL 下即 /mnt 挂载路径"。
4. 三层审查改为"复用 `code-review` 技能的 quality/security 子命令"，不复制规则。
5. 替换 description。

**建议 catalog description（108 字符）**：

```text
把本仓库或其他项目的组件萃取成知识手册并复写到目标项目时使用：6 维耦合分析、10 章组件手册、脱敏与标准化复写、NOTE/COMPONENT/SOLUTION/CODE 入库路由；需先确定 grimoire 基址。
```

---

## 10. `handoff-bundle` — 跨 Agent 任务交接（P1）

**是什么 / 给谁**：把当前会话的任务上下文和散落资产冻结成可核验交接包（`HANDOFF.md`、`ASSET_MANIFEST.json`、`checksums.sha256`、`git-status.txt`、`changes.patch`、`NEXT_TASK_PROMPT.md`），交给另一个 Agent/会话继续。给"任务没做完要换会话/换 Agent/换人"的开发者。

**价值**：★★★★★。**有真实使用证据**：仓库根 `handoff_bundle/` 就是 2026-10-28 一次真实交接的产物（`handoff_bundle/HANDOFF.md:1-8`：特高压水油年度报告模块移植，源/目标工作区、已完成项、资产清单、校验和齐全）。技能本身写得很成熟：能力三模式（完整/文件/对话）自动降级、多模态降级、敏感信息红线、SHA-256 闭环、接收方"先核验再复述后继续"铁律。

**DSH 兼容性**：
- ✅ name 合法；description 178 字符，单行（<500）。
- ⚠️ `type: Skill`、`version: 1.1.0`（第 4-5 行）被忽略，建议并入 `metadata`。
- ✅ **零外部工具硬依赖**：文件系统、SHA-256、标准 Git 都是 DSH 自带能力；正文不绑定 gitnexus/IDE subagent（`SKILL.md:12-26` 明确声明工具无关）。
- ⚠️ DSH 没有"新建 Codex/GPT 任务"这种能力（`SKILL.md:35/41` 的"任务管理工具"、`NEXT_TASK_PROMPT` 的发送流程）；但技能自带的"文件模式/对话模式"降级正好覆盖——DSH 下应走"文件模式"：生成包 + 给用户路径与启动提示词。
- ✅ 无 `../aIk-coding-style` 链接，也不受改名影响。
- 当前工作区已存在安装先例：`.agents/skills/` 下有 8 个指向 `aik-skills-lab/*` 的符号链接（含 `handoff-bundle -> ../../aik-skills-lab/handoff-bundle`，审计时点观测），本会话 catalog 已能看到这些技能，说明这种安装方式在 DSH 下可用。

**内容质量**：
- 无明显过期/错误；`assets/handoff-skill-portable.md`（395 行）是单文件便携副本，frontmatter 里的 name 是 `codex-handoff-bundle`（`assets/handoff-skill-portable.md:12`）——它在 assets 子目录里**不会被 DSH 当作技能**，无冲突；但若有人把它平铺到 skills root，会多出一个名为 `codex-handoff-bundle` 的技能（可接受，需知情）。
- 小瑕疵：`SKILL.md:41` "并能使用任务管理工具"对 DSH 不成立，建议改为"能创建新会话/子代理时可继续发送"。
- 与 DSH 原生能力（会话 fork、Agent Teams、任务板）有部分功能重叠，但**跨工具/跨模型的可携带产物**仍是它独有的价值。

**结论：MIGRATE AS-IS（可选微调）。** 装上即可用；建议 2 处微调：第 35/41 行的"任务管理工具"改为 DSH 语义（新会话/子代理/或文件模式）；额外键并入 `metadata`。

**建议 catalog description（93 字符）**：

```text
需要把当前任务上下文与散落资产冻结成可核验交接包、交给另一个 Agent 或新会话继续时使用：产出 HANDOFF.md、资产清单、SHA-256 校验、Git 状态与接收方启动提示词。
```

---

## 11. `skill-tester` — 技能质量测试（P3，维护态）

**是什么 / 给谁**：用 RED-GREEN-REFACTOR 验证"技能是否真的约束住模型"：无技能基线（RED）→ 加载技能（GREEN）→ 收紧指令（REFACTOR）。给技能库维护者，不是日常开发任务。

**价值**：★★★☆☆（迁移期 ★★★★）。本次迁移正好需要它来验证迁移后的技能是否还起作用；但对日常 Java SDD 工作流几乎零使用频率。

**DSH 兼容性**：
- ✅ name 合法；description 121 字符，单行；只有 `type`/`version` 额外键。
- ⚠️ **方法论适配**：RED 阶段需要"不加载技能"的对照，DSH 单会话内做不到（catalog 是会话级固定）；实际执行需临时移出技能目录 + 新开会话/子代理。可行但要在正文写清。
- ⚠️ 5 个场景文件里有 5 处 `../../aIk-coding-style/SKILL.md` 链接（`references/code-implementation-scenarios.md:6` 等），依赖 aIk 目录名。
- ✅ 无平台依赖；不写代码文件（只产出测试报告文本）。

**内容质量（过期最严重）**：
- `test-results/comprehensive-report.md` 是 **2026-05-13** 的旧报告：只测了 **5/42** 个技能，且被测对象是 v3.0.0 已合并删除的旧技能名（`code-generator`、`code-style-reviewer`、`code-quality-reviewer`、`code-security-reviewer`、`database-designer`，见该文件 3、5、13-17 行）；其中还断言"表名 `order_info` 非 `T_ORDER` 为违规"（`comprehensive-report.md:156`），与现行小写表名规范相反。**这份报告会直接误导维护者，迁移时应归档或删除。**
- `SKILL.md:175-186` 的"建议优先测试的 10 个关键技能"列表数量（10）与现状（11）不符，且用了旧名 `aIk-coding-style`。
- 方法论本身没问题，5 个场景文件（348–643 行）内容详实，可直接用于迁移后的回归测试。

**结论：MIGRATE WITH ADAPTER。** 改动：
1. `test-results/comprehensive-report.md` 移到 `test-results/archive/` 或删除，并在 SKILL.md 注明"历史报告已过期"。
2. `SKILL.md:175-186` 更新为当前 11 个技能（迁移后再更新一次），名字改 `aik-coding-style`。
3. 正文补一段"在 DSH 中如何做 RED 对照"（临时把技能目录移出 root 后新开会话）。
4. 建议加 `disable-model-invocation: true`（只在 UI 菜单可用、不进模型 catalog）——它是维护型技能，没必要每会话常驻；代价是模型不会自动发现它。
5. 若改 aIk 目录名，同步 5 处链接。

**建议 catalog description（109 字符）**：

```text
维护技能库时使用：用 RED-GREEN-REFACTOR 验证某个技能是否真的约束住模型行为（无技能基线违规、加载后是否拦截、收紧指令回归），输出检测率/误报率与改进项。用户说"测试技能 / 验证技能有效性"时加载。
```

---

## 12. `ccgui-plugin-creator` — CC GUI 插件开发（SKIP）

**是什么 / 给谁**：为 **CC GUI** 桌面客户端写插件（Tier-0 声明式 CSS/JSON 或 Tier-1 单文件 ESM），含 manifest 校验、权限最小化、SDK API 参考。给"开发 CC GUI 插件"的人。

**价值**：对 Java/Spring SDD 工作流 **无价值**（不同产品线）；对 CC GUI 插件开发价值很高，但那是另一个上下文。

**DSH 兼容性（技术上反而最干净）**：
- ✅ name 合法；description 194 字符；**只有 name+description 两个键**，是 11 个里 frontmatter 最接近 DSH 原生格式的。
- ❌ 内容强绑定 CC GUI 宿主：`~/.ccgui-next/plugins/`、`~/ccgui-plugins/`（`SKILL.md:22/28/99`）、"从本地目录安装"的 UI 流程；`references/sdk-api.md:1-6` 是**从 CC GUI 源码 `src/features/plugins/skill/creator-skill-docs.ts` 生成**的（SDK 0.3.20），`SKILL.md:124` 指向 CC GUI 仓库的 `docs/plugin-development-guide.zh-CN.md`（本工作区不存在）。
- ❌ 所有权在外：`ccgui-plugin-creator/.ccgui-managed.json` 记录 `managedBy: ccgui`、`sourceHash` 和文件清单（注意其中是 Windows 反斜杠 `references\\sdk-api.md`），说明这个目录由 CC GUI 工具自动同步/校验。复制进 DSH 会成为一份**无人维护的漂移副本**（SDK 更新后 `sdk-api.md` 必过期）。

**结论：SKIP。** 理由：①与 Java SDD 工作流无关；②由 CC GUI 工具托管、文档由源码生成，迁移必然产生双份和漂移；③本工作区缺少其宿主上下文（CC GUI 源码仓库）。若以后确实想用 DSH 做 CC GUI 插件开发，应作为**独立 profile/独立技能根**安装，并接受 `sdk-api.md` 需按 CC GUI 源码重新生成。

---

## 13. 跨技能问题（迁移前应统一处理）

### 13.1 三处会产出错误代码的规范矛盾（高优先）

| # | 矛盾 | 证据 | 真实情况 |
|---|------|------|---------|
| 1 | 表名**大写**下划线 vs **小写**下划线 | `system-design/SKILL.md:93,276` + `README.md:154`（`T_ORDER`） vs `aIk-coding-style/references/16-naming.md:13`（`lb_module_entity`）、`references/08-database.md` 模板 | 本仓库 `sql/aik_knowledge_tables.sql` 是 `aik_knowledge_category` 等小写下划线；`KnowledgePo.java:33` `@TableName("aik_knowledge")` → **小写正确** |
| 2 | DTO/VO 注解 `@ApiModel` vs `@Schema` | `aIk-coding-style/SKILL.md:174-178`、`code-implementation/SKILL.md:38-42` 硬编码 `@ApiModel` vs 同技能 `references/01-api-doc-annotations.md:3-8` 的"二选一" | 本仓库 **47 个 Java 文件用 `@Schema`**（SpringDoc），`@ApiModel` 不生效 |
| 3 | `@Transactional` 何时用 | `aIk-coding-style/SKILL.md:196-200`（必须确认、单表不加） vs `code-implementation/SKILL.md:94`（直接"实现事务管理"） | aIk 规则更严格且更正确，应统一到它 |

> 建议：迁移前先修好这 3 处，否则 DSH 里的模型会"按技能规范"写出与项目现状冲突的代码。

### 13.2 合并遗留的薄壳 reference（5 个，建议合并/删除）

`system-design/references/api-design-guide.md`（12 行）、`db-design-guide.md`（6 行）、`code-review/references/style-guide.md`（14 行）、`quality-guide.md`（9 行）、`testing/references/unit-test-guide.md`（6 行）——内容只有"去读 aIk 的某几个文件"的链接列表，标题还留着已删除的旧原子技能名（API Designer / Code Style Reviewer / Unit Test Generator）。它们不影响使用，但让 L3 多一层无意义跳转。

### 13.3 陈旧资产与文档（会误导，建议迁移时清理）

| 资产 | 问题 | 证据 |
|------|------|------|
| `skill-tester/test-results/comprehensive-report.md` | 2026-05-13、只测 5/42、全部是已删除的旧技能名；含与大写表名规范一致的错误断言 | `:3`、`:5`、`:13-17`、`:156` |
| 4 个模板页脚 | 落款为已删除的 `spec-devops`/`spec-requirement-analyser`/`spec-qa-analyser` | `devops/assets/deploy-plan-template.md:285`、`devops/assets/ops-manual-template.md:425`、`requirement-engineering/assets/prd-template.md:228`、`testing/assets/test-plan-template.md:201` |
| `README.md` / `agent.md` / `QUICK-START.md` | 都写"10 个技能"（实际 11）；安装说明全是 `C:\Users\arrowInknee\.lingma\skills` 等 Windows/Lingma/Qoder 路径 | `README.md:7`、`agent.md:7/30`、`QUICK-START.md:9-12`、`SKILL-LINK-GUIDE.md` 全文 |
| `batch-pack-skills.ps1` | 用 `d:\...` 绝对路径打包，仅供 Windows 发布用 | `batch-pack-skills.ps1:8-9` |
| 根 `README` 的 L3 表格 | 仍写"name/description/type/version"为 L1，DSH 下 type/version 无效 | `README.md:46`、`agent.md` 类似表述 |

> 这些是**文档/资产**，不是技能本体：迁移 DSH 时不要把它们当技能安装；如需保留知识，转成 `dsh-migration/` 下的参考文档即可。

### 13.4 跨技能相对路径耦合（41 处 / 18 个文件）

- 全部指向 `../aIk-coding-style/...`：code-implementation 21、code-review 9、skill-tester 5、system-design 4、testing 2。
- 在 DSH 中：`references/x.md`（同技能内）天然可用；跨技能 `../` 只有在**所有技能装在同一 root** 时才成立。
- **安装形态决定是否断链**：
  - **符号链接安装**（本工作区 `.agents/skills/` 下已有 8 个此类链接，审计时观测）：`..` 经 symlink 解析回 `aik-skills-lab/`，相对链接**自动有效**，零替换。
  - **复制安装**：必须整组复制全部技能并保持目录名，否则部分链接 404（不阻断技能加载，只是 L3 引用失效）。
- 若把 `aIk-coding-style` 目录改名，这 41 处必须同步替换；只改 frontmatter name 则无需替换。

### 13.5 写入安全（DSH workspace-write 模式）

8 个技能会直接落盘（requirement → `docs/prd/`、system-design → `docs/sdd/`、code-implementation → `src/main/java/` + `docs/api/` + SQL、testing → `src/test/` + `docs/test/`、devops → 配置文件/`pom.xml`/`deploy/`、component-extract-rewrite → 目标项目代码 + grimoire 手册、handoff-bundle → `handoff_bundle/`、ccgui-plugin-creator → 插件目录），正文普遍没有"写前确认"门禁（`aIk-coding-style` 事务规则是唯一例外）。DSH 的 workspace-write 策略下代理可直接写工作区文件，建议迁移版统一加一句：

> 落盘前先列出将要创建/修改的文件路径与摘要，等用户确认后再写；一次 full 流程结束后给出变更清单。

最需要改的是 `devops/SKILL.md:131-137`（"如无，添加依赖"直接改 pom）、`code-implementation/SKILL.md:165-173`（full 直接生成整套代码）。

### 13.6 重复与分叉风险

- **安装副本 vs 源库**：`.agents/skills/` 下已有 8 个指向 `aik-skills-lab/<skill>` 的符号链接（审计时观测），这种形态不会漂移；若改成"复制"，两份内容会分叉。
- **便携副本**：`handoff-bundle/assets/handoff-skill-portable.md`（name `codex-handoff-bundle`）在 assets 内不会被发现；平铺安装才会多一条技能。
- **仓库卫生**：根 `.gitignore` **没有**忽略 `.agents/`（也没有 `.dsh/`）；当前 `.agents/skills/` 下只有指向库内的符号链接，git 会把它们记成 symlink 条目（不会展开内容），入库可接受；但若改成复制安装，就会把整套技能提交两遍——建议先决定"入库 symlink"还是"忽略目录"。

### 13.7 子命令模式在 DSH 下的表现（不阻断）

DSH 没有子命令概念：模型只会加载整份 SKILL.md，然后根据用户措辞自己挑子命令（11 份正文都完整列出了子命令，所以可行）。风险是 description 里若只写"全流程技能"，用户说"设计订单表"时模型可能选错技能（见 §13.1 描述建议：每份 description 都写明输入→输出边界）。

---

## 14. 总体建议

### 14.1 Top 5 建议

1. **先修 1 行名称（P0，审计时点已执行）**：`aIk-coding-style/SKILL.md:2` → `name: aik-coding-style`。不改则 DSH 永远看不到这个技能（无 UI 报错），而它是其余 5 个技能的规则来源。目录名可暂不改，41 处相对链接继续有效。
2. **迁 7 个高价值技能，其余按需**：`aik-coding-style`、`requirement-engineering`、`system-design`、`code-implementation`、`code-review`、`testing`、`handoff-bundle`。这 7 个覆盖 SDD 全链路 + 已证明可用的交接能力；`devops` 作为第 8 个（按需，审计时已一并安装）、`component-extract-rewrite` 重适配后再装、`skill-tester` 设 `disable-model-invocation` 或后装、`ccgui-plugin-creator` 不迁。
3. **description 全部重写为"做什么 + 何时加载 + 输入→输出"**：DSH 只靠 description 路由，现有 5 个技能都是"XX 全流程技能——…"的同构句式（尤其 requirement/system-design/code-implementation/testing/devops），互相竞争；按附录 A 替换后边界清晰。
4. **迁移前修 3 处规范矛盾**（表名大小写、`@ApiModel`/`@Schema`、`@Transactional`），否则会"按规范写出错误代码"；顺带清理 §13.2/§13.3 的薄壳与过期资产。
5. **安装形态用符号链接、并决定 git 策略**：装到 `<repo>/.dsh/skills/`（或已可用的 `<repo>/.agents/skills/`），每条技能一个 symlink 指回 `aik-skills-lab/<skill>`。好处：单份维护、41 处 `../` 链接自动有效、工作区已验证可被发现（catalog 已出现 8 个 aik 技能）。同时决定 `.agents/skills`/`.dsh/skills` 是否加进 `.gitignore`，避免把技能库提交两遍。

### 14.2 建议的最小高价值子集

| 技能 | 角色 | 建议 description 字符 |
|------|------|:---:|
| `aik-coding-style` | 规则地基（写/改代码前加载） | 130 |
| `requirement-engineering` | SDD 第 1 步：需求 → PRD | 107 |
| `system-design` | SDD 第 2 步：PRD → SDD | 121 |
| `code-implementation` | SDD 第 3 步：SDD → 代码 | 148 |
| `code-review` | SDD 第 4 步：代码审查 | 109 |
| `testing` | SDD 第 5 步：测试 + 覆盖率 | 116 |
| `handoff-bundle` | 断点续传 / 换会话 / 换 Agent | 93 |
| **合计 7 个** | | **824 字符** |

可选第 8：`devops`（110 字符）——有部署需求再加。`component-extract-rewrite`（108）建议在重写 grimoire 定位链后单独上线。`skill-tester`（109）建议 `disable-model-invocation: true`（用户菜单可用、模型 catalog 不占位）。

> 审计时点观测：`.agents/skills/` 已装上表 7 个 + `devops`（即第 8 个也已装）；未安装的是 `component-extract-rewrite`（需先重写定位链）、`skill-tester`（维护型）、`ccgui-plugin-creator`（SKIP），与建议一致。剩余主要工作是**替换 description**（catalog 实测仍是旧文案）与**修 3 处规范矛盾**。

### 14.3 catalog 描述预算

- 现状：11 个技能 description 合计 **1491 字符**（若全装，每会话常驻）。
- 建议子集：7 个合计 **824 字符**；加 `devops` 为 934；加全部可迁 10 个为 **1151 字符**（仍低于现状，且每份都 ≤200 字符）。
- DSH 渲染上限 500 字符/条，本库最长 194（ccgui），**不会截断**；无需担心。
- 经验值：一个会话里 model-invocable 技能超过 ~12–15 个后，catalog 的"选哪个"歧义成本开始明显；建议控制在该量级内。

### 14.4 风险清单

| # | 风险 | 触发条件 | 缓解 |
|---|------|---------|------|
| 1 | 技能静默装不上 | name 不合法（当前仅 aIk） | 安装后对照 catalog 名单；改 1 行 name |
| 2 | L3 相对链接 404 | 复制安装 / 只装部分 / 改 aIk 目录名 | 整组符号链接安装；若改名则批量替换 41 处 |
| 3 | 按规范写出错误代码 | 未修 §13.1 三处矛盾 | 迁移前先修 |
| 4 | 未经确认写文件/改 pom | workspace-write + 技能无门禁 | 迁移版加"落盘前确认"句（§13.5） |
| 5 | grimoire 定位链在 Linux/WSL 必失败 | component-extract-rewrite 原样安装 | 重写为"工作区命中 → 环境变量 → 询问"，删除自动写环境变量 |
| 6 | 陈旧资产继续误导 | skill-tester 旧报告、spec-* 页脚、README 10/11 | 归档/清理（§13.3） |
| 7 | 双份维护漂移 | 复制安装而非 symlink | symlink；或明确只在一处改 |
| 8 | catalog 注意力成本 | 11 个全装 + DSH 自带技能 | 只装 7–8 个；维护型技能用 disable-model-invocation |
| 9 | 通用名冲突 | `testing`/`devops`/`code-review` 与未来其他源同名 | 当前无冲突；若冲突按 root rank（project > user）静默覆盖，届时再加 `aik-` 前缀 |
| 10 | 子命令误选 | description 只写"全流程" | 用附录 A 的"输入→输出"式 description |

### 14.5 落地步骤（可直接执行）

1. **改名（P0，审计时点已执行）**：`aIk-coding-style/SKILL.md:2` → `name: aik-coding-style`（工作树已改；目录名与 41 处链接未动，符合最小改动）。可选：再把 18 个 reference 的"来源"头与 41 处链接统一改目录名（一次性替换，成本可控，非必需）。
2. **修矛盾**：system-design 表名（`:93/:276`）→ 小写；code-implementation `@Transactional`（`:94`）与注解（`:38-42`）；code-implementation SQL 目录（`:145/:216`）；devops pom 依赖加确认（`:131-137`）。
3. **选 root**：推荐 `<repo>/.dsh/skills/`（DSH 项目级 root，`findProjectRoot` 会命中本仓库根）；也可用 `<repo>/.agents/skills/`（已有先例且被发现）。
4. **符号链接安装**（审计时点已完成 8 个：上表 7 个 + `devops`；下述命令供新环境复现）：
   ```bash
   cd /mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire
   mkdir -p .agents/skills            # 或 .dsh/skills
   for s in aIk-coding-style requirement-engineering system-design code-implementation \
            code-review testing handoff-bundle devops; do
     ln -sfn "../../aik-skills-lab/$s" ".agents/skills/$s"
   done
   ```
   （DSH 显示名取自 frontmatter，链接名用 `aIk-coding-style` 不影响 catalog 显示 `aik-coding-style`；若已把目录改名则相应调整。）
5. **验证 catalog**：新开/刷新 DSH 会话，确认 `<available_skills>` 出现所选子集（当前观测 8 条），name 与预期一致（`aik-coding-style` 而非 `aIk-coding-style`），无重复、无缺失。
6. **验证加载与资源**：让模型 `skill` 加载 `code-implementation`，然后按 `references/skeleton-guide.md` → `../aIk-coding-style/references/02-module-directory.md` 打开 L3 文件，确认路径可读。
7. **验证路由**：分别用"审查这段代码"、"为 OrderService 写单元测试"、"设计订单表"试触发，确认命中 code-review / testing / system-design 而不是相邻技能（description 区分度检验）。
8. **git 策略**：在根 `.gitignore` 追加 `.dsh/skills/`（或决定入库 `.agents/skills` 的 symlink——当前 `git status` 显示 `.agents/` 为未跟踪目录），避免技能库以"复制"形式被提交两遍。

### 14.6 验收标准

- 所选子集（§14.2 的 7 个；当前观测 8 个含 `devops`）在 DSH catalog 中全部出现，name 合法且无静默丢失。
- 每个技能的 description 为单行、≤200 字符、含"何时加载"语义。
- 从任一技能按相对路径打开 `references/`、`assets/` 成功；`../aIk-coding-style/...` 链接可打开（至少一个样本）。
- 技能加载后正文不含 `type`/`version`/`trigger` 依赖（这些键已被并入 metadata 或保留但无关）。
- 3 处规范矛盾已修正，抽查"表名/注解/事务"三类输出与仓库现状一致。
- 若安装 skill-tester：旧测试报告已归档，且 DSH 下 RED 对照方法写入正文。

---

## 附录 A：建议 catalog description 汇总（可直接粘贴到各 SKILL.md 第 3 行）

| 技能 | 建议 description | 字符 |
|------|-----------------|:---:|
| `aik-coding-style` | 编写或修改 Java 后端代码（Spring Boot 2.7 + MyBatis-Plus + Lombok）时使用：统一模块目录、命名、分层、DTO/VO/PO 注解、注释与日志、Mapper 与建表规范，并提供提交前检查清单。动手写代码或新建模块前加载。 | 130 |
| `requirement-engineering` | 把原始需求整理成 PRD 时使用：提取功能点、澄清模糊点、检测需求冲突、编写用户故事与 Gherkin 验收标准、评估技术可行性、按 MoSCoW 排优先级。用户说"分析需求 / 写 PRD / 排优先级"时加载。 | 107 |
| `system-design` | 从 PRD 产出系统设计（SDD）时使用：分层架构、数据库表与索引、RESTful API 契约、核心流程与状态机、技术选型决策树、P0-P3 设计评审，产出 docs/sdd。用户说"设计架构 / 建表 / 设计接口 / 技术选型"时加载。 | 121 |
| `code-implementation` | 按 SDD 实现 Java 代码时使用：生成 Controller/Service/Mapper/PO/DTO/VO 骨架、填充业务逻辑、生成 API 文档与建表迁移 SQL、补模块 README；产出须符合 aik-coding-style。用户说"实现功能 / 写代码 / 生成骨架"时加载。 | 148 |
| `code-review` | 审查 Java 后端代码时使用：风格（对照 aik-coding-style）、质量（N+1/空指针/事务边界/资源泄漏）、安全（SQL 注入/越权/敏感日志）、缺陷模式四维检查，按 P0-P3 输出带修复建议的报告。 | 109 |
| `testing` | 生成或评审 Java 测试时使用：JUnit 5 + Mockito + AssertJ 单测、@SpringBootTest 集成测试、API 测试文档、JaCoCo 覆盖率阈值、测试数据隔离策略，并按准入/准出门禁判断能否发布。 | 116 |
| `devops` | 准备 Java 应用部署上线时使用：多环境配置、jar/tar 打包、systemd/Docker/Compose 脚本、Actuator 健康检查、Logback 日志轮转、故障排查与回滚方案，产出部署计划与运维手册。 | 110 |
| `component-extract-rewrite` | 把本仓库或其他项目的组件萃取成知识手册并复写到目标项目时使用：6 维耦合分析、10 章组件手册、脱敏与标准化复写、NOTE/COMPONENT/SOLUTION/CODE 入库路由；需先确定 grimoire 基址。 | 108 |
| `handoff-bundle` | 需要把当前任务上下文与散落资产冻结成可核验交接包、交给另一个 Agent 或新会话继续时使用：产出 HANDOFF.md、资产清单、SHA-256 校验、Git 状态与接收方启动提示词。 | 93 |
| `skill-tester` | 维护技能库时使用：用 RED-GREEN-REFACTOR 验证某个技能是否真的约束住模型行为（无技能基线违规、加载后是否拦截、收紧指令回归），输出检测率/误报率与改进项。用户说"测试技能 / 验证技能有效性"时加载。 | 109 |

> 全部单行、≤200 字符、可直接作为 DSH frontmatter `description` 使用。`ccgui-plugin-creator` 建议 SKIP，不提供迁移 description（其现有 194 字符描述本身已合规）。

## 附录 B：11 个 bundle 清单与 L3 资源

| 技能 | SKILL.md 行数 / 字节 | references | assets | scripts | 历史产出证据 |
|------|:---:|---:|---:|---:|---|
| aIk-coding-style | 210 / 9622 | 18（1825 行） | — | 2（bom-checker、naming-validator） | 本仓库大量 Java 代码遵循其规范 |
| ccgui-plugin-creator | 124 / 8199 | 1（sdk-api.md 408 行） | — | — | `.ccgui-managed.json` 托管记录 |
| code-implementation | 245 / 8338 | 4 | 2 | — | — |
| code-review | 197 / 6726 | 3 | 3 | — | — |
| component-extract-rewrite | 210 / 7363 | 5 | — | — | `grimoire-files/component-manuals/` 4 份手册 |
| devops | 276 / 7717 | 1 | 2 | — | — |
| handoff-bundle | 152 / 10813 | 4 + schema | 1（便携版） | — | 仓库根 `handoff_bundle/`（2026-10-28 真实交接） |
| requirement-engineering | 261 / 8298 | 3 | 1 | — | — |
| skill-tester | 188 / 6201 | 5（场景 2298 行） | — | — | `test-results/`（2026-05-13，已过期） |
| system-design | 279 / 8151 | 5 | 2 | — | — |
| testing | 242 / 7042 | 4 | 2 | — | — |

> 合计：`SKILL.md` 2384 行 / 约 88 KB；全部 L3 资源约 13,000 行。正文仅在技能被调用时进入上下文，常驻成本仅为附录 A 的 description。

# GitNexus 六技能 与 工作区散落技能资产 —— DSH 迁移审计

- 审计对象：`.claude/skills/gitnexus/{gitnexus-cli,gitnexus-exploring,gitnexus-impact-analysis,gitnexus-debugging,gitnexus-refactoring,gitnexus-guide}/SKILL.md`，以及工作区内其他潜在可迁移的 skill 类资产
- 审计性质：**只读**调查 + 本文件。未修改任何其他文件，未改动 `~/.dsh`，未提交
- 审计时间：2026-11-10（Asia/Shanghai）
- 环境：Windows 宿主 + WSL（工作区 `/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire`），Node `v24.19.0`，GitNexus npm 包 `1.6.4`（WSL 用户级安装于 `~/.local/share/gitnexus/`），DSH 由 Web profile 承载
- 证据约定：`文件:行号` 指向本次实测的工作区/安装目录文件；命令输出见第 10 节

---

## 0. 结论摘要（TL;DR）

1. **GitNexus MCP 今天对 DSH 不可用。** DSH Web profile 里没有任何 MCP server 配置（`~/.dsh/profiles/web/cordis.patch.yml` 全文 38 行无 MCP 条目、profile 内无 `mcp*.json` 配置、`mcp__*` 工具与 `read_mcp_resource`/`list_mcp_resources` 资源工具在本会话均不存在）。能力本身在（base bundle 统一挂载了 `dsh-mcp-resources`），但**没有 server 就什么都不出现**（证据：`dsh-base/README.zh.md:64`，`dsh-base/cordis.patch.yml:492-493`）。
2. 这 6 个技能的**结构全部是合法 DSH skill**（用 DSH 真实 provider 实测：6/6 `status: OK`，description 170–223 字符，远小于目录上限 500）。问题不在格式，而在**正文依赖的 `gitnexus_*` 工具与 `gitnexus://` 资源在 DSH 中一个都不存在**。
3. 逐技能裁决（本轮**建议 0 个迁入**）：

   | 技能 | 判定 | 一句话 |
   |---|---|---|
   | gitnexus-impact-analysis | MIGRATE WITH ADAPTER（受 MCP 阻塞） | 价值最高（AGENTS.md 强制条款的来源），但 100% 依赖 `impact`/`detect_changes`/`processes` |
   | gitnexus-exploring | MIGRATE WITH ADAPTER（受 MCP 阻塞） | 依赖 `query`/`context` + 4 类资源，无 MCP 无法执行 |
   | gitnexus-debugging | MIGRATE WITH ADAPTER（受 MCP 阻塞） | 依赖 `query`/`context`/`cypher` + 资源，可归并为 exploring/impact 的补充 |
   | gitnexus-refactoring | MIGRATE WITH ADAPTER（受 MCP 阻塞） | 工具密度最高（24 处），且 `rename` 是 DSH 无替代的能力 |
   | gitnexus-cli | MIGRATE WITH ADAPTER（可选、随 MCP 阶段） | 唯一基本不依赖 MCP 的工具技能；但 CLI 在本环境需改命令前缀 |
   | gitnexus-guide | **SKIP** | GitNexus MCP 服务器自带 instructions 会被 DSH 注入系统提示词（`mcp-client/README.zh.md:12`），此技能是冗余的第三方副本，且 MCP 未启用时零价值 |

4. 迁移的真正前置条件不是"改文案"，而是三步：**① 在 profile 里加一条 `dsh-mcp-client` 配置；② 在 WSL 侧注册索引（`gitnexus index`，或重跑 `analyze`）；③ 改 6 个技能的命名/资源/命令前缀**。未完成 ①② 就迁入 ③，等于给模型一套会调用不存在工具的指令——会把"合规"退化成"编造"。
5. 附带发现（供 Lead 决策，均只读观测）：
   - `.codex/config.toml:2` 指向 `E:\software\nvm\nodejs\gitnexus.cmd`，该文件在当前环境**不存在**（E: 的 nvm `nodejs` 已切到 v24.19.0，各版本 `node_modules` 内均无 gitnexus）→ Codex 侧的 GitNexus 接线已失效。
   - `.qoder-backup/mcp.json:1-11` 只有 Qoder 自身的 http MCP（含 `x-mcp-key` 凭据），**没有 GitNexus**；不要把这个模式/凭据复制进 DSH。
   - Windows 侧 `C:\Users\arrowInknee\.gitnexus\registry.json` 里本仓库仍按 `D:\...` 路径注册（`:51-56`），而 WSL 的 `~/.gitnexus/registry.json` **不存在** → WSL 下 `gitnexus list`/`query` 直接报 "No indexed repositories found"。
   - 索引已 **stale**：`.gitnexus/meta.json:3` 记录 `0fbcf5e`，当前 HEAD 为 `3b1c577`（`gitnexus status` 实测）。
   - 安装包 `1.6.4` 还带 **第 7 个技能 `gitnexus-pr-review`**（`~/.local/share/gitnexus/node_modules/gitnexus/skills/gitnexus-pr-review.md`），仓库 `.claude/skills/gitnexus/` 里没有；本次不在任务范围，可在 P2 评估。
   - `AGENTS.md:10-21` 的 "MUST 用 `gitnexus_impact` / `gitnexus_detect_changes`" 条款会被 DSH 当作工作区指令注入（本会话 system 提示即含 `Instructions from: AGENTS.md`），但工具不存在 → 存在"要求 agent 做一件它做不到的事"的风险，见 §8 R5。

---

## 1. 关键问题 1：这 6 个技能依赖什么？

### 1.1 技能正文里的"伪工具名"其实是 MCP 裸工具名

6 个技能通篇用 `gitnexus_query` / `gitnexus_context` / `gitnexus_impact` / `gitnexus_rename` / `gitnexus_detect_changes` / `gitnexus_cypher` 这类写法（例：`.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md:20,22,31,35,58,61,76,79,89`；`gitnexus-refactoring/SKILL.md` 内 24 处）。这些**不是 MCP 服务器真实注册的工具名**。实测 `gitnexus@1.6.4` 的 MCP 服务器注册的是**裸名**：

| 真实注册名（`dist/mcp/tools.js`） | 行号 |
|---|---|
| `list_repos` | `:27` |
| `query` | `:45` |
| `cypher` | `:108` |
| `context` | `:167` |
| `detect_changes` | `:214` |
| `rename` | `:245` |
| `impact` | `:280` |

> 因此 `gitnexus_*` 是 GitNexus 技能文档自己约定的人类别名（各宿主里展示用的名字），**不是 MCP 协议里的真实工具名**。DSH 只按 `mcp__<serverName>__<rawName>` 解析（`dsh-mcp-client/README.zh.md:75`），所以 `gitnexus_impact` 在 DSH 下**不会解析成任何工具**。

### 1.2 MCP 资源 URI

技能大量使用 `READ gitnexus://repo/{name}/context` 之类（`gitnexus-exploring/SKILL.md:19-23,43-46`；`gitnexus-guide/SKILL.md:49-54`）。服务器实际提供的资源清单（`dist/mcp/resources.js`）：

| URI / 模板 | 行号 |
|---|---|
| `gitnexus://repos` | `:14` |
| `gitnexus://setup` | `:20` |
| `gitnexus://repo/{name}/context` | `:33` |
| `gitnexus://repo/{name}/clusters` | `:39` |
| `gitnexus://repo/{name}/processes` | `:45` |
| `gitnexus://repo/{name}/schema` | `:51` |
| `gitnexus://repo/{name}/cluster/{clusterName}` | `:57` |
| `gitnexus://repo/{name}/process/{processName}` | `:63` |
| `gitnexus://group/{name}/contracts` / `status` | `:69` / `:75` |

关键点：**"READ 某 URI"在 DSH 不是原生动作**。DSH 通过 `dsh-mcp-resources` 统一提供 3 个共享工具 `list_mcp_resources` / `list_mcp_resource_templates` / `read_mcp_resource`，且都要求显式 `server` 参数（`dsh-mcp-resources/README.zh.md:38`）。没有已配置 server 时这三个工具根本不出现（同文 `:34`）。

### 1.3 CLI 依赖

技能里的命令是 `npx gitnexus analyze|status|clean|wiki|list`（`gitnexus-cli/SKILL.md:15,31,39,52,69`）。实测 `gitnexus@1.6.4` 的 CLI 命令集（`node .../dist/cli/index.js --help`）：`setup / analyze / index / serve / mcp / list / status / doctor / clean / remove / wiki / augment / publish / query / context / impact / cypher / detect-changes / eval-server`；命令注册行见 `dist/cli/index.js:13,17,50,56,62,66,70,74,78,84,90,106,110,121,130,138,146,151,160`，其中 `mcp` 即 stdio MCP 服务器（`dist/cli/index.js:62`）。

**在本 WSL 环境中 `npx gitnexus` 跑不通**（实测报 `npm error code EROFS`：npm cache `~/.npm` 在当前 DSH 文件沙箱下只读；同时 `v22.23.2/bin/gitnexus` 是断链，目标 `v22.23.2/lib/node_modules/gitnexus` 不存在）；但**直接 `node <绝对路径>/dist/cli/index.js` 可以正常跑**（`--help` / `status` / `list` 均成功）。这决定了 §6 的命令前缀适配方案。

### 1.4 各 IDE 的接线现状（对照）

| 宿主 | 配置文件 | 现状 | 证据 |
|---|---|---|---|
| Claude Code | `AGENTS.md`（= `CLAUDE.md`，二者 `diff` 完全一致） | 有强制条款 + 技能索引，但 `.claude/` 无 `settings.json`，**没有 hook** | `AGENTS.md:1-43`；`.claude/` 下仅 `skills/` |
| Codex | `.codex/config.toml` | `[mcp_servers.gitnexus]`，`command=E:\software\nvm\nodejs\gitnexus.cmd`，`args=["mcp"]`；**该二进制当前不存在** | `.codex/config.toml:1-5`；`/mnt/e/software/nvm/nodejs/` 实测无 `gitnexus.cmd` |
| Qoder | `.qoder-backup/mcp.json` | 只有 `qoder` http server，**无 GitNexus** | `.qoder-backup/mcp.json:2-10` |
| Qoder 权限 | `.qoder/settings.local.json` | 仅在一条 allow 命令串里提到 `gitnexus --version`，不是 MCP 配置 | `.qoder/settings.local.json:34` |
| **DSH** | `~/.dsh/profiles/web/cordis.patch.yml` | **无任何 MCP 条目** | 全文 38 行，无 `mcp` 字样（`grep` 实测）；`package.json` bundles 亦无 MCP bundle |
| GitNexus 自身数据 | `.gitnexus/` | `lbug` 55MB + `meta.json`；索引 stale；meta 记录的是 Windows 路径 | `.gitnexus/meta.json:2-13`；`gitnexus status` 实测 |

### 1.5 DSH 侧 MCP 可用性判定（问题 1 的结论）

**不可用。** 三层证据：

1. **没有配置**：`~/.dsh/profiles/web/cordis.patch.yml`（逐行读完，38 行）只有 `ui-settings-general`、`llm-pi-ai`、`better-sidebar`、`plugin-market`、`agent-default-model`、`hmr-memory`、`llm-deepseek`、`cot-translation` 八个条目；`~/.dsh/profiles/web/` 下无任何 `mcp*.json`；对 profile 的 `package.json/cordis.yml/cordis.patch.yml/hub-settings.json` grep `gitnexus` 无命中。
2. **能力在、默认无 server**：base bundle 统一挂载 `@deepseek-ai/dsh-mcp-resources`（`dsh-base/cordis.patch.yml:492-493`、`dsh-base/README.zh.md:64`），`dsh-mcp-client` 随 dsh 分发但**默认不启用任何服务器**（`dsh-mcp-client/README.zh.md:12`）。
3. **会话侧可观测事实**：本会话工具目录里**没有任何 `mcp__*` 工具**，也没有 `read_mcp_resource` 等 3 个资源工具——按设计，这就是"调用方作用域无已配置 MCP server"的状态（`dsh-mcp-resources/README.zh.md:34`）。

> 想启用只需在 profile 的 patch 层加一条配置（模板见 §6.4），**但那是 `~/.dsh` 的用户级改动，超出本审计写权限，未执行**。

---

## 2. 关键问题 2：DSH MCP 工具命名约定与精确映射

### 2.1 约定

`dsh-mcp-client` 把每个服务器工具的模型可见名固定为 **`mcp__<serverName>__<rawName>`**（`dsh-mcp-client/README.zh.md:75`，实现 `dist/index.js:97`；与 Claude Code / Codex 同形），`serverName` 必须是 `[A-Za-z0-9_-]{1,32}` 且在同一注册作用域唯一（同文 `:58`）。`gitnexus` 完全合规。

### 2.2 精确映射表（改造时的唯一真源）

设 DSH 配置 `serverName: gitnexus`：

| 技能正文写法 | MCP 裸名 | DSH 工具名（模型可见） |
|---|---|---|
| `gitnexus_query({query})` | `query` | **`mcp__gitnexus__query`** |
| `gitnexus_context({name})` | `context` | **`mcp__gitnexus__context`** |
| `gitnexus_impact({target, direction})` | `impact` | **`mcp__gitnexus__impact`** |
| `gitnexus_rename({symbol_name, new_name, dry_run})` | `rename` | **`mcp__gitnexus__rename`** |
| `gitnexus_detect_changes({scope})` | `detect_changes` | **`mcp__gitnexus__detect_changes`** |
| `gitnexus_cypher({query})` | `cypher` | **`mcp__gitnexus__cypher`** |
| `gitnexus_list_repos()` / `list_repos()` | `list_repos` | **`mcp__gitnexus__list_repos`** |

| 技能正文写法 | DSH 实际调用形态 |
|---|---|
| `READ gitnexus://repos`（`exploring:19`） | `list_mcp_resources({server:"gitnexus"})` 后 `read_mcp_resource({server:"gitnexus", uri:"gitnexus://repos"})` |
| `READ gitnexus://repo/{name}/context` | `read_mcp_resource({server:"gitnexus", uri:"gitnexus://repo/AikSteinsGrimoire/context"})` |
| `READ gitnexus://repo/{name}/process/{name}`（模板） | 先 `list_mcp_resource_templates({server:"gitnexus"})`，展开模板后 `read_mcp_resource({server:"gitnexus", uri:"…"})` |
| `READ gitnexus://repo/{name}/schema`（`guide:54`） | `read_mcp_resource({server:"gitnexus", uri:"gitnexus://repo/AikSteinsGrimoire/schema"})` |

> `server` / `uri` / `cursor` 参数与 `list → 展开模板 → read` 的三步法见 `dsh-mcp-resources/README.zh.md:38`。`{name}` 占位符应替换为实测仓库名 `AikSteinsGrimoire`（`.gitnexus/meta.json:2` 的 repoPath + 注册表 `C:\Users\arrowInknee\.gitnexus\registry.json:51` 的 name；也可在运行时先 `list_mcp_resources` 发现）。

### 2.3 DSH 技能发现机制（问题 4）

`dsh-skill-filesystem` 默认扫描根（rank 越高越靠后，同名冲突按 rank 决胜）：

| Rank | 来源 | 路径 |
|---|---|---|
| 100 | `project-dsh` | `<projectRoot>/.dsh/skills` |
| 200 | `project-agents` | `<projectRoot>/.agents/skills` |
| 300 | `custom` | `Config.customSkillDirs` |
| 400 | `user-dsh` | `<dshHome>/skills`（默认 `~/.dsh/skills`） |
| 500 | `user-agents` | `<agentsHome>/skills`（默认 `~/.agents/skills`） |

（`dsh-skill-filesystem/README.zh.md:50-54`；`<projectRoot>` = 最近的含 `.git` 的祖先，`:56`）

- **`.claude/skills` 不在扫描范围内**——所以今天 6 个技能在 DSH 目录里完全不可见（本会话前的技能目录只列内置技能，无任何 `gitnexus-*`）。
- 可扫描形态只有两种：根目录下**顶层** `<name>/SKILL.md` 目录包，或**顶层** `<name>.md` 平铺文件；**刻意不支持嵌套 `**/SKILL.md`**（`:36`）。
  - 推论：`.claude/skills/gitnexus/<name>/SKILL.md` 相对 `.agents/skills` 是**两层**嵌套，直接整体链接 `.claude/skills/gitnexus` 也不会被发现；必须**逐个**链接/复制到根的顶层（`<root>/gitnexus-cli/` 或 `<root>/gitnexus-cli.md`）。
- 符号链接可用：目录条目与加载都会跟随符号链接并保留发现路径（`:40`），watch 也支持（`:74`）。**本会话已实测**：`.agents/skills/` 下的符号链接被实时发现（见 §9 的实测记录）。
- frontmatter：必填 `name`（必须 kebab-case，正则 `/^[a-z0-9]+(?:-[a-z0-9]+)*$/`，见 `dsh-skill/lib/index.js:17`）与 `description`；可选 `whenToUse`、`metadata`、`disable-model-invocation`、`user-invocable`（`:36,38`）。非法名字/缺 frontmatter 会**逐条告警并丢弃**（`dsh-skill-filesystem/lib/index.js:686-707`）。
- 目录描述长度可由 `catalogDescriptionMaxLength` 限制，默认 **500** 字符（`dsh-tool-skill/README.zh.md:48`）；6 个技能的 description 实测 170–223 字符，**无需截断**。
- `.gitignore:10` 忽略 `.claude/`，`:53-54` 忽略 `AGENTS.md`/`CLAUDE.md` → **技能源文件不入库**，这影响迁移策略（见 §6.3）。

---

## 3. 逐技能审计

> 每个技能都先给"是否结构合法"的实测结果：用 `dsh-migration/tools/list-skills.mjs`（它直接 import 安装版 `@deepseek-ai/dsh-skill-filesystem` 的真实 provider）以 `.claude/skills/gitnexus` 为 root 扫描，**6/6 `status: OK`，0 warnings, 0 failures**。

### 3.1 `gitnexus-cli`（83 行）

- **做什么**：`analyze` / `status` / `clean` / `wiki` / `list` 五个 CLI 子命令与 flag 参考，以及"索引后先读 context 资源"的收尾步骤（`SKILL.md:10-72`）。
- **价值**：唯一基本不依赖 MCP 工具的技能；是 MCP 可用后"保索引新鲜"的必要配套（stale 会让所有 MCP 工具结论失真）。
- **在 DSH 下会坏的精确点**：
  - `:8` "All commands work via `npx`" —— 本环境 `npx gitnexus` 失败（EROFS / PATH 无有效 gitnexus）。
  - `:18` "generates CLAUDE.md / AGENTS.md context files" —— DSH 不读 `CLAUDE.md`（DSH 读工作区 `AGENTS.md`；本会话 system 提示已注入 `AGENTS.md`），但该句只是描述产物，可不改。
  - `:26` "In Claude Code, a PostToolUse hook detects staleness…" —— 宿主专属，且本仓库 `.claude/` 下没有 hook 配置，**这句话在当前工作区本身就不成立**。
  - `:55` `~/.gitnexus/config.json`、`:72` `~/.gitnexus/registry.json` —— WSL 下 `~/.gitnexus/` 不存在（只有 Windows 侧的 `C:\Users\arrowInknee\.gitnexus\`），需按实际 HOME 说明。
  - `:76-77` "Read `gitnexus://repo/{name}/context`" 与 "use the other GitNexus skills" —— 资源读取方式与技能名都需适配。
  - `:82` "Restart Claude Code to reload the MCP server" —— DSH 无此动作；DSH 侧等价做法是**改一下该条 `dsh-mcp-client` 配置触发原地重载**（`dsh-mcp-client/README.zh.md:93`）或重启 harness。
- **判定**：**MIGRATE WITH ADAPTER**（可选，性价比取决于是否启用 MCP；本轮不迁）。
- **精确适配编辑**（逐行）：
  | 行 | 原文 | 改为 |
  |---|---|---|
  | 8 | ``All commands work via `npx` — no global install required.`` | ``All commands work via `gitnexus`（本环境用 `dsh-migration/bin/gitnexus` 包装脚本，见 §6.2）—— 无需全局安装。`` |
  | 15/31/39/52/69 | `npx gitnexus <cmd>` | `dsh-migration/bin/gitnexus <cmd>` |
  | 26 | …`In Claude Code, a PostToolUse hook…` | 删掉 hook 句；改为：`DSH 无 PostToolUse hook；改动后用 gitnexus status 自检，stale 则重跑 analyze` |
  | 55 | `~/.gitnexus/config.json` | `$GITNEXUS_HOME/config.json（默认为当前用户 HOME 下的 ~/.gitnexus/）` |
  | 76 | `Read gitnexus://repo/{name}/context` | `read_mcp_resource({server:"gitnexus", uri:"gitnexus://repo/AikSteinsGrimoire/context"})` |
  | 77 | `use the other GitNexus skills (exploring, debugging, impact-analysis, refactoring)` | 改用 DSH 目录名 `gitnexus-exploring / gitnexus-debugging / gitnexus-impact-analysis / gitnexus-refactoring` |
  | 82 | `Restart Claude Code to reload the MCP server` | `重启 harness，或改动 dsh-mcp-client 配置条目触发原地重载`（`dsh-mcp-client/README.zh.md:93`） |
- **建议 catalog description**：`运维 GitNexus 本地索引：用 analyze/status/clean/wiki/list 建库、查新鲜度、清库、生成 wiki；依赖 GitNexus CLI 与 .gitnexus/ 数据目录。`

### 3.2 `gitnexus-exploring`（78 行，10 处工具 + 9 处资源）

- **做什么**：理解架构 / 追执行流的标准五步：`gitnexus://repos` → `context` 资源 → `query` → `context` 工具 → `process/{name}` 资源（`SKILL.md:16-24`），外加 checklist 与资源/工具速查表（`:28-65`）。
- **价值**：高。`query` 返回"按执行流分组"的结果，是 grep 的语义替代；这正是 AGENTS.md 推荐的探索方式（`AGENTS.md:13`）。
- **在 DSH 下会坏的精确点**：`:19-23`（3/5 步是资源或工具）、`:31-36` checklist、`:43-46` 资源表、`:50-65` 工具示例、`:70-77` 完整示例——全部依赖不存在的工具/资源与 `{name}` 占位符。
- **判定**：**MIGRATE WITH ADAPTER**（受 MCP 阻塞）。
- **精确适配编辑**：
  1. `:19` `READ gitnexus://repos` → `list_mcp_resources({server:"gitnexus"})`；
  2. `:20,31` `READ gitnexus://repo/{name}/context` → `read_mcp_resource({server:"gitnexus", uri:"gitnexus://repo/AikSteinsGrimoire/context"})`；
  3. `:21,50,53,71` `gitnexus_query(` → `mcp__gitnexus__query(`；`:22,58,61,74` `gitnexus_context(` → `mcp__gitnexus__context(`；
  4. `:23` `READ gitnexus://repo/{name}/process/{name}` → 先 `list_mcp_resource_templates({server:"gitnexus"})` 再 `read_mcp_resource`；
  5. `:26` `npx gitnexus analyze` → `dsh-migration/bin/gitnexus analyze`；`{name}` 全部实名为 `AikSteinsGrimoire`。
- **建议 catalog description**：`需要先了解代码如何工作、理解架构、追踪执行流时使用：用 GitNexus 知识图谱按执行流探索（需 GitNexus MCP）。`

### 3.3 `gitnexus-impact-analysis`（97 行，9 处工具 + 2 处资源）

- **做什么**：改代码前的影响面分析：`impact(upstream)` → `processes` 资源 → `detect_changes()`，含 d=1/2/3 风险分级与 LOW→CRITICAL 判定表（`SKILL.md:17-54`），以及两个工具的输入输出示例（`:56-84`）。
- **价值**：**最高**。它是 `AGENTS.md:10,18` 强制条款的落地手册；`detect_changes` 把 git diff 映射到受影响符号/执行流，是 DSH 自身没有的能力。
- **在 DSH 下会坏的精确点**：`:20`（impact）、`:21`（processes 资源）、`:22`（detect_changes）是工作流主干；`:31,35` checklist；`:60-84` 工具示例；`:89-97` 示例；`:26` CLI 前缀。
- **判定**：**MIGRATE WITH ADAPTER**（受 MCP 阻塞；若将来启用 MCP，**本技能应第一个迁**）。
- **精确适配编辑**：`:20,31,58,61,76,89` → `mcp__gitnexus__impact(...)`；`:22,35,79` → `mcp__gitnexus__detect_changes(...)`；`:21,93` `READ processes` → `read_mcp_resource({server:"gitnexus", uri:"gitnexus://repo/AikSteinsGrimoire/processes"})`；`:26` 命令前缀；`{name}` → `AikSteinsGrimoire`。
- **建议 catalog description**：`修改符号前评估影响面时使用：用 GitNexus 查 blast radius（d=1/2/3）、受影响执行流与提交前 diff 影响（需 GitNexus MCP）。`

### 3.4 `gitnexus-debugging`（89 行，14 处工具 + 2 处资源）

- **做什么**：从报错/症状出发的调试流程：`query` 找相关执行流 → `context` 看 callers/callees → `process` 资源追执行链 → `cypher` 自定义路径查询（`SKILL.md:18-23`），附症状→方法映射表与 Cypher 示例（`:41-73`）。
- **价值**：中高；但其中 60% 与影响分析/探索重叠（`query`/`context` 用法相同），独有价值主要是 `cypher` 与"症状→工具"映射表（`:41-47`）。
- **在 DSH 下会坏的精确点**：`:19-22` 工作流、`:31-35` checklist、`:43,51,54,59,62,68` 工具段、`:71-73` Cypher、`:78-88` 示例——全部依赖 MCP。
- **判定**：**MIGRATE WITH ADAPTER**（受 MCP 阻塞）。若想压缩技能数量，可与 `gitnexus-exploring` 合并为"探索与调试"，但需重写 checklist。
- **精确适配编辑**：`:19,31,43,51,54,78` → `mcp__gitnexus__query(...)`；`:20,33,59,62,82` → `mcp__gitnexus__context(...)`；`:22,35,68` → `mcp__gitnexus__cypher(...)`；`:21,85` `READ process/{name}` → 模板列举 + `read_mcp_resource`；`:25` 命令前缀。
- **建议 catalog description**：`排查 bug、追踪报错来源时使用：用 GitNexus 从症状定位执行流与可疑符号，必要时用 Cypher 追调用链（需 GitNexus MCP）。`

### 3.5 `gitnexus-refactoring`（121 行，24 处工具 + 0 资源）

- **做什么**：重命名/抽模块/拆分三类重构的 checklist，核心是 `rename`（图编辑改法 + ast_search 改法、dry_run 先预览）与改后 `detect_changes` 验范围（`SKILL.md:27-60,62-95`），附风险规则表（`:97-104`）。
- **价值**：高。`rename` 基于调用图、带 confidence 分级，是"禁止 find-and-replace"（`AGENTS.md:20`）的唯一安全实现；DSH 没有等价内置能力。
- **在 DSH 下会坏的精确点**：工具密度最高——`:19-21,32,34,35,42,43,46,53,55,58,64,67,73,76,81,84,90,101,103,109,115,118` 全部是 `gitnexus_*` 调用或提及；`:25` CLI 前缀。注意 `:90-95` 的 Cypher 依赖 schema 资源（`guide:54`），也需替换。
- **判定**：**MIGRATE WITH ADAPTER**（受 MCP 阻塞）。
- **精确适配编辑**：`:32,34,64,67,101,109,115` → `mcp__gitnexus__rename(...)`；`:19,43,55,73,76` → `mcp__gitnexus__impact(...)`；`:35,46,58,81,84,118` → `mcp__gitnexus__detect_changes(...)`；`:42,53` → `mcp__gitnexus__context(...)`；`gitnexus_query`（`:103` 提及）→ `mcp__gitnexus__query`；`:90` `**gitnexus_cypher**` → `**mcp__gitnexus__cypher**`；`:25` 命令前缀。
- **建议 catalog description**：`重命名、抽模块、拆分服务等重构时使用：用 GitNexus 图改法做多文件安全重命名，并用 detect_changes 验证影响范围（需 GitNexus MCP）。`

### 3.6 `gitnexus-guide`（64 行，0 处工具调用 + 8 处资源）

- **做什么**：GitNexus 自身的速查：技能路由表（`:20-29`）、7 个工具一览（`:31-41`）、6 个资源一览（`:43-54`）、图 schema（`:56-63`）。
- **价值**：**在 DSH 下接近零**。理由：
  1. GitNexus MCP 服务器**自带 instructions**（`dist/mcp/resources.js:495-510` 构造工具/资源清单，并以 server instructions 形式暴露），而 DSH 会把服务器指令**作为字面文本并入系统提示词**（`dsh-mcp-client/README.zh.md:12`）→ 本技能是同一信息的第三方副本，天然会漂移。
  2. 技能路由表（`:22-29`）与 `AGENTS.md:36-41`、以及 DSH 自身的技能目录重复三份。
  3. schema 已可由 `gitnexus://repo/{name}/schema` 资源按需读取（`resources.js:51`），无需固化正文。
  4. MCP 未启用时它没有任何可执行内容（只有 `:18` 一句 CLI）。
- **判定**：**SKIP**。若将来有人坚持要"DSH 内自描述"，正确做法是把 schema/资源清单做成**按需读取的 reference 文件**，而不是技能正文。
- **（不建议的 catalog description，仅供记录）**：`GitNexus 工具/资源/schema 速查。`

---

## 4. 关键问题 3：其他"技能类"资产清单

> 任务描述里把 `aik-skills-lab/agent.md`、`QUICK-START.md`、`SKILL-LINK-GUIDE.md` 当作"文档非技能"——这个判断对这三个文件成立，但 `aik-skills-lab/` 下**还有 11 个真正带 frontmatter 的技能**，必须单独列出。

| 路径 | 实际类型 | 是否是"可复用技能" | 判定 | 依据 |
|---|---|---|---|---|
| `aik-skills-lab/{aIk-coding-style,ccgui-plugin-creator,code-implementation,code-review,component-extract-rewrite,devops,handoff-bundle,requirement-engineering,skill-tester,system-design,testing}/SKILL.md`（11 个） | 完整技能目录包（多数含 `references/`、`assets/`） | **是** | **不属于本报告范围**（已由并行迁移任务处理；`dsh-migration/skills.manifest` 已登记 10 个，`ccgui-plugin-creator` 明确不迁） | `dsh-migration/skills.manifest:16-25`、`:12-13`；provider 实测 8 个已链接到 `.agents/skills/` 且目录已实时出现 |
| `aik-skills-lab/agent.md` | 人设/系统提示词（`aIk-DevAgent`），无 frontmatter | 否 | **SKIP** | 形如 agent preset，不是 skill；provider 会告警 `missing YAML frontmatter`（实测） |
| `aik-skills-lab/QUICK-START.md`、`SKILL-LINK-GUIDE.md`、`README.md` | 文档 | 否 | **SKIP** | 无 frontmatter（实测 provider 逐条告警） |
| `aik-skills-lab/batch-pack-skills.ps1` | 打包脚本 | 否 | **SKIP** | 工具脚本，无技能语义 |
| `.qoder/specs/决赛PPT内容方案_task-f26.md` | 一次性任务方案 | 否 | **SKIP** | 单次交付物，无复用接口 |
| `.ccgui/project-map/AikSteinsGrimoire-27e22b8d/`（12 个 JSON）+ `.ccgui/prompts/`（空目录） | 项目地图生成缓存 | 否 | **SKIP** | 生成物缓存；`.gitignore:50` 已忽略 `.ccgui/` |
| `.agent-teams/attachment-overhaul/team.json`、`.agent-teams/retired-members.json` | AgentTeam 运行时状态 | 否 | **SKIP** | 运行时状态；`.gitignore:57` 已忽略 |
| `handoff_bundle/`（`HANDOFF.md`、`ASSET_MANIFEST.json`、`checksums.sha256`、`assets/`、`*.docx` 等） | handoff-bundle 技能的**产物实例** | 否 | **SKIP** | 技能在 `aik-skills-lab/handoff-bundle/`；这里是某次交接的输出 |
| `grimoire-agent/agent/{aIk-agent.md,decision-log.md,README.md}`、`grimoire-agent/archive/*`、`evolution/protocol.md` | 项目 agent 人设/日志/流程文档 | 否 | **SKIP** | 无 SKILL.md、无 frontmatter；属 agent 设定与项目文档 |
| `AGENTS.md` / `CLAUDE.md`（内容完全相同） | 工作区指令（GitNexus 生成块 + 项目约定） | 否 | **SKIP（但需处置，见 R5）** | DSH 直接把 `AGENTS.md` 作为 workspace instructions 注入（本会话 system 提示实测含 `Instructions from: AGENTS.md`）；`.gitignore:53-54` 已忽略 |
| `.codex/config.toml`、`.qoder-backup/mcp.json`、`.qoder/settings.local.json` | MCP/权限接线配置 | 否 | **SKIP** | 见 §1.4；且 `.qoder-backup/mcp.json:7` 含凭据 `x-mcp-key`，不应复制扩散 |
| `dsh-migration/tests/fixtures/root/{bad-name,good-skill,link-good}/SKILL.md` | 迁移工具链测试夹具 | 否 | **SKIP** | 仅供 `list-skills.mjs` 自测，不是产品技能 |
| `.claude/skills/gitnexus/*/SKILL.md`（6 个） | 本报告主体 | 是（GitNexus 生成） | 见 §3 | — |

补充事实：安装包 `1.6.4` 的 `skills/` 里还有 **7 个平铺 `.md`**（`gitnexus-*.md`），其中 `gitnexus-pr-review.md` 仓库缺失；6 个共有技能与仓库版本 **逐字节相同**（`cmp` 实测 6/6 IDENTICAL）。平铺 `.md` 恰好是 DSH 支持的第二种形态（`dsh-skill-filesystem/README.zh.md:36`），可作为将来迁移的来源之一。

---

## 5. 关键问题 4：迁移机械——"照抄目录"为什么不够

| 障碍 | 事实 | 后果 |
|---|---|---|
| `.claude/skills` 不在扫描根 | 根只有 `.dsh/skills`、`.agents/skills`、custom、`~/.dsh/skills`、`~/.agents/skills`（`README.zh.md:50-54`） | 必须复制/符号链接到其中一个根 |
| 不支持嵌套 `**/SKILL.md` | `README.zh.md:36` | **不能**把 `.claude/skills/gitnexus` 整个链到 `.agents/skills/gitnexus`（会变成 `.agents/skills/gitnexus/<name>/SKILL.md` 两层，扫描不到）；必须逐个到顶层 |
| 名称必须 kebab-case | 正则 `dsh-skill/lib/index.js:17`；非法则丢弃并告警（`dsh-skill-filesystem/lib/index.js:686-707`） | 6 个 GitNexus 技能名均合法（`gitnexus-cli` 等）；但同批的 `aIk-coding-style` 原名含大写，已在本会话 17:56 被并行任务改成 `aik-coding-style`（现 provider 实测 OK） |
| 源目录被 git 忽略 | `.gitignore:10` 忽略 `.claude/` | 新克隆的仓库里没有这 6 个技能（要等 GitNexus 重新生成），**迁移物本身应可版本化**→ 建议自持副本而非链到 `.claude/` |
| 源目录可再生 | `.gitnexus/meta.json:4` `indexedAt` 与 6 个 SKILL.md 的 mtime 同为 2026-05-19 09:44（索引时生成）；`gitnexus-cli/SKILL.md:18` 明确 analyze 会生成 AGENTS.md/CLAUDE.md | 直接链 `.claude/` 会在下次 `analyze` 时被覆盖（含你的 `mcp__` 适配），必须持有副本 |
| 符号链接跨平台 | 仓库在 `/mnt/d`（Windows 盘），Windows 侧 Git 需 `core.symlinks=true` 才能保留 | 团队若在 Windows 用 Git 检出任一链接，可能变普通文件/丢失；安装脚本已有 `skip - not a symlink` 保护（`install.sh:94-97`） |

**实测对照（证明上述判断，也证明"格式不是问题"）：**

| 作为 root 传入 | 结果 |
|---|---|
| `.claude/skills/gitnexus`（显式 root） | 6/6 技能 `status: OK`，0 warnings —— 说明**只要放对根，这 6 个技能今天就能被 DSH 发现并加载** |
| `.agents/skills`（默认 project-agents 根） | 实时发现并行任务安装的 8 个 aik-skills-lab 技能，`status: OK`，0 warnings —— 说明**符号链接目录 + 顶层 bundle 形态在真实 DSH 中有效** |
| 本会话技能目录 | 迁移前后：内置技能 → 出现 `handoff-bundle`（17:50 首个链接生效；其间还闪现过 `dsh-probe` 探测技能又消失）→ 17:56 批量 8 个 aik-skills-lab 技能出现；**全程始终没有 gitnexus-*** —— 说明 `.claude/skills` 确实不被扫描 |

> 注意：`customSkillDirs` 理论上可以指向 `.claude/skills/gitnexus` 从而"零拷贝"发现这 6 个技能（rank 300，`README.zh.md:52`）。**不建议**采用：① 要改 `~/.dsh` 配置（profile 级、非仓库级，换机器/换 profile 即失效）；② 源目录仍是 gitignored + 可被 `analyze` 重新生成的产物，适配会被覆盖；③ 与仓库既有的 `dsh-migration/install.sh` 单一安装入口相冲突。

---

## 6. 一个"适配后的 GitNexus 技能"到底要改什么

### 6.1 通用改造清单（8 项）

1. **落位**：逐个技能放到某个 DSH 根的**顶层**：`<root>/gitnexus-cli/`（目录包）或 `<root>/gitnexus-cli.md`（平铺）。不要保留 `gitnexus/` 中间层。
2. **工具名**：7 个裸名加前缀 → `mcp__gitnexus__{query,context,impact,rename,detect_changes,cypher,list_repos}`（映射见 §2.2）。可一条 sed 完成（绝大多数命中都是 `gitnexus_X(` 形态）：
   ```bash
   sed -i -E 's/\bgitnexus_(query|context|impact|rename|detect_changes|cypher|list_repos)\b/mcp__gitnexus__\1/g' SKILL.md
   ```
   逐文件命中行：cli 0 处；exploring `:21,22,32,34,50,53,58,61,71,74`；impact `:20,22,31,35,58,61,76,79,89`；debugging `:19,20,22,31,33,35,43,51,54,59,62,68,78,82`；refactoring 24 处（见 §3.5）；guide 仅 `:40` 一处 `cypher` 提及（但 guide 判定 SKIP）。
3. **资源读取**：所有 `READ gitnexus://…` 改写成 `list_mcp_resources` / `list_mcp_resource_templates` + `read_mcp_resource({server:"gitnexus", uri:"…"})`（§2.2 表）。
4. **`{name}` 具名**：替换为 `AikSteinsGrimoire`（或先 `list_mcp_resources({server:"gitnexus"})` 动态发现）。
5. **CLI 前缀**：`npx gitnexus X` → `dsh-migration/bin/gitnexus X`（包装脚本见 §6.2），因为本环境 `npx gitnexus` 实测失败。
6. **删除宿主专属表述**：`Claude Code`（cli `:26,82`）、`Restart Claude Code`、`CLAUDE.md` 相关（cli `:18` 可改为"生成 AGENTS.md"）、"PostToolUse hook"（当前仓库无 hook，本身不成立）。
7. **frontmatter/描述**：`name` 保持 kebab-case；`description` 建议中文化并加"需 GitNexus MCP"门槛词，便于目录路由与"工具不可用时不要调用"的决策。6 个现有 description 170–223 字符，未超 500 上限。
8. **前置能力声明**：在正文开头加一句"若 `mcp__gitnexus__*` 不在工具列表中，先告知用户 GitNexus MCP 未启用，改用 grep/read 手工普查并显式标注偏离"——这条把"技能不可执行"从幻觉变成可观测的降级路径（也正好对齐 AGENTS.md 的 MUST 条款，见 R5）。

### 6.2 建议的 CLI 包装脚本（解决 npx 失败 + 路径漂移）

```bash
#!/usr/bin/env bash
# dsh-migration/bin/gitnexus
# 本环境实测：npx gitnexus 因 npm cache 只读/无有效全局 bin 而失败；
# 用户级安装 ~/.local/share/gitnexus/node_modules/gitnexus 可用。
exec node "${GITNEXUS_CLI:-$HOME/.local/share/gitnexus/node_modules/gitnexus/dist/cli/index.js}" "$@"
```

### 6.3 建议的落地形态（供 Lead 决策，需改 `dsh-migration/`，超出本审计写权限）

- **不要**把 `.claude/skills/gitnexus/*` 直接链进 `.agents/skills`（源头 gitignored + 可再生 + 会被 analyze 覆盖）。
- **建议**：在 `dsh-migration/skills/gitnexus/` 下建立 5 个**自持适配副本**（cli / exploring / impact-analysis / debugging / refactoring），以 `gitnexus@1.6.4` 为基线（仓库现有 6 个与其**逐字节相同**，便于对账），在 `skills.manifest` 增加 5 行（`set` 建议 `extra`，因为强依赖 MCP）。
- `gitnexus-guide` 不迁；`gitnexus-pr-review` 若要用，同样需先过 MCP 门槛（可作为 `extra`）。
- 与现有 `install.sh` 的语义兼容：manifest 的源目录可指向 `dsh-migration/skills/...`，脚本本身无需改（`install.sh:86-113` 只是按 `<name> <src> <set>` 建链接）。

### 6.4 启用 GitNexus MCP 的配置模板（**未执行**；需改 `~/.dsh`，属用户级操作）

`~/.dsh/profiles/web/cordis.patch.yml` 追加（字段语义见 `dsh-mcp-client/README.zh.md:34-67`；`serverName` 约束 `:58`）：

```yaml
- id: mcp-gitnexus
  name: '@deepseek-ai/dsh-mcp-client'
  config:
    serverName: gitnexus
    transport: stdio
    command: /home/arrowinknee/.nvm/versions/node/v24.19.0/bin/node
    args:
      - /home/arrowinknee/.local/share/gitnexus/node_modules/gitnexus/dist/cli/index.js
      - mcp
    cwd: /mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire
    toolCallTimeoutMs: 60000
```

启用前必须先补索引注册（否则工具在但仓库为空）：

```bash
# 仅注册现有 .gitnexus/，不重新分析（CLI help: index = "Register an existing .gitnexus/ folder"）
dsh-migration/bin/gitnexus index /mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire
# 若拒绝跨平台读取旧的 lbug（Windows 侧生成），再退回全量重建（索引已 stale，本来也该重跑）
dsh-migration/bin/gitnexus analyze
dsh-migration/bin/gitnexus list        # 期望出现 AikSteinsGrimoire
```

验收方式：新会话里应出现 **7 个 `mcp__gitnexus__*` 工具** + 3 个资源工具（`list_mcp_resources` / `list_mcp_resource_templates` / `read_mcp_resource`），且 `read_mcp_resource({server:"gitnexus", uri:"gitnexus://repo/AikSteinsGrimoire/context"})` 返回 stats + 新鲜度。

---

## 7. 风险登记

| ID | 风险 | 触发条件 | 证据 | 缓解 |
|---|---|---|---|---|
| R1 | **"合规幻觉"**：迁入后模型看到 "MUST run `gitnexus_impact`"，但工具不存在，可能编造影响面结论 | 只迁技能、不启 MCP | §1.5；`AGENTS.md:10-21` | 本轮不迁；或迁入时加"工具缺失则降级并显式标注"条款（§6.1 第 8 条） |
| R2 | **索引不可用**：WSL 无 registry + 索引 stale | 启用 MCP 后未 `gitnexus index`/`analyze` | `~/.gitnexus` 不存在；Windows registry `:51-56`；`gitnexus status`=stale（`0fbcf5e` vs `3b1c577`） | 启用流程串 `index → list → 读 context 资源` 三步 |
| R3 | **跨平台索引**：`.gitnexus/lbug` 由 Windows 侧 GitNexus 写入，WSL 侧 1.6.4 读取未经证实 | 直接 `gitnexus index` 后调用工具 | `.gitnexus/meta.json:2,4`（`D:\...`、Windows 时间）；lbug 55MB 二进制 | 先用 `gitnexus context/query` 冒烟；失败则 `analyze` 重建（`.gitnexus` 已 gitignored，重建无版本风险） |
| R4 | **可再生源被覆盖**：`analyze` 会重生成技能/AGENTS.md，抹掉适配 | 链 `.claude/` 或直接改它 | `gitnexus-cli/SKILL.md:18`；mtime 与 indexedAt 一致 | 自持副本（§6.3） |
| R5 | **AGENTS.md 冲突**：DSH 注入的 `AGENTS.md` 要求 MUST 用不存在的工具；该文件被 `.gitignore` 忽略且会由 GitNexus 重新生成 | 每次 DSH 会话 | 本会话 system 提示含 `Instructions from: AGENTS.md`；`AGENTS.md:10-21`；`.gitignore:53` | 二选一：① 启用 MCP；② 把该块降级为"若存在 GitNexus MCP 工具则…，否则用 grep/手工并标注偏离"，并接受 GitNexus 重生成会覆盖（要么不再用 GitNexus 生成，要么保留后处理步骤） |
| R6 | **凭据扩散**：`.qoder-backup/mcp.json:7` 含 `x-mcp-key` | 把 Qoder MCP 配置复制进 DSH | 同文件 | 不要把该 http server 纳入 DSH；如必须，用 `!!js process.env.X` 引用（`dsh-mcp-client/README.zh.md:43`） |
| R7 | **Windows 符号链接检出**：`.agents/skills/*` 链在 Windows 侧 Git 可能丢失 | 交叉检出 | `install.sh:94-97`；仓库在 `/mnt/d` | 迁移物用副本；安装脚本保持"非链接则跳过"保护 |

---

## 8. 建议的行动顺序

- **P0（今天，MCP 未启用）**
  1. `gitnexus-*` 保持 0 迁入（与 `dsh-migration/skills.manifest:11-15` 的排除声明一致，本报告即其"see report"）。
  2. 处置 R5：`AGENTS.md` 的 MUST 块要么加可用性条件，要么接受"未启用期间的显式降级"约定；避免 DSH 会话被无法执行的强制条款牵着走。
  3. 修 `.codex/config.toml` 的失效路径或标注弃用（`E:\software\nvm\nodejs\gitnexus.cmd` 不存在）——不属本报告写权限，仅登记。
- **P1（决定启用 GitNexus MCP 时）**
  1. 按 §6.4 加 profile 条目（用户级，需 Lead/用户执行）。
  2. `index`（或 `analyze`）→ `list` → 读 `context` 资源冒烟（R2/R3）。
  3. 按 §6 适配 **5 个**技能（impact-analysis 优先），以自持副本形式落 `dsh-migration/skills/gitnexus/` + manifest `extra` 行。
  4. 用 `node dsh-migration/tools/list-skills.mjs .agents/skills` 验收；新会话确认 7 个 `mcp__gitnexus__*` 出现。
- **P2（可选）**
  - 评估 `gitnexus-pr-review`（1.6.4 自带、仓库缺失）。
  - 若维护成本高，考虑把 cli/exploring/debugging 合并成一个 `gitnexus` 技能（refactoring 与 impact-analysis 保持独立，因其 checklist 长且强约束）。

---

## 9. 本次审计可复现的关键命令

```bash
# 结构合法性（用 DSH 真实 provider 实现，只读）
node dsh-migration/tools/list-skills.mjs .claude/skills/gitnexus   # → 6/6 OK, descriptions 170–223
node dsh-migration/tools/list-skills.mjs .agents/skills            # → 已链接的 aik-skills-lab 技能 OK

# CLI 能力（npx 在本沙箱失败，直接 node 调用成功）
node ~/.local/share/gitnexus/node_modules/gitnexus/dist/cli/index.js --help
node ~/.local/share/gitnexus/node_modules/gitnexus/dist/cli/index.js status   # → stale (0fbcf5e vs 3b1c577)
node ~/.local/share/gitnexus/node_modules/gitnexus/dist/cli/index.js list     # → No indexed repositories found
node ~/.local/share/gitnexus/node_modules/gitnexus/dist/cli/index.js query attachment  # → 同上（registry 缺失）

# MCP 工具/资源的真实名字
grep -n "name: '" ~/.local/share/gitnexus/node_modules/gitnexus/dist/mcp/tools.js
grep -n "uriTemplate\|uri: 'gitnexus" ~/.local/share/gitnexus/node_modules/gitnexus/dist/mcp/resources.js

# DSH 侧：无 MCP 配置 / 无 gitnexus
grep -in 'mcp\|gitnexus' ~/.dsh/profiles/web/cordis.patch.yml   # → 无命中
grep -rn 'gitnexus' ~/.dsh/profiles/web/{cordis.yml,package.json,hub-settings.json}  # → 无命中

# 包内技能 vs 仓库技能
cmp .claude/skills/gitnexus/gitnexus-cli/SKILL.md ~/.local/share/gitnexus/node_modules/gitnexus/skills/gitnexus-cli.md  # → IDENTICAL
```

---

## 10. 证据附录（关键实测输出摘录）

1. **6 技能结构合法**（`list-skills.mjs .claude/skills/gitnexus`）：`"name": "gitnexus-cli", "status": "OK", "descriptionChars": 223`；debugging `170`；exploring `223`；guide `222`；impact-analysis `200`；refactoring `202`。`"failures": [], "warnings": []`。
2. **`.agents/skills` 实时发现**：该目录 17:50 起出现 `handoff-bundle -> ../../aik-skills-lab/handoff-bundle`，17:56 扩到 8 个链接；会话技能目录同步出现同名技能（`handoff-bundle` 及其后 8 项），全程无 `gitnexus-*` → 证明"`project-agents` 根 + 顶层符号链接"有效，而 `.claude/skills` 不被扫描。
3. **DSH 无 MCP**：本会话工具集无 `mcp__*`、无 `read_mcp_resource`/`list_mcp_resources`；`dsh-base` 只统一挂载资源包（`dsh-base/cordis.patch.yml:492-493`），无 server 时不暴露工具（`dsh-mcp-resources/README.zh.md:34`）。
4. **GitNexus CLI 在本环境**：`--help` 输出 20 个子命令（含 `mcp`）；`status` 报 `Status: ⚠️ stale (re-run gitnexus analyze)`；`list` 报 `No indexed repositories found`；`query attachment` 同样因 registry 缺失失败。
5. **Codex 接线失效**：`.codex/config.toml:2` 的 `E:\software\nvm\nodejs\gitnexus.cmd` 不存在；`/mnt/e/software/nvm/nodejs`→`v24.19.0`，其 `node_modules` 仅 `corepack/npm/yarn`；`v22.15.1` 等版本亦无 gitnexus。
6. **索引元数据**：`.gitnexus/meta.json`：`repoPath: D:\JeBrainsWorkSpace\AikSteinsGrimoire`、`lastCommit: 0fbcf5e…`、`indexedAt: 2026-05-19T01:44:45.846Z`、`files:107 / nodes:991 / edges:2187 / communities:49 / processes:67 / embeddings:0`；`capabilities.vectorSearch.status: unavailable`（`exact-scan`，`VECTOR disabled on this platform`）→ **不要建议使用语义检索/向量检索**，`--embeddings` 在本平台没有收益。
7. **包版本与技能基线**：`~/.local/share/gitnexus/node_modules/gitnexus/package.json:2-3` = `gitnexus 1.6.4`；其 `skills/` 与仓库 6 个技能 `cmp` 6/6 IDENTICAL；额外存在 `gitnexus-pr-review.md`（5429 B）。

---

### 附：本报告使用的判定口径

- **MIGRATE AS-IS**：改目录落位即可用，正文无需改动。**本次无一项适用**（资源/工具写法在 DSH 全部不成立）。
- **MIGRATE WITH ADAPTER**：结构合法，但需按 §6 改工具名/资源调用/命令前缀/host 专属表述；"受 MCP 阻塞"= 改动可提前做，但**启用后才能产生价值**，本轮不执行。
- **SKIP**：迁入收益 ≤ 维护成本或与现有机制冗余（本次为 `gitnexus-guide`）。

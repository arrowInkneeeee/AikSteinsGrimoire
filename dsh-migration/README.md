# DSH skill migration（aik-skills-lab → DeepSeek Harness）

把工作区里沉淀的 Java/Spring Boot 全生命周期技能库（`aik-skills-lab/`）接入 DSH 的
skill 系统，让 DSH 会话能像 Lingma/Qoder/Claude 一样自动发现、按需加载这些技能。

## 结论（详见 `report/MIGRATION-ASSESSMENT.md`）

| 资产 | 判定 | 当前状态 |
|---|---|---|
| `aik-skills-lab/` 的 8 个核心技能 | ✅ 值得迁移 | **已装到用户级 `~/.dsh/skills` 并通过验证** |
| `aik-skills-lab/` 的 2 个可选技能 | ✅ 已装 | 已通过 `--set all` 安装（用户级） |
| `ccgui-plugin-creator` | ❌ 不迁 | CC GUI 产品线专用，与 DSH 无关 |
| `.claude/skills/gitnexus/*`（6 个） | ⏸ 暂缓 | 依赖 GitNexus MCP，DSH 当前未接；见报告 |

按用户决定：**只保留用户级安装，项目级 `.agents/skills` 已删除**。用户级安装让所有
DSH 会话（不分项目、不分 profile）都能看到这 10 个技能。

## 已安装内容

用户级技能根：`~/.dsh/skills/`（DSH 的 `user-dsh` 根，rank 400）。
每个条目都是指向源库的**绝对符号链接**，技能本体仍只有一份：

```
~/.dsh/skills/
├── aIk-coding-style          -> <repo>/aik-skills-lab/aIk-coding-style      (catalog name: aik-coding-style)
├── requirement-engineering   -> <repo>/aik-skills-lab/requirement-engineering
├── system-design             -> <repo>/aik-skills-lab/system-design
├── code-implementation       -> <repo>/aik-skills-lab/code-implementation
├── code-review               -> <repo>/aik-skills-lab/code-review
├── testing                   -> <repo>/aik-skills-lab/testing
├── devops                    -> <repo>/aik-skills-lab/devops
├── handoff-bundle            -> <repo>/aik-skills-lab/handoff-bundle
├── component-extract-rewrite -> <repo>/aik-skills-lab/component-extract-rewrite
└── skill-tester              -> <repo>/aik-skills-lab/skill-tester   (disable-model-invocation)
```

安装后 DSH 会话目录会多出这 10 条技能（其中 `skill-tester` 为维护态、仅用户菜单可见），可直接用
`/code-review` 显式调用，或由模型按 description 自动加载。技能正文里的
`references/`、`assets/` 作为 L3 资源按需读取，不占常驻上下文。

> 为什么链接名是 `aIk-coding-style` 而 catalog 里是 `aik-coding-style`：DSH 只校验
> frontmatter 的 `name`，目录名仅作资源基址；保留原目录名可让技能正文里 41 处
> `../aIk-coding-style/...` 相对链接继续有效（否则要全库改链接）。

## 快速开始

```bash
# 用户级（默认，已执行；重复执行是幂等的）
./dsh-migration/install.sh

# 校验：用 DSH 自带的 skill provider 真实跑一遍发现 + 加载
./dsh-migration/install.sh --verify

# 只装核心（默认）或连可选技能一起装
./dsh-migration/install.sh --set all

# 项目级（只对本仓库会话生效，一般不需要）
./dsh-migration/install.sh --project

# 卸载（只删本脚本创建的链接，不动用户自己的技能）
./dsh-migration/install.sh --uninstall            # 用户级
./dsh-migration/install.sh --project --uninstall  # 项目级
```

生效方式：新开 DSH 会话会重新扫描技能根；同一会话在技能根变化被 watcher 或
`write`/`edit` 工具感知后会自动刷新 catalog。安装完想立刻确认，用 `--verify`（读真实
provider，不受 catalog 缓存影响）。

## 目录说明

| 路径 | 作用 |
|---|---|
| `skills.manifest` | 迁移清单：链接名、源目录、核心/可选、DSH catalog 名 |
| `install.sh` | 安装/卸载/校验；默认用户级，`--project` 切项目级 |
| `tools/list-skills.mjs` | 用 DSH 安装包里的真实 provider 验证发现与加载 |
| `tools/apply-audit-fixes.mjs` | 已执行的一次性源码修正（description/规范矛盾/写入门禁等） |
| `report/MIGRATION-ASSESSMENT.md` | 迁移评估与决策报告（先看这个） |
| `report/skills-aik-audit.md` | aik-skills-lab 11 个技能的逐项审计 |
| `report/skills-gitnexus-audit.md` | GitNexus 6 技能与散落资产审计（含 MCP 接通步骤） |
| `tests/fixtures/` | 验证脚本的自测夹具 |

## 源码适配与修正（已应用，用户批准）

### 1. 硬性适配（DSH 能否发现）

```diff
- name: aIk-coding-style
+ name: aik-coding-style   # DSH 的 skill 名必须是严格 kebab-case；大写会被静默丢弃
```

### 2. 审计建议的修正（`tools/apply-audit-fixes.mjs`，15 个文件）

- **description 全部重写**（10 个技能）：DSH 路由只看 name+description，原 `trigger:`
  触发词无效；重写后每个 description 都写明"做什么 + 何时加载 + 输入→输出"。
  8 个已装技能常驻字符从 1073 降到 **934**（约 0.5k tokens/会话）。
- **3 处会产出错误代码的规范矛盾**：表名/字段名统一为小写下划线（本仓库实际是
  `aik_knowledge` 等）；DTO/VO 注解改为"按 `references/01` 选 Swagger `@ApiModel` 或
  SpringDoc `@Schema`（本仓库 `@Schema`）"；`@Transactional` 统一到 aIk 事务门禁
  （默认不加、多表写入先确认）。顺带修正 SQL 目录、`Result`→`ApiResponse` 的表述。
- **落盘确认门禁**：8 个会写文件的技能正文加了一句"先列将改文件→用户确认→再写"。
- **component-extract-rewrite 重适配**：定位链从 PowerShell/Everything/自动写用户环境
  变量，改为"当前工作区 → 只读 `AIK_GRIMOIRE_HOME` → 询问用户"，GitNexus 前置降为可选。
- **skill-tester**：加 `disable-model-invocation: true`（维护型技能不进模型 catalog），
  测试目标清单更新为 11 个。

## 刻意不迁移的内容

- `ccgui-plugin-creator`：CC GUI 桌面端的插件 SDK，不是 DSH 插件体系；迁入必漂移。
- `.claude/skills/gitnexus/*`：正文依赖 `gitnexus_*` 工具与 `gitnexus://` 资源，
  DSH web profile 目前没有 MCP server；且仓库索引已 stale、WSL 侧未注册。
  接通步骤见 `report/skills-gitnexus-audit.md`（加 `dsh-mcp-client` 配置 → 在 WSL 重建索引 → 改技能工具名/前缀 → 后再迁）。
- `aik-skills-lab/agent.md`、`README/QUICK-START/SKILL-LINK-GUIDE` 等：文档/人设，不是技能。
- 根 `handoff_bundle/`：`handoff-bundle` 技能的一次真实产出实例，不是技能本身。

## 注意与限制

1. **用户级安装影响所有 DSH 会话**：8 个技能 description 常驻约 934 字符（约 0.5k
   tokens/会话）。如果只在 Java 项目里想用，可改用 `--project` 装到具体仓库。
2. **链接指向本仓库的绝对路径**：仓库移动/改名后需重新执行 `install.sh`（脚本按自身位置
   计算源路径，重跑即可修好链接）。
3. **catalog 常驻成本与路由**：审计建议的 7 技能子集为 824 字符；`devops` 是第 8 个，
   不需要可 `--uninstall` 后手动重装 `--set core` 前从 manifest 移除。
4. **不要改 `aIk-coding-style` 目录名**：改则需同步替换全库 41 处相对链接。

## 回滚

```bash
./dsh-migration/install.sh --uninstall            # 用户级卸载
./dsh-migration/install.sh --project --uninstall  # 项目级卸载（若有）
```

源码修正已写入 `aik-skills-lab/`：每处改动都在 `tools/apply-audit-fixes.mjs` 里有
old/new 对照（可反向执行）；技能名适配在 `aIk-coding-style/SKILL.md` 第 2 行。
当前工作树因 WSL/Windows 行尾差异本就整体显示 modified，回退源码建议按脚本条目逐项处理。

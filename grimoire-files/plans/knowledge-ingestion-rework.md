# 知识摄取与魔典定位链路 · 终极改造方案

> 版本：v1.4 | 更新：2026-11-11 | 状态：生效中（P0/P1/P2 已执行；P3 核心已实现） | 权威范围：`component-extract-rewrite` 技能的多源知识摄取与"物理机魔典目录"定位链路；`skill-tester` 的改造决策；`aik-skills-lab` 迁移遗留问题的统一修复清单
>
> 关联权威与依据：`grimoire-files/design-docs/knowledge-module/design.md`（知识模型）、`sql/aik_knowledge_tables.sql`（DDL）、`grimoire-files/design-docs/system-module/api-contract.md`（接口）、`aik-skills-lab/`（技能唯一源）、`dsh-migration/report/MIGRATION-ASSESSMENT.md` 与两份技能审计。
>
> 方案已获批准并执行 P0/P1；P2 进行中。执行范围：技能文件与文档；不改 `knowledge` 模块代码；数据库默认只生成 SQL 草案（不执行）。
>
> v1.3 → v1.4：P3 核心实现——`scripts/knowledge-import.mjs`（draft / verify / apply-dry / apply+rollback、DB 自动探测、行数上限与审计）；已完成无副作用冒烟测试（commit 后立即回滚，库内行数归零）；实现选型改为 Node CLI（DSH 自带，跨平台）+ mysql 客户端，Java JDBC 留作后续可选。
>
> v1.2 → v1.3：P2 完成——skill-tester 归档旧报告/补 DSH RED 方法/场景批注；C1 薄壳保留批注（TODO C1）；C2 陈旧页脚；C3 `Result`→`ApiResponse`；C4 find 括号；C6 安装文档；grimoire README §2/§3/§4 登记。C7（frontmatter 三键清理）留 P3。
>
> v1.1 → v1.2：用户批准执行并开工；记录 P0/P1 已完成（定位器、定位协议、源适配器、落盘/入库参考与 dry-run 验证、全部技能用户级安装）与 P2 进行中；确认 C1 五个薄壳 reference 本轮**保留批注**、下轮迭代再合并/删除（TODO C1）。
>
> v1.0 → v1.1：吸收 2026-11-11 用户决策——明确"虚拟机/WSL = 本机 WSL2、物理机 = 本机 Windows、两者同一份文件"，删除主线中的"拷贝桥"（降级为离机兜底）；补充环境检测矩阵；确认技能名按 DSH 规范、`aIk` 标识不动；全部技能用户级安装；外部知识与 `learning-notes` 分流；网页知识"总结归纳 + 出处"；`ApiResponse` 统一；入库"默认 SQL + 可选自动执行 DB + 未来 AI 自主护栏"；报告落点定为 `plans/ingest-reports/`；补充"Qoder 整库 junction vs DSH per-skill symlink"的单副本说明。

## 0. 背景与目标

### 0.1 当前位置与已确认事实

- DSH 迁移已完成：8 个核心技能以**符号链接**装在用户级 `~/.dsh/skills`（每条指向 `aik-skills-lab/<技能>`，内容零副本）；`component-extract-rewrite` 未安装，`skill-tester` 未安装（已设 `disable-model-invocation`），`ccgui-plugin-creator` 不迁。
- **术语约定（用户明确，长期有效）**：
  - "虚拟机 / WSL" = 当前电脑上的 **WSL2（Ubuntu 26.04）**，不是独立 VMware/VirtualBox 虚拟机。
  - "物理机" = 本机 **Windows 11**（`D:\...` 等路径），除非特指。
  - 两者是**同一份物理文件**：仓库真身在 `D:\JeBrainsWorkSpace\AikSteinsGrimoire`，WSL 通过 `/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire`（9p/drvfs）直接读写；不存在两份内容，不需要同步。
- 用户已确认：`component-extract-rewrite` 需要，但必须重造"检索链路"；本质是**从不同项目 / 任意工作空间 / 网站萃取有效知识，最终落到魔典**。`skill-tester` 若保留则需同步原始技能改造。

### 0.2 目标（完成后达到的状态）

1. `component-extract-rewrite` 成为**多源知识摄入入口**：源 = 本地外部项目 / 任意工作空间文档 / 网站页面 / 用户粘贴文本；目标 = 本机魔典。
2. 定位链路**先检测环境、再选路**：WSL2 / Windows 原生 / 独立 VM / 容器分别走对应策略；只读、可验证；不再依赖 PowerShell / Everything / 自动写环境变量。
3. 摄取产出落到现有知识模型：Markdown 文件落到正确目录 + `aik_knowledge` 元数据（默认 SQL 草案，可选自动入库）+ 索引更新 + 可校验清单。
4. `skill-tester` 变为**维护态回归工具**，场景与当前 11 个技能及修正后规范一致。
5. `aik-skills-lab` 遗留问题一次性收口；所有修复在**原始技能**上完成，DSH 符号链接自动生效，无需重装。

### 0.3 非目标

- 不改 `aIk` 品牌/标识（见 §1.3）。
- 不新增知识类型、不改 `aik_knowledge` DDL（P0–P2；P3 自动入库也不改表）。
- 不把网站全文大段搬进仓库（默认"总结归纳 + 出处"）。
- 不在"讨论/分析"阶段执行任何写操作。

## 1. 约束（铁律）

### 1.1 环境与术语（已确认）

| 概念 | 定义 | 路径 |
|---|---|---|
| 虚拟机 / WSL | 本机 WSL2，Ubuntu 26.04 | `/home/arrowinknee`，仓库经 `/mnt/d/...` 访问 |
| 物理机 | 本机 Windows 11 | `D:\JeBrainsWorkSpace\AikSteinsGrimoire` |
| 两者关系 | **同一份物理文件**（drvfs 直通），非副本 | 改一处，另一侧立即可见 |

**结论：当前不需要"VM ↔ 物理机拷贝"，也不需要同步脚本。** 拷贝桥仅作为未来"离机/独立 VM/无共享文件系统"时的兜底（§4.9）。

### 1.2 单副本原则（技能分发）

技能只有一份真身：`aik-skills-lab/`（在仓库里）。各宿主通过**文件系统链接**引用它，绝不复制：

| 宿主 | 链接形态 | 目标 |
|---|---|---|
| Qoder | 整库 Junction（`C:\Users\arrowInknee\.qoder\skills`） | `aik-skills-lab/` 根 |
| QoderCN | 整库 SymbolicLink | `aik-skills-lab/` 根 |
| DSH（WSL，当前） | **per-skill symlink**（`~/.dsh/skills/<技能>`） | 每个 `aik-skills-lab/<技能>` |
| DSH（Windows 原生，如使用） | per-skill Junction（`mklink /J`） | 每个 `aik-skills-lab/<技能>` |

- DSH 用 per-skill 而不是整库链接：精选技能（排除 ccgui）、统计更省、避免把 README 等文档当候选技能告警。
- 因此**不存在"两份技能"，也没有同步问题**。复制只在宿主无法建链接时兜底，且必须带校验（P3 才考虑）。
- 维护动作：仓库路径变化 → 重建链接（`./dsh-migration/install.sh` 幂等）；两边不要同时执行 git 写操作；仓库缺 `.gitattributes` 导致的行尾噪声属独立问题（建议另开一次统一）。

### 1.3 `aIk` 固定标识与技能名

- `aIk`、`aIk-coding-style` 目录、`AikSteinsGrimoire`、`AIK_GRIMOIRE_HOME`、`@author a I k .`、`aIk-agent` 一律不改；不重命名目录、不批量替换。
- 技能名按 DSH 规范：`aIk-coding-style/SKILL.md` 的 `name:` 为 `aik-coding-style`（DSH 强制的 kebab-case 字段，当前已生效；不视为 aIk 标识变更）。
- 目录名默认不改；若未来坚持目录也改，需同批更新全库 41 处相对链接（单独立项，不在本方案内）。

### 1.4 其他约束

1. **单一来源**：技能只改 `aik-skills-lab/`；DSH/IDE 用链接共享。
2. **写操作门禁**：分析/讨论只读；落盘前列出将创建/修改的文件与摘要并等确认；流程结束给变更清单。
3. **知识模型权威**：包结构/表结构以 `knowledge-module/design.md` 为准，DDL/代码优先于文档。
4. **DSH 写策略**：会话为 workspace-write 时只能写会话工作区。落盘魔典推荐**以魔典为 workspace 开 DSH 会话**；在外部项目会话执行时必须在写之前停止并提示（或显式审批升权），禁止静默失败。
5. **文档规范**：遵守 `grimoire-files/README.md` §5（文档头四行、变更历史追加、不产生带版本号的新文件名、不保留平行副本）。

## 2. 现状诊断（证据）

| # | 问题 | 证据 | 影响 |
|---|---|---|---|
| D1 | 定位链依赖 PowerShell / Everything，并会**自动持久写用户环境变量** | `aik-skills-lab/component-extract-rewrite/references/grimoire-location-chain.md:5-29`、`:75-80` | WSL/DSH 下 Step 4 必失败；写用户环境变量是破坏性副作用 |
| D2 | 定位失败时把产物写当前工作区 `doc/_grimoire_pending/` | 同文件 `:70-73` | 会把"没找到魔典"伪装成成功，产物散落 |
| D3 | 仅支持"源项目路径 + 组件范围"，不支持外部项目/任意工作空间/网站 | `SKILL.md:36-40`、`:64-67`、`:138-142` | 与"多源知识摄入"不符 |
| D4 | SKILL.md 路由表与真实目录不一致 | `SKILL.md:160-167`：NOTE→`scrolls/`、COMPONENT→`components/{名}/`、SOLUTION→`solutions/{名}/`、CODE→`components/{名}/code/`；实际见 §5.4 | 产物落错位置 |
| D5 | `resource_path` 三处约定不一致 | `design.md:250-300`（包路径）；`solutions/README.md:20-22`（`solutions/{名}/`）；`_import_temp.sql`（文件相对路径） | 元数据不可对账 |
| D6 | 入库链路硬编码 | `ImportKnowledge.java:7` 写死 `D:\...` + 固定 20 条 item；`_import_temp.sql` 生成物 | 换机即坏、无幂等/无 manifest/无校验 |
| D7 | 无"网站/非代码文档"入口；NOTE 与 `learning-notes/` 关系未定义 | `SKILL.md` 全文；`grimoire-scrolls/README.md` 与 `grimoire-files/learning-notes/` 并存 | 网站知识无处可去或落错层 |
| D8 | `source_project`/`source_path`/`ext_json` 未被写入 | `KnowledgePo`/DDL 有字段；导入脚本不写来源 | 无法追溯来源 |
| D9 | 脱敏只覆盖代码字段 | `references/desensitization-patterns.md` | 网页正文可能带入 token/隐私/未授权全文 |
| D10 | `skill-tester` 报告过期且与当前规则冲突 | `skill-tester/test-results/comprehensive-report.md`（2026-05-13、5/42、旧技能名；`:156` 断言大写表名） | 维护者按错误结论改技能 |
| D11 | `skill-tester` RED 对照在 DSH 单会话不可行；场景夹具未同步本轮修正 | `SKILL.md:170-186`；5 个 `references/*-scenarios.md` | 测试结论不可信 |
| D12 | 跨技能遗留：薄壳 reference、陈旧页脚、`Result`/`ApiResponse`、`find` 缺括号、Windows 安装文档 | 见 §8 | 维护噪音，误导生成 |
| D13 | "虚拟机/物理机需要拷贝同步"的误解 | 已澄清：同一份文件 | 本方案明确不复制、不写同步脚本（§1.2/§4.8） |

## 3. 目标架构（六层）

```
┌────────────────────────────────────────────────────────────────────────────┐
│ E0 Env Detect   环境检测：WSL2 / Windows 原生 / 独立 VM / 容器 → 选路       │
│ L0 Locator      魔典定位（只读、可验证、跨环境路径映射）                    │
│ L1 Sources      外部项目 / 工作空间文档 / 网站 / 粘贴文本（适配器）          │
│ L2 Classifier   NOTE / COMPONENT / SOLUTION / CODE（判定树 + 歧义即问）      │
│ L3 Processor    规范化 · 代码提取 · 脱敏 · 去重 · 溯源                      │
│ L4 Landing      文件路由 + 索引更新 + ingest manifest                       │
│ L5 Metadata     入库（默认 SQL 草案，可选自动执行 DB）+ 校验                │
└────────────────────────────────────────────────────────────────────────────┘
```

- **E0 必须在 L0 之前**：不同环境的可达路径不同（WSL 的 `/mnt/d`、Windows 的 `D:\`、独立 VM 的共享目录、容器 volume），策略不同。
- **L2–L4 在"以魔典为 workspace"的会话执行**（§1.4 约束 4）。
- **L5 分两档**：默认产出 SQL 草案；显式开启才自动执行（§5.6）。

## 4. 检索链路设计（E0 + L0，本方案核心）

### 4.1 环境检测（先检测、再选路）

| 检测信号（只读） | 判定 | 魔典可达性 / 路径形态 |
|---|---|---|
| `/proc/version` 含 `microsoft`、`WSL_DISTRO_NAME` 存在、`wslpath` 可执行 | WSL2（当前环境） | ✅ 经 `/mnt/<盘>/...` 直接读写物理机磁盘 |
| `systemd-detect-virt` 输出 vmware/virtualbox/kvm；`/mnt/hgfs`、`/media/sf_*` 存在 | 独立 VM（有共享目录） | ✅ 共享目录直写；路径形如 `/mnt/hgfs/...` |
| 同上但无共享挂载、且无 `/mnt/d` 等 Windows 盘 | 独立 VM（无桥） | ❌ 走拷贝桥（§4.9，未来场景） |
| 存在 `powershell.exe`、路径 `C:\`/`D:\`、无 Linux 内核 | Windows 原生 | ✅ 直接 `D:\...` |
| `/.dockerenv` 或 cgroup 含 docker/containerd | 容器 | 取决于 volume mount，按挂载点判断 |

检测结果写入定位输出与摄取报告（`envType` + `strategy`），便于复盘。

### 4.2 魔典身份验证（is_grimoire）

必须同时满足（只读）：

| 检查 | 内容 |
|---|---|
| M1 | `<root>/grimoire-files/README.md` 存在 |
| M2 | `<root>/grimoire-scrolls/README.md` 存在 |
| M3 | `<root>/src/main/java/io/aik/steins/grimoire/knowledge/` 存在 |
| M4 | `<root>/pom.xml` 含 `<name>AikSteinsGrimoire</name>` 或 `<artifactId>aik-steins-grimoire</artifactId>` |
| M5 | `git -C <root> remote get-url origin` 命中 `AikSteinsGrimoire`（大小写不敏感） |

- 额外输出 `head`（`git rev-parse --short HEAD`）与 `remote` 供对账；陈旧只告警不阻断。

### 4.3 定位顺序（只读，命中即止）

```
E0  environment = detect()                    # §4.1
P0  cwd 及其祖先：is_grimoire(cwd)            # 已在魔典里
P1  machine anchor（按宿主候选）：
      ~/.aik/grimoire-anchor.json
      %USERPROFILE%\.aik\grimoire-anchor.json
      $DSH_HOME/aik/grimoire-anchor.json
    读取 per-host paths，逐个 is_grimoire 验证
P2  环境变量 AIK_GRIMOIRE_HOME（只读）
P3  anchor 中的跨宿主路径，用 wslpath -u/-w 转换后验证
P4  有界扫描（只读、限时 10s）：
      WSL：/mnt/d/JeBrainsWorkSpace、$HOME/Workspace 等
      Windows：D:\JeBrainsWorkSpace、C:\Users\*\... 等
      只认目录名含 AikSteinsGrimoire 或 git remote 命中的候选
P5  仍无 → 停止，列出已尝试路径，询问用户真实目录
```

- P1–P4 全部只读；P5 用户给出路径并确认后，才可**可选地**写 anchor（一次性、显式 `--remember`、经写入门禁确认）。
- 禁止：自动写 `AIK_GRIMOIRE_HOME`、自动 `git pull`、失败时写当前工作区、猜测后直接落盘。

### 4.4 machine anchor（书签，不是链接也不是拷贝）

**是什么**：一个几行 JSON 的小配置文件，记录"本机上魔典在哪"，让技能从任意 cwd 调用时不用每次翻磁盘。类比 IDE 记住项目路径；**只存路径与身份信息，不含凭据，不复制内容**。

推荐位置（默认）：`~/.aik/grimoire-anchor.json`（WSL/Linux）与 `%USERPROFILE%\.aik\grimoire-anchor.json`（Windows）；`$DSH_HOME/aik/grimoire-anchor.json` 作为 DSH 专用备份（可选）。

```json
{
  "repo": "AikSteinsGrimoire",
  "remote": "git@github.com:arrowInkneeeee/AikSteinsGrimoire.git",
  "paths": {
    "wsl": "/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire",
    "windows": "D:\\JeBrainsWorkSpace\\AikSteinsGrimoire"
  },
  "fingerprint": { "pomName": "AikSteinsGrimoire", "head": "<写入时短 commit>" },
  "writtenAt": "2026-11-11T10:00:00+08:00",
  "writtenBy": "AI-assisted (user approved)"
}
```

- anchor 只是缓存提示，每次仍逐项 `is_grimoire` 验证；失效即忽略并走 P2–P5。
- 写入必须用户明确同意（一次）；缺失也能工作。

### 4.5 多副本 / 冲突处理

- 多个通过验证的候选：优先 anchor；否则列出全部候选（host、remote、HEAD）让用户选；不自动选"最近的"。
- 同一路径的 Windows/WSL 两种表示按当前宿主选择，报告注明另一表示。
- HEAD 落后只告警，不自动同步、不阻断落盘。

### 4.6 输出契约与失败契约

成功：`{repo, host, envType, strategy, path, remote, head, verifiedBy, confidence}`。
失败：`{status:"NEED_PATH", attempted:[...], hints:[...]}` 并**停止流程**，不得写任何文件。

### 4.7 DSH 写策略门禁（落盘前）

```
if grimoire.path 不在 session workspace 内:
    停止 → 提示二选一：
      A) 以魔典为 workspace 重新打开 DSH 会话（推荐）
      B) 对本次落盘显式授予越界写权限（审批），报告记录
```

### 4.8 技能分发形态对照（澄清"两份/同步"）

| 宿主 | 当前形态 | 内容副本 | 同步 |
|---|---|---|---|
| Qoder / QoderCN | 整库 Junction/SymbolicLink → `aik-skills-lab` | 0（同一份） | 不需要 |
| DSH（WSL） | `~/.dsh/skills` 下 per-skill symlink（当前 8 条） | 0（同一份） | 不需要 |
| DSH（Windows 原生，如启用） | `C:\Users\<用户>\.dsh\skills` 下 per-skill Junction | 0（同一份） | 不需要 |
| 无法建链接的宿主 | 复制 | 1 份/宿主 | 需要校验/同步脚本（兜底，非本方案主线） |

- Windows 原生 DSH 的创建方式（需管理员或开发者模式）：`mklink /J "C:\Users\arrowInknee\.dsh\skills\code-review" "D:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab\code-review"`。
- 注意：`C:\Users\arrowInknee\.dsh\skills` 现在只有一个 `ccgui-plugin-creator` 的真实副本（CC GUI 工具管理，非本库），不要把它当成本库技能。
- 仓库路径移动后：重跑 `./dsh-migration/install.sh`；Windows 侧 junction 同步重建。

### 4.9 拷贝桥（仅未来离机/独立 VM 无共享时）

- 生成侧 `ingest --offline` → `knowledge-bundle/`：按目标相对路径的 Markdown + `manifest.json` + `checksums.sha256` + `merge.sql` + 说明。
- 传输：共享目录 / SMB / U 盘 / 网盘。
- 接收侧（魔典 workspace）：`merge <bundle>` 校验 checksums + is_grimoire + 去重，**只追加不覆盖**，生成入库 SQL，输出 merge 报告。
- 当前 WSL2 ↔ Windows 同机同盘，**不使用**此路径；仅保留设计。

### 4.10 工具实现

- `aik-skills-lab/component-extract-rewrite/scripts/locate-grimoire.sh`（POSIX sh，只读、`--json`、`--remember` 需显式确认）。
- 参考文档给出 Windows PowerShell 等价步骤（不依赖 Everything）：`Test-Path` 候选 → `git -C <candidate> remote get-url origin` → `pom.xml`/目录标记校验。
- P4 有界扫描超时即放弃，不做全盘搜索。

## 5. 多源知识摄取流水线（L1–L5）

### 5.1 源适配器

| source_type | 输入 | 采集工具（DSH） | 产物 | 限制 / 降级 |
|---|---|---|---|---|
| `local-repo` | 外部项目路径（可跨盘） | 文件工具、`bash`+`grep`、`git`（只读）；GitNexus 可选增强 | 代码清单、依赖、耦合点、脱敏点 | 无 GitNexus 用 grep/read；不写源项目 |
| `workspace-doc` | 任意工作空间的 md/docs/spec | 文件工具、`grep` | 文档结构、可复用方案/规范 | 只读；不移动源文件 |
| `web-page` | 单个或多个 URL | `web_fetch`（原文）+ `read_page`（JS 页面/摘要）；图片 modlens；PDF 按需 | **总结归纳**（核心结论/要点/适用场景/坑）+ 标题/URL/站点/抓取时间/许可说明 | 不抓登录/付费墙；默认不搬全文；失败即报错不猜 |
| `pasted-text` | 用户粘贴 | 无 | 原文 + 来源说明 | 需用户声明来源/许可 |

### 5.2 知识分类（L2）

```
是代码/可复用实现？
  ├─ 是，需在本仓库可编译 → COMPONENT（组件）或 SOLUTION（方案：问题→步骤→验证）
  └─ 否/只是片段 → CODE（片段，仅存 DB content）
是文档/文章/教程/规范/技巧总结？
  ├─ 来自外部（网站/外部项目）→ NOTE，落 grimoire-scrolls/
  └─ 仅当用户明确指定"这是我整理给 AI 学习的材料" → NOTE，落 grimoire-files/learning-notes/
无法判定 → 列出候选类型让用户选
```

**重要边界（用户已确认）**：
- 外部萃取出来的知识**都属于外部知识**（NOTE/COMPONENT/SOLUTION/CODE）。
- `grimoire-files/learning-notes/` **只放用户特殊整理、明确要 AI 学习的东西**；技能**不自动写**该目录，除非用户明确要求。
- 网页知识 = 对"学到的技巧/方法"的**总结归纳**，不是全文搬运。

### 5.3 规范化、去重与脱敏（L3）

- 标题/文件名：`kebab-case` ASCII slug；`aIk`/`AIK_*` 标识保持原样；冲突加 `-2` 并记录。
- 代码提取：按文件/类拆分，标注语言与依赖；禁止把源项目密钥、地址、业务术语带入。
- 脱敏（扩展 `desensitization-patterns.md`）：密码/token/cookie/私钥/连接串/内网地址/手机号/身份信息；疑似凭据命中即停止并报告，不"自动打码后继续"。
- 去重：文件路径冲突 + `aik_knowledge.uk_code` 冲突 + 标题/内容 SHA-256 比对；命中即 skip 或要求改 code，不覆盖已有知识。

### 5.4 落地路由（L4，按真实目录，取代 SKILL.md 旧表）

| type | 适用 | 文件落点 | `resource_path` 约定（建议） |
|---|---|---|---|
| NOTE(1) | 外部文章/教程/技巧总结 | `grimoire-scrolls/{slug}/{slug}.md` | `grimoire-scrolls/{slug}/` |
| NOTE(1) | 用户指定"给 AI 学习"的整理材料 | `grimoire-files/learning-notes/{slug}.md` | 同文件路径 |
| COMPONENT(2) | 组件手册 | `grimoire-files/component-manuals/{code}.md` | 同文件路径 |
| COMPONENT(2) | 可编译组件代码 | `src/main/java/io/aik/steins/grimoire/components/{code}/` | `io.aik.steins.grimoire.components.{code}`（写 `ext_json.codePath`） |
| SOLUTION(3) | 方案（问题/步骤/验证） | `src/main/java/io/aik/steins/grimoire/solutions/{code}/`（README/problem/steps/verification） | `io.aik.steins.grimoire.solutions.{code}` |
| CODE(4) | 代码片段 | 不落文件 | 空；`ext_json.language` + `content` |

- 索引更新：`grimoire-scrolls/README.md` 卷轴表、`solutions/README.md` 方案表（新增时）、组件模块 README；**不更新 `grimoire-files/README.md`**（它把 `component-manuals/`、`learning-notes/` 排除在系统设计文档之外）。
- 命名冲突按 `code`/slug 处理，manifest 记录。

### 5.5 ingest manifest（幂等与审计）

`assets/ingest-manifest.schema.json`（草案）：

```json
{
  "runId": "20261111-1000",
  "grimoire": { "path": "...", "head": "...", "remote": "..." },
  "sources": [{ "type": "web-page", "ref": "https://...", "fetchedAt": "..." }],
  "items": [{
    "type": "NOTE",
    "code": "spring-boot-config",
    "target": "grimoire-scrolls/spring-boot-config/spring-boot-config.md",
    "sha256": "...",
    "db": { "table": "aik_knowledge", "code": "spring-boot-config", "status": "pending|inserted|skipped" },
    "approval": { "landing": true, "dbImport": false }
  }]
}
```

- 重跑：目标文件存在且 `sha256` 一致 → skip；不一致 → 报"已有不同版本"，不覆盖。
- `--dry-run` 输出将写文件清单与入库 SQL，不落盘。

### 5.6 元数据入库（L5，两档模式）

**默认（P0）：生成 SQL 草案，人工执行**

- 产出 `ingest-reports/{runId}.sql`：`INSERT ... ON DUPLICATE KEY UPDATE`（按 `uk_code` 幂等）。
- 字段映射：`title`/`code`/`type`/`summary`/`content`/`source_project`/`source_path`/`resource_path`/`ext_json`/`category_id`/`status`。
- `ext_json`：`{sourceUrl, fetchedAt, license, codePath, tags, dependencies}`。
- 入库前必须用户确认（`design.md` §4 的"询问用户：是否入库"）。

**可选（P1/P2）：自动调用 DB（显式开启，非默认）**

- 模式：`db=draft`（默认）/ `db=apply`（显式）/ `db=verify`。
- 执行顺序：连接探活（`SELECT 1`）→ dry-run 打印影响行 → 用户/策略确认 → 单事务执行 → 行数/幂等校验 → 审计报告 → 失败整体回滚。
- 实现：优先**受控 Java CLI（JDBC，mysql-connector 已在 pom）**，WSL/Windows 均可跑；WSL 下 `mysql` CLI（`/usr/bin/mysql`）可作快速通道。
- 凭据：运行时从 `src/main/resources/application.yml`（本地 dev：`127.0.0.1:3306/aik_steins_grimoire`）读取，或 `DSH_DB_*` 环境变量覆盖；**不写入技能、不写入报告、不落 git**。
- 白名单：只允许 `aik_knowledge` 的 INSERT/UPDATE；禁止 DDL/DELETE/DROP；单次行数上限；生成"按 code 删除本次新增"的回滚 SQL。
- 未来 AI 自主（用户表示可能放权）：权限分级 **T0 只读 / T1 写文件需确认 / T2 写 DB 需确认 / T3 白名单内自主**；自主模式必须带 dry-run 历史、行数上限、审计、回滚、kill switch；凭据永不暴露给模型/报告。

### 5.7 校验与报告（L5）

- 文件：存在、可读、Markdown 链接可解析、`sha256` 与 manifest 一致。
- DB：按 `code` 逐条核对行数/字段；`resource_path` 可定位；重复运行不新增行。
- 安全：全量扫描 manifest 与落盘内容，确认无凭据/隐私残留。
- **报告落点（用户授权由本方案决定）**：`grimoire-files/plans/ingest-reports/{runId}.md`，SQL 草案同目录 `{runId}.sql`；不放进 `grimoire-files/storage/`（那是后台上传附件存储）。若不想入库 git，可退到 `~/.aik/ingest-reports/`（本机）。

## 6. `component-extract-rewrite` 技能改造明细

### 6.1 定位与名称

- 技能名保持 `component-extract-rewrite`；description 改为"多源知识摄取 + 组件萃取复写 + 魔典落盘"，明确"先定位魔典"。
- 摄取知识入库与组件复写是同一条流水线的两个出口。

### 6.2 子命令

| 子命令 | 用途 | 变化 |
|---|---|---|
| `locate` | 输出环境类型、魔典路径、身份验证、候选清单；零写入 | **新增** |
| `analyze` | 6 维代码分析（外部项目/工作空间） | 保留；输入源扩展 |
| `extract` | 10 章组件手册 | 保留；输出路径按 §5.4 |
| `rewrite` | 按手册脱敏复写标准化代码 | 保留；三层审查改为引用 `code-review` |
| `ingest` | 多源（repo/workspace/web/pasted）→ 分类 → 落盘 → manifest（可选入库） | **新增** |
| `full-pipeline` | locate → ingest/analyze → extract → rewrite → 落盘 → 校验 | 重写步骤 |

### 6.3 文件级改造清单

| 文件 | 动作 |
|---|---|
| `SKILL.md` | 重写：description、子命令、输入/输出契约、路由表（§5.4）、DSH 写策略门禁、人机协作节点 |
| `references/grimoire-location-chain.md` | 重写为定位协议（保留文件名减少链接变动；正文按 §4） |
| `references/knowledge-landing.md` | 新增：路由表、manifest、索引更新、入库 SQL 模板、校验 |
| `references/source-adapters.md` | 新增：四类源的工具矩阵与降级 |
| `references/safety-redaction.md` | 新增：网页/代码通用脱敏与版权策略 |
| `references/rewriter-delivery-spec.md` | 三层审查改为引用 `code-review` 的 quality/security |
| `scripts/locate-grimoire.sh` | 新增（只读/`--json`/`--remember` 带确认） |
| `assets/ingest-manifest.schema.json`、`assets/ingest-report-template.md` | 新增 |

### 6.4 人机协作与写入门禁

1. 环境/定位结果确认（多候选必须选）。
2. 源清单确认。
3. 类型判定歧义确认。
4. 落盘计划确认（文件清单 + 摘要）。
5. 入库确认（默认否）。
6. 结束：变更清单 + 校验报告。

### 6.5 安装范围

改造验证通过后，**全部可迁技能用户级安装**：`./dsh-migration/install.sh --set all`（补齐 `component-extract-rewrite` 与 `skill-tester`；其余 8 个已装无需重装）。`ccgui-plugin-creator` 不迁。`skill-tester` 保持 `disable-model-invocation`（仅用户菜单可见）。

## 7. `skill-tester` 改造决策与方案

### 7.1 必要性：需要，但只作维护态工具

- 技能会生成代码，且迁移中已暴露多处内容矛盾（表名大小写、注解、事务、路由表）；需要回归手段。
- 频率低 → 保持 `disable-model-invocation: true`（用户菜单可见，不占模型 catalog）。
- 维护对象是 `aik-skills-lab` 技能，不是业务代码。

### 7.2 改造清单

1. 归档旧报告：`test-results/comprehensive-report.md` → `test-results/archive/2026-05-13-comprehensive-report.md`，SKILL.md 注明过期。
2. **场景夹具与原始技能同步**：重写/校准 5 个 `references/*-scenarios.md` 的期望，与新规范一致（小写表名、`@Schema`/`@ApiModel` 二选一、`@Transactional` 门禁、SQL 目录、路由表）。规则：技能改一处，场景期望同批改。
3. DSH RED 基线方法：临时把技能链接移出技能根（或独立 profile）→ 新开会话跑基线 → 恢复 → 新会话跑 GREEN；写清步骤与风险。
4. 验收指标：RED 违规率 > 50%、GREEN 拦截率 ≥ 90%、误报 ≤ 10%（可按实际调整）。
5. 目标清单：与当前 11 个技能名一致。
6. 精简过期/重复场景，避免维护 2000+ 行过期夹具。

## 8. 跨技能统一修复清单（与原始技能同批）

> 原则：**先改 `aik-skills-lab/` 原始技能，再同步 skill-tester 场景与文档；DSH 符号链接自动生效，无需重装**。`aIk` 标识一律不动。

| # | 项目 | 文件 | 动作 | 优先级 |
|---|---|---|---|---|
| C1 | 5 个薄壳 reference | `system-design/references/api-design-guide.md`、`db-design-guide.md`；`code-review/references/style-guide.md`、`quality-guide.md`；`testing/references/unit-test-guide.md` | 合并进 SKILL 索引后删除（或补实内容）；去掉旧原子技能名 | P2 |
| C2 | 陈旧页脚 | `devops/assets/deploy-plan-template.md`、`ops-manual-template.md`、`requirement-engineering/assets/prd-template.md`、`testing/assets/test-plan-template.md` 等 | 旧技能名（`spec-*`）→ 当前技能名 | P2 |
| C3 | `Result` vs `ApiResponse` | `system-design/SKILL.md:63/68/275`、`code-implementation/SKILL.md:65/168/234`、`aIk-coding-style/SKILL.md` 响应封装说明 | **统一 `ApiResponse` 示例**（"优先复用项目现有统一响应类，本仓库为 `ApiResponse<T>`"） | P1 |
| C4 | `find` 缺括号 | `system-design/SKILL.md:33-44` | `find src \( -type d -name common -o -type d -name util \)` 或拆成两次 | P1 |
| C5 | 路由表过期 | `component-extract-rewrite/SKILL.md:160-167` | 用 §5.4 真实目录表替换 | P1 |
| C6 | 文档安装说明 | `aik-skills-lab/README.md`、`QUICK-START.md`、`SKILL-LINK-GUIDE.md`、`agent.md` | 更新为"用户级符号链接/junction"；删 Windows 专用路径 | P2 |
| C7 | frontmatter 三键 | 10 个可迁技能 | `type`/`version`/`trigger` 收进 `metadata`（纯整洁，缓做） | P3 |
| C8 | `skill-tester` 报告与场景 | 见 §7.2 | 归档 + 同步 | P1 |

## 9. 分阶段执行计划

> 批准后执行；每阶段结束提交一次变更并跑验收。

### P0 — 环境检测 + 定位链路 + 技能骨架

- 改：`component-extract-rewrite/SKILL.md`（子命令/契约/门禁）、`references/grimoire-location-chain.md`（定位协议 §4）、`scripts/locate-grimoire.sh`、`references/source-adapters.md`。
- 验证：WSL 魔典内 / WSL 外部目录 / 任意目录三种 cwd 跑 `locate`；失败路径不写文件、不写环境变量、不写 pending 目录。
- 门禁：只读验证通过才进入 P1。

### P1 — 摄取流水线 + 落盘 + SQL 草案

- 改：`references/knowledge-landing.md`、`assets/ingest-manifest.schema.json`、`assets/ingest-report-template.md`；SKILL.md 的 `ingest`/`full-pipeline`；§8 的 C3/C4/C5/C8。
- 验证：外部项目代码、网页、工作空间文档各跑 dry-run；再落盘到魔典（用户确认）；生成 manifest + SQL 草案（不执行）；校验链接/摘要。
- 安装：通过后 `./dsh-migration/install.sh --set all` 安装全部技能（用户级）。

### P2 — skill-tester + 跨技能修复 + 文档收口

- 改：§7 + §8 剩余；同步 skill-tester 场景；更新 `grimoire-files/README.md` §2/§4（登记本方案与 `ingest-reports/`）。
- **执行进展（2026-11-11）**：P0、P1、P2 已完成（见下"§9.1 已执行记录"）；P3（自动入库 + frontmatter 清理）可选待定。
- **C1 决策（用户）**：五个薄壳 reference 本轮**保留批注**，下一轮迭代再合并/删除；文件内已加 `TODO(C1)` 标记与说明。

### §9.1 已执行记录（2026-11-11）

| 阶段 | 交付 | 验证 |
|---|---|---|
| P0 | `scripts/locate-grimoire.sh`；重写 `references/grimoire-location-chain.md`；新增 `references/source-adapters.md`；`SKILL.md` v2.0.0（locate/ingest/analyze/extract/rewrite/full-pipeline） | 仓库内/仓库外/失败/anchor 四场景实测；`NEED_PATH` 零写入；无环境变量写入 |
| P1 | `references/knowledge-landing.md`；`assets/ingest-manifest.schema.json`；`assets/ingest-report-template.md`；`dsh-migration/tests/p1-dryrun/` 夹具 | dry-run：web-page NOTE + local-repo 冲突/跳过；manifest 字段/SHA/路由校验通过；未写魔典、未入库 |
| 安装 | `./dsh-migration/install.sh --set all` | `~/.dsh/skills` 10 条链接；真实 provider 10/10 OK；catalog 已出现 component-extract-rewrite |
| P2（已完成） | skill-tester 归档旧报告 + DSH RED 方法 + 场景批注；C1 批注（TODO C1）；C2 页脚；C3 ApiResponse；C4 find 括号；C6 安装文档；grimoire README 登记 | 11/11、10/10 provider 校验通过；live catalog 含 component；`TODO(C1)`×5 已就位 |
| P3（核心已完成） | `scripts/knowledge-import.mjs`：draft / verify / apply-dry / apply+rollback；DB 自动探测（WSL 默认网关 → Windows MySQL）；行数上限、审计、回滚 SQL | 只读 verify、事务 rollback、commit 冒烟测试通过（测试行已删除，`aik_knowledge` 未留痕）；真实知识入库待用户确认 |
- 验证：skill-tester RED/GREEN 跑通；11 个技能 frontmatter/发现/加载全绿；DSH catalog 正确；无 `aIk` 改名。

### P3（核心已完成）— 自动入库与 AI 自主分级

- 已实现 `scripts/knowledge-import.mjs`：`draft`（默认）/ `verify` / `apply-dry`（事务后回滚）/ `apply --yes`（提交 + 审计 + 回滚 SQL）；DB 自动探测；白名单/行数上限。
- 剩余：把 `db=apply` 接入 `ingest` 流程的真实落盘路径（等用户对首条知识落盘确认后联调）；T0–T3 权限分级配置留待放权时落地。

## 10. 验收标准

| # | 场景 | 通过判据 |
|---|---|---|
| A1 | WSL 魔典内会话 `locate` | 命中 P0；`envType=wsl2`；`repo/remote/head` 正确；零写入 |
| A2 | Windows 原生 DSH（如启用）`locate` | 命中 `D:\...`；零写入 |
| A3 | WSL 外部工作区 `locate` | 命中 anchor/环境变量/扫描；不写 anchor、不写文件 |
| A4 | 任意目录不可达 | 输出 NEED_PATH + attempted；流程停止，零写入 |
| A5 | 网页 `ingest`（dry-run） | NOTE 摘要 + 出处 + 目标 `grimoire-scrolls/{slug}/`；不搬全文 |
| A6 | 外部项目代码 `ingest`（dry-run） | 正确分类 + 目标路径 + manifest + SQL 草案 |
| A7 | 工作空间文档 `ingest` | 正确判定 NOTE（或指定 learning-notes）；索引更新 |
| A8 | 重复运行 | 文件/DB 不重复（sha256/code 命中即 skip） |
| A9 | 凭据样本 | 命中即停并报告，不落盘 |
| A10 | 落盘会话 = 外部项目 workspace | 写前停止，提示切换/审批；不静默失败 |
| A11 | DB `apply`（可选） | dry-run → 事务 → 行数/幂等校验；回滚 SQL 可用 |
| A12 | skill-tester | RED 基线违规被记录；GREEN 拦截率达标；无过期断言 |
| A13 | 全库回归 | 10 个可迁技能全部用户级安装并可加载；`aIk` 标识/目录未变 |
| A14 | 分发形态 | Qoder junction、DSH per-skill symlink 指向同一仓库；0 内容副本 |

## 11. 风险与回滚

| 风险 | 缓解 |
|---|---|
| 写魔典越权（DSH workspace-write） | 以魔典为 workspace；或显式审批；落盘前门禁（§4.7） |
| 定位到错误副本 | 5 项身份验证 + 多候选让用户选；anchor 仅提示 |
| anchor 失效/泄露 | 只存路径；权限 600；逐次验证；失效走 P2–P5 |
| DB 导入污染 | 默认不入库；事务 + `ON DUPLICATE KEY`；`--dry-run`；回滚 SQL；白名单 |
| 网页版权/隐私 | 默认摘要 + 出处；不抓付费墙；脱敏扫描；用户确认 |
| 同名/重复知识 | `uk_code` + 文件冲突 + sha256 去重，不覆盖 |
| 技能修改漂移 | 单一源 `aik-skills-lab/` + 场景同步 + DSH symlink |
| 仓库移动导致链接失效 | 重跑 `install.sh`；Windows junction 重建 |
| 行尾噪声（无 `.gitattributes`） | 独立问题，建议另开一次统一；不塞进本方案 |

**回滚**：技能改动 `git checkout -- aik-skills-lab/<file>`；DSH `./dsh-migration/install.sh --uninstall`；DB 按 `code` 删除；落盘文件删除 + manifest 记录。

## 12. 决策记录（2026-11-11 已确认）

| # | 决策 | 状态 |
|---|---|---|
| 1 | 术语：虚拟机/WSL=本机 WSL2；物理机=本机 Windows；同一份文件 | ✅ 已定 |
| 2 | 技能名按 DSH 规范 `aik-coding-style`；`aIk` 目录/标识不动 | ✅ 已定（目录默认不改） |
| 3 | 不复制技能、不写同步脚本；Qoder 整库 junction、DSH per-skill symlink；Windows 原生 DSH 用 junction | ✅ 已定 |
| 4 | 拷贝桥仅未来离机/独立 VM 兜底，当前不用 | ✅ 已定 |
| 5 | 全部可迁技能用户级安装；ccgui 不迁；skill-tester 仅用户菜单 | ✅ 已定 |
| 6 | 外部萃取 → NOTE/COMPONENT/SOLUTION/CODE；`learning-notes/` 仅用户特殊整理材料，技能不自动写 | ✅ 已定 |
| 7 | 网页知识 = 总结归纳 + 出处，不搬全文 | ✅ 已定 |
| 8 | 统一 `ApiResponse` 示例 | ✅ 已定 |
| 9 | 入库默认 SQL 草案 + 可选自动 DB 执行（含未来 AI 自主分级与护栏） | ✅ 已定 |
| 10 | 报告落点 `grimoire-files/plans/ingest-reports/{runId}.md` + `.sql` | ✅ 已定 |
| 11 | anchor 默认 `~/.aik/grimoire-anchor.json`（+ Windows 对应用户目录；DSH home 可选备份） | ✅ 已定（首次写入需用户批准） |
| 12 | 自动入库实现：Node CLI（`knowledge-import.mjs`，DSH 自带）+ mysql 客户端；Java JDBC 留作后续可选 | ✅ 已实现并冒烟测试（2026-11-11） |
| 13 | C1 五个薄壳 reference：本轮保留批注、下轮迭代再合并/删除（TODO C1，勿忘） | ✅ 已定（2026-11-11） |
| 14 | P2 按需开发；`skill-tester` 保留为维护态（`disable-model-invocation`） | ✅ 已定（2026-11-11） |

## 13. 附录

### 13.1 定位算法伪代码

```text
function locate():
  env = detect_environment()                  # wsl2 / windows / vm-shared / vm-isolated / container
  for d in ancestors(cwd): if is_grimoire(d): return OK(d, env, "cwd")
  for a in anchor_candidates():
    if exists(a):
      for p in parse(a).paths.values() + translate(p): if is_grimoire(p): return OK(p, env, "anchor")
  if is_grimoire(env AIK_GRIMOIRE_HOME): return OK(..., "env")
  hits = bounded_scan(roots_for(env), depth<=3, timeout=10s)
  if len(hits)==1: return OK(hits[0], env, "scan")
  if len(hits)>1: return ASK(hits)
  return NEED_PATH(attempted)

function is_grimoire(p):
  return exists(p/grimoire-files/README.md)
      && exists(p/grimoire-scrolls/README.md)
      && exists(p/src/main/java/io/aik/steins/grimoire/knowledge)
      && grep(p/pom.xml, "AikSteinsGrimoire|aik-steins-grimoire")
      && git_remote(p) contains "AikSteinsGrimoire"
```

### 13.2 落地目录树（新增/变更）

```
AikSteinsGrimoire/
├── grimoire-files/
│   ├── component-manuals/{code}.md            # COMPONENT 手册
│   ├── learning-notes/{slug}.md               # 仅用户指定"给 AI 学习"的整理材料
│   └── plans/ingest-reports/{runId}.md/.sql   # 摄取运行报告与 SQL 草案
├── grimoire-scrolls/{slug}/{slug}.md          # 外部文章/教程/技巧总结 NOTE
└── src/main/java/io/aik/steins/grimoire/
    ├── components/{code}/                     # 可编译组件
    └── solutions/{code}/                      # 方案
```

### 13.3 落地后需同步的文档（P2）

- `grimoire-files/README.md` §2 目录树（`plans/ingest-reports/`）与 §4 状态表（登记本方案）。
- `plans/execution-plan.md` 属附件域改造的仓库级执行计划，**不混入**本方案（避免制造第二个所有者）；全局索引由 `grimoire-files/README.md` §3 承担。
- `aik-skills-lab/README.md`、`QUICK-START.md`、`SKILL-LINK-GUIDE.md` 更新安装说明。

### 13.4 参考

- `dsh-migration/report/MIGRATION-ASSESSMENT.md`、`skills-aik-audit.md`、`skills-gitnexus-audit.md`
- `grimoire-files/design-docs/knowledge-module/design.md`、`sql/aik_knowledge_tables.sql`
- `aik-skills-lab/component-extract-rewrite/`、`aik-skills-lab/skill-tester/`
- `grimoire-files/README.md`（文档规范与权威边界）

## 附录：变更历史

| 版本 | 日期 | 变更内容 |
|---|---|---|
| v1.0 | 2026-11-11 | 初版：定位链路重造、多源知识摄取流水线、component 技能改造、skill-tester 决策、跨技能统一修复清单、分阶段执行与验收 |
| v1.1 | 2026-11-11 | 吸收用户决策：术语与同一份文件事实；单副本分发（Qoder junction / DSH per-skill symlink，Windows DSH 用 junction）；删除主线拷贝桥（降级为离机兜底）；新增环境检测矩阵；技能名按 DSH 规范、aIk 标识不动；全部技能用户级安装；外部知识与 learning-notes 分流；网页总结归纳；ApiResponse 统一；入库默认 SQL + 可选自动 DB + AI 自主护栏；报告落点 `plans/ingest-reports/`；anchor 说明与默认位置；决策记录表 |
| v1.2 | 2026-11-11 | 批准执行；记录 P0/P1 已执行与验证、全部技能安装；P2 进行中；C1 薄壳 reference 本轮保留批注、下轮迭代删除（TODO C1）；grimoire README §2/§3/§4 登记本方案 |
| v1.4 | 2026-11-11 | P3 核心：`knowledge-import.mjs`（draft/verify/apply-dry/apply+rollback、DB 自动探测、审计、行数上限）；实现选型 Node CLI；冒烟测试通过且库内无残留 |
| v1.3 | 2026-11-11 | P2 完成：skill-tester 归档旧报告 + DSH RED 方法 + 5 场景批注；C1 保留批注（TODO C1，下轮删除）；C2 页脚；C3 Result→ApiResponse；C4 find 括号；C6 安装文档；README/grimoire 登记收口；C7 留 P3 |

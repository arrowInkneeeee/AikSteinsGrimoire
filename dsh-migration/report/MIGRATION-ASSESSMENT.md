# 迁移评估：aik-skills-lab → DSH 技能系统

> 评估对象：工作区里沉淀的两套技能资产——`aik-skills-lab/`（11 个技能，Java/Spring Boot 全生命周期）
> 与 `.claude/skills/gitnexus/`（6 个，GitNexus 代码智能）
> 评估日期：2026-11-10　方式：逐技能审计（2 名 teammate 并行）+ DSH 真实 provider 实测 + 运行中的 Web 服务热加载验证
> 明细报告：[skills-aik-audit.md](skills-aik-audit.md)（566 行）、[skills-gitnexus-audit.md](skills-gitnexus-audit.md)（424 行）

## 0. 一句话结论

**有价值，但值得迁移的是"SDD 流程类"技能，不是全部。** 8 个核心技能已按用户要求装到
**用户级 `~/.dsh/skills`**（所有 DSH 会话可用），并用 DSH 真实 provider 验证 8/8 可发现、
可加载；`ccgui-plugin-creator` 跳过；GitNexus 6 个技能价值高但**当前不可用**（DSH 没接
GitNexus MCP），正确做法是先接通 MCP 再迁，而不是把"会调用不存在工具"的指令塞给模型。
用户同时批准了审计建议的源码修正，已全部应用。

## 1. 价值判断

| # | 资产 | 价值 | 判定 | 状态 |
|---|---|---|---|---|
| 1 | `aIk-coding-style` | 规则地基：本仓库 Java 代码风格的唯一来源，其余流程技能都引用它 | ADAPTER（1 行） | ✅ 已装（用户级） |
| 2 | `requirement-engineering` | 需求 → PRD，SDD 第 1 步 | AS-IS | ✅ 已装 |
| 3 | `system-design` | PRD → SDD（架构/库表/API/选型/评审） | ADAPTER（小） | ✅ 已装 |
| 4 | `code-implementation` | SDD → 骨架/业务/API 文档/迁移 SQL | ADAPTER | ✅ 已装 |
| 5 | `code-review` | 风格/质量/安全/缺陷四维审查 + 门禁 | ADAPTER（小） | ✅ 已装 |
| 6 | `testing` | 单测/集成/API 测试/覆盖率 | ADAPTER（小） | ✅ 已装 |
| 7 | `devops` | 配置/打包/部署/健康检查/日志/排障 | AS-IS | ✅ 已装（可选） |
| 8 | `handoff-bundle` | 跨会话/跨 Agent 可核验交接包，工具无关，有真实产出佐证 | AS-IS | ✅ 已装 |
| 9 | `component-extract-rewrite` | 组件萃取复写 + 多源知识摄取；原定位链绑定 Windows/PowerShell + 会持久写用户环境变量 | ADAPTER（重） | ✅ 已装（重适配 + 多源摄取） |
| 10 | `skill-tester` | 技能质量测试（RED-GREEN-REFACTOR）；维护型技能 | ADAPTER | ✅ 已装（维护态） |
| 11 | `ccgui-plugin-creator` | CC GUI 桌面端插件 SDK，与 DSH 插件体系无关 | **SKIP** | ❌ 不迁 |
| 12 | GitNexus 6 技能 | 代码图查询/影响面/重构，价值最高的代码智能能力 | **暂缓**（受 MCP 阻塞） | ❌ 未迁 |

价值密度：`aik-skills-lab` 11 个 `SKILL.md` 共 2384 行 / 约 88 KB，L3 资源约 1.3 万行；正文
只在技能被加载时进上下文，**常驻成本只有 description（9 个模型可见技能合计约 1084 字符，
约 0.5k tokens/会话）**，性价比高。

## 2. DSH 侧的关键事实（决定迁移形态）

来自 DSH 加载器源码与实测：

1. **技能根**：`<project>/.dsh/skills`（rank 100）、`<project>/.agents/skills`（200）、自定义目录（300）、`~/.dsh/skills`（400）、`~/.agents/skills`（500）。**不扫描 `.claude/skills`**。
2. **发现规则**：只认 `<root>/<name>/SKILL.md` 或 `<root>/<name>.md`，一层深；`name`/`description` 必填；`name` 必须匹配 `^[a-z0-9]+(?:-[a-z0-9]+)*$`，不合法会**静默丢弃**（只打日志）。
3. **`type`/`version`/`trigger` 全部无效**：未知键被忽略；DSH 路由**只看 description**。原技能库里 103 个触发词在 DSH 中不参与任何匹配。
4. **L3 资源不预加载**：加载技能只注入正文 + 资源基址，`references/`/`assets/` 由模型按需打开；正文里的相对路径按"相对技能基址"解析。
5. **description 上限 500 字符**，重写后本库最长 148，不会被截断。
6. **符号链接安装可行**：DSH 用 `realpath` 解析后 `stat`，链接目录会被识别为 directory；资源基址保留技能根下的链接路径。因此 symlink 安装既能"单一来源"，又能让 41 处 `../aIk-coding-style/...` 跨技能链接继续按基址解析。

## 3. 已执行的迁移（可回滚）

### 3.1 源库适配

```diff
# aik-skills-lab/aIk-coding-style/SKILL.md
- name: aIk-coding-style
+ name: aik-coding-style
```

原因：`aIk` 含大写，DSH 会静默丢弃该技能，而它是其余 5 个技能引用的规则源。
目录名保持 `aIk-coding-style`，所以全库 41 处相对链接零改动。

### 3.2 安装形态（按用户决定：只要用户级）

用户级技能根 `~/.dsh/skills/` 下 8 条绝对符号链接（脚本幂等，可重复执行）：

```
aIk-coding-style        -> <repo>/aik-skills-lab/aIk-coding-style      catalog: aik-coding-style
requirement-engineering -> <repo>/aik-skills-lab/requirement-engineering
system-design           -> <repo>/aik-skills-lab/system-design
code-implementation     -> <repo>/aik-skills-lab/code-implementation
code-review             -> <repo>/aik-skills-lab/code-review
testing                 -> <repo>/aik-skills-lab/testing
devops                  -> <repo>/aik-skills-lab/devops
handoff-bundle          -> <repo>/aik-skills-lab/handoff-bundle
```

项目级 `.agents/skills` 已按用户要求删除（`install.sh --uninstall`），安装脚本默认也改成
用户级；项目级保留为 `--project` 选项。

### 3.3 已应用的审计修正（用户批准"全部应用"）

`tools/apply-audit-fixes.mjs`（一次性、每处替换断言全局唯一，15 个文件）：

| 类别 | 内容 | 覆盖 |
|---|---|---|
| description 重写 | 10 个技能改为"做什么 + 何时加载 + 输入→输出"，适配 DSH 的 description-only 路由 | 10 个 `SKILL.md` |
| 规范矛盾 1 | 表名"大写下划线"→ 小写下划线（与 `references/16-naming.md`、本仓库 `aik_knowledge*` 一致） | system-design、README |
| 规范矛盾 2 | DTO/VO 注解从硬编码 `@ApiModel` → "按 references/01 选 Swagger/SpringDoc，本仓库 `@Schema`" | aIk、code-implementation、agent.md |
| 规范矛盾 3 | `@Transactional` 统一到 aIk 门禁（默认不加、多表写入先确认） | code-implementation |
| 相关一致性 | SQL 目录统一为"项目现有 SQL 目录（默认 `sql/`）"；`Result`→项目响应类 `ApiResponse`；主键统一 `IdType.INPUT` | code-implementation、system-design、aIk refs |
| 落盘确认门禁 | 8 个会写文件的技能正文加"先列将改文件→用户确认→再写→结束给变更清单" | 8 个 `SKILL.md` |
| component 重适配 | PowerShell/Everything/自动写用户环境变量的定位链 → "当前工作区 → 只读 `AIK_GRIMOIRE_HOME` → 询问用户"；GitNexus 前置降为可选；纯英文路径注 WSL 语义 | component-extract-rewrite |
| 维护卫生 | skill-tester 加 `disable-model-invocation: true`、测试清单 10→11 并更新名称；style-checklist 标注"以 aIk 规范为准" | skill-tester、code-review |

### 3.4 新增文件（全部在 `dsh-migration/`，未提交 git）

| 文件 | 作用 |
|---|---|
| `install.sh` | 安装/卸载/校验；默认用户级，`--project`、`--set all`、`--dry-run`、`--verify` |
| `skills.manifest` | 迁移清单（链接名/源目录/core-extra/catalog 名），单一配置入口 |
| `tools/list-skills.mjs` | 用 DSH 安装包内真实 `dsh-skill-filesystem` provider 验证发现+加载 |
| `tools/apply-audit-fixes.mjs` | 已执行的一次性源码修正脚本（含 old/new 对照，可反向追溯） |
| `README.md` | 迁移说明、快速开始、限制、回滚 |
| `report/*.md` | 本评估 + 两份审计 |

## 4. 验证记录（实测，不是"应该能用"）

1. **真实 provider 全量校验（用户级根）**：8/8 `OK`，`complete: true`，`missing: []`，`failures: []`，`warnings: 0`；description 合计 **934 字符**；每条都成功加载正文并解析出 `references/`、`assets/` 基址。
2. **当前 Web 服务加载实测**：`skill` 工具实际加载 `code-review`（经 `~/.dsh/skills` 链接），返回正文 + `Base directory: /home/arrowinknee/.dsh/skills/code-review`，且正文里能看到新加的"落盘确认门禁"——说明加载读的是修正后的源码。
3. **热刷新机制实测（项目级 root 上完成）**：创建技能根后，运行中的会话 `<available_skills>` 自动追加了 8 条；技能根内变更后 catalog 在下一步刷新。用户级安装新开会话即生效。
4. **11 个 bundle 全部 frontmatter 合法**（含未安装的 optional 技能）：以 `aik-skills-lab/` 为根跑 provider，10 个可迁移技能全部 `status: OK`，无 name/description 解析告警。

## 5. 未做 / 待决策

| 事项 | 现状 | 建议 |
|---|---|---|
| GitNexus 迁移 | 暂缓 | 前置：profile 加 `dsh-mcp-client` 配置 + WSL 侧重建索引（当前 `.gitnexus` 记录 `0fbcf5e`，HEAD `3b1c577`，WSL registry 缺失）+ 把技能里的 `gitnexus_*` 改为 `mcp__gitnexus__*` 裸工具名（`impact`/`query`/`context`/`rename`/`detect_changes`/`cypher`/`list_repos`） |
| optional 技能 | 已装（`--set all`） | `component-extract-rewrite` 已重适配；skill-tester 为"仅用户菜单可用" |
| 陈旧文档/资产 | 旧测试报告、spec-* 页脚、QUICK-START 的 Windows 安装说明等未清理 | 不影响 DSH 运行；需要时按审计 §13.3 清理 |
| `AGENTS.md` 的 GitNexus MUST 条款 | 仍会被 DSH 注入，但工具不存在 | 接通 MCP 或把该块改成条件式（审计 gitnexus 报告 R5） |
| `.codex/config.toml` GitNexus 接线 | 指向已不存在的 `E:\...\gitnexus.cmd` | Codex 侧独立问题，按需修复 |

## 6. 风险与回滚

- **风险 1（低）**：落盘确认门禁只是"指令级"，模型仍可能在长流程里连续写文件；DSH workspace-write 本身可回滚工作区，重要变更建议配合 git。
- **风险 2（低）**：用户级安装给所有会话增加约 0.5k tokens 常驻；只在 Java 项目用时改用 `--project`。
- **风险 3（低）**：仓库移动后 `~/.dsh/skills` 链接失效 → 重跑 `./dsh-migration/install.sh` 即可。
- **回滚**：`./dsh-migration/install.sh --uninstall` 删除 8 条链接；源码修正按 `tools/apply-audit-fixes.mjs` 的 old/new 反向处理，技能名适配在 `aIk-coding-style/SKILL.md` 第 2 行。

## 7. 结论

**值得迁移，且已完成。** 迁移成本低（1 行名称适配 + 8 个符号链接 + 安装脚本 + 一次性源码修正），
收益是 DSH 会话直接获得整套 Java SDD 能力，并与 Lingma/Qoder 共享同一份技能源、永不漂移。
GitNexus 那 6 个技能先不迁是对的——在 MCP 接通前迁入只会让模型调用不存在的工具；按
gitnexus 审计报告的三步接通后再迁即可。

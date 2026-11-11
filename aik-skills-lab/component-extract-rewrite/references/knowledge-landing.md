# 知识落盘与入库（knowledge landing）

> 本文件是 `component-extract-rewrite` 的 L3 参考：摄取/萃取完成后**如何落到魔典**——文件放哪、索引怎么更新、元数据怎么入库、怎么校验与回滚。
> 权威方案：`grimoire-files/plans/knowledge-ingestion-rework.md` §5.4–§5.7；
> 知识模型权威：`grimoire-files/design-docs/knowledge-module/design.md` 与 `sql/aik_knowledge_tables.sql`（代码/DDL 优先于文档）。
> 前置条件：已通过 `references/grimoire-location-chain.md` 定位到 `$GRIMOIRE`，且当前会话已获得写魔典的权限。

## 0. 铁律

1. **落盘前确认**：先列出将创建/修改的文件清单与摘要，用户确认后才写。
2. **只写魔典**：不写源项目/源目录；不覆盖已有不同版本的知识（同 `code` 冲突必须停下）。
3. **单一来源**：文件落在魔典仓库；`aik_knowledge` 只是元数据；`aIk` 标识保持原样。
4. **默认不入库**：文件落盘与数据库入库是两件事；入库默认只生成 SQL 草案，显式确认后才执行。
5. **可追溯**：每条知识都有来源（`source_project` / `source_path` / `ext_json.sourceUrl`）和校验（SHA-256）。

## 1. 知识类型与落点（真实目录）

| type | 适用 | 文件落点 | `resource_path` 约定 |
|---|---|---|---|
| NOTE(1) | 外部文章/教程/技巧总结 | `grimoire-scrolls/{slug}/{slug}.md` | `grimoire-scrolls/{slug}/` |
| NOTE(1) | 用户明确指定"给 AI 学习"的整理材料 | `grimoire-files/learning-notes/{slug}.md` | 同文件路径 |
| COMPONENT(2) | 组件知识手册 | `grimoire-files/component-manuals/{code}.md` | 同文件路径 |
| COMPONENT(2) | 可编译组件代码 | `src/main/java/io/aik/steins/grimoire/components/{code}/` | `io.aik.steins.grimoire.components.{code}`（另写 `ext_json.codePath`） |
| SOLUTION(3) | 方案（问题/步骤/验证） | `src/main/java/io/aik/steins/grimoire/solutions/{code}/`（README/problem/steps/verification） | `io.aik.steins.grimoire.solutions.{code}` |
| CODE(4) | 代码片段 | 不落文件（仅 DB `content`） | 空；`ext_json.language` |

边界：
- **外部萃取的知识一律落"外部知识"目录**（scrolls / component-manuals / components / solutions）。
- `grimoire-files/learning-notes/` **只放用户特殊整理、明确要 AI 学习的东西**；技能不自动写该目录，除非用户明确要求。
- `ccgui-plugin-creator` 不属于本库迁移范围；不要写进 DSH 技能根。

## 2. 文件格式要求

### 2.1 NOTE（卷轴/学习笔记）

```markdown
# {标题}

> 类型：NOTE | 来源：{URL/项目} | 采集：{YYYY-MM-DD} | 许可：{若有}

## 核心结论
（3–6 条）

## 要点 / 步骤
（结构化要点，不贴全文）

## 适用场景与限制

## 参考
- [{来源标题}]({URL})（采集于 YYYY-MM-DD）
```

### 2.2 COMPONENT 手册

按 `references/component-manual-structure.md` 的 10 章结构；文件头注明来源项目、版本、采集时间。

### 2.3 SOLUTION

```
solutions/{code}/
├── README.md          # 方案概述、适用/不适用
├── problem.md         # 问题与约束
├── steps/*.md         # 解决步骤（编号）
└── verification.md    # 验证方式与结果
```

### 2.4 CODE

不落文件；正文写入 `aik_knowledge.content`，`ext_json` 记录语言/依赖/来源。若用户明确要求落文件，再单独确认路径（避免破坏"代码可编译"边界）。

## 3. 索引更新

| 落点 | 需要更新 |
|---|---|
| `grimoire-scrolls/{slug}/` | `grimoire-scrolls/README.md` 的卷轴表 |
| `src/.../solutions/{code}/` | `src/.../solutions/README.md` 的方案表 |
| `src/.../components/{code}/` | 组件包内 `README.md`（若有） |
| `grimoire-files/component-manuals/` | 不更新 `grimoire-files/README.md`（知识产物被该 README 明确排除在系统设计文档之外） |

## 4. ingest-manifest.json（幂等与审计）

字段定义见 `assets/ingest-manifest.schema.json`；示例：

```json
{
  "runId": "20261111-1130",
  "grimoire": { "path": "/mnt/d/.../AikSteinsGrimoire", "head": "3b1c577", "remote": "git@github.com:..." },
  "sources": [
    { "type": "web-page", "ref": "https://example.com/x", "fetchedAt": "2026-11-11T11:30:00+08:00", "license": "CC BY-SA" }
  ],
  "items": [
    {
      "type": "NOTE",
      "code": "spring-boot-config",
      "target": "grimoire-scrolls/spring-boot-config/spring-boot-config.md",
      "sha256": "…",
      "db": { "table": "aik_knowledge", "code": "spring-boot-config", "status": "pending" },
      "approval": { "landing": true, "dbImport": false }
    }
  ]
}
```

规则：
- 目标文件已存在且 `sha256` 一致 → **skip**；不一致 → 报"已有不同版本"，**不覆盖**。
- `code` 必须 kebab-case 且全局唯一（`aik_knowledge.uk_code`）。
- 重跑必须幂等；manifest 每次运行一份（落 `grimoire-files/plans/ingest-reports/`）。

## 5. 元数据入库

### 5.1 默认：SQL 草案（不执行）

生成 `grimoire-files/plans/ingest-reports/{runId}.sql`：

```sql
SET NAMES utf8mb4;
INSERT INTO aik_knowledge
  (id, title, code, type, summary, content, source_project, source_path,
   resource_path, ext_json, category_id, status)
VALUES
  (<雪花ID>, '<title>', '<code>', <type>, '<summary>', '<content>',
   '<source_project>', '<source_path>', '<resource_path>', '<ext_json>', <category_id>, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title), type = VALUES(type), summary = VALUES(summary),
  content = VALUES(content), source_project = VALUES(source_project),
  source_path = VALUES(source_path), resource_path = VALUES(resource_path),
  ext_json = VALUES(ext_json), category_id = VALUES(category_id), status = 1;
```

字段映射：

| 字段 | 来源 |
|---|---|
| `title` | 文档一级标题 / 组件/方案名 |
| `code` | manifest.code |
| `type` | 1/2/3/4 |
| `summary` | 前 200 字摘要（去 Markdown 标记） |
| `content` | NOTE 正文 / CODE 代码；COMPONENT/SOLUTION 可空 |
| `source_project` | 源项目名 / 网站域名 |
| `source_path` | 源路径 / URL |
| `resource_path` | 见 §1 约定 |
| `ext_json` | `{sourceUrl, fetchedAt, license, codePath, tags, dependencies}` |
| `category_id` | 现有分类 ID；缺失则询问用户 |
| `status` | 1 |

> 用户确认后才执行；执行时保持单事务，失败整体回滚；`id` 用雪花 ID（参考现有 `ImportKnowledge` 的生成方式或由用户指定）。

### 5.2 可选：自动调用 DB（`db=apply`，需显式开启）

- 顺序：连接探活（`SELECT 1`）→ dry-run 打印将影响的行 → 用户确认 → 单事务 → 行数/幂等校验 → 审计 → 回滚 SQL。
- 凭据：运行时读 `application.yml` 或 `DSH_DB_*` 环境变量；**不写入技能、不写入报告、不落 git**。
- 白名单：只允许 `aik_knowledge` 的 `INSERT/UPDATE`；禁止 DDL/DELETE/DROP；单次行数上限。
- 执行器：`scripts/knowledge-import.mjs`（Node，DSH 环境自带；WSL 已实测）：
  - `--draft`（默认）：只生成 SQL 草案，不连库；
  - `--verify`：只读校验（SELECT）；
  - `--apply-dry`：事务内执行后 `ROLLBACK`，验证 SQL 但不落库；
  - `--apply --yes`：单事务提交，执行后按 `code` 校验、写审计 JSON 与回滚 SQL；
  - 连接：`DSH_DB_*` 环境变量或 `application.yml`；WSL 直连 `127.0.0.1` 失败时自动尝试默认网关（Windows MySQL）。
  - 凭据只经 `MYSQL_PWD` 传递，不写日志/报告；白名单表、行数上限、失败整体回滚。

## 6. 运行报告

路径：`grimoire-files/plans/ingest-reports/{runId}.md`，模板见 `assets/ingest-report-template.md`；内容包括：
- 环境与定位结果（envType / path / head / source）；
- 源清单；
- 每条知识的类型/目标路径/SHA-256/决策（新建/跳过/冲突）；
- DB 入库状态（未入库/已生成 SQL/已执行）；
- 索引更新清单；
- 校验结果；
- 未处置项与风险。

## 7. 校验清单

- [ ] 目标文件存在、可读、Markdown 链接可解析
- [ ] `sha256` 与 manifest 一致
- [ ] 无同 code/slug 冲突；无覆盖已有不同版本
- [ ] 索引文件已更新（scrolls/solutions/README）
- [ ] 无凭据/隐私残留 / 无未授权全文
- [ ] SQL 草案字段完整、`uk_code` 语义幂等
- [ ] 报告已生成，路径符合规范

## 8. 回滚

```text
文件：按 manifest 的 target 删除本次新建文件（不删既有）
数据库：按 code 删除本次新增行（保留既有；执行前备份）
运行报告：保留（审计记录），必要时标注"已回滚"
```

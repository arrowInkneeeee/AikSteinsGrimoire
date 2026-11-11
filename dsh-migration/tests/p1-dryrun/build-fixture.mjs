// P1 验证夹具：按 knowledge-landing.md 的 manifest/SQL 契约生成 dry-run 产物。
// 这是测试工具，不属于技能；不写魔典、不连数据库。
import { createHash } from 'node:crypto'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const dir = import.meta.dirname
const draft = readFileSync(resolve(dir, 'conventional-commits.draft.md'), 'utf8')
const sha256 = createHash('sha256').update(draft).digest('hex')
const runId = '20261111-1130'

const manifest = {
  runId,
  grimoire: {
    path: '/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire',
    head: '3b1c577',
    remote: 'git@github.com:arrowInkneeeee/AikSteinsGrimoire.git',
  },
  env: { envType: 'wsl2', strategy: 'direct' },
  sources: [
    {
      type: 'web-page',
      ref: 'https://www.conventionalcommits.org/en/v1.0.0/',
      fetchedAt: '2026-11-11T11:26:00+08:00',
      title: 'Conventional Commits 1.0.0',
      site: 'conventionalcommits.org',
      license: 'CC BY 3.0',
    },
    {
      type: 'local-repo',
      ref: '/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire/src/main/java/io/aik/steins/grimoire/components/threadpool',
      fetchedAt: '2026-11-11T11:27:00+08:00',
      notes: '既有组件包；用于验证 COMPONENT 分类与目标已存在的冲突处理',
    },
  ],
  items: [
    {
      type: 'NOTE',
      code: 'conventional-commits',
      target: 'grimoire-scrolls/conventional-commits/conventional-commits.md',
      sha256,
      sourceRef: 'https://www.conventionalcommits.org/en/v1.0.0/',
      db: { table: 'aik_knowledge', code: 'conventional-commits', status: 'pending' },
      approval: { landing: false, dbImport: false },
    },
    {
      type: 'COMPONENT',
      code: 'threadpool-executor',
      target: 'grimoire-files/component-manuals/threadpool-executor.md',
      sha256: '0000000000000000000000000000000000000000000000000000000000000000',
      sourceRef: '/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire/src/main/java/io/aik/steins/grimoire/components/threadpool',
      db: { table: 'aik_knowledge', code: 'threadpool-executor', status: 'skipped' },
      approval: { landing: false, dbImport: false },
    },
  ],
  result: { created: 0, skipped: 1, conflicts: 1, failed: 0 },
}

const title = 'Conventional Commits 速记'
const summary = '提交信息采用 <type>(<scope>): <description> 结构；fix→PATCH、feat→MINOR、BREAKING CHANGE→MAJOR，可据此自动生成 CHANGELOG 与推导版本号。'
const content = draft
const extJson = JSON.stringify({
  sourceUrl: 'https://www.conventionalcommits.org/en/v1.0.0/',
  fetchedAt: '2026-11-11T11:26:00+08:00',
  license: 'CC BY 3.0',
  tags: ['git', 'commit', 'versioning'],
})
const sqlEscape = (s) => s.replace(/'/g, "''")

const sql = `SET NAMES utf8mb4;
-- DRY-RUN 草案：未执行。按 uk_code 幂等；执行前请先备份。
INSERT INTO aik_knowledge
  (id, title, code, type, summary, content, source_project, source_path, resource_path, ext_json, category_id, status)
VALUES
  (1288834972714496020, '${sqlEscape(title)}', 'conventional-commits', 1, '${sqlEscape(summary)}',
   '${sqlEscape(content)}', 'conventionalcommits.org',
   'https://www.conventionalcommits.org/en/v1.0.0/',
   'grimoire-scrolls/conventional-commits/',
   '${sqlEscape(extJson)}', NULL, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title), type = VALUES(type), summary = VALUES(summary),
  content = VALUES(content), source_project = VALUES(source_project),
  source_path = VALUES(source_path), resource_path = VALUES(resource_path),
  ext_json = VALUES(ext_json), category_id = VALUES(category_id), status = 1;
`

const report = `# 知识摄取运行报告 · ${runId}（P1 dry-run 验证）

> 生成时间：2026-11-11 11:30 | 技能：\`component-extract-rewrite\` | 会话 workspace：\`/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire\`
> 魔典：\`/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire\`（envType=wsl2, head=3b1c577）
> 结论：**dry-run 成功**（不会写魔典、不会入库）。这是 P1 验证夹具，不是真实知识摄取。

## 1. 定位与环境

| 项 | 值 |
|---|---|
| 环境类型 | wsl2 |
| 定位来源 | cwd |
| 魔典路径 | \`/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire\` |
| 写入策略 | 以魔典为 workspace（本夹具不写任何文件） |

## 2. 源清单

| # | 类型 | 来源 | 采集时间 | 许可 |
|---|---|---|---|---|
| 1 | web-page | https://www.conventionalcommits.org/en/v1.0.0/ | 2026-11-11T11:26+08:00 | CC BY 3.0 |

## 3. 知识条目

| # | 类型 | code | 目标路径 | SHA-256（前 12） | 决策 | 入库状态 |
|---|---|---|---|---|---|---|
| 1 | NOTE | conventional-commits | \`grimoire-scrolls/conventional-commits/conventional-commits.md\` | \`${sha256.slice(0, 12)}\` | 将新建（dry-run 未写） | pending |
| 2 | COMPONENT | threadpool-executor | \`grimoire-files/component-manuals/threadpool-executor.md\` | 目标已存在 | **冲突/跳过**：已有手册，不覆盖 | skipped |

### 冲突/跳过原因

- \`threadpool-executor\`：目标 \`grimoire-files/component-manuals/threadpool-executor.md\` 已存在，按"不覆盖已有不同版本"规则跳过（需用户决定是否更新）。

## 4. 索引更新（计划）

| 索引文件 | 变更 |
|---|---|
| \`grimoire-scrolls/README.md\` | 追加 conventional-commits 一行（dry-run 未写） |

## 5. 元数据入库

- [x] 已生成 SQL 草案：\`${runId}.sql\`（本目录）
- [ ] 已执行（默认不执行）
- 说明：\`category_id\` 暂为 NULL，执行前需用户指定分类。

## 6. 校验结果

- [x] 目标路径符合 \`knowledge-landing.md\` §1 路由
- [x] SHA-256 已计算（\`${sha256}\`）
- [x] 无凭据/隐私残留（来源为公开规范页面，仅摘要+出处）
- [x] manifest 符合 \`assets/ingest-manifest.schema.json\`
- [ ] 索引已更新（dry-run 不写）

## 7. 未处置项与风险

- 分类 ID 未确认；真实摄取时需用户指定。
- 网页为英文原文，落盘内容为中文总结（符合"总结归纳 + 出处"策略）。
`

writeFileSync(resolve(dir, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n')
writeFileSync(resolve(dir, `${runId}.sql`), sql)
writeFileSync(resolve(dir, 'report.md'), report)
console.log(JSON.stringify({ runId, sha256, target: manifest.items[0].target }, null, 2))

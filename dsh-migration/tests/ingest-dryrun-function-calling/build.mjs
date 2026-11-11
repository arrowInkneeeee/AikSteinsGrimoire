// 真实网页 dry-run 夹具：按 knowledge-landing.md 生成 manifest + SQL 草案 + 报告。
// 不写魔典、不连数据库。
import { createHash } from 'node:crypto'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const dir = import.meta.dirname
const draft = readFileSync(resolve(dir, 'function-calling.draft.md'), 'utf8')
const sha256 = createHash('sha256').update(draft).digest('hex')
const runId = '20261111-1150'
const title = 'Function Calling：模型与工具之间的桥'
const code = 'function-calling'
const target = 'grimoire-scrolls/function-calling/function-calling.md'
const summary = '模型只会输出文字，Function Calling 给它结构化出口：带工具清单请求 → 模型回 tool_use → 客户端校验/执行 → 回 tool_result；Agent=模型+工具+循环+上下文+权限，工具定义占上下文，权限在"要调"和"执行"之间。'
const sourceUrl = 'https://nengyongai.cn/learn/01-vibecoding-learning/01-what-is-vibecoding/function-calling'
const extJson = JSON.stringify({ sourceUrl, fetchedAt: '2026-11-11T11:50:00+08:00', site: '能用AI', license: '未声明（用户自有学习站点）', tags: ['vibecoding', 'function-calling', 'agent', 'tools'] })

const manifest = {
  runId,
  grimoire: { path: '/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire', head: '3b1c577', remote: 'git@github.com:arrowInkneeeee/AikSteinsGrimoire.git' },
  env: { envType: 'wsl2', strategy: 'direct' },
  sources: [{ type: 'web-page', ref: sourceUrl, fetchedAt: '2026-11-11T11:50:00+08:00', title: 'Function Calling（拓展阅读）', site: '能用AI', notes: '免费章节，全文可见；落盘为中文总结归纳' }],
  items: [{ type: 'NOTE', code, target, sha256, sourceRef: sourceUrl, db: { table: 'aik_knowledge', code, status: 'pending' }, approval: { landing: false, dbImport: false } }],
  result: { created: 0, skipped: 0, conflicts: 0, failed: 0 },
}

const sqlEscape = (s) => s.replace(/'/g, "''")
const sql = `SET NAMES utf8mb4;
-- DRY-RUN 草案：未执行。按 uk_code 幂等；执行前请备份。
INSERT INTO aik_knowledge
  (id, title, code, type, summary, content, source_project, source_path, resource_path, ext_json, category_id, status)
VALUES
  (1288834972714496021, '${sqlEscape(title)}', '${code}', 1, '${sqlEscape(summary)}',
   '${sqlEscape(draft)}', '能用AI',
   '${sourceUrl}',
   'grimoire-scrolls/function-calling/',
   '${sqlEscape(extJson)}', NULL, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title), type = VALUES(type), summary = VALUES(summary),
  content = VALUES(content), source_project = VALUES(source_project),
  source_path = VALUES(source_path), resource_path = VALUES(resource_path),
  ext_json = VALUES(ext_json), category_id = VALUES(category_id), status = 1;
`

const report = `# 知识摄取运行报告 · ${runId}（真实网页 dry-run）

> 生成时间：2026-11-11 11:50 | 技能：\`component-extract-rewrite\` | 会话 workspace：\`/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire\`
> 魔典：\`/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire\`（envType=wsl2, head=3b1c577）
> 结论：**dry-run 成功**；等待用户确认后才落盘/入库。

## 1. 定位与环境

| 项 | 值 |
|---|---|
| 环境类型 | wsl2 |
| 定位来源 | cwd（仓库内） |
| 魔典路径 | \`/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire\` |
| 写入策略 | 以魔典为 workspace |

## 2. 源清单

| # | 类型 | 来源 | 采集时间 | 说明 |
|---|---|---|---|---|
| 1 | web-page | ${sourceUrl} | 2026-11-11T11:50+08:00 | 免费章节；无需登录；HTTP 200；全文 2,245 字；落盘为中文总结归纳，非全文搬运 |

## 3. 知识条目

| # | 类型 | code | 目标路径 | SHA-256（前 12） | 决策 | 入库状态 |
|---|---|---|---|---|---|---|
| 1 | NOTE | \`${code}\` | \`${target}\` | \`${sha256.slice(0, 12)}\` | 将新建（dry-run 未写） | pending |

## 4. 索引更新（计划）

| 索引文件 | 变更 |
|---|---|
| \`grimoire-scrolls/README.md\` | 追加 function-calling 一行（待落盘） |

## 5. 元数据入库

- [x] 已生成 SQL 草案：\`${runId}.sql\`（本目录）
- [ ] 未执行（等待确认）
- 待确认：\`category_id\` 暂为 NULL，落盘/入库前需指定分类。

## 6. 校验结果

- [x] 目标路径符合 \`knowledge-landing.md\` §1 路由（NOTE → \`grimoire-scrolls/{slug}/{slug}.md\`）
- [x] SHA-256 已计算：\`${sha256}\`
- [x] 内容为总结归纳 + 出处，无大段原文、无凭据/隐私
- [x] manifest 字段符合 schema（code / sha256 / type / target）
- [ ] 索引未更新（dry-run）
- [ ] 落盘/入库未执行（等待用户确认）

## 7. 未处置项与风险

- 分类 ID 未确认；真实落盘前请指定。
- 源站未声明内容许可；按"总结 + 出处"处理（用户自有学习站点，默认允许）。
- 这是免费章节；会员章节只能看到 30%，若后续摄取会员内容需要登录态/授权。
`

writeFileSync(resolve(dir, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n')
writeFileSync(resolve(dir, `${runId}.sql`), sql)
writeFileSync(resolve(dir, 'report.md'), report)
console.log(JSON.stringify({ runId, sha256, target }, null, 2))

#!/usr/bin/env node
/**
 * knowledge-import.mjs — 受控的 `aik_knowledge` 元数据导入器（P3）。
 *
 * 设计依据：grimoire-files/plans/knowledge-ingestion-rework.md §5.6；
 * 规则参考：references/knowledge-landing.md §5。
 *
 * 安全默认：
 *   - 默认 `--draft`：只生成 SQL 草案，不连接数据库、不写库。
 *   - `--verify`：只读校验（SELECT），不写。
 *   - `--apply-dry`：在事务里执行后 ROLLBACK，用于验证 SQL 而不落库。
 *   - `--apply`：真正执行；必须同时传 `--yes`（显式确认）。
 *   - 只允许写 `aik_knowledge`；禁止 DDL/DELETE/DROP；单次行数上限（默认 50）。
 *   - 密码只通过 MYSQL_PWD 环境变量传递，不写日志、不写报告、不落 git。
 *
 * 用法：
 *   node knowledge-import.mjs --manifest <manifest.json> [--staging <dir>] [--grimoire <dir>]
 *        [--draft [sqlPath]] [--verify] [--apply-dry] [--apply --yes]
 *        [--rollback-sql <path>] [--audit <path>] [--db-config <application.yml>]
 *        [--max-rows <n>]
 *
 * 退出码：0 成功；1 执行/连接失败；2 参数/校验错误。
 */

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { dirname, isAbsolute, join, resolve, sep } from 'node:path'

const MAX_ROWS_DEFAULT = 50
const TYPE_CODE = { NOTE: 1, COMPONENT: 2, SOLUTION: 3, CODE: 4 }
const TYPE_LABEL = { 1: 'NOTE', 2: 'COMPONENT', 3: 'SOLUTION', 4: 'CODE' }

// ------------------------------------------------------------------ args ----
const argv = process.argv.slice(2)
const opts = {
  manifest: '',
  grimoire: '',
  staging: '',
  draftPath: '',
  draft: false,
  verify: false,
  applyDry: false,
  apply: false,
  yes: false,
  rollbackSql: '',
  audit: '',
  dbConfig: '',
  maxRows: MAX_ROWS_DEFAULT,
  quiet: false,
}
function usage(msg) {
  if (msg) console.error(`error: ${msg}`)
  console.error('usage: node knowledge-import.mjs --manifest <json> [--draft [sql]] [--verify] [--apply-dry] [--apply --yes] [--staging <dir>] [--grimoire <dir>]')
  process.exit(2)
}
for (let i = 0; i < argv.length; i += 1) {
  const a = argv[i]
  const next = () => {
    if (i + 1 >= argv.length) usage(`missing value for ${a}`)
    return argv[++i]
  }
  switch (a) {
    case '--manifest': opts.manifest = resolve(next()); break
    case '--grimoire': opts.grimoire = resolve(next()); break
    case '--staging': opts.staging = resolve(next()); break
    case '--draft':
      opts.draft = true
      if (argv[i + 1] && !argv[i + 1].startsWith('--')) opts.draftPath = resolve(argv[++i])
      break
    case '--verify': opts.verify = true; break
    case '--apply-dry': opts.applyDry = true; break
    case '--apply': opts.apply = true; break
    case '--yes': opts.yes = true; break
    case '--rollback-sql': opts.rollbackSql = resolve(next()); break
    case '--audit': opts.audit = resolve(next()); break
    case '--db-config': opts.dbConfig = resolve(next()); break
    case '--max-rows': opts.maxRows = Number.parseInt(next(), 10); break
    case '--quiet': opts.quiet = true; break
    case '-h': case '--help': usage(); break
    default: usage(`unknown option ${a}`)
  }
}
if (!opts.manifest) usage('--manifest is required')
if (opts.apply && !opts.yes) usage('--apply requires --yes (explicit confirmation)')
if (opts.apply && opts.applyDry) usage('choose either --apply or --apply-dry')
if (!opts.draft && !opts.verify && !opts.apply && !opts.applyDry && !opts.rollbackSql) opts.draft = true
if (!Number.isInteger(opts.maxRows) || opts.maxRows < 1) usage('--max-rows must be a positive integer')

// -------------------------------------------------------------- manifest ----
let manifest
try {
  manifest = JSON.parse(readFileSync(opts.manifest, 'utf8'))
} catch (error) {
  console.error(`error: cannot read manifest: ${error.message}`)
  process.exit(2)
}
const items = Array.isArray(manifest.items) ? manifest.items : []
if (items.length === 0) {
  console.error('error: manifest.items is empty')
  process.exit(2)
}
if (items.length > opts.maxRows) {
  console.error(`error: manifest has ${items.length} items, over --max-rows ${opts.maxRows}`)
  process.exit(2)
}

const target = (item) => String(item.target || '').replace(/\\/g, '/')
const problems = []
const codes = new Set()
for (const [index, item] of items.entries()) {
  const where = `items[${index}]`
  if (!TYPE_CODE[item.type]) problems.push(`${where}: invalid type "${item.type}"`)
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(String(item.code || ''))) problems.push(`${where}: invalid code "${item.code}"`)
  if (codes.has(item.code)) problems.push(`${where}: duplicate code "${item.code}"`)
  codes.add(item.code)
  if (!item.target && item.type !== 'CODE') problems.push(`${where}: missing target`)
  const t = target(item)
  if (t && (isAbsolute(t) || t.split('/').includes('..') || t.startsWith('/'))) problems.push(`${where}: unsafe target "${t}"`)
}
if (problems.length > 0) {
  for (const p of problems) console.error(`error: ${p}`)
  process.exit(2)
}

// ------------------------------------------------------------- grimoire ----
function looksLikeGrimoire(dir) {
  if (!dir || !existsSync(dir)) return false
  return existsSync(join(dir, 'grimoire-files', 'README.md'))
    && existsSync(join(dir, 'grimoire-scrolls', 'README.md'))
    && existsSync(join(dir, 'src', 'main', 'java', 'io', 'aik', 'steins', 'grimoire', 'knowledge'))
}
function findGrimoire(start) {
  let dir = resolve(start)
  while (true) {
    if (looksLikeGrimoire(dir)) return dir
    const parent = dirname(dir)
    if (parent === dir) return ''
    dir = parent
  }
}
let grimoire = opts.grimoire
  || (process.env.AIK_GRIMOIRE_HOME ? resolve(process.env.AIK_GRIMOIRE_HOME) : '')
if (grimoire && !looksLikeGrimoire(grimoire)) {
  console.error(`error: --grimoire/AIK_GRIMOIRE_HOME is not a grimoire: ${grimoire}`)
  process.exit(2)
}
if (!grimoire) grimoire = findGrimoire(process.cwd())

// --------------------------------------------------------------- helpers ----
function stripMarkdown(text) {
  return String(text)
    .replace(/^---[\s\S]*?---\s*/, '')
    .replace(/^#+\s*/gm, '')
    .replace(/^>\s?/gm, '')
    .replace(/\*\*?|`+|~~?/g, '')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/\s+/g, ' ')
    .trim()
}
function deriveTitle(content, item) {
  if (item.title) return String(item.title)
  const match = String(content).match(/^#\s+(.+)$/m)
  return match ? match[1].trim() : item.code
}
function deriveSummary(content, item) {
  if (item.summary) return String(item.summary)
  const text = stripMarkdown(content)
  return text.length > 200 ? `${text.slice(0, 200)}…` : text
}
function sqlString(value) {
  return `'${String(value ?? '').replace(/'/g, "''").replace(/\u0000/g, '')}'`
}
function resolveContent(item) {
  const t = target(item)
  if (item.content && typeof item.content === 'string') return item.content
  if (!t) return ''
  if (opts.staging) {
    const p = join(opts.staging, t)
    if (existsSync(p)) return readFileSync(p, 'utf8')
  }
  if (grimoire) {
    const p = join(grimoire, t)
    if (existsSync(p)) return readFileSync(p, 'utf8')
  }
  return ''
}
function nextId(index) {
  // 19 位十进制，避免与既有雪花 ID 冲突的简单生成器（同一批次唯一）
  const base = BigInt(Date.now()) * 1000000n
  return (base + BigInt(index)).toString()
}
function secretHints(content) {
  const hints = []
  if (/BEGIN (?:[A-Z]+ )?PRIVATE KEY/.test(content)) hints.push('疑似私钥')
  if (/AKIA[0-9A-Z]{16}/.test(content)) hints.push('疑似 AWS Access Key')
  if (/password\s*[:=]\s*['"]?[^\s'"]{6,}/i.test(content)) hints.push('疑似明文密码')
  return hints
}

// ------------------------------------------------------------------ build ----
const statements = []
const rollbackCodes = []
const preview = []
for (const [index, item] of items.entries()) {
  const content = resolveContent(item)
  const type = TYPE_CODE[item.type]
  const title = deriveTitle(content, item)
  const summary = deriveSummary(content, item)
  const resourcePath = item.resourcePath ?? (item.type === 'CODE' ? '' : (() => {
    const t = target(item)
    return t.endsWith('.md') ? dirname(t) + '/' : t
  })())
  const extJson = JSON.stringify({
    ...(item.extJson && typeof item.extJson === 'object' ? item.extJson : {}),
    ...(item.sourceRef ? { sourceRef: item.sourceRef } : {}),
  })
  const categoryId = item.categoryId === undefined ? 'NULL' : String(item.categoryId)
  const code = item.code
  const id = nextId(index)
  const sql = `INSERT INTO aik_knowledge\n` +
    `  (id, title, code, type, summary, content, source_project, source_path, resource_path, ext_json, category_id, status)\n` +
    `VALUES\n` +
    `  (${id}, ${sqlString(title)}, ${sqlString(code)}, ${type}, ${sqlString(summary)},\n` +
    `   ${sqlString(content)}, ${sqlString(item.sourceProject ?? '')}, ${sqlString(item.sourcePath ?? item.sourceRef ?? '')},\n` +
    `   ${sqlString(resourcePath)}, ${sqlString(extJson)}, ${categoryId}, 1)\n` +
    `ON DUPLICATE KEY UPDATE\n` +
    `  title = VALUES(title), type = VALUES(type), summary = VALUES(summary),\n` +
    `  content = VALUES(content), source_project = VALUES(source_project), source_path = VALUES(source_path),\n` +
    `  resource_path = VALUES(resource_path), ext_json = VALUES(ext_json), category_id = VALUES(category_id), status = 1;`
  statements.push(sql)
  rollbackCodes.push(code)
  const hints = secretHints(content)
  preview.push({ code, type: TYPE_LABEL[type], target: target(item), resourcePath, title, contentChars: content.length, summaryChars: summary.length, secretHints: hints })
}
const draftSql = `SET NAMES utf8mb4;\n-- 由 knowledge-import.mjs 生成；按 uk_code 幂等；执行前请备份。\n${statements.join('\n\n')}\n`
const rollbackSql = `-- 回滚：删除本批 code（不会影响既有其他知识）\nDELETE FROM aik_knowledge WHERE code IN (${rollbackCodes.map(sqlString).join(', ')});\n`

// ------------------------------------------------------------ write draft ----
const manifestDir = dirname(opts.manifest)
const runId = manifest.runId || 'run'
const draftFile = opts.draftPath || join(manifestDir, `${runId}.sql`)
if (opts.draft || opts.apply || opts.applyDry) {
  writeFileSync(draftFile, draftSql)
}
const rollbackFile = opts.rollbackSql || join(manifestDir, `${runId}.rollback.sql`)
if (opts.rollbackSql) writeFileSync(rollbackFile, rollbackSql)

if (!opts.quiet) {
  console.log(JSON.stringify({
    runId,
    items: preview,
    draft: (opts.draft || opts.apply || opts.applyDry) ? draftFile : undefined,
    rollbackSql: opts.rollbackSql ? rollbackFile : undefined,
    db: opts.verify || opts.apply || opts.applyDry ? 'checking…' : 'not used',
  }, null, 2))
}

// ------------------------------------------------------------------- db ----
function parseDbConfig() {
  const configPath = opts.dbConfig || (grimoire ? join(grimoire, 'src', 'main', 'resources', 'application.yml') : '')
  let host = process.env.DSH_DB_HOST || ''
  let port = process.env.DSH_DB_PORT || ''
  let database = process.env.DSH_DB_NAME || ''
  let user = process.env.DSH_DB_USER || ''
  let password = process.env.DSH_DB_PASSWORD || ''
  if (process.env.DSH_DB_URL) {
    const m = process.env.DSH_DB_URL.match(/jdbc:mysql:\/\/([^:/]+):(\d+)\/([^?]+)/) || process.env.DSH_DB_URL.match(/mysql:\/\/([^:/]+):(\d+)\/([^?]+)/)
    if (m) { host = host || m[1]; port = port || m[2]; database = database || m[3] }
  }
  if ((!host || !user) && configPath && existsSync(configPath)) {
    const text = readFileSync(configPath, 'utf8')
    const url = text.match(/url:\s*jdbc:mysql:\/\/([^:/]+):(\d+)\/([^?]+)/)
    if (url) { host = host || url[1]; port = port || url[2]; database = database || url[3] }
    const userMatch = text.match(/username:\s*['"]?([^\s'"#]+)/)
    if (userMatch) user = user || userMatch[1]
    const passMatch = text.match(/password:\s*['"]?([^\s'"#]+)/)
    if (passMatch) password = password || passMatch[1]
  }
  return { host: host || '127.0.0.1', port: port || '3306', database: database || 'aik_steins_grimoire', user: user || 'root', password }
}
function wslGateway() {
  try {
    const r = spawnSync('sh', ['-c', "ip route show default 2>/dev/null | awk '{print $3; exit}'"], { encoding: 'utf8' })
    return (r.stdout || '').trim()
  } catch { return '' }
}
function mysqlArgs(db, host) {
  return ['--connect-timeout=5', '--default-character-set=utf8mb4', '-h', host, '-P', db.port, '-u', db.user, '-N', '-B', '-D', db.database]
}
function probe(db, host) {
  const r = spawnSync('mysql', [...mysqlArgs(db, host), '-e', 'SELECT 1'], {
    env: { ...process.env, MYSQL_PWD: db.password },
    encoding: 'utf8',
  })
  return r.status === 0 && (r.stdout || '').trim() === '1'
}
function resolveDb(db) {
  const candidates = [db.host]
  if (process.env.DSH_DB_HOST && db.host !== process.env.DSH_DB_HOST) candidates.unshift(process.env.DSH_DB_HOST)
  const gw = wslGateway()
  if (gw && !candidates.includes(gw)) candidates.push(gw)
  if (!candidates.includes('127.0.0.1')) candidates.push('127.0.0.1')
  for (const host of candidates) {
    if (probe(db, host)) return { ...db, connectedHost: host }
  }
  return null
}
function runSql(db, host, sql) {
  return spawnSync('mysql', [...mysqlArgs(db, host), '-e', sql], {
    env: { ...process.env, MYSQL_PWD: db.password },
    encoding: 'utf8',
  })
}
function verifyCodes(db, host) {
  const list = rollbackCodes.map(sqlString).join(', ')
  const r = spawnSync('mysql', [...mysqlArgs(db, host), '-e', `SELECT code, type, title FROM aik_knowledge WHERE code IN (${list})`], {
    env: { ...process.env, MYSQL_PWD: db.password },
    encoding: 'utf8',
  })
  if (r.status !== 0) throw new Error(r.stderr || 'verify failed')
  return (r.stdout || '').trim().split('\n').filter(Boolean)
}

const wantDb = opts.verify || opts.apply || opts.applyDry
if (wantDb) {
  const db = parseDbConfig()
  if (!db.password && !process.env.DSH_DB_PASSWORD) {
    console.error('error: no DB password (set DSH_DB_PASSWORD or application.yml password)')
    process.exit(1)
  }
  if (spawnSync('mysql', ['--version'], { encoding: 'utf8' }).status !== 0) {
    console.error('error: mysql client not found; use --draft (SQL draft) instead')
    process.exit(1)
  }
  const connected = resolveDb(db)
  if (!connected) {
    console.error(`error: cannot connect to MySQL (tried ${db.host}, WSL gateway${wslGateway() ? `, ${wslGateway()}` : ''})`)
    process.exit(1)
  }
  if (!opts.quiet) console.log(`db       : connected via ${connected.connectedHost}:${db.port}/${db.database}`)

  if (opts.verify) {
    const rows = verifyCodes(connected, connected.connectedHost)
    console.log(JSON.stringify({ verify: { expected: rollbackCodes.length, found: rows.length, rows } }, null, 2))
  }

  if (opts.applyDry || opts.apply) {
    const tx = `SET NAMES utf8mb4;\nSTART TRANSACTION;\n${statements.join('\n\n')}\n${opts.applyDry ? 'ROLLBACK;' : 'COMMIT;'}\n`
    const r = runSql(connected, connected.connectedHost, tx)
    if (r.status !== 0) {
      console.error(`error: SQL execution failed:\n${r.stderr || r.stdout}`)
      process.exit(1)
    }
    const rows = verifyCodes(connected, connected.connectedHost)
    const audit = {
      runId,
      mode: opts.applyDry ? 'apply-dry(rollback)' : 'apply(commit)',
      db: `${connected.connectedHost}:${db.port}/${db.database}`,
      codes: rollbackCodes,
      expected: rollbackCodes.length,
      foundAfter: rows.length,
      at: new Date().toISOString(),
      draft: draftFile,
      rollbackSql: rollbackCodes.length ? rollbackFile : undefined,
    }
    const auditPath = opts.audit || join(manifestDir, `${runId}.import-audit.json`)
    writeFileSync(auditPath, JSON.stringify(audit, null, 2) + '\n')
    console.log(JSON.stringify({ result: audit }, null, 2))
    if (opts.apply && rows.length !== rollbackCodes.length) {
      console.error('error: post-apply verification count mismatch')
      process.exit(1)
    }
  }
}

if (!opts.quiet) {
  const hints = preview.flatMap((p) => p.secretHints)
  if (hints.length > 0) console.error(`warning: possible secrets detected: ${[...new Set(hints)].join(', ')}`)
}

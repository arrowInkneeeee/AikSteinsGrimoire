#!/usr/bin/env node
/**
 * Verify DSH skill discovery against real skill roots.
 *
 * Uses the exact provider implementation shipped with the installed DSH
 * (`@deepseek-ai/dsh-skill-filesystem`) so the result matches what the
 * web/headless service would put in a session catalog: same roots, same
 * frontmatter parsing, same name validation, same on-demand body loading.
 *
 * Usage:
 *   node dsh-migration/tools/list-skills.mjs <root> [<root> ...]
 *   node dsh-migration/tools/list-skills.mjs --expect name-a,name-b <root>
 *
 * `--expect` fails the run when any expected catalog name was not discovered
 * (this is how install.sh --verify catches a skill silently dropped because of
 * invalid frontmatter).
 *
 * Exit code 0 = every discovered skill loads and every expected name is present.
 * Exit code 1 = missing expected name, failed load, failed root, or empty root.
 */

import { existsSync } from 'node:fs'
import { dirname, resolve } from 'node:path'

const DSH_SKILL_FILESYSTEM =
  process.env.DSH_SKILL_FILESYSTEM ??
  '/home/arrowinknee/.nvm/versions/node/v24.19.0/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-skill-filesystem/lib/index.js'

const { FileSystemSkillProvider } = await import(DSH_SKILL_FILESYSTEM)

const roots = []
let expected = []
for (let i = 2; i < process.argv.length; i += 1) {
  const arg = process.argv[i]
  if (arg === '--expect') {
    expected = (process.argv[++i] ?? '')
      .split(',')
      .map((name) => name.trim())
      .filter(Boolean)
  } else {
    roots.push(resolve(arg))
  }
}
if (roots.length === 0) {
  console.error('usage: node dsh-migration/tools/list-skills.mjs [--expect a,b] <skill-root> [...]')
  process.exit(2)
}

const warnings = []
const ctx = {
  logger: {
    warn(message) {
      warnings.push(String(message))
    },
  },
  // Pure filesystem provider: no ctx.fs service in this harness, which makes
  // the provider use its plain-Node discovery path.
  get() {
    return undefined
  },
}
const controller = new AbortController()
const control = { invalidate() {}, signal: controller.signal }
const provider = new FileSystemSkillProvider(ctx, control, {
  providerName: 'filesystem',
  includeDefaultRoots: false,
  customSkillDirs: roots,
  watch: false,
})

const listed = await provider.list({ cwd: process.cwd(), signal: controller.signal })
const candidates = Array.isArray(listed) ? listed : listed.candidates
const complete = Array.isArray(listed) ? true : listed.complete !== false

const rows = []
let failures = 0

for (const candidate of candidates) {
  let loaded
  try {
    loaded = await provider.get(candidate, { signal: controller.signal })
  } catch (error) {
    failures += 1
    rows.push({ name: candidate.name, status: 'LOAD_ERROR', error: String(error) })
    continue
  }
  if (loaded === undefined) {
    failures += 1
    rows.push({ name: candidate.name, status: 'MISSING_AT_LOAD' })
    continue
  }
  const body = loaded.content ?? ''
  const base = dirname(loaded.path)
  rows.push({
    name: loaded.name,
    status: 'OK',
    descriptionChars: [...loaded.description].length,
    bodyChars: [...body].length,
    source: loaded.source,
    path: loaded.path,
    resourceBase: loaded.resourceBase?.path,
    resources: {
      references: existsSync(resolve(base, 'references')),
      assets: existsSync(resolve(base, 'assets')),
      scripts: existsSync(resolve(base, 'scripts')),
    },
  })
}

await provider.dispose()

const discovered = rows.filter((row) => row.status === 'OK').map((row) => row.name)
const missing = expected.filter((name) => !discovered.includes(name))
const extra = discovered.filter((name) => !expected.includes(name))

const payload = {
  roots,
  complete,
  expected,
  missing,
  extra,
  skills: rows.filter((row) => row.status === 'OK'),
  failures: rows.filter((row) => row.status !== 'OK'),
  warnings,
}
console.log(JSON.stringify(payload, null, 2))

if (!complete) {
  console.error('discovery incomplete: one or more roots failed')
  failures += 1
}
if (missing.length > 0) {
  console.error(`missing expected skill(s): ${missing.join(', ')}`)
  failures += 1
}
for (const warning of warnings) console.error(`[provider] ${warning}`)
if (failures > 0 || rows.length === 0) process.exit(1)

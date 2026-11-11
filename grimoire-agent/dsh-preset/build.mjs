// Regenerate cordis.patch.yml from persona-prefix.txt + the current standard preset.
// Usage: node build.mjs   (requires `dsh` on PATH; run from this directory)
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const PROFILE = process.env.DSH_PROFILE || 'web';

const dump = execFileSync('dsh', ['--profile', PROFILE, '--dump-config'], { encoding: 'utf8' });
const lines = dump.split(/\r?\n/);
const start = lines.findIndex((line) => line === '- id: preset-standard');
if (start < 0) throw new Error('preset-standard not found in dsh --dump-config');
let end = -1;
for (let i = start + 1; i < lines.length; i += 1) {
  if (/^- id: preset-/.test(lines[i])) { end = i; break; }
}
if (end < 0) throw new Error('end of preset-standard block not found');
const block = lines.slice(start, end);
const pluginsAt = block.indexOf('    plugins:');
if (pluginsAt < 0) throw new Error('plugins list not found in standard preset');
let children = block.slice(pluginsAt + 1);
const personaStart = children.findIndex((line) => line === '      - id: persona');
if (personaStart < 0) throw new Error('persona child not found in standard preset');
let personaEnd = children.length;
for (let i = personaStart + 1; i < children.length; i += 1) {
  if (children[i].startsWith('      - id: ')) { personaEnd = i; break; }
}
children = [...children.slice(0, personaStart), ...children.slice(personaEnd)];

const prefix = readFileSync(join(HERE, 'persona-prefix.txt'), 'utf8').replace(/\r\n/g, '\n').replace(/\s+$/, '');
const prefixLines = prefix.split('\n').map((line) => (line ? `            ${line}` : ''));
const persona = [
  '      - id: persona',
  "        name: '@deepseek-ai/dsh-persona'",
  '        config:',
  '          prefix: |',
  ...prefixLines,
];
const header = [
  '- id: preset-grimoire-trinity',
  "  name: '@deepseek-ai/dsh-agent-preset'",
  '  config:',
  '    id: grimoire-trinity',
  '    name: "月見里 & 綾雾"',
  '    description: "一个 Agent，两个人设：綾雾彌奈（默认/日常）与月見里緋緖（开发）"',
  '    order: 10',
  '    plugins:',
];
const row = [...header, ...persona, ...children];
const patch = ['- insert:', ...row.map((line) => `  ${line}`), ''].join('\n');
writeFileSync(join(HERE, 'cordis.patch.yml'), patch, 'utf8');
console.log(`wrote cordis.patch.yml (${row.length} lines, ${children.length} child plugins)`);

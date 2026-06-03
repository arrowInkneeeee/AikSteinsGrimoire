---
name: gitnexus
description: GitNexus code intelligence suite for exploring, debugging, impact analysis, refactoring, PR review. Use when working with GitNexus MCP tools to understand code, trace bugs, assess blast radius, refactor safely, or review pull requests.
type: Skill
version: 1.0.0
compatibility: Requires GitNexus MCP tools and project index (.gitnexus/)
---

# GitNexus Code Intelligence Suite

GitNexus provides powerful code understanding capabilities through a knowledge graph of your codebase. This suite includes 7 specialized skills for different workflows.

## 🚀 Quick Start

### ⚠️ Prerequisites: Index Your Project First

GitNexus tools require a code index to work. Before using any skill:

**1. Check if index exists:**
```bash
npx gitnexus status
```

**2. Create or refresh index** (run from project root):
```bash
npx gitnexus analyze
```

**3. Verify index loaded:**
In conversation, read: `gitnexus://repo/{project-name}/context`

> 💡 **Tip**: The index is project-specific. Each new project needs its own index.

Then choose a skill below based on your task.

## 📚 Available Skills

| Skill | When to Use | Key Tools |
|-------|-------------|-----------|
| **[getting-started](references/gitnexus-getting-started.md)** | **NEW TO GITNEXUS?** Complete guide: install → scenarios → skills → performance | Full workflow |
| **[exploring](references/gitnexus-exploring.md)** | "How does X work?", explore architecture, trace execution flows | `query`, `context`, process resources |
| **[debugging](references/gitnexus-debugging.md)** | "Why is X failing?", trace errors, investigate bugs | `query`, `context`, `cypher` |
| **[impact-analysis](references/gitnexus-impact-analysis.md)** | "What will break if I change X?", safety analysis before editing | `impact`, `detect_changes` |
| **[refactoring](references/gitnexus-refactoring.md)** | "Rename this function", extract/split/restructure code | `rename`, `impact`, `detect_changes` |
| **[pr-review](references/gitnexus-pr-review.md)** | "Review this PR", assess merge safety, check test coverage | `detect_changes`, `impact`, `context` |
| **[cli](references/gitnexus-cli.md)** | Index/analyze repo, check status, clean index, generate wiki | CLI commands |
| **[guide](references/gitnexus-guide.md)** | Tool reference, graph schema, query syntax | All tools reference |

## 🎯 Common Workflows

### Understanding Code (New Codebase)

```
1. READ gitnexus://repo/{name}/context
2. gitnexus_query({query: "authentication flow"})
3. gitnexus_context({name: "validateUser"})
4. READ gitnexus://repo/{name}/process/LoginFlow
```

### Debugging a Bug

```
1. gitnexus_query({query: "payment validation error"})
2. gitnexus_context({name: "validatePayment"})
3. READ gitnexus://repo/{name}/process/CheckoutFlow
4. Root cause found: external API without timeout
```

### Before Making Changes

```
1. gitnexus_impact({target: "validateUser", direction: "upstream"})
2. Review d=1 items (WILL BREAK)
3. Check affected processes
4. Assess risk: LOW/MEDIUM/HIGH/CRITICAL
5. Update all callers before committing
```

### Refactoring Code

```
1. gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})
2. Review preview (graph edits safe, ast_search need review)
3. Apply: gitnexus_rename({...}, dry_run: false)
4. Verify: gitnexus_detect_changes()
5. Run tests for affected processes
```

### Reviewing Pull Requests

```
1. gh pr diff <number>
2. gitnexus_detect_changes({scope: "compare", base_ref: "main"})
3. For each changed symbol: gitnexus_impact({target, direction: "upstream"})
4. Check if d=1 callers are updated in PR
5. Write review with risk assessment
```

## 🛠️ Core Tools Reference

| Tool | Purpose | Example |
|------|---------|---------|
| `query` | Find execution flows by concept | `gitnexus_query({query: "payment processing"})` |
| `context` | 360° view of a symbol | `gitnexus_context({name: "validateUser"})` |
| `impact` | Blast radius analysis | `gitnexus_impact({target: "X", direction: "upstream"})` |
| `detect_changes` | Git-diff impact mapping | `gitnexus_detect_changes({scope: "staged"})` |
| `rename` | Automated multi-file rename | `gitnexus_rename({symbol_name: "old", new_name: "new"})` |
| `cypher` | Custom graph queries | `MATCH (a)-[:CALLS]->(b) RETURN a,b` |

## 📊 Risk Assessment Guide

| Signal | Risk Level | Action |
|--------|------------|--------|
| <5 symbols, few processes | **LOW** | Safe to proceed |
| 5-15 symbols, 2-5 processes | **MEDIUM** | Test thoroughly |
| >15 symbols or many processes | **HIGH** | Careful planning needed |
| Critical path (auth, payments) | **CRITICAL** | Maximum caution, full test coverage |

## 🔧 CLI Commands

| Command | Purpose |
|---------|---------|
| `npx gitnexus analyze` | Build/refresh index |
| `npx gitnexus status` | Check index freshness |
| `npx gitnexus clean` | Delete index |
| `npx gitnexus wiki` | Generate docs from graph |
| `npx gitnexus list` | Show indexed repos |

**Common flags:**
- `--force`: Force operation
- `--embeddings`: Enable semantic search (off by default)
- `--all`: Apply to all repos

## 📖 Graph Schema

**Nodes:** File, Function, Class, Interface, Method, Community, Process

**Edges (CodeRelation.type):** CALLS, IMPORTS, EXTENDS, IMPLEMENTS, DEFINES, MEMBER_OF, STEP_IN_PROCESS

**Example Cypher Query:**
```cypher
MATCH (caller)-[:CodeRelation {type: 'CALLS'}]->(f:Function {name: "myFunc"})
RETURN caller.name, caller.filePath
```

## ⚠️ Important Rules

1. **ALWAYS run impact analysis before editing** any non-trivial code
2. **NEVER ignore HIGH/CRITICAL risk warnings** from impact analysis
3. **MUST update all d=1 (WILL BREAK) dependents** before committing
4. **ALWAYS verify changes** with `gitnexus_detect_changes()` after refactoring
5. **Re-run analyze after major changes** to keep index fresh

## 🎓 Learning Path

**New to GitNexus?** Start here:

1. **必读**: [references/gitnexus-getting-started.md](references/gitnexus-getting-started.md) - 完整入门指南 (安装→场景→技能→性能→最佳实践)
2. Read [references/gitnexus-cli.md](references/gitnexus-cli.md) for CLI commands reference
3. Read [references/gitnexus-guide.md](references/gitnexus-guide.md) for tool reference
4. Try [references/gitnexus-exploring.md](references/gitnexus-exploring.md) on your codebase
5. Practice [references/gitnexus-debugging.md](references/gitnexus-debugging.md) on known issues
6. Learn [references/gitnexus-impact-analysis.md](references/gitnexus-impact-analysis.md) before making changes
7. Master [references/gitnexus-refactoring.md](references/gitnexus-refactoring.md) and [references/gitnexus-pr-review.md](references/gitnexus-pr-review.md)

## 💡 Pro Tips

- **Start broad**: Use `query` with high-level concepts ("authentication", "payment flow")
- **Go deep**: Use `context` on key symbols to see all connections
- **Trace flows**: Read process resources for step-by-step execution traces
- **Check confidence**: Impact analysis shows confidence scores (>0.8 = high confidence)
- **Use dry_run**: Always preview refactoring changes before applying

## 📝 Additional Resources

- Individual skill files contain detailed workflows and examples
- Each skill has its own checklist for systematic execution
- Process resources (`gitnexus://repo/{name}/process/{name}`) provide execution traces (~200 tokens)
- Cluster resources show functional areas with cohesion scores

---

**Remember:** GitNexus is your code intelligence copilot. Use it systematically for safer, faster, more confident development!

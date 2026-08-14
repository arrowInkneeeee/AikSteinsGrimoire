# grimoire-agent — 綾雾彌奈智能体管理目录

> 版本：v1.0 | 日期：2026-07-31
> 本目录独立管理綾雾彌奈（aIk-agent）的智能体定义、进化协议和扩展计划。

---

## 目录结构

```
grimoire-agent/
├── agent/                          # 智能体运行时文件（Qoder 通过 symlink 引用）
│   ├── aIk-agent.md                # 智能体本体（v2.0，symlink 源文件）
│   ├── evolution-protocol.md       # 进化协议（四支柱 + 里程碑制）
│   ├── decision-log.md             # 具体决策档案（按领域分表）
│   └── README.md                   # 本文件
├── agent-zero/                     # 初始企划存档（只读，历史参考）
│   ├── 01-agent-plan.md
│   ├── 02-personality-design.md
│   ├── 03-confirmation-gates.md
│   ├── 04-clarification-questions.md
│   └── README.md
└── plan/                           # 计划与设计文档
    ├── agent-expansion-plan.md     # 扩展计划（Phase 1 进行中）
    └── evolution-mechanism-redesign.md  # 进化机制重构设计文档
```

---

## Qoder 注册方式

### Symlink 注册

Qoder 通过文件级 SymbolicLink 加载智能体定义，需管理员权限执行：

```powershell
# 创建 symlink（管理员 PowerShell）
New-Item -ItemType SymbolicLink `
  -Path "C:\Users\arrowInknee\.qoder-cn\agents\aIk-agent.md" `
  -Target "D:\JeBrainsWorkSpace\AikSteinsGrimoire\grimoire-agent\agent\aIk-agent.md"
```

**验证**：
```powershell
Get-Item "C:\Users\arrowInknee\.qoder-cn\agents\aIk-agent.md" | Select-Object LinkType, Target
```

### Frontmatter 格式参考

`aIk-agent.md` 头部的 frontmatter 决定 Qoder 路由行为，**修改时不可改动**：

```yaml
---
name: aIk-agent
description: 綾雾彌奈 — 觀月的专属 Java 后端开发搭档...（路由匹配依据）
tools: Read, Glob, Grep, Bash, Write, SearchReplace, SearchCodebase, LSP, Skill, WebSearch, WebFetch
---
```

- `name`：智能体标识，影响 Qoder 内部路由
- `description`：路由匹配的关键字段，改动会导致路由失效
- `tools`：可用工具列表

### 触发方式

- 自动路由：当用户请求开发任务时，Qoder 根据 description 匹配
- 手动触发：用户说"月見里帮我"/"綾雾帮我"/"绫雾帮我"

---

## 进化机制说明

### 四支柱架构（v2.0）

| 支柱 | 作用 | 落地位置 |
|------|------|---------|
| 支柱 1：强制对话结论 | 每个任务单元结束时输出信号摘要 | `aIk-agent.md` 硬性规则 |
| 支柱 2：用户驱动归档 | 用户说"记录一下"触发归档 | 默认智能体执行 |
| 支柱 3：里程碑制信任进阶 | 可验证的能力里程碑替代计数制 | `evolution-protocol.md` |
| 支柱 4：具体决策档案 | 只记实际决策，不做抽象推断 | `decision-log.md` |

### 归档工作流

```
綾雾彌奈（任务结束）
  → 输出信号摘要（对话末尾）
  → UpdateMemory 暂存（兜底）

用户（下次 Quest）
  → 和默认智能体说"记录一下" + 描述
  → 默认智能体读 decision-log.md → 追加 → 检查里程碑
  → 如达成里程碑 → 提议信任进阶
  → 删除"未归档信号"记忆条目
```

### 信任等级（按领域独立）

| 领域 | 当前等级 | 起始理由 |
|------|---------|---------|
| 后端 | Lv.1（初始） | 用户最熟悉，需更多验证 |
| 前端 | Lv.2（熟悉） | 用户不熟悉前端，高自主 + 详细说明 |
| 通用 | Lv.1（初始） | 默认 |

---

## 踩坑记录

### 智能体注册方案对比

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| `.agent.md`（项目级） | 简单，放在项目根目录即可 | 和项目绑定，无法跨项目管理智能体身份 | ❌ 不适合 |
| Qoder Plugin | 可以打包分发 | 面向通用插件，不适合个人定制智能体 | ❌ 不适合 |
| **对话级原生（symlink + frontmatter）** | 灵活、个人化、修改源文件实时生效 | 需要管理员权限建 symlink | ✅ 采用 |

### 已知问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Move-Item 迁移文件后 Git 不识别 | Git 需要 `git add` 才能识别 rename | 迁移后立即 `git add` |
| IDE 文件树不显示新目录 | 文件树缓存未刷新 | 重启 IDE 或手动刷新 |
| Symlink 创建失败 | 非管理员权限 | 用管理员 PowerShell 执行 |
| D1-D6/G1-G3 维度从未真正生效 | LLM 无法可靠执行抽象偏好推断 | 改用具体决策档案 |
| 对话隔离导致跨对话计数失效 | Qoder 每次对话是全新实例 | 改用里程碑制 + UpdateMemory 兜底 |

---

## 日常维护

| 操作 | 步骤 |
|------|------|
| **更新智能体内容** | 直接编辑 `agent/aIk-agent.md` → Qoder 实时生效（symlink） |
| **记录新决策** | 编辑 `agent/decision-log.md` → 在对应领域表中追加行 |
| **更新进化协议** | 编辑 `agent/evolution-protocol.md` → 同步检查里程碑表 |
| **重建 symlink** | 管理员 PowerShell 执行 `New-Item -ItemType SymbolicLink`（见上方命令） |
| **新增智能体** | 在 `agent/` 下创建 `.md` 文件 + 创建 symlink + 配置 frontmatter |
| **信任进阶** | 检查 `evolution-protocol.md` 里程碑 → 提议 → 用户确认后更新 `aIk-agent.md` 等级字段 |
| **文件保护** | 修改后执行 `git add grimoire-agent/` 暂存到本地 |

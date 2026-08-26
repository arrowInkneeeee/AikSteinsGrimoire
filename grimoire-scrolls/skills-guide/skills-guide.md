# Skills 从入门到精通

> 来源：微信公众号「朱昆鹏AI手记」系列文章  
> 作者：朱昆鹏mm  
> 原文地址：
> - [上篇](https://mp.weixin.qq.com/s/VJkbxcg5ZpSQ3qlOAFAVGQ)
> - [中篇](https://mp.weixin.qq.com/s/xfWhdGMazSL_Jw-JpPMH0Q)
> - [下篇](https://mp.weixin.qq.com/s/5ultoRIPrMWZ166563q6Ww)

---

## 一、概述

Skills 是 Claude Code（及 Codex、Cursor 等平台）支持的一种 AI 技能扩展机制，本质上是**AI 可以随时查阅的说明文档**。当 AI 判断当前任务需要某个 Skill 时，会按需加载该 Skill 的内容到上下文中，从而提升特定任务下的输出质量。

Skills 的诞生时间线：
- 2025 年 10 月 16 日：Anthropics 最早推出 Skills 概念
- 2025 年 12 月 18 日：Anthropics 正式将 Skills 定义为 AI 开放标准

---

## 二、Skills 的核心特性

### 2.1 按需加载机制

Skills 与 CLAUDE.md 的最大区别在于加载方式：

| 特性 | CLAUDE.md | Skills |
|------|-----------|--------|
| 加载时机 | 会话启动时全量注入 | 按需加载 |
| 元数据 | 无 | name + description 始终可见 |
| 正文内容 | 全部进入上下文 | 仅调用时加载 |
| 目录引用 | 内容拼接到上下文 | 需要时才加载 |

元数据（name / description）在对话开始时就加载到 AI 上下文中，但正文内容只会在 Skill 被调用时才加载。这避免了大量 Skills 撑爆上下文的问题。

### 2.2 支持脚本执行

SKILL.md 中可以引用脚本，且脚本**不消耗 token**——脚本不会加载到上下文中，而是直接执行并获取结果。

### 2.3 触发方式

1. **自动触发**：AI 在每轮对话开始时，将全局和项目级的 Skills 元数据加载，自主判断是否需要调用某个 Skill
2. **手动触发**：通过斜杠指令（`/<skill-name>`）手动触发
3. **安全控制**：可配置 `disable-model-invocation: true`，使 Skill 只能手动触发，不会被自动调用

---

## 三、Skills 的构成

### 3.1 存放位置

Skills 支持多个层级，按优先级和作用范围划分：

| 层级 | 路径 | 作用范围 |
|------|------|----------|
| 项目级 | `{project}/.claude/skills/<skill-name>/SKILL.md` | 仅当前项目可用 |
| 全局级 | `~/.claude/skills/<skill-name>/SKILL.md` | 所有项目可用 |
| managed | `managed path/.claude/skills/` | 管理级共享 |
| plugin | 插件目录 | 插件自带 |
| bundled | Claude Code 内置 | 内置技能 |
| mcp | MCP Server 暴露 | 远程 prompt 型技能 |
| legacy | `.claude/commands/` | 旧版兼容 |

### 3.2 SKILL.md 标准格式

```yaml
---
name: skills-demo
description: 这是一个演示 skills 的 demo
disable-model-invocation: true   # 可选：禁止自动触发
---

# skills demo

## 后端
后端相关的内容请阅读 `notes-1.md`

## 数据库
数据库相关的内容请阅读 `notes-2.md`
```

**格式要求**：
- 必须使用目录格式：`.claude/skills/<skill-name>/SKILL.md`
- 单个 `.md` 文件直接放在 skills 目录下**不会被加载**
- 必须包含 `name` 和 `description` 两项元数据

### 3.3 内容引用规则

- SKILL.md 中的引用（如 `notes-1.md`）**不会自动拼接到上下文**，只有在需要时才加载
- 配套文件（脚本、模板、示例）可以放在 Skill 目录下，通过 base directory 机制访问

---

## 四、Skills 的加载与执行流程

```
启动 Claude Code
  ↓
初始化内置 Skills
  ↓
扫描 managed / user / project / plugin / bundled / mcp / legacy 中的 Skills
  ↓
解析 SKILL.md frontmatter
  ↓
注册成 prompt command
  ↓
每轮对话通过 system-reminder 暴露"技能清单"
  ↓
模型判断某个 Skill 匹配任务
  ↓
调用 Skill 工具
  ↓
读取并展开完整 SKILL.md
  ↓
处理参数、变量、!`command` 动态执行
  ↓
把 Skill 正文作为 meta user message 注入当前对话
  ↓
模型按 Skill 指令继续执行
```

### 4.1 动态发现机制

Skills 不仅会在启动时扫描，当模型操作深层目录中的文件时，还会**沿文件路径向上动态查找** `.claude/skills` 目录，实现条件激活。

### 4.2 Skill 展开处理

Skill 被调用后，正文会经过以下处理：
- 注入 `Base directory for this skill: /path/to/skill`
- 替换 `$ARGUMENTS` 和具名参数
- 替换 `${CLAUDE_SKILL_DIR}` 和 `${CLAUDE_SESSION_ID}`
- 执行 `!command` 动态 shell 命令（MCP Skills 不支持此功能）

### 4.3 上下文保存机制

已调用的 Skills 会被记录到 `invokedSkills`，并在上下文压缩（compact）后创建 `invoked_skills` attachment，确保 compact 后模型仍能遵守已调用 Skill 的规则。

---

## 五、源码中的硬性限制

### 5.1 目录格式限制

`.claude/skills/` 目录下**不支持单个 `.md` 文件**：
- ✅ `.claude/skills/review/SKILL.md`
- ❌ `.claude/skills/review.md`

### 5.2 上下文预算限制

| 限制项 | 默认值 | 说明 |
|--------|--------|------|
| 技能清单预算 | 上下文窗口的 1% | 约 8000 字符（200k tokens 场景） |
| 单个描述上限 | 250 字符 | `description + when_to_use` 超过则截断 |

### 5.3 MCP Skills 安全限制

MCP Skills 不执行动态 shell 命令（`!command`），因为远程来源的 Markdown 不被信任。

---

## 六、实践建议

1. **Skill 命名**：使用有意义的 `name` 和简洁的 `description`，便于 AI 判断是否匹配
2. **目录组织**：将 Skill 的配套文件（脚本、模板、参考文档）放在同一目录下
3. **按需拆分**：参考文件拆分到独立 `.md` 文件，通过引用方式加载，节省 token
4. **安全控制**：高风险 Skill（如部署、删除等操作）配置 `disable-model-invocation: true`
5. **元数据精炼**：description 控制在 250 字符以内，避免被截断

---

## 七、参考资源

- 原文系列：微信公众号「朱昆鹏AI手记」
- 源码参考：[NanmiCoder/cc-haha](https://github.com/NanmiCoder/cc-haha)（Claude Code 泄露源码）
- 官方定义：[Anthropics Agent Skills 公告](https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills)

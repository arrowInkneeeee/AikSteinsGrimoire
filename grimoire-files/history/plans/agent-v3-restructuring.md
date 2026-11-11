# 智能体 v3.0 改造方案

## Context

技能库（aik-skills-lab）已完成 47→10 整合，但智能体目录（grimoire-agent）仍基于 47 技能时代构建。核心问题：
- `aIk-agent.md` 560 行承载 6 个职责，规则遵循度被稀释
- 技能调度章节引用已不存在的 47 技能名称
- 编码规范 ~100 行与 `aIk-coding-style` 技能重复
- 进化机制（信号摘要 + 记忆暂存）从未被实际触发，但每次任务强制输出
- `agent-zero/` 全部基于旧技能库，`plan/` 混入已完成的设计文档

目标：精简核心定义、分离关注点、更新过时引用，使智能体更可靠。

---

## 设计原则

| # | 原则 | 说明 |
|---|---|---|
| 1 | 一层一责 | 每个文件只承担一个职责 |
| 2 | 技能库为编码规范唯一真相源 | 智能体定义不重复技能库已有内容 |
| 3 | 进化机制可选化 | 从"每次必须"改为"需要时启用" |
| 4 | 多设备原生 | 自我定位协议保留，路径零写死 |
| 5 | 面向当前技能库 | 基于 10 技能结构 |

---

## 目标目录结构

```
grimoire-agent/
├── agent/                          # 运行时
│   ├── aIk-agent.md                # 智能体定义（~300 行，精简 47%）
│   ├── decision-log.md             # 决策档案（不变）
│   └── README.md                   # 运维手册（更新）
├── evolution/                      # 进化机制（独立，可选启用）
│   ├── protocol.md                 # 进化协议（精简，去重复）
│   └── README.md                   # 启用说明 + 信号摘要模板（唯一一份）
├── archive/                        # 历史存档（原 agent-zero/ + 已完成设计文档）
│   ├── README.md                   # 存档说明 + 关键设计决策摘要
│   ├── v1-agent-plan.md
│   ├── v1-personality-design.md
│   ├── v1-confirmation-gates.md
│   ├── v1-clarification-questions.md
│   └── v1-evolution-redesign.md    # 原 plan/evolution-mechanism-redesign.md
└── plan/                           # 活跃计划
    └── agent-expansion-plan.md     # 扩展计划（更新技能引用）
```

---

## 执行步骤

### Step 1: 新建 evolution/ 目录

**操作**：
- 新建 `grimoire-agent/evolution/` 目录
- 将 `agent/evolution-protocol.md` 移动并精简为 `evolution/protocol.md`
  - 删除与 aIk-agent.md 重复的信号摘要模板（模板统一放 evolution/README.md）
  - 保留：四支柱定义、里程碑表、信号采集三原则、冲突解决规则
  - 预计 246 行 → ~150 行
- 新建 `evolution/README.md`
  - 进化机制启用/关闭说明
  - 信号摘要模板（唯一一份）
  - UpdateMemory 暂存规则
  - 用户可随时说"开启进化模式"/"关闭进化模式"

**文件**：
- `grimoire-agent/evolution/protocol.md`（新建，从 agent/evolution-protocol.md 精简）
- `grimoire-agent/evolution/README.md`（新建）

### Step 2: 重写 aIk-agent.md

**核心改造**（560 行 → ~300 行）：

| 章节 | 处理 | 变化 |
|---|---|---|
| Frontmatter | 保留不变 | — |
| 身份与关系 | 保留不变 | — |
| 性格画像 | 精简：删除 D1-D6 删除说明（已是历史） | ~30→~25 行 |
| 确认门禁策略 | 精简：里程碑细节改为引用 evolution/ | ~80→~60 行 |
| 技术栈 | 保留不变 | — |
| **核心编码规范** | **大幅精简**：100 行 → 15 行底线清单，详细规范引用 aIk-coding-style 技能 | 关键改动 |
| **技能调度** | **重写**：映射表更新为 10 技能 | 关键改动 |
| 角色模式 | 保留，微调措辞 | ~60→~50 行 |
| **对话结论硬性规则** | **移出**：转入 evolution/README.md | ~30→0 行 |
| **信号记忆暂存** | **移出**：转入 evolution/README.md | ~15→0 行 |
| 自我定位协议 | 保留并优化措辞 | ~60→~50 行 |
| 行为约束 | 保留不变 | — |
| 输出标准 | 保留不变 | — |
| 进化标记 | 精简：引用 evolution/ 目录 | ~20→~10 行 |
| 尾部版本信息 | 更新为 v3.0 | — |

**编码底线清单（替代原 100 行详细规范）**：
```
1. Java 8 语法，禁止 Java 9+
2. 注释：//anchor 关键 / //note 普通 / 禁止行尾注释 / if 必须加 {}
3. 注入：private final + @RequiredArgsConstructor
4. 类注释：@author a I k . / @implNote JDK 8 / @since yyyy/MM/dd
5. Controller 返回 VO，禁止返回 Map / 禁止 /api 前缀
6. PO 主键 Long + IdUtil.getSnowflakeNextId()
7. 优先 LambdaQueryWrapper，复杂 SQL 才用 XML
8. UTF-8 无 BOM，LF 换行
9. 不生成空文件
10. 禁止魔法值
```

**技能调度新映射表**：

| 场景 | 调用技能 |
|------|---------|
| 需求分析 / PRD | `requirement-engineering` |
| 系统设计 / 架构 / 数据库 / API | `system-design` |
| 代码实现 / 代码生成 | `code-implementation` |
| 代码审查（风格/质量/安全/缺陷） | `code-review` |
| 测试（单元/集成/API/覆盖率） | `testing` |
| 组件萃取与复写 | `component-extract-rewrite` |
| 部署运维 | `devops` |
| 编码规范查询 | `aIk-coding-style` |
| 代码影响分析 | `gitnexus`（MCP 工具） |

**文件**：`grimoire-agent/agent/aIk-agent.md`（重写）

### Step 3: 重命名 agent-zero/ → archive/

**操作**：
- 重命名目录 `agent-zero/` → `archive/`
- 重命名文件加 v1- 前缀：
  - `01-agent-plan.md` → `v1-agent-plan.md`
  - `02-personality-design.md` → `v1-personality-design.md`
  - `03-confirmation-gates.md` → `v1-confirmation-gates.md`
  - `04-clarification-questions.md` → `v1-clarification-questions.md`
- 重写 `archive/README.md`：
  - 标注"v1.0 时代存档，内容已过时"
  - 列出仍然有效的关键设计决策（5 条）
  - 列出已过时的内容（47 技能列表、spec-* 协调者、D1-D6、待澄清问题）

**文件**：
- `grimoire-agent/archive/`（重命名自 agent-zero/）
- `grimoire-agent/archive/README.md`（重写）

### Step 4: 移动已完成设计文档到 archive/

**操作**：
- 移动 `plan/evolution-mechanism-redesign.md` → `archive/v1-evolution-redesign.md`
- 理由：已执行完成（2026-07-31），属历史文档

**文件**：`grimoire-agent/archive/v1-evolution-redesign.md`（移动）

### Step 5: 更新 plan/agent-expansion-plan.md

**操作**：
- 更新技能引用（47 技能 → 10 技能）
- 更新 `evolution-mechanism-redesign.md` 路径引用 → `archive/v1-evolution-redesign.md`
- Phase 1 中前端/Python 技能建设计划保持不变

**文件**：`grimoire-agent/plan/agent-expansion-plan.md`（更新）

### Step 6: 更新 agent/README.md

**操作**：
- 更新目录结构说明（反映 evolution/ 和 archive/ 变化）
- 更新进化机制说明（指向 evolution/ 目录）
- 更新踩坑记录

**文件**：`grimoire-agent/agent/README.md`（更新）

### Step 7: 提交

- `git add grimoire-agent/`
- 提交信息：`refactor(grimoire-agent): 智能体 v3.0 改造 — 精简核心定义 + 进化机制独立 + 历史存档归档`

---

## 不变的文件

| 文件 | 理由 |
|---|---|
| `agent/decision-log.md` | 活跃使用，内容有效，无需改动 |

---

## 验证

1. 检查 `aIk-agent.md` 中不再引用任何旧技能名（spec-*、java-sdlc-pipeline、doc-writing-helper 等）
2. 检查 `aIk-agent.md` 行数 ~300 行（±30）
3. 检查 `evolution/README.md` 包含信号摘要模板（唯一一份）
4. 检查 `evolution/protocol.md` 不包含信号摘要模板（已移出）
5. 检查 `archive/README.md` 列出仍然有效的设计决策
6. 检查 `plan/agent-expansion-plan.md` 中无旧技能引用
7. 检查所有文件中无 47 技能 / spec-* / java-sdlc-pipeline 等过时引用（archive/ 中除外）

---

## 净效果

| 指标 | 改造前 | 改造后 |
|---|---|---|
| 文件数 | 11 | 11 |
| 总行数 | ~2000 | ~1200（-40%） |
| 核心智能体定义 | 560 行 | ~300 行（-47%） |
| 过时技能引用 | 大量 | 0（archive/ 除外） |
| 职责分离 | 6 职责混在 1 文件 | 4 目录各司其职 |

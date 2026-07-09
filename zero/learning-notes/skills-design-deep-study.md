# Agent Skills 设计理念深度学习总结

> 研读来源：《Agent Skill规范、构建与设计模式》(阿里技术) + 《Agent Skills 入门和精通》(萌萌哒草头将军)  
> 日期：2026-05-13  
> 目的：为后续 Skills 库的优化改造提供理论指导和实践框架  

---

## 一、核心认知：Skill 不是 Prompt

### 1.1 本质差异

| 维度 | 传统 Prompt | Agent Skill |
|------|-----------|-------------|
| 形态 | 一次性指令文本 | 结构化文件夹 (SKILL.md + scripts/ + references/ + assets/) |
| 加载 | 全部注入上下文 | L1→L2→L3 渐进式加载 |
| 复用 | 复制粘贴 | 文件系统级模块化 |
| 边界 | 模糊 | 明确的任务、工具、流程、输出边界 |
| 可测试性 | 靠人工感受 | 可量化评测、可回归对比 |

**关键洞察**：Skill 是把机器学习工程纪律引入提示词工程——泛化、防过拟合、可测试、可回归。

### 1.2 标准目录结构

```
my-skill/
├── SKILL.md        # 核心：元数据(YAML frontmatter) + 操作指令（必填）
├── scripts/        # 自包含执行脚本，输出进入上下文，代码不占 token
├── references/     # 按需加载的技术文档、规范文件
└── assets/         # 模板、静态资源
```

### 1.3 渐进式加载机制（三层架构）

这是 Skills 体系最核心的设计：

```
L1 (常驻): 仅 name + description → 用于意图匹配，token 消耗极低
    ↓ 模型决策触发
L2 (激活): 加载完整 SKILL.md 正文 → 建议控制在 5000 词以内
    ↓ 指令引用
L3 (按需): 动态读取 references/ 或执行 scripts/ → 结果进上下文，源码不进
```

**效果**：多技能共存时，初始上下文负载下降约 90%。一套 30 个技能的库，启动时只占用 ~3KB 的元数据。

### 1.4 触发逻辑

- **模型驱动激活**（非关键词/正则匹配）：依赖 LLM 的意图识别能力
- **description 是触发入口**：必须用祈使句式，聚焦"用户要做什么"而非"技能内部怎么做"
- **覆盖表达变体**：同一意图可能有多种说法，description 需要覆盖

**禁忌**：description 只陈述触发条件，**严禁概括执行流程**——否则模型会跳过正文直接按摘要"走捷径"。

---

## 二、SKILL.md 规范要点

### 2.1 元数据规范 (YAML Frontmatter)

```yaml
---
name: my-skill-name          # 全小写+连字符，与父目录名一致，禁止大写或连续连字符
description: >-              # 祈使句式，描述用户场景和触发时机，≤1024 字符
  当用户需要做 X 时使用此技能。
  覆盖场景包括 A、B、C。
  本技能将引导用户完成...
license: MIT                 # 可选
metadata:                    # 可选扩展
  author: xxx
  version: "1.0"
allowed-tools:               # 可选：工具白名单
  - Bash
  - Read
---
```

### 2.2 正文编写原则

1. **解释规则成因，而非堆砌禁令**  
   ❌ "不要用 @Autowired"  
   ✅ "使用 private final + @RequiredArgsConstructor，因为构造器注入可保证依赖不可变且便于单元测试"

2. **泛化防过拟合**  
   ❌ "当用户说'创建用户模块'时..."（绑定具体场景）  
   ✅ "当用户要求创建新的业务模块时..."（抽象到模式层面）

3. **保持极度精简**  
   - 依据任务的脆弱性动态调整约束自由度
   - 简单任务给更多自主权，高风险任务收紧约束

4. **复杂流程嵌入检查清单**  
   - 带进度标记 `[ ] → [x]` 的校验步骤
   - 配合验证脚本，精准报错定位

---

## 三、RED-GREEN-REFACTOR 开发纪律

这是从 TDD（测试驱动开发）借来的核心理念，应用于 Skill 开发：

```
RED    → 在无技能的高压场景下暴露模型的违规行为
GREEN  → 编写最小化指令来拦截这些漏洞
REFACTOR → 持续封堵模型的"合理化推脱"路径
```

### 3.1 四类测试策略

| 类型 | 测试方法 | 适用场景 |
|------|---------|---------|
| **纪律型** | 时间 + 疲劳双重施压，验证服从性 | 代码规范、格式约束 |
| **技术型** | 新场景迁移能力测试 | API 调用、框架使用 |
| **思维型** | 评估模式适用边界 | 架构设计、方案选型 |
| **资料型** | 信息检索与落地准确度 | 文档生成、知识问答 |

### 3.2 双人迭代模式

推荐 **专家设计 + 独立测试者执行** 的分工：
- 设计者不参与测试，避免认知盲区
- 测试者按 RED-GREEN-REFACTOR 循环独立验证
- 每次迭代输出量化对比数据

---

## 四、三 Agent 评估架构

这是 Skill-Creator 工具链的核心评估体系：

```
┌──────────────┐    ┌──────────────────┐    ┌──────────────┐
│  Scorer      │    │  Blind           │    │  Analyzer    │
│  (评分者)     │    │  Comparator      │    │  (分析者)     │
│              │    │  (盲比较者)        │    │              │
├──────────────┤    ├──────────────────┤    ├──────────────┤
│ 核查断言真实性 │    │ 剥离来源信息      │    │ 事后归因      │
│ 防表面合规    │    │ 内容准确度评分    │    │ 模式识别      │
│ 掩盖实质失败  │    │ 结构可用性评分    │    │ 基准统计      │
│              │    │                  │    │ 优先级排序    │
└──────────────┘    └──────────────────┘    └──────────────┘
```

**核心思想**：自动化评估不能只看"表面合规"（如格式正确），更要核查"实质正确"（如逻辑错误、边界遗漏）。盲比较消除品牌偏差，分析者提供可操作的改进清单。

---

## 五、Google 五种技能设计模式

这是整个文章中最具实操价值的部分：

### 模式 1: Tool Wrapper（工具封装器）

```
场景：封装框架规范、API 使用惯例
机制：按需加载专家知识
```

**示例**：将 Spring Boot 项目结构规范封装为技能，当用户创建新模块时自动注入目录约定。

**对应当前技能库**：`aIk-coding-style`、`config-manager`、`log-configurator`

### 模式 2: Generator（生成器）

```
场景：需要严格一致的输出格式
机制：模板填充 + 主动反问澄清歧义
```

**示例**：API 文档生成器，用固定模板填充 + 对模糊参数主动追问。

**对应当前技能库**：`api-doc-generator`、`code-generator`、`db-migration-generator`、`test-generator`

### 模式 3: Reviewer（审查者）

```
场景：质量检查、代码审查
机制：检查清单与执行逻辑解耦，强调问题归因而非单纯报错
```

**关键设计**：不要只说"第 10 行有问题"，要说"第 10 行存在 N+1 查询风险，因为循环内调用了 Mapper 方法，建议改为批量查询"。

**对应当前技能库**：`code-quality-reviewer`、`code-security-reviewer`、`code-style-reviewer`、`design-review-checker`

### 模式 4: Inversion（反转模式）

```
场景：需求不明确时的信息采集
机制：由模型先行结构化采集需求，而非被动等待用户给全信息
```

**示例**：用户说"我要加个登录"，技能主动追问：认证方式？Session 还是 JWT？是否需要刷新令牌？多设备登录策略？

**对应当前技能库**：`requirement-clarifier`、`requirement-extractor`、`feasibility-checker`

### 模式 5: Pipeline（流水线模式）

```
场景：多阶段交付、需要质量门禁
机制：强制线性分步 + 人工确认卡点，严禁跳步
```

**示例**：
```
[需求分析] → [设计评审] → [代码生成] → [代码审查] → [测试生成] → [部署配置]
     ↓            ↓            ↓            ↓            ↓            ↓
  人工确认     人工确认      自动执行     自动审查     自动生成     人工确认
```

**对应当前技能库**：`spec-designer`、`spec-implementer`、`spec-qa-analyser`、`spec-devops`

### 模式选型决策

| 面临的问题 | 推荐模式 |
|-----------|---------|
| 知识需要沉淀复用 | Tool Wrapper |
| 输出格式需要严格一致 | Generator |
| 生成结果需要质量把关 | Reviewer |
| 需求信息不完整 | Inversion |
| 任务需要多阶段交付 | Pipeline |

**组合使用**：模式之间可以灵活嵌套。例如 Pipeline 末端叠加 Reviewer（自动审查），或 Inversion 完成后接入 Generator（需求采集完后自动生成代码）。

---

## 六、对当前技能库的诊断分析

### 6.1 当前技能库全景

```
当前 30+ 技能，覆盖 Java SDLC 全流程：

需求层:  requirement-extractor, requirement-clarifier, user-story-generator,
         acceptance-criteria-writer, feasibility-checker, conflict-detector,
         priority-assessor, spec-requirement-analyser

设计层:  architecture-designer, database-designer, process-designer,
         tech-solution-selector, spec-designer, api-designer,
         design-review-checker

开发层:  spec-implementer, code-generator, code-implementer,
         aIk-coding-style

审查层:  code-quality-reviewer, code-security-reviewer, code-style-reviewer,
         bug-pattern-analyzer

测试层:  spec-qa-analyser, unit-test-generator, integration-test-generator,
         api-test-generator, test-generator, test-data-manager,
         coverage-reporter

运维层:  spec-devops, package-builder, deploy-script-generator,
         config-manager, log-configurator, health-check-designer,
         troubleshooting-guide, db-migration-generator

工具类:  doc-writing-helper, api-doc-generator, gitnexus
```

### 6.2 潜在改进方向

1. **description 精准度审查**
   - 部分技能的 description 可能过于简短或模糊
   - 需要确保覆盖用户表达变体
   - 检查是否存在"暴露内部流程"的 description

2. **渐进式加载优化**
   - 检查哪些技能的 SKILL.md 过长，应将详细文档移至 references/
   - 脚本逻辑应移入 scripts/，利用 L3 机制实现"代码不占 token"

3. **Pipeline 串联缺失**
   - 当前技能分散独立，缺少顶层 Pipeline 编排
   - `spec-*` 系列已具备 Pipeline 雏形，但可以进一步强化卡点

4. **Reviewer 归因深度**
   - 审查类技能需要从"报错"升级到"归因+建议"
   - 代码审查需要量化指标（如引入的 N+1 风险数、空指针风险数）

5. **RED-GREEN-REFACTOR 循环未建立**
   - 缺少 Skill 本身的测试验证机制
   - 没有对比基准数据（有技能 vs 无技能的产出质量差异）

---

## 七、后续改造行动指南

### 第一阶段：规范对齐

- [ ] 逐一审查所有 SKILL.md 的 YAML frontmatter 合规性
- [ ] 优化 description 为祈使句式，覆盖表达变体
- [ ] 确保 name 全小写+连字符，与目录名一致
- [ ] 过大的 SKILL.md 拆分到 references/

### 第二阶段：架构升级

- [ ] 引入 Pipeline 顶层编排技能，串联 SDLC 全流程
- [ ] 在关键节点嵌入 Reviewer 质量门禁
- [ ] 审查类技能增加"归因分析"能力

### 第三阶段：质量体系

- [ ] 建立 Skill 测试用例（有/无技能对照组）
- [ ] 引入量化评估指标
- [ ] 实施 RED-GREEN-REFACTOR 迭代周期

### 第四阶段：持续优化

- [ ] 基于实际使用数据调整 description 匹配率
- [ ] 收缩过拟合的指令
- [ ] 提取高频操作为独立 scripts/

---

## 八、关键金句摘录

> "Skill 不是 Prompt——它是围绕任务、工具、流程和输出边界的结构化行为设计。"

> "将机器学习工程纪律引入提示词开发：泛化防过拟合，解释规则成因而非堆砌禁令。"

> "description 只陈述触发条件，严禁概括执行流程——防止模型跳过正文直接按摘要走捷径。"

> "先暴露模型的违规借口，再编写最小化指令拦截漏洞，最后持续封堵合理化推脱路径。"

> "不要只报错——要归因。审查者的价值不在于发现表面问题，而在于揭示问题根因。"

> "流水线的每一道卡点都是人工确认——这不是低效，而是对质量的必要敬畏。"

---

*本总结基于阿里技术发布的《Agent Skill规范、构建与设计模式》及腾讯云开发者社区的《Agent Skills 入门和精通》深度研读而作。*

# 组件知识萃取与复写工作流 — 实施计划（终稿）

> 版本: v1.0
> 日期: 2026-05-14
> 状态: 方案定稿，待实施

---

## 上下文

**问题**：在多个项目中开发过有价值的组件和解决方案，但缺少标准化的萃取、文档化和复写流程。现有代码与业务耦合、信息不完整、难以复用。旧的积累项目结构混乱，决定废弃重建。

**关键约束**：
- gitnexus ✅ 已全局安装 v1.6.4，MCP 13 工具就绪 — Phase 1 核心依赖
- 积累项目从零开始，全新 Spring Boot 项目，遵循 aIk-coding-style

**目标**：建立一套标准化工作流，让用户能在源项目中手动指定范围 → 产出结构化组件手册 → 在全新积累项目中按规范复写出脱敏、去业务耦合的代码。

---

## 跨项目操作方式

```
┌─ 对话 1：在源项目 ─────────────────────────┐
│                                              │
│  cd D:\workspace\source-project              │
│  1. "帮我萃取 XxxService 这个分布式锁方案"    │
│     → [自动检查/构建 gitnexus 索引]           │
│     → [spec-component-extractor 执行]        │
│  2. 产出：Component Manual v1.0              │
│     → 保存到本地                              │
│                                              │
└──────────────────────────────────────────────┘
                    │
        Component Manual（桥梁文档，自包含）
                    │
                    ▼
┌─ 对话 2：在积累项目 ─────────────────────────┐
│                                              │
│  cd D:\workspace\accumulation-lab            │
│  1. "基于手册复写为 accumulation-lab 组件"    │
│     → [spec-component-rewriter 执行]         │
│  2. 产出：标准化代码 + 配置 + 测试 + 文档     │
│     → 写入 accumulation-component-{name}/    │
│                                              │
└──────────────────────────────────────────────┘
```

Phase 1 和 Phase 2 可在不同时间、不同项目中分别执行。手册本身即知识资产。

---

## 设计方案：2阶段 + 5个新技能

### Phase 1 — 站点A（萃取+文档化）
由 `spec-component-extractor` 协调，接收用户指定的代码范围，产出结构化 **Component Manual**

### Phase 2 — 站点B（脱敏复写）
由 `spec-component-rewriter` 协调，接收 Component Manual，在目标项目中产出标准化代码 + 配置 + 文档

---

## 新增技能清单（5个）

| 技能 | 类型 | 设计模式 | 阶段 | 职责 |
|------|------|---------|------|------|
| `component-code-analyzer` | Skill | Generator | Phase1 | 深度代码分析：识别架构模式、业务耦合点、敏感数据、依赖关系 |
| `spec-component-extractor` | Sub-agent | Inversion | Phase1 | 协调6个现有技能 + 1个新技能，产出 Component Manual |
| `component-code-rewriter` | Skill | Generator | Phase2 | 基于手册复写脱敏代码，遵循 aIk-coding-style 规范 |
| `spec-component-rewriter` | Sub-agent | Inversion | Phase2 | 协调6个现有技能 + 1个新技能，产出标准化代码交付物 |
| `component-extraction-rewriting-workflow` | Sub-agent | Pipeline | 全流程 | 可选，串联2阶段为完整流水线 |

---

## 实施步骤

### Step 1: 创建 `component-code-analyzer`
- 新建 `c:\Users\arrowInknee\.lingma\skills\component-code-analyzer\SKILL.md`
- L2 核心指令：定义6个分析维度（架构/分层/耦合/敏感/依赖/扩展点）
- L3 参考：`references/coupling-patterns.md` — 常见业务耦合模式识别规则
- **代码获取方式**：使用 gitnexus `query` 语义搜索组件相关流程，`context` 获取核心符号的 360° 视图（调用者/被调用者/所属流程），`cypher` 追踪完整调用链
- 输入：gitnexus 返回的依赖图 + 调用链 + 源文件全文
- 输出：结构化分析报告（含耦合清单、脱敏建议）

### Step 2: 创建 `spec-component-extractor`
- 新建 `c:\Users\arrowInknee\.lingma\skills\spec-component-extractor\SKILL.md`
- 编排 8 步流程：
  ```
  步骤0: 自动索引检查 → npx gitnexus status，无索引或过期则自动 npx gitnexus analyze
  步骤1: gitnexus 语义探索(query+context)
  步骤2: 代码分析
  步骤3: 流程设计
  步骤4: 架构映射
  步骤5: 技术分析
  步骤6: 手册生成
  步骤7: 质量审核
  ```
- 3个人工卡点：范围确认 → 分析报告审核 → 手册审批
- 输出：10章结构化 Component Manual

### Step 3: 创建 `component-code-rewriter`
- 新建 `c:\Users\arrowInknee\.lingma\skills\component-code-rewriter\SKILL.md`
- L2 核心指令：6大脱敏策略（命名/值/数据/依赖/凭证/风格）+ aIk-coding-style 强制检查
- L3 参考：`references/desensitization-patterns.md` — 脱敏模式与替换策略
- 输入：Component Manual + code-generator 生成的骨架
- 输出：标准化、无业务耦合的 Java 代码

### Step 4: 创建 `spec-component-rewriter`
- 新建 `c:\Users\arrowInknee\.lingma\skills\spec-component-rewriter\SKILL.md`
- 编排6步流程：目标确认 → 脱敏设计 → 骨架生成 → 逻辑复写 → 配置处理 → 三层审查 → 文档测试
- 3个人工卡点：目标项目确认 → 脱敏策略审核 → 最终审批

### Step 5: 创建 `component-extraction-rewriting-workflow`（可选）
- 新建 `c:\Users\arrowInknee\.lingma\skills\component-extraction-rewriting-workflow\SKILL.md`
- 引用 java-sdlc-pipeline 的 Pipeline 模式
- 2阶段门禁：手册完整性审核 → 三层审查 + 脱敏审核

### Step 6: 积累项目初始化（一次性工作）
- 新建一个干净 Spring Boot 项目，技术栈：Java 8 + Spring Boot 2.7 + MyBatis-Plus 3.5 + Maven
- 使用 `spec-designer` + `spec-implementer` 技能链创建
- 建立标准包结构和通用组件（Result、BaseEntity、PageDTO、BusinessException）
- 配置 Logback、Actuator
- 后续每个组件作为独立 module 或 package 接入

---

## 积累项目结构设计（一次性工作）

```
accumulation-lab/                        # 积累项目根目录
├── pom.xml                              # 父 POM（Spring Boot 2.7 + MyBatis-Plus 3.5）
├── README.md
│
├── accumulation-common/                 # 通用基础模块
│   ├── pom.xml
│   └── src/main/java/com/xxx/common/
│       ├── base/BaseEntity.java         # 基础实体
│       ├── result/Result.java           # 统一返回
│       ├── page/PageDTO.java            # 分页参数
│       ├── exception/BusinessException.java
│       ├── config/                      # 通用配置
│       └── util/                        # 通用工具类
│
├── accumulation-component-{name}/       # 每个萃取组件一个 module
│   ├── pom.xml
│   ├── README.md                        # 组件使用手册
│   └── src/main/java/com/xxx/component/{name}/
│       ├── config/                      # 组件配置（@ConfigurationProperties）
│       ├── service/                     # 服务接口
│       │   └── impl/                    # 服务实现
│       ├── common/
│       │   ├── dto/                     # 入参 DTO
│       │   ├── vo/                      # 出参 VO
│       │   └── constant/                # 组件常量
│       └── util/                        # 组件工具类
│
└── accumulation-docs/                   # 组件手册归档
    └── {component-name}-manual-v1.0.md  # 每个组件的萃取手册
```

**配置管理**：每个组件 module 有自己的 `application-{component}.yml`，通过 `spring.profiles.include` 按需启用。

---

## 与 SDLC 流程的关系

本工作流与 `java-sdlc-pipeline` 并行，互不干扰：

```
java-sdlc-pipeline                          component-extraction-rewriting-workflow
══════════════════                          ══════════════════════════════════════
Phase 1: 需求分析 ──→ PRD                  Phase 1: 萃取文档化 ──→ Component Manual
    ↓ 门禁 + 人工确认                           ↓ 门禁 (手册完整性审核) + 人工确认
Phase 2: 系统设计 ──→ SDD                  Phase 2: 脱敏复写   ──→ 标准化代码+配置+测试
    ↓ 门禁 + 人工确认
Phase 3: 开发实施 ──→ 代码+测试
    ↓
Phase 4: 测试QA   ──→ 测试报告
    ↓
Phase 5: 部署运维 ──→ 部署方案
```

- 三不原则：不修改现有 SKILL.md，不插入 SDLC 流水线，不改变任何原子技能行为
- 新技能只调用原子技能，不影响其逻辑

---

## 复用的现有技能（15个）

| 技能 | Phase 1 | Phase 2 | 用途 |
|------|---------|---------|------|
| `gitnexus` | ✅ | - | 代码探索：语义搜索(query)、符号360°视图(context)、影响分析(impact)、调用链追踪(cypher) |
| `process-designer` | ✅ | - | 核心流程时序图绘制 |
| `architecture-designer` | ✅ | - | 组件分层映射、包结构规范化 |
| `tech-solution-selector` | ✅ | - | 依赖分析、技术方案梳理 |
| `doc-writing-helper` | ✅ | ✅ | 正式风格文档撰写（手册+使用指南） |
| `design-review-checker` | ✅ | - | 手册完整性审核 |
| `code-generator` | - | ✅ | 按手册生成规范化代码骨架 |
| `config-manager` | - | ✅ | 配置外化、多环境管理 |
| `code-style-reviewer` | - | ✅ | 命名/注释/格式审查 |
| `code-quality-reviewer` | - | ✅ | N+1/空指针/事务审查 |
| `code-security-reviewer` | - | ✅ | SQL注入/权限/残留敏感数据 |
| `unit-test-generator` | - | ✅ | JUnit 5 + Mockito 测试生成 |
| `api-doc-generator` | - | ✅ | API 接口文档（可选） |
| `aIk-coding-style` | ✅ | ✅ | 全局编码规范（全流程引用） |
| `skill-tester` | ✅ | ✅ | RED-GREEN-REFACTOR 质量验证 |

---

## Component Manual 文档模板结构

```
# 组件手册：{组件名称}
## 1. 组件概述（功能/技术特征）
## 2. 架构设计（包结构/分层映射/设计模式）
## 3. 类与接口清单（核心类/扩展点）
## 4. 核心流程（时序图/事务边界/异步处理）
## 5. 配置与依赖（外部/内部依赖清单）
## 6. 业务耦合清单（耦合-ID/位置/类型/脱敏建议）
## 7. 敏感数据清单（敏感-ID/位置/类型/处理方式）
## 8. 脱敏与复写指导（命名映射表/抽象化策略/配置外化）
## 9. 使用指南（快速开始/配置说明/扩展示例）
## 10. 附录（源文件清单/依赖树/变更历史）
```

---

## 数据流转

```
用户指定范围 (文件/包/方法列表 + 粒度)
    ▼
[gitnexus query + context] → 依赖图、调用链、受影响的符号
    ▼
[component-code-analyzer] → 代码分析报告（架构+耦合+敏感+依赖）
    ▼
[process-designer + architecture-designer + tech-solution-selector]
    ▼
[doc-writing-helper] → Component Manual（10章结构化文档）
    ▼  ← 人工确认，进入 Phase 2
[积累项目初始化] → 全新 Spring Boot 项目，遵循 aIk-coding-style
    ▼
[code-generator] → 规范化代码骨架
    ▼
[component-code-rewriter] → 脱敏去耦的完整代码
    ▼
[config-manager + 三层审查 + 测试/文档生成]
    ▼
完整交付物：代码 + 配置 + 测试 + 文档
```

---

## 需要修改的文件

无。所有工作为新增技能，不修改任何现有文件。

## 新增文件清单

```
component-code-analyzer/
  SKILL.md
  references/coupling-patterns.md
component-code-rewriter/
  SKILL.md
  references/desensitization-patterns.md
spec-component-extractor/
  SKILL.md
spec-component-rewriter/
  SKILL.md
component-extraction-rewriting-workflow/
  SKILL.md
```

---

## 验证方案

1. **Phase 1 端到端测试**：找一个已有项目中的组件，用 `spec-component-extractor` 执行完整萃取流程，验证产出的 Component Manual 是否覆盖10章、耦合清单是否准确、脱敏指导是否可行
2. **Phase 2 端到端测试**：用 Phase 1 产出的手册，在积累项目中执行 `spec-component-rewriter`，验证复写代码能否编译通过、三层审查是否全部通过、残留业务信息是否清零
3. **RED-GREEN-REFACTOR**（skill-tester）：定义测试场景，验证无技能时的违规行为 → 加载技能后的行为约束效果 → 迭代收紧指令
4. **代码编译验证**：复写后的代码在目标项目中 `mvn compile` 通过，`mvn test` 所有单元测试通过

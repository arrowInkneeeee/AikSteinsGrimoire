# Java Spring Boot 技能模板库

## 简介

一套完整的软件开发生命周期（SDLC）技能库，专为 Java 8 + Spring Boot + MyBatis-Plus + Lombok 技术栈设计。

包含 8 大类别共 **48 个技能**，覆盖需求分析、系统设计、开发实施、测试质量、部署运维全流程。新增 `java-sdlc-pipeline` 统一流水线入口、`component-extraction-rewriting-workflow` 组件萃取复写工作流和 `skill-tester` 技能质量体系，遵循 Agent Skills L3 渐进式加载最佳实践。

## 技能库结构

```
.skills/
├── Pipeline 统一入口（2个技能）
│   ├── java-sdlc-pipeline               # 5阶段SDLC流水线，含质量门禁与人工确认
│   └── component-extraction-rewriting-workflow  # 组件萃取复写流水线（2阶段+人工确认）
│
├── 组件萃取复写（5个技能）
│   ├── component-code-analyzer           # 6维度代码分析 + 业务耦合检测（L3: 161行核心 + references）
│   ├── spec-component-extractor          # 萃取协调者（gitnexus auto-index + 人工确认）
│   ├── component-code-rewriter           # 6大脱敏策略 + aIk-coding-style 规范检查（L3: 211行核心 + references）
│   ├── spec-component-rewriter           # 复写协调者（3层代码审查 + 统一编码风格）
│   └── component-extraction-rewriting-workflow  # Pipeline 统一入口
│
├── 规范与文档（2个技能）
│   ├── aIk-coding-style                  # aIk个人Java后端编码规范（L3: 203行核心 + 18 references）
│   └── doc-writing-helper                # 文档编写助手（周报、项目文档、润色续写）
│
├── 需求分析阶段（8个技能）
│   ├── requirement-extractor             # 需求提取
│   ├── requirement-clarifier             # 需求澄清
│   ├── conflict-detector                 # 冲突检测
│   ├── user-story-generator              # 用户故事生成
│   ├── acceptance-criteria-writer        # 验收标准编写
│   ├── feasibility-checker               # 可行性检查
│   ├── priority-assessor                 # 优先级评估
│   └── spec-requirement-analyser         # 需求分析协调者
│
├── 系统设计阶段（7个技能）
│   ├── architecture-designer             # 架构设计
│   ├── database-designer                 # 数据库设计
│   ├── api-designer                      # API设计
│   ├── process-designer                  # 流程设计
│   ├── tech-solution-selector            # 技术方案选择
│   ├── design-review-checker             # 设计评审检查
│   └── spec-designer                     # 设计协调者
│
├── 开发实施阶段（8个技能）
│   ├── code-generator                    # 代码骨架生成
│   ├── code-implementer                  # 业务逻辑实现
│   ├── code-style-reviewer               # 代码风格审查
│   ├── code-quality-reviewer             # 代码质量审查
│   ├── code-security-reviewer            # 代码安全审查
│   ├── db-migration-generator            # 数据库迁移生成
│   ├── api-doc-generator                 # API文档生成
│   └── spec-implementer                  # 开发协调者
│
├── 测试质量阶段（8个技能）
│   ├── skill-tester                      # 技能质量测试（RED-GREEN-REFACTOR框架）
│   ├── unit-test-generator               # 单元测试生成（含Controller/Mapper测试）
│   ├── integration-test-generator        # 集成测试生成
│   ├── api-test-generator                # API测试文档生成
│   ├── test-data-manager                 # 测试数据管理
│   ├── coverage-reporter                 # 覆盖率报告
│   ├── bug-pattern-analyzer              # 缺陷模式分析
│   └── spec-qa-analyser                  # 测试协调者
│
└── 部署运维与工具集成（9个技能）
    ├── gitnexus                          # GitNexus代码智能套件（探索/调试/影响分析/PR审查）
    ├── handoff-bundle                    # 跨Agent任务交接包（HANDOFF.md+SHA-256校验+持久化资产）
    ├── package-builder                   # 打包构建
    ├── deploy-script-generator           # 部署脚本生成
    ├── config-manager                    # 配置管理
    ├── health-check-designer             # 健康检查设计
    ├── log-configurator                  # 日志配置
    ├── troubleshooting-guide             # 故障排查指南
    └── spec-devops                       # 运维协调者
```

### 设计模式分布

| 设计模式 | 数量 | 代表技能 |
|---------|------|---------|
| **Pipeline**（流水线） | 2 | java-sdlc-pipeline, component-extraction-rewriting-workflow |
| **Generator**（生成器） | 21 | unit-test-generator, code-generator, component-code-rewriter 等 |
| **Reviewer**（审查者） | 9 | code-style-reviewer, code-quality-reviewer, code-security-reviewer 等 |
| **Inversion**（反转控制/协调者） | 7 | spec-requirement-analyser, spec-designer, spec-component-extractor 等 |
| **Tool Wrapper**（工具封装） | 9 | gitnexus, handoff-bundle, config-manager, log-configurator 等 |

### L3 渐进式加载架构

所有技能遵循 Agent Skills 三级加载规范：

| 层级 | 内容 | 说明 |
|------|------|------|
| **L1**（元数据） | YAML frontmatter | name, description, type, version — 零成本加载 |
| **L2**（核心指令） | SKILL.md 正文 | 核心工作流，≤5000词，始终在上下文 |
| **L3**（按需加载） | references/, assets/, scripts/ | 参考文档、模板、脚本，仅在需要时加载 |

**示例：** `aIk-coding-style` 从 1,916 行压缩为 203 行核心（L2），18 个参考文件按需加载（L3）。

## 使用方法

### 方式一：Pipeline 统一入口（推荐）

使用 `java-sdlc-pipeline` 一键启动全流程：

```
启动Java SDLC流水线，开发订单管理模块
```

流水线将按 5 个阶段依次执行，每个阶段通过质量门禁后方可进入下一阶段：
1. 需求分析 → 2. 系统设计 → 3. 开发实施 → 4. 测试验证 → 5. 部署运维

使用 `component-extraction-rewriting-workflow` 跨项目萃取复写组件：

```
启动组件萃取复写流水线，从 XX 项目提取 XX 模块
```

流水线将按 2 个阶段执行（跨项目操作）：
1. **Phase 1（源项目）**：gitnexus 自动建索引 → 6维度代码分析 → 业务耦合检测 → 产出 Component Manual
2. **Phase 2（积累项目）**：基于 Manual → 6大脱敏策略重写 → aIk-coding-style 规范检查 → 3层代码审查

### 方式二：直接自然语言描述

在 Lingma 对话中直接描述需求，系统自动匹配最佳技能：

```
我需要开发一个订单管理系统，包含下单、支付、查询功能
```

或针对特定任务：

```
为 OrderService 生成单元测试
审查 OrderController 的代码质量
生成订单模块的API文档
```

### 方式三：全局/项目级安装

**全局使用（已配置）：**
```
C:\Users\arrowInknee\.lingma\skills\
```

**项目级符号链接（推荐，自动同步）：**
```bash
# Windows CMD（管理员权限）
mklink /J .skills C:\Users\arrowInknee\.lingma\skills

# Windows PowerShell（管理员权限）
cmd /c mklink /J .skills C:\Users\arrowInknee\.lingma\skills

# 或使用 PowerShell 原生命令
New-Item -ItemType Junction -Path ".skills" -Target "C:\Users\arrowInknee\.lingma\skills"

# Mac/Linux
ln -s ~/.lingma/skills .skills
```

**项目级复制（完全隔离）：**
```bash
xcopy /E /I "C:\Users\arrowInknee\.lingma\skills" ".skills"
```

## 技能质量体系

### skill-tester：RED-GREEN-REFACTOR 框架

每个技能均可通过 `skill-tester` 进行质量验证：

```
测试 aIk-coding-style 技能
```

测试流程：
- **RED**：定义预期行为（测试场景）
- **GREEN**：执行技能，验证输出是否符合预期
- **REFACTOR**：根据测试结果优化技能

支持 4 种测试策略：纪律型（命名/格式）、技术型（代码生成正确性）、思维型（设计决策合理性）、资料型（参考文档完整性）。

## 按阶段使用指南

### Pipeline 统一入口

**SDLC 流水线：**
```
启动Java SDLC流水线，开发XX模块

流水线自动协调：
→ 需求分析（spec-requirement-analyser + 原子技能）
→ 系统设计（spec-designer + 原子技能）
→ 开发实施（spec-implementer → code-generator → code-implementer → code-quality-reviewer）
→ 测试验证（spec-qa-analyser → unit-test-generator → coverage-reporter）
→ 部署运维（spec-devops → package-builder → deploy-script-generator）

每阶段设质量门禁：需求冲突检测 → 设计评审 → 代码审查 → 覆盖率≥60%
关键节点设人工确认点，确认后方可继续
```

**组件萃取复写流水线（跨项目操作）：**
```
在源项目中启动：启动组件萃取复写流水线，提取 XX 模块的 XX 功能

Phase 1（源项目）自动协调：
→ Step 0: gitnexus auto-index 建索引
→ Step 1-4: component-code-analyzer 6维度分析 + 业务耦合检测
→ Step 5-8: 生成 Component Manual
→ 人工确认点：审查手册完整性

Phase 2（积累项目）自动协调：
→ 基于 Component Manual 的6大脱敏策略重写代码
→ component-code-rewriter: 命名脱敏、值脱敏、依赖抽象化
→ spec-component-rewriter: 3层代码审查（风格→质量→安全）
→ 产出：Java代码 + 配置文件 + 单元测试 + 使用文档
→ 人工确认点：验证脱敏完整性
```

### 1. 需求分析阶段

**启动协调者：**
```
帮我分析这个订单管理系统的需求

输入：用户需求描述文档
输出：需求规格说明书（功能列表、用户故事、验收标准）
```

**单独使用原子技能：**
```
从以下文本提取功能需求...
为订单管理功能生成用户故事
编写下单功能的验收标准
```

### 2. 系统设计阶段

**启动协调者：**
```
基于需求文档进行系统设计

输入：需求规格说明书
输出：系统设计文档（架构、数据库、API、流程）
```

**单独使用原子技能：**
```
设计订单模块的分层架构
设计订单表结构
设计订单相关接口
```

### 3. 开发实施阶段

**启动协调者：**
```
开始开发订单管理模块

输入：系统设计文档
输出：代码实现 + 数据库脚本 + API文档
```

**单独使用原子技能：**
```
生成 Order 模块的代码骨架
实现 OrderService.createOrder 方法
审查 OrderService 代码质量
生成订单表的数据库迁移脚本
```

### 4. 测试与质量保证阶段

**启动协调者：**
```
制定测试策略并生成测试

输入：代码实现
输出：测试计划 + 单元测试 + 集成测试 + API测试文档 + 覆盖率报告
```

**单独使用原子技能：**
```
为 OrderService 生成单元测试（含Controller/Mapper层）
为 OrderMapper 生成集成测试
生成订单接口的测试文档
生成测试覆盖率报告
```

### 5. 部署与运维阶段

**启动协调者：**
```
准备部署方案

输入：项目结构和配置
输出：部署包 + 部署脚本 + 运维手册
```

**单独使用原子技能：**
```
构建部署包（jar + tar）
生成 systemd 和 Docker 部署脚本
生成多环境配置文件
生成故障排查手册
```

## 技能测试指南

对技能库本身进行质量验证：

```
测试 unit-test-generator 技能    # 验证单元测试生成质量
测试 aIk-coding-style 技能       # 验证编码规范一致性
测试所有技能                      # 全量质量检查
```

## 技术栈预埋

所有技能已针对以下技术栈进行预埋：

- **Java 8**：Stream API、Lambda、Optional，无 Java 9+ 语法
- **Spring Boot 2.7.x**：Spring 生态
- **MyBatis-Plus 3.5.x**：LambdaQueryWrapper、BaseMapper、IService
- **Lombok**：@Data、@Builder、@RequiredArgsConstructor、@Slf4j
- **分层架构**：Controller/Service(impl)/Mapper/Entity/DO/DTO/VO
- **数据库规范**：大写下划线命名（T_ORDER、CREATE_TIME）
- **测试框架**：JUnit 5 + Mockito + AssertJ
- **构建工具**：Maven

## 快速开始示例

### 示例1：新项目从0到部署（Pipeline模式）

```
你：启动Java SDLC流水线，开发订单管理系统

AI：执行5阶段流水线：

【阶段1/5：需求分析】
→ 质量门禁：PRD冲突检测 ✓
→ 人工确认点：需求确认
→ 输出：功能列表、用户故事、验收标准

【阶段2/5：系统设计】
→ 质量门禁：设计评审 ✓
→ 输出：架构设计、数据库设计、API设计

【阶段3/5：开发实施】
→ quality-gate: code-review ✓
→ 输出：Java代码、数据库脚本、API文档

【阶段4/5：测试验证】
→ quality-gate: coverage≥60% ✓
→ 输出：测试报告、覆盖率分析

【阶段5/5：部署运维】
→ 输出：部署包、运维手册
```

### 示例2：单独生成单元测试

```
你：为 OrderService 生成单元测试

AI：自动调用 unit-test-generator
→ 分析 OrderService 的方法签名
→ 识别需要测试的业务场景（正常/异常/边界）
→ 生成 JUnit 5 + Mockito + AssertJ 测试代码
→ 包含 Controller 和 Mapper 层测试
→ 输出到 src/test/java/...
```

### 示例3：代码质量审查

```
你：审查 OrderService 的代码质量

AI：自动协调审查技能：
→ code-style-reviewer：命名、注释、格式检查
→ code-quality-reviewer：N+1查询、空指针、事务边界
→ code-security-reviewer：SQL注入、权限校验
→ bug-pattern-analyzer：常见缺陷模式识别
输出完整的审查报告
```

## 注意事项

1. **技能类型说明**
   - `spec-` 开头的是协调者技能（Inversion模式），统筹多个原子技能完成复杂任务
   - 其他是原子技能，执行单一具体任务
   - `java-sdlc-pipeline` 是统一流水线入口，一键启动全流程
   - `skill-tester` 是技能质量体系，用于验证技能输出质量
   - `handoff-bundle` 是技术栈无关的跨 Agent 交接工具，不参与 SDLC 流水线

2. **配置复用**
   - 所有技能遵循"优先复用项目已有"原则
   - Result/PageDTO、全局异常处理、配置类等不会重复创建

3. **人工决策点**
   - 数据库类型确认
   - 覆盖率阈值调整
   - 敏感信息处理
   - 部署时间窗口
   - Pipeline各阶段间需人工确认

4. **L3渐进式加载**
   - L1（YAML元数据）：name/description/type/version
   - L2（SKILL.md核心）：核心工作流，始终在上下文
   - L3（按需加载）：references/参考文档, assets/模板, scripts/脚本

## 更新日志

### v1.2.0 (2026-08-12)
- **新增**：`handoff-bundle` 跨 Agent 任务交接技能（Tool Wrapper 模式，技术栈无关）
  - L3 拆分：SKILL.md 核心指令 + references/（handoff-template、asset-manifest-schema、bundle-structure、verification-protocol）
  - 便携版：assets/handoff-skill-portable.md 单文件版，可外部分发给未安装 aik-skills-lab 的 GPT/Codex/Claude
  - 工程化增益：asset-manifest-schema.json 用 JSON Schema draft-07 + enum 收紧目录穿越/优先级/状态字段
  - 无关性声明：技术栈/模型族/宿主工具三不绑定，契约层只依赖文件系统/SHA-256/Git 三类跨平台标准
- 技能总数：47 → 48

### v1.1.0 (2026-05-14)
- **新增**：`component-extraction-rewriting-workflow` 组件萃取复写流水线（Pipeline 统一入口）
- **新增**：`component-code-analyzer` 6维度代码分析 + 业务耦合检测（L3: 161行核心 + references）
- **新增**：`spec-component-extractor` 萃取协调者（gitnexus auto-index + 人工确认）
- **新增**：`component-code-rewriter` 6大脱敏策略 + 代码风格审查（L3: 211行核心 + references）
- **新增**：`spec-component-rewriter` 复写协调者（3层代码审查）
- **新增**：`component-extraction-workflow` 组件萃取复写流水线设计方案（后归档至 `zero/learning-notes/`）
- **集成**：gitnexus MCP 代码智能引擎，支持跨项目知识图谱分析
- 技能总数：42 → 47

### v1.0.0 (2024-05-13)
- **架构升级**：全面重构至 Agent Skills L3 渐进式加载架构
- **新增**：`java-sdlc-pipeline` 统一流水线入口（5阶段+质量门禁+人工确认）
- **新增**：`skill-tester` 技能质量体系（RED-GREEN-REFACTOR框架）
- **新增**：`scripts/` 全局脚本（bom-checker.sh, naming-validator.sh）
- **重构**：`aIk-coding-style` 拆分为 203行核心 + 18个reference文件
- **合并**：`test-generator` 合并至 `unit-test-generator`（扩展Controller/Mapper测试）
- **目录**：15+技能新增 L3 references/assets/ 目录 + 模板文件
- **元数据**：全量修复 18 个缺失 type/version 的 YAML frontmatter
- **描述**：41 个技能 description 重写为祈使语气

### v1.0.0 (2024-03-18)
- 初始版本
- 包含5个阶段共38个技能
- 支持 Java 8 + Spring Boot + MyBatis-Plus 技术栈

## 许可证

MIT License - 可自由使用和修改

## 联系方式

如有问题或建议，请联系项目维护者。

---

**更新日期：** 2026-05-14  
**技能总数：** 48个（37个原子技能 + 7个协调者 + 2个流水线 + 1个质量体系 + 1个工具集成）  
**设计模式：** 5种（Pipeline / Generator / Reviewer / Inversion / Tool Wrapper）  
**适用技术栈：** Java 8 + Spring Boot + MyBatis-Plus + Lombok  
**架构标准：** Agent Skills L3 渐进式加载

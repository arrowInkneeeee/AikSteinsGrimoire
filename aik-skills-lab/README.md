# aIk Skills Lab

## 简介

一套完整的软件开发生命周期（SDLC）技能库，专为 Java 8 + Spring Boot + MyBatis-Plus + Lombok 技术栈设计。

包含 **10 个技能**，采用子命令模式覆盖需求分析、系统设计、开发实施、测试质量、部署运维、组件萃取全流程。消灭编排层，每个技能独立可用，通过 `full` 子命令支持全流程执行。

## 技能库结构

```
aik-skills-lab/
├── aIk-coding-style/              # Java 后端编码规范（所有技能的基础）
├── requirement-engineering/       # 需求工程全流程 → PRD
├── system-design/                 # 系统设计全流程 → SDD
├── code-implementation/           # 代码实现全流程 → 可运行代码
├── code-review/                   # 代码审查全流程（4维）
├── testing/                       # 测试全流程 → 测试计划+代码+覆盖率
├── component-extract-rewrite/     # 组件萃取与复写全流程
├── devops/                        # 部署运维全流程
├── handoff-bundle/                # 跨 Agent 任务交接
└── skill-tester/                  # 技能质量测试
```

### 技能详情

| # | 技能 | 子命令 | 输入 | 输出 |
|---|------|--------|------|------|
| 1 | **aIk-coding-style** | - | - | 编码规范约束（被所有技能引用） |
| 2 | **requirement-engineering** | extract / clarify / conflict-check / user-story / feasibility / prioritize / full | 原始需求文本 | PRD 文档 |
| 3 | **system-design** | architecture / database / api / process / tech-select / review / full | PRD | SDD 文档 |
| 4 | **code-implementation** | skeleton / implement / api-doc / db-migration / full | SDD | 可运行代码 + API文档 + SQL脚本 |
| 5 | **code-review** | style / quality / security / bug-pattern / full | 源代码 | 审查报告 |
| 6 | **testing** | unit / integration / api / coverage / data / full | 源代码 | 测试代码 + 覆盖率报告 |
| 7 | **component-extract-rewrite** | analyze / extract / rewrite / full-pipeline | 源项目代码 | 组件手册 + 标准化代码 |
| 8 | **devops** | config / package / deploy / health / log / troubleshoot / full | 项目配置 | 部署包 + 运维手册 |
| 9 | **handoff-bundle** | - | 当前任务上下文 | HANDOFF.md + 资产包 |
| 10 | **skill-tester** | - | 目标技能 | RED-GREEN-REFACTOR 测试报告 |

### L3 渐进式加载架构

所有技能遵循 Agent Skills 三级加载规范：

| 层级 | 内容 | 说明 |
|------|------|------|
| **L1**（元数据） | YAML frontmatter | name, description, type, version — 零成本加载 |
| **L2**（核心指令） | SKILL.md 正文 | 核心工作流 + 子命令定义，始终在上下文 |
| **L3**（按需加载） | references/, assets/, scripts/ | 参考文档、模板、脚本，仅在需要时加载 |

## 使用方法

### 方式一：全流程开发

使用各技能的 `full` 子命令，按阶段依次执行：

```
# 阶段1：需求分析
分析以下需求 → requirement-engineering full

# 阶段2：系统设计
基于 PRD 设计系统 → system-design full

# 阶段3：代码实现
根据 SDD 实现代码 → code-implementation full

# 阶段4：代码审查
审查代码质量 → code-review full

# 阶段5：测试
生成测试 → testing full

# 阶段6：部署
准备部署方案 → devops full
```

### 方式二：单阶段/子命令

直接调用特定子命令完成单一任务：

```
从以下文本提取功能需求           → requirement-engineering extract
设计订单表结构                   → system-design database
生成代码骨架                     → code-implementation skeleton
审查代码风格                     → code-review style
生成单元测试                     → testing unit
构建部署包                       → devops package
```

### 方式三：组件萃取复写

跨项目萃取和复写组件：

```
分析 XX 模块的可复用逻辑         → component-extract-rewrite analyze
萃取 XX 组件知识手册             → component-extract-rewrite extract
基于手册复写为标准代码            → component-extract-rewrite rewrite
完整萃取复写流水线               → component-extract-rewrite full-pipeline
```

### 方式四：全局/项目级安装

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

# Mac/Linux
ln -s ~/.lingma/skills .skills
```

## 技能间协作

```
requirement-engineering ──PRD──→ system-design ──SDD──→ code-implementation ──代码──→ code-review
                                                                              ↓
                                                                         testing
                                                                              ↓
                                                                          devops

component-extract-rewrite：独立流程（analyze → extract → rewrite）

handoff-bundle：任意阶段可调用，冻结当前状态交接给另一个 Agent
skill-tester：任意技能均可被测试验证
```

所有代码产出均受 `aIk-coding-style` 约束。

## 质量门禁

| 阶段 | 门禁条件 |
|------|---------|
| 需求分析 | PRD 无 high severity 冲突 |
| 系统设计 | 设计评审 P0=0, P1=0, P2≤2 |
| 代码实现 | 通过 code-review full |
| 测试 | 入口：行覆盖>60%, 分支>50%；出口：行覆盖>70%, 分支>60% |

## 技术栈预埋

所有技能已针对以下技术栈进行预埋：

- **Java 8**：Stream API、Lambda、Optional，无 Java 9+ 语法
- **Spring Boot 2.7.x**：Spring 生态
- **MyBatis-Plus 3.5.x**：LambdaQueryWrapper、BaseMapper、IService
- **Lombok**：@Data、@Builder、@RequiredArgsConstructor、@Slf4j
- **分层架构**：Controller/Service(impl)/Mapper/Entity/DTO/VO
- **数据库规范**：大写下划线命名（T_ORDER、CREATE_TIME）
- **测试框架**：JUnit 5 + Mockito + AssertJ
- **构建工具**：Maven

## 注意事项

1. **子命令模式**：每个技能通过子命令提供细粒度控制，`full` 子命令执行完整流程
2. **优先复用**：所有技能遵循"优先复用项目已有"原则（Result / BaseEntity / PageDTO 等不重复创建）
3. **人工决策点**：数据库类型确认、覆盖率阈值调整、敏感信息处理、部署时间窗口等场景需暂停确认
4. **L3 渐进式加载**：
   - L1（YAML 元数据）：name/description/type/version
   - L2（SKILL.md 核心）：核心工作流 + 子命令，始终在上下文
   - L3（按需加载）：references/ 参考文档, assets/ 模板, scripts/ 脚本

## 更新日志

### v3.0.0 (2026-10-09)
- **架构重构**：47 → 10 技能整合，消灭编排层
- **新增子命令模式**：每个技能通过子命令提供细粒度控制
- **合并**：7 个需求原子技能 → `requirement-engineering`
- **合并**：6 个设计原子技能 → `system-design`
- **合并**：code-generator + code-implementer + api-doc-generator + db-migration-generator → `code-implementation`
- **合并**：3 个 reviewer + bug-pattern-analyzer → `code-review`
- **合并**：6 个测试技能 → `testing`
- **合并**：5 个组件相关技能 → `component-extract-rewrite`
- **合并**：6 个部署运维技能 → `devops`
- **删除**：9 个编排器（java-sdlc-pipeline, spec-* 系列）
- **保留**：aIk-coding-style, handoff-bundle, skill-tester 不变
- **迁移**：12 个 assets 模板 + 25 个 references 文件（含 14 个新提取的决策矩阵/检查清单）

### v2.0.0 (2026-08-12)
- **新增**：`handoff-bundle` 跨 Agent 任务交接技能
- 技能总数：47 → 48

### v1.1.0 (2026-05-14)
- **新增**：`component-extraction-rewriting-workflow` 组件萃取复写流水线
- **新增**：`component-code-analyzer`, `spec-component-extractor`, `component-code-rewriter`, `spec-component-rewriter`
- 技能总数：42 → 47

### v1.0.0 (2024-05-13)
- **架构升级**：全面重构至 Agent Skills L3 渐进式加载架构
- **新增**：`java-sdlc-pipeline` 统一流水线入口
- **新增**：`skill-tester` 技能质量体系
- **重构**：`aIk-coding-style` 拆分为 203 行核心 + 18 个 reference 文件
- 技能总数：38 → 42

### v1.0.0 (2024-03-18)
- 初始版本
- 包含 5 个阶段共 38 个技能
- 支持 Java 8 + Spring Boot + MyBatis-Plus 技术栈

## 许可证

MIT License - 可自由使用和修改

---

**更新日期：** 2026-10-09  
**技能总数：** 10 个（7 个全流程技能 + aIk-coding-style + handoff-bundle + skill-tester）  
**设计模式：** 子命令模式（Sub-command）  
**适用技术栈：** Java 8 + Spring Boot + MyBatis-Plus + Lombok  
**架构标准：** Agent Skills L3 渐进式加载

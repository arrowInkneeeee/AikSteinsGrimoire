---
name: system-design
description: 系统设计全流程技能——架构分层设计、数据库设计、API设计、流程设计、技术选型、设计评审，最终产出结构化SDD文档。适用于"系统设计"、"架构设计"、"数据库设计"、"API设计"、"流程设计"、"技术选型"、"设计评审"、"出设计方案"等场景。
type: Skill
version: 1.0.0
trigger:
  - 系统设计
  - 架构设计
  - 数据库设计
  - API设计
  - 流程设计
  - 技术选型
  - 设计评审
  - 出设计方案
  - 技术方案设计
  - architecture-designer
  - database-designer
  - api-designer
  - process-designer
  - tech-solution-selector
  - design-review-checker
  - spec-designer
---

# system-design

系统设计全流程技能，覆盖架构分层、数据库表设计、API接口设计、核心流程设计、技术方案选型和设计评审，最终产出结构化SDD文档。

## 前置检查

设计开始前，必须检查项目现有结构和通用组件：

```bash
# 检查项目结构
find src -type d -name "common" -o -name "util" -o -name "config" | head -20

# 检查已有通用类
grep -r "class Result" --include="*.java" src/
grep -r "class BaseEntity" --include="*.java" src/
grep -r "class PageDTO" --include="*.java" src/

# 检查 pom.xml 依赖
cat pom.xml | grep -E "(redis|rabbitmq|redisson|kafka)"
```

## 子命令

### architecture

分层架构设计，定义包结构、各层职责和组件交互规则。

**触发条件**：用户说"设计架构"、"分层设计"、"包结构设计"

**输入**：
- PRD文档或功能需求描述
- 项目技术栈信息

**输出**：
- 包结构设计（controller/service/dao/entity/dto/vo/common）
- 各层职责定义
- 通用组件复用清单（优先复用项目已有的 Result, BaseEntity, PageDTO）
- 模块划分方案

**执行步骤**：
1. 分析需求，识别业务模块边界
2. 检查项目已有的通用组件（Result, BaseEntity, PageDTO等）
3. 设计包结构，遵循 aIk-coding-style 分层规范
4. 定义各层职责和交互规则
5. 标注可复用的项目通用组件
6. 输出架构设计文档

---

### database

数据库表结构设计，包含DDL、索引和MyBatis-Plus实体类。

**触发条件**：用户说"设计数据库"、"建表设计"、"表结构设计"

**输入**：
- PRD文档或功能需求描述
- 数据库类型（MySQL/PostgreSQL/Oracle）

**输出**：
- ER图（PlantUML格式）
- 表结构设计（字段、类型、约束）
- 索引设计（IDX_/UK_前缀）
- MyBatis-Plus实体类代码
- 建表SQL脚本

**执行步骤**：
1. 分析需求中的实体和关系
2. 设计表结构（表名大写下划线，字段大写下划线）
3. 设计索引（主键、唯一索引、普通索引）
4. 生成ER图（PlantUML格式）
5. 生成MyBatis-Plus实体类（遵循 aIk-coding-style 注解规范）
6. 生成建表SQL
7. 使用 `assets/er-diagram-template.md` 模板输出

---

### api

RESTful API接口设计，定义请求参数、响应结构和URL路径。

**触发条件**：用户说"设计API"、"设计接口"、"接口设计"

**输入**：
- PRD文档或功能需求描述
- 架构设计结果（包结构）

**输出**：
- API列表（方法 + URL + 说明）
- 请求参数定义（DTO）
- 响应结构定义（VO，统一使用 Result 包装）
- 错误码定义
- API契约文档

**执行步骤**：
1. 分析需求中的操作行为，映射为RESTful操作
2. 设计URL路径（复数名词、层级关系）
3. 定义请求参数（Query/Path/Body）
4. 定义响应结构（统一使用 Result<T> 包装）
5. 设计错误码体系
6. 使用 `assets/api-contract-template.md` 模板输出

---

### process

核心业务流程设计，包含时序图、状态机和事务设计。

**触发条件**：用户说"设计流程"、"时序图"、"状态机"

**输入**：
- PRD文档或功能需求描述
- 架构设计和API设计结果

**输出**：
- 业务流程时序图（PlantUML格式）
- 状态机设计（枚举 + 状态转换规则）
- 事务边界标注和传播行为说明

**执行步骤**：
1. 识别核心业务流程
2. 绘制时序图（PlantUML格式）
3. 设计状态机（枚举定义 + 转换规则）
4. 标注事务边界和传播行为
5. 使用 `references/transaction-patterns.md` 中的事务模式
6. 输出流程设计文档

---

### tech-select

技术方案选型，覆盖缓存、MQ、分布式锁、异步处理等场景。

**触发条件**：用户说"技术选型"、"选型"、"用什么技术"

**输入**：
- 功能需求描述
- 项目已有依赖（从pom.xml检测）

**输出**：
- 技术方案对比表
- 推荐方案及理由
- 需要引入的新依赖

**执行步骤**：
1. 加载 `references/tech-selection-tree.md` 中的决策树
2. 检测项目已有依赖（Redis/RabbitMQ/Kafka等）
3. 按决策树逐场景选型：
   - 缓存方案（Spring Cache/Redis/Caffeine/多级缓存）
   - 消息队列（RabbitMQ/Kafka/RocketMQ）
   - 分布式锁（Redisson/Redis+Lua/Zookeeper）
   - 异步处理（@Async/CompletableFuture/Spring Event）
4. 输出对比表和推荐方案
5. 列出需要引入的新依赖

---

### review

设计评审检查，识别设计缺陷和风险点。

**触发条件**：用户说"设计评审"、"检查设计"、"评审设计方案"

**输入**：
- SDD文档或各子设计输出

**输出**：
- 评审报告（P0-P3分级）
- 待解决问题清单
- 优化建议

**执行步骤**：
1. 加载 `references/design-review-checklist.md` 中的P0-P3检查清单
2. 逐项检查9大类别：
   - 项目复用、分层架构、数据库设计、接口设计
   - 流程设计、性能、技术选型、Java规范
3. 为每个问题标注严重度（P0=阻塞/P1=严重/P2=警告/P3=建议）
4. 输出评审报告

**质量门禁**：
- P0 = 0（阻塞问题必须修复）
- P1 = 0（严重问题必须修复）
- P2 <= 2（警告可记录后继续）

---

### full

完整系统设计流程，从PRD到结构化SDD文档。

**触发条件**：用户说"完整设计"、"系统设计"、"出设计方案"等全流程场景

**输入**：
- PRD文档（来自 requirement-engineering 的输出）

**输出**：
- 结构化SDD文档（Markdown格式），保存至 `docs/sdd/` 目录

**执行步骤**：
1. 执行前置检查（检查项目已有组件和依赖）
2. 执行 `architecture` → 架构设计
3. 执行 `database` → 数据库设计
4. 执行 `api` → API接口设计
5. 执行 `process` → 流程设计
6. 执行 `tech-select` → 技术选型
7. 执行 `review` → 设计评审
8. 整合所有输出为SDD文档（使用SDD模板结构）
9. 保存SDD文档至 `docs/sdd/` 目录

**质量门禁**：
- 设计评审 P0 = 0, P1 = 0, P2 <= 2

---

## 输入契约

| 输入项 | 路径/来源 | 必须 | 降级策略 |
|--------|----------|------|---------|
| PRD文档 | `docs/prd/` 或用户粘贴 | 是 | 用户直接描述需求也可启动 |
| 项目技术栈 | pom.xml | 是 | 默认 Java 8 + Spring Boot + MyBatis-Plus + MySQL |
| 数据库类型 | pom.xml 或用户指定 | 否 | 默认 MySQL |

## 输出契约

| 输出项 | 路径 | 格式 |
|--------|------|------|
| SDD文档 | `docs/sdd/sdd-{项目名}-v{版本}.md` | Markdown |
| ER图 | 内联于SDD | PlantUML |
| 建表SQL | `docs/sdd/sql/` | SQL文件 |
| 评审报告 | 内联于SDD附录 | 表格 |

## 工作流上下文

**上游技能**：
- `requirement-engineering`：产出PRD文档

**下游技能**：
- `code-implementation`：消费SDD文档，生成代码
- `testing`：消费SDD中的API设计和流程设计，生成测试

## 质量门禁

| 检查点 | 条件 | 动作 |
|--------|------|------|
| 前置检查 | 已检查项目通用组件 | 继续 |
| 设计评审 | P0=0, P1=0, P2<=2 | 继续，否则修复后重新评审 |

## 约束

- 所有代码产出必须遵循 `aIk-coding-style` 规范
- 优先复用项目已有的通用组件（Result, BaseEntity, PageDTO）
- 表名大写下划线，字段大写下划线，索引 IDX_/UK_ 前缀
- Java类驼峰命名
- SDD文档使用 Markdown 格式
- 时序图和ER图使用 PlantUML 格式

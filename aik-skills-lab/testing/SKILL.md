---
name: testing
description: 测试全流程技能——单元测试、集成测试、API测试、覆盖率报告、测试数据管理，覆盖测试生命周期全流程。适用于"生成单元测试"、"生成集成测试"、"API测试"、"覆盖率报告"、"测试数据"、"制定测试计划"、"测试策略"等场景。
type: Skill
version: 1.0.0
trigger:
  - 生成单元测试
  - 生成集成测试
  - API测试
  - 覆盖率报告
  - 测试数据
  - 制定测试计划
  - 测试策略
  - 质量保证
  - QA分析
  - unit-test-generator
  - integration-test-generator
  - api-test-generator
  - coverage-reporter
  - test-data-manager
  - spec-qa-analyser
---

# testing

测试全流程技能，覆盖单元测试、集成测试、API测试、覆盖率报告和测试数据管理，最终产出测试计划 + 测试代码 + 覆盖率报告。

## 子命令

### unit

生成JUnit 5 + Mockito + AssertJ单元测试代码。

**触发条件**：用户说"生成单元测试"、"写单元测试"

**输入**：
- Java源代码（Service层或工具类）

**输出**：
- 单元测试代码文件

**执行步骤**：
1. 加载 `assets/test-class-template.md` 测试类模板
2. 加载 `references/unit-test-guide.md` 单元测试指南
3. 分析目标类的依赖关系
4. 为每个公共方法生成测试用例：
   - 正常流程（Happy Path）
   - 异常流程（参数为null、业务异常）
   - 边界条件（空集合、最大值、最小值）
5. 使用 `@Mock` 模拟外部依赖
6. 使用 `@InjectMocks` 注入被测对象
7. 使用 AssertJ 断言风格
8. 测试类命名：`XxTest`，放在 `src/test/java/` 对应包下
9. 遵循 aIk-coding-style 规范

---

### integration

生成@SpringBootTest集成测试代码。

**触发条件**：用户说"生成集成测试"、"写集成测试"

**输入**：
- Java源代码（Mapper/Repository层）
- 数据库类型（MySQL/PostgreSQL/Oracle）

**输出**：
- 集成测试代码文件

**执行步骤**：
1. 检测数据库类型（从pom.xml或用户指定）
2. 选择测试方案：
   - 有Testcontainers依赖 → 使用Testcontainers
   - 无Testcontainers → 使用H2内存数据库
3. 生成集成测试代码：
   - 使用 `@SpringBootTest`
   - 使用 `@Transactional` 自动回滚
   - 测试真实数据库交互（Mapper SQL执行）
   - 测试事务行为
   - 测试数据库约束（唯一索引、外键）
4. 测试类命名：`XxIntegrationTest`

---

### api

生成Markdown格式的API接口测试文档。

**触发条件**：用户说"生成API测试文档"、"接口测试"

**输入**：
- API文档（来自 code-implementation 的 api-doc 输出）
- 或Controller源代码

**输出**：
- API测试文档（Markdown格式）

**执行步骤**：
1. 解析API接口定义
2. 为每个接口生成测试用例：
   - 正常请求（正确参数）
   - 异常请求（缺少必填参数、参数类型错误）
   - 边界请求（空值、超长字符串）
3. 为每个测试用例提供：
   - 请求示例（curl命令或Apipost配置）
   - 预期响应（状态码 + 响应体）
4. 输出API测试文档

---

### coverage

配置JaCoCo覆盖率统计并生成报告。

**触发条件**：用户说"覆盖率报告"、"统计覆盖率"

**输入**：
- 项目pom.xml
- 覆盖率目标（可选）

**输出**：
- JaCoCo Maven配置
- 覆盖率报告（HTML + CSV）

**执行步骤**：
1. 加载 `references/coverage-thresholds.md` 阈值配置
2. 检查pom.xml是否已有JaCoCo插件
3. 生成/更新JaCoCo Maven配置：
   - 行覆盖率目标
   - 分支覆盖率目标
   - 排除规则（配置类、常量类、VO/DTO等）
4. 提供报告生成命令：`mvn verify`
5. 建议阈值策略：
   - 初期：行>60%，分支>50%
   - 中期：行>70%，分支>60%
   - 长期：行>80%，分支>70%

---

### data

设计测试数据管理方案。

**触发条件**：用户说"测试数据"、"数据管理"、"数据隔离"

**输入**：
- 测试代码
- 数据库类型

**输出**：
- 测试数据管理方案（代码 + SQL脚本）

**执行步骤**：
1. 加载 `references/test-data-strategies.md` 策略指南
2. 根据测试类型选择策略：
   - **策略1：事务回滚**（推荐）— `@Transactional` 自动回滚
   - **策略2：@BeforeEach/@AfterEach** — 手动准备和清理
   - **策略3：@Sql注解** — SQL脚本初始化
   - **策略4：Builder模式** — 测试数据构造器
3. 生成测试数据常量类（TestDataConstants）
4. 生成测试数据构造器（TestDataPreparer）
5. 生成SQL初始化脚本（如需要）

---

### full

完整测试流程，产出测试计划 + 测试代码 + 覆盖率报告。

**触发条件**：用户说"完整测试"、"制定测试计划"、"测试策略"

**输入**：
- SDD文档或代码（来自上游技能）
- 测试范围

**输出**：
- 测试计划文档（使用 `assets/test-plan-template.md` 模板）
- 单元测试代码
- 集成测试代码（如需要）
- API测试文档
- 覆盖率配置

**执行步骤**：
1. 加载 `assets/test-plan-template.md` 测试计划模板
2. 分析测试范围和目标
3. 制定测试策略（单元/集成/API比例）
4. 执行 `unit` → 生成单元测试
5. 执行 `integration` → 生成集成测试（如需要）
6. 执行 `api` → 生成API测试文档
7. 执行 `coverage` → 配置覆盖率
8. 执行 `data` → 设计测试数据方案
9. 整合输出测试计划文档
10. 加载 `references/quality-gates.md` 检查质量门禁

**质量门禁**：
- 准入标准：通过率>90%，行覆盖>60%，分支>50%，无P0/P1缺陷
- 准出标准：全部通过，行覆盖>70%，分支>60%，接口文档已交付

---

## 输入契约

| 输入项 | 路径/来源 | 必须 | 降级策略 |
|--------|----------|------|---------|
| Java源代码 | 项目源码目录 | 是 | 无 |
| SDD文档 | `docs/sdd/` | 否（full模式需要） | 直接从代码分析 |
| 数据库类型 | pom.xml | 否 | 默认 MySQL |

## 输出契约

| 输出项 | 路径 | 格式 |
|--------|------|------|
| 单元测试 | `src/test/java/` | Java源文件 |
| 集成测试 | `src/test/java/` | Java源文件 |
| API测试文档 | `docs/test/api-test.md` | Markdown |
| 测试计划 | `docs/test/test-plan.md` | Markdown |
| 覆盖率报告 | `target/site/jacoco/` | HTML + CSV |

## 工作流上下文

**上游技能**：
- `code-implementation`：产出待测试的代码
- `code-review`：审查通过后的代码进入测试

**下游技能**：
- `devops`：测试通过后进行部署

## 质量门禁

| 检查点 | 条件 | 动作 |
|--------|------|------|
| 准入标准 | 通过率>90%, 行覆盖>60%, 分支>50% | 继续，否则补充测试 |
| 准出标准 | 全部通过, 行覆盖>70%, 分支>60% | 通过，否则补充测试 |

## 约束

- 测试框架：JUnit 5 + Mockito + AssertJ
- 集成测试使用 `@SpringBootTest`
- 覆盖率工具：JaCoCo
- 测试数据隔离：优先使用事务回滚
- 所有测试代码遵循 `aIk-coding-style` 规范

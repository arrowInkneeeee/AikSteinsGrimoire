# 快速开始指南

## 一分钟上手

### 1. 确认技能已安装

```bash
# 检查用户级技能目录
dir C:\Users\arrowInknee\.lingma\skills\
```

重启 IDE，在设置中查看"技能"是否显示 10 个技能。

### 2. 推荐使用方式

**全流程开发**：按阶段依次使用各技能的 `full` 子命令

```
分析以下需求，产出 PRD
基于 PRD 设计系统架构
根据 SDD 实现代码
审查代码质量
生成测试
准备部署方案
```

**单阶段任务**：直接调用特定子命令

```
从以下文本提取功能需求           → requirement-engineering extract
设计订单表结构                   → system-design database
生成代码骨架                     → code-implementation skeleton
审查代码风格                     → code-review style
生成单元测试                     → testing unit
构建部署包                       → devops package
```

### 3. 新项目中使用

**方式A：符号链接（推荐，自动同步）**

```bash
# Windows CMD（管理员权限）
mklink /J .skills C:\Users\arrowInknee\.lingma\skills

# Windows PowerShell（管理员权限）
cmd /c mklink /J .skills C:\Users\arrowInknee\.lingma\skills

# Mac/Linux
ln -s ~/.lingma/skills .skills
```

**优点：** 一处修改，全局生效；无需手动同步

**方式B：复制到项目（完全隔离）**

```bash
mkdir my-project
cd my-project

# 复制技能到项目（如需项目级隔离）
xcopy /E /I "C:\Users\arrowInknee\.lingma\skills" ".skills"
```

**注意：** 复制后项目级和用户级独立，修改不会自动同步

### 4. 常用需求描述速查

| 需求场景 | 描述方式 |
|---------|---------|
| 需求分析 | `分析以下需求，包含...功能` |
| 需求澄清 | `澄清这份需求文档中的模糊点` |
| 冲突检测 | `检测这些需求之间是否有矛盾` |
| 用户故事 | `把功能列表转为用户故事` |
| 可行性评估 | `评估这个需求的技术可行性` |
| 优先级排序 | `按 MoSCoW 方法排优先级` |
| 架构设计 | `设计系统分层架构` |
| 数据库设计 | `设计订单表结构` |
| API 设计 | `设计 RESTful API 接口` |
| 技术选型 | `对比 Redis 和 Caffeine 的适用场景` |
| 设计评审 | `评审这份设计文档` |
| 生成代码骨架 | `生成订单模块代码骨架` |
| 实现业务逻辑 | `根据 SDD 填充业务逻辑` |
| 生成 API 文档 | `生成接口文档` |
| 生成迁移脚本 | `生成数据库迁移 SQL` |
| 代码风格审查 | `审查代码风格是否符合规范` |
| 代码质量审查 | `检查 N+1 查询和事务问题` |
| 安全审查 | `检查 SQL 注入和越权风险` |
| 缺陷模式分析 | `分析代码中的常见缺陷模式` |
| 生成单元测试 | `为 OrderService 生成单元测试` |
| 生成集成测试 | `为 OrderMapper 生成集成测试` |
| 覆盖率报告 | `生成测试覆盖率报告` |
| 组件分析 | `分析 XX 模块的可复用逻辑` |
| 组件萃取 | `萃取 XX 组件知识手册` |
| 组件复写 | `基于手册复写为标准代码` |
| 多环境配置 | `生成 dev/test/prod 配置` |
| 构建部署包 | `构建可部署的包` |
| 健康检查 | `配置 Actuator 健康检查` |
| 日志配置 | `配置 Logback 日志输出` |
| 故障排查 | `生成故障排查手册` |
| 任务交接 | `交接当前任务给下一个 Agent` |
| 技能测试 | `测试 code-review 技能` |

## 典型场景示例

### 场景1：新项目全流程开发

```
用户：开发订单管理系统，包含用户下单、支付、查询、取消

阶段1：需求分析
  → requirement-engineering full
  → 产出：PRD（功能列表、用户故事、验收标准）
  → 人工确认：请确认需求规格说明书

阶段2：系统设计
  → system-design full
  → 产出：SDD（架构设计、数据库设计、API 设计）
  → 人工确认：请确认系统设计文档

阶段3：代码实现
  → code-implementation full
  → 产出：Controller/Service/Mapper/Entity 代码 + API 文档 + SQL 脚本
  → 所有代码自动遵循 aIk-coding-style

阶段4：代码审查
  → code-review full
  → 产出：风格/质量/安全/缺陷模式 四维审查报告

阶段5：测试
  → testing full
  → 产出：测试计划 + 单元测试 + 集成测试 + 覆盖率报告

阶段6：部署
  → devops full
  → 产出：部署包 + 部署脚本 + 运维手册
```

### 场景2：已有项目单独生成测试

```
用户：为 OrderService 生成单元测试

  → testing unit
  → 分析 OrderService 的方法签名
  → 识别需要测试的业务场景
  → 生成 JUnit 5 + Mockito + AssertJ 测试代码
  → 输出到 src/test/java/...
```

### 场景3：代码审查

```
用户：审查 OrderService 的代码质量

  → code-review full
  → 自动执行四维审查：
    1. 风格审查：命名规范、注释格式、代码风格
    2. 质量审查：N+1 查询、空指针、事务边界
    3. 安全审查：SQL 注入风险、权限校验
    4. 缺陷模式：识别常见缺陷模式
  → 产出：综合审查报告 + 修复建议
```

### 场景4：项目容器化部署

```
用户：把项目改成 Docker 部署

  → devops full
  → 自动执行：
    1. 多环境配置管理
    2. 构建部署包（jar + tar）
    3. 生成 Dockerfile、docker-compose.yml
    4. 配置 Actuator 健康检查
    5. 配置 Logback 容器日志输出
    6. 生成故障排查手册
```

### 场景5：测试技能本身的质量

```
用户：测试 code-review 技能

  → skill-tester

RED 阶段：
  → 定义测试场景（违规代码片段）
  → 不加载技能，让模型直接审查
  → 记录基线违规检测率

GREEN 阶段：
  → 加载 code-review 技能
  → 重新审查相同代码
  → 验证违规检测率是否 > 90%

REFACTOR 阶段：
  → 分析漏检项
  → 收紧技能指令
  → 回归测试所有场景
```

## 子命令速查

| 技能 | 子命令 | 用途 |
|------|--------|------|
| requirement-engineering | extract | 从文本提取功能点 |
| | clarify | 识别模糊点并生成澄清问题 |
| | conflict-check | 检测矛盾/重复/依赖缺失 |
| | user-story | 转为标准用户故事 + 验收标准 |
| | feasibility | 技术可行性评估 |
| | prioritize | MoSCoW 优先级排序 |
| | full | 完整需求分析 → PRD |
| system-design | architecture | 分层架构设计 |
| | database | 数据库表设计 |
| | api | API 接口设计 |
| | process | 业务流程设计 |
| | tech-select | 技术方案选型 |
| | review | 设计评审 |
| | full | 完整系统设计 → SDD |
| code-implementation | skeleton | 生成代码骨架（目录+类+注解） |
| | implement | 填充业务逻辑 |
| | api-doc | 生成 API 文档 |
| | db-migration | 生成数据库迁移脚本 |
| | full | 从 SDD 到完整可运行代码 |
| code-review | style | 命名/注释/格式审查 |
| | quality | N+1/空指针/事务审查 |
| | security | SQL 注入/越权审查 |
| | bug-pattern | 缺陷模式分析 |
| | full | 四维全面审查 |
| testing | unit | JUnit 5 + Mockito 单元测试 |
| | integration | @SpringBootTest 集成测试 |
| | api | API 测试文档 |
| | coverage | JaCoCo 覆盖率配置 |
| | data | 测试数据管理策略 |
| | full | 完整测试 → 计划+代码+覆盖率 |
| component-extract-rewrite | analyze | 6 维代码分析 |
| | extract | 萃取组件知识手册 |
| | rewrite | 脱敏复写为标准代码 |
| | full-pipeline | 萃取 + 复写 + 入库 |
| devops | config | 多环境配置管理 |
| | package | 构建部署包 |
| | deploy | 生成部署脚本 |
| | health | 健康检查配置 |
| | log | 日志系统配置 |
| | troubleshoot | 故障排查手册 |
| | full | 完整部署 → 计划+手册 |

## L3 渐进式加载速查

| 触发词 | 加载内容 | 示例 |
|--------|---------|------|
| 自动加载 | L1 YAML 元数据 + L2 核心指令 | 所有技能 |
| `参考`/`规范`/`模板` | L3 references/ | aIk-coding-style |
| `模板`/`生成` | L3 assets/ | requirement-engineering |
| `脚本`/`检查` | scripts/ | aIk-coding-style/scripts/ |

## 故障排除

### 问题1：技能未显示在设置中

```bash
# 确认技能目录存在
dir C:\Users\arrowInknee\.lingma\skills\

# 确保 10 个技能目录均包含 SKILL.md
dir /s /b C:\Users\arrowInknee\.lingma\skills\*\SKILL.md

# 重启 IDE
```

### 问题2：调用技能无响应

- 检查网络连接
- 尝试直接描述需求
- 确认技能文件完整（SKILL.md 存在且含 YAML frontmatter）

### 问题3：生成的代码不符合规范

在对话中补充规范要求：

```
请按照以下规范生成代码：
- 遵循 aIk-coding-style 编码规范
- Java 8 语法
- 使用 MyBatis-Plus LambdaQueryWrapper
- Service 继承 IService
```

### 问题4：全流程中途需要暂停

每个阶段完成后会暂停等待确认，回复"确认"或"继续"进入下一阶段：
```
确认需求规格说明书，继续系统设计
```

## 最佳实践

### DO
- 全流程开发按阶段使用各技能的 `full` 子命令
- 单一任务直接调用对应子命令
- 先需求和设计，再编码
- 明确输入需求，越详细输出越准确
- 分阶段确认，避免一次性生成过多内容
- 定期使用 `skill-tester` 验证技能质量

### DON'T
- 不要跳过设计直接生成代码
- 不要期望一次生成完美代码，需要迭代
- 不要忽视代码审查环节
- 不要在生产环境直接部署未测试的代码

## 联系支持

如有问题，请参考：
- 完整文档：`README.md`
- 智能体定义：`agent.md`
- 技能详情：各技能目录下的 `SKILL.md`
- 编码规范：`aIk-coding-style/references/`

---

**技能总数：** 10 个 | **设计模式：** 子命令模式（Sub-command） | **架构标准：** Agent Skills L3 渐进式加载
**适用技术栈：** Java 8 + Spring Boot + MyBatis-Plus + Lombok

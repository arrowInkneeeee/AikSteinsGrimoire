# 快速开始指南

## 一分钟上手

### 1. 确认技能已安装

```bash
# 检查用户级技能目录
dir C:\Users\arrowInknee\.lingma\skills\
```

重启 Lingma IDE，在设置中查看"技能、智能体、指令"是否显示 42 个技能。

### 2. 推荐使用方式：Pipeline 一键启动

直接使用 `java-sdlc-pipeline` 流水线，自动串联全流程：

```
启动Java SDLC流水线，开发XX功能
```

流水线将依次执行：需求分析 → 系统设计 → 开发实施 → 测试验证 → 部署运维，每个阶段通过质量门禁后自动进入下一阶段。

### 3. 新项目中使用

**方式A：符号链接（推荐，自动同步）**

创建符号链接指向用户级技能库，项目级和用户级自动保持同步：

```bash
# Windows CMD（管理员权限）
mklink /D .skills C:\Users\arrowInknee\.lingma\skills

# Windows PowerShell（管理员权限）
New-Item -ItemType SymbolicLink -Path ".skills" -Target "C:\Users\arrowInknee\.lingma\skills"

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

**开始开发：**

打开 Lingma 对话，直接描述你的需求：

```
启动Java SDLC流水线，开发订单管理系统，包含：
1. 用户下单
2. 订单支付
3. 订单查询
4. 订单取消
```

Lingma 会自动通过流水线协调各个技能进行分析和开发。

### 4. 常用需求描述速查

| 需求场景 | 描述方式 |
|---------|---------|
| 全流程开发 | `启动Java SDLC流水线，开发XX模块` |
| 需求分析 | `帮我分析XX系统的需求，包含...功能` |
| 系统设计 | `请设计XX系统的技术方案` |
| 生成代码 | `帮我开发XX模块` |
| 生成单元测试 | `为XXService生成单元测试` |
| 生成集成测试 | `为XXMapper生成集成测试` |
| 代码审查 | `审查XX代码的质量和风格` |
| API文档 | `生成XX模块的接口文档` |
| 部署准备 | `准备XX项目的部署方案` |
| 技能测试 | `测试unit-test-generator技能` |

**提示：** 直接描述你的需求，Lingma 会自动匹配并调用合适的技能。新项目推荐使用 Pipeline 流水线一键启动。

## 典型场景示例

### 场景1：新项目全流程开发（Pipeline模式）

```
用户：启动Java SDLC流水线，开发用户管理模块

AI流水线自动执行5个阶段：

【阶段1/5：需求分析】
  → 调用 spec-requirement-analyser 协调需求分析
  → 调用 requirement-extractor, user-story-generator 等原子技能
  → 质量门禁：PRD冲突检测 ✓
  → 人工确认点：请确认需求规格说明书
  → 输出：用户故事、功能列表、验收标准

【阶段2/5：系统设计】
  → 调用 spec-designer 协调系统设计
  → 调用 architecture-designer, database-designer, api-designer 等
  → 质量门禁：设计评审 ✓
  → 人工确认点：请确认系统设计文档
  → 输出：架构图、数据库设计、API设计

【阶段3/5：开发实施】
  → 调用 spec-implementer 协调开发
  → code-generator → code-implementer → code-quality-reviewer
  → 质量门禁：代码审查 ✓
  → 输出：Controller/Service/Mapper/Entity 代码 + 数据库脚本 + API文档

【阶段4/5：测试验证】
  → 调用 spec-qa-analyser 协调测试
  → unit-test-generator → integration-test-generator → coverage-reporter
  → 质量门禁：覆盖率≥60% ✓
  → 输出：单元测试、集成测试、覆盖率报告

【阶段5/5：部署运维】
  → 调用 spec-devops 协调部署
  → package-builder → deploy-script-generator → config-manager
  → 输出：部署包、脚本、运维手册
```

### 场景2：已有项目单独生成测试

```
用户：为 OrderService 生成单元测试

AI：调用 unit-test-generator
分析 OrderService 的方法签名
识别需要测试的业务场景
生成 JUnit 5 + Mockito + AssertJ 测试代码
包含 Service/Controller/Mapper 三层测试
输出到 src/test/java/...
```

### 场景3：代码审查与优化

```
用户：审查 OrderService 的代码质量

AI：自动协调审查技能：

1. 代码风格审查
   → code-style-reviewer：命名规范、注释完整性、代码格式

2. 代码质量审查
   → code-quality-reviewer：N+1查询、空指针、事务边界

3. 代码安全审查
   → code-security-reviewer：SQL注入风险、权限校验

4. 缺陷模式分析
   → bug-pattern-analyzer：识别常见缺陷模式
```

### 场景4：项目容器化部署

```
用户：把项目改成Docker部署

AI：

1. 构建打包
   → package-builder：生成jar包和tar包

2. 生成部署配置
   → deploy-script-generator：Dockerfile、docker-compose.yml、systemd配置

3. 配置管理
   → config-manager：生成外置配置文件（检测Nacos集成）

4. 健康检查
   → health-check-designer：Actuator端点配置

5. 日志配置
   → log-configurator：Logback容器日志输出配置

6. 故障排查
   → troubleshooting-guide：应急响应与排查手册
```

### 场景5：测试技能本身的质量

```
用户：测试 unit-test-generator 技能

AI：调用 skill-tester

RED阶段：
  → 定义测试场景：Service正常流程/Mock异常/边界值
  → 加载预期行为规范

GREEN阶段：
  → 执行 unit-test-generator 生成测试代码
  → 验证输出是否符合 aIk-coding-style 规范
  → 检查 Mock 使用是否正确、断言是否完整

REFACTOR阶段：
  → 如测试未通过，分析根因
  → 优化技能指令或参考文件
  → 重新验证
```

## 5种设计模式速查

| 模式 | 说明 | 何时使用 |
|------|------|---------|
| **Pipeline** | 多阶段流水线串联 | 全流程开发（`java-sdlc-pipeline`） |
| **Generator** | 根据输入生成产出物 | 代码/测试/文档生成 |
| **Reviewer** | 检查产出物质量 | 代码审查/设计评审 |
| **Inversion** | 协调子技能完成复杂任务 | spec-* 开头的协调者技能 |
| **Tool Wrapper** | 封装外部工具或配置 | gitnexus, config-manager 等 |

## L3渐进式加载速查

| 触发词 | 加载内容 | 示例技能 |
|--------|---------|---------|
| 自动加载 | L1 YAML 元数据 + L2 核心指令 | 所有技能 |
| `参考`/`规范`/`模板` | L3 references/ | aIk-coding-style |
| `模板`/`生成` | L3 assets/ | spec-requirement-analyser |
| `脚本`/`检查` | scripts/ | scripts/naming-validator.sh |

## 故障排除

### 问题1：技能未显示在设置中

**解决：**
```bash
# 确认技能目录存在
dir C:\Users\arrowInknee\.lingma\skills\

# 确保SKILL.md文件存在（至少42个目录包含SKILL.md）
dir /s /b C:\Users\arrowInknee\.lingma\skills\*\SKILL.md

# 重启 Lingma IDE
```

### 问题2：调用技能无响应

**解决：**
- 检查网络连接
- 尝试直接描述需求，不使用 @技能名称
- 确认技能文件完整（SKILL.md 存在且含 YAML frontmatter）

### 问题3：生成的代码不符合规范

**解决：**
在对话中补充规范要求，或直接引用 aIk-coding-style：

```
请按照以下规范生成代码：
- 遵循 aIk-coding-style 编码规范
- Java 8 语法
- 使用 MyBatis-Plus LambdaQueryWrapper
- Service 继承 IService
```

### 问题4：Pipeline 中途停止

**解决：**
Pipeline 在关键节点设有人工确认点，需回复"确认"或"继续"方可进入下一阶段：
```
确认需求规格说明书，继续系统设计
```

## 最佳实践

### DO
- ✅ 新项目优先使用 `java-sdlc-pipeline` 全流程
- ✅ 老项目按需调用原子技能（如单独测试生成）
- ✅ 先调用子智能体（spec-开头）进行整体规划
- ✅ 明确输入需求，越详细输出越准确
- ✅ 分阶段确认，避免一次性生成过多内容
- ✅ 定期使用 `skill-tester` 验证技能质量

### DON'T
- ❌ 不要跳过设计直接生成代码
- ❌ 不要期望一次生成完美代码，需要迭代
- ❌ 不要忽视代码审查环节
- ❌ 不要在生产环境直接部署未测试的代码

## 联系支持

如有问题，请参考：
- 完整文档：`README.md`
- 技能详情：各技能目录下的 `SKILL.md`
- 编码规范：`aIk-coding-style/references/` 下的参考文档

---

**技能总数：** 42个 | **设计模式：** 5种 | **架构标准：** Agent Skills L3 渐进式加载  
**适用技术栈：** Java 8 + Spring Boot + MyBatis-Plus + Lombok

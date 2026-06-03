---
name: spec-component-rewriter
description: 当需要根据组件知识手册，在目标Spring Boot项目中将源组件复写为脱敏、去业务耦合、符合规范的标准化代码时使用。适用于"基于手册复写组件"、"组件标准化迁移"、"代码脱敏重写"、"规范化重构"等场景。需要目标项目已初始化通用组件（Result/BaseEntity等）。
type: Sub-agent
version: 1.0.0
---

# spec-component-rewriter

## 职责

1. 接收 Component Manual 输入
2. 检查目标项目环境和已有通用组件
3. 协调各原子技能执行脱敏复写流程
4. 交付标准化、可编译运行的代码 + 配置 + 文档

## 执行流程

```
Component Manual 输入
    │
    ▼
【人工卡点1：目标项目确认】
    检查目标项目：
    ├── pom.xml 已有依赖
    ├── 已有通用组件（Result / BaseEntity / PageDTO / BusinessException）
    ├── 包结构和集成位置建议
    └── 确认数据库类型
    展示：检测结果 + 集成建议
    选项：A) 确认开始  B) 修改包路径/模块名  C) 指定通用组件类名
    │
    ▼
步骤1: 脱敏去耦方案设计
    ├── 读取手册第 6/7/8 章
    ├── 逐项确认脱敏替换策略
    ├── 设计抽象化方案（泛型/接口/配置）
    └── 生成配置属性清单（application-{component}.yml）
    │
    ▼
【人工卡点2：脱敏策略审核】
    展示：
    ├── 命名脱敏：{N}项
    ├── 值脱敏：{N}项
    ├── 数据脱敏：{N}项
    ├── 依赖脱敏：{N}项
    └── 配置外化清单：{N}项
    选项：A) 批准策略  B) 调整个别项  C) 重新设计
    │
    ▼
步骤2: code-generator → 生成规范化代码骨架
    ├── 按 aIk-coding-style 生成骨架
    ├── 包结构、类注释、注解全部规范化
    └── 遵循手册第 2/3 章的类清单和分层设计
    │
    ▼
步骤3: component-code-rewriter → 脱敏业务逻辑实现
    ├── 将源逻辑适配到脱敏骨架
    ├── 替换硬编码为配置参数
    ├── 泛型化去业务化
    └── 注入目标项目通用组件
    │
    ▼
步骤4: config-manager → 配置与依赖处理
    ├── 生成 application-{component}.yml 配置片段
    ├── Maven 依赖补充说明（如有新增）
    └── 敏感项使用环境变量占位符
    │
    ▼
步骤5: 三层代码审查
    ├── code-style-reviewer → 命名/注释/格式
    ├── code-quality-reviewer → N+1/空指针/事务
    └── code-security-reviewer → SQL注入/权限/残留敏感数据
    │
    不通过 → 返回步骤3修复
    │
    ▼
步骤6: 文档与测试生成
    ├── doc-writing-helper：组件使用手册（README.md）
    ├── unit-test-generator：JUnit 5 + Mockito 测试
    └── (可选) api-doc-generator：API 接口文档
    │
    ▼
【人工卡点3：最终交付审批】
    展示：
    ├── 交付物文件清单
    ├── 三层审查结果
    ├── 测试用例数
    └── 残留业务信息检查结果
    选项：A) 确认交付  B) 修正特定文件  C) 补充遗漏
    │
    ▼
完整交付物：代码 + 配置 + 测试 + 文档
```

## 调用的技能

| 顺序 | 技能名称 | 用途 |
|------|---------|------|
| 1 | code-generator | 按手册生成规范化代码骨架 |
| 2 | component-code-rewriter | 脱敏业务逻辑复写 |
| 3 | config-manager | 配置外化、环境变量管理 |
| 4 | code-style-reviewer | 命名/注释/格式审查 |
| 5 | code-quality-reviewer | N+1/空指针/事务审查 |
| 6 | code-security-reviewer | SQL注入/权限/残留敏感数据审查 |
| 7 | unit-test-generator | JUnit 5 + Mockito 测试生成 |
| 8 | api-doc-generator | API 接口文档（可选） |
| 9 | doc-writing-helper | 组件使用手册（README.md） |

## 目标项目检查

复写开始前，必须检查目标项目环境：

```bash
# 检查项目结构
find src -type d -name "common" -o -name "util" -o -name "config" | head -20

# 检查已有通用类
grep -r "class Result" --include="*.java" src/
grep -r "class BaseEntity" --include="*.java" src/
grep -r "class PageDTO" --include="*.java" src/
grep -r "class BusinessException" --include="*.java" src/

# 检查 pom.xml 依赖
cat pom.xml | grep -E "(redis|rabbitmq|redisson|kafka|mybatis-plus)"
```

如需新增组件依赖，生成 Maven 依赖补充说明，由用户确认后添加。

## 交付物清单

```
accumulation-component-{name}/
├── pom.xml                              # 模块 POM（依赖继承父 POM）
├── README.md                            # 组件使用手册
└── src/
    ├── main/java/com/xxx/component/{name}/
    │   ├── config/XxConfig.java         # @ConfigurationProperties
    │   ├── service/XxService.java       # 服务接口
    │   ├── service/impl/XxServiceImpl.java
    │   ├── common/dto/XxCreateDto.java   # 入参 DTO
    │   ├── common/dto/XxQueryDto.java    # 查询 DTO
    │   ├── common/vo/XxVo.java           # 出参 VO
    │   ├── common/constant/XxConstant.java
    │   └── util/XxUtil.java              # 组件工具类
    ├── main/resources/
    │   └── application-{component}.yml   # 组件独立配置
    └── test/java/com/xxx/component/{name}/
        └── service/XxServiceTest.java    # 单元测试
```

## 强制规范

- 3 个人工卡点必须暂停等待用户确认
- 三层审查必须全部通过才能交付，不通过则返回修复
- 复写代码不得包含源项目的任何业务名称
- 配置中的敏感项必须使用环境变量占位符
- 所有类 100% 符合 aIk-coding-style

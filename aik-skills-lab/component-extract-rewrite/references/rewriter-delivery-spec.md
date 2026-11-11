# 复写交付物规范

## 交付物结构

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

## 3 层审查流程（由 `code-review` 技能执行）

本文件不复制审查规则，复写后直接调用 `code-review` 的对应子命令：

| 层 | 子命令 | 关注点 |
|---|---|---|
| 第1层：风格 | `code-review style` | 命名、注释、格式（对照 `aIk-coding-style`） |
| 第2层：质量 | `code-review quality` | N+1 查询、空指针、事务边界、资源泄漏 |
| 第3层：安全 | `code-review security` | SQL 注入、越权、残留敏感数据 |

**三层全部通过才能交付，不通过则返回修复。**

## 3 个人工检查点

### 检查点1：目标项目确认

复写前检查目标项目：
- pom.xml 已有依赖
- 已有通用组件（ApiResponse / BaseEntity / PageDTO / BusinessException）
- 包结构和集成位置建议
- 确认数据库类型

选项：A) 确认开始  B) 修改包路径/模块名  C) 指定通用组件类名

### 检查点2：脱敏策略审核

展示脱敏去耦方案：
- 命名脱敏：{N} 项
- 值脱敏：{N} 项
- 数据脱敏：{N} 项
- 依赖脱敏：{N} 项
- 配置外化清单：{N} 项

选项：A) 批准策略  B) 调整个别项  C) 重新设计

### 检查点3：最终交付审批

展示：
- 交付物文件清单
- `code-review` 三层审查结果（风格 / 质量 / 安全）
- 测试用例数
- 残留业务信息检查结果

选项：A) 确认交付  B) 修正特定文件  C) 补充遗漏

## 强制规范

- 复写代码不得包含源项目的任何业务名称
- 配置中的敏感项必须使用环境变量占位符
- 所有类 100% 符合 aIk-coding-style
- `code-review` 的 style / quality / security 必须全部通过才能交付

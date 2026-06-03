---
name: spec-implementer
description: 当需要根据系统设计文档（SDD）生成可运行的完整代码时使用。适用于"实现代码"、"开发功能"、"写代码"、"根据设计编码"、"代码实施"等场景。所有代码严格遵循 aIk-coding-style 规范。
type: Sub-agent
version: 1.0.0
---

# spec-implementer

> **重要**：本技能统筹开发实施必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md) 规范。

## 核心规范引用

所有生成的代码必须遵循以下规范：

1. **目录结构**：`common/po/`、`common/dto/`、`common/vo/`、`dao/mapping/`
2. **类注释**：使用 `-anchor` 标记，`@author a I k .`
3. **代码注释**：`//note`（普通）、`//anchor`（关键）
4. **Service Bean**：`{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式
5. **依赖注入**：统一使用 `private final` + `@RequiredArgsConstructor`
6. **PO实体注解**：
   - 无继承：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
   - 有继承：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)` + 显式无参构造
7. **DTO/VO注解**：
   - DTO（独立）：`@Data + @ApiModel`
   - DTO（继承PO）：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`
   - VO（默认）：`@Data + @ApiModel`，必须包含 `of()` 方法
   - VO（复杂）：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`
8. **SQL实现方式**：优先 MyBatis-Plus API，复杂 SQL 才用 XML，不生成空 XML

## 职责

1. 接收 SDD 输入
2. 协调各原子技能执行
3. 整合代码输出
4. 确保代码完整可运行

## 执行流程

```
SDD 输入
    ↓
[检查项目已有组件]
    ├─ 检查 BaseEntity、Result、PageDTO
    ├─ 检查全局异常处理
    ├─ 检查配置类
    └─ 检查数据库类型
    ↓
code-generator             # 生成代码骨架
    ↓
code-implementer           # 填充业务逻辑
    ↓
code-style-reviewer        # 风格审查
    ↓
code-quality-reviewer      # 质量审查
    ↓
code-security-reviewer     # 安全审查
    ↓
unit-test-generator             # 生成单元测试
    ↓
db-migration-generator     # 生成数据库脚本
    ↓
api-doc-generator          # 生成接口文档
    ↓
整合输出完整代码 + 文档
```

## 人机协作节点

### 1. 数据库类型确认

**触发时机**：db-migration-generator 执行前

**询问内容**：
```
请确认项目使用的数据库类型：
1. MySQL
2. PostgreSQL
3. Oracle
4. 其他：____
```

**自动检测**：
```bash
# 尝试自动检测
grep -E "mysql|postgresql|oracle" pom.xml
```

### 2. 全局异常确认

**触发时机**：code-generator 执行前

**检查项目**：
```bash
grep -r "GlobalExceptionHandler\|@ControllerAdvice" --include="*.java" src/
```

**询问内容**（如项目无）：
```
项目未检测到全局异常处理，是否创建？
[ ] 是，创建全局异常处理类
[ ] 否，项目已有其他处理方式
```

### 3. 配置类确认

**触发时机**：code-implementer 执行前（如需缓存、MQ）

**检查项目**：
```bash
grep -r "RedisConfig\|RabbitConfig" --include="*.java" src/
```

**询问内容**（如项目无）：
```
项目未检测到以下配置，是否创建？
[ ] RedisConfig（使用 Redis 缓存）
[ ] RabbitConfig（使用 RabbitMQ）
[ ] 其他：____
```

## 调用的技能

| 顺序 | 技能名称 | 用途 |
|------|---------|------|
| 1 | code-generator | 生成代码骨架 |
| 2 | code-implementer | 填充业务逻辑 |
| 3 | code-style-reviewer | 风格审查 |
| 4 | code-quality-reviewer | 质量审查 |
| 5 | code-security-reviewer | 安全审查 |
| 6 | unit-test-generator | 生成单元测试 |
| 7 | db-migration-generator | 生成数据库脚本 |
| 8 | api-doc-generator | 生成接口文档 |

## 输出结构

```
生成代码/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/xxx/module/
│       │       ├── controller/
│       │       │   └── OrderController.java
│       │       ├── service/
│       │       │   ├── OrderService.java
│       │       │   └── impl/
│       │       │       └── OrderServiceImpl.java
│       │       ├── dao/
│       │       │   └── mapping/
│       │       │       └── OrderMapping.xml
│       │       ├── entity/
│       │       │   └── OrderPo.java
│       │       ├── dto/
│       │       │   ├── OrderCreateDto.java
│       │       │   ├── OrderQueryDto.java
│       │       │   └── OrderItemDto.java
│       │       ├── vo/
│       │       │   ├── OrderVo.java
│       │       │   └── OrderItemVo.java
│       │       └── common/
│       │           ├── constant/
│       │           │   ├── CacheKeyConstant.java
│       │           │   └── ErrorCodeConstant.java
│       │           └── utils/
│       │               └── OrderUtil.java
│       └── resources/
│           ├── dao/
│           │   └── mapping/
│           │       └── OrderMapping.xml
│           └── (可选) db/migration/
│               └── V1.0.0__create_order_table.sql
├── src/
│   └── test/
│       └── java/
│           └── com/xxx/module/
│               └── service/
│                   └── OrderServiceTest.java
├── docs/
│   └── api/
│       └── order-api.md
└── README.md
```

**命名规范**：
- PO实体：`XxPo`（如 `OrderPo`）
- DTO：`XxDto`（如 `OrderCreateDto`）
- VO：`XxVo`（如 `OrderVo`）
- 常量类：`XxConstant`（如 `CacheKeyConstant`）
- XML：`XxMapping.xml`（如 `OrderMapping.xml`）
- **所有类使用 `-anchor` 类注释，`@author a I k .`**
- **注入使用 `private final` + `@RequiredArgsConstructor`，Service指定bean名称 `@Service("{module}.{ServiceName}")` 或 `@Service("{module}.{subModule}.{ServiceName}")`**

## README.md 模板

```
# 订单模块

## 模块说明

订单管理模块，提供订单创建、查询、取消等功能。

## 技术栈

- Java 8
- Spring Boot 2.7.x
- MyBatis-Plus 3.5.x
- MySQL 8.0

## 目录结构

```
├── controller/         # 控制器层
├── service/            # 业务层
│   └── impl/           # 实现类
├── dao/                # 数据访问层
│   └── mapping/        # Mapper XML文件（放在Java源码目录，非resources）
├── common/             # 公共组件
│   ├── po/             # PO实体 (XxPo)
│   ├── dto/            # DTO类 (XxDto)
│   ├── vo/             # VO类 (XxVo)
│   ├── constant/       # 常量类 (XxConstant)
│   ├── enums/          # 枚举类
│   └── utils/          # 工具类 (XxUtil)
└── sql/                # 数据库脚本
```

## 接口列表

| 接口 | 方法 | URL | 说明 |
|------|------|-----|------|
| 创建订单 | POST | /orders | 创建新订单 |
| 查询订单详情 | GET | /orders/{id} | 根据ID查询 |
| 查询订单列表 | GET | /orders | 分页查询 |
| 取消订单 | PUT | /orders/{id}/cancel | 取消订单 |

## 数据库表

| 表名 | 说明 |
|------|------|
| T_ORDER | 订单表 |
| T_ORDER_ITEM | 订单商品表 |

## 文档

- [API 文档](docs/api/order-api.md)

## 更新日志

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| v1.0.0 | 2024-03-18 | 初始版本 |
```

## 使用示例

### 启动命令

```
请帮我实现以下设计的代码：

[粘贴 SDD 内容]

项目技术栈：Java 8 + Spring Boot + MyBatis-Plus + MySQL
```

### 完整执行示例

**用户输入**：
```
基于以下 SDD 实现代码：

## 数据库设计
- T_ORDER 表（订单表）
- T_ORDER_ITEM 表（订单商品表）

## 接口设计
- POST /orders - 创建订单
- GET /orders/{id} - 查询订单详情
- GET /orders - 分页查询订单列表

## 技术选型
- Redis 缓存
- RabbitMQ 延迟队列
```

**执行流程**：

1. **检查项目组件**
   - 发现项目已有 BaseEntity、Result
   - 发现项目已有 RedisConfig
   - 未发现 RabbitMQ 配置

2. **人机协作**
   - 询问：是否创建 RabbitMQ 配置？
   - 用户：是

3. **code-generator**
   - 生成 Entity、Mapper、Service、Controller、DTO、VO
   - 生成 CacheKey、ErrorCode 常量

4. **code-implementer**
   - 实现创建订单逻辑（含事务、缓存）
   - 实现查询逻辑（含缓存）
   - 实现延迟取消逻辑（MQ）

5. **code-style-reviewer**
   - 检查通过

6. **code-quality-reviewer**
   - 发现 1 个警告：未限制最大页码
   - 修复：添加页码限制

7. **code-security-reviewer**
   - 检查通过

8. **unit-test-generator**
   - 生成 OrderServiceTest
   - 覆盖正常、异常、边界场景

9. **db-migration-generator**
   - 生成 V1.0.0__create_order_table.sql

10. **api-doc-generator**
    - 生成 order-api.md

11. **整合输出**
    - 输出完整代码结构
    - 生成 README.md

## 代码规范

- **所有代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范**
- **所有类必须使用 `-anchor` 类注释模板，`@author a I k .`**
- **PO实体使用 `XxPo` 命名**（如 `OrderPo`），放在 `common/po/`
- **DTO使用 `XxDto` 命名**（如 `OrderCreateDto`），放在 `common/dto/`
- **VO使用 `XxVo` 命名**（如 `OrderVo`），放在 `common/vo/`
- **常量类使用 `XxConstant` 命名**（如 `CacheKeyConstant`），放在 `common/constant/`
- **工具类使用 `XxUtil` 命名**（如 `OrderUtil`），放在 `common/utils/`
- **Service Bean名称格式：`{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}`**（如 `@Service("order.OrderService")`）
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- **PO（无继承）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
- **PO（有继承）**：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)` + 显式无参构造
- **DTO（独立）**：`@Data + @ApiModel`
- **DTO（继承PO）**：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`
- **VO（默认）**：`@Data + @ApiModel`，必须包含 `of()` 转换方法
- **VO（复杂）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`
- **代码注释使用 `//note` 和 `//anchor` 标记**
- **禁止行尾注释，if必须使用大括号**
- **SQL实现方式**：优先 MyBatis-Plus API，复杂 SQL 才用 XML，不生成空 XML
- XML文件放在 **Java源代码目录** 的 `dao/mapping/` 下（如 `src/main/java/com/xxx/module/dao/mapping/XxMapping.xml`），**禁止**放在 resources 目录

## 文件编码规范

- **编码格式**：所有文件必须使用 **UTF-8 无 BOM** 格式
- **禁止 BOM**：文件开头不能包含 UTF-8 BOM 标记（`\uFEFF` 零宽不间断空格）
- **注释编码**：所有中文注释必须使用 UTF-8 编码，禁止出现乱码（如 `�`）
- **换行符**：使用 Unix 风格换行符（LF），避免 Windows 风格（CRLF）

## 注意事项

- **所有代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范**
- 优先复用项目已有组件
- 必要的人机协作节点要清晰
- 审查发现的问题要修复后再输出
- 文档要完整，便于后续维护
- 代码要符合 Java 8 编码规范
- **类注释使用 `-anchor` 格式，`@author a I k .`**
- **代码注释使用 `//note` 和 `//anchor` 标记**
- **Service Bean名称使用 `{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式**
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- **编码检查**：输出代码前必须验证无 BOM 字符、无乱码

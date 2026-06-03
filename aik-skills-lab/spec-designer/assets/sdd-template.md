# 系统设计文档 (SDD)

> 生成时间: {yyyy-MM-dd}
> 版本: v{version}
> 基于 PRD: {PRD版本}

---

## 1. 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | {项目名称} |
| 文档版本 | v{version} |
| 创建日期 | {yyyy-MM-dd} |
| 设计人 | {designer} |
| 审核人 | {reviewer} |
| 状态 | 草稿 / 已审核 / 已确认 |

---

## 2. 架构设计

### 2.1 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 后端框架 | Spring Boot | {version} | 主框架 |
| ORM | MyBatis-Plus | {version} | 数据访问层 |
| 数据库 | MySQL | 8.0 | 关系型数据库 |
| 缓存 | Redis | 7.0 | 分布式缓存 |
| 消息队列 | RabbitMQ | 3.x | 异步消息 |
| 构建工具 | Maven | 3.x | 项目构建 |

### 2.2 包结构

```
src/main/java/com/{company}/{project}/
├── common/              # 通用组件
│   ├── base/            # 基类（BaseEntity、BaseService）
│   ├── config/          # 配置类
│   ├── constant/        # 常量定义
│   ├── exception/       # 异常定义
│   ├── result/          # 统一返回（Result）
│   └── util/            # 工具类
├── module/              # 业务模块
│   └── {moduleName}/
│       ├── controller/  # 控制器
│       ├── service/     # 服务接口与实现
│       │   └── impl/
│       ├── mapper/      # MyBatis Mapper
│       ├── entity/      # PO实体
│       │   ├── po/      # 持久化对象
│       │   ├── dto/     # 数据传输对象
│       │   └── vo/      # 视图对象
│       └── constant/    # 模块常量
└── Application.java     # 启动类

src/main/resources/
├── application.yml      # 主配置
├── application-dev.yml  # 开发环境配置
└── mapper/              # Mapper XML
```

### 2.3 分层职责

| 层级 | 职责 | 约束 |
|------|------|------|
| Controller | 接收请求、参数校验、调用Service、返回响应 | 不包含业务逻辑 |
| Service | 业务逻辑实现、事务管理、调用Mapper | 不直接处理HTTP请求 |
| Mapper | 数据访问、SQL执行 | 不包含业务逻辑 |
| Entity | 数据载体 | PO/DTO/VO 各司其职 |

### 2.4 通用组件（复用项目已有）

| 组件 | 来源 | 说明 |
|------|------|------|
| Result | com.{company}.common.result.Result | 统一返回格式 |
| BaseEntity | com.{company}.common.base.BaseEntity | 通用实体基类 |
| PageDTO | com.{company}.common.base.PageDTO | 分页参数 |
| BaseService | com.{company}.common.base.BaseService | 通用Service基类 |

### 2.5 模块划分

| 模块 | 名称 | 依赖模块 |
|------|------|---------|
| {module1} | {模块描述} | 无 |
| {module2} | {模块描述} | {module1} |

---

## 3. 数据库设计

### 3.1 ER 图

```plantuml
@startuml
entity T_{TABLE1} {
    * ID : BIGINT <<PK>>
    --
    * {FIELD} : {TYPE} <<UK>>
    * STATUS : TINYINT
    * DELETED : TINYINT
    * CREATE_TIME : DATETIME
    * UPDATE_TIME : DATETIME
}

entity T_{TABLE2} {
    * ID : BIGINT <<PK>>
    --
    * {FK_FIELD} : BIGINT <<FK>>
    * STATUS : TINYINT
    * DELETED : TINYINT
    * CREATE_TIME : DATETIME
    * UPDATE_TIME : DATETIME
}

T_{TABLE1} ||--o{ T_{TABLE2} : {关系描述}
@enduml
```

### 3.2 表结构设计

#### T_{TABLE_NAME}（{表说明}）

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| ID | BIGINT | 20 | 否 | AUTO_INCREMENT | 主键 |
| {FIELD_NAME} | {TYPE} | {LENGTH} | 是否 | {DEFAULT} | {说明} |
| STATUS | TINYINT | 1 | 否 | 0 | 状态 |
| DELETED | TINYINT | 1 | 否 | 0 | 逻辑删除：0-未删除，1-已删除 |
| CREATE_TIME | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| UPDATE_TIME | DATETIME | - | 否 | CURRENT_TIMESTAMP | 更新时间 |
| CREATE_BY | BIGINT | 20 | 是 | - | 创建人ID |
| UPDATE_BY | BIGINT | 20 | 是 | - | 更新人ID |

**索引**：

```sql
PRIMARY KEY (ID),
UNIQUE KEY UK_{FIELD} ({FIELD}),
KEY IDX_{FIELD} ({FIELD}),
KEY IDX_{FIELD1}_{FIELD2} ({FIELD1}, {FIELD2})
```

### 3.3 MyBatis-Plus 实体类

```java
/**
 * -anchor {实体描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("{TABLE_NAME}")
public class {Entity}Po extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("{FIELD_NAME}")
    private {Type} {fieldName};

    @TableField("{FIELD_NAME}")
    private {Type} {fieldName};
}
```

### 3.4 建表 SQL

```sql
CREATE TABLE {TABLE_NAME} (
    ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    {FIELD_NAME} {TYPE} NOT NULL COMMENT '{说明}',
    -- 审计字段
    DELETED TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UPDATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CREATE_BY BIGINT COMMENT '创建人ID',
    UPDATE_BY BIGINT COMMENT '更新人ID',
    -- 索引
    PRIMARY KEY (ID),
    UNIQUE KEY UK_{FIELD} ({FIELD}),
    KEY IDX_{FIELD} ({FIELD})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{表说明}';
```

---

## 4. API 设计

### 4.1 API 列表

| 接口 | 方法 | URL | 说明 | 权限 |
|------|------|-----|------|------|
| {接口名称} | {HTTP方法} | {URL路径} | {说明} | {权限要求} |
| {接口名称} | {HTTP方法} | {URL路径} | {说明} | {权限要求} |

### 4.2 请求/响应定义

#### {接口名称}

**请求**：

```java
@Data
@ApiModel("{DTO说明}")
public class {Xx}Dto {

    @ApiModelProperty(value = "{字段说明}", required = true)
    @NotNull(message = "{字段}不能为空")
    private {Type} {fieldName};

    @ApiModelProperty(value = "{字段说明}")
    private {Type} {fieldName};
}
```

**响应**：

```java
Result<{Xx}Vo>

@Data
@ApiModel("{VO说明}")
public class {Xx}Vo {

    @ApiModelProperty("{字段说明}")
    private {Type} {fieldName};
}
```

### 4.3 错误码定义

| 错误码 | HTTP状态码 | 说明 | 场景 |
|--------|-----------|------|------|
| 200 | 200 | 成功 | 正常返回 |
| 400 | 400 | 参数错误 | 参数校验失败 |
| 401 | 401 | 未授权 | 未登录或Token失效 |
| 403 | 403 | 禁止访问 | 无权限 |
| 404 | 404 | 资源不存在 | 数据不存在 |
| 500 | 500 | 系统错误 | 服务器内部错误 |
| {业务码} | {HTTP码} | {说明} | {场景} |

---

## 5. 核心流程设计

### 5.1 业务流程时序图

```
用户 -> Controller: {请求}
Controller -> Service: {方法调用}
Service -> Mapper: {数据查询}
Mapper --> Service: {返回数据}
Service -> Service: {业务处理}
Service -> Mapper: {数据保存}
Mapper --> Service: {返回结果}
Service --> Controller: {VO}
Controller --> 用户: Result
```

### 5.2 状态机设计

```
{状态1} --> {状态2}: {触发条件}
{状态2} --> {状态3}: {触发条件}
{状态3} --> {状态4}: {触发条件}
{状态2} --> {状态5}: {异常触发条件}
```

### 5.3 事务设计

| 操作 | 事务边界 | 传播行为 | 回滚条件 |
|------|---------|---------|---------|
| {操作名称} | {方法名} | REQUIRED | RuntimeException |
| {操作名称} | {方法名} | REQUIRES_NEW | Exception |

---

## 6. 技术选型

### 6.1 缓存方案

- **缓存方式**: Redis
- **缓存策略**: Cache-Aside（旁路缓存）
- **缓存Key**: `{project}:{module}:{id}`
- **过期时间**: {X}分钟

### 6.2 消息队列

- **MQ选型**: RabbitMQ
- **使用场景**: {场景描述}
- **队列设计**:

| 队列名 | 交换机 | 路由键 | 消费者 |
|--------|--------|--------|--------|
| {queueName} | {exchange} | {routingKey} | {consumer} |

### 6.3 分布式锁

- **锁方式**: Redisson
- **锁Key**: `lock:{resource}:{id}`
- **等待时间**: {X}秒
- **持锁时间**: {X}秒

### 6.4 异步处理

- **方式**: @Async / MQ
- **场景**: {场景描述}

---

## 7. 设计评审

### 7.1 评审结论

| 项目 | 结论 |
|------|------|
| 架构合理性 | 通过 / 需修改 |
| 数据库设计 | 通过 / 需修改 |
| 接口设计 | 通过 / 需修改 |
| 流程设计 | 通过 / 需修改 |
| 技术选型 | 通过 / 需修改 |

### 7.2 待解决问题

| 编号 | 问题 | 严重程度 | 建议方案 | 负责人 | 状态 |
|------|------|---------|---------|--------|------|
| I-001 | {问题描述} | 高/中/低 | {建议} | {负责人} | 待解决 |
| I-002 | {问题描述} | 高/中/低 | {建议} | {负责人} | 待解决 |

### 7.3 风险点

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| {风险描述} | 高/中/低 | 高/中/低 | {措施} |

### 7.4 优化建议

- {建议1}
- {建议2}

---

## 8. 附录

### 8.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | 大写下划线，T_前缀 | T_ORDER |
| 字段 | 大写下划线 | ORDER_NO |
| 索引 | IDX_/UK_前缀 | IDX_USER_ID, UK_ORDER_NO |
| Java类 | 驼峰命名 | OrderService |
| PO | XxPo | OrderPo |
| DTO | XxDto | OrderCreateDto |
| VO | XxVo | OrderVo |
| Service Bean | {module}.{Name} | order.OrderService |

### 8.2 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | {yyyy-MM-dd} | 初始版本 | {author} |

### 8.3 参考文档

- {PRD名称}: {路径}
- {技术规范}: {路径}

---

> 本文档由 spec-designer 生成，用于指导后续开发实施工作。

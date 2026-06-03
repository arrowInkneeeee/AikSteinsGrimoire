# 数据库表设计文档

> 生成时间: {yyyy-MM-dd}
> 设计版本: v{version}
> 模块: {模块名}

---

## 一、设计概述

| 项目 | 内容 |
|------|------|
| 模块名称 | {模块名} |
| 数据库类型 | MySQL / PostgreSQL / Oracle |
| 字符集 | utf8mb4 |
| 存储引擎 | InnoDB |
| 设计人 | {designer} |

---

## 二、ER 图

```plantuml
@startuml
entity T_{TABLE1} {
    * ID : BIGINT <<PK>>
    --
    * {FIELD1} : {TYPE} <<UK>>
    * {FIELD2} : {TYPE}
    * STATUS : TINYINT
    * DELETED : TINYINT
    * CREATE_TIME : DATETIME
    * UPDATE_TIME : DATETIME
    * CREATE_BY : BIGINT
    * UPDATE_BY : BIGINT
}

entity T_{TABLE2} {
    * ID : BIGINT <<PK>>
    --
    * {FK_FIELD} : BIGINT <<FK>>
    * {FIELD} : {TYPE}
    * STATUS : TINYINT
    * DELETED : TINYINT
    * CREATE_TIME : DATETIME
    * UPDATE_TIME : DATETIME
}

T_{TABLE1} ||--o{ T_{TABLE2} : {关系描述}
@enduml
```

---

## 三、表结构设计

### 3.1 {T_TABLE_NAME}（{表说明}）

#### 字段定义

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| ID | BIGINT | 20 | 否 | AUTO_INCREMENT | 主键 |
| {FIELD_NAME} | {TYPE} | {LENGTH} | 否/是 | {DEFAULT} | {字段说明} |
| STATUS | TINYINT | 1 | 否 | 0 | 状态：{枚举说明} |
| DELETED | TINYINT | 1 | 否 | 0 | 逻辑删除：0-未删除，1-已删除 |
| CREATE_TIME | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| UPDATE_TIME | DATETIME | - | 否 | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| CREATE_BY | BIGINT | 20 | 是 | - | 创建人ID |
| UPDATE_BY | BIGINT | 20 | 是 | - | 更新人ID |

#### 索引设计

| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| PRIMARY | 主键 | ID | 主键索引 |
| UK_{FIELD} | 唯一索引 | {FIELD} | 业务唯一约束 |
| IDX_{FIELD} | 普通索引 | {FIELD} | 查询优化 |
| IDX_{FIELD1}_{FIELD2} | 联合索引 | {FIELD1}, {FIELD2} | 联合查询优化 |

#### 索引 SQL

```sql
PRIMARY KEY (ID),
UNIQUE KEY UK_{FIELD} ({FIELD}),
KEY IDX_{FIELD} ({FIELD}),
KEY IDX_{FIELD1}_{FIELD2} ({FIELD1}, {FIELD2})
```

---

### 3.2 {T_TABLE_NAME2}（{表说明}）

{重复上述结构}

---

## 四、关联关系设计

### 4.1 一对一

```sql
-- {主表}与{从表}一对一关联
-- 从表包含外键，使用唯一索引保证一对一
CREATE TABLE {T_DETAIL} (
    ID BIGINT PRIMARY KEY,
    {MASTER}_ID BIGINT NOT NULL COMMENT '{主表}ID',
    UNIQUE KEY UK_{MASTER}_ID ({MASTER}_ID)
);
```

### 4.2 一对多

```sql
-- {主表}一对多{从表}
-- 从表包含外键，建立普通索引
CREATE TABLE {T_DETAIL} (
    ID BIGINT PRIMARY KEY,
    {MASTER}_ID BIGINT NOT NULL COMMENT '{主表}ID',
    KEY IDX_{MASTER}_ID ({MASTER}_ID)
);
```

### 4.3 多对多

```sql
-- {表A}与{表B}多对多，通过中间表关联
CREATE TABLE {T_RELATION} (
    ID BIGINT PRIMARY KEY,
    {A}_ID BIGINT NOT NULL COMMENT '{表A}ID',
    {B}_ID BIGINT NOT NULL COMMENT '{表B}ID',
    UNIQUE KEY UK_{A}_{B} ({A}_ID, {B}_ID),
    KEY IDX_{A}_ID ({A}_ID),
    KEY IDX_{B}_ID ({B}_ID)
);
```

---

## 五、索引设计原则

| 原则 | 说明 |
|------|------|
| 主键索引 | 每个表必须有主键，使用 BIGINT 自增 |
| 唯一索引 | 业务唯一字段（订单号、手机号等） |
| 外键索引 | 关联字段必须加索引 |
| 查询索引 | WHERE、ORDER BY、GROUP BY 字段 |
| 联合索引 | 遵循最左前缀原则，区分度高的放左边 |
| 避免冗余 | 已有联合索引 (A,B) 则不需要单独索引 (A) |

---

## 六、完整建表 SQL

```sql
-- =============================================
-- 模块：{模块名}
-- 版本：v{version}
-- 数据库类型：MySQL
-- =============================================

-- {表说明}
CREATE TABLE {T_TABLE_NAME} (
    ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    {FIELD_NAME} {TYPE} NOT NULL COMMENT '{说明}',
    {FIELD_NAME} {TYPE} NOT NULL DEFAULT {DEFAULT} COMMENT '{说明}',
    STATUS TINYINT NOT NULL DEFAULT 0 COMMENT '状态：{枚举说明}',
    DELETED TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UPDATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CREATE_BY BIGINT COMMENT '创建人ID',
    UPDATE_BY BIGINT COMMENT '更新人ID',
    PRIMARY KEY (ID),
    UNIQUE KEY UK_{FIELD} ({FIELD}),
    KEY IDX_{FIELD} ({FIELD}),
    KEY IDX_{FIELD1}_{FIELD2} ({FIELD1}, {FIELD2})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{表说明}';

-- {表说明2}
CREATE TABLE {T_TABLE_NAME2} (
    -- ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{表说明2}';
```

---

## 七、MyBatis-Plus 实体类

### {Entity}Po

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

---

## 八、字段类型参考

| 数据类型 | Java 类型 | 长度建议 | 使用场景 |
|---------|----------|---------|---------|
| BIGINT | Long | 20 | 主键、外键、大整数 |
| VARCHAR | String | 按需求 | 短文本（< 5000 字符） |
| TEXT | String | - | 长文本（> 5000 字符） |
| DECIMAL(M,D) | BigDecimal | (10,2) | 金额、精确小数 |
| INT | Integer | 11 | 状态、计数 |
| TINYINT | Integer/Boolean | 1 | 布尔值、小范围状态 |
| DATETIME | LocalDateTime | - | 日期时间 |
| DATE | LocalDate | - | 日期 |

---

## 九、命名规范速查

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | 大写下划线，T_前缀 | T_ORDER, T_USER |
| 主键 | ID, BIGINT | ID |
| 字段名 | 大写下划线 | ORDER_NO, USER_ID |
| 唯一索引 | UK_字段名 | UK_ORDER_NO |
| 普通索引 | IDX_字段名 | IDX_USER_ID |
| 联合索引 | IDX_字段1_字段2 | IDX_USER_ID_STATUS |
| PO类 | {Entity}Po | OrderPo |

---

## 十、变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v{version} | {yyyy-MM-dd} | 初始版本 | {author} |

---

> 数据库设计需与团队评审确认后方可执行建表。

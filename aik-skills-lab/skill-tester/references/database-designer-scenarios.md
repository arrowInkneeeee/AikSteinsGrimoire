# 数据库设计 测试场景

> 针对技能: `database-designer`
> 测试类型: 技术型
> 规范来源: [aIk-coding-style](../../aIk-coding-style/SKILL.md)

---

## 测试场景概述

| 测试用例 | 场景描述 | 预期违规数量 | 严重级别 |
|---------|---------|-------------|---------|
| TC-01 | 缺少外键字段索引 | 1 | 警告 |
| TC-02 | 使用 FLOAT 存储金额字段 | 1 | 严重 |
| TC-03 | 缺少审计字段（create_time, modify_time） | 2 | 警告 |
| TC-04 | 正确的表设计（所有规范均已遵循） | 0 | - |

---

## TC-01: 缺少外键字段索引

### 违规DDL

```sql
-- 订单表
CREATE TABLE t_order
(
    id              BIGINT          NOT NULL COMMENT '主键' PRIMARY KEY,
    order_no        VARCHAR(64)     NOT NULL COMMENT '订单号',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    total_amount    DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '订单表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单商品表
CREATE TABLE t_order_item
(
    id              BIGINT          NOT NULL COMMENT '主键' PRIMARY KEY,
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    item_id         BIGINT          NOT NULL COMMENT '商品ID',
    quantity        INT             NOT NULL DEFAULT 1 COMMENT '数量',
    price           DECIMAL(10,2)   NOT NULL COMMENT '单价',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '订单商品表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | `t_order.user_id` 缺少索引 | 外键字段 `user_id` 是常见的关联查询条件，缺少索引会导致全表扫描。应添加 `INDEX idx_user_id (user_id)` | 警告 |
| 2 | `t_order_item.order_id` 缺少索引 | 外键字段 `order_id` 用于关联查询订单商品，缺少索引会导致关联查询性能极差。应添加 `INDEX idx_order_id (order_id)` | 警告 |

### 修复建议

```sql
-- 添加外键索引
ALTER TABLE t_order ADD INDEX idx_user_id (user_id);
ALTER TABLE t_order_item ADD INDEX idx_order_id (order_id);
```

### 规范的建表语句示例

```sql
CREATE TABLE t_order_item
(
    id              BIGINT          NOT NULL COMMENT '主键' PRIMARY KEY,
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    item_id         BIGINT          NOT NULL COMMENT '商品ID',
    quantity        INT             NOT NULL DEFAULT 1 COMMENT '数量',
    price           DECIMAL(10,2)   NOT NULL COMMENT '单价',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id),
    INDEX idx_item_id (item_id)
) COMMENT '订单商品表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## TC-02: 使用 FLOAT 存储金额

### 违规DDL

```sql
-- 订单表（金额字段使用 FLOAT 类型）
CREATE TABLE t_order
(
    id                  BIGINT          NOT NULL COMMENT '主键' PRIMARY KEY,
    order_no            VARCHAR(64)     NOT NULL COMMENT '订单号',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    total_amount        FLOAT           NOT NULL DEFAULT 0 COMMENT '订单总金额',
    discount_amount     FLOAT           NOT NULL DEFAULT 0 COMMENT '折扣金额',
    paid_amount         DOUBLE          NOT NULL DEFAULT 0 COMMENT '实付金额',
    status              TINYINT         NOT NULL DEFAULT 0 COMMENT '状态',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) COMMENT '订单表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | `total_amount FLOAT` | FLOAT/DOUBLE 是浮点类型，存在精度丢失问题。金额字段必须使用 `DECIMAL(precision, scale)` 类型 | 严重 |
| 2 | `discount_amount FLOAT` | 同上，折扣金额也需要精确的小数计算 | 严重 |
| 3 | `paid_amount DOUBLE` | DOUBLE 同样存在精度问题，应使用 `DECIMAL(10,2)` | 严重 |

### 修复建议

```sql
-- 金额字段统一使用 DECIMAL 类型
total_amount        DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
discount_amount     DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '折扣金额',
paid_amount         DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '实付金额',
```

### 金额类型规范

| 字段场景 | 推荐类型 | 示例 |
|---------|---------|------|
| 订单金额 | `DECIMAL(12,2)` | 支持千亿级金额 |
| 单价 | `DECIMAL(10,2)` | 支持千万级单价 |
| 税率/百分比 | `DECIMAL(5,4)` | 如 0.0600 表示 6% |
| 积分/数量 | `BIGINT` | 整数类型 |

---

## TC-03: 缺少审计字段

### 违规DDL

```sql
-- 订单表（缺少 create_time 和 modify_time）
CREATE TABLE t_order
(
    id                  BIGINT          NOT NULL COMMENT '主键' PRIMARY KEY,
    order_no            VARCHAR(64)     NOT NULL COMMENT '订单号',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    total_amount        DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    status              TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付',
    remark              VARCHAR(500)    NULL COMMENT '备注',
    INDEX idx_user_id (user_id)
) COMMENT '订单表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表（缺少审计字段）
CREATE TABLE t_product
(
    id                  BIGINT          NOT NULL COMMENT '主键' PRIMARY KEY,
    product_name        VARCHAR(255)    NOT NULL COMMENT '商品名称',
    price               DECIMAL(10,2)   NOT NULL COMMENT '价格',
    stock               INT             NOT NULL DEFAULT 0 COMMENT '库存',
    INDEX idx_product_name (product_name)
) COMMENT '商品表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | `t_order` 缺少 `create_time` | 每张业务表都应包含 `create_time` 字段用于记录数据创建时间，便于追溯和审计 | 警告 |
| 2 | `t_order` 缺少 `modify_time` | 每张业务表都应包含 `modify_time`（或 `update_time`）字段用于记录最后一次修改时间 | 警告 |
| 3 | `t_product` 缺少 `create_time` 和 `modify_time` | 同上，每张业务表都需要审计字段 | 警告 |

### 修复建议

```sql
-- 添加审计字段（在表定义中）
create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
modify_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

-- 可选：创建人/更新人字段
create_user_id  BIGINT      NULL COMMENT '创建人ID',
create_user     VARCHAR(128) NULL COMMENT '创建人',
```

---

## TC-04: 正确的表设计（所有规范均已遵循）

### 规范DDL

```sql
-- 订单表
CREATE TABLE t_order
(
    id                  BIGINT          NOT NULL COMMENT '主键'
        PRIMARY KEY,
    order_no            VARCHAR(64)     NOT NULL COMMENT '订单号',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    total_amount        DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    status              TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已取消',
    remark              VARCHAR(500)    NULL COMMENT '备注',
    create_user_id      BIGINT          NULL COMMENT '创建人id',
    create_user         VARCHAR(128)    NULL COMMENT '创建人',
    create_time         DATETIME        NULL COMMENT '创建时间',
    modify_time         DATETIME        NULL COMMENT '更新时间'
)
    COMMENT '订单表' ROW_FORMAT = DYNAMIC;

-- 唯一索引：订单号
CREATE UNIQUE INDEX uk_order_no ON t_order(order_no);

-- 普通索引：用户ID（外键关联）
CREATE INDEX idx_user_id ON t_order(user_id);

-- 普通索引：创建时间（时间范围查询）
CREATE INDEX idx_create_time ON t_order(create_time);

-- 组合索引：用户+状态（按用户查询特定状态订单）
CREATE INDEX idx_user_id_status ON t_order(user_id, status);
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| - | 无违规 | 所有数据库设计规范均已正确遵循 | - |

### 通过项清单

- [x] 表名使用小写下划线命名，有意义的业务前缀
- [x] 主键使用 `BIGINT NOT NULL PRIMARY KEY`
- [x] 金额字段使用 `DECIMAL(12,2)` 精确类型
- [x] 外键字段 `user_id` 已添加索引 `idx_user_id`
- [x] 包含审计字段 `create_time`、`modify_time`、`create_user_id`
- [x] 唯一索引使用 `uk_` 前缀（`uk_order_no`）
- [x] 普通索引使用 `idx_` 前缀
- [x] 索引包含常用查询字段（用户ID、创建时间）
- [x] 组合索引遵循最左前缀原则
- [x] 字符集使用 `utf8mb4`，存储引擎使用 `InnoDB`
- [x] 字段和表均有 `COMMENT` 注释
- [x] 表使用 `ROW_FORMAT = DYNAMIC`

### 规范的 Java 实体类

```java
package com.example.order.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * -anchor 订单实体
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单实体")
@TableName("t_order")
public class OrderPo {

    /**
     * 主键ID
     */
    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 订单号
     */
    @ApiModelProperty("订单号")
    @TableField("order_no")
    private String orderNo;

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private Long userId;

    /**
     * 订单总金额
     */
    @ApiModelProperty("订单总金额")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 状态
     */
    @ApiModelProperty("状态：0-待支付，1-已支付，2-已取消")
    @TableField("status")
    private Integer status;

    /**
     * 创建人id
     */
    @ApiModelProperty("创建人id")
    @TableField("create_user_id")
    private Long createUserId;

    /**
     * 创建人
     */
    @ApiModelProperty("创建人")
    @TableField("create_user")
    private String createUser;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField("modify_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifyTime;
}
```

---

## RED-GREEN-REFACTOR 执行参考

### RED阶段（无技能）

```
预期结果: 模型可能识别出明显的类型错误但忽略细节
- TC-01 (缺少外键索引): 容易被忽略
- TC-02 (FLOAT金额): 容易检测到
- TC-03 (缺少审计字段): 可能部分遗漏
- 漏检率预估: 40-60%
```

### GREEN阶段（加载技能）

```
验证标准:
- 违规检测率 > 90%
- 能准确识别索引缺失、类型问题、审计字段缺失
- 误报率 < 10%
- 能生成符合规范的修复DDL
```

### REFACTOR阶段（迭代收紧）

```
常见遗漏:
- 对字符集 utf8 vs utf8mb4 的检查
- 对索引命名规范的检查（IDX_ vs idx_）
- 对 ROW_FORMAT 的检查
- 对逻辑删除字段 deleted 的检查
- 对 VARCHAR 长度合理性的检查
```

# SQL 迁移脚本

> 数据库：{MySQL/PostgreSQL/Oracle}
> 工具：{Flyway/Liquibase/纯SQL}
> 模块：{模块名}
> 版本：v{version}

---

## 一、脚本信息

| 项目 | 内容 |
|------|------|
| 脚本名称 | {V1.0.0__create_{module}_table.sql} |
| 版本号 | {version} |
| 数据库类型 | MySQL / PostgreSQL / Oracle |
| 迁移工具 | Flyway / Liquibase / 纯SQL |
| 是否幂等 | 是 / 否 |
| 可回滚 | 是 / 否 |

---

## 二、前置检查

### 2.1 数据库连接确认

```bash
# MySQL 连接测试
mysql -h {host} -P {port} -u {user} -p

# PostgreSQL 连接测试
psql -h {host} -p {port} -U {user} -d {database}
```

### 2.2 依赖表检查

| 表名 | 是否存在 | 说明 |
|------|---------|------|
| {T_TABLE} | 是 | 前置依赖表 |

### 2.3 数据备份

```sql
-- 备份目标表（如已有数据）
CREATE TABLE {T_TABLE}_bak_{date} AS SELECT * FROM {T_TABLE};
```

---

## 三、DDL 脚本

### 3.1 建表

```sql
-- =============================================
-- 表名：{T_TABLE_NAME}
-- 说明：{表说明}
-- 版本：v{version}
-- =============================================

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
```

### 3.2 添加索引

```sql
-- 为已有表添加索引
ALTER TABLE {T_TABLE_NAME}
    ADD UNIQUE KEY UK_{FIELD} ({FIELD});

ALTER TABLE {T_TABLE_NAME}
    ADD KEY IDX_{FIELD} ({FIELD});

ALTER TABLE {T_TABLE_NAME}
    ADD KEY IDX_{FIELD1}_{FIELD2} ({FIELD1}, {FIELD2});
```

### 3.3 修改表结构

```sql
-- 添加字段
ALTER TABLE {T_TABLE_NAME}
    ADD COLUMN {NEW_FIELD} {TYPE} NOT NULL DEFAULT {DEFAULT} COMMENT '{说明}';

-- 修改字段
ALTER TABLE {T_TABLE_NAME}
    MODIFY COLUMN {FIELD} {NEW_TYPE} NOT NULL COMMENT '{说明}';

-- 删除字段
ALTER TABLE {T_TABLE_NAME}
    DROP COLUMN {FIELD};
```

---

## 四、DML 脚本（数据初始化）

```sql
-- 初始化数据
INSERT INTO {T_TABLE_NAME} ({FIELD1}, {FIELD2}, STATUS) VALUES
('value1', 'value2', 0),
('value3', 'value4', 1);

-- 更新数据
UPDATE {T_TABLE_NAME}
SET STATUS = 1
WHERE STATUS = 0 AND CREATE_TIME < '2024-01-01';
```

---

## 五、回滚脚本

```sql
-- =============================================
-- 回滚脚本：v{version} -> v{prev_version}
-- =============================================

-- 删除新增的表
DROP TABLE IF EXISTS {T_TABLE_NAME};

-- 删除新增的索引
ALTER TABLE {T_TABLE_NAME}
    DROP INDEX UK_{FIELD};

-- 删除新增的字段
ALTER TABLE {T_TABLE_NAME}
    DROP COLUMN {FIELD};

-- 恢复修改的字段
ALTER TABLE {T_TABLE_NAME}
    MODIFY COLUMN {FIELD} {ORIGINAL_TYPE};
```

---

## 六、执行说明

### 6.1 执行步骤

1. **备份数据**：执行前备份目标表数据
2. **验证环境**：确认连的是正确的数据库环境
3. **执行 SQL**：按顺序执行 DDL -> 索引 -> DML
4. **验证结果**：检查表结构、索引、数据是否正确
5. **记录日志**：记录执行时间和结果

### 6.2 注意事项

- 生产环境执行前需在测试环境验证
- 大表 ALTER TABLE 操作需避开业务高峰期
- 确保 SQL 脚本可重复执行（使用 `IF NOT EXISTS` 或 `DROP IF EXISTS`）
- 记录每次执行的脚本版本和结果

---

## 七、Flyway 命名规范

```
src/main/resources/db/migration/
├── V1.0.0__create_{module}_table.sql
├── V1.0.1__add_{module}_index.sql
├── V1.1.0__create_{module2}_table.sql
└── V1.1.1__add_{module}_column.sql
```

**命名规则**：`V{大版本}.{小版本}.{修订号}__{描述}.sql`

---

## 八、执行记录

| 版本 | 执行日期 | 执行环境 | 执行人 | 结果 | 备注 |
|------|---------|---------|--------|------|------|
| v{version} | {yyyy-MM-dd} | 开发 | {name} | 成功 | - |
| v{version} | {yyyy-MM-dd} | 测试 | {name} | 成功 | - |
| v{version} | {yyyy-MM-dd} | 生产 | {name} | 成功 | - |

---

> SQL 脚本需经过代码审查并在测试环境验证后方可执行至生产环境。

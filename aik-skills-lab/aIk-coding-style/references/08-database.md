# 数据库规范

> 来源：aIk-coding-style 规范

## 建表语句模板

```sql
-- {表描述}
CREATE TABLE {table_name}
(
    id                  bigint          not null comment '主键'
        primary key,
    field_name          varchar(128)    null comment '字段描述',
    create_user_id      bigint          null comment '创建人id',
    create_user         varchar(128)    null comment '创建人',
    create_time         datetime        null comment '创建时间',
    modify_time         datetime        null comment '更新时间'
)
    comment '{表描述}' row_format = DYNAMIC;

-- 索引（根据业务需要添加）
CREATE INDEX idx_field_name ON {table_name}(field_name);
CREATE INDEX idx_create_time ON {table_name}(create_time);
```

## 字段长度规范

| 字段类型 | 长度 | 说明 |
|---------|------|------|
| 普通文本 | varchar(128) ~ varchar(255) | 名称、编号等 |
| 长文本 | varchar(1024) | 描述、备注 |
| 图片IDs | varchar(500) | 多张图片ID逗号分隔 |
| 图片名称 | varchar(2000) | 多张图片名称逗号分隔 |
| 备注/描述 | varchar(1024) | 长文本字段 |

## 索引策略

- 单字段索引：常用查询字段
- 组合索引：多级联动查询（如树形结构）
- 时间索引：`create_time`, `modify_time`, 业务时间字段

## SQL文件位置

- 放在模块包下的 `sql/` 目录
- 文件名：`{table_name}.sql`
- 包含：建表语句 + 索引语句

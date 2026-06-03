# 代码注释规范

> 来源：aIk-coding-style 规范 · 行注释格式与规则

### 行注释规范

| 注释类型 | 格式 | 使用场景 |
|---------|------|---------|
| 普通注释 | `//note {内容}` | 一般代码说明 |
| 关键注释 | `//anchor {内容}` | 关键逻辑、重要业务点 |

### 注释规则
- **禁止行尾注释**：注释必须单独一行，不能跟在代码后面
- **if必须使用大括号**：即使单行也要用 `{}`
- **关键代码必须有注释**：复杂逻辑、业务规则等

### 示例
```java
//note 参数校验
if (StrUtil.isBlank(dto.getFieldName())) {
    throw new BusinessException("参数错误：fieldName不能为空");
}

//anchor 构建实体并设置审计字段
{Entity}Po entity = new {Entity}Po();
BeanUtil.copyProperties(dto, entity);
entity.setId(IdUtil.getSnowflakeNextId());
```

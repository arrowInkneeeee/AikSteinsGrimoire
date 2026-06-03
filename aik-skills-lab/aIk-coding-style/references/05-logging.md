# 日志规范

> 来源：aIk-coding-style 规范 · 日志级别与使用要求

### 使用要求
- **Controller层**：必须添加 `@Slf4j` 注解
- **ServiceImpl层**：必须添加 `@Slf4j` 注解
- **关键信息**：使用 `log.info()` 输出
- **异常情况**：使用 `log.error()` 输出

### 日志级别使用
| 级别 | 使用场景 |
|------|---------|
| `log.info()` | 业务流程关键节点、重要操作记录 |
| `log.error()` | 异常、错误、失败操作 |
| `log.debug()` | 调试信息（可选） |

### 示例
```java
@Slf4j
@Service("{module}.{Entity}Service")
public class {Entity}ServiceImpl extends ServiceImpl<{Entity}Mapper, {Entity}Po>
        implements {Entity}Service {

    @Override
    public {Entity}Po add({Entity}Dto dto) {
        //note 参数校验
        if (StrUtil.isBlank(dto.getFieldName())) {
            log.error("新增失败，参数错误：fieldName为空");
            throw new BusinessException("参数错误：fieldName不能为空");
        }

        //anchor 构建实体
        {Entity}Po entity = new {Entity}Po();
        BeanUtil.copyProperties(dto, entity);
        entity.setId(IdUtil.getSnowflakeNextId());
        
        log.info("新增{Entity}，ID：{}，名称：{}", entity.getId(), entity.getName());

        {entity}Mapper.insert(entity);
        return entity;
    }
}
```

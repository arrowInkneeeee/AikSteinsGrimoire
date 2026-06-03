# 业务逻辑规范

> 来源：aIk-coding-style 规范

## 删除策略
- **物理删除**：直接使用 `deleteById()` 或 `deleteBatchIds()`
- 根据项目需求决定是否提供逻辑删除

## 分页查询
- 默认页码从1开始
- 最大页面大小限制为100条
- 使用 `Math.max()` 和 `Math.min()` 确保安全值

## 时间处理
- 使用 `LocalDateTime`
- 创建时间和修改时间在新增/修改时自动设置
- 时间范围查询使用 `ge()` 和 `le()`

## 代码组织与重构

### SQL 查询构建
- 优先使用 MyBatis-Plus 的 `Wrapper` 构建查询条件
- 简单查询使用 `LambdaQueryWrapper`，类型安全且支持 IDE 提示
```java
LambdaQueryWrapper<EntityPo> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(EntityPo::getStatus, 1)
       .like(StringUtils.isNotBlank(keyword), EntityPo::getName, keyword)
       .orderByDesc(EntityPo::getCreateTime);
```

### Stream 流式处理
- 数据转换、过滤、分组等操作优先考虑使用 Stream API
- 适用于逻辑不太复杂的场景，代码更简洁易读
```java
//note 简单转换使用 Stream
List<Long> ids = records.stream()
    .map(EntityPo::getId)
    .collect(Collectors.toList());

//note 简单分组使用 Stream
Map<String, List<EntityPo>> groupMap = records.stream()
    .collect(Collectors.groupingBy(EntityPo::getType));
```
- **注意：** 如果业务逻辑特别复杂，避免过度使用 Stream，改用传统循环提高可读性

### 方法抽取
- 类内复用的独立逻辑抽取为 `private` 方法
- 跨类复用的通用逻辑抽取到 `common/utils/` 下的工具类
```java
//note 类内私有方法
private void validateParams(EntityDto dto) {
    // 参数校验逻辑
}

//note 跨类通用方法放到 utils
// common/utils/EntityUtils.java
public static String formatEntityName(String name) {
    // 通用格式化逻辑
}
```

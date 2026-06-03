# Service层规范

> 来源：aIk-coding-style 规范

## Service 继承规范

| Service 类型 | 接口继承 | 实现类继承 | 说明 |
|-------------|---------|-----------|------|
| 数据库实体 Service | `extends IService<PO>` | `extends ServiceImpl<Mapper, PO>` | 绑定数据库实体的 CRUD 服务 |
| 普通业务 Service | 不继承 | 不继承 | 不绑定数据库实体的业务逻辑服务 |

## 数据库实体 Service 接口

绑定数据库实体的 Service 需要继承 `IService<PO>`。

```java
package {package}.service;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * -anchor {Service描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
public interface {Entity}Service extends IService<{Entity}Po> {
    
    Page<{Entity}Po> findPage({Entity}QueryDto queryDto);
    
    {Entity}Po findById(Long id);
    
    {Entity}Po add({Entity}Dto dto);
    
    {Entity}Po modify({Entity}Dto dto);
    
    Boolean remove(Long id);
}
```

## 数据库实体 Service 实现类

实现类放在 `service/impl/` 包下，继承 `ServiceImpl<Mapper, PO>`。

```java
package {package}.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * -anchor {Service实现类描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Slf4j
@RequiredArgsConstructor
@Service("{module}.{Entity}Service")
public class {Entity}ServiceImpl extends ServiceImpl<{Entity}Mapper, {Entity}Po>
        implements {Entity}Service {

    //note 默认页码
    private static final int DEFAULT_PAGE = 1;
    
    //note 最大分页大小
    private static final int MAX_PAGE_SIZE = 100;

    private final {Entity}Mapper {entity}Mapper;

    @Override
    public Page<{Entity}Po> findPage({Entity}QueryDto queryDto) {
        //note 计算分页参数
        long currentPage = Math.max(DEFAULT_PAGE, queryDto.getCurrent());
        long pageSize = Math.max(DEFAULT_PAGE, Math.min(queryDto.getSize(), MAX_PAGE_SIZE));
        Page<{Entity}Po> page = new Page<>(currentPage, pageSize);

        LambdaQueryWrapper<{Entity}Po> wrapper = new LambdaQueryWrapper<>();
        
        //note 构建查询条件
        if (StrUtil.isNotBlank(queryDto.getKeyword())) {
            wrapper.like({Entity}Po::getFieldName, queryDto.getKeyword());
        }
        
        wrapper.orderByDesc({Entity}Po::getCreateTime);
        
        Page<{Entity}Po> resultPage = this.page(page, wrapper);
        log.info("分页查询{Entity}成功，共{}条", resultPage.getTotal());
        return resultPage;
    }

    @Override
    public {Entity}Po findById(Long id) {
        {Entity}Po entity = {entity}Mapper.selectById(id);
        if (entity == null) {
            log.error("查询{Entity}失败，ID不存在：{}", id);
            throw new BusinessException("数据不存在");
        }
        return entity;
    }

    @Override
    public {Entity}Po add({Entity}Dto dto) {
        //note 参数校验
        if (StrUtil.isBlank(dto.getFieldName())) {
            log.error("新增{Entity}失败，参数错误：fieldName为空");
            throw new BusinessException("参数错误：fieldName不能为空");
        }

        //anchor 构建实体并设置审计字段
        {Entity}Po entity = new {Entity}Po();
        BeanUtil.copyProperties(dto, entity);
        entity.setId(IdUtil.getSnowflakeNextId());
        //note 使用项目统一的用户信息获取方式
        entity.setCreateUserId(getCurrentUserId());
        entity.setCreateUser(getCurrentUserName());
        entity.setCreateTime(LocalDateTime.now());
        entity.setModifyTime(LocalDateTime.now());

        //note 保存到数据库
        {entity}Mapper.insert(entity);
        log.info("新增{Entity}成功，ID：{}", entity.getId());
        return entity;
    }

    @Override
    public {Entity}Po modify({Entity}Dto dto) {
        //note 校验ID
        if (dto.getId() == null) {
            log.error("修改{Entity}失败，ID为空");
            throw new BusinessException("参数错误：ID不能为空");
        }

        //note 检查数据是否存在
        {Entity}Po exist = {entity}Mapper.selectById(dto.getId());
        if (exist == null) {
            log.error("修改{Entity}失败，数据不存在，ID：{}", dto.getId());
            throw new BusinessException("数据不存在");
        }

        //anchor 更新实体
        {Entity}Po entity = new {Entity}Po();
        BeanUtil.copyProperties(dto, entity);
        entity.setModifyTime(LocalDateTime.now());

        {entity}Mapper.updateById(entity);
        log.info("修改{Entity}成功，ID：{}", dto.getId());
        
        {Entity}Po result = {entity}Mapper.selectById(dto.getId());
        return result;
    }

    @Override
    public Boolean remove(Long id) {
        //note 检查数据是否存在
        {Entity}Po exist = {entity}Mapper.selectById(id);
        if (exist == null) {
            log.error("删除{Entity}失败，数据不存在，ID：{}", id);
            throw new BusinessException("数据不存在");
        }

        //note 执行物理删除
        int result = {entity}Mapper.deleteById(id);
        log.info("删除{Entity}成功，ID：{}", id);
        return result > 0;
    }
}
```

## 普通业务 Service（不绑定数据库实体）

不涉及数据库实体操作的 Service，不需要继承 MyBatis-Plus 的接口和实现类。

```java
package {package}.service;

/**
 * -anchor {普通业务Service描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
public interface {Business}Service {
    
    //note 普通业务方法，不涉及数据库实体
    void doBusiness({Business}Dto dto);
}
```

```java
package {package}.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * -anchor {普通业务Service实现}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Slf4j
@RequiredArgsConstructor
@Service("{module}.{Business}Service")
public class {Business}ServiceImpl implements {Business}Service {

    @Override
    public void doBusiness({Business}Dto dto) {
        //note 业务逻辑处理
        log.info("执行业务逻辑");
    }
}
```

## Service层关键技术点

**分页查询：**
```java
//note 使用常量替代魔法值
long currentPage = Math.max(DEFAULT_PAGE, queryDto.getCurrent());
long pageSize = Math.max(DEFAULT_PAGE, Math.min(queryDto.getSize(), MAX_PAGE_SIZE));
Page<{Entity}Po> page = new Page<>(currentPage, pageSize);
```

**Bean复制：**
```java
BeanUtil.copyProperties(dto, entity);
```

**异常处理：**
```java
//note 根据项目异常规范抛出异常
throw new BusinessException("参数错误");
throw new RuntimeException("数据不存在");
```

**查询条件构建：**
```java
LambdaQueryWrapper<{Entity}> wrapper = new LambdaQueryWrapper<>();
if (StrUtil.isNotBlank(queryDto.getKeyword())) {
    wrapper.like({Entity}::getFieldName, queryDto.getKeyword());
}
wrapper.orderByDesc({Entity}::getCreateTime);
```

## 事务规范

### 核心原则：慎用 `@Transactional`，使用前必须经用户确认

**使用 `@Transactional` 之前，必须先与用户确认，获得明确许可后方可使用。**

单条 `INSERT`/`UPDATE`/`DELETE` 本身就是原子性的，MySQL 会自动包裹在隐式事务中。外层再套 `@Transactional` 只会增加 AOP 代理开销、占用连接池资源，且无实际收益。

### 不加 `@Transactional` 的场景

| 场景 | 理由 |
|------|------|
| 单表单条 `INSERT` | 数据库隐式事务已保证原子性 |
| 单表单条 `UPDATE` | 同上 |
| 单表单条 `DELETE` | 同上 |
| 纯读操作（`SELECT`） | 无写操作，无需事务保护 |
| 先 `SELECT` 校验再单表写入 | `@Transactional` 默认隔离级别下查询不加锁，无法防止竞态条件 |

### 必须加 `@Transactional` 的场景

满足以下**至少一条**时才添加：

1. **一次方法里写多张表**（如 article 新增时同时插入 article_tag 关联表）
2. **先更新 A 表、再根据 A 的结果更新 B 表**，两步必须同时成功或同时失败
3. **批量操作需要统一回滚**（如批量插入、批量更新）

### 事务内严禁混入非事务操作

**事务只能回滚数据库，回滚不了缓存、MQ、文件、RPC。** 如果在 `@Transactional` 方法内混入了这些操作，一旦事务回滚，会导致数据不一致。

```java
// bad：事务内更新缓存，回滚后缓存与数据库不一致
@Transactional
public void add(EntityDto dto) {
    entityMapper.insert(po);
    redisTemplate.opsForValue().set(po.getId(), po);  // 回滚不了！
}

// good：先写库，再在事务外更新缓存
public void add(EntityDto dto) {
    entityMapper.insert(po);
    cache.put(po.getId(), po);  // 即使缓存失败，数据库数据已正确
}
```

### 竞态条件不应靠事务解决

`selectById` → `selectCount` → `deleteById` 这种"查询-校验-写入"模式，在默认 `READ_COMMITTED` 隔离级别下**不会加行锁**，`@Transactional` 挡不住并发插入。应靠以下方式解决：

- **数据库唯一索引**（首选）
- **`SELECT ... FOR UPDATE`**（显式加锁，慎用）
- **捕获 `DuplicateKeyException` 转业务异常**

```java
// bad：事务无法防止并发重复插入
@Transactional
public void add(EntityDto dto) {
    Long count = mapper.selectCount(...);
    AssertUtils.isTrue(count == 0, "已存在");
    mapper.insert(po);  // 并发下仍可能重复
}

// good：唯一索引兜底
// CREATE UNIQUE INDEX uk_field ON table(field);
public void add(EntityDto dto) {
    try {
        mapper.insert(po);
    } catch (DuplicateKeyException e) {
        throw new BusinessException("已存在");
    }
}
```

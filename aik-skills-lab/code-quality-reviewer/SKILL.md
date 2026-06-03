---
name: code-quality-reviewer
description: 代码质量审查（N+1、空指针、事务），严格遵循 aIk-coding-style 规范
type: Skill
version: 1.0.0
---

# code-quality-reviewer

> **重要**：本技能审查代码质量必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范。

## 核心规范引用

审查代码前必须阅读并遵循以下规范：

1. **代码注释**：使用 `//note`（普通）和 `//anchor`（关键）
2. **日志规范**：关键信息用 `log.info()`，异常用 `log.error()`
3. **Service Bean**：`{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式
4. **依赖注入**：统一使用 `private final` + `@RequiredArgsConstructor`
5. **常量**：使用 `private static final` 替代魔法值

## 输入

- **业务代码**: code-implementer 实现的代码

## 输出

质量审查报告

## 检查项

### 性能问题

#### N+1 查询

```java
// 错误：循环中查询数据库
public List<OrderVO> getOrdersWithItems(List<Long> orderIds) {
    List<OrderVO> result = new ArrayList<>();
    for (Long orderId : orderIds) {
        OrderVO order = getById(orderId);  // N 次查询
        List<OrderItemVO> items = itemService.getByOrderId(orderId);  // N 次查询
        order.setItems(items);
        result.add(order);
    }
    return result;
}

// 正确：使用 JOIN 或批量查询
public List<OrderVO> getOrdersWithItems(List<Long> orderIds) {
    // 1. 批量查询订单
    List<OrderEntity> orders = lambdaQuery()
            .in(OrderEntity::getId, orderIds)
            .list();
    
    // 2. 批量查询订单商品
    List<Long> ids = orders.stream().map(OrderEntity::getId).collect(toList());
    Map<Long, List<OrderItemVO>> itemMap = itemService.getMapByOrderIds(ids);
    
    // 3. 组装数据
    return orders.stream()
            .map(order -> {
                OrderVO vo = convertToVO(order);
                vo.setItems(itemMap.get(order.getId()));
                return vo;
            })
            .collect(toList());
}
```

**检查点**：
- [ ] 循环中无数据库查询
- [ ] 使用批量查询替代多次单条查询
- [ ] 使用 JOIN 替代多次查询

#### 深度分页

```java
// 错误：深度分页性能差
@Override
public PageResult<OrderVO> list(OrderQueryDTO dto) {
    Page<OrderEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    lambdaQuery().page(page);  // limit 100000, 10
    // ...
}

// 正确：限制最大页码或使用游标分页
@Override
public PageResult<OrderVO> list(OrderQueryDTO dto) {
    // 限制最大页码
    if (dto.getPageNum() > 100) {
        throw new BusinessException("最多查询前 100 页");
    }
    
    // 或使用游标分页
    // ...
}
```

**检查点**：
- [ ] 限制最大页码（如 100 页）
- [ ] 大数据量使用游标分页

#### 大事务

```java
// 错误：事务范围过大
@Transactional
public void processOrder(OrderDTO dto) {
    // 1. 保存订单
    saveOrder(dto);
    
    // 2. 调用外部接口（耗时）
    callExternalApi(dto);  // 事务挂起，占用连接
    
    // 3. 发送 MQ
    sendMessage(dto);  // 事务挂起，占用连接
    
    // 4. 更新统计
    updateStatistics(dto);
}

// 正确：缩小事务范围
public void processOrder(OrderDTO dto) {
    // 1. 保存订单（事务内）
    OrderEntity order = saveOrderInTransaction(dto);
    
    // 2. 调用外部接口（事务外）
    callExternalApi(dto);
    
    // 3. 发送 MQ（事务外）
    sendMessage(dto);
    
    // 4. 更新统计（事务外）
    updateStatistics(dto);
}

@Transactional
public OrderEntity saveOrderInTransaction(OrderDTO dto) {
    return saveOrder(dto);
}
```

**检查点**：
- [ ] 事务内无外部 HTTP 调用
- [ ] 事务内无 MQ 发送
- [ ] 事务内无复杂计算

#### 缓存使用

```java
// 错误：缓存未命中频繁查库
public OrderVO getById(Long id) {
    // 缓存未设置，每次都查库
    return convertToVO(orderMapper.selectById(id));
}

// 正确：热点数据加缓存
@Cacheable(value = "order", key = "#id")
public OrderVO getById(Long id) {
    return convertToVO(orderMapper.selectById(id));
}
```

**检查点**：
- [ ] 热点数据使用缓存
- [ ] 缓存设置合理过期时间
- [ ] 缓存更新时清除或更新缓存

### 空指针风险

#### 方法入参

```java
// 错误：未校验入参
public OrderVO createOrder(OrderCreateDTO dto) {
    Long userId = dto.getUserId();  // dto 可能为 null
    // ...
}

// 正确：校验入参
public OrderVO createOrder(OrderCreateDTO dto) {
    if (dto == null) {
        throw new BusinessException("参数不能为空");
    }
    if (dto.getUserId() == null) {
        throw new BusinessException("用户ID不能为空");
    }
    // ...
}

// 或使用 JSR-303
public OrderVO createOrder(@Validated OrderCreateDTO dto) {
    // ...
}
```

#### 数据库查询结果

```java
// 错误：未判空
public OrderVO getById(Long id) {
    OrderEntity entity = orderMapper.selectById(id);
    return convertToVO(entity);  // entity 可能为 null
}

// 正确：使用 Optional
public OrderVO getById(Long id) {
    return Optional.ofNullable(orderMapper.selectById(id))
            .map(this::convertToVO)
            .orElseThrow(() -> new BusinessException("订单不存在"));
}

// 或显式判空
public OrderVO getById(Long id) {
    OrderEntity entity = orderMapper.selectById(id);
    if (entity == null) {
        throw new BusinessException("订单不存在");
    }
    return convertToVO(entity);
}
```

#### 集合操作

```java
// 错误：未判空
public BigDecimal calculateTotal(List<OrderItemDTO> items) {
    return items.stream()  // items 可能为 null
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}

// 正确：判空或使用空集合
public BigDecimal calculateTotal(List<OrderItemDTO> items) {
    if (CollectionUtils.isEmpty(items)) {
        return BigDecimal.ZERO;
    }
    return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**检查点**：
- [ ] 方法入参校验
- [ ] 数据库查询结果判空
- [ ] 集合判空或初始化
- [ ] 使用 Optional 处理可能为 null 的值

### 事务问题

#### 事务边界

```java
// 错误：事务范围过大（见上文大事务示例）

// 正确：事务只包含必要的数据库操作
@Transactional
public void saveOrderAndDeductStock(OrderDTO dto) {
    // 1. 保存订单
    saveOrder(dto);
    
    // 2. 扣减库存
    deductStock(dto);
}
```

#### 事务传播

```java
// 错误：同类方法调用，事务失效
@Service
public class OrderServiceImpl implements OrderService {
    
    @Transactional
    public void createOrder(OrderDTO dto) {
        saveOrder(dto);
        this.updateStatistics(dto);  // 事务失效！
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatistics(OrderDTO dto) {
        // 新事务不会生效
    }
}

// 正确：注入自身或使用 AopContext
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderService self;  // 注入自身
    
    @Transactional
    public void createOrder(OrderDTO dto) {
        saveOrder(dto);
        self.updateStatistics(dto);  // 使用代理对象调用
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatistics(OrderDTO dto) {
        // 新事务生效
    }
}
```

#### 事务回滚

```java
// 错误：异常被捕获，事务不回滚
@Transactional
public void createOrder(OrderDTO dto) {
    try {
        saveOrder(dto);
    } catch (Exception e) {
        log.error("保存订单失败", e);  // 异常被吞，事务不回滚
    }
}

// 正确：抛出 RuntimeException
@Transactional
public void createOrder(OrderDTO dto) {
    try {
        saveOrder(dto);
    } catch (Exception e) {
        log.error("保存订单失败", e);
        throw new BusinessException("创建订单失败");  // 抛出异常，事务回滚
    }
}
```

**检查点**：
- [ ] 事务范围合理
- [ ] 同类方法调用使用代理对象
- [ ] 异常正确抛出，确保事务回滚
- [ ] 指定 rollbackFor = Exception.class

### 资源管理

#### 锁释放

```java
// 错误：锁可能未释放
public void processWithLock(String key) {
    RLock lock = redissonClient.getLock(key);
    lock.tryLock(3, 10, TimeUnit.SECONDS);
    try {
        // 业务逻辑
    } finally {
        lock.unlock();  // 如果 tryLock 失败，unlock 会报错
    }
}

// 正确：检查锁是否成功
public void processWithLock(String key) {
    RLock lock = redissonClient.getLock(key);
    boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
    if (!locked) {
        throw new BusinessException("获取锁失败");
    }
    try {
        // 业务逻辑
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

**检查点**：
- [ ] 锁正确释放
- [ ] 流正确关闭（try-with-resources）
- [ ] 连接正确关闭

## 输出格式

```markdown
## 代码质量审查报告

### 审查概览

| 检查项 | 通过 | 警告 | 失败 |
|--------|------|------|------|
| 性能问题 | 15 | 3 | 1 |
| 空指针风险 | 28 | 2 | 0 |
| 事务问题 | 12 | 1 | 0 |
| 资源管理 | 8 | 0 | 0 |

### 性能问题

✅ **通过**
- 无 N+1 查询
- 合理使用缓存

⚠️ **警告**
- OrderService.list() 未限制最大页码（第 45 行）
- OrderService.createOrder() 事务范围过大，包含外部调用（第 78-85 行）

❌ **失败**
- OrderService.getOrdersWithItems() 存在 N+1 查询（第 32 行循环中查询数据库）

### 空指针风险

✅ **通过**
- 方法入参有校验
- 数据库查询结果有判空

⚠️ **警告**
- OrderService.calculateTotal() 未判空 items 参数（第 56 行）
- OrderController.getById() 未处理 service 返回 null（第 89 行）

### 事务问题

✅ **通过**
- 事务边界合理
- 异常正确抛出

⚠️ **警告**
- OrderServiceImpl.updateStatistics() 同类方法调用，事务可能失效（第 102 行）

### 资源管理

✅ **通过**
- 锁正确释放
- 流正确关闭

### 修复建议

**高优先级**：
1. 修复 N+1 查询，使用批量查询（第 32 行）

**中优先级**：
1. OrderService.list() 添加最大页码限制（第 45 行）
2. 拆分 OrderService.createOrder() 事务（第 78-85 行）

**低优先级**：
1. OrderService.calculateTotal() 添加判空（第 56 行）
2. 修复同类方法调用事务失效（第 102 行）
```

## 严重级别

| 级别 | 说明 | 示例 |
|------|------|------|
| **严重** | 必须立即修复 | N+1 查询、事务失效 |
| **警告** | 建议修复 | 未限制页码、事务范围大 |
| **提示** | 可选修复 | 缺少判空 |

## 注意事项

- **所有代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范**
- N+1 查询严重影响性能，必须修复
- 空指针是常见 Bug，要严格检查
- 事务问题可能导致数据不一致
- 资源泄漏会导致系统不稳定
- **代码注释使用 `//note` 和 `//anchor` 标记**
- **Service Bean名称使用 `{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式**
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- **使用常量替代魔法值**

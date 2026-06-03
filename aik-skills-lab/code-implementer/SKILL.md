---
name: code-implementer
description: 填充业务逻辑，实现功能，严格遵循 aIk-coding-style 规范
type: Skill
version: 1.0.0
---

# code-implementer

> **重要**：本技能实现代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范。

## 核心规范引用

实现代码前必须阅读并遵循以下规范：

1. **代码注释**：使用 `//note`（普通注释）和 `//anchor`（关键注释）
2. **日志规范**：Controller和ServiceImpl必须添加 `@Slf4j`，关键信息用 `log.info()`，异常用 `log.error()`
3. **Service Bean**：`@Service("{module}.{ServiceName}")` 或 `@Service("{module}.{subModule}.{ServiceName}")`
4. **依赖注入**：统一使用 `private final` + `@RequiredArgsConstructor`
5. **PO实体**：`@Data + @SuperBuilder + @ToString(callSuper = true)`
6. **目录结构**：`common/po/`、`common/dto/`、`common/vo/`、`dao/mapping/`

## 输入

- **代码骨架**: code-generator 生成的代码
- **核心流程设计**: SDD 中的时序图、状态机
- **技术选型**: 缓存、MQ、锁等方案

## 输出

完整业务代码

## 工作流

1. **分析核心流程设计**
   - 理解时序图流程
   - 理解状态机流转
   - 识别事务边界

2. **分析技术选型**
   - 缓存使用场景
   - MQ 使用场景
   - 分布式锁使用场景

3. **填充 Service 业务逻辑**
   - 实现接口方法
   - 添加业务校验
   - 处理异常流程

4. **实现 Mapper 复杂 SQL**
   - 编写 XML SQL
   - 优化查询性能

5. **输出完整业务代码**

## Java 8 风格

### Stream API 使用

```java
// 集合转换
public List<OrderVO> convertToVOList(List<OrderEntity> entities) {
    return entities.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
}

// 过滤
public List<OrderVO> getPaidOrders(List<OrderVO> orders) {
    return orders.stream()
            .filter(order -> OrderStatus.PAID.getCode().equals(order.getStatus()))
            .collect(Collectors.toList());
}

// 分组
public Map<Integer, List<OrderVO>> groupByStatus(List<OrderVO> orders) {
    return orders.stream()
            .collect(Collectors.groupingBy(OrderVO::getStatus));
}

// 求和
public BigDecimal calculateTotalAmount(List<OrderItemDTO> items) {
    return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

### Optional 使用

```java
// 避免空指针
public OrderVO getById(Long id) {
    return Optional.ofNullable(super.getById(id))
            .map(this::convertToVO)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
}

// 链式操作
public String getUserName(Long userId) {
    return Optional.ofNullable(userService.getById(userId))
            .map(User::getName)
            .orElse("未知用户");
}

// 条件执行
public void updateOrderStatus(Long orderId, Integer status) {
    Optional.ofNullable(orderMapper.selectById(orderId))
            .ifPresent(order -> {
                order.setStatus(status);
                orderMapper.updateById(order);
            });
}
```

### 复杂业务（不使用 Stream）

```java
// 复杂业务逻辑，使用传统方式更清晰
public void processComplexOrder(OrderDTO dto) {
    // 1. 校验订单
    if (!validateOrder(dto)) {
        throw new BusinessException("订单校验失败");
    }
    
    // 2. 处理各种业务规则
    if (dto.getType() == OrderType.NORMAL) {
        processNormalOrder(dto);
    } else if (dto.getType() == OrderType.GROUP) {
        processGroupOrder(dto);
    } else if (dto.getType() == OrderType.SECKILL) {
        processSeckillOrder(dto);
    }
    
    // 3. 后续处理
    // ...
}
```

## MyBatis-Plus 使用

### LambdaQueryWrapper

```java
// 简单查询
@Override
public List<OrderEntity> getByUserId(Long userId) {
    return lambdaQuery()
            .eq(OrderEntity::getUserId, userId)
            .eq(OrderEntity::getDeleted, 0)
            .orderByDesc(OrderEntity::getCreateTime)
            .list();
}

// 条件查询
@Override
public List<OrderEntity> search(OrderQueryDTO dto) {
    LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<>();
    
    wrapper.eq(OrderEntity::getUserId, dto.getUserId())
           .eq(dto.getStatus() != null, OrderEntity::getStatus, dto.getStatus())
           .ge(dto.getStartDate() != null, OrderEntity::getCreateTime, dto.getStartDate())
           .le(dto.getEndDate() != null, OrderEntity::getCreateTime, dto.getEndDate())
           .eq(OrderEntity::getDeleted, 0)
           .orderByDesc(OrderEntity::getCreateTime);
    
    return list(wrapper);
}

// 分页查询
@Override
public PageResult<OrderVO> list(OrderQueryDTO dto) {
    Page<OrderEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    
    lambdaQuery()
            .eq(OrderEntity::getUserId, dto.getUserId())
            .eq(dto.getStatus() != null, OrderEntity::getStatus, dto.getStatus())
            .eq(OrderEntity::getDeleted, 0)
            .orderByDesc(OrderEntity::getCreateTime)
            .page(page);
    
    List<OrderVO> records = page.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    
    return PageResult.<OrderVO>builder()
            .list(records)
            .total(page.getTotal())
            .pageNum((int) page.getCurrent())
            .pageSize((int) page.getSize())
            .build();
}
```

### LambdaUpdateWrapper

```java
// 更新状态
@Override
public boolean updateStatus(Long orderId, Integer status) {
    return lambdaUpdate()
            .set(OrderEntity::getStatus, status)
            .set(OrderEntity::getUpdateTime, LocalDateTime.now())
            .eq(OrderEntity::getId, orderId)
            .eq(OrderEntity::getDeleted, 0)
            .update();
}

// 批量更新
@Override
public boolean batchUpdateStatus(List<Long> orderIds, Integer status) {
    return lambdaUpdate()
            .set(OrderEntity::getStatus, status)
            .set(OrderEntity::getUpdateTime, LocalDateTime.now())
            .in(OrderEntity::getId, orderIds)
            .update();
}
```

## 事务控制

### 基本事务

```java
@Override
@Transactional(rollbackFor = Exception.class)
public OrderVO createOrder(OrderCreateDTO dto) {
    // 1. 保存订单
    OrderEntity order = new OrderEntity();
    // ... 设置属性
    save(order);
    
    // 2. 扣减库存（失败回滚）
    boolean deducted = stockService.deductStock(dto.getProductId(), dto.getQuantity());
    if (!deducted) {
        throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH, "库存不足");
    }
    
    // 3. 创建支付记录（失败回滚）
    paymentService.createPayment(order);
    
    return convertToVO(order);
}
```

### 独立事务

```java
@Override
@Transactional(propagation = Propagation.REQUIRES_NEW, 
               rollbackFor = Exception.class)
public void handlePayCallback(PayCallbackDTO dto) {
    // 独立事务处理支付回调
    // 不受外部事务影响
}
```

### 只读事务

```java
@Override
@Transactional(readOnly = true)
public List<OrderVO> list(OrderQueryDTO dto) {
    // 只读查询，优化性能
}
```

## 日志规范

```java
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
}
```

**规范说明**：
- 使用 `-anchor` 类注释
- **Service Bean名称必须使用 `{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式**
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- **代码注释使用 `//note`（普通）和 `//anchor`（关键）**
- **关键信息使用 `log.info()`，异常使用 `log.error()`**
- **使用常量替代魔法值**（如 `DEFAULT_PAGE`、`MAX_PAGE_SIZE`）
- **使用 `BeanUtil.copyProperties` 进行对象复制**

## 缓存使用

### Spring Cache

```java
/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:24
 * -
 **/
@Slf4j
@RequiredArgsConstructor
@Service("productService")
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductPo>
        implements ProductService {

    private final ProductMapper productMapper;

    @Override
    @Cacheable(value = "product", key = "#productId")
    public ProductVo getById(Long productId) {
        log.debug("-note 查询商品，ID：{}", productId);
        ProductPo entity = super.getById(productId);
        return convertToVo(entity);
    }

    @Override
    @CacheEvict(value = "product", key = "#productId")
    public boolean updateProduct(Long productId, ProductUpdateDto dto) {
        // -anchor 更新商品，清除缓存
        return updateById(convertToEntity(dto));
    }

    @Override
    @CacheEvict(value = "product", allEntries = true)
    public boolean batchUpdateStatus(List<Long> productIds, Integer status) {
        // -anchor 批量更新，清除所有缓存
        return lambdaUpdate()
                .set(ProductPo::getStatus, status)
                .in(ProductPo::getId, productIds)
                .update();
    }
}
```

### Redis 手动控制

```java
/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:24
 * -
 **/
@Slf4j
@RequiredArgsConstructor
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderPo>
        implements OrderService {

    private final StringRedisTemplate redisTemplate;

    private final OrderMapper orderMapper;

    @Override
    public OrderVo getById(Long orderId) {
        String key = CacheKeyConstant.orderKey(orderId);

        // -anchor 查缓存
        String json = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            log.debug("-note 命中缓存，key：{}", key);
            return JSONUtil.toBean(json, OrderVo.class);
        }

        // -anchor 查数据库
        OrderPo entity = orderMapper.selectById(orderId);
        if (entity == null) {
            return null;
        }

        // -anchor 转 VO
        OrderVo vo = convertToVo(entity);

        // -anchor 写缓存（10分钟过期）
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(vo),
                Duration.ofMinutes(10));

        return vo;
    }
}
```

## 分布式锁

```java
/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:24
 * -
 **/
@Slf4j
@RequiredArgsConstructor
@Service("stockService")
public class StockServiceImpl implements StockService {

    private final RedissonClient redissonClient;

    private final StockMapper stockMapper;

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        String lockKey = CacheKeyConstant.stockLockKey(productId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // -anchor 尝试获取锁，等待3秒，锁10秒自动释放
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("-anchor 获取锁失败，productId：{}", productId);
                return false;
            }

            try {
                // -anchor 查库存
                StockPo stock = stockMapper.selectById(productId);
                if (stock == null || stock.getAvailable() < quantity) {
                    return false;
                }

                // -anchor 扣减库存
                int result = stockMapper.deduct(productId, quantity);
                return result > 0;

            } finally {
                // -anchor 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.error("-anchor 获取锁异常，productId：{}", productId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
```

## 消息队列

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final RabbitTemplate rabbitTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO dto) {
        // 创建订单...
        OrderEntity order = doCreateOrder(dto);
        
        // 发送延迟消息（30分钟后检查超时）
        rabbitTemplate.convertAndSend(
            "order.exchange", 
            "order.delay", 
            order.getOrderNo(),
            message -> {
                message.getMessageProperties().setExpiration("1800000"); // 30分钟
                return message;
            }
        );
        
        return convertToVO(order);
    }
}

// 消费者
@Component
@RabbitListener(queues = "order.cancel.queue")
@Slf4j
public class OrderCancelListener {
    
    private final OrderService orderService;
    
    @RabbitHandler
    public void handle(String orderNo) {
        log.info("收到订单取消消息，订单号：{}"， orderNo);
        try {
            orderService.cancelTimeoutOrder(orderNo);
        } catch (Exception e) {
            log.error("处理订单取消消息失败，订单号：{}"， orderNo, e);
            // 根据业务决定是否重试
        }
    }
}
```

## 状态机实现

```java
/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:24
 * -
 **/
@Slf4j
@RequiredArgsConstructor
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderPo>
        implements OrderService {

    private final OrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        // -anchor 查询订单
        OrderPo order = lambdaQuery()
                .eq(OrderPo::getOrderNo, orderNo)
                .one();

        if (order == null) {
            throw new BusinessException(ErrorCodeConstant.ORDER_NOT_FOUND, "订单不存在");
        }

        // -anchor 状态校验
        OrderStatus currentStatus = OrderStatus.of(order.getStatus());
        OrderStatus targetStatus = OrderStatus.PAID;

        if (!currentStatus.canTransferTo(targetStatus)) {
            log.warn("-anchor 订单状态不允许流转，订单号：{}，当前状态：{}，目标状态：{}",
                    orderNo, currentStatus.getDesc(), targetStatus.getDesc());
            throw new BusinessException(ErrorCodeConstant.ORDER_STATUS_ERROR,
                    "订单状态不允许操作");
        }

        // -anchor 更新状态
        boolean updated = lambdaUpdate()
                .set(OrderPo::getStatus, targetStatus.getCode())
                .set(OrderPo::getPayTime, LocalDateTime.now())
                .eq(OrderPo::getId, order.getId())
                .eq(OrderPo::getStatus, currentStatus.getCode()) // 乐观锁
                .update();

        if (!updated) {
            throw new BusinessException("订单状态更新失败，请重试");
        }

        log.info("-anchor 订单支付成功，订单号：{}，状态：{} -> {}",
                orderNo, currentStatus.getDesc(), targetStatus.getDesc());

        // -anchor 后续处理：发送通知、更新统计等
        eventPublisher.publishEvent(new OrderPaidEvent(order));
    }
}
```

## 规范说明

- **所有代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范**
- **所有类必须使用 `-anchor` 类注释模板，@author 固定为 `a I k .`**
- **Service Bean名称必须使用 `{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式**（如 `@Service("order.OrderService")`）
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- **代码注释使用 `//note`（普通）和 `//anchor`（关键）**
- **PO实体使用 `XxPo` 命名，DTO使用 `XxDto`，VO使用 `XxVo`**
- **常量类使用 `XxConstant` 命名**（如 `CacheKeyConstant`、`ErrorCodeConstant`）
- **使用 `StrUtil`、`BeanUtil`、`IdUtil` 等Hutool工具类**
- **使用 `BeanUtil.copyProperties` 进行对象复制**
- **使用 `LambdaQueryWrapper` 构建查询条件**
- **分页参数使用 `Math.max()` 和 `Math.min()` 确保安全值**
- **SQL 实现方式**：优先 MyBatis-Plus API，复杂 SQL 才用 XML
- **VO 转换**：使用 `of()` 静态方法进行 PO 到 VO 的转换

## 注意事项

- **所有代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范**
- 优先使用 Java 8 Stream API 处理集合（简单场景）
- 空值处理使用 Optional，避免 != null 判断
- 复杂业务逻辑不使用 Stream，保证可读性
- 事务边界要明确，避免大事务
- **日志记录核心流程和异常，使用 `log.info()` 和 `log.error()`**
- 缓存要考虑一致性，及时清除
- 分布式锁要确保释放，避免死锁
- 状态机要校验流转合法性
- **代码注释使用 `//note` 和 `//anchor` 标记**
- **禁止行尾注释，if必须使用大括号**

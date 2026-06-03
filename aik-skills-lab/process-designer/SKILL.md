---
name: process-designer
description: 核心业务流程时序图、状态机设计
type: Skill
version: 1.0.0
---

# process-designer

## 输入

- **关键用户故事**: 优先级高的核心业务流程
- **验收标准**: 业务场景和边界条件

## 输出

流程设计文档

## 工作流

1. **识别核心业务流程**
   - 从用户故事提取关键流程
   - 识别参与者和系统组件
   - 确定流程触发条件

2. **设计时序图**
   - 使用 PlantUML 格式
   - 标注方法调用和参数
   - 标注事务边界

3. **设计状态机**（如需要）
   - 定义状态枚举
   - 定义状态流转规则
   - 定义状态变更触发条件

4. **标注事务边界**
   - 标识 @Transactional 范围
   - 标注事务传播行为
   - 识别事务失效风险点

5. **输出流程设计文档**

## 时序图规范（PlantUML）

```plantuml
@startuml
actor User
participant "OrderController" as Controller
participant "OrderService" as Service
participant "StockService" as Stock
participant "OrderMapper" as Mapper
participant "PaymentService" as Payment

defaultFontSize 14
skinparam sequence {
    ArrowColor Black
    ActorBorderColor Black
    LifeLineBorderColor Black
    LifeLineBackgroundColor White
    ParticipantBorderColor Black
    ParticipantBackgroundColor White
}

User -> Controller: POST /orders
Controller -> Service: createOrder(OrderDTO)

activate Service
Service -> Service: 校验参数
Service -> Stock: deductStock(productId, quantity)
Stock --> Service: boolean

alt 库存充足
    Service -> Mapper: insert(OrderEntity)
    Mapper --> Service: int
    
    Service -> Payment: createPayment(order)
    Payment --> Service: PaymentResult
    
    Service --> Controller: OrderVO
else 库存不足
    Service --> Controller: 抛出 BusinessException("库存不足")
end
deactivate Service

Controller --> User: Result<OrderVO>
@enduml
```

## 状态机设计

### 状态枚举定义

```java
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");
    
    private final Integer code;
    private final String desc;
    
    /**
     * 判断是否可以流转到目标状态
     */
    public boolean canTransferTo(OrderStatus target) {
        switch (this) {
            case PENDING_PAYMENT:
                return target == PAID || target == CANCELLED;
            case PAID:
                return target == SHIPPED;
            case SHIPPED:
                return target == COMPLETED;
            default:
                return false;
        }
    }
    
    /**
     * 获取允许的目标状态列表
     */
    public List<OrderStatus> getAllowedTargets() {
        return Arrays.stream(OrderStatus.values())
                .filter(this::canTransferTo)
                .collect(Collectors.toList());
    }
}
```

### 状态流转规则

| 当前状态 | 允许流转到 | 触发条件 |
|---------|-----------|---------|
| 待支付 | 已支付 | 支付成功回调 |
| 待支付 | 已取消 | 用户取消 / 超时取消 |
| 已支付 | 已发货 | 商家发货 |
| 已发货 | 已完成 | 用户确认收货 / 自动确认 |

### 状态变更实现

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> 
        implements OrderService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        OrderEntity order = lambdaQuery()
                .eq(OrderEntity::getOrderNo, orderNo)
                .one();
        
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        // 状态校验
        OrderStatus currentStatus = OrderStatus.of(order.getStatus());
        OrderStatus targetStatus = OrderStatus.PAID;
        
        if (!currentStatus.canTransferTo(targetStatus)) {
            throw new BusinessException(
                String.format("订单状态不允许流转：%s -> %s", 
                    currentStatus.getDesc(), targetStatus.getDesc())
            );
        }
        
        // 更新状态
        lambdaUpdate()
                .set(OrderEntity::getStatus, targetStatus.getCode())
                .set(OrderEntity::getPayTime, LocalDateTime.now())
                .eq(OrderEntity::getId, order.getId())
                .update();
        
        log.info("订单支付成功，订单号：{}，状态：{} -> {}", 
                orderNo, currentStatus.getDesc(), targetStatus.getDesc());
    }
}
```

## 事务边界标注

### 事务范围

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> 
        implements OrderService {
    
    /**
     * 创建订单 - 需要事务
     * 包含：保存订单、扣减库存、创建支付记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO dto) {
        // 1. 保存订单
        // 2. 扣减库存
        // 3. 创建支付记录
    }
    
    /**
     * 查询订单 - 不需要事务
     */
    @Override
    public OrderVO getById(Long id) {
        // 纯查询操作
    }
    
    /**
     * 支付回调 - 需要事务，独立事务
     * 避免外部事务影响支付结果处理
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, 
                   rollbackFor = Exception.class)
    public void handlePayCallback(PayCallbackDTO dto) {
        // 处理支付回调
    }
}
```

### 事务失效场景

| 场景 | 说明 | 解决方案 |
|------|------|---------|
| 同类方法调用 | this.method() 调用 | 注入自身或使用 AopContext |
| 异步方法 | @Async 方法 | 事务在异步线程中独立 |
| 非 public 方法 | @Transactional 要求 public | 改为 public |
| 异常被捕获 | try-catch 吞掉异常 | 抛出 RuntimeException |
| 错误的异常类型 | 默认只回滚 RuntimeException | 指定 rollbackFor |

## 输出格式

```markdown
## 核心流程设计

### 4.1 下单流程时序图

```plantuml
@startuml
actor User
participant "OrderController" as Controller
participant "OrderService" as Service
participant "StockService" as Stock
participant "OrderMapper" as Mapper
participant "PaymentService" as Payment

defaultFontSize 14

User -> Controller: POST /orders
Controller -> Service: createOrder(dto)

activate Service #LightBlue
note right: @Transactional

Service -> Service: 校验参数
Service -> Stock: deductStock(productId, quantity)
Stock --> Service: boolean

alt 库存充足
    Service -> Mapper: insert(order)
    Mapper --> Service: 影响行数
    
    Service -> Payment: createPayment(order)
    Payment --> Service: paymentResult
    
    Service --> Controller: OrderVO
else 库存不足
    Service --> Controller: 抛出 BusinessException
end

deactivate Service

Controller --> User: Result<OrderVO>
@enduml
```

**事务边界**：
- `OrderService.createOrder()` 方法添加 `@Transactional(rollbackFor = Exception.class)`
- 事务传播：REQUIRED（默认）
- 回滚条件：所有异常

### 4.2 订单状态机

**状态定义**：
```java
public enum OrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");
    
    private final Integer code;
    private final String desc;
    
    public boolean canTransferTo(OrderStatus target) {
        switch (this) {
            case PENDING_PAYMENT:
                return target == PAID || target == CANCELLED;
            case PAID:
                return target == SHIPPED;
            case SHIPPED:
                return target == COMPLETED;
            default:
                return false;
        }
    }
}
```

**状态流转规则**：
| 当前状态 | 允许流转到 | 触发条件 |
|---------|-----------|---------|
| 待支付 | 已支付 | 支付成功回调 |
| 待支付 | 已取消 | 用户取消 / 超时取消（延迟队列）|
| 已支付 | 已发货 | 商家发货 |
| 已发货 | 已完成 | 用户确认收货 / 7天自动确认 |

**状态变更实现要点**：
1. 先查询当前状态
2. 校验状态流转合法性
3. 更新状态并记录日志
4. 异步发送状态变更通知（如需要）

### 4.3 事务设计

| 方法 | 事务 | 传播行为 | 说明 |
|------|------|---------|------|
| createOrder | 是 | REQUIRED | 创建订单，包含多个操作 |
| getById | 否 | - | 纯查询 |
| handlePayCallback | 是 | REQUIRES_NEW | 独立事务，避免影响回调 |
| cancelOrder | 是 | REQUIRED | 取消订单，释放资源 |
```

## 注意事项

- 时序图使用 PlantUML 格式，便于版本控制
- 状态机必须定义流转规则，避免非法状态变更
- 事务边界要明确，避免大事务和事务失效
- 异步操作要考虑事务提交后再执行
- 状态变更要记录日志，便于问题排查

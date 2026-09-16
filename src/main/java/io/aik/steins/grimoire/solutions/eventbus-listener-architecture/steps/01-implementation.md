# 实施步骤

## Step 1：引入基础设施

将 `components/eventbus/` 下的代码复制到目标项目，调整包路径。

确保 `pom.xml` 中有 Guava 依赖：

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
</dependency>
```

## Step 2：定义业务事件

按业务语义创建事件类，继承 `StageEvent<T>`：

```java
public class OrderCreatedEvent extends StageEvent<Long> {
    public OrderCreatedEvent(StageEnum stage, Long orderId) {
        super(stage, orderId);
    }
}
```

**命名约定**：`{业务动作}Event`，如 `TaskCompletedEvent`、`OrderCreatedEvent`。

## Step 3：在业务操作后发布事件

在 Service/Action 层的业务操作完成后发布事件：

```java
@Service
public class OrderService {
    
    @Autowired
    private EventBusManager eventBusManager;
    
    public void createOrder(Order order) {
        orderMapper.insert(order);
        
        // 发布事件
        eventBusManager.post(new OrderCreatedEvent(StageEnum.ON, order.getId()));
    }
}
```

## Step 4：编写监听器

继承 `AbstractStageListener`，实现联动逻辑：

```java
@Component
public class OrderCreatedListener extends AbstractStageListener<OrderCreatedEvent, Long> {

    @Autowired
    private EventBusManager eventBusManager;
    
    @Autowired
    private InventoryService inventoryService;

    @PostConstruct
    public void reg() {
        registerSelf(eventBusManager);
    }

    @Override
    protected StageEnum getTargetStage() {
        return StageEnum.ON;
    }

    @Override
    protected void handleEvent(OrderCreatedEvent event) {
        Long orderId = event.getPayload();
        inventoryService.deduct(orderId);
    }

    @SafeSubscribe
    public void onOrderCreated(OrderCreatedEvent event) {
        processEvent(event);
    }
}
```

## Step 5：验证

1. 启动应用，确认日志中出现 `Listener registered: XxxListener`
2. 触发业务操作，确认监听器被调用
3. 测试异常场景：监听器抛异常不影响其他监听器

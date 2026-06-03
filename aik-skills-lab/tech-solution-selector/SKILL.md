---
name: tech-solution-selector
description: 技术方案选型（缓存、MQ、锁等）
type: Skill
version: 1.0.0
---

# tech-solution-selector

## 输入

- **非功能性需求**: 性能、并发、可靠性要求
- **可行性评估**: 需求分析阶段的技术约束
- **pom.xml**: 项目已有依赖

## 输出

技术选型报告

## 工作流

1. **检查项目已有技术栈**
   - 检查 pom.xml 依赖
   - 检查已有配置类
   - 记录项目技术偏好

2. **分析性能需求**
   - QPS/TPS 要求
   - 响应时间要求
   - 数据量评估

3. **评估缓存需求**
   - 热点数据识别
   - 读多写少场景
   - 缓存一致性要求

4. **评估消息队列需求**
   - 异步处理场景
   - 解耦需求
   - 削峰填谷需求

5. **评估分布式锁需求**
   - 并发控制场景
   - 幂等性要求

6. **输出技术选型报告**

## 技术选型原则

1. **优先复用项目已有技术栈**
2. 如无，根据场景推荐最合适方案
3. 全新项目才做完整技术选型

## 检查项目已有依赖

```bash
# 检查缓存相关
grep -E "(redis|caffeine|cache)" pom.xml

# 检查消息队列
grep -E "(rabbitmq|kafka|rocketmq)" pom.xml

# 检查分布式锁
grep -E "(redisson|zookeeper|curator)" pom.xml

# 检查异步处理
grep -E "(async| CompletableFuture)" pom.xml
```

## 缓存方案选型

### 方案对比

| 方案 | 适用场景 | 优点 | 缺点 | 检查依赖 |
|------|---------|------|------|---------|
| **Spring Cache** | 简单方法级缓存 | 简单易用，注解驱动 | 功能有限 | `spring-boot-starter-cache` |
| **Redis** | 分布式缓存 | 功能丰富，持久化 | 网络开销 | `spring-boot-starter-data-redis` |
| **Caffeine** | 本地缓存 | 性能极高 | 单机限制 | `caffeine` |
| **多级缓存** | 高并发读 | 兼顾性能和一致性 | 复杂度高 | 上述组合 |

### 选型建议

```
读多写少 + 单机部署 → Caffeine
读多写少 + 分布式 + 简单场景 → Spring Cache + Redis
读多写少 + 分布式 + 复杂场景 → Redis + 手动控制
高并发读 + 一致性要求高 → Caffeine + Redis 多级缓存
```

### 缓存 Key 规范

放在 `common.constant`，**如项目已有则复用**：

```java
public final class CacheKey {
    private CacheKey() {}
    
    public static final String USER = "user:";
    public static final String ORDER = "order:";
    public static final String PRODUCT = "product:";
    
    public static String userKey(Long userId) {
        return USER + userId;
    }
    
    public static String orderKey(Long orderId) {
        return ORDER + orderId;
    }
}
```

### 实现示例

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity> 
        implements ProductService {
    
    private final StringRedisTemplate redisTemplate;
    
    @Override
    public ProductVO getById(Long productId) {
        String key = CacheKey.productKey(productId);
        
        // 查缓存
        String json = redisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            log.debug("命中缓存，key：{}"， key);
            return JSONUtil.toBean(json, ProductVO.class);
        }
        
        // 查数据库
        ProductEntity entity = super.getById(productId);
        if (entity == null) {
            return null;
        }
        
        // 转 VO
        ProductVO vo = convertToVO(entity);
        
        // 写缓存（设置过期时间）
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(vo), 
                Duration.ofMinutes(30));
        
        return vo;
    }
}
```

## 消息队列选型

### 方案对比

| 方案 | 适用场景 | 优点 | 缺点 | 检查依赖 |
|------|---------|------|------|---------|
| **RabbitMQ** | 可靠性要求高，路由复杂 | 功能丰富，延迟队列 | 吞吐量一般 | `spring-boot-starter-amqp` |
| **Kafka** | 高吞吐，日志采集 | 吞吐量极高 | 功能简单 | `spring-kafka` |
| **RocketMQ** | 金融级可靠性 | 功能丰富，延迟消息 | 依赖较重 | `rocketmq-spring-boot-starter` |

### 选型建议

```
可靠性要求高 + 复杂路由 → RabbitMQ
高吞吐 + 日志/大数据 → Kafka
金融级 + 延迟消息 → RocketMQ
项目已有 → 复用已有
```

### 应用场景

- **延迟队列**：订单超时取消、定时任务
- **异步通知**：支付成功通知、短信发送
- **削峰填谷**：秒杀、大促
- **数据同步**：数据变更同步 ES

### 实现示例（RabbitMQ）

```java
@Configuration
public class RabbitConfig {
    
    // 延迟队列：订单超时取消
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable("order.delay.queue")
                .withArgument("x-dead-letter-exchange", "order.exchange")
                .withArgument("x-dead-letter-routing-key", "order.cancel")
                .withArgument("x-message-ttl", 30 * 60 * 1000) // 30分钟
                .build();
    }
    
    @Bean
    public Queue orderCancelQueue() {
        return new Queue("order.cancel.queue");
    }
    
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
    }
    
    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(orderExchange())
                .with("order.cancel");
    }
}

// 发送延迟消息
@Service
@RequiredArgsConstructor
public class OrderService {
    private final RabbitTemplate rabbitTemplate;
    
    public void createOrder(OrderDTO dto) {
        // 创建订单...
        
        // 发送延迟消息
        rabbitTemplate.convertAndSend(
            "order.exchange", 
            "order.delay", 
            orderNo,
            message -> {
                message.getMessageProperties().setExpiration("1800000"); // 30分钟
                return message;
            }
        );
    }
}

// 消费取消消息
@Component
@RabbitListener(queues = "order.cancel.queue")
@Slf4j
public class OrderCancelListener {
    
    @RabbitHandler
    public void handle(String orderNo) {
        log.info("收到订单取消消息，订单号：{}"， orderNo);
        // 取消订单逻辑
    }
}
```

## 分布式锁选型

### 方案对比

| 方案 | 适用场景 | 优点 | 缺点 | 检查依赖 |
|------|---------|------|------|---------|
| **Redisson** | 功能完善，推荐 | 功能丰富，看门狗 | 额外依赖 | `redisson` |
| **Redis + Lua** | 简单场景 | 轻量 | 需自己实现 | `spring-boot-starter-data-redis` |
| **Zookeeper** | 强一致性 | 可靠性高 | 复杂度高 | `curator-framework` |

### 选型建议

```
功能完善 + 易用 → Redisson（推荐）
简单场景 + 轻量 → Redis + Lua
强一致性 + 高可靠 → Zookeeper
项目已有 → 复用已有
```

### 应用场景

- **库存扣减**：防止超卖
- **订单号生成**：防止重复
- **定时任务**：集群环境下单节点执行
- **接口幂等**：防止重复提交

### 实现示例（Redisson）

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {
    private final RedissonClient redissonClient;
    private final StockMapper stockMapper;
    
    public boolean deductStock(Long productId, Integer quantity) {
        String lockKey = "stock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，等待3秒，锁10秒自动释放
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取锁失败，productId：{}"， productId);
                return false;
            }
            
            try {
                // 查库存
                Stock stock = stockMapper.selectById(productId);
                if (stock == null || stock.getAvailable() < quantity) {
                    return false;
                }
                
                // 扣减库存
                int result = stockMapper.deduct(productId, quantity);
                return result > 0;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            log.error("获取锁异常"， e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
```

## 异步处理选型

### 方案对比

| 方案 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| **@Async** | 简单异步 | 简单易用 | 无法获取结果 |
| **CompletableFuture** | 复杂编排 | 功能强大 | 代码复杂 |
| **Spring Event** | 解耦事件 | 松耦合 | 同进程内 |

### 实现示例

```java
// @Async 方式
@Service
@RequiredArgsConstructor
public class OrderService {
    
    @Async("taskExecutor")
    public void asyncSendSms(String phone, String content) {
        // 异步发送短信
    }
}

// CompletableFuture 方式
@Service
@RequiredArgsConstructor
public class OrderService {
    
    public OrderVO createOrder(OrderDTO dto) {
        // 保存订单
        OrderEntity order = saveOrder(dto);
        
        // 异步处理：发送通知、更新统计等
        CompletableFuture.runAsync(() -> {
            sendOrderNotification(order);
        });
        
        return convertToVO(order);
    }
}

// Spring Event 方式
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 处理订单创建事件
    }
}
```

## 输出格式

```markdown
## 技术方案选型

### 5.1 缓存方案

**选型**：Redis

**选型理由**：
- 项目已有 `spring-boot-starter-data-redis` 依赖
- 商品信息读多写少，适合缓存
- 需要分布式缓存支持集群部署

**应用场景**：
| 场景 | Key 设计 | 过期时间 | 说明 |
|------|---------|---------|------|
| 商品信息 | product:{productId} | 30分钟 | 热点数据缓存 |
| 用户信息 | user:{userId} | 60分钟 | 用户信息缓存 |
| 订单信息 | order:{orderId} | 10分钟 | 短期缓存 |

**实现要点**：
- 使用 StringRedisTemplate
- JSON 序列化
- 缓存穿透：布隆过滤器
- 缓存击穿：互斥锁
- 缓存雪崩：随机过期时间

### 5.2 消息队列

**选型**：RabbitMQ

**选型理由**：
- 项目已有 `spring-boot-starter-amqp` 依赖
- 需要延迟队列实现订单超时取消
- 可靠性要求高

**应用场景**：
| 场景 | Exchange | Queue | Routing Key |
|------|----------|-------|-------------|
| 订单超时取消 | order.exchange | order.cancel.queue | order.cancel |
| 支付成功通知 | payment.exchange | payment.success.queue | - |
| 库存回滚 | stock.exchange | stock.rollback.queue | stock.rollback |

### 5.3 分布式锁

**选型**：Redisson

**选型理由**：
- 功能完善，支持看门狗自动续期
- 使用简单，API 友好

**应用场景**：
| 场景 | Key 设计 | 说明 |
|------|---------|------|
| 库存扣减 | stock:{productId} | 防止超卖 |
| 订单创建 | order:create:{userId} | 防止重复提交 |

### 5.4 异步处理

**选型**：@Async + CompletableFuture

**应用场景**：
- 发送短信/邮件通知（@Async）
- 数据统计更新（CompletableFuture）

### 5.5 技术依赖检查清单

- [x] Redis：`spring-boot-starter-data-redis`
- [x] RabbitMQ：`spring-boot-starter-amqp`
- [x] Redisson：`redisson`
- [ ] Kafka：未引入（如需要高吞吐再考虑）
```

## 注意事项

- 必须优先检查项目已有依赖，复用已有技术栈
- 技术选型要考虑团队熟悉度
- 避免引入过多中间件增加复杂度
- 缓存要考虑一致性、穿透、击穿、雪崩问题
- 分布式锁要考虑锁续期、可重入、公平性
- 消息队列要考虑消息可靠性、顺序性、重复消费

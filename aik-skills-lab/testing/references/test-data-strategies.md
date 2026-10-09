# 测试数据管理策略

## 策略1：事务回滚（推荐）

使用 `@Transactional` 注解，测试完成后自动回滚：

```java
@SpringBootTest
@Transactional
@Rollback  // 默认true，测试后回滚
class OrderServiceIT {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void test() {
        // 插入的数据会在测试后自动回滚
        orderMapper.insert(order);
    }
}
```

**适用场景**：大多数集成测试，不需要验证数据持久化的场景

## 策略2：@BeforeEach / @AfterEach

每个测试方法前后准备和清理数据：

```java
@SpringBootTest
class OrderServiceIT {

    @BeforeEach
    void setUp() {
        insertBaseData();
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }
}
```

**适用场景**：需要特定数据状态的测试，不能使用事务回滚的场景（如测试事务本身）

## 策略3：@Sql 注解

使用 SQL 脚本准备数据：

```java
@SpringBootTest
@Sql(scripts = "/sql/order-test-data.sql", 
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", 
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OrderServiceIT {
    // ...
}
```

**适用场景**：复杂的数据准备逻辑，需要复用相同数据集的多个测试

### SQL 脚本示例

```sql
-- /src/test/resources/sql/order-test-data.sql

-- 清理旧数据（确保可重复执行）
DELETE FROM t_order WHERE order_no LIKE 'TEST%';
DELETE FROM t_user WHERE username LIKE 'test%';

-- 插入测试用户
INSERT INTO t_user (id, username, phone, create_time) 
VALUES (999001, 'test_user_001', '13800138001', NOW());

-- 插入测试订单
INSERT INTO t_order (id, user_id, order_no, amount, status, create_time)
VALUES (999001, 999001, 'TEST202403180001', 100.00, 1, NOW());
```

```sql
-- /src/test/resources/sql/cleanup.sql
DELETE FROM t_order WHERE order_no LIKE 'TEST%';
DELETE FROM t_user WHERE username LIKE 'test%';
```

## 策略4：Builder 模式

使用 Builder 模式创建测试数据：

```java
@Test
void test() {
    Order order = OrderBuilder.anOrder()
        .withUserId(100L)
        .withStatus(1)
        .withAmount(new BigDecimal("100.00"))
        .build();
    
    orderMapper.insert(order);
}
```

## 辅助模式

### TestDataConstants — 测试数据 ID 范围

```java
public final class TestDataConstants {
    private TestDataConstants() {}

    // 测试用户ID范围（避免与业务数据冲突）
    public static final Long TEST_USER_ID_START = 999001L;
    public static final Long TEST_USER_ID_END = 999100L;

    // 测试订单ID范围
    public static final Long TEST_ORDER_ID_START = 999001L;
    public static final Long TEST_ORDER_ID_END = 999100L;

    // 测试标识
    public static final String TEST_ORDER_PREFIX = "TEST";
    public static final String TEST_USER_PREFIX = "test_";
}
```

### TestDataPreparer — 基础数据准备类

```java
@Component
public class TestDataPreparer {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    public Order prepareOrder(Long userId) {
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setStatus(1);
        order.setAmount(new BigDecimal("100.00"));
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    public User prepareUser() {
        User user = new User();
        user.setUsername("test_user");
        user.setPhone("13800138000");
        userMapper.insert(user);
        return user;
    }

    public void cleanAll() {
        orderMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());
    }

    private String generateOrderNo() {
        return "TEST" + System.currentTimeMillis();
    }
}
```

## 最佳实践

- 使用事务回滚作为默认策略
- 测试数据 ID 使用特定范围，避免与业务数据冲突
- 测试数据使用可识别前缀（如 TEST_）
- 保持测试数据最小化，只准备必要数据
- 使用 Builder 模式或工厂方法创建复杂对象
- 不要依赖测试执行顺序
- 不要在测试间共享可变数据
- 不要使用生产数据作为测试数据

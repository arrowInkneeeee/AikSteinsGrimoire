---
name: test-data-manager
description: 为Java测试提供测试数据管理方案，包括测试数据的准备、清理和隔离策略。使用@BeforeEach/@AfterEach、@Sql注解或事务回滚确保测试间数据独立，避免测试数据污染。
type: Skill
version: 1.0.0
---

# Test Data Manager

## Purpose

为Java Spring Boot测试提供统一的测试数据管理方案，确保测试数据的准备、清理和隔离，避免测试间的数据污染。

## When to Use

- 需要为集成测试准备基础数据
- 需要确保测试间数据独立
- 需要清理测试产生的数据
- 需要管理大量测试数据场景

## Data Management Strategies

### 策略1：事务回滚（推荐）

使用`@Transactional`注解，测试完成后自动回滚：

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

**适用场景：**
- 大多数集成测试
- 不需要验证数据持久化的场景

### 策略2：@BeforeEach / @AfterEach

每个测试方法前后准备和清理数据：

```java
@SpringBootTest
class OrderServiceIT {

    @Autowired
    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        // 准备基础数据
        insertBaseData();
    }

    @AfterEach
    void tearDown() {
        // 清理数据
        cleanTestData();
    }
}
```

**适用场景：**
- 需要特定数据状态的测试
- 不能使用事务回滚的场景（如测试事务本身）

### 策略3：@Sql 注解

使用SQL脚本准备数据：

```java
@SpringBootTest
@Sql(scripts = "/sql/order-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OrderServiceIT {
    // ...
}
```

**适用场景：**
- 复杂的数据准备逻辑
- 需要复用相同数据集的多个测试

### 策略4：测试数据Builder模式

使用Builder模式创建测试数据：

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

## Implementation Guidelines

### 1. 基础数据准备类

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
        return "ORD" + System.currentTimeMillis();
    }
}
```

### 2. 测试中使用

```java
@SpringBootTest
@Transactional
class OrderServiceIT {

    @Autowired
    private TestDataPreparer testDataPreparer;

    @Autowired
    private OrderService orderService;

    @Test
    @DisplayName("查询用户订单")
    void queryUserOrders() {
        // given
        User user = testDataPreparer.prepareUser();
        testDataPreparer.prepareOrder(user.getId());
        testDataPreparer.prepareOrder(user.getId());

        // when
        List<Order> orders = orderService.getByUserId(user.getId());

        // then
        assertThat(orders).hasSize(2);
    }
}
```

### 3. SQL脚本方式

```sql
-- /src/test/resources/sql/order-test-data.sql

-- 清理旧数据（确保可重复执行）
DELETE FROM t_order WHERE order_no LIKE 'TEST%';
DELETE FROM t_user WHERE username LIKE 'test%';

-- 插入测试用户
INSERT INTO t_user (id, username, phone, create_time) 
VALUES (999001, 'test_user_001', '13800138001', NOW());

INSERT INTO t_user (id, username, phone, create_time) 
VALUES (999002, 'test_user_002', '13800138002', NOW());

-- 插入测试订单
INSERT INTO t_order (id, user_id, order_no, amount, status, create_time)
VALUES (999001, 999001, 'TEST202403180001', 100.00, 1, NOW());

INSERT INTO t_order (id, user_id, order_no, amount, status, create_time)
VALUES (999002, 999001, 'TEST202403180002', 200.00, 1, NOW());

INSERT INTO t_order (id, user_id, order_no, amount, status, create_time)
VALUES (999003, 999002, 'TEST202403180003', 150.00, 2, NOW());
```

```sql
-- /src/test/resources/sql/cleanup.sql

DELETE FROM t_order WHERE order_no LIKE 'TEST%';
DELETE FROM t_user WHERE username LIKE 'test%';
```

### 4. 常量管理

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

## Best Practices

### DO

- 使用事务回滚作为默认策略
- 测试数据ID使用特定范围，避免与业务数据冲突
- 测试数据使用可识别前缀（如TEST_）
- 保持测试数据最小化，只准备必要数据
- 使用Builder模式或工厂方法创建复杂对象

### DON'T

- 不要依赖测试执行顺序
- 不要在测试间共享可变数据
- 不要使用生产数据作为测试数据
- 不要手动清理数据（优先使用自动回滚）

## Example: 完整测试类

```java
@SpringBootTest
@Transactional
@DisplayName("订单服务集成测试")
class OrderServiceIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 准备基础用户数据
        testUser = new User();
        testUser.setId(999001L);
        testUser.setUsername("test_user");
        testUser.setPhone("13800138000");
        userMapper.insert(testUser);
    }

    @Nested
    @DisplayName("创建订单")
    class CreateOrderTest {

        @Test
        @DisplayName("正常创建订单")
        void createOrder_success() {
            // given
            OrderDTO dto = new OrderDTO();
            dto.setUserId(testUser.getId());
            dto.setItems(Arrays.asList(
                createItem(1L, 2),
                createItem(2L, 1)
            ));

            // when
            Long orderId = orderService.createOrder(dto);

            // then
            assertThat(orderId).isNotNull();
            Order saved = orderMapper.selectById(orderId);
            assertThat(saved).isNotNull();
            assertThat(saved.getUserId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("创建订单-库存不足")
        void createOrder_insufficientStock() {
            // given
            OrderDTO dto = new OrderDTO();
            dto.setUserId(testUser.getId());
            dto.setItems(Arrays.asList(
                createItem(1L, 9999)  // 超库存
            ));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
        }
    }

    @Nested
    @DisplayName("查询订单")
    class QueryOrderTest {

        @BeforeEach
        void setUp() {
            // 准备订单数据
            insertTestOrders();
        }

        @Test
        @DisplayName("根据用户ID查询订单列表")
        void getByUserId_success() {
            // when
            List<Order> orders = orderService.getByUserId(testUser.getId());

            // then
            assertThat(orders).hasSize(2);
        }
    }

    private OrderItemDTO createItem(Long productId, Integer quantity) {
        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private void insertTestOrders() {
        for (int i = 1; i <= 2; i++) {
            Order order = new Order();
            order.setUserId(testUser.getId());
            order.setOrderNo("TEST" + String.format("%03d", i));
            order.setAmount(new BigDecimal(i * 100));
            order.setStatus(1);
            order.setCreateTime(LocalDateTime.now());
            orderMapper.insert(order);
        }
    }
}
```

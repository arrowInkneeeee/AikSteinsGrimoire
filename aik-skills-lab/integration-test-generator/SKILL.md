---
name: integration-test-generator
description: 为Java Spring Boot项目生成集成测试，使用@SpringBootTest测试真实数据库交互。自动检测数据库类型（MySQL/PostgreSQL/Oracle），使用Testcontainers或H2进行测试。适用于验证Mapper SQL执行、事务行为和数据库约束。
type: Skill
version: 1.0.0
---

# Integration Test Generator

## Purpose

为Java Spring Boot项目生成集成测试，验证真实的数据库交互行为，包括SQL执行、事务管理和数据库约束。

## When to Use

- 需要验证Mapper层的真实SQL执行
- 测试复杂查询（多表关联、嵌套查询）
- 验证事务回滚行为
- 测试数据库约束（唯一索引、外键、检查约束）
- 验证乐观锁（@Version）行为

## Testing Strategy

### 测试范围

**必须测试：**
- 自定义SQL方法（@Select/@Insert/@Update/@Delete）
- 复杂LambdaQueryWrapper查询
- 多表关联查询
- 批量操作（insertBatch、updateBatch）
- 分页查询（IPage）

**不测试：**
- BaseMapper自带方法（selectById等，MyBatis-Plus已保证）
- 简单的单表CRUD

### 数据库选择策略

自动检测pom.xml中的数据库依赖：

| 检测到依赖 | 测试方案 |
|-----------|----------|
| mysql-connector-java | Testcontainers + MySQL容器 |
| postgresql | Testcontainers + PostgreSQL容器 |
| ojdbc | Testcontainers + Oracle容器（如可用）或H2兼容模式 |
| h2 | H2内存数据库 |
| 其他/未检测到 | 询问用户或默认H2 |

### 测试隔离

所有集成测试使用`@Transactional`确保数据回滚，不污染数据库：

```java
@SpringBootTest
@Transactional
@Rollback
class OrderMapperTest {
    // 测试完成后自动回滚
}
```

## Test Structure

```java
/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:09
 * -
 **/
@SpringBootTest
@Transactional
@DisplayName("订单Mapper集成测试")
class OrderMapperIT {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    @DisplayName("根据状态查询订单列表")
    void selectByStatus_success() {
        // -anchor given: 准备测试数据
        // -anchor when: 执行查询
        // -anchor then: 验证结果
    }
}
```

**规范说明**：
- 集成测试类使用 `-anchor` 类注释
- 集成测试类命名建议使用 `XxIT` 后缀（如 `OrderMapperIT`）
- 使用 `@DisplayName` 描述测试类和方法
- 测试步骤使用 `-anchor` 标记

## Generation Rules

### 1. 测试类命名

```java
// 被测Mapper: OrderMapper
// 测试类: OrderMapperIT 或 OrderMapperTest

// 推荐区分：
// OrderServiceTest - 单元测试（Mock）
// OrderMapperIT - 集成测试（真实数据库）
```

### 2. 测试数据准备

```java
@Test
@DisplayName("查询用户订单列表")
void selectByUserId_success() {
    // -anchor given
    OrderPo order1 = new OrderPo();
    order1.setUserId(1L);
    order1.setOrderNo("ORD001");
    order1.setStatus(1);
    orderMapper.insert(order1);

    OrderPo order2 = new OrderPo();
    order2.setUserId(1L);
    order2.setOrderNo("ORD002");
    order2.setStatus(2);
    orderMapper.insert(order2);

    // -anchor when
    List<OrderPo> result = orderMapper.selectByUserId(1L);

    // -anchor then
    assertThat(result).hasSize(2);
    assertThat(result).extracting(OrderPo::getOrderNo).contains("ORD001", "ORD002");
}
```

**规范说明**：
- 测试数据类使用 `XxPo` 命名规范
- 测试步骤使用 `-anchor` 标记（given/when/then）

### 3. LambdaQueryWrapper 测试

```java
@Test
@DisplayName("使用LambdaQueryWrapper条件查询")
void selectList_withWrapper_success() {
    // given
    insertTestData();

    // when
    LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Order::getStatus, 1)
           .ge(Order::getCreateTime, startDate)
           .orderByDesc(Order::getCreateTime);
    List<Order> result = orderMapper.selectList(wrapper);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getStatus()).isEqualTo(1);
}
```

### 4. 分页查询测试

```java
@Test
@DisplayName("分页查询订单")
void selectPage_success() {
    // given
    insertTestData(20); // 插入20条数据

    // when
    Page<Order> page = new Page<>(1, 10); // 第1页，每页10条
    LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
    wrapper.orderByDesc(Order::getCreateTime);
    IPage<Order> result = orderMapper.selectPage(page, wrapper);

    // then
    assertThat(result.getTotal()).isEqualTo(20);
    assertThat(result.getRecords()).hasSize(10);
    assertThat(result.getCurrent()).isEqualTo(1);
    assertThat(result.getPages()).isEqualTo(2);
}
```

### 5. 自定义SQL测试

```java
@Test
@DisplayName("自定义SQL查询订单详情")
void selectOrderDetail_success() {
    // given
    Long orderId = insertTestOrderWithDetail();

    // when
    OrderDetailVO detail = orderMapper.selectOrderDetail(orderId);

    // then
    assertThat(detail).isNotNull();
    assertThat(detail.getOrderItems()).isNotEmpty();
}
```

### 6. 事务回滚测试

```java
@Test
@DisplayName("事务回滚验证")
@Transactional
void transaction_rollback() {
    // given
    Order order = createTestOrder();

    // when
    try {
        orderService.createOrderWithException(order);
    } catch (Exception e) {
        // expected
    }

    // then: 验证数据未插入
    Order saved = orderMapper.selectById(order.getId());
    assertThat(saved).isNull();
}
```

## Testcontainers Configuration

### MySQL

```java
@TestConfiguration
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
    }
}
```

### H2（Fallback）

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## Dependencies

```xml
<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>

<!-- H2 Fallback -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

## Output Location

```
src/test/java/com/example/module/mapper/OrderMapperIT.java
```

## Example Output

```java
package com.example.order.mapper;

import com.example.order.entity.OrderPo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:09
 * -
 **/
@SpringBootTest
@Transactional
@Rollback
@DisplayName("订单Mapper集成测试")
class OrderMapperIT {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    @DisplayName("插入订单并查询")
    void insertAndSelect_success() {
        // -anchor given
        OrderPo order = new OrderPo();
        order.setUserId(1L);
        order.setOrderNo("ORD202403180001");
        order.setAmount(new BigDecimal("100.00"));
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());

        // -anchor when
        int insertCount = orderMapper.insert(order);
        OrderPo result = orderMapper.selectById(order.getId());

        // -anchor then
        assertThat(insertCount).isEqualTo(1);
        assertThat(result).isNotNull();
        assertThat(result.getOrderNo()).isEqualTo("ORD202403180001");
    }

    @Test
    @DisplayName("使用LambdaQueryWrapper条件查询")
    void selectList_withWrapper_success() {
        // -anchor given
        insertTestOrders();

        // -anchor when
        LambdaQueryWrapper<OrderPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPo::getStatus, 1)
               .ge(OrderPo::getAmount, new BigDecimal("50.00"));
        List<OrderPo> result = orderMapper.selectList(wrapper);

        // -anchor then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(OrderPo::getStatus).containsOnly(1);
    }

    @Test
    @DisplayName("分页查询订单")
    void selectPage_success() {
        // -anchor given
        insertTestOrders(25);

        // -anchor when
        Page<OrderPo> page = new Page<>(2, 10);
        LambdaQueryWrapper<OrderPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OrderPo::getCreateTime);
        IPage<OrderPo> result = orderMapper.selectPage(page, wrapper);

        // -anchor then
        assertThat(result.getTotal()).isEqualTo(25);
        assertThat(result.getRecords()).hasSize(10);
        assertThat(result.getCurrent()).isEqualTo(2);
    }

    private void insertTestOrders() {
        for (int i = 1; i <= 5; i++) {
            OrderPo order = new OrderPo();
            order.setUserId((long) i);
            order.setOrderNo("ORD" + String.format("%03d", i));
            order.setAmount(new BigDecimal(i * 10));
            order.setStatus(i % 2 == 0 ? 1 : 0);
            order.setCreateTime(LocalDateTime.now());
            orderMapper.insert(order);
        }
    }

    private void insertTestOrders(int count) {
        for (int i = 1; i <= count; i++) {
            OrderPo order = new OrderPo();
            order.setUserId((long) i);
            order.setOrderNo("ORD" + String.format("%03d", i));
            order.setAmount(new BigDecimal(i * 10));
            order.setStatus(1);
            order.setCreateTime(LocalDateTime.now());
            orderMapper.insert(order);
        }
    }
}
```

**规范说明**：
- 集成测试类使用 `-anchor` 类注释
- 集成测试类命名建议使用 `XxIT` 后缀（如 `OrderMapperIT`）
- 使用 `@DisplayName` 描述测试类和方法
- 测试步骤使用 `-anchor` 标记（given/when/then）
- 测试数据类使用 `XxPo` 命名规范

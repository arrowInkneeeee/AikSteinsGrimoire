# Java 常见缺陷模式与预防清单

## 8 种常见缺陷模式

### 1. 空指针异常 (NullPointerException)

```java
// 问题代码
String name = user.getName().trim();

// 正确做法
String name = Optional.ofNullable(user.getName())
    .map(String::trim)
    .orElse("");
```

**预防**：使用 Optional、返回空集合而非 null、Objects.requireNonNull 前置校验

### 2. 并发问题

```java
// 问题代码
private int counter = 0;
public void increment() { counter++; }

// 正确做法
private AtomicInteger counter = new AtomicInteger(0);
public void increment() { counter.incrementAndGet(); }
```

**预防**：识别共享可变状态、使用线程安全类（Atomic*, ConcurrentHashMap）、Spring Bean 默认单例注意状态管理

### 3. 资源泄漏

```java
// 问题代码
InputStream is = new FileInputStream("file.txt");
// 忘记关闭

// 正确做法
try (InputStream is = new FileInputStream("file.txt")) {
    // 使用is...
}
```

**预防**：使用 try-with-resources、@Cleanup（Lombok）、确保数据库连接/流/锁释放

### 4. SQL 注入

```java
// 问题代码
@Select("SELECT * FROM user WHERE name = '" + name + "'")
User findByName(String name);

// 正确做法
@Select("SELECT * FROM user WHERE name = #{name}")
User findByName(@Param("name") String name);
```

**预防**：使用 #{} 参数绑定避免 ${}、使用 MyBatis-Plus 条件构造器、校验和转义用户输入

### 5. 事务问题

```java
// 问题代码
@Transactional
public void createOrder(Order order) {
    orderMapper.insert(order);
    try {
        paymentService.pay(order);
    } catch (Exception e) {
        log.error("支付失败", e);  // 异常被吃掉，事务不回滚
    }
}

// 正确做法
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    orderMapper.insert(order);
    try {
        paymentService.pay(order);
    } catch (Exception e) {
        log.error("支付失败", e);
        throw new BusinessException("支付失败", e);
    }
}
```

**预防**：理解传播行为和回滚规则、不要私自捕获异常、明确指定 rollbackFor、注意同类方法调用不走代理

### 6. N+1 查询问题

```java
// 问题代码
List<Order> orders = orderMapper.selectList();
for (Order order : orders) {
    User user = userMapper.selectById(order.getUserId());  // 每次循环查数据库
}

// 正确做法：批量查询
List<Long> userIds = orders.stream()
    .map(Order::getUserId)
    .collect(Collectors.toList());
Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
    .stream()
    .collect(Collectors.toMap(User::getId, Function.identity()));
```

**预防**：使用自定义 SQL 和 JOIN、使用分页、开启 SQL 日志监控查询次数

### 7. 日期时间处理错误

```java
// 问题代码
Date now = new Date();  // 可读性差，时区问题

// 正确做法（Java 8）
LocalDateTime now = LocalDateTime.now();
ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
```

**预防**：使用 Java 8 日期时间 API、明确时区处理、前后端约定统一格式（ISO-8601）

### 8. 类型转换异常

```java
// 问题代码
Long id = (Long) request.get("id");  // 可能是Integer

// 正确做法
Long id = Long.valueOf(request.get("id").toString());
```

**预防**：避免强制类型转换、使用类型安全的 DTO、使用 MapStruct、校验输入参数

---

## 缺陷预防清单

### 空值处理

- [ ] 方法参数是否进行 null 检查
- [ ] 方法返回值是否可能为 null（文档说明）
- [ ] 链式调用是否存在 NPE 风险
- [ ] Optional 使用是否规范

### 并发安全

- [ ] 是否存在共享可变状态
- [ ] 单例 Bean 是否有实例变量
- [ ] 并发集合使用是否正确
- [ ] 锁的粒度是否合适

### 资源管理

- [ ] 流是否正确关闭
- [ ] 数据库连接是否释放
- [ ] 锁是否正确释放
- [ ] 是否使用 try-with-resources

### 数据访问

- [ ] 是否存在 N+1 查询
- [ ] SQL 是否使用参数绑定
- [ ] 事务边界是否合理
- [ ] 分页参数是否正确

### 异常处理

- [ ] 异常是否被正确捕获和处理
- [ ] 是否捕获了过于宽泛的异常
- [ ] 异常信息是否有意义
- [ ] 是否吞掉了异常

### 类型安全

- [ ] 强制类型转换是否安全
- [ ] 泛型使用是否正确
- [ ] 原始类型是否避免使用
- [ ] 数字计算是否存在溢出

### 业务逻辑

- [ ] 边界条件是否处理
- [ ] 并发场景是否考虑
- [ ] 幂等性是否保证
- [ ] 状态流转是否合理
